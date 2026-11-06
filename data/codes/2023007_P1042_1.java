import java.util.Scanner;

public class StudentManager {
    static String[] nm;
    static int[] sc;

    public static void main(String[] a) {
        Scanner r = new Scanner(System.in);
        int k = r.nextInt();
        nm = new String[k];
        sc = new int[k];
        for (int i = 0; i < k; i++) {
            nm[i] = r.next();
            sc[i] = r.nextInt();
        }
        for (int i = 0; i < k - 1; i++) {
            int mx = i;
            for (int j = i + 1; j < k; j++) {
                if (sc[j] > sc[mx]) mx = j;
            }
            int t1 = sc[i]; sc[i] = sc[mx]; sc[mx] = t1;
            String t2 = nm[i]; nm[i] = nm[mx]; nm[mx] = t2;
        }
        int sum = 0;
        for (int v : sc) sum += v;
        for (int i = 0; i < k; i++) {
            System.out.println(nm[i] + " " + sc[i]);
        }
        System.out.println(sum * 1.0 / k);
    }
}
