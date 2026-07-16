package me.shaoxia;

import com.formdev.flatlaf.FlatDarkLaf;
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
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Vector;

class MainLauncher extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainContainer;
    public static final Font MAIN_FONT = new Font("微软雅黑", Font.PLAIN, 16);
    public static final Font BOLD_FONT = new Font("微软雅黑", Font.BOLD, 18);

    public MainLauncher() {
        setTitle("代理助手 1.3 ");
        setSize(1200, 950);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLocationRelativeTo(null);

        // --- 新增：设置窗口图标 ---
        try {
            // 路径逻辑：先找根目录，再找 src 目录
            String iconPath = "orange.png";
            File iconFile = new File(iconPath);
            if (!iconFile.exists()) {
                iconFile = new File("src/" + iconPath);
            }

            if (iconFile.exists()) {
                ImageIcon icon = new ImageIcon(iconFile.getAbsolutePath());
                setIconImage(icon.getImage());
            }
        } catch (Exception e) {
            // 即使图标加载失败，也不影响程序启动
            System.err.println("图标加载失败");
        }
        // -----------------------

        cardLayout = new CardLayout();

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        mainContainer.add(createMainMenu(), "MENU");
        mainContainer.add(new RenamerPanel(this), "RENAMER");
        mainContainer.add(new CheckerPanel(this), "CHECKER");

        add(mainContainer);
        cardLayout.show(mainContainer, "MENU");
    }

    private JPanel createMainMenu() {
        BackgroundPanel menuPanel = new BackgroundPanel("texture2.png");
        menuPanel.setLayout(new BorderLayout());

        JPanel topArea = new JPanel(new GridBagLayout());
        topArea.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(30, 20, 20, 20);

        JLabel title = new JLabel("代理助手 1.3");
        title.setFont(new Font("微软雅黑", Font.BOLD, 45));
        title.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        topArea.add(title, gbc);

        JButton btn1 = createModuleButton("协同改名助手", "RENAMER", new Color(52, 152, 219));
        JButton btn2 = createModuleButton("信息协同数据校对助手", "CHECKER", new Color(46, 204, 113));

        gbc.gridwidth = 1; gbc.gridy = 1;
        gbc.gridx = 0; topArea.add(btn1, gbc);
        gbc.gridx = 1; topArea.add(btn2, gbc);

        JPanel gameContainer = new JPanel(new BorderLayout());
        gameContainer.setOpaque(false);
        DinoGamePanel gamePanel = new DinoGamePanel();
        gamePanel.setVisible(false);

        JButton toggleGameBtn = new JButton("开启摸鱼模式");
        toggleGameBtn.setFont(MAIN_FONT);
        toggleGameBtn.addActionListener(e -> {
            boolean visible = !gamePanel.isVisible();
            gamePanel.setVisible(visible);
            toggleGameBtn.setText(visible ? "老板来了！(收起游戏)" : "摸鱼时间 (开启游戏)");
            menuPanel.revalidate();
            if(visible) gamePanel.requestFocusInWindow();
        });

        JPanel btnWrapper = new JPanel();
        btnWrapper.setOpaque(false);
        btnWrapper.add(toggleGameBtn);

        gameContainer.add(btnWrapper, BorderLayout.NORTH);
        gameContainer.add(gamePanel, BorderLayout.SOUTH);

        menuPanel.add(topArea, BorderLayout.CENTER);
        menuPanel.add(gameContainer, BorderLayout.SOUTH);

        return menuPanel;
    }

    private JButton createModuleButton(String text, String cardName, Color bg) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(350, 110));
        btn.setFont(new Font("微软雅黑", Font.BOLD, 22));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.addActionListener(e -> cardLayout.show(mainContainer, cardName));
        return btn;
    }

    public void showMenu() { cardLayout.show(mainContainer, "MENU"); }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        UIManager.put("defaultFont", new Font("微软雅黑", Font.PLAIN, 14));
        UIManager.put("OptionPane.messageFont", new Font("微软雅黑", Font.PLAIN, 14));
        SwingUtilities.invokeLater(() -> new MainLauncher().setVisible(true));
    }
}

