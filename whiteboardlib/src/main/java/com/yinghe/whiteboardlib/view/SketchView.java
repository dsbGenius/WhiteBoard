/*
 * Copyright (C) 2015 Federico Iosue (federico.iosue@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.yinghe.whiteboardlib.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Environment;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

import com.yinghe.whiteboardlib.R;
import com.yinghe.whiteboardlib.Utils.BitmapUtils;
import com.yinghe.whiteboardlib.bean.DrawRecord;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.yinghe.whiteboardlib.bean.DrawRecord.STROKE_TYPE_BITMAP;
import static com.yinghe.whiteboardlib.bean.DrawRecord.STROKE_TYPE_CIRCLE;
import static com.yinghe.whiteboardlib.bean.DrawRecord.STROKE_TYPE_DRAW;
import static com.yinghe.whiteboardlib.bean.DrawRecord.STROKE_TYPE_ERASER;
import static com.yinghe.whiteboardlib.bean.DrawRecord.STROKE_TYPE_LINE;
import static com.yinghe.whiteboardlib.bean.DrawRecord.STROKE_TYPE_RECTANGLE;
import static com.yinghe.whiteboardlib.bean.DrawRecord.STROKE_TYPE_TEXT;


public class SketchView extends ImageView implements OnTouchListener {

    public interface TextWindowCallback {
        void onText(View view, DrawRecord record);
    }


    public void setTextWindowCallback(TextWindowCallback textWindowCallback) {
        this.textWindowCallback = textWindowCallback;
    }

    private TextWindowCallback textWindowCallback;
    private static final float TOUCH_TOLERANCE = 4;

    public static final int EDIT_STROKE = 1;
    public static final int EDIT_PHOTO = 2;
    public static final int DEFAULT_STROKE_SIZE = 7;
    public static final int DEFAULT_STROKE_ALPHA = 100;
    public static final int DEFAULT_ERASER_SIZE = 50;


    private float strokeSize = DEFAULT_STROKE_SIZE;
    private int strokeRealColor = Color.BLACK;//画笔实际颜色
    private int strokeColor = Color.BLACK;//画笔颜色
    private int strokeAlpha = 255;//画笔透明度
    private float eraserSize = DEFAULT_ERASER_SIZE;
    private int background = Color.WHITE;

    Bitmap mirrorMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.mark_mirror);
    Bitmap deleteMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.mark_delete);
    Bitmap rotateMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.mark_rotate);
    //    Bitmap rotateMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.test);
    RectF markerMirrorRect = new RectF(0, 0, mirrorMarkBM.getWidth(), mirrorMarkBM.getHeight());//镜像标记边界
    RectF markerDeleteRect = new RectF(0, 0, deleteMarkBM.getWidth(), deleteMarkBM.getHeight());//删除标记边界
    RectF markerRotateRect = new RectF(0, 0, rotateMarkBM.getWidth(), rotateMarkBM.getHeight());//旋转标记边界

    private Path m_Path;
    private Paint m_Paint;
    private float downX, downY, preX, preY;
    private int width, height;

    private List<DrawRecord> photoRecordList = new ArrayList<>();
    private List<DrawRecord> strokeRecordList = new ArrayList<>();
    private List<DrawRecord> strokeRedoList = new ArrayList<>();
    private Context mContext;

    private Bitmap bitmap;
    private Bitmap curPhotoBM;
    DrawRecord curStrokeRecord;
    DrawRecord curPhotoRecord;


    private int editMode = EDIT_STROKE;
    private static float SCALE_MAX = 4.0f;
    private static float SCALE_MIN = 0.2f;

    float simpleScale = 0.5f;//图片载入的缩放倍数

    public void setStrokeType(int strokeType) {
        this.strokeType = strokeType;
    }

    public int getStrokeType() {
        return strokeType;
    }

    private int strokeType = DrawRecord.STROKE_TYPE_DRAW;

    private OnDrawChangedListener onDrawChangedListener;


    public SketchView(Context context, AttributeSet attr) {
        super(context, attr);

        this.mContext = context;

        setFocusable(true);
        setFocusableInTouchMode(true);
        setBackgroundColor(Color.WHITE);

        this.setOnTouchListener(this);

        m_Paint = new Paint();
        m_Paint.setAntiAlias(true);
        m_Paint.setDither(true);
        m_Paint.setColor(strokeRealColor);
        m_Paint.setStyle(Paint.Style.STROKE);
        m_Paint.setStrokeJoin(Paint.Join.ROUND);
        m_Paint.setStrokeCap(Paint.Cap.ROUND);
        m_Paint.setStrokeWidth(strokeSize);
        invalidate();
    }


//    public void setStrokeMode(int strokeMode) {
//        if (strokeMode == STROKE || strokeMode == ERASER)
//            this.strokeMode = strokeMode;
//    }


    public int getStrokeAlpha() {
        return strokeAlpha;
    }

    public void setStrokeAlpha(int mAlpha) {
        this.strokeAlpha = mAlpha;
        calculColor();
        m_Paint.setStrokeWidth(strokeSize);
    }

    public int getStrokeColor() {
        return this.strokeRealColor;
    }


    public void setStrokeColor(int color) {
        strokeColor = color;
        calculColor();
        m_Paint.setColor(strokeRealColor);
    }


    private void calculColor() {
        strokeRealColor = Color.argb(strokeAlpha, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor));
    }


//    public int getStrokeMode() {
//        return this.strokeMode;
//    }


    /**
     * Change canvass background and force redraw
     */
    public void setBackgroundBitmap(Activity mActivity, Bitmap bitmap) {
        if (!bitmap.isMutable()) {
            Bitmap.Config bitmapConfig = bitmap.getConfig();
            // set default bitmap config if none
            if (bitmapConfig == null) {
                bitmapConfig = Bitmap.Config.ARGB_8888;
            }
            bitmap = bitmap.copy(bitmapConfig, true);
        }
        this.bitmap = bitmap;
//		this.bitmap = getScaledBitmap(mActivity, bitmap);
        invalidate();
//		mCanvas = new Canvas(bitmap);
    }


    private Bitmap getScaledBitmap(Activity mActivity, Bitmap bitmap) {
        DisplayMetrics display = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(display);
        int screenWidth = display.widthPixels;
        int screenHeight = display.heightPixels;
        float scale = bitmap.getWidth() / screenWidth > bitmap.getHeight() / screenHeight ? bitmap.getWidth() /
                screenWidth : bitmap.getHeight() / screenHeight;
        int scaledWidth = (int) (bitmap.getWidth() / scale);
        int scaledHeight = (int) (bitmap.getHeight() / scale);
        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }


    @Override
    public boolean onTouch(View arg0, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_down(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        return true;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0, 0, null);
        }
        drawRecord(canvas);
        if (onDrawChangedListener != null)
            onDrawChangedListener.onDrawChanged();
    }

    private void drawRecord(Canvas canvas) {
        for (DrawRecord record : photoRecordList) {
            canvas.drawBitmap(record.bitmap, record.matrix, null);
        }
        if (editMode == EDIT_PHOTO && curPhotoRecord != null) {
            SCALE_MAX = curPhotoRecord.scaleMax;
            float[] photoCorners = calculateCorners(curPhotoRecord);//计算图片四个角点和中心点
            drawBoard(canvas, photoCorners);//绘制图形边线
            drawMarks(canvas, photoCorners);//绘制边角图片
        }
        for (DrawRecord record : strokeRecordList) {
            int type = record.type;
            if (type == DrawRecord.STROKE_TYPE_ERASER || type == DrawRecord.STROKE_TYPE_DRAW || type == DrawRecord.STROKE_TYPE_LINE) {
                canvas.drawPath(record.path, record.paint);
            } else if (type == STROKE_TYPE_CIRCLE) {
                canvas.drawOval(record.rect, record.paint);
            } else if (type == STROKE_TYPE_RECTANGLE) {
                canvas.drawRect(record.rect, record.paint);
            } else if (type == STROKE_TYPE_TEXT) {
                if (record.text != null) {
                    StaticLayout layout = new StaticLayout(record.text, record.textPaint, record.textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
                    canvas.translate(record.textOffX, record.textOffY);
                    layout.draw(canvas);
                    canvas.translate(-record.textOffX, -record.textOffY);
                }
            }
        }
    }

    private void drawBoard(Canvas canvas, float[] photoCorners) {
        Path photoBorderPath = new Path();
        photoBorderPath.moveTo(photoCorners[0], photoCorners[1]);
        photoBorderPath.lineTo(photoCorners[2], photoCorners[3]);
        photoBorderPath.lineTo(photoCorners[4], photoCorners[5]);
        photoBorderPath.lineTo(photoCorners[6], photoCorners[7]);
        photoBorderPath.lineTo(photoCorners[0], photoCorners[1]);
        canvas.drawPath(photoBorderPath, curPhotoRecord.paint);
    }

    private void drawMarks(Canvas canvas, float[] photoCorners) {
        float x;
        float y;
        x = photoCorners[0] - markerMirrorRect.width() / 2;
        y = photoCorners[1] - markerMirrorRect.height() / 2;
        markerMirrorRect.offsetTo(x, y);
//        canvas.drawRect(markerMirrorRect, p);
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

    private float[] calculateCorners(DrawRecord record) {
        float[] photoCornersSrc = new float[10];//0,1代表左上角点XY，2,3代表右上角点XY，4,5代表右下角点XY，6,7代表左下角点XY，8,9代表中心点XY
        float[] photoCorners = new float[10];//0,1代表左上角点XY，2,3代表右上角点XY，4,5代表右下角点XY，6,7代表左下角点XY，8,9代表中心点XY
        RectF rectF = record.photoRectSrc;
        photoCornersSrc[0] = rectF.left;
        photoCornersSrc[1] = rectF.top;
        photoCornersSrc[2] = rectF.right;
        photoCornersSrc[3] = rectF.top;
        photoCornersSrc[4] = rectF.right;
        photoCornersSrc[5] = rectF.bottom;
        photoCornersSrc[6] = rectF.left;
        photoCornersSrc[7] = rectF.bottom;
        photoCornersSrc[8] = rectF.centerX();
        photoCornersSrc[9] = rectF.centerY();
        curPhotoRecord.matrix.mapPoints(photoCorners, photoCornersSrc);
        return photoCorners;
    }

    private float getMaxScale(RectF photoSrc) {
        return Math.max(getWidth(), getHeight()) / Math.max(photoSrc.width(), photoSrc.height());
//        SCALE_MIN = SCALE_MAX / 5;
    }
    public void addStrokeRecord(DrawRecord record) {
        strokeRecordList.add(record);
        invalidate();
    }

    public void addPhotoRecord(DrawRecord record) {
        photoRecordList.add(record);
        invalidate();
    }

    private void touch_down(float x, float y) {
        preX = downX = x;
        preY = downY = y;
        if (editMode == EDIT_STROKE) {
            strokeRedoList.clear();
            curStrokeRecord = new DrawRecord(strokeType);
            if (strokeType == STROKE_TYPE_ERASER) {
                m_Path = new Path();
                m_Path.moveTo(downX, downY);
                m_Paint.setColor(Color.WHITE);
                m_Paint.setStrokeWidth(eraserSize);
                curStrokeRecord.paint = new Paint(m_Paint); // Clones the mPaint object
                curStrokeRecord.path = m_Path;
            } else if (strokeType == STROKE_TYPE_DRAW || strokeType == STROKE_TYPE_LINE) {
                m_Path = new Path();
                m_Path.moveTo(downX, downY);
                curStrokeRecord.path = m_Path;
                m_Paint.setColor(strokeRealColor);
                m_Paint.setStrokeWidth(strokeSize);
                curStrokeRecord.paint = new Paint(m_Paint); // Clones the mPaint object
            } else if (strokeType == STROKE_TYPE_CIRCLE || strokeType == STROKE_TYPE_RECTANGLE) {
                RectF rect = new RectF(x, y, x, y);
                curStrokeRecord.rect = rect;
                m_Paint.setColor(strokeRealColor);
                m_Paint.setStrokeWidth(strokeSize);
                curStrokeRecord.paint = new Paint(m_Paint); // Clones the mPaint object
            } else if (strokeType == STROKE_TYPE_TEXT) {
                curStrokeRecord.textOffX = (int) x;
                curStrokeRecord.textOffY = (int) y;
                TextPaint tp = new TextPaint();
                tp.setColor(strokeRealColor);
                curStrokeRecord.textPaint = tp; // Clones the mPaint object
                textWindowCallback.onText(this, curStrokeRecord);
                return;
            }
            strokeRecordList.add(curStrokeRecord);
        } else if (editMode == EDIT_PHOTO) {
            float[] downPoint = new float[]{downX, downY};
            float[] invertPoint = new float[2];
            Matrix invertMatrix = new Matrix();
            for (DrawRecord record : photoRecordList) {
                record.matrix.invert(invertMatrix);
                invertMatrix.mapPoints(invertPoint, downPoint);
                if (record.photoRectSrc.contains(invertPoint[0], invertPoint[1])) {
                    setCurPhotoRecord(record);
                    break;
                }
            }
        }

    }


    private void touch_move(float x, float y) {
        if (editMode == EDIT_STROKE) {
            if (strokeType == STROKE_TYPE_ERASER) {
                m_Path.quadTo(preX, preY, (x + preX) / 2, (y + preY) / 2);
            } else if (strokeType == STROKE_TYPE_DRAW) {
                m_Path.quadTo(preX, preY, (x + preX) / 2, (y + preY) / 2);
            } else if (strokeType == STROKE_TYPE_LINE) {
                m_Path.reset();
                m_Path.moveTo(downX, downY);
                m_Path.lineTo(x, y);
            } else if (strokeType == STROKE_TYPE_CIRCLE || strokeType == STROKE_TYPE_RECTANGLE) {
                curStrokeRecord.rect.set(downX < x ? downX : x, downY < y ? downY : y, downX > x ? downX : x, downY > y ? downY : y);
            } else if (strokeType == STROKE_TYPE_TEXT) {

            }
        } else if (editMode == EDIT_PHOTO) {

        }
        preX = x;
        preY = y;
    }


    private void touch_up() {
    }


    /**
     * Returns a new bitmap associated with drawed canvas
     */
    public Bitmap getBitmap() {
        if (strokeRecordList.size() == 0)
            return null;

        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            bitmap.eraseColor(background);
        }
        Canvas canvas = new Canvas(bitmap);
        drawRecord(canvas);
        return bitmap;
    }


    /*
     * 删除一笔
     */
    public void undo() {
        if (strokeRecordList.size() > 0) {
            strokeRedoList.add(strokeRecordList.get(strokeRecordList.size() - 1));
            strokeRecordList.remove(strokeRecordList.size() - 1);
            invalidate();
        }
    }


    /*
     * 撤销
     */
    public void redo() {
        if (strokeRedoList.size() > 0) {
            strokeRecordList.add(strokeRedoList.get(strokeRedoList.size() - 1));
            strokeRedoList.remove(strokeRedoList.size() - 1);
        }
        invalidate();
    }


    public int getRedoCount() {
        return strokeRedoList.size();
    }


    public int getRecordCount() {
        return strokeRecordList.size();
    }


    public int getStrokeSize() {
        return Math.round(this.strokeSize);
    }


    public void setSize(int size, int eraserOrStroke) {
        switch (eraserOrStroke) {
            case STROKE_TYPE_DRAW:
                strokeSize = size;
                break;
            case STROKE_TYPE_ERASER:
                eraserSize = size;
                break;
        }

    }


    public void erase() {
        strokeRecordList.clear();
        strokeRedoList.clear();
        // 先判断是否已经回收
        if (bitmap != null && !bitmap.isRecycled()) {
            // 回收并且置为null
            bitmap.recycle();
            bitmap = null;
        }
        System.gc();
        invalidate();
    }


    public void setOnDrawChangedListener(OnDrawChangedListener listener) {
        this.onDrawChangedListener = listener;
    }

    public interface OnDrawChangedListener {

        public void onDrawChanged();
    }

    public void addPhotoByPath(String path) {
        Bitmap sampleBM = null;
        if (path.contains(Environment.getExternalStorageDirectory().toString())) {
            sampleBM = setSDCardPhoto(path);
        } else {
            sampleBM = setAssetsPhoto(path);
        }
        if (sampleBM != null) {
            DrawRecord newRecord = new DrawRecord(STROKE_TYPE_BITMAP);
            newRecord.bitmap = sampleBM;
            newRecord.photoRectSrc = new RectF(0, 0, newRecord.bitmap.getWidth(), newRecord.bitmap.getHeight());
            newRecord.scaleMax = getMaxScale(newRecord.photoRectSrc);//放大倍数
            newRecord.matrix = new Matrix();
            newRecord.matrix.postTranslate(getWidth() / 2 - sampleBM.getWidth() / 2, getHeight() / 2 - sampleBM.getHeight() / 2);
            newRecord.paint = new Paint();
            newRecord.paint.setColor(Color.GRAY);
            newRecord.paint.setStrokeWidth(BitmapUtils.dip2px(mContext, 0.8f));
            newRecord.paint.setStyle(Paint.Style.STROKE);
            setCurPhotoRecord(newRecord);
        }
    }

    private void setCurPhotoRecord(DrawRecord record) {
        photoRecordList.remove(record);
        photoRecordList.add(record);
        curPhotoRecord = record;
        invalidate();
    }

    public Bitmap setSDCardPhoto(String path) {
        File file = new File(path);
        if (file.exists()) {
            return BitmapUtils.decodeSampleBitMapFromFile(mContext, path, simpleScale);
        } else {
            return null;
        }
    }

    public Bitmap setAssetsPhoto(String path) {
        return BitmapUtils.getBitmapFromAssets(mContext, path);
    }

    public void setEditMode(int editMode) {
        this.editMode = editMode;
    }
}