package com.dinikos.file_cloud_easy.client;

import com.dinikos.file_cloud_easy.common.Command;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.DriverManager;
import java.util.Optional;

public class LoginController {
    @FXML
    TextField login;

    @FXML
    PasswordField password;

    @FXML
    VBox globParent;

//    @FXML
//    ListView<String> clientList;

    public int id;
    public int id2;
    public int id3;

    public Controller backController;

    public void auth(ActionEvent actionEvent) {
        backController.setClearListCloud();

        System.out.println(login.getText() + " " + password.getText());
        System.out.println("authId = " + id);
        globParent.getScene().getWindow().hide();
        backController.connect();
        if  (!login.getText().trim().isEmpty() || !password.getText().trim().isEmpty()) {
            Network.sendMsg(new Command("/auth " + login.getText() + " " +  password.getText(), ""  ));
        } else {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Field is empty! Enter login and password", ButtonType.OK);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get().getText().equals("OK")) {
                System.out.println("You clicked OK");
            }
        }
        login.clear();
        password.clear();
        if (backController.isAuthorized) {
            backController.labelAutorizeNOK.setText("Autorize!");
        }
    }

    public void sign(ActionEvent actionEvent) {
        System.out.println(login.getText() + " " + password.getText());
        System.out.println("signId = " + id2);
        globParent.getScene().getWindow().hide();


        if  (!login.getText().trim().isEmpty() || !password.getText().trim().isEmpty()) {

            Network.sendMsg(new Command("/sign " + login.getText() + " " +  password.getText(), ""  ));
            System.out.println("/sign " + login.getText() + " " +  password.getText());
            backController.setClearListCloud();
            backController.connect();
        } else {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Field is empty! Enter login and password", ButtonType.OK);
            // showAndWait() показывает Alert и блокирует остальное приложение пока мы не закроем Alert
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get().getText().equals("OK")) {
                System.out.println("You clicked OK");
            }
        }
        login.clear();
        password.clear();
    }

    public void disconnect(ActionEvent actionEvent) {
        backController.setClearListCloud();
        System.out.println("discon_data= " + login.getText() + "/" + password.getText());
        System.out.println("disconId = " + id3);
        globParent.getScene().getWindow().hide();
        if (backController.isAuthorized) {
            Network.sendMsg(new Command("/end_login", ""));
            backController.labelAutorizeNOK.setText("!Autorize");
        }
        login.clear();
        password.clear();
        System.out.println("all_clear");
    }

}
