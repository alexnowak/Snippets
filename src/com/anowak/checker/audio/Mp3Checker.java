/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.anowak.checker.audio;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 *
 * @author Alex
 */
public class Mp3Checker extends Application {

    static final Logger logger = Logger.getLogger(Mp3Checker.class.getName());
    Path mp3File;
    Scene scene;

    public Path getMp3File() {
        return mp3File;
    }

    public void setMp3File(String mp3FileName) throws IOException {
        FileSystem fs = FileSystems.getDefault();
        this.mp3File = fs.getPath(mp3FileName);
        logger.info("mp3 File: " + mp3File.toAbsolutePath() + " fileType: " + Files.probeContentType(mp3File));
    }

    public Scene getScene() {
        return scene;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public Mp3Checker() {
        logger.info("Constructor Mp3Checker() called.");
    }


    static public void main(String[] args) throws Exception {
        if (args.length != 1) {
            logger.severe("Usage: java com.anowak.checker.audio.Mp3Checker <mp3file>");
            System.exit(-1);
        }
        Application.launch(args);
    }

    private void check() throws URISyntaxException {
        logger.info("URI: " + mp3File.toUri());
        Media hit = new Media(mp3File.toUri().toASCIIString());
        MediaPlayer mp = new MediaPlayer(hit);
     //   mp.play();



     mp.setAutoPlay(true);

     // Create the view and add it to the Scene.
     MediaView mediaView = new MediaView(mp);
     logger.info("Gotcha scene: " + scene);
     Group grp = (Group) scene.getRoot();
     logger.info("Gotcha group: " + grp);
     grp.getChildren().add(mediaView);
    }

    @Override
    public void start(Stage stage) throws Exception {
        
        List<String> params = getParameters().getRaw();
        
        if (params.size() != 1) 
            throw new Exception("Usage: java com.anowak.checker.audio.Mp3Checker <mp3file>");
        
        Scene scene = new Scene(new Group(new Text(25, 25, "Hello World!"))); 

        stage.setTitle("Welcome to JavaFX!"); 
        stage.setScene(scene); 
        stage.sizeToScene(); 
        stage.show(); 

        String mp3File = params.get(0);
        setScene(scene);
        setMp3File(mp3File);
        check();
        
        logger.info("Und tschuess!");
        // Platform.exit();
    }

}
