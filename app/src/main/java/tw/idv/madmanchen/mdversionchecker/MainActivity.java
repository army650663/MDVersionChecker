package tw.idv.madmanchen.mdversionchecker;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.Map;

import madmanchen.idv.tw.mdversioncheckerlib.MDVersionChecker;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    private Context mContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        // 伺服器版本檢查
        new MDVersionChecker()
                .checkServer("http://pub.mysoqi.com/appupdate/index1.php", "com.agenttw", "1.32.060")
                // optional : 設定讀取視窗
                .setLoadingView(this, "Check", "Version checking")
                // optional : 設定更新視窗
                .setUpdateDialog(this, "System info", "Need to update")
                // optional : 設定使用者取消動作
                .setCancelButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                })
                // 開始檢查
                .check(new MDVersionChecker.CheckVersionCallback() {
                    @Override
                    public void same(Map<String, String> infoMap) {
                        Log.i(TAG, "same: " + infoMap.toString());
                    }

                    @Override
                    public void different(Map<String, String> infoMap, AlertDialog.Builder updateDialog) {
                        Log.i(TAG, "different: " + infoMap.toString());
                        updateDialog.show();
                    }

                    @Override
                    public void error(String error) {
                        Log.i(TAG, "error: " + error);
                    }
                });

        // Google Play 版本檢查
        new MDVersionChecker()
                .checkGooglePlay("com.agenttw", "1.11.11")
                .setLoadingView(mContext, "Google play version check", "Version checking")
                .setUpdateDialog(mContext, "Need update", "Have new version!")
                .check(new MDVersionChecker.CheckVersionCallback() {
                    @Override
                    public void same(Map<String, String> infoMap) {
                        Log.i(TAG, "same: " + infoMap.toString());
                    }

                    @Override
                    public void different(Map<String, String> infoMap, AlertDialog.Builder updateDialog) {
                        Log.i(TAG, "different: " + infoMap.toString());
                        updateDialog.show();
                    }

                    @Override
                    public void error(String error) {
                        Log.i(TAG, "error: " + error);
                    }
                });
    }
}
