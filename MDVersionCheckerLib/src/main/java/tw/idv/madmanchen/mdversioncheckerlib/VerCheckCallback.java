package tw.idv.madmanchen.mdversioncheckerlib;

import android.support.v7.app.AlertDialog;

import java.util.Map;

/**
 * Author:      chenshaowei
 * Version      V1.0
 * Description:
 * Modification History:
 * Date         Author          version         Description
 * ---------------------------------------------------------------------
 * 2017/7/26      chenshaowei         V1.0.0          Create
 * Why & What is modified:
 */

public interface VerCheckCallback {
    void same(Map<String, String> infoMap);

    void different(Map<String, String> infoMap, AlertDialog.Builder updateDialog);

    void error(String error);
}
