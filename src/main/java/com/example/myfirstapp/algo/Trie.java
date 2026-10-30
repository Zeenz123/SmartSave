package com.example.myfirstapp.algo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Data Structure: Trie (prefix tree) for O(L) prefix lookup, used for autocomplete
 * of usernames / guardian names and AI vocabulary matching.
 */
public class Trie {
    private static final class Node {
        final Map<Character, Node> children = new TreeMap<>();
        boolean terminal;
        String word;
    }

    private final Node root = new Node();
    private int size;

    public void insert(String word) {
        if (word == null || word.isBlank()) return;
        String w = word.toLowerCase().trim();
        Node cur = root;
        for (char c : w.toCharArray()) {
            cur = cur.children.computeIfAbsent(c, k -> new Node());
        }
        if (!cur.terminal) size++;
        cur.terminal = true;
        cur.word = word.trim();
    }

    public boolean contains(String word) {
        Node n = find(word);
        return n != null && n.terminal;
    }

    /** Returns up to {@code limit} stored words beginning with the prefix, in alphabetical order. */
    public List<String> withPrefix(String prefix, int limit) {
        List<String> out = new ArrayList<>();
        Node n = find(prefix);
        if (n != null) collect(n, out, limit);
        return out;
    }

    public int size() {
        return size;
    }

    private Node find(String s) {
        if (s == null) return null;
        Node cur = root;
        for (char c : s.toLowerCase().trim().toCharArray()) {
            cur = cur.children.get(c);
            if (cur == null) return null;
        }
        return cur;
    }

    private void collect(Node n, List<String> out, int limit) {
        if (out.size() >= limit) return;
        if (n.terminal) out.add(n.word);
        for (Node child : n.children.values()) {
            collect(child, out, limit);
            if (out.size() >= limit) return;
        }
    }
}
