package ru.yandex.market.checkout.carter.web;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.client.Carter;
import ru.yandex.market.checkout.carter.model.AddItemRequest;
import ru.yandex.market.checkout.carter.model.CartItem;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.CartRequest;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.ItemPromo;
import ru.yandex.market.checkout.carter.model.OfferBenefit;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.model.ReplaceItemsRequest;
import ru.yandex.market.checkout.carter.model.UserIdType;
import ru.yandex.market.checkout.carter.report.OfferInfoReportService;
import ru.yandex.market.checkout.carter.report.ReportGeneratorParameters;
import ru.yandex.market.checkout.carter.report.ReportMockConfigurer;
import ru.yandex.market.checkout.carter.storage.StorageCartService;
import ru.yandex.market.checkout.carter.util.converter.CartConverter;
import ru.yandex.market.checkout.carter.utils.CarterTestUtils;
import ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.carter.model.Color.WHITE;
import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.storage.StorageCartService.LIST_BASKET_ID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.asType;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.assertCartIsEmpty;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.assertItemsEqual;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.cloneItemWith;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.createCartFor;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItemWith;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItemWithBundle;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.getSingleItemFromCart;
import static ru.yandex.market.checkout.carter.web.CarterWebParam.PARAM_USER_GROUP;

@MockBean(OfferInfoReportService.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CartResourceControllerTest extends CarterMockedDbTestBase {

    private static final int NEW_COUNT = 3;
    private static final int LIST_ID = -1;
    private static final String PROMO_KEY = "xMpCOKC554INzFCab3WE2w";
    private static final String OFFER = "some_offer";
    private static final String ANOTHER_OFFER = "another_offer";

    @Autowired
    private Carter carterClient;
    @Autowired
    private StorageCartService storageCartService;
    @Autowired
    private ReportMockConfigurer reportMockConfigurer;
    @Autowired
    private ReportMockConfigurer reportMockConfigurerWhite;

    private UserContext uidContext;
    private UserContext uuidContext;
    private UserContext yandexUuidContext;

    @BeforeEach
    public void setUp() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        uidContext = UserContext.of(OwnerKey.of(Color.BLUE, UID, "" + rnd.nextLong(1, Long.MAX_VALUE)));
        uuidContext = UserContext.of(OwnerKey.of(Color.BLUE, UserIdType.UUID, UUID.randomUUID().toString()));
        yandexUuidContext = UserContext.of(OwnerKey.of(
                Color.BLUE, UserIdType.YANDEXUID, UUID.randomUUID().toString()));
    }

    @AfterEach
    public void clear() {
        reportMockConfigurer.resetMock();
        reportMockConfigurerWhite.resetMock();
    }

    @Test
    public void shouldMergeUUIDCartToAuthorized() {
        CartViewModel cart = carterClient.getCart(uuidContext.getUserAnyId(), UserIdType.UUID, Color.BLUE);
        CartListViewModel list = cart.getLists().get(0);
        Long id = carterClient.addItem(
                uuidContext.getUserAnyId(),
                UserIdType.UUID,
                list.getId(),
                Color.BLUE,
                CartConverter.convert(generateItem(OFFER)),
                null
        );
        assertNotNull(id);

        CartViewModel actualCart = carterClient.getCart(uidContext.getUserAnyId(), UID, Color.BLUE);

        assertEquals(0, actualCart.getLists().get(0).getItems().size());

        assertTrue(
                carterClient.mergeItems(
                        uuidContext.getUserAnyId(),
                        UserIdType.UUID,
                        uidContext.getUserAnyId(),
                        UID,
                        Color.BLUE
                )
        );

        actualCart = carterClient.getCart(uidContext.getUserAnyId(), UID, Color.BLUE);
        assertEquals(1, actualCart.getLists().get(0).getItems().size());
    }

    @Test
    public void shouldMergeUUIDCartToUnauthorizedYandexUUID() {
        CartViewModel cart = carterClient.getCart(uuidContext.getUserAnyId(), UserIdType.UUID, Color.BLUE);
        CartListViewModel list = cart.getLists().get(0);
        Long id = carterClient.addItem(
                uuidContext.getUserAnyId(),
                UserIdType.UUID,
                list.getId(),
                Color.BLUE,
                CartConverter.convert(generateItem(OFFER)),
                null
        );
        assertNotNull(id);

        CartViewModel actualCart = carterClient.getCart(yandexUuidContext.getUserAnyId(), UserIdType.YANDEXUID,
                Color.BLUE);
        assertEquals(0, actualCart.getLists().get(0).getItems().size());

        assertTrue(
                carterClient.mergeItems(
                        uuidContext.getUserAnyId(),
                        UserIdType.UUID,
                        yandexUuidContext.getUserAnyId(),
                        UserIdType.YANDEXUID, Color.BLUE
                ));

        actualCart = carterClient.getCart(yandexUuidContext.getUserAnyId(), UserIdType.YANDEXUID, Color.BLUE);
        assertEquals(1, actualCart.getLists().get(0).getItems().size());
    }

    @Test
    public void shouldReturnFakeCartForNotExistingOwner() {
        String uuid = UUID.randomUUID().toString();
        CartViewModel cart = carterClient.getCart(uuid, UserIdType.UUID, Color.BLUE);

        assertEquals(cart.getLists().get(0).getId(), LIST_BASKET_ID);
        assertEquals(0, cart.getLists().get(0).getItems().size());
    }

    @Test
    public void shouldReturnFakeCartForExistingOwnerWithEmptyCart() {
        String uuid = UUID.randomUUID().toString();
        CartViewModel cart = carterClient.getCart(uuid, UserIdType.UUID, Color.BLUE);

        assertEquals(cart.getLists().get(0).getId(), LIST_BASKET_ID);
        assertEquals(0, cart.getLists().get(0).getItems().size());
    }

    @Test
    public void shouldReturnExistingCart() {
        String uuid = UUID.randomUUID().toString();
        ItemOffer expectedItem = generateItem(OFFER);
        carterClient.addItem(uuid, UserIdType.UUID, LIST_BASKET_ID, Color.BLUE, CartConverter.convert(expectedItem),
                null);

        Cart cart = CartConverter.convert(carterClient.getCart(uuid, UserIdType.UUID, Color.BLUE));
        ItemOffer actualItem = getSingleItemFromCart(cart);
        assertItemsEqual(expectedItem, actualItem);
    }

    @Test
    public void shouldNotAddOfferWithNullFee() {
        assertThrows(ErrorCodeException.class, () -> {
            ItemOffer expectedItem = generateItemWith(OFFER, offer -> offer.setFee(null));

            carterClient.addItem(
                    uuidContext.getUserAnyId(),
                    UID,
                    LIST_BASKET_ID,
                    Color.BLUE,
                    CartConverter.convert(expectedItem),
                    null
            );
        });
    }

    @Test
    @Disabled
    public void shouldNotAddOfferWithNullMsku() {
        assertThrows(ErrorCodeException.class, () -> {
            ItemOffer expectedItem = generateItemWith(OFFER, offer -> offer.setMsku(null));

            carterClient.addItem(
                    uuidContext.getUserAnyId(),
                    UID,
                    LIST_BASKET_ID,
                    Color.BLUE,
                    CartConverter.convert(expectedItem),
                    null
            );
        });
    }


    @Test
    public void shouldAddOffer() {
        final ItemOffer item = generateItem(OFFER);

        carterClient.addItem(uidContext.getUserAnyId(), UID, LIST_BASKET_ID, Color.BLUE, CartConverter.convert(item),
                null);

        final ItemOffer createdItem = getSingleItemFromCart(
                CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID,
                        Color.BLUE
                )));
        assertItemsEqual(item, createdItem);
    }

    @Test
    public void shouldReplaceSameMskuOfferItem() {
        final ItemOffer item = generateItem(OFFER);
        final ItemOffer anotherItem = cloneItemWith(item, offer -> offer.setObjId(ANOTHER_OFFER));

        carterClient.addItem(uidContext.getUserAnyId(), UID, LIST_BASKET_ID, Color.BLUE, CartConverter.convert(item),
                null);
        carterClient.addItem(uidContext.getUserAnyId(), UID, LIST_BASKET_ID, Color.BLUE,
                CartConverter.convert(anotherItem), null);

        final ItemOffer createdItem = getSingleItemFromCart(
                CartConverter.convert(carterClient.getCart(
                        uidContext.getUserAnyId(),
                        UID,
                        Color.BLUE
                )));
        assertItemsEqual(anotherItem, createdItem);
    }

    //TODO MARKETCHECKOUT-14382
//    @Test
    public void shouldReplaceSameMskuOfferItemOnSameBatchAdd() {
        final ItemOffer item = generateItemWith(OFFER, offer -> offer.setMsku(123L));
        final ItemOffer anotherItem = generateItemWith(ANOTHER_OFFER, offer -> offer.setMsku(123L));

        carterClient.addItems(
                uidContext.getUserAnyId(),
                uidContext.getUserIdType(),
                LIST_BASKET_ID,
                Color.BLUE,
                CartConverter.convert(createCartFor(uidContext, item, anotherItem)),
                null
        );

        final ItemOffer createdItem = getSingleItemFromCart(
                CartConverter.convert(carterClient.getCart(
                        uidContext.getUserAnyId(),
                        uidContext.getUserIdType(),
                        Color.BLUE
                )));
        assertItemsEqual(anotherItem, createdItem);
    }

    @Test
    public void shouldNotReplaceSameMskuOfferItemWhenMultiOffersEnabled() {
        final ItemOffer item = generateItem(OFFER);
        final ItemOffer anotherItem = cloneItemWith(item, offer -> offer.setObjId(ANOTHER_OFFER));

        carterClient.addItem(
                uidContext.getUserAnyId(),
                UID,
                LIST_BASKET_ID,
                Color.BLUE,
                CartConverter.convert(item),
                null,
                true
        );
        carterClient.addItem(
                uidContext.getUserAnyId(),
                UID,
                LIST_BASKET_ID,
                Color.BLUE,
                CartConverter.convert(anotherItem),
                null,
                true
        );

        Cart cart = CartConverter.convert(carterClient.getCart(
                uidContext.getUserAnyId(),
                UID,
                Color.BLUE
        ));
        assertNotNull(cart.getBasketList());
        TreeSet<ItemOffer> items = new TreeSet<>(CarterTestUtils.OFFER_STATE_COMPARATOR);
        items.addAll(asType(cart.getBasketList().getItems(), ItemOffer.class));

        assertThat(items, hasSize(2));
        assertTrue(items.contains(item));
        assertTrue(items.contains(anotherItem));
    }

    @ParameterizedTest
    @ValueSource(strings = {"BLUE", "WHITE"})
    public void shouldUpdateCountForOfferItem(Color color) {
        // create blue item, should be able to update from any color
        final ItemOffer item = generateItem(OFFER);

        long itemId = carterClient.addItem(
                uidContext.getUserAnyId(),
                UID,
                LIST_BASKET_ID,
                Color.BLUE,
                CartConverter.convert(item),
                null
        );

        final ItemOffer createdItem = getSingleItemFromCart(
                CartConverter.convert(carterClient.getCart(
                        uidContext.getUserAnyId(),
                        UID,
                        Color.BLUE
                ))
        );
        assertItemsEqual(item, createdItem);
        assertThat(createdItem.getCount(), comparesEqualTo(1));

        carterClient.updateItem(
                uidContext.getUserAnyId(),
                UID,
                LIST_BASKET_ID,
                color,
                itemId,
                NEW_COUNT,
                null
        );

        reportMockConfigurer.mockReportOk();
        final ItemOffer updatedItem = getSingleItemFromCart(
                CartConverter.convert(carterClient.getCart(
                        uidContext.getUserAnyId(),
                        UID,
                        Color.BLUE
                )));
        assertItemsEqual(item, updatedItem);
        assertEquals(NEW_COUNT, updatedItem.getCount().longValue());
    }

    @Test
    public void shouldReturnCart() {
        CartViewModel cart = carterClient.getCart(uidContext.getUserAnyId(), UID, Color.BLUE);
        assertNotNull(cart);
        assertNotNull(cart.getLists().get(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"BLUE", "WHITE"})
    public void shouldUseExperimentsOnCartActualization(Color color) {
        ItemOffer item = generateItem(OFFER);
        ReportGeneratorParameters reportGeneratorParameters = new ReportGeneratorParameters()
                .addReportOffer(item.getMsku(), new ReportGeneratorParameters.ReportOffer("testWareMd5", 145));
        reportMockConfigurer.mockReportOk(reportGeneratorParameters);
        carterClient.addItem(uidContext.getUserAnyId(), UID, -1, color, CartConverter.convert(item), "");
        CartRequest request = CartRequest.builder(uidContext.getUserAnyId(), UID)
                .withRgb(color)
                .withExperiments("just_experiment")
                .withConsolidate(true)
                .withRegion(213L)
                .build();
        CartViewModel cart = carterClient.getCart(request);

        // white and blue cart requests should go to blue report with rgb=blue
        reportMockConfigurerWhite.verifyReportColorCalls(null, 0);
        assertNotNull(cart);
    }

    @ParameterizedTest
    @ValueSource(strings = {"BLUE", "WHITE"})
    public void testCartActualizationReportCallWithForceWhiteExperiment(Color color) {
        ItemOffer item = generateItem(OFFER);
        ReportGeneratorParameters reportGeneratorParameters = new ReportGeneratorParameters()
                .addReportOffer(item.getMsku(), new ReportGeneratorParameters.ReportOffer("testWareMd5", 145));
        reportMockConfigurerWhite.mockReportOk(reportGeneratorParameters);
        carterClient.addItem(uidContext.getUserAnyId(), UID, -1, color, CartConverter.convert(item), "");
        CartRequest request = CartRequest.builder(uidContext.getUserAnyId(), UID)
                .withRgb(color)
                .withExperiments(Experiments.BERU_USE_WHITE_REPORT + "=1")
                .withConsolidate(true)
                .withRegion(213L)
                .build();
        CartViewModel cart = carterClient.getCart(request);
        reportMockConfigurer.verifyReportColorCalls(ru.yandex.market.common.report.model.Color.BLUE, 0);
        assertNotNull(cart);
    }

    @Test
    public void shouldUsePerkPromoIdOnCartActualization() {
        ItemOffer item = generateItem(OFFER);
        ReportGeneratorParameters reportGeneratorParameters = new ReportGeneratorParameters()
                .addReportOffer(item.getMsku(), new ReportGeneratorParameters.ReportOffer("testWareMd5", 145));
        reportMockConfigurer.mockReportOk(reportGeneratorParameters);
        carterClient.addItem(uidContext.getUserAnyId(), UID, -1, Color.BLUE, CartConverter.convert(item), "");
        CartRequest request = CartRequest.builder(uidContext.getUserAnyId(), UID)
                .withRgb(Color.BLUE)
                .withPerkPromoId("some_promo_id")
                .withConsolidate(true)
                .withRegion(213L)
                .build();
        CartViewModel cart = carterClient.getCart(request);
        assertNotNull(cart);
    }

    @ParameterizedTest
    @ValueSource(strings = {"BLUE", "WHITE"})
    public void shouldDeleteItemById(Color color) {
        // create blue item, should be able to delete from any color
        carterClient.addItem(
                uidContext.getUserAnyId(),
                uidContext.getUserIdType(),
                LIST_BASKET_ID,
                Color.BLUE,
                CartConverter.convert(generateItem(OFFER)),
                null
        );
        Cart cart = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, Color.BLUE));
        ItemOffer itemOffer = getSingleItemFromCart(cart);

        assertThat(itemOffer, notNullValue());
        assertThat(itemOffer.getId(), greaterThan(0L));

        carterClient.removeItem(
                uidContext.getUserAnyId(),
                uidContext.getUserIdType(),
                LIST_BASKET_ID,
                color,
                itemOffer.getId(),
                null
        );

        assertThat(
                carterClient.getCart(uidContext.getUserAnyId(), UID, Color.BLUE).getLists().get(0).getItems(),
                empty()
        );
    }

    @Test
    public void shouldBulkCreateItems() {
        Cart cart = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), uidContext.getUserIdType(),
                Color.BLUE));

        final int initCartSize = cart.getBasketList().getItems().size();
        final int count = 10;
        carterClient.addItems(
                uidContext.getUserAnyId(),
                uidContext.getUserIdType(),
                LIST_ID,
                Color.BLUE,
                CartConverter.convert(CarterTestUtils.generateCartList(count)),
                PARAM_USER_GROUP
        );

        cart = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), uidContext.getUserIdType(),
                Color.BLUE));
        assertThat(cart.getBasketList().getItems().size(), equalTo(initCartSize + count));
    }

    @Test
    public void shouldNotActualizeAndConsolidateItemsWithoutMskuWithSingleOffer() {
        ItemOffer item = generateItem("A");
        item.setMsku(null);
        carterClient.addItem(uidContext.getUserAnyId(), UID, LIST_BASKET_ID, WHITE, CartConverter.convert(item), null);

        ItemOffer item2 = generateItem("B");
        item2.setMsku(null);
        carterClient.addItem(uidContext.getUserAnyId(), UID, LIST_BASKET_ID, WHITE, CartConverter.convert(item2), null);

        CartRequest request = CartRequest.builder(uidContext.getUserAnyId(), UID)
                .withRgb(Color.WHITE)
                .withConsolidate(true)
                .withEnableMultiOffers(true)
                .withDsbsEnabled(true)
                .withRegion(213L)
                .build();
        Cart whiteCart = CartConverter.convert(carterClient.getCart(request));

        Collection<CartItem> cartItems = whiteCart.getBasketList().getItems();
        assertThat(cartItems, hasSize(2));

        reportMockConfigurerWhite.verifyReportMockNotCalled();
    }

    @Test
    public void shouldNotActualizeAndConsolidateItemsWithoutMskuWithMultipleOffers() {
        ItemOffer item = generateItem("A");
        item.setMsku(null);
        carterClient.addItem(uidContext.getUserAnyId(), UID, LIST_BASKET_ID, WHITE, CartConverter.convert(item), null);

        ItemOffer item2 = generateItem("B");
        item2.setMsku(123L);
        carterClient.addItem(uidContext.getUserAnyId(), UID, LIST_BASKET_ID, WHITE, CartConverter.convert(item2), null);

        ReportGeneratorParameters reportGeneratorParameters = new ReportGeneratorParameters()
                .addReportOffer(123L, new ReportGeneratorParameters.ReportOffer("testWareMd5", 145));

        reportMockConfigurerWhite.mockReportOk(reportGeneratorParameters);
        CartRequest request = CartRequest.builder(uidContext.getUserAnyId(), UID)
                .withRgb(Color.WHITE)
                .withConsolidate(true)
                .withEnableMultiOffers(true)
                .withDsbsEnabled(true)
                .withRegion(213L)
                .build();
        Cart cart = CartConverter.convert(carterClient.getCart(request));

        Collection<CartItem> cartItems = cart.getBasketList().getItems();
        assertThat(cartItems, hasSize(2));
        assertTrue(cartItems.stream().noneMatch(it -> ((ItemOffer) it).isExpired()));
    }

    @Test
    public void shouldNotReturnDsbsOffersWhenFlagDisabled() {
        ItemOffer item = generateItem("A");
        item.setMsku(null);
        carterClient.addItem(uidContext.getUserAnyId(), UID, LIST_BASKET_ID, WHITE, CartConverter.convert(item), null);

        ItemOffer item2 = generateItem("B");
        item2.setMsku(null);
        carterClient.addItem(uidContext.getUserAnyId(), UID, LIST_BASKET_ID, WHITE, CartConverter.convert(item2), null);

        ItemOffer item3 = generateItem("C");
        carterClient.addItem(uidContext.getUserAnyId(), UID, LIST_BASKET_ID, WHITE, CartConverter.convert(item3), null);

        CartRequest request = CartRequest.builder(uidContext.getUserAnyId(), UID)
                .withRgb(Color.WHITE)
                .withConsolidate(false)
                .withEnableMultiOffers(true)
                .withRegion(213L)
                .build();
        Cart whiteCart = CartConverter.convert(carterClient.getCart(request));

        Collection<CartItem> cartItems = whiteCart.getBasketList().getItems();
        assertThat(cartItems, hasSize(1));
        assertTrue(cartItems.stream().noneMatch(it -> ((ItemOffer) it).getMsku() == null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"BLUE", "WHITE"})
    public void shouldBulkDeleteItemsByUserId(Color color) {
        // create blue item, should be able to delete from any color
        int count = 10;
        carterClient.addItems(
                uidContext.getUserAnyId(),
                uidContext.getUserIdType(),
                LIST_ID,
                Color.BLUE,
                CartConverter.convert(CarterTestUtils.generateCartList(count)),
                PARAM_USER_GROUP
        );

        Cart cart = CartConverter.convert(carterClient.getCart(
                uidContext.getUserAnyId(),
                uidContext.getUserIdType(),
                Color.BLUE
        ));

        List<Long> itemIds = cart.getBasketList()
                .getItems()
                .stream()
                .map(CartItem::getId)
                .collect(toList());

        carterClient.removeItems(
                uidContext.getUserAnyId(),
                uidContext.getUserIdType(),
                LIST_ID,
                color,
                itemIds,
                PARAM_USER_GROUP
        );
        cart = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), uidContext.getUserIdType(),
                Color.BLUE));
        assertThat(cart.getBasketList().getItems().size(), equalTo(0));
    }

    //TODO проверить кейсы - странно что мы ожидаем внутренний идентификатор картера снаружи
    @Test
    public void shouldBulkDeleteItemsByOwnerId() {
        int count = 10;
        carterClient.addItems(
                uidContext.getUserAnyId(), UID, LIST_ID, Color.BLUE,
                CartConverter.convert(CarterTestUtils.generateCartList(count)),
                PARAM_USER_GROUP
        );

        Cart cart = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, Color.BLUE));

        List<Long> itemIds = cart.getBasketList()
                .getItems()
                .stream()
                .map(CartItem::getId)
                .collect(toList());

        carterClient.removeItems(
                uidContext.getUserAnyId(),
                UID,
                LIST_ID,
                Color.BLUE,
                itemIds,
                PARAM_USER_GROUP
        );
        cart = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, Color.BLUE));
        assertThat(cart.getBasketList().getItems().size(), equalTo(0));
    }

    @Test
    public void shouldDeleteMissingItemsByDefault() {
        storageCartService.replaceCartListOwnerId(uidContext, createCartFor(
                uidContext
        ));

        carterClient.removeItems(
                uidContext.getUserAnyId(),
                uidContext.getUserIdType(),
                LIST_BASKET_ID,
                List.of(-1L, -2L),
                null
        );
    }

    @Test
    public void shouldNotConsolidateWhenNullRegion() {
        checkNotConsolidate(null, true);
    }

    @Test
    public void shouldNotConsolidateWhenFlag() {
        checkNotConsolidate(456L, false);
    }

    @Test
    public void shouldSaveLabelFieldsAndReturnInResponse() {
        CartList cart = createCartFor(
                uidContext,
                generateItem("some_offer"),
                generateItem("another offer")
        );

        carterClient.addItems(uidContext.getUserAnyId(), UID, LIST_BASKET_ID, Color.BLUE, CartConverter.convert(cart),
                null);

        CartList result = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, 456L,
                Color.BLUE, false)).getBasketList();

        assertThat(result.getItems(), hasItems(
                hasProperty("label", is("some_offer")),
                hasProperty("label", is("another offer"))
        ));
    }

    @Test
    public void shouldSaveLabelFieldsForBundledItemsAndReturnInResponse() {
        CartList cart = createCartFor(
                uidContext,
                generateItemWithBundle("primary offer", "promo bundle", true),
                generateItemWithBundle("gift offer", "promo bundle"),
                generateItem("primary offer", 1, RandomUtils.nextInt())
        );

        carterClient.addItems(uidContext.getUserAnyId(), UID, LIST_BASKET_ID, Color.BLUE, CartConverter.convert(cart),
                null);

        CartList result = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, 456L,
                Color.BLUE, false)).getBasketList();

        assertThat(result.getItems(), hasItems(
                hasProperty("label", is("primary offer in promo bundle")),
                hasProperty("label", is("gift offer in promo bundle")),
                hasProperty("label", is("primary offer"))
        ));
    }

    @Test
    public void shouldReturnLabelFieldsForCartUpdate() {
        CartList cart = createCartFor(
                uidContext,
                generateItemWithBundle("primary offer", "promo bundle", true),
                generateItemWithBundle("gift offer", "promo bundle"),
                generateItem("primary offer")
        );

        carterClient.updateCart(uidContext.getUserAnyId(), UID, Color.BLUE, CartConverter.convert(cart));

        CartList result = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, 456L,
                Color.BLUE, false)).getBasketList();

        assertThat(result.getItems(), hasItems(
                hasProperty("label", is("primary offer in promo bundle")),
                hasProperty("label", is("gift offer in promo bundle")),
                hasProperty("label", is("primary offer"))
        ));
    }

    @Test
    public void shouldNotAcceptLongLabels() {
        final ItemOffer longLabeledItem = generateItem("primary offer");
        longLabeledItem.setLabel(StringUtils.repeat("blablabla ",
                CartResourceController.MAX_LABEL_LENGTH / 10 + 1)); // > threshold
        final CartList longLabeledCart = createCartFor(uidContext, longLabeledItem);
        assertThrows(ErrorCodeException.class,
                () -> carterClient.updateCart(uidContext.getUserAnyId(), UID, Color.BLUE,
                        CartConverter.convert(longLabeledCart)));
    }

    @Test
    public void shouldUpdateAndReturnPromoType() {
        final ItemOffer item = generateItem("primary offer");
        item.setPromoType(ReportPromoType.BLUE_FLASH.getCode());
        final CartList cart = createCartFor(uidContext, item);

        final CartList result = CartConverter.convert(carterClient.updateCart(uidContext.getUserAnyId(), UID,
                Color.BLUE, CartConverter.convert(cart)));

        assertNotNull(result);
        assertThat(result.getItems(), hasSize(1));
        assertThat(result.getItems(), hasItems(
                hasProperty("promoType", is("blue-flash"))
        ));
    }

    @Test
    public void shouldUpdateAndReturnPromoKey() {
        final ItemOffer item = generateItem("primary offer");
        item.setPromoKey("test promo key");
        final CartList cart = createCartFor(uidContext, item);

        final CartList result = CartConverter.convert(carterClient.updateCart(uidContext.getUserAnyId(), UID,
                Color.BLUE, CartConverter.convert(cart)));

        assertNotNull(result);
        assertThat(result.getItems(), hasSize(1));
        assertThat(result.getItems(), hasItems(
                hasProperty("promoKey", is("test promo key"))
        ));
    }

    @Test
    public void shouldNotFailOnObjTypeAbsence() {
        CartList cart = createCartFor(
                uidContext,
                generateItemWith("primary offer", item -> item.setObjType(null))
        );

        carterClient.updateCart(uidContext.getUserAnyId(), UID, Color.BLUE, CartConverter.convert(cart));

        CartList result = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, 456L,
                Color.BLUE, false)).getBasketList();

        assertThat(result.getItems(), hasItem(
                hasProperty("objType", is(CartItem.Type.OFFER))
        ));
    }

    @Test
    public void shouldMergeCartToAuthorizedWithMultioffer() {
        ItemOffer item1 = generateItem("item1-171");
        item1.setWarehouseId(171);
        item1.setMsku(1L);

        ItemOffer item2 = generateItem("item1-172");
        item2.setMsku(1L);
        item2.setWarehouseId(172);

        Cart userCart = CartConverter.convert(carterClient.getCart(uuidContext.getUserAnyId(), UserIdType.UUID,
                Color.BLUE));
        Cart uuidCart = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, Color.BLUE));
        assertCartIsEmpty(userCart);
        assertCartIsEmpty(uuidCart);

        carterClient.addItem(uuidContext.getUserAnyId(), UserIdType.UUID, LIST_BASKET_ID, Color.BLUE,
                CartConverter.convert(item1), null,
                true);
        carterClient.addItem(uidContext.getUserAnyId(), UID, LIST_BASKET_ID, Color.BLUE, CartConverter.convert(item2),
                null, true);

        userCart = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, Color.BLUE));
        assertEquals(1, userCart.getBasketList().getItems().size());

        uuidCart = CartConverter.convert(carterClient.getCart(uuidContext.getUserAnyId(), UserIdType.UUID, Color.BLUE));
        assertEquals(1, uuidCart.getBasketList().getItems().size());

        assertTrue(
                carterClient.mergeItems(
                        uuidContext.getUserAnyId(),
                        UserIdType.UUID,
                        uidContext.getUserAnyId(),
                        UID,
                        Color.BLUE,
                        true
                )
        );

        userCart = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, Color.BLUE));
        assertEquals(2, userCart.getBasketList().getItems().size());
    }

    @Test
    public void shouldMergeCartToAuthorizedWithMultiofferWithoutLocks() {

        ItemOffer item1 = generateItem("item1-171");
        item1.setWarehouseId(171);
        item1.setMsku(1L);

        ItemOffer item2 = generateItem("item1-172");
        item2.setMsku(1L);
        item2.setWarehouseId(172);

        Cart userCart = CartConverter.convert(carterClient.getCart(uuidContext.getUserAnyId(), UserIdType.UUID,
                Color.BLUE));
        Cart uuidCart = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, Color.BLUE));
        assertCartIsEmpty(userCart);
        assertCartIsEmpty(uuidCart);

        carterClient.addItem(uuidContext.getUserAnyId(), UserIdType.UUID, LIST_BASKET_ID, Color.BLUE,
                CartConverter.convert(item1), null,
                true);
        carterClient.addItem(uidContext.getUserAnyId(), UID, LIST_BASKET_ID, Color.BLUE, CartConverter.convert(item2),
                null, true);

        userCart = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, Color.BLUE));
        assertEquals(1, userCart.getBasketList().getItems().size());

        uuidCart = CartConverter.convert(carterClient.getCart(uuidContext.getUserAnyId(), UserIdType.UUID, Color.BLUE));
        assertEquals(1, uuidCart.getBasketList().getItems().size());

        assertTrue(
                carterClient.mergeItems(
                        uuidContext.getUserAnyId(),
                        UserIdType.UUID,
                        uidContext.getUserAnyId(),
                        UID,
                        Color.BLUE,
                        true
                )
        );

        userCart = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, Color.BLUE));
        assertEquals(2, userCart.getBasketList().getItems().size());
    }

    @Test
    public void shouldSaveBenefitOnAddingItem() {
        final ItemOffer item = generateItem(OFFER);
        item.setBenefit(OfferBenefit.FASTER_DELIVERY);

        carterClient.addItem(
                uidContext.getUserAnyId(),
                UID,
                LIST_BASKET_ID,
                Color.BLUE,
                CartConverter.convert(item),
                null
        );

        var cart = carterClient.getCart(uidContext.getUserAnyId(), UID, Color.BLUE);
        var items = cart.getLists().get(0).getItems();
        assertThat(items, hasSize(1));
        assertEquals(OfferBenefit.FASTER_DELIVERY, items.get(0).getBenefit());
    }

    @Test
    @Disabled
    public void getCartWithItemWithMultiPromoTest() {
        prepareTestData("json/item_multipromo.json");
        var cart = CartConverter.convert(carterClient.getCart("1", UID, Color.BLUE));
        assertThat(cart.getBasketList().getItems(), hasSize(4));
        CartItem item = cart.getBasketList().getItems().get(0);
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
    public void shouldSaveOfferFeaturesOnAddingItem() {
        final ItemOffer item = generateItem(OFFER);
        item.setFeatures(Set.of("EXPRESS_DELIVERY"));
        carterClient.addItem(
                uidContext.getUserAnyId(),
                UID,
                LIST_BASKET_ID,
                Color.BLUE,
                CartConverter.convert(item),
                null
        );
        var cart = carterClient.getCart(uidContext.getUserAnyId(), UID, Color.BLUE);
        var items = cart.getLists().get(0).getItems();
        assertThat(items, hasSize(1));
        assertEquals(Set.of("EXPRESS_DELIVERY"), items.get(0).getFeatures());
    }

    @Test
    @DisplayName("POSITIVE: Итоговые значения корзины: Добавление одного товара")
    void getCartWithSingleItemTotalsTest() {

        final ItemOffer item = generateItem(OFFER, RandomUtils.nextInt(1, 10), OFFER.hashCode());
        carterClient.addItem(AddItemRequest.builder()
                .withColor(WHITE)
                .withUserAnyId(uidContext.getUserAnyId())
                .withUserIdType(UID)
                .withCartItem(CartConverter.convert(item))
                .withEnableMultiOffers(true)
                .build());

        var cart = carterClient.getCart(uidContext.getUserAnyId(), UID, Color.WHITE);

        assertEquals(item.getCount(), cart.getLists().get(0).getTotalCount());
        assertEquals(item.getPriceWithoutVat(), cart.getLists().get(0).getItems().get(0).getPriceWithoutVat());
        Assertions.assertThat(cart.getLists().get(0).getTotalPrice())
                .isEqualByComparingTo(item.getPrice().multiply(new BigDecimal(item.getCount())));
    }

    @Test
    @DisplayName("POSITIVE: Итоговые значения корзины: Добавление нескольких товаров")
    void getCartWithSeveralItemsTotalsTest() {

        final ItemOffer item = generateItem(OFFER, RandomUtils.nextInt(1, 10), OFFER.hashCode());
        String offer = RandomStringUtils.randomAlphabetic(10);
        final ItemOffer item1 = generateItem(offer, RandomUtils.nextInt(1, 10), offer.hashCode());

        var cart = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, Color.WHITE));
        cart.getBasketList().addItem(item);
        cart.getBasketList().addItem(item1);

        CartList list = CartConverter.convert(carterClient.updateCart(ReplaceItemsRequest.builder()
                .withUserAnyId(uidContext.getUserAnyId())
                .withUserIdType(UID)
                .withColor(WHITE)
                .withCartList(CartConverter.convert(cart.getBasketList()))
                .build()));

        assertEquals(item.getCount() + item1.getCount(), list.getTotalCount());
        Assertions.assertThat(list.getTotalPrice())
                .isEqualByComparingTo(
                        item.getPrice()
                                .multiply(new BigDecimal(item.getCount()))
                                .add(item1.getPrice().multiply(new BigDecimal(item1.getCount())))
                );
    }

    @Test
    @DisplayName("POSITIVE: Итоговые значения корзины: Удаление товара")
    void getCartWithNoItemsTotalsTest() {

        final ItemOffer item = generateItem(OFFER, RandomUtils.nextInt(1, 10), OFFER.hashCode());
        long itemId = carterClient.addItem(AddItemRequest.builder()
                .withColor(WHITE)
                .withUserAnyId(uidContext.getUserAnyId())
                .withUserIdType(UID)
                .withCartItem(CartConverter.convert(item))
                .withEnableMultiOffers(true)
                .build());

        carterClient.removeItem(uidContext.getUserAnyId(), UID, -1, WHITE, itemId, null);

        var cart = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, Color.WHITE));

        assertEquals(0, cart.getBasketList().getTotalCount());
        Assertions.assertThat(cart.getBasketList().getTotalPrice())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("POSITIVE: Итоговые значения корзины: Изменение количества товара")
    void getCartChangeItemCountTotalsTest() {

        final ItemOffer item = generateItem(OFFER, RandomUtils.nextInt(1, 10), OFFER.hashCode());
        long itemId = carterClient.addItem(AddItemRequest.builder()
                .withColor(WHITE)
                .withUserAnyId(uidContext.getUserAnyId())
                .withUserIdType(UID)
                .withCartItem(CartConverter.convert(item))
                .withEnableMultiOffers(true)
                .build());

        int newCount = RandomUtils.nextInt(1, 10);
        carterClient.updateItem(uidContext.getUserAnyId(), UID, -1, WHITE, itemId, newCount, null);

        var cart = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, Color.WHITE));

        assertEquals(newCount, cart.getBasketList().getTotalCount());
        Assertions.assertThat(cart.getBasketList().getTotalPrice())
                .isEqualByComparingTo(
                        item.getPrice().multiply(new BigDecimal(newCount))
                );
    }

    private void checkNotConsolidate(Long region, boolean consolidate) {
        Long itemMsku = 123L;
        ItemOffer item1Blue = generateItem("A");
        item1Blue.setMsku(itemMsku);
        carterClient.addItem(uidContext.getUserAnyId(), UID, LIST_BASKET_ID, Color.BLUE,
                CartConverter.convert(item1Blue), null);

        ReportGeneratorParameters reportGeneratorParameters = new ReportGeneratorParameters()
                .addReportOffer(itemMsku, new ReportGeneratorParameters.ReportOffer("testWareMd5", 145));

        reportMockConfigurer.mockReportOk(reportGeneratorParameters);
        Cart blueCartAfter = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, region,
                Color.BLUE, consolidate));

        Collection<CartItem> cartItems = blueCartAfter.getBasketList().getItems();
        MatcherAssert.assertThat(cartItems, hasSize(1));
        assertFalse(((ItemOffer) cartItems.iterator().next()).isExpired());

        reportMockConfigurer.verifyReportMockNotCalled();
    }
}
