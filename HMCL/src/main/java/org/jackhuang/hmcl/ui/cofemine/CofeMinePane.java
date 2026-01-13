/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2025  huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.ui.cofemine;

import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import org.jackhuang.hmcl.cofemine.CofeMineModpackManifest;
import org.jackhuang.hmcl.cofemine.CofeMineModpackService;
import org.jackhuang.hmcl.cofemine.CofeMineServerStatus;
import org.jackhuang.hmcl.cofemine.CofeMineServerStatusService;
import org.jackhuang.hmcl.task.TaskExecutor;
import org.jackhuang.hmcl.task.TaskListener;
import org.jackhuang.hmcl.ui.Controllers;
import org.jackhuang.hmcl.ui.FXUtils;
import org.jackhuang.hmcl.ui.SVG;
import org.jackhuang.hmcl.ui.construct.MessageDialogPane;
import org.jackhuang.hmcl.util.StringUtils;
import org.jackhuang.hmcl.util.TaskCancellationAction;
import org.jackhuang.hmcl.util.io.FileUtils;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.jackhuang.hmcl.setting.ConfigHolder.config;
import static org.jackhuang.hmcl.util.i18n.I18n.i18n;

public final class CofeMinePane extends VBox {
    private static final String SITE_URL = "https://cofemine.ru";
    private static final String SERVER_HOST = "server.cofemine.ru";
    private static final int SERVER_PORT = 25565;
    private static final String LOGO_PATH = "/assets/img/icon.png";

    private final CofeMineServerStatusService statusService = new CofeMineServerStatusService(
            SERVER_HOST, SERVER_PORT, Duration.ofSeconds(30));
    private final CofeMineModpackService modpackService = new CofeMineModpackService();

    private final Circle statusDot = new Circle(4);
    private final Label statusLabel = new Label();
    private final Label statusDetail = new Label();
    private final Label statusMotd = new Label();
    private final Label modpackPathLabel = new Label();
    private final JFXButton modpackButton = new JFXButton();
    private final JFXButton openFolderButton = new JFXButton(i18n("cofemine.modpack.open_folder"));
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final BooleanProperty expanded = new SimpleBooleanProperty(true);

