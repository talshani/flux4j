package io.tals.flux4j.shared;

/**
 * @author Tal Shani
 */
public interface FluxDispatcher {
    <T> StoreChangeSubscription bind(StoreChangeBinder<T> binder, T component);
    boolean isDispatching();
}
