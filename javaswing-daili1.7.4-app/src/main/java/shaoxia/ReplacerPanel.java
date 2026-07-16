package shaoxia;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;

import javax.swing.*;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
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

/** 文档替换助手 ReplacerPanel - 强韧防御版 (拟态圆角 + 虚线黑洞 UI + 完美拖拽穿透) */
public class ReplacerPanel extends BackgroundPanel {
    private JTextField[] inputs = new JTextField[6];
    private JTextField customKeyInput;
    private final String[] placeholders = {
            "XXX项目", "NARI-XXXXXX", "2026年XX月XX日", "XXXXXXXXXXX", "****@qq.com"
    };
    private JTextArea logArea;

    public ReplacerPanel(MainLauncher parent) {
        super("texture2.png");

        ToolTipManager.sharedInstance().setInitialDelay(200);
        ToolTipManager.sharedInstance().setDismissDelay(15000);

        setLayout(new BorderLayout());

        // 顶部导航
        ActionButton backBtn = new ActionButton("<< Menu", new Color(255, 255, 255, 30));
        backBtn.setPreferredSize(new Dimension(130, 36));
        backBtn.addActionListener(e -> parent.showMenu());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        top.add(backBtn);
        add(top, BorderLayout.NORTH);

        // 主配置区
        JPanel p = new JPanel(new GridLayout(7, 1, 5, 5));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 50, 10, 50));

        for (int i = 0; i < 5; i++) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            JLabel label = new JLabel((i + 1) + ". 修改 [" + placeholders[i] + "] 为: ");
            label.setPreferredSize(new Dimension(220, 30));
            label.setForeground(Color.WHITE);
            row.add(label, BorderLayout.WEST);
            inputs[i] = createCustomTextField();
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
        customKeyInput = createCustomTextField();
        customKeyInput.setToolTipText("输入需要被替换的自定义内容");
        leftPanel.add(customKeyInput, BorderLayout.CENTER);
        JLabel label6End = new JLabel(" 为: ");
        label6End.setForeground(Color.WHITE);
        leftPanel.add(label6End, BorderLayout.EAST);
        row6.add(leftPanel, BorderLayout.WEST);
        inputs[5] = createCustomTextField();
        row6.add(inputs[5], BorderLayout.CENTER);
        p.add(row6);

        // 透明日志区本体
        logArea = new JTextArea(
                "  ___________________________ \n" +
                        " | [ ]                   [X] |\n" +
                        " |---------------------------|\n" +
                        " | >_ SUP DOCX PPTX          |\n" +
                        " | >_ XLSX XLS               |\n" +
                        " | >_ NO DOC.                |\n" +
                        " |___________________________|\n"
        );
        logArea.setFont(new Font("Monospaced", Font.BOLD, 12));
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

        // 把滚动条包在一个带有“圆角 + 虚线描边”的拟态容器里
        JPanel logWrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 画暗色圆角底板
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);

                // 画高级感虚线边框
                Stroke dashed = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{10.0f, 8.0f}, 0.0f);
                g2.setStroke(dashed);
                g2.setColor(new Color(255, 255, 255, 110));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 30, 30);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        logWrapper.setOpaque(false);
        logWrapper.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        logWrapper.add(scroll, BorderLayout.CENTER);

        // ✨ 核心修复：建立通用的拖拽监听器，并将其绑定到“所有”可能被拖放覆盖的组件上！
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

        JPanel centerContainer = new JPanel(new BorderLayout(10, 10));
        centerContainer.setOpaque(false);
        centerContainer.add(p, BorderLayout.NORTH);
        centerContainer.add(logWrapper, BorderLayout.CENTER);
        add(centerContainer, BorderLayout.CENTER);
    }

    private JTextField createCustomTextField() {
        JTextField textField = new JTextField() {
            @Override
            public boolean isOpaque() { return false; }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.dispose();
                super.paintComponent(g);
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
        };

        textField.setUI(new javax.swing.plaf.basic.BasicTextFieldUI());
        textField.setOpaque(false);
        textField.setBackground(new Color(0, 0, 0, 0));
        textField.setForeground(Color.WHITE);
        textField.setCaretColor(Color.WHITE);
        textField.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        textField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        return textField;
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
                if (color != null) newRun.setColor(color);
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

    class ActionButton extends JButton {
        private Color bgColor;

        public ActionButton(String text, Color bgColor) {
            super(text);
            this.bgColor = bgColor;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("微软雅黑", Font.BOLD, 13));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        public void updateColor(Color newColor) {
            this.bgColor = newColor;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean isPressed = getModel().isPressed();
            boolean isHover = getModel().isRollover();

            Color drawColor = bgColor;
            if (isPressed) {
                drawColor = bgColor.darker();
            } else if (isHover) {
                drawColor = bgColor.brighter();
            }

            g2.setColor(drawColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight() - 1, getHeight() - 1);

            g2.setColor(new Color(255, 255, 255, 60));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, getHeight() - 3, getHeight() - 3);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}