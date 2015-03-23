package io.tals.flux4j.apt;

import com.squareup.javapoet.*;

import javax.annotation.Generated;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;

/**
 * @author Tal Shani
 */
class StoreHelperJavaBuilder {

    public static void writeStoreHelper(Model.Store store, Filer filer) throws IOException {
        Model.StoreHelper storeHelper = store.helperClass();
        JavaFileObject sourceFile;
        try {
            sourceFile = filer.createSourceFile(storeHelper.fullyQualifiedName(), store.typeElement());
        } catch (Exception ignored) {
            return;
        }
        Writer writer = sourceFile.openWriter();
        try {
            TypeSpec.Builder typeSpec = TypeSpec.classBuilder(storeHelper.className());

            typeSpec.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
            typeSpec.addAnnotation(AnnotationSpec.builder(Generated.class)
                    .addMember("value", "$S", DispatcherProcessor.class.getCanonicalName())
                    .build());

            for (Model.HandlerMethod handlerMethod : store.handlerMethods()) {
                typeSpec.addMethod(createActionHandlerMethod(handlerMethod));
            }

            JavaFile javaFile = JavaFile.builder(storeHelper.packageName(), typeSpec.build()).build();
            javaFile.writeTo(writer);
        } finally {
            writer.close();
        }
    }

    private static MethodSpec createActionHandlerMethod(Model.HandlerMethod method) {
        Model.Action action = method.action();
        TypeMirror actionType = action.type();

        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.name())
                .returns(TypeName.BOOLEAN)
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC);

        builder.addParameter(TypeName.get(method.store().typeElement().asType()), "store");
        StringBuilder invokeParams = new StringBuilder();
        if (actionType != null) {
            builder.addParameter(TypeName.get(actionType), "action");
            invokeParams.append("action");
        }
        for (Model.Store store : method.dependencies()) {
            builder.addParameter(TypeName.get(store.typeElement().asType()), "store" + store.id());
            if (invokeParams.length() > 0) invokeParams.append(", ");
            invokeParams.append("store").append(store.id());
        }
        if (method.isReturningBoolean()) {
            builder.addStatement("return store.$L($L)", method.name(), invokeParams.toString());
        } else {
            builder.addStatement("store.$L($L)", method.name(), invokeParams.toString());
            builder.addStatement("return false");
        }

        return builder.build();
    }

}
