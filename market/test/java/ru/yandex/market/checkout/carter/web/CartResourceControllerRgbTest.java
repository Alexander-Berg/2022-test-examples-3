package ru.yandex.market.checkout.carter.web;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.client.Carter;
import ru.yandex.market.checkout.carter.model.AddItemRequest;
import ru.yandex.market.checkout.carter.model.CartItem;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.UserIdType;
import ru.yandex.market.checkout.carter.report.ReportGeneratorParameters;
import ru.yandex.market.checkout.carter.report.ReportGeneratorParameters.ReportOffer;
import ru.yandex.market.checkout.carter.report.ReportMockConfigurer;
import ru.yandex.market.checkout.carter.storage.StorageCartService;
import ru.yandex.market.checkout.carter.util.converter.CartConverter;
import ru.yandex.market.checkout.carter.utils.CarterTestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.model.UserIdType.UUID;
import static ru.yandex.market.checkout.carter.model.UserIdType.YANDEXUID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.getSingleItemFromCart;

/**
 * Created by asafev on 10/01/2018.
 */
public class CartResourceControllerRgbTest extends CarterMockedDbTestBase {

    private static final String USER_ID = "777";
    private static final String UUID_ID = "12345";
    private static final String YANDEXUID_ID = "54321";
    @Autowired
    protected WireMockServer reportMock;
    @Autowired
    private Carter carterClient;
    @Autowired
    private ReportMockConfigurer reportMockConfigurer;

    @AfterEach
    public void clean() {
        reportMockConfigurer.resetMock();
    }

    @Test
    public void testCreateAndGetRgbItems() {
        final ItemOffer item2 = generateItem("blue");
        final ItemOffer item3 = generateItem("red");

        carterClient.addItem(USER_ID, UID, StorageCartService.LIST_BASKET_ID, Color.BLUE,
                CartConverter.convert(item2), null);
        carterClient.addItem(USER_ID, UID, StorageCartService.LIST_BASKET_ID, Color.RED, CartConverter.convert(item3),
                null);

        reportMockConfigurer.mockReportOk();
        ItemOfferViewModel createdItem = getSingleItemFromCart(carterClient.getCart(USER_ID, UID, Color.BLUE));
        CarterTestUtils.assertItemsEqual(item2, CartConverter.convert(createdItem));

        reportMockConfigurer.mockReportOk();
        createdItem = getSingleItemFromCart(carterClient.getCart(USER_ID, UID, Color.RED));
        CarterTestUtils.assertItemsEqual(item3, CartConverter.convert(createdItem));
    }

    @Test
    public void testCreateAndDeleteRgbItem() {
        final ItemOffer itemBlue = generateItem("conflicting");
        itemBlue.setMsku(13L);
        final ItemOffer itemRed = generateItem("conflicting");

        carterClient.addItem(USER_ID, UID, StorageCartService.LIST_BASKET_ID, Color.BLUE,
                CartConverter.convert(itemBlue), null);
        carterClient.addItem(USER_ID, UID, StorageCartService.LIST_BASKET_ID, Color.RED,
                CartConverter.convert(itemRed), null);

        ItemOfferViewModel createdItem;
        // сначала проверяем, что у нас ровно один элемент в синем списке
        createdItem = getSingleItemFromCart(carterClient.getCart(USER_ID, UID, Color.BLUE));
        CarterTestUtils.assertItemsEqual(itemBlue, CartConverter.convert(createdItem));

        carterClient.removeItem(USER_ID, UID, StorageCartService.LIST_BASKET_ID, Color.BLUE, createdItem.getId(), null);

        reportMockConfigurer.mockReportOk();
        CartViewModel blueCartAfterDeletion = carterClient.getCart(USER_ID, UID, Color.BLUE);
        CarterTestUtils.assertCartIsEmpty(CartConverter.convert(blueCartAfterDeletion));

        createdItem = getSingleItemFromCart(carterClient.getCart(USER_ID, UID, Color.RED));
        CarterTestUtils.assertItemsEqual(itemRed, CartConverter.convert(createdItem));
    }

