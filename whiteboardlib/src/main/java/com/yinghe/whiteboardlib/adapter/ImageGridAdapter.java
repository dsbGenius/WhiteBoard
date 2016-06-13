package com.yinghe.whiteboardlib.adapter;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.yinghe.whiteboardlib.MultiImageSelectorFragment;
import com.yinghe.whiteboardlib.R;
import com.yinghe.whiteboardlib.bean.Image;
import com.yinghe.whiteboardlib.fragment.WhiteBoardFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 图片Adapter
 */
public class ImageGridAdapter extends BaseAdapter {

    private static final int TYPE_CAMERA = 0;
    private static final int TYPE_NORMAL = 1;
    private final int mRequestType;

    private Context mContext;

    private LayoutInflater mInflater;
    private boolean showCamera = true;
    private boolean showSelectIndicator = true;

    private List<Image> mImages = new ArrayList<>();
    private List<Image> mSelectedImages = new ArrayList<>();

    final int mGridWidth;

    public ImageGridAdapter(Context context, boolean showCamera, int column, int requestType) {
        mContext = context;
        mRequestType = requestType;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.showCamera = showCamera;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int width = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point size = new Point();
            wm.getDefaultDisplay().getSize(size);
            width = size.x;
        } else {
            width = wm.getDefaultDisplay().getWidth();
        }
        mGridWidth = width / column;
    }

    /**
     * 显示选择指示器
     *
     * @param b
     */
    public void showSelectIndicator(boolean b) {
        showSelectIndicator = b;
    }

    public void setShowCamera(boolean b) {
        if (showCamera == b) return;

        showCamera = b;
        notifyDataSetChanged();
    }

    public boolean isShowCamera() {
        return showCamera;
    }

    /**
     * 选择某个图片，改变选择状态
     *
     * @param image
     */
    public void select(Image image) {
        if (mSelectedImages.contains(image)) {
            mSelectedImages.remove(image);
        } else {
            mSelectedImages.add(image);
        }
        notifyDataSetChanged();
    }

    /**
     * 通过图片路径设置默认选择
     *
     * @param resultList
     */
    public void setDefaultSelected(ArrayList<String> resultList) {
        for (String path : resultList) {
            Image image = getImageByPath(path);
            if (image != null) {
                mSelectedImages.add(image);
            }
        }
        if (mSelectedImages.size() > 0) {
            notifyDataSetChanged();
        }
    }

    private Image getImageByPath(String path) {
        if (mImages != null && mImages.size() > 0) {
            for (Image image : mImages) {
                if (image.path.equalsIgnoreCase(path)) {
                    return image;
                }
            }
        }
        return null;
    }

    /**
     * 设置数据集
     *
     * @param images
     */
    public void setData(List<Image> images) {
        mSelectedImages.clear();

        if (images != null && images.size() > 0) {
            mImages = images;
        } else {
            mImages.clear();
        }
        notifyDataSetChanged();
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (showCamera) {
            return position == 0 ? TYPE_CAMERA : TYPE_NORMAL;
        }
        return TYPE_NORMAL;
    }

    private int getAllPicIndex() {
        int showCameraIndex = 0;
        if (mRequestType == WhiteBoardFragment.REQUEST_IMAGE) {
            showCameraIndex = 0;
        } else if (mRequestType == WhiteBoardFragment.REQUEST_BACKGROUND) {
            showCameraIndex = 1;
        }
        return showCameraIndex;
    }

    @Override
    public int getCount() {
        return showCamera ? mImages.size() + 1 : mImages.size();
    }

    @Override
    public Image getItem(int i) {
        if (showCamera) {
            if (i ==0) {
                return null;
            }
            return mImages.get(i - 1);
        } else {
            return mImages.get(i);
        }
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        if (isShowCamera()) {
            if (i == 0) {
                view = mInflater.inflate(R.layout.list_item_camera, viewGroup, false);
                return view;
            }
        }

        ViewHolder holder;
        if (view == null) {
            view = mInflater.inflate(R.layout.list_item_image, viewGroup, false);
            holder = new ViewHolder(view);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        if (holder != null) {
            holder.bindData(getItem(i));
        }

        return view;
    }

    class ViewHolder {
        ImageView image;
        ImageView indicator;
        View mask;

        ViewHolder(View view) {
            image = (ImageView) view.findViewById(R.id.image);
            indicator = (ImageView) view.findViewById(R.id.checkmark);
            mask = view.findViewById(R.id.mask);
            view.setTag(this);
        }

        void bindData(final Image data) {
            if (data == null) return;
            // 处理单选和多选状态
            if (showSelectIndicator) {
                indicator.setVisibility(View.VISIBLE);
                if (mSelectedImages.contains(data)) {
                    // 设置选中状态
                    indicator.setImageResource(R.drawable.btn_selected);
                    mask.setVisibility(View.VISIBLE);
                } else {
                    // 未选择
                    indicator.setImageResource(R.drawable.btn_unselected);
                    mask.setVisibility(View.GONE);
                }
            } else {
                indicator.setVisibility(View.GONE);
            }
            if (!data.name.contains("assets")) {
                File imageFile = new File(data.path);
                //这里构建assets文件
                if (imageFile.exists()) {
                    // 显示图片
                    Picasso.with(mContext)
                            .load(imageFile)
                            .placeholder(R.drawable.default_error)
                            .tag(MultiImageSelectorFragment.TAG)
                            .resize(mGridWidth, mGridWidth)
                            .centerCrop()
                            .into(image);
                }
            } else {
                Picasso.with(mContext)
                        .load("file:///android_asset/" + data.path)
                        .placeholder(R.drawable.default_error)
                        .tag(MultiImageSelectorFragment.TAG)
                        .resize(mGridWidth, mGridWidth)
                        .centerCrop()
                        .into(image);
            }
        }
    }

}
