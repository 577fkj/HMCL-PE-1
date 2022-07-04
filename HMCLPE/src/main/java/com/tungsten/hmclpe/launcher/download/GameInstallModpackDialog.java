package com.tungsten.hmclpe.launcher.download;

import static com.tungsten.hmclpe.launcher.uis.game.download.right.game.DownloadForgeUI.FORGE_VERSION_MANIFEST;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.gson.Gson;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.launcher.MainActivity;
import com.tungsten.hmclpe.launcher.download.fabric.FabricAPIInstallTask;
import com.tungsten.hmclpe.launcher.download.fabric.FabricGameVersion;
import com.tungsten.hmclpe.launcher.download.fabric.FabricInstallTask;
import com.tungsten.hmclpe.launcher.download.fabric.FabricLoaderVersion;
import com.tungsten.hmclpe.launcher.download.forge.ForgeDownloadTask;
import com.tungsten.hmclpe.launcher.download.forge.ForgeInstallTask;
import com.tungsten.hmclpe.launcher.download.forge.ForgeVersion;
import com.tungsten.hmclpe.launcher.download.game.MinecraftInstallTask;
import com.tungsten.hmclpe.launcher.download.game.VersionManifest;
import com.tungsten.hmclpe.launcher.download.liteloader.LiteLoaderInstallTask;
import com.tungsten.hmclpe.launcher.download.liteloader.LiteLoaderVersion;
import com.tungsten.hmclpe.launcher.download.optifine.OptifineDownloadTask;
import com.tungsten.hmclpe.launcher.download.optifine.OptifineInstallTask;
import com.tungsten.hmclpe.launcher.download.optifine.OptifineVersion;
import com.tungsten.hmclpe.launcher.game.Argument;
import com.tungsten.hmclpe.launcher.game.Artifact;
import com.tungsten.hmclpe.launcher.game.RuledArgument;
import com.tungsten.hmclpe.launcher.game.Version;
import com.tungsten.hmclpe.launcher.list.install.DownloadTaskListAdapter;
import com.tungsten.hmclpe.launcher.list.install.DownloadTaskListBean;
import com.tungsten.hmclpe.launcher.uis.game.download.DownloadUrlSource;
import com.tungsten.hmclpe.launcher.uis.game.download.right.game.DownloadForgeUI;
import com.tungsten.hmclpe.utils.file.AssetsUtils;
import com.tungsten.hmclpe.utils.file.FileStringUtils;
import com.tungsten.hmclpe.utils.gson.JsonUtils;
import com.tungsten.hmclpe.utils.io.NetSpeed;
import com.tungsten.hmclpe.utils.io.NetSpeedTimer;
import com.tungsten.hmclpe.utils.io.NetworkUtils;
import com.tungsten.hmclpe.utils.platform.Bits;
import com.tungsten.hmclpe.launcher.uis.game.download.right.game.DownloadForgeUI.ForgeCompareTool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class GameInstallModpackDialog extends Dialog implements View.OnClickListener, Handler.Callback {

    private static final String OFFICIAL_LOADER_META_URL = "https://meta.fabricmc.net/v2/versions/loader";
    private static final String OFFICIAL_GAME_META_URL = "https://meta.fabricmc.net/v2/versions/game";

    private static final String BMCLAPI_LOADER_META_URL = "https://bmclapi2.bangbang93.com/fabric-meta/v2/versions/loader";
    private static final String BMCLAPI_GAME_META_URL = "https://bmclapi2.bangbang93.com/fabric-meta/v2/versions/game";

    private Context context;
    private MainActivity activity;

    private String name;
    private String version;
    private String forgeVersion;
    private String optifineVersion;
    private String liteLoaderVersion;
    private String fabricVersion;
    private String fabricAPIVersion;

    private VersionManifest.Version nameManifest;
    private VersionManifest.Version versionManifest;
    private ForgeVersion forgeVersionManifest;
    private OptifineVersion optifineVersionManifest;
    private LiteLoaderVersion liteLoaderVersionManifest;
    private FabricLoaderVersion fabricVersionManifest;
    private FabricGameVersion fabricAPIVersionManifest;

    private MinecraftInstallTask minecraftInstallTask;
    private LiteLoaderInstallTask liteLoaderInstallTask;
    private ForgeDownloadTask forgeDownloadTask;
    private ForgeInstallTask forgeInstallTask;
    private OptifineDownloadTask optifineDownloadTask;
    private OptifineInstallTask optifineInstallTask;
    private FabricInstallTask fabricInstallTask;
    private FabricAPIInstallTask fabricAPIInstallTask;

    private Version gameVersionJson;

    private RecyclerView taskListView;
    private DownloadTaskListAdapter downloadTaskListAdapter;

    private NetSpeedTimer netSpeedTimer;
    private TextView speedText;
    private Button cancelButton;

    public GameInstallModpackDialog(@NonNull Context context, MainActivity activity, String name, String version, String forgeVersion, String optifineVersion, String liteLoaderVersion, String fabricVersion, String fabricAPIVersion) throws IOException {
        super(context);
        this.context = context;
        this.activity = activity;
        this.name = name;
        this.version = version;
        this.forgeVersion = forgeVersion;
        this.optifineVersion = optifineVersion;
        this.liteLoaderVersion = liteLoaderVersion;
        this.fabricVersion = fabricVersion;
        this.fabricAPIVersion = fabricAPIVersion;
        setContentView(R.layout.dialog_install_game);
        setCancelable(false);
        init();
    }

    @Override
    public void onClick(View v) {
        if (v == cancelButton){
            exit();
            activity.backToLastUI();
            new Thread(() -> {
                activity.uiManager.versionListUI.refreshVersionList();
            }).start();
        }
    }

    private void init() throws IOException {
        taskListView = findViewById(R.id.download_task_list);

        taskListView.setLayoutManager(new LinearLayoutManager(context));
        downloadTaskListAdapter = new DownloadTaskListAdapter(context);
        taskListView.setAdapter(downloadTaskListAdapter);
        Objects.requireNonNull(taskListView.getItemAnimator()).setAddDuration(0L);
        taskListView.getItemAnimator().setChangeDuration(0L);
        taskListView.getItemAnimator().setMoveDuration(0L);
        taskListView.getItemAnimator().setRemoveDuration(0L);
        ((SimpleItemAnimator)taskListView.getItemAnimator()).setSupportsChangeAnimations(false);

        speedText = findViewById(R.id.download_speed_text);
        cancelButton = findViewById(R.id.cancel_install_game);
        cancelButton.setOnClickListener(this);

        Handler handler = new Handler(this);
        netSpeedTimer = new NetSpeedTimer(context, new NetSpeed(), handler).setDelayTime(0).setPeriodTime(1000);
        netSpeedTimer.startSpeedTimer();

        startDownloadTasks();
    }

    private void startDownloadTasks() throws IOException {
        System.out.println("---------------------------------------------------------------source:" + DownloadUrlSource.getSource(activity.launcherSetting.downloadUrlSource));
        if (!new File(activity.launcherSetting.gameFileDirectory + "/launcher_profiles.json").exists()) {
            AssetsUtils.getInstance(activity.getApplicationContext()).copyAssetsToSD("launcher_profiles.json", activity.launcherSetting.gameFileDirectory + "/launcher_profiles.json");
        }

        DownloadTaskListBean downloadTaskListBean = new DownloadTaskListBean("正在获取版本信息...", null, null, "");
        downloadTaskListAdapter.addDownloadTask(downloadTaskListBean);
        String response = NetworkUtils.doGet(NetworkUtils.toURL(DownloadUrlSource.getSubUrl(DownloadUrlSource.getSource(activity.launcherSetting.downloadUrlSource), DownloadUrlSource.VERSION_MANIFEST)));
        downloadTaskListBean.progress = 50;
        downloadTaskListAdapter.onProgress(downloadTaskListBean);
        VersionManifest versionManifest = new Gson().fromJson(response, VersionManifest.class);
        for (VersionManifest.Version version : versionManifest.versions) {
            if (version.id.equals(this.version)) {
                this.versionManifest = version;
                break;
            }
        }
        downloadTaskListBean.progress = 100;
        downloadTaskListAdapter.onComplete(downloadTaskListBean);

        downloadMinecraft();
    }

    public void downloadMinecraft(){
        minecraftInstallTask = new MinecraftInstallTask(activity, name, downloadTaskListAdapter, new MinecraftInstallTask.InstallMinecraftCallback() {
            @Override
            public void onStart() {
                
            }

            @Override
            public void onFailed(Exception e) {
                throwException(e);
            }

            @Override
            public void onFinish(Version version) {
                gameVersionJson = version;
                downloadLiteLoader();
            }
        });
        minecraftInstallTask.execute(versionManifest);
    }

    public void downloadLiteLoader(){
        if (!liteLoaderVersion.isEmpty()) {
            // TODO: 2022/7/4 下载LiteLoader
//            liteLoaderInstallTask = new LiteLoaderInstallTask(activity, downloadTaskListAdapter, new LiteLoaderInstallTask.InstallLiteLoaderCallback() {
//                @Override
//                public void onStart() {
//
//                }
//
//                @Override
//                public void onFailed(Exception e) {
//                    throwException(e);
//                }
//
//                @Override
//                public void onFinish(Version version) {
//                    gameVersionJson = PatchMerger.mergePatch(gameVersionJson,version);
//                    downloadForge();
//                }
//            });
//            liteLoaderInstallTask.execute(liteLoaderVersionManifest);
        }
        else {
            downloadForge();
        }
    }

    public void downloadForge(){
        if (!forgeVersion.isEmpty()) {
            ArrayList<ForgeVersion> list = new ArrayList<>();
            try {
                String response = NetworkUtils.doGet(NetworkUtils.toURL(FORGE_VERSION_MANIFEST + version));
                Gson gson = new Gson();
                ForgeVersion[] forgeVersion = gson.fromJson(response, ForgeVersion[].class);
                list.addAll(Arrays.asList(forgeVersion));
                list.sort(new ForgeCompareTool());
            } catch (IOException e) {
                throwException(e);
            }
            for (ForgeVersion forgeVersion : list) {
                if (forgeVersion.getVersion().equals(this.forgeVersion) && forgeVersion.getGameVersion().equals(version)) {
                    forgeVersionManifest = forgeVersion;
                    break;
                }
            }
            forgeDownloadTask = new ForgeDownloadTask(activity, downloadTaskListAdapter, new ForgeDownloadTask.DownloadForgeCallback() {
                @Override
                public void onStart() {

                }

                @Override
                public void onFinish(Exception e) {
                    if (e == null) {
                        installForge();
                    }
                    else {
                        throwException(e);
                    }
                }
            });
            forgeDownloadTask.execute(forgeVersionManifest);
        }
        else {
            downloadOptifine();
        }
    }

    public void installForge() {
        forgeInstallTask = new ForgeInstallTask(activity, name, downloadTaskListAdapter, new ForgeInstallTask.InstallForgeCallback() {
            @Override
            public void onStart() {

            }

            @Override
            public void onFailed(Exception e) {
                throwException(e);
            }

            @Override
            public void onFinish(Version version) {
                gameVersionJson = PatchMerger.mergePatch(gameVersionJson,version);
                downloadOptifine();
            }
        });
        forgeInstallTask.execute(forgeVersionManifest);
    }

    public void downloadOptifine() {
        if (optifineVersion != null) {
            // TODO: 2022/7/4 下载Optifine
//            optifineDownloadTask = new OptifineDownloadTask(activity, downloadTaskListAdapter, new OptifineDownloadTask.DownloadOptifineCallback() {
//                @Override
//                public void onStart() {
//
//                }
//
//                @Override
//                public void onFinish(Exception e) {
//                    if (e == null) {
//                        installOptifine();
//                    }
//                    else {
//                        throwException(e);
//                    }
//                }
//            });
//            optifineDownloadTask.execute(optifineVersionManifest);
        }
        else {
            downloadFabric();
        }
    }

    public void installOptifine() {
//        optifineInstallTask = new OptifineInstallTask(activity, name, downloadTaskListAdapter, new OptifineInstallTask.InstallOptifineCallback() {
//            @Override
//            public void onStart() {
//
//            }
//
//            @Override
//            public void onFailed(Exception e) {
//                throwException(e);
//            }
//
//            @Override
//            public void onFinish(Version version) {
//                gameVersionJson = PatchMerger.mergeOptifinePatch(gameVersionJson,version);
//                downloadFabric();
//            }
//        });
//        optifineInstallTask.execute(optifineVersionManifest);
    }

    public void downloadFabric(){
        if (fabricVersion != null) {
            // TODO: 2022/7/4 下载Fabric
            String loaderUrl;
            String gameUrl;
            if (DownloadUrlSource.getSource(activity.launcherSetting.downloadUrlSource) == 0) {
                loaderUrl = OFFICIAL_LOADER_META_URL;
                gameUrl = OFFICIAL_GAME_META_URL;
            }
            else {
                loaderUrl = BMCLAPI_LOADER_META_URL;
                gameUrl = BMCLAPI_GAME_META_URL;
            }
            ArrayList<FabricLoaderVersion> loaderVersions = new ArrayList<>();
            try {
                String loaderResponse = NetworkUtils.doGet(NetworkUtils.toURL(loaderUrl));
                FabricLoaderVersion[] fabricLoaderVersions = new Gson().fromJson(loaderResponse, FabricLoaderVersion[].class);
                loaderVersions.addAll(Arrays.asList(fabricLoaderVersions));
            } catch (IOException e) {
                throwException(e);
            }
            for (FabricLoaderVersion fabricLoaderVersion : loaderVersions) {
                if (fabricLoaderVersion.version.equals(this.fabricVersion)) {
                    fabricVersionManifest = fabricLoaderVersion;
                    break;
                }
            }
            fabricInstallTask = new FabricInstallTask(activity, downloadTaskListAdapter, version, new FabricInstallTask.InstallFabricCallback() {
                @Override
                public void onStart() {

                }

                @Override
                public void onFailed(Exception e) {
                    throwException(e);
                }

                @Override
                public void onFinish(Version version) {
                    gameVersionJson = PatchMerger.mergePatch(gameVersionJson,version);
                    downloadFabricAPI();
                }
            });
            fabricInstallTask.execute(fabricVersionManifest);
        }
        else {
            downloadFabricAPI();
        }
    }

    public void downloadFabricAPI(){
        if (fabricAPIVersion != null) {
            // TODO: 2022/7/4 下载FabricAPI
//            fabricAPIInstallTask = new FabricAPIInstallTask(activity, name, downloadTaskListAdapter, new FabricAPIInstallTask.InstallFabricAPICallback() {
//                @Override
//                public void onStart() {
//
//                }
//
//                @Override
//                public void onFinish(Exception e) {
//                    if (e == null) {
//                        installJson();
//                    }
//                    else {
//                        throwException(e);
//                    }
//                }
//            });
//            fabricAPIInstallTask.execute(fabricAPIVersionManifest);
        }
        else {
            installJson();
        }
    }

    public void installJson(){
        String gameFilePath = activity.launcherSetting.gameFileDirectory;
        Gson gson = JsonUtils.defaultGsonBuilder()
                .registerTypeAdapter(Artifact.class, new Artifact.Serializer())
                .registerTypeAdapter(Bits.class, new Bits.Serializer())
                .registerTypeAdapter(RuledArgument.class, new RuledArgument.Serializer())
                .registerTypeAdapter(Argument.class, new Argument.Deserializer())
                .create();
        String string = gson.toJson(gameVersionJson);
        FileStringUtils.writeFile(gameFilePath + "/versions/" + name + "/" + name + ".json",string);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.dialog_install_success_title));
        builder.setMessage(context.getString(R.string.dialog_install_success_text));
        builder.setCancelable(false);
        builder.setPositiveButton(context.getString(R.string.dialog_install_success_positive), (dialogInterface, i) -> {
            activity.backToLastUI();
            new Thread(() -> {
                activity.uiManager.versionListUI.refreshVersionList();
            }).start();
        });
        exit();
        builder.create().show();
    }
    
    public void throwException(Exception e) {
        activity.runOnUiThread(() -> {
            exit();
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(getContext().getString(R.string.dialog_install_fail_title));
            builder.setMessage(e.toString());
            builder.setPositiveButton(getContext().getString(R.string.dialog_install_fail_positive), (dialogInterface, i) -> {});
            builder.create().show();
        });
    }

    private void exit(){
        if (minecraftInstallTask != null && minecraftInstallTask.getStatus() != null && minecraftInstallTask.getStatus() == AsyncTask.Status.RUNNING) {
            minecraftInstallTask.cancel(true);
        }
        if (liteLoaderInstallTask != null && liteLoaderInstallTask.getStatus() != null && liteLoaderInstallTask.getStatus() == AsyncTask.Status.RUNNING) {
            liteLoaderInstallTask.cancel(true);
        }
        if (forgeDownloadTask != null && forgeDownloadTask.getStatus() != null && forgeDownloadTask.getStatus() == AsyncTask.Status.RUNNING) {
            forgeDownloadTask.cancel(true);
        }
        if (forgeInstallTask != null && forgeInstallTask.getStatus() != null && forgeInstallTask.getStatus() == AsyncTask.Status.RUNNING) {
            forgeInstallTask.cancel(true);
        }
        if (optifineDownloadTask != null && optifineDownloadTask.getStatus() != null && optifineDownloadTask.getStatus() == AsyncTask.Status.RUNNING) {
            optifineDownloadTask.cancel(true);
        }
        if (optifineInstallTask != null && optifineInstallTask.getStatus() != null && optifineInstallTask.getStatus() == AsyncTask.Status.RUNNING) {
            optifineInstallTask.cancel(true);
        }
        if (fabricInstallTask != null && fabricInstallTask.getStatus() != null && fabricInstallTask.getStatus() == AsyncTask.Status.RUNNING) {
            fabricInstallTask.cancel(true);
        }
        if (fabricAPIInstallTask != null && fabricAPIInstallTask.getStatus() != null && fabricAPIInstallTask.getStatus() == AsyncTask.Status.RUNNING) {
            fabricAPIInstallTask.cancel(true);
        }
        if (forgeInstallTask != null) {
            forgeInstallTask.cancelBuild();
        }
        if (optifineInstallTask != null) {
            optifineInstallTask.cancelBuild();
        }
        netSpeedTimer.stopSpeedTimer();
        dismiss();
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (msg.what == NetSpeedTimer.NET_SPEED_TIMER_DEFAULT) {
            String speed = (String) msg.obj;
            speedText.setText(speed);
        }
        return false;
    }

}
