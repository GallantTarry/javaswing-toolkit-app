package me.shaoxia;

import com.formdev.flatlaf.FlatDarkLaf;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 代理助手 1.5 完全集成版
 * 集成了：协同改名、数据校对、文件检索、评分生成、摸鱼游戏

 */
class MainLauncher extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainContainer;
    public static final Font MAIN_FONT = new Font("微软雅黑", Font.PLAIN, 16);
    public static final Font BOLD_FONT = new Font("微软雅黑", Font.BOLD, 18);

    public MainLauncher() {
        setTitle("代理助手 1.5 ");
        setSize(1200, 950);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 设置图标逻辑保留...
        try {
            String iconPath = "orange.png";
            File iconFile = new File(iconPath);
            if (!iconFile.exists()) iconFile = new File("src/" + iconPath);
            if (iconFile.exists()) {
                ImageIcon icon = new ImageIcon(iconFile.getAbsolutePath());
                setIconImage(icon.getImage());
            }
        } catch (Exception e) {
        }

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        mainContainer.add(createMainMenu(), "MENU");
        mainContainer.add(new RenamerPanel(this), "RENAMER");
        mainContainer.add(new CheckerPanel(this), "CHECKER");
        mainContainer.add(new FinderPanel(this), "FINDER");
        mainContainer.add(new ScoringPanel(this), "SCORING");
        mainContainer.add(new ReplacerPanel(this), "REPLACER");
        mainContainer.add(new ConverterPanel(this), "CONVERTER");


        add(mainContainer);
        cardLayout.show(mainContainer, "MENU");
    }

    private JPanel createMainMenu() {
        BackgroundPanel menuPanel = new BackgroundPanel("texture2.png");
        menuPanel.setLayout(new BorderLayout());

        // 1. 主容器：负责把所有内容往中间靠拢
        JPanel topArea = new JPanel(new GridBagLayout());
        topArea.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();

        // --- 标题部分 ---
        JLabel title = new JLabel("代理助手 1.5");
        title.setFont(new Font("微软雅黑", Font.BOLD, 52)); // 稍微加大了一点，更有排面
        title.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;   // 跨越2列
        gbc.insets = new Insets(100, 0, 60, 0); // 顶部留白100（决定了偏上位置），下方离按钮60
        gbc.weighty = 0;
        topArea.add(title, gbc);

        // --- 2x3 按钮矩阵 ---
        JButton btn1 = createModuleButton("协同改名助手", "RENAMER", new Color(41, 128, 185));
        JButton btn2 = createModuleButton("数据校对助手", "CHECKER", new Color(39, 174, 96));
        JButton btn3 = createModuleButton("文件检索助手", "FINDER", new Color(142, 68, 173));
        JButton btn4 = createModuleButton("评分生成助手", "SCORING", new Color(211, 84, 0));
        JButton btn5 = createModuleButton("一键替换助手", "REPLACER", new Color(22, 160, 133));
        JButton btn6 = createModuleButton("一键转换助手", "CONVERTER", new Color(192, 57, 43));

        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(10, 10, 10, 15); // 按钮之间的间距

        // 第一排 (Row 1)
        gbc.gridy = 1;
        gbc.gridx = 0; topArea.add(btn1, gbc);
        gbc.gridx = 1; topArea.add(btn2, gbc);

        // 第二排 (Row 2)
        gbc.gridy = 2;
        gbc.gridx = 0; topArea.add(btn3, gbc);
        gbc.gridx = 1; topArea.add(btn4, gbc);

        // 第三排 (Row 3) - 实现 3x2 分布
        gbc.gridy = 3;
        gbc.gridx = 0; topArea.add(btn5, gbc);
        gbc.gridx = 1; topArea.add(btn6, gbc);
        // --- 3. 关键：底部占位符 ---
        // 这个“弹簧”会占据下方所有剩余空间，把上面的内容顶到“中间偏上”
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        topArea.add(Box.createVerticalGlue(), gbc);

        // --- 游戏唤醒逻辑 (保持你的空格触发) ---
        DinoGamePanel gamePanel = new DinoGamePanel();
        gamePanel.setVisible(false);
        gamePanel.setPreferredSize(new Dimension(800, 260)); // 给游戏留好地儿

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE && !gamePanel.isVisible()) {
                    gamePanel.setVisible(true);
                    menuPanel.revalidate();
                    gamePanel.requestFocusInWindow();
                    return true;
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE && gamePanel.isVisible()) {
                    gamePanel.setVisible(false);
                    menuPanel.revalidate();
                    return true;
                }
            }
            return false;
        });

        menuPanel.add(topArea, BorderLayout.CENTER);
        menuPanel.add(gamePanel, BorderLayout.SOUTH);
        return menuPanel;
    }

    // 后续 createModuleButton, showMenu, main 方法保持不变...
    private JButton createModuleButton(String text, String cardName, Color themeColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isHover = getModel().isRollover();
                int alpha = isHover ? 200 : 150;
                g2.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), alpha));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
                g2.setStroke(new BasicStroke(1.8f));
                g2.setColor(new Color(255, 255, 255, isHover ? 120 : 70));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 25, 25);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.setColor(new Color(0, 0, 0, 60));
                g2.drawString(getText(), x + 1, y + 1);
                g2.setColor(Color.WHITE);
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(270, 110));
        btn.setFont(new Font("微软雅黑", Font.BOLD, 20));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setFont(new Font("微软雅黑", Font.BOLD, 22));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setFont(new Font("微软雅黑", Font.BOLD, 20));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                cardLayout.show(mainContainer, cardName);
            }
        });
        return btn;
    }

    public void showMenu() {
        cardLayout.show(mainContainer, "MENU");
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        UIManager.put("defaultFont", new Font("微软雅黑", Font.PLAIN, 14));
        SwingUtilities.invokeLater(() -> new MainLauncher().setVisible(true));
    }
}

/**
 * 改名模块
 */
class RenamerPanel extends BackgroundPanel {
    private JComboBox<String> biaoCombo, baoCombo, typeCombo, companyCombo;
    private JTextField newCompanyInput;
    private JLabel previewLabel;
    private DefaultComboBoxModel<String> companyModel;

