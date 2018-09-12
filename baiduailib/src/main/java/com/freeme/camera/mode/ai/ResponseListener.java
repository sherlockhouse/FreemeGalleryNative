package com.freeme.camera.mode.ai;

/**
 * Created by azmohan on 18-3-13.
 */

public interface ResponseListener {
    void onLoadStart();

    void onLoadSuccess(IKOResultBean result);

    void onLoadFailure();
}
