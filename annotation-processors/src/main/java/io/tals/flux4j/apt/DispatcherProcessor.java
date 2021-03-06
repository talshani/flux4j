package io.tals.flux4j.apt;

import io.tals.flux4j.shared.AppDispatcher;
import com.google.auto.common.MoreElements;
import com.google.auto.service.AutoService;

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
@SupportedAnnotationTypes({"io.tals.flux4j.shared.AppDispatcher", "io.tals.flux4j.shared.ActionHandler"})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public final class DispatcherProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final Messager messager = processingEnv.getMessager();
        final Filer filer = processingEnv.getFiler();
//        boolean claimed = (annotations.size() == 1
//                && annotations.iterator().next().getQualifiedName().toString().equals(
//                AppDispatcher.class.getName()));
//
//        if(!claimed) return false;

        boolean hasInject = processingEnv.getElementUtils().getTypeElement("javax.inject.Inject") != null;
        boolean hasSingleton = processingEnv.getElementUtils().getTypeElement("javax.inject.Singleton") != null;

        if (roundEnv.processingOver()) return false;
        for (Element dispatcher : roundEnv.getElementsAnnotatedWith(AppDispatcher.class)) {
            Model.Dispatcher model = Model.dispatcher(processingEnv.getTypeUtils(), processingEnv.getMessager(), MoreElements.asType(dispatcher));
            if (model == null) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Could not construct dispatcher model", dispatcher);
                continue;
            }

            for (Model.Store store : model.stores()) {
                try {
                    StoreHelperJavaBuilder.writeStoreHelper(store, filer);
                } catch (IOException e) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Failed to write store dispatcher helper", store.typeElement());
                }
            }

            try {
                DispatcherJavaBuilder.writeDispatcher(model, hasInject, hasSingleton, filer);
            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Failed to write dispatcher", model.typeElement());
            }
        }
        return true;
    }
}
