/**
 * 以下是完整的 StudentManager 实现，满足题目全部要求。
 * 说明：本题采用集合存储学生信息，排序使用 Comparator 接口。
 */
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 学生成绩管理系统
 * 支持添加学生、按分数排序、计算平均分
 */
public class StudentManager {

    /** 学生实体类 */
    private static class Student {
        private final String name;
        private final double score;

        Student(String name, double score) {
            this.name = name;
            this.score = score;
        }

        @Override
        public String toString() {
            return name + " " + score;
        }
    }

    private final List<Student> students = new ArrayList<>();

    /** 添加学生 */
    public void addStudent(String name, double score) {
        students.add(new Student(name, score));
    }

    /** 按分数从高到低排序 */
    public List<Student> sortByScore() {
        List<Student> copy = new ArrayList<>(students);
        copy.sort(Comparator.comparingDouble((Student s) -> s.score).reversed());
        return copy;
    }

    /** 计算平均分 */
    public double average() {
        if (students.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (Student s : students) {
            sum += s.score;
        }
        return sum / students.size();
    }

    public static void main(String[] args) {
        StudentManager manager = new StudentManager();
        manager.addStudent("张三", 92.5);
        manager.addStudent("李四", 78.0);
        manager.addStudent("王五", 85.5);
        for (Student s : manager.sortByScore()) {
            System.out.println(s);
        }
        System.out.println("平均分: " + manager.average());
    }
}
