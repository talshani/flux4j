package example.todo.ui_javafx;

import example.todo.TodoAppDispatcher;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * @author Tal Shani
 */
public class TodoApplication extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        final TodoAppInjector injector = Dagger_TodoAppInjector.create();

        stage.setTitle("Sample Todo App!");
        Button btn = new Button();
        btn.setText("Say 'Hello World'");
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                TodoAppDispatcher dispatcher = injector.dispatcher();
                dispatcher.dispatchCreate("Created with the button");
            }
        });

        VBox vBox = new VBox();
        StackPane root = new StackPane();
        TodoListControl todoControl = injector.todoControl();
        vBox.getChildren().addAll(todoControl, btn);
        root.getChildren().add(vBox);
        stage.setScene(new Scene(root, 300, 250));
        stage.show();
    }
}
