package com.tungsten.hmclpe.launcher.mod;

import android.os.AsyncTask;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

public abstract class Modpack {
    private String author;
    private String description;
    private transient Charset encoding;
    private String gameVersion;
    private ModpackManifest manifest;
    private String name;
    private String version;

    public abstract AsyncTask getInstallTask(File file, String str);

    public Modpack() {
        this("", null, null, null, null, null, null);
    }

    public Modpack(String name, String author, String version, String gameVersion, String description, Charset encoding, ModpackManifest manifest) {
        this.name = name;
        this.author = author;
        this.version = version;
        this.gameVersion = gameVersion;
        this.description = description;
        this.encoding = encoding;
        this.manifest = manifest;
    }

    public String getName() {
        return name;
    }

    public Modpack setName(String str) {
        name = str;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public Modpack setAuthor(String str) {
        author = str;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public Modpack setVersion(String str) {
        version = str;
        return this;
    }

    public String getGameVersion() {
        return gameVersion;
    }

    public Modpack setGameVersion(String str) {
        gameVersion = str;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Modpack setDescription(String str) {
        description = str;
        return this;
    }

    public Charset getEncoding() {
        return encoding;
    }

    public Modpack setEncoding(Charset charset) {
        encoding = charset;
        return this;
    }

    public ModpackManifest getManifest() {
        return manifest;
    }

    public Modpack setManifest(ModpackManifest modpackManifest) {
        manifest = modpackManifest;
        return this;
    }

    public static boolean acceptFile(String str, List<String> list, List<String> list2) {
        if (str.isEmpty()) {
            return true;
        }
        for (String str2 : list) {
            if (str.equals(str2)) {
                return false;
            }
        }
        if (list2 == null || list2.isEmpty()) {
            return true;
        }
        for (String str3 : list2) {
            if (str.equals(str3)) {
                return true;
            }
        }
        return false;
    }
}
