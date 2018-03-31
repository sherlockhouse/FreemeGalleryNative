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

package com.android.gallery3d.ui;

import com.android.gallery3d.data.LocalAlbum;
import com.android.gallery3d.util.MediaSetUtils;
import com.freeme.data.StoryAlbumSet;
import com.freeme.gallery.R;
import com.freeme.gallery.app.AbstractGalleryActivity;
import com.android.gallery3d.app.AlbumSetDataLoader;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.ColorTexture;
import com.android.gallery3d.glrenderer.FadeInTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.ResourceTexture;
import com.android.gallery3d.glrenderer.StringTexture;
import com.android.gallery3d.glrenderer.Texture;
import com.android.gallery3d.glrenderer.TiledTexture;
import com.android.gallery3d.glrenderer.UploadedTexture;

public class AlbumSetSlotRenderer extends AbstractSlotRenderer {
    @SuppressWarnings("unused")
    private static final String TAG        = "AlbumSetView";

    protected final LabelSpec               mLabelSpec;
    public static final int CACHE_SIZE =  96;
    private final int mPlaceholderColor;
    private final   ColorTexture            mWaitLoadingTexture;
    private final   ResourceTexture         mCameraOverlay;
    private final   AbstractGalleryActivity mActivity;
    private final   SelectionManager        mSelectionManager;
    //*/ Added by Linguanrong for story album, 2015-4-7
    private final ResourceTexture mBabyAlbumOverlay;
    private final ResourceTexture mLoveAlbumOverlay;
    private final ResourceTexture mAddAlbumOverlay;
    private final ResourceTexture mBabyAlbumTarget;
    private final ResourceTexture mLoveAlbumTarget;
    private final StringTexture   mAddAlbumText;
    protected AlbumSetSlidingWindow mDataWindow;
    private   SlotView              mSlotView;
    private int mPressedIndex = -1;
    private boolean mAnimatePressedUp;
    private Path mHighlightItemPath = null;
    private boolean mInSelectionMode;
    private int[] mDefaultAlbum = new int[]{
//            R.drawable.default_album_0,
            R.drawable.default_album_1,
            R.drawable.default_album_1,
//            R.drawable.default_album_2
            R.drawable.default_album_1
    };

    private final ResourceTexture[] mDefaultAlbumOverlay = new ResourceTexture[mDefaultAlbum.length];
    //*/

    public static class LabelSpec {
        public int labelBackgroundHeight;
        public int titleOffset;
        public int countOffset;
        public int titleFontSize;
        public int countFontSize;
        public int leftMargin;
        public int iconSize;
        public int titleRightMargin;
        public int backgroundColor;
        public int titleColor;
        public int countColor;
        public int borderSize;
        //*/ Added by Linguanrong for story album, 2015-4-2
        public boolean isStoryAlbum = false;
        //*/
    }

    public AlbumSetSlotRenderer(AbstractGalleryActivity activity,
                                SelectionManager selectionManager,
                                SlotView slotView, LabelSpec labelSpec, int placeholderColor) {
        super(activity);
        mActivity = activity;
        mSelectionManager = selectionManager;
        mSlotView = slotView;
        mLabelSpec = labelSpec;
        mPlaceholderColor = placeholderColor;

        mWaitLoadingTexture = new ColorTexture(mPlaceholderColor);
        mWaitLoadingTexture.setSize(1, 1);
        mCameraOverlay = new ResourceTexture(activity,
                com.freeme.gallery.R.drawable.ic_cameraalbum_overlay);

        //*/ Added by Linguanrong for story album, 2015-4-7
        mBabyAlbumOverlay = new ResourceTexture(activity, R.drawable.default_baby_album);
        mLoveAlbumOverlay = new ResourceTexture(activity, R.drawable.default_love_album);
        mAddAlbumOverlay = new ResourceTexture(activity, R.drawable.default_add_album);
        mBabyAlbumTarget = new ResourceTexture(activity, R.drawable.target_baby_album);
        mLoveAlbumTarget = new ResourceTexture(activity, R.drawable.target_love_album);
        for (int i = 0; i < mDefaultAlbum.length; i++) {
            mDefaultAlbumOverlay[i] = new ResourceTexture(activity, mDefaultAlbum[i]);
        }
        mAddAlbumText = StringTexture.newInstance(
                activity.getResources().getString(R.string.add_story_album),
                labelSpec.titleFontSize, labelSpec.titleColor);
        //*/
    }

    public void setPressedIndex(int index) {
        if (mPressedIndex == index) return;
        mPressedIndex = index;
        mSlotView.invalidate();
    }

