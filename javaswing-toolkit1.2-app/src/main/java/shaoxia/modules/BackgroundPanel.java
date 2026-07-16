package shaoxia.modules;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * 背景面板组件 (支持动态读取与实时切换壁纸)
 */
public class BackgroundPanel extends JPanel {
    private Image img;

    public BackgroundPanel(String defaultPath) {
        setOpaque(false);
        // 从本地偏好设置中读取用户选择的背景图，如果没有设置过，就用传入的 defaultPath
        Preferences prefs = Preferences.userNodeForPackage(shaoxia.MainLauncher.class);
        String savedPath = prefs.get("bg_image_path", defaultPath);

        // 执行图片加载
        loadImage(savedPath);
    }

    /**
     * 新增：动态加载图片并刷新面板的方法
     * @param path 图片在 resources 下的相对路径
     */
    public void loadImage(String path) {
        java.net.URL imgUrl = getClass().getClassLoader().getResource(path);

        if (imgUrl != null) {
            img = new ImageIcon(imgUrl).getImage();
        } else {
            System.err.println("找不到图片资源: " + path);
            img = null; // 找不到时清空，走备用纯色逻辑
        }
        repaint(); // 通知底层重新绘制界面，实现瞬间切换
    }

    @Override
    protected void paintComponent(Graphics g) {
        // 建议将 super.paintComponent(g) 移到最前面，这是 Swing 的标准规范，防止图层覆盖问题
        super.paintComponent(g);

        if (img != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int panelW = getWidth(), panelH = getHeight(), imgW = img.getWidth(this), imgH = img.getHeight(this);
            if (imgW > 0 && imgH > 0) {
                double scale = Math.max((double) panelW / imgW, (double) panelH / imgH);
                int drawW = (int) (imgW * scale), drawH = (int) (imgH * scale);
                g2.drawImage(img, (panelW - drawW) / 2, (panelH - drawH) / 2, drawW, drawH, this);
            }

            // === 【新增：降低背景明亮度的暗色遮罩】 ===
            // 第四个参数 120 是 Alpha 透明度，范围 0~255。
            // 0 代表全透明（原图亮度），255 代表纯黑（完全看不见图）。
            // 少侠可以根据按钮的玻璃质感自行上下微调这个数值。
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRect(0, 0, panelW, panelH);
            // ==========================================

            g2.dispose();
        } else {
            g.setColor(new Color(35, 35, 35));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}