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
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javafx.application.Application;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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

    enum Side {

	LEFT, RIGHT, BOTH
    };

    // JavaFX components
    private Label statusBar = new Label("");
    private TableView<TableRow> resultTable = new TableView<>();

    private TextField dir1Field = new TextField();
    private TextField dir2Field = new TextField();
    Map<String, TableRow> map = new HashMap<>();

    private Path root1Dir, root2Dir;
    private Side currentSide;
    private Stage stage;
    private Scene scene;
    private EventHandler<ActionEvent> dirEventHandler = new EventHandler<ActionEvent>() {
	@Override
	public void handle(ActionEvent event) {
	    Button btn = (Button) event.getSource();
	    logger.fine("RootDir is: " + root1Dir + " Button: " + btn.getId());

	    Side side = btn.getId().contains("1") ? Side.LEFT : Side.RIGHT;

	    DirectoryChooser dirChooser = new DirectoryChooser();
	    dirChooser.setTitle("Select Root Directory for " + btn.getId());

	    dirChooser.setInitialDirectory(side == Side.LEFT ? root1Dir.toFile() : root2Dir.toFile());
	    File dir = dirChooser.showDialog(stage);
	    if (dir == null) {
		logger.info("No directroy selected.");
		return;
	    }
	    logger.info("Selected dir: " + dir.getAbsolutePath());
	    try {
		setRootDir(side, dir.getAbsolutePath());
		if (side == Side.LEFT) {
		    dir1Field.setText(root1Dir.toString());
		    statusBar.setText("New " + side + " root dir: " + root1Dir.toString());
		} else {
		    dir2Field.setText(root2Dir.toString());
		    statusBar.setText("New " + side + " root dir: " + root2Dir.toString());
		}
	    } catch (IOException e) {
		logger.log(Level.SEVERE, "Error with selected dir: " + dir.getAbsolutePath(), e);
	    }
	}
    };

    /**
     * Class holding table map. Each row contains one file of each dir.
     */
    public static class TableRow {

	public SimpleStringProperty fileName = new SimpleStringProperty();
	public SimpleStringProperty fileType = new SimpleStringProperty();

	public SimpleStringProperty checksum1 = new SimpleStringProperty("");
	public SimpleLongProperty size1 = new SimpleLongProperty(-1L);

	public SimpleStringProperty checksum2 = new SimpleStringProperty("");
	public SimpleLongProperty size2 = new SimpleLongProperty(-1L);

	public SimpleStringProperty status = new SimpleStringProperty("");

	protected Side side;
	private FileTime creationTime;
	private FileTime lastAccessTime;

	public TableRow() {
	}

	public TableRow(String fileName, long size, String checkSum, String fileType, Side side,
		FileTime creationTime, FileTime lastAccessTime) {
	    setFileName(fileName);
	    setSide(side);
	    setFileType(fileType);

	    this.creationTime = creationTime;
	    this.lastAccessTime = lastAccessTime;

	    if (side == Side.LEFT) {
		setSize1(size);
		setChecksum1(checkSum);
	    } else {
		setSize2(size);
		setChecksum2(checkSum);
	    }
	}

	public Side getSide() {
	    return side;
	}

	public final void setSide(Side side) {
	    this.side = side;
	}

	// common props
	public final void setFileName(String fileName) {
	    this.fileName.set(fileName);
	}

	public String getFileName() {
	    return this.fileName.get();
	}

	public String getFileType() {
	    return fileType.get();
	}

	public final void setFileType(String fileType) {
	    this.fileType.set(fileType);
	}

	// left side
	public String getChecksum1() {
	    return this.checksum1.get();
	}

	public final void setChecksum1(String checksum) {
	    this.checksum1.set(checksum);
	}

	public Long getSize1() {
	    return size1.get();
	}

	public final void setSize1(long size) {
	    this.size1.set(size);
	}

	// right Side
	public String getChecksum2() {
	    return this.checksum2.get();
	}

	public final void setChecksum2(String checksum) {
	    this.checksum2.set(checksum);
	}

	public long getSize2() {
	    return size2.get();
	}

	public final void setSize2(long size) {
	    this.size2.set(size);
	}

	private void setFile2Attributes(long size, String checkSum) {
	    this.side = Side.BOTH;
	    setSize2(size);
	    setChecksum2(checkSum);
	}

	public String getStatus() {
	    // Check if either left or right side is missing
	    switch (getSide()) {
		case LEFT:
		    return "RIGHT MISSING";
		case RIGHT:
		    return "LEFT MISSING";
	    }

	    // File exists in both directories, check for identity.
	    boolean identical = (getSize1() == getSize2()) && (getChecksum1().compareTo(getChecksum2()) == 0);
	    this.status.set(identical ? "EQUAL" : "DIFFERENT");
	    return this.status.get();
	}

	@Override
	public String toString() {
	    return "" + side + " " + getFileName()
		    + " " + getSize1() + " " + getChecksum1()
		    + " " + getSize2() + " " + getChecksum2();
	}
    }

    /**
     * Creates table for holding TableRows
     */
    private TableView createTable() {
	TableView<TableRow> table = new TableView<>();

	TableColumn<TableRow, String> colFileName = new TableColumn<>("Filename");
	colFileName.setPrefWidth(400);
	colFileName.setCellValueFactory(new PropertyValueFactory<TableRow, String>("fileName"));

	TableColumn<TableRow, String> colType = new TableColumn<>("Type");
	colType.setCellValueFactory(new PropertyValueFactory<TableRow, String>("fileType"));

	TableColumn<TableRow, Long> colSize1 = new TableColumn<>("Size1");
	colSize1.setCellValueFactory(new PropertyValueFactory<TableRow, Long>("size1"));

	TableColumn<TableRow, String> colChecksum1 = new TableColumn<>("Checksum1");
	colChecksum1.setPrefWidth(250);
	colChecksum1.setCellValueFactory(new PropertyValueFactory("checksum1"));

	TableColumn<TableRow, Long> colSize2 = new TableColumn<>("Size2");
	colSize2.setCellValueFactory(new PropertyValueFactory<TableRow, Long>("size2"));

	TableColumn<TableRow, String> colChecksum2 = new TableColumn<>("Checksum2");
	colChecksum2.setPrefWidth(250);
	colChecksum2.setCellValueFactory(new PropertyValueFactory("checksum2"));

	TableColumn<TableRow, String> colStatus = new TableColumn<>("Status");
	//colStatus.setPrefWidth(250);
	colStatus.setCellValueFactory(new PropertyValueFactory("status"));

	table.getColumns().addAll(colFileName, colType, colChecksum1, colSize1, colStatus, colChecksum2, colSize2);

	return table;
    }

    /**
     * Main function of this calls. Will lunch JavaFX
     *
     * @param args First optional argument can define root directory for check run.
     *
     * @throws Exception
     */
    static public void main(String[] args) throws Exception {
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
	configureLogging(Level.FINER);

	List<String> params = getParameters().getRaw();

	if (params.size() != 2) {
	    logger.info("Usage: java com.anowak.checker.multimedia.MultiMediaChecker <rootDir1> <rootDir2>");
	    setRootDir(Side.LEFT, ".");
	    setRootDir(Side.RIGHT, ".");
	} else {
	    setRootDir(Side.LEFT, params.get(0));
	    setRootDir(Side.RIGHT, params.get(1));
	}

	statusBar.setMaxWidth(Double.MAX_VALUE);
	statusBar.setText("LEFT DIR: " + root1Dir.toString() + " RIGHT DIR: " + root2Dir.toString());

	Button startBtn = new Button("Start");
	startBtn.setOnAction(new EventHandler<ActionEvent>() {
	    @Override
	    public void handle(ActionEvent event) {
		try {
		    map.clear();

		    setRootDir(Side.LEFT, dir1Field.getText());
		    setRootDir(Side.RIGHT, dir2Field.getText());

		    statusBar.setText("Searching dir1: " + dir1Field.getText() + " ...");
		    navigateDirs(root1Dir, Side.LEFT);
		    statusBar.setText("Searching dir2: " + dir2Field.getText() + " ...");
		    navigateDirs(root2Dir, Side.RIGHT);

		    logger.info("Number of items in map: " + map.size());

		    resultTable.setItems(FXCollections.observableArrayList(map.values()));
		} catch (IOException e) {
		    logger.log(Level.SEVERE, "IOException caught: ", e);
		}
	    }
	});

	Button newBtn = new Button("New");
	newBtn.setOnAction(new EventHandler<ActionEvent>() {
	    @Override
	    public void handle(ActionEvent event) {
		statusBar.setText("Adding table row");
		map.put("/aaa/aaa/aaa", new TableRow("/aaa/aaa/aaa", 1000, "aaa", "aaa", Side.LEFT,
			FileTime.fromMillis(System.currentTimeMillis()), FileTime.fromMillis(System.currentTimeMillis())));
		map.put("/bbb/bbb/bbb", new TableRow("/bbb/bbb/bbb", 1000, "bbb", "bbb", Side.RIGHT,
			FileTime.fromMillis(System.currentTimeMillis()), FileTime.fromMillis(System.currentTimeMillis())));

	    }
	});

	Button clearBtn = new Button("Clear");
	clearBtn.setOnAction(new EventHandler<ActionEvent>() {

	    @Override
	    public void handle(ActionEvent event) {
		statusBar.setText("Clearing table...");
		map.clear();
		resultTable.getItems().clear();
	    }
	});

	Button dirBtn1 = new Button("...");
	dirBtn1.setId("Button1");
	dirBtn1.setOnAction(dirEventHandler);
	dir1Field.setText(root1Dir.toString());
	dir1Field.setMinWidth(400);

	Button dirBtn2 = new Button("...");
	dirBtn2.setId("Button2");
	dirBtn2.setOnAction(dirEventHandler);
	dir2Field.setText(root2Dir.toString());
	dir2Field.setMinWidth(400);

	HBox controls = new HBox();
	HBox.setHgrow(dir1Field, Priority.ALWAYS);
	HBox.setHgrow(dir2Field, Priority.ALWAYS);
	controls.getChildren().addAll(startBtn, dir1Field, dirBtn1, dir2Field, dirBtn2, newBtn, clearBtn);
	controls.setSpacing(10);

	this.resultTable = createTable();

	AnchorPane.setTopAnchor(resultTable, 10.0);
	AnchorPane.setLeftAnchor(resultTable, 10.0);
	AnchorPane.setRightAnchor(resultTable, 10.0);
	AnchorPane.setBottomAnchor(resultTable, 10.0);

	VBox vbox = new VBox(3);
	VBox.setVgrow(resultTable, Priority.ALWAYS);
	vbox.getChildren().addAll(controls, resultTable, statusBar);

	scene = new Scene(vbox);

	primaryStage.setTitle("Multimedia File Consistency Checker");
	primaryStage.setScene(scene);
	primaryStage.sizeToScene();

	this.stage = primaryStage;
	stage.show();
    }

    public Path getRootDir(boolean isDir1) {
	return isDir1 ? root1Dir : root2Dir;
    }

    public void setRootDir(Side side, String rootDir) throws IOException {
	FileSystem fs = FileSystems.getDefault();
	if (side == Side.LEFT) {
	    this.root1Dir = fs.getPath(rootDir);
	    this.root1Dir = this.root1Dir.toRealPath(LinkOption.NOFOLLOW_LINKS);
	} else {
	    this.root2Dir = fs.getPath(rootDir);
	    this.root2Dir = this.root2Dir.toRealPath(LinkOption.NOFOLLOW_LINKS);
	}
	logger.info("Root " + side + " dir: " + rootDir);
    }

    private void navigateDirs(Path rootDir, Side side) throws IOException {
	logger.info("Getting list of " + side + " files ...");

	this.currentSide = side;

	PrintFiles pf = new PrintFiles();
	Files.walkFileTree(rootDir, pf);

	logger.info("======================================================");
	logger.info("= Dir: " + rootDir);
	logger.info("= Statistics: " + pf.getNumberProcessedDirectories()
		+ " directories " + pf.getNumberProcessedFiles() + " files");
	for (String k : map.keySet()) {
	    logger.fine(k + ": " + map.get(k));
	}
	logger.info("======================================================");

	statusBar.setText("DONE! " + pf.getNumberProcessedDirectories() + " dirs, "
		+ pf.getNumberProcessedFiles() + " files");
    }

    private class PrintFiles extends SimpleFileVisitor<Path> {

	private int nFiles = 0;
	private int nDirs = 0;

	// Print information about
	// each type of file.
	@Override
	public FileVisitResult visitFile(Path file,
		BasicFileAttributes attr) throws IOException {

	    StringBuilder log = new StringBuilder();

	    log.append("[" + nFiles + "] " + currentSide + " ");

	    if (attr.isSymbolicLink()) {
		log.append("Symbolic link: " + file);
	    } else if (attr.isRegularFile()) {
		log.append("Regular file: " + file);
	    } else {
		log.append("Other: " + file);
	    }
	    String contentType = Files.probeContentType(file);
	    String checkSum = checksum(file.toFile());
	    log.append(" (" + attr.size() + " bytes) "
		    + "Type: " + ((contentType == null) ? "unknown" : contentType));
	    log.append(" MD5=" + checkSum + "\n");
	    logger.finer(log.toString());

	    /**
	     * **** for testing... System.out.print(" MD2 =" + checksum(file.toFile(),"MD2") + "\n");
	     * System.out.print(" SHA-1 =" + checksum(file.toFile(),"SHA-1") + "\n"); System.out.print(" SHA-256=" +
	     * checksum(file.toFile(),"SHA-256") + "\n"); System.out.print(" SHA-384=" +
	     * checksum(file.toFile(),"SHA-384") + "\n"); System.out.print(" SHA-512=" +
	     * checksum(file.toFile(),"SHA-512") + "\n"); ***
	     */
	    String key = getKey(file.toString());
	    if (map.containsKey(key)) {
		TableRow media = map.get(key);
		media.setFile2Attributes(attr.size(), checkSum);
	    } else {
		TableRow media = new TableRow(key, attr.size(), checkSum, contentType, currentSide,
			attr.creationTime(), attr.lastModifiedTime());
		TableRow prev = map.put(key, media);
	    }
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
	    logger.fine("Directory " + nDirs + ": " + dir);
	    nDirs++;
	    return CONTINUE;
	}

	/**
	 * If there is some error accessing the file, let the user know. If you don't override this method and an error
	 * occurs, an IOException is thrown.
	 *
	 * @param file - reference to file with error
	 * @param exc - IO exception
	 * @return the visit result
	 */
	@Override
	public FileVisitResult visitFileFailed(Path file,
		IOException ex) {
	    logger.log(Level.SEVERE, "Error while navigating " + file, ex);
	    return CONTINUE;
	}

	private String getKey(String fullpath) {
	    String key;
	    if (currentSide == Side.LEFT) {
		logger.finest("fullpath=" + fullpath + " side=" + currentSide + " rootDir=" + root1Dir);
		key = fullpath.substring(root1Dir.toString().length() + 1);
	    } else {
		logger.finest("fullpath=" + fullpath + " side=" + currentSide + " rootDir=" + root2Dir);
		key = fullpath.substring(root2Dir.toString().length() + 1);
	    }
	    logger.finest("\tkey=" + key);
	    return key;
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
	return checksum(file, "MD5");
    }

    /**
     * Checksum of a file contents based on different algorithms. This will read the <it>entire</it> file.
     *
     * @param file The file
     * @param algorithm Algorithm used for building checksum: "MD2","MD5","SHA-1","SHA-256","SHA-384","SHA-512"
     * @return Hex checksum string starting with "0x<it>&lt;hex_value&gt;</it>"
     */
    static public String checksum(File file, String algorithm) {
	try {
	    java.security.MessageDigest msgDigest;
	    try (InputStream fin = new FileInputStream(file)) {
		msgDigest = MessageDigest.getInstance(algorithm);
		byte[] buffer = new byte[1024];
		int read;
		do {
		    read = fin.read(buffer);
		    if (read > 0) {
			msgDigest.update(buffer, 0, read);
		    }
		} while (read != -1);
	    }
	    byte[] digest = msgDigest.digest();
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

    private void configureLogging(Level level) {
	// removing all default handlers
	Handler handlers[] = logger.getHandlers();
	System.out.println("Number of logger handlers: " + handlers.length + " useParentHandler=" + logger.getUseParentHandlers());
	logger.setUseParentHandlers(false);

	for (Handler handler : handlers) {
	    System.out.println("Removing log handler: " + handler.toString() + "...");
	    logger.removeHandler(handler);
	}

	ConsoleHandler handler = new ConsoleHandler();
	handler.setLevel(level);
	handler.setFormatter(new SimpleFormatter() {
	    @Override
	    public synchronized String format(LogRecord record) {
		return String.format("%1$-7.7s %2$-20.20s: %3$s\n", record.getLevel(), record.getSourceMethodName(), record.getMessage());
	    }
	});

	logger.addHandler(handler);
	logger.setLevel(level);
    }
}
