package ru.yandex.market.logistics.nesu.service.dropoff;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName(
    "Тест на выполнение подзадачи отключения дропоффа. Установить лимит партнеров в конфигурации доступности в 0."
)
class SetZeroPartnersLimitExecutorTest extends AbstractDisablingSubtaskTest {

    @Test
    @DatabaseSetup("/service/dropoff/before/set_zero_partners_availability_limit.xml")
    @ExpectedDatabase(
        value = "/service/dropoff/after/set_zero_partners_availability_limit.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная установка лимита партнеров.")
    void successSetLimit() {
        disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(100, 1));
    }

}
