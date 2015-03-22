package io.tals.flux4j.apt;

import com.squareup.javapoet.*;
import io.tals.flux4j.shared.StoreChangeBinder;

import javax.annotation.Generated;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;

/**
 * @author Tal Shani
 */
class ComponentBinderJavaBuilder {

    public static TypeSpec buildComponentBinder(ComponentModel.ComponentClass component) throws IOException {
        ComponentModel.ComponentBinder binder = component.binder();
        TypeSpec.Builder typeSpec = TypeSpec.classBuilder(binder.className());

        typeSpec.addModifiers(Modifier.FINAL);
        typeSpec.addAnnotation(AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", StoreChangeHandlerProcessor.class.getCanonicalName())
                .build());

        typeSpec.addSuperinterface(
                ParameterizedTypeName.get(ClassName.get(StoreChangeBinder.class),
                        ClassName.get(component.packageName(), component.className()))
        );

        // implement the fire method
        MethodSpec.Builder method = MethodSpec.methodBuilder("fireChangeEvent")
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.FINAL)
                .addAnnotation(Override.class);
        method.addParameter(Object.class, "component")
                .addParameter(Object[].class, "allStores")
                .addParameter(Boolean[].class, "changedStores");
        {
            // create local variables
            for (TypeElement typeElement : component.stores()) {
                method.addStatement("$T $L = null", ClassName.get(typeElement), "store" + component.storeId(typeElement));
            }
            for (ComponentModel.StoreChangeHandlerMethod handlerMethod : component.methods()) {
                String eventName = "fireEvent" + component.handlerId(handlerMethod);
                method.addStatement("boolean $L = false", eventName);
            }
            method.beginControlFlow("for (int i = 0; i < allStores.length; i++)");
            {
                method.addStatement("Object store = allStores[i]");
                for (TypeElement typeElement : component.stores()) {
                    method.beginControlFlow("if (store instanceof $T)", ClassName.get(typeElement));
                    method.addStatement("$L = ($T)store", "store" + component.storeId(typeElement), ClassName.get(typeElement));
                    for (ComponentModel.StoreChangeHandlerMethod handlerMethod : component.methods()) {
                        if (handlerMethod.dependsOnStore(typeElement)) {
                            String eventName = "fireEvent" + component.handlerId(handlerMethod);
                            method.addStatement("if(!$L) $L = changedStores[i]", eventName, eventName);
                            method.addStatement("break");
                        }
                    }
                    method.endControlFlow();
                }

            }
            method.endControlFlow();

            ClassName componentClassName = ClassName.get(component.element());
            method.addStatement("$T castedComponent = ($T) component", componentClassName, componentClassName);

            for (ComponentModel.StoreChangeHandlerMethod handlerMethod : component.methods()) {
                StringBuilder conditionCode = new StringBuilder();
                StringBuilder invokeCode = new StringBuilder();
                conditionCode.append("fireEvent").append(component.handlerId(handlerMethod));
                for (ComponentModel.StoreDependency storeDependency : handlerMethod.storesDependencies()) {
                    conditionCode.append(" && ").append("store").append(component.storeId(storeDependency.element())).append(" != null");
                    if (invokeCode.length() > 0) invokeCode.append(", ");
                    invokeCode.append("store").append(component.storeId(storeDependency.element()));
                }
                method.beginControlFlow("if ($L)", conditionCode.toString());
                method.addStatement("castedComponent.$L($L)", handlerMethod.name(), invokeCode.toString());
                method.endControlFlow();

            }


        }

        typeSpec.addMethod(method.build());


        return typeSpec.build();
    }


}
