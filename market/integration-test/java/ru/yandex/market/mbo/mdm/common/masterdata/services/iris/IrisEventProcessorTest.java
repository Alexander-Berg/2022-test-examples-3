package ru.yandex.market.mbo.mdm.common.masterdata.services.iris;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.FromIrisItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.pgaudit.PgAuditRecord;
import ru.yandex.market.mbo.pgaudit.PgAuditRepository;
import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SuppressWarnings("checkstyle:magicNumber")
public class IrisEventProcessorTest extends MdmBaseIntegrationTestClass {

    private static final int SEED = 343215;

    private EnhancedRandom random;

    @Autowired
    private IrisEventProcessor irisEventProcessor;

    @Autowired
    private FromIrisItemRepository fromIrisItemRepository;

    @Autowired
    private PgAuditRepository pgAuditRepository;

    @Before
    public void setUp() {
        random = TestDataUtils.defaultRandomBuilder(SEED)
            .randomize(CompressionCodec.class, (Randomizer<CompressionCodec>) () -> CompressionCodec.RAW)
            .build();
    }

    @Test
    public void whenHasUnprocessedItemShouldProcessAll() {
        List<FromIrisItemWrapper> unprocessedItems1 = fromIrisItemRepository.getUnprocessedItemsBatch(1);
        Assertions.assertThat(unprocessedItems1).isEmpty();

        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(new ShopSkuKey(1, "test"),
            MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(1.0, 1.5, 2.0, 0.001, null, null, 0L));
        MessageMeta messageMeta = random.nextObject(MessageMeta.class);

        MessageData messageData = new MessageData(item.toByteArray(), 0, messageMeta);
        MessageBatch messageBatch = new MessageBatch("topic", 0, Collections.singletonList(messageData));

        irisEventProcessor.process(messageBatch);

        List<FromIrisItemWrapper> expectedItems = item.getInformationList()
            .stream()
            .map(information -> item.toBuilder().clearInformation().addInformation(information).build())
            .map(FromIrisItemWrapper::new)
            .collect(Collectors.toList());

        List<FromIrisItemWrapper> unprocessedItems2 = fromIrisItemRepository.getUnprocessedItemsBatch(
            expectedItems.size() + 1);

        Assertions.assertThat(unprocessedItems2)
            .usingElementComparatorIgnoringFields("receivedTs")
            .containsExactlyInAnyOrderElementsOf(expectedItems);

        List<PgAuditRecord> auditRecords = pgAuditRepository.findAll("from_iris_item");
        Assertions.assertThat(auditRecords).hasSameSizeAs(item.getInformationList());
    }
}
