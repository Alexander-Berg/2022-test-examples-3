package ru.yandex.market.indexer.problem;

import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.feed.FeedService;
import ru.yandex.market.core.feed.model.FeedProcessingType;
import ru.yandex.market.core.feed.model.FeedSiteType;
import ru.yandex.market.core.indexer.model.FeedStatus;
import ru.yandex.market.core.indexer.model.IndexerType;
import ru.yandex.market.core.indexer.model.ReturnCode;
import ru.yandex.market.indexer.problem.strategy.impl.CommonFeedsStrategy;

/**
 * @author fbokovikov
 */
@ExtendWith(MockitoExtension.class)
class ProblemFeedsProcessorTest extends FunctionalTest {

    @Mock
    private CommonFeedsStrategy commonFeedsStrategy;

    @Autowired
    private FeedService feedService;

    private ProblemFeedsProcessor problemFeedsProcessor;

    @BeforeEach
    void init() {
        problemFeedsProcessor = new ProblemFeedsProcessor(
                feedService,
                ImmutableMap.of(
                        FeedSiteType.MARKET, commonFeedsStrategy
                ));
    }

    @Test
    @DbUnitDataSet(before = "testSiteTypesDivision.csv")
    void testNewSiteTypesDivision() {
        problemFeedsProcessor.processProblemFeedsInLastGeneration(prepareFeedStatuses());

        Mockito.verify(commonFeedsStrategy)
                .processProblemFeeds(
                        ArgumentMatchers.argThat(new FeedIndexationInfoCollectionMatcher(Arrays.asList(2L, 3L, 5L, 8L)))
                );
    }

    private Map<Long, FeedStatus> prepareFeedStatuses() {
        return Map.of(2L, new FeedStatus.Builder()
                        .setFeedId(2L)
                        .setLastGenerationId(0L)
                        .setSiteType(FeedSiteType.MARKET)
                        .setIndexerType(IndexerType.MAIN)
                        .setInIndex(true)
                        .setFatalInLastGeneration(false)
                        .setLastFullGenReturnCode(ReturnCode.OK)
                        .setLastNotFatalFullGenerationId(1L)
                        .setLastFullGenReturnCodeCount(1)
                        .setFeedProcessingType(FeedProcessingType.PULL)
                        .build(),
                3L, new FeedStatus.Builder()
                        .setFeedId(3L)
                        .setLastGenerationId(0L)
                        .setSiteType(FeedSiteType.MARKET)
                        .setIndexerType(IndexerType.MAIN)
                        .setInIndex(true)
                        .setFatalInLastGeneration(false)
                        .setLastFullGenReturnCode(ReturnCode.OK)
                        .setLastNotFatalFullGenerationId(1L)
                        .setLastFullGenReturnCodeCount(1)
                        .setFeedProcessingType(FeedProcessingType.PULL)
                        .build(),
                5L, new FeedStatus.Builder()
                        .setFeedId(5L)
                        .setLastGenerationId(0L)
                        .setSiteType(FeedSiteType.MARKET)
                        .setIndexerType(IndexerType.MAIN)
                        .setInIndex(true)
                        .setFatalInLastGeneration(false)
                        .setLastFullGenReturnCode(ReturnCode.OK)
                        .setLastNotFatalFullGenerationId(1L)
                        .setLastFullGenReturnCodeCount(1)
                        .setFeedProcessingType(FeedProcessingType.PULL)
                        .build(),
                8L, new FeedStatus.Builder()
                        .setFeedId(8L)
                        .setLastGenerationId(0L)
                        .setSiteType(FeedSiteType.MARKET)
                        .setIndexerType(IndexerType.MAIN)
                        .setInIndex(true)
                        .setFatalInLastGeneration(false)
                        .setLastFullGenReturnCode(ReturnCode.OK)
                        .setLastNotFatalFullGenerationId(1L)
                        .setLastFullGenReturnCodeCount(1)
                        .setFeedProcessingType(FeedProcessingType.PULL)
                        .build());
    }
}
