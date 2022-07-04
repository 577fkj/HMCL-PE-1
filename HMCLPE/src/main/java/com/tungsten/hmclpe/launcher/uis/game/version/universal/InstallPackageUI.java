package com.tungsten.hmclpe.launcher.uis.game.version.universal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tungsten.filepicker.Constants;
import com.tungsten.filepicker.FileChooser;
import com.tungsten.filepicker.FolderChooser;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.launcher.MainActivity;
import com.tungsten.hmclpe.launcher.dialogs.EditDownloadNameDialog;
import com.tungsten.hmclpe.launcher.dialogs.EditDownloadUrlDialog;
import com.tungsten.hmclpe.launcher.download.GameInstallDialog;
import com.tungsten.hmclpe.launcher.list.install.DownloadTaskListBean;
import com.tungsten.hmclpe.launcher.mod.ManuallyCreatedModpackException;
import com.tungsten.hmclpe.launcher.mod.Modpack;
import com.tungsten.hmclpe.launcher.mod.ModpackHelper;
import com.tungsten.hmclpe.launcher.mod.UnsupportedModpackException;
import com.tungsten.hmclpe.launcher.mod.hmcl.HMCLModpackInstallTask;
import com.tungsten.hmclpe.launcher.mod.mcbbs.McbbsModpackLocalInstallTask;
import com.tungsten.hmclpe.launcher.mod.mcbbs.McbbsModpackManifest;
import com.tungsten.hmclpe.launcher.mod.modrinth.ModrinthInstallTask;
import com.tungsten.hmclpe.launcher.mod.multimc.MultiMCModpackInstallTask;
import com.tungsten.hmclpe.launcher.setting.SettingUtils;
import com.tungsten.hmclpe.launcher.uis.tools.BaseUI;
import com.tungsten.hmclpe.manifest.AppManifest;
import com.tungsten.hmclpe.utils.animation.CustomAnimationUtils;
import com.tungsten.hmclpe.utils.file.UriUtils;
import com.tungsten.hmclpe.utils.io.FileUtils;
import com.tungsten.hmclpe.utils.io.ZipTools;
import com.tungsten.hmclpe.utils.string.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class InstallPackageUI extends BaseUI implements View.OnClickListener {
    public static final int SELECT_PACKAGE_REQUEST = 5700;
    public static final int SELECT_SAVE_PACKAGE_REQUEST = 5701;
    private TextView authorText;
    private EditText editName;
    private Button install;
    private LinearLayout installLayout;
    private LinearLayout installLocal;
    private LinearLayout installOnline;
    public LinearLayout installPackageUI;
    public Modpack modpack;
    private TextView nameText;
    private TextView pathText;
    private ProgressBar progressBar;
    private LinearLayout selectLayout;
    private Button showDescription;
    private TextView versionText;

    private String url = "";

    private File packFile;

    private final EditDownloadUrlDialog.CallBacks editUrlCallBacks = new EditDownloadUrlDialog.CallBacks() {
        @Override
        public void onSuccess(String url) {
            InstallPackageUI.this.url = url;
            Intent intent = new Intent(context, FolderChooser.class);
            intent.putExtra(Constants.SELECTION_MODE, Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
            intent.putExtra(Constants.INITIAL_DIRECTORY, new File(AppManifest.DEFAULT_GAME_DIR).getAbsolutePath());
            activity.startActivityForResult(intent, SELECT_SAVE_PACKAGE_REQUEST);
        }

        @Override
        public void onCancel() {

        }
    };

    private final EditDownloadNameDialog.CallBacks downloadCallbacks = new EditDownloadNameDialog.CallBacks() {
        @Override
        public void onDownloadSuccess(String path) {
            packFile = new File(path);
            parseInstallPackage(packFile);
        }

        @Override
        public void onDownloadError(ArrayList<DownloadTaskListBean> failedFile) {

        }
    };

    public InstallPackageUI(Context context, MainActivity activity) {
        super(context, activity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        installPackageUI = activity.findViewById(R.id.ui_install_package);
        selectLayout = activity.findViewById(R.id.select_package_layout);
        installLayout = activity.findViewById(R.id.install_package_layout);
        progressBar = activity.findViewById(R.id.loading_package_info_progress);
        installLocal = activity.findViewById(R.id.install_package_local);
        installOnline = activity.findViewById(R.id.install_package_online);

        installLocal.setOnClickListener(this);
        installOnline.setOnClickListener(this);

        pathText = activity.findViewById(R.id.package_path);
        nameText = activity.findViewById(R.id.package_name);
        versionText = activity.findViewById(R.id.package_version);
        authorText = activity.findViewById(R.id.package_author);
        editName = activity.findViewById(R.id.edit_package_name);
        showDescription = activity.findViewById(R.id.show_package_description);
        install = activity.findViewById(R.id.install_package);

        showDescription.setOnClickListener(this);
        install.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view == installLocal) {
            Intent intent = new Intent(context, FileChooser.class);
            intent.putExtra(Constants.SELECTION_MODE, Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
            intent.putExtra(Constants.ALLOWED_FILE_EXTENSIONS, "zip;mrpack");
            intent.putExtra(Constants.INITIAL_DIRECTORY, new File(Environment.getExternalStorageDirectory().getAbsolutePath()).getAbsolutePath());
            activity.startActivityForResult(intent, SELECT_PACKAGE_REQUEST);
        }
        if (view == showDescription && modpack != null && modpack.getDescription() != null && StringUtils.isNotBlank(modpack.getDescription())) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(context.getString(R.string.dialog_package_description_title));
            builder.setMessage(Html.fromHtml(modpack.getDescription(), 0));
            builder.setPositiveButton(context.getString(R.string.dialog_package_description_positive), null);
            builder.create().show();
        }
        if (view == installOnline) {
            new EditDownloadUrlDialog(activity, editUrlCallBacks).show();
        }
        if (view == install) {
            // TODO: install modpack
            if (editName.getText().toString().isEmpty()) {
                Toast.makeText(context, context.getString(R.string.dialog_package_name_empty), Toast.LENGTH_SHORT).show();
                return;
            }
            installPackage();
        }
    }

    private void installPackage() {
        if (modpack == null) {
            throw new NullPointerException("modpack is null");
        }
        String verName = editName.getText().toString();
        boolean exist = SettingUtils.getLocalVersionNames(activity.launcherSetting.gameFileDirectory).contains(verName);
        if (exist) {
            Toast.makeText(context, context.getString(R.string.install_game_ui_exist), Toast.LENGTH_SHORT).show();
            return;
        }
        AsyncTask installTask = modpack.getInstallTask(packFile, verName);
        switch (modpack.getManifest().getProvider().getName()) {
            case "Mcbbs":
                McbbsModpackLocalInstallTask mcbbsModpackLocalInstallTask = (McbbsModpackLocalInstallTask) installTask;
                mcbbsModpackLocalInstallTask.execute(activity);
                break;
            case "MultiMC":
                MultiMCModpackInstallTask multiMCModpackLocalInstallTask = (MultiMCModpackInstallTask) installTask;
                multiMCModpackLocalInstallTask.execute(activity);
                break;
            case "Modrinth":
                ModrinthInstallTask modrinthModpackInstallTask = (ModrinthInstallTask) installTask;
                modrinthModpackInstallTask.execute(activity);
                break;
            case "HMCL":
                HMCLModpackInstallTask hmclModpackInstallTask = (HMCLModpackInstallTask) installTask;
                hmclModpackInstallTask.execute(activity);
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        activity.showBarTitle(context.getResources().getString(R.string.install_package_ui_title), false, true);
        CustomAnimationUtils.showViewFromLeft(installPackageUI, activity, context, true);
        selectLayout.setVisibility(View.VISIBLE);
        installLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(installPackageUI, activity, context, true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_PACKAGE_REQUEST && resultCode != -1) {
            activity.backToLastUI();
        }
        if (requestCode == SELECT_PACKAGE_REQUEST && resultCode == -1 && data != null) {
            final String filePath = UriUtils.getRealPathFromUri_AboveApi19(context, data.getData());
            assert filePath != null;
            packFile = new File(filePath);
            parseInstallPackage(packFile);
        }
        if (requestCode == SELECT_SAVE_PACKAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            new EditDownloadNameDialog(context, this, url, false, UriUtils.getRealPathFromUri_AboveApi19(context, uri), downloadCallbacks).show();
        }
    }

    private void parseInstallPackage(File file) {
        String filePath = file.getAbsolutePath();
        selectLayout.setVisibility(View.GONE);
        installLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                modpack = ModpackHelper.readModpackManifest(file.toPath(), ZipTools.findSuitableEncoding(file.toPath()));
                activity.runOnUiThread(() -> {
                    selectLayout.setVisibility(View.GONE);
                    installLayout.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    pathText.setText(filePath);

                    String modPackName = modpack.getName() == null || (StringUtils.isBlank(modpack.getName())) ? FileUtils.getNameWithoutExtension(file) : modpack.getName();
                    nameText.setText(modPackName);
                    editName.setText(modPackName);

                    String version = modpack.getVersion() == null ? "" : modpack.getVersion();
                    versionText.setText(version);

                    String author = "";
                    if (modpack.getAuthor() != null) {
                        author = modpack.getAuthor();
                    }
                    authorText.setText(author);
                });
            } catch (ManuallyCreatedModpackException e) {
                e.printStackTrace();
                modpack = null;
                activity.runOnUiThread(() -> {
                    selectLayout.setVisibility(View.GONE);
                    installLayout.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    pathText.setText(filePath);
                    nameText.setText(file.getName());
                    versionText.setText("");
                    authorText.setText("");
                    editName.setText(FileUtils.getNameWithoutExtension(file));
                    AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                    dialog.setTitle(context.getString(R.string.dialog_package_manual_warn_title));
                    dialog.setMessage(context.getString(R.string.dialog_package_manual_warn_msg));
                    dialog.setPositiveButton(context.getString(R.string.dialog_package_manual_warn_positive), null);
                    dialog.setNegativeButton(context.getString(R.string.dialog_package_manual_warn_negative), (DialogInterface dialogInterface, int ii) -> activity.backToLastUI());
                    dialog.create().show();
                });
            } catch (UnsupportedModpackException | IOException e2) {
                e2.printStackTrace();
                modpack = null;
                activity.runOnUiThread(() -> {
                    activity.backToLastUI();
                    AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                    dialog.setTitle(context.getString(R.string.dialog_package_not_support_title));
                    dialog.setMessage(context.getString(R.string.dialog_package_not_support_msg));
                    dialog.setPositiveButton(context.getString(R.string.dialog_package_not_support_exit), null);
                    dialog.create().show();
                });
            }
        }).start();
    }
}
