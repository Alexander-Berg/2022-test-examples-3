package ru.yandex.market.tpl.billing.service.tariffs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tariffs.client.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.ModelType;
import ru.yandex.market.mbi.tariffs.client.model.Partner;
import ru.yandex.market.mbi.tariffs.client.model.PartnerType;
import ru.yandex.market.mbi.tariffs.client.model.PvzBrandingTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.PvzTariffFlatJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.PvzTariffRewardJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.PvzTariffZoneEnum;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.model.PickupPointBrandingType;
import ru.yandex.market.tpl.billing.model.ServiceType;
import ru.yandex.market.tpl.billing.model.TariffValueType;
import ru.yandex.market.tpl.billing.model.entity.PickupPoint;
import ru.yandex.market.tpl.billing.model.pvz.PvzTariff;
import ru.yandex.market.tpl.billing.model.pvz.PvzTariffRewardDto;
import ru.yandex.market.tpl.billing.repository.PvzBrandedTariffZoneRegionRepository;
import ru.yandex.market.tpl.billing.repository.PvzBrandedTariffZoneRepository;
import ru.yandex.market.tpl.billing.service.pvz.PvzTariffService;
import ru.yandex.market.tpl.billing.service.tariff.TariffService;
import ru.yandex.market.tpl.billing.service.tariff.TariffsIterator;
import ru.yandex.market.tpl.billing.util.TariffServiceUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты для {@link ru.yandex.market.tpl.billing.service.pvz.PvzTariffService}
 */
@DbUnitDataSet(before = "/database/service/tariffs/pvztariffservice/before/testGetRewardBrandedTariff.csv")
public class PvzTariffServiceTest extends AbstractFunctionalTest {

    private static final LocalDate BILLING_DEFAULT_DATE = LocalDate.of(2022, Month.JANUARY, 10);
    private static final LocalDate BRANDED_SINCE_DEFAULT_DATE = LocalDate.of(2021, Month.JANUARY, 1);
    private static final long BRAND_REGION_MOSCOW = 1L;

    private PvzTariffService pvzTariffService;

    @Autowired
    private TariffService tariffService;

    @Autowired
    private PvzBrandedTariffZoneRegionRepository pvzBrandedTariffZoneRegionRepository;
    @Autowired
    private PvzBrandedTariffZoneRepository pvzBrandedTariffZoneRepositor;

    /**
     * Мапа с отображением типа услуги на метод из пвз тариф сервиса
     */
    private final Map<ServiceType, Function<PickupPoint, Optional<PvzTariff>>> mapServiceToFlatTariff = Map.ofEntries(
            Map.entry(ServiceType.PVZ_CARD_COMPENSATION, pvz -> pvzTariffService.getCardCompensationTariff(pvz)),
            Map.entry(ServiceType.PVZ_CASH_COMPENSATION, pvz -> pvzTariffService.getCashCompensationTariff(pvz)),
            Map.entry(ServiceType.PVZ_DROPOFF, pvz -> pvzTariffService.getDropoffTariff(pvz)),
            Map.entry(ServiceType.PVZ_DROPOFF_RETURN, pvz -> pvzTariffService.getDropoffReturnTariff(pvz)),
            Map.entry(ServiceType.PVZ_RETURN, pvz -> pvzTariffService.getReturnTariff(pvz)),
            Map.entry(ServiceType.PVZ_REWARD_YADO, pvz -> pvzTariffService.getYaDostavkaRewardTariff(pvz)),
            Map.entry(ServiceType.PVZ_REWARD_DBS, pvz -> pvzTariffService.getDbsRewardTariff(pvz)),
            Map.entry(ServiceType.PVZ_DBS_INCOME, pvz -> pvzTariffService.getDbsIncomeTariff(pvz)),
            Map.entry(ServiceType.PVZ_DBS_OUTCOME, pvz -> pvzTariffService.getDbsOutcomeTariff(pvz))
    );

    @BeforeEach
    void setUp() {
        pvzTariffService = init(BILLING_DEFAULT_DATE);
    }

