package ru.yandex.market.billing.distribution.share;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.distribution.imports.dao.DistributionPartnerDao;
import ru.yandex.market.billing.distribution.share.model.DistributionPartnerSegmentExtended;
import ru.yandex.market.billing.distribution.share.stats.DistributionOrderStatsRegionalSettings;
import ru.yandex.market.billing.pg.dao.PgCategoryDao;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.billing.distribution.share.DistributionTariffName;
import ru.yandex.market.core.billing.distribution.share.stats.model.DistributionOrderCalculationOrder;
import ru.yandex.market.core.fulfillment.tariff.TariffsIterator;
import ru.yandex.market.core.fulfillment.tariff.TariffsService;
import ru.yandex.market.mbi.tariffs.client.model.DistributionJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.Partner;
import ru.yandex.market.mbi.tariffs.client.model.PartnerType;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "db/ApiDistributionTariffSupplierTest.before.csv")
public class ApiDistributionTariffSupplierTest extends FunctionalTest {
    private static final LocalDate DAY1 = LocalDate.of(2022, Month.APRIL, 1);
    private static final LocalDate DAY2 = LocalDate.of(2022, Month.APRIL, 2);
    private static final long NORMAL_CLID_MARKETING_BLOGGER = 1111111;
    private static final long NORMAL_CLID_CLOSER_OTHERS = 2222222;
    private static final long CUSTOM_CLID = 1212121;
    private static final long CEHAC_SUBCATEGORY = 44444;
    private static final long COMMON_SUBCATEGORY = 111;
    private static final long TARGET_REGION_ID = 10995;
    private static final long NON_TARGET_REGION_ID = 10996;
    private static final TariffDTO TARIFF1_CUSTOM = createTariffDto(
            1L, CUSTOM_CLID, DAY1, DAY2,
            List.of(
                    schema(11L, null, "ALL", BigDecimal.valueOf(50)),
                    schema(444L, null, "CEHAC", BigDecimal.valueOf(30))
            ));
    private static final TariffDTO TARIFF2_CUSTOM = createTariffDto(
            2L, CUSTOM_CLID, DAY2, DAY2.plusDays(1),
            List.of(
                    schema(11L, null, "ALL", BigDecimal.valueOf(45)),
                    schema(444L, null, "CEHAC", BigDecimal.valueOf(25))
            ));
    private static final TariffDTO TARIFF_COMMON = createTariffDto(
            3L, null, DAY1, DAY2.plusDays(1),
            List.of(
                    schema(11L, "marketing-bloggers", "ALL", BigDecimal.valueOf(20)),
                    schema(444L, "marketing-bloggers", "CEHAC", BigDecimal.valueOf(15)),
                    schema(11L,  "closer-others", "ALL", BigDecimal.valueOf(10)),
                    schema(444L, "closer-others", "CEHAC", BigDecimal.valueOf(5)),
                    schema(11L,
                            DistributionPartnerSegmentExtended.MOBILE_NONTARGET_GEO.getId(),
                            DistributionTariffName.ALL.name(),
                            BigDecimal.valueOf(40))
            ));

    private final TariffsService tariffsService = Mockito.mock(TariffsService.class);

    @Autowired
    private DistributionPartnerDao distributionPartnerDao;

    @Autowired
    private PgCategoryDao pgCategoryDao;

    @Autowired
    private DistributionOrderStatsRegionalSettings regionalSettings;

    private ApiDistributionTariffSupplier supplier;

    private ClidInfoCache getClidInfoCache() {
        return new ClidInfoCache(distributionPartnerDao);
    }

    @BeforeEach
    public void setup() {
        setupTariffsServiceBehaviour();
        supplier = new ApiDistributionTariffSupplier(
                tariffsService,
                pgCategoryDao,
                getClidInfoCache(),
                regionalSettings);
    }

    @Test
    public void test() {
        getAndAssert(CUSTOM_CLID, DAY1, CEHAC_SUBCATEGORY,
                new DistributionTariffRateAndName(
                        BigDecimal.valueOf(0.3),
                        DistributionTariffName.CEHAC,
                        null));

        getAndAssert(CUSTOM_CLID, DAY2, CEHAC_SUBCATEGORY,
                new DistributionTariffRateAndName(
                        BigDecimal.valueOf(0.25),
                        DistributionTariffName.CEHAC,
                        null));
        getAndAssert(NORMAL_CLID_MARKETING_BLOGGER, DAY1, CEHAC_SUBCATEGORY,
                new DistributionTariffRateAndName(
                        BigDecimal.valueOf(0.15),
                        DistributionTariffName.CEHAC,
                        null));

        getAndAssert(NORMAL_CLID_MARKETING_BLOGGER, DAY2, COMMON_SUBCATEGORY,
                new DistributionTariffRateAndName(BigDecimal.valueOf(0.2),
                        DistributionTariffName.ALL,
                        null));

        getAndAssert(NORMAL_CLID_CLOSER_OTHERS, DAY2, COMMON_SUBCATEGORY,
                new DistributionTariffRateAndName(
                        BigDecimal.valueOf(0.1),
                        DistributionTariffName.ALL,
                        null));
    }

