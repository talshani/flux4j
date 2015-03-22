package io.tals.flux4j.shared;

/**
 * @author Tal Shani
 */
public interface StoreChangeSubscription {
    void unbind();
    void enabled();
    void disable();
    boolean isEnabled();
}
