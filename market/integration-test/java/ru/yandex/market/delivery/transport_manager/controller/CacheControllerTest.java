package ru.yandex.market.delivery.transport_manager.controller;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

public class CacheControllerTest extends AbstractContextualTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void init() {
        ConcurrentMapCacheManager concurrentMapCacheManager = (ConcurrentMapCacheManager) cacheManager;
        concurrentMapCacheManager.setCacheNames(List.of("test"));
        concurrentMapCacheManager.getCache("test").put("testkey", "testvalue");
    }

    @Test
    void testClearAll() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/cache/clear"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        softly.assertThat(cacheManager.getCache("test").get("testkey")).isNull();
    }

    @Test
    void testClearConcreteCache() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/cache/test/clear"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        softly.assertThat(cacheManager.getCache("test").get("testkey")).isNull();
    }
}
