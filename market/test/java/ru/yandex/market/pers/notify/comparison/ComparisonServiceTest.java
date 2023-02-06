package ru.yandex.market.pers.notify.comparison;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import ru.yandex.market.pers.notify.comparison.model.ComparisonItem;
import ru.yandex.market.pers.notify.comparison.model.SaveComparisonItemException;
import ru.yandex.market.pers.notify.model.Identity;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.Uuid;
import ru.yandex.market.pers.notify.model.YandexUid;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityDAO;
import ru.yandex.market.pers.notify.test.MarketUtilsMockedDbTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 22.11.16
 */
public abstract class ComparisonServiceTest extends MarketUtilsMockedDbTest {
    protected ComparisonService comparisonService;
    @Autowired
    protected SubscriptionAndIdentityDAO subscriptionAndIdentityDAO;

    @Test
    public void getItems() throws Exception {
        Identity identity = new Uuid("FFFFUUUU");
        long id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity);
        int categoryId = 324;
        String productId = "lkjlkj344b";
        Long regionId = 54L;
        Long sku = 12345L;
        assertTrue(comparisonService.saveItem(identity, new ComparisonItem(id, categoryId, productId, sku, regionId)));
        long itemId = comparisonService.getItems(id, productId).get(0).getId();
        List<ComparisonItem> items = comparisonService.getItems(id);
        assertEquals(1, items.size());
        assertEquals(categoryId, items.get(0).getCategoryId());
        assertEquals(productId, items.get(0).getProductId());
        assertEquals(itemId, (long) items.get(0).getId());
        assertEquals(id, items.get(0).getIdentityId());
        assertEquals(regionId, items.get(0).getRegionId());
        assertEquals(sku, items.get(0).getSku());
    }

    @Test
    public void getItemsCnt() throws Exception {
        Identity identity = new Uuid("383");
        long id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity);
        int categoryId1 = 983248;
        String productId1 = "asdflkdfa";
        assertTrue(comparisonService.saveItem(identity, new ComparisonItem(id, categoryId1, productId1)));
        long itemId1 = comparisonService.getItems(id, productId1).get(0).getId();
        int categoryId2 = 321345;
        String productId2 = "asdlkjas82";
        assertTrue(comparisonService.saveItem(identity, new ComparisonItem(id, categoryId2, productId2)));
        long itemId2 = comparisonService.getItems(id, productId2).get(0).getId();
        assertNotEquals(itemId1, itemId2);
        assertEquals(1, comparisonService.getItemsCnt(id, categoryId1));
        assertEquals(1, comparisonService.getItemsCnt(id, categoryId2));
        assertEquals(0, comparisonService.getItemsCnt(id, 1230));
    }

    @Test
    public void getComparisonsCnt() throws Exception {
        Identity identity = new YandexUid("320482304");
        long id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity);
        int categoryId1 = 324324;
        String productId1 = "2309842302";
        assertTrue(comparisonService.saveItem(identity, new ComparisonItem(id, categoryId1, productId1)));
        long itemId1 = comparisonService.getItems(id, productId1).get(0).getId();
        int categoryId2 = 98732;
        String productId2 = "dslfwer3232";
        assertTrue(comparisonService.saveItem(identity, new ComparisonItem(id, categoryId2, productId2)));
        long itemId2 = comparisonService.getItems(id, productId2).get(0).getId();
        assertNotEquals(itemId1, itemId2);
        assertEquals(2, comparisonService.getComparisonsCnt(id));

        id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(new YandexUid("8kjnk"));
        int categoryId = 111;
        productId1 = "76guy7t6";
        assertTrue(comparisonService.saveItem(identity, new ComparisonItem(id, categoryId, productId1)));
        productId2 = "8joou";
        assertTrue(comparisonService.saveItem(identity, new ComparisonItem(id, categoryId, productId2)));
        assertEquals(1, comparisonService.getComparisonsCnt(id));
    }

    @Test
    public void saveSameItem() throws Exception {
        Identity identity = new Uid(3242L);
        long id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity);
        long categoryId = 75231;
        String productId = "klj23h4ljqh312";
        assertTrue(comparisonService.saveItem(identity, new ComparisonItem(id, categoryId, productId, 1L, 213L)));
        long itemId1 = comparisonService.getItem(id, productId, 1L).getId();
        Thread.sleep(2000);
        ComparisonItem item1 = comparisonService.getItems(id).get(0);
        assertTrue(comparisonService.saveItem(identity, new ComparisonItem(id, categoryId, productId, 1L, 54L)));
        long itemId2 = comparisonService.getItem(id, productId, 1L).getId();
        assertEquals(itemId1, itemId2);
        ComparisonItem item2 = comparisonService.getItems(id).get(0);
        assertTrue(item1.getCreationTime().getTime() < item2.getCreationTime().getTime());
        assertEquals(Long.valueOf(54L), item2.getRegionId());
    }

    @Test
    public void saveItem() throws Exception {
        Identity identity = new Uid(3242L);
        long id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity);
        long categoryId = 75231;
        String productId = "klj23h4ljqh312";
        Long regionId = 65654L;
        Long sku = 123L;
        assertTrue(comparisonService.saveItem(identity, new ComparisonItem(id, categoryId, productId, sku, regionId)));
        List<ComparisonItem> items = comparisonService.getItems(id);
        assertEquals(1, items.size());
        assertEquals(categoryId, items.get(0).getCategoryId());
        assertEquals(productId, items.get(0).getProductId());
        assertEquals(id, items.get(0).getIdentityId());
        assertEquals(regionId, items.get(0).getRegionId());
        assertEquals(sku, items.get(0).getSku());
    }

    @Test
    public void saveItemSafe() throws Exception {
        Identity identity = new Uid(9872348L);
        long id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity);
        long categoryId = 123324;
        String productId = "kjsdfu98cv";
        Long regionId = 324124L;
        Long sku = 123L;
        assertTrue(comparisonService.saveItemSafe(identity, new ComparisonItem(id, categoryId, productId, sku, regionId)));
        List<ComparisonItem> items = comparisonService.getItems(id);
        assertEquals(1, items.size());
        assertEquals(categoryId, items.get(0).getCategoryId());
        assertEquals(productId, items.get(0).getProductId());
        assertEquals(id, items.get(0).getIdentityId());
        assertEquals(regionId, items.get(0).getRegionId());
        assertEquals(sku, items.get(0).getSku());
    }

    @Test
    public void saveItemSafeMoreThanLimitInDifferentCategories() throws Exception {
        int limit = ComparisonService.MAX_ITEMS_IN_CATEGORY_CNT;
        Identity identity = new Uid(9872348L);
        long id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity);
        for (int i = 0; i < limit + 1; i++) {
            assertTrue(comparisonService.saveItemSafe(identity, new ComparisonItem(id, 4332 + i, "dsfkj832" + i)));
        }
    }

    @Test
    public void saveItemSafeCanSaveLimitInCategory() throws Exception {
        int limit = ComparisonService.MAX_ITEMS_IN_CATEGORY_CNT;
        Identity identity = new Uid(32490812L);
        long id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity);
        long categoryId = 98887L;
        for (int i = 0; i < limit; i++) {
            assertTrue(comparisonService.saveItemSafe(identity, new ComparisonItem(id, categoryId, ".,m,9" + i)));
        }
    }

    @Test
    public void saveItemsSafeCanSaveLimitInCategory() throws Exception {
        int limit = ComparisonService.MAX_ITEMS_IN_CATEGORY_CNT;
        Identity identity = new Uid(32490812L);
        long id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity);
        long categoryId = 98887L;
        for (int i = 0; i < limit; i++) {
            assertTrue(comparisonService.saveItemsSafe(identity, Collections.singletonList(new ComparisonItem(id, categoryId, ".,m,9" + i))));
        }
    }

    @Test
    public void saveItemSafeNoMoreThanLimitInCategory() throws Exception {
        assertThrows(SaveComparisonItemException.class, () -> {
            int limit = ComparisonService.MAX_ITEMS_IN_CATEGORY_CNT;
            Identity identity = new Uid(987732L);
            long id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity);
            long categoryId = 34523L;
            for (int i = 0; i < limit; i++) {
                assertTrue(comparisonService.saveItemSafe(identity, new ComparisonItem(id, categoryId, ".,m,9" + i)));
            }
            comparisonService.saveItemSafe(identity, new ComparisonItem(id, categoryId, ".,m,9" + (limit + 1)));
        });
    }

    @Test
    public void saveItemsSafeNoMoreThanLimitInCategory() throws Exception {
        assertThrows(SaveComparisonItemException.class, () -> {
            int limit = ComparisonService.MAX_ITEMS_IN_CATEGORY_CNT;
            Identity identity = new Uid(987732L);
            long id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity);
            long categoryId = 34523L;
            for (int i = 0; i < limit; i++) {
                assertTrue(comparisonService.saveItemsSafe(identity, Collections.singletonList(new ComparisonItem(id, categoryId, ".,m,9" + i))));
            }
            comparisonService.saveItemsSafe(identity, Collections.singletonList(new ComparisonItem(id, categoryId, ".,m,9" + (limit + 1))));
        });
    }

    @Test
    public void saveItemsSafeCanSaveLimitInDifferentCategories() throws Exception {
        int limit = ComparisonService.MAX_ITEMS_IN_CATEGORY_CNT;
        Identity identity = new Uid(328476L);
        long id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity);
        List<ComparisonItem> items = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < limit; j++) {
                items.add(new ComparisonItem(id, j, ".,sdafslda" + i));
            }
        }
        assertTrue(comparisonService.saveItemsSafe(identity, items));
    }

    @Test
    public void saveItemsSafeCantSaveMoreThanLimitInDifferentCategories() throws Exception {
        assertThrows(SaveComparisonItemException.class, () -> {
            int limit = ComparisonService.MAX_ITEMS_IN_CATEGORY_CNT;
            Identity identity = new Uid(4837632L);
            long id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity);
            List<ComparisonItem> items = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < limit; j++) {
                    items.add(new ComparisonItem(id, i, ".,sdafslda" + j));
                }
            }
            items.add(new ComparisonItem(id, 0, ".,sdafslda" + (limit + 1)));
            comparisonService.saveItemsSafe(identity, items);
        });
    }

    @Test
    public void saveItemWithoutIdentity() throws Exception {
        assertThrows(DataIntegrityViolationException.class, () -> {
            comparisonService.saveItem(null, new ComparisonItem(123, 75231, "klj23h4ljqh312"));
        });
    }

    @Test
    public void saveItemWithoutProductId() throws Exception {
        assertThrows(DataIntegrityViolationException.class, () -> {
            comparisonService.saveItem(null, new ComparisonItem(1, 1, null));
        });
    }

    @Test
    public void saveItems() throws Exception {
        Identity identity = new YandexUid("oweirutw");
        long id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity);
        assertTrue(comparisonService.saveItems(identity, Arrays.asList(
            new ComparisonItem(id, 389045, "34ru58gj", null, 34L),
            new ComparisonItem(id, 389045, "dvldfsv", null, 47L),
            new ComparisonItem(id, 389045, "33333", null,54L),
            new ComparisonItem(id, 389045, "33333", null, 213L),
            new ComparisonItem(id, 31241, "33333", null, 75643L),
            new ComparisonItem(id, 235234, "lkpojoi", null, 99999L)
        )));
        List<ComparisonItem> items = comparisonService.getItems(id);
        assertEquals(4, items.size());
        ComparisonItem item = items.stream().filter(i -> "34ru58gj".equals(i.getProductId())).findAny()
            .orElseThrow(AssertionError::new);
        assertEquals(389045L, item.getCategoryId());
        assertEquals(34L, item.getRegionId().longValue());
        item = items.stream().filter(i -> "dvldfsv".equals(i.getProductId())).findAny()
            .orElseThrow(AssertionError::new);
        assertEquals(389045L, item.getCategoryId());
        assertEquals(47L, item.getRegionId().longValue());
        item = items.stream().filter(i -> "33333".equals(i.getProductId())).findAny()
            .orElseThrow(AssertionError::new);
        assertEquals(31241, item.getCategoryId());
        assertEquals(75643L, item.getRegionId().longValue());
        item = items.stream().filter(i -> "lkpojoi".equals(i.getProductId())).findAny()
            .orElseThrow(AssertionError::new);
        assertEquals(235234, item.getCategoryId());
        assertEquals(99999L, item.getRegionId().longValue());
    }

    @Test
    public void saveItemsSafe() throws Exception {
        Identity identity = new YandexUid("sdljkaOI");
        long id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity);
        assertTrue(comparisonService.saveItemsSafe(identity, Arrays.asList(
            new ComparisonItem(id, 389045, "34ru58gj"),
            new ComparisonItem(id, 389045, "dvldfsv"),
            new ComparisonItem(id, 389045, "33333"),
            new ComparisonItem(id, 389045, "33333"),
            new ComparisonItem(id, 31241, "33333"),
            new ComparisonItem(id, 235234, "lkpojoi")
        )));
        List<ComparisonItem> items = comparisonService.getItems(id);
        assertEquals(4, items.size());
    }

    @Test
    public void removeItem() throws Exception {
        Identity identity = new Uid(32433242L);
        long id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity);
        long categoryId = 8988;
        String productId = "2dlskjfxcv";
        Long sku = 123L;
        assertTrue(comparisonService.saveItem(identity, new ComparisonItem(id, categoryId, productId, sku)));
        assertTrue(comparisonService.removeItem(identity, productId, sku, id));
        assertNull(comparisonService.getItem(id, productId, sku));
    }

    @Test
    public void removeItemNotOwned() throws Exception {
        Identity identity1 = new Uid(73244L);
        long id1 = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity1);
        Identity identity2 = new Uid(73245L);
        long id2 = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity2);
        long categoryId = 95689;
        String productId = "loioi542";
        Long sku = 123L;
        assertTrue(comparisonService.saveItem(identity1, new ComparisonItem(id1, categoryId, productId, sku)));
        assertFalse(comparisonService.removeItem(identity2, productId, sku, id2));
        assertNotNull(comparisonService.getItem(id1, productId, sku));
    }

    @Test
    public void removeItemsByProductId() throws  Exception {
        Identity identity = new Uid(32433242L);
        long id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity);
        long categoryId = 8988;
        String productId = "2dlskjfxcv";
        Long sku1 = 1111L;
        Long sku2 = 2222L;
        assertTrue(comparisonService.saveItem(identity, new ComparisonItem(id, categoryId, productId, sku1)));
        assertTrue(comparisonService.saveItem(identity, new ComparisonItem(id, categoryId, productId, sku2)));
        assertTrue(comparisonService.removeItems(identity, productId, id));
        assertNull(comparisonService.getItem(id, productId, sku1));
        assertNull(comparisonService.getItem(id, productId, sku2));
    }

    @Test
    public void removeItems() throws Exception {
        Identity identity = new Uuid("324lkj8nm");
        long id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity);
        assertTrue(comparisonService.saveItems(identity, Arrays.asList(
            new ComparisonItem(id, 45563, "34rulkngfw58gj"),
            new ComparisonItem(id, 45563, "333jnn33"),
            new ComparisonItem(id, 235234, "lkpojoi")
        )));
        assertEquals(3, comparisonService.getItems(id).size());
        assertTrue(comparisonService.removeItems(identity, 45563, id));
        assertEquals(1, comparisonService.getItems(id).size());
        assertEquals(1, comparisonService.getComparisonsCnt(id));
    }

    @Test
    public void removeUserItems() throws Exception {
        Identity identity = new Uid(7324467L);
        long id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity);
        assertTrue(comparisonService.saveItems(identity, Arrays.asList(
            new ComparisonItem(id, 45563, "34rulkngfw58gj"),
            new ComparisonItem(id, 45563, "333jnn33"),
            new ComparisonItem(id, 235234, "lkpojoi"),
            new ComparisonItem(id, 90091, "gdfsgdfsg")
        )));
        assertEquals(4, comparisonService.getItems(id).size());
        assertTrue(comparisonService.removeItems(identity, id));
        assertEquals(0, comparisonService.getItems(id).size());
        assertEquals(0, comparisonService.getComparisonsCnt(id));
    }

    @Test
    public void testMergeEmptyList() throws Exception {
        Identity identityFrom = new Uuid("9382hjas");
        long idFrom = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identityFrom);
        Identity identityTo = new Uid(21368L);
        long idTo = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identityTo);
        comparisonService.merge(idFrom, identityFrom, idTo, identityTo, 213L);
        List<ComparisonItem> itemsFrom = comparisonService.getItems(idFrom);
        assertEquals(0, itemsFrom.size());
        List<ComparisonItem> itemsTo = comparisonService.getItems(idTo);
        assertEquals(0, itemsTo.size());
    }

    @Test
    public void testMergeWithNullRegion() throws Exception {
        Identity identityFrom = new Uuid("324lkj8nm");
        long idFrom = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identityFrom);
        Identity identityTo = new Uid(324324L);
        long idTo = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identityTo);
        long categoryId = 323468L;
        String productId = "327ewfjnfwheiuf23";
        Long sku = 12345L;

        assertTrue(comparisonService.saveItem(identityTo,
            new ComparisonItem(idTo, categoryId, productId, sku, 34L)
        ));
        Thread.sleep(2000);
        assertTrue(comparisonService.saveItems(identityFrom, Arrays.asList(
            new ComparisonItem(idFrom, categoryId, productId, sku, 213L)
        )));
        comparisonService.merge(idFrom, identityFrom, idTo, identityTo, null);
        List<ComparisonItem> itemsFrom = comparisonService.getItems(idFrom);
        assertEquals(0, itemsFrom.size());
        List<ComparisonItem> itemsTo = comparisonService.getItems(idTo);
        assertEquals(1, itemsTo.size());
        assertEquals(categoryId, itemsTo.get(0).getCategoryId());
        assertEquals(productId, itemsTo.get(0).getProductId());
        assertEquals(idTo, itemsTo.get(0).getIdentityId());
        assertEquals(213L, itemsTo.get(0).getRegionId().longValue());
        assertEquals(sku, itemsTo.get(0).getSku());
    }

    @Test
    public void testMerge() throws Exception {
        Identity identityFrom = new Uuid("324lkj8nm");
        long idFrom = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identityFrom);
        Identity identityTo = new Uid(324324L);
        long idTo = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identityTo);

        long categoryIdInBoth = 323468L;
        String productIdInBoth = "327ewfjnfwheiuf23";

        assertTrue(comparisonService.saveItems(identityTo, Arrays.asList(
            new ComparisonItem(idTo, 983645, "32r9y7wefo"),
            new ComparisonItem(idTo, categoryIdInBoth, productIdInBoth)
        )));
        List<ComparisonItem> itemsTo = comparisonService.getItems(idTo);
        assertEquals(2, itemsTo.size());

        Thread.sleep(2000);

        assertTrue(comparisonService.saveItems(identityFrom, Arrays.asList(
            new ComparisonItem(idFrom, categoryIdInBoth, "237ydwefkjwfw"),
            new ComparisonItem(idFrom, categoryIdInBoth, "32ed98fsjdf"),
            new ComparisonItem(idFrom, 12398, "328dwjefslj1"),
            new ComparisonItem(idFrom, 983645, "1kuh1hf9"),
            new ComparisonItem(idFrom, 983645, "d8732dhsd"),
            new ComparisonItem(idFrom, categoryIdInBoth, productIdInBoth)
        )));
        List<ComparisonItem> itemsFrom = comparisonService.getItems(idFrom);
        assertEquals(6, itemsFrom.size());

        // sorted by time
        assertEquals(productIdInBoth, itemsFrom.get(0).getProductId());
        Date lastTimestampInBoth = itemsFrom.get(0).getCreationTime();

        // sorted by time
        assertEquals(productIdInBoth, itemsTo.get(0).getProductId());
        assertTrue(lastTimestampInBoth.getTime() > itemsTo.get(0).getCreationTime().getTime());

        comparisonService.merge(idFrom, identityFrom, idTo, identityTo, null);

        List<ComparisonItem> itemsFromAfterMerge = comparisonService.getItems(idFrom);
        assertEquals(0, itemsFromAfterMerge.size());
        List<ComparisonItem> itemsToAfterMerge = comparisonService.getItems(idTo);
        assertEquals(7, itemsToAfterMerge.size());

        for (ComparisonItem item : itemsFrom) {
            assertTrue(itemsToAfterMerge.stream().anyMatch(i ->
                Objects.equals(i.getProductId(), item.getProductId())
                    && Objects.equals(i.getCategoryId(), item.getCategoryId())));
        }

        for (ComparisonItem item : itemsTo) {
            assertTrue(itemsToAfterMerge.stream().anyMatch(i ->
                Objects.equals(i.getProductId(), item.getProductId())
                    && Objects.equals(i.getCategoryId(), item.getCategoryId())));
        }

        assertTrue(itemsToAfterMerge.stream().anyMatch(i ->
            Objects.equals(categoryIdInBoth, i.getCategoryId())
                && Objects.equals(productIdInBoth, i.getProductId())
                && Objects.equals(lastTimestampInBoth, i.getCreationTime())
        ));
    }
}
