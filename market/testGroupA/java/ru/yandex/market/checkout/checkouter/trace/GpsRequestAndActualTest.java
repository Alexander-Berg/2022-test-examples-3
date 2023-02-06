package ru.yandex.market.checkout.checkouter.trace;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CartParameters;
import ru.yandex.market.checkout.checkouter.client.CheckoutParameters;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class GpsRequestAndActualTest extends AbstractTraceLogTestBase {

    private static final String DIFFERENT_HASH =
            "vujQrQNMAdOzZmnKZ6gFMPXWOueGz50jupRcKN87Azi6lFwo3zsDOGTa22maa4ZBZNrbaZprhkG5atQBWNJmD4YW6HNFGsfvYHV" +
                    "+A5lGEe0=";

    @BeforeEach
    void before() {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndGps();
    }

    @Test
    void shouldNotFailIfNotHaveGps() {
        Parameters parameters = new Parameters();

        CartParameters cartParameters = CartParameters.builder()
                .withUid(BuyerProvider.UID)
                .build();

        MultiCart builtMultiCart = parameters.getBuiltMultiCart();
        builtMultiCart.getCarts().forEach(
                o -> {
                    AddressImpl address = (AddressImpl) o.getDelivery().getBuyerAddress();
                    address.setGps(null);
                    address.setPersonalGpsId(null);
                }
        );

        MultiCart cart = checkouterAPI.cart(builtMultiCart, cartParameters);

        List<Map<String, String>> events = TraceLogHelper.awaitTraceLog(inMemoryAppender, List.of());

        List<Map<String, String>> inEvents = events.stream()
                .filter(e -> "IN".equals(e.get("type")))
                .collect(Collectors.toList());

        MatcherAssert.assertThat(inEvents, hasSize(1));

        Map<String, String> inEvent = inEvents.get(0);
        MatcherAssert.assertThat(inEvent, Matchers.not(hasEntry(Matchers.is("kv.requestGps"),
                Matchers.any(String.class))));
        MatcherAssert.assertThat(inEvent, Matchers.not(hasEntry(Matchers.is("kv.actualGps"),
                Matchers.any(String.class))));
        MatcherAssert.assertThat(inEvent, Matchers.not(hasEntry(Matchers.is("kv.requestPersonalGps"),
                Matchers.any(String.class))));
        MatcherAssert.assertThat(inEvent, Matchers.not(hasEntry(Matchers.is("kv.actualPersonalGps"),
                Matchers.any(String.class))));
    }

    @Test
    void shouldSaveGps() {
        Parameters parameters = new Parameters();

        CartParameters cartParameters = CartParameters.builder()
                .withUid(BuyerProvider.UID)
                .build();

        MultiCart builtMultiCart = parameters.getBuiltMultiCart();
        builtMultiCart.getCarts().forEach(
                o -> {
                    AddressImpl address = (AddressImpl) o.getDelivery().getBuyerAddress();
                    address.setGps("22.34,56.78");
                    address.setPersonalGpsId("g424dfx2x3");
                }
        );

        MultiCart cart = checkouterAPI.cart(builtMultiCart, cartParameters);

        List<Map<String, String>> events = TraceLogHelper.awaitTraceLog(inMemoryAppender, List.of());

        List<Map<String, String>> inEvents = events.stream()
                .filter(e -> "IN".equals(e.get("type")))
                .collect(Collectors.toList());

        MatcherAssert.assertThat(inEvents, hasSize(1));

        Map<String, String> inEvent = inEvents.get(0);
        MatcherAssert.assertThat(inEvent, hasEntry("kv.requestGps", "22.34,56.78"));
        MatcherAssert.assertThat(inEvent, hasEntry("kv.actualGps", "22.34,56.78"));
        MatcherAssert.assertThat(inEvent, hasEntry("kv.requestPersonalGps", "g424dfx2x3"));
        MatcherAssert.assertThat(inEvent, hasEntry("kv.actualPersonalGps", "g424dfx2x3"));
    }

    @Test
    void shouldNotFailIfNotHaveGpsInCheckout() {

        Parameters parameters = new Parameters();


        CheckoutParameters cartParameters = CheckoutParameters.builder()
                .withUid(BuyerProvider.UID)
                .build();
        MultiOrder multiOrder = new MultiOrder();
        MultiCart builtMultiCart = parameters.getBuiltMultiCart();
        BeanUtils.copyProperties(builtMultiCart, multiOrder);
        multiOrder.getCarts().forEach(
                o -> {
                    ((AddressImpl) o.getDelivery().getBuyerAddress()).setGps(null);
                    o.getDelivery().setHash(DIFFERENT_HASH);
                }
        );

        MultiOrder order = checkouterAPI.checkout(multiOrder, cartParameters);

        List<Map<String, String>> events = TraceLogHelper.awaitTraceLog(inMemoryAppender, List.of());

        List<Map<String, String>> inEvents = events.stream()
                .filter(e -> "IN".equals(e.get("type")))
                .collect(Collectors.toList());

        MatcherAssert.assertThat(inEvents, hasSize(1));

        Map<String, String> inEvent = inEvents.get(0);
        MatcherAssert.assertThat(inEvent, Matchers.not(hasEntry(Matchers.is("kv.requestGps"),
                Matchers.any(String.class))));
        MatcherAssert.assertThat(inEvent, Matchers.not(hasEntry(Matchers.is("kv.requestPersonalGps"),
                Matchers.any(String.class))));
    }

    @Test
    void shouldSaveGpsInCheckout() {

        Parameters parameters = new Parameters();


        CheckoutParameters cartParameters = CheckoutParameters.builder()
                .withUid(BuyerProvider.UID)
                .build();
        MultiOrder multiOrder = new MultiOrder();
        MultiCart builtMultiCart = parameters.getBuiltMultiCart();
        BeanUtils.copyProperties(builtMultiCart, multiOrder);
        multiOrder.getCarts().forEach(
                o -> {
                    o.getDelivery().setHash(DIFFERENT_HASH);
                    AddressImpl address = (AddressImpl) (o.getDelivery().getBuyerAddress());
                    address.setGps("22.34,56.78");
                    address.setPersonalGpsId("g424dfx2x3");
                }
        );

        MultiOrder order = checkouterAPI.checkout(multiOrder, cartParameters);

        List<Map<String, String>> events = TraceLogHelper.awaitTraceLog(inMemoryAppender, List.of());

        List<Map<String, String>> inEvents = events.stream()
                .filter(e -> "IN".equals(e.get("type")))
                .collect(Collectors.toList());

        MatcherAssert.assertThat(inEvents, hasSize(1));

        Map<String, String> inEvent = inEvents.get(0);
        MatcherAssert.assertThat(inEvent, hasEntry("kv.requestGps", "22.34,56.78"));
        MatcherAssert.assertThat(inEvent, hasEntry("kv.requestPersonalGps", "g424dfx2x3"));
    }
}
