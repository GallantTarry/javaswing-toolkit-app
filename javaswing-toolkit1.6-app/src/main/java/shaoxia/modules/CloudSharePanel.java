package shaoxia.modules;

import shaoxia.MainLauncher;
import shaoxia.Utils.ActionButton;
import shaoxia.Utils.GlassDropPanel;
import shaoxia.Utils.GlassPanel;
import shaoxia.Utils.GlassTextField;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

// --- ZXing 二维码库相关导入 ---
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 极速云传面板 (原生 SSH 穿透版 + 文件夹动态打包 + 纯CSS暗黑渐变美学)
 * 启动即挂载、无悬浮提示、原生 SSH 白嫖公网节点、Web端零流量背景、支持移动端扫码直连
 */
public class CloudSharePanel extends BackgroundPanel {
    private MainLauncher parent;
    private List<File> sharedFiles = new ArrayList<>();

    // 网络服务底层
    private HttpServer localServer;
    private Process sshProcess;
    private final int SHARE_PORT = 6506;

    // UI 组件
    private JLabel fileStatusLabel;
    private GlassTextField lanUrlField;
    private GlassTextField wanUrlField;
    private JButton lanQrBtn;
    private JButton wanQrBtn;
    private CloudEngineIndicator engineIndicator;
    private GlassDropPanel dropArea;
    private ActionButton toggleShareBtn;

    // 引擎状态
    private volatile boolean isLocalReady = false;
    private volatile boolean isSshChecking = false;
    private volatile boolean isSshReady = false;

    public CloudSharePanel(MainLauncher parent) {
        // 桌面端依然保留你的门.png背景
        super("bg_imgs/门.png");
        this.parent = parent;
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        // 注册 JVM 停机钩子：当软件强行关闭时，无条件斩杀 SSH 穿透进程
        Runtime.getRuntime().addShutdownHook(new Thread(this::killSshProcess));

        initTopBar();
        initCoreWorkspace();

        // 界面加载完毕后，立刻启动全局服务！
        startSharing();
    }

