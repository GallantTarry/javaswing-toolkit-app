package shaoxia.Utils;

import javax.swing.*;
import java.awt.*;

/**
 * 全局通用的高质感毛玻璃拖拽/监听面板
 * 采用全局统一黑雾色底色，外加双层边框（实线白霜 + 内部虚线）
 */
public class GlassDropPanel extends JPanel {

    public GlassDropPanel() {
        this(new BorderLayout());
    }

    public GlassDropPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        // 默认设置好内边距，防止内部的文字或组件贴边
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. 核心底色：直接调用咱们全局统一的黑雾玻璃色
        g2.setColor(ActionButton.DEFAULT_GLASS_COLOR);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);

        // 2. 外部高光：1.5f 粗细的白霜实线描边
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(new Color(255, 255, 255, 60));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);

        // 3. 内部环绕：标志性的呼吸感虚线
        Stroke dashed = new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{12.0f, 8.0f}, 0.0f);
        g2.setStroke(dashed);
        g2.setColor(new Color(255, 255, 255, 120));
        g2.drawRoundRect(8, 8, getWidth() - 17, getHeight() - 17, 22, 22);

        g2.dispose();
        super.paintComponent(g);
    }
}