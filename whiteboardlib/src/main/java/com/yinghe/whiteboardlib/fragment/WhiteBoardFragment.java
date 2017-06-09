package com.yinghe.whiteboardlib.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yinghe.whiteboardlib.MultiImageSelector;
import com.yinghe.whiteboardlib.R;
import com.yinghe.whiteboardlib.Utils.BitmapUtils;
import com.yinghe.whiteboardlib.Utils.FileUtils;
import com.yinghe.whiteboardlib.Utils.ScreenUtils;
import com.yinghe.whiteboardlib.Utils.TimeUtils;
import com.yinghe.whiteboardlib.adapter.SketchDataGridAdapter;
import com.yinghe.whiteboardlib.bean.SketchData;
import com.yinghe.whiteboardlib.bean.StrokeRecord;
import com.yinghe.whiteboardlib.view.SketchView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static android.app.ProgressDialog.show;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_CIRCLE;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_DRAW;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_ERASER;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_LINE;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_RECTANGLE;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_TEXT;

public class WhiteBoardFragment extends Fragment implements SketchView.OnDrawChangedListener, View.OnClickListener {

    public static final int REQUEST_IMAGE = 2;
    public static final int REQUEST_BACKGROUND = 3;
    //文件保存目录
    public static final String TEMP_FILE_PATH = Environment.getExternalStorageDirectory().toString() + "/mizhu/temp/";
    public static final String FILE_PATH = Environment.getExternalStorageDirectory().toString() + "/mizhu/sketchPhoto/";

    public static final String TEMP_FILE_NAME = "temp_";
    public static final String TEMP_FILE = "temp";

    static final int COLOR_BLACK = Color.parseColor("#ff000000");
    static final int COLOR_RED = Color.parseColor("#ffff4444");
    static final int COLOR_GREEN = Color.parseColor("#ff99cc00");
    static final int COLOR_ORANGE = Color.parseColor("#ffffbb33");
    static final int COLOR_BLUE = Color.parseColor("#ff33b5e5");
    private static final float BTN_ALPHA = 0.4f;
    //
    public static int sketchViewHeight;
    public static int sketchViewWidth;
    public static int sketchViewRight;
    public static int sketchViewBottom;
    public static int decorHeight;
    public static int decorWidth;
    final String TAG = getClass().getSimpleName();
    int keyboardHeight;
    int textOffX;
    int textOffY;

    public SketchView getSketchView() {
        return mSketchView;
    }

    SketchView mSketchView;//画板
    View controlLayout;//控制布局
    //    ImageView btn_add;//添加画板
    ImageView btn_stroke;//画笔
    ImageView btn_eraser;//橡皮擦
    ImageView btn_undo;//撤销
    ImageView btn_redo;//取消撤销
    ImageView btn_photo;//加载图片
    ImageView btn_background;//背景图片
    ImageView btn_drag;//拖拽
    //    ImageView btn_save;//保存
    ImageView btn_empty;//清空
    //        ImageView btn_send;//推送
    Button btn_close;//关闭
    ImageView btn_commit;//提交
    ImageView iv_show_question;
    //    ImageView btn_send_space;//推送按钮间隔
    RadioGroup strokeTypeRG, strokeColorRG;
    Activity activity;//上下文
    int strokeMode;//模式
    int strokeType;//模式
    EditText saveET;
    AlertDialog saveDialog;
    GridView sketchGV;
    SketchDataGridAdapter sketchGVAdapter;
    int pupWindowsDPWidth = 300;//弹窗宽度，单位DP
    int strokePupWindowsDPHeight = 275;//画笔弹窗高度，单位DP
    int eraserPupWindowsDPHeight = 90;//橡皮擦弹窗高度，单位DP
    WhiteBoardCallback mWhiteBoardCallback;
    PushBtnCallback pushBtnCallback;
    boolean isTeacher;
    boolean isPush;
    String netBGUrl;
    PopupWindow strokePopupWindow, eraserPopupWindow, textPopupWindow;//画笔、橡皮擦参数设置弹窗实例
    private View popupStrokeLayout, popupEraserLayout, popupTextLayout;//画笔、橡皮擦弹窗布局
    private SeekBar strokeSeekBar, strokeAlphaSeekBar, eraserSeekBar;
    private ImageView strokeImageView, strokeAlphaImage, eraserImageView;//画笔宽度，画笔不透明度，橡皮擦宽度IV
    private EditText strokeET;//绘制文字的内容
    //    //    private SketchData curSketchData;
//    private List<String> sketchPathList = new ArrayList<>();
//    private int dataPosition;
    private int size;
    private AlertDialog dialog;
    private ArrayList<String> mSelectPath;
    private List<SketchData> sketchDataList = new ArrayList<>();
    private TextView tv_save;
    private FrameLayout fl_question_window;

    /**
     * show 默认新建一个学生端功能
     *
     * @author TangentLu
     * create at 16/6/17 上午9:59
     */
    public static WhiteBoardFragment newInstance() {
        return new WhiteBoardFragment();
    }

