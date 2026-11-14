package ahocorasick;

import java.util.ArrayList;
import java.util.List;

public final class KMP {

    private KMP() {
    }

    public static int[] prefixFunction(String pattern) {
        int m = pattern.length();
        int[] pi = new int[m];
        int k = 0;
        for (int i = 1; i < m; i++) {
            while (k > 0 && pattern.charAt(i) != pattern.charAt(k)) {
                k = pi[k - 1];
            }
            if (pattern.charAt(i) == pattern.charAt(k)) {
                k++;
            }
            pi[i] = k;
        }
        return pi;
    }

    public static List<Integer> search(String text, String pattern) {
        List<Integer> result = new ArrayList<>();
        int[] pi = prefixFunction(pattern);
        int m = pattern.length();
        int q = 0;
        for (int i = 0; i < text.length(); i++) {
            while (q > 0 && text.charAt(i) != pattern.charAt(q)) {
                q = pi[q - 1];
            }
            if (text.charAt(i) == pattern.charAt(q)) {
                q++;
            }
            if (q == m) {
                result.add(i - m + 1);
                q = pi[q - 1];
            }
        }
        return result;
    }

    public static List<Match> searchAll(String text, String[] patterns) {
        List<Match> result = new ArrayList<>();
        for (int p = 0; p < patterns.length; p++) {
            for (int start : search(text, patterns[p])) {
                result.add(new Match(p, start));
            }
        }
        return result;
    }

    public static long countAll(String text, String[] patterns) {
        long total = 0;
        for (String pattern : patterns) {
            int[] pi = prefixFunction(pattern);
            int m = pattern.length();
            int q = 0;
            for (int i = 0; i < text.length(); i++) {
                while (q > 0 && text.charAt(i) != pattern.charAt(q)) {
                    q = pi[q - 1];
                }
                if (text.charAt(i) == pattern.charAt(q)) {
                    q++;
                }
                if (q == m) {
                    total++;
                    q = pi[q - 1];
                }
            }
        }
        return total;
    }
}
