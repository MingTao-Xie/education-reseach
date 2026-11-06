/**
 * 一条"证据"。
 *
 * 设计要点：判定结果一定要能指着说"是这一条让他被标记的"。
 * 只给一个分数、说不出理由，老师不敢用，学生也会闹。
 *
 * strong = true 表示这是"复制粘贴痕迹"这类硬证据（比如代码里残留了
 * AI 输出的说明文字），命中就可以直接判不合格；其余证据只累加分数。
 */
public class Evidence {

    public final String rule;    // 规则代号，程序里用来分类
    public final String detail;  // 给人看的说明，会原样出现在报告里
    public final int weight;     // 权重
    public final boolean strong; // 是否硬证据

    public Evidence(String rule, String detail, int weight, boolean strong) {
        this.rule = rule;
        this.detail = detail;
        this.weight = weight;
        this.strong = strong;
    }

    @Override
    public String toString() {
        return "[" + rule + " +" + weight + "] " + detail;
    }
}
