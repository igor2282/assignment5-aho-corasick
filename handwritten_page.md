# Aho–Corasick — one-page explanation (copy by hand)

**Idea.** Aho–Corasick finds all occurrences of many patterns in a text in one pass. It builds an automaton from the patterns and then reads the text from left to right, never going back.

**Step 1 — Trie.** I put all patterns into a trie. Each node is a prefix of some pattern; patterns with a common prefix share the same path. A node where a pattern ends is marked with that pattern. Example: he, she, his, hers → root–h–e–r–s, root–h–i–s, root–s–h–e.

**Step 2 — Failure links.** For every node v, fail(v) points to the node of the longest proper suffix of v's string that is also a prefix in the trie. I build them with BFS:
- children of the root → fail = root;
- for child v of u by letter c: go f = fail(u), and while f has no child c and f is not the root, go f = fail(f). If f has child c, fail(v) = that child, else root.
BFS is needed because fail(u) is always shallower, so it is already computed.
Example: fail("sh") = "h", fail("she") = "he".

**Step 3 — Output links.** out(v) = nearest node on the failure chain where a pattern ends. If fail(v) is a pattern end then out(v) = fail(v), else out(v) = out(fail(v)). This finds patterns that are suffixes of the current state, like "he" inside "she".

**Step 4 — Search.** Start at the root. For each text character c: while the current state has no child c and is not the root, follow the failure link; then take the child c (or stay at the root). Report the patterns of the current node and follow output links to report the others. A pattern of length L that ends at position i starts at i − L + 1.
Example "ushers": u → root, s → "s", h → "sh", e → "she" → report she, out → "he" → report he; r → "her", s → "hers" → report hers.

**Why it finds every pattern.** The state is always the longest pattern prefix that ends at the current position. A failure link only drops to a shorter suffix that can still match, so nothing is skipped, and output links list all shorter patterns that end here.

**Complexity.** n = text length, m = total pattern length, k = number of patterns, z = number of matches.
- Trie: O(m). Failure + output links: O(m) (amortised: depth grows by 1 per step, each failure jump decreases it).
- Search: O(n + z) — depth grows by at most 1 per character, so at most 2n moves; each match reported in O(1).
- Total O(m + n + z); space O(m) nodes (O(m·σ) with arrays per node).
- KMP for each pattern: O(m + k·n), because the text is read k times.

**Results.** With 1000 patterns and n = 100 000, AC took ≈ 4.7 ms and KMP ≈ 373 ms (≈ 79× faster). With 10 patterns and n = 1000 KMP was faster, because AC has a higher constant cost (trie, maps).
