package com.emr.gds.features.clinicalLab.adapter.in.ui;

import com.emr.gds.features.clinicalLab.adapter.out.persistence.JdbcClinicalLabRepository;
import com.emr.gds.features.clinicalLab.domain.ClinicalLabItem;
import com.emr.gds.features.clinicalLab.domain.ClinicalLabRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;

import javafx.stage.Stage;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ClinicalLabController implements Initializable {

    // Left Panel
    @FXML private ListView<String> selectedItemsList;

    // Center Panel
    @FXML private TextField searchField;
    @FXML private TableView<ClinicalLabItem> labTable;
    @FXML private TableColumn<ClinicalLabItem, String> colCategory;
    @FXML private TableColumn<ClinicalLabItem, String> colTestName;
    @FXML private TableColumn<ClinicalLabItem, String> colUnit;
    @FXML private TableColumn<ClinicalLabItem, String> colMaleRef;
    @FXML private TableColumn<ClinicalLabItem, String> colFemaleRef;
    @FXML private TableColumn<ClinicalLabItem, String> colCodes;
    @FXML private TableColumn<ClinicalLabItem, String> colComments;

    // Right Panel (Details)
    @FXML private TextField editTestName;
    @FXML private TextField editCategory;
    @FXML private Label lblUnit;
    @FXML private Label lblMaleRange;
    @FXML private Label lblMaleRef;
    @FXML private Label lblFemaleRange;
    @FXML private Label lblFemaleRef;
    @FXML private TextField editCodes;
    @FXML private TextArea editComments;
    @FXML private Button editButton;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;


    private final ClinicalLabRepository database;
    private final ObservableList<ClinicalLabItem> masterData = FXCollections.observableArrayList();
    private final ObservableList<String> selectedItems = FXCollections.observableArrayList();

    public ClinicalLabController() {
        this(new JdbcClinicalLabRepository());
    }

    public ClinicalLabController(ClinicalLabRepository database) {
        this.database = database;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupSelectionModel();
        selectedItemsList.setItems(selectedItems);
        loadData();
        setEditable(false);
    }

    private void setupTable() {
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colTestName.setCellValueFactory(new PropertyValueFactory<>("testName"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colMaleRef.setCellValueFactory(new PropertyValueFactory<>("maleReferenceRange"));
        colFemaleRef.setCellValueFactory(new PropertyValueFactory<>("femaleReferenceRange"));
        colCodes.setCellValueFactory(new PropertyValueFactory<>("codes"));
        colComments.setCellValueFactory(new PropertyValueFactory<>("comments"));
    }

    private void setupSelectionModel() {
        labTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showDetails(newVal);
            } else {
                clearDetails();
            }
        });

        // Handle double-click on table rows
        labTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                addToSelection();
            }
        });
    }

    @FXML
    private void loadData() {
        masterData.clear();
        masterData.addAll(database.getAllItems());
        labTable.setItems(masterData);
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText();
        if (query == null || query.trim().isEmpty()) {
            labTable.setItems(masterData);
        } else {
            // Use database search directly
            java.util.List<ClinicalLabItem> results = database.searchItems(query);
            labTable.setItems(FXCollections.observableArrayList(results));
        }
    }

    private void showDetails(ClinicalLabItem item) {
        editTestName.setText(item.getTestName());
        editCategory.setText(item.getCategory());
        lblUnit.setText(item.getUnit());

        String mLow = item.getMaleRangeLow() != null ? String.valueOf(item.getMaleRangeLow()) : "-";
        String mHigh = item.getMaleRangeHigh() != null ? String.valueOf(item.getMaleRangeHigh()) : "-";
        lblMaleRange.setText(mLow + " - " + mHigh);
        lblMaleRef.setText(item.getMaleReferenceRange() != null ? item.getMaleReferenceRange() : "");

        String fLow = item.getFemaleRangeLow() != null ? String.valueOf(item.getFemaleRangeLow()) : "-";
        String fHigh = item.getFemaleRangeHigh() != null ? String.valueOf(item.getFemaleRangeHigh()) : "-";
        lblFemaleRange.setText(fLow + " - " + fHigh);
        lblFemaleRef.setText(item.getFemaleReferenceRange() != null ? item.getFemaleReferenceRange() : "");

        editCodes.setText(item.getCodes() != null ? item.getCodes() : "-");
        editComments.setText(item.getComments() != null ? item.getComments() : "-");
    }

    private void clearDetails() {
        editTestName.setText("-");
        editCategory.setText("-");
        lblUnit.setText("-");
        lblMaleRange.setText("-");
        lblMaleRef.setText("-");
        lblFemaleRange.setText("-");
        lblFemaleRef.setText("-");
        editCodes.setText("-");
        editComments.setText("-");
    }

    @FXML
    private void handleEdit() {
        ClinicalLabItem selected = labTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText("No Item Selected");
            alert.setContentText("Please select an item to edit.");
            alert.showAndWait();
            return;
        }
        setEditable(true);
    }

    @FXML
    private void handleSave() {
        ClinicalLabItem selected = labTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.setTestName(editTestName.getText());
            selected.setCategory(editCategory.getText());
            selected.setCodes(editCodes.getText());
            selected.setComments(editComments.getText());
            try {
                database.updateItem(selected);
            } catch (RuntimeException e) {
                showPersistenceError("save changes", e);
                return;
            }
            labTable.refresh();
            showDetails(selected);
        }
        setEditable(false);
    }

    @FXML
    private void handleCancel() {
        ClinicalLabItem selected = labTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showDetails(selected);
        }
        setEditable(false);
    }

    private void setEditable(boolean editable) {
        editTestName.setEditable(editable);
        editCategory.setEditable(editable);
        editCodes.setEditable(editable);
        editComments.setEditable(editable);

        if (editable) {
            editTestName.setStyle("-fx-background-color: white; -fx-border-color: #ced4da; -fx-border-radius: 4;");
            editCategory.setStyle("-fx-background-color: white; -fx-border-color: #ced4da; -fx-border-radius: 4;");
            editCodes.setStyle("-fx-background-color: white; -fx-border-color: #ced4da; -fx-border-radius: 4;");
            editComments.setStyle("-fx-background-color: white; -fx-border-color: #ced4da; -fx-border-radius: 4;");
        } else {
            editTestName.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
            editCategory.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
            editCodes.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
            editComments.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        }

        editButton.setVisible(!editable);
        saveButton.setVisible(editable);
        cancelButton.setVisible(editable);
    }


    @FXML
    private void handleAdd() {
        Dialog<ClinicalLabItem> dialog = new Dialog<>();
        dialog.setTitle("Add New Clinical Lab Item");
        dialog.setHeaderText("Enter details for the new lab item.");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField categoryField = new TextField();
        categoryField.setPromptText("Category");
        TextField testNameField = new TextField();
        testNameField.setPromptText("Test Name");
        TextField unitField = new TextField();
        unitField.setPromptText("Unit");
        TextField maleRefRangeField = new TextField();
        maleRefRangeField.setPromptText("Male Reference Range");
        TextField femaleRefRangeField = new TextField();
        femaleRefRangeField.setPromptText("Female Reference Range");
        TextField codesField = new TextField();
        codesField.setPromptText("Codes");
        TextArea commentsArea = new TextArea();
        commentsArea.setPromptText("Comments");
        commentsArea.setPrefRowCount(3);

        grid.add(new Label("Category:"), 0, 0);
        grid.add(categoryField, 1, 0);
        grid.add(new Label("Test Name:"), 0, 1);
        grid.add(testNameField, 1, 1);
        grid.add(new Label("Unit:"), 0, 2);
        grid.add(unitField, 1, 2);
        grid.add(new Label("Male Ref Range:"), 0, 3);
        grid.add(maleRefRangeField, 1, 3);
        grid.add(new Label("Female Ref Range:"), 0, 4);
        grid.add(femaleRefRangeField, 1, 4);
        grid.add(new Label("Codes:"), 0, 5);
        grid.add(codesField, 1, 5);
        grid.add(new Label("Comments:"), 0, 6);
        grid.add(commentsArea, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    return new ClinicalLabItem(
                        0, // ID will be set by the database
                        categoryField.getText(),
                        testNameField.getText(),
                        unitField.getText(),
                        null,
                        null,
                        null,
                        null,
                        maleRefRangeField.getText(),
                        femaleRefRangeField.getText(),
                        codesField.getText(),
                        commentsArea.getText()
                    );
                } catch (NumberFormatException e) {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Input Error");
                    errorAlert.setHeaderText("Invalid Number Format");
                    errorAlert.setContentText("Please ensure numeric fields contain valid numbers.");
                    errorAlert.showAndWait();
                    return null;
                }
            }
            return null;
        });

        Optional<ClinicalLabItem> result = dialog.showAndWait();
        result.ifPresent(newItem -> {
            try {
                database.insertItem(newItem);
            } catch (RuntimeException e) {
                showPersistenceError("add the item", e);
                return;
            }
            loadData(); // Refresh the table
            labTable.getSelectionModel().select(newItem); // Select the newly added item
            showDetails(newItem); // Show details of newly added item
        });
    }

    @FXML
    private void handleDelete() {
        ClinicalLabItem selected = labTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText("No Item Selected");
            alert.setContentText("Please select an item to delete.");
            alert.showAndWait();
            return;
        }

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Deletion");
        confirmationAlert.setHeaderText("Delete Item: " + selected.getTestName());
        confirmationAlert.setContentText("Are you sure you want to delete this item?");

        Optional<ButtonType> result = confirmationAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                database.deleteItem(selected.getId());
            } catch (RuntimeException e) {
                showPersistenceError("delete the item", e);
                return;
            }
            loadData(); // Refresh the table
            clearDetails(); // Clear details panel
        }
    }

    private void showPersistenceError(String action, Exception cause) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Save Failed");
        alert.setHeaderText("Could not " + action + ".");
        alert.setContentText("Your change was not saved. Please try again. Details: " + cause.getMessage());
        alert.showAndWait();
    }

    @FXML
    private void addToSelection() {
        ClinicalLabItem selected = labTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            String itemStr = selected.getTestName() + " (" + selected.getCategory() + ")";
            if (!selectedItems.contains(itemStr)) {
                selectedItems.add(itemStr);
            }
        }
    }

    @FXML
    private void copySelected() {
        if (selectedItems.isEmpty()) return;
        String content = String.join("\n", selectedItems);
        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(content);
        Clipboard.getSystemClipboard().setContent(clipboardContent);
    }

    @FXML
    private void clearSelection() {
        selectedItems.clear();
    }

    @FXML
    private void handleSaveToEmr() {
        if (selectedItems.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText("No Items Selected");
            alert.setContentText("Please add items to Selected Tests before saving to EMR.");
            alert.showAndWait();
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (String item : selectedItems) {
            sb.append("Lab Item: ").append(item).append("\n");
        }
        com.emr.gds.input.IAIMain.getTextAreaManager().appendTextToSection(9, sb.toString());
    }

    @FXML
    private void handleQuit() {
        com.emr.gds.IttiaApp.closeWorkspaceTabOrStage(labTable);
    }

    @FXML
    private void onQuit() {
        handleQuit();
    }
}
