package shaoxia;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

// PY脚本助手 (终极物理透明穿透 + 删减表头版)
public class PyHelperPanel extends BackgroundPanel {
    private MainLauncher launcher;
    private JTable scriptTable;
    private DefaultTableModel tableModel;
    private JTextArea detailTextArea;

    // 脚本存放路径
    private final String SCRIPT_RESOURCE_PATH = "/scripts/";
    // Python 安装包内置路径
    private final String PYTHON_ENV_PATH = "/env/python-installer.exe";

    public PyHelperPanel(MainLauncher launcher) {
        super("texture2.png");
        this.launcher = launcher;
        setLayout(new BorderLayout());
        setOpaque(false);

        initUI();
    }

    private void initUI() {
        // === 1. 顶部导航栏 ===
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        topBar.setOpaque(false);

        ActionButton backBtn = new ActionButton("<< 返回主菜单", new Color(255, 255, 255, 30));
        backBtn.setPreferredSize(new Dimension(130, 36));
        backBtn.addActionListener(e -> launcher.showMenu());
        topBar.add(backBtn);

        JLabel titleLabel = new JLabel(" PY 脚本助手");
        titleLabel.setFont(MainLauncher.BOLD_FONT);
        titleLabel.setForeground(Color.WHITE);
        topBar.add(titleLabel);

        add(topBar, BorderLayout.NORTH);

        // === 2. 中心区域 (包含表格 和 详细说明框) ===
        JPanel centerPanel = new JPanel(new BorderLayout(0, 15));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

        // -- 表格部分 --
        String[] columnNames = {"编号", "脚本名称", "简述", "文件名", "详细说明"};
        Object[][] rowData = {
                {"001", "多表一键合并", "一键合并多个同格式的Excel表", "001_Merge_Excels.py",
                        "【功能详细说明】\n场景：收到了几十个不同单位发来的同格式Excel，一键将它们合并成一张总表。\n核心库：pandas"},
                {"002", "总表条件拆分", "按指定条件拆分总表为多个独立文件", "002_Split_Excel.py",
                        "【功能详细说明】\n场景：将包含几千行数据的项目总表，按照单列的字段生成若干个表格，比方说一个总表有若干不同项目经理，您选择表，再从字段中选择采购经理，那么他就会根据不同经理的名字，汇出若干个xlsx表格\n核心库：pandas / openpyxl"},
        };

        tableModel = new DefaultTableModel(rowData, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        scriptTable = new JTable(tableModel);

        // ⚔️ 强行剥夺 FlatLaf 对表格的渲染控制权
        scriptTable.setUI(new javax.swing.plaf.basic.BasicTableUI());
        scriptTable.setOpaque(false);
        scriptTable.setBackground(new Color(0, 0, 0, 0));

        // ⚔️ 釜底抽薪：直接移除表头（编号、脚本名称、简述那一行彻底消失）
        scriptTable.setTableHeader(null);

        scriptTable.setFont(MainLauncher.MAIN_FONT);
        scriptTable.setRowHeight(40);
        scriptTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        scriptTable.getColumnModel().removeColumn(scriptTable.getColumnModel().getColumn(4));
        scriptTable.getColumnModel().removeColumn(scriptTable.getColumnModel().getColumn(3));

        scriptTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        scriptTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        scriptTable.getColumnModel().getColumn(2).setPreferredWidth(350);

        scriptTable.setForeground(Color.WHITE);
        scriptTable.setShowGrid(false);
        scriptTable.setIntercellSpacing(new Dimension(0, 0));

        // 自定义数据单元格渲染器（彻底抽空底色）
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setOpaque(false);
                if (isSelected) {
                    setBackground(new Color(255, 255, 255, 40));
                } else {
                    setBackground(new Color(0, 0, 0, 0));
                }
                if (column == 0) setHorizontalAlignment(JLabel.CENTER);
                else setHorizontalAlignment(JLabel.LEFT);
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
        for (int i = 0; i < scriptTable.getColumnCount(); i++) {
            scriptTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // ==========================================================
        // ⚔️ 终极绝杀：抽离所有底板并彻底卸载表头视口
        // ==========================================================
        JScrollPane tableScrollPane = new JScrollPane(scriptTable);
        tableScrollPane.setUI(new javax.swing.plaf.basic.BasicScrollPaneUI());
        tableScrollPane.setOpaque(false);
        tableScrollPane.setBackground(new Color(0,0,0,0));
        tableScrollPane.setBorder(null);

        // 1. 抽空主视口（表格数据区）的底色
        tableScrollPane.getViewport().setUI(new javax.swing.plaf.basic.BasicViewportUI());
        tableScrollPane.getViewport().setOpaque(false);
        tableScrollPane.getViewport().setBackground(new Color(0,0,0,0));

        // 2. 移除列头视口，不给系统任何渲染灰色的机会
        tableScrollPane.setColumnHeaderView(null);

        GlassPanel tableGlassWrapper = new GlassPanel(new BorderLayout());
        tableGlassWrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tableGlassWrapper.add(tableScrollPane, BorderLayout.CENTER);
        centerPanel.add(tableGlassWrapper, BorderLayout.CENTER);

        // -- 大说明框部分 --
        detailTextArea = new JTextArea("请在上方表格中选择一个脚本以查看详细说明...");

        detailTextArea.setUI(new javax.swing.plaf.basic.BasicTextAreaUI());
        detailTextArea.setOpaque(false);
        detailTextArea.setBackground(new Color(0, 0, 0, 0));

        detailTextArea.setFont(MainLauncher.MAIN_FONT);
        detailTextArea.setEditable(false);
        detailTextArea.setLineWrap(true);
        detailTextArea.setWrapStyleWord(true);
        detailTextArea.setForeground(new Color(230, 230, 230));
        detailTextArea.setMargin(new Insets(5, 5, 5, 5));

        JScrollPane detailScrollPane = new JScrollPane(detailTextArea);
        detailScrollPane.setUI(new javax.swing.plaf.basic.BasicScrollPaneUI());
        detailScrollPane.setOpaque(false);
        detailScrollPane.setBackground(new Color(0,0,0,0));
        detailScrollPane.setBorder(null);

        detailScrollPane.getViewport().setUI(new javax.swing.plaf.basic.BasicViewportUI());
        detailScrollPane.getViewport().setOpaque(false);
        detailScrollPane.getViewport().setBackground(new Color(0,0,0,0));

        GlassPanel detailGlassWrapper = new GlassPanel(new BorderLayout(0, 8));
        detailGlassWrapper.setPreferredSize(new Dimension(0, 180));
        detailGlassWrapper.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel detailTitle = new JLabel("详细脚本说明");
        detailTitle.setFont(MainLauncher.BOLD_FONT);
        detailTitle.setForeground(Color.WHITE);

        detailGlassWrapper.add(detailTitle, BorderLayout.NORTH);
        detailGlassWrapper.add(detailScrollPane, BorderLayout.CENTER);

        centerPanel.add(detailGlassWrapper, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);

        // === 绑定表格点击事件 ===
        scriptTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int viewRow = scriptTable.getSelectedRow();
                if (viewRow != -1) {
                    int modelRow = scriptTable.convertRowIndexToModel(viewRow);
                    String details = (String) tableModel.getValueAt(modelRow, 4);
                    detailTextArea.setText(details);
                    detailTextArea.setCaretPosition(0);
                }
            }
        });

