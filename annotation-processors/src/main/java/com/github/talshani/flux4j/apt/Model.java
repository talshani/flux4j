package com.github.talshani.flux4j.apt;

import com.github.talshani.flux4j.shared.ActionHandler;
import com.github.talshani.flux4j.shared.AppDispatcher;
import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.value.AutoValue;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import javax.annotation.Nullable;

import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementKindVisitor7;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.*;

/**
 * @author Tal Shani
 */
final class Model {

    static Dispatcher dispatcher(Types types, Messager messager, TypeElement typeElement) {
        return new AutoValue_Model_Dispatcher(types, messager, typeElement);
    }

    @AutoValue
    abstract static class Store {
        abstract int id();

        abstract Dispatcher dispatcher();

        abstract TypeElement typeElement();

        private ImmutableList<HandlerMethod> handlerMethods = null;

        public StoreHelper helperClass() {
            return new AutoValue_Model_StoreHelper(this);
        }

        public String className() {
            return typeElement().getSimpleName().toString();
        }

        public String packageName() {
            return MoreElements.getPackage(typeElement()).getQualifiedName().toString();
        }

        public String fullyQualifiedName() {
            return packageName() + "." + className();
        }

        public ImmutableList<HandlerMethod> handlerMethods() {
            if (handlerMethods == null) {
                ImmutableList.Builder<HandlerMethod> builder = new ImmutableList.Builder<HandlerMethod>();
                typeElement().accept(new HandlerMethodsFinder(), builder);
                handlerMethods = builder.build();
            }
            return handlerMethods;
        }

        public HandlerMethod findHandlerFor(Action action) {
            for (HandlerMethod handlerMethod : handlerMethods()) {
                if (handlerMethod.action().equals(action)) {
                    return handlerMethod;
                }
            }
            return null;
        }

        private class HandlerMethodsFinder extends ElementKindVisitor7<Void, ImmutableList.Builder<HandlerMethod>> {
            @Override
            public Void visitExecutableAsMethod(ExecutableElement e, ImmutableList.Builder<HandlerMethod> builder) {
                if (!HandlerMethod.canBeHandler(e)) return null;
                HandlerMethod handler = new AutoValue_Model_HandlerMethod(Store.this, e);
                handler.validate();
                builder.add(handler);
                return super.visitExecutableAsMethod(e, builder);
            }

            @Override
            public Void visitType(TypeElement e, ImmutableList.Builder<HandlerMethod> handlerMethodBuilder) {
                for (Element element : e.getEnclosedElements()) {
                    element.accept(this, handlerMethodBuilder);
                }
                return super.visitType(e, handlerMethodBuilder);
            }
        }
    }

    @AutoValue
    abstract static class HandlerMethod {
        abstract Store store();

        abstract ExecutableElement methodElement();

        public String name() {
            return methodElement().getSimpleName().toString();
        }

        public Action action() {
            ExecutableElement methodElement = methodElement();
            Messager messager = store().dispatcher().messager();
            return getAction(methodElement, messager, name(), "handle");
        }

        public boolean isTypedHandler() {
            return action().isTyped();
        }

        public ImmutableList<TypeMirror> rawDependencies() {
            AnnotationMirror annotationMirror = getAnnotationMirror(methodElement(), ActionHandler.class);
            List<TypeMirror> dependencies = getTypeMirrors(getAnnotationValue(annotationMirror, "dependencies"));
            return ImmutableList.copyOf(dependencies);
        }

        public ImmutableList<Store> dependencies() {
            ImmutableList.Builder<Store> builder = ImmutableList.builder();
            for (TypeMirror mirror : rawDependencies()) {
                Store storeByType = store().dispatcher().findStoreByType(mirror);
                if (storeByType == null) throw new RuntimeException("Unexpected null");
                builder.add(storeByType);
            }
            return builder.build();
        }

        public static boolean canBeHandler(ExecutableElement el) {
            return MoreElements.isAnnotationPresent(el, ActionHandler.class);
        }


        public void validate() {
            Messager messager = store().dispatcher().messager();
        }
    }

    @AutoValue
    abstract static class StoreHelper {
        abstract Store store();

