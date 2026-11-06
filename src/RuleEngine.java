import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 判定引擎：把所有规则跑一遍，给每条提交出三档结论。
 *
 * ⚠️ 所有阈值都集中在文件顶部的常量里，改完重新编译即可。
 * 一开始不要凭感觉定，先用真实数据跑、看名单、人工核对，再回来调。
 *
 * 规则分两类：
 *   硬证据（strong）：代码里残留 AI 输出痕迹 → 一条就判不合格
 *   软证据：累加分数，分数够了才升级
 */
public class RuleEngine {

    // ---------------- 权重 ----------------
    public static final int W_ARTIFACT = 5;        // 复制痕迹（硬）
    public static final int W_SPEED = 4;           // 打字速度超物理上限（硬）
    public static final int W_SIMILAR = 4;         // 与他人代码高度相似
    public static final int W_FULLWIDTH = 3;       // 代码区出现全角标点
    public static final int W_UNUSED_IMPORT = 1;   // 有未使用的 import
    public static final int W_SINGLE_SUBMIT = 1;   // 该题只有一次提交
    public static final int W_JAVADOC_ALL = 1;     // 每个方法都有 Javadoc

    // ---------------- 阈值 ----------------
    /** 人手打字的物理上限（字符/分钟）。普通学生含思考约 80~150。 */
    public static final double SPEED_LIMIT = 400.0;
    /** 相似度超过多少算"同源" */
    public static final double SIMILAR_LIMIT = 0.85;
    /** 分数达到多少判不合格 */
    public static final int FAIL_SCORE = 8;
    /** 分数达到多少需要复核 */
    public static final int REVIEW_SCORE = 3;

    /** AI 说话的口头禅，出现在注释里几乎不可能是学生自己写的 */
    private static final String[] AI_PHRASES = {
            "以下是", "希望对你有帮助", "如需修改", "完整实现", "示例代码如下",
            "上述代码", "这段代码的作用", "如有疑问", "可以进一步", "核心思想是"
    };

    /** Markdown 标记：从聊天窗口复制时容易一起粘进来 */
    private static final String[] MARKDOWN_MARKS = {"**", "###", "- [", "|---", "```"};

    public List<Finding> analyze(List<Submission> submissions) {
        Map<Submission, Finding> findings = new LinkedHashMap<>();
        for (Submission s : submissions) {
            findings.put(s, new Finding(s));
        }

        // ---------- 第一步：按"学生+题目"分组，组内按时间排序 ----------
        Map<String, List<Submission>> groups = new LinkedHashMap<>();
        for (Submission s : submissions) {
            List<Submission> list = groups.get(s.groupKey());
            if (list == null) {
                list = new ArrayList<>();
                groups.put(s.groupKey(), list);
            }
            list.add(s);
        }

        for (List<Submission> group : groups.values()) {
            group.sort(Comparator.comparing(s -> s.submitTime));
            for (int i = 0; i < group.size(); i++) {
                Submission s = group.get(i);
                s.order = i + 1;
                Finding f = findings.get(s);
                f.codeInfo = CodeInfo.parse(s.code);
                applySingleRules(f, group, i);
            }
        }

        // ---------- 第二步：同一道题横向比对（抓"多人交同一份答案"） ----------
        applySimilarityRules(submissions, findings);

        // ---------- 第三步：汇总出结论 ----------
        for (Finding f : findings.values()) {
            if (f.hasStrongEvidence()) {
                f.verdict = Finding.FAIL;
            } else if (f.score >= FAIL_SCORE) {
                f.verdict = Finding.FAIL;
            } else if (f.score >= REVIEW_SCORE) {
                f.verdict = Finding.REVIEW;
            } else {
                f.verdict = Finding.PASS;
            }
        }

        List<Finding> result = new ArrayList<>(findings.values());
        result.sort((a, b) -> Integer.compare(b.score, a.score));
        return result;
    }

    // ================================================================== //
    // 单条提交的规则
    // ================================================================== //
    private void applySingleRules(Finding f, List<Submission> group, int index) {
        Submission s = group.get(index);
        CodeInfo info = f.codeInfo;

        // --- 规则 1：AI 输出痕迹（硬证据） ---
        String hit = findAiPhrase(info.comments);
        if (hit != null) {
            f.add(new Evidence("ARTIFACT_PHRASE",
                    "注释中出现 AI 输出话术「" + hit + "」", W_ARTIFACT, true));
        }
        String md = findMarkdown(info.comments);
        if (md != null) {
            f.add(new Evidence("ARTIFACT_MARKDOWN",
                    "注释中出现 Markdown 标记「" + md + "」", W_ARTIFACT, true));
        }
        // 代码块围栏直接看原文：它一般连注释都不是，属于粘错了地方
        if (s.code.contains("```")) {
            f.add(new Evidence("ARTIFACT_FENCE",
                    "代码中出现 Markdown 代码块围栏 ```", W_ARTIFACT, true));
        }

        // --- 规则 2：代码区出现全角标点 ---
        if (info.hasFullWidthPunctInCode) {
            f.add(new Evidence("FULLWIDTH",
                    "代码区出现全角标点（中文输入法下的括号/分号），通常是整段粘贴导致",
                    W_FULLWIDTH, false));
        }

        // --- 规则 3：未使用的 import ---
        if (!info.unusedImports.isEmpty()) {
            f.add(new Evidence("UNUSED_IMPORT",
                    "存在未使用的 import：" + String.join("、", info.unusedImports),
                    W_UNUSED_IMPORT, false));
        }

        // --- 规则 4：每个方法都写了 Javadoc ---
        if (info.methodCount >= 3 && info.javadocCount >= info.methodCount) {
            f.add(new Evidence("JAVADOC_ALL",
                    "全部 " + info.methodCount + " 个方法都带 Javadoc，注释完整度超出课程作业常见水平",
                    W_JAVADOC_ALL, false));
        }

        // --- 规则 5：该题只有一次提交 ---
        if (group.size() == 1) {
            f.add(new Evidence("SINGLE_SUBMIT",
                    "该题仅提交 1 次且无中途修改（可能是本地写好再上传，属弱信号）",
                    W_SINGLE_SUBMIT, false));
        }

        // --- 规则 6：打字速度超出物理上限 ---
        if (index > 0) {
            Submission prev = group.get(index - 1);
            double minutes = Duration.between(prev.submitTime, s.submitTime).toMillis() / 60000.0;
            int prevLen = countChars(prev.code);
            int currLen = countChars(s.code);
            int kept = commonEdgeLength(prev.code, s.code);
            int added = Math.max(0, currLen - kept);
            if (minutes <= 0.0) {
                minutes = 0.01;      // 同一秒内提交，按极小间隔处理
            }
            double cpm = added / minutes;
            if (cpm > SPEED_LIMIT) {
                f.add(new Evidence("SPEED",
                        String.format("两次提交间隔 %.1f 分钟，新增约 %d 个字符，"
                                        + "约合 %.0f 字符/分钟（人手打上限约 %.0f）",
                                minutes, added, cpm, SPEED_LIMIT),
                        W_SPEED, true));
            }
        }
    }

