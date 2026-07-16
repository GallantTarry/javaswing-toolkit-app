package shaoxia;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 软件下载助手 SoftwarePanel (终极视觉版：全局抗锯齿圆角 + 黄金比例胶囊按钮)
 */
public class SoftwarePanel extends BackgroundPanel {

    private JPanel listContainer;

    // ✨ 在这里配置您放入 resources/software/ 下的安装包全名
    private final String[] packagedSoftware = {
            "Everything-Setup.exe",
            "卸载工具.7z",
            "pdf24.exe"

    };

    public SoftwarePanel(MainLauncher parent) {
        super("texture2.png");
        setLayout(new BorderLayout());

        initTopBar(parent);
        initContentArea();
    }

    private void initTopBar(MainLauncher parent) {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(15, 25, 10, 25));

        JButton backBtn = new ActionButton("<< Menu", new Color(255, 255, 255, 30));
        backBtn.setPreferredSize(new Dimension(130, 36));
        backBtn.addActionListener(e -> parent.showMenu());

        topBar.add(backBtn, BorderLayout.WEST);

        JLabel titleLabel = new JLabel("");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        topBar.add(titleLabel, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);
    }

    private void initContentArea() {
        listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setOpaque(false);
        listContainer.setBorder(new EmptyBorder(20, 40, 20, 40));

        JScrollPane scrollPane = new JScrollPane(listContainer);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        loadSoftwareList();
    }

    private void loadSoftwareList() {
        listContainer.removeAll();
        boolean hasSoftware = false;

        for (String fileName : packagedSoftware) {
            if (getClass().getResource("/software/" + fileName) != null) {
                listContainer.add(createSoftwareCard(fileName));
                listContainer.add(Box.createVerticalStrut(15));
                hasSoftware = true;
            }
        }

        if (!hasSoftware) {
            JLabel emptyMsg = new JLabel("未在安装包内检测到配置的软件。");
            emptyMsg.setFont(new Font("微软雅黑", Font.BOLD, 16));
            emptyMsg.setForeground(new Color(255, 255, 255));
            listContainer.add(emptyMsg);
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    private JPanel createSoftwareCard(String fileName) {
        // 圆润透明卡片背景
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(0, 0, 0, 90));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);

                g2.setStroke(new BasicStroke(1.2f));
                g2.setColor(new Color(255, 255, 255, 45));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 25, 12, 25)); // 左右留出充足内边距
        card.setMaximumSize(new Dimension(800, 80));

        // 左侧：软件名称和状态 (完美垂直居中)
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        infoPanel.setOpaque(false);
        JLabel nameLabel = new JLabel(fileName);
        nameLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        nameLabel.setForeground(Color.WHITE);

        JLabel sizeLabel = new JLabel("状态: 已封装在程序内部");
        sizeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        sizeLabel.setForeground(new Color(180, 180, 180));

        infoPanel.add(nameLabel);
        infoPanel.add(sizeLabel);
        card.add(infoPanel, BorderLayout.CENTER);

        // ✨ 右侧：使用 GridBagLayout 保证按钮绝对垂直居中，且间距统一
        JPanel btnPanel = new JPanel(new GridBagLayout());
        btnPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 12, 0, 0); // 按钮之间的间距锁定为12像素
        gbc.gridy = 0;

        // 生成统一比例的自定义圆润按钮
        ActionButton descBtn = new ActionButton("功能说明", new Color(41, 128, 185));
        ActionButton exportBtn = new ActionButton("一键解包到桌面", new Color(39, 174, 96));

        descBtn.addActionListener(e -> showDescription(fileName));
        exportBtn.addActionListener(e -> exportToDesktop(fileName, exportBtn));

        gbc.gridx = 0; btnPanel.add(descBtn, gbc);
        gbc.gridx = 1; btnPanel.add(exportBtn, gbc);

        card.add(btnPanel, BorderLayout.EAST);
        return card;
    }

    /**
     * ✨ 核心视觉组件：纯代码重绘的“胶囊型圆润按钮”
     */
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
            // ✨ 强行锁定比例尺寸，让所有方块里的按钮显得一样大、且对齐
            setPreferredSize(new Dimension(135, 38));
        }

        public void updateColor(Color newColor) {
            this.bgColor = newColor;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean isPressed = getModel().isPressed();
            boolean isHover = getModel().isRollover();

            // 物理反馈效果：悬浮变量，按压变暗
            Color drawColor = bgColor;
            if (isPressed) {
                drawColor = bgColor.darker();
            } else if (isHover) {
                drawColor = bgColor.brighter();
            }

            // 绘制胶囊背景 (使用 getHeight() 作为圆角弧度，就是完美的半圆形边缘)
            g2.setColor(drawColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight() - 1, getHeight() - 1);

            // 绘制按钮内发光边框
            g2.setColor(new Color(255, 255, 255, 60));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, getHeight() - 3, getHeight() - 3);

            g2.dispose();
            super.paintComponent(g); // 必须保留，用来在背景之上绘制居中的文字
        }
    }

    private void showDescription(String softwareFileName) {
        int dotIndex = softwareFileName.lastIndexOf('.');
        String baseName = (dotIndex > 0) ? softwareFileName.substring(0, dotIndex) : softwareFileName;
        String txtName = baseName + ".txt";

        StringBuilder descText = new StringBuilder();
        try (InputStream is = getClass().getResourceAsStream("/software/" + txtName)) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        descText.append(line).append("\n");
                    }
                }
            } else {
                descText.append("暂无该软件的详细说明。\n您可以随时在 resources/software 文件夹中添加一个名为【")
                        .append(txtName).append("】的 UTF-8 编码文件进行重新打包更新！");
            }
        } catch (Exception e) {
            descText.append("读取说明文件失败。");
        }

        JTextArea textArea = new JTextArea(descText.toString());
        textArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setOpaque(false);
        textArea.setForeground(Color.WHITE);
        textArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(450, 260));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);

        JOptionPane.showMessageDialog(this, scrollPane, softwareFileName + " - 功能说明", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportToDesktop(String fileName, ActionButton btn) {
        File desktopPath = FileSystemView.getFileSystemView().getHomeDirectory();
        File targetFile = new File(desktopPath, fileName);

        if (targetFile.exists()) {
            int overwrite = JOptionPane.showConfirmDialog(this,
                    "桌面上已存在同名文件，是否覆盖？", "文件冲突", JOptionPane.YES_NO_OPTION);
            if (overwrite != JOptionPane.YES_OPTION) return;
        }

        btn.setText("正在解包...");
        btn.setEnabled(false);

        new Thread(() -> {
            try (InputStream is = getClass().getResourceAsStream("/software/" + fileName)) {
                if (is == null) {
                    throw new Exception("在程序包内找不到该资源流！");
                }
                Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                SwingUtilities.invokeLater(() -> {
                    btn.setText("导出成功！");
                    btn.updateColor(new Color(142, 68, 173)); // 紫色反馈
                    JOptionPane.showMessageDialog(this, "成功解包至桌面！\n" + targetFile.getAbsolutePath(), "完成", JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    btn.setText("导出失败");
                    btn.updateColor(new Color(231, 76, 60)); // 红色警告
                    JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                Timer timer = new Timer(3000, e -> {
                    btn.setText("一键解包到桌面");
                    btn.updateColor(new Color(39, 174, 96)); // 恢复绿色
                    btn.setEnabled(true);
                });
                timer.setRepeats(false);
                timer.start();
            }
        }).start();
    }
}