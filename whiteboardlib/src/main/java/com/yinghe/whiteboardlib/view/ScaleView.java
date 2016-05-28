package com.yinghe.whiteboardlib.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.yinghe.whiteboardlib.R;
import com.yinghe.whiteboardlib.Utils.Utils;

/**
 *
 * @author zhy
 * 博客地址：http://blog.csdn.net/lmj623565791
 */
public class ScaleView extends ImageView implements
        OnTouchListener, ViewTreeObserver.OnGlobalLayoutListener

{
    private static final String TAG = ScaleView.class.getSimpleName();
    public static final float SCALE_MAX = 8.0f;
    private static final float SCALE_MID = 0.2f;
    private static final int MODE_DRAG = 1;
    private static final int MODE_SCALE = 2;
    private static final int MODE_ROTATE = 3;

    Context context;
    int actionMode;
    /**
     * 初始化时的缩放比例，如果图片宽或高大于屏幕，此值将小于0
     */
    private float initScale = 1.0f;
    private boolean once = true;
    private boolean first = true;
    /**
     * 用于存放矩阵的9个值
     */
    private static float[] matrixValues = new float[9];


    Bitmap mirrorMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.mark_mirror);
    Bitmap deleteMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.mark_delete);
    Bitmap rotateMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.mark_rotate);
    RectF photoRectSrc = null;
    RectF markerMirrorRect = new RectF(0, 0, mirrorMarkBM.getWidth(), mirrorMarkBM.getHeight());//旋转标记边界
    RectF markerDeleteRect = new RectF(0, 0, deleteMarkBM.getWidth(), deleteMarkBM.getHeight());//旋转标记边界
    RectF markerRotateRect = new RectF(0, 0, rotateMarkBM.getWidth(), rotateMarkBM.getHeight());//旋转标记边界
    RectF photoRect = new RectF();
    Paint p = new Paint();

    PointF startP = new PointF();
    PointF preP = new PointF();
    PointF curP = new PointF();

    PointF preVector = new PointF();
    PointF curVector = new PointF();
    /**
     * 缩放手势
     */
    private ScaleGestureDetector mScaleGestureDetector = null;
    /**
     * 拖动手势
     */
    private GestureDetector mGestureDetector;

    private final Matrix mScaleMatrix = new Matrix();
    private final Matrix markerScaleMatrix = new Matrix();
    private boolean isAutoScale;

    private boolean isCheckTopAndBottom = true;
    private boolean isCheckLeftAndRight = true;

    public ScaleView(Context context)
    {
        this(context, null);
    }

    public ScaleView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.context = context;
        super.setScaleType(ScaleType.MATRIX);
        mScaleGestureDetector = new ScaleGestureDetector(context, new OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (getDrawable() == null)
                    return true;
                onScaleAction(detector);
                return true;
            }


            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {

            }
        });
        this.setOnTouchListener(this);
        p.setColor(Color.BLUE);
        p.setStrokeWidth(Utils.dip2px(context, 0.8f));
        p.setStyle(Paint.Style.STROKE);
    }

    private void onDragAction(float distanceX, float distanceY) {
        int x = (int) distanceX;
        int y = (int) distanceY;
        mScaleMatrix.postTranslate(x, y);
        setImageMatrix(mScaleMatrix);
    }

    private void onScaleAction(ScaleGestureDetector detector) {
        float x = detector.getFocusX();
        float y = detector.getFocusY();
        float scale = getScale();
        float scaleFactor = detector.getScaleFactor();
        /**
         * 缩放的范围控制
         */
        if ((scale < SCALE_MAX && scaleFactor > SCALE_MID)
                || (scale > initScale && scaleFactor < 1.0f)) {
            /**
             * 最大值最小值判断
             */
//            if (scaleFactor * scale < initScale) {
//                scaleFactor = initScale / scale;
//            }
            if (scaleFactor * scale < SCALE_MID) {
                scaleFactor = SCALE_MID / scale;
            }
            if (scaleFactor * scale > SCALE_MAX) {
                scaleFactor = SCALE_MAX / scale;
            }
            /**
             * 设置缩放比例
             */
            mScaleMatrix.postScale(scaleFactor, scaleFactor, (int)x, (int)y);
            setImageMatrix(mScaleMatrix);
        }
    }


    /**
     * 获取p1到p2的线段的长度
     *
     * @return
     */
    double getLineLength(PointF vector) {
        return Math.sqrt(vector.x * vector.x + vector.y * vector.y);
    }

    private void onRotateAction() {

        float a = (float) Math.sqrt((curP.x - photoRect.centerX()) * (curP.x - photoRect.centerX())
                + (curP.y - photoRect.centerY()) * (curP.y - photoRect.centerY()));
        float b = (float) Math.sqrt(photoRect.width() / 2*photoRect.width() / 2+photoRect.height() / 2*photoRect.height() / 2);
        float scale = a / b;
        if (scale != 0)
        mScaleMatrix.postScale(scale, scale, photoRect.centerX(), photoRect.centerY());

        //根据触点 构建两个向量，计算两个向量角度.
        preVector.set(preP.x - photoRect.centerX(), preP.y - photoRect.centerY());//旧向量
        curVector.set(curP.x - photoRect.centerX(), curP.y - photoRect.centerY());//新向量

        double preVectorLen = getLineLength(preVector);
        double curVectorLen = getLineLength(curVector);
        //计算两个向量的夹角.
        double cosAlpha = (preVector.x * curVector.x + preVector.y * curVector.y)
                / (preVectorLen * curVectorLen);

        //由于计算误差，可能会带来略大于1的cos，例如
        if (cosAlpha > 1.0f) {
            cosAlpha = 1.0f;
        }
        //本次的角度已经计算出来。
        double dAngle = Math.acos(cosAlpha) * 180.0 / 3.14;

        System.out.println("" + dAngle);
        // 判断顺时针和逆时针.
        //判断方法其实很简单，这里的v1v2其实相差角度很小的。
        //v1v2先Normalize，
        preVector.x /= preVectorLen;
        preVector.y /= preVectorLen;
        curVector.x /= curVectorLen;
        curVector.y /= curVectorLen;
        //作v2的逆时针垂直向量。
        PointF v2Vec = new PointF(curVector.y, -curVector.x);

        //判断这个垂直向量和v1的点积，点积>0表示俩向量夹角锐角。=0表示垂直，<0表示钝角
        float vDot = preVector.x * v2Vec.x + preVector.y * v2Vec.y;
        if (vDot > 0) {
            //v2的逆时针垂直向量和v1是锐角关系，说明v1在v2的逆时针方向。
        } else {
            dAngle = -dAngle;
        }
//
//        angle += dAngle;
//
//        //角度你懂的。
//        if (angle >= 360) {
//
//            angle -= 360;
//        }
//        if (angle < 0) {
//            angle +=360;
//        }
        mScaleMatrix.postRotate((float) dAngle, photoRect.centerX(), photoRect.centerY());
        setImageMatrix(mScaleMatrix);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        canvas.rotate(2);
        drawPhotoRect(canvas);
        drawMarkers(canvas);
    }

    private void drawPhotoRect(Canvas canvas) {
        if (first) {//首次绘制调整边界
            photoRectSrc = new RectF(getDrawable().getBounds());//图片的边界
            markerScaleMatrix.postTranslate((getWidth() + photoRect.width() - markerRotateRect.width()) / 2, (getHeight() + photoRect.height() - markerRotateRect.height()) / 2);//将标记Matrix与图片同步
            first = false;
        }
        mScaleMatrix.mapRect(photoRect,photoRectSrc);
        canvas.drawRect(photoRect, p);
    }

    private void drawMarkers(Canvas canvas) {
        float x;
        float y;

        x=photoRect.left- markerMirrorRect.width()/2;
        y=photoRect.top- markerMirrorRect.height()/2;
        markerMirrorRect.offsetTo(x,y);
//        canvas.drawRect(markerMirrorRect, p);
        canvas.drawBitmap(mirrorMarkBM,x,y,null);

        x=photoRect.right- markerDeleteRect.width()/2;
        y=photoRect.top- markerDeleteRect.height()/2;
        markerDeleteRect.offsetTo(x,y);
//        canvas.drawRect(markerDeleteRect, p);
        canvas.drawBitmap(deleteMarkBM,x,y,null);

        x=photoRect.right- markerRotateRect.width()/2;
        y=photoRect.bottom- markerRotateRect.height()/2;
        markerRotateRect.offsetTo(x,y);
//        canvas.drawRect(markerRotateRect, p);
        canvas.drawBitmap(rotateMarkBM,x,y,null);

    }


    /**
     * 在缩放时，进行图片显示范围的控制
     */
    private void checkBorderAndCenterWhenScale()
    {

        RectF rect = getMatrixRectF();
        float deltaX = 0;
        float deltaY = 0;

        int width = getWidth();
        int height = getHeight();

        // 如果宽或高大于屏幕，则控制范围
        if (rect.width() >= width)
        {
            if (rect.left > 0)
            {
                deltaX = -rect.left;
            }
            if (rect.right < width)
            {
                deltaX = width - rect.right;
            }
        }
        if (rect.height() >= height)
        {
            if (rect.top > 0)
            {
                deltaY = -rect.top;
            }
            if (rect.bottom < height)
            {
                deltaY = height - rect.bottom;
            }
        }
        // 如果宽或高小于屏幕，则让其居中
        if (rect.width() < width)
        {
            deltaX = width * 0.5f - rect.right + 0.5f * rect.width();
        }
        if (rect.height() < height)
        {
            deltaY = height * 0.5f - rect.bottom + 0.5f * rect.height();
        }
        Log.e(TAG, "deltaX = " + deltaX + " , deltaY = " + deltaY);

        mScaleMatrix.postTranslate(deltaX, deltaY);

    }

    /**
     * 根据当前图片的Matrix获得图片的范围
     *
     * @return
     */
    private RectF getMatrixRectF()
    {
        Matrix matrix = mScaleMatrix;
        RectF rect = new RectF();
        Drawable d = getDrawable();
        if (null != d)
        {
            rect.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            matrix.mapRect(rect);
        }
        return rect;
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        curP.set(event.getX(), event.getY());
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                startP.set(event.getX(), event.getY());
                if (markerRotateRect.contains((int) startP.x, (int) startP.y)) {
                    actionMode = MODE_ROTATE;
                } else {
                    actionMode = MODE_DRAG;
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                actionMode = MODE_SCALE;
                break;

            case MotionEvent.ACTION_MOVE:
                if (actionMode == MODE_SCALE) {
                    mScaleGestureDetector.onTouchEvent(event);//双指缩放
                } else if (actionMode == MODE_DRAG) {
                    onDragAction(curP.x - preP.x, curP.y - preP.y);
                } else if (actionMode == MODE_ROTATE) {
                    onRotateAction();
                }
                preP.set(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                break;
            default:
                break;
        }
        preP.set(event.getX(), event.getY());
        return true;
    }



    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }

    @Override
    public void onGlobalLayout()
    {
        if (once)
        {
            Drawable d = getDrawable();
            if (d == null)
                return;
            Log.e(TAG, d.getIntrinsicWidth() + " , " + d.getIntrinsicHeight());
            int width = getWidth();
            int height = getHeight();
            // 拿到图片的宽和高
            int dw = d.getIntrinsicWidth();
            int dh = d.getIntrinsicHeight();
            float scale = 1.0f;
            // 如果图片的宽或者高大于屏幕，则缩放至屏幕的宽或者高
            if (dw > width && dh <= height)
            {
                scale = width * 1.0f / dw;
            }
            if (dh > height && dw <= width)
            {
                scale = height * 1.0f / dh;
            }
            // 如果宽和高都大于屏幕，则让其按按比例适应屏幕大小
            if (dw > width && dh > height)
            {
                scale = Math.min(width * 1.0f / dw, height * 1.0f / dh);
            }
            initScale = scale;

            Log.e(TAG, "initScale = " + initScale);
            mScaleMatrix.postTranslate((width - dw) / 2, (height - dh) / 2);
            mScaleMatrix.postScale(scale, scale, getWidth() / 2,
                    getHeight() / 2);
            // 图片移动至屏幕中心
            setImageMatrix(mScaleMatrix);
            once = false;
        }

    }

    /**
     * 移动时，进行边界判断，主要判断宽或高大于屏幕的
     */
    private void checkMatrixBounds()
    {
        RectF rect = getMatrixRectF();

        float deltaX = 0, deltaY = 0;
        final float viewWidth = getWidth();
        final float viewHeight = getHeight();
        // 判断移动或缩放后，图片显示是否超出屏幕边界
        if (rect.top > 0 && isCheckTopAndBottom)
        {
            deltaY = -rect.top;
        }
        if (rect.bottom < viewHeight && isCheckTopAndBottom)
        {
            deltaY = viewHeight - rect.bottom;
        }
        if (rect.left > 0 && isCheckLeftAndRight)
        {
            deltaX = -rect.left;
        }
        if (rect.right < viewWidth && isCheckLeftAndRight)
        {
            deltaX = viewWidth - rect.right;
        }
        mScaleMatrix.postTranslate(deltaX, deltaY);
    }

    public float getScale() {
        mScaleMatrix.getValues(matrixValues);
        return matrixValues[Matrix.MSCALE_X];
    }

    public float getTranslateX() {
        mScaleMatrix.getValues(matrixValues);
        return matrixValues[Matrix.MTRANS_X];
    }

    public float getTranslateY() {
        mScaleMatrix.getValues(matrixValues);
        return matrixValues[Matrix.MTRANS_Y];
    }

    public float getRotate()
    {
        mScaleMatrix.getValues(matrixValues);
        return matrixValues[Matrix.MTRANS_Y];
    }

}
