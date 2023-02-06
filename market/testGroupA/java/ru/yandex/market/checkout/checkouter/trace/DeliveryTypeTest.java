package ru.yandex.market.checkout.checkouter.trace;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CartParameters;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class DeliveryTypeTest extends AbstractTraceLogTestBase {

    @Test
    void shouldNotFailIfNotHaveAddressType() {
        Parameters parameters = new Parameters();

        CartParameters cartParameters = CartParameters.builder()
                .withUid(BuyerProvider.UID)
                .build();

        MultiCart builtMultiCart = parameters.getBuiltMultiCart();
        builtMultiCart.getCarts().forEach(
                o -> o.getDelivery().setType(null)
        );

        checkouterAPI.cart(builtMultiCart, cartParameters);

        List<Map<String, String>> events = TraceLogHelper.awaitTraceLog(inMemoryAppender, List.of());

        List<Map<String, String>> inEvents = events.stream()
                .filter(e -> "IN".equals(e.get("type")))
                .collect(Collectors.toList());

        MatcherAssert.assertThat(inEvents, hasSize(1));

        Map<String, String> inEvent = inEvents.get(0);
        MatcherAssert.assertThat(inEvent, Matchers.not(hasEntry(Matchers.is("kv.deliveryTypes"),
                Matchers.any(String.class))));
    }

    @Test
    void shouldSaveAddressType() {
        Parameters parameters = new Parameters();

        CartParameters cartParameters = CartParameters.builder()
                .withUid(BuyerProvider.UID)
                .build();

        MultiCart builtMultiCart = parameters.getBuiltMultiCart();
        builtMultiCart.getCarts().forEach(
                o -> o.getDelivery().setType(DeliveryType.DELIVERY)
        );

        checkouterAPI.cart(builtMultiCart, cartParameters);

        List<Map<String, String>> events = TraceLogHelper.awaitTraceLog(inMemoryAppender, List.of());

        List<Map<String, String>> inEvents = events.stream()
                .filter(e -> "IN".equals(e.get("type")))
                .collect(Collectors.toList());

        MatcherAssert.assertThat(inEvents, hasSize(1));

        Map<String, String> inEvent = inEvents.get(0);
        MatcherAssert.assertThat(inEvent, hasEntry("kv.deliveryTypes", "DELIVERY"));
    }

}
