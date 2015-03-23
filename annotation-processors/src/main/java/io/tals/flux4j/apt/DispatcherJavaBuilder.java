package io.tals.flux4j.apt;

import com.google.auto.common.MoreElements;
import com.squareup.javapoet.*;
import io.tals.flux4j.shared.StoreChangeBinder;
import io.tals.flux4j.shared.StoreChangeSubscription;
import io.tals.flux4j.shared.StoreChangesManager;

import javax.annotation.Generated;
import javax.annotation.processing.Filer;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * @author Tal Shani
 */
class DispatcherJavaBuilder {
    public static final String CONSTRUCTOR_PARAM_PREFIX = "param_";

    public static void writeDispatcher(Model.Dispatcher dispatcher, boolean hasInject, boolean hasSingleton, Filer filer) throws IOException {
        Model.DispatcherImplementation implementation = dispatcher.implementation();
        JavaFileObject sourceFile = filer.createSourceFile(implementation.fullyQualifiedName(), implementation.dispatcherTypeElement());
        Writer writer = sourceFile.openWriter();
        try {
            TypeSpec.Builder typeSpec = TypeSpec.classBuilder(implementation.className());

            typeSpec.addAnnotation(AnnotationSpec.builder(Generated.class)
                    .addMember("value", "$S", DispatcherProcessor.class.getCanonicalName())
                    .build());

            if (hasSingleton) {
                typeSpec.addAnnotation(ClassName.get("javax.inject", "Singleton"));
            }

            if (!dispatcher.isInterface()) {
                typeSpec.superclass(TypeName.get(dispatcher.typeElement().asType()));
            } else {
                typeSpec.addSuperinterface(TypeName.get(dispatcher.typeElement().asType()));
            }

            // ---------- MEMBERS
            typeSpec.addField(FieldSpec.builder(TypeName.BOOLEAN, "dispatching", Modifier.PRIVATE)
                    .initializer("$L", "false").build());
            for (Model.Store store : dispatcher.stores()) {
                typeSpec.addField(TypeName.get(store.typeElement().asType()), "store" + store.id(), Modifier.PRIVATE, Modifier.FINAL);
            }
            for (Model.Store store : dispatcher.stores()) {
                typeSpec.addField(FieldSpec.builder(TypeName.BOOLEAN, "store" + store.id() + "Changed", Modifier.PRIVATE)
                        .initializer("$L", "false").build());
            }


            typeSpec.addMethod(createDispatcherConstructor(hasInject, dispatcher));

            typeSpec.addMethod(MethodSpec.methodBuilder("isDispatching")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addStatement("return dispatching")
                    .returns(TypeName.BOOLEAN)
                    .build());
            typeSpec.addMethod(createResetChangedStatesMethod(dispatcher));
            for (Model.DispatchingMethod method : dispatcher.dispatchingMethods()) {
                typeSpec.addMethod(createDispatchingMethod(method));
            }


            typeSpec.addField(FieldSpec.builder(TypeName.get(StoreChangesManager.class), "storeChangesManager",
                    Modifier.FINAL, Modifier.PRIVATE)
                    .initializer("new $T()", TypeName.get(StoreChangesManager.class))
                    .build());
            typeSpec.addMethod(
                    MethodSpec.methodBuilder("bind")
                            .returns(StoreChangeSubscription.class)
                            .addAnnotation(Override.class)
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .addTypeVariable(TypeVariableName.get("T"))
                            .addParameter(
                                    ParameterizedTypeName.get(ClassName.get(StoreChangeBinder.class), TypeVariableName.get("T")),
                                    "binder"
                            )
                            .addParameter(
                                    TypeVariableName.get("T"),
                                    "component"
                            )
                            .addStatement("return storeChangesManager.add(binder, component)")
                            .build()
            );


            typeSpec.addMethod(createFireChangeEventsMethod(dispatcher));


            JavaFile javaFile = JavaFile.builder(implementation.packageName(), typeSpec.build()).build();
            javaFile.writeTo(writer);
        } finally {
            writer.close();
        }
    }

