package shaoxia.modules;

import org.json.JSONArray;
import org.json.JSONObject;
import shaoxia.MainLauncher;
import shaoxia.Utils.ActionButton;
import shaoxia.Utils.GlassPanel;
import shaoxia.Utils.GlassTextField;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 区域与邮编精准解析面板
 * (640x480 黄金比例适配版 + 独立后台网络探针 + 灰/绿断网反馈 + 强制TLS1.2防断网 + 双栏文化扩展 + 原生JSON强解析 + 横板导出)
 */
public class RegionFetcherPanel extends BackgroundPanel {
    private MainLauncher parent;
    private GlassTextField addressInput;

    // 改为双栏文本区
    private JTextPane regionArea;
    private JTextPane cultureArea;

    // 右上角地球仪探针
    private EarthIndicator earthIndicator;
    private boolean isNetworkConnected = true;

    // 专属高德 API Key
    private static final String AMAP_KEY = "6ad43a78eb65ad02c2f222ccdee197e8";

    // 开启 SNI 支持
    static {
        System.setProperty("jsse.enableSNIExtension", "true");
        System.setProperty("https.protocols", "TLSv1.2");
    }

    private Timer debounceTimer;

    public RegionFetcherPanel(MainLauncher parent) {
        super("bg_imgs/月球与地球.png");
        this.parent = parent;
        setLayout(new BorderLayout());
        setOpaque(false);

        initTopBar();
        initCenterArea();
        startNetworkMonitor();
    }

