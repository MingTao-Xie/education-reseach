import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 代码相似度计算（用来抓"多个人交了同一份 AI 答案"）。
 *
 * 原理很简单，不需要任何第三方库：
 *   1. 把代码里的空白去掉
 *   2. 取所有长度为 k 的连续片段（shingle），放进集合
 *   3. 两份代码的集合求 Jaccard 相似度 = 交集 / 并集
 *
 * 为什么要算两遍？
 *   第一遍保留变量名：能抓住"连变量名都一样"的直接复制
 *   第二遍把变量名统一替换成 V：能抓住"改了几个变量名"的伪原创
 * 取两者的较大值，是为了宁可多报、也不要漏报，反正后面还有人工复核兜底。
 */
public class Similarity {

    /** Java 关键字：这些词不能掩盖，否则代码结构就看不出来了 */
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new",
            "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while", "true", "false", "null",
            "var", "record", "String", "System", "Math", "Object", "Override"));

    /**
     * 计算两份代码的相似度，返回 0.0 ~ 1.0。
     */
    public static double compare(CodeInfo a, CodeInfo b) {
        if (a == null || b == null) {
            return 0.0;
        }
        double raw = jaccard(shingles(a.codeOnly, 5), shingles(b.codeOnly, 5));
        double masked = jaccard(
                shingles(maskIdentifiers(a.codeOnly), 5),
                shingles(maskIdentifiers(b.codeOnly), 5));
        return Math.max(raw, masked);
    }

    /** 把代码切成 k 长度的片段集合（先去掉所有空白） */
    public static Set<String> shingles(String code, int k) {
        Set<String> set = new HashSet<>();
        String text = code.replaceAll("\\s+", "");
        if (text.length() < k) {
            if (text.length() > 0) {
                set.add(text);
            }
            return set;
        }
        for (int i = 0; i + k <= text.length(); i++) {
            set.add(text.substring(i, i + k));
        }
        return set;
    }

    /** Jaccard = 交集大小 / 并集大小 */
    public static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) inter.size() / union.size();
    }

    /**
     * 把所有非关键字标识符替换成 V。
     * 这样 a.b.c(x) 和 s.t.u(y) 会被认为是同一种结构。
     */
    public static String maskIdentifiers(String code) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int n = code.length();
        while (i < n) {
            char c = code.charAt(i);
            if (Character.isJavaIdentifierStart(c)) {
                int start = i;
                while (i < n && Character.isJavaIdentifierPart(code.charAt(i))) {
                    i++;
                }
                String word = code.substring(start, i);
                sb.append(KEYWORDS.contains(word) ? word : "V");
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }
}
