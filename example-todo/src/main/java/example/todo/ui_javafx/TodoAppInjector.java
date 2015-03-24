package example.todo.ui_javafx;

import dagger.Component;
import example.todo.TodoAppDispatcher;
import example.todo.TodoStore;

import javax.inject.Singleton;

/**
 * @author Tal Shani
 */
@Component(modules = TodoModule.class)
@Singleton
interface TodoAppInjector {
    TodoListControl todoControl();

    TodoStore todoStore();

    TodoAppDispatcher dispatcher();
}