    // ================================================================== //
    // 跨学生相似度
    // ================================================================== //
    private void applySimilarityRules(List<Submission> submissions,
                                      Map<Submission, Finding> findings) {
        // 同一道题下，每个学生只取他最后交的那一版参与比对
        Map<String, List<Submission>> byProblem = new LinkedHashMap<>();
        for (Submission s : submissions) {
            List<Submission> list = byProblem.get(s.problemId);
            if (list == null) {
                list = new ArrayList<>();
                byProblem.put(s.problemId, list);
            }
            list.add(s);
        }

        for (Map.Entry<String, List<Submission>> entry : byProblem.entrySet()) {
            Map<String, Submission> latestByStudent = new LinkedHashMap<>();
            for (Submission s : entry.getValue()) {
                Submission current = latestByStudent.get(s.studentId);
                if (current == null || s.order > current.order) {
                    latestByStudent.put(s.studentId, s);
                }
            }

            List<Submission> list = new ArrayList<>(latestByStudent.values());
            // 每个人收集"所有和他高度相似的其他人"，因为"和 3 个人都一样"
            // 比"和 1 个人一样"要有说服力得多
            Map<Submission, List<String>> peers = new LinkedHashMap<>();
            Map<Submission, Double> bestSim = new LinkedHashMap<>();

            for (int i = 0; i < list.size(); i++) {
                for (int j = i + 1; j < list.size(); j++) {
                    Submission a = list.get(i);
                    Submission b = list.get(j);
                    // CodeInfo 在第一步已经解析过，这里直接复用，不重复解析
                    double sim = Similarity.compare(findings.get(a).codeInfo,
                            findings.get(b).codeInfo);
                    if (sim >= SIMILAR_LIMIT) {
                        addPeer(peers, bestSim, a, b, sim);
                        addPeer(peers, bestSim, b, a, sim);
                    }
                }
            }

            for (Map.Entry<Submission, List<String>> e : peers.entrySet()) {
                Submission self = e.getKey();
                List<String> names = e.getValue();
                StringBuilder who = new StringBuilder();
                for (int i = 0; i < names.size() && i < 3; i++) {
                    if (i > 0) {
                        who.append("、");
                    }
                    who.append(names.get(i));
                }
                if (names.size() > 3) {
                    who.append(" 等");
                }
                findings.get(self).add(new Evidence("SIMILAR",
                        String.format("与 %d 份提交的代码高度相似（%s），最高相似度 %.2f",
                                names.size(), who, bestSim.get(self)),
                        W_SIMILAR, false));
            }
        }
    }

    private void addPeer(Map<Submission, List<String>> peers,
                         Map<Submission, Double> bestSim,
                         Submission self, Submission peer, double sim) {
        List<String> names = peers.get(self);
        if (names == null) {
            names = new ArrayList<>();
            peers.put(self, names);
        }
        names.add(peer.name + "(" + peer.studentId + ")");
        Double current = bestSim.get(self);
        if (current == null || sim > current) {
            bestSim.put(self, sim);
        }
    }

    // ================================================================== //
    // 小工具
    // ================================================================== //
    private String findAiPhrase(String comments) {
        for (String phrase : AI_PHRASES) {
            if (comments.contains(phrase)) {
                return phrase;
            }
        }
        return null;
    }

    private String findMarkdown(String comments) {
        for (String mark : MARKDOWN_MARKS) {
            if (comments.contains(mark)) {
                return mark;
            }
        }
        return null;
    }

    /** 统计非空白字符数 */
    public static int countChars(String code) {
        if (code == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < code.length(); i++) {
            if (!Character.isWhitespace(code.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    /**
     * 估算两份代码"保留了多少字符"：取最长公共前缀 + 最长公共后缀。
     * 这是 diff 的极简近似——足够用来判断"这次提交是新写了多少"。
     */
    public static int commonEdgeLength(String a, String b) {
        if (a == null || b == null) {
            return 0;
        }
        int prefix = 0;
        int limit = Math.min(a.length(), b.length());
        while (prefix < limit && a.charAt(prefix) == b.charAt(prefix)) {
            prefix++;
        }
        int suffix = 0;
        while (suffix < limit - prefix
                && a.charAt(a.length() - 1 - suffix) == b.charAt(b.length() - 1 - suffix)) {
            suffix++;
        }
        return prefix + suffix;
    }
}
