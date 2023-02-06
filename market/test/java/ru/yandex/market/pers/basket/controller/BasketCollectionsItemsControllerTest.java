package ru.yandex.market.pers.basket.controller;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.pers.basket.collections.CollectionsService;
import ru.yandex.market.pers.basket.model.BasketItemFilter;
import ru.yandex.market.pers.basket.model.BasketReferenceItem;
import ru.yandex.market.pers.basket.service.BasketService;
import ru.yandex.market.pers.list.mock.CollectionsItemsMvcMock;
import ru.yandex.market.pers.list.model.v2.enums.ReferenceType;
import ru.yandex.market.util.FormatUtils;
import ru.yandex.market.util.ListUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor.WHITE;
import static ru.yandex.market.pers.list.model.v2.enums.ReferenceType.PRODUCT;

public class BasketCollectionsItemsControllerTest extends AbstractBasketControllerTest {
    @Autowired
    private CollectionsItemsMvcMock collectionsItemsMvc;

    @Autowired
    protected JdbcTemplate pgaasJdbcTemplate;

    @Autowired
    private BasketService basketService;

    @Autowired
    private CollectionsService collectionsService;

    @Test
    public void testSimpleSave(){
        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        itemToSave.setTitle("test title");

        BasketReferenceItem savedItem = collectionsItemsMvc.addItem(OWNER_UID, itemToSave);

        List<BasketReferenceItem> items = basketService.getItems(BasketItemFilter.any());
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
    public void addDelete() {
        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        BasketReferenceItem itemToSave2 = generateItem(WHITE, PRODUCT, MODEL_ID + 1);

        BasketReferenceItem savedItem = collectionsItemsMvc.addItem(OWNER_UID, itemToSave);
        BasketReferenceItem savedItem2 = collectionsItemsMvc.addItem(OWNER_UID, itemToSave2);

        List<BasketReferenceItem> items = basketService.getItems(BasketItemFilter.any());
        assertEquals(2, items.size());
        assertTrue(ListUtils.toSet(items, BasketReferenceItem::getId)
                .containsAll(List.of(savedItem.getId(), savedItem2.getId())));

        collectionsItemsMvc.deleteItemsMvc(
            OWNER_UID, savedItem.getReferenceType(), savedItem.getReferenceId()
        );

        // Delete item must not be added into the queue
        assertNull(collectionsService.getItemFromQueue());

        items = basketService.getItems(BasketItemFilter.any());
        assertEquals(1, items.size());
        assertEquals(savedItem2.getId(), items.get(0).getId());

        List<BasketArchiveItemTestDto> archiveList = pgaasJdbcTemplate.query(
                "SELECT * FROM basket_items_archive",
                BasketArchiveItemTestDto::valueOf
        );
        assertEquals(1, archiveList.size());
        assertEquals(savedItem.getTitle(), archiveList.get(0).getData().getTitle());
        assertEquals(savedItem.getId(), archiveList.get(0).getData().getId());
        assertEquals(FormatUtils.toJson(savedItem), FormatUtils.toJson(archiveList.get(0).getData()));
    }

    @Test
    public void testWrongOwner() {
        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        BasketReferenceItem savedItem = collectionsItemsMvc.addItem(OWNER_UID, itemToSave);
        collectionsItemsMvc.deleteItemsMvc(
                OWNER_UID_2, savedItem.getReferenceType(), savedItem.getReferenceId()
        );

        List<BasketReferenceItem> items = basketService.getItems(BasketItemFilter.any());

        assertEquals(1, items.size());
        assertEquals(savedItem.getId(),items.get(0).getId());
    }

    @Test
    public void testWrongRefType() {
        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        BasketReferenceItem savedItem = collectionsItemsMvc.addItem(OWNER_UID, itemToSave);
        collectionsItemsMvc.deleteItemsMvc(
                OWNER_UID_2, ReferenceType.OFFER, savedItem.getReferenceId()
        );

        List<BasketReferenceItem> items = basketService.getItems(BasketItemFilter.any());

        assertEquals(1, items.size());
        assertEquals(savedItem.getId(),items.get(0).getId());
    }

    @Test
    public void testWrongRefId() {
        BasketReferenceItem itemToSave = generateItem(WHITE, PRODUCT, MODEL_ID);
        BasketReferenceItem savedItem = collectionsItemsMvc.addItem(OWNER_UID, itemToSave);
        collectionsItemsMvc.deleteItemsMvc(
                OWNER_UID_2, savedItem.getReferenceType(), "test"
        );

        List<BasketReferenceItem> items = basketService.getItems(BasketItemFilter.any());

        assertEquals(1, items.size());
        assertEquals(savedItem.getId(),items.get(0).getId());
    }
}
