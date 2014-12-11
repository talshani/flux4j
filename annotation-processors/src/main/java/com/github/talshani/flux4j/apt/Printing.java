package com.github.talshani.flux4j.apt;

import com.github.talshani.flux4j.shared.Dispatcher;
import com.github.talshani.flux4j.shared.FluxConfig;
import com.google.common.base.Joiner;

import javax.annotation.processing.Filer;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tal Shani
 */
final class Printing {
    private static final String INDENT = "    ";
    private static final String injectAnnotation = null;

    public static void writeDispatcher(Model.Dispatcher dispatcher, Filer filer) throws IOException {
        Model.DispatcherImplementation implementation = dispatcher.implementation();
        JavaFileObject sourceFile = filer.createSourceFile(implementation.fullyQualifiedName(), implementation.dispatcherTypeElement());
        PrintWriter _writer = new PrintWriter(sourceFile.openWriter());
        try {
            IndentPrinter writer = new IndentPrinter(_writer, INDENT);
            writer.println("package " + implementation.packageName() + ";");
            writer.println(String.format("@javax.annotation.Generated(\"%s\")", DispatcherProcessor.class.getCanonicalName()));

            List<String> implementTypes = new ArrayList<String>();
            String extendsType = null;
            implementTypes.add(Dispatcher.class.getCanonicalName());

            if (!dispatcher.isInterface()) {
                extendsType = dispatcher.className();
            } else {
                implementTypes.add(dispatcher.className());
            }

            writer.println(String.format("public final class %s%s implements %s {", implementation.className(),
                    extendsType == null ? "" : " extends " + extendsType,
                    Joiner.on(", ").join(implementTypes)
            ));
            writer.indent();
            {
                writer.println();
                writer.println("private boolean dispatching = false;");
                writer.println(String.format("private final %s config;", FluxConfig.class.getCanonicalName()));

                writer.println();
                // store members
                for (Model.Store store : dispatcher.stores()) {
                    writer.println(String.format("private final %s store%d;", store.fullyQualifiedName(), store.id()));
                }
                writer.println();

                if (injectAnnotation != null) {
                    writer.println("@" + injectAnnotation);
                }

                // constructor
                writer.print("public ").print(implementation.className()).print("(");
                writer.println().indent().indent().println(String.format("%s config", FluxConfig.class.getCanonicalName()));
                for (Model.Store store : dispatcher.stores()) {
                    writer.println(String.format(",%s store%d", store.fullyQualifiedName(), store.id()));
                }
                writer.outdent().println(") {").indent();
                writer.println("this.config = config;");
                for (Model.Store store : dispatcher.stores()) {
                    writer.println(String.format("this.store%1$d = store%1$d;", store.id()));
                }
                writer.outdent().println("}");

                // boolean isDispatching() member
                writer.println();
                writer.println("@Override");
                writer.println("public boolean isDispatching() {");
                writer.indent().println("return dispatching;").outdent();
                writer.println("}");
            }
            writer.println();
            // implement dispatching methods
            for (Model.DispatchingMethod dispatchingMethod : dispatcher.dispatchingMethods()) {
                Model.Action action = dispatchingMethod.action();
                boolean isTyped = action.isTyped();
                writer.println("@Override");
                if (isTyped) {
                    writer.println(String.format("public void %s(%s action) {", dispatchingMethod.name(), action.type()));
                } else {
                    writer.println(String.format("public void %s() {", dispatchingMethod.name()));
                }
                writer.indent();
                {
                    // guard against dispatching while it is in progress
                    // this can happen if while we update a store, it dispatches another action
                    writer.println("if (dispatching) {");
                    writer.indent().println("throw new RuntimeException(config.dispatchingInProgressError());").outdent();
                    writer.println("}");
                    writer.println("dispatching = true;");
                    writer.println();
                    writer.println("try {").indent();

                    for (Model.HandlerMethod handler : dispatcher.findStoreHandlersForAction(action)) {
                        if (action.isTyped()) {
                            writer.println(String.format("%s.%s(store%d, action);",
                                    handler.store().helperClass().fullyQualifiedName(),
                                    handler.name(), handler.store().id()));
                        } else {
                            writer.println(String.format("%s.%s(store%d);",
                                    handler.store().helperClass().fullyQualifiedName(),
                                    handler.name(), handler.store().id()));
                        }
                    }

                    writer.outdent().println("} finally {").indent();
                    writer.println("dispatching = false;");
                    writer.outdent().println("}");
                }
                writer.outdent();
                writer.println("}");
                writer.println();
            }
            writer.outdent();
            writer.println("}");
        } finally {
            _writer.close();
        }
    }

    public static void writeStore(Model.Store store, Filer filer) throws IOException {
        Model.StoreHelper storeHelper = store.helperClass();
        JavaFileObject sourceFile = filer.createSourceFile(storeHelper.fullyQualifiedName(), store.typeElement());
        PrintWriter _writer = new PrintWriter(sourceFile.openWriter());
        try {
            IndentPrinter writer = new IndentPrinter(_writer, INDENT);
            writer.println("package " + storeHelper.packageName() + ";");
            writer.println(String.format("@javax.annotation.Generated(\"%s\")", DispatcherProcessor.class.getCanonicalName()));
            writer.println(String.format("public final class %s {", storeHelper.className()));
            writer.indent();
            {
                for (Model.HandlerMethod handlerMethod : store.handlerMethods()) {
                    writeActionHandleMethod(handlerMethod, writer);
                }
            }
            writer.outdent();
            writer.println("}");
        } finally {
            _writer.close();
        }
    }

    private static void writeActionHandleMethod(Model.HandlerMethod method, IndentPrinter writer) throws IOException {
        Model.Action action = method.action();
        if (action.isTyped()) {
            writer.println(String.format("public static void %s(%s store, %s action) {", method.name(),
                    method.store().fullyQualifiedName(), action.type()));
        } else {
            writer.println(String.format("public static void %s(%s store) {", method.name(),
                    method.store().fullyQualifiedName()));
        }
        writer.indent();
        writer.println(String.format("store.%s(%s);", method.name(),
                action.isTyped() ? "action" : ""));
        writer.outdent();
        writer.println("}");
    }
}