    /**
     * show 新建一个教师端的画板碎片，有推送按钮
     *
     * @param callback 推送按钮监听器，接受返回的图片文件路径可用于显示文件
     * @author TangentLu
     * create at 16/6/17 上午9:57
     */
    public static WhiteBoardFragment newInstance(WhiteBoardCallback callback) {
        WhiteBoardFragment fragment = new WhiteBoardFragment();
        fragment.mWhiteBoardCallback = callback;
        fragment.isTeacher = true;
        return fragment;
    }

    /**
     * show 新建一个学生端的画板碎片，有关闭和提交按钮
     *
     * @param callback 推送按钮监听器，接受返回的图片文件路径可用于显示文件
     * @author TangentLu
     * create at 16/6/17 上午9:57
     */
    public static WhiteBoardFragment newInstance(PushBtnCallback callback, String imgUrl) {
        WhiteBoardFragment fragment = new WhiteBoardFragment();
        fragment.pushBtnCallback = callback;
        fragment.isPush = true;
        fragment.netBGUrl = imgUrl;
        return fragment;
    }

    public void setCommitDone() {
//        btn_commit.setBackgroundResource(R.color.gray);
//        btn_commit.setText("已提交");
//        btn_commit.setEnabled(false);
    }

    /**
     * @param imgPath 添加的背景图片文件路径
     * @author TangentLu
     * create at 16/6/21 下午3:39
     * show 设置当前白板的背景图片
     */
    public void setCurBackgroundByPath(String imgPath) {
        showSketchView(true);
        mSketchView.setBackgroundByPath(imgPath);
    }

    public void setCurBackgroundByBitmap(Bitmap bm) {
        showSketchView(true);
        mSketchView.setBackgroundByBitmap(bm);
    }

    /**
     * show  新增白板并设置白板的背景图片
     *
     * @param imgPath 添加的背景图片文件路径
     * @author TangentLu
     * create at 16/6/21 下午3:39
     */
    public void setNewBackgroundByPath(String imgPath) {
        showSketchView(true);
        SketchData newSketchData = new SketchData();
        sketchDataList.add(newSketchData);
        mSketchView.updateSketchData(newSketchData);
        setCurBackgroundByPath(imgPath);
        mSketchView.setEditMode(SketchView.EDIT_STROKE);
    }

    /**
     * show 新增图片到当前白板
     *
     * @param imgPath 新增的图片路径
     *                create at 16/6/21 下午3:42
     */
    public void addPhotoByPath(String imgPath) {
        showSketchView(true);
        mSketchView.addPhotoByPath(imgPath);
        mSketchView.setEditMode(SketchView.EDIT_PHOTO);//切换图片编辑模式
    }

    /**
     * show 新增图片到当前白板
     *
     * @param bitmap 新增的图片路径
     *               create at 16/6/21 下午3:42
     */
    public void addPhotoByBitmap(Bitmap bitmap) {
        showSketchView(true);
        mSketchView.addPhotoByBitmap(bitmap);
        mSketchView.setEditMode(SketchView.EDIT_PHOTO);//切换图片编辑模式
        resetBtnStatus(btn_drag);
    }

    /**
     * show 新增图片到当前白板
     *
     * @param bitmap 新增的图片路径
     *               create at 16/6/21 下午3:42
     */
    public void addPhotoByBitmap(Bitmap bitmap, int[] position) {
        showSketchView(true);
        mSketchView.addPhotoByBitmap(bitmap, position);
//        mSketchView.setEditMode(SketchView.EDIT_PHOTO);//切换图片编辑模式
    }

    /**
     * show 获取当前白板的BitMap
     * <p>
     * create at 16/6/21 下午3:44
     */
    public Bitmap getResultBitmap() {
        return mSketchView.getResultBitmap();
    }