    @Test
    @DisplayName("Тест на получение брендированного тарифа")
    void testGetRewardBrandedTariff() {
        PickupPoint firstPvz = createBrandedPickupPoint(1L, BRAND_REGION_MOSCOW, 4L);
        PickupPoint secondPvz = createBrandedPickupPoint(2L, BRAND_REGION_MOSCOW, 5L);
        PickupPoint thirdPvz = createBrandedPickupPoint(3L, BRAND_REGION_MOSCOW, 6L)
                .setBrandedSince(BILLING_DEFAULT_DATE.minusDays(30));

        Mockito.doAnswer(invocation -> new TariffsIterator((pageNumber, batchSize) -> {
            if (pageNumber != 0) {
                return List.of();
            }

            TariffDTO firstTariff = createTariffPvzRewardTariffWithoutMeta(1, BILLING_DEFAULT_DATE.minusYears(1), null, new Partner().id(1L).type(PartnerType.PVZ));
            firstTariff.setMeta(List.of(
                    createPvzRewardTariffMeta(0, 2, PvzBrandingTypeEnum.FULL, PvzTariffZoneEnum.MOSCOW, new BigDecimal("50")),
                    createPvzRewardTariffMeta(2, 100, PvzBrandingTypeEnum.FULL, PvzTariffZoneEnum.MOSCOW, new BigDecimal("10"))
            ));

            TariffDTO secondTariff = createTariffPvzRewardTariffWithoutMeta(2, BILLING_DEFAULT_DATE.minusYears(1), null, new Partner().id(5L).type(PartnerType.PVZ_PARTNER));
            secondTariff.setMeta(List.of(
                    createPvzRewardTariffMeta(0, 100, PvzBrandingTypeEnum.FULL, PvzTariffZoneEnum.MOSCOW, new BigDecimal("50"))
            ));

            TariffDTO thirdTariff = createTariffPvzRewardTariffWithoutMeta(3, BILLING_DEFAULT_DATE.minusYears(1), null, null);
            thirdTariff.setMeta(List.of(
                    createPvzRewardTariffMeta(0, 3, PvzBrandingTypeEnum.FULL, PvzTariffZoneEnum.MOSCOW, new BigDecimal("20")),
                    createPvzRewardTariffMeta(3, 6, PvzBrandingTypeEnum.FULL, PvzTariffZoneEnum.MOSCOW, new BigDecimal("15")),
                    createPvzRewardTariffMeta(6, null, PvzBrandingTypeEnum.FULL, PvzTariffZoneEnum.MOSCOW, new BigDecimal("10"))
            ));

            return List.of(firstTariff, secondTariff, thirdTariff);

        })).when(tariffService).findTariffs(new TariffFindQuery()
                .isActive(true)
                .serviceType(TariffServiceUtils.convertFrom(ServiceType.PVZ_REWARD))
                .targetDate(BILLING_DEFAULT_DATE)
        );

        Optional<PvzTariff> firstTariff = pvzTariffService.getRewardTariff(firstPvz);
        assertTrue(firstTariff.isPresent());
        firstTariff.ifPresent(pvzTariff -> {
            assertEquals(1L, pvzTariff.getId());
            assertEquals(TariffValueType.RELATIVE, pvzTariff.getValueType());
            assertEquals(new BigDecimal("0.10"), pvzTariff.getAmount());
        });

        Optional<PvzTariff> secondTariff = pvzTariffService.getRewardTariff(secondPvz);
        assertTrue(secondTariff.isPresent());
        secondTariff.ifPresent(pvzTariff -> {
            assertEquals(2L, pvzTariff.getId());
            assertEquals(TariffValueType.RELATIVE, pvzTariff.getValueType());
            assertEquals(new BigDecimal("0.50"), pvzTariff.getAmount());
        });

        Optional<PvzTariff> thirdTariff = pvzTariffService.getRewardTariff(thirdPvz);
        assertTrue(thirdTariff.isPresent());
        thirdTariff.ifPresent(pvzTariff -> {
            assertEquals(3L, pvzTariff.getId());
            assertEquals(TariffValueType.RELATIVE, pvzTariff.getValueType());
            assertEquals(new BigDecimal("0.20"), pvzTariff.getAmount());
        });
    }

