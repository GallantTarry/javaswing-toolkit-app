package shaoxia;

import javafx.scene.layout.StackPane;
import java.net.URL;

/**
 * 背景面板组件 (JavaFX 现代化版)
 * 已彻底抛弃 Swing JPanel 和 Graphics2D 画笔重绘
 */
public class BackgroundPanel extends StackPane {

    public BackgroundPanel(String path) {
        super(); // 初始化 StackPane 容器

        // 1. 寻找图片资源 (保留了你原来的寻找逻辑)
        URL imgUrl = getClass().getClassLoader().getResource(path);

        if (imgUrl != null) {
            // 2. 将资源路径转化为 CSS 可识别的 URL 格式
            String imageUrl = imgUrl.toExternalForm();

            // 3. 降维打击：用几行 CSS 彻底替代原来 paintComponent 里的复杂矩阵缩放算法
            // -fx-background-size: cover 完美等价于你写的 double scale = Math.max(...) 逻辑
            this.setStyle(
                    "-fx-background-image: url('" + imageUrl + "'); " +
                            "-fx-background-size: cover; " +
                            "-fx-background-position: center center; " +
                            "-fx-background-repeat: no-repeat;"
            );
        } else {
            System.err.println("找不到图片资源: " + path);
            // 容错处理：如果图片丢了，给一个优雅的极暗夜色底色
            this.setStyle("-fx-background-color: #2B2B2B;");
        }
    }
}