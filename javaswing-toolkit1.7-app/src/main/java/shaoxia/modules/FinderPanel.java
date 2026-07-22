package shaoxia.modules;

import shaoxia.MainLauncher;
import shaoxia.Utils.ActionButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** * 文件检索助手 FinderPanel
 * (极限暗夜毛玻璃 + 彻底抛弃JTable换用纯净JList + 完美消除所有系统边框版)
 */
public class FinderPanel extends BackgroundPanel {
    private JTextField pathDisplayField;
    private JTextArea rulesTextArea;

    // ✨ 核心替换：使用更纯粹的 JList 替代臃肿的 JTable
    private JList<File> fileList;
    private DefaultListModel<File> listModel;

    private List<File> allFiles = new ArrayList<>();
    private JLabel statusLabel;

    // 文件与文件夹类型过滤复选框
    private JCheckBox chkWord, chkExcel, chkPdf, chkOther, chkFolderOnly;

    public FinderPanel(MainLauncher parent) {
        super("bg_imgs/月球与地球.png");
        setLayout(new BorderLayout(10, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        initTopBar(parent);
        initLeftRulesArea();
        initCenterListArea(); // 替换为全新的 List 区域
        initBottomStatusBar();
    }

    private void initTopBar(MainLauncher parent) {
        // 响应式流布局容器：保证窗口怎么缩放都不会重叠
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        // --- 第一行：核心操作按钮 ---
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row1.setOpaque(false);

        // 统一按钮大小为 130x36
        ActionButton backBtn = new ActionButton("<< 返回菜单");
        backBtn.setPreferredSize(new Dimension(130, 36));
        backBtn.addActionListener(e -> parent.showMenu());

        ActionButton loadBtn = new ActionButton("扫描文件夹");
        loadBtn.setPreferredSize(new Dimension(130, 36));
        loadBtn.addActionListener(e -> selectAndLoad());

        ActionButton extractBtn = new ActionButton("提取关键项");
        extractBtn.setPreferredSize(new Dimension(130, 36));
        extractBtn.addActionListener(e -> quickExtract());

        ActionButton showAllBtn = new ActionButton("刷新总览");
        showAllBtn.setPreferredSize(new Dimension(130, 36));
        showAllBtn.addActionListener(e -> {
            resetFilters();
            filterFiles();
        });

        row1.add(backBtn);
        row1.add(loadBtn);
        row1.add(extractBtn);
        row1.add(showAllBtn);

        // --- 第二行：类型与文件夹过滤 ---
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row2.setOpaque(false);

        JLabel filterIcon = new JLabel("类型筛选: ");
        filterIcon.setForeground(new Color(200, 200, 200));
        filterIcon.setFont(new Font("微软雅黑", Font.BOLD, 13));
        row2.add(filterIcon);

        chkWord = createStyledCheckBox("Word");
        chkExcel = createStyledCheckBox("Excel");
        chkPdf = createStyledCheckBox("PDF");
        chkOther = createStyledCheckBox("其他");

        chkFolderOnly = createStyledCheckBox("仅文件夹");
        chkFolderOnly.setForeground(new Color(255, 255, 255));

        resetFilters();

        chkWord.addItemListener(e -> filterFiles());
        chkExcel.addItemListener(e -> filterFiles());
        chkPdf.addItemListener(e -> filterFiles());
        chkOther.addItemListener(e -> filterFiles());
        chkFolderOnly.addItemListener(e -> {
            boolean folderOnly = chkFolderOnly.isSelected();
            chkWord.setEnabled(!folderOnly);
            chkExcel.setEnabled(!folderOnly);
            chkPdf.setEnabled(!folderOnly);
            chkOther.setEnabled(!folderOnly);
            filterFiles();
        });

        row2.add(chkWord);
        row2.add(chkExcel);
        row2.add(chkPdf);
        row2.add(chkOther);
        row2.add(new JLabel("  |  ")); // 分隔符
        row2.add(chkFolderOnly);

        // --- 第三行：路径展示框 ---
        JPanel row3 = new JPanel(new BorderLayout());
        row3.setOpaque(false);
        row3.setBorder(new EmptyBorder(5, 10, 5, 10));

        pathDisplayField = new JTextField(" 未选择任何文件夹...") {
            @Override
            public boolean isOpaque() { return false; }
        };
        pathDisplayField.setUI(new javax.swing.plaf.basic.BasicTextFieldUI());
        pathDisplayField.setEditable(false);
        pathDisplayField.setBackground(new Color(0, 0, 0, 0));
        pathDisplayField.setForeground(new Color(255, 255, 255));
        pathDisplayField.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        pathDisplayField.setFont(new Font("微软雅黑", Font.BOLD, 13));

        GlassPanel pathGlass = new GlassPanel(new BorderLayout());
        pathGlass.setPreferredSize(new Dimension(0, 35));
        pathGlass.add(pathDisplayField, BorderLayout.CENTER);
        row3.add(pathGlass, BorderLayout.CENTER);

        topContainer.add(row1);
        topContainer.add(row2);
        topContainer.add(row3);

        add(topContainer, BorderLayout.NORTH);
    }

    private void initLeftRulesArea() {
        rulesTextArea = new JTextArea("评审报告\n专家签到表\n应答文件上传情况统计") {
            @Override
            public boolean isOpaque() { return false; }
        };
        rulesTextArea.setUI(new javax.swing.plaf.basic.BasicTextAreaUI());
        rulesTextArea.setBackground(new Color(0, 0, 0, 0));
        rulesTextArea.setForeground(Color.WHITE);
        rulesTextArea.setCaretColor(Color.WHITE);
        rulesTextArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        rulesTextArea.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane rulesScroll = new JScrollPane(rulesTextArea) {
            @Override
            public boolean isOpaque() { return false; }
        };
        rulesScroll.setUI(new javax.swing.plaf.basic.BasicScrollPaneUI());
        rulesScroll.getViewport().setUI(new javax.swing.plaf.basic.BasicViewportUI());
        rulesScroll.setOpaque(false);
        rulesScroll.getViewport().setOpaque(false);
        rulesScroll.setBackground(new Color(0,0,0,0));
        rulesScroll.getViewport().setBackground(new Color(0,0,0,0));

        // 抹杀内嵌边框
        rulesScroll.setBorder(null);
        rulesScroll.setViewportBorder(null);

        GlassPanel rulesGlassWrapper = new GlassPanel(new BorderLayout(0, 5));
        rulesGlassWrapper.setPreferredSize(new Dimension(250, 0));
        rulesGlassWrapper.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel rulesTitle = new JLabel("逐行匹配");
        rulesTitle.setFont(MainLauncher.BOLD_FONT);
        rulesTitle.setForeground(Color.WHITE);

        rulesGlassWrapper.add(rulesTitle, BorderLayout.NORTH);
        rulesGlassWrapper.add(rulesScroll, BorderLayout.CENTER);

        // 用一个透明面板包裹，左侧强行推入 10 像素
        JPanel leftAlignWrapper = new JPanel(new BorderLayout());
        leftAlignWrapper.setOpaque(false);
        leftAlignWrapper.setBorder(new EmptyBorder(0, 10, 0, 0));
        leftAlignWrapper.add(rulesGlassWrapper, BorderLayout.CENTER);

        add(leftAlignWrapper, BorderLayout.WEST);
    }

    // ✨ 核心重构：彻底抛弃 JTable，换用原生纯净的 JList
    private void initCenterListArea() {
        listModel = new DefaultListModel<>();

        fileList = new JList<>(listModel) {
            @Override
            public boolean isOpaque() { return false; }

            @Override
            public String getToolTipText(MouseEvent e) {
                int index = locationToIndex(e.getPoint());
                if (index > -1) {
                    File f = getModel().getElementAt(index);
                    // ✨ 优化：加入了固定最大宽度 (width: 450px) 和强制换行逻辑 (word-break: break-all)
                    return "<html><body style='font-family:微软雅黑; font-size:11px; color:#E0E0E0; margin:0; padding:2px; width:450px;'>"
                            + "<span style='color:#888888; font-size:10px;'>文件路径：</span><br>"
                            + "<div style='word-break: break-all; margin-top:3px; line-height:1.4;'>"
                            + f.getAbsolutePath()
                            + "</div>"
                            + "</body></html>";
                }
                return null;
            }

            // ✨ 终极和解版：放弃魔改圆角，拥抱 VS Code 级的高级直角质感
            @Override
            public JToolTip createToolTip() {
                JToolTip tip = super.createToolTip();

                // 强制实心，不给系统底层留任何作妖的空间
                tip.setOpaque(true);
                tip.setBackground(new Color(35, 38, 42)); // 极暗高级灰底色

                // 彻底放弃 drawRoundRect，直接使用极为干练的 1px 细线直角边框
                tip.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(85, 85, 85), 1),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10) // 留出呼吸感
                ));
                return tip;
            }
            // ✨ 核心修复：直接原地重写 JToolTip，告别 setUI 报错！

        };
        // 彻底扒掉系统底色与边框
        fileList.setOpaque(false);
        fileList.setBackground(new Color(0, 0, 0, 0));
        fileList.setBorder(BorderFactory.createEmptyBorder());

        // 极致纯净的列表渲染器 (只显示文件名，圆角高光悬停)
        // 极致纯净的列表渲染器 (现代双行排版：支持名称、类型、大小、时间)
        fileList.setCellRenderer(new DefaultListCellRenderer() {
            // 准备一个日期格式化器
            private final java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                // 强制 cellHasFocus 为 false，彻底杀死系统焦点虚线框
                super.getListCellRendererComponent(list, value, index, isSelected, false);
                setOpaque(false);

                File f = (File) value;
                String name = f.getName();

                // 1. 获取文件类型
                String type = f.isDirectory() ? "文件夹" : "未知类型";
                if (!f.isDirectory() && name.lastIndexOf('.') > 0) {
                    type = name.substring(name.lastIndexOf('.') + 1).toUpperCase() + " 文件";
                }

                // 2. 获取并智能格式化文件大小
                String sizeStr = "--";
                if (!f.isDirectory()) {
                    long bytes = f.length();
                    if (bytes < 1024) sizeStr = bytes + " B";
                    else if (bytes < 1024 * 1024) sizeStr = String.format("%.2f KB", bytes / 1024.0);
                    else sizeStr = String.format("%.2f MB", bytes / (1024.0 * 1024.0));
                }

                // 3. 获取修改时间
                String timeStr = sdf.format(new java.util.Date(f.lastModified()));

                // 4. 配置不同状态下的高雅色彩 (选中时文字发白，未选中时副标题变暗)
                String titleColor = isSelected ? "#FFFFFF" : "#E6E6E6";
                String subColor = isSelected ? "#C0C0C0" : "#888888";


                // 5. 组合 HTML 现代排版 (主标题大字重，副标题细字弱化，用竖线优雅分割)
                String html = String.format(
                        "<html><div style='padding: 2px 0;'>" + // <-- 稍微缩小了行距 (padding: 2px)
                                "<span style='font-size:12px; font-weight:bold; color:%s;'>%s</span><br>" + // ✨ 主标题缩小为 12px
                                "<span style='font-size:9px; color:%s; margin-top:2px;'>" +  // ✨ 副标题缩小为 9px，更加精致
                                "类型: %s &nbsp;&nbsp;|&nbsp;&nbsp; 大小: %s &nbsp;&nbsp;|&nbsp;&nbsp; 修改: %s" +
                                "</span></div></html>",
                        titleColor, name, subColor, type, sizeStr, timeStr
                );

                setText(html);
                setBorder(new EmptyBorder(6, 15, 6, 10)); // 调整双行内容的呼吸感边距

                if (isSelected) {
                    setBackground(new Color(255, 255, 255, 30));
                } else {
                    setBackground(new Color(0, 0, 0, 0));
                }
                return this;
            }

            @Override
            protected void paintComponent(Graphics g) {
                if (getBackground().getAlpha() > 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    // 悬停/选中时的完美微圆角高光
                    g2.fillRoundRect(4, 2, getWidth() - 8, getHeight() - 4, 12, 12);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        });

        // 极其优雅的拖拽导出逻辑 (直接抓取 File 对象，告别隐藏列)
        fileList.setDragEnabled(true);
        fileList.setTransferHandler(new TransferHandler() {
            @Override
            protected Transferable createTransferable(JComponent c) {
                List<File> selectedFiles = fileList.getSelectedValuesList();
                if (selectedFiles.isEmpty()) return null;
                return new Transferable() {
                    public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{DataFlavor.javaFileListFlavor}; }
                    public boolean isDataFlavorSupported(DataFlavor f) { return DataFlavor.javaFileListFlavor.equals(f); }
                    public Object getTransferData(DataFlavor f) { return selectedFiles; }
                };
            }
            @Override
            public int getSourceActions(JComponent c) { return COPY; }
        });

        // 干净利落的滚动容器
        JScrollPane listScroll = new JScrollPane(fileList) {
            @Override
            public boolean isOpaque() { return false; }
        };
        listScroll.setOpaque(false);
        listScroll.getViewport().setOpaque(false);
        listScroll.setBackground(new Color(0,0,0,0));
        listScroll.getViewport().setBackground(new Color(0,0,0,0));

        // 双重保险：干掉外边框和视口内边框，绝不留一丝直角痕迹
        listScroll.setBorder(null);
        listScroll.setViewportBorder(null);

        // 装入我们自己写的全局圆弧玻璃框
        GlassPanel listGlassWrapper = new GlassPanel(new BorderLayout());
        listGlassWrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        listGlassWrapper.add(listScroll, BorderLayout.CENTER);

        // 用一个透明面板包裹，右侧强行推入 10 像素
        JPanel centerAlignWrapper = new JPanel(new BorderLayout());
        centerAlignWrapper.setOpaque(false);
        centerAlignWrapper.setBorder(new EmptyBorder(0, 0, 0, 10));
        centerAlignWrapper.add(listGlassWrapper, BorderLayout.CENTER);

        add(centerAlignWrapper, BorderLayout.CENTER);
    }

    private void initBottomStatusBar() {
        statusLabel = new JLabel(" 准备就绪");
        statusLabel.setForeground(new Color(180, 180, 180));
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        add(statusLabel, BorderLayout.SOUTH);
    }

    // --- 业务逻辑 ---

    private void selectAndLoad() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File sel = chooser.getSelectedFile();
            pathDisplayField.setText(" 当前扫描路径： " + sel.getAbsolutePath());
            scanDirectory(sel);
        }
    }

    private void scanDirectory(File dir) {
        allFiles.clear();
        statusLabel.setText(" 正在扫描...");
        new Thread(() -> {
            recursiveWalk(dir);
            SwingUtilities.invokeLater(() -> {
                filterFiles();
                statusLabel.setText(" 扫描完成。发现项目： " + allFiles.size() + " 项");
            });
        }).start();
    }

    private void recursiveWalk(File f) {
        File[] list = f.listFiles();
        if (list != null) {
            for (File child : list) {
                allFiles.add(child);
                if (child.isDirectory()) {
                    recursiveWalk(child);
                }
            }
        }
    }

    private void filterFiles() {
        listModel.clear();

        boolean folderOnly = chkFolderOnly.isSelected();
        boolean showWord = chkWord.isSelected();
        boolean showExcel = chkExcel.isSelected();
        boolean showPdf = chkPdf.isSelected();
        boolean showOther = chkOther.isSelected();

        for (File f : allFiles) {
            String name = f.getName().toLowerCase();
            boolean match = false;

            if (folderOnly) {
                if (f.isDirectory()) match = true;
            } else {
                if (f.isDirectory()) continue;
                if (showWord && (name.endsWith(".doc") || name.endsWith(".docx"))) match = true;
                else if (showExcel && (name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".csv"))) match = true;
                else if (showPdf && name.endsWith(".pdf")) match = true;
                else if (showOther && !(name.endsWith(".doc") || name.endsWith(".docx") ||
                        name.endsWith(".xls") || name.endsWith(".xlsx") ||
                        name.endsWith(".csv") || name.endsWith(".pdf") )) {
                    match = true;
                }
            }

            if (match) {
                // 直接塞入 File 对象，不需要隐藏列
                listModel.addElement(f);
            }
        }
    }

    private void quickExtract() {
        String[] rules = rulesTextArea.getText().split("\n");
        listModel.clear();

        boolean folderOnly = chkFolderOnly.isSelected();
        boolean showWord = chkWord.isSelected();
        boolean showExcel = chkExcel.isSelected();
        boolean showPdf = chkPdf.isSelected();
        boolean showOther = chkOther.isSelected();

        int matchCount = 0;
        for (File f : allFiles) {
            String name = f.getName().toLowerCase();
            boolean typeMatch = false;

            if (folderOnly) {
                if (f.isDirectory()) typeMatch = true;
            } else {
                if (f.isDirectory()) continue;
                if (showWord && (name.endsWith(".doc") || name.endsWith(".docx"))) typeMatch = true;
                else if (showExcel && (name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".csv"))) typeMatch = true;
                else if (showPdf && name.endsWith(".pdf")) typeMatch = true;
                else if (showOther && !(name.endsWith(".doc") || name.endsWith(".docx") ||
                        name.endsWith(".xls") || name.endsWith(".xlsx") ||
                        name.endsWith(".csv") || name.endsWith(".pdf") )) {
                    typeMatch = true;
                }
            }

            if (!typeMatch) continue;

            for (String r : rules) {
                if (!r.trim().isEmpty() && name.contains(r.trim().toLowerCase())) {
                    // 同样直接塞入 File 对象
                    listModel.addElement(f);
                    matchCount++;
                    break;
                }
            }
        }
        statusLabel.setText(" 关键项提取完成。总计找到匹配： " + matchCount + " 项");
    }

    private void resetFilters() {
        chkWord.setSelected(true);
        chkExcel.setSelected(true);
        chkPdf.setSelected(true);
        chkOther.setSelected(true);
        chkFolderOnly.setSelected(false);
    }

    // --- UI 样式辅助方法 ---

    private JCheckBox createStyledCheckBox(String text) {
        JCheckBox chk = new JCheckBox(text);
        chk.setOpaque(false);
        chk.setForeground(Color.WHITE);
        chk.setFocusPainted(false);
        chk.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        chk.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return chk;
    }

    // ✨ 核心复用组件：极暗毛玻璃容器 (优化对比度版)
    // （为保证少侠代码一键运行不报错，这里暂且保留作为内部类，若你已移至全局可删去此段）
    class GlassPanel extends JPanel {
        public GlassPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
            setBackground(new Color(0, 0, 0, 0));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 降低白光反光，加深黑底遮罩，让里面的白色文字对比度飙升
            g2.setColor(new Color(255, 255, 255, 10));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 25, 25);
            g2.setColor(new Color(0, 0, 0, 110)); // 暗度拉满
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 25, 25);

            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(new Color(255, 255, 255, 60));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 25, 25);
            g2.dispose();
        }
    }
}