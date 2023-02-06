package ru.yandex.market.wms.common.spring.service.unit;

import java.time.Clock;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.wms.common.model.enums.CounterName;
import ru.yandex.market.wms.common.spring.dao.implementation.CounterDao;
import ru.yandex.market.wms.common.spring.service.CounterService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.common.spring.service.CounterService.DEFAULT_CACHE_SIZE;

class CounterServiceTest {
    private static final String FORMAT = "%010d";

    private static final ExecutorService CACHED_THREAD_POOL = Executors.newCachedThreadPool();

    @Test
    void getNextKey() {
        CounterName counterName = CounterName.ITRN_SERIAL_KEY;
        int oldKey = 100;

        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(oldKey + DEFAULT_CACHE_SIZE)
                .thenReturn(oldKey + DEFAULT_CACHE_SIZE * 2);

        CounterDao dao = new CounterDao(jdbc, Clock.systemUTC());
        CounterService keyGenService = new CounterService(dao);

        int expectedNextKey = oldKey + 1;

        for (int i = 0; i < DEFAULT_CACHE_SIZE; i++) {
            String nextKey = keyGenService.getNextCounterValue(counterName, FORMAT);
            assertEquals(String.format(FORMAT, expectedNextKey++), nextKey);
        }
        verify(jdbc).queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class));

        // потратили кеш, новые значения должны подтянуться из базы
        String nextKey = keyGenService.getNextCounterValue(counterName, FORMAT);
        assertEquals(String.format(FORMAT, expectedNextKey), nextKey);
        verify(jdbc, times(2)).queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class));
    }

    @Test
    void getNextKeyConcurrently() throws InterruptedException {
        CounterName key1 = CounterName.ITRN_KEY;
        CounterName key2 = CounterName.ITRN_SERIAL_KEY;
        CounterName key3 = CounterName.TASK_DETAIL_KEY;
        CounterName key4 = CounterName.PICK_DETAIL_KEY;

        int cyclesCount = 10_000;
        int iterationsCount = cyclesCount * DEFAULT_CACHE_SIZE;

        MockCounterDao dao = spy(new MockCounterDao());
        CounterService counterService = new CounterService(dao);

        Callable<Void> task1 = () -> {
            for (int i = 0; i < iterationsCount; i++) {
                counterService.getNextCounterValue(key1);
            }
            return null;
        };
        Callable<Void> task2 = () -> {
            for (int i = 0; i < iterationsCount; i++) {
                counterService.getNextCounterValue(key1);
                counterService.getNextCounterValue(key2);
            }
            return null;
        };
        Callable<Void> task3 = () -> {
            for (int i = 0; i < iterationsCount; i++) {
                counterService.getNextCounterValue(key1);
                counterService.getNextCounterValue(key2);
                counterService.getNextCounterValue(key3);
            }
            return null;
        };
        Callable<Void> task4 = () -> {
            for (int i = 0; i < iterationsCount; i++) {
                counterService.getNextCounterValue(key1);
                counterService.getNextCounterValue(key2);
                counterService.getNextCounterValue(key3);
                counterService.getNextCounterValue(key4);
            }
            return null;
        };

        CACHED_THREAD_POOL.invokeAll(Arrays.asList(task1, task2, task3, task4), 10, TimeUnit.SECONDS);

        assertEquals(4 * iterationsCount, dao.currentValues.get(key1));
        assertEquals(3 * iterationsCount, dao.currentValues.get(key2));
        assertEquals(2 * iterationsCount, dao.currentValues.get(key3));
        assertEquals(1 * iterationsCount, dao.currentValues.get(key4));

        verify(dao, times(4 * cyclesCount)).getNextCounterValues(key1, DEFAULT_CACHE_SIZE);
        verify(dao, times(3 * cyclesCount)).getNextCounterValues(key2, DEFAULT_CACHE_SIZE);
        verify(dao, times(2 * cyclesCount)).getNextCounterValues(key3, DEFAULT_CACHE_SIZE);
        verify(dao, times(1 * cyclesCount)).getNextCounterValues(key4, DEFAULT_CACHE_SIZE);
    }

    private static class MockCounterDao extends CounterDao {
        private final Map<CounterName, Integer> currentValues = new HashMap<>();

        MockCounterDao() {
            super(null, null);
        }

        @Override
        public IntStream getNextCounterValues(CounterName counterName, int size) {
            int oldValue = currentValues.getOrDefault(counterName, 0);
            int newValue = oldValue + DEFAULT_CACHE_SIZE;
            currentValues.put(counterName, newValue);
            return IntStream.rangeClosed(oldValue + 1, newValue);
        }
    }
}
