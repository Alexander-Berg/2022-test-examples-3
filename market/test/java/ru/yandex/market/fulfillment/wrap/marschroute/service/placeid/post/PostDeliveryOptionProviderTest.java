package ru.yandex.market.fulfillment.wrap.marschroute.service.placeid.post;

import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.market.fulfillment.wrap.marschroute.factory.DeliveryOptions;
import ru.yandex.market.fulfillment.wrap.marschroute.model.MarschrouteLocation;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschroutePaymentType;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.PostDeliveryId;
import ru.yandex.market.fulfillment.wrap.marschroute.model.request.delivery.city.MarschrouteDeliveryCityRequest;
import ru.yandex.market.fulfillment.wrap.marschroute.model.request.order.MarschrouteCreateOrderRequest;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.delivery.city.DeliveryOption;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.delivery.city.MarschrouteDeliveryCity;
import ru.yandex.market.fulfillment.wrap.marschroute.service.MarschrouteDeliveryCityService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.exception.NoPostDeliveryOptionAvailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.fulfillment.wrap.marschroute.factory.DeliveryOptions.postDeliveryOption;
import static ru.yandex.market.fulfillment.wrap.marschroute.factory.MarschrouteCreateOrderRequests.createOrderRequest;
import static ru.yandex.market.fulfillment.wrap.marschroute.factory.MarschrouteLocations.TEST_LOCALITY;
import static ru.yandex.market.fulfillment.wrap.marschroute.factory.MarschrouteLocations.location;
import static ru.yandex.market.fulfillment.wrap.marschroute.factory.MarschrouteLocations.locationWithPrefix;
import static ru.yandex.market.fulfillment.wrap.marschroute.factory.MarschrouteOrders.order;
import static ru.yandex.market.fulfillment.wrap.marschroute.model.base.PostDeliveryId.POST_ONLINE;
import static ru.yandex.market.fulfillment.wrap.marschroute.service.placeid.post.PostDeliveryOptionProvider.LOCATION_PREFIXES;


class PostDeliveryOptionProviderTest {

    private static final String KLADR = "555";

    private PostDeliveryOptionProvider deliveryOptionProvider;
    private MarschrouteDeliveryCityService deliveryCityService;

    private static Stream<Arguments> provideLocationPrefixes() {
        return LOCATION_PREFIXES.stream()
                .map(Arguments::of);
    }

    @BeforeEach
    void init() {
        this.deliveryCityService = Mockito.mock(MarschrouteDeliveryCityService.class);
        this.deliveryOptionProvider = new PostDeliveryOptionProvider(deliveryCityService);
    }

    @EnumSource(PostDeliveryId.class)
    @ParameterizedTest
    void testPostDeliveryOptionFound(PostDeliveryId postDeliveryId) {
        DeliveryOption expectedPostOption = postDeliveryOption(postDeliveryId.getDeliveryId());

        MarschrouteDeliveryCity marschrouteDeliveryCity = new MarschrouteDeliveryCity();
        marschrouteDeliveryCity.setDeliveryOptions(new HashMap<String, DeliveryOption>() {{
            put(DeliveryOptions.POST_OPTION, expectedPostOption);
            put(DeliveryOptions.COURIER_OPTION, DeliveryOptions.courierDeliveryOption());
            put(DeliveryOptions.PICKUP_OPTION, DeliveryOptions.pickupDeliveryOption());
        }});

        when(deliveryCityService.getDeliveryCites(any())).thenReturn(
                Collections.singletonList(marschrouteDeliveryCity)
        );

        DeliveryOption actualOption = deliveryOptionProvider.findDeliveryOptions(
                orderRequest(postDeliveryId.getCode(), location(KLADR))
        );

        assertThat(actualOption)
                .as("Asserting that post delivery is returned")
                .isSameAs(expectedPostOption);

        verify(deliveryCityService, times(1))
                .getDeliveryCites(any());
    }

    @ParameterizedTest
    @MethodSource("provideLocationPrefixes")
    void testPostDeliveryModifiedLocality(String locationPrefix) {
        DeliveryOption expectedPostOption = postDeliveryOption(POST_ONLINE.getDeliveryId());

        MarschrouteCreateOrderRequest createOrderRequest =
                orderRequest(POST_ONLINE.getCode(), locationWithPrefix(KLADR, locationPrefix));

        MarschrouteDeliveryCity marschrouteDeliveryCity = new MarschrouteDeliveryCity();
        marschrouteDeliveryCity.setDeliveryOptions(new HashMap<>() {{
            put(DeliveryOptions.POST_OPTION, expectedPostOption);
            put(DeliveryOptions.COURIER_OPTION, DeliveryOptions.courierDeliveryOption());
            put(DeliveryOptions.PICKUP_OPTION, DeliveryOptions.pickupDeliveryOption());
        }});

        MarschrouteDeliveryCityRequest deliveryCityRequest = new MarschrouteDeliveryCityRequest();
        deliveryOptionProvider.fillRequestFields(deliveryCityRequest, createOrderRequest);
        deliveryCityRequest.setName(TEST_LOCALITY);

        when(deliveryCityService.getDeliveryCites(refEq(deliveryCityRequest)))
                .thenReturn(Collections.singletonList(marschrouteDeliveryCity));

        DeliveryOption actualOption = deliveryOptionProvider.findDeliveryOptions(createOrderRequest);

        assertThat(actualOption)
                .as("Asserting that post delivery is returned")
                .isSameAs(expectedPostOption);

        verify(deliveryCityService, times(3))
                .getDeliveryCites(any());
    }

    @EnumSource(PostDeliveryId.class)
    @ParameterizedTest
    void testNoPostDeliveryOptionFound(PostDeliveryId postDeliveryId) {

        MarschrouteDeliveryCity marschrouteDeliveryCity = new MarschrouteDeliveryCity();
        marschrouteDeliveryCity.setDeliveryOptions(new HashMap<String, DeliveryOption>() {{
            put(DeliveryOptions.COURIER_OPTION, DeliveryOptions.courierDeliveryOption());
            put(DeliveryOptions.PICKUP_OPTION, DeliveryOptions.pickupDeliveryOption());
        }});

        when(deliveryCityService.getDeliveryCites(any())).thenReturn(
                Collections.singletonList(marschrouteDeliveryCity)
        );

        assertThatThrownBy(
                () -> deliveryOptionProvider.findDeliveryOptions(orderRequest(postDeliveryId.getCode(),
                        location(KLADR)))
        ).isInstanceOf(NoPostDeliveryOptionAvailableException.class);
        verify(deliveryCityService, times(1)).getDeliveryCites(any());
    }

    private MarschrouteCreateOrderRequest orderRequest(int deliveryServiceId, MarschrouteLocation location) {
        return createOrderRequest(order(
                location,
                MarschroutePaymentType.CASH,
                100,
                null,
                String.valueOf(deliveryServiceId)
        ));
    }
}
