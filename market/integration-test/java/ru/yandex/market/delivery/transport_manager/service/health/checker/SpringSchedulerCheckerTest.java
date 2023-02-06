package ru.yandex.market.delivery.transport_manager.service.health.checker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.util.TestSpringScheduler;

class SpringSchedulerCheckerTest extends AbstractContextualTest {
    @Autowired
    private SpringSchedulerChecker springSchedulerChecker;
    @Autowired
    private TestSpringScheduler testSpringScheduler;

    @AfterEach
    void tearDown() {
        testSpringScheduler.setThrowEx(false);
    }

    @Test
    public void testOk() {
        softly.assertThat(springSchedulerChecker.getIssues()).isEqualTo("0;OK");
    }

    @Test
    public void setFail() throws InterruptedException {
        testSpringScheduler.setThrowEx(true);
        Thread.sleep(500);
        softly.assertThat(springSchedulerChecker.getIssues())
            .isEqualTo("2;TestSpringScheduler.doSomething: Error message sample");
    }
}
