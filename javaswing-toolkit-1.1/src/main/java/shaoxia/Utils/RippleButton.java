package shaoxia.Utils; // 注意核对你的包名

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 终极波纹按钮：融合了少侠的物理按压、字体放大、阴影特效与水波纹引擎 主类按钮
 */
public class RippleButton extends JButton {

    private int mouseX, mouseY;
    private float rippleRadius = 0;
    private float rippleAlpha = 0f;
    private Timer rippleTimer;

    private Color themeColor; // 接收你传进来的主题色
    private boolean isHover = false;
    private boolean isPressedState = false;

    public RippleButton(String text, Color themeColor) {
        super(text);
        this.themeColor = themeColor;

        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setFont(new Font("微软雅黑", Font.BOLD, 20)); // 初始字体大小
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 水波纹动画引擎
        rippleTimer = new Timer(15, e -> {
            rippleRadius += 15;
            rippleAlpha -= 0.04f;
            if (rippleAlpha <= 0f) {
                rippleAlpha = 0f;
                rippleTimer.stop();
            }
            repaint();
        });

        // 鼠标事件监听
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                isPressedState = true;
                mouseX = e.getX();
                mouseY = e.getY();
                rippleRadius = 0;
                rippleAlpha = 0.5f; // 触发波纹
                rippleTimer.start();
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressedState = false; // 恢复物理按压
                repaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                isHover = true;
                setFont(new Font("微软雅黑", Font.BOLD, 22)); // ✨ 保留少侠的字体放大特效
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHover = false;
                setFont(new Font("微软雅黑", Font.BOLD, 20)); // ✨ 恢复字体
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // ✨ 保留少侠的动态透明度与物理位移量
        int alpha = isPressedState ? 220 : (isHover ? 200 : 150);
        int offset = isPressedState ? 2 : 0;

        // 1. 画带有透明度的主题底色 (25px 圆角)
        g2.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), alpha));
        g2.fillRoundRect(offset, offset, getWidth() - offset * 2, getHeight() - offset * 2, 25, 25);

        // 2. 设定限制区域（核心黑科技：让水波纹绝对不溢出 25px 的圆角边界）
        Shape clipShape = new java.awt.geom.RoundRectangle2D.Float(offset, offset, getWidth() - offset * 2, getHeight() - offset * 2, 25, 25);
        g2.setClip(clipShape);

        // 3. 绘制水波纹动画
        if (rippleAlpha > 0) {
            g2.setColor(new Color(255, 255, 255, (int) (rippleAlpha * 255)));
            int d = (int) (rippleRadius * 2);
            g2.fillOval(mouseX - (int) rippleRadius, mouseY - (int) rippleRadius, d, d);
        }

        g2.setClip(null); // 恢复正常画笔范围

        // 4. ✨ 保留少侠的反光边框
        g2.setStroke(new BasicStroke(1.8f));
        g2.setColor(new Color(255, 255, 255, isHover ? 120 : 70));
        g2.drawRoundRect(offset + 1, offset + 1, getWidth() - 3 - offset * 2, getHeight() - 3 - offset * 2, 25, 25);

        // 5. ✨ 保留少侠的立体阴影文字算法
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(getText())) / 2;
        int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;

        g2.setColor(new Color(0, 0, 0, 60)); // 画阴影
        g2.drawString(getText(), x + 1, y + 1 + offset);
        g2.setColor(Color.WHITE); // 画纯白正文
        g2.drawString(getText(), x, y + offset);

        g2.dispose();
    }
}