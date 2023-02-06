package ru.yandex.market.pers.basket.controller;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.basket.model.BasketReferenceItem;
import ru.yandex.market.pers.basket.model.ItemReference;
import ru.yandex.market.pers.list.mock.BasketSovetnikItemsMvcMocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor.BLUE;
import static ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor.WHITE;
import static ru.yandex.market.pers.list.model.v2.enums.ReferenceType.OFFER;
import static ru.yandex.market.pers.list.model.v2.enums.ReferenceType.PRODUCT;
import static ru.yandex.market.pers.list.model.v2.enums.ReferenceType.SKU;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 12.04.2021
 */
public class BasketSovetnikItemsControllerTest extends AbstractBasketControllerTest {
    @Autowired
    private BasketSovetnikItemsMvcMocks basketItemsMvc;

    @Test
    public void testSovetnikApiSimpleWork() {
        List<ItemReference> itemsToFind = List.of(
            new ItemReference(PRODUCT, MODEL_ID + 1),
            new ItemReference(OFFER, OFFER_ID + 2),
            new ItemReference(SKU, SKU_ID)
        );

        assertEquals(0, basketItemsMvc.getExistingItems(OWNER_UID, WHITE, itemsToFind).size());
        String src = "test";
        List<BasketReferenceItem> savedItems = List.of(
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, PRODUCT, MODEL_ID)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, PRODUCT, MODEL_ID + 1)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, OFFER, OFFER_ID)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, OFFER, OFFER_ID + 1)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(WHITE, OFFER, OFFER_ID + 2)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(BLUE, SKU, SKU_ID)),
            basketItemsMvc.addItem(OWNER_UID, generateItem(BLUE, SKU, SKU_ID), null, src)
        );

        List<BasketReferenceItem> existingItems = basketItemsMvc.getExistingItems(OWNER_UID, WHITE, itemsToFind);

        // check all expected items are found
        assertContainsAll(existingItems, List.of(savedItems.get(1), savedItems.get(4)));

        assertEquals(src, savedItems.get(6).getSource());
        assertNull(savedItems.get(5).getSource());

        List<BasketReferenceItem> pagedItems = basketItemsMvc.getItems(OWNER_UID, DEF_PAGE_SIZE);
        assertContainsAll(pagedItems, savedItems.subList(0, 6));

        // remove some items and check again
        basketItemsMvc.deleteItemsMvc(OWNER_UID, savedItems.get(4).getId());
        existingItems = basketItemsMvc.getExistingItems(OWNER_UID, WHITE, itemsToFind);
        assertContainsAll(existingItems, List.of(savedItems.get(1)));

        //check cache is used. Delete all and check that items are still found
        basketService.deleteAllForOwner(basketService.getOwnerId(OWNER_UID));
        assertEquals(1, basketItemsMvc.getExistingItems(OWNER_UID, WHITE, itemsToFind).size());

        resetCache();

        assertEquals(0, basketItemsMvc.getExistingItems(OWNER_UID, WHITE, itemsToFind).size());

    }
}
