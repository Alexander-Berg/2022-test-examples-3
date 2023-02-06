package ru.yandex.market.checkout.carter.storage.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.model.CartItem;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.ItemPromo;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.storage.StorageCartService;
import ru.yandex.market.checkout.carter.storage.dao.ydb.YdbDao;
import ru.yandex.market.checkout.carter.web.UserContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.carter.json.format.ItemField.BUNDLE_ID;
import static ru.yandex.market.checkout.carter.json.format.ItemField.COUNT;
import static ru.yandex.market.checkout.carter.json.format.ItemField.LABEL;
import static ru.yandex.market.checkout.carter.json.format.ItemField.NAME;
import static ru.yandex.market.checkout.carter.json.format.ItemField.OBJ_ID;
import static ru.yandex.market.checkout.carter.json.format.ItemField.Offer.DESCRIPTION;
import static ru.yandex.market.checkout.carter.json.format.ItemField.Offer.FEE;
import static ru.yandex.market.checkout.carter.json.format.ItemField.Offer.HID;
import static ru.yandex.market.checkout.carter.json.format.ItemField.Offer.IS_EXPIRED;
import static ru.yandex.market.checkout.carter.json.format.ItemField.Offer.KIND_2_PARAMS;
import static ru.yandex.market.checkout.carter.json.format.ItemField.Offer.MODEL_ID;
import static ru.yandex.market.checkout.carter.json.format.ItemField.Offer.MSKU;
import static ru.yandex.market.checkout.carter.json.format.ItemField.Offer.OUTLET_ID;
import static ru.yandex.market.checkout.carter.json.format.ItemField.Offer.PICTURE_URL;
import static ru.yandex.market.checkout.carter.json.format.ItemField.Offer.PRICE;
import static ru.yandex.market.checkout.carter.json.format.ItemField.Offer.SHOP_ID;
import static ru.yandex.market.checkout.carter.model.Color.BLUE;
import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.createCartFor;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.hasFieldsThatShouldNotBeNull;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.itemOf;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.keyOf;

@ExtendWith(SpringExtension.class)
public class CartListDaoUpdateTest extends CarterMockedDbTestBase {

    private static final String FIRST_OFFER = "first offer";
    private static final String SECOND_OFFER = "second offer";
    @Autowired
    private StorageCartService cartService;
    @Autowired
    private YdbDao ydbDao;
    private UserContext userContext;
    private CartList basket;
    private ItemOffer expected;

    public static Stream<Arguments> parameterizedTestData() {

        return List.of(
                checkField(BUNDLE_ID, ItemOffer::setBundleId, ItemOffer::getBundleId, "some bundle id"),
                checkField(NAME, ItemOffer::setName, ItemOffer::getName, "some name"),
                checkField(COUNT, ItemOffer::setCount, ItemOffer::getCount, 12),
                checkField(OBJ_ID, ItemOffer::setObjId, ItemOffer::getObjId, "some ware md5"),
                checkField(LABEL, ItemOffer::setLabel, ItemOffer::getLabel, "some label"),
                checkField(SHOP_ID, ItemOffer::setShopId, ItemOffer::getShopId, 123L),
                checkField(MODEL_ID, ItemOffer::setModelId, ItemOffer::getModelId, 123L),
                checkField(HID, ItemOffer::setHid, ItemOffer::getHid, 123L),
                checkField(DESCRIPTION, ItemOffer::setDesc, ItemOffer::getDesc, "some description"),
                checkField(FEE, ItemOffer::setFee, ItemOffer::getFee, "some fee"),
                checkField(PICTURE_URL, ItemOffer::setPictureUrl, ItemOffer::getPictureUrl, "some url"),
                checkField(KIND_2_PARAMS, ItemOffer::setKind2Params, ItemOffer::getKind2Params, "some params"),
                checkField(PRICE, ItemOffer::setPrice,
                        ItemOffer::getPrice,
                        BigDecimal.valueOf(123).setScale(2, RoundingMode.FLOOR)),
                checkField(OUTLET_ID, ItemOffer::setOutletId, ItemOffer::getOutletId, "some outlet"),
                checkField(MSKU, ItemOffer::setMsku, ItemOffer::getMsku, 123L),
                checkField(IS_EXPIRED, ItemOffer::setExpired, ItemOffer::isExpired, true)
        ).stream().map(Arguments::of);
    }

    private static <T extends CartItem, D> Object[] checkField(String field, BiConsumer<T, D> setter,
                                                               Function<T, D> getter,
                                                               D value) {
        return new Object[]{
                field,
                modifier(setter, value),
                accept(getter, value)
        };
    }

    private static <T extends CartItem, D> Consumer<T> modifier(BiConsumer<T, D> setter, D value) {
        return item -> setter.accept(item, value);
    }

    private static <T extends CartItem, D> Predicate<T> accept(Function<T, D> getter, D value) {
        return item -> Objects.equals(getter.apply(item), value);
    }

