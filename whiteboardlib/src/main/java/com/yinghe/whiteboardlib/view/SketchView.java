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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;

import com.yinghe.whiteboardlib.R;
import com.yinghe.whiteboardlib.Utils.BitmapUtils;
import com.yinghe.whiteboardlib.Utils.ScreenUtils;
import com.yinghe.whiteboardlib.bean.PhotoRecord;
import com.yinghe.whiteboardlib.bean.SketchData;
import com.yinghe.whiteboardlib.bean.StrokeRecord;

import java.io.File;
import java.util.logging.Logger;

import static com.yinghe.whiteboardlib.Utils.BitmapUtils.createBitmapThumbnail;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_CIRCLE;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_DRAW;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_ERASER;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_LINE;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_RECTANGLE;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_TEXT;


public class SketchView extends View implements OnTouchListener {

    public static final int EDIT_STROKE = 1;
    public static final int EDIT_PHOTO = 2;
    public static final int DEFAULT_STROKE_SIZE = 3;
    public static final int DEFAULT_STROKE_ALPHA = 100;
    public static final int DEFAULT_ERASER_SIZE = 50;
    public static final float TOUCH_TOLERANCE = 4;
    public static final int ACTION_NONE = 0;
    public static final int ACTION_DRAG = 1;
    public static final int ACTION_SCALE = 2;
    public static final int ACTION_ROTATE = 3;
    //    public int curSketchData.editMode = EDIT_STROKE;
    public static float SCALE_MAX = 4.0f;
    public static float SCALE_MIN = 0.2f;
    public static float SCALE_MIN_LEN;
    public final String TAG = getClass().getSimpleName();
    public Paint boardPaint;

    public Bitmap mirrorMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.mark_copy);
    public Bitmap deleteMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.mark_delete);
    public Bitmap rotateMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.mark_rotate);
    public Bitmap resetMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.mark_reset);
    //    Bitmap rotateMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.test);
    public RectF markerCopyRect = new RectF(0, 0, mirrorMarkBM.getWidth(), mirrorMarkBM.getHeight());//镜像标记边界
    public RectF markerDeleteRect = new RectF(0, 0, deleteMarkBM.getWidth(), deleteMarkBM.getHeight());//删除标记边界
    public RectF markerRotateRect = new RectF(0, 0, rotateMarkBM.getWidth(), rotateMarkBM.getHeight());//旋转标记边界
    public RectF markerResetRect = new RectF(0, 0, resetMarkBM.getWidth(), resetMarkBM.getHeight());//旋转标记边界
    public SketchData curSketchData;
    //    public Bitmap curSketchData.backgroundBM;
    public Rect backgroundSrcRect = new Rect();
    public Rect backgroundDstRect = new Rect();
    public StrokeRecord curStrokeRecord;
    public PhotoRecord curPhotoRecord;
    public int actionMode;
    public float simpleScale = 0.5f;//图片载入的缩放倍数
    public TextWindowCallback textWindowCallback;
    public float strokeSize = DEFAULT_STROKE_SIZE;
    public int strokeRealColor = Color.BLACK;//画笔实际颜色
    public int strokeColor = Color.BLACK;//画笔颜色
    public int strokeAlpha = 255;//画笔透明度
    public float eraserSize = DEFAULT_ERASER_SIZE;
    public Path strokePath;
    public Paint strokePaint;
    public float downX, downY, preX, preY, curX, curY;
    public int mWidth, mHeight;
    //    public List<PhotoRecord> curSketchData.photoRecordList;
//    public List<StrokeRecord> curSketchData.strokeRecordList;
//    public List<StrokeRecord> curSketchData.strokeRedoList;
    public Context mContext;
    public int drawDensity = 2;//绘制密度,数值越高图像质量越低、性能越好
    /**
     * 缩放手势
     */
    public ScaleGestureDetector mScaleGestureDetector = null;
    public OnDrawChangedListener onDrawChangedListener;

    public SketchView(Context context, AttributeSet attr) {
        super(context, attr);
        this.mContext = context;
//        setSketchData(new SketchData());
        initParams(context);
        if (isFocusable()) {
            this.setOnTouchListener(this);
            mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
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
        }
        invalidate();
    }

    public void setTextWindowCallback(TextWindowCallback textWindowCallback) {
        this.textWindowCallback = textWindowCallback;
    }

    public int getStrokeType() {
        return curSketchData.strokeType;
    }

