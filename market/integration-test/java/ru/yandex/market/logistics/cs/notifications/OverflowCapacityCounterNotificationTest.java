package ru.yandex.market.logistics.cs.notifications;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Тестирование уведомления о переполнении капасити")
class OverflowCapacityCounterNotificationTest extends CommonCapacityCounterNotificationTest {

    @DisplayName("Проверка срабатывания условия нотификации при первой отправке")
    @DatabaseSetup(
        value = "/repository/notifications/overflow/before/before_notification_send.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/repository/notifications/overflow/after/after_notification_send.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void shouldSendFirstTest() {
        updateCountersWithAmountAndUnitType(300, 600, DAY);
        waitUntilDayOffTasksFinished(2);
        waitUntilCounterTasksFinished(3);
    }
}
