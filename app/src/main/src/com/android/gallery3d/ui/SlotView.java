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

import android.graphics.Rect;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;

import com.android.gallery3d.anim.Animation;
import com.android.gallery3d.anim.StateTransitionAnimation;
import com.android.gallery3d.app.GalleryActionBar;
import com.android.gallery3d.data.MediaItem;
import com.freeme.gallery.app.AbstractGalleryActivity;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.common.Utils;
import com.mediatek.gallery3d.layout.FancyHelper;
import com.mediatek.galleryframework.base.MediaData.MediaType;

import java.util.ArrayList;
import java.util.HashMap;

public class SlotView extends GLView {


    @SuppressWarnings("unused")
    private static final String TAG = "SlotView";
    //*/Modified by Tyd Linguanrong for Gallery new style, 2013-12-17
    //private static final boolean WIDE = true;
    private static final boolean WIDE       = false;
    //*/
    private static final int     INDEX_NONE = -1;
    public static final int RENDER_MORE_PASS = 1;
    public static final int RENDER_MORE_FRAME = 2;
    public interface Listener {
        public void onDown(int index);
        public void onUp(boolean followedByLongPress);
        public void onSingleTapUp(int index);
        public void onLongTap(int index);
        public void onScrollPositionChanged(int position, int total);
    }

    public static class SimpleListener implements Listener {
        @Override public void onDown(int index) {}
        @Override public void onUp(boolean followedByLongPress) {}
        @Override public void onSingleTapUp(int index) {}
        @Override public void onLongTap(int index) {}
        @Override public void onScrollPositionChanged(int position, int total) {}
    }

    public static interface SlotRenderer {
        public void prepareDrawing();
        public void onVisibleRangeChanged(int visibleStart, int visibleEnd);
        public void onSlotSizeChanged(int width, int height);
        public int renderSlot(GLCanvas canvas, int index, int pass, int width, int height);
    }

    private final GestureDetector mGestureDetector;
    private final ScrollerHelper mScroller;
    private final Paper mPaper = new Paper();
    private final Layout        mLayout        = new Layout();


    private Listener                mListener;
    private UserInteractionListener mUIListener;
    private       boolean       mMoreAnimation = false;
    private       SlotAnimation mAnimation     = null;
    private       int           mStartIndex    = INDEX_NONE;
    // whether the down action happened while the view is scrolling.
    private boolean mDownInScrolling;
    private int mOverscrollEffect = OVERSCROLL_NONE;
    private final Handler mHandler;

    private SlotRenderer mRenderer;

    private int[] mRequestRenderSlots = new int[16];

    public static final int OVERSCROLL_3D = 0;
    public static final int OVERSCROLL_SYSTEM = 1;
    public static final int OVERSCROLL_NONE = 2;

    // to prevent allocating memory
    private final Rect mTempRect = new Rect();

    public SlotView(AbstractGalleryActivity activity, Spec spec, AlbumSetSlotRenderer.LabelSpec labelSpec) {
        mGestureDetector = new GestureDetector(activity, new MyGestureListener());
        mScroller = new ScrollerHelper(activity);
        mHandler = new SynchronizedHandler(activity.getGLRoot());
        setSlotSpec(spec, labelSpec);
    }

    public SlotView(AbstractGalleryActivity activity, Spec spec) {
        mGestureDetector = new GestureDetector(activity, new MyGestureListener());
        mScroller = new ScrollerHelper(activity);
        mHandler = new SynchronizedHandler(activity.getGLRoot());
        setSlotSpec(spec);
    }

    public void setSlotRenderer(SlotRenderer slotDrawer) {
        mRenderer = slotDrawer;
        if (mRenderer != null) {
            mRenderer.onSlotSizeChanged(mLayout.mSlotWidth, mLayout.mSlotHeight);
            mRenderer.onVisibleRangeChanged(getVisibleStart(), getVisibleEnd());
        }
    }



    public void setCenterIndex(int index) {
        int slotCount = mLayout.mSlotCount;
        if (index < 0 || index >= slotCount) {
            return;
        }
        Rect rect = mLayout.getSlotRect(index, mTempRect);
        int position = WIDE
                ? (rect.left + rect.right - getWidth()) / 2
                : (rect.top + rect.bottom - getHeight()) / 2;
        setScrollPosition(position);
    }

