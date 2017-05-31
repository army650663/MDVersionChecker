package tw.idv.madmanchen.mdversioncheckerlib.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * Author:      chenshaowei.
 * Version      V1.0
 * Date:        2016/11/10
 * Description:
 * Modification History:
 * Date         Author          version         Description
 * ---------------------------------------------------------------------
 * 2016/11/10      chenshaowei         V1.0            Create
 * What is modified:
 */

public class FileUtils {

    /**
     * 是否可以讀取外部儲存
     */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /**
     * 是否可以寫入外部儲存
     */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    /**
     * 儲存字串檔
     *
     * @param file   : 儲存檔案位置
     * @param string : 要寫入的字串
     */
    public static void writeStringToFile(File file, String string) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(string.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 讀取字串檔
     *
     * @param file        : 檔案位置
     * @param charSetName : "UTF-8"...
     * @return String
     */
    public static String readStringByFile(File file, @Nullable String charSetName) {
        String str = "";
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int buffLen;
            while ((buffLen = bis.read(buffer)) > 0) {
                bos.write(buffer, 0, buffLen);
            }
            if (charSetName != null) {
                str = bos.toString(charSetName);
            } else {
                str = bos.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * 智慧選擇檔案開啟方式
     *
     * @param context context
     * @param file    要開啟的檔案
     */
    public static void smartOpenFile(Context context, File file) {
        smartOpenFile(context, file, "fileProvider");
    }

    public static void smartOpenFile(Context context, File file, String providerName) {
        smartOpenFile(context, file, providerName, null);
    }

    public static void smartOpenFile(Context context, File file, String providerName, @Nullable String chooserTitle) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(context, context.getPackageName() + "." + providerName, file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(file);
        }
        intent.setDataAndType(uri, HttpURLConnection.guessContentTypeFromName(file.getName()));
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            if (chooserTitle != null) {
                Intent chooser = Intent.createChooser(intent, chooserTitle);
                context.startActivity(chooser);
            } else {
                context.startActivity(intent);
            }
        }
    }

    /**
     * 取得檔案的 MimeType
     *
     * @param file 檔案
     */
    public static String getMimeTypeFromFile(File file) {
        return HttpURLConnection.guessContentTypeFromName(file.getName());
    }

    /**
     * 取得輸入流 MimeType
     *
     * @param inputStream 輸入流
     */
    public static String getMimeTypeFromStream(InputStream inputStream) {
        try {
            return HttpURLConnection.guessContentTypeFromStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "*/*";
    }
}
