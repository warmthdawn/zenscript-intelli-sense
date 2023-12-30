package raylras.zen.model;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import raylas.zen.treesitter.Tree;
import raylras.zen.model.parser.ZenScriptLexer;
import raylras.zen.model.scope.Scope;
import raylras.zen.model.symbol.ImportSymbol;
import raylras.zen.model.symbol.Symbol;
import raylras.zen.util.PathUtil;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class CompilationUnit {

    public static final String ZS_FILE_EXTENSION = ".zs";
    public static final String DZS_FILE_EXTENSION = ".dzs";

    private final Path path;
    private final CompilationEnvironment env;
    private final String qualifiedName;
    private final String simpleName;

    private final List<ImportSymbol> imports = new ArrayList<>();
    private final Map<ParseTree, Scope> scopeMap = new IdentityHashMap<>();
    private final Map<ParseTree, Symbol> symbolMap = new IdentityHashMap<>();

    private ANTLRErrorListener errorListener;

    private CommonTokenStream tokenStream;
    private ParseTree parseTree;

    private Tree tsParseTree;

    public CompilationUnit(Path path, CompilationEnvironment env) {
        this.path = path;
        this.env = env;
        this.qualifiedName = Compilations.extractClassName(env.relativize(path));
        this.simpleName = PathUtil.getFileNameWithoutSuffix(path);
    }

    public List<ImportSymbol> getImports() {
        return Collections.unmodifiableList(imports);
    }

    public void addImport(ImportSymbol importSymbol) {
        imports.add(importSymbol);
    }

    public Optional<Scope> getScope(ParseTree cst) {
        return Optional.ofNullable(scopeMap.get(cst));
    }

    public void addScope(Scope scope) {
        scopeMap.put(scope.getCst(), scope);
    }

    public Optional<Symbol> getSymbol(ParseTree
                                               cst) {
        return Optional.ofNullable(symbolMap.get(cst));
    }

    public <T extends Symbol> Optional<T> getSymbol(ParseTree cst, Class<T> clazz) {
        return getSymbol(cst)
                .filter(clazz::isInstance)
                .map(clazz::cast);
    }

    public void putSymbol(ParseTree cst, Symbol symbol) {
        symbolMap.put(cst, symbol);
    }

    public Collection<Scope> getScopes() {
        return scopeMap.values();
    }

    public Collection<Symbol> getSymbols() {
        return symbolMap.values();
    }

    public List<Symbol> getTopLevelSymbols() {
        return getScope(parseTree)
                .map(Scope::getSymbols)
                .orElseGet(Collections::emptyList);
    }

    public List<Preprocessor> getPreprocessors() {
        List<Token> tokens = tokenStream.getHiddenTokensToRight(0, ZenScriptLexer.PREPROCESSOR_CHANNEL);
        return tokens.stream()
                .map(Token::getText)
                .map(Preprocessor::create)
                .collect(Collectors.toList());
    }

    public Path getPath() {
        return path;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public CompilationEnvironment getEnv() {
        return env;
    }

    public ParseTree getParseTree() {
        return parseTree;
    }

    public void setParseTree(ParseTree parseTree) {
        this.parseTree = parseTree;
    }

    public Tree getTsParseTree() {
        return tsParseTree;
    }

    public void setTsParseTree(Tree tsParseTree) {
        this.tsParseTree = tsParseTree;
    }

    public CommonTokenStream getTokenStream() {
        return tokenStream;
    }

    public void setTokenStream(CommonTokenStream tokenStream) {
        this.tokenStream = tokenStream;
    }

    public ANTLRErrorListener getErrorListener() {
        return errorListener;
    }

    public void setErrorListener(ANTLRErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    public void accept(Visitor<?> visitor) {
        visitor.visit(parseTree);
    }

    public void accept(Listener listener) {
        ParseTreeWalker.DEFAULT.walk(listener, parseTree);
    }

    public boolean isGenerated() {
        return Compilations.isDzsFile(path);
    }

    public void clear() {
        imports.clear();
        scopeMap.clear();
        symbolMap.clear();
        tokenStream = null;
        parseTree = null;
    }

    @Override
    public String toString() {
        return String.valueOf(path);
    }

}
