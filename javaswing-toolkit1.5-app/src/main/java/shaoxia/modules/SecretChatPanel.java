package shaoxia.modules;

import shaoxia.MainLauncher;
import shaoxia.Utils.ActionButton;
import shaoxia.Utils.GlassPanel;
import shaoxia.Utils.GlassTextField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * 加密通话面板 (动态 N 进制编码版) - UI 终极美化版
 * 允许自定义任意长度(≥2)、任意字符(中英皆可)作为不重复密钥。
 * 支持加密所有 UTF-8 字符（中英文、符号、表情包等）。
 */
public class SecretChatPanel extends BackgroundPanel {
    private MainLauncher parent;
    private GlassTextField keyField;
    private JTextArea inputArea;
    private JTextArea outputArea;

    public SecretChatPanel(MainLauncher parent) {
        super("bg_imgs/月球与地球.png");
        this.parent = parent;
        setLayout(new BorderLayout(15, 15));
        setOpaque(false);
        setBorder(new EmptyBorder(15, 20, 20, 20));

        initTopBar();
        initCoreWorkspace();
    }

    private void initTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        ActionButton backBtn = new ActionButton("<< 返回菜单");
        backBtn.setPreferredSize(new Dimension(130, 36));
        backBtn.addActionListener(e -> parent.showMenu());
        topBar.add(backBtn, BorderLayout.WEST);

