package ru.yandex.market.pers.basket.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.Cookie;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.basket.PersBasketTest;
import ru.yandex.market.pers.basket.cache.BasketCache;
import ru.yandex.market.pers.basket.model.BasketReferenceItem;
import ru.yandex.market.pers.basket.service.BasketService;
import ru.yandex.market.pers.basket.utils.BasketApiUtils;
import ru.yandex.market.pers.list.mock.BasketItemsMvcMock;
import ru.yandex.market.pers.list.mock.BasketOldMvcMocks;
import ru.yandex.market.pers.list.mock.BasketV2MvcMocks;
import ru.yandex.market.pers.list.model.BasketItem;
import ru.yandex.market.pers.list.model.BasketItemType;
import ru.yandex.market.pers.list.model.BasketOwner;
import ru.yandex.market.pers.list.model.PersBasketConst;
import ru.yandex.market.pers.list.model.UserList;
import ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor;
import ru.yandex.market.util.ListUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.basket.controller.AbstractBasketControllerTest.MODEL_ID;
import static ru.yandex.market.pers.basket.controller.AbstractBasketControllerTest.OFFER_ID;
import static ru.yandex.market.pers.basket.controller.AbstractBasketControllerTest.SKU_ID;
import static ru.yandex.market.pers.basket.controller.AbstractBasketControllerTest.assertContainsAll;
import static ru.yandex.market.pers.basket.controller.AbstractBasketControllerTest.generateItem;
import static ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor.BLUE;
import static ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor.WHITE;
import static ru.yandex.market.pers.list.model.v2.enums.ReferenceType.OFFER;
import static ru.yandex.market.pers.list.model.v2.enums.ReferenceType.PRODUCT;
import static ru.yandex.market.pers.list.model.v2.enums.ReferenceType.SKU;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         31.05.16
 */
public class BasketControllerTest extends PersBasketTest {
    private static final long UID = 23427062L;
    private static final String UUID = "sdfldsfjsd33223";
    private static final String YANDEXUID = "3928478648324323";
    public static final BasketOwner OWNER_UID = BasketOwner.fromUid(UID);

    private static final String REGION_ID = "213";

    @Autowired
    private BasketService basketService;
    @Autowired
    private BasketCache basketCache;

    @Autowired
    private BasketOldMvcMocks basketOldMvc;
    @Autowired
    private BasketV2MvcMocks basketV2Mvc;
    @Autowired
    private BasketItemsMvcMock basketItemsMvc;

