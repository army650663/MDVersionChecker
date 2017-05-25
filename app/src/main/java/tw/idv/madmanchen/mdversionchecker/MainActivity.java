package tw.idv.madmanchen.mdversionchecker;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.Map;

import madmanchen.idv.tw.mdversioncheckerlib.MDVersionChecker;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new MDVersionChecker()
                .setLoadingView(this, "Check", "Version checking")
                .setUpdateDialog(this, "System info", "Need to update")
                .checkServer("http://192.168.42.241/appupdate/", "com.agenttw", "1.29.060")
                .setCancelButton("Noppp", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                })
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

                    }
                });
    }
}
