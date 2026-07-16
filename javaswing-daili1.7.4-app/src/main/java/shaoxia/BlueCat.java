package shaoxia;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BlueCat extends JComponent {

    // ===== 基础属性 =====
    private double catX = 3000, catY = 3000;   //默认坐标 把他丢出去
    private final int catSize = 90; // 猫咪显示基准高度
    private boolean isDragging = false;
    private boolean isJumping = false;
    private int dragOffsetX, dragOffsetY;
    private double startX, startY, targetX, targetY;
    private double jumpProgress = 0;
    private final double jumpHeight = 110;
    private boolean faceRight = true;

    // 全局强力控制显隐开关
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

    // 对话气泡
    private String bubbleText = "";
    private boolean showBubble = false;
    private Timer bubbleTimer;
    private final Random random = new Random();
    private final JFrame parentFrame;

    // 图片缓存
    private final Map<Integer, Image> catImages = new HashMap<>();
    private boolean isImageLoaded = false;

    // 9个真实的目标落脚网格点
    private final Point[] grid = new Point[9];
    private final ArrayList<JButton> cachedButtons = new ArrayList<>();

    public BlueCat(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        setOpaque(false);
        // 此时获取的宽高可能不准，但无妨，后续 invokeLater 会更新
        setBounds(0, 0, parentFrame.getWidth(), parentFrame.getHeight());

        loadAndSliceSheet();
        initMouse();
        startEngines();

        // ✨ 核心修改：利用 invokeLater 延迟执行初始位置的计算
        // 确保在父组件完全渲染、布局完成之后，再去寻找按钮方块
        SwingUtilities.invokeLater(() -> {
            updateBounds(); // 同步一次最新的全屏边界并重新计算九宫格

            // 将小猫的初始位置强制绑定在第一个方块（或者你指定的其他方块）上
            if (grid[0] != null) {
                catX = grid[0].x;
                catY = grid[0].y;
                repaint();
            }
        });

        if (isImageLoaded) {
            say("Wow! It's so nice to meet you!");
        }
    }

    /**
     * 全局强力控制显隐方法
     */
    public void setCatVisible(boolean visible) {
        this.forceHidden = !visible;
        this.setVisible(visible);
        if (visible) {
            recalculateGrid();
        }
        repaint();
    }

    /**
     * 位置高度保持最满意的完美高度（+18）
     */
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
                java.net.URL url = getClass().getResource("/resources/bluecat_sheet.png");
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
                Image transparentImg = filterWhiteBackground(sub);
                catImages.put(actions[i], transparentImg);
            }
            isImageLoaded = true;
        } catch (Exception e) {
            System.err.println("BlueCat 切图失败：" + e.getMessage());
        }
    }

    private Image filterWhiteBackground(BufferedImage src) {
        int w = src.getWidth(); int h = src.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF; int g = (rgb >> 8) & 0xFF; int b = rgb & 0xFF;
                if (r > 240 && g > 240 && b > 240) { result.setRGB(x, y, 0x00FFFFFF); }
                else { result.setRGB(x, y, rgb); }
            }
        }
        return result;
    }

    @Override
    public boolean contains(int x, int y) {
        if (forceHidden || !isVisible()) return false;
        if (showBubble) {
            int bw = Math.min(220, bubbleText.length() * 10 + 30);
            if (x >= catX - bw / 2 && x <= catX + bw / 2 && y >= catY - catSize - 40 && y <= catY - catSize / 2) return true;
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

                pressPoint = e.getPoint();
                dragTriggered = false;
                isJumping = false;

                dragOffsetX = (int) (e.getX() - catX);
                dragOffsetY = (int) (e.getY() - catY);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (forceHidden || !isVisible() || pressPoint == null) return;

                if (!dragTriggered && pressPoint.distance(e.getPoint()) > 5) {
                    dragTriggered = true;
                    isDragging = true;
                    currentAction = DRAG;
                    say(randomDragSay());
                }

                if (dragTriggered) {
                    catX = e.getX() - dragOffsetX;
                    catY = e.getY() - dragOffsetY;

                    catX = Math.max(catSize / 2.0, Math.min(catX, parentFrame.getWidth() - catSize / 2.0));
                    catY = Math.max(catSize / 2.0, Math.min(catY, parentFrame.getHeight() - catSize / 2.0));
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (forceHidden || !isVisible()) return;

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
                    currentAction = LICK;
                    say(randomSay());
                }
                repaint();
            }
        };

        addMouseListener(catMouseAdapter);
        addMouseMotionListener(catMouseAdapter);
    }

    private void startEngines() {
        fpsTimer = new Timer(16, g -> {
            if (forceHidden || !isVisible()) return;
            animTime += 0.15;
            if (isJumping && !isDragging) {
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
            repaint();
        });
        fpsTimer.start();

        actionTimer = new Timer(4500, e -> {
            if (forceHidden || !isVisible()) return;
            recalculateGrid();

            if (!isDragging && !isJumping && parentFrame.isActive()) {

                // 不点它的时候，也有概率主动唠叨废话
                if (random.nextInt(100) < 35) {
                    say(randomIdleSay());
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

    /**
     * 1. 鼠标单纯点击时说的词
     */
    private String randomSay() {
        String[] words = {


        };
        return words[random.nextInt(words.length)];
    }

    /**
     * 2. 拖拽刚按下瞬间蹦出的随机词
     */
    private String randomDragSay() {
        String[] words = {

        };
        return words[random.nextInt(words.length)];
    }

    /**
     * ✨ 3.【新增词库】：鼠标拖拽释放（松开）时蹦出的随机内容
     */
    private String randomReleaseSay() {
        String[] words = {

        };
        return words[random.nextInt(words.length)];
    }

    /**
     * 4. 闲着不点它时，自己唠叨的日常废话
     */
    /**
     * 4. 闲着不点它时，自己唠叨的英文“废话圣经” (BlueCat's English Bible)
     */
    private String randomIdleSay() {
        String[] words = {
                // --- 【原版中文语录的完美地道翻译】 ---
                "Ah, foolish human.",
                "Hey! Want to go for a swim together?",
                "How's the weather over there? Looks like a thunderstorm is brewing here!",
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
                "Is it summer already? Being a single cat is so lonely!",
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
                "It's another romantic season, isn't it?",
                "I always manage to land exactly on the blocks. Isn't that magical?",
                "Do you sleep 10 hours a day?",
                "Exactly how many bids are there? How many packages?",
                "Drink more water, human.",
                "If you close me, I'm going to start playing StarCraft 2.",
                "What is making you so sad?",
                "Are you blowing the AC directly at yourself?",
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
                "One day I suddenly thought: what if it's autumn where you are, but I only know how to say 'summer is here'?",
                "Shaoxia said I should talk according to the seasons. What a demanding guy!",
                "May God guide you and me.",

                // --- 【独家扩充：程序世界与第四面墙】 ---
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
                "Why don't you take a walk? The sun is allegedly nice.",
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
                "I'm going to sleep now. Try not to break anything while I'm gone."
        };
        return words[random.nextInt(words.length)];
    }

    public void say(String text) {
        this.bubbleText = text; this.showBubble = true; repaint();
        if (bubbleTimer != null) bubbleTimer.stop();
        bubbleTimer = new Timer(5000, e -> { showBubble = false; repaint(); });
        bubbleTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (forceHidden || !isVisible()) return;
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int cx = (int) catX;
        int cy = (int) catY;

        // 1. 地面阴影
        double shadowScale = isJumping ? (1.0 - (4 * 0.25 * jumpProgress * (1 - jumpProgress))) : 1.0;
        g2.setColor(new Color(0, 0, 0, (int)(25 * shadowScale)));
        g2.fillOval(cx - (int)(25 * shadowScale), cy - 2, (int)(50 * shadowScale), 5);

        // 2. 猫咪主体
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

        // 3. 睡觉 Zzz
        if (currentAction == SLEEP) {
            g2.setColor(new Color(140, 170, 220));
            g2.setFont(new Font("Arial", Font.BOLD, 11));
            int zCount = ((int)(animTime / 2)) % 3;
            if (zCount >= 0) g2.drawString("z", cx + 20, cy - catSize + 25);
            if (zCount >= 1) g2.drawString("Z", cx + 28, cy - catSize + 15);
            if (zCount >= 2) g2.drawString("Z", cx + 38, cy - catSize + 5);
        }

        // 4. 对话气泡
        if (showBubble && !bubbleText.isEmpty()) {
            g2.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            FontMetrics fm = g2.getFontMetrics();
            int bw = fm.stringWidth(bubbleText) + 20;
            int bh = 28;
            int bx = cx - bw / 2;
            int by = cy - catSize - bh - 5;
            bx = Math.max(15, Math.min(bx, parentFrame.getWidth() - bw - 15));
            by = Math.max(15, by);
            g2.setColor(new Color(255, 255, 255, 245));
            g2.fillRoundRect(bx, by, bw, bh, 12, 12);
            g2.setColor(new Color(112, 128, 144));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(bx, by, bw, bh, 12, 12);
            int[] tx = {cx - 4, cx + 4, cx}; int[] ty = {by + bh, by + bh, by + bh + 5};
            g2.setColor(new Color(255, 255, 255, 245)); g2.fillPolygon(tx, ty, 3);
            g2.setColor(new Color(50, 50, 50)); g2.drawString(bubbleText, bx + 10, by + 18);
        }
        g2.dispose();
    }

    public void updateBounds() {
        setBounds(0, 0, parentFrame.getWidth(), parentFrame.getHeight());
        recalculateGrid();
        repaint();
    }
}