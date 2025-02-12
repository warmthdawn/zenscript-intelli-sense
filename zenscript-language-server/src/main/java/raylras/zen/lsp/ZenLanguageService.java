package raylras.zen.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raylras.zen.lsp.provider.*;
import raylras.zen.model.CompilationUnit;
import raylras.zen.model.Compilations;
import raylras.zen.model.Document;
import raylras.zen.util.PathUtil;
import raylras.zen.util.Watcher;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ZenLanguageService implements TextDocumentService, WorkspaceService {

    private static final Logger logger = LoggerFactory.getLogger(ZenLanguageService.class);

    final WorkspaceManager manager;

    public ZenLanguageService(WorkspaceManager manager) {
        this.manager = manager;
    }

    public void initializeWorkspaces(List<WorkspaceFolder> workspaces) {
        if (workspaces != null) {
            workspaces.forEach(workspace -> {
                manager.addWorkspace(workspace);
                logger.info("{}", workspace);
            });
        }
    }

    private <T> CompletableFuture<T> emptyFuture() {
        return CompletableFuture.completedFuture(null);
    }

    /* Text Document Service */

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        try {
            Path path = PathUtil.toPath(params.getTextDocument().getUri());
            var watcher = Watcher.watch(() -> manager.createEnvIfNotExists(path));
            logger.info("didOpen {} [{}]", path.getFileName(), watcher.getElapsedMillis());
        } catch (Exception e) {
            logger.error("didOpen {}", params, e);
        }
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        try (Document doc = manager.openAsWrite(params.getTextDocument())) {
            doc.getUnit().ifPresent(unit -> {
                var watcher = Watcher.watch(() -> {
                    String source = params.getContentChanges().get(0).getText();
                    Compilations.load(unit, source);
                });
                logger.trace("didChange {} [{}]", unit.getPath().getFileName(), watcher.getElapsedMillis());
            });
        } catch (Exception e) {
            logger.error("didChange {}", params, e);
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
        try (Document doc = manager.openAsRead(params.getTextDocument())) {
            return CompletableFuture.supplyAsync(() -> doc.getUnit().flatMap(unit -> {
                var watcher = Watcher.watch(() -> CompletionProvider.completion(unit, params));
                if (watcher.isResultPresent()) {
                    int line = params.getPosition().getLine() + 1;
                    int column = params.getPosition().getCharacter();
                    logger.info("completion {} at ({},{}) [{}]", unit.getPath().getFileName(), line, column, watcher.getElapsedMillis());
                }
                return watcher.getResult();
            }).orElse(null));
        } catch (Exception e) {
            logger.error("completion {}", params, e);
            return emptyFuture();
        }
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
        return emptyFuture();
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        try (Document doc = manager.openAsRead(params.getTextDocument())) {
            return CompletableFuture.supplyAsync(() -> doc.getUnit().flatMap(unit -> {
                var watcher = Watcher.watch(() -> HoverProvider.hover(unit, params));
                if (watcher.isResultPresent()) {
                    int line = params.getPosition().getLine() + 1;
                    int column = params.getPosition().getCharacter();
                    logger.info("hover {} at ({},{}) [{}]", unit.getPath().getFileName(), line, column, watcher.getElapsedMillis());
                }
                return watcher.getResult();
            }).orElse(null));
        } catch (Exception e) {
            logger.error("hover {}", params, e);
            return emptyFuture();
        }
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams params) {
        try (Document doc = manager.openAsRead(params.getTextDocument())) {
            return CompletableFuture.supplyAsync(() -> doc.getUnit().flatMap(unit -> {
                var watcher = Watcher.watch(() -> DefinitionProvider.definition(unit, params));
                if (watcher.isResultPresent()) {
                    int line = params.getPosition().getLine() + 1;
                    int column = params.getPosition().getCharacter();
                    logger.info("definition {} at ({},{}) [{}]", unit.getPath().getFileName(), line, column, watcher.getElapsedMillis());
                }
                return watcher.getResult();
            }).orElse(null));
        } catch (Exception e) {
            logger.error("definition {}", params, e);
            return emptyFuture();
        }
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
        try (Document doc = manager.openAsRead(params.getTextDocument())) {
            return CompletableFuture.supplyAsync(() -> doc.getUnit().flatMap(unit -> {
                var watcher = Watcher.watch(() -> ReferencesProvider.references(unit, params));
                if (watcher.isResultPresent()) {
                    int line = params.getPosition().getLine() + 1;
                    int column = params.getPosition().getCharacter();
                    logger.info("references {} at ({},{}) [{}]", unit.getPath().getFileName(), line, column, watcher.getElapsedMillis());
                }
                return watcher.getResult();
            }).orElse(null));
        } catch (Exception e) {
            logger.error("references {}", params, e);
            return emptyFuture();
        }
    }

    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
        try (Document doc = manager.openAsRead(params.getTextDocument())) {
            return CompletableFuture.supplyAsync(() -> doc.getUnit().flatMap(unit -> {
                var watcher = Watcher.watch(() -> DocumentSymbolProvider.documentSymbol(unit, params));
                if (watcher.isResultPresent()) {
                    logger.info("documentSymbol {} [{}]", unit.getPath().getFileName(), watcher.getElapsedMillis());
                }
                return watcher.getResult();
            }).orElse(null));
        } catch (Exception e) {
            logger.error("documentSymbol {}", params, e);
            return emptyFuture();
        }
    }

    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
        try (Document doc = manager.openAsRead(params.getTextDocument())) {
            return CompletableFuture.supplyAsync(() -> doc.getUnit().flatMap(unit -> {
                var watcher = Watcher.watch(() -> SemanticTokensProvider.semanticTokensFull(unit, params));
                if (watcher.isResultPresent()) {
                    logger.info("semanticTokensFull {} [{}]", unit.getPath().getFileName(), watcher.getElapsedMillis());
                }
                return watcher.getResult();
            }).orElse(null));
        } catch (Exception e) {
            logger.error("semanticTokensFull {}", params, e);
            return emptyFuture();
        }
    }

    /* End Text Document Service */

    /* Workspace Service */

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        var watcher = Watcher.watch(() -> {
            for (FileEvent event : params.getChanges()) {
                try {
                    Path documentPath = PathUtil.toPath(event.getUri());
                    manager.createEnvIfNotExists(documentPath);
                    manager.getEnv(documentPath).ifPresent(env -> {
                        switch (event.getType()) {
                            case Created -> {
                                CompilationUnit unit = env.createUnit(documentPath);
                                Compilations.load(unit);
                            }
                            case Changed -> {
                                CompilationUnit unit = env.getUnit(documentPath);
                                Compilations.load(unit);
                            }
                            case Deleted -> {
                                env.removeUnit(documentPath);
                            }
                        }
                    });
                } catch (Exception e) {
                    logger.error("didChangeWatchedFiles {}", event, e);
                }
            }
        });
        logger.info("didChangeWatchedFiles [{}]", watcher.getElapsedMillis());
    }

    @Override
    public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
        params.getEvent().getRemoved().forEach(workspace -> {
            manager.removeWorkspace(workspace);
            logger.info("Removed Workspace folder: {}", workspace);
        });
        params.getEvent().getAdded().forEach(workspace -> {
            manager.addWorkspace(workspace);
            logger.info("Added Workspace folder: {}", workspace);
        });
    }

    /* End Workspace Service */

}