    public RenamerPanel(MainLauncher parent) {
        super("texture2.png");
        setLayout(new GridLayout(1, 2));
        setOpaque(false);

        // 左侧控制区
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBackground(new Color(45, 48, 50, 200));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.weightx = 1.0;

        JButton backBtn = new JButton(" << 返回主菜单 ");
        backBtn.setFont(MainLauncher.BOLD_FONT);
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        leftPanel.add(backBtn, gbc);
        backBtn.addActionListener(e -> parent.showMenu());

        biaoCombo = new JComboBox<>(generateList("标", 20));
        addControlRow(leftPanel, gbc, 1, "1. 选择标段:", biaoCombo);

        baoCombo = new JComboBox<>(generateList("包", 20));
        addControlRow(leftPanel, gbc, 2, "2. 选择包号:", baoCombo);

        String[] types = {"价格文件", "商务文件", "技术文件", "二轮报价文件", "三轮报价文件", "四轮报价文件", "五轮报价文件", "六轮报价文件", "首轮分项报价文件" ,"二轮分项报价文件", "三轮分项报价文件", "四轮分项报价文件", "五轮分项报价文件", "六轮分项报价文件"};
        typeCombo = new JComboBox<>(types);
        addControlRow(leftPanel, gbc, 3, "3. 文件类型:", typeCombo);

        companyModel = new DefaultComboBoxModel<>(new String[]{"请先添加供应商"});
        companyCombo = new JComboBox<>(companyModel);
        addControlRow(leftPanel, gbc, 4, "4. 选择公司:", companyCombo);

        newCompanyInput = new JTextField();
        addControlRow(leftPanel, gbc, 5, "5. 管理供应商:", newCompanyInput);

        JPanel btnP = new JPanel(new GridLayout(1, 2, 8, 0));
        btnP.setOpaque(false);
        JButton save = new JButton("保存公司"), reset = new JButton("重置名单");
        btnP.add(save);
        btnP.add(reset);
        gbc.gridy = 6;
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        leftPanel.add(btnP, gbc);

        // 事件处理
        save.addActionListener(e -> {
            String n = newCompanyInput.getText().trim();
            if (!n.isEmpty()) {
                if (companyModel.getSize() > 0 && companyModel.getElementAt(0).contains("请先"))
                    companyModel.removeElementAt(0);
                companyModel.addElement(n);
                companyCombo.setSelectedItem(n);
                newCompanyInput.setText("");
                updatePreview();
            }
        });
        reset.addActionListener(e -> {
            companyModel.removeAllElements();
            companyModel.addElement("请先添加供应商");
            updatePreview();
        });

        // 右侧预览与拖入
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setOpaque(false);
        GridBagConstraints rGbc = new GridBagConstraints();
        rGbc.insets = new Insets(20, 20, 20, 20);
        rGbc.fill = GridBagConstraints.BOTH;

        previewLabel = new JLabel("预览：等待输入...", JLabel.CENTER);
        previewLabel.setFont(MainLauncher.BOLD_FONT);
        previewLabel.setForeground(Color.WHITE);
        rGbc.gridy = 0;
        rGbc.weighty = 0.2;
        rGbc.weightx = 1.0;
        rightPanel.add(previewLabel, rGbc);

        JPanel dropArea = new JPanel(new BorderLayout());
        dropArea.setOpaque(false);
        dropArea.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.WHITE, 2, true), " 拖入文件重命名 ", TitledBorder.CENTER, TitledBorder.TOP, MainLauncher.BOLD_FONT, Color.WHITE));
        JLabel dl = new JLabel("<html><center>准备就绪<br>拖入文件自动处理</center></html>", JLabel.CENTER);
        dl.setForeground(Color.WHITE);
        dropArea.add(dl);
        setDropTarget(dropArea);
        rGbc.gridy = 1;
        rGbc.weighty = 0.8;
        rightPanel.add(dropArea, rGbc);

        ActionListener listener = e -> updatePreview();
        biaoCombo.addActionListener(listener);
        baoCombo.addActionListener(listener);
        typeCombo.addActionListener(listener);
        companyCombo.addActionListener(listener);

        add(leftPanel);
        add(rightPanel);
    }

    private void addControlRow(JPanel p, GridBagConstraints g, int r, String t, Component c) {
        g.gridy = r;
        g.gridx = 0;
        g.weightx = 0.3;
        g.gridwidth = 1;
        JLabel l = new JLabel(t);
        l.setForeground(Color.WHITE);
        p.add(l, g);
        g.gridx = 1;
        g.weightx = 0.7;
        p.add(c, g);
    }

    private void updatePreview() {
        Object selected = companyCombo.getSelectedItem();
        String c = (selected != null) ? selected.toString() : "未选公司";
        if (c.contains("请先")) c = "未选公司";
        previewLabel.setText("预览：" + biaoCombo.getSelectedItem() + baoCombo.getSelectedItem() + "_" + c + "_" + typeCombo.getSelectedItem());
    }

    private void setDropTarget(JPanel p) {
        new DropTarget(p, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            public void drop(DropTargetDropEvent dtde) {
                try {
                    String comp = companyCombo.getSelectedItem().toString();
                    if (comp.contains("请先")) {
                        dtde.rejectDrop();
                        JOptionPane.showMessageDialog(null, "请先输入并保存供应商名称！", "操作拦截", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    for (File file : files) {
                        String ext = file.getName().contains(".") ? file.getName().substring(file.getName().lastIndexOf(".")) : "";
                        String baseName = biaoCombo.getSelectedItem() + (String) baoCombo.getSelectedItem() + "_" + comp + "_" + typeCombo.getSelectedItem();
                        File newFile = new File(file.getParent(), baseName + ext);
                        if (newFile.exists()) {
                            int choice = JOptionPane.showConfirmDialog(null, "文件夹内已存在文件：\n" + newFile.getName() + "\n是否自动更名保存？", "检测到重名", JOptionPane.YES_NO_OPTION);
                            if (choice == JOptionPane.YES_OPTION) {
                                int count = 1;
                                while (newFile.exists()) {
                                    newFile = new File(file.getParent(), baseName + "_复件" + count + ext);
                                    count++;
                                }
                            } else continue;
                        }
                        file.renameTo(newFile);
                    }
                    JOptionPane.showMessageDialog(null, "处理成功！");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private Vector<String> generateList(String p, int c) {
        Vector<String> v = new Vector<>();
        for (int i = 1; i <= c; i++) v.add(p + i);
        return v;
    }
}

/**
 * 校验模块
 */
class CheckerPanel extends BackgroundPanel {
    public CheckerPanel(MainLauncher parent) {
        super("texture2.png");
        setLayout(new BorderLayout());
        setOpaque(false);

        JButton backBtn = new JButton(" << 返回主菜单 ");
        backBtn.addActionListener(e -> parent.showMenu());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        top.add(backBtn);
        add(top, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        JLabel infoLabel = new JLabel("<html><center><h2>信息协同数据校对助手</h2><p>将 <b>.xlsx</b> 招标数据文件拖入下方方框进行递减和协同串标逻辑校验</p></center></html>");
        infoLabel.setForeground(Color.WHITE);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 30, 0);
        centerPanel.add(infoLabel, gbc);

        JPanel dropArea = new JPanel(new BorderLayout());
        dropArea.setPreferredSize(new Dimension(650, 350));
        dropArea.setOpaque(false);
        dropArea.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.WHITE, 2, true), " 拖入 XLSX 进行逻辑校验 ", TitledBorder.CENTER, TitledBorder.TOP, MainLauncher.BOLD_FONT, Color.WHITE));
        JLabel dl = new JLabel("请拖入 Excel 文件", JLabel.CENTER);
        dl.setForeground(Color.WHITE);
        dl.setFont(MainLauncher.BOLD_FONT);
        dropArea.add(dl);
        setCheckDropTarget(dropArea);
        gbc.gridy = 1;
        centerPanel.add(dropArea, gbc);
        add(centerPanel, BorderLayout.CENTER);
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

    private void processExcel(File file) {
        List<String> errors = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            int supIdx = -1;
            String[] rounds = {"首轮报价", "二轮报价", "三轮报价", "四轮报价", "五轮报价", "六轮报价", "终轮报价"};
            int[] colIdxMap = new int[rounds.length];
            Arrays.fill(colIdxMap, -1);
            boolean foundHeader = false;
            Map<Integer, Map<String, List<String>>> percentDuplicateMap = new HashMap<>();

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
                for (int i = 0; i < colIdxMap.length; i++) {
                    int cIdx = colIdxMap[i];
                    if (cIdx == -1) continue;
                    Double currPrice = getNumericValue(row.getCell(cIdx));
                    if (currPrice != null) {
                        if (prevPrice != null) {
                            if (currPrice > prevPrice) {
                                errors.add("【涨价异常】" + company + "\n    " + rounds[i] + "(" + currPrice + ") 高于前一轮(" + prevPrice + ")");
                            }
                            if (prevPrice > 0) {
                                double dropPercent = (prevPrice - currPrice) / prevPrice * 100.0;
                                String percentStr = String.format("%.4f", dropPercent);
                                percentDuplicateMap.putIfAbsent(i, new HashMap<>());
                                Map<String, List<String>> pMap = percentDuplicateMap.get(i);
                                pMap.putIfAbsent(percentStr, new ArrayList<>());
                                pMap.get(percentStr).add(company);
                            }
                        }
                        prevPrice = currPrice;
                    }
                }
            }
            for (Map.Entry<Integer, Map<String, List<String>>> roundEntry : percentDuplicateMap.entrySet()) {
                String roundName = rounds[roundEntry.getKey()];
                for (Map.Entry<String, List<String>> pEntry : roundEntry.getValue().entrySet()) {
                    List<String> companies = pEntry.getValue();
                    if (companies.size() > 1 && Double.parseDouble(pEntry.getKey()) > 0.0001) {
                        errors.add("【疑似协同投标】" + roundName + " 发现一致降幅: " + pEntry.getKey() + "%\n    涉及供应商: " + String.join(", ", companies));
                    }
                }
            }
            if (errors.isEmpty()) JOptionPane.showMessageDialog(null, "校对完成，数据逻辑正常。");
            else showErrorDialog(errors);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "解析失败，请检查文件。");
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

    private void showErrorDialog(List<String> errors) {
        JTextArea area = new JTextArea(String.join("\n\n", errors));
        area.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        area.setEditable(false);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(600, 450));
        JOptionPane.showMessageDialog(null, scroll, "报价数据风险预警", JOptionPane.WARNING_MESSAGE);
    }
}

/**
 * 检索模块
 */
class FinderPanel extends JPanel {
    private JTextField searchField, pathDisplayField;
    private JTextArea rulesTextArea;
    private JTable fileTable;
    private DefaultTableModel tableModel;
    private List<File> allFiles = new ArrayList<>();
    private JLabel statusLabel;

    public FinderPanel(MainLauncher parent) {
        setLayout(new BorderLayout());
        setBackground(new Color(45, 48, 50));
        JPanel topBar = new JPanel(new BorderLayout(10, 0));
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton backBtn = new JButton(" << 返回 ");
        backBtn.addActionListener(e -> parent.showMenu());

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnGroup.setOpaque(false);
        JButton loadBtn = new JButton("1. 选择文件夹扫描");
        loadBtn.addActionListener(e -> selectAndLoad());
        JButton extractBtn = new JButton("2. 一键提取核心文件");
        extractBtn.addActionListener(e -> quickExtract());
        JButton showAllBtn = new JButton("3. 返回全览");
        showAllBtn.addActionListener(e -> {
            searchField.setText("");
            filterFiles();
        });
        btnGroup.add(loadBtn);
        btnGroup.add(extractBtn);
        btnGroup.add(showAllBtn);

        JPanel searchBar = new JPanel(new BorderLayout(5, 0));
        searchBar.setOpaque(false);
        searchField = new JTextField(15);
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                filterFiles();
            }
        });
        searchBar.add(new JLabel("过滤: "), BorderLayout.WEST);
        searchBar.add(searchField, BorderLayout.CENTER);

        topBar.add(backBtn, BorderLayout.WEST);
        topBar.add(btnGroup, BorderLayout.CENTER);
        topBar.add(searchBar, BorderLayout.EAST);

        pathDisplayField = new JTextField("尚未选择文件夹");
        pathDisplayField.setEditable(false);
        pathDisplayField.setBackground(new Color(60, 63, 65));
        pathDisplayField.setForeground(Color.YELLOW);

        JPanel northStack = new JPanel(new BorderLayout());
        northStack.setOpaque(false);
        northStack.add(topBar, BorderLayout.NORTH);
        northStack.add(pathDisplayField, BorderLayout.SOUTH);

        rulesTextArea = new JTextArea("评审报告\n专家签到表\n应答文件上传情况统计");
        JScrollPane rulesScroll = new JScrollPane(rulesTextArea);
        rulesScroll.setPreferredSize(new Dimension(200, 0));
        rulesScroll.setBorder(BorderFactory.createTitledBorder("提取规则"));

        tableModel = new DefaultTableModel(new String[]{"文件名", "所在路径", "大小", "修改日期"}, 0);
        fileTable = new JTable(tableModel);
        fileTable.setDragEnabled(true);
        fileTable.setTransferHandler(new TransferHandler() {
            @Override
            protected java.awt.datatransfer.Transferable createTransferable(JComponent c) {
                int[] rows = fileTable.getSelectedRows();
                if (rows.length == 0) return null;
                List<File> files = new ArrayList<>();
                for (int r : rows)
                    files.add(new File((String) tableModel.getValueAt(r, 1), (String) tableModel.getValueAt(r, 0)));
                return new java.awt.datatransfer.Transferable() {
                    public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
                        return new java.awt.datatransfer.DataFlavor[]{java.awt.datatransfer.DataFlavor.javaFileListFlavor};
                    }

                    public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor f) {
                        return java.awt.datatransfer.DataFlavor.javaFileListFlavor.equals(f);
                    }

                    public Object getTransferData(java.awt.datatransfer.DataFlavor f) {
                        return files;
                    }
                };
            }

            @Override
            public int getSourceActions(JComponent c) {
                return COPY;
            }
        });

        add(northStack, BorderLayout.NORTH);
        add(rulesScroll, BorderLayout.WEST);
        add(new JScrollPane(fileTable), BorderLayout.CENTER);
        statusLabel = new JLabel("就绪");
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void selectAndLoad() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File sel = chooser.getSelectedFile();
            pathDisplayField.setText(sel.getAbsolutePath());
            scanDirectory(sel);
        }
    }

    private void scanDirectory(File dir) {
        allFiles.clear();
        new Thread(() -> {
            recursiveWalk(dir);
            SwingUtilities.invokeLater(() -> {
                filterFiles();
                statusLabel.setText("扫描完成:" + allFiles.size());
            });
        }).start();
    }

    private void recursiveWalk(File f) {
        File[] list = f.listFiles();
        if (list != null) for (File child : list)
            if (child.isDirectory()) recursiveWalk(child);
            else allFiles.add(child);
    }

    private void filterFiles() {
        String q = searchField.getText().toLowerCase();
        tableModel.setRowCount(0);
        for (File f : allFiles)
            if (f.getName().toLowerCase().contains(q))
                tableModel.addRow(new Object[]{f.getName(), f.getParent(), f.length() / 1024 + "KB", ""});
    }

    private void quickExtract() {
        String[] rules = rulesTextArea.getText().split("\n");
        tableModel.setRowCount(0);
        for (File f : allFiles) {
            String name = f.getName().toLowerCase();
            for (String r : rules) {
                if (!r.trim().isEmpty() && name.contains(r.trim().toLowerCase())) {
                    tableModel.addRow(new Object[]{f.getName(), f.getParent(), f.length() / 1024 + "KB", ""});
                    break;
                }
            }
        }
    }
}

