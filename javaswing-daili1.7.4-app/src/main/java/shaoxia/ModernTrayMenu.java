package shaoxia;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** * ✨ 极其精致的现代化圆角微透托盘悬浮窗菜单 (替代传统老旧 PopupMenu)
 */
public class ModernTrayMenu extends JWindow {
    public ModernTrayMenu(Window owner, MainLauncher launcher) {
        super(owner);
        setAlwaysOnTop(true);
        // 关键点：将底座窗口设为完全透明，使抗锯齿圆角边缘绝不漏出黑边
        setBackground(new Color(0, 0, 0, 0));

        // 主绘图面板
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 绘制卡片底色：采用高级半透明暗色系，与 FlatLaf 完美融合
                g2.setColor(new Color(32, 33, 38, 245));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16); // 优雅的 16px 大圆角

                // 绘制极细的高光微透微边框，极富精致感
                g2.setStroke(new BasicStroke(1.0f));
                g2.setColor(new Color(255, 255, 255, 35));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(3, 4, 3, 4);

        // 顶层状态小标题
        JLabel titleLabel = new JLabel("Toolkit - Running in Background");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 12));
        titleLabel.setForeground(new Color(140, 145, 155));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0;
        mainPanel.add(titleLabel, gbc);

        // 微弱的分隔线
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255, 255, 255, 20));
        gbc.gridy = 1;
        mainPanel.add(sep, gbc);

        // 按钮 1：显示面板
        JButton showBtn = createMenuButton("  Show Main Panel", new Color(255, 255, 255, 25));
        showBtn.addActionListener(e -> {
            setVisible(false);
            launcher.setVisible(true);
            launcher.setExtendedState(JFrame.NORMAL);
            launcher.toFront();
        });
        gbc.gridy = 2;
        mainPanel.add(showBtn, gbc);

        // 按钮 2：完全退出 (带柔和警示红底色)
        JButton exitBtn = createMenuButton("  Exit System", new Color(231, 76, 60, 45));
        exitBtn.setForeground(new Color(241, 105, 105)); // 优雅的淡红警示文字
        exitBtn.addActionListener(e -> System.exit(0));
        gbc.gridy = 3;
        mainPanel.add(exitBtn, gbc);

        add(mainPanel);
        pack(); // 自动计算最佳尺寸

        // 当用户点击电脑屏幕其他任何地方时，菜单自动“失焦隐藏”，体验极佳
        addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {}
            @Override
            public void windowLostFocus(WindowEvent e) {
                setVisible(false);
            }
        });
    }

    /** 统一流水线定制现代菜单按钮 */
    private JButton createMenuButton(String text, Color hoverBg) {
        JButton btn = new JButton(text) {
            private boolean isHover = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) { isHover = true; repaint(); }
                    @Override
                    public void mouseExited(MouseEvent e) { isHover = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 悬浮状态下呈现高亮圆角切片
                if (isHover) {
                    g2.setColor(hoverBg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                g2.setFont(getFont());
                g2.setColor(getForeground());
                FontMetrics fm = g2.getFontMetrics();
                int x = 10; // 文字左对齐
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setFont(MainLauncher.MAIN_FONT);
        btn.setForeground(Color.WHITE);
        btn.setPreferredSize(new Dimension(175, 38)); // 黄金舒适比例尺寸
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}