package com.wa2c.android.medoly.plugin.action.tweet;

import android.content.Context;
import android.widget.Toast;

public class AppUtils {

    public static void showToast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static void showToast(Context context, int stringId) {
        Toast.makeText(context, stringId, Toast.LENGTH_SHORT).show();
    }
}
