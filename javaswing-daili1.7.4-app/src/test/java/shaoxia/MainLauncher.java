package shaoxia;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.RGBImageFilter;
import java.util.prefs.Preferences;

/** 主页面板块 MainLauncher */
public class MainLauncher extends JFrame {
    private BlueCat blueCat;
    private CardLayout cardLayout;
    private JPanel mainContainer;
    public static final Font MAIN_FONT = new Font("微软雅黑", Font.PLAIN, 16);
    public static final Font BOLD_FONT = new Font("微软雅黑", Font.BOLD, 18);

    public MainLauncher() {
        setTitle("");

        // 读取本地记忆的窗口尺寸，默认 960x720
        Preferences prefs = Preferences.userNodeForPackage(MainLauncher.class);
        int winWidth = prefs.getInt("window_width", 960);
        int winHeight = prefs.getInt("window_height", 720);
        setSize(winWidth, winHeight);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        Image appIcon = null;
        try {
            java.net.URL iconUrl = getClass().getResource("/orange.png");
            if (iconUrl != null) {
                appIcon = new ImageIcon(iconUrl).getImage();
                setIconImage(appIcon);
            } else {
                System.err.println("没找到图片路径：/orange.png");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        initSystemTray(appIcon);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);
        add(mainContainer);
//板块
        mainContainer.add(createMainMenu(), "MENU");
        mainContainer.add(new RenamerPanel(this), "RENAMER");
        mainContainer.add(new CheckerPanel(this), "CHECKER");
        mainContainer.add(new FinderPanel(this), "FINDER");
        mainContainer.add(new ScoringPanel(this), "SCORING");
        mainContainer.add(new ReplacerPanel(this), "REPLACER");
        mainContainer.add(new SoftwarePanel(this), "SOFTWARE");
        mainContainer.add(new ComparisonPanel(this), "COMPARISON");
        mainContainer.add(new PyHelperPanel(this), "PY_HELPER");

        cardLayout.show(mainContainer, "MENU");

        // === 【召唤蓝猫】 ===
        blueCat = new BlueCat(this);
        setGlassPane(blueCat);
        blueCat.setCatVisible(true);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                blueCat.updateBounds();
            }
        });
    }

    private void initSystemTray(Image iconImage) {
        if (!SystemTray.isSupported()) {
            System.err.println("当前系统不支持系统托盘功能，回退为常规关闭模式。");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            return;
        }

        if (iconImage == null) {
            iconImage = new ImageIcon(new byte[0]).getImage();
        }

        SystemTray tray = SystemTray.getSystemTray();
        PopupMenu popupMenu = new PopupMenu();

        MenuItem showItem = new MenuItem("OPEN APPLICATION");
        showItem.addActionListener(e -> {
            setVisible(true);
            setExtendedState(JFrame.NORMAL);
            toFront();
        });

        MenuItem exitItem = new MenuItem("EXIT");
        exitItem.addActionListener(e -> {
            System.exit(0);
        });

        popupMenu.add(showItem);
        popupMenu.addSeparator();
        popupMenu.add(exitItem);

        TrayIcon trayIcon = new TrayIcon(iconImage, "代理助手 - 后台运行中", popupMenu);
        trayIcon.setImageAutoSize(true);

        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    setVisible(true);
                    setExtendedState(JFrame.NORMAL);
                    toFront();
                }
            }
        });

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.err.println("系统托盘图标添加失败！");
        }
    }

    public void switchPanel(String cardName) {
        cardLayout.show(mainContainer, cardName);

        if (blueCat != null) {
            if ("MENU".equals(cardName)) {
                blueCat.setCatVisible(true);
            } else {
                blueCat.setCatVisible(false);
            }
        }
    }

    /**
     * 纯代码绘制的抗锯齿齿轮按钮
     */
    class GearButton extends JButton {
        public GearButton() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setToolTipText("系统设置");
            setPreferredSize(new Dimension(45, 45));
        }


        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            Color color = getModel().isRollover()
                    ? Color.WHITE
                    : new Color(200, 200, 200, 180);

            g2.setColor(color);
            g2.setStroke(new BasicStroke(
                    2f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));

            // ====================
            // 鱼尾
            // ====================
            Polygon tail = new Polygon();
            tail.addPoint(cx - 16, cy);
            tail.addPoint(cx - 24, cy - 7);
            tail.addPoint(cx - 24, cy + 7);

            g2.drawPolygon(tail);

            // ====================
            // 主骨
            // ====================
            g2.drawLine(
                    cx - 16,
                    cy,
                    cx + 10,
                    cy);

            // ====================
            // 鱼刺
            // ====================
            for (int x = cx - 8; x <= cx + 4; x += 6) {

                g2.drawLine(
                        x,
                        cy,
                        x - 4,
                        cy - 5);

                g2.drawLine(
                        x,
                        cy,
                        x - 4,
                        cy + 5);
            }

            // ====================
            // 鱼头
            // ====================
            g2.drawOval(
                    cx + 8,
                    cy - 7,
                    14,
                    14);

            // 眼睛
            g2.fillOval(
                    cx + 15,
                    cy - 1,
                    2,
                    2);

            g2.dispose();
        }
    }




    private JPanel createMainMenu() {
        BackgroundPanel menuPanel = new BackgroundPanel("texture2.png");
        menuPanel.setLayout(new BorderLayout());

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        topBar.setOpaque(false);

        GearButton settingsBtn = new GearButton();
        // ✨ 这里直接调用我们分离出去的新类方法！
        settingsBtn.addActionListener(e -> SettingsDialog.showDialog(MainLauncher.this));
        topBar.add(settingsBtn);

        JPanel topArea = new JPanel(new GridBagLayout());
        topArea.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel title = new JLabel("代理助手");
        title.setFont(new Font("幼圆", Font.BOLD, 56));
        title.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.insets = new Insets(10, 0, 40, 0);
        gbc.weighty = 0;
        topArea.add(title, gbc);

        JButton btn1 = createModuleButton("全能改名助手", "RENAMER", new Color(41, 128, 185));
        JButton btn2 = createModuleButton("数据校对助手", "CHECKER", new Color(39, 174, 96));
        JButton btn3 = createModuleButton("文件检索助手", "FINDER", new Color(142, 68, 173));
        JButton btn4 = createModuleButton("评分生成助手", "SCORING", new Color(211, 84, 0));
        JButton btn5 = createModuleButton("文档替换助手", "REPLACER", new Color(255, 215, 0));
        JButton btn6 = createModuleButton("软件下载助手", "SOFTWARE", new Color(192, 57, 43));
        JButton btn7 = createModuleButton("公告比对助手", "COMPARISON", new Color(44, 62, 80));
        JButton btn8 = createModuleButton("派森脚本助手", "PY_HELPER", new Color(212, 185, 182));
        JButton btn9 = createModuleButton("暂时关闭小猫", "DEV", new Color(127, 140, 141));

        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 10, 10, 10);

        gbc.gridy = 1;
        gbc.weightx = 1.0;   gbc.gridx = 0; topArea.add(btn1, gbc);
        gbc.weightx = 0.618; gbc.gridx = 1; topArea.add(btn2, gbc);
        gbc.weightx = 1.0;   gbc.gridx = 2; topArea.add(btn3, gbc);

        gbc.gridy = 2;
        gbc.weightx = 1.0;   gbc.gridx = 0; topArea.add(btn4, gbc);
        gbc.weightx = 0.618; gbc.gridx = 1; topArea.add(btn5, gbc);
        gbc.weightx = 1.0;   gbc.gridx = 2; topArea.add(btn6, gbc);

        gbc.gridy = 3;
        gbc.weightx = 1.0;   gbc.gridx = 0; topArea.add(btn7, gbc);
        gbc.weightx = 0.618; gbc.gridx = 1; topArea.add(btn8, gbc);
        gbc.weightx = 1.0;   gbc.gridx = 2; topArea.add(btn9, gbc);

        gbc.gridy = 4;
        gbc.gridwidth = 3;
        gbc.weighty = 1.0;
        topArea.add(Box.createVerticalGlue(), gbc);

        DinoGamePanel gamePanel = new DinoGamePanel();
        gamePanel.setVisible(false);
        gamePanel.setPreferredSize(new Dimension(800, 260));

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE && !gamePanel.isVisible()) {
                    gamePanel.setVisible(true);
                    menuPanel.revalidate();
                    gamePanel.requestFocusInWindow();
                    return true;
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE && gamePanel.isVisible()) {
                    gamePanel.setVisible(false);
                    menuPanel.revalidate();
                    return true;
                }
            }
            return false;
        });

        menuPanel.add(topBar, BorderLayout.NORTH);
        menuPanel.add(topArea, BorderLayout.CENTER);
        menuPanel.add(gamePanel, BorderLayout.SOUTH);
        return menuPanel;
    }

    private JButton createModuleButton(String text, String cardName, Color themeColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean isHover = getModel().isRollover();
                // 1. 获取按压状态
                boolean isPressed = getModel().isPressed();

                // 2. 只有按下去的时候颜色加深，模拟受力状态
                int alpha = isPressed ? 220 : (isHover ? 200 : 150);

                // 3. 产生物理下凹感：按下时向右下偏移 2 个像素
                int offset = isPressed ? 2 : 0;

                g2.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), alpha));
                g2.fillRoundRect(offset, offset, getWidth() - offset * 2, getHeight() - offset * 2, 25, 25);
                g2.setStroke(new BasicStroke(1.8f));
                g2.setColor(new Color(255, 255, 255, isHover ? 120 : 70));
                g2.drawRoundRect(offset + 1, offset + 1, getWidth() - 3 - offset * 2, getHeight() - 3 - offset * 2, 25, 25);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;

                // 文字也跟着偏移
                g2.setColor(new Color(0, 0, 0, 60));
                g2.drawString(getText(), x + 1, y + 1 + offset);
                g2.setColor(Color.WHITE);
                g2.drawString(getText(), x, y + offset);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(270, 110));
        btn.setFont(new Font("微软雅黑", Font.BOLD, 20));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 【关键点 1】改用标准的 ActionListener，它自带“必须在按钮内松开鼠标才执行”的标准交互逻辑
        btn.addActionListener(e -> switchPanel(cardName));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { btn.setFont(new Font("微软雅黑", Font.BOLD, 22)); }
            @Override
            public void mouseExited(MouseEvent e) { btn.setFont(new Font("微软雅黑", Font.BOLD, 20)); }
            // 【关键点 2】删除了原来的 mousePressed，防止点下瞬间立刻跳转
        });
        return btn;
    }

    public void showMenu() {
        switchPanel("MENU");
    }

    public static void main(String[] args) {
        // 极致精简后的透明闪图窗口逻辑
        JWindow splashScreen = new JWindow();
        splashScreen.setBackground(new Color(0, 0, 0, 0));

        java.net.URL gifUrl = MainLauncher.class.getClassLoader().getResource("loading.gif");

        if (gifUrl != null) {
            Image image = Toolkit.getDefaultToolkit().createImage(gifUrl);
            ImageFilter filter = new RGBImageFilter() {
                public final int filterRGB(int x, int y, int rgb) {
                    if ((rgb | 0xFF000000) == 0xFFFFFFFF) {
                        return 0x00FFFFFF & rgb;
                    }
                    return rgb;
                }
            };
            Image transparentGif = Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(image.getSource(), filter));
            JLabel gifLabel = new JLabel(new ImageIcon(transparentGif));
            splashScreen.getContentPane().add(gifLabel);
        }

        splashScreen.pack();
        splashScreen.setLocationRelativeTo(null);
        splashScreen.setVisible(true);

        // 启动主程序线程
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (Exception e) {}

            SwingUtilities.invokeLater(() -> {
                // === 第一步：必须开启 FlatLaf 的自定义窗口装饰 ===
                JFrame.setDefaultLookAndFeelDecorated(true);

                // === 第二步：设置标题栏背景和分割线为透明色 ===
                UIManager.put("TitlePane.background", new Color(0, 0, 0, 0));
                UIManager.put("TitlePane.borderColor", new Color(0, 0, 0, 0));

                FlatDarkLaf.setup();
                UIManager.put("defaultFont", new Font("微软雅黑", Font.PLAIN, 14));

                MainLauncher launcher = new MainLauncher();

                // === 第三步：告诉 FlatLaf 停止绘制默认的标题栏背景 ===
                launcher.getRootPane().putClientProperty("JRootPane.titleBarShowBackground", false);



                // 【注意：这里已经删除了 launcher.setBackground(...) 解决报错】

                splashScreen.dispose(); // 关闭闪图
                launcher.setVisible(true); // 显示主界面
            });
        }).start();
    }
}