/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.anowak.checker.multimedia;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 *
 * @author Alex
 */
public class MultiMediaChecker extends Application {

    static final Logger logger = Logger.getLogger(MultiMediaChecker.class.getName());
    private Path rootDir;

    // JavaFX components
    Label statusBar = new Label("");
    TableView<MediaFile> table = new TableView<MediaFile>();
    TextField dirField = new TextField();
    private Stage stage;
    Scene scene;

    static public void main(String[] args) throws Exception {
        if (args.length != 1) {
            logger.severe("Usage: java com.anowak.checker.audio.Mp3Checker <mp3file>");
            System.exit(-1);
        }
        Application.launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.setLevel(Level.FINEST);

        List<String> params = getParameters().getRaw();

        if (params.size() != 1) {
            logger.info("Usage: java com.anowak.checker.multimedia.MultiMediaChecker <rootDir>");
            setRootDir(".");
        } else {
            setRootDir(params.get(0));
        }

        statusBar.setMaxWidth(Double.MAX_VALUE);
        statusBar.setText("Root Dir: " + rootDir.toString());


        Button startButton = new Button("Start");
        startButton.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                statusBar.setText("Searching dir: " + dirField.getText() + " ...");
            }
        });

        Button dirButton = new Button("...");
        dirButton.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                System.out.println("Logger Level: " + logger.getLevel());
                logger.fine("RootDir is: " + rootDir);
                DirectoryChooser dirChooser = new DirectoryChooser();
                dirChooser.setTitle("Select Root Directory");
                dirChooser.setInitialDirectory(rootDir.toFile());
                File dir = dirChooser.showDialog(stage);
                if (dir == null) {
                    logger.info("No directroy selected.");
                    return;
                }
                logger.info("Selected dir: " + dir.getAbsolutePath());
                try {
                    setRootDir(dir.getAbsolutePath());
                    dirField.setText(rootDir.toString());
                    statusBar.setText("New root dir: " + rootDir.toString());
                    printSizes("statusBar", statusBar);
                    printSizes("dirField", dirField);

                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error with selected dir: " + dir.getAbsolutePath(), e);
                }

            }
        });

        dirField.setText(rootDir.toString());
        dirField.setMinWidth(400);

        HBox controls = new HBox();
        controls.getChildren().addAll(startButton, dirField, dirButton);
        controls.setSpacing(10);

        table = new TableView<MediaFile>();

        VBox vbox = new VBox(3);
        vbox.getChildren().addAll(controls, table, statusBar);

        //   vbox.setSpacing(10);
//        logger.info("vbox.fillWidthProperty="+vbox.fillWidthProperty());

        AnchorPane pane = new AnchorPane();
        AnchorPane.setTopAnchor(vbox, 10.0);
        AnchorPane.setLeftAnchor(vbox, 10.0);
        AnchorPane.setRightAnchor(vbox, 10.0);
        AnchorPane.setBottomAnchor(vbox, 10.0);
        AnchorPane.setLeftAnchor(controls, 10.0);
        AnchorPane.setRightAnchor(controls, 10.0);

        pane.getChildren().addAll(vbox);

//        AnchorPane anchorpane = new AnchorPane();
     // List should stretch as anchorpane is resized
//     ListView list = new ListView();
//     AnchorPane.setTopAnchor(list, 10.0);
//     AnchorPane.setLeftAnchor(list, 10.0);
//     AnchorPane.setRightAnchor(list, 65.0);
//     AnchorPane.setBottomAnchor(list, 10.0);
//     // Button will float on right edge
//     Button button = new Button("Add");
//     AnchorPane.setTopAnchor(button, 10.0);
//     AnchorPane.setRightAnchor(button, 10.0);
//     anchorpane.getChildren().addAll(list, button);

        scene = new Scene(pane);

        primaryStage.setTitle("Multimedia File Consistency Checker");
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();

        this.stage = primaryStage;

        dirField.setMinWidth(300);
        dirField.setMaxWidth(Double.MAX_VALUE);

        printSizes("statusBar", statusBar);
        printSizes("dirField", dirField);

        stage.show();
    }

    public Path getRootDir() {
        return rootDir;
    }

    public void setRootDir(String rootDir) throws IOException {
        FileSystem fs = FileSystems.getDefault();
        this.rootDir = fs.getPath(rootDir);
        this.rootDir = this.rootDir.toRealPath(LinkOption.NOFOLLOW_LINKS);
        logger.info("Root dir: " + this.rootDir);
    }

    private void navigateDirs() throws IOException {
        ObservableList<MediaFile> mediaFiles = getMultiMediaFiles();
        table.setItems(mediaFiles);

    }

    private ObservableList<MediaFile> getMultiMediaFiles() throws IOException {
        ObservableList<MediaFile> mediaFiles = FXCollections.observableList(getFileList());
        return mediaFiles;
    }

    private List<MediaFile> getFileList() throws IOException {
        List<MediaFile> files = new ArrayList<>();
        files.add(new MediaFile(rootDir));

        PrintFiles pf = new PrintFiles();
        Files.walkFileTree(rootDir, pf);

        logger.info("======================================================");
        System.out.println("= Statistics: " + pf.getNumberProcessedDirectories()
                + " directories " + pf.getNumberProcessedFiles() + " files");
        logger.info("======================================================");

        statusBar.setText("" + pf.getNumberProcessedDirectories() + " dirs, " + pf.getNumberProcessedFiles() + " files");
        return files;
    }

    private void printSizes(String name, Control control) {
        logger.info(name
                + ": width=" + control.getWidth()
                + " minWidth=" + control.getMinWidth()
                + " maxWidth=" + control.getMaxWidth()
                + " prefWidth=" + control.getPrefWidth());
    }

    private static class MediaFile {

        private StringProperty fileName;

        public MediaFile() {
        }

        public MediaFile(String fileName) {
            this.fileName.set(fileName);
        }

        public MediaFile(Path file) {
            this.fileName = new SimpleStringProperty(file.toString());
        }

    }

    private static class PrintFiles extends SimpleFileVisitor<Path> {

        private int nFiles = 0;
        private int nDirs = 0;

        // Print information about
        // each type of file.
        @Override
        public FileVisitResult visitFile(Path file,
                BasicFileAttributes attr) throws IOException {

            System.out.print("[" + nFiles + "] ");

            if (attr.isSymbolicLink()) {
                System.out.format("Symbolic link: %s ", file);
            } else if (attr.isRegularFile()) {
                System.out.format("Regular file: %s ", file);
            } else {
                System.out.format("Other: %s ", file);
            }
            String contentType = Files.probeContentType(file);
            System.out.println("(" + attr.size() + " bytes) "
                    + "Type: " + ((contentType == null) ? "unknown" : contentType));
            nFiles++;
            return CONTINUE;
        }

        public int getNumberProcessedFiles() {
            return nFiles;
        }

        public int getNumberProcessedDirectories() {
            return nDirs;
        }

        // Print each directory visited.
        @Override
        public FileVisitResult postVisitDirectory(Path dir,
                IOException exc) {
            System.out.format("Directory %d: %s%n", nDirs, dir);
            nDirs++;
            return CONTINUE;
        }

        /**
         * If there is some error accessing the file, let the user know. If you
         * don't override this method and an error occurs, an IOException is
         * thrown.
         *
         * @param file - reference to file with error
         * @param exc - IO exception
         * @return the visit result
         */
        @Override
        public FileVisitResult visitFileFailed(Path file,
                IOException exc) {
            System.err.println(exc);
            return CONTINUE;
        }
    }
}
