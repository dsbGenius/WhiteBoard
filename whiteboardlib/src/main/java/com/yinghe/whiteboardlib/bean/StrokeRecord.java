package com.yinghe.whiteboardlib.bean;

import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.text.TextPaint;

public class StrokeRecord {
    public static final int STROKE_TYPE_ERASER = 1;
    public static final int STROKE_TYPE_DRAW = 2;
    public static final int STROKE_TYPE_LINE = 3;
    public static final int STROKE_TYPE_CIRCLE = 4;
    public static final int STROKE_TYPE_RECTANGLE = 5;
    public static final int STROKE_TYPE_TEXT = 6;

    public int type;//记录类型
    public Paint paint;//笔类
    public Path path;//画笔路径数据
    public PointF[] linePoints; //线数据
    public RectF rect; //圆、矩形区域
    public String text;//文字
    public TextPaint textPaint;//笔类

    public int textOffX;
    public int textOffY;
    public int textWidth;//文字位置

    public StrokeRecord(int type) {
        this.type = type;
    }
}