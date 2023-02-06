package ru.yandex.market.mbo.tms.redis;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.redis.tovartree.TovarTreeRedisReader;
import ru.yandex.market.mbo.redis.tovartree.TovarTreeRedisWriter;
import ru.yandex.market.mbo.tms.redis.queue.RedisUpdateResult;
import ru.yandex.market.mbo.utils.OracleDbTimestampGetter;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:magicnumber")
public class UpdateRedisTovarTreeServiceTest {

    private TovarTreeRedisReader tovarTreeRedisReader;
    private TovarTreeRedisWriter tovarTreeRedisWriter;
    private OracleDbTimestampGetter oracleDbTimestampGetter;
    private UpdateRedisTovarTreeService updateRedisTovarTreeService;


    @Before
    public void setUp() {
        updateRedisTovarTreeService = new UpdateRedisTovarTreeService(
            tovarTreeRedisWriter = mock(TovarTreeRedisWriter.class),
            tovarTreeRedisReader = mock(TovarTreeRedisReader.class),
            mock(TovarTreeService.class),
            oracleDbTimestampGetter = mock(OracleDbTimestampGetter.class));

    }

    @Test
    public void onUpdateShouldCallWriterNoRedisTsFound() {
        when(tovarTreeRedisReader.getTovarTreeTs())
            .thenReturn(Optional.empty());
        when(oracleDbTimestampGetter.getCurrentUtcMillis())
            .thenReturn(1L);
        updateRedisTovarTreeService.updateTovarTree(1L);
        verify(tovarTreeRedisWriter, times(1))
            .putTovarTree(anyLong(), any());
    }

    @Test
    public void onUpdateShouldCallWriterIfCalledTsIsGreaterThanRedisTs() {
        when(tovarTreeRedisReader.getTovarTreeTs())
            .thenReturn(Optional.of(4L));
        when(tovarTreeRedisWriter.putTovarTree(anyLong(), any()))
            .thenReturn(10L);
        when(oracleDbTimestampGetter.getCurrentUtcMillis())
            .thenReturn(1L);
        updateRedisTovarTreeService.updateTovarTree(5L);
        verify(tovarTreeRedisWriter, times(1))
            .putTovarTree(anyLong(), any());
    }

    @Test
    public void onUpdateShouldCallWriterIfCalledWithoutTs() {
        when(tovarTreeRedisReader.getTovarTreeTs())
            .thenReturn(Optional.of(4L));
        when(tovarTreeRedisWriter.putTovarTree(anyLong(), any()))
            .thenReturn(10L);
        when(oracleDbTimestampGetter.getCurrentUtcMillis())
            .thenReturn(1L);
        updateRedisTovarTreeService.updateTovarTree();
        verify(tovarTreeRedisWriter, times(1))
            .putTovarTree(anyLong(), any());
    }

    @Test
    public void onUpdateShouldNotCallIfRedisTsIsGreaterOrEquals() {
        long timestamp = 4L;
        when(tovarTreeRedisReader.getTovarTreeTs())
            .thenReturn(Optional.of(timestamp));
        when(oracleDbTimestampGetter.getCurrentUtcMillis())
            .thenReturn(1L);
        RedisUpdateResult handle = updateRedisTovarTreeService.updateTovarTree(timestamp);
        assertThat(handle).isEqualTo(RedisUpdateResult.SKIPPED);
        verify(tovarTreeRedisWriter, times(0))
            .putTovarTree(anyLong(), any());

        when(tovarTreeRedisReader.getTovarTreeTs())
            .thenReturn(Optional.of(timestamp + 1));
        handle = updateRedisTovarTreeService.updateTovarTree(timestamp);
        assertThat(handle).isEqualTo(RedisUpdateResult.SKIPPED);
        verify(tovarTreeRedisWriter, times(0))
            .putTovarTree(anyLong(), any());
    }

    @Test
    public void onRemoveShouldCallWriterNoRedisTsFound() {
        when(tovarTreeRedisReader.getTovarTreeTs())
            .thenReturn(Optional.empty());
        updateRedisTovarTreeService.removeTovarTree(1L);
        verify(tovarTreeRedisWriter, times(1))
            .removeTovarTree();
    }

    @Test
    public void onRemoveShouldCallWriterIfCalledTsIsGreaterThanRedisTs() {
        when(tovarTreeRedisReader.getTovarTreeTs())
            .thenReturn(Optional.of(4L));
        updateRedisTovarTreeService.removeTovarTree(5L);
        verify(tovarTreeRedisWriter, times(1))
            .removeTovarTree();
    }

    @Test
    public void onRemoveShouldNotCallIfRedisTsIsGreater() {
        long timestamp = 4L;
        when(tovarTreeRedisReader.getTovarTreeTs())
            .thenReturn(Optional.of(timestamp + 1));
        RedisUpdateResult handle = updateRedisTovarTreeService.removeTovarTree(timestamp);
        assertThat(handle).isEqualTo(RedisUpdateResult.SKIPPED);
        verify(tovarTreeRedisWriter, times(0))
            .removeTovarTree();
    }
}
