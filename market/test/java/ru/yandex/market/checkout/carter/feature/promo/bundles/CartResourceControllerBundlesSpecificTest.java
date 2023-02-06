package ru.yandex.market.checkout.carter.feature.promo.bundles;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.client.Carter;
import ru.yandex.market.checkout.carter.model.CartItem;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.util.Utils;
import ru.yandex.market.checkout.carter.util.converter.CartConverter;
import ru.yandex.market.checkout.carter.web.UserContext;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.storage.StorageCartService.LIST_BASKET_ID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.createCartFor;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItemWithBundle;

public class CartResourceControllerBundlesSpecificTest extends CarterMockedDbTestBase {

    @Autowired
    private Carter carterClient;

    private UserContext userContext;

    @BeforeEach
    public void prepare() {
        userContext = UserContext.of(OwnerKey.of(Color.BLUE, UID,
                "" + Math.abs(ThreadLocalRandom.current().nextLong()))
        );
    }

    @Test
    public void shouldUpdateCartAndSaveAllItems() {
        final CartList cart = createCartFor(
                userContext,
                generateItem("primary offer", 10),
                generateItem("gift offer", 1)
        );

        final CartList updateResult = CartConverter.convert(carterClient.updateCart(userContext.getUserAnyId(), UID,
                Color.BLUE, CartConverter.convert(cart)));
        assertNotNull(updateResult);
        assertThat(updateResult.getItems(), hasSize(2));
        assertThat(updateResult.getItems(), hasItems(
                allOf(
                        hasProperty("id", greaterThan(0L)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("label", is("primary offer")),
                        hasProperty("count", comparesEqualTo(10))
                ),
                allOf(
                        hasProperty("id", greaterThan(0L)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("label", is("gift offer")),
                        hasProperty("count", comparesEqualTo(1))
                )
        ));

        final ItemOffer itemOffer = generateItemWithBundle("primary offer", "promo bundle", true);
        final CartList newCart = createCartFor(userContext, itemOffer);
        updateResult.getItems().forEach(item -> {
            if (item.getCount().equals(1)) {
                item.setPrimaryInBundle(false);
                item.setBundleId(itemOffer.getBundleId());
            } else {
                item.setCount(item.getCount() - 1);
            }
            newCart.getItems().add(item);
        });
        final CartList cartList = CartConverter.convert(carterClient.updateCart(userContext.getUserAnyId(), UID,
                Color.BLUE, CartConverter.convert(newCart)));
        assertNotNull(cartList);
        assertThat(cartList.getItems(), hasSize(3));

        final CartList result = CartConverter.convert(carterClient.getCart(
                userContext.getUserAnyId(), UID, 456L, Color.BLUE, false)).getBasketList();
        assertNotNull(result);
        assertThat(result.getItems(), hasSize(3));
        assertThat(result.getItems(), hasItems(
                allOf(
                        hasProperty("id", greaterThan(0L)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("label", is("primary offer")),
                        hasProperty("count", comparesEqualTo(9))
                ),
                allOf(
                        hasProperty("id", greaterThan(0L)),
                        hasProperty("bundleId", equalTo(itemOffer.getBundleId())),
                        hasProperty("label", is("gift offer")),
                        hasProperty("count", comparesEqualTo(1))
                ),
                allOf(
                        hasProperty("id", greaterThan(0L)),
                        hasProperty("bundleId", equalTo(itemOffer.getBundleId())),
                        hasProperty("label", is("primary offer in promo bundle")),
                        hasProperty("count", comparesEqualTo(1))
                )
        ));
    }

    @Test
    public void shouldUpdateCartAndSaveAllItemsForBundle() {
        // Создаём корзину с бандлом
        CartList cart = createCartFor(
                userContext,
                generateItemWithBundle("primary offer", "promo bundle", true),
                generateItemWithBundle("gift offer", "promo bundle")
        );
        carterClient.addItems(userContext.getUserAnyId(), UID, LIST_BASKET_ID, Color.BLUE,
                CartConverter.convert(cart), null);

        final CartList firstCart = CartConverter.convert(carterClient.getCart(
                userContext.getUserAnyId(), UID, 456L, Color.BLUE, false)).getBasketList();

        assertNotNull(firstCart);

        // Удаляем подарок
        final List<CartItem> giftItems = firstCart.getItems().stream()
                .filter(Predicate.not(CartItem::isPrimaryInBundle))
                .collect(toList());

        assertThat(giftItems, hasSize(1));

        final CartItem giftItem = giftItems.get(0);

        carterClient.removeItems(userContext.getUserAnyId(), UID, LIST_BASKET_ID, Color.BLUE,
                List.of(giftItem.getId()), null
        );

        final CartList cartAfterDelete = CartConverter.convert(carterClient.getCart(
                userContext.getUserAnyId(), UID, 456L, Color.BLUE, false)).getBasketList();

        assertNotNull(cartAfterDelete);
        assertThat(cartAfterDelete.getItems(), hasSize(1));

        final CartItem item = cartAfterDelete.getItems().get(0);
        assertTrue(item.isPrimaryInBundle());
        assertNotNull(item.getBundleId());
        assertEquals(1, item.getCount().intValue());

        // Увеличиваем количество основного товара
        carterClient.updateItem(userContext.getUserAnyId(), UID, LIST_BASKET_ID, Color.BLUE,
                item.getId(), 10, null
        );
        final CartList cartAfterIncreaseCount = CartConverter.convert(carterClient.getCart(
                userContext.getUserAnyId(), UID, 456L, Color.BLUE, false)).getBasketList();
        assertNotNull(cartAfterIncreaseCount);
        assertThat(cartAfterIncreaseCount.getItems(), hasSize(1));

        final CartItem updatedItem = cartAfterIncreaseCount.getItems().get(0);
        assertTrue(updatedItem.isPrimaryInBundle());
        assertNotNull(updatedItem.getBundleId());
        assertEquals(10, updatedItem.getCount().intValue());

        // Восстанавливаем подарок
        final long id = carterClient.addItem(userContext.getUserAnyId(), UID, LIST_BASKET_ID, Color.BLUE,
                CartConverter.convert(giftItem), null, true
        );

        final CartList cartAfterRestore = CartConverter.convert(carterClient.getCart(
                userContext.getUserAnyId(), UID, 456L, Color.BLUE, false)).getBasketList();

        assertNotNull(cartAfterRestore);
        assertThat(cartAfterRestore.getItems(), hasSize(2));

        // Обновляем корзину
        final CartList newCart = prepareNewCart(cartAfterRestore, userContext);
        final CartList updatedCart = CartConverter.convert(carterClient.updateCart(userContext.getUserAnyId(), UID,
                Color.BLUE, CartConverter.convert(newCart)));
        assertNotNull(updatedCart);
        assertThat(updatedCart.getItems(), hasSize(3));
    }

    private CartList prepareNewCart(@Nonnull final CartList cartAfterRestore, UserContext userContext) {
        final CartList newCart = CartList.of(
                cartAfterRestore.getId(),
                Utils.getNowUpToSeconds(),
                cartAfterRestore.getType(),
                null,
                Color.BLUE
        );
        final CartItem primaryItem = cartAfterRestore.getItems().stream()
                .filter(CartItem::isPrimaryInBundle)
                .findFirst()
                .orElseThrow();
        final CartItem bundledItem = new ItemOffer(primaryItem);
        bundledItem.setCount(1);
        bundledItem.setId(0);
        final CartItem notBundledItem = new ItemOffer(primaryItem);
        notBundledItem.setCount(9);
        notBundledItem.setBundleId(null);
        notBundledItem.setBundlePromoId(null);
        notBundledItem.setPrimaryInBundle(false);
        final CartItem savedGiftItem = cartAfterRestore.getItems().stream()
                .filter(Predicate.not(CartItem::isPrimaryInBundle))
                .findFirst()
                .orElseThrow();
        newCart.getItems().addAll(List.of(bundledItem, notBundledItem, savedGiftItem));
        return newCart;
    }
}
