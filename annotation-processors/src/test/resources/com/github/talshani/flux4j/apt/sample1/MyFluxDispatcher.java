package com.github.talshani.flux4j.apt.sample1;

import com.github.talshani.flux4j.shared.AppDispatcher;

/**
 * @author Tal Shani
 */
@AppDispatcher(
        stores = {
                OtherStore.class,
                SomeStore.class
        }
)
public interface MyFluxDispatcher {
        void dispatch(AnAction action);
        void dispatchOtherStringAction();
}
