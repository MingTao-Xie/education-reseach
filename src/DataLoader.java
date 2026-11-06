import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 读平台导出的 CSV。
 *
 * 设计上故意做成"容错 + 报错清楚"：
 *   - 表头名字能认中英文两套（学号 / studentId 都行），列的顺序随意
 *   - 文件带 BOM 也能读
 *   - 某一行解析失败不会整体崩，而是记下来在最后统一提示
 *
 * 新手最容易踩的坑：CSV 用 Excel 另存过之后会变成 GBK 编码，
 * 这里统一按 UTF-8 读，读出来是乱码的话，用记事本另存为 UTF-8 即可。
 */
public class DataLoader {

    private static final DateTimeFormatter[] TIME_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
    };

    private static final String[][] ALIASES = {
            // 字段名, 可能的表头写法（全部转小写后比较）
            {"studentId", "studentid", "student_id", "sid", "学号", "id"},
            {"name", "name", "studentname", "姓名", "学生姓名"},
            {"problemId", "problemid", "problem_id", "pid", "题号", "题目编号", "题目"},
            {"submitTime", "submittime", "submit_time", "time", "提交时间", "提交日期"},
            {"result", "result", "status", "结果", "判题结果", "状态"},
            {"codeFile", "codefile", "code_file", "file", "代码文件", "代码路径", "文件名"},
            {"code", "code", "源码", "代码内容"},
    };

    public final List<String> errors = new ArrayList<>();

    public List<Submission> load(Path csvPath) throws IOException {
        List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new IOException("CSV 是空的：" + csvPath);
        }

        // 去掉 UTF-8 BOM
        String headerLine = lines.get(0).replace("\uFEFF", "");
        List<String> header = parseLine(headerLine);
        Map<String, Integer> index = buildIndex(header);

        if (!index.containsKey("studentId") || !index.containsKey("submitTime")) {
            throw new IOException("表头里找不到必需的列。至少要有：学号、提交时间。\n"
                    + "实际读到的表头是：" + header);
        }

        Path baseDir = csvPath.toAbsolutePath().getParent();
        List<Submission> result = new ArrayList<>();
        for (int lineNo = 1; lineNo < lines.size(); lineNo++) {
            String raw = lines.get(lineNo);
            if (raw.trim().isEmpty()) {
                continue;
            }
            List<String> cols = parseLine(raw);
            try {
                String studentId = get(cols, index, "studentId");
                String name = get(cols, index, "name");
                String problemId = get(cols, index, "problemId");
                String timeText = get(cols, index, "submitTime");
                String status = get(cols, index, "result");
                String codeFile = get(cols, index, "codeFile");

                if (studentId.isEmpty() || timeText.isEmpty()) {
                    errors.add("第 " + (lineNo + 1) + " 行缺少学号或提交时间，已跳过");
                    continue;
                }

                Submission s = new Submission(studentId, name.isEmpty() ? studentId : name,
                        problemId.isEmpty() ? "未命名题目" : problemId,
                        parseTime(timeText), status.isEmpty() ? "未知" : status, codeFile);

                // 代码优先从文件读；没有文件就读 CSV 里的内联列
                if (!codeFile.isEmpty()) {
                    Path codePath = baseDir.resolve(codeFile);
                    if (Files.exists(codePath)) {
                        s.code = Files.readString(codePath, StandardCharsets.UTF_8);
                    } else {
                        errors.add("第 " + (lineNo + 1) + " 行的代码文件不存在：" + codePath);
                        s.code = "";
                    }
                } else {
                    s.code = get(cols, index, "code");
                }
                result.add(s);
            } catch (Exception e) {
                errors.add("第 " + (lineNo + 1) + " 行解析失败：" + e.getMessage());
            }
        }
        return result;
    }

    private Map<String, Integer> buildIndex(List<String> header) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            String cell = header.get(i).trim().toLowerCase();
            for (String[] alias : ALIASES) {
                for (int k = 1; k < alias.length; k++) {
                    if (cell.equals(alias[k]) || cell.equals(alias[k].toLowerCase())) {
                        if (!index.containsKey(alias[0])) {
                            index.put(alias[0], i);
                        }
                    }
                }
            }
        }
        return index;
    }

    private String get(List<String> cols, Map<String, Integer> index, String key) {
        Integer i = index.get(key);
        if (i == null || i >= cols.size()) {
            return "";
        }
        return cols.get(i).trim();
    }

    private LocalDateTime parseTime(String text) {
        String value = text.trim().replace('/', '-');
        for (DateTimeFormatter fmt : TIME_FORMATS) {
            try {
                return LocalDateTime.parse(text.trim(), fmt);
            } catch (Exception ignored) {
                // 换下一种格式再试
            }
        }
        throw new IllegalArgumentException("无法识别的时间格式：" + text);
    }

    /**
     * 极简 CSV 解析：支持双引号包裹、双引号转义（""）。
     * 不支持字段内换行——所以代码建议放单独文件，不要塞进 CSV。
     */
    public static List<String> parseLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuote) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++;
                    } else {
                        inQuote = false;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == '"') {
                    inQuote = true;
                } else if (c == ',') {
                    out.add(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
        }
        out.add(sb.toString());
        return out;
    }
}
