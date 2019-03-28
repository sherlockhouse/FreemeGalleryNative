/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
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

package com.android.gallery3d.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.ClusterAlbumSet;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.FadeTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.ui.ActionModeHandler;
import com.android.gallery3d.ui.ActionModeHandler.ActionModeListener;
import com.android.gallery3d.ui.AlbumSetSlotRenderer;
import com.android.gallery3d.ui.DetailsHelper;
import com.android.gallery3d.ui.DetailsHelper.CloseListener;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SlotView;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.HelpUtils;
import com.freeme.data.StoryAlbumSet;
import com.freeme.extern.HideModeHandler;
import com.freeme.extern.IBucketAlbum;
import com.freeme.gallery.app.AlbumPicker;
import com.freeme.gallery.app.GalleryActivity;
import com.freeme.jigsaw.app.JigsawEntry;
import com.freeme.page.AlbumCameraPage;
import com.freeme.page.AlbumStorySetPage;
import com.freeme.page.AlbumTimeShaftPage;
import com.freeme.settings.GallerySettings;
import com.freeme.utils.FrameworkSupportUtils;
import com.freeme.utils.FreemeUtils;
import com.mediatek.gallery3d.util.PermissionHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

//import com.android.gallery3d.settings.GallerySettings;



public class AlbumSetPage extends ActivityState implements
        SelectionManager.SelectionListener, GalleryActionBar.ClusterRunner,
        EyePosition.EyePositionListener, MediaSet.SyncListener {
    public static final String KEY_MEDIA_PATH = "media-path";
    public static final String KEY_SET_TITLE = "set-title";
    public static final String KEY_SET_SUBTITLE = "set-subtitle";
    public static final String KEY_SELECTED_CLUSTER_TYPE = "selected-cluster";
    //*/ Added by Tyd Linguanrong for gallery secret photos, 2014-4-3
    public static final String KEY_VISITOR_MODE = "visitor-mode";
    public static final String KEY_VISITOR_TYPE = "visitor-type";
    //*/ Added by Linguanrong for story album, 2015-4-9
    public static final String KEY_STORY_SELECT_MODE = "story-select-mode";
    public static final String KEY_STORY_SELECT_INDEX = "story-select-index";
    //*/
    public static final String KEY_STORY_FROM_CHILD = "story_from_child";
    public static final String KEY_VISIBLE_ALL_SET = "visible_albumeset";
    @SuppressWarnings("unused")
    private static final String TAG = "AlbumSetPage";
    //*/
    private static final int MSG_PICK_ALBUM = 1;
    private static final int DATA_CACHE_SIZE = 256;
    private static final int REQUEST_DO_ANIMATION = 1;
    private static final int BIT_LOADING_RELOAD = 1;
    private static final int BIT_LOADING_SYNC = 2;
    protected SelectionManager mSelectionManager;
    WeakReference<Toast> mEmptyAlbumToast = null;
    private boolean mIsActive = false;
    private SlotView mSlotView;
    private AlbumSetSlotRenderer mAlbumSetView;
    private Config.AlbumSetPage mConfig;
    private MediaSet mMediaSet;
    private String mTitle;
    private String mSubtitle;
    private GalleryActionBar mActionBar;
    private int mSelectedAction;
    private AlbumSetDataLoader mAlbumSetDataAdapter;
    private boolean mGetContent;
    private boolean mGetAlbum;
    private ActionModeHandler mActionModeHandler;
    private DetailsHelper mDetailsHelper;
    private MyDetailsSource mDetailsSource;
    private boolean mShowDetails;
    private EyePosition mEyePosition;
    private Handler mHandler;
    // The eyes' position of the user, the origin is at the center of the
    // device and the unit is in pixels.
    private float mX;
    private float mY;
    private float mZ;
    private Future<Integer> mSyncTask = null;
    private int mLoadingBits = 0;
    private boolean mInitialSynced = false;
    private boolean mShowedEmptyToastForSelf = false;
    /// M: [BUG.ADD]  if get the mTitle/mSubTitle,they will not change when switch language@{
    private int mClusterType = -1;
    /// @}
    //*/Added by droi Linguanrong for Gallery new style, 2013-12-19
    private int mSlotViewPadding = 0;
    private int mBottomPadding = 0;
    private boolean mResumeSelection = false;
    //*/
    private boolean resumeFromCommunity = false;
    private View mEmptyView;
    //*/
    //*/ Added by Tyd Linguanrong for secret photos, 2014-2-17
    private boolean mVisitorMode;
    private boolean mIsSecretImages;
    //*/ Added by Linguanrong for story album, 2015-4-9
    private boolean mStorySelectMode;
    //*/
    private boolean mStoryFromChild;
    private int mStoryBucketId = -1;
    //*/ Added by xueweili for hide album, 2015-7-23
    private boolean mIsHideAlbumSet = false;
    //*/
    private HideModeHandler mHideModeHandler = null;
    // Added by TYD Theobald_Wu on 2014/01 [end]
    // Added by TYD Theobald_Wu on 2014/01 [begin] for jigsaw feature
    private boolean mJigsawPicker;
    //*/
    private final GLView mRootPane = new GLView() {
        private final float mMatrix[] = new float[16];

        @Override
        protected void render(GLCanvas canvas) {
            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
            GalleryUtils.setViewPointMatrix(mMatrix,
                    getWidth() / 2 + mX, getHeight() / 2 + mY, mZ);
            canvas.multiplyMatrix(mMatrix, 0);
            super.render(canvas);
            canvas.restore();
        }

        @Override
        protected void onLayout(
                boolean changed, int left, int top, int right, int bottom) {
            mEyePosition.resetPosition();

            //*/ Modified by droi Linguanrong for freeme gallery, 16-1-13
            //int slotViewTop = mActionBar.getHeight() + mConfig.paddingTop;
            int slotViewTop = mActionBar.getHeight() + mConfig.paddingTop + mActivity.mStatusBarHeight;
            //*/
            int slotViewBottom = bottom - top - mConfig.paddingBottom;
            /*/ Modified by droi Linguanrong for story album, 2015-6-19
            int slotViewRight = right - left;
            /*/
            int slotViewLeft = left + mConfig.paddingLeftRight;
            int slotViewRight = right - left - mConfig.paddingLeftRight;
            //*/

            // Added by TYD Theobald_Wu on 2014/01 [begin] for jigsaw feature
            if (mJigsawPicker && mActivity instanceof JigsawEntry) {
                JigsawEntry jigsaw = (JigsawEntry) mActivity;
                slotViewTop = mActionBar.getHeight();
                slotViewBottom = slotViewBottom - jigsaw.getBottomCtrlHeight();
            }
            // Added by TYD Theobald_Wu on 2014/01 [end]

            if (mShowDetails) {
                mDetailsHelper.layout(left, slotViewTop, right, bottom);
            } else {
                mAlbumSetView.setHighlightItemPath(null);
            }

            //*/Modified by droi Linguanrong for Gallery new style, 2013-12-19
            if ((mJigsawPicker && mActivity instanceof JigsawEntry) || mGetContent || mGetAlbum) {
                mSlotView.layout(slotViewLeft, slotViewTop, slotViewRight,
                        slotViewBottom - mSlotViewPadding);
            } else {
                mSlotView.layout(slotViewLeft, slotViewTop, slotViewRight,
                        slotViewBottom - mBottomPadding - mSlotViewPadding);
            }
            //*/
        }
    };

    @Override
    protected int getBackgroundColorId() {
        return R.color.albumset_background;
    }

    //*/ Added by droi Linguanrong for lock orientation, 16-3-1
    private OrientationManager mOrientationManager;

    @Override
    public void onEyePositionChanged(float x, float y, float z) {
        mRootPane.lockRendering();
        mX = x;
        mY = y;
        mZ = z;
        mRootPane.unlockRendering();
        mRootPane.invalidate();
    }

    @Override
    public void onBackPressed() {
        if (mShowDetails) {
            hideDetails();
            //*/ Added by xueweili for press back when in hide state, 2015-7-23
        } else if (mIsHideAlbumSet) {
            hideAlbumStateFinish();
            return;
            //*/
        } else if (mSelectionManager.inSelectionMode()) {
            mSelectionManager.leaveSelectionMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onSelectionModeChange(int mode) {
        switch (mode) {
            case SelectionManager.ENTER_SELECTION_MODE: {
                //*/ Added by xueweili for start hide mode, 2015-7-23
                if (!mIsHideAlbumSet) {
                    mActionModeHandler.startActionMode();
                }
                //*/
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                break;
            }
            case SelectionManager.LEAVE_SELECTION_MODE: {
                mActionModeHandler.finishActionMode();
                mRootPane.invalidate();
                break;
            }

            case SelectionManager.DESELECT_ALL_MODE:
            case SelectionManager.SELECT_ALL_MODE: {
                mActionModeHandler.updateSupportedOperation();
                //*/ Added by xueweili for select all album in hide state, 2015-7-23
                if (mIsHideAlbumSet) {
                    mHideModeHandler.updateSelectionMenu();
                }
                //*/
                mRootPane.invalidate();
                break;
            }
        }
    }

    private void getSlotCenter(int slotIndex, int center[]) {
        Rect offset = new Rect();
        mRootPane.getBoundsOf(mSlotView, offset);
        Rect r = mSlotView.getSlotRect(slotIndex);
        int scrollX = mSlotView.getScrollX();
        int scrollY = mSlotView.getScrollY();
        center[0] = offset.left + (r.left + r.right) / 2 - scrollX;
        center[1] = offset.top + (r.top + r.bottom) / 2 - scrollY;
    }

    @Override
    public void onSelectionChange(Path path, boolean selected) {
        mActionModeHandler.setTitle(getSelectedString());

        mActionModeHandler.updateSupportedOperation(path, selected);
        //*/ Added by xueweili for change title when select hide , 2015-7-23
        if (mIsHideAlbumSet) {
            mHideModeHandler.updateSelectionMenu();
            mHideModeHandler.setTitle(getSelectedString());
        }
        //*/
    }


    public void onSingleTapUp(int slotIndex) {
        if (!mIsActive) return;

        if (mSelectionManager.inSelectionMode()) {
            MediaSet targetSet = mAlbumSetDataAdapter.getMediaSet(slotIndex);
            if (targetSet == null) return; // Content is dirty, we shall reload soon
            mSelectionManager.toggle(targetSet.getPath());
            mSlotView.invalidate();
        } else {
            // Show pressed-up animation for the single-tap.
            mAlbumSetView.setPressedIndex(slotIndex);
            mAlbumSetView.setPressedUp();
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PICK_ALBUM, slotIndex, 0),
                    FadeTexture.DURATION);
        }
    }

    public String getSelectedString() {
        int count = mSelectionManager.getSelectedCount();
        int action = mActionBar.getClusterTypeAction();
        int string = action == FreemeUtils.CLUSTER_BY_ALBUM
                ? R.plurals.number_of_albums_selected
                : R.plurals.number_of_groups_selected;
        String format = mActivity.getResources().getQuantityString(string, count);
        return String.format(format, count);
    }

    private static boolean albumShouldOpenInFilmstrip(MediaSet album) {
        int itemCount = album.getMediaItemCount();
        ArrayList<MediaItem> list = (itemCount == 1) ? album.getMediaItem(0, 1) : null;
        // open in film strip only if there's one item in the album and the item exists
        return (list != null && !list.isEmpty());
    }

    @Override
    public void onSyncDone(final MediaSet mediaSet, final int resultCode) {
        if (resultCode == MediaSet.SYNC_RESULT_ERROR) {
            Log.d(TAG, "onSyncDone: " + Utils.maskDebugInfo(mediaSet.getName()) + " result="
                    + resultCode);
        }
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GLRoot root = mActivity.getGLRoot();
                root.lockRenderThread();
                try {
                    if (resultCode == MediaSet.SYNC_RESULT_SUCCESS) {
                        mInitialSynced = true;
                    }
                    clearLoadingBit(BIT_LOADING_SYNC);
                    if (resultCode == MediaSet.SYNC_RESULT_ERROR && mIsActive) {
                        Log.w(TAG, "failed to load album set");
                    }
                } finally {
                    root.unlockRenderThread();
                }
            }
        });
    }



    private void showEmptyAlbumToast(int toastLength) {
        Toast toast;
        if (mEmptyAlbumToast != null) {
            toast = mEmptyAlbumToast.get();
            if (toast != null) {
                toast.show();
                return;
            }
        }

        //*/ Modified by Tyd Linguanrong for secret photos, 2014-5-29
        int str_id = R.string.empty_album;
        if (mVisitorMode || mStorySelectMode) {
            if (mIsSecretImages) {
                str_id = R.string.empty_album_image;
            } else {
                str_id = R.string.empty_album_video;
            }
        }
        toast = Toast.makeText(mActivity, str_id, toastLength);
        //*/
        mEmptyAlbumToast = new WeakReference<>(toast);
        toast.show();
    }

    private class MyDetailsSource implements DetailsHelper.DetailsSource {
        private int mIndex;

        @Override
        public int size() {
            return mAlbumSetDataAdapter.size();
        }

        @Override
        public int setIndex() {
            Path id = mSelectionManager.getSelected(false).get(0);
            mIndex = mAlbumSetDataAdapter.findSet(id);
            return mIndex;
        }

        @Override
        public MediaDetails getDetails() {
            MediaObject item = mAlbumSetDataAdapter.getMediaSet(mIndex);
            if (item != null) {
                mAlbumSetView.setHighlightItemPath(item.getPath());
                return item.getDetails();
            } else {
                return null;
            }
        }
    }

    private void hideEmptyAlbumToast() {
        if (mEmptyAlbumToast != null) {
            Toast toast = mEmptyAlbumToast.get();
            if (toast != null) toast.cancel();
        }
    }

    private void pickAlbum(int slotIndex) {
        if (!mIsActive) return;

        if (isSlotIndexZero(slotIndex)) {
            ((GalleryActivity)mActivity).gotoAIActivity(GalleryActivity.CLOUD_ALBUM, null);
            return;
        }

        MediaSet targetSet = mAlbumSetDataAdapter.getMediaSet(slotIndex);
        if (targetSet == null) return; // Content is dirty, we shall reload soon
        if (targetSet.getTotalMediaItemCount() == 0) {
            showEmptyAlbumToast(Toast.LENGTH_SHORT);
            return;
        }
        hideEmptyAlbumToast();

        //*/ Added by droi Linguanrong for freeme gallery, 16-1-13
        if (mActivity instanceof GalleryActivity) {
            ((GalleryActivity) mActivity).setBottomTabVisibility(false);
        }
        //*/

        String mediaPath = targetSet.getPath().toString();

        Bundle data = new Bundle(getData());
        //*/ Added by xueweili for set hide flag, 2015-7-23
        data.putBoolean(KEY_VISIBLE_ALL_SET, false);
        //*/
        int[] center = new int[2];
        getSlotCenter(slotIndex, center);
        data.putIntArray(AlbumPage.KEY_SET_CENTER, center);
        if (mGetAlbum && targetSet.isLeafAlbum()) {
            Activity activity = mActivity;
            Intent result = new Intent()
                    .putExtra(AlbumPicker.KEY_ALBUM_PATH, targetSet.getPath().toString());
            activity.setResult(Activity.RESULT_OK, result);
            activity.finish();
        } else if (targetSet.getSubMediaSetCount() > 0) {
            data.putString(AlbumSetPage.KEY_MEDIA_PATH, mediaPath);
            mActivity.getStateManager().startStateForResult(
                    AlbumSetPage.class, REQUEST_DO_ANIMATION, data);
        } else {
            if (!mGetContent && albumShouldOpenInFilmstrip(targetSet)) {
                data.putParcelable(PhotoPage.KEY_OPEN_ANIMATION_RECT,
                        mSlotView.getSlotRect(slotIndex, mRootPane));
                data.putInt(PhotoPage.KEY_INDEX_HINT, 0);
                data.putString(PhotoPage.KEY_MEDIA_SET_PATH,
                        mediaPath);
                data.putBoolean(PhotoPage.KEY_START_IN_FILMSTRIP, true);
                data.putBoolean(PhotoPage.KEY_IN_CAMERA_ROLL, targetSet.isCameraRoll());
                mActivity.getStateManager().startStateForResult(
                        FilmstripPage.class, AlbumPage.REQUEST_PHOTO, data);
                return;
            }
            data.putString(AlbumPage.KEY_MEDIA_PATH, mediaPath);

            //*/ Modified by Tyd Linguanrong for secret photos, 2014-5-29
            data.putBoolean(AlbumPage.KEY_VISITOR_TYPE, mIsSecretImages);
            data.putBoolean(AlbumPage.KEY_STORY_SELECT_MODE, mStorySelectMode);
            data.putBoolean(AlbumPage.KEY_STORY_FROM_CHILD, mStoryFromChild);
            data.putInt(AlbumPage.KEY_STORY_SELECT_INDEX, mStoryBucketId);

            if (mVisitorMode || mStorySelectMode) {
                mActivity.getStateManager().switchState(this, AlbumPage.class, data);
            } else {
                mActivity.getStateManager().startStateForResult(
                        AlbumPage.class, REQUEST_DO_ANIMATION, data);
            }
            //*/
        }
    }

    private void onDown(int index) {
        if (isSlotIndexZero(index)) return;
        mAlbumSetView.setPressedIndex(index);
    }

    private void onUp(boolean followedByLongPress) {
        if (followedByLongPress) {
            // Avoid showing press-up animations for long-press.
            mAlbumSetView.setPressedIndex(-1);
        } else {
            mAlbumSetView.setPressedUp();
        }
    }

    public void onLongTap(int slotIndex) {
        if (mGetContent || mGetAlbum || mIsHideAlbumSet) return;
        MediaSet set = mAlbumSetDataAdapter.getMediaSet(slotIndex);
        if (set == null) return;
        if (isSlotIndexZero(slotIndex)) return;
        mSelectionManager.setAutoLeaveSelectionMode(true);
        mSelectionManager.toggle(set.getPath());
        mSlotView.invalidate();
    }

    private boolean isSlotIndexZero(int slotIndex) {
        if (!FrameworkSupportUtils.isSupportCloud()) return false;
        if (0 == slotIndex) return true;
        return false;
    }

    @Override
    public void doCluster(int clusterType) {
        /// M: [FEATURE.ADD] [Runtime permission] @{
        if (clusterType == FilterUtils.CLUSTER_BY_LOCATION
                && !PermissionHelper.checkAndRequestForLocationCluster(mActivity)) {
            Log.i(TAG, "<doCluster> permission not granted");
            mNeedDoClusterType = clusterType;
            return;
        }
        /// @}
        //*/Modified by droi Linguanrong for Gallery new style, 2013-12-16
        if (mSelectionManager != null && mSelectionManager.inSelectionMode()) {
            mSelectionManager.leaveSelectionMode();
            //*/ Added by xueweili for set actionbar view, 2015-7-23
            if (mIsHideAlbumSet) {
                mActionBar.setCustomView(null);
            }
            //*/
        }

        String newPath;
        Bundle data = new Bundle(getData());
        //*/ Added by xueweili for set visible flag  , 2015-7-23
        data.putBoolean(KEY_VISIBLE_ALL_SET, false);
        //*/
        if (clusterType == FreemeUtils.CLUSTER_BY_CAMERE) {
            newPath = mActivity.getDataManager().makeCameraSetPath();
            //*/ Added by droi Linguanrong for Gallery new style, 2013-12-30
            boolean mTimeShaftPage = mActivity.mSharedPreferences.getBoolean("default_page", true);
            if (mTimeShaftPage) {
                data.putString(AlbumTimeShaftPage.KEY_MEDIA_PATH, newPath);
                mActivity.getStateManager().switchState(this, AlbumTimeShaftPage.class, data);
            } else {
                data.putString(AlbumCameraPage.KEY_MEDIA_PATH, newPath);
                mActivity.getStateManager().switchState(this, AlbumCameraPage.class, data);
            }
            //*/
        } else if (clusterType == FreemeUtils.CLUSTER_BY_STORY) {
            data.putBoolean(GalleryActivity.KEY_GET_CONTENT, false);
            data.putString(AlbumStorySetPage.KEY_MEDIA_PATH, StoryAlbumSet.PATH.toString());
            data.putInt(AlbumStorySetPage.KEY_SELECTED_CLUSTER_TYPE, clusterType);
            mActivity.getStateManager().switchState(this, AlbumStorySetPage.class, data);
        } else if (clusterType == FreemeUtils.CLUSTER_BY_COMMUNITY) {
            ((GalleryActivity) mActivity).startCommunity();
        }
    }

    @Override
    public void onCreate(Bundle data, Bundle restoreState) {
        super.onCreate(data, restoreState);
        initializeViews();
        initializeData(data);
        mGetContent = data.getBoolean(GalleryActivity.KEY_GET_CONTENT, false);
        mGetAlbum = data.getBoolean(GalleryActivity.KEY_GET_ALBUM, false);
        mTitle = data.getString(AlbumSetPage.KEY_SET_TITLE);
        /// M: [BUG.MODIFY] @{
        // Get clustertype here, if get the mTitle/mSubTitle,
        // they will not change when switch language.
        //mSubtitle = data.getString(AlbumSetPage.KEY_SET_SUBTITLE);
        mClusterType = data.getInt(AlbumSetPage.KEY_SELECTED_CLUSTER_TYPE);
        /// @}
        mEyePosition = new EyePosition(mActivity.getAndroidContext(), this);
        mDetailsSource = new MyDetailsSource();
        if (mGetContent || mGetAlbum) {
            mActionBar = mActivity.getGalleryActionBarWithoutTap();
        } else {
            mActionBar = mActivity.getGalleryActionBar();
        }
        mSelectedAction = data.getInt(AlbumSetPage.KEY_SELECTED_CLUSTER_TYPE, FreemeUtils.CLUSTER_BY_ALBUM);
        //*/ Added by droi Linguanrong for freeme gallery, 16-1-14
        resumeFromCommunity = data.getBoolean(FreemeUtils.KEY_FROM_COMMUNITY);
        //*/

        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_PICK_ALBUM: {
                        pickAlbum(message.arg1);
                        break;
                    }
                    default:
                        throw new AssertionError(message.what);
                }
            }
        };


        //*/ Added by Tyd Linguanrong for secret photos, 2014-2-17
        mVisitorMode = data.getBoolean(KEY_VISITOR_MODE, false);
        mIsSecretImages = data.getBoolean(KEY_VISITOR_TYPE, true);
        //*/
        //*/ Added by Linguanrong for story album, 2015-4-9
        mStorySelectMode = data.getBoolean(KEY_STORY_SELECT_MODE, false);
        mStoryBucketId = data.getInt(KEY_STORY_SELECT_INDEX, -1);
        mStoryFromChild = data.getBoolean(KEY_STORY_FROM_CHILD, false);
        //*/
        //*/ Added by xueweili for auto select slectionMode, 2015-7-23
        if (mIsHideAlbumSet) {
            mSelectionManager.setAutoLeaveSelectionMode(false);
            mSelectionManager.enterSelectionMode();
        }
        //*/

        // Added by TYD Theobald_Wu on 2014/01 [begin] for jigsaw feature
        mJigsawPicker = data.getBoolean(JigsawEntry.KEY_JIGSAW_PICKER, false);
        // Added by TYD Theobald_Wu on 2014/01 [end]
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDestroyed = true;
        /*/ Added by droi Linguanrong for lock orientation, 16-3-1
        mOrientationManager.unlockOrientation();
        //*/

        cleanupCameraButton();
        mActionModeHandler.destroy();

        //*/ Added by Tyd Linguanrong for Gallery new style, 2014-4-24
        hideCameraButton();
        //*/
    }

    private boolean setupCameraButton() {
        if (!GalleryUtils.isCameraAvailable(mActivity)) return false;
        RelativeLayout galleryRoot = (RelativeLayout) mActivity
                .findViewById(R.id.gallery_root);
        if (galleryRoot == null) return false;

        mEmptyView = LayoutInflater.from(mActivity).inflate(R.layout.empty_tips, null);
        TextView btnCamera = (TextView) mEmptyView.findViewById(R.id.btn_camera);
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                GalleryUtils.startCameraActivity(mActivity);
            }
        });

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        galleryRoot.addView(mEmptyView, lp);
        return true;
    }

    private void cleanupCameraButton() {
        if (mEmptyView == null) return;
        RelativeLayout galleryRoot = (RelativeLayout) mActivity
                .findViewById(R.id.gallery_root);
        if (galleryRoot == null) return;
        galleryRoot.removeView(mEmptyView);
        mEmptyView = null;
    }

    private void showCameraButton() {
        if (mEmptyView == null && !setupCameraButton()) return;
        mEmptyView.setVisibility(View.VISIBLE);

        if (mActionBar == null || mActionBar.getMenu() == null) return;
        MenuItem select = mActionBar.getMenu().findItem(R.id.action_select);
        if (select != null) {
            select.setVisible(false);
            //*/ Added by xueweili for hide menu visble only in CLUSTER_BY_ALBUM type, 2015-7-23
            if (mSelectedAction == FreemeUtils.CLUSTER_BY_ALBUM) {
                String selectBucketIds = mActivity.mSharedPreferences.getString("visible_key", "");
                MenuItem hideItem = mActionBar.getMenu().findItem(R.id.action_visible_all);
                if (hideItem != null) {
                    hideItem.setVisible(!selectBucketIds.isEmpty());
                }
            }
            //*/
        }
        //*/
    }
    volatile boolean mDestroyed = false;
    private void hideCameraButton() {
        if (mEmptyView == null) return;
        mEmptyView.setVisibility(View.GONE);
        //*/ Added by Linguanrong for set menu item visible, 2015-4-24
        if (mActionBar == null || mActionBar.getMenu() == null) return;
        MenuItem select = mActionBar.getMenu().findItem(R.id.action_select);
        if (select != null) {
            select.setVisible(true);
            //*/ Added by xueweili for hide menu visble albumsets not null, 2015-7-23
            if (mSelectedAction == FreemeUtils.CLUSTER_BY_ALBUM) {
                MenuItem hideItem = mActionBar.getMenu().findItem(R.id.action_visible_all);
                if (hideItem != null) {
                    hideItem.setVisible(true);
                }
            }
            //*/
        }
        //*/
    }

    private void clearLoadingBit(int loadingBit) {
        mLoadingBits &= ~loadingBit;
        if (mLoadingBits == 0 && mIsActive) {
            if (mAlbumSetDataAdapter.size() == 0) {
                // If this is not the top of the gallery folder hierarchy,
                // tell the parent AlbumSetPage instance to handle displaying
                // the empty album toast, otherwise show it within this
                // instance

                //*/ Added by Tyd Linguanrong for secret photos, 2014-5-29
                if (mStorySelectMode) {
                    mShowedEmptyToastForSelf = true;
                    mSlotView.invalidate();
                    if (!mGetAlbum) {
                        showCameraButton();
                    }
                } else if (mVisitorMode) {
                    mShowedEmptyToastForSelf = true;
                    showEmptyAlbumToast(Toast.LENGTH_LONG);
                    mSlotView.invalidate();
                } else
                    //*/
                    if (mActivity.getStateManager().getStateCount() > 1) {
                        Intent result = new Intent();
                        result.putExtra(AlbumPage.KEY_EMPTY_ALBUM, true);
                        setStateResult(Activity.RESULT_OK, result);
                        mActivity.getStateManager().finishState(this);
                    } else {
                        mShowedEmptyToastForSelf = true;
                        //showEmptyAlbumToast(Toast.LENGTH_LONG);
                        mSlotView.invalidate();
                        /*/ Modified by Linguanrong for hide camera button in select mode
                        if (!mGetAlbum && !mIsHideAlbumSet) {
                            showCameraButton();
                        }
                        //*/
                    }
                return;
            }
        }
        // Hide the empty album toast if we are in the root instance of
        // AlbumSetPage and the album is no longer empty (for instance,
        // after a sync is completed and web albums have been synced)
        if (mShowedEmptyToastForSelf) {
            mShowedEmptyToastForSelf = false;
            hideEmptyAlbumToast();
            //*/ Modified by Tyd Linguanrong for secret photos, 2014-5-29
            if (!mVisitorMode) {
                hideCameraButton();
            }
            //*/
        }
    }

    private void setLoadingBit(int loadingBit) {
        mLoadingBits |= loadingBit;
    }

    @Override
    public void onPause() {
        super.onPause();
        mIsActive = false;

        //*/ Modified by xueweili for set mIsSelectionMode in GalleryActivity, 2015-7-23
        if (mIsHideAlbumSet) {
            mActivity.mIsSelectionMode = false;
        } else {
            //*/ Added by Linguanrong for story album, 2015-7-2
            mActivity.mIsSelectionMode = mSelectionManager != null && mSelectionManager.inSelectionMode();
            //*/
        }

        //*/
        mAlbumSetDataAdapter.pause();
        mAlbumSetView.pause();
        mActionModeHandler.pause();
        mEyePosition.pause();
        DetailsHelper.pause();
        // Call disableClusterMenu to avoid receiving callback after paused.
        // Don't hide menu here otherwise the list menu will disappear earlier than
        // the action bar, which is janky and unwanted behavior.
        mActionBar.disableClusterMenu(false);
        if (mSyncTask != null) {
            mSyncTask.cancel();
            mSyncTask = null;
            clearLoadingBit(BIT_LOADING_SYNC);
        }

        hideCameraButton();
    }

    @Override
    public void onResume() {
        super.onResume();

        //*/ Added by droi Linguanrong for lock orientation, 16-3-1
        mOrientationManager.lockOrientation(true);
        //*/

        //*/ Added by droi Linguanrong for freeme gallery, 16-1-13
        if (mActivity instanceof GalleryActivity) {
            mActionBar.enableClusterMenu(mSelectedAction, this);
            ((GalleryActivity) mActivity).setBottomTabVisibility(
                    !mGetContent && !mGetAlbum && !mStorySelectMode && !mVisitorMode);
            if (resumeFromCommunity) {
                resumeFromCommunity = false;
                startIntroAnimation();
            }
        }
        //*/

        mIsActive = true;
        setContentPane(mRootPane);

        // Set the reload bit here to prevent it exit this page in clearLoadingBit().
        setLoadingBit(BIT_LOADING_RELOAD);
        mAlbumSetDataAdapter.resume();

        mAlbumSetView.resume();
        mEyePosition.resume();
        mActionModeHandler.resume();
        if (!mInitialSynced) {
            setLoadingBit(BIT_LOADING_SYNC);
            mSyncTask = mMediaSet.requestSync(AlbumSetPage.this);
        }

        //*/ Added by Tyd Linguanrong for Gallery new style, 2014-4-24
        if (mMediaSet != null && mMediaSet.getSubMediaSetCount() != 0) {
            hideCameraButton();
        }
        //*/
		
        /// M: [FEATURE.ADD] [Runtime permission] @{
        if (mClusterType == FilterUtils.CLUSTER_BY_LOCATION
                && !PermissionHelper.checkLocationPermission(mActivity)) {
            Log.i(TAG, "<onResume> CLUSTER_BY_LOCATION, permisison not granted, finish");
            PermissionHelper.showDeniedPrompt(mActivity);
            mActivity.getStateManager().finishState(AlbumSetPage.this);
            return;
        }
        /// @}
    }

    private void initializeData(Bundle data) {
        String mediaPath = data.getString(AlbumSetPage.KEY_MEDIA_PATH);
        //*/ Added by xueweili for init not hidden ablum path, 2015-7-23
        mIsHideAlbumSet = data.getBoolean(KEY_VISIBLE_ALL_SET, false);
        if (mIsHideAlbumSet) {
            mediaPath = FilterUtils.switchFilterPath(mediaPath, FilterUtils.FILTER_HIDE_CAMERA);
            mHideModeHandler = new HideModeHandler(mActivity, mSelectionManager);
        } else {
            mediaPath = FilterUtils.switchFilterPath(mediaPath, FilterUtils.FILTER_CAN_VISIBLE);
        }
        //*/

        mMediaSet = mActivity.getDataManager().getMediaSet(mediaPath);
        mSelectionManager.setSourceMediaSet(mMediaSet);
        mAlbumSetDataAdapter = new AlbumSetDataLoader(
                mActivity, mMediaSet, DATA_CACHE_SIZE);
        mAlbumSetDataAdapter.setLoadingListener(new MyLoadingListener());
        mAlbumSetView.setModel(mAlbumSetDataAdapter);
    }

    private void initializeViews() {
        mSelectionManager = new SelectionManager(mActivity, true);
        mSelectionManager.setSelectionListener(this);

        mConfig = Config.AlbumSetPage.get(mActivity);
        mSlotView = new SlotView(mActivity, mConfig.slotViewSpec);
        //*/Added by droi Linguanrong for Gallery new style, 2013-12-19
        mSlotViewPadding = mConfig.slotViewSpec.slotPadding;
        mBottomPadding = mConfig.slotViewSpec.bottomPadding;
        //*/
        mAlbumSetView = new AlbumSetSlotRenderer(
                mActivity, mSelectionManager, mSlotView, mConfig.labelSpec,
                mConfig.placeholderColor);
        mSlotView.setSlotRenderer(mAlbumSetView);
        mSlotView.setListener(new SlotView.SimpleListener() {
            @Override
            public void onDown(int index) {
                AlbumSetPage.this.onDown(index);
            }

            @Override
            public void onUp(boolean followedByLongPress) {
                AlbumSetPage.this.onUp(followedByLongPress);
            }

            @Override
            public void onSingleTapUp(int slotIndex) {
                AlbumSetPage.this.onSingleTapUp(slotIndex);
            }

            @Override
            public void onLongTap(int slotIndex) {
                AlbumSetPage.this.onLongTap(slotIndex);
            }
        });

        mActionModeHandler = new ActionModeHandler(mActivity, mSelectionManager);
        mActionModeHandler.setActionModeListener(new ActionModeListener() {
            @Override
            public boolean onActionItemClicked(MenuItem item) {
                return onItemSelected(item);
            }
        });
        mRootPane.addComponent(mSlotView);

        //*/ Added by droi Linguanrong for lock orientation, 16-3-1
        mOrientationManager = mActivity.getOrientationManager();
        mActivity.getGLRoot().setOrientationSource(mOrientationManager);
        //*/
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        mActionBar.setDisplayOptions(true, GalleryActionBar.SHOWTITLE);
        //*/ Added by Tyd Linguanrong for secret photos, 2014-2-22
        if (mVisitorMode || mStorySelectMode) {
            mActionBar.setDisplayOptions(true, true);
            mActionBar.setTitle(mActivity.getResources().getString(R.string.app_name));
            return true;
        }
        //*/

        Activity activity = mActivity;
        MenuInflater inflater = getSupportMenuInflater();

        if (mGetContent) {
            if (mActivity instanceof GalleryActivity) {
                ((GalleryActivity) mActivity).setBottomTabVisibility(false);
            }
            //*/ Modified by Tyd Linguanrong for secret photos, 2014-2-22
            if (mActivity instanceof JigsawEntry) {
                mActionBar.setDisplayOptions(true, true);
            } else {
                //inflater.inflate(R.menu.pickup, menu);
                mActionBar.createActionBarMenu(R.menu.pickup, menu);
                menu.findItem(R.id.action_cancel).setVisible(false);
                int typeBits = mData.getInt(
                        GalleryActivity.KEY_TYPE_BITS, DataManager.INCLUDE_IMAGE);
                mActionBar.setTitle(GalleryUtils.getSelectionModePrompt(typeBits));
            }
            //*/
        } else if (mGetAlbum) {
            inflater.inflate(R.menu.pickup, menu);
            mActionBar.setTitle(R.string.select_album);
            //*/ Added by xueweili for set actionbar when in hiden mode , 2015-7-23
        } else if (mIsHideAlbumSet) {
            mHideModeHandler.startHideMode(menu);
            //*/
        } else {
            //*/ Modified by droi Linguanrong for freeme gallery, 2016-1-14
            mActionBar.setDisplayOptions(false, GalleryActionBar.SHOWTITLE);
            //mActionBar.enableClusterMenu(mSelectedAction, this);
            //inflater.inflate(R.menu.albumset, menu);
            mActionBar.createActionBarMenu(R.menu.albumset, menu);
            //*/
            FrameworkSupportUtils.setAiMenu(menu);
            //*/ Modified by droi Linguanrong for freeme gallery, 2016-1-14
            MenuItem selectItem = menu.findItem(R.id.action_select);
            selectItem.setTitle(R.string.select_album);
            /*/
            boolean selectAlbums = !inAlbum &&
                    mActionBar.getClusterTypeAction() == FilterUtils.CLUSTER_BY_ALBUM;
            MenuItem selectItem = menu.findItem(R.id.action_select);
            selectItem.setTitle(activity.getString(
                    selectAlbums ? R.string.select_album : R.string.select_group));
            //*/
            selectItem.setTitle(R.string.select_album);
            MenuItem cameraItem = menu.findItem(R.id.action_camera);
            cameraItem.setVisible(GalleryUtils.isCameraAvailable(activity));

            FilterUtils.setupMenuItems(mActionBar, mMediaSet.getPath(), false);

            Intent helpIntent = HelpUtils.getHelpIntent(activity);

            MenuItem helpItem = menu.findItem(R.id.action_general_help);
            helpItem.setVisible(helpIntent != null);
            if (helpIntent != null) helpItem.setIntent(helpIntent);
            /// M: [BUG.ADD] if get the mTitle/mSubTitle,they will not change @{
            // when switch language.
            if (mTitle != null) {
                mTitle = mMediaSet.getName();
                mSubtitle = GalleryActionBar.getClusterByTypeString(mActivity, mClusterType);
            }
            /// @}
            //*/ Added by xueweili for finde hide menu, 2015-7-23
            MenuItem hideItem = menu.findItem(R.id.action_visible_all);
            if (mSelectedAction != FreemeUtils.CLUSTER_BY_ALBUM) {
                hideItem.setVisible(false);
            }
            //*/

            //*/ Added by Linguanrong for guide, 2015-08-10
            boolean guide = mActivity.mSharedPreferences.getBoolean("showHideGuide", true);
            String str = mActivity.getResources().getString(R.string.hide_album);
            hideItem.setTitle(guide ? getGuideTitle(str) : str);
            //*/
            //*/ Added by droi Linguanrong for freeme gallery, 16-1-14
            mTitle = mTitle != null ? mMediaSet.getName()
                    : mActivity.getResources().getString(R.string.tab_by_all);
            //*/

            mActionBar.setTitle(mTitle);
            mActionBar.setSubtitle(mSubtitle);
        }
        return true;
    }

    @Override
    protected boolean onItemSelected(MenuItem item) {
        Activity activity = mActivity;
        switch (item.getItemId()) {
            case R.id.action_aialbum: {
                ((GalleryActivity)mActivity).gotoAIActivity(GalleryActivity.CLOUD_ALBUM, null);
                return true;
            }
            //*/ Added by Tyd Linguanrong for Gallery new style, 2014-3-5
            case android.R.id.home:
                onBackPressed();
                return true;
            //*/

            case R.id.action_cancel:
                activity.setResult(Activity.RESULT_CANCELED);
                activity.finish();
                return true;
            case R.id.action_select:
                mSelectionManager.setAutoLeaveSelectionMode(false);
                mSelectionManager.enterSelectionMode();
                return true;
            case R.id.action_details:
                if (mAlbumSetDataAdapter.size() != 0) {
                    if (mShowDetails) {
                        hideDetails();
                    } else {
                        showDetails();
                    }
                } else {
                    Toast.makeText(activity,
                            activity.getText(R.string.no_albums_alert),
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.action_camera: {
                GalleryUtils.startCameraActivity(activity);
                return true;
            }
//            case R.id.action_manage_offline: {
//                Bundle data = new Bundle();
//                String mediaPath = mActivity.getDataManager().getTopSetPath(
//                        DataManager.INCLUDE_ALL);
//                data.putString(AlbumSetPage.KEY_MEDIA_PATH, mediaPath);
//                mActivity.getStateManager().startState(ManageCachePage.class, data);
//                return true;
//            }
//            case R.id.action_sync_picasa_albums: {
//                PicasaSource.requestSync(activity);
//                return true;
//            }
            case R.id.action_settings: {
                activity.startActivity(new Intent(activity, GallerySettings.class));
                return true;
            }

            //*/ Added by xueweili for hide function menu, 2015-7-23
            case R.id.action_visible_all: {
                //*/ Added by Linguanrong for guide, 2015-08-10
                if (mActivity.mSharedPreferences.getBoolean("showHideGuide", true)) {
                    mActivity.mEditor.putBoolean("showHideGuide", false);
                    mActivity.mEditor.commit();
                }
                //*/

                //*/ Added by tyd Linguanrong for statistic, 15-12-18
//                StatisticUtil.generateStatisticInfo(mActivity, StatisticData.OPTION_ALBUM_HIDE);
                //*/

                // for baas analytics
//                DroiAnalytics.onEvent(mActivity, StatisticData.OPTION_ALBUM_HIDE);

                Bundle data = new Bundle(getData());
                data.putBoolean(KEY_VISIBLE_ALL_SET, true);
                mActivity.getStateManager().switchState(this, AlbumSetPage.class, data);
                return true;
            }

            case R.id.action_confirm: {
                ArrayList<MediaObject> selected = getSelectedMediaObjects();
                if (selected == null) {
                    selected = new ArrayList<MediaObject>();
                }
                String selectStr = "";
                for (int i = 0; i < selected.size(); i++) {
                    MediaObject selectItem = selected.get(i);
                    if (selectItem instanceof IBucketAlbum) {
                        selectStr = selectStr + ((IBucketAlbum) selectItem).getBucketId() + ",";
                    }
                }
                mActivity.mEditor.putString("visible_key", selectStr);
                mActivity.mEditor.commit();
                hideAlbumStateFinish();
                return true;
            }
            //*/

            // Added by TYD Theobald_Wu on 2014/01 [begin] for jigsaw feature
            case R.id.action_jigsaw: {
                //*/ Added by tyd Linguanrong for statistic, 15-12-18
//                StatisticUtil.generateStatisticInfo(mActivity, StatisticData.OPTION_JIGSAW);
                //*/
                // for baas analytics
//                DroiAnalytics.onEvent(mActivity, StatisticData.OPTION_JIGSAW);

                Intent intent = new Intent(mActivity, JigsawEntry.class);
                mActivity.startActivity(intent);
                return true;
            }
            // Added by TYD Theobald_Wu on 2014/01 [end]

            default:
                return false;
        }
    }

    @Override
    protected void onStateResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_DO_ANIMATION: {
                mSlotView.startRisingAnimation();
            }
        }
    }

    private void hideDetails() {
        mShowDetails = false;
        mDetailsHelper.hide();
        mAlbumSetView.setHighlightItemPath(null);
        mSlotView.invalidate();
    }

    private void showDetails() {
        mShowDetails = true;
        if (mDetailsHelper == null) {
            mDetailsHelper = new DetailsHelper(mActivity, mRootPane, mDetailsSource);
            mDetailsHelper.setCloseListener(new CloseListener() {
                @Override
                public void onClose() {
                    hideDetails();
                }
            });
        }
        mDetailsHelper.show();
    }

    //*/ Added by xueweili for  hide albumset , 2015-7-23
    private void hideAlbumStateFinish() {
        mActionBar.setCustomView(null);
        mActionBar.initActionBar();
        mActionBar.selectTap(2);
        Bundle data = new Bundle(getData());
        data.putBoolean(KEY_VISIBLE_ALL_SET, false);
        mActivity.getStateManager().switchState(this, AlbumSetPage.class, data);
    }

    private class MyLoadingListener implements LoadingListener {
        @Override
        public void onLoadingStarted() {
            setLoadingBit(BIT_LOADING_RELOAD);
        }

        @Override
        public void onLoadingFinished(boolean loadingFailed) {
            clearLoadingBit(BIT_LOADING_RELOAD);

            //*/ Added by xueweili for set hidepage state select Mode, 2015-7-23
            if (mIsHideAlbumSet) {
                String selectBucketIds = mActivity.mSharedPreferences.getString("visible_key", "");
                int count = mMediaSet.getSubMediaSetCount();
                for (int i = 0; i < count; i++) {
                    MediaSet set = mMediaSet.getSubMediaSet(i);
                    IBucketAlbum album = (IBucketAlbum) set;
                    if (selectBucketIds.contains(String.valueOf(album.getBucketId()))) {
                        mSelectionManager.toggle(set.getPath());
                    }
                }
                mHideModeHandler.setEnable(count > 0);
            }
            //*/

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isDestroyed()) {
                        return;
                    }
                    int setCount = mMediaSet != null ? mMediaSet.getSubMediaSetCount() : 0;
                    if (mMediaSet == null) return;

                    if(setCount == 0) {
                        if (!mGetAlbum && !mIsHideAlbumSet) {
                            showCameraButton();
                        }
                    } else {
                        hideCameraButton();
                    }
                }
            }, 500);
            boolean inSelectionMode = (mSelectionManager != null && mSelectionManager
                    .inSelectionMode());
            int setCount = mMediaSet != null ? mMediaSet.getSubMediaSetCount() : 0;
            Log.d(TAG, "<onLoadingFinished> set count=" + setCount);
            Log.d(TAG, "<onLoadingFinished> inSelectionMode=" + inSelectionMode);
            mSelectionManager.onSourceContentChanged();
            boolean restore = false;
            if (setCount > 0 && inSelectionMode) {
                if (mNeedUpdateSelection) {
                    mNeedUpdateSelection = false;
                    restore = true;
                    mSelectionManager.restoreSelection();
                }
                mActionModeHandler.updateSupportedOperation();
                mActionModeHandler.updateSelectionMenu();
            }
            if (!restore) {
                mRestoreSelectionDone = true;
            }
            /// @}
            /// M: [BUG.ADD] @{
            // ClusterAlbumSet name is designed to be localized, and the
            // localized name is calculated in reload(). Therefore we may obtain
            // a miss-localized name which is corresponding to the obsolete
            // (previous) Locale from ClusterAlbumSet if it is not finished
            // reloading. Here we re-get its name after it finished reloading,
            // and set the title of action bar to be the obtained name.
            if (mTitle != null && mActionBar != null && !inSelectionMode
                    && (mMediaSet instanceof ClusterAlbumSet)) {
                String title = mMediaSet.getName();
                String subtitle =
                        GalleryActionBar.getClusterByTypeString(mActivity, mClusterType);
                if (!mTitle.equalsIgnoreCase(title)
                        || (subtitle != null && !subtitle.equalsIgnoreCase(mSubtitle))) {
                    mTitle = title;
                    mSubtitle = subtitle;
                    mActionBar.setTitle(mTitle);
                    mActionBar.setSubtitle(mSubtitle);
                    Log.d(TAG, "<onLoadingFinished> mTitle:" + mTitle + "mSubtitle = "
                            + mSubtitle);
                    mActionBar.notifyDataSetChanged();
                }
            }
            /// @}
        }
    }
    private ArrayList<MediaObject> getSelectedMediaObjects() {
        ArrayList<Path> unexpandedPaths = mSelectionManager.getSelected(false);
        if (unexpandedPaths.isEmpty()) {
            return null;
        }
        ArrayList<MediaObject> selected = new ArrayList<MediaObject>();
        DataManager manager = mActivity.getDataManager();
        for (Path path : unexpandedPaths) {
            selected.add(manager.getMediaObject(path));
        }
        return selected;
    }
    //*/

    //*/ Added by Linguanrong for guide, 2015-08-14
    public CharSequence getGuideTitle(String str) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(str + "  ");
        Drawable drawable = mActivity.getResources().getDrawable(R.drawable.guide_icon);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        ImageSpan span = new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);
        ssb.setSpan(span, ssb.length() - 1, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssb;
    }
    //*/
	    //********************************************************************
    //*                              MTK                                 *
    //********************************************************************

    // Flag to specify whether mSelectionManager.restoreSelection task has done
    private boolean mRestoreSelectionDone;
    // Save selection for onPause/onResume
    private boolean mNeedUpdateSelection = false;
    // If restore selection not done in selection mode,
    // after click one slot, show 'wait' toast
    private Toast mWaitToast = null;

    /// M: [BUG.ADD] leave selection mode when plug out sdcard @{
