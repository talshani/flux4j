package io.tals.flux4j.shared;

/**
 * @author Tal Shani
 */
public final class StoreChangeSubscriptionEntry {
    private final StoreChangeBinder binder;
    private final Object component;

    StoreChangeSubscriptionEntry(StoreChangeBinder binder, Object component) {
        this.binder = binder;
        this.component = component;
    }

    public StoreChangeBinder getBinder() {
        return binder;
    }

    public Object getComponent() {
        return component;
    }
}
