package ru.yandex.market.checkout.carter.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.model.CartItem;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.model.UserIdType;
import ru.yandex.market.checkout.carter.web.UserContext;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.createCartFor;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.extractBasketList;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItemWith;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.itemOf;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.keyOf;

/**
 * @author imelnikov
 */
//TODO: пересмотреть тесты - есть дубликаты кейсов
public class StorageCartServiceTest extends CarterMockedDbTestBase {

    private static final String FIRST_OFFER = "first offer";
    private static final String SECOND_OFFER = "second offer";

    private final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    private UserContext userContext;
    @Value("${carter.user_limits.limit_items}")
    private int limitItems;
    @Autowired
    private StorageCartService storageCartService;

    @BeforeEach
    public void prepare() {
        userContext = UserContext.of(OwnerKey.of(
                Color.BLUE, UID, "" + rnd.nextLong(1, Integer.MAX_VALUE)));
    }

    @Test
    public void shouldAddNewItemsIfNotExistOnMerge() {
        UserContext from = UserContext.of(OwnerKey.of(Color.BLUE, UserIdType.UUID,
                UUID.randomUUID().toString()));
        UserContext to = UserContext.of(OwnerKey.of(Color.BLUE, UID,
                String.valueOf(Math.abs(rnd.nextLong()))));

        ItemOffer offer = generateItem(FIRST_OFFER);
        storageCartService.createOrReplaceItemByMsku(from, offer);

        CartList cartListBeforeMerge = extractBasketList(storageCartService.getListsOwnerId(to));
        assertThat(cartListBeforeMerge.getItems(), is(empty()));

        storageCartService.mergeItemsToOwnerList(
                to.getUserIdType(),
                to.getUserAnyId(),
                from.getUserIdType(),
                from.getUserAnyId(),
                to.getColor(),
                false
        );

        CartList cartListAfterMerge = extractBasketList(storageCartService.getListsOwnerId(to));
        assertThat(cartListAfterMerge.getItems(), hasSize(1));
    }

    @Test
    public void shouldAddAllColorItemsWhenRgbIsNull() {
        UserContext fromWhite =
                UserContext.of(OwnerKey.of(Color.WHITE, UserIdType.UUID, UUID.randomUUID().toString()));
        ItemOffer offer = generateItem(FIRST_OFFER);
        storageCartService.createOrReplaceItemByMsku(fromWhite, offer);

        UserContext fromFoodtech =
                UserContext.of(OwnerKey.of(Color.FOODTECH, UserIdType.UUID, fromWhite.getUserAnyId()));
        ItemOffer secondOffer = generateItem(SECOND_OFFER);
        storageCartService.createOrReplaceItemByMsku(fromFoodtech, secondOffer);

        UserContext to =
                UserContext.of(OwnerKey.of(Color.BLUE, UID, String.valueOf(Math.abs(rnd.nextLong()))));
        CartList cartListBeforeMerge = extractBasketList(storageCartService.getListsOwnerId(to));
        assertThat(cartListBeforeMerge.getItems(), is(empty()));

        storageCartService.mergeItemsToOwnerList(
                to.getUserIdType(),
                to.getUserAnyId(),
                fromWhite.getUserIdType(),
                fromWhite.getUserAnyId(),
                null,
                false
        );
        List<Color> colorList = List.of(Color.WHITE, Color.FOODTECH);
        List<CartList> cartListsAfterMerge =
                storageCartService.getCartLists(to.getUserIdType(), to.getUserAnyId(), colorList).getResult();

        assertThat(cartListsAfterMerge, hasSize(2));
        assertThat(cartListsAfterMerge, hasItems(
                allOf(
                        hasProperty("rgb", equalTo(Color.WHITE)),
                        hasProperty("items", hasSize(1)),
                        hasProperty("items", hasItems(is(offer)))
                ),
                allOf(
                        hasProperty("rgb", equalTo(Color.FOODTECH)),
                        hasProperty("items", hasSize(1)),
                        hasProperty("items", hasItems(is(secondOffer)))
                )
        ));
    }

