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
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jackhuang.hmcl.cofemine.CofeMinePanelClient;
import org.jackhuang.hmcl.cofemine.CofeMinePanelPack;
import org.jackhuang.hmcl.ui.Controllers;
import org.jackhuang.hmcl.ui.construct.DialogCloseEvent;

import java.util.List;
import java.util.function.Consumer;

import static org.jackhuang.hmcl.util.i18n.I18n.i18n;

/**
 * Modal dialog showing every pack available on the configured CofeMine
 * Panel (via {@code GET /api/p/index.json}). The user picks one entry
 * and the supplied callback fires with the chosen pack.
 */
public final class CofeMinePackPickerDialog extends JFXDialogLayout {

    public CofeMinePackPickerDialog(CofeMinePanelClient client, Consumer<CofeMinePanelPack> onSelected) {
        setHeading(new Label(i18n("cofemine.modpack.pick.title")));

        Label loadingLabel = new Label(i18n("cofemine.modpack.pick.loading"));
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("text-danger");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        JFXListView<CofeMinePanelPack> listView = new JFXListView<>();
        listView.setCellFactory(list -> new PackCell());
        listView.setPrefHeight(360);
        listView.setVisible(false);
        listView.setManaged(false);

        VBox body = new VBox(8, loadingLabel, errorLabel, listView);
        body.setPadding(new Insets(4, 4, 4, 4));
        body.setMinWidth(540);
        VBox.setVgrow(listView, Priority.ALWAYS);
        setBody(body);

        JFXButton cancelButton = new JFXButton(i18n("button.cancel"));
        cancelButton.getStyleClass().add("dialog-cancel");
        cancelButton.addEventHandler(ActionEvent.ACTION, e -> fireEvent(new DialogCloseEvent()));

        JFXButton installButton = new JFXButton(i18n("cofemine.modpack.pick.install"));
        installButton.getStyleClass().add("dialog-accept");
        installButton.setDisable(true);
        installButton.addEventHandler(ActionEvent.ACTION, e -> {
            CofeMinePanelPack chosen = listView.getSelectionModel().getSelectedItem();
            if (chosen == null) return;
            fireEvent(new DialogCloseEvent());
            onSelected.accept(chosen);
        });

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) ->
                installButton.setDisable(newVal == null));

        setActions(cancelButton, installButton);

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

    private static final class PackCell extends ListCell<CofeMinePanelPack> {
        @Override
        protected void updateItem(CofeMinePanelPack item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            Label name = new Label(item.getDisplayName() != null ? item.getDisplayName() : item.getId());
            name.getStyleClass().add("strong");

            String mc = item.getMinecraft() != null ? item.getMinecraft() : "?";
            String loader = item.getLoader() != null ? item.getLoader() : "vanilla";
            String version = item.getLoaderVersion() != null ? " " + item.getLoaderVersion() : "";

            Label meta = new Label(i18n("cofemine.modpack.pick.meta", mc, loader + version));
            meta.getStyleClass().add("subtitle-label");

            VBox left = new VBox(2, name, meta);
            HBox row = new HBox(8, left);
            HBox.setHgrow(left, Priority.ALWAYS);
            row.setPadding(new Insets(8, 12, 8, 12));

            setGraphic(row);
            setText(null);
        }
    }
}
