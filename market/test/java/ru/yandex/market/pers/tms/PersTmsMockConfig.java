package ru.yandex.market.pers.tms;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTreePlainTextBuilder;
import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.auth.Credentials;
import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.pers.grade.core.db.GradeMasterJdbc;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;
import ru.yandex.market.pers.tms.config.external.TmsExternalConfig;
import ru.yandex.market.pers.tms.config.internal.PersTmsCoreConfig;
import ru.yandex.market.pers.tms.saas.IndexGenerationIdFactory;
import ru.yandex.market.pers.tms.yt.dumper.dumper.IndexerMetricLogProxy;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.YtClientMocks;
import ru.yandex.market.pers.yt.YtClusterType;
import ru.yandex.market.pers.yt.tm.TransferManagerClient;
import ru.yandex.market.saas.indexer.ferryman.FerrymanService;
import ru.yandex.market.saas.indexer.ferryman.model.YtTableRef;
import ru.yandex.market.saas.search.SaasSearchService;
import ru.yandex.market.tms.quartz2.service.JobService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 26.11.2021
 */
@ComponentScan(basePackageClasses = {PersTmsCoreConfig.class, TmsExternalConfig.class})
@ComponentScan( // all @Services in project, but not configurations
    basePackageClasses = {Launcher.class},
    excludeFilters = @ComponentScan.Filter(Configuration.class)
)
@Configuration
public class PersTmsMockConfig {
    @Bean
    @Primary
    public YtClient mainYtClient() {
        return PersTestMocksHolder.registerMock(YtClient.class, ytClient ->
            YtClientMocks.baseMock(YtClusterType.HAHN, ytClient));
    }

    @Bean
    public YtClient arnoldYtClient() {
        return PersTestMocksHolder.registerMock(YtClient.class, ytClient ->
            YtClientMocks.baseMock(YtClusterType.ARNOLD, ytClient));
    }

    @Bean
    public TransferManagerClient transferManagerClient() {
        return PersTestMocksHolder.registerMock(TransferManagerClient.class);
    }

    @Bean
    public FerrymanService ferrymanServiceForStatic() {
        return buildFerrymanMock();
    }

    @Bean
    public FerrymanService ferrymanServiceForAuthorKv() {
        return buildFerrymanMock();
    }

    @Bean
    public FerrymanService ferrymanServiceForQaKv() {
        return buildFerrymanMock();
    }

    private FerrymanService buildFerrymanMock() {
        return PersTestMocksHolder.registerMock(FerrymanService.class, ferrymanService -> {
            try {
                when(ferrymanService.addTable(any(YtTableRef.class))).thenReturn("FERRYMAN_BATCH");
            } catch (Exception ignored) {
            }
        });
    }

    @Bean
    public JobService jobService() {
        return PersTestMocksHolder.registerMock(JobService.class);
    }


    @Bean
    public JdbcTemplate ytJdbcTemplate() {
        return PersTestMocksHolder.registerMock(JdbcTemplate.class);
    }

    @Bean
    public JdbcTemplate basketJdbcTemplate() {
        return PersTestMocksHolder.registerMock(JdbcTemplate.class);
    }

    @Bean
    public JdbcTemplate qaJdbcTemplate() {
        return PersTestMocksHolder.registerMock(JdbcTemplate.class);
    }

    @Bean
    public JdbcTemplate persHistoryJdbcTemplate() {
        return PersTestMocksHolder.registerMock(JdbcTemplate.class);
    }

    @Bean
    public TransactionTemplate basketTransactionTemplate() {
        return buildTransactionTemplateMock();
    }

    @Bean
    public TransactionTemplate persHistoryTransactionTemplate() {
        return buildTransactionTemplateMock();
    }

    private TransactionTemplate buildTransactionTemplateMock() {
        return PersTestMocksHolder.registerMock(TransactionTemplate.class, transactionTemplate -> {
            Mockito.doAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(new SimpleTransactionStatus());
            }).when(transactionTemplate).execute(any());
        });
    }

    @Bean
    public LogbrokerClientFactory lbkxClientFactory() {
        return PersTestMocksHolder.registerMock(LogbrokerClientFactory.class);
    }

    @Bean
    public LogbrokerClientFactory logbrokerClientFactory() {
        return PersTestMocksHolder.registerMock(LogbrokerClientFactory.class);
    }

    @Bean
    public Supplier<Credentials> credentialsSupplier() {
        return PersTestMocksHolder.registerMock(Supplier.class);
    }


    @Bean
    public IndexerMetricLogProxy indexerMetricLogProxy() {
        return PersTestMocksHolder.registerMock(IndexerMetricLogProxy.class);
    }

    @Bean
    public SaasSearchService saasSearchService() {
        return PersTestMocksHolder.registerMock(SaasSearchService.class);
    }

    @Bean
    public MdsS3Client mdsClient() {
        return PersTestMocksHolder.registerMock(MdsS3Client.class);
    }

    @Bean
    public IndexGenerationIdFactory indexGenerationIdFactory() {
        return PersTestMocksHolder.registerMock(IndexGenerationIdFactory.class, mock -> {
            when(mock.buildNewGenerationId(anyLong())).thenReturn(UUID.randomUUID().toString());
        });
    }

    @Bean
    public Supplier<List<String>> shopNames4Moderation() {
        return PersTestMocksHolder.registerMock(Supplier.class, result -> {
            when(result.get()).thenReturn(Collections.emptyList());
        });
    }

    @Bean
    public JdbcTemplate pgJdbcTemplateForYtDumperService(GradeMasterJdbc gradeMasterJdbc) {
        return gradeMasterJdbc.getPgJdbcTemplate();
    }

    @Bean
    public RegionService regionService() throws Exception {
        RegionTreePlainTextBuilder builder = new RegionTreePlainTextBuilder();
        builder.setTimeoutMillis(3000);
        builder.setPlainTextURL(getClass().getResource("/geoexport.txt"));
        builder.setSkipHeader(true);
        builder.setSkipUnRootRegions(true);

        RegionService regionService = new RegionService();
        regionService.setRegionTreeBuilders(List.of(builder));
        return regionService;
    }

    @Bean("monitoring")
    @Primary
    public ComplexMonitoring monitoring() {
        return new ComplexMonitoring();
    }

    @Bean("ping")
    public ComplexMonitoring ping() {
        return new ComplexMonitoring();
    }


}