    @Test
    @DisplayName("Тест на получение брендированного тарифа, который матчится по пвз")
    void testGetBrandedTariffMatchedByPvz() {
        Partner pvzPartnerFromTariffs = new Partner().id(1L).type(PartnerType.PVZ);
        PickupPoint firstPvz = createBrandedPickupPoint(1L, BRAND_REGION_MOSCOW, pvzPartnerFromTariffs.getId());

        Mockito.doAnswer(invocation -> new TariffsIterator((pageNumber, batchSize) -> {
            if (pageNumber != 0) {
                return List.of();
            }

            TariffDTO firstTariff = createTariffPvzRewardTariffWithoutMeta(1, BILLING_DEFAULT_DATE.minusYears(1), null, pvzPartnerFromTariffs);
            firstTariff.setMeta(List.of(
                    createPvzRewardTariffMeta(0, 2, PvzBrandingTypeEnum.FULL, PvzTariffZoneEnum.MOSCOW, new BigDecimal("50")),
                    createPvzRewardTariffMeta(2, 100, PvzBrandingTypeEnum.FULL, PvzTariffZoneEnum.MOSCOW, new BigDecimal("10"))
            ));

            return List.of(firstTariff);

        })).when(tariffService).findTariffs(new TariffFindQuery()
                .isActive(true)
                .serviceType(TariffServiceUtils.convertFrom(ServiceType.PVZ_REWARD))
                .targetDate(BILLING_DEFAULT_DATE)
        );

        Optional<PvzTariff> rewardTariff = pvzTariffService.getRewardTariff(firstPvz);
        assertTrue(rewardTariff.isPresent());
        rewardTariff.ifPresent(pvzTariff -> {
            assertEquals(1L, pvzTariff.getId());
            assertEquals(TariffValueType.RELATIVE, pvzTariff.getValueType());
            assertEquals(new BigDecimal("0.10"), pvzTariff.getAmount());
        });
    }

    @Test
    @DisplayName("Тест на получение брендированного тарифа, который матчится по партнеру")
    void testGetBrandedTariffMatchedByPartner() {
        PickupPoint firstPvz = createBrandedPickupPoint(1L, BRAND_REGION_MOSCOW, 4L);

        Mockito.doAnswer(invocation -> new TariffsIterator((pageNumber, batchSize) -> {
            if (pageNumber != 0) {
                return List.of();
            }

            // просто по пвз этот тариф проходит, но не проходит по мете - у этого тарифа время жизни от 0 до 2х месяцев
            // и больше меты нет. а наш пвз живет уже 12 месяцев
            TariffDTO firstTariff = createTariffPvzRewardTariffWithoutMeta(1, BILLING_DEFAULT_DATE.minusYears(1), null, new Partner().id(1L).type(PartnerType.PVZ));
            firstTariff.setMeta(List.of(
                    createPvzRewardTariffMeta(0, 2, PvzBrandingTypeEnum.FULL, PvzTariffZoneEnum.MOSCOW, new BigDecimal("50"))
            ));

            TariffDTO secondTariff = createTariffPvzRewardTariffWithoutMeta(2, BILLING_DEFAULT_DATE.minusYears(1), null, new Partner().id(4L).type(PartnerType.PVZ_PARTNER));
            secondTariff.setMeta(List.of(
                    createPvzRewardTariffMeta(0, 100, PvzBrandingTypeEnum.FULL, PvzTariffZoneEnum.MOSCOW, new BigDecimal("20"))
            ));

            return List.of(firstTariff, secondTariff);

        })).when(tariffService).findTariffs(new TariffFindQuery()
                .isActive(true)
                .serviceType(TariffServiceUtils.convertFrom(ServiceType.PVZ_REWARD))
                .targetDate(BILLING_DEFAULT_DATE)
        );

        Optional<PvzTariff> rewardTariff = pvzTariffService.getRewardTariff(firstPvz);
        assertTrue(rewardTariff.isPresent());
        rewardTariff.ifPresent(pvzTariff -> {
            assertEquals(2L, pvzTariff.getId());
            assertEquals(TariffValueType.RELATIVE, pvzTariff.getValueType());
            assertEquals(new BigDecimal("0.20"), pvzTariff.getAmount());
        });
    }