//    @Override
//    public void onEjectSdcard() {
//        if (mSelectionManager != null && mSelectionManager.inSelectionMode()) {
//            Log.i(TAG, "<onEjectSdcard> leaveSelectionMode");
//            mSelectionManager.leaveSelectionMode();
//        }
//    }
    /// @}

    public void onSelectionRestoreDone() {
        if (!mIsActive) {
            return;
        }
        mRestoreSelectionDone = true;
        // Update selection menu after restore done @{
        mActionModeHandler.updateSupportedOperation();
        mActionModeHandler.updateSelectionMenu();
    }

    /// M: [FEATURE.ADD] fancy layout @{
    private int mLayoutType = -1;
    private DisplayMetrics getDisplayMetrics() {
        WindowManager wm = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics reMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(reMetrics);
        Log.i(TAG, "<getDisplayMetrix> <Fancy> Display Metrics: " + reMetrics.widthPixels
                + " x " + reMetrics.heightPixels);
        return reMetrics;
    }
    /// @}

    /// M: [BUG.ADD] Save dataManager object.
    @Override
    protected void onSaveState(Bundle outState) {
        // keep record of current DataManager object.
        String dataManager = mActivity.getDataManager().toString();
        String processId = String.valueOf(android.os.Process.myPid());
        outState.putString(KEY_DATA_OBJECT, dataManager);
        outState.putString(KEY_PROCESS_ID, processId);
        Log.d(TAG, "<onSaveState> dataManager = " + dataManager
                + ", processId = " + processId);
    }
    /// @}

    /// M: [PERF.ADD] add for delete many files performance improve @{
    @Override
    public void setProviderSensive(boolean isProviderSensive) {
        mAlbumSetDataAdapter.setSourceSensive(isProviderSensive);
    }
    @Override
    public void fakeProviderChange() {
        mAlbumSetDataAdapter.fakeSourceChange();
    }
    /// @}

    /// M: [FEATURE.ADD] [Runtime permission] @{
    private int mNeedDoClusterType = 0;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        if (PermissionHelper.isAllPermissionsGranted(permissions, grantResults)) {
            doCluster(mNeedDoClusterType);
        } else {
            PermissionHelper.showDeniedPromptIfNeeded(mActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }
    /// @}

    /// M: [FEATURE.ADD] Multi-window. @{
    private boolean mIsInMultiWindowMode = false;
//    private MultiWindowListener mMultiWindowListener = new MultiWindowListener();
//
//    /**
//     * Use MultiWindowListener to monitor entering or leaving multi-window.
//     */
//    private class MultiWindowListener implements
//            AbstractGalleryActivity.MultiWindowModeListener {
//
//        @Override
//        public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
//            if (mIsInMultiWindowMode == isInMultiWindowMode) {
//                return;
//            }
//            mRootPane.lockRendering();
//            Log.d(TAG, "<onMultiWindowModeChanged> isInMultiWindowMode: "
//                    + isInMultiWindowMode);
//            mIsInMultiWindowMode = isInMultiWindowMode;
//            if (mIsInMultiWindowMode) {
//                Log.d(TAG, "<onMultiWindowModeChanged> switch to MULTI_WINDOW_LAYOUT");
//                mLayoutType = FancyHelper.MULTI_WINDOW_LAYOUT;
//                mAlbumSetView.onEyePositionChanged(mLayoutType);
//                mSlotView.switchLayout(mLayoutType);
//            } else {
//                Log.d(TAG, "<onMultiWindowModeChanged> <Fancy> enter");
//                DisplayMetrics reMetrics = getDisplayMetrics();
//                FancyHelper.doFancyInitialization(reMetrics.widthPixels,
//                        reMetrics.heightPixels);
//                mLayoutType = mEyePosition.getLayoutType();
//                Log.d(TAG, "<onMultiWindowModeChanged> <Fancy> begin to switchLayout");
//                mAlbumSetView.onEyePositionChanged(mLayoutType);
//                mSlotView.switchLayout(mLayoutType);
//            }
//            mRootPane.unlockRendering();
//        }
//    }
    /// @}
}
