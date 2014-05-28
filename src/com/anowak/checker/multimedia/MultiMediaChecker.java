/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.anowak.checker.multimedia;

import java.io.BufferedInputStream;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javafx.application.Application;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * Compare 2 directories. Determine checksum of each file.
 *
 * @author Alex
 */
public class MultiMediaChecker extends Application {

    static final Logger logger = Logger.getLogger(MultiMediaChecker.class.getName());
    private long totalTime;

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
		    displayMessage("New " + side + " root dir: " + root1Dir.toString());
		} else {
		    dir2Field.setText(root2Dir.toString());
		    displayMessage("New " + side + " root dir: " + root2Dir.toString());
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
	private Date creationTime1;
	private Date lastAccessTime1;
	public SimpleStringProperty creationTimeString1 = new SimpleStringProperty("");
	private Date creationTime2;
	private Date lastAccessTime2;
	public SimpleStringProperty creationTimeString2 = new SimpleStringProperty("");

	public TableRow() {
	}

	public TableRow(String fileName, long size, String checkSum, String fileType, Side side,
		FileTime creationTime, FileTime lastAccessTime) {
	    setFileName(fileName);
	    setSide(side);
	    setFileType(fileType);

	    if (side == Side.LEFT) {
		setSize1(size);
		setChecksum1(checkSum);
		this.creationTime1 = new Date(creationTime.toMillis());
		this.lastAccessTime1 = new Date(lastAccessTime.toMillis());
		setCreationTimeString1(formatDate(this.creationTime1));
	    } else {
		setSize2(size);
		setChecksum2(checkSum);
		this.creationTime2 = new Date(creationTime.toMillis());
		this.lastAccessTime2 = new Date(lastAccessTime.toMillis());
		setCreationTimeString1(formatDate(this.creationTime2));
	    }
	}

	public Date getCreationTime1() {
	    return this.creationTime1;
	}

	public Date getLastAccessTime1() {
	    return this.lastAccessTime1;
	}

	public Date getLastAccessTime2() {
	    return this.lastAccessTime2;
	}

	public Date getCreationTime2() {
	    return this.creationTime2;
	}

	public void setCreationTimeString1(String creationTime) {
	    this.creationTimeString1.set(creationTime);
	}

	public void setCreationTimeString2(String creationTime) {
	    this.creationTimeString2.set(creationTime);
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

	private void setFile2Attributes(long size, String checkSum,
		FileTime creationTime, FileTime lastAccessTime) {
	    this.side = Side.BOTH;
	    setSize2(size);
	    setChecksum2(checkSum);
	    this.creationTime2 = new Date(creationTime.toMillis());
	    this.lastAccessTime2 = new Date(lastAccessTime.toMillis());
	    setCreationTimeString1(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(this.creationTime2));
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
	colStatus.setPrefWidth(100);
	colStatus.setCellValueFactory(new PropertyValueFactory("status"));

	TableColumn<TableRow, String> colCreationDate1 = new TableColumn<>("Date1");
	colCreationDate1.setPrefWidth(125);
	colCreationDate1.setComparator(new Comparator<String>() {
	    @Override
	    public int compare(String t0, String t1) {
		return compareDates(t0, t1);
	    }
	});
	colCreationDate1.setCellValueFactory(new Callback<CellDataFeatures<TableRow, String>, ObservableValue<String>>() {
	    @Override
	    public ObservableValue<String> call(CellDataFeatures<TableRow, String> param) {
		return new SimpleObjectProperty(formatDate(param.getValue().getLastAccessTime1()));
	    }
	});

	TableColumn<TableRow, String> colCreationDate2 = new TableColumn<>("Date2");
	colCreationDate2.setPrefWidth(130);
	colCreationDate2.setComparator(new Comparator<String>() {
	    @Override
	    public int compare(String t0, String t1) {
		return compareDates(t0, t1);
	    }
	});
	colCreationDate2.setCellValueFactory(new Callback<CellDataFeatures<TableRow, String>, ObservableValue<String>>() {
	    @Override
	    public ObservableValue<String> call(CellDataFeatures<TableRow, String> param) {
		return new SimpleObjectProperty(formatDate(param.getValue().getLastAccessTime2()));
	    }
	});

	table.getColumns().addAll(colFileName, colType, colChecksum1, colSize1, colCreationDate1, colStatus,
		colChecksum2, colSize2, colCreationDate2);

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
	displayMessage("LEFT DIR: " + root1Dir.toString() + " RIGHT DIR: " + root2Dir.toString());

	Button startBtn = new Button("Start");
	startBtn.setOnAction(new EventHandler<ActionEvent>() {
	    @Override
	    public void handle(ActionEvent event) {
		try {
		    map.clear();

		    setRootDir(Side.LEFT, dir1Field.getText());
		    setRootDir(Side.RIGHT, dir2Field.getText());

		    if (getRootDir(true).toAbsolutePath().toString().indexOf(getRootDir(false).toAbsolutePath().toString()) != -1
			    || getRootDir(false).toAbsolutePath().toString().indexOf(getRootDir(true).toAbsolutePath().toString()) != -1) {
			// TODO: Display warning dialog
			String msg = "Paths are within the same branch. Dosen't make sense, eh?";
			logger.warning(msg);
			displayMessage(msg);
			return;
		    }

		    displayMessage("Searching dir1: " + dir1Field.getText() + " ...");
		    navigateDirs(root1Dir, Side.LEFT);
		    displayMessage("Searching dir2: " + dir2Field.getText() + " ...");
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
		displayMessage("Adding table row");
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
		displayMessage("Clearing table...");
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
	controls.setPadding(new Insets(10));

	this.resultTable = createTable();

	AnchorPane.setTopAnchor(resultTable, 10.0);
	AnchorPane.setLeftAnchor(resultTable, 10.0);
	AnchorPane.setRightAnchor(resultTable, 10.0);
	AnchorPane.setBottomAnchor(resultTable, 10.0);

	VBox vbox = new VBox();
	VBox.setVgrow(resultTable, Priority.ALWAYS);
	vbox.getChildren().addAll(controls, resultTable, statusBar);
	vbox.setPadding(new Insets(10));

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
	long startTime = System.currentTimeMillis();


	this.currentSide = side;

	PrintFiles pf = new PrintFiles();
	Files.walkFileTree(rootDir, pf);

	long stopTime = System.currentTimeMillis();
	totalTime = stopTime-startTime;

	
	logger.info("======================================================");
	logger.info("= Dir: " + rootDir);
	logger.info("= Statistics: " + pf.getNumberProcessedDirectories()
		+ " directories " + pf.getNumberProcessedFiles() + " files");
	for (String k : map.keySet()) {
	    logger.fine(k + ": " + map.get(k));
	}
	logger.info("======================================================");

	displayMessage("DONE! " + pf.getNumberProcessedDirectories() + " dirs, "
		+ pf.getNumberProcessedFiles() + " files processed in " + totalTime/1000.0 + " seconds");
    }

    private class PrintFiles extends SimpleFileVisitor<Path> {
	int nFiles = 0;
	int nDirs = 0;
	private MessageDigest msgDigest=null;
	private long msecs;

    private MessageDigest getMessageDigest(String algorithm) throws NoSuchAlgorithmException {
	if (msgDigest!=null) {
	    msgDigest.reset();
	    return msgDigest;
	}
	msgDigest = MessageDigest.getInstance(algorithm);
	return msgDigest;
    }

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
	    log.append(" MD5=" + checkSum + " " + attr.lastModifiedTime() + " " + attr.creationTime() + "\n");
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
		media.setFile2Attributes(attr.size(), checkSum, attr.lastModifiedTime(), attr.lastModifiedTime());
	    } else {
		TableRow media = new TableRow(key, attr.size(), checkSum, contentType, currentSide,
			attr.lastModifiedTime(), attr.lastModifiedTime());
		map.put(key, media);
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

	/**
	 * Print each directory visited.
	 * @param dir The directory
	 * @param exc Exception
	 * @return 
	 */
	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
	    logger.fine("#"+nDirs + " Directory: " + dir);
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
	public FileVisitResult visitFileFailed(Path file, IOException ex) {
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
	
	public String checksum(File file) {
	    return checksum(file, "MD5");
	}

	/**
	 * Checksum of a file contents based on different algorithms. This will read the <it>entire</it> file.
	 *
	 * @param file The file
	 * @param algorithm Algorithm used for building checksum: "MD2","MD5","SHA-1","SHA-256","SHA-384","SHA-512"
	 * @return Hex checksum string starting with "0x<it>&lt;hex_value&gt;</it>"
	 */
	public String checksum(File file, String algorithm) {
	    try {
		try (BufferedInputStream fin = new BufferedInputStream(new FileInputStream(file))) {
		    MessageDigest msgDigest = getMessageDigest(algorithm);
		    byte[] buffer = new byte[1024];
		    int read;
		    startTimer();
		    long size=0;
		    do {
			read = fin.read(buffer);
			if (read > 0) {
			    size++;
			    msgDigest.update(buffer, 0, read);
			}
		    } while (read != -1);
		    stopTimer(file.length());
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
		String msg = "Error building checksum of " + file.getAbsolutePath();
		logger.log(Level.SEVERE, msg, e);
		return null;
	    }
	}

	private void startTimer() {
	    msecs = System.currentTimeMillis();
	}

	private void stopTimer(long size) {
	    long curTime = System.currentTimeMillis();
	    double durationInSec = (curTime*1.0 - msecs*1.0)/1000.0;
	    double speed = (size/1.0E6)/durationInSec;
	    logger.fine("Duration: " + durationInSec + " sec, Size: " + size/1.0E6 + " MB,  Speed: " + speed + " MB/s");
	}
    }

    private void sleep(long miliSecs) {
//        try {
//            Thread.sleep(miliSecs);
//        } catch (InterruptedException ex) {
//            logger.log(Level.SEVERE, "Sleep exception: ", ex);
//        }
    }

    static public int compareDates(String t0, String t1) {
	if ((t0 == null || t0.isEmpty()) && (t1 == null || t1.isEmpty())) {
	    return 0;  // t0 == t1
	}
	if ((t0 == null || t0.isEmpty()) && (t1 != null && !t1.isEmpty())) {
	    return -1; // t0 > t1
	}
	if ((t0 != null && !t0.isEmpty()) && (t1 == null || t1.isEmpty())) {
	    return 1; // t0 < t1
	}
	try {
	    SimpleDateFormat format = new SimpleDateFormat("MM/dd/YYYY HH:mm:ss");
	    Date d1 = format.parse(t0);
	    Date d2 = format.parse(t1);
	    return Long.compare(d1.getTime(), d2.getTime());
	} catch (ParseException p) {
	    logger.log(Level.INFO, "Date parse exception (t0=" + t0 + ",t1=" + t1 + ")", p);
	}
	return 0;
    }

    static public String formatDate(Date date) {
	String dateString = "";
	if (date != null) {
	    SimpleDateFormat format = new SimpleDateFormat("MM/dd/YYYY HH:mm:ss");
	    dateString = format.format(date);
	}
	//logger.finer("CreationDate: " + dateString);
	return dateString;
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

    public void displayMessage(String msg) {
	statusBar.setText(msg);
    }
}