/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.anowak.checker.multimedia;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.CONTINUE;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 *
 * @author Alex
 */
public class MultiMediaChecker extends Application {

    static final Logger logger = Logger.getLogger(MultiMediaChecker.class.getName());
    private Path rootDir;

    // JavaFX components
    private Label statusBar = new Label("");
    private volatile TableView<MediaFile> table = new TableView<>();
    private TextField dirField = new TextField();
    private Stage stage;
    private Scene scene;

    ObservableList<MediaFile> data = FXCollections.observableArrayList();

    /**
     * Main function of this calls. Will lunch JavaFX
     *
     * @param args First optional argument can define root directory for check
     * run.
     *
     * @throws Exception
     */
    static public void main(String[] args) throws Exception {
        if (args.length != 1) {
            logger.severe("Usage: java com.anowak.checker.audio.Mp3Checker <mp3file>");
            System.exit(-1);
        }
        Application.launch(args);
    }

    /**
     * Entry method upon start JavaFX app.
     *
     * @param primaryStage top-level stage
     * @throws Exception
     */
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
                try {
                    navigateDirs();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "IOException caught: ", e);
                }
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
        HBox.setHgrow(dirField, Priority.ALWAYS);
        controls.getChildren().addAll(startButton, dirField, dirButton);
        controls.setSpacing(10);

        configureTable();

        VBox vbox = new VBox(3);
        VBox.setVgrow(table, Priority.ALWAYS);
        vbox.getChildren().addAll(controls, table, statusBar);

        AnchorPane pane = new AnchorPane();
        AnchorPane.setTopAnchor(vbox, 10.0);
        AnchorPane.setLeftAnchor(vbox, 10.0);
        AnchorPane.setRightAnchor(vbox, 10.0);
        AnchorPane.setBottomAnchor(vbox, 10.0);

        pane.getChildren().addAll(vbox);

        scene = new Scene(pane);

        primaryStage.setTitle("Multimedia File Consistency Checker");
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();

        this.stage = primaryStage;

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
        logger.info("Getting list of files ...");

        data.clear();

        // adding 2 test rows:
        data.add(new MediaFile(FileSystems.getDefault().getPath("/aaa/aaa/aaa"), 1000, "aaa", "bbb"));
        sleep(1000);
        data.add(new MediaFile(FileSystems.getDefault().getPath("/bbb/bbb/bbb"), 2000, "bbb", "ccc"));
        sleep(1000);

        Provider[] providers = Security.getProviders();
        for (Provider p : providers) {
            System.out.println(p.getName() + " : " + p.getInfo());
        }

        PrintFiles pf = new PrintFiles();
        Files.walkFileTree(rootDir, pf);

        logger.info("======================================================");
        System.out.println("= Statistics: " + pf.getNumberProcessedDirectories()
                + " directories " + pf.getNumberProcessedFiles() + " files");
        logger.info("======================================================");

        statusBar.setText("DONE! " + pf.getNumberProcessedDirectories() + " dirs, " + pf.getNumberProcessedFiles() + " files");

    }

    private void printSizes(String name, Control control) {
        logger.info(name
                + ": width=" + control.getWidth()
                + " minWidth=" + control.getMinWidth()
                + " maxWidth=" + control.getMaxWidth()
                + " prefWidth=" + control.getPrefWidth());
    }

    /**
     * Defines table layout and column definition.
     */
    private void configureTable() {
        table = new TableView<MediaFile>(data);

        TableColumn<MediaFile, String> colFileName = new TableColumn<>("Filename");
        colFileName.setPrefWidth(600);
        colFileName.setCellValueFactory(new PropertyValueFactory<MediaFile, String>("fileName"));

        TableColumn<MediaFile, Long> colSize = new TableColumn<>("Size");
        colSize.setCellValueFactory(new PropertyValueFactory<MediaFile, Long>("size"));

        TableColumn<MediaFile, String> colChecksum = new TableColumn<>("Checksum");
        colChecksum.setPrefWidth(250);
        colChecksum.setCellValueFactory(new PropertyValueFactory("checksum"));

        TableColumn<MediaFile, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(new PropertyValueFactory<MediaFile, String>("fileType"));

        table.getColumns().addAll(colFileName, colType, colChecksum, colSize);

    }

    private class PrintFiles extends SimpleFileVisitor<Path> {

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
            String checkSum = checksum(file.toFile());
            System.out.print("(" + attr.size() + " bytes) "
                    + "Type: " + ((contentType == null) ? "unknown" : contentType));
            System.out.print(" MD5=" + checkSum + "\n");

            MediaFile media = new MediaFile(file, attr.size(), checkSum, contentType);
            data.add(media);
            sleep(500);

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

    private void sleep(long miliSecs) {
//        try {
//            Thread.sleep(miliSecs);
//        } catch (InterruptedException ex) {
//            logger.log(Level.SEVERE, "Sleep exception: ", ex);
//        }
    }

    static public String checksum(File file) {
        try {
            java.security.MessageDigest md5er;
            try (InputStream fin = new FileInputStream(file)) {
                md5er = MessageDigest.getInstance("MD5");  // MD5, or CRC32
                byte[] buffer = new byte[1024];
                int read;
                do {
                    read = fin.read(buffer);
                    if (read > 0) {
                        md5er.update(buffer, 0, read);
                    }
                } while (read != -1);
            }
            byte[] digest = md5er.digest();
            if (digest == null) {
                return null;
            }
            String strDigest = "0x";
            for (int i = 0; i < digest.length; i++) {
                strDigest += Integer.toString((digest[i] & 0xff)
                        + 0x100, 16).substring(1).toUpperCase();
            }
            return strDigest;
        } catch (IOException | NoSuchAlgorithmException e) {
            System.out.println("Error:");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Class holding table data. Each row contains a file.
     */
    public static class MediaFile {

        public SimpleStringProperty fileName = new SimpleStringProperty();
        public SimpleStringProperty fileType = new SimpleStringProperty();
        public SimpleStringProperty checksum = new SimpleStringProperty();
        public SimpleLongProperty size = new SimpleLongProperty();

        public MediaFile() {
        }

        public MediaFile(String fileName) {
            this.fileName.set(fileName);
        }

        public MediaFile(Path file) {
            this.fileName = new SimpleStringProperty(file.toString());
        }

        public MediaFile(Path file, long size, String checkSum, String fileType) {
            this.fileName.set(file.toString());
            this.size.set(size);
            this.checksum.set(checkSum);
            this.fileType.set(fileType);
        }

        public String getFileName() {
            return this.fileName.get();
        }

        public void setFileName(String fileName) {
            this.fileName.set(fileName);
        }

        public String getFileType() {
            return fileType.get();
        }

        public void setFileType(String fileType) {
            this.fileType.set(fileType);
        }

        public String getChecksum() {
            return this.checksum.get();
        }

        public void setChecksum(String checksum) {
            this.checksum.set(checksum);
        }

        public Long getSize() {
            return size.get();
        }

        public void setSize(long size) {
            this.size.set(size);
        }
    }

}
