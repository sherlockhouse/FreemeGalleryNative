package com.freeme.camera.mode.ai;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.scott.freeme.baiduai.R;
import com.scott.freeme.baiduai.R2;

import butterknife.BindView;
import butterknife.ButterKnife;

//import com.freeme.camera.custom.CameraCustomXmlParser;
//import com.freeme.camera.util.CameraUtil;

public class IKOActivity extends Activity implements ResponseListener, View.OnClickListener {

    private static final String IKO_IMAGE_SEARCH_URL = "com.ume.browser";
    private static final int EXECUTOR_THREAD_POOL_SIZE = 2;
    private IKOResultBean mIkoResultBean;
    @BindView(R2.id.iko_toolbar)
    Toolbar mIkoToolbar;
    @BindView(R2.id.error_text)
    TextView mErrorText;
    @BindView(R2.id.error_text_retry)
    TextView mErrorTextRetry;
    @BindView(R2.id.image_original)
    ImageView mImageOriginal;
    @BindView(R2.id.iko_name)
    TextView mIkoName;
    @BindView(R2.id.image_iko)
    ImageView mImageIko;
    @BindView(R2.id.text_iko)
    TextView mTextIko;
    @BindView(R2.id.iko_scrollview)
    ScrollView mIkoScrollview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initSystemBar(true);
        setContentView(R.layout.activity_iko);
        ButterKnife.bind(this);
        IKOSearchUtil.getInstance().setResponseListener(this);
        mImageIko.setOnClickListener(this);
        mTextIko.setOnClickListener(this);
        new Thread(new IKORunnable()).start();
        mIkoToolbar.setNavigationIcon(R.drawable.ic_iko_back);
        mIkoToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }



    private static class IKORunnable implements Runnable {
        @Override
        public void run() {
            IKOSearchUtil.getInstance().loadIKOBean();
        }
    }

    public void initSystemBar(Boolean isLight) {
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            if (isLight) {
                window.setStatusBarColor(getResources().getColor(R.color.iko_background_color));
            } else {
                window.setStatusBarColor(getResources().getColor(R.color.iko_background_color));
            }
            View decor = window.getDecorView();
            int ui = decor.getSystemUiVisibility();
            if (isLight) {
                ui |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                ui &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decor.setSystemUiVisibility(ui);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        overridePendingTransition(R.anim.camera_zoom, R.anim.zoom_out);
    }

    @Override
    public void onLoadStart() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                IKOSearchUtil.getInstance().showLoading(IKOActivity.this);
            }
        });
    }

    @Override
    public void onLoadSuccess(final IKOResultBean mIkoResultBean) {
        this.mIkoResultBean = mIkoResultBean;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                IKOSearchUtil.getInstance().stopLoading();
                handleViewDisplay();
            }
        });
    }

    private void handleViewDisplay() {
        mIkoScrollview.setVisibility(View.VISIBLE);
        if (!isFinishing()) {
            Glide.with(IKOActivity.this)
                    .load(mIkoResultBean.getmResultBean().getResult().get(0).getBaike_info().getImage_url())
                    .override((int) getResources().getDimension(R.dimen.iko_search_image_width),
                            (int) getResources().getDimension(R.dimen.iko_search_image_height))
                    .into(mImageIko);
        }
        mImageIko.setTag(mIkoResultBean.getmResultBean().getResult().get(0).getBaike_info().getImage_url());
        mImageOriginal.setImageBitmap(BitmapFactory.decodeByteArray(mIkoResultBean.getmResultByte(), 0,
                mIkoResultBean.getmResultByte().length, null));
        mTextIko.setText(mIkoResultBean.getmResultBean().getResult().get(0).getBaike_info().getDescription());
        mTextIko.setTag(mIkoResultBean.getmResultBean().getResult().get(0).getBaike_info().getBaike_url());
        mIkoName.setText(mIkoResultBean.getmResultBean().getResult().get(0).getKeyword());
    }

    @Override
    public void onLoadFailure() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                IKOSearchUtil.getInstance().stopLoading();
                mIkoScrollview.setVisibility(View.GONE);
                mErrorText.setVisibility(View.VISIBLE);
                mErrorText.setText(getResources().getString(R.string.not_search_image));
            }
        });
    }

    @Override
    public void onClick(View view) {
        String url = null;
        final int id = view.getId();
        if (id == R.id.image_iko || id == R.id.text_iko) {
            url = (String) view.getTag();
        } else if (id == R.id.error_text_retry) {
            finish();
        }

        if (url != null) {
            startSearch(url);
        }
    }

    public void startSearch(String url) {
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        try {
            intent.setPackage(IKO_IMAGE_SEARCH_URL);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            intent.setPackage(null);
            IKOSearchUtil.startActivitySafely(this, intent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        IKOSearchUtil.getInstance().stopLoading();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
