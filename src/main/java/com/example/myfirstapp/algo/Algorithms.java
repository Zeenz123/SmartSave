package com.example.myfirstapp.algo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Algorithms: stable merge sort (O(n log n)), binary search (O(log n)) and
 * Levenshtein edit distance (dynamic programming, O(n*m)).
 */
public final class Algorithms {
    private Algorithms() {
    }

    public static <T> List<T> mergeSort(List<T> input, Comparator<? super T> cmp) {
        if (input.size() <= 1) return new ArrayList<>(input);
        int mid = input.size() / 2;
        List<T> left = mergeSort(input.subList(0, mid), cmp);
        List<T> right = mergeSort(input.subList(mid, input.size()), cmp);
        List<T> merged = new ArrayList<>(input.size());
        int i = 0, j = 0;
        while (i < left.size() && j < right.size()) {
            if (cmp.compare(left.get(i), right.get(j)) <= 0) merged.add(left.get(i++));
            else merged.add(right.get(j++));
        }
        while (i < left.size()) merged.add(left.get(i++));
        while (j < right.size()) merged.add(right.get(j++));
        return merged;
    }

    /** Returns the index of key in an ascending sorted list, or -(insertionPoint)-1 if absent. */
    public static <T> int binarySearch(List<T> sorted, T key, Comparator<? super T> cmp) {
        int lo = 0, hi = sorted.size() - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int c = cmp.compare(sorted.get(mid), key);
            if (c < 0) lo = mid + 1;
            else if (c > 0) hi = mid - 1;
            else return mid;
        }
        return -(lo + 1);
    }

    public static int editDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }
}
