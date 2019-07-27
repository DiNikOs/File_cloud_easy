package com.dinikos.file_cloud_easy.client;

import com.dinikos.file_cloud_easy.common.*;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller extends ChannelInboundHandlerAdapter implements Initializable {

    @FXML
    ListView<String> simpleListView;

    @FXML
    ListView<String> simpleListView2;

    @FXML
    Label filesDragAndDrop, labelDragWindow;

    @FXML
    VBox mainVBox;

    @FXML
    StackPane mainStackPane;

    @FXML
    Button btnShowSelectedElement;

    @FXML
    Button btnDelSelectedElement;

    private boolean isAuthorized;

    // Выполняется при старте контроллера
    // Для работы этого метода необходимо чтобы контроллер реализовал интерфейс Initializable
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        initializeDragAndDropLabel();
        initializeWindowDragAndDropLabel();
        btnShowSelectedElement.setPrefSize(120,60);
        btnDelSelectedElement.setPrefSize(120,60);
        isAuthorized = false;
        connect();
        refreshLocalFilesList();
        System.out.println("==INITIALIZE==");
    }

    void connect () {
        if (Network.isConnected()) {
            return;
        }
        Network.start();
        Network.sendMsg(new Command("/InitClient", ""));
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    AbstractMessage am = Network.readObject();

                    if (isAuthorized) {
                        System.out.println("Auturiz=true");
                        if (am instanceof FileMessage) {
                            FileMessage fm = (FileMessage) am;
                            Files.write(Paths.get("client/client_storage/" + fm.getFilename()),
                                    fm.getData(), StandardOpenOption.CREATE);
                            refreshLocalFilesList();
                        }
                        if (am instanceof FileList) {
                            FileList fl = (FileList) am;
                            System.out.println("FlList= " + fl.getFileList());
                            if (fl.getFileList()!=null) {
                                System.out.println("FlListSize= " + fl.getFileList().size());
                                refreshServerFilesList(fl);
                            }
                        }
                    }  else {
                        setClearListCloud();
                    }
                    if (am instanceof Command) {
                        String cmd = ((Command) am).getCommand();
                        System.out.println("putCMD= " + cmd);
                        if (cmd.equals("/exit")) {
                            break;
                        }
                        if (cmd.equals("/authOk")){
                            isAuthorized = true;
                        }
                        if (cmd.equals("/end")){
                            Network.sendMsg(new Command("/AuthNOK", ""));
                            isAuthorized = false;
                            System.out.println("endController");
                        }
                    }
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                Network.stop();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void refreshLocalFilesList() {
        updateUI(() -> {
            try {
                setClearListUser();
                Files.list(Paths.get("client/client_storage")).
                        map(p -> p.getFileName().toString()).
                        forEach(o -> simpleListView.getItems().add(o));
                int lenList = simpleListView.getItems().size();
                System.out.println("len list= " + lenList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void refreshServerFilesList(FileList f) {
        updateUI(() -> {
            setClearListCloud();
            simpleListView2.getItems().addAll(f.getFileList());
        });
    }

    public static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    public void initializeDragAndDropLabel() {
        filesDragAndDrop.setOnDragOver(event -> {
            if (event.getGestureSource() != filesDragAndDrop && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        filesDragAndDrop.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                filesDragAndDrop.setText("");
                for (File o : db.getFiles()) {
                    filesDragAndDrop.setText(filesDragAndDrop.getText() + o.getAbsolutePath() + " ");
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    public void btnShowAuth(ActionEvent actionEvent) {
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = loader.load();
            LoginController lc = (LoginController) loader.getController();
            lc.id = 100;
            lc.backController = this;

            stage.setTitle("Autorization");
            stage.setScene(new Scene(root, 200, 200));
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    double dragDeltaX, dragDeltaY;

    public void initializeWindowDragAndDropLabel() {
        Platform.runLater(() -> {
            Stage stage = (Stage) mainVBox.getScene().getWindow();

            labelDragWindow.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    // record a delta distance for the drag and drop operation.
                    dragDeltaX = stage.getX() - mouseEvent.getScreenX();
                    dragDeltaY = stage.getY() - mouseEvent.getScreenY();
                }
            });
            labelDragWindow.setOnMouseDragged(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    stage.setX(mouseEvent.getScreenX() + dragDeltaX);
                    stage.setY(mouseEvent.getScreenY() + dragDeltaY);
                }
            });
        });
    }

    public void btnExit(ActionEvent actionEvent) {
        if (Network.isConnected()) {
            Network.sendMsg(new Command("/exit", ""));
        }
        System.exit(0);
    }

    public void printSelectedItemInListView(ActionEvent actionEvent) {
        System.out.println("==BTN_UPLOAD==");
        System.out.println("action= " + actionEvent.toString());
        if (simpleListView.isFocused()) {
            System.out.println("startBTN List1");
            try {
                Network.sendMsg(new FileMessage(Paths.get("client/client_storage/" + simpleListView.getSelectionModel().getSelectedItem())));
                filesDragAndDrop.setText("Drop files here!");
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(simpleListView.getSelectionModel().getSelectedItem());
        } else if (simpleListView2.isFocused()) {
            System.out.println("startBTN List2");
            Network.sendMsg(new FileRequest(simpleListView2.getSelectionModel().getSelectedItem()));
            filesDragAndDrop.setText("Drop files here!");
            System.out.println("transfer OK");
        } else if (!filesDragAndDrop.getText().equals("Drop files here!")) {
            try {
                Network.sendMsg(new FileMessage(Paths.get(filesDragAndDrop.getText().trim())));
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("tranfer Obj OK ");
            System.out.println("dropText: " + filesDragAndDrop.getText());
            filesDragAndDrop.setText("Drop files here!");
        }
        else {
            Alert alert = new Alert(Alert.AlertType.NONE, "Select the file to download.", ButtonType.OK);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get().getText().equals("OK")) {
                System.out.println("You clicked OK");
            }
        }
        System.out.println("==BTN_UPLOAD_END==" + "\n");
    }

    public void DelSelectedItemInListView(ActionEvent actionEvent) {
        System.out.println("===DelBTN===");
        if (simpleListView.isFocused()) {
            File file = new File("client/client_storage/" + simpleListView.getSelectionModel().getSelectedItem());
            if( file.delete()){
                System.out.println("client/client_storage/" + simpleListView.getSelectionModel().getSelectedItem() + " файл удален");
            } else {
                System.out.println("Файла" +  simpleListView.getSelectionModel().getSelectedItem() + " не обнаружен");
            }
             refreshLocalFilesList();
        } else if (simpleListView2.isFocused()) {
            Network.sendMsg(new Command("/delFile",simpleListView2.getSelectionModel().getSelectedItem()));
            System.out.println("SendMsgDel");
        } else {
            Alert alert = new Alert(Alert.AlertType.NONE, "Select the file from to delete.", ButtonType.OK);
            // showAndWait() показывает Alert и блокирует остальное приложение пока мы не закроем Alert
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get().getText().equals("OK")) {
                System.out.println("You clicked OK");
            }
        }
        filesDragAndDrop.setText("Drop files here!");
        System.out.println("===DelBTNstop===");
    }

    public void setClearListUser() {
        simpleListView.getItems().clear();
        System.out.println("Clear_User");
    }

    public void setClearListCloud() {
        simpleListView2.getItems().clear();
        System.out.println("Clear_Cloud");
    }
}