    @BeforeEach
    public void prepare() {
        userContext = UserContext.of(OwnerKey.of(BLUE, UID,
                "" + Math.abs(ThreadLocalRandom.current().nextLong())));
        basket = cartService.replaceCartListOwnerId(userContext, createCartFor(
                userContext,
                generateItem(FIRST_OFFER, rnd.nextInt(1, 10)),
                generateItem(SECOND_OFFER, rnd.nextInt(1, 10))
        )).getResult();

        expected = itemOf(basket, keyOf(FIRST_OFFER));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void shouldChangeOnBulkUpdate(String name, Consumer<ItemOffer> modifier, Predicate<ItemOffer> asserter) {
        modifier.accept(expected);


        ydbDao.bulkCreateItems(Set.of(expected), userContext);
        CartList updated = ydbDao.loadListsForUserContext(userContext).get(0);

        assertThat(updated.getItems(), hasSize(2));
        assertThat(updated.getItems(), everyItem(hasFieldsThatShouldNotBeNull()));
        assertTrue(asserter.test(itemOf(updated, keyOf(expected.getName(), expected.getBundleId()))));
    }

    @Test
    @SuppressWarnings("checkstyle:HiddenField")
    public void getListWithMultiPromoTest() {
        UserContext userContext = prepareTestData("json/item_multipromo.json");

        List<CartList> lists = ydbDao.loadListsForUserContext(userContext);
        assertThat(lists, hasSize(1));
        CartList list = lists.get(0);

        assertThat(list.getItems(), hasSize(4));
        CartItem item = list.getItems().stream().filter(i -> i.getObjId().equals("33133")).findFirst().get();
        getMultiPromoTest(item);
    }

    private void getMultiPromoTest(CartItem item) {
        assertThat(item.getPromos(), hasSize(3));
        assertThat(
                item.getPromos(),
                containsInAnyOrder(
                        new ItemPromo("key 1", "blue-3p-flash-discount"),
                        new ItemPromo("key 2", "durect-discount"),
                        new ItemPromo("key 3", "chespest-as-a-gift")
                )
        );
    }

    @Test
    @SuppressWarnings("checkstyle:HiddenField")
    public void getItemWithMultiPromoTest() {
        UserContext userContext = prepareTestData("json/item_multipromo.json");

        List<ItemOffer> items = ydbDao.loadItemsWithUserAndColor(userContext);
        assertThat(items, hasSize(4));
        CartItem item = items.stream().filter(i -> i.getObjId().equals("33133")).findFirst().get();
        getMultiPromoTest(item);
    }

    @Test
    @SuppressWarnings("checkstyle:HiddenField")
    public void updateItemsWithMultiPromoTest() {
        cleanDatabase();
        UserContext userContext = prepareTestData("json/update_item_multipromo.json");

        List<ItemOffer> offers = ydbDao.loadItemsWithUserAndColor(userContext);

        ItemOffer item1 = offers.get(0);
        item1.setListId(111);

        ItemPromo promo1 = new ItemPromo("key1", "type1");
        ItemPromo promo2 = new ItemPromo("key2", "type2");

        ItemOffer item2 = offers.get(1);
        item2.setListId(111);

        List<CartItem> storedItems = Arrays.asList(item1, item2);

        ItemOffer item1New = new ItemOffer(CartItem.Type.OFFER, "33133", "name");
        item1New.setListId(111);
        item1New.setId(item1.getId());
        item1New.setModelId(item1.getModelId());
        ItemPromo promo4 = new ItemPromo("key4", "type4");
        item1New.setPromos(Set.of(promo1, promo2, promo4));

        ItemOffer item2New = new ItemOffer(CartItem.Type.OFFER, "33233", "name");
        item2New.setListId(111);
        item2New.setId(item2.getId());
        item2New.setModelId(item2.getModelId());
        ItemPromo promo5 = new ItemPromo("key5", "type5");
        ItemPromo promo6 = new ItemPromo("key6", "type6");
        item2New.setPromos(Set.of(promo4, promo5, promo6));

        Set<CartItem> itemsToUpdate = Set.of(item1New, item2New);

        ydbDao.bulkCreateItems(itemsToUpdate, userContext);

        List<ItemOffer> items = ydbDao.loadItemsWithUserAndColor(userContext);
        assertThat(items, hasSize(2));
        assertThat(
                items.stream()
                        .map(CartItem::getObjId)
                        .collect(Collectors.toSet()),
                containsInAnyOrder("33133", "33233")
        );

        ItemOffer item1Actual = items.get(0);
        assertThat(item1Actual.getPromos(), hasSize(3));
        assertThat(
                item1Actual.getPromos(),
                containsInAnyOrder(
                        new ItemPromo("key1", "type1"),
                        new ItemPromo("key2", "type2"),
                        new ItemPromo("key4", "type4")
                )
        );

        ItemOffer item2Actual = items.get(1);
        assertEquals(item2Actual.getObjId(), "33233");
        assertThat(item2Actual.getPromos(), hasSize(3));
        assertThat(
                item2Actual.getPromos(),
                containsInAnyOrder(
                        new ItemPromo("key4", "type4"),
                        new ItemPromo("key5", "type5"),
                        new ItemPromo("key6", "type6")
                )
        );
    }
}