    @Test
    @DisplayName("Тест на получение брендированного тарифа общего (матчинг всего остального не подошел)")
    void testGetBrandedTariffGeneral() {
        PickupPoint firstPvz = createBrandedPickupPoint(1L, BRAND_REGION_MOSCOW, 4L);

        Mockito.doAnswer(invocation -> new TariffsIterator((pageNumber, batchSize) -> {
            if (pageNumber != 0) {
                return List.of();
            }

            // подходит по пвз, но не проходит по мете
            TariffDTO firstTariff = createTariffPvzRewardTariffWithoutMeta(1, BILLING_DEFAULT_DATE.minusYears(1), null, new Partner().id(1L).type(PartnerType.PVZ));
            firstTariff.setMeta(List.of(
                    createPvzRewardTariffMeta(0, 2, PvzBrandingTypeEnum.FULL, PvzTariffZoneEnum.MOSCOW, new BigDecimal("50"))
            ));

            // подходит по партнеру и времени жизни, но не проходит по тарифной зоне
            TariffDTO secondTariff = createTariffPvzRewardTariffWithoutMeta(2, BILLING_DEFAULT_DATE.minusYears(1), null, new Partner().id(4L).type(PartnerType.PVZ_PARTNER));
            secondTariff.setMeta(List.of(
                    createPvzRewardTariffMeta(0, 100, PvzBrandingTypeEnum.FULL, PvzTariffZoneEnum.SPB, new BigDecimal("20"))
            ));

            TariffDTO generalTariff = createTariffPvzRewardTariffWithoutMeta(3, BILLING_DEFAULT_DATE.minusYears(1), null, null);
            generalTariff.setMeta(List.of(
                    createPvzRewardTariffMeta(0, 3, PvzBrandingTypeEnum.FULL, PvzTariffZoneEnum.MOSCOW, new BigDecimal("12")),
                    createPvzRewardTariffMeta(3, 6, PvzBrandingTypeEnum.FULL, PvzTariffZoneEnum.MOSCOW, new BigDecimal("10")),
                    createPvzRewardTariffMeta(6, null, PvzBrandingTypeEnum.FULL, PvzTariffZoneEnum.MOSCOW, new BigDecimal("8"))
            ));

            return List.of(firstTariff, secondTariff, generalTariff);

        })).when(tariffService).findTariffs(new TariffFindQuery()
                .isActive(true)
                .serviceType(TariffServiceUtils.convertFrom(ServiceType.PVZ_REWARD))
                .targetDate(BILLING_DEFAULT_DATE)
        );

        Optional<PvzTariff> rewardTariff = pvzTariffService.getRewardTariff(firstPvz);
        assertTrue(rewardTariff.isPresent());
        rewardTariff.ifPresent(pvzTariff -> {
            assertEquals(3L, pvzTariff.getId());
            assertEquals(TariffValueType.RELATIVE, pvzTariff.getValueType());
            assertEquals(new BigDecimal("0.08"), pvzTariff.getAmount());
        });
    }