    /**
     * show 手动保存当前画板到文件，耗时操作
     *
     * @param filePath 保存的文件路径
     * @param imgName  保存的文件名
     * @return 返回保存后的文件路径
     * @author TangentLu
     * create at 16/6/21 下午3:46
     */
    public File saveInOI(String filePath, String imgName) {
        return saveInOI(filePath, imgName, 80);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();//初始化上下文
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_white_board, container, false);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //下面的代码主要是为了解决软键盘弹出后遮挡住文字录入PopWindow的问题
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);//获取rootView的可视区域
                int screenHeight = rootView.getHeight();//获取rootView的高度
                keyboardHeight = screenHeight - (r.bottom - r.top);//用rootView的高度减去rootView的可视区域高度得到软键盘高度
                if (textOffY > (sketchViewHeight - keyboardHeight)) {//如果输入焦点出现在软键盘显示的范围内则进行布局上移操作
                    rootView.setTop(-keyboardHeight);//rootView整体上移软键盘高度
                    //更新PopupWindow的位置
                    int x = textOffX;
                    int y = textOffY - mSketchView.getHeight();
                    textPopupWindow.update(mSketchView, x, y,
                            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
                }
            }
        });
        findView(rootView);//载入所有的按钮实例
        initDrawParams();//初始化绘画参数
        initPopupWindows();//初始化弹框
        initSaveDialog();
        initData();
        initSketchGV();
        return rootView;
    }

    public FrameLayout getQuestionWindow() {
        return fl_question_window;
    }

    private void initData() {
        SketchData newSketchData = new SketchData();
        sketchDataList.add(newSketchData);
        mSketchView.setSketchData(newSketchData);
    }

    private void initSketchGV() {
        sketchGVAdapter = new SketchDataGridAdapter(activity, sketchDataList, new SketchDataGridAdapter.OnActionCallback() {
            @Override
            public void onDeleteCallback(int position) {
                sketchDataList.remove(position);
                sketchGVAdapter.notifyDataSetChanged();
            }

            @Override
            public void onAddCallback() {
                SketchData newSketchData = new SketchData();
                sketchDataList.add(newSketchData);
                mSketchView.updateSketchData(newSketchData);
                mSketchView.setEditMode(SketchView.EDIT_STROKE);//切换笔画编辑模式
                showSketchView(true);
            }

            @Override
            public void onSelectCallback(SketchData sketchData) {
                mSketchView.updateSketchData(sketchData);
//                mSketchView.setEditMode(SketchView.EDIT_PHOTO);//切换图片编辑模式
                showSketchView(true);
            }
        });
        sketchGV.setAdapter(sketchGVAdapter);
    }

    private void showSketchView(boolean b) {
        mSketchView.setVisibility(b ? View.VISIBLE : View.GONE);
        sketchGV.setVisibility(!b ? View.VISIBLE : View.GONE);
    }

    private void initSaveDialog() {
        saveET = new EditText(activity);
        saveET.setHint("新文件名");
        saveET.setGravity(Gravity.CENTER);
        saveET.setSingleLine();
        saveET.setInputType(EditorInfo.TYPE_CLASS_TEXT);
        saveET.setImeOptions(EditorInfo.IME_ACTION_DONE);
        saveET.setSelectAllOnFocus(true);
        saveET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    ScreenUtils.hideInput(saveDialog.getCurrentFocus());
                    saveDialog.dismiss();
                    String input = saveET.getText().toString();
                    saveInUI(input + ".png");
                }
                return true;
            }
        });
        saveDialog = new AlertDialog.Builder(getActivity())
                .setTitle("请输入保存文件名")
                .setMessage("")
                .setView(saveET)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ScreenUtils.hideInput(saveDialog.getCurrentFocus());
                        String input = saveET.getText().toString();
                        saveInUI(input + ".png");
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ScreenUtils.hideInput(saveDialog.getCurrentFocus());
                    }
                })
                .setCancelable(false)
                .create();
    }

    private void initDrawParams() {
        //默认为画笔模式
        strokeMode = STROKE_TYPE_DRAW;

        //画笔宽度缩放基准参数
        Drawable circleDrawable = getResources().getDrawable(R.drawable.circle);
        assert circleDrawable != null;
        size = circleDrawable.getIntrinsicWidth();
    }

    private void initPopupWindows() {
        initStrokePop();
        initEraserPop();
        initTextPop();
    }

    private void initTextPop() {
        textPopupWindow = new PopupWindow(activity);
        textPopupWindow.setContentView(popupTextLayout);
        textPopupWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);//宽度200dp
        textPopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);//高度自适应
        textPopupWindow.setFocusable(true);
        textPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        textPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        textPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (!strokeET.getText().toString().equals("")) {
                    StrokeRecord record = new StrokeRecord(strokeType);
                    record.text = strokeET.getText().toString();
                }
            }
        });
    }

    private void initEraserPop() {
        //橡皮擦弹窗
        eraserPopupWindow = new PopupWindow(activity);
        eraserPopupWindow.setContentView(popupEraserLayout);//设置主体布局
        eraserPopupWindow.setWidth(ScreenUtils.dip2px(getActivity(), pupWindowsDPWidth));//宽度200dp
//        eraserPopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);//高度自适应
        eraserPopupWindow.setHeight(ScreenUtils.dip2px(getActivity(), eraserPupWindowsDPHeight));//高度自适应
        eraserPopupWindow.setFocusable(true);
        eraserPopupWindow.setBackgroundDrawable(new BitmapDrawable());//设置空白背景
        eraserPopupWindow.setAnimationStyle(R.style.mypopwindow_anim_style);//动画
        //橡皮擦宽度拖动条
        eraserSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }


            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }


            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                setSeekBarProgress(progress, STROKE_TYPE_ERASER);
            }
        });
        eraserSeekBar.setProgress(SketchView.DEFAULT_ERASER_SIZE);
    }

    private void initStrokePop() {
        //画笔弹窗
        strokePopupWindow = new PopupWindow(activity);
        strokePopupWindow.setContentView(popupStrokeLayout);//设置主体布局
        strokePopupWindow.setWidth(ScreenUtils.dip2px(getActivity(), pupWindowsDPWidth));//宽度
//        strokePopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);//高度自适应
        strokePopupWindow.setHeight(ScreenUtils.dip2px(getActivity(), strokePupWindowsDPHeight));//高度
        strokePopupWindow.setFocusable(true);
        strokePopupWindow.setBackgroundDrawable(new BitmapDrawable());//设置空白背景
        strokePopupWindow.setAnimationStyle(R.style.mypopwindow_anim_style);//动画
        strokeTypeRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int resId = R.drawable.s_db_ct_ic_pe_che;
                if (checkedId == R.id.stroke_type_rbtn_draw) {
                    strokeType = STROKE_TYPE_DRAW;
                } else if (checkedId == R.id.stroke_type_rbtn_line) {
                    strokeType = STROKE_TYPE_LINE;
                    resId = R.drawable.ct_ic_sl;
                } else if (checkedId == R.id.stroke_type_rbtn_circle) {
                    strokeType = STROKE_TYPE_CIRCLE;
                    resId = R.drawable.ct_ic_ro;
                } else if (checkedId == R.id.stroke_type_rbtn_rectangle) {
                    strokeType = STROKE_TYPE_RECTANGLE;
                    resId = R.drawable.ct_ic_sq;
                } else if (checkedId == R.id.stroke_type_rbtn_text) {//文字输入模式
                    strokeType = STROKE_TYPE_TEXT;
                    resId = R.drawable.ct_ic_wo;
                }
//                btn_stroke.setImageResource(resId);
                mSketchView.setStrokeType(strokeType);
                strokePopupWindow.dismiss();//切换画笔后隐藏
            }
        });
        strokeColorRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int color = COLOR_BLACK;
                if (checkedId == R.id.stroke_color_black) {
                    color = COLOR_BLACK;
                } else if (checkedId == R.id.stroke_color_red) {
                    color = COLOR_RED;
                } else if (checkedId == R.id.stroke_color_green) {
                    color = COLOR_GREEN;
                } else if (checkedId == R.id.stroke_color_orange) {
                    color = COLOR_ORANGE;
                } else if (checkedId == R.id.stroke_color_blue) {
                    color = COLOR_BLUE;
                }
                mSketchView.setStrokeColor(color);
            }
        });
        //画笔宽度拖动条
        strokeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }


            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }


            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                setSeekBarProgress(progress, STROKE_TYPE_DRAW);
            }
        });
        strokeSeekBar.setProgress(SketchView.DEFAULT_STROKE_SIZE);
