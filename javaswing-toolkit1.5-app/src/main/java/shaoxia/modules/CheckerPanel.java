package shaoxia.modules;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.awt.Font;
import shaoxia.MainLauncher;
import shaoxia.Utils.ActionButton;
import shaoxia.Utils.GlassComboBox;
import shaoxia.Utils.GlassDropPanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.List;

/**
 * 数据校对助手 CheckerPanel (极致无按钮版 - 沉浸式 Travelers 交互 + 模糊比对功能)
 */
public class CheckerPanel extends BackgroundPanel {

    private JLabel dl;
    private JPanel dropArea;

    private final Color DASHBOARD_BG = new Color(45, 48, 50);
    private final Color TEXT_COLOR = new Color(230, 230, 230);

    // 音乐播放控制
    private volatile boolean isPlaying = false;
    private javazoom.jl.player.Player player;

    // 模糊比对级别 (0: 精确, 1: 个位, 2: 十位, 3: 百位)
    private int fuzzyLevel = 0;

    // --- 高级现代感 UI 颜色配置 ---
    private final Color MODERN_BG = new Color(30, 32, 36);        // 极度深邃的背景黑
    private final Color PANEL_BG = new Color(42, 45, 52);         // 面板浮层黑
    private final Color BORDER_COLOR = new Color(65, 70, 80);     // 隐约可见的边框灰
    private final Color TEXT_PRIMARY = new Color(240, 240, 245);  // 高亮白字体
    private final Color TEXT_SECONDARY = new Color(170, 175, 185);// 次级灰字体
    private final Font TITLE_FONT = new Font("微软雅黑", Font.BOLD, 15);
    private final Font BODY_FONT = new Font("微软雅黑", Font.PLAIN, 14);