    @Test
    @DisplayName("Тест на получение тарифа за вознаграждение небрендированного пвз")
    void testGetNoneBrandedTariff() {
        PickupPoint firstPvz = createNoneBrandedPickupPoint(1L, 4L);
        PickupPoint secondPvz = createNoneBrandedPickupPoint(2L, 5L);
        PickupPoint thirdPvz = createNoneBrandedPickupPoint(3L, 6L);

        Mockito.doAnswer(invocation -> new TariffsIterator((pageNumber, batchSize) -> {
            if (pageNumber != 0) {
                return List.of();
            }

            // подходит по пвз
            TariffDTO firstTariff = createTariffPvzRewardTariffWithoutMeta(1, BILLING_DEFAULT_DATE.minusYears(1), null, new Partner().id(1L).type(PartnerType.PVZ));
            firstTariff.setMeta(List.of(
                    createPvzRewardNoneBrandTariffMeta(new BigDecimal("50"))
            ));

            // подходит по партнеру
            TariffDTO secondTariff = createTariffPvzRewardTariffWithoutMeta(2, BILLING_DEFAULT_DATE.minusYears(1), null, new Partner().id(5L).type(PartnerType.PVZ_PARTNER));
            secondTariff.setMeta(List.of(
                    createPvzRewardNoneBrandTariffMeta(new BigDecimal("40"))
            ));

            TariffDTO thirdTariff = createTariffPvzRewardTariffWithoutMeta(3, BILLING_DEFAULT_DATE.minusYears(1), null, null);
            thirdTariff.setMeta(List.of(
                    createPvzRewardNoneBrandTariffMeta(new BigDecimal("30"))
            ));

            return List.of(firstTariff, secondTariff, thirdTariff);

        })).when(tariffService).findTariffs(new TariffFindQuery()
                .isActive(true)
                .serviceType(TariffServiceUtils.convertFrom(ServiceType.PVZ_REWARD))
                .targetDate(BILLING_DEFAULT_DATE)
        );

        Optional<PvzTariff> firstTariff = pvzTariffService.getRewardTariff(firstPvz);
        assertTrue(firstTariff.isPresent());
        firstTariff.ifPresent(pvzTariff -> {
            assertEquals(1L, pvzTariff.getId());
            assertEquals(TariffValueType.ABSOLUTE, pvzTariff.getValueType());
            assertEquals(new BigDecimal("50"), pvzTariff.getAmount());
        });

        Optional<PvzTariff> secondTariff = pvzTariffService.getRewardTariff(secondPvz);
        assertTrue(secondTariff.isPresent());
        secondTariff.ifPresent(pvzTariff -> {
            assertEquals(2L, pvzTariff.getId());
            assertEquals(TariffValueType.ABSOLUTE, pvzTariff.getValueType());
            assertEquals(new BigDecimal("40"), pvzTariff.getAmount());
        });

        Optional<PvzTariff> thirdTariff = pvzTariffService.getRewardTariff(thirdPvz);
        assertTrue(thirdTariff.isPresent());
        thirdTariff.ifPresent(pvzTariff -> {
            assertEquals(3L, pvzTariff.getId());
            assertEquals(TariffValueType.ABSOLUTE, pvzTariff.getValueType());
            assertEquals(new BigDecimal("30"), pvzTariff.getAmount());
        });
    }

    @DisplayName("Тест на получение плоского тарифа")
    @ParameterizedTest
    @EnumSource(
            value = ServiceType.class,
            names = {"PVZ_CASH_COMPENSATION", "PVZ_CARD_COMPENSATION", "PVZ_DROPOFF", "PVZ_DROPOFF_RETURN",
                    "PVZ_RETURN", "PVZ_REWARD_YADO", "PVZ_REWARD_DBS", "PVZ_DBS_INCOME", "PVZ_DBS_OUTCOME"},
            mode = EnumSource.Mode.INCLUDE
    )
    void testGetFlatTariffs(ServiceType serviceType) {
        PickupPoint firstPvz = createNoneBrandedPickupPoint(1L, 4L);
        PickupPoint secondPvz = createNoneBrandedPickupPoint(2L, 5L);
        PickupPoint thirdPvz = createNoneBrandedPickupPoint(3L, 6L);

        Mockito.doAnswer(invocation -> new TariffsIterator((pageNumber, batchSize) -> {
            if (pageNumber != 0) {
                return List.of();
            }

            // подходит по пвз
            TariffDTO firstTariff = createTariffCardCompensationTariffWithoutMeta(1, BILLING_DEFAULT_DATE.minusYears(1), null, new Partner().id(1L).type(PartnerType.PVZ));
            firstTariff.setMeta(List.of(
                    createFlatDtoTariffMeta(new BigDecimal("50"))
            ));

            // подходит по партнеру
            TariffDTO secondTariff = createTariffCardCompensationTariffWithoutMeta(2, BILLING_DEFAULT_DATE.minusYears(1), null, new Partner().id(5L).type(PartnerType.PVZ_PARTNER));
            secondTariff.setMeta(List.of(
                    createFlatDtoTariffMeta(new BigDecimal("40"))
            ));

            // общий
            TariffDTO thirdTariff = createTariffCardCompensationTariffWithoutMeta(3, BILLING_DEFAULT_DATE.minusYears(1), null, null);
            thirdTariff.setMeta(List.of(
                    createFlatDtoTariffMeta(new BigDecimal("30"))
            ));

            return List.of(firstTariff, secondTariff, thirdTariff);

        })).when(tariffService).findTariffs(new TariffFindQuery()
                .isActive(true)
                .serviceType(TariffServiceUtils.convertFrom(serviceType))
                .targetDate(BILLING_DEFAULT_DATE)
        );

        Optional<PvzTariff> firstTariff = mapServiceToFlatTariff.get(serviceType).apply(firstPvz);
        assertTrue(firstTariff.isPresent());
        firstTariff.ifPresent(pvzTariff -> {
            assertEquals(1L, pvzTariff.getId());
            assertEquals(TariffValueType.ABSOLUTE, pvzTariff.getValueType());
            assertEquals(new BigDecimal("50"), pvzTariff.getAmount());
        });

        Optional<PvzTariff> secondTariff = mapServiceToFlatTariff.get(serviceType).apply(secondPvz);
        assertTrue(secondTariff.isPresent());
        secondTariff.ifPresent(pvzTariff -> {
            assertEquals(2L, pvzTariff.getId());
            assertEquals(TariffValueType.ABSOLUTE, pvzTariff.getValueType());
            assertEquals(new BigDecimal("40"), pvzTariff.getAmount());
        });

        Optional<PvzTariff> thirdTariff = mapServiceToFlatTariff.get(serviceType).apply(thirdPvz);
        assertTrue(thirdTariff.isPresent());
        thirdTariff.ifPresent(pvzTariff -> {
            assertEquals(3L, pvzTariff.getId());
            assertEquals(TariffValueType.ABSOLUTE, pvzTariff.getValueType());
            assertEquals(new BigDecimal("30"), pvzTariff.getAmount());
        });
    }