    public void setPressedUp() {
        if (mPressedIndex == -1) return;
        mAnimatePressedUp = true;
        mSlotView.invalidate();
    }

    public void setHighlightItemPath(Path path) {
        if (mHighlightItemPath == path) return;
        mHighlightItemPath = path;
        mSlotView.invalidate();
    }

    public void setModel(AlbumSetDataLoader model) {
        if (mDataWindow != null) {
            mDataWindow.setListener(null);
            mDataWindow = null;
            mSlotView.setSlotCount(0);
        }
        if (model != null) {
            mDataWindow = new AlbumSetSlidingWindow(
                    mActivity, model, mLabelSpec, CACHE_SIZE);
            mDataWindow.setListener(new MyCacheListener());
            mSlotView.setSlotCount(mDataWindow.size());
        }
    }

    private static Texture checkLabelTexture(Texture texture) {
        return ((texture instanceof UploadedTexture)
                && ((UploadedTexture) texture).isUploading())
                ? null
                : texture;
    }

    private static Texture checkContentTexture(Texture texture) {
        return ((texture instanceof TiledTexture)
                && !((TiledTexture) texture).isReady())
                ? null
                : texture;
    }

    @Override
    public int renderSlot(GLCanvas canvas, int index, int pass, int width, int height) {
        //*/ Added by droi Linguanrong for freeme gallery, 16-1-16
        boolean isIgnore = isLastStoryAlbum(index);
        if (!isIgnore) {
            width = width - mSlotView.getSlotSpec().slotGap;
            drawOutSideFrame(canvas, 0, 0, width, height + mLabelSpec.labelBackgroundHeight);
        }
        //*/

        AlbumSetSlidingWindow.AlbumSetEntry entry = mDataWindow.get(index);
        int renderRequestFlags = 0;
        renderRequestFlags |= renderContent(canvas, entry, width, height);
        //*/ Modified by Linguanrong for story album, 2015-4-8
        if (mLabelSpec.isStoryAlbum) {
            if (isIgnore) {
                return renderRequestFlags;
            }
            renderStoryAlbumOverlay(canvas, index, width, height);
        }

        if (isAddStoryAlbum(index)) {
            renderAddAlbumLabel(canvas, height);
        } else {
            renderRequestFlags |= renderLabel(canvas, entry, width, height);
        }
        //*/
        renderRequestFlags |= renderOverlay(canvas, index, entry, width, height);
        return renderRequestFlags;
    }




    protected int renderOverlay(
            GLCanvas canvas, int index, AlbumSetSlidingWindow.AlbumSetEntry entry, int width, int height) {
        int renderRequestFlags = 0;
        if (entry.album != null && entry.album.getName().equals(LocalAlbum.getLocalizedName(mActivity.getResources(),
                MediaSetUtils.CLOUD_BUCKET_ID, ""))) {
            drawCloudAlbumOverlay(canvas, width, height);
        }

        if (entry.album != null && entry.album.isCameraRoll()) {
            //*/ Modified by Linguanrong for pos of camera overlay
            int uncoveredHeight = height - mLabelSpec.labelBackgroundHeight;
            int dim = uncoveredHeight / 2 + mLabelSpec.labelBackgroundHeight / 4;
            mCameraOverlay.draw(canvas, (width - dim) / 2,
                    (uncoveredHeight - dim) / 2 + mLabelSpec.labelBackgroundHeight / 2, dim, dim);
            /*/
            int uncoveredHeight = height - mLabelSpec.labelBackgroundHeight;
            int dim = uncoveredHeight / 2;
            mCameraOverlay.draw(canvas, (width - dim) / 2,
                    (uncoveredHeight - dim) / 2, dim, dim);
            //*/
        }

        //*/ Added by Linguanrong for story album, 2015-4-7
        if (isAddStoryAlbum(index)) {
            return renderRequestFlags;
        }
        //*/

        if (mPressedIndex == index) {
            if (mAnimatePressedUp) {
                drawPressedUpFrame(canvas, width, height);
                renderRequestFlags |= SlotView.RENDER_MORE_FRAME;
                if (isPressedUpFrameFinished()) {
                    mAnimatePressedUp = false;
                    mPressedIndex = -1;
                }
            } else {
                drawPressedFrame(canvas, width, height);
            }
        } else if ((mHighlightItemPath != null) && (mHighlightItemPath == entry.setPath)) {
            drawSelectedFrame(canvas, width, height);
        } else if (mInSelectionMode && mSelectionManager.isItemSelected(entry.setPath)) {
            drawSelectedFrame(canvas, width, height);
        }
        return renderRequestFlags;
    }

