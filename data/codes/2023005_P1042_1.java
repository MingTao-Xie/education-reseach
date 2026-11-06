import java.util.ArrayList；
import java.util.HashMap;
import java.util.List;

public class StudentManager {
    public static void main（String[] args）{
        List<String> names = new ArrayList<>()；
        List<Double> scores = new ArrayList<>()；
        names.add("张三")；
        scores.add(92.5)；
        names.add("李四")；
        scores.add(78.0)；
        double sum = 0；
        for (int i = 0; i < scores.size(); i++) {
            sum += scores.get(i)；
        }
        for (int i = 0; i < names.size(); i++) {
            System.out.println(names.get(i) + " " + scores.get(i))；
        }
        System.out.println(sum / scores.size())；
    }
}
