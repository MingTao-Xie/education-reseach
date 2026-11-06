import java.util.Scanner;

public class StudentManager {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        String[] names = new String[n];
        int[] scores = new int[n];
        for (int i = 0; i < n; i++) {
            names[i] = sc.next();
            scores[i] = sc.nextInt();
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (scores[j] < scores[j + 1]) {
                    int t = scores[j];
                    scores[j] = scores[j + 1];
                    scores[j + 1] = t;
                    String s = names[j];
                    names[j] = names[j + 1];
                    names[j + 1] = s;
                }
            }
        }
        double sum = 0;
        for (int v : scores) {
            sum += v;
        }
        for (int i = 0; i < n; i++) {
            System.out.println(names[i] + " " + scores[i]);
        }
        System.out.printf("%.2f\n", sum / n);
    }
