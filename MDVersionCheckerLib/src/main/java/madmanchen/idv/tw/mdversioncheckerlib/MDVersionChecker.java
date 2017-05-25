package madmanchen.idv.tw.mdversioncheckerlib;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.IntDef;
import android.support.v7.app.AlertDialog;
import android.webkit.URLUtil;

import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import madmanchen.idv.tw.mdversioncheckerlib.utils.FileUtils;

/**
 * Author:      chenshaowei
 * Version      V1.0
 * Description:
 * Modification History:
 * Date         Author          version         Description
 * ---------------------------------------------------------------------
 * 2017/5/24      chenshaowei         V1.0.0          Create
 * Why & What is modified:
 */

public class MDVersionChecker extends AsyncTask<String, Number, Boolean> {
    private static final String TAG = "MDVersionChecker";
    public static final int GOOGLE_PLAY = 0;
    public static final int SERVER = 1;

    @IntDef({GOOGLE_PLAY, SERVER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CheckType {
    }

    public interface CheckVersionCallback {
        void same(Map<String, String> infoMap);

        void different(Map<String, String> infoMap, AlertDialog.Builder updateDialog);

        void error(String error);
    }

    private CheckVersionCallback mVersionCallback;
    private ProgressDialog mProgressDialog;
    private AlertDialog.Builder mAlertDialog;

    private String updateBtnText = "update";
    private String cancelBtnText = "cancel";
    private DialogInterface.OnClickListener mUpdateOnClickListener;
    private DialogInterface.OnClickListener mCanceloOnClickListener;

    private Map<String, String> infoMap = new HashMap<>();
    private String pkgName, verName;
    private String url;
    private String downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
    private String providerName = "fileprovider";
    private int checkType = 0;

    public MDVersionChecker checkGooglePlay(String pkgName, String verName) {
        checkType = GOOGLE_PLAY;
        this.pkgName = pkgName;
        this.verName = verName;
        return this;
    }

    public MDVersionChecker checkServer(String url, String pkgName, String verName) {
        checkType = SERVER;
        this.url = url;
        this.pkgName = pkgName;
        this.verName = verName;
        return this;
    }

    public MDVersionChecker setLoadingView(Context context, String title, String msg) {
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(msg);
        mProgressDialog.setCancelable(false);
        return this;
    }

    public MDVersionChecker setUpdateDialog(Context context, String title, String msg) {
        mAlertDialog = new AlertDialog.Builder(context);
        mAlertDialog.setTitle(title);
        mAlertDialog.setMessage(msg);
        mAlertDialog.setCancelable(false);
        return this;
    }

    public MDVersionChecker setUpdateButton(String btnText, DialogInterface.OnClickListener listener) {
        updateBtnText = btnText;
        mUpdateOnClickListener = listener;
        return this;
    }

    public MDVersionChecker setCancelButton(String btnText, DialogInterface.OnClickListener listener) {
        cancelBtnText = btnText;
        mCanceloOnClickListener = listener;
        return this;
    }

    public MDVersionChecker setDownloadPath(String path) {
        this.downloadPath = path;
        return this;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public MDVersionChecker setProviderName(String providerName) {
        this.providerName = providerName;
        return this;
    }


    public void check(CheckVersionCallback callback) {
        this.mVersionCallback = callback;
        execute();
    }

    private String getVerNameFromGooglePlay(String pkgName) {
        String url = "https://play.google.com/store/apps/details?id=" + pkgName;
        String verName = null;
        try {
            verName = Jsoup.connect(url)
                    .timeout(10000)
                    .get()
                    .select("div[itemprop=softwareVersion]")
                    .first()
                    .ownText();
        } catch (Exception e) {
            e.printStackTrace();
            if (mVersionCallback != null) {
                mVersionCallback.error(e.getMessage());
            }
        }
        infoMap.put("apkUrl", url);
        infoMap.put("verName", verName);
        return verName;
    }

    private String getVerNameFromServer(String pkgName) {
        String verName = null;
        String apkUrl = null;
        try {
            URL url = new URL(this.url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.getOutputStream().write(("pkgName=" + pkgName).getBytes());
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream());
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                byte[] buffer = new byte[2048];
                int buffLen;
                while ((buffLen = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, buffLen);
                }
                String json = outputStream.toString();
                if (json != null) {
                    JSONObject jsonObject = new JSONObject(json);
                    boolean result = jsonObject.optBoolean("result");
                    String msg = jsonObject.optString("msg");
                    if (result) {
                        JSONObject contentJObj = jsonObject.optJSONObject("content");
                        verName = contentJObj.optString("verName");
                        apkUrl = contentJObj.optString("apkUrl");
                    } else {
                        if (mVersionCallback != null) {
                            mVersionCallback.error(msg);
                        }
                    }
                }
                inputStream.close();
                outputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (mVersionCallback != null) {
                mVersionCallback.error(e.getMessage());
            }
        }
        infoMap.put("apkUrl", apkUrl);
        infoMap.put("verName", verName);
        return verName;
    }

    private DialogInterface.OnClickListener getDefaultUpdateOnClickListener() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                switch (checkType) {
                    case GOOGLE_PLAY:
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkgName));
                        if (!haveIntent(mAlertDialog.getContext(), intent)) {
                            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + pkgName));
                        }
                        mAlertDialog.getContext().startActivity(intent);
                        break;

