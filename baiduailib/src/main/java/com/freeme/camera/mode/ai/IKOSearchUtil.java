package com.freeme.camera.mode.ai;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatDialog;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import com.baidu.aip.imageclassify.AipImageClassify;
import com.google.gson.Gson;
import com.scott.freeme.baiduai.R;
import com.wang.avi.AVLoadingIndicatorView;
import com.wang.avi.Indicator;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by azmohan on 18-3-13.
 */

public class IKOSearchUtil {

    private static final int LOADER_SIZE_SCALE = 8;
    private static final String INDICATOR_VIEW_STYLE = "BallClipRotatePulseIndicator";
    private static final ArrayList<AppCompatDialog> LOADERS = new ArrayList<>();
    private static final String APP_ID = "11738181";
    private static final String API_KEY = "EGjGGndFw7c02HhycAfKCQyr";
    private static final String SECRET_KEY = "2TctHk9i9kQ0La3jqE0ISj31pOYfU30M";

    private ResponseListener responseListener;
    private IKOResultBean mIKOResultBean;

    public static IKOSearchUtil getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final IKOSearchUtil INSTANCE = new IKOSearchUtil();
    }

    public void setResponseListener(ResponseListener responseListener) {
        this.responseListener = responseListener;
    }

    public void setIKOResultBean(IKOResultBean mIKOResultBean) {
        this.mIKOResultBean = mIKOResultBean;
    }

    public void loadIKOBean() {
        try {
            responseListener.onLoadStart();
            AipImageClassify client = new AipImageClassify(APP_ID, API_KEY, SECRET_KEY);
            client.setConnectionTimeoutInMillis(2000);
            client.setSocketTimeoutInMillis(60000);
            HashMap<String, String> options = new HashMap<String, String>();
            options.put("baike_num", "5");
            JSONObject res = client.advancedGeneral(mIKOResultBean.getmResultByte(), options);
            Gson gson = new Gson();
            IKOBean ikoBean = gson.fromJson(res.toString(), IKOBean.class);
            if (ikoBean.getResult().get(0).getBaike_info().getBaike_url() == null
                    || ikoBean.getResult().get(0).getBaike_info().getDescription() == null
                    || ikoBean.getResult().get(0).getBaike_info().getImage_url() == null) {
                responseListener.onLoadFailure();
                return;
            }
            mIKOResultBean.setmResultBean(ikoBean);
            responseListener.onLoadSuccess(mIKOResultBean);
        } catch (RuntimeException e) {
            responseListener.onLoadFailure();
        }
    }

    public static boolean getNetWorkStatus(final Activity activity) {
        boolean netSataus = false;
        ConnectivityManager cwjManager = (ConnectivityManager) activity
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cwjManager.getActiveNetworkInfo() != null) {
            netSataus = cwjManager.getActiveNetworkInfo().isConnectedOrConnecting();
        }

        if (!netSataus) {
            NetworkInfo.State gprs = cwjManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
            cwjManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
            if (gprs == NetworkInfo.State.CONNECTED || gprs == NetworkInfo.State.CONNECTING) {
                netSataus = true;
            }
        }
        return netSataus;
    }

    public void showLoading(Context context) {
        final AppCompatDialog dialog = new AppCompatDialog(context, R.style.dialog);
        final AVLoadingIndicatorView avLoadingIndicatorView = new AVLoadingIndicatorView(context);
        avLoadingIndicatorView.setIndicatorColor(Color.parseColor("#9000ff00"));
        avLoadingIndicatorView.setIndicator(getIndicator(INDICATOR_VIEW_STYLE));
        dialog.setContentView(avLoadingIndicatorView);
        dialog.setCancelable(false);
        Point point = getScreenResolution(context);

        final Window dialogWindow = dialog.getWindow();
        if (dialogWindow != null) {
            final WindowManager.LayoutParams lp = dialogWindow.getAttributes();
            lp.width = point.x / LOADER_SIZE_SCALE;
            lp.height = point.y / LOADER_SIZE_SCALE;
            lp.gravity = Gravity.CENTER;
        }
        LOADERS.add(dialog);
        dialog.show();
    }

    public void stopLoading() {
        for (AppCompatDialog dialog : LOADERS) {
            if (dialog != null) {
                if (dialog.isShowing()) {
                    dialog.cancel();
                }
            }
        }
    }

    private Point getScreenResolution(Context context) {
        final Resources resources = context.getResources();
        final DisplayMetrics dm = resources.getDisplayMetrics();
        final Point point = new Point();
        point.x = dm.widthPixels;
        point.y = dm.heightPixels;
        return point;
    }

    private static Indicator getIndicator(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        final StringBuilder drawableClassName = new StringBuilder();
        if (!name.contains(".")) {
            final String defaultPackageName = AVLoadingIndicatorView.class.getPackage().getName();
            drawableClassName.append(defaultPackageName)
                    .append(".indicators")
                    .append(".");
        }
        drawableClassName.append(name);
        try {
            final Class<?> drawableClass = Class.forName(drawableClassName.toString());
            return (Indicator) drawableClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
