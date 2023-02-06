package ru.yandex.market.core.yt.dynamic.samovar;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.protobuf.Timestamp;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.inside.yt.kosher.impl.YtUtils;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.environment.UnitedCatalogEnvironmentService;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.core.yt.YtRpcClientFactory;
import ru.yandex.market.core.yt.dynamic.ReplicaCluster;
import ru.yandex.market.core.yt.dynamic.samovar.feed.SamovarFeed;
import ru.yandex.market.core.yt.dynamic.samovar.feed.SamovarFeedMapper;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.yt.YtUtil;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;
import ru.yandex.yt.ytclient.bus.DefaultBusConnector;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.proxy.YtCluster;
import ru.yandex.yt.ytclient.rpc.RpcCredentials;

/**
 * интеграциионый тест для девелоперской отладки чтения из YT
 * для работы необходимо указать свой логин и oauth токен.
 * токен можно получить здесь - https://oauth.yt.yandex.net/
 */
@Disabled("проверка интеграции с YT - чтения и записи. dev отладка")
class SamovarFeedImporterServiceIntegrationTest {

    private final String user = "robot-market-mbi-ts";
    private final String token = "...";
    private SamovarFeedImporterService samovarFeedImporterService;

    @Autowired
    private UnitedCatalogEnvironmentService unitedCatalogEnvironmentService;

    @BeforeEach
    void setup() {
        DefaultBusConnector connector = new DefaultBusConnector(new NioEventLoopGroup(), true);
        YtRpcClientFactory factory = new YtRpcClientFactory(connector, new RpcCredentials(user, token));
        YtCluster ytCluster = new YtCluster("markov");
        YtClient ytClient = factory.build(Collections.singletonList(ytCluster));

        ru.yandex.market.mbi.yt.YtCluster markov = new ru.yandex.market.mbi.yt.YtCluster("markov",
                YtUtils.http("markov" + YtUtil.YT_YANDEX_NET, token));
        ru.yandex.market.mbi.yt.YtCluster hahn = new ru.yandex.market.mbi.yt.YtCluster("hahn",
                YtUtils.http("hahn" + YtUtil.YT_YANDEX_NET, token));
        ru.yandex.market.mbi.yt.YtCluster arnold = new ru.yandex.market.mbi.yt.YtCluster("arnold",
                YtUtils.http("arnold" + YtUtil.YT_YANDEX_NET, token));

        List<ReplicaCluster> replicaClusters = Arrays.asList(
                new ReplicaCluster("hahn", hahn),
                new ReplicaCluster("arnold", arnold));
        String tablePath = "//home/market/testing/mbi/samovar/blue";
        EnvironmentService environmentService = Mockito.mock(EnvironmentService.class);
        Mockito.when(environmentService.getIntValue(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt()))
                .thenReturn(10000);
        SamovarFeedImporterServiceSettings settings = new SamovarFeedImporterServiceSettings(environmentService, "changed.url");

        samovarFeedImporterService = new SamovarFeedImporterService(
                null, new SamovarFeedMapper(environmentService), settings,
                unitedCatalogEnvironmentService, tablePath, replicaClusters, markov, ytClient);
    }

    @Test
    @DisplayName("Чтение SamovarFeed из YT")
    void readSamovarFeedFromYt() {
        final List<SamovarFeed> samovarFeeds = samovarFeedImporterService.readSamovarFeedsFromYt();
        System.out.println(samovarFeeds);
    }

    @Test
    @DisplayName("Запись SamovarFeed в YT")
    void writeSamovarFeedToYt() {
        final String tableName = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")
                .withZone(DateTimes.MOSCOW_TIME_ZONE)
                .format(Instant.now());
        List<SamovarFeed> samovarFeeds = List.of(createSamovarFeed("http://feed.ru/", List.of(1L)));
        samovarFeedImporterService.writeSamovarFeedsToYt(tableName, samovarFeeds);
    }

    public SamovarFeed createSamovarFeed(String url, Collection<Long> feedIds) {
        List<SamovarContextOuterClass.FeedInfo> feedInfos = feedIds.stream()
                .map(id -> createFeedInfo(url, id))
                .collect(Collectors.toList());

        return SamovarFeed.builder()
                .setUrl(url)
                .setPeriodMinutes(20)
                .setTimeoutSeconds(100)
                .setContext(feedInfos, EnvironmentType.TESTING)
                .build();
    }

    private SamovarContextOuterClass.FeedInfo createFeedInfo(String url, Long feedId) {
        Timestamp updatedAt = Timestamp.newBuilder().setSeconds(1575948458).setNanos(172803000).build();
        return SamovarContextOuterClass.FeedInfo.newBuilder()
                .setUrl(url)
                .setCampaignType(CampaignType.SUPPLIER.name())
                .setFeedId(feedId)
                .setUpdatedAt(updatedAt)
                .setShopId(113L)
                .addWarehouses(SamovarContextOuterClass.FeedInfo.WarehouseInfo.newBuilder()
                        .setWarehouseId(1000)
                        .setFeedId(feedId)
                        .build())
                .build();
    }
}