// --- 板块 1：协同改名助手 ---
class RenamerPanel extends JPanel {
    private JComboBox<String> biaoCombo, baoCombo, typeCombo, companyCombo;
    private JTextField newCompanyInput;
    private JLabel previewLabel;
    private DefaultComboBoxModel<String> companyModel;

    public RenamerPanel(MainLauncher parent) {
        setLayout(new GridLayout(1, 2));

        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBackground(new Color(45, 48, 50));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.weightx = 1.0;

        JButton backBtn = new JButton(" << 返回主菜单 ");
        backBtn.setFont(MainLauncher.BOLD_FONT);
        gbc.gridy = 0; gbc.gridx = 0; gbc.gridwidth = 2;
        leftPanel.add(backBtn, gbc);
        backBtn.addActionListener(e -> parent.showMenu());

        biaoCombo = new JComboBox<>(generateList("标", 20));
        addControlRow(leftPanel, gbc, 1, "1. 选择标段:", biaoCombo);
        baoCombo = new JComboBox<>(generateList("包", 20));
        addControlRow(leftPanel, gbc, 2, "2. 选择包号:", baoCombo);

        // 指定的文件类型
        String[] types = {
                "价格文件", "商务文件", "技术文件", "二轮报价文件", "三轮报价文件", "四轮报价文件",
                "五轮报价文件", "六轮报价文件", "二轮分项报价文件", "三轮分项报价文件",
                "四轮分项报价文件", "五轮分项报价文件", "六轮分项报价文件"
        };
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
        btnP.add(save); btnP.add(reset);
        gbc.gridy = 6; gbc.gridx = 1; gbc.gridwidth = 1; leftPanel.add(btnP, gbc);

        save.addActionListener(e -> {
            String n = newCompanyInput.getText().trim();
            if(!n.isEmpty()){
                if(companyModel.getSize() > 0 && companyModel.getElementAt(0).contains("请先")) companyModel.removeElementAt(0);
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

        BackgroundPanel rightPanel = new BackgroundPanel("texture2.png");
        rightPanel.setLayout(new GridBagLayout());
        GridBagConstraints rGbc = new GridBagConstraints();
        rGbc.insets = new Insets(20, 20, 20, 20);
        rGbc.fill = GridBagConstraints.BOTH;

        previewLabel = new JLabel("预览：等待输入...", JLabel.CENTER);
        previewLabel.setFont(MainLauncher.BOLD_FONT);
        previewLabel.setForeground(Color.WHITE);
        rGbc.gridy = 0; rGbc.weighty = 0.2; rGbc.weightx = 1.0;
        rightPanel.add(previewLabel, rGbc);

        JPanel dropArea = new JPanel(new BorderLayout());
        dropArea.setOpaque(false);
        dropArea.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2, true),
                " 拖入文件重命名 ", TitledBorder.CENTER, TitledBorder.TOP, MainLauncher.BOLD_FONT, Color.WHITE));

        JLabel dl = new JLabel("<html><center>准备就绪<br>拖入文件自动处理</center></html>", JLabel.CENTER);
        dl.setForeground(Color.WHITE);
        dropArea.add(dl);

        setDropTarget(dropArea);
        rGbc.gridy = 1; rGbc.weighty = 0.8;
        rightPanel.add(dropArea, rGbc);

        ActionListener listener = e -> updatePreview();
        biaoCombo.addActionListener(listener); baoCombo.addActionListener(listener);
        typeCombo.addActionListener(listener); companyCombo.addActionListener(listener);

        add(leftPanel); add(rightPanel);
    }

    private void addControlRow(JPanel p, GridBagConstraints g, int r, String t, Component c) {
        g.gridy = r; g.gridx = 0; g.weightx = 0.3; g.gridwidth = 1;
        JLabel l = new JLabel(t); l.setForeground(Color.WHITE); p.add(l, g);
        g.gridx = 1; g.weightx = 0.7; p.add(c, g);
    }

    private void updatePreview() {
        Object selected = companyCombo.getSelectedItem();
        String c = (selected != null) ? selected.toString() : "未选公司";
        if(c.contains("请先")) c = "未选公司";
        previewLabel.setText("预览：" + biaoCombo.getSelectedItem() + baoCombo.getSelectedItem() + "_" + c + "_" + typeCombo.getSelectedItem());
    }

