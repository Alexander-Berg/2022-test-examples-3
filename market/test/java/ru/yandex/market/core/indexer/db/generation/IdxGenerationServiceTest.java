package ru.yandex.market.core.indexer.db.generation;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.common.excel.MarketTemplate;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.util.bulk.BulkProcessor;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feed.model.FeedFileType;
import ru.yandex.market.core.feed.model.FeedProcessingType;
import ru.yandex.market.core.feed.model.FeedSiteType;
import ru.yandex.market.core.indexer.model.ColoredFeedLog;
import ru.yandex.market.core.indexer.model.FeedSession;
import ru.yandex.market.core.indexer.model.FeedStatus;
import ru.yandex.market.core.indexer.model.GenerationMeta;
import ru.yandex.market.core.indexer.model.GenerationType;
import ru.yandex.market.core.indexer.model.IndexedStatus;
import ru.yandex.market.core.indexer.model.IndexerType;
import ru.yandex.market.core.indexer.model.ReturnCode;
import ru.yandex.market.core.indexer.parser.FeedLogCodeStats;
import ru.yandex.market.core.indexer.parser.FeedLogLine;
import ru.yandex.market.core.indexer.parser.ParseLogParsed;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.util.DateTimes;

import static org.assertj.core.api.Assertions.assertThat;

@DbUnitDataSet(before = "data/IdxGenerationServiceTest.before.csv")
class IdxGenerationServiceTest extends FunctionalTest {

    private static final String URL_IN_ARCHIVE = "url";
    @Autowired
    private IdxGenerationService idxGenerationService;
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    private ParamService paramService;

    @Test
    @DbUnitDataSet(before = "data/getAllSiteFeedsStatusTest.before.csv")
    void getAllSiteFeedsStatusesTest() {
        Map<Long, FeedStatus> siteFeedsStatuses =
                idxGenerationService.getAllSiteFeedsStatuses(FeedSiteType.YELLOW_MARKET, IndexerType.MAIN);
        assertThat(siteFeedsStatuses).hasSize(3);
        assertThat(siteFeedsStatuses.values()).containsExactlyInAnyOrder(
                new FeedStatus.Builder()
                        .setFeedId(11L)
                        .setLastGenerationId(34543L)
                        .setLastNotFatalGenerationId(34543L)
                        .setLastNotFatalFullGenerationId(34543L)
                        .setLastFullGenReturnCode(ReturnCode.OK)
                        .setLastFullGenReturnCodeCount(1)
                        .setFullGenerationDownloadTime(Instant.ofEpochSecond(1554042702L))
                        .setFullGenerationValidOffersCount(34L)
                        .setLastSuccessGenerationId(34543L)
                        .setLastSuccessFullGenerationId(34543L)
                        .setSiteType(FeedSiteType.YELLOW_MARKET)
                        .setIndexerType(IndexerType.MAIN)
                        .setFatalInLastGeneration(false)
                        .setInIndex(true)
                        .setFullGenerationTotalOffersCount(100L)
                        .setFullGenerationTotalErrorsCount(20L)
                        .setFullGenerationTotalWarningsCount(10L)
                        .setFullGenerationCpcRealOffersCount(70L)
                        .setFullGenerationCpaRealOffersCount(30L)
                        .setLastFullGenFeedErrorCode("560")
                        .setLastFullGenFeedErrorDetails(Map.of("arg1", "val1"))
                        .setLastFullGenFeedUrl("http://archive.url/feed")
                        .setBusinessId(98765L)
                        .build(),
                new FeedStatus.Builder()
                        .setFeedId(12L)
                        .setLastGenerationId(34545L)
                        .setLastNotFatalGenerationId(34544L)
                        .setLastNotFatalFullGenerationId(34544L)
                        .setLastFullGenReturnCode(ReturnCode.ERROR)
                        .setLastFullGenReturnCodeCount(2)
                        .setFullGenerationDownloadTime(Instant.ofEpochSecond(1554107862L))
                        .setFullGenerationValidOffersCount(0L)
                        .setPrev1FullGenReturnCode(ReturnCode.ERROR)
                        .setSiteType(FeedSiteType.YELLOW_MARKET)
                        .setIndexerType(IndexerType.MAIN)
                        .setFatalInLastGeneration(true)
                        .setInIndex(true)
                        .build(),
                new FeedStatus.Builder()
                        .setFeedId(16L)
                        .setLastGenerationId(37787L)
                        .setSiteType(FeedSiteType.YELLOW_MARKET)
                        .setIndexerType(IndexerType.MAIN)
                        .setFatalInLastGeneration(true)
                        .setInIndex(true)
                        .build()
        );
    }

