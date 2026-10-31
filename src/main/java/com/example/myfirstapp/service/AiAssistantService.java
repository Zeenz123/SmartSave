package com.example.myfirstapp.service;

import com.example.myfirstapp.ai.AiInsightEngine;
import com.example.myfirstapp.ai.GeminiAiService;
import com.example.myfirstapp.ai.IntentClassifier;
import com.example.myfirstapp.dao.TransactionDao;
import com.example.myfirstapp.model.Transaction;
import com.example.myfirstapp.model.User;
import com.example.myfirstapp.model.Wallet;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Intelligent AI Assistant providing personalized study recommendations,
 * financial discipline guidance, and saving coaching.
 * Powered by Google Gemini API with fallback to local heuristic models.
 */
public class AiAssistantService {
    private final AuthService authService;
    private final WalletService walletService;
    private final TransactionDao transactionDao = new TransactionDao();
    private final IntentClassifier classifier = new IntentClassifier();
    private final AiInsightEngine insightEngine = new AiInsightEngine();
    private final GeminiAiService geminiService = GeminiAiService.getInstance();

    public AiAssistantService() {
        this.authService = AuthService.getInstance();
        this.walletService = new WalletService();
    }

    public AiAssistantService(AuthService authService, WalletService walletService) {
        this.authService = authService;
        this.walletService = walletService;
    }

    public String getPersonalizedGreeting(User user) {
        if (user == null) user = authService.getCurrentUser();
        if (user == null) return "Hello! I am your SmartSave AI Assistant. How can I help you today?";

        Wallet wallet = walletService.getWallet(user.getId());
        return String.format(Locale.getDefault(),
                "Hello %s! 👋\n" +
                "You have saved ₹%.2f towards your target of ₹%.2f (%d%% completed).\n" +
                "You also hold %.0f Release Coins!\n" +
                "Ask me about study tasks, early-release rules, or tips to beat impulse spending.",
                user.getFullName(), wallet.getTotalWealth(), wallet.getTargetBalance(),
                wallet.getProgressPercentage(), wallet.getReleaseCoins());
    }

    public String answerQuestion(String query, User user) {
        if (query == null || query.isBlank()) {
            return "Please enter a question so I can assist you!";
        }

        Wallet wallet = user != null ? walletService.getWallet(user.getId()) : null;
        List<Transaction> txs = user != null ? transactionDao.findByUserId(user.getId()) : Collections.emptyList();

        IntentClassifier.Intent intent = classifier.classify(query);

        if (intent == IntentClassifier.Intent.PROGRESS && user != null && wallet != null) {
            return geminiService.predictGoalTimeline(wallet, txs, user.getId(), insightEngine);
        }

        if (intent == IntentClassifier.Intent.INSIGHTS && user != null && wallet != null) {
            return geminiService.analyzeSpending(txs, wallet, insightEngine, user.getId());
        }

        // Check if Gemini can provide a live generative answer
        String geminiReply = geminiService.askAssistant(query, user, wallet, txs);
        if (geminiReply != null && !geminiReply.isBlank()) {
            return geminiReply;
        }

        if (intent == IntentClassifier.Intent.GUARDIAN) {
            return "🛡️ Guardians: your Parent / Close Friend can approve early release and target changes. Open the Guardian screen to add one, and to approve people who listed you as theirs.";
        }

        if (intent == IntentClassifier.Intent.EARLY_RELEASE) {
            return "🛡️ Early Release System Overview:\n" +
                   "Funds remain locked until your target balance is reached. In emergencies, you have 5 options:\n" +
                   "1. Parent Approval: Verified approval from your registered parent.\n" +
                   "2. Close Friend Approval: Sign-off from a trusted peer.\n" +
                   "3. Medical Emergency: Upload doctor report with AI verification.\n" +
                   "4. AI Study Tasks: Complete educational modules with forward-skip protection.\n" +
                   "5. Release Coins: Redeem coins at ₹1 per coin.";
        }

        if (intent == IntentClassifier.Intent.STUDY) {
            String sub = user != null ? user.getSubjects() : "General";
            return "📚 Recommended Study Focus:\n" +
                   "Based on your profile subjects (" + sub + "), I recommend:\n" +
                   "• Task 1: Understanding Compound Interest & Impulse Spending (+15 Coins)\n" +
                   "• Task 2: Algorithmic Thinking: Time Complexity & Big-O (+20 Coins)\n" +
                   "Remember: forward-skipping is capped at 25 seconds to foster real learning!";
        }

        if (intent == IntentClassifier.Intent.COINS) {
            return "🪙 Release Coins Rules:\n" +
                   "• 1 Release Coin = ₹1.00 of early release authority.\n" +
                   "• Watching a verified video ad earns up to 5 Coins (duplicate rewards prevented).\n" +
                   "• Sharing your referral code earns +25 Coins.\n" +
                   "• Solving educational quizzes earns 15–20 Coins.";
        }

        if (intent == IntentClassifier.Intent.TIPS) {
            return "💡 Smart Discipline Tip:\n" +
                   "Follow the '72-Hour Rule': whenever you feel an urge to make a non-essential purchase, lock that amount in your SmartSave vault for 3 days. In 85% of cases, the impulse subsides and your savings stay intact!";
        }

        return "🤖 AI Assistant Response:\n" +
               "I'm here to help you stay disciplined with your finances and studies! You can ask me how to earn Release Coins, how early withdrawal approvals work, or how to set personalized learning goals.";
    }
}
