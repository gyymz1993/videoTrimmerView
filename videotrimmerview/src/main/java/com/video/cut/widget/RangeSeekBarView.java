package com.video.cut.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.video.cut.R;
import com.video.cut.utils.TrimVideoUtil;
import com.video.cut.utils.UnitConverter;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class RangeSeekBarView extends View {
    public static final int INVALID_POINTER_ID = 255;
    public static final int ACTION_POINTER_INDEX_MASK = 0x0000ff00, ACTION_POINTER_INDEX_SHIFT = 8;
    private static final String TAG = RangeSeekBarView.class.getSimpleName();
    private static final int TextPositionY = UnitConverter.dpToPx(7);
    private static final int paddingTop = UnitConverter.dpToPx(10);
    private static final int paddingButtom = UnitConverter.dpToPx(25);
    private static final int textSize = UnitConverter.dpToPx(10);
    private static SimpleDateFormat msFormat = new SimpleDateFormat("m:s");
    private final Paint mVideoTrimTimePaintL = new Paint();
    private final Paint mVideoTrimTimePaintR = new Paint();
    private final Paint mShadow = new Paint();
    private final float padding = 0;
    Bitmap mControllerBitmap;
    private int mActivePointerId = INVALID_POINTER_ID;
    private long mMinShootTime = TrimVideoUtil.MIN_SHOOT_DURATION;
    private double absoluteMinValuePrim, absoluteMaxValuePrim;
    private double normalizedMinValue = 0d;//点坐标占总长度的比例值，范围从0-1
    private double normalizedMaxValue = 1d;//点坐标占总长度的比例值，范围从0-1
    private double normalizedMinValueTime = 0d;
    private double normalizedMaxValueTime = 1d;// normalized：规格化的--点坐标占总长度的比例值，范围从0-1
    private int mScaledTouchSlop;
    private Bitmap thumbImageLeft;
    private Bitmap thumbImageRight;
    private Bitmap thumbPressedImage;
    private Paint paint;
    private Paint rectPaint;
    private int thumbWidth;
    private float thumbHalfWidth;
    private double mStartPosition = 0;
    private double mEndPosition = 0;
    private float thumbPaddingTop = 0;
    private boolean isTouchDown;
    private float mDownMotionX;
    private boolean mIsDragging;
    private Thumb pressedThumb;
    private boolean isMin;
    private double min_width = 1;//最小裁剪距离
    private boolean notifyWhileDragging = false;
    private OnRangeSeekBarChangeListener mRangeSeekBarChangeListener;
    private int whiteColorRes = getContext().getResources().getColor(R.color.white);
    private int mControllerWidth;
    private int mControllerHeight;

    public RangeSeekBarView(Context context) {
        super(context);
    }

    public RangeSeekBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RangeSeekBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RangeSeekBarView(Context context, long absoluteMinValuePrim, long absoluteMaxValuePrim) {
        super(context);
        this.absoluteMinValuePrim = absoluteMinValuePrim;
        this.absoluteMaxValuePrim = absoluteMaxValuePrim;
        setFocusable(true);
        setFocusableInTouchMode(true);
        init();
        initControllBitmap();
        //setBackgroundColor(getResources().getColor(R.color.shadow_color));
    }

    public static String timeParseMinute(long duration) {
        try {
            return msFormat.format(Long.valueOf(duration));
        } catch (Exception var3) {
            var3.printStackTrace();
            return "0.0";
        }
    }

    public static String timeParse(long duration) {
        String time = "";
        if (duration > 1000L) {
            time = timeParseMinute(duration);
        } else {
            long minute = duration / 60000L;
            long seconds = duration % 60000L;
            long second = (long) Math.round((float) seconds / 1000.0F);
            if (minute < 10L) {
                time = time + "0";
            }
            time = time + minute + ":";
            if (second < 10L) {
                time = time + "0";
            }
            time = time + second;
        }

        return time;
    }

    private void init() {
        mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        thumbImageLeft = BitmapFactory.decodeResource(getResources(), R.drawable.icon_video_thumb);

        int width = thumbImageLeft.getWidth();
        int height = thumbImageLeft.getHeight();
        int newWidth = UnitConverter.dpToPx(16);
        int newHeight = UnitConverter.dpToPx(55);
        float scaleWidth = newWidth * 1.0f / width;
        float scaleHeight = newHeight * 1.0f / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        thumbImageLeft = Bitmap.createBitmap(thumbImageLeft, 0, 0, width, height - paddingTop, matrix, true);
        thumbImageRight = thumbImageLeft;
        thumbPressedImage = thumbImageLeft;
        thumbWidth = newWidth;
        thumbHalfWidth = thumbWidth / 2;
        int shadowColor = getContext().getResources().getColor(R.color.shadow_translucent);
        mShadow.setAntiAlias(true);
        mShadow.setColor(shadowColor);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint.setStyle(Paint.Style.FILL);
        rectPaint.setColor(whiteColorRes);

        mVideoTrimTimePaintL.setStrokeWidth(3);
        mVideoTrimTimePaintL.setARGB(255, 51, 51, 51);
        mVideoTrimTimePaintL.setTextSize(textSize);
        mVideoTrimTimePaintL.setAntiAlias(true);
        mVideoTrimTimePaintL.setColor(whiteColorRes);
        mVideoTrimTimePaintL.setTextAlign(Paint.Align.LEFT);

        mVideoTrimTimePaintR.setStrokeWidth(3);
        mVideoTrimTimePaintR.setARGB(255, 51, 51, 51);
        mVideoTrimTimePaintR.setTextSize(textSize);
        mVideoTrimTimePaintR.setAntiAlias(true);
        mVideoTrimTimePaintR.setColor(whiteColorRes);
        mVideoTrimTimePaintR.setTextAlign(Paint.Align.RIGHT);
    }

    private void initControllBitmap() {
        //设置控制点的图标
        mControllerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.buttom_icon);
        mControllerWidth = mControllerBitmap.getWidth();
        mControllerHeight = mControllerBitmap.getHeight();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 300;
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
            width = MeasureSpec.getSize(widthMeasureSpec);
        }
        int height = 120;
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {
            height = MeasureSpec.getSize(heightMeasureSpec);
        }
        setMeasuredDimension(width, height);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float bg_middle_left = 0;
        float bg_middle_right = getWidth() - getPaddingRight();
        float rangeL = normalizedToScreen(normalizedMinValue);
        float rangeR = normalizedToScreen(normalizedMaxValue);

        //只显示可编辑区域高亮
