package ru.yandex.market.ff.service.util;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.service.implementation.utils.RegistryUtils;
import ru.yandex.market.logistic.gateway.common.model.common.CompositeId;
import ru.yandex.market.logistic.gateway.common.model.common.PartialId;
import ru.yandex.market.logistic.gateway.common.model.common.PartialIdType;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryType;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.common.UnitCount;
import ru.yandex.market.logistic.gateway.common.model.common.UnitCountType;
import ru.yandex.market.logistic.gateway.common.model.common.UnitInfo;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OutboundRegistry;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.RegistryItem;

public class RegistryUtilsTest {


    @Test
    public void getOutboundRegistryWithUpdatedCountsWhenDuplicateKeyTest() {

        RequestItem requestItem = new RequestItem();
        requestItem.setArticle("test");
        requestItem.setSupplierId(1L);
        requestItem.setCount(10);
        RequestItem requestItem2 = new RequestItem();
        requestItem2.setArticle("test");
        requestItem2.setSupplierId(1L);
        requestItem2.setCount(20);


        CompositeId unitId = new CompositeId(
                List.of(new PartialId(PartialIdType.ARTICLE, "test"), new PartialId(PartialIdType.VENDOR_ID, "1")));

        UnitCount unitCount = new UnitCount.UnitCountBuilder()
                .setCountType(UnitCountType.FIT)
                .setQuantity(5)
                .setUnitIds(List.of(unitId))
                .build();

        RegistryItem registryItem = RegistryItem
                .builder(UnitInfo.builder()
                        .setCounts(List.of(unitCount))
                        .setCompositeId(unitId)
                        .build())
                .build();

        List<RequestItem> requestItems = List.of(requestItem, requestItem2);

        OutboundRegistry outboundRegistry = OutboundRegistry.builder(
                ResourceId.builder().setPartnerId("1").setYandexId("1").build(),
                ResourceId.builder().setPartnerId("1").setYandexId("1").build(),
                RegistryType.FACTUAL
        )
                .setBoxes(List.of())
                .setComment("comment")
                .setItems(List.of(registryItem))
                .build();


        OutboundRegistry outboundRegistryWithUpdatedCounts =
                RegistryUtils.getOutboundRegistryWithUpdatedCounts(outboundRegistry, null, requestItems);

        Assertions.assertEquals(1, outboundRegistryWithUpdatedCounts.getItems().size());
        Assertions.assertEquals(30, outboundRegistryWithUpdatedCounts.getItems()
                .get(0)
                .getUnitInfo()
                .getCounts()
                .get(0)
                .getQuantity()
        );

    }
}
