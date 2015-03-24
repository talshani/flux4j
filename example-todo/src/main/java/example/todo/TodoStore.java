package example.todo;

import com.google.common.collect.ImmutableList;
import io.tals.flux4j.shared.ActionHandler;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Tal Shani
 */
@Singleton
public class TodoStore {

    private final Map<Long, TodoItem> items = new HashMap<Long, TodoItem>();

    @Inject
    public TodoStore() {
    }

    /**
     * Get the entire collection of TODOs.
     *
     * @return
     */
    public List<TodoItem> getAll() {
        return ImmutableList.copyOf(items.values());
    }

    @ActionHandler
    boolean handleTodoCreate(TodoCreateAction action) {
        String text = action.getText().trim();
        if (!text.isEmpty()) {
            create(text);
            return true;
        }
        return false;
    }

    @ActionHandler
    boolean handleTodoDestroyMoo(TodoDestroyAction action) {
        if(items.containsKey(action.getId())) {
            items.remove(action.getId());
            return true;
        }
        return false;
    }

    @ActionHandler
    boolean handle(TodoCompletedAction action) {
        if(items.containsKey(action.getId())) {
            items.get(action.getId()).setComplete(action.isCompleted());
            return true;
        }
        return false;
    }

    /**
     * Create a TO-DO item.
     *
     * @param text text The content of the TO-DO
     */
    private void create(String text) {
        // Using the current timestamp in place of a real id.
        long id = new Date().getTime();
        items.put(id, new TodoItem(id, false, text));
    }
}
