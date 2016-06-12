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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

import com.yinghe.whiteboardlib.bean.StrokeRecord;

import java.util.ArrayList;
import java.util.List;

import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_CIRCLE;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_DRAW;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_ERASER;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_LINE;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_RECTANGLE;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_TEXT;


public class SketchView extends ImageView implements OnTouchListener {

    public interface TextWindowCallback {
        void onText(View view, StrokeRecord record);
    }


    public void setTextWindowCallback(TextWindowCallback textWindowCallback) {
        this.textWindowCallback = textWindowCallback;
    }

    private TextWindowCallback textWindowCallback;
    private static final float TOUCH_TOLERANCE = 4;

    //    public static final int STROKE = 0;
//    public static final int ERASER = 1;
    public static final int DEFAULT_STROKE_SIZE = 7;
    public static final int DEFAULT_STROKE_ALPHA = 100;
    public static final int DEFAULT_ERASER_SIZE = 50;


    private float strokeSize = DEFAULT_STROKE_SIZE;
    private int strokeRealColor = Color.BLACK;//画笔实际颜色
    private int strokeColor = Color.BLACK;//画笔颜色
    private int strokeAlpha = 255;//画笔透明度
    private float eraserSize = DEFAULT_ERASER_SIZE;
    private int background = Color.WHITE;
//    private int background = Color.TRANSPARENT;

    //	private Canvas mCanvas;
    private Path m_Path;
    private Paint m_Paint;
    private float downX, downY, preX, preY;
    private int width, height;

    private List<StrokeRecord> photoRecordList = new ArrayList<>();
    private List<StrokeRecord> strokeRecordList = new ArrayList<>();
    private List<StrokeRecord> strokeRedoList = new ArrayList<>();
    private Context mContext;

    private Bitmap bitmap;
    StrokeRecord curRecord;

    public void setStrokeType(int strokeType) {
        this.strokeType = strokeType;
    }

    public int getStrokeType() {
        return strokeType;
    }

    private int strokeType = StrokeRecord.STROKE_TYPE_DRAW;

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
                touch_start(x, y);
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
        for (StrokeRecord record : photoRecordList) {
            canvas.drawBitmap(record.bitmap, record.matrix, null);
        }
        for (StrokeRecord record : strokeRecordList) {
            int type = record.type;
            if (type == StrokeRecord.STROKE_TYPE_ERASER || type == StrokeRecord.STROKE_TYPE_DRAW || type == StrokeRecord.STROKE_TYPE_LINE) {
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

    public void addStrokeRecord(StrokeRecord record) {
        strokeRecordList.add(record);
        invalidate();
    }

    public void addPhotoRecord(StrokeRecord record) {
        photoRecordList.add(record);
        invalidate();
    }

    private void touch_start(float x, float y) {
        preX = downX = x;
        preY = downY = y;
        strokeRedoList.clear();
//        setStrokeType(6);
        curRecord = new StrokeRecord(strokeType);
        if (strokeType == STROKE_TYPE_ERASER) {
            m_Path = new Path();
            m_Path.moveTo(downX, downY);
            m_Paint.setColor(Color.WHITE);
            m_Paint.setStrokeWidth(eraserSize);
            curRecord.paint = new Paint(m_Paint); // Clones the mPaint object
            curRecord.path = m_Path;
        } else if (strokeType == STROKE_TYPE_DRAW || strokeType == STROKE_TYPE_LINE) {
            m_Path = new Path();
            m_Path.moveTo(downX, downY);
            curRecord.path = m_Path;
            m_Paint.setColor(strokeRealColor);
            m_Paint.setStrokeWidth(strokeSize);
            curRecord.paint = new Paint(m_Paint); // Clones the mPaint object
        } else if (strokeType == STROKE_TYPE_CIRCLE || strokeType == STROKE_TYPE_RECTANGLE) {
            RectF rect = new RectF(x, y, x, y);
            curRecord.rect = rect;
            m_Paint.setColor(strokeRealColor);
            m_Paint.setStrokeWidth(strokeSize);
            curRecord.paint = new Paint(m_Paint); // Clones the mPaint object
        } else if (strokeType == STROKE_TYPE_TEXT) {
            curRecord.textOffX = (int) x;
            curRecord.textOffY = (int) y;
            TextPaint tp = new TextPaint();
            tp.setColor(strokeRealColor);
            curRecord.textPaint = tp; // Clones the mPaint object
            textWindowCallback.onText(this, curRecord);
            return;
        }
        strokeRecordList.add(curRecord);
    }


    private void touch_move(float x, float y) {
        if (strokeType == STROKE_TYPE_ERASER) {
            m_Path.quadTo(preX, preY, (x + preX) / 2, (y + preY) / 2);
        } else if (strokeType == STROKE_TYPE_DRAW) {
            m_Path.quadTo(preX, preY, (x + preX) / 2, (y + preY) / 2);
        } else if (strokeType == STROKE_TYPE_LINE) {
            m_Path.reset();
            m_Path.moveTo(downX, downY);
            m_Path.lineTo(x, y);
        } else if (strokeType == STROKE_TYPE_CIRCLE || strokeType == STROKE_TYPE_RECTANGLE) {
            curRecord.rect.set(downX < x ? downX : x, downY < y ? downY : y, downX > x ? downX : x, downY > y ? downY : y);
        } else if (strokeType == STROKE_TYPE_TEXT) {

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


}