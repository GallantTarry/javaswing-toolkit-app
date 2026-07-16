package shaoxia;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** * 文件检索助手 FinderPanel
 * (极限暗夜毛玻璃 + 纯享无表头 + 响应式布局版)
 */
public class FinderPanel extends BackgroundPanel {
    private JTextField pathDisplayField;
    private JTextArea rulesTextArea;
    private JTable fileTable;
    private DefaultTableModel tableModel;
    private List<File> allFiles = new ArrayList<>();
    private JLabel statusLabel;

    // 文件与文件夹类型过滤复选框
    private JCheckBox chkWord, chkExcel, chkPdf, chkOther, chkFolderOnly;

    public FinderPanel(MainLauncher parent) {
        super("texture2.png");
        setLayout(new BorderLayout(10, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        initTopBar(parent);
        initLeftRulesArea();
        initCenterTableArea();
        initBottomStatusBar();
    }

    private void initTopBar(MainLauncher parent) {
        // ✨ 响应式流布局容器：保证窗口怎么缩放都不会重叠
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        // --- 第一行：核心操作按钮 ---
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row1.setOpaque(false);

        ActionButton backBtn = new ActionButton("<< Menu", new Color(255, 255, 255, 30));
        backBtn.setPreferredSize(new Dimension(130, 36));
        backBtn.addActionListener(e -> parent.showMenu());

        ActionButton loadBtn = new ActionButton("1. 选择文件夹扫描", new Color(41, 128, 185));
        loadBtn.addActionListener(e -> selectAndLoad());

        ActionButton extractBtn = new ActionButton("2. 一键提取核心项", new Color(39, 174, 96));
        extractBtn.addActionListener(e -> quickExtract());

        ActionButton showAllBtn = new ActionButton("3. 刷新全览", new Color(142, 68, 173));
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
        chkOther = createStyledCheckBox("其他文件");

        chkFolderOnly = createStyledCheckBox("仅检索文件夹");
        chkFolderOnly.setForeground(new Color(255, 215, 0));

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

        pathDisplayField = new JTextField(" 尚未选择文件夹...") {
            @Override
            public boolean isOpaque() { return false; }
        };
        pathDisplayField.setUI(new javax.swing.plaf.basic.BasicTextFieldUI());
        pathDisplayField.setEditable(false);
        pathDisplayField.setBackground(new Color(0, 0, 0, 0));
        pathDisplayField.setForeground(new Color(46, 204, 113));
        pathDisplayField.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        pathDisplayField.setFont(new Font("微软雅黑", Font.BOLD, 13));

        GlassPanel pathGlass = new GlassPanel(new BorderLayout());
        pathGlass.setPreferredSize(new Dimension(0, 35));
        pathGlass.add(pathDisplayField, BorderLayout.CENTER);
        row3.add(pathGlass, BorderLayout.CENTER);

        // 将三行按顺序装入顶部容器
        topContainer.add(row1);
        topContainer.add(row2);
        topContainer.add(row3);

        add(topContainer, BorderLayout.NORTH);
    }

    private void initLeftRulesArea() {
        rulesTextArea = new JTextArea("评审报告\n专家签到表\n应答文件上传情况统计\n项目编号-综合报表") {
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
        rulesScroll.setBorder(null);

        GlassPanel rulesGlassWrapper = new GlassPanel(new BorderLayout(0, 5));
        rulesGlassWrapper.setPreferredSize(new Dimension(250, 0));
        rulesGlassWrapper.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel rulesTitle = new JLabel("提取规则 (逐行匹配)");
        rulesTitle.setFont(MainLauncher.BOLD_FONT);
        rulesTitle.setForeground(Color.WHITE);

        rulesGlassWrapper.add(rulesTitle, BorderLayout.NORTH);
        rulesGlassWrapper.add(rulesScroll, BorderLayout.CENTER);

        add(rulesGlassWrapper, BorderLayout.WEST);
    }

    private void initCenterTableArea() {
        // ✨ 模型里保留绝对路径（第2列），但是界面上不展示它
        tableModel = new DefaultTableModel(new String[]{"文件名", "绝对路径(隐藏)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        fileTable = new JTable(tableModel) {
            @Override
            public boolean isOpaque() { return false; }
        };
        fileTable.setUI(new javax.swing.plaf.basic.BasicTableUI());
        fileTable.setOpaque(false);
        fileTable.setBackground(new Color(0, 0, 0, 0));
        fileTable.setForeground(Color.WHITE);
        fileTable.setShowGrid(false);
        fileTable.setIntercellSpacing(new Dimension(0, 0));
        fileTable.setSelectionBackground(new Color(255, 255, 255, 40));
        fileTable.setSelectionForeground(Color.WHITE);
        fileTable.setRowHeight(40); // 稍微加宽行高，更显大气
        fileTable.setFont(new Font("微软雅黑", Font.PLAIN, 14));

        // ✨ 釜底抽薪：去掉表头
        fileTable.setTableHeader(null);

        // ✨ 隐藏路径列，用户只能看到纯净的文件名
        fileTable.getColumnModel().removeColumn(fileTable.getColumnModel().getColumn(1));

        // 居左渲染，并增加一点点文字左边距
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setOpaque(false);
                if (isSelected) {
                    setBackground(new Color(255, 255, 255, 40));
                } else {
                    setBackground(new Color(0, 0, 0, 0));
                }
                setHorizontalAlignment(JLabel.LEFT);
                setBorder(new EmptyBorder(0, 15, 0, 0)); // 让文字不要紧贴左侧边缘
                return this;
            }
            @Override
            protected void paintComponent(Graphics g) {
                if (getBackground().getAlpha() > 0) {
                    g.setColor(getBackground());
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
                super.paintComponent(g);
            }
        };
        fileTable.getColumnModel().getColumn(0).setCellRenderer(leftRenderer);

        // ✨ 文件拖拽导出功能：从隐藏模型中精准抓取真实路径
        fileTable.setDragEnabled(true);
        fileTable.setTransferHandler(new TransferHandler() {
            @Override
            protected Transferable createTransferable(JComponent c) {
                int[] rows = fileTable.getSelectedRows();
                if (rows.length == 0) return null;
                List<File> files = new ArrayList<>();
                for (int r : rows) {
                    int modelRow = fileTable.convertRowIndexToModel(r);
                    // 从隐藏的第2列拿到完整的绝对路径
                    String hiddenPath = (String) tableModel.getValueAt(modelRow, 1);
                    files.add(new File(hiddenPath));
                }
                return new Transferable() {
                    public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{DataFlavor.javaFileListFlavor}; }
                    public boolean isDataFlavorSupported(DataFlavor f) { return DataFlavor.javaFileListFlavor.equals(f); }
                    public Object getTransferData(DataFlavor f) { return files; }
                };
            }
            @Override
            public int getSourceActions(JComponent c) { return COPY; }
        });

        // 彻底抽离底板
        JScrollPane tableScroll = new JScrollPane(fileTable) {
            @Override
            public boolean isOpaque() { return false; }
        };
        tableScroll.setUI(new javax.swing.plaf.basic.BasicScrollPaneUI());
        tableScroll.setOpaque(false);
        tableScroll.setBackground(new Color(0,0,0,0));
        tableScroll.setBorder(null);

        tableScroll.getViewport().setUI(new javax.swing.plaf.basic.BasicViewportUI());
        tableScroll.getViewport().setOpaque(false);
        tableScroll.getViewport().setBackground(new Color(0,0,0,0));

        // 彻底摧毁列头视口
        tableScroll.setColumnHeaderView(null);

        GlassPanel tableGlassWrapper = new GlassPanel(new BorderLayout());
        tableGlassWrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tableGlassWrapper.add(tableScroll, BorderLayout.CENTER);

        add(tableGlassWrapper, BorderLayout.CENTER);
    }

    private void initBottomStatusBar() {
        statusLabel = new JLabel(" 就绪");
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
            pathDisplayField.setText(" 当前扫描目录: " + sel.getAbsolutePath());
            scanDirectory(sel);
        }
    }

    private void scanDirectory(File dir) {
        allFiles.clear();
        statusLabel.setText(" 正在扫描，请稍候...");
        new Thread(() -> {
            recursiveWalk(dir);
            SwingUtilities.invokeLater(() -> {
                filterFiles();
                statusLabel.setText(" 扫描完成，共找到项目: " + allFiles.size() + " 个");
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
        tableModel.setRowCount(0);

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
                // 仅注入 文件名 和 绝对路径(被隐藏)
                tableModel.addRow(new Object[]{f.getName(), f.getAbsolutePath()});
            }
        }
    }

    private void quickExtract() {
        String[] rules = rulesTextArea.getText().split("\n");
        tableModel.setRowCount(0);

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
                    tableModel.addRow(new Object[]{f.getName(), f.getAbsolutePath()});
                    matchCount++;
                    break;
                }
            }
        }
        statusLabel.setText(" 核心项提取完成，共匹配到: " + matchCount + " 个");
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

    // ✨ 核心复用组件 1：极暗毛玻璃容器 (优化对比度版)
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

    // ✨ 核心复用组件 2：胶囊按钮
    class ActionButton extends JButton {
        private Color bgColor;
        public ActionButton(String text, Color bgColor) {
            super(text);
            this.bgColor = bgColor;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("微软雅黑", Font.BOLD, 13));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean isPressed = getModel().isPressed();
            boolean isHover = getModel().isRollover();
            Color drawColor = bgColor;
            if (isPressed) drawColor = bgColor.darker();
            else if (isHover) drawColor = bgColor.brighter();

            g2.setColor(drawColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight() - 1, getHeight() - 1);
            g2.setColor(new Color(255, 255, 255, 60));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, getHeight() - 3, getHeight() - 3);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}