    private void setDropTarget(JPanel p) {
        new DropTarget(p, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            public void drop(DropTargetDropEvent dtde) {
                try {
                    String comp = companyCombo.getSelectedItem().toString();
                    if(comp.contains("请先")) {
                        dtde.rejectDrop();
                        JOptionPane.showMessageDialog(null, "请先输入供应商名称！", "操作拦截", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    for(File file : files) {
                        String ext = file.getName().contains(".") ? file.getName().substring(file.getName().lastIndexOf(".")) : "";
                        String baseName = biaoCombo.getSelectedItem() + (String)baoCombo.getSelectedItem() + "_" + comp + "_" + typeCombo.getSelectedItem();
                        File newFile = new File(file.getParent(), baseName + ext);

                        // 重名提醒逻辑
                        if(newFile.exists()) {
                            int choice = JOptionPane.showConfirmDialog(null,
                                    "文件夹内已存在文件：\n" + newFile.getName() + "\n是否自动更名保存？",
                                    "检测到重名", JOptionPane.YES_NO_OPTION);

                            if(choice == JOptionPane.YES_OPTION) {
                                int count = 1;
                                while(newFile.exists()){
                                    newFile = new File(file.getParent(), baseName + "_复件" + count + ext);
                                    count++;
                                }
                            } else {
                                continue; // 跳过当前文件
                            }
                        }
                        file.renameTo(newFile);
                    }
                    JOptionPane.showMessageDialog(null, "处理成功！");
                } catch(Exception ex) { ex.printStackTrace(); }
            }
        });
    }

    private Vector<String> generateList(String p, int c) {
        Vector<String> v = new Vector<>();
        for(int i=1; i<=c; i++) v.add(p+i);
        return v;
    }
}

// --- 板块 2：信息协同数据校对助手 ---
class CheckerPanel extends JPanel {
    public CheckerPanel(MainLauncher parent) {
        setLayout(new BorderLayout());
        setBackground(new Color(45, 48, 50));
        JButton backBtn = new JButton(" << 返回主菜单 ");
        backBtn.addActionListener(e -> parent.showMenu());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false); top.add(backBtn);
        add(top, BorderLayout.NORTH);

        BackgroundPanel centerPanel = new BackgroundPanel("texture2.png");
        centerPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        JLabel infoLabel = new JLabel("<html><center><h2>信息协同数据校对助手</h2>" +
                "<p>将 <b>.xlsx</b> 招标数据文件拖入下方方框进行递减逻辑校验</p></center></html>");
        infoLabel.setForeground(Color.WHITE);
        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 30, 0);
        centerPanel.add(infoLabel, gbc);

        JPanel dropArea = new JPanel(new BorderLayout());
        dropArea.setPreferredSize(new Dimension(650, 350));
        dropArea.setOpaque(false);
        dropArea.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2, true),
                " 拖入 XLSX 进行逻辑校验 ", TitledBorder.CENTER, TitledBorder.TOP, MainLauncher.BOLD_FONT, Color.WHITE));
        JLabel dl = new JLabel("请拖入 Excel 文件", JLabel.CENTER);
        dl.setForeground(Color.WHITE); dl.setFont(MainLauncher.BOLD_FONT);
        dropArea.add(dl);
        setCheckDropTarget(dropArea);
        gbc.gridy = 1; centerPanel.add(dropArea, gbc);
        add(centerPanel, BorderLayout.CENTER);
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

    private void processExcel(File file) {
        List<String> errors = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            int supIdx = -1;
            String[] rounds = {"首轮报价", "二轮报价", "三轮报价", "四轮报价", "五轮报价", "六轮报价", "终轮报价"};
            int[] colIdxMap = new int[rounds.length];
            Arrays.fill(colIdxMap, -1);
            boolean foundHeader = false;
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
                double prevPrice = Double.MAX_VALUE;
                String prevName = "前一轮";
                for (int i = 0; i < colIdxMap.length; i++) {
                    int cIdx = colIdxMap[i];
                    if (cIdx == -1) continue;
                    Double currPrice = getNumericValue(row.getCell(cIdx));
                    if (currPrice != null) {
                        if (currPrice > prevPrice) {
                            errors.add("【逻辑错误】" + company + "\n    " + rounds[i] + "(" + currPrice + ") 竟然大于 " + prevName + "(" + prevPrice + ")");
                        }
                        prevPrice = currPrice;
                        prevName = rounds[i];
                    }
                }
            }
            if (errors.isEmpty()) JOptionPane.showMessageDialog(null, "校对完成，数据递减逻辑正确！");
            else showErrorDialog(errors);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "解析失败，请确保格式正确。");
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

    private void showErrorDialog(List<String> errors) {
        JTextArea area = new JTextArea(String.join("\n\n", errors));
        area.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        area.setEditable(false);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(550, 400));
        JOptionPane.showMessageDialog(null, scroll, "报价异常警报", JOptionPane.WARNING_MESSAGE);
    }
}

