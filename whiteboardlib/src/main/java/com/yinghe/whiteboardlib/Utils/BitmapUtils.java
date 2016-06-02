package com.yinghe.whiteboardlib.Utils;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.WindowManager;

/**
 * Created by TangentLu on 2015/8/19.
 */
public class BitmapUtils {

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

    public static Bitmap decodeSampleBitMapFromFile(Context context, String filePath, float sampleScale) {
        //先得到bitmap的高宽
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        //再用屏幕一半高宽、缩小后的高宽对比，取小值进行缩放
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int reqWidth = wm.getDefaultDisplay().getWidth()/2;
        int reqHeight = wm.getDefaultDisplay().getWidth()/2;
        int scaleWidth = (int) (options.outWidth * sampleScale);
        int scaleHeight = (int) (options.outHeight * sampleScale);
        reqWidth = Math.min(reqWidth, scaleWidth);
        reqHeight = Math.min(reqHeight, scaleHeight);
        options = sampleBitmapOptions(context, options, reqWidth, reqHeight);
        Bitmap bm = BitmapFactory.decodeFile(filePath, options);
        Log.e("xxx", bm.getByteCount() + "");
        return bm;
    }
    public static Bitmap decodeSampleBitMapFromResource(Context context, int resId, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), resId, options);
        options = sampleBitmapOptions(context, options, reqWidth, reqHeight);
        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), resId, options);
        Log.e("xxx", bm.getByteCount() + "");
        return bm;
    }

    public static BitmapFactory.Options sampleBitmapOptions(
            Context context, BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int targetDensity = context.getResources().getDisplayMetrics().densityDpi;
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        double xSScale = ((double) options.outWidth) / ((double) reqWidth);
        double ySScale = ((double) options.outHeight) / ((double) reqHeight);

        double startScale = xSScale > ySScale ? xSScale : ySScale;

        options.inScaled = true;
        options.inDensity = (int) (targetDensity * startScale);
        options.inTargetDensity = targetDensity;
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        return options;
    }
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }


}
