package com.yinghe.whiteboardlib.Utils;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Matrix;

/**
 * Created by TangentLu on 2015/8/19.
 */
public class Utils {

    public static int px2dip(Context context,float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
    public static int dip2px(Context context,float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static boolean isLandScreen(Context context) {
        int ori =context.getResources().getConfiguration().orientation;//获取屏幕方向
        return ori == Configuration.ORIENTATION_LANDSCAPE;
    }
}
