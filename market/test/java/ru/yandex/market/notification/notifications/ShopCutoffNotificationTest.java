package ru.yandex.market.notification.notifications;

import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.shop.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

class ShopCutoffNotificationTest extends FunctionalTest {
    @Autowired
    @Qualifier("namedParameterJdbcTemplate")
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;

    @Test
    void basicTest() throws ParseException {
        var schedule = new CronNotificationSchedule("0 0 7 * * ?", ZoneId.of("Europe/Moscow"));
        PeriodicNotificationWithoutPreparation notification = new ShopCutoffNotification(schedule, jdbcTemplate, partnerTypeAwareService);

        assertThat(notification.getNotificationId()).isEqualTo("ShopCutoffNotification");
        assertThat(notification.getPartnerNotification(1L)).isEmpty();
        assertThat(notification.getPartnerIds()).isEmpty();

        Instant time = ZonedDateTime.parse("2021-02-03T14:41:45+03:00").toInstant();
        Instant nextTime = ZonedDateTime.parse("2021-02-04T07:00+03:00").toInstant();
        assertThat(notification.getNextNotificationTimeAfter(time)).isEqualTo(nextTime);
    }
}