    @Test
    public void testAddItems() throws Exception {
        List<BasketItem> itemsToAdd = generateAllKindsOfItems();
        BasketOwner owner = BasketOwner.fromUid(UID);
        basketOldMvc.addItems(owner, itemsToAdd);

        // check query
        String itemsJson = basketOldMvc.getItemsMvc(owner);
        String expectedJson = "[{\"id\":111,\"type\":\"MODEL\",\"displayName\":\"Model item\",\"labelIds\":[],\"hid\":1,\"uid\":23427062,\"createDate\":1597767817000,\"modelId\":1}," +
            "{\"id\":111,\"type\":\"GROUP\",\"displayName\":\"Group item\",\"labelIds\":[],\"hid\":2,\"uid\":23427062,\"createDate\":1597767817000,\"groupId\":2}," +
            "{\"id\":111,\"type\":\"CLUSTER\",\"displayName\":\"Cluster item\",\"labelIds\":[],\"hid\":3,\"uid\":23427062,\"createDate\":1597767817000,\"clusterId\":3}," +
            "{\"id\":111,\"type\":\"OFFER\",\"displayName\":\"Offer item\",\"labelIds\":[],\"hid\":4,\"uid\":23427062,\"offerId\":\"offer-id\",\"createDate\":1597767817000,\"modelId\":4}]";
        JSONAssert.assertEquals(expectedJson, itemsJson,
            new CustomComparator(JSONCompareMode.NON_EXTENSIBLE, // ony order, strict fields
                new Customization("[*].id", (o1, o2) -> true),
                new Customization("[*].createDate", (o1, o2) -> true)));

        // check added in pgaas
        String itemsV2 = basketV2Mvc.getItemsMvc(owner, MarketplaceColor.WHITE);
        // hint: expected json should not contain unique values in skipped fields (1h lost here)
        String expectedV2 = "{\"result\":{\"total\":4,\"items\":[\n" +
            "{\"id\":111,\"owner_id\":999,\"rgb\":\"white\",\"reference_type\":\"offer\",\"reference_id\":\"offer-id\",\"title\":\"Offer item\",\"added_at\":\"1\",\"secondary_references\":[{\"type\":\"hid\",\"id\":\"4\"},{\"type\":\"modelId\",\"id\":\"4\"},{\"type\":\"foundInReport\",\"id\":\"0\"}]},\n" +
            "{\"id\":111,\"owner_id\":999,\"rgb\":\"white\",\"reference_type\":\"product\",\"reference_id\":\"3\",\"title\":\"Cluster item\",\"added_at\":\"1\",\"secondary_references\":[{\"type\":\"hid\",\"id\":\"3\"},{\"type\":\"modelType\",\"id\":\"CLUSTER\"},{\"type\":\"foundInReport\",\"id\":\"0\"}]},\n" +
            "{\"id\":111,\"owner_id\":999,\"rgb\":\"white\",\"reference_type\":\"product\",\"reference_id\":\"2\",\"title\":\"Group item\",\"added_at\":\"1\",\"secondary_references\":[{\"type\":\"hid\",\"id\":\"2\"},{\"type\":\"modelType\",\"id\":\"GROUP\"},{\"type\":\"foundInReport\",\"id\":\"0\"}]},\n" +
            "{\"id\":111,\"owner_id\":999,\"rgb\":\"white\",\"reference_type\":\"product\",\"reference_id\":\"1\",\"title\":\"Model item\",\"added_at\":\"1\",\"secondary_references\":[{\"type\":\"hid\",\"id\":\"1\"},{\"type\":\"modelType\",\"id\":\"MODEL\"},{\"type\":\"foundInReport\",\"id\":\"0\"}]}]}}";
        JSONAssert.assertEquals(expectedV2, itemsV2,
            new CustomComparator(JSONCompareMode.NON_EXTENSIBLE, // ony order, strict fields
                new Customization("result.items[*].id", (o1, o2) -> true),
                new Customization("result.items[*].owner_id", (o1, o2) -> true),
                new Customization("result.items[*].added_at", (o1, o2) -> true)));
    }

