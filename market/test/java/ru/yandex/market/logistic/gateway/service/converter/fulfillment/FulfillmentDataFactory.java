package ru.yandex.market.logistic.gateway.service.converter.fulfillment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.google.common.collect.ImmutableList;

import ru.yandex.market.logistic.api.model.common.PartnerType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetAttachedDocsRequest;
import ru.yandex.market.logistics.werewolf.model.entity.DocOrder;
import ru.yandex.market.logistics.werewolf.model.entity.RtaOrdersData;

public final class FulfillmentDataFactory {
    private FulfillmentDataFactory() {
        throw new IllegalArgumentException();
    }

    public static GetAttachedDocsRequest.DocOrder createDocOrder(ResourceId id) {
        return new GetAttachedDocsRequest.DocOrder(
            id,
            BigDecimal.TEN,
            BigDecimal.ONE,
            1
        );
    }

    public static DocOrder.DocOrderBuilder wwDocOrderBuilder(String partnerId, String yandexId) {
        return DocOrder.builder()
            .partnerId(partnerId)
            .yandexId(yandexId)
            .assessedCost(BigDecimal.TEN)
            .weight(BigDecimal.ONE)
            .placesCount(1);
    }

    public static GetAttachedDocsRequest.OrdersData createOrdersData() {
        return new GetAttachedDocsRequest.OrdersData(
            ImmutableList.of(
                createDocOrder(createResourceId("a1", "b1").build()),
                createDocOrder(createResourceId("a2", "b2").build())
            ),
            createResourceId("sh1", "shp1").build(),
            DateTime.fromLocalDateTime(LocalDateTime.MAX),
            new GetAttachedDocsRequest.DocSender("testName", "1"),
            new GetAttachedDocsRequest.DocPartner("legalName", PartnerType.MARKET_SORTING_CENTER)
        );
    }

    public static RtaOrdersData.RtaOrdersDataBuilder wwOrdersDataBuilder() {
        return RtaOrdersData.builder()
            .orders(ImmutableList.of(
                wwDocOrderBuilder("b1", "a1").build(),
                wwDocOrderBuilder("b2", "a2").build()
            ))
            .partnerLegalName("legalName")
            .senderLegalName("testName")
            .shipmentId("shp1")
            .senderId("1")
            .shipmentDate(LocalDate.MAX);
    }

    public static ResourceId.ResourceIdBuilder createResourceId(String yandexId, String partnerId) {
        return ResourceId.builder().setYandexId(yandexId).setPartnerId(partnerId);
    }
}
