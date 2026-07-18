package shaoxia.modules;

import shaoxia.MainLauncher;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.prefs.Preferences;

public class BackgroundPanel extends JPanel {
    private Image originalImg;

    public BackgroundPanel(String defaultPath) {
        setOpaque(false);
        Preferences prefs = Preferences.userNodeForPackage(MainLauncher.class);
        String savedPath = prefs.get("bg_image_path", defaultPath);
        loadImage(savedPath);
    }

    public void loadImage(String path) {
        java.net.URL imgUrl = getClass().getClassLoader().getResource(path);
        if (imgUrl != null) {
            originalImg = new ImageIcon(imgUrl).getImage();
        } else {
            // 支持读取本地绝对路径的自定义图片
            File localFile = new File(path);
            if (localFile.exists() && !localFile.isDirectory()) {
                originalImg = new ImageIcon(localFile.getAbsolutePath()).getImage();
            } else {
                System.err.println("找不到图片资源: " + path);
                originalImg = null;
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int panelW = getWidth();
        int panelH = getHeight();

        if (originalImg != null) {
            Graphics2D g2 = (Graphics2D) g.create();

            int imgW = originalImg.getWidth(this);
            int imgH = originalImg.getHeight(this);

            if (imgW > 0 && imgH > 0) {
                // 计算等比例缩放的尺寸，保证图片铺满且不变形
                double scale = Math.max((double) panelW / imgW, (double) panelH / imgH);
                int drawW = (int) (imgW * scale);
                int drawH = (int) (imgH * scale);

                // 让图片完美居中
                int x = (panelW - drawW) / 2;
                int y = (panelH - drawH) / 2;

                // 彻底抛弃内存缓存，直接将原始无损图片交给底层显卡/原生接口实时绘制！
                g2.drawImage(originalImg, x, y, drawW, drawH, this);
            }

            // 绘制自定明暗度的毛玻璃黑纱遮罩
            Preferences prefs = Preferences.userNodeForPackage(MainLauncher.class);
            int darkness = prefs.getInt("bg_darkness", 120);

            g2.setColor(new Color(0, 0, 0, darkness));
            g2.fillRect(0, 0, panelW, panelH);

            g2.dispose();
        } else {
            g.setColor(new Color(35, 35, 35));
            g.fillRect(0, 0, panelW, panelH);
        }
    }
}