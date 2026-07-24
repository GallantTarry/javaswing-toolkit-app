package shaoxia.modules;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import shaoxia.MainLauncher;
import shaoxia.Utils.ActionButton;
import shaoxia.Utils.GlassPanel;
import shaoxia.Utils.GlassTextField;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 文档替换助手 ReplacerPanel - 强韧防御版 (全局统一极暗玻璃 + 完美拖拽穿透 + 纯黑模式) */
public class ReplacerPanel extends BackgroundPanel {
    private JTextField[] inputs = new JTextField[6];
    private JTextField customKeyInput;
    private final String[] placeholders = {
            "XXX项目", "NARI-XXXXXX", "2026年XX月XX日", "XXXXXXXXXXX", "****@qq.com"
    };
    private JTextArea logArea;

    // ✨ 新增：纯黑模式状态标志
    private volatile boolean isBlackModeEnabled = true;

    public ReplacerPanel(MainLauncher parent) {
        super("bg_imgs/月球与地球.png");

        ToolTipManager.sharedInstance().setInitialDelay(200);
        ToolTipManager.sharedInstance().setDismissDelay(15000);

        setLayout(new BorderLayout());

        // 顶部导航：完全对标其他面板的嵌套结构与边缘距离参数
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 20));

        ActionButton backBtn = new ActionButton("<< 返回菜单");
        backBtn.setPreferredSize(new Dimension(130, 36));
        backBtn.addActionListener(e -> parent.showMenu());

        // 取消 FlowLayout 默认的 5px 间隙，实现精确像素对齐
        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftTop.setOpaque(false);
        leftTop.add(backBtn);
        topBar.add(leftTop, BorderLayout.WEST);

        // ✨ 新增：右上角纯黑模式指示灯组件接入
        BlackModeIndicator blackModeIndicator = new BlackModeIndicator();
        topBar.add(blackModeIndicator, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // 主配置区 (稍微缩减行间距以节省整体高度，防止挤压下方日志框)
        JPanel p = new JPanel(new GridLayout(7, 1, 5, 2));
        p.setOpaque(false);
        // 调整边距：顶部保持25(适度下移)，底部间距压缩到5，防止撑破640x520分辨率
        p.setBorder(BorderFactory.createEmptyBorder(25, 50, 5, 50));

        for (int i = 0; i < 5; i++) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            JLabel label = new JLabel((i + 1) + ". 修改 [" + placeholders[i] + "] 为: ");
            label.setPreferredSize(new Dimension(220, 30));
            label.setForeground(Color.WHITE);
            row.add(label, BorderLayout.WEST);

            inputs[i] = new GlassTextField();
            row.add(inputs[i], BorderLayout.CENTER);

            p.add(row);
        }

        JPanel row6 = new JPanel(new BorderLayout(10, 0));
        row6.setOpaque(false);
        JPanel leftPanel = new JPanel(new BorderLayout(5, 0));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(220, 30));
        JLabel label6 = new JLabel("6. 修改 ");
        label6.setForeground(Color.WHITE);
        leftPanel.add(label6, BorderLayout.WEST);

        customKeyInput = new GlassTextField();
        customKeyInput.setToolTipText("输入需要被替换的自定义内容");
        leftPanel.add(customKeyInput, BorderLayout.CENTER);

        JLabel label6End = new JLabel(" 为: ");
        label6End.setForeground(Color.WHITE);
        leftPanel.add(label6End, BorderLayout.EAST);
        row6.add(leftPanel, BorderLayout.WEST);

        inputs[5] = new GlassTextField();
        row6.add(inputs[5], BorderLayout.CENTER);
        p.add(row6);

        // 透明日志区本体
        logArea = new JTextArea(
                "\n" +
                        "     >_ 请将整个 文件夹 或 单/多份文件 拖拽至此...\n" +
                        "     >_ [WARN] 核心引擎不支持 .doc 旧版文档！\n" +
                        "     >_ [FAIL] 强行解析将导致未知错误 (X_X) 后果自负。\n"
        );
        logArea.setFont(new Font("微软雅黑", Font.BOLD, 12));
        logArea.setUI(new javax.swing.plaf.basic.BasicTextAreaUI());
        logArea.setEditable(false);
        logArea.setOpaque(false);
        logArea.setBackground(new Color(0, 0, 0, 0));
        logArea.setForeground(Color.WHITE);
        logArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(logArea) {
            @Override
            public boolean isOpaque() { return false; }
        };
        scroll.setUI(new javax.swing.plaf.basic.BasicScrollPaneUI());
        scroll.getViewport().setUI(new javax.swing.plaf.basic.BasicViewportUI());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBackground(new Color(0, 0, 0, 0));
        scroll.getViewport().setBackground(new Color(0, 0, 0, 0));
        scroll.setBorder(null);
        scroll.setViewportBorder(null); // 彻底抹杀视口边框！

        // 极致精简：直接调用全局的极暗玻璃容器
        GlassPanel logWrapper = new GlassPanel(new BorderLayout());
        logWrapper.setBorder(BorderFactory.createEmptyBorder(5, 15, 15, 15));
        logWrapper.add(scroll, BorderLayout.CENTER);

        DropTargetAdapter dropAdapter = new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                    dtde.dropComplete(true);

                    Map<String, String> map = new HashMap<>();
                    for (int i = 0; i < 5; i++) {
                        String v = inputs[i].getText().trim();
                        if (!v.isEmpty()) map.put(placeholders[i], v);
                    }
                    String customKey = customKeyInput.getText().trim();
                    String customVal = inputs[5].getText().trim();
                    if (!customKey.isEmpty() && !customVal.isEmpty()) {
                        map.put(customKey, customVal);
                    }

                    if (map.isEmpty()) {
                        log("请至少输入一个替换内容！");
                        return;
                    }

                    SwingUtilities.invokeLater(() -> {
                        String msg = "同志您好！检测到拖入了 " + files.size() + " 个项目（若是文件夹则包含内部所有文件）。\n\n确定要进行批量内容替换吗？\n(此操作不可逆，请确保已做好文件备份！)";

                        int confirm = JOptionPane.showConfirmDialog(
                                ReplacerPanel.this,
                                msg,
                                "批量替换确认",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE
                        );

                        if (confirm == JOptionPane.YES_OPTION) {
                            log(">>> 开始执行批量替换任务...");
                            new Thread(() -> {
                                for (File f : files) recursiveScan(f, map);
                                log(">>> 任务全部完成！");
                                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "所有文件处理完成！"));
                            }).start();
                        } else {
                            log("--- 已取消本次拖入操作 ---");
                        }
                    });

                } catch (Exception e) {
                    log("拖拽异常: " + e.getMessage());
                }
            }
        };

        // 强行把拖拽监听打穿到底部区域的每一层！
        new DropTarget(logWrapper, dropAdapter);
        new DropTarget(logArea, dropAdapter);
        new DropTarget(scroll, dropAdapter);

        // 垂直间距缩小为 5，进一步紧凑布局避免滚动条
        JPanel centerContainer = new JPanel(new BorderLayout(10, 5));
        centerContainer.setOpaque(false);
        centerContainer.add(p, BorderLayout.NORTH);
        centerContainer.add(logWrapper, BorderLayout.CENTER);
        add(centerContainer, BorderLayout.CENTER);
    }

    private void recursiveScan(File file, Map<String, String> map) {
        if (file.isDirectory()) {
            File[] list = file.listFiles();
            if (list != null) for (File c : list) recursiveScan(c, map);
        } else {
            processFile(file, map);
        }
    }

    private void processFile(File src, Map<String, String> map) {
        String name = src.getName().toLowerCase();
        File temp = new File(src.getAbsolutePath() + ".tmp");
        boolean success = false;

        try {
            try (InputStream is = FileMagic.prepareToCheckMagic(new FileInputStream(src))) {
                FileMagic fm = FileMagic.valueOf(is);

                if (fm == FileMagic.OOXML && name.endsWith(".docx")) {
                    try (XWPFDocument doc = new XWPFDocument(is)) {
                        for (XWPFParagraph p : doc.getParagraphs()) replaceInDocx(p, map);
                        for (XWPFTable t : doc.getTables()) {
                            for (XWPFTableRow r : t.getRows()) {
                                for (XWPFTableCell c : r.getTableCells()) {
                                    for (XWPFParagraph p : c.getParagraphs()) replaceInDocx(p, map);
                                }
                            }
                        }
                        try (FileOutputStream fos = new FileOutputStream(temp)) { doc.write(fos); }
                        success = true;
                    }
                }
                else if (fm == FileMagic.OOXML && name.endsWith(".pptx")) {
                    try (XMLSlideShow ppt = new XMLSlideShow(is)) {
                        for (XSLFSlide slide : ppt.getSlides()) {
                            for (XSLFShape shape : slide.getShapes()) {
                                if (shape instanceof XSLFTextShape) {
                                    replaceInPptx((XSLFTextShape) shape, map);
                                }
                                else if (shape instanceof XSLFTable) {
                                    XSLFTable table = (XSLFTable) shape;
                                    for (XSLFTableRow row : table.getRows()) {
                                        for (XSLFTableCell cell : row.getCells()) {
                                            replaceInPptx(cell, map);
                                        }
                                    }
                                }
                            }
                        }
                        try (FileOutputStream fos = new FileOutputStream(temp)) { ppt.write(fos); }
                        success = true;
                    }
                }
                else if (fm == FileMagic.OLE2 && name.endsWith(".doc")) {
                    try (HWPFDocument doc = new HWPFDocument(is)) {
                        org.apache.poi.hwpf.usermodel.Range r = doc.getRange();
                        for (Map.Entry<String, String> e : map.entrySet()) {
                            r.replaceText(e.getKey(), e.getValue());
                        }
                        try (FileOutputStream fos = new FileOutputStream(temp)) { doc.write(fos); }
                        success = true;
                    }
                }
                else if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                    try (Workbook wb = WorkbookFactory.create(is)) {
                        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                            for (Row r : wb.getSheetAt(i)) {
                                for (Cell c : r) {
                                    if (c.getCellType() == CellType.STRING) {
                                        String v = c.getStringCellValue();
                                        for (Map.Entry<String, String> e : map.entrySet()) {
                                            v = v.replace(e.getKey(), e.getValue());
                                        }
                                        c.setCellValue(v);
                                    }
                                }
                            }
                        }
                        try (FileOutputStream fos = new FileOutputStream(temp)) { wb.write(fos); }
                        success = true;
                    }
                }
            }

            if (success && temp.exists()) {
                Files.copy(temp.toPath(), src.toPath(), StandardCopyOption.REPLACE_EXISTING);
                log("[处理成功] " + src.getName());
            }

        } catch (Throwable t) {
            log("[报错跳过] " + src.getName() + " -> " + t.toString());
        } finally {
            if (temp.exists()) temp.delete();
        }
    }

    private void replaceInPptx(XSLFTextShape shape, Map<String, String> map) {
        for (XSLFTextParagraph p : shape.getTextParagraphs()) {
            List<XSLFTextRun> runs = p.getTextRuns();
            if (runs.isEmpty()) continue;

            String pText = "";
            for (XSLFTextRun run : runs) pText += run.getRawText();

            boolean hit = false;
            for (String key : map.keySet()) {
                if (pText.contains(key)) { hit = true; break; }
            }

            if (hit) {
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    pText = pText.replace(entry.getKey(), entry.getValue());
                }

                XSLFTextRun firstRun = runs.get(0);
                String text = pText;

                for (int i = runs.size() - 1; i >= 1; i--) {
                    runs.get(i).setText("");
                }
                firstRun.setText(text);

                // ✨ 新增：如果是纯黑模式，强制覆盖PPT字体颜色
                if (isBlackModeEnabled) {
                    firstRun.setFontColor(Color.BLACK);
                }
            }
        }
    }

    private void replaceInDocx(XWPFParagraph p, Map<String, String> map) {
        String pText = p.getText();
        if (pText == null || pText.isEmpty()) return;
        boolean hit = false;
        for (String key : map.keySet()) {
            if (pText.contains(key)) { hit = true; break; }
        }
        if (hit) {
            String newText = pText;
            for (Map.Entry<String, String> entry : map.entrySet()) {
                newText = newText.replace(entry.getKey(), entry.getValue());
            }
            List<XWPFRun> runs = p.getRuns();
            if (!runs.isEmpty()) {
                XWPFRun origin = runs.get(0);
                String fontFamily = origin.getFontFamily();
                int fontSize = origin.getFontSize();
                String color = origin.getColor();
                boolean isBold = origin.isBold();
                boolean isItalic = origin.isItalic();
                UnderlinePatterns underline = origin.getUnderline();
                for (int i = runs.size() - 1; i >= 0; i--) p.removeRun(i);
                XWPFRun newRun = p.createRun();
                newRun.setText(newText);
                if (fontFamily != null) newRun.setFontFamily(fontFamily);
                if (fontSize != -1) newRun.setFontSize(fontSize);

                // ✨ 新增：纯黑模式色彩拦截 (大小依然保持 origin 的 fontSize 属性)
                if (isBlackModeEnabled) {
                    newRun.setColor("000000"); // 纯黑色HEX
                } else if (color != null) {
                    newRun.setColor(color);
                }

                newRun.setBold(isBold);
                newRun.setItalic(isItalic);
                newRun.setUnderline(underline);
                newRun.getCTR().addNewRPr().addNewRFonts().setEastAsia(fontFamily);
            }
        }
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // ✨ 新增：纯黑模式右上角指示灯组件
    // ✨ 新增：纯黑模式右上角指示灯组件（三原色环绕版）
    // ✨ 新增：纯黑/彩色模式右上角指示灯组件（逻辑修正版）
    class BlackModeIndicator extends JComponent {
        public BlackModeIndicator() {
            setPreferredSize(new Dimension(50, 50));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    isBlackModeEnabled = !isBlackModeEnabled;
                    repaint();
                    log(isBlackModeEnabled ?
                            ">>> [设置已变] 纯黑模式已开启：新替换的文本颜色强制为黑色。" :
                            ">>> [设置已变] 彩色模式已开启：新替换的文本保留原色彩。");
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            // 开启抗锯齿，保证边缘平滑
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            // 定义外围环绕圈的参数
            int ringRadius = 15;
            int ringDiameter = ringRadius * 2;
            int ringX = cx - ringRadius;
            int ringY = cy - ringRadius;

            if (!isBlackModeEnabled) {
                // 关闭纯黑 (即彩色模式)：三原色 (RGB) 科技感环绕
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // 顶部红色弧
                g2.setColor(new Color(255, 71, 87));
                g2.drawArc(ringX, ringY, ringDiameter, ringDiameter, 95, 110);

                // 左下绿色弧
                g2.setColor(new Color(46, 213, 115));
                g2.drawArc(ringX, ringY, ringDiameter, ringDiameter, 215, 110);

                // 右下蓝色弧
                g2.setColor(new Color(30, 144, 255));
                g2.drawArc(ringX, ringY, ringDiameter, ringDiameter, 335, 110);

                // 彩色模式内核
                g2.setColor(new Color(40, 40, 40));
                g2.fillOval(cx - 10, cy - 10, 20, 20);
            } else {
                // 默认/开启纯黑模式：极简暗色闭合灰环
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(80, 80, 80));
                g2.drawArc(ringX, ringY, ringDiameter, ringDiameter, 0, 360);

                // 纯黑模式极暗内核
                g2.setColor(new Color(15, 15, 15));
                g2.fillOval(cx - 10, cy - 10, 20, 20);
            }

            // 绘制内部徽章：代表文字排版的 "A" 字符
            g2.setColor(isBlackModeEnabled ? new Color(150, 150, 150) : Color.WHITE);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int[] aX = {cx - 4, cx, cx + 4};
            int[] aY = {cy + 4, cy - 5, cy + 4};
            g2.drawPolyline(aX, aY, 3);
            g2.drawLine(cx - 2, cy + 1, cx + 2, cy + 1);

            g2.dispose();
        }
    }
}