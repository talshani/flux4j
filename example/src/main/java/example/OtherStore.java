package example;

import io.tals.flux4j.shared.ActionHandler;

/**
 * @author Tal Shani
 */
public class OtherStore {
    @ActionHandler()
    void handle(AnAction anAction, SomeStore someStore) {

    }
    @ActionHandler
    void handleOtherStringAction() {

    }
}