    @Test
    @DbUnitDataSet(before = "data/updateFeedStatusTest.before.csv", after = "data/updateFeedStatusTest.after.csv")
    void updateFeedStatusTest() {
        BulkProcessor<FeedStatus> merger = idxGenerationService.getFeedStatusMerger();
        GenerationMeta.Builder yellowMetaBuilder = new GenerationMeta.Builder()
                .setFeedType(FeedSiteType.YELLOW_MARKET)
                .setGenerationType(GenerationType.FULL)
                .setIndexerType(IndexerType.MAIN)
                .setReleaseDate(Instant.now())
                .setName("name")
                .setKey("key")
                .setMitype("mitype");

        // добавляется новый фид id=1 в плейншифте, статус ОК
        FeedStatus feedStatus = FeedStatus.createNew(
                yellowMetaBuilder
                        .setName("name")
                        .setId(123L)
                        .setIndexerType(IndexerType.PLANESHIFT)
                        .build(),
                new ColoredFeedLog.Builder()
                        .setMetaId(123L)
                        .setFeedId(1L)
                        .setInIndex(true)
                        .setValidOffersCount(115L)
                        .setDownloadDate(Instant.now())
                        .setReleaseDate(Instant.now())
                        .setReturnCode(ReturnCode.OK)
                        .setGenerationType(GenerationType.FULL)
                        .setIndexerType(IndexerType.PLANESHIFT)
                        .setBusinessId(98765L)
                        .build(),
                null
        );
        merger.processAndFlush(Collections.singletonList(feedStatus));

        // добавляется новый фид id=1 в основном индексе, статус ОК
        var feedSessionStartTime = Instant.now();
        var feedSessionId = new FeedSession.FeedSessionId(
                1L,
                IndexerType.MAIN,
                "20180101_0101",
                feedSessionStartTime,
                "stratocaster"
        );
        feedStatus = FeedStatus.createNew(
                yellowMetaBuilder
                        .setId(125L)
                        .setIndexerType(IndexerType.MAIN)
                        .build(),
                new ColoredFeedLog.Builder()
                        .setMetaId(125L)
                        .setFeedId(1L)
                        .setInIndex(true)
                        .setValidOffersCount(115L)
                        .setDownloadDate(Instant.now())
                        .setReleaseDate(Instant.now())
                        .setReturnCode(ReturnCode.OK)
                        .setGenerationType(GenerationType.FULL)
                        .setIndexerType(IndexerType.MAIN)
                        .setTotalOffersCount(100L)
                        .setCpcRealOffersCount(70L)
                        .setCpaRealOffersCount(30L)
                        .setBusinessId(987654L)
                        .build(),
                new FeedSession.Builder()
                        .setId(feedSessionId)
                        .setParseLog("...")
                        .setParseLogParsed(
                                new ParseLogParsed(
                                        Arrays.asList(
                                                createFeedCode("444", ""),
                                                createFeedCode("404", "")
                                        ),
                                        Collections.emptyList()
                                )
                        )
                        .setDownloadTime(Instant.now())
                        .setParseReturnCode(ReturnCode.FATAL)
                        .setUrlInArchive(URL_IN_ARCHIVE)
                        .build()
        );
        merger.processAndFlush(Collections.singletonList(feedStatus));

        // добавляется новый фид id=2 в основном индексе, статус FATAL
        feedStatus = FeedStatus.createNew(
                yellowMetaBuilder
                        .setId(123L)
                        .setIndexerType(IndexerType.PLANESHIFT)
                        .build(),
                new ColoredFeedLog.Builder()
                        .setMetaId(123L)
                        .setFeedId(2L)
                        .setInIndex(true)
                        .setReleaseDate(Instant.now())
                        .setReturnCode(ReturnCode.FATAL)
                        .setIndexerType(IndexerType.MAIN)
                        .setGenerationType(GenerationType.FULL)
                        .build(),
                null
        );
        merger.processAndFlush(Collections.singletonList(feedStatus));

        Map<Long, FeedStatus> yellowMainFeedsStatuses =
                idxGenerationService.getAllSiteFeedsStatuses(FeedSiteType.YELLOW_MARKET, IndexerType.MAIN);

        // обновляется фид id=3 в основном индексе, был ОК, стал ERROR
        feedStatus = yellowMainFeedsStatuses.get(3L).update(
                new ColoredFeedLog.Builder()
                        .setMetaId(127L)
                        .setFeedId(3L)
                        .setInIndex(true)
                        .setValidOffersCount(23L)
                        .setDownloadDate(Instant.now())
                        .setReleaseDate(Instant.now())
                        .setReturnCode(ReturnCode.ERROR)
                        .setGenerationType(GenerationType.FULL)
                        .setIndexerType(IndexerType.MAIN)
                        .build(),
                100L,
                new FeedSession.Builder()
                        .setId(feedSessionId)
                        .setParseLog("...")
                        .setParseLogParsed(
                                new ParseLogParsed(
                                        Arrays.asList(
                                                createFeedCode("444", ""),
                                                FeedLogCodeStats.builder()
                                                        .setCode("560")
                                                        .setSubcode("")
                                                        .incrementErrorsCount()
                                                        .addExample(new FeedLogLine("560", null, null, null, "", null, null, Map.of("arg1", "val1")))
                                                        .build()
                                        ),
                                        Collections.emptyList()
                                )
                        )
                        .setDownloadTime(Instant.now())
                        .setParseReturnCode(ReturnCode.FATAL)
                        .setUrlInArchive(URL_IN_ARCHIVE)
                        .build()
        );
        merger.processAndFlush(Collections.singletonList(feedStatus));

        // обновляется фид id=4 в основном индексе, был WARNING, стал FATAL
        feedStatus = yellowMainFeedsStatuses.get(4L).update(
                new ColoredFeedLog.Builder()
                        .setMetaId(129L)
                        .setFeedId(4L)
                        .setInIndex(true)
                        .setDownloadDate(Instant.now())
                        .setReleaseDate(Instant.now())
                        .setReturnCode(ReturnCode.FATAL)
                        .setGenerationType(GenerationType.FULL)
                        .setIndexerType(IndexerType.MAIN)
                        .build(),
                100L,
                null
        );
        merger.processAndFlush(Collections.singletonList(feedStatus));

        // обновляется фид id=5 в основном индексе, был ERROR, выпал из индекса
        feedStatus = yellowMainFeedsStatuses.get(5L).update(
                new ColoredFeedLog.Builder()
                        .setMetaId(129L)
                        .setFeedId(5L)
                        .setInIndex(false)
                        .setReleaseDate(Instant.now())
                        .setGenerationType(GenerationType.FULL)
                        .setIndexerType(IndexerType.MAIN)
                        .setBusinessId(987654321L)
                        .build(),
                null,
                null
        );
        merger.processAndFlush(Collections.singletonList(feedStatus));

        // обновляется фид id=6 в основном индексе, был FATAL, стал WARNING
        feedStatus = yellowMainFeedsStatuses.get(6L).update(
                new ColoredFeedLog.Builder()
                        .setMetaId(130L)
                        .setFeedId(6L)
                        .setInIndex(true)
                        .setValidOffersCount(56L)
                        .setDownloadDate(Instant.now())
                        .setReleaseDate(Instant.now())
                        .setReturnCode(ReturnCode.WARNING)
                        .setGenerationType(GenerationType.FULL)
                        .setIndexerType(IndexerType.MAIN)
                        .setBusinessId(9876543210L)
                        .build(),
                100L,
                null
        );
        merger.processAndFlush(Collections.singletonList(feedStatus));

        // обновляется фид id=7 в основном индексе, был не в индексе, стал ОК
        assertThat(yellowMainFeedsStatuses.get(7L).isInIndex()).isFalse();
        feedStatus = yellowMainFeedsStatuses.get(7L).update(
                new ColoredFeedLog.Builder()
                        .setMetaId(130L)
                        .setFeedId(7L)
                        .setInIndex(true)
                        .setValidOffersCount(89L)
                        .setDownloadDate(Instant.now())
                        .setReleaseDate(Instant.now())
                        .setReturnCode(ReturnCode.OK)
                        .setGenerationType(GenerationType.FULL)
                        .setIndexerType(IndexerType.MAIN)
                        .build(),
                100L,
                null
        );
        merger.processAndFlush(Collections.singletonList(feedStatus));
    }

