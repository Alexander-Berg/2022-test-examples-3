package ru.yandex.market.mbo.mdm.common.masterdata.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.google.common.base.Ticker;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;

/**
 * @author dmserebr
 * @date 13/05/2020
 */
@SuppressWarnings("checkstyle:magicNumber")
public class StorageKeyValueCachingServiceTest {
    private static final Instant INSTANT = Instant.now();

    private StorageKeyValueServiceMock keyValueServiceMock;
    private StorageKeyValueCachingServiceMock keyValueCachingServiceMock;
    private Instant currentTs = INSTANT;

    @Before
    public void before() {
        keyValueServiceMock = Mockito.spy(StorageKeyValueServiceMock.class);

        Ticker ticker = new Ticker() {
            @Override
            public long read() {
                // Timestamp in nanoseconds
                return currentTs.toEpochMilli() * 1_000_000;
            }
        };
        keyValueCachingServiceMock = new StorageKeyValueCachingServiceMock(keyValueServiceMock, ticker);
    }

    @Test
    public void testAddAndGet() {
        keyValueServiceMock.putValue("testKey", "testValue");
        keyValueServiceMock.putValue("anotherTestKey", 123);
        keyValueServiceMock.putValue("listKey", List.of(1, 2));

        Assertions.assertThat(keyValueCachingServiceMock.getValue("testKey", String.class)).isEqualTo("testValue");
        Assertions.assertThat(keyValueCachingServiceMock.getValue("anotherTestKey", Integer.class)).isEqualTo(123);
        Assertions.assertThat(keyValueCachingServiceMock.getList("listKey", Integer.class)).isEqualTo(List.of(1, 2));
    }

    @Test
    public void testIfMultipleGetShouldCallDelegateOnlyOnce() {
        keyValueServiceMock.putValue("testKey", "testValue");
        keyValueServiceMock.putValue("anotherTestKey", 123);
        keyValueServiceMock.putValue("listKey", List.of(1, 2));

        Assertions.assertThat(keyValueCachingServiceMock.getValue("testKey", String.class)).isEqualTo("testValue");
        Assertions.assertThat(keyValueCachingServiceMock.getValue("anotherTestKey", Integer.class)).isEqualTo(123);
        Assertions.assertThat(keyValueCachingServiceMock.getList("listKey", Integer.class)).isEqualTo(List.of(1, 2));

        Mockito.verify(keyValueServiceMock, Mockito.times(2)).getValue(Mockito.any(String.class), Mockito.any());
        Mockito.verify(keyValueServiceMock, Mockito.times(1)).getList(Mockito.any(String.class), Mockito.any());

        // read again
        Assertions.assertThat(keyValueCachingServiceMock.getValue("testKey", String.class)).isEqualTo("testValue");
        Assertions.assertThat(keyValueCachingServiceMock.getValue("anotherTestKey", Integer.class)).isEqualTo(123);
        Assertions.assertThat(keyValueCachingServiceMock.getList("listKey", Integer.class)).isEqualTo(List.of(1, 2));

        // only first read results in calls into delegate
        Mockito.verify(keyValueServiceMock, Mockito.times(2)).getValue(Mockito.any(String.class), Mockito.any());
        Mockito.verify(keyValueServiceMock, Mockito.times(1)).getList(Mockito.any(String.class), Mockito.any());
    }

    @Test
    public void testAddGetAfterSomeTime() {
        keyValueServiceMock.putValue("testKey", "testValue");
        keyValueServiceMock.putValue("anotherTestKey", 123);
        keyValueServiceMock.putValue("listKey", List.of(1, 2));

        currentTs = INSTANT.plus(1, ChronoUnit.HOURS);

        Assertions.assertThat(keyValueCachingServiceMock.getValue("testKey", String.class)).isEqualTo("testValue");
        Assertions.assertThat(keyValueCachingServiceMock.getValue("anotherTestKey", Integer.class)).isEqualTo(123);
        Assertions.assertThat(keyValueCachingServiceMock.getList("listKey", Integer.class)).isEqualTo(List.of(1, 2));

        // values are read from delegate, not cache
        Mockito.verify(keyValueServiceMock, Mockito.times(2)).getValue(Mockito.any(String.class), Mockito.any());
        Mockito.verify(keyValueServiceMock, Mockito.times(1)).getList(Mockito.any(String.class), Mockito.any());
    }

    @Test
    public void testCanStoreNullsOrEmptyListsInCache() {
        // repeated attempt to load absent value does not result in many delegate calls
        Assertions.assertThat(keyValueCachingServiceMock.getValue("testKey", String.class)).isNull();
        Assertions.assertThat(keyValueCachingServiceMock.getValue("testKey", String.class)).isNull();
        Assertions.assertThat(keyValueCachingServiceMock.getValue("testKey", String.class)).isNull();
        Assertions.assertThat(keyValueCachingServiceMock.getList("testAnotherKey", String.class)).isEmpty();

        Mockito.verify(keyValueServiceMock, Mockito.times(1)).getValue(Mockito.any(String.class), Mockito.any());
        Mockito.verify(keyValueServiceMock, Mockito.times(1)).getList(Mockito.any(String.class), Mockito.any());

        currentTs = INSTANT.plus(1, ChronoUnit.HOURS);

        Assertions.assertThat(keyValueCachingServiceMock.getValue("testKey", String.class)).isNull();
        Assertions.assertThat(keyValueCachingServiceMock.getList("testAnotherKey", String.class)).isEmpty();

        Mockito.verify(keyValueServiceMock, Mockito.times(2)).getValue(Mockito.any(String.class), Mockito.any());
        Mockito.verify(keyValueServiceMock, Mockito.times(2)).getList(Mockito.any(String.class), Mockito.any());
    }
}
