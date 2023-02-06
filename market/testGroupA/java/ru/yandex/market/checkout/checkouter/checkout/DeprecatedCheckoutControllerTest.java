package ru.yandex.market.checkout.checkouter.checkout;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryCipherService;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.common.util.StringEscapeUtils.escapeXml;

/**
 * For /checkout tests use OrderCreateHelper
 *
 * @author apershukov
 */
public class DeprecatedCheckoutControllerTest extends AbstractWebTestBase {

    private static final long SHOP_ID = 4545;

    @Autowired
    private DeliveryCipherService deliveryCipherService;

    @Autowired
    private TestSerializationService testSerializationService;

    /**
     * При чекауте заказа, доставляемого сторонней службой доставки посылка не создается
     * <p>
     * Действие
     * 1. выполнить запрос к POST /checkout с телом, содержащим заказ, доставляемый сторонней службой
     * <p>
     * Проверки
     * 1. У созданного заказа отсутсвует посылка
     */

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("При чекауте заказа, доставляемого сторонней службой доставки посылка не создается")
    @Test
    @Deprecated
    @Disabled
    public void testDoNotCreateShipmentOnSelfDeliveryOrderCheckout() throws Exception {
        // Действие
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.setDeliveryServiceId(99L);
        parameters.setPushApiDeliveryResponse(
                DeliveryProvider.buildPickupDeliveryResponseWithOutletCode(DeliveryResponse::new));
        parameters.getOrder().setDelivery(DeliveryProvider.shopSelfPickupDeliveryByOutletCode().build());

        Order order = orderCreateHelper.createOrder(parameters);

        // Проверки
        order = orderService.getOrder(order.getId());
        assertThat(order.getDelivery().getParcels(), empty());
    }

    @Test
    public void testPushApiErrorHandling() throws Exception {
        // Действие
        Date shipmentDate = Date.from(LocalDate.now().plus(3, ChronoUnit.DAYS).atStartOfDay()
                .toInstant(ZoneOffset.UTC));

        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.PICKUP);
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        delivery.setServiceName("Foreign Service");
        delivery.setPrice(BigDecimal.valueOf(1.11));
        delivery.setBuyerPrice(BigDecimal.valueOf(1.11));
        delivery.setDeliveryServiceId(99L);
        delivery.setDeliveryDates(new DeliveryDates(shipmentDate, shipmentDate));
        deliveryCipherService.cipherDelivery(delivery);

        mockReport();

        String errorMessage = "500 Server Error: Regions not set for shop 10206336";
        pushApiMock.stubFor(
                WireMock.post(urlPathEqualTo("/shops/" + SHOP_ID + "/cart"))
                        .willReturn(aResponse()
                                .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .withBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                        "<error>\n" +
                                        "    <code>HTTP</code>\n" +
                                        "    <message>" + escapeXml(errorMessage) + "</message>\n" +
                                        "    <shop-admin>true</shop-admin>\n" +
                                        "</error>"))
        );

        MultiOrder body = getBodyByDelivery(delivery, shipmentDate);
        MvcResult result = mockMvc.perform(post("/checkout")
                .param(CheckouterClientParams.SANDBOX, "true")
                .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(testSerializationService.serializeCheckouterObject(body)))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn();

        MultiOrder multiOrder = testSerializationService.deserializeCheckouterObject(
                result.getResponse().getContentAsString(), MultiOrder.class
        );

        assertEquals(1, multiOrder.getOrderFailuresCount());
        assertEquals(OrderFailure.Code.UNKNOWN_ERROR, multiOrder.getOrderFailures().get(0).getErrorCode());
        assertEquals(errorMessage, multiOrder.getOrderFailures().get(0).getErrorDetails());
    }

    private MultiOrder getBodyByDelivery(Delivery delivery, Date shipmentDate) {
        OrderItem orderItem = OrderItemProvider.pushApiOrderItem();
        orderItem.setShowInfo("xM/FOMXZ" +
                "/CT2p8XZDequQiC1LmwkyLv2XTooLHtupTbo0Ulmwh6snIVAHzG2EOoOWqmzkxbtXXLpbo9z9Th8HU1m" +
                "Sind8bdieVyxWzclBMhOeMMWxUOY48VSeBmYxusL");
        orderItem.setBuyerPrice(orderItem.getPrice());

        Order order = new Order();
        order.setShopId(4545L);
        order.setItems(Collections.singletonList(orderItem));
        order.setFake(true);
        order.setDelivery(DeliveryProvider.getByDelivery(delivery, shipmentDate));

        MultiOrder multiOrder = new MultiOrder();
        multiOrder.setBuyerRegionId(213L);
        multiOrder.setPaymentMethod(PaymentMethod.YANDEX);
        multiOrder.setBuyerCurrency(Currency.RUR);
        multiOrder.setBuyer(BuyerProvider.getBuyer());
        multiOrder.setOrders(Collections.singletonList(order));

        return multiOrder;
    }

    private void mockReport() throws IOException {
        reportMockWhite.stubFor(
                get(urlPathEqualTo("/yandsearch"))
                        .withQueryParam("place", equalTo("offerinfo"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withBody(IOUtils.toString(DeprecatedCheckoutControllerTest.class
                                                .getResourceAsStream("single_offer.json")))
                        )
        );

        reportMockWhite.stubFor(
                get(urlPathEqualTo("/yandsearch"))
                        .withQueryParam("place", equalTo("shop_info"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withBody("{\"results\": []}")
                        )
        );

        reportMockWhite.stubFor(
                get(urlPathEqualTo("/yandsearch"))
                        .withQueryParam("place", equalTo("outlets"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "text/xml")
                                        .withStatus(200)
                                        .withBody(
                                                IOUtils.toString(getClass()
                                                        .getResourceAsStream("outlet.xml"))
                                        )
                        )
        );
    }
}
