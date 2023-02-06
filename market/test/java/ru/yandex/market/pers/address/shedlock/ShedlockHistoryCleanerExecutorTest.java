package ru.yandex.market.pers.address.shedlock;


import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.address.model.ShedlockHistory;
import ru.yandex.market.pers.address.services.ShedlockHistoryService;
import ru.yandex.market.pers.address.util.BaseWebTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static ru.yandex.market.pers.address.model.ShedlockHistoryStatus.ERROR;
import static ru.yandex.market.pers.address.model.ShedlockHistoryStatus.IN_PROGRESS;
import static ru.yandex.market.pers.address.shedlock.ShedlockHistoryCleanerExecutor.SHEDLOCK_NAME;

public class ShedlockHistoryCleanerExecutorTest extends BaseWebTest {
    @Autowired
    private ShedlockHistoryService shedlockHistoryService;
    @Autowired
    private ShedlockHistoryCleanerExecutor shedlockHistoryCleanerExecutor;

    @Test
    public void shouldCleanLastMonthRows() {
        ShedlockHistory.ShedlockHistoryBuilder young = generateWithStartFinishTime(
                new DateTime().minusHours(2).toDate(),
                new Date());
        ShedlockHistory.ShedlockHistoryBuilder old = generateWithStartFinishTime(
                new DateTime().minusMonths(3).toDate(),
                new DateTime().minusMonths(2).toDate());
        young.withId(shedlockHistoryService.addShedlockHistory(young.build()));
        old.withId(shedlockHistoryService.addShedlockHistory(old.build()));
        shedlockHistoryCleanerExecutor.processDeleteOldShedlockHistory();
        List<ShedlockHistory> result = shedlockHistoryService.getShedlockHistories();

        assertThat(result, hasItem(young.build()));
        assertThat(result, not(hasItem(old.build())));
        assertThat(result, not(hasItem(hasProperty("status", equalTo(ERROR.getCode())))));
    }

    @Test
    public void shouldLogCleanLastMonthRows() {
        shedlockHistoryCleanerExecutor.processDeleteOldShedlockHistory();
        List<ShedlockHistory> result = shedlockHistoryService.getShedlockHistories();

        assertThat(result, contains(hasProperty("name", equalTo(SHEDLOCK_NAME))));
        assertThat(result, not(hasItem(hasProperty("status", equalTo(ERROR.getCode())))));
    }

    ShedlockHistory.ShedlockHistoryBuilder generateWithStartFinishTime(Date start, Date finish) {
        return ShedlockHistory.ShedlockHistoryBuilder.builder()
                .withStartTime(start)
                .withFinishTime(finish)
                .withName("test-name")
                .withStatus(IN_PROGRESS.getCode())
                .withLockedBy("host")
                .withTraceId("traceId");
    }
}
