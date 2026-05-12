package top.hcode.hoj.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 客观题答案规范化：仅允许 A-D，去重后按字母升序拼接（与库存答案格式一致）。
 */
public final class QuizAnswerUtils {

    private QuizAnswerUtils() {
    }

    /**
     * @param raw 用户输入，如 "A"、 "B,A" 、 " cb " 、 "AC"
     * @return 大写升序无分隔，如 "ABC"
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().toUpperCase().replace(",", "").replace(" ", "").replace("，", "");
        Set<Character> set = new LinkedHashSet<>();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'D') {
                set.add(c);
            }
        }
        List<Character> list = new ArrayList<>(set);
        Collections.sort(list);
        StringBuilder sb = new StringBuilder();
        for (Character c : list) {
            sb.append(c);
        }
        return sb.toString();
    }

    public static boolean isValidSingle(String normalized) {
        return normalized != null && normalized.length() == 1;
    }

    public static boolean isValidMultiple(String normalized) {
        return normalized != null && normalized.length() >= 2 && normalized.length() <= 4;
    }
}