// --- 板块 3：摸鱼小游戏 ---
class DinoGamePanel extends JPanel implements ActionListener {
    private Timer timer = new Timer(18, this);
    private int dinoY = 160, velY = 0, score = 0;
    private boolean isOver = false, isStarted = false;
    private List<Rectangle> obstacles = new ArrayList<>();
    private List<Point> clouds = new ArrayList<>();
    private Random r = new Random();

    public DinoGamePanel() {
        setPreferredSize(new Dimension(0, 240));
        setFocusable(true);
        for(int i=0; i<3; i++) clouds.add(new Point(r.nextInt(1000), 30 + r.nextInt(50)));
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_SPACE) {
                    if(!isStarted) { isStarted = true; timer.start(); }
                    else if(isOver) reset();
                    else if(dinoY >= 160) velY = -15;
                }
            }
        });
    }

    private void reset() { score = 0; isOver = false; obstacles.clear(); dinoY = 160; timer.start(); }

    public void actionPerformed(ActionEvent e) {
        dinoY += velY;
        if(dinoY < 160) velY += 1;
        else { dinoY = 160; velY = 0; }
        for(Point p : clouds) { p.x -= 1; if(p.x < -100) p.x = 1200; }
        if(r.nextInt(50) == 1 && (obstacles.isEmpty() || obstacles.get(obstacles.size()-1).x < 850)) {
            obstacles.add(new Rectangle(1200, 170, 25, 40));
        }
        for(int i=0; i<obstacles.size(); i++) {
            Rectangle p = obstacles.get(i); p.x -= 12;
            if(p.intersects(new Rectangle(80, dinoY, 35, 40))) { isOver = true; timer.stop(); }
            if(p.x < -50) obstacles.remove(i--);
        }
        score++; repaint();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(247, 247, 247)); g2.fillRect(0,0,getWidth(),getHeight());
        g2.setColor(new Color(83, 83, 83));
        g2.drawLine(0, 210, getWidth(), 210);
        g2.setColor(new Color(173, 216, 230));
        for(Point p : clouds) g2.fillOval(p.x, p.y, 40, 20);
        g2.setColor(new Color(50, 50, 50));
        g2.fillRect(80, dinoY, 35, 40);
        g2.setColor(new Color(220, 20, 60));
        for(Rectangle p : obstacles) g2.fillRect(p.x, p.y, p.width, p.height);
        g2.setColor(Color.BLACK);
        g2.drawString("SCORE: " + score, getWidth()-100, 30);
        if(isOver) g2.drawString("GAME OVER! SPACE TO RESTART", getWidth()/2-150, 100);
        if(!isStarted) g2.drawString("PRESS SPACE TO START", getWidth()/2-80, 100);
    }
}

class BackgroundPanel extends JPanel {
    private Image img;
    public BackgroundPanel(String path) {
        setOpaque(false);
        File f = new File(path);
        if(!f.exists()) f = new File("src/" + path);
        if(f.exists()) img = new ImageIcon(f.getAbsolutePath()).getImage();
    }
    protected void paintComponent(Graphics g) {
        if(img != null) g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
        else { g.setColor(new Color(35, 35, 35)); g.fillRect(0, 0, getWidth(), getHeight()); }
        super.paintComponent(g);
    }
}