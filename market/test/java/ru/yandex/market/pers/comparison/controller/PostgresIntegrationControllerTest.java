package ru.yandex.market.pers.comparison.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.pers.comparison.PersComparisonTest;
import ru.yandex.market.pers.comparison.dto.ComparisonItemRequestDto;
import ru.yandex.market.pers.comparison.model.ComparisonItem;
import ru.yandex.market.pers.comparison.model.UserType;
import ru.yandex.market.pers.comparison.service.ComparisonServiceNew;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PostgresIntegrationControllerTest extends PersComparisonTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ComparisonServiceNew comparisonServicePg;

    @Test
    public void testSaveItems() throws Exception {
        long uid = 12344;
        long categoryId = 5461;
        long regionId = 2345;
        String[] productIds = new String[]{"111", "222", "333", "444"};
        List<ComparisonItemRequestDto> items = Arrays.asList(
            new ComparisonItemRequestDto(String.valueOf(categoryId), productIds[0]),
            new ComparisonItemRequestDto(String.valueOf(categoryId), productIds[1]),
            new ComparisonItemRequestDto(String.valueOf(categoryId), productIds[2]),
            new ComparisonItemRequestDto(String.valueOf(categoryId), productIds[3])
        );
        // save items
        mockMvc
            .perform(post(String.format("/comparison/%s/%d/items", "UID", uid))
                .param("regionId", String.valueOf(regionId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        // check count in pg
        int comparisonsCnt = comparisonServicePg.getItemsCnt(UserType.UID, String.valueOf(uid), categoryId);
        assertEquals(items.size(), comparisonsCnt);
        // check items
        long identityId = -1;
        List<ComparisonTestItem> expectedItems = Arrays.asList(
            new ComparisonTestItem(categoryId, productIds[0], regionId),
            new ComparisonTestItem(categoryId, productIds[1], regionId),
            new ComparisonTestItem(categoryId, productIds[2], regionId),
            new ComparisonTestItem(categoryId, productIds[3], regionId)
        );

        List<ComparisonItem> actualItems = comparisonServicePg.getItems(UserType.UID, String.valueOf(uid));
        checkComparisons(expectedItems, actualItems);
    }

    @Test
    public void testSaveDuplicatedItems() throws Exception {
        long regionId = 213;
        List<ComparisonItemRequestDto> items = Arrays.asList(
            new ComparisonItemRequestDto("342398", "2344565"),
            new ComparisonItemRequestDto("5461", "345342"),
            new ComparisonItemRequestDto("5461", "2342390"),
            new ComparisonItemRequestDto("5461", "2342390")
        );
        List<ComparisonTestItem> expectedItems = Arrays.asList(
            new ComparisonTestItem(342398L, "2344565", regionId),
            new ComparisonTestItem(5461L, "345342", regionId),
            new ComparisonTestItem(5461L, "2342390", regionId)
        );

        // save items
        mockMvc
            .perform(post(String.format("/comparison/%s/%d/items", "UID", UID))
                .param("regionId", String.valueOf(regionId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        List<ComparisonItem> actualItems = comparisonServicePg.getItems(UserType.UID, String.valueOf(UID));
        checkComparisons(expectedItems, actualItems);
    }

    @Test
    public void testRemoveItemsByCategory() throws Exception {
        long uid = 12344;
        long categoryId1 = 5461;
        long categoryId2 = categoryId1 + 10;
        long regionId = 2345;
        String[] productIds = new String[]{"111", "222", "333", "444"};
        List<ComparisonItemRequestDto> items = Arrays.asList(
            new ComparisonItemRequestDto(String.valueOf(categoryId1), productIds[0]),
            new ComparisonItemRequestDto(String.valueOf(categoryId2), productIds[1]),
            new ComparisonItemRequestDto(String.valueOf(categoryId2), productIds[2]),
            new ComparisonItemRequestDto(String.valueOf(categoryId1), productIds[3])
        );
        // save items
        mockMvc
            .perform(post(String.format("/comparison/%s/%d/items", "UID", uid))
                .param("regionId", String.valueOf(regionId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        // delete items by one category
        mockMvc
            .perform(delete(String.format("/comparison/%s/%s/category/%d", "UID", uid, categoryId1)))
            .andDo(print())
            .andExpect(status().isOk())
        ;

        // check count in pg
        int comparisonsCnt = comparisonServicePg.getItemsCnt(UserType.UID, String.valueOf(uid), categoryId1);
        assertEquals(0, comparisonsCnt);
        comparisonsCnt = comparisonServicePg.getItemsCnt(UserType.UID, String.valueOf(uid), categoryId2);
        assertEquals(2, comparisonsCnt);
        // check items
        List<ComparisonTestItem> expectedItems = Arrays.asList(
            new ComparisonTestItem(categoryId2, productIds[1], regionId),
            new ComparisonTestItem(categoryId2, productIds[2], regionId)
        );
        List<ComparisonItem> actualItems = comparisonServicePg.getItems(UserType.UID, String.valueOf(uid));
        checkComparisons(expectedItems, actualItems);
    }

    @Test
    public void testRemoveItemsWithEmptyProductId() throws Exception {
        long uid = 12344;
        long categoryId1 = 5461;
        long regionId = 2345;
        long sku = 641386498;
        String productId = "111";
        List<ComparisonItemRequestDto> items = Arrays.asList(
            new ComparisonItemRequestDto(String.valueOf(categoryId1), productId),
            new ComparisonItemRequestDto(String.valueOf(categoryId1), null, sku)
        );
        // save items
        mockMvc
            .perform(post(String.format("/comparison/%s/%d/items", "UID", uid))
                .param("regionId", String.valueOf(regionId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        // delete items by one category
        mockMvc
            .perform(delete(String.format("/comparison/%s/%s/product?productId=&sku=%s", "UID", uid, sku)))
            .andDo(print())
            .andExpect(status().isOk())
        ;

        // check count in pg
        int comparisonsCnt = comparisonServicePg.getItemsCnt(UserType.UID, String.valueOf(uid), categoryId1);
        assertEquals(1, comparisonsCnt);
        // check items
        List<ComparisonTestItem> expectedItems = Collections.singletonList(
            new ComparisonTestItem(categoryId1, productId, regionId)
        );
        List<ComparisonItem> actualItems = comparisonServicePg.getItems(UserType.UID, String.valueOf(uid));
        checkComparisons(expectedItems, actualItems);
    }

    @Test
    public void testMergeItems() throws Exception {
        String uuid = "uuid34567";
        long uid = 3456;
        long categoryId1 = 5461;
        long categoryId2 = categoryId1 + 100;
        long regionId = 2345;
        String[] productIds = new String[]{"111", "222", "333", "444", "555", "666"};
        List<ComparisonItemRequestDto> itemsForUUid = Arrays.asList(
            new ComparisonItemRequestDto(String.valueOf(categoryId1), productIds[0]),
            new ComparisonItemRequestDto(String.valueOf(categoryId2), productIds[1]),
            new ComparisonItemRequestDto(String.valueOf(categoryId1), productIds[2]),
            new ComparisonItemRequestDto(String.valueOf(categoryId2), productIds[3])
        );
        // save items for UUID
        mockMvc
            .perform(post(String.format("/comparison/%s/%s/items", "UUID", uuid))
                .param("regionId", String.valueOf(regionId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(itemsForUUid)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;
        List<ComparisonItemRequestDto> itemsForUid = Arrays.asList(
            new ComparisonItemRequestDto(String.valueOf(categoryId1), productIds[2]),
            new ComparisonItemRequestDto(String.valueOf(categoryId2), productIds[3]),
            new ComparisonItemRequestDto(String.valueOf(categoryId1), productIds[4]),
            new ComparisonItemRequestDto(String.valueOf(categoryId2), productIds[5])

        );
        // save items for UUID
        mockMvc
            .perform(post(String.format("/comparison/%s/%d/items", "UID", uid))
                .param("regionId", String.valueOf(regionId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(itemsForUid)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        // merge items from UUID to UID
        mockMvc
            .perform(patch(String.format("/comparison/%s/%s", "UUID", uuid))
                .param("userTypeTo", "UID").param("userIdTo", String.valueOf(uid)))
            .andDo(print())
            .andExpect(status().isOk())
        ;

        // check count in pg
        int comparisonsCnt = comparisonServicePg.getItemsCnt(UserType.UUID, uuid, categoryId1);
        assertEquals(0, comparisonsCnt);
        comparisonsCnt = comparisonServicePg.getItemsCnt(UserType.UID, String.valueOf(uid), categoryId2);
        assertEquals(3, comparisonsCnt);
        // check items for uid
        long identityId = -1;
        List<ComparisonTestItem> expectedItems = Arrays.asList(
            new ComparisonTestItem(categoryId1, productIds[0], regionId),
            new ComparisonTestItem(categoryId2, productIds[1], regionId),
            new ComparisonTestItem(categoryId1, productIds[2], regionId),
            new ComparisonTestItem(categoryId2, productIds[3], regionId),
            new ComparisonTestItem(categoryId1, productIds[4], regionId),
            new ComparisonTestItem(categoryId2, productIds[5], regionId)
        );

        List<ComparisonItem> actualItems = comparisonServicePg.getItems(UserType.UID, String.valueOf(uid));
        checkComparisons(expectedItems, actualItems);
        // check items for uгid
        actualItems = comparisonServicePg.getItems(UserType.UUID, uuid);
        checkComparisons(Collections.emptyList(), actualItems);
    }


    private void checkComparisons(List<ComparisonTestItem> expectedItems, List<ComparisonItem> actual) {
        List<ComparisonTestItem> actualItems = actual.stream()
            .map(it -> new ComparisonTestItem(it.getCategoryId(), it.getProductId(), it.getRegionId()))
            .collect(Collectors.toList());
        assertEquals(expectedItems.size(), actualItems.size());
        assertTrue(new HashSet<>(expectedItems).containsAll(actualItems));
    }

    class ComparisonTestItem extends ComparisonItem {
        ComparisonTestItem(long categoryId, String productId, Long regionId) {
            super(categoryId, productId, null, regionId);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ComparisonItem that = (ComparisonItem) o;

            return this.getCategoryId() == that.getCategoryId() &&
                Objects.equals(this.getProductId(), that.getProductId()) &&
                Objects.equals(this.getSku(), that.getSku()) &&
                Objects.equals(this.getRegionId(), that.getRegionId()) &&
                Objects.equals(this.getCreationTime(), that.getCreationTime());
        }
    }
}
