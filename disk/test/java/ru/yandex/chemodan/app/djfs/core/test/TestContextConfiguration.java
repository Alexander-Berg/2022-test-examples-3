package ru.yandex.chemodan.app.djfs.core.test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import com.mongodb.MongoClient;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.DjfsCoreContextConfiguration;
import ru.yandex.chemodan.app.djfs.core.EventManager;
import ru.yandex.chemodan.app.djfs.core.album.MockGeobase;
import ru.yandex.chemodan.app.djfs.core.client.DjfsInMemoryClientContextConfiguration;
import ru.yandex.chemodan.app.djfs.core.db.mongo.DjfsInMemoryMongoContextConfiguration;
import ru.yandex.chemodan.app.djfs.core.db.pg.DjfsEmbeddedPgContextConfiguration;
import ru.yandex.chemodan.app.djfs.core.db.pg.TransactionUtils;
import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsResourceDao;
import ru.yandex.chemodan.app.djfs.core.filesystem.Filesystem;
import ru.yandex.chemodan.app.djfs.core.filesystem.MongoSupportBlockedHidsDao;
import ru.yandex.chemodan.app.djfs.core.share.ShareInfoManager;
import ru.yandex.chemodan.app.djfs.core.share.ShareManager;
import ru.yandex.chemodan.app.djfs.core.tasks.DjfsTasksContextConfiguration;
import ru.yandex.chemodan.app.djfs.core.test.util.DjfsTestUtil;
import ru.yandex.chemodan.app.djfs.core.test.util.FilesystemTestUtil;
import ru.yandex.chemodan.app.djfs.core.test.util.ShareTestUtil;
import ru.yandex.chemodan.zk.configuration.ZkEmbedded;
import ru.yandex.commune.bazinga.pg.storage.PgBazingaStorage;
import ru.yandex.commune.bazinga.test.BazingaTaskManagerStub;
import ru.yandex.commune.zk2.ZkConfiguration;
import ru.yandex.commune.zk2.ZkPath;
import ru.yandex.commune.zk2.ZkWatcher;
import ru.yandex.commune.zk2.client.ZkManagerContextConfiguration;
import ru.yandex.commune.zk2.client.ZkWrapperBase;
import ru.yandex.misc.concurrent.CountDownLatches;
import ru.yandex.misc.net.HostnameUtils;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.thread.executor.SyncExecutor;

import static org.mockito.Mockito.spy;

/**
 * @author eoshch
 */
@Configuration
@Import({
        DjfsCoreContextConfiguration.class,
        DjfsEmbeddedPgContextConfiguration.class,
        DjfsInMemoryClientContextConfiguration.class,
        DjfsInMemoryMongoContextConfiguration.class,
        MockCeleryTaskManagerContextConfiguration.class,
        MockHistoryContextConfiguration.class,
        DjfsTasksContextConfiguration.class,
        ZkManagerContextConfiguration.class,
        // must be last
        RandomFailuresContextConfiguration.class,
})
public class TestContextConfiguration {
    @Bean
    @Primary
    public ExecutorService standardExecutorService() {
        return new SyncExecutor();
    }

    @Bean
    public FilesystemTestUtil filesystemTestUtil(DjfsResourceDao djfsResourceDao, ShareInfoManager shareInfoManager,
            Filesystem filesystem, TransactionUtils transactionUtils)
    {
        return new FilesystemTestUtil(djfsResourceDao, shareInfoManager, filesystem, transactionUtils);
    }

    @Bean
    public ShareTestUtil shareTestUtil(Filesystem filesystem, ShareManager shareManager)
    {
        return new ShareTestUtil(filesystem, shareManager);
    }

    @Bean
    public DjfsTestUtil djfsTestUtil(FilesystemTestUtil filesystemTestUtil, ShareTestUtil shareTestUtil) {
        return new DjfsTestUtil(filesystemTestUtil, shareTestUtil);
    }

    @Bean
    public EventInterceptor eventInterceptor() {
        return new EventInterceptor();
    }

    public static class EventInterceptor {
        public ListF<EventManager.Event> events = Cf.arrayList();

        @EventManager.EventHandler
        public void handle(EventManager.Event event) {
            events.add(event);
        }
    }

    @Bean
    public BazingaTaskManagerStub bazingaTaskManager() {
        return new BazingaTaskManagerStub();
    }

    @Bean
    @Primary
    public PgBazingaStorage pgBazingaStorage() {
        return Mockito.mock(PgBazingaStorage.class);
    }

    @Bean
    @Primary
    public MockGeobase geobase6() {
        return new MockGeobase();
    }

    @Bean
    public MongoSupportBlockedHidsDao mongoSupportBlockedHidsDao(@Qualifier("common") MongoClient mongoClient) {
        // поиск по коллекции через $in в Fongo для BsonBinary не работает, т.к. BsonBinary не имплементит Comparable
        return spy(new MongoSupportBlockedHidsDao(mongoClient));
    }

    @Bean
    public ZkPath zkRoot(ZkConfiguration configuration) {
        ZkPath root = new ZkPath("/djfs-"
                + HostnameUtils.localHostname()
                + "_" + Random2.R.nextAlnum(5));
        initRootPath(root, configuration);
        return root;
    }

    @Bean
    public ZkConfiguration zkConfiguration(ZkEmbedded zkEmbedded) {
        return zkEmbedded.getConfiguration();
    }

    @Bean
    public ZkEmbedded zkEmbedded() throws Exception {
        return new ZkEmbedded();
    }

    private void initRootPath(ZkPath path, ZkConfiguration configuration) {
        try {
            final CountDownLatch connectLatch = new CountDownLatch(1);
            ZkWatcher watcher = new ZkWatcher() {
                @Override
                protected String name() {
                    return "connector";
                }

                @Override
                protected void onConnected() {
                    connectLatch.countDown();
                }

                @Override
                protected void onSessionExpired() {
                    connectLatch.countDown();
                }
            };
            ZooKeeper zk = new ZooKeeper(configuration.getConnectionUrl(),
                    configuration.getTimeout(), watcher);
            CountDownLatches.await(connectLatch);
            new ZkWrapperBase(zk).create(path, Option.empty(), CreateMode.PERSISTENT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
