package me.shaoxia;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import javax.imageio.ImageIO;
import java.util.*;
import java.util.List;

/**
 * 数据校对助手 CheckerPanel (深灰纯净底色对齐版)
 * 已升级：支持多供应商绝对报价走势联合渲染，精准拦截等差等比串标
 * 已升级：预警明细全面采用“一行一家”纵向列表排版
 * 已升级：增加 Y 轴数字标尺刻度
 */
public class CheckerPanel extends BackgroundPanel {

    private JLabel infoLabel;
    private JLabel dl;
    private TitledBorder dropBorder;
    private JPanel dropArea;

    // 全局统一对齐 FinderPanel 的深灰色调，并设置浅色字体
    private final Color DASHBOARD_BG = new Color(45, 48, 50);
    private final Color TEXT_COLOR = new Color(230, 230, 230);

    public CheckerPanel(MainLauncher parent) {
        super("texture2.png");
        setLayout(new BorderLayout());
        setOpaque(false);

        JButton backBtn = new JButton(" << 返回主菜单 ");
        backBtn.setFocusPainted(false);
        backBtn.addActionListener(e -> parent.showMenu());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        top.add(backBtn);
        add(top, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();

        infoLabel = new JLabel("<html><body style='text-align: center;'><h2>信息协同数据校对助手</h2><p><b></b></p></body></html>");
        infoLabel.setForeground(Color.WHITE);
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0; gbc.gridx = 0; gbc.weightx = 1.0; gbc.weighty = 0.1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(20, 50, 10, 50);
        centerPanel.add(infoLabel, gbc);

        dropArea = new JPanel(new BorderLayout());
        dropArea.setOpaque(false);

        dropBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2, true),
                " 拖入 XLSX 进行逻辑校验 ",
                TitledBorder.CENTER, TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 13), Color.WHITE
        );
        dropArea.setBorder(dropBorder);

        dl = new JLabel("请拖入 Excel 报价表", JLabel.CENTER);
        dl.setForeground(Color.WHITE);
        dl.setFont(new Font("微软雅黑", Font.BOLD, 18));
        dropArea.add(dl);
        setCheckDropTarget(dropArea);

        gbc.gridy = 1; gbc.weightx = 1.0; gbc.weighty = 0.8;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 50, 40, 50);
        centerPanel.add(dropArea, gbc);
        add(centerPanel, BorderLayout.CENTER);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) { adjustFontSize(); }
        });
    }

    private void adjustFontSize() {
        int width = getWidth();
        if (width <= 0) return;
        double scale = Math.max(0.7, Math.min(width / 800.0, 1.4));
        int infoFontSize = (int) (12 * scale);
        int titleFontSize = (int) (16 * scale);

        infoLabel.setText("<html><body style='text-align: center;'>"
                + "<h2 style='font-size:" + titleFontSize + "px; margin: 0; padding: 0;'>信息协同数据校对助手</h2>"
                + "<p style='font-size:" + infoFontSize + "px; margin-top: 8px;'><b></b></p>"
                + "</body></html>");

        dl.setFont(new Font("微软雅黑", Font.BOLD, (int) (18 * scale)));
        dropBorder.setTitleFont(new Font("微软雅黑", Font.BOLD, (int) (13 * scale)));
        dropArea.repaint();
        this.validate();
    }

    private void setCheckDropTarget(JPanel p) {
        new DropTarget(p, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) processExcel(files.get(0));
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });
    }

    static class QuoteDropNode {
        String phaseName;
        Double dropPercent;
        QuoteDropNode(String phaseName, Double dropPercent) {
            this.phaseName = phaseName;
            this.dropPercent = dropPercent;
        }
    }

    static class PriceNode {
        String roundName;
        Double price;
        PriceNode(String roundName, Double price) {
            this.roundName = roundName;
            this.price = price;
        }
    }

    private void processExcel(File file) {
        List<String> errors = new ArrayList<>();
        Map<String, List<QuoteDropNode>> dropDataMap = new LinkedHashMap<>();
        Map<String, List<PriceNode>> priceDataMap = new LinkedHashMap<>();
        Set<String> flaggedCompanies = new HashSet<>();

        try (FileInputStream fis = new FileInputStream(file); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            int supIdx = -1;
            String[] rounds = {"首轮报价", "二轮报价", "三轮报价", "四轮报价", "五轮报价", "六轮报价", "终轮报价"};
            int[] colIdxMap = new int[rounds.length];
            Arrays.fill(colIdxMap, -1);
            boolean foundHeader = false;

            Map<Integer, Map<String, List<String>>> percentDuplicateMap = new HashMap<>();
            Map<Integer, Map<String, List<String>>> valueDuplicateMap = new HashMap<>();
            Map<Integer, Map<String, Double>> roundPricesMap = new HashMap<>();

            for (Row row : sheet) {
                if (!foundHeader) {
                    for (Cell cell : row) {
                        String val = getCellValue(cell);
                        if (val.contains("供应商名称")) supIdx = cell.getColumnIndex();
                        for (int i = 0; i < rounds.length; i++) {
                            if (val.equals(rounds[i])) colIdxMap[i] = cell.getColumnIndex();
                        }
                    }
                    if (supIdx != -1) foundHeader = true;
                    continue;
                }

                String company = getCellValue(row.getCell(supIdx));
                if (company.isEmpty() || company.contains("※")) continue;

                Double prevPrice = null;
                String prevRoundName = null;
                List<QuoteDropNode> companyDrops = new ArrayList<>();
                List<PriceNode> companyPrices = new ArrayList<>();

                for (int i = 0; i < colIdxMap.length; i++) {
                    int cIdx = colIdxMap[i];
                    if (cIdx == -1) continue;
                    Double currPrice = getNumericValue(row.getCell(cIdx));

                    if (currPrice != null) {
                        companyPrices.add(new PriceNode(rounds[i], currPrice));
                        roundPricesMap.computeIfAbsent(i, k -> new HashMap<>()).put(company, currPrice);

                        if (prevPrice != null && prevRoundName != null) {
                            if (currPrice > prevPrice) {
                                errors.add("【涨价异常】" + company + "\n    " + rounds[i] + "(" + currPrice + ") 高于上一轮(" + prevPrice + ")");
                                flaggedCompanies.add(company);
                            }

                            double dropPercent = 0.0;
                            if (prevPrice > 0) {
                                dropPercent = (prevPrice - currPrice) / prevPrice * 100.0;
                                String percentStr = String.format("%.4f", dropPercent);

                                percentDuplicateMap.putIfAbsent(i, new HashMap<>());
                                percentDuplicateMap.get(i).putIfAbsent(percentStr, new ArrayList<>());
                                percentDuplicateMap.get(i).get(percentStr).add(company);

                                double dropValue = prevPrice - currPrice;
                                if (dropValue > 0.0001) {
                                    String valueStr = String.format("%.2f", dropValue);
                                    valueDuplicateMap.putIfAbsent(i, new HashMap<>());
                                    valueDuplicateMap.get(i).putIfAbsent(valueStr, new ArrayList<>());
                                    valueDuplicateMap.get(i).get(valueStr).add(company);
                                }
                            }

                            String phaseName = prevRoundName.replace("报价", "") + "->" + rounds[i].replace("报价", "");
                            companyDrops.add(new QuoteDropNode(phaseName, dropPercent));
                        }
                        prevPrice = currPrice;
                        prevRoundName = rounds[i];
                    }
                }
                if (!companyDrops.isEmpty()) dropDataMap.put(company, companyDrops);
                if (!companyPrices.isEmpty()) priceDataMap.put(company, companyPrices);
            }

            for (Map.Entry<Integer, Map<String, List<String>>> roundEntry : percentDuplicateMap.entrySet()) {
                String roundName = rounds[roundEntry.getKey()];
                for (Map.Entry<String, List<String>> pEntry : roundEntry.getValue().entrySet()) {
                    List<String> companies = pEntry.getValue();
                    if (companies.size() > 1 && Double.parseDouble(pEntry.getKey()) > 0.0001) {
                        errors.add("【一致比例降幅】" + roundName + " 降幅: " + pEntry.getKey() + "%\n    涉及:\n      - " + String.join("\n      - ", companies));
                        flaggedCompanies.addAll(companies);
                    }
                }
            }

            for (Map.Entry<Integer, Map<String, List<String>>> roundEntry : valueDuplicateMap.entrySet()) {
                String roundName = rounds[roundEntry.getKey()];
                for (Map.Entry<String, List<String>> vEntry : roundEntry.getValue().entrySet()) {
                    List<String> companies = vEntry.getValue();
                    if (companies.size() > 1) {
                        errors.add("【一致定额降幅】" + roundName + " 降幅: " + vEntry.getKey() + " \n    涉及:\n      - " + String.join("\n      - ", companies));
                        flaggedCompanies.addAll(companies);
                    }
                }
            }

            for (Map.Entry<Integer, Map<String, Double>> entry : roundPricesMap.entrySet()) {
                int roundIdx = entry.getKey();
                String roundName = rounds[roundIdx];
                Map<String, Double> prices = entry.getValue();
                if (prices.size() >= 3) {
                    checkAPGP(roundName, prices, errors, flaggedCompanies);
                }
            }

            if (errors.isEmpty() && flaggedCompanies.isEmpty()) {
                JOptionPane.showMessageDialog(null, "校对完成，数据逻辑正常。");
            } else {
                Map<String, List<QuoteDropNode>> flaggedHistoryDrops = new HashMap<>();
                Map<String, List<PriceNode>> flaggedHistoryPrices = new HashMap<>();

                for (String c : flaggedCompanies) {
                    if (dropDataMap.containsKey(c)) flaggedHistoryDrops.put(c, dropDataMap.get(c));
                    if (priceDataMap.containsKey(c)) flaggedHistoryPrices.put(c, priceDataMap.get(c));
                }
                showResultsDialog(errors, flaggedHistoryPrices, flaggedHistoryDrops);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "解析失败，请检查文件格式。");
        }
    }

    private void checkAPGP(String roundName, Map<String, Double> companyPrices, List<String> errors, Set<String> flaggedCompanies) {
        // ================= 基础排序：升序（用于常规的等差、等比校验） =================
        List<Map.Entry<String, Double>> list = new ArrayList<>(companyPrices.entrySet());
        list.sort(Map.Entry.comparingByValue());
        int n = list.size();

        // 1. 常规直接等差与直接等比校验（沿用原逻辑，从小到大）
        for (int i = 0; i < n - 2; i++) {
            for (int j = i + 1; j < n - 1; j++) {
                double p1 = list.get(i).getValue();
                double p2 = list.get(j).getValue();

                double diff = p2 - p1;
                if (diff > 0.0001) {
                    List<String> apSeq = new ArrayList<>();
                    List<String> rawNames = new ArrayList<>();
                    apSeq.add(list.get(i).getKey() + "(" + p1 + ")");
                    apSeq.add(list.get(j).getKey() + "(" + p2 + ")");
                    rawNames.add(list.get(i).getKey());
                    rawNames.add(list.get(j).getKey());

                    double expected = p2 + diff;
                    for (int k = j + 1; k < n; k++) {
                        if (Math.abs(list.get(k).getValue() - expected) < 0.0001) {
                            apSeq.add(list.get(k).getKey() + "(" + list.get(k).getValue() + ")");
                            rawNames.add(list.get(k).getKey());
                            expected += diff;
                        }
                    }
                    if (apSeq.size() >= 3) {
                        errors.add(String.format("【等差串通预警】%s 中发现等差报价序列 (等差额: %.2f)\n    涉及:\n      - %s", roundName, diff, String.join("\n      - ", apSeq)));
                        flaggedCompanies.addAll(rawNames);
                    }
                }

                if (p1 > 0.0001) {
                    double ratio = p2 / p1;
                    if (ratio > 1.0001) {
                        List<String> gpSeq = new ArrayList<>();
                        List<String> rawNamesGP = new ArrayList<>();
                        gpSeq.add(list.get(i).getKey() + "(" + p1 + ")");
                        gpSeq.add(list.get(j).getKey() + "(" + p2 + ")");
                        rawNamesGP.add(list.get(i).getKey());
                        rawNamesGP.add(list.get(j).getKey());

                        double expected = p2 * ratio;
                        for (int k = j + 1; k < n; k++) {
                            if (Math.abs(list.get(k).getValue() - expected) < 0.0001) {
                                gpSeq.add(list.get(k).getKey() + "(" + list.get(k).getValue() + ")");
                                rawNamesGP.add(list.get(k).getKey());
                                expected *= ratio;
                            }
                        }
                        if (gpSeq.size() >= 3) {
                            errors.add(String.format("【等比串通预警】%s 中发现等比报价序列 (比例系数: %.4f)\n    涉及:\n      - %s", roundName, ratio, String.join("\n      - ", gpSeq)));
                            flaggedCompanies.addAll(rawNamesGP);
                        }
                    }
                }
            }
        }

        // ================= 2. 独立差值等比逻辑：降序（捕捉 0.5 这样的缩水差值系数） =================
        List<Map.Entry<String, Double>> descList = new ArrayList<>(companyPrices.entrySet());
        // 强制由大到小降序排列
        descList.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        for (int i = 0; i < n - 3; i++) {
            for (int j = i + 1; j < n - 2; j++) {
                for (int k = j + 1; k < n - 1; k++) {
                    double p1 = descList.get(i).getValue(); // 例如 1000
                    double p2 = descList.get(j).getValue(); // 例如 950
                    double p3 = descList.get(k).getValue(); // 例如 925

                    double d1 = p1 - p2; // 1000 - 950 = 50
                    double d2 = p2 - p3; // 950 - 925 = 25

                    // 确保差值有效且不为0，避免被除数为0引发NaN
                    if (d1 > 0.0001 && d2 > 0.0001) {
                        double diffRatio = d2 / d1; // 25 / 50 = 0.5

                        List<String> diffGpSeq = new ArrayList<>();
                        List<String> rawNamesDiffGP = new ArrayList<>();

                        diffGpSeq.add(descList.get(i).getKey() + "(" + p1 + ")");
                        diffGpSeq.add(descList.get(j).getKey() + "(" + p2 + ")");
                        diffGpSeq.add(descList.get(k).getKey() + "(" + p3 + ")");

                        rawNamesDiffGP.add(descList.get(i).getKey());
                        rawNamesDiffGP.add(descList.get(j).getKey());
                        rawNamesDiffGP.add(descList.get(k).getKey());

                        double expectedDiff = d2 * diffRatio;   // 25 * 0.5 = 12.5
                        double expectedPrice = p3 - expectedDiff; // 925 - 12.5 = 912.5

                        // 寻找第4个及之后延续此差值比例的报价点
                        for (int m = k + 1; m < n; m++) {
                            if (Math.abs(descList.get(m).getValue() - expectedPrice) < 0.0001) {
                                diffGpSeq.add(descList.get(m).getKey() + "(" + descList.get(m).getValue() + ")");
                                rawNamesDiffGP.add(descList.get(m).getKey());

                                expectedDiff *= diffRatio;
                                expectedPrice -= expectedDiff;
                            }
                        }

                        // 核心防误报逻辑：至少需要4家供应商配合才能确认“差值等比”规律
                        if (diffGpSeq.size() >= 4) {
                            errors.add(String.format("【差值等比串通预警】%s 中发现差值呈等比递减规律 (差值衰减系数: %.4f)\n    涉及:\n      - %s",
                                    roundName, diffRatio, String.join("\n      - ", diffGpSeq)));
                            flaggedCompanies.addAll(rawNamesDiffGP);
                        }
                    }
                }
            }
        }
    }

    // ================== 三分布局 UI 组装 ==================

    private void showResultsDialog(List<String> errors, Map<String, List<PriceNode>> priceData, Map<String, List<QuoteDropNode>> dropData) {
        JDialog dialog = new JDialog((Frame) null, "校对结果综合看板", true);
        dialog.setSize(1250, 700);
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout());

        dialog.getContentPane().setBackground(DASHBOARD_BG);

        // 1. 左侧：预警明细
        JTextArea logArea = new JTextArea(String.join("\n\n", errors));
        logArea.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setMargin(new Insets(10, 10, 10, 10));
        logArea.setBackground(DASHBOARD_BG);
        logArea.setForeground(TEXT_COLOR);

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setOpaque(false);
        logScroll.getViewport().setOpaque(false);
        logScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), "协同校验预警明细(单位:万元/%)",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("微软雅黑", Font.BOLD, 13), TEXT_COLOR));

        // 2. 中间：走势图面板
        PriceTrendChartPanel chartPanel = new PriceTrendChartPanel(priceData);
        chartPanel.setOpaque(false);
        chartPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), "异常供应商绝对报价走势 (单位: 万元)",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("微软雅黑", Font.BOLD, 13), TEXT_COLOR));

        // 3. 右侧：各家百分比列表
        JTextArea detailArea = new JTextArea();
        detailArea.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        detailArea.setEditable(false);
        detailArea.setBackground(DASHBOARD_BG);
        detailArea.setForeground(TEXT_COLOR);
        detailArea.setMargin(new Insets(10, 10, 10, 10));

        StringBuilder detailSb = new StringBuilder();
        for (Map.Entry<String, List<QuoteDropNode>> entry : dropData.entrySet()) {
            detailSb.append("【").append(entry.getKey()).append("】\n");
            for (QuoteDropNode node : entry.getValue()) {
                detailSb.append("  ").append(node.phaseName).append(" : ")
                        .append(String.format("%.2f%%", node.dropPercent)).append("\n");
            }
            detailSb.append("\n");
        }
        detailArea.setText(detailSb.toString());
        detailArea.setCaretPosition(0);

        JScrollPane detailScroll = new JScrollPane(detailArea);
        detailScroll.setOpaque(false);
        detailScroll.getViewport().setOpaque(false);
        detailScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), "各轮降幅百分比明细",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("微软雅黑", Font.BOLD, 13), TEXT_COLOR));
        detailScroll.setPreferredSize(new Dimension(280, 0));

        JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chartPanel, detailScroll);
        rightSplit.setOpaque(false);
        rightSplit.setResizeWeight(1.0);
        rightSplit.setDividerSize(4);
        rightSplit.setBorder(BorderFactory.createEmptyBorder());

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, logScroll, rightSplit);
        mainSplit.setOpaque(false);
        mainSplit.setDividerLocation(420);
        mainSplit.setDividerSize(4);
        mainSplit.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        dialog.add(mainSplit, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 15, 0));
        JButton exportBtn = new JButton(" 导出校验报告与看板截图 ");
        exportBtn.setFont(new Font("微软雅黑", Font.BOLD, 13));
        exportBtn.setFocusPainted(false);
        exportBtn.addActionListener(e -> exportDashboard(dialog, errors, dropData));
        bottomPanel.add(exportBtn);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void exportDashboard(JDialog dialog, List<String> errors, Map<String, List<QuoteDropNode>> dropData) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择看板导出保存位置");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showSaveDialog(dialog) == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            try {
                long timestamp = System.currentTimeMillis();

                File txtFile = new File(dir, "协同校验分析报告_" + timestamp + ".txt");
                try (PrintWriter writer = new PrintWriter(txtFile, "UTF-8")) {
                    writer.println("================ 数据逻辑协同预警明细 ================\n");
                    for (String err : errors) {
                        writer.println(err + "\n");
                    }
                    writer.println("\n================ 各轮降幅详细明细 ================\n");
                    for (Map.Entry<String, List<QuoteDropNode>> entry : dropData.entrySet()) {
                        writer.println("【" + entry.getKey() + "】");
                        for (QuoteDropNode node : entry.getValue()) {
                            writer.println("  " + node.phaseName + " : " + String.format("%.2f%%", node.dropPercent));
                        }
                        writer.println();
                    }
                }

                File imgFile = new File(dir, "综合看板截图_" + timestamp + ".png");
                BufferedImage img = new BufferedImage(dialog.getWidth(), dialog.getHeight(), BufferedImage.TYPE_INT_RGB);
                dialog.paint(img.getGraphics());
                ImageIO.write(img, "png", imgFile);

                JOptionPane.showMessageDialog(dialog, "看板数据与截图导出成功！\n保存路径：" + dir.getAbsolutePath());
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "导出异常: " + ex.getMessage());
            }
        }
    }

    // ================== 图表渲染引擎 ==================

    class PriceTrendChartPanel extends JPanel {
        private Map<String, List<PriceNode>> data;
        private Color[] lineColors = {
                new Color(231, 76, 60), new Color(52, 152, 219), new Color(46, 204, 113),
                new Color(155, 89, 182), new Color(241, 196, 15), new Color(230, 126, 34)
        };

        private double scale = 1.0;
        private double translateX = 0.0;
        private double translateY = 0.0;
        private Point dragStart = null;

        private Map<Rectangle, String> tooltipMap = new HashMap<>();

        public PriceTrendChartPanel(Map<String, List<PriceNode>> data) {
            this.data = data;

            ToolTipManager.sharedInstance().registerComponent(this);
            setToolTipText("");

            addMouseWheelListener(e -> {
                double oldScale = scale;
                if (e.getWheelRotation() < 0) scale *= 1.15;
                else scale /= 1.15;
                scale = Math.max(0.3, Math.min(scale, 10.0));

                double f = scale / oldScale;
                translateX = e.getX() - f * (e.getX() - translateX);
                translateY = e.getY() - f * (e.getY() - translateY);
                repaint();
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) { dragStart = e.getPoint(); }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragStart != null) {
                        translateX += e.getX() - dragStart.getX();
                        translateY += e.getY() - dragStart.getY();
                        dragStart = e.getPoint();
                        repaint();
                    }
                }
            });
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            Point p = event.getPoint();
            for (Map.Entry<Rectangle, String> entry : tooltipMap.entrySet()) {
                if (entry.getKey().contains(p)) {
                    return entry.getValue();
                }
            }
            return null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            tooltipMap.clear();

            if (data == null || data.isEmpty()) return;

            AffineTransform saveAT = g2d.getTransform();
            g2d.translate(translateX, translateY);
            g2d.scale(scale, scale);
            AffineTransform logicalAT = g2d.getTransform();

            // 为了左侧放得下数字刻度，把 padding 从原来的 60 稍微调大到 70
            int padding = 70;
            int width = Math.max(1, getWidth() - padding * 2);
            int height = Math.max(1, getHeight() - padding * 2);

            List<String> allRounds = new ArrayList<>();
            double maxPrice = 0.0, minPrice = Double.MAX_VALUE;
            for (List<PriceNode> nodes : data.values()) {
                for (PriceNode node : nodes) {
                    if (!allRounds.contains(node.roundName)) allRounds.add(node.roundName);
                    if (node.price > maxPrice) maxPrice = node.price;
                    if (node.price < minPrice) minPrice = node.price;
                }
            }
            if (minPrice == Double.MAX_VALUE) minPrice = 0;
            if (maxPrice == minPrice) { maxPrice += 10; minPrice = Math.max(0, minPrice - 10); }
            else {
                double diff = maxPrice - minPrice;
                maxPrice += diff * 0.15;
                minPrice = Math.max(0, minPrice - diff * 0.15);
            }

            // ================== 新增：绘制 Y 轴主线、数字刻度与参考横线 ==================
            g2d.setColor(new Color(80, 80, 80));
            g2d.drawLine(padding, padding, padding, padding + height); // Y 轴主线

            int yTicks = 5; // 将 Y 轴分为 5 个刻度区间
            g2d.setFont(new Font("微软雅黑", Font.PLAIN, 11));
            FontMetrics fm = g2d.getFontMetrics();

            for (int i = 0; i <= yTicks; i++) {
                double tickValue = minPrice + (maxPrice - minPrice) * i / yTicks;
                int yPos = padding + height - (int) (height * i / yTicks);

                // 1. 画横向虚线辅助网格（可选，让图表更专业）
                g2d.setColor(new Color(60, 60, 60));
                Stroke oldStroke = g2d.getStroke();
                g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f}, 0.0f));
                g2d.drawLine(padding, yPos, padding + width, yPos);
                g2d.setStroke(oldStroke);

                // 2. 画 Y 轴数字刻度标签
                g2d.setColor(TEXT_COLOR);
                String label = String.format("%.2f", tickValue); // 保留两位小数
                int labelWidth = fm.stringWidth(label);
                // 数字向左偏移，并在右侧留出 8 像素的间距
                g2d.drawString(label, padding - labelWidth - 8, yPos + 4);
            }
            // =========================================================================

            g2d.setColor(TEXT_COLOR);
            g2d.setFont(new Font("微软雅黑", Font.BOLD, 12));
            int roundCount = Math.max(1, allRounds.size());
            int xStep = width / roundCount;
            for (int i = 0; i < allRounds.size(); i++) {
                int x = padding + i * xStep + (xStep / 2);
                g2d.drawString(allRounds.get(i), x - 20, padding + height + 25);
            }

            int colorIdx = 0;
            g2d.setStroke(new BasicStroke(2.0f));

            for (Map.Entry<String, List<PriceNode>> entry : data.entrySet()) {
                Color c = lineColors[colorIdx % lineColors.length];
                List<PriceNode> nodes = entry.getValue();
                String companyName = entry.getKey();

                Integer prevX = null, prevY = null;
                for (PriceNode node : nodes) {
                    int roundIdx = allRounds.indexOf(node.roundName);
                    if (roundIdx == -1) continue;

                    int x = padding + roundIdx * xStep + (xStep / 2);
                    int y = padding + height - (int) ((node.price - minPrice) / (maxPrice - minPrice) * height);

                    g2d.setColor(c);
                    if (prevX != null && prevY != null) g2d.drawLine(prevX, prevY, x, y);
                    g2d.fillOval(x - 5, y - 5, 10, 10);

                    Point2D.Double src = new Point2D.Double(x, y);
                    Point2D.Double dst = new Point2D.Double();
                    logicalAT.transform(src, dst);
                    Rectangle pointRect = new Rectangle((int)dst.x - 7, (int)dst.y - 7, 14, 14);
                    tooltipMap.put(pointRect, companyName + " (" + node.roundName + ": " + node.price + " 万)");

                    prevX = x; prevY = y;
                }
                colorIdx++;
            }

            g2d.setTransform(saveAT);

            int legendY = 20;
            colorIdx = 0;
            for (Map.Entry<String, List<PriceNode>> entry : data.entrySet()) {
                Color c = lineColors[colorIdx % lineColors.length];
                g2d.setColor(c);
                g2d.fillRect(getWidth() - 130, legendY, 15, 15);
                g2d.setColor(TEXT_COLOR);
                g2d.setFont(new Font("微软雅黑", Font.PLAIN, 12));

                String fullCompanyName = entry.getKey();
                String shortName = fullCompanyName.length() > 6 ? fullCompanyName.substring(0,6) + ".." : fullCompanyName;
                g2d.drawString(shortName, getWidth() - 105, legendY + 12);

                Rectangle legRect = new Rectangle(getWidth() - 130, legendY, 120, 15);
                tooltipMap.put(legRect, fullCompanyName);

                legendY += 25;
                colorIdx++;
            }

            g2d.setColor(Color.GRAY);
            g2d.drawString("图表支持鼠标拖拽与滚轮缩放查看细节", 15, 25);
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        try {
            if (cell.getCellType() == CellType.NUMERIC) return String.valueOf(cell.getNumericCellValue());
            return cell.getStringCellValue().trim();
        } catch (Exception e) { return ""; }
    }

    private Double getNumericValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA) return cell.getNumericCellValue();
            return Double.parseDouble(cell.getStringCellValue().trim());
        } catch (Exception e) { return null; }
    }
}