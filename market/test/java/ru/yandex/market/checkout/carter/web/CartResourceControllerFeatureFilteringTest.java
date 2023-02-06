package ru.yandex.market.checkout.carter.web;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.client.Carter;
import ru.yandex.market.checkout.carter.model.AddItemRequest;
import ru.yandex.market.checkout.carter.model.CartRequest;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.EatsFilter;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.model.UserIdType;
import ru.yandex.market.checkout.carter.util.converter.CartConverter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItemWith;

/**
 * @author zagidullinri
 * @date 17.02.2022
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CartResourceControllerFeatureFilteringTest extends CarterMockedDbTestBase {

    private static final String OFFER = "another_offer";
    private static final String EATS_OFFER = "some_offer";

    @Autowired
    private Carter carterClient;

    @Test
    public void shouldReturnAllItemsWhenEatsFilterIsWithRetail() {
        String uuid = UUID.randomUUID().toString();
        UserContext userContext = UserContext.of(OwnerKey.of(Color.WHITE, UserIdType.UUID, uuid));
        addItem(userContext, OFFER, Collections.emptySet());
        addItem(userContext, EATS_OFFER, Collections.singleton(EatsFilter.EATS_RETAIL_FEATURE));

        CartRequest cartRequest = CartRequest.builder(userContext.getUserAnyId(), userContext.getUserIdType())
                .withRgb(Collections.singletonList(userContext.getColor()))
                .withEatsFilter(EatsFilter.WITH_RETAIL)
                .build();
        Cart cart = CartConverter.convert(carterClient.getCart(cartRequest));
        assertThat(cart.getBasketList().getItems(), hasSize(2));
    }

    @Test
    public void shouldReturnEatsItemsWhenEatsFilterIsOnlyRetail() {
        String uuid = UUID.randomUUID().toString();
        UserContext userContext = UserContext.of(OwnerKey.of(Color.WHITE, UserIdType.UUID, uuid));
        addItem(userContext, OFFER, Collections.emptySet());
        addItem(userContext, EATS_OFFER, Collections.singleton(EatsFilter.EATS_RETAIL_FEATURE));

        CartRequest cartRequest = CartRequest.builder(userContext.getUserAnyId(), userContext.getUserIdType())
                .withRgb(Collections.singletonList(userContext.getColor()))
                .withEatsFilter(EatsFilter.ONLY_RETAIL)
                .build();
        Cart cart = CartConverter.convert(carterClient.getCart(cartRequest));
        assertThat(cart.getBasketList().getItems(), hasSize(1));
        assertThat(cart.getBasketList().getItems(), hasItem(hasProperty("name", is(EATS_OFFER))));
    }

    @Test
    public void shouldNotReturnEatsItemsWhenEatsFilterIsWithoutRetail() {
        String uuid = UUID.randomUUID().toString();
        UserContext userContext = UserContext.of(OwnerKey.of(Color.WHITE, UserIdType.UUID, uuid));
        addItem(userContext, OFFER, Collections.emptySet());
        addItem(userContext, EATS_OFFER, Collections.singleton(EatsFilter.EATS_RETAIL_FEATURE));

        CartRequest cartRequest = CartRequest.builder(userContext.getUserAnyId(), userContext.getUserIdType())
                .withRgb(Collections.singletonList(userContext.getColor()))
                .withEatsFilter(EatsFilter.WITHOUT_RETAIL)
                .build();
        Cart cart = CartConverter.convert(carterClient.getCart(cartRequest));
        assertThat(cart.getBasketList().getItems(), hasSize(1));
        assertThat(cart.getBasketList().getItems(), hasItem(hasProperty("name", is(OFFER))));
    }

    @Test
    public void shouldNotReturnEatsItemsWhenEatsFilterIsNull() {
        String uuid = UUID.randomUUID().toString();
        UserContext userContext = UserContext.of(OwnerKey.of(Color.WHITE, UserIdType.UUID, uuid));
        addItem(userContext, OFFER, Collections.emptySet());
        addItem(userContext, EATS_OFFER, Collections.singleton(EatsFilter.EATS_RETAIL_FEATURE));

        CartRequest cartRequest = CartRequest.builder(userContext.getUserAnyId(), userContext.getUserIdType())
                .withRgb(Collections.singletonList(userContext.getColor()))
                .build();
        Cart cart = CartConverter.convert(carterClient.getCart(cartRequest));
        assertThat(cart.getBasketList().getItems(), hasSize(1));
    }

    @NotNull
    private ItemOffer addItem(UserContext userContext, String offerId, Set<String> features) {
        ItemOffer itemOffer = generateItemWith(offerId, offer -> offer.setFeatures(features));

        AddItemRequest addItemRequest = AddItemRequest.builder()
                .withUserAnyId(userContext.getUserAnyId())
                .withUserIdType(userContext.getUserIdType())
                .withColor(userContext.getColor())
                .withCartItem(CartConverter.convert(itemOffer))
                .build();
        carterClient.addItem(addItemRequest);
        return itemOffer;
    }
}