/**
 * 评分生成模块 - 精准修复重复行逻辑
 */
class ScoringPanel extends JPanel {
    private JTabbedPane tabbedPane;
    private JButton exportBtn;
    private List<TableDataHolder> extractedTables = new ArrayList<>();

    public ScoringPanel(MainLauncher parent) {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(45, 48, 50));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.setOpaque(false);
        JButton backBtn = new JButton(" << 返回主菜单 ");
        backBtn.addActionListener(e -> parent.showMenu());
        topBar.add(backBtn);

        JLabel dropLabel = new JLabel("<html><center>【 拖入采购文件.docx 】<br>生成技术/商务评分表.xlsx<br></center></html>", JLabel.CENTER);
        dropLabel.setFont(new Font("微软雅黑", Font.BOLD, 22));
        dropLabel.setPreferredSize(new Dimension(0, 180));
        dropLabel.setOpaque(true);
        dropLabel.setBackground(new Color(30, 30, 30));
        dropLabel.setForeground(new Color(212, 175, 55));
        dropLabel.setBorder(BorderFactory.createDashedBorder(Color.GRAY, 3, 5, 2, true));

        tabbedPane = new JTabbedPane();
        exportBtn = new JButton(" 一键导出 Excel");
        exportBtn.setFont(new Font("微软雅黑", Font.BOLD, 20));
        exportBtn.setPreferredSize(new Dimension(0, 60));
        exportBtn.setEnabled(false);
        exportBtn.addActionListener(e -> exportToExcel());

