package ru.yandex.market.ff.dbqueue.producer;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.PutFFInboundRegistryPayload;
import ru.yandex.market.ff.util.FileContentUtils;
import ru.yandex.market.logistic.gateway.common.model.common.CompositeId;
import ru.yandex.market.logistic.gateway.common.model.common.Korobyte;
import ru.yandex.market.logistic.gateway.common.model.common.PartialId;
import ru.yandex.market.logistic.gateway.common.model.common.PartialIdType;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryBox;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryPallet;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryType;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.common.UnitCount;
import ru.yandex.market.logistic.gateway.common.model.common.UnitCountType;
import ru.yandex.market.logistic.gateway.common.model.common.UnitInfo;
import ru.yandex.market.logistic.gateway.common.model.common.UnitRelation;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundRegistry;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.RegistryItem;
import ru.yandex.market.logistic.gateway.common.model.utils.DateTime;

public class PutFFInboundRegistryQueueProducerSerializationTest extends IntegrationTest {

    @Autowired
    private PutFFInboundRegistryQueueProducer putFFInboundRegistryQueueProducer;

    @Test
    public void testSerializeWorks() throws IOException {
        PutFFInboundRegistryPayload payload = new PutFFInboundRegistryPayload(1L,
                InboundRegistry.builder(ResourceId.builder().setYandexId("3").build(),
                        ResourceId.builder().setYandexId("1").build(),
                        RegistryType.PLANNED)
                        .setDate(DateTime.fromLocalDateTime(LocalDateTime.of(2018, 1, 5, 10, 0)))
                        .setComment("Comment")
                        .setBoxes(getBoxes())
                        .setPallets(getPallets())
                        .setItems(getItems())
                        .build()
        );
        String payloadString = putFFInboundRegistryQueueProducer.getPayloadTransformer().fromObject(payload);

        JSONAssert.assertEquals(FileContentUtils.getFileContent(
                "consumer/put_ff_inbound_registry_payload_serialization.json"),
                payloadString, JSONCompareMode.NON_EXTENSIBLE);
    }

    private List<RegistryBox> getBoxes() {
        CompositeId compositeId = getCompositeId(List.of(
                new PartialId(PartialIdType.BOX_ID, "P001"),
                new PartialId(PartialIdType.ORDER_ID, "12345")));

        return List.of(new RegistryBox(
                new UnitInfo(
                        List.of(getUnitCount(1, UnitCountType.FIT)),
                        compositeId,
                        List.of(new UnitRelation(getCompositeId(List.of(new PartialId(PartialIdType.PALLET_ID,
                                "PL1001"))))),
                        null, null)
        ));
    }

    private List<RegistryPallet> getPallets() {
        CompositeId compositeId = getCompositeId(List.of(new PartialId(PartialIdType.PALLET_ID, "PL1001")));

        return List.of(new RegistryPallet(
                new UnitInfo(
                        List.of(getUnitCount(1, UnitCountType.FIT)),
                        compositeId,
                        null,
                        null, "Some pallet")
        ));
    }

    private List<RegistryItem> getItems() {
        CompositeId compositeId = getCompositeId(List.of(new PartialId(PartialIdType.ARTICLE, "ssku123"),
                new PartialId(PartialIdType.VENDOR_ID, "444444")));

        return List.of(new RegistryItem(
                new UnitInfo(
                        List.of(getUnitCount(10, UnitCountType.FIT),
                                getUnitCount(2, UnitCountType.DEFECT)),
                        compositeId,
                        List.of(
                                new UnitRelation(
                                        getCompositeId(List.of(
                                                new PartialId(PartialIdType.ORDER_ID, "12345"),
                                                new PartialId(PartialIdType.BOX_ID, "P001"))
                                        )
                                )
                        ),
                        new Korobyte(20, 10, 30, BigDecimal.valueOf(40L), null, null), "Some pallet"),
                List.of("vendorCode1", "vendorCode2"),
                null, "name", BigDecimal.valueOf(100L), null, null, null, null, null, true, 10,
                5, null, "Comment", null, null, null, null, null, null,
                null, null, null, null, null, null
        ));
    }

    private CompositeId getCompositeId(List<PartialId> partialIds) {
        return new CompositeId(partialIds);
    }

    private UnitCount getUnitCount(int quantity, UnitCountType countType) {
        return new UnitCount.UnitCountBuilder()
                .setCountType(countType)
                .setQuantity(quantity)
                .build();
    }
}
