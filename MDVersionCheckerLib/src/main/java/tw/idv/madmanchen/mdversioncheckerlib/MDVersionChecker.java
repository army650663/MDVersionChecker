package tw.idv.madmanchen.mdversioncheckerlib;

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
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.v7.app.AlertDialog;
import android.util.Log;
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

import tw.idv.madmanchen.mdversioncheckerlib.utils.FileUtils;

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

    private VerCheckCallback mVersionVerCheckCallback;
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
    private boolean isStepCompare;
    private boolean isCompel;


    /**
     * 至 Google Play 檢查版本
     *
     * @param pkgName 包名
     * @param verName 版本名
     */
    public MDVersionChecker checkGooglePlay(String pkgName, String verName) {
        checkType = GOOGLE_PLAY;
        this.pkgName = pkgName;
        this.verName = verName;
        return this;
    }

    /**
     * 至 Server 檢查版本
     *
     * @param url     Server 網址
     * @param pkgName 包名
     * @param verName 版本名
     */
    public MDVersionChecker checkServer(String url, String pkgName, String verName) {
        checkType = SERVER;
        this.url = url;
        this.pkgName = pkgName;
        this.verName = verName;
        return this;
    }

    /**
     * 設定讀取視窗
     *
     * @param context Context
     * @param title   標題
     * @param msg     訊息
     */
    public MDVersionChecker setLoadingView(Context context, String title, String msg) {
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(msg);
        mProgressDialog.setCancelable(false);
        return this;
    }

    /**
     * 設定升級視窗
     *
     * @param context Context
     * @param title   標題
     * @param msg     訊息
     */
    public MDVersionChecker setUpdateDialog(Context context, String title, String msg) {
        mAlertDialog = new AlertDialog.Builder(context);
        mAlertDialog.setTitle(title);
        mAlertDialog.setMessage(msg);
        mAlertDialog.setCancelable(false);
        return this;
    }

    /**
     * 是否逐步比較版本
     * <p>
     * 系統版本 1.33.060 伺服器版本 1.33.061
     * 1 = 1, 33 = 33, 060 < 061 return false
     *
     * @param isStepCompare 是否逐步比較
     */
    public MDVersionChecker isStepCompare(boolean isStepCompare) {
        this.isStepCompare = isStepCompare;
        return this;
    }

    /**
     * 設定更新按鈕
     *
     * @param btnText  按鈕文字
     * @param listener 監聽器
     */
    public MDVersionChecker setUpdateButton(String btnText, DialogInterface.OnClickListener listener) {
        updateBtnText = btnText;
        mUpdateOnClickListener = listener;
        return this;
    }

    /**
     * 設定取消按鈕
     *
     * @param btnText  按鈕文字
     * @param listener 監聽器
     */
    public MDVersionChecker setCancelButton(String btnText, DialogInterface.OnClickListener listener) {
        cancelBtnText = btnText;
        mCanceloOnClickListener = listener;
        return this;
    }

    public MDVersionChecker isCompel(boolean isCompel) {
        this.isCompel = isCompel;
        return this;
    }

    /**
     * 設定下載路徑
     *
     * @param path 檔案路徑
     *             預設路徑為 Downloads 資料夾
     */
    public MDVersionChecker setDownloadPath(String path) {
        this.downloadPath = path;
        return this;
    }

    /**
     * 設定 Provider 名稱
     * <p>
     * Android N 以上使用外部程式開啟內部檔案需要使用 Provider
     *
     * @param providerName Provider 名稱
     */
    @TargetApi(Build.VERSION_CODES.N)
    public MDVersionChecker setProviderName(String providerName) {
        this.providerName = providerName;
        return this;
    }

    /**
     * 開始檢查
     *
     * @param verCheckCallback 檢查回傳
     */
    public void check(VerCheckCallback verCheckCallback) {
        this.mVersionVerCheckCallback = verCheckCallback;
        execute(pkgName);
    }


    /**
     * 至 Google Play 取得版本名
     * <p>
     * 使用 Jsoup 抓取 Google Play html 版本名稱
     *
     * @param pkgName 包名
     */
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
            if (mVersionVerCheckCallback != null) {
                mVersionVerCheckCallback.error(e.getMessage());
            }
        }
        infoMap.put("apkUrl", url);
        infoMap.put("verName", verName);

        return verName;
    }


    /**
     * 至 Server 取得版本名
     * <p>
     * 使用 API 取得版本名稱及 APK 下載位置
     *
     * @param pkgName 包名
     */
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
                        verName = contentJObj.optString("vername_android");
                        apkUrl = contentJObj.optString("apkurl");
                        Log.i(TAG, "getVerNameFromServer: " + contentJObj.toString());
                        Log.i(TAG, "getVerNameFromServer: " + apkUrl);
                    } else {
                        if (mVersionVerCheckCallback != null) {
                            mVersionVerCheckCallback.error(msg);
                        }
                    }
                }
                inputStream.close();
                outputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (mVersionVerCheckCallback != null) {
                mVersionVerCheckCallback.error(e.getMessage());
            }
        }
        infoMap.put("apkUrl", apkUrl);
        Log.i(TAG, "getVerNameFromServer: " + apkUrl);
        infoMap.put("verName", verName);
        return verName;
    }

    /**
     * 取得預設更新按鈕監聽器
     */
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
                        if (mProgressDialog != null && mProgressDialog.isShowing()) {
                            mProgressDialog.dismiss();
                        }
                        final String apkUrl = infoMap.get("apkUrl");
                        Log.i(TAG, apkUrl);
                        final String fileName = URLUtil.guessFileName(apkUrl, null, null);
                        final File apkFile = new File(downloadPath, fileName);
                        final Context context = mAlertDialog.getContext();

                        final ProgressDialog downloadDialog = new ProgressDialog(context);
                        downloadDialog.setMessage(fileName);
                        downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        downloadDialog.setProgressNumberFormat("%dKB/%dKB");
                        downloadDialog.show();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (apkUrl != null) {
                                    try {
                                        URL url = new URL(apkUrl);
                                        Log.i(TAG, "run: " + apkUrl);
                                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                        connection.setDoOutput(true);
                                        connection.setDoInput(true);
                                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                            BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream());

                                            FileOutputStream fileOutputStream = new FileOutputStream(apkFile);
                                            final int fileLength = connection.getContentLength();
                                            byte[] buffer = new byte[2048];
                                            int buffLen;
                                            int readLen = 0;

                                            while ((buffLen = inputStream.read(buffer)) > 0) {
                                                fileOutputStream.write(buffer, 0, buffLen);
                                                readLen += buffLen;
                                                final int finalReadLen = readLen;
                                                // Run on main thread
                                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        downloadDialog.setMax((int) (fileLength / Math.pow(1024, 1)));
                                                        downloadDialog.setProgress((int) (finalReadLen / Math.pow(1024, 1)));
                                                    }
                                                });

                                            }
                                            inputStream.close();
                                            fileOutputStream.close();
                                            // Run on main thread
                                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    downloadDialog.dismiss();
                                                }
                                            });
                                            FileUtils.smartOpenFile(context, apkFile, providerName);
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        if (mVersionVerCheckCallback != null) {
                                            mVersionVerCheckCallback.error(e.getMessage());
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

    /**
     * 取得預設取消按鈕監聽器
     */
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
    protected Boolean doInBackground(String... strings) {
        String verName = null;
        switch (checkType) {
            case GOOGLE_PLAY:
                verName = getVerNameFromGooglePlay(strings[0]);
                break;

            case SERVER:
                verName = getVerNameFromServer(strings[0]);
                break;
        }
        if (verName != null) {
            if (isStepCompare) {
                if (this.verName.equals(verName)) {
                    return true;
                }
                String[] systemVerNameArr = this.verName.split("\\.");
                String[] serverVerNameArr = verName.split("\\.");
                if (systemVerNameArr.length == serverVerNameArr.length) {
                    for (int i = 0; i < systemVerNameArr.length; i++) {
                        int systemInt = Integer.parseInt(systemVerNameArr[i]);
                        int serverInt = Integer.parseInt(serverVerNameArr[i]);
                        Log.i(TAG, "doInBackground: " + this.verName + ":" + verName);
                        Log.i(TAG, "doInBackground: " + systemInt + ":" + serverInt);
                        // 1.1.0 : 1.0.9
                        if (systemInt > serverInt) {
                            return true;
                        }
                    }
                    return false;
                } else {
                    return this.verName.equals(verName);
                }
            } else {
                return this.verName.equals(verName);
            }
        }
        return null;

    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        if (mVersionVerCheckCallback != null) {
            if (aBoolean != null) {
                if (aBoolean) {
                    mVersionVerCheckCallback.same(infoMap);
                } else {
                    if (mAlertDialog != null) {
                        mAlertDialog.setNegativeButton(updateBtnText, mUpdateOnClickListener != null ? mUpdateOnClickListener : getDefaultUpdateOnClickListener());
                        if (!isCompel) {
                            mAlertDialog.setNeutralButton(cancelBtnText, mCanceloOnClickListener != null ? mCanceloOnClickListener : getDefaultCancelOnClickListener());
                        }
                    }
                    mVersionVerCheckCallback.different(infoMap, mAlertDialog);
                }
            }
        }
    }

    /**
     * 檢查是否有可開啟的 Intent
     *
     * @param context Context
     * @param intent  Intent
     */
    private boolean haveIntent(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        List activities = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return activities.size() > 0;
    }
}
