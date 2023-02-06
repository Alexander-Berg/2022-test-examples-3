package ru.yandex.market.logistics.nesu.jobs;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.converter.DropoffDisablingConverter;
import ru.yandex.market.logistics.nesu.jobs.executor.UpdateDisableDropoffReasonsExecutor;
import ru.yandex.market.logistics.nesu.service.dropoff.DropoffDisablingReasonService;
import ru.yandex.market.logistics.nesu.service.dropoff.PvzDropoffService;
import ru.yandex.market.pvz.client.logistics.dto.LogisticsDeactivationReasonDto;

import static org.mockito.Mockito.when;

@DisplayName("Обновление причин отключения дропоффа")
public class UpdateDisableDropoffReasonsExecutorTest extends AbstractContextualTest {

    @Autowired
    private PvzDropoffService pvzDropoffService;
    @Autowired
    private DropoffDisablingReasonService dropoffDisablingReasonService;
    @Autowired
    private DropoffDisablingConverter dropoffDisablingConverter;

    private UpdateDisableDropoffReasonsExecutor updateDisableDropoffReasonsExecutor;

    @BeforeEach
    public void setup() {
        updateDisableDropoffReasonsExecutor = new UpdateDisableDropoffReasonsExecutor(
            dropoffDisablingReasonService,
            pvzDropoffService,
            dropoffDisablingConverter
        );
    }

    @Test
    @DisplayName("Успешное обновление причин отключения")
    @DatabaseSetup("/jobs/executors/update_disable_dropoff_reasons/disable_dropoff_reasons_before.xml")
    @ExpectedDatabase(
        value = "/jobs/executors/update_disable_dropoff_reasons/disable_dropoff_reasons_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successUpdate() {
        when(pvzDropoffService.getLogisticsDeactivationReasons()).thenReturn(
            List.of(
                createReasonDto("UNPROFITABLE", "Модифицированная нерентабельность"),
                createReasonDto("UNPROFITABLE_1", "Нерентабельность 1")
            )
        );

        updateDisableDropoffReasonsExecutor.doJob(null);
    }

    @Nonnull
    private LogisticsDeactivationReasonDto createReasonDto(String logisticsReason, String reasons) {
        return LogisticsDeactivationReasonDto.builder()
            .logisticsReason(logisticsReason)
            .reason(reasons)
            .build();
    }
}
