package io.tals.flux4j.apt.sample1;

import io.tals.flux4j.shared.ActionHandler;

/**
 * @author Tal Shani
 */
public class OtherStore {
    @ActionHandler(
            dependencies = {SomeStore.class}
    )
    void handle(AnAction anAction) {

    }
    @ActionHandler
    void handleOtherStringAction() {

    }
}
