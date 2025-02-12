package raylras.zen.model.resolve;

import org.antlr.v4.runtime.tree.ParseTree;
import raylras.zen.model.CompilationUnit;
import raylras.zen.model.Compilations;
import raylras.zen.model.Visitor;
import raylras.zen.model.parser.ZenScriptParser.*;
import raylras.zen.model.symbol.*;
import raylras.zen.util.Ranges;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public final class SymbolResolver {

    private static final List<Class<?>> ROOT_EXPRESSION_PARENTS = List.of(
            ImportDeclarationContext.class,
            ForeachStatementContext.class,
            ForeachVariableContext.class,
            WhileStatementContext.class,
            IfStatementContext.class,
            ExpressionStatementContext.class,
            ReturnStatementContext.class
    );

    private SymbolResolver() {}

    public static Collection<Symbol> lookupSymbol(ParseTree cst, CompilationUnit unit) {
        ParseTree expr = findRootExpression(cst);
        if (expr == null) {
            return Collections.emptyList();
        }
        SymbolVisitor visitor = new SymbolVisitor(unit, cst);
        expr.accept(visitor);
        return Collections.unmodifiableCollection(visitor.result);
    }

    public static Collection<ClassSymbol> lookupClass(QualifiedNameContext cst, CompilationUnit unit) {
        SymbolVisitor visitor = new SymbolVisitor(unit, cst);
        cst.accept(visitor);
        Collection<ClassSymbol> classes = new ArrayList<>();
        for (Symbol symbol : visitor.result) {
            if (symbol instanceof ClassSymbol classSymbol) {
                classes.add(classSymbol);
            } else if (symbol instanceof ImportSymbol importSymbol) {
                importSymbol.getTargets().stream()
                        .filter(ClassSymbol.class::isInstance)
                        .map(ClassSymbol.class::cast)
                        .forEach(classes::add);
            }
        }
        return classes;
    }

    private static ParseTree findRootExpression(ParseTree cst) {
        ParseTree current = cst;
        while (current != null) {
            ParseTree parent = current.getParent();
            if (parent != null && ROOT_EXPRESSION_PARENTS.contains(parent.getClass())) {
                return current;
            }
            current = parent;
        }
        return null;
    }

    private static class SymbolVisitor extends Visitor<SymbolProvider> {
        final CompilationUnit unit;
        final ParseTree cst;
        Collection<Symbol> result = Collections.emptyList();

        SymbolVisitor(CompilationUnit unit, ParseTree cst) {
            this.unit = unit;
            this.cst = cst;
        }

        @Override
        public SymbolProvider visitQualifiedName(QualifiedNameContext ctx) {
            List<SimpleNameContext> simpleNames = ctx.simpleName();
            if (simpleNames.isEmpty()) {
                return SymbolProvider.empty();
            }

            SimpleNameContext start = simpleNames.get(0);
            Collection<? extends Symbol> symbols = lookupSymbol(start, start.getText());
            updateResult(symbols);
            for (int i = 1; i < simpleNames.size(); i++) {
                symbols = accessMember(symbols, simpleNames.get(i).getText());
                if (Ranges.contains(cst, simpleNames.get(i))) {
                    updateResult(symbols);
                }
            }
            return SymbolProvider.of(symbols);
        }

        @Override
        public SymbolProvider visitSimpleNameExpr(SimpleNameExprContext ctx) {
            Collection<Symbol> symbols = lookupSymbol(ctx, ctx.simpleName().getText());
            if (Ranges.contains(cst, ctx.simpleName())) {
                updateResult(symbols);
            }
            return SymbolProvider.of(symbols);
        }

        @Override
        public SymbolProvider visitMemberAccessExpr(MemberAccessExprContext ctx) {
            SymbolProvider provider = visit(ctx.expression());
            if (provider.getSymbols().size() != 1) {
                return SymbolProvider.empty();
            }

            Symbol symbol = provider.getSymbols().stream().findFirst().orElse(null);
            Collection<Symbol> symbols;
            if (symbol instanceof ClassSymbol classSymbol) {
                symbols = classSymbol.getSymbols().stream()
                        .filter(Symbol::isStatic)
                        .filter(isSymbolNameEquals(ctx.simpleName().getText()))
                        .toList();
            } else if (symbol.getType() instanceof SymbolProvider type) {
                symbols = type.withExpands(unit.getEnv()).getSymbols().stream()
                        .filter(isSymbolNameEquals(ctx.simpleName().getText()))
                        .toList();
            } else {
                symbols = Collections.emptyList();
            }
            if (Ranges.contains(cst, ctx.simpleName())) {
                updateResult(symbols);
            }
            return SymbolProvider.of(symbols);
        }

        @Override
        protected SymbolProvider defaultResult() {
            return SymbolProvider.empty();
        }

        void updateResult(Collection<? extends Symbol> result) {
            this.result = Collections.unmodifiableCollection(result);
        }

        Collection<Symbol> lookupSymbol(ParseTree cst, String name) {
            Collection<? extends Symbol> result;

            result = lookupLocalSymbol(cst, name);
            if (!result.isEmpty()) {
                return Collections.unmodifiableCollection(result);
            }

            result = lookupToplevelSymbol(name);
            if (!result.isEmpty()) {
                return Collections.unmodifiableCollection(result);
            }

            result = lookupImportSymbol(name);
            if (!result.isEmpty()) {
                return Collections.unmodifiableCollection(result);
            }

            result = lookupGlobalSymbol(name);
            if (!result.isEmpty()) {
                return Collections.unmodifiableCollection(result);
            }

            result = lookupPackageSymbol(name);
            if (!result.isEmpty()) {
                return Collections.unmodifiableCollection(result);
            }

            return Collections.emptyList();
        }

        Collection<Symbol> lookupLocalSymbol(ParseTree cst, String name) {
            return Compilations.lookupScope(unit, cst)
                    .map(scope -> scope.getSymbols().stream()
                            .filter(isSymbolNameEquals(name))
                            .toList())
                    .orElseGet(Collections::emptyList);
        }

        Collection<Symbol> lookupToplevelSymbol(String name) {
            return unit.getTopLevelSymbols().stream()
                    .filter(isSymbolNameEquals(name))
                    .toList();
        }

        Collection<ImportSymbol> lookupImportSymbol(String name) {
            return unit.getImports().stream()
                    .filter(isSymbolNameEquals(name))
                    .toList();
        }

        Collection<Symbol> lookupGlobalSymbol(String name) {
            return unit.getEnv().getGlobals()
                    .filter(isSymbolNameEquals(name))
                    .toList();
        }

        Collection<PackageSymbol> lookupPackageSymbol(String name) {
            return unit.getEnv().getRootPackage().getSubpackages().stream()
                    .filter(isSymbolNameEquals(name))
                    .toList();
        }

        Collection<Symbol> accessMember(Collection<? extends Symbol> symbolSpace, String memberName) {
            if (symbolSpace.size() != 1) {
                return Collections.emptyList();
            }
            return symbolSpace.stream()
                    .filter(SymbolProvider.class::isInstance)
                    .map(SymbolProvider.class::cast)
                    .flatMap(provider -> provider.getSymbols().stream())
                    .filter(isSymbolNameEquals(memberName))
                    .toList();
        }

        <T extends Symbol> Predicate<T> isSymbolNameEquals(String name) {
            return symbol -> name.equals(symbol.getName());
        }
    }

}
