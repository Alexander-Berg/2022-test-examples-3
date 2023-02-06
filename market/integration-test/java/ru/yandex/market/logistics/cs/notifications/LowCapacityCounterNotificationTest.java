package ru.yandex.market.logistics.cs.notifications;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.cs.monitoring.SolomonClient;

@DisplayName("Тестирование уведомления о заполнении 90% капасити")
class LowCapacityCounterNotificationTest extends CommonCapacityCounterNotificationTest {

    @Autowired
    SolomonClient client;

    @DisplayName("Проверка срабатывания условия нотификации при первой отправке")
    @DatabaseSetup(
        value = "/repository/notifications/low/before/before_notification_send.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/repository/notifications/low/after/after_notification_send.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void shouldSendFirstTest() {
        updateCountersWithAmountAndUnitType(300, 600, DAY);
        waitUntilCounterTasksFinished(3);
    }
}
