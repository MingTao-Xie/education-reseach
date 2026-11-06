import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 输出三份东西：
 *   1. report.csv   —— 可以再用 Excel 二次筛选
 *   2. report.html  —— 给老师看的，双击就能开，带颜色和证据
 *   3. 控制台摘要    —— 你自己跑完一眼看结论
 *
 * 报告里永远写"建议复核"，不写"判定作弊"。这是刻意的措辞选择：
 * 工具只提供线索，结论由老师下。
 */
public class ReportWriter {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void write(List<Finding> findings, Path outDir) throws IOException {
        Files.createDirectories(outDir);
        writeCsv(findings, outDir.resolve("report.csv"));
        writeHtml(findings, outDir.resolve("report.html"));
        printSummary(findings);
    }

    // ------------------------------------------------------------------ //
    private void writeCsv(List<Finding> findings, Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("学号,姓名,题目,提交时间,第几次提交,判题结果,结论,风险分,证据\n");
        for (Finding f : findings) {
            sb.append(csvField(f.submission.studentId)).append(',')
              .append(csvField(f.submission.name)).append(',')
              .append(csvField(f.submission.problemId)).append(',')
              .append(f.submission.submitTime.format(TIME_FMT)).append(',')
              .append(f.submission.order).append(',')
              .append(csvField(f.submission.result)).append(',')
              .append(f.verdict).append(',')
              .append(f.score).append(',')
              .append(csvField(f.evidenceLine())).append('\n');
        }
        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
    }

    private void writeHtml(List<Finding> findings, Path path) throws IOException {
        int fail = 0;
        int review = 0;
        int pass = 0;
        for (Finding f : findings) {
            if (f.verdict.equals(Finding.FAIL)) {
                fail++;
            } else if (f.verdict.equals(Finding.REVIEW)) {
                review++;
            } else {
                pass++;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\">")
          .append("<title>作业提交检查报告</title><style>")
          .append("body{font-family:'Microsoft YaHei',sans-serif;background:#f5f7fa;margin:0;padding:32px;color:#1f2329}")
          .append("h1{font-size:22px;margin:0 0 6px}")
          .append(".sub{color:#6b7280;font-size:13px;margin-bottom:20px}")
          .append(".cards{display:flex;gap:14px;margin-bottom:24px}")
          .append(".card{background:#fff;border-radius:10px;padding:16px 22px;box-shadow:0 1px 3px rgba(0,0,0,.08);min-width:120px}")
          .append(".card .n{font-size:28px;font-weight:700}")
          .append(".card .t{font-size:12px;color:#6b7280;margin-top:2px}")
          .append("table{width:100%;border-collapse:collapse;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,.08)}")
          .append("th,td{padding:10px 12px;text-align:left;font-size:13px;border-bottom:1px solid #eef0f4;vertical-align:top}")
          .append("th{background:#eef2f9;font-size:12px;color:#374151}")
          .append(".fail{color:#c0392b;font-weight:700}.review{color:#b7791f;font-weight:700}.pass{color:#1e8449}")
          .append(".ev{margin:0;padding-left:16px;color:#4b5563;line-height:1.7}")
          .append(".tag{display:inline-block;background:#fdeeee;color:#c0392b;border-radius:4px;padding:0 6px;font-size:11px;margin-left:6px}")
          .append("</style></head><body>")
          .append("<h1>作业提交检查报告</h1>")
          .append("<div class=\"sub\">生成时间：")
          .append(LocalDateTime.now().format(TIME_FMT))
          .append("　·　共 ").append(findings.size()).append(" 条提交")
          .append("　·　本报告只提供线索，最终结论请以面谈结果为准</div>");

        sb.append("<div class=\"cards\">")
          .append(card(fail, "建议不合格", "fail"))
          .append(card(review, "建议复核", "review"))
          .append(card(pass, "未发现问题", "pass"))
          .append("</div>");

        sb.append("<table><tr><th>学生</th><th>题目</th><th>提交时间</th><th>结论</th>")
          .append("<th>风险分</th><th>证据</th></tr>");

        for (Finding f : findings) {
            String cls = f.verdict.equals(Finding.FAIL) ? "fail"
                    : f.verdict.equals(Finding.REVIEW) ? "review" : "pass";
            sb.append("<tr><td>").append(htmlEscape(f.submission.name))
              .append("<br><span style=\"color:#9ca3af;font-size:12px\">")
              .append(htmlEscape(f.submission.studentId)).append("</span></td>")
              .append("<td>").append(htmlEscape(f.submission.problemId))
              .append("<br><span style=\"color:#9ca3af;font-size:12px\">第 ")
              .append(f.submission.order).append(" 次提交</span></td>")
              .append("<td>").append(f.submission.submitTime.format(TIME_FMT)).append("</td>")
              .append("<td class=\"").append(cls).append("\">").append(f.verdict).append("</td>")
              .append("<td>").append(f.score).append("</td>")
              .append("<td>");
            if (f.evidences.isEmpty()) {
                sb.append("<span style=\"color:#9ca3af\">无</span>");
            } else {
                sb.append("<ul class=\"ev\">");
                for (Evidence e : f.evidences) {
                    sb.append("<li>").append(htmlEscape(e.detail));
                    if (e.strong) {
                        sb.append("<span class=\"tag\">硬证据</span>");
                    }
                    sb.append("</li>");
                }
                sb.append("</ul>");
            }
            sb.append("</td></tr>");
        }
        sb.append("</table></body></html>");

        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
    }

    private String card(int number, String title, String cls) {
        return "<div class=\"card\"><div class=\"n " + cls + "\">" + number
                + "</div><div class=\"t\">" + title + "</div></div>";
    }

    private void printSummary(List<Finding> findings) {
        int fail = 0;
        int review = 0;
        int pass = 0;
        for (Finding f : findings) {
            if (f.verdict.equals(Finding.FAIL)) {
                fail++;
            } else if (f.verdict.equals(Finding.REVIEW)) {
                review++;
            } else {
                pass++;
            }
        }
        System.out.println();
        System.out.println("================= 检查完成 =================");
        System.out.println("提交总数 : " + findings.size());
        System.out.println("建议不合格: " + fail);
        System.out.println("建议复核  : " + review);
        System.out.println("未发现问题: " + pass);
        System.out.println();
        System.out.println("---- 需要处理的前 10 条 ----");
        int shown = 0;
        for (Finding f : findings) {
            if (f.verdict.equals(Finding.PASS)) {
                continue;
            }
            if (shown++ >= 10) {
                break;
            }
            System.out.printf("%-6s %-10s %-8s %-4s 分=%-2d %s%n",
                    f.submission.studentId, f.submission.name, f.submission.problemId,
                    f.verdict, f.score, f.evidenceLine());
        }
    }

    /** CSV 字段转义：含逗号、引号、换行时必须整体加引号，内部引号翻倍 */
    private String csvField(String text) {
        if (text == null) {
            return "";
        }
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    /** HTML 转义：防止代码内容里的尖括号破坏页面结构 */
    private String htmlEscape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
