package io.tals.flux4j.apt;

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
    public static final String CONSTRUCTOR_PARAM_PREFIX = "param_";

    public static void writeDispatcher(Model.Dispatcher dispatcher, boolean hasInject, boolean hasSingleton, Filer filer) throws IOException {
        Model.DispatcherImplementation implementation = dispatcher.implementation();
        JavaFileObject sourceFile = filer.createSourceFile(implementation.fullyQualifiedName(), implementation.dispatcherTypeElement());
        PrintWriter _writer = new PrintWriter(sourceFile.openWriter());
        try {
            IndentPrinter writer = new IndentPrinter(_writer, INDENT);
            writer.println("package " + implementation.packageName() + ";");
            writer.println(String.format("@javax.annotation.Generated(\"%s\")", DispatcherProcessor.class.getCanonicalName()));
            if (hasSingleton) {
                writer.println("@javax.inject.Singleton");
            }

            List<String> implementTypes = new ArrayList<String>();
            String extendsType = null;

            if (!dispatcher.isInterface()) {
                extendsType = dispatcher.className();
            } else {
                implementTypes.add(dispatcher.className());
            }

            writer.println(String.format("public final class %s%s%s {", implementation.className(),
                    extendsType == null ? "" : " extends " + extendsType,
                    implementTypes.isEmpty() ? "" : " implements " + Joiner.on(", ").join(implementTypes)
            ));
            writer.indent();
            {
                writer.println();
                writer.println("private boolean dispatching = false;");

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
                if (hasInject) {
                    writer.println("@javax.inject.Inject");
                }
                writer.print("public ").print(implementation.className()).print("(");
                writer.println().indent().indent();
                List<String> constructorParameters = new ArrayList<String>();
                for (Model.MethodParameter constructorParam : dispatcher.constructorParementers()) {
                    constructorParameters.add(constructorParam.asDeclarationInJavaCode(CONSTRUCTOR_PARAM_PREFIX));
                }

                for (Model.Store store : dispatcher.stores()) {
                    constructorParameters.add(String.format("%s store%d", store.fullyQualifiedName(), store.id()));
                }

                for (int i = 0; i < constructorParameters.size(); i++) {
                    String parameter = constructorParameters.get(i);
                    writer.print(parameter);
                    if (i != constructorParameters.size() - 1) writer.print(",");
                    writer.println();
                }

                writer.outdent().println(") {").indent();

                // if base class had constructor, we should call is
                if (dispatcher.hasConstructor()) {
                    writer.print("super(").indent();
                    boolean first = true;
                    for (Model.MethodParameter parameter : dispatcher.constructorParementers()) {
                        if (!first) writer.print(",");
                        else writer.println();
                        first = false;
                        writer.println(parameter.asNameInJavaCode(CONSTRUCTOR_PARAM_PREFIX));
                    }
                    writer.outdent().println(");");
                }

                for (Model.Store store : dispatcher.stores()) {
                    writer.println(String.format("this.store%1$d = store%1$d;", store.id()));
                }
                writer.outdent().println("}");

                // boolean isDispatching() member
                writer.println();
//                writer.println("@Override");
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
                    writer.println("if (dispatching) {").indent();
                    if (dispatcher.hasDispatchInProgressHandler()) {
                        writer.print(Model.Dispatcher.DISPATCH_IN_PROGRESS_HANDLER_NAME).println("();");
                        writer.println("return;");
                    } else {
                        writer.println("throw new RuntimeException(\"Dispatch already in progress\");");
                    }
                    writer.outdent().println("}");
                    writer.println("dispatching = true;");
                    writer.println();
                    writer.println("try {").indent();

                    if (dispatcher.hasDispatchStartedHandler()) {
                        writer.print(Model.Dispatcher.DISPATCH_STARTED_HANDLER_NAME).println("();");
                    }

                    for (Model.HandlerMethod handler : dispatcher.findStoreHandlersForAction(action)) {
                        // create string of dependencies to be passed to the handling method
                        StringBuilder dependencies = new StringBuilder();
                        for (Model.Store store : handler.dependencies()) {
                            dependencies.append(", ").append("store").append(store.id());
                        }

                        if (action.isTyped()) {
                            writer.println(String.format("%s.%s(store%d, action%s);",
                                    handler.store().helperClass().fullyQualifiedName(),
                                    handler.name(), handler.store().id(), dependencies
                            ));
                        } else {
                            writer.println(String.format("%s.%s(store%d%s);",
                                    handler.store().helperClass().fullyQualifiedName(),
                                    handler.name(), handler.store().id(), dependencies
                            ));
                        }
                    }
                    if (dispatcher.hasErrorHandler()) {
                        writer.outdent().println("} catch (Throwable exception) {").indent();
                        writer.print(Model.Dispatcher.DISPATCH_ERROR_HANDLER_NAME).print("(exception);").println();
                    }
                    writer.outdent().println("} finally {").indent();
                    writer.println("dispatching = false;");
                    if (dispatcher.hasDispatchEndedHandler()) {
                        writer.print(Model.Dispatcher.DISPATCH_ENDED_HANDLER_NAME).println("();");
                    }
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
        JavaFileObject sourceFile;
        try {
            sourceFile = filer.createSourceFile(storeHelper.fullyQualifiedName(), store.typeElement());
        } catch (Exception ignored) {
            return;
        }
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

        // pass in all dependencies
        StringBuilder signatureSb = new StringBuilder();
        StringBuilder callSb = new StringBuilder();
        for (Model.Store store : method.dependencies()) {
            signatureSb.append(", ");
            if (callSb.length() > 0 || action.isTyped()) callSb.append(", ");
            signatureSb.append(store.className()).append(" ").append("store").append(store.id());
            callSb.append("store").append(store.id());
        }


        if (action.isTyped()) {
            writer.println(String.format("public static void %s(%s store, %s action%s) {", method.name(),
                    method.store().fullyQualifiedName(), action.type(), signatureSb));
        } else {
            writer.println(String.format("public static void %s(%s store) {", method.name(),
                    method.store().fullyQualifiedName()));
        }
        writer.indent();
        writer.println(String.format("store.%s(%s%s);", method.name(),
                action.isTyped() ? "action" : "", callSb));
        writer.outdent();
        writer.println("}");
    }
}
