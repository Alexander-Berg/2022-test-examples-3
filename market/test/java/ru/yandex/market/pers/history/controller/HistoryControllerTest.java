package ru.yandex.market.pers.history.controller;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.pers.history.Item;
import ru.yandex.market.pers.history.dj.DjClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class HistoryControllerTest extends AbstractHistoryControllerTest {

    @Autowired
    private DjClient djClient;

    @Before
    public void setUp() {
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Test
    public void testGetHistoryIsCached() throws Exception {
        addHistoryForUid(createItem());

        assertEquals(0, cache.size());
        getHistoryUid();
        assertEquals(1, cache.size());
        getHistoryUid();
        assertEquals(1, cache.size());
    }

    @Test
    public void differentCacheKeysForDifferentMarketColors() throws Exception {
        addHistoryForUid(createItem());

        assertEquals(0, cache.size());
        getHistoryUid();
        assertEquals(1, cache.size());
        getBlueHistoryUid();
        assertEquals(2, cache.size());
    }

    @Test
    public void testHistoryDj() throws Exception {

        Mockito.when(djClient.getHistory(any(), anyString(), anyInt(), any())).thenReturn(
            Collections.emptyList()).thenReturn(Collections.singletonList(createBlueItem(RESOURCE_ID, SKU)));

        addHistoryForUid(createBlueItem(RESOURCE_ID, SKU));

        List<Item> items = getBlueHistoryUid();
        assertEquals(0, items.size());

        addHistoryForUid(createBlueItem(RESOURCE_ID, ANOTHER_SKU));

        items = getBlueHistoryUid();
        assertEquals(1, items.size());
        assertHasItem(items, RESOURCE_ID, SKU);
    }

    @Test
    public void testHistoryYandexUid() throws Exception {

        Mockito.when(djClient.getHistory(any(), anyString(), anyInt(), any())).thenReturn(
            Collections.emptyList()).thenReturn(Collections.singletonList(createBlueItemYandexUid(RESOURCE_ID, SKU)));

        addHistoryForYandexuid(createBlueItemYandexUid(RESOURCE_ID, SKU));

        List<Item> items = getHistoryYandexUid(null, null);
        assertEquals(0, items.size());

        addHistoryForYandexuid(createBlueItemYandexUid(RESOURCE_ID, ANOTHER_SKU));

        items = getHistoryYandexUid(null, null);
        assertEquals(1, items.size());
        assertHasItem(items, RESOURCE_ID, SKU);
    }

    private void assertHasItem(List<Item> items, Long resourceId, String sku) {
        assertTrue(items.stream()
            .anyMatch(item -> resourceId.equals(item.getResourceId()) && sku.equals(item.getMarketSku())));
    }

    private void checkHistorySize(int expectedSize) throws Exception {
        List<Item> resultItems = getHistoryUid();
        assertEquals(expectedSize, resultItems.size());
    }

    private List<Item> getHistoryWithLimit(long limit) throws Exception {
        return getHistoryUid(null, limit);
    }

    private List<Item> getHistoryUid() throws Exception {
        return getHistoryUid(null, null);
    }

    private List<Item> getBlueHistoryUid() throws Exception {
        return getHistoryUid("blue", null);
    }

    private List<Item> getHistoryUid(String rgb, Long limit) throws Exception {
        return getHistoryUid(rgb, limit, HISTORY_URL_UID);
    }

    private List<Item> getHistoryYandexUid(String rgb, Long limit) throws Exception {
        return getHistoryUid(rgb, limit, HISTORY_URL_YANDEXUID);
    }

    private List<Item> getHistoryUid(String rgb, Long limit, String url) throws Exception {
        MockHttpServletRequestBuilder request = get(url);
        if (rgb != null) {
            request = request.param("rgb", rgb);
        }
        if (limit != null) {
            request = request.param("limit", limit.toString());
        }
        Result result = objectMapper.readValue(
            mockMvc.perform(request.accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Result.class);
        assertNotNull(result.data);
        return result.data;
    }

}