    /**
     * Проверяем, что если магазин исправил ошибку фида в последнем поколении, то мы сбросим статус.
     */
    @Test
    @DbUnitDataSet(
            before = "data/cleanUpLastFeedError.before.csv",
            after = "data/cleanUpLastFeedError.after.csv"
    )
    void cleanUpLastFeedError() {
        BulkProcessor<FeedStatus> merger = idxGenerationService.getFeedStatusMerger();
        FeedStatus feedStatus = idxGenerationService.getFeedStatuses(List.of(3L), IndexerType.MAIN).get(0);

        var feedSessionStartTime = Instant.now();
        var feedSessionId = new FeedSession.FeedSessionId(
                1L,
                IndexerType.MAIN,
                "20180101_0101",
                feedSessionStartTime,
                "stratocaster"
        );
        FeedStatus newFeedStatus = feedStatus.update(
                new ColoredFeedLog.Builder()
                        .setMetaId(127L)
                        .setFeedId(3L)
                        .setInIndex(true)
                        .setValidOffersCount(23L)
                        .setDownloadDate(Instant.now())
                        .setReleaseDate(Instant.now())
                        .setReturnCode(ReturnCode.WARNING)
                        .setGenerationType(GenerationType.FULL)
                        .setIndexerType(IndexerType.MAIN)
                        .build(),
                100L,
                new FeedSession.Builder()
                        .setId(feedSessionId)
                        .setParseLog("...")
                        .setParseLogParsed(
                                new ParseLogParsed(
                                        List.of(
                                                createFeedCode("444", "")
                                        ),
                                        Collections.emptyList()
                                )
                        )
                        .setDownloadTime(Instant.now())
                        .setParseReturnCode(ReturnCode.WARNING)
                        .setUrlInArchive(URL_IN_ARCHIVE)
                        .build()
        );
        merger.processAndFlush(Collections.singletonList(newFeedStatus));
    }

