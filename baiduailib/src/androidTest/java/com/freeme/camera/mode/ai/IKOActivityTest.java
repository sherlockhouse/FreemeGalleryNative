package com.freeme.camera.mode.ai;


import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;


/**
 * 作者：gulincheng
 * 日期:2018/09/11 15:36
 * 文件描述:
 */
@RunWith(AndroidJUnit4.class)
public class IKOActivityTest {

    @Rule
    public final ActivityTestRule<IKOActivity> main
            =new ActivityTestRule(IKOActivity.class, true);

    @Test
    public void startSearch() {
        main.getActivity().startSearch("http://www.baidu.com");
    }
}