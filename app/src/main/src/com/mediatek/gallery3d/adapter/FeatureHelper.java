package com.mediatek.gallery3d.adapter;

import android.content.ContentResolver;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;

import com.android.gallery3d.R;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.ResourceTexture;
import com.android.gallery3d.glrenderer.Texture;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallery3d.util.TraceHelper;
import com.mediatek.galleryframework.base.ThumbType;
import com.mediatek.perfservice.IPerfServiceWrapper;


import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;

import scott.freeme.com.mtkreflectlib.MTKStorageManager;
import scott.freeme.com.mtkreflectlib.MTKStorageManagerEx;

public class FeatureHelper {
    private static final String TAG = "MtkGallery2/FeatureHelper";

    public static final String EXTRA_ENABLE_VIDEO_LIST = "mediatek.intent.extra.ENABLE_VIDEO_LIST";
    private static final String CACHE_SUFFIX = "/Android/data/com.android.gallery3d/cache";

    // added for rendering sub-type overlay on micro-thumbnail
    private static ResourceTexture sRefocusOverlay = null;
    private static ResourceTexture sSlowMotionOverlay = null;
    private static ResourceTexture sRawOverlay = null;
    private static StorageManager sStorageManager = null;
    // Add for first launch performance.
    private static int BOOST_POLICY_TIME_OUT = 1500;

//    private static final HashMap<SupportOperation, Integer> sSpMap =
//            new HashMap<SupportOperation, Integer>();
//
//    static {
//        sSpMap.put(SupportOperation.DELETE, MediaObject.SUPPORT_DELETE);
//        sSpMap.put(SupportOperation.ROTATE, MediaObject.SUPPORT_ROTATE);
//        sSpMap.put(SupportOperation.SHARE, MediaObject.SUPPORT_SHARE);
//        sSpMap.put(SupportOperation.CROP, MediaObject.SUPPORT_CROP);
//        sSpMap.put(SupportOperation.SHOW_ON_MAP,
//                MediaObject.SUPPORT_SHOW_ON_MAP);
//        sSpMap.put(SupportOperation.SETAS, MediaObject.SUPPORT_SETAS);
//        sSpMap.put(SupportOperation.FULL_IMAGE, MediaObject.SUPPORT_FULL_IMAGE);
//        sSpMap.put(SupportOperation.PLAY, MediaObject.SUPPORT_PLAY);
//        sSpMap.put(SupportOperation.CACHE, MediaObject.SUPPORT_CACHE);
//        sSpMap.put(SupportOperation.EDIT, MediaObject.SUPPORT_EDIT);
//        sSpMap.put(SupportOperation.INFO, MediaObject.SUPPORT_INFO);
//        sSpMap.put(SupportOperation.TRIM, MediaObject.SUPPORT_TRIM);
//        sSpMap.put(SupportOperation.UNLOCK, MediaObject.SUPPORT_UNLOCK);
//        sSpMap.put(SupportOperation.BACK, MediaObject.SUPPORT_BACK);
//        sSpMap.put(SupportOperation.ACTION, MediaObject.SUPPORT_ACTION);
//        sSpMap.put(SupportOperation.CAMERA_SHORTCUT,
//                MediaObject.SUPPORT_CAMERA_SHORTCUT);
//        sSpMap.put(SupportOperation.MUTE, MediaObject.SUPPORT_MUTE);
//        sSpMap.put(SupportOperation.PRINT, MediaObject.SUPPORT_PRINT);
//        sSpMap.put(SupportOperation.EXPORT, MediaObject.SUPPORT_EXPORT);
//        sSpMap.put(SupportOperation.PROTECTION_INFO, MediaObject.SUPPORT_PROTECTION_INFO);
//    }
//
//    public static int mergeSupportOperations(int originSp,
//            ArrayList<SupportOperation> exSp,
//            ArrayList<SupportOperation> exNotSp) {
//        if (exSp != null && exSp.size() != 0) {
//            int size = exSp.size();
//            for (int i = 0; i < size; i++) {
//                originSp |= sSpMap.get(exSp.get(i));
//            }
//        }
//        if (exNotSp != null && exNotSp.size() != 0) {
//            int size = exNotSp.size();
//            for (int i = 0; i < size; i++) {
//                originSp &= ~sSpMap.get(exNotSp.get(i));
//            }
//        }
//        return originSp;
//    }
//
    public static ThumbType convertToThumbType(int type) {
        switch (type) {
        case MediaItem.TYPE_THUMBNAIL:
            return ThumbType.MIDDLE;
        case MediaItem.TYPE_MICROTHUMBNAIL:
            return ThumbType.MICRO;
//        case MediaItem.TYPE_FANCYTHUMBNAIL:
//            return ThumbType.FANCY;
//        case MediaItem.TYPE_HIGHQUALITYTHUMBNAIL:
//            return ThumbType.HIGHQUALITY;
        default:
            Log.e(TAG, "<covertToThumbType> not support type");
            assert (false);
            return null;
        }
    }
//
//    public static void drawMicroThumbOverLay(Context context, GLCanvas canvas,
//            int width, int height, MediaItem item) {
//        if (item == null) {
//            return;
//        }
//
//        renderMediaTypeOverlay(context, canvas, width, height, item
//                .getMediaData());
//    }
//
//    public static void renderMediaTypeOverlay(Context context, GLCanvas canvas,
//            int width, int height, MediaData data) {
//        ResourceTexture overlay = null;
//
//        // for Refocus
//        if (data.mediaType == MediaData.MediaType.DEPTH_IMAGE) {
//            if (sRefocusOverlay == null) {
//                sRefocusOverlay = new ResourceTexture(context,
//                        R.drawable.m_refocus_tile);
//            }
//            overlay = sRefocusOverlay;
//        }
//
//        // for slow motion
//        if (data.isSlowMotion) {
//            if (sSlowMotionOverlay == null) {
//                sSlowMotionOverlay = new ResourceTexture(context,
//                        R.drawable.m_ic_slowmotion_albumview);
//            }
//            overlay = sSlowMotionOverlay;
//        }
//
//        // for raw
//        if (data.mediaType == MediaData.MediaType.RAW) {
//            if (sRawOverlay == null) {
//                sRawOverlay = new ResourceTexture(context,
//                        R.drawable.m_ic_raw);
//            }
//            overlay = sRawOverlay;
//        }
//
//        if (overlay != null) {
//            int side = Math.min(width, height) / 5;
//            overlay.draw(canvas, side / 4, height - side * 5 / 4, side, side);
//        }
//
//        // <DRM>
//        if (data.mediaType == MediaData.MediaType.DRM) {
//            drawDrmLockicon(context, canvas, data.filePath, 0, 0, width, height, 1.0f);
//        }
//    }
//
//    public static void setExtBundle(AbstractGalleryActivity activity,
//            Intent intent, Bundle data, Path path) {
//        /// [Split up into camera and gallery] @{
//        data.putBoolean(PhotoPage.KEY_LAUNCH_FROM_CAMERA,
//                intent.getBooleanExtra(PhotoPage.KEY_LAUNCH_FROM_CAMERA, false));
//        /// @}
//        MediaObject object = activity.getDataManager().getMediaObject(path);
//        if (object instanceof MediaItem) {
//            MediaItem item = (MediaItem) object;
//            MediaData md = item.getMediaData();
//            /// M: [FEATURE.ADD] [Camera independent from Gallery] @{
//            // Add for launch from secure camera
//            if (intent.getExtras() != null
//                    && intent.getBooleanExtra(PhotoPage.IS_SECURE_CAMERA, false)
//                    && intent.getExtras().getSerializable(PhotoPage.SECURE_ALBUM) != null) {
//                data.putSerializable(PhotoPage.SECURE_ALBUM,
//                        intent.getExtras().getSerializable(PhotoPage.SECURE_ALBUM));
//                data.putString(PhotoPage.KEY_MEDIA_SET_PATH,
//                        intent.getStringExtra(PhotoPage.SECURE_PATH));
//                data.putBoolean(PhotoPage.IS_SECURE_CAMERA,
//                        intent.getBooleanExtra(PhotoPage.IS_SECURE_CAMERA, false));
//            }
//            /// @}
//        }
//    }

