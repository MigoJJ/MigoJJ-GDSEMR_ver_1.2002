package com.emr.gds.features.thyroid.adapter.in.ui;

import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class ThyroidPregnancyUiTest {

    private Scene scene;
    private ThyroidPregnancy view;

    @Start
    private void start(Stage stage) {
        view = new ThyroidPregnancy();
        stage.setScene(new Scene(view));
        stage.show();
        scene = stage.getScene();
    }

    @Test
    void selectingConditionUpdatesDiagnosisField(FxRobot robot) {
        CheckBox condition = view.lookupAll(".check-box").stream()
                .map(CheckBox.class::cast)
                .filter(checkBox -> "Overt Hypothyroidism".equals(checkBox.getText()))
                .findFirst()
                .orElseThrow();
        Set<javafx.scene.Node> textFields = view.lookupAll(".text-field");

        robot.interact(condition::fire);

        assertTrue(textFields.stream()
                .map(TextField.class::cast)
                .anyMatch(field -> field.getText().contains("Overt Hypothyroidism")));
    }
}
