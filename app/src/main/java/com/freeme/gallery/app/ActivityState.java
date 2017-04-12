/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.freeme.gallery.app;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.freeme.gallery.R;
import com.freeme.gallery.anim.StateTransitionAnimation;
import com.freeme.gallery.glrenderer.RawTexture;
import com.freeme.gallery.ui.GLView;
import com.freeme.gallery.ui.PreparePageFadeoutTexture;
import com.freeme.gallery.util.GalleryUtils;
import com.freeme.page.AlbumStoryPage;

abstract public class ActivityState {
    protected static final int FLAG_HIDE_ACTION_BAR            = 1;
    protected static final int FLAG_HIDE_STATUS_BAR            = 2;
    protected static final int FLAG_SCREEN_ON_WHEN_PLUGGED     = 4;
    protected static final int FLAG_SCREEN_ON_ALWAYS           = 8;
    protected static final int FLAG_ALLOW_LOCK_WHILE_SCREEN_ON = 16;
    protected static final int FLAG_SHOW_WHEN_LOCKED           = 32;
    private static final String KEY_TRANSITION_IN = "transition-in";
    protected AbstractGalleryActivity mActivity;
    protected Bundle                  mData;
    protected int                     mFlags;
    protected ResultEntry mReceivedResults;
    protected ResultEntry mResult;
    protected float[] mBackgroundColor;
    boolean mIsFinishing = false;
    private boolean mDestroyed = false;
    private boolean mPlugged   = false;
    BroadcastReceiver mPowerIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                boolean plugged = (0 != intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0));

                if (plugged != mPlugged) {
                    mPlugged = plugged;
                    setScreenFlags();
                }
            }
        }
    };
    private StateTransitionAnimation.Transition mNextTransition =
            StateTransitionAnimation.Transition.None;
    private StateTransitionAnimation mIntroAnimation;
    private GLView                   mContentPane;

    protected ActivityState() {
    }

    protected void setContentPane(GLView content) {
        mContentPane = content;
        if (mIntroAnimation != null) {
            mContentPane.setIntroAnimation(mIntroAnimation);
            mIntroAnimation = null;
        }
        mContentPane.setBackgroundColor(getBackgroundColor());
        mActivity.getGLRoot().setContentPane(mContentPane);
    }

    protected float[] getBackgroundColor() {
        return mBackgroundColor;
    }

    void initialize(AbstractGalleryActivity activity, Bundle data) {
        mActivity = activity;
        mData = data;
    }

    public Bundle getData() {
        return mData;
    }

    protected void onBackPressed() {
        mActivity.getStateManager().finishState(this);
    }

    protected void setStateResult(int resultCode, Intent data) {
        if (mResult == null) return;
        mResult.resultCode = resultCode;
        mResult.resultData = data;
    }

    protected void onConfigurationChanged(Configuration config) {
    }

    protected void onSaveState(Bundle outState) {
    }

    protected void onCreate(Bundle data, Bundle storedState) {
//        mBackgroundColor = GalleryUtils.intColorToFloatARGBArray(
//                mActivity.getResources().getColor(getBackgroundColorId()));

        if (getBackgroundColorId() == -1) {
            mBackgroundColor = getBackgroundColor();
        } else {
            mBackgroundColor = GalleryUtils.intColorToFloatARGBArray(
                    mActivity.getResources().getColor(getBackgroundColorId()));
        }
    }

    protected int getBackgroundColorId() {
        return R.color.default_background;
    }

    protected void clearStateResult() {
    }

    protected void transitionOnNextPause(Class<? extends ActivityState> outgoing,
                                         Class<? extends ActivityState> incoming,
                                         StateTransitionAnimation.Transition hint) {
        if (outgoing == SinglePhotoPage.class && incoming == AlbumPage.class) {
            mNextTransition = StateTransitionAnimation.Transition.Outgoing;
        } else if (outgoing == AlbumPage.class && incoming == SinglePhotoPage.class) {
            mNextTransition = StateTransitionAnimation.Transition.PhotoIncoming;
        } else {
            mNextTransition = hint;
        }
    }

    protected void performHapticFeedback(int feedbackConstant) {
        mActivity.getWindow().getDecorView().performHapticFeedback(feedbackConstant,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
    }

    protected void onPause() {
        if (0 != (mFlags & FLAG_SCREEN_ON_WHEN_PLUGGED)) {
            mActivity.unregisterReceiver(mPowerIntentReceiver);
        }
        if (mNextTransition != StateTransitionAnimation.Transition.None) {
            mActivity.getTransitionStore().put(KEY_TRANSITION_IN, mNextTransition);
            mActivity.getGLRoot().lockRenderThread();
            try {
                PreparePageFadeoutTexture.prepareFadeOutTexture(mActivity, mContentPane);
            } finally {
                mActivity.getGLRoot().unlockRenderThread();
            }
            mNextTransition = StateTransitionAnimation.Transition.None;
        }
    }

    // should only be called by StateManager
    void resume() {
        AbstractGalleryActivity activity = mActivity;
        ActionBar actionBar = activity.getActionBar();
        if (actionBar != null) {
            if ((mFlags & FLAG_HIDE_ACTION_BAR) != 0) {
                actionBar.hide();
            } else {
                actionBar.show();
            }
            int stateCount = mActivity.getStateManager().getStateCount();
            mActivity.getGalleryActionBar().setDisplayOptions(stateCount > 1, true);
        }

        /*/ Modified by Linguanrong for story album, 2015-7-2
        activity.invalidateOptionsMenu();
        /*/
        if (!mActivity.mIsSelectionMode) {
            activity.invalidateOptionsMenu();
        }
        //*/

        setScreenFlags();

        boolean lightsOut = ((mFlags & FLAG_HIDE_STATUS_BAR) != 0);
        mActivity.getGLRoot().setLightsOutMode(lightsOut);

        ResultEntry entry = mReceivedResults;
        if (entry != null) {
            mReceivedResults = null;
            onStateResult(entry.requestCode, entry.resultCode, entry.resultData);
        }

        if (0 != (mFlags & FLAG_SCREEN_ON_WHEN_PLUGGED)) {
            // we need to know whether the device is plugged in to do this correctly
            final IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            activity.registerReceiver(mPowerIntentReceiver, filter);
        }

        onResume();

        // the transition store should be cleared after resume;
        mActivity.getTransitionStore().clear();
    }

    private void setScreenFlags() {
        final Window win = mActivity.getWindow();
        final WindowManager.LayoutParams params = win.getAttributes();
        if ((0 != (mFlags & FLAG_SCREEN_ON_ALWAYS)) ||
                (mPlugged && 0 != (mFlags & FLAG_SCREEN_ON_WHEN_PLUGGED))) {
            params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        } else {
            params.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        }
        if (0 != (mFlags & FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)) {
            params.flags |= WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON;
        } else {
            params.flags &= ~WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON;
        }
        if (0 != (mFlags & FLAG_SHOW_WHEN_LOCKED)) {
            params.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        } else {
            params.flags &= ~WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        }

        //*/ Added by Linguanrong for story album, 2015-08-05
        params.flags &= ~WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        //params.flags &= ~WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
        //*/

        //*/ Added by Tyd Linguanrong for PhotoPage window laout params, 2014-6-11
        try {
            ActivityState topState = mActivity.getStateManager().getTopState();
            //*/ Added by Linguanrong for story album, 2015-08-05
            if (topState instanceof AlbumStoryPage) {
                params.flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
            }
//            else if (topState instanceof PhotoPage) {
//                params.flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
//            }
            //*/
        } catch (AssertionError e) {
            e.printStackTrace();
        }
        //*/

        win.setAttributes(params);
    }

    protected void onStateResult(int requestCode, int resultCode, Intent data) {
    }

    // a subclass of ActivityState should override the method to resume itself
    protected void onResume() {
        RawTexture fade = mActivity.getTransitionStore().get(
                PreparePageFadeoutTexture.KEY_FADE_TEXTURE);
        mNextTransition = mActivity.getTransitionStore().get(
                KEY_TRANSITION_IN, StateTransitionAnimation.Transition.None);
        if (mNextTransition != StateTransitionAnimation.Transition.None) {
            mIntroAnimation = new StateTransitionAnimation(mNextTransition, fade);
            mNextTransition = StateTransitionAnimation.Transition.None;
        }
    }

    protected boolean onCreateActionBar(Menu menu) {
        // TODO: we should return false if there is no menu to show
        //       this is a workaround for a bug in system
        return true;
    }

    protected boolean onItemSelected(MenuItem item) {
        return false;
    }

    protected void onDestroy() {
        mDestroyed = true;
    }

    boolean isDestroyed() {
        return mDestroyed;
    }

    public boolean isFinishing() {
        return mIsFinishing;
    }

    protected MenuInflater getSupportMenuInflater() {
        return mActivity.getMenuInflater();
    }

    //*/ Added by droi Linguanrong for freeme gallery, 16-1-14
    public void startIntroAnimation() {
        RawTexture fade = mActivity.getTransitionStore().get(
                PreparePageFadeoutTexture.KEY_FADE_TEXTURE);
        mNextTransition = mActivity.getTransitionStore().get(
                KEY_TRANSITION_IN, StateTransitionAnimation.Transition.Incoming);
        if (mNextTransition != StateTransitionAnimation.Transition.None) {
            mIntroAnimation = new StateTransitionAnimation(mNextTransition, fade);
            mNextTransition = StateTransitionAnimation.Transition.None;
        }
    }

    protected static class ResultEntry {
        public int requestCode;
        public int resultCode = Activity.RESULT_CANCELED;
        public Intent resultData;
    }

    public void setBackgroundColor(float [] color) {
        mBackgroundColor = color;
        mContentPane.setBackgroundColor(mBackgroundColor);
    }
    //*/
}
