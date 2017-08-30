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

package com.android.gallery3d.data;

import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.UriMatcher;
import android.net.Uri;

import com.freeme.data.StoryAlbum;
import com.freeme.data.StoryAlbumSet;
import com.freeme.data.StoryMergeAlbum;
import com.freeme.data.VisitorAlbum;
import com.freeme.data.VisitorAlbumVideo;
import com.freeme.gallery.app.GalleryActivity;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.MediaSet.ItemConsumer;
import com.freeme.provider.GalleryStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

class LocalSource extends MediaSource {

    public static final String KEY_BUCKET_ID = "bucketId";

    private GalleryApp mApplication;
    private PathMatcher mMatcher;
    private static final int NO_MATCH = -1;
    private final UriMatcher mUriMatcher = new UriMatcher(NO_MATCH);
    public static final Comparator<PathId> sIdComparator = new IdComparator();

    private static final int LOCAL_IMAGE_ALBUMSET = 0;
    private static final int LOCAL_VIDEO_ALBUMSET = 1;
    private static final int LOCAL_IMAGE_ALBUM = 2;
    private static final int LOCAL_VIDEO_ALBUM = 3;
    private static final int LOCAL_IMAGE_ITEM = 4;
    private static final int LOCAL_VIDEO_ITEM = 5;
    private static final int LOCAL_ALL_ALBUMSET = 6;
    private static final int LOCAL_ALL_ALBUM = 7;
    //*/ Added by Tyd Linguanrong for secret photos, 2014-2-17
    private static final int LOCAL_VISITOR_ALBUM = 8;
    private static final int LOCAL_VISITOR_VIDEO = 9;
    //*/ Added by Linguanrong for story album, 2015-4-7
    private static final int LOCAL_STORY_ALBUMSET = 10;
    private static final int LOCAL_STORY_ALBUM    = 11;
    private static final int LOCAL_ALL_ALBUM_CAMERA      = 12;
    private static final String TAG = "Gallery2/LocalSource";

    private ContentProviderClient mClient;

