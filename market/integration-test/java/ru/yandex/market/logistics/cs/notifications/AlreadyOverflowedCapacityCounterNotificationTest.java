package ru.yandex.market.logistics.cs.notifications;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Тестирование повторного уведомления о переполнении капасити")
class AlreadyOverflowedCapacityCounterNotificationTest extends CommonCapacityCounterNotificationTest {

    @DisplayName("Проверка срабатывания условия нотификации после уже отправленной о переполнении ранее")
    @DatabaseSetup(
        value = "/repository/notifications/overflow/before/before_second_notification_send.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/repository/notifications/overflow/after/after_second_notification_send.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void shouldSendSecondTest() {
        updateCountersWithAmountAndUnitType(300, 600, DAY);
        waitUntilCounterTasksFinished(3);
    }

    @DisplayName("Проверка не срабатывания условия повторной нотификации при недостижении порога")
    @DatabaseSetup(
        value = "/repository/notifications/overflow/before/before_second_notification_send.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/repository/notifications/overflow/after/after_second_notification_not_send.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void shouldNotSendSecondTest() {
        updateCountersWithAmountAndUnitType(9, 18, DAY);
        waitUntilCounterTasksFinished(3);
    }
}
