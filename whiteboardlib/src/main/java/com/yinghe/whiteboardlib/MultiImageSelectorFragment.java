package com.yinghe.whiteboardlib;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.ListPopupWindow;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.yinghe.whiteboardlib.Utils.DensityUtil;
import com.yinghe.whiteboardlib.Utils.FileUtils;
import com.yinghe.whiteboardlib.adapter.FolderAdapter;
import com.yinghe.whiteboardlib.adapter.ImageGridAdapter;
import com.yinghe.whiteboardlib.bean.Folder;
import com.yinghe.whiteboardlib.bean.Image;
import com.yinghe.whiteboardlib.fragment.WhiteBoardFragment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * 图片选择器
 * 包括
 * 图片gridView
 * 图片目录popWindow
 */
public class MultiImageSelectorFragment extends Fragment {

    public static final String TAG = "MultiImageSelectorFragment";

    private static final int REQUEST_STORAGE_WRITE_ACCESS_PERMISSION = 110;
    public static final int REQUEST_CAMERA = 100;

    private static final String KEY_TEMP_FILE = "key_temp_file";

    // Single choice
    public static final int MODE_SINGLE = 0;
    // Multi choice
    public static final int MODE_MULTI = 1;

    /**
     * Max image size，int，
     */
    public static final String EXTRA_SELECT_COUNT = "max_select_count";
    /**
     * Select mode，{@link #MODE_MULTI} by default
     */
    public static final String EXTRA_SELECT_MODE = "select_count_mode";
    /**
     * Whether show camera，true by default
     */
    public static final String EXTRA_SHOW_CAMERA = "show_camera";
    /**
     * Original data set
     */
    public static final String EXTRA_DEFAULT_SELECTED_LIST = "default_list";

    // loaders
    private static final int LOADER_ALL = 0;
    private static final int LOADER_CATEGORY = 1;
    private static final int REQUEST_CROP = 101;

    // image result data set
    private ArrayList<String> resultList = new ArrayList<>();
    // folder result data set
    private ArrayList<Folder> mResultFolder = new ArrayList<>();

    private GridView mImageGridView;
    private Callback mCallback;

    private ImageGridAdapter mImageAdapter;
    private FolderAdapter mFolderAdapter;

    private ListPopupWindow mFolderPopupWindow;

    private TextView mCategoryText;
    private View mPopupAnchorView;

    private boolean hasFolderGened = false;//在生成图片,文件夹等数据时判断是否已经全部加载完成

    private File mTmpFile;
    private int mRequestType;
    private boolean mIsFristLoad;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mCallback = (Callback) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException("The Activity must implement MultiImageSelectorFragment.Callback interface...");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_multi_image, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final int mode = selectMode();
        getRequstType();
        if (mode == MODE_MULTI) {//多选模式
            ArrayList<String> tmp = getArguments().getStringArrayList(EXTRA_DEFAULT_SELECTED_LIST);
            if (tmp != null && tmp.size() > 0) {
                resultList = tmp;
            }
        }
        mImageAdapter = new ImageGridAdapter(getActivity(), showCamera(), 3,mRequestType);
        mImageAdapter.showSelectIndicator(mode == MODE_MULTI);

        mPopupAnchorView = view.findViewById(R.id.footer);

        mCategoryText = (TextView) view.findViewById(R.id.category_btn);
        if (mRequestType== WhiteBoardFragment.REQUEST_IMAGE) {
            mCategoryText.setText(R.string.folder_all);
        } else if (mRequestType == WhiteBoardFragment.REQUEST_BACKGROUND) {
            mCategoryText.setText(R.string.folder_first);
        }