    private SSLContext getBypassSSLContext() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
        };
        SSLContext sc = SSLContext.getInstance("TLSv1.2");
        sc.init(null, trustAllCerts, new SecureRandom());
        return sc;
    }

    private void initTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(10, 25, 0, 15));

        JPanel leftWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        leftWrapper.setOpaque(false);

        ActionButton backBtn = new ActionButton("<< 返回菜单");
        backBtn.setPreferredSize(new Dimension(130, 36));
        backBtn.addActionListener(e -> parent.showMenu());
        leftWrapper.add(backBtn);

        topBar.add(leftWrapper, BorderLayout.WEST);

        earthIndicator = new EarthIndicator();
        topBar.add(earthIndicator, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);
    }

    private void initCenterArea() {
        JPanel centerContainer = new JPanel(new BorderLayout(0, 12));
        centerContainer.setOpaque(false);
        centerContainer.setBorder(new EmptyBorder(10, 25, 20, 25));

        GlassPanel inputWrapper = new GlassPanel(new BorderLayout(10, 0));
        inputWrapper.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel("输入详细地址: ", JLabel.LEFT);
        titleLabel.setForeground(new Color(220, 220, 220));
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 15));

        addressInput = new GlassTextField();
        addressInput.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        addressInput.setPreferredSize(new Dimension(0, 38));

        inputWrapper.add(titleLabel, BorderLayout.WEST);
        inputWrapper.add(addressInput, BorderLayout.CENTER);

        // ▼▼▼ 改造为左右双栏布局 ▼▼▼
        GlassPanel outputWrapper = new GlassPanel(new BorderLayout());
        outputWrapper.setBorder(new EmptyBorder(15, 20, 10, 20));

        JPanel splitPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        splitPanel.setOpaque(false);

        regionArea = createTransparentTextPane("等待输入...");
        cultureArea = createTransparentTextPane("文化与美誉将在此显示...");

        // ✨ 核心黑科技：利用 GridBagLayout 天然的垂直居中特性包裹文本区
        JPanel regionWrapper = new JPanel(new GridBagLayout());
        regionWrapper.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL; // 仅横向填充铺满以支持自动换行，纵向不填充以实现绝对居中
        regionWrapper.add(regionArea, gbc);

        JPanel cultureWrapper = new JPanel(new GridBagLayout());
        cultureWrapper.setOpaque(false);
        cultureWrapper.add(cultureArea, gbc);

        splitPanel.add(regionWrapper);
        splitPanel.add(cultureWrapper);
        outputWrapper.add(splitPanel, BorderLayout.CENTER);
        // ▲▲▲ 改造结束 ▲▲▲

        // 构建底部面板，包含提示语和新按钮
        JPanel bottomWrapper = new JPanel(new BorderLayout(0, 5));
        bottomWrapper.setOpaque(false);

       // JLabel tipLabel = new JLabel("提示：双击左侧复制邮编信息，双击右侧复制文化摘要", JLabel.CENTER);
       // tipLabel.setForeground(new Color(150, 150, 150));
       // tipLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
       // bottomWrapper.add(tipLabel, BorderLayout.NORTH);

        // 新增：横板导出按钮 (使用统一的 ActionButton 样式)
        JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
        exportPanel.setOpaque(false);
        ActionButton exportBtn = new ActionButton("横版导出");
        exportBtn.setPreferredSize(new Dimension(130, 32));
        exportBtn.addActionListener(e -> exportHorizontalTxt());
        exportPanel.add(exportBtn);

        bottomWrapper.add(exportPanel, BorderLayout.CENTER);

        outputWrapper.add(bottomWrapper, BorderLayout.SOUTH);

        centerContainer.add(inputWrapper, BorderLayout.NORTH);
        centerContainer.add(outputWrapper, BorderLayout.CENTER);
        add(centerContainer, BorderLayout.CENTER);

        debounceTimer = new Timer(1000, e -> {
            String address = addressInput.getText().trim();
            if (address.isEmpty()) {
                regionArea.setText("等待输入...");
                cultureArea.setText("文化与美誉将在此显示...");
                return;
            }
            regionArea.setText("正在请求定位...");
            cultureArea.setText("等待区域确认...");
            fetchAdcodeAndParseLocalJson(address);
        });
        debounceTimer.setRepeats(false);

        addressInput.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { debounceTimer.restart(); }
            @Override
            public void removeUpdate(DocumentEvent e) { debounceTimer.restart(); }
            @Override
            public void changedUpdate(DocumentEvent e) { debounceTimer.restart(); }
        });
    }

    private JTextPane createTransparentTextPane(String defaultText) {
        JTextPane pane = new JTextPane() {
            @Override
            public void setText(String t) {
                super.setText(t);
                StyledDocument doc = this.getStyledDocument();
                SimpleAttributeSet center = new SimpleAttributeSet();
                StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
                doc.setParagraphAttributes(0, doc.getLength(), center, false);
            }
        };

        pane.setOpaque(false);
        pane.setBackground(new Color(0, 0, 0, 0));
        pane.setForeground(new Color(240, 240, 240));
        pane.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        pane.setEditable(false);
        pane.setCursor(new Cursor(Cursor.HAND_CURSOR));

        pane.setText(defaultText);

        pane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String text = pane.getText();
                    if (!text.contains("等待") && !text.contains("失败") && !text.contains("正在") && !text.contains("异常")) {
                        String copyText = text.replace("[ 定位与邮编解析成功 ]\n\n", "")
                                .replace("[ 传统文化与美誉 ]\n\n", "").trim();
                        StringSelection selection = new StringSelection(copyText);
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                        JOptionPane.showMessageDialog(RegionFetcherPanel.this, "内容已成功复制到剪贴板！", "提示", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        });
        return pane;
    }

    // ==========================================
    // 导出横板文本功能：自动解析冒号并拼接
    // ==========================================
    private void exportHorizontalTxt() {
        String text = regionArea.getText();

        // 校验：如果当前显示的是等待或错误信息，则不执行导出
        if (text == null || text.trim().isEmpty() || text.contains("等待") || text.contains("失败") || text.contains("正在") || text.contains("异常") || text.contains("未在本地")) {
            JOptionPane.showMessageDialog(this, "当前无有效的解析数据可供导出！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 提取信息逻辑
        StringBuilder resultBuilder = new StringBuilder();
        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("[")) { // 跳过空行和标题行 (如 [ 定位与邮编解析成功 ])
                continue;
            }

            // 按照冒号分割 (支持中文冒号和英文冒号)
            String[] parts = line.split("[:：]");
            if (parts.length > 1) {
                String val = parts[1].trim(); // 获取冒号后面的值
                if (!val.isEmpty()) {
                    if (resultBuilder.length() > 0) {
                        resultBuilder.append("_");
                    }
                    resultBuilder.append(val);
                }
            }
        }

        String finalName = resultBuilder.toString();

        if (finalName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "未能解析到有效数据字段，请检查内容格式！", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 保存文件对话框
        // 直接获取当前用户的桌面路径
        File desktopDir = new File(System.getProperty("user.home"), "Desktop");
        // 如果系统找不到 Desktop 文件夹，也可以考虑加上兼容判断，但一般主流系统都适用
        File fileToSave = new File(desktopDir, finalName + ".txt");

        try (FileWriter fw = new FileWriter(fileToSave, StandardCharsets.UTF_8)) {
            // 将文件名本身写入文本内部
            fw.write(finalName);
            JOptionPane.showMessageDialog(this, "文件已成功直接导出至桌面: \n" + fileToSave.getAbsolutePath(), "导出成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "文件导出至桌面失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==========================================
    // 核心定位逻辑：双重保险机制 (精确查找 -> 省市县降级)
    // ==========================================
    private void fetchAdcodeAndParseLocalJson(String address) {
        new Thread(() -> {
            try {
                // 第一套逻辑：使用完整输入地址进行精准匹配
                String targetAdcode = getAdcodeFromAmap(address, address);

                // 如果第一套逻辑未命中（被乱指或搜不到），启动第二套逻辑：提取省市县前缀补充
                if (targetAdcode == null) {
                    String prefix = extractAdminPrefix(address);
                    // 确保提取到了省市县，且不等于原地址，才进行降级搜索
                    if (!prefix.isEmpty() && !prefix.equals(address)) {
                        SwingUtilities.invokeLater(() -> regionArea.setText("精准匹配未命中，启动省市县降级检索..."));
                        // 第二次尝试：用提取出的省市县去搜，但仍以用户的原地址作为严格比对的底线
                        targetAdcode = getAdcodeFromAmap(prefix, address);
                    }
                }

                // 最终判定
                if (targetAdcode != null) {
                    // 拿到了准确的 adcode，去本地字典调取数据
                    searchLocalAdcodeJson(targetAdcode);
                } else {
                    // 两套逻辑均宣告失败
                    SwingUtilities.invokeLater(() -> regionArea.setText("[失败] 高德地图无法精准匹配该区域，\n请检查地址或尝试仅输入省市县名称"));
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> regionArea.setText("[异常] 定位网络请求发生错误"));
            }
        }).start();
    }

    // ==========================================
    // 底层高德探针：负责发包与弹性身份核实 (2.0升级版)
    // ==========================================
    private String getAdcodeFromAmap(String searchKeyword, String originalAddress) throws Exception {
        String urlStr = "https://restapi.amap.com/v3/geocode/geo?address="
                + URLEncoder.encode(searchKeyword, "UTF-8") + "&key=" + AMAP_KEY;

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        if (conn instanceof HttpsURLConnection) {
            ((HttpsURLConnection) conn).setSSLSocketFactory(getBypassSSLContext().getSocketFactory());
        }

        conn.setRequestMethod("GET");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);

        if (conn.getResponseCode() == 200) {
            Scanner s = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name()).useDelimiter("\\A");
            String json = s.hasNext() ? s.next() : "";
            s.close();

            JSONObject responseObj = new JSONObject(json);
            if ("1".equals(responseObj.optString("status"))) {
                JSONArray geocodes = responseObj.optJSONArray("geocodes");
                if (geocodes != null && geocodes.length() > 0) {
                    for (int i = 0; i < geocodes.length(); i++) {
                        JSONObject gc = geocodes.getJSONObject(i);

                        // 防御机制：清理高德的空数组
                        String prov = gc.optString("province").replace("[]", "");
                        String city = gc.optString("city").replace("[]", "");
                        String dist = gc.optString("district").replace("[]", "");

                        // ✨ 核心修复：提取“核心地名” ✨
                        // 剥离掉死板的行政后缀，防止“苏州工业园区”匹配不上“苏州市”
                        String coreProv = prov.replace("省", "").replace("市", "");
                        String coreCity = city.replace("市", "");
                        String coreDist = dist.replace("区", "").replace("县", "").replace("市", "");

                        // 护城河 2.0：只要核心地名匹配，或者查询的是特殊功能区，直接放行！
                        if ((!coreDist.isEmpty() && originalAddress.contains(coreDist)) ||
                                (!coreCity.isEmpty() && originalAddress.contains(coreCity)) ||
                                (!coreProv.isEmpty() && originalAddress.contains(coreProv)) ||
                                // 特殊功能区宽容兜底：如果地址里带有园区/开发区等字眼，大概率是跨行政区的特殊地带
                                originalAddress.contains("园区") ||
                                originalAddress.contains("开发区") ||
                                originalAddress.contains("新区")) {

                            return gc.optString("adcode");
                        }
                    }
                }
            }
        }
        return null;
    }

    // ==========================================
    // 智能提取工具：从长串地址中剥离【省市县】
    // ==========================================
    private String extractAdminPrefix(String address) {
        // 使用正则匹配连续的中文+行政单位后缀
        Matcher m = Pattern.compile("([\\u4e00-\\u9fa5]+?[省市区县州盟])").matcher(address);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (m.find()) {
            // 确保行政区划是连续书写的，中间不能断开
            if (m.start() == lastEnd) {
                sb.append(m.group());
                lastEnd = m.end();
            } else {
                break; // 遇到干扰词中断提取
            }
        }
        return sb.toString();
    }

    private void searchLocalAdcodeJson(String targetAdcode) {
        try {
            InputStream is = getClass().getResourceAsStream("/regionfetcher/Adcode.json");
            if (is == null) {
                SwingUtilities.invokeLater(() -> regionArea.setText("[严重错误] 缺少 Adcode.json"));
                return;
            }

            Scanner s = new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
            String fullJson = s.hasNext() ? s.next() : "";
            s.close();

            // 由于本地大文件解析如果使用 JSON 库加载全文会很消耗内存，这里依然保留局部正则匹配的方式以确保极速响应
            Matcher m = Pattern.compile("\"code\"\\s*:\\s*\"" + targetAdcode + "\"").matcher(fullJson);

            if (m.find()) {
                int targetIdx = m.start();
                int startIdx = fullJson.lastIndexOf("{", targetIdx);
                int endIdx = fullJson.indexOf("}", targetIdx);

                if (startIdx != -1 && endIdx != -1) {
                    String block = fullJson.substring(startIdx, endIdx + 1);

                    String province = extractJsonField(block, "province");
                    String city = extractJsonField(block, "city");
                    String name = extractJsonField(block, "name");
                    String zipCode = extractJsonField(block, "zipCode");

                    StringBuilder out = new StringBuilder();
                    out.append("[ 定位与邮编解析成功 ]\n\n");
                    out.append("省/直辖市: \t").append(province).append("\n");
                    if (!city.isEmpty() && !city.equals(province)) {
                        out.append("地 级 市: \t").append(city).append("\n");
                    }
                    out.append("区 / 县: \t").append(name).append("\n");
                    out.append("邮政编码: \t").append(zipCode).append("\n");

                    SwingUtilities.invokeLater(() -> {
                        regionArea.setText(out.toString());
                        String fullLocation = province + (city.equals(province) ? "" : city) + name;
                        fetchCultureAndReputation(fullLocation);
                    });
                    return;
                }
            }
            SwingUtilities.invokeLater(() -> regionArea.setText("未在本地字典收录该信息。"));
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> regionArea.setText("[异常] 读取本地字典库失败"));
        }
    }

    /**
     * 异步请求国内免费大模型（智谱 GLM-4-Flash）探寻深度文化底蕴
     */
    private void fetchCultureAndReputation(String location) {
        cultureArea.setText("正在跨越山海，\n探寻属于这里的文化印记...");
        new Thread(() -> {
            try {
                String apiKey = "cc990ca1040c41bbba6e8ac4520fca31.vEwVhAKbdbIjk8KY";

                String urlStr = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                if (conn instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) conn).setSSLSocketFactory(getBypassSSLContext().getSocketFactory());
                }

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(12000);

                // ✨ 核心修改：放宽字数限制，引导模型输出更长的细节
                String prompt = "请详细介绍一下“" + location + "”的独特传统文化、历史底蕴和地方美誉。字数控制在200字到250字左右，内容请尽量丰富、充实且有深度。" +
                        "要求：必须突出该地的具体细节（如特色风俗、历史地标、人文故事或非遗文化等），直接返回介绍的正文，绝不要任何客套话、多余排版或Markdown格式，文本中绝对请勿使用双引号以免JSON解析出错。";

                String jsonInputString = "{\n" +
                        "  \"model\": \"glm-4-flash\",\n" +
                        "  \"messages\": [\n" +
                        "    {\"role\": \"user\", \"content\": \"" + prompt + "\"}\n" +
                        "  ]\n" +
                        "}";

                try (java.io.OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                if (conn.getResponseCode() == 200) {
                    Scanner s = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name()).useDelimiter("\\A");
                    String responseJson = s.hasNext() ? s.next() : "";
                    s.close();

                    try {
                        JSONObject aiResponseObj = new JSONObject(responseJson);
                        JSONArray choices = aiResponseObj.optJSONArray("choices");
                        if (choices != null && choices.length() > 0) {
                            JSONObject messageObj = choices.getJSONObject(0).optJSONObject("message");
                            if (messageObj != null) {
                                String contentExtract = messageObj.optString("content");
                                SwingUtilities.invokeLater(() ->
                                        cultureArea.setText("[ 传统文化与美誉 ]\n\n" + contentExtract)
                                );
                            } else {
                                SwingUtilities.invokeLater(() -> cultureArea.setText("[ 提示 ]\n未找到有效的回答正文。"));
                            }
                        } else {
                            SwingUtilities.invokeLater(() -> cultureArea.setText("[ 提示 ]\n大模型未返回有效选择结果。"));
                        }
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> cultureArea.setText("[ 异常 ]\n大模型返回的数据格式无法解析。"));
                    }

                } else {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            cultureArea.setText("[ 获取失败 ]\n接口访问受限，状态码: " + conn.getResponseCode());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> cultureArea.setText("[ 异常 ]\n探寻过程发生中断: " + e.getMessage()));
            }
        }).start();
    }

    private String extractJsonField(String jsonBlock, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"(.*?)\"").matcher(jsonBlock);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private void startNetworkMonitor() {
        Thread monitorThread = new Thread(() -> {
            while (true) {
                try {
                    URL url = new URL("https://restapi.amap.com");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    if (conn instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) conn).setSSLSocketFactory(getBypassSSLContext().getSocketFactory());
                    }

                    conn.setRequestMethod("HEAD");
                    conn.setConnectTimeout(2000);
                    conn.setReadTimeout(2000);
                    int code = conn.getResponseCode();
                    setNetworkState(code == 200);
                } catch (Exception e) {
                    setNetworkState(false);
                }

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private void setNetworkState(boolean isOk) {
        if (this.isNetworkConnected != isOk) {
            this.isNetworkConnected = isOk;
            if (earthIndicator != null) {
                earthIndicator.repaint();
            }
        }
    }

    class EarthIndicator extends JPanel {
        private double angle = 0;

        public EarthIndicator() {
            setOpaque(false);
            setPreferredSize(new Dimension(60, 60));

            Timer timer = new Timer(50, e -> {
                angle += 0.05;
                if (angle >= Math.PI * 2) angle -= Math.PI * 2;
                repaint();
            });
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2 - 8;
            int r = 16;

            Color glowColor = isNetworkConnected ? new Color(0, 255, 100, 50) : new Color(60, 60, 60, 40);
            Color lineColor = isNetworkConnected ? new Color(0, 220, 50) : new Color(90, 90, 90);

            g2.setColor(glowColor);
            g2.fillOval(cx - r - 4, cy - r - 4, (r + 4) * 2, (r + 4) * 2);

            g2.setColor(lineColor);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(cx - r, cy - r, r * 2, r * 2);

            g2.drawLine(cx - r, cy, cx + r, cy);
            int halfDist = (int) (r * 0.866);
            g2.drawLine(cx - halfDist, cy - r / 2, cx + halfDist, cy - r / 2);
            g2.drawLine(cx - halfDist, cy + r / 2, cx + halfDist, cy + r / 2);

            int sweep1 = (int) (Math.sin(angle) * r);
            int sweep2 = (int) (Math.sin(angle + Math.PI / 2) * r);
            g2.drawOval(cx - Math.abs(sweep1), cy - r, Math.abs(sweep1) * 2, r * 2);
            g2.drawOval(cx - Math.abs(sweep2), cy - r, Math.abs(sweep2) * 2, r * 2);

            g2.setFont(new Font("Arial", Font.BOLD, 10));
            String statusText = isNetworkConnected ? "ON" : "OFF";
            int tw = g2.getFontMetrics().stringWidth(statusText);
            g2.drawString(statusText, cx - tw / 2, cy + r + 15);

            g2.dispose();
        }
    }
}