    @Test
    @DbUnitDataSet(
            before = "data/cleanUpFeedsStatusesTest.before.csv",
            after = "data/cleanUpFeedsStatusesTest.after.csv"
    )
    void cleanUpFeedsStatusesTest() {
        idxGenerationService.cleanUpFeedsStatuses();
    }

    @Test
    @DbUnitDataSet(
            before = "data/updateDatasourceIsInIndexParamTest.before.csv",
            after = "data/updateDatasourceIsInIndexParamTest.after.csv"
    )
    void updateDatasourceIsInIndexParamTest() {
        idxGenerationService.updateDatasourceIsInIndexParam(IndexerType.MAIN, EnumSet.allOf(FeedSiteType.class));
    }

    @Test
    @DisplayName("Получить список фидов из истории")
    @DbUnitDataSet(before = "data/getFeedsInHistoryTest.before.csv")
    void getFeedsInHistoryTest() {
        Set<Long> actual711 = idxGenerationService.getFeedsInHistory(711);
        assertThat(actual711).containsExactly(1010L, 1012L);
    }

    @Test
    @DbUnitDataSet(after = "data/updateFeedStatusesTest.after.csv")
    void updateFeedStatusesTest() {
        idxGenerationService.getFeedStatusMerger().processAndFlush(getMainFeedStatuses());
        idxGenerationService.getFeedStatusMerger().processAndFlush(getPlaneshiftFeedStatuses());
    }

