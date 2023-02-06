package ru.yandex.market.tpl.billing.service.pvz;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.exception.ExceptionCollector;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tariffs.client.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.ModelType;
import ru.yandex.market.mbi.tariffs.client.model.Partner;
import ru.yandex.market.mbi.tariffs.client.model.PartnerType;
import ru.yandex.market.mbi.tariffs.client.model.PvzBrandingTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.PvzTariffRewardJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.PvzTariffZoneEnum;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.model.TariffValueType;
import ru.yandex.market.tpl.billing.model.pvz.PvzTariffRewardDto;
import ru.yandex.market.tpl.billing.service.tariff.TariffService;
import ru.yandex.market.tpl.billing.service.tariff.TariffsIterator;
import ru.yandex.market.tpl.billing.util.BillingConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * Тесты для {@link CalcBrandedPvzGmvService}
 */
class CalcBrandedPvzGmvServiceTest extends AbstractFunctionalTest {
    private static final Partner CUSTOM_PVZ = new Partner().id(4L).type(PartnerType.PVZ);

    @Autowired
    private CalcBrandedPvzGmvService calcBrandedPvzGmvService;

    @Autowired
    private TariffService tariffService;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> new TariffsIterator((page, size) -> {
            if (page != 0) {
                return List.of();
            }

            TariffDTO generalTariff = createTariff(1L, null);
            generalTariff.setMeta(List.of(
                    createMeta(20, 0, 1_000_000),
                    createMeta(15, 1_000_000, 1_500_000),
                    createMeta(10, 1_500_000, 3_500_000),
                    createMeta(8, 3_500_000, 6_000_000),
                    createMeta(5, 6_000_000, null)
            ));
            TariffDTO customTariff = createTariff(2L, CUSTOM_PVZ);
            customTariff.setMeta(List.of(
                    createMeta(30, 0, 1_000_000),
                    createMeta(20, 1_000_000, 6_000_000),
                    createMeta(10, 6_000_000, null)
            ));

            return List.of(generalTariff, customTariff);

        })).when(tariffService).findTariffs(any(TariffFindQuery.class));
    }

    private TariffDTO createTariff(long tariffId, Partner partner) {
        TariffDTO tariffDTO = new TariffDTO();
        tariffDTO.setId(tariffId);
        tariffDTO.setIsActive(true);
        tariffDTO.setDateFrom(BillingConstants.Pvz.BILLING_START_DAY_OF_BILLING_BY_GMV);
        tariffDTO.setModelType(ModelType.THIRD_PARTY_LOGISTICS_PVZ);
        tariffDTO.setPartner(partner);
        tariffDTO.setServiceType(ServiceTypeEnum.PVZ_REWARD);
        return tariffDTO;
    }

    private PvzTariffRewardJsonSchema createMeta(int percentAmount, int fromGmv, Integer toGmv) {
        PvzTariffRewardJsonSchema rewardJsonSchema = new PvzTariffRewardJsonSchema();
        rewardJsonSchema.setFromMonthAge(0);
        rewardJsonSchema.setPvzBrandingType(PvzBrandingTypeEnum.FULL);
        rewardJsonSchema.setPvzTariffZone(PvzTariffZoneEnum.MOSCOW);
        rewardJsonSchema.setAmount(BigDecimal.valueOf(percentAmount));
        rewardJsonSchema.setType(CommonJsonSchema.TypeEnum.RELATIVE);
        rewardJsonSchema.setCurrency("RUB");
        rewardJsonSchema.setBillingUnit(BillingUnitEnum.ORDER);
        rewardJsonSchema.setGmvFrom(fromGmv);
        rewardJsonSchema.setGmvTo(toGmv);
        return rewardJsonSchema;
    }

    @Test
    @DisplayName("Тест на то, что биллинг по gmv не сработает до 1ого февраля 2022 года")
    @DbUnitDataSet(before = "/database/service/pvz/CalcBrandedPvzGmvService/before/calculate_month.csv",
            after = "/database/service/pvz/CalcBrandedPvzGmvService/after/no_calculated_month.csv")
    void testCalcIsNotRunningBeforeTargetMonth() {
        calcBrandedPvzGmvService.calcGmvRewardAtMonth(
                LocalDate.of(2022, Month.JANUARY, 20),
                new ExceptionCollector()
        );
    }

    @Test
    @DisplayName("Тест на то, что биллинг по gmv корректно отработает за февраль 2022 года")
    @DbUnitDataSet(before = "/database/service/pvz/CalcBrandedPvzGmvService/before/calculate_month.csv")
    @DbUnitDataSet(after = "/database/service/pvz/CalcBrandedPvzGmvService/after/calculate_month.csv")
    void basicTestOnCalculateMonth() {
        try (ExceptionCollector exceptionCollector = new ExceptionCollector()) {
            calcBrandedPvzGmvService.calcGmvRewardAtMonth(
                    LocalDate.of(2022, Month.FEBRUARY, 10),
                    exceptionCollector
            );
        }
    }

    @DisplayName("Тест на проверку расчета общего вознаграждения и проверка получившейся формулы")
    @ParameterizedTest(name = "[{index}] : {2} = {3}")
    @MethodSource("testCalculateRewardData")
    void testCalculateGmvReward(
            List<PvzTariffRewardDto> tariffs,
            BigDecimal gmv,
            String expectedFormula,
            BigDecimal expectedReward
    ) {
        Pair<String, BigDecimal> formulaAndRewardOpt = calcBrandedPvzGmvService.calculateGmvReward(tariffs, gmv);
        assertEquals(expectedFormula, formulaAndRewardOpt.getLeft());
        assertEquals(0, expectedReward.compareTo(formulaAndRewardOpt.getRight()));
    }

    private static Stream<Arguments> testCalculateRewardData() {
        List<PvzTariffRewardDto> tariffs = List.of(
            createPvzRewardDto(0, 1_000_000, 20),
            createPvzRewardDto(1_000_000, 1_500_000, 15),
            createPvzRewardDto(1_500_000, 3_500_000, 10),
            createPvzRewardDto(3_500_000, 6_000_000, 8),
            createPvzRewardDto(6_000_000, null, 5)
        );

        return Stream.of(
                Arguments.of(
                        tariffs,
                        BigDecimal.valueOf(500_000),
                        "500000*0.2",
                        BigDecimal.valueOf(100_000)
                ),
                Arguments.of(
                        tariffs,
                        BigDecimal.valueOf(1_000_000),
                        "1000000*0.2",
                        BigDecimal.valueOf(200_000)
                ),
                Arguments.of(
                        tariffs,
                        BigDecimal.valueOf(1_200_000),
                        "1000000*0.2 + 200000*0.15",
                        BigDecimal.valueOf(230_000)
                ),
                Arguments.of(
                        tariffs,
                        BigDecimal.valueOf(4_000_000),
                        "1000000*0.2 + 500000*0.15 + 2000000*0.1 + 500000*0.08",
                        BigDecimal.valueOf(515_000)
                ),
                Arguments.of(
                        tariffs,
                        BigDecimal.valueOf(9_000_000),
                        "1000000*0.2 + 500000*0.15 + 2000000*0.1 + 2500000*0.08 + 3000000*0.05",
                        BigDecimal.valueOf(825_000)
                )
        );
    }

    private static PvzTariffRewardDto createPvzRewardDto(int gmvFrom, Integer gmvTo, int percent) {
        PvzTariffRewardDto tariff = new PvzTariffRewardDto();
        tariff.setGmvFrom(gmvFrom);
        tariff.setGmvTo(gmvTo);
        tariff.setAmount(BigDecimal.valueOf(percent).movePointLeft(2));
        tariff.setValueType(TariffValueType.RELATIVE);
        return tariff;
    }
}
