package example.todo;

/**
 * @author Tal Shani
 */
public class TodoDestroyAction {
    private final long id;

    public TodoDestroyAction(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
