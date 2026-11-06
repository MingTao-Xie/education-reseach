import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 程序入口。
 *
 * 用法：
 *   java -cp out Main                                 用默认路径 data/submissions.csv
 *   java -cp out Main data/submissions.csv            指定 CSV
 *   java -cp out Main data/submissions.csv out2       指定 CSV 和输出目录
 */
public class Main {

    public static void main(String[] args) {
        Path csv = Paths.get(args.length > 0 ? args[0] : "data/submissions.csv");
        Path outDir = Paths.get(args.length > 1 ? args[1] : "out");

        System.out.println("读取数据: " + csv.toAbsolutePath());

        DataLoader loader = new DataLoader();
        List<Submission> submissions;
        try {
            submissions = loader.load(csv);
        } catch (Exception e) {
            System.out.println("读取失败: " + e.getMessage());
            return;
        }

        if (!loader.errors.isEmpty()) {
            System.out.println("有 " + loader.errors.size() + " 处数据问题：");
            for (String err : loader.errors) {
                System.out.println("  - " + err);
            }
        }
        System.out.println("有效提交: " + submissions.size() + " 条");

        RuleEngine engine = new RuleEngine();
        List<Finding> findings = engine.analyze(submissions);

        // 评分是按"学生+题目"给的，所以报告只保留每个人每题的最后一次提交；
        // 中间版本只作为"有没有迭代过程"的证据参与计算，不单独出现在报告里。
        List<Finding> lastOnly = pickLastSubmission(findings);
        System.out.println("学生题目组合: " + lastOnly.size() + " 组（报告按此统计）");

        try {
            new ReportWriter().write(lastOnly, outDir);
            System.out.println();
            System.out.println("报告已生成：");
            System.out.println("  " + outDir.resolve("report.csv").toAbsolutePath());
            System.out.println("  " + outDir.resolve("report.html").toAbsolutePath());
        } catch (Exception e) {
            System.out.println("写报告失败: " + e.getMessage());
        }
    }

    /** 每组（学生+题目）只保留最后一次提交，并按风险分从高到低排序 */
    private static List<Finding> pickLastSubmission(List<Finding> findings) {
        java.util.Map<String, Finding> last = new java.util.LinkedHashMap<>();
        for (Finding f : findings) {
            Finding current = last.get(f.submission.groupKey());
            if (current == null || f.submission.order > current.submission.order) {
                last.put(f.submission.groupKey(), f);
            }
        }
        List<Finding> result = new java.util.ArrayList<>(last.values());
        result.sort((a, b) -> Integer.compare(b.score, a.score));
        return result;
    }
}
