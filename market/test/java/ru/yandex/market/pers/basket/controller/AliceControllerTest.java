package ru.yandex.market.pers.basket.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.basket.PersBasketTest;
import ru.yandex.market.pers.basket.model.AliceEntry;
import ru.yandex.market.pers.basket.model.AliceEntryResponse;
import ru.yandex.market.pers.basket.model.AliceEntrySaveRequest;
import ru.yandex.market.pers.basket.model.ResultLimit;
import ru.yandex.market.pers.list.mock.AliceMvcMocks;
import ru.yandex.market.pers.list.model.BasketOwner;
import ru.yandex.market.util.ListUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 22.08.2020
 */
public class AliceControllerTest extends PersBasketTest {
    @Autowired
    private AliceMvcMocks aliceMvc;

    @Test
    public void testAddDelete() {
        BasketOwner owner = BasketOwner.fromUid(123);

        List<AliceEntrySaveRequest> itemsToSave = List.of(
            new AliceEntrySaveRequest("Test item", List.of()),
            new AliceEntrySaveRequest("Test item 2", List.of()),
            new AliceEntrySaveRequest("Test item another more", List.of())
        );
        Set<String> textsToSave = ListUtils.toSet(itemsToSave, AliceEntrySaveRequest::getText);

        aliceMvc.addItems(owner, itemsToSave);

        assertEquals(3, aliceMvc.getItemsCount(owner));

        AliceEntryResponse allItems = aliceMvc.getItems(owner, null);
        assertEquals(3, allItems.getTotal());
        Map<String, Long> textToIdMap = ListUtils.toMap(allItems.getEntries(), AliceEntry::getText, AliceEntry::getId);

        // all texts are saved
        assertTrue(textsToSave.containsAll(textToIdMap.keySet()));

        // try to delete
        aliceMvc.deleteItem(owner, textToIdMap.get("Test item 2"));

        AliceEntryResponse allItemsAfterDelete = aliceMvc.getItems(owner, null);
        assertEquals(2, allItemsAfterDelete.getTotal());
        Map<String, Long> testToIdMapAfterDelete = ListUtils.toMap(allItemsAfterDelete.getEntries(),
            AliceEntry::getText, AliceEntry::getId);

        assertTrue(testToIdMapAfterDelete.keySet().containsAll(
            List.of(
                "Test item",
                "Test item another more"
            )
        ));
    }

    @Test
    public void testPaging() {
        BasketOwner owner = BasketOwner.fromUid(123);

        List<AliceEntrySaveRequest> itemsToSave = List.of(
            new AliceEntrySaveRequest("Test item", List.of()),
            new AliceEntrySaveRequest("Test item 2", List.of()),
            new AliceEntrySaveRequest("Test item another more", List.of())
        );

        aliceMvc.addItems(owner, itemsToSave);

        //stabilize sort
        pgaasJdbcTemplate.update("update alice_entry set added_at = now() - make_interval(hours := length(text))");

        int pageSize = 2;
        AliceEntryResponse page1 = aliceMvc.getItems(owner, new ResultLimit(0, pageSize));
        assertEquals(3, page1.getTotal());
        assertEquals(2, page1.getEntries().size());
        Set<String> foundItems = ListUtils.toSet(page1.getEntries(), AliceEntry::getText);
        Set<String> expectedItems = Set.of(
            "Test item",
            "Test item 2"
        );

        assertTrue(foundItems.containsAll(expectedItems));

        AliceEntryResponse page2 = aliceMvc.getItems(owner, new ResultLimit(2, pageSize));
        assertEquals(3, page2.getTotal());
        assertEquals(1, page2.getEntries().size());
        assertTrue(ListUtils.toSet(page2.getEntries(), AliceEntry::getText).contains("Test item another more"));
    }
}