    @Test
    public void shouldNotReplaceOfferByMsku() {
        ItemOffer itemBlue = generateItem("asdasd");
        itemBlue.setMsku(123L);

        carterClient.addItem(USER_ID, UID, StorageCartService.LIST_BASKET_ID, Color.BLUE,
                CartConverter.convert(itemBlue), null);

        ReportGeneratorParameters reportGeneratorParameters = new ReportGeneratorParameters()
                .addReportOffer(123L, "asdasd");

        reportMockConfigurer.mockReportOk(reportGeneratorParameters);
        CartViewModel blueCartAfterReplace = carterClient.getCart(USER_ID, UID, Color.BLUE);

        Assertions.assertEquals(itemBlue.getObjId(),
                CartConverter.convert(blueCartAfterReplace).getBasketList().getItems().iterator().next().getObjId());
    }

    private void assertHasAdultTrue() {
        assertThat(
                reportMock.getAllServeEvents()
                        .stream()
                        .map(e -> e.getRequest().getUrl())
                        .collect(Collectors.toList()),
                everyItem(containsString("adult=1"))
        );
    }

    @Nonnull
    private ItemOffer getItemByOfferId(String offerId, Collection<CartItem> itemsAfterReplace) {
        return itemsAfterReplace.stream()
                .map(i -> (ItemOffer) i)
                .filter(i -> offerId.equals(i.getObjId()))
                .findAny()
                .orElseThrow(() -> new RuntimeException("No item " + offerId));
    }

    @Test
    public void shouldNotReplaceOfferByMskuAndWarehouseIfNotMainWarehouse() {
        ItemOffer itemBlueA = generateItem("A");
        itemBlueA.setMsku(121L);

        ItemOffer itemBlueB = generateItem("B");
        itemBlueB.setMsku(122L);

        carterClient.addItem(USER_ID, UID, StorageCartService.LIST_BASKET_ID, Color.BLUE,
                CartConverter.convert(itemBlueA), null);
        carterClient.addItem(USER_ID, UID, StorageCartService.LIST_BASKET_ID, Color.BLUE,
                CartConverter.convert(itemBlueB), null);

        ReportGeneratorParameters reportGeneratorParameters = new ReportGeneratorParameters()
                .addReportOffer(121L, new ReportOffer(itemBlueA.getObjId(), 146))
                .addReportOffer(122L, new ReportOffer(itemBlueB.getObjId(), 146));

        reportMockConfigurer.mockReportOk(reportGeneratorParameters);
        CartViewModel blueCartAfterReplace = carterClient.getCart(USER_ID, UID, Color.BLUE);

        Collection<CartItem> itemsAfterReplace = CartConverter.convert(blueCartAfterReplace).getBasketList().getItems();
        assertThat(itemsAfterReplace, hasSize(2));
        Assertions.assertTrue(itemsAfterReplace.stream().anyMatch(i -> itemBlueA.getObjId().equals(i.getObjId())));
        Assertions.assertTrue(itemsAfterReplace.stream().anyMatch(i -> itemBlueB.getObjId().equals(i.getObjId())));
    }

    @Test
    public void shouldNotReplaceOfferByMskuAndWarehouseForWhite() {
        ItemOffer whiteItem = generateItem("A");
        whiteItem.setMsku(121L);

        carterClient.addItem(
                AddItemRequest.builder()
                        .withCartItem(CartConverter.convert(whiteItem))
                        .withUserAnyId(USER_ID)
                        .withColor(Color.WHITE)
                        .withUserIdType(UID)
                        .build()
        );

        CartViewModel whiteCart = carterClient.getCart(USER_ID, UID, Color.WHITE);

        reportMockConfigurer.verifyReportMockNotCalled();
        Collection<CartItem> items = CartConverter.convert(whiteCart).getBasketList().getItems();
        assertThat(items, hasSize(1));
        Assertions.assertTrue(items.stream().anyMatch(i -> whiteItem.getObjId().equals(i.getObjId())));
    }

    @Test
    public void shouldNotConflictedOnWhiteAndBlueCart() {
        final ItemOffer blueItem = generateItem("blue");
        final ItemOffer whiteItem = generateItem("white");

        carterClient.addItem(
                AddItemRequest.builder()
                        .withCartItem(CartConverter.convert(blueItem))
                        .withUserAnyId(USER_ID)
                        .withColor(Color.BLUE)
                        .withUserIdType(UID)
                        .build()
        );

        carterClient.addItem(
                AddItemRequest.builder()
                        .withCartItem(CartConverter.convert(whiteItem))
                        .withUserAnyId(USER_ID)
                        .withColor(Color.WHITE)
                        .withUserIdType(UID)
                        .build()
        );

        reportMockConfigurer.mockReportOk();
        ItemOffer createdItem = CartConverter.convert(getSingleItemFromCart(carterClient.getCart(USER_ID, UID,
                Color.BLUE)));
        CarterTestUtils.assertItemsEqual(blueItem, createdItem);

        createdItem = CartConverter.convert(getSingleItemFromCart(carterClient.getCart(USER_ID, UID, Color.WHITE)));
        CarterTestUtils.assertItemsEqual(whiteItem, createdItem);
    }