        new DropTarget(dropLabel, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
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

        add(topBar, BorderLayout.NORTH);
        add(dropLabel, BorderLayout.CENTER);
        JPanel content = new JPanel(new BorderLayout());
        content.add(tabbedPane, BorderLayout.CENTER);
        content.add(exportBtn, BorderLayout.SOUTH);
        add(content, BorderLayout.SOUTH);
    }

    private void parseDocx(File file) {
        tabbedPane.removeAll();
        extractedTables.clear();
        try (FileInputStream fis = new FileInputStream(file); XWPFDocument doc = new XWPFDocument(fis)) {
            List<IBodyElement> elements = doc.getBodyElements();
            for (int i = 0; i < elements.size(); i++) {
                IBodyElement el = elements.get(i);
                if (el instanceof XWPFParagraph) {
                    String text = ((XWPFParagraph) el).getText().trim();
                    boolean isTarget = (text.contains("技术评分标准") || text.contains("商务评分标准") || text.contains("技术评审细则") || text.contains("商务评审细则"));
                    if (isTarget && text.contains("%")) {
                        for (int k = i + 1; k < Math.min(i + 2, elements.size()); k++) {
                            if (elements.get(k) instanceof XWPFTable) {
                                TableDataHolder holder = processTable((XWPFTable) elements.get(k), text);
                                if (holder != null) {
                                    extractedTables.add(holder);
                                    displayTable(holder);
                                }
                                break;
                            }
                        }
                    }
                }
            }
            exportBtn.setEnabled(!extractedTables.isEmpty());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private TableDataHolder processTable(XWPFTable table, String title) {
        List<XWPFTableRow> rows = table.getRows();
        if (rows.size() <= 1) return null;

        int originalColCount = rows.get(0).getTableCells().size();
        List<Integer> validColIndices = new ArrayList<>();
        XWPFTableRow headerRow = rows.get(0);
        for (int c = 0; c < originalColCount; c++) {
            String headerText = headerRow.getCell(c).getText().trim();
            if (!headerText.contains("分") && !headerText.contains("值") && !headerText.contains("得"))
                validColIndices.add(c);
        }
        if (validColIndices.isEmpty()) return null;

        List<String[]> dataRows = new ArrayList<>();
        Set<String> rowFingerprints = new HashSet<>(); // 核心修复：用于检测内容重复
        String[] prevRowData = new String[originalColCount];

        for (int r = 1; r < rows.size(); r++) {
            XWPFTableRow row = rows.get(r);
            String[] rowData = new String[validColIndices.size()];
            StringBuilder rowContent = new StringBuilder();

            for (int i = 0; i < validColIndices.size(); i++) {
                int realIdx = validColIndices.get(i);
                String cellText = (realIdx < row.getTableCells().size())
                        ? row.getCell(realIdx).getText().trim().replaceAll("[\\n\\r\\t]+", " ")
                        : "";

                if (cellText.isEmpty() && r > 1) cellText = prevRowData[realIdx];

                rowData[i] = cellText;
                prevRowData[realIdx] = cellText;
                rowContent.append(cellText).append("|"); // 构建行指纹
            }

            // 核心修复：如果这一行内容（rowContent）在当前表格中已存在，则跳过，不加入 dataRows
            String fingerprint = rowContent.toString();
            if (!fingerprint.replace("|", "").isEmpty() && !rowFingerprints.contains(fingerprint)) {
                dataRows.add(rowData);
                rowFingerprints.add(fingerprint);
            }
        }
        return dataRows.isEmpty() ? null : new TableDataHolder(title, dataRows);
    }

    private void displayTable(TableDataHolder holder) {
        tabbedPane.addTab(holder.title, new JScrollPane(new JTable(new DefaultTableModel(holder.data.toArray(new Object[0][0]), new String[holder.data.get(0).length]))));
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
}

/**
 * 摸鱼小游戏模块
 *///////


class DinoGamePanel extends JPanel implements ActionListener {


    // --- 新增多线程相关变量 ---
    private volatile boolean running = true;
    private Thread logicThread;
    private final Object logicLock = new Object();

    // 在构造函数 DinoGamePanel() 的末尾增加以下启动逻辑
    private void startLogicThread() {
        logicThread = new Thread(() -> {
            while (running) {
                long startTime = System.currentTimeMillis();

                // 仅在游戏运行时进行后台逻辑计算
                if (isStarted && !isOver && !isBossMode) {
                    synchronized (logicLock) {
                        // 将原本在 actionPerformed 里的逻辑抽离到这里，实现并行计算
                        runBackgroundLogic();
                    }
                }

                // 控制逻辑帧率为 60FPS 左右，减轻 CPU 负担
                long sleepTime = 16 - (System.currentTimeMillis() - startTime);
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }, "GameLogicThread");
        logicThread.setPriority(Thread.MAX_PRIORITY); // 给予逻辑线程高优先级
        logicThread.start();
    }

    // 新增：后台逻辑处理方法
    private void runBackgroundLogic() {
        // 1. 速度与分数增量计算（原 actionPerformed 逻辑）
        if (gameSpeed < MAX_SPEED) gameSpeed += ACCELERATION;
        score += (gameSpeed / 42.0);

        // 2. 物理与 AI 处理
        updatePhysics();
        handleAIAndEnemies();
        handleCollisions();

        // 3. 屏幕震动衰减
        if (screenShake > 0) screenShake--;
    }

    // 新增：资源释放逻辑（建议在窗口关闭时调用）
    public void stopGame() {
        running = false;
        if (logicThread != null) logicThread.interrupt();
    }


    private static final int HEIGHT = 260;
    private static final int GROUND_Y = 210;
    private Timer timer;
    private Random rand = new Random();
    private boolean isStarted = false;
    private boolean isOver = false;
    private boolean isBossMode = false; // 新增：老板键模式标志

    // --- 速度变量 ---
    private double gameSpeed = 7.0;
    private final double ACCELERATION = 0.0018;
    private final double MAX_SPEED = 26.0;
    private double score = 0;
    private int screenShake = 0;

    // --- 黄金布局变量 ---
    private static final int DINO_X = 220;
    private static final int ORC_X_OFFSET = 260;
    private static final int THIEF_X_OFFSET = -180;

    // --- 物理逻辑 ---
    private double dinoY = GROUND_Y - 44;
    private double dinoVelY = 0;
    private boolean isBlockingRight = false;
    private boolean isBlockingLeft = false;

    private double orcY = GROUND_Y - 44;
    private double orcVelY = 0;

    private double thiefY = GROUND_Y - 40;
    private double thiefVelY = 0;

    // --- 集合 ---
    private List<Obstacle> obstacles = new ArrayList<>();
    private List<DustParticle> dust = new ArrayList<>();
    private List<Point> clouds = new ArrayList<>();
    private List<Bomb> bombs = new ArrayList<>();
    private List<Dagger> daggers = new ArrayList<>();

    private int bombCooldown = 0;
    private int daggerCooldown = 0;

    public DinoGamePanel() {
        setPreferredSize(new Dimension(800, HEIGHT));
        setDoubleBuffered(true);
        setOpaque(false);
        setFocusable(true);
        for (int i = 0; i < 6; i++) clouds.add(new Point(rand.nextInt(1500), 30 + rand.nextInt(50)));

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();

                // --- 核心改动：老板键逻辑 ---
                if (code == KeyEvent.VK_ESCAPE) {
                    isBossMode = true;
                    isStarted = false;
                    repaint();
                    return;
                }

                if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_UP) {
                    if (isBossMode) {
                        isBossMode = false; // 按空格退出老板模式
                        resetGame();
                    } else if (!isStarted || isOver) {
                        resetGame();
                    } else if (dinoY >= GROUND_Y - 45) {
                        dinoVelY = -15.5;
                        createDust(DINO_X + 10);
                    }
                }

                if (!isBossMode) { // 仅在非老板模式下响应防御
                    if (code == KeyEvent.VK_RIGHT) {
                        isBlockingRight = true;
                        isBlockingLeft = false;
                    }
                    if (code == KeyEvent.VK_LEFT) {
                        isBlockingLeft = true;
                        isBlockingRight = false;
                    }
                }
            }

            public void keyReleased(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_RIGHT) isBlockingRight = false;
                if (code == KeyEvent.VK_LEFT) isBlockingLeft = false;
            }
        });
        timer = new Timer(16, this);
        timer.start();
    }

    private void resetGame() {
        score = 0;
        gameSpeed = 7.0;
        isStarted = true;
        isOver = false;
        isBossMode = false;
        dinoY = GROUND_Y - 44;
        dinoVelY = 0;
        orcY = GROUND_Y - 44;
        orcVelY = 0;
        thiefY = GROUND_Y - 40;
        thiefVelY = 0;
        obstacles.clear();
        dust.clear();
        bombs.clear();
        daggers.clear();
        bombCooldown = 120;
        daggerCooldown = 60;
        requestFocusInWindow();
    }

    private void createDust(int x) {
        for (int i = 0; i < 6; i++) dust.add(new DustParticle(x, GROUND_Y));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isBossMode || !isStarted || isOver) {
            repaint();
            return;
        }

        if (gameSpeed < MAX_SPEED) gameSpeed += ACCELERATION;
        score += (gameSpeed / 42.0);

        updatePhysics();
        handleAIAndEnemies();
        handleCollisions();

        if (screenShake > 0) screenShake--;
        repaint();
    }

    private void updatePhysics() {
        dinoY += dinoVelY;
        if (dinoY < GROUND_Y - 44) dinoVelY += 0.88;
        else {
            if (dinoVelY > 5) {
                screenShake = 6;
                createDust(DINO_X + 10);
            }
            dinoY = GROUND_Y - 44;
            dinoVelY = 0;
        }

        orcY += orcVelY;
        if (orcY < GROUND_Y - 44) orcVelY += 0.88;
        else {
            orcY = GROUND_Y - 44;
            orcVelY = 0;
        }

        thiefY += thiefVelY;
        if (thiefY < GROUND_Y - 40) thiefVelY += 0.88;
        else {
            thiefY = GROUND_Y - 40;
            thiefVelY = 0;
        }
    }

    private void handleAIAndEnemies() {
        double orcRealX = DINO_X + ORC_X_OFFSET;
        if (bombCooldown > 0) bombCooldown--;
        if (bombCooldown <= 0 && rand.nextInt(100) < 2) {
            bombs.add(new Bomb((int) orcRealX, (int) orcY));
            bombCooldown = 90 + rand.nextInt(100);
        }
        if (score > 100) {
            if (daggerCooldown > 0) daggerCooldown--;
            if (daggerCooldown <= 0 && rand.nextInt(100) < 3) {
                daggers.add(new Dagger(-20, (int) thiefY + 12));
                daggerCooldown = 60 + rand.nextInt(70);
            }
        }
        for (Obstacle ob : obstacles) {
            if (ob.x > orcRealX && ob.x < orcRealX + 140 && orcY >= GROUND_Y - 45) {
                orcVelY = -15;
                break;
            }
        }
    }

    private void handleCollisions() {
        Rectangle dinoHitbox = new Rectangle(DINO_X + 5, (int) dinoY, 34, 44);
        Iterator<Obstacle> obIter = obstacles.iterator();
        while (obIter.hasNext()) {
            Obstacle ob = obIter.next();
            ob.x -= gameSpeed;
            if (ob.getBounds().intersects(dinoHitbox)) {
                isOver = true;
                screenShake = 15;
            }
            if (ob.x < -100) obIter.remove();
        }
        Iterator<Bomb> bIter = bombs.iterator();
        while (bIter.hasNext()) {
            Bomb b = bIter.next();
            if (b.update(gameSpeed)) {
                bIter.remove();
                continue;
            }
            if (b.getBounds().intersects(dinoHitbox)) {
                if (isBlockingRight) {
                    bIter.remove();
                    score += 10;
                    screenShake = 3;
                } else {
                    isOver = true;
                    screenShake = 20;
                }
            }
        }
        Iterator<Dagger> dIter = daggers.iterator();
        while (dIter.hasNext()) {
            Dagger d = dIter.next();
            if (d.update(gameSpeed)) {
                dIter.remove();
                continue;
            }
            if (d.getBounds().intersects(dinoHitbox)) {
                if (isBlockingLeft) {
                    dIter.remove();
                    score += 15;
                    screenShake = 2;
                } else {
                    isOver = true;
                    screenShake = 20;
                }
            }
        }
        for (Point p : clouds) {
            p.x -= (gameSpeed * 0.15);
            if (p.x < -150) {
                p.x = getWidth() + rand.nextInt(300);
                p.y = 30 + rand.nextInt(50);
            }
        }
        if (rand.nextInt(100) < 5 && (obstacles.isEmpty() || obstacles.get(obstacles.size() - 1).x < getWidth() - 350)) {
            obstacles.add(new Obstacle(getWidth(), GROUND_Y - 60));
        }
        for (int i = 0; i < dust.size(); i++) {
            if (dust.get(i).update()) dust.remove(i--);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (isBossMode) return; // 老板来了，直接涂空不画

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (screenShake > 0)
            g2.translate(rand.nextInt(screenShake) - screenShake / 2, rand.nextInt(screenShake) - screenShake / 2);

        drawFancyClouds(g2);
        g2.setColor(new Color(120, 120, 120, 80));
        g2.drawLine(0, GROUND_Y, getWidth(), GROUND_Y);

        for (DustParticle p : dust) p.draw(g2);
        for (Obstacle ob : obstacles) ob.render(g2);
        for (Bomb b : bombs) b.render(g2);
        for (Dagger d : daggers) d.render(g2);

        drawDino(g2, DINO_X, (int) dinoY);
        drawOrcWithPig(g2, DINO_X + ORC_X_OFFSET, (int) orcY);
        if (score > 100) drawThief(g2, DINO_X + THIEF_X_OFFSET, (int) thiefY);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 18));
        g2.drawString(String.format("SCORE: %05d", (int) score), getWidth() - 180, 40);

        if (isOver) {
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRect(0, 0, getWidth(), HEIGHT);
            g2.setColor(Color.WHITE);
            g2.drawString("MISSION FAILED - PRESS SPACE", getWidth() / 2 - 140, HEIGHT / 2);
        }
        Toolkit.getDefaultToolkit().sync();
    }

    private void drawDino(Graphics2D g2, int x, int y) {
        int p = 3;
        Color armor = new Color(210, 210, 220);
        Color plume = new Color(220, 40, 40);
        Color shield = new Color(190, 150, 60);
        g2.setColor(plume);
        g2.fillRect(x + 2 * p, y - 6 * p, 5 * p, 2 * p);
        g2.setColor(armor);
        g2.fillRect(x + 4 * p, y - 4 * p, 6 * p, 6 * p);
        g2.fillRect(x + 3 * p, y + 2 * p, 7 * p, 8 * p);
        if (isBlockingRight) {
            g2.setColor(shield);
            g2.fillRoundRect(x + 10 * p, y - 2, 6 * p, 12 * p, 5, 5);
        } else if (isBlockingLeft) {
            g2.setColor(shield);
            g2.fillRoundRect(x - 5 * p, y - 2, 6 * p, 12 * p, 5, 5);
        } else {
            g2.setColor(shield);
            g2.fillRect(x + 8 * p, y + 3 * p, 4 * p, 6 * p);
        }
        g2.setColor(new Color(80, 85, 100));
        long anim = (System.currentTimeMillis() / 120) % 2;
        if (anim == 0) {
            g2.fillRect(x + 4 * p, y + 10 * p, 2 * p, 3 * p);
            g2.fillRect(x + 7 * p, y + 10 * p, 2 * p, 1 * p);
        } else {
            g2.fillRect(x + 4 * p, y + 10 * p, 2 * p, 1 * p);
            g2.fillRect(x + 7 * p, y + 10 * p, 2 * p, 3 * p);
        }
    }

    /// /////////////////////////////////////小偷背猪
    private void drawOrcWithPig(Graphics2D g2, int x, int y) {
        Graphics2D g = (Graphics2D) g2.create();

        // 纯正像素风
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        int p = 5;
        long t = System.currentTimeMillis() / 200;
        int walkFrame = (int) (t % 2);
        int bounce = (walkFrame == 0) ? p : 0;

        // --- 核心改动：整体向上移动偏移量 ---
        int offsetY = -3 * p; // 向上移动 3 个像素单位

        // --- 调色盘 ---
        Color thiefSkin = new Color(240, 190, 150);
        Color thiefBlack = new Color(30, 30, 35);
        Color pigPink = new Color(255, 170, 190);
        Color pigDark = new Color(220, 130, 150);
        Color crownGold = new Color(255, 200, 0);
        Color ropeColor = new Color(101, 67, 33);

        // 1. 绘制小偷的腿 (应用 offsetY)
        g.setColor(thiefBlack);
        if (walkFrame == 0) {
            drawRect(g, x + 3 * p, y + 10 * p + offsetY, 2 * p, 3 * p, p);
            drawRect(g, x + 7 * p, y + 11 * p + offsetY, 3 * p, 2 * p, p);
        } else {
            drawRect(g, x + 3 * p, y + 11 * p + offsetY, 3 * p, 2 * p, p);
            drawRect(g, x + 8 * p, y + 10 * p + offsetY, 2 * p, 3 * p, p);
        }

        // 2. 绘制巨型皇家猪 (应用 offsetY)
        int px = x - 5 * p;
        int py = y - 7 * p + bounce + offsetY;

        // --- 猪身轮廓 ---
        g.setColor(pigDark);
        drawRect(g, px, py + 2 * p, 16 * p, 10 * p, p);
        g.setColor(pigPink);
        drawRect(g, px + p, py + p, 14 * p, 10 * p, p);

        // --- 猪鼻子 ---
        g.setColor(new Color(255, 110, 140));
        drawRect(g, px - 2 * p, py + 5 * p, 4 * p, 4 * p, p);
        g.setColor(new Color(60, 20, 30));
        drawRect(g, px - p, py + 6 * p, p, 2 * p, p);

        // --- 猪眼睛 ---
        g.setColor(Color.WHITE);
        drawRect(g, px + 3 * p, py + 3 * p, 2 * p, 2 * p, p);
        drawRect(g, px + 8 * p, py + 3 * p, 2 * p, 2 * p, p);
        g.setColor(Color.BLACK);
        drawRect(g, px + 4 * p, py + 3 * p, p, p, p);

        // --- 皇家皇冠 ---
        g.setColor(crownGold);
        drawRect(g, px + 6 * p, py - 2 * p, 5 * p, p, p);
        drawRect(g, px + 6 * p, py - 3 * p, p, p, p);
        drawRect(g, px + 8 * p, py - 4 * p, p, 2 * p, p);
        drawRect(g, px + 10 * p, py - 3 * p, p, p, p);

        // 3. 绘制猥琐小偷上半身 (应用 offsetY)
        g.setColor(thiefBlack);
        drawRect(g, x + 2 * p, y + 5 * p + offsetY, 10 * p, 5 * p, p);

        int hx = x + 10 * p;
        int hy = y + 5 * p + offsetY;
        drawRect(g, hx, hy, 5 * p, 4 * p, p);

        g.setColor(thiefSkin);
        drawRect(g, hx + 2 * p, hy + p, 3 * p, 2 * p, p);

        g.setColor(Color.BLACK);
        int eyeShift = (int) (System.currentTimeMillis() / 400 % 2);
        drawRect(g, hx + 2 * p + eyeShift, hy + p, 1, 1, p);
        drawRect(g, hx + 4 * p, hy + p, 1, 1, p);

        // 4. 绳子与手 (应用 offsetY)
        g.setColor(ropeColor);
        drawRect(g, px + 5 * p, py, p, 12 * p, p);
        drawRect(g, px + 11 * p, py, p, 11 * p, p);

        g.setColor(thiefSkin);
        drawRect(g, x + p, y + 7 * p + bounce + offsetY, 2 * p, 2 * p, p);

        g.dispose();
    }

    /**
     * 像素绘制辅助
     */
    private void drawRect(Graphics2D g, int x, int y, int w, int h, int p) {
        g.fillRect(x, y, w, h);
    }

    /// ////////////////////////////
    private void drawThief(Graphics2D g2, int x, int y) {
        int p = 3;
        g2.setColor(new Color(45, 45, 65));
        g2.fillRect(x + p, y + 2 * p, 5 * p, 8 * p);
        g2.setColor(Color.BLACK);
        g2.fillRect(x + 2 * p, y - p, 3 * p, 3 * p);
        g2.setColor(new Color(200, 60, 60));
        g2.fillRect(x + 3 * p, y + 2 * p, 3 * p, p);
        long t = System.currentTimeMillis() / 150;
        g2.setColor(new Color(30, 30, 40));
        if (t % 2 == 0) {
            g2.fillRect(x + 2 * p, y + 10 * p, 2 * p, 2 * p);
        } else {
            g2.fillRect(x + 5 * p, y + 10 * p, 2 * p, 2 * p);
        }
    }

    private void drawFancyClouds(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 200));
        for (Point p : clouds) {
            g2.fillRoundRect(p.x, p.y, 60, 20, 15, 15);
            g2.fillRoundRect(p.x + 15, p.y - 15, 40, 25, 20, 20);
        }
    }

    class Bomb {
        double x, y, vx, vy;
        boolean onGround = false;

        Bomb(int x, int y) {
            this.x = x;
            this.y = y;
            this.vx = -4.5 - rand.nextDouble() * 2;
            this.vy = -7;
        }

        boolean update(double speed) {
            if (!onGround) {
                x += vx;
                y += vy;
                vy += 0.5;
                if (y >= GROUND_Y - 15) {
                    y = GROUND_Y - 15;
                    onGround = true;
                }
            } else x -= speed;
            return x < -50;
        }

        void render(Graphics2D g2) {
            g2.setColor(Color.BLACK);
            g2.fillOval((int) x, (int) y, 15, 15);
            g2.setColor(Color.RED);
            g2.fillRect((int) x + 6, (int) y - 4, 3, 5);
        }

        Rectangle getBounds() {
            return new Rectangle((int) x, (int) y, 15, 15);
        }
    }

    class Dagger {
        double x, y;

        Dagger(int x, int y) {
            this.x = x;
            this.y = y;
        }

        boolean update(double speed) {
            x += (speed + 4.5);
            return x > 1000;
        }

        void render(Graphics2D g2) {
            g2.setColor(Color.LIGHT_GRAY);
            g2.fillPolygon(new int[]{(int) x, (int) x + 20, (int) x}, new int[]{(int) y, (int) y + 5, (int) y + 10}, 3);
        }

        Rectangle getBounds() {
            return new Rectangle((int) x, (int) y, 20, 10);
        }
    }

    class Obstacle {
        double x;
        int y;

        Obstacle(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void render(Graphics2D g2) {
            g2.setColor(new Color(90, 55, 35));
            g2.fillRect((int) x + 15, y + 32, 10, 28);
            g2.setColor(new Color(40, 130, 40));
            g2.fillOval((int) x, y + 10, 40, 32);
        }

        Rectangle getBounds() {
            return new Rectangle((int) x + 5, y, 30, 60);
        }
    }

    class DustParticle {
        double x, y, vx, vy;
        int life = 255;

        DustParticle(int x, int y) {
            this.x = x;
            this.y = y;
            this.vx = -3 - rand.nextDouble() * 2;
            this.vy = -rand.nextDouble();
        }

        boolean update() {
            x += vx;
            y += vy;
            life -= 12;
            return life <= 0;
        }

        void draw(Graphics2D g2) {
            g2.setColor(new Color(200, 200, 200, life));
            g2.fillOval((int) x, (int) y, 5, 5);
        }
    }
}
/// //////////////////////////////////

