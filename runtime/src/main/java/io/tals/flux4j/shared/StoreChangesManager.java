package io.tals.flux4j.shared;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Tal Shani
 */
public final class StoreChangesManager {

    private final List<Entry> entries = new LinkedList<Entry>();

    public <T> StoreChangeSubscription add(StoreChangeBinder<T> binder, T component) {
        Entry entry = new Entry(binder, component);
        entries.add(entry);
        return new Subscription(entry);
    }

    public void fire(Object[] allStores, Boolean[] changedStores) {
        for (Entry entry : entries) {
            if (entry.enabled) {
                // NOTE: we don't clone the arrays because the binders are generated by our code so we know
                // they don't modify the passed in array
                entry.binder.fireChangeEvent(entry.component, allStores, changedStores);
            }
        }
    }

    private static class Entry {
        private final StoreChangeBinder binder;
        private final Object component;
        private boolean enabled = true;

        Entry(StoreChangeBinder binder, Object component) {
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

    private class Subscription implements StoreChangeSubscription {

        private Entry entry;

        private Subscription(Entry entry) {
            this.entry = entry;
        }

        @Override
        public void unbind() {
            if (entry == null) return;
            entries.remove(entry);
            entry = null;
        }

        @Override
        public void enabled() {
            if (entry != null) {
                entry.enabled = true;
            }
        }

        @Override
        public void disable() {
            if (entry != null) {
                entry.enabled = false;
            }
        }

        @Override
        public boolean isEnabled() {
            return entry != null && entry.enabled;
        }
    }
}