//        strokeColorRG.check(R.thumbnail.stroke_color_black);

        //画笔不透明度拖动条
        strokeAlphaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }


            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }


            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                int alpha = (progress * 255) / 100;//百分比转换成256级透明度
                mSketchView.setStrokeAlpha(alpha);
                strokeAlphaImage.setAlpha(alpha);
            }
        });
        strokeAlphaSeekBar.setProgress(SketchView.DEFAULT_STROKE_ALPHA);
    }

    private void findView(View view) {

        fl_question_window = (FrameLayout) view.findViewById(R.id.fl_windows);//题目窗口
        sketchGV = (GridView) view.findViewById(R.id.sketch_data_gv);

        //画板整体布局
        mSketchView = (SketchView) view.findViewById(R.id.sketch_view);

        controlLayout = view.findViewById(R.id.controlLayout);

//        btn_add = (ImageView) view.findViewById(R.id.btn_add);
        btn_stroke = (ImageView) view.findViewById(R.id.btn_stroke);
        btn_eraser = (ImageView) view.findViewById(R.id.btn_eraser);
        btn_undo = (ImageView) view.findViewById(R.id.btn_undo);
        btn_redo = (ImageView) view.findViewById(R.id.btn_redo);
        btn_photo = (ImageView) view.findViewById(R.id.btn_photo);
        btn_background = (ImageView) view.findViewById(R.id.btn_background);
        btn_drag = (ImageView) view.findViewById(R.id.btn_drag);
//        btn_save = (ImageView) view.findViewById(R.id.btn_save);
        btn_empty = (ImageView) view.findViewById(R.id.btn_empty);
//        btn_send = (ImageView) view.findViewById(R.id.btn_send);
        tv_save = (TextView) view.findViewById(R.id.tv_save);
//        btn_close = (Button) view.findViewById(R.id.btn_close);
        btn_commit = (ImageView) view.findViewById(R.id.btn_commit);
        iv_show_question = (ImageView) view.findViewById(R.id.iv_see_question);
//        btn_send_space = (ImageView) view.findViewById(R.id.btn_send_space);
//        iv_commit = (ImageView) view.findViewById(R.id.iv_commit);
        tv_save.setOnClickListener(this);
//        iv_commit.setOnClickListener(this);
//        tv_commit.setOnClickListener(this);
        if (isTeacher) {
//            btn_send.setVisibility(View.GONE);
//            btn_send_space.setVisibility(View.VISIBLE);
//            btn_send.setOnClickListener(this);
        }
        if (isPush) {//学生接受白板推送模式
//            btn_close.setVisibility(View.VISIBLE);
            btn_commit.setVisibility(View.VISIBLE);
//            btn_send_space.setVisibility(View.VISIBLE);
//            btn_close.setOnClickListener(this);
            btn_commit.setOnClickListener(this);
//            btn_add.setVisibility(View.GONE);
        }

        //设置点击监听
        btn_commit.setOnClickListener(this);
        mSketchView.setOnDrawChangedListener(this);//设置撤销动作监听器
//        btn_add.setOnClickListener(this);
        btn_stroke.setOnClickListener(this);
        btn_eraser.setOnClickListener(this);
        btn_undo.setOnClickListener(this);
        btn_redo.setOnClickListener(this);
        btn_empty.setOnClickListener(this);
//        btn_save.setOnClickListener(this);
        btn_photo.setOnClickListener(this);
        btn_background.setOnClickListener(this);
        btn_drag.setOnClickListener(this);
        iv_show_question.setOnClickListener(this);
        mSketchView.setTextWindowCallback(new SketchView.TextWindowCallback() {
            @Override
            public void onText(View anchor, StrokeRecord record) {
                textOffX = record.textOffX;
                textOffY = record.textOffY;
                showTextPopupWindow(anchor, record);
            }
        });

        // popupWindow布局
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity
                .LAYOUT_INFLATER_SERVICE);
        //画笔弹窗布局
        popupStrokeLayout = inflater.inflate(R.layout.popup_sketch_stroke, null);
        strokeImageView = (ImageView) popupStrokeLayout.findViewById(R.id.stroke_circle);
        strokeAlphaImage = (ImageView) popupStrokeLayout.findViewById(R.id.stroke_alpha_circle);
        strokeSeekBar = (SeekBar) (popupStrokeLayout.findViewById(R.id.stroke_seekbar));
        strokeAlphaSeekBar = (SeekBar) (popupStrokeLayout.findViewById(R.id.stroke_alpha_seekbar));
        //画笔颜色
        strokeTypeRG = (RadioGroup) popupStrokeLayout.findViewById(R.id.stroke_type_radio_group);
        strokeColorRG = (RadioGroup) popupStrokeLayout.findViewById(R.id.stroke_color_radio_group);

        //橡皮擦弹窗布局
        popupEraserLayout = inflater.inflate(R.layout.popup_sketch_eraser, null);
        eraserImageView = (ImageView) popupEraserLayout.findViewById(R.id.stroke_circle);
        eraserSeekBar = (SeekBar) (popupEraserLayout.findViewById(R.id.stroke_seekbar));
        //文本录入弹窗布局
        popupTextLayout = inflater.inflate(R.layout.popup_sketch_text, null);
        strokeET = (EditText) popupTextLayout.findViewById(R.id.text_pupwindow_et);
        getSketchSize();//计算选择图片弹窗的高宽
    }

    @Override
    public void onResume() {
        super.onResume();
        if (netBGUrl != null) {
            showNetImg();
        }
    }

    private void showNetImg() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(netBGUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    InputStream is = connection.getInputStream();
                    final Bitmap bitmap = BitmapFactory.decodeStream(is);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setCurBackgroundByBitmap(bitmap);
                            netBGUrl = null;
                        }
                    });
                    connection.disconnect();
                } catch (final Exception e) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), "推送图像载入出错：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();


