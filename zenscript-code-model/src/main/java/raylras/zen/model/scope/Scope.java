package raylras.zen.model.scope;

import org.antlr.v4.runtime.tree.ParseTree;
import raylras.zen.model.symbol.SymbolProvider;
import raylras.zen.model.symbol.Symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Scope implements SymbolProvider {

    private final Scope parent;
    private final ParseTree cst;
    private final List<Symbol> symbols = new ArrayList<>();

    public Scope(Scope parent, ParseTree cst) {
        this.parent = parent;
        this.cst = cst;
    }

    public void addSymbol(Symbol symbol) {
        symbols.add(symbol);
    }

    public void removeSymbol(Symbol symbol) {
        symbols.remove(symbol);
    }

    public Symbol lookupSymbol(String simpleName) {
        return lookupSymbol(simpleName, Symbol.class);
    }

    public <T extends Symbol> T lookupSymbol(String simpleName, Class<T> clazz) {
        Scope scope = this;
        while (scope != null) {
            for (Symbol symbol : scope.getSymbols()) {
                if (clazz.isInstance(symbol)
                        && Objects.equals(symbol.getName(), simpleName)) {
                    return clazz.cast(symbol);
                }
            }
            scope = scope.parent;
        }
        return null;
    }

    public Scope getParent() {
        return parent;
    }

    @Override
    public List<Symbol> getSymbols() {
        return symbols;
    }

    public ParseTree getCst() {
        return cst;
    }

}
