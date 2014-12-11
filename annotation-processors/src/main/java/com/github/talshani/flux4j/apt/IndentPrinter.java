package com.github.talshani.flux4j.apt;

import java.io.PrintWriter;

/**
 * @author Tal Shani
 */
final class IndentPrinter {

    private int indentLevel;
    private final String indent;
    private final PrintWriter out;
    private boolean lineEnded = true;

    public IndentPrinter(PrintWriter out) {
        this(out, "  ");
    }

    public IndentPrinter(PrintWriter out, String indent) {
        this.out = out;
        this.indent = indent;
    }

    public IndentPrinter println(Object value) {
        print(value.toString());
        println();
        return this;
    }

    public IndentPrinter println(String text) {
        print(text);
        println();
        return this;
    }

    public IndentPrinter print(String text) {
        if(lineEnded) printIndent();
        out.print(text);
        lineEnded = false;
        return this;
    }

    public IndentPrinter print(Object object) {
        if(lineEnded) printIndent();
        out.print(object);
        lineEnded = false;
        return this;
    }

    public IndentPrinter printIndent() {
        for (int i = 0; i < indentLevel; i++) {
            out.print(indent);
        }
        return this;
    }

    public IndentPrinter println() {
        out.println();
        lineEnded = true;
        return this;
    }

    public IndentPrinter indent() {
        ++indentLevel;
        return this;
    }

    public IndentPrinter outdent() {
        --indentLevel;
        return this;
    }

    public int getIndentLevel() {
        return indentLevel;
    }

    public void setIndentLevel(int indentLevel) {
        this.indentLevel = indentLevel;
    }
}
