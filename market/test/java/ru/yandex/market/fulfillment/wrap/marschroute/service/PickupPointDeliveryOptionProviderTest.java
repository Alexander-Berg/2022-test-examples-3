package ru.yandex.market.fulfillment.wrap.marschroute.service;

import java.util.Collections;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.core.configuration.xml.XmlMappingConfiguration;
import ru.yandex.market.fulfillment.wrap.core.util.FulfillmentApiKeyManager;
import ru.yandex.market.fulfillment.wrap.marschroute.configuration.client.DeliveryClientConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.configuration.client.MarschrouteClientConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.factory.DeliveryOptions;
import ru.yandex.market.fulfillment.wrap.marschroute.model.MarschrouteOrder;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschroutePaymentType;
import ru.yandex.market.fulfillment.wrap.marschroute.model.request.order.MarschrouteCreateOrderRequest;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.delivery.city.DeliveryOption;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.delivery.city.MarschrouteDeliveryCity;
import ru.yandex.market.fulfillment.wrap.marschroute.service.exception.InvalidNumberOfSuitablePickupPointsException;
import ru.yandex.market.fulfillment.wrap.marschroute.service.exception.MissingPickupPointCodeException;
import ru.yandex.market.fulfillment.wrap.marschroute.service.placeid.pickup.PickupPointDeliveryOptionProvider;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.fulfillment.wrap.marschroute.factory.MarschrouteCreateOrderRequests.createOrderRequest;
import static ru.yandex.market.fulfillment.wrap.marschroute.factory.MarschrouteLocations.location;
import static ru.yandex.market.fulfillment.wrap.marschroute.factory.MarschrouteOrders.order;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
        XmlMappingConfiguration.class,
        DeliveryClientConfiguration.class,
        MarschrouteClientConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(PickupPointDeliveryOptionProvider.class)
class PickupPointDeliveryOptionProviderTest extends BaseIntegrationTest {

    @Autowired
    private PickupPointDeliveryOptionProvider provider;

    @MockBean
    private MarschrouteDeliveryCityService deliveryCityServiceMock;

    @MockBean
    private FulfillmentApiKeyManager apiKeyProvider;

    @BeforeEach
    void before() {
        when(apiKeyProvider.getKey()).thenReturn("1");
    }


    @Test
    void testProvideWhenPickupPointIsNull() {
        softly.assertThatThrownBy(() -> provider.findDeliveryOption(createOrderRequest(order())))
                .isInstanceOf(MissingPickupPointCodeException.class);
    }

    @Test
    void testProvideWhenNoDeliveryOptionsAreFound() {
        when(deliveryCityServiceMock.getDeliveryOptions(any())).thenReturn(Collections.emptyList());

        softly.assertThatThrownBy(this::callAndVerifyMocks)
                .isInstanceOf(InvalidNumberOfSuitablePickupPointsException.class);
    }

    @Test
    void testProvideWhenMultipleSuitableOptionsAreFound() {
        DeliveryOption deliveryOption = new DeliveryOption().setTransportApiCode("1");

        MarschrouteDeliveryCity marschrouteDeliveryCity = new MarschrouteDeliveryCity();
        marschrouteDeliveryCity.setDeliveryOptions(new HashMap<String, DeliveryOption>() {{
            put(DeliveryOptions.PICKUP_OPTION, deliveryOption);
            put(DeliveryOptions.COURIER_OPTION, deliveryOption);
        }});

        when(deliveryCityServiceMock.getDeliveryCites(any()))
                .thenReturn(Collections.singletonList(marschrouteDeliveryCity));

        softly.assertThatThrownBy(this::callAndVerifyMocks)
                .isInstanceOf(InvalidNumberOfSuitablePickupPointsException.class);
    }

    private void callAndVerifyMocks() {
        try {
            provider.findDeliveryOption(createFilledRequest());
        } finally {
            verify(deliveryCityServiceMock).getDeliveryOptions(any());
        }
    }

    private MarschrouteCreateOrderRequest createFilledRequest() {
        MarschrouteOrder order = order(location(""),
                MarschroutePaymentType.CASH,
                100,
                "1"
        );

        return createOrderRequest(order);
    }
}
