package com.freeme.utils;

import android.view.Menu;
import android.view.MenuItem;

import com.freeme.gallery.R;

/*
对于一些需要framework支持的特性,把相关配置和说明放在这里
 */

public class FrameworkSupportUtils {
    private static final String TAG = "FrameworkSupportUtils";
    public static final float SCALE_LIMIT = 1;
    public static final float SCALE_MAX_EXTRA = 1.5f;
    private static String support_refocus_prop = "ro.freeme.sprd.refocus";
    private static boolean canRefocus = false;

    private static final String support_voice_prop = "ro.freeme.voiceimage";
    private static boolean supportVoice = false;

    private static final String   scale_limit    =    "ro.freeme.scale_limit";
    private static final String   scale_max_extra =   "ro.freeme.scale_max_extra";

    private static final String support_cloud_prop = "ro.freeme.ctcc.cloudalbum";
    private static boolean supportCloud = false;

    private static final String screen_brightness_pro = "ro.freeme.screenbrightness";

    private static final SettingProperties mSettings;
    static {
        mSettings = SettingProperties.getInstance();
    }

    public static boolean isSupportRefocusImage() {
        if (mSettings.getBoolean(support_refocus_prop)) {
            canRefocus = true;
        }
        return canRefocus;
    }

    public static boolean isSupportVoiceImage() {
        if (mSettings.getBoolean(support_voice_prop)) {
            supportVoice = true;
        }
        return supportVoice;
    }

    public static boolean isSupportCloud() {
        if (mSettings.getBoolean(support_cloud_prop)) {
            supportCloud = true;
        }
        return supportCloud;
    }

    public static void  setSupportCloud(boolean support) {
        supportCloud = support;
    }

    public static void setAiMenu(Menu menu) {
        if (FrameworkSupportUtils.isSupportCloud()) {
            if (menu != null) {
                MenuItem aiItem = menu.findItem(R.id.action_aialbum);
                if (aiItem != null) {
                    aiItem.setVisible(true);
                }
            }
        }
    }

    public static float getScreenBritness() {
        return mSettings.getFloat(screen_brightness_pro);
    }

    public static float getSCaleLimit() {
        float result = mSettings.getFloat(scale_limit);
        if (result == 0) {
            return SCALE_LIMIT;
        } else {
            return result;
        }
    }

    public static float getScaleMaxExtra() {
        float result = mSettings.getFloat(scale_max_extra);
        if (result == 0) {
            return SCALE_MAX_EXTRA;
        } else {
            return result;
        }
    }
}
