package example.todo.ui_javafx;

import example.todo.TodoAppDispatcher;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import javax.inject.Inject;

/**
 * @author Tal Shani
 */
public class TodoItemControl extends HBox {

    private final CheckBox completedCheckbox;
    private final Label itemLabel;
    private long id = -1;

    @Inject
    public TodoItemControl(final TodoAppDispatcher dispatcher) {
        completedCheckbox = new CheckBox();
        itemLabel = new Label();
        getChildren().addAll(completedCheckbox, itemLabel);
        completedCheckbox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
                if (aBoolean == t1) return;
                if (t1) {
                    dispatcher.dispatchCompleted(id);
                } else {
                    dispatcher.dispatchNotCompleted(id);
                }
            }
        });
    }

    public void setData(long id, String text, boolean complete) {
        this.id = id;
        itemLabel.setText(text);
        completedCheckbox.setSelected(complete);
        itemLabel.setStyle("-fx-background-color: " + (complete ? "green;" : "white;"));
    }
}
