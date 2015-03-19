package io.tals.flux4j.apt;

/**
 * @author Tal Shani
 */
class ModelBuildingException extends RuntimeException {
    public ModelBuildingException(String message) {
        super(message);
    }

    public ModelBuildingException() {
        super();
    }
}
