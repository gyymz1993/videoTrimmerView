package com.video.cut.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.qiniu.pili.droid.shortvideo.PLShortVideoTrimmer;
import com.qiniu.pili.droid.shortvideo.PLVideoSaveListener;
import com.video.cut.interfaces.SingleCallback;
import com.video.cut.interfaces.TrimVideoListener;

import java.util.HashMap;


public class TrimVideoUtil {

    public static final int VIDEO_MAX_TIME = 10;// 10秒
    public static final int MAX_COUNT_RANGE = 10;  //seekBar的区域内一共有多少张图片
    public static final int RECYCLER_VIEW_PADDING = UnitConverter.dpToPx(35);
    private static final String TAG = TrimVideoUtil.class.getSimpleName();
    private static final int SCREEN_WIDTH_FULL = DeviceUtil.getDeviceWidth();
    public static final int VIDEO_FRAMES_WIDTH = SCREEN_WIDTH_FULL - RECYCLER_VIEW_PADDING * 2;
    private static final int THUMB_WIDTH = (SCREEN_WIDTH_FULL - RECYCLER_VIEW_PADDING * 2) / VIDEO_MAX_TIME;
    private static final int THUMB_HEIGHT = UnitConverter.dpToPx(50);
    public static long MIN_SHOOT_DURATION = 3000L;// 最小剪辑时间3s
    public static long MAX_SHOOT_DURATION = VIDEO_MAX_TIME * 6000L;//视频最多剪切多长时间10s
    public static PLShortVideoTrimmer mShortVideoTrimmer;
    public static int count = 0;
    static TrimVideoListener mcallback;
    static android.os.Handler handler = new android.os.Handler();
    static Runnable runnable = new Runnable() {
        @Override
        public void run() {
            count++;
            handler.postDelayed(this, 1000);
            if (count > 10) {
                handler.removeCallbacks(runnable);
                count = 0;
                if (mcallback != null) {
                    mcallback.onFailed();
                    mcallback = null;
                }
            }
        }
    };

