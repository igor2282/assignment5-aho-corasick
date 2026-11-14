package ahocorasick;

public record Match(int patternIndex, int start) implements Comparable<Match> {

    @Override
    public int compareTo(Match other) {
        if (start != other.start) {
            return Integer.compare(start, other.start);
        }
        return Integer.compare(patternIndex, other.patternIndex);
    }
}
