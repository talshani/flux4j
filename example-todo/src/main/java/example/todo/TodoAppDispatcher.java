package example.todo;

import io.tals.flux4j.shared.AppDispatcher;
import io.tals.flux4j.shared.FluxDispatcher;

/**
 * @author Tal Shani
 */
@AppDispatcher(
        stores = {TodoStore.class}
)
public abstract class TodoAppDispatcher implements FluxDispatcher {

    abstract void dispatchCreate(TodoCreateAction action);

    abstract void dispatchDestroy(TodoDestroyAction action);

    abstract void dispatch(TodoCompletedAction action);

    public void dispatchCreate(String text) {
        dispatchCreate(new TodoCreateAction(text));
    }

    public void dispatchCompleted(long id) {
        dispatch(new TodoCompletedAction(id, true));
    }

    public void dispatchNotCompleted(long id) {
        dispatch(new TodoCompletedAction(id, false));
    }

    public void dispatchDestroy(long id) {
        dispatchDestroy(new TodoDestroyAction(id));
    }

}
