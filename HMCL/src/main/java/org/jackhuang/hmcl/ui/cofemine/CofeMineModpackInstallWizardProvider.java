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

import javafx.beans.property.BooleanProperty;
import javafx.scene.Node;
import org.jackhuang.hmcl.cofemine.CofeMineInstallPlan;
import org.jackhuang.hmcl.cofemine.CofeMineModpackManifest;
import org.jackhuang.hmcl.cofemine.CofeMineModpackService;
import org.jackhuang.hmcl.download.DownloadProvider;
import org.jackhuang.hmcl.download.RemoteVersion;
import org.jackhuang.hmcl.setting.DownloadProviders;
import org.jackhuang.hmcl.setting.Profile;
import org.jackhuang.hmcl.task.Schedulers;
import org.jackhuang.hmcl.task.Task;
import org.jackhuang.hmcl.ui.download.InstallersPage;
import org.jackhuang.hmcl.ui.download.UpdateInstallerWizardProvider;
import org.jackhuang.hmcl.ui.download.VersionsPage;
import org.jackhuang.hmcl.ui.wizard.WizardController;
import org.jackhuang.hmcl.ui.wizard.WizardProvider;
import org.jackhuang.hmcl.util.SettingsMap;
import org.jackhuang.hmcl.util.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.jackhuang.hmcl.util.i18n.I18n.i18n;

public final class CofeMineModpackInstallWizardProvider implements WizardProvider {
    private final Profile profile;
    private final Path targetDir;
    private final String zipUrl;
    private final String manifestUrl;
    private final CofeMineModpackManifest manifest;
    private final CofeMineModpackService modpackService;
    private final boolean preferArchiveDescriptor;
    private final BooleanProperty busy;
    private final Runnable onSuccess;
    private final DownloadProvider downloadProvider = DownloadProviders.getDownloadProvider();

    public CofeMineModpackInstallWizardProvider(Profile profile,
                                                Path targetDir,
                                                String zipUrl,
                                                String manifestUrl,
                                                CofeMineModpackManifest manifest,
                                                CofeMineModpackService modpackService,
                                                boolean preferArchiveDescriptor,
                                                BooleanProperty busy,
                                                Runnable onSuccess) {
        this.profile = profile;
        this.targetDir = targetDir;
        this.zipUrl = zipUrl;
        this.manifestUrl = manifestUrl;
        this.manifest = manifest;
        this.modpackService = modpackService;
        this.preferArchiveDescriptor = preferArchiveDescriptor;
        this.busy = busy;
        this.onSuccess = onSuccess;
    }

    @Override
    public void start(SettingsMap settings) {
    }

    @Override
    public Object finish(SettingsMap settings) {
        RemoteVersion gameVersion = (RemoteVersion) settings.get("game");
        if (gameVersion == null) {
            throw new IllegalStateException("Missing game version selection");
        }
        String name = (String) settings.get("name");
        List<RemoteVersion> loaders = new ArrayList<>();
        settings.asStringMap().forEach((key, value) -> {
            if (!"game".equals(key) && value instanceof RemoteVersion remoteVersion) {
                loaders.add(remoteVersion);
            }
        });

        CofeMineInstallPlan plan = new CofeMineInstallPlan(
                StringUtils.isBlank(name) ? null : name,
                gameVersion.getGameVersion(),
                loaders,
                preferArchiveDescriptor
        );

        Task<Void> task;
        try {
            task = modpackService.createInstallTask(profile, targetDir, zipUrl, manifest, manifestUrl, plan);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start modpack install", e);
        }

        if (busy != null) {
            busy.set(true);
        }
        Task<Void> wrapped = task.whenComplete(Schedulers.javafx(), exception -> {
            if (busy != null) {
                busy.set(false);
            }
            if (exception == null && onSuccess != null) {
                onSuccess.run();
            }
        });

        settings.put("title", i18n("cofemine.modpack.installing"));
        settings.put("success_message", i18n("install.success"));
        settings.put(FailureCallback.KEY, (ignored, exception, next) -> UpdateInstallerWizardProvider.alertFailureMessage(exception, next));
        return wrapped;
    }

    @Override
    public Node createPage(WizardController controller, int step, SettingsMap settings) {
        switch (step) {
            case 0:
                return new VersionsPage(controller, i18n("install.installer.choose", i18n("install.installer.game")), "",
                        downloadProvider, "game",
                        () -> controller.onNext(new InstallersPage(controller, profile.getRepository(),
                                ((RemoteVersion) controller.getSettings().get("game")).getGameVersion(), downloadProvider)));
            default:
                throw new IllegalStateException("error step " + step + ", settings: " + settings + ", pages: " + controller.getPages());
        }
    }

    @Override
    public boolean cancel() {
        return true;
    }
}
