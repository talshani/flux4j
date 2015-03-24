package example.todo;

/**
 * @author Tal Shani
 */
public class TodoCompletedAction {
    private final long id;
    private final boolean completed;

    public TodoCompletedAction(long id, boolean completed) {
        this.id = id;
        this.completed = completed;
    }


    public long getId() {
        return id;
    }

    public boolean isCompleted() {
        return completed;
    }
}
