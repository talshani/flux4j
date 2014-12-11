package example;

import com.github.talshani.flux4j.shared.ActionHandler;

/**
 * An example of a store interface without implementation
 *
 * @author Tal Shani
 */
public interface StoreInterface {
    @ActionHandler
    void handleOtherStringAction();
}