/**
 * 背景面板组件
 */
class BackgroundPanel extends JPanel {
    private Image img;

    public BackgroundPanel(String path) {
        setOpaque(false);
        File f = new File(path);
        if (!f.exists()) f = new File("src/" + path);
        if (f.exists()) img = new ImageIcon(f.getAbsolutePath()).getImage();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (img != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int panelW = getWidth(), panelH = getHeight(), imgW = img.getWidth(this), imgH = img.getHeight(this);
            if (imgW > 0 && imgH > 0) {
                double scale = Math.max((double) panelW / imgW, (double) panelH / imgH);
                int drawW = (int) (imgW * scale), drawH = (int) (imgH * scale);
                g2.drawImage(img, (panelW - drawW) / 2, (panelH - drawH) / 2, drawW, drawH, this);
            }
            g2.dispose();
        } else {
            g.setColor(new Color(35, 35, 35));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        super.paintComponent(g);
    }
}
///一件替换助手

class ReplacerPanel extends BackgroundPanel {
    private JTextField[] inputs = new JTextField[5];
    private final String[] placeholders = {
            "XXX项目", "NARI-XXXXXX", "2026年XX月XX日", "XXXXXXXXXXX", "****@qq.com"
    };
    private JTextArea logArea;

    public ReplacerPanel(MainLauncher parent) {
        super("texture2.png");
        setLayout(new BorderLayout());

        // 顶部导航
        JButton backBtn = new JButton(" << 返回主菜单 ");
        backBtn.addActionListener(e -> parent.showMenu());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        top.add(backBtn);
        add(top, BorderLayout.NORTH);

        // 主配置区 (GridLayout)
        JPanel p = new JPanel(new GridLayout(6, 1, 5, 5));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 50, 10, 50));

