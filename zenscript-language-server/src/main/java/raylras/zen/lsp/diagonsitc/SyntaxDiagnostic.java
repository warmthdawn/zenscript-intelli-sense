package raylras.zen.lsp.diagonsitc;

import org.antlr.v4.runtime.tree.ParseTree;
import raylas.zen.treesitter.Node;
import raylas.zen.treesitter.Tree;
import raylas.zen.treesitter.TreeCursor;
import raylras.zen.model.CompilationUnit;
import raylras.zen.model.Visitor;
import raylras.zen.model.parser.ZenScriptParser;
import raylras.zen.util.Position;
import raylras.zen.util.Range;

import java.util.ArrayList;
import java.util.List;

public class SyntaxDiagnostic {

    public static boolean checkSyntax(CompilationUnit cu, List<ZenDiagnostic> diagnostics) {
        List<ZenDiagnostic> result = new ArrayList<>();
        SyntaxVisitor visitor = new SyntaxVisitor(cu.isGenerated(), result);
        visitor.visit(cu.getParseTree());
        diagnostics.addAll(result);
        result.clear();
        if (cu.isGenerated()) {
            return !result.isEmpty();
        }
        checkByTSTree(result, cu.getTsParseTree());
        diagnostics.addAll(result);
        return !result.isEmpty();
    }

    private static void iterateChild(List<ZenDiagnostic> result, TreeCursor cursor) {
        String msg = "";
        Node node = cursor.getNode();
        if (node.isError()) {
            msg = "ERROR";
        } else if (node.isMissing()) {
            msg = "MISSING:" + node.getSymbolName();
        }
        if (!msg.isEmpty()) {
            result.add(new ZenDiagnostic(msg, node.getRange(), ZenDiagnostic.Type.SyntaxError, ZenDiagnostic.Severity.Error));
        }

        if (!node.getHasError()) {
            return;
        }
        if (!cursor.gotoFirstChild()) {
            return;
        }
        do {
            iterateChild(result, cursor);
        }
        while (cursor.gotoNextSibling());

        cursor.gotoParent();

    }

    private static void checkByTSTree(List<ZenDiagnostic> result, Tree tree) {

        try (TreeCursor cursor = tree.walk()) {

            iterateChild(result, cursor);
        }


    }

    private static class SyntaxVisitor extends Visitor<Void> {
        private final boolean isDzs;
        private final List<ZenDiagnostic> diagnostics;

        private void addError(String msg, Range range) {
            diagnostics.add(new ZenDiagnostic(msg, range, ZenDiagnostic.Type.SyntaxError, ZenDiagnostic.Severity.Error));
        }


        private void missingAfter(String msg, ParseTree node) {
            Position nodeEnd = Range.of(node).end();
            Range range = Range.of(nodeEnd.line(), nodeEnd.column(), nodeEnd.line(), nodeEnd.column() + 1);
            diagnostics.add(new ZenDiagnostic(msg, range, ZenDiagnostic.Type.SyntaxError, ZenDiagnostic.Severity.Error));
        }


        private SyntaxVisitor(boolean isDzs, List<ZenDiagnostic> diagnostics) {
            this.isDzs = isDzs;
            this.diagnostics = diagnostics;
        }

        @Override
        public Void visitImportDeclaration(ZenScriptParser.ImportDeclarationContext ctx) {
            if (ctx.qualifiedName() == null) {
                missingAfter("Identifier expected.", ctx.IMPORT());
            }

//            if(ctx.AS() != null && ctx.alias() == null)

            return super.visitImportDeclaration(ctx);
        }

        @Override
        public Void visitInvaildStatementInClassBody(ZenScriptParser.InvaildStatementInClassBodyContext ctx) {

            Range range = Range.of(ctx);
            String msg = "Invalid statement in class body";
            diagnostics.add(new ZenDiagnostic(msg, range, ZenDiagnostic.Type.SyntaxError, ZenDiagnostic.Severity.Error));

            return null;
        }


    }
}
