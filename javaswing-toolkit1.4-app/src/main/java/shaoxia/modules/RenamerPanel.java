package shaoxia.modules;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import shaoxia.MainLauncher;
import shaoxia.Utils.ActionButton;
import shaoxia.Utils.GlassComboBox;
import shaoxia.Utils.GlassDropPanel;
import shaoxia.Utils.GlassTextField;

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

/** 协同改名助手 RenamerPanel (全局黑雾玻璃组件化完全体 - 15px圆角统一版) */
public class RenamerPanel extends BackgroundPanel {
    private JComboBox<String> biaoCombo, baoCombo, typeCombo, companyCombo;
    private JTextField newCompanyInput;
    private JLabel previewLabel;
    private DefaultComboBoxModel<String> companyModel;

    private JPanel leftPanel;
    private JPanel rightPanel;
    private boolean isFlipped = false;

    // ✨ 状态机：0=协同改名助手(默认), 1=协同批量改名助手, 2=农业银行回单命名生成, 3=招商银行回单命名生成
    private int currentMode = 0;
    private JLabel dropInfoLabel;
    private ActionButton modeToggleBtn;

    // 保留此变量用于按钮点击切换颜色
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
        super("bg_imgs/月球与地球.png");
        setLayout(new GridLayout(1, 2));
        setOpaque(false);

        // ---------------- --- 控制面板区 ---------------- ---
        leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(false);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JPanel topLeftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        topLeftPanel.setOpaque(false);
        ActionButton backBtn = new ActionButton("<< Menu");
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

        // 1. 翻转页面按钮
        ActionButton flipBtn = new ActionButton("翻转页面");
        flipBtn.setPreferredSize(new Dimension(0, 38));
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

        // 2. 模式切换按钮
        modeToggleBtn = new ActionButton("当前模式: 协同改名助手");
        modeToggleBtn.setPreferredSize(new Dimension(0, 38));
        gbc.gridy = 1;
        centerLeftPanel.add(modeToggleBtn, gbc);

        // 3. 应答文件夹按钮
        ActionButton generateDirBtn = new ActionButton("应答文件夹生成");
        generateDirBtn.setPreferredSize(new Dimension(0, 38));
        gbc.gridy = 2;
        centerLeftPanel.add(generateDirBtn, gbc);
        generateDirBtn.addActionListener(e -> handleGenerateDirectories());

        biaoCombo = new GlassComboBox<>(new DefaultComboBoxModel<>(generateList("标", 20)));
        addControlRow(centerLeftPanel, gbc, 3, "1. 选择标段:", biaoCombo);

        baoCombo = new GlassComboBox<>(new DefaultComboBoxModel<>(generateList("包", 20)));
        addControlRow(centerLeftPanel, gbc, 4, "2. 选择包号:", baoCombo);

        String[] types = {"价格文件", "商务文件", "技术文件", "二轮报价", "三轮报价", "四轮报价", "五轮报价", "六轮报价", "首轮明细报价" ,"二轮明细报价", "三轮明细报价", "四轮明细报价", "五轮明细报价", "六轮明细报价"};
        typeCombo = new GlassComboBox<>(new DefaultComboBoxModel<>(types));
        addControlRow(centerLeftPanel, gbc, 5, "3. 文件类型:", typeCombo);

        companyModel = new DefaultComboBoxModel<>(new String[]{"请先添加供应商"});
        companyCombo = new GlassComboBox<>(companyModel);
        addControlRow(centerLeftPanel, gbc, 6, "4. 选择公司:", companyCombo);

        newCompanyInput = new GlassTextField();
        addControlRow(centerLeftPanel, gbc, 7, "5. 管理供应商:", newCompanyInput);

        JPanel btnP = new JPanel(new GridLayout(1, 3, 8, 0));
        btnP.setOpaque(false);
        ActionButton save = new ActionButton("保存公司");
        ActionButton delete = new ActionButton("删除公司");
        ActionButton reset = new ActionButton("重置名单");

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

        GlassDropPanel dropArea = new GlassDropPanel();
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

        // ✨ 核心逻辑：四态按钮切换
        modeToggleBtn.addActionListener(e -> {
            currentMode = (currentMode + 1) % 4; // 0 -> 1 -> 2 -> 3 循环
            switchUIByMode();
        });

