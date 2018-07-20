package com.video.cut.widget;

import android.animation.ValueAnimator;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.qiniu.pili.droid.shortvideo.PLShortVideoTrimmer;
import com.qiniu.pili.droid.shortvideo.PLVideoSaveListener;
import com.video.cut.R;
import com.video.cut.features.trim.VideoTrimmerAdapter;
import com.video.cut.interfaces.IVideoTrimmerView;
import com.video.cut.interfaces.SingleCallback;
import com.video.cut.interfaces.TrimVideoListener;
import com.video.cut.utils.BackgroundExecutor;
import com.video.cut.utils.TrimVideoUtil;
import com.video.cut.utils.UiThreadExecutor;

import java.io.File;

import static com.video.cut.utils.TrimVideoUtil.RECYCLER_VIEW_PADDING;
import static com.video.cut.utils.TrimVideoUtil.VIDEO_FRAMES_WIDTH;

public class VideoTrimmerView extends FrameLayout implements IVideoTrimmerView {

    private static final String TAG = VideoTrimmerView.class.getSimpleName();
    public OnClickListener onFinishListener;
    public PLShortVideoTrimmer mShortVideoTrimmer;
    Handler handler = new Handler();
    private int mMaxWidth = VIDEO_FRAMES_WIDTH;
    private Context mContext;
    private RelativeLayout mLinearVideo;
    private VideoView mVideoView;
    private ProgressBar videoLoading;
    private ImageView mPlayView;
    private RecyclerView mVideoThumbRecyclerView;
    private RangeSeekBarView mRangeSeekBarView;
    private LinearLayout mSeekBarLayout;
    private ImageView mRedProgressIcon;
    private TextView mVideoShootTipTv;
    private float mAverageMsPx;//每毫秒所占的px
    private float averagePxMs;//每px所占用的ms毫秒
    private String mSourceUri;
    private String mFinalPath;
    private TrimVideoListener mOnTrimVideoListener;
    private int mDuration = 0;
    private VideoTrimmerAdapter mVideoThumbAdapter;
    private boolean isFromRestore = true;
    //new
    private long mLeftProgressPos, mRightProgressPos;
    private long mRedProgressBarPos = 0;
    private long scrollPos = 0;
    private int mScaledTouchSlop;
    private int lastScrollX;
    private boolean isSeeking;
    private boolean isOverScaledTouchSlop;
    private final RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            Log.d(TAG, "newState = " + newState);
            mVideoView.pause();
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            isSeeking = false;
            int scrollX = calcScrollXDistance();
            //达不到滑动的距离
            if (Math.abs(lastScrollX - scrollX) < mScaledTouchSlop) {
                isOverScaledTouchSlop = false;
                return;
            }
            isOverScaledTouchSlop = true;
            //初始状态,why ? 因为默认的时候有35dp的空白！
            if (scrollX == -RECYCLER_VIEW_PADDING) {
                scrollPos = 0;
            } else {
                isSeeking = true;
                scrollPos = (long) (mAverageMsPx * (RECYCLER_VIEW_PADDING + scrollX));
                mLeftProgressPos = mRangeSeekBarView.getSelectedMinValue() + scrollPos;
                mRightProgressPos = mRangeSeekBarView.getSelectedMaxValue() + scrollPos;
                Log.d(TAG, "onScrolled >>>> mLeftProgressPos = " + mLeftProgressPos);
                Log.d(TAG, "onScrolled >>>> mRightProgressPos = " + mRightProgressPos);
                mRedProgressBarPos = mLeftProgressPos;
                if (mVideoView.isPlaying()) {
                    mVideoView.pause();
                    setPlayPauseViewIcon(false);
                }
                mRedProgressIcon.setVisibility(GONE);
                seekTo(mLeftProgressPos);
                mRangeSeekBarView.setStartEndTime(mLeftProgressPos, mRightProgressPos);
                mRangeSeekBarView.invalidate();
            }
            lastScrollX = scrollX;
        }
    };
    private int mThumbsTotalCount;
    private ValueAnimator mRedProgressAnimator;
    private Handler mAnimationHandler = new Handler();
    private Runnable mAnimationRunnable = new Runnable() {

        @Override
        public void run() {
            updateVideoProgress();
        }
    };
    private final RangeSeekBarView.OnRangeSeekBarChangeListener mOnRangeSeekBarChangeListener = new RangeSeekBarView.OnRangeSeekBarChangeListener() {
        @Override
        public void onRangeSeekBarValuesChanged(RangeSeekBarView bar, long minValue, long maxValue, int action, boolean isMin,
                                                RangeSeekBarView.Thumb pressedThumb) {
            // Log.d(TAG, "-----minValue----->>>>>>" + minValue);
            // Log.d(TAG, "-----maxValue----->>>>>>" + maxValue);
            Log.e(TAG, "-----scrollPos mLeftProgressPos----->>>>>>" + minValue);
            Log.e(TAG, "-----scrollPos mRightProgressPos----->>>>>>" + maxValue);
            mLeftProgressPos = minValue + scrollPos;
            mRedProgressBarPos = mLeftProgressPos;
            mRightProgressPos = maxValue + scrollPos;
            Log.d(TAG, "-----mLeftProgressPos----->>>>>>" + mLeftProgressPos);
            Log.d(TAG, "-----mRightProgressPos----->>>>>>" + mRightProgressPos);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    isSeeking = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    isSeeking = true;
                    seekTo((int) (pressedThumb == RangeSeekBarView.Thumb.MIN ? mLeftProgressPos : mRightProgressPos));
                    break;
                case MotionEvent.ACTION_UP:
                    isSeeking = false;
                    seekTo((int) mLeftProgressPos);
                    break;
                default:
                    break;
            }
            mRangeSeekBarView.setStartEndTime(mLeftProgressPos, mRightProgressPos);
            onVideoPlay();
            //PLShortVideoTrimmer

        }
    };

    public VideoTrimmerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoTrimmerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        LayoutInflater.from(context).inflate(R.layout.video_trimmer_view, this, true);

        mLinearVideo = findViewById(R.id.layout_surface_view);
        mVideoView = findViewById(R.id.video_loader);
        videoLoading = findViewById(R.id.video_loading);

        mPlayView = findViewById(R.id.icon_video_play);
        mSeekBarLayout = findViewById(R.id.seekBarLayout);
        mRedProgressIcon = findViewById(R.id.positionIcon);
        mVideoShootTipTv = findViewById(R.id.video_shoot_tip);
        mVideoThumbRecyclerView = findViewById(R.id.video_frames_recyclerView);
        mVideoThumbRecyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
        mVideoThumbAdapter = new VideoTrimmerAdapter(mContext);
        mVideoThumbRecyclerView.setAdapter(mVideoThumbAdapter);
        mVideoThumbRecyclerView.addOnScrollListener(mOnScrollListener);
        mSeekBarLayout.setVisibility(GONE);
        mVideoShootTipTv.setVisibility(GONE);
        setUpListeners();
    }

    private void initRangeSeekBarView() {
        int rangeWidth;
        mLeftProgressPos = 0;
        if (mDuration <= TrimVideoUtil.MAX_SHOOT_DURATION) {
            //mThumbsTotalCount = (int) Math.ceil(mDuration * 1.0 / 1000) ;
            mThumbsTotalCount = TrimVideoUtil.MAX_COUNT_RANGE;
            rangeWidth = mMaxWidth;
            mRightProgressPos = mDuration;

        } else {
            //mThumbsTotalCount = (int) Math.floor(mDuration * 1.0f / (TrimVideoUtil.MAX_SHOOT_DURATION * 1.0f) * TrimVideoUtil.MAX_COUNT_RANGE);
            mThumbsTotalCount = (int) (mDuration * 1.0f / (TrimVideoUtil.MAX_SHOOT_DURATION * 1.0f) * TrimVideoUtil.MAX_COUNT_RANGE);
            //重新设置ItmeWidth
            //MAX_COUNT_RANGE = Integer.valueOf(String.valueOf(mDuration / 1000));
            //mItemWidth = (int) Math.ceil(maxWidth / MAX_COUNT_RANGE);
            rangeWidth = mMaxWidth / TrimVideoUtil.MAX_COUNT_RANGE * mThumbsTotalCount;
            mRightProgressPos = TrimVideoUtil.MAX_SHOOT_DURATION;
        }
        mVideoThumbRecyclerView.addItemDecoration(new SpacesItemDecoration2(RECYCLER_VIEW_PADDING, mThumbsTotalCount));

        mRangeSeekBarView = new RangeSeekBarView(mContext, mLeftProgressPos, mRightProgressPos);
        mRangeSeekBarView.setSelectedMinValue(mLeftProgressPos);
        mRangeSeekBarView.setSelectedMaxValue(mRightProgressPos);

        mRangeSeekBarView.setStartEndTime(mLeftProgressPos, mRightProgressPos);
        mRangeSeekBarView.setMinShootTime(TrimVideoUtil.MIN_SHOOT_DURATION);
        mRangeSeekBarView.setNotifyWhileDragging(true);
        mRangeSeekBarView.setOnRangeSeekBarChangeListener(mOnRangeSeekBarChangeListener);
        mSeekBarLayout.addView(mRangeSeekBarView);

//        //防止不足空隙
//        if (mDuration <= TrimVideoUtil.MAX_SHOOT_DURATION) {
//            int tempDuration = (int) Math.floor(mDuration * 1.0 / 1000) * 1000;
//            mAverageMsPx = tempDuration * 1.0f / rangeWidth * 1.0f;
//        } else {
//            mAverageMsPx = mDuration * 1.0f / (rangeWidth - RECYCLER_VIEW_PADDING) * 1.0f;
//        }

        mAverageMsPx = mDuration * 1.0f / rangeWidth * 1.0f;
        averagePxMs = (mMaxWidth * 1.0f / (mRightProgressPos - mLeftProgressPos));
        //mAverageMsPx = mDuration * 1.0f / rangeWidth * 1.0f;
        //  averagePxMs = (mMaxWidth * 1.0f / (mRightProgressPos - mLeftProgressPos));
    }

    @SuppressLint("StringFormatInvalid")
    public void initVideoByURI(final String videoURI) {
        try {
            mSourceUri = videoURI;
            mVideoView.setVideoPath(videoURI);
            mVideoView.requestFocus();
            mVideoShootTipTv.setText(String.format(mContext.getResources().getString(R.string.video_shoot_tip), TrimVideoUtil.VIDEO_MAX_TIME));
            mVideoView.start();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDuration = mVideoView.getDuration();
                    initRangeSeekBarView();
                    startShootVideoThumbs(mContext, mSourceUri, mThumbsTotalCount, 0, mDuration);
                }
            }, 500);

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
//                                Bitmap b;
//                                if (bitmap == null && mVideoThumbAdapter.getmBitmaps().size() != 0) {
//                                    int size = mVideoThumbAdapter.getmBitmaps().size();
//                                    b = mVideoThumbAdapter.getmBitmaps().get(size - 1);
//                                } else {
//                                    b = bitmap;
//                                }
                                if (totalThumbsCount <= mVideoThumbAdapter.getmBitmaps().size()) {
                                    return;
                                } else {
                                    mVideoThumbAdapter.addBitmaps(bitmap);
                                }


                                if (mSeekBarLayout.getVisibility() == GONE) {
                                    mSeekBarLayout.setVisibility(VISIBLE);
                                }
                                if (mVideoShootTipTv.getVisibility() == GONE) {
                                    mVideoShootTipTv.setVisibility(VISIBLE);
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
        if (mShortVideoTrimmer != null) {
            mShortVideoTrimmer.cancelTrim();
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
        //监听视频播放完的代码  相当于循环播放
        //mVideoView.start();
//        if (onVideoViewListener != null) {
//            onVideoViewListener.onCompleted();
//        }
    }

    private void onVideoReset() {
        mVideoView.pause();
        setPlayPauseViewIcon(false);
    }

//    public OnVideoViewListener getOnVideoViewListener() {
//        return onVideoViewListener;
//    }
//
//    public void setOnVideoViewListener(OnVideoViewListener onVideoViewListener) {
//        this.onVideoViewListener = onVideoViewListener;
//    }

    private void playVideoOrPause() {
        mRedProgressBarPos = mVideoView.getCurrentPosition();
        if (mVideoView.isPlaying()) {
            mVideoView.pause();
            pauseRedProgressAnimation();
        } else {
            mVideoView.start();
            playingRedProgressAnimation();
        }
        setPlayPauseViewIcon(mVideoView.isPlaying());
        videoLoading.setVisibility(GONE);
//        if (onVideoViewListener != null) {
//            onVideoViewListener.onStart();
//        }
    }

    public void onVideoPlay() {
        mVideoView.start();
        playingRedProgressAnimation();
    }

    public void onVideoPause() {
        if (mVideoView.isPlaying()) {
            seekTo(mLeftProgressPos);//复位
            mVideoView.pause();
            setPlayPauseViewIcon(false);
            mRedProgressIcon.setVisibility(GONE);
        }
    }

    public void setOnTrimVideoListener(TrimVideoListener onTrimVideoListener) {
        mOnTrimVideoListener = onTrimVideoListener;
    }

    private void setUpListeners() {
        findViewById(R.id.cancelBtn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancelClicked();
            }
        });

        findViewById(R.id.finishBtn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onSaveClicked();
            }
        });
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
        mPlayView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                playVideoOrPause();
            }
        });
    }

    public void setFinishListener(OnClickListener onClickListener) {
        this.onFinishListener = onClickListener;
    }

    private void onSaveClicked() {
        if (mRightProgressPos - mLeftProgressPos < TrimVideoUtil.MIN_SHOOT_DURATION) {
            Toast.makeText(mContext, "视频长不足3秒,无法上传", Toast.LENGTH_SHORT).show();
        } else {
            mVideoView.pause();
            // TrimVideoUtil.trim(mContext, mSourceUri, getTrimmedVideoPath(), mLeftProgressPos, mRightProgressPos, mOnTrimVideoListener);
            onDone(mContext, mSourceUri, getTrimmedVideoPath(), mLeftProgressPos, mRightProgressPos, mOnTrimVideoListener);
        }
    }

    public void onSaveClicked(String outFile) {
        if (mRightProgressPos - mLeftProgressPos < TrimVideoUtil.MIN_SHOOT_DURATION) {
            Toast.makeText(mContext, "视频长不足3秒,无法上传", Toast.LENGTH_SHORT).show();
            mOnTrimVideoListener.onFailed();
        } else {
            mVideoView.pause();
            String[] videoWHDA = TrimVideoUtil.videoWHDA(mSourceUri);
//            if (Long.valueOf(videoWHDA[2]) < 20000L) {
//                TrimVideoUtil.trim(mContext, mSourceUri, outFile, mLeftProgressPos, mRightProgressPos, mOnTrimVideoListener);
//                //TrimVideoUtil.trim(context, inputFile, outputFile, startMs, endMs, callback);
//            } else {
//
//            }
            mRightProgressPos -= 100;
            mLeftProgressPos -= 100;
            onDone(mContext, mSourceUri, outFile, mLeftProgressPos, mRightProgressPos, mOnTrimVideoListener);


//            mOnTrimVideoListener.onStartTrim();
//            boolean isSuceed = TrimVideoUtil.startclipDecodeVideo(mSourceUri, outFile, mLeftProgressPos, mRightProgressPos);
//            if (isSuceed) {
//                mOnTrimVideoListener.onFinishTrim(outFile);
//            } else {
//                mOnTrimVideoListener.onFailed();
//            }

        }
    }

    public void onDone(final Context context, final String inputFile, final String outputFile, final long startMs, final long endMs, final TrimVideoListener callback) {
        if (callback != null) {
            callback.onStartTrim();
        }
        mShortVideoTrimmer = new PLShortVideoTrimmer(context, inputFile, outputFile);
        PLShortVideoTrimmer.TRIM_MODE mode = PLShortVideoTrimmer.TRIM_MODE.ACCURATE;//: PLShortVideoTrimmer.TRIM_MODE.ACCURATE
        mShortVideoTrimmer.trim(startMs, endMs, mode, new PLVideoSaveListener() {
            @Override
            public void onSaveVideoSuccess(String path) {
                Log.e("TAG", "onDone" + path);
                if (callback != null) {
                    callback.onFinishTrim(path);
                }
            }

            @Override
            public void onSaveVideoFailed(final int errorCode) {
                //TrimVideoUtil.trim(context, inputFile, outputFile, startMs, endMs, callback);
                Log.e("TAG", "onDone" + errorCode);
                TrimVideoUtil.trim(context, inputFile, outputFile, startMs, endMs, callback);
//                if (callback != null)
//                    callback.onFailed();
            }

            @Override
            public void onSaveVideoCanceled() {
                if (callback != null)
                    callback.onCancel();
            }

            @Override
            public void onProgressUpdate(final float percentage) {
                if (callback != null)
                    callback.onProgressUpdate(percentage);
            }
        });
        //  mShortVideoTrimmer.cancelTrim();
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
        mPlayView.setImageResource(isPlaying ? R.drawable.icon_video_pause_black : R.drawable.icon_video_play_black);
    }

    /**
     * 水平滑动了多少px
     */
    private int calcScrollXDistance() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) mVideoThumbRecyclerView.getLayoutManager();
        int position = layoutManager.findFirstVisibleItemPosition();
        View firstVisibleChildView = layoutManager.findViewByPosition(position);
        int itemWidth = firstVisibleChildView.getWidth();
        return (position) * itemWidth - firstVisibleChildView.getLeft();
    }

    private void playingRedProgressAnimation() {
        pauseRedProgressAnimation();
        playingAnimation();
        mAnimationHandler.post(mAnimationRunnable);
    }

    @SuppressLint("NewApi")
    private void playingAnimation() {
        if (mRedProgressIcon.getVisibility() == View.GONE) {
            mRedProgressIcon.setVisibility(View.GONE);
        }
        final LayoutParams params = (LayoutParams) mRedProgressIcon.getLayoutParams();
        int start = (int) (RECYCLER_VIEW_PADDING + (mRedProgressBarPos - scrollPos) * averagePxMs);
        int end = (int) (RECYCLER_VIEW_PADDING + (mRightProgressPos - scrollPos) * averagePxMs);
        mRedProgressAnimator = ValueAnimator.ofInt(start, end).setDuration((mRightProgressPos - scrollPos) - (mRedProgressBarPos - scrollPos));
        mRedProgressAnimator.setInterpolator(new LinearInterpolator());
        mRedProgressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.leftMargin = (int) animation.getAnimatedValue();
                mRedProgressIcon.setLayoutParams(params);
                Log.d(TAG, "----onAnimationUpdate--->>>>>>>" + mRedProgressBarPos);
            }
        });
        mRedProgressAnimator.start();
    }

    private void pauseRedProgressAnimation() {
        mRedProgressIcon.clearAnimation();
        if (mRedProgressAnimator != null && mRedProgressAnimator.isRunning()) {
            mAnimationHandler.removeCallbacks(mAnimationRunnable);
            mRedProgressAnimator.cancel();
        }
    }

    private void updateVideoProgress() {
        long currentPosition = mVideoView.getCurrentPosition();
        Log.d(TAG, "updateVideoProgress currentPosition = " + currentPosition);
        if (currentPosition >= (mRightProgressPos)) {
            mRedProgressBarPos = mLeftProgressPos;
            pauseRedProgressAnimation();
            onVideoPause();
        } else {
            mAnimationHandler.post(mAnimationRunnable);
        }
    }

    /**
     * Cancel trim thread execut action when finish
     */
    @Override
    public void onDestroy() {
        mOnTrimVideoListener = null;
        if (TrimVideoUtil.mShortVideoTrimmer != null) {
            TrimVideoUtil.mShortVideoTrimmer.destroy();
        }
        BackgroundExecutor.cancelAll("", true);
        UiThreadExecutor.cancelAll("");
    }
//
//    public interface OnVideoViewListener {
//        void onStart();
//
//        void onCompleted();
//    }
}
