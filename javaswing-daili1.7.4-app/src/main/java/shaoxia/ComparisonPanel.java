package shaoxia;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** * 公告比对助手 ComparisonPanel
 * 完美继承 BackgroundPanel，自动支持 src 目录下图片加载
 */
public class ComparisonPanel extends BackgroundPanel {
    private MainLauncher parent;
    private JTextArea logArea;
    private File wordFile, pdfFile;
    private JLabel wordLabel, pdfLabel;

    public ComparisonPanel(MainLauncher parent) {
        // 核心修改 1：通过 super 完美复用 BackgroundPanel 的本地/src图片寻找机制
        super("texture2.png");
        this.parent = parent;

        // 整体布局：增加顶部和底部的垂直间距
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(50, 60, 40, 60));

        // 3. 顶部：拖拽区域 (圆弧实线框)
        JPanel dropPanel = new JPanel(new GridLayout(1, 2, 60, 0));
        dropPanel.setOpaque(false);
        wordLabel = createDropBox("【Drop DOCX document】");
        pdfLabel = createDropBox("【Drop PDF document】");
        setupDND(wordLabel, true);
        setupDND(pdfLabel, false);
        dropPanel.add(wordLabel);
        dropPanel.add(pdfLabel);

        // 4. 中部：日志区域 (增加与顶部的距离)
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("微软雅黑", Font.BOLD, 16));
        logArea.setForeground(Color.WHITE);
        logArea.setOpaque(false);
        logArea.setLineWrap(true);
        logArea.setMargin(new Insets(15, 20, 15, 20));
        logArea.setText("Hello! Awaiting your command.\n");

        JScrollPane scrollPane = new JScrollPane(logArea) {
            @Override
            protected void paintComponent(Graphics g) {
                // 日志框圆弧背景
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 80)); // 半透明黑
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
                g2.setColor(new Color(255, 255, 255, 100)); // 白色实线边框
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 25, 25);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(new EmptyBorder(5, 5, 5, 5));

        // 用一个带边距的面板包裹日志区，产生物理距离
        JPanel logContainer = new JPanel(new BorderLayout());
        logContainer.setOpaque(false);
        logContainer.setBorder(new EmptyBorder(40, 0, 40, 0)); // 这是与上下的距离
        logContainer.add(scrollPane, BorderLayout.CENTER);

        // 5. 底部：圆弧实体按钮 (替换为极简样式的 ActionButton)
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 60, 0));
        btnPanel.setOpaque(false);

        ActionButton btnRun = new ActionButton("Run Check", new Color(255, 255, 255, 30));
        ActionButton btnBack = new ActionButton("<< Menu", new Color(255, 255, 255, 30));

        // 设置舒适的大小
        btnRun.setPreferredSize(new Dimension(160, 42));
        btnBack.setPreferredSize(new Dimension(160, 42));

        btnRun.addActionListener(e -> startMatch());
        btnBack.addActionListener(e -> parent.showMenu());

        btnPanel.add(btnRun);
        btnPanel.add(btnBack);

        add(dropPanel, BorderLayout.NORTH);
        add(logContainer, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    // 拖拽框圆弧重绘
    private JLabel createDropBox(String t) {
        JLabel l = new JLabel(t, JLabel.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 20));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2)); // 实线
                g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 30, 30);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setPreferredSize(new Dimension(300, 150));
        l.setForeground(Color.WHITE);
        l.setFont(new Font("微软雅黑", Font.BOLD, 16));
        return l;
    }

    private void startMatch() {
        if (wordFile == null || pdfFile == null) {
            log("Please drop files first！");
            return;
        }
        log("Processing task...");
        new Thread(() -> {
            try {
                String w = readWord(wordFile);
                String p = readPdf(pdfFile);
                SwingUtilities.invokeLater(() -> executeCompare(w, p));
            } catch (Exception ex) {
                log("Error: File might be upside down!" + ex.getMessage());
            }
        }).start();
    }

    private void executeCompare(String w, String p) {
        log("--- Task completed! ---");
        boolean isAllPassed = true;

        String wNo = find(w, "采购编号[:：]?\\s*([^\\s]+)");
        String pNo = find(p, "(?:采购项目编号|招标编号|采购编号)[:：]?\\s*([^\\s\\n]+)");

        if (wNo != null) {
            wNo = wNo.replaceAll("[)）]+$", "").trim();
        }
        if (pNo != null) {
            pNo = pNo.replaceAll("[)）]+$", "").trim();
        }

        if (!checkResult(wNo, pNo, "【项目编号】")) {
            isAllPassed = false;
        }

        String wc = w.replaceAll("\\s", "");
        String pc = p.replaceAll("\\s", "");

        String wT1 = find(wc, "至(\\d{4}年\\d{1,2}月\\d{1,2}日(上午|下午)?\\d{1,2}时(?:\\d{1,2}分)?)");
        String pT1 = find(pc, "(?:采购文件获取截止时间|获取时间.*?到)(\\d{4}[年-]\\d{1,2}[月-]\\d{1,2}日?\\s*(?:上午|下午)?\\d{1,2}时?\\s*(?::|分)?\\d{1,2}分?)");

        if (!checkTimeResult(wT1, pT1, "【获取截止时间】")) {
            isAllPassed = false;
        }

        String wT2 = find(wc, "截止时间.*?(\\d{4}年\\d{1,2}月\\d{1,2}日(上午|下午)?\\d{1,2}时\\d{1,2}分)");
        String pT2 = find(pc, "(?:开启应答文件时间|递交截止时间|首次应答文件提交的截止时间).*?(\\d{4}[年-]\\d{1,2}[月-]\\d{1,2}日?\\s*(?:上午|下午)?\\d{1,2}时?\\s*(?::|分)?\\d{1,2}分?)");

        if (!checkTimeResult(wT2, pT2, "【应答截止时间】")) {
            isAllPassed = false;
        }

        final boolean finalStatus = isAllPassed;
        SwingUtilities.invokeLater(() -> {
            if (finalStatus) {
                JOptionPane.showMessageDialog(this, "Success! All items matched.", "校对结果", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Error! Mismatches found. See logs for details.", "校对结果", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private boolean checkResult(String w, String p, String lab) {
        if (w == null || p == null) {
            log("" + lab + " 提取失败");
            return false;
        }
        boolean match = w.trim().equalsIgnoreCase(p.trim());
        log(lab + " -> Word:[" + w.trim() + "] vs PDF:[" + p.trim() + "] -> " + (match ? "Matched" : "Conflict"));
        return match;
    }

    private boolean checkTimeResult(String wt, String pt, String lab) {
        if (wt == null || pt == null) {
            log("" + lab + " 提取数据缺失");
            return false;
        }
        String nW = normalize(wt);
        String nP = pt.replaceAll("[^0-9]", "");
        boolean match = nP.startsWith(nW);
        log(lab + " -> Word:[" + nW + "] vs PDF:[" + nP + "] -> " + (match ? "Matched" : "Conflict"));
        return match;
    }

    private String normalize(String raw) {
        if (raw == null) return "";

        Matcher m = Pattern.compile("(\\d{4})[年-]?(\\d{1,2})[月-]?(\\d{1,2})日?\\s*(上午|下午)?\\s*(\\d{1,2})[时:]?(\\d{1,2})?分?").matcher(raw);
        if (m.find()) {
            int year = Integer.parseInt(m.group(1));
            int month = Integer.parseInt(m.group(2));
            int day = Integer.parseInt(m.group(3));
            int hour = Integer.parseInt(m.group(5));

            if (raw.contains("下午") && hour < 12) hour += 12;
            if (raw.contains("上午") && hour == 12) hour = 0;

            int min = (m.group(6) != null) ? Integer.parseInt(m.group(6)) : 0;

            return String.format("%04d%02d%02d%02d%02d", year, month, day, hour, min);
        }
        return raw.replaceAll("[^0-9]", "");
    }

    private String readWord(File f) throws Exception {
        try (FileInputStream fis = new FileInputStream(f); XWPFDocument doc = new XWPFDocument(fis)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph p : doc.getParagraphs()) sb.append(p.getText()).append("\n");
            return sb.toString();
        }
    }

    private String readPdf(File f) throws Exception {
        try (FileInputStream fis = new FileInputStream(f)) {
            PdfReader reader = new PdfReader(fis);
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= reader.getNumberOfPages(); i++) sb.append(PdfTextExtractor.getTextFromPage(reader, i)).append("\n");
            reader.close();
            return sb.toString();
        }
    }

    private String find(String t, String r) {
        Matcher m = Pattern.compile(r).matcher(t);
        return m.find() ? m.group(1) : null;
    }

    private void setupDND(JLabel l, boolean isWord) {
        new DropTarget(l, new DropTargetAdapter() {
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    File f = files.get(0);
                    if (isWord) wordFile = f; else pdfFile = f;
                    l.setText("<html><center>Loaded<br/>" + f.getName() + "</center></html>");
                    l.setForeground(Color.WHITE);
                } catch (Exception e) {}
            }
        });
    }

    private void log(String m) {
        SwingUtilities.invokeLater(() -> logArea.append(m + "\n"));
    }

    // 内部类：悬浮反馈的半透明药丸按钮
    class ActionButton extends JButton {
        private Color bgColor;

        public ActionButton(String text, Color bgColor) {
            super(text);
            this.bgColor = bgColor;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setForeground(Color.WHITE);
            // 这里将字体稍微调大到 15，匹配你原来比对面板的视觉大小
            setFont(new Font("微软雅黑", Font.BOLD, 15));
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
            // getHeight() 决定了圆角的程度，这样能画出完美的胶囊(药丸)形
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight() - 1, getHeight() - 1);

            // 白色半透明玻璃描边
            g2.setColor(new Color(255, 255, 255, 60));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, getHeight() - 3, getHeight() - 3);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}