    @Test
    public void shouldNotReplaceOfferByMskuAndWarehouseIfWarehouseIsDefault() {
        ItemOffer itemBlueA = generateItem("A");
        itemBlueA.setMsku(121L);

        ItemOffer itemBlueB = generateItem("B");
        itemBlueB.setMsku(122L);

        carterClient.addItem(USER_ID, UID, StorageCartService.LIST_BASKET_ID, Color.BLUE,
                CartConverter.convert(itemBlueA), null);
        carterClient.addItem(USER_ID, UID, StorageCartService.LIST_BASKET_ID, Color.BLUE,
                CartConverter.convert(itemBlueB), null);

        ReportGeneratorParameters reportGeneratorParameters = new ReportGeneratorParameters()
                .addReportOffer(121L, new ReportOffer(itemBlueA.getObjId(), 1))
                .addReportOffer(122L, new ReportOffer(itemBlueB.getObjId(), 145));

        reportMockConfigurer.mockReportOk(reportGeneratorParameters);
        CartViewModel blueCartAfterReplace = carterClient.getCart(USER_ID, UID, Color.BLUE);

        Collection<CartItem> itemsAfterReplace = CartConverter.convert(blueCartAfterReplace).getBasketList().getItems();
        assertThat(itemsAfterReplace, hasSize(2));
        Assertions.assertTrue(itemsAfterReplace.stream().anyMatch(i -> itemBlueA.getObjId().equals(i.getObjId())));
        Assertions.assertTrue(itemsAfterReplace.stream().anyMatch(i -> itemBlueB.getObjId().equals(i.getObjId())));
    }

    @Test
    public void testBulkCreateRgbItems() {
        final CartList blueItems = CarterTestUtils.generateCartList("blue1", "blue2", "blue3");
        final CartList redItems = CarterTestUtils.generateCartList("red1", "red2");

        carterClient.addItems(USER_ID, UID, StorageCartService.LIST_BASKET_ID, Color.BLUE,
                CartConverter.convert(blueItems), null);
        carterClient.addItems(USER_ID, UID, StorageCartService.LIST_BASKET_ID, Color.RED,
                CartConverter.convert(redItems), null);

        Cart blueCart = CartConverter.convert(carterClient.getCart(USER_ID, UID, Color.BLUE));
        assertCart(blueCart, blueItems);
        Cart redCart = CartConverter.convert(carterClient.getCart(USER_ID, UID, Color.RED));
        assertCart(redCart, redItems);
    }

    @Test
    public void testBulkDeleteRgbItems() {
        // Подготовим корзинки. Пусть двойное тестирование, но и ладно.
        final CartList blueItems = CarterTestUtils.generateCartList("blue1", "blue2", "blue3");
        final CartList redItems = CarterTestUtils.generateCartList("red1", "red2");

        carterClient.addItems(USER_ID, UID, StorageCartService.LIST_BASKET_ID, Color.BLUE,
                CartConverter.convert(blueItems), null);
        carterClient.addItems(USER_ID, UID, StorageCartService.LIST_BASKET_ID, Color.RED,
                CartConverter.convert(redItems), null);

        CartViewModel blueCart = carterClient.getCart(USER_ID, UID, Color.BLUE);
        Collection<CartItem> items = CartConverter.convert(blueCart).getBasketList().getItems();
        assertThat(items, hasSize(3));
        Set<Long> originalBlueIds = items.stream()
                .map(CartItem::getId)
                .collect(Collectors.toSet());
        List<Long> deleteIds = items.stream()
                .skip(1)
                .map(CartItem::getId)
                .collect(Collectors.toList());
        carterClient.removeItems(USER_ID, UID, StorageCartService.LIST_BASKET_ID, Color.BLUE, deleteIds, null);
        Set<Long> deletedIdsSet = new HashSet<>(deleteIds);
        Cart updatedBlueCart = CartConverter.convert(carterClient.getCart(USER_ID, UID, Color.BLUE));
        assertThat(updatedBlueCart, notNullValue());
        assertThat(updatedBlueCart.getBasketList(), notNullValue());
        Collection<CartItem> survivedBlueItems = updatedBlueCart.getBasketList().getItems();
        Set<Long> survivedBlueIds = survivedBlueItems.stream()
                .map(CartItem::getId)
                .collect(Collectors.toSet());
        // убеждаемся, что средивыживших нет тех, кого попросили помереть
        assertThat(CollectionUtils.intersection(deletedIdsSet, survivedBlueIds), empty());
        // убеждаемся, что умерли только те, кого попросили
        Assertions.assertEquals(
                new HashSet<>(deletedIdsSet),
                new HashSet<>(CollectionUtils.disjunction(originalBlueIds, survivedBlueIds)),
                "Deleted different than expected"
        );
        Cart redCart = CartConverter.convert(carterClient.getCart(USER_ID, UID, Color.RED));
        assertThat(redCart.getBasketList().getItems(), hasSize(redItems.getItems().size()));
    }

