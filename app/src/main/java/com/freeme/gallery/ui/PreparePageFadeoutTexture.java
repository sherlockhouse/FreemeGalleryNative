package com.freeme.gallery.ui;

import android.os.ConditionVariable;

import com.freeme.gallery.app.AbstractGalleryActivity;
import com.freeme.gallery.glrenderer.GLCanvas;
import com.freeme.gallery.glrenderer.RawTexture;
import com.freeme.gallery.ui.GLRoot.OnGLIdleListener;

public class PreparePageFadeoutTexture implements OnGLIdleListener {
    public static final  String KEY_FADE_TEXTURE = "fade_texture";
    private static final long   TIMEOUT          = 200;
    private RawTexture mTexture;
    private ConditionVariable mResultReady = new ConditionVariable(false);
    private boolean           mCancelled   = false;
    private GLView mRootPane;

    public PreparePageFadeoutTexture(GLView rootPane) {
        if (rootPane == null) {
            mCancelled = true;
            return;
        }
        int w = rootPane.getWidth();
        int h = rootPane.getHeight();
        if (w == 0 || h == 0) {
            mCancelled = true;
            return;
        }
        mTexture = new RawTexture(w, h, true);
        mRootPane = rootPane;
    }

    public static void prepareFadeOutTexture(AbstractGalleryActivity activity,
                                             GLView rootPane) {
        PreparePageFadeoutTexture task = new PreparePageFadeoutTexture(rootPane);
        if (task.isCancelled()) return;
        GLRoot root = activity.getGLRoot();
        RawTexture texture = null;
        root.unlockRenderThread();
        try {
            root.addOnGLIdleListener(task);
            texture = task.get();
        } finally {
            root.lockRenderThread();
        }

        if (texture == null) {
            return;
        }
        activity.getTransitionStore().put(KEY_FADE_TEXTURE, texture);
    }

    public boolean isCancelled() {
        return mCancelled;
    }

    public synchronized RawTexture get() {
        if (mCancelled) {
            return null;
        } else if (mResultReady.block(TIMEOUT)) {
            return mTexture;
        } else {
            mCancelled = true;
            return null;
        }
    }

    @Override
    public boolean onGLIdle(GLCanvas canvas, boolean renderRequested) {
        if (!mCancelled) {
            try {
                canvas.beginRenderTarget(mTexture);
                mRootPane.render(canvas);
                canvas.endRenderTarget();
            } catch (RuntimeException e) {
                mTexture = null;
            }
        } else {
            mTexture = null;
        }
        mResultReady.open();
        return false;
    }
}
