import java.time.LocalDateTime;

/**
 * 一条提交记录 = 平台 CSV 里的一行。
 *
 * 平台的导出一般长这样（表头必须能对上，顺序无所谓）：
 *   studentId,name,problemId,submitTime,result,codeFile
 *   2023001,张三,P1042,2026-10-12 03:14:22,AC,codes/2023001_P1042_1.java
 *
 * codeFile 是代码文件的路径（相对 CSV 所在目录）。用文件而不是把代码塞进 CSV，
 * 是因为代码里有逗号、引号、换行，塞进 CSV 很容易解析炸掉。
 */
public class Submission {

    public String studentId;
    public String name;
    public String problemId;
    public LocalDateTime submitTime;
    public String result;      // AC / WA / CE / RE / TLE
    public String codeFile;

    public String code;        // 读进来的代码正文
    public int order;          // 同一学生同一题的第几次提交，从 1 开始

    public Submission(String studentId, String name, String problemId,
                      LocalDateTime submitTime, String result, String codeFile) {
        this.studentId = studentId;
        this.name = name;
        this.problemId = problemId;
        this.submitTime = submitTime;
        this.result = result;
        this.codeFile = codeFile;
    }

    /** 同一个学生 + 同一道题 = 一组，用于判断"有没有迭代过程"。 */
    public String groupKey() {
        return studentId + "#" + problemId;
    }

    @Override
    public String toString() {
        return studentId + " " + name + " " + problemId + " #" + order + " " + result;
    }
}
