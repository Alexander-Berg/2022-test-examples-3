package ru.yandex.market.antifraud.orders.test.config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import ru.yandex.market.antifraud.orders.auth.AuthService;
import ru.yandex.market.antifraud.orders.config.AppConfig;
import ru.yandex.market.antifraud.orders.config.CheckouterClientConfiguration;
import ru.yandex.market.antifraud.orders.config.DatasourceConfiguration;
import ru.yandex.market.antifraud.orders.config.ExceptionHandlingControllerAdvice;
import ru.yandex.market.antifraud.orders.config.ExecutorConfiguration;
import ru.yandex.market.antifraud.orders.config.LiquibaseConfiguration;
import ru.yandex.market.antifraud.orders.config.QueueConfiguration;
import ru.yandex.market.antifraud.orders.config.SchedulerConfiguration;
import ru.yandex.market.antifraud.orders.config.TvmIdentity;
import ru.yandex.market.antifraud.orders.config.YtTablePaths;
import ru.yandex.market.antifraud.orders.external.crm.HttpLiluCrmClient;
import ru.yandex.market.antifraud.orders.external.volva.HttpVolvaClient;
import ru.yandex.market.antifraud.orders.storage.dao.IdmRoleDao;
import ru.yandex.market.sdk.userinfo.domain.Uid;
import ru.yandex.market.sdk.userinfo.service.ResolveUidService;
import ru.yandex.market.volva.logbroker.LogbrokerProducer;
import ru.yandex.passport.tvmauth.TvmClient;
import ru.yandex.yt.ytclient.proxy.SelectRowsRequest;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.tables.TableSchema;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.stream.Collectors.toMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author dzvyagin
 */
@Configuration
@Profile("integration-test")
@Import({
    EmbeddedPgConfig.class,
    LiquibaseConfiguration.class,
    ZooTestConfiguration.class,

    AppConfig.DetectorConfiguration.class,
    AppConfig.ServiceConfiguration.class,
    AppConfig.ControllerConfiguration.class,
    AppConfig.StorageConfiguration.class,

    QueueConfiguration.class,
    DatasourceConfiguration.class,
    ExceptionHandlingControllerAdvice.class,
    LiquibaseConfiguration.class,
    SchedulerConfiguration.class,
    CacheTestConfiguration.class,
    ExecutorConfiguration.class,

    CheckouterClientConfiguration.class
})
public class AppTestConfiguration {

    @Bean
    public ResolveUidService resolveUidService() {
        ResolveUidService result = mock(ResolveUidService.class);
        // resolveUidService.resolve(uid).getRestrictSideEffectsFlag();
        Uid uid = Uid.ofPassport(0);
        when(result.resolve(anyLong())).thenReturn(uid);
        return result;
    }

    @Bean
    public HttpLiluCrmClient httpLiluCrmClient() {
        return mock(HttpLiluCrmClient.class);
    }

    @Bean
    public HttpVolvaClient httpVolvaClient() {
        return mock(HttpVolvaClient.class);
    }

    @Bean
    public TvmClient tvmClient() {
        return mock(TvmClient.class);
    }

    @Bean
    public Map<Integer, TvmIdentity> clientTvmIds() {
        return Arrays.stream(TvmIdentity.values())
            .collect(toMap(v -> 24000 + v.ordinal(), Function.identity()));
    }

    @Bean
    public YtClient ytClient() {
        YtClient ytClient = mock(YtClient.class);
        when(ytClient.waitProxies())
            .thenReturn(CompletableFuture.supplyAsync(() -> null));
        when(ytClient.selectRows(anyString()))
            .thenReturn(CompletableFuture.supplyAsync(() -> new UnversionedRowset(mock(TableSchema.class),
                List.of())));
        when(ytClient.selectRows(any(SelectRowsRequest.class)))
            .thenReturn(CompletableFuture.supplyAsync(() -> new UnversionedRowset(mock(TableSchema.class),
                List.of())));
        when(ytClient.selectRows(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));
        return ytClient;
    }

    @Bean
    public YtTablePaths ytTablePaths() {
        return new YtTablePaths();
    }

    @Bean
    public IdmRoleDao idmRoleDao(NamedParameterJdbcOperations pgaasJdbcOperations) {
        return new IdmRoleDao(pgaasJdbcOperations);
    }

    @Bean
    public AuthService authService(IdmRoleDao idmRoleDao) {
        return mock(AuthService.class);
    }

    @Bean
    public LeaderSelector leaderSelector(){
        return mock(LeaderSelector.class);
    }

    @Bean
    public LogbrokerProducer logbrokerProducer() {
        return mock(LogbrokerProducer.class);
    }
}
