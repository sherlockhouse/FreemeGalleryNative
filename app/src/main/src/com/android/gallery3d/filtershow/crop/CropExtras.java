/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.filtershow.crop;

import android.net.Uri;

public class CropExtras {

    public static final String KEY_CROPPED_RECT = "cropped-rect";
    public static final String KEY_OUTPUT_X = "outputX";
    public static final String KEY_OUTPUT_Y = "outputY";
    public static final String KEY_SCALE = "scale";
    public static final String KEY_SCALE_UP_IF_NEEDED = "scaleUpIfNeeded";
    public static final String KEY_ASPECT_X = "aspectX";
    public static final String KEY_ASPECT_Y = "aspectY";
    public static final String KEY_SET_AS_WALLPAPER = "set-as-wallpaper";
    public static final String KEY_RETURN_DATA = "return-data";
    public static final String KEY_DATA = "data";
    public static final String KEY_SPOTLIGHT_X = "spotlightX";
    public static final String KEY_SPOTLIGHT_Y = "spotlightY";
    public static final String KEY_SHOW_WHEN_LOCKED = "showWhenLocked";
    public static final String KEY_OUTPUT_FORMAT = "outputFormat";

    private int mOutputX = 0;
    private int mOutputY = 0;
    private boolean mScaleUp = true;
    private int mAspectX = 0;
    private int mAspectY = 0;
    private boolean mSetAsWallpaper = false;
    private boolean mReturnData = false;
    private Uri mExtraOutput = null;
    private String mOutputFormat = null;
    private boolean mShowWhenLocked = false;
    private float mSpotlightX = 0;
    private float mSpotlightY = 0;

    public CropExtras(int outputX, int outputY, boolean scaleUp, int aspectX, int aspectY,
            boolean setAsWallpaper, boolean returnData, Uri extraOutput, String outputFormat,
            boolean showWhenLocked, float spotlightX, float spotlightY) {
        mOutputX = outputX;
        mOutputY = outputY;
        mScaleUp = scaleUp;
        mAspectX = aspectX;
        mAspectY = aspectY;
        mSetAsWallpaper = setAsWallpaper;
        mReturnData = returnData;
        mExtraOutput = extraOutput;
        mOutputFormat = outputFormat;
        mShowWhenLocked = showWhenLocked;
        mSpotlightX = spotlightX;
        mSpotlightY = spotlightY;
    }

    public CropExtras(CropExtras c) {
        /// M: [DEBUG.MODIFY] @{
        // Add support for return compress data
        /* this(c.mOutputX, c.mOutputY, c.mScaleUp, c.mAspectX, c.mAspectY, c.mSetAsWallpaper,
                c.mReturnData, c.mExtraOutput, c.mOutputFormat, c.mShowWhenLocked,
                c.mSpotlightX, c.mSpotlightY);*/
        this(c.mOutputX, c.mOutputY, c.mScaleUp, c.mAspectX, c.mAspectY, c.mSetAsWallpaper,
				//*/ tyd.biantao, 20140409. wallpaper.
                c.mSetAsLockWallpaper,
                //*/
                c.mReturnData, c.mExtraOutput, c.mOutputFormat, c.mShowWhenLocked,
                c.mSpotlightX, c.mSpotlightY, c.mReturnDataCompress);
        /// @}
    }

    public int getOutputX() {
        return mOutputX;
    }

    public int getOutputY() {
        return mOutputY;
    }

    public boolean getScaleUp() {
        return mScaleUp;
    }

    public int getAspectX() {
        return mAspectX;
    }

    public int getAspectY() {
        return mAspectY;
    }

    public boolean getSetAsWallpaper() {
        return mSetAsWallpaper;
    }

    public boolean getReturnData() {
        return mReturnData;
    }

    public Uri getExtraOutput() {
        return mExtraOutput;
    }

    public String getOutputFormat() {
        return mOutputFormat;
    }

    public boolean getShowWhenLocked() {
        return mShowWhenLocked;
    }

    public float getSpotlightX() {
        return mSpotlightX;
    }

    public float getSpotlightY() {
        return mSpotlightY;
    }

    //********************************************************************
    //*                              MTK                                 *
    //********************************************************************

    // In order to decrease the size of return data, add the way to return the compressed data
    // If mReturnDataCompress is true, the return data is byte array of bitmap compressed,
    // or else the return data is Bitmap.
    public static final String KEY_RETURN_DATA_COMPRESS = "return-data-compress";
    public static final String KEY_DATA_COMPRESS = "data-compress";

    private boolean mReturnDataCompress = false;

    public CropExtras(int outputX, int outputY, boolean scaleUp, int aspectX, int aspectY,
            boolean setAsWallpaper, boolean setAsLockWallpaper,boolean returnData, Uri extraOutput,
                      String outputFormat, boolean showWhenLocked, float spotlightX, float spotlightY,
            boolean returnDataCompress) {
        this(outputX, outputY, scaleUp, aspectX, aspectY,
            setAsWallpaper, returnData, extraOutput, outputFormat,
            showWhenLocked, spotlightX, spotlightY);
        mReturnDataCompress = returnDataCompress;
    }

    public boolean getReturnDataCompressed() {
        return mReturnDataCompress;
    }


//*****************freeme*******************************//
    //*/ tyd.biantao, 20140409. wallpaper.
    public static final String KEY_IS_INIT_FULL_SELECTION = "isInitFullSelection";
    public static final String KEY_SET_AS_LOCKSCREEN      = "set-as-lockscreen";
    public static final int    WALLPAPER_SCREEN_SPAN      = 2;
	private boolean mSetAsLockWallpaper = false;
    private boolean mIsInitFullSelection = false;
	
    public boolean getSetAsLockWallpaper() {
        return mSetAsLockWallpaper;
    }

    public boolean getInitFullSelection() {
        return mIsInitFullSelection;
    }

    public void applyWallpaperParameters(int outputX, int outputY, boolean scaleUp, boolean initFullSelection,
                                         int aspectX, int aspectY, float spotlightX, float spotlightY) {
        mOutputX = outputX;
        mOutputY = outputY;
        mScaleUp = scaleUp;
        mIsInitFullSelection = initFullSelection;
        mAspectX = aspectX;
        mAspectY = aspectY;
        mSpotlightX = spotlightX;
        mSpotlightY = spotlightY;
    }
    //*/
}
