package ru.yandex.market.core.yt.dynamic.samovar;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import Market.DataCamp.API.UpdateTask;
import Market.DataCamp.DataCampOfferMeta;
import com.google.protobuf.Timestamp;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.common.util.exception.ExceptionCollector;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.misc.resource.ResourceAccessCredentials;
import ru.yandex.market.core.yt.dynamic.samovar.feed.SamovarFeed;
import ru.yandex.market.core.yt.dynamic.samovar.feed.SamovarFeedMapper;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass.FeedInfo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты для {@link SamovarFeedImporterService}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@DbUnitDataSet(before = "SamovarFeedImporterServiceEnv.before.csv")
class SamovarFeedImporterServiceFunctionalTest extends FunctionalTest {

    @Autowired
    @Qualifier("samovarFeedsImporterService")
    private SamovarFeedImporterService samovarFeedImporterService;

    @Autowired
    private EnvironmentService environmentService;

    @Test
    @DisplayName("Получение данных для записи в YT с проверкой измененных урлов")
    void testFeedServiceMerge() {
        List<SamovarFeed> samovarFeedsYt = List.of(
                createSamovarFeed("http://feed1.ru/", Map.of(1L, 113L), CampaignType.SUPPLIER)
        );
        List<SamovarFeed> samovarFeedsDb = List.of(
                createSamovarFeed("http://feed1.ru/", Map.of(2L, 113L), CampaignType.SUPPLIER)
        );
        List<SamovarFeed> result = samovarFeedImporterService.mergeState(samovarFeedsDb, samovarFeedsYt);
        assertEquals(1, result.size());
        assertEquals(samovarFeedsDb, result);

        samovarFeedsYt = List.of(
                createSamovarFeed("http://feed.ru/", Map.of(1L, 113L, 2L, 113L), CampaignType.SUPPLIER),
                createSamovarFeed("http://feed3.ru/", Map.of(3L, 113L), CampaignType.SUPPLIER));
        samovarFeedsDb = List.of(
                createSamovarFeed("http://feed-updated.ru/", Map.of(1L, 113L, 2L, 113L), CampaignType.SUPPLIER),
                createSamovarFeed("http://feed2.ru/", Map.of(4L, 113L), CampaignType.SUPPLIER)
        );
        result = samovarFeedImporterService.mergeState(samovarFeedsDb, samovarFeedsYt);
        assertEquals(4, result.size());
        Assertions.assertThat(result)
                .containsAll(
                        List.of(
                                SamovarFeed.builder()
                                        .of(samovarFeedsYt.get(0))
                                        .disable()
                                        .build(),
                                SamovarFeed.builder()
                                        .of(samovarFeedsYt.get(1))
                                        .disable()
                                        .build()
                        ))
                .containsAll(samovarFeedsDb);
    }

    @Test
    @DisplayName("Получение данных для записи в YT с проверкой измененных урлов - когда уже есть выключенные")
    void testSupplierSamovarFeedServiceMergeWithDisabled() {
        List<SamovarFeed> samovarFeedsYt = List.of(
                createSamovarFeed("http://feed.ru/", Map.of(1L, 113L, 2L, 113L), CampaignType.SUPPLIER),
                createSamovarFeed("http://feed-old.ru/", Map.of(3L, 113L, 4L, 113L), CampaignType.SUPPLIER, false,
                        Instant.now()),
                createSamovarFeed("http://feed-olds.ru/", Map.of(5L, 113L, 6L, 113L), CampaignType.SUPPLIER, false,
                        Instant.now().minus(Duration.ofHours(98))),
                createSamovarFeed("http://feed3.ru/", Map.of(7L, 113L), CampaignType.SUPPLIER));
        List<SamovarFeed> samovarFeedsDb = List.of(
                createSamovarFeed("http://feed-updated.ru/", Map.of(1L, 113L, 2L, 113L), CampaignType.SUPPLIER),
                createSamovarFeed("http://feed2.ru/", Map.of(7L, 113L), CampaignType.SUPPLIER)
        );
        List<SamovarFeed> result = samovarFeedImporterService.mergeState(samovarFeedsDb, samovarFeedsYt);
        assertEquals(5, result.size());
        Assertions.assertThat(result)
                .containsAll(
                        List.of(
                                SamovarFeed.builder()
                                        .of(samovarFeedsYt.get(0))
                                        .disable()
                                        .build(),

                                samovarFeedsYt.get(1),

                                SamovarFeed.builder()
                                        .of(samovarFeedsYt.get(3))
                                        .disable()
                                        .build()
                        ))
                .containsAll(samovarFeedsDb);
    }

