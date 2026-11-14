# Aho–Corasick Multi-Pattern String Matching

Java 17+ implementation of the Aho–Corasick algorithm with experimental comparison against running KMP separately for every pattern.

## Project structure

```
aho-corasick/
├── pom.xml
├── run.sh
├── src/main/java/ahocorasick/
│   ├── AhoCorasick.java     trie, failure links, output links, search
│   ├── KMP.java             prefix function + single-pattern search
│   ├── Match.java           (patternIndex, start) result record
│   ├── DataGenerator.java   reproducible random texts and patterns
│   ├── Benchmark.java       9 experiments x 5 runs, System.nanoTime()
│   └── Main.java            small demo (he / she / his / hers)
├── src/test/java/ahocorasick/
│   └── AhoCorasickTest.java correctness tests
└── results/
    ├── results.csv
    └── results.md
```

## How to run

```bash
./run.sh
```

or manually:

```bash
javac -d out src/main/java/ahocorasick/*.java src/test/java/ahocorasick/*.java
java -cp out ahocorasick.Main
java -cp out ahocorasick.AhoCorasickTest
java -cp out ahocorasick.Benchmark results
```

With Maven: `mvn compile`, then `java -cp target/classes ahocorasick.Benchmark`.

## Implementation

### 1. Trie construction
Every pattern is inserted character by character starting from the root. Each node stores a map `char -> child`, its depth and the list of pattern ids that end in it. Common prefixes are shared, so the trie has at most `m + 1` nodes, where `m` is the total length of all patterns.

### 2. Failure links
Failure links are built with BFS, level by level, so that when a node is processed the links of all shallower nodes are already known.

- Children of the root fail to the root.
- For a node `v` reached from `u` by character `c`: start at `f = fail(u)` and follow failure links until a node with a child on `c` is found (or the root is reached). `fail(v)` is that child, otherwise the root.

`fail(v)` is the node representing the **longest proper suffix** of the string of `v` that is also a prefix of some pattern.

### 3. Output links
`outputLink(v)` points to the nearest node on the failure chain of `v` where some pattern ends (or `-1`). It is computed during the same BFS:

```
outputLink(v) = fail(v)              if a pattern ends in fail(v)
              = outputLink(fail(v))  otherwise
```

When the automaton is in state `v`, all patterns ending at the current position are: the patterns of `v` itself and the patterns of `outputLink(v)`, `outputLink(outputLink(v))`, ... This visits only nodes that really contain matches, so reporting costs `O(1)` per match.

### 4. Search
The text is scanned once from left to right. For each character the automaton moves to the child on that character; on a mismatch it follows failure links until a transition exists or the root is reached. After the transition, all matches are collected through the output links. The text pointer never moves back.

Demo output (`Main`):

```
Text: ushers
Patterns: he, she, his, hers
Trie nodes: 10
  "she" at [1, 3]
  "he" at [2, 3]
  "hers" at [2, 5]
```

## Tests

`AhoCorasickTest` (14 checks, all passing):

- classic `he / she / his / hers` example
- overlapping matches (`aa`, `aaa` in `aaaa`)
- patterns that are suffixes of each other (checks output links)
- patterns that are prefixes of each other
- duplicate patterns, no matches, empty text, single-character patterns, empty pattern rejected
- KMP prefix function and KMP search
- 200 random cases compared with naive `indexOf` search
- 50 random cases compared with KMP
- all 9 required size combinations: Aho–Corasick and KMP report the same number of matches

## Experimental setup

- Pattern counts: **10, 100, 1,000**
- Text lengths: **1,000, 10,000, 100,000** characters
- Alphabet: 8 letters (`a`–`h`), pattern length 4–10; half of the patterns are substrings of the text so that matches actually occur
- Fixed random seed, so results are reproducible
- JVM warm-up before measurements, plus 3 warm-up runs per case
- Each case is run **5 times** with `System.nanoTime()`; the table shows the **average**
- AC time = trie + failure link construction + search. KMP time = prefix function + search for every pattern separately
- Before timing, the number of matches of both algorithms is compared; the benchmark stops if they differ

Environment: Windows, PowerShell, JDK 17+.

## Results

| Patterns | Text length | Trie nodes | Matches | AC build (ms) | AC search (ms) | AC total (ms) | KMP x patterns (ms) | KMP / AC |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 10 | 1000 | 66 | 5 | 0.022 | 0.110 | 0.131 | 0.047 | 0.36x |
| 10 | 10000 | 52 | 7 | 0.047 | 0.536 | 0.582 | 0.440 | 0.76x |
| 10 | 100000 | 77 | 6 | 0.050 | 2.262 | 2.312 | 4.561 | 1.97x |
| 100 | 1000 | 555 | 69 | 0.553 | 0.040 | 0.593 | 0.607 | 1.02x |
| 100 | 10000 | 547 | 81 | 0.454 | 0.314 | 0.768 | 4.914 | 6.40x |
| 100 | 100000 | 584 | 407 | 0.544 | 2.243 | 2.787 | 38.603 | 13.85x |
| 1000 | 1000 | 4057 | 541 | 2.996 | 0.125 | 3.121 | 4.313 | 1.38x |
| 1000 | 10000 | 4321 | 894 | 0.983 | 0.395 | 1.378 | 36.524 | 26.51x |
| 1000 | 100000 | 4349 | 4753 | 0.960 | 3.769 | 4.729 | 372.747 | 78.81x |

Raw data: [`results/results.csv`](results/results.csv). Absolute times depend on the machine; the trends are what matter.

### Observations

