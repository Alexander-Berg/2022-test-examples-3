package ru.yandex.market.checkout.carter.storage.dao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.carter.CartException;
import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.model.CartItem;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.ItemPromo;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.model.UserIdType;
import ru.yandex.market.checkout.carter.storage.StorageCartService;
import ru.yandex.market.checkout.carter.storage.Update;
import ru.yandex.market.checkout.carter.storage.dao.ydb.YdbDao;
import ru.yandex.market.checkout.carter.web.UserContext;
import ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class CartItemDaoTest extends CarterMockedDbTestBase {

    @Autowired
    private StorageCartService storageCartService;
    @Autowired
    private YdbDao ydbDao;

    private long firstItemId;
    private long secondItemId;
    private UserContext userContext;

    @BeforeEach
    public void setUp() {
        userContext = UserContext.of(OwnerKey.of(
                Color.BLUE, UserIdType.UID, "" + rnd.nextLong(1, Integer.MAX_VALUE)));
        this.firstItemId = addItem(userContext, firstItem());
        this.secondItemId = addItem(userContext, secondItem());
    }

    @Test
    public void deleteById() {
        assertTrue(hasItemParams(firstItemId));
        assertTrue(hasItemPromo(firstItemId));

        assertEquals(1, ydbDao.deleteById(firstItemId, userContext));
        assertEquals(1, ydbDao.deleteById(firstItemId, userContext));

        checkItemWasDeleted(firstItemId);
        // Убедимся, что параметры и промо тоже удалены
        assertFalse(hasItemParams(firstItemId));
        assertFalse(hasItemPromo(firstItemId));
    }

    @Test
    public void shouldDeleteItemsWithParams() {
        assertTrue(hasItemParams(firstItemId));
        assertTrue(hasItemParams(secondItemId));

        assertTrue(hasItemPromo(firstItemId));
        assertTrue(hasItemPromo(secondItemId));

        final Set<Long> ids = Set.of(firstItemId, secondItemId);
        assertEquals(2, ydbDao.deleteItems(ids, userContext));
        assertEquals(2, ydbDao.deleteItems(ids, userContext));

        checkItemWasDeleted(firstItemId);
        checkItemWasDeleted(secondItemId);
        // Убедимся, что параметры и промо тоже удалены
        assertFalse(hasItemParams(firstItemId));
        assertFalse(hasItemParams(secondItemId));

        assertFalse(hasItemPromo(firstItemId));
        assertFalse(hasItemPromo(secondItemId));
    }

    @Test
    public void updateItemCount() {

        assertEquals(1, ydbDao.updateItemCount(firstItemId, 11, Instant.now(), userContext));
        final CartItem cartItem = ydbDao.getCartItemWithoutProperties(userContext, firstItemId);
        assertNotNull(cartItem);
        assertEquals(11, cartItem.getCount().intValue());
    }

    @Test
    public void getItemWithMultiPromoTest() {
        prepareTestData("json/item_multipromo.json");
        List<ItemOffer> items = ydbDao.loadItemsForOwner(
                UserContext.of(
                        OwnerKey.of(
                                Color.BLUE,
                                UserIdType.UID,
                                "1"
                        )
                )
        );
        assertThat(items, hasSize(4));

        items = items.stream()
                .sorted(Comparator.comparingLong(CartItem::getId))
                .collect(Collectors.toList());

        ItemOffer item = items.stream().filter(i -> i.getObjId().equals("33133")).findFirst().get();
        assertThat(item.getPromos(), hasSize(3));
        assertThat(
                item.getPromos(),
                containsInAnyOrder(
                        new ItemPromo("key 1", "blue-3p-flash-discount"),
                        new ItemPromo("key 2", "durect-discount"),
                        new ItemPromo("key 3", "chespest-as-a-gift")
                )
        );

        item = items.stream().filter(i -> i.getObjId().equals("33233")).findFirst().get();
        assertThat(item.getPromos(), hasSize(1));
        assertThat(
                item.getPromos(),
                containsInAnyOrder(
                        new ItemPromo("key 1", "chespest-as-a-gift")
                )
        );

        item = items.stream().filter(i -> i.getObjId().equals("33333")).findFirst().get();
        assertThat(item.getPromos(), nullValue());

        item = items.stream().filter(i -> i.getObjId().equals("33433")).findFirst().get();
        assertThat(item.getPromos(), nullValue());
    }

    private ItemOffer firstItem() {
        final ItemOffer item = new ItemOffer("offer1-md5", "iPhone7");
        item.setShopId(1L);
        item.setHid(2L);
        item.setFee("some fee 1");
        item.setMsku(3L);
        item.setPrice(BigDecimal.TEN);
        item.setPromoType(ReportPromoType.CHEAPEST_AS_GIFT.getCode());

        Set<ItemPromo> promos = new HashSet<>();
        promos.add(new ItemPromo("promoKey", "promoType"));
        promos.add(new ItemPromo("promoKey2", "promoType3"));
        promos.add(new ItemPromo("promoKey3", "promoType3"));
        item.setPromos(promos);
        return item;
    }

    private ItemOffer secondItem() {
        final ItemOffer item = new ItemOffer("offer2-md5", "iPad Pro");
        item.setShopId(1L);
        item.setHid(2L);
        item.setFee("some fee 2");
        item.setMsku(33L);
        item.setPrice(BigDecimal.TEN);
        item.setBundleId("some bundle id");
        item.setBundlePromoId("some bundle promo id");
        item.setPromoType(ReportPromoType.GENERIC_BUNDLE.getCode());

        Set<ItemPromo> promos = new HashSet<>();
        promos.add(new ItemPromo("promoKey", "promoType"));
        promos.add(new ItemPromo("promoKey2", "promoType3"));
        promos.add(new ItemPromo("promoKey3", "promoType3"));
        promos.add(new ItemPromo("promoKey4", "promoType4"));
        item.setPromos(promos);
        return item;
    }

    private boolean hasItemParams(long itemId) {
        List<ItemOffer> items = ydbDao.getOwnerItems(Collections.singletonList(itemId), userContext);
        return !items.isEmpty();
    }

    private boolean hasItemPromo(long itemId) {
        List<ItemOffer> items = ydbDao.getOwnerItems(Collections.singletonList(itemId), userContext);
        return !items.isEmpty() && !items.get(0).getPromos().isEmpty();
    }

    private long addItem(@Nonnull final UserContext userContext, @Nonnull final ItemOffer item) {
        final Update<Long> itemResult = storageCartService.createOrReplaceItemByMsku(userContext, item);
        assertNotNull(itemResult);
        final long itemId = itemResult.getResult();
        final CartItem cartItem = ydbDao.getCartItemWithoutProperties(userContext, itemId);
        assertNotNull(cartItem);
        return itemId;
    }

    private void checkItemWasDeleted(final long itemId) {
        try {
            // Проверим, что в БД ничего не осталось
            ydbDao.getCartItemWithoutProperties(userContext, itemId);
            fail(String.format("cart item with id = %d still exists in DB", itemId));
        } catch (CartException e) {
            assertEquals(CartException.ERROR_404, e.getStatusCode());
        }
    }
}
