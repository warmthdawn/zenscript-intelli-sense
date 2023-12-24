package raylras.zen.lsp.diagonsitc;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DiagnosticTag;
import raylras.zen.util.Range;

import java.util.ArrayList;
import java.util.Collections;

public class ZenDiagnostic {

    private String msg;

    private Range range;

    private Type type;
    private Severity severity;


    public ZenDiagnostic(String msg, Range range, Type type, Severity severity) {
        this.msg = msg;
        this.range = range;
        this.type = type;
        this.severity = severity;
    }

    public enum Type {
        SyntaxError,

    }


    public enum Severity {
        Error,
        Warning,
        Hint,
        Info,
        Useless,
        Deprecated
    }


    public Diagnostic toLSPDiagnostic() {
        Diagnostic diagnostic = new Diagnostic();

        diagnostic.setRange(this.range.toLspRange());
        diagnostic.setMessage(this.msg);
        DiagnosticSeverity severity;
        switch (this.severity) {
            case Error -> severity = DiagnosticSeverity.Error;
            case Warning, Useless, Deprecated -> severity = DiagnosticSeverity.Warning;
            case Hint -> severity = DiagnosticSeverity.Hint;
            default -> severity = DiagnosticSeverity.Information;
        }
        diagnostic.setSeverity(severity);

        if (this.severity == Severity.Useless) {
            diagnostic.setTags(Collections.singletonList(DiagnosticTag.Unnecessary));
        } else if (this.severity == Severity.Deprecated) {
            diagnostic.setTags(Collections.singletonList(DiagnosticTag.Deprecated));
        }

        return diagnostic;
    }
}
