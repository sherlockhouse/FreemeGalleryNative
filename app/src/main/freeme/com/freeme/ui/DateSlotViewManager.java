package com.freeme.ui;

import android.content.res.Resources;
import android.graphics.Rect;

import com.freeme.data.StoryAlbumSet;
import com.freeme.gallery.R;
import com.freeme.gallery.app.AbstractGalleryActivity;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.freeme.utils.FreemeUtils;
import com.freeme.utils.LogUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class DateSlotViewManager {
    private static final String TAG = "DateSlotViewManager";

    private final static int OVER_INDEX    = 1;
    private final static int OVER_POSITION = 2;
    //*/ Added by Linguanrong adjust ui render, 2015-6-13
    public static boolean EXPEND_SUPPORT  = false;
    // The count of date item
    public int mCountDateItem = 0;
    // The templet accumulate item
    public int[] mNumDateItemPlus;
    public ArrayList<MediaItem>     mMediaItem       = new ArrayList<MediaItem>();
    public ArrayList<String>        mDate            = new ArrayList<String>();
    public ArrayList<Integer>       mChildeItemCount = new ArrayList<Integer>();
    public HashMap<String, Integer> mItemMap         = new HashMap<String, Integer>();
    //*/ Added by Linguanrong for story album, 2015-5-23
    public ArrayList<String>        mTmpDate         = new ArrayList<String>();
    public ArrayList<String>        mDayCount        = new ArrayList<String>();
    // The templet accumulate height
    private int[] mDateItemPlusHeight;
    // The templet item total
    private int mNumDateItemCountTotal = 0;
    // The templet height total
    private int mDateItemTotalHeight = 0;
    // item(Slot) params
    private int mItemHeight        = 0;
    private int mItemWidth         = 0;
    private int mSlotWidth         = 0;
    private int mSlotHeight        = 0;
    private int mSlotGap           = 0;
    private int mHorizontalPadding = 0;
    private int mVerticalPadding   = 0;
    private int mSlotCount         = 0;
    private int mLayoutHeight      = 0;
    private int mCurrentPosition   = 0;
    private AbstractGalleryActivity mActivity;
    private DateSlotView mDateSlotView;
    private MediaSet     mData;
    private String mTempDate = "1970-1-1";
    private int     mStoryIndex        = -1;
    private int     mHeaderHeight      = 0;
    private int     mTimeLineHeight    = 0;
    private int     mDateItemBgPadding = 0;
    private boolean mShowHeader        = false;
    //*/
    private Rect[] mTempletRect = new Rect[6];
    private String[] mCurrenWeek;
    private String[] mLastWeek;
    private String   mToday, mYesterday;
    private int mMediaItemCount = 0;
    private int mUnitCount      = 1;
    private int mTitleHeight  = 0;
    private int mRemarkHeight = 0;
    private       boolean mIsCollapseInit = EXPEND_SUPPORT;
    //*/

    public DateSlotViewManager(AbstractGalleryActivity activity,
                               DateSlotView slotView, MediaSet mediaSet) {
        mActivity = activity;
        mDateSlotView = slotView;
        mData = mediaSet;

        Resources res = activity.getResources();
        mCurrenWeek = res.getStringArray(R.array.current_week);
        mLastWeek = res.getStringArray(R.array.last_week);
        mToday = res.getString(R.string.album_timeshaft_today);
        mYesterday = res.getString(R.string.album_timeshaft_yesterday);
        //*/ Modified by Linguanrong for story album, 2015-5-21
        if (mDateSlotView.mSlotViewSpec.isStory) {
            EXPEND_SUPPORT = true;
            mUnitCount = res.getInteger(R.integer.albumstory_rows_land);
            mDateItemBgPadding = (int) res.getDimension(R.dimen.story_date_item_bg_padding);
            mStoryIndex = mDateSlotView.getStoryIndex();
            if (mStoryIndex == StoryAlbumSet.ALBUM_BABY_ID
                    || mStoryIndex == StoryAlbumSet.ALBUM_LOVE_ID) {
                mShowHeader = true;
            }

            for (int i = 0; i < mTempletRect.length; i++) {
                mTempletRect[i] = new Rect(0, 0, 0, 0);
            }
        } else {
            EXPEND_SUPPORT = false;
            mTitleHeight = (int) res.getDimension(R.dimen.timeshaft_title_height);
            mRemarkHeight = (int) res.getDimension(R.dimen.timeshaft_remark_height);
            mUnitCount = res.getInteger(R.integer.album_timeshaft_rows_land);
        }
        mIsCollapseInit = EXPEND_SUPPORT;
        //*/
    }

    public void setItemsHeight(int headerHeight, int btnHeight, int timeLineHeight,
                               int textHeight, int topPadding, int bottomPadding) {
        mHeaderHeight = headerHeight;
        mTimeLineHeight = timeLineHeight;
        mTitleHeight = timeLineHeight + btnHeight;
        mRemarkHeight = textHeight + bottomPadding * 2;
    }

    public void setParams(int slotWidth, int slotHeight, int slotGap,
                          int slotPadding, int slotCount, int height) {
        mSlotWidth = slotWidth;
        mSlotHeight = slotHeight;
        mSlotGap = slotGap;
        mHorizontalPadding = slotPadding;
        if (mDateSlotView.mSlotViewSpec.isStory) {
            mVerticalPadding = mDateSlotView.mSlotViewSpec.slotPaddingV;
        } else {
            mVerticalPadding = slotPadding;
        }
        mSlotCount = slotCount;
        mLayoutHeight = height;

        if (isInitReady() && mSlotCount != 0) {
            updateMediaItem();
            initDatas();
        }
    }

    private boolean isInitReady() {
        return mSlotWidth != 1 && mSlotHeight != 1 && mSlotCount != 0;
    }

    private void updateMediaItem() {
        String date;
        String tmpStr = "";
        boolean showWeek = false;
        long ms = 0;
        int tmpCount = 0;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date d = new Date();

        mMediaItemCount = mData.getMediaItemCount();
        mMediaItem = mData.getMediaItem(0, mMediaItemCount);

        mTempDate = "1970-1-1";
        mDate.removeAll(mDate);
        //*/ Added by Linguanrong for story album, 2015-5-23
        mTmpDate.removeAll(mTmpDate);
        //*/
        for (int i = 0; i < mMediaItem.size(); i++) {
            ms = mMediaItem.get(i).getDateInMs() * 1000;
            d.setTime(ms);
            date = dateFormat.format(d);
            if (!mTempDate.equals(date)) {
                mTempDate = date;
                //*/ Added by Linguanrong for story album, 2015-5-23
                mTmpDate.add(date);
                //*/
                tmpStr = calcCalendar(ms);
                if ("".equals(tmpStr)) {
                    mDate.add(date);
                    showWeek = false;
                } else {
                    mDate.add(tmpStr);
                    showWeek = true;
                }
            }

            if (showWeek) {
                mItemMap.put(tmpStr, i + 1);
            } else {
                mItemMap.put(date, i + 1);
            }
        }

        mChildeItemCount.removeAll(mChildeItemCount);
        for (int i = 0; i < mDate.size(); i++) {
            if (0 == i) {
                mChildeItemCount.add(i, mItemMap.get(mDate.get(i)));
            } else {
                tmpCount = mItemMap.get(mDate.get(i - 1));
                tmpCount = mItemMap.get(mDate.get(i)) - tmpCount;
                mChildeItemCount.add(i, tmpCount);
            }

            if (mDateSlotView.mBtnExpanded.get(i) != null) {
                mDateSlotView.mBtnExpanded.put(i, mDateSlotView.mBtnExpanded.get(i));
            } else {
                mDateSlotView.mBtnExpanded.put(i, mIsCollapseInit);
            }
            if(!mDateSlotView.mSelectBtnTick.containsKey(i)){
                mDateSlotView.mSelectBtnTick.put(i, false);
            }
        }

        mCountDateItem = mDate.size();

        //*/ Added by Linguanrong for story album, 2015-5-23
        calcDayCount();
        //*/
    }

    private void initDatas() {
        int row_num = 0;
        mItemHeight = mSlotHeight + mSlotGap;
        mItemWidth = mSlotWidth + mSlotGap;

        mNumDateItemPlus = new int[mCountDateItem];
        mDateItemPlusHeight = new int[mCountDateItem];

        //*/ Modified by Linguanrong for story album, 2015-6-24
        if (mDateSlotView.mSlotViewSpec.isStory) {
            for (int i = 0; i < mCountDateItem; i++) {
                mNumDateItemPlus[i] = mItemMap.get(mDate.get(i));
                if (i == 0) {
                    if (mDateSlotView.mBtnExpanded.get(i)) {
                        row_num = 2;
                    } else {
                        if (mNumDateItemPlus[i] % mUnitCount == 0) {
                            row_num = mNumDateItemPlus[i] / mUnitCount + 1;
                        } else {
                            row_num = mNumDateItemPlus[i] / mUnitCount + 2;
                        }
                    }
                    mDateItemPlusHeight[i] = row_num * mItemHeight + mTitleHeight + mHeaderHeight;
                } else {
                    if (mDateSlotView.mBtnExpanded.get(i)) {
                        row_num = 2;
                        mDateItemPlusHeight[i] = row_num * mItemHeight
                                + mDateItemPlusHeight[i - 1] + mTitleHeight + mRemarkHeight;
                    } else {
                        if ((mNumDateItemPlus[i] - mNumDateItemPlus[i - 1]) % mUnitCount == 0) {
                            row_num = (mNumDateItemPlus[i] - mNumDateItemPlus[i - 1]) / mUnitCount + 1;
                        } else {
                            row_num = (mNumDateItemPlus[i] - mNumDateItemPlus[i - 1]) / mUnitCount + 2;
                        }
                        mDateItemPlusHeight[i] = row_num * mItemHeight
                                + mDateItemPlusHeight[i - 1] + mTitleHeight + mRemarkHeight;
                    }
                }
            }
        } else {
            for (int i = 0; i < mCountDateItem; i++) {
                mNumDateItemPlus[i] = mItemMap.get(mDate.get(i));
                if (i == 0) {
                    if (mDateSlotView.mBtnExpanded.get(i)) {
                        if (mNumDateItemPlus[i] > mUnitCount) {
                            row_num = 2;
                        } else {
                            row_num = 1;
                        }
                    } else {
                        if (mNumDateItemPlus[i] % mUnitCount == 0) {
                            row_num = mNumDateItemPlus[i] / mUnitCount;
                        } else {
                            row_num = mNumDateItemPlus[i] / mUnitCount + 1;
                        }
                    }
                    mDateItemPlusHeight[i] = row_num * mItemHeight + mTitleHeight + mHeaderHeight;
                } else {
                    if (mDateSlotView.mBtnExpanded.get(i)) {
                        if ((mNumDateItemPlus[i] - mNumDateItemPlus[i - 1]) > mUnitCount) {
                            row_num = 2;
                        } else {
                            row_num = 1;
                        }
                        mDateItemPlusHeight[i] = row_num * mItemHeight
                                + mDateItemPlusHeight[i - 1] + mTitleHeight + mRemarkHeight;
                    } else {
                        if ((mNumDateItemPlus[i] - mNumDateItemPlus[i - 1]) % mUnitCount == 0) {
                            row_num = (mNumDateItemPlus[i] - mNumDateItemPlus[i - 1]) / mUnitCount;
                        } else {
                            row_num = (mNumDateItemPlus[i] - mNumDateItemPlus[i - 1]) / mUnitCount + 1;
                        }
                        mDateItemPlusHeight[i] = row_num * mItemHeight
                                + mDateItemPlusHeight[i - 1] + mTitleHeight + mRemarkHeight;
                    }
                }
            }
        }
        //*/

        if (mCountDateItem == 0) {
            mNumDateItemCountTotal = 0;
            mDateItemTotalHeight = 0;
        } else {
            mNumDateItemCountTotal = mNumDateItemPlus[mCountDateItem - 1];
            mDateItemTotalHeight = mDateItemPlusHeight[mCountDateItem - 1];
        }

        mDateSlotView.mIsInited = true;
        mDateSlotView.initRender();
    }

    private String calcCalendar(long ms) {
        String str;
        Calendar calendar = Calendar.getInstance();
        int curDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int curWeekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
        int curYear = calendar.get(Calendar.YEAR);
        calendar.setTimeInMillis(ms);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);

        if (year == curYear) {
            int lastWeek = curWeekOfYear - 1;
            if (curWeekOfYear == weekOfYear) {
                int yesterday = curDayOfWeek - 1;
                if (curDayOfWeek == dayOfWeek) {
                    str = mToday;
                } else if (dayOfWeek == yesterday) {
                    str = mYesterday;
                } else {
                    str = mCurrenWeek[(dayOfWeek - 1)];
                }
            } else if (weekOfYear == lastWeek) {
                str = mLastWeek[(dayOfWeek - 1)];
            } else {
                str = "";
            }
        } else {
            str = "";
        }

        return str;
    }

    public void calcDayCount() {
        if (mShowHeader) {
            Calendar calendar = mDateSlotView.getCalendar();
            Resources res = mActivity.getResources();
            mDayCount.removeAll(mDayCount);
            for (int i = 0; i < mTmpDate.size(); i++) {
                mDayCount.add(getDaysBetween(res, calendar, mTmpDate.get(i)));
            }
            mDateSlotView.updateDayCount();
        }
    }

    public String getDaysBetween(Resources res, Calendar calendar, String dateStr) {
        int year, month, day;
        int yearCount = 0;
        boolean babyStory = mStoryIndex == StoryAlbumSet.ALBUM_BABY_ID;
        boolean before = false;

        Calendar tagday = Calendar.getInstance();
        String[] date = dateStr.split(FreemeUtils.DATE_SPLIT);
        year = Integer.valueOf(date[0]);
        month = Integer.valueOf(date[1]);
        day = Integer.valueOf(date[2]);
        tagday.set(year, month - 1, day);

        if (FreemeUtils.STORY_DEBUG) {
            LogUtil.i("connor", "dm dateStr = " + dateStr);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String str = dateFormat.format(calendar.getTime());
            LogUtil.i("connor", "dm calendar  = " + str);
        }

        // if earlier, then swap
        if (tagday.before(calendar)) {
            before = true;
            Calendar swap = calendar;
            calendar = tagday;
            tagday = swap;
        }

        int tagYear = tagday.get(Calendar.YEAR);

        // full month
        int yearGap = tagYear - calendar.get(Calendar.YEAR);
        if (yearGap == 0 && !before) {
            Calendar tmp = (Calendar) calendar.clone();
            tmp.add(Calendar.MONTH, 1);

            if (tmp.get(Calendar.MONTH) == tagday.get(Calendar.MONTH)
                    && tmp.get(Calendar.DAY_OF_MONTH) == tagday.get(Calendar.DAY_OF_MONTH)) {
                return res.getString(R.string.baby_full_month);
            }
        }
        int days = tagday.get(Calendar.DAY_OF_YEAR) - calendar.get(Calendar.DAY_OF_YEAR);

        // get days
        if (calendar.get(Calendar.YEAR) != tagYear) {
            calendar = (Calendar) calendar.clone();
            while (calendar.get(Calendar.YEAR) != tagYear) {
                days += calendar.getActualMaximum(Calendar.DAY_OF_YEAR);
                calendar.add(Calendar.YEAR, 1);
                yearCount++;
            }
        }

        // earlier
        if (before) {
            return res.getString(babyStory ? R.string.baby_story_before_born
                    : R.string.love_story_before_love, days);
        } else {
            // full year
            if (calendar.get(Calendar.MONTH) == tagday.get(Calendar.MONTH)
                    && calendar.get(Calendar.DAY_OF_MONTH) == tagday.get(Calendar.DAY_OF_MONTH)
                    && yearCount > 0) {
                return res.getString(babyStory ? R.string.baby_full_year : R.string.love_full_year, yearCount);
            }

            days++;
            if (days == 1) {
                return res.getString(babyStory ? R.string.baby_born : R.string.love_date);
            } else if (babyStory && days == 100) {
                return res.getString(R.string.baby_hundred_day);
            }
        }

        return res.getString(R.string.day_count, days);
    }

    public int getContentHeight() {
        return mDateItemTotalHeight;
    }

    public int getDateItemCount() {
        return mCountDateItem;
    }

    public int getSlotIndex(int absoluteX, int absoluteY, int index) {
        if (!isInitReady()) return 0;

        int frontCount;
        int frontHeight;
        int dateItemCount;

        int dateItem_id = getDateItemId(absoluteY, OVER_POSITION);
        if (dateItem_id == 0) {
            frontCount = 0;
            //*/ Modified by Linguanrong for story album, 2015-5-21
            frontHeight = mTitleHeight + mHeaderHeight;
            dateItemCount = mNumDateItemPlus[dateItem_id];
            //*/
        } else {
            frontCount = mNumDateItemPlus[dateItem_id - 1];
            frontHeight = mDateItemPlusHeight[dateItem_id - 1]
                    + mTitleHeight + mRemarkHeight;
            //*/ Added by Linguanrong for story album, 2015-6-25
            dateItemCount = mNumDateItemPlus[dateItem_id] - mNumDateItemPlus[dateItem_id - 1];
            //*/
        }

        //*/ Modified by Linguanrong for story album, 2015-6-25
        if (dateItemCount <= 0) {
            return -1;
        }

        int overPos = absoluteY - frontHeight;
        if (overPos < 0) {
            return -1;
        }

        int row = 0;
        int col = 0;
        if (mDateSlotView.mSlotViewSpec.isStory) {
            switch (dateItemCount) {
                case 1:
                    row = overPos / (mItemHeight * 2);
                    col = absoluteX / (mItemWidth * mUnitCount);
                    break;

                case 2:
                    row = overPos / (mItemHeight * 2);
                    col = absoluteX / (mItemWidth * mUnitCount / 2);
                    break;

                default:
                    row = overPos / mItemHeight;
                    col = absoluteX / mItemWidth;

                    if (row >= 2) {
                        row--;
                    } else {
                        if (row == 1 && col == 0) {
                            row = 0;
                            col = 2;
                        } else if (col >= 1) {
                            row = 0;
                            col = 1;
                        }
                    }
                    break;
            }
        } else {
            row = overPos / mItemHeight;
            col = absoluteX / mItemWidth;
        }
        //*/

        index = Math.max(0, (row * mUnitCount + col + frontCount));

        if (index > mNumDateItemPlus[dateItem_id] - 1) {
            index = -1;
        }

        return index;
    }

    private int getDateItemId(int result, int overType) {
        int id = 0;

        switch (overType) {
            case OVER_INDEX:
                for (int i = 0; i < mCountDateItem; i++) {
                    if (i == 0) {
                        if (result < mNumDateItemPlus[0]) {
                            id = 0;
                        }
                    } else {
                        if (result >= mNumDateItemPlus[i - 1] && result <= mNumDateItemPlus[i]) {
                            id = i;
                        }
                    }
                }
                break;

            case OVER_POSITION:
                for (int i = 0; i < mCountDateItem; i++) {
                    if (i == 0) {
                        if (result < mDateItemPlusHeight[0]) {
                            id = 0;
                        }
                    } else {
                        int max = 0;
                        if (mDateItemPlusHeight[mCountDateItem - 1] < (mLayoutHeight + mCurrentPosition)) {
                            max = mLayoutHeight + mCurrentPosition;
                        } else {
                            max = mDateItemPlusHeight[i];
                        }

                        if (result >= mDateItemPlusHeight[i - 1] && result <= max) {
                            id = i;
                        }
                    }
                }
                break;
        }

        return id;
    }

    public int getVisibleRangeIndex(int position, int result, boolean isStart) {
        if (!isInitReady()) return 0;

        int frontCount;
        int frontHeight;

        if (isStart) {
            mCurrentPosition = position;
        }

        int dateItem_id = getDateItemId(position, OVER_POSITION);
        if (dateItem_id == 0) {
            frontCount = 0;
            //*/ Modified by Linguanrong for story album, 2015-5-21
            frontHeight = mTitleHeight + mHeaderHeight;
            //*/
        } else {
            frontCount = mNumDateItemPlus[dateItem_id - 1];
            frontHeight = mDateItemPlusHeight[dateItem_id - 1]
                    + mTitleHeight + mRemarkHeight;
        }

        int overPos = position - frontHeight;

        if (isStart) {
            //*/ Modified by Linguanrong for story album, 2015-6-24
            if (mDateSlotView.mSlotViewSpec.isStory) {
                if (overPos < mSlotHeight * 2 + mSlotGap) {
                    result = frontCount;
                } else {
                    int startRow = overPos / mItemHeight - 1;
                    result = Math.max(0, mUnitCount * startRow + frontCount);
                }
            } else {
                int startRow = overPos / mItemHeight;
                result = Math.max(0, mUnitCount * startRow + frontCount);
            }
            //*/
        } else {
            int endRow = (overPos + mItemHeight - 1) / mItemHeight + 1;
            result = Math.min(mSlotCount, mUnitCount * endRow + frontCount);
        }

        return result;
    }

    public Rect getDividerRect(int id, int width, Rect rect) {
        int pos = 0;
        if (mDateItemPlusHeight != null && mDateItemPlusHeight.length > 0) {
            //*/ Modified by Linguanrong for story album, 2015-5-21
            if (id == 0) {
                pos = mHeaderHeight;
            } else {
                pos += mDateItemPlusHeight[id - 1] + mRemarkHeight;
            }
            //*/
        }

        rect.set(mHorizontalPadding, pos, width - mHorizontalPadding, pos + 1);

        return rect;
    }
    //*/

    //*/ Added by Linguanrong for story album, 2015-6-26
    public Rect getDateItemBgRect(int id, int width) {
        Rect rect = new Rect();
        int top = 0;
        if (mDateItemPlusHeight != null && mDateItemPlusHeight.length > 0) {
            if (id == 0) {
                top = mHeaderHeight + mTimeLineHeight;
                rect.set(mDateItemBgPadding, top, width - mDateItemBgPadding * 2,
                        mDateItemPlusHeight[id] - top + mRemarkHeight);
            } else {
                top = mDateItemPlusHeight[id - 1] + mRemarkHeight + mTimeLineHeight;
                rect.set(mDateItemBgPadding, top, width - mDateItemBgPadding * 2,
                        mDateItemPlusHeight[id] - mDateItemPlusHeight[id - 1] - mTimeLineHeight);
            }
        }

        return rect;
    }

    public int getIndex(int index) {
        return getDateItemId(index, OVER_INDEX);
    }

    public int getPosItemId(int pos) {
        return getDateItemId(pos, OVER_POSITION);
    }

    public void dateManagerDataUpdate() {
        initDatas();
        mDateSlotView.invalidate();
        if (mCountDateItem != 0
                && ((mCurrentPosition + mLayoutHeight) > mDateItemPlusHeight[mCountDateItem - 1])) {
            int position = mDateItemPlusHeight[mCountDateItem - 1] - mLayoutHeight;
            mDateSlotView.setScrollPosition(position);
        }
    }

    //*/ Added by Linguanrong for story album, 2015-5-23
    public int getHeaderHeight() {
        return mHeaderHeight;
    }

    public boolean isSinglePhoto(int index) {
        if (mDateSlotView.mSlotViewSpec.isStory) {
            if (mNumDateItemPlus.length == 0) {
                return false;
            }

            int dateItem_id = getDateItemId(index, OVER_INDEX);
            int dateItemCount = dateItem_id == 0 ? mNumDateItemPlus[dateItem_id]
                    : mNumDateItemPlus[dateItem_id] - mNumDateItemPlus[dateItem_id - 1];

            return dateItemCount == 1;
        } else {
            return false;
        }
    }

    public boolean isLargePhoto(int index) {
        Rect rect = new Rect(0, 0, 0, 0);
        rect = getSlotRect(index, rect);

        return rect.right - rect.left >= mSlotWidth * 2
                || rect.bottom - rect.top >= mSlotHeight * 2;
    }

    public Rect getSlotRect(int index, Rect rect) {
        if (!isInitReady() || mNumDateItemPlus.length == 0) {
            rect.set(0, 0, 0, 0);
            return rect;
        }

        int overIndex;
        int frontHeight;

        int dateItem_id = getDateItemId(index, OVER_INDEX);
        if (dateItem_id == 0) {
            overIndex = index;
            //*/ Modified by Linguanrong for story album, 2015-5-21
            frontHeight = mTitleHeight + mHeaderHeight;
            //*/
        } else {
            overIndex = index - mNumDateItemPlus[dateItem_id - 1];
            frontHeight = mDateItemPlusHeight[dateItem_id - 1]
                    + mTitleHeight + mRemarkHeight;
        }

        //*/ Modified by Linguanrong for story album, 2015-6-24
        int left = 0;
        int top = 0;
        int right = 0;
        int bottom = 0;
        if (mDateSlotView.mSlotViewSpec.isStory) {
            int dateItemCount = dateItem_id == 0 ? mNumDateItemPlus[dateItem_id]
                    : mNumDateItemPlus[dateItem_id] - mNumDateItemPlus[dateItem_id - 1];
            if (dateItemCount == 1) {
                left = mHorizontalPadding;
                top = mVerticalPadding + frontHeight;
                right = left + mSlotWidth * mUnitCount + mSlotGap * (mUnitCount - 1);
                bottom = top + mSlotHeight * 2 + mSlotGap;
            } else if (dateItemCount == 2) {
                switch (overIndex) {
                    case 0:
                        left = mHorizontalPadding;
                        top = mVerticalPadding + frontHeight;
                        right = left + mItemWidth + mSlotWidth / 2 - mSlotGap / 2;
                        bottom = top + mSlotHeight * 2 + mSlotGap;
                        break;

                    case 1:
                        left = mHorizontalPadding + mItemWidth + mSlotWidth / 2 + mSlotGap / 2;
                        top = mVerticalPadding + frontHeight;
                        right = mHorizontalPadding + mItemWidth * mUnitCount - mSlotGap;
                        bottom = top + mSlotHeight * 2 + mSlotGap;
                        break;
                }
            } else {
                switch (overIndex) {
                    case 0:
                        left = mHorizontalPadding;
                        top = mVerticalPadding + frontHeight;
                        right = left + mSlotWidth;
                        bottom = top + mSlotHeight;
                        break;

                    case 1:
                        left = mHorizontalPadding + mItemWidth;
                        top = mVerticalPadding + frontHeight;
                        right = left + mSlotWidth * 2 + mSlotGap;
                        bottom = top + mSlotHeight * 2 + mSlotGap;
                        break;

                    case 2:
                        left = mHorizontalPadding;
                        top = mVerticalPadding + frontHeight + mItemHeight;
                        right = left + mSlotWidth;
                        bottom = top + mSlotHeight;
                        break;

                    default:
                        int col, row;
                        row = overIndex / mUnitCount + 1;
                        col = overIndex % mUnitCount;
                        left = mHorizontalPadding + col * mItemWidth;
                        top = mVerticalPadding + row * mItemHeight + frontHeight;
                        right = left + mSlotWidth;
                        bottom = top + mSlotHeight;
                        break;
                }
            }
            rect.set(left, top, right, bottom);
        } else {
            int col, row;
            row = overIndex / mUnitCount;
            col = overIndex - row * mUnitCount;
            int x = mHorizontalPadding + col * mItemWidth;
            if (col > mUnitCount - 3) {
                x += (col - 1);
            }
            int y = mVerticalPadding + row * mItemHeight + frontHeight;
            rect.set(x, y, x + mSlotWidth, y + mSlotHeight);
        }
        //*/

        return rect;
    }
    //*/
}
