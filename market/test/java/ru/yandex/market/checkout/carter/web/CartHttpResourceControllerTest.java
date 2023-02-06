package ru.yandex.market.checkout.carter.web;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.beans.SamePropertyValuesAs;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.json.format.ItemField;
import ru.yandex.market.checkout.carter.model.BundleUtils;
import ru.yandex.market.checkout.carter.model.CartItem;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.ServiceInfo;
import ru.yandex.market.checkout.carter.model.UserIdType;
import ru.yandex.market.checkout.carter.storage.dao.ydb.CarterYdbDao;
import ru.yandex.market.checkout.carter.util.converter.CartConverter;
import ru.yandex.market.checkout.carter.utils.CarterHttpHelper;
import ru.yandex.market.checkout.carter.utils.serialization.TestSerializationService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.RUSSIA_REGION_ID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItemWithBundle;

/**
 * @author Kirill Khromov
 * date: 29/01/2018
 */
@ExtendWith(SpringExtension.class)
public class CartHttpResourceControllerTest extends CarterMockedDbTestBase {

    private static final Integer COUNT = 1;
    private static final Integer NEW_COUNT = 3;
    private static final String USER_ID = "777";
    private static final String UUID_STR = "ffffuuuuu";
    private static final String FAKE_USER_ID = "5875984795734";
    private static final ItemOffer OFFER = generateItem(Color.BLUE, "test");
    private static final ResultMatcher RESULT_MATCHER = status().isOk();
    private static final ResultMatcher RESULT_MATCHER_NOT_FOUND = status().isNotFound();
    private static final ResultMatcher RESULT_MATCHER_BAD_REQUEST = status().isBadRequest();

    @Autowired
    private CarterHttpHelper carterHttpHelper;
    @Autowired
    private TestSerializationService testSerializationService;

