package ahocorasick;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;

public class Benchmark {

    private static final int[] PATTERN_COUNTS = {10, 100, 1000};
    private static final int[] TEXT_LENGTHS = {1000, 10000, 100000};
    private static final int RUNS = 5;
    private static final int WARMUP = 3;
    private static final long SEED = 42L;

    private static long sink;

    public static void main(String[] args) throws IOException {
        Path outDir = Path.of(args.length > 0 ? args[0] : "results");
        Files.createDirectories(outDir);

        StringBuilder csv = new StringBuilder();
        csv.append("patterns,textLength,trieNodes,matches,acBuildMs,acSearchMs,acTotalMs,kmpTotalMs,speedup\n");
        StringBuilder md = new StringBuilder();
        md.append("| Patterns | Text length | Trie nodes | Matches | AC build (ms) | AC search (ms) | AC total (ms) | KMP x patterns (ms) | KMP / AC |\n");
        md.append("|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");

        System.out.printf("%-9s %-11s %-10s %-9s %-12s %-13s %-12s %-12s %-8s%n",
                "patterns", "textLength", "nodes", "matches", "acBuild", "acSearch", "acTotal", "kmpTotal", "speedup");

        Random warmRandom = new Random(1);
        String warmText = DataGenerator.randomText(20000, warmRandom);
        String[] warmPatterns = DataGenerator.patterns(200, warmText, warmRandom);
        for (int w = 0; w < 30; w++) {
            sink += new AhoCorasick(warmPatterns).count(warmText);
            sink += KMP.countAll(warmText, warmPatterns);
        }

        for (int p : PATTERN_COUNTS) {
            for (int n : TEXT_LENGTHS) {
                Random random = new Random(SEED + p * 31L + n);
                String text = DataGenerator.randomText(n, random);
                String[] patterns = DataGenerator.patterns(p, text, random);

                long acMatches = new AhoCorasick(patterns).count(text);
                long kmpMatches = KMP.countAll(text, patterns);
                if (acMatches != kmpMatches) {
                    throw new IllegalStateException("Mismatch: AC=" + acMatches + " KMP=" + kmpMatches);
                }

                for (int w = 0; w < WARMUP; w++) {
                    sink += new AhoCorasick(patterns).count(text);
                    sink += KMP.countAll(text, patterns);
                }

                long buildSum = 0;
                long searchSum = 0;
                long kmpSum = 0;
                int nodes = 0;
                for (int r = 0; r < RUNS; r++) {
                    long t0 = System.nanoTime();
                    AhoCorasick ac = new AhoCorasick(patterns);
                    long t1 = System.nanoTime();
                    sink += ac.count(text);
                    long t2 = System.nanoTime();
                    sink += KMP.countAll(text, patterns);
                    long t3 = System.nanoTime();
                    buildSum += t1 - t0;
                    searchSum += t2 - t1;
                    kmpSum += t3 - t2;
                    nodes = ac.nodeCount();
                }

                double build = buildSum / (double) RUNS / 1e6;
                double search = searchSum / (double) RUNS / 1e6;
                double total = build + search;
                double kmp = kmpSum / (double) RUNS / 1e6;
                double speedup = kmp / total;

                csv.append(String.format(Locale.US, "%d,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.2f%n",
                        p, n, nodes, acMatches, build, search, total, kmp, speedup));
                md.append(String.format(Locale.US, "| %d | %d | %d | %d | %.3f | %.3f | %.3f | %.3f | %.2fx |%n",
                        p, n, nodes, acMatches, build, search, total, kmp, speedup));
                System.out.printf(Locale.US, "%-9d %-11d %-10d %-9d %-12.3f %-13.3f %-12.3f %-12.3f %.2fx%n",
                        p, n, nodes, acMatches, build, search, total, kmp, speedup);
            }
        }

        Files.writeString(outDir.resolve("results.csv"), csv.toString());
        Files.writeString(outDir.resolve("results.md"), md.toString());
        System.out.println("checksum " + sink);
    }
}
