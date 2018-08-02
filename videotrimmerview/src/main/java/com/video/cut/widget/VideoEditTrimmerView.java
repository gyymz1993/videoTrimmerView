package com.video.cut.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.video.cut.R;
import com.video.cut.features.trim.VideoEditTrimmerAdapter;
import com.video.cut.interfaces.IVideoTrimmerView;
import com.video.cut.interfaces.SingleCallback;
import com.video.cut.interfaces.TrimVideoListener;
import com.video.cut.utils.BackgroundExecutor;
import com.video.cut.utils.DeviceUtil;
import com.video.cut.utils.TrimVideoUtil;
import com.video.cut.utils.UiThreadExecutor;
import com.video.cut.utils.UnitConverter;

import java.io.File;

public class VideoEditTrimmerView extends FrameLayout implements IVideoTrimmerView {

    private static final String TAG = VideoEditTrimmerView.class.getSimpleName();
    public static long MAX_SHOOT_DURATION = 10 * 1000L;//视频最多剪切多长时间10s
    public OnClickListener onFinishListener;
    Handler handler = new Handler();
    private int mMaxWidth = DeviceUtil.getDeviceWidth();
    private Context mContext;
    private RelativeLayout mLinearVideo;
    private VideoView mVideoView;
    private ProgressBar videoLoading;
    private TextView id_tv_progress;
    private RecyclerView mVideoThumbRecyclerView;
    private float mAverageMsPx;//每毫秒所占的px
    private String mSourceUri;
    private String mFinalPath;
    private TrimVideoListener mOnTrimVideoListener;
    private int mDuration = 0;
    private VideoEditTrimmerAdapter mVideoThumbAdapter;
    private boolean isFromRestore = true;
    private long mLeftProgressPos;
    private long mRedProgressBarPos = 0;
    private long scrollPos = 0;
    private int lastScrollX;
    private final RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                seekTo(mLeftProgressPos);
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            int scrollX = calcScrollXDistance();
            Log.d(TAG, "onScrolled >>>> scrollX = " + scrollX);
            //达不到滑动的距离
            if (Math.abs(lastScrollX - scrollX) < 0) {
                return;
            }
            scrollPos = (long) (mAverageMsPx * scrollX);
            if (scrollPos > mDuration) {
                scrollPos = mDuration;
            }
            double progress = Math.ceil(scrollPos / 10.0 / 10.0) / 10;
            id_tv_progress.setText(progress + "");
            lastScrollX = scrollX;
        }
    };
    private int mThumbsTotalCount;

    public VideoEditTrimmerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoEditTrimmerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        LayoutInflater.from(context).inflate(R.layout.video_edit_trimmer_view, this, true);
        mLinearVideo = findViewById(R.id.layout_surface_view);
        mVideoView = findViewById(R.id.video_loader);
        videoLoading = findViewById(R.id.video_loading);
        id_tv_progress = findViewById(R.id.id_tv_progress);

        mVideoThumbRecyclerView = findViewById(R.id.video_frames_recyclerView);
        mVideoThumbRecyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
        mVideoThumbAdapter = new VideoEditTrimmerAdapter(mContext);
        mVideoThumbAdapter.addHeaderView();
        mVideoThumbAdapter.addFooterView();
        mVideoThumbRecyclerView.setAdapter(mVideoThumbAdapter);
        mVideoThumbRecyclerView.addOnScrollListener(mOnScrollListener);
        setUpListeners();
    }

    private void initRangeSeekBarView() {
        float rangeWidth;
        mLeftProgressPos = 0;
        if (mDuration <= MAX_SHOOT_DURATION) {
            mThumbsTotalCount = TrimVideoUtil.MAX_COUNT_RANGE;
            rangeWidth = mMaxWidth;
        } else {
            mThumbsTotalCount = (int) (mDuration * 1.0f / (MAX_SHOOT_DURATION * 1.0f) * TrimVideoUtil.MAX_COUNT_RANGE);
            rangeWidth = mMaxWidth * 1.0f / TrimVideoUtil.MAX_COUNT_RANGE * mThumbsTotalCount;
        }
        //有一定的误差
        rangeWidth += (int) Math.ceil(DeviceUtil.getDeviceWidth() / TrimVideoUtil.MAX_COUNT_RANGE);
        mAverageMsPx = mDuration * 1.0f / rangeWidth;
    }

    @SuppressLint("StringFormatInvalid")
    public void initVideoByURI(final String videoURI) {
        try {
            mSourceUri = videoURI;
            mVideoView.setVideoPath(videoURI);
            mVideoView.requestFocus();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDuration = mVideoView.getDuration();
                    initRangeSeekBarView();
                    startShootVideoThumbs(mContext, mSourceUri, mThumbsTotalCount, 0, mDuration);
                }
            }, 500);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mVideoView.start();
                }
            }, 1000);

        } catch (Exception e) {

        }

    }

    private void startShootVideoThumbs(final Context context, final String videoUri, final int totalThumbsCount, long startPosition, long endPosition) {
        TrimVideoUtil.backgroundShootVideoThumb(context, videoUri, totalThumbsCount, startPosition, endPosition,
                new SingleCallback<Bitmap, Integer>() {
                    @Override
                    public void onSingleCallback(final Bitmap bitmap, final Integer interval) {
                        UiThreadExecutor.runTask("", new Runnable() {
                            @Override
                            public void run() {
                                Bitmap b;
                                if (totalThumbsCount <= mVideoThumbAdapter.getmBitmaps().size()) {
                                    return;
                                } else {
                                    if (null == bitmap) {
                                        b = mVideoThumbAdapter.getmBitmaps().get(mVideoThumbAdapter.getmBitmaps().size() - 1);
                                    } else {
                                        b = bitmap;
                                    }
                                    mVideoThumbAdapter.addBitmaps(b);
                                }
                            }
                        }, 0L);
                    }
                });
    }

    public void onCancelClicked() {
        if (mOnTrimVideoListener != null) {
            mOnTrimVideoListener.onCancel();
        }
    }

    private void videoPrepared(MediaPlayer mp) {
        ViewGroup.LayoutParams lp = mVideoView.getLayoutParams();
        int videoWidth = mp.getVideoWidth();
        int videoHeight = mp.getVideoHeight();
        float videoProportion = (float) videoWidth / (float) videoHeight;
        int screenWidth = mLinearVideo.getWidth();
        int screenHeight = mLinearVideo.getHeight();
        float screenProportion = (float) screenWidth / (float) screenHeight;

        if (videoProportion > screenProportion) {
            lp.width = screenWidth;
            lp.height = (int) ((float) screenWidth / videoProportion);
        } else {
            lp.width = (int) (videoProportion * (float) screenHeight);
            lp.height = screenHeight;
        }
        mVideoView.setLayoutParams(lp);
        mDuration = mVideoView.getDuration();
        if (!getRestoreState()) {
            seekTo((int) mRedProgressBarPos);
        } else {
            setRestoreState(false);
            seekTo((int) mRedProgressBarPos);
        }

    }

    private void videoCompleted() {
        seekTo(mLeftProgressPos);
        setPlayPauseViewIcon(false);
    }

    private void onVideoReset() {
        mVideoView.pause();
        setPlayPauseViewIcon(false);
    }


    private void playVideoOrPause() {
        mRedProgressBarPos = mVideoView.getCurrentPosition();
        if (mVideoView.isPlaying()) {
            mVideoView.pause();
        } else {
            mVideoView.start();
        }
        setPlayPauseViewIcon(mVideoView.isPlaying());
        videoLoading.setVisibility(GONE);
    }

    public void onVideoPlay() {
        mVideoView.start();
    }

    public void onVideoPause() {
        if (mVideoView.isPlaying()) {
            seekTo(mLeftProgressPos);//复位
            mVideoView.pause();
            setPlayPauseViewIcon(false);
        }
    }

    public void setOnTrimVideoListener(TrimVideoListener onTrimVideoListener) {
        mOnTrimVideoListener = onTrimVideoListener;
    }

    private void setUpListeners() {
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                videoPrepared(mp);
            }
        });
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                videoCompleted();
            }
        });
    }

    public void setFinishListener(OnClickListener onClickListener) {
        this.onFinishListener = onClickListener;
    }


    @Deprecated
    public void onSaveClicked(String outFile) {
        mVideoView.pause();
        mOnTrimVideoListener.onStartTrim();
    }


    private String getTrimmedVideoPath() {
        if (mFinalPath == null) {
            File file = mContext.getExternalCacheDir();
            if (file != null) mFinalPath = file.getAbsolutePath();
        }
        return mFinalPath;
    }

    private void seekTo(long msec) {
        mVideoView.seekTo((int) msec);
    }

    private boolean getRestoreState() {
        return isFromRestore;
    }

    public void setRestoreState(boolean fromRestore) {
        isFromRestore = fromRestore;
    }

    private void setPlayPauseViewIcon(boolean isPlaying) {
        //mPlayView.setImageResource(isPlaying ? R.drawable.icon_video_pause_black : R.drawable.icon_video_play_black);
    }

