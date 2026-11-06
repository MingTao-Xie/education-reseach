import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 成绩管理器
 */
public class StudentManager {

    private static class Stu {
        private final String name;
        private final double score;

        Stu(String name, double score) {
            this.name = name;
            this.score = score;
        }

        @Override
        public String toString() {
            return name + " " + score;
        }
    }

    private final List<Stu> list = new ArrayList<>();

    public void addStudent(String name, double score) {
        list.add(new Stu(name, score));
    }

    public List<Stu> sortByScore() {
        List<Stu> copy = new ArrayList<>(list);
        copy.sort(Comparator.comparingDouble((Stu s) -> s.score).reversed());
        return copy;
    }

    public double average() {
        if (list.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (Stu s : list) {
            sum += s.score;
        }
        return sum / list.size();
    }

    public static void main(String[] args) {
        StudentManager m = new StudentManager();
        m.addStudent("张三", 92.5);
        m.addStudent("李四", 78.0);
        m.addStudent("王五", 85.5);
        for (Stu s : m.sortByScore()) {
            System.out.println(s);
        }
        System.out.println("平均分: " + m.average());
    }
}
