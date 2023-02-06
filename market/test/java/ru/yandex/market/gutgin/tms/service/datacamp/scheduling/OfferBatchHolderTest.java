package ru.yandex.market.gutgin.tms.service.datacamp.scheduling;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.gutgin.tms.config.OfferSchedulingConfig;
import ru.yandex.market.gutgin.tms.service.datacamp.scheduling.batching.OfferBatchHolder;
import ru.yandex.market.gutgin.tms.service.datacamp.scheduling.batching.OfferGroupingKey;
import ru.yandex.market.gutgin.tms.service.datacamp.scheduling.batching.OffersGroupingWithStrategy;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.gutgin.tms.service.datacamp.scheduling.OfferBatchHelper.getNewDatacampStrategyOffer;

@ContextConfiguration(
        classes = {OfferSchedulingConfig.class}
)
@RunWith(SpringJUnit4ClassRunner.class)
public class OfferBatchHolderTest {
    private static final int BATCH_SIZE = 10;
    private static final int BUSINESS_SIZE = 30;
    private static final int COUNT_IN_PROCESS = 20;

    @Autowired
    private OfferProcessingStrategy fastCreateStrategy;
    @Autowired
    private OfferProcessingStrategy fastEditStrategy;

    private OfferBatchHolder offerBatchHolder;
    private OfferBatchHolder offerBatchHolderWithAlreadyInProgress;
    private OfferGroupingKey offerGroupingKey;

    @Before
    public void setUp() {
        offerBatchHolder = new OfferBatchHolder(BATCH_SIZE, BUSINESS_SIZE, 0);
        offerBatchHolderWithAlreadyInProgress = new OfferBatchHolder(BATCH_SIZE, BUSINESS_SIZE, COUNT_IN_PROCESS);
        offerGroupingKey = new OfferGroupingKey(0L, 0, 0, 0, PipelineType.DATA_CAMP);
    }

    @Test
    public void makeSureAllOffersWithTheSameGroupSingleStrategyAreInTheSameBatch() {
        long groupId = 0L;

        OfferGroupingKey key1 = new OfferGroupingKey(null, 0, groupId, 0, PipelineType.FAST_CARD);
        OffersGroupingWithStrategy offersGroupingWithStrategy = new OffersGroupingWithStrategy(key1, fastCreateStrategy,
                IntStream.range(0, 10).mapToObj(id -> getNewDatacampStrategyOffer(id, groupId))
                        .collect(Collectors.toList()));

        offerBatchHolder.addBatch(offersGroupingWithStrategy);
        assertThat(offerBatchHolder.getBatches().size()).isEqualTo(1);
        assertThat(offerBatchHolder.getBatches().get(0).getOffers().size()).isEqualTo(10);
        assertThat(offerBatchHolder.getBatches().get(0).getOffers().stream()
                .allMatch(offerInfo -> offerInfo.getGroupId() == groupId)).isTrue();
    }

    @Test
    public void makeSureAllOffersWithTheSameGroupMultipleStrategiesAreInTheSameBatch() {
        long groupId = 0L;
        long groupI2 = 1L;

        OfferGroupingKey key1 = new OfferGroupingKey(null, 0, groupId, 0, PipelineType.FAST_CARD);
        OfferGroupingKey key2 = new OfferGroupingKey(null, 0, groupId, 0, PipelineType.FAST_CARD);
        OfferGroupingKey key3 = new OfferGroupingKey(null, 0, groupId, 0, PipelineType.FAST_CARD);
        OffersGroupingWithStrategy offersGroupingWithStrategy1 = new OffersGroupingWithStrategy(key1, fastCreateStrategy,
                IntStream.range(0, 5).mapToObj(id -> getNewDatacampStrategyOffer(id, groupId))
                        .collect(Collectors.toList()));
        OffersGroupingWithStrategy offersGroupingWithStrategy2 = new OffersGroupingWithStrategy(key2, fastEditStrategy,
                IntStream.range(10, 30).mapToObj(id -> getNewDatacampStrategyOffer(id, groupI2))
                        .collect(Collectors.toList()));
        OffersGroupingWithStrategy offersGroupingWithStrategy3 = new OffersGroupingWithStrategy(key3, fastCreateStrategy,
                IntStream.range(5, 10).mapToObj(id -> getNewDatacampStrategyOffer(id, groupId))
                        .collect(Collectors.toList()));

        offerBatchHolder.addBatch(offersGroupingWithStrategy1);
        offerBatchHolder.addBatch(offersGroupingWithStrategy2);
        offerBatchHolder.addBatch(offersGroupingWithStrategy3);
        assertThat(offerBatchHolder.getBatches().size()).isEqualTo(2);
        assertThat(offerBatchHolder.getBatches().stream().anyMatch(batch ->
                batch.getOffers().stream()
                .allMatch(offerInfo -> offerInfo.getGroupId() == groupId) && batch.getOffers().size() == 10)).isTrue();
        assertThat(offerBatchHolder.getBatches().stream().anyMatch(batch ->
                batch.getOffers().stream()
                .allMatch(offerInfo -> offerInfo.getGroupId() == groupI2) && batch.getOffers().size() == 20)).isTrue();
    }