//    public int curSketchData.strokeType = StrokeRecord.STROKE_TYPE_DRAW;

    public void setStrokeType(int strokeType) {
        this.curSketchData.strokeType = strokeType;
    }

    public void setSketchData(SketchData sketchData) {
        this.curSketchData = sketchData;
        curPhotoRecord = null;
    }

    public void updateSketchData(SketchData sketchData) {
        if (curSketchData != null)
            curSketchData.thumbnailBM = getThumbnailResultBitmap();//更新数据前先保存上一份数据的缩略图
        setSketchData(sketchData);
    }

    public void initParams(Context context) {

//        setFocusable(true);
//        setFocusableInTouchMode(true);
        setBackgroundColor(Color.WHITE);

        strokePaint = new Paint();
        strokePaint.setAntiAlias(true);
        strokePaint.setDither(true);
        strokePaint.setColor(strokeRealColor);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeWidth(strokeSize);

        boardPaint = new Paint();
        boardPaint.setColor(Color.GRAY);
        boardPaint.setStrokeWidth(ScreenUtils.dip2px(mContext, 0.8f));
        boardPaint.setStyle(Paint.Style.STROKE);

        SCALE_MIN_LEN = ScreenUtils.dip2px(context, 20);
    }

    public void setStrokeAlpha(int mAlpha) {
        this.strokeAlpha = mAlpha;
        calculColor();
        strokePaint.setStrokeWidth(strokeSize);
    }

    public void setStrokeColor(int color) {
        strokeColor = color;
        calculColor();
        strokePaint.setColor(strokeRealColor);
    }

    public void calculColor() {
        strokeRealColor = Color.argb(strokeAlpha, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(mWidth, mHeight);
    }

    int[] location = new int[2];

    @Override
    public boolean onTouch(View arg0, MotionEvent event) {
        getLocationInWindow(location); //获取在当前窗口内的绝对坐标
        curX = (event.getRawX() - location[0]) / drawDensity;
        curY = (event.getRawY() - location[1]) / drawDensity;
        int toolType = event.getToolType(0);
//        //检测到手指点击自动进入拖动图片模式
//        if (toolType == MotionEvent.TOOL_TYPE_FINGER&&curSketchData.editMode == EDIT_STROKE) {
//            curSketchData.editMode = EDIT_PHOTO;
//        } else if (toolType == MotionEvent.TOOL_TYPE_STYLUS){//检测到手写板开始绘画则自动进入绘画模式
//            curSketchData.editMode = EDIT_STROKE;
//        }
//        Log.d(getClass().getSimpleName(), "onTouch======" + toolType);
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                float downDistance = spacing(event);
                if (actionMode == ACTION_DRAG && downDistance > 10)//防止误触
                    actionMode = ACTION_SCALE;
                break;
            case MotionEvent.ACTION_DOWN:
                touch_down();
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(event);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        preX = curX;
        preY = curY;
        return true;
    }

    public float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawBackground(canvas);
        drawRecord(canvas);
        if (onDrawChangedListener != null)
            onDrawChangedListener.onDrawChanged();
    }

    public void drawBackground(Canvas canvas) {
        if (curSketchData.backgroundBM != null) {
//            Rect dstRect = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
//            canvas.drawBitmap(curSketchData.backgroundBM, backgroundSrcRect, backgroundDstRect, null);
            Matrix matrix = new Matrix();
            float wScale = (float) canvas.getWidth() / curSketchData.backgroundBM.getWidth();
            float hScale = (float) canvas.getHeight() / curSketchData.backgroundBM.getHeight();
            matrix.postScale(wScale, hScale);
            canvas.drawBitmap(curSketchData.backgroundBM, matrix, null);
//            canvas.drawBitmap(curSketchData.backgroundBM, backgroundSrcRect, dstRect, null);
            Log.d(TAG, "drawBackground:src= " + backgroundSrcRect.toString() + ";dst=" + backgroundDstRect.toString());
        } else {
//            try {
//                setBackgroundByPath("background/bg_yellow_board.png");
//            canvas.drawColor(Color.rgb(246, 246, 246));
//            } catch (Exception e) {
//                e.printStackTrace();
//            canvas.drawColor(Color.rgb(246, 246, 246));
            canvas.drawColor(Color.rgb(239, 234, 224));
//            }
        }
    }

    public void drawRecord(Canvas canvas) {
        drawRecord(canvas, true);
    }

    public Bitmap tempBitmap;//临时绘制的bitmap
    public Canvas tempCanvas;
    public Bitmap tempHoldBitmap;//保存已固化的笔画bitmap
    public Canvas tempHoldCanvas;

    public void drawRecord(Canvas canvas, boolean isDrawBoard) {
        if (curSketchData != null) {
            for (PhotoRecord record : curSketchData.photoRecordList) {
                if (record != null) {
                    Log.d(getClass().getSimpleName(), "drawRecord" + record.bitmap.toString());
                    canvas.drawBitmap(record.bitmap, record.matrix, null);
                }
            }
            if (isDrawBoard && curSketchData.editMode == EDIT_PHOTO && curPhotoRecord != null) {
                SCALE_MAX = curPhotoRecord.scaleMax;
                float[] photoCorners = calculateCorners(curPhotoRecord);//计算图片四个角点和中心点
                drawBoard(canvas, photoCorners);//绘制图形边线
                drawMarks(canvas, photoCorners);//绘制边角图片
            }
            //新建一个临时画布，以便橡皮擦生效
            if (tempBitmap == null) {
                tempBitmap = Bitmap.createBitmap(getWidth() / drawDensity, getHeight() / drawDensity, Bitmap.Config.ARGB_4444);
                tempCanvas = new Canvas(tempBitmap);
            }
            //新建一个临时画布，以便保存过多的画笔
            if (tempHoldBitmap == null) {
                tempHoldBitmap = Bitmap.createBitmap(getWidth() / drawDensity, getHeight() / drawDensity, Bitmap.Config.ARGB_4444);
                tempHoldCanvas = new Canvas(tempHoldBitmap);
            }
//            Canvas tempCanvas = new Canvas(tempBitmap);
            //把十个操作以前的笔画全都画进固化层
            while (curSketchData.strokeRecordList.size() > 10) {
                StrokeRecord record = curSketchData.strokeRecordList.get(0);
                int type = record.type;
                if (type == StrokeRecord.STROKE_TYPE_ERASER) {//橡皮擦需要在固化层也绘制
                    tempHoldCanvas.drawPath(record.path, record.paint);
                } else if (type == StrokeRecord.STROKE_TYPE_DRAW || type == StrokeRecord.STROKE_TYPE_LINE) {
                    tempHoldCanvas.drawPath(record.path, record.paint);
                } else if (type == STROKE_TYPE_CIRCLE) {
                    tempHoldCanvas.drawOval(record.rect, record.paint);
                } else if (type == STROKE_TYPE_RECTANGLE) {
                    tempHoldCanvas.drawRect(record.rect, record.paint);
                } else if (type == STROKE_TYPE_TEXT) {
                    if (record.text != null) {
                        StaticLayout layout = new StaticLayout(record.text, record.textPaint, record.textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
                        tempHoldCanvas.translate(record.textOffX, record.textOffY);
                        layout.draw(tempHoldCanvas);
                        tempHoldCanvas.translate(-record.textOffX, -record.textOffY);
                    }
                }
                curSketchData.strokeRecordList.remove(0);
            }
            clearCanvas(tempCanvas);//清空画布
            tempCanvas.drawColor(Color.TRANSPARENT);
            tempCanvas.drawBitmap(tempHoldBitmap, new Rect(0, 0, tempHoldBitmap.getWidth(), tempHoldBitmap.getHeight()), new Rect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight()), null);
            for (StrokeRecord record : curSketchData.strokeRecordList) {
                int type = record.type;
                if (type == StrokeRecord.STROKE_TYPE_ERASER) {//橡皮擦需要在固化层也绘制
                    tempCanvas.drawPath(record.path, record.paint);
                    tempHoldCanvas.drawPath(record.path, record.paint);
                } else if (type == StrokeRecord.STROKE_TYPE_DRAW || type == StrokeRecord.STROKE_TYPE_LINE) {
                    tempCanvas.drawPath(record.path, record.paint);
                } else if (type == STROKE_TYPE_CIRCLE) {
                    tempCanvas.drawOval(record.rect, record.paint);
                } else if (type == STROKE_TYPE_RECTANGLE) {
                    tempCanvas.drawRect(record.rect, record.paint);
                } else if (type == STROKE_TYPE_TEXT) {
                    if (record.text != null) {
                        StaticLayout layout = new StaticLayout(record.text, record.textPaint, record.textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
                        tempCanvas.translate(record.textOffX, record.textOffY);
                        layout.draw(tempCanvas);
                        tempCanvas.translate(-record.textOffX, -record.textOffY);
                    }
                }
            }
            canvas.drawBitmap(tempBitmap, new Rect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight()), new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), null);
        }

    }

    /**
     * 清理画布canvas
     *
     * @param temptCanvas
     */
    public void clearCanvas(Canvas temptCanvas) {
        Paint p = new Paint();
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        temptCanvas.drawPaint(p);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
    }

    //绘制图像边线（由于图形旋转或不一定是矩形，所以用Path绘制边线）
    public void drawBoard(Canvas canvas, float[] photoCorners) {
        Path photoBorderPath = new Path();
        photoBorderPath.moveTo(photoCorners[0], photoCorners[1]);
        photoBorderPath.lineTo(photoCorners[2], photoCorners[3]);
        photoBorderPath.lineTo(photoCorners[4], photoCorners[5]);
        photoBorderPath.lineTo(photoCorners[6], photoCorners[7]);
        photoBorderPath.lineTo(photoCorners[0], photoCorners[1]);
        canvas.drawPath(photoBorderPath, boardPaint);
    }

    //绘制边角操作图标
    public void drawMarks(Canvas canvas, float[] photoCorners) {
        float x;
        float y;
        x = photoCorners[0] - markerCopyRect.width() / 2;
        y = photoCorners[1] - markerCopyRect.height() / 2;
        markerCopyRect.offsetTo(x, y);
        canvas.drawBitmap(mirrorMarkBM, x, y, null);

        x = photoCorners[2] - markerDeleteRect.width() / 2;
        y = photoCorners[3] - markerDeleteRect.height() / 2;
        markerDeleteRect.offsetTo(x, y);
        canvas.drawBitmap(deleteMarkBM, x, y, null);

        x = photoCorners[4] - markerRotateRect.width() / 2;
        y = photoCorners[5] - markerRotateRect.height() / 2;
        markerRotateRect.offsetTo(x, y);
        canvas.drawBitmap(rotateMarkBM, x, y, null);

        x = photoCorners[6] - markerResetRect.width() / 2;
        y = photoCorners[7] - markerResetRect.height() / 2;
        markerResetRect.offsetTo(x, y);
        canvas.drawBitmap(resetMarkBM, x, y, null);
    }

    public float[] calculateCorners(PhotoRecord record) {
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

    public float getMaxScale(RectF photoSrc) {
        return Math.max(getWidth(), getHeight()) / Math.max(photoSrc.width(), photoSrc.height());
//        SCALE_MIN = SCALE_MAX / 5;
    }

    public void addStrokeRecord(StrokeRecord record) {
        curSketchData.strokeRecordList.add(record);
        invalidate();
    }

    public void touch_down() {
        downX = curX;
        downY = curY;
        if (curSketchData.editMode == EDIT_STROKE) {
            curSketchData.strokeRedoList.clear();
            curStrokeRecord = new StrokeRecord(curSketchData.strokeType);
            strokePaint.setAntiAlias(true);//由于降低密度绘制，所以需要抗锯齿
            if (curSketchData.strokeType == StrokeRecord.STROKE_TYPE_ERASER) {
                strokePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));//关键代码
            } else {
                strokePaint.setXfermode(null);//关键代码
            }
            if (curSketchData.strokeType == STROKE_TYPE_ERASER) {
                strokePath = new Path();
                strokePath.moveTo(downX, downY);
                strokePaint.setColor(Color.WHITE);
                strokePaint.setStrokeWidth(eraserSize);
                curStrokeRecord.paint = new Paint(strokePaint); // Clones the mPaint object
                curStrokeRecord.path = strokePath;
            } else if (curSketchData.strokeType == STROKE_TYPE_DRAW || curSketchData.strokeType == STROKE_TYPE_LINE) {
                strokePath = new Path();
                strokePath.moveTo(downX, downY);
                curStrokeRecord.path = strokePath;
                strokePaint.setColor(strokeRealColor);
                strokePaint.setStrokeWidth(strokeSize);
                curStrokeRecord.paint = new Paint(strokePaint); // Clones the mPaint object
            } else if (curSketchData.strokeType == STROKE_TYPE_CIRCLE || curSketchData.strokeType == STROKE_TYPE_RECTANGLE) {
                RectF rect = new RectF(downX, downY, downX, downY);
                curStrokeRecord.rect = rect;
                strokePaint.setColor(strokeRealColor);
                strokePaint.setStrokeWidth(strokeSize);
                curStrokeRecord.paint = new Paint(strokePaint); // Clones the mPaint object
            } else if (curSketchData.strokeType == STROKE_TYPE_TEXT) {
                curStrokeRecord.textOffX = (int) downX;
                curStrokeRecord.textOffY = (int) downY;
                TextPaint tp = new TextPaint();
                tp.setColor(strokeRealColor);
                curStrokeRecord.textPaint = tp; // Clones the mPaint object
                textWindowCallback.onText(this, curStrokeRecord);
                return;
            }
            curSketchData.strokeRecordList.add(curStrokeRecord);
        } else if (curSketchData.editMode == EDIT_PHOTO) {
            float[] downPoint = new float[]{downX * drawDensity, downY * drawDensity};//还原点倍数
            if (isInMarkRect(downPoint)) {// 先判操作标记区域
                return;
            }
            if (isInPhotoRect(curPhotoRecord, downPoint)) {//再判断是否点击了当前图片
                actionMode = ACTION_DRAG;
                return;
            }
            selectPhoto(downPoint);//最后判断是否点击了其他图片
        }
    }

    //judge click which photo，then can edit the photo
    public void selectPhoto(float[] downPoint) {
        PhotoRecord clickRecord = null;
        for (int i = curSketchData.photoRecordList.size() - 1; i >= 0; i--) {
            PhotoRecord record = curSketchData.photoRecordList.get(i);
            if (isInPhotoRect(record, downPoint)) {
                clickRecord = record;
                break;
            }
        }
        if (clickRecord != null) {
            setCurPhotoRecord(clickRecord);
            actionMode = ACTION_DRAG;
        } else {
            actionMode = ACTION_NONE;
        }
    }

    public boolean isInMarkRect(float[] downPoint) {
        if (markerRotateRect.contains(downPoint[0], (int) downPoint[1])) {//判断是否在区域内
            actionMode = ACTION_ROTATE;
            return true;
        }
        if (markerDeleteRect.contains(downPoint[0], (int) downPoint[1])) {//判断是否在区域内
            curSketchData.photoRecordList.remove(curPhotoRecord);
            setCurPhotoRecord(null);
            actionMode = ACTION_NONE;
            return true;
        }
        if (markerCopyRect.contains(downPoint[0], (int) downPoint[1])) {//判断是否在区域内
            PhotoRecord newRecord = initPhotoRecord(curPhotoRecord.bitmap);
            newRecord.matrix = new Matrix(curPhotoRecord.matrix);
            newRecord.matrix.postTranslate(ScreenUtils.dip2px(mContext, 20), ScreenUtils.dip2px(mContext, 20));//偏移小段距离以分辨新复制的图片
            setCurPhotoRecord(newRecord);
            actionMode = ACTION_NONE;
            return true;
        }
        if (markerResetRect.contains(downPoint[0], (int) downPoint[1])) {//判断是否在区域内
            curPhotoRecord.matrix.reset();
            curPhotoRecord.matrix.setTranslate(getWidth() / 2 - curPhotoRecord.photoRectSrc.width() / 2,
                    getHeight() / 2 - curPhotoRecord.photoRectSrc.height() / 2);
            actionMode = ACTION_NONE;
            return true;
        }
        return false;
    }

    public boolean isInPhotoRect(PhotoRecord record, float[] downPoint) {
        if (record != null) {
            float[] invertPoint = new float[2];
            Matrix invertMatrix = new Matrix();
            record.matrix.invert(invertMatrix);
            invertMatrix.mapPoints(invertPoint, downPoint);
            return record.photoRectSrc.contains(invertPoint[0], invertPoint[1]);
        }
        return false;
    }

    public void touch_move(MotionEvent event) {
        if (curSketchData.editMode == EDIT_STROKE) {
            if (curSketchData.strokeType == STROKE_TYPE_ERASER) {
                strokePath.quadTo(preX, preY, (curX + preX) / 2, (curY + preY) / 2);
            } else if (curSketchData.strokeType == STROKE_TYPE_DRAW) {
                strokePath.quadTo(preX, preY, (curX + preX) / 2, (curY + preY) / 2);
            } else if (curSketchData.strokeType == STROKE_TYPE_LINE) {
                strokePath.reset();
                strokePath.moveTo(downX, downY);
                strokePath.lineTo(curX, curY);
            } else if (curSketchData.strokeType == STROKE_TYPE_CIRCLE || curSketchData.strokeType == STROKE_TYPE_RECTANGLE) {
                curStrokeRecord.rect.set(downX < curX ? downX : curX, downY < curY ? downY : curY, downX > curX ? downX : curX, downY > curY ? downY : curY);
            } else if (curSketchData.strokeType == STROKE_TYPE_TEXT) {

            }
        } else if (curSketchData.editMode == EDIT_PHOTO && curPhotoRecord != null) {
            if (actionMode == ACTION_DRAG) {
                onDragAction((curX - preX) * drawDensity, (curY - preY) * drawDensity);
            } else if (actionMode == ACTION_ROTATE) {
                onRotateAction(curPhotoRecord);
            } else if (actionMode == ACTION_SCALE) {
                mScaleGestureDetector.onTouchEvent(event);
            }
        }
        preX = curX;
        preY = curY;
    }

    public void onScaleAction(ScaleGestureDetector detector) {
        float[] photoCorners = calculateCorners(curPhotoRecord);
        //目前图片对角线长度
        float len = (float) Math.sqrt(Math.pow(photoCorners[0] - photoCorners[4], 2) + Math.pow(photoCorners[1] - photoCorners[5], 2));
        double photoLen = Math.sqrt(Math.pow(curPhotoRecord.photoRectSrc.width(), 2) + Math.pow(curPhotoRecord.photoRectSrc.height(), 2));
        float scaleFactor = detector.getScaleFactor();
        //设置Matrix缩放参数
        if ((scaleFactor < 1 && len >= photoLen * SCALE_MIN && len >= SCALE_MIN_LEN) || (scaleFactor > 1 && len <= photoLen * SCALE_MAX)) {
            Log.e(scaleFactor + "", scaleFactor + "");
            curPhotoRecord.matrix.postScale(scaleFactor, scaleFactor, photoCorners[8], photoCorners[9]);
        }
    }

    public void onRotateAction(PhotoRecord record) {
        float[] corners = calculateCorners(record);
        //放大
        //目前触摸点与图片显示中心距离,curX*drawDensity为还原缩小密度点数值
        float a = (float) Math.sqrt(Math.pow(curX * drawDensity - corners[8], 2) + Math.pow(curY * drawDensity - corners[9], 2));
        //目前上次旋转图标与图片显示中心距离
        float b = (float) Math.sqrt(Math.pow(corners[4] - corners[0], 2) + Math.pow(corners[5] - corners[1], 2)) / 2;
//        Log.e(TAG, "onRotateAction: a=" + a + ";b=" + b);
        //设置Matrix缩放参数
        double photoLen = Math.sqrt(Math.pow(record.photoRectSrc.width(), 2) + Math.pow(record.photoRectSrc.height(), 2));
        if (a >= photoLen / 2 * SCALE_MIN && a >= SCALE_MIN_LEN && a <= photoLen / 2 * SCALE_MAX) {
            //这种计算方法可以保持旋转图标坐标与触摸点同步缩放
            float scale = a / b;
            record.matrix.postScale(scale, scale, corners[8], corners[9]);
        }

        //旋转
        //根据移动坐标的变化构建两个向量，以便计算两个向量角度.
        PointF preVector = new PointF();
        PointF curVector = new PointF();
        preVector.set((preX * drawDensity - corners[8]), preY * drawDensity - corners[9]);//旋转后向量
        curVector.set(curX * drawDensity - corners[8], curY * drawDensity - corners[9]);//旋转前向量
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
        record.matrix.postRotate((float) dAngle, corners[8], corners[9]);
    }

    /**
     * 获取p1到p2的线段的长度
     *
     * @return
     */
    public double getVectorLength(PointF vector) {
        return Math.sqrt(vector.x * vector.x + vector.y * vector.y);
    }

    public void onDragAction(float distanceX, float distanceY) {
        curPhotoRecord.matrix.postTranslate((int) distanceX, (int) distanceY);
    }

    public void touch_up() {
    }

    @NonNull
    public Bitmap getResultBitmap() {
        return getResultBitmap(null);
    }

    @NonNull
    public Bitmap getResultBitmap(Bitmap addBitmap) {
        Bitmap newBM = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.RGB_565);
