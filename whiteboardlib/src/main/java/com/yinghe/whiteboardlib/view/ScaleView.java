package com.yinghe.whiteboardlib.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

import com.yinghe.whiteboardlib.R;
import com.yinghe.whiteboardlib.Utils.BitmapUtils;

import java.io.File;

/**
 * @author tangentLu
 *         博客地址：http://www.jianshu.com/users/9efe1db2c646/latest_articles
 */
public class ScaleView extends ImageView implements
        OnTouchListener

{
    private static final String TAG = ScaleView.class.getSimpleName();
    public static float SCALE_MAX = 4.0f;
    private static float SCALE_MIN = 0.2f;
    private static final int MODE_NONE = 0;
    private static final int MODE_DRAG = 1;
    private static final int MODE_SCALE = 2;
    private static final int MODE_ROTATE = 3;

    float simpleScale = 0.5f;//图片载入的缩放倍数
    Context context;
    int actionMode;
    /**
     * 初始化时的缩放比例，如果图片宽或高大于屏幕，此值将小于0
     */
    private float initScale = 1.0f;
    private boolean first = true;
    /**
     * 用于存放矩阵的9个值
     */
    private static float[] matrixValues = new float[9];


    RectF photoRectSrc = null;
    Bitmap photoSampleBM = null;
    Bitmap mirrorMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.mark_copy);
    Bitmap deleteMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.mark_delete);
    Bitmap rotateMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.mark_rotate);
    //    Bitmap rotateMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.test);
    RectF markerMirrorRect = new RectF(0, 0, mirrorMarkBM.getWidth(), mirrorMarkBM.getHeight());//镜像标记边界
    RectF markerDeleteRect = new RectF(0, 0, deleteMarkBM.getWidth(), deleteMarkBM.getHeight());//删除标记边界
    RectF markerRotateRect = new RectF(0, 0, rotateMarkBM.getWidth(), rotateMarkBM.getHeight());//旋转标记边界


    double photoLen;//图片对角线长度
    Paint p = new Paint();

    PointF startP = new PointF();
    PointF preP = new PointF();
    PointF curP = new PointF();

    PointF preVector = new PointF();
    PointF curVector = new PointF();

    float[] photoCornersSrc = new float[10];//0,1代表左上角点XY，2,3代表右上角点XY，4,5代表右下角点XY，6,7代表左下角点XY，8,9代表中心点XY
    float[] photoCorners = new float[10];//0,1代表左上角点XY，2,3代表右上角点XY，4,5代表右下角点XY，6,7代表左下角点XY，8,9代表中心点XY

    Path photoBorderPath = new Path();
    Rect viewRect = new Rect();


    /**
     * 缩放手势
     */
    private ScaleGestureDetector mScaleGestureDetector = null;
    /**
     * 拖动手势
     */
    private GestureDetector mGestureDetector;

    private final Matrix mPhotoMatrix = new Matrix();//图片的变换矩阵
    private final Matrix mInvertPhotoMatrix = new Matrix();//图片的变换逆矩阵
    //    private final Matrix markerMatrix = new Matrix();
    private boolean isAutoScale;

    private boolean isCheckTopAndBottom = true;
    private boolean isCheckLeftAndRight = true;

    public ScaleView(Context context) {
        this(context, null);
    }

    public ScaleView(Context context, AttributeSet attrs) {
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
        p.setColor(Color.GRAY);
        p.setStrokeWidth(BitmapUtils.dip2px(context, 0.8f));
        p.setStyle(Paint.Style.STROKE);
    }

    private void onDragAction(float distanceX, float distanceY) {
        mPhotoMatrix.postTranslate((int) distanceX, (int) distanceY);
        setImageMatrix(mPhotoMatrix);

    }

    private void onScaleAction(ScaleGestureDetector detector) {
        //目前图片对角线长度
        float len = (float) Math.sqrt(Math.pow(photoCorners[0] - photoCorners[4], 2) + Math.pow(photoCorners[1] - photoCorners[5], 2));
        float scaleFactor = detector.getScaleFactor();
        //设置Matrix缩放参数
        if ((scaleFactor < 1 && len >= photoLen * SCALE_MIN) || (scaleFactor > 1 && len <= photoLen * SCALE_MAX)) {
            Log.e(scaleFactor + "", scaleFactor + "");
            mPhotoMatrix.postScale(scaleFactor, scaleFactor, photoCorners[8], photoCorners[9]);
            setImageMatrix(mPhotoMatrix);
        }
    }


    /**
     * 获取p1到p2的线段的长度
     *
     * @return
     */
    double getVectorLength(PointF vector) {
        return Math.sqrt(vector.x * vector.x + vector.y * vector.y);
    }

    private void onRotateAction() {
        //放大
        //目前触摸点与图片显示中心距离
        float a = (float) Math.sqrt(Math.pow(curP.x - photoCorners[8], 2) + Math.pow(curP.y - photoCorners[9], 2));
        //目前上次旋转图标与图片显示中心距离
        float b = (float) Math.sqrt(Math.pow(photoCorners[4] - photoCorners[0], 2) + Math.pow(photoCorners[5] - photoCorners[1], 2)) / 2;

        //设置Matrix缩放参数
        if (a >= photoLen / 2 * SCALE_MIN && a <= photoLen / 2 * SCALE_MAX) {
            //这种计算方法可以保持旋转图标坐标与触摸点同步缩放
            float scale = a / b;
            mPhotoMatrix.postScale(scale, scale, photoCorners[8], photoCorners[9]);
        }

        //旋转
        //根据移动坐标的变化构建两个向量，以便计算两个向量角度.
        preVector.set(preP.x - photoCorners[8], preP.y - photoCorners[9]);//旋转后向量
        curVector.set(curP.x - photoCorners[8], curP.y - photoCorners[9]);//旋转前向量
        //计算向量长度
        double preVectorLen = getVectorLength(preVector);
        double curVectorLen = getVectorLength(curVector);
        //计算两个向量的夹角.
        double cosAlpha = (preVector.x * curVector.x + preVector.y * curVector.y)
                / (preVectorLen * curVectorLen);
        //由于计算误差，可能会带来略大于1的cos，例如
        if (cosAlpha > 1.0f) {
            cosAlpha = 1.0f;
        }
        //本次的角度已经计算出来。
        double dAngle = Math.acos(cosAlpha) * 180.0 / Math.PI;
        // 判断顺时针和逆时针.
        //判断方法其实很简单，这里的v1v2其实相差角度很小的。
        //先转换成单位向量
        preVector.x /= preVectorLen;
        preVector.y /= preVectorLen;
        curVector.x /= curVectorLen;
        curVector.y /= curVectorLen;
        //作curVector的逆时针垂直向量。
        PointF verticalVec = new PointF(curVector.y, -curVector.x);

        //判断这个垂直向量和v1的点积，点积>0表示俩向量夹角锐角。=0表示垂直，<0表示钝角
        float vDot = preVector.x * verticalVec.x + preVector.y * verticalVec.y;
        if (vDot > 0) {
            //v2的逆时针垂直向量和v1是锐角关系，说明v1在v2的逆时针方向。
        } else {
            dAngle = -dAngle;
        }
        mPhotoMatrix.postRotate((float) dAngle, photoCorners[8], photoCorners[9]);
        setImageMatrix(mPhotoMatrix);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getDrawable() != null) {
            drawPhotoBorder(canvas);
            super.onDraw(canvas);
            drawMarkers(canvas);
        } else {
            super.onDraw(canvas);
        }
    }

    private void setLimitScale() {
        SCALE_MAX = Math.max(getWidth(), getHeight()) / Math.max(photoRectSrc.width(), photoRectSrc.height());
//        SCALE_MIN = SCALE_MAX / 5;
    }

    public void setPhotoPath(String path) {
        if (path.contains(Environment.getExternalStorageDirectory().toString())) {
            photoSampleBM = setSDCardPhoto(path);
        } else {
            photoSampleBM = setAssetsPhoto(path);
        }
        if (photoSampleBM != null) {
            setImageBitmap(photoSampleBM);
            first = true;
            invalidate();
        }
    }

    public Bitmap setSDCardPhoto(String path) {
        File file = new File(path);
        if (file.exists()) {
            return BitmapUtils.decodeSampleBitMapFromFile(context, path, simpleScale);
        } else {
            return null;
        }
    }

    public Bitmap setAssetsPhoto(String path) {
        return BitmapUtils.getBitmapFromAssets(context, path);
    }

    private void drawPhotoBorder(Canvas canvas) {
        if (first) {//首次绘制调整边界
            mPhotoMatrix.reset();
//            getGlobalVisibleRect(viewRect);
            photoRectSrc = new RectF(getDrawable().getBounds());//图片的边界
            setLimitScale();//放大倍数
            photoCornersSrc[0] = photoRectSrc.left;
            photoCornersSrc[1] = photoRectSrc.top;
            photoCornersSrc[2] = photoRectSrc.right;
            photoCornersSrc[3] = photoRectSrc.top;
            photoCornersSrc[4] = photoRectSrc.right;
            photoCornersSrc[5] = photoRectSrc.bottom;
            photoCornersSrc[6] = photoRectSrc.left;
            photoCornersSrc[7] = photoRectSrc.bottom;
            photoCornersSrc[8] = photoRectSrc.centerX();
            photoCornersSrc[9] = photoRectSrc.centerY();
            photoLen = Math.sqrt(Math.pow(photoRectSrc.width(), 2) + Math.pow(photoRectSrc.height(), 2));
//            markerMatrix.postTranslate((getWidth() + photoRectSrc.width() - markerRotateRect.width()) / 2,
//                    (getHeight() + photoRectSrc.height() - markerRotateRect.height()) / 2);//将标记Matrix与图片同步
            first = false;
            mPhotoMatrix.postTranslate(getWidth() / 2 - photoRectSrc.width() / 2, getHeight() / 2 - photoRectSrc.height() / 2);
            setImageMatrix(mPhotoMatrix);
        }
        mPhotoMatrix.mapPoints(photoCorners, photoCornersSrc);
        photoBorderPath.reset();
        photoBorderPath.moveTo(photoCorners[0], photoCorners[1]);
        photoBorderPath.lineTo(photoCorners[2], photoCorners[3]);
        photoBorderPath.lineTo(photoCorners[4], photoCorners[5]);
        photoBorderPath.lineTo(photoCorners[6], photoCorners[7]);
        photoBorderPath.lineTo(photoCorners[0], photoCorners[1]);
        canvas.drawPath(photoBorderPath, p);
    }

    private void drawMarkers(Canvas canvas) {
        float x;
        float y;


        x = photoCorners[0] - markerMirrorRect.width() / 2;
        y = photoCorners[1] - markerMirrorRect.height() / 2;
        markerMirrorRect.offsetTo(x, y);
//        canvas.drawRect(markerCopyRect, p);
//        canvas.drawBitmap(mirrorMarkBM,x,y,null);

        x = photoCorners[2] - markerDeleteRect.width() / 2;
        y = photoCorners[3] - markerDeleteRect.height() / 2;
        markerDeleteRect.offsetTo(x, y);
//        canvas.drawRect(markerDeleteRect, p);
//        canvas.drawBitmap(deleteMarkBM,x,y,null);

        x = photoCorners[4] - markerRotateRect.width() / 2;
        y = photoCorners[5] - markerRotateRect.height() / 2;
        markerRotateRect.offsetTo(x, y);
//        canvas.drawRect(markerRotateRect, p);
        canvas.drawBitmap(rotateMarkBM, x, y, null);
    }


    /**
     * 在缩放时，进行图片显示范围的控制
     */
    private void checkBorderAndCenterWhenScale() {

        RectF rect = getMatrixRectF();
        float deltaX = 0;
        float deltaY = 0;

        int width = getWidth();
        int height = getHeight();

        // 如果宽或高大于屏幕，则控制范围
        if (rect.width() >= width) {
            if (rect.left > 0) {
                deltaX = -rect.left;
            }
            if (rect.right < width) {
                deltaX = width - rect.right;
            }
        }
        if (rect.height() >= height) {
            if (rect.top > 0) {
                deltaY = -rect.top;
            }
            if (rect.bottom < height) {
                deltaY = height - rect.bottom;
            }
        }
        // 如果宽或高小于屏幕，则让其居中
        if (rect.width() < width) {
            deltaX = width * 0.5f - rect.right + 0.5f * rect.width();
        }
        if (rect.height() < height) {
            deltaY = height * 0.5f - rect.bottom + 0.5f * rect.height();
        }
        Log.e(TAG, "deltaX = " + deltaX + " , deltaY = " + deltaY);

        mPhotoMatrix.postTranslate(deltaX, deltaY);

    }

    /**
     * 根据当前图片的Matrix获得图片的范围
     *
     * @return
     */
    private RectF getMatrixRectF() {
        Matrix matrix = mPhotoMatrix;
        RectF rect = new RectF();
        Drawable d = getDrawable();
        if (null != d) {
            rect.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            matrix.mapRect(rect);
        }
        return rect;
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        curP.set(x, y);
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                startP.set(x, y);
                actionMode = MODE_NONE;//重置操作模式
                mPhotoMatrix.invert(mInvertPhotoMatrix);//计算最新的逆矩阵
                float[] invertPoint = new float[2];
                mInvertPhotoMatrix.mapPoints(invertPoint, new float[]{x, y});//对点击点进行逆矩阵变换
                if (markerRotateRect.contains((int) startP.x, (int) startP.y)) {//判断是否在区域内
                    actionMode = MODE_ROTATE;
                    break;
                }
                if (photoRectSrc.contains(invertPoint[0], invertPoint[1])) {
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
                preP.set(x, y);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                break;
            default:
                break;
        }
        preP.set(x, y);
        return true;
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
//        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
//        getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }

//    @Override
//    public void onGlobalLayout()
//    {
//        if (once)
//        {
//            Drawable d = getDrawable();
//            if (d == null)
//                return;
//            Log.e(TAG, d.getIntrinsicWidth() + " , " + d.getIntrinsicHeight());
//            int width = getWidth();
//            int height = getHeight();
//            // 拿到图片的宽和高
//            int dw = d.getIntrinsicWidth();
//            int dh = d.getIntrinsicHeight();
//            float scale = 1.0f;
//            // 如果图片的宽或者高大于屏幕，则缩放至屏幕的宽或者高
//            if (dw > width && dh <= height)
//            {
//                scale = width * 1.0f / dw;
//            }
//            if (dh > height && dw <= width)
//            {
//                scale = height * 1.0f / dh;
//            }
//            // 如果宽和高都大于屏幕，则让其按按比例适应屏幕大小
//            if (dw > width && dh > height)
//            {
//                scale = Math.min(width * 1.0f / dw, height * 1.0f / dh);
//            }
//            initScale = scale;
//
//            Log.e(TAG, "initScale = " + initScale);
//            mPhotoMatrix.postTranslate((width - dw) / 2, (height - dh) / 2);
//            mPhotoMatrix.postScale(scale, scale, getWidth() / 2,
//                    getHeight() / 2);
//            // 图片移动至屏幕中心
//            setImageMatrix(mPhotoMatrix);
//            once = false;
//        }
//
//    }

    /**
     * 判断是否超出边界
     */
    private boolean isOutOfBounds(int x, int y) {
        return !viewRect.contains(x, y);
//        RectF rect = getMatrixRectF();
//
//        float deltaX = 0, deltaY = 0;
//        final float viewWidth = getWidth();
//        final float viewHeight = getHeight();
//        // 判断移动或缩放后，图片显示是否超出屏幕边界
//        if (rect.top > 0 && isCheckTopAndBottom)
//        {
//            deltaY = -rect.top;
//        }
//        if (rect.bottom < viewHeight && isCheckTopAndBottom)
//        {
//            deltaY = viewHeight - rect.bottom;
//        }
//        if (rect.left > 0 && isCheckLeftAndRight)
//        {
//            deltaX = -rect.left;
//        }
//        if (rect.right < viewWidth && isCheckLeftAndRight)
//        {
//            deltaX = viewWidth - rect.right;
//        }
//        mPhotoMatrix.postTranslate(deltaX, deltaY);
    }

    public float getScale() {
        mPhotoMatrix.getValues(matrixValues);
        return matrixValues[Matrix.MSCALE_X];
    }

    public float getTranslateX() {
        mPhotoMatrix.getValues(matrixValues);
        return matrixValues[Matrix.MTRANS_X];
    }

    public float getTranslateY() {
        mPhotoMatrix.getValues(matrixValues);
        return matrixValues[Matrix.MTRANS_Y];
    }

    public float getRotate() {
        mPhotoMatrix.getValues(matrixValues);
        return matrixValues[Matrix.MTRANS_Y];
    }

    public Bitmap getPhotoSampleBM() {
        return photoSampleBM;
    }

    public Matrix getPhotoMatrix() {
        return mPhotoMatrix;
    }
}