    private void getAndAssertFirstOrder(long clid, LocalDate day, long categoryId, DistributionTariffRateAndName expectedResult) {
        getAndAssert(clid, day, categoryId, TARGET_REGION_ID, false, true, expectedResult);
    }

    private void getAndAssert(long clid, LocalDate day, long categoryId, DistributionTariffRateAndName expectedResult) {
        getAndAssert(clid, day, categoryId, TARGET_REGION_ID, false, false, expectedResult);
    }

    private void getAndAssert(long clid, LocalDate day,
                              long categoryId, Long regionId, boolean isMobileInstall, boolean isFirstOrder,
                              DistributionTariffRateAndName expectedResult) {
        var result = supplier.get(DistributionOrderCalculationOrder.builder()
                        .setClid(clid)
                        .setOrderCreationTime(day.atTime(2, 3))
                        .setMobileInstall(isMobileInstall)
                        .setIsFirstOrder(isFirstOrder)
                        .build(),
                categoryId,
                regionId
        );
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void testBadClid() {
        // считается по ставке closer-others
        getAndAssert(18L, DAY1, 11L,
                new DistributionTariffRateAndName(BigDecimal.valueOf(0.1), DistributionTariffName.ALL, null));
    }

    @Test
    public void testBadDay() {
        // тарифов для этого дня нет - UNKNOWN
        getAndAssert(NORMAL_CLID_CLOSER_OTHERS, DAY2.plusDays(2), 11L,
                new DistributionTariffRateAndName(BigDecimal.ZERO, DistributionTariffName.UNKNOWN, null));
    }

    @Test
    public void testBadCategory() {
        getAndAssert(NORMAL_CLID_CLOSER_OTHERS, DAY2, 333L,
                new DistributionTariffRateAndName(BigDecimal.ZERO, DistributionTariffName.UNKNOWN, null));
    }

    @Test
    public void testMultipleTariffsForDate() {
        var day = LocalDate.of(2022, Month.MARCH, 1);
        var query = new TariffFindQuery()
                .serviceType(ServiceTypeEnum.DISTRIBUTION)
                .targetDate(day)
                .isActive(true);
        when(tariffsService.findTariffs(query)).thenReturn(
                getTariffsIterator(List.of(TARIFF_COMMON, TARIFF_COMMON))
        );
        getAndAssert(NORMAL_CLID_CLOSER_OTHERS, day, 11L,
                new DistributionTariffRateAndName(BigDecimal.valueOf(0.1), DistributionTariffName.ALL, null));
    }

    @Test
    public void testBothClidAndSegmentNull() {
        var day = LocalDate.of(2022, Month.JANUARY, 1);
        var query = new TariffFindQuery()
                .serviceType(ServiceTypeEnum.DISTRIBUTION)
                .targetDate(day)
                .isActive(true);
        when(tariffsService.findTariffs(query)).thenReturn(
                getTariffsIterator(List.of(createTariffDto(
                        1001L, null, day, null,
                        List.of(
                                schema(11L, null, "ALL", BigDecimal.valueOf(0.5)
                        ))))));
        getAndAssert(NORMAL_CLID_CLOSER_OTHERS, day, 11L,
                new DistributionTariffRateAndName(BigDecimal.ZERO, DistributionTariffName.UNKNOWN, null));
    }

    @Test
    public void testBadSegmentInTariff() {
        var day = LocalDate.of(2022, Month.JANUARY, 1);
        var query = new TariffFindQuery()
                .serviceType(ServiceTypeEnum.DISTRIBUTION)
                .targetDate(day)
                .isActive(true);
        when(tariffsService.findTariffs(query)).thenReturn(
                getTariffsIterator(List.of(createTariffDto(
                        1001L, null, day, null,
                        List.of(
                                schema(11L, "i-dont-know-what-is-segment", "ALL", BigDecimal.valueOf(0.5)

                                ))))));
        getAndAssert(NORMAL_CLID_CLOSER_OTHERS, day, 11L,
                new DistributionTariffRateAndName(
                        BigDecimal.ZERO, DistributionTariffName.UNKNOWN, null));
    }

    @Test
    public void testBadTariffName() {
        var day = LocalDate.of(2022, Month.JANUARY, 1);
        var query = new TariffFindQuery()
                .serviceType(ServiceTypeEnum.DISTRIBUTION)
                .targetDate(day)
                .isActive(true);
        when(tariffsService.findTariffs(query)).thenReturn(
                getTariffsIterator(List.of(createTariffDto(
                        1001L, null, day, null,
                        List.of(
                                schema(11L, "closer-others", "CEHAC_NEW", BigDecimal.valueOf(0.5)
                                ))))));
        getAndAssert(NORMAL_CLID_CLOSER_OTHERS, day, 11L,
                new DistributionTariffRateAndName(
                        BigDecimal.ZERO, DistributionTariffName.UNKNOWN, null));
    }

    @Test
    public void testMaxPaymentRub() {
        var query = new TariffFindQuery()
                .serviceType(ServiceTypeEnum.DISTRIBUTION)
                .targetDate(DAY1)
                .isActive(true);
        when(tariffsService.findTariffs(query)).thenReturn(
                getTariffsIterator(
                        List.of(
                        createTariffDto(1L, CUSTOM_CLID, DAY1, DAY2,
                        List.of(
                                schema(11L, null, "ALL", BigDecimal.valueOf(50))
                                .maxPaymentRub(1500)
                        ))
        )));
        getAndAssert(CUSTOM_CLID, DAY1, 11L,
                new DistributionTariffRateAndName(
                        BigDecimal.valueOf(0.5), DistributionTariffName.ALL, BigDecimal.valueOf(1500)));
    }

    @Test
    public void testFirstOrderTariff() {
        var query = new TariffFindQuery()
                .serviceType(ServiceTypeEnum.DISTRIBUTION)
                .targetDate(DAY1)
                .isActive(true);
        when(tariffsService.findTariffs(query)).thenReturn(
                getTariffsIterator(
                        List.of(
                                createTariffDto(1L, CUSTOM_CLID, DAY1, DAY2,
                                        List.of(
                                                schema(11L, null, "ALL", BigDecimal.valueOf(50)).isFirstOrderOnly(true),
                                                schema(11L, null, "ALL", BigDecimal.valueOf(20))
                                        ))
                        )));
        getAndAssertFirstOrder(CUSTOM_CLID, DAY1, 11L,
                new DistributionTariffRateAndName(
                        BigDecimal.valueOf(0.5), DistributionTariffName.ALL, null));
        getAndAssert(CUSTOM_CLID, DAY1, 11L,
                new DistributionTariffRateAndName(
                        BigDecimal.valueOf(0.2), DistributionTariffName.ALL, null));
    }

    @Test
    public void testMobileInstallTariff() {
        getAndAssert(NORMAL_CLID_CLOSER_OTHERS, DAY2, 11L, TARGET_REGION_ID, true, false,
                new DistributionTariffRateAndName(BigDecimal.valueOf(0.4), DistributionTariffName.ALL, null));

        getAndAssert(NORMAL_CLID_MARKETING_BLOGGER, DAY2, 11L, NON_TARGET_REGION_ID, true, false,
                new DistributionTariffRateAndName( BigDecimal.valueOf(0.4), DistributionTariffName.ALL, null));
    }

    private void setupTariffsServiceBehaviour() {
        var query1 = new TariffFindQuery()
                .serviceType(ServiceTypeEnum.DISTRIBUTION)
                .targetDate(DAY1)
                .isActive(true);
        when(tariffsService.findTariffs(query1)).thenReturn(
                getTariffsIterator(List.of(TARIFF1_CUSTOM, TARIFF_COMMON))
        );
        var query2 = new TariffFindQuery()
                .serviceType(ServiceTypeEnum.DISTRIBUTION)
                .targetDate(DAY2)
                .isActive(true);
        when(tariffsService.findTariffs(query2)).thenReturn(
                getTariffsIterator(List.of(TARIFF2_CUSTOM, TARIFF_COMMON)));
    }

    private static TariffsIterator getTariffsIterator(List<TariffDTO> tariffs) {
        return new TariffsIterator(
                (pageNum, batchSize) -> {
                    if (pageNum == 0) {
                        return tariffs;
                    } else {
                        return List.of();
                    }
                },
                tariffs.size());
    }

    private static TariffDTO createTariffDto(
            long id, @Nullable Long clid, LocalDate dateFrom, LocalDate dateTo, List<?> schemas) {
        var result = new TariffDTO().id(id).isActive(true);
        if (clid != null) {
            result.setPartner(new Partner().id(clid).type(PartnerType.DISTRIBUTION));
        }
        result.setDateFrom(dateFrom);
        result.setDateTo(dateTo);
        schemas.forEach(result::addMetaItem);
        return result;
    }

    private static DistributionJsonSchema schema(
            @Nullable Long categoryId,
            @Nullable String partnerSegment,
            String tariffName,
            BigDecimal value) {
        var result = new DistributionJsonSchema()
                .categoryId(categoryId)
                .tariffName(tariffName)
                .partnerSegmentTariffKey(partnerSegment);
        result.setAmount(value);
        return result;
    }

}