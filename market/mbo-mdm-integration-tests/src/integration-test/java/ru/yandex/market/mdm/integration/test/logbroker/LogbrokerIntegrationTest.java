package ru.yandex.market.mdm.integration.test.logbroker;

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
public class LogbrokerIntegrationTest extends MdmBaseIntegrationTestWithLogbrokerClass {
    private static final int SEED = 343;

    @Resource
    private IrisEventProcessor irisEventProcessor;

    @Resource(name = "lbkxClientFactory")
    private LogbrokerClientFactory lbkxClientFactory;

    @Resource(name = "irisToMdmConsumerConfig")
    private StreamConsumerConfig irisConsumerConfig;

    @Resource
    private FromIrisItemRepository fromIrisItemRepository;

    @Resource(name = "irisToMdmProducer")
    private LogbrokerProducerService<MdmIrisPayload.Item> irisReverseLogbrokerProducerService;

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

    /**
     * Избегайте параллельного запуска - пишется и читается один и тот же топик логброкера.
     */
    @Test
    public void whenSendMessageToLogbrokerShouldReceiveAsUnprocessed() {
        MdmIrisPayload.Item item = supplierItem();
        LogbrokerEvent<MdmIrisPayload.Item> event = new LogbrokerEvent<MdmIrisPayload.Item>().setEvent(item);

        LogbrokerContext<MdmIrisPayload.Item> context = new LogbrokerContext<>();
        context.setEvents(Collections.singletonList(event), 1, MdmIrisPayload.Item::toByteArray);
        context.setOnSuccessBatchConsumer(logbrokerEvents -> {
            Assertions.assertThat(logbrokerEvents)
                .extracting(LogbrokerEvent::getEvent)
                .containsExactly(item);
        });
        context.setOnFailureBatchConsumer(logbrokerEvents -> {
            throw new RuntimeException("Failed to sent event to logbroker, please check Write stats here: " +
                "https://nda.ya.ru/t/ajozn_A73VuWM6");
        });

        irisReverseLogbrokerProducerService.uploadEvents(context);

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
