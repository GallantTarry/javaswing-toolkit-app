package me.shaoxia;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;


//PY脚本助手
public class PyHelperPanel extends JPanel {
    private MainLauncher launcher;
    private JTable scriptTable;
    private DefaultTableModel tableModel;
    private JTextArea detailTextArea;

    // 脚本存放路径
    private final String SCRIPT_RESOURCE_PATH = "/scripts/";
    // Python 安装包内置路径
    private final String PYTHON_ENV_PATH = "/env/python-installer.exe";

    public PyHelperPanel(MainLauncher launcher) {
        this.launcher = launcher;
        setLayout(new BorderLayout());
        setOpaque(false);

        initUI();
    }

    private void initUI() {
        // === 1. 顶部导航栏 ===
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        topBar.setOpaque(false);
        JButton backBtn = new JButton("<< 返回主菜单");
        backBtn.setFont(MainLauncher.MAIN_FONT);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> launcher.showMenu());
        topBar.add(backBtn);

        JLabel titleLabel = new JLabel(" PY 脚本助手");
        titleLabel.setFont(MainLauncher.BOLD_FONT);
        titleLabel.setForeground(Color.WHITE);
        topBar.add(titleLabel);

        add(topBar, BorderLayout.NORTH);

        // === 2. 中心区域 (包含表格 和 详细说明框) ===
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

        // -- 表格部分 --
        String[] columnNames = {"编号", "脚本名称", "简述", "文件名", "详细说明"};
        Object[][] rowData = {
                // 【一、 Excel 数据与报表处理篇】
                {"001", "多表一键合并", "一键合并多个同格式的Excel表", "001_Merge_Excels.py",
                        "【功能详细说明】\n场景：收到了几十个不同单位发来的同格式Excel，一键将它们合并成一张总表。\n核心库：pandas"},

                {"002", "总表条件拆分", "按指定条件拆分总表为多个独立文件", "002_Split_Excel.py",
                        "【功能详细说明】\n场景：将包含几千行数据的项目总表，按照单列的字段生成若干个表格，比方说一个总表有若干不同项目经理，您选择表，再从字段中选择采购经理，那么他就会根据不同经理的名字，汇出若干个xlsx表格\n核心库：pandas / openpyxl"},

                {"003", "跨文件特定单元格提取", "批量提取多表特定单元格汇总", "003_Extract_Cells.py",
                        "【功能详细说明】\n它可以一键遍历你所选定的文件夹下的所有 Excel 文件（.xlsx格式），精准提取相同您指定的几个特定单元格（例如 B2, D5, E3）里的数据，，随后将这些零散的数据汇总合并成一张名为“专项提取汇总.xlsx”的新总表输出，B2,D5,E3作为新列字段，下方是检索的所有信息。\n核心库：openpyxl"},

                {"004", "两表差异高亮比对", "自动高亮核对新旧版本数据差异", "004_Compare_Excels.py",
                        "【功能详细说明】\n场景：核对最新版清单和历史版清单，用红色背景色自动高亮出被修改过、删除或新增的单元格。\n核心库：pandas / openpyxl"},

                {"005", "批量清洗不可见字符", "一键清理首尾空格及隐藏换行符", "005_Clean_Excel_Data.py",
                        "【功能详细说明】\n场景：从网页或老系统导出的Excel经常带有隐藏换行符和首尾空格导致无法计算，该脚本一键全局清洗。\n核心库：pandas"},


                {"006", "Excel数据批量生成Word报告", "读取表格数据批量生成排版文档", "006_Excel_to_Word.py",
                        "【功能详细说明】\n场景：提前做好一个Word模板（如通知书、评审报告），读取Excel里的一列数据，批量生成任意多的一样的改名后的Word模板副本。\n核心库：docxtpl"},

                {"007", "批量拆分与提取指定页", "精准剥离PDF指定页或拆解为单页", "007_Extract_PDF_Pages.py",
                        "【功能详细说明】\n场景：从几百页的标书或图纸中，精准剥离出想要的页数格式为逗号分隔，x-x，或者把单页拆解成独立文件。\n核心库：PyPDF2 / PyMuPDF"},




        };

