package ru.yandex.market.logistics.cs.notifications;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.cs.service.CapacityValueCounterService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("Тестирование игнорирования замьюченных нотификаций")
public class MutedCounterNotificationsTest extends CommonCapacityCounterNotificationTest {

    @Autowired
    private CapacityValueCounterService counterService;

    @Autowired
    private TestableClock clock;

    private static final ZoneId UTC = ZoneId.of("UTC");

    @BeforeEach
    public void prepare() {
        clock.setFixed(
            LocalDateTime.now(UTC)
                .withHour(12)
                .withMinute(0)
                .truncatedTo(ChronoUnit.MINUTES)
                .toInstant(ZoneOffset.UTC),
            UTC
        );
    }

    @DisplayName("Проверка игнорирования уведомлений")
    @DatabaseSetup(
        value = "/repository/notifications/muted/before/before_notification_send.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/repository/notifications/muted/after/after_muted_not_send.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void shouldSendFirstTest() {
        updateCountersWithAmountAndUnitType(5, 600, DAY);

        waitUntilCounterTasksFinished(2);
        refreshQueue();

        counterService.updateThresholdsAccordingToCorrespondingValues(List.of(104L));

        updateCountersWithAmountAndUnitType(295, 0, DAY);

        waitUntilCounterTasksFinished(2);
    }

    @Test
    @DatabaseSetup(
        value = "/repository/notifications/muted/before/before_notification_send.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/repository/notifications/muted/after/set_muted_for_new_notifications.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Нотификации игнорируются если их до этого небыло")
    void setMuteIfZeroNotificationsWasSent() {
        counterService.updateThresholdsAccordingToCorrespondingValues(List.of(104L));

        updateCountersWithAmountAndUnitType(295, 0, DAY);
        waitUntilCounterTasksFinished(2);

    }

    @Test
    @DatabaseSetup(
        value = "/repository/notifications/muted/before/with_old_notification.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/repository/notifications/muted/after/with_old_notification.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Мьют не выставляется на старые нотификации")
    void muteDidntSetOnOldCounterNotifications() {
        counterService.updateThresholdsAccordingToCorrespondingValues(List.of(104L));
    }

    @DisplayName("Проверка отправки всех нотификаций, кроме повторного")
    @ExpectedDatabase(
        value = "/repository/notifications/common/after/after_all_notifications_send_with_mute.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void shouldMutedIfItIsAlreadyOverflown() {
        updateCountersWithAmountAndUnitType(150, 300, DAY);
        waitUntilCounterTasksFinished(3);

        verify(lmsClient, times(5)).searchCapacity(any());
        verify(lmsClient, times(5)).getPartner(any());
    }
}