    public CofeMinePane() {
        Label title = new Label(i18n("cofemine.panel.title"));
        title.getStyleClass().add("cofemine-title");
        Label subtitle = new Label(i18n("cofemine.panel.subtitle"));
        subtitle.getStyleClass().add("cofemine-subtitle");

        VBox header = new VBox(2, title, subtitle);

        var headerLogo = FXUtils.newBuiltinImage(LOGO_PATH);
        var headerIcon = new javafx.scene.image.ImageView(headerLogo);
        headerIcon.setFitWidth(24);
        headerIcon.setFitHeight(24);
        headerIcon.setPreserveRatio(true);
        headerIcon.getStyleClass().add("cofemine-collapse-logo");
        headerIcon.setOnMouseClicked(event -> expanded.set(false));

        HBox headerLine = new HBox(8, header, headerIcon);
        headerLine.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(header, Priority.ALWAYS);

        JFXButton refreshButton = new JFXButton();
        refreshButton.getStyleClass().add("toggle-icon-tiny");
        refreshButton.setGraphic(SVG.REFRESH.createIcon(14));
        refreshButton.setOnAction(event -> statusService.refreshNow());
        FXUtils.installFastTooltip(refreshButton, i18n("cofemine.server.refresh"));

        HBox statusLine = new HBox(8, statusDot, statusLabel, refreshButton);
        statusLine.getStyleClass().add("cofemine-status-line");
        statusLine.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        statusDetail.getStyleClass().add("cofemine-subtitle");
        statusMotd.getStyleClass().add("cofemine-subtitle");
        statusMotd.setWrapText(true);

        VBox statusBox = new VBox(4, statusLine, statusDetail, statusMotd);

        JFXButton siteButton = new JFXButton(i18n("cofemine.site"));
        siteButton.getStyleClass().add("cofemine-action-button");
        siteButton.setOnAction(event -> FXUtils.openLink(SITE_URL));

        modpackButton.getStyleClass().add("cofemine-action-button");
        modpackButton.setOnAction(event -> onModpackAction());

        openFolderButton.getStyleClass().add("cofemine-secondary-button");
        openFolderButton.setOnAction(event -> resolveInstancePath().ifPresent(FXUtils::openFolder));

        VBox actionBox = new VBox(8, siteButton, modpackButton, openFolderButton);
        actionBox.setFillWidth(true);
        siteButton.setMaxWidth(Double.MAX_VALUE);
        modpackButton.setMaxWidth(Double.MAX_VALUE);
        openFolderButton.setMaxWidth(Double.MAX_VALUE);

        Label modpackTitle = new Label(i18n("cofemine.modpack.title"));
        modpackTitle.getStyleClass().add("cofemine-subtitle");
        modpackPathLabel.getStyleClass().add("cofemine-subtitle");

        VBox modpackBox = new VBox(6, modpackTitle, modpackPathLabel, actionBox);
        modpackBox.setPadding(new Insets(4, 0, 0, 0));

        VBox expandedPane = new VBox(10, headerLine, statusBox, modpackBox);
        expandedPane.getStyleClass().addAll("card", "cofemine-panel");

        var collapsedIcon = new javafx.scene.image.ImageView(FXUtils.newBuiltinImage(LOGO_PATH));
        collapsedIcon.setFitWidth(64);
        collapsedIcon.setFitHeight(64);
        collapsedIcon.setPreserveRatio(true);

        var collapsedPane = new javafx.scene.layout.StackPane(collapsedIcon);
        collapsedPane.getStyleClass().addAll("card", "cofemine-panel-collapsed");
        collapsedPane.setOnMouseClicked(event -> expanded.set(true));

        getChildren().setAll(expandedPane, collapsedPane);

        statusService.statusProperty().addListener((obs, oldVal, newVal) -> updateStatus(newVal));
        updateStatus(statusService.getStatus());
        updateModpackState();

        expanded.addListener((obs, oldVal, newVal) -> {
            expandedPane.setVisible(newVal);
            expandedPane.setManaged(newVal);
            collapsedPane.setVisible(!newVal);
            collapsedPane.setManaged(!newVal);
        });
        expandedPane.setVisible(expanded.get());
        expandedPane.setManaged(expanded.get());
        collapsedPane.setVisible(!expanded.get());
        collapsedPane.setManaged(!expanded.get());

        busy.addListener((obs, oldVal, newVal) -> {
            modpackButton.setDisable(newVal);
            openFolderButton.setDisable(newVal || resolveInstancePath().isEmpty());
        });

        statusService.start();
    }

    private void updateStatus(CofeMineServerStatus status) {
        statusDot.getStyleClass().setAll("cofemine-status-dot");
        boolean online = status != null && status.online();
        if (online) {
            statusDot.getStyleClass().add("online");
            statusLabel.setText(i18n("cofemine.server.online"));
        } else if (status != null && "loading".equals(status.error())) {
            statusDot.getStyleClass().add("pending");
            statusLabel.setText(i18n("cofemine.server.loading"));
        } else {
            statusDot.getStyleClass().add("offline");
            statusLabel.setText(i18n("cofemine.server.offline"));
        }

        List<String> details = new ArrayList<>();
        if (online && status.hasPlayers()) {
            details.add(i18n("cofemine.server.players", status.playersOnline(), status.playersMax()));
        }
        if (online && status.hasPing()) {
            details.add(i18n("cofemine.server.ping", status.pingMillis()));
        }
        if (!online && status != null && status.error() != null) {
            details.add(status.error());
        }

        statusDetail.setText(details.isEmpty() ? "" : String.join(" • ", details));

        String motd = status != null ? status.motd() : null;
        statusMotd.setText(motd == null || motd.isBlank() ? "" : i18n("cofemine.server.motd", motd));
    }

