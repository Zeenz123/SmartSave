package com.example.myfirstapp.ai;

import com.example.myfirstapp.algo.TransactionAnalytics;
import com.example.myfirstapp.model.Transaction;
import com.example.myfirstapp.model.Wallet;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * AI layer: data-driven savings coach. Combines Collections + Algorithms output
 * (regression forecast, z-score anomaly detection, aggregation) into plain-language insights.
 */
public class AiInsightEngine {

    public String forecast(Wallet wallet, List<Transaction> txs, int userId) {
        double wealth = wallet.getTotalWealth();
        double target = wallet.getTargetBalance();
        if (wealth >= target) {
            return String.format(Locale.US, "🎉 You have already reached your target of ₹%.2f. Direct withdrawal is unlocked!", target);
        }
        TreeMap<LocalDate, Double> byDay = TransactionAnalytics.cumulativeByDay(txs, userId);
        double slope = TransactionAnalytics.regressionSlope(new ArrayList<>(byDay.values()));
        long days = TransactionAnalytics.daysToTarget(wealth, target, slope);
        if (days < 0) {
            return String.format(Locale.US,
                    "📈 You are at ₹%.2f of ₹%.2f (%d%%). I need at least two days of positive saving activity to forecast your finish date - keep depositing!",
                    wealth, target, wallet.getProgressPercentage());
        }
        return String.format(Locale.US,
                "📈 Progress: ₹%.2f of ₹%.2f (%d%%). At your current saving pace of about ₹%.2f/day, you should reach your target in ~%d day(s) (%s).",
                wealth, target, wallet.getProgressPercentage(), slope, days, LocalDate.now().plusDays(days));
    }

    public String insights(List<Transaction> txs, int userId) {
        if (txs.isEmpty()) {
            return "🔍 No transactions yet - add some cash or make a transfer and I will analyze your habits.";
        }
        StringBuilder sb = new StringBuilder("🔍 Spending & Saving Insights:\n");
        for (Map.Entry<Transaction.Type, Double> e : TransactionAnalytics.totalsByType(txs).entrySet()) {
            sb.append(String.format(Locale.US, "• %s: ₹%.2f\n", e.getKey().name().replace('_', ' '), e.getValue()));
        }
        List<Transaction> top = TransactionAnalytics.topN(txs, 3);
        sb.append("Largest movements: ");
        for (int i = 0; i < top.size(); i++) {
            sb.append(String.format(Locale.US, "%s₹%.2f (%s)", i > 0 ? ", " : "", top.get(i).getAmount(), top.get(i).getType()));
        }
        List<Transaction> odd = TransactionAnalytics.anomalies(txs, userId, 1.5);
        if (!odd.isEmpty()) {
            sb.append("\n⚠️ Unusually large outflow detected: ");
            for (Transaction t : odd) sb.append(String.format(Locale.US, "₹%.2f (%s) ", t.getAmount(), t.getType()));
            sb.append("- consider locking that amount next time.");
        }
        return sb.toString();
    }
}
