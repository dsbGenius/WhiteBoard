package com.yinghe.whiteboardlib.bean;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ChiEr on 16/6/16.
 */
public class WhiteBoardData {
    List<PhotoRecord> strokeRecordList;
    List<StrokeRecord> photoRecordList;
    Bitmap backgroundBM;

    WhiteBoardData() {
        strokeRecordList = new ArrayList<>();
        photoRecordList = new ArrayList<>();
    }
}
