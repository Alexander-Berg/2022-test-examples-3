package ru.yandex.market.mbo.tms.redis;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.params.CategoryParametersExtractorService;
import ru.yandex.market.mbo.redis.categories.CategoryRedisReader;
import ru.yandex.market.mbo.redis.categories.CategoryRedisWriter;
import ru.yandex.market.mbo.tms.redis.queue.RedisUpdateResult;
import ru.yandex.market.mbo.utils.OracleDbTimestampGetter;

import java.util.Optional;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:magicnumber")
public class UpdateRedisCategoryServiceTest {

    private CategoryRedisReader categoryRedisReader;
    private CategoryRedisWriter categoryRedisWriter;
    private OracleDbTimestampGetter oracleDbTimestampGetter;
    private UpdateRedisCategoryService updateRedisCategoryService;


    @Before
    public void setUp() {
        updateRedisCategoryService = new UpdateRedisCategoryService(
            categoryRedisWriter = mock(CategoryRedisWriter.class),
            categoryRedisReader = mock(CategoryRedisReader.class),
            mock(CategoryParametersExtractorService.class),
            oracleDbTimestampGetter = mock(OracleDbTimestampGetter.class));

    }

    @Test
    public void onUpdateShouldCallWriterNoRedisTsFound() {
        long hid = 1L;
        when(categoryRedisReader.getCategoryTs(eq(hid)))
            .thenReturn(Optional.empty());
        when(oracleDbTimestampGetter.getCurrentUtcMillis())
            .thenReturn(1L);
        updateRedisCategoryService.updateCategory(hid, 1L);
        verify(categoryRedisWriter, times(1))
            .putCategory(anyLong(), any());
    }

    @Test
    public void onUpdateShouldCallWriterIfCalledTsIsGreaterThanRedisTs() {
        long hid = 1L;
        when(categoryRedisReader.getCategoryTs(eq(hid)))
            .thenReturn(Optional.of(4L));
        when(categoryRedisWriter.putCategory(anyLong(), any()))
            .thenReturn(10L);
        when(oracleDbTimestampGetter.getCurrentUtcMillis())
            .thenReturn(1L);
        updateRedisCategoryService.updateCategory(hid, 5L);
        verify(categoryRedisWriter, times(1))
            .putCategory(anyLong(), any());
    }

    @Test
    public void onUpdateShouldCallWriterIfCalledWithoutTs() {
        long hid = 1L;
        when(categoryRedisReader.getCategoryTs(eq(hid)))
            .thenReturn(Optional.of(4L));
        when(categoryRedisWriter.putCategory(anyLong(), any()))
            .thenReturn(10L);
        when(oracleDbTimestampGetter.getCurrentUtcMillis())
            .thenReturn(1L);
        updateRedisCategoryService.updateCategory(hid);
        verify(categoryRedisWriter, times(1))
            .putCategory(anyLong(), any());
    }

    @Test
    public void onUpdateShouldNotCallIfRedisTsIsGreaterOrEquals() {
        long hid = 1L;
        long timestamp = 4L;
        when(categoryRedisReader.getCategoryTs(eq(hid)))
            .thenReturn(Optional.of(timestamp));
        when(oracleDbTimestampGetter.getCurrentUtcMillis())
            .thenReturn(1L);
        RedisUpdateResult handle = updateRedisCategoryService.updateCategory(hid, timestamp);
        Assertions.assertThat(handle).isEqualTo(RedisUpdateResult.SKIPPED);
        verify(categoryRedisWriter, times(0))
            .putCategory(anyLong(), any());

        when(categoryRedisReader.getCategoryTs(eq(hid)))
            .thenReturn(Optional.of(timestamp + 1));
        handle = updateRedisCategoryService.updateCategory(hid, timestamp);
        Assertions.assertThat(handle).isEqualTo(RedisUpdateResult.SKIPPED);
        verify(categoryRedisWriter, times(0))
            .putCategory(anyLong(), any());
    }

    @Test
    public void onRemoveShouldCallWriterNoRedisTsFound() {
        long hid = 1L;
        when(categoryRedisReader.getCategoryTs(eq(hid)))
            .thenReturn(Optional.empty());
        updateRedisCategoryService.removeCategory(hid, 1L);
        verify(categoryRedisWriter, times(1))
            .removeCategory(anyLong());
    }

    @Test
    public void onRemoveShouldCallWriterIfCalledTsIsGreaterThanRedisTs() {
        long hid = 1L;
        when(categoryRedisReader.getCategoryTs(eq(hid)))
            .thenReturn(Optional.of(4L));
        updateRedisCategoryService.removeCategory(hid, 5L);
        verify(categoryRedisWriter, times(1))
            .removeCategory(anyLong());
    }

    @Test
    public void onRemoveShouldNotCallIfRedisTsIsGreater() {
        long hid = 1L;
        long timestamp = 4L;
        when(categoryRedisReader.getCategoryTs(eq(hid)))
            .thenReturn(Optional.of(timestamp + 1));
        RedisUpdateResult handle = updateRedisCategoryService.removeCategory(hid, timestamp);
        Assertions.assertThat(handle).isEqualTo(RedisUpdateResult.SKIPPED);
        verify(categoryRedisWriter, times(0))
            .removeCategory(anyLong());
    }
}
