package ru.yandex.market.checkout.carter.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matcher;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.carter.model.BundleUtils;
import ru.yandex.market.checkout.carter.model.CartItem;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.ColoredOwnerProvider;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.storage.Update;
import ru.yandex.market.checkout.carter.web.Cart;
import ru.yandex.market.checkout.carter.web.CartViewModel;
import ru.yandex.market.checkout.carter.web.ItemOfferViewModel;
import ru.yandex.market.checkout.carter.web.UserContext;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class CarterTestUtils {

    public static final long RUSSIA_REGION_ID = 225L;
    public static final Comparator<ItemOffer> OFFER_STATE_COMPARATOR = Comparator.comparing(
            ItemOffer::getObjId,
            nullsLast(naturalOrder())
    )
            .thenComparing(ItemOffer::getObjType, nullsLast(naturalOrder()))
            .thenComparing(ItemOffer::getMsku, nullsLast(naturalOrder()))
            .thenComparing(ItemOffer::isExpired, nullsLast(naturalOrder()))
            .thenComparing(ItemOffer::getName, nullsLast(naturalOrder()))
            .thenComparing(ItemOffer::getShopId, nullsLast(naturalOrder()))
            .thenComparing(ItemOffer::getModelId, nullsLast(naturalOrder()))
            .thenComparing(ItemOffer::getHid, nullsLast(naturalOrder()))
            .thenComparing(ItemOffer::getDesc, nullsLast(naturalOrder()))
            .thenComparing(ItemOffer::getFee, nullsLast(naturalOrder()))
            .thenComparing(ItemOffer::getOutletId, nullsLast(naturalOrder()));
    public static final String BUNDLE_NULL = "_";
    // fee=0.0200 fee_sum=3.97 show_block_id=9691078107358612136 show_uid=969107810735861213606008
    public static final String FEE = "8-qH2tqoDtKE6aHsEk-igaBtOPE4mzE_FNMCN4UY6cJ0gY0xbnPQEhGVS1" +
            "-4eEnCLJC0xqH3x77lmGA6N9tjFqM-r7e7jPVoFqmWrhOBz7m9vULGKQ6G7Q,,";
    private static final ThreadLocalRandom RND = ThreadLocalRandom.current();
    private static final AtomicLong COUNTER = new AtomicLong(System.currentTimeMillis() / 1000);


    private CarterTestUtils() {
    }

    public static ItemOffer getSingleItemFromCart(Cart cart) {
        assertNotNull(cart.getBasketList());
        assertEquals(1, cart.getBasketList().getItems().size());
        return (ItemOffer) cart.getBasketList().getItems().iterator().next();
    }

    public static ItemOfferViewModel getSingleItemFromCart(CartViewModel cart) {
        assertNotNull(cart.getLists().get(0));
        assertEquals(1, cart.getLists().get(0).getItems().size());
        return cart.getLists().get(0).getItems().iterator().next();
    }

    public static CartList generateCartList(String... objIds) {
        CartList list = new CartList();
        list.setType(CartList.Type.BASKET);
        for (String objId : objIds) {
            list.addItem(CarterTestUtils.generateItem(objId));
        }
        return list;
    }

    public static CartList generateCartList(int itemsCount) {
        CartList list = new CartList();
        list.setType(CartList.Type.BASKET);
        for (int i = 0; i < itemsCount; i++) {
            list.addItem(CarterTestUtils.generateItem(String.valueOf(i)));
        }
        return list;
    }

    public static void assertCartIsEmpty(Cart cart) {
        assertNotNull(cart);
        assertNotNull(cart.getBasketList());
        assertNotNull(cart.getBasketList().getItems());
        assertThat(cart.getBasketList().getItems(), org.hamcrest.Matchers.empty());
    }

    public static void assertItemsEqual(ItemOffer item1, ItemOffer item2) {
        assertEquals(0, OFFER_STATE_COMPARATOR.compare(item1, item2));
    }

    public static Multimap<String, CartItem> groupByBundles(Collection<? extends CartItem> cartItems) {
        return cartItems.stream()
                .reduce(HashMultimap.create(), (map, item) -> {
                    map.put((BundleUtils.isPromoBundle(item) ? item.getBundleId() : BUNDLE_NULL), item);
                    return map;
                }, (m1, m2) -> {
                    m1.putAll(m2);
                    return m1;
                });
    }

    public static ItemOffer generateItem(String offerId, int count, long msku) {
        final ItemOffer item = new ItemOffer(offerId.replaceAll("[ ]", "_"), offerId);
        item.setShopId(155L);
        item.setHid(124534242L);
        item.setLabel(offerId);
        item.setMsku(msku);
        item.setPrice(BigDecimal.valueOf(RND.nextDouble(1000, 10000)).setScale(2, RoundingMode.FLOOR));
        item.setModelId(RND.nextLong(1, 50000));
        item.setDesc("some words about nothing");
        item.setCreateTime(new Date(COUNTER.getAndIncrement() * 1000));
        item.setFee(UUID.randomUUID().toString());
        item.setCount(count);
        item.setRegionId(RUSSIA_REGION_ID);
        item.setPriceWithoutVat(new BigDecimal(102030).setScale(2, RoundingMode.FLOOR));
        return item;
    }

    public static ItemOffer generateItem(String offerId, int count) {
        return generateItem(offerId, count, offerId.hashCode());
    }

    public static ItemOffer generateItem(Color color, String offerId) {
        ItemOffer result = generateItem(offerId, 1, offerId.hashCode());
        result.setColor(color);
        return result;
    }

    public static ItemOffer generateItem(String offerId) {
        return generateItem(offerId, 1, offerId.hashCode());
    }

    public static ItemOffer generateItemWith(String offerId, Consumer<ItemOffer> decorator) {
        ItemOffer offer = generateItem(offerId);
        decorator.accept(offer);
        return offer;
    }

    public static ItemOffer generateMskuItemWith(Long msku, Consumer<ItemOffer> decorator) {
        ItemOffer offer = generateItem(UUID.randomUUID().toString());
        offer.setMsku(msku);
        decorator.accept(offer);
        return offer;
    }

    public static ItemOffer generateItemWithBundle(
            String offerId, String bundle, int count, long msku, boolean primary
    ) {
        ItemOffer offer = generateItem(offerId, count, msku);
        offer.setLabel(offerId + " in " + bundle);
        offer.setBundleId(bundle);
        offer.setBundlePromoId(DigestUtils.md5Hex(bundle));
        offer.setPrimaryInBundle(primary);
        offer.setFee(UUID.randomUUID().toString());
        offer.setCount(count);
        return offer;
    }

    public static ItemOffer generateItemWithBundle(String offerId, String bundle, int count, long msku) {
        return generateItemWithBundle(offerId, bundle, count, msku, false);
    }

    public static ItemOffer generateItemWithBundle(String offerId, String bundle, int count, boolean primary) {
        return generateItemWithBundle(offerId, bundle, count, offerId.hashCode(), primary);
    }

    public static ItemOffer generateItemWithBundle(String offerId, String bundle, int count) {
        return generateItemWithBundle(offerId, bundle, count, offerId.hashCode(), false);
    }

    public static ItemOffer generateItemWithBundle(String offerId, String bundle, boolean primary) {
        return generateItemWithBundle(offerId, bundle, 1, offerId.hashCode(), primary);
    }

    public static ItemOffer generateItemWithBundle(String offerId, String bundle) {
        return generateItemWithBundle(offerId, bundle, 1, offerId.hashCode(), false);
    }

    public static ItemOffer cloneItemWith(ItemOffer offer, Consumer<ItemOffer> decorator) {
        ItemOffer copy = cloneOffer(offer);
        decorator.accept(offer);
        return copy;
    }

    public static Stream<ItemOffer> cloneItemsWith(Stream<ItemOffer> offerStream, Consumer<ItemOffer> decorator) {
        List<ItemOffer> offers = offerStream.collect(Collectors.toList());
        return Stream.concat(offers.stream(), offers.stream()
                .map(ItemOffer::new)
                .peek(decorator));
    }

    public static Stream<ItemOffer> generateSomeItems(int itemsCount) {
        return IntStream.range(0, itemsCount).mapToObj(i -> generateItem(UUID.randomUUID().toString()));
    }

    public static Stream<ItemOffer> generateSomeBundles(int bundleCount, int itemsInBundleCount) {
        return IntStream.range(0, bundleCount).mapToObj(i -> UUID.randomUUID().toString())
                .flatMap(bundleId -> generateSomeItems(itemsInBundleCount)
                        .peek(offer -> {
                            offer.setBundleId(bundleId);
                            offer.setBundlePromoId(DigestUtils.md5Hex(bundleId));
                        }));
    }

    public static CartList cloneCart(CartList cartList) {
        return asCart(new ColoredOwnerProvider() {
            @Nonnull
            @Override
            public Color getColor() {
                return cartList.getRgb();
            }
        }, cartList.getItems().stream()
                .map(ItemOffer::new)
                .collect(Collectors.toMap(CarterTestUtils::keyOf, ItemOffer.class::cast)));
    }

    public static ItemOffer cloneOffer(ItemOffer offer) {
        ItemOffer clone = new ItemOffer(offer);
        clone.setId(0);
        clone.setCreateTime(new Date(COUNTER.getAndIncrement() * 1000));
        return clone;
    }

    public static CartList extractBasketList(Update<List<CartList>> listUpdate) {
        return extractBasketList(listUpdate.getResult());
    }

    public static CartList extractBasketList(List<CartList> lists) {
        return lists.stream()
                .filter(list -> CartList.Type.BASKET.equals(list.getType()))
                .findFirst().orElseThrow(() -> new IllegalStateException("Default list must be created"));
    }

    public static CartList asCart(@Nonnull ColoredOwnerProvider ownerProvider, Map<?, ItemOffer> itemMap) {
        CartList cartList = new CartList();
        cartList.setType(CartList.Type.BASKET);
        cartList.setName("some cart");
        cartList.setRgb(ownerProvider.getColor());

        itemMap.values().forEach(cartList::addItem);

        return cartList;
    }

    public static Map<Pair<String, String>, ItemOffer> cloneAsMap(CartList basket) {
        return asMap(cloneCart(basket));
    }

    public static Map<Pair<String, String>, ItemOffer> asMap(CartList basket) {
        return basket.getItems().stream()
                .map(ItemOffer.class::cast)
                .collect(Collectors.toMap(CarterTestUtils::keyOf, Function.identity()));
    }

    public static ItemOffer findById(CartList cartList, Long id) {
        return cartList.getItems().stream()
                .filter((i -> id.equals(i.getId())))
                .map(ItemOffer.class::cast)
                .findFirst().orElse(null);
    }

    public static Pair<String, String> keyOf(String name) {
        return Pair.of(name, BUNDLE_NULL);
    }

    public static Pair<String, String> keyOf(String name, String bundle) {
        return Pair.of(name, StringUtils.defaultString(bundle, BUNDLE_NULL));
    }

    public static Pair<String, String> keyOf(CartItem item) {
        return keyOf(item.getName(), BundleUtils.getPromoBundleId(item));
    }

    public static ItemOffer addItemTo(Map<Pair<String, String>, ItemOffer> change, ItemOffer itemOffer) {
        ItemOffer existed = change.get(keyOf(itemOffer));
        if (existed != null) {
            existed.setCount(existed.getCount() + itemOffer.getCount());
        } else {
            change.put(keyOf(itemOffer), itemOffer);
        }
        return itemOffer;
    }

    public static ItemOffer fromItem(ItemOffer offer, Consumer<ItemOffer> chainedDecorator) {
        chainedDecorator.accept(offer);
        return offer;
    }

    public static ItemOffer itemOf(CartList cartList, Pair<String, String> nameKey) {
        return itemOf(cartList, nameKey, itemOffer -> {
        });
    }

    public static void everyItemOf(CartList cartList, Consumer<ItemOffer> customize) {
        cartList.getItems().stream()
                .map(ItemOffer.class::cast)
                .forEach(customize);
    }

    public static <T> Collection<T> collectFromItems(CartList cartList, Function<ItemOffer, T> functionReference) {
        return cartList.getItems().stream()
                .map(ItemOffer.class::cast)
                .map(functionReference)
                .collect(Collectors.toSet());
    }

    public static ItemOffer itemOf(CartList cartList, Pair<String, String> nameKey, Consumer<ItemOffer> customizer) {
        ItemOffer original = requireNonNull(asMap(cartList).get(nameKey));
        ItemOffer offer = cloneOffer(original);
        offer.setId(original.getId());
        customizer.accept(offer);
        return offer;
    }

    public static ItemOffer itemWithIdOf(
            CartList cartList, Pair<String, String> nameKey, Consumer<ItemOffer> customizer
    ) {
        ItemOffer original = requireNonNull(asMap(cartList).get(nameKey));
        ItemOffer offer = new ItemOffer(null, null);
        offer.setId(original.getId());
        customizer.accept(offer);
        return offer;
    }

    @Nonnull
    public static CartList createCartFor(@Nonnull UserContext userContext, ItemOffer... offers) {
        CartList cartList = new CartList();
        cartList.setType(CartList.Type.BASKET);
        cartList.setName("some cart");
        cartList.setRgb(userContext.getColor());

        Arrays.stream(offers).forEach(cartList::addItem);
        return cartList;
    }

    @Nonnull
    public static CartList createCartFor(@Nonnull UserContext userContext, Collection<ItemOffer> offers) {
        CartList cartList = new CartList();
        cartList.setType(CartList.Type.BASKET);
        cartList.setName("some cart");
        cartList.setRgb(userContext.getColor());
        cartList.setItems(offers);
        return cartList;
    }

    public static int hashWritableParams(ItemOffer offer) {
        return Objects.hash(
                offer.getName(),
                offer.getShopId(),
                offer.getModelId(),
                offer.getMsku(),
                offer.getPrice(),
                offer.getDesc(),
                offer.getFee(),
                offer.getHid(),
                offer.getOutletId(),
                offer.getKind2Params(),
                offer.isExpired()
        );
    }

    public static void valuableFieldsShouldNotBeNull(CartList list) {
        assertThat(list.getItems(), everyItem(hasFieldsThatShouldNotBeNull()));
    }

    public static Matcher<CartItem> hasFieldsThatShouldNotBeNull() {
        return allOf(
                hasProperty("objId", notNullValue()),
                hasProperty("price", notNullValue()),
                hasProperty("count", notNullValue()),
                hasProperty("hid", notNullValue()),
                hasProperty("shopId", notNullValue())
        );
    }

    public static <E> List<E> asType(List<CartItem> items, Class<E> type) {
        return items.stream()
                .map(type::cast)
                .collect(Collectors.toUnmodifiableList());
    }
}
