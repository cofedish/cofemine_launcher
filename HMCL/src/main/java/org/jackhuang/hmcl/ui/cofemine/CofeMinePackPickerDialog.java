/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2025  huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package org.jackhuang.hmcl.ui.cofemine;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import org.jackhuang.hmcl.Metadata;
import org.jackhuang.hmcl.cofemine.CofeMinePanelClient;
import org.jackhuang.hmcl.cofemine.CofeMinePanelPack;
import org.jackhuang.hmcl.ui.Controllers;
import org.jackhuang.hmcl.ui.construct.DialogCloseEvent;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

import static org.jackhuang.hmcl.util.i18n.I18n.i18n;

/**
 * Modal dialog showing every pack available on the configured CofeMine
 * Panel (via {@code GET /api/p/index.json}). The user picks one entry
 * and a target install directory. {@code onSelected} fires with both.
 */
public final class CofeMinePackPickerDialog extends JFXDialogLayout {

    public CofeMinePackPickerDialog(CofeMinePanelClient client,
                                    BiConsumer<CofeMinePanelPack, Path> onSelected) {
        setHeading(new Label(i18n("cofemine.modpack.pick.title")));

        Label loadingLabel = new Label(i18n("cofemine.modpack.pick.loading"));
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("text-danger");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        JFXListView<CofeMinePanelPack> listView = new JFXListView<>();
        listView.setCellFactory(list -> new PackCell());
        listView.setPrefHeight(320);
        listView.setVisible(false);
        listView.setManaged(false);

        // --- Install-location row ---------------------------------------
        Label pathLabel = new Label(i18n("cofemine.modpack.pick.install_to"));
        JFXTextField pathField = new JFXTextField();
        pathField.setPromptText(i18n("cofemine.modpack.pick.install_to.hint"));
        HBox.setHgrow(pathField, Priority.ALWAYS);
        pathField.setMinWidth(280);

        JFXButton browseButton = new JFXButton(i18n("cofemine.modpack.pick.browse"));
        browseButton.getStyleClass().add("dialog-cancel");
        browseButton.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(i18n("cofemine.modpack.pick.install_to"));
            Path current = parsePath(pathField.getText());
            if (current != null && current.getParent() != null && java.nio.file.Files.isDirectory(current.getParent())) {
                chooser.setInitialDirectory(current.getParent().toFile());
            } else {
                chooser.setInitialDirectory(Metadata.MINECRAFT_DIRECTORY.getParent() != null
                        ? Metadata.MINECRAFT_DIRECTORY.getParent().toFile()
                        : new File(System.getProperty("user.home", ".")));
            }
            File chosen = chooser.showDialog(Controllers.getStage());
            if (chosen != null) {
                CofeMinePanelPack pick = listView.getSelectionModel().getSelectedItem();
                pathField.setText(buildDefaultPath(chosen.toPath(), pick).toString());
            }
        });

        HBox pathRow = new HBox(6, pathField, browseButton);
        pathRow.setAlignment(Pos.CENTER_LEFT);

        VBox pathBox = new VBox(4, pathLabel, pathRow);
        pathBox.setPadding(new Insets(4, 0, 0, 0));

        VBox body = new VBox(8, loadingLabel, errorLabel, listView, pathBox);
        body.setPadding(new Insets(4));
        body.setMinWidth(540);
        VBox.setVgrow(listView, Priority.ALWAYS);
        setBody(body);

        // --- Actions ----------------------------------------------------
        JFXButton cancelButton = new JFXButton(i18n("button.cancel"));
        cancelButton.getStyleClass().add("dialog-cancel");
        cancelButton.addEventHandler(ActionEvent.ACTION, e -> fireEvent(new DialogCloseEvent()));

        JFXButton installButton = new JFXButton(i18n("cofemine.modpack.pick.install"));
        installButton.getStyleClass().addAll("dialog-accept", "cofemine-primary-button");
        installButton.setDefaultButton(true);
        installButton.setDisable(true);
        installButton.addEventHandler(ActionEvent.ACTION, e -> {
            CofeMinePanelPack chosen = listView.getSelectionModel().getSelectedItem();
            Path target = parsePath(pathField.getText());
            if (chosen == null || target == null) return;
            fireEvent(new DialogCloseEvent());
            onSelected.accept(chosen, target);
        });

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            installButton.setDisable(newVal == null);
            if (newVal != null) {
                Path defaultRoot = Metadata.MINECRAFT_DIRECTORY.resolveSibling("cofemine-packs");
                pathField.setText(buildDefaultPath(defaultRoot, newVal).toString());
            }
        });

        setActions(cancelButton, installButton);

        // --- Fetch the index --------------------------------------------
        client.fetchIndexAsync().whenCompleteAsync((packs, error) -> {
            loadingLabel.setVisible(false);
            loadingLabel.setManaged(false);
            if (error != null) {
                errorLabel.setText(i18n("cofemine.modpack.pick.error", error.getMessage()));
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }
            List<CofeMinePanelPack> filtered = packs.stream()
                    .filter(p -> p.getMrpackUrl() != null && !p.getMrpackUrl().isBlank())
                    .toList();
            if (filtered.isEmpty()) {
                errorLabel.setText(i18n("cofemine.modpack.pick.empty"));
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }
            listView.getItems().setAll(filtered);
            listView.setVisible(true);
            listView.setManaged(true);
            listView.getSelectionModel().selectFirst();
        }, Platform::runLater);
    }

    public void show() {
        Controllers.dialog(this);
    }

    private static Path parsePath(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        try {
            return Paths.get(trimmed).toAbsolutePath().normalize();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Path buildDefaultPath(Path parent, CofeMinePanelPack pack) {
        String slug = (pack.getDisplayName() != null ? pack.getDisplayName() : pack.getId())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.isEmpty()) slug = "cofemine-pack";
        return parent.resolve(slug);
    }

    private static final class PackCell extends ListCell<CofeMinePanelPack> {

        private final HBox row;
        private final Label name = new Label();
        private final Label meta = new Label();

        PackCell() {
            name.getStyleClass().add("strong");
            meta.getStyleClass().add("subtitle-label");
            VBox left = new VBox(2, name, meta);
            row = new HBox(8, left);
            HBox.setHgrow(left, Priority.ALWAYS);
            row.setPadding(new Insets(8, 12, 8, 12));
            row.getStyleClass().add("cofemine-pack-pick-cell");

            // Make selection visible on the inner row, not just the list
            // background. Without this the user's selection is easy to miss
            // when there's only one entry in the list.
            selectedProperty().addListener((obs, was, is) -> applySelectedStyle(is));
            applySelectedStyle(false);
        }

        private void applySelectedStyle(boolean selected) {
            if (selected) {
                row.setStyle("-fx-background-color: rgba(110,66,38,0.22); -fx-background-radius: 6;");
            } else {
                row.setStyle("-fx-background-color: transparent; -fx-background-radius: 6;");
            }
        }

        @Override
        protected void updateItem(CofeMinePanelPack item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            name.setText(item.getDisplayName() != null ? item.getDisplayName() : item.getId());
            String mc = item.getMinecraft() != null ? item.getMinecraft() : "?";
            String loader = item.getLoader() != null ? item.getLoader() : "vanilla";
            String version = item.getLoaderVersion() != null ? " " + item.getLoaderVersion() : "";
            meta.setText(i18n("cofemine.modpack.pick.meta", mc, loader + version));
            setGraphic(row);
            setText(null);
        }
    }
}
