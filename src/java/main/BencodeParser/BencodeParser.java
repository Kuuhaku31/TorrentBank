package BencodeParser;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public class BencodeParser {

    private final byte[] data;
    private int          index = 0;

    // 解析后的根对象
    private Map<String, Object> root;

    // info 字典的原始字节区间（用于计算 info-hash）
    private int infoStart = -1;
    private int infoEnd   = -1;

    /* ================= 构造函数 ================= */

    public BencodeParser(byte[] fileContent) {
        this.data     = fileContent;
        Object parsed = parseNext();
        if(parsed instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> castedMap = (Map<String, Object>)parsed;
            root                          = castedMap;
        }
    }

    /* ================= 对外方法 ================= */

    // 尝试提取种子 hash
    public String getHashFromTorrent() {
        try {
            // 情况1：.fastresume 直接有 info-hash
            Object fastHash = root.get("info-hash");
            if(fastHash instanceof byte[]) {
                return bytesToHex((byte[])fastHash);
            }

            // 情况2：.torrent 需要计算 info 字典 SHA1
            if(infoStart != -1 && infoEnd != -1) {
                MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                sha1.update(data, infoStart, infoEnd - infoStart);
                return bytesToHex(sha1.digest());
            }

        } catch(Exception ignored) {
        }

        return null;
    }

    // 尝试提取种子文件名
    public String getFileNameFromTorrent() {
        try {
            Object info = root.get("info");
            if(info instanceof Map) {
                Object name = ((Map<?, ?>) info).get("name");
                if(name instanceof byte[]) {
                    return new String((byte[])name, StandardCharsets.UTF_8);
                }
            }
        } catch(Exception ignored) {
        }

        return null;
    }

    // 提取大小
    public Long getFileSizeFromTorrent() {
        try {
            Object info = root.get("info");
            if(info instanceof Map) {
        Map<?, ?> infoMap = (Map<?, ?>) info;

        // 单文件模式
        if(infoMap.containsKey("length")) {
            Object lengthObj = infoMap.get("length");
            if(lengthObj instanceof Long) {
                return (Long)lengthObj;
            }
        }

        // 多文件模式
        if(infoMap.containsKey("files") && infoMap.get("files") instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> files     = (List<Object>)infoMap.get("files");
            long         totalSize = 0L;
            for(Object fileObj : files) {
                if(fileObj instanceof Map) {
              Map<?, ?> fileMap = (Map<?, ?>) fileObj;
              Object    lengthObj = fileMap.get("length");
              if(lengthObj instanceof Long) {
                  totalSize += (Long)lengthObj;
              } else {
                  return null; // 如果有文件缺少 length 字段，则无法确定总大小
              }
                } else {
                    return null; // 无效的 files 列表项
                }
            }
            return totalSize;
        }
            }
        } catch(Exception ignored) {
        }

        return null;
    }

    // 尝试提取 qBt-category
    public String getCategoryFromFastResume() {
        try {
            Object category = root.get("qBt-category");
            if(category instanceof byte[]) {
                return new String((byte[])category, StandardCharsets.UTF_8);
            }
        } catch(Exception ignored) {
        }

        return null;
    }

    // 尝试提取 save_path
    public String getSavePathFromFastResume() {
        try {
            Object savePath = root.get("save_path");
            if(savePath instanceof byte[]) {
                return new String((byte[])savePath, StandardCharsets.UTF_8);
            }
        } catch(Exception ignored) {
        }

        return null;
    }

    /* ================= 重写方法 ================= */

    @Override
    public String toString() {
        if(root == null) return "null";

        // 递归输出
        return formatValue(root, 0);
    }

    private String formatValue(Object value, int depth) {
        if(value == null) {
            return "null";
        }

        if(value instanceof Map<?, ?>) {
            return formatMap((Map<?, ?>) value, depth);
        }

        if(value instanceof List<?>) {
            return formatList((List<?>) value, depth);
        }

        if(value instanceof byte[]) {
            return '"' + new String((byte[])value, StandardCharsets.UTF_8) + '"';
        }

        return String.valueOf(value);
    }

    private String formatMap(Map<?, ?> map, int depth) {
        if(map.isEmpty()) {
            return "{}";
        }

        String indent        = "  ".repeat(depth);
        String childIndent   = "  ".repeat(depth + 1);
        StringBuilder sb     = new StringBuilder();
        Iterator<?> iterator = map.entrySet().iterator();

        sb.append("{\n");
        while(iterator.hasNext()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>)iterator.next();
            sb.append(childIndent)
              .append(String.valueOf(entry.getKey()))
              .append(": ")
              .append(formatValue(entry.getValue(), depth + 1));

            if(iterator.hasNext()) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append(indent).append('}');

        return sb.toString();
    }

    private String formatList(List<?> list, int depth) {
        if(list.isEmpty()) {
            return "[]";
        }

        String indent      = "  ".repeat(depth);
        String childIndent = "  ".repeat(depth + 1);
        StringBuilder sb   = new StringBuilder();

        sb.append("[\n");
        for(int i = 0; i < list.size(); i++) {
            sb.append(childIndent).append(formatValue(list.get(i), depth + 1));
            if(i < list.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append(indent).append(']');

        return sb.toString();
    }

    /* ================= Bencode 解析 ================= */

    private Object parseNext() {
        char c = (char)data[index];

        if(c == 'd')
            return parseDictionary();
        if(c == 'l')
            return parseList();
        if(c == 'i')
            return parseInteger();
        if(Character.isDigit(c))
            return parseBytes();

        throw new RuntimeException("Invalid Bencode at index " + index);
    }

    private Map<String, Object> parseDictionary() {
        index++; // skip 'd'
        Map<String, Object> map = new LinkedHashMap<>();

        while((char)data[index] != 'e') {

            byte[] keyBytes = (byte[])parseNext();
            String key      = new String(keyBytes, StandardCharsets.UTF_8);

            // 记录 info 字典的字节范围
            if("info".equals(key)) {
                infoStart    = index;
                Object value = parseNext();
                infoEnd      = index;
                map.put(key, value);
                continue;
            }

            Object value = parseNext();
            map.put(key, value);
        }

        index++; // skip 'e'
        return map;
    }

    private List<Object> parseList() {
        index++; // skip 'l'
        List<Object> list = new ArrayList<>();

        while((char)data[index] != 'e') {
            list.add(parseNext());
        }

        index++;
        return list;
    }

    private Long parseInteger() {
        index++; // skip 'i'
        int start = index;

        while((char)data[index] != 'e') {
            index++;
        }

        long value = Long.parseLong(new String(data, start, index - start));
        index++;
        return value;
    }

    private byte[] parseBytes() {
        int start = index;

        while((char)data[index] != ':') {
            index++;
        }

        int length = Integer.parseInt(new String(data, start, index - start));
        index++;

        byte[] value = Arrays.copyOfRange(data, index, index + length);
        index += length;

        return value;
    }

    /* ================= 工具方法 ================= */

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for(byte b : bytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
