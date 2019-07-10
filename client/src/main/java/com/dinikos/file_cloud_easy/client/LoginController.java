package com.dinikos.file_cloud_easy.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.DriverManager;

public class LoginController {
    @FXML
    TextField login;

    @FXML
    PasswordField password;

    @FXML
    VBox globParent;

    @FXML
    ListView<String> clientList;



    public int id;

    private boolean isAuthorized;

    Socket socket;
    DataInputStream in;
    DataOutputStream out;

    final String IP_ADRESS = "localhost";
    final int PORT = 8189;

    public Controller backController;

    public void setAuthorized(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
        if(!isAuthorized) {
//            upperPanel.setVisible(true);
//            upperPanel.setManaged(true);
//            bottomPanel.setVisible(false);
//            bottomPanel.setManaged(false);
            clientList.setVisible(false);
            clientList.setManaged(false);
        } else {
//            upperPanel.setVisible(false);
//            upperPanel.setManaged(false);
//            bottomPanel.setVisible(true);
//            bottomPanel.setManaged(true);
            clientList.setVisible(true);
            clientList.setManaged(true);
        }
    }

    public void auth(ActionEvent actionEvent) {
        System.out.println(login.getText() + " " + password.getText());
        System.out.println("id = " + id);
        globParent.getScene().getWindow().hide();

        if(socket == null || socket.isClosed()) {
            connect();
        }

        try {
            if  (login.getText().trim().isEmpty() || password.getText().trim().isEmpty()) {
                out.writeUTF("/auth " + "null" + " " + "null");
            } else {
                out.writeUTF("/auth " + login.getText() + " " + password.getText());
            }
            login.clear();
            password.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sign(ActionEvent actionEvent) {
        System.out.println(login.getText() + " " + password.getText());
        System.out.println("id = " + id);
        globParent.getScene().getWindow().hide();




//        CREATE TABLE user (
//                id       INTEGER PRIMARY KEY AUTOINCREMENT,
//                login    TEXT    UNIQUE,
//                password TEXT,
//                nickname TEXT    UNIQUE
//        );

    }

    public void connect() {

        try {
            socket = new Socket(IP_ADRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            String str = in.readUTF();
                            if (str.startsWith("/authok")) {
                                setAuthorized(true);
                              //  textArea.clear();
                                break;
                            } else if (str.equals("/closeAuth")) {
                               // textArea.appendText("Таймаут подключения 120 сек" + "\n");
                                out.writeUTF("/end2");
                                break;
                            } else {
                               // textArea.appendText(str + "\n");
                            }
                        }

                        while (true) {
                            String str = in.readUTF();
                            if(str.startsWith("/")) {

                                if (str.equals("/serverclosed"))  {
                                    // closeApp();
                                    break;
                                }
                                if (str.startsWith("/clientlist ")) {
                                    String[] tokens = str.split(" ");
                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {
//                                            MiniStage miniStage = new MiniStage();
//                                            miniStage.show();

                                            clientList.getItems().clear();
                                            for (int i = 1; i < tokens.length; i++) {
                                                clientList.getItems().add(tokens[i]);
                                            }
                                        }
                                    });
                                }
                            } else {
                               // textArea.appendText(str + "\n");
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            socket.close();
                           // textArea.appendText("Сокет закрыт" + "\n");
                            System.out.println("Сокет закрыт" + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        setAuthorized(false);
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg() {
        try {
          //  out.writeUTF(textField.getText());
            out.writeUTF("send MSG");
           // textField.clear();
           // textField.requestFocus();
            System.out.println("send MSG");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
