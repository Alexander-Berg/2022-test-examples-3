package ru.yandex.market.checkout.checkouter.util.cache;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import com.google.common.util.concurrent.UncheckedExecutionException;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.util.cahce.Cache;
import ru.yandex.market.checkout.checkouter.util.cahce.DuplicatedCacheBuilder;
import ru.yandex.market.common.zk.ZooClient;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class DuplicatedCacheTest extends AbstractServicesTestBase {

    private final String zooCachePath = "/checkout/duplicated-cache-test";
    private ZooClient zooClient;
    private Cache<Long, TestValue> cache;
    private final AtomicBoolean shouldThrowWhenLoading = new AtomicBoolean(false);
    private final Function<Long, TestValue> loader = Mockito.spy(new Loader());
    private final TestValue value1 = loader.apply(1L);
    private final TestValue value2 = loader.apply(2L);
    private final Function<Long, String> keySerializer = String::valueOf;
    private final Function<TestValue, String> valueSerializer = value -> value.id + "_" + value.message;
    private final Function<String, TestValue> valueDeserializer = valueString -> {
        String[] parts = valueString.split("_");
        return new TestValue(Long.parseLong(parts[0]), parts[1]);
    };

    @Autowired
    public void setZooClient(ZooClient zooClient) {
        this.zooClient = Mockito.spy(zooClient);
    }

    @BeforeEach
    public void setUp() throws KeeperException {
        zooClient.setOrCreateData(zooCachePath, "");
        cache = DuplicatedCacheBuilder
                .newLoadingZooCacheBuilder(loader, zooClient, zooCachePath, valueDeserializer)
                .withKeySerializer(keySerializer)
                .withValueSerializer(valueSerializer)
                .expireLocalAfterWrite(1000, TimeUnit.MILLISECONDS)
                .expireGlobalAfterWrite(2000, TimeUnit.MILLISECONDS)
                .withClock(getClock())
                .build();
        resetMocks();
    }

    @AfterEach
    public void tearDown() throws KeeperException {
        zooClient.deleteWithChildren(zooCachePath);
    }

    @Test
    public void checkCacheNotExpireWithFixedClock() throws Exception {
        setFixedTime(getClock().instant());
        cache.get(value1.id);
        verifyDataSource(DataSource.LOADER);

        Thread.sleep(3000); // Ждем, когда при нормальном течениии времени кэш протухнет

        // Проверяем, что значение достается из локального кэша
        cache.get(value1.id);
        verifyDataSource(DataSource.LOCAL_CACHE);
    }

    @Test
    public void uniquenessTest() throws Exception {
        assertThat(cache.get(value1.id), equalTo(value1));
        verifyDataSource(DataSource.LOADER);
        assertThat(cache.get(value2.id), equalTo(value2));
        verifyDataSource(DataSource.LOADER);
        assertThat(cache.get(value2.id), equalTo(value2));
        verifyDataSource(DataSource.LOCAL_CACHE);
        assertThat(cache.get(value1.id), equalTo(value1));
        verifyDataSource(DataSource.LOCAL_CACHE);
    }

    @Test
    public void timingTest() throws Exception {
        // Кладем в кэш
        cache.get(value1.id);
        verifyDataSource(DataSource.LOADER);

        // Ломаем лоадер, достаем из кэша
        shouldThrowWhenLoading.set(true);
        TestValue value = cache.get(value1.id);
        assertThat(value, equalTo(value1));
        verifyDataSource(DataSource.LOCAL_CACHE);

        // Кэш еще не протух
        Thread.sleep(500); // Прошло 500 мс
        value = cache.get(value1.id);
        assertThat(value, equalTo(value1));
        verifyDataSource(DataSource.LOCAL_CACHE);

        // Локальный кэш протух
        Thread.sleep(1000); // Прошло 1500 мс
        value = cache.get(value1.id);
        assertThat(value, equalTo(value1));
        verifyDataSource(DataSource.ZOOKEEPER);

        // Протухли и локальный, и zk кэш
        Thread.sleep(1000); // Прошло 2500 мс
        Assertions.assertThrows(UncheckedExecutionException.class, () -> cache.get(value1.id));
        verifyDataSource(DataSource.ZOOKEEPER);

        // Чиним лоадер
        shouldThrowWhenLoading.set(false);
        value = cache.get(value1.id);
        assertThat(value, equalTo(value1));
        verifyDataSource(DataSource.LOADER);

        value = cache.get(value1.id);
        assertThat(value, equalTo(value1));
        verifyDataSource(DataSource.LOCAL_CACHE);
    }

    @Test
    public void basicTest() throws Exception {
        // Ломаем поставщика, получаем ошибку, роверяем вызов лоадера
        shouldThrowWhenLoading.set(true);
        Assertions.assertThrows(UncheckedExecutionException.class, () -> cache.get(value1.id));
        verifyDataSource(DataSource.ZOOKEEPER); // Запросили лоадер, получили ошибку, пошли в zk, ничего не нашли

        // Чиним лоадера, значение сохраняется в кэше, проверяем вызов лоадера
        shouldThrowWhenLoading.set(false);
        TestValue value = cache.get(value1.id);
        assertThat(value, equalTo(value1));
        verifyDataSource(DataSource.LOADER);

        // Ломаем лоадера, достаем из кэша, проверяем, что лоадер не вызывался
        shouldThrowWhenLoading.set(true);
        value = cache.get(value1.id);
        assertThat(value, equalTo(value1));
        verifyDataSource(DataSource.LOCAL_CACHE);
    }

    private void verifyDataSource(DataSource source) throws Exception {
        if (source == DataSource.LOCAL_CACHE) { // Берем из локального кэша (не вызываем лоадер и zk)
            Mockito.verify(loader, Mockito.never()).apply(Mockito.any());
            Mockito.verify(zooClient, Mockito.never()).getStringData(Mockito.anyString(), Mockito.any(Stat.class));
        } else if (source == DataSource.LOADER) { // Вызываем лоадер, не вызываем zk
            Mockito.verify(loader).apply(Mockito.any());
            Mockito.verify(zooClient, Mockito.never()).getStringData(Mockito.anyString(), Mockito.any(Stat.class));
        } else if (source == DataSource.ZOOKEEPER) { // Вызываем лоадер, но из-за ошибки идем в zk
            Mockito.verify(loader).apply(Mockito.any());
            Mockito.verify(zooClient).getStringData(Mockito.anyString(), Mockito.any(Stat.class));
        } else {
            throw new IllegalArgumentException();
        }
        resetMocks();
    }

    private void resetMocks() {
        Mockito.reset(loader, zooClient);
    }

    private enum DataSource {
        LOCAL_CACHE, LOADER, ZOOKEEPER
    }

    private static class TestValue {

        final long id;
        final String message;

        TestValue(long id, String message) {
            this.id = id;
            this.message = message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TestValue testValue = (TestValue) o;
            return id == testValue.id &&
                    Objects.equals(message, testValue.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, message);
        }
    }

    private class Loader implements Function<Long, TestValue> {

        @Override
        public TestValue apply(Long id) {
            if (shouldThrowWhenLoading.get()) {
                throw new RuntimeException();
            }
            return new TestValue(id, "This is test value with id: " + id);
        }
    }
}
