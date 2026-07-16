package shaoxia.Utils;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolTipUI;
import java.awt.*;

public class GlassToolTipUI extends BasicToolTipUI {
    public static ComponentUI createUI(JComponent c) { return new GlassToolTipUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        JToolTip tip = (JToolTip) c;
        tip.setOpaque(false); // 必须透明，否则会露底色
        tip.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 黑雾玻璃色：与你之前设置的 GlassTextField 保持一致
        g2.setColor(new Color(30, 35, 40, 220));
        g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 15, 15);

        // 白霜边框
        g2.setColor(new Color(255, 255, 255, 60));
        g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 15, 15);

        g2.dispose();
        super.paint(g, c); // 最后画文字
    }
}