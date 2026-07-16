package shaoxia.modules;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.prefs.Preferences;

public class BackgroundPanel extends JPanel {
    private Image originalImg;
    private BufferedImage cachedScaledImg; // 缓存缩放后的图片

    public BackgroundPanel(String defaultPath) {
        setOpaque(false);
        Preferences prefs = Preferences.userNodeForPackage(shaoxia.MainLauncher.class);
        String savedPath = prefs.get("bg_image_path", defaultPath);
        loadImage(savedPath);

        // 监听窗口尺寸变化，动态重新生成缓存
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateCachedImage();
            }
        });
    }

    public void loadImage(String path) {
        java.net.URL imgUrl = getClass().getClassLoader().getResource(path);
        if (imgUrl != null) {
            originalImg = new ImageIcon(imgUrl).getImage();
            updateCachedImage(); // 加载新图后立即更新缓存
        } else {
            System.err.println("找不到图片资源: " + path);
            originalImg = null;
            cachedScaledImg = null;
        }
        repaint();
    }

    private void updateCachedImage() {
        if (originalImg == null || getWidth() <= 0 || getHeight() <= 0) return;

        int panelW = getWidth();
        int panelH = getHeight();
        int imgW = originalImg.getWidth(this);
        int imgH = originalImg.getHeight(this);

        if (imgW <= 0 || imgH <= 0) return;

        double scale = Math.max((double) panelW / imgW, (double) panelH / imgH);
        int drawW = (int) (imgW * scale);
        int drawH = (int) (imgH * scale);

        // 创建一张支持透明度的内存图像作为缓存
        cachedScaledImg = new BufferedImage(panelW, panelH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = cachedScaledImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(originalImg, (panelW - drawW) / 2, (panelH - drawH) / 2, drawW, drawH, null);

        // 顺便把毛玻璃暗色遮罩也画进缓存里，一劳永逸
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(0, 0, panelW, panelH);
        g2.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (cachedScaledImg != null) {
            // O(1) 极速绘制，告别计算！
            g.drawImage(cachedScaledImg, 0, 0, null);
        } else {
            g.setColor(new Color(35, 35, 35));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}