package ru.yandex.direct.jobs.statistics.auctionstat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtTable;

import static java.time.format.DateTimeFormatter.ofPattern;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.ytwrapper.model.YtCluster.ARNOLD;
import static ru.yandex.direct.ytwrapper.model.YtCluster.HAHN;

class AuctionStatServiceTest {

    private final List<YtCluster> clusters = List.of(HAHN, ARNOLD);
    private final YtTable testTable = new YtTable("test_table");
    private final AuctionStatService auctionStatService = mock(AuctionStatService.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Test
    void getSuitableCluster_FirstClusterFresher() {
        var now = LocalDateTime.now();
        var firstClusterAttribute = now.minusMinutes(2).format(DATE_TIME_FORMATTER);
        var secondClusterAttribute = now.minusMinutes(3).format(DATE_TIME_FORMATTER);
        var previousAttribute = now.minusMinutes(10).format(DATE_TIME_FORMATTER);
        when(auctionStatService.tryGetClusterData(eq(HAHN), eq(testTable))).thenReturn(Optional.of(new AuctionStatService.ClusterData(HAHN, firstClusterAttribute)));
        when(auctionStatService.tryGetClusterData(eq(ARNOLD), eq(testTable))).thenReturn(Optional.of(new AuctionStatService.ClusterData(ARNOLD, secondClusterAttribute)));
        when(auctionStatService.getSuitableCluster(any(), any(), any())).thenCallRealMethod();
        var clusterDataGot = auctionStatService.getSuitableCluster(clusters, testTable, previousAttribute);

        var clusterDataExpected = new AuctionStatService.ClusterData(HAHN, firstClusterAttribute);
        assertThat(clusterDataGot).isEqualTo(clusterDataExpected);
    }

    @Test
    void getSuitableCluster_SecondClusterFresher() {
        var now = LocalDateTime.now();
        var firstClusterAttribute = now.minusMinutes(3).format(DATE_TIME_FORMATTER);
        var secondClusterAttribute = now.minusMinutes(2).format(DATE_TIME_FORMATTER);
        var previousAttribute = now.minusMinutes(10).format(DATE_TIME_FORMATTER);
        when(auctionStatService.tryGetClusterData(eq(HAHN), eq(testTable))).thenReturn(Optional.of(new AuctionStatService.ClusterData(HAHN, firstClusterAttribute)));
        when(auctionStatService.tryGetClusterData(eq(ARNOLD), eq(testTable))).thenReturn(Optional.of(new AuctionStatService.ClusterData(ARNOLD, secondClusterAttribute)));
        when(auctionStatService.getSuitableCluster(any(), any(), any())).thenCallRealMethod();
        var clusterDataGot = auctionStatService.getSuitableCluster(clusters, testTable, previousAttribute);

        var clusterDataExpected = new AuctionStatService.ClusterData(ARNOLD, secondClusterAttribute);
        assertThat(clusterDataGot).isEqualTo(clusterDataExpected);
    }

    @Test
    void getSuitableCluster_AllClustersOld() {
        var now = LocalDateTime.now();
        var attribute = now.minusHours(1).format(DATE_TIME_FORMATTER);
        when(auctionStatService.tryGetClusterData(eq(HAHN), eq(testTable))).thenReturn(Optional.of(new AuctionStatService.ClusterData(HAHN, attribute)));
        when(auctionStatService.tryGetClusterData(eq(ARNOLD), eq(testTable))).thenReturn(Optional.of(new AuctionStatService.ClusterData(ARNOLD, attribute)));
        when(auctionStatService.getSuitableCluster(any(), any(), any())).thenCallRealMethod();
        var clusterDataGot = auctionStatService.getSuitableCluster(clusters, testTable, attribute);

        assertThat(clusterDataGot).isNull();
    }

    @Test
    void getSuitableCluster_ClustersUnavailable() {
        var now = LocalDateTime.now();
        var attribute = now.minusHours(1).format(DATE_TIME_FORMATTER);
        when(auctionStatService.tryGetClusterData(any(), eq(testTable))).thenReturn(Optional.empty());
        when(auctionStatService.getSuitableCluster(any(), any(), any())).thenCallRealMethod();
        assertThatThrownBy(() -> auctionStatService.getSuitableCluster(clusters, testTable, attribute)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getSuitableCluster_ClusterTooOld() {
        var now = LocalDateTime.now();
        var tooOldAttribute = now.minusYears(18).format(DATE_TIME_FORMATTER);
        when(auctionStatService.tryGetClusterData(eq(HAHN), eq(testTable))).thenReturn(Optional.of(new AuctionStatService.ClusterData(HAHN, tooOldAttribute)));
        when(auctionStatService.tryGetClusterData(eq(ARNOLD), eq(testTable))).thenReturn(Optional.of(new AuctionStatService.ClusterData(ARNOLD, tooOldAttribute)));
        when(auctionStatService.getSuitableCluster(any(), any(), any())).thenCallRealMethod();
        assertThatThrownBy(() -> auctionStatService.getSuitableCluster(clusters, testTable, null)).isInstanceOf(IllegalStateException.class);
    }
}
