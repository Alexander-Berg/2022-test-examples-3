package ru.yandex.market.logistics.cs.notifications;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("Тестирование всех уведомлений о переполнении капасити")
class AllCapacityCounterNotificationsTest extends CommonCapacityCounterNotificationTest {

    @DisplayName("Проверка отправки всех нотификаций, кроме повторного")
    @ExpectedDatabase(
        value = "/repository/notifications/common/after/after_all_notifications_send.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup(
        value = "/repository/notifications/common/before/before_all_notification_send.xml",
        type = DatabaseOperation.REFRESH
    )
    @Test
    void shouldSendAllTest() {
        updateCountersWithAmountAndUnitType(150, 300, DAY);
        waitUntilCounterTasksFinished(3);
        refreshQueue();

        verify(lmsClient, times(5)).searchCapacity(any());
        verify(lmsClient, times(5)).getPartner(any());

        updateCountersWithAmountAndUnitType(150, 300, DAY);
        waitUntilCounterTasksFinished(2);

        verify(lmsClient, times(7)).searchCapacity(any());
        verify(lmsClient, times(7)).getPartner(any());
    }

    @DisplayName("Проверка не отправления нотификаций для прошедших дат счетчиков капасити")
    @DatabaseSetup(
        value = "/repository/notifications/common/before/before_no_one_notification_send.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/repository/notifications/common/after/after_no_one_notification_send.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void shouldSendNothingTest() {
        updateCountersWithAmountAndUnitType(300, 600, DAY.minusDays(1));
    }
}
