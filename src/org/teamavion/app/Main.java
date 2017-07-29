package org.teamavion.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {

    @Override
    public void start(Stage loginStage) throws Exception{
        Parent LoginRoot = FXMLLoader.load(getClass().getResource("../assets/login.fxml"));
        loginStage.initStyle(StageStyle.DECORATED);
        loginStage.setTitle("Team Avion Desktop Application");
        loginStage.setScene(new Scene(LoginRoot, 421.0, 581.0));
        loginStage.setResizable(false);
        loginStage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}
