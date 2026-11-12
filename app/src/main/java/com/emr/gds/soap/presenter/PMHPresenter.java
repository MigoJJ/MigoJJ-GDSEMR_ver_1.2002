package com.emr.gds.soap.presenter;

import com.emr.gds.input.IAITextAreaManager;
import com.emr.gds.soap.config.CategoryConfig;
import com.emr.gds.soap.config.PMHConfig;
import com.emr.gds.soap.model.PMHEntry;
import com.emr.gds.soap.model.PMHEntryViewModel;
import com.emr.gds.soap.service.PMHService;
import com.emr.gds.soap.view.PMHView;
import com.emr.gds.util.AutoSaveManager; // Import AutoSaveManager
import com.emr.gds.util.DebouncedUpdater;
import com.emr.gds.util.ErrorHandler;
import com.emr.gds.util.I18N;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PMHPresenter {

    private final PMHService pmhService;
    private final PMHView pmhView;
    private Stage stage;

    private final IAITextAreaManager textAreaManager;
    private final TextArea externalTarget;

    private final List<PMHEntryViewModel> entries = new ArrayList<>();
    private final Map<String, String> abbrevMap;
    private final PMHConfig config;
    private final DebouncedUpdater debouncedUpdater = new DebouncedUpdater();
    private final ErrorHandler errorHandler = new ErrorHandler();
    private final AutoSaveManager autoSaveManager = new AutoSaveManager("pmh_autosave.json"); // AutoSaveManager instance

    public PMHPresenter(PMHService pmhService, PMHView pmhView, IAITextAreaManager manager, TextArea externalTarget, Map<String, String> abbrevMap) {
        this.pmhService = pmhService;
        this.pmhView = pmhView;
        this.textAreaManager = manager;
        this.externalTarget = externalTarget;
        this.abbrevMap = (abbrevMap != null) ? abbrevMap : Collections.emptyMap();
        
        PMHConfig loadedConfig; // Declare a local variable
        try {
            loadedConfig = PMHConfig.load("/pmh-config.json");
        } catch (IOException e) {
            errorHandler.handle(e, ErrorHandler.ErrorContext.INITIALIZATION, I18N.get("pmh.config.load_error"));
            loadedConfig = new PMHConfig(); // Assign to local variable in case of error
        }
        this.config = loadedConfig; // Assign the final field once here
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setOnHidden(e -> debouncedUpdater.shutdown());
    }

    public void initializeUI() {
        // Attempt to restore saved state
        Optional<List<PMHEntry>> restoredEntries = autoSaveManager.restore();
        Map<String, PMHEntry> restoredMap = new HashMap<>();
        restoredEntries.ifPresent(list -> list.forEach(entry -> restoredMap.put(entry.getCategory(), entry)));

        int row = 0, col = 0;
        for (CategoryConfig categoryConfig : config.getCategories()) {
            PMHEntry pmhEntry;
            if (restoredMap.containsKey(categoryConfig.getName())) {
                pmhEntry = restoredMap.get(categoryConfig.getName());
            } else {
                pmhEntry = new PMHEntry(
                    categoryConfig.getName(),
                    false, // initial selected state
                    "",    // initial notes
                    categoryConfig.isDefaultDotTarget(),
                    categoryConfig.getType()
                );
            }
            PMHEntryViewModel viewModel = new PMHEntryViewModel(pmhEntry);
            entries.add(viewModel);

            CheckBox cb = new CheckBox();
            cb.textProperty().bind(viewModel.categoryProperty());
            cb.setFont(pmhView.getDefaultFont());
            cb.setTooltip(new Tooltip(I18N.get("pmh.tooltip.select_if_applicable") + categoryConfig.getName()));

            TextArea ta = new TextArea();
            ta.setPromptText(I18N.get("pmh.textarea.prompt_details_for") + categoryConfig.getName());
            ta.setWrapText(true);
            ta.setPrefRowCount(2);
            ta.setFont(pmhView.getDefaultFont());

            viewModel.bindToUI(cb, ta);

            pmhView.addCategoryControl(categoryConfig.getName(), cb, ta, row, col); // Pass category name

            // Add listener to update summary pane in real-time with debouncing
            viewModel.selectedProperty().addListener((obs, oldVal, newVal) -> debouncedUpdater.schedule(this::updateAndAutoSave, 300));
            viewModel.notesProperty().addListener((obs, oldVal, newVal) -> debouncedUpdater.schedule(this::updateAndAutoSave, 300));

            addAbbreviationExpansionListener(ta);

            col++;
            if (col >= config.getNumColumns()) {
                col = 0;
                row++;
            }
        }
        
        pmhView.getBtnSave().setOnAction(e -> onSave());
        pmhView.getBtnDefault().setOnAction(e -> applyDefaultDots());
        pmhView.getBtnClear().setOnAction(e -> onClear());
        pmhView.getBtnCopy().setOnAction(e -> onCopy());
        pmhView.getBtnFMH().setOnAction(e -> openEMRFMH());
        pmhView.getBtnQuit().setOnAction(e -> onQuit());

        updateLiveSummary(); // Initial state
    }

    private void updateAndAutoSave() {
        updateLiveSummary();
        List<PMHEntry> currentEntries = entries.stream()
                                            .map(PMHEntryViewModel::getPmhEntry)
                                            .collect(Collectors.toList());
        autoSaveManager.autoSave(currentEntries);
    }

    public void onSave() {
        List<PMHEntry> currentEntries = entries.stream()
                                            .map(PMHEntryViewModel::getPmhEntry)
                                            .collect(Collectors.toList());
        String summary = pmhService.buildSummary(currentEntries, true);

        if (externalTarget != null) {
            int caret = externalTarget.getCaretPosition();
            externalTarget.insertText(caret, summary);
            pmhView.getOutputArea().setText(I18N.get("pmh.save.external_editor_message") + "\n" + summary);
        } else {
            pmhView.getOutputArea().setText(summary);
        }
    }

    private void onCopy() {
        List<PMHEntry> currentEntries = entries.stream()
                                            .map(PMHEntryViewModel::getPmhEntry)
                                            .collect(Collectors.toList());
        String summary = pmhService.buildSummary(currentEntries, false);
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(summary);
        clipboard.setContent(content);

        String originalText = pmhView.getOutputArea().getText();
        pmhView.getOutputArea().setText(I18N.get("pmh.copy.copied_to_clipboard") + "\n\n" + originalText);
        Timer restoreTimer = new Timer("pmh-clipboard-restore", true);
        restoreTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    Platform.runLater(() -> pmhView.getOutputArea().setText(originalText));
                } finally {
                    restoreTimer.cancel();
                }
            }
        }, 2000);
    }

    private void onClear() {
        entries.forEach(viewModel -> {
            viewModel.selectedProperty().set(false);
            viewModel.notesProperty().set("");
            pmhView.highlightTextArea(viewModel.getCategory(), false); // Remove highlight
        });
        updateAndAutoSave(); // Trigger auto-save after clearing
    }

    public void applyDefaultDots() {
        List<PMHEntry> currentModels = entries.stream()
                                            .map(PMHEntryViewModel::getPmhEntry)
                                            .collect(Collectors.toList());
        pmhService.applyDefaultDots(currentModels);

        for (PMHEntryViewModel viewModel : entries) {
            String originalNotes = viewModel.notesProperty().get();
            String updatedNotes = viewModel.getPmhEntry().getNotes();
            if (!originalNotes.equals(updatedNotes)) {
                viewModel.notesProperty().set(updatedNotes);
                pmhView.highlightTextArea(viewModel.getCategory(), true); // Highlight if notes changed
            } else {
                 pmhView.highlightTextArea(viewModel.getCategory(), false); // Ensure highlight is removed if no change
            }
        }
        updateAndAutoSave(); // Trigger auto-save after applying default dots
    }

    public void onQuit() {
        if (stage != null) {
            stage.close();
        }
    }

    private void updateLiveSummary() {
        List<PMHEntry> currentEntries = entries.stream()
                                            .map(PMHEntryViewModel::getPmhEntry)
                                            .collect(Collectors.toList());
        String summary = pmhService.buildSummary(currentEntries, false);
        pmhView.getOutputArea().setText(summary);
    }

    private void addAbbreviationExpansionListener(TextArea ta) {
        ta.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.SPACE) {
                if (expandAbbreviationOnSpace(ta)) event.consume();
            }
        });
    }

    private boolean expandAbbreviationOnSpace(TextArea ta) {
        int caret = ta.getCaretPosition();
        String upToCaret = ta.getText(0, caret);
        int start = Math.max(upToCaret.lastIndexOf(' '), upToCaret.lastIndexOf('\n')) + 1;

        String word = upToCaret.substring(start).trim();
        Optional<String> replacement = pmhService.expandAbbreviation(word);

        if (replacement.isEmpty()) return false;

        Platform.runLater(() -> {
            ta.deleteText(start, caret);
            ta.insertText(start, replacement.get() + " ");
        });
        return true;
    }

    private void openEMRFMH() {
        com.emr.gds.features.history.adapter.in.ui.FamilyHistoryStage.open(textAreaManager, abbrevMap);
    }

    private void showError(String header, Throwable t) {
        errorHandler.handle(t, ErrorHandler.ErrorContext.OTHER, header);
    }

    private void showInfo(String header, String content) {
        errorHandler.showInfo(header, content);
    }
}
