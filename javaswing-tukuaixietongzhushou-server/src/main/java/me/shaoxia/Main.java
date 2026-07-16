package me.shaoxia;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.List;
import java.util.Vector;

// 背景面板类：强化了图片加载逻辑和透明度处理
class BackgroundPanel extends JPanel {
    private Image backgroundImage;

    public BackgroundPanel() {
        setOpaque(false); // 关键：设置为不透明，允许背景图透出来
        loadTexture();
    }

    private void loadTexture() {
        // 依次尝试：当前目录、src目录
        File file = new File("texture.png");
        if (!file.exists()) {
            file = new File("src/texture.png");
        }

        if (file.exists()) {
            this.backgroundImage = new ImageIcon(file.getAbsolutePath()).getImage();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        // 先清空区域防止重影
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        if (backgroundImage != null) {
            // 绘制背景图，拉伸至全屏
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            // 如果图没了，显示深灰色背景
            g.setColor(new Color(60, 63, 65));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        super.paintComponent(g);
    }
}

class UltraBidRenamer extends JFrame {
    private JComboBox<String> biaoCombo, baoCombo, typeCombo, companyCombo;
    private JTextField newCompanyInput;
    private JLabel previewLabel;
    private JPanel dropArea;
    private BackgroundPanel rightPanel;
    private DefaultComboBoxModel<String> companyModel;

    private final Font FANG_SONG_BIG = new Font("仿宋", Font.BOLD, 22);
    private final Font FANG_SONG_LEFT_LABEL = new Font("仿宋", Font.BOLD, 18);
    private final Font FANG_SONG_LEFT_COMP = new Font("仿宋", Font.PLAIN, 18);
    private final Font FANG_SONG_MID = new Font("仿宋", Font.BOLD, 18);

    public UltraBidRenamer() {
        setTitle("协同助手1.2");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(1, 2, 10, 0));

        // --- 左侧：功能配置区 ---
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBackground(new Color(245, 245, 245)); // 给左侧一个干净的底色
        leftPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.weightx = 1.0;

        biaoCombo = new JComboBox<>(generateList("标", 10));
        addControlRow(leftPanel, gbc, 0, "1. 选择标段:", biaoCombo);
        baoCombo = new JComboBox<>(generateList("包", 10));
        addControlRow(leftPanel, gbc, 1, "2. 选择包号:", baoCombo);

        String[] fileTypes = {
                "价格文件", "商务文件", "技术文件",
                "二轮报价文件", "三轮报价文件", "四轮报价文件", "五轮报价文件", "六轮报价文件",
                "二轮分项报价文件", "三轮分项报价文件", "四轮分项报价文件", "五轮分项报价文件", "六轮分项报价文件"
        };
        typeCombo = new JComboBox<>(fileTypes);
        addControlRow(leftPanel, gbc, 2, "3. 文件类型:", typeCombo);

        companyModel = new DefaultComboBoxModel<>(new String[]{"请先在下方添加公司"});
        companyCombo = new JComboBox<>(companyModel);
        addControlRow(leftPanel, gbc, 3, "4. 选择公司:", companyCombo);

        newCompanyInput = new JTextField();
        addControlRow(leftPanel, gbc, 4, "5. 管理供应商:", newCompanyInput);

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        btnPanel.setOpaque(false);
        JButton saveBtn = new JButton("保存公司");
        JButton resetBtn = new JButton("重置名单");
        saveBtn.setFont(new Font("仿宋", Font.BOLD, 14));
        resetBtn.setFont(new Font("仿宋", Font.BOLD, 14));
        btnPanel.add(saveBtn);
        btnPanel.add(resetBtn);
        gbc.gridy = 5; gbc.gridx = 1;
        leftPanel.add(btnPanel, gbc);

        // --- 右侧：背景与拖拽 ---
        setupRightSide();

        // 逻辑
        saveBtn.addActionListener(e -> {
            String name = newCompanyInput.getText().trim();
            if(!name.isEmpty()) {
                if(companyModel.getSize() >= 10 && !companyModel.getElementAt(0).contains("请先")) {
                    JOptionPane.showMessageDialog(this, "公司总数已达10个上限！");
                    return;
                }
                if(companyModel.getElementAt(0).contains("请先")) companyModel.removeElementAt(0);
                companyModel.addElement(name);
                newCompanyInput.setText("");
                updatePreview();
            }
        });

        resetBtn.addActionListener(e -> {
            companyModel.removeAllElements();
            companyModel.addElement("请先在下方添加公司");
            updatePreview();
        });

        addListeners();
        add(leftPanel); add(rightPanel);
        updatePreview();
    }

    private void setupRightSide() {
        rightPanel = new BackgroundPanel();
        rightPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbcR = new GridBagConstraints();
        gbcR.fill = GridBagConstraints.BOTH; gbcR.weightx = 1.0;

        // 预览框：半透明背景
        JPanel previewContainer = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(255, 255, 255, 200));
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        previewContainer.setOpaque(false);
        previewLabel = new JLabel("预览区", JLabel.CENTER);
        previewLabel.setFont(FANG_SONG_BIG);
        previewContainer.add(previewLabel, BorderLayout.CENTER);
        gbcR.gridy = 0; gbcR.weighty = 0.2;
        rightPanel.add(previewContainer, gbcR);

        // 拖拽区：半透明背景
        dropArea = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(255, 255, 255, 150));
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        dropArea.setOpaque(false);
        dropArea.setBorder(BorderFactory.createTitledBorder(null, "【请把您的文件拖进来】", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, FANG_SONG_MID));
        JLabel dl = new JLabel("<html><center>直接把文件拖进来<br>自动完成重命名</center></html>", JLabel.CENTER);
        dl.setFont(FANG_SONG_MID);
        dropArea.add(dl, BorderLayout.CENTER);
        setDropTarget(dropArea);

