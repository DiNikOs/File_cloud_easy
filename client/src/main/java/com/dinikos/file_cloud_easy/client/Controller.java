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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
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


//    @FXML
//    TextField tfFileName;
//

    @FXML
    ListView<Person> customCellListView;

    @FXML
    ListView<String> simpleListView;

    @FXML
    ListView<String> simpleListView2;

    @FXML
    TableView<Person> personsTable;

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

    SimpleBooleanProperty btnDisabled = new SimpleBooleanProperty(false);

    private boolean isAuthorized;

    Socket socket;
    DataInputStream in;
    DataOutputStream out;
    StringBuffer sb, sb2;
    //StringBuffer sb2;
    final int BTNSIZE = 50;


    final String IP_ADRESS = "localhost";


    // Выполняется при старте контроллера
    // Для работы этого метода необходимо чтобы контроллер реализовал интерфейс Initializable
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        initializeDragAndDropLabel();
        initializeWindowDragAndDropLabel();
        initializeSceneStyle();
       // initializeSimpleListView();
        btnShowSelectedElement.disableProperty().bind(btnDisabled);
        btnShowSelectedElement.setPrefWidth(BTNSIZE);
        btnDelSelectedElement.setPrefWidth(BTNSIZE);
        sb = new StringBuffer();
       // String auth = "/auth Java";

        Network.start();
      //
//        outMsg.writeObject("/auth java");
//        outMsg.writeObject("Hello server");
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    AbstractMessage am = Network.readObject();
                    if (am instanceof FileMessage) {
                        FileMessage fm = (FileMessage) am;
                        Files.write(Paths.get("client/client_storage/" + fm.getFilename()),
                                fm.getData(), StandardOpenOption.CREATE);
                        refreshLocalFilesList();
                    }
                    if (am instanceof FileList) {
                        FileList fl = (FileList) am;
                        //TODO // transfer
                        System.out.println("FlList= " + fl.getFileList());
                        System.out.println("FlListSize= " + fl.getFileList().size());
                        refreshServerFilesList(fl);
                    }
                    if (am instanceof Command) {
                        String cmd = ((Command) am).getCommand();
                        if (cmd.equals("/authOk")){
                            Network.sendMsg(new Command("/getList", ""));
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
        refreshLocalFilesList();

        initializeFocus(simpleListView,1);
        initializeFocus(simpleListView2,0);
        System.out.println("==INITIALIZE==");
    }

        //TODO // doto
    public void refreshLocalFilesList() {
        updateUI(() -> {
            try {
                //simpleListView.getItems().clear();
                setClearListUser();
                Files.list(Paths.get("client/client_storage")).
                        map(p -> p.getFileName().toString()).
                        forEach(o -> simpleListView.getItems().add(o));
                Network.sendMsg(new Command("/getList", ""));
                System.out.println("simpleList= " + simpleListView.getItems().toString());
                int lenList = simpleListView.getItems().size();
                System.out.println("len list= " + lenList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void refreshServerFilesList(FileList f) {
        updateUI(() -> {
            //simpleListView2.getItems().clear();
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

    // Показывает Alert с возможностью нажатия одной из двух кнопок
    public void btnShowAlert(ActionEvent actionEvent) {
        // Создаем Alert, указываем текст и кнопки, которые на нем должны быть
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you agree?", ButtonType.OK, ButtonType.CANCEL);
        // showAndWait() показывает Alert и блокирует остальное приложение пока мы не закроем Alert
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get().getText().equals("OK")) {
            System.out.println("You clicked OK");
        } else if (result.get().getText().equals("Cancel")) {
            System.out.println("You clicked Cancel");
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

    public void btnShowModal(ActionEvent actionEvent) {
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

    public void initializeSceneStyle() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                mainStackPane.setPadding(new Insets(20, 20, 20, 20));
                mainStackPane.getChildren().get(0).setEffect(new DropShadow(15, Color.BLACK));
            }
        });
    }

    public String initializeFocus(ListView<String> simpleList, int part) {
        refreshLocalFilesList();
        MultipleSelectionModel<String> langsSelectionModel = simpleList.getSelectionModel();
        // устанавливаем слушатель для отслеживания изменений
        langsSelectionModel.selectedItemProperty().addListener(new ChangeListener<String>(){
            public void changed(ObservableValue<? extends String> changed, String oldValue, String newValue){
                System.out.println("SelectedNew: " + newValue);
                System.out.println("SelectedOld: " + oldValue);
                // selectedLbl.setText("Selected: " + newValue);
                sb.setLength(0);
                System.out.println("sb= " + sb);
                if (part==0) sb.append(newValue);
                if (part==1) sb.append(oldValue);
                System.out.println("sb2= " + sb);
            }
        });
        if (sb.equals("")) return null;
        return sb.toString();
    }

    public void btnExit(ActionEvent actionEvent) {
        System.exit(0);
    }

    public void btnShow2SceneStage(ActionEvent actionEvent) {
        Parent root = null;
        try {
            root = FXMLLoader.load(getClass().getResource("/Scene1.fxml"));
            Stage stage = new Stage();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printSelectedItemInListView(ActionEvent actionEvent) {
        System.out.println("==BTN_UPLOAD==");
        System.out.println("action= " + actionEvent.toString());
//TODO // download
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
            //Network.sendMsg(new FileRequest(initializeFocus(simpleListView2, 1)));
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
            // showAndWait() показывает Alert и блокирует остальное приложение пока мы не закроем Alert
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
            File file = new File("client/client_storage/" + initializeFocus(simpleListView, 1));
            if( file.delete()){
                System.out.println("client/client_storage/" + initializeFocus(simpleListView, 1) + " файл удален");
            } else {
                System.out.println("Файла" +  initializeFocus(simpleListView, 1) + " не обнаружен");
            }
             refreshLocalFilesList();
        } else if (simpleListView2.isFocused()) {
            Network.sendMsg(new Command("/delFile",initializeFocus(simpleListView2,0)));
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

    public void changeBindedBoolean(ActionEvent actionEvent) {
        btnDisabled.set(!btnDisabled.get());
    }

    public void setClearListUser() {
        simpleListView.getItems().clear();
    }

    public void setClearListCloud() {
        simpleListView2.getItems().clear();
    }
}