        // === 3. 底部操作栏 ===
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        bottomBar.setOpaque(false);

        ActionButton installPyBtn = new ActionButton("安装 Python 环境", new Color(41, 128, 185));
        installPyBtn.setPreferredSize(new Dimension(160, 38));
        installPyBtn.addActionListener(e -> installBundledPythonEnv());

        ActionButton installLibsBtn = new ActionButton("一键安装环境库", new Color(142, 68, 173));
        installLibsBtn.setPreferredSize(new Dimension(160, 38));
        installLibsBtn.addActionListener(e -> installPythonDependencies());

        ActionButton exportBtn = new ActionButton("导出选中脚本", new Color(39, 174, 96));
        exportBtn.setPreferredSize(new Dimension(160, 38));
        exportBtn.addActionListener(e -> exportSelectedScript());

        bottomBar.add(installPyBtn);
        bottomBar.add(installLibsBtn);
        bottomBar.add(exportBtn);

        add(bottomBar, BorderLayout.SOUTH);
    }

    private void exportSelectedScript() {
        int selectedViewRow = scriptTable.getSelectedRow();
        if (selectedViewRow == -1) {
            JOptionPane.showMessageDialog(this, "请先在表格中选择一个要导出的脚本！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = scriptTable.convertRowIndexToModel(selectedViewRow);
        String scriptName = (String) tableModel.getValueAt(modelRow, 1);
        String fileName = (String) tableModel.getValueAt(modelRow, 3);

        File desktopDir = FileSystemView.getFileSystemView().getHomeDirectory();
        File destFile = new File(desktopDir, fileName);

        try (InputStream is = getClass().getResourceAsStream(SCRIPT_RESOURCE_PATH + fileName)) {
            if (is == null) {
                JOptionPane.showMessageDialog(this,
                        "未找到文件！请检查项目源码的 resources" + SCRIPT_RESOURCE_PATH + " 目录下是否存在：" + fileName,
                        "导出失败", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Files.copy(is, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            JOptionPane.showMessageDialog(this, "脚本【" + scriptName + "】已成功导出到桌面！\n" + destFile.getAbsolutePath(), "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "导出发生错误：\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void installPythonDependencies() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "这将在后台为系统安装这 10 大核心脚本所需的 Python 依赖库。\n包含：pandas, openpyxl, python-docx, docxtpl, pypdf, pywin32。\n\n需要电脑保持联网，通常只需 10-20 秒，是否继续？",
                "极速配置环境库", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "正在后台静默下载并安装核心库...\n这只需极短的时间，安装完成前请勿关闭程序，稍后会弹窗提示结果。", "极速配置中", JOptionPane.INFORMATION_MESSAGE)
                );

                String[] cmd = {
                        "cmd.exe", "/c",
                        "pip install pandas openpyxl python-docx docxtpl pypdf pywin32 -i https://mirrors.aliyun.com/pypi/simple/ --trusted-host mirrors.aliyun.com"
                };

                Process process = Runtime.getRuntime().exec(cmd);
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(this, "太爽了！办公自动化 10 大核心依赖库已全部安装完毕！\n现在你可以畅快运行所有的脚本了。", "配置成功", JOptionPane.INFORMATION_MESSAGE)
                    );
                } else {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(this, "安装遇到阻力。请确认 Python 环境变量已正确配置。\n退出码：" + exitCode, "配置失败", JOptionPane.ERROR_MESSAGE)
                    );
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "执行命令异常：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE)
                );
            }
        }).start();
    }

    private void installBundledPythonEnv() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "这将会把系统内置的 Python 环境安装包提取到桌面并准备运行。\n是否继续？",
                "提取确认", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        File desktopDir = FileSystemView.getFileSystemView().getHomeDirectory();
        File destFile = new File(desktopDir, "python-installer.exe");

        new Thread(() -> {
            try (InputStream is = getClass().getResourceAsStream(PYTHON_ENV_PATH)) {
                if (is == null) {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(PyHelperPanel.this,
                                    "找不到内置安装包！\n请确保打包前已将 python-installer.exe 放入 resources/env/ 目录下。",
                                    "资源缺失", JOptionPane.ERROR_MESSAGE)
                    );
                    return;
                }

                Files.copy(is, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                SwingUtilities.invokeLater(() -> {
                    int runConfirm = JOptionPane.showConfirmDialog(PyHelperPanel.this,
                            "Python 安装包已成功提取到桌面！\n\n【重要提示】：安装时请务必勾选界面底部的 \n'Add python.exe to PATH'\n'Use admin privileges when instaling py.exe'\n\n是否立即启动安装程序？",
                            "提取完成", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

                    if (runConfirm == JOptionPane.YES_OPTION) {
                        try {
                            Desktop.getDesktop().open(destFile);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(PyHelperPanel.this, "请手动在桌面双击 python-installer.exe 进行安装。", "提示", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(PyHelperPanel.this, "提取发生错误：\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE)
                );
            }
        }).start();
    }

    // ✨ 核心复用组件 1：毛玻璃容器 (已断绝系统底色绘制)
    class GlassPanel extends JPanel {
        public GlassPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
            setBackground(new Color(0, 0, 0, 0));
        }

        @Override
        protected void paintComponent(Graphics g) {
            // 彻底切断 super.paintComponent()
            // 阻止任何系统级的底色“偷渡”行为，完全由我们自己画玻璃
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(255, 255, 255, 15));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 25, 25);

            g2.setColor(new Color(0, 0, 0, 50));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 25, 25);

            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(new Color(255, 255, 255, 70));
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
            if (isPressed) {
                drawColor = bgColor.darker();
            } else if (isHover) {
                drawColor = bgColor.brighter();
            }

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