//        Rect leftRect = new Rect((int) bg_middle_left,  getHeight(), (int) rangeL, 0);
//        Rect rightRect = new Rect((int) rangeR,  getHeight(), (int) bg_middle_right, 0);

        //修改为选中高亮
        Rect leftRect = new Rect((int) bg_middle_left, 0, (int) rangeL, getHeight());
        Rect rightRect = new Rect((int) rangeR, 0, (int) bg_middle_right, getHeight());
        canvas.drawRect(leftRect, mShadow);
        canvas.drawRect(rightRect, mShadow);

        //头部和底部的矩形隐藏
        // canvas.drawRect(rangeL, thumbPaddingTop + paddingTop, rangeR, thumbPaddingTop + UnitConverter.dpToPx(2) + paddingTop, rectPaint);
        //canvas.drawRect(rangeL, getHeight() - UnitConverter.dpToPx(2), rangeR, getHeight(), rectPaint);

        drawThumb(normalizedToScreen(normalizedMinValue), false, canvas, true);
        drawThumb(normalizedToScreen(normalizedMaxValue), false, canvas, false);
        drawVideoTrimTimeText(canvas);
    }

    private void drawThumb(float screenCoord, boolean pressed, Canvas canvas, boolean isLeft) {
        canvas.drawBitmap(pressed ? thumbPressedImage : (isLeft ? thumbImageLeft : thumbImageRight), screenCoord - (isLeft ? 0 : thumbWidth), paddingTop,
                paint);
    }

    private void drawVideoTrimTimeText(Canvas canvas) {
        DecimalFormat df = new DecimalFormat("#.0");
        String leftThumbsTime;
        if (mStartPosition == 0) {
            leftThumbsTime = "0.0";
        } else {
            if (mStartPosition < 1) {
                leftThumbsTime = "" + mStartPosition;
            } else {
                leftThumbsTime = df.format(mStartPosition);
            }
        }
        String rightThumbsTime = df.format(mEndPosition);
        canvas.drawText(leftThumbsTime, normalizedToScreen(normalizedMinValue), TextPositionY, mVideoTrimTimePaintL);
        canvas.drawText(rightThumbsTime, normalizedToScreen(normalizedMaxValue), TextPositionY, mVideoTrimTimePaintR);
        drawVideoTrimBitmap(canvas);
    }