    @Test
    @DisplayName(
            "Получение данных для записи в YT с проверкой измененных урлов, удаленных фидов и выключенными магазинами"
    )
    @DbUnitDataSet(before = "SamovarFeedImporterService.before.csv")
    void mergeState_shopAndSupplierWithDisabledOperation_listOfElements() {
        List<SamovarFeed> samovarFeedsYt = List.of(
                createSamovarFeed("http://feed2.ru/", Map.of(1L, 113L, 2L, 113L), CampaignType.SHOP),
                createSamovarFeed("http://feed-old.ru/", Map.of(3L, 113L, 4L, 113L), CampaignType.SHOP, false,
                        Instant.now()),
                createSamovarFeed("http://feed-olds.ru/", Map.of(5L, 113L), CampaignType.SHOP, false,
                        Instant.now().minus(Duration.ofHours(35))),
                createSamovarFeed("http://feed-updated.ru/", Map.of(5L, 113L), CampaignType.SHOP, false,
                        Instant.now()),
                createSamovarFeed("http://feed3.ru/", Map.of(1069L, 25L, 3L, 113L), CampaignType.SHOP));
        List<SamovarFeed> samovarFeedsDb = List.of(
                createSamovarFeed("http://feed-updated.ru/", Map.of(5545L, 22L), CampaignType.SHOP),
                createSamovarFeed("http://feed3.ru/", Map.of(1069L, 25L), CampaignType.SHOP)
        );
        List<SamovarFeed> result = samovarFeedImporterService.mergeState(samovarFeedsDb, samovarFeedsYt);
        assertEquals(4, result.size());
        Assertions.assertThat(result)
                .containsAll(
                        List.of(
                                SamovarFeed.builder()
                                        .of(samovarFeedsYt.get(0))
                                        .disable()
                                        .build(),
                                SamovarFeed.builder()
                                        .of(samovarFeedsYt.get(1))
                                        .disable()
                                        .build(),
                                samovarFeedsDb.get(0),
                                createSamovarFeed("http://feed3.ru/", Map.of(1069L, 25L), CampaignType.SHOP)
                        )
                );
    }

