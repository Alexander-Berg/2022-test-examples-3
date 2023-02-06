package ru.yandex.market.sc.tms.queue;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.tms.domain.queue.QueueLogTableCleaner;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;
import ru.yandex.market.tpl.common.db.queue.log.QueueLog;
import ru.yandex.market.tpl.common.db.queue.log.QueueLogEvent;
import ru.yandex.market.tpl.common.db.queue.log.QueueLogRepository;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTmsTest
public class QueueLogTableCleanerTest {

    @Autowired
    QueueLogTableCleaner queueLogTableCleaner;
    @Autowired
    QueueLogRepository queueLogRepository;
    @Autowired
    TestFactory testFactory;
    @MockBean
    Clock clock;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setupMockClock(clock, Instant.now());
    }

    @Test
    void cleanLog() {
        createQueueLog(1L);
        assertThat(queueLogRepository.findAll().size()).isEqualTo(1);
        testFactory.setupMockClock(clock, clock.instant().plus(15, ChronoUnit.DAYS));
        queueLogTableCleaner.clean();
        assertThat(queueLogRepository.findAll().size()).isEqualTo(0);
    }

    @Test
    void doNotCleanNewLogEntries() {
        createQueueLog(1L);
        assertThat(queueLogRepository.findAll().size()).isEqualTo(1);
        queueLogTableCleaner.clean();
        assertThat(queueLogRepository.findAll().size()).isEqualTo(1);
    }

    private void createQueueLog(Long taskId) {
        queueLogRepository.save(new QueueLog()
                .setTaskId(taskId)
                .setQueueName("test")
                .setEvent(QueueLogEvent.SUCCESSFUL)
        );
    }
}
