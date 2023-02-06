package ru.yandex.market.logistics.cs.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.cs.AbstractIntegrationTest;


@DisplayName("Тестирование сервиса расчистки базы данных")
public class ClearServiceTest extends AbstractIntegrationTest {

    @Autowired
    private ClearService clearService;

    @DisplayName("Успешно очищаем устаревшие данные")
    @DatabaseSetup("/repository/event/before/clear_events.xml")
    @ExpectedDatabase(
        value = "/repository/event/after/clear_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testClearOutdatedData() {
        clearService.clearOutdatedData();
    }

}
