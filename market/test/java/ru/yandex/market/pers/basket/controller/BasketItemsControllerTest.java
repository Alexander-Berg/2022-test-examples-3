package ru.yandex.market.pers.basket.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClientResponseException;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.pers.basket.BasketCollectionsRequestDto;
import ru.yandex.market.pers.basket.CollectionsClient;
import ru.yandex.market.pers.basket.collections.CollectionsService;
import ru.yandex.market.pers.basket.collections.OperationType;
import ru.yandex.market.pers.basket.model.BasketCrTimeToken;
import ru.yandex.market.pers.basket.model.BasketItemFilter;
import ru.yandex.market.pers.basket.model.BasketReferenceItem;
import ru.yandex.market.pers.basket.model.ItemReference;
import ru.yandex.market.pers.basket.model.ResultDto;
import ru.yandex.market.pers.basket.model.SecondaryReference;
import ru.yandex.market.pers.basket.utils.BasketApiUtils;
import ru.yandex.market.pers.list.mock.BasketItemsMvcMock;
import ru.yandex.market.pers.list.model.v2.enums.SecondaryReferenceType;
import ru.yandex.market.report.ReportService;
import ru.yandex.market.report.model.Category;
import ru.yandex.market.report.model.Model;
import ru.yandex.market.report.model.Offer;
import ru.yandex.market.report.model.OfferPrices;
import ru.yandex.market.report.model.ProductType;
import ru.yandex.market.util.FormatUtils;
import ru.yandex.market.util.ListUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor.BLUE;
import static ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor.RED;
import static ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor.WHITE;
import static ru.yandex.market.pers.list.model.v2.enums.ReferenceType.FEED_GROUP_ID_HASH;
import static ru.yandex.market.pers.list.model.v2.enums.ReferenceType.OFFER;
import static ru.yandex.market.pers.list.model.v2.enums.ReferenceType.PRODUCT;
import static ru.yandex.market.pers.list.model.v2.enums.ReferenceType.SKU;
import static ru.yandex.market.pers.list.model.v2.enums.SecondaryReferenceType.FOUND_IN_REPORT;
import static ru.yandex.market.pers.list.model.v2.enums.SecondaryReferenceType.HID;
import static ru.yandex.market.pers.list.model.v2.enums.SecondaryReferenceType.MODEL_TYPE;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 04.09.2020
 */
public class BasketItemsControllerTest extends AbstractBasketControllerTest {
    @Autowired
    private BasketItemsMvcMock basketItemsMvc;

    @Autowired
    private ReportService reportService;

    @Autowired
    private CollectionsService collectionsService;

    @Autowired
    private CollectionsClient collectionsClient;

