package me.shaoxia;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.File;

/**主页面板块 MainLauncher */

public class MainLauncher extends JFrame
{
    private BlueCat blueCat;
    private CardLayout cardLayout;
    private JPanel mainContainer;
    public static final Font MAIN_FONT = new Font("微软雅黑", Font.PLAIN, 16);
    public static final Font BOLD_FONT = new Font("微软雅黑", Font.BOLD, 18);

    public MainLauncher() {
        setTitle("代理助手 1.6 ");
        setSize(960, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            // 假设你的图片路径是 /resources/orange.png
            // getResource 的路径必须以 / 开头，代表从 classpath 的根目录开始寻找
            java.net.URL iconUrl = getClass().getResource("/orange.png");

            if (iconUrl != null) {
                ImageIcon icon = new ImageIcon(iconUrl);
                setIconImage(icon.getImage());
            } else {
                System.err.println("没找到图片路径：/orange.png");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);
        add(mainContainer);

        mainContainer.add(createMainMenu(), "MENU");
        mainContainer.add(new RenamerPanel(this), "RENAMER");
        mainContainer.add(new CheckerPanel(this), "CHECKER");
        mainContainer.add(new FinderPanel(this), "FINDER");
        mainContainer.add(new ScoringPanel(this), "SCORING");
        mainContainer.add(new ReplacerPanel(this), "REPLACER");
        mainContainer.add(new ConverterPanel(this), "CONVERTER");
        mainContainer.add(new me.shaoxia.ComparisonPanel(this), "COMPARISON");

        cardLayout.show(mainContainer, "MENU");

        // === 【召唤蓝猫】 ===
        blueCat = new BlueCat(this);
        setGlassPane(blueCat);

        // 确保启动时初始状态是对的
        blueCat.setCatVisible(true);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                blueCat.updateBounds();
            }
        });
    }

    /**
     * ✨【核心拦截控制中枢】：
     * 不管是主菜单按钮点击，还是继承子类通过任何方式切换卡片回来，统一在这个核心方法里拦截过滤。
     */
    public void switchPanel(String cardName) {
        cardLayout.show(mainContainer, cardName);

        // 全方位强制显隐：只有在主菜单卡片 "MENU" 时，猫咪才被允许出来玩，去别的位置则彻底不渲染！
        if (blueCat != null) {
            if ("MENU".equals(cardName)) {
                blueCat.setCatVisible(true);
            } else {
                blueCat.setCatVisible(false);
            }
        }
    }

    private JPanel createMainMenu() {
        BackgroundPanel menuPanel = new BackgroundPanel("texture2.png");
        menuPanel.setLayout(new BorderLayout());

        JPanel topArea = new JPanel(new GridBagLayout());
        topArea.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel title = new JLabel("代理助手 1.6");
        title.setFont(new Font("微软雅黑", Font.BOLD, 52));
        title.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.insets = new Insets(100, 0, 60, 0);
        gbc.weighty = 0;
        topArea.add(title, gbc);

        JButton btn1 = createModuleButton("协同改名助手", "RENAMER", new Color(41, 128, 185));
        JButton btn2 = createModuleButton("数据校对助手", "CHECKER", new Color(39, 174, 96));
        JButton btn3 = createModuleButton("文件检索助手", "FINDER", new Color(142, 68, 173));
        JButton btn4 = createModuleButton("评分生成助手", "SCORING", new Color(211, 84, 0));
        JButton btn5 = createModuleButton("文档替换助手", "REPLACER", new Color(255, 215, 0));
        JButton btn6 = createModuleButton("一键转换助手", "CONVERTER", new Color(192, 57, 43));
        JButton btn7 = createModuleButton("公告比对助手", "COMPARISON", new Color(44, 62, 80));
        JButton btn8 = createModuleButton("待开发...", "DEV", new Color(127, 140, 141));
        JButton btn9 = createModuleButton("待开发...", "DEV", new Color(127, 140, 141));

        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(10, 10, 10, 10);

        gbc.gridy = 1;
        gbc.gridx = 0; topArea.add(btn1, gbc);
        gbc.gridx = 1; topArea.add(btn2, gbc);
        gbc.gridx = 2; topArea.add(btn3, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0; topArea.add(btn4, gbc);
        gbc.gridx = 1; topArea.add(btn5, gbc);
        gbc.gridx = 2; topArea.add(btn6, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0; topArea.add(btn7, gbc);
        gbc.gridx = 1; topArea.add(btn8, gbc);
        gbc.gridx = 2; topArea.add(btn9, gbc);

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
                int alpha = isHover ? 200 : 150;
                g2.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), alpha));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
                g2.setStroke(new BasicStroke(1.8f));
                g2.setColor(new Color(255, 255, 255, isHover ? 120 : 70));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 25, 25);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.setColor(new Color(0, 0, 0, 60));
                g2.drawString(getText(), x + 1, y + 1);
                g2.setColor(Color.WHITE);
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(270, 110));
        btn.setFont(new Font("微软雅黑", Font.BOLD, 20));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { btn.setFont(new Font("微软雅黑", Font.BOLD, 22)); }
            @Override
            public void mouseExited(MouseEvent e) { btn.setFont(new Font("微软雅黑", Font.BOLD, 20)); }
            @Override
            public void mousePressed(MouseEvent e) {
                // ✨ 统一走带拦截隐藏机制的调度中心
                switchPanel(cardName);
            }
        });
        return btn;
    }

    public void showMenu() {
        // ✨ 确保返回菜单时也走统一调度，将猫咪重新开启
        switchPanel("MENU");
    }

    public static void main(String[] args) {


        // 第一步：创建一个隐形的父窗口
        JFrame hiddenOwner = new JFrame();
        hiddenOwner.setUndecorated(true);
        hiddenOwner.pack();


        // 第二步：创建启动闪屏窗口
        JWindow smallBox = new JWindow(hiddenOwner);
        // ✨【关键】设置窗口背景透明
        smallBox.setBackground(new Color(0, 0, 0, 0));

        // ✨ 尝试从 resources 加载动画 GIF
        java.net.URL gifUrl = MainLauncher.class.getClassLoader().getResource("loading.gif");

        if (gifUrl != null) {
            // 1. 加载原图
            Image image = Toolkit.getDefaultToolkit().createImage(gifUrl);

            // 2. 创建一个颜色过滤器，专门把白色 (RGB: 255, 255, 255) 过滤掉
            ImageFilter filter = new RGBImageFilter() {
                public final int filterRGB(int x, int y, int rgb) {
                    // 如果像素颜色是白色（或者非常接近白色），将其 Alpha 通道设为 0
                    if ((rgb | 0xFF000000) == 0xFFFFFFFF) {
                        return 0x00FFFFFF & rgb;
                    }
                    return rgb;
                }
            };

            // 3. 应用过滤器并生成新的图像对象
            ImageProducer ip = new FilteredImageSource(image.getSource(), filter);
            Image transparentGif = Toolkit.getDefaultToolkit().createImage(ip);

            // 4. 将处理后的 GIF 放入 Label
            JLabel gifLabel = new JLabel(new ImageIcon(transparentGif));
            // ✨【关键】让 Label 背景透明，不遮挡下层
            gifLabel.setOpaque(false);

            // 将面板背景也设为透明
            JPanel content = new JPanel(new BorderLayout());
            content.setOpaque(false);
            content.add(gifLabel, BorderLayout.CENTER);

            smallBox.setContentPane(content);
            smallBox.pack();
        } else {
            // 降级方案
            JLabel loadingLabel = new JLabel("代理助手 1.6 正在启动...", SwingConstants.CENTER);
            loadingLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
            smallBox.getContentPane().add(loadingLabel);
            smallBox.getRootPane().setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            smallBox.setSize(180, 146);
        }

        smallBox.setLocationRelativeTo(null);
        smallBox.setVisible(true);

        // 第三步：后台线程加载主程序
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (Exception e) {}

            SwingUtilities.invokeLater(() -> {
                FlatDarkLaf.setup();
                UIManager.put("defaultFont", new Font("微软雅黑", Font.PLAIN, 14));

                MainLauncher launcher = new MainLauncher();
                smallBox.dispose();
                hiddenOwner.dispose();
                launcher.setVisible(true);
            });
        }).start();
    }
}