    @Test
    @DbUnitDataSet(before = "data/getFeedStatusesMainTest.before.csv")
    void getFeedStatusesMainTest() {
        List<FeedStatus> expected = getMainFeedStatuses();
        List<FeedStatus> actual = idxGenerationService.getFeedStatuses(
                expected.stream().map(FeedStatus::getFeedId).collect(Collectors.toList()),
                IndexerType.MAIN);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DbUnitDataSet(before = "data/getFeedStatusesPlaneshiftTest.before.csv")
    void getFeedStatusesPlaneshiftTest() {
        List<FeedStatus> expected = getPlaneshiftFeedStatuses();
        List<FeedStatus> actual = idxGenerationService.getFeedStatuses(
                expected.stream().map(FeedStatus::getFeedId).collect(Collectors.toList()),
                IndexerType.PLANESHIFT);
        assertThat(actual).isEqualTo(expected);
    }

    private List<FeedStatus> getMainFeedStatuses() {
        return List.of(
                new FeedStatus.Builder()
                        .setFeedId(0L)
                        .setLastGenerationId(0L)
                        .setLastNotFatalGenerationId(0L)
                        .setLastNotFatalFullGenerationId(0L)
                        .setLastFullGenReturnCode(ReturnCode.OK)
                        .setLastFullGenReturnCodeCount(0)
                        .setFullGenerationDownloadTime(Instant.parse("2000-01-01T00:00:00Z"))
                        .setFullGenerationValidOffersCount(0L)
                        .setPrev1FullGenReturnCode(ReturnCode.OK)
                        .setPrev2FullGenReturnCode(ReturnCode.OK)
                        .setPrev2FullGenReturnCode(ReturnCode.OK)
                        .setLastSuccessGenerationId(0L)
                        .setLastSuccessFullGenerationId(0L)
                        .setSiteType(FeedSiteType.MARKET)
                        .setIndexerType(IndexerType.MAIN)
                        .setFatalInLastGeneration(true)
                        .setInIndex(false)
                        .build(),
                new FeedStatus.Builder()
                        .setFeedId(2L)
                        .setLastGenerationId(2L)
                        .setLastNotFatalGenerationId(2L)
                        .setLastNotFatalFullGenerationId(2L)
                        .setLastFullGenReturnCode(ReturnCode.ERROR)
                        .setLastFullGenReturnCodeCount(2)
                        .setFullGenerationDownloadTime(Instant.parse("2002-01-01T00:00:00Z"))
                        .setFullGenerationValidOffersCount(2L)
                        .setPrev1FullGenReturnCode(ReturnCode.ERROR)
                        .setPrev2FullGenReturnCode(ReturnCode.ERROR)
                        .setPrev2FullGenReturnCode(ReturnCode.ERROR)
                        .setLastSuccessGenerationId(2L)
                        .setLastSuccessFullGenerationId(2L)
                        .setSiteType(FeedSiteType.BLUE_MARKET)
                        .setIndexerType(IndexerType.MAIN)
                        .setFatalInLastGeneration(true)
                        .setInIndex(false)
                        .build()
        );
    }

    private List<FeedStatus> getPlaneshiftFeedStatuses() {
        return List.of(
                new FeedStatus.Builder()
                        .setFeedId(3L)
                        .setLastGenerationId(3L)
                        .setLastNotFatalGenerationId(3L)
                        .setLastNotFatalFullGenerationId(3L)
                        .setLastFullGenReturnCode(ReturnCode.FATAL)
                        .setLastFullGenReturnCodeCount(3)
                        .setFullGenerationDownloadTime(Instant.parse("2003-01-01T00:00:00Z"))
                        .setFullGenerationValidOffersCount(3L)
                        .setPrev1FullGenReturnCode(ReturnCode.FATAL)
                        .setPrev2FullGenReturnCode(ReturnCode.FATAL)
                        .setPrev2FullGenReturnCode(ReturnCode.FATAL)
                        .setLastSuccessGenerationId(3L)
                        .setLastSuccessFullGenerationId(3L)
                        .setSiteType(FeedSiteType.YELLOW_MARKET)
                        .setIndexerType(IndexerType.PLANESHIFT)
                        .setFatalInLastGeneration(false)
                        .setInIndex(true)
                        .build()
        );
    }

    ColoredFeedLog getFeedLog() {
        return new ColoredFeedLog.Builder()
                .setMetaId(1613L)
                .setFeedId(12345L)
                .setInIndex(true)
                .setIndexerType(IndexerType.MAIN)
                .setGenerationType(GenerationType.FULL)
                .setShopId(54321L)
                .setStartDate(Instant.ofEpochSecond(1234567))
                .setDownloadDate(Instant.ofEpochSecond(2345678))
                .setReleaseDate(Instant.ofEpochSecond(3456789))
                .setReturnCode(ReturnCode.OK)
                .setDownloadReturnCode(ReturnCode.OK)
                .setParseReturnCode(ReturnCode.OK)
                .setIndexedStatus(IndexedStatus.OK)
                .setYmlDate("27.10.2019")
                .setFeedFileType(FeedFileType.CSV)
                .setMarketTemplate(MarketTemplate.ALCOHOL)
                .setDownloadStatus("200 OK")
                .setTotalOffersCount(99L)
                .setValidOffersCount(88L)
                .setWarnOffersCount(87L)
                .setErrorOffersCount(86L)
                .setCpcRealOffersCount(50L)
                .setCpaRealOffersCount(49L)
                .setDiscountOffersCount(77L)
                .setHonestDiscountOffersCount(66L)
                .setWhitePromosOffersCount(55L)
                .setHonestWhitePromosOffersCount(44L)
                .setMatchedOffersCount(33L)
                .setMatchedClusterOfferCount(22L)
                .setTotalPromosCount(11L)
                .setValidPromosCount(111L)
                .setPrimaryOffersWithPromoCount(222L)
                .setCluster("stratocater")
                .setLastSessionName("20192710")
                .setCachedSessionName("20192810")
                .setFeedProcessingType(FeedProcessingType.PULL)
                .setLastSessionStartTime(DateTimes.toInstant(2020, 1, 1, 10, 20, 30))
                .setCachedSessionStartTime(DateTimes.toInstant(2020, 2, 1, 10, 20, 30))
                .build();
    }

    @Test
    @DbUnitDataSet(after = "data/insertFeedLogTest.after.csv")
    void insertFeedLogTest() {
        ColoredFeedLog feedLog = getFeedLog();
        idxGenerationService.getFeedLogInserter().processAndFlush(Collections.singletonList(feedLog));
    }

    @Test
    @DbUnitDataSet(before = "data/getFeedLogTest.before.csv")
    void getFeedLogTest() {
        ColoredFeedLog expected = getFeedLog();
        Optional<ColoredFeedLog> actual = idxGenerationService.getFeedLog(expected.getMetaId(),
                expected.getFeedId());
        assertThat(actual).contains(expected);
        assertThat(idxGenerationService.getFeedLog(1L, 1L)).isNotPresent();
    }

    private static FeedLogCodeStats createFeedCode(String code, String subcode) {
        return FeedLogCodeStats.builder()
                .setCode(code)
                .setSubcode(subcode)
                .build();
    }
}
