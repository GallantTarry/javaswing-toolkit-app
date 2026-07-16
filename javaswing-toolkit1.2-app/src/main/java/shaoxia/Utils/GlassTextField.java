package shaoxia.Utils;

import javax.swing.*;
import java.awt.*;

/**
 * 全局通用的高质感毛玻璃输入框 仅仅是输入框
 * 完美融合版：15px 圆润弧度 + 全局统一黑雾底色 + 自定义黑透悬浮提示框
 */
public class GlassTextField extends JTextField {

    // 保持不变：提取自全局标准的黑雾半透玻璃色
    public static final Color DEFAULT_GLASS_COLOR = new Color(30, 35, 40, 120);

    public GlassTextField() {
        super();
        // 拦截系统默认UI，防止 Windows/Mac 底层强行给文本框填充白底
        setUI(new javax.swing.plaf.basic.BasicTextFieldUI());

        // 剥离原生底色属性，将绘制权交由 paintComponent 接管
        setOpaque(false);
        setBackground(new Color(0, 0, 0, 0));

        setForeground(Color.WHITE);
        setCaretColor(Color.WHITE);

        // 保持 ReplacerPanel 里的呼吸边距
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        setFont(new Font("微软雅黑", Font.PLAIN, 14));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // ✨ 灰度保持不变：使用全局的深邃黑雾色
        g2.setColor(DEFAULT_GLASS_COLOR);

        // ✨ 弧度改变：使用 ReplacerPanel 中的 15px 圆润大圆角
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

        super.paintComponent(g);
        g2.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 纯净的白霜描边，紧贴 15px 圆角
        g2.setColor(new Color(255, 255, 255, 60));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);

        g2.dispose();
    }

    // --- 保留原汁原味的鼠标悬停提示 (ToolTip) 美化 ---
    @Override
    public String getToolTipText() {
        String text = getText().trim();
        if (!text.isEmpty()) {
            return "<html><body style='font-family:微软雅黑; font-size:13px; padding:2px; color:white;'>"
                    + "<b>当前输入内容：</b><br>" + text + "</body></html>";
        }
        String defaultTip = super.getToolTipText();
        if (defaultTip != null) {
            return "<html><body style='font-family:微软雅黑; font-size:13px; padding:2px; color:white;'>"
                    + defaultTip + "</body></html>";
        }
        return null;
    }

    @Override
    public JToolTip createToolTip() {
        JToolTip tip = new JToolTip() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(20, 20, 20, 220)); // 深色提示框底色
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tip.setComponent(this);
        tip.setOpaque(false);
        tip.setBackground(new Color(0, 0, 0, 0));
        tip.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        return tip;
    }
}