package ru.yandex.market.core.partner;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.delivery.experiment.StocksByPiExperiment;
import ru.yandex.market.core.partner.model.PartnerTypeAwareInfo;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты для {@link PartnerTypeAwareService}.
 */
public class PartnerTypeAwareServiceTest extends FunctionalTest {

    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;

    @Autowired
    @Qualifier("environmentService")
    private EnvironmentService environmentService;

    @Autowired
    private StocksByPiExperiment stocksByPiExperiment;

    @BeforeEach
    public void init() {
        stocksByPiExperiment.reset();
        environmentService.setValue(StocksByPiExperiment.USE_STOCKS_BY_PI_IN_MBI_VAR, "false");
    }

    @Test
    @DbUnitDataSet(before = "PartnerTypeAwareServiceTest.testIsSmbBatch.before.csv")
    void testIsSmbBatch() {
        var shopIds = LongStream.range(10001L, 10012L) // shop 10011 does not exist
                .boxed()
                .collect(Collectors.toSet());

        assertThat(partnerTypeAwareService.filterSmbShops(shopIds))
                .isEqualTo(Set.of(10001L, 10010L));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testDataForDropshipBySeller")
    @DbUnitDataSet(before = "PartnerTypeAwareServiceTest.testIsDsbs.before.csv")
    void testIsDropshipBySeller(String testName, long partnerId, boolean expectedResult) {
        assertThat(partnerTypeAwareService.isDropshipBySeller(partnerId))
                .isEqualTo(expectedResult);
        assertThat(partnerTypeAwareService.getPartnerTypeAwareInfo(partnerId).isDropshipBySeller())
                .isEqualTo(expectedResult);
    }

    @Test
    @DbUnitDataSet(before = "PartnerTypeAwareServiceTest.testIsDsbs.before.csv")
    void testIsDropshipBySellerBulk() {
        Map<Long, Boolean> dsbsMap = partnerTypeAwareService.isDropshipBySeller(
                List.of(10001L, 10006L, 10007L, 10008L, 999L));

        assertThat(dsbsMap)
                .isEqualTo(Map.of(10001L, false, 10006L, true, 10007L, true, 10008L, true));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testDataForCrossdock")
    @DbUnitDataSet(before = "PartnerTypeAwareServiceTest.testIsCrossDock.before.csv")
    void testIsCrossdockSupplier(String testName, long partnerId, boolean expectedResult) {
        assertThat(partnerTypeAwareService.isCrossdockSupplier(partnerId))
                .isEqualTo(expectedResult);
        assertThat(partnerTypeAwareService.getPartnerTypeAwareInfo(partnerId).isCrossdock())
                .isEqualTo(expectedResult);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testDataForCampaignType")
    @DbUnitDataSet(before = "PartnerTypeAwareServiceTest.testIsCrossDock.before.csv")
    void testShopOrSupplier(String testName, long partnerId, CampaignType campaignType, boolean expectedResult) {
        PartnerTypeAwareInfo info = partnerTypeAwareService.getPartnerTypeAwareInfo(partnerId);
        assertThat(partnerTypeAwareService.isCampaignPresentType(partnerId, campaignType))
                .isEqualTo(expectedResult);
        assertThat(info.isShop() == campaignType.isShop())
                .isEqualTo(expectedResult);
        assertThat(info.isSupplier() == campaignType.isSupplier())
                .isEqualTo(expectedResult);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testDataForPartnerTypeAwareInfo")
    @DbUnitDataSet(before = "PartnerTypeAwareServiceTest.testIsCrossDock.before.csv")
    void testPartnerTypeAwareInfo(String testName, long partnerId, PartnerTypeAwareInfo info) {
        PartnerTypeAwareInfo actually = partnerTypeAwareService.getPartnerTypeAwareInfo(partnerId);
        assertThat(actually.getPartnerId())
                .isEqualTo(info.getPartnerId());
        assertThat(actually.isCrossdock())
                .isEqualTo(info.isCrossdock());
        assertThat(actually.isFulfillment())
                .isEqualTo(info.isFulfillment());
        assertThat(actually.isTurboPlus())
                .isEqualTo(info.isTurboPlus());
        assertThat(actually.isDropshipBySeller())
                .isEqualTo(info.isDropshipBySeller());
        assertThat(actually.isShop())
                .isEqualTo(info.isShop());
        assertThat(actually.isSupplier())
                .isEqualTo(info.isSupplier());
    }

    @ParameterizedTest
    @MethodSource("isUnitedCatalogSource")
    @DbUnitDataSet(before = "PartnerTypeAwareServiceTest.testIsUnitedCatalog.before.csv")
    void testIsUnitedCatalog(long partnerId, boolean expectedResult) {
        assertThat(partnerTypeAwareService.isUnitedCatalogPartner(partnerId))
                .isEqualTo(expectedResult);
    }

    @Test
    @DisplayName("Из списка всех партнеров вернутся только те, что в Едином Каталоге.")
    @DbUnitDataSet(before = "PartnerTypeAwareServiceTest.testIsUnitedCatalog.before.csv")
    void getUnitedPartnerList_allTypes_unitedOnly() {
        assertThat(partnerTypeAwareService.getUnitedPartnerList(List.of(10001L, 10002L, 10003L)))
                .isEqualTo(Set.of(10001L));
    }

    @Test
    @DbUnitDataSet(before = "PartnerTypeAwareServiceTest.testIsCrossDock.before.csv")
    void testPartnerTypeAwareInfoBatch() {
        List<Long> ids = getIds(testDataForPartnerTypeAwareInfo());
        Map<Long, PartnerTypeAwareInfo> infoById = partnerTypeAwareService.getPartnersTypeAwareInfo(
                ids
        );
        assertThat(infoById.size()).isEqualTo(ids.size());
        List<PartnerTypeAwareInfo> infos = getPartnerTypeAwareInfos(testDataForPartnerTypeAwareInfo());
        for (int k = 0; k < ids.size(); k++) {
            Long id = ids.get(k);
            PartnerTypeAwareInfo expected = infos.get(k);
            PartnerTypeAwareInfo actual = infoById.get(id);
            assertThat(actual.getPartnerId())
                    .isEqualTo(expected.getPartnerId());
            assertThat(actual.isCrossdock())
                    .isEqualTo(expected.isCrossdock());
            assertThat(actual.isFulfillment())
                    .isEqualTo(expected.isFulfillment());
            assertThat(actual.isTurboPlus())
                    .isEqualTo(expected.isTurboPlus());
            assertThat(actual.isDropshipBySeller())
                    .isEqualTo(expected.isDropshipBySeller());
            assertThat(actual.isShop())
                    .isEqualTo(expected.isShop());
            assertThat(actual.isSupplier())
                    .isEqualTo(expected.isSupplier());
        }
    }

    @Test
    @DbUnitDataSet(before = "PartnerTypeAwareServiceTest.testIsDsbs.before.csv")
    void testIsDropshipBySellerBatch() {
        testBatch(PartnerTypeAwareServiceTest::testDataForDropshipBySeller, PartnerTypeAwareInfo::isDropshipBySeller);
    }

    @Test
    @DbUnitDataSet(before = "PartnerTypeAwareServiceTest.testIsCrossDock.before.csv")
    void testIsCrossdockSupplierBatch() {
        testBatch(PartnerTypeAwareServiceTest::testDataForCrossdock, PartnerTypeAwareInfo::isCrossdock);
    }

    @Test
    @DbUnitDataSet(before = "PartnerTypeAwareServiceTest.testIsDropshipBatch.before.csv")
    void testIsDropshipSupplierBatch() {
        Set<Long> expected = new HashSet<>();
        expected.add(11L);
        expected.add(13L);
        List<Long> partnerIds = List.of(11L, 12L, 13L, 14L, 15L, 16L);
        Set<Long> result = partnerTypeAwareService.isDropship(partnerIds);
        assertEquals(expected, result);
    }

    private void testBatch(Supplier<Stream<Arguments>> argumentsSupplier, Predicate<PartnerTypeAwareInfo> predicate) {
        List<Long> ids = getIds(argumentsSupplier.get());
        Map<Long, PartnerTypeAwareInfo> infoById = partnerTypeAwareService.getPartnersTypeAwareInfo(
                ids
        );
        assertThat(infoById.size()).isEqualTo(ids.size());
        List<Boolean> expectedResults = getExpectedResults(argumentsSupplier.get());
        for (int k = 0; k < ids.size(); k++) {
            Long id = ids.get(k);
            Boolean expectedResult = expectedResults.get(k);
            assertThat(predicate.test(infoById.get(id))).isEqualTo(expectedResult);
        }
    }

    @Nonnull
    private List<Long> getIds(Stream<Arguments> argumentsStream) {
        return argumentsStream.map(arg -> (Long) arg.get()[1]).collect(Collectors.toList());
    }

    @Nonnull
    private List<Boolean> getExpectedResults(Stream<Arguments> argumentsStream) {
        return argumentsStream.map(arg -> (Boolean) arg.get()[2]).collect(Collectors.toList());
    }

    @Nonnull
    private List<PartnerTypeAwareInfo> getPartnerTypeAwareInfos(Stream<Arguments> argumentsStream) {
        return argumentsStream.map(arg -> (PartnerTypeAwareInfo) arg.get()[2]).collect(Collectors.toList());
    }

    @Nonnull
    private static Stream<Arguments> isUnitedCatalogSource() {
        return Stream.of(
                Arguments.of(10001L, true),
                Arguments.of(10002L, false),
                // Статус NO по умолчанию
                Arguments.of(10003L, false)
        );
    }

    @Nonnull
    private static Stream<Arguments> testDataForPartnerTypeAwareInfo() {
        return Stream.of(
                Arguments.of("Crossdock & Fulfillment", 10002L,
                        PartnerTypeAwareInfo.builder()
                                .setClickCollect(false)
                                .setCrossdock(true)
                                .setTurboPlus(false)
                                .setFulfillment(true)
                                .setDropshipBySeller(false)
                                .setDropship(false)
                                .setPartnerId(10002L)
                                .setSupplier(true)
                                .setShop(false)
                                .build()),
                Arguments.of(
                        "!Crossdock & !Fulfillment", 10001L,
                        PartnerTypeAwareInfo.builder()
                                .setClickCollect(false)
                                .setCrossdock(false)
                                .setSupplier(false)
                                .setShop(true)
                                .setTurboPlus(false)
                                .setFulfillment(false)
                                .setDropshipBySeller(false)
                                .setDropship(false)
                                .setPartnerId(10001L)
                                .build()
                )
        );
    }

    @Nonnull
    private static Stream<Arguments> testDataForCrossdock() {
        return Stream.of(
                Arguments.of("Фича CROSSDOCK в REVOKE", 10001L, false),
                Arguments.of("Фича CROSSDOCK в SUCCESS без cutoff", 10002L, true),
                Arguments.of("Фича CROSSDOCK в SUCCESS с PARTNER cutoff", 10003L, true),
                Arguments.of("Фича не CROSSDOCK", 10004L, false),
                Arguments.of("Фича CROSSDOCK в SUCCESS с HIDDEN cutoff", 10005L, false)
        );
    }

    @Nonnull
    private static Stream<Arguments> testDataForCampaignType() {
        return Stream.of(
                Arguments.of("Магазин", 10001L, CampaignType.SHOP, true),
                Arguments.of("Не магазин", 10002L, CampaignType.SHOP, false),
                Arguments.of("Поставщик", 10003L, CampaignType.SUPPLIER, true),
                Arguments.of("Не поставщик", 10004L, CampaignType.SUPPLIER, false)
        );
    }

    @Nonnull
    private static Stream<Arguments> testDataForTurboPlus() {
        return Stream.of(
                Arguments.of("Фича турбо не заперсисчена", 10002L, false),
                Arguments.of("Фича турбо в REVOKE", 10003L, false),
                Arguments.of("Фича турбо в SUCCESS", 10004L, true),
                Arguments.of("Фича турбо в DONT_WANT ", 10005L, false),
                Arguments.of("Фича SELF_DELIVERY в revoke", 10006L, false)
        );
    }

    @Nonnull
    private static Stream<Arguments> testDataForDropshipBySeller() {
        return Stream.of(
                Arguments.of("Фича DROPSHIP_BY_SELLER в DONT_WANT", 10001L, false),
                Arguments.of("Фича SELF_DELIVERY в REVOKE", 10006L, true),
                Arguments.of("Фича SELF_DELIVERY с катоффом PARTNER", 10007L, true),
                Arguments.of("Фича SELF_DELIVERY в SUCCESS", 10008L, true)
        );
    }

    @Test
    @DbUnitDataSet(before = "PartnerTypeAwareServiceTest.testStocksByPiFlagOff.before.csv")
    public void testStocksByPiFlagOff() {
        assertThat(partnerTypeAwareService.worksWithStocksByPartnerInterface(10001L)).isFalse();
    }

    @Test
    @DbUnitDataSet(before = "PartnerTypeAwareServiceTest.testStocksByPiFlagOn.before.csv")
    public void testStocksByPiFlagOn() {
        environmentService.setValue(StocksByPiExperiment.USE_STOCKS_BY_PI_IN_MBI_VAR, "true");
        assertThat(partnerTypeAwareService.worksWithStocksByPartnerInterface(10001L)).isTrue();
    }

    @Test
    @DbUnitDataSet(before = "PartnerTypeAwareServiceTest.testStocksByPiFlagOnAndNoValue.before.csv")
    public void testStocksByPiFlagOnAndNoValue() {
        assertThat(partnerTypeAwareService.worksWithStocksByPartnerInterface(10001L)).isFalse();
    }
}
