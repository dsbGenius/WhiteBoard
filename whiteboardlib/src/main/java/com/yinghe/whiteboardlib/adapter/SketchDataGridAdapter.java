package com.yinghe.whiteboardlib.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.yinghe.whiteboardlib.R;
import com.yinghe.whiteboardlib.Utils.ScreenUtils;
import com.yinghe.whiteboardlib.bean.SketchData;

import java.util.List;

/**
 * 图片Adapter
 */
public class SketchDataGridAdapter extends BaseAdapter {


    public interface OnDeleteCallback {
        void onDeleteCallback(int position);
    }

    float ratio;


    List<SketchData> sketchDataList;

    private Context mContext;
    private LayoutInflater mInflater;
    private OnDeleteCallback onDeleteCallback;

    public SketchDataGridAdapter(Context context, List<SketchData> sketchDataList, OnDeleteCallback onDeleteCallback) {
        this.mContext = context;
        ratio = (float) ScreenUtils.getScreenSize(context).x / ScreenUtils.getScreenSize(context).y;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.onDeleteCallback = onDeleteCallback;
        this.sketchDataList = sketchDataList;
    }

    @Override
    public int getCount() {
        return sketchDataList.size();
    }

    @Override
    public Bitmap getItem(int position) {
        return sketchDataList.get(position).thumbnailBM;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            View view = mInflater.inflate(R.layout.grid_item_sketch_data, null);
            holder = new ViewHolder();
            bindView(view, holder);
            holder.deleteIV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onDeleteCallback != null)
                        onDeleteCallback.onDeleteCallback(position);
                }
            });
            view.setTag(holder);
            convertView = view;
        } else {
            holder = (ViewHolder) convertView.getTag();
            ViewGroup.LayoutParams layoutParams = convertView.getLayoutParams();
            layoutParams.height = (int) (layoutParams.width * ratio);
        }
        showData(holder, position);
        return convertView;
    }

    private void showData(ViewHolder holder, int position) {
        if (getItem(position) != null) {
            Drawable drawable = new BitmapDrawable(mContext.getResources(), getItem(position));
            holder.sketchIV.setImageDrawable(drawable);
        }
        Log.d("", "getView: w=" + holder.sketchIV.getWidth() + "h=" + holder.sketchIV.getHeight());

        holder.numberTV.setText(position + 1 + "");
    }

    private void bindView(View view, ViewHolder holder) {
        holder.sketchIV = (ImageView) view.findViewById(R.id.grid_sketch);
        holder.deleteIV = (ImageView) view.findViewById(R.id.grid_delete);
        holder.numberTV = (TextView) view.findViewById(R.id.grid_number);
    }

    class ViewHolder {
        ImageView sketchIV;
        ImageView deleteIV;
        TextView numberTV;
    }
}