//    /**
//     * 水平滑动了多少px
//     */
//    private int calcScrollXDistance() {
//        LinearLayoutManager layoutManager = (LinearLayoutManager) mVideoThumbRecyclerView.getLayoutManager();
//        int position = layoutManager.findFirstVisibleItemPosition();
//        View firstVisibleChildView = layoutManager.findViewByPosition(position);
//        int itemWidth = firstVisibleChildView.getWidth();
//        return (position) * itemWidth - firstVisibleChildView.getLeft();
//    }

    protected int calcScrollXDistance() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) mVideoThumbRecyclerView.getLayoutManager();
        int position = layoutManager.findFirstVisibleItemPosition();
        View firstVisibleChildView = layoutManager.findViewByPosition(position);
        int itemWidth = firstVisibleChildView.getWidth();
        //如果大于1加上前面head的宽度
        if (position >= 1) {
            return (position) * itemWidth - firstVisibleChildView.getLeft() + UnitConverter.getDisplayMetrics().widthPixels / 2;
        } else {
            return (position) * itemWidth - firstVisibleChildView.getLeft();
        }
    }


    private void updateVideoProgress() {
        long currentPosition = mVideoView.getCurrentPosition();
    }

    /**
     * Cancel trim thread execut action when finish
     */
    @Override
    public void onDestroy() {
        mOnTrimVideoListener = null;
        BackgroundExecutor.cancelAll("", true);
        UiThreadExecutor.cancelAll("");
    }
}
