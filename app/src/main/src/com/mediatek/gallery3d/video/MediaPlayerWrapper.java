
package com.mediatek.gallery3d.video;

import java.io.IOException;
import java.util.Map;

import com.android.gallery3d.app.ControllerOverlay;
import com.mediatek.gallery3d.util.Log;

import android.content.Context;
import android.media.AudioManager;
//import android.media.Cea708CaptionRenderer;
//import android.media.ClosedCaptionRenderer;
import android.media.MediaPlayer;
//import android.media.Metadata;
//import android.media.SubtitleController;
//import android.media.TtmlRenderer;
//import android.media.WebVttRenderer;
import android.net.Uri;
import android.view.SurfaceHolder;

public class MediaPlayerWrapper implements MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnInfoListener,
        MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnSeekCompleteListener,
        MovieView.SurfaceCallback {

    private static final String TAG = "Gallery2/VideoPlayer/MediaPlayerWrapper";

    // / M: Handle for non-google notify messages make CTS or 3rd party app fail,
    // MTK_PLAYBACK value 1 means the player is MTK video player {@
    private static int KEY_PLAYBACK_PARAMETER = 2100;
    private static int MTK_PLAYBACK_VALUE = 1;
    // / @}

    private Context mContext;
    private MovieView mMovieView;
    private MediaPlayer mMediaPlayer;
    private Uri mUri;
    private Map<String, String> mHeaders;
    private SurfaceHolder mSurfaceHolder;
    private int mCurrentBufferPercentage;
    private int mSeekWhenPrepared;
    private Listener mListener;
    private boolean mOnResumed;

    // all possible internal states
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    private int mCurrentState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;

    private boolean mCanPause;
    private boolean mCanSeekBack;
    private boolean mCanSeekForward;

    private int mVideoWidth;
    private int mVideoHeight;

    private Object mSurfaceWidth;
    private Object mSurfaceHeight;

    private int mDuration;

    private int mAudioSession;

    // / for slowmotion {@
    private boolean mEnableSlowMotionSpeed;
    private int mSlowMotionSpeed;
    private String mSlowMotionSection;
    private int mFps;

    // / @}

    public MediaPlayerWrapper(Context context, MovieView movieView) {
        this.mContext = context;
        this.mMovieView = movieView;
        initialize();
    }

    private void initialize() {
        if (mMovieView != null) {
            mMovieView.setSurfaceListener(this);
        }
    }

    public void setVideoURI(Uri uri) {
        setVideoURI(uri, null);
    }

    public void setVideoURI(Uri uri, Map<String, String> headers) {
        Log.v(TAG, "setVideoURI(" + uri + ", " + headers + ")");
        mDuration = -1;
        setResumed(true);
        mUri = uri;
        mHeaders = headers;
        openVideo();
    }

    /**
     * surfaceCreate will invoke openVideo after the activity stoped. Here set
     * this flag to avoid openVideo after the activity stoped.
     *
     * @param resume
     */
    public void setResumed(final boolean resume) {
        Log.v(TAG, "setResumed(" + resume + ") mUri=" + mUri + ", mOnResumed="
                + mOnResumed);
        mOnResumed = resume;
    }

    private void openVideo() {
        if (!mOnResumed || mUri == null || mSurfaceHolder == null) {
            Log.v(TAG, "openVideo, not ready for playback just yet," +
                    " will try again later, mOnResumed = " + mOnResumed +
                    ", mUri = " + mUri + ", mSurfaceHolder = " + mSurfaceHolder);
            return;
        }
        Log.v(TAG, "openVideo");
        // we shouldn't clear clear the target state, because somebody might
        // have called start() previously
        release(false);

        try {
            mMediaPlayer = new MediaPlayer();
            /*final SubtitleController controller = new SubtitleController(
                    mContext, mMediaPlayer.getMediaTimeProvider(), mMediaPlayer);
            controller.registerRenderer(new WebVttRenderer(mContext));
            controller.registerRenderer(new TtmlRenderer(mContext));
            controller.registerRenderer(new Cea708CaptionRenderer(mContext));
            controller.registerRenderer(new ClosedCaptionRenderer(mContext));
            mMediaPlayer.setSubtitleAnchor(controller, mMovieView);*/
            if (mAudioSession != 0) {
                mMediaPlayer.setAudioSessionId(mAudioSession);
            } else {
                mAudioSession = mMediaPlayer.getAudioSessionId();
            }
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnVideoSizeChangedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnInfoListener(this);
            mMediaPlayer.setOnBufferingUpdateListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
            mMediaPlayer.setDataSource(mContext, mUri, mHeaders);
            /*if (MtkVideoFeature.isMtkMediaPlayer()) {
                mMediaPlayer.setParameter(KEY_PLAYBACK_PARAMETER, MTK_PLAYBACK_VALUE);
            }*/
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.prepareAsync();

            mCurrentBufferPercentage = 0;
            mCurrentState = STATE_PREPARING;
        } catch (IOException ex) {
            Log.e(TAG, "unable to open content: " + mUri, ex);
            onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "unable to open content: " + mUri, ex);
            onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        }
    }

    public void stop() {
        Log.v(TAG, "stop");
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            mTargetState = STATE_IDLE;
        }
    }

    /**
     * release the media player in any state
     *
     * @param cleartargetstate
     */
    private void release(boolean cleartargetstate) {
        Log.v(TAG, "release");
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            if (cleartargetstate) {
                mTargetState = STATE_IDLE;
            }
        }
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        Log.v(TAG, "onSeekComplete");
        if (mListener != null) {
            mListener.onSeekComplete(mp);
        }
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        Log.v(TAG, "onVideoSizeChanged, width = " + width + ", height = "
                + height);
        mVideoWidth = mp.getVideoWidth();
        mVideoHeight = mp.getVideoHeight();
        if (mVideoWidth != 0 && mVideoHeight != 0 && mSurfaceHolder != null) {
            mSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);
            // TODO mNeedWaitLayout
        }
        if (mListener != null) {
            mListener.onVideoSizeChanged(mp, width, height);
        }
        if (mMovieView != null) {
            mMovieView.setVideoLayout(mVideoWidth, mVideoHeight);
        }
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        Log.v(TAG, "onInfo(" + mp + ") what: " + what + ", extra:" + extra);
        if (mListener != null) {
            mListener.onInfo(mp, what, extra);
        }
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.v(TAG, "onPrepared(" + mp + ")");
        mCurrentState = STATE_PREPARED;

        /* get the capabilities of the player of this stream */
        /*final Metadata data = mp.getMetadata(MediaPlayer.METADATA_ALL,
                MediaPlayer.BYPASS_METADATA_FILTER);
        if (data != null) {
            mCanPause = !data.has(Metadata.PAUSE_AVAILABLE)
                    || data.getBoolean(Metadata.PAUSE_AVAILABLE);
            mCanSeekBack = !data.has(Metadata.SEEK_BACKWARD_AVAILABLE)
                    || data.getBoolean(Metadata.SEEK_BACKWARD_AVAILABLE);
            mCanSeekForward = !data.has(Metadata.SEEK_FORWARD_AVAILABLE)
                    || data.getBoolean(Metadata.SEEK_FORWARD_AVAILABLE);
        } else {*/
            mCanPause = true;
            mCanSeekBack = true;
            mCanSeekForward = true;
            Log.v(TAG, "Metadata is null!");
        //}
        Log.v(TAG, "onPrepared, mCanPause=" + mCanPause + ", mCanSeekBack = "
                + mCanSeekBack + ", mCanSeekForward = " + mCanSeekForward);

        if (mListener != null) {
            mListener.onPrepared(mp);
        }

        Log.d(TAG, "onPrepared, mSeekWhenPrepared = " + mSeekWhenPrepared);
        if (mSeekWhenPrepared != 0) {
            seekTo(mSeekWhenPrepared);
        }

        // TODO for video size changed before started issue

        mVideoWidth = mp.getVideoWidth();
        mVideoHeight = mp.getVideoHeight();
        Log.v(TAG, "onPrepared, mVideoWidth = " + mVideoWidth
                + ", mVideoHeight = " + mVideoHeight);

        if (mVideoWidth != 0 && mVideoHeight != 0 && mSurfaceHolder != null) {
            mSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);
        }

        if (mTargetState == STATE_PLAYING) {
            start();
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Log.v(TAG, "onBufferingUpdate(" + mp + ")" + ", percent: " + percent);
        mCurrentBufferPercentage = percent;
        if (mListener != null) {
            mListener.onBufferingUpdate(mp, percent);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.v(TAG, "onCompletion(" + mp + ")");
        mCurrentState = STATE_PLAYBACK_COMPLETED;
        mTargetState = STATE_PLAYBACK_COMPLETED;
        if (mListener != null) {
            mListener.onCompletion(mp);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "onError(" + mp + "), what = " + what + ", extra = " + extra);
        // avoid notify two or more errors
        if (mCurrentState == STATE_ERROR) {
            Log.v(TAG, "current state is error, skip error: " + what + ", "
                    + extra);
            return true;
        }
        mCurrentState = STATE_ERROR;
        mTargetState = STATE_ERROR;
        if (mListener != null) {
            mListener.onError(mp, what, extra);
        }
        return true;
    }

    public void start() {
        Log.v(TAG, "start()");
        // M: to clear mEnableSlowMotionSpeed flag on start() every time
        // to avoid call enableSlowMotionSpeed invalid
        //disableSlowMotionSpeed();
        if (isInPlaybackState()) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
        }
        mTargetState = STATE_PLAYING;
    }

    public void pause() {
        Log.v(TAG, "pause");
        if (isInPlaybackState() && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mCurrentState = STATE_PAUSED;
        }
        mTargetState = STATE_PAUSED;
    }

    public void suspend() {
        Log.v(TAG, "suspend");
        release(false);
    }

    public void resume() {
        Log.v(TAG, "resume");
        openVideo();
    }

    public void seekTo(int msec) {
        Log.v(TAG, "seekTo " + msec);
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(msec);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = msec;
        }
    }

    public int getCurrentPosition() {
        int position = 0;
        if (mSeekWhenPrepared > 0) {
            // if just only record position in previous seek,
            // return last recorded seek position at here to restore
            position = mSeekWhenPrepared;
        } else if (isInPlaybackState()) {
            position = mMediaPlayer.getCurrentPosition();
        }
        Log.v(TAG, "getCurrentPosition()= " + position);
        return position;
    }

    // for duration displayed
    public void setDuration(final int duration) {
        Log.v(TAG, "setDuration(" + duration + ")");
        mDuration = (duration > 0 ? -duration : duration);
    }

    public int getDuration() {
        if (isInPlaybackState()) {
            if (mDuration > 0) {
                return mDuration;
            }
            mDuration = mMediaPlayer.getDuration();
            Log.v(TAG, "getDuration from mediaplayer is " + mDuration);
        }
        return mDuration;
    }

    public void clearDuration() {
        Log.v(TAG, "clearDuration() mDuration=" + mDuration);
        mDuration = -1;
    }

    // clear the seek position any way.
    // this will effect the case: stop video before it's seek completed.
    public void clearSeek() {
        Log.v(TAG, "clearSeek() mSeekWhenPrepared=" + mSeekWhenPrepared);
        mSeekWhenPrepared = 0;
    }

    public int getBufferPercentage() {
        if (mMediaPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    public boolean isPlaying() {
        boolean isPlaying = isInPlaybackState() && mMediaPlayer.isPlaying();
        Log.v(TAG, "isPlaying = " + isPlaying);
        return isPlaying;
    }

    public boolean isCurrentPlaying() {
        Log.v(TAG, "isCurrentPlaying() mCurrentState=" + mCurrentState);
        return mCurrentState == STATE_PLAYING;
    }

    public boolean isInPlaybackState() {
        Log.v(TAG, "isInPlaybackState() mCurrentState= " + mCurrentState);
        return (mMediaPlayer != null && mCurrentState != STATE_ERROR
                && mCurrentState != STATE_IDLE && mCurrentState != STATE_PREPARING);
    }

    public boolean canPause() {
        Log.v(TAG, "canPause: " + mCanPause);
        return mCanPause;
    }

    public boolean canSeekBackward() {
        Log.v(TAG, "mCanSeekBack: " + mCanSeekBack);
        return mCanSeekBack;
    }

    public boolean canSeekForward() {
        Log.v(TAG, "mCanSeekForward: " + mCanSeekForward);
        return mCanSeekForward;
    }

    public int getAudioSessionId() {
        if (mAudioSession == 0) {
            MediaPlayer foo = new MediaPlayer();
            mAudioSession = foo.getAudioSessionId();
            foo.release();
        }
        return mAudioSession;
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public interface Listener {

        public void onSeekComplete(MediaPlayer mp);

        public void onVideoSizeChanged(MediaPlayer mp, int width, int height);

        public boolean onInfo(MediaPlayer mp, int what, int extra);

        public void onPrepared(MediaPlayer mp);

        public void onBufferingUpdate(MediaPlayer mp, int percent);

        public void onCompletion(MediaPlayer mp);

        public boolean onError(MediaPlayer mp, int what, int extra);
    }

    @Override
    public void onSurfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "onSurfaceCreated(" + holder + ")");
        mSurfaceHolder = holder;
        openVideo();
    }

    @Override
    public void onSurfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        Log.v(TAG, "surfaceChanged(" + holder + ", " + format + ", " + width
                + ", " + height + ")");
        Log.v(TAG, "surfaceChanged() mMediaPlayer=" + mMediaPlayer
                + ", mTargetState=" + mTargetState + ", mVideoWidth="
                + mVideoWidth + ", mVideoHeight=" + mVideoHeight);
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        final boolean isValidState = (mTargetState == STATE_PLAYING);
        final boolean hasValidSize = (mVideoWidth == width && mVideoHeight == height);
        if (mMediaPlayer != null && isValidState && hasValidSize
                && mCurrentState != STATE_PLAYING) {
            if (mSeekWhenPrepared != 0) {
                seekTo(mSeekWhenPrepared);
            }
            Log.v(TAG, "surfaceChanged() start()");
            start();
        }
    }

    @Override
    public void onSurfaceDestroyed(SurfaceHolder holder) {
        // after we return from this we can't use the surface any more
        Log.v(TAG, "surfaceDestroyed(" + holder + ")");
        if (mMultiWindowListener != null) {
            mMultiWindowListener.onSurfaceDestroyed();
        }
        mSurfaceHolder = null;
        release(true);
    }

    // / SlowMotion {@
    public void setSlowMotionSpeed(int speed) {
        Log.v(TAG, "setSlowMotionSpeed " + speed);
        /*if (mMediaPlayer != null && mEnableSlowMotionSpeed && speed != 0) {
            mMediaPlayer.setParameter(SlowMotionItem.KEY_SLOW_MOTION_SPEED,
                    speed);
        } else {*/
            Log.d(TAG, "setSlowMotionSpeed fail, set mEnableSlowMotionSpeed false");
            mEnableSlowMotionSpeed = false;
//        }
//        mSlowMotionSpeed = speed;
    }

    /*public void setSlowMotionSection(String section) {
        Log.v(TAG, "setSlowMotionSection " + section);
        if (mMediaPlayer != null) {
            mMediaPlayer.setParameter(SlowMotionItem.KEY_SLOW_MOTION_SECTION,
                    section);
        }
        mSlowMotionSection = section;
    }*/

    public void enableSlowMotionSpeed() {
        if (!mEnableSlowMotionSpeed) {
            Log.v(TAG, "enableSlowMotionSpeed");
            mEnableSlowMotionSpeed = true;
            setSlowMotionSpeed(mSlowMotionSpeed);
        }
    }

