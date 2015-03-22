package io.tals.flux4j.apt;

import com.google.auto.common.MoreElements;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import io.tals.flux4j.shared.StoreChangeHandler;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * @author Tal Shani
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("io.tals.flux4j.shared.StoreChangeHandler")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public final class StoreChangeHandlerProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final Messager messager = processingEnv.getMessager();
        final Filer filer = processingEnv.getFiler();
        boolean claimed = (annotations.size() == 1
                && annotations.iterator().next().getQualifiedName().toString().equals(
                StoreChangeHandler.class.getName()));

        if (!claimed) return false;

        // group methods by containing class
        Map<String, List<ExecutableElement>> methods = new HashMap<String, List<ExecutableElement>>();
        for (Element componentHandler : roundEnv.getElementsAnnotatedWith(StoreChangeHandler.class)) {
            ExecutableElement method = MoreElements.asExecutable(componentHandler);
            String key = method.getEnclosingElement().toString();
            if (!methods.containsKey(key)) {
                methods.put(key, new ArrayList<ExecutableElement>());
            }
            methods.get(key).add(method);
        }

        for (String key : methods.keySet()) {
            ComponentModel.ComponentClass component = ComponentModel.createComponent(processingEnv.getTypeUtils(),
                    messager, methods.get(key));

            ComponentModel.ComponentBinder binder = component.binder();

            try {

                JavaFileObject sourceFile = filer.createSourceFile(binder.fullyQualifiedName(), component.element());
                Writer writer = sourceFile.openWriter();

                try {
                    TypeSpec typeSpec = ComponentBinderJavaBuilder.buildComponentBinder(component);
                    JavaFile javaFile = JavaFile.builder(binder.packageName(), typeSpec).build();
                    javaFile.writeTo(writer);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    writer.close();
                }
            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Failed to write source file for: " + binder.fullyQualifiedName(), component.element());
            }


        }


        return true;
    }
}
