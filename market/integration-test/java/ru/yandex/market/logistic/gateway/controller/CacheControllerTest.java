package ru.yandex.market.logistic.gateway.controller;

import java.util.List;

import org.junit.Test;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.web.util.NestedServletException;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CacheControllerTest extends AbstractIntegrationTest {

    private static final String CACHE_NAME = "cacheName";
    private static final String ADDITIONAL_CACHE_NAME = "additionalCacheName";

    @Mock
    private Cache cacheMock;

    @Mock
    private Cache additionalCacheMock;

    @MockBean
    private CacheManager cacheManager;

    @Test
    public void evictByName() throws Exception {
        when(cacheManager.getCache(CACHE_NAME)).thenReturn(cacheMock);

        mockMvc.perform(patch("/cache/evict/" + CACHE_NAME))
            .andExpect(status().isOk())
            .andExpect(content().string(""));

        verify(cacheMock).clear();
    }

    @Test
    public void evictByNameNotFound() {
        when(cacheManager.getCache(CACHE_NAME)).thenReturn(null);

        softAssert.assertThatThrownBy(() -> mockMvc.perform(patch("/cache/evict/" + CACHE_NAME)))
            .isInstanceOf(NestedServletException.class)
            .hasMessage(
                "Request processing failed; nested exception is " +
                    "java.lang.IllegalArgumentException: Cache 'cacheName' not exists"
            );
    }

    @Test
    public void evictAll() throws Exception {
        when(cacheManager.getCacheNames()).thenReturn(List.of(CACHE_NAME, ADDITIONAL_CACHE_NAME));
        when(cacheManager.getCache(CACHE_NAME)).thenReturn(cacheMock);
        when(cacheManager.getCache(ADDITIONAL_CACHE_NAME)).thenReturn(additionalCacheMock);

        mockMvc.perform(patch("/cache/evict-all"))
            .andExpect(status().isOk())
            .andExpect(content().string(""));

        verify(cacheMock).clear();
        verify(additionalCacheMock).clear();
    }
}
