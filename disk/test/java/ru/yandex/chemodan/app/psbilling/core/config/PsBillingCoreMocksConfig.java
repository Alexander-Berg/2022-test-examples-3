package ru.yandex.chemodan.app.psbilling.core.config;

import com.amazonaws.services.s3.AmazonS3;
import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.function.Function0;
import ru.yandex.chemodan.app.psbilling.core.PsBillingTransactionsFactory;
import ru.yandex.chemodan.app.psbilling.core.config.featureflags.FeatureFlags;
import ru.yandex.chemodan.app.psbilling.core.mocks.BalanceClientStub;
import ru.yandex.chemodan.app.psbilling.core.mocks.BazingaTaskManagerMock;
import ru.yandex.chemodan.app.psbilling.core.mocks.Blackbox2MockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.mocks.FeaturesSynchronizeRestTemplateMockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.mocks.MailSenderMockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.mocks.PurchaseReportingServiceMockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.mocks.TextsManagerMockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.mocks.TrustClientMockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.tasks.execution.TaskScheduler;
import ru.yandex.chemodan.app.psbilling.core.yt.YtSelectService;
import ru.yandex.chemodan.app.uaas.experiments.ExperimentsManager;
import ru.yandex.chemodan.directory.client.DirectoryClient;
import ru.yandex.chemodan.mpfs.MpfsClient;
import ru.yandex.commune.bazinga.BazingaTaskManager;
import ru.yandex.commune.bazinga.pg.storage.shard.JobsPartitionShardResolver;
import ru.yandex.inside.geobase.Geobase;

import static org.mockito.Mockito.CALLS_REAL_METHODS;

@Configuration
@Import({Blackbox2MockConfiguration.class,
        TextsManagerMockConfiguration.class,
        MailSenderMockConfiguration.class,
        TrustClientMockConfiguration.class,
        YtExportMockConfiguration.class,
        FeaturesSynchronizeRestTemplateMockConfiguration.class,
        PurchaseReportingServiceMockConfiguration.class
})
public class PsBillingCoreMocksConfig {
    private final MapF<Class<?>, Object> MOCKS = Cf.hashMap();
    @Autowired
    private Blackbox2MockConfiguration blackBoxMockConfiguration;
    @Autowired
    private TextsManagerMockConfiguration textsManagerMockConfiguration;
    @Autowired
    private MailSenderMockConfiguration mailSenderMockConfiguration;
    @Autowired
    private TrustClientMockConfiguration trustClientMockConfiguration;
    private BazingaTaskManagerMock bazingaTaskManagerMock;
    private BalanceClientStub balanceClientStub;
    @Autowired
    private PsBillingTransactionsFactory psBillingTransactionsFactory;

    public void resetMocks() {
        MOCKS.values().forEach(Mockito::reset);
        blackBoxMockConfiguration.reset();
        textsManagerMockConfiguration.reset();
        mailSenderMockConfiguration.reset();
        trustClientMockConfiguration.reset();
        bazingaTaskManagerMock.reset();
        balanceClientStub().reset();
        defaultSetup();
    }

    @Bean
    public BazingaTaskManagerMock bazingaTaskManager(ApplicationContext applicationContext) {
        return bazingaTaskManagerMock == null ? bazingaTaskManagerMock =
                new BazingaTaskManagerMock(applicationContext) : bazingaTaskManagerMock;
    }

    @Primary
    @Bean
    public YtSelectService ytSelectService() {
        return addMock(YtSelectService.class);
    }

    @Primary
    @Bean
    public DirectoryClient directoryClientMock() {
        return addMock(DirectoryClient.class);
    }

    @Bean
    @Primary
    public Geobase geobaseMock() {
        return addMock(Geobase.class);
    }

    @Bean
    @Primary
    public JobsPartitionShardResolver jobsPartitionShardResolver() {
        return addMock(JobsPartitionShardResolver.class);
    }

    @Bean
    @Primary
    public BalanceClientStub balanceClientStub() {
        return balanceClientStub == null ? balanceClientStub = new BalanceClientStub() : balanceClientStub;
    }

    @Primary
    @Bean
    public ExperimentsManager experimentsManagerMock() {
        return addMock(ExperimentsManager.class);
    }

    @Primary
    @Bean
    public TaskScheduler taskScheduler(ExperimentsManager experimentsManager, BazingaTaskManager bazingaTaskManager,
                                       FeatureFlags featureFlags) {
        return addMock(TaskScheduler.class,
                Mockito.withSettings()
                        .spiedInstance(new TaskScheduler(experimentsManager, bazingaTaskManager, featureFlags))
                        .defaultAnswer(CALLS_REAL_METHODS));
    }

    @Primary
    @Bean
    public MpfsClient mpfsClientMock() {
        return addMock(MpfsClient.class);
    }

    @Primary
    @Bean
    public Settings settings() {
        return addMock(Settings.class, Mockito.withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
    }

    @Primary
    @Bean
    public AmazonS3 mdsAmazonS3() {
        return addMock(AmazonS3.class);
    }

    @Primary
    @Bean(name = {"balanceRestTemplate"})
    public RestTemplate balanceRestTemplate() {
        return addMock(RestTemplate.class);
    }

    public <T> T addMock(Class<T> mockClass) {
        return addMock(mockClass, () -> Mockito.mock(mockClass, Mockito.withSettings()));
    }

    public <T> T addMock(Class<T> mockClass, MockSettings settings) {
        return addMock(mockClass, () -> Mockito.mock(mockClass, settings));
    }

    public <T> T addMock(Class<T> mockClass, Function0<T> mockCreator) {
        if (MOCKS.containsKeyTs(mockClass)) {
            return getMock(mockClass);
        }

        T mock = mockCreator.apply();
        MOCKS.put(mockClass, mock);
        return mock;
    }

    public <T> T getMock(Class<T> mockClass) {
        return mockClass.cast(MOCKS.getOrThrow(mockClass, "no such mock"));
    }

    private void defaultSetup() {
        Mockito.when((getMock(ExperimentsManager.class)).getFlags(Mockito.anyLong()))
                .thenReturn(Cf.list());
        TaskScheduler mock = getMock(TaskScheduler.class);
        Mockito.doNothing().when(mock).scheduleAbandonedCartEmailTask(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(mock).schedulePleaseComeBackTask(Mockito.any());
        Mockito.doNothing().when(mock).schedulePleaseComeBackTask(Mockito.any(), Mockito.any());

        textsManagerMockConfiguration.turnMockOn();
        psBillingTransactionsFactory.setupMocks();
    }
}