    @Test
    void testRewardTariffWithoutErrorsWithMixedRewardTariffs() {
        PickupPoint brandPvz = createBrandedPickupPoint(1L, BRAND_REGION_MOSCOW, 4L);
        PickupPoint noneBrandPvz = createNoneBrandedPickupPoint(2L, 5L);

        Mockito.doAnswer(invocation -> new TariffsIterator((pageNumber, batchSize) -> {
            if (pageNumber != 0) {
                return List.of();
            }

            TariffDTO firstTariff = createTariffPvzRewardTariffWithoutMeta(1, BILLING_DEFAULT_DATE.minusYears(1), null, null);
            firstTariff.setMeta(List.of(
                    createPvzRewardTariffMeta(0, 3, PvzBrandingTypeEnum.FULL, PvzTariffZoneEnum.MOSCOW, new BigDecimal("20")),
                    createPvzRewardTariffMeta(3, 6, PvzBrandingTypeEnum.FULL, PvzTariffZoneEnum.MOSCOW, new BigDecimal("15")),
                    createPvzRewardTariffMeta(6, null, PvzBrandingTypeEnum.FULL, PvzTariffZoneEnum.MOSCOW, new BigDecimal("10")),
                    createPvzRewardNoneBrandTariffMeta(new BigDecimal("40"))
            ));

            return List.of(firstTariff);

        })).when(tariffService).findTariffs(new TariffFindQuery()
                .isActive(true)
                .serviceType(TariffServiceUtils.convertFrom(ServiceType.PVZ_REWARD))
                .targetDate(BILLING_DEFAULT_DATE)
        );

        Optional<PvzTariff> brandTariff = pvzTariffService.getRewardTariff(brandPvz);
        assertTrue(brandTariff.isPresent());
        brandTariff.ifPresent(pvzTariff -> {
            assertEquals(1L, pvzTariff.getId());
            assertEquals(TariffValueType.RELATIVE, pvzTariff.getValueType());
            assertEquals(new BigDecimal("0.10"), pvzTariff.getAmount());
        });

        Optional<PvzTariff> noneBrandTariff = pvzTariffService.getRewardTariff(noneBrandPvz);
        assertTrue(noneBrandTariff.isPresent());
        noneBrandTariff.ifPresent(pvzTariff -> {
            assertEquals(1L, pvzTariff.getId());
            assertEquals(TariffValueType.ABSOLUTE, pvzTariff.getValueType());
            assertEquals(new BigDecimal("40"), pvzTariff.getAmount());
        });
    }

