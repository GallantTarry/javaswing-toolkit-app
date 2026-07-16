package shaoxia;

import com.formdev.flatlaf.FlatDarkLaf;
import shaoxia.Utils.RippleButton;
import shaoxia.modules.*;

import shaoxia.modules.RenamerPanel;
import shaoxia.ui.components.BlueCat;
import shaoxia.ui.components.ModernTrayMenu;
import shaoxia.ui.components.SettingsDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
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
 * （终极顺滑圆角 + 极致抗锯齿防模糊 + 根治猫咪图层撕裂闪烁 + 全局悬浮关闭版）
 * ✨ 本版特性：支持选择直接退出或动画托盘，彻底消除图标锯齿，引入硬件级高清重采样
 */
public class MainLauncher extends JFrame {

    private BlueCat blueCat;
    private SavePigPanel gamePanel;
    private CardLayout cardLayout;
    private JPanel mainContainer;
    private CloseButton closeButton;
    private static RandomAccessFile raf;
    private static FileChannel channel;
    private static FileLock lock;

    public static final Font MAIN_FONT = new Font("微软雅黑", Font.PLAIN, 16);
    public static final Font BOLD_FONT = new Font("微软雅黑", Font.BOLD, 18);

    public MainLauncher() {
        setTitle("");

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

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
                // ✨ 读取关闭设置：1 代表直接退出，0 代表动画托盘
                Preferences prefs = Preferences.userNodeForPackage(MainLauncher.class);
                if (prefs.getInt("close_action", 0) == 1) {
                    System.exit(0);
                } else {
                    animateToTray();
                }
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
        mainContainer.add(new ConverterPanel(this), "CONVERTER");
        mainContainer.add(new SecretChatPanel(this), "SECRET");

        cardLayout.show(mainContainer, "MENU");

        blueCat = new BlueCat(this);
        getLayeredPane().add(blueCat, JLayeredPane.POPUP_LAYER);
        blueCat.setCatVisible(prefs.getBoolean("cat_enabled", true));

        closeButton = new CloseButton();
        closeButton.setBounds(winWidth - 45, 15, 30, 30);
        closeButton.addActionListener(e -> {
            // ✨ 读取关闭设置：1 代表直接退出，0 代表动画托盘
            Preferences localPrefs = Preferences.userNodeForPackage(MainLauncher.class);
            if (localPrefs.getInt("close_action", 0) == 1) {
                System.exit(0);
            } else {
                animateToTray();
            }
        });
        getLayeredPane().add(closeButton, Integer.valueOf(JLayeredPane.POPUP_LAYER + 1));

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (blueCat != null) {
                    blueCat.updateBounds();
                }
                if (closeButton != null) {
                    closeButton.setBounds(getWidth() - 45, 15, 30, 30);
                }
            }
        });

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

    private void animateToTray() {
        Rectangle startBounds = getBounds();

        final int totalFrames = 15;
        Timer timer = new Timer(10, null);

        timer.addActionListener(new ActionListener() {
            int currentFrame = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                currentFrame++;
                float progress = (float) currentFrame / totalFrames;

                float easeOut = 1.0f - (1.0f - progress) * (1.0f - progress);

                float newOpacity = 1.0f - easeOut;
                setOpacity(Math.max(0.0f, newOpacity));

                float scale = 1.0f - (0.1f * easeOut);
                int newW = (int) (startBounds.width * scale);
                int newH = (int) (startBounds.height * scale);

                int newX = startBounds.x + (startBounds.width - newW) / 2;
                int newY = startBounds.y + (startBounds.height - newH) / 2;

                setBounds(newX, newY, Math.max(1, newW), Math.max(1, newH));

                if (currentFrame >= totalFrames) {
                    timer.stop();
                    setVisible(false);
                    setBounds(startBounds);
                    setOpacity(1.0f);
                }
            }
        });
        timer.start();
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
        TrayIcon trayIcon = new TrayIcon(iconImage, "Toolkit - 后台运行中");
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
            Preferences prefs = Preferences.userNodeForPackage(MainLauncher.class);
            boolean isCatEnabled = prefs.getBoolean("cat_enabled", true);

            if ("MENU".equals(cardName) && isCatEnabled) {
                blueCat.setCatVisible(true);
            } else {
                blueCat.setCatVisible(false);
            }
        }
        if (closeButton != null) {
            closeButton.setVisible("MENU".equals(cardName));
        }
    }

    class CloseButton extends JButton {
        public CloseButton() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean isHover = getModel().isRollover();
            boolean isPressed = getModel().isArmed();

            if (isHover) {
                g2.setColor(isPressed ? new Color(200, 30, 30, 220) : new Color(232, 17, 35, 220));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }

            g2.setColor(isHover ? Color.WHITE : new Color(160, 165, 175));
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int gap = 10;
            g2.drawLine(gap, gap, getWidth() - gap, getHeight() - gap);
            g2.drawLine(getWidth() - gap, gap, gap, getHeight() - gap);

            g2.dispose();
        }
    }

    class GearButton extends JButton {
        public GearButton() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
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

        menuPanel.setFocusable(true);
        menuPanel.enableInputMethods(false);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        topBar.setOpaque(false);

        GearButton settingsBtn = new GearButton();
        settingsBtn.addActionListener(e -> SettingsDialog.showDialog(MainLauncher.this));
        topBar.add(settingsBtn);

        JPanel topArea = new JPanel(new GridBagLayout());
        topArea.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel title = new JLabel("ToolKit");
        title.setFont(new Font("Montserrat", Font.BOLD, 56));
        title.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.insets = new Insets(10, 0, 40, 0);
        gbc.weighty = 0;
        topArea.add(title, gbc);

        Color glassColor = new Color(30, 35, 40, 120);

        JButton btn1 = createModuleButton("RENAMER", glassColor, "/icons/renamer.png");
        JButton btn2 = createModuleButton("CHECKER", glassColor, "/icons/checker.png");
        JButton btn3 = createModuleButton("FINDER", glassColor, "/icons/finder.png");
        JButton btn4 = createModuleButton("SCORING", glassColor, "/icons/scoring.png");
        JButton btn5 = createModuleButton("REPLACER", glassColor, "/icons/replacer.png");
        JButton btn6 = createModuleButton("COMPARISON", glassColor, "/icons/comparison.png");
        JButton btn7 = createModuleButton("REGION", glassColor, "/icons/region.png");
        JButton btn8 = createModuleButton("CONVERTER", glassColor, "/icons/converter.png");
        JButton btn9 = createModuleButton("SECRET", glassColor, "/icons/secret.png");

        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;

        gbc.gridy = 1;
        gbc.weightx = 1.0; gbc.gridx = 0; topArea.add(btn1, gbc);
        gbc.weightx = 1.0; gbc.gridx = 1; topArea.add(btn2, gbc);
        gbc.weightx = 1.0; gbc.gridx = 2; topArea.add(btn3, gbc);

        gbc.gridy = 2;
        gbc.weightx = 1.0; gbc.gridx = 0; topArea.add(btn4, gbc);
        gbc.weightx = 1.0; gbc.gridx = 1; topArea.add(btn5, gbc);
        gbc.weightx = 1.0; gbc.gridx = 2; topArea.add(btn6, gbc);

        gbc.gridy = 3;
        gbc.weightx = 1.0; gbc.gridx = 0; topArea.add(btn7, gbc);
        gbc.weightx = 1.0; gbc.gridx = 1; topArea.add(btn8, gbc);
        gbc.weightx = 1.0; gbc.gridx = 2; topArea.add(btn9, gbc);

        gbc.gridy = 4;
        gbc.gridwidth = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        topArea.add(Box.createVerticalGlue(), gbc);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                if (e.getKeyCode() == KeyEvent.VK_G && menuPanel.isShowing() && (gamePanel == null || !gamePanel.isVisible())) {
                    gamePanel = new SavePigPanel();
                    gamePanel.enableInputMethods(false);
                    mainContainer.add(gamePanel, "GAME");
                    switchPanel("GAME");
                    gamePanel.requestFocusInWindow();
                    return true;
                }

                if (e.getKeyCode() == KeyEvent.VK_E && gamePanel != null && gamePanel.isVisible()) {
                    gamePanel.stopGame();
                    mainContainer.remove(gamePanel);
                    gamePanel = null;

                    SwingUtilities.invokeLater(() -> {
                        switchPanel("MENU");
                        menuPanel.requestFocusInWindow();
                    });
                    return true;
                }
            }
            return false;
        });

        menuPanel.add(topBar, BorderLayout.NORTH);
        menuPanel.add(topArea, BorderLayout.CENTER);

        return menuPanel;
    }

    private JButton createModuleButton(String cardName, Color themeColor, String iconPath) {
        RippleButton btn = new RippleButton("", themeColor);
        btn.setPreferredSize(new Dimension(270, 110));
        btn.setFocusable(false);

        try {
            java.net.URL imgURL = getClass().getResource(iconPath);
            if (imgURL != null) {
                ImageIcon originalIcon = new ImageIcon(imgURL);

                btn.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        int w = btn.getWidth();
                        int h = btn.getHeight();
                        if (w <= 0 || h <= 0) return;

                        int iconSize = Math.max(32, (int)(Math.min(w, h) * 0.45));

                        BufferedImage scaledImg = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2 = scaledImg.createGraphics();

                        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        g2.drawImage(originalIcon.getImage(), 0, 0, iconSize, iconSize, null);
                        g2.dispose();

                        btn.setIcon(new ImageIcon(scaledImg));
                    }
                });
            } else {
                System.err.println("找不到图标资源，请检查路径: " + iconPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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

                JLabel msg = new JLabel("  Toolkit 已在后台运行  ", SwingConstants.CENTER);
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