    public LocalSource(GalleryApp context) {
        super("local");
        mApplication = context;
        mMatcher = new PathMatcher();
        mMatcher.add("/local/image", LOCAL_IMAGE_ALBUMSET);
        mMatcher.add("/local/video", LOCAL_VIDEO_ALBUMSET);
        mMatcher.add("/local/all", LOCAL_ALL_ALBUMSET);

        //*/ Added by Linguanrong for story album, 2015-4-7
        mMatcher.add(StoryAlbumSet.PATH.toString(), LOCAL_STORY_ALBUMSET);
        mMatcher.add(StoryAlbumSet.PATH_ALL, LOCAL_STORY_ALBUM);
        //*/

        mMatcher.add("/local/image/*", LOCAL_IMAGE_ALBUM);
        mMatcher.add("/local/video/*", LOCAL_VIDEO_ALBUM);
        mMatcher.add("/local/all/*", LOCAL_ALL_ALBUM);
        mMatcher.add("/local/camera/*", LOCAL_ALL_ALBUM_CAMERA);
        mMatcher.add("/local/image/item/*", LOCAL_IMAGE_ITEM);
        mMatcher.add("/local/video/item/*", LOCAL_VIDEO_ITEM);

        //*/ Added by Tyd Linguanrong for secret photos, 2014-2-17
        mMatcher.add(VisitorAlbum.PATH.toString(), LOCAL_VISITOR_ALBUM);
        mMatcher.add(VisitorAlbumVideo.PATH.toString(), LOCAL_VISITOR_VIDEO);
        //*/

        mUriMatcher.addURI(GalleryStore.AUTHORITY,
                "external/images/media/#", LOCAL_IMAGE_ITEM);
        mUriMatcher.addURI(GalleryStore.AUTHORITY,
                "external/video/media/#", LOCAL_VIDEO_ITEM);
        mUriMatcher.addURI(GalleryStore.AUTHORITY,
                "external/images/media", LOCAL_IMAGE_ALBUM);
        mUriMatcher.addURI(GalleryStore.AUTHORITY,
                "external/video/media", LOCAL_VIDEO_ALBUM);
        mUriMatcher.addURI(GalleryStore.AUTHORITY,
                "external/file", LOCAL_ALL_ALBUM);
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        GalleryApp app = mApplication;
        switch (mMatcher.match(path)) {
            case LOCAL_ALL_ALBUMSET:
            case LOCAL_IMAGE_ALBUMSET:
            case LOCAL_VIDEO_ALBUMSET:
                return new LocalAlbumSet(path, mApplication);
            case LOCAL_IMAGE_ALBUM:
                return new LocalAlbum(path, app, mMatcher.getIntVar(0), true);
            case LOCAL_VIDEO_ALBUM:
                return new LocalAlbum(path, app, mMatcher.getIntVar(0), false);
            case LOCAL_ALL_ALBUM: {
                int bucketId = mMatcher.getIntVar(0);
                DataManager dataManager = app.getDataManager();
                MediaSet imageSet = (MediaSet) dataManager.getMediaObject(
                        LocalAlbumSet.PATH_IMAGE.getChild(bucketId));
                MediaSet videoSet = (MediaSet) dataManager.getMediaObject(
                        LocalAlbumSet.PATH_VIDEO.getChild(bucketId));
                Comparator<MediaItem> comp = DataManager.sDateTakenComparator;
                return new LocalMergeAlbum(
                        path, comp, new MediaSet[]{imageSet,videoSet}, bucketId);
            }
            case LOCAL_ALL_ALBUM_CAMERA:{
                String  var = mMatcher.getVar(0);
                String[] paths = Path.splitSequence(var);
                MediaSet[] mediaSet = new MediaSet[paths.length];
                for (int i = 0; i < paths.length; i++) {
                    String tempPath = paths[i];
                    //nt bucketId = Integer.valueOf(tempPath);
                    Path localPath = Path.fromString(tempPath);
                    MediaSet set = (MediaSet) this.createMediaObject(localPath);
                    mediaSet[i] = set;
                }
                Comparator<MediaItem> comp = DataManager.sDateTakenComparator;
                return new LocalMergeAlbum(
                        path, comp,mediaSet, -1);
            }
            case LOCAL_IMAGE_ITEM:
                return new LocalImage(path, mApplication, mMatcher.getIntVar(0));
            case LOCAL_VIDEO_ITEM:
                return new LocalVideo(path, mApplication, mMatcher.getIntVar(0));

            //*/ Added by Tyd Linguanrong for secret photos, 2014-2-17
            case LOCAL_VISITOR_ALBUM:
                return new VisitorAlbum(path, mApplication);

            case LOCAL_VISITOR_VIDEO:
                return new VisitorAlbumVideo(path, mApplication);
            //*/

            //*/ Added by Linguanrong for story album, 2015-4-7
            case LOCAL_STORY_ALBUMSET:
                return new StoryAlbumSet(path, mApplication);

            case LOCAL_STORY_ALBUM: {
                int storyId = mMatcher.getIntVar(0);
                DataManager dataManager = app.getDataManager();
                Comparator<MediaItem> comp = DataManager.sDateTakenComparator;
                return new StoryMergeAlbum(mApplication, path, comp, new MediaSet[]{
                        getStoryAlbum(dataManager, MEDIA_TYPE_IMAGE, StoryAlbumSet.PATH_IMAGE, storyId, ""),
                        getStoryAlbum(dataManager, MEDIA_TYPE_VIDEO, StoryAlbumSet.PATH_VIDEO, storyId, "")},
                        storyId, "");
            }
            //*/

            default:
                throw new RuntimeException("bad path: " + path);
        }
    }

    private static int getMediaType(String type, int defaultType) {
        if (type == null) return defaultType;
        try {
            int value = Integer.parseInt(type);
            if ((value & (MEDIA_TYPE_IMAGE
                    | MEDIA_TYPE_VIDEO)) != 0) return value;
        } catch (NumberFormatException e) {
            Log.w(TAG, "invalid type: " + type, e);
        }
        return defaultType;
    }

    // The media type bit passed by the intent
    private static final int MEDIA_TYPE_ALL = 0;
    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 4;

    private Path getAlbumPath(Uri uri, int defaultType) {
        int mediaType = getMediaType(
                uri.getQueryParameter(GalleryActivity.KEY_MEDIA_TYPES),
                defaultType);
        String bucketId = uri.getQueryParameter(KEY_BUCKET_ID);
        int id = 0;
        try {
            id = Integer.parseInt(bucketId);
        } catch (NumberFormatException e) {
            Log.w(TAG, "invalid bucket id: " + bucketId, e);
            return null;
        }
        switch (mediaType) {
            case MEDIA_TYPE_IMAGE:
                return Path.fromString("/local/image").getChild(id);
            case MEDIA_TYPE_VIDEO:
                return Path.fromString("/local/video").getChild(id);
            default:
                return Path.fromString("/local/all").getChild(id);
        }
    }

