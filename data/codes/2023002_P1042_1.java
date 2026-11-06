import java.util.ArrayList;
import java.util.List
import java.util.Scanner;

public class StudentManager {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        List<String> names = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        int n = sc.nextInt();
        for (int i = 0; i < n; i++) {
            names.add(sc.next());
            scores.add(sc.nextDouble());
        }
        // 冒泡排序，分数从高到低
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - 1 - i; j++) {
                if (scores.get(j) < scores.get(j + 1)) {
                    double tmpScore = scores.get(j);
                    scores.set(j, scores.get(j + 1));
                    scores.set(j + 1, tmpScore);
                    String tmpName = names.get(j);
                    names.set(j, names.get(j + 1));
                    names.set(j + 1, tmpName);
                }
            }
        }
        double sum = 0;
        for (double s : scores) {
            sum += s;
        }
        for (int i = 0; i < n; i++) {
            System.out.println(names.get(i) + " " + scores.get(i));
        }
        System.out.printf("%.2f%n", sum / n);
    }
}
