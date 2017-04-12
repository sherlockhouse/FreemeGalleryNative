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

package com.freeme.gallery.ui;

import android.graphics.Bitmap;
import android.os.Message;

import com.freeme.gallery.app.AbstractGalleryActivity;
import com.freeme.gallery.app.AlbumDataLoader;
import com.freeme.gallery.data.MediaItem;
import com.freeme.gallery.data.MediaObject;
import com.freeme.gallery.data.Path;
import com.freeme.gallery.glrenderer.Texture;
import com.freeme.gallery.glrenderer.TiledTexture;
import com.freeme.gallery.util.JobLimiter;
import com.freeme.gallerycommon.common.Utils;
import com.freeme.gallerycommon.util.Future;
import com.freeme.gallerycommon.util.FutureListener;
import com.freeme.ui.DateSlotView;

public class AlbumSlidingWindow implements AlbumDataLoader.DataListener {
    @SuppressWarnings("unused")
    private static final String TAG = "AlbumSlidingWindow";

    private static final int MSG_UPDATE_ENTRY = 0;
    private static final int JOB_LIMIT        = 2;
    private final AlbumDataLoader       mSource;
    private final AlbumEntry            mData[];
    private final SynchronizedHandler   mHandler;
    private final JobLimiter            mThreadPool;
    private final TiledTexture.Uploader mTileUploader;
    private int mSize;
    private int mContentStart = 0;
    private int mContentEnd   = 0;
    private int mActiveStart = 0;
    private int mActiveEnd   = 0;
    private Listener mListener;
    private int     mActiveRequestCount = 0;
    private boolean mIsActive           = false;
    //*/ Added by Tyd Linguanrong for Gallery new style, 2014-2-15
    private GLView mGLView;
    public AlbumSlidingWindow(AbstractGalleryActivity activity, GLView glView,
                              AlbumDataLoader source, int cacheSize) {
        source.setDataListener(this);
        mSource = source;
        mData = new AlbumEntry[cacheSize];
        mSize = source.size();
        mGLView = glView;

        mHandler = new SynchronizedHandler(activity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                Utils.assertTrue(message.what == MSG_UPDATE_ENTRY);
                ((ThumbnailLoader) message.obj).updateEntry();
            }
        };

