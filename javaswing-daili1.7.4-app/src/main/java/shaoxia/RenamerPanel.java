package shaoxia;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 协同改名助手 RenamerPanel (Y轴精准夹击提取防越界 + 去除户名标签最终版 + 全全局黑雾玻璃美化) */
class RenamerPanel extends BackgroundPanel {
    private JComboBox<String> biaoCombo, baoCombo, typeCombo, companyCombo;
    private JTextField newCompanyInput;
    private JLabel previewLabel;
    private DefaultComboBoxModel<String> companyModel;

    private JPanel leftPanel;
    private JPanel rightPanel;
    private boolean isFlipped = false;

    private boolean isReceiptMode = false;
    private JLabel dropInfoLabel;
    private ActionButton receiptToggleBtn;

    // --- 全局统一玻璃质感色（黑雾半透玻璃） ---
    private final Color glassColor = new Color(30, 35, 40, 120);

    // 内部类：用于存放精准坐标的文字块
    class TextChunk {
        String text;
        float x, y, endX;

        public TextChunk(String text, float x, float y, float endX) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.endX = endX;
        }
    }

    public RenamerPanel(MainLauncher parent) {
        super("texture2.png");
        setLayout(new GridLayout(1, 2));
        setOpaque(false);

        // ---------------- --- 控制面板区 ---------------- ---
        leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(false);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JPanel topLeftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        topLeftPanel.setOpaque(false);
        ActionButton backBtn = new ActionButton("<< Menu", glassColor);
        backBtn.setPreferredSize(new Dimension(130, 36));
        backBtn.addActionListener(e -> parent.showMenu());
        topLeftPanel.add(backBtn);
        leftPanel.add(topLeftPanel, BorderLayout.NORTH);

        JPanel centerLeftPanel = new JPanel(new GridBagLayout());
        centerLeftPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.weightx = 1.0;
        gbc.weighty = 0;

        ActionButton flipBtn = new ActionButton("翻转页面", glassColor);
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        centerLeftPanel.add(flipBtn, gbc);

        flipBtn.addActionListener(e -> {
            isFlipped = !isFlipped;
            removeAll();
            if (isFlipped) {
                add(rightPanel);
                add(leftPanel);
            } else {
                add(leftPanel);
                add(rightPanel);
            }
            revalidate();
            repaint();
        });

        receiptToggleBtn = new ActionButton("电子回单命名生成", glassColor);
        gbc.gridy = 1;
        centerLeftPanel.add(receiptToggleBtn, gbc);

        ActionButton generateDirBtn = new ActionButton("应答文件夹生成", glassColor);
        gbc.gridy = 2;
        centerLeftPanel.add(generateDirBtn, gbc);
        generateDirBtn.addActionListener(e -> handleGenerateDirectories());

        // 使用专门定制的玻璃化方法创建全部下拉框
        biaoCombo = createGlassComboBox(new DefaultComboBoxModel<>(generateList("标", 20)));
        addControlRow(centerLeftPanel, gbc, 3, "1. 选择标段:", biaoCombo);

        baoCombo = createGlassComboBox(new DefaultComboBoxModel<>(generateList("包", 20)));
        addControlRow(centerLeftPanel, gbc, 4, "2. 选择包号:", baoCombo);

        String[] types = {"价格文件", "商务文件", "技术文件", "二轮报价", "三轮报价", "四轮报价", "五轮报价", "六轮报价", "首轮明细报价" ,"二轮明细报价", "三轮明细报价", "四轮明细报价", "五轮明细报价", "六轮明细报价"};
        typeCombo = createGlassComboBox(new DefaultComboBoxModel<>(types));
        addControlRow(centerLeftPanel, gbc, 5, "3. 文件类型:", typeCombo);

        companyModel = new DefaultComboBoxModel<>(new String[]{"请先添加供应商"});
        companyCombo = createGlassComboBox(companyModel);
        addControlRow(centerLeftPanel, gbc, 6, "4. 选择公司:", companyCombo);

        // 玻璃质感文本输入框
        newCompanyInput = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(glassColor);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                super.paintComponent(g);
                g2.dispose();
            }
            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 60));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
            }
        };
        newCompanyInput.setOpaque(false);
        newCompanyInput.setForeground(Color.WHITE);
        newCompanyInput.setCaretColor(Color.WHITE);
        newCompanyInput.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));

        addControlRow(centerLeftPanel, gbc, 7, "5. 管理供应商:", newCompanyInput);

        JPanel btnP = new JPanel(new GridLayout(1, 3, 8, 0));
        btnP.setOpaque(false);
        ActionButton save = new ActionButton("保存公司", glassColor);
        ActionButton delete = new ActionButton("删除公司", glassColor);
        ActionButton reset = new ActionButton("重置名单", glassColor);

        save.setPreferredSize(new Dimension(0, 32));
        btnP.add(save);
        btnP.add(delete);
        btnP.add(reset);

        gbc.gridy = 8;
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        centerLeftPanel.add(btnP, gbc);

        leftPanel.add(centerLeftPanel, BorderLayout.CENTER);

        // ---------------- --- 监听与预览面板区 ---------------- ---
        rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setOpaque(false);
        GridBagConstraints rGbc = new GridBagConstraints();
        rGbc.insets = new Insets(10, 20, 20, 20);
        rGbc.fill = GridBagConstraints.BOTH;

        previewLabel = new JLabel("预览：等待输入...", JLabel.CENTER);
        previewLabel.setFont(MainLauncher.BOLD_FONT);
        previewLabel.setForeground(Color.WHITE);
        rGbc.gridy = 0;
        rGbc.weighty = 0.05;
        rGbc.weightx = 1.0;
        rightPanel.add(previewLabel, rGbc);

        JPanel dropArea = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 35, 40, 90));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);
                g2.setColor(new Color(255, 255, 255, 90));
                Stroke dashed = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{10.0f, 8.0f}, 0.0f);
                g2.setStroke(dashed);
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 30, 30);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        dropArea.setOpaque(false);
        dropArea.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        dropInfoLabel = new JLabel(getDropInfoHtml("拖入文件重命名", "系统准备就绪，释放文件即可处理"), JLabel.CENTER);
        dropInfoLabel.setForeground(Color.WHITE);
        dropArea.add(dropInfoLabel, BorderLayout.CENTER);

        setDropTarget(dropArea);

        rGbc.gridy = 1;
        rGbc.weighty = 0.95;
        rightPanel.add(dropArea, rGbc);

        add(leftPanel);
        add(rightPanel);

        // ---------------- --- 事件监听绑定 ---------------- ---
        ActionListener listener = e -> updatePreview();
        biaoCombo.addActionListener(listener);
        baoCombo.addActionListener(listener);
        typeCombo.addActionListener(listener);
        companyCombo.addActionListener(listener);

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

        delete.addActionListener(e -> {
            String selected = (String) companyCombo.getSelectedItem();
            if (selected != null && !selected.contains("请先添加")) {
                companyModel.removeElement(selected);
                if (companyModel.getSize() == 0) {
                    companyModel.addElement("请先添加供应商");
                }
                updatePreview();
            } else {
                JOptionPane.showMessageDialog(this, "请先选择一个具体的供应商！");
            }
        });

        newCompanyInput.addActionListener(e -> save.doClick());

        reset.addActionListener(e -> {
            companyModel.removeAllElements();
            companyModel.addElement("请先添加供应商");
            updatePreview();
        });

        receiptToggleBtn.addActionListener(e -> {
            isReceiptMode = !isReceiptMode;
            if (isReceiptMode) {
                receiptToggleBtn.setText("协同改名助手");
                receiptToggleBtn.updateColor(glassColor);
                dropInfoLabel.setText(getDropInfoHtml("拖入回单文件夹重命名", "系统将自动解析PDF并按格式生成归档"));
                previewLabel.setText("预览：收款方账户_收款方户名_交易时间_金额.pdf");
                toggleLeftControls(false);
            } else {
                receiptToggleBtn.setText("电子回单命名生成");
                receiptToggleBtn.updateColor(glassColor);
                dropInfoLabel.setText(getDropInfoHtml("拖入文件重命名", "系统准备就绪，释放文件即可处理"));
                updatePreview();
                toggleLeftControls(true);
            }
        });
    }

    // --- 独家定制的纯净玻璃质感下拉框生成器 ---
    // --- 彻底修复的纯净玻璃质感下拉框生成器 ---
    private JComboBox<String> createGlassComboBox(ComboBoxModel<String> model) {
        JComboBox<String> combo = new JComboBox<String>(model) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(glassColor); // 内部填充玻璃色
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                super.paintComponent(g);
                g2.dispose();
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 60)); // 白霜描边，和文本框一致
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
            }
        };

        combo.setOpaque(false);
        combo.setForeground(Color.WHITE);
        combo.setBackground(new Color(0, 0, 0, 0)); // 剥离系统默认底色

        // 彻底重写底层的下拉框 UI 组件，拦截默认的纯白矩形渲染和系统高亮色
        combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                // 替换掉默认那个极丑的立体灰色小方块按钮
                JButton btn = new JButton("▼");
                btn.setContentAreaFilled(false);
                btn.setBorderPainted(false);
                btn.setFocusPainted(false);
                btn.setForeground(new Color(255, 255, 255, 180));
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                return btn;
            }
            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                // 关键拦截：什么都不画，彻底杀掉系统自带的灰色和蓝色选中背景，让我们的玻璃背景透出来
            }
        });

        // 适配展开后的下拉列表，区分“合上展示”和“展开列表”的状态
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (index == -1) {
                    // index 为 -1 代表此时画的是下拉框表面（合上时的状态）
                    // 必须设置为透明，让底下 paintComponent 画的玻璃色透上来！
                    l.setOpaque(false);
                } else {
                    // index 不为 -1 代表此时画的是展开后的下拉选项列表
                    l.setOpaque(true);
                    if (isSelected) {
                        l.setBackground(new Color(80, 90, 100)); // 鼠标悬停时的深灰色
                    } else {
                        l.setBackground(new Color(40, 45, 50)); // 下拉列表基础底色
                    }
                }

                l.setForeground(Color.WHITE);
                l.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8)); // 增加文字呼吸感
                return l;
            }
        });

        return combo;
    }

    private void toggleLeftControls(boolean state) {
        biaoCombo.setEnabled(state);
        baoCombo.setEnabled(state);
        typeCombo.setEnabled(state);
        companyCombo.setEnabled(state);
        newCompanyInput.setEnabled(state);
    }

    private String getDropInfoHtml(String title, String subtitle) {
        return "<html><center>" +
                "<font size='6'><b>" + title + "</b></font><br><br>" +
                "<font size='4' color='#B0B0B0'>" + subtitle + "</font>" +
                "</center></html>";
    }

    private void handleGenerateDirectories() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("请选择包含报价文件的目录");

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();
            File[] files = selectedDir.listFiles();
            if (files == null) return;

            int successCount = 0;
            for (File file : files) {
                if (file.isDirectory()) continue;

                String fileName = file.getName();
                int firstUnder = fileName.indexOf('_');
                int lastUnder = fileName.lastIndexOf('_');

                if (firstUnder != -1 && lastUnder != -1 && firstUnder != lastUnder) {
                    String supplierName = fileName.substring(firstUnder + 1, lastUnder);
                    String typeWithExt = fileName.substring(lastUnder + 1);

                    String typeNoExt = typeWithExt;
                    if (typeWithExt.contains(".")) {
                        typeNoExt = typeWithExt.substring(0, typeWithExt.lastIndexOf('.'));
                    }

                    String rootFolderName = getTargetRootFolder(typeNoExt);

                    if (rootFolderName != null) {
                        File rootDir = new File(selectedDir, rootFolderName);
                        File supplierDir = new File(rootDir, supplierName);

                        if (!supplierDir.exists()) supplierDir.mkdirs();

                        File targetFile = new File(supplierDir, fileName);
                        if (file.renameTo(targetFile)) successCount++;
                    }
                }
            }
            JOptionPane.showMessageDialog(this, "智能归档完毕！\n共成功处理并分类了 " + successCount + " 个文件。");
        }
    }

    private String getTargetRootFolder(String type) {
        if (type.contains("技术") || type.contains("商务") || type.contains("价格") || type.contains("首轮") ) {
            return "应答文件";
        }
        return null;
    }

    private void addControlRow(JPanel p, GridBagConstraints g, int r, String t, Component c) {
        g.gridy = r;
        g.gridx = 0;
        g.weighty = 0;
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
        if (isReceiptMode) return;
        Object selected = companyCombo.getSelectedItem();
        String c = (selected != null) ? selected.toString() : "未选公司";
        if (c.contains("请先")) c = "未选公司";
        previewLabel.setText("预览：" + biaoCombo.getSelectedItem() + baoCombo.getSelectedItem() + "_" + c + "_" + typeCombo.getSelectedItem());
    }

    private void setDropTarget(JPanel p) {
        new DropTarget(p, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            public void drop(DropTargetDropEvent dtde) {
                if (isReceiptMode) {
                    handleReceiptDrop(dtde);
                } else {
                    handleNormalDrop(dtde);
                }
            }
        });
    }

    private void handleReceiptDrop(DropTargetDropEvent dtde) {
        try {
            dtde.acceptDrop(DnDConstants.ACTION_COPY);
            List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            int successCount = 0;

            for (File fileOrDir : files) {
                if (fileOrDir.isDirectory()) {
                    File outDir = new File(fileOrDir.getParent(), fileOrDir.getName() + "_重命名");
                    if (!outDir.exists()) outDir.mkdirs();

                    File[] pdfs = fileOrDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
                    if (pdfs != null) {
                        for (File pdf : pdfs) {
                            if (splitAndProcessPdf(pdf, outDir)) successCount++;
                        }
                    }
                } else if (fileOrDir.getName().toLowerCase().endsWith(".pdf")) {
                    File outDir = new File(fileOrDir.getParent(), "回单重命名_输出");
                    if (!outDir.exists()) outDir.mkdirs();
                    if (splitAndProcessPdf(fileOrDir, outDir)) successCount++;
                }
            }
            JOptionPane.showMessageDialog(null, "电子回单处理完毕！\n处理流程已完成，请检查输出文件夹。");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "处理时发生异常: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean splitAndProcessPdf(File sourcePdf, File outDir) {
        boolean processed = false;
        try (PDDocument document = PDDocument.load(sourcePdf)) {
            int totalPages = document.getNumberOfPages();
            String originalName = sourcePdf.getName().replaceFirst("[.][^.]+$", "");

            if (totalPages > 1) {
                for (int i = 0; i < totalPages; i++) {
                    PDDocument singleDoc = new PDDocument();
                    singleDoc.addPage(document.getPage(i));
                    File tempFile = new File(outDir, originalName + "_page" + (i + 1) + ".pdf");
                    singleDoc.save(tempFile);
                    singleDoc.close();
                    if (processSinglePdfReceipt(tempFile, outDir)) processed = true;
                    tempFile.delete();
                }
            } else {
                processed = processSinglePdfReceipt(sourcePdf, outDir);
            }
        } catch (Exception e) {
            System.err.println("读取文件失败: " + sourcePdf.getName());
            e.printStackTrace();
        }
        return processed;
    }

    private boolean processSinglePdfReceipt(File pdfFile, File outDir) {
        try (PDDocument document = PDDocument.load(pdfFile)) {

            final List<TextChunk> chunks = new ArrayList<>();
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
                    if (textPositions.isEmpty()) return;
                    String text = string.trim();
                    if (!text.isEmpty()) {
                        float x = textPositions.get(0).getXDirAdj();
                        float y = textPositions.get(0).getYDirAdj();
                        chunks.add(new TextChunk(text, x, y, x + textPositions.get(textPositions.size()-1).getWidthDirAdj()));
                    }
                }
            };
            stripper.setSortByPosition(true);
            stripper.getText(document);

            float boundaryX = 200f;
            for (TextChunk c : chunks) {
                if (c.text.contains("收款方")) {
                    boundaryX = c.x - 40;
                    break;
                }
            }

            float rightAccountY = -1;
            float rightNameY = -1;
            float rightBankY = -1;

            for (TextChunk c : chunks) {
                if (c.x > boundaryX) {
                    if (c.text.contains("账号") && rightAccountY == -1) rightAccountY = c.y;
                    if (c.text.contains("户名") && rightNameY == -1) rightNameY = c.y;
                    if (c.text.contains("开户行") && rightBankY == -1) rightBankY = c.y;
                }
            }

            StringBuilder payeeNameBuilder = new StringBuilder();
            float minY = (rightAccountY != -1) ? rightAccountY + 5 : ((rightNameY != -1) ? rightNameY - 15 : 0);
            float maxY = (rightBankY != -1) ? rightBankY - 5 : Float.MAX_VALUE;

            for (TextChunk c : chunks) {
                if (c.x > boundaryX && c.y > minY && c.y < maxY) {
                    String t = c.text.trim();
                    t = t.replace("户名", "").replace("收款方", "").replace("付款方", "").replace("账号", "").replace(":", "").replace("：", "").trim();
                    if (!t.isEmpty()) {
                        payeeNameBuilder.append(t);
                    }
                }
            }

            String payeeName = payeeNameBuilder.toString().replaceAll("\\s+", "");
            if (payeeName.isEmpty()) {
                payeeName = "内部转取或未记载";
            }

            String payeeAccount = "未获取账号";
            if (rightAccountY != -1) {
                for (TextChunk c : chunks) {
                    if (c.x > boundaryX && Math.abs(c.y - rightAccountY) < 15) {
                        Matcher m = Pattern.compile("(\\d{10,30})").matcher(c.text.replaceAll("\\s", ""));
                        if (m.find()) {
                            payeeAccount = m.group(1);
                            break;
                        }
                    }
                }
            }

            StringBuilder globalText = new StringBuilder();
            for (TextChunk c : chunks) globalText.append(c.text).append(" ");
            String gText = globalText.toString();

            String transactionDate = "未知时间";
            Matcher dateM1 = Pattern.compile("交易时间\\s*(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})").matcher(gText);
            if (dateM1.find()) {
                transactionDate = dateM1.group(1).replace(":", ".");
            } else {
                Matcher dateM2 = Pattern.compile("交易时间\\s*(\\d{4}-\\d{2}-\\d{2})").matcher(gText);
                if (dateM2.find()) {
                    transactionDate = dateM2.group(1);
                }
            }

            String amount = "0.00";
            Matcher amtM = Pattern.compile("金额[（(]小写[）)]\\s*([\\d,]+\\.\\d{2})").matcher(gText);
            if (amtM.find()) {
                amount = amtM.group(1).replace(",", "");
            }

            String newFileName = String.format("%s_%s_%s_%s.pdf", payeeAccount, payeeName, transactionDate, amount);

            File targetFile = new File(outDir, newFileName);
            int copyIndex = 1;
            while (targetFile.exists()) {
                targetFile = new File(outDir, newFileName.replace(".pdf", "_副本" + copyIndex + ".pdf"));
                copyIndex++;
            }

            Files.copy(pdfFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;

        } catch (Exception e) {
            System.err.println("解析PDF失败: " + pdfFile.getName());
            e.printStackTrace();
            return false;
        }
    }

    private void handleNormalDrop(DropTargetDropEvent dtde) {
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

    private Vector<String> generateList(String p, int c) {
        Vector<String> v = new Vector<>();
        for (int i = 1; i <= c; i++) v.add(p + i);
        return v;
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
                int newAlpha = Math.min(255, bgColor.getAlpha() + 40);
                drawColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), newAlpha);
            } else if (isHover) {
                int newAlpha = Math.max(0, bgColor.getAlpha() - 40);
                drawColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), newAlpha);
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