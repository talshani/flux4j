package io.tals.flux4j.shared;

/**
 * @author Tal Shani
 */
public interface StoreChangeBinder<T> {
    void fireChangeEvent(Object component, Object[] allStores, Boolean[] changedStores);
}
