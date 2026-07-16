package me.shaoxia;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.Vector;

/**协同改名助手 RenamerPanel*/

class RenamerPanel extends BackgroundPanel
{
    private JComboBox<String> biaoCombo, baoCombo, typeCombo, companyCombo;
    private JTextField newCompanyInput;
    private JLabel previewLabel;
    private DefaultComboBoxModel<String> companyModel;

    // 将左右面板提升为成员变量，方便后续翻转位置
    private JPanel leftPanel;
    private JPanel rightPanel;
    private boolean isFlipped = false; // 记录当前是否已翻转

    public RenamerPanel(MainLauncher parent) {
        super("texture2.png");
        // 整体改为左右均分布局，让左半边和右半边各占一半
        setLayout(new GridLayout(1, 2));
        setOpaque(false);

        // ---------------- --- 控制面板区（包含灰色背景） ---------------- ---
        leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(new Color(45, 48, 50, 220)); // 统一的深灰色背景
        leftPanel.setOpaque(true); // 开启不透明，确保灰色覆盖整个区域
        leftPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // 【左上角】返回主菜单按钮放入灰色区域内部
        JPanel topLeftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        topLeftPanel.setOpaque(false);
        JButton backBtn = new JButton(" << 返回主菜单 ");
        backBtn.setFont(MainLauncher.BOLD_FONT);
        backBtn.addActionListener(e -> parent.showMenu());
        topLeftPanel.add(backBtn);
        leftPanel.add(topLeftPanel, BorderLayout.NORTH);

        // 【居中对齐区】剩下的核心控制组件
        JPanel centerLeftPanel = new JPanel(new GridBagLayout());
        centerLeftPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(12, 5, 12, 5);
        gbc.weightx = 1.0;
        gbc.weighty = 0; // 保持为0，整体居中

        // ---------------- 新增：翻转页面按钮 ----------------
        JButton flipBtn = new JButton("翻转页面");
        flipBtn.setFont(MainLauncher.BOLD_FONT);
        flipBtn.setBackground(new Color(46, 139, 87)); // 绿色 (SeaGreen)
        flipBtn.setForeground(Color.WHITE);
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        centerLeftPanel.add(flipBtn, gbc);

        // 绑定翻转逻辑
        flipBtn.addActionListener(e -> {
            isFlipped = !isFlipped;
            removeAll(); // 移除当前左右面板
            if (isFlipped) {
                add(rightPanel); // 监听面板放左边
                add(leftPanel);  // 控制面板放右边
            } else {
                add(leftPanel);  // 恢复默认：控制左，监听右
                add(rightPanel);
            }
            revalidate(); // 重新计算布局
            repaint();    // 重新绘制界面
        });

        // ---------------- 一键生成文件夹按钮 (向下移一位) ----------------
        JButton generateDirBtn = new JButton("一键生成文件夹");
        generateDirBtn.setFont(MainLauncher.BOLD_FONT);
        generateDirBtn.setBackground(new Color(70, 130, 180));
        generateDirBtn.setForeground(Color.WHITE);
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        centerLeftPanel.add(generateDirBtn, gbc);

        // 一键生成文件夹逻辑绑定
        generateDirBtn.addActionListener(e -> handleGenerateDirectories());

        // 表单组件以此类推，向下顺延 gridy
        biaoCombo = new JComboBox<>(generateList("标", 20));
        addControlRow(centerLeftPanel, gbc, 2, "1. 选择标段:", biaoCombo);

        baoCombo = new JComboBox<>(generateList("包", 20));
        addControlRow(centerLeftPanel, gbc, 3, "2. 选择包号:", baoCombo);

        String[] types = {"价格文件", "商务文件", "技术文件", "二轮报价", "三轮报价", "四轮报价", "五轮报价", "六轮报价", "首轮明细报价" ,"二轮明细报价", "三轮明细报价", "四轮明细报价", "五轮明细报价", "六轮明细报价"};
        typeCombo = new JComboBox<>(types);
        addControlRow(centerLeftPanel, gbc, 4, "3. 文件类型:", typeCombo);

        companyModel = new DefaultComboBoxModel<>(new String[]{"请先添加供应商"});
        companyCombo = new JComboBox<>(companyModel);
        addControlRow(centerLeftPanel, gbc, 5, "4. 选择公司:", companyCombo);

        newCompanyInput = new JTextField();
        addControlRow(centerLeftPanel, gbc, 6, "5. 管理供应商:", newCompanyInput);

        JPanel btnP = new JPanel(new GridLayout(1, 3, 8, 0));
        btnP.setOpaque(false);
        JButton save = new JButton("保存公司"), delete = new JButton("删除公司"), reset = new JButton("重置名单");
        btnP.add(save);
        btnP.add(delete);
        btnP.add(reset);
        gbc.gridy = 7;
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        centerLeftPanel.add(btnP, gbc);

        leftPanel.add(centerLeftPanel, BorderLayout.CENTER);

        // ---------------- --- 监听与预览面板区 ---------------- ---
        rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setOpaque(false);
        GridBagConstraints rGbc = new GridBagConstraints();
        rGbc.insets = new Insets(10, 20, 20, 20); // 顶部边距稍微调小，配合向上扩大
        rGbc.fill = GridBagConstraints.BOTH;

        previewLabel = new JLabel("预览：等待输入...", JLabel.CENTER);
        previewLabel.setFont(MainLauncher.BOLD_FONT);
        previewLabel.setForeground(Color.WHITE);
        rGbc.gridy = 0;
        rGbc.weighty = 0.05; // 压缩预览标签占据的垂直空间
        rGbc.weightx = 1.0;
        rightPanel.add(previewLabel, rGbc);

        JPanel dropArea = new JPanel(new BorderLayout());
        dropArea.setOpaque(false);
        dropArea.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.WHITE, 2, true), " 拖入文件重命名 ", TitledBorder.CENTER, TitledBorder.TOP, MainLauncher.BOLD_FONT, Color.WHITE));
        JLabel dl = new JLabel("<html><center>准备就绪<br>拖入文件自动处理</center></html>", JLabel.CENTER);
        dl.setForeground(Color.WHITE);
        dropArea.add(dl);
        setDropTarget(dropArea);
        rGbc.gridy = 1;
        rGbc.weighty = 0.95; // 监听框占据绝大部分垂直空间，实现向上扩大
        rightPanel.add(dropArea, rGbc);

        // 默认布局：先加控制区，再加监听区
        add(leftPanel);
        add(rightPanel);

        // ---------------- --- 事件监听绑定 ---------------- ---
        ActionListener listener = e -> updatePreview();
        biaoCombo.addActionListener(listener);
        baoCombo.addActionListener(listener);
        typeCombo.addActionListener(listener);
        companyCombo.addActionListener(listener);

        save.addActionListener(e -> {
            String n = newCompanyInput.getText().trim();
            if (!n.isEmpty()) {
                if (companyModel.getSize() > 0 && companyModel.getElementAt(0).contains("请先"))
                    companyModel.removeElementAt(0);
                companyModel.addElement(n);
                companyCombo.setSelectedItem(n);
                newCompanyInput.setText("");
                updatePreview();
            }
        });
        delete.addActionListener(e -> {
            String selected = (String) companyCombo.getSelectedItem();
            if (selected != null && !selected.contains("请先添加")) {
                companyModel.removeElement(selected);
                if (companyModel.getSize() == 0) {
                    companyModel.addElement("请先添加供应商");
                }
                updatePreview();
            } else {
                JOptionPane.showMessageDialog(this, "请先选择一个具体的供应商！");
            }
        });

        newCompanyInput.addActionListener(e -> save.doClick());

        reset.addActionListener(e -> {
            companyModel.removeAllElements();
            companyModel.addElement("请先添加供应商");
            updatePreview();
        });
    }

    /**
     * 一键生成文件夹并归档逻辑
     */
    private void handleGenerateDirectories() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("请选择包含报价文件的目录");

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();
            File[] files = selectedDir.listFiles();
            if (files == null) return;

            int successCount = 0;
            for (File file : files) {
                if (file.isDirectory()) continue;

                String fileName = file.getName();
                int firstUnder = fileName.indexOf('_');
                int lastUnder = fileName.lastIndexOf('_');

                if (firstUnder != -1 && lastUnder != -1 && firstUnder != lastUnder) {
                    String supplierName = fileName.substring(firstUnder + 1, lastUnder);
                    String typeWithExt = fileName.substring(lastUnder + 1);

                    String typeNoExt = typeWithExt;
                    if (typeWithExt.contains(".")) {
                        typeNoExt = typeWithExt.substring(0, typeWithExt.lastIndexOf('.'));
                    }

                    String rootFolderName = getTargetRootFolder(typeNoExt);

                    if (rootFolderName != null) {
                        File rootDir = new File(selectedDir, rootFolderName);
                        File supplierDir = new File(rootDir, supplierName);

                        if (!supplierDir.exists()) {
                            supplierDir.mkdirs();
                        }

                        File targetFile = new File(supplierDir, fileName);
                        if (file.renameTo(targetFile)) {
                            successCount++;
                        }
                    }
                }
            }
            JOptionPane.showMessageDialog(this, "智能归档完毕！\n共成功处理并分类了 " + successCount + " 个技术/商务/价格文件。\n（其它类型文件已自动忽略）", "处理成功", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 过滤策略
     */
    private String getTargetRootFolder(String type) {
        if (type.contains("技术") || type.contains("商务") || type.contains("价格") || type.contains("首轮") ) {
            return "应答文件";
        }
        return null;
    }

    private void addControlRow(JPanel p, GridBagConstraints g, int r, String t, Component c) {
        g.gridy = r;
        g.gridx = 0;
        g.weighty = 0;
        g.weightx = 0.3;
        g.gridwidth = 1;
        JLabel l = new JLabel(t);
        l.setForeground(Color.WHITE);
        p.add(l, g);
        g.gridx = 1;
        g.weightx = 0.7;
        p.add(c, g);
    }

    private void updatePreview() {
        Object selected = companyCombo.getSelectedItem();
        String c = (selected != null) ? selected.toString() : "未选公司";
        if (c.contains("请先")) c = "未选公司";
        previewLabel.setText("预览：" + biaoCombo.getSelectedItem() + baoCombo.getSelectedItem() + "_" + c + "_" + typeCombo.getSelectedItem());
    }

    private void setDropTarget(JPanel p) {
        new DropTarget(p, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            public void drop(DropTargetDropEvent dtde) {
                try {
                    String comp = companyCombo.getSelectedItem().toString();
                    if (comp.contains("请先")) {
                        dtde.rejectDrop();
                        JOptionPane.showMessageDialog(null, "请先输入并保存供应商名称！", "操作拦截", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    for (File file : files) {
                        String ext = file.getName().contains(".") ? file.getName().substring(file.getName().lastIndexOf(".")) : "";
                        String baseName = biaoCombo.getSelectedItem() + (String) baoCombo.getSelectedItem() + "_" + comp + "_" + typeCombo.getSelectedItem();
                        File newFile = new File(file.getParent(), baseName + ext);
                        if (newFile.exists()) {
                            int choice = JOptionPane.showConfirmDialog(null, "文件夹内已存在文件：\n" + newFile.getName() + "\n是否自动更名保存？", "检测到重名", JOptionPane.YES_NO_OPTION);
                            if (choice == JOptionPane.YES_OPTION) {
                                int count = 1;
                                while (newFile.exists()) {
                                    newFile = new File(file.getParent(), baseName + "_复件" + count + ext);
                                    count++;
                                }
                            } else continue;
                        }
                        file.renameTo(newFile);
                    }
                    JOptionPane.showMessageDialog(null, "处理成功！");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private Vector<String> generateList(String p, int c) {
        Vector<String> v = new Vector<>();
        for (int i = 1; i <= c; i++) v.add(p + i);
        return v;
    }
}