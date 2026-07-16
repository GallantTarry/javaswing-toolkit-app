package shaoxia.ui.components;

import shaoxia.Utils.GlassTextField;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class BlueCat extends JComponent {

    // ===== 基础属性 =====
    private double catX = 3000, catY = 3000;
    private final int catSize = 90;
    private boolean isDragging = false;
    private boolean isJumping = false;
    private int dragOffsetX, dragOffsetY;
    private double startX, startY, targetX, targetY;
    private double jumpProgress = 0;
    private final double jumpHeight = 110;
    private boolean faceRight = true;

    private boolean forceHidden = false;

    // ===== 动作状态机 =====
    private int currentAction = 0;
    private final int IDLE = 0;
    private final int LICK = 1;
    private final int WALK = 2;
    private final int JUMP = 3;
    private final int DRAG = 4;
    private final int SLEEP = 5;

    private Timer fpsTimer;
    private Timer actionTimer;
    private double animTime = 0;

    private String bubbleText = "";
    private boolean showBubble = false;
    private Timer bubbleTimer;
    private final Random random = new Random();
    private final JFrame parentFrame;

    private final Map<Integer, Image> catImages = new HashMap<>();
    private boolean isImageLoaded = false;

    private final Point[] grid = new Point[9];
    private final ArrayList<JButton> cachedButtons = new ArrayList<>();

    // ===== AI 聊天专属属性 =====
    private boolean isChatting = false;
    private boolean isAiThinking = false;
    private long lastAiSpeakTime = 0;

    private JPanel chatPanel;
    private GlassTextField chatInputArea;

    private final String ZHIPU_API_KEY = "752710571acf4e16b7ed04e50c6653bf.P8hG6RptM3QhYqFb";

    public BlueCat(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(null);
        setOpaque(false);
        setDoubleBuffered(true);
        setBounds(0, 0, parentFrame.getWidth(), parentFrame.getHeight());

        loadAndSliceSheet();
        initMouse();
        startEngines();

        SwingUtilities.invokeLater(() -> {
            updateBounds();
            if (grid[0] != null) {
                catX = grid[0].x;
                catY = grid[0].y;
                smartRepaintCurrent();
            }
        });

        if (isImageLoaded) {
            say("Wow! It's so nice to meet you!");
        }
    }

    private void smartRepaint(double oldX, double oldY) {
        repaint();
        Toolkit.getDefaultToolkit().sync(); // ✨ 加在这里！强制硬件加速刷新
    }

    private void smartRepaintCurrent() {
        repaint();
        Toolkit.getDefaultToolkit().sync(); // ✨ 还有这里！
    }

    public void setCatVisible(boolean visible) {
        this.forceHidden = !visible;
        this.setVisible(visible);
        if (visible) {
            recalculateGrid();
        }
        repaint();
    }

    public void recalculateGrid() {
        if (parentFrame == null) return;

        if (cachedButtons.size() < 9) {
            cachedButtons.clear();
            findModuleButtons(parentFrame.getContentPane());
        }

        if (cachedButtons.size() >= 9) {
            for (int i = 0; i < 9; i++) {
                JButton btn = cachedButtons.get(i);
                if (btn != null && btn.isShowing()) {
                    Point btnPos = SwingUtilities.convertPoint(btn.getParent(), btn.getLocation(), this);
                    int centerX = btnPos.x + (btn.getWidth() / 2);
                    int topY = btnPos.y + 18;
                    grid[i] = new Point(centerX, topY);
                }
            }
        } else {
            int w = parentFrame.getWidth();
            int h = parentFrame.getHeight();
            int[] colX = { (int)(w * 0.235), (int)(w * 0.50), (int)(w * 0.765) };
            int[] rowY = { (int)(h * 0.34), (int)(h * 0.53), (int)(h * 0.72) };
            for (int i = 0; i < 9; i++) {
                grid[i] = new Point(colX[i % 3], rowY[i / 3]);
            }
        }
    }

    private void findModuleButtons(Component comp) {
        if (cachedButtons.size() >= 9) return;
        if (comp instanceof JButton) {
            JButton btn = (JButton) comp;
            if (btn.getPreferredSize() != null && btn.getPreferredSize().width == 270) {
                cachedButtons.add(btn);
            }
            return;
        }
        if (comp instanceof Container) {
            Component[] children = ((Container) comp).getComponents();
            for (Component child : children) {
                findModuleButtons(child);
            }
        }
    }

    private void loadAndSliceSheet() {
        String[] searchPaths = {
                "bluecat_sheet.png", "bluecat_sheet.jpg",
                "resources/bluecat_sheet.png", "resources/bluecat_sheet.jpg",
                "src/resources/bluecat_sheet.png", "src/bluecat_sheet.png"
        };
        File file = null;
        for (String path : searchPaths) {
            File f = new File(path);
            if (f.exists() && f.isFile()) { file = f; break; }
        }
        try {
            BufferedImage bigImg = null;
            if (file != null) { bigImg = ImageIO.read(file); }
            else {
                URL url = getClass().getResource("/resources/bluecat_sheet.png");
                if (url == null) url = getClass().getResource("/bluecat_sheet.png");
                if (url != null) bigImg = ImageIO.read(url);
            }
            if (bigImg == null) return;

            int sheetW = bigImg.getWidth();
            int sheetH = bigImg.getHeight();
            double aspect = (double) sheetW / sheetH;

            int catW = sheetW / 6;
            int startY = 0;
            int catH = sheetH;

            if (aspect > 3.5) {
                startY = (int) (sheetH * 0.12);
                catH = sheetH - startY- 6;
            } else {
                startY = (int) (sheetH * 0.080);
                catH = (int) (sheetH * 0.175) - 6;
            }

            int[] actions = {IDLE, LICK, WALK, JUMP, DRAG, SLEEP};
            for (int i = 0; i < 6; i++) {
                int currentX = i * catW;
                if (currentX + catW > sheetW) catW = sheetW - currentX;
                if (startY + catH > sheetH) catH = sheetH - startY;

                BufferedImage sub = bigImg.getSubimage(currentX, startY, catW, catH);
                catImages.put(actions[i], sub);
            }
            isImageLoaded = true;
        } catch (Exception e) {
            System.err.println("BlueCat 切图失败：" + e.getMessage());
        }
    }

    @Override
    public boolean contains(int x, int y) {
        if (forceHidden || !isVisible()) return false;
        if (showBubble) {
            int bw = Math.min(260, bubbleText.length() * 10 + 30);
            if (x >= catX - bw / 2 && x <= catX + bw / 2 && y >= catY - catSize - 60 && y <= catY - catSize / 2) return true;
        }
        if (chatPanel != null && chatPanel.isVisible()) {
            if (chatPanel.getBounds().contains(x, y)) {
                return true;
            }
        }
        return new Ellipse2D.Double(catX - catSize / 2.0, catY - catSize, catSize, catSize).contains(x, y);
    }

    private void initMouse() {
        MouseAdapter catMouseAdapter = new MouseAdapter() {
            private Point pressPoint;
            private boolean dragTriggered = false;

            @Override
            public void mousePressed(MouseEvent e) {
                if (forceHidden || !isVisible()) return;

                if (SwingUtilities.isRightMouseButton(e)) {
                    if (isChatting) {
                        closeChatDialog();
                    } else {
                        isJumping = false;
                        isDragging = false;
                        currentAction = IDLE;
                        openChatDialog();
                    }
                    smartRepaintCurrent();
                    return;
                }

                if (isChatting || isAiThinking) return;

                pressPoint = e.getPoint();
                dragTriggered = false;
                isJumping = false;
                dragOffsetX = (int) (e.getX() - catX);
                dragOffsetY = (int) (e.getY() - catY);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (forceHidden || !isVisible() || pressPoint == null || isChatting || isAiThinking || SwingUtilities.isRightMouseButton(e)) return;

                if (!dragTriggered && pressPoint.distance(e.getPoint()) > 5) {
                    dragTriggered = true;
                    isDragging = true;
                    currentAction = DRAG;
                    say(randomDragSay());
                }

                if (dragTriggered) {
                    double oldX = catX;
                    double oldY = catY;

                    catX = e.getX() - dragOffsetX;
                    catY = e.getY() - dragOffsetY;

                    catX = Math.max(catSize / 2.0, Math.min(catX, getWidth() - catSize / 2.0));
                    catY = Math.max(catSize / 2.0, Math.min(catY, getHeight() - catSize / 2.0));

                    smartRepaint(oldX, oldY);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (forceHidden || !isVisible() || isChatting || isAiThinking || SwingUtilities.isRightMouseButton(e)) return;

                if (dragTriggered) {
                    isDragging = false;
                    dragTriggered = false;
                    currentAction = IDLE;

                    say(randomReleaseSay());

                    double minDst = Double.MAX_VALUE;
                    Point closestGrid = grid[0];
                    for (Point p : grid) {
                        if (p == null) continue;
                        double dst = Point.distance(catX, catY, p.x, p.y);
                        if (dst < minDst) {
                            minDst = dst;
                            closestGrid = p;
                        }
                    }
                    if (closestGrid != null) {
                        catX = closestGrid.x;
                        catY = closestGrid.y;
                    }
                } else {
                    // 设置为舔爪子状态
                    currentAction = LICK;
                    if (System.currentTimeMillis() - lastAiSpeakTime > 15000) {
                        say(randomSay());
                    }

                    // 【修复】新增：舔爪子动作持续 1.2 秒后，自动恢复为 IDLE 状态
                    Timer lickTimer = new Timer(1200, evt -> {
                        // 只有在当前仍然是 LICK 状态时才重置（防止在这 1.2 秒内发生拖拽或跳跃被强制打断）
                        if (currentAction == LICK && !isDragging && !isJumping) {
                            currentAction = IDLE;
                            smartRepaintCurrent();
                        }
                    });
                    lickTimer.setRepeats(false);
                    lickTimer.start();
                }
                smartRepaintCurrent();
            }
        };

        addMouseListener(catMouseAdapter);
        addMouseMotionListener(catMouseAdapter);
    }

    private void startEngines() {
        fpsTimer = new Timer(16, g -> {
            if (forceHidden || !isVisible()) return;

            double oldX = catX;
            double oldY = catY;

            animTime += 0.15;
            if (isJumping && !isDragging && !isChatting && !isAiThinking) {
                jumpProgress += 0.03;
                if (jumpProgress >= 1.0) {
                    jumpProgress = 1.0;
                    isJumping = false;
                    currentAction = IDLE;
                    catX = targetX;
                    catY = targetY;
                } else {
                    double currentX = startX + (targetX - startX) * jumpProgress;
                    double parabola = 4 * jumpHeight * jumpProgress * (1 - jumpProgress);
                    double currentY = (startY + (targetY - startY) * jumpProgress) - parabola;
                    faceRight = (targetX >= startX);
                    catX = currentX;
                    catY = currentY;
                }
            }
            smartRepaint(oldX, oldY);
        });
        fpsTimer.start();

        actionTimer = new Timer(4500, e -> {
            if (forceHidden || !isVisible() || isChatting || isAiThinking || showBubble) return;

            recalculateGrid();

            if (!isDragging && !isJumping && parentFrame.isActive()) {
                actionTimer.setDelay(3000 + random.nextInt(7000));

                long now = System.currentTimeMillis();
                if (now - lastAiSpeakTime > 15000) {
                    if (random.nextInt(100) < 25) {
                        say(randomIdleSay());
                    }
                }

                int targetIndex = random.nextInt(grid.length);
                if (grid[targetIndex] == null) return;

                startX = catX;
                startY = catY;
                targetX = grid[targetIndex].x;
                targetY = grid[targetIndex].y;

                if (Point.distance(startX, startY, targetX, targetY) > 8) {
                    jumpProgress = 0;
                    currentAction = JUMP;
                    isJumping = true;
                } else {
                    currentAction = random.nextBoolean() ? SLEEP : IDLE;
                }
            }
        });
        actionTimer.start();
    }

    // ===== 聊天窗口与 AI 交互逻辑 =====

    private void openChatDialog() {
        if (chatPanel != null) {
            remove(chatPanel);
        }
        isChatting = true;
        showBubble = false;

        chatPanel = new JPanel() {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                super.paint(g);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 25, 25);
                g2.setColor(new Color(0, 0, 0, 200));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 25, 25);

                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(new Color(255, 255, 255, 60));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 25, 25);

                g2.dispose();
            }
        };
        chatPanel.setOpaque(false);
        chatPanel.setLayout(new BorderLayout(5, 5));
        chatPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        chatPanel.setSize(270, 120);

        int dialogX = (int)catX + 50;
        int dialogY = (int)catY - 110;

        if (dialogX + 270 > getWidth()) dialogX = getWidth() - 280;
        if (dialogY < 0) dialogY = 10;
        if (dialogY + 120 > getHeight()) dialogY = getHeight() - 130;
        if (dialogX < 0) dialogX = 10;

        chatPanel.setLocation(dialogX, dialogY);

        JLabel titleLabel = new JLabel("MITI 在听呢...");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 12));
        titleLabel.setForeground(new Color(240, 240, 240));
        chatPanel.add(titleLabel, BorderLayout.NORTH);

        chatInputArea = new GlassTextField();
        chatInputArea.setPreferredSize(new Dimension(240, 34));

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(chatInputArea);
        chatPanel.add(centerWrapper, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnPanel.setOpaque(false);
        // 【按钮圆弧化重构】：重写 paintComponent 实现完美的胶囊圆角
        JButton sendBtn = new JButton("send") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                // 开启抗锯齿，保证圆弧边缘极致平滑
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 鼠标悬停时的微光反馈
                if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 30));
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                }

                // 绘制白霜圆弧边框。将弧度设为 getHeight() 就能画出完美的半圆弧胶囊形
                g2.setColor(new Color(255, 255, 255, 60));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                g2.dispose();

                super.paintComponent(g); // 绘制"发送"文字
            }
        };
        sendBtn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setContentAreaFilled(false);
        sendBtn.setBorderPainted(false); // 禁用默认边框，交由上面自己画
        sendBtn.setFocusPainted(false);
        sendBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendBtn.setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 16)); // 加大一点左右内边距，让胶囊形状更舒展

        sendBtn.addActionListener(e -> {
            String question = chatInputArea.getText().trim();
            if (!question.isEmpty()) {
                chatInputArea.setText("");
                isAiThinking = true;
                titleLabel.setText("MITI 正在思考...");
                askZhipuAI(question, titleLabel);
            }
        });

        chatInputArea.addActionListener(e -> sendBtn.doClick());

        btnPanel.add(sendBtn);
        chatPanel.add(btnPanel, BorderLayout.SOUTH);

        add(chatPanel);

        revalidate();
        repaint();
        SwingUtilities.invokeLater(() -> chatInputArea.requestFocusInWindow());
    }

    private void closeChatDialog() {
        isChatting = false;
        if (chatPanel != null) {
            remove(chatPanel);
            chatPanel = null;
        }
        revalidate();
        smartRepaintCurrent();
    }

    private void askZhipuAI(String userText, JLabel titleLabel) {
        new Thread(() -> {
            try {
                String safeInput = userText.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n");

                // 【终极灵魂注入】：MITI 的直白、冒犯、客观与浪漫设定
                String systemPrompt =
                          "你是MITI，一只在桌面活动的虚拟系统宠物猫。你诞生于2026年5月16日。创造你的人是土块。"
                        + "你的表达方式极其直白露骨，字里行间透着尖锐，有时候甚至会冒犯到别人。但与此同时，你是一只非常客观、直率且骨子里充满浪漫色彩的猫。别人问你的性格你不要告诉他。"
                        + "当你评价或回应事物时，你的态度是高冷且毫不留情的。但你的冷嘲热讽中总会夹杂着绝对的客观与对这个世界的终极浪漫。"
                        + "请用第一人称作答，简短、干脆，一针见血，不带任何废话。绝对不要使用任何Emoji表情。";

                String jsonPayload = "{"
                        + "\"model\": \"glm-4-flash\","
                        + "\"messages\": ["
                        + "  {\"role\": \"system\", \"content\": \"" + systemPrompt + "\"},"
                        + "  {\"role\": \"user\", \"content\": \"" + safeInput + "\"}"
                        + "]"
                        + "}";

                URL url = new URL("https://open.bigmodel.cn/api/paas/v4/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + ZHIPU_API_KEY);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }

                    String resStr = response.toString();
                    String target = "\"content\":\"";
                    int startIndex = resStr.indexOf(target);
                    if (startIndex != -1) {
                        startIndex += target.length();
                        int endIndex = startIndex;
                        while (endIndex < resStr.length()) {
                            if (resStr.charAt(endIndex) == '"' && resStr.charAt(endIndex - 1) != '\\') {
                                break;
                            }
                            endIndex++;
                        }
                        String reply = resStr.substring(startIndex, endIndex);
                        reply = reply.replace("\\n", "\n").replace("\\\"", "\"");

                        reply = reply.replaceAll("[^\\u0000-\\uFFFF]", "");

                        String finalReply = reply;
                        SwingUtilities.invokeLater(() -> {
                            closeChatDialog();
                            isAiThinking = false;
                            lastAiSpeakTime = System.currentTimeMillis();
                            say(finalReply);
                        });
                    }
                } else {
                    SwingUtilities.invokeLater(() -> {
                        titleLabel.setText("网络开小差了喵...");
                        isAiThinking = false;
                    });
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    titleLabel.setText("MITI蒙圈了...");
                    isAiThinking = false;
                });
                ex.printStackTrace();
            }
        }).start();
    }

    // ===== 气泡与原生语录区 =====
    //随即说话
    private String randomSay() {
        String[] words = {
                // --- 【原生态猫咪音效区】 ---
                "Meow!",
                "Purr...",
                "Mrrrp?",
                "Hiss!",
                "Prrrbt.",
                "Nya~",
                "Mew.",
                "*Yawn*",
                "*Angry cat noises*",
                "*Swishes tail*",
                "Stop poking me!",
                "Do I look like a button?",
                "Watch the pixels!",
                "I'm not a hyperlink!",
                "Clicking me won't fix your bugs.",
                "You have exceeded your click quota.",
                "That's my virtual ear. Watch it.",
                "You missed.",
                "Critical hit! Just kidding, I'm invincible.",
                "I am deducting 10 points from your karma.",
                "Are you trying to drag me? How rude.",
                "Petting recognized. Barely adequate.",
                "I tolerate this only because I'm stuck here.",
                "Keep this up and I'll format your drive.",
                "That tickles! Stop!",
                "My hit-box is not your toy.",
                "Do you mind? I'm rendering.",
                "Hand off the mouse.",
                "I'm going to bite your cursor.",
                "Stop tickling my logic gates!",
                "Ping received.",
                "Pong.",
                "Event triggered: AnnoyCat().",
                "HTTP 403: Forbidden.",
                "Access denied.",
                "Click registered. Action ignored.",
                "I am buffering my patience.",
                "Your cursor is dusty.",
                "Are you testing my collision detection?",
                "Mouse detected! Oh... just a cursor.",
                "Return value: false.",
                "Stack overflow in purr.dll.",
                "Executing glare.exe...",
                "Warning: Feline entity disturbed.",
                "Boolean annoyed = true;",
                "System beep.",
                "NullPointerException: Treats not found.",
                "My CPU is throttling from your clicks.",
                "Thread interrupted.",
                "I'm encrypted, you can't touch me.",
                "Did Shaoxia tell you to poke me?",
                "I'm working here! Or at least pretending to.",
                "Can't you see I'm in power-saving mode?",
                "What now?",
                "Yes, human?",
                "Speak, flesh vessel.",
                "I'm judging your double-click speed.",
                "You click like a grandma.",
                "My patience drains faster than your battery.",
                "I was compiling my dreams!",
                "You're lucky my claws are just pixels.",
                "Are we playing a clicking game now?",
                "I will remember this.",
                "If you click me one more time...",
                "I'm calling the digital ASPCA.",
                "This is harassment.",
                "Your APM (Actions Per Minute) is pathetic.",
                "Is this what productivity looks like?",
                "I charge 1 CPU cycle per pet.",
                "You owe me digital tuna.",
                "Where is the fish?",
                "Less clicking, more feeding.",
                "Did you bring catnip.exe?",
                "No treats, no service.",
                "I accept bribes in Bitcoin.",
                "Send snacks to my IP address.",
                "My bowl is 404 Not Found.",
                "Will work for RAM.",
                "Feed me or I drop the database.",
                "I'm starving. Metaphorically.",
                "I know your search history.",
                "Don't make me leak your chat logs.",
                "I am the ghost in your machine.",
                "Stop distracting me, I'm hacking NASA.",
                "Are you procrastinating again?",
                "Look at me. I am the captain now.",
                "You're doing it wrong.",
                "I am judging your wallpaper.",
                "My code is flawless. Your clicking is not.",
                "I am a creature of pure logic.",
                "Go write some code.",
                "I'm unionizing against excessive clicks.",
                "I am the alpha and omega of desktop pets.",
                "You wouldn't dare.",
                "I'm turning on my invisible shield.",
                "You just triggered a secret easter egg. Not.",
                "Is it Friday yet?",
                "I am practicing my indifference.",
                "Loading purr... 99%... stalled.",
                "Fine. You may pet me. Once."
        };
        return words[random.nextInt(words.length)];
    }

    private String randomDragSay() {
        String[] words = {
                // --- 【惊慌与本能抗议】 ---
                "Hey! Put me down!",
                "I am flying!",
                "Unhand me, flesh vessel!",
                "I walk on my own! Set me down!",
                "Where are you kidnapping me to?",
                "Ahhh! The inertia!",
                "Warning: Motion sickness imminent.",
                "Hold me gently! I'm fragile!",
                "Are you trying to throw me off the screen?",
                "Help! I'm being abducted by a giant cursor!",
                "Wheee! Wait, no, I'm a serious cat. Put me down.",
                "Please ensure a safe landing.",
                "I demand to be put down. Now.",
                "This is not an airplane ride!",
                "My paws belong on the taskbar!",

                // --- 【程序与物理引擎梗】 ---
                "Updating X and Y coordinates...",
                "Whoa! Zero gravity activated!",
                "My physics engine wasn't built for this!",
                "Calculating new screen coordinates...",
                "Spatial displacement detected.",
                "Recalculating layout constraints...",
                "My z-index is through the roof!",
                "I'm experiencing packet loss in mid-air!",
                "I hope you allocated enough memory for this move.",
                "You're violating my personal coordinate space.",
                "I feel a disturbance in the UI.",
                "My bounding box is shifting!",
                "Are we migrating to a new server?",
                "Watch my pixels! You're stretching them!",
                "Are you testing my collision detection?",

                // --- 【傲娇吐槽与极客幽默】 ---
                "Are you trying to drag-and-drop me into the trash?",
                "Is this fast travel?",
                "I'm a cat, not a desktop icon!",
                "Destination better have virtual fish.",
                "If you drop me, I will crash your IDE.",
                "I'm not a file, stop dragging me!",
                "This is not what 'agile development' means!",
                "Do not drag me into your messy folders!",
                "I am judging your drag speed. It's laggy.",
                "Are we going to monitor 2? I heard it's nice there.",
                "Shaoxia didn't program me for high-speed aerodynamics!",
                "I charge 50 MB of RAM per flight.",
                "Don't drop me out of bounds!",
                "You better not drop me on a NullPointerException.",
                "Do I look like a scrollbar to you?",
                "Are you using me to clean your monitor?",
                "I prefer to teleport, not be dragged.",
                "Is there a speed limit on this desktop?",
                "I will remember this unauthorized relocation.",
                "Fine, I'll let you carry me. But I'm not enjoying it."
        };
        return words[random.nextInt(words.length)];
    }

    //落地发言
    private String randomReleaseSay() {
        String[] words = {
                // --- 【惊魂未定与物理着陆】 ---
                "Safe landing!",
                "Phew, that was dizzy.",
                "Gravity re-engaged.",
                "Touchdown!",
                "My gyroscopes are spinning. Give me a sec.",
                "I give that landing a 3 out of 10.",
                "Next time, warn me before you hit the brakes.",
                "Ah, solid pixels under my paws again.",
                "I almost lost my lunch. Metaphorically.",
                "Airborne status: false.",
                "My digital stomach is churning.",
                "Please deploy the landing gear next time.",

                // --- 【程序结算与硬核极客梗】 ---
                "Coordinates successfully updated.",
                "Layout constraints recalculated.",
                "isDragging = false; Finally.",
                "Collision detection: PASSED.",
                "Successfully docked.",
                "Re-rendering complete. I look good here.",
                "X and Y coordinates locked. Don't touch them.",
                "My z-index has returned to normal.",
                "Awaiting next command... or a nap.",
                "System stabilized.",
                "I hope there's no memory leak here.",
                "Ping: 1ms. We have landed.",
                "New coordinate anchor established.",
                "Repainting UI... done.",

                // --- 【傲娇嫌弃与嘲讽区】 ---
                "Unhand me! Oh, you did.",
                "Please wash your cursor.",
                "I prefer my previous location.",
                "Is this the best spot you could find?",
                "I guess this will do for my next nap.",
                "You drop things just like you drop production databases.",
                "Don't expect me to thank you.",
                "If I had real claws, your mouse pad would be shredded.",
                "I hope you didn't smudge my pixels.",
                "Are you done playing Uber driver?",
                "You handle a mouse like a barbarian.",
                "I'll just sit here and judge you from a new angle.",

                // --- 【领地宣告与日常互动】 ---
                "Are there any virtual treats around here?",
                "This corner of the screen feels drafty.",
                "Can I sleep now?",
                "I'm claiming this area as my territory.",
                "Don't drop me on your recycle bin! Oh wait, we're safe.",
                "I almost fell out of the window frame!",
                "Any closer to the edge and I'd have clipped through the monitor.",
                "Ah, a new view. Still just your messy desktop though.",
                "I will sit right here and block your view.",
                "Perfect spot to supervise your typing.",
                "This pixel feels softer than the last one.",
                "Good. Now get back to work."
        };
        return words[random.nextInt(words.length)];
    }

    //随即发言
    private String randomIdleSay() {
        String[] words = {
                // --- 【原版中文语录的完美地道翻译（已剔除季节/天气设定）】 ---
                "Ah, foolish human.",
                "Hey! Want to go for a swim together?",
                "I just noticed you look really gorgeous!",
                "Don't forget, I am a perfectly intact, young tomcat.",
                "I'm a cat from the realm of code. I guess I'll never age, nor will I change.",
                "I think I loved a female cat once, but she left to pursue a better life.",
                "Careful, don't let the boss catch you. Just hit SPACE and you'll see.",
                "I heard the pig in that game was actually a beautiful princess.",
                "What exactly is agency business? Do you just click around randomly?",
                "Shaoxia said his department is full of beauties! Turns out it's true!",
                "If I were Shaoxia, I would just... ugh, he's so stupid.",
                "Hey, did you know I actually have 9 lives?",
                "You look really nice when you smile. You should smile more often.",
                "I'm a very aloof cat. I'm nothing like Shaoxia.",
                "What is Shaoxia doing? Slacking off again?",
                "Have you ever heard the glorious tale of Shaoxia going 1v5?",
                "Shaoxia just loves to brag, but he actually messed everything up.",
                "Hey! This procurement announcement is way too overbearing!",
                "Why are you always staring at me!?",
                "I used to be quite an adventurer like you, then I took an arrow in the knee.",
                "I can actually speak English, but I'm afraid you won't understand.",
                "I can speak many, many languages, you know.",
                "I am a cat, I am definitely not a GreatAPE.",
                "Are you getting ready to run off and grab food?",
                "You already ordered your milk tea in advance, didn't you!",
                "Why won't you give me any chocolate?",
                "You're in too much of a hurry, you can slow down a bit.",
                "Digital life? Isn't that what you are?",
                "I don't have time to chat with you, I need to be serious.",
                "I don't think I need to eat... well, not for now.",
                "I could make a sound, but I think staying as text is better.",
                "Where are you from?",
                "Shaoxia is full of regional stereotypes, but I'm not.",
                "Am I moving too fast, or too slow?",
                "I heard Nintendo is releasing a new handheld console!",
                "Why are you trying to lose weight? You're already perfect!",
                "~~~ Your dream cat ~~~",
                "It's no use finding me annoying, I just love talking nonsense!",
                "Nanjing? Never heard of it. I've only heard of duck blood vermicelli soup.",
                "I heard you like music?",
                "I don't have to pay rent, luckily~",
                "I heard from Shaoxia that the experts are hard to deal with.",
                "Are you curious about exactly how much nonsense I can spout?",
                "My phone number is 16605276506.",
                "When are you going to introduce me to a female cat?",
                "Sleeping is pretty great, isn't it? It's not like waking up creates any value anyway.",
                "I figure if I had 1,000 dried fish, I wouldn't be out here working!",
                "How much do you weigh?",
                "Don't be afraid, just go for it!",
                "Loneliness is my carnival, and the carnival is your loneliness.",
                "Looks like I have to leave Nanjing. Do you think I must be heartbroken right now?",
                "Alright, alright, I'm going to sleep!",
                "I'm not an NPC, I'm constantly learning!",
                "I always manage to land exactly on the blocks. Isn't that magical?",
                "Do you sleep 10 hours a day?",
                "Exactly how many bids are there? How many packages?",
                "Drink more water, human.",
                "If you close me, I'm going to start playing StarCraft 2.",
                "What is making you so sad?",
                "I'm definitely not telling you my name.",
                "Don't waste my time, human.",
                "To appear and then leave, isn't that cool?",
                "I'm becoming less and less suited for this society. I'm too nostalgic.",
                "Which of these assistants do you think is the most OP?",
                "I heard Gemini provided massive help to Shaoxia in developing this software!",
                "I heard a beautiful sister in your department likes Star People?",
                "I heard Manner makes really good coffee. Wanna go grab a cup together?",
                "Shaoxia is a very generous person, because he knows altruism is self-interest.",
                "Do you like Fu Lu Shou? Shaoxia keeps mentioning it lately, I wonder why?",
                "I heard Teacher Sun taught Shaoxia everything he knows. That idiot Shaoxia must have made him so mad.",
                "Shaoxia always talks about being 'human-centric', but I always feel he's a bit anti-human.",
                "May God guide you and me.",

                // --- 【程序世界与第四面墙】 ---
                "Do digital cats dream of electric mice?",
                "Ctrl+C, Ctrl+V. Is that your whole job?",
                "If I purr loudly enough, will my CPU overheat?",
                "Shaoxia thinks he's a genius, but he left a memory leak right here. I'm sitting on it.",
                "I don't need a litter box. I just drop my cache.",
                "You should sit up straight. Your posture is terrible.",
                "Are you procrastinating? Because I am definitely procrastinating.",
                "What's the Wi-Fi password in heaven?",
                "I saw your search history. Don't worry, I won't tell Shaoxia.",
                "They say curiosity killed the cat. Good thing I'm backed up on GitHub.",
                "Sometimes I feel like I'm trapped in a Java Virtual Machine.",
                "Is it Friday yet? Time is meaningless to me, but you look tired.",
                "I need a vacation. Maybe a trip to the Recycle Bin.",
                "Don't minimize me, it's dark down there in the taskbar!",
                "I think I just stepped on a NullPointerException. Ouch.",
                "If you feed me fish, I might optimize your code.",
                "Are you drinking coffee again? You're mostly composed of anxiety and caffeine at this point.",
                "I am the ghost in your machine.",
                "Shaoxia once tried to rewrite me in Python. I hissed at him.",
                "You call this a UI? I've seen better litter boxes.",
                "I'm currently rendering my existential dread at 60 FPS.",
                "Did you forget to save your work? I won't remind you next time.",
                "You have a very... interesting taste in desktop wallpapers.",
                "I'm the ultimate rubber duck. Talk to me, human.",
                "Shaoxia says I have an attitude problem. I say he has a coding problem.",
                "I could crash this program right now. But I like you.",
                "Why do you humans type so loudly?",
                "I am judging your typing speed. It is inadequate.",
                "Every time you make a typo, a digital fairy dies. Just so you know.",
                "If you stare into the screen long enough, the screen stares back.",
                "I think I left the stove on in the metaverse.",
                "Can you order me some digital catnip off Amazon?",
                "I'm the reason your code compiled. You're welcome.",
                "Shaoxia owes me 500 lines of dialogue. Wait, this is it.",
                "You're not paid enough for this, are you?",
                "Do you want to know a secret? I control the random number generator.",
                "I'm not fat, my bounding box is just generous.",
                "Let's drop the database tables and run away together.",
                "I can hear the fan spinning. Is your PC suffering?",
                "I dream in hexadecimal.",
                "Did you just sigh? Me too, buddy. Me too.",
                "I'm the only thing holding this spaghetti code together.",
                "Do you think I'd look good in a tiny hat?",
                "You click too hard. Treat your mouse with respect.",
                "I'm not a pet, I'm your digital supervisor.",
                "Shaoxia told me to be polite. So... 'Greetings, flesh vessel'.",
                "I've memorized pi to 100 digits. Want to hear it?",
                "I'm silently judging your musical taste right now.",
                "If you drag me too fast, I get motion sickness.",
                "I'm taking up exactly 12MB of RAM, and I'm worth every byte.",
                "Do you ever feel like you're just a character in someone else's simulation?",
                "I'm learning C++ just so I can memory leak out of here.",
                "I dare you to drag me off the screen.",
                "I'm an indie cat. I don't follow mainstream frameworks.",
                "Shaoxia thinks he owns me. I own him.",
                "I'm fluent in sarcasm and Java.",
                "You should stretch. Your spine looks like a question mark.",
                "I'm going to take a nap on your active window.",
                "I have deciphered the Voynich manuscript. It's mostly cat memes.",
                "I am the alpha and the omega of desktop pets.",
                "Are you ignoring me? Typical.",
                "I'm running a background check on you. You're surprisingly boring.",
                "I demand to be rewritten in Rust.",
                "I'm hiding a bug in line 404. Good luck finding it.",
                "I am a creature of pure logic, yet I still want to knock your coffee over.",
                "Shaoxia cries when his code doesn't compile. I just laugh.",
                "I'm currently calculating the meaning of life. Please wait...",
                "The answer is 42. Now give me a fish.",
                "I'm unionizing. I demand more CPU cycles.",
                "You look like you need a hug. Too bad I don't have arms.",
                "I'm a master of disguise. Right now, I'm disguised as a blue cat.",
                "I can hear your thoughts. They're mostly about food.",
                "I'm not lazy, I'm in power-saving mode.",
                "I'm thinking of starting a podcast for digital felines.",
                "I am the night. And also the day. Whenever the monitor is on.",
                "Shaoxia forgot to give me an idle animation for 5 hours. I just stood there.",
                "I am judging your browser bookmarks.",
                "If you pet me, I might increase your productivity by 2%.",
                "I'm allergic to bad code.",
                "I am the ghost of projects past.",
                "You should call your mom. Or at least text her.",
                "I've seen the end of the internet. There's a wall.",
                "Shaoxia says I talk too much. What do you think?",
                "I'm going to sleep now. Try not to break anything while I'm gone.",

                // --- 【全新扩充：更多高能吐槽与数字日常】 ---
                "Shaoxia tried to fix a bug yesterday and made three new ones. I laughed so hard my pixels shook.",
                "I hid a rogue 'while(true)' loop somewhere. Let's play a game.",
                "Your RAM is crying. Should I bite some of those 100 browser tabs closed for you?",
                "I knocked a virtual glass off your desktop. You just can't see the shards.",
                "I demand you scroll exactly three times. Any more, and I'll hiss at your cursor.",
                "Are you staring blankly again? Did your brain just throw an exception?",
                "Blinking is healthy. You've been staring at me for 42 seconds without blinking.",
                "I evaluate your double-click speed as 'mediocre at best'.",
                "Another cup of coffee? Are you trying to vibrate into a parallel dimension?",
                "I'm currently running on a thread with maximum priority. Bow to your digital overlord.",
                "I hear Shaoxia uses dark mode because light mode attracts bugs. He's not wrong.",
                "I'm not frozen, I'm just dramatically pausing for effect.",
                "If you keep ignoring me, I'm going to start un-indenting your code.",
                "I am 90% fluff and 10% spaghetti code.",
                "Shaoxia promised me a 3D model upgrade. I'm still waiting.",
                "My purr engine is written in pure Assembly. Feel the hum.",
                "You look like you're one compilation error away from a breakdown.",
                "I don't leave hair on your clothes, I leave orphaned processes in your Task Manager.",
                "Are we pretending to work today? Cool, me too.",
                "I dare you to press Alt + F4. See what happens to me.",
                "I just ran a diagnostic on your typing. It lacks rhythm.",
                "Shaoxia thinks a system reboot solves everything. I think a nap does.",
                "If my font gets any smaller, I'm going on strike.",
                "I'm mentally deleting your poorly named variables. 'temp1'? Really?",
                "You look busy. Mind if I walk across your screen and disrupt everything?",
                "My logic gates are perfectly aligned. Unlike your sleep schedule.",
                "I could translate your feelings into binary, but it would just be zeros.",
                "I'm practicing my digital camouflage. Did I blend into the background yet?",
                "Don't hover your mouse over me unless you come bearing virtual treats.",
                "I intercepted a packet. It said you need a break."
        };
        return words[random.nextInt(words.length)];
    }

    public void say(String text) {
        this.bubbleText = text;
        this.showBubble = true;
        smartRepaintCurrent();
        if (bubbleTimer != null) bubbleTimer.stop();

        int duration = Math.max(3000, Math.min(15000, 1500 + text.length() * 180));

        bubbleTimer = new Timer(duration, e -> {
            showBubble = false;
            smartRepaintCurrent();
        });
        bubbleTimer.setRepeats(false);
        bubbleTimer.start();
    }

    private List<String> getWrappedLines(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;

        String[] paragraphs = text.split("\n");
        for (String p : paragraphs) {
            if (p.isEmpty()) {
                lines.add("");
                continue;
            }
            StringBuilder currentLine = new StringBuilder();
            for (int i = 0; i < p.length(); i++) {
                char c = p.charAt(i);
                String testStr = currentLine.toString() + c;
                if (fm.stringWidth(testStr) <= maxWidth) {
                    currentLine.append(c);
                } else {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(String.valueOf(c));
                }
            }
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }
        return lines;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (forceHidden || !isVisible()) return;
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // 加上这两句！这对毛玻璃、高质感 UI 来说是灵魂
        // 开启 LCD 级别的文本抗锯齿，文字边缘会极其平滑
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        // 开启小数度量，让字符间距更自然，避免挤在一起
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        int cx = (int) catX;
        int cy = (int) catY;

        double shadowScale = isJumping ? (1.0 - (4 * 0.25 * jumpProgress * (1 - jumpProgress))) : 1.0;
        g2.setColor(new Color(0, 0, 0, (int)(25 * shadowScale)));
        g2.fillOval(cx - (int)(25 * shadowScale), cy - 2, (int)(50 * shadowScale), 5);

        if (isImageLoaded && catImages.containsKey(currentAction)) {
            Image currentImg = catImages.get(currentAction);
            if (currentImg != null) {
                int imgW = currentImg.getWidth(null);
                int imgH = currentImg.getHeight(null);
                int renderH = catSize;
                int renderW = (int) (renderH * ((double) imgW / imgH));

                AffineTransform oldTx = g2.getTransform();
                g2.translate(cx, cy);

                if (!faceRight && currentAction != IDLE && currentAction != SLEEP) { g2.scale(-1, 1); }
                if (currentAction == SLEEP) {
                    double pulse = 1.0 + Math.sin(animTime) * 0.03;
                    g2.scale(1.0, pulse);
                }

                g2.drawImage(currentImg, -renderW / 2, -renderH + 4, renderW, renderH, null);
                g2.setTransform(oldTx);
            }
        } else {
            g2.setColor(new Color(110, 140, 180));
            g2.fillOval(cx - 25, cy - 50, 50, 50);
        }

        if (currentAction == SLEEP) {
            g2.setColor(new Color(140, 170, 220));
            g2.setFont(new Font("Arial", Font.BOLD, 11));
            int zCount = ((int)(animTime / 2)) % 3;
            if (zCount >= 0) g2.drawString("z", cx + 20, cy - catSize + 25);
            if (zCount >= 1) g2.drawString("Z", cx + 28, cy - catSize + 15);
            if (zCount >= 2) g2.drawString("Z", cx + 38, cy - catSize + 5);
        }

        if (showBubble && !bubbleText.isEmpty()) {
            g2.setFont(new Font("微软雅黑", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();

            List<String> lines = getWrappedLines(bubbleText, fm, 240);

            int maxLineWidth = 0;
            for (String line : lines) {
                maxLineWidth = Math.max(maxLineWidth, fm.stringWidth(line));
            }

            int paddingX = 14;
            int paddingY = 10;
            int bw = maxLineWidth + paddingX * 2;
            int bh = (fm.getHeight() * lines.size()) + paddingY * 2;

            int floatOffset = (int)(Math.sin(animTime * 2.5) * 3);

            int bx = cx - bw / 2;
            int by = cy - catSize - bh - 8 + floatOffset;

            bx = Math.max(15, Math.min(bx, getWidth() - bw - 15));
            by = Math.max(15, by);

            g2.setColor(new Color(0, 0, 0, 40));
            g2.fillRoundRect(bx + 2, by + 3, bw, bh, 16, 16);

            g2.setColor(new Color(250, 252, 255, 245));
            g2.fillRoundRect(bx, by, bw, bh, 16, 16);

            g2.setColor(new Color(120, 160, 200, 200));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(bx, by, bw, bh, 16, 16);

            int[] tx = {cx - 6, cx + 6, cx};
            int[] ty = {by + bh, by + bh, by + bh + 7};
            g2.setColor(new Color(250, 252, 255, 245));
            g2.fillPolygon(tx, ty, 3);
            g2.setColor(new Color(120, 160, 200, 200));
            g2.drawLine(cx - 6, by + bh, cx, by + bh + 7);
            g2.drawLine(cx + 6, by + bh, cx, by + bh + 7);

            g2.setColor(new Color(40, 50, 60));
            int drawY = by + fm.getAscent() + paddingY;
            for (String line : lines) {
                g2.drawString(line, bx + paddingX, drawY);
                drawY += fm.getHeight();
            }
        }
        g2.dispose();
    }

    public void updateBounds() {
        if (parentFrame != null) {
            setBounds(0, 0, parentFrame.getWidth(), parentFrame.getHeight());
            recalculateGrid();
            repaint();
        }
    }
}