    public static Uri tryContentMediaUri(Context context, Uri uri) {
        if (null == uri) {
            return null;
        }

        String scheme = uri.getScheme();
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(MediaStore.AUTHORITY, "external/file/#", 1);
        if (uriMatcher.match(uri) == 1) {
            return getUriById(context, uri);
        }
        if (!ContentResolver.SCHEME_FILE.equals(scheme)) {
            return uri;
        } else {
            String path = uri.getPath();
            Log.d(TAG, "<tryContentMediaUri> for " + path);
            if (!new File(path).exists()) {
                return null;
            }
        }
        return getUriByPath(context, uri);
    }

    private static Uri getUriByPath(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            // for file kinds of uri, query media database
            cursor = Media.query(context.getContentResolver(),
                    MediaStore.Files.getContentUri("external"), new String[] {
                    Media._ID, Media.MIME_TYPE, Media.BUCKET_ID }, "_data=(?)",
                    new String[] { uri.getPath() }, null); // " bucket_id ASC, _id ASC");
            if (null != cursor && cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String mimeType = cursor.getString(1);
                String contentUri = null;
                Uri resultUri = null;
                if (mimeType == null) {
                    return null;
                } else {
                    if (mimeType.startsWith("image/")) {
                        contentUri = Media.getContentUri("external").toString();
                    } else if (mimeType.startsWith("video/")) {
                        contentUri = MediaStore.Video.Media.getContentUri("external").toString();
                    } else {
                        Log.i(TAG, "<getUriByPath> id = " + id + ", mimeType = " + mimeType
                                + ", not begin with image/ or video/, return uri " + uri);
                        return uri;
                    }
                }
                resultUri = Uri.parse(contentUri + "/" + id);
                Log.i(TAG, "<getUriByPath> got " + resultUri);
                return resultUri;
            } else {
                Log.w(TAG, "<getUriByPath> fail to convert " + uri);
                return uri;
            }
        } finally {
            if (null != cursor) {
                cursor.close();
                cursor = null;
            }
        }
    }

    private static Uri getUriById(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = Media.query(context.getContentResolver(),
                    MediaStore.Files.getContentUri("external"), new String[] {
                    Media._ID, Media.MIME_TYPE, Media.BUCKET_ID }, "_id=(?)",
                    new String[] { uri.getLastPathSegment() }, null);
            if (null != cursor && cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String mimeType = cursor.getString(1);
                String contentUri = null;
                Uri resultUri = null;
                if (mimeType.startsWith("image/")) {
                    contentUri = Media.getContentUri("external").toString();
                } else if (mimeType.startsWith("video/")) {
                    contentUri = MediaStore.Video.Media.getContentUri("external").toString();
                } else {
                    Log.i(TAG, "<getUriById> id = " + id + ", mimeType = " + mimeType
                            + ", not begin with image/ or video/, return uri " + uri);
                    return uri;
                }
                resultUri = Uri.parse(contentUri + "/" + id);
                Log.i(TAG, "<getUriById> got " + resultUri);
                return resultUri;
            } else {
                Log.w(TAG, "<getUriById> fail to convert " + uri);
                return uri;
            }
        } finally {
            if (null != cursor) {
                cursor.close();
                cursor = null;
            }
        }
    }
    public static File getExternalCacheDir(Context context) {
        if (context == null) {
            Log.e(TAG, "<getExternalCacheDir> context is null, return null");
            return null;
        }

        // get internal storage / phone storage
        // if volume is mounted && not external sd card && not usb otg,
        // we treat it as internal storage / phone storage
        if (sStorageManager == null) {

            sStorageManager = (StorageManager) context
                    .getSystemService(Context.STORAGE_SERVICE);
        }
        String[] volumes = MTKStorageManager.getVolumePaths(sStorageManager);
        String internalStoragePath = null;
        for (String str : volumes) {
            if (MTKStorageManagerEx.isExternalSDCard(str)) {
                Log.i(TAG, "<getExternalCacheDir> " + str + " isExternalSDCard");
                continue;
            }
            if (MTKStorageManagerEx.isUSBOTG(str)) {
                Log.i(TAG, "<getExternalCacheDir> " + str + " isUSBOTG");
                continue;
            }
            if (Environment.MEDIA_MOUNTED.equalsIgnoreCase(MTKStorageManager.getVolumeState(sStorageManager, str))) {
                internalStoragePath = str;
                Log.i(TAG, "<getExternalCacheDir> set " + str + " as internalStoragePath");
                break;
            }
        }
        if (internalStoragePath == null || internalStoragePath.equals("")) {
            Log.e(TAG, "<getExternalCacheDir> internalStoragePath is null, return null");
            return null;
        }

        // get cache directory on internal storage or phone storage
        String cachePath = internalStoragePath + CACHE_SUFFIX;
        Log.i(TAG, "<getExternalCacheDir> return external cache dir is " + cachePath);
        File result = new File(cachePath);
        if (result.exists()) {
            return result;
        }
        if (result.mkdirs()) {
            return result;
        }
        Log.e(TAG, "<getExternalCacheDir> Fail to create external cache dir, return null");
        return null;
    }
