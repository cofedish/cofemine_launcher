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
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
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
import org.jackhuang.hmcl.ui.animation.AnimationUtils;
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
    private static final String LOGO_PATH = "/assets/img/icon@4x.png";
    private static final double LOGO_SIZE = 28;
    private static final double COLLAPSED_SCALE = 0.6;
    private static final double COLLAPSED_LOGO_SCALE = 0.85;
    private static final javafx.util.Duration TOGGLE_DURATION = javafx.util.Duration.millis(240);

    private CofeMineServerStatusService statusService;
    private final CofeMineModpackService modpackService = new CofeMineModpackService();
    private final javafx.beans.value.ChangeListener<Object> serverListener = (obs, oldVal, newVal) -> rebuildStatusService();
    private final javafx.beans.value.ChangeListener<CofeMineServerStatus> statusListener = (obs, oldVal, newVal) -> updateStatus(newVal);

    private final Circle statusDot = new Circle(4);
    private final Label statusLabel = new Label();
    private final Label statusDetail = new Label();
    private final Label statusMotd = new Label();
    private final Label modpackPathLabel = new Label();
    private final JFXButton modpackButton = new JFXButton();
    private final JFXButton openFolderButton = new JFXButton(i18n("cofemine.modpack.open_folder"));
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final BooleanProperty expanded = new SimpleBooleanProperty(true);
    private final VBox expandedPane;
    private final StackPane collapsedPane;
    private boolean toggleInitialized = false;

    public CofeMinePane() {
        Label title = new Label(i18n("cofemine.panel.title"));
        title.getStyleClass().add("cofemine-title");
        Label subtitle = new Label(i18n("cofemine.panel.subtitle"));
        subtitle.getStyleClass().add("cofemine-subtitle");

        VBox header = new VBox(2, title, subtitle);

        var headerLogo = FXUtils.newBuiltinImage(LOGO_PATH, 24, 24, true, true);
        var headerIcon = new ImageView(headerLogo);
        headerIcon.setFitWidth(24);
        headerIcon.setFitHeight(24);
        headerIcon.setPreserveRatio(true);
        headerIcon.getStyleClass().add("cofemine-collapse-logo");
        headerIcon.setCursor(Cursor.HAND);
        headerIcon.setOnMouseClicked(event -> expanded.set(false));

        HBox headerLine = new HBox(8, header, headerIcon);
        headerLine.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(header, Priority.ALWAYS);

        JFXButton refreshButton = new JFXButton();
        refreshButton.getStyleClass().add("toggle-icon-tiny");
        refreshButton.setGraphic(SVG.REFRESH.createIcon(14));
        refreshButton.setOnAction(event -> {
            if (statusService != null) {
                statusService.refreshNow();
            }
        });
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

        expandedPane = new VBox(10, headerLine, statusBox, modpackBox);
        expandedPane.getStyleClass().addAll("card", "cofemine-panel");

        var collapsedIcon = new ImageView(FXUtils.newBuiltinImage(LOGO_PATH, LOGO_SIZE, LOGO_SIZE, true, true));
        collapsedIcon.setFitWidth(LOGO_SIZE);
        collapsedIcon.setFitHeight(LOGO_SIZE);
        collapsedIcon.setPreserveRatio(true);
        collapsedIcon.setCursor(Cursor.HAND);
        collapsedIcon.setOnMouseClicked(event -> expanded.set(true));

        collapsedPane = new StackPane(collapsedIcon);
        collapsedPane.getStyleClass().add("cofemine-panel-collapsed");
        StackPane.setAlignment(collapsedIcon, Pos.TOP_RIGHT);
        collapsedPane.setPickOnBounds(false);

        StackPane container = new StackPane(expandedPane, collapsedPane);
        StackPane.setAlignment(expandedPane, Pos.TOP_RIGHT);
        StackPane.setAlignment(collapsedPane, Pos.TOP_RIGHT);
        getChildren().setAll(container);

        rebuildStatusService();
        updateModpackState();

        expanded.addListener((obs, oldVal, newVal) -> togglePane(newVal));
        togglePane(expanded.get());

        busy.addListener((obs, oldVal, newVal) -> {
            modpackButton.setDisable(newVal);
            openFolderButton.setDisable(newVal || resolveInstancePath().isEmpty());
        });

        config().cofemineServerHostProperty().addListener(serverListener);
        config().cofemineServerPortProperty().addListener(serverListener);
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

    private void rebuildStatusService() {
        if (statusService != null) {
            statusService.statusProperty().removeListener(statusListener);
            statusService.close();
        }

        String host = StringUtils.defaultIfBlank(config().getCofemineServerHost(), "server.cofemine.ru");
        int port = config().getCofemineServerPort() > 0 ? config().getCofemineServerPort() : 25565;
        statusService = new CofeMineServerStatusService(host, port, Duration.ofSeconds(30));
        statusService.statusProperty().addListener(statusListener);
        updateStatus(statusService.getStatus());
        statusService.start();
    }

    private void togglePane(boolean showExpanded) {
        if (!toggleInitialized || !AnimationUtils.isAnimationEnabled()) {
            expandedPane.setVisible(showExpanded);
            expandedPane.setManaged(showExpanded);
            collapsedPane.setVisible(!showExpanded);
            collapsedPane.setManaged(!showExpanded);
            toggleInitialized = true;
            return;
        }

        toggleInitialized = true;
        expandedPane.setManaged(true);
        expandedPane.setVisible(true);
        collapsedPane.setManaged(true);
        collapsedPane.setVisible(true);

        double panelWidth = getWidth() > 0 ? getWidth() : getPrefWidth();
        if (panelWidth <= 0) {
            panelWidth = 320;
        }
        double panelHeight = expandedPane.getHeight() > 0 ? expandedPane.getHeight() : expandedPane.prefHeight(-1);
        if (panelHeight <= 0) {
            panelHeight = 200;
        }
        double shiftX = panelWidth * (1 - COLLAPSED_SCALE) / 2;
        double shiftY = panelHeight * (1 - COLLAPSED_SCALE) / 2;

        Timeline animation = new Timeline();
        if (showExpanded) {
            expandedPane.setOpacity(0);
            expandedPane.setScaleX(COLLAPSED_SCALE);
            expandedPane.setScaleY(COLLAPSED_SCALE);
            expandedPane.setTranslateX(shiftX);
            expandedPane.setTranslateY(-shiftY);

            collapsedPane.setOpacity(1);
            collapsedPane.setScaleX(COLLAPSED_LOGO_SCALE);
            collapsedPane.setScaleY(COLLAPSED_LOGO_SCALE);

            animation.getKeyFrames().addAll(
                    new KeyFrame(javafx.util.Duration.ZERO,
                            new KeyValue(expandedPane.opacityProperty(), 0, FXUtils.SINE),
                            new KeyValue(expandedPane.scaleXProperty(), COLLAPSED_SCALE, FXUtils.SINE),
                            new KeyValue(expandedPane.scaleYProperty(), COLLAPSED_SCALE, FXUtils.SINE),
                            new KeyValue(expandedPane.translateXProperty(), shiftX, FXUtils.SINE),
                            new KeyValue(expandedPane.translateYProperty(), -shiftY, FXUtils.SINE),
                            new KeyValue(collapsedPane.opacityProperty(), 1, FXUtils.SINE),
                            new KeyValue(collapsedPane.scaleXProperty(), COLLAPSED_LOGO_SCALE, FXUtils.SINE),
                            new KeyValue(collapsedPane.scaleYProperty(), COLLAPSED_LOGO_SCALE, FXUtils.SINE)),
                    new KeyFrame(TOGGLE_DURATION,
                            new KeyValue(expandedPane.opacityProperty(), 1, FXUtils.SINE),
                            new KeyValue(expandedPane.scaleXProperty(), 1, FXUtils.SINE),
                            new KeyValue(expandedPane.scaleYProperty(), 1, FXUtils.SINE),
                            new KeyValue(expandedPane.translateXProperty(), 0, FXUtils.SINE),
                            new KeyValue(expandedPane.translateYProperty(), 0, FXUtils.SINE),
                            new KeyValue(collapsedPane.opacityProperty(), 0, FXUtils.SINE),
                            new KeyValue(collapsedPane.scaleXProperty(), 1, FXUtils.SINE),
                            new KeyValue(collapsedPane.scaleYProperty(), 1, FXUtils.SINE))
            );
            animation.setOnFinished(event -> {
                collapsedPane.setVisible(false);
                collapsedPane.setManaged(false);
            });
        } else {
            expandedPane.setOpacity(1);
            expandedPane.setScaleX(1);
            expandedPane.setScaleY(1);
            expandedPane.setTranslateX(0);
            expandedPane.setTranslateY(0);

            collapsedPane.setOpacity(0);
            collapsedPane.setScaleX(COLLAPSED_LOGO_SCALE);
            collapsedPane.setScaleY(COLLAPSED_LOGO_SCALE);

            animation.getKeyFrames().addAll(
                    new KeyFrame(javafx.util.Duration.ZERO,
                            new KeyValue(expandedPane.opacityProperty(), 1, FXUtils.SINE),
                            new KeyValue(expandedPane.scaleXProperty(), 1, FXUtils.SINE),
                            new KeyValue(expandedPane.scaleYProperty(), 1, FXUtils.SINE),
                            new KeyValue(expandedPane.translateXProperty(), 0, FXUtils.SINE),
                            new KeyValue(expandedPane.translateYProperty(), 0, FXUtils.SINE),
                            new KeyValue(collapsedPane.opacityProperty(), 0, FXUtils.SINE),
                            new KeyValue(collapsedPane.scaleXProperty(), COLLAPSED_LOGO_SCALE, FXUtils.SINE),
                            new KeyValue(collapsedPane.scaleYProperty(), COLLAPSED_LOGO_SCALE, FXUtils.SINE)),
                    new KeyFrame(TOGGLE_DURATION,
                            new KeyValue(expandedPane.opacityProperty(), 0, FXUtils.SINE),
                            new KeyValue(expandedPane.scaleXProperty(), COLLAPSED_SCALE, FXUtils.SINE),
                            new KeyValue(expandedPane.scaleYProperty(), COLLAPSED_SCALE, FXUtils.SINE),
                            new KeyValue(expandedPane.translateXProperty(), shiftX, FXUtils.SINE),
                            new KeyValue(expandedPane.translateYProperty(), -shiftY, FXUtils.SINE),
                            new KeyValue(collapsedPane.opacityProperty(), 1, FXUtils.SINE),
                            new KeyValue(collapsedPane.scaleXProperty(), 1, FXUtils.SINE),
                            new KeyValue(collapsedPane.scaleYProperty(), 1, FXUtils.SINE))
            );
            animation.setOnFinished(event -> {
                expandedPane.setVisible(false);
                expandedPane.setManaged(false);
            });
        }

        FXUtils.playAnimation(this, "cofemine-toggle", animation);
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