    public void makeSlotVisible(int index) {
        Rect rect = mLayout.getSlotRect(index, mTempRect);
        if (rect == null) {
            return;
        }
        int visibleBegin = WIDE ? mScrollX : mScrollY;
        int visibleLength = WIDE ? getWidth() : getHeight();
        int visibleEnd = visibleBegin + visibleLength;
        int slotBegin = WIDE ? rect.left : rect.top;
        int slotEnd = WIDE ? rect.right : rect.bottom;

        int position = visibleBegin;
        if (visibleLength < slotEnd - slotBegin) {
            position = visibleBegin;
        } else if (slotBegin < visibleBegin) {
            position = slotBegin;
        } else if (slotEnd > visibleEnd) {
            position = slotEnd - visibleLength;
        }

        setScrollPosition(position);
    }


    //*/ Added by droi Linguanrong for freeme gallery, 16-1-16
    public Spec getSlotSpec() {
        return mLayout.mSpec;
    }
    public void setScrollPosition(int position) {
        position = Utils.clamp(position, 0, mLayout.getScrollLimit());
        mScroller.setPosition(position);
        updateScrollPosition(position, false);
    }

    public void setSlotSpec(Spec spec, AlbumSetSlotRenderer.LabelSpec labelSpec) {
        mLayout.setSlotSpec(spec, labelSpec);
    }

    public void setSlotSpec(Spec spec) {
        mLayout.setSlotSpec(spec);
    }

    @Override
    public void addComponent(GLView view) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void onLayout(boolean changeSize, int l, int t, int r, int b) {
        if (!changeSize) return;

        // Make sure we are still at a resonable scroll position after the size
        // is changed (like orientation change). We choose to keep the center
        // visible slot still visible. This is arbitrary but reasonable.
        int visibleIndex =
                (mLayout.getVisibleStart() + mLayout.getVisibleEnd()) / 2;
        mLayout.setSize(r - l, b - t);
        makeSlotVisible(visibleIndex);
        if (mOverscrollEffect == OVERSCROLL_3D) {
            mPaper.setSize(r - l, b - t);
        }
    }

    public void startScatteringAnimation(RelativePosition position) {
        mAnimation = new ScatteringAnimation(position);
        mAnimation.start();
        if (mLayout.mSlotCount != 0) invalidate();
    }

    public void startRisingAnimation() {
        mAnimation = new RisingAnimation();
        mAnimation.start();
        if (mLayout.mSlotCount != 0) invalidate();
    }
    private void updateScrollPosition(int position, boolean force) {
        if (!force && (WIDE ? position == mScrollX : position == mScrollY)) return;
        if (WIDE) {
            mScrollX = position;
        } else {
            mScrollY = position;
        }
        mLayout.setScrollPosition(position);
        onScrollPositionChanged(position);
    }

    protected void onScrollPositionChanged(int newPosition) {
        int limit = mLayout.getScrollLimit();
        mListener.onScrollPositionChanged(newPosition, limit);
    }
    public Rect getSlotRect(int slotIndex) {
        return mLayout.getSlotRect(slotIndex, new Rect());
    }

    @Override
    protected boolean onTouch(MotionEvent event) {
        if (mUIListener != null) mUIListener.onUserInteraction();
        mGestureDetector.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownInScrolling = !mScroller.isFinished();
                mScroller.forceFinished();
                break;
            case MotionEvent.ACTION_UP:
                mPaper.onRelease();
                invalidate();
                break;
        }
        return true;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setUserInteractionListener(UserInteractionListener listener) {
        mUIListener = listener;
    }

    public void setOverscrollEffect(int kind) {
        mOverscrollEffect = kind;
        mScroller.setOverfling(kind == OVERSCROLL_SYSTEM);
    }

    private static int[] expandIntArray(int array[], int capacity) {
        while (array.length < capacity) {
            array = new int[array.length * 2];
        }
        return array;
    }

