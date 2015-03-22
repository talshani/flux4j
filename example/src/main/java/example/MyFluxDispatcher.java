package example;

import io.tals.flux4j.shared.AppDispatcher;

import javax.inject.Named;

/**
 * @author Tal Shani
 */
@AppDispatcher(
        stores = {
                OtherStore.class,
                SomeStore.class,
                StoreInterface.class
        }
)
public abstract class MyFluxDispatcher {

    public MyFluxDispatcher(@Named("xxx") String moo) {
    }

    void onDispatchError(Throwable exception) {

    }

    void onDispatchInProgressError() {

    }

    void onDispatchStart() {

    }

    void onDispatchEnd() {

    }

    void digestChanges(Object... stores) {

    }

    //        boolean isDispatching();
    abstract void dispatch(AnAction action);

    abstract void dispatchOtherStringAction();
}
