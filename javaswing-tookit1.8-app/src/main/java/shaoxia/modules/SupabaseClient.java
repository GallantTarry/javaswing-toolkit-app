package shaoxia.modules;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
//supabase客户端接口
public class SupabaseClient {

    // 你的 Project URL
    private static final String SUPABASE_URL = "https://xhqcaollhziqplsentlk.supabase.co/rest/v1/dinogame";

    // 你的 anon public API key
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhocWNhb2xsaHppcXBsc2VudGxrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODI4MTc4OTAsImV4cCI6MjA5ODM5Mzg5MH0.KNtJGpO85mpLYUucAAn6d3ksNxQ3EBR9FVJmJiFBELU";

    // 数据结构：用于存储排行榜的一行记录
    public static class ScoreRecord {
        public String playerName;
        public long score;
        public String createdAt;

        public ScoreRecord(String playerName, long score, String createdAt) {
            this.playerName = playerName;
            this.score = score;
            this.createdAt = createdAt;
        }
    }

    /**
     * 上传分数到数据库
     */
    public static void uploadScore(String playerName, long score) {
        new Thread(() -> {
            try {
                URL url = new URL(SUPABASE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Prefer", "return=minimal");
                conn.setDoOutput(true);

                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                String safeName = playerName.replace("\"", "\\\"");
                String jsonPayload = String.format("{\"player_name\": \"%s\", \"score\": %d}", safeName, score);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                if (code == 201) {
                    System.out.println("数据已成功入库！");
                } else {
                    System.out.println("上传受阻，状态码: " + code);
                    try (Scanner scanner = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8.name())) {
                        System.out.println("错误详情: " + (scanner.hasNext() ? scanner.next() : ""));
                    }
                }
            } catch (Exception e) {
                System.out.println("SupabaseClient 网络异常: " + e.getMessage());
            }
        }).start();
    }

    /**
     * 拉取全球前10名排行榜数据
     */
    public static void fetchTopScores(Consumer<List<ScoreRecord>> callback) {
        new Thread(() -> {
            try {
                // 通过 URL 参数告诉 Supabase：按分数降序，最多拿 10 条
                URL url = new URL(SUPABASE_URL + "?select=player_name,score,created_at&order=score.desc&limit=10");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int code = conn.getResponseCode();
                if (code == 200) {
                    try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
                        String json = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                        List<ScoreRecord> records = parseJson(json);
                        javax.swing.SwingUtilities.invokeLater(() -> callback.accept(records));
                    }
                } else {
                    System.out.println("拉取排行榜失败，状态码: " + code);
                    javax.swing.SwingUtilities.invokeLater(() -> callback.accept(null));
                }
            } catch (Exception e) {
                System.out.println("获取排行榜网络异常: " + e.getMessage());
                javax.swing.SwingUtilities.invokeLater(() -> callback.accept(null));
            }
        }).start();
    }

    /**
     * 原生极简 JSON 解析，规避依赖报错
     */
    private static List<ScoreRecord> parseJson(String json) {
        List<ScoreRecord> list = new ArrayList<>();
        String[] objects = json.split("}");
        for (String obj : objects) {
            String name = extractString(obj, "\"player_name\"");
            String scoreStr = extractNumber(obj, "\"score\"");
            String date = extractString(obj, "\"created_at\"");

            if (name != null && scoreStr != null && date != null) {
                String cleanDate = date;
                try {
                    // 自动将 Supabase 的 UTC 时间转换为咱们东八区的北京时间
                    OffsetDateTime odt = OffsetDateTime.parse(date);
                    cleanDate = odt.atZoneSameInstant(ZoneId.of("Asia/Shanghai"))
                            .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
                } catch (Exception e) {
                    if (date.length() >= 16) cleanDate = date.substring(5, 10) + " " + date.substring(11, 16);
                }
                try {
                    list.add(new ScoreRecord(name, Long.parseLong(scoreStr), cleanDate));
                } catch (Exception e) {}
            }
        }
        return list;
    }

    private static String extractString(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int colonIdx = json.indexOf(":", idx);
        int quote1 = json.indexOf("\"", colonIdx);
        int quote2 = json.indexOf("\"", quote1 + 1);
        if (quote1 != -1 && quote2 != -1) return json.substring(quote1 + 1, quote2);
        return null;
    }

    private static String extractNumber(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int colonIdx = json.indexOf(":", idx);
        int endIdx = json.indexOf(",", colonIdx);
        if (endIdx == -1) endIdx = json.length();
        String val = json.substring(colonIdx + 1, endIdx).replaceAll("[^0-9]", "");
        return val.isEmpty() ? null : val;
    }
}