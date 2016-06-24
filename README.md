##[SketchBoard项目][3]简介
> ##SketchBoard是一个可涂鸦、绘图、添加文字、图像（可旋转缩放）、背景的Fragment，其中主要由SketchView利用matrix完成所有图形绘制操作。

＊＊图像旋转缩放高仿美图APP的操作方式＊＊

##一、效果演示
####1.1 画笔演示.gif
![画笔演示.gif][101]

####1.2 图像操作演示.gif
![图像操作演示.gif][102]


####1.3 画板切换演示.gif
![画板切换演示.gif][103]

##二、使用说明
####2.1 已上传Bintray，build.gradle加入即可：
> ####compile 'com.yinghe:whiteboardlib:1.0.9'

####2.2 在activity中直接使用：
```java
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //获取Fragment管理器
        FragmentTransaction ts = getSupportFragmentManager().beginTransaction();
        //获取WhiteBoardFragment实例
        WhiteBoardFragment  whiteBoardFragment = WhiteBoardFragment.newInstance();
        //添加到界面中
        ts.add(R.id.fl_main, whiteBoardFragment, "wb").commit();
    }
```

####2.3 WhiteBoardFragment的API说明：
```java
     /**
     * show 默认新建一个学生端功能
     * @author TangentLu
     * create at 16/6/17 上午9:59
     */
    public static WhiteBoardFragment newInstance() {
    }

    /**
     * show 新建一个教师端的画板碎片，有推送按钮
     * @param callback 推送按钮监听器，接受返回的图片文件路径可用于显示文件
     * @author TangentLu
     * create at 16/6/17 上午9:57
     */
    public static WhiteBoardFragment newInstance(SendBtnCallback callback) {
    }

    /**
     * @param imgPath 添加的背景图片文件路径
     * @author TangentLu
     * create at 16/6/21 下午3:39
     * show 设置当前白板的背景图片
     */
    public void setCurBackgroundByPath(String imgPath) {
    }

    /**
     * show  新增白板并设置白板的背景图片
     * @param imgPath 添加的背景图片文件路径
     * @author TangentLu
     * create at 16/6/21 下午3:39
     */
    public void setNewBackgroundByPath(String imgPath) {
    }

    /**
     * show 新增图片到当前白板
     * @param imgPath 新增的图片路径
     * @author TangentLu
     * create at 16/6/21 下午3:42
     */
    public void addPhotoByPath(String imgPath) {
    }


    /**
     * show 获取当前白板的BitMap
     * @author TangentLu
     * create at 16/6/21 下午3:44
     */
    public Bitmap getResultBitmap() {
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
    }
```

##三、技术博客
主要技术难点可参考以下技术博客:

[Android画板（一）：软键盘遮挡输入焦点的完美解决方案][1]

[Android画板（二）：Matrix实现美图APP的旋转缩放][2]


[1]:http://www.jianshu.com/p/aaf117c49dd7
[2]:http://www.jianshu.com/p/1f2756ddc6f7
[3]:https://github.com/dsbGenius/WhiteBoard


[101]:https://github.com/dsbGenius/WhiteBoard/blob/master/captures/stroke.gif
[102]:https://github.com/dsbGenius/WhiteBoard/blob/master/captures/image.gif
[103]:https://github.com/dsbGenius/WhiteBoard/blob/master/captures/switchSketchBoard.gif
