package ru.yandex.market.checkout.checkouter.actualization.utils;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.actualization.utils.DeliveryMatcher.matchesDeliveryOption;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature.DEFERRED_COURIER;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature.DEFERRED_COURIER_ONE_HOUR_INTERVAL;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature.DEFERRED_COURIER_WIDE_INTERVAL;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature.EXPRESS_DELIVERY_FASTEST;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature.EXPRESS_DELIVERY_WIDE;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature.ON_DEMAND;

public class DeliveryMatcherTest {

    @Test
    @DisplayName("POSITIVE: Опция без фичей должна матчится с опцией с фичой DEFERRED_COURIER на /checkout")
    public void shouldMatchOptionsWithDeferredCourierAndEmptyFeatureOnCheckout() {
        Delivery emptyFeaturesDelivery = DeliveryProvider.yandexDelivery().build();
        Delivery deferredCourierDelivery = DeliveryProvider.yandexDelivery()
                        .features(Set.of(DEFERRED_COURIER))
                        .build();

        assertTrue(matchesDeliveryOption(emptyFeaturesDelivery, deferredCourierDelivery, true));
        assertTrue(matchesDeliveryOption(deferredCourierDelivery, emptyFeaturesDelivery, true));
    }

    @Test
    @DisplayName("NEGATIVE: Опция без фичей не должна матчится с опцией с фичой DEFERRED_COURIER на /cart")
    public void shouldNotMatchOptionsWithDeferredCourierAndEmptyFeatureOnCart() {
        Delivery emptyFeaturesDelivery = DeliveryProvider.yandexDelivery().build();
        Delivery deferredCourierDelivery = DeliveryProvider.yandexDelivery()
                .features(Set.of(DEFERRED_COURIER))
                .build();

        assertFalse(matchesDeliveryOption(emptyFeaturesDelivery, deferredCourierDelivery, false));
        assertFalse(matchesDeliveryOption(deferredCourierDelivery, emptyFeaturesDelivery, false));
    }

    @DisplayName("POSITIVE: Опция с фичей DEFERRED_COURIER должна матчится с опцией с фичой DEFERRED_COURIER")
    @ParameterizedTest(name = "{displayName}: isCheckoutOperation={0}")
    @ValueSource(booleans = {false, true})
    public void shouldMatchOptionsWithDeferredCourierFeature(boolean isCheckoutOperation) {
        Delivery deferredCourierDelivery1 = DeliveryProvider.yandexDelivery()
                .features(Set.of(DEFERRED_COURIER))
                .build();
        Delivery deferredCourierDelivery2 = deferredCourierDelivery1.clone();

        assertTrue(matchesDeliveryOption(deferredCourierDelivery1, deferredCourierDelivery2, isCheckoutOperation));
        assertTrue(matchesDeliveryOption(deferredCourierDelivery2, deferredCourierDelivery1, isCheckoutOperation));
    }

    @Test
    @DisplayName("POSITIVE: Опция с фичей DEFERRED_COURIER и DEFERRED_COURIER_WIDE_INTERVAL" +
            " должна матчится с опцией с фичой DEFERRED_COURIER на /checkout")
    public void shouldMatchOptionsWithDifferentDeferredCourierFeatureOnCheckout1() {
        Delivery deferredCourierDelivery1 = DeliveryProvider.yandexDelivery()
                .features(Set.of(DEFERRED_COURIER))
                .build();
        Delivery deferredCourierDelivery2 =  DeliveryProvider.yandexDelivery()
                .features(Set.of(DEFERRED_COURIER, DEFERRED_COURIER_WIDE_INTERVAL))
                .build();

        assertTrue(matchesDeliveryOption(deferredCourierDelivery1, deferredCourierDelivery2, true));
        assertTrue(matchesDeliveryOption(deferredCourierDelivery2, deferredCourierDelivery1, true));
    }
    @Test
    @DisplayName("NEGATIVE: Опция с фичей DEFERRED_COURIER и DEFERRED_COURIER_WIDE_INTERVAL" +
            " не должна матчится с опцией с фичой DEFERRED_COURIER на /cart")
    public void shouldNotMatchOptionsWithDifferentDeferredCourierFeatureOnCart1() {
        Delivery deferredCourierDelivery1 = DeliveryProvider.yandexDelivery()
                .features(Set.of(DEFERRED_COURIER))
                .build();
        Delivery deferredCourierDelivery2 =  DeliveryProvider.yandexDelivery()
                .features(Set.of(DEFERRED_COURIER, DEFERRED_COURIER_WIDE_INTERVAL))
                .build();

        assertFalse(matchesDeliveryOption(deferredCourierDelivery1, deferredCourierDelivery2, false));
        assertFalse(matchesDeliveryOption(deferredCourierDelivery2, deferredCourierDelivery1, false));
    }

    @Test
    @DisplayName("POSITIVE: Опция с фичей DEFERRED_COURIER и DEFERRED_COURIER_ONE_HOUR_INTERVAL" +
            " должна матчится с опцией с фичой DEFERRED_COURIER на /checkout")
    public void shouldMatchOptionsWithDifferentDeferredCourierFeatureOnCheckout2() {
        Delivery deferredCourierDelivery1 = DeliveryProvider.yandexDelivery()
                .features(Set.of(DEFERRED_COURIER))
                .build();
        Delivery deferredCourierDelivery2 =  DeliveryProvider.yandexDelivery()
                .features(Set.of(DEFERRED_COURIER, DEFERRED_COURIER_ONE_HOUR_INTERVAL))
                .build();

        assertTrue(matchesDeliveryOption(deferredCourierDelivery1, deferredCourierDelivery2, true));
        assertTrue(matchesDeliveryOption(deferredCourierDelivery2, deferredCourierDelivery1, true));
    }