- **AC search time depends on the text length, not on the number of patterns.** For n = 100,000 the search takes about 2.3 ms with 10 patterns, 2.2 ms with 100 and 3.8 ms with 1,000. The small growth comes from more reported matches and larger hash maps / worse cache locality, not from extra passes over the text.
- **KMP time grows linearly with the number of patterns.** For n = 100,000: 4.6 ms → 38.6 ms → 373 ms, roughly ×10 each time the pattern count grows ×10, because the text is scanned once per pattern.
- **Build time depends only on the patterns** (~0.05 ms for 10, ~0.5 ms for 100, ~1–3 ms for 1,000) and does not change with text length.
- **Speed-up grows with both dimensions**: from 0.36× (AC is slower) for 10 patterns / 1,000 characters up to **79×** for 1,000 patterns / 100,000 characters.
- **For few patterns and short texts KMP wins.** With 10 patterns on texts of 1,000 and 10,000 characters KMP was faster (0.36× and 0.76×). KMP uses simple array lookups, while AC pays for hash-map transitions, object nodes and trie construction. The constant factor of AC is higher, so it only pays off once the number of patterns is large enough.

## Complexity analysis

Notation: `n` = text length, `k` = number of patterns, `m` = total length of all patterns, `z` = number of reported matches, `σ` = alphabet size.

### Aho–Corasick

| Phase | Time | Explanation |
|---|---|---|
| Trie construction | `O(m)` | each pattern character is processed once, hash-map insert/lookup is `O(1)` expected |
| Failure + output links | `O(m)` | BFS visits each node once; along one pattern path the failure depth can increase by at most 1 per step and every failure jump decreases it, so total failure jumps are bounded by `m` (amortised argument, same as KMP) |
| Search | `O(n + z)` | each character increases the depth by at most 1 and every failure jump decreases it, so at most `2n` transitions; output links visit only nodes with matches, `O(1)` per match |
| **Total** | **`O(m + n + z)`** | |

**Space:** `O(m)` nodes; each node stores a map of children, so the total number of edges is `O(m)`. With a fixed array of size σ per node it would be `O(m·σ)` but with `O(1)` worst-case transitions. Output storage is `O(z)` if all matches are collected (the benchmark only counts them).

In our experiments the trie had 4,349 nodes for 1,000 patterns with a total length of 6,961 characters, showing how shared prefixes reduce the size below `m`.

### KMP run separately for each pattern

| Phase | Time |
|---|---|
| Prefix function for pattern `i` | `O(mᵢ)` |
| Search for pattern `i` | `O(n)` |
| **Total for k patterns** | **`O(m + k·n)`** |

**Space:** `O(max mᵢ)` at a time (one prefix array), plus `O(z)` for results.

### Comparison

The difference is `O(n + z)` vs `O(k·n)`: Aho–Corasick reads the text once regardless of `k`, while KMP reads it `k` times. With `k = 1000` and `n = 100,000` that is 100,000 vs 100,000,000 character steps, which matches the measured ~79× gap.

## Roles of the trie and failure links

- **Trie** merges all patterns into one structure. Shared prefixes are stored once, and the state of the automaton means "the longest pattern prefix that ends at the current text position". This lets all patterns be checked at the same time in a single pass.
- **Failure links** answer "where should I continue on a mismatch without moving back in the text?". They point to the longest suffix of the current match that is still a prefix of some pattern, so no possible match is lost and no character is re-read. It is the multi-pattern generalisation of the KMP prefix function.
- **Output links** handle patterns that are suffixes of the current state (for example `he` inside `she`). Without them such matches would be missed, or every failure chain would have to be walked at every position.

## Why Aho–Corasick suits multiple patterns

- One linear pass over the text, independent of the number of patterns.
- Preprocessing is linear in the total pattern length and is done once, so the same automaton can scan many texts or a stream.
- It finds every occurrence, including overlapping ones and patterns nested in each other.
- It works online: characters can be fed one by one (network packets, logs).

Typical uses: antivirus signature scanning, intrusion detection (Snort), keyword/spam filters, `grep -F` / `fgrep`, DNA motif search.

## Advantages and limitations

| | Aho–Corasick | KMP per pattern |
|---|---|---|
| Time | `O(m + n + z)` | `O(m + k·n)` |
| Text passes | 1 | k |
| Best for | many patterns, long texts, streams, repeated scans | one or very few patterns |
| Memory | whole trie, `O(m)` nodes with maps (or `O(m·σ)` with arrays) | one prefix array |
| Implementation | more complex: trie, BFS, two kinds of links | short and simple |
| Constant factor | higher (objects, hash maps, pointer chasing) | very low (arrays, sequential access) |
| Adding a pattern | requires rebuilding links | independent, just run once more |

**Aho–Corasick limitations:** higher memory use, especially with large alphabets (Unicode) if arrays are used; the automaton must be rebuilt when the pattern set changes; for 1–10 patterns and short texts the overhead makes it slower than KMP (0.36× in our 10 × 1,000 case); the output size `z` can be large when patterns overlap a lot.

**KMP limitations:** cost grows linearly with the number of patterns; the text must be scanned k times, which is impractical for streams; no sharing of work between patterns with common prefixes.

## Conclusion

The experiments confirm the theory: Aho–Corasick search time stays almost flat as the number of patterns grows from 10 to 1,000, while KMP time grows linearly with it. For 1,000 patterns on a 100,000-character text Aho–Corasick was about 79 times faster (4.7 ms vs 373 ms). For a small number of patterns on short texts KMP remains the simpler and faster choice.