        // 初始渲染一次预览
        updatePreview();
    }

    // ✨ 状态机UI控制中枢
    private void switchUIByMode() {
        modeToggleBtn.updateColor(glassColor);

        if (currentMode == 0) {
            modeToggleBtn.setText("当前模式: 协同改名助手");
            toggleLeftControls(true, true);
            dropInfoLabel.setText(getDropInfoHtml("拖入文件重命名", "系统准备就绪，释放文件即可处理"));
            updatePreview();

        } else if (currentMode == 1) {
            modeToggleBtn.setText("当前模式: 协同批量改名助手");
            toggleLeftControls(true, false);
            dropInfoLabel.setText(getDropInfoHtml("拖入批量文件重命名", "将自动截取倒数一二下划线之间文字作为供应商"));
            updatePreview();

        } else if (currentMode == 2) {
            modeToggleBtn.setText("当前模式: 农业银行回单命名生成");
            toggleLeftControls(false, false);
            dropInfoLabel.setText(getDropInfoHtml("拖入农行回单文件夹重命名", "系统将自动解析PDF并按农行格式生成归档"));
            updatePreview();

        } else if (currentMode == 3) {
            modeToggleBtn.setText("当前模式: 招商银行回单命名生成");
            toggleLeftControls(false, false);
            dropInfoLabel.setText(getDropInfoHtml("拖入招行回单文件夹重命名", "系统将自动解析PDF并按招行格式生成归档"));
            updatePreview();
        }
    }

    private void toggleLeftControls(boolean basicControls, boolean supplierControls) {
        biaoCombo.setEnabled(basicControls);
        baoCombo.setEnabled(basicControls);
        typeCombo.setEnabled(basicControls);

        companyCombo.setEnabled(supplierControls);
        newCompanyInput.setEnabled(supplierControls);
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
        if (currentMode == 2) {
            previewLabel.setText("预览：收款方账户_收款方户名_交易时间_金额.pdf");
            return;
        }
        if (currentMode == 3) {
            previewLabel.setText("预览：收款账号_收款人_交易日期_金额.pdf");
            return;
        }
        if (currentMode == 1) {
            previewLabel.setText("预览：" + biaoCombo.getSelectedItem() + baoCombo.getSelectedItem() + "_[自动提取供应商]_" + typeCombo.getSelectedItem());
            return;
        }
        // 模式 0 逻辑
        Object selected = companyCombo.getSelectedItem();
        String c = (selected != null) ? selected.toString() : "未选公司";
        if (c.contains("请先")) c = "未选公司";
        previewLabel.setText("预览：" + biaoCombo.getSelectedItem() + baoCombo.getSelectedItem() + "_" + c + "_" + typeCombo.getSelectedItem());
    }

    private void setDropTarget(JPanel p) {
        new DropTarget(p, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            public void drop(DropTargetDropEvent dtde) {
                // ✨ 拖拽事件分发器
                if (currentMode == 2 || currentMode == 3) {
                    handleReceiptDrop(dtde);
                } else if (currentMode == 1) {
                    handleBatchDrop(dtde);
                } else {
                    handleNormalDrop(dtde);
                }
            }
        });
    }

    private void handleBatchDrop(DropTargetDropEvent dtde) {
        try {
            dtde.acceptDrop(DnDConstants.ACTION_COPY);
            List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

            for (File file : files) {
                if (file.isDirectory()) continue;

                String fileName = file.getName();
                String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")) : "";
                String nameNoExt = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName;

                int lastUnder = nameNoExt.lastIndexOf('_');
                if (lastUnder == -1) continue;
                int secondLastUnder = nameNoExt.lastIndexOf('_', lastUnder - 1);
                if (secondLastUnder == -1) continue;

                String autoSupplier = nameNoExt.substring(secondLastUnder + 1, lastUnder);

                String baseName = biaoCombo.getSelectedItem() + (String) baoCombo.getSelectedItem() + "_" + autoSupplier + "_" + typeCombo.getSelectedItem();
                File newFile = new File(file.getParent(), baseName + ext);

                if (newFile.exists()) {
                    int count = 1;
                    while (newFile.exists()) {
                        newFile = new File(file.getParent(), baseName + "_复件" + count + ext);
                        count++;
                    }
                }
                file.renameTo(newFile);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
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
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
        // ✨ 如果是模式3（招商银行），直接拦截并走专属处理逻辑，其余模式原封不动
        if (currentMode == 3) {
            return processCMBReceipt(pdfFile, outDir);
        }

        // ======================= 下方为原封不动的模式 2（农业银行）解析逻辑 =======================
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

    // ✨ 新增：专属的招商银行解析引擎 (模式3)
    private boolean processCMBReceipt(File pdfFile, File outDir) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String gText = stripper.getText(document);

            String payeeAccount = "未获取账号";
            String payeeName = "未获取收款人";
            String transactionDate = "未知时间";
            String amount = "0.00";

            // 1. 提取交易日期并格式化 YYYY-MM-DD
            Matcher dateM = Pattern.compile("交易日期[：:]?\\s*(\\d{4})\\s*年\\s*(\\d{1,2})\\s*月\\s*(\\d{1,2})").matcher(gText);
            if (dateM.find()) {
                transactionDate = String.format("%04d-%02d-%02d",
                        Integer.parseInt(dateM.group(1)),
                        Integer.parseInt(dateM.group(2)),
                        Integer.parseInt(dateM.group(3)));
            }

            // 2. 提取收款账号
            Matcher accM = Pattern.compile("收款账号[：:]?\\s*([0-9a-zA-Z]+)").matcher(gText);
            if (accM.find()) {
                payeeAccount = accM.group(1);
            }

            // 3. 提取收款人 (读取直到行尾并剔除空格)
            Matcher nameM = Pattern.compile("收款人[：:]?\\s*([^\\r\\n]+)").matcher(gText);
            if (nameM.find()) {
                payeeName = nameM.group(1).replaceAll("\\s+", "");
            }

            // 4. 提取交易金额 (过滤掉 CNY 字符与金额中的逗号)
            Matcher amtM = Pattern.compile("交易金额[（(]小写[）)][：:]?\\s*[A-Za-z]*\\s*([\\d,\\.]+)").matcher(gText);
            if (amtM.find()) {
                amount = amtM.group(1).replace(",", "");
            }

            // 组装最终文件名
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
            System.err.println("解析招商银行PDF失败: " + pdfFile.getName());
            e.printStackTrace();
            return false;
        }
    }

    private Vector<String> generateList(String p, int c) {
        Vector<String> v = new Vector<>();
        for (int i = 1; i <= c; i++) v.add(p + i);
        return v;
    }
}