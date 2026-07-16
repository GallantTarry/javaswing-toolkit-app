package shaoxia.modules;

import shaoxia.MainLauncher;
import shaoxia.Utils.ActionButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
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

        ActionButton backBtn = new ActionButton("<< Menu");
        backBtn.setPreferredSize(new Dimension(100, 36));
        backBtn.addActionListener(e -> parent.showMenu());

        ActionButton loadBtn = new ActionButton("Scan Folder");
        loadBtn.setPreferredSize(new Dimension(120, 36));
        loadBtn.addActionListener(e -> selectAndLoad());

        ActionButton extractBtn = new ActionButton("Extract Key Items");
        extractBtn.setPreferredSize(new Dimension(150, 36));
        extractBtn.addActionListener(e -> quickExtract());

        ActionButton showAllBtn = new ActionButton("Refresh Overview");
        showAllBtn.setPreferredSize(new Dimension(150, 36));
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
        chkOther = createStyledCheckBox("Others");

        chkFolderOnly = createStyledCheckBox("Folders Only");
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

        pathDisplayField = new JTextField(" No folder selected...") {
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

        JLabel rulesTitle = new JLabel("Match line by line");
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
        };
        // 彻底扒掉系统底色与边框
        fileList.setOpaque(false);
        fileList.setBackground(new Color(0, 0, 0, 0));
        fileList.setBorder(BorderFactory.createEmptyBorder());

        // 极致纯净的列表渲染器 (只显示文件名，圆角高光悬停)
        fileList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                // 强制 cellHasFocus 为 false，彻底杀死系统焦点虚线框
                super.getListCellRendererComponent(list, value, index, isSelected, false);
                setOpaque(false);

                // 从模型中直接拿到 File 对象，但只在界面上渲染名字
                File f = (File) value;
                setText(f.getName());

                setFont(new Font("微软雅黑", Font.PLAIN, 14));
                setBorder(new EmptyBorder(8, 15, 8, 0)); // 设置行高与左边距的呼吸感

                if (isSelected) {
                    setBackground(new Color(255, 255, 255, 30));
                    setForeground(Color.WHITE);
                } else {
                    setBackground(new Color(0, 0, 0, 0));
                    setForeground(new Color(230, 230, 230));
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
        statusLabel = new JLabel(" Ready");
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
            pathDisplayField.setText(" Current scan path: " + sel.getAbsolutePath());
            scanDirectory(sel);
        }
    }

    private void scanDirectory(File dir) {
        allFiles.clear();
        statusLabel.setText(" Scanning...");
        new Thread(() -> {
            recursiveWalk(dir);
            SwingUtilities.invokeLater(() -> {
                filterFiles();
                statusLabel.setText(" Scan complete. Items found: " + allFiles.size() + " items");
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
        statusLabel.setText(" Key items extraction complete. Total matches found: " + matchCount + " items");
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