        /**
         * 左下角文字的点击事件
         */
        mCategoryText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mFolderPopupWindow == null) {//初次点击时创建弹出框以及数据初始化
                    createPopupFolderList();
                }
                if (mFolderPopupWindow.isShowing()) {
                    mFolderPopupWindow.dismiss();
                } else {
                    mFolderPopupWindow.show();
                    int index = mFolderAdapter.getSelectIndex();
                    index = index == 0 ? index : index - 1;
                    mFolderPopupWindow.getListView().setSelection(index);
                }
            }
        });

        mImageGridView = (GridView) view.findViewById(R.id.grid);
        mImageGridView.setAdapter(mImageAdapter);
        mImageGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mImageAdapter.isShowCamera()) {
                    if (i == 0) {
                        showCameraAction();
                    } else {
                        Image image = (Image) adapterView.getAdapter().getItem(i);
                        selectImageFromGrid(image, mode);
                    }
                } else {
                    Image image = (Image) adapterView.getAdapter().getItem(i);
                    selectImageFromGrid(image, mode);
                }
            }
        });
        mImageGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_FLING) {
                    Picasso.with(view.getContext()).pauseTag(TAG);
                } else {
                    Picasso.with(view.getContext()).resumeTag(TAG);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });

        mFolderAdapter = new FolderAdapter(getActivity(),mRequestType);
    }

    private void getRequstType() {
        mRequestType = getArguments().getInt(MultiImageSelectorActivity.EXTRA_REQUEST_TYPE);
    }

    /**
     * Create popup ListView
     */
    private void createPopupFolderList() {
//        Point point = ScreenUtils.getScreenSize(getActivity());
////        int width = point.x;
//        int width =getActivity().getWindow().getDecorView().getWidth()- DensityUtil.dip2px(getActivity(),60);
//        int height = (int) (point.y * (4.5f/8.0f));
        int orientation = this.getResources().getConfiguration().orientation;
        mFolderPopupWindow = new ListPopupWindow(getActivity());
        mFolderPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        mFolderPopupWindow.setAdapter(mFolderAdapter);
//        mFolderPopupWindow.setContentWidth(width);
//        mFolderPopupWindow.setWidth(width);
//        mFolderPopupWindow.setHeight(height);
        setPopupWindowSize(orientation);
        mFolderPopupWindow.setAnchorView(mPopupAnchorView);
        mFolderPopupWindow.setModal(true);
        mFolderPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                mFolderAdapter.setSelectIndex(i);

                final int index = i;
                final AdapterView TempAdapterView = adapterView;

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mFolderPopupWindow.dismiss();

                        if (index == getAllPicIndex()) {
                            getActivity().getSupportLoaderManager().restartLoader(LOADER_ALL, null, mLoaderCallback);
                            mCategoryText.setText(R.string.folder_all);
                            if (showCamera()) {
                                mImageAdapter.setShowCamera(true);
                            } else {
                                mImageAdapter.setShowCamera(false);
                            }
                        } else {
                            Folder folder = (Folder) TempAdapterView.getAdapter().getItem(index);
                            if (null != folder) {
                                mImageAdapter.setData(folder.images);
                                mCategoryText.setText(folder.name);
                                if (resultList != null && resultList.size() > 0) {
                                    mImageAdapter.setDefaultSelected(resultList);
                                }
                            }
                            mImageAdapter.setShowCamera(false);
                        }

                        mImageGridView.smoothScrollToPosition(0);
                    }
                }, 100);

            }
        });
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
    private void setPopupWindowSize(int orientation) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {//横屏
            int screenWidth = Math.max(WhiteBoardFragment.sketchViewHeight, WhiteBoardFragment.sketchViewWidth);
            int screenHight = Math.min(WhiteBoardFragment.sketchViewHeight, WhiteBoardFragment.sketchViewWidth);
            mFolderPopupWindow.setWidth(screenWidth / 2 - DensityUtil.dip2px(getActivity(), 60));
            mFolderPopupWindow.setHeight(screenHight / 2);
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {//竖屏
            int screenWidth = Math.min(WhiteBoardFragment.sketchViewHeight, WhiteBoardFragment.sketchViewWidth);
            int screenHight = Math.max(WhiteBoardFragment.sketchViewHeight, WhiteBoardFragment.sketchViewWidth);
            mFolderPopupWindow.setWidth(screenWidth);
            mFolderPopupWindow.setContentWidth(screenWidth);
            mFolderPopupWindow.setHeight(screenHight / 3);
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_TEMP_FILE, mTmpFile);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            mTmpFile = (File) savedInstanceState.getSerializable(KEY_TEMP_FILE);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // load image data
        getRequstType();
        mIsFristLoad = true;
        getActivity().getSupportLoaderManager().initLoader(LOADER_ALL, null, mLoaderCallback);
    }

    /**
     * 拍照成功后的回调
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAMERA) {
            if (resultCode == Activity.RESULT_OK) {
                if (mTmpFile != null) {
                    if(mRequestType==WhiteBoardFragment.REQUEST_IMAGE) {//如果是拍照作为画板素材的话
                        cropPhoto(Uri.fromFile(mTmpFile));// 裁剪图片
                    }else if(mRequestType==WhiteBoardFragment.REQUEST_BACKGROUND) {//拍照作为画板背景
                        if (mCallback != null) {
                            mCallback.onCameraShot(mTmpFile);
                        }
                    }
                }
            } else {
                // delete tmp file
                Toast.makeText(getActivity(),"拍照失败,请重新拍照",Toast.LENGTH_SHORT).show();
                while (mTmpFile != null && mTmpFile.exists()) {
                    boolean success = mTmpFile.delete();
                    if (success) {
                        mTmpFile = null;
                    }
                }
            }
        } else if (requestCode == REQUEST_CROP) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Bundle extras = data.getExtras();
                    Bitmap bitmap = extras.getParcelable("data");
                    dealWithBitmap(bitmap);//裁剪图片后的操作

                }

            }
        }
    }

    /**
     * 将裁剪后的图片保存到本地以及回调到画板中
     * @param bitmap
     */
    private void dealWithBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            String sdStatus = Environment.getExternalStorageState();
            if (!sdStatus.equals(Environment.MEDIA_MOUNTED)) { // 检测sd是否可用
                return;
            }
            FileOutputStream b = null;
            String path = Environment.getExternalStorageDirectory().getPath()+"/whiteBoardLib/";
            File file = new File(path);
            file.mkdirs();// 创建文件夹
            String fileName = path + UUID.randomUUID() + ".jpg";// 图片名字
            try {
                b = new FileOutputStream(fileName);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, b);// 把数据写入文件

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    // 关闭流
                    b.flush();
                    b.close();
                    if (mCallback != null) {
                        File imgFile = new File(fileName);
                        mCallback.onCameraShot(imgFile);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }
    }

    /**
     * 调用系统的裁剪
     *
     * @param uri
     */
    public void cropPhoto(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", 150);
        intent.putExtra("outputY", 150);
        intent.putExtra("return-data", true);
        startActivityForResult(intent, REQUEST_CROP);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (mFolderPopupWindow != null) {
            if (mFolderPopupWindow.isShowing()) {
                mFolderPopupWindow.dismiss();
            }
            setPopupWindowSize(newConfig.orientation);
        }
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Open camera
     */
    private void showCameraAction() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    getString(R.string.permission_rationale_write_storage),
                    REQUEST_STORAGE_WRITE_ACCESS_PERMISSION);
        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                try {
                    mTmpFile = FileUtils.createTmpFile(getActivity());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (mTmpFile != null && mTmpFile.exists()) {
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTmpFile));
                    startActivityForResult(intent, REQUEST_CAMERA);
                } else {
                    Toast.makeText(getActivity(), R.string.error_image_not_exist, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getActivity(), R.string.msg_no_camera, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 请求 写入sd卡权限
     * @param permission
     * @param rationale
     * @param requestCode
     */
    private void requestPermission(final String permission, String rationale, final int requestCode) {
        if (shouldShowRequestPermissionRationale(permission)) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.permission_dialog_title)
                    .setMessage(rationale)
                    .setPositiveButton(R.string.permission_dialog_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[]{permission}, requestCode);
                        }
                    })
                    .setNegativeButton(R.string.permission_dialog_cancel, null)
                    .create().show();
        } else {
            requestPermissions(new String[]{permission}, requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_STORAGE_WRITE_ACCESS_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showCameraAction();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * notify callback
     *
     * @param image image data
     */
    private void selectImageFromGrid(Image image, int mode) {
        if (image != null) {
            if (mode == MODE_MULTI) {
                if (resultList.contains(image.path)) {
                    resultList.remove(image.path);
                    if (mCallback != null) {
                        mCallback.onImageUnselected(image.path);
                    }
                } else {
                    if (selectImageCount() == resultList.size()) {
                        Toast.makeText(getActivity(), R.string.msg_amount_limit, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    resultList.add(image.path);
                    if (mCallback != null) {
                        mCallback.onImageSelected(image.path);
                    }
                }
                mImageAdapter.select(image);
            } else if (mode == MODE_SINGLE) {
                if (mCallback != null) {
                    mCallback.onSingleImageSelected(image.path);
                }
            }
        }
    }

    /**
     * 查询图片数据库的某个文件夹所有图片数据
     * 以及所有包含图片的文件夹数据
     */
    private LoaderManager.LoaderCallbacks<Cursor> mLoaderCallback = new LoaderManager.LoaderCallbacks<Cursor>() {
        //要查询的数据
        private final String[] IMAGE_PROJECTION = {
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media._ID};

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            CursorLoader cursorLoader = null;
            if (id == LOADER_ALL) {
                cursorLoader = new CursorLoader(getActivity(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_PROJECTION,
                        IMAGE_PROJECTION[4] + ">0 AND " + IMAGE_PROJECTION[3] + "=? OR " + IMAGE_PROJECTION[3] + "=? ",
                        new String[]{"image/jpeg", "image/png"}, IMAGE_PROJECTION[2] + " DESC");
            } else if (id == LOADER_CATEGORY) {
                cursorLoader = new CursorLoader(getActivity(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_PROJECTION,
                        IMAGE_PROJECTION[4] + ">0 AND " + IMAGE_PROJECTION[0] + " like '%" + args.getString("path") + "%'",
                        null, IMAGE_PROJECTION[2] + " DESC");
            }
            return cursorLoader;
        }

        private boolean fileExist(String path) {
            if (!TextUtils.isEmpty(path)) {
                return new File(path).exists();
            }
            return false;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data != null) {
                if (data.getCount() > 0) {
                    List<Image> images = new ArrayList<>();//所有的图片
                    data.moveToFirst();
                    do {
                        String path = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[0]));
                        String name = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[1]));
                        long dateTime = data.getLong(data.getColumnIndexOrThrow(IMAGE_PROJECTION[2]));
                        Image image = null;
                        if (fileExist(path) && !TextUtils.isEmpty(name)) {
                            image = new Image(path, name, dateTime);
                            images.add(image);
                        }
                        if (!hasFolderGened && fileExist(path)) {
                            // get all folder data
                            File folderFile = new File(path).getParentFile();
                            if (folderFile != null && folderFile.exists()) {
                                String fp = folderFile.getAbsolutePath();
                                Folder f = getFolderByPath(fp);
                                if (f == null) {
                                    Folder folder = new Folder();
                                    folder.name = folderFile.getName();
                                    folder.path = fp;
                                    folder.cover = image;
                                    List<Image> imageList = new ArrayList<>();
                                    imageList.add(image);
                                    folder.images = imageList;
                                    mResultFolder.add(folder);
                                } else {
                                    f.images.add(image);
                                }
                            }
                        }

                    } while (data.moveToNext());


                    if (!hasFolderGened) {
                        //在这里添加assets
                        try {
                            String assestPath = "img";
                            String folderName = "素材";
                            if(mRequestType==WhiteBoardFragment.REQUEST_IMAGE) {
                                assestPath = "img";
                                folderName = "素材";
                            }else if (mRequestType==WhiteBoardFragment.REQUEST_BACKGROUND){
                                assestPath = "background";
                                folderName = "背景素材";
                            }
                            String[] files =getActivity().getAssets().list(assestPath);
                            List<Image> AssetImages = new ArrayList<>();
                            Folder folder = new Folder();
                            for (int i = 0; i < files.length; i++) {
                                Image image = new Image();
                                image.path = assestPath + "/" + files[i];
                                image.name = "assets/"+files[i];
                                AssetImages.add(image);
                            }
                            folder.cover = AssetImages.get(0);
                            folder.images = AssetImages;
                            folder.name =folderName ;
                            folder.path = "assets";
                            mResultFolder.add(0,folder);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }finally {
                            mFolderAdapter.setData(mResultFolder);
                            hasFolderGened = true;
                        }

                    }
                    if(mRequestType==WhiteBoardFragment.REQUEST_IMAGE) {
                        mImageAdapter.setData(images);
                        mImageAdapter.setShowCamera(true);
                    } else if (mRequestType == WhiteBoardFragment.REQUEST_BACKGROUND) {
                        if(mIsFristLoad) {
                            mImageAdapter.setShowCamera(true);
                            mImageAdapter.setData(mResultFolder.get(0).images);
                            mIsFristLoad = false;
                        }else {
                            mImageAdapter.setShowCamera(false);
                            mImageAdapter.setData(images);
                        }
                    }

                    if (resultList != null && resultList.size() > 0) {
                        mImageAdapter.setDefaultSelected(resultList);
                    }
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }
    };

    private Folder getFolderByPath(String path) {
        if (mResultFolder != null) {
            for (Folder folder : mResultFolder) {
                if (TextUtils.equals(folder.path, path)) {
                    return folder;
                }
            }
        }
        return null;
    }

    /**
     *
     * 是否显示拍照
     * @return
     */
    private boolean showCamera() {
        boolean isShowCamera =getArguments() == null || getArguments().getBoolean(EXTRA_SHOW_CAMERA, true);
        if (mRequestType== WhiteBoardFragment.REQUEST_IMAGE) {
            return isShowCamera;
        } else if (mRequestType == WhiteBoardFragment.REQUEST_BACKGROUND) {
            return false;
        }
        return isShowCamera;
    }

    /**
     * 多选模式还是单选模式
     * @return
     */
    private int selectMode() {
        return getArguments() == null ? MODE_MULTI : getArguments().getInt(EXTRA_SELECT_MODE);
    }

    /**
     * 多选模式下最多的选择图片张数
     * @return
     */
    private int selectImageCount() {
        return getArguments() == null ? 9 : getArguments().getInt(EXTRA_SELECT_COUNT);
    }

    /**
     * Callback for host activity
     */
    public interface Callback {
        void onSingleImageSelected(String path);

        void onImageSelected(String path);

        void onImageUnselected(String path);

        void onCameraShot(File imageFile);
    }
}
