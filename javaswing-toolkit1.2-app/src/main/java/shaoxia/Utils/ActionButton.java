package shaoxia.Utils;

import javax.swing.*;
import java.awt.*;

/**
 * 按钮工具类
 * 完美复刻 RenamerPanel 返回按钮样式：胶囊形状 + 黑雾玻璃色 + 悬停明暗反馈
 */
public class ActionButton extends JButton {

    // 默认使用 RenamerPanel 中的返回按钮底色
    public static final Color DEFAULT_GLASS_COLOR = new Color(30, 35, 40, 120);

    private Color bgColor;

    public ActionButton(String text) {
        this(text, DEFAULT_GLASS_COLOR);
    }

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

        Color drawColor = bgColor;

        // 悬停/点击时的透明度明暗变化
        if (isPressed) {
            int newAlpha = Math.min(255, bgColor.getAlpha() + 40);
            drawColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), newAlpha);
        } else if (isHover) {
            int newAlpha = Math.max(0, bgColor.getAlpha() - 40);
            drawColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), newAlpha);
        }

        // 保持原汁原味的胶囊形状 (两端全圆)
        g2.setColor(drawColor);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight() - 1, getHeight() - 1);

        // 高光白霜描边
        g2.setColor(new Color(255, 255, 255, 60));
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, getHeight() - 3, getHeight() - 3);

        g2.dispose();
        super.paintComponent(g);
    }
}