    @Test
    @DisplayName("Если урл не удалось канонизировать, то добавим в выгрузку as is. Но в конце бросим исключение")
    @DbUnitDataSet(before = "testSamovarFeedServiceInvalidUrl.before.csv")
    void testSamovarFeedServiceInvalidUrl() {
        List<SamovarFeed> result = invokeGetFeeds(true, "Can't canonize URL invalid_url for Samovar");

        Set<String> actual = result.stream()
                .map(SamovarFeed::getUrl)
                .collect(Collectors.toSet());

        Set<String> expected = Set.of(
                "invalid_url",
                "http://feed.ru/"
        );

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("У белых есть бизнес")
    @DbUnitDataSet(before = "testSamovarFeedServiceWhiteBusiness.before.csv")
    void testSamovarFeedServiceWhiteBusiness() {
        List<SamovarFeed> result = invokeGetFeeds();
        assertEquals(1, result.size());

        var feedsList = result.get(0).getContext().getFeedsList();
        assertEquals(1, feedsList.size());
        assertEquals(10111, feedsList.get(0).getBusinessId());
    }

    @Test
    @DisplayName("Выключенный, но живой магазин попадает в выгрузку")
    @DbUnitDataSet(before = "testSamovarAlivePartners.before.csv")
    void testSamovarAlivePartners() {
        checkSamovarAlive(Set.of(10L, 11L));
    }

    private void checkSamovarAlive(Set<Long> expectedFeedIds) {
        List<SamovarFeed> result = invokeGetFeeds();

        Set<Long> actualFeedIds = result.stream()
                .map(SamovarFeed::getContext)
                .map(SamovarContextOuterClass.SamovarContext::getFeedsList)
                .flatMap(Collection::stream)
                .map(FeedInfo::getFeedId)
                .collect(Collectors.toSet());

        assertEquals(expectedFeedIds, actualFeedIds);
    }

    @Test
    @DisplayName("Белый и синий с одним url попадут в один FeedInfo")
    @DbUnitDataSet(before = "testSamovarFeedServiceWhiteAndBlue.before.csv")
    void testSamovarFeedServiceWhiteAndBlue() {
        List<SamovarFeed> result = invokeGetFeeds();
        assertEquals(1, result.size());

        SamovarFeed samovarFeed = result.get(0);
        assertEquals("http://feed.ru/", samovarFeed.getUrl());

        var feedsList = samovarFeed.getContext().getFeedsList();
        assertEquals(4, feedsList.size());

        var actualFeeds = feedsList.stream()
                .map(e -> Triple.of(e.getShopId(), e.getFeedId(), e.getCampaignType()))
                .collect(Collectors.toSet());
        var expectedFeeds = Set.of(
                Triple.of(111L, 10L, CampaignType.SHOP.name()),
                Triple.of(111L, 11L, CampaignType.SHOP.name()),
                Triple.of(112L, 11L, CampaignType.SUPPLIER.name())
        );

        var actualFeedType = feedsList.stream()
                .map(e -> Pair.of(e.getShopId(), e.getFeedType()))
                .collect(Collectors.toSet());
        var expectedFeedType = Set.of(
                Pair.of(111L, FeedInfo.FeedType.STOCKS),
                Pair.of(111L, FeedInfo.FeedType.PRICES),
                Pair.of(111L, FeedInfo.FeedType.ASSORTMENT),
                Pair.of(112L, FeedInfo.FeedType.ASSORTMENT)
        );

        var actualParsingFields = feedsList.stream()
                        .map(feedInfo -> feedInfo.getParsingFieldsList().stream().collect(Collectors.toList()))
                                .collect(Collectors.toList());
        var expectedParsingFields = List.of(
                List.of("sku"), new ArrayList<String>(), List.of("sku"), new ArrayList<String>()
        );

        Assertions.assertThat(actualFeeds)
                .containsExactlyInAnyOrderElementsOf(expectedFeeds);
        Assertions.assertThat(actualFeedType)
                .containsExactlyInAnyOrderElementsOf(expectedFeedType);
        Assertions.assertThat(actualParsingFields)
                .containsExactlyInAnyOrderElementsOf(expectedParsingFields);
    }

    @Test
    @DisplayName("Дефолтные и аплоадные фиды не попадают в выгрузку")
    @DbUnitDataSet(before = "testSamovarFeedServiceDefaultAndUpload.before.csv")
    void testSamovarFeedServiceDefaultAndUpload() {
        List<SamovarFeed> result = invokeGetFeeds();
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Правильно строятся строки для выгрузки фидов в YT для самовара c одинаковым урлом")
    @DbUnitDataSet(before = "testSupplierSamovarFeedService.batch.before.csv")
    void testSupplierSamovarFeedServiceBatch() {
        List<SamovarFeed> result = invokeGetFeeds();
        assertEquals(1, result.size());
        Map<Long, FeedInfo> expected = buildExpectedBatchFeed().getContext()
                .getFeedsList()
                .stream()
                .collect(Collectors.toMap(FeedInfo::getShopId, Function.identity()));
        result.get(0)
                .getContext()
                .getFeedsList()
                .forEach(feedInfo -> {
                    FeedInfo expectedFeedInfo = expected.get(feedInfo.getShopId());
                    ProtoTestUtil.assertThat(feedInfo)
                            .ignoringFieldsMatchingRegexes(".*updatedA.*")
                            .isEqualTo(expectedFeedInfo);
                });
    }

    @Test
    @DisplayName("Правильно строятся строки для выгрузки фидов в YT для самовара c несколькими складами")
    @DbUnitDataSet(before = "testSupplierSamovarFeedService.multipleWarehouses.before.csv")
    void testSupplierSamovarFeedServiceWithMultipleWarehouses() {
        List<SamovarFeed> result = invokeGetFeeds();
        assertEquals(1, result.size());
        SamovarFeed expected = buildExpectedFeedWithMultipleWarehouses();
        Assertions.assertThat(result.get(0).getContext().getFeedsList().get(0).getWarehousesList())
                .containsExactlyInAnyOrderElementsOf(expected.getContext().getFeedsList().get(0).getWarehousesList());
    }

    @Test
    @DisplayName("Правильно строятся строки для выгрузки фидов в YT для самовара c дефолтным таймаутом и периодом")
    @DbUnitDataSet(before = "testSupplierSamovarFeedService.timeout.default.before.csv")
    void testSupplierSamovarFeedServiceWithDefaultTimeoutAndPeriod() {
        List<SamovarFeed> result = invokeGetFeeds();
        assertEquals(1, result.size());
        assertEquals(SamovarFeedMapper.DEFAULT_TIMEOUT_SECONDS, result.get(0).getTimeoutSeconds());
        assertEquals(20, result.get(0).getPeriodMinutes());
    }

    @Test
    @DisplayName("Правильно строятся строки для выгрузки фидов в YT для самовара c таймаутом и периодом")
    @DbUnitDataSet(before = "testSupplierSamovarFeedService.timeout.before.csv")
    void testSupplierSamovarFeedServiceWithTimeoutAndPeriod() {
        List<SamovarFeed> result = invokeGetFeeds();
        assertEquals(1, result.size());
        assertEquals(20, result.get(0).getTimeoutSeconds());
        assertEquals(40, result.get(0).getPeriodMinutes());
    }

    @Test
    @DisplayName("Плохие символы в урле экранируются перед экспортом")
    @DbUnitDataSet(before = "testSamovarFeedService.escaping.before.csv")
    void testEscapingBeforeExport() {
        List<SamovarFeed> result = invokeGetFeeds();
        assertEquals(1, result.size());
        assertEquals("https://severodvinsk.takaro.ru/index.php?route=feed/yandex_market&city_code=%7BD29%7D", result.get(0).getUrl());
    }

    @Test
    @DisplayName("Выбор наибольшего таймаута для выгрузки фидов в YT для самовара c несколькими таймаутами и периодами")
    @DbUnitDataSet(before = "testSupplierSamovarFeedService.timeout.multiple.before.csv")
    void testSupplierSamovarFeedServiceWithMultipleTimeoutAndPeriod() {
        List<SamovarFeed> result = invokeGetFeeds();
        assertEquals(1, result.size());
        assertEquals(500, result.get(0).getTimeoutSeconds());
        assertEquals(5, result.get(0).getPeriodMinutes());
    }

    @Test
    @DisplayName("В выгрузку попадают все Партнеры с однаковым урлом фида")
    @DbUnitDataSet(before = "testShopAndDirectFeeds.before.csv")
    void testShopAndDirectFeeds() {
        List<SamovarFeed> result = invokeGetFeeds();
        assertEquals(1, result.size(), "Оба фида должны выгружаться одной строкой");
        SamovarFeed feed = result.get(0);
        assertEquals(2, feed.getContext().getFeedsList().size());
    }

    @Test
    @DisplayName("Правильно строятся строки для YT в случае отсутствия фидов")
    void testSupplierSamovarFeedServiceWithoutFeeds() {
        List<SamovarFeed> result = invokeGetFeeds();
        assertTrue(result.isEmpty());
    }

    private List<SamovarFeed> invokeGetFeeds() {
        return invokeGetFeeds(false, null);
    }

    private List<SamovarFeed> invokeGetFeeds(boolean expectThrows, String expectedMessage) {
        ExceptionCollector exceptionCollector = new ExceptionCollector();
        List<SamovarFeed> result = samovarFeedImporterService.getFeeds(exceptionCollector);

        if (expectThrows) {
            RuntimeException runtimeException = assertThrows(RuntimeException.class, exceptionCollector::close);
            assertEquals(expectedMessage, runtimeException.getMessage());
        } else {
            assertDoesNotThrow(exceptionCollector::close);
        }

        return result;
    }

    private SamovarFeed buildExpectedFeedWithMultipleWarehouses() {
        Timestamp updatedAt = Timestamp.newBuilder().setSeconds(1575948458).setNanos(172803000).build();
        List<FeedInfo> feedInfos1 = new ArrayList<>();

        feedInfos1.add(FeedInfo.newBuilder()
                .setUrl("http://feed.ru/")
                .setCampaignType(CampaignType.SUPPLIER.name())
                .setFeedId(10L)
                .addWarehouses(FeedInfo.WarehouseInfo.newBuilder()
                        .setWarehouseId(1000)
                        .setFeedId(1)
                        .setType("fulfillment")
                        .build())
                .addWarehouses(FeedInfo.WarehouseInfo.newBuilder()
                        .setWarehouseId(2000)
                        .setFeedId(2)
                        .setType("fulfillment")
                        .build())
                .setUpdatedAt(
                        updatedAt)
                .setShopId(111L)
                .build());

        return SamovarFeed.builder()
                .setUrl("http://feed.ru/")
                .setCredentials(ResourceAccessCredentials.of("testLogin", "testPwd"))
                .setPeriodMinutes(20)
                .setTimeoutSeconds(SamovarFeedMapper.DEFAULT_TIMEOUT_SECONDS)
                .setContext(feedInfos1, environmentService.getCurrentEnvironmentType())
                .build();
    }

    private SamovarFeed buildExpectedBatchFeed() {
        Timestamp updatedAt = Timestamp.newBuilder().setSeconds(1575948458).setNanos(172803000).build();
        List<FeedInfo> feedInfos1 = new ArrayList<>();

        feedInfos1.add(FeedInfo.newBuilder()
                .setUrl("http://feed.ru/")
                .setCampaignType(CampaignType.SUPPLIER.name())
                .setFeedId(10L)
                .addWarehouses(FeedInfo.WarehouseInfo.newBuilder()
                        .setWarehouseId(1000)
                        .setFeedId(1)
                        .setType("fulfillment")
                        .setExternalId("111_ex")
                        .build())
                .setUpdatedAt(
                        updatedAt)
                .setShopId(111L)
                        .setShopsDatParameters(UpdateTask.ShopsDatParameters.newBuilder()
                                .setIsSiteMarket(true)
                                .setIsDiscountsEnabled(true)
                                .setColor(DataCampOfferMeta.MarketColor.BLUE)
                                .setUrl("http://feed.ru/")
                                .setBlueStatus("REAL")
                                .setSupplierType("3")
                                .setIsMock(false)
                                .setLocalRegionTzOffset(10800)
                                .setCpa(UpdateTask.ProgramStatus.PROGRAM_STATUS_REAL)
                                .build())
                .setFeedType(FeedInfo.FeedType.ASSORTMENT)
                .build());

        feedInfos1.add(FeedInfo.newBuilder()
                .setUrl("http://feed.ru")
                .setCampaignType(CampaignType.SUPPLIER.name())
                .setFeedId(11L)
                .setUpdatedAt(
                        updatedAt)
                .setShopId(112L)
                .addWarehouses(FeedInfo.WarehouseInfo.newBuilder()
                        .setWarehouseId(1000)
                        .setFeedId(11L)
                        .setType("fulfillment")
                        .setExternalId("111_ex")
                        .build())
                .setShopsDatParameters(UpdateTask.ShopsDatParameters.newBuilder()
                        .setIsSiteMarket(true)
                        .setIsDiscountsEnabled(true)
                        .setColor(DataCampOfferMeta.MarketColor.BLUE)
                        .setUrl("http://feed.ru")
                        .setBlueStatus("REAL")
                        .setSupplierType("3")
                        .setIsMock(false)
                        .setLocalRegionTzOffset(10800)
                        .setCpa(UpdateTask.ProgramStatus.PROGRAM_STATUS_REAL)
                        .build())
                .setFeedType(FeedInfo.FeedType.ASSORTMENT)
                .build());

        return SamovarFeed.builder()
                .setUrl("http://feed.ru/")
                .setCredentials(ResourceAccessCredentials.of("testLogin", "testPwd"))
                .setPeriodMinutes(20)
                .setTimeoutSeconds(SamovarFeedMapper.DEFAULT_TIMEOUT_SECONDS)
                .setContext(feedInfos1, environmentService.getCurrentEnvironmentType())
                .build();
    }

    private SamovarFeed createSamovarFeed(String url, Map<Long, Long> feedShopMap, CampaignType campaignType) {
        return createSamovarFeed(url, feedShopMap, campaignType, true, null);
    }

    private SamovarFeed createSamovarFeed(String url, Map<Long, Long> feedShopMap, CampaignType campaignType,
                                          boolean enabled, Instant disabledTimestamp) {
        List<FeedInfo> feedInfos = feedShopMap.entrySet()
                .stream()
                .map(entry -> createFeedInfo(url, entry.getKey(), entry.getValue(), campaignType))
                .collect(Collectors.toList());

        return SamovarFeed.builder()
                .setUrl(url)
                .setPeriodMinutes(20)
                .setContext(feedInfos, environmentService.getCurrentEnvironmentType())
                .setTimeoutSeconds(SamovarFeedMapper.DEFAULT_TIMEOUT_SECONDS)
                .setEnabled(enabled)
                .setDisabledTimestamp(disabledTimestamp)
                .build();
    }

    private FeedInfo createFeedInfo(String url, Long feedId, long shopId,
                                    CampaignType campaignType) {
        Timestamp updatedAt = Timestamp.newBuilder()
                .setSeconds(1575948458)
                .setNanos(172803000)
                .build();
        return FeedInfo.newBuilder()
                .setUrl(url)
                .setCampaignType(campaignType.name())
                .setFeedId(feedId)
                .setUpdatedAt(updatedAt)
                .setShopId(shopId)
                .addWarehouses(FeedInfo.WarehouseInfo.newBuilder()
                        .setWarehouseId(1000)
                        .setFeedId(feedId)
                        .build())
                .build();
    }
}
