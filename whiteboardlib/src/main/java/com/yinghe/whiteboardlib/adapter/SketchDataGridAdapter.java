package com.yinghe.whiteboardlib.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.yinghe.whiteboardlib.R;
import com.yinghe.whiteboardlib.bean.SketchData;

import java.util.List;

/**
 * 图片Adapter
 */
public class SketchDataGridAdapter extends BaseAdapter {

    public interface OnDeleteCallback {
        void onDeleteCallback(int position);
    }

    List<SketchData> sketchDataList;

    private Context mContext;
    private LayoutInflater mInflater;
    private OnDeleteCallback onDeleteCallback;

    public SketchDataGridAdapter(Context context, List<SketchData> sketchDataList, OnDeleteCallback onDeleteCallback) {
        this.mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.onDeleteCallback = onDeleteCallback;
        this.sketchDataList = sketchDataList;
    }

    @Override
    public int getCount() {
        return sketchDataList.size();
    }

    @Override
    public SketchData getItem(int position) {
        return sketchDataList.get(position);
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
        }
        showData(holder, position);
        return convertView;
    }

    private void showData(ViewHolder holder, int position) {
        Picasso.with(mContext)
                .load(getItem(position).thumbnailFile)
                .placeholder(R.drawable.default_error)
                .tag("aa")
//                .resize(holder.sketchIV.getWidth(), holder.sketchIV.getHeight())
                .resize(200, 200)
                .centerCrop()
                .into(holder.sketchIV);
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