    @Test
    public void shouldAddSpecificColorItemsWhenRgbIsNotNull() {
        UserContext fromWhite = UserContext.of(OwnerKey.of(Color.WHITE, UserIdType.UUID,
                UUID.randomUUID().toString()));
        ItemOffer offer = generateItem(FIRST_OFFER);
        storageCartService.createOrReplaceItemByMsku(fromWhite, offer);

        UserContext fromFoodtech = UserContext.of(OwnerKey.of(Color.FOODTECH, fromWhite.getUserIdType(),
                fromWhite.getUserAnyId()));
        ItemOffer secondOffer = generateItem(SECOND_OFFER);
        storageCartService.createOrReplaceItemByMsku(fromFoodtech, secondOffer);

        UserContext to = UserContext.of(OwnerKey.of(Color.BLUE, UID, String.valueOf(Math.abs(rnd.nextLong()))));
        CartList cartListBeforeMerge = extractBasketList(storageCartService.getListsOwnerId(to));
        assertThat(cartListBeforeMerge.getItems(), is(empty()));

        storageCartService.mergeItemsToOwnerList(
                to.getUserIdType(),
                to.getUserAnyId(),
                fromWhite.getUserIdType(),
                fromWhite.getUserAnyId(),
                Color.WHITE,
                false
        );
        List<Color> colorList = List.of(Color.WHITE, Color.FOODTECH);
        List<CartList> cartListsAfterMerge =
                storageCartService.getCartLists(to.getUserIdType(), to.getUserAnyId(), colorList).getResult();

        assertThat(cartListsAfterMerge, hasSize(2));
        assertThat(cartListsAfterMerge, hasItems(
                allOf(
                        hasProperty("rgb", equalTo(Color.WHITE)),
                        hasProperty("items", hasSize(1)),
                        hasProperty("items", hasItems(is(offer)))
                ),
                allOf(
                        hasProperty("rgb", equalTo(Color.FOODTECH)),
                        hasProperty("items", hasSize(0))
                )
        ));
    }

    @Test
    public void shouldSkipDuplicateItems() {
        CartList basketList = storageCartService.replaceCartListOwnerId(
                userContext,
                createCartFor(userContext, generateItem(FIRST_OFFER))
        ).getResult();

        assertThat(basketList.getItems(), not(empty()));

        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(createCartFor(
                userContext,
                generateItem(FIRST_OFFER),
                generateItem(SECOND_OFFER)
        )), true);