        for (int i = 0; i < 5; i++) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            JLabel label = new JLabel((i + 1) + ". 修改 [" + placeholders[i] + "] 为: ");
            label.setPreferredSize(new Dimension(220, 30));
            label.setForeground(Color.WHITE); // 适配的背景色
            row.add(label, BorderLayout.WEST);
            inputs[i] = new JTextField();
            row.add(inputs[i], BorderLayout.CENTER);
            p.add(row);
        }

        // 日志显示区
        logArea = new JTextArea("【操作手册】\n1. 在上方对应框内填入新内容。\n2. 直接拖入包含文件的文件夹或文件。\n3. 支持 .docx, .xls, .xlsx 。\n4. 完美保留原文档样式。\n------------------------------------------------\n");
        logArea.setEditable(false);
        logArea.setBackground(new Color(0, 0, 0, 60));
        logArea.setForeground(Color.CYAN);

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        // 绑定拖拽逻辑
        new DropTarget(logArea, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                    Map<String, String> map = new HashMap<>();
                    for (int i = 0; i < 5; i++) {
                        String v = inputs[i].getText().trim();
                        if (!v.isEmpty()) map.put(placeholders[i], v);
                    }

                    if (map.isEmpty()) {
                        log("请至少输入一个替换内容！");
                        return;
                    }

                    new Thread(() -> {
                        for (File f : files) recursiveScan(f, map);
                        log(" 任务已完成！");
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "处理完成！"));
                    }).start();
                } catch (Exception e) {
                    log("异常: " + e.getMessage());
                }
            }
        });

        JPanel centerContainer = new JPanel(new BorderLayout(10, 10));
        centerContainer.setOpaque(false);
        centerContainer.add(p, BorderLayout.NORTH);
        centerContainer.add(scroll, BorderLayout.CENTER);

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

        try (InputStream is = FileMagic.prepareToCheckMagic(new FileInputStream(src))) {
            FileMagic fm = FileMagic.valueOf(is);

            // 处理 .docx
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
            // 处理 .doc
            else if (fm == FileMagic.OLE2 && name.endsWith(".doc")) {
                try (HWPFDocument doc = new HWPFDocument(is)) {
                    org.apache.poi.hwpf.usermodel.Range r = doc.getRange();
                    for (Map.Entry<String, String> e : map.entrySet()) {
                        r.replaceText(e.getKey(), e.getValue()); // 定稿版默认支持
                    }
                    try (FileOutputStream fos = new FileOutputStream(temp)) { doc.write(fos); }
                    success = true;
                }
            }
            // 处理 Excel
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

            if (success && temp.exists()) {
                Files.copy(temp.toPath(), src.toPath(), StandardCopyOption.REPLACE_EXISTING);
                log("[处理成功] " + src.getName());
            }
        } catch (Exception e) {
            log("[跳过] " + src.getName() + " -> " + e.getMessage());
        } finally {
            if (temp.exists()) temp.delete();
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
                // 1. 暂存第一个 Run 的样式属性
                XWPFRun origin = runs.get(0);
                String fontFamily = origin.getFontFamily();
                int fontSize = origin.getFontSize();
                String color = origin.getColor();
                boolean isBold = origin.isBold();
                boolean isItalic = origin.isItalic();
                UnderlinePatterns underline = origin.getUnderline();

                // 2. 清空当前段落的所有 Run
                for (int i = runs.size() - 1; i >= 0; i--) {
                    p.removeRun(i);
                }

                // 3. 创建新 Run 并恢复样式
                XWPFRun newRun = p.createRun();
                newRun.setText(newText);

                // 恢复属性（如果原属性存在则设置）
                if (fontFamily != null) newRun.setFontFamily(fontFamily);
                if (fontSize != -1) newRun.setFontSize(fontSize);
                if (color != null) newRun.setColor(color);
                newRun.setBold(isBold);
                newRun.setItalic(isItalic);
                newRun.setUnderline(underline);

                // 针对中文字体可能失效的问题，可以强制指定
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
}
/**
 * 一键转换助手
 */

