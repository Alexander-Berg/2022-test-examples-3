package ru.yandex.market.checkout.checkouter.order.edit;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StorageLimitDatesOptionsTest extends AbstractWebTestBase {

    @Autowired
    private WireMockServer pvzMock;
    @Autowired
    private WireMockServer postamatMock;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @Test
    public void shouldNotReturnOptionsForProcessingPvzOrder() {
        var order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPickup(DeliveryProvider.MOCK_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        var orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setChangeRequestTypes(Set.of(ChangeRequestType.STORAGE_LIMIT_DATE));
        var orderEditOptionsResponse = client.getOrderEditOptions(order.getId(),
                ClientRole.USER,
                order.getBuyer().getUid(),
                List.of(order.getRgb()),
                orderEditOptionsRequest);

        assertNotNull(orderEditOptionsResponse);
        assertNotNull(orderEditOptionsResponse.getStorageLimitDatesOptions());
        assertTrue(orderEditOptionsResponse.getStorageLimitDatesOptions().isEmpty());
    }

    @Test
    public void shouldNotReturnOptionsForDeliveryPvzOrder() {
        var order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPickup(DeliveryProvider.MOCK_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        var orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setChangeRequestTypes(Set.of(ChangeRequestType.STORAGE_LIMIT_DATE));
        var orderEditOptionsResponse = client.getOrderEditOptions(order.getId(),
                ClientRole.USER,
                order.getBuyer().getUid(),
                List.of(order.getRgb()),
                orderEditOptionsRequest);

        assertNotNull(orderEditOptionsResponse);
        assertNotNull(orderEditOptionsResponse.getStorageLimitDatesOptions());
        assertTrue(orderEditOptionsResponse.getStorageLimitDatesOptions().isEmpty());
    }

    @Test
    public void shouldReturnOptionsForPickupPvzOrder() {
        var order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPickup(DeliveryProvider.MOCK_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PICKUP);

        pvzMock.stubFor(
                get(urlPathEqualTo("/logistics/pickup-point/orders/" + order.getId() + "/reschedule-expiration"))
                        .willReturn(okJson("[\"2021-08-09\", \"2021-08-10\", \"2021-08-12\", \"2021-08-11\"]")
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
        );

        var orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setChangeRequestTypes(Set.of(ChangeRequestType.STORAGE_LIMIT_DATE));
        var orderEditOptionsResponse = client.getOrderEditOptions(order.getId(),
                ClientRole.USER,
                order.getBuyer().getUid(),
                List.of(order.getRgb()),
                orderEditOptionsRequest);

        assertNotNull(orderEditOptionsResponse);
        var storageLimitDatesOptions = orderEditOptionsResponse.getStorageLimitDatesOptions();
        assertNotNull(storageLimitDatesOptions);
        assertEquals(4, storageLimitDatesOptions.size());
        //is sorted list
        assertEquals(storageLimitDatesOptions.stream().sorted().collect(Collectors.toList()), storageLimitDatesOptions);
    }

    @Test
    public void shouldNotReturnOptionsForProcessingPostamatOrder() {
        var order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPostTerm(DeliveryProvider.MOCK_POST_TERM_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        var orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setChangeRequestTypes(Set.of(ChangeRequestType.STORAGE_LIMIT_DATE));
        var orderEditOptionsResponse = client.getOrderEditOptions(order.getId(),
                ClientRole.USER,
                order.getBuyer().getUid(),
                List.of(order.getRgb()),
                orderEditOptionsRequest);

        assertNotNull(orderEditOptionsResponse);
        assertNotNull(orderEditOptionsResponse.getStorageLimitDatesOptions());
        assertTrue(orderEditOptionsResponse.getStorageLimitDatesOptions().isEmpty());
    }

    @Test
    public void shouldNotReturnOptionsForDeliveryPostamatOrder() {
        var order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPostTerm(DeliveryProvider.MOCK_POST_TERM_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        var orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setChangeRequestTypes(Set.of(ChangeRequestType.STORAGE_LIMIT_DATE));
        var orderEditOptionsResponse = client.getOrderEditOptions(order.getId(),
                ClientRole.USER,
                order.getBuyer().getUid(),
                List.of(order.getRgb()),
                orderEditOptionsRequest);

        assertNotNull(orderEditOptionsResponse);
        assertNotNull(orderEditOptionsResponse.getStorageLimitDatesOptions());
        assertTrue(orderEditOptionsResponse.getStorageLimitDatesOptions().isEmpty());
    }

    @Test
    public void shouldReturnOptionsForPickupPostamatOrder() {
        var order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPostTerm(DeliveryProvider.MOCK_POST_TERM_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PICKUP);

        postamatMock.stubFor(
                get(urlPathEqualTo("/boxbot/api/pincode/logistics/orders/" + order.getId() + "/reschedule-expiration"))
                        .willReturn(okJson("[\"2021-08-09\", \"2021-08-10\", \"2021-08-12\", \"2021-08-11\"]")
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
        );

        var orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setChangeRequestTypes(Set.of(ChangeRequestType.STORAGE_LIMIT_DATE));
        var orderEditOptionsResponse = client.getOrderEditOptions(order.getId(),
                ClientRole.USER,
                order.getBuyer().getUid(),
                List.of(order.getRgb()),
                orderEditOptionsRequest);

        assertNotNull(orderEditOptionsResponse);
        var storageLimitDatesOptions = orderEditOptionsResponse.getStorageLimitDatesOptions();
        assertNotNull(storageLimitDatesOptions);
        assertEquals(4, storageLimitDatesOptions.size());
        //is sorted list
        assertEquals(storageLimitDatesOptions.stream().sorted().collect(Collectors.toList()), storageLimitDatesOptions);
    }
}
