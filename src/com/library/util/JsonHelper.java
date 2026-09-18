package com.library.util;

import java.util.*;

/**
 * Lightweight, dependency-free JSON serialization and parsing utility.
 * Implemented in pure Java standard library.
 */
public final class JsonHelper {

    private JsonHelper() {}

    /**
     * Escapes characters for JSON string output.
     */
    public static String escapeJson(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        String hex = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(hex.substring(hex.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Unescapes a JSON string.
     */
    public static String unescapeJson(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int len = input.length();
        while (i < len) {
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < len) {
                char next = input.charAt(i + 1);
                switch (next) {
                    case '"':  sb.append('"'); i += 2; break;
                    case '\\': sb.append('\\'); i += 2; break;
                    case '/':  sb.append('/'); i += 2; break;
                    case 'b':  sb.append('\b'); i += 2; break;
                    case 'f':  sb.append('\f'); i += 2; break;
                    case 'n':  sb.append('\n'); i += 2; break;
                    case 'r':  sb.append('\r'); i += 2; break;
                    case 't':  sb.append('\t'); i += 2; break;
                    case 'u':
                        if (i + 5 < len) {
                            String hex = input.substring(i + 2, i + 6);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 6;
                        } else {
                            sb.append(c);
                            i++;
                        }
                        break;
                    default:
                        sb.append(next);
                        i += 2;
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * Serializes a list of property maps into a formatted JSON array string.
     */
    public static String toJsonArray(List<Map<String, String>> objectList) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < objectList.size(); i++) {
            Map<String, String> map = objectList.get(i);
            sb.append("  {\n");
            int j = 0;
            int size = map.size();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                sb.append("    \"").append(escapeJson(entry.getKey())).append("\": ");
                String val = entry.getValue();
                if (val == null) {
                    sb.append("null");
                } else {
                    sb.append("\"").append(escapeJson(val)).append("\"");
                }
                if (++j < size) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("  }");
            if (i < objectList.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]\n");
        return sb.toString();
    }

    /**
     * Parses a JSON array of flat objects into a List of Map<String, String>.
     */
    public static List<Map<String, String>> parseJsonArray(String json) {
        List<Map<String, String>> result = new ArrayList<>();
        if (json == null) return result;
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return result;
        }

        // Tokenize into objects
        int i = 0;
        int len = trimmed.length();
        boolean inString = false;
        boolean isEscaped = false;

        while (i < len) {
            char c = trimmed.charAt(i);
            if (c == '{') {
                // Find matching closing brace
                int start = i;
                int braceDepth = 0;
                while (i < len) {
                    char ch = trimmed.charAt(i);
                    if (ch == '\\' && inString) {
                        isEscaped = !isEscaped;
                    } else {
                        if (ch == '"' && !isEscaped) {
                            inString = !inString;
                        }
                        isEscaped = false;
                    }
                    if (!inString) {
                        if (ch == '{') braceDepth++;
                        else if (ch == '}') {
                            braceDepth--;
                            if (braceDepth == 0) {
                                int end = i + 1;
                                String objectJson = trimmed.substring(start, end);
                                Map<String, String> objMap = parseJsonObject(objectJson);
                                if (!objMap.isEmpty()) {
                                    result.add(objMap);
                                }
                                break;
                            }
                        }
                    }
                    i++;
                }
            }
            i++;
        }
        return result;
    }

    /**
     * Parses an individual JSON object into key-value pairs.
     */
    public static Map<String, String> parseJsonObject(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null) return map;
        String content = json.trim();
        if (content.startsWith("{")) content = content.substring(1);
        if (content.endsWith("}")) content = content.substring(0, content.length() - 1);

        int i = 0;
        int len = content.length();
        boolean inString = false;
        boolean isEscaped = false;
        StringBuilder token = new StringBuilder();
        List<String> keyValues = new ArrayList<>();

        while (i < len) {
            char c = content.charAt(i);
            if (c == '\\' && inString) {
                isEscaped = !isEscaped;
                token.append(c);
            } else {
                if (c == '"' && !isEscaped) {
                    inString = !inString;
                }
                isEscaped = false;
                if (c == ',' && !inString) {
                    keyValues.add(token.toString().trim());
                    token.setLength(0);
                    i++;
                    continue;
                }
                token.append(c);
            }
            i++;
        }
        if (token.length() > 0 && !token.toString().trim().isEmpty()) {
            keyValues.add(token.toString().trim());
        }

        for (String pair : keyValues) {
            int colonIndex = -1;
            boolean strFlag = false;
            boolean escFlag = false;
            for (int k = 0; k < pair.length(); k++) {
                char ch = pair.charAt(k);
                if (ch == '\\' && strFlag) {
                    escFlag = !escFlag;
                } else {
                    if (ch == '"' && !escFlag) {
                        strFlag = !strFlag;
                    }
                    escFlag = false;
                    if (ch == ':' && !strFlag) {
                        colonIndex = k;
                        break;
                    }
                }
            }

            if (colonIndex != -1) {
                String rawKey = pair.substring(0, colonIndex).trim();
                String rawVal = pair.substring(colonIndex + 1).trim();

                String cleanKey = cleanQuotes(rawKey);
                String cleanVal = cleanQuotes(rawVal);
                map.put(unescapeJson(cleanKey), unescapeJson(cleanVal));
            }
        }
        return map;
    }

    private static String cleanQuotes(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1);
        }
        if ("null".equalsIgnoreCase(s)) {
            return null;
        }
        return s;
    }
}
