package ahocorasick;

import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

public final class DataGenerator {

    public static final String ALPHABET = "abcdefgh";
    public static final int MIN_PATTERN_LENGTH = 4;
    public static final int MAX_PATTERN_LENGTH = 10;

    private DataGenerator() {
    }

    public static String randomText(int length, Random random) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public static String[] patterns(int count, String text, Random random) {
        Set<String> set = new LinkedHashSet<>();
        while (set.size() < count) {
            int len = MIN_PATTERN_LENGTH + random.nextInt(MAX_PATTERN_LENGTH - MIN_PATTERN_LENGTH + 1);
            if (random.nextBoolean() && text.length() >= len) {
                int start = random.nextInt(text.length() - len + 1);
                set.add(text.substring(start, start + len));
            } else {
                set.add(randomText(len, random));
            }
        }
        return set.toArray(new String[0]);
    }
}
