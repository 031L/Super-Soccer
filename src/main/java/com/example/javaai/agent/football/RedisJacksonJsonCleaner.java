package com.example.javaai.agent.football;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 清洗 Spring Data Redis（Jackson 默认类型）序列化后的 JSON，便于 Data Agent 解析。
 * <ul>
 *   <li>移除 {@code @class} 类型元数据</li>
 *   <li>展开 {@code ["java.math.BigDecimal", 2.95]}、{@code ["java.util.ArrayList", [...]]} 等包装</li>
 *   <li>将字符串中的 {@code \xHH} UTF-8 十六进制转义还原为中文等可读文本</li>
 *   <li>省略值为 null 的字段，减小 prompt 体积</li>
 * </ul>
 */
public final class RedisJacksonJsonCleaner {

    private static final Pattern UTF8_HEX_RUN = Pattern.compile("(\\\\x[0-9a-fA-F]{2})+");

    private RedisJacksonJsonCleaner() {
    }

    /**
     * 清洗 JSON 字符串；非 JSON 时原样返回 trim 结果。
     */
    public static String cleanJson(String raw) {
        if (StrUtil.isBlank(raw)) {
            return raw;
        }
        String trimmed = preprocessHexEscapesInRawJson(raw.trim());
        if (!JSONUtil.isTypeJSON(trimmed)) {
            return trimmed;
        }
        Object cleaned = clean(JSONUtil.parse(trimmed));
        return JSONUtil.toJsonPrettyStr(cleaned);
    }

    /**
     * Spring Data Redis 写入的 JSON 字符串常含非标准的 {@code \xHH} UTF-8 转义，标准解析器无法读取，需先还原。
     */
    static String preprocessHexEscapesInRawJson(String raw) {
        if (raw == null || !raw.contains("\\x")) {
            return raw;
        }
        Matcher matcher = UTF8_HEX_RUN.matcher(raw);
        StringBuilder out = new StringBuilder(raw.length());
        int last = 0;
        while (matcher.find()) {
            out.append(raw, last, matcher.start());
            out.append(decodeHexRun(matcher.group()));
            last = matcher.end();
        }
        out.append(raw.substring(last));
        return out.toString();
    }

    public static Object clean(Object node) {
        if (node == null) {
            return null;
        }
        if (node instanceof JSONArray array) {
            return cleanArray(array);
        }
        if (node instanceof JSONObject object) {
            return cleanObject(object);
        }
        if (node instanceof String text) {
            return decodeUtf8HexEscapes(text);
        }
        return node;
    }

    private static Object cleanArray(JSONArray array) {
        if (array.isEmpty()) {
            return new JSONArray();
        }
        if (array.size() == 2 && array.get(0) instanceof String typeName && isJavaTypeWrapper(typeName)) {
            return clean(array.get(1));
        }
        JSONArray result = new JSONArray();
        for (int i = 0; i < array.size(); i++) {
            Object item = clean(array.get(i));
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    private static JSONObject cleanObject(JSONObject object) {
        JSONObject result = new JSONObject();
        for (String key : object.keySet()) {
            if ("@class".equals(key)) {
                continue;
            }
            Object value = object.get(key);
            if (value == null || value instanceof cn.hutool.json.JSONNull) {
                continue;
            }
            Object cleaned = clean(value);
            if (cleaned != null) {
                result.set(key, cleaned);
            }
        }
        return result;
    }

    private static boolean isJavaTypeWrapper(String typeName) {
        return typeName.startsWith("java.")
                || typeName.startsWith("javax.")
                || typeName.startsWith("jakarta.");
    }

    /**
     * 将 {@code \xe6\x96\xaf} 形式的 UTF-8 十六进制转义还原为 Unicode 文本。
     */
    static String decodeUtf8HexEscapes(String input) {
        if (input == null || !input.contains("\\x")) {
            return input;
        }
        Matcher matcher = UTF8_HEX_RUN.matcher(input);
        StringBuilder out = new StringBuilder(input.length());
        int last = 0;
        while (matcher.find()) {
            out.append(input, last, matcher.start());
            out.append(decodeHexRun(matcher.group()));
            last = matcher.end();
        }
        out.append(input.substring(last));
        return out.toString();
    }

    private static String decodeHexRun(String run) {
        List<Byte> bytes = new ArrayList<>();
        int i = 0;
        while (i + 3 < run.length()) {
            if (run.charAt(i) == '\\' && run.charAt(i + 1) == 'x') {
                bytes.add((byte) Integer.parseInt(run.substring(i + 2, i + 4), 16));
                i += 4;
            } else {
                break;
            }
        }
        if (bytes.isEmpty()) {
            return run;
        }
        byte[] arr = new byte[bytes.size()];
        for (int j = 0; j < bytes.size(); j++) {
            arr[j] = bytes.get(j);
        }
        return new String(arr, StandardCharsets.UTF_8);
    }
}
