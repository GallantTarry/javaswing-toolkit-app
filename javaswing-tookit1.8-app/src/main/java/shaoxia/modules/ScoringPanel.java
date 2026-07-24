package shaoxia.modules;

import javazoom.jl.player.Player;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;
import shaoxia.MainLauncher;
import shaoxia.Utils.ActionButton;
import shaoxia.Utils.GlassDropPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 评分生成助手 ScoringPanel (一体式极简自动导出版 + 玉珍单曲循环 + 继承BackgroundPanel) */
public class ScoringPanel extends BackgroundPanel {

    private List<TableDataHolder> extractedTables = new ArrayList<>();

    // 🎵 音乐播放控制参数
    private boolean isMusicPlaying = false;
    private Thread musicThread;
    private Player player;

    public ScoringPanel(MainLauncher parent) {
        // 🎨 核心修复：直接调用父类构造统一背景图，彻底抛弃冗余的图片读取代码
        super("bg_imgs/月球与地球.png");
        setLayout(new BorderLayout(10, 10));

        // 顶部返回按钮区 (关联停止音乐机制)
        // ✨ 完全对标 ConverterPanel 的嵌套结构与边缘距离参数
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 20));

        ActionButton backBtn = new ActionButton("<< 返回菜单");
        backBtn.setPreferredSize(new Dimension(130, 36));
        backBtn.addActionListener(e -> {
            stopMusic(); // 返回主菜单时自动销毁音乐线程
            parent.showMenu();
        });

        // 取消 FlowLayout 默认的 5px 间隙，实现精确像素对齐
        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftTop.setOpaque(false);
        leftTop.add(backBtn);
        topBar.add(leftTop, BorderLayout.WEST);

        // 🚀 修改：右上角跳转图标 (圆绵羊版)，完全对标其他面板的 50x50 JComponent
        SheepIndicator sheepLink = new SheepIndicator();
        sheepLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://tools.pdf24.org/zh/"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(ScoringPanel.this, "无法打开浏览器，请检查网络设置。");
                }
            }
        });
        topBar.add(sheepLink, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // 一体式超大拖拽区域 (毛玻璃拟态 UI)
        GlassDropPanel dropArea = new GlassDropPanel();
        dropArea.setOpaque(false);
        dropArea.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- 终极完美居中方案：彻底弃用 HTML，改用原生 GridBagLayout 实现绝对居中 ---
        JPanel centerTextPanel = new JPanel(new GridBagLayout());
        centerTextPanel.setOpaque(false); // 保持透明，露出底部的毛玻璃效果

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE; // 让组件自动按行往下堆叠
        gbc.anchor = GridBagConstraints.CENTER;  // 核心：强制所有组件在物理中心对齐
        gbc.insets = new Insets(8, 0, 8, 0);     // 设置行与行之间的上下间距

        // 字号从原先的 24px 放大到 32px（粗体），少侠可根据视觉效果自由调整该数值
        Font labelFont = new Font(Font.MONOSPACED, Font.BOLD, 32);

        String[] textLines = {"WE ARE", "\uD83D\uDD4A ALL", "GREAT", "APES"};

        for (String line : textLines) {
            JLabel label = new JLabel(line);
            label.setFont(labelFont);
            label.setForeground(Color.WHITE);
            centerTextPanel.add(label, gbc); // 运用居中约束加入面板
        }

        // 将这个完美居中的面板放入您的毛玻璃拖拽区中
        dropArea.add(centerTextPanel, BorderLayout.CENTER);
        // --- 核心修复结束 ---

        // 神来之笔：让黑洞本身变成音乐播放/暂停的隐藏按钮
        dropArea.setCursor(new Cursor(Cursor.HAND_CURSOR));
        dropArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleMusic(); // 点击任意区域触发音乐
            }
        });

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(40, 80, 80, 80));
        centerWrapper.add(dropArea, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // 绑定拖拽监听 (保留核心解析功能)
        new DropTarget(dropArea, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        parseDocx(files.get(0));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    // 🎵 音乐控制区：单曲循环引擎
    private void toggleMusic() {
        if (isMusicPlaying) {
            stopMusic();
        } else {
            playMusic();
        }
    }

    private void playMusic() {
        isMusicPlaying = true;
        musicThread = new Thread(() -> {
            // 使用 while 循环实现单曲死循环播放
            while (isMusicPlaying) {
                try {
                    InputStream is = getClass().getResourceAsStream("/music/To The Moon.mp3");
                    if (is == null) {
                        System.err.println("找不到 /music/To The Moon.mp3，请检查 resources 目录");
                        isMusicPlaying = false;
                        break;
                    }
                    player = new Player(is);
                    player.play();
                } catch (Exception e) {
                    if (isMusicPlaying) {
                        e.printStackTrace();
                    }
                }
            }
        });
        musicThread.start();
    }

    private void stopMusic() {
        isMusicPlaying = false;
        if (player != null) {
            player.close();
        }
        if (musicThread != null) {
            musicThread.interrupt();
        }
    }

    private void parseDocx(File file) {
        extractedTables.clear();
        try (FileInputStream fis = new FileInputStream(file); XWPFDocument doc = new XWPFDocument(fis)) {
            List<IBodyElement> elements = doc.getBodyElements();

            String currentMainTitle = "";
            String currentSubTitle = "";
            boolean isCapturing = false;

            for (int i = 0; i < elements.size(); i++) {
                IBodyElement el = elements.get(i);

                if (el instanceof XWPFParagraph) {
                    String text = ((XWPFParagraph) el).getText().trim();
                    if (text.isEmpty()) continue;

                    boolean isTargetHeader = (text.contains("技术评分标准") || text.contains("商务评分标准")
                            || text.contains("技术评审细则") || text.contains("商务评审细则"))
                            && (text.contains("%") || text.contains("权重"));

                    if (isTargetHeader) {
                        currentMainTitle = text;
                        currentSubTitle = "";
                        isCapturing = true;
                        continue;
                    }

                    if (isCapturing) {
                        if ((text.startsWith("第") && text.contains("章")) || text.contains("应答文件格式")) {
                            isCapturing = false;
                        } else {
                            if (text.length() <= 30) {
                                currentSubTitle = text.replaceAll("[:：]$", "");
                            } else {
                                currentSubTitle = "补充说明";
                            }
                        }
                    }
                }

                if (isCapturing && el instanceof XWPFTable) {
                    XWPFTable table = (XWPFTable) el;
                    if (table.getRows().size() > 0) {
                        String finalTitle = currentMainTitle;
                        if (!currentSubTitle.isEmpty()) {
                            finalTitle = finalTitle + " - " + currentSubTitle;
                        }
                        processTable(table, finalTitle);
                    }
                }
            }

            if (!extractedTables.isEmpty()) {
                autoExportToExcel();
            } else {
                JOptionPane.showMessageDialog(this,
                        "很抱歉，我读取不到。",
                        "该回去啦",
                        JOptionPane.WARNING_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "解析过程出错，文件是否正在被其他程序占用？");
        }
    }

    private void processTable(XWPFTable table, String title) {
        List<XWPFTableRow> rows = table.getRows();
        if (rows == null || rows.isEmpty()) return;

        int startDataIdx = 0;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).getTableCells().size() > 1) {
                startDataIdx = i;
                break;
            }
        }

        if (startDataIdx >= rows.size()) return;

        int originalColCount = rows.get(startDataIdx).getTableCells().size();
        int skipCols = 3;
        if (originalColCount <= skipCols) skipCols = 0;
        int newColCount = originalColCount - skipCols;

        List<String[]> data = new ArrayList<>();
        String[] lastRowMemory = new String[originalColCount];
        Arrays.fill(lastRowMemory, "");

        for (int r = startDataIdx; r < rows.size(); r++) {
            XWPFTableRow row = rows.get(r);
            List<XWPFTableCell> cells = row.getTableCells();
            String[] rowData = new String[newColCount];
            boolean hasContent = false;

            for (int j = 0; j < originalColCount; j++) {
                String currentVal = "";
                if (j < cells.size()) {
                    currentVal = cells.get(j).getText().trim().replaceAll("[\\n\\r\\t]+", " ");
                }

                if (currentVal.isEmpty()) {
                    currentVal = lastRowMemory[j];
                } else {
                    lastRowMemory[j] = currentVal;
                }

                if (j >= skipCols) {
                    rowData[j - skipCols] = currentVal;
                    if (!currentVal.isEmpty()) hasContent = true;
                }
            }
            if (hasContent) data.add(rowData);
        }

        if (!data.isEmpty()) {
            TableDataHolder existingHolder = null;
            for (TableDataHolder holder : extractedTables) {
                if (holder.title.equals(title)) {
                    existingHolder = holder;
                    break;
                }
            }

            if (existingHolder != null) {
                if (existingHolder.data.get(0).length == newColCount) {
                    existingHolder.data.addAll(data);
                } else {
                    extractedTables.add(new TableDataHolder(title + "_续表", data));
                }
            } else {
                TableDataHolder holder = new TableDataHolder(title, data);
                extractedTables.add(holder);
            }
        }
    }

    private void autoExportToExcel() {
        try {
            File desktop = new File(System.getProperty("user.home"), "Desktop");

            int techCount = 0;
            int bizCount = 0;
            String[] cnNumbers = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};

            for (TableDataHolder holder : extractedTables) {
                boolean isTech = holder.title.contains("技术");
                boolean isBiz = holder.title.contains("商务");

                String baseType = isTech ? "技术评分标准" : (isBiz ? "商务评分标准" : "其他评分标准");
                int currentIdx = isTech ? (++techCount) : (isBiz ? (++bizCount) : 1);

                String cnIndex = currentIdx < cnNumbers.length ? cnNumbers[currentIdx] : String.valueOf(currentIdx);
                String fileNameBase = baseType + "第" + cnIndex + "份";

                File targetFile = getUniqueFile(desktop, fileNameBase, "xlsx");

                XSSFWorkbook wb = new XSSFWorkbook();
                XSSFCellStyle style = createBaseStyle(wb);
                XSSFSheet sheet = wb.createSheet("Sheet1");
                List<String[]> data = holder.data;

                for (int r = 0; r < data.size(); r++) {
                    XSSFRow row = sheet.createRow(r);
                    for (int c = 0; c < data.get(r).length; c++) {
                        XSSFCell cell = row.createCell(c);
                        cell.setCellValue(data.get(r)[c]);
                        cell.setCellStyle(style);
                    }
                }
                for (int c = 0; c < data.get(0).length; c++) physicalMerge(sheet, data, c);
                for (int i = 0; i < data.get(0).length; i++) sheet.setColumnWidth(i, 256 * 40);

                try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                    wb.write(fos);
                }
            }

            JOptionPane.showMessageDialog(this,
                    "读取成功\n" +
                            "共生成 " + techCount + " 份技术评分标准，" + bizCount + " 份商务评分标准。\n" +
                            "文件已成功送达您的桌面。",
                    "起风了",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Excel 自动生成过程出现异常！", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private File getUniqueFile(File dir, String baseName, String ext) {
        File file = new File(dir, baseName + "." + ext);
        if (!file.exists()) return file;
        int count = 1;
        while (true) {
            file = new File(dir, baseName + "_副本" + count + "." + ext);
            if (!file.exists()) return file;
            count++;
        }
    }

    private void physicalMerge(XSSFSheet sheet, List<String[]> data, int colIdx) {
        int startRow = 0;
        for (int r = 1; r <= data.size(); r++) {
            String current = (r < data.size()) ? data.get(r)[colIdx] : "EOF";
            String previous = data.get(startRow)[colIdx];
            if (!current.equals(previous)) {
                if (r - 1 > startRow && !previous.isEmpty()) {
                    sheet.addMergedRegion(new CellRangeAddress(startRow, r - 1, colIdx, colIdx));
                }
                startRow = r;
            }
        }
    }

    private XSSFCellStyle createBaseStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    static class TableDataHolder {
        String title;
        List<String[]> data;

        TableDataHolder(String t, List<String[]> d) {
            this.title = t;
            this.data = d;
        }
    }

    /**
     * 🎨 原生自绘的“圆绵羊”图标指示器组件 (对标 50x50 标准)
     */
    /**
     * 🎨 基于内部数据库原生自绘的“正版 PDF24 绵羊”神兽图标
     * 完美还原大突眼、厚嘴唇、非对称羊毛与水平耷拉的耳朵
     */
    /**
     * 🎨 基于内部数据库原生自绘的“正版 PDF24 绵羊”神兽图标
     * 完美还原大突眼、厚嘴唇、非对称羊毛与水平耷拉的耳朵
     */
    /**
     * 🎨 基于内部数据库原生自绘的“正版 PDF24 绵羊”神兽图标
     * 完美还原大突眼、厚嘴唇、非对称羊毛与水平耷拉的耳朵
     */
    static class SheepIndicator extends JComponent {

        // ✨ 1. 缩放比例：0.8
        private final double SCALE = 0.8;

        // ✨ 2. 坐标微调参数 (少侠可随意修改这俩数值)
        private final int OFFSET_X = 10; // 负数代表向左移动
        private final int OFFSET_Y = 8;  // 正数代表向下移动

        public SheepIndicator() {
            // 为了防止向左、向下平移时超出边框导致被“裁切”，
            // 我们把隐形画框（安全区）从原本的 50x50 放大到了 65x65。
            // 另外，在 BorderLayout.EAST (靠右排列) 布局中，画框变宽本身就会在视觉上将图标向左推。
            setPreferredSize(new Dimension(65, 65));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            // ✨ 核心修改：先平移坐标轴。
            // 其中的 12 像素是基础缓冲距离，用来抵消 OFFSET_X 的负数，保证左移时图形坐标不会小于 0 而被裁掉。
            g2.translate(12 + OFFSET_X, OFFSET_Y);

            // 然后再按照 0.8 的比例统一缩放
            g2.scale(SCALE, SCALE);

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setStroke(new BasicStroke(
                    1.8f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));

            // =========================
            // 耳朵
            // =========================

            g2.setColor(new Color(120,120,120));
            g2.fillOval(4,22,8,10);
            g2.fillOval(38,22,8,10);

            g2.setColor(Color.BLACK);
            g2.drawOval(4,22,8,10);
            g2.drawOval(38,22,8,10);

            // =========================
            // 羊毛
            // =========================

            Area wool = new Area();

            wool.add(new Area(new Ellipse2D.Float(9,7,14,14)));
            wool.add(new Area(new Ellipse2D.Float(18,4,15,15)));
            wool.add(new Area(new Ellipse2D.Float(28,7,14,14)));

            wool.add(new Area(new Ellipse2D.Float(7,15,16,16)));
            wool.add(new Area(new Ellipse2D.Float(17,12,18,18)));
            wool.add(new Area(new Ellipse2D.Float(27,15,16,16)));

            g2.setColor(Color.WHITE);
            g2.fill(wool);

            g2.setColor(Color.BLACK);
            g2.draw(wool);

            // =========================
            // 鼻口
            // =========================

            Area snout = new Area(
                    new Ellipse2D.Float(
                            10,21,
                            30,19));

            g2.setColor(new Color(244,205,160));
            g2.fill(snout);

            g2.setColor(Color.BLACK);
            g2.draw(snout);

            // =========================
            // 左眼
            // =========================

            g2.setColor(Color.WHITE);
            g2.fillOval(12,12,15,15);

            g2.setColor(Color.BLACK);
            g2.drawOval(12,12,15,15);

            g2.fillOval(19,18,3,3);

            // =========================
            // 右眼（更大）
            // =========================

            g2.setColor(Color.WHITE);
            g2.fillOval(23,8,20,20);

            g2.setColor(Color.BLACK);
            g2.drawOval(23,8,20,20);

            g2.fillOval(32,17,3,3);

            // =========================
            // 嘴巴
            // =========================

            g2.drawArc(
                    14,28,
                    10,8,
                    150,
                    130);

            g2.drawArc(
                    18,27,
                    4,4,
                    20,
                    90);

            // =========================
            // 鼻孔
            // =========================

            g2.fillOval(20,27,2,3);
            g2.fillOval(27,26,2,3);

            // =========================
            // 眉毛
            // =========================

            g2.drawLine(14,10,18,11);
            g2.drawLine(34,6,38,8);

            g2.dispose();
        }
    }
}