package com.example.myfirstapp.ai;

import com.example.myfirstapp.algo.Algorithms;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI layer: lightweight weighted keyword intent classifier that runs fully offline in Core Java.
 * Tokens are matched exactly or fuzzily (edit distance, so typos like "withdrwal" still work);
 * each intent accumulates a score and the highest-scoring intent wins.
 */
public class IntentClassifier {
    public enum Intent { EARLY_RELEASE, STUDY, COINS, TIPS, PROGRESS, INSIGHTS, GUARDIAN, UNKNOWN }

    private static final Set<String> STOP_WORDS = new HashSet<>(List.of(
            "the", "a", "an", "is", "are", "to", "of", "my", "me", "i", "do", "does", "how", "what", "can", "should", "for", "and", "in", "on", "it"));

    private final Map<Intent, Map<String, Double>> vocabulary = new EnumMap<>(Intent.class);

    public IntentClassifier() {
        add(Intent.EARLY_RELEASE, 2, "early", "withdraw", "withdrawal", "emergency", "unlock", "approval", "locked");
        add(Intent.STUDY, 2, "task", "study", "learn", "video", "quiz", "education", "subject");
        add(Intent.COINS, 2, "coin", "coins", "ad", "ads", "refer", "referral", "reward", "earn");
        add(Intent.TIPS, 2, "tip", "tips", "advice", "impulse", "budget", "discipline", "save");
        add(Intent.PROGRESS, 3, "progress", "forecast", "predict", "long", "when", "reach", "target", "goal", "days", "eta");
        add(Intent.INSIGHTS, 3, "insight", "insights", "analysis", "analyze", "spending", "spend", "unusual", "anomaly", "summary", "report", "history");
        add(Intent.GUARDIAN, 2, "guardian", "parent", "friend", "permission", "ward");
    }

    private void add(Intent intent, double weight, String... words) {
        Map<String, Double> m = vocabulary.computeIfAbsent(intent, k -> new HashMap<>());
        for (String w : words) m.put(w, weight);
    }

    public static List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        for (String t : text.toLowerCase().split("[^a-z0-9]+")) {
            if (!t.isEmpty() && !STOP_WORDS.contains(t)) out.add(t);
        }
        return out;
    }

    public Map<Intent, Double> score(String query) {
        Map<Intent, Double> scores = new EnumMap<>(Intent.class);
        for (String token : tokenize(query)) {
            for (Map.Entry<Intent, Map<String, Double>> e : vocabulary.entrySet()) {
                Double exact = e.getValue().get(token);
                if (exact != null) {
                    scores.merge(e.getKey(), exact, Double::sum);
                } else if (token.length() >= 5) {
                    for (Map.Entry<String, Double> kw : e.getValue().entrySet()) {
                        if (kw.getKey().length() >= 5 && Algorithms.editDistance(token, kw.getKey()) <= 1) {
                            scores.merge(e.getKey(), kw.getValue() * 0.7, Double::sum);
                            break;
                        }
                    }
                }
            }
        }
        return scores;
    }

    public Intent classify(String query) {
        Intent best = Intent.UNKNOWN;
        double bestScore = 1.0;
        for (Map.Entry<Intent, Double> e : score(query).entrySet()) {
            if (e.getValue() > bestScore) {
                best = e.getKey();
                bestScore = e.getValue();
            }
        }
        return best;
    }
}
