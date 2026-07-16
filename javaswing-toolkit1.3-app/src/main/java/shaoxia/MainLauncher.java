package shaoxia;

import com.formdev.flatlaf.FlatDarkLaf;
import shaoxia.Utils.RippleButton;
import shaoxia.modules.*;
import shaoxia.ui.components.BlueCat;
import shaoxia.ui.components.ModernTrayMenu;
import shaoxia.ui.components.SettingsDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.RGBImageFilter;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.prefs.Preferences;

/**
 * 主页面板块 MainLauncher
 * （终极顺滑圆角 + 极致抗锯齿防模糊 + 根治猫咪图层撕裂闪烁版）
 */
public class MainLauncher extends JFrame {


    private BlueCat blueCat;
    private CardLayout cardLayout;
    private JPanel mainContainer;
    private static RandomAccessFile raf;
    private static FileChannel channel;
    private static FileLock lock;

    public static final Font MAIN_FONT = new Font("微软雅黑", Font.PLAIN, 16);
    public static final Font BOLD_FONT = new Font("微软雅黑", Font.BOLD, 18);

    public MainLauncher() {
        setTitle("");

        // === 【防锯齿核心 1：让系统级窗口彻底隐身】 ===
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        // ===========================================

        Preferences prefs = Preferences.userNodeForPackage(MainLauncher.class);
        int winWidth = prefs.getInt("window_width", 640);
        int winHeight = prefs.getInt("window_height", 520);
        setSize(winWidth, winHeight);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        Image appIcon = null;
        try {
            java.net.URL iconUrl = getClass().getResource("/app.png");
            if (iconUrl != null) {
                appIcon = new ImageIcon(iconUrl).getImage();
                setIconImage(appIcon);
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

        mainContainer = new JPanel(cardLayout) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth();
                int h = getHeight();
                Shape clipShape = new java.awt.geom.RoundRectangle2D.Double(0, 0, w, h, 25, 25);

                g2.setColor(UIManager.getColor("Panel.background"));
                g2.fill(clipShape);

                // 【核心防闪烁修复】：改用 .clip() 取交集
                g2.clip(clipShape);

                super.paint(g2);
                g2.dispose();
            }
        };
        mainContainer.setOpaque(false);
        add(mainContainer);

        mainContainer.add(createMainMenu(), "MENU");
        mainContainer.add(new RenamerPanel(this), "RENAMER");
        mainContainer.add(new CheckerPanel(this), "CHECKER");
        mainContainer.add(new FinderPanel(this), "FINDER");
        mainContainer.add(new ScoringPanel(this), "SCORING");
        mainContainer.add(new ReplacerPanel(this), "REPLACER");
        mainContainer.add(new ComparisonPanel(this), "COMPARISON");
        mainContainer.add(new RegionFetcherPanel(this), "REGION");

        cardLayout.show(mainContainer, "MENU");

        // === 【核心修复：彻底消灭闪烁的层级替换术】 ===
        blueCat = new BlueCat(this);
        // 不再使用充满 Bug 的 GlassPane，将其强行拉入与按钮平级的 LayeredPane 中的最高层！
        getLayeredPane().add(blueCat, JLayeredPane.POPUP_LAYER);
        // 读取记忆，决定开局是否召唤小猫

        blueCat.setCatVisible(prefs.getBoolean("cat_enabled", true));
        // ============================================

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (blueCat != null) {
                    blueCat.updateBounds();
                }
            }
        });

        // === 【窗口拖拽移动与边缘放大缩小】 ===
        MouseAdapter dragAndResizeListener = new MouseAdapter() {
            private final int BORDER_THICKNESS = 10;
            private int cursorType = Cursor.DEFAULT_CURSOR;
            private Point startPos = null;
            private Rectangle startBounds = null;

            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                int w = getWidth();
                int h = getHeight();

                boolean left = p.x < BORDER_THICKNESS;
                boolean right = p.x > w - BORDER_THICKNESS;
                boolean top = p.y < BORDER_THICKNESS;
                boolean bottom = p.y > h - BORDER_THICKNESS;

                if (top && left) cursorType = Cursor.NW_RESIZE_CURSOR;
                else if (top && right) cursorType = Cursor.NE_RESIZE_CURSOR;
                else if (bottom && left) cursorType = Cursor.SW_RESIZE_CURSOR;
                else if (bottom && right) cursorType = Cursor.SE_RESIZE_CURSOR;
                else if (top) cursorType = Cursor.N_RESIZE_CURSOR;
                else if (bottom) cursorType = Cursor.S_RESIZE_CURSOR;
                else if (left) cursorType = Cursor.W_RESIZE_CURSOR;
                else if (right) cursorType = Cursor.E_RESIZE_CURSOR;
                else cursorType = Cursor.DEFAULT_CURSOR;

                setCursor(Cursor.getPredefinedCursor(cursorType));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                startPos = e.getLocationOnScreen();
                startBounds = getBounds();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (startPos == null || startBounds == null) return;
                Point p = e.getLocationOnScreen();
                int dx = p.x - startPos.x;
                int dy = p.y - startPos.y;

                if (cursorType == Cursor.DEFAULT_CURSOR) {
                    setLocation(startBounds.x + dx, startBounds.y + dy);
                } else {
                    Rectangle bounds = new Rectangle(startBounds);

                    if (cursorType == Cursor.E_RESIZE_CURSOR || cursorType == Cursor.NE_RESIZE_CURSOR || cursorType == Cursor.SE_RESIZE_CURSOR) {
                        bounds.width += dx;
                    }
                    if (cursorType == Cursor.S_RESIZE_CURSOR || cursorType == Cursor.SW_RESIZE_CURSOR || cursorType == Cursor.SE_RESIZE_CURSOR) {
                        bounds.height += dy;
                    }
                    if (cursorType == Cursor.W_RESIZE_CURSOR || cursorType == Cursor.NW_RESIZE_CURSOR || cursorType == Cursor.SW_RESIZE_CURSOR) {
                        bounds.width -= dx;
                        bounds.x += dx;
                    }
                    if (cursorType == Cursor.N_RESIZE_CURSOR || cursorType == Cursor.NW_RESIZE_CURSOR || cursorType == Cursor.NE_RESIZE_CURSOR) {
                        bounds.height -= dy;
                        bounds.y += dy;
                    }

                    int minWidth = 640;
                    int minHeight = 520;

                    if (bounds.width < minWidth) {
                        bounds.width = minWidth;
                        if (cursorType == Cursor.W_RESIZE_CURSOR || cursorType == Cursor.NW_RESIZE_CURSOR || cursorType == Cursor.SW_RESIZE_CURSOR) {
                            bounds.x = startBounds.x + startBounds.width - minWidth;
                        }
                    }
                    if (bounds.height < minHeight) {
                        bounds.height = minHeight;
                        if (cursorType == Cursor.N_RESIZE_CURSOR || cursorType == Cursor.NW_RESIZE_CURSOR || cursorType == Cursor.NE_RESIZE_CURSOR) {
                            bounds.y = startBounds.y + startBounds.height - minHeight;
                        }
                    }

                    setBounds(bounds);
                }
            }
        };

        mainContainer.addMouseListener(dragAndResizeListener);
        mainContainer.addMouseMotionListener(dragAndResizeListener);
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
        ModernTrayMenu modernMenu = new ModernTrayMenu(this, this);
        TrayIcon trayIcon = new TrayIcon(iconImage, "Toolkit - Running in Background");
        trayIcon.setImageAutoSize(true);

        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    Point mousePos = MouseInfo.getPointerInfo().getLocation();
                    int x = mousePos.x - modernMenu.getWidth() / 2;
                    int y = mousePos.y - modernMenu.getHeight() - 5;

                    Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                    if (x + modernMenu.getWidth() > screenBounds.x + screenBounds.width) {
                        x = screenBounds.x + screenBounds.width - modernMenu.getWidth() - 5;
                    }
                    if (x < screenBounds.x) x = screenBounds.x + 5;
                    if (y + modernMenu.getHeight() > screenBounds.y + screenBounds.height) {
                        y = screenBounds.y + screenBounds.height - modernMenu.getHeight() - 5;
                    }
                    if (y < screenBounds.y) {
                        y = mousePos.y + 20;
                    }

                    modernMenu.setLocation(x, y);
                    modernMenu.setVisible(true);
                    modernMenu.requestFocus();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    modernMenu.setVisible(false);
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
            // 实时读取记忆状态
            Preferences prefs = Preferences.userNodeForPackage(MainLauncher.class);
            boolean isCatEnabled = prefs.getBoolean("cat_enabled", true);

            // 只有当回到主菜单，且设置里开启了小猫时，才显示小猫
            if ("MENU".equals(cardName) && isCatEnabled) {
                blueCat.setCatVisible(true);
            } else {
                blueCat.setCatVisible(false);
            }
        }
    }

    class GearButton extends JButton {
        public GearButton() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setToolTipText("Settings");
            setPreferredSize(new Dimension(45, 45));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            int radius = 11;

            boolean isHover = getModel().isRollover();

            g2.setColor(isHover ? new Color(130, 135, 145) : new Color(100, 105, 115));
            g2.fillRoundRect(cx - radius / 2, cy + radius + 7, radius, 4, 3, 3);
            g2.fillRect(cx - 2, cy + radius + 3, 4, 5);

            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(isHover ? Color.WHITE : new Color(170, 175, 185));
            g2.drawArc(cx - radius - 4, cy - radius - 4, (radius + 4) * 2, (radius + 4) * 2, -70, 180);

            g2.setColor(isHover ? new Color(52, 152, 219) : new Color(41, 128, 185));
            g2.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

            g2.setClip(new java.awt.geom.Ellipse2D.Float(cx - radius, cy - radius, radius * 2, radius * 2));
            g2.setColor(isHover ? new Color(46, 204, 113) : new Color(39, 174, 96));
            g2.fillOval(cx - radius / 2, cy - radius / 3, radius + 5, radius - 2);
            g2.fillOval(cx + radius / 5, cy + radius / 8, radius / 2, radius + 5);
            g2.fillOval(cx - radius + 2, cy + radius / 2, radius + 5, radius / 2);

            g2.setColor(new Color(255, 255, 255, isHover ? 70 : 40));
            g2.setStroke(new BasicStroke(0.8f));
            g2.drawOval(cx - radius, cy - radius / 2, radius * 2, radius);
            g2.drawOval(cx - radius, cy - radius / 5, radius * 2, radius * 2 / 5);
            g2.drawOval(cx - radius, cy + radius / 5, radius * 2, radius);
            g2.drawOval(cx - radius / 2, cy - radius, radius, radius * 2);
            g2.drawOval(cx - radius / 5, cy - radius, radius * 2 / 5, radius * 2);
            g2.drawOval(cx + radius / 5, cy - radius, radius, radius * 2);

            g2.setClip(null);
            g2.setColor(new Color(255, 255, 255, isHover ? 130 : 80));
            g2.fillOval(cx - radius / 2, cy - radius + 2, radius, radius / 3);

            g2.setColor(isHover ? Color.WHITE : new Color(170, 175, 185));
            int axisX = (int) (Math.cos(Math.toRadians(-70)) * (radius + 4));
            int axisY = (int) (Math.sin(Math.toRadians(-70)) * (radius + 4));
            g2.fillOval(cx + axisX - 2, cy + axisY - 2, 4, 4);
            g2.fillOval(cx - axisX - 2, cy - axisY - 2, 4, 4);

            g2.dispose();
        }
    }

    private JPanel createMainMenu() {
        BackgroundPanel menuPanel = new BackgroundPanel("bg_imgs/月球与地球.png");
        menuPanel.setLayout(new BorderLayout());

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        topBar.setOpaque(false);

        GearButton settingsBtn = new GearButton();
        settingsBtn.addActionListener(e -> SettingsDialog.showDialog(MainLauncher.this));
        topBar.add(settingsBtn);

        JPanel topArea = new JPanel(new GridBagLayout());
        topArea.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel title = new JLabel("Toolkit");
        title.setFont(new Font("Montserrat", Font.BOLD, 56));
        title.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.insets = new Insets(10, 0, 40, 0);
        gbc.weighty = 0;
        topArea.add(title, gbc);

        Color glassColor = new Color(30, 35, 40, 120);

        JButton btn1 = createModuleButton("RenamerPanel", "RENAMER", glassColor);
        JButton btn2 = createModuleButton("CheckerPanel", "CHECKER", glassColor);
        JButton btn3 = createModuleButton("FinderPanel", "FINDER", glassColor);
        JButton btn4 = createModuleButton("ScoringPanel", "SCORING", glassColor);
        JButton btn5 = createModuleButton("ReplacerPanel", "REPLACER", glassColor);
        JButton btn6 = createModuleButton("ComparisonPanel", "COMPARISON", glassColor);
        JButton btn7 = createModuleButton("RegionFetcherPanel", "REGION", glassColor);
        JButton btn8 = createModuleButton("OasisPanel", "DEV", glassColor);
        JButton btn9 = createModuleButton("HavenPanel", "DEV", glassColor);

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
        gamePanel.setPreferredSize(new Dimension(800, 320));

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
        RippleButton btn = new RippleButton(text, themeColor);
        btn.setPreferredSize(new Dimension(270, 110));
        btn.addActionListener(e -> switchPanel(cardName));
        return btn;
    }

    public void showMenu() {
        switchPanel("MENU");
    }

    public static void main(String[] args) {

        System.setProperty("sun.java2d.d3d", "false");
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("sun.java2d.noddraw", "true");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        FlatDarkLaf.setup();

        try {
            File lockFile = new File(System.getProperty("java.io.tmpdir"), "daili_assistant.lock");
            raf = new RandomAccessFile(lockFile, "rw");
            channel = raf.getChannel();
            lock = channel.tryLock();

            if (lock == null) {
                UIManager.put("OptionPane.messageForeground", Color.WHITE);
                UIManager.put("OptionPane.border", BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(80, 80, 80), 1),
                        BorderFactory.createEmptyBorder(15, 20, 15, 20)
                ));

                Color btnBg = new Color(60, 63, 65);
                UIManager.put("Button.background", btnBg);
                UIManager.put("Button.foreground", Color.WHITE);

                UIManager.put("Button.default.background", btnBg);
                UIManager.put("Button.default.foreground", Color.WHITE);
                UIManager.put("Button.default.borderColor", new Color(80, 80, 80));
                UIManager.put("Button.default.focusedBorderColor", new Color(100, 100, 100));
                UIManager.put("Button.default.hoverBackground", new Color(75, 78, 81));

                JDialog dialog = new JDialog((Frame)null, "提示", true);
                dialog.setUndecorated(true);
                dialog.setBackground(new Color(32, 33, 38, 255));

                JPanel content = new JPanel();
                content.setBackground(new Color(32, 33, 38));
                content.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 80), 1));
                content.setLayout(new BorderLayout(15, 15));

                JLabel msg = new JLabel("  Toolkit is already running.  ", SwingConstants.CENTER);
                msg.setForeground(Color.WHITE);
                msg.setFont(new Font("微软雅黑", Font.PLAIN, 14));
                msg.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

                JButton btn = new JButton("ok");
                btn.setFocusPainted(false);
                btn.addActionListener(e -> System.exit(0));

                content.add(msg, BorderLayout.CENTER);
                content.add(btn, BorderLayout.SOUTH);

                dialog.add(content);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);

                System.exit(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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

        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
            }

            SwingUtilities.invokeLater(() -> {
                MainLauncher launcher = new MainLauncher();
                splashScreen.dispose();
                launcher.setVisible(true);
            });
        }).start();
    }
}