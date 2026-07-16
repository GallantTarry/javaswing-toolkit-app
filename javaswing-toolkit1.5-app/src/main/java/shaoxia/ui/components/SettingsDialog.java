package shaoxia.ui.components;

import shaoxia.MainLauncher;
import shaoxia.modules.BackgroundPanel;
import shaoxia.Utils.ActionButton;
import shaoxia.Utils.GlassComboBox;
import shaoxia.Utils.GlassTextField;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URI;
import java.util.prefs.Preferences;

/**
 * 系统设置与帮助对话框工具类
 * 专门负责管理窗口尺寸设置、壁纸切换、关闭行为与软件说明书 (完全不透明实心底色版)
 */
public class SettingsDialog {

    public static void showDialog(JFrame parentFrame) {

        JDialog dialog = new JDialog(parentFrame, "系统设置与帮助", true);
        dialog.setSize(600, 560);
        dialog.setLocationRelativeTo(parentFrame);

        // 核心机制 1：去除系统自带边框，窗口级别完全透明以支持圆角
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        Preferences prefs = Preferences.userNodeForPackage(MainLauncher.class);

        // ✨ 核心机制 2：重写根容器，使用完全【不透明】的实心深色作为底板，彻底遮挡背后杂乱内容
        JPanel rootPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 纯实心深色底板，没有任何透明度
                g2.setColor(new Color(35, 38, 43));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

                // 保留最外层的玻璃白霜边缘，维持 UI 统一的高级感
                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(new Color(255, 255, 255, 60));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        rootPanel.setOpaque(false);

        // --- 自定义标题栏 (支持鼠标拖拽) ---
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);
        titleBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 5, 15));

        JLabel titleLabel = new JLabel("系统设置与帮助");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 15));
        titleLabel.setForeground(Color.WHITE);
        titleBar.add(titleLabel, BorderLayout.WEST);

        // ✨ 核心优化：采用手工绘图的 CloseButton，绝不出现字符变方块的 Bug
        CloseButton closeBtn = new CloseButton();
        closeBtn.addActionListener(e -> dialog.dispose());
        titleBar.add(closeBtn, BorderLayout.EAST);

        // 绑定拖拽事件
        MouseAdapter dragListener = new MouseAdapter() {
            Point loc;
            public void mousePressed(MouseEvent e) { loc = e.getPoint(); }
            public void mouseDragged(MouseEvent e) {
                Point p = dialog.getLocation();
                dialog.setLocation(p.x + e.getX() - loc.x, p.y + e.getY() - loc.y);
            }
        };
        titleBar.addMouseListener(dragListener);
        titleBar.addMouseMotionListener(dragListener);

        rootPanel.add(titleBar, BorderLayout.NORTH);

        Font mainFont = new Font("微软雅黑", Font.PLAIN, 16);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(mainFont);

        // 彻底透明化选项卡，使其完美贴合刚才画的实心底板
        tabs.setOpaque(false);
        tabs.setBackground(new Color(0, 0, 0, 0));

        // === 选项卡 1: 外观与界面 ===
        JPanel appearancePanel = new JPanel();
        appearancePanel.setLayout(new BoxLayout(appearancePanel, BoxLayout.Y_AXIS));
        appearancePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        appearancePanel.setOpaque(false);

        // --- 0. 主窗口关闭行为设置区 ---
        JPanel closeActionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        closeActionPanel.setOpaque(false);
        JLabel closeActionLabel = new JLabel("主窗口关闭行为: ");
        closeActionLabel.setFont(mainFont);
        closeActionLabel.setForeground(Color.WHITE);

        String[] closeOptions = {"最小化到系统托盘", "直接退出软件"};
        GlassComboBox<String> closeCombo = new GlassComboBox<>(new DefaultComboBoxModel<>(closeOptions));
        closeCombo.setFont(mainFont);
        closeCombo.setPreferredSize(new Dimension(160, 32));

        // 读取记忆的关闭行为，默认为 0 (最小化到托盘)
        closeCombo.setSelectedIndex(prefs.getInt("close_action", 0));

        closeActionPanel.add(closeActionLabel);
        closeActionPanel.add(closeCombo);

        // --- 1. 窗口大小设置区 ---
        JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        sizePanel.setOpaque(false);
        JLabel sizeLabel = new JLabel("默认启动尺寸: ");
        sizeLabel.setFont(mainFont);
        sizeLabel.setForeground(Color.WHITE);

        String[] resolutions = {"640x520", "960x720"};
        GlassComboBox<String> resCombo = new GlassComboBox<>(new DefaultComboBoxModel<>(resolutions));
        resCombo.setFont(mainFont);
        resCombo.setEditable(true);

        resCombo.setEditor(new javax.swing.plaf.basic.BasicComboBoxEditor() {
            @Override
            protected JTextField createEditorComponent() {
                GlassTextField editor = new GlassTextField() {
                    @Override
                    protected void paintBorder(Graphics g) { }
                };
                editor.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
                return editor;
            }
        });
        resCombo.setPreferredSize(new Dimension(160, 32));

        String currentRes = prefs.getInt("window_width", 640) + "x" + prefs.getInt("window_height", 520);
        resCombo.setSelectedItem(currentRes);

        sizePanel.add(sizeLabel);
        sizePanel.add(resCombo);

        JLabel sizeTip = new JLabel("(可输入自定义不可小于640x520)");
        sizeTip.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        sizeTip.setForeground(new Color(170, 175, 185));
        sizePanel.add(sizeTip);

        // --- 2. 全局背景图设置区 ---
        JPanel bgPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        bgPanel.setOpaque(false);
        JLabel bgLabel = new JLabel("全局背景壁纸: ");
        bgLabel.setFont(mainFont);
        bgLabel.setForeground(Color.WHITE);

        String[] bgNames = {"月球与地球","房间","门"};
        String[] bgPaths = {
                "bg_imgs/月球与地球.png",
                "bg_imgs/房间.png",
                "bg_imgs/门.png",
        };

        GlassComboBox<String> bgCombo = new GlassComboBox<>(new DefaultComboBoxModel<>(bgNames));
        bgCombo.setFont(mainFont);
        bgCombo.setPreferredSize(new Dimension(160, 32));

        String currentBg = prefs.get("bg_image_path", "bg_imgs/月球与地球.png");

        boolean isBuiltIn = false;
        for (int i = 0; i < bgPaths.length; i++) {
            if (bgPaths[i].equals(currentBg)) {
                bgCombo.setSelectedIndex(i);
                isBuiltIn = true;
                break;
            }
        }

        if (!isBuiltIn) {
            bgCombo.addItem(currentBg);
            bgCombo.setSelectedItem(currentBg);
        }

        ActionButton customBgBtn = new ActionButton("选取本地图片");
        customBgBtn.setFont(mainFont);
        customBgBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("选择自定义背景图片");
            chooser.setFileFilter(new FileNameExtensionFilter("图片文件 (PNG, JPG, BMP)", "png", "jpg", "jpeg", "bmp"));
            if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                String customPath = selectedFile.getAbsolutePath();
                bgCombo.addItem(customPath);
                bgCombo.setSelectedItem(customPath);
            }
        });

        bgPanel.add(bgLabel);
        bgPanel.add(bgCombo);
        bgPanel.add(customBgBtn);

        // --- 3. 背景明暗程度设置区 ---
        JPanel darknessPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        darknessPanel.setOpaque(false);
        JLabel darknessLabel = new JLabel("背景明暗调节: ");
        darknessLabel.setFont(mainFont);
        darknessLabel.setForeground(Color.WHITE);

        JSlider darknessSlider = new JSlider(0, 255, prefs.getInt("bg_darkness", 120));
        darknessSlider.setMajorTickSpacing(51);
        darknessSlider.setPaintTicks(true);
        darknessSlider.setOpaque(false);
        darknessSlider.setForeground(Color.WHITE);

        JLabel lightTip = new JLabel("明亮");
        lightTip.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        lightTip.setForeground(Color.WHITE);
        JLabel darkTip = new JLabel("暗沉");
        darkTip.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        darkTip.setForeground(Color.WHITE);

        darknessPanel.add(darknessLabel);
        darknessPanel.add(lightTip);
        darknessPanel.add(darknessSlider);
        darknessPanel.add(darkTip);

        // --- 4. 小猫开关设置区 ---
        JPanel catPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        catPanel.setOpaque(false);
        JLabel catLabel = new JLabel("桌面小猫(MITI): ");
        catLabel.setFont(mainFont);
        catLabel.setForeground(Color.WHITE);

        JCheckBox catCheckBox = new JCheckBox("启用可恶的小猫？");
        catCheckBox.setFont(mainFont);
        catCheckBox.setForeground(Color.WHITE);
        catCheckBox.setOpaque(false);
        catCheckBox.setSelected(prefs.getBoolean("cat_enabled", false));

        catPanel.add(catLabel);
        catPanel.add(catCheckBox);

        // --- 5. 统一应用按钮区 ---
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 15));
        btnPanel.setOpaque(false);
        ActionButton applyBtn = new ActionButton("保存设置");
        applyBtn.setFont(new Font("微软雅黑", Font.BOLD, 16));
        applyBtn.setPreferredSize(new Dimension(200, 45));

        applyBtn.addActionListener(e -> {

            // 保存关闭行为设置
            prefs.putInt("close_action", closeCombo.getSelectedIndex());
            prefs.putInt("bg_darkness", darknessSlider.getValue());

            String selRes = (String) resCombo.getSelectedItem();
            if(selRes != null && selRes.contains("x")) {
                try {
                    String[] parts = selRes.split("x");
                    int w = Integer.parseInt(parts[0].trim());
                    int h = Integer.parseInt(parts[1].trim());

                    if (w < 640 || h < 520) {
                        JOptionPane.showMessageDialog(dialog, "为保证界面排版正常显示，窗口尺寸不能小于 640x520，请重新输入！", "尺寸过小", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    parentFrame.setSize(w, h);
                    parentFrame.setLocationRelativeTo(null);
                    prefs.putInt("window_width", w);
                    prefs.putInt("window_height", h);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "尺寸输入格式异常，请参照 640x520 的格式重新输入！", "系统提示", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            String selectedName = (String) bgCombo.getSelectedItem();
            String finalPath = selectedName;

            for (int i = 0; i < bgNames.length; i++) {
                if (bgNames[i].equals(selectedName)) {
                    finalPath = bgPaths[i];
                    break;
                }
            }
            prefs.put("bg_image_path", finalPath);

            updateAllBackgrounds(parentFrame, finalPath);

            boolean enableCat = catCheckBox.isSelected();
            prefs.putBoolean("cat_enabled", enableCat);

            if (parentFrame instanceof MainLauncher) {
                ((MainLauncher) parentFrame).switchPanel("MENU");
            }

            JOptionPane.showMessageDialog(dialog, "外观设置已成功应用并保存！");
        });

        btnPanel.add(applyBtn);

        appearancePanel.add(closeActionPanel);
        appearancePanel.add(sizePanel);
        appearancePanel.add(bgPanel);
        appearancePanel.add(darknessPanel);
        appearancePanel.add(catPanel);
        appearancePanel.add(btnPanel);

        tabs.addTab("外观与界面", appearancePanel);

        // === 选项卡 2: 软件说明书 ===
        // ✨ 修改了结尾的文本，移除了无法直接点击的裸链接
        String manualText = """
                                                        【ToolKit 全模块说明书】
               ===============================================
               教学视频请访问   https://www.bilibili.com/video/BV1U2K36wET4/
             
               1.改名助手: 实现了拖入文件自动改文件名的功能。
               
               2.数据审核: 对信息协同数据表进行审核。
               
               3.文件检索: 扫描文件夹，在逐行匹配中输入名称，点击提取关键项即可检索出来。
               
               4.评分读表: 拖入采购文件即可生成已经合并且加上单元格方框的.xlsx表格文件到桌面上。
               
               5.文档转换: 将需要修改的文档内容输入，然后拖入整个文件夹或单/多个文件全局内容修改。
               
               6.公告比对: 提供采购公告word和ECP电工交易专区采购公告导出的pdf文件进行数据比对。
               
               7.地区查询: 输入地址自动检索出省市县和邮政编码，并提供横板导出txt。
               
               8.文档转换: 调用office2013及以上版本的引擎对文档进行转换。
               
               9.加密通话: 输入密钥可以对原文进行加密，或者同密钥对加密进行解码。
               
               10.拯救公猪: 主页面按G呼出按E退出，为纪念一个有趣的灵魂。
         
               =================依赖与开源组件鸣谢==================
               
               1.FlatLaf (3.0)：感谢 FlatLaf 提供现代化的深色毛玻璃跨平台 UI 视觉支撑。
               
               2.iTextPDF (5.5.13.3)：感谢 iTextPDF 提供极其稳定的动态 PDF 生成与深度操作支持。
               
               3.Apache PDFBox (2.0.31)：感谢 Apache PDFBox 强大的底层 PDF 纯文本与元数据解析能力。
               
               4.Apache POI (5.2.3)：感谢 Apache POI 筑牢处理老版 XLS 办公文档的底层基石。
               
               5.Apache POI OOXML (5.2.3)：感谢 Apache POI OOXML 实现 XLSX/DOCX 等新版复杂办公数据的完美自动化清洗与比对。
               
               6.Apache POI Scratchpad (5.2.3)：感谢 Apache POI Scratchpad 提供更丰富的复杂 Office 格式扩展支持。
               
               7.Log4j API (2.17.1)：感谢 Log4j API 提供标准化的高效并发日志记录接口。
               
               8.Log4j Core (2.17.1)：感谢 Log4j Core 为系统状态追踪与异常排查提供坚实的日志输出引擎。
               
               9.Google Gson (2.10.1)：感谢 Google Gson 带来极度丝滑的 Java 对象与复杂 JSON 互转体验。
               
               10.JLayer (1.0.1)：感谢 JLayer 引擎赋予工具灵动的 MP3 复古音效与跨线程音频解码能力。
               
               11.org.json (20240303)：感谢 org.json 提供极简优雅的树状节点解析，精准解构网络 API 返回数据。
               
               12.Maven Compiler Plugin (3.11.0)：感谢 Maven Compiler 插件保驾护航，完美锁定并顺滑编译前沿的 Java 21 代码。
               
               13.Maven Shade Plugin (3.5.1)：感谢 Maven Shade 超级插件完美融合错综复杂的依赖并剥离冲突签名，凝练出纯粹的独立可执行程序。
               
               =====================特别鸣谢=====================
               
               感谢 Gemini 对开发提供的巨大帮助与支持！
               
               感谢 supabase 数据库为拯救公猪游戏提供了后端数据库。
               
               感谢 智谱Ai 为桌宠MITI提供了智慧。
               
               代码已上传至github并开源！再次感谢您的使用。
               
               """;

        JTextArea manualArea = new JTextArea(manualText);
        manualArea.setEditable(false);
        manualArea.setLineWrap(true);
        manualArea.setWrapStyleWord(true);
        manualArea.setMargin(new Insets(15, 15, 15, 15));
        manualArea.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        manualArea.setCaretPosition(0);

        manualArea.setOpaque(false);
        manualArea.setBackground(new Color(0, 0, 0, 0));
        manualArea.setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(manualArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        JPanel manualWrapper = new JPanel(new BorderLayout());
        manualWrapper.setOpaque(false);
        manualWrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        manualWrapper.add(scrollPane, BorderLayout.CENTER);

        // ✨ 新增：在说明书面板的最底部，添加一个直达主页的专属跳转按钮
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
        linkPanel.setOpaque(false);
        ActionButton linkBtn = new ActionButton("点击访问作者主页");
        linkBtn.setPreferredSize(new Dimension(280, 38));
        linkBtn.addActionListener(e -> {
            try {
                // 调用系统默认浏览器打开网页
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("https://gallanttarry.github.io/TuKuai/"));
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "无法打开浏览器，请手动访问: \nhttps://gallanttarry.github.io/TuKuai/", "跳转失败", JOptionPane.ERROR_MESSAGE);
            }
        });
        linkPanel.add(linkBtn);
        manualWrapper.add(linkPanel, BorderLayout.SOUTH);

        tabs.addTab("软件说明书", manualWrapper);

        // 将组装好的标签页添加入底板
        rootPanel.add(tabs, BorderLayout.CENTER);

        dialog.add(rootPanel);
        dialog.setVisible(true);
    }

    private static void updateAllBackgrounds(Container container, String newPath) {
        for (Component c : container.getComponents()) {
            if (c instanceof BackgroundPanel) {
                ((BackgroundPanel) c).loadImage(newPath);
            } else if (c instanceof Container) {
                updateAllBackgrounds((Container) c, newPath);
            }
        }
    }

    /**
     * ✨ 完美移植主菜单关闭按钮，底层绘图保证永不乱码
     */
    static class CloseButton extends JButton {
        public CloseButton() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(30, 30));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean isHover = getModel().isRollover();
            boolean isPressed = getModel().isArmed();

            // 悬停泛红反馈效果
            if (isHover) {
                g2.setColor(isPressed ? new Color(200, 30, 30, 220) : new Color(232, 17, 35, 220));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }

            g2.setColor(isHover ? Color.WHITE : new Color(170, 175, 185));
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // 精确绘制 X 线条
            int gap = 10;
            g2.drawLine(gap, gap, getWidth() - gap, getHeight() - gap);
            g2.drawLine(getWidth() - gap, gap, gap, getHeight() - gap);

            g2.dispose();
        }
    }
}