    public static void trim(Context context, String inputFile, final String outputFile, long startMs, long endMs, final TrimVideoListener callback) {
        //  final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        //  final String outputName = "trimmedVideo_" + timeStamp + ".mp4";
        // outputFile = outputFile + "/" + outputName;
        mcallback = callback;
        String start = convertSecondsToTime(startMs / 1000);
        String duration = convertSecondsToTime((endMs - startMs) / 1000);
        long induration = (endMs - startMs) / 1000;

        /** 裁剪视频ffmpeg指令说明：
         * ffmpeg -ss START -t DURATION -i INPUT -vcodec copy -acodec copy OUTPUT
         -ss 开始时间，如： 00:00:20，表示从20秒开始；
         -t 时长，如： 00:00:10，表示截取10秒长的视频；
         -i 输入，后面是空格，紧跟着就是输入视频文件；
         -vcodec copy 和 -acodec copy 表示所要使用的视频和音频的编码格式，这里指定为copy表示原样拷贝；
         INPUT，输入视频文件；
         OUTPUT，输出视频文件
         ffmpeg -ss 0:1:30 -t 0:0:20 -i input.avi -vcodec copy -acodec copy output.avi    //剪切视频

         目前剪切clip出来的视频时长不太对。
         比如把一个5分钟视频剪切到2分钟，剪切后视频能播放的长度是2分钟，但视频显示时长是5分钟。
         看源码命令行是 ffmpeg -ss 0.0 -t 120 " + "-accurate_seek -i " + mPath + " -vcodec copy -acodec copy " + tempVideoPath + " -y",
         后来改成ffmpeg -ss 0.0 -t 120 " + "-accurate_seek -i " + mPath + " -vcodec copy -acodec copy -to 120 " + tempVideoPath + " -y"，后面加了个-to限制输出视频长度就正常了。

         */
        //ffmpeg -ss 0.0 -t 120 " + "-accurate_seek -i " + mPath + " -vcodec copy -acodec copy -to 120 " + tempVideoPath + " -y"
      //  String cmd = "-ss " + start + " -t " + duration + " -i " + inputFile + " -vcodec copy -acodec copy to " + induration + " " + outputFile;

        String cmd = "-ss " + start + " -t " + duration + " -i " + inputFile + " -vcodec copy -acodec copy " + outputFile;
        String[] command = cmd.split(" ");
        count = 0;
        handler.post(runnable);
        try {
            // final String tempOutFile = outputFile;
            FFmpeg.getInstance(context).execute(command, new ExecuteBinaryResponseHandler() {

                @Override
                public void onSuccess(String s) {
                    handler.removeCallbacks(runnable);
                    count = 0;
                    if (callback != null) {
                        callback.onFinishTrim(outputFile);
                    }
                }

                @Override
                public void onStart() {
                    //callback.onStartTrim();
                    if (callback != null) {
                        callback.onStartTrim();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            handler.removeCallbacks(runnable);
            count = 0;
            if (callback != null) {
                callback.onFailed();
            }
        }
    }

    public static String[] videoWHDA(String videoUrl) {
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        if (videoUrl.startsWith("http") || videoUrl.startsWith("https")) {
            metadataRetriever.setDataSource(videoUrl, new HashMap());
        } else {
            metadataRetriever.setDataSource(videoUrl);
        }
        String width = metadataRetriever.extractMetadata(18);
        String height = metadataRetriever.extractMetadata(19);
        String duration = metadataRetriever.extractMetadata(9);
        //duration = String.valueOf(Double.valueOf(duration).doubleValue() / 1000.0D);
        String angle = metadataRetriever.extractMetadata(24);
        if (angle.equals("90") || angle.equals("270")) {
            String tempWidth = width;
            width = height;
            height = tempWidth;
        }

        metadataRetriever.release();
        return new String[]{width, height, duration, angle};
    }

    @SuppressLint("NewApi")
    public static void backgroundShootVideoThumb(final Context context, final String videoUri, final int totalThumbsCount, final long startPosition,
                                                 final long endPosition, final SingleCallback<Bitmap, Integer> callback) {
        BackgroundExecutor.execute(new BackgroundExecutor.Task("", 0L, "") {
            @Override
            public void execute() {
                try {
                    MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                    //如果是网络路径
                    if (videoUri.toString().startsWith("http") || videoUri.toString().startsWith("https")) {
                        mediaMetadataRetriever.setDataSource(videoUri.toString(), new HashMap());
                    } else {
                        mediaMetadataRetriever.setDataSource(videoUri.toString());
                    }
                    // Retrieve media data use microsecond
                    long interval = (endPosition - startPosition) / (totalThumbsCount - 1);
                    for (long i = 0; i < totalThumbsCount; ++i) {
                        long frameTime = startPosition + interval * i;
                        Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime(frameTime * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                        try {
                            bitmap = Bitmap.createScaledBitmap(bitmap, THUMB_WIDTH, THUMB_HEIGHT, false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        callback.onSingleCallback(bitmap, (int) interval);
                    }
                    mediaMetadataRetriever.release();
                } catch (final Throwable e) {
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                }
            }
        });
    }

    private static String convertSecondsToTime(long seconds) {
        String timeStr = null;
        int hour = 0;
        int minute = 0;
        int second = 0;
        if (seconds <= 0) {
            return "00:00";
        } else {
            minute = (int) seconds / 60;
            if (minute < 60) {
                second = (int) seconds % 60;
                timeStr = "00:" + unitFormat(minute) + ":" + unitFormat(second);
            } else {
                hour = minute / 60;
                if (hour > 99) return "99:59:59";
                minute = minute % 60;
                second = (int) (seconds - hour * 3600 - minute * 60);
                timeStr = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second);
            }
        }
        return timeStr;
    }

    private static String unitFormat(int i) {
        String retStr = null;
        if (i >= 0 && i < 10) {
            retStr = "0" + Integer.toString(i);
        } else {
            retStr = "" + i;
        }
        return retStr;
    }

    public static void onDone(final Context context, final String inputFile, final String outputFile, final long startMs, final long endMs, final TrimVideoListener callback) {
        if (callback != null) {
            callback.onStartTrim();
        }
        mShortVideoTrimmer = new PLShortVideoTrimmer(context, inputFile, outputFile);
        PLShortVideoTrimmer.TRIM_MODE mode = PLShortVideoTrimmer.TRIM_MODE.ACCURATE;//: PLShortVideoTrimmer.TRIM_MODE.ACCURATE
        mShortVideoTrimmer.trim(startMs, endMs, mode, new PLVideoSaveListener() {
            @Override
            public void onSaveVideoSuccess(String path) {
                callback.onFinishTrim(path);
            }

            @Override
            public void onSaveVideoFailed(final int errorCode) {
                trim(context, inputFile, outputFile, startMs, endMs, callback);
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


    public static boolean startclipDecodeVideo(String videoPath, String newVideoPath, final long startMs, final long endMs) {
        //起始时间  截取时长
        long clipDuration = endMs - startMs;
        boolean finish = new VideoDecoder().decodeVideo(videoPath, newVideoPath, startMs, clipDuration);
        if (finish) {
            // T_.showCustomToast("视频地址：" + newVideoPath);
            // L_.e(newVideoPath + "startDecodeVideo 视频剪切成功");
            return true;
        } else {
            // L_.e(newVideoPath + "视频剪切失败");
            return false;
        }
    }
}
