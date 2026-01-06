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
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.jackhuang.hmcl.cofemine.CofeMineServerStatus;
import org.jackhuang.hmcl.cofemine.CofeMineServerStatusService;
import org.jackhuang.hmcl.ui.FXUtils;
import org.jackhuang.hmcl.ui.SVG;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.jackhuang.hmcl.util.i18n.I18n.i18n;

public final class CofeMinePane extends VBox {
    private static final String SITE_URL = "https://cofemine.ru";
    private static final String SERVER_HOST = "cofemine.online";
    private static final int SERVER_PORT = 25565;

    private final CofeMineServerStatusService statusService = new CofeMineServerStatusService(
            SERVER_HOST, SERVER_PORT, Duration.ofSeconds(30));

    private final Circle statusDot = new Circle(4);
    private final Label statusLabel = new Label();
    private final Label statusDetail = new Label();
    private final Label statusMotd = new Label();

    public CofeMinePane() {
        getStyleClass().addAll("card", "cofemine-panel");

        Label title = new Label(i18n("cofemine.panel.title"));
        title.getStyleClass().add("cofemine-title");
        Label subtitle = new Label(i18n("cofemine.panel.subtitle"));
        subtitle.getStyleClass().add("cofemine-subtitle");

        VBox header = new VBox(2, title, subtitle);

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

        VBox actionBox = new VBox(8, siteButton);
        actionBox.setFillWidth(true);
        siteButton.setMaxWidth(Double.MAX_VALUE);

        getChildren().setAll(header, statusBox, actionBox);

        statusService.statusProperty().addListener((obs, oldVal, newVal) -> updateStatus(newVal));
        updateStatus(statusService.getStatus());

        statusService.start();
    }

    private void updateStatus(CofeMineServerStatus status) {
        statusDot.getStyleClass().setAll("cofemine-status-dot");
        boolean online = status != null && status.online();
        if (online) {
            statusDot.getStyleClass().add("online");
            statusLabel.setText(i18n("cofemine.server.online"));
        } else if (status != null && "loading".equals(status.error())) {
            statusLabel.setText(i18n("cofemine.server.loading"));
        } else {
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
}
