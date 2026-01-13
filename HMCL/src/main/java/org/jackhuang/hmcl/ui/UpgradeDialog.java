/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2021  huangyuhui <huanghongxun2008@126.com> and contributors
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
package org.jackhuang.hmcl.ui;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXSpinner;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.jackhuang.hmcl.Metadata;
import org.jackhuang.hmcl.task.Schedulers;
import org.jackhuang.hmcl.task.Task;
import org.jackhuang.hmcl.ui.construct.DialogCloseEvent;
import org.jackhuang.hmcl.ui.construct.JFXHyperlink;
import org.jackhuang.hmcl.upgrade.RemoteVersion;
import org.jackhuang.hmcl.util.StringUtils;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.io.HttpRequest;
import org.jackhuang.hmcl.util.versioning.VersionNumber;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jackhuang.hmcl.Metadata.CHANGELOG_URL;
import static org.jackhuang.hmcl.ui.FXUtils.onEscPressed;
import static org.jackhuang.hmcl.util.i18n.I18n.i18n;
import static org.jackhuang.hmcl.util.logging.Logger.LOG;

public final class UpgradeDialog extends JFXDialogLayout {

    public UpgradeDialog(RemoteVersion remoteVersion, Runnable updateRunnable) {
        maxWidthProperty().bind(Controllers.getScene().widthProperty().multiply(0.7));
        maxHeightProperty().bind(Controllers.getScene().heightProperty().multiply(0.7));

        setHeading(new Label(i18n("update.changelog")));
        setBody(new JFXSpinner());

        String url = CHANGELOG_URL + ".html";

        Task.supplyAsync(Schedulers.io(), () -> {
            VersionNumber targetVersion = VersionNumber.asVersion(remoteVersion.getVersion());
            VersionNumber currentVersion = VersionNumber.asVersion(Metadata.VERSION);
            if (targetVersion.compareTo(currentVersion) <= 0)
                // Downgrade update, no need to display changelog
                return null;

            Document document = Jsoup.parse(new URL(url), 30 * 1000);
            Node node = document.selectFirst("h1[data-version=\"%s\"]".formatted(targetVersion));

            if (node == null || !"h1".equals(node.nodeName())) {
                LOG.warning("Changelog not found, falling back to GitHub compare");
                return loadCompareChangelog(remoteVersion);
            }

            HTMLRenderer renderer = new HTMLRenderer(uri -> {
                LOG.info("Open link: " + uri);
                FXUtils.openLink(uri.toString());
            });

            do {
                if ("h1".equals(node.nodeName())) {
                    String changelogVersion = node.attr("data-version");
                    if (StringUtils.isBlank(changelogVersion) || currentVersion.compareTo(changelogVersion) >= 0) {
                        break;
                    }
                }
                renderer.appendNode(node);
                node = node.nextSibling();
            } while (node != null);

            renderer.mergeLineBreaks();
            return renderer.render();
        }).whenComplete(Schedulers.javafx(), (result, exception) -> {
            if (exception == null) {
                if (result != null) {
                    ScrollPane scrollPane = new ScrollPane(result);
                    scrollPane.setFitToWidth(true);
                    FXUtils.smoothScrolling(scrollPane);
                    setBody(scrollPane);
                } else {
                    setBody();
                }
            } else {
                LOG.warning("Failed to load update log, trying to open it in browser");
                FXUtils.openLink(url);
                setBody();
            }
        }).start();

        JFXHyperlink openInBrowser = new JFXHyperlink(i18n("web.view_in_browser"));
        openInBrowser.setExternalLink(url);

        JFXButton updateButton = new JFXButton(i18n("update.accept"));
        updateButton.getStyleClass().add("dialog-accept");
        updateButton.setOnAction(e -> updateRunnable.run());

        JFXButton cancelButton = new JFXButton(i18n("button.cancel"));
        cancelButton.getStyleClass().add("dialog-cancel");
        cancelButton.setOnAction(e -> fireEvent(new DialogCloseEvent()));

        setActions(openInBrowser, updateButton, cancelButton);
        onEscPressed(this, cancelButton::fire);
    }

    private static Node loadCompareChangelog(RemoteVersion remoteVersion) throws Exception {
        String repoPath = extractRepoPath(Metadata.GITHUB_URL);
        if (repoPath == null) {
            return null;
        }

        String baseTag = "v" + Metadata.VERSION;
        String headTag = "v" + remoteVersion.getVersion();
        if (baseTag.equals(headTag)) {
            return null;
        }

        String apiUrl = "https://api.github.com/repos/" + repoPath + "/compare/" + baseTag + "..." + headTag;
        String json = HttpRequest.GET(apiUrl).header("User-Agent", "CofeMine Launcher").getString();
        JsonObject response = JsonUtils.fromNonNullJson(json, JsonObject.class);
        JsonArray commits = response.getAsJsonArray("commits");
        if (commits == null || commits.size() == 0) {
            return null;
        }

        VBox list = new VBox(6);
        for (int i = 0; i < commits.size(); i++) {
            JsonObject commit = commits.get(i).getAsJsonObject().getAsJsonObject("commit");
            if (commit == null) {
                continue;
            }
            String message = commit.get("message").getAsString();
            String title = message.split("\n", 2)[0];
            Label item = new Label("\u2022 " + title);
            item.setWrapText(true);
            item.getStyleClass().add("subtitle-label");
            list.getChildren().add(item);
        }
        return list;
    }

    private static String extractRepoPath(String url) {
        if (url == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("github\\.com/(?<owner>[^/]+)/(?<repo>[^/#?]+)").matcher(url);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group("owner") + "/" + matcher.group("repo");
    }
}