    @Test
    public void testMergeRgbItemsWithColorMismatch() {
        final CartList blueItems = CarterTestUtils.generateCartList("blue1", "blue2", "blue3");
        carterClient.addItems(UUID_ID, UUID, StorageCartService.LIST_BASKET_ID, Color.BLUE,
                CartConverter.convert(blueItems), null);

        carterClient.mergeItems(UUID_ID, UUID, USER_ID, UID, Color.GREEN);
        Cart greenCart = CartConverter.convert(carterClient.getCart(USER_ID, UID, Color.GREEN));

        assertThat("Green cart should be empty", greenCart.getBasketList().getItems(), empty());
    }

    @Deprecated
    @Disabled
    @Test
    public void testMergeRgbItemsWithDifferentColors() {
        testMerge(UUID_ID, UUID);
    }

    @Deprecated
    @Disabled
    @Test
    public void testMergeYandexUidRgbItemsWithDifferentColors() {
        testMerge(YANDEXUID_ID, YANDEXUID);
    }

    private void testMerge(String fromId, UserIdType fromIdType) {
        final CartList blueItems = CarterTestUtils.generateCartList("blue1", "blue2", "blue3");
        final CartList greenItems = CarterTestUtils.generateCartList("green1", "green2");
        carterClient.addItems(fromId, fromIdType, StorageCartService.LIST_BASKET_ID, Color.BLUE,
                CartConverter.convert(blueItems), null);
        carterClient.addItems(fromId, fromIdType, StorageCartService.LIST_BASKET_ID, Color.GREEN,
                CartConverter.convert(greenItems), null);

        carterClient.mergeItems(fromId, fromIdType, USER_ID, UID, Color.BLUE);
        Cart greenCart = CartConverter.convert(carterClient.getCart(USER_ID, UID, Color.BLUE));

        assertCart(greenCart, blueItems);
    }

    @Test
    public void testUpdateRgbItems() {
        // Create two items - blue and green
        carterClient.addItem(USER_ID, UID, StorageCartService.LIST_BASKET_ID, Color.BLUE,
                CartConverter.convert(generateItem("blue")), null);
        carterClient.addItem(USER_ID, UID, StorageCartService.LIST_BASKET_ID, Color.WHITE,
                CartConverter.convert(generateItem("white")), null);
        Cart result = CartConverter.convert(carterClient.getCart(USER_ID, UID, Color.BLUE));
        ItemOffer item1 = getSingleItemFromCart(result);
        assertThat("Initial value", item1.getCount(), equalTo(1));

        // Update blue item
        carterClient.updateItem(USER_ID, UID, StorageCartService.LIST_BASKET_ID, Color.BLUE, item1.getId(), 10, null);

        // Check blue item updated, green - not
        result = CartConverter.convert(carterClient.getCart(USER_ID, UID, Color.BLUE));
        assertThat("Count should not change", getSingleItemFromCart(result).getCount(), equalTo(10));
    }

    @Test
    public void shouldNotReplaceByDefault() {
        ItemOffer itemBefore = generateItem("asdasd");
        itemBefore.setMsku(123L);
        carterClient.addItem(USER_ID, UID, StorageCartService.LIST_BASKET_ID, Color.BLUE,
                CartConverter.convert(itemBefore), null);

        ReportGeneratorParameters reportGeneratorParameters = new ReportGeneratorParameters()
                .addReportOffer(123L, "defdef");

        reportMockConfigurer.mockReportOk(reportGeneratorParameters);
        Cart blueCartAfter = CartConverter.convert(carterClient.getCart(USER_ID, UID, 321L, Color.BLUE));

        Collection<CartItem> cartItems = blueCartAfter.getBasketList().getItems();
        ItemOffer itemAfter = findByMsku(cartItems, itemBefore.getMsku());

        assertEquals(itemBefore, itemAfter);
    }

