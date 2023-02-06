package ru.yandex.market.tpl.billing.executor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.logistics.tarifficator.model.dto.tpl.CourierTariffDto;
import ru.yandex.market.logistics.tarifficator.model.dto.tpl.CourierTariffOptionDto;
import ru.yandex.market.logistics.tarifficator.model.dto.tpl.CourierTariffPriceDto;
import ru.yandex.market.logistics.tarifficator.model.enums.tpl.CourierTariffOptionType;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.repository.EnvironmentRepository;
import ru.yandex.market.tpl.billing.service.SyncCourierTariffsService;
import ru.yandex.market.tpl.billing.task.executor.SyncCourierTariffsExecutor;
import ru.yandex.market.tpl.billing.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DbUnitDataSet(
        before = {
                "/database/executor/synccouriertariffs/before/company.csv",
                "/database/executor/synccouriertariffs/before/tariff_zone.csv",
                "/database/executor/synccouriertariffs/before/feature_is_enabled.csv"})
//TODO: https://st.yandex-team.ru/MARKETTPLBILL-244 удалить флаг SYNC_COURIER_TARIFFS_FEATURE_ENABLED
public class SyncCourierTariffsExecutorTest extends AbstractFunctionalTest {

    @Autowired
    private TestableClock clock;

    @Autowired
    private TarifficatorClient tarifficatorClient;

    @Autowired
    private SyncCourierTariffsService syncCourierTariffsService;

    @Autowired
    private EnvironmentRepository environmentRepository;

    private SyncCourierTariffsExecutor syncCourierTariffsExecutor;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2021-02-23T12:00:00Z"), DateTimeUtil.DEFAULT_ZONE_ID);
        syncCourierTariffsExecutor = new SyncCourierTariffsExecutor(
            clock,
            syncCourierTariffsService,
            environmentRepository
        );
        verifyNoMoreInteractions(tarifficatorClient);
    }

    @Test
    @DisplayName("Тариф на дату не найден")
    void tariffNotFound() {
        Assertions.assertThatThrownBy(() -> syncCourierTariffsExecutor.doJob())
            .isInstanceOf(TplIllegalStateException.class)
            .hasMessage("Tariff not found for date 2021-02-23");
        verify(tarifficatorClient).getActualTplCourierTariff(LocalDate.of(2021, 2, 23));
    }

    @Test
    @DisplayName("Тариф на дату найден, тарифов в базе нет")
    @DbUnitDataSet(after = "/database/executor/synccouriertariffs/after/synced_tariff_1.csv")
    void newTariffFoundNoTariffsInDatabase() {
        when(tarifficatorClient.getActualTplCourierTariff(LocalDate.of(2021, 2, 23)))
            .thenReturn(fillCourierTariff(1L, 1000L));
        syncCourierTariffsExecutor.doJob();
        verify(tarifficatorClient).getActualTplCourierTariff(LocalDate.of(2021, 2, 23));
    }

    @Test
    @DisplayName("Тариф на дату найден, в базе этот тариф уже есть")
    @DbUnitDataSet(
            before ="/database/executor/synccouriertariffs/before/synced_tariff_1.csv",
            after = "/database/executor/synccouriertariffs/after/synced_tariff_1.csv")
    void foundOldTariff() {
        when(tarifficatorClient.getActualTplCourierTariff(LocalDate.of(2021, 2, 23)))
            .thenReturn(fillCourierTariff(1L, 1000L));
        syncCourierTariffsExecutor.doJob();
        verify(tarifficatorClient).getActualTplCourierTariff(LocalDate.of(2021, 2, 23));
    }

    @Test
    @DisplayName("Найден новый тариф на дату - тариф перезаписывается")
    @DbUnitDataSet(
            before = "/database/executor/synccouriertariffs/before/synced_tariff_1.csv",
            after = "/database/executor/synccouriertariffs/after/synced_tariff_2.csv")
    void foundNewTariffOverride() {
        when(tarifficatorClient.getActualTplCourierTariff(LocalDate.of(2021, 2, 23)))
            .thenReturn(fillCourierTariff(2L, 2000L));
        syncCourierTariffsExecutor.doJob();
        verify(tarifficatorClient).getActualTplCourierTariff(LocalDate.of(2021, 2, 23));
    }

    @Test
    @DisplayName("Найден новый тариф на дату - старый тариф перестаёт действовать на новые даты")
    @DbUnitDataSet(
            before = "/database/executor/synccouriertariffs/before/tariff_1_from_date_january.csv",
            after = "/database/executor/synccouriertariffs/after/synced_tariff_2_tariff_1_changed_to_date.csv")
    void foundNewTariff() {
        when(tarifficatorClient.getActualTplCourierTariff(LocalDate.of(2021, 2, 23)))
            .thenReturn(fillCourierTariff(2L, 2000L));
        syncCourierTariffsExecutor.doJob();
        verify(tarifficatorClient).getActualTplCourierTariff(LocalDate.of(2021, 2, 23));
    }

    @Nonnull
    private Optional<CourierTariffDto> fillCourierTariff(Long tarifficatorId, Long minTariff) {
        return Optional.of(
            new CourierTariffDto()
                .setId(tarifficatorId)
                .setFromDate(LocalDate.of(2021, 2, 1))
                .setToDate(LocalDate.of(2021, 2, 28))
                .setCourierTariffOptions(List.of(
                    new CourierTariffOptionDto()
                        .setCourierTariffZoneId(1L)
                        .setType(CourierTariffOptionType.SMALL_GOODS)
                        .setSortingCenterId(1L)
                        .setCourierCompanyId(1L)
                        .setFromDistance(0L)
                        .setToDistance(130L)
                        .setPrice(new CourierTariffPriceDto()
                            .setMinTariff(minTariff)
                            .setStandardTariff(BigDecimal.valueOf(100))
                            .setBusinessTariff(BigDecimal.valueOf(110))
                            .setLockerTariff(BigDecimal.valueOf(120))
                            .setPvzTariff(BigDecimal.valueOf(130))
                            .setLockerBoxTariff(BigDecimal.valueOf(140))
                            .setPvzBoxTariff(BigDecimal.valueOf(150))
                        )
                ))
        );
    }
}
