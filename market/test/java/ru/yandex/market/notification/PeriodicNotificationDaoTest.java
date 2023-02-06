package ru.yandex.market.notification;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DbUnitDataSet
class PeriodicNotificationDaoTest extends FunctionalTest {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    PeriodicNotificationDao dao;

    private final Instant time = Instant.ofEpochSecond(1612423292L);

    @Test
    void testNoNotificationsAfterTime() {
        assertEquals(Collections.emptySet(), dao.getTemplatesWithNextNotificationTimeAfter(time));
    }

    @Test
    void testInsert() {
        transactionTemplate.execute(status -> {
            dao.upsert("notification1Id", time);
            dao.upsert("notification2Id", time.minusSeconds(100));
            return null;
        });

        assertEquals(Collections.emptySet(), dao.getTemplatesWithNextNotificationTimeAfter(time));
        assertEquals(Set.of("notification1Id"), dao.getTemplatesWithNextNotificationTimeAfter(time.minusSeconds(1)));
        assertEquals(Set.of("notification1Id", "notification2Id"), dao.getTemplatesWithNextNotificationTimeAfter(time.minusSeconds(101)));
    }

    @Test
    void testUpdate() {
        dao.upsert("notification1Id", time);
        dao.upsert("notification2Id", time.plusSeconds(1));
        assertEquals(Set.of("notification2Id"), dao.getTemplatesWithNextNotificationTimeAfter(time));

        dao.upsert("notification1Id", time.plusSeconds(1));
        dao.upsert("notification2Id", time);
        assertEquals(Set.of("notification1Id"), dao.getTemplatesWithNextNotificationTimeAfter(time));
    }
}
