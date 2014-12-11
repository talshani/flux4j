package com.github.talshani.flux4j.apt.sample1;

import com.github.talshani.flux4j.shared.ActionHandler;

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
