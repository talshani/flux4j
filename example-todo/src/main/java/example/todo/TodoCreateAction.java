package example.todo;

/**
 * @author Tal Shani
 */
public class TodoCreateAction {
    private final String text;

    public TodoCreateAction(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