//
    public static String getDefaultPath() {
        String path = MTKStorageManagerEx.getDefaultPath();
        return path;
    }

    public static String getDefaultStorageState(Context context) {
        if (sStorageManager == null && context == null) {
            return null;
        }
        if (sStorageManager == null) {
            sStorageManager = (StorageManager) context
                    .getSystemService(Context.STORAGE_SERVICE);
        }
        String path = MTKStorageManagerEx.getDefaultPath();
        if (path == null) {
            return null;
        }
        String volumeState = MTKStorageManager.getVolumeState(sStorageManager, path);
        Log.v(TAG, "<getDefaultStorageState> default path = " + path
                + ", state = " + volumeState);
        return volumeState;
    }

    private static void drawRightBottom(GLCanvas canvas, Texture tex, int x,
            int y, int width, int height, float scale) {
        if (null == tex) {
            return;
        }
        int texWidth = (int) ((float) tex.getWidth() * scale);
        int texHeight = (int) ((float) tex.getHeight() * scale);
        tex.draw(canvas, x + width - texWidth, y + height - texHeight,
                texWidth, texHeight);
    }

    public static boolean isLocalUri(Uri uri) {
        if (uri == null) {
            return false;
        }
        boolean isLocal = ContentResolver.SCHEME_FILE.equals(uri.getScheme());
        isLocal |= ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())
                && MediaStore.AUTHORITY.equals(uri.getAuthority());
        return isLocal;
    }

    public static MediaDetails convertStringArrayToDetails(String[] array) {
        if (array == null || array.length < 1) {
            return null;
        }
        MediaDetails res = new MediaDetails();
        for (int i = 0; i < array.length; i++) {
            if (array[i] != null) {
                res.addDetail(i + 1, array[i]);
            }
        }
        return res;
    }


    // HW limitation @{
    private static final int JPEG_DECODE_LENGTH_MAX = 8192;

    public static boolean isJpegOutOfLimit(String mimeType, int width,
            int height) {
        if (mimeType.equals("image/jpeg")
                && (width > JPEG_DECODE_LENGTH_MAX || height > JPEG_DECODE_LENGTH_MAX)) {
            return true;
        }
        return false;
    }
    // @}





    public static void refreshResource(Context context) {
        if (sRefocusOverlay != null) {
            sRefocusOverlay.recycle();
        }
        if (sSlowMotionOverlay != null) {
            sSlowMotionOverlay.recycle();
        }
        sRefocusOverlay = new ResourceTexture(context, R.drawable.m_refocus_tile);
        sSlowMotionOverlay = new ResourceTexture(context, R.drawable.m_ic_slowmotion_albumview);
    }

    /// M: [BUG.ADD] read DNG EXIF details. @{
    public static int getOrientationFromExif(String filePath, InputStream is) {
        int orientation = 0;
        int rotation = 0;
        try {
            ExifInterface exif;
            if (filePath != null && !filePath.equals("")) {
                exif = new ExifInterface(filePath);
            } else if (null != is &&
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                exif = new ExifInterface(is);
            } else {
                Log.d(TAG, "<getOrientationFromExif> sdk version issue, return 0");
                return 0;
            }
            orientation =
                    exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,
                            android.media.ExifInterface.ORIENTATION_UNDEFINED);
        } catch (IOException e) {
            Log.e(TAG, "<getOrientationFromExif> IOException", e);
            return 0;
        }
        Log.d(TAG, "<getOrientationFromExif> exif orientation: " + orientation);
        switch (orientation) {
            case android.media.ExifInterface.ORIENTATION_NORMAL:
                rotation = 0;
                break;
            case android.media.ExifInterface.ORIENTATION_ROTATE_90:
                rotation = 90;
                break;
            case android.media.ExifInterface.ORIENTATION_ROTATE_180:
                rotation = 180;
                break;
            case android.media.ExifInterface.ORIENTATION_ROTATE_270:
                rotation = 270;
                break;
            default:
                rotation = 0;
                break;
        }
        Log.d(TAG, "<getOrientationFromExif> rotation: " + rotation);
        return rotation;
    }
    /// @}

    /**
     * Modify CPU boost policy for first launch performance.
     *
     * @param context
     *            GalleryActivity
     */
    public static void modifyBoostPolicy(Context context) {
        if (!isCacheFileExists(context)) {
            TraceHelper.traceBegin(">>>>FeatureHelper-perfService Enabled");
            Class<?> mPerfServiceWrapper = null;
            try {
                mPerfServiceWrapper = Class.forName("com.mediatek.perfservice.PerfServiceWrapper");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            IPerfServiceWrapper perfService = null;
            try {
                perfService = (IPerfServiceWrapper) mPerfServiceWrapper.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (perfService != null) {
                perfService.boostEnableTimeoutMs(IPerfServiceWrapper.SCN_GALLERY_BOOST,
                        BOOST_POLICY_TIME_OUT);
                Log.d(TAG, "<modifyBoostPolicy> perfService set Enabled");
            }
            TraceHelper.traceEnd();
        }
    }

    /**
     * Check if launch gallery for the first time by check cache exists.
     * @param context
     *            Application context
     * @return cache file exists status
     */
    public static boolean isCacheFileExists(Context context) {
        File cacheDir = getExternalCacheDir(context);
        if (cacheDir == null) {
            return false;
        }
        File[] fs = cacheDir.listFiles();
        if (fs == null || fs.length == 0) {
            return false;
        }
        for (File file : fs) {
            if (file.getName().endsWith("idx")) {
                Log.d(TAG, "<isCacheFileExists> File cache exists!");
                return true;
            }
        }
        return false;
    }
}
