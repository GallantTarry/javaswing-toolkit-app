package shaoxia.modules;

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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.List;

/**
 * 原生文件转换助手 ConverterPanel (圣洁铭文 + 音乐播放 + 视觉中心上移)
 */
public class ConverterPanel extends BackgroundPanel {

    private JLabel dl;
    private GlassDropPanel dropArea;
    private OfficeEngineIndicator engineIndicator;

    private volatile boolean isEngineReady = false;
    private volatile boolean isCheckingEngine = true;
    private volatile boolean isConverting = false;

    private int currentProgress = 0;
    private Timer progressTimer;
    private String currentCenterText = "God Leads Us Along";

    // 音乐播放控制
    private volatile boolean isPlaying = false;
    private javazoom.jl.player.Player player;

    public ConverterPanel(MainLauncher parent) {
        super("bg_imgs/月球与地球.png");
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 20));

        ActionButton backBtn = new ActionButton("<< Menu");
        backBtn.setPreferredSize(new Dimension(130, 36));
        backBtn.addActionListener(e -> {
            stopMusic(); // 返回时切断圣歌
            parent.showMenu();
        });

        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftTop.setOpaque(false);
        leftTop.add(backBtn);
        topBar.add(leftTop, BorderLayout.WEST);

        engineIndicator = new OfficeEngineIndicator();
        engineIndicator.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isConverting) checkOfficeEngine();
            }
        });
        topBar.add(engineIndicator, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // 核心尺寸对标与上移：
        // 左右保持 80 确保与 CheckerPanel 绝对等宽，上下加起来是 140 确保绝对等高。
        // 将 top 从 60 缩减到 40，将 bottom 增加到 100，实现整体版面上移。
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(40, 80, 80, 80));

        // 内部面板严格采用 (20, 20, 20, 20) 边距
        dropArea = new GlassDropPanel();
        dropArea.setOpaque(false);
        dropArea.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        dropArea.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 隐形交互：点击面板控制圣歌播放
        dropArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleMusic();
            }
        });

        dl = new JLabel("", JLabel.CENTER);
        dropArea.add(dl, BorderLayout.CENTER);

        setConvertDropTarget(dropArea);

        centerPanel.add(dropArea, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        adjustFontSize();
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) { adjustFontSize(); }
        });

        checkOfficeEngine();
    }

    private void adjustFontSize() {
        int width = getWidth();
        if (width <= 0) return;

        double scale = Math.max(0.6, Math.min(width / 800.0, 1.4));
        int titleSize = (int) (48 * scale);
        int englishSize = (int) (26 * scale);

        String displayText;
        String color;

        if (isConverting) {
            int stage = currentProgress / 25;
            if (stage > 3) stage = 3;

            // 肃穆对称的四句诗篇，采用庄严的译本排版
            String[] phrases = {
                    "The LORD is my shepherd,",
                    "Resting in the green pastures,",
                    "Led beside the still waters,",
                    "He restores my weary soul."
            };

            displayText = "<span style='font-family: \"Georgia\", \"Times New Roman\", serif; font-size:" + englishSize + "px; font-style:italic; letter-spacing: 1px;'>" + phrases[stage] + "</span>" +
                    "<br><br><span style='font-family: monospaced; font-size:" + (titleSize / 3) + "px; color:#FFFFFF; opacity: 0.6; letter-spacing: 4px; font-weight:bold;'> " + currentProgress + "% </span>";
            color = "#EAEAEA";
        } else {
            displayText = "<span style='font-family: monospaced; font-size:" + titleSize + "px; font-weight:bold; letter-spacing: 8px;'>" + currentCenterText + "</span>";
            color = "white";
        }

        dl.setText("<html><center>" +
                "<span style='color:" + color + ";'>" +
                displayText + "</span>" +
                "</center></html>");

        dropArea.repaint();
        this.validate();
    }

    // --- 隐形音乐播放引擎 ---
    private void toggleMusic() {
        if (isPlaying) {
            stopMusic();
        } else {
            isPlaying = true;
            new Thread(() -> {
                try {
                    java.io.InputStream is = getClass().getResourceAsStream("/music/God Leads Us Along.mp3");

                    if (is != null) {
                        player = new javazoom.jl.player.Player(is);
                        player.play();

                        if (isPlaying) {
                            isPlaying = false;
                        }
                    } else {
                        System.out.println("提示：类路径中未找到 /music/God Leads Us Along.mp3");
                        isPlaying = false;
                    }
                } catch (Exception e) {
                    System.out.println("音乐播放中断或异常: " + e.getMessage());
                    isPlaying = false;
                }
            }).start();
        }
    }

    private void stopMusic() {
        isPlaying = false;
        if (player != null) {
            player.close();
        }
    }

    private String runPowerShellInMemory(String psCode) throws Exception {
        byte[] bytes = psCode.getBytes("UTF-16LE");
        String b64 = Base64.getEncoder().encodeToString(bytes);

        ProcessBuilder pb = new ProcessBuilder(
                "powershell", "-ExecutionPolicy", "Bypass", "-NoProfile", "-WindowStyle", "Hidden", "-EncodedCommand", b64
        );

        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("GBK")));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line);
        }
        process.waitFor();
        return output.toString().trim();
    }

    private void checkOfficeEngine() {
        isCheckingEngine = true;
        engineIndicator.repaint();

        new Thread(() -> {
            try {
                // 已移除强制关闭 WPS 的暴力指令，防止误杀其他用户的正常操作
                String psCode =
                        "$word = $null\n" +
                                "$ids = @('Word.Application.16', 'Word.Application.15', 'Word.Application.14', 'Word.Application')\n" +
                                "foreach ($id in $ids) {\n" +
                                "    try { $word = New-Object -ComObject $id } catch { continue }\n" +
                                "    if ($word -ne $null) {\n" +
                                "        if ($word.Name -notmatch 'WPS') { Write-Output 'SUCCESS'; $word.Quit(); exit 0 }\n" +
                                "        $word.Quit()\n" +
                                "        $word = $null\n" +
                                "    }\n" +
                                "}\n" +
                                "Write-Output 'FAIL'; exit 1";

                String result = runPowerShellInMemory(psCode);
                isEngineReady = result.equals("SUCCESS");

                // 已删除这里原本阻断式的 JOptionPane 弹窗报错，改为静默由右上角指示灯指示
            } catch (Exception e) {
                isEngineReady = false;
            } finally {
                isCheckingEngine = false;
                SwingUtilities.invokeLater(this::repaint);
            }
        }).start();
    }

    private void setConvertDropTarget(JPanel p) {
        new DropTarget(p, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    if (!isEngineReady) {
                        dtde.rejectDrop();
                        JOptionPane.showMessageDialog(null, "Office 引擎未就绪，请确认已安装 Microsoft Word。");
                        return;
                    }
                    if (isConverting) {
                        dtde.rejectDrop();
                        JOptionPane.showMessageDialog(null, "进程正在执行中，请耐心等待。");
                        return;
                    }

                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (files.isEmpty()) return;

                    if (files.size() > 1) {
                        JOptionPane.showMessageDialog(null, "为保证排版精确，目前仅支持单文件处理。");
                    }

                    File targetFile = files.get(0);
                    if (targetFile.isDirectory()) {
                        JOptionPane.showMessageDialog(null, "系统暂不支持对文件夹进行操作。");
                        return;
                    }

                    startConversionTask(targetFile);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void startConversionTask(File inputFile) {
        String name = inputFile.getName().toLowerCase();
        int typeFlag = 0;
        String ext = "";

        if (name.endsWith(".doc") || name.endsWith(".docx")) {
            typeFlag = 1; ext = "pdf";
        } else if (name.endsWith(".pdf")) {
            typeFlag = 2; ext = "docx";
        } else if (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg") || name.endsWith(".bmp")) {
            typeFlag = 3; ext = "pdf";
        } else {
            JOptionPane.showMessageDialog(this, "格式不兼容，系统仅支持 Word、PDF 或图片格式。");
            return;
        }

        final int finalTypeFlag = typeFlag;
        final String finalExt = ext;
        final String baseName = inputFile.getName().replaceFirst("[.][^.]+$", "");

        isConverting = true;
        currentProgress = 0;
        adjustFontSize();

        progressTimer = new Timer(100, e -> {
            if (currentProgress < 95) {
                int step = (currentProgress < 60) ? (int)(Math.random() * 3 + 1) : 1;
                currentProgress = Math.min(95, currentProgress + step);
                adjustFontSize();
            }
        });
        progressTimer.start();

        new Thread(() -> {
            try {
                File desktopDir = new File(System.getProperty("user.home"), "Desktop");
                File outFile = new File(desktopDir, baseName + "." + finalExt);

                int count = 1;
                while (outFile.exists()) {
                    outFile = new File(desktopDir, baseName + "_复件" + count + "." + finalExt);
                    count++;
                }

                String inPath = inputFile.getAbsolutePath().replace("'", "''");
                String outPath = outFile.getAbsolutePath().replace("'", "''");

                // 已移除强制关闭 WPS 的指令
                String psCode =
                        "$ErrorActionPreference = 'Stop'\n" +
                                "$word = $null\n" +
                                "$ids = @('Word.Application.16', 'Word.Application.15', 'Word.Application.14', 'Word.Application')\n" +
                                "foreach ($id in $ids) {\n" +
                                "    try { $word = New-Object -ComObject $id } catch { continue }\n" +
                                "    if ($word -ne $null) {\n" +
                                "        if ($word.Name -notmatch 'WPS') { break }\n" +
                                "        $word.Quit(); $word = $null\n" +
                                "    }\n" +
                                "}\n" +
                                "if ($word -eq $null) { Write-Output '无法唤醒纯净的Word引擎'; exit 1 }\n" +
                                "try {\n" +
                                "    $word.Visible = $false; $word.DisplayAlerts = 0; $word.AutomationSecurity = 3\n" +
                                "    if (" + finalTypeFlag + " -eq 1) {\n" +
                                "        $doc = $word.Documents.Open('" + inPath + "', $false, $true)\n" +
                                "        $doc.SaveAs([ref]'" + outPath + "', [ref]17)\n" +
                                "        $doc.Close([ref]$false)\n" +
                                "    } elseif (" + finalTypeFlag + " -eq 2) {\n" +
                                "        $doc = $word.Documents.Open('" + inPath + "', $false, $false)\n" +
                                "        $doc.SaveAs([ref]'" + outPath + "', [ref]16)\n" +
                                "        $doc.Close([ref]$false)\n" +
                                "    } elseif (" + finalTypeFlag + " -eq 3) {\n" +
                                "        $doc = $word.Documents.Add()\n" +
                                "        $shape = $doc.InlineShapes.AddPicture('" + inPath + "', $false, $true)\n" +
                                "        $doc.SaveAs([ref]'" + outPath + "', [ref]17)\n" +
                                "        $doc.Close([ref]$false)\n" +
                                "    }\n" +
                                "    $word.Quit(); Write-Output 'SUCCESS'; exit 0\n" +
                                "} catch {\n" +
                                "    Write-Output $_.Exception.Message\n" +
                                "    try { $word.Quit() } catch {}\n" +
                                "    exit 1\n" +
                                "}";

                String finalResultLog = runPowerShellInMemory(psCode);

                File finalOutFile = outFile;
                SwingUtilities.invokeLater(() -> {
                    progressTimer.stop();
                    if (finalOutFile.exists() && finalResultLog.equals("SUCCESS")) {
                        currentProgress = 100;
                        adjustFontSize();
                        new Timer(500, evt -> {
                            ((Timer)evt.getSource()).stop();
                            isConverting = false;
                            adjustFontSize();
                            JOptionPane.showMessageDialog(ConverterPanel.this, "转换已完成。\n文件已妥善保存于桌面：\n" + finalOutFile.getName(), "系统通知", JOptionPane.INFORMATION_MESSAGE);
                        }).start();
                    } else {
                        isConverting = false;
                        adjustFontSize();
                        String failMsg = finalResultLog.isEmpty() ? "内存注入被系统安全策略拦截。" : finalResultLog;
                        if (failMsg.length() > 60) failMsg = failMsg.substring(0, 60) + "...";
                        JOptionPane.showMessageDialog(ConverterPanel.this, "转换中止。\n系统日志：\n" + failMsg, "系统异常", JOptionPane.ERROR_MESSAGE);
                    }
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    progressTimer.stop();
                    isConverting = false;
                    adjustFontSize();
                    JOptionPane.showMessageDialog(ConverterPanel.this, "发生异常：" + e.getMessage(), "系统异常", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    class OfficeEngineIndicator extends JComponent {

        public OfficeEngineIndicator() {
            setPreferredSize(new Dimension(50, 50));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            //setToolTipText("Office Core (Click to Reboot)");
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            Color coreColor;
            Color glowColor;
            if (isCheckingEngine) {
                coreColor = new Color(255, 180, 0);
                glowColor = new Color(255, 180, 0, 80);
            } else if (isEngineReady) {
                coreColor = new Color(0, 150, 255);
                glowColor = new Color(0, 150, 255, 100);
            } else {
                coreColor = new Color(100, 100, 100);
                glowColor = new Color(50, 50, 50, 50);
            }

            if (isEngineReady && isConverting) {
                long time = System.currentTimeMillis();
                int pulseRadius = 18 + (int)(Math.sin(time / 100.0) * 4);
                g2.setColor(new Color(230, 230, 230, 80));
                g2.fillOval(cx - pulseRadius, cy - pulseRadius, pulseRadius * 2, pulseRadius * 2);
            } else {
                g2.setColor(glowColor);
                g2.fillOval(cx - 16, cy - 16, 32, 32);
            }

            g2.setColor(coreColor);
            g2.fillRoundRect(cx - 10, cy - 10, 20, 20, 6, 6);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int[] wX = {cx - 5, cx - 3, cx, cx + 3, cx + 5};
            int[] wY = {cy - 4, cy + 4, cy, cy + 4, cy - 4};
            g2.drawPolyline(wX, wY, 5);

            g2.dispose();
        }
    }
}