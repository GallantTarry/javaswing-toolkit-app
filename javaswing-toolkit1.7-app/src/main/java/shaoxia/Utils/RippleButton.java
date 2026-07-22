package shaoxia.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 终极波纹按钮：融合物理按压、字体放大、阴影特效与水波纹引擎，且【绝对不撕裂 Z 轴图层】
 * ✨ 本版升级：完美支持原生 Icon 居中渲染，彻底兼容纯图标/图文混排模式
 * ✨ 最新优化：引入 Ease-out 缓动算法与 RadialGradientPaint 径向渐变，实现顶级 Material Design 质感
 */
public class RippleButton extends JButton {

    private int mouseX, mouseY;
    private float rippleRadius = 0;
    private float rippleAlpha = 0f;
    private Timer rippleTimer;

    private Color themeColor;
    private boolean isHover = false;
    private boolean isPressedState = false;

    public RippleButton(String text, Color themeColor) {
        super(text);
        this.themeColor = themeColor;

        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setFont(new Font("微软雅黑", Font.BOLD, 20));
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        rippleTimer = new Timer(15, e -> {
            // 1. 动态计算目标最大半径 (保证能完全覆盖按钮的最远对角线)
            float targetRadius = (float) Math.max(getWidth(), getHeight()) * 1.2f;

            // 2. 引入 Ease-out 缓动算法：距离目标越远扩散越快，越接近目标越慢
            rippleRadius += (targetRadius - rippleRadius) * 0.15f + 1.0f;

            // 3. 智能透明度衰减：如果鼠标已松开，或者波纹已经扩散到大部分区域，才开始消散
            if (!isPressedState || rippleRadius > targetRadius * 0.7f) {
                rippleAlpha -= 0.03f;
            }

            if (rippleAlpha <= 0f) {
                rippleAlpha = 0f;
                rippleTimer.stop();
            }
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                isPressedState = true;
                mouseX = e.getX();
                mouseY = e.getY();
                rippleRadius = 10;   // 给一个微小的初始半径，避免刚点击时显得生硬
                rippleAlpha = 0.35f; // 调低初始透明度，让水波纹更柔和高级
                rippleTimer.start();
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressedState = false;
                repaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                isHover = true;
                setFont(new Font("微软雅黑", Font.BOLD, 22));
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHover = false;
                setFont(new Font("微软雅黑", Font.BOLD, 20));
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int alpha = isPressedState ? 220 : (isHover ? 200 : 150);
        int offset = isPressedState ? 2 : 0;

        // 1. 画底色
        g2.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), alpha));
        g2.fillRoundRect(offset, offset, getWidth() - offset * 2, getHeight() - offset * 2, 25, 25);

        // 【核心防闪烁修复】：保存旧的 Clip，使用 .clip() 取交集，用完恢复！
        Shape oldClip = g2.getClip();
        Shape clipShape = new java.awt.geom.RoundRectangle2D.Float(offset, offset, getWidth() - offset * 2, getHeight() - offset * 2, 25, 25);
        g2.clip(clipShape);

        // 3. 绘制水波纹动画
        if (rippleAlpha > 0 && rippleRadius > 0) {
            int r = (int) rippleRadius;
            int currentAlpha = Math.max(0, Math.min(255, (int) (rippleAlpha * 255)));

            // 使用 RadialGradientPaint 实现边缘柔和过渡的高级光晕感
            RadialGradientPaint rgp = new RadialGradientPaint(
                    new Point(mouseX, mouseY),
                    r,
                    new float[]{0.0f, 0.7f, 1.0f},
                    new Color[]{
                            new Color(255, 255, 255, currentAlpha),
                            new Color(255, 255, 255, (int)(currentAlpha * 0.6)), // 中段渐弱
                            new Color(255, 255, 255, 0)                            // 边缘完全透明
                    }
            );
            g2.setPaint(rgp);
            g2.fillOval(mouseX - r, mouseY - r, r * 2, r * 2);
        }

        g2.setClip(oldClip); // 完美恢复画笔界限

        // 4. 反光边框
        g2.setStroke(new BasicStroke(1.8f));
        g2.setColor(new Color(255, 255, 255, isHover ? 120 : 70));
        g2.drawRoundRect(offset + 1, offset + 1, getWidth() - 3 - offset * 2, getHeight() - 3 - offset * 2, 25, 25);

        // ✨ 5. 核心修复：绘制外部传入的图标 (完美居中，并跟随按压 offset 偏移)
        Icon icon = getIcon();
        if (icon != null) {
            int iconX = (getWidth() - icon.getIconWidth()) / 2;
            int iconY = (getHeight() - icon.getIconHeight()) / 2;
            icon.paintIcon(this, g2, iconX, iconY + offset);
        }

        // 6. 立体阴影文字算法 (兼容文字模式，如果没有文字则跳过)
        String text = getText();
        if (text != null && !text.isEmpty()) {
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;

            g2.setColor(new Color(0, 0, 0, 60));
            g2.drawString(text, x + 1, y + 1 + offset);
            g2.setColor(Color.WHITE);
            g2.drawString(text, x, y + offset);
        }

        g2.dispose();
    }
}