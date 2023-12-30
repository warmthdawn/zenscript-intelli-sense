package raylras.zen.lsp.diagonsitc;

import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.services.LanguageClient;
import raylras.zen.model.CompilationEnvironment;
import raylras.zen.model.CompilationUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

public class Diagnostics {

    private static final WeakHashMap<CompilationUnit, List<ZenDiagnostic>> storage = new WeakHashMap<>();

    private static List<ZenDiagnostic> getDiagnostics(CompilationUnit unit) {
        return storage.computeIfAbsent(unit, u -> new ArrayList<>());
    }

    public static void clear(CompilationUnit cu, LanguageClient client) {
        List<ZenDiagnostic> diagnostics = getDiagnostics(cu);
        diagnostics.clear();
        publishDiagnostics(cu, diagnostics, client);
    }

    public static void checkSyntax(CompilationEnvironment env, LanguageClient client) {

        for (CompilationUnit unit : env.getUnits()) {
            checkSyntax(unit, client);
        }
    }

    public static void checkSyntax(CompilationUnit cu, LanguageClient client) {

        List<ZenDiagnostic> diagnostics = getDiagnostics(cu);
        CompletableFuture.runAsync(() -> {
            if(SyntaxDiagnostic.checkSyntax(cu, diagnostics)) {
                publishDiagnostics(cu, diagnostics, client);
            }

        });

    }


    public static void publishDiagnostics(CompilationUnit cu, List<ZenDiagnostic> diagnostics, LanguageClient client) {

        PublishDiagnosticsParams params = new PublishDiagnosticsParams();

        params.setDiagnostics(diagnostics.stream().map(ZenDiagnostic::toLSPDiagnostic).toList());
        params.setUri(cu.getPath().toUri().toString());

        client.publishDiagnostics(params);

    }
}
