package com.yinghe.whiteboardlib.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.yinghe.whiteboardlib.MultiImageSelectorFragment;
import com.yinghe.whiteboardlib.R;
import com.yinghe.whiteboardlib.bean.Folder;
import com.yinghe.whiteboardlib.fragment.WhiteBoardFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * 文件夹Adapter
 * Created by Nereo on 2015/4/7.
 * Updated by nereo on 2016/1/19.
 */
public class FolderAdapter extends BaseAdapter {

    private Context mContext;
    private LayoutInflater mInflater;

    private List<Folder> mFolders = new ArrayList<>();

    int mImageSize;

    int lastSelected = 0;
    private int mRequestType;

    public FolderAdapter(Context context,int requstType) {
        mContext = context;
        mRequestType = requstType;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mImageSize = mContext.getResources().getDimensionPixelOffset(R.dimen.folder_cover_size);
    }

    /**
     * 设置数据集
     *
     * @param folders
     */
    public void setData(List<Folder> folders) {
        if (folders != null && folders.size() > 0) {
            mFolders = folders;
        } else {
            mFolders.clear();
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if (mRequestType== WhiteBoardFragment.REQUEST_IMAGE) {
            return mFolders.size() + 1;
        } else if (mRequestType == WhiteBoardFragment.REQUEST_BACKGROUND) {
            return mFolders.size();
        }
        return mFolders.size() + 1;
    }

    @Override
    public Folder getItem(int i) {
        if (mRequestType== WhiteBoardFragment.REQUEST_IMAGE) {
            if (i == 0) return null;
            return mFolders.get(i - 1);
        } else if (mRequestType == WhiteBoardFragment.REQUEST_BACKGROUND) {
            if(i==1) return null;
            return mFolders.get(i);
        }
        if (i == 0) return null;
        return mFolders.get(i - 1);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if (view == null) {
            view = mInflater.inflate(R.layout.list_item_folder, viewGroup, false);
            holder = new ViewHolder(view);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        if (holder != null) {
            int showAllIndex = 0;
            if (mRequestType== WhiteBoardFragment.REQUEST_IMAGE) {
                showAllIndex = 0;
            } else if (mRequestType == WhiteBoardFragment.REQUEST_BACKGROUND) {
                showAllIndex = 1;
            }
            if (i == showAllIndex) {//这里要改
                holder.name.setText(R.string.folder_all);
                holder.path.setText("/sdcard");
                holder.size.setText(String.format("%d%s",
                        getTotalImageSize(), mContext.getResources().getString(R.string.photo_unit)));
                if (mFolders.size() > 0) {
                    Folder f = mFolders.get(1);
                    File coverFile = new File(f.cover.path);
                    if (f != null) {
                        Picasso.with(mContext)
                                .load(coverFile)
                                .error(R.drawable.default_error)
                                .resizeDimen(R.dimen.folder_cover_size, R.dimen.folder_cover_size)
                                .centerCrop()
                                .into(holder.cover);
                    } else {
                        holder.cover.setImageResource(R.drawable.default_error);
                    }
                }
            } else {
                holder.bindData(getItem(i));
            }
            if (lastSelected == i) {
                holder.indicator.setVisibility(View.VISIBLE);
            } else {
                holder.indicator.setVisibility(View.INVISIBLE);
            }
        }
        return view;
    }

    private int getTotalImageSize() {
        int result = 0;
        if (mFolders != null && mFolders.size() > 0) {
            for (Folder f : mFolders) {
                result += f.images.size();
            }
        }
        return result;
    }

    public void setSelectIndex(int i) {
        if (lastSelected == i) return;

        lastSelected = i;
        notifyDataSetChanged();
    }

    public int getSelectIndex() {
        return lastSelected;
    }

    class ViewHolder {
        ImageView cover;
        TextView name;
        TextView path;
        TextView size;
        ImageView indicator;

        ViewHolder(View view) {
            cover = (ImageView) view.findViewById(R.id.cover);
            name = (TextView) view.findViewById(R.id.name);
            path = (TextView) view.findViewById(R.id.path);
            size = (TextView) view.findViewById(R.id.size);
            indicator = (ImageView) view.findViewById(R.id.indicator);
            view.setTag(this);
        }

        void bindData(Folder data) {
            if (data == null) {
                return;
            }
            name.setText(data.name);
            path.setText(data.path);
            if (data.images != null) {
                size.setText(String.format("%d%s", data.images.size(), mContext.getResources().getString(R.string.photo_unit)));
            } else {
                size.setText("*" + mContext.getResources().getString(R.string.photo_unit));
            }
            if (data.cover != null) {
                // 显示图片
                if (!data.path.contains("asset")) {
                    File file = new File(data.cover.path);
                    if (file != null) {
                        Picasso.with(mContext)
                                .load(file)
                                .placeholder(R.drawable.default_error)
                                .resizeDimen(R.dimen.folder_cover_size, R.dimen.folder_cover_size)
                                .centerCrop()
                                .into(cover);
                    }
                } else {
                    Picasso.with(mContext)
                            .load("file:///android_asset/" + data.cover.path)
                            .placeholder(R.drawable.default_error)
                            .tag(MultiImageSelectorFragment.TAG)
                            .resizeDimen(R.dimen.folder_cover_size, R.dimen.folder_cover_size)
                            .centerCrop()
                            .into(cover);
//                    Bitmap bitmap = null;
//                    bitmap = BitmapUtils.getBitmapFromAssets(mContext, data.cover.path);
//                    if (bitmap != null) {
//                        cover.setImageBitmap(bitmap);
//                    }else {
//                        cover.setImageResource(R.drawable.default_error);
//                    }
                }

            } else {
                cover.setImageResource(R.drawable.default_error);
            }
        }
    }

}