                    case SERVER:
                        final String apkUrl = infoMap.get("apkUrl");
                        final String fileName = URLUtil.guessFileName(apkUrl, null, null);
                        final File apkFile = new File(downloadPath, fileName);
                        final Context context = mAlertDialog.getContext();

                        mProgressDialog = new ProgressDialog(context);
                        mProgressDialog.setMessage(fileName);
                        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        mProgressDialog.setProgressNumberFormat("%dKB/%dKB");
                        mProgressDialog.show();

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (apkUrl != null) {
                                    try {
                                        URL url = new URL(apkUrl);
                                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                        connection.setDoOutput(true);
                                        connection.setDoInput(true);
                                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                            BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream());

                                            FileOutputStream fileOutputStream = new FileOutputStream(apkFile);
                                            int fileLength = connection.getContentLength();
                                            byte[] buffer = new byte[2048];
                                            int buffLen;

                                            while ((buffLen = inputStream.read(buffer)) > 0) {
                                                fileOutputStream.write(buffer, 0, buffLen);
                                                mProgressDialog.setMax((int) (fileLength / Math.pow(1024, 1)));
                                                mProgressDialog.setProgress((int) (fileLength / Math.pow(1024, 1)));
                                            }
                                            inputStream.close();
                                            fileOutputStream.close();
                                            mProgressDialog.dismiss();
                                            FileUtils.smartOpenFile(context, apkFile, providerName);
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        if (mVersionCallback != null) {
                                            mVersionCallback.error(e.getMessage());
                                        }
                                    }
                                }
                            }
                        }).start();
                        break;
                }
            }
        };
    }

    private DialogInterface.OnClickListener getDefaultCancelOnClickListener() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        };
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (mProgressDialog != null && !mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
    }

    @Override
    protected void onProgressUpdate(Number... values) {
        super.onProgressUpdate(values);
        if (mProgressDialog != null) {
            mProgressDialog.setProgress(values[0].intValue());
            mProgressDialog.setMax(values[1].intValue());
        }
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        String verName = null;
        switch (checkType) {
            case GOOGLE_PLAY:
                verName = getVerNameFromGooglePlay(pkgName);
                break;

            case SERVER:
                verName = getVerNameFromServer(pkgName);
                break;
        }
        return this.verName.equals(verName);
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        if (mVersionCallback != null) {
            if (aBoolean) {
                mVersionCallback.same(infoMap);
            } else {
                if (mAlertDialog != null) {
                    mAlertDialog.setNegativeButton(updateBtnText, mUpdateOnClickListener != null ? mUpdateOnClickListener : getDefaultUpdateOnClickListener());
                    mAlertDialog.setNeutralButton(cancelBtnText, mCanceloOnClickListener != null ? mCanceloOnClickListener : getDefaultCancelOnClickListener());
                }
                mVersionCallback.different(infoMap, mAlertDialog);
            }
        }
    }

    private boolean haveIntent(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        List activities = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return activities.size() > 0;
    }

    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}
