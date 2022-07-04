package com.tungsten.hmclpe.launcher.mod.mcbbs;

import android.os.AsyncTask;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.tungsten.hmclpe.launcher.download.LibraryAnalyzer;
import com.tungsten.hmclpe.launcher.game.Library;
import com.tungsten.hmclpe.launcher.mod.Modpack;
import com.tungsten.hmclpe.launcher.mod.ModpackManifest;
import com.tungsten.hmclpe.launcher.mod.ModpackProvider;
import com.tungsten.hmclpe.utils.gson.tools.JsonSubtype;
import com.tungsten.hmclpe.utils.gson.tools.JsonType;
import com.tungsten.hmclpe.utils.gson.tools.TolerableValidationException;
import com.tungsten.hmclpe.utils.gson.tools.Validation;
import com.tungsten.hmclpe.utils.io.NetworkUtils;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class McbbsModpackManifest implements ModpackManifest, Validation {
    public static final String MANIFEST_TYPE = "minecraftModpack";
    private final List<Addon> addons;
    private final String author;
    private final String description;
    private final String fileApi;
    private final List<File> files;
    private final boolean forceUpdate;
    private final LaunchInfo launchInfo;
    private final List<Library> libraries;
    private final String manifestType;
    private final int manifestVersion;
    private final String name;
    @SerializedName("origin")
    private final List<Origin> origins;
    private final Settings settings;
    private final String url;
    private final String version;

    public McbbsModpackManifest() {
        this(MANIFEST_TYPE, 1, "", "", "", "", null, "", false, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), new Settings(), new LaunchInfo());
    }

    public McbbsModpackManifest(String manifestType, int manifestVersion, String name, String version, String author, String description, String fileApi, String url, boolean forceUpdate, List<Origin> origins, List<Addon> addons, List<Library> libraries, List<File> files, Settings settings, LaunchInfo launchInfo) {
        this.manifestType = manifestType;
        this.manifestVersion = manifestVersion;
        this.name = name;
        this.version = version;
        this.author = author;
        this.description = description;
        this.fileApi = fileApi;
        this.url = url;
        this.forceUpdate = forceUpdate;
        this.origins = origins;
        this.addons = addons;
        this.libraries = libraries;
        this.files = files;
        this.settings = settings;
        this.launchInfo = launchInfo;
    }

    public String getManifestType() {
        return manifestType;
    }

    public int getManifestVersion() {
        return manifestVersion;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public String getFileApi() {
        return fileApi;
    }

    public String getUrl() {
        return url;
    }

    public boolean isForceUpdate() {
        return forceUpdate;
    }

    public List<Origin> getOrigins() {
        return origins;
    }

    public List<Addon> getAddons() {
        return addons;
    }

    public List<Library> getLibraries() {
        return libraries;
    }

    public List<File> getFiles() {
        return files;
    }

    public Settings getSettings() {
        return settings;
    }

    public LaunchInfo getLaunchInfo() {
        return launchInfo;
    }

    public McbbsModpackManifest setFiles(List<File> list) {
        return new McbbsModpackManifest(manifestType, manifestVersion, name, version, author, description, fileApi, url, forceUpdate, origins, addons, libraries, list, settings, launchInfo);
    }

    @Override
    public ModpackProvider getProvider() {
        return McbbsModpackProvider.INSTANCE;
    }

    @Override
    public void validate() throws JsonParseException, TolerableValidationException {
        if (!MANIFEST_TYPE.equals(manifestType)) {
            throw new JsonParseException("McbbsModpackManifest.manifestType must be 'minecraftModpack'");
        } else if (files == null) {
            throw new JsonParseException("McbbsModpackManifest.files cannot be null");
        } else if (addons == null) {
            throw new JsonParseException("McbbsModpackManifest.addons cannot be null");
        }
    }

    public static final class Origin {
        private final int id;
        private final String type;

        public Origin() {
            this("", 0);
        }

        public Origin(String str, int i) {
            type = str;
            id = i;
        }

        public String getType() {
            return type;
        }

        public int getId() {
            return id;
        }
    }

    public static final class Addon {
        private final String id;
        private final String version;

        public Addon() {
            this("", "");
        }

        public Addon(String str, String str2) {
            id = str;
            version = str2;
        }

        public String getId() {
            return id;
        }

        public String getVersion() {
            return version;
        }
    }

    public static final class Settings {
        @SerializedName("install_mods")
        private final boolean installMods;
        @SerializedName("install_resourcepack")
        private final boolean installResourcepack;

        public Settings() {
            this(true, true);
        }

        public Settings(boolean z, boolean z2) {
            installMods = z;
            installResourcepack = z2;
        }

        public boolean isInstallMods() {
            return installMods;
        }

        public boolean isInstallResourcepack() {
            return installResourcepack;
        }
    }

    @JsonType(property = "type", subtypes = {@JsonSubtype(clazz = AddonFile.class, name = "addon"), @JsonSubtype(clazz = CurseFile.class, name = "curse")})
    public static abstract class File implements Validation {
        protected final boolean force;

        @Override
        public void validate() throws JsonParseException, TolerableValidationException {
        }

        public File(boolean z) {
            force = z;
        }

        public boolean isForce() {
            return force;
        }
    }

    public static final class AddonFile extends File {
        private final String hash;
        private final String path;

        public AddonFile(boolean z, String str, String str2) {
            super(z);
            path = (String) Objects.requireNonNull(str);
            hash = (String) Objects.requireNonNull(str2);
        }

        public String getPath() {
            return path;
        }

        public String getHash() {
            return hash;
        }

        @Override
        public void validate() throws JsonParseException, TolerableValidationException {
            super.validate();
            Validation.requireNonNull(path, "AddonFile.path cannot be null");
            Validation.requireNonNull(hash, "AddonFile.hash cannot be null");
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            return path.equals(((AddonFile) obj).path);
        }

        public int hashCode() {
            return Objects.hash(path);
        }
    }

    public static final class CurseFile extends File {
        private final int fileID;
        private final String fileName;
        private final int projectID;
        private final String url;

        public CurseFile() {
            this(false, 0, 0, "", "");
        }

        public CurseFile(boolean z, int i, int i2, String str, String str2) {
            super(z);
            projectID = i;
            fileID = i2;
            fileName = str;
            url = str2;
        }

        public int getProjectID() {
            return projectID;
        }

        public int getFileID() {
            return fileID;
        }

        public String getFileName() {
            return fileName;
        }

        public URL getUrl() {
            String str = url;
            return str == null ? NetworkUtils.toURL("https://www.curseforge.com/minecraft/mc-mods/" + projectID + "/download/" + fileID + "/file") : NetworkUtils.toURL(NetworkUtils.encodeLocation(str));
        }

        public CurseFile withFileName(String str) {
            return new CurseFile(force, projectID, fileID, str, url);
        }

        public CurseFile withURL(String str) {
            return new CurseFile(force, projectID, fileID, fileName, str);
        }

        @Override
        public void validate() throws JsonParseException, TolerableValidationException {
            super.validate();
            if (projectID == 0 || fileID == 0) {
                throw new JsonParseException("CurseFile.{projectID|fileID} cannot be empty.");
            }
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            CurseFile curseFile = (CurseFile) obj;
            return projectID == curseFile.projectID && fileID == curseFile.fileID;
        }

        public int hashCode() {
            return Objects.hash(projectID, fileID);
        }
    }


    public static final class LaunchInfo {
        @SerializedName("javaArgument")
        private final List<String> javaArguments;
        @SerializedName("launchArgument")
        private final List<String> launchArguments;
        private final int minMemory;
        private final List<Integer> supportJava;

        public LaunchInfo() {
            this(0, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }

        public LaunchInfo(int i, List<Integer> list, List<String> list2, List<String> list3) {
            minMemory = i;
            supportJava = list;
            launchArguments = list2;
            javaArguments = list3;
        }

        public int getMinMemory() {
            return minMemory;
        }

        public List<Integer> getSupportJava() {
            return supportJava;
        }

        public List<String> getLaunchArguments() {
            return Optional.ofNullable(launchArguments).orElseGet(Collections::emptyList);
        }

        public List<String> getJavaArguments() {
            return Optional.ofNullable(javaArguments).orElseGet(Collections::emptyList);
        }
    }

    public static class ServerInfo {
        private final String authlibInjectorServer;

        public ServerInfo() {
            this(null);
        }

        public ServerInfo(String str) {
            authlibInjectorServer = str;
        }

        public String getAuthlibInjectorServer() {
            return authlibInjectorServer;
        }
    }

    public Modpack toModpack(Charset arg11) throws IOException {
        String version = addons.stream().filter((Addon addon) -> LibraryAnalyzer.LibraryType.MINECRAFT.getPatchId().equals(addon.id)).findAny().orElseThrow(() -> new IOException("Cannot find game version")).getVersion();
        return new Modpack(name, author, this.version, version, this.description, arg11, this) {
            @Override
            public AsyncTask getInstallTask(java.io.File arg3, String arg4) {
                return new McbbsModpackLocalInstallTask(arg3, this, McbbsModpackManifest.this, arg4);
            }
        };
    }
}