//        Bitmap newBM = Bitmap.createBitmap(1280, 800, Bitmap.Config.RGB_565);

        Canvas canvas = new Canvas(newBM);
//        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));//抗锯齿
        //绘制背景
        drawBackground(canvas);
        drawRecord(canvas, false);

        if (addBitmap != null) {
            canvas.drawBitmap(addBitmap, 0, 0, null);
        }
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();
//        return newBM;
        Bitmap bitmap = BitmapUtils.createBitmapThumbnail(newBM, true, 800, 1280);
        return bitmap;
    }

    @NonNull
    public void createCurThumbnailBM() {
        curSketchData.thumbnailBM = getThumbnailResultBitmap();
    }

    @NonNull
    public Bitmap getThumbnailResultBitmap() {
        return createBitmapThumbnail(getResultBitmap(), true, ScreenUtils.dip2px(mContext, 200), ScreenUtils.dip2px(mContext, 200));
    }

    /*
     * 删除一笔
     */
    public void undo() {
        if (curSketchData.strokeRecordList.size() > 0) {
            curSketchData.strokeRedoList.add(curSketchData.strokeRecordList.get(curSketchData.strokeRecordList.size() - 1));
            curSketchData.strokeRecordList.remove(curSketchData.strokeRecordList.size() - 1);
            invalidate();
        }
    }

    /*
     * 撤销
     */
    public void redo() {
        if (curSketchData.strokeRedoList.size() > 0) {
            curSketchData.strokeRecordList.add(curSketchData.strokeRedoList.get(curSketchData.strokeRedoList.size() - 1));
            curSketchData.strokeRedoList.remove(curSketchData.strokeRedoList.size() - 1);
        }
        invalidate();
    }

    public int getRedoCount() {
        return curSketchData.strokeRedoList != null ? curSketchData.strokeRedoList.size() : 0;
    }

    public int getRecordCount() {
        return (curSketchData.strokeRecordList != null && curSketchData.photoRecordList != null) ? curSketchData.strokeRecordList.size() + curSketchData.photoRecordList.size() : 0;
    }

    public int getStrokeRecordCount() {
        return curSketchData.strokeRecordList != null ? curSketchData.strokeRecordList.size() : 0;
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
        // 先判断是否已经回收
        for (PhotoRecord record : curSketchData.photoRecordList) {
            if (record != null && record.bitmap != null && !record.bitmap.isRecycled()) {
                record.bitmap.recycle();
                record.bitmap = null;
            }
        }
        if (curSketchData.backgroundBM != null && !curSketchData.backgroundBM.isRecycled()) {
            // 回收并且置为null
            curSketchData.backgroundBM.recycle();
            curSketchData.backgroundBM = null;
        }
        curSketchData.strokeRecordList.clear();
        curSketchData.photoRecordList.clear();
        curSketchData.strokeRedoList.clear();
        curPhotoRecord = null;

        tempCanvas = null;
        tempBitmap.recycle();
        tempBitmap = null;
        tempHoldCanvas = null;
        tempHoldBitmap.recycle();
        tempHoldBitmap = null;
        System.gc();
        invalidate();
    }

    public void setOnDrawChangedListener(OnDrawChangedListener listener) {
        this.onDrawChangedListener = listener;
    }

    public void addPhotoByPath(String path) {
        Bitmap sampleBM = getSampleBitMap(path);
        addPhotoByBitmap(sampleBM);
    }

    public void addPhotoByBitmap(Bitmap sampleBM) {
        if (sampleBM != null) {
            PhotoRecord newRecord = initPhotoRecord(sampleBM);
            setCurPhotoRecord(newRecord);
        } else {
            Toast.makeText(mContext, "图片文件路径有误！", Toast.LENGTH_SHORT).show();
        }
    }

    public void addPhotoByBitmap(Bitmap sampleBM, int[] position) {
        if (sampleBM != null) {
            PhotoRecord newRecord = initPhotoRecord(sampleBM, position);
            setCurPhotoRecord(newRecord);
        } else {
            Toast.makeText(mContext, "图片文件路径有误！", Toast.LENGTH_SHORT).show();
        }
    }

    public void removeCurrentPhotoRecord() {
        curSketchData.photoRecordList.remove(curPhotoRecord);
        setCurPhotoRecord(null);
        actionMode = ACTION_NONE;
    }

    public void setBackgroundByPath(Bitmap bm) {
        setBackgroundByBitmap(bm);
    }

    public void setBackgroundByPath(String path) {
        Bitmap sampleBM = getSampleBitMap(path);
        if (sampleBM != null) {
            setBackgroundByBitmap(sampleBM);
        } else {
            Toast.makeText(mContext, "图片文件路径有误！", Toast.LENGTH_SHORT).show();
        }
    }

    public void setBackgroundByBitmap(Bitmap sampleBM) {
        curSketchData.backgroundBM = sampleBM;
        backgroundSrcRect = new Rect(0, 0, curSketchData.backgroundBM.getWidth(), curSketchData.backgroundBM.getHeight());
        backgroundDstRect = new Rect(0, 0, mWidth, mHeight);
        invalidate();
    }

    public Bitmap getSampleBitMap(String path) {
        Bitmap sampleBM = null;
        if (path.contains(Environment.getExternalStorageDirectory().toString())) {
            sampleBM = getSDCardPhoto(path);
        } else {
            sampleBM = getAssetsPhoto(path);
        }
        return sampleBM;
    }

    @NonNull
    public PhotoRecord initPhotoRecord(Bitmap bitmap) {
        PhotoRecord newRecord = new PhotoRecord();
        newRecord.bitmap = bitmap;
        newRecord.photoRectSrc = new RectF(0, 0, newRecord.bitmap.getWidth(), newRecord.bitmap.getHeight());
        newRecord.scaleMax = getMaxScale(newRecord.photoRectSrc);//放大倍数
        newRecord.matrix = new Matrix();
        newRecord.matrix.postTranslate(getWidth() / 2 - bitmap.getWidth() / 2, getHeight() / 2 - bitmap.getHeight() / 2);
        return newRecord;
    }

    @NonNull
    public PhotoRecord initPhotoRecord(Bitmap bitmap, int[] position) {
        PhotoRecord newRecord = new PhotoRecord();
        newRecord.bitmap = bitmap;
        newRecord.photoRectSrc = new RectF(0, 0, newRecord.bitmap.getWidth(), newRecord.bitmap.getHeight());
        newRecord.scaleMax = getMaxScale(newRecord.photoRectSrc);//放大倍数
        newRecord.matrix = new Matrix();
        newRecord.matrix.postTranslate(position[0], position[1]);
        return newRecord;
    }

    public void setCurPhotoRecord(PhotoRecord record) {
        curSketchData.photoRecordList.remove(record);
        curSketchData.photoRecordList.add(record);
        curPhotoRecord = record;
        invalidate();
    }

    public Bitmap getSDCardPhoto(String path) {
        File file = new File(path);
        if (file.exists()) {
            return BitmapUtils.decodeSampleBitMapFromFile(mContext, path, simpleScale);
        } else {
            return null;
        }
    }

    public Bitmap getAssetsPhoto(String path) {
        return BitmapUtils.getBitmapFromAssets(mContext, path);
    }

    public int getEditMode() {
        return curSketchData.editMode;
    }

    public void setEditMode(int editMode) {
        this.curSketchData.editMode = editMode;
        invalidate();
    }

    public interface TextWindowCallback {
        void onText(View view, StrokeRecord record);
    }

    public interface OnDrawChangedListener {

        public void onDrawChanged();
    }
}