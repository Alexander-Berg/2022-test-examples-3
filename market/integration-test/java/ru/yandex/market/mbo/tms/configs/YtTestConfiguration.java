package ru.yandex.market.mbo.tms.configs;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.mbo.appcontext.UnstableInit;
import ru.yandex.market.mbo.configs.yt.YtPoolConfig;
import ru.yandex.market.mbo.core.modelstorage.yt.mapreduce.YtModelsMapReduceService;
import ru.yandex.market.mbo.synchronizer.export.storage.ReplicationService;
import ru.yandex.market.mbo.tms.yt.modelstorage.backup.BackupYtModelsExecutor;
import ru.yandex.market.mbo.tms.yt.modelstorage.replication.ReplicateYtBackupsExecutor;
import ru.yandex.market.mbo.tms.yt.modelstorage.replication.ReplicateYtDumpsExecutor;
import ru.yandex.market.mbo.yt.transfermanager.TransferManagerClient;
import ru.yandex.market.mbo.yt.transfermanager.TransferManagerService;
import ru.yandex.market.yt.util.client.YtHttpClientFactory;

/**
 * @author galaev@yandex-team.ru
 * @since 06/12/2018.
 */
@Configuration
@Import({
    YtTestPropertiesConfiguration.class,
    YtPoolConfig.class
})
public class YtTestConfiguration {

    @Value("${mbo.yt.http.proxy}")
    private String ytHttpProxy;

    @Value("${mbo.yt.replication.http.proxy}")
    private String ytReplicationHttpProxy;

    @Value("${mbo.yt.http.mboRobot.token}")
    private String ytToken;

    @Value("${mbo.yt.tmp.path}")
    private String ytTmpPath;

    @Value("${mbo.yt.rootPath}")
    private String mboRootPath;

    @Value("${mbo.yt.tms.export.rootPath}")
    private String exportRootPath;

    @Value("${mbo.yt.recipe.rootPath}")
    private String recipeRootPath;

    @Value("${mbo.modelstorage.table.models}")
    private String modelsTablePath;

    @Value("${mbo.yt.cluster}")
    private String sourceCluster;

    @Value("${mbo.yt.replication.cluster}")
    private String destinationCluster;

    @Value("${transfer.manager.url}")
    private String transferManagerUrl;

    @Value("${transfer.manager.maxTaskCount:10}")
    private int maxTaskCount;

    @Value("${transfer.manager.waitTimeoutSeconds:10800}")
    private long waitTimeoutSeconds;

    @Value("${transfer.manager.retryTimeoutSeconds:20}")
    private long retryTimeoutSeconds;

    @Value("${transfer.manager.maxNotValidAttempts:5}")
    private int maxNotValidAttempts;

    @Resource
    private YtPoolConfig ytPoolConfig;

    @Bean
    public YtHttpClientFactory ytFactory() {
        return new YtHttpClientFactory(ytHttpProxy, ytToken, ytTmpPath);
    }

    @Bean
    public Yt ytApi(YtHttpClientFactory ytFactory) {
        return ytFactory.getInstance();
    }

    @Bean
    public YtHttpClientFactory ytReplicaFactory() {
        return new YtHttpClientFactory(ytReplicationHttpProxy, ytToken, ytTmpPath);
    }

    @Bean
    public Yt ytReplicaApi(YtHttpClientFactory ytReplicaFactory) {
        return ytReplicaFactory.getInstance();
    }

    @Bean
    public YtModelsMapReduceService ytModelsMapReduceService(Yt ytApi) {
        return new YtModelsMapReduceService(UnstableInit.simple(ytApi).waiting(), modelsTablePath);
    }

    @Bean
    public TransferManagerClient transferManagerClient() {
        return new TransferManagerClient(transferManagerUrl, ytToken, ytPoolConfig.getCommonPool());
    }

    @Bean
    public ReplicationService replicationService(Yt ytApi,
                                                 Yt ytReplicaApi) {
        return new ReplicationService(transferManagerService(),
            ytApi, ytReplicaApi, sourceCluster, destinationCluster, exportRootPath);
    }

    @Bean
    public TransferManagerService transferManagerService() {
        return new TransferManagerService(
            transferManagerClient(),
            maxTaskCount,
            TimeUnit.SECONDS.toMillis(waitTimeoutSeconds),
            TimeUnit.SECONDS.toMillis(retryTimeoutSeconds),
            maxNotValidAttempts
        );
    }

    @Bean
    public BackupYtModelsExecutor backupYtModelsExecutor(Yt ytApi,
                                                         YtModelsMapReduceService ytModelsMapReduceService) {
        return new BackupYtModelsExecutor(ytApi, ytModelsMapReduceService,
            ytPoolConfig.getBackupPool(), mboRootPath, modelsTablePath);
    }

    @Bean
    public ReplicateYtDumpsExecutor replicateYtDumpsExecutor(ReplicationService replicationService,
                                                             Yt ytApi,
                                                             Yt ytReplicaApi) {
        return new ReplicateYtDumpsExecutor(replicationService, ytApi, ytReplicaApi, exportRootPath);
    }

    @Bean
    public ReplicateYtDumpsExecutor replicateYtRecipesExecutor(ReplicationService replicationService,
                                                               Yt ytApi,
                                                               Yt ytReplicaApi) {
        return new ReplicateYtDumpsExecutor(replicationService, ytApi, ytReplicaApi, recipeRootPath);
    }

    @Bean
    public ReplicateYtBackupsExecutor replicateYtBackupsExecutor(TransferManagerService transferManager,
                                                                 Yt ytApi,
                                                                 Yt ytReplicaApi) {
        return new ReplicateYtBackupsExecutor(transferManager, ytApi, ytReplicaApi,
            sourceCluster, destinationCluster,
            mboRootPath);
    }
}
