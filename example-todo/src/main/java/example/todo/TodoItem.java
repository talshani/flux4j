package example.todo;

/**
 * @author Tal Shani
 */
public class TodoItem {
    private final long id;
    private String text;
    private boolean complete;

    public TodoItem(long id, boolean complete, String text) {
        this.id = id;
        this.text = text;
        this.complete = complete;
    }

    public long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }
}
