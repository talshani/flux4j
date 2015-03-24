package io.tals.flux4j.apt;

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.squareup.javapoet.MethodSpec;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Tal Shani
 */
final class ComponentModel {


    public static ComponentClass createComponent(Types types, Messager messager, Iterable<ExecutableElement> handlerElements) {
        ExecutableElement firstHandler = Iterables.getFirst(handlerElements, null);
        assert firstHandler != null;
        TypeElement typeElement = MoreElements.asType(firstHandler.getEnclosingElement());
        ComponentClass component = new AutoValue_ComponentModel_ComponentClass(typeElement, types);
        component.methodElements = ImmutableList.copyOf(handlerElements);
        return component;
    }


    @AutoValue
    abstract static class ComponentClass {
        abstract TypeElement element();

        abstract Types types();

        private ImmutableList<StoreChangeHandlerMethod> methods = null;
        private ImmutableList<ExecutableElement> methodElements = null;
        private ComponentBinder binder = null;

        public ImmutableList<StoreChangeHandlerMethod> methods() {
            if (methods == null) {
                ImmutableList.Builder<StoreChangeHandlerMethod> builder = ImmutableList.builder();
                for (ExecutableElement methodElement : methodElements) {
                    StoreChangeHandlerMethod handler = new AutoValue_ComponentModel_StoreChangeHandlerMethod(this, methodElement);
                    builder.add(handler);
                }
                methods = builder.build();
            }
            return methods;
        }

        public ComponentBinder binder() {
            if (binder == null) {
                binder = new AutoValue_ComponentModel_ComponentBinder(this);
            }
            return binder;
        }

        public String packageName() {
            return MoreElements.getPackage(element()).getQualifiedName().toString();
        }

        public String className() {
            return element().getSimpleName().toString();
        }

        public String fullyQualifiedName() {
            return packageName() + "." + className();
        }

        public ImmutableSet<TypeElement> stores() {
            ImmutableSet.Builder<TypeElement> builder = ImmutableSet.builder();
            for (StoreChangeHandlerMethod method : methods()) {
                for (StoreDependency storeDependency : method.storesDependencies()) {
                    builder.add(storeDependency.element());
                }
            }
            return builder.build();
        }

        private final Map<TypeElement, Integer> storeMapping = new HashMap<TypeElement, Integer>();

        public int storeId(TypeElement storeElement) {
            if (!storeMapping.containsKey(storeElement)) {
                storeMapping.put(storeElement, storeMapping.size());

            }
            return storeMapping.get(storeElement);
        }

        private final Map<StoreChangeHandlerMethod, Integer> handlerMapping = new HashMap<StoreChangeHandlerMethod, Integer>();

        public int handlerId(StoreChangeHandlerMethod method) {
            if (!handlerMapping.containsKey(method)) {
                handlerMapping.put(method, handlerMapping.size());

            }
            return handlerMapping.get(method);
        }
    }

    @AutoValue
    abstract static class StoreChangeHandlerMethod {

        private ImmutableList<StoreDependency> stores = null;

        abstract ComponentClass component();

        abstract ExecutableElement methodElement();

        public ImmutableList<StoreDependency> storesDependencies() {
            if (stores == null) {
                ImmutableList.Builder<StoreDependency> builder = ImmutableList.builder();
                for (VariableElement variableElement : methodElement().getParameters()) {
                    StoreDependency store = new AutoValue_ComponentModel_StoreDependency(variableElement.asType(), this);
                    builder.add(store);
                }
                stores = builder.build();
            }
            return stores;
        }

        public boolean dependsOnStore(TypeElement typeElement) {
            for (StoreDependency storeDependency : storesDependencies()) {
                if (storeDependency.element().equals(typeElement)) return true;
            }
            return false;
        }

        public String name() {
            return methodElement().getSimpleName().toString();
        }
    }

    @AutoValue
    abstract static class ComponentBinder {
        abstract ComponentClass component();

        public String className() {
            return component().className() + "_StoreChangeBinder";
        }

        public String packageName() {
            return component().packageName();
        }

        public String fullyQualifiedName() {
            return packageName() + "." + className();
        }
    }

    @AutoValue
    abstract static class StoreDependency {

        abstract TypeMirror type();

        abstract StoreChangeHandlerMethod handlerMethod();

        public TypeElement element() {
            return MoreTypes.asTypeElement(type());
        }

        public String key() {
            return type().toString();
        }
    }
}
