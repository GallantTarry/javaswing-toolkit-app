package shaoxia.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;

/**
 * 全局通用的高质感毛玻璃输入框
 * 完美融合版：15px 圆润弧度 + 全局统一黑雾底色 + 底层防撕裂
 */
public class GlassTextField extends JTextField {

    public static final Color DEFAULT_GLASS_COLOR = new Color(30, 35, 40, 120);

    public GlassTextField() {
        super();
        setUI(new javax.swing.plaf.basic.BasicTextFieldUI());

        setOpaque(false);
        setBackground(new Color(0, 0, 0, 0));
        setForeground(Color.WHITE);
        setCaretColor(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        setFont(new Font("微软雅黑", Font.PLAIN, 14));

        // 【底层抗撕裂核心 1】：监听中文输入法的组字状态，强迫父容器重绘
        addInputMethodListener(new InputMethodListener() {
            @Override
            public void inputMethodTextChanged(InputMethodEvent event) { forceRepaint(); }
            @Override
            public void caretPositionChanged(InputMethodEvent event) { forceRepaint(); }
        });
    }

    private void forceRepaint() {
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w != null) w.repaint();
        else repaint();
    }

    // 【底层抗撕裂核心 2】：拦截局部重绘请求，转为全局重绘，杜绝光标闪烁残影
    @Override
    public void repaint(long tm, int x, int y, int width, int height) {
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w != null) {
            w.repaint();
        } else {
            super.repaint(tm, x, y, width, height);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(DEFAULT_GLASS_COLOR);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
        super.paintComponent(g);
        g2.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(255, 255, 255, 60));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
        g2.dispose();
    }

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
                g2.setColor(new Color(20, 20, 20, 220));
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