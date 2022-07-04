package com.tungsten.hmclpe.launcher.mod.mcbbs;

import android.os.AsyncTask;

import com.tungsten.hmclpe.launcher.MainActivity;
import com.tungsten.hmclpe.launcher.download.GameInstallDialog;
import com.tungsten.hmclpe.launcher.download.GameInstallModpackDialog;
import com.tungsten.hmclpe.launcher.download.LibraryAnalyzer;
import com.tungsten.hmclpe.launcher.mod.Modpack;
import java.io.File;
import java.io.IOException;

public class McbbsModpackLocalInstallTask extends AsyncTask<Object, Integer, Exception> {
    Modpack modpack;
    File file;
    String versionName;
    McbbsModpackManifest mcbbsModpackManifest;

    @Override
    public Exception doInBackground(Object... objArr) {
        MainActivity activity = (MainActivity) objArr[0];
        String forgeVersion =
                mcbbsModpackManifest
                        .getAddons()
                        .stream()
                        .filter((McbbsModpackManifest.Addon addon) ->
                                LibraryAnalyzer.LibraryType.FORGE.getPatchId().equals(addon.getId())
                        )
                        .findAny()
                        .orElseGet(McbbsModpackManifest.Addon::new)
                        .getVersion();
        String optifineVersion =
                mcbbsModpackManifest
                        .getAddons()
                        .stream()
                        .filter((McbbsModpackManifest.Addon addon) ->
                                LibraryAnalyzer.LibraryType.OPTIFINE.getPatchId().equals(addon.getId())
                        )
                        .findAny()
                        .orElseGet(McbbsModpackManifest.Addon::new)
                        .getVersion();
        String liteLoaderVersion =
                mcbbsModpackManifest
                        .getAddons()
                        .stream()
                        .filter((McbbsModpackManifest.Addon addon) ->
                                LibraryAnalyzer.LibraryType.LITELOADER.getPatchId().equals(addon.getId())
                        )
                        .findAny()
                        .orElseGet(McbbsModpackManifest.Addon::new)
                        .getVersion();
        String fabricVersion =
                mcbbsModpackManifest
                        .getAddons()
                        .stream()
                        .filter((McbbsModpackManifest.Addon addon) ->
                                LibraryAnalyzer.LibraryType.FABRIC.getPatchId().equals(addon.getId())
                        )
                        .findAny()
                        .orElseGet(McbbsModpackManifest.Addon::new)
                        .getVersion();
        String fabricAPIVersion =
                mcbbsModpackManifest
                        .getAddons()
                        .stream()
                        .filter((McbbsModpackManifest.Addon addon) ->
                                LibraryAnalyzer.LibraryType.FABRIC_API.getPatchId().equals(addon.getId())
                        )
                        .findAny()
                        .orElseGet(McbbsModpackManifest.Addon::new)
                        .getVersion();
        try {
            GameInstallModpackDialog gameInstallDialog = new GameInstallModpackDialog(activity, activity, versionName, modpack.getGameVersion(), forgeVersion, optifineVersion, liteLoaderVersion, fabricVersion, fabricAPIVersion);
            gameInstallDialog.show();
        } catch (IOException e) {
            return e;
        }
        return null;
    }

    public McbbsModpackLocalInstallTask(File file, Modpack modpack, McbbsModpackManifest mcbbsModpackManifest, String versionName) {
        this.file = file;
        this.modpack = modpack;
        this.mcbbsModpackManifest = mcbbsModpackManifest;
        this.versionName = versionName;

    }
}