class ConverterPanel extends BackgroundPanel {

    private JTextArea logArea;

    // ======= 内置任务队列（单类实现“调度器”）=======
    private ExecutorService pool = Executors.newFixedThreadPool(2);

    public ConverterPanel(MainLauncher parent) {
        super("texture2.png");
        setLayout(new BorderLayout());

        initTopBar(parent);
        initLogArea();
        setupDropTarget();
        resetLog();
    }

    // ================= UI =================

    private void initTopBar(MainLauncher parent) {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(15, 25, 10, 25));

        JButton backBtn = new JButton(" << 返回主菜单 ");
        backBtn.setFont(new Font("微软雅黑", Font.PLAIN, 14)); // 图片中字体偏细，建议用 PLAIN
        backBtn.setForeground(new Color(220, 220, 220));    // 淡淡的灰白色
        backBtn.setFocusPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

// 1. 设置半透明深灰色背景（这是图片质感的来源）
        backBtn.setBackground(new Color(60, 60, 60, 180));
        backBtn.setOpaque(false); // 必须为 false 才能配合自定义绘制

// 2. 复合边框：外层是科技蓝细边，内层是留白
        backBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 100, 150, 150), 1), // 科技蓝半透明边框
                BorderFactory.createEmptyBorder(5, 15, 5, 15)                   // 内部留白
        ));

