package shaoxia; // 声明当前类所在的包名为 shaoxia

import com.formdev.flatlaf.FlatDarkLaf; // 引入 FlatLaf 提供的深色主题类
import shaoxia.Utils.RippleButton;
import shaoxia.modules.*;
import shaoxia.ui.components.BlueCat;
import shaoxia.ui.components.ModernTrayMenu;
import shaoxia.ui.components.SettingsDialog;

import javax.swing.*; // 引入 Java Swing 核心图形界面库
import java.awt.*; // 引入 Java AWT 窗口工具包
import java.awt.event.*; // 引入 Java 事件监听器相关的类
import java.awt.image.FilteredImageSource; // 引入图像过滤源类
import java.awt.image.ImageFilter; // 引入图像过滤器基础类
import java.awt.image.RGBImageFilter; // 引入基于 RGB 像素的图像过滤器类
import java.io.File; // 引入文件操作类
import java.io.RandomAccessFile; // 引入随机访问文件类，用于文件锁
import java.nio.channels.FileChannel; // 引入文件通道类，用于建立底层锁
import java.nio.channels.FileLock; // 引入文件锁类，防止程序多开
import java.util.prefs.Preferences; // 引入用户偏好设置类，用于记忆配置

/** 主页面板块 MainLauncher */ // 类的文档注释
public class MainLauncher extends JFrame { // 定义 MainLauncher 类，继承自 JFrame 主窗口
    private BlueCat blueCat; // 声明一个 BlueCat (蓝猫) 类型的私有变量
    private CardLayout cardLayout; // 声明卡片布局管理器，用于切换不同的功能面板
    private JPanel mainContainer; // 声明主容器面板，承载所有其他子面板
    private static RandomAccessFile raf; // 声明静态随机访问文件对象，用于程序多开检测
    private static FileChannel channel; // 声明静态文件通道对象，配合多开检测
    private static FileLock lock; // 声明静态文件锁对象，确保唯一运行实例

    public static final Font MAIN_FONT = new Font("微软雅黑", Font.PLAIN, 16); // 定义全局通用的常规字体
    public static final Font BOLD_FONT = new Font("微软雅黑", Font.BOLD, 18); // 定义全局通用的加粗字体

    public MainLauncher() { // MainLauncher 类的构造方法
        setTitle(""); // 设置窗口标题为空字符串（因为是无边框窗口）

        setUndecorated(true); // 开启无边框模式，隐藏系统自带的标题栏和最小化/关闭按钮

        // 读取本地记忆的窗口尺寸，默认 960x720
        Preferences prefs = Preferences.userNodeForPackage(MainLauncher.class); // 获取当前类的偏好设置节点
        int winWidth = prefs.getInt("window_width", 640); // 从本地读取存储的窗口宽度，若无则默认为 960
        int winHeight = prefs.getInt("window_height", 520); // 从本地读取存储的窗口高度，若无则默认为 720
        setSize(winWidth, winHeight); // 应用读取到的宽度和高度设置窗口大小

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // 设置点击关闭时的默认操作为“什么都不做”（由托盘接管）
        setLocationRelativeTo(null); // 将窗口居中显示在屏幕中央

        Image appIcon = null; // 声明应用程序的图标变量，初始为空
        try { // 开始捕获可能发生的异常
            java.net.URL iconUrl = getClass().getResource("/app.png"); // 尝试从资源目录中获取橘子图标的路径
            if (iconUrl != null) { // 如果找到了图片路径
                appIcon = new ImageIcon(iconUrl).getImage(); // 将路径转换为 Image 对象
                setIconImage(appIcon); // 设置当前窗口的任务栏图标
            } else { // 如果没找到图片路径
                System.err.println("没找到图片路径：/app.png"); // 在控制台输出错误提示
            }
        } catch (Exception e) { // 捕获异常
            e.printStackTrace(); // 打印异常的堆栈信息
        }

        initSystemTray(appIcon); // 调用系统托盘初始化方法，并传入刚才读取的图标

        addWindowListener(new WindowAdapter() { // 给窗口添加生命周期监听器
            @Override // 重写窗口正在关闭时的方法
            public void windowClosing(WindowEvent e) { // 当触发窗口关闭事件时
                setVisible(false); // 不退出程序，而是将窗口隐藏到后台（托盘）
            }
        });

        cardLayout = new CardLayout(); // 实例化卡片布局管理器对象
        mainContainer = new JPanel(cardLayout); // 实例化主容器面板，并为其设置卡片布局
        add(mainContainer); // 将主容器添加到 JFrame 窗口中

        // 往主容器中添加各个功能板块，并为每个板块定义一个独一无二的名字 (Key)
        mainContainer.add(createMainMenu(), "MENU"); // 添加主菜单面板，命名为 "MENU"
        mainContainer.add(new RenamerPanel(this), "RENAMER"); // 添加全能改名面板，命名为 "RENAMER"
        mainContainer.add(new CheckerPanel(this), "CHECKER"); // 添加数据校对面板，命名为 "CHECKER"
        mainContainer.add(new FinderPanel(this), "FINDER"); // 添加文件检索面板，命名为 "FINDER"
        mainContainer.add(new ScoringPanel(this), "SCORING"); // 添加评分生成面板，命名为 "SCORING"
        mainContainer.add(new ReplacerPanel(this), "REPLACER"); // 添加文档替换面板，命名为 "REPLACER"
        mainContainer.add(new ComparisonPanel(this), "COMPARISON"); // 添加公告比对面板，命名为 "COMPARISON"
        //新增在这里
        mainContainer.add(new RegionFetcherPanel(this), "REGION");

        cardLayout.show(mainContainer, "MENU"); // 默认展示名为 "MENU" 的主菜单面板

        // === 【召唤蓝猫】 ===
        blueCat = new BlueCat(this); // 实例化蓝猫特效组件，传入当前窗口实例
        setGlassPane(blueCat); // 将蓝猫设置为窗口的透明玻璃层 (最高层级)
        blueCat.setCatVisible(true); // 设置蓝猫初始状态为可见

        // === 【1. 窗口圆角与随动裁剪】 ===
        addComponentListener(new ComponentAdapter() { // 监听窗口组件变化事件
            @Override // 重写组件尺寸改变的方法
            public void componentResized(ComponentEvent e) { // 当窗口大小被拉伸或缩小时
                // 给无边框窗口裁剪出圆角，25 表示圆角的像素弧度
                setShape(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 25, 25));

                if (blueCat != null) { // 如果蓝猫对象不为空
                    blueCat.updateBounds(); // 通知蓝猫更新它在窗口中的相对坐标
                }
            }
        });

