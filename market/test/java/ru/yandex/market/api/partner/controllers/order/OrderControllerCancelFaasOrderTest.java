package ru.yandex.market.api.partner.controllers.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.api.partner.client.orderservice.PapiOrderServiceClient;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.orderservice.client.model.ActorType;
import ru.yandex.market.orderservice.client.model.BuyerDto;
import ru.yandex.market.orderservice.client.model.ChangeOrderStatus;
import ru.yandex.market.orderservice.client.model.ChangeOrderStatusResponse;
import ru.yandex.market.orderservice.client.model.ChangeOrderStatusResponseResult;
import ru.yandex.market.orderservice.client.model.CurrencyValue;
import ru.yandex.market.orderservice.client.model.DeliveryType;
import ru.yandex.market.orderservice.client.model.GetPartnerOrderPiResponse;
import ru.yandex.market.orderservice.client.model.OrderAddressDto;
import ru.yandex.market.orderservice.client.model.OrderStatus;
import ru.yandex.market.orderservice.client.model.OrderSubStatus;
import ru.yandex.market.orderservice.client.model.PartnerDetailedOrderPiDto;
import ru.yandex.market.orderservice.client.model.PartnerOrderDeliveryPiDto;
import ru.yandex.market.orderservice.client.model.PartnerOrderItemPiDto;
import ru.yandex.market.orderservice.client.model.PaymentMethod;
import ru.yandex.market.orderservice.client.model.PaymentType;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "OrderControllerGetFaasOrderTest.before.csv")
class OrderControllerCancelFaasOrderTest extends FunctionalTest implements ResourceUtilitiesMixin {
    private static final Logger LOG = LoggerFactory.getLogger(OrderControllerV2OrderCreateTest.class);

    private static final long PARTNER_ID = 668L;
    private static final long ORDER_ID = 1000000000000L + 123456L;

    @Autowired
    private PapiOrderServiceClient papiOrderServiceClient;

    @Test
    void cancelOrderJson() {
        prepareMocks();

        //language=JSON
        String orderCreateRequestJson = "{\n" +
                "  \"order\": {\n" +
                "    \"status\": \"CANCELLED\",\n" +
                "    \"substatus\": \"USER_REFUSED_PRODUCT\"\n" +
                "  }\n" +
                "}";

        try {
            ResponseEntity<String> response = FunctionalTestHelper.makeRequestWithContentType(
                    urlBasePrefix + String.format("/campaigns/10668/orders/%s/status.json", ORDER_ID),
                    HttpMethod.PUT,
                    orderCreateRequestJson,
                    String.class,
                    MediaType.APPLICATION_JSON);

            MbiAsserts.assertJsonEquals(
                    resourceAsString("OrderControllerCancelFaasOrderTest.cancelOrderResponse.json"),
                    response.getBody()
            );

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            LOG.error(ex + ", response: " + ex.getResponseBodyAsString());
            throw (ex);
        }

    }

    @Test
    void cancelOrderXml() {
        prepareMocks();

        //language=XML
        String orderCreateRequestXml = "<order status='CANCELLED' substatus='USER_REFUSED_PRODUCT'/>";

        try {
            ResponseEntity<String> response = FunctionalTestHelper.makeRequestWithContentType(
                    urlBasePrefix + String.format("/campaigns/10668/orders/%s/status.xml", ORDER_ID),
                    HttpMethod.PUT,
                    orderCreateRequestXml,
                    String.class,
                    MediaType.APPLICATION_XML);

            MbiAsserts.assertXmlEquals(
                    resourceAsString("OrderControllerCancelFaasOrderTest.cancelOrderResponse.xml"),
                    response.getBody()
            );

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            LOG.error(ex + ", response: " + ex.getResponseBodyAsString());
            throw (ex);
        }

    }

    private void prepareMocks() {
        when(papiOrderServiceClient.postChangeOrderStatus(
                eq(PARTNER_ID),
                eq(ORDER_ID),
                eq(ChangeOrderStatus.CANCELLED),
                eq(ActorType.API),
                eq(67282295L),
                eq(OrderSubStatus.USER_REFUSED_PRODUCT))
        ).thenReturn(CompletableFuture.completedFuture(
                new ChangeOrderStatusResponse()
                        .result(new ChangeOrderStatusResponseResult())
        ));

        PartnerDetailedOrderPiDto osOrder = (PartnerDetailedOrderPiDto) new PartnerDetailedOrderPiDto()
                .buyer(new BuyerDto())
                .orderId(ORDER_ID)
                .status(OrderStatus.DELIVERY)
                .hasCancellationRequest(true)
                .substatus(OrderSubStatus.SHIPPED)
                .createdAt(LocalDateTime.parse("2011-12-03T10:15:30")
                        .atZone(ZoneId.systemDefault())
                        .toOffsetDateTime())
                .itemsTotal(new CurrencyValue().value(BigDecimal.valueOf(1500L)))
                .subsidyTotal(new CurrencyValue().value(BigDecimal.ZERO))
                .paymentType(PaymentType.POSTPAID)
                .paymentMethod(PaymentMethod.YANDEX)
                .lines(List.of())
                .deliveryInfo(new PartnerOrderDeliveryPiDto()
                        .address(new OrderAddressDto()
                                .country("Россия")
                                .postcode("119313")
                                .city("Москва")
                                .subway("Проспект Вернадского")
                                .street("Ленинский проспект")
                                .house("90")
                                .floor("6")
                        )
                        .deliveryType(DeliveryType.DELIVERY))
                .lines(List.of(
                        new PartnerOrderItemPiDto()
                                .price(new CurrencyValue().value(BigDecimal.valueOf(1500)))
                                .offerName("Батарейка PKCELL")
                                .count(2L)
                                .feedId(1500L)
                                .shopSku("SHOPSKU!")
                                .itemId(1234L)
                                .ffWarehouseId(172L)
                ));

        when(papiOrderServiceClient.getPartnerOrderPI(eq(PARTNER_ID), eq(ORDER_ID)))
                .thenReturn(CompletableFuture.completedFuture(
                        new GetPartnerOrderPiResponse()
                                .result(osOrder)
                ));
    }
}