// 3. 重写绘制方法，实现那层深色填充
        backBtn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void update(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 绘制深色背景
                g2.setColor(c.getBackground());
                g2.fillRect(0, 0, c.getWidth(), c.getHeight());

                super.update(g2, c);
                g2.dispose();
            }
        });
        backBtn.setFont(new Font("微软雅黑", Font.BOLD, 13));

        backBtn.setContentAreaFilled(false);

        backBtn.addActionListener(e -> parent.showMenu());

        topBar.add(backBtn, BorderLayout.WEST);
        add(topBar, BorderLayout.NORTH);
    }

    private void initLogArea() {
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setOpaque(false);
        logArea.setForeground(new Color(255, 215, 0));
        logArea.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 15));
        logArea.setLineWrap(true);
        logArea.setMargin(new Insets(30, 40, 30, 40));

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));

        add(scroll, BorderLayout.CENTER);
    }

    // ================= 拖拽 =================

    private void setupDropTarget() {
        new DropTarget(logArea, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) dtde.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);

                    startBatch(files);

                } catch (Exception e) {
                    log("拖拽失败: " + e.getMessage());
                }
            }
        });
    }

    // ================= 批量入口 =================

    private void startBatch(List<File> files) {
        log("\n引擎启动：任务队列加载中...");

        for (File f : files) {
            pool.submit(() -> processFileWithRetry(f));
        }
    }

    // ================= 带重试 =================

    private void processFileWithRetry(File file) {
        int maxRetry = 2;

        for (int i = 0; i <= maxRetry; i++) {
            try {
                processSingleFile(file);
                return;
            } catch (Exception e) {
                if (i == maxRetry) {
                    log(" 最终失败: " + file.getName());
                } else {
                    log(" 重试中 (" + (i + 1) + "): " + file.getName());
                }
            }
        }
    }

    // ================= 核心转换 =================

    private void processSingleFile(File file) throws Exception {

        String name = file.getName().toLowerCase();

        boolean isPdf = name.endsWith(".pdf");
        boolean isWord = name.endsWith(".docx") || name.endsWith(".doc");

        if (!isPdf && !isWord) {
            log(" 跳过: " + file.getName());
            return;
        }

        File output = buildOutput(file, isPdf);

        log(" 处理中: " + file.getName());

        File vbs = createVbsScript(file.getAbsolutePath(), output.getAbsolutePath(), isPdf);

        Process process = Runtime.getRuntime()
                .exec("wscript.exe \"" + vbs.getAbsolutePath() + "\"");

        boolean finished = process.waitFor(120, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("超时");
        }

        if (!output.exists()) {
            throw new RuntimeException("转换失败");
        }

        log("    完成 → " + output.getName());

        vbs.delete();
    }

    // ================= 输出文件策略 =================

    private File buildOutput(File input, boolean pdfToWord) {

        String base = input.getAbsolutePath().replaceAll("\\..+$", "");
        String ext = pdfToWord ? ".docx" : ".pdf";

        File out = new File(base + ext);

        // 防止覆盖
        int i = 1;
        while (out.exists()) {
            out = new File(base + "_" + i + ext);
            i++;
        }

        return out;
    }

    // ================= VBS 引擎 =================

    private File createVbsScript(String input, String output, boolean pdfToDocx) throws IOException {

        File vbs = File.createTempFile("engine_", ".vbs");

        int format = pdfToDocx ? 16 : 17;

        String script =
                "On Error Resume Next\n" +
                        "Set app = CreateObject(\"WPS.Application\")\n" +
                        "If Err.Number <> 0 Then\n" +
                        "  Err.Clear\n" +
                        "  Set app = CreateObject(\"Word.Application\")\n" +
                        "End If\n" +
                        "\n" +
                        "If app Is Nothing Then WScript.Quit\n" +
                        "\n" +
                        "app.Visible = False\n" +
                        "app.DisplayAlerts = 0\n" +
                        "\n" +
                        "Set doc = app.Documents.Open(\"" + escape(input) + "\", False, True)\n" +
                        "\n" +
                        "If Err.Number = 0 Then\n" +
                        "  doc.SaveAs2 \"" + escape(output) + "\", " + format + "\n" +
                        "  doc.Close False\n" +
                        "End If\n" +
                        "\n" +
                        "app.Quit\n" +
                        "Set doc = Nothing\n" +
                        "Set app = Nothing\n";

        try (OutputStreamWriter writer =
                     new OutputStreamWriter(new FileOutputStream(vbs), "GBK")) {
            writer.write(script);
        }

        return vbs;
    }

    private String escape(String path) {
        return path.replace("\\", "\\\\");
    }

    // ================= 日志 =================

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void resetLog() {
        logArea.setText(
                "--------------------------------------------\n" +
                        "引擎：WPS / Office 自动切换\n" +
                        "支持：PDF 互相转换 Word\n" +
                        "--------------------------------------------\n"
        );
    }
}