import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把一份 Java 源码拆成三样东西：
 *
 *   1. codeOnly  —— 去掉注释、把字符串内容清空后的"纯代码"
 *   2. comments  —— 所有注释的正文拼在一起
 *   3. 各种统计  —— 行数、方法数、Javadoc 数、import 列表……
 *
 * 为什么要区分"代码区"和"注释区"？
 * 因为有些规则只能看注释（比如"以下是完整的实现"这种 AI 话术只在注释里），
 * 有些规则只能看代码（比如全角分号），混在一起会大量误报。
 *
 * 这里用的是手写的逐字符扫描，不是编译器级别的词法分析，
 * 足够应付学生作业；对嵌套注释、文本块(\\"\\"\\")等极端写法不保证正确。
 */
public class CodeInfo {

    /** 去掉注释与字符串内容后的代码 */
    public String codeOnly = "";
    /** 所有注释正文 */
    public String comments = "";
    /** 源码总行数 */
    public int lineCount;
    /** 代码区非空白字符数（比"文件字节数"更能代表工作量） */
    public int codeCharCount;
    /** 大致的方法个数 */
    public int methodCount;
    /** Javadoc（斜杠+两个星号）的个数 */
    public int javadocCount;
    /** import 语句里没被用到的类名 */
    public final List<String> unusedImports = new ArrayList<>();
    /** 代码区出现了全角标点（说明是从中文输入环境粘贴过来的） */
    public boolean hasFullWidthPunctInCode;

    /** 全角标点集合：这些字符在 Java 里都是非法语法符号 */
    private static final String FULL_WIDTH = "（）：；，。！？、【】“”‘’";

    /** 方法声明的粗略正则（够用就行，不追求 100% 准确） */
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "(?m)^[ \\t]*(?:public|private|protected|static|final|synchronized|abstract|native)"
                    + "[\\w\\s<>\\[\\],\\.@]*\\s+([A-Za-z_$][\\w$]*)\\s*\\([^)]*\\)\\s*(?:throws[\\w\\s,]+)?\\{");

    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "(?m)^\\s*import\\s+(?:static\\s+)?([\\w\\.\\*]+)\\s*;");

    /** 解析一份源码 */
    public static CodeInfo parse(String code) {
        CodeInfo info = new CodeInfo();
        if (code == null) {
            return info;
        }
        info.lineCount = code.split("\n", -1).length;

        StringBuilder codeBuf = new StringBuilder();
        StringBuilder commentBuf = new StringBuilder();
        int n = code.length();
        int i = 0;
        while (i < n) {
            char c = code.charAt(i);
            char next = (i + 1 < n) ? code.charAt(i + 1) : '\0';

            if (c == '/' && next == '/') {                 // 行注释
                int end = code.indexOf('\n', i);
                if (end < 0) {
                    end = n;
                }
                commentBuf.append(code, i + 2, end).append('\n');
                i = end;
            } else if (c == '/' && next == '*') {          // 块注释 / Javadoc
                boolean javadoc = (i + 2 < n && code.charAt(i + 2) == '*');
                int end = code.indexOf("*/", i + 2);
                if (end < 0) {
                    end = n;
                }
                commentBuf.append(code, i + 2, end).append('\n');
                if (javadoc) {
                    info.javadocCount++;
                }
                codeBuf.append(' ');                        // 注释整段丢掉
                i = Math.min(end + 2, n);
            } else if (c == '"') {                          // 字符串
                i++;
                while (i < n && code.charAt(i) != '"') {
                    if (code.charAt(i) == '\\') {
                        i++;
                    }
                    i++;
                }
                i++;
                codeBuf.append("\"\"");                     // 内容清空，只留引号
            } else if (c == '\'') {                         // 字符
                i++;
                while (i < n && code.charAt(i) != '\'') {
                    if (code.charAt(i) == '\\') {
                        i++;
                    }
                    i++;
                }
                i++;
                codeBuf.append("''");
            } else {
                codeBuf.append(c);
                i++;
            }
        }

        info.codeOnly = codeBuf.toString();
        info.comments = commentBuf.toString();

        // 代码区非空白字符数
        int chars = 0;
        for (int k = 0; k < info.codeOnly.length(); k++) {
            if (!Character.isWhitespace(info.codeOnly.charAt(k))) {
                chars++;
            }
        }
        info.codeCharCount = chars;

        // 方法数
        Matcher m = METHOD_PATTERN.matcher(info.codeOnly);
        while (m.find()) {
            info.methodCount++;
        }

        // 全角标点（只看代码区，注释和字符串里的中文标点是正常的）
        for (int k = 0; k < info.codeOnly.length(); k++) {
            if (FULL_WIDTH.indexOf(info.codeOnly.charAt(k)) >= 0) {
                info.hasFullWidthPunctInCode = true;
                break;
            }
        }

        // 未使用的 import
        Matcher im = IMPORT_PATTERN.matcher(code);
        while (im.find()) {
            String full = im.group(1);
            if (full.endsWith("*")) {
                continue;                               // 通配 import 不检查
            }
            String simple = full.substring(full.lastIndexOf('.') + 1);
            if (!containsWord(info.codeOnly, simple)) {
                info.unusedImports.add(simple);
            }
        }
        return info;
    }

    /** 判断一段文本里是否出现过某个完整单词（避免 Set 被 Contains 之类误判） */
    public static boolean containsWord(String text, String word) {
        return Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(text).find();
    }
}