        public TypeElement storeTypeElement() {
            return store().typeElement();
        }

        public String className() {
            return store().className() + "_DispatcherHelper";
        }

        public String packageName() {
            return store().packageName();
        }

        public String fullyQualifiedName() {
            return packageName() + "." + className();
        }
    }

    @AutoValue
    abstract static class Dispatcher {

        abstract Types types();

        abstract Messager messager();

        abstract TypeElement typeElement();

        private ImmutableList<Store> stores = null;
        private ImmutableList<DispatchingMethod> dispatchingMethods = null;

        public DispatcherImplementation implementation() {
            return new AutoValue_Model_DispatcherImplementation(this);
        }

        public String className() {
            return typeElement().getSimpleName().toString();
        }

        public boolean isInterface() {
            return typeElement().getKind() == ElementKind.INTERFACE;
        }

        public String packageName() {
            return MoreElements.getPackage(typeElement()).getQualifiedName().toString();
        }

        public String fullyQualifiedName() {
            return packageName() + "." + className();
        }

        public ImmutableList<Store> stores() {
            if (stores == null) {
                AnnotationMirror annotationMirror = getAnnotationMirror(typeElement(), AppDispatcher.class);
                ImmutableList.Builder<Store> storesBuilder = ImmutableList.builder();
                int storeId = 0;
                for (TypeMirror storeType : getTypeMirrors(getAnnotationValue(annotationMirror, "stores"))) {
                    storesBuilder.add(new AutoValue_Model_Store(storeId++, Dispatcher.this, MoreTypes.asTypeElement(types(), storeType)));
                }
                stores = storesBuilder.build();
            }
            return stores;
        }


        public ImmutableList<DispatchingMethod> dispatchingMethods() {
            if (dispatchingMethods == null) {
                ImmutableList.Builder<DispatchingMethod> builder = new ImmutableList.Builder<DispatchingMethod>();
                typeElement().accept(new DispatchingMethodsFinder(), builder);
                dispatchingMethods = builder.build();
            }
            return dispatchingMethods;
        }

        public ImmutableList<HandlerMethod> findStoreHandlersForAction(Action action) {
            Multimap<HandlerMethod, HandlerMethod> dependencies = HashMultimap.create();
            List<HandlerMethod> handlers = new ArrayList<HandlerMethod>();
            for (Store store : stores()) {
                HandlerMethod handler = store.findHandlerFor(action);
                if (handler == null) continue;
                handlers.add(handler);
                for (Store dep : handler.dependencies()) {
                    HandlerMethod depHandler = dep.findHandlerFor(action);
                    if (depHandler != null) {
                        dependencies.put(handler, depHandler);
                    } else {
                        messager().printMessage(Diagnostic.Kind.WARNING, String.format("%s has a redundant dependency on %s", handler, dep));
                    }
                }
            }
            List<HandlerMethod> list = new ArrayList<HandlerMethod>(handlers.size());
            Set<HandlerMethod> visited = new HashSet<HandlerMethod>(handlers.size());
            for (HandlerMethod handler : handlers) {
                addHandlerToList(handler, dependencies, list, visited);
            }

            if (list.size() != handlers.size()) {
                messager().printMessage(Diagnostic.Kind.ERROR, list.toString());
                messager().printMessage(Diagnostic.Kind.ERROR, handlers.toString());
                messager().printMessage(Diagnostic.Kind.ERROR, "Could not resolve dependencies order", typeElement());
            }

            return ImmutableList.copyOf(list);
        }

        private void addHandlerToList(HandlerMethod method, Multimap<HandlerMethod, HandlerMethod> dependencies,
                                      List<HandlerMethod> list, Set<HandlerMethod> visited) {
            if (list.contains(method)) return;
            if (visited.contains(method)) {
                messager().printMessage(Diagnostic.Kind.ERROR, "Circular dependency detected", method.methodElement());
                throw new RuntimeException();
            }
            visited.add(method);
            for (HandlerMethod dep : dependencies.get(method)) {
                addHandlerToList(dep, dependencies, list, visited);
            }
            list.add(method);
        }