/*    public void disableSlowMotionSpeed() {
        if (mEnableSlowMotionSpeed) {
            Log.v(TAG, "disableSlowMotionSpeed");
            if (mMediaPlayer != null) {
                mMediaPlayer.setParameter(SlowMotionItem.KEY_SLOW_MOTION_SPEED,
                        SlowMotionItem.SLOW_MOTION_NORMAL_SPEED);
            }
            mEnableSlowMotionSpeed = false;
        }
    }*/

    /**
     * Query current video's fps from MediaPlayer. The fps is used to determine
     * slowmotion video's speed range. It only can be got after MediaPlayer
     * started
     *
     * @return videp's fps (30, 120, 240)
     */
    public int getFps() {
        String fps = null;
        /*if (mMediaPlayer != null) {
            fps = mMediaPlayer.getStringParameter(SlowMotionItem.KEY_SLOW_MOTION_FPS);
        }*/
        Log.v(TAG, "get fps is " + fps);
        /*if (fps == null || SlowMotionItem.NORMAL_VIDEO_FPS == Integer.parseInt(fps)) {
            mFps = SlowMotionItem.INVALID_FPS;
        } else {*/
            mFps = Integer.parseInt(fps);
//        }
        return mFps;
    }
    // / @}

    /// M: AMS can not ensure onStop will be executed immediately if
    //  MovieActivity not visible, in multi window mode, need call
    //  doOnPause to ensure finish release action if surface destroyed {@
    private MultiWindowListener mMultiWindowListener;

    public void setMultiWindowListener(MultiWindowListener listener) {
        this.mMultiWindowListener = listener;
    }

    public interface MultiWindowListener {
        public void onSurfaceDestroyed();
    }
    /// @}
}
