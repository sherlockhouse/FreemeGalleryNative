package com.freeme.elementscenter;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public final class ECOfflineUtil {
    public final static int    WATERWARK_TYPE_CODE    = 100001;
    public final static int    CHILDMODE_TYPE_CODE    = 100101;
    public final static int    POSE_TYPE_CODE         = 100201;
    public final static int    JIGSAW_TYPE_CODE       = 100301;
    public static final String WATERWARK_PATH         = "/.ElementsCenter/WaterMark/";
    public static final String CHILDMODE_PATH         = "/.ElementsCenter/ChildMode/";
    public static final String POSEMODE_PATH          = "/.ElementsCenter/PoseMode/";
    public static final String JIGSAW_PATH            = "/.ElementsCenter/JigSaw/";
    public static final String SEPARATOR              = "_";
    public static final String INFO_FILE_ID           = "info";
    public static final String PICTURE_SUFFIX         = ".png";
    public static final String PICTURE_SUFFIX_DOC9    = ".9.png";
    public static final String AUDIO_SUFFIX           = ".ogg";
    public static final String THUMBNAIL_FILE_ID      = "thumbnail";
    public static final String PRIMITIVE_FILE_ID      = "primitive";
    public static final String ITEM_STATUS_NEW_FILE   = "status_new";
    public static final String WATERWARK_TYPE_ARRAY[] = {
            "travel", "food", "catchword", "regards", "selfie", "mood"
    };

    public static final String POSEMODE_TYPE_ARRAY[] = {
            "male", "female", "family"
    };

    public static final String[] TYPE_ARRAY = {
            "watermark", "childmode", "posemode", "jigsaw",
    };

    public static void deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            file.delete();
        }
    }

    public static String utf8UrlDecode(String text) {
        String result = "";
        int p = 0;
        if (text != null && text.length() > 0) {
            text = text.toLowerCase();
            p = text.indexOf("%e");
            if (p == -1) return text;
            while (p != -1) {
                result += text.substring(0, p);
                text = text.substring(p, text.length());
                if (text == "" || text.length() < 9) return result;
                result += codeToWord(text.substring(0, 9));
                text = text.substring(9, text.length());
                p = text.indexOf("%e");
            }
        }
        return result + text;
    }

    private static String codeToWord(String text) {
        String result;
        if (utf8CodeCheck(text)) {
            byte[] code = new byte[3];
            code[0] = (byte) (Integer.parseInt(text.substring(1, 3), 16) - 256);
            code[1] = (byte) (Integer.parseInt(text.substring(4, 6), 16) - 256);
            code[2] = (byte) (Integer.parseInt(text.substring(7, 9), 16) - 256);
            try {
                result = new String(code, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                result = null;
            }
        } else {
            result = text;
        }
        return result;
    }

    private static boolean utf8CodeCheck(String text) {
        String sign = "";
        if (text.startsWith("%e")) for (int i = 0, p = 0; p != -1; i++) {
            p = text.indexOf("%", p);
            if (p != -1) p++;
            sign += p;
        }
        return sign.equals("147-1");
    }

    public static boolean isUtf8Url(String text) {
        text = text.toLowerCase();
        int p = text.indexOf("%");
        if (p != -1 && text.length() - p > 9) {
            text = text.substring(p, p + 9);
        }
        return utf8CodeCheck(text);
    }

    public static List<ECOfflineItemData> getOfflineItemDataByType(int typeCode, int pageType) {
        ArrayList<ECOfflineItemData> retItemList = new ArrayList<ECOfflineItemData>();
        List<String> retList = getFileNameAndPageType(typeCode, pageType);
        String fileRoot = retList.get(0);
        String pageTypeCode = retList.get(1);
        String thumbnailFileName = retList.get(2);
        String primitiveFileName = retList.get(3);
        String infoFileName = retList.get(4);
        File root = new File(fileRoot);
        File[] files = root.listFiles();
        if (files == null) {
            return retItemList;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                String fileName = file.getName();
                String[] split = fileName.split(SEPARATOR);
                String pageTypeName = split[1];
                if (pageTypeName.equals(pageTypeCode)) {
                    boolean isExist = isFileExist(file.getAbsolutePath() + "/" + thumbnailFileName);
                    if (isExist) {
                        isExist = isFileExist(file.getAbsolutePath() + "/" + primitiveFileName);
                    }
                    if (isExist) {
                        String name = split[0];
                        String thumbnaiFullName = file.getAbsolutePath() + "/" + thumbnailFileName;
                        String primitiveFullName = file.getAbsolutePath() + "/" + primitiveFileName;
                        String itemNewStatusFileFullName = file.getAbsolutePath() + "/"
                                + ITEM_STATUS_NEW_FILE;
                        ECOfflineItemData item = new ECOfflineItemData();
                        File thumbnailFile = new File(thumbnaiFullName);
                        File primitiveFile = new File(primitiveFullName);
                        URL thumbnailUrl = null;
                        URL primitiveUrl = null;
                        String tUrlStr = "";
                        String pUrlStr = "";
                        try {
                            thumbnailUrl = thumbnailFile.toURL();
                            primitiveUrl = primitiveFile.toURL();
                        } catch (MalformedURLException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        if (thumbnailUrl != null) {
                            tUrlStr = utf8UrlEncode(thumbnailUrl.toString());
                        }

                        if (primitiveUrl != null) {
                            pUrlStr = utf8UrlEncode(primitiveUrl.toString());
                        }
                        item.mName = name;
                        item.mType = typeCode;
                        item.mPageType = pageType;
                        item.mPrimitiveUrl = pUrlStr;
                        item.mThumbnailUrl = tUrlStr;
                        item.mItemNewStatusFileFullName = itemNewStatusFileFullName;
                        String infoFullName = file.getAbsolutePath() + "/" + infoFileName;
                        readInfoFromFile(infoFullName, item);
                        retItemList.add(item);
                    }
                }
            }
        }
        return retItemList;
    }

    public static List<String> getFileNameAndPageType(int typeCode, int pageType) {
        ArrayList<String> retList = new ArrayList<String>();
        String fileRoot = "";
        String pageTypeCode = "";
        String thumbnailFileName = "";
        String primitiveFileName = "";
        String infoFileName = INFO_FILE_ID;
        switch (typeCode) {
            case WATERWARK_TYPE_CODE:
                fileRoot = getSDPath() + WATERWARK_PATH;
                pageTypeCode = WATERWARK_TYPE_ARRAY[pageType];
                if (pageTypeCode.equals("selfie")) {
                    primitiveFileName = PRIMITIVE_FILE_ID + PICTURE_SUFFIX_DOC9;
                } else {
                    primitiveFileName = PRIMITIVE_FILE_ID + PICTURE_SUFFIX;
                }
                thumbnailFileName = THUMBNAIL_FILE_ID + PICTURE_SUFFIX;
                break;
            case CHILDMODE_TYPE_CODE:
                fileRoot = getSDPath() + CHILDMODE_PATH;
                pageTypeCode = "childmode";
                thumbnailFileName = THUMBNAIL_FILE_ID + PICTURE_SUFFIX;
                primitiveFileName = PRIMITIVE_FILE_ID + AUDIO_SUFFIX;
                break;
            case POSE_TYPE_CODE:
                fileRoot = getSDPath() + POSEMODE_PATH;
                pageTypeCode = POSEMODE_TYPE_ARRAY[pageType];
                thumbnailFileName = THUMBNAIL_FILE_ID + PICTURE_SUFFIX;
                primitiveFileName = PRIMITIVE_FILE_ID + PICTURE_SUFFIX;
                break;
            case JIGSAW_TYPE_CODE:
                fileRoot = getSDPath() + JIGSAW_PATH;
                pageTypeCode = "jigsaw";
                thumbnailFileName = THUMBNAIL_FILE_ID + PICTURE_SUFFIX;
                primitiveFileName = PRIMITIVE_FILE_ID + PICTURE_SUFFIX;
                break;
            default:
                break;
        }

        retList.add(fileRoot);
        retList.add(pageTypeCode);
        retList.add(thumbnailFileName);
        retList.add(primitiveFileName);
        retList.add(infoFileName);
        return retList;
    }

    public static boolean isFileExist(String fileName) {
        boolean isExist = false;
        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            isExist = true;
        }
        return isExist;
    }

    public static String utf8UrlEncode(String text) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0 && c <= 255) {
                result.append(c);
            } else {
                byte[] b = new byte[0];
                try {
                    b = Character.toString(c).getBytes("UTF-8");
                } catch (Exception ex) {
                }
                for (int j = 0; j < b.length; j++) {
                    int k = b[j];
                    if (k < 0) k += 256;
                    result.append("%" + Integer.toHexString(k).toUpperCase());
                }
            }
        }
        return result.toString();
    }

    public static boolean readInfoFromFile(String fileName, ECOfflineItemData info) {
        boolean isOk = false;
        File file = new File(fileName);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            info.mPrompt = reader.readLine();
            String colorStr = reader.readLine();
            info.mColor = Integer.parseInt(colorStr);
            reader.close();
            isOk = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            info.mColor = -1;
            e.printStackTrace();
        }
        return isOk;
    }

    public static String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();
        } else {
            return null;
        }
        return sdDir.toString();
    }
}
