package ru.yandex.market.gutgin.tms.service.datacamp.scheduling;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.Streams;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.gutgin.tms.config.OfferSchedulingConfig;
import ru.yandex.market.gutgin.tms.service.datacamp.scheduling.batching.OfferBatchHolder;
import ru.yandex.market.gutgin.tms.service.datacamp.scheduling.batching.OfferExtendedInfo;
import ru.yandex.market.partner.content.common.db.dao.dcp.DatacampOfferDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.gutgin.tms.service.datacamp.scheduling.OfferBatchHelper.getEditDatacampStrategyOffer;
import static ru.yandex.market.gutgin.tms.service.datacamp.scheduling.OfferBatchHelper.getNewDatacampStrategyOffer;
import static ru.yandex.market.gutgin.tms.service.datacamp.scheduling.OfferProcessingStrategy.Priority.DEFAULT;
import static ru.yandex.market.gutgin.tms.service.datacamp.scheduling.OfferProcessingStrategy.Priority.HIGH;

@ContextConfiguration(
        classes = {OfferSchedulingConfig.class}
)
@RunWith(SpringJUnit4ClassRunner.class)
public class OfferInfoBatchProducerTest {
    private OfferInfoBatchProducer offerInfoBatchProducer;
    private static final int BATCH_SIZE = 10;
    private static final int BUSINESS_SIZE = 100;

    @Autowired
    List<OfferProcessingStrategy> offerProcessingStrategies;

    @Before
    public void setUp() {
        offerInfoBatchProducer = new OfferInfoBatchProducer(BATCH_SIZE, BUSINESS_SIZE, offerProcessingStrategies);
    }

    @Test
    public void whenOffersSuitableExactlyForOneStrategyShouldLinkThem() {
        List<DatacampOfferDao.OfferInfo> offers = IntStream.range(0, 11)
                .mapToObj(OfferBatchHelper::getNewFastCardStrategyOffer)
                .collect(Collectors.toList());

        List<OfferExtendedInfo> offerWithStrategy =
                offerInfoBatchProducer.mapValidOffersByStrategy(offers, null);
        assertThat(offerWithStrategy).hasSize(11);
        List<OfferProcessingStrategy> actualStrategies = offerWithStrategy
                .stream()
                .map(OfferExtendedInfo::getStrategy)
                .distinct()
                .collect(Collectors.toList());

        assertThat(actualStrategies.size()).isOne();
        assertThat(actualStrategies.get(0).getPipelineType()).isEqualTo(PipelineType.FAST_CARD);
        assertThat(actualStrategies.get(0).getPriority()).isEqualTo(HIGH.getValue());
    }

    @Test
    public void whenNoStrategyForOfferShouldThrowException() {
        List<DatacampOfferDao.OfferInfo> offers = Stream.concat(
                IntStream.range(0, 11).mapToObj(OfferBatchHelper::getNoStrategyOffer),
                IntStream.range(12, 22).mapToObj(OfferBatchHelper::getNewFastCardStrategyOffer))
                .collect(Collectors.toList());

        AtomicReference<Collection<Long>> holder = new AtomicReference<>();
        offerInfoBatchProducer.mapValidOffersByStrategy(offers, holder::set);
        assertThat(holder.get().size()).isEqualTo(11);
    }

    @Test
    public void whenDifferentGroupsCheckDifferentBatches() {
        List<DatacampOfferDao.OfferInfo> offers = Streams.concat(
                        IntStream.range(0, 11).mapToObj(OfferBatchHelper::getNewFastCardStrategyOffer),
                        IntStream.range(11, 21).mapToObj(id -> getNewDatacampStrategyOffer(id, 0L)),
                        IntStream.range(21, 30).mapToObj(id -> getNewDatacampStrategyOffer(id, 1L)))
                .collect(Collectors.toList());

        OfferBatchHolder offerBatchHolder = offerInfoBatchProducer.createBatches(offers,
                null, 0);
        assertThat(offerBatchHolder).isNotNull();
        //Все офферы без группы с совпадающими бизнесом, категорией, стратегией и т.п. - в одном батче,
        //не превышающем BATCH_SIZE
        assertThat(offerBatchHolder.getBatches().size()).isEqualTo(4);
        assertThat(offerBatchHolder.getBatches().stream()
                .filter(batch -> batch.getStrategy().getPipelineType() == PipelineType.FAST_CARD).count()).isEqualTo(2);
        assertThat(offerBatchHolder.getBatches().stream()
                .filter(batch -> batch.getStrategy().getPipelineType() == PipelineType.CSKU).count()).isEqualTo(2);
    }

    @Test
    public void whenSingleGroupDifferentStrategiesCheckSingePipelineCreated() {
        List<DatacampOfferDao.OfferInfo> offers = Streams.concat(
                        IntStream.range(0, 5).mapToObj(id -> getNewDatacampStrategyOffer(id, 0L)),
                        IntStream.range(6, 10).mapToObj(id -> getEditDatacampStrategyOffer(id, 0L)))
                .collect(Collectors.toList());

        OfferBatchHolder offerBatchHolder = offerInfoBatchProducer.createBatches(offers,
                null, 0);
        assertThat(offerBatchHolder).isNotNull();
        assertThat(offerBatchHolder.getBatches().size()).isEqualTo(1);
        assertThat(offerBatchHolder.getBatches().get(0).getStrategy().getPipelineType()).isEqualTo(PipelineType.CSKU);
        assertThat(offerBatchHolder.getBatches().get(0).getStrategy().getPriority()).isEqualTo(DEFAULT.getValue());
    }

}
