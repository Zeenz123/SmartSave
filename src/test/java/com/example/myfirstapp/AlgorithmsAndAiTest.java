package com.example.myfirstapp;

import com.example.myfirstapp.ai.AiInsightEngine;
import com.example.myfirstapp.ai.IntentClassifier;
import com.example.myfirstapp.algo.Algorithms;
import com.example.myfirstapp.algo.Trie;
import com.example.myfirstapp.algo.TransactionAnalytics;
import com.example.myfirstapp.model.Transaction;
import com.example.myfirstapp.model.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlgorithmsAndAiTest {
    private static Transaction tx(int sender, int recipient, double amt, Transaction.Type type, int daysAgo) {
        return new Transaction(0, "T", sender, recipient, amt, type, Transaction.Status.SUCCESS, "",
                LocalDateTime.now().minusDays(daysAgo));
    }

    @Test
    @DisplayName("Merge sort and binary search work on collections")
    void sortAndSearch() {
        List<Integer> sorted = Algorithms.mergeSort(List.of(5, 3, 9, 1, 7), Comparator.naturalOrder());
        assertThat(sorted).containsExactly(1, 3, 5, 7, 9);
        assertThat(Algorithms.binarySearch(sorted, 7, Comparator.naturalOrder())).isEqualTo(3);
        assertThat(Algorithms.binarySearch(sorted, 4, Comparator.naturalOrder())).isNegative();
        assertThat(Algorithms.editDistance("kitten", "sitting")).isEqualTo(3);
    }

    @Test
    @DisplayName("Trie returns prefix matches")
    void trie() {
        Trie t = new Trie();
        t.insert("Sarah Jenkins");
        t.insert("sam");
        t.insert("David");
        assertThat(t.withPrefix("sa", 10)).containsExactly("sam", "Sarah Jenkins");
        assertThat(t.contains("david")).isTrue();
    }

    @Test
    @DisplayName("Intent classifier routes queries, tolerating typos")
    void intents() {
        IntentClassifier c = new IntentClassifier();
        assertThat(c.classify("how does early withdrawal work")).isEqualTo(IntentClassifier.Intent.EARLY_RELEASE);
        assertThat(c.classify("when will I reach my target")).isEqualTo(IntentClassifier.Intent.PROGRESS);
        assertThat(c.classify("show my spending analysis")).isEqualTo(IntentClassifier.Intent.INSIGHTS);
        assertThat(c.classify("withdrwal emergancy")).isEqualTo(IntentClassifier.Intent.EARLY_RELEASE);
        assertThat(c.classify("blah")).isEqualTo(IntentClassifier.Intent.UNKNOWN);
    }

    @Test
    @DisplayName("Regression forecast and anomaly detection")
    void analytics() {
        List<Transaction> txs = new ArrayList<>();
        for (int d = 4; d >= 1; d--) txs.add(tx(0, 1, 100, Transaction.Type.DEPOSIT, d));
        assertThat(TransactionAnalytics.regressionSlope(List.of(100.0, 200.0, 300.0))).isEqualTo(100.0);
        assertThat(TransactionAnalytics.daysToTarget(400, 1000, 100)).isEqualTo(6);
        assertThat(TransactionAnalytics.daysToTarget(0, 100, Double.NaN)).isEqualTo(-1);

        for (int i = 0; i < 6; i++) txs.add(tx(1, 2, 10, Transaction.Type.TRANSFER, 0));
        txs.add(tx(1, 2, 500, Transaction.Type.TRANSFER, 0));
        assertThat(TransactionAnalytics.anomalies(txs, 1, 1.5)).hasSize(1);
        assertThat(TransactionAnalytics.topN(txs, 1).get(0).getAmount()).isEqualTo(500);

        Wallet w = new Wallet(1, 400, 0, 1000, 0);
        assertThat(new AiInsightEngine().forecast(w, txs.subList(0, 4), 1)).contains("day(s)");
    }
}
