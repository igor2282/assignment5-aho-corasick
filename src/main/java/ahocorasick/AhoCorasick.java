package ahocorasick;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AhoCorasick {

    private static final class Node {
        final Map<Character, Integer> children = new HashMap<>();
        int fail;
        int outputLink = -1;
        final List<Integer> patternIds = new ArrayList<>(1);
        int depth;
    }

    private final List<Node> nodes = new ArrayList<>();
    private final String[] patterns;

    public AhoCorasick(String[] patterns) {
        this.patterns = patterns.clone();
        nodes.add(new Node());
        for (int i = 0; i < this.patterns.length; i++) {
            insert(this.patterns[i], i);
        }
        buildFailureLinks();
    }

    private void insert(String pattern, int id) {
        if (pattern == null || pattern.isEmpty()) {
            throw new IllegalArgumentException("Patterns must be non-empty");
        }
        int current = 0;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            Integer next = nodes.get(current).children.get(c);
            if (next == null) {
                Node node = new Node();
                node.depth = nodes.get(current).depth + 1;
                nodes.add(node);
                next = nodes.size() - 1;
                nodes.get(current).children.put(c, next);
            }
            current = next;
        }
        nodes.get(current).patternIds.add(id);
    }

    private void buildFailureLinks() {
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        Node root = nodes.get(0);
        root.fail = 0;
        for (int child : root.children.values()) {
            nodes.get(child).fail = 0;
            queue.add(child);
        }
        while (!queue.isEmpty()) {
            int u = queue.poll();
            Node nodeU = nodes.get(u);
            for (Map.Entry<Character, Integer> entry : nodeU.children.entrySet()) {
                char c = entry.getKey();
                int v = entry.getValue();
                int f = nodeU.fail;
                while (f != 0 && !nodes.get(f).children.containsKey(c)) {
                    f = nodes.get(f).fail;
                }
                Integer target = nodes.get(f).children.get(c);
                int failV = (target != null && target != v) ? target : 0;
                Node nodeV = nodes.get(v);
                nodeV.fail = failV;
                Node failNode = nodes.get(failV);
                nodeV.outputLink = failNode.patternIds.isEmpty() ? failNode.outputLink : failV;
                queue.add(v);
            }
        }
    }

    private int step(int state, char c) {
        while (true) {
            Integer next = nodes.get(state).children.get(c);
            if (next != null) {
                return next;
            }
            if (state == 0) {
                return 0;
            }
            state = nodes.get(state).fail;
        }
    }

    public List<Match> search(String text) {
        List<Match> result = new ArrayList<>();
        int state = 0;
        for (int i = 0; i < text.length(); i++) {
            state = step(state, text.charAt(i));
            int out = nodes.get(state).patternIds.isEmpty() ? nodes.get(state).outputLink : state;
            while (out != -1) {
                Node node = nodes.get(out);
                for (int id : node.patternIds) {
                    result.add(new Match(id, i - patterns[id].length() + 1));
                }
                out = node.outputLink;
            }
        }
        return result;
    }

    public long count(String text) {
        long total = 0;
        int state = 0;
        for (int i = 0; i < text.length(); i++) {
            state = step(state, text.charAt(i));
            int out = nodes.get(state).patternIds.isEmpty() ? nodes.get(state).outputLink : state;
            while (out != -1) {
                Node node = nodes.get(out);
                total += node.patternIds.size();
                out = node.outputLink;
            }
        }
        return total;
    }

    public int nodeCount() {
        return nodes.size();
    }

    public String[] getPatterns() {
        return patterns.clone();
    }
}