        tableModel = new DefaultTableModel(rowData, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        scriptTable = new JTable(tableModel);

        scriptTable = new JTable(tableModel);
        scriptTable.setFont(MainLauncher.MAIN_FONT);
        scriptTable.setRowHeight(35);
        scriptTable.getTableHeader().setFont(MainLauncher.BOLD_FONT);
        scriptTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 从视图中隐藏 第4列(文件名) 和 第5列(详细说明)
        scriptTable.getColumnModel().removeColumn(scriptTable.getColumnModel().getColumn(4));
        scriptTable.getColumnModel().removeColumn(scriptTable.getColumnModel().getColumn(3));

        scriptTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        scriptTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        scriptTable.getColumnModel().getColumn(2).setPreferredWidth(350);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        scriptTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        JScrollPane tableScrollPane = new JScrollPane(scriptTable);
        centerPanel.add(tableScrollPane, BorderLayout.CENTER);

        // -- 大说明框部分 --
        detailTextArea = new JTextArea("请在上方表格中选择一个脚本以查看详细说明...");
        detailTextArea.setFont(MainLauncher.MAIN_FONT);
        detailTextArea.setEditable(false);
        detailTextArea.setLineWrap(true);
        detailTextArea.setWrapStyleWord(true);
        detailTextArea.setBackground(new Color(43, 45, 48));
        detailTextArea.setForeground(new Color(200, 200, 200));
        detailTextArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane detailScrollPane = new JScrollPane(detailTextArea);
        detailScrollPane.setPreferredSize(new Dimension(0, 180));
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100)),
                " 详细脚本说明 ", TitledBorder.LEFT, TitledBorder.TOP,
                MainLauncher.BOLD_FONT, Color.LIGHT_GRAY);
        detailScrollPane.setBorder(border);

        centerPanel.add(detailScrollPane, BorderLayout.SOUTH);
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

        JButton installPyBtn = new JButton("安装 Python 环境");
        installPyBtn.setFont(MainLauncher.MAIN_FONT);
        installPyBtn.setBackground(new Color(41, 128, 185)); // 蓝色
        installPyBtn.setForeground(Color.WHITE);
        installPyBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        installPyBtn.addActionListener(e -> installBundledPythonEnv());

        // 新增的“一键安装环境库”按钮
        JButton installLibsBtn = new JButton("一键安装环境库");
        installLibsBtn.setFont(MainLauncher.MAIN_FONT);
        installLibsBtn.setBackground(new Color(142, 68, 173)); // 紫色，用于视觉区分
        installLibsBtn.setForeground(Color.WHITE);
        installLibsBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        installLibsBtn.addActionListener(e -> installPythonDependencies());

        JButton exportBtn = new JButton("导出选中脚本到桌面");
        exportBtn.setFont(MainLauncher.MAIN_FONT);
        exportBtn.setBackground(new Color(39, 174, 96)); // 绿色
        exportBtn.setForeground(Color.WHITE);
        exportBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exportBtn.addActionListener(e -> exportSelectedScript());

        // 依次将三个按钮加入底部操作栏
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

    /**
     * 一键安装所有必备的 Python 第三方库（使用清华源加速）
     */
    /**
     * 一键安装必备的 Python 第三方库（轻量极速版）
     */
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

                // 核心命令：极简轻量的依赖库，清华源极速秒下
                // 核心命令：使用阿里云国内镜像极速秒下，并信任主机防证书拦截
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

    /**
     * 从 resources/env/ 提取内置的 Python 安装包并自动运行
     */
    private void installBundledPythonEnv() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "这将会把系统内置的 Python 环境安装包提取到桌面并准备运行。\n是否继续？",
                "提取确认", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        File desktopDir = FileSystemView.getFileSystemView().getHomeDirectory();
        File destFile = new File(desktopDir, "python-installer.exe");

        // 依然放在新线程里，提取大文件时不会让UI卡顿
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

                // 覆盖拷贝流到桌面
                Files.copy(is, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                SwingUtilities.invokeLater(() -> {
                    int runConfirm = JOptionPane.showConfirmDialog(PyHelperPanel.this,
                            "Python 安装包已成功提取到桌面！\n\n【重要提示】：安装时请务必勾选界面底部的 \n'Add python.exe to PATH'\n'Use admin privileges when instaling py.exe'\n\n是否立即启动安装程序？",
                            "提取完成", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

                    if (runConfirm == JOptionPane.YES_OPTION) {
                        try {
                            // 自动帮用户打开桌面上的安装包
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
}