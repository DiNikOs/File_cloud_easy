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
    Label filesDragAndDrop, labelDragWindow, labelAutorizeNOK;

    @FXML
    VBox mainVBox;

    @FXML
    StackPane mainStackPane;

    @FXML
    Button btnShowSelectedElement;

    @FXML
    Button btnDelSelectedElement;

    protected boolean isAuthorized;
    private StringBuffer text;


    // Выполняется при старте контроллера
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        initializeDragAndDropLabel();
        labelAutorizeNOK.setVisible(true);
        initializeWindowDragAndDropLabel();
        btnShowSelectedElement.setPrefSize(120,60);
        btnDelSelectedElement.setPrefSize(120,60);
        isAuthorized = false;
        text = new StringBuffer();
        connect();
        refreshLocalFilesList();
        System.out.println("==INITIALIZE==");
    }

    void connect () {
        if (Network.isConnected()) {
            return;
        }
        Network.start();
        Network.sendMsg(new Command("/init", ""));
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    AbstractMessage am = Network.readObject();
                    if (am instanceof Command) {
                        String cmd = ((Command) am).getCommand();
                        System.out.println("putCMD= " + cmd);
                        if (cmd.equals("/exit")) {
                            isAuthorized = false;
                            getLabelAuth (isAuthorized);
                            break;
                        }
                        if (cmd.equals("/authOk")){
                            isAuthorized = true;
                        }
                        if (cmd.equals("/signOK")){
                            getAlert ("/signOK");
                            System.out.println("**signOK**");
                        }
                        if (cmd.equals("/signNOK")){
                            getAlert ("/signNOK");
                            System.out.println("**signNOK**");
                        }
                        if (cmd.equals("/delOK")){
                            getAlert ("/delOK");
                            System.out.println("**delOK**");
                        }
                        if (cmd.equals("/authNOK")){
                            getAlert ("/log");
                            isAuthorized = false;
                            System.out.println("**authNOK**");
                        }
                        if (cmd.equals("/needDiscon")){
                            getAlert ("/discon");
                            System.out.println("**needDisconnect**");
                        }
                        getLabelAuth (isAuthorized);
                    }

                    if (isAuthorized) {
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
        getStageAuth();
    }

    public void getStageAuth () {
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
        if (simpleListView.isFocused()) {
           // System.out.println("startBTN List1");
            if (!isAuthorized) {
                getAlert ("/up");
            }
            if (!Network.isConnected()) {
                return;
            }
            try {
                Network.sendMsg(new FileMessage(Paths.get("client/client_storage/" + simpleListView.getSelectionModel().getSelectedItem())));
                filesDragAndDrop.setText("Drop files here!");
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(simpleListView.getSelectionModel().getSelectedItem());
        } else if (simpleListView2.isFocused()) {
            if (!isAuthorized) {
                getAlert ("/up");
            }
           // System.out.println("startBTN List2");
            if (!Network.isConnected()) {
                return;
            }
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
            getAlert ("/file");
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
            if (!isAuthorized) {
                getAlert ("/up");
            }
            if (!Network.isConnected()) {
                return;
            }
            Network.sendMsg(new Command("/delFile",simpleListView2.getSelectionModel().getSelectedItem()));
           // System.out.println("SendMsgDel");
        } else {
            getAlert ("/del");
        }
        filesDragAndDrop.setText("Drop files here!");
        System.out.println("===DelBTNstop===");
    }

    public void getAlert (String condition) {
        updateUI(() -> {
            text.setLength(0);
            switch (condition) {
                case "/log"     : text.append("Login or password not correct!");
                                break;
                case "/discon"  : text.append("Press button Disconnect!");
                                break;
                case "/up"      : text.append("Need authorization!");
                                break;
                case "/del"     : text.append("Select the file from to delete.");
                                break;
                case "/file"    : text.append("Select the file to download.");
                                break;
                case "/signOK"  : text.append("User is created.");
                                break;
                case "/delOK"   : text.append("User is deleted!");
                                break;
                case "/signNOK" : text.append("This login is busy!");
                                break;
                default: return;
            }
            Alert alert = new Alert(Alert.AlertType.NONE, text.toString(), ButtonType.OK);
            Optional<ButtonType> result = alert.showAndWait();
            if (condition.equals("/log")||condition.equals("/discon")) {
                getStageAuth();
            }
        });
    }

    public void getLabelAuth (boolean auth) {
        updateUI(() -> {
            if (auth) {
                labelAutorizeNOK.setText("Autorized!");
            } else {
                labelAutorizeNOK.setText("!Autorized");
            }
        });
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
