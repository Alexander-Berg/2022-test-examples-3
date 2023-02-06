package ru.yandex.market.mdm.integration.test.logbroker;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.consumer.stream.StreamConsumerConfig;
import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.common.logbroker.LogbrokerConsumerService;
import ru.yandex.market.mbo.common.logbroker.LogbrokerContext;
import ru.yandex.market.mbo.common.logbroker.LogbrokerEvent;
import ru.yandex.market.mbo.common.logbroker.LogbrokerListener;
import ru.yandex.market.mbo.common.logbroker.LogbrokerProducerService;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.FromIrisItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.IrisEventProcessor;
import ru.yandex.market.mboc.common.MdmBaseIntegrationTestWithLogbrokerClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;

@DirtiesContext
@SuppressWarnings("checkstyle:MagicNumber")
public class LogbrokerBatchIntegrationTest extends MdmBaseIntegrationTestWithLogbrokerClass {
    private static final int SEED = 343;

    @Resource
    private IrisEventProcessor irisEventProcessor;

    @Resource(name = "lbkxClientFactory")
    private LogbrokerClientFactory lbkxClientFactory;

    @Resource(name = "irisToMdmConsumerConfig")
    private StreamConsumerConfig irisConsumerConfig;

    @Resource
    private FromIrisItemRepository fromIrisItemRepository;

    @Resource(name = "irisToMdmBatchProducer")
    private LogbrokerProducerService<MdmIrisPayload.ItemBatch> irisReverseLogbrokerBatchProducerService;

    private EnhancedRandom random;

    private LogbrokerConsumerService irisLogbrokerConsumerService;

    @Before
    public void setUp() {
        random = TestDataUtils.defaultRandom(SEED);

        irisLogbrokerConsumerService = new LogbrokerConsumerService(
            lbkxClientFactory,
            irisConsumerConfig,
            new LogbrokerListener(irisEventProcessor));
    }

    @After
    public void tearDown() {
        irisLogbrokerConsumerService.tearDown();
    }

    @Test
    public void whenSendBatchedMessageToLogbrokerShouldReceiveAsUnprocessed() {
        MdmIrisPayload.Item item = supplierItem();
        MdmIrisPayload.ItemBatch batch = MdmIrisPayload.ItemBatch.newBuilder()
            .addItem(item)
            .setSendTs(Instant.now().toEpochMilli())
            .build();
        LogbrokerEvent<MdmIrisPayload.ItemBatch> event = new LogbrokerEvent<MdmIrisPayload.ItemBatch>().setEvent(batch);

        LogbrokerContext<MdmIrisPayload.ItemBatch> context = new LogbrokerContext<>();
        context.setEvents(Collections.singletonList(event), 1, MdmIrisPayload.ItemBatch::toByteArray);
        context.setOnSuccessBatchConsumer(logbrokerEvents -> {
            Assertions.assertThat(logbrokerEvents)
                .extracting(LogbrokerEvent::getEvent)
                .containsExactly(batch);
        });
        context.setOnFailureBatchConsumer(logbrokerEvents -> {
            throw new RuntimeException("Failed to sent event to logbroker, please check Write stats here: " +
                "https://nda.ya.ru/t/ajozn_A73VuWM6");
        });

        irisReverseLogbrokerBatchProducerService.uploadEvents(context);

        Awaitility.await().atMost(20, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                List<FromIrisItemWrapper> unprocessedItems = fromIrisItemRepository.getUnprocessedItemsBatch(1);
                Assertions.assertThat(unprocessedItems)
                    .usingElementComparatorIgnoringFields("receivedTs")
                    .containsExactlyInAnyOrder(new FromIrisItemWrapper(item));
            });
    }

    private MdmIrisPayload.Item supplierItem() {
        MdmIrisPayload.Item randItem = random.nextObject(MdmIrisPayload.Item.class).toBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(100500L)
                .setShopSku("ololo")
                .build()).build();

        // MARKETMDM-301, MARKETMDM-89 temporary disable checking received lifetime. until it doesn't supported
        MdmIrisPayload.ReferenceInformation info = randItem.getInformation(0)
            .toBuilder()
            .setSource(MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.WAREHOUSE).build())
            .setItemShippingUnit(ItemWrapperTestUtil.generateShippingUnit(
                getRandomDimensionInCm(), getRandomDimensionInCm(), getRandomDimensionInCm(),
                getRandomWeightInKg(), getRandomWeightInKg(), getRandomWeightInKg()))
            .clearLifetime()
            .build();
        return randItem.toBuilder().clearInformation().addInformation(info).build();
    }

    private double getRandomDimensionInCm() {
        return random.nextDouble() * 10 + 10;
    }

    private double getRandomWeightInKg() {
        return random.nextDouble() * 0.5 + 0.5;
    }
}