        add(topBar, BorderLayout.NORTH);
    }

    private void initCoreWorkspace() {
        JPanel workspace = new JPanel(new BorderLayout(10, 10));
        workspace.setOpaque(false);

        // 顶部：密钥设置区
        GlassPanel keyPanel = new GlassPanel(new BorderLayout(15, 0));
        keyPanel.setBorder(new EmptyBorder(12, 15, 12, 15));

        JLabel keyLabel = new JLabel("加密密钥 (至少2个不重复字符，中英皆可): ");
        keyLabel.setForeground(Color.WHITE);
        keyLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));

        keyField = new GlassTextField();
        keyField.setText("AB"); // 默认用2个英文作为示例 (二进制加密)
        keyField.setPreferredSize(new Dimension(0, 36));
        keyField.setFont(new Font("微软雅黑", Font.BOLD, 14));
        keyField.setHorizontalAlignment(JTextField.CENTER);

        keyPanel.add(keyLabel, BorderLayout.WEST);
        keyPanel.add(keyField, BorderLayout.CENTER);
        workspace.add(keyPanel, BorderLayout.NORTH);

        // 中间：输入与输出区
        JPanel textPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        textPanel.setOpaque(false);

        // ✨ 新增核心：创建一个通用的双击复制监听器
        MouseAdapter doubleClickCopyListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    JTextArea src = (JTextArea) e.getSource();
                    String text = src.getText().trim();
                    if (!text.isEmpty()) {
                        StringSelection selection = new StringSelection(text);
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                        JOptionPane.showMessageDialog(SecretChatPanel.this, "内容已成功复制到剪贴板！", "提示", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        };

        // 左侧输入区
        GlassPanel leftWrapper = new GlassPanel(new BorderLayout(0, 10));
        leftWrapper.setBorder(new EmptyBorder(15, 15, 15, 15));
        // ✨ 修改了标题文本以作提示
        JLabel inputLabel = new JLabel("密文输入:", JLabel.LEFT);
        inputLabel.setForeground(Color.WHITE);
        inputLabel.setFont(new Font("微软雅黑", Font.BOLD, 13));

        inputArea = new JTextArea("Hello World");
        inputArea.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        inputArea.setLineWrap(true);
        inputArea.setOpaque(false);
        inputArea.setForeground(Color.WHITE);
        inputArea.setBackground(new Color(0, 0, 0, 0));
        inputArea.setCaretColor(Color.WHITE);
        // ✨ 为输入区绑定双击复制
        inputArea.addMouseListener(doubleClickCopyListener);

        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setOpaque(false);
        inputScroll.getViewport().setOpaque(false);
        inputScroll.setBorder(null);

        leftWrapper.add(inputLabel, BorderLayout.NORTH);
        leftWrapper.add(inputScroll, BorderLayout.CENTER);
        textPanel.add(leftWrapper);

        // 右侧输出区
        GlassPanel rightWrapper = new GlassPanel(new BorderLayout(0, 10));
        rightWrapper.setBorder(new EmptyBorder(15, 15, 15, 15));
        // ✨ 修改了标题文本以作提示
        JLabel outputLabel = new JLabel("转换结果:", JLabel.LEFT);
        outputLabel.setForeground(Color.WHITE);
        outputLabel.setFont(new Font("微软雅黑", Font.BOLD, 13));

        outputArea = new JTextArea();
        outputArea.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        outputArea.setLineWrap(true);
        outputArea.setEditable(false);
        outputArea.setOpaque(false);
        outputArea.setForeground(Color.WHITE);
        outputArea.setBackground(new Color(0, 0, 0, 0));
        // ✨ 为输出区绑定双击复制
        outputArea.addMouseListener(doubleClickCopyListener);

        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setOpaque(false);
        outputScroll.getViewport().setOpaque(false);
        outputScroll.setBorder(null);

        rightWrapper.add(outputLabel, BorderLayout.NORTH);
        rightWrapper.add(outputScroll, BorderLayout.CENTER);
        textPanel.add(rightWrapper);

        workspace.add(textPanel, BorderLayout.CENTER);

        // 底部：操作按钮区
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        btnPanel.setOpaque(false);

        // 左侧按钮居中包裹
        JPanel leftBtnWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        leftBtnWrapper.setOpaque(false);
        ActionButton encryptBtn = new ActionButton("执行加密运算 ->");
        encryptBtn.setPreferredSize(new Dimension(160, 42));
        encryptBtn.addActionListener(e -> processText(true));
        leftBtnWrapper.add(encryptBtn);

        // 右侧按钮居中包裹
        JPanel rightBtnWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        rightBtnWrapper.setOpaque(false);
        ActionButton decryptBtn = new ActionButton("执行解密逆运算 <-");
        decryptBtn.setPreferredSize(new Dimension(160, 42));
        decryptBtn.addActionListener(e -> processText(false));
        rightBtnWrapper.add(decryptBtn);

        btnPanel.add(leftBtnWrapper);
        btnPanel.add(rightBtnWrapper);
        workspace.add(btnPanel, BorderLayout.SOUTH);

        add(workspace, BorderLayout.CENTER);
    }

    private void processText(boolean isEncrypt) {
        String key = keyField.getText().trim();

        // --- 动态校验密钥合法性 ---
        if (key.length() < 2) {
            JOptionPane.showMessageDialog(this, "密钥太短啦！至少需要 2 个字符。", "拦截", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 校验是否有重复字符
        Set<Character> charSet = new HashSet<>();
        for (char c : key.toCharArray()) {
            charSet.add(c);
        }
        if (charSet.size() != key.length()) {
            JOptionPane.showMessageDialog(this, "密钥中包含重复字符，请确保每个字符都是唯一的！", "拦截", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String input = inputArea.getText();
        if (input.isEmpty()) return;

        try {
            if (isEncrypt) {
                String result = encrypt(input, key);
                outputArea.setText(result);
            } else {
                String result = decrypt(input, key);
                outputArea.setText(result);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "运算失败，请检查密文是否完整或密钥是否匹配。\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- 核心算法：基于动态 N 进制的字节流编码 ---

    /**
     * 计算用 N 进制表示一个字节(0-255)需要多少位字符
     */
    private int getRequiredDigits(int radix) {
        int digits = 1;
        while (Math.pow(radix, digits) < 256) {
            digits++;
        }
        return digits;
    }

    private String encrypt(String plainText, String key) {
        byte[] bytes = plainText.getBytes(StandardCharsets.UTF_8);
        int radix = key.length(); // 动态进制 (比如密钥是2个字，就是2进制)
        int digits = getRequiredDigits(radix);

        StringBuilder sb = new StringBuilder(bytes.length * digits);

        for (byte b : bytes) {
            int val = b & 0xFF; // 转为无符号整数 (0-255)
            char[] chunk = new char[digits];
            // 将字节转为 N 进制数，并直接映射为密钥中的对应字符
            for (int i = digits - 1; i >= 0; i--) {
                chunk[i] = key.charAt(val % radix);
                val /= radix;
            }
            sb.append(chunk);
        }
        return sb.toString();
    }

    private String decrypt(String cipherText, String key) {
        String cleanText = cipherText.replaceAll("\\s+", ""); // 移除可能存在的换行或空格
        int radix = key.length();
        int digits = getRequiredDigits(radix);

        if (cleanText.length() % digits != 0) {
            throw new IllegalArgumentException("密文长度必须是 " + digits + " 的倍数");
        }

        byte[] bytes = new byte[cleanText.length() / digits];
        for (int i = 0; i < bytes.length; i++) {
            int val = 0;
            // 将 digits 个字符逆向还原为字节数值
            for (int j = 0; j < digits; j++) {
                char c = cleanText.charAt(i * digits + j);
                int digitValue = key.indexOf(c);
                if (digitValue == -1) {
                    throw new IllegalArgumentException("密文包含密钥范围外的非法字符: " + c);
                }
                val = val * radix + digitValue;
            }
            if (val > 255) {
                throw new IllegalArgumentException("解码数值越界，可能是密钥不匹配导致的。");
            }
            bytes[i] = (byte) val;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}