    @Test
    @DisplayName("Тест на ошибку для получения тарифов для расчет вознаграждения по GMV для небрендированного пвз")
    void testExceptionOnGetGmvTariffForNoneBranded() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            PickupPoint pvz = createNoneBrandedPickupPoint(1L, 1L);
            pvzTariffService.getBrandingRewardGmvTariffs(pvz);
        });

        assertEquals("PickupPoint must be FULL branded", exception.getMessage());
    }

    @Test
    @DisplayName("Тест на ошибку для получения тарифов для расчет вознаграждения от GMV для бпвз без billing branding since")
    void testExceptionOnGetGmvTariffWithoutBillingBrandedSinceProperty() {
        assertThrows(NullPointerException.class, () -> {
            PickupPoint pvz = createBrandedPickupPoint(1L, BRAND_REGION_MOSCOW, 1L);
            pvzTariffService.getBrandingRewardGmvTariffs(pvz);
        });
    }

    @Test
    @DisplayName("Тест на получения GMV тарифов на основании billing branded since")
    void testGetGmvTariffs() {
        LocalDate targetDate = LocalDate.of(2022, Month.FEBRUARY, 1);
        pvzTariffService = init(targetDate);

        Mockito.doAnswer(invocation -> new TariffsIterator((pageNumber, batchSize) -> {
            if (pageNumber != 0) {
                return List.of();
            }

            TariffDTO firstTariff = createTariffPvzRewardTariffWithoutMeta(1, targetDate, null, null);
            firstTariff.setMeta(List.of(
                    createPvzRewardTariffMeta(0, 1, PvzBrandingTypeEnum.FULL, PvzTariffZoneEnum.MOSCOW, new BigDecimal("10"))
                            .gmvFrom(0)
                            .gmvTo(1_000_000),
                    createPvzRewardTariffMeta(0, 1, PvzBrandingTypeEnum.FULL, PvzTariffZoneEnum.MOSCOW, new BigDecimal("5"))
                            .gmvFrom(1_000_000),
                    createPvzRewardTariffMeta(1, null, PvzBrandingTypeEnum.FULL, PvzTariffZoneEnum.MOSCOW, new BigDecimal("20"))
                            .gmvFrom(0),
                    createPvzRewardNoneBrandTariffMeta(new BigDecimal(40))
            ));

            return List.of(firstTariff);

        })).when(tariffService).findTariffs(new TariffFindQuery()
                .isActive(true)
                .serviceType(TariffServiceUtils.convertFrom(ServiceType.PVZ_REWARD))
                .targetDate(targetDate)
        );

        PickupPoint pvz1 = createBrandedPickupPoint(1L, BRAND_REGION_MOSCOW, 1L)
                .setBillingBrandedSince(targetDate);
        PickupPoint pvz2 = createBrandedPickupPoint(2L, BRAND_REGION_MOSCOW, 1L)
                .setBillingBrandedSince(targetDate.plusMonths(1));

        // для обоих пвз получаем 2 меты
        // первый пвз попадает, потому что время жизни = 0 месяцев
        // второй пвз попадет, потому что время жизни = -1 месяца от даты биллинга.
        List<PvzTariffRewardDto> gmvTariffs1 = pvzTariffService.getBrandingRewardGmvTariffs(pvz1);
        assertThat(gmvTariffs1, hasSize(2));

        List<PvzTariffRewardDto> gmvTariffs2 = pvzTariffService.getBrandingRewardGmvTariffs(pvz2);
        assertThat(gmvTariffs2, hasSize(2));
    }

    private PvzTariffService init(LocalDate billingDate) {
        return new PvzTariffService(
                billingDate,
                tariffService,
                pvzBrandedTariffZoneRegionRepository,
                pvzBrandedTariffZoneRepositor
        );
    }

    private PickupPoint createNoneBrandedPickupPoint(long id, Long partnerId) {
        return createPickupPoint(id, false, null, partnerId);
    }

    private PickupPoint createBrandedPickupPoint(long id, long brandRegionId, long partnerId) {
        return createPickupPoint(id, true, brandRegionId, partnerId);
    }

    private PickupPoint createPickupPoint(long id, boolean isBranded, Long brandRegionId, long partnerId) {
        return new PickupPoint()
                .setActive(true)
                .setBrandedSince(isBranded ? BRANDED_SINCE_DEFAULT_DATE : null)
                .setBrandingType(isBranded ? PickupPointBrandingType.FULL : PickupPointBrandingType.NONE)
                .setBrandRegionId(brandRegionId)
                .setCreatedAt(OffsetDateTime.now())
                .setId(id)
                .setName(id + "_name")
                .setPartnerId(partnerId)
                .setPvzMarketId(123L)
                .setReturnAllowed(true)
                .setUpdatedAt(OffsetDateTime.now());
    }

    private TariffDTO createTariffPvzRewardTariffWithoutMeta(long tariffId, LocalDate dateFrom, LocalDate dateTo, Partner partner) {
        TariffDTO tariffDTO = new TariffDTO();
        tariffDTO.setId(tariffId);
        tariffDTO.setIsActive(true);
        tariffDTO.setDateFrom(dateFrom);
        tariffDTO.setDateTo(dateTo);
        tariffDTO.setModelType(ModelType.THIRD_PARTY_LOGISTICS_PVZ);
        tariffDTO.setPartner(partner);
        tariffDTO.setServiceType(ServiceTypeEnum.PVZ_REWARD);
        return tariffDTO;
    }

    private TariffDTO createTariffCardCompensationTariffWithoutMeta(long tariffId, LocalDate dateFrom, LocalDate dateTo, Partner partner) {
        TariffDTO tariffDTO = new TariffDTO();
        tariffDTO.setId(tariffId);
        tariffDTO.setIsActive(true);
        tariffDTO.setDateFrom(dateFrom);
        tariffDTO.setDateTo(dateTo);
        tariffDTO.setModelType(ModelType.THIRD_PARTY_LOGISTICS_PVZ);
        tariffDTO.setPartner(partner);
        tariffDTO.setServiceType(ServiceTypeEnum.PVZ_CARD_COMPENSATION);
        return tariffDTO;
    }

    private PvzTariffRewardJsonSchema createPvzRewardTariffMeta(Integer fromMonth, Integer toMonth, PvzBrandingTypeEnum brandingType, PvzTariffZoneEnum tariffZone, BigDecimal amount) {
        PvzTariffRewardJsonSchema rewardJsonSchema = new PvzTariffRewardJsonSchema();
        rewardJsonSchema.setFromMonthAge(fromMonth);
        rewardJsonSchema.setToMonthAge(toMonth);
        rewardJsonSchema.setPvzBrandingType(brandingType);
        rewardJsonSchema.setPvzTariffZone(tariffZone);
        rewardJsonSchema.setAmount(amount);
        rewardJsonSchema.setType(CommonJsonSchema.TypeEnum.RELATIVE);
        rewardJsonSchema.setCurrency("RUB");
        rewardJsonSchema.setBillingUnit(BillingUnitEnum.ORDER);
        return rewardJsonSchema;
    }

    private PvzTariffRewardJsonSchema createPvzRewardNoneBrandTariffMeta(BigDecimal amount) {
        PvzTariffRewardJsonSchema rewardJsonSchema = new PvzTariffRewardJsonSchema();
        rewardJsonSchema.setPvzBrandingType(PvzBrandingTypeEnum.NONE);
        rewardJsonSchema.setAmount(amount);
        rewardJsonSchema.setType(CommonJsonSchema.TypeEnum.ABSOLUTE);
        rewardJsonSchema.setCurrency("RUB");
        rewardJsonSchema.setBillingUnit(BillingUnitEnum.ORDER);
        return rewardJsonSchema;
    }

    private PvzTariffFlatJsonSchema createFlatDtoTariffMeta(BigDecimal amount) {
        PvzTariffFlatJsonSchema rewardJsonSchema = new PvzTariffFlatJsonSchema();
        rewardJsonSchema.setAmount(amount);
        rewardJsonSchema.setType(CommonJsonSchema.TypeEnum.ABSOLUTE);
        rewardJsonSchema.setCurrency("RUB");
        rewardJsonSchema.setBillingUnit(BillingUnitEnum.ORDER);
        return rewardJsonSchema;
    }
}
