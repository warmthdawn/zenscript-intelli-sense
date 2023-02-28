package raylras.zen.code.tree;

import raylras.zen.code.Range;
import raylras.zen.code.scope.LocalScope;
import raylras.zen.code.symbol.ClassSymbol;
import raylras.zen.code.tree.stmt.VariableDecl;

import java.util.List;

/**
 * Represents a class declaration such as "zenClass name { }".
 * e.g. "zenClass Foo { var foo = null; function bar() { } }".
 */
public class ClassDecl extends TreeNode {

    public Name name;
    public List<VariableDecl> fields;
    public List<ConstructorDecl> constructors;
    public List<FunctionDecl> methods;
    public ClassSymbol symbol;
    public LocalScope localScope;

    public ClassDecl(Name name, List<VariableDecl> fields, List<ConstructorDecl> constructors, List<FunctionDecl> methods, Range range) {
        super(range);
        this.name = name;
        this.constructors = constructors;
        this.fields = fields;
        this.methods = methods;
    }

    @Override
    public <R> R accept(TreeVisitor<R> visitor) {
        return visitor.visitClassDecl(this);
    }

}
