package ru.yandex.market.core.notification.history.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PartnerLastNotificationServiceFunctionalTest extends FunctionalTest {

    @Autowired
    public PartnerLastNotificationService lastNotificationService;

    @Test
    @DbUnitDataSet(before = "PartnerLastNotificationServiceFunctionalTest.before.csv",
            after = "PartnerLastNotificationServiceFunctionalTest.after.csv")
    public void testRecordsDoNotRepeatAfterRepeatedCalls() {
        lastNotificationService.saveNewNotificationWasSent(1, 1);

        lastNotificationService.saveNewNotificationWasSent(1, 2);
        lastNotificationService.saveNewNotificationWasSent(1, 2);

        lastNotificationService.saveNewNotificationWasSent(2, 1);
        lastNotificationService.saveNewNotificationWasSent(2, 1);
        lastNotificationService.saveNewNotificationWasSent(2, 1);

        lastNotificationService.saveNewNotificationWasSent(2, 2);
        lastNotificationService.saveNewNotificationWasSent(2, 2);
        lastNotificationService.saveNewNotificationWasSent(2, 2);
        lastNotificationService.saveNewNotificationWasSent(2, 2);
    }

    @Test
    @DbUnitDataSet(before = "PartnerLastNotificationServiceFunctionalTest.before.csv")
    public void testCheckGoesCorrect() {
        lastNotificationService.saveNewNotificationWasSent(1, 1);

        boolean rightCheck = lastNotificationService.wasNotificationSent(1, 1);

        boolean falseCheck = lastNotificationService.wasNotificationSent(1, 2);
        boolean falseCheck2 = lastNotificationService.wasNotificationSent(2, 1);
        boolean falseCheck3 = lastNotificationService.wasNotificationSent(2, 2);

        assertThat(rightCheck, is(true));
        assertThat(falseCheck, is(false));
        assertThat(falseCheck2, is(false));
        assertThat(falseCheck3, is(false));
    }

    @Test
    @DbUnitDataSet(before = "PartnerLastNotificationServiceFunctionalTest.before.csv")
    public void testCheckWithDateGoesCorrect() {
        lastNotificationService.saveNewNotificationWasSent(1, 1);

        boolean rightCheck = lastNotificationService.wasNotificationSentAfter(1, 1, dayAgo());
        boolean falseCheck = lastNotificationService.wasNotificationSentAfter(1, 1, dayAfter());

        assertThat(rightCheck, is(true));
        assertThat(falseCheck, is(false));
    }

    private Instant dayAfter() {
        return Instant.now().plus(1, ChronoUnit.DAYS);
    }

    private Instant dayAgo() {
        return Instant.now().minus(1, ChronoUnit.DAYS);
    }
}
