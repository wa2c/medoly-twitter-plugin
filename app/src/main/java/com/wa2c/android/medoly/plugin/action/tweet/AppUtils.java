package com.wa2c.android.medoly.plugin.action.tweet;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by wa2c on 2015/05/06.
 */
public class AppUtils {

    public static void showToast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static void showToast(Context context, int stringId) {
        Toast.makeText(context, stringId, Toast.LENGTH_SHORT).show();
    }
}
