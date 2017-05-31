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
    compile 'com.github.army650663:MDVersionChecker:v1.0.2'
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
**伺服器回傳格式**
- 伺服器接收參數 pkgName。

``` json
    {
      "result": true,
      "msg": "",
      "content": {
        "pkgName": "com.agenttw",
        "verCode": "33",
        "verName": "1.33.060",
        "apkUrl": "http://pub.mysoqi.com/ht_agent/AgentTW.apk"
      }
    }
```

``` java
    HashMap<String, String> map = new HashMap<>();
    map.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, "This permission use update");
    map.put(Manifest.permission.READ_SMS, "This permission send sms");
    new MDPermission(this)
            .addPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_SMS)
            .addRationale(map)
            .setRationaleButtonText("好Der")
            .start(new MDPermission.PermissionCallbacks() {
                @Override
                public void success(List<String> perms) {
                    Log.i(TAG, "success: " + perms.toString());
              
                @Override
                public void fail(List<String> perms) {
                    Log.i(TAG, "fail: " + perms.toString());
                }
            });
```