    private static MethodSpec createFireChangeEventsMethod(Model.Dispatcher dispatcher) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("fireChangeEvents")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL);

        StringBuilder changed = new StringBuilder();
        StringBuilder stores = new StringBuilder();
        for (Model.Store store : dispatcher.stores()) {
            if(changed.length() > 0) changed.append(", ");
            if(stores.length() > 0) stores.append(", ");
            changed.append("store").append(store.id()).append("Changed");
            stores.append("store").append(store.id());
        }
        builder.addStatement("Boolean[] changed = new Boolean[] {$L}", changed.toString());
        builder.addStatement("Object[] stores = new Object[] {$L}", stores.toString());
        builder.addStatement("storeChangesManager.fire(stores, changed)");

        return builder.build();
    }

    private static MethodSpec createResetChangedStatesMethod(Model.Dispatcher dispatcher) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("resetChangedStates");
        builder.addModifiers(Modifier.PRIVATE, Modifier.FINAL);
        for (Model.Store store : dispatcher.stores()) {
            builder.addStatement("$N = false", "store" + store.id() + "Changed");
        }
        return builder.build();
    }

    private static MethodSpec createDispatcherConstructor(boolean hasInject, Model.Dispatcher dispatcher) {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder();
        builder.addModifiers(Modifier.PUBLIC);
        if (hasInject) {
            builder.addAnnotation(ClassName.get("javax.inject", "Inject"));
        }

        // add constructor params
        String[] superParams = new String[dispatcher.constructorParementers().size()];
        int i = 0;
        for (Model.MethodParameter param : dispatcher.constructorParementers()) {
            ParameterSpec.Builder paramBuilder = ParameterSpec.builder(TypeName.get(param.type()), CONSTRUCTOR_PARAM_PREFIX + param.name());
            for (AnnotationMirror annotationMirror : param.annotations()) {
                Element annotationElement = annotationMirror.getAnnotationType().asElement();
                ClassName className = ClassName.bestGuess(MoreElements.getPackage(annotationElement).getQualifiedName() + "." + annotationElement.getSimpleName().toString());
                AnnotationSpec.Builder annotationBuilder = AnnotationSpec.builder(className);
                Map<? extends ExecutableElement, ? extends AnnotationValue> values = annotationMirror.getElementValues();
                for (ExecutableElement key : values.keySet()) {
                    AnnotationValue annotationValue = values.get(key);
                    annotationBuilder.addMember(key.getSimpleName().toString(), "$L", annotationValue);
                }
                paramBuilder.addAnnotation(annotationBuilder.build());
            }
            builder.addParameter(paramBuilder.build());
            superParams[i++] = CONSTRUCTOR_PARAM_PREFIX + param.name();
        }
        for (Model.Store store : dispatcher.stores()) {
            builder.addParameter(TypeName.get(store.typeElement().asType()), "store" + store.id());
        }

        // if base class had constructor, we should call is
        if (dispatcher.hasConstructor()) {
            builder.addStatement("super($L)", StringUtils.joinParams(superParams));
        }
        for (Model.Store store : dispatcher.stores()) {
            builder.addStatement("this.$N = $N", "store" + store.id(), "store" + store.id());
        }
        return builder.build();
    }

    private static MethodSpec createDispatchingMethod(Model.DispatchingMethod method) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.name());
        builder.addAnnotation(Override.class);
        builder.returns(TypeName.VOID);
        builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        Model.Action action = method.action();
        if (action.isTyped()) {
            TypeMirror actionType = action.type();
            assert actionType != null;
            builder.addParameter(TypeName.get(actionType), "action");
        }
        // guard against dispatching while it is in progress
        // this can happen if while we update a store, it dispatches another action
        builder.beginControlFlow("if ($N)", "dispatching");
        {
            if (method.dispatcher().hasDispatchInProgressHandler()) {
                builder.addStatement("$L()", Model.Dispatcher.DISPATCH_IN_PROGRESS_HANDLER_NAME);
                builder.addStatement("return");
            } else {
                builder.addStatement("throw new RuntimeException($S)", "Dispatch already in progress");
            }
        }
        builder.endControlFlow();
        builder.addStatement("dispatching = true");
        builder.beginControlFlow("try");
        {
            builder.addStatement("resetChangedStates()");
            if (method.dispatcher().hasDispatchStartedHandler()) {
                builder.addStatement("$L()", Model.Dispatcher.DISPATCH_STARTED_HANDLER_NAME);
            }

            for (Model.HandlerMethod handler : method.dispatcher().findStoreHandlersForAction(action)) {
                // create string of dependencies to be passed to the handling method
                StringBuilder parameters = new StringBuilder();
                if (action.isTyped()) {
                    parameters.append(", action");
                }
                for (Model.Store store : handler.dependencies()) {
                    parameters.append(", ").append("store").append(store.id());
                }

                builder.addStatement("$N = $T.$L($N$L)",
                        "store" + handler.store().id() + "Changed",
                        ClassName.get(handler.store().helperClass().packageName(), handler.store().helperClass().className()),
                        handler.name(), "store" + handler.store().id(), parameters.toString()
                );
            }
        }
        if (method.dispatcher().hasErrorHandler()) {
            builder.endControlFlow().beginControlFlow("catch (Throwable exception)");
            {
                builder.addStatement("$L(exception)", Model.Dispatcher.DISPATCH_ERROR_HANDLER_NAME);
            }
        }
        builder.endControlFlow().beginControlFlow("finally");
        {
            builder.addStatement("dispatching = false");
            if (method.dispatcher().hasDispatchCompletedHandler()) {
                builder.addStatement("$L()", Model.Dispatcher.DISPATCH_COMPLETED_HANDLER_NAME);
            }
        }
        builder.endControlFlow();
        builder.addStatement("fireChangeEvents()");

        return builder.build();
    }
}
