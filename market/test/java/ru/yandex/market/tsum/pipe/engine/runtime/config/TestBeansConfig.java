package ru.yandex.market.tsum.pipe.engine.runtime.config;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.grpc.Channel;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.test.context.TestPropertySource;
import ru.yandex.commune.bazinga.BazingaTaskManager;
import ru.yandex.market.experiments3.client.Experiments3Client;
import ru.yandex.market.monitoring.ComplicatedMonitoring;
import ru.yandex.market.tsum.clients.arcadia.RootArcadiaClient;
import ru.yandex.market.tsum.clients.bitbucket.BitbucketClient;
import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.clients.tankapi.TankApiClient;
import ru.yandex.market.tsum.core.mongo.MockMongoTransactions;
import ru.yandex.market.tsum.core.mongo.MongoTransactions;
import ru.yandex.market.tsum.core.notify.common.NotificationCenter;
import ru.yandex.market.tsum.event.TsumEventApiGrpc;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.runtime.JobContextFactory;
import ru.yandex.market.tsum.pipe.engine.runtime.LaunchUrlProvider;
import ru.yandex.market.tsum.pipe.engine.runtime.PipeProvider;
import ru.yandex.market.tsum.pipe.engine.runtime.di.ProjectContextProvider;
import ru.yandex.market.tsum.pipe.engine.runtime.di.ResourceInjector;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContextFactory;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestLaunchUrlProvider;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestResourceInjector;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.Notificator;
import ru.yandex.market.tsum.pipe.engine.source_code.SourceCodeService;
import ru.yandex.market.tsum.pipe.engine.source_code.impl.ReflectionsSourceCodeProvider;
import ru.yandex.market.tsum.pipe.engine.source_code.impl.SourceCodeServiceImpl;

import static org.mockito.Mockito.mock;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 10.03.17
 */
@Configuration
@ComponentScan(value = {
    "ru.yandex.market.tsum.pipe.engine.runtime.helpers"
})
@TestPropertySource({"classpath:test.properties"})
public class TestBeansConfig {
    @Autowired
    private ApplicationContext ctx;

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        MongoTemplate mongoTemplate = new MongoTemplate(mongoDbFactory(mongoClient));
        //removes _class
        ((MappingMongoConverter) mongoTemplate.getConverter()).setTypeMapper(new DefaultMongoTypeMapper(null));
        return mongoTemplate;
    }

    @Bean
    public MongoDbFactory mongoDbFactory(MongoClient mongoClient) {
        return new SimpleMongoDbFactory(mongoClient, "test");
    }

    @Bean(destroyMethod = "shutdown")
    public MongoServer mongoServer() {
        MongoServer mongoServer = new MongoServer(new MemoryBackend());
        mongoServer.bind();
        return mongoServer;
    }

    @Bean(destroyMethod = "close")
    public MongoClient mongoClient(MongoServer mongoServer) {
        return new MongoClient(new ServerAddress(mongoServer.getLocalAddress()));
    }

    @Bean
    public MongoConverter mongoConverter(MongoTemplate mongoTemplate) {
        return mongoTemplate.getConverter();
    }

    @Bean
    public MongoTransactions mongoTransactions(MongoTemplate mongoTemplate, ApplicationContext applicationContext) {
        return new MockMongoTransactions(mongoTemplate, applicationContext);
    }

    @Bean
    public NotificationCenter notificationCenter() {
        return mock(NotificationCenter.class);
    }

    @Bean
    public GitHubClient gitHubClient() {
        return mock(GitHubClient.class);
    }

    @Bean
    public BitbucketClient bitbucketClient() {
        return mock(BitbucketClient.class);
    }

    @Bean
    public RootArcadiaClient arcadiaClient() {
        return mock(RootArcadiaClient.class);
    }

    @Bean
    public TankApiClient tankApiClient() {
        return mock(TankApiClient.class);
    }

    @Bean
    public ComplicatedMonitoring complicatedMonitoring() {
        return mock(ComplicatedMonitoring.class);
    }

    @Bean
    public Notificator notificator() {
        return mock(Notificator.class);
    }

    @Bean
    public LaunchUrlProvider launchUrlProvider() {
        return new TestLaunchUrlProvider();
    }

    @Bean
    public JobContextFactory jobContextFactory() {
        return new TestJobContextFactory();
    }

    @Bean
    public PipeProvider pipeProvider() {
        return pipeId -> ctx.getBean(pipeId, Pipeline.class);
    }

    @Bean
    public TsumEventApiGrpc.TsumEventApiBlockingStub tsumEventApiBlockingApiClient() {
        return TsumEventApiGrpc.newBlockingStub(mock(Channel.class));
    }

    @Bean
    public ProjectContextProvider projectContextProvider() {
        return new ProjectContextProvider() {
            @Override
            public ApplicationContext get(String projectId) {
                return ctx;
            }

            @Override
            public ApplicationContext getRoot() {
                return ctx;
            }
        };
    }

    @Bean
    public ResourceInjector resourceInjector() {
        return new TestResourceInjector();
    }

    @Lazy
    @Bean
    public SourceCodeService sourceCodeEntityService() {
        return new SourceCodeServiceImpl(
            new ReflectionsSourceCodeProvider(ReflectionsSourceCodeProvider.SOURCE_CODE_PACKAGE)
        );
    }

    @Bean
    public BazingaTaskManager bazingaTaskManager() {
        return Mockito.mock(BazingaTaskManager.class);
    }

    @Bean
    public Experiments3Client experiments3Client() {
        return mock(Experiments3Client.class);
    }
}
