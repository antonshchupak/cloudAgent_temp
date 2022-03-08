package ru.geekbrains.cloudAgent.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import ru.geekbrains.cloudAgent.common.AbstractMessage;
import ru.geekbrains.cloudAgent.common.FileInfo;
import ru.geekbrains.cloudAgent.common.FileMessage;
import ru.geekbrains.cloudAgent.common.FileRequest;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {

    @FXML
    TextField filenameField, loginField;

    @FXML
    PasswordField passwordField;

    @FXML
    HBox loginPanel, filePanel, btnBar;

    @FXML
    ListView<FileInfo> filesListPanel;


    private Network network;
    private String username;
    Path root;
    Path selectedDownloadFile;
    Path selectedUploadFile;


    // описываем подключение клиента
    public void login() {
        if (loginField.getText().isEmpty()) {
            showErrorAlert("Имя пользователя не может быть пустым");
            return;
        }
        try {
            network.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            network.tryToLogin(loginField.getText(), passwordField.getText());
        } catch (IOException e) {
            showErrorAlert("Невозможно отправить данные пользователя");
        }
    }

    //скрываем панель аутентификации

    public void setUsername(String username) {
        this.username = username;
        boolean usernameIsNull = username == null;
        loginPanel.setVisible(usernameIsNull);
        loginPanel.setManaged(usernameIsNull);
        filePanel.setVisible(!usernameIsNull);
        filePanel.setManaged(!usernameIsNull);
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.setTitle("Cloud Agent");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    //готовим файлы для отображения в листвью
    public void filesFormattedView() {
        filesListPanel.setCellFactory(new Callback<ListView<FileInfo>, ListCell<FileInfo>>() {
            @Override
            public ListCell<FileInfo> call(ListView<FileInfo> param) {
                return new ListCell<FileInfo>() {
                    @Override
                    protected void updateItem(FileInfo item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setText(null);
                            setStyle("");
                        } else {
                            String formattedFilename = String.format("%-64s", item.getFilename());
                            String formattedFileLength = String.format("%,d bytes", item.getLength());
                            if (item.getLength() == -1L) {
                                formattedFileLength = String.format("%s", "[ DIR ]");
                            }
                            String text = String.format("%s %-20s", formattedFilename, formattedFileLength);
                            setText(text);
                        }
                    }
                };
            }
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            network.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        filesFormattedView(); //выводим список файлов на сервере после подключения клиента
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    AbstractMessage am = network.readObject();
                    if (am instanceof FileMessage) {
                        FileMessage fm = (FileMessage) am;
                        Files.write(Paths.get("client-repo/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                        refreshFilesList(); //обновляем список файлов на сервере
                    }
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                network.disconnect();
            }
        });
        t.setDaemon(true);
        t.start();
        refreshFilesList(); //обновляем список файлов на сервере
    }

    public void uploadFile(ActionEvent actionEvent) {
        FileInfo fileInfo = filesListPanel.getSelectionModel().getSelectedItem();
        if (fileInfo == null) {
            return;
        }
        if (selectedDownloadFile == null) {
            selectedDownloadFile = root.resolve(fileInfo.getFilename());
            return;
        }
        if (selectedDownloadFile != null) {
            try {
                network.readObject();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Unable to upload file: " + fileInfo.getFilename());
                alert.showAndWait();
            }

        }
    }

    public void downloadFile(ActionEvent actionEvent) {
        FileInfo fileInfo = filesListPanel.getSelectionModel().getSelectedItem();
        if (fileInfo == null) {
            return;
        }
        if (selectedDownloadFile == null) {
            selectedDownloadFile = root.resolve(fileInfo.getFilename());
            return;
        }
        if (selectedDownloadFile != null) {
            try {
                network.sendMsg(new FileRequest(fileInfo.getFilename()));
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Unable to download file: " + fileInfo.getFilename());
                alert.showAndWait();
            }

        }
    }

    public void showOnClientFiles(ActionEvent actionEvent) {
        goToPath(Paths.get("client-repo"));
    }

    public void showOnServerFiles(ActionEvent actionEvent) {
        goToPath(Paths.get("server-repo"));
    }

    public void logout(ActionEvent actionEvent) {
        network.disconnect();
    }

    public void exit() {
        network.disconnect();
    }


    public List<FileInfo> scanFiles(Path root) {
        try {
            return Files.list(root).map(FileInfo::new).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Something wrong with scan files: " + root);
        }
    }

    public void goToPath(Path path) {
        root = path;
        filenameField.setText(root.toAbsolutePath().toString());
        filesListPanel.getItems().clear();
        filesListPanel.getItems().addAll(scanFiles(path));
    }

    public void refreshFilesList() {
        goToPath(root);
    }

}

