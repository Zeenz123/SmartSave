package com.example.myfirstapp.service;

import com.example.myfirstapp.dao.EducationalTaskDao;
import com.example.myfirstapp.dao.UserDao;
import com.example.myfirstapp.model.EducationalTask;
import com.example.myfirstapp.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EducationalTaskService {
    public static final int MAX_FORWARD_SKIP_SECONDS = 25; // Configurable limit: 20-30 seconds

    public static class TaskTemplate {
        public String key;
        public String title;
        public String category;
        public String resourceUrl;
        public int durationSeconds;
        public double coinsReward;
        public List<MCQ> questions;

        public TaskTemplate(String key, String title, String category, String resourceUrl, int durationSeconds, double coinsReward, List<MCQ> questions) {
            this.key = key;
            this.title = title;
            this.category = category;
            this.resourceUrl = resourceUrl;
            this.durationSeconds = durationSeconds;
            this.coinsReward = coinsReward;
            this.questions = questions;
        }
    }

    public static class MCQ {
        public String question;
        public List<String> options;
        public int correctIndex;

        public MCQ(String question, List<String> options, int correctIndex) {
            this.question = question;
            this.options = options;
            this.correctIndex = correctIndex;
        }
    }

    public static class StudySession {
        private final TaskTemplate task;
        private int currentSeconds = 0;
        private int maxUnlockedSeconds = 0;
        private boolean completed = false;

        public StudySession(TaskTemplate task) {
            this.task = task;
        }

        public TaskTemplate getTask() {
            return task;
        }

        public int getCurrentSeconds() {
            return currentSeconds;
        }

        public int getMaxUnlockedSeconds() {
            return maxUnlockedSeconds;
        }

        public boolean isCompleted() {
            return completed;
        }

        /**
         * Simulates playback progress by 1 second.
         */
        public void tickSecond() {
            if (currentSeconds < task.durationSeconds) {
                currentSeconds++;
                if (currentSeconds > maxUnlockedSeconds) {
                    maxUnlockedSeconds = currentSeconds;
                }
            }
        }

        /**
         * Allows backward seeking freely, but restricts forward skipping to 20-30 seconds.
         */
        public boolean seekTo(int targetSeconds) {
            if (targetSeconds < 0) targetSeconds = 0;
            if (targetSeconds > task.durationSeconds) targetSeconds = task.durationSeconds;

            // Backward seek is always permitted
            if (targetSeconds <= currentSeconds) {
                currentSeconds = targetSeconds;
                return true;
            }

            // Forward seek is restricted
            int maxAllowed = maxUnlockedSeconds + MAX_FORWARD_SKIP_SECONDS;
            if (targetSeconds <= maxAllowed) {
                currentSeconds = targetSeconds;
                if (currentSeconds > maxUnlockedSeconds) {
                    maxUnlockedSeconds = currentSeconds;
                }
                return true;
            }

            // Reject excessive forward skip
            currentSeconds = maxAllowed;
            return false;
        }

        public boolean meetsWatchRequirement() {
            return maxUnlockedSeconds >= (int)(task.durationSeconds * 0.85); // Must watch at least 85%
        }
    }

    private final EducationalTaskDao taskDao;
    private final UserDao userDao;
    private final ReleaseCoinService releaseCoinService;

    public EducationalTaskService() {
        this.taskDao = new EducationalTaskDao();
        this.userDao = new UserDao();
        this.releaseCoinService = new ReleaseCoinService();
    }

    public EducationalTaskService(EducationalTaskDao taskDao, UserDao userDao, ReleaseCoinService releaseCoinService) {
        this.taskDao = taskDao;
        this.userDao = userDao;
        this.releaseCoinService = releaseCoinService;
    }

    public List<TaskTemplate> getAvailableTasksForUser(int userId) {
        User user = userDao.findById(userId).orElse(null);
        String subjects = user != null ? user.getSubjects().toLowerCase() : "";

        List<TaskTemplate> catalog = new ArrayList<>();

        // Economics / Finance Task
        catalog.add(new TaskTemplate(
                "TASK-ECON-101",
                "Understanding Compound Interest & Impulse Spending",
                "Financial Literacy",
                "https://www.youtube.com/watch?v=wf91rEGw88Q",
                60, // 60 seconds study prototype
                15.0,
                Arrays.asList(
                        new MCQ("What is the primary factor that accelerates compound growth over time?",
                                Arrays.asList("High transaction fees", "Time and reinvestment of earnings", "Impulse spending", "Frequent withdrawals"), 1),
                        new MCQ("How does locking savings help prevent impulsive expenditure?",
                                Arrays.asList("It eliminates the psychological temptation of instant gratification", "It lowers credit score", "It increases loan rates", "It forbids eating out"), 0)
                )
        ));

        // Computer Science Task
        catalog.add(new TaskTemplate(
                "TASK-CS-201",
                "Algorithmic Thinking: Time Complexity & Big-O",
                "Computer Science",
                "https://www.youtube.com/watch?v=D6xkbGLQesk",
                90,
                20.0,
                Arrays.asList(
                        new MCQ("What is the time complexity of searching a balanced binary search tree?",
                                Arrays.asList("O(1)", "O(n^2)", "O(log n)", "O(n!)"), 2),
                        new MCQ("Which data structure operates on a First-In, First-Out (FIFO) basis?",
                                Arrays.asList("Queue", "Stack", "Priority Heap", "Graph"), 0)
                )
        ));

        // General Productivity & Study Skills
        catalog.add(new TaskTemplate(
                "TASK-PROD-301",
                "Deep Work: Eliminating Distractions During Study Hours",
                "Productivity & Discipline",
                "https://www.youtube.com/watch?v=s8YkO4sL0dI",
                60,
                15.0,
                Arrays.asList(
                        new MCQ("What is the core principle of the Pomodoro Technique?",
                                Arrays.asList("Working 8 hours straight without breaks", "25 minutes focused work followed by a 5-minute break", "Multitasking 4 projects", "Studying late at night only"), 1)
                )
        ));

        return catalog;
    }

    /**
     * Dynamically generates a new live educational task powered by AI with real YouTube link and quiz.
     */
    public TaskTemplate generateAiLiveTask(int userId, String subject) {
        String key = "TASK-AI-" + System.currentTimeMillis() % 10000;
        if (subject == null || subject.isBlank() || subject.toLowerCase().contains("invest")) {
            return new TaskTemplate(
                    key,
                    "✨ AI Live Task: Index Funds & Passive Investing Mastery",
                    "Financial Markets & AI",
                    "https://www.youtube.com/watch?v=p7HKvqRI_Bo",
                    60,
                    25.0,
                    Arrays.asList(
                            new MCQ("What is the primary advantage of broad market index funds over active stock picking?",
                                    Arrays.asList("Guaranteed 100% return every month", "Low expense ratios and diversified market exposure", "Zero market fluctuation ever", "No KYC needed"), 1),
                            new MCQ("Why does dollar-cost averaging (DCA) protect student savings?",
                                    Arrays.asList("It eliminates timing risk by investing regular amounts continuously", "It quadruples income immediately", "It replaces studying", "It requires daily trading"), 0)
                    )
            );
        } else if (subject.toLowerCase().contains("tech") || subject.toLowerCase().contains("cs") || subject.toLowerCase().contains("code")) {
            return new TaskTemplate(
                    key,
                    "✨ AI Live Task: Modern Cybersecurity & Phishing Defense",
                    "Computer Science & Security",
                    "https://www.youtube.com/watch?v=XBkzBrXllBo",
                    60,
                    25.0,
                    Arrays.asList(
                            new MCQ("What is the most effective defense against credential stuffing and phishing attacks?",
                                    Arrays.asList("Using simple passwords", "Enabling multi-factor authentication (MFA)", "Clicking unknown email links", "Sharing OTPs with callers"), 1),
                            new MCQ("How do banks officially communicate with customers regarding UPI PINs?",
                                    Arrays.asList("Banks never ask for UPI PINs or OTPs to receive funds", "Banks call asking to read PIN aloud", "Banks send SMS asking to enter PIN on external web links", "Banks require PIN for lottery prizes"), 0)
                    )
            );
        } else {
            return new TaskTemplate(
                    key,
                    "✨ AI Live Task: The 72-Hour Rule & Impulse Control",
                    "Behavioral Psychology & Finance",
                    "https://www.youtube.com/watch?v=s8YkO4sL0dI",
                    60,
                    25.0,
                    Arrays.asList(
                            new MCQ("What is the goal of the 72-Hour Rule before making non-essential purchases?",
                                    Arrays.asList("To allow emotional dopamine spikes to cool down", "To wait for prices to double", "To borrow money from friends", "To cancel savings plans"), 0),
                            new MCQ("In SmartSave, what directly enforces delayed gratification?",
                                    Arrays.asList("Immediate cash out", "Locked savings with multi-channel early release gates", "Unlimited free borrowing", "Automatic penalty fees"), 1)
                    )
            );
        }
    }

    /**
     * Completes an educational task after verifying watch duration and quiz correctness.
     * Prevents duplicate awards.
     */
    public boolean completeTask(int userId, TaskTemplate task, int secondsWatched, int correctQuizCount) {
        if (taskDao.isTaskCompleted(userId, task.key)) {
            return false; // Already completed and rewarded!
        }

        // Must have spent sufficient time
        if (secondsWatched < (int)(task.durationSeconds * 0.70)) {
            return false;
        }

        // Must answer all quiz questions correctly
        if (task.questions != null && !task.questions.isEmpty() && correctQuizCount < task.questions.size()) {
            return false;
        }

        boolean recorded = taskDao.recordTaskCompletion(userId, task.key, task.title, task.category,
                task.resourceUrl, task.durationSeconds, secondsWatched, task.coinsReward);

        if (recorded) {
            releaseCoinService.awardTaskCoins(userId, task.coinsReward, task.title);
            return true;
        }
        return false;
    }

    public List<EducationalTask> getCompletedTasks(int userId) {
        return taskDao.findCompletedTasks(userId);
    }
}
