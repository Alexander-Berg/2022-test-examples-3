package ru.yandex.market.notification.notifications;

import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.billing.cpa_auction.CpaAuctionBillingDao;
import ru.yandex.market.core.supplier.SupplierService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.shop.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;


class NettingNotificationServiceTest extends FunctionalTest {
    private PeriodicNotificationWithoutPreparation notification;
    private CronNotificationSchedule schedule;

    @Autowired
    private CpaAuctionBillingDao cpaAuctionBillingDao;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private EnvironmentService environmentService;

    @BeforeEach
    void setUp() throws ParseException {
        schedule = new CronNotificationSchedule("0 0 7 * * ?", ZoneId.of("Europe/Moscow"));
        notification = new NettingNotificationService(cpaAuctionBillingDao, schedule, supplierService,
                environmentService);
        environmentService.setValue(NettingNotificationService.DISABLE_NETTING_NOTIFICATIONS_MBI, "false");
    }

    @Test
    void testGetNotificationId() {
        assertThat(notification.getNotificationId()).isEqualTo("NettingNotification");
    }

    @Test
    @DisplayName("Тест на корректное получения списка партнеров")
    @DbUnitDataSet(before = "NettingNotificationServiceTest.before.csv")
    void testGetPartnerIds() {
        var partnerIds = notification.getPartnerIds();
        assertThat(partnerIds).containsExactlyInAnyOrder(1L, 7L);
    }

    @Test
    void testNextNotificationTimeAfter() {
        Instant time = Instant.parse("2021-02-04T11:41:45Z");

        Instant nextTime = schedule.getNextNotificationTimeAfter(time);
        assertThat(nextTime).isEqualTo(Instant.parse("2021-02-05T04:00:00Z"));

        Instant nextNextTime = schedule.getNextNotificationTimeAfter(nextTime);
        assertThat(nextNextTime).isEqualTo(Instant.parse("2021-02-06T04:00:00Z"));
    }
}
