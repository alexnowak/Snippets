/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.anowak.javafx;

import java.util.Random;
import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;



/**
 * Editable TableView with dynamic row
 * 
 * The article "JavaFX: Editable multi-column TableView" 
 * (http://java-buddy.blogspot.com/2013/03/javafx-editable-multi-column-tableview.html)
 * implement TableView with fixed number of row and column. 
 * 
 * This article demonstrate how to implement editable TableView with dynamic number of row.
 * @web http://java-buddy.blogspot.com/2013/03/javafx-editable-tableview-with-dynamic.html
 */
public class JavaFXDynTable extends Application {
    
    private TableView tableView = new TableView();
    private Button btnNew = new Button("New Record");
    
    static Random random = new Random();
    
    static final String Day[] = {
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday"};

    public static class Record {
        private final SimpleIntegerProperty id;
        private final SimpleIntegerProperty value_0;
        private final SimpleIntegerProperty value_1;
        private final SimpleIntegerProperty value_2;
        private final SimpleIntegerProperty value_3;
        private final SimpleIntegerProperty value_4;
        
        Record(int i, int v0, int v1, int v2, int v3, 
                int v4) {
            this.id = new SimpleIntegerProperty(i);
            this.value_0 = new SimpleIntegerProperty(v0);
            this.value_1 = new SimpleIntegerProperty(v1);
            this.value_2 = new SimpleIntegerProperty(v2);
            this.value_3 = new SimpleIntegerProperty(v3);
            this.value_4 = new SimpleIntegerProperty(v4);
        }
        
        public int getId() {
            return id.get();
        }

        public void setId(int v) {
            id.set(v);
        }
        
        public int getValue_0() {
            return value_0.get();
        }

        public void setValue_0(int v) {
            value_0.set(v);
        }
        
        public int getValue_1() {
            return value_1.get();
        }

        public void setValue_1(int v) {
            value_1.set(v);
        }
        
        public int getValue_2() {
            return value_2.get();
        }

        public void setValue_2(int v) {
            value_2.set(v);
        }
        
        public int getValue_3() {
            return value_3.get();
        }

        public void setValue_3(int v) {
            value_3.set(v);
        }
        
        public int getValue_4() {
            return value_4.get();
        }

        public void setValue_4(int v) {
            value_4.set(v);
        }
        
    };
    
    ObservableList<Record> data = FXCollections.observableArrayList();
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("java-buddy.blogspot.com");
        tableView.setEditable(true);
        Callback<TableColumn, TableCell> cellFactory =
                new Callback<TableColumn, TableCell>() {
                    
                    @Override
                    public TableCell call(TableColumn p) {
                        return new EditingCell();
                    }
                };
        
        btnNew.setOnAction(btnNewHandler);
        
        //init table
        //Un-editable column of "id"
        TableColumn col_id = new TableColumn("ID");
        tableView.getColumns().add(col_id);
        col_id.setCellValueFactory(
                    new PropertyValueFactory<Record, String>("id"));
        
        //Editable columns
        for(int i=0; i<Day.length; i++){
            TableColumn col = new TableColumn(Day[i]);
            col.setCellValueFactory(
                    new PropertyValueFactory<Record, String>(
                            "value_" + String.valueOf(i)));
            tableView.getColumns().add(col);
            col.setCellFactory(cellFactory);
        }
        tableView.setItems(data);
        
        Group root = new Group();
        VBox vBox = new VBox();
        vBox.setSpacing(10);
        vBox.getChildren().addAll(btnNew, tableView);
        root.getChildren().add(vBox);
        primaryStage.setScene(new Scene(root, 500, 400));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
    
    EventHandler<ActionEvent> btnNewHandler = 
            new EventHandler<ActionEvent>(){

        @Override
        public void handle(ActionEvent t) {
            
            //generate new Record with random number
            int newId = data.size();
            Record newRec = new Record(
                    newId,
                    random.nextInt(100), 
                    random.nextInt(100), 
                    random.nextInt(100), 
                    random.nextInt(100), 
                    random.nextInt(100));
            data.add(newRec);
            
        }
    };
    
    class EditingCell extends TableCell<XYChart.Data, Number> {
         
        private TextField textField;
         
        public EditingCell() {}
         
        @Override
        public void startEdit() {
             
            super.startEdit();
             
            if (textField == null) {
                createTextField();
            }
             
            setGraphic(textField);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            textField.selectAll();
        }
         
        @Override
        public void cancelEdit() {
            super.cancelEdit();
             
            setText(String.valueOf(getItem()));
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        }
         
        @Override
        public void updateItem(Number item, boolean empty) {
            super.updateItem(item, empty);
             
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(getString());
                    }
                    setGraphic(textField);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                } else {
                    setText(getString());
                    setContentDisplay(ContentDisplay.TEXT_ONLY);
                }
            }
        }
         
        private void createTextField() {
            textField = new TextField(getString());
            textField.setMinWidth(this.getWidth() - this.getGraphicTextGap()*2);
            textField.setOnKeyPressed(new EventHandler<KeyEvent>() {
                 
                @Override
                public void handle(KeyEvent t) {
                    if (t.getCode() == KeyCode.ENTER) {
                        commitEdit(Integer.parseInt(textField.getText()));
                    } else if (t.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                    }
                }
            });
        }
         
        private String getString() {
            return getItem() == null ? "" : getItem().toString();
        }
    }

}
