package com.yinghe.whiteboardlib.adapter;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.yinghe.whiteboardlib.R;
import com.yinghe.whiteboardlib.Utils.ScreenUtils;
import com.yinghe.whiteboardlib.bean.SketchData;

import java.util.List;

/**
 * 图片Adapter
 */
public class SketchDataGridAdapter extends BaseAdapter {


    String TAG = "tangentLu";

    public interface OnActionCallback {
        void onDeleteCallback(int position);

        void onAddCallback();

        void onSelectCallback(SketchData sketchData);
    }

    float ratio;
    int itemHeight;


    List<SketchData> sketchDataList;

    private Context mContext;
    private LayoutInflater mInflater;
    private OnActionCallback onActionCallback;

    public SketchDataGridAdapter(Context context, List<SketchData> sketchDataList, OnActionCallback onActionCallback) {
        this.mContext = context;
        ratio = (float) ScreenUtils.getScreenSize(context).x / ScreenUtils.getScreenSize(context).y;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.onActionCallback = onActionCallback;
        this.sketchDataList = sketchDataList;
    }

    @Override
    public int getCount() {
        return sketchDataList.size() + 1;
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
        final ViewHolder holder;
        final View view;
        if (convertView == null) {
            view = mInflater.inflate(R.layout.grid_item_sketch_data, null);
        } else {
            view = convertView;
        }
        holder = new ViewHolder();
        bindView(view, holder, position);
        showData(holder, position);
        return view;
    }

    private void showData(final ViewHolder holder, int position) {
        if (getCount() > 1 && position == getCount() - 1) {
            showAdd(holder, true);
        } else {
            showAdd(holder, false);
            if (getCount() > 2) {
                holder.deleteIV.setVisibility(View.VISIBLE);
            } else {
                holder.deleteIV.setVisibility(View.GONE);
            }
            if (getItem(position) != null) {
                Drawable drawable = new BitmapDrawable(mContext.getResources(), getItem(position).thumbnailBM);
                holder.sketchIV.setImageDrawable(drawable);
            }
            holder.numberTV.setText(position + 1 + "");
        }
        ViewGroup.LayoutParams lp = holder.rootView.getLayoutParams();
        //为保持白板缩略图高宽比例，快被GridView玩死了。。。
        if (holder.rootView.getMeasuredWidth() == 0) {
            lp.height = itemHeight;
        } else {
            itemHeight = lp.height = (int) (holder.rootView.getMeasuredWidth() / ratio);
        }
    }

    private void bindView(View view, final ViewHolder holder, final int position) {
        holder.rootView = view.findViewById(R.id.grid_sketch_root_view);
        holder.sketchLay = view.findViewById(R.id.grid_sketch_lay);
        holder.sketchLay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onActionCallback.onSelectCallback(getItem(position));
            }
        });
        holder.sketchIV = (ImageView) view.findViewById(R.id.grid_sketch);
        holder.deleteIV = (ImageView) view.findViewById(R.id.grid_delete);
        holder.deleteIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getCount() > 1 && onActionCallback != null)
                    onActionCallback.onDeleteCallback(position);
            }
        });
        holder.numberTV = (TextView) view.findViewById(R.id.grid_number);
        holder.addIV = (ImageView) view.findViewById(R.id.grid_add);
        holder.addIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onActionCallback != null && getCount() < 11)
                    onActionCallback.onAddCallback();
                else
                    Toast.makeText(mContext, R.string.sketch_count_alert, Toast.LENGTH_SHORT).show();
            }
        });
    }

    class ViewHolder {
        View sketchLay;
        View rootView;
        ImageView sketchIV;
        ImageView deleteIV;
        TextView numberTV;
        ImageView addIV;
    }

    void showAdd(ViewHolder holder, boolean b) {
        holder.sketchLay.setVisibility(!b ? View.VISIBLE : View.GONE);
        holder.addIV.setVisibility(b ? View.VISIBLE : View.GONE);
    }
}
