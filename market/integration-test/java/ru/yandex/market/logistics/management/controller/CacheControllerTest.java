package ru.yandex.market.logistics.management.controller;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import ru.yandex.market.logistics.management.AbstractContextualTest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CacheControllerTest extends AbstractContextualTest {

    private static final String CACHE_NAME = "cacheName";
    private static final String ADDITIONAL_CACHE_NAME = "additionalCacheName";

    @Mock
    private Cache cacheMock;

    @Mock
    private Cache additionalCacheMock;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void evictByName() throws Exception {
        when(cacheManager.getCache(CACHE_NAME)).thenReturn(cacheMock);

        mockMvc.perform(patch("/cache/evict/" + CACHE_NAME))
            .andExpect(status().isOk())
            .andExpect(content().string(""));

        verify(cacheMock).clear();
    }

    @Test
    void evictByNameNotFound() throws Exception {
        when(cacheManager.getCache(CACHE_NAME)).thenReturn(null);

        mockMvc.perform(patch("/cache/evict/" + CACHE_NAME))
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Cache not exists cacheName"));
    }

    @Test
    void evictAll() throws Exception {
        when(cacheManager.getCacheNames()).thenReturn(ImmutableList.of(CACHE_NAME, ADDITIONAL_CACHE_NAME));
        when(cacheManager.getCache(CACHE_NAME)).thenReturn(cacheMock);
        when(cacheManager.getCache(ADDITIONAL_CACHE_NAME)).thenReturn(additionalCacheMock);

        mockMvc.perform(patch("/cache/evict-all"))
            .andExpect(status().isOk())
            .andExpect(content().string(""));

        verify(cacheMock).clear();
        verify(additionalCacheMock).clear();
    }
}