    private void initTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 20));

        ActionButton backBtn = new ActionButton("<< 返回菜单");
        backBtn.setPreferredSize(new Dimension(130, 36));
        backBtn.addActionListener(e -> {
            stopSharing();
            parent.showMenu();
        });

        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftTop.setOpaque(false);
        leftTop.add(backBtn);
        topBar.add(leftTop, BorderLayout.WEST);

        // 引入右上角动态矢量云朵指示灯
        engineIndicator = new CloudEngineIndicator();
        engineIndicator.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                redeploySshTunnel();
            }
        });
        topBar.add(engineIndicator, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);
    }

    private void initCoreWorkspace() {
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 60, 30, 60));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        // ================= 1. 监听窗口 (权重占比 3) =================
        dropArea = new GlassDropPanel(new BorderLayout());
        dropArea.setOpaque(false);
        dropArea.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        fileStatusLabel = new JLabel("拖入任意文件/文件夹实现热挂载 (双击清空)", SwingConstants.CENTER);
        fileStatusLabel.setForeground(new Color(200, 200, 200));
        fileStatusLabel.setFont(new Font("微软雅黑", Font.BOLD, 20));
        dropArea.add(fileStatusLabel, BorderLayout.CENTER);

        dropArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    sharedFiles.clear();
                    updateFileStatus();
                }
            }
        });

        dropArea.setDropTarget(new DropTarget(dropArea, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = dtde.getTransferable();
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        if (files != null && !files.isEmpty()) {
                            // 停止递归铺开，直接按整体存入，实现文件夹总览
                            for (File f : files) {
                                if (!sharedFiles.contains(f)) sharedFiles.add(f);
                            }
                            updateFileStatus();
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }));

        gbc.gridy = 0;
        gbc.weighty = 3.0;
        gbc.insets = new Insets(0, 0, 15, 0);
        centerPanel.add(dropArea, gbc);

        // ================= 2. LAN 直连框 (权重占比 1) =================
        GlassPanel lanPanel = new GlassPanel(new BorderLayout(10, 0));
        lanPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
        JLabel lanLabel = new JLabel("LAN 直连:");
        lanLabel.setForeground(new Color(180, 180, 180));
        lanLabel.setFont(new Font("微软雅黑", Font.BOLD, 15));
        lanLabel.setPreferredSize(new Dimension(95, 30));

        lanUrlField = new GlassTextField();
        lanUrlField.setEditable(false);
        lanUrlField.setText("引擎预热中...");
        lanUrlField.setFont(new Font("微软雅黑", Font.BOLD, 14));
        lanUrlField.setForeground(Color.WHITE);
        bindClickCopyAndJump(lanUrlField);

        lanQrBtn = createRoundedRectButton("扫码");
        lanQrBtn.addActionListener(e -> showQRCode(lanUrlField.getText()));

        lanPanel.add(lanLabel, BorderLayout.WEST);
        lanPanel.add(lanUrlField, BorderLayout.CENTER);
        lanPanel.add(lanQrBtn, BorderLayout.EAST);

        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 15, 0);
        centerPanel.add(lanPanel, gbc);

        // ================= 3. WAN 穿透框 (权重占比 1) =================
        GlassPanel wanPanel = new GlassPanel(new BorderLayout(10, 0));
        wanPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
        JLabel wanLabel = new JLabel("WAN 穿透:");
        wanLabel.setForeground(new Color(180, 180, 180));
        wanLabel.setFont(new Font("微软雅黑", Font.BOLD, 15));
        wanLabel.setPreferredSize(new Dimension(95, 30));

        wanUrlField = new GlassTextField();
        wanUrlField.setEditable(false);
        wanUrlField.setText("正在构建 SSH 本地隧道...");
        wanUrlField.setFont(new Font("微软雅黑", Font.BOLD, 14));
        wanUrlField.setForeground(Color.WHITE);
        bindClickCopyAndJump(wanUrlField);

        wanQrBtn = createRoundedRectButton("扫码");
        wanQrBtn.addActionListener(e -> showQRCode(wanUrlField.getText()));

        wanPanel.add(wanLabel, BorderLayout.WEST);
        wanPanel.add(wanUrlField, BorderLayout.CENTER);
        wanPanel.add(wanQrBtn, BorderLayout.EAST);

        gbc.gridy = 2;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 20, 0);
        centerPanel.add(wanPanel, gbc);

        // ================= 4. 底部总控按钮 =================
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnPanel.setOpaque(false);

        toggleShareBtn = new ActionButton("关闭引擎");
        toggleShareBtn.setPreferredSize(new Dimension(200, 45));
        toggleShareBtn.setFont(new Font("微软雅黑", Font.BOLD, 16));
        toggleShareBtn.addActionListener(e -> {
            if (isLocalReady) stopSharing();
            else startSharing();
        });
        btnPanel.add(toggleShareBtn);

        gbc.gridy = 3;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 0);
        centerPanel.add(btnPanel, gbc);

        add(centerPanel, BorderLayout.CENTER);
    }

    // ================= 自定义 UI 组件 =================

    /**
     * 创建一个和前方输入框弧度一致，同时继承 ActionButton 黑雾风格的圆角矩形按钮
     */
    private JButton createRoundedRectButton(String text) {
        JButton btn = new JButton(text) {
            private final Color bgColor = new Color(30, 35, 40, 120); // ActionButton 统一黑雾玻璃色

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean isPressed = getModel().isPressed();
                boolean isHover = getModel().isRollover();
                Color drawColor = bgColor;

                // 悬停/点击时的透明度明暗变化，与 ActionButton 保持绝对一致
                if (isPressed) {
                    int newAlpha = Math.min(255, bgColor.getAlpha() + 40);
                    drawColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), newAlpha);
                } else if (isHover) {
                    int newAlpha = Math.max(0, bgColor.getAlpha() - 40);
                    drawColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), newAlpha);
                }

                // 形状不变：和 GlassTextField 严格统一的 15px 圆角矩形
                int arc = 15;

                // 画底色
                g2.setColor(drawColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

                // 画边框：采用 ActionButton 的高光白霜描边
                g2.setColor(new Color(255, 255, 255, 60));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("微软雅黑", Font.BOLD, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(80, 30));
        return btn;
    }

    // ================= 核心网络识别引擎 =================

    private String getLocalIPv4() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    // ================= 工具方法 =================

    // 递归计算文件/文件夹真实总大小
    public static long calculateFolderSize(File file) {
        if (file == null || !file.exists()) return 0;
        if (file.isFile()) return file.length();

        long size = 0;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                size += calculateFolderSize(child);
            }
        }
        return size;
    }

    private void updateFileStatus() {
        if (sharedFiles.isEmpty()) {
            fileStatusLabel.setText("拖入任意文件/文件夹实现热挂载 (双击清空)");
            fileStatusLabel.setForeground(new Color(200, 200, 200));
            return;
        }
        long totalSize = 0;
        for (File f : sharedFiles) {
            totalSize += calculateFolderSize(f);
        }
        fileStatusLabel.setText("已挂载 " + sharedFiles.size() + " 个项目 (共 " + formatSize(totalSize) + ") - 运行中");
        fileStatusLabel.setForeground(Color.WHITE);
    }

    private static String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024.0));
        return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
    }

    private void bindClickCopyAndJump(JTextField field) {
        field.setCursor(new Cursor(Cursor.HAND_CURSOR));
        field.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    String url = field.getText().trim();
                    if (url.startsWith("http")) {
                        StringSelection selection = new StringSelection(url);
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                        try { Desktop.getDesktop().browse(new URI(url)); } catch (Exception ignored) {}
                    }
                }
            }
        });
    }

    // ================= 弹窗渲染二维码逻辑 =================

    private void showQRCode(String url) {
        // 如果文本框内容不是合法链接（比如正在加载中、引擎已关闭），则拦截弹窗
        if (url == null || url.trim().isEmpty() || !url.startsWith("http")) {
            JOptionPane.showMessageDialog(this,
                    "引擎尚未就绪或链接无效，请等待引擎预热完毕！",
                    "未就绪",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = new MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, 250, 250, hints);
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            BufferedImage rawImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    // 黑底透明交错处理，融合弹窗质感
                    rawImage.setRGB(x, y, matrix.get(x, y) ? 0xFF111111 : 0xFFFFFFFF);
                }
            }

            // === 增加圆弧正方形渲染逻辑 ===
            int arc = 25; // 边缘圆角弧度，可根据喜好调节
            BufferedImage roundedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = roundedImage.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 设置圆角裁剪区域并将原始二维码绘入
            g2.setClip(new RoundRectangle2D.Float(0, 0, width, height, arc, arc));
            g2.drawImage(rawImage, 0, 0, null);

            // 取消裁剪，补充一个柔和的外边框，提升弹窗内的独立质感
            g2.setClip(null);
            g2.setColor(new Color(200, 200, 200, 80));
            g2.drawRoundRect(0, 0, width - 1, height - 1, arc, arc);
            g2.dispose();

            JOptionPane.showMessageDialog(this,
                    new JLabel(new ImageIcon(roundedImage)),
                    "扫描直连",
                    JOptionPane.PLAIN_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "二维码生成失败", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================= 核心网络逻辑 =================

    private void startSharing() {
        try {
            if (localServer == null) {
                localServer = HttpServer.create(new InetSocketAddress(SHARE_PORT), 0);
                localServer.createContext("/", new IndexHandler(sharedFiles));
                localServer.createContext("/download", new FileTransportHandler(sharedFiles, true));
                localServer.createContext("/view", new FileTransportHandler(sharedFiles, false));
                localServer.createContext("/favicon.ico", new FaviconHandler());

                localServer.setExecutor(Executors.newFixedThreadPool(20));
                localServer.start();
            }

            lanUrlField.setText("http://" + getLocalIPv4() + ":" + SHARE_PORT + "/");
            isLocalReady = true;

            toggleShareBtn.setText("关闭服务");

            redeploySshTunnel();

        } catch (Exception ex) {
            lanUrlField.setText("本地端口分配失败: 6506可能被占用");
            isLocalReady = false;
        }
    }

    private void stopSharing() {
        if (localServer != null) {
            localServer.stop(0);
            localServer = null;
        }
        killSshProcess();
        isLocalReady = false;
        isSshReady = false;
        isSshChecking = false;
        engineIndicator.repaint();

        lanUrlField.setText("服务已关闭");
        wanUrlField.setText("隧道已销毁");
        toggleShareBtn.setText("启动服务");
    }

    private void killSshProcess() {
        if (sshProcess != null && sshProcess.isAlive()) {
            sshProcess.destroyForcibly();
            sshProcess = null;
        }
    }

    private void redeploySshTunnel() {
        if (!isLocalReady) return;
        killSshProcess();
        startSshTunnel();
    }

    // ✨ 核心机制：利用原生 SSH 建立穿透隧道
    private void startSshTunnel() {
        wanUrlField.setText("正在向公网节点请求 SSH 映射...");
        isSshChecking = true;
        isSshReady = false;
        engineIndicator.repaint();

        new Thread(() -> {
            try {
                ProcessBuilder builder = new ProcessBuilder(
                        "ssh",
                        "-o", "StrictHostKeyChecking=no",
                        "-R", "80:localhost:" + SHARE_PORT,
                        "nokey@localhost.run"
                );
                builder.redirectErrorStream(true);
                sshProcess = builder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(sshProcess.getInputStream()));
                String line;

                Pattern urlPattern = Pattern.compile("(https?://[a-zA-Z0-9-]+\\.lhr\\.life)");
                boolean urlFound = false;

                while ((line = reader.readLine()) != null) {
                    if (!urlFound) {
                        Matcher matcher = urlPattern.matcher(line);
                        if (matcher.find()) {
                            String publicUrl = matcher.group(1) + "/";
                            urlFound = true;
                            isSshChecking = false;
                            isSshReady = true;
                            SwingUtilities.invokeLater(() -> {
                                wanUrlField.setText(publicUrl);
                                engineIndicator.repaint();
                            });
                        }
                    }
                }
            } catch (Exception e) {
                isSshChecking = false;
                SwingUtilities.invokeLater(() -> {
                    wanUrlField.setText("[调度异常] 当前系统环境缺失原生 SSH 服务");
                    engineIndicator.repaint();
                });
            }
        }).start();
    }

    // ================= 高级纯矢量 iCloud 风格云朵渲染器 =================

    class CloudEngineIndicator extends JComponent {
        private Timer pulseTimer;
        private double pulseAngle = 0;

        public CloudEngineIndicator() {
            setPreferredSize(new Dimension(50, 50));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            pulseTimer = new Timer(50, e -> {
                if (isSshChecking || isSshReady) {
                    pulseAngle += 0.1;
                    repaint();
                }
            });
            pulseTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            Color cloudColor;
            Color glowColor;

            if (isSshReady) {
                cloudColor = new Color(52, 152, 219);
                glowColor = new Color(52, 152, 219, 80);
            } else if (isSshChecking) {
                cloudColor = new Color(243, 156, 18);
                glowColor = new Color(243, 156, 18, 60);
            } else {
                cloudColor = new Color(110, 115, 120);
                glowColor = new Color(110, 115, 120, 30);
            }

            if (isSshReady || isSshChecking) {
                int pulseRadius = 13 + (int)(Math.sin(pulseAngle) * 3);
                g2.setColor(new Color(cloudColor.getRed(), cloudColor.getGreen(), cloudColor.getBlue(), 30));
                g2.fillOval(cx - pulseRadius, cy - pulseRadius, pulseRadius * 2, pulseRadius * 2);
            }

            g2.setColor(glowColor);
            g2.fillOval(cx - 15, cy - 15, 30, 30);

            g2.setColor(cloudColor);
            int x = cx - 11;
            int y = cy - 5;
            g2.fillRoundRect(x, y + 4, 22, 8, 8, 8);
            g2.fillOval(x + 2, y - 3, 12, 12);
            g2.fillOval(x + 11, y, 9, 9);

            g2.dispose();
        }
    }

    // ================= Web 渲染与数据流传输 =================

    static class IndexHandler implements HttpHandler {
        private final List<File> files;
        public IndexHandler(List<File> files) { this.files = files; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html lang='zh-CN'><head><meta charset='UTF-8'>")
                    .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
                    .append("<title>ToolKit 极速云传</title>")
                    .append("<link rel='shortcut icon' href='/favicon.ico' type='image/x-icon'>")
                    .append("<style>")
                    .append(":root { --primary: #333333; --text: #1e293b; --muted: #e2e8f0; } ")
                    .append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: radial-gradient(circle at 50% 0%, #334155 0%, #0f172a 60%, #020617 100%); background-attachment: fixed; color: var(--text); padding: 2rem 1rem; margin: 0; } ")
                    .append(".container { max-width: 800px; margin: 0 auto; } ")
                    .append(".header { text-align: center; margin-bottom: 3rem; color: white; text-shadow: 0 2px 8px rgba(0,0,0,0.8); } ")
                    .append(".header h1 { font-size: 2.2rem; margin: 0; font-weight: 700; letter-spacing: 1px; } ")
                    .append(".header p { color: #cbd5e1; margin-top: 8px; } ")
                    .append(".file-list { display: grid; gap: 1rem; } ")
                    .append(".file-card { background: rgba(255, 255, 255, 0.9); backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px); border-radius: 12px; padding: 16px 24px; display: flex; align-items: center; box-shadow: 0 8px 16px rgba(0,0,0,0.3); transition: all 0.2s ease; border: 1px solid rgba(255,255,255,0.15); } ")
                    .append(".file-card:hover { transform: translateY(-3px); box-shadow: 0 12px 20px rgba(0,0,0,0.4); } ")
                    .append(".icon { font-size: 28px; margin-right: 20px; } ")
                    .append(".info { flex: 1; min-width: 0; } ")
                    .append(".name { font-weight: 600; font-size: 1.1rem; margin: 0 0 4px 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: #0f172a; } ")
                    .append(".meta { font-size: 0.85rem; color: #475569; margin: 0; } ")
                    .append(".actions { display: flex; gap: 8px; margin-left: 15px; } ")
                    .append(".btn { padding: 8px 16px; border-radius: 8px; font-size: 0.9rem; font-weight: 600; text-decoration: none; transition: all 0.15s ease; border: none; } ")
                    .append(".btn-view { background: #e2e8f0; color: #334155; } ")
                    .append(".btn-view:hover { background: #cbd5e1; } ")
                    .append(".btn-down { background: var(--primary); color: white; } ")
                    .append(".btn-down:hover { background: #000000; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.4); } ")
                    .append(".empty-state { text-align: center; padding: 3rem; color: rgba(255,255,255,0.7); font-size: 1.1rem; border: 2px dashed rgba(255,255,255,0.3); border-radius: 12px; background: rgba(0,0,0,0.2); backdrop-filter: blur(5px); } ")
                    .append("</style></head><body>")
                    .append("<div class='container'><div class='header'><h1>🚀 ToolKit 极速云传</h1><p>安全链路映射中，实时拉取云端文件</p></div><div class='file-list'>");

            if (files.isEmpty()) {
                html.append("<div class='empty-state'>主机目前尚未挂载任何文件，请稍候刷新...</div>");
            } else {
                for (int i = 0; i < files.size(); i++) {
                    File f = files.get(i);
                    String name = f.getName();
                    boolean isDir = f.isDirectory();
                    String ext = getExtension(name).toLowerCase();

                    String icon = isDir ? "📁" : "📄";
                    if (!isDir) {
                        if (ext.matches("(jpg|jpeg|png|gif|webp|bmp)")) icon = "🖼️";
                        else if (ext.matches("(zip|rar|7z|tar|gz)")) icon = "📦";
                        else if (ext.matches("(mp4|mkv|avi|mov)")) icon = "🎥";
                        else if (ext.matches("(mp3|wav|flac)")) icon = "🎵";
                        else if (ext.matches("(java|py|json|xml|html|css|js|c|cpp)")) icon = "🧑‍💻";
                    }

                    String metaInfo = isDir ? "文件夹 (" + formatSize(calculateFolderSize(f)) + ")" : formatSize(f.length());
                    boolean canPreview = !isDir && ext.matches("(jpg|jpeg|png|gif|webp|bmp|txt|pdf)");

                    html.append("<div class='file-card'>")
                            .append("<div class='icon'>").append(icon).append("</div>")
                            .append("<div class='info'>")
                            .append("<p class='name' title='").append(name).append("'>").append(name).append("</p>")
                            .append("<p class='meta'>").append(metaInfo).append("</p>")
                            .append("</div><div class='actions'>");

                    if (canPreview) {
                        html.append("<a href='/view?id=").append(i).append("' class='btn btn-view' target='_blank'>预览</a>");
                    }
                    html.append("<a href='/download?id=").append(i).append("' class='btn btn-down'>下载</a>")
                            .append("</div></div>");
                }
            }
            html.append("</div></div></body></html>");

            byte[] response = html.toString().getBytes("UTF-8");
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(response); }
        }

        private String getExtension(String fileName) {
            int idx = fileName.lastIndexOf('.');
            return (idx > 0) ? fileName.substring(idx + 1) : "";
        }
    }

    static class FileTransportHandler implements HttpHandler {
        private final List<File> files;
        private final boolean isDownload;

        public FileTransportHandler(List<File> files, boolean isDownload) {
            this.files = files;
            this.isDownload = isDownload;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            int id = -1;
            if (query != null && query.startsWith("id=")) {
                try { id = Integer.parseInt(query.substring(3)); } catch (Exception ignored) {}
            }

            if (id >= 0 && id < files.size()) {
                File file = files.get(id);
                String encodeName = URLEncoder.encode(file.getName(), "UTF-8").replaceAll("\\+", "%20");

                if (file.isDirectory()) {
                    if (!isDownload) {
                        exchange.sendResponseHeaders(403, -1);
                        return;
                    }
                    exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename*=UTF-8''" + encodeName + ".zip");
                    exchange.getResponseHeaders().add("Content-Type", "application/zip");
                    exchange.sendResponseHeaders(200, 0);

                    try (OutputStream os = exchange.getResponseBody();
                         java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(os)) {
                        zipDirectory(file, file.getName(), zos);
                    }
                    return;
                }

                if (isDownload) {
                    exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename*=UTF-8''" + encodeName);
                } else {
                    exchange.getResponseHeaders().add("Content-Disposition", "inline; filename*=UTF-8''" + encodeName);
                    String ext = file.getName().substring(file.getName().lastIndexOf('.') + 1).toLowerCase();
                    exchange.getResponseHeaders().add("Content-Type", getMimeType(ext));
                }
                exchange.sendResponseHeaders(200, file.length());
                try (OutputStream os = exchange.getResponseBody();
                     FileInputStream fs = new FileInputStream(file)) {
                    byte[] buffer = new byte[16384];
                    int count;
                    while ((count = fs.read(buffer)) >= 0) { os.write(buffer, 0, count); }
                }
            } else {
                String error = "404 - 资源已断开连接";
                exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
                exchange.sendResponseHeaders(404, error.getBytes("UTF-8").length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(error.getBytes("UTF-8")); }
            }
        }

        private void zipDirectory(File fileToZip, String fileName, java.util.zip.ZipOutputStream zos) throws IOException {
            if (fileToZip.isHidden()) return;
            if (fileToZip.isDirectory()) {
                if (fileName.endsWith("/")) {
                    zos.putNextEntry(new java.util.zip.ZipEntry(fileName));
                } else {
                    zos.putNextEntry(new java.util.zip.ZipEntry(fileName + "/"));
                }
                zos.closeEntry();
                File[] children = fileToZip.listFiles();
                if (children != null) {
                    for (File childFile : children) {
                        zipDirectory(childFile, fileName + "/" + childFile.getName(), zos);
                    }
                }
                return;
            }
            java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(fileName);
            zos.putNextEntry(zipEntry);
            try (FileInputStream fis = new FileInputStream(fileToZip)) {
                byte[] bytes = new byte[16384];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zos.write(bytes, 0, length);
                }
            }
        }

        private String getMimeType(String ext) {
            switch (ext) {
                case "jpg": case "jpeg": return "image/jpeg";
                case "png": return "image/png";
                case "gif": return "image/gif";
                case "webp": return "image/webp";
                case "pdf": return "application/pdf";
                case "txt": return "text/plain; charset=utf-8";
                default: return "application/octet-stream";
            }
        }
    }

    static class FaviconHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (InputStream is = CloudSharePanel.class.getResourceAsStream("/logo_app.ico")) {
                if (is == null) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                byte[] bytes = is.readAllBytes();
                exchange.getResponseHeaders().add("Content-Type", "image/x-icon");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            } catch (Exception e) {
                exchange.sendResponseHeaders(500, -1);
            }
        }
    }
}