package ahocorasick;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AhoCorasickTest {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        classicExample();
        overlappingMatches();
        patternIsSuffixOfAnother();
        patternIsPrefixOfAnother();
        duplicatePatterns();
        noMatches();
        emptyText();
        singleCharacterPatterns();
        emptyPatternRejected();
        kmpPrefixFunction();
        kmpBasicSearch();
        randomAgainstNaive();
        randomAgainstKmp();
        allRequiredSizes();

        System.out.println();
        System.out.println("Passed: " + passed + ", Failed: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("[PASS] " + name);
        } else {
            failed++;
            System.out.println("[FAIL] " + name);
        }
    }

    private static List<Match> naive(String text, String[] patterns) {
        List<Match> result = new ArrayList<>();
        for (int p = 0; p < patterns.length; p++) {
            int from = 0;
            while (true) {
                int idx = text.indexOf(patterns[p], from);
                if (idx < 0) {
                    break;
                }
                result.add(new Match(p, idx));
                from = idx + 1;
            }
        }
        Collections.sort(result);
        return result;
    }

    private static List<Match> sorted(List<Match> list) {
        List<Match> copy = new ArrayList<>(list);
        Collections.sort(copy);
        return copy;
    }

    private static void classicExample() {
        String[] patterns = {"he", "she", "his", "hers"};
        List<Match> actual = sorted(new AhoCorasick(patterns).search("ushers"));
        List<Match> expected = List.of(new Match(1, 1), new Match(0, 2), new Match(3, 2));
        check("classic he/she/his/hers on 'ushers'", actual.equals(expected));
    }

    private static void overlappingMatches() {
        String[] patterns = {"aa", "aaa"};
        List<Match> actual = sorted(new AhoCorasick(patterns).search("aaaa"));
        check("overlapping matches", actual.equals(naive("aaaa", patterns)) && actual.size() == 5);
    }

    private static void patternIsSuffixOfAnother() {
        String[] patterns = {"abcd", "bcd", "cd", "d"};
        List<Match> actual = sorted(new AhoCorasick(patterns).search("xabcdx"));
        check("output links report suffix patterns", actual.equals(naive("xabcdx", patterns)) && actual.size() == 4);
    }

    private static void patternIsPrefixOfAnother() {
        String[] patterns = {"a", "ab", "abc"};
        List<Match> actual = sorted(new AhoCorasick(patterns).search("abcab"));
        check("prefix patterns", actual.equals(naive("abcab", patterns)));
    }

    private static void duplicatePatterns() {
        String[] patterns = {"ab", "ab"};
        List<Match> actual = sorted(new AhoCorasick(patterns).search("abab"));
        check("duplicate patterns both reported", actual.size() == 4);
    }

    private static void noMatches() {
        String[] patterns = {"xyz", "qq"};
        check("no matches", new AhoCorasick(patterns).search("abcabcabc").isEmpty());
    }

    private static void emptyText() {
        check("empty text", new AhoCorasick(new String[]{"a"}).search("").isEmpty());
    }

    private static void singleCharacterPatterns() {
        String[] patterns = {"a", "b"};
        check("single character patterns", new AhoCorasick(patterns).search("abba").size() == 4);
    }

    private static void emptyPatternRejected() {
        boolean thrown = false;
        try {
            new AhoCorasick(new String[]{"a", ""});
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        check("empty pattern rejected", thrown);
    }

    private static void kmpPrefixFunction() {
        int[] pi = KMP.prefixFunction("ababaca");
        check("KMP prefix function", java.util.Arrays.equals(pi, new int[]{0, 0, 1, 2, 3, 0, 1}));
    }

    private static void kmpBasicSearch() {
        check("KMP search", KMP.search("abababab", "abab").equals(List.of(0, 2, 4)));
    }

    private static void randomAgainstNaive() {
        Random random = new Random(7);
        boolean ok = true;
        for (int t = 0; t < 200 && ok; t++) {
            String text = DataGenerator.randomText(1 + random.nextInt(300), random);
            String[] patterns = DataGenerator.patterns(1 + random.nextInt(30), text, random);
            ok = sorted(new AhoCorasick(patterns).search(text)).equals(naive(text, patterns));
        }
        check("200 random cases match naive search", ok);
    }

    private static void randomAgainstKmp() {
        Random random = new Random(11);
        boolean ok = true;
        for (int t = 0; t < 50 && ok; t++) {
            String text = DataGenerator.randomText(2000, random);
            String[] patterns = DataGenerator.patterns(50, text, random);
            ok = sorted(new AhoCorasick(patterns).search(text)).equals(sorted(KMP.searchAll(text, patterns)));
        }
        check("50 random cases match KMP", ok);
    }

    private static void allRequiredSizes() {
        int[] counts = {10, 100, 1000};
        int[] lengths = {1000, 10000, 100000};
        boolean ok = true;
        for (int p : counts) {
            for (int n : lengths) {
                Random random = new Random(p * 1000L + n);
                String text = DataGenerator.randomText(n, random);
                String[] patterns = DataGenerator.patterns(p, text, random);
                long ac = new AhoCorasick(patterns).count(text);
                long kmp = KMP.countAll(text, patterns);
                if (ac != kmp) {
                    ok = false;
                }
            }
        }
        check("AC and KMP agree for all 9 required size combinations", ok);
    }
}
