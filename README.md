# MDVersionChecker
## 使用
**1. Gradle dependency** (recommended)

  -  Add the following to your project level `build.gradle`:
	 
``` gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```
  -  Add this to your app `build.gradle`:
	 
``` gradle
dependencies {
    compile 'com.github.army650663:MDVersionChecker:1.0.5'
}
```

**2. Proguard**
    - Add this to your app `proguard-rules.pro`
    
``` proguard
  -keep public class org.jsoup.** {
    public *;
  }
```

#### 範例
##### Google Play 版本檢查
 
 ``` java
    new MDVersionChecker()
        .checkGooglePlay("com.agenttw", "1.11.11")
        .setLoadingView(mContext, "Google play version check", "Version checking")
        .setUpdateDialog(mContext, "Need update", "Have new version!")
        .check(new MDVersionChecker.CheckVersionCallback() {
            @Override
            public void same(Map<String, String> infoMap) {
                Log.i(TAG, "same: " + infoMap.toString());
        
            @Override
            public void different(Map<String, String> infoMap, AlertDialog.Builder updateDialog) {
                Log.i(TAG, "different: " + infoMap.toString());
                updateDialog.show();
        
            @Override
            public void error(String error) {
                Log.i(TAG, "error: " + error);
            }
        });
    
 ``` 
 
##### Server 版本檢查
- 伺服器接收參數 pkgName。

###### 伺服器回傳格式

``` json
    {
      "result": true,
      "msg": "",
      "content": {
        "vername_android": "1.34.060",
        "apkurl": "http://google.com/AgentTW.apk"
      }
    }
```

###### 伺服器版本檢查

``` java
    // 伺服器版本檢查
    new MDVersionChecker()
        .checkServer("http://my.server/appupdate/", "com.appname", "1.33.050")
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
        // optional : 是否逐步比對版本名稱
        .isStepCompare(true)
        // 開始檢查
        .check(new VerCheckCallback() {
            @Override
            public void same(Map<String, String> infoMap) {
                Log.i(TAG, "same: " + infoMap.toString());
          
            @Override
            public void different(Map<String, String> infoMap, AlertDialog.Builder updateDialog) {
                Log.i(TAG, "different: " + infoMap.toString());
                updateDialog.setMessage("Your version is " + "1.33.050" + ", Server version is " + infoMap.get("verName"));
                updateDialog.show();
          
            @Override
            public void error(String error) {
                Log.i(TAG, "error: " + error);
            }
        });
```