    @Test
    @DisplayName("NEGATIVE: Опция с фичей DEFERRED_COURIER и DEFERRED_COURIER_ONE_HOUR_INTERVAL" +
            " не должна матчится с опцией с фичой DEFERRED_COURIER на /cart")
    public void shouldNotMatchOptionsWithDifferentDeferredCourierFeatureOnCart2() {
        Delivery deferredCourierDelivery1 = DeliveryProvider.yandexDelivery()
                .features(Set.of(DEFERRED_COURIER))
                .build();
        Delivery deferredCourierDelivery2 =  DeliveryProvider.yandexDelivery()
                .features(Set.of(DEFERRED_COURIER, DEFERRED_COURIER_ONE_HOUR_INTERVAL))
                .build();

        assertFalse(matchesDeliveryOption(deferredCourierDelivery1, deferredCourierDelivery2, false));
        assertFalse(matchesDeliveryOption(deferredCourierDelivery2, deferredCourierDelivery1, false));
    }

    @DisplayName("NEGATIVE: Опция c фичей ON_DEMAND не должна матчится с опцией с фичой DEFERRED_COURIER")
    @ParameterizedTest(name = "{displayName}: isCheckoutOperation={0}")
    @ValueSource(booleans = {false, true})
    public void shouldNotMatchOptionsWithDeferredCourierAndOnDemandFeature(boolean isCheckoutOperation) {
        Delivery onDemandFeaturesDelivery = DeliveryProvider.yandexDelivery()
                .features(Set.of(ON_DEMAND))
                .build();
        Delivery deferredCourierDelivery = DeliveryProvider.yandexDelivery()
                .features(Set.of(DEFERRED_COURIER))
                .build();

        assertFalse(matchesDeliveryOption(onDemandFeaturesDelivery, deferredCourierDelivery, isCheckoutOperation));
        assertFalse(matchesDeliveryOption(deferredCourierDelivery, onDemandFeaturesDelivery, isCheckoutOperation));
    }

    @DisplayName("NEGATIVE: Опция c фичей ON_DEMAND не должна матчится с опцией без фичей")
    @ParameterizedTest(name = "{displayName}: isCheckoutOperation={0}")
    @ValueSource(booleans = {false, true})
    public void shouldNotMatchOptionsWithOnDemandAndEmptyFeature(boolean isCheckoutOperation) {
        Delivery onDemandFeaturesDelivery = DeliveryProvider.yandexDelivery()
                .features(Set.of(ON_DEMAND))
                .build();
        Delivery emptyFeaturesDelivery = DeliveryProvider.yandexDelivery().build();

        assertFalse(matchesDeliveryOption(onDemandFeaturesDelivery, emptyFeaturesDelivery, isCheckoutOperation));
        assertFalse(matchesDeliveryOption(emptyFeaturesDelivery, onDemandFeaturesDelivery, isCheckoutOperation));
    }

    @Test
    @DisplayName("POSITIVE: Опции с фичами EXPRESS_DELIVERY_FASTEST и EXPRESS_DELIVERY_WIDE должны игнорироваться")
    public void shouldIgnoreExpressDeliveryFeatures() {
        Delivery aDelivery = DeliveryProvider.yandexDelivery()
                .features(Set.of(EXPRESS_DELIVERY_WIDE, EXPRESS_DELIVERY_FASTEST))
                .build();
        Delivery bDelivery = DeliveryProvider.yandexDelivery().build();

        assertTrue(matchesDeliveryOption(aDelivery, bDelivery, true, true));
        assertTrue(matchesDeliveryOption(bDelivery, aDelivery, true, true));
    }

    @Test
    @DisplayName("POSITIVE: Опции с фичами EXPRESS_DELIVERY_FASTEST и EXPRESS_DELIVERY_WIDE должны игнорироваться")
    public void shouldIgnoreExpressDeliveryFeaturesWithOtherFeatures() {
        Delivery aDelivery = DeliveryProvider.yandexDelivery()
                .features(Set.of(ON_DEMAND, EXPRESS_DELIVERY_WIDE, EXPRESS_DELIVERY_FASTEST))
                .build();
        Delivery bDelivery = DeliveryProvider.yandexDelivery()
                .features(Set.of(ON_DEMAND))
                .build();

        assertTrue(matchesDeliveryOption(aDelivery, bDelivery, true, true));
        assertTrue(matchesDeliveryOption(bDelivery, aDelivery, true, true));
    }

    @Test
    @DisplayName("NEGATIVE: Опции с фичами EXPRESS_DELIVERY_FASTEST и EXPRESS_DELIVERY_WIDE не должны игнорироваться")
    public void shouldNotIgnoreExpressDeliveryFeatures() {
        Delivery aDelivery = DeliveryProvider.yandexDelivery()
                .features(Set.of(ON_DEMAND, EXPRESS_DELIVERY_WIDE, EXPRESS_DELIVERY_FASTEST))
                .build();
        Delivery bDelivery = DeliveryProvider.yandexDelivery()
                .features(Set.of(ON_DEMAND))
                .build();

        assertFalse(matchesDeliveryOption(aDelivery, bDelivery, true, false));
        assertFalse(matchesDeliveryOption(bDelivery, aDelivery, true, false));
    }
}