    @Test
    public void testBackwardsCompatibility() throws Exception {
        List<BasketReferenceItem> savedItems = List.of(
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, PRODUCT, MODEL_ID)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, PRODUCT, MODEL_ID + 1)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, OFFER, OFFER_ID)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(BLUE, SKU, SKU_ID)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, SKU, SKU_ID + 1))
        );

        assertEquals(3, basketOldMvc.getItemsCount(OWNER_UID));
        assertEquals(3, basketV2Mvc.getItemsCount(OWNER_UID, WHITE, ok));
        assertEquals(2, basketV2Mvc.getItemsCount(OWNER_UID, BLUE, ok));

        assertContainsAll(ListUtils.toList(basketOldMvc.getItems(OWNER_UID), BasketApiUtils::convertItemToV2),
            List.of(savedItems.get(0), savedItems.get(1), savedItems.get(2)));
        assertContainsAll(basketV2Mvc.getItems(OWNER_UID, WHITE),
            List.of(savedItems.get(0), savedItems.get(1), savedItems.get(2)));
        assertContainsAll(basketV2Mvc.getItems(OWNER_UID, BLUE), List.of(savedItems.get(3), savedItems.get(4)));
    }

    @Test
    public void testAddItemsYandexUid() throws Exception {
        List<BasketItem> itemsToAdd = generateAllKindsOfItems();
        BasketOwner owner = BasketOwner.fromYandexUid(YANDEXUID);
        List<BasketItem> savedItems = basketOldMvc.addItems(owner, itemsToAdd);
        assertEquals(itemsToAdd.size(), savedItems.size());
        assertNotNull(savedItems.get(0).getId());

        // check query
        List<BasketItem> foundItems = basketOldMvc.getItems(owner);
        assertEquals(itemsToAdd.size(), foundItems.size());

        // check added in pgaas
        List<BasketReferenceItem> foundItemsV2 = basketV2Mvc.getItems(owner, MarketplaceColor.WHITE);
        assertEquals(itemsToAdd.size(), foundItemsV2.size());

        // try delete
        basketOldMvc.deleteItemsMvc(owner, List.of(savedItems.get(0).getId()));
        foundItems = basketOldMvc.getItems(owner);
        assertEquals(itemsToAdd.size() - 1, foundItems.size());
    }

    @Test
    public void testOwnerCacheWorksAsExpected() {
        // no owner in cache on start
        assertEquals(0, mockedCacheMap.size());

        BasketOwner owner = BasketOwner.fromYandexUid(YANDEXUID);
        Long ownerId = basketCache.getOwnerId(owner);
        assertNull(ownerId);

        // cached null object
        assertEquals(1, mockedCacheMap.size());

        // save item
        Long ownerIdAdded = basketCache.getOrAddOwnerId(owner);
        assertNotNull(ownerIdAdded);

        // cache cleaned
        assertEquals(0, mockedCacheMap.size());

        // owner cached properly
        ownerId = basketCache.getOwnerId(owner);
        assertEquals(ownerIdAdded, ownerId);

        // owner is cached now
        assertEquals(1, mockedCacheMap.size());
    }

    @Test
    public void testVeryComplexCacheOwnerError() {
        // owner cache should be reset when owner creates

        // fill owner cache with NONE since it does not exists
        BasketOwner owner = BasketOwner.fromYandexUid(YANDEXUID+1);
        Long ownerId = basketCache.getOwnerId(owner);
        assertNull(ownerId);
        ownerId = basketCache.getOwnerId(owner);
        assertNull(ownerId);

        // save items - reset cache
        List<BasketItem> savedItems = basketOldMvc.addItems(owner, generateAllKindsOfItems());
        assertEquals(4, savedItems.size());

        // try delete - read created owner from cache
        basketOldMvc.deleteItemsMvc(owner, List.of(savedItems.get(0).getId()));
        List<BasketItem> foundItems = basketOldMvc.getItems(owner);
        assertEquals(3, foundItems.size());

        // when owner cache does not reset - this remove would do nothing since there is no owner in cache
    }

    @Test
    public void testAddItemNoName() throws Exception {
        List<BasketItem> itemsToAdd = generateItems(1);
        itemsToAdd.get(0).setDisplayName(null);

        BasketOwner owner = BasketOwner.fromUid(UID);
        basketOldMvc.addItems(owner, itemsToAdd);

        assertEquals(1, basketOldMvc.getItems(owner).size());
    }

    @Test
    public void testDeleteItemsChanged() throws Exception {
        int itemsToAdd = 3;
        BasketOwner owner = BasketOwner.fromUid(UID);
        basketOldMvc.addItems(owner, generateItems(itemsToAdd));

        // check added to old
        List<BasketItem> originalItems = basketOldMvc.getItems(owner);
        List<Long> basketItemsIds = ListUtils.toList(originalItems, BasketItem::getId);
        assertEquals(itemsToAdd, basketItemsIds.size());

        // check added to v2
        List<Long> basketV2ItemsIds = ListUtils.toList(basketV2Mvc.getItems(owner, MarketplaceColor.WHITE), BasketReferenceItem::getId);
        assertEquals(itemsToAdd, basketItemsIds.size());

        // delete item
        BasketItem itemToDelete = originalItems.get(0);
        Long itemIdToDelete = itemToDelete.getId();
        basketOldMvc.deleteItemsMvc(owner, List.of(itemIdToDelete));
    }

    @Test
    public void testDeleteItems() throws Exception {
        int itemsToAdd = 3;
        BasketOwner owner = BasketOwner.fromUid(UID);
        basketOldMvc.addItems(owner, generateItems(itemsToAdd));

        // check added to old
        List<BasketItem> originalItems = basketOldMvc.getItems(owner);
        List<Long> basketItemsIds = ListUtils.toList(originalItems, BasketItem::getId);
        assertEquals(itemsToAdd, basketItemsIds.size());

        // check added to v2
        List<Long> basketV2ItemsIds = ListUtils.toList(basketV2Mvc.getItems(owner, MarketplaceColor.WHITE), BasketReferenceItem::getId);
        assertEquals(itemsToAdd, basketItemsIds.size());

        // delete item
        BasketItem itemToDelete = originalItems.get(0);
        Long itemIdToDelete = itemToDelete.getId();
        basketOldMvc.deleteItemsMvc(owner, List.of(itemIdToDelete));

        // check only expected item deleted
        List<Long> basketItemsAfterDelete = ListUtils.toList(basketOldMvc.getItems(owner), BasketItem::getId);
        assertEquals(itemsToAdd - 1, basketItemsAfterDelete.size());
        assertTrue(basketItemsIds.containsAll(basketItemsAfterDelete));

        // check item also deleted from pgaas
        List<BasketReferenceItem> itemsV2AfterDelete = basketV2Mvc.getItems(owner, MarketplaceColor.WHITE);
        Set<Long> itemIdsV2AfterDelete = itemsV2AfterDelete.stream()
            .map(BasketReferenceItem::getReferenceId)
            .map(Long::parseLong)
            .collect(Collectors.toSet());
        assertTrue(ListUtils.toSet(originalItems, BasketItem::getModelId).containsAll(itemIdsV2AfterDelete));
        assertFalse(itemIdsV2AfterDelete.contains(itemToDelete.getModelId()));
    }

    @Test
    public void testEnsureFieldsSize() throws Exception {
        BasketOwner owner = BasketOwner.fromUid(UID);
        List<BasketItem> items = generateItems(1);
        items.get(0).setDisplayName(StringUtils.repeat("d", 150));
        items.get(0).setReferer(StringUtils.repeat("r", 40));

        basketOldMvc.addItems(owner, items);
        List<BasketItem> returnedItems = basketOldMvc.getItems(owner);
        assertEquals(PersBasketConst.DISPLAY_NAME_SIZE, returnedItems.get(0).getDisplayName().length());
    }

    @Test
    public void testMergeToUnauthorized() throws Exception {
        BasketOwner ownerUuid = BasketOwner.fromUuid(UUID);
        BasketOwner ownerYandexUid = BasketOwner.fromYandexUid(YANDEXUID);

        basketOldMvc.mergeItemsMvc(
            ownerUuid,
            ownerYandexUid,
            status().isBadRequest()
        );
    }

    @Test
    public void testMergeToAuthorized() throws Exception {
        BasketOwner ownerUuid = BasketOwner.fromUuid(UUID);
        BasketOwner ownerUid = BasketOwner.fromUid(UID);

        int itemsCount = 5;
        List<BasketItem> items = generateItems(itemsCount);

        // create items for UUID
        basketOldMvc.addItems(ownerUuid, items);

        // get items for UUID
        String itemsString = basketOldMvc.getItemsMvc(ownerUuid);
        assertNotEquals("[]", itemsString);

        // no items for UID
        assertEquals("[]", basketOldMvc.getItemsMvc(ownerUid));

        // merge items from UUID to UID
        basketOldMvc.mergeItemsMvc(ownerUuid, ownerUid);

        // items for UUID are removed
        assertEquals(0, basketOldMvc.getItems(ownerUuid).size());

        // items for UID is the same as for UUID
        String newItemsString = basketOldMvc.getItemsMvc(ownerUid);

        String oldStr = itemsString
            .replaceAll("id\":[^\",}]+", "id\":1")
            .replaceAll("labelIds\":\\[[^\\]]+", "labelIds\":[")
            .replaceAll("createDate\":[^\",}]+", "createDate\":1");

        String newStr = newItemsString
            .replaceAll("id\":[^\",}]+", "id\":1")
            .replaceAll("labelIds\":\\[[^\\]]+", "labelIds\":[")
            .replaceAll("createDate\":[^\",}]+", "createDate\":1");

        JSONAssert.assertEquals(oldStr, newStr, false);

        // simple test for merge in pgaas
        assertEquals(0, basketV2Mvc.getItems(ownerUuid, MarketplaceColor.WHITE).size());
        assertEquals(items.size(), basketV2Mvc.getItems(ownerUid, MarketplaceColor.WHITE).size());
    }

    @Test
    public void testMergeFromAuthorized() throws Exception {
        basketOldMvc.mergeItemsMvc(
            BasketOwner.fromUid(UID),
            BasketOwner.fromUuid(UUID),
            status().isBadRequest()
        );
    }

    @Test
    public void testSync() throws Exception {
        BasketOwner owner = BasketOwner.fromUid(UID);

        int itemsCount = 5;
        List<BasketItem> items = generateItems(itemsCount);

        // create items for UUID
        basketOldMvc.syncItemsMvc(owner, items);

        assertEquals(itemsCount, basketOldMvc.getItems(owner).size());

        // test for sync in pgaas
        assertEquals(items.size(), basketV2Mvc.getItems(owner, MarketplaceColor.WHITE).size());
    }

    @Test
    public void testAddItemsWithRegion() throws Exception {
        BasketOwner owner = BasketOwner.fromUid(UID);

        int itemsToAdd = 4;

        // create items for UUID
        basketOldMvc.addItemsMvc(owner, generateItems(itemsToAdd),
            builder -> builder.cookie(new Cookie("currentRegionId", REGION_ID)));

        List<BasketItem> basketItems = basketOldMvc.getItems(owner);


        basketItems.forEach(item -> {
            // since field is ignored
            Assert.assertNull(item.getRegion());
        });

        // test in pgaas
        basketV2Mvc.getItems(owner, MarketplaceColor.WHITE).forEach(item-> {
                // actually awailable in pgaas
                assertNotNull(item.getRegionId());
                assertEquals(REGION_ID, item.getRegionId().toString());
            }
        );
    }

    @Test
    public void testMergeItemsWithRegion() throws Exception {
        BasketOwner ownerFrom = BasketOwner.fromUuid(UUID);

        int count = 5;
        List<BasketItem> items = generateItems(count);

        // create items for UUID
        basketOldMvc.addItemsMvc(ownerFrom, items,
            builder -> builder.cookie(new Cookie("currentRegionId", REGION_ID)));

        List<BasketItem> itemsForUuid = basketOldMvc.getItems(ownerFrom);
        assertEquals(count, itemsForUuid.size());
        itemsForUuid.forEach(item ->
            // no region in field since ignored
            Assert.assertNull(item.getRegion()));

        basketV2Mvc.getItems(ownerFrom, MarketplaceColor.WHITE).forEach(item->
                // actually saved
                Assert.assertEquals(Integer.valueOf(REGION_ID), item.getRegionId())
        );

        BasketOwner ownerTo = BasketOwner.fromUid(UID);
        basketOldMvc.mergeItemsMvc(ownerFrom, ownerTo);

        List<BasketItem> itemsForUid = basketOldMvc.getItems(ownerTo);
        assertEquals(count, itemsForUid.size());
        itemsForUid.forEach(item ->
            // no region in field since ignored
            Assert.assertNull(item.getRegion())
        );

        basketV2Mvc.getItems(ownerTo, MarketplaceColor.WHITE).forEach(item->
            // actually merged
            Assert.assertEquals(Integer.valueOf(REGION_ID), item.getRegionId())
        );

    }

    /**
     * 1) Добавление, редактирование, удаление элемента списка.
     * Повторное добавление не должно плодить itemы
     * 2) По списку itemов вернуть те, которые в списке
     */
    @Test
    public void testItems() {
        BasketOwner owner = BasketOwner.fromUid(UID);

        // add
        int startModelId = 1;
        BasketItem item = generateItems(BasketItemType.MODEL, startModelId, 1).get(0);
        BasketItem itemGroup = generateItems(BasketItemType.GROUP, startModelId + 1, 1).get(0);

        List<BasketItem> createdItems = basketOldMvc.addItems(owner, Collections.singletonList(item));
        assertEquals(1, createdItems.size());

        item.setOwnerId(createdItems.get(0).getOwnerId());
        assertTrue(createdItems.contains(item));

        // add again, check not added twice
        basketOldMvc.addItems(owner, Collections.singletonList(item));
        List<BasketItem> userItems = basketOldMvc.getItems(owner);
        assertEquals(1, userItems.size());
        assertTrue(userItems.contains(item));

        // test can add another item
        createdItems = basketOldMvc.addItems(owner, Collections.singletonList(itemGroup));
        assertEquals(1, createdItems.size());
        assertTrue(createdItems.contains(itemGroup));

        // check actual items
        userItems = basketOldMvc.getItems(owner);
        assertEquals(2, userItems.size());
        assertTrue(userItems.containsAll(List.of(item, itemGroup)));

        // test delete
        basketOldMvc.deleteItemsMvc(owner, ListUtils.toList(userItems, BasketItem::getId));
        userItems = basketOldMvc.getItems(owner);
        assertTrue(userItems.isEmpty());
    }

    @Test
    public void testItemsExisting() {
        BasketOwner owner = BasketOwner.fromUid(UID);

        List<BasketItem> knownItems = generateAllKindsOfItems();
        basketOldMvc.addItems(owner, knownItems);

        // items not from list
        List<BasketItem> unknownItems = generateItems(BasketItemType.GROUP, 100, 1);

        assertEquals(0, basketOldMvc.getExistingItems(owner, unknownItems).size());

        // any existing items from list
        knownItems.forEach(item -> {
            List<BasketItem> existing = basketOldMvc.getExistingItems(owner,
                Collections.singletonList(item));
            assertEquals(1, existing.size());
            assertTrue(existing.get(0).getId() > 0);
        });

        // all known items
        List<BasketItem> existing = basketOldMvc.getExistingItems(owner, knownItems);
        assertEquals(knownItems.size(), existing.size());
        assertFalse(existing.stream().anyMatch(x -> x.getId() == null));
    }

    /**
     * Пустой незалогиненный, заполненный логин
     */
    @Test
    public void testSyncEmpty() throws Exception {
        BasketOwner owner = BasketOwner.fromUid(UID);
        basketOldMvc.addItems(owner, generateAllKindsOfItems());

        UserList oldList = basketOldMvc.getUserList(owner);
        UserList newList = new UserList();
        UserList syncList = basketOldMvc.syncItems(owner, newList);
        assertEquals(oldList, syncList);

        assertEquals(oldList.getItems().size(), syncList.getItems().size());
        assertTrue(syncList.getItems().containsAll(oldList.getItems()));
    }

    /**
     * Заполненный незалогиненный, пустой логин
     */
    @Test
    public void testSyncWithEmpty() throws Exception {
        BasketOwner owner = BasketOwner.fromUid(UID);

        UserList oldList = basketOldMvc.getUserList(owner);
        assertTrue(oldList.getItems().isEmpty());
        assertTrue(oldList.getLabels().isEmpty());

        UserList newList = new UserList();
        newList.setItems(generateAllKindsOfItems());
        UserList syncList = basketOldMvc.syncItems(owner, newList);

        UserList expected = new UserList();
        expected.setOwnerId(basketService.getOwnerId(owner));
        expected.setUserId(UID);
        expected.setItems(new ArrayList<>());
        expected.getItems().addAll(newList.getItems());

        newList.setOwnerId(basketService.getOwnerId(owner));
        newList.setUserId(UID);
        Collections.reverse(newList.getItems());

        assertEquals(newList.getItems().size(), syncList.getItems().size());
        assertTrue(syncList.getItems().containsAll(newList.getItems()));
        assertEquals(basketService.getOwnerId(owner).longValue(), syncList.getOwnerId());
        assertEquals(UID, syncList.getUserId().longValue());
        assertEquals(0, syncList.getLabels().size());
    }

    /**
     * Заполнены оба, но без пересечения
     */
    @Test
    public void testSyncDifferent() throws Exception {
        BasketOwner owner = BasketOwner.fromUid(UID);
        basketOldMvc.addItems(owner, generateAllKindsOfItems());

        UserList oldList = basketOldMvc.getUserList(owner);
        UserList newList = new UserList();
        newList.setItems(generateItems(BasketItemType.GROUP, 100, 2));

        UserList syncList = basketOldMvc.syncItems(owner, newList);

        ArrayList<BasketItem> expectedList = new ArrayList<>();
        expectedList.addAll(oldList.getItems());
        expectedList.addAll(newList.getItems());

        assertEquals(expectedList.size(), syncList.getItems().size());
        assertTrue(syncList.getItems().containsAll(expectedList));
    }

    /**
     * Заполнены оба, пересечение по элементам
     */
    @Test
    public void testSyncBoth() throws Exception {
        BasketOwner owner = BasketOwner.fromUid(UID);
        basketOldMvc.addItems(owner, generateAllKindsOfItems());

        UserList oldList = basketOldMvc.getUserList(owner);
        UserList newList = new UserList();
        newList.setItems(generateItems(BasketItemType.GROUP, 100, 2));

        // небольшое пересечение - 1 элемент есть в обоих множествах
        newList.getItems().add(oldList.getItems().get(0));

        UserList syncList = basketOldMvc.syncItems(owner, newList);

        ArrayList<BasketItem> expectedList = new ArrayList<>();
        expectedList.addAll(oldList.getItems());
        expectedList.addAll(newList.getItems());

        // -1 т.к. один в пересечении
        assertEquals(expectedList.size() - 1, syncList.getItems().size());
        assertTrue(syncList.getItems().containsAll(expectedList));
    }

    private List<BasketItem> generateItems(int size) {
        int startId = Math.abs(RND.nextInt());
        return generateItems(BasketItemType.MODEL, startId, size);
    }

    private List<BasketItem> generateItems(BasketItemType type, int startModelId, int size) {
        int startId = startModelId;
        int hid = 1;
        List<BasketItem> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            BasketItem item = new BasketItem(type);
            item.setHid(hid++);
            item.setModelId(startId++);
            item.setDisplayName(java.util.UUID.randomUUID().toString());
            item.setUid(UID);
            result.add(item);
        }
        return result;
    }

    private List<BasketItem> generateAllKindsOfItems() {
        List<BasketItem> result = new ArrayList<>();

        BasketItem item = new BasketItem(BasketItemType.MODEL);
        item.setHid(1);
        item.setModelId(1);
        item.setDisplayName("Model item");
        result.add(item);

        item = new BasketItem(BasketItemType.GROUP);
        item.setHid(2);
        item.setModelId(2);
        item.setDisplayName("Group item");
        result.add(item);

        item = new BasketItem(BasketItemType.CLUSTER);
        item.setHid(3);
        item.setModelId(3);
        item.setDisplayName("Cluster item");
        result.add(item);

        item = new BasketItem(BasketItemType.OFFER);
        item.setHid(4);
        item.setModelId(4);
        item.setOfferId("offer-id");
        item.setDisplayName("Offer item");
        result.add(item);

        return result;
    }
}