//        Log.e(TAG, "showNetImg: ");
//        Target target = new Target() {
//            @Override
//            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
//                Log.e(TAG, "onBitmapLoaded: ");
//                mSketchView.setBackgroundByBitmap(bitmap);
//            }
//
//            @Override
//            public void onBitmapFailed(Drawable errorDrawable) {
//                Log.e(TAG, "onBitmapFailed: ");
//            }
//
//            @Override
//            public void onPrepareLoad(Drawable placeHolderDrawable) {
//                Log.e(TAG, "onPrepareLoad: " + netBGUrl);
//            }
//        };
//        //Picasso下载
//        Picasso.with(getActivity()).load(netBGUrl).into(target);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getSketchSize();
    }

    private void getSketchSize() {
        ViewTreeObserver vto = mSketchView.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                if (sketchViewHeight == 0 && sketchViewWidth == 0) {
                    int height = mSketchView.getMeasuredHeight();
                    int width = mSketchView.getMeasuredWidth();
                    sketchViewHeight = height;
                    sketchViewWidth = width;
                    sketchViewRight = mSketchView.getRight();
                    sketchViewBottom = mSketchView.getBottom();
                    Log.i("onPreDraw", sketchViewHeight + "  " + sketchViewWidth);
                    decorHeight = getActivity().getWindow().getDecorView().getMeasuredHeight();
                    decorWidth = getActivity().getWindow().getDecorView().getMeasuredWidth();
                    Log.i("onPreDraw", "decor height:" + decorHeight + "   width:" + decorHeight);
                    int height3 = controlLayout.getMeasuredHeight();
                    int width3 = controlLayout.getMeasuredWidth();
                    Log.i("onPreDraw", "controlLayout  height:" + height3 + "   width:" + width3);
                }
                return true;
            }
        });
        Log.i("getSketchSize", sketchViewHeight + "  " + sketchViewWidth);
    }

    protected void setSeekBarProgress(int progress, int drawMode) {
        int calcProgress = progress > 1 ? progress : 1;
        int newSize = Math.round((size / 100f) * calcProgress);
        int offset = Math.round((size - newSize) / 2);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(newSize, newSize);
        lp.setMargins(offset, offset, offset, offset);
        if (drawMode == STROKE_TYPE_DRAW) {
            strokeImageView.setLayoutParams(lp);
        } else {
            eraserImageView.setLayoutParams(lp);
        }
        mSketchView.setSize(newSize, drawMode);
    }

    @Override
    public void onDrawChanged() {
        // Undo
        if (mSketchView.getStrokeRecordCount() > 0)
            btn_undo.setAlpha(1f);
        else
            btn_undo.setAlpha(0.4f);
        // Redo
        if (mSketchView.getRedoCount() > 0)
            btn_redo.setAlpha(1f);
        else
            btn_redo.setAlpha(0.4f);
    }

    private void updateGV() {
        sketchGVAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
//        if (id == R.id.btn_add) {
//            if (mSketchView.getVisibility() == View.VISIBLE) {
//                mSketchView.createCurThumbnailBM();
//                showSketchView(false);
//            } else {
//                showSketchView(true);
//            }
//            updateGV();
//        } else
        if (id == R.id.btn_stroke) {
            if (mSketchView.getEditMode() == SketchView.EDIT_STROKE && mSketchView.getStrokeType() != STROKE_TYPE_ERASER) {
                showParamsPopupWindow(v, STROKE_TYPE_DRAW);
            } else {
                int checkedId = strokeTypeRG.getCheckedRadioButtonId();
                if (checkedId == R.id.stroke_type_rbtn_draw) {
                    strokeType = STROKE_TYPE_DRAW;
                } else if (checkedId == R.id.stroke_type_rbtn_line) {
                    strokeType = STROKE_TYPE_LINE;
                } else if (checkedId == R.id.stroke_type_rbtn_circle) {
                    strokeType = STROKE_TYPE_CIRCLE;
                } else if (checkedId == R.id.stroke_type_rbtn_rectangle) {
                    strokeType = STROKE_TYPE_RECTANGLE;
                } else if (checkedId == R.id.stroke_type_rbtn_text) {
                    strokeType = STROKE_TYPE_TEXT;
                }
                mSketchView.setStrokeType(strokeType);
            }
            mSketchView.setEditMode(SketchView.EDIT_STROKE);
            resetBtnStatus(btn_stroke);
        } else if (id == R.id.btn_eraser) {
            if (mSketchView.getEditMode() == SketchView.EDIT_STROKE && mSketchView.getStrokeType() == STROKE_TYPE_ERASER) {
                showParamsPopupWindow(v, STROKE_TYPE_ERASER);
            } else {
                mSketchView.setStrokeType(STROKE_TYPE_ERASER);
            }
            mSketchView.setEditMode(SketchView.EDIT_STROKE);
            resetBtnStatus(btn_eraser);
        } else if (id == R.id.btn_undo) {
            mSketchView.undo();
        } else if (id == R.id.btn_redo) {
            mSketchView.redo();
        } else if (id == R.id.btn_empty) {
            askForErase();
        } else if (id == R.id.btn_photo) {
            startMultiImageSelector(REQUEST_IMAGE);
        } else if (id == R.id.btn_background) {
            startMultiImageSelector(REQUEST_BACKGROUND);
        } else if (id == R.id.btn_drag) {
            mSketchView.setEditMode(SketchView.EDIT_PHOTO);
            resetBtnStatus(btn_drag);
        } else if (id == R.id.btn_commit) {
            if (mWhiteBoardCallback != null) {
                String photoName = TEMP_FILE_NAME + TimeUtils.getNowTimeString();
                new saveToFileCallBackTask().execute(photoName);
            }
        }
//        else if (id == R.id.btn_close) {
//            if (pushBtnCallback != null) {
//                pushBtnCallback.onCloseBtnClick();
//            }
//        }
        else if (id == R.id.btn_commit) {
            if (pushBtnCallback != null) {
                String photoName = TEMP_FILE_NAME + TimeUtils.getNowTimeString();
                pushBtnCallback.onCommitBtnClick(saveInOI(TEMP_FILE_PATH, photoName, 50));
            }
        } else if (id == R.id.iv_see_question) {
            if (mWhiteBoardCallback != null) {
                mWhiteBoardCallback.onShowQuestion();
            }
        }
    }

    private void startMultiImageSelector(int request) {
        MultiImageSelector selector = MultiImageSelector.create(getActivity());
        selector.showCamera(true);
        selector.count(9);
        selector.single();
        selector.origin(mSelectPath);
        Bundle boundsBundle = new Bundle();
        Rect rect = new Rect();
        mSketchView.getLocalVisibleRect(rect);
        int[] boundsInts = new int[4];
        //noinspection Range
        mSketchView.getLocationInWindow(boundsInts);
//        boundsInts[1] -= ScreenUtils.getStatusBarHeight(activity);
//        boundsInts[1] -= controlLayout.getHeight();
        boundsInts[2] = mSketchView.getWidth();
        boundsInts[3] = mSketchView.getHeight();
        selector.start(this, boundsInts, request);
    }

    private void showSaveDialog() {
        saveDialog.show();
        saveET.setText(TimeUtils.getNowTimeString());
        saveET.selectAll();
        ScreenUtils.showInput(mSketchView);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == getActivity().RESULT_OK) {
                mSelectPath = data.getStringArrayListExtra(MultiImageSelector.EXTRA_RESULT);
                String path = "";
                if (mSelectPath.size() == 1) {
                    path = mSelectPath.get(0);
                } else if (mSelectPath == null || mSelectPath.size() == 0) {
                    Toast.makeText(getActivity(), "图片加载失败,请重试!", Toast.LENGTH_LONG).show();
                }
                //加载图片
                mSketchView.addPhotoByPath(path);
                mSketchView.setEditMode(SketchView.EDIT_PHOTO);
                resetBtnStatus(btn_drag);
            }
        } else if (requestCode == REQUEST_BACKGROUND) {//设置背景成功
            if (resultCode == getActivity().RESULT_OK) {
                mSelectPath = data.getStringArrayListExtra(MultiImageSelector.EXTRA_RESULT);
                String path = "";
                if (mSelectPath.size() == 1) {
                    path = mSelectPath.get(0);
                } else if (mSelectPath == null || mSelectPath.size() == 0) {
                    Toast.makeText(getActivity(), "图片加载失败,请重试!", Toast.LENGTH_LONG).show();
                }
                mSketchView.setBackgroundByPath(path);
                Log.i("imgPath", path);
                //加载图片设置画板背景
            }
        }
    }

    private void showParamsPopupWindow(View anchor, int drawMode) {
        if (BitmapUtils.isLandScreen(activity)) {
            if (drawMode == STROKE_TYPE_DRAW) {
                strokePopupWindow.showAsDropDown(anchor, (ScreenUtils.dip2px(activity, -pupWindowsDPWidth) + anchor.getWidth()) / 2, 0);
//                strokePopupWindow.showAsDropDown(anchor, ScreenUtils.dip2px(activity, -pupWindowsDPWidth), -anchor.getHeight());
            } else {
//                eraserPopupWindow.showAsDropDown(anchor, 0, 0);
                eraserPopupWindow.showAsDropDown(anchor, (ScreenUtils.dip2px(activity, -pupWindowsDPWidth) + anchor.getWidth()) / 2, 0);
//                eraserPopupWindow.showAsDropDown(anchor, ScreenUtils.dip2px(activity, -pupWindowsDPWidth), -anchor.getHeight());
            }
        } else {
            if (drawMode == STROKE_TYPE_DRAW) {
//                strokePopupWindow.showAsDropDown(anchor, 0, ScreenUtils.dip2px(activity, -strokePupWindowsDPHeight) - anchor.getHeight());
                strokePopupWindow.showAsDropDown(anchor, 0, 0);
            } else {
//                eraserPopupWindow.showAsDropDown(anchor, 0, ScreenUtils.dip2px(activity, -eraserPupWindowsDPHeight) - anchor.getHeight());
                eraserPopupWindow.showAsDropDown(anchor, 0, 0);
            }
        }
    }

    /**
     * 弹出键盘输入文字
     *
     * @param anchor
     * @param record
     */
    private void showTextPopupWindow(View anchor, final StrokeRecord record) {
        strokeET.requestFocus();
//        textPopupWindow.showAsDropDown(anchor, record.textOffX, record.textOffY - mSketchView.getHeight());
        textPopupWindow.showAtLocation(anchor, Gravity.CENTER, 0, 0);
        //隐藏题目悬浮窗
        if (mWhiteBoardCallback != null) {
            mWhiteBoardCallback.dismissWindow();
        }
        textPopupWindow.setSoftInputMode(PopupWindow.INPUT_METHOD_NEEDED);
        InputMethodManager imm = (InputMethodManager) activity
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        textPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                //显示题目悬浮窗
                if (mWhiteBoardCallback != null) {
                    mWhiteBoardCallback.showWindow();
                }
                if (!strokeET.getText().toString().equals("")) {
                    record.text = strokeET.getText().toString();
                    record.textPaint.setTextSize(strokeET.getTextSize());
                    record.textWidth = strokeET.getMaxWidth();
                    mSketchView.addStrokeRecord(record);
                }
            }
        });
    }

    private void saveInUI(final String imgName) {
        new saveToFileTask().execute(imgName);
    }

    /**
     * show 保存图片到本地文件，耗时操作
     *
     * @param filePath 文件保存路径
     * @param imgName  文件名
     * @param compress 压缩百分比1-100
     * @return 返回保存的图片文件
     * @author TangentLu
     * create at 16/6/17 上午11:18
     */
    public File saveInOI(String filePath, String imgName, int compress) {
        if (!imgName.contains(".png")) {
            imgName += ".png";
        }
//        Log.e(TAG, "saveInOI: " + System.currentTimeMillis());

        //添加题目到画板中 使用回调方法.
        Bitmap newBM;
        if (mWhiteBoardCallback != null) {
            newBM = mSketchView.getResultBitmap(mWhiteBoardCallback.getAddBitmap());
        } else {
            newBM = mSketchView.getResultBitmap();
        }

//        Log.e(TAG, "saveInOI: " + System.currentTimeMillis());

        try {
            File dir = new File(filePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File f = new File(filePath, imgName);
            if (!f.exists()) {
                f.createNewFile();
            } else {
                f.delete();
            }
            FileOutputStream out = new FileOutputStream(f);
//            Log.e(TAG, "saveInOI: " + System.currentTimeMillis());

            if (compress >= 1 && compress <= 100)
                newBM.compress(Bitmap.CompressFormat.PNG, compress, out);
            else {
                newBM.compress(Bitmap.CompressFormat.PNG, 80, out);
            }
            Log.e(TAG, "saveInOI: " + System.currentTimeMillis());

            out.close();
            newBM.recycle();
            newBM = null;
            return f;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 清空画板
     */
    private void askForErase() {
        if (mWhiteBoardCallback != null) {
            mWhiteBoardCallback.dismissWindow();
        }
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setMessage("是否清空画板?")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSketchView.erase();
                        if (mWhiteBoardCallback == null) return;
                        mWhiteBoardCallback.showWindow();
                    }
                })
                .create();
        alertDialog.show();
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (mWhiteBoardCallback == null) return;
                mWhiteBoardCallback.showWindow();
            }
        });
    }

    private void resetBtnStatus(ImageView chosenIV) {
        btn_eraser.setImageResource(R.drawable.s_db_ct_ic_es);
        btn_stroke.setImageResource(R.drawable.s_db_ct_ic_pe);
        btn_drag.setImageResource(R.drawable.s_db_ct_ic_ct);
        int i = chosenIV.getId();
        if (i == R.id.btn_eraser) {
            chosenIV.setImageResource(R.drawable.s_db_ct_ic_es_che);
        } else if (i == R.id.btn_stroke) {
            chosenIV.setImageResource(R.drawable.s_db_ct_ic_pe_che);
        } else if (i == R.id.btn_drag) {
            chosenIV.setImageResource(R.drawable.s_db_ct_ic_ct_che);
        }
    }


    public interface WhiteBoardCallback {
        /**
         * 提交图片答案
         *
         * @param filePath
         */
        void onSendBtnClick(File filePath);

        /**
         * 添加题目bitmap到画板中
         */
        Bitmap getAddBitmap();

        /**
         * 显示悬浮窗
         */
        void showWindow();

        /**
         * 隐藏悬浮窗
         */
        void dismissWindow();

        @Deprecated
        void onShowQuestion();
    }

    public interface PushBtnCallback {
        void onCloseBtnClick();

        void onCommitBtnClick(File filePath);
    }

    class saveToFileTask extends AsyncTask<String, Void, File> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new AlertDialog.Builder(activity)
                    .setTitle("保存画板")
                    .setMessage("保存中...")
                    .show();

        }

        @Override
        protected File doInBackground(String... photoName) {
            return saveInOI(FILE_PATH, photoName[0]);
        }

        @Override
        protected void onPostExecute(File file) {
            super.onPostExecute(file);
            if (file.exists())
                Toast.makeText(getActivity(), file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(getActivity(), "保存失败！", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            FileUtils.scanFile(getActivity(), file.getPath());
        }
    }

    /**
     * 点击完成时保存图片的任务
     */
    class saveToFileCallBackTask extends AsyncTask<String, Void, File> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setCancelable(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage("保存中...");
            progressDialog.show();
        }

        @Override
        protected File doInBackground(String... photoName) {
            return saveInOI(TEMP_FILE_PATH, photoName[0], 50);
        }

        @Override
        protected void onPostExecute(File file) {
            super.onPostExecute(file);
//            if (file.exists())
//                Toast.makeText(getActivity(), file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
//            else
//                Toast.makeText(getActivity(), "保存失败！", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            mWhiteBoardCallback.onSendBtnClick(file);
        }
    }
}