        // === 【2. 窗口拖拽移动与边缘放大缩小】 ===
        MouseAdapter dragAndResizeListener = new MouseAdapter() { // 创建一个通用的鼠标事件适配器
            private final int BORDER_THICKNESS = 10; // 定义窗口边缘感应区的厚度为 10 像素
            private int cursorType = Cursor.DEFAULT_CURSOR; // 记录当前应当显示的鼠标指针状态，默认为普通指针
            private Point startPos = null; // 记录鼠标按下时的屏幕绝对坐标
            private Rectangle startBounds = null; // 记录鼠标按下时窗口的尺寸和位置

            @Override // 重写鼠标移动事件
            public void mouseMoved(MouseEvent e) { // 当鼠标在窗口内移动时不断触发
                Point p = e.getPoint(); // 获取鼠标当前在窗口内的相对坐标
                int w = getWidth(); // 获取窗口的当前宽度
                int h = getHeight(); // 获取窗口的当前高度

                // 判断鼠标是否处于窗口边缘的 10 像素感应区内
                boolean left = p.x < BORDER_THICKNESS; // 是否在左边缘
                boolean right = p.x > w - BORDER_THICKNESS; // 是否在右边缘
                boolean top = p.y < BORDER_THICKNESS; // 是否在上边缘
                boolean bottom = p.y > h - BORDER_THICKNESS; // 是否在下边缘

                // 根据鼠标所处边缘的位置，切换对应的 8 向缩放箭头指针
                if (top && left) cursorType = Cursor.NW_RESIZE_CURSOR; // 左上角
                else if (top && right) cursorType = Cursor.NE_RESIZE_CURSOR; // 右上角
                else if (bottom && left) cursorType = Cursor.SW_RESIZE_CURSOR; // 左下角
                else if (bottom && right) cursorType = Cursor.SE_RESIZE_CURSOR; // 右下角
                else if (top) cursorType = Cursor.N_RESIZE_CURSOR; // 顶部
                else if (bottom) cursorType = Cursor.S_RESIZE_CURSOR; // 底部
                else if (left) cursorType = Cursor.W_RESIZE_CURSOR; // 左侧
                else if (right) cursorType = Cursor.E_RESIZE_CURSOR; // 右侧
                else cursorType = Cursor.DEFAULT_CURSOR; // 不在边缘时恢复默认指针

                setCursor(Cursor.getPredefinedCursor(cursorType)); // 应用算好的鼠标指针样式
            }

            @Override // 重写鼠标按下事件
            public void mousePressed(MouseEvent e) { // 当鼠标左键/右键被按下时
                startPos = e.getLocationOnScreen(); // 记录此刻鼠标在屏幕上的绝对坐标
                startBounds = getBounds(); // 记录此刻窗口在屏幕上的大小和位置
            }

            @Override // 重写鼠标拖拽事件
            public void mouseDragged(MouseEvent e) { // 当按住鼠标按键并移动时不断触发
                if (startPos == null || startBounds == null) return; // 如果没有初始坐标数据，直接跳过防报错
                Point p = e.getLocationOnScreen(); // 获取拖拽过程中鼠标的当前屏幕绝对坐标
                int dx = p.x - startPos.x; // 计算鼠标在 X 轴向移动的偏移量
                int dy = p.y - startPos.y; // 计算鼠标在 Y 轴向移动的偏移量

                if (cursorType == Cursor.DEFAULT_CURSOR) { // 如果指针是普通状态（说明按下的不是边缘）
                    // 此时执行拖拽整个窗口的操作，根据偏移量重新设定窗口位置
                    setLocation(startBounds.x + dx, startBounds.y + dy);
                } else { // 如果按下的是边缘区域
                    // 此时执行拉伸/缩小窗口操作
                    Rectangle bounds = new Rectangle(startBounds); // 复制初始窗口框体数据

                    // 如果按住的是右侧、右上、右下，则调整宽度
                    if (cursorType == Cursor.E_RESIZE_CURSOR || cursorType == Cursor.NE_RESIZE_CURSOR || cursorType == Cursor.SE_RESIZE_CURSOR) {
                        bounds.width += dx; // 宽度增加/减少偏移量
                    }
                    // 如果按住的是底部、左下、右下，则调整高度
                    if (cursorType == Cursor.S_RESIZE_CURSOR || cursorType == Cursor.SW_RESIZE_CURSOR || cursorType == Cursor.SE_RESIZE_CURSOR) {
                        bounds.height += dy; // 高度增加/减少偏移量
                    }
                    // 如果按住的是左侧、左上、左下，不仅要改宽度，还要反向修改窗口左上角的 X 坐标
                    if (cursorType == Cursor.W_RESIZE_CURSOR || cursorType == Cursor.NW_RESIZE_CURSOR || cursorType == Cursor.SW_RESIZE_CURSOR) {
                        bounds.width -= dx; // 宽度反向变化
                        bounds.x += dx; // X 坐标跟随鼠标位置
                    }
                    // 如果按住的是上方、左上、右上，不仅改高度，还要反向修改窗口左上角的 Y 坐标
                    if (cursorType == Cursor.N_RESIZE_CURSOR || cursorType == Cursor.NW_RESIZE_CURSOR || cursorType == Cursor.NE_RESIZE_CURSOR) {
                        bounds.height -= dy; // 高度反向变化
                        bounds.y += dy; // Y 坐标跟随鼠标位置
                    }

                    // 设置窗口的安全最小尺寸，防止被缩成一条线导致界面崩溃
                    int minWidth = 640; // 最小宽度限制为 960
                    int minHeight = 480; // 最小高度限制为 720

                    if (bounds.width < minWidth) { // 如果拉伸后的宽度小于最小值
                        bounds.width = minWidth; // 强制设回最小值
                        // 如果是从左侧拉伸导致的超限，还要把 X 坐标顶回去
                        if (cursorType == Cursor.W_RESIZE_CURSOR || cursorType == Cursor.NW_RESIZE_CURSOR || cursorType == Cursor.SW_RESIZE_CURSOR) {
                            bounds.x = startBounds.x + startBounds.width - minWidth;
                        }
                    }
                    if (bounds.height < minHeight) { // 如果拉伸后的高度小于最小值
                        bounds.height = minHeight; // 强制设回最小值
                        // 如果是从上方拉伸导致的超限，还要把 Y 坐标顶回去
                        if (cursorType == Cursor.N_RESIZE_CURSOR || cursorType == Cursor.NW_RESIZE_CURSOR || cursorType == Cursor.NE_RESIZE_CURSOR) {
                            bounds.y = startBounds.y + startBounds.height - minHeight;
                        }
                    }

                    setBounds(bounds); // 把计算好的最终框体大小和位置应用给窗口
                }
            }
        };