    @Test
    public void shouldNotConflictOnYandexGoAndWhiteCart() {
        final ItemOffer blueItem = generateItem("yandex-go");
        final ItemOffer whiteItem = generateItem("white");

        carterClient.addItem(
                AddItemRequest.builder()
                        .withCartItem(CartConverter.convert(blueItem))
                        .withUserAnyId(USER_ID)
                        .withColor(Color.YANDEX_GO)
                        .withUserIdType(UID)
                        .build()
        );

        carterClient.addItem(
                AddItemRequest.builder()
                        .withCartItem(CartConverter.convert(whiteItem))
                        .withUserAnyId(USER_ID)
                        .withColor(Color.WHITE)
                        .withUserIdType(UID)
                        .build()
        );

        reportMockConfigurer.mockReportOk();
        ItemOffer createdItem = CartConverter.convert(getSingleItemFromCart(carterClient.getCart(USER_ID, UID,
                Color.YANDEX_GO)));
        CarterTestUtils.assertItemsEqual(blueItem, createdItem);

        createdItem = CartConverter.convert(getSingleItemFromCart(carterClient.getCart(USER_ID, UID, Color.WHITE)));
        CarterTestUtils.assertItemsEqual(whiteItem, createdItem);
    }

    @Test
    public void testMultipleColorsBasketRequest() {
        var item1 = generateItem("offer1", 2);
        var item2 = generateItem("offer2", 3);
        carterClient.addItem(AddItemRequest.builder()
                .withUserAnyId(USER_ID)
                .withUserIdType(UID)
                .withCartItem(CartConverter.convert(item1))
                .withColor(Color.BLUE)
                .build());
        carterClient.addItem(AddItemRequest.builder()
                .withUserAnyId(USER_ID)
                .withUserIdType(UID)
                .withCartItem(CartConverter.convert(item2))
                .withColor(Color.FOODTECH)
                .build());

        CartViewModel cart = carterClient.getCart(USER_ID, UID, null, List.of(Color.BLUE, Color.FOODTECH), false);

        assertThat(cart.getLists(), hasSize(1));
        var basketList = cart.getLists().get(0);
        assertEquals(CartList.Type.BASKET, basketList.getType());
        assertThat(basketList.getItems(), hasSize(2));
        var actualItem1 = basketList
                .getItems()
                .stream()
                .filter(x -> x.getObjId().equals(item1.getObjId()))
                .findFirst()
                .orElseThrow();
        var actualItem2 = basketList
                .getItems()
                .stream()
                .filter(x -> x.getObjId().equals(item2.getObjId()))
                .findFirst()
                .orElseThrow();
        assertEquals(item1.getMsku(), actualItem1.getMsku());
        assertEquals(item2.getMsku(), actualItem2.getMsku());
        var item1Total = item1.getPrice().multiply(new BigDecimal(item1.getCount()));
        var item2Total = item2.getPrice().multiply(new BigDecimal(item2.getCount()));
        assertEquals(item1Total.add(item2Total), basketList.getTotalPrice());
    }

    private List<String> cartListToIds(List<CartList> blueItems) {
        return blueItems.stream()
                .flatMap(i -> i.getItems().stream())
                .map(CartItem::getObjId)
                .collect(Collectors.toList());
    }

    private void assertCart(Cart cart, CartList... expectedItems) {
        List<String> expectedItemIds = cartListToIds(Arrays.asList(expectedItems));
        List<String> realItemIds = cartListToIds(Collections.singletonList(cart.getBasketList()));
        assertThat(
                "Cart items count is wrong",
                realItemIds,
                hasSize(expectedItemIds.size())
        );
        assertThat(
                "Cart items has wrong ids",
                realItemIds,
                containsInAnyOrder(expectedItemIds.toArray())
        );
    }

    private ItemOffer findByMsku(Collection<CartItem> cartItems, Long msku) {
        Optional<ItemOffer> optional = cartItems.stream().map(item -> (ItemOffer) item).
                filter(item -> item.getMsku().equals(msku)).findFirst();

        assertTrue(optional.isPresent());
        return optional.get();
    }
}
