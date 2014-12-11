package example;

import com.github.talshani.flux4j.shared.AppDispatcher;

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
public interface MyFluxDispatcher {
        void dispatch(AnAction action);
        void dispatchOtherStringAction();
}