    private void updateModpackState() {
        Optional<Path> instancePath = resolveInstancePath();
        boolean installed = instancePath.isPresent() && CofeMineModpackService.isInstalled(instancePath.get());

        if (installed) {
            modpackButton.setText(i18n("cofemine.modpack.update"));
            modpackButton.setGraphic(SVG.UPDATE.createIcon(16));
            modpackPathLabel.setText(i18n("cofemine.modpack.installed_at", instancePath.get().toString()));
            openFolderButton.setDisable(false);
        } else {
            modpackButton.setText(i18n("cofemine.modpack.install"));
            modpackButton.setGraphic(SVG.DOWNLOAD.createIcon(16));
            modpackPathLabel.setText(i18n("cofemine.modpack.not_installed"));
            openFolderButton.setDisable(true);
        }
    }

    private Optional<Path> resolveInstancePath() {
        return FileUtils.tryGetPath(config().getCofemineInstancePath());
    }

    private void onModpackAction() {
        if (busy.get()) {
            return;
        }

        Optional<Path> instancePath = resolveInstancePath();
        boolean installed = instancePath.isPresent() && CofeMineModpackService.isInstalled(instancePath.get());

        if (!installed) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(i18n("cofemine.modpack.choose_folder"));
            if (instancePath.isPresent()) {
                chooser.setInitialDirectory(instancePath.get().toFile());
            }
            var selected = chooser.showDialog(Controllers.getStage());
            if (selected == null) {
                return;
            }
            runModpackTask(Mode.INSTALL, selected.toPath());
        } else {
            runModpackTask(Mode.UPDATE, instancePath.get());
        }
    }

    private void runModpackTask(Mode mode, Path targetDir) {
        String zipUrl = config().getCofemineModpackZipUrl();
        String manifestUrl = config().getCofemineModpackManifestUrl();
        if (StringUtils.isBlank(zipUrl)) {
            Controllers.dialog(i18n("cofemine.modpack.url.missing"),
                    i18n("message.error"),
                    MessageDialogPane.MessageType.ERROR);
            return;
        }
        busy.set(true);

        modpackService.loadManifestAsync(manifestUrl).whenCompleteAsync((manifest, error) -> {
            CofeMineModpackManifest resolvedManifest = manifest;
            try {
                TaskExecutor executor = (mode == Mode.INSTALL
                        ? modpackService.createInstallTask(targetDir, zipUrl, resolvedManifest, manifestUrl)
                        : modpackService.createUpdateTask(targetDir, zipUrl, resolvedManifest, manifestUrl))
                        .executor();

                executor.addTaskListener(new TaskListener() {
                    @Override
                    public void onStop(boolean success, TaskExecutor executor) {
                        Platform.runLater(() -> {
                            busy.set(false);
                            if (success) {
                                config().setCofemineInstancePath(targetDir.toString());
                                CofeMineModpackService.ensureProfile(targetDir);
                                updateModpackState();
                            } else if (executor.getException() != null) {
                                Controllers.dialog(StringUtils.getStackTrace(executor.getException()),
                                        i18n("message.error"),
                                        MessageDialogPane.MessageType.ERROR);
                            }
                        });
                    }
                });

                Controllers.taskDialog(executor,
                        i18n(mode == Mode.INSTALL ? "cofemine.modpack.installing" : "cofemine.modpack.updating"),
                        TaskCancellationAction.NORMAL);
                executor.start();
            } catch (Exception e) {
                busy.set(false);
                Controllers.dialog(StringUtils.getStackTrace(e),
                        i18n("message.error"),
                        MessageDialogPane.MessageType.ERROR);
            }
        }, Platform::runLater);
    }

    private enum Mode {
        INSTALL,
        UPDATE
    }
}