    public CheckerPanel(MainLauncher parent) {
        super("bg_imgs/月球与地球.png");
        setLayout(new BorderLayout());
        setOpaque(false);

        // 仅保留最基础的返回主菜单（防困死），其他按钮全部拔除
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 20));

        ActionButton backBtn = new ActionButton("<< 返回菜单");
        backBtn.setPreferredSize(new Dimension(130, 36));
        backBtn.addActionListener(e -> {
            stopMusic();
            parent.showMenu();
        });

        // 取消 FlowLayout 默认的 5px 间隙，实现精确像素对齐
        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftTop.setOpaque(false);
        leftTop.add(backBtn);
        topBar.add(leftTop, BorderLayout.WEST);

        // 顶部导包 (如果没有的话请加上)
        // import shaoxia.Utils.GlassComboBox;

        // --- 新增右上角：模糊比对选项 ---
        JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 5));
        rightTop.setOpaque(false);

        String[] fuzzyOptions = {"精确校对", "模糊至个位校对", "模糊至十位校对", "模糊至百位校对"};

        // ✨ 修改点：实现隐形式下拉框 (居中、加粗、悬停显现)
        JComboBox<String> fuzzyCombo = new JComboBox<>(fuzzyOptions) {
            private boolean isHovered = false;
            private final Color glassColor = new Color(30, 35, 40, 120);

            {
                setOpaque(false);
                setBackground(new Color(0, 0, 0, 0));
                setForeground(Color.WHITE);
                setFont(new Font("微软雅黑", Font.BOLD, 14)); // ✨ 主字体加粗

                // 监听鼠标悬浮状态
                MouseAdapter hoverAdapter = new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                    @Override
                    public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
                };
                addMouseListener(hoverAdapter);

                // 自定义 UI
                setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
                    @Override
                    protected JButton createArrowButton() {
                        JButton btn = new JButton("▼") {
                            @Override
                            public void paint(Graphics g) {
                                // ✨ 只有鼠标悬停或下拉列表展开时，才显示右侧的小箭头
                                // 使用 comboBox.isPopupVisible() 明确指向外层的下拉框
                                if (isHovered || comboBox.isPopupVisible()) {
                                    super.paint(g);
                                }
                            }
                        };
                        btn.setContentAreaFilled(false);
                        btn.setBorderPainted(false);
                        btn.setFocusPainted(false);
                        btn.setForeground(new Color(255, 255, 255, 180));
                        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                        return btn;
                    }
                    @Override
                    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {}
                });

                // 让内部组件(如箭头)也感应 hover，防止鼠标移动时闪烁
                for (Component c : getComponents()) {
                    c.addMouseListener(hoverAdapter);
                }

                // ✨ 修改 Renderer 实现文字居中和列表项加粗
                // ✨ 修改 Renderer 实现文字居中和列表项加粗
                setRenderer(new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                        l.setHorizontalAlignment(SwingConstants.CENTER); // ✨ 居中
                        l.setFont(new Font("微软雅黑", Font.BOLD, 14)); // ✨ 加粗
                        l.setOpaque(index != -1);
                        l.setBackground(isSelected ? new Color(80, 90, 100) : new Color(40, 45, 50));
                        l.setForeground(Color.WHITE);

                        // ✨ 核心修改：区分选中项（主框体内）和下拉列表项的边距
                        if (index == -1) {
                            // index 为 -1 代表当前显示在选择框主框体内的那一项
                            // 增加左侧边距（将原来的 8 改为 20 或更大），抵消右侧下拉箭头占用的视觉空间，使其向右偏移
                            l.setBorder(BorderFactory.createEmptyBorder(4, 25, 4, 8));
                        } else {
                            // 下拉列表中的普通项保持不变
                            l.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                        }

                        return l;
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                // ✨ 只有 hover 或者点开时，才渲染底部的毛玻璃背景，平时隐藏
                if (isHovered || isPopupVisible()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(glassColor);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                    g2.dispose();
                }
                super.paintComponent(g); // 文本始终显示
            }

            @Override
            protected void paintBorder(Graphics g) {
                // ✨ 同理，只有 hover 时才显示边框线条
                if (isHovered || isPopupVisible()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(255, 255, 255, 60));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
                    g2.dispose();
                }
            }
        };

        // 保持与左侧返回按钮一致的 (130, 36) 大小
        fuzzyCombo.setPreferredSize(new Dimension(180, 36));
        fuzzyCombo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        fuzzyCombo.setFocusable(false);
        fuzzyCombo.addActionListener(e -> fuzzyLevel = fuzzyCombo.getSelectedIndex());

        rightTop.add(fuzzyCombo);
        topBar.add(rightTop, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // 修复缩放消失：改用弹性边距包裹的 BorderLayout
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        // 这里的边距是弹性的，窗口变小也不会把中间的框挤碎
        centerPanel.setBorder(BorderFactory.createEmptyBorder(60, 80, 80, 80));

        // 直接实例化全局拖拽面板组件
        dropArea = new GlassDropPanel();
        dropArea.setOpaque(false);
        dropArea.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 让黑洞本身变成交互按钮
        dropArea.setCursor(new Cursor(Cursor.HAND_CURSOR));
        dropArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleMusic(); // 点击任意区域触发音乐
            }
        });

        dl = new JLabel("", JLabel.CENTER);
        dropArea.add(dl, BorderLayout.CENTER);

        // 保持拖拽进入区域执行校对的功能
        setCheckDropTarget(dropArea);

        centerPanel.add(dropArea, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        adjustFontSize();

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustFontSize();
            }
        });
    }

    private void adjustFontSize() {
        int width = getWidth();
        if (width <= 0) return;

        double scale = Math.max(0.6, Math.min(width / 800.0, 1.4));
        int titleSize = (int) (48 * scale);

        // 保持极致极简
        dl.setText("<html><center>" +
                "<span style='font-family: monospaced; font-size:" + titleSize + "px; font-weight:bold; color:white; letter-spacing: 8px;'>" +
                "Travelers</span>" +
                "</center></html>");

        dropArea.repaint();
        this.validate();
    }

    // --- 无界面触发的音乐控制逻辑 ---
    private void toggleMusic() {
        if (isPlaying) {
            stopMusic();
        } else {
            isPlaying = true;
            new Thread(() -> {
                try {
                    java.io.InputStream is = getClass().getResourceAsStream("/music/Travelers.mp3");
                    if (is != null) {
                        player = new javazoom.jl.player.Player(is);
                        player.play(); // 阻塞播放

                        if (isPlaying) {
                            isPlaying = false;
                        }
                    } else {
                        System.out.println("🎵 提示：类路径中未找到 /music/Travelers.mp3");
                        isPlaying = false;
                    }
                } catch (Exception e) {
                    System.out.println("🎵 音乐播放被中断或出现异常: " + e.getMessage());
                    isPlaying = false;
                }
            }).start();
        }
    }

    private void stopMusic() {
        isPlaying = false;
        if (player != null) {
            player.close(); // 强行切断底层音频流
        }
    }

    private void setCheckDropTarget(JPanel p) {
        new DropTarget(p, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) processExcel(files.get(0));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
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

    /**
     * 根据当前选择的模糊级别对价格进行四舍五入
     */
    private Double applyFuzzyRounding(Double price, int level) {
        if (price == null) return null;
        switch (level) {
            case 1: return (double) Math.round(price); // 个位
            case 2: return (double) (Math.round(price / 10.0) * 10); // 十位
            case 3: return (double) (Math.round(price / 100.0) * 100); // 百位
            default: return price; // 精确
        }
    }

    private void processExcel(File file) {
        List<String> globalErrors = new ArrayList<>();
        Map<String, List<QuoteDropNode>> globalDropDataMap = new LinkedHashMap<>();
        Map<String, List<PriceNode>> globalPriceDataMap = new LinkedHashMap<>();
        Set<String> globalFlaggedKeys = new HashSet<>();

        Map<String, Map<Integer, Map<String, List<String>>>> pkgPercentDupMap = new HashMap<>();
        Map<String, Map<Integer, Map<String, List<String>>>> pkgValueDupMap = new HashMap<>();
        Map<String, Map<Integer, Map<String, Double>>> pkgRoundPricesMap = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(file); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            int supIdx = -1;
            int pkgIdx = -1;
            String[] rounds = {"首轮报价", "二轮报价", "三轮报价", "四轮报价", "五轮报价", "六轮报价", "终轮报价"};
            int[] colIdxMap = new int[rounds.length];
            Arrays.fill(colIdxMap, -1);
            boolean foundHeader = false;

            for (Row row : sheet) {
                if (!foundHeader) {
                    for (Cell cell : row) {
                        String val = getCellValue(cell);
                        if (val.contains("供应商名称")) supIdx = cell.getColumnIndex();
                        if (val.equals("标包编号")) pkgIdx = cell.getColumnIndex();
                        for (int i = 0; i < rounds.length; i++) {
                            if (val.equals(rounds[i])) colIdxMap[i] = cell.getColumnIndex();
                        }
                    }
                    if (supIdx != -1) foundHeader = true;
                    continue;
                }

                String company = getCellValue(row.getCell(supIdx)).replaceAll("[\\n\\r\\t]", "").trim();
                if (company.isEmpty() || company.contains("※")) continue;

                String pkgName = "默认标包";
                if (pkgIdx != -1) {
                    String rawPkg = getCellValue(row.getCell(pkgIdx));
                    if (!rawPkg.trim().isEmpty()) {
                        // ✨ 核心修改：将字符串中的 ^L^ 替换为空，实现隐藏效果
                        pkgName = rawPkg.trim().replace("^L^", "");
                    }
                }
                String uniqueKey = company + " [" + pkgName + "]";

                Double prevPrice = null;
                String prevRoundName = null;
                List<QuoteDropNode> companyDrops = new ArrayList<>();
                List<PriceNode> companyPrices = new ArrayList<>();

                for (int i = 0; i < colIdxMap.length; i++) {
                    int cIdx = colIdxMap[i];
                    if (cIdx == -1) continue;

                    Double currPrice = getNumericValue(row.getCell(cIdx));

                    if (currPrice != null) {
                        // ✨ 新增模糊比对拦截，在此处对抓取到的价格进行四舍五入
                        currPrice = applyFuzzyRounding(currPrice, fuzzyLevel);

                        companyPrices.add(new PriceNode(rounds[i], currPrice));

                        pkgRoundPricesMap.computeIfAbsent(pkgName, k -> new HashMap<>())
                                .computeIfAbsent(i, k -> new HashMap<>())
                                .put(uniqueKey, currPrice);

                        if (prevPrice != null && prevRoundName != null) {
                            if (currPrice > prevPrice) {
                                globalErrors.add("【涨价异常】" + uniqueKey + "\n    " + rounds[i] + "(" + currPrice + ") 高于上一轮(" + prevPrice + ")");
                                globalFlaggedKeys.add(uniqueKey);
                            }

                            double dropPercent = 0.0;
                            if (prevPrice > 0) {
                                dropPercent = (prevPrice - currPrice) / prevPrice * 100.0;
                                String percentStr = String.format("%.4f", dropPercent);

                                pkgPercentDupMap.computeIfAbsent(pkgName, k -> new HashMap<>())
                                        .computeIfAbsent(i, k -> new HashMap<>())
                                        .computeIfAbsent(percentStr, k -> new ArrayList<>())
                                        .add(uniqueKey);

                                double dropValue = prevPrice - currPrice;
                                if (dropValue > 0.0001) {
                                    String valueStr = String.format("%.2f", dropValue);
                                    pkgValueDupMap.computeIfAbsent(pkgName, k -> new HashMap<>())
                                            .computeIfAbsent(i, k -> new HashMap<>())
                                            .computeIfAbsent(valueStr, k -> new ArrayList<>())
                                            .add(uniqueKey);
                                }
                            }

                            String phaseName = prevRoundName.replace("报价", "") + "->" + rounds[i].replace("报价", "");
                            companyDrops.add(new QuoteDropNode(phaseName, dropPercent));
                        }
                        prevPrice = currPrice;
                        prevRoundName = rounds[i];
                    }
                }
                if (!companyDrops.isEmpty()) globalDropDataMap.put(uniqueKey, companyDrops);
                if (!companyPrices.isEmpty()) globalPriceDataMap.put(uniqueKey, companyPrices);
            }

            for (String pkgName : pkgRoundPricesMap.keySet()) {

                Map<Integer, Map<String, List<String>>> percentMap = pkgPercentDupMap.getOrDefault(pkgName, new HashMap<>());
                for (Map.Entry<Integer, Map<String, List<String>>> roundEntry : percentMap.entrySet()) {
                    String roundName = rounds[roundEntry.getKey()];
                    for (Map.Entry<String, List<String>> pEntry : roundEntry.getValue().entrySet()) {
                        List<String> keys = pEntry.getValue();
                        if (keys.size() > 1 && Double.parseDouble(pEntry.getKey()) > 0.0001) {
                            globalErrors.add("【一致比例降幅】[" + pkgName + "] " + roundName + " 降幅: " + pEntry.getKey() + "%\n    涉及:\n      - " + String.join("\n      - ", keys));
                            globalFlaggedKeys.addAll(keys);
                        }
                    }
                }

                Map<Integer, Map<String, List<String>>> valueMap = pkgValueDupMap.getOrDefault(pkgName, new HashMap<>());
                for (Map.Entry<Integer, Map<String, List<String>>> roundEntry : valueMap.entrySet()) {
                    String roundName = rounds[roundEntry.getKey()];
                    for (Map.Entry<String, List<String>> vEntry : roundEntry.getValue().entrySet()) {
                        List<String> keys = vEntry.getValue();
                        if (keys.size() > 1) {
                            globalErrors.add("【一致定额降幅】[" + pkgName + "] " + roundName + " 降幅: " + vEntry.getKey() + " \n    涉及:\n      - " + String.join("\n      - ", keys));
                            globalFlaggedKeys.addAll(keys);
                        }
                    }
                }

                Map<Integer, Map<String, Double>> roundPrices = pkgRoundPricesMap.get(pkgName);
                if (roundPrices != null) {
                    for (Map.Entry<Integer, Map<String, Double>> entry : roundPrices.entrySet()) {
                        int roundIdx = entry.getKey();
                        String roundName = rounds[roundIdx];
                        Map<String, Double> prices = entry.getValue();
                        if (prices.size() >= 3) {
                            checkAPGP("[" + pkgName + "] " + roundName, prices, globalErrors, globalFlaggedKeys);
                        }
                    }
                }
            }

            if (globalErrors.isEmpty() && globalFlaggedKeys.isEmpty()) {
                JOptionPane.showMessageDialog(null, "校对完成，数据逻辑正常，未发现同标包协同风险。");

            } else {
                Map<String, List<QuoteDropNode>> flaggedHistoryDrops = new HashMap<>();
                Map<String, List<PriceNode>> flaggedHistoryPrices = new HashMap<>();

                for (String k : globalFlaggedKeys) {
                    if (globalDropDataMap.containsKey(k)) flaggedHistoryDrops.put(k, globalDropDataMap.get(k));
                    if (globalPriceDataMap.containsKey(k)) flaggedHistoryPrices.put(k, globalPriceDataMap.get(k));
                }
                showResultsDialog(globalErrors, flaggedHistoryPrices, flaggedHistoryDrops);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "解析失败，请检查文件格式。");
        }
    }

    private void checkAPGP(String roundName, Map<String, Double> companyPrices, List<String> errors, Set<String> flaggedCompanies) {
        List<Map.Entry<String, Double>> list = new ArrayList<>(companyPrices.entrySet());
        list.sort(Map.Entry.comparingByValue());
        int n = list.size();

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

        List<Map.Entry<String, Double>> descList = new ArrayList<>(companyPrices.entrySet());
        descList.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        for (int i = 0; i < n - 3; i++) {
            for (int j = i + 1; j < n - 2; j++) {
                for (int k = j + 1; k < n - 1; k++) {
                    double p1 = descList.get(i).getValue();
                    double p2 = descList.get(j).getValue();
                    double p3 = descList.get(k).getValue();

                    double d1 = p1 - p2;
                    double d2 = p2 - p3;

                    if (d1 > 0.0001 && d2 > 0.0001) {
                        double diffRatio = d2 / d1;

                        List<String> diffGpSeq = new ArrayList<>();
                        List<String> rawNamesDiffGP = new ArrayList<>();

                        diffGpSeq.add(descList.get(i).getKey() + "(" + p1 + ")");
                        diffGpSeq.add(descList.get(j).getKey() + "(" + p2 + ")");
                        diffGpSeq.add(descList.get(k).getKey() + "(" + p3 + ")");

                        rawNamesDiffGP.add(descList.get(i).getKey());
                        rawNamesDiffGP.add(descList.get(j).getKey());
                        rawNamesDiffGP.add(descList.get(k).getKey());

                        double expectedDiff = d2 * diffRatio;
                        double expectedPrice = p3 - expectedDiff;

                        for (int m = k + 1; m < n; m++) {
                            if (Math.abs(descList.get(m).getValue() - expectedPrice) < 0.0001) {
                                diffGpSeq.add(descList.get(m).getKey() + "(" + descList.get(m).getValue() + ")");
                                rawNamesDiffGP.add(descList.get(m).getKey());

                                expectedDiff *= diffRatio;
                                expectedPrice -= expectedDiff;
                            }
                        }

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

    private void showResultsDialog(List<String> errors, Map<String, List<PriceNode>> priceData, Map<String, List<QuoteDropNode>> dropData) {
        JDialog dialog = new JDialog((Frame) null, "协同校验看板", true);
        dialog.setSize(1280, 760);
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(MODERN_BG);

        // --- 1. 左侧：预警明细日志 ---
        JTextArea logArea = new JTextArea(String.join("\n\n", errors));
        logArea.setFont(BODY_FONT);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setMargin(new Insets(15, 15, 15, 15));
        logArea.setBackground(PANEL_BG);
        logArea.setForeground(TEXT_PRIMARY);

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel logWrapper = new JPanel(new BorderLayout());
        logWrapper.setBackground(PANEL_BG);
        logWrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        // ✨ 修改点：去除了前面的符号
        JLabel logTitle = new JLabel(" 协同校验预警明细 (万元 / %)");
        logTitle.setFont(TITLE_FONT);
        logTitle.setForeground(new Color(255, 107, 107));
        logTitle.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        logWrapper.add(logTitle, BorderLayout.NORTH);
        logWrapper.add(logScroll, BorderLayout.CENTER);

        // --- 2. 右侧上：高级折线图表 ---
        PriceTrendChartPanel chartPanel = new PriceTrendChartPanel(priceData);
        chartPanel.setBackground(PANEL_BG);

        JPanel chartWrapper = new JPanel(new BorderLayout());
        chartWrapper.setBackground(PANEL_BG);
        chartWrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        // ✨ 修改点：去除了前面的符号
        JLabel chartTitle = new JLabel(" 异常报价绝对走势图 (万元)");
        chartTitle.setFont(TITLE_FONT);
        chartTitle.setForeground(TEXT_SECONDARY);
        chartTitle.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        chartWrapper.add(chartTitle, BorderLayout.NORTH);
        chartWrapper.add(chartPanel, BorderLayout.CENTER);

        // --- 3. 右侧下：降幅明细 ---
        JTextArea detailArea = new JTextArea();
        detailArea.setFont(BODY_FONT);
        detailArea.setEditable(false);
        detailArea.setBackground(PANEL_BG);
        detailArea.setForeground(TEXT_PRIMARY);
        detailArea.setMargin(new Insets(15, 15, 15, 15));

        StringBuilder detailSb = new StringBuilder();
        for (Map.Entry<String, List<QuoteDropNode>> entry : dropData.entrySet()) {
            detailSb.append(" ▪ ").append(entry.getKey()).append("\n");
            for (QuoteDropNode node : entry.getValue()) {
                detailSb.append("    └─ ").append(node.phaseName).append(" : ")
                        .append(String.format("%.2f%%", node.dropPercent)).append("\n");
            }
            detailSb.append("\n");
        }
        detailArea.setText(detailSb.toString());
        detailArea.setCaretPosition(0);

        JScrollPane detailScroll = new JScrollPane(detailArea);
        detailScroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel detailWrapper = new JPanel(new BorderLayout());
        detailWrapper.setBackground(PANEL_BG);
        detailWrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        // ✨ 修改点：去除了前面的符号
        JLabel detailTitle = new JLabel(" 各轮降幅百分比穿透");
        detailTitle.setFont(TITLE_FONT);
        detailTitle.setForeground(TEXT_SECONDARY);
        detailTitle.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        detailWrapper.add(detailTitle, BorderLayout.NORTH);
        detailWrapper.add(detailScroll, BorderLayout.CENTER);

        // --- 布局组合 ---
        JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chartWrapper, detailWrapper);
        rightSplit.setOpaque(false);
        rightSplit.setResizeWeight(0.65);
        rightSplit.setDividerSize(8);
        rightSplit.setBorder(BorderFactory.createEmptyBorder());

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, logWrapper, rightSplit);
        mainSplit.setOpaque(false);
        mainSplit.setDividerLocation(420);
        mainSplit.setDividerSize(8);
        mainSplit.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        dialog.add(mainSplit, BorderLayout.CENTER);

        // --- 底部：现代感导出按钮 ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 15));
        bottomPanel.setBackground(MODERN_BG);

        JButton exportBtn = new JButton("导出报告与截图");
        exportBtn.setFont(new Font("微软雅黑", Font.BOLD, 14));
        exportBtn.setForeground(Color.WHITE);
        exportBtn.setBackground(new Color(52, 152, 219));
        exportBtn.setFocusPainted(false);
        exportBtn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));

        exportBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { exportBtn.setBackground(new Color(41, 128, 185)); }
            public void mouseExited(MouseEvent e) { exportBtn.setBackground(new Color(52, 152, 219)); }
        });
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
                // ✨ 核心修改：生成年月日时分秒时间戳
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());

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

    // --- 高级图表 UI 重构 ---
    class PriceTrendChartPanel extends JPanel {
        private Map<String, List<PriceNode>> data;
        private Color[] lineColors = {
                new Color(255, 107, 107), new Color(72, 219, 251), new Color(29, 209, 161),
                new Color(165, 94, 234), new Color(254, 202, 87), new Color(255, 159, 67)
        };

        private double scale = 1.0;
        private double translateX = 0.0;
        private double translateY = 0.0;
        private Point dragStart = null;
        private Map<Rectangle, String> tooltipMap = new HashMap<>();

        public PriceTrendChartPanel(Map<String, List<PriceNode>> data) {
            this.data = data;
            setOpaque(false);

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
                if (entry.getKey().contains(p)) return entry.getValue();
            }
            return null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            tooltipMap.clear();
            if (data == null || data.isEmpty()) return;

            AffineTransform saveAT = g2d.getTransform();
            g2d.translate(translateX, translateY);
            g2d.scale(scale, scale);
            AffineTransform logicalAT = g2d.getTransform();

            int padding = 60;
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
            if (maxPrice == minPrice) {
                maxPrice += 10;
                minPrice = Math.max(0, minPrice - 10);
            } else {
                double diff = maxPrice - minPrice;
                maxPrice += diff * 0.15;
                minPrice = Math.max(0, minPrice - diff * 0.15);
            }

            int yTicks = 5;
            g2d.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            FontMetrics fm = g2d.getFontMetrics();

            for (int i = 0; i <= yTicks; i++) {
                double tickValue = minPrice + (maxPrice - minPrice) * i / yTicks;
                int yPos = padding + height - (int) (height * i / yTicks);

                g2d.setColor(new Color(75, 80, 90));
                Stroke oldStroke = g2d.getStroke();
                g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{2.0f}, 0.0f));
                g2d.drawLine(padding, yPos, padding + width, yPos);
                g2d.setStroke(oldStroke);

                g2d.setColor(TEXT_SECONDARY);
                String label = String.format("%.2f", tickValue);
                int labelWidth = fm.stringWidth(label);
                g2d.drawString(label, padding - labelWidth - 12, yPos + 4);
            }

            g2d.setColor(TEXT_PRIMARY);
            int roundCount = Math.max(1, allRounds.size());
            int xStep = width / roundCount;
            for (int i = 0; i < allRounds.size(); i++) {
                int x = padding + i * xStep + (xStep / 2);
                String rName = allRounds.get(i);
                int rWidth = fm.stringWidth(rName);
                g2d.drawString(rName, x - (rWidth/2), padding + height + 25);
            }

            int colorIdx = 0;
            g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            for (Map.Entry<String, List<PriceNode>> entry : data.entrySet()) {
                Color c = lineColors[colorIdx % lineColors.length];
                List<PriceNode> nodes = entry.getValue();
                String uniqueKey = entry.getKey();

                Integer prevX = null, prevY = null;
                for (PriceNode node : nodes) {
                    int roundIdx = allRounds.indexOf(node.roundName);
                    if (roundIdx == -1) continue;

                    int x = padding + roundIdx * xStep + (xStep / 2);
                    int y = padding + height - (int) ((node.price - minPrice) / (maxPrice - minPrice) * height);

                    g2d.setColor(c);
                    if (prevX != null && prevY != null) {
                        g2d.drawLine(prevX, prevY, x, y);
                    }

                    g2d.setColor(PANEL_BG);
                    g2d.fillOval(x - 6, y - 6, 12, 12);
                    g2d.setColor(c);
                    g2d.drawOval(x - 6, y - 6, 12, 12);
                    g2d.fillOval(x - 3, y - 3, 6, 6);

                    Point2D.Double src = new Point2D.Double(x, y);
                    Point2D.Double dst = new Point2D.Double();
                    logicalAT.transform(src, dst);
                    Rectangle pointRect = new Rectangle((int) dst.x - 10, (int) dst.y - 10, 20, 20);
                    tooltipMap.put(pointRect, uniqueKey + " (" + node.roundName + ": " + node.price + " 万)");

                    prevX = x;
                    prevY = y;
                }
                colorIdx++;
            }

            g2d.setTransform(saveAT);

            int legendX = getWidth() - 160;
            int legendY = 30;
            colorIdx = 0;
            for (Map.Entry<String, List<PriceNode>> entry : data.entrySet()) {
                Color c = lineColors[colorIdx % lineColors.length];

                g2d.setColor(c);
                g2d.fillRoundRect(legendX, legendY, 14, 14, 4, 4);

                g2d.setColor(TEXT_PRIMARY);
                g2d.setFont(new Font("微软雅黑", Font.PLAIN, 12));
                String fullKey = entry.getKey();
                String shortName = fullKey.length() > 8 ? fullKey.substring(0, 8) + ".." : fullKey;
                g2d.drawString(shortName, legendX + 22, legendY + 12);

                Rectangle legRect = new Rectangle(legendX, legendY, 140, 15);
                tooltipMap.put(legRect, fullKey);

                legendY += 24;
                colorIdx++;
            }

            g2d.setColor(new Color(100, 105, 115));
            g2d.setFont(new Font("微软雅黑", Font.PLAIN, 11));
            g2d.drawString("提示: 图表支持鼠标拖拽平移与滚轮缩放细节", 15, getHeight() - 15);
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        try {
            if (cell.getCellType() == CellType.NUMERIC) return String.valueOf(cell.getNumericCellValue());
            return cell.getStringCellValue().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private Double getNumericValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA)
                return cell.getNumericCellValue();
            return Double.parseDouble(cell.getStringCellValue().trim());
        } catch (Exception e) {
            return null;
        }
    }
}