package com.video.cut.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.video.cut.interfaces.CompressVideoListener;
import com.video.cut.interfaces.SingleCallback;
import com.video.cut.interfaces.TrimVideoListener;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;


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
    public static int count = 0;
    static TrimVideoListener mcallback;
    static android.os.Handler handler = new android.os.Handler();
    static Runnable runnable = new Runnable() {
        @Override
        public void run() {
            count++;
            handler.postDelayed(this, 1000);
            if (count > 100) {
                handler.removeCallbacks(runnable);
                count = 0;
                if (mcallback != null) {
                    mcallback.onFailed();
                    mcallback = null;
                }
            }
        }
    };
    static Timer timer;
    static int cnt = 0;
    static TimerTask timerTask;
    static CompressVideoListener compressVideoListener;

    public static void trim(Context context, String inputFile, final String outputFile, long startMs, long endMs, final TrimVideoListener callback) {
        //  final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        //  final String outputName = "trimmedVideo_" + timeStamp + ".mp4";
        // outputFile = outputFile + "/" + outputName;
        mcallback = callback;
        String start = convertSecondsToTime(startMs / 1000);
        String duration = convertSecondsToTime((endMs - startMs) / 1000);

        double startTime = startMs / 10.0 / 10.0 / 10;
        double induration = (endMs - startMs) / 10.0 / 10.0 / 10;
        if (induration < 3.0) {
            induration = 3.0;
        }
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
         ffmpeg -i input.wav -c:a libfaac -q:a 330 -cutoff 15000 output.m4a
         */
        /**
         * 会减少几秒
         */
        //String cmd = "-ss " + start + " -i " + inputFile + " -ss 0" + " -t " + duration + " -c copy -map 0 " + outputFile;
        String cmd = "-i " + inputFile + " -ss " + startTime + " -t " + induration + " -c copy -map 0 " + outputFile;
        // ffmpeg -i source.mp4 -ss 577.92 -t 11.98 -c copy -map 0 clip1.mp4
        //  String cmd = "-ss " + start + " -t " + duration + " -i " + inputFile + " -vcodec copy -acodec copy " + outputFile;
        String[] command = cmd.split(" ");
        count = 0;
        handler.post(runnable);
        try {
            // final String tempOutFile = outputFile;
            FFmpeg.getInstance(context).execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onSuccess(String s) {
                    Log.e("TAG", s + "----");
                    handler.removeCallbacks(runnable);
                    count = 0;
                    if (callback != null) {
                        callback.onFinishTrim(outputFile);
                    }
                }

                @Override
                public void onStart() {
                    if (callback != null) {
                        callback.onStartTrim();
                    }
                }

                @Override
                public void onFailure(String message) {
                    super.onFailure(message);
                    Log.e("TAG", message + "----");
                }

                @Override
                public void onProgress(String message) {
                    super.onProgress(message);
                    Log.e("TAG", message + "----");
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

    public static void compress(Context context, String inputFile, String outputFile, final CompressVideoListener callback) {
        compressVideoListener = callback;
        if (timer == null) {
            timer = new Timer();
        }
        timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        cnt++;
                        if (cnt > 100) {
                            if (compressVideoListener != null) {
                                compressVideoListener.onFailure("");
                            }
                            timer.cancel();
                            timerTask.cancel();
                            timer = null;
                        }
                    }
                });
            }
        };
        //timer.schedule(timerTask, 0, 1000);
        // String cmd = "-threads 2 -y -i " + inputFile + " -strict -2 -vcodec libx264 -preset ultrafast -crf 28 -acodec copy -ac 2 " + outputFile;
        String cmd = " -threads 4 -i " + inputFile + " -r 29.97 -vcodec libx264 -s 480x272 -flags +loop -cmp chroma -deblockalpha 0 -deblockbeta 0 -crf 28 -bt 256k -refs 1 -coder 0 -me umh -me_range 16 -subq 5 -partitions parti4x4+parti8x8+partp8x8 -g 250 -keyint_min 25 -level 30 -qmin 10 -qmax 51 -trellis 2 -sc_threshold 40 -i_qfactor 0.71 -acodec libfaac -ab 128k -ar 48000 -ac 2 " + outputFile;
        String[] command = cmd.split(" ");
        try {
            FFmpeg.getInstance(context).execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String msg) {
                    Log.e("TAG", msg + "----");
                    timer.cancel();
                    timerTask.cancel();
                    timer = null;
                    callback.onFailure("Compress video failed!");
                    callback.onFinish();
                }

                @Override
                public void onSuccess(String msg) {
                    Log.e("TAG", msg + "----");
                    timer.cancel();
                    timerTask.cancel();
                    timer = null;
                    callback.onSuccess("Compress video successed!");
                    callback.onFinish();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
            callback.onFailure("Compress video failed!");
            timer.cancel();
            timerTask.cancel();
            timer = null;
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
