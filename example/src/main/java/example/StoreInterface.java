package example;

import io.tals.flux4j.shared.ActionHandler;

/**
 * An example of a store interface without implementation
 *
 * @author Tal Shani
 */
public interface StoreInterface {
    @ActionHandler
    void handleOtherStringAction(OtherStore t1, SomeStore t2);
}
