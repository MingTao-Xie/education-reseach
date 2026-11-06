import java.util.ArrayList;
import java.util.List;

/**
 * 一条提交的最终判定结果。
 *
 * 三档结论（不是"作弊/没作弊"的黑白判断）：
 *   不合格 —— 有硬证据（复制痕迹），或分数很高
 *   复核   —— 有可疑迹象，需要老师找学生谈话
 *   合格   —— 没发现问题
 *
 * 程序永远只输出"建议"，最终由老师决定。这是为了保护认真写作业的学生。
 */
public class Finding {

    public static final String FAIL = "不合格";
    public static final String REVIEW = "复核";
    public static final String PASS = "合格";

    public final Submission submission;
    public final List<Evidence> evidences = new ArrayList<>();
    public int score;
    public String verdict = PASS;
    public CodeInfo codeInfo;

    public Finding(Submission submission) {
        this.submission = submission;
    }

    public void add(Evidence e) {
        evidences.add(e);
        score += e.weight;
    }

    public boolean hasStrongEvidence() {
        for (Evidence e : evidences) {
            if (e.strong) {
                return true;
            }
        }
        return false;
    }

    /** 把证据拼成一行，写进 CSV 用。 */
    public String evidenceLine() {
        if (evidences.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < evidences.size(); i++) {
            if (i > 0) {
                sb.append("；");
            }
            sb.append(evidences.get(i).detail);
        }
        return sb.toString();
    }
}
