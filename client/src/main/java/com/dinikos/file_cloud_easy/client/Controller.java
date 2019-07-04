package com.dinikos.file_cloud_easy.client;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;


public class Controller {
    @FXML
    TextArea textArea;

    @FXML
    TextField textField;

    public void sendMessage() {
        textArea.appendText(textField.getText());
        textArea.appendText("\n");
        textField.clear();
        textField.requestFocus();
    }
}