        public Store findStoreByType(TypeMirror mirror) {
            for (Store store : stores()) {
                if (store.typeElement().getQualifiedName().equals(MoreTypes.asTypeElement(types(), mirror).getQualifiedName())) {
                    return store;
                }
            }
            return null;
        }

        private class DispatchingMethodsFinder extends ElementKindVisitor7<Void, ImmutableList.Builder<DispatchingMethod>> {
            @Override
            public Void visitExecutableAsMethod(ExecutableElement e, ImmutableList.Builder<DispatchingMethod> builder) {
                if (!e.getModifiers().contains(Modifier.ABSTRACT)) {
                    return null;
                }
                if (e.getReturnType().getKind() != TypeKind.VOID) {
                    messager().printMessage(Diagnostic.Kind.ERROR, "Dispatch methods cannot have return type", e);
                }
                builder.add(new AutoValue_Model_DispatchingMethod(Dispatcher.this, e));
                return super.visitExecutableAsMethod(e, builder);
            }

            @Override
            public Void visitType(TypeElement e, ImmutableList.Builder<DispatchingMethod> handlerMethodBuilder) {
                for (Element element : e.getEnclosedElements()) {
                    element.accept(this, handlerMethodBuilder);
                }
                return super.visitType(e, handlerMethodBuilder);
            }
        }
    }


    @AutoValue
    abstract static class DispatcherImplementation {
        abstract Dispatcher dispatcher();

        public TypeElement dispatcherTypeElement() {
            return dispatcher().typeElement();
        }

        public String className() {
            return dispatcher().className() + "_AutoImpl";
        }

        public String packageName() {
            return dispatcher().packageName();
        }

        public String fullyQualifiedName() {
            return packageName() + "." + className();
        }
    }

    @AutoValue
    abstract static class DispatchingMethod {

        public static final String PREFIX = "dispatch";

        abstract Dispatcher dispatcher();

        abstract ExecutableElement methodElement();

        public Action action() {
            ExecutableElement methodElement = methodElement();
            Messager messager = dispatcher().messager();
            String methodName = name();
            String prefix = PREFIX;
            return getAction(methodElement, messager, methodName, prefix);
        }

        public boolean isTypedDispatcher() {
            return action().isTyped();
        }

        public boolean isStringDispatcher() {
            return action().isStringAction();
        }

        public String name() {
            return methodElement().getSimpleName().toString();
        }
    }

    private static Action getAction(ExecutableElement methodElement, Messager messager, String methodName, String prefix) {
        if (methodElement.getParameters().size() == 1) {
            return new AutoValue_Model_Action(methodElement.getParameters().get(0).asType(), null);
        } else if (methodName.startsWith(prefix)) {
            return new AutoValue_Model_Action(null, methodName.substring(prefix.length()));
        } else {
            String msg = "Action must be typed or a string";
            messager.printMessage(Diagnostic.Kind.ERROR, msg, methodElement);
            throw new RuntimeException(msg);
        }
    }


    private static List<TypeMirror> getTypeMirrors(AnnotationValue annotationValue) {
        if (annotationValue == null) return new ArrayList<TypeMirror>(0);
        List<? extends AnnotationValue> items = (List<? extends AnnotationValue>) annotationValue.getValue();
        if (items == null || items.isEmpty()) {
            return new ArrayList<TypeMirror>(0);
        }
        List<TypeMirror> typeMirrors = new ArrayList<TypeMirror>(items.size());
        for (AnnotationValue item : items) {
            typeMirrors.add((TypeMirror) item.getValue());
        }
        return typeMirrors;
    }

    @AutoValue
    public static abstract class Action {
        @Nullable
        abstract TypeMirror type();

        @Nullable
        abstract String name();

        public boolean isTyped() {
            return type() != null;
        }

        public boolean isStringAction() {
            return name() != null;
        }
    }

    private static AnnotationValue getAnnotationValue(AnnotationMirror annotationMirror, String name) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
            if (name.equals(entry.getKey().getSimpleName().toString())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static AnnotationMirror getAnnotationMirror(Element el, Class<? extends Annotation> cls) {
        return MoreElements.getAnnotationMirror(el, cls).get();
    }
}