//    private void drawVideoTrimTimeText(Canvas canvas) {
//
////        DecimalFormat df = new DecimalFormat("#.0");
////        String leftThumbsTime;
////        if (mStartPosition == 0) {
////            leftThumbsTime = 0 + "";
////        } else {
////            if (mStartPosition < 1) {
////                leftThumbsTime = "" + mStartPosition;
////            } else {
////                leftThumbsTime = df.format(mStartPosition);
////            }
////        }
////        String rightThumbsTime = df.format(mEndPosition);
//
//        String leftThumbsTime = DateUtil.convertSecondsToTime(mStartPosition);
//        String rightThumbsTime = DateUtil.convertSecondsToTime(mEndPosition);
////    String leftThumbsTime = DateUtil.convertSecondsToTime(mStartPosition);
////    String rightThumbsTime = DateUtil.convertSecondsToTime(mEndPosition);
//        canvas.drawText(leftThumbsTime, normalizedToScreen(normalizedMinValue), TextPositionY, mVideoTrimTimePaintL);
//        canvas.drawText(rightThumbsTime, normalizedToScreen(normalizedMaxValue), TextPositionY, mVideoTrimTimePaintR);
//
//        drawVideoTrimBitmap(canvas);
//    }

    private void drawVideoTrimBitmap(Canvas canvas) {
//    canvas.drawBitmap(mControllerBitmap, canvas.getWidth() - mControllerWidth
//            , canvas.getHeight() - mControllerHeight, mVideoTrimTimePaintR);
//    canvas.drawBitmap(mControllerBitmap, canvas.getWidth() - mControllerWidth
//            , canvas.getHeight() - mControllerHeight, mVideoTrimTimePaintR);
        canvas.drawBitmap(mControllerBitmap, normalizedToScreen(normalizedMinValue) + thumbHalfWidth - (mControllerWidth / 2)
                , getHeight() - paddingButtom, mVideoTrimTimePaintL);
        canvas.drawBitmap(mControllerBitmap, normalizedToScreen(normalizedMaxValue) - thumbHalfWidth - (mControllerWidth / 2),
                getHeight() - paddingButtom, mVideoTrimTimePaintR);
        //canvas.drawText(leftThumbsTime, normalizedToScreen(normalizedMinValue), TextPositionY, mVideoTrimTimePaintL);
        //canvas.drawText(rightThumbsTime, normalizedToScreen(normalizedMaxValue), TextPositionY, mVideoTrimTimePaintR);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isTouchDown) {
            return super.onTouchEvent(event);
        }
        if (event.getPointerCount() > 1) {
            return super.onTouchEvent(event);
        }

        if (!isEnabled()) return false;
        if (absoluteMaxValuePrim <= mMinShootTime) {
            Log.e("TAG","absoluteMaxValuePrim"+absoluteMaxValuePrim);
            return super.onTouchEvent(event);
        }
        int pointerIndex;// 记录点击点的index
        final int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                //记住最后一个手指点击屏幕的点的坐标x，mDownMotionX
                mActivePointerId = event.getPointerId(event.getPointerCount() - 1);
                pointerIndex = event.findPointerIndex(mActivePointerId);
                mDownMotionX = event.getX(pointerIndex);
                // 判断touch到的是最大值thumb还是最小值thumb
                pressedThumb = evalPressedThumb(mDownMotionX);
                if (pressedThumb == null) return super.onTouchEvent(event);
                setPressed(true);// 设置该控件被按下了
                onStartTrackingTouch();// 置mIsDragging为true，开始追踪touch事件
                trackTouchEvent(event);
                attemptClaimDrag();
                if (mRangeSeekBarChangeListener != null) {
                    mRangeSeekBarChangeListener.onRangeSeekBarValuesChanged(this, getSelectedMinValue(), getSelectedMaxValue(), MotionEvent.ACTION_DOWN, isMin,
                            pressedThumb);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (pressedThumb != null) {
                    if (mIsDragging) {
                        trackTouchEvent(event);
                    } else {
                        // Scroll to follow the motion event
                        pointerIndex = event.findPointerIndex(mActivePointerId);
                        final float x = event.getX(pointerIndex);// 手指在控件上点的X坐标
                        // 手指没有点在最大最小值上，并且在控件上有滑动事件
                        if (Math.abs(x - mDownMotionX) > mScaledTouchSlop) {
                            setPressed(true);
                            Log.e(TAG, "没有拖住最大最小值");// 一直不会执行？
                            invalidate();
                            onStartTrackingTouch();
                            trackTouchEvent(event);
                            attemptClaimDrag();
                        }
                    }
                    if (notifyWhileDragging && mRangeSeekBarChangeListener != null) {
                        mRangeSeekBarChangeListener.onRangeSeekBarValuesChanged(this, getSelectedMinValue(), getSelectedMaxValue(), MotionEvent.ACTION_MOVE,
                                isMin, pressedThumb);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                }

                invalidate();
                if (mRangeSeekBarChangeListener != null) {
                    mRangeSeekBarChangeListener.onRangeSeekBarValuesChanged(this, getSelectedMinValue(), getSelectedMaxValue(), MotionEvent.ACTION_UP, isMin,
                            pressedThumb);
                }
                pressedThumb = null;// 手指抬起，则置被touch到的thumb为空
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                final int index = event.getPointerCount() - 1;
                // final int index = ev.getActionIndex();
                mDownMotionX = event.getX(index);
                mActivePointerId = event.getPointerId(index);
                invalidate();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate(); // see above explanation
                break;
            default:
                break;
        }
        return true;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mDownMotionX = ev.getX(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    private void trackTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 1) return;
        final int pointerIndex = event.findPointerIndex(mActivePointerId);// 得到按下点的index
        float x = 0;
        try {
            x = event.getX(pointerIndex);
        } catch (Exception e) {
            // Log.e(TAG, "trackTouchEvent return : " + x);
            return;
        }
        Log.e(TAG, "trackTouchEvent: " + event.getAction() + " x: " + x + "event.getX():" + event.getX());
        if (Thumb.MIN.equals(pressedThumb)) {
            // screenToNormalized(x)-->得到规格化的0-1的值
            //Log.e(TAG, "trackTouchEvent Thumb.MIN : " + x);
            setNormalizedMinValue(screenToNormalized(x, 0));
        } else if (Thumb.MAX.equals(pressedThumb)) {
            setNormalizedMaxValue(screenToNormalized(x, 1));
        }
    }

    private double screenToNormalized(float screenCoord, int position) {
        Log.e(TAG, "screenToNormalized screenCoord : " + screenCoord + "   ==:position:" + position);
        int width = getWidth();
        if (width <= 2 * padding) {
            // prevent division by zero, simply return 0.
            Log.e(TAG, "screenToNormalized screenCoord : " + screenCoord + "   ==:position:" + position + "return 0d");
            return 0d;
        } else {
            isMin = false;
            double current_width = screenCoord;
            float rangeL = normalizedToScreen(normalizedMinValue);
            float rangeR = normalizedToScreen(normalizedMaxValue);
            double min = mMinShootTime / (absoluteMaxValuePrim - absoluteMinValuePrim) * (width - thumbWidth * 2);
            if (absoluteMaxValuePrim > 5 * 60 * 1000) {//大于5分钟的精确小数四位
                DecimalFormat df = new DecimalFormat("0.0000");
                min_width = Double.parseDouble(df.format(min));
            } else {
                min_width = Math.round(min + 0.5d);
            }
            if (position == 0) {
                if (isInThumbRangeLeft(screenCoord, normalizedMinValue, 0.5)) {
                    return normalizedMinValue;
                }

                float rightPosition = (getWidth() - rangeR) >= 0 ? (getWidth() - rangeR) : 0;
                double left_length = getValueLength() - (rightPosition + min_width);

                if (current_width > rangeL) {
                    current_width = rangeL + (current_width - rangeL);
                } else if (current_width <= rangeL) {
                    current_width = rangeL - (rangeL - current_width);
                }

                if (current_width > left_length) {
                    isMin = true;
                    current_width = left_length;
                }

//                if (current_width < thumbWidth * 2 / 3) {
//                    current_width = 0;
//                }

                double resultTime = (current_width - padding) / (width - 2 * thumbWidth);
                normalizedMinValueTime = Math.min(1d, Math.max(0d, resultTime));
                double result = (current_width - padding) / (width - 2 * padding);
                Log.e(TAG, "screenToNormalized result : " + result);
                return Math.min(1d, Math.max(0d, result));// 保证该该值为0-1之间，但是什么时候这个判断有用呢？
            } else {
                if (isInThumbRange(screenCoord, normalizedMaxValue, 0.5)) {
                    return normalizedMaxValue;
                }

                double right_length = getValueLength() - (rangeL + min_width);
                if (current_width > rangeR) {
                    current_width = rangeR + (current_width - rangeR);
                } else if (current_width <= rangeR) {
                    current_width = rangeR - (rangeR - current_width);
                }

                double paddingRight = getWidth() - current_width;

                if (paddingRight > right_length) {
                    isMin = true;
                    current_width = getWidth() - right_length;
                    paddingRight = right_length;
                }

                if (paddingRight < thumbWidth * 2 / 3) {
                    current_width = getWidth();
                    paddingRight = 0;
                }

                double resultTime = (paddingRight - padding) / (width - 2 * thumbWidth);
                resultTime = 1 - resultTime;
                normalizedMaxValueTime = Math.min(1d, Math.max(0d, resultTime));
                double result = (current_width - padding) / (width - 2 * padding);
                Log.e(TAG, "screenToNormalized result : " + result);
                return Math.min(1d, Math.max(0d, result));// 保证该该值为0-1之间，但是什么时候这个判断有用呢？
            }
        }
    }

    private int getValueLength() {
        return (getWidth() - 2 * thumbWidth);
    }

    /**
     * 计算位于哪个Thumb内
     *
     * @param touchX touchX
     * @return 被touch的是空还是最大值或最小值
     */
    private Thumb evalPressedThumb(float touchX) {
        Thumb result = null;
        boolean minThumbPressed = isInThumbRange(touchX, normalizedMinValue, 2);// 触摸点是否在最小值图片范围内
        boolean maxThumbPressed = isInThumbRange(touchX, normalizedMaxValue, 2);
        if (minThumbPressed && maxThumbPressed) {
            // 如果两个thumbs重叠在一起，无法判断拖动哪个，做以下处理
            // 触摸点在屏幕右侧，则判断为touch到了最小值thumb，反之判断为touch到了最大值thumb
            result = (touchX / getWidth() > 0.5f) ? Thumb.MIN : Thumb.MAX;
        } else if (minThumbPressed) {
            result = Thumb.MIN;
        } else if (maxThumbPressed) {
            result = Thumb.MAX;
        }
        return result;
    }

    private boolean isInThumbRange(float touchX, double normalizedThumbValue, double scale) {
        // 当前触摸点X坐标-最小值图片中心点在屏幕的X坐标之差<=最小点图片的宽度的一般
        // 即判断触摸点是否在以最小值图片中心为原点，宽度一半为半径的圆内。
        return Math.abs(touchX - normalizedToScreen(normalizedThumbValue)) <= thumbHalfWidth * scale;
    }

    private boolean isInThumbRangeLeft(float touchX, double normalizedThumbValue, double scale) {
        // 当前触摸点X坐标-最小值图片中心点在屏幕的X坐标之差<=最小点图片的宽度的一般
        // 即判断触摸点是否在以最小值图片中心为原点，宽度一半为半径的圆内。
        return Math.abs(touchX - normalizedToScreen(normalizedThumbValue) - thumbWidth) <= thumbHalfWidth * scale;
    }

    /**
     * 试图告诉父view不要拦截子控件的drag
     */
    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    void onStartTrackingTouch() {
        mIsDragging = true;
    }

    void onStopTrackingTouch() {
        mIsDragging = false;
    }

    public void setMinShootTime(long min_cut_time) {
        this.mMinShootTime = min_cut_time;
    }

    private float normalizedToScreen(double normalizedCoord) {
        Log.e("TAG", "normalizedCoord:" + normalizedCoord);
        return (float) (getPaddingLeft() + normalizedCoord * (getWidth() - getPaddingLeft() - getPaddingRight()));
    }

    private double valueToNormalized(long value) {
        if (0 == absoluteMaxValuePrim - absoluteMinValuePrim) {
            return 0d;
        }
        return (value - absoluteMinValuePrim) / (absoluteMaxValuePrim - absoluteMinValuePrim);
    }

    public void setStartEndTime(long start, long end) {
        this.mStartPosition = start / 100 * 1.0 / 10;
        this.mEndPosition = end / 100 * 1.0 / 10;
    }

    /**
     * String转换成double 保留N位小数。
     *
     * @param a
     * @return
     */
    public double stringToDouble(String a) {
        double b = Double.valueOf(a) / 100 / 10 * 1.0;
        DecimalFormat df = new DecimalFormat("#.0");//此为保留1位小数，若想保留2位小数，则填写#.00  ，以此类推
        String temp = df.format(b);
        b = Double.valueOf(temp);
        return b;
    }


    public void setNormalizedMinValue(double value) {
        Log.e("TAG", "setNormalizedMinValue value" + value);
        normalizedMinValue = Math.max(0d, Math.min(1d, Math.min(value, normalizedMaxValue)));
        Log.e("TAG", "setNormalizedMinValue setNormalizedMinValue" + normalizedMinValue);
        invalidate();// 重新绘制此view
    }

    public void setNormalizedMaxValue(double value) {
        normalizedMaxValue = Math.max(0d, Math.min(1d, Math.max(value, normalizedMinValue)));
        invalidate();// 重新绘制此view
    }

    public long getSelectedMinValue() {
        return normalizedToValue(normalizedMinValueTime);
    }

    public void setSelectedMinValue(long value) {
        if (0 == (absoluteMaxValuePrim - absoluteMinValuePrim)) {
            setNormalizedMinValue(0d);
        } else {
            setNormalizedMinValue(valueToNormalized(value));
        }
    }

    public long getSelectedMaxValue() {
        return normalizedToValue(normalizedMaxValueTime);
    }

    public void setSelectedMaxValue(long value) {
        if (0 == (absoluteMaxValuePrim - absoluteMinValuePrim)) {
            setNormalizedMaxValue(1d);
        } else {
            setNormalizedMaxValue(valueToNormalized(value));
        }
    }

    private long normalizedToValue(double normalized) {
        return (long) (absoluteMinValuePrim + normalized * (absoluteMaxValuePrim - absoluteMinValuePrim));
    }

    /**
     * 供外部activity调用，控制是都在拖动的时候打印log信息，默认是false不打印
     */
    public boolean isNotifyWhileDragging() {
        return notifyWhileDragging;
    }

    public void setNotifyWhileDragging(boolean flag) {
        this.notifyWhileDragging = flag;
    }

    public void setTouchDown(boolean touchDown) {
        isTouchDown = touchDown;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable("SUPER", super.onSaveInstanceState());
        bundle.putDouble("MIN", normalizedMinValue);
        bundle.putDouble("MAX", normalizedMaxValue);
        bundle.putDouble("MIN_TIME", normalizedMinValueTime);
        bundle.putDouble("MAX_TIME", normalizedMaxValueTime);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcel) {
        final Bundle bundle = (Bundle) parcel;
        super.onRestoreInstanceState(bundle.getParcelable("SUPER"));
        normalizedMinValue = bundle.getDouble("MIN");
        normalizedMaxValue = bundle.getDouble("MAX");
        normalizedMinValueTime = bundle.getDouble("MIN_TIME");
        normalizedMaxValueTime = bundle.getDouble("MAX_TIME");
    }

    public void setOnRangeSeekBarChangeListener(OnRangeSeekBarChangeListener listener) {
        this.mRangeSeekBarChangeListener = listener;
    }

    public enum Thumb {
        MIN, MAX
    }

    public interface OnRangeSeekBarChangeListener {
        void onRangeSeekBarValuesChanged(RangeSeekBarView bar, long minValue, long maxValue, int action, boolean isMin, Thumb pressedThumb);
    }
}
