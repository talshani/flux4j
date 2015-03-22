package example.ui;

import example.MyFluxDispatcher;
import example.OtherStore;
import example.SomeStore;
import io.tals.flux4j.shared.StoreChangeHandler;
import io.tals.flux4j.shared.StoreChangeSubscription;

/**
 * @author Tal Shani
 */
public class SomeUiComponent {

    private final StoreChangeSubscription subscription;

    public SomeUiComponent(MyFluxDispatcher dispatcher, SomeUiComponent_StoreChangeBinder binder) {
        subscription = dispatcher.bind(binder, this);
//        dispatcher.onChange(binder, this);

    }

    void render() {

    }

    @StoreChangeHandler
    void handleStoreChange(OtherStore otherStore, SomeStore someStore) {

    }
}
