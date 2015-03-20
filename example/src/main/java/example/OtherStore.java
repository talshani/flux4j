package example;

import io.tals.flux4j.shared.ActionHandler;

/**
 * @author Tal Shani
 */
public class OtherStore {
    @ActionHandler()
    boolean handle(AnAction anAction, SomeStore someStore) {
        return true;
    }

    @ActionHandler
    boolean handleOtherStringAction() {
        return false;
    }
}