    @Autowired
    private CarterYdbDao carterYdbDao;


    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.asList(new Object[][]{
                {UserIdType.UID, USER_ID},
                {UserIdType.UUID, UUID_STR},
                {UserIdType.YANDEXUID, FAKE_USER_ID}
        }).stream().map(Arguments::of);
    }

    @DisplayName("POST /item: добавление айтема в корзину")
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void postItemTest(UserIdType userIdType, String userId) throws Exception {
        carterHttpHelper.cleanCart(userIdType, userId);
        CartListViewModel cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Long listId = cartList.getId();
        Long itemId = carterHttpHelper.postItem(userIdType, userId, listId, OFFER, RESULT_MATCHER);
        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        ItemOfferViewModel cartItem = cartList.getItems().iterator().next();
        assertThat("Has different property values",
                CartConverter.convert(cartItem),
                SamePropertyValuesAs.samePropertyValuesAs(OFFER, "id", "createTime", "cartFee", "price"));
        assertEquals((long) itemId, cartItem.getId());
        BigDecimal positivePrice = cartItem.getPrice().abs();
        BigDecimal positiveActualPrice = OFFER.getPrice().abs();
        assertTrue(positivePrice.subtract(positiveActualPrice).compareTo(BigDecimal.valueOf(1, 3)) < 0);
        // при добавлении айтема в корзину cartFee берётся из поля fee
        assertEquals(cartItem.getCartFee(), OFFER.getFee());
    }

    @DisplayName("PUT /item: изменение количества айтема в корзине")
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void putItemTest(UserIdType userIdType, String userId) throws Exception {
        carterHttpHelper.cleanCart(userIdType, userId);
        CartListViewModel cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Long listId = cartList.getId();
        Long itemId = carterHttpHelper.postItem(userIdType, userId, listId, OFFER, RESULT_MATCHER);
        carterHttpHelper.putItem(userIdType, userId, listId, itemId, NEW_COUNT, RESULT_MATCHER);
        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        ItemOfferViewModel cartItem = cartList.getItems().iterator().next();
        assertEquals((long) itemId, cartItem.getId());
        assertEquals(NEW_COUNT, cartItem.getCount());
    }

    @DisplayName("DELETE /item: удаление айтема из корзины")
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void deleteItemTest(UserIdType userIdType, String userId) throws Exception {
        carterHttpHelper.cleanCart(userIdType, userId);
        CartListViewModel cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Long listId = cartList.getId();
        Long itemId = carterHttpHelper.postItem(userIdType, userId, listId, OFFER, RESULT_MATCHER);
        carterHttpHelper.deleteItem(userIdType, userId, listId, itemId, RESULT_MATCHER);
        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Collection<ItemOfferViewModel> cartItems = cartList.getItems();
        assertTrue(cartItems.isEmpty());
    }

    @DisplayName("POST /items: добавление нескольких айтемов в корзину")
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void postItemsTest(UserIdType userIdType, String userId) throws Exception {
        carterHttpHelper.cleanCart(userIdType, userId);
        CartListViewModel cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Long listId = cartList.getId();
        var item1 = generateItem("test1");
        String offerString1 = testSerializationService.serializeCarterObject(item1);
        var item2 = generateItem("test2");
        String offerString2 = testSerializationService.serializeCarterObject(item2);
        List<String> items = Arrays.asList(offerString1, offerString2);
        carterHttpHelper.postItems(userIdType, userId, listId, items, RESULT_MATCHER);
        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Collection<ItemOfferViewModel> cartItems = cartList.getItems();
        assertEquals(2, cartItems.size());
        cartItems
                .forEach(item -> assertEquals(item.getRegionId(), RUSSIA_REGION_ID));
        // при добавлении айтема в корзину cartFee берётся из поля fee
        assertTrue(cartItems.stream()
                .allMatch(item -> {
                    var cartFee = item.getCartFee();
                    return cartFee.equals(item1.getFee()) || cartFee.equals(item2.getFee());
                }));
    }

    @DisplayName("POST /items: добавление нескольких айтемов с бандлом в корзину")
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void postBundledItemsTest(UserIdType userIdType, String userId) throws Exception {
        carterHttpHelper.cleanCart(userIdType, userId);
        CartListViewModel cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Long listId = cartList.getId();
        ItemOffer firstItem = generateItemWithBundle("test1", "1");
        ItemOffer secondItem = generateItemWithBundle("test2", "1");

        List<String> bundledOffers = List.of(firstItem.getObjId(), secondItem.getObjId());
        String updatedBundleId = BundleUtils.generatePromoBundleIdFromOfferIds(bundledOffers);

        String offerString1 = testSerializationService.serializeCarterObject(firstItem);
        String offerString2 = testSerializationService.serializeCarterObject(secondItem);
        List<String> items = Arrays.asList(offerString1, offerString2);
        carterHttpHelper.postItems(userIdType, userId, listId, items, RESULT_MATCHER);
        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Collection<ItemOfferViewModel> cartItems = cartList.getItems();
        assertEquals(2, cartItems.size());
        for (ItemOfferViewModel item : cartItems) {
            assertEquals(updatedBundleId, item.getBundleId());
            assertEquals(RUSSIA_REGION_ID, item.getRegionId());
        }
    }

    @DisplayName("POST /items: добавление айтема с бандлом в корзину")
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void postBundledItemTest(UserIdType userIdType, String userId) throws Exception {
        carterHttpHelper.cleanCart(userIdType, userId);
        CartListViewModel cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Long listId = cartList.getId();
        ItemOffer firstItem = generateItemWithBundle("test1", "bundle1");
        String offerString1 = testSerializationService.serializeCarterObject(firstItem);
        List<String> items = List.of(offerString1);

        carterHttpHelper.postItems(userIdType, userId, listId, items, RESULT_MATCHER);
        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        List<ItemOffer> cartItems =
                cartList.getItems().stream().map(CartConverter::convert).collect(Collectors.toList());
        assertEquals(1, cartItems.size());
        CartItem item = cartItems.get(0);
        assertEquals("bundle1", item.getBundleId());
        assertEquals(RUSSIA_REGION_ID, item.getRegionId());
    }

    @DisplayName("DELETE /items: удаление нескольких айтемов из корзины")
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void deleteItemsTest(UserIdType userIdType, String userId) throws Exception {
        carterHttpHelper.cleanCart(userIdType, userId);
        CartListViewModel cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Long listId = cartList.getId();
        String offerString = testSerializationService.serializeCarterObject(OFFER);
        List<String> items = Arrays.asList(offerString, offerString);
        carterHttpHelper.postItems(userIdType, userId, listId, items, RESULT_MATCHER);
        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Collection<ItemOfferViewModel> cartItems = cartList.getItems();
        List<Long> itemsIds = new ArrayList<>();
        for (ItemOfferViewModel item : cartItems) {
            itemsIds.add(item.getId());
        }
        carterHttpHelper.deleteItems(userIdType, userId, listId, itemsIds, RESULT_MATCHER);
        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        cartItems = cartList.getItems();
        assertTrue(cartItems.isEmpty());
    }

    @DisplayName("DELETE /item: удаление левого айтема из корзины")
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void deleteItemFailTest(UserIdType userIdType, String userId) throws Exception {
        carterHttpHelper.cleanCart(userIdType, userId);
        CartListViewModel cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Long listId = cartList.getId();
        Long itemId = carterHttpHelper.postItem(userIdType, userId, listId, OFFER, RESULT_MATCHER);
        carterHttpHelper.deleteItem(userIdType, userId, listId, itemId + 1, RESULT_MATCHER_NOT_FOUND);
        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        ItemOfferViewModel cartItem = cartList.getItems().iterator().next();
        assertEquals((long) itemId, cartItem.getId());
    }

    @DisplayName("PUT /item: изменение количества левого айтема в корзине")
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void putItemFailTest(UserIdType userIdType, String userId) throws Exception {
        carterHttpHelper.cleanCart(userIdType, userId);
        CartListViewModel cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Long listId = cartList.getId();
        Long itemId = carterHttpHelper.postItem(userIdType, userId, listId, OFFER, RESULT_MATCHER);
        carterHttpHelper.putItem(userIdType, userId, listId, itemId + 1, NEW_COUNT, RESULT_MATCHER_NOT_FOUND);
        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        ItemOfferViewModel cartItem = cartList.getItems().iterator().next();
        assertEquals((long) itemId, cartItem.getId());
        assertEquals(COUNT, cartItem.getCount());
        // Проверяем, что PUT не затер regionId
        assertEquals(RUSSIA_REGION_ID, cartItem.getRegionId());
    }

    @DisplayName("PUT /item: изменение количества айтема в корзине на ноль")
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void putItemFailZeroCountTest(UserIdType userIdType, String userId) throws Exception {
        carterHttpHelper.cleanCart(userIdType, userId);
        CartListViewModel cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Long listId = cartList.getId();
        Long itemId = carterHttpHelper.postItem(userIdType, userId, listId, OFFER, RESULT_MATCHER);
        carterHttpHelper.putItem(userIdType, userId, listId, itemId, 0, RESULT_MATCHER_BAD_REQUEST);
        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        ItemOfferViewModel cartItem = cartList.getItems().iterator().next();
        assertEquals((long) itemId, cartItem.getId());
        assertEquals(COUNT, cartItem.getCount());
    }

    @DisplayName("POST /item: добавление в корзину айтема без дескрипшна")
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void postItemWithoutDescriptionTest(UserIdType userIdType, String userId) throws Exception {
        carterHttpHelper.cleanCart(userIdType, userId);
        CartListViewModel cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Long listId = cartList.getId();
        ItemOffer offerWithNullDescription = generateItem("test");
        offerWithNullDescription.setDesc(null);
        Long itemId = carterHttpHelper.postItem(userIdType, userId, listId, offerWithNullDescription, RESULT_MATCHER);
        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        ItemOfferViewModel cartItem = cartList.getItems().iterator().next();
        assertEquals((long) itemId, cartItem.getId());
        assertNull(cartItem.getDesc());
    }

    @DisplayName("POST /item: добавление айтема без каунта в корзину") //275
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void postItemWithoutCountTest(UserIdType userIdType, String userId) throws Exception {
        carterHttpHelper.cleanCart(userIdType, userId);
        CartListViewModel cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Long listId = cartList.getId();
        ItemOffer offerWithoutCount = generateItem("test");
        offerWithoutCount.setCount(null);
        Long itemId = carterHttpHelper.postItem(userIdType, userId, listId, offerWithoutCount, RESULT_MATCHER);
        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        ItemOfferViewModel cartItem = cartList.getItems().iterator().next();
        assertEquals((long) itemId, cartItem.getId());
        assertEquals(1, (int) cartItem.getCount());
    }

    @DisplayName("POST /item: добавление айтема с каунтом превышающим лимит в корзину")
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void postItemWithCountOverLimitTest(UserIdType userIdType, String userId) throws Exception {
        carterHttpHelper.cleanCart(userIdType, userId);
        CartListViewModel cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Long listId = cartList.getId();
        ItemOffer offerWithoutOverLimitCount = generateItem("test");
        offerWithoutOverLimitCount.setCount(501);
        Long itemId = carterHttpHelper.postItem(userIdType, userId, listId, offerWithoutOverLimitCount, RESULT_MATCHER);
        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        ItemOfferViewModel cartItem = cartList.getItems().iterator().next();
        assertEquals((long) itemId, cartItem.getId());
    }

    @DisplayName("POST /item: добавление уже добавленного айтема в корзину")
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void postDoubleItemTest(UserIdType userIdType, String userId) throws Exception {
        carterHttpHelper.cleanCart(userIdType, userId);
        CartListViewModel cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Long listId = cartList.getId();
        Long itemId = carterHttpHelper.postItem(userIdType, userId, listId, OFFER, RESULT_MATCHER);
        assertEquals(carterHttpHelper.postItem(userIdType, userId, listId, OFFER, RESULT_MATCHER), itemId);
        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        ItemOfferViewModel cartItem = cartList.getItems().iterator().next();
        assertEquals((long) itemId, cartItem.getId());
    }

    @DisplayName("POST /item: добавление афйтема с другим objId но тем же msku")
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void postDoubleItemBuMskuTest(UserIdType userIdType, String userId) throws Exception {
        carterHttpHelper.cleanCart(userIdType, userId);
        CartListViewModel cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Long listId = cartList.getId();

        Long itemId = carterHttpHelper.postItem(userIdType, userId, listId, OFFER, RESULT_MATCHER);

        ItemOffer anotherOffer = generateItem("test2");
        anotherOffer.setMsku(OFFER.getMsku());

        Long idAfterId = carterHttpHelper.postItem(userIdType, userId, listId, anotherOffer, RESULT_MATCHER);

        assertNotEquals(idAfterId, itemId);
        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        ItemOfferViewModel cartItem = cartList.getItems().iterator().next();
        assertEquals((long) idAfterId, cartItem.getId());
    }

    @DisplayName("GET /search: поиск устройств/пользователей бросивших корзины")
    @Disabled //не работает по непонятным причинам
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void getSearchesTest(UserIdType userIdType, String userId) throws Exception {
        carterHttpHelper.cleanCart(userIdType, userId);
        Long from = 0L;
        Long to = new Date().getTime();
        carterHttpHelper.getSearchOwners(from, to, RESULT_MATCHER);
        carterHttpHelper.getSearch(from, to, RESULT_MATCHER);
    }


    @DisplayName("POST /item: добавление айтема в корзину с serviceItem")
    @Test
    public void serviceInfoCreateAndSaveInCartItemTest() throws Exception {
        UserIdType userIdType = UserIdType.UID;
        String userId = USER_ID;
        ItemOffer offer = generateItem(Color.BLUE, "test");

        carterHttpHelper.cleanCart(userIdType, userId);
        CartListViewModel cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Long listId = cartList.getId();

        offer.setServices(Arrays.asList(new ServiceInfo(123L), new ServiceInfo(456L)));

        Long itemId = carterHttpHelper.postItem(userIdType, userId, listId, offer, RESULT_MATCHER);
        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        ItemOfferViewModel cartItem = cartList.getItems().iterator().next();
        assertThat("Has different property values",
                CartConverter.convert(cartItem),
                SamePropertyValuesAs.samePropertyValuesAs(offer, "id", "createTime", "cartFee", "price"));

        assertEquals((long) itemId, cartItem.getId());
    }

    @DisplayName("POST /items: добавление нескольких айтемов в корзину")
    @Test
    public void serviceInfoCreateAndSaveInCartItemsTest() throws Exception {
        UserIdType userIdType = UserIdType.UID;
        String userId = USER_ID;
        carterHttpHelper.cleanCart(userIdType, userId);
        CartListViewModel cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Long listId = cartList.getId();
        var item1 = generateItem("test1");
        item1.setServices(Collections.singletonList(new ServiceInfo(123L)));
        String offerString1 = testSerializationService.serializeCarterObject(item1);
        var item2 = generateItem("test2");
        item2.setServices(Collections.singletonList(new ServiceInfo(456L)));
        String offerString2 = testSerializationService.serializeCarterObject(item2);
        List<String> items = Arrays.asList(offerString1, offerString2);
        carterHttpHelper.postItems(userIdType, userId, listId, items, RESULT_MATCHER);
        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Collection<ItemOfferViewModel> cartItems = cartList.getItems();
        assertEquals(2, cartItems.size());
        assertTrue(cartItems.stream().anyMatch(it -> it.getServices().get(0).getServiceId() == 123L));
        assertTrue(cartItems.stream().anyMatch(it -> it.getServices().get(0).getServiceId() == 456L));
    }

    @Test
    public void updateServicesTest() throws Exception {
        UserIdType userIdType = UserIdType.UID;
        String userId = USER_ID;
        carterHttpHelper.cleanCart(userIdType, userId);
        CartListViewModel cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Long listId = cartList.getId();
        var item1 = generateItem("test1");
        item1.setServices(Collections.singletonList(new ServiceInfo(12L)));
        String offerString1 = testSerializationService.serializeCarterObject(item1);
        var item2 = generateItem("test2");
        item2.setServices(Collections.singletonList(new ServiceInfo(34L)));
        String offerString2 = testSerializationService.serializeCarterObject(item2);
        List<String> items = Arrays.asList(offerString1, offerString2);
        carterHttpHelper.postItems(userIdType, userId, listId, items, RESULT_MATCHER);
        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);

        List<ItemOfferViewModel> cartListItems = cartList.getItems();
        assertEquals(1, cartListItems.get(0).getServices().size());
        assertEquals(1, cartListItems.get(1).getServices().size());

        var updateItem1 = cartListItems.get(0);
        updateItem1.setFieldsToChange(Collections.singleton(ItemField.SERVICES));
        updateItem1.setServices(Collections.singletonList(new ServiceInfo(56L)));
        String updateOfferString1 = testSerializationService.serializeCarterObject(updateItem1);
        var updateItem2 = cartListItems.get(1);
        updateItem2.setFieldsToChange(Collections.singleton(ItemField.SERVICES));
        updateItem2.setServices(Collections.singletonList(new ServiceInfo(78L)));
        String updateOfferString2 = testSerializationService.serializeCarterObject(updateItem2);
        List<String> updateItems = Arrays.asList(updateOfferString1, updateOfferString2);

        carterHttpHelper.updateItems(userIdType, userId, listId, updateItems, RESULT_MATCHER);

        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);

        List<ItemOfferViewModel> updatedCartListItems = cartList.getItems();
        assertEquals(1, updatedCartListItems.get(0).getServices().size());
        assertEquals(1, updatedCartListItems.get(1).getServices().size());

        assertTrue(updatedCartListItems.stream().anyMatch(it -> it.getServices().get(0).getServiceId() == 56));
        assertTrue(updatedCartListItems.stream().anyMatch(it -> it.getServices().get(0).getServiceId() == 78));
    }

    @Test
    public void updateCartWithServicesTest() throws Exception {
        UserIdType userIdType = UserIdType.UID;
        String userId = USER_ID;
        carterHttpHelper.cleanCart(userIdType, userId);
        CartListViewModel cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);
        Long listId = cartList.getId();
        var item1 = generateItem("test1");
        item1.setServices(Collections.singletonList(new ServiceInfo(12L)));
        String offerString1 = testSerializationService.serializeCarterObject(item1);
        var item2 = generateItem("test2");
        item2.setServices(Collections.singletonList(new ServiceInfo(34L)));
        String offerString2 = testSerializationService.serializeCarterObject(item2);
        List<String> items = Arrays.asList(offerString1, offerString2);
        carterHttpHelper.postItems(userIdType, userId, listId, items, RESULT_MATCHER);
        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);

        List<ItemOfferViewModel> cartListItems = cartList.getItems();
        assertEquals(1, cartListItems.get(0).getServices().size());
        assertEquals(1, cartListItems.get(1).getServices().size());

        var updateItem1 = cartListItems.get(0);
        updateItem1.setServices(Collections.singletonList(new ServiceInfo(56L)));
        String updateOfferString1 = testSerializationService.serializeCarterObject(updateItem1);
        var updateItem2 = cartListItems.get(1);
        updateItem2.setServices(Collections.singletonList(new ServiceInfo(78L)));
        String updateOfferString2 = testSerializationService.serializeCarterObject(updateItem2);
        List<String> updateItems = Arrays.asList(updateOfferString1, updateOfferString2);

        carterHttpHelper.updateCart(userIdType, userId, listId, updateItems, RESULT_MATCHER);

        cartList = carterHttpHelper.getList(userIdType, userId, RESULT_MATCHER);

        List<ItemOfferViewModel> updatedCartListItems = cartList.getItems();
        assertEquals(1, updatedCartListItems.get(0).getServices().size());
        assertEquals(1, updatedCartListItems.get(1).getServices().size());

        assertTrue(updatedCartListItems.stream().anyMatch(it -> it.getServices().get(0).getServiceId() == 56));
        assertTrue(updatedCartListItems.stream().anyMatch(it -> it.getServices().get(0).getServiceId() == 78));
    }


}
