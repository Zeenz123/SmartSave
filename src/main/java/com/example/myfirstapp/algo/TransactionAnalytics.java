package com.example.myfirstapp.algo;

import com.example.myfirstapp.model.Transaction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * Collections + Algorithms layer: turns raw transaction rows into numbers the AI layer reasons over.
 * Uses EnumMap/TreeMap (aggregation), a min-heap (top-N) and least-squares regression (forecast).
 */
public final class TransactionAnalytics {
    private TransactionAnalytics() {
    }

    /** Signed effect of a transaction on the given user's total balance. */
    public static double netEffect(Transaction t, int userId) {
        if (t.getStatus() != Transaction.Status.SUCCESS) return 0;
        boolean sent = t.getSenderId() != null && t.getSenderId() == userId;
        boolean received = t.getRecipientId() != null && t.getRecipientId() == userId;
        switch (t.getType()) {
            case DEPOSIT:
                return received ? t.getAmount() : 0;
            case WITHDRAWAL:
                return sent ? -t.getAmount() : 0;
            case TRANSFER:
                return (received ? t.getAmount() : 0) - (sent ? t.getAmount() : 0);
            default:
                return 0;
        }
    }

    public static Map<Transaction.Type, Double> totalsByType(List<Transaction> txs) {
        Map<Transaction.Type, Double> totals = new EnumMap<>(Transaction.Type.class);
        for (Transaction t : txs) {
            if (t.getStatus() == Transaction.Status.SUCCESS) totals.merge(t.getType(), t.getAmount(), Double::sum);
        }
        return totals;
    }

    /** Top-N largest transactions using a bounded min-heap: O(n log N). */
    public static List<Transaction> topN(List<Transaction> txs, int n) {
        PriorityQueue<Transaction> heap = new PriorityQueue<>(Comparator.comparingDouble(Transaction::getAmount));
        for (Transaction t : txs) {
            heap.offer(t);
            if (heap.size() > n) heap.poll();
        }
        List<Transaction> out = new ArrayList<>(heap);
        out.sort(Comparator.comparingDouble(Transaction::getAmount).reversed());
        return out;
    }

    /** Cumulative net balance change per calendar day (ordered by date). */
    public static TreeMap<LocalDate, Double> cumulativeByDay(List<Transaction> txs, int userId) {
        TreeMap<LocalDate, Double> daily = new TreeMap<>();
        for (Transaction t : txs) {
            if (t.getCreatedAt() == null) continue;
            daily.merge(t.getCreatedAt().toLocalDate(), netEffect(t, userId), Double::sum);
        }
        double running = 0;
        for (Map.Entry<LocalDate, Double> e : daily.entrySet()) {
            running += e.getValue();
            e.setValue(running);
        }
        return daily;
    }

    /** Least-squares slope of y over x = 0..n-1. Returns NaN when fewer than 2 points. */
    public static double regressionSlope(List<Double> y) {
        int n = y.size();
        if (n < 2) return Double.NaN;
        double sx = 0, sy = 0, sxy = 0, sxx = 0;
        for (int i = 0; i < n; i++) {
            sx += i;
            sy += y.get(i);
            sxy += i * y.get(i);
            sxx += (double) i * i;
        }
        double denom = n * sxx - sx * sx;
        return denom == 0 ? Double.NaN : (n * sxy - sx * sy) / denom;
    }

    /** Estimated days until target is reached at the historical saving rate; -1 if unknown/never. */
    public static long daysToTarget(double currentWealth, double target, double slopePerDay) {
        if (currentWealth >= target) return 0;
        if (Double.isNaN(slopePerDay) || slopePerDay <= 0) return -1;
        return (long) Math.ceil((target - currentWealth) / slopePerDay);
    }

    /** Outflow transactions whose amount is more than {@code threshold} std-devs above the mean (z-score). */
    public static List<Transaction> anomalies(List<Transaction> txs, int userId, double threshold) {
        List<Transaction> outflows = new ArrayList<>();
        for (Transaction t : txs) {
            if (netEffect(t, userId) < 0) outflows.add(t);
        }
        List<Transaction> flagged = new ArrayList<>();
        if (outflows.size() < 4) return flagged;
        double mean = 0;
        for (Transaction t : outflows) mean += t.getAmount();
        mean /= outflows.size();
        double var = 0;
        for (Transaction t : outflows) var += (t.getAmount() - mean) * (t.getAmount() - mean);
        double sd = Math.sqrt(var / outflows.size());
        if (sd == 0) return flagged;
        for (Transaction t : outflows) {
            if ((t.getAmount() - mean) / sd > threshold) flagged.add(t);
        }
        return flagged;
    }
}