        gbcR.gridy = 1; gbcR.weighty = 0.8; gbcR.insets = new Insets(10,0,0,0);
        rightPanel.add(dropArea, gbcR);
    }

    private void addControlRow(JPanel panel, GridBagConstraints gbc, int row, String text, Component c) {
        gbc.gridy = row; gbc.gridx = 0; gbc.weightx = 0.3;
        JLabel l = new JLabel(text); l.setFont(FANG_SONG_LEFT_LABEL);
        panel.add(l, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        c.setFont(FANG_SONG_LEFT_COMP);
        panel.add(c, gbc);
    }

    private void addListeners() {
        companyCombo.addActionListener(e -> updatePreview());
        biaoCombo.addActionListener(e -> updatePreview());
        baoCombo.addActionListener(e -> updatePreview());
        typeCombo.addActionListener(e -> updatePreview());
    }

    private void updatePreview() {
        String company = companyCombo.getSelectedItem() != null ? companyCombo.getSelectedItem().toString() : "";
        if(company.contains("请先")) company = "未选公司";
        String name = biaoCombo.getSelectedItem() + (String) baoCombo.getSelectedItem() + "_" + company + "_" + typeCombo.getSelectedItem();
        previewLabel.setText("预览：" + name);
        previewLabel.repaint();
    }

    private void setDropTarget(JPanel panel) {
        new DropTarget(panel, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            public void drop(DropTargetDropEvent dtde) {
                try {
                    String company = companyCombo.getSelectedItem().toString();
                    if(company.contains("请先")) {
                        dtde.rejectDrop();
                        JOptionPane.showMessageDialog(UltraBidRenamer.this, "您好，请先在下方添加并选择公司名称！", "提醒", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    int success = 0;
                    for (File f : files) {
                        int status = processRename(f, company);
                        if (status == 1) success++;
                        else if (status == -1) {
                            JOptionPane.showMessageDialog(UltraBidRenamer.this, "对不起，请检查您的文件夹下是否已有同名文件", "重名警告", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                    if (success > 0) JOptionPane.showMessageDialog(UltraBidRenamer.this, "您好，成功处理 " + success + " 个文件！", "消息", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    private int processRename(File file, String company) {
        String fileName = file.getName();
        String suffix = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")) : "";
        String newName = biaoCombo.getSelectedItem() + (String) baoCombo.getSelectedItem() + "_" + company + "_" + typeCombo.getSelectedItem() + suffix;
        File target = new File(file.getParent(), newName);
        if (target.exists()) return -1;
        return file.renameTo(target) ? 1 : 0;
    }

    private Vector<String> generateList(String p, int c) {
        Vector<String> v = new Vector<>();
        for (int i = 1; i <= c; i++) v.add(p + i);
        return v;
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new UltraBidRenamer().setVisible(true));
    }
}