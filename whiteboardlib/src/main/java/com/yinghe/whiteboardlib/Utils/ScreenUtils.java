package com.yinghe.whiteboardlib.Utils;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import java.lang.reflect.Field;

/**
 *  屏幕工具
 * Created by nereo on 15/11/19.
 * Updated by nereo on 2016/1/19.
 */
public class ScreenUtils {

    public static Point getScreenSize(Context context){
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point out = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            display.getSize(out);
        }else{
            int width = display.getWidth();
            int height = display.getHeight();
            out.set(width, height);
        }
        return out;
    }
    /**
     * 获取状态栏高度
     */
    public static int getStatusBarHeight(Context context) {
        Class<?> c = null;
        Object obj = null;
        Field field = null;
        int x = 0, sbar = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            sbar = context.getResources().getDimensionPixelSize(x);
        } catch (Exception e1) {
            Log.e("getStatusBarHight()", "get status bar height fail");
            e1.printStackTrace();
        }
        int statusBarHeight = sbar;
        Log.i("onPreDraw", "statusBarHeight: "+statusBarHeight);
        return statusBarHeight;
    }

    public static void hideInput(View v) {
        InputMethodManager inputManager = (InputMethodManager) v
                .getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        //调用系统输入法
        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);

    }
    public static void showInput(View v) {
        InputMethodManager inputManager = (InputMethodManager) v
                .getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        //调用系统输入法
        inputManager.showSoftInput(v, InputMethodManager.SHOW_FORCED);
    }

    public static void toggleInput(Context context) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }


}
