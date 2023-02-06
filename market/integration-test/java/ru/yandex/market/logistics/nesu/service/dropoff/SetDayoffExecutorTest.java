package ru.yandex.market.logistics.nesu.service.dropoff;

import java.time.LocalDate;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pvz.client.logistics.dto.DeactivateDropoffDto;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Тест на выполнение подзадачи отключения дропоффа. Отключение дропоффа через TplPvz.")
public class SetDayoffExecutorTest extends AbstractDisablingSubtaskTest {

    private static final String REASON = "UNPROFITABLE";

    @AfterEach
    void tearDownTplPvz() {
        verifyNoMoreInteractions(pvzLogisticsClient);
    }

    @Test
    @DatabaseSetup("/service/dropoff/before/disable_dropoff_tpl_pvz.xml")
    @ExpectedDatabase(
        value = "/service/dropoff/after/disable_dropoff_tpl_pvz.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное отключение дропоффа")
    void successDisablingDropoff() {
        disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(100, 1));

        verify(pvzLogisticsClient).deactivateDropoff(
            DROPOFF_LOGISTIC_POINT_ID_321,
            DeactivateDropoffDto.builder()
                .date(LocalDate.of(2021, 12, 8))
                .reason(REASON)
                .build()
        );
    }

}
