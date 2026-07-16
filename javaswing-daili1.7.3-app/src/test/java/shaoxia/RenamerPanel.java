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

// 引入 PDFBox 依赖库

/**协同改名助手 RenamerPanel (Y轴精准夹击提取防越界 + 去除户名标签最终版)*/
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
        leftPanel.setBackground(new Color(45, 48, 50, 220));
        leftPanel.setOpaque(true);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JPanel topLeftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        topLeftPanel.setOpaque(false);
        ActionButton backBtn = new ActionButton("<< 返回主菜单", new Color(255, 255, 255, 30));
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

        ActionButton flipBtn = new ActionButton("翻转页面", new Color(46, 139, 87));
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

        receiptToggleBtn = new ActionButton("电子回单命名生成", new Color(230, 126, 34));
        gbc.gridy = 1;
        centerLeftPanel.add(receiptToggleBtn, gbc);

        ActionButton generateDirBtn = new ActionButton("应答文件夹生成", new Color(70, 130, 180));
        gbc.gridy = 2;
        centerLeftPanel.add(generateDirBtn, gbc);
        generateDirBtn.addActionListener(e -> handleGenerateDirectories());

        biaoCombo = new JComboBox<>(generateList("标", 20));
        addControlRow(centerLeftPanel, gbc, 3, "1. 选择标段:", biaoCombo);

        baoCombo = new JComboBox<>(generateList("包", 20));
        addControlRow(centerLeftPanel, gbc, 4, "2. 选择包号:", baoCombo);

        String[] types = {"价格文件", "商务文件", "技术文件", "二轮报价", "三轮报价", "四轮报价", "五轮报价", "六轮报价", "首轮明细报价" ,"二轮明细报价", "三轮明细报价", "四轮明细报价", "五轮明细报价", "六轮明细报价"};
        typeCombo = new JComboBox<>(types);
        addControlRow(centerLeftPanel, gbc, 5, "3. 文件类型:", typeCombo);

        companyModel = new DefaultComboBoxModel<>(new String[]{"请先添加供应商"});
        companyCombo = new JComboBox<>(companyModel);
        addControlRow(centerLeftPanel, gbc, 6, "4. 选择公司:", companyCombo);

        newCompanyInput = new JTextField();
        addControlRow(centerLeftPanel, gbc, 7, "5. 管理供应商:", newCompanyInput);

        JPanel btnP = new JPanel(new GridLayout(1, 3, 8, 0));
        btnP.setOpaque(false);
        ActionButton save = new ActionButton("保存公司", new Color(39, 174, 96));
        ActionButton delete = new ActionButton("删除公司", new Color(231, 76, 60));
        ActionButton reset = new ActionButton("重置名单", new Color(149, 165, 166));

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
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);
                g2.setColor(new Color(255, 255, 255, 110));
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
                receiptToggleBtn.updateColor(new Color(155, 89, 182));
                dropInfoLabel.setText(getDropInfoHtml("拖入回单文件夹重命名", "系统将自动解析PDF并按格式生成归档"));
                // ⭐️ 修改点：更新预览界面显示的格式，去掉了“收款方户名”标签
                previewLabel.setText("预览：收款方账户_收款方户名_交易时间_金额.pdf");
                toggleLeftControls(false);
            } else {
                receiptToggleBtn.setText("电子回单命名生成");
                receiptToggleBtn.updateColor(new Color(230, 126, 34));
                dropInfoLabel.setText(getDropInfoHtml("拖入文件重命名", "系统准备就绪，释放文件即可处理"));
                updatePreview();
                toggleLeftControls(true);
            }
        });
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

    // 处理 PDF 拖拽入口，包含自动拆分页逻辑
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

    // 智能拆分多页并分发解析
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
                    tempFile.delete(); // 识别完后删除临时拆分页
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

    // ✨【核心革命】：Y 轴上下夹击法！彻底无视换行与错位
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
            stripper.setSortByPosition(true); // 保证文字大体是从上到下
            stripper.getText(document);

            // 1. 获取右半边的 X 轴分割线（寻找“收款方”）
            float boundaryX = 200f; // 默认给个页面居中值
            for (TextChunk c : chunks) {
                if (c.text.contains("收款方")) {
                    boundaryX = c.x - 40; // 确保完全处于右半边
                    break;
                }
            }

            // 2. 探针：获取右半侧所有关键标签的 Y 轴（高度）基准线！
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

            // ================= 3. 提取收款方户名 (Y轴夹击法：防断行、防开户行误入) =================
            StringBuilder payeeNameBuilder = new StringBuilder();
            // 天花板：在账号所在行的下方（或者户名稍微往上一点）
            float minY = (rightAccountY != -1) ? rightAccountY + 5 : ((rightNameY != -1) ? rightNameY - 15 : 0);
            // 地板：严格在开户行所在行的上方！
            float maxY = (rightBankY != -1) ? rightBankY - 5 : Float.MAX_VALUE;

            for (TextChunk c : chunks) {
                // 只要你处于右半边，且高度在天花板和地板之间，全部强制收割！
                if (c.x > boundaryX && c.y > minY && c.y < maxY) {
                    String t = c.text.trim();
                    // 暴力清洗所有混入的表头标签
                    t = t.replace("户名", "").replace("收款方", "").replace("付款方", "").replace("账号", "").replace(":", "").replace("：", "").trim();
                    if (!t.isEmpty()) {
                        payeeNameBuilder.append(t);
                    }
                }
            }

            // 剔除所有内部空格，完美解决 “有限公 司” 被空格断开的排版恶心问题
            String payeeName = payeeNameBuilder.toString().replaceAll("\\s+", "");
            if (payeeName.isEmpty()) {
                payeeName = "内部转取或未记载";
            }

            // ================= 4. 提取收款方账号 =================
            String payeeAccount = "未获取账号";
            if (rightAccountY != -1) {
                for (TextChunk c : chunks) {
                    if (c.x > boundaryX && Math.abs(c.y - rightAccountY) < 15) {
                        Matcher m = Pattern.compile("(\\d{10,30})").matcher(c.text.replaceAll("\\s", ""));
                        if (m.find()) {
                            payeeAccount = m.group(1);
                            break; // 抓到即止
                        }
                    }
                }
            }

            // ================= 5. 提取日期和金额 (全局拼接搜索) =================
            StringBuilder globalText = new StringBuilder();
            for (TextChunk c : chunks) globalText.append(c.text).append(" ");
            String gText = globalText.toString();

            String transactionDate = "未知时间";
            Matcher dateM1 = Pattern.compile("交易时间\\s*(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})").matcher(gText);
            if (dateM1.find()) {
                // 将 Windows 文件名不允许的冒号替换为点号
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
                amount = amtM.group(1).replace(",", ""); // 剔除千分位逗号
            }

            // ================= 6. 终极格式组装 =================
            // ⭐️ 修改点：去除了原本的“收款方户名_”字样
            String newFileName = String.format("%s_%s_%s_%s.pdf", payeeAccount, payeeName, transactionDate, amount);

            // ================= 7. 副本防重命名机制 =================
            File targetFile = new File(outDir, newFileName);
            int copyIndex = 1;
            while (targetFile.exists()) {
                targetFile = new File(outDir, newFileName.replace(".pdf", "_副本" + copyIndex + ".pdf"));
                copyIndex++;
            }

            // 执行文件归档保存
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