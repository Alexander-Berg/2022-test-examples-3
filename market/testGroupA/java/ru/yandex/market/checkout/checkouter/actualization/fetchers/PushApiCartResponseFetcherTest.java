package ru.yandex.market.checkout.checkouter.actualization.fetchers;

import java.util.List;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.pushApi.PushApiConfigurer;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author zagidullinri
 * @date 08.06.2022
 */
public class PushApiCartResponseFetcherTest extends AbstractServicesTestBase {

    @Autowired
    private WireMockServer pushApiMock;
    @Autowired
    private PushApiConfigurer pushApiConfigurer;
    @Autowired
    private PushApiCartResponseFetcher pushApiCartResponseFetcher;
    @Autowired
    private TestSerializationService serializationService;

    @Test
    public void fetcherShouldSendPersonalIds() {
        Order order = OrderProvider.getBlueOrder();
        pushApiConfigurer.mockCart(order, List.of(), List.of(), false);

        pushApiCartResponseFetcher.fetch(createContext(order));
        List<ServeEvent> allServeEvents = pushApiMock.getAllServeEvents();
        assertThat(allServeEvents, hasSize(1));
        ServeEvent next = allServeEvents.iterator().next();
        Cart requestedCart = serializationService.deserializePushApiObject(
                next.getRequest().getBodyAsString(),
                Cart.class
        );

        assertThat(requestedCart.getBuyer().getPersonalPhoneId(), equalTo(BuyerProvider.PERSONAL_PHONE_ID));
        assertThat(requestedCart.getBuyer().getPersonalEmailId(), equalTo(BuyerProvider.PERSONAL_EMAIL_ID));
        assertThat(requestedCart.getBuyer().getPersonalFullNameId(), equalTo(BuyerProvider.PERSONAL_FULL_NAME_ID));
    }

    private ImmutableActualizationContext createContext(Order order) {
        ActualizationContext context = ActualizationContext.builder()
                .withImmutableMulticartContext(
                        ImmutableMultiCartContext.from(
                                MultiCartContext.createBy(ImmutableMultiCartParameters.builder().build(), Map.of()),
                                MultiCartProvider.single(order)))
                .withCart(order)
                .withInitialCart(ImmutableOrder.from(order))
                .build();
        return ImmutableActualizationContext.of(context);
    }
}
