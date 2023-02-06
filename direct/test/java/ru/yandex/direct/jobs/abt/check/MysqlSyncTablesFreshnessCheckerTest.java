package ru.yandex.direct.jobs.abt.check;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.ytcomponents.repository.YtClusterFreshnessRepository;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MysqlSyncTablesFreshnessCheckerTest {

    @Test
    void oneBaseUpdateOnlyInCalculateDate() {
        var ytClusterFreshnessRepository = mock(YtClusterFreshnessRepository.class);
        var ytProvider = mock(YtProvider.class);
        var mysqlSyncTablesFreshnessChecker = new MysqlSyncTablesFreshnessChecker(ytClusterFreshnessRepository,
                ytProvider, "");
        Instant calculateDate = Instant.ofEpochSecond(1586267683L); // 2020-04-07 13:54:43
        when(ytClusterFreshnessRepository.loadPpcdictTimestamp(any(), anyString())).thenReturn(1586303999L);
        when(ytClusterFreshnessRepository.loadShardToTimestamp(any(), anyString())).thenReturn(Map.of(1, 1586304000L));
        var isFresh = mysqlSyncTablesFreshnessChecker.check(YtCluster.ZENO, calculateDate);
        assertThat(isFresh).isFalse();

    }

    @Test
    void allBasesUpdateInNextDate() {
        var ytClusterFreshnessRepository = mock(YtClusterFreshnessRepository.class);
        var ytProvider = mock(YtProvider.class);
        var mysqlSyncTablesFreshnessChecker = new MysqlSyncTablesFreshnessChecker(ytClusterFreshnessRepository,
                ytProvider, "");
        Instant calculateDate = Instant.ofEpochSecond(1586267683L); // 2020-04-07 13:54:43
        when(ytClusterFreshnessRepository.loadPpcdictTimestamp(any(), anyString())).thenReturn(1586304001L);
        when(ytClusterFreshnessRepository.loadShardToTimestamp(any(), anyString())).thenReturn(Map.of(1, 1586304000L));
        var isFresh = mysqlSyncTablesFreshnessChecker.check(YtCluster.ZENO, calculateDate);
        assertThat(isFresh).isTrue();
    }

    @Test
    void nullPpcdictFreshness() {
        var ytClusterFreshnessRepository = mock(YtClusterFreshnessRepository.class);
        var ytProvider = mock(YtProvider.class);
        var mysqlSyncTablesFreshnessChecker = new MysqlSyncTablesFreshnessChecker(ytClusterFreshnessRepository,
                ytProvider, "");
        Instant calculateDate = Instant.ofEpochSecond(1586267683L); // 2020-04-07 13:54:43
        when(ytClusterFreshnessRepository.loadPpcdictTimestamp(any(), anyString())).thenReturn(null);
        when(ytClusterFreshnessRepository.loadShardToTimestamp(any(), anyString())).thenReturn(Map.of(1, 1586304000L));
        var isFresh = mysqlSyncTablesFreshnessChecker.check(YtCluster.ZENO, calculateDate);
        assertThat(isFresh).isFalse();
    }

    @Test
    void nullPpcFreshness() {
        var ytClusterFreshnessRepository = mock(YtClusterFreshnessRepository.class);
        var ytProvider = mock(YtProvider.class);
        var mysqlSyncTablesFreshnessChecker = new MysqlSyncTablesFreshnessChecker(ytClusterFreshnessRepository,
                ytProvider, "");
        Instant calculateDate = Instant.ofEpochSecond(1586267683L); // 2020-04-07 13:54:43
        when(ytClusterFreshnessRepository.loadPpcdictTimestamp(any(), anyString())).thenReturn(1586304001L);
        when(ytClusterFreshnessRepository.loadShardToTimestamp(any(), anyString())).thenReturn(null);
        var isFresh = mysqlSyncTablesFreshnessChecker.check(YtCluster.ZENO, calculateDate);
        assertThat(isFresh).isFalse();
    }
}
