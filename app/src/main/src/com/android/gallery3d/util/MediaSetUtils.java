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

package com.android.gallery3d.util;

import android.os.Environment;

import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class MediaSetUtils {
    public static final Comparator<MediaSet> NAME_COMPARATOR = new NameComparator();

    //        public static final int CAMERA_BUCKET_ID               = GalleryUtils.getBucketId(
    //            Environment.getExternalStorageDirectory().toString() + "/"
    //                    + BucketNames.CAMERA);
    public static final int CAMERA_BUCKET_ID = GalleryUtils.getBucketId(getCameraPath());
    public static final int DOWNLOAD_BUCKET_ID = GalleryUtils.getBucketId(
            Environment.getExternalStorageDirectory().toString() + "/"
                    + BucketNames.DOWNLOAD);
    public static final int EDITED_ONLINE_PHOTOS_BUCKET_ID = GalleryUtils.getBucketId(
            Environment.getExternalStorageDirectory().toString() + "/"
                    + BucketNames.EDITED_ONLINE_PHOTOS);
    public static final int IMPORTED_BUCKET_ID = GalleryUtils.getBucketId(
            Environment.getExternalStorageDirectory().toString() + "/"
                    + BucketNames.IMPORTED);
    public static final int SNAPSHOT_BUCKET_ID = GalleryUtils.getBucketId(
            Environment.getExternalStorageDirectory().toString() +
                    "/" + BucketNames.SCREENSHOTS);

    //*/ Added by droi Linguanrong for flock save image, 16-5-23
    public static final int FLOCK_BUCKET_ID = GalleryUtils.getBucketId(
            Environment.getExternalStorageDirectory().toString() + "/"
                    + BucketNames.FLOCK);

    public static final int FLOCK_DOWNLOAD_BUCKET_ID = GalleryUtils.getBucketId(
            Environment.getExternalStorageDirectory().toString() + "/"
                    + BucketNames.FLOCK_DOWNLOAD);
    //*/

    public static final int CLOUD_BUCKET_ID = GalleryUtils.getBucketId(
            Environment.getExternalStorageDirectory().toString() + "/"
                    + BucketNames.CLOUD);

    private static final Path[] CAMERA_PATHS = {
            Path.fromString("/local/all/" + CAMERA_BUCKET_ID),
            Path.fromString("/local/image/" + CAMERA_BUCKET_ID),
            Path.fromString("/local/video/" + CAMERA_BUCKET_ID)
    };
    //*/ Added by droi Linguanrong for freeme gallery, 16-2-1
    private static final String STORAGE_PATH_SD1 = "/storage/sdcard0";
    private static final String STORAGE_PATH_SD2 = "/storage/sdcard1";
    private static final String STORAGE_PATH_EMULATED = "/storage/emulated/";
    private static final String STORAGE_PATH_SHARE_SD = "/storage/emulated/0";
    private static final String STORAGE_PATH_SD1_ICS = "/mnt/sdcard";
    private static final String STORAGE_PATH_SD2_ICS = "/mnt/sdcard2";
    private static Map<Integer, String> paths = new HashMap<Integer, String>();

    static {
        paths.put(GalleryUtils.getBucketId(
                STORAGE_PATH_SD1 + "/"
                        + BucketNames.CAMERA), STORAGE_PATH_SD1);
        paths.put(GalleryUtils.getBucketId(
                STORAGE_PATH_SD2 + "/"
                        + BucketNames.CAMERA), STORAGE_PATH_SD2);

        paths.put(GalleryUtils.getBucketId(
                STORAGE_PATH_EMULATED + "/"
                        + BucketNames.CAMERA), STORAGE_PATH_EMULATED);

        paths.put(GalleryUtils.getBucketId(
                STORAGE_PATH_SHARE_SD + "/"
                        + BucketNames.CAMERA), STORAGE_PATH_SHARE_SD);

        paths.put(GalleryUtils.getBucketId(
                STORAGE_PATH_SD1_ICS + "/"
                        + BucketNames.CAMERA), STORAGE_PATH_SD1_ICS);

        paths.put(GalleryUtils.getBucketId(
                STORAGE_PATH_SD2_ICS + "/"
                        + BucketNames.CAMERA), STORAGE_PATH_SD2_ICS);

    }

    public static boolean isCameraSource(Path path) {
        return CAMERA_PATHS[0] == path || CAMERA_PATHS[1] == path
                || CAMERA_PATHS[2] == path;
    }

    public static boolean isCameraPath(int bucketId) {
        return paths.containsKey(bucketId);
    }

    private static String getCameraPath() {
        String pathTmp = null;
        String pathCamera = Environment.getExternalStorageDirectory().toString() + "/"
                + BucketNames.CAMERA;

        File file = new File(pathCamera);
        if (!file.exists()) {
            pathTmp = containsAndroFolder();
            if (pathTmp == null) {
                file.mkdirs();
                return pathCamera;
            }
        } else {
            return pathCamera;
        }

        return pathTmp;
    }

    private static String containsAndroFolder() {
        String path = Environment.getExternalStorageDirectory().toString() + "/"
                + BucketNames.ANDRO;

        File file = new File(path);
        if (file.exists()) {
            return path;
        }
        return null;
    }

    // Sort MediaSets by name
    public static class NameComparator implements Comparator<MediaSet> {
        @Override
        public int compare(MediaSet set1, MediaSet set2) {
            int result = set1.getName().compareToIgnoreCase(set2.getName());
            if (result != 0) {
                return result;
            }
            return set1.getPath().toString().compareTo(set2.getPath().toString());
        }
    }
    //*/
}
