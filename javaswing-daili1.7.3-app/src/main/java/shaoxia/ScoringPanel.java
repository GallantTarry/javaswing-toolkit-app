package shaoxia;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**评分生成助手 ScoringPanel (终极毛玻璃监听区版)*/

public class ScoringPanel extends JPanel
{
    private JTabbedPane tabbedPane;
    private ActionButton exportBtn;
    private List<TableDataHolder> extractedTables = new ArrayList<>();

    public ScoringPanel(MainLauncher parent) {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(45, 48, 50));

        // 1. 顶部返回按钮区
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.setOpaque(false);
        ActionButton backBtn = new ActionButton("<< 返回主菜单", new Color(255, 255, 255, 30));
        backBtn.setPreferredSize(new Dimension(130, 36));
        backBtn.addActionListener(e -> parent.showMenu());
        topBar.add(backBtn);

        // ✨ 2. 固定的拖拽区域 (升级为毛玻璃拟态 UI)
        JPanel dropArea = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                // 开启像素级抗锯齿
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // --- 模拟毛玻璃 (Glassmorphism) ---
                // 第一层：半透明白底，提升透光率
                g2.setColor(new Color(255, 255, 255, 15));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);

                // 第二层：深色质感遮罩，融入整体深色主题
                g2.setColor(new Color(0, 0, 0, 50));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);

                // 第三层：模拟玻璃边缘的折射高光轮廓
                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(new Color(255, 255, 255, 70));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 30, 30);

                // 第四层：内部的虚线指示框 (更显精细的圆角虚线)
                Stroke dashed = new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{12.0f, 8.0f}, 0.0f);
                g2.setStroke(dashed);
                g2.setColor(new Color(255, 255, 255, 120));
                g2.drawRoundRect(8, 8, getWidth() - 17, getHeight() - 17, 22, 22);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        dropArea.setOpaque(false);
        dropArea.setPreferredSize(new Dimension(0, 140));
        dropArea.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 内部文字标签 (剔除背景，融入毛玻璃)
        JLabel innerLabel = new JLabel("<html><center>" + "<font face='monospaced' size='6' color='white'>" +
                "WE ARE ALL GREAT \uD83D\uDC12APES\uD83D\uDC12<br>" +
                "<font size='4' color='#DDDDDD'>请将 Word 采购文件拖拽至此区域</font>" +
                "</font></center></html>", JLabel.CENTER);
        dropArea.add(innerLabel, BorderLayout.CENTER);

        // 组装头部
        JPanel northPanel = new JPanel(new BorderLayout(5, 5));
        northPanel.setOpaque(false);
        // 让整个顶部留出一点外边距，避免毛玻璃框贴边
        northPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 5, 15));
        northPanel.add(topBar, BorderLayout.NORTH);
        northPanel.add(dropArea, BorderLayout.CENTER);
        add(northPanel, BorderLayout.NORTH);

        // 3. 中间选项卡面板初始化
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("微软雅黑", Font.PLAIN, 13));

        JScrollPane contentScroll = new JScrollPane(tabbedPane);
        contentScroll.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 15));
        contentScroll.setOpaque(false);
        contentScroll.getViewport().setOpaque(false);
        contentScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        contentScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(contentScroll, BorderLayout.CENTER);

        // 4. 底部导出按钮区
        exportBtn = new ActionButton("一键导出 Excel", new Color(39, 174, 96));
        exportBtn.setFont(new Font("微软雅黑", Font.BOLD, 20));
        exportBtn.setPreferredSize(new Dimension(0, 55));
        exportBtn.setEnabled(false);
        exportBtn.addActionListener(e -> exportToExcel());

        JPanel bottomWrapper = new JPanel(new BorderLayout());
        bottomWrapper.setOpaque(false);
        bottomWrapper.setBorder(BorderFactory.createEmptyBorder(10, 40, 20, 40));
        bottomWrapper.add(exportBtn, BorderLayout.CENTER);

        add(bottomWrapper, BorderLayout.SOUTH);

        // ✨ 将拖拽监听器绑定到新的毛玻璃区域
        new DropTarget(dropArea, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) parseDocx(files.get(0));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void parseDocx(File file) {
        tabbedPane.removeAll();
        extractedTables.clear();
        try (FileInputStream fis = new FileInputStream(file); XWPFDocument doc = new XWPFDocument(fis)) {
            List<IBodyElement> elements = doc.getBodyElements();

            String currentTitle = "";
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
                        currentTitle = text;
                        isCapturing = true;
                        continue;
                    }

                    if (isCapturing && (text.startsWith("第") && text.contains("章") || text.contains("应答文件格式"))) {
                        if (!isTargetHeader) {
                            isCapturing = false;
                        }
                    }
                }

                if (isCapturing && el instanceof XWPFTable) {
                    XWPFTable table = (XWPFTable) el;
                    if (table.getRows().size() > 1) {
                        processTable(table, currentTitle);
                    }
                }
            }

            if (!extractedTables.isEmpty()) {
                exportBtn.setEnabled(true);
                JOptionPane.showMessageDialog(this,
                        "检索成功，共找到 " + extractedTables.size() + " 个评分标准表格！",
                        "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                exportBtn.setEnabled(false);
                JOptionPane.showMessageDialog(this,
                        "未检索到符合条件的评分标准表格，请检查文档内容！",
                        "警告",
                        JOptionPane.WARNING_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "解析过程出错，您是不是选错文件了或者文件正在被占用。");
        }
    }

    private void processTable(XWPFTable table, String title) {
        List<XWPFTableRow> rows = table.getRows();
        XWPFTableRow firstRow = rows.get(0);
        List<Integer> validCols = new ArrayList<>();

        for (int i = 0; i < firstRow.getTableCells().size(); i++) {
            String head = firstRow.getCell(i).getText().trim();
            if (!head.contains("分") && !head.contains("值") && !head.contains("得") && !head.isEmpty()) {
                validCols.add(i);
            }
        }

        if (validCols.isEmpty()) return;

        List<String[]> data = new ArrayList<>();
        String[] lastRowMemory = new String[validCols.size()];
        Arrays.fill(lastRowMemory, "");

        for (int r = 1; r < rows.size(); r++) {
            XWPFTableRow row = rows.get(r);
            List<XWPFTableCell> cells = row.getTableCells();
            String[] rowData = new String[validCols.size()];
            boolean hasContent = false;

            for (int j = 0; j < validCols.size(); j++) {
                int targetIdx = validCols.get(j);
                String currentVal = "";
                if (targetIdx < cells.size()) {
                    currentVal = cells.get(targetIdx).getText().trim().replaceAll("[\\n\\r\\t]+", " ");
                }
                if (currentVal.isEmpty()) {
                    currentVal = lastRowMemory[j];
                } else {
                    lastRowMemory[j] = currentVal;
                }
                rowData[j] = currentVal;
                if (!currentVal.isEmpty()) hasContent = true;
            }
            if (hasContent) data.add(rowData);
        }

        if (!data.isEmpty()) {
            TableDataHolder holder = new TableDataHolder(title, data);
            extractedTables.add(holder);
            displayTable(holder);
        }
    }

    private void displayTable(TableDataHolder holder) {
        DefaultTableModel model = new DefaultTableModel(holder.data.toArray(new Object[0][0]), new String[holder.data.get(0).length]);
        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setFont(new Font("微软雅黑", Font.PLAIN, 12));

        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        for (int i = 0; i < table.getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setPreferredWidth(180);
            column.setMinWidth(100);
        }

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        tabbedPane.addTab(holder.title, tableScroll);
    }

    private void exportToExcel() {
        try {
            File desktop = new File(System.getProperty("user.home"), "Desktop");
            for (TableDataHolder holder : extractedTables) {
                String fileNameBase = holder.title.contains("技术") ? "技术评分标准" : "商务评分标准";
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
            JOptionPane.showMessageDialog(this, "Excel 导出成功");
        } catch (Exception ex) {
            ex.printStackTrace();
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

            Color drawColor = bgColor;

            if (!isEnabled()) {
                drawColor = new Color(80, 80, 80, 160);
            } else {
                boolean isPressed = getModel().isPressed();
                boolean isHover = getModel().isRollover();
                if (isPressed) {
                    drawColor = bgColor.darker();
                } else if (isHover) {
                    drawColor = bgColor.brighter();
                }
            }

            g2.setColor(drawColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight() - 1, getHeight() - 1);

            if (isEnabled()) {
                g2.setColor(new Color(255, 255, 255, 60));
            } else {
                g2.setColor(new Color(255, 255, 255, 20));
            }
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, getHeight() - 3, getHeight() - 3);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}