        mThreadPool = new JobLimiter(activity.getThreadPool(), JOB_LIMIT);
        mTileUploader = new TiledTexture.Uploader(activity.getGLRoot());
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }
    //*/

    public AlbumEntry get(int slotIndex) {
        if (!isActiveSlot(slotIndex)) {
            Utils.fail("invalid slot: %s outsides (%s, %s)",
                    slotIndex, mActiveStart, mActiveEnd);
        }
        return mData[slotIndex % mData.length];
    }

    public boolean isActiveSlot(int slotIndex) {
        return slotIndex >= mActiveStart && slotIndex < mActiveEnd;
    }

    public void setActiveWindow(int start, int end) {
        //*/ Modified by Tyd Linguanrong for Gallery new style, 2014-2-15
        if (mGLView instanceof SlotView) {
            if (!(start <= end && end - start <= mData.length && end <= mSize)) {
                Utils.fail("%s, %s, %s, %s", start, end, mData.length, mSize);
            }
        }
        //*/
        AlbumEntry data[] = mData;

        mActiveStart = start;
        mActiveEnd = end;

        int contentStart = Utils.clamp((start + end) / 2 - data.length / 2,
                0, Math.max(0, mSize - data.length));
        int contentEnd = Math.min(contentStart + data.length, mSize);
        setContentWindow(contentStart, contentEnd);
        updateTextureUploadQueue();
        if (mIsActive) updateAllImageRequests();
    }

    private void setContentWindow(int contentStart, int contentEnd) {
        if (contentStart == mContentStart && contentEnd == mContentEnd) return;

        if (!mIsActive) {
            mContentStart = contentStart;
            mContentEnd = contentEnd;
            mSource.setActiveWindow(contentStart, contentEnd);
            return;
        }

        if (contentStart >= mContentEnd || mContentStart >= contentEnd) {
            for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
                freeSlotContent(i);
            }
            mSource.setActiveWindow(contentStart, contentEnd);
            for (int i = contentStart; i < contentEnd; ++i) {
                prepareSlotContent(i);
            }
        } else {
            for (int i = mContentStart; i < contentStart; ++i) {
                freeSlotContent(i);
            }
            for (int i = contentEnd, n = mContentEnd; i < n; ++i) {
                freeSlotContent(i);
            }
            mSource.setActiveWindow(contentStart, contentEnd);
            for (int i = contentStart, n = mContentStart; i < n; ++i) {
                prepareSlotContent(i);
            }
            for (int i = mContentEnd; i < contentEnd; ++i) {
                prepareSlotContent(i);
            }
        }

        mContentStart = contentStart;
        mContentEnd = contentEnd;
    }

    private void updateTextureUploadQueue() {
        if (!mIsActive) return;
        mTileUploader.clear();

        // add foreground textures
        for (int i = mActiveStart, n = mActiveEnd; i < n; ++i) {
            AlbumEntry entry = mData[i % mData.length];
            if (entry.bitmapTexture != null) {
                mTileUploader.addTexture(entry.bitmapTexture);
            }
        }

        // add background textures
        int range = Math.max(
                (mContentEnd - mActiveEnd), (mActiveStart - mContentStart));
        for (int i = 0; i < range; ++i) {
            uploadBgTextureInSlot(mActiveEnd + i);
            uploadBgTextureInSlot(mActiveStart - i - 1);
        }
    }

    private void updateAllImageRequests() {
        mActiveRequestCount = 0;
        for (int i = mActiveStart, n = mActiveEnd; i < n; ++i) {
            if (requestSlotImage(i)) ++mActiveRequestCount;
        }
        if (mActiveRequestCount == 0) {
            requestNonactiveImages();
        } else {
            cancelNonactiveImages();
        }
    }

    private void freeSlotContent(int slotIndex) {
        AlbumEntry data[] = mData;
        int index = slotIndex % data.length;
        AlbumEntry entry = data[index];
        if (entry.contentLoader != null) entry.contentLoader.recycle();
        if (entry.bitmapTexture != null) entry.bitmapTexture.recycle();
        data[index] = null;
    }

    private void prepareSlotContent(int slotIndex) {
        AlbumEntry entry = new AlbumEntry();
        MediaItem item = mSource.get(slotIndex); // item could be null;
        entry.item = item;
        entry.mediaType = (item == null)
                ? MediaItem.MEDIA_TYPE_UNKNOWN
                : entry.item.getMediaType();
        entry.path = (item == null) ? null : item.getPath();
        entry.rotation = (item == null) ? 0 : item.getRotation();
        entry.contentLoader = new ThumbnailLoader(slotIndex, entry.item);
        mData[slotIndex % mData.length] = entry;
    }

    private void uploadBgTextureInSlot(int index) {
        if (index < mContentEnd && index >= mContentStart) {
            AlbumEntry entry = mData[index % mData.length];
            if (entry.bitmapTexture != null) {
                mTileUploader.addTexture(entry.bitmapTexture);
            }
        }
    }

    // return whether the request is in progress or not
    private boolean requestSlotImage(int slotIndex) {
        if (slotIndex < mContentStart || slotIndex >= mContentEnd) return false;
        AlbumEntry entry = mData[slotIndex % mData.length];
        if (entry.content != null || entry.item == null) return false;

        // Set up the panorama callback
        entry.mPanoSupportListener = new PanoSupportListener(entry);
        entry.item.getPanoramaSupport(entry.mPanoSupportListener);

        entry.contentLoader.startLoad();
        return entry.contentLoader.isRequestInProgress();
    }

    // We would like to request non active slots in the following order:
    // Order:    8 6 4 2                   1 3 5 7
    //         |---------|---------------|---------|
    //                   |<-  active  ->|
    //         |<-------- cached range ----------->|
    private void requestNonactiveImages() {
        int range = Math.max(
                (mContentEnd - mActiveEnd), (mActiveStart - mContentStart));
        for (int i = 0; i < range; ++i) {
            requestSlotImage(mActiveEnd + i);
            requestSlotImage(mActiveStart - 1 - i);
        }
    }

    private void cancelNonactiveImages() {
        int range = Math.max(
                (mContentEnd - mActiveEnd), (mActiveStart - mContentStart));
        for (int i = 0; i < range; ++i) {
            cancelSlotImage(mActiveEnd + i);
            cancelSlotImage(mActiveStart - 1 - i);
        }
    }

    private void cancelSlotImage(int slotIndex) {
        if (slotIndex < mContentStart || slotIndex >= mContentEnd) return;
        AlbumEntry item = mData[slotIndex % mData.length];
        if (item.contentLoader != null) item.contentLoader.cancelLoad();
    }

    @Override
    public void onContentChanged(int index) {
        if (index >= mContentStart && index < mContentEnd && mIsActive) {
            freeSlotContent(index);
            prepareSlotContent(index);
            updateAllImageRequests();
            if (mListener != null && isActiveSlot(index)) {
                mListener.onContentChanged();
            }
        }
    }

    @Override
    public void onSizeChanged(int size) {
        if (mSize != size) {
            mSize = size;
            if (mListener != null) mListener.onSizeChanged(mSize);
            if (mContentEnd > mSize) mContentEnd = mSize;
            if (mActiveEnd > mSize) mActiveEnd = mSize;
        }
    }

    public void resume() {
        mIsActive = true;
        TiledTexture.prepareResources();
        for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
            prepareSlotContent(i);
        }
        updateAllImageRequests();
    }

    public void pause() {
        mIsActive = false;
        mTileUploader.clear();
        TiledTexture.freeResources();
        for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
            freeSlotContent(i);
        }
    }

    public interface Listener {
        void onSizeChanged(int size);

        void onContentChanged();
    }

    public static class AlbumEntry {
        public  MediaItem           item;
        public  Path                path;
        public  boolean             isPanorama;
        public  int                 rotation;
        public  int                 mediaType;
        public  boolean             isWaitDisplayed;
        public  TiledTexture        bitmapTexture;
        public  Texture             content;
        private BitmapLoader        contentLoader;
        private PanoSupportListener mPanoSupportListener;
    }

    private class PanoSupportListener implements MediaObject.PanoramaSupportCallback {
        public final AlbumEntry mEntry;

        public PanoSupportListener(AlbumEntry entry) {
            mEntry = entry;
        }

        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean isPanorama,
                                          boolean isPanorama360) {
            if (mEntry != null) mEntry.isPanorama = isPanorama;
        }
    }

    private class ThumbnailLoader extends BitmapLoader {
        private final int       mSlotIndex;
        private final MediaItem mItem;

        public ThumbnailLoader(int slotIndex, MediaItem item) {
            mSlotIndex = slotIndex;
            mItem = item;
        }

        @Override
        protected Future<Bitmap> submitBitmapTask(FutureListener<Bitmap> l) {
            //*/ Modified by Linguanrong for story album, 2015-08-04
            int thumbnailType = MediaItem.TYPE_MICROTHUMBNAIL;
            if (mGLView instanceof DateSlotView
                    && (((DateSlotView) mGLView).isSinglePhoto(mSlotIndex)
                    || ((DateSlotView) mGLView).isLargePhoto(mSlotIndex))) {
                thumbnailType = MediaItem.TYPE_THUMBNAIL;
            }

            return mThreadPool.submit(
                    mItem.requestImage(thumbnailType), this);
            //*/
        }

        @Override
        protected void onLoadComplete(Bitmap bitmap) {
            mHandler.obtainMessage(MSG_UPDATE_ENTRY, this).sendToTarget();
        }

        public void updateEntry() {
            Bitmap bitmap = getBitmap();
            if (bitmap == null) return; // error or recycled
            AlbumEntry entry = mData[mSlotIndex % mData.length];
            entry.bitmapTexture = new TiledTexture(bitmap);
            entry.content = entry.bitmapTexture;

            if (isActiveSlot(mSlotIndex)) {
                mTileUploader.addTexture(entry.bitmapTexture);
                --mActiveRequestCount;
                if (mActiveRequestCount == 0) requestNonactiveImages();
                if (mListener != null) mListener.onContentChanged();
            } else {
                mTileUploader.addTexture(entry.bitmapTexture);
            }
        }
    }
}
