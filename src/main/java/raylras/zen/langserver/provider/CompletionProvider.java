package raylras.zen.langserver.provider;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import org.eclipse.lsp4j.*;
import raylras.zen.code.CompilationUnit;
import raylras.zen.code.data.Declarator;
import raylras.zen.code.symbol.*;
import raylras.zen.langserver.data.CompletionData;
import raylras.zen.code.parser.ZenScriptLexer;
import raylras.zen.code.parser.ZenScriptParser.*;
import raylras.zen.langserver.search.CompletionDataResolver;
import raylras.zen.code.scope.Scope;
import raylras.zen.code.type.*;
import raylras.zen.l10n.L10N;
import raylras.zen.service.LibraryService;
import raylras.zen.util.*;
import raylras.zen.util.Range;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CompletionProvider {

    private final CompilationUnit unit;
    private final List<CompletionItem> data = new ArrayList<>();
    private boolean isInComplete = false;
    private static final int MAX_ITEMS = 50;

    private final CompletionData completionData;

    private static final String[] KEYWORDS = makeKeywords();

    public CompletionProvider(CompilationUnit unit, CompletionData completionNode) {
        this.unit = unit;
        this.completionData = completionNode;
    }

    public static CompletionList completion(CompilationUnit unit, CompletionParams params) {
        Range cursorPos = Ranges.from(params.getPosition());
        CompletionData completionData = new CompletionDataResolver(unit, cursorPos).resolve(unit.parseTree);
        CompletionProvider provider = new CompletionProvider(unit, completionData);
        provider.complete();
        return new CompletionList(provider.isInComplete, provider.data);
    }

    private void complete() {


        switch (completionData.kind) {
            case IDENTIFIER:
                completeIdentifier();
                break;
            case IMPORT:
                completeImport();
                break;
            case MEMBER_ACCESS:
                completeMemberAccess();
                break;
            case BRACKET_HANDLER:
                completeBracketHandler();
                break;
            case DEFAULT:
                completeDefault();
                break;
            case NONE:
                break;
        }

    }


    // basic completion methods
    private void completeIdentifier() {
        if (completionData.node instanceof LocalAccessExprContext) {
            completeLocalSymbols(s -> true);
            completeGlobalSymbols(s -> true, true);
            completeKeywords();
        } else {
            completeLocalSymbols(s -> s.getKind().isClass());
            completeGlobalSymbols(s -> s.getKind().isClass(), true);
        }

        completeAutoImportedClass();
    }

    private void completeImport() {

        QualifiedNameContext qualifierExpr = ((ImportDeclarationContext) completionData.node).qualifiedName();
        String qualifiedName = qualifierExpr.getText();

        if (Strings.isNullOrEmpty(qualifiedName)) {
            completeGlobalSymbols(s -> !s.isDeclaredBy(Declarator.GLOBAL), true);
        } else {

            Map<String, CompletionItem> itemMap = new HashMap<>();

            Tuple<String, Collection<String>> possiblePackage = MemberUtils.findPackages(unit, qualifiedName);

            for (String child : possiblePackage.second) {
                itemMap.put(child, makePackage(child, false));
            }
            if (possiblePackage.first != null) {
                for (Symbol member : unit.environment().getSymbolsOfPackage(possiblePackage.first)) {
                    itemMap.put(member.getName(), makeItem(member));
                }
            }

            ClassSymbol availableClass = unit.environment().findSymbol(ClassSymbol.class, qualifiedName);
            if (availableClass != null) {
                MemberUtils.iterateMembers(unit.environment(), availableClass.getType(), true, member -> {
                    if (!isNameMatchesCompleting(member.getName())) {
                        return;
                    }
                    itemMap.put(member.getName(), makeItem(member));
                });
            }

            data.addAll(itemMap.values());
        }

    }

    private void completeBracketHandler() {

    }

    private void completeMemberAccess() {
        ExpressionContext qualifierExpr = completionData.getQualifierExpression();


        Tuple<Boolean, Type> qualifierType = MemberUtils.resolveQualifierTarget(unit, qualifierExpr);
        boolean endWithParen = completionData.isEndsWithParen();

        if (!TypeUtils.isValidType(qualifierType.second)) {
            String text = qualifierExpr.getText();
            Tuple<String, Collection<String>> possiblePackage = MemberUtils.findPackages(unit, text);
            addPackageAndChildren(possiblePackage.first, possiblePackage.second, endWithParen);
            return;
        }


        addMemberAccess(qualifierType.second, qualifierType.first, endWithParen);
    }

    private void addPackageAndChildren(@Nullable String packageName, Collection<String> childPackages, boolean endsWithParen) {
        if (packageName != null) {

            for (Symbol member : unit.environment().getSymbolsOfPackage(packageName)) {
                if (member.getKind().isFunction()) {
                    data.add(makeFunction((FunctionSymbol) member, !endsWithParen));
                } else {
                    data.add(makeItem(member));
                }
            }
        }
        for (String child : childPackages) {
            data.add(makePackage(child, false));
        }
    }

    private void completeDefault() {
        completeKeywords();
    }


    private void completeLocalSymbols(Predicate<Symbol> condition) {
        Scope scope = unit.lookupScope(completionData.node);
        if (scope == null)
            return;
        boolean endWithParen = completionData.isEndsWithParen();

        List<Symbol> symbols = unit.lookupLocalSymbols(Symbol.class, scope,
            it -> isNameMatchesCompleting(it.getName())
        );
        for (Symbol symbol : symbols) {
            if (!condition.test(symbol)) {
                continue;
            }
            if (symbol.getKind() == ZenSymbolKind.IMPORT) {
                ImportSymbol importSymbol = (ImportSymbol) symbol;
                if (importSymbol.isFunctionImport()) {
                    for (FunctionSymbol functionTarget : importSymbol.getFunctionTargets()) {
                        if (!condition.test(functionTarget)) {
                            continue;
                        }
                        data.add(makeFunction(functionTarget, !endWithParen));
                    }
                } else {
                    Symbol target = importSymbol.getSimpleTarget();
                    if (!condition.test(target)) {
                        continue;
                    }
                    if (target != null) {
                        data.add(makeItem(target));
                    } else {
                        data.add(makeItem(symbol));
                    }
                }
            } else if (symbol.getKind().isFunction()) {
                data.add(makeFunction((FunctionSymbol) symbol, !endWithParen));
            } else {
                data.add(makeItem(symbol));
            }
        }
    }

    private void completeGlobalSymbols(Predicate<Symbol> condition, boolean addPackages) {
        for (Symbol member : unit.environment().getGlobals()) {
            if (!condition.test(member)) {
                continue;
            }
            if (isNameMatchesCompleting(member.getName())) {
                data.add(makeItem(member));
            }
        }

        if (addPackages) {
            if (isNameMatchesCompleting("scripts")) {
                data.add(makePackage("scripts", true));
            }
            for (String rootPackageName : unit.environment().libraryService().allRootPackageNames()) {
                data.add(makePackage(rootPackageName, true));
            }
        }
    }

    private void completeAutoImportedClass() {
        LibraryService libraryService = unit.environment().libraryService();
        String completingString = completionData.completingString;
        for (String clazzName : libraryService.allGlobalClasses()) {
            if (SymbolUtils.isNativeClass(clazzName)) {
                continue;
            }
            if (data.size() > MAX_ITEMS) {
                isInComplete = true;
                break;
            }
            String simpleClassName = StringUtils.getSimpleName(clazzName);
            if (StringUtils.matchesPartialName(simpleClassName, completingString)) {
                ClassSymbol classSymbol = libraryService.getClassSymbol(clazzName);
                CompletionItem item = makeItem(classSymbol);
                item.setAdditionalTextEdits(makeAutoImports(classSymbol));
                data.add(item);
            }
        }
    }


    private boolean isNameMatchesCompleting(String candidate) {
        return StringUtils.matchesPartialName(candidate, completionData.completingString);
    }

    private void addMemberAccess(Type type, boolean isStatic, boolean endsWithParen) {
//        HashMap<String, List<FunctionSymbol>> functions = new HashMap<>();
        MemberUtils.iterateMembers(unit.environment(), type, isStatic, member -> {

            if (!isNameMatchesCompleting(member.getName())) {
                return;
            }

            if (member.getKind().isFunction()) {
//                functions.computeIfAbsent(member.getName(), n -> new ArrayList<>())
//                    .add((FunctionSymbol) member);
                data.add(makeFunction((FunctionSymbol) member, !endsWithParen));
            } else {
                data.add(makeItem(member));
            }

        });

//        for (List<FunctionSymbol> overloads : functions.values()) {
//            data.add(makeFunctions(overloads, !endsWithParen));
//        }
    }

    private void completeKeywords() {
        for (String keyword : KEYWORDS) {
            if (isNameMatchesCompleting(keyword)) {
                CompletionItem item = new CompletionItem(keyword);
                item.setKind(CompletionItemKind.Keyword);
                item.setDetail(L10N.getString("l10n.keyword"));
                data.add(item);
            }
        }
    }

    private static CompletionItemKind getCompletionItemKind(Symbol symbol) {
        switch (symbol.getKind()) {
            case ZEN_CLASS:
            case NATIVE_CLASS:
            case LIBRARY_CLASS:
                return CompletionItemKind.Class;
            case INTERFACE:
            case FUNCTIONAL_INTERFACE:
                return CompletionItemKind.Interface;
            case OPERATOR:
                return CompletionItemKind.Operator;
            case LOCAL_VARIABLE:
                return CompletionItemKind.Variable;
            case GLOBAL_VARIABLE:
            case FIELD:
                return CompletionItemKind.Field;
            case FUNCTION_PARAMETER:
                return CompletionItemKind.Property;
            case FUNCTION:
            case FUNCTION_EXPRESSION:
            case EXPAND_FUNCTION:
                return CompletionItemKind.Function;
            case CONSTRUCTOR:
                return CompletionItemKind.Constructor;
            case NONE:
            default:
                return null;
        }
    }

    // tool methods for make completionItem
    private static String[] makeKeywords() {
        try {
            Pattern pattern = Pattern.compile("^[a-zA-Z].*");
            Method method = ZenScriptLexer.class.getDeclaredMethod("makeLiteralNames");
            method.setAccessible(true);
            String[] literalNames = (String[]) method.invoke(null);
            List<String> keywordList = Arrays.stream(literalNames)
                .filter(Objects::nonNull)
                .map(literal -> literal.replaceAll("'", ""))
                .filter(literal -> pattern.matcher(literal).matches())
                .collect(Collectors.toList());
            return keywordList.toArray(new String[]{});
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return new String[]{};
    }

    private Range getImportInsertPlace() {
        List<ImportDeclarationContext> imports = ((CompilationUnitContext) unit.parseTree).importDeclaration();
        ImportDeclarationContext last = imports.get(imports.size() - 1);

        int line = last.stop.getLine() - 1;
        int column = last.stop.getCharPositionInLine() + last.stop.getText().length();

        return new Range(line, column + 1, line, column + 1);

    }

    private List<TextEdit> makeAutoImports(ClassSymbol symbol) {
        TextEdit textEdit = new TextEdit();

        textEdit.setRange(Ranges.toLSPRange(getImportInsertPlace()));
        textEdit.setNewText("\nimport " + symbol.getQualifiedName() + ";");
        return Collections.singletonList(textEdit);
    }

    private CompletionItem makeItem(Symbol symbol) {
        CompletionItem item = new CompletionItem();
        if (symbol instanceof ClassSymbol) {
            // class symbol name may contain package.
            String name = StringUtils.getSimpleName(((ClassSymbol) symbol).getQualifiedName());
            item.setLabel(name);
        } else {
            item.setLabel(symbol.getName());
        }
        item.setKind(getCompletionItemKind(symbol));
        item.setDetail(symbol.toString());
//        item.setData();
        return item;
    }

    private CompletionItem makePackage(String packageName, boolean isRoot) {
        CompletionItem item = new CompletionItem();
        item.setLabel(packageName);
        item.setKind(CompletionItemKind.Module);
        item.setDetail(packageName);
//        item.setData();
        return item;
    }

    private CompletionItem makeFunction(FunctionSymbol function, boolean addParens) {
        CompletionItem item = new CompletionItem();

        // build label
        StringBuilder labelBuilder = new StringBuilder();
        labelBuilder.append(function.getName()).append("(");

        List<Type> paramTypes = function.getType().paramTypes;
        List<String> paramNames = function.getParamNames();
        for (int i = 0; i < paramTypes.size(); i++) {
            Type paramType = paramTypes.get(i);
            String paramName = paramNames.get(i);
            labelBuilder.append(paramName).append(" as ").append(paramType.toString());
            if (i < paramTypes.size() - 1) {
                labelBuilder.append(", ");
            }
        }
        labelBuilder.append(")");
        item.setLabel(labelBuilder.toString());
        item.setKind(CompletionItemKind.Function);
        item.setInsertText(function.getName());
        item.setFilterText(function.getName());
        item.setDetail(function.getReturnType() + " " + function);
//        item.setData();
        if (addParens) {
            if (paramTypes.isEmpty()) {
                item.setInsertText(function.getName() + "()$0");
            } else {
                StringBuilder insertTextBuilder = new StringBuilder();
                insertTextBuilder.append(function.getName()).append("(");

                for (int i = 0; i < paramTypes.size(); i++) {
                    String paramName = paramNames.get(i);
                    insertTextBuilder.append("${").append(i + 1).append(":")
                        .append(paramName)
                        .append("}");
                    if (i < paramNames.size() - 1) {
                        insertTextBuilder.append(", ");
                    }
                }
                insertTextBuilder.append(")$0");
                item.setInsertText(insertTextBuilder.toString());
                // Activate signatureHelp
                // see https://github.com/microsoft/vscode/issues/78806
                Command command = new Command();
                item.setCommand(command);
                command.setCommand("editor.action.triggerParameterHints");
                command.setTitle("Trigger Parameter Hints");
            }
            item.setInsertTextFormat(InsertTextFormat.Snippet);
        }
        return item;
    }

    // grouping overloads
    private CompletionItem makeFunctions(List<FunctionSymbol> overloads, boolean addParens) {
        FunctionSymbol first = overloads.get(0);
        CompletionItem item = new CompletionItem();
        item.setLabel(first.getName());
        item.setKind(CompletionItemKind.Function);
        item.setDetail(first.getReturnType() + " " + first);
//        item.setData();
        if (addParens) {
            List<Type> paramTypes = first.getType().paramTypes;
            if (overloads.size() == 1 && paramTypes.isEmpty()) {
                item.setInsertText(first.getName() + "()$0");
            } else {
                item.setInsertText(first.getName() + "($0)");
                // Activate signatureHelp
                // see https://github.com/microsoft/vscode/issues/78806
                Command command = new Command();
                item.setCommand(command);
                command.setCommand("editor.action.triggerParameterHints");
                command.setTitle("Trigger Parameter Hints");
            }
            item.setInsertTextFormat(InsertTextFormat.Snippet);
        }
        return item;
    }


}
