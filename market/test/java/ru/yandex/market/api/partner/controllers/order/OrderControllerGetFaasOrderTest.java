package ru.yandex.market.api.partner.controllers.order;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.assertj.core.api.Assertions;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.api.partner.client.orderservice.PapiOrderServiceClient;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.common.retrofit.CommonRetrofitHttpExecutionException;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.orderservice.client.model.ApiError;
import ru.yandex.market.orderservice.client.model.BuyerDto;
import ru.yandex.market.orderservice.client.model.CommonApiResponse;
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
class OrderControllerGetFaasOrderTest extends FunctionalTest implements ResourceUtilitiesMixin {
    private static final Logger LOG = LoggerFactory.getLogger(OrderControllerV2OrderCreateTest.class);

    private static final long PARTNER_ID = 668L;
    private static final long ORDER_ID = 1000000000000L + 123456L;

    @Autowired
    private PapiOrderServiceClient papiOrderServiceClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void getOrderJson() {
        prepareMocks();

        try {
            ResponseEntity<String> response = FunctionalTestHelper.makeRequest(
                    urlBasePrefix + String.format("/campaigns/10668/orders/%s.json", ORDER_ID),
                    HttpMethod.GET,
                    String.class);

            MbiAsserts.assertJsonEquals(
                    resourceAsString("OrderControllerGetFaasOrderTest.getOrderResponse.json"),
                    response.getBody()
            );

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            LOG.error(ex + ", response: " + ex.getResponseBodyAsString());
            throw (ex);
        }

    }

    @Test
    void getOrderXml() {
        prepareMocks();

        try {
            ResponseEntity<String> response = FunctionalTestHelper.makeRequest(
                    urlBasePrefix + String.format("/campaigns/10668/orders/%s.xml", ORDER_ID),
                    HttpMethod.GET,
                    String.class);

            MbiAsserts.assertXmlEquals(
                    resourceAsString("OrderControllerGetFaasOrderTest.getOrderResponse.xml"),
                    response.getBody()
            );

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            LOG.error(ex + ", response: " + ex.getResponseBodyAsString());
            throw (ex);
        }

    }

    @Test
    void getOrderNotFound() throws IOException {
        var notFoundResult = new CommonApiResponse();
        var notFoundError = new ApiError();
        notFoundError.setCode(ApiError.CodeEnum.ORDER_NOT_FOUND);
        notFoundError.setMessage("Order not found: partnerId = 668, orderId = 1000000123456");
        notFoundResult.setErrors(List.of(notFoundError));

        when(papiOrderServiceClient.getPartnerOrderPI(eq(PARTNER_ID), eq(ORDER_ID)))
                .thenReturn(CompletableFuture.failedFuture(
                        new CompletionException(
                                new CommonRetrofitHttpExecutionException(
                                        "some message",
                                        404,
                                        null,
                                        mapper.writeValueAsString(notFoundResult)
                                )
                        )
                ));

        HttpClientErrorException exception = org.junit.jupiter.api.Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + String.format("/campaigns/10668/orders/%s.xml", ORDER_ID),
                        HttpMethod.GET,
                        String.class)
        );

        Assertions.assertThat(exception.getRawStatusCode()).isEqualTo(404);
    }

    private void prepareMocks() {
        PartnerDetailedOrderPiDto osOrder = new PartnerDetailedOrderPiDto()
                .buyer(new BuyerDto()
                        .firstName("Иван")
                        .middleName("Иванович")
                        .lastName("Иванов")
                        .phone("+74957777777")
                        .email("ivan.ivanov@yandex.ru"))
                .orderId(ORDER_ID)
                .status(OrderStatus.DELIVERY)
                .substatus(OrderSubStatus.SHIPPED)
                .createdAt(LocalDateTime.parse("2011-12-03T10:15:30")
                        .atZone(ZoneId.systemDefault())
                        .toOffsetDateTime())
                .itemsTotal(new CurrencyValue().value(BigDecimal.valueOf(1500L)).currency("RUB"))
                .subsidyTotal(new CurrencyValue().value(BigDecimal.ZERO).currency("RUB"))
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
                                .cis(List.of("CIS1", "CIS2"))
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
