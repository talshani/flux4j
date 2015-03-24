package example.todo.ui_javafx;

import example.todo.TodoAppDispatcher;
import example.todo.TodoItem;
import example.todo.TodoStore;
import io.tals.flux4j.shared.StoreChangeBinder;
import io.tals.flux4j.shared.StoreChangeHandler;
import io.tals.flux4j.shared.StoreChangeSubscription;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

/**
 * @author Tal Shani
 */
public class TodoListControl extends VBox {

    private final TodoStore todoStore;
    private final StoreChangeSubscription binding;
    private final Provider<TodoItemControl> todoItemControlProvider;

    @Inject
    public TodoListControl(TodoStore todoStore, StoreChangeBinder<TodoListControl> binder, TodoAppDispatcher dispatcher,
                           Provider<TodoItemControl> todoItemControlProvider) {
        this.todoStore = todoStore;
        binding = dispatcher.bind(binder, this);

        this.todoItemControlProvider = todoItemControlProvider;
        setStyle("-fx-background-color: antiquewhite;");

        for (TodoItem item : todoStore.getAll()) {
            TodoItemControl control = todoItemControlProvider.get();
            updateItemControl(item, control);
            getChildren().add(control);
        }
    }

    private void updateItemControl(TodoItem item, TodoItemControl control) {
        control.setData(item.getId(), item.getText(), item.isComplete());
    }


    @StoreChangeHandler
    void onStoreChange(TodoStore todoStore) {
        List<TodoItem> items = todoStore.getAll();
        ObservableList<Node> children = getChildren();

        int itemsCount = items.size();
        int childCount = children.size();

        int toDelete = Math.max(0, childCount - itemsCount);
        while(toDelete > 0) {
            children.remove(itemsCount);
        }

        int i;
        for (i = 0; i < childCount; i++) {
            updateItemControl(items.get(i), (TodoItemControl) children.get(i));
        }
        for (; i < itemsCount; i++) {
            TodoItemControl newControl = todoItemControlProvider.get();
            children.add(newControl);
            updateItemControl(items.get(i), newControl);
        }

    }


}
