package example.todo.ui_javafx;

import dagger.Module;
import dagger.Provides;
import example.todo.TodoAppDispatcher;
import example.todo.TodoAppDispatcher_AutoImpl;
import example.todo.TodoStore;
import io.tals.flux4j.shared.StoreChangeBinder;

import javax.inject.Singleton;

/**
 * @author Tal Shani
 */
@Module
public class TodoModule {
    @Provides
    @Singleton
    TodoAppDispatcher provideTodoAppDispatcher(TodoStore store) {
        return new TodoAppDispatcher_AutoImpl(store);
    }
    @Provides
    StoreChangeBinder<TodoListControl> provide_TodoListControlBinder() {
        return new TodoListControl_StoreChangeBinder();
    }
}
