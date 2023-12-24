package raylras.zen.lsp.diagonsitc;

import ai.serenade.treesitter.Node;
import ai.serenade.treesitter.Tree;
import org.antlr.v4.runtime.tree.ParseTree;
import raylras.zen.model.CompilationUnit;
import raylras.zen.model.Visitor;
import raylras.zen.model.parser.ZenScriptParser;
import raylras.zen.util.Position;
import raylras.zen.util.Range;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class SyntaxDiagnostic {

    public static boolean checkSyntax(CompilationUnit cu, List<ZenDiagnostic> diagnostics) {
        List<ZenDiagnostic> result = new ArrayList<>();
        SyntaxVisitor visitor = new SyntaxVisitor(cu.isGenerated(), result);
        visitor.visit(cu.getParseTree());
        if(result.isEmpty()) {
            return false;
        }
        diagnostics.addAll(result);
        return true;
    }

    private static void checkByTSTree(List<ZenDiagnostic> result, Tree tree) {

//        ArrayDeque<Node> queue = new ArrayDeque<>();
//
//        queue.push(tree.getRootNode());
//
//        while (!queue.isEmpty()) {
//            Node node = queue.pop();
//            if(!node.hasError()) {
//                continue;
//            }
//            if(node.isError()) {
//
//                result.add(new ZenDiagnostic("err", Range.of(0,0,0,0), ZenDiagnostic.Type.SyntaxError, ZenDiagnostic.Severity.Error));
//            }
//
//
//            for(int i=0;i<node.getChildCount();i++) {
//                queue.push(node.getChild(i));
//            }
//        }
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
            if(ctx.qualifiedName() == null) {
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
