package com.yinghe.whiteboardlib;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import com.yinghe.whiteboardlib.Utils.ScreenUtils;
import com.yinghe.whiteboardlib.fragment.WhiteBoardFragment;

import java.io.File;
import java.util.ArrayList;


public class MultiImageSelectorActivity extends AppCompatActivity
        implements MultiImageSelectorFragment.Callback {

    // Single choice
    public static final int MODE_SINGLE = 0;
    // Multi choice
    public static final int MODE_MULTI = 1;

    /**
     * Max image size，int，{@link #DEFAULT_IMAGE_SIZE} by default
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
     * Result data set，ArrayList&lt;String&gt;
     */
    public static final String EXTRA_RESULT = "select_result";
    /**
     * View bounds;
     */
    public static final String EXTRA_BOUNDS = "activity_bounds";
    /**
     * Original data set
     */
    public static final String EXTRA_DEFAULT_SELECTED_LIST = "default_list";
    // Default image size
    private static final int DEFAULT_IMAGE_SIZE = 9;
    public static final String EXTRA_REQUEST_TYPE = "request_type";

    private ArrayList<String> resultList = new ArrayList<>();
    private Button mSubmitButton;
    private int mDefaultCount = DEFAULT_IMAGE_SIZE;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.dialogActivity);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        int orientation = this.getResources().getConfiguration().orientation;
        setContentView(R.layout.activity_image_selector);
        setActivitySize(orientation);
        getWindow().getDecorView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                finish();
                return true;
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.BLACK);
        }


        //获取各种配置信息
        final Intent intent = getIntent();
        mDefaultCount = intent.getIntExtra(EXTRA_SELECT_COUNT, DEFAULT_IMAGE_SIZE);
        int mRequestType = intent.getIntExtra(EXTRA_REQUEST_TYPE, WhiteBoardFragment.REQUEST_IMAGE);
        final int mode = intent.getIntExtra(EXTRA_SELECT_MODE, MODE_MULTI);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mRequestType == WhiteBoardFragment.REQUEST_BACKGROUND) {
            toolbar.setTitle("选择背景");
        } else if (mRequestType == WhiteBoardFragment.REQUEST_IMAGE) {
            toolbar.setTitle("选择图片");
        }
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        //设置返回操作
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        final boolean isShow = intent.getBooleanExtra(EXTRA_SHOW_CAMERA, true);
        if (mode == MODE_MULTI && intent.hasExtra(EXTRA_DEFAULT_SELECTED_LIST)) {
            resultList = intent.getStringArrayListExtra(EXTRA_DEFAULT_SELECTED_LIST);
        }
        //多选模式下的提交按钮
        mSubmitButton = (Button) findViewById(R.id.commit);
        if (mode == MODE_MULTI) {
            updateDoneText(resultList);
            mSubmitButton.setVisibility(View.VISIBLE);
            mSubmitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (resultList != null && resultList.size() > 0) {
                        // Notify success
                        Intent data = new Intent();
                        data.putStringArrayListExtra(EXTRA_RESULT, resultList);
                        setResult(RESULT_OK, data);
                    } else {
                        setResult(RESULT_CANCELED);
                    }
                    finish();
                }
            });
        } else {
            mSubmitButton.setVisibility(View.GONE);
        }
        //载入fragment
        if (savedInstanceState == null) {
            Bundle bundle = new Bundle();
            bundle.putInt(MultiImageSelectorFragment.EXTRA_SELECT_COUNT, mDefaultCount);
            bundle.putInt(MultiImageSelectorFragment.EXTRA_SELECT_MODE, mode);
            bundle.putBoolean(MultiImageSelectorFragment.EXTRA_SHOW_CAMERA, isShow);
            bundle.putStringArrayList(MultiImageSelectorFragment.EXTRA_DEFAULT_SELECTED_LIST, resultList);
            bundle.putInt(EXTRA_REQUEST_TYPE, mRequestType);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.image_grid, Fragment.instantiate(this, MultiImageSelectorFragment.class.getName(), bundle))
                    .commit();
        }

    }

    /**
     * 设置activity的尺寸以及在屏幕上的位置
     *
     * @param orientation
     */
    private void setActivitySize(int orientation) {
        Intent intent = getIntent();
        int[] bounds = intent.getIntArrayExtra(EXTRA_BOUNDS);
        WindowManager.LayoutParams p = getWindow().getAttributes();  //获取对话框当前的参数值
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {//横屏
            p.x = bounds[0] + bounds[2] / 2;
            p.y = bounds[1];
            p.width = bounds[2] / 2;
            p.height = bounds[3];
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {//竖屏
            p.x = bounds[1];
            p.y = bounds[1] + bounds[3] / 2;
            p.width = bounds[2];
            p.height = bounds[3] / 2;
        }
        getWindow().setGravity(Gravity.LEFT | Gravity.TOP);
        getWindow().setAttributes(p);
    }

    /**
     * 屏幕旋转时也需要调整activity的大小
     *
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int orientation = newConfig.orientation;
//        setActivitySize(orientation);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Update done button by select image data
     *
     * @param resultList selected image data
     */
    private void updateDoneText(ArrayList<String> resultList) {
        int size = 0;
        if (resultList == null || resultList.size() <= 0) {
            mSubmitButton.setText(R.string.action_done);
            mSubmitButton.setEnabled(false);
        } else {
            size = resultList.size();
            mSubmitButton.setEnabled(true);
        }
        mSubmitButton.setText(getString(R.string.action_button_string,
                getString(R.string.action_done), size, mDefaultCount));
    }

    @Override
    public void onSingleImageSelected(String path) {
        Intent data = new Intent();
        resultList.add(path);
        data.putStringArrayListExtra(EXTRA_RESULT, resultList);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onImageSelected(String path) {
        if (!resultList.contains(path)) {
            resultList.add(path);
        }
        updateDoneText(resultList);
    }

    @Override
    public void onImageUnselected(String path) {
        if (resultList.contains(path)) {
            resultList.remove(path);
        }
        updateDoneText(resultList);
    }

    @Override
    public void onCameraShot(File imageFile) {
        if (imageFile != null) {
            // notify system the image has change
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(imageFile)));

            Intent data = new Intent();
            resultList.add(imageFile.getAbsolutePath());
            data.putStringArrayListExtra(EXTRA_RESULT, resultList);
            setResult(RESULT_OK, data);
            finish();
        }
    }
}
