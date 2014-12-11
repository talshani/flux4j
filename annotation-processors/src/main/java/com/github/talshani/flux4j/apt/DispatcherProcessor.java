package com.github.talshani.flux4j.apt;

import com.github.talshani.flux4j.shared.AppDispatcher;
import com.google.auto.common.MoreElements;
import com.google.auto.service.AutoService;
import com.google.auto.value.AutoAnnotation;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Tal Shani
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("com.github.talshani.flux4j.shared.AppDispatcher")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public final class DispatcherProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final Messager messager = processingEnv.getMessager();
        final Filer filer = processingEnv.getFiler();
        boolean claimed = (annotations.size() == 1
                && annotations.iterator().next().getQualifiedName().toString().equals(
                AppDispatcher.class.getName()));

        if(!claimed) return false;

        for (Element dispatcher : roundEnv.getElementsAnnotatedWith(AppDispatcher.class)) {
            Model.Dispatcher model = Model.dispatcher(processingEnv.getTypeUtils(), processingEnv.getMessager(), MoreElements.asType(dispatcher));

            for (Model.Store store : model.stores()) {
                try {
                    Printing.writeStore(store, filer);
                } catch (IOException e) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Failed to write store dispatcher helper", store.typeElement());
                }
            }

            try {
                Printing.writeDispatcher(model, filer);
            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Failed to write dispatcher", model.typeElement());
            }
        }
        return true;
    }

    private List<TypeMirror> getTypeMirrors(AnnotationValue annotationValue) {
        List<? extends AnnotationValue> items = (List<? extends AnnotationValue>) annotationValue.getValue();
        List<TypeMirror> typeMirrors = new ArrayList<TypeMirror>(items.size());
        for (AnnotationValue item : items) {
            typeMirrors.add((TypeMirror) item.getValue());
        }
        return typeMirrors;
    }

    private AnnotationValue getAnnotationValue(AnnotationMirror annotationMirror, String name) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
            if (name.equals(entry.getKey().getSimpleName().toString())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
