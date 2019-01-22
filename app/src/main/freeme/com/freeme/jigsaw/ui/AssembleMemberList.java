/*
 * Class name: AssembleMemberList
 * 
 * Description: member list that make up jigsaw, it assembled
 *              to a picture by selecting item.
 *
 * Author: Theobald_wu, contact with wuqizhi@tydtech.com
 * 
 * Date: 2014/01   
 * 
 * Copyright (C) 2014 TYD Technology Co.,Ltd.
 * 
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freeme.jigsaw.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.freeme.elementscenter.ECOfflineItemData;
import com.freeme.elementscenter.ECOfflineUtil;
import com.freeme.gallery.R;
import com.freeme.jigsaw.app.JigsawEntry;
import com.freeme.jigsaw.util.AsyncImageCache;
import com.freeme.jigsaw.util.Helper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;

public class AssembleMemberList extends LinearLayout implements View.OnClickListener {
    private static final int[][] TEMPLATE_RESOURCE_IDS = {
            {
                    R.array.template_2_thumb_ic_array, R.array.template_2_array
            }, {
            R.array.template_3_thumb_ic_array, R.array.template_3_array
    }, {
            R.array.template_4_thumb_ic_array, R.array.template_4_array
    }, {
            R.array.template_5_thumb_ic_array, R.array.template_5_array
    }, {
            R.array.template_6_thumb_ic_array, R.array.template_6_array
    }
    };
    // set thumb icon and assemble elements, e.g
    // -------------------thumb icon -------assembles element----
    // template: -------R.drawable.xxx-------R.layout.xxx ----
    // background: -------R.drawable.xxx-------R.drawable.xxx ----
    private SparseIntArray mItems;
    // the type of list
    private Helper.jigsaw_element_type mType               = Helper.jigsaw_element_type.NONE;
    // the child count of template
    private int                        mTemplateChildCount = 0;
    private onSelectListener mListener;
    // mSelectedIndex[0] is the highlight index of template list,
    // mSelectedIndex[1] is the highlight index of background list.
    private int[] mSelectedIndex = new int[2];
    private AsyncImageCache   mAsyncImageCache;
    private int               mThumbnailW;
    private int               mThumbnailH;
    private Context           mContext;
    private SharedPreferences mSharedPref;
    private              View    mSelectedVoiceItem    = null;
    private              View    mSelectedVoiceItem1   = null;

    public AssembleMemberList(Context context) {
        super(context);
        mContext = context;
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public AssembleMemberList(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSelectedIndex[0] = 0;
        mSelectedIndex[1] = 0;
        mContext = context;
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void initialize(Helper.jigsaw_element_type type, int count) {
        mTemplateChildCount = count;
        mSelectedVoiceItem = null;
        mSelectedVoiceItem1 = null;
        // set default highlight index of template thumb list
        int def_template_layout = Helper.getDefaultTemplate(getContext(), count);
        int[] res = new int[2];

        if (getTemplateRes(res) >= 0) {
            TypedArray array = getContext().getResources().obtainTypedArray(res[1]);
            int n = array.length();
            for (int i = 0; i < n; ++i) {
                int layout = array.getResourceId(i, 0);
                if (layout == def_template_layout) {
                    mSelectedIndex[0] = i;
                    break;
                }
            }
            array.recycle();
        }

        // default thumb list
        makeUp(type);
    }

    /**
     * Get template resources, thumbs array id and layouts array id
     *
     * @param out : save result, out[0] is thumbs array id and out[1] is layouts
     *            array id.
     * @return -1 if arguments is not allowable, otherwise this is 0.
     */
    private int getTemplateRes(int[] out) {
        int result = -1;

        if (mTemplateChildCount >= 2 && mTemplateChildCount <= Helper.TEMPLATE_CHILD_MAX
                && out != null && out.length == 2) {
            out[0] = TEMPLATE_RESOURCE_IDS[mTemplateChildCount - 2][0];
            out[1] = TEMPLATE_RESOURCE_IDS[mTemplateChildCount - 2][1];
            result = 0;
        }

        return result;
    }

    /**
     * Make up thumb icon list and hold item that click used
     *
     * @param type : the type of list, template or background
     */
    public void makeUp(Helper.jigsaw_element_type type) {
        if (mType == type) {
            // avoid repeat make up the same list.
            return;
        }
        mType = type;
        int iconsRes = 0;
        int elementsRes = 0;

        if (Helper.jigsaw_element_type.BACKGROUND == mType) {
            iconsRes = R.array.jigsaw_bg_thumb_ic_array;
            elementsRes = R.array.jigsaw_background_array;
        } else if (Helper.jigsaw_element_type.TEMPLATE == mType) {
            int[] res = new int[2];

            if (getTemplateRes(res) >= 0) {
                iconsRes = res[0];
                elementsRes = res[1];
            } else {
                if (Helper.DEBUG) {
                    Log.i(Helper.TAG, "AssembleMemberList::makeUp(), getTemplateRes fail!");
                }
            }
        }

        Resources res = getContext().getResources();
        int[] icons = getIds(res, iconsRes);
        int[] elements = getIds(res, elementsRes);
        if (icons != null && elements != null && icons.length > 0
                && elements.length >= icons.length) {
            // load image resources to mItems
            if (mItems == null) {
                mItems = new SparseIntArray(icons.length);
            }

            if (mItems != null) {
                mItems.clear();

                for (int i = 0; i < icons.length; i++) {
                    mItems.put(icons[i], elements[i]);
                }

                loadThumbList();
            }
        } else {
            if (Helper.DEBUG) {
                Log.i(Helper.TAG, "AssembleMemberList::makeUp(), load resoures fail!");
            }
        }
    }

    // end

    /**
     * Get drawable id array from xml resoures
     *
     * @param res   : Resources
     * @param resId : id
     * @return
     */
    private int[] getIds(Resources res, int resId) {
        if (resId == 0) return null;

        TypedArray array = res.obtainTypedArray(resId);
        int n = array.length();
        int ids[] = new int[n];
        for (int i = 0; i < n; ++i) {
            ids[i] = array.getResourceId(i, 0);
        }
        array.recycle();

        return ids;
    }

    /**
     * Generate thumb list, to select assemble elements
     */
    private void loadThumbList() {
        // regenerate list
        removeAllViews();
        for (int i = 0; i < mItems.size(); i++) {
            RelativeLayout item = (RelativeLayout) LayoutInflater.from(getContext()).inflate(
                    R.layout.assemble_thumb_item, this, false);
            ImageView iv = (ImageView) item.findViewById(R.id.jigsaws);
            iv.setImageResource(mItems.keyAt(i));
            item.setId(mItems.keyAt(i));

            item.setOnClickListener(this);

//            if (i == mSelectedIndex[0] && mType == Helper.jigsaw_element_type.TEMPLATE
//                    || i == mSelectedIndex[1] && mType == Helper.jigsaw_element_type.BACKGROUND) {
//                // item.setSelected(true);
//            }
            seleted(item);
            if (mSelectedVoiceItem == null && mType == Helper.jigsaw_element_type.BACKGROUND) {
                item.setSelected(true);
                mSelectedVoiceItem = item;
            }
            if (mSelectedVoiceItem1 == null && mType == Helper.jigsaw_element_type.TEMPLATE) {
                item.setSelected(true);
                mSelectedVoiceItem1 = item;
            }
            addView(item);

        }
        // add mingjun
        if (mType == Helper.jigsaw_element_type.BACKGROUND) {
            addDynimBkImgSettingList();
        }
        // end
    }

    private void seleted(View item) {

        if (mSelectedVoiceItem != null && mSelectedVoiceItem.getId() == item.getId()) {
            Log.i("zhang", " mSelectedVoiceItem.getId()==" + mSelectedVoiceItem.getTag()
                    + "item.getId()===" + item.getTag());
            if (item.getId() == -1) {
                if (mSelectedVoiceItem.getTag().toString().equals(item.getTag().toString())) {
                    item.setSelected(true);
                    mSelectedVoiceItem = item;
                }
            } else {
                item.setSelected(true);
                mSelectedVoiceItem = item;
            }
        }
        if (mSelectedVoiceItem1 != null && mSelectedVoiceItem1.getId() == item.getId()) {
            item.setSelected(true);
            mSelectedVoiceItem1 = item;
        }
    }

    // add mingjun
    public void addDynimBkImgSettingList() {
        // removeAllViews();
        mAsyncImageCache = ((JigsawEntry) mContext).getAsyncImageCached();
        mThumbnailW = getContext().getResources().getDimensionPixelSize(
                R.dimen.ec_jigsaw_thumbnailc_w);
        mThumbnailH = getContext().getResources().getDimensionPixelSize(
                R.dimen.ec_jigsaw_thumbnailc_h);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        List<ECOfflineItemData> itemList = ECOfflineUtil.getOfflineItemDataByType(ECOfflineUtil.JIGSAW_TYPE_CODE,
                0);
        for (ECOfflineItemData itemData : itemList) {
            RelativeLayout item = (RelativeLayout) inflater.inflate(R.layout.assemble_thumb_item,
                    this, false);
            ImageView iv = (ImageView) item.findViewById(R.id.jigsaws);
            Bitmap ivImg = getDiskBitmap(itemData.mThumbnailUrl);
            // iv.setImageBitmap(ivImg);
            mAsyncImageCache.displayImage(iv, (Drawable) null, mThumbnailW, mThumbnailH,
                    new AsyncImageCache.NetworkImageGenerator(itemData.mThumbnailUrl,
                            itemData.mThumbnailUrl));
            ImageView in = (ImageView) item.findViewById(R.id.element_new);
            item.setTag(item.getId(), itemData.mPrimitiveUrl);
            String key = String.valueOf(itemData.mType) + itemData.mName;
            item.setTag(key);
            seleted(item);
            item.setOnClickListener(this);
            if (readOfflineNewStatus(key, false)) {
                in.setVisibility(View.VISIBLE);
                addView(item, 0);
            } else {
                addView(item);
            }

        }
    }

    // add mingjun
    public static Bitmap getDiskBitmap(String url) {
        Bitmap bitmap = null;
        InputStream in = null;
        BufferedOutputStream out = null;
        try {
            in = new BufferedInputStream(new URL(url).openStream(), 1024);
            final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
            out = new BufferedOutputStream(dataStream, 1024);
            copy(in, out);
            out.flush();
            byte[] data = dataStream.toByteArray();
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            data = null;
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // end

    private boolean readOfflineNewStatus(String key, boolean defaultVal) {
        return mSharedPref.getBoolean(key, defaultVal);
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] b = new byte[1024];
        int read;
        while ((read = in.read(b)) != -1) {
            out.write(b, 0, read);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        // enabled children
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setEnabled(enabled);
        }
    }

    // modified for mingjun
    @Override
    public void onClick(View v) {
        Bitmap bitmap;
        int element_res = -1;
        int old_highlight = -1;
        int highlight = -1;

        if (v.getTag(v.getId()) == null) {
            element_res = mItems.get(v.getId());
            if (Helper.DEBUG) {
                Log.i(Helper.TAG, "AssembleMemberList::onClick(), element_res = " + element_res);
            }

            highlight = mItems.indexOfValue(element_res);
            if (mType == Helper.jigsaw_element_type.TEMPLATE) {
//                old_highlight = mSelectedIndex[0];
//                mSelectedIndex[0] = highlight;
                if (mSelectedVoiceItem1.getId() == v.getId()) {
                    return;
                }
                if (mSelectedVoiceItem1 != null) {
                    mSelectedVoiceItem1.setSelected(false);
                }
                Log.i("zhang",
                        "onclick--mSelectedVoiceItem1.getId()==" + mSelectedVoiceItem1.getId()
                                + "item.getId==" + v.getId());
                v.setSelected(true);
                mSelectedVoiceItem1 = v;
            } else if (mType == Helper.jigsaw_element_type.BACKGROUND) {
//                old_highlight = mSelectedIndex[1];
//                mSelectedIndex[1] = highlight;
                if (mSelectedVoiceItem.getId() == v.getId()) {
                    return;
                }
                if (mSelectedVoiceItem != null) {
                    mSelectedVoiceItem.setSelected(false);
                }

                v.setSelected(true);
                mSelectedVoiceItem = v;
            }
            // seleted(v);
            // if (highlight == old_highlight) {
            // // avoid click the same item
            // return;
            // }

            Resources res = getContext().getResources();

            bitmap = BitmapFactory.decodeResource(res, element_res);
        } else {

            String data = v.getTag().toString();
            // String[] datas = data.split("_");
            ImageView in = (ImageView) v.findViewById(R.id.element_new);
            in.setVisibility(View.GONE);
            saveOfflineNewStatus(data, false);
            // String index = datas[0];
            // highlight = Integer.parseInt(index);
            // old_highlight = mSelectedIndex[1];
            // mSelectedIndex[1] = highlight;
            if (mSelectedVoiceItem != null) {
                mSelectedVoiceItem.setSelected(false);
            }

            v.setSelected(true);
            mSelectedVoiceItem = v;
            bitmap = getDiskBitmap(v.getTag(v.getId()).toString());
        }

        // update highlight
//        if (old_highlight >= 0) {
//            for (int i = 0; i < getChildCount(); i++) {
//                if (i == old_highlight) {
//                    if (i > 2) {
//                        if (i == 3) {
//                            getChildAt(i - 3).setSelected(false);
//                        } else {
//                            getChildAt(i).setSelected(false);
//                        }
//                    } else {
//                        getChildAt(i + h).setSelected(false);
//                    }
//
//                    break;
//                }
//            }
//        }
        // v.setSelected(true);
        if (mListener != null) {
            mListener.onSelected(mType, element_res, bitmap);
        }
    }

    private void saveOfflineNewStatus(String key, boolean value) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public void setOnSelectListener(onSelectListener listener) {
        mListener = listener;
    }

    //*/ Added by Droi Kimi Wu on 20151009 for refresh assemble list, maybe have downloaded resources.
    public void updateThumbList() {
        loadThumbList();
    }
    // end

    // modified for mingjun
    public interface onSelectListener {
        /**
         * @param type    : background or template
         * @param element : resource id of assemble element
         */
        void onSelected(Helper.jigsaw_element_type type, int element, Bitmap elementBitmap);
    }
    //*/
}