        // 将上面写好的鼠标事件适配器，同时绑定给主容器的监听池和拖拽监听池
        mainContainer.addMouseListener(dragAndResizeListener);
        mainContainer.addMouseMotionListener(dragAndResizeListener);
    }

    private void initSystemTray(Image iconImage) { // 初始化系统托盘的方法
        if (!SystemTray.isSupported()) { // 检查当前操作系统是否支持托盘功能
            System.err.println("当前系统不支持系统托盘功能，回退为常规关闭模式。"); // 打印提示
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 若不支持，则直接把关闭行为改为退出程序
            return; // 终止此方法
        }

        if (iconImage == null) { // 如果传进来的托盘图标是空的
            iconImage = new ImageIcon(new byte[0]).getImage(); // 给一个空白默认图片防报错
        }

        SystemTray tray = SystemTray.getSystemTray(); // 获取系统托盘的实例
        ModernTrayMenu modernMenu = new ModernTrayMenu(this, this); // 实例化咱们自己写的现代化圆角托盘菜单
        TrayIcon trayIcon = new TrayIcon(iconImage, "Toolkit - Running in Background"); // 创建托盘图标对象并设置提示语
        trayIcon.setImageAutoSize(true); // 开启托盘图标自适应系统大小功能

        trayIcon.addMouseListener(new MouseAdapter() { // 给托盘图标添加鼠标点击事件
            @Override // 重写鼠标释放事件（右键通常用释放触发更精准）
            public void mouseReleased(MouseEvent e) { // 当鼠标按键松开时
                if (e.getButton() == MouseEvent.BUTTON3) { // 检查如果松开的是鼠标右键
                    Point mousePos = MouseInfo.getPointerInfo().getLocation(); // 获取当前鼠标指针在屏幕上的绝对位置

                    // 计算自定义菜单的弹出坐标，让菜单水平居中对齐鼠标，并在上方留 5 像素间距
                    int x = mousePos.x - modernMenu.getWidth() / 2;
                    int y = mousePos.y - modernMenu.getHeight() - 5;

                    // 获取当前屏幕的可用最大边界区域（刨除系统任务栏）
                    Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                    // 下面是防越界处理：如果菜单右侧超出了屏幕，就把菜单向左推
                    if (x + modernMenu.getWidth() > screenBounds.x + screenBounds.width) {
                        x = screenBounds.x + screenBounds.width - modernMenu.getWidth() - 5;
                    }
                    // 如果菜单左侧超出了屏幕，就把菜单向右推
                    if (x < screenBounds.x) x = screenBounds.x + 5;
                    // 如果菜单底部超出了屏幕边界
                    if (y + modernMenu.getHeight() > screenBounds.y + screenBounds.height) {
                        y = screenBounds.y + screenBounds.height - modernMenu.getHeight() - 5;
                    }
                    // 如果菜单顶部超出了屏幕边界（说明任务栏在上面或者空间不够）
                    if (y < screenBounds.y) {
                        y = mousePos.y + 20; // 改为在鼠标下方 20 像素处弹出
                    }

                    modernMenu.setLocation(x, y); // 设置菜单最终算好的弹出坐标
                    modernMenu.setVisible(true); // 将菜单显示出来
                    modernMenu.requestFocus(); // 给菜单请求焦点，这样点击其他地方时它能自动隐藏
                }
            }

            @Override // 重写鼠标点击事件
            public void mouseClicked(MouseEvent e) { // 当鼠标发生点击行为时
                // 如果是左键点击，并且连续点击了 2 下（双击）
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    modernMenu.setVisible(false); // 先隐藏可能存在弹出的托盘菜单
                    setVisible(true); // 恢复主程序的可见状态
                    setExtendedState(JFrame.NORMAL); // 取消可能存在的最小化状态
                    toFront(); // 强制将主程序窗口提到所有窗口的最前面
                }
            }
        });

        try { // 尝试添加托盘操作
            tray.add(trayIcon); // 将设置好的图标正式加进系统的托盘区
        } catch (AWTException e) { // 如果遇到 AWT 异常（比如系统阻止）
            System.err.println("系统托盘图标添加失败！"); // 打印错误信息
        }
    }

    public void switchPanel(String cardName) { // 切换面板的公共方法
        cardLayout.show(mainContainer, cardName); // 命令卡片布局器切换到对应 Key 名字的界面

        if (blueCat != null) { // 如果蓝猫对象存在
            if ("MENU".equals(cardName)) { // 检查如果切回的是 "MENU" 主菜单
                blueCat.setCatVisible(true); // 就把蓝猫放出来
            } else { // 如果切到的是其他子功能面板
                blueCat.setCatVisible(false); // 就把蓝猫隐藏起来
            }
        }
    }

    // 一个纯代码绘制的齿轮状设置按钮内部类
    class GearButton extends JButton {
        public GearButton() { // 构造方法
            setContentAreaFilled(false); // 不填充按钮背景，保持透明
            setBorderPainted(false); // 不绘制系统默认按钮边框
            setFocusPainted(false); // 点击时不显示那个虚线的焦点框
            setCursor(new Cursor(Cursor.HAND_CURSOR)); // 鼠标放上去变成小手
            setToolTipText("Settings"); // 设置鼠标悬停时的文字提示
            setPreferredSize(new Dimension(45, 45)); // 强制规定按钮大小为 45x45 像素
        }

        @Override // 重写系统组件绘制方法
        protected void paintComponent(Graphics g) { // 核心绘图区
            Graphics2D g2 = (Graphics2D) g.create(); // 复制画笔，开启 2D 高级绘图模式

            // 开启画笔的抗锯齿属性，让画出的线条和圆形平滑无毛边
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = getWidth() / 2; // 计算按钮中心点的 X 坐标
            int cy = getHeight() / 2; // 计算按钮中心点的 Y 坐标
            int radius = 11; // 地球的半径大小（这个数值和原先的鱼骨头占位面积完美契合）

            // 检查鼠标是否正悬停在按钮上方，用于实现悬浮点亮的交互效果
            boolean isHover = getModel().isRollover();

            // ==== 1. 绘制底座和支撑柱 ====
            // 悬浮时金属底座略微变亮
            g2.setColor(isHover ? new Color(130, 135, 145) : new Color(100, 105, 115));
            g2.fillRoundRect(cx - radius / 2, cy + radius + 7, radius, 4, 3, 3); // 底盘
            g2.fillRect(cx - 2, cy + radius + 3, 4, 5); // 支撑柱

            // ==== 2. 绘制倾斜的半圆弧形金属支架 ====
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(isHover ? Color.WHITE : new Color(170, 175, 185)); // 悬浮时支架变纯白
            g2.drawArc(cx - radius - 4, cy - radius - 4, (radius + 4) * 2, (radius + 4) * 2, -70, 180);

            // ==== 3. 绘制地球本体 (蔚蓝色海洋背景) ====
            g2.setColor(isHover ? new Color(52, 152, 219) : new Color(41, 128, 185)); // 悬浮时海洋更明亮
            g2.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

            // ==== 4. 绘制陆地色块 (利用剪裁防止画出地球边界) ====
            g2.setClip(new java.awt.geom.Ellipse2D.Float(cx - radius, cy - radius, radius * 2, radius * 2));
            g2.setColor(isHover ? new Color(46, 204, 113) : new Color(39, 174, 96)); // 悬浮时陆地更翠绿
            // 简单模拟几个大陆板块的抽象形状
            g2.fillOval(cx - radius / 2, cy - radius / 3, radius + 5, radius - 2);
            g2.fillOval(cx + radius / 5, cy + radius / 8, radius / 2, radius + 5);
            g2.fillOval(cx - radius + 2, cy + radius / 2, radius + 5, radius / 2);

            // ==== 5. 绘制经纬网格 (增加 3D 球体空间感) ====
            g2.setColor(new Color(255, 255, 255, isHover ? 70 : 40)); // 半透明白色网线
            g2.setStroke(new BasicStroke(0.8f));
            // 纬线 (横向椭圆)
            g2.drawOval(cx - radius, cy - radius / 2, radius * 2, radius);
            g2.drawOval(cx - radius, cy - radius / 5, radius * 2, radius * 2 / 5);
            g2.drawOval(cx - radius, cy + radius / 5, radius * 2, radius);
            // 经线 (纵向椭圆)
            g2.drawOval(cx - radius / 2, cy - radius, radius, radius * 2);
            g2.drawOval(cx - radius / 5, cy - radius, radius * 2 / 5, radius * 2);
            g2.drawOval(cx + radius / 5, cy - radius, radius, radius * 2);

            // ==== 6. 绘制球面顶部高光 (凸显玻璃/光滑质感) ====
            g2.setClip(null); // 取消剪裁，回到正常画板
            g2.setColor(new Color(255, 255, 255, isHover ? 130 : 80)); // 更亮一点的半透明白
            g2.fillOval(cx - radius / 2, cy - radius + 2, radius, radius / 3);

            // ==== 7. 绘制支架与地球连接的两个小圆轴心 ====
            g2.setColor(isHover ? Color.WHITE : new Color(170, 175, 185));
            int axisX = (int) (Math.cos(Math.toRadians(-70)) * (radius + 4));
            int axisY = (int) (Math.sin(Math.toRadians(-70)) * (radius + 4));
            g2.fillOval(cx + axisX - 2, cy + axisY - 2, 4, 4);
            g2.fillOval(cx - axisX - 2, cy - axisY - 2, 4, 4);

            g2.dispose(); // 用完画笔后销毁，释放内存
        }
    }

    private JPanel createMainMenu() { // 创建主界面面板的方法
        BackgroundPanel menuPanel = new BackgroundPanel("bg_main.png"); // 实例化带背景图的面板
        menuPanel.setLayout(new BorderLayout()); // 将其设置为边界布局 (分东南西北中)

        // 顶部的系统操作栏，左对齐，组件之间间距分别为 15 和 10
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        topBar.setOpaque(false); // 设置顶部栏透明，防止遮挡背景图

        GearButton settingsBtn = new GearButton(); // 实例化刚才手绘的那个齿轮按钮
        settingsBtn.addActionListener(e -> SettingsDialog.showDialog(MainLauncher.this)); // 给按钮绑上点击弹出设置面板的事件
        topBar.add(settingsBtn); // 把按钮加到顶部栏里

        // 中间的功能区面板，采用网格包布局管理，能精确控制控件的比例和位置
        JPanel topArea = new JPanel(new GridBagLayout());
        topArea.setOpaque(false); // 同样设置透明
        GridBagConstraints gbc = new GridBagConstraints(); // 实例化网格包约束条件对象

        JLabel title = new JLabel("Toolkit"); // 创建标题文字标签
        title.setFont(new Font("Montserrat", Font.BOLD, 56)); // 设置标题字体为幼圆，粗体，56号字
        title.setForeground(Color.WHITE); // 设置标题文字颜色为纯白色
        gbc.gridx = 0; // 指定放在网格的第 0 列
        gbc.gridy = 0; // 指定放在网格的第 0 行
        gbc.gridwidth = 3; // 占据 3 列的宽度，让它居中横跨整个页面
        gbc.insets = new Insets(10, 0, 40, 0); // 设置标题周围的边距：上 10，下 40
        gbc.weighty = 0; // Y 轴不分配额外多余空间
        topArea.add(title, gbc); // 将标题连同规矩一起添加到面板

        // 下面开始连续创建 9 个花里胡哨的功能按钮，调用封装好的生成方法
// --- 统一全局玻璃质感（黑雾半透玻璃，R, G, B, Alpha） ---
        Color glassColor = new Color(30, 35, 40, 120);

        JButton btn1 = createModuleButton("RenamerPanel", "RENAMER", glassColor);
        JButton btn2 = createModuleButton("CheckerPanel", "CHECKER", glassColor);
        JButton btn3 = createModuleButton("FinderPanel", "FINDER", glassColor);

        JButton btn4 = createModuleButton("ScoringPanel", "SCORING", glassColor);
        JButton btn5 = createModuleButton("ReplacerPanel", "REPLACER", glassColor);
        JButton btn6 = createModuleButton("ComparisonPanel", "COMPARISON", glassColor);

        JButton btn7 = createModuleButton("RegionFetcherPanel", "REGION", glassColor);
        JButton btn8 = createModuleButton("OasisPanel", "PY_HELPER", glassColor);
        JButton btn9 = createModuleButton("HavenPanel", "DEV", glassColor);


        gbc.gridwidth = 1; // 恢复占位为 1 列，按钮各自独立排布
        gbc.insets = new Insets(10, 10, 10, 10); // 设置每个按钮四周保持 10 像素的间距

        // 第 1 行的三个按钮布局配置
        gbc.gridy = 1; // 放到网格第 1 行
        gbc.weightx = 1.0;   gbc.gridx = 0; topArea.add(btn1, gbc); // 第一列拉伸比例 1.0
        gbc.weightx = 0.618; gbc.gridx = 1; topArea.add(btn2, gbc); // 第二列拉伸比例 0.618 (黄金分割点)
        gbc.weightx = 1.0;   gbc.gridx = 2; topArea.add(btn3, gbc); // 第三列拉伸比例 1.0

        // 第 2 行的三个按钮布局配置
        gbc.gridy = 2; // 放到网格第 2 行
        gbc.weightx = 1.0;   gbc.gridx = 0; topArea.add(btn4, gbc);
        gbc.weightx = 0.618; gbc.gridx = 1; topArea.add(btn5, gbc);
        gbc.weightx = 1.0;   gbc.gridx = 2; topArea.add(btn6, gbc);

        // 第 3 行的三个按钮布局配置
        gbc.gridy = 3; // 放到网格第 3 行
        gbc.weightx = 1.0;   gbc.gridx = 0; topArea.add(btn7, gbc);
        gbc.weightx = 0.618; gbc.gridx = 1; topArea.add(btn8, gbc);
        gbc.weightx = 1.0;   gbc.gridx = 2; topArea.add(btn9, gbc);

        // 底部留出弹性空间，把按钮全往上顶
        gbc.gridy = 4; // 放到最后一行
        gbc.gridwidth = 3; // 横跨三列
        gbc.weighty = 1.0; // 分配底下所有的 Y 轴剩余空间
        topArea.add(Box.createVerticalGlue(), gbc); // 塞入一个隐形的弹性胶水组件

        DinoGamePanel gamePanel = new DinoGamePanel(); // 实例化彩蛋恐龙游戏面板
        gamePanel.setVisible(false); // 默认将游戏隐藏起来
        gamePanel.setPreferredSize(new Dimension(800, 320)); // 设置游戏界面的宽高预设值

        // 获取全局键盘事件管理器，用来监听隐藏游戏的快捷键
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED) { // 拦截键盘按键被压下的瞬间
                // 如果按了空格键，且当前游戏没有显示出来
                if (e.getKeyCode() == KeyEvent.VK_SPACE && !gamePanel.isVisible()) {
                    gamePanel.setVisible(true); // 显示小恐龙游戏
                    menuPanel.revalidate(); // 重新验证并刷新主界面布局
                    gamePanel.requestFocusInWindow(); // 把系统的焦点交给游戏，方便接收操作
                    return true; // 拦截成功，不再向下传递事件
                }
                // 如果按了 ESC 键，且游戏当前正在显示
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE && gamePanel.isVisible()) {
                    gamePanel.setVisible(false); // 隐藏小恐龙游戏
                    menuPanel.revalidate(); // 重新验证并恢复布局
                    return true; // 拦截成功
                }
            }
            return false; // 不是上述按键，放行事件
        });

        menuPanel.add(topBar, BorderLayout.NORTH); // 将顶部齿轮设置栏贴在北方(上方)
        menuPanel.add(topArea, BorderLayout.CENTER); // 将按钮网格区贴在中央
        menuPanel.add(gamePanel, BorderLayout.SOUTH); // 将彩蛋游戏面板贴在南方(下方)
        return menuPanel; // 返回装配好的主菜单面板
    }

    // 负责制造带有毛玻璃、阴影效果的功能按钮的工程方法
    private JButton createModuleButton(String text, String cardName, Color themeColor) {
        // 1. 直接召唤融合了所有特效的神兵
        RippleButton btn = new RippleButton(text, themeColor);

        // 2. 保持你原本的大气尺寸 270x110
        btn.setPreferredSize(new Dimension(270, 110));

        // 3. 绑定原有的卡片切换逻辑
        btn.addActionListener(e -> switchPanel(cardName));

        return btn;
    }

    public void showMenu() { // 提供给别的界面的接口，方便他们一键返回主菜单
        switchPanel("MENU"); // 切换卡片到主菜单
    }

    // ====== Java 程序的绝对入口，第一声枪响的地方 ======
    public static void main(String[] args) {

        // 1. 第一时间初始化 FlatLaf 默认原生主题外观，给后面的拦截弹窗先穿上酷衣服
        FlatDarkLaf.setup();


        try { // 开启异常捕获，准备执行文件锁的 IO 逻辑
            // 在系统的临时目录获取一个属于当前程序的锁文件
            File lockFile = new File(System.getProperty("java.io.tmpdir"), "daili_assistant.lock");
            raf = new RandomAccessFile(lockFile, "rw"); // 用读写模式访问这个文件
            channel = raf.getChannel(); // 拿到文件的底层通道
            lock = channel.tryLock(); // 尝试申请这把锁！如果没别人用就能拿到，别人在用就会返回 null

            if (lock == null) { // 如果拿不到锁，说明程序已经被打开过一次了，触发【防多开拦截机制】

                // 🌟 核心点：因为只拦截不进主程序，所以定制样式配置专门塞在这里，不污染别处
                UIManager.put("OptionPane.messageForeground", Color.WHITE); // 文字变白
                UIManager.put("OptionPane.border", BorderFactory.createCompoundBorder( // 组合边框修复
                        BorderFactory.createLineBorder(new Color(80, 80, 80), 1), // 外圈深灰一像素线
                        BorderFactory.createEmptyBorder(15, 20, 15, 20) // 内圈加宽距离防拥挤
                ));

                Color btnBg = new Color(60, 63, 65); // 调配高级灰按钮色
                UIManager.put("Button.background", btnBg); // 普通按钮底色
                UIManager.put("Button.foreground", Color.WHITE); // 普通按钮文字

                // 重点对付 JOptionPane 那个默认顽固蓝色高亮的 Default 按钮
                UIManager.put("Button.default.background", btnBg);
                UIManager.put("Button.default.foreground", Color.WHITE);
                UIManager.put("Button.default.borderColor", new Color(80, 80, 80));
                UIManager.put("Button.default.focusedBorderColor", new Color(100, 100, 100));
                UIManager.put("Button.default.hoverBackground", new Color(75, 78, 81));

                // 自己手捏一个无边框的对话框来当提示
                JDialog dialog = new JDialog((Frame)null, "提示", true); // 指定模态
                dialog.setUndecorated(true); // 去掉 Windows 默认标题栏
                dialog.setBackground(new Color(32, 33, 38, 255)); // 强制窗口底色为深色

                JPanel content = new JPanel(); // 捏一个面板承载内容
                content.setBackground(new Color(32, 33, 38)); // 设背景
                content.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 80), 1)); // 加极细边框
                content.setLayout(new BorderLayout(15, 15)); // 设置布局并拉开间距

                // 创建居中的警告提示语
                JLabel msg = new JLabel("  Toolkit is already running.  ", SwingConstants.CENTER);
                msg.setForeground(Color.WHITE); // 字变白
                msg.setFont(new Font("微软雅黑", Font.PLAIN, 14)); // 字体微调
                msg.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // 让文字四周喘口气

                JButton btn = new JButton("ok"); // 做一个确定按钮
                btn.setFocusPainted(false); // 去除焦点框
                btn.addActionListener(e -> System.exit(0)); // 绑上点击直接击杀进程的事件

                content.add(msg, BorderLayout.CENTER); // 把文字放中间
                content.add(btn, BorderLayout.SOUTH); // 把按钮放底下

                dialog.add(content); // 面板贴回弹窗
                dialog.pack(); // 让弹窗自动根据内容收缩成恰当大小
                dialog.setLocationRelativeTo(null); // 在屏幕中间出现
                dialog.setVisible(true); // 阻塞式展示！程序会一直停在这里等用户点确定

                System.exit(0); // 为防万一，再补一枪关闭进程
            }
        } catch (Exception e) { // 如果上面读写文件锁抛出异常
            e.printStackTrace(); // 打印出来看看到底是啥毛病
        }

        // ==========================================
        // 走到这里，说明是【第一次正常打开程序】，开始展示加载动画
        // ==========================================

        JWindow splashScreen = new JWindow(); // 建立一个轻量级透明窗体当启动页 (Splash)
        splashScreen.setBackground(new Color(0, 0, 0, 0)); // 将背景颜色透明度设为 0（绝对透明）

        // 通过类加载器获取您的 GIF 闪图资源路径
        java.net.URL gifUrl = MainLauncher.class.getClassLoader().getResource("loading.gif");

        if (gifUrl != null) { // 如果成功读到了动图
            Image image = Toolkit.getDefaultToolkit().createImage(gifUrl); // 利用底层绘图工具将其载入成图片对象
            ImageFilter filter = new RGBImageFilter() { // 利用 RGB 滤镜系统来扣掉背景

                public final int filterRGB(int x, int y, int rgb) { // 逐像素遍历计算
                    // 检查如果像素是白色（0xFFFFFFFF）
                    if ((rgb | 0xFF000000) == 0xFFFFFFFF) {
                        return 0x00FFFFFF & rgb; // 将白色的 Alpha 通道设为全透明
                    }
                    return rgb; // 非白色原样返回
                }
            };
            // 生成经过滤镜处理后的透明新图片
            Image transparentGif = Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(image.getSource(), filter));
            // 把这层透明图片包裹在一张标签上
            JLabel gifLabel = new JLabel(new ImageIcon(transparentGif));
            // 把这层标签塞进刚才建好的启动页里
            splashScreen.getContentPane().add(gifLabel);
        }

        splashScreen.pack(); // 收紧启动页宽高使其恰好包住 GIF 图
        splashScreen.setLocationRelativeTo(null); // 居中闪图位置
        splashScreen.setVisible(true); // 大喊一声：闪图！给我亮个相！

        // ========================================================
        // 🌟 修复 "闪图不闪了" 的核心药方：新开独立倒计时线程，释放 UI 绘制权
        // ========================================================
        new Thread(() -> { // 开启一个独立的后台工人线程
            try {
                Thread.sleep(2000); // 让这个工人直接睡 2000 毫秒 (2秒)，这期间界面主线程很闲，就会尽情播放您的 GIF
            } catch (Exception e) {
                // 睡觉被打断也不管
            }

            // 睡醒之后，通知 UI 主线程（EDT）可以开始干活了
            SwingUtilities.invokeLater(() -> {
                // 实例化沉重的主启动器窗口 (此时因为有衣服了，里面全是好看的原生扁平风组件)
                MainLauncher launcher = new MainLauncher();
                // 过河拆桥，把刚才播放完毕的启动页关掉销毁
                splashScreen.dispose();
                // 掀开主界面的盖头，正式开始使用
                launcher.setVisible(true);
            });
        }).start(); // 狠狠在工人屁股上踢一脚，让独立线程跑起来！
    }
}