        assertThat(
                extractBasketList(storageCartService.getListsOwnerId(userContext)).getItems(),
                hasSize(2)
        );
    }

    @Test
    public void shouldMergeDuplicateItemsOnBulkCreation() {
        CartList basketList = storageCartService.replaceCartListOwnerId(
                userContext,
                createCartFor(userContext, generateItem(FIRST_OFFER))
        ).getResult();

        assertThat(basketList.getItems(), not(empty()));

        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(createCartFor(
                userContext,
                generateItem(FIRST_OFFER),
                generateItem(FIRST_OFFER)
        )), true);

        assertThat(
                extractBasketList(storageCartService.getListsOwnerId(userContext)).getItems(),
                hasSize(1)
        );
    }

    @Test
    @DisplayName("В запросе на добавление пачки офферов, пришли дубли по msku ")
    public void shouldMergeDuplicateItemsByMskuOnBulkCreation() {
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(createCartFor(
                userContext,
                generateItem(FIRST_OFFER, 1, 1),
                generateItem(SECOND_OFFER, 1, 1)
        )), false);

        assertThat(
                extractBasketList(storageCartService.getListsOwnerId(userContext)).getItems(),
                hasSize(1)
        );
    }

    @Test
    public void shouldReplaceItemByMskuIfExists() {
        CartList basketList = storageCartService.replaceCartListOwnerId(
                userContext,
                createCartFor(userContext, generateItemWith(FIRST_OFFER, offer -> offer.setMsku(42L)))
        ).getResult();

        assertThat(basketList.getItems(), not(empty()));

        storageCartService.createOrReplaceItemByMsku(userContext, generateItemWith(SECOND_OFFER,
                offer -> offer.setMsku(42L)));

        basketList = extractBasketList(storageCartService.getListsOwnerId(userContext));

        assertThat(basketList.getItems(), hasSize(1));
        assertThat(basketList.getItems(), hasItem(hasProperty("name", is(SECOND_OFFER))));
    }

    @Test
    public void shouldNotReplaceItemByMskuIfSetMultiOffers() {
        CartList basketList = storageCartService.replaceCartListOwnerId(
                userContext,
                createCartFor(userContext, generateItemWith(FIRST_OFFER, offer -> offer.setMsku(42L)))
        ).getResult();

        assertThat(basketList.getItems(), not(empty()));

        storageCartService.createItemNoMskuCheck(userContext, generateItemWith(SECOND_OFFER,
                offer -> offer.setMsku(42L)));

        basketList = extractBasketList(storageCartService.getListsOwnerId(userContext));

        assertThat(basketList.getItems(), hasSize(2));
        assertThat(basketList.getItems(), hasItems(
                hasProperty("name", is(FIRST_OFFER)),
                hasProperty("name", is(SECOND_OFFER))
        ));
    }

    @Test
    public void shouldReplaceItemByMskuOnBulkCreate() {
        CartList basketList = storageCartService.replaceCartListOwnerId(
                userContext,
                createCartFor(userContext, generateItemWith(FIRST_OFFER, offer -> offer.setMsku(42L)))
        ).getResult();

        assertThat(basketList.getItems(), not(empty()));

        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(createCartFor(
                userContext,
                generateItemWith(SECOND_OFFER, offer -> offer.setMsku(42L))
        )), false);
        basketList = extractBasketList(storageCartService.getListsOwnerId(userContext));

        assertThat(basketList.getItems(), hasSize(1));
        assertThat(basketList.getItems(), hasItem(hasProperty("name", is(SECOND_OFFER))));
    }

    @Test
    public void shouldNotReplaceItemByMskuOnBulkCreateIfSetMultiOffers() {
        CartList basketList = storageCartService.replaceCartListOwnerId(
                userContext,
                createCartFor(userContext, generateItemWith(FIRST_OFFER, offer -> offer.setMsku(42L)))
        ).getResult();

        assertThat(basketList.getItems(), not(empty()));

        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(createCartFor(
                userContext,
                generateItemWith(SECOND_OFFER, offer -> offer.setMsku(42L))
        )), true);

        basketList = extractBasketList(storageCartService.getListsOwnerId(userContext));

        assertThat(basketList.getItems(), hasSize(2));
        assertThat(basketList.getItems(), hasItems(
                hasProperty("name", is(FIRST_OFFER)),
                hasProperty("name", is(SECOND_OFFER))
        ));
    }

    @Test
    public void shouldCreateEmptyBasket() {
        assertEquals(1, storageCartService.getListsOwnerId(userContext).getResult().size());
    }

    @Test
    public void shouldCreateSingleItemWithoutMskuCheck() {
        CartList basketList = extractBasketList(storageCartService.getListsOwnerId(userContext));

        assertThat(basketList.getItems(), empty());

        storageCartService.createItemNoMskuCheck(userContext, generateItem(FIRST_OFFER));

        basketList = extractBasketList(storageCartService.getListsOwnerId(userContext));
        assertThat(basketList.getItems(), hasSize(1));
    }

    @Test
    public void shouldCreateSingleItemWithMskuCheck() {
        CartList basketList = extractBasketList(storageCartService.getListsOwnerId(userContext));

        assertThat(basketList.getItems(), empty());

        storageCartService.createOrReplaceItemByMsku(userContext, generateItem(FIRST_OFFER));

        basketList = extractBasketList(storageCartService.getListsOwnerId(userContext));
        assertThat(basketList.getItems(), hasSize(1));
    }

    @Test
    public void shouldCreateItemsBulked() {
        CartList basketList = extractBasketList(storageCartService.getListsOwnerId(userContext));

        assertThat(basketList.getItems(), empty());


        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(createCartFor(
                userContext,
                generateItem(FIRST_OFFER),
                generateItem(SECOND_OFFER)
        )), true);

        basketList = extractBasketList(storageCartService.getListsOwnerId(userContext));
        assertThat(basketList.getItems(), hasSize(2));
    }

    @Test
    public void shouldStoreItemKind2Params() {
        CartList basketList = extractBasketList(storageCartService.getListsOwnerId(userContext));

        assertThat(basketList.getItems(), empty());

        String kind2Params = "[{\"id\":\"13887626\",\"kind\":2,\"name\":\"Цвет\",\"noffers\":1,\"position\":9," +
                "\"subType\":\"color\",\"type\":\"enum\",\"xslname\":\"color_glob\",\"values\":[{\"initialFound\":1," +
                "\"group\":\"красный\",\"found\":1,\"value\":\"красный\",\"code\":\"#FF0000\",\"id\":\"13891866\"}]," +
                "\"units\":[]}]";

        storageCartService.createOrReplaceItemByMsku(userContext, generateItemWith(FIRST_OFFER, offer -> {
            offer.setDesc("Отличный айФончик");
            offer.setFee("29834cu8vy4b");
            offer.setPictureUrl("test.url");

            offer.setKind2Params(kind2Params);
        }));

        basketList = extractBasketList(storageCartService.getListsOwnerId(userContext));

        assertThat(basketList.getItems(), hasSize(1));
        assertThat(basketList.getItems(), hasItem(
                allOf(
                        hasProperty("kind2Params", is(kind2Params)),
                        hasProperty("fee", is("29834cu8vy4b")),
                        hasProperty("desc", is("Отличный айФончик")),
                        hasProperty("pictureUrl", is("test.url"))
                )
        ));
    }

    @Test
    public void shouldUpdateItemCount() {
        CartList basketList = storageCartService.replaceCartListOwnerId(
                userContext,
                createCartFor(userContext, generateItem(FIRST_OFFER))
        ).getResult();

        assertThat(basketList.getItems(), not(empty()));

        storageCartService.updateItemCountOwnerId(userContext, itemOf(basketList, keyOf(FIRST_OFFER)).getId(), 5);

        basketList = extractBasketList(storageCartService.getListsOwnerId(userContext));

        assertThat(basketList.getItems(), not(empty()));
        assertThat(itemOf(basketList, keyOf(FIRST_OFFER)).getCount(), comparesEqualTo(5));
    }

    @Test
    public void shouldBulkDeleteBasketItems() {
        CartList basketList = storageCartService.replaceCartListOwnerId(
                userContext,
                createCartFor(
                        userContext,
                        generateItem(FIRST_OFFER),
                        generateItem(SECOND_OFFER)
                )
        ).getResult();

        assertThat(basketList.getItems(), hasSize(2));

        storageCartService.deleteItems(userContext, basketList.getItems().stream()
                .map(CartItem::getId)
                .collect(toList()));

        basketList = extractBasketList(storageCartService.getListsOwnerId(userContext));

        assertThat(basketList.getItems(), empty());
    }

    @Test
    public void shouldLimitListItems() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            for (int i = 0; i < limitItems; i++) {
                storageCartService.createOrReplaceItemByMsku(userContext, generateItem(i + ""));
            }

            storageCartService.createOrReplaceItemByMsku(userContext, generateItem(limitItems + ""));
        });
    }

    @Test
    public void shouldLimitBulkCreate() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            List<ItemOffer> items = new ArrayList<>(limitItems + 1);
            for (int i = 0; i <= limitItems; i++) {
                items.add(generateItem(i + ""));
            }

            storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(createCartFor(
                    userContext,
                    items
            )), true);
        });
    }


    @Test
    public void shouldNotFailOnItemsWithoutMskuOnBulkCreation() {
        UserContext uc = UserContext.of(OwnerKey.of(
                Color.WHITE, UID, "" + rnd.nextLong(1, Integer.MAX_VALUE)));
        storageCartService.bulkUpdateItemsOwnerId(
                uc, List.of(createCartFor(
                        userContext,
                        generateItem(FIRST_OFFER, 1, 1),
                        generateItemWith(SECOND_OFFER, itemOffer -> itemOffer.setMsku(null))
                )), false);

        assertThat(
                extractBasketList(storageCartService.getListsOwnerId(uc)).getItems(),
                hasSize(2)
        );
    }

}
