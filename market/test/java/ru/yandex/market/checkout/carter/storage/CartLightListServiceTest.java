package ru.yandex.market.checkout.carter.storage;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.report.model.OfferInfoWithPromo;
import ru.yandex.market.checkout.carter.storage.dao.ydb.YdbDao;
import ru.yandex.market.checkout.carter.web.Cart;
import ru.yandex.market.checkout.carter.web.UserContext;
import ru.yandex.market.checkout.common.report.ColoredGenericMarketReportService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.carter.model.Color.WHITE;
import static ru.yandex.market.checkout.carter.model.UserIdType.YANDEXUID;

@MockBean(ColoredGenericMarketReportService.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CartLightListServiceTest extends CarterMockedDbTestBase {
    @Autowired
    private CartLightListService cartLightListService;
    @Autowired
    private YdbDao ydbDao;
    @Autowired
    private ColoredGenericMarketReportService reportService;


    @Test
    @DisplayName("POSITIVE: Успешное получение корзины с одним айтемом")
    void singleItemTest() {
        String userId = RandomStringUtils.random(10);
        String anotherUserId = RandomStringUtils.random(10);

        createCartList(userId, 1, 0, 0);
        createCartList(anotherUserId, 1, 0, 0);

        assertCartsEquals(userId, true);
    }

    @Test
    @DisplayName("POSITIVE: Успешное получение корзины с несколькими айтемами")
    void multipleItemsTest() {
        String userId = RandomStringUtils.random(10);
        String anotherUserId = RandomStringUtils.random(10);

        createCartList(userId, 3, 0, 0);
        createCartList(anotherUserId, 1, 0, 0);

        assertCartsEquals(userId, true);
    }

    @Test
    @DisplayName("POSITIVE: Успешное получение корзины с несколькими айтемами и параметрами")
    void multipleItemsWithParamsTest() {
        String userId = RandomStringUtils.randomAlphabetic(10);
        String anotherUserId = RandomStringUtils.random(10);

        createCartList(userId, 3, 0, 0);
        createCartList(anotherUserId, 1, 0, 0);

        assertCartsEquals(userId, true);
    }

    @Test
    @DisplayName("POSITIVE: Успешное получение корзины с несколькими айтемами, параметрами и промо")
    void multipleItemsWithParamsAndPromosTest() {
        String userId = RandomStringUtils.random(10);
        String anotherUserId = RandomStringUtils.random(10);

        createCartList(userId, 3, 3, 0);
        createCartList(anotherUserId, 1, 0, 0);

        assertCartsEquals(userId, true);
    }

    @Test
    @DisplayName("POSITIVE: У пользователя нет корзины")
    void nullCartTest() {
        String userId = RandomStringUtils.random(10);
        String anotherUserId = RandomStringUtils.random(10);

        Cart cart = cartLightListService.getCartListByOwner(YANDEXUID, userId, WHITE, false, false);
        createCartList(anotherUserId, 1, 0, 0);

        assertNotNull(cart.getBasketList());
        assertTrue(cart.getBasketList().getItems().isEmpty());
    }

    @Test
    @DisplayName("POSITIVE: У пользователя пустая корзина")
    void emptyCartTest() {
        String userId = RandomStringUtils.random(10);
        String anotherUserId = RandomStringUtils.random(10);

        createCartList(userId, 0, 0, 0);
        createCartList(anotherUserId, 1, 0, 0);

        assertCartsEquals(userId, false);
    }

    @Test
    @DisplayName("Показываем все элементы корзины, даже с пометкой adult = true")
    void allAgesOffers() {
        String userId = RandomStringUtils.random(10);
        int total = 10;
        int forAdult = 3;

        createCartList(userId, total, 0, forAdult);
        ydbDao.loadItemsWithUserAndColor(UserContext.of(WHITE, userId, YANDEXUID));

        Cart cart = cartLightListService.getCartListByOwner(YANDEXUID, userId, WHITE, true, false);
        cart.getBasketList();

        assertEquals(total, cart.getBasketList().getItems().size());
    }

    @Test
    @DisplayName("Не показываем то, что с пометкой adult = true")
    void withoutAdultOffers() {
        String userId = RandomStringUtils.random(10);
        int total = 10;
        int forAdult = 3;

        createCartList(userId, total, 0, forAdult);
        Cart cart = cartLightListService.getCartListByOwner(YANDEXUID, userId, WHITE, false, false);
        cart.getBasketList();

        assertEquals(total - forAdult, cart.getBasketList().getItems().size());
    }

    @Test
    @DisplayName("Получаем актуальные данные по офферам в корзине")
    void actualOffer() throws Exception {

        String userId = RandomStringUtils.randomNumeric(10);
        int total = 10;
        int forAdult = 3;

        createCartList(userId, total, 10, forAdult, true);

        List<ItemOffer> itemOffers = ydbDao.loadItemsWithUserAndColor(UserContext.of(WHITE, userId, YANDEXUID));
        // Mapping assertions
        itemOffers.forEach(item -> {
            assertNotNull(item.getActualizedObjId());
            assertNotNull(item.getActualizedPrice());
            assertTrue(CollectionUtils.isNotEmpty(item.getPromos()));
        });

        List<OfferInfoWithPromo> offerInfos = itemOffers.stream()
                .map(item -> new OfferInfoWithPromo(
                        item.getActualizedObjId(),
                        new BigDecimal(RandomUtils.nextInt(100, 1000)),
                        null,
                        new JSONObject()
                ))
                .collect(Collectors.toList());
        when(reportService.executeSearchAndParse(Mockito.any(), Mockito.any())).thenReturn(offerInfos);

        Cart cart = cartLightListService.getCartListByOwner(YANDEXUID, userId, WHITE, false, true);

        var items = cart.getBasketList().getItems();
        var offersMap = offerInfos.stream()
                .collect(Collectors.toMap(OfferInfoWithPromo::getOfferId, Function.identity()));

        items.forEach(item -> {
            assertEquals(item.getActualPrice(), offersMap.get(item.getActualizedObjId()).getPrice());
            assertTrue(CollectionUtils.isEmpty(item.getPromos()));
        });
    }

    @Test
    @DisplayName("Проверяем, что мы не падаем в режиме обновления, если promos - null и actualPromo - null")
    void nullPromo() throws Exception {
        String userId = RandomStringUtils.randomNumeric(10);
        int total = 10;
        int forAdult = 3;

        createCartList(userId, total, 10, forAdult, true, true);

        List<ItemOffer> itemOffers = ydbDao.loadItemsWithUserAndColor(UserContext.of(WHITE, userId, YANDEXUID));
        // Mapping assertions
        itemOffers.forEach(item -> {
            assertNull(item.getPromos());
        });

        List<OfferInfoWithPromo> offerInfos = itemOffers.stream()
                .map(item -> new OfferInfoWithPromo(
                        item.getActualizedObjId(),
                        new BigDecimal(RandomUtils.nextInt(100, 1000)),
                        null,
                        new JSONObject()
                ))
                .collect(Collectors.toList());
        when(reportService.executeSearchAndParse(Mockito.any(), Mockito.any())).thenReturn(offerInfos);

        cartLightListService.getCartListByOwner(YANDEXUID, userId, WHITE, false, true);
    }

    private void assertCartsEquals(String userId, boolean withTime) {
        UserContext userContext = UserContext.of(OwnerKey.of(WHITE, YANDEXUID, userId));
        List<CartList> lists = ydbDao.loadListsForUserContext(userContext);
        Cart cart = cartLightListService.getCartListByOwner(YANDEXUID, userId, WHITE, false, false);

        CartList expected = lists.get(0);
        CartList actual = cart.getBasketList();

        assertCartsEquals(expected, actual, withTime);
    }
}
