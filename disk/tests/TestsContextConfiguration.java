package ru.yandex.chemodan.app.smartcache.worker.tests;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.chemodan.app.dataapi.test.FullContextTestsContextConfiguration;
import ru.yandex.chemodan.app.smartcache.worker.SmartCacheContextConfiguration;
import ru.yandex.chemodan.app.smartcache.worker.clusterizer.ClusterizerManager;
import ru.yandex.chemodan.app.smartcache.worker.dataapi.cleanup.CleanupManager;
import ru.yandex.chemodan.mpfs.MpfsClient;
import ru.yandex.commune.bazinga.pg.storage.PgBazingaStorage;
import ru.yandex.commune.bazinga.test.BazingaTaskManagerStub;

/**
 * @author osidorkin
 */
@Import({
    SmartCacheContextConfiguration.class,
    FullContextTestsContextConfiguration.class,
})
@Configuration
public class TestsContextConfiguration {
    @Bean
    @Primary
    public BazingaTaskManagerStub bazingaTaskManagerStub() {
        return new BazingaTaskManagerStub();
    }

    @Bean
    @Primary
    public CleanupManager cleanupManager() {
        return Mockito.mock(CleanupManager.class);
    }

    @Bean
    public PgBazingaStorage bazingaStorageMock() {
        return Mockito.mock(PgBazingaStorage.class);
    }

    @Bean(destroyMethod = "shutdown")
    public MongoServer mongoServer() {
        MongoServer mongoServer = new MongoServer(new MemoryBackend());
        mongoServer.bind();
        return mongoServer;
    }

    @Bean(destroyMethod = "close")
    @Primary
    public MongoClient expireTrackingMongoClient(MongoServer mongoServer) {
        return new MongoClient(new ServerAddress(mongoServer.getLocalAddress()));
    }

    @Bean
    public MpfsClient mpfsClient() {
        return Mockito.mock(MpfsClient.class);
    }

    @Bean
    @Primary
    public ClusterizerManager clusterizerManager() {
        return Mockito.mock(ClusterizerManager.class);
    }
}
