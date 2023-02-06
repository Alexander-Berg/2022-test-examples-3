package ru.yandex.market.checkout.carter.storage.dao;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.model.CartItem;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.storage.StorageCartService;
import ru.yandex.market.checkout.carter.storage.dao.ydb.YdbDao;
import ru.yandex.market.checkout.carter.web.UserContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.carter.model.Color.BLUE;
import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.createCartFor;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.hasFieldsThatShouldNotBeNull;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.itemOf;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.keyOf;

public class CartListDaoReadTest extends CarterMockedDbTestBase {

    private static final String FIRST_OFFER = "first offer";
    private static final String SECOND_OFFER = "second offer";
    private static final String THIRD_OFFER = "third offer";

    @Autowired
    private StorageCartService cartService;
    @Autowired
    private YdbDao ydbDao;

    private UserContext userContext;
    private CartList basket;

    private static <T extends CartItem> Matcher<Iterable<T>> hasAllItems() {
        return hasItems(
                allOf(hasProperty("name", is(FIRST_OFFER)), hasFieldsThatShouldNotBeNull()),
                allOf(hasProperty("name", is(SECOND_OFFER)), hasFieldsThatShouldNotBeNull()),
                allOf(hasProperty("name", is(THIRD_OFFER)), hasFieldsThatShouldNotBeNull())
        );
    }

    @BeforeEach
    public void prepare() {
        userContext = UserContext.of(OwnerKey.of(BLUE, UID,
                "" + Math.abs(ThreadLocalRandom.current().nextLong()))
        );

        basket = cartService.replaceCartListOwnerId(userContext, createCartFor(
                userContext,
                generateItem(FIRST_OFFER, rnd.nextInt(1, 10)),
                generateItem(SECOND_OFFER, rnd.nextInt(1, 10)),
                generateItem(THIRD_OFFER, rnd.nextInt(1, 10))
        )).getResult();
    }

    @Test
    public void shouldLoadItemsForOwner() {
        assertThat(ydbDao.loadItemsWithUserAndColor(userContext), hasAllItems());
    }

    @Test
    public void shouldLoadItemsForLists() {
        assertThat(ydbDao.loadItemsWithUserAndColor(userContext), hasAllItems());
    }

    @Test
    public void shouldGetCartItemWithoutProperties() {
        CartItem item = ydbDao.getCartItemWithoutProperties(userContext,
                itemOf(basket, keyOf(FIRST_OFFER)).getId());

        assertThat(item, not(hasProperty("msku", notNullValue())));
    }

    @Test
    public void shouldQueryItemByWareMd5() {
        ItemOffer expected = itemOf(basket, keyOf(FIRST_OFFER));

        assertThat(
                ydbDao.queryItemByUserOrOwnerId(userContext, expected).getResult(),
                is(expected.getId())
        );
    }

    @Test
    public void shouldNotQueryItemByWareMd5IfNotExist() {
        ItemOffer expected = itemOf(basket, keyOf(FIRST_OFFER), i -> i.setObjId("some ware_md5"));

        assertThat(ydbDao.queryItemByUserOrOwnerId(userContext, expected), nullValue());
    }

    @Test
    public void shouldQueryItemByMSKU() {
        ItemOffer expected = itemOf(basket, keyOf(FIRST_OFFER));

        assertThat(
                ydbDao.queryItem(userContext, expected).getResult(),
                is(expected.getId())
        );
    }

    @Test
    public void shouldNotQueryItemByMSKUIfNotExist() {
        ItemOffer expected = itemOf(basket, keyOf(FIRST_OFFER), i -> i.setMsku(123L));

        assertThat(ydbDao.queryItem(userContext, expected), nullValue());
    }

    @Test
    public void shouldDeleteItemsForList() {
        ydbDao.deleteItemsForOwner(List.of(
                itemOf(basket, keyOf(FIRST_OFFER)).getId(),
                itemOf(basket, keyOf(SECOND_OFFER)).getId()
        ), userContext);

        CartList updated = ydbDao.loadListsForUserContext(userContext).get(0);

        assertThat(updated.getItems(), hasSize(1));
        assertThat(updated.getItems(), hasItem(hasFieldsThatShouldNotBeNull()));
    }

    @Test
    public void shouldDeleteItemsForOwner() {
        ydbDao.deleteItemsForOwner(List.of(
                itemOf(basket, keyOf(FIRST_OFFER)).getId(),
                itemOf(basket, keyOf(SECOND_OFFER)).getId()
        ), userContext);

        CartList upadated = ydbDao.loadListsForUserContext(userContext).get(0);

        assertThat(upadated.getItems(), hasSize(1));
        assertThat(upadated.getItems(), hasItem(hasFieldsThatShouldNotBeNull()));
    }

    @Test
    public void regionIdIsNullTest() {
        ItemOffer offer = generateItem("test-offer-id");
        offer.setRegionId(null);
        basket = cartService.replaceCartListOwnerId(userContext,
                createCartFor(userContext, offer)).getResult();

        nullRegionIdAssertion(ydbDao.loadListsForUserContext(userContext).get(0).getItems());
        nullRegionIdAssertion(ydbDao.loadItemsWithUserAndColor(userContext));
    }

    private void nullRegionIdAssertion(List<? extends CartItem> itemOffers) {
        assertTrue(itemOffers
                .stream()
                .anyMatch(item -> item.getObjId().equals("test-offer-id")
                        && item.getRegionId() == null));
    }
}