    @Override
    public Path findPathByUri(Uri uri, String type) {
        try {
            switch (mUriMatcher.match(uri)) {
                case LOCAL_IMAGE_ITEM: {
                    long id = ContentUris.parseId(uri);
                    return id >= 0 ? LocalImage.ITEM_PATH.getChild(id) : null;
                }
                case LOCAL_VIDEO_ITEM: {
                    long id = ContentUris.parseId(uri);
                    return id >= 0 ? LocalVideo.ITEM_PATH.getChild(id) : null;
                }
                case LOCAL_IMAGE_ALBUM: {
                    return getAlbumPath(uri, MEDIA_TYPE_IMAGE);
                }
                case LOCAL_VIDEO_ALBUM: {
                    return getAlbumPath(uri, MEDIA_TYPE_VIDEO);
                }
                case LOCAL_ALL_ALBUM: {
                    return getAlbumPath(uri, MEDIA_TYPE_ALL);
                }
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "uri: " + uri.toString(), e);
        }
        return null;
    }

    @Override
    public Path getDefaultSetOf(Path item) {
        MediaObject object = mApplication.getDataManager().getMediaObject(item);
        if (object instanceof LocalMediaItem) {
            return Path.fromString("/local/all").getChild(
                    String.valueOf(((LocalMediaItem) object).getBucketId()));
        }
        return null;
    }

    @Override
    public void mapMediaItems(ArrayList<PathId> list, ItemConsumer consumer) {
        ArrayList<PathId> imageList = new ArrayList<PathId>();
        ArrayList<PathId> videoList = new ArrayList<PathId>();
        int n = list.size();
        for (int i = 0; i < n; i++) {
            PathId pid = list.get(i);
            // We assume the form is: "/local/{image,video}/item/#"
            // We don't use mMatcher for efficiency's reason.
            Path parent = pid.path.getParent();
            if (parent == LocalImage.ITEM_PATH) {
                imageList.add(pid);
            } else if (parent == LocalVideo.ITEM_PATH) {
                videoList.add(pid);
            }
        }
        // TODO: use "files" table so we can merge the two cases.
        processMapMediaItems(imageList, consumer, true);
        processMapMediaItems(videoList, consumer, false);
    }

    private void processMapMediaItems(ArrayList<PathId> list,
            ItemConsumer consumer, boolean isImage) {
        // Sort path by path id
        Collections.sort(list, sIdComparator);
        int n = list.size();
        for (int i = 0; i < n; ) {
            PathId pid = list.get(i);

            // Find a range of items.
            ArrayList<Integer> ids = new ArrayList<Integer>();
            int startId = Integer.parseInt(pid.path.getSuffix());
            ids.add(startId);

            int j;
            for (j = i + 1; j < n; j++) {
                PathId pid2 = list.get(j);
                int curId = Integer.parseInt(pid2.path.getSuffix());
                if (curId - startId >= MediaSet.MEDIAITEM_BATCH_FETCH_COUNT) {
                    break;
                }
                ids.add(curId);
            }

            MediaItem[] items = LocalAlbum.getMediaItemById(
                    mApplication, isImage, ids);
            for(int k = i ; k < j; k++) {
                PathId pid2 = list.get(k);
                consumer.consume(pid2.id, items[k - i]);
            }

            i = j;
        }
    }

    //*/ Added by Linguanrong for story album, 2015-4-7
    private MediaSet getStoryAlbum(
            DataManager manager, int type, Path parent, int story, String name) {
        synchronized (DataManager.LOCK) {
            Path path = parent.getChild(story);
            MediaObject object = manager.peekMediaObject(path);
            if (object != null) return (MediaSet) object;
            switch (type) {
                case MEDIA_TYPE_IMAGE:
                    return new StoryAlbum(path, mApplication, true, story, name);
                case MEDIA_TYPE_VIDEO:
                    return new StoryAlbum(path, mApplication, false, story, name);
            }
            throw new IllegalArgumentException(String.valueOf(type));
        }
    }



    // This is a comparator which compares the suffix number in two Paths.
    private static class IdComparator implements Comparator<PathId> {
        @Override
        public int compare(PathId p1, PathId p2) {
            String s1 = p1.path.getSuffix();
            String s2 = p2.path.getSuffix();
            int len1 = s1.length();
            int len2 = s2.length();
            if (len1 < len2) {
                return -1;
            } else if (len1 > len2) {
                return 1;
            } else {
                return s1.compareTo(s2);
            }
        }
    }
    //*/

    @Override
    public void resume() {
        mClient = mApplication.getContentResolver()
                .acquireContentProviderClient(GalleryStore.AUTHORITY);
    }

    @Override
    public void pause() {
        mClient.release();
        mClient = null;
    }
}