    @Test
    public void testLoadInvalidSize() {
        basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, PRODUCT, MODEL_ID));
        basketItemsMvc.getItemsMvc(OWNER_UID, null, BasketItemsController.MAX_PAGE_SIZE + 1, ERR_4XX);
        basketItemsMvc.getItemsMvc(OWNER_UID, null, 0, ERR_4XX);
    }

    @Test
    public void testPagingSameTime() {
        int itemsCount = 7;
        List<BasketReferenceItem> savedItems = IntStream.range(0, itemsCount)
            .mapToObj(idx -> basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, PRODUCT, MODEL_ID+idx)))
            .collect(Collectors.toList());

        // set same time for all records
        pgaasJdbcTemplate.update("update basket_items set added_at = now() where rgb = 0");

        int pageSize = 3;
        BasketCrTimeToken token = null;

        // test token and page contents
        var result = basketItemsMvc.getItemsFull(OWNER_UID, token, pageSize);
        assertEquals(3, result.getItems().size());
        assertTrue(result.hasMore());
        assertNotNull(result.getToken());
        assertEquals(savedItems.get(2).getId(), result.getToken().getLastId());
        assertNotNull(result.getToken().getLastCrTime());
        assertTrue(ListUtils.toSet(savedItems.subList(0, 3), BasketReferenceItem::getId).containsAll(
            ListUtils.toSet(result.getItems(), BasketReferenceItem::getId)
        ));

        // second page
        result = basketItemsMvc.getItemsFull(OWNER_UID, result.getToken(), pageSize);
        assertEquals(3, result.getItems().size());
        assertTrue(result.hasMore());
        assertNotNull(result.getToken());
        assertEquals(savedItems.get(5).getId(), result.getToken().getLastId());
        assertNotNull(result.getToken().getLastCrTime());
        assertTrue(ListUtils.toSet(savedItems.subList(3, 6), BasketReferenceItem::getId).containsAll(
            ListUtils.toSet(result.getItems(), BasketReferenceItem::getId)
        ));

        result = basketItemsMvc.getItemsFull(OWNER_UID, result.getToken(), pageSize);
        assertEquals(1, result.getItems().size());
        assertFalse(result.hasMore());
        assertNotNull(result.getToken());
        assertNull(result.getToken().getLastId());
        assertNotNull(result.getToken().getLastCrTime());
        // current contract - last time == iso time of last element
        assertEquals(getLast(result.getItems()).getAddedAtInternal().toString(), result.getToken().getLastCrTime());
        assertTrue(ListUtils.toSet(savedItems.subList(6, 7), BasketReferenceItem::getId).containsAll(
            ListUtils.toSet(result.getItems(), BasketReferenceItem::getId)
        ));

        // run once more to check token not changes then none received
        BasketCrTimeToken lastToken = result.getToken();
        result = basketItemsMvc.getItemsFull(OWNER_UID, result.getToken(), pageSize);
        assertEquals(0, result.getItems().size());
        assertFalse(result.hasMore());
        assertNotNull(result.getToken());
        assertEquals(lastToken.getLastId(), result.getToken().getLastId());
        assertEquals(lastToken.getLastCrTime(), result.getToken().getLastCrTime());
    }

    private <T> T getLast(List<T> data){
        return  data == null || data.isEmpty() ? null : data.get(data.size() - 1);
    }

    private void shiftDateSec(long id, int time) {
        pgaasJdbcTemplate.update(
            "update basket_items " +
                "set added_at = added_at + ? * interval '1' second  " +
                "where id = ?",
            time, id);
    }

    @Test
    public void testPagingComplex() {
        // case to check paging when there are same time in some pages and some are not.
        // test here time shift in seconds
        // time - id
        // 999 - 5
        // 998 - 3
        // 998 - 4
        // 997 - 2
        // 996 - 1

        int itemsCount = 5;
        List<BasketReferenceItem> savedItems = IntStream.range(0, itemsCount)
            .mapToObj(idx -> basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, PRODUCT, MODEL_ID+idx)))
            .collect(Collectors.toList());

        // set same time for all records
        pgaasJdbcTemplate.update("update basket_items set added_at = now() where rgb = 0");
        shiftDateSec(savedItems.get(1).getId(), 1);
        shiftDateSec(savedItems.get(2).getId(), 2);
        shiftDateSec(savedItems.get(3).getId(), 2);
        shiftDateSec(savedItems.get(4).getId(), 3);

        int pageSize = 2;
        BasketCrTimeToken token = null;

        // test token and page contents
        var result = basketItemsMvc.getItemsFull(OWNER_UID, token, pageSize);
        assertEquals(2, result.getItems().size());
        assertTrue(result.hasMore());
        assertNotNull(result.getToken());
        assertEquals(savedItems.get(2).getId(), result.getToken().getLastId());
        assertNotNull(result.getToken().getLastCrTime());
        assertTrue(ListUtils.toSet(List.of(savedItems.get(4), savedItems.get(2)), BasketReferenceItem::getId).containsAll(
            ListUtils.toSet(result.getItems(), BasketReferenceItem::getId)
        ));

        // second page
        result = basketItemsMvc.getItemsFull(OWNER_UID, result.getToken(), pageSize);
        assertEquals(2, result.getItems().size());
        assertTrue(result.hasMore());
        assertNotNull(result.getToken());
        assertNull(result.getToken().getLastId());
        assertNotNull(result.getToken().getLastCrTime());
        assertTrue(ListUtils.toSet(List.of(savedItems.get(3), savedItems.get(1)), BasketReferenceItem::getId).containsAll(
            ListUtils.toSet(result.getItems(), BasketReferenceItem::getId)
        ));

        result = basketItemsMvc.getItemsFull(OWNER_UID, result.getToken(), pageSize);
        assertEquals(1, result.getItems().size());
        assertFalse(result.hasMore());
        assertNotNull(result.getToken());
        assertNull(result.getToken().getLastId());
        assertNotNull(result.getToken().getLastCrTime());
        assertTrue(ListUtils.toSet(savedItems.subList(0, 1), BasketReferenceItem::getId).containsAll(
            ListUtils.toSet(result.getItems(), BasketReferenceItem::getId)
        ));

        // run once more to check token not changes then none received
        BasketCrTimeToken lastToken = result.getToken();
        result = basketItemsMvc.getItemsFull(OWNER_UID, result.getToken(), pageSize);
        assertEquals(0, result.getItems().size());
        assertFalse(result.hasMore());
        assertNotNull(result.getToken());
        assertEquals(lastToken.getLastId(), result.getToken().getLastId());
        assertEquals(lastToken.getLastCrTime(), result.getToken().getLastCrTime());
    }

    @Test
    public void testCountAndPagingCache() {
        int itemsCount = 3;
        List<BasketReferenceItem> savedItems = IntStream.range(0, itemsCount)
            .mapToObj(idx -> basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, PRODUCT, MODEL_ID + idx)))
            .collect(Collectors.toList());

        // set same time for all records
        pgaasJdbcTemplate.update("update basket_items set added_at = now() where rgb = 0");

        int pageSize = 2;

        // test count
        assertEquals(3, basketItemsMvc.getItemsCount(OWNER_UID));

        // test token and page contents
        var result = basketItemsMvc.getItemsFull(OWNER_UID, null, pageSize);
        assertEquals(2, result.getItems().size());
        assertTrue(result.hasMore());
        assertNotNull(result.getToken());

        // second page
        BasketCrTimeToken nextToken = result.getToken();
        result = basketItemsMvc.getItemsFull(OWNER_UID, nextToken, pageSize);
        assertEquals(1, result.getItems().size());
        assertFalse(result.hasMore());

        // delete items internally
        basketService.deleteAllForOwner(basketService.getOwnerId(OWNER_UID));

        // check cache is old
        assertEquals(3, basketItemsMvc.getItemsCount(OWNER_UID));

        result = basketItemsMvc.getItemsFull(OWNER_UID, null, pageSize);
        assertEquals(2, result.getItems().size());
        assertTrue(result.hasMore());

        // but 2nd page is not cached -> no data
        nextToken = result.getToken();
        result = basketItemsMvc.getItemsFull(OWNER_UID, nextToken, pageSize);
        assertEquals(0, result.getItems().size());
        assertFalse(result.hasMore());

        // reset cache, check results are empty now
        resetCache();

        assertEquals(0, basketItemsMvc.getItemsCount(OWNER_UID));

        result = basketItemsMvc.getItemsFull(OWNER_UID, null, pageSize);
        assertEquals(0, result.getItems().size());
        assertFalse(result.hasMore());

    }

    @Test
    public void testReflist() {
        List<BasketReferenceItem> savedItems = List.of(
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, PRODUCT, MODEL_ID)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, PRODUCT, MODEL_ID + 1)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, OFFER, OFFER_ID)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, OFFER, OFFER_ID + 1)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, OFFER, OFFER_ID + 2)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(BLUE, SKU, SKU_ID)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, SKU, SKU_ID + 1))
        );

        List<ItemReference> foundRefs = basketItemsMvc.getItemsRefs(OWNER_UID);
        assertContainsAllRef(foundRefs, savedItems);

        foundRefs.forEach(ref -> assertNotNull(ref.getId()));
        ListUtils.toSet(foundRefs, ItemReference::getId).containsAll(
            ListUtils.toSet(savedItems, BasketReferenceItem::getId)
        );
    }

    @Test
    public void testExisting() {
        List<ItemReference> itemsToFind = List.of(
            new ItemReference(PRODUCT, MODEL_ID + 1),
            new ItemReference(OFFER, OFFER_ID + 2),
            new ItemReference(SKU, SKU_ID)
        );

        assertEquals(0, basketItemsMvc.getExistingItems(OWNER_UID, WHITE, itemsToFind).size());
        assertEquals(0, basketItemsMvc.getExistingItemsDto(OWNER_UID, WHITE, itemsToFind).getTotal().intValue());

        List<BasketReferenceItem> savedItems = List.of(
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, PRODUCT, MODEL_ID)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, PRODUCT, MODEL_ID + 1)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, OFFER, OFFER_ID)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, OFFER, OFFER_ID + 1)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, OFFER, OFFER_ID + 2)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(BLUE, SKU, SKU_ID)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, SKU, SKU_ID + 1))
        );

        List<BasketReferenceItem> existingItems = basketItemsMvc.getExistingItems(OWNER_UID, WHITE, itemsToFind);

        // check all expected items are found
        assertContainsAll(existingItems, List.of(savedItems.get(1), savedItems.get(4)));
        assertEquals(5, basketItemsMvc.getExistingItemsDto(OWNER_UID, WHITE, itemsToFind).getTotal().intValue());

        // check without color
        existingItems = basketItemsMvc.getExistingItems(OWNER_UID, null, itemsToFind);
        assertContainsAll(existingItems, List.of(savedItems.get(1), savedItems.get(4), savedItems.get(5)));
        assertEquals(7, basketItemsMvc.getExistingItemsDto(OWNER_UID, null, itemsToFind).getTotal().intValue());

        //check cache is used. Delete all and check that items are still found
        basketService.deleteAllForOwner(basketService.getOwnerId(OWNER_UID));
        assertEquals(2, basketItemsMvc.getExistingItems(OWNER_UID, WHITE, itemsToFind).size());

        resetCache();

        assertEquals(0, basketItemsMvc.getExistingItems(OWNER_UID, WHITE, itemsToFind).size());
    }

    @Test
    public void testExistingNoCache() {
        List<ItemReference> itemsToFind = List.of(
            new ItemReference(PRODUCT, MODEL_ID + 1),
            new ItemReference(OFFER, OFFER_ID + 2),
            new ItemReference(SKU, SKU_ID)
        );

        assertEquals(0, basketItemsMvc.getExistingItemsNoCache(OWNER_UID, WHITE, itemsToFind).size());

        List<BasketReferenceItem> savedItems = List.of(
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, PRODUCT, MODEL_ID)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, PRODUCT, MODEL_ID + 1)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, OFFER, OFFER_ID)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, OFFER, OFFER_ID + 1)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, OFFER, OFFER_ID + 2)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(BLUE, SKU, SKU_ID))
        );

        List<BasketReferenceItem> existingItems = basketItemsMvc.getExistingItemsNoCache(OWNER_UID, WHITE, itemsToFind);

        // check all expected items are found
        assertContainsAll(existingItems, List.of(savedItems.get(1), savedItems.get(4)));

        //check cache is not used. Delete all and check that items are not found
        basketService.deleteAllForOwner(basketService.getOwnerId(OWNER_UID));
        assertEquals(0, basketItemsMvc.getExistingItemsNoCache(OWNER_UID, WHITE, itemsToFind).size());

        resetCache();

        assertEquals(0, basketItemsMvc.getExistingItemsNoCache(OWNER_UID, WHITE, itemsToFind).size());
    }

    @Test
    public void testAddSimple() {
        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        itemToSave.setTitle("test title");

        BasketReferenceItem savedItem = basketItemsMvc.addItem(OWNER_UID, itemToSave);

        List<BasketReferenceItem> items = basketItemsMvc.getItems(OWNER_UID, DEF_PAGE_SIZE);
        assertEquals(1, items.size());
        // check item saved
        assertItem(savedItem, PRODUCT, MODEL_ID);
        assertItem(items.get(0), PRODUCT, MODEL_ID);
        assertEquals("test title", items.get(0).getTitle());
        // check item id/owner returns after save
        assertNotNull(savedItem.getId());
        assertEquals(savedItem.getId(), items.get(0).getId());
        assertNotNull(savedItem.getOwnerId());
        assertEquals(savedItem.getOwnerId(), items.get(0).getOwnerId());
        // no region passed
        assertNull(savedItem.getRegionId());
    }

    @Test
    public void testAddWithRegion() {
        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        int regionId = 20;

        BasketReferenceItem savedItem = basketItemsMvc.addItem(OWNER_UID, itemToSave, regionId);

        List<BasketReferenceItem> items = basketItemsMvc.getItems(OWNER_UID, DEF_PAGE_SIZE);
        assertEquals(1, items.size());
        // check region
        assertNotNull(savedItem.getRegionId());
        assertEquals(regionId, savedItem.getRegionId().intValue());
        assertNotNull(items.get(0).getRegionId());
        assertEquals(regionId, items.get(0).getRegionId().intValue());
    }

    @Test
    public void testAddWithoutSource() {
        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        int regionId = 20;

        BasketReferenceItem savedItem = basketItemsMvc.addItem(OWNER_UID, itemToSave, regionId);
        List<BasketReferenceItem> items = basketItemsMvc.getItems(OWNER_UID, DEF_PAGE_SIZE);
        assertEquals(1, items.size());
        assertNull(savedItem.getSource());
        assertNull(items.get(0).getSource());
    }

    @Test
    public void testAddWithSource() {
        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        String source = "test source";
        int regionId = 20;

        BasketReferenceItem savedItem = basketItemsMvc.addItem(OWNER_UID, itemToSave, regionId, source);
        List<BasketReferenceItem> items = basketItemsMvc.getItems(OWNER_UID, DEF_PAGE_SIZE);
        assertEquals(1, items.size());
        assertNotNull(savedItem.getSource());
        assertEquals(source, savedItem.getSource());
        assertNotNull(items.get(0).getSource());
        assertEquals(source, items.get(0).getSource());
    }

    @Test
    public void testAddSame() {
        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        BasketReferenceItem itemToSave2 = generateItem(WHITE, PRODUCT, MODEL_ID);

        BasketReferenceItem savedItem = basketItemsMvc.addItem(OWNER_UID, itemToSave);
        BasketReferenceItem savedItem2 = basketItemsMvc.addItem(OWNER_UID, itemToSave2);

        List<BasketReferenceItem> items = basketItemsMvc.getItems(OWNER_UID, DEF_PAGE_SIZE);
        assertEquals(1, items.size());

        // check item id/owner returns after save
        assertEquals(savedItem.getId(), items.get(0).getId());
        assertEquals(savedItem2.getId(), items.get(0).getId());
    }

    @Test
    public void testAddBad() {
        basketItemsMvc.addItemsMvc(OWNER_UID, generateItem(WHITE, PRODUCT, "test"), null, ERR_4XX);
        basketItemsMvc.addItemsMvc(OWNER_UID, generateItem(RED, PRODUCT, MODEL_ID), null, ERR_4XX);
        basketItemsMvc.addItemsMvc(OWNER_UID, generateItem(BLUE, PRODUCT, MODEL_ID), null, ERR_4XX);
        basketItemsMvc.addItemsMvc(OWNER_UID, generateItem(BLUE, OFFER, OFFER_ID), null, ERR_4XX);

        // check invalid hid
        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        BasketApiUtils.updateData(itemToSave, HID, "invalid");
        basketItemsMvc.addItemsMvc(OWNER_UID, itemToSave, null, ERR_4XX);

        // check invalid model_id
        itemToSave = generateItem(WHITE, OFFER, OFFER_ID);
        BasketApiUtils.updateData(itemToSave, SecondaryReferenceType.MODEL_ID, "invalid");
        basketItemsMvc.addItemsMvc(OWNER_UID, itemToSave, null, ERR_4XX);

        // check missing mandatory secondary ref (no more mandatory references)
        itemToSave = generateItem(WHITE, OFFER, OFFER_ID);
        itemToSave.setData(Map.of());
        basketItemsMvc.addItemsMvc(OWNER_UID, itemToSave, null, ok);

        // check ok with long modelId
        itemToSave = generateItem(WHITE, PRODUCT, "123456789123456789");
        basketItemsMvc.addItemsMvc(OWNER_UID, itemToSave, null, ok);

        // check ok with long modelId
        itemToSave = generateItem(WHITE, OFFER, OFFER_ID);
        BasketApiUtils.updateData(itemToSave, SecondaryReferenceType.MODEL_ID, "123456789123456789");
        basketItemsMvc.addItemsMvc(OWNER_UID, itemToSave, null, ok);
    }

    @Test
    public void testAddDelete() {
        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        BasketReferenceItem itemToSave2 = generateItem(WHITE, PRODUCT, MODEL_ID + 1);

        BasketReferenceItem savedItem = basketItemsMvc.addItem(OWNER_UID, itemToSave);
        BasketReferenceItem savedItem2 = basketItemsMvc.addItem(OWNER_UID, itemToSave2);

        List<BasketReferenceItem> items = basketItemsMvc.getItems(OWNER_UID, DEF_PAGE_SIZE);
        assertEquals(2, items.size());
        assertTrue(ListUtils.toSet(items, BasketReferenceItem::getId)
            .containsAll(List.of(savedItem.getId(), savedItem2.getId())));

        basketItemsMvc.deleteItemsMvc(OWNER_UID, savedItem.getId());

        items = basketItemsMvc.getItems(OWNER_UID, DEF_PAGE_SIZE);
        assertEquals(1, items.size());
        assertEquals(savedItem2.getId(), items.get(0).getId());

        List<BasketArchiveItemTestDto> archiveList = pgaasJdbcTemplate.query(
            "SELECT * FROM basket_items_archive",
            BasketArchiveItemTestDto::valueOf
        );

        assertEquals(1, archiveList.size());
        assertEquals(savedItem.getId(), archiveList.get(0).getId());
        assertEquals(savedItem.getOwnerId(), archiveList.get(0).getOwnerId());
        assertEquals(savedItem.getColor(), archiveList.get(0).getData().getColor());
        assertEquals(FormatUtils.toJson(savedItem), FormatUtils.toJson(archiveList.get(0).getData()));
    }

    @Test
    public void testDoubleDelete() {
        BasketReferenceItem itemToSave1 = generateItem(WHITE, PRODUCT, MODEL_ID);
        itemToSave1.setTitle("test1");
        BasketReferenceItem itemToSave2 = generateItem(WHITE, PRODUCT, MODEL_ID);
        itemToSave2.setTitle("test2");

        BasketReferenceItem savedItem1 = basketItemsMvc.addItem(OWNER_UID, itemToSave1);
        basketItemsMvc.deleteItemsMvc(OWNER_UID, savedItem1.getId());
        BasketReferenceItem savedItem2 = basketItemsMvc.addItem(OWNER_UID, itemToSave2);
        basketItemsMvc.deleteItemsMvc(OWNER_UID, savedItem2.getId());

        List<BasketArchiveItemTestDto> archiveList = pgaasJdbcTemplate.query(
                "SELECT * FROM basket_items_archive",
                BasketArchiveItemTestDto::valueOf
        );
        assertEquals(1, archiveList.size());
        assertEquals(savedItem2.getTitle(), archiveList.get(0).getData().getTitle());
        assertEquals(savedItem2.getId(), archiveList.get(0).getData().getId());
        assertEquals(FormatUtils.toJson(savedItem2), FormatUtils.toJson(archiveList.get(0).getData()));
    }

    @Test
    public void testAddDeleteDifferentColors() {
        assertEquals(0, basketItemsMvc.getItemsCount(OWNER_UID));
        assertEquals(0, basketItemsMvc.getItems(OWNER_UID, DEF_PAGE_SIZE).size());

        List<BasketReferenceItem> savedItems = List.of(
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, PRODUCT, MODEL_ID)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, OFFER, OFFER_ID)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(BLUE, SKU, SKU_ID)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(RED, FEED_GROUP_ID_HASH, RED_ID))
        );

        List<BasketReferenceItem> items = basketItemsMvc.getItems(OWNER_UID, DEF_PAGE_SIZE);
        assertEquals(3, items.size());
        assertEquals(3, basketItemsMvc.getItemsCount(OWNER_UID));
        assertEquals(4, basketService.getItemsCount(BasketItemFilter.forOwner(OWNER_UID)));

        // check all expected items are returned (all except red)
        assertContainsAll(items, List.of(savedItems.get(0), savedItems.get(1), savedItems.get(2)));

        // try to delete all (even unavailable)
        savedItems.forEach(item -> {
            basketItemsMvc.deleteItemsMvc(OWNER_UID, item.getId());
        });

        assertEquals(0, basketItemsMvc.getItemsCount(OWNER_UID));
        assertEquals(0, basketService.getItemsCount(BasketItemFilter.forOwner(OWNER_UID)));
    }

    @Test
    public void testAddDeleteWrongOwner() {
        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        BasketReferenceItem itemToSave2 = generateItem(WHITE, PRODUCT, MODEL_ID + 1);

        BasketReferenceItem savedItem = basketItemsMvc.addItem(OWNER_UID, itemToSave);
        BasketReferenceItem savedItem2 = basketItemsMvc.addItem(OWNER_UID, itemToSave2);

        basketItemsMvc.deleteItemsMvc(OWNER_UID_2, savedItem.getId());

        List<BasketReferenceItem> items = basketItemsMvc.getItems(OWNER_UID, DEF_PAGE_SIZE);
        assertEquals(2, items.size());
        assertTrue(ListUtils.toSet(items, BasketReferenceItem::getId)
            .containsAll(List.of(savedItem.getId(), savedItem2.getId())));
    }

    @Test
    public void testMergeBad() {
        basketItemsMvc.mergeItemsMvc(OWNER_UID, OWNER_YUID, null, status().is4xxClientError());
        basketItemsMvc.mergeItemsMvc(OWNER_YUID, OWNER_UUID, null, status().is4xxClientError());
    }

    @Test
    public void testMerge() {
        List<BasketReferenceItem> yuidItems = List.of(
            basketItemsMvc.addItem(OWNER_YUID, generateItem(WHITE, PRODUCT, MODEL_ID)),
            basketItemsMvc.addItem(OWNER_YUID, generateItem(WHITE, PRODUCT, MODEL_ID + 1))
        );
        List<BasketReferenceItem> uidItems = List.of(
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, PRODUCT, MODEL_ID + 1)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, PRODUCT, MODEL_ID + 2))
        );

        assertEquals(2, basketItemsMvc.getItems(OWNER_YUID, DEF_PAGE_SIZE).size());
        assertEquals(2, basketItemsMvc.getItems(OWNER_UID, DEF_PAGE_SIZE).size());

        assertEquals(2, basketItemsMvc.getItemsCount(OWNER_YUID));
        assertEquals(2, basketItemsMvc.getItemsCount(OWNER_UID));

        basketItemsMvc.mergeItems(OWNER_YUID, OWNER_UID);

        List<BasketReferenceItem> items = basketItemsMvc.getItems(OWNER_UID, DEF_PAGE_SIZE);
        assertEquals(3, items.size());

        // id not changed for existing
        assertTrue(ListUtils.toSet(items, BasketReferenceItem::getId)
            .containsAll(ListUtils.toList(uidItems, BasketReferenceItem::getId)));

        // contains all references
        assertTrue(ListUtils.toSet(items, BasketReferenceItem::getReferenceId)
            .containsAll(ListUtils.toList(uidItems, BasketReferenceItem::getReferenceId)));
        assertTrue(ListUtils.toSet(items, BasketReferenceItem::getReferenceId)
            .contains(yuidItems.get(0).getReferenceId()));

        assertEquals(0, basketItemsMvc.getItems(OWNER_YUID, DEF_PAGE_SIZE).size());
        assertEquals(0, basketItemsMvc.getItemsCount(OWNER_YUID));

        // check source user removed
        assertNull(basketService.getOwnerId(OWNER_UUID));
    }

    @Test
    public void testMergeWithRegion() {
        int region1 = 1;
        int region2 = 2;
        int region3 = 3;

        List<BasketReferenceItem> uuidItems = List.of(
            basketItemsMvc.addItem(OWNER_UUID, generateItem(WHITE, PRODUCT, MODEL_ID), region1),
            basketItemsMvc.addItem(OWNER_UUID, generateItem(WHITE, PRODUCT, MODEL_ID + 1), region1),
            basketItemsMvc.addItem(OWNER_UUID, generateItem(WHITE, PRODUCT, MODEL_ID + 3))
        );
        List<BasketReferenceItem> uidItems = List.of(
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, PRODUCT, MODEL_ID + 1), region2),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, PRODUCT, MODEL_ID + 2), region2)
        );

        basketItemsMvc.mergeItems(OWNER_UUID, OWNER_UID,region3);

        List<BasketReferenceItem> items = basketItemsMvc.getItems(OWNER_UID, DEF_PAGE_SIZE);
        assertEquals(4, items.size());

        Map<String, BasketReferenceItem> mapByRef = ListUtils.toMap(items, BasketReferenceItem::getReferenceId);
        assertEquals(region1, mapByRef.get(MODEL_ID).getRegionId().intValue());
        assertEquals(region2, mapByRef.get(MODEL_ID + 1).getRegionId().intValue());
        assertEquals(region2, mapByRef.get(MODEL_ID + 2).getRegionId().intValue());
        assertEquals(region3, mapByRef.get(MODEL_ID + 3).getRegionId().intValue());

        assertEquals(0, basketItemsMvc.getItems(OWNER_UUID, DEF_PAGE_SIZE).size());
        assertEquals(0, basketItemsMvc.getItemsCount(OWNER_UUID));

        // check source user removed
        assertNull(basketService.getOwnerId(OWNER_UUID));
    }

    @Test
    public void checkFillModelFromReport() {
        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        itemToSave.setData(Map.of(MODEL_TYPE.getName(), "any value"));

        Model model = new Model();
        model.setName("Model name");
        model.setPictureUrl("picture_url");
        model.setCategory(new Category(123, "name"));
        model.setType(ProductType.BOOK);
        when(reportService.getModelsByIds(anyList())).thenReturn(Map.of(Long.parseLong(MODEL_ID), model));

        BasketReferenceItem savedItem = basketItemsMvc.addItem(OWNER_UUID, itemToSave);

        assertEquals("Model name", savedItem.getTitle());
        assertEquals("picture_url", savedItem.getImageBaseUrl());
        assertEquals("123", getSecondaryReferenceValue(savedItem, HID).orElse(null));
        assertEquals("MODEL", getSecondaryReferenceValue(savedItem, MODEL_TYPE).orElse(null));
        assertNull(getSecondaryReferenceValue(savedItem, FOUND_IN_REPORT).orElse(null));
    }

    @Test
    public void checkFillModelMinimalFromReport() {
        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        itemToSave.setData(Map.of(MODEL_TYPE.getName(), "any value"));

        Model model = new Model();
        model.setName("Model name");
        when(reportService.getModelsByIds(anyList())).thenReturn(Map.of(Long.parseLong(MODEL_ID), model));

        BasketReferenceItem savedItem = basketItemsMvc.addItem(OWNER_UUID, itemToSave);

        assertEquals("Model name", savedItem.getTitle());
        assertEquals(DEFAULT_IMAGE, savedItem.getImageBaseUrl());
        assertNull(getSecondaryReferenceValue(savedItem, HID).orElse(null));
        assertEquals("MODEL", getSecondaryReferenceValue(savedItem, MODEL_TYPE).orElse(null));
        assertNull(getSecondaryReferenceValue(savedItem, FOUND_IN_REPORT).orElse(null));
    }

    @Test
    public void checkFillModelNotInReport() {
        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        itemToSave.setData(Map.of());

        when(reportService.getModelsByIds(anyList())).thenReturn(Map.of());

        BasketReferenceItem savedItem = basketItemsMvc.addItem(OWNER_UUID, itemToSave);

        assertNotNull( savedItem.getTitle());
        assertEquals(DEFAULT_IMAGE, savedItem.getImageBaseUrl());
        assertNull(getSecondaryReferenceValue(savedItem, HID).orElse(null));
        assertNull(getSecondaryReferenceValue(savedItem, MODEL_TYPE).orElse(null));
        assertEquals("0", getSecondaryReferenceValue(savedItem, FOUND_IN_REPORT).orElse(null));
    }

    @Test
    public void checkFillOfferFromReport() {
        BasketReferenceItem itemToSave = generateItem(WHITE, OFFER, OFFER_ID);
        itemToSave.setData(Map.of(MODEL_TYPE.getName(), "MODEL"));

        Offer offer = new Offer();
        offer.setName("Model name (offer)");
        offer.setPictureUrl("picture_url");
        offer.setCategory(new Category(123, "name"));
        offer.setModelId(33313L);
        offer.setPrices(new OfferPrices(Currency.RUR, BigDecimal.valueOf(12), BigDecimal.valueOf(12)));
        when(reportService.getOffersByIds(anyList())).thenReturn(Optional.of(Map.of(OFFER_ID, offer)));

        BasketReferenceItem savedItem = basketItemsMvc.addItem(OWNER_UUID, itemToSave);

        assertEquals("Model name (offer)", savedItem.getTitle());
        assertEquals("picture_url", savedItem.getImageBaseUrl());
        assertEquals("123", getSecondaryReferenceValue(savedItem, HID).orElse(null));
        assertEquals("33313", getSecondaryReferenceValue(savedItem, SecondaryReferenceType.MODEL_ID)
            .orElse(null));
        assertEquals(Currency.RUR, savedItem.getPrice().getCurrency());
        assertEquals(BigDecimal.valueOf(12), savedItem.getPrice().getAmount());
        assertNull(getSecondaryReferenceValue(savedItem, FOUND_IN_REPORT).orElse(null));
    }

    @Test
    public void checkFillOfferMinInfoFromReport() {
        BasketReferenceItem itemToSave = generateItem(WHITE, OFFER, OFFER_ID);
        itemToSave.setData(Map.of(MODEL_TYPE.getName(), "MODEL"));

        Offer offer = new Offer();
        offer.setName("Model name (offer)");

        when(reportService.getOffersByIds(anyList())).thenReturn(Optional.of(Map.of(OFFER_ID, offer)));

        BasketReferenceItem savedItem = basketItemsMvc.addItem(OWNER_UUID, itemToSave);

        assertEquals("Model name (offer)", savedItem.getTitle());
        assertEquals(DEFAULT_IMAGE, savedItem.getImageBaseUrl());
        assertNull(getSecondaryReferenceValue(savedItem, HID).orElse(null));
        assertNull(getSecondaryReferenceValue(savedItem, SecondaryReferenceType.MODEL_ID).orElse(null));
        assertNull(savedItem.getPrice());
        assertNull(getSecondaryReferenceValue(savedItem, FOUND_IN_REPORT).orElse(null));
    }

    @Test
    public void checkFillOfferNotInReport() {
        BasketReferenceItem itemToSave = generateItem(WHITE, OFFER, OFFER_ID);
        itemToSave.setData(Map.of());

        when(reportService.getOffersByIds(anyList())).thenReturn(Optional.of(Map.of()));

        BasketReferenceItem savedItem = basketItemsMvc.addItem(OWNER_UUID, itemToSave);

        assertNotNull(savedItem.getTitle());
        assertEquals(DEFAULT_IMAGE, savedItem.getImageBaseUrl());
        assertNull(getSecondaryReferenceValue(savedItem, HID).orElse(null));
        assertNull(getSecondaryReferenceValue(savedItem, SecondaryReferenceType.MODEL_ID).orElse(null));
        assertNull(savedItem.getPrice());
        assertEquals("0", getSecondaryReferenceValue(savedItem, FOUND_IN_REPORT).orElse(null));
    }

    @Test
    public void checkReferenceTypeName() throws IOException {
        String json = "{\"added_at\": \"2007-12-03T10:15:30.00Z\", \"id\": 0, \"image_base_url\": \"https://avatars.mds.yandex.net/get-marketpic/901531/market_Aneq8fbtwGYb8Swr-l4z_A\", \"owner_id\": 0, \"price\": {\"amount\": 345.13, \"currency\": \"RUR\"}, \"reference_id\": \"123a\", \"reference_type\": \"SKU\", \"regionId\": 0, \"rgb\": \"WHITE\", \"secondary_references\": [{\"id\": \"8734hjhufe84ui45hjf\", \"type\": \"offerId\"}], \"source\": \"string\", \"title\": \"TITLE\", \"wishlist_ids\": [0]}";
        String item = basketItemsMvc.addItemsMvc(OWNER_UID, json, 1, "", ok);
        assertNotNull(item);
        ResultDto<BasketReferenceItem> items = FormatUtils.fromJson(item, new TypeReference<>() {});
        BasketReferenceItem item1 = items.getResult();
        assertEquals(SKU, item1.getReferenceType());
        assertEquals(WHITE, item1.getColor());
    }

    @Test
    public void correctAddItemToCollections() {
        final String title = "test title";

        collectionsService.clearQueue();

        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        itemToSave.setTitle(title);
        basketItemsMvc.addItem(OWNER_UID, itemToSave);

        assertNotNull(collectionsService.getItemFromQueue());
        assertEquals(itemToSave.getTitle(), collectionsService.getItemFromQueue().getData().getTitle());
        collectionsService.processQueue();
        assertNull(collectionsService.getItemFromQueue());

        ArgumentCaptor<BasketCollectionsRequestDto> requestCaptor = ArgumentCaptor
                .forClass(BasketCollectionsRequestDto.class);
        Mockito.verify(collectionsClient).createCollectionsCard(any(), requestCaptor.capture(), any());
        assertEquals(title, requestCaptor.getValue().getDescription());
        assertNotNull(requestCaptor.getValue().getMetaDto());
    }

    @Test
    public void addAndDeleteToCollectionsQueue() {
        collectionsService.clearQueue();

        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        itemToSave.setTitle("test title");
        itemToSave.setId((long) 555);

        BasketReferenceItem savedItem = basketItemsMvc.addItem(OWNER_UID, itemToSave);
        assertEquals(OperationType.ADD, collectionsService.getItemFromQueue().getOperationType());
        collectionsService.deleteItemFromQueue(collectionsService.getItemFromQueue().getId());

        basketItemsMvc.deleteItemsMvc(OWNER_UID, savedItem.getId());
        assertNotNull(collectionsService.getItemFromQueue());
        assertEquals(OperationType.DELETE, collectionsService.getItemFromQueue().getOperationType());
        collectionsService.deleteItemFromQueue(collectionsService.getItemFromQueue().getId());

        collectionsService.processQueue();
        assertNull(collectionsService.getItemFromQueue());
    }

    @Test
    public void addExistingInCollectionsItem() {
        collectionsService.clearQueue();

        when(collectionsClient.createCollectionsCard(any(), any(), any()))
                .thenThrow(new RestClientResponseException("This item was already added", 409, "error", null, null, null));

        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        itemToSave.setTitle("409");
        basketItemsMvc.addItem(OWNER_UID, itemToSave);
        assertNotNull(collectionsService.getItemFromQueue());
        assertEquals(itemToSave.getTitle(), collectionsService.getItemFromQueue().getData().getTitle());
        collectionsService.processQueue();
        assertNull(collectionsService.getItemFromQueue());
    }

    @Test
    public void deleteNotExistingItem() {
        collectionsService.clearQueue();

        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        itemToSave.setTitle("test title");
        itemToSave.setId((long) 555);
        basketItemsMvc.deleteItemsMvc(OWNER_UID, 1);
        assertNull(collectionsService.getItemFromQueue());
    }

    @Test
    public void addWithCollectionsError() {
        collectionsService.clearQueue();

        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        itemToSave.setTitle("500");

        when(collectionsClient.createCollectionsCard(any(), any(), any()))
                .thenThrow(new RestClientResponseException("Internal server error", 500, "error", null, null, null));

        basketItemsMvc.addItem(OWNER_UID, itemToSave);
        collectionsService.processQueue();
        assertNotNull(collectionsService.getItemFromQueue());
        assertEquals(itemToSave.getTitle(), collectionsService.getItemFromQueue().getData().getTitle());
    }

    @Test
    public void addInCollectionsByNotAuthorizedUser() {
        collectionsService.clearQueue();

        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        itemToSave.setTitle("test title");
        basketItemsMvc.addItem(OWNER_UUID, itemToSave);

        assertNull(collectionsService.getItemFromQueue());
    }

    private Optional<String> getSecondaryReferenceValue(BasketReferenceItem item, SecondaryReferenceType type) {
        return item.getSecondaryReferences().stream()
            .filter(x -> x.getType() != null && x.getId() != null)
            .filter(x -> x.getType().equals(type.getName()))
            .findFirst()
            .map(SecondaryReference::getId);
    }

}