    @Test
    public void makeSureTwoBatchesCreatedWhenMoreThanBatchLimit() {
        long groupId1 = 0L;
        long groupId2 = 1L;
        OfferGroupingKey key1 = new OfferGroupingKey(null, 0, groupId1, 0, PipelineType.FAST_CARD);
        OfferGroupingKey key2 = new OfferGroupingKey(null, 0, groupId2, 0, PipelineType.FAST_CARD);
        OffersGroupingWithStrategy offersGroupingWithStrategy1 = new OffersGroupingWithStrategy(key1, fastCreateStrategy,
                IntStream.range(0, 9).mapToObj(id -> getNewDatacampStrategyOffer(id, groupId1))
                        .collect(Collectors.toList()));
        OffersGroupingWithStrategy offersGroupingWithStrategy2 = new OffersGroupingWithStrategy(key2, fastEditStrategy,
                IntStream.range(9, 11).mapToObj(id -> getNewDatacampStrategyOffer(id, groupId2))
                        .collect(Collectors.toList()));

        offerBatchHolder.addBatch(offersGroupingWithStrategy1);
        offerBatchHolder.addBatch(offersGroupingWithStrategy2);
        assertThat(offerBatchHolder.getBatches().size()).isEqualTo(2);
        assertThat(offerBatchHolder.getBatches().stream().anyMatch(batch -> batch.getOffers().size() == 9)).isTrue();
        assertThat(offerBatchHolder.getBatches().stream().anyMatch(batch -> batch.getOffers().size() == 2)).isTrue();
        assertThat(offerBatchHolder.getBatches().stream().mapToInt(batch -> batch.getOffers().size()).sum())
                .isEqualTo(11);
    }

    @Test
    public void makeSureBatchesAreNotCreatedWhenMoreThanBusinessLimit() {
        long groupId1 = 0L;
        long groupId2 = 0L;

        OfferGroupingKey key1 = new OfferGroupingKey(null, 0, groupId1, 0, PipelineType.FAST_CARD);
        OfferGroupingKey key2 = new OfferGroupingKey(null, 0, groupId2, 0, PipelineType.FAST_CARD);
        OffersGroupingWithStrategy offersGroupingWithStrategy1 = new OffersGroupingWithStrategy(key1, fastCreateStrategy,
                IntStream.range(0, 30).mapToObj(id -> getNewDatacampStrategyOffer(id, groupId1))
                        .collect(Collectors.toList()));
        OffersGroupingWithStrategy offersGroupingWithStrategy2 = new OffersGroupingWithStrategy(key2, fastEditStrategy,
                IntStream.range(30, 50).mapToObj(id -> getNewDatacampStrategyOffer(id, groupId2))
                        .collect(Collectors.toList()));

        offerBatchHolder.addBatch(offersGroupingWithStrategy1);
        offerBatchHolder.addBatch(offersGroupingWithStrategy2);
        assertThat(offerBatchHolder.getBatches().size()).isEqualTo(1);
        assertThat(offerBatchHolder.getBatches().get(0).getOffers().size()).isEqualTo(30);
    }

    @Test
    public void makeSureBatchesAreNotCreatedWhenMoreThanBusinessLimitAndSomeAlreadyInProcess() {
        long groupId = 0L;

        OfferGroupingKey key1 = new OfferGroupingKey(null, 0, groupId, 0, PipelineType.FAST_CARD);
        OffersGroupingWithStrategy offersGroupingWithStrategy1 = new OffersGroupingWithStrategy(key1, fastCreateStrategy,
                IntStream.range(0, 20).mapToObj(id -> getNewDatacampStrategyOffer(id, groupId))
                        .collect(Collectors.toList()));

        offerBatchHolderWithAlreadyInProgress.addBatch(offersGroupingWithStrategy1);
        assertThat(offerBatchHolder.getBatches().size()).isEqualTo(0);
    }
}