    private boolean isLastStoryAlbum(int index) {
        return mLabelSpec.isStoryAlbum && mInSelectionMode && isAddStoryAlbum(index);
    }

    protected int renderContent(
            GLCanvas canvas, AlbumSetSlidingWindow.AlbumSetEntry entry, int width, int height) {
        int renderRequestFlags = 0;

        Texture content = checkContentTexture(entry.content);
        if (content == null) {
            content = mWaitLoadingTexture;
            entry.isWaitLoadingDisplayed = true;
        } else if (entry.isWaitLoadingDisplayed) {
            entry.isWaitLoadingDisplayed = false;
            content = new FadeInTexture(mPlaceholderColor, entry.bitmapTexture);
            entry.content = content;
        }
        drawContent(canvas, content, width, height, entry.rotation);
        if ((content instanceof FadeInTexture) &&
                ((FadeInTexture) content).isAnimating()) {
            renderRequestFlags |= SlotView.RENDER_MORE_FRAME;
        }

        return renderRequestFlags;
    }

    //*/


    @Override
    public void prepareDrawing() {
        mInSelectionMode = mSelectionManager.inSelectionMode();
    }

    private class MyCacheListener implements AlbumSetSlidingWindow.Listener {

        @Override
        public void onSizeChanged(int size) {
            mSlotView.setSlotCount(size);
        }

        @Override
        public void onContentChanged() {
            mSlotView.invalidate();
        }
    }

    public void pause() {
        mDataWindow.pause();
    }

    public void resume() {
        mDataWindow.resume();
    }

    public void setInSelectionMode(boolean selectionMode) {
        mInSelectionMode = selectionMode;
    }
    @Override
    public void onVisibleRangeChanged(int visibleStart, int visibleEnd) {
        if (mDataWindow != null) {
            mDataWindow.setActiveWindow(visibleStart, visibleEnd);
        }
    }

    @Override
    public void onSlotSizeChanged(int width, int height) {
        if (mDataWindow != null) {
            mDataWindow.onSlotSizeChanged(width, height);
        }
    }

 
    //*/ Added by Linguanrong for story album, 2015-4-7
    private void renderStoryAlbumOverlay(GLCanvas canvas, int index, int width, int height) {
        if (0 == mDataWindow.getCount(index)) {
            if (0 == index) {
                mBabyAlbumOverlay.draw(canvas, 0, 0, width, height);
                mBabyAlbumTarget.draw(canvas, 0, 0);
            } else if (1 == index) {
                mLoveAlbumOverlay.draw(canvas, 0, 0, width, height);
                mLoveAlbumTarget.draw(canvas, 0, 0);
            } else if (isAddStoryAlbum(index)) {
                mAddAlbumOverlay.draw(canvas, 0, 0, width, height);
            } else {
                mDefaultAlbumOverlay[index % mDefaultAlbum.length].draw(canvas, 0, 0, width, height);
            }
        } else {
            if (0 == index) {
                mBabyAlbumTarget.draw(canvas, 0, 0);
            } else if (1 == index) {
                mLoveAlbumTarget.draw(canvas, 0, 0);
            }
        }
    }

    //*/ Added by Linguanrong for story album, 2015-6-12
    private boolean isAddStoryAlbum(int index) {
        return mLabelSpec.isStoryAlbum && index == mDataWindow.size() - 1
                && mDataWindow.getCount(index) == 0 && !StoryAlbumSet.isNotMaxAlbum;
    }

    private void renderAddAlbumLabel(GLCanvas canvas, int height) {
        int b = AlbumLabelMaker.getBorderSize();
        int h = mLabelSpec.labelBackgroundHeight;
        //mAddAlbumText.draw(canvas, -b, height - h / 2 + b);
        mAddAlbumText.draw(canvas, b + mLabelSpec.leftMargin, height + b +
                (mLabelSpec.labelBackgroundHeight - mLabelSpec.titleFontSize) / 2); //under
    }

    protected int renderLabel(
            GLCanvas canvas, AlbumSetSlidingWindow.AlbumSetEntry entry, int width, int height) {
        Texture content = checkLabelTexture(entry.labelTexture);
        if (content == null) {
            content = mWaitLoadingTexture;
        }
        int b = AlbumLabelMaker.getBorderSize();
        int h = mLabelSpec.labelBackgroundHeight;
        //*/ Modified by droi Linguanrong for freeme gallery, 16-1-16
        //content.draw(canvas, -b, height - h + b, width + b + b, h);
        content.draw(canvas, -b, height + b, width + b + b, h); // under
        //*/

        return 0;
    }
    //*/
}
