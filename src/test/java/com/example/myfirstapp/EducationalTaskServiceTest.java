package com.example.myfirstapp;

import com.example.myfirstapp.dao.DatabaseManager;
import com.example.myfirstapp.dao.EducationalTaskDao;
import com.example.myfirstapp.dao.UserDao;
import com.example.myfirstapp.model.User;
import com.example.myfirstapp.service.EducationalTaskService;
import com.example.myfirstapp.service.ReleaseCoinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EducationalTaskServiceTest {
    private EducationalTaskService taskService;
    private User testUser;

    @BeforeEach
    void setUp() {
        String testDbUrl = "jdbc:sqlite:test_tasks_" + UUID.randomUUID() + ".db";
        DatabaseManager.resetWithUrl(testDbUrl);
        DatabaseManager dbManager = DatabaseManager.getInstance();

        EducationalTaskDao taskDao = new EducationalTaskDao(dbManager);
        UserDao userDao = new UserDao(dbManager);
        ReleaseCoinService releaseCoinService = new ReleaseCoinService();

        taskService = new EducationalTaskService(taskDao, userDao, releaseCoinService);
        testUser = userDao.findByUsername("admin").orElseThrow();
    }

    @Test
    @DisplayName("Study session restricts forward skipping while allowing backward seeking")
    void testForwardSkipLimiterAndBackwardSeek() {
        List<EducationalTaskService.TaskTemplate> tasks = taskService.getAvailableTasksForUser(testUser.getId());
        EducationalTaskService.TaskTemplate task = tasks.get(0); // 60-second task

        EducationalTaskService.StudySession session = new EducationalTaskService.StudySession(task);

        // Advance 10 seconds legitimately
        for (int i = 0; i < 10; i++) {
            session.tickSecond();
        }
        assertThat(session.getCurrentSeconds()).isEqualTo(10);
        assertThat(session.getMaxUnlockedSeconds()).isEqualTo(10);

        // Attempt forward skip by 20 seconds (from 10 to 30) -> permitted because <= 25s skip limit
        boolean allowedSkip = session.seekTo(30);
        assertThat(allowedSkip).isTrue();
        assertThat(session.getCurrentSeconds()).isEqualTo(30);

        // Attempt excessive forward skip by 50 seconds (from 30 to 80) -> MUST BE REJECTED!
        boolean excessiveSkip = session.seekTo(80);
        assertThat(excessiveSkip).isFalse();
        // Position should be clamped to maxUnlockedSeconds + 25 (30 + 25 = 55)
        assertThat(session.getCurrentSeconds()).isEqualTo(55);

        // Backward seek from 55 back to 15 to review material -> ALWAYS permitted!
        boolean backwardSeek = session.seekTo(15);
        assertThat(backwardSeek).isTrue();
        assertThat(session.getCurrentSeconds()).isEqualTo(15);
    }

    @Test
    @DisplayName("Completing educational task awards Release Coins and prevents duplicate reward")
    void testTaskCompletionAndDuplicatePrevention() {
        List<EducationalTaskService.TaskTemplate> tasks = taskService.getAvailableTasksForUser(testUser.getId());
        EducationalTaskService.TaskTemplate task = tasks.get(0);

        // Fails if watch duration is insufficient
        boolean premature = taskService.completeTask(testUser.getId(), task, 10, task.questions.size());
        assertThat(premature).isFalse();

        // Fails if quiz answers are incomplete or incorrect
        boolean failedQuiz = taskService.completeTask(testUser.getId(), task, 55, 0);
        assertThat(failedQuiz).isFalse();

        // Succeeds when both watch time and quiz answers satisfy requirements
        boolean successful = taskService.completeTask(testUser.getId(), task, 55, task.questions.size());
        assertThat(successful).isTrue();

        // Attempting to complete and earn coins for the SAME task again must be rejected
        boolean duplicate = taskService.completeTask(testUser.getId(), task, 60, task.questions.size());
        assertThat(duplicate).isFalse();
    }
}
