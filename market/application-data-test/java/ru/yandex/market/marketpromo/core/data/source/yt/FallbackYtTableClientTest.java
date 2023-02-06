package ru.yandex.market.marketpromo.core.data.source.yt;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import ru.yandex.market.marketpromo.core.test.ServiceTestBase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.marketpromo.core.data.source.yt.YtTableClient.YtCluster.ARNOLD;
import static ru.yandex.market.marketpromo.core.data.source.yt.YtTableClient.YtCluster.HAHN;


public class FallbackYtTableClientTest extends ServiceTestBase {

    private final static String TABLE_PATH = "//home/table/path";

    @Mock
    public YtTableClient hahnClient;

    @Mock
    public YtTableClient arnoldClient;

    private FallbackYtTableClientProxy fallbackAwareClient;

    @BeforeEach
    public void setUp() {
        this.fallbackAwareClient = new FallbackYtTableClientProxy(Map.of(HAHN, hahnClient, ARNOLD, arnoldClient));
    }

    @Test
    public void shouldProxyToActualCluster() {
        when(hahnClient.readCreationTime(YtPath.of(HAHN, TABLE_PATH))).thenReturn(LocalDateTime.now());
        when(arnoldClient.readCreationTime(YtPath.of(ARNOLD, TABLE_PATH))).thenReturn(LocalDateTime.now()
                .minus(1,
                        ChronoUnit.DAYS));

        fallbackAwareClient.readTable(TABLE_PATH, entries -> {
        });

        verify(hahnClient, times(1))
                .readTable(any(YtPath.class), any(Consumer.class));
        verify(arnoldClient, never()).readTable(any(YtPath.class), any(Consumer.class));

        when(hahnClient.readCreationTime(YtPath.of(HAHN, TABLE_PATH))).thenReturn(LocalDateTime.now()
                .minus(3,
                        ChronoUnit.DAYS));

        fallbackAwareClient.readTable(TABLE_PATH, entries -> {
        });

        verify(hahnClient, times(1)).readTable(any(YtPath.class), any(Consumer.class));
        verify(arnoldClient, times(1)).readTable(any(YtPath.class), any(Consumer.class));
    }
}
