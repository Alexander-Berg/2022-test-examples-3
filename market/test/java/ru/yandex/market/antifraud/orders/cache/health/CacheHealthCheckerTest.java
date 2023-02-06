package ru.yandex.market.antifraud.orders.cache.health;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author dzvyagin
 */
public class CacheHealthCheckerTest {

    private CacheHealthChecker healthChecker;
    private Cache cache;

    @Before
    public void init() {
        CacheManager cacheManager = mock(CacheManager.class);
        cache = mock(Cache.class);
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        healthChecker = new CacheHealthChecker(cacheManager);
    }

    @Test
    public void getStatusOK() {
        var status = healthChecker.getStatus();
        assertThat(status.getState()).isEqualTo(CacheState.OK);
    }

    @Test
    public void getStatusFlappingRead() {
        when(cache.get(anyString(), eq(CacheStatus.class)))
                .thenAnswer(i -> {
                    TimeUnit.MILLISECONDS.sleep(200);
                    return null;
                });
        var status = healthChecker.getStatus();
        assertThat(status.getState()).isEqualTo(CacheState.FLAPPING);
    }

    @Test
    public void getStatusFlappingWrite() {
        doAnswer(i -> {
            TimeUnit.MILLISECONDS.sleep(200);
            return null;
        }).when(cache).put(anyString(), any());
        var status = healthChecker.getStatus();
        assertThat(status.getState()).isEqualTo(CacheState.FLAPPING);
    }


    @Test
    public void getStatusUnavailable() {
        when(cache.get(anyString(), eq(CacheStatus.class)))
                .thenThrow(new RuntimeException());
        var status = healthChecker.getStatus();
        assertThat(status.getState()).isEqualTo(CacheState.UNAVAILABLE);
    }

    @Test
    public void getStatString() {
        healthChecker.checkHealth();
        healthChecker.checkHealth();
        healthChecker.checkHealth();
        String status = healthChecker.getStatString();
        System.out.println(status);
        status = status.replaceAll("\n", " ");
        assertThat(status).matches(".*Time period: \\d{1,3} ms.*");
        assertThat(status).matches(".*Status OK: count - 3, AVG check time - \\d{1,3}.\\d{3} ms.*");
        assertThat(status).matches(".*Status FLAPPING: count - 0, AVG check time = n/a ms.*");
        assertThat(status).matches(".*Status UNAVAILABLE: count - 0, AVG time = n/a ms.*");
    }
}
