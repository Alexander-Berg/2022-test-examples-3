package ru.yandex.market.billing.distribution.share.stats;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.billing.distribution.share.DistributionCategoryTariffRates;
import ru.yandex.market.billing.distribution.share.DistributionTariffRateAndName;
import ru.yandex.market.billing.distribution.share.DistributionTariffSupplier;
import ru.yandex.market.core.billing.distribution.share.stats.model.DistributionOrderCalculationOrder;
import ru.yandex.market.core.billing.distribution.share.DistributionTariffName;
import ru.yandex.market.core.billing.distribution.share.stats.DistributionOrderStatsDao;
import ru.yandex.market.core.billing.distribution.share.stats.model.DistributionOrderStatsPromocode;
import ru.yandex.market.core.report.model.Color;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DistributionOrderStatsTariffServiceTest {

    private static final DistributionTariffRateAndName CEHAC_TARIFF =
            new DistributionTariffRateAndName(BigDecimal.ONE, DistributionTariffName.CEHAC, null);

    private static final DistributionTariffRateAndName FMCG_TARIFF =
            new DistributionTariffRateAndName(BigDecimal.ONE, DistributionTariffName.FMCG, null);

    private DistributionOrderStatsTariffService distributionOrderStatsTariffService;

    @Mock
    private DistributionTariffSupplier supplier;

    @BeforeEach
    private void setup() {
        distributionOrderStatsTariffService = new DistributionOrderStatsTariffService(supplier);
    }

    @Test
    public void testGet() {
        when(supplier.get(any(), anyLong(), anyLong())).thenReturn(CEHAC_TARIFF);

        DistributionTariffRateAndName result = distributionOrderStatsTariffService.getTariff(
                DistributionOrderCalculationOrder.builder()
                        .setOrderCreationTime(LocalDateTime.parse("2020-01-01T00:00:00"))
                        .setClid(111)
                        .build(),
                111L,
                null,
                1L
        );

        Assertions.assertThat(result).isEqualTo(CEHAC_TARIFF);
    }

    @Test
    public void testIncreasedTariff() {
        when(supplier.get(any(), anyLong(), anyLong())).thenReturn(FMCG_TARIFF);

        var spy = Mockito.spy(distributionOrderStatsTariffService);
        Mockito.doReturn(List.of(
                    new DistributionOrderStatsTariffService.DistributionOrderStatsIncreasedTariff(
                            Set.of(2482178L),
                            LocalDateTime.parse("2021-11-23T00:00:00"),
                            LocalDateTime.parse("2021-11-30T23:59:59"),
                            0.25d,
                            DistributionOrderStatsTariffService.IncreasedTariffType.ORDER_CREATION_DATE)
            )).when(spy).getIncreasedTariffs();

        DistributionTariffRateAndName result = spy.getTariff(
                DistributionOrderCalculationOrder.builder()
                        .setOrderCreationTime(LocalDateTime.parse("2021-11-23T00:00:00"))
                        .setClid(2482178L)
                        .build(),
                111L,
                null,
                1L
        );

        DistributionTariffRateAndName result2 = spy.getTariff(
                DistributionOrderCalculationOrder.builder()
                        .setOrderCreationTime(LocalDateTime.parse("2021-11-30T23:59:59"))
                        .setClid(2482178L)
                        .build(),
                111L,
                null,
                1L
        );

        Assertions.assertThat(result.getTariffRate()).isEqualTo(BigDecimal.valueOf(1.25d));
        Assertions.assertThat(result2.getTariffRate()).isEqualTo(BigDecimal.valueOf(1.25d));
    }

    @Test
    public void testIncreasedTariffByApprovalTime() {
        when(supplier.get(any(), anyLong(), anyLong())).thenReturn(FMCG_TARIFF);

        var spy = Mockito.spy(distributionOrderStatsTariffService);

        //noinspection ResultOfMethodCallIgnored
        Mockito.doReturn(List.of(
                new DistributionOrderStatsTariffService.DistributionOrderStatsIncreasedTariff(
                        Set.of(2482178L),
                        LocalDateTime.parse("2021-11-23T00:00:00"),
                        LocalDateTime.parse("2021-11-30T23:59:59"),
                        0.25d,
                        DistributionOrderStatsTariffService.IncreasedTariffType.APPROVAL_DATE)
        )).when(spy).getIncreasedTariffs();

        var resultApproved = spy.getTariff(
                DistributionOrderCalculationOrder.builder()
                        .setOrderCreationTime(LocalDateTime.parse("2021-11-01T00:00:00"))
                        .setClid(2482178L)
                        .build(),
                111L,
                LocalDateTime.parse("2021-11-24T00:00:00"),
                1L
        );
        var resultNotApprovedMatches = spy.getTariff(
                DistributionOrderCalculationOrder.builder()
                        .setOrderCreationTime(LocalDateTime.parse("2021-11-10T00:00:00"))
                        .setClid(2482178L)
                        .build(),
                111L,
                null,
                1L
        );
        var resultNotApprovedNoMatch = spy.getTariff(
                DistributionOrderCalculationOrder.builder()
                        .setOrderCreationTime(LocalDateTime.parse("2021-11-30T23:59:59"))
                        .setClid(2482178L)
                        .build(),
                111L,
                null,
                1L
        );

        Assertions.assertThat(resultApproved.getTariffRate()).isEqualTo(BigDecimal.valueOf(1.25d));
        Assertions.assertThat(resultNotApprovedMatches.getTariffRate()).isEqualTo(BigDecimal.valueOf(1.25d));
        Assertions.assertThat(resultNotApprovedNoMatch.getTariffRate()).isEqualTo(BigDecimal.valueOf(1));
    }

    @Test
    public void testPromoClid() {
        var order = DistributionOrderCalculationOrder.builder()
                .setOrderCreationTime(LocalDateTime.parse("2020-01-01T00:00:00"))
                .setClid(222)
                .setPromocodeData(List.of(
                        new DistributionOrderStatsPromocode(222, "philips_aug_2021_AF")))
                .build();
        when(supplier.get(eq(order), eq(111L), eq(1L))).thenReturn(FMCG_TARIFF);

        DistributionTariffRateAndName result = distributionOrderStatsTariffService.getTariff(
                order, 111L, null, 1L
        );

        Assertions.assertThat(result).usingRecursiveComparison()
                .isEqualTo(FMCG_TARIFF);
    }
}
