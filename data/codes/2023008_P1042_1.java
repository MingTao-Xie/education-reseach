import java.util.Scanner;

public class StudentManager {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        String[] name = new String[n];
        double[] score = new double[n];
        for (int i = 0; i < n; i++) {
            name[i] = sc.next();
            score[i] = sc.nextDouble();
        }
        double sum = 0;
        for (int i = 0; i < n; i++) {
            sum += score[i];
        }
        for (int i = 0; i < n; i++) {
            System.out.println(name[i] + " " + score[i]);
        }
        System.out.println(sum / n);
    }
}
