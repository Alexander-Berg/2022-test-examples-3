package ru.yandex.market.billing.fulfillment.billing.storage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.fulfillment.billing.storage.tariff.StorageTurnoverTariffService;
import ru.yandex.market.billing.fulfillment.tariffs.TariffsIterator;
import ru.yandex.market.billing.fulfillment.tariffs.TariffsService;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.mbi.tariffs.client.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.FfStorageTurnoverJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.ModelType;
import ru.yandex.market.mbi.tariffs.client.model.Partner;
import ru.yandex.market.mbi.tariffs.client.model.PartnerType;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;

/**
 * Тесты для {@link ru.yandex.market.billing.fulfillment.billing.storage.tariff.StorageTurnoverTariffService}
 */
public class StorageTurnoverTariffServiceTest extends FunctionalTest {
    private static long tariffId = 0;
    private static final LocalDate LOCAL_DATE_1_JULE_2022 = LocalDate.of(2022, Month.JULY, 1);
    private static final LocalDate LOCAL_DATE_10_JULE_2022 = LocalDate.of(2022, Month.JULY, 1);

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private TariffsService tariffsService;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> new TariffsIterator((pageNumber, batchSize) -> {
            TariffFindQuery findQuery = invocation.getArgument(0);
            assertThat(findQuery.getIsActive()).as("Only active tariffs should be available").isTrue();

            if (findQuery.getServiceType() != ServiceTypeEnum.FF_STORAGE_TURNOVER) {
                throw new IllegalArgumentException("Allowed only FF_STORAGE_TURNOVER");
            }

            return pageNumber != 0 ? List.of() : tariffs();
        }))
                .when(tariffsService).findTariffs(new TariffFindQuery()
                        .isActive(true)
                        .serviceType(ServiceTypeEnum.FF_STORAGE_TURNOVER)
                        .targetDate(LOCAL_DATE_10_JULE_2022)
                );

    }

    @ParameterizedTest(name = "[{index}] {displayName}")
    @MethodSource("testTariffsFromTariffenatorData")
    void testTariffsFromTariffenator(
            double turnover,
            long partnerId,
            long departmentId,
            BigDecimal expectedValue
    ) {
        environmentService.setValue("StorageTurnoverTariffService.useTariffenator", "true");
        StorageTurnoverTariffService storageTurnoverTariffService = new StorageTurnoverTariffService(
                LOCAL_DATE_10_JULE_2022,
                tariffsService,
                environmentService
        );
        BigDecimal actualValue = storageTurnoverTariffService.getTariff(turnover, partnerId, departmentId);
        Assertions.assertEquals(
                0,
                actualValue.compareTo(expectedValue),
                "Actual: " + actualValue + ", expected : " + expectedValue
        );
    }

    private static Stream<Arguments> testTariffsFromTariffenatorData() {
        return Stream.of(
                // тариф на конкретного партнер + департамент
                Arguments.of(
                        130d,
                        1L,
                        90402L,
                        BigDecimal.valueOf(40)
                ),
                Arguments.of(
                        Double.POSITIVE_INFINITY,
                        1L,
                        90402L,
                        BigDecimal.valueOf(60)
                ),
                // тариф на конкретного партнера (отдельного департамента нет)
                Arguments.of(
                        130d,
                        1L,
                        90401L,
                        BigDecimal.valueOf(50)
                ),
                Arguments.of(
                        Double.POSITIVE_INFINITY,
                        1L,
                        90401L,
                        BigDecimal.valueOf(70)
                ),
                // тариф на общего партнера + департамент
                Arguments.of(
                        130d,
                        2L,
                        90402L,
                        BigDecimal.valueOf(400)
                ),
                Arguments.of(
                        Double.POSITIVE_INFINITY,
                        2L,
                        90402L,
                        BigDecimal.valueOf(600)
                ),
                // тариф на конкретного партнера (отдельного департамента нет)
                Arguments.of(
                        130d,
                        2L,
                        90401L,
                        BigDecimal.valueOf(500)
                ),
                Arguments.of(
                        Double.POSITIVE_INFINITY,
                        2L,
                        90401L,
                        BigDecimal.valueOf(700)
                )
        );
    }



    private List<TariffDTO> tariffs() {
        return List.of(
                createTariff(1L, List.of(
                        // на конкретный департамент тариф
                        createMeta(90402L, BigDecimal.ZERO, BigDecimal.valueOf(120), BigDecimal.valueOf(20)),
                        createMeta(90402L, BigDecimal.valueOf(120), BigDecimal.valueOf(150), BigDecimal.valueOf(40)),
                        createMeta(90402L, BigDecimal.valueOf(150), null, BigDecimal.valueOf(60)),

                        // на общий департамент
                        createMeta(null, BigDecimal.ZERO, BigDecimal.valueOf(120), BigDecimal.valueOf(30)),
                        createMeta(null, BigDecimal.valueOf(120), BigDecimal.valueOf(150), BigDecimal.valueOf(50)),
                        createMeta(null, BigDecimal.valueOf(150), null, BigDecimal.valueOf(70))
                )),
                createTariff(null, List.of(
                        // на конкретный департамент тариф
                        createMeta(90402L, BigDecimal.ZERO, BigDecimal.valueOf(120), BigDecimal.valueOf(200)),
                        createMeta(90402L, BigDecimal.valueOf(120), BigDecimal.valueOf(150), BigDecimal.valueOf(400)),
                        createMeta(90402L, BigDecimal.valueOf(150), null, BigDecimal.valueOf(600)),

                        // на общий департамент
                        createMeta(null, BigDecimal.ZERO, BigDecimal.valueOf(120), BigDecimal.valueOf(300)),
                        createMeta(null, BigDecimal.valueOf(120), BigDecimal.valueOf(150), BigDecimal.valueOf(500)),
                        createMeta(null, BigDecimal.valueOf(150), null, BigDecimal.valueOf(700))
                ))
        );
    }

    private TariffDTO createTariff(
            Long partnerId,
            List<Object> meta
    ) {
        TariffDTO tariff = new TariffDTO();
        tariff.setId(tariffId++);
        tariff.setPartner(partnerId == null ? null : new Partner().id(partnerId).type(PartnerType.SUPPLIER));
        tariff.setIsActive(true);
        tariff.setDateFrom(LOCAL_DATE_1_JULE_2022);
        tariff.setModelType(ModelType.FULFILLMENT_BY_YANDEX);
        tariff.setServiceType(ServiceTypeEnum.FF_STORAGE_TURNOVER);
        tariff.setMeta(meta);
        return tariff;
    }

    private CommonJsonSchema createMeta(
            Long categoryId,
            BigDecimal turnoverFrom,
            BigDecimal turnoverTo,
            BigDecimal amount
    ) {
        return new FfStorageTurnoverJsonSchema()
                .categoryId(categoryId)
                .turnoverFrom(turnoverFrom)
                .turnoverTo(turnoverTo)
                .billingUnit(BillingUnitEnum.ITEM)
                .amount(amount)
                .currency("RUB")
                .type(CommonJsonSchema.TypeEnum.ABSOLUTE);
    }
}