    @Override
    protected void render(GLCanvas canvas) {
        super.render(canvas);

        if (mRenderer == null) return;
        mRenderer.prepareDrawing();

        long animTime = AnimationTime.get();
        boolean more = mScroller.advanceAnimation(animTime);
        more |= mLayout.advanceAnimation(animTime);
        int oldX = mScrollX;
        updateScrollPosition(mScroller.getPosition(), false);

        boolean paperActive = false;
        if (mOverscrollEffect == OVERSCROLL_3D) {
            // Check if an edge is reached and notify mPaper if so.
            int newX = mScrollX;
            int limit = mLayout.getScrollLimit();
            if (oldX > 0 && newX == 0 || oldX < limit && newX == limit) {
                float v = mScroller.getCurrVelocity();
                if (newX == limit) v = -v;

                // I don't know why, but getCurrVelocity() can return NaN.
                if (!Float.isNaN(v)) {
                    mPaper.edgeReached(v);
                }
            }
            paperActive = mPaper.advanceAnimation();
        }

        more |= paperActive;

        if (mAnimation != null) {
            more |= mAnimation.calculate(animTime);
        }

        canvas.translate(-mScrollX, -mScrollY);

        int requestCount = 0;
        int requestedSlot[] = expandIntArray(mRequestRenderSlots,
                mLayout.mVisibleEnd - mLayout.mVisibleStart);

        for (int i = mLayout.mVisibleEnd - 1; i >= mLayout.mVisibleStart; --i) {
            int r = renderItem(canvas, i, 0, paperActive);
            if ((r & RENDER_MORE_FRAME) != 0) more = true;
            if ((r & RENDER_MORE_PASS) != 0) requestedSlot[requestCount++] = i;
        }

        for (int pass = 1; requestCount != 0; ++pass) {
            int newCount = 0;
            for (int i = 0; i < requestCount; ++i) {
                int r = renderItem(canvas,
                        requestedSlot[i], pass, paperActive);
                if ((r & RENDER_MORE_FRAME) != 0) more = true;
                if ((r & RENDER_MORE_PASS) != 0) requestedSlot[newCount++] = i;
            }
            requestCount = newCount;
        }

        canvas.translate(mScrollX, mScrollY);

        if (more) invalidate();

        final UserInteractionListener listener = mUIListener;
        if (mMoreAnimation && !more && listener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onUserInteractionEnd();
                }
            });
        }
        mMoreAnimation = more;
    }

    private int renderItem(
            GLCanvas canvas, int index, int pass, boolean paperActive) {
        canvas.save(GLCanvas.SAVE_FLAG_ALPHA | GLCanvas.SAVE_FLAG_MATRIX);
        Rect rect = mLayout.getSlotRect(index, mTempRect);
        if (paperActive) {
            //*/ Modified by Tyd Linguanrong for Gallery new style, 2013-12-30
            //canvas.multiplyMatrix(mPaper.getTransform(rect, mScrollX), 0);
            canvas.multiplyMatrix(mPaper.getTransformH(rect, mScrollY), 0);
            //*/
        } else {
            canvas.translate(rect.left, rect.top, 0);
        }
        if (mAnimation != null && mAnimation.isActive()) {
            mAnimation.apply(canvas, index, rect);
        }
        int result = mRenderer.renderSlot(
                canvas, index, pass, rect.right - rect.left, rect.bottom - rect.top);
        canvas.restore();
        return result;
    }

    public static abstract class SlotAnimation extends Animation {
        protected float mProgress = 0;

        public SlotAnimation() {
            setInterpolator(new DecelerateInterpolator(4));
            setDuration(1500);
        }

        @Override
        protected void onCalculate(float progress) {
            mProgress = progress;
        }

        abstract public void apply(GLCanvas canvas, int slotIndex, Rect target);
    }

    public static class RisingAnimation extends SlotAnimation {
        private static final int RISING_DISTANCE = 128;

        @Override
        public void apply(GLCanvas canvas, int slotIndex, Rect target) {
            canvas.translate(0, 0, RISING_DISTANCE * (1 - mProgress));
        }
    }

    public static class ScatteringAnimation extends SlotAnimation {
        private int PHOTO_DISTANCE = 1000;
        private RelativePosition mCenter;

        public ScatteringAnimation(RelativePosition center) {
            mCenter = center;
        }

        @Override
        public void apply(GLCanvas canvas, int slotIndex, Rect target) {
            canvas.translate(
                    (mCenter.getX() - target.centerX()) * (1 - mProgress),
                    (mCenter.getY() - target.centerY()) * (1 - mProgress),
                    slotIndex * PHOTO_DISTANCE * (1 - mProgress));
            canvas.setAlpha(mProgress);
        }
    }

    // This Spec class is used to specify the size of each slot in the SlotView.
    // There are two ways to do it:
    //
    // (1) Specify slotWidth and slotHeight: they specify the width and height
    //     of each slot. The number of rows and the gap between slots will be
    //     determined automatically.
    // (2) Specify rowsLand, rowsPort, and slotGap: they specify the number
    //     of rows in landscape/portrait mode and the gap between slots. The
    //     width and height of each slot is determined automatically.
    //
    // The initial value of -1 means they are not specified.
    public static class Spec {
        public int slotWidth = -1;
        public int slotHeight = -1;
        public int slotHeightAdditional = 0;

        public int rowsLand = -1;
        public int rowsPort = -1;
        public int slotGap = -1;

        //*/Added by Tyd Linguanrong for Gallery new style, 2013-12-19
        public int slotPadding = 0;
        public int bottomPadding = 0;
        public int slotGapV = 0;
        //*/
    }
    

    private class MyGestureListener implements GestureDetector.OnGestureListener {
        private boolean isDown;

        // We call the listener's onDown() when our onShowPress() is called and
        // call the listener's onUp() when we receive any further event.
        @Override
        public void onShowPress(MotionEvent e) {
            GLRoot root = getGLRoot();
            root.lockRenderThread();
            try {
                if (isDown) return;
                int index = mLayout.getSlotIndexByPosition(e.getX(), e.getY());
                if (index != INDEX_NONE) {
                    isDown = true;
                    mListener.onDown(index);
                }
            } finally {
                root.unlockRenderThread();
            }
        }

        private void cancelDown(boolean byLongPress) {
            if (!isDown) return;
            isDown = false;
            mListener.onUp(byLongPress);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1,
                MotionEvent e2, float velocityX, float velocityY) {
            cancelDown(false);
            int scrollLimit = mLayout.getScrollLimit();
            if (scrollLimit == 0) return false;
            float velocity = WIDE ? velocityX : velocityY;
            mScroller.fling((int) -velocity, 0, scrollLimit);
            if (mUIListener != null) mUIListener.onUserInteractionBegin();
            invalidate();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1,
                MotionEvent e2, float distanceX, float distanceY) {
            cancelDown(false);
            float distance = WIDE ? distanceX : distanceY;
            int overDistance = mScroller.startScroll(
                    Math.round(distance), 0, mLayout.getScrollLimit());
            if (mOverscrollEffect == OVERSCROLL_3D && overDistance != 0) {
                mPaper.overScroll(overDistance);
            }
            invalidate();
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            cancelDown(false);
            if (mDownInScrolling) return true;

            //*/ Added by Linguanrong for gallery pick animation, 2014-9-3
            StateTransitionAnimation.setOffset((int) e.getX(), (int) e.getY());
            //*/

            int index = mLayout.getSlotIndexByPosition(e.getX(), e.getY());
            if (index != INDEX_NONE) mListener.onSingleTapUp(index);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            cancelDown(true);
            if (mDownInScrolling) return;
            lockRendering();
            try {
                int index = mLayout.getSlotIndexByPosition(e.getX(), e.getY());
                if (index != INDEX_NONE) mListener.onLongTap(index);
            } finally {
                unlockRendering();
            }
        }
    }

    public void setStartIndex(int index) {
        mStartIndex = index;
    }

    // Return true if the layout parameters have been changed
    public boolean setSlotCount(int slotCount) {
        boolean changed = mLayout.setSlotCount(slotCount);

        // mStartIndex is applied the first time setSlotCount is called.
        if (mStartIndex != INDEX_NONE) {
            setCenterIndex(mStartIndex);
            mStartIndex = INDEX_NONE;
        }
        // Reset the scroll position to avoid scrolling over the updated limit.
        setScrollPosition(WIDE ? mScrollX : mScrollY);
        return changed;
    }

    public int getVisibleStart() {
        return mLayout.getVisibleStart();
    }

    public int getVisibleEnd() {
        return mLayout.getVisibleEnd();
    }

    public int getScrollX() {
        return mScrollX;
    }

    public int getScrollY() {
        return mScrollY;
    }

    public Rect getSlotRect(int slotIndex, GLView rootPane) {
        // Get slot rectangle relative to this root pane.
        Rect offset = new Rect();
        rootPane.getBoundsOf(this, offset);
        Rect r = getSlotRect(slotIndex);
        r.offset(offset.left - getScrollX(),
                offset.top - getScrollY());
        return r;
    }

    private static class IntegerAnimation extends Animation {
        private int mTarget;
        private int mCurrent = 0;
        private int mFrom = 0;
        private boolean mEnabled = false;

        public void setEnabled(boolean enabled) {
            mEnabled = enabled;
        }

        public void startAnimateTo(int target) {
            if (!mEnabled) {
                mTarget = mCurrent = target;
                return;
            }
            if (target == mTarget) return;

            mFrom = mCurrent;
            mTarget = target;
            setDuration(180);
            start();
        }

        public int get() {
            return mCurrent;
        }

        public int getTarget() {
            return mTarget;
        }

        @Override
        protected void onCalculate(float progress) {
            mCurrent = Math.round(mFrom + progress * (mTarget - mFrom));
            if (progress == 1f) mEnabled = false;
        }
    }

    public class Layout {

        private int mVisibleStart;
        private int mVisibleEnd;

        private int mSlotCount;
        private int mSlotWidth;
        private int mSlotHeight;
        private int mSlotGap;
        //*/ Added by Linguanrong for story album, 2015-4-3
        private int mSlotGapV;
        //*/

        private Spec mSpec;
        private AlbumSetSlotRenderer.LabelSpec mLabelSpec;

        private int mWidth;
        private int mHeight;

        private int mUnitCount;
        private int mContentLength;
        private int mScrollPosition;

        //*/Added by Tyd Linguanrong for Gallery new style, 2013-12-17
        private int mSlotPadding = 0;
        //*/

        private IntegerAnimation mVerticalPadding   = new IntegerAnimation();
        private IntegerAnimation mHorizontalPadding = new IntegerAnimation();

        public void setSlotSpec(Spec spec, AlbumSetSlotRenderer.LabelSpec labelSpec) {
            mSpec = spec;
            mLabelSpec = labelSpec;
        }

        public void setSlotSpec(Spec spec) {
            mSpec = spec;
        }

        public boolean setSlotCount(int slotCount) {
            if (slotCount == mSlotCount) return false;
            if (mSlotCount != 0) {
                mHorizontalPadding.setEnabled(true);
                mVerticalPadding.setEnabled(true);
            }
            mSlotCount = slotCount;
            int hPadding = mHorizontalPadding.getTarget();
            int vPadding = mVerticalPadding.getTarget();
            initLayoutParameters();
            return vPadding != mVerticalPadding.getTarget()
                    || hPadding != mHorizontalPadding.getTarget();
        }

        private void initLayoutParameters() {
            // Initialize mSlotWidth and mSlotHeight from mSpec

            //*/Modified by Tyd Linguanrong for Gallery new style, 2013-12-17
            mSlotPadding = mSpec.slotPadding != 0 ? mSpec.slotPadding : 0;
            mSlotGap = mSpec.slotWidth != -1 ? 0 : mSpec.slotGap;
            mSlotGapV = mSpec.slotGapV == 0 ? mSpec.slotGap : mSpec.slotGapV;

            if (mLabelSpec != null) {
                mSlotPadding = mSlotGap = mWidth / 23;
            }
            if (mSpec.slotWidth != -1) {
                mSlotGap = 0;
                mSlotWidth = mSpec.slotWidth;
                mSlotHeight = mSpec.slotHeight;
            } else {
                if (WIDE) {
                    int rows = (mWidth > mHeight) ? mSpec.rowsLand : mSpec.rowsPort;
                    //mSlotGap = mSpec.slotGap;
                    mSlotHeight = Math.max(1, (mHeight - (rows - 1) * mSlotGap) / rows);
                    mSlotWidth = mSlotHeight - mSpec.slotHeightAdditional;
                } else {
                    boolean isLand = false;//(mWidth > mHeight) ? true : false;
                    int columns = isLand ? mSpec.rowsPort : mSpec.rowsLand;
                    mSlotHeight = mSlotWidth = Math.max(1,
                            (mWidth - (columns - 1) * mSlotGap - mSlotPadding * 2) / columns);
                    if (mLabelSpec != null) { //use mLabelSpec to judge if in new albumset mode to avoid change albumpage
                        mSlotHeight = mSlotWidth * 2 / 3;
                    }
//                    int rows = isLand ? mSpec.rowsLand : mSpec.rowsPort;
//                    if(mActivity instanceof JigsawEntry) rows = rows - 1;
//                    if(mSpec.slotGapV != 0) {
//                        mSlotHeight = mSlotWidth;
//                    } else {
//                        mSlotHeight = Math.max(1, (mHeight - (rows - 1) * mSlotGap - mSlotPadding) / rows);
//                    }

                }
            }

            if (mLabelSpec != null) {
                mSlotGapV = mLabelSpec.titleFontSize * 3 + mLabelSpec.countFontSize;
            }
            //*/

            if (mRenderer != null) {
                mRenderer.onSlotSizeChanged(mSlotWidth, mSlotHeight);
            }

            int[] padding = new int[2];
            if (WIDE) {
                initLayoutParameters(mWidth, mHeight, mSlotWidth, mSlotHeight, padding);
                mVerticalPadding.startAnimateTo(padding[0]);
                mHorizontalPadding.startAnimateTo(padding[1]);
            } else {
                initLayoutParameters(mHeight, mWidth, mSlotHeight, mSlotWidth, padding);
                mVerticalPadding.startAnimateTo(padding[1]);
                mHorizontalPadding.startAnimateTo(padding[0]);
            }
            updateVisibleSlotRange();
        }

        // Calculate
        // (1) mUnitCount: the number of slots we can fit into one column (or row).
        // (2) mContentLength: the width (or height) we need to display all the
        //     columns (rows).
        // (3) padding[]: the vertical and horizontal padding we need in order
        //     to put the slots towards to the center of the display.
        //
        // The "major" direction is the direction the user can scroll. The other
        // direction is the "minor" direction.
        //
        // The comments inside this method are the description when the major
        // directon is horizontal (X), and the minor directon is vertical (Y).
        private void initLayoutParameters(
                int majorLength, int minorLength,  /* The view width and height */
                int majorUnitSize, int minorUnitSize,  /* The slot width and height */
                int[] padding) {
            int unitCount = (minorLength + mSlotGap) / (minorUnitSize + mSlotGap);
            if (unitCount == 0) unitCount = 1;
            mUnitCount = unitCount;

            // We put extra padding above and below the column.
            int availableUnits = Math.min(mUnitCount, mSlotCount);
            int usedMinorLength = availableUnits * minorUnitSize +
                    (availableUnits - 1) * mSlotGap;
            padding[0] = (minorLength - usedMinorLength) / 2;

            // Then calculate how many columns we need for all slots.
            int count = ((mSlotCount + mUnitCount - 1) / mUnitCount);
            //*/Modified by Tyd Linguanrong for Gallery new style, 2013-12-20
            //mContentLength = count * majorUnitSize + (count - 1) * mSlotGap;
            mContentLength = count * majorUnitSize + count * mSlotGapV;
            //*/

            // If the content length is less then the screen width, put
            // extra padding in left and right.
            padding[1] = Math.max(0, (majorLength - mContentLength) / 2);

            //*/Added by Tyd Linguanrong for Gallery new style, 2013-12-17
            if (!WIDE) {
                padding[0] = mSlotPadding;
                padding[1] = mSlotPadding;
            }
            //*/
        }

        private void updateVisibleSlotRange() {
            int position = mScrollPosition;

            if (WIDE) {
                int startCol = position / (mSlotWidth + mSlotGap);
                int start = Math.max(0, mUnitCount * startCol);
                int endCol = (position + mWidth + mSlotWidth + mSlotGap - 1) /
                        (mSlotWidth + mSlotGap);
                int end = Math.min(mSlotCount, mUnitCount * endCol);
                setVisibleRange(start, end);
            } else {
                int startRow = position / (mSlotHeight + mSlotGapV);
                int start = Math.max(0, mUnitCount * startRow);
                int endRow = (position + mHeight + mSlotHeight + mSlotGapV - 1) /
                        (mSlotHeight + mSlotGapV);
                int end = Math.min(mSlotCount, mUnitCount * endRow);
                setVisibleRange(start, end);
            }
        }

        private void setVisibleRange(int start, int end) {
            if (start == mVisibleStart && end == mVisibleEnd) return;
            if (start < end) {
                mVisibleStart = start;
                mVisibleEnd = end;
            } else {
                mVisibleStart = mVisibleEnd = 0;
            }
            if (mRenderer != null) {
                mRenderer.onVisibleRangeChanged(mVisibleStart, mVisibleEnd);
            }
        }

        public Rect getSlotRect(int index, Rect rect) {
            if (mUnitCount <= 0) {
                return null;
            }
            int col, row;
            if (WIDE) {
                col = index / mUnitCount;
                row = index - col * mUnitCount;
            } else {
                row = index / mUnitCount;
                col = index - row * mUnitCount;
            }

            int x = mHorizontalPadding.get() + col * (mSlotWidth + mSlotGap);
            int y = mVerticalPadding.get() + row * (mSlotHeight + mSlotGapV);
            rect.set(x, y, x + mSlotWidth, y + mSlotHeight);
            return rect;
        }

        public int getSlotWidth() {
            return mSlotWidth;
        }

        public int getSlotHeight() {
            return mSlotHeight;
        }

        public void setSize(int width, int height) {
            mWidth = width;
            mHeight = height;
            initLayoutParameters();
        }

        public void setScrollPosition(int position) {
            if (mScrollPosition == position) return;
            mScrollPosition = position;
            updateVisibleSlotRange();
        }

        public int getVisibleStart() {
            return mVisibleStart;
        }

        public int getVisibleEnd() {
            return mVisibleEnd;
        }

        public int getSlotIndexByPosition(float x, float y) {
            int absoluteX = Math.round(x) + (WIDE ? mScrollPosition : 0);
            int absoluteY = Math.round(y) + (WIDE ? 0 : mScrollPosition);

            absoluteX -= mHorizontalPadding.get();
            absoluteY -= mVerticalPadding.get();

            if (absoluteX < 0 || absoluteY < 0) {
                return INDEX_NONE;
            }

            int columnIdx = absoluteX / (mSlotWidth + mSlotGap);
            int rowIdx = absoluteY / (mSlotHeight + mSlotGapV);

            if (!WIDE && columnIdx >= mUnitCount) {
                return INDEX_NONE;
            }

            if (WIDE && rowIdx >= mUnitCount) {
                return INDEX_NONE;
            }

            if (absoluteX % (mSlotWidth + mSlotGap) >= mSlotWidth) {
                return INDEX_NONE;
            }

            if (absoluteY % (mSlotHeight + mSlotGapV) >= mSlotHeight) {
                return INDEX_NONE;
            }

            int index = WIDE
                    ? (columnIdx * mUnitCount + rowIdx)
                    : (rowIdx * mUnitCount + columnIdx);

            return index >= mSlotCount ? INDEX_NONE : index;
        }

        public int getScrollLimit() {
            int limit = WIDE ? mContentLength - mWidth : mContentLength - mHeight;
            return limit <= 0 ? 0 : limit;
        }

        public boolean advanceAnimation(long animTime) {
            // use '|' to make sure both sides will be executed
            return mVerticalPadding.calculate(animTime) | mHorizontalPadding.calculate(animTime);
        }
    }
    /// M: [FEATURE.ADD] fancy layout @{
    private ArrayList<SlotEntry> mSlotArray = new ArrayList<SlotEntry>();
    private HashMap<Integer, ArrayList<SlotEntry>> mSlotMapByColumn =
            new HashMap<Integer, ArrayList<SlotEntry>>();

    /**
     * slotEntry stands for each slot.
     */
    public static class SlotEntry {
        public int slotIndex;
        public int imageWidth;
        public int imageHeight;
        public int scaledWidth;
        // use these info to judge if slot is changed
        public int oriImageWidth;
        public int oriImageHeight;
        public String mimeType;
        public int rotation;
        public String albumName;

        public int scaledHeight;
        public int inWhichCol;
        public int inWhichRow;
        public Rect slotRect;
        public boolean isLandCameraFolder = false;
        private MediaType mMediaType = MediaType.INVALID;

        /**
         * Construction of SlotEntry.
         * @param index The index of slot
         * @param oritation The orientation of item, PORT or LAND
         * @param imageW the width of slot image
         * @param imageH the height of slot image
         * @param item the MediaItem of current index
         * @param imageRotation the rotation of media item
         * @param slotW the width of slot
         * @param gap the gap between slot
         * @param isCameraFolder if current folder is camera or not
         * @param name the name of current item
         */
        public SlotEntry(int index, int oritation, int imageW, int imageH, MediaItem item,
                         int imageRotation, int slotW, int gap, boolean isCameraFolder, String name) {
            slotIndex = index;
            imageWidth = imageW;
            imageHeight = imageH;
            rotation = imageRotation;
            oriImageWidth = item.getWidth();
            oriImageHeight = item.getHeight();
            mimeType = item.getMimeType();
            albumName = name;

            if (imageWidth <= 0 || imageHeight <= 0) {
                // fix JE: item width or height is 0 in db
                imageWidth = slotW;
                imageHeight = slotW;
                isLandCameraFolder = false;
                scaledWidth = scaledHeight = slotW;
                Log.d(TAG, "<SlotEntry> imageWidth or imageHeight is 0, make default entry!!");
            } else {
                if (isCameraFolder && oritation == LAND) {
                    isLandCameraFolder = true;
                    scaledWidth = 2 * slotW + gap;
                    // scaledWidth and scaledHeight must meet 5:2 or 2:5
                    scaledHeight = Utils.clamp(imageHeight * scaledWidth / imageWidth,
                            (int) (scaledWidth * FancyHelper.FANCY_CROP_RATIO_LAND),
                            (int) (scaledWidth * FancyHelper.FANCY_CROP_RATIO));
                    MediaType mediaType = item.getMediaData().mediaType;
                    if (mediaType != MediaType.VIDEO) {
                        scaledHeight = Math.round((float) scaledWidth
                                / FancyHelper.FANCY_CROP_RATIO_CAMERA);
                    }
                    mMediaType = mediaType;
                } else {
                    scaledWidth = slotW;
                    // scaledWidth and scaledHeight must meet 5:2 or 2:5
                    scaledHeight = Utils.clamp(imageHeight * scaledWidth / imageWidth,
                            (int) (scaledWidth * FancyHelper.FANCY_CROP_RATIO_LAND),
                            (int) (scaledWidth * FancyHelper.FANCY_CROP_RATIO));
                }
            }
        }

        /**
         * Update the parameters of slot entry.
         * @param slotWidth
         * @param slotGap
         */
        public void update(int slotWidth, int slotGap) {
            if (imageWidth == 0 || imageHeight == 0) {
                scaledWidth = scaledHeight = slotWidth;
            } else {
                if (isLandCameraFolder) {
                    scaledWidth = 2 * slotWidth + slotGap;
                    // scaledWidth and scaledHeight must meet 5:2 or 2:5
                    scaledHeight = Utils.clamp(imageHeight * scaledWidth / imageWidth,
                            (int) (scaledWidth * FancyHelper.FANCY_CROP_RATIO_LAND),
                            (int) (scaledWidth * FancyHelper.FANCY_CROP_RATIO));
                    if (mMediaType != MediaType.VIDEO) {
                        scaledHeight = Math.round((float) scaledWidth
                                / FancyHelper.FANCY_CROP_RATIO_CAMERA);
                    }
                } else {
                    scaledWidth = slotWidth;
                    // scaledWidth and scaledHeight must meet 5:2 or 2:5
                    scaledHeight = Utils.clamp(imageHeight * scaledWidth / imageWidth,
                            (int) (scaledWidth * FancyHelper.FANCY_CROP_RATIO_LAND),
                            (int) (scaledWidth * FancyHelper.FANCY_CROP_RATIO));
                }
            }
        }
    }

    // use this Array to collect all slots
    public ArrayList mSlotEntry = new ArrayList<SlotEntry>();
    private AbstractGalleryActivity mActivity = null;

    public static final int COL_NUM = 2;
    public static final int LAND = 0;
    public static final int PORT = 1;

    public ArrayList <Layout> mLayoutArray = new ArrayList();
    private Spec mDefaultLayoutSpec;

    private int mBackUpSlotCount = -1;
    private Layout mSwitchFromLayout;
    private GalleryActionBar mActionBar;

    /// M: [FEATURE.ADD] Multi-window. @{
    private Spec mMultiWindowLayoutSpec;

    /**
     * Set SlotView spec of multi-window mode.
     * @param spec spec
     */
    public void setMultiWindowSpec(Spec spec) {
        mMultiWindowLayoutSpec = spec;
    }
    /// @}

    public void setActionBar(GalleryActionBar actionBar) {
        mActionBar = actionBar;
    }



    
}
