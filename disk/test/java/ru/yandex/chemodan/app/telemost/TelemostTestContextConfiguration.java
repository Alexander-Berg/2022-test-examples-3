package ru.yandex.chemodan.app.telemost;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.chemodan.app.telemost.calendar.CalendarClient;
import ru.yandex.chemodan.app.telemost.calendar.CalendarClientStub;
import ru.yandex.chemodan.app.telemost.chat.ChatClient;
import ru.yandex.chemodan.app.telemost.chat.ChatClientStub;
import ru.yandex.chemodan.app.telemost.common.TelemostConstants;
import ru.yandex.chemodan.app.telemost.mock.TelemostAuditLogMockConfiguration;
import ru.yandex.chemodan.app.telemost.mock.blackbox.MockBlackboxRequestExecutor;
import ru.yandex.chemodan.app.telemost.mock.mediator.RoomGrpcMockService;
import ru.yandex.chemodan.app.telemost.mock.properties.PropertyManagerStub;
import ru.yandex.chemodan.app.telemost.mock.uaas.MockExperimentsManager;
import ru.yandex.chemodan.app.telemost.orchestrator.TranslatorsOrchestrator;
import ru.yandex.chemodan.app.telemost.orchestrator.TranslatorsOrchestratorStub;
import ru.yandex.chemodan.app.telemost.repository.dao.BroadcastDao;
import ru.yandex.chemodan.app.telemost.repository.dao.StreamDao;
import ru.yandex.chemodan.app.telemost.repository.dao.UserDao;
import ru.yandex.chemodan.app.telemost.services.BroadcastService;
import ru.yandex.chemodan.app.telemost.services.BroadcastUriService;
import ru.yandex.chemodan.app.telemost.services.PropertyManager;
import ru.yandex.chemodan.app.telemost.services.StaffService;
import ru.yandex.chemodan.app.telemost.services.UserService;
import ru.yandex.chemodan.app.telemost.tools.ConferenceHelper;
import ru.yandex.chemodan.app.telemost.translator.TranslatorClient;
import ru.yandex.chemodan.app.telemost.ugcLive.UgcLiveClient;
import ru.yandex.chemodan.app.telemost.ugcLive.UgcLiveClientStub;
import ru.yandex.chemodan.app.telemost.ugcLive.UgcLiveStreamPublisher;
import ru.yandex.chemodan.app.uaas.client.UaasClient;
import ru.yandex.chemodan.app.uaas.experiments.ExperimentsManager;
import ru.yandex.chemodan.app.uaas.parser.UaasConditionParser;
import ru.yandex.chemodan.app.uaas.zk.UaasOverrideController;
import ru.yandex.chemodan.boot.value.OverridableValuePrefix;
import ru.yandex.chemodan.grpc.client.DeadlineInterceptor;
import ru.yandex.chemodan.grpc.client.LoggingGrpcInterceptor;
import ru.yandex.chemodan.util.jdbc.DataSourceProperties;
import ru.yandex.chemodan.util.test.EmbeddedDBDataSourceProperties;
import ru.yandex.chemodan.xiva.BasicXivaClient;
import ru.yandex.commune.a3.action.result.ApplicationInfo;
import ru.yandex.inside.passport.blackbox2.Blackbox2;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.misc.db.embedded.EmbeddedPostgres;
import ru.yandex.misc.db.embedded.PreparedDbProvider;
import ru.yandex.misc.version.AppName;
import ru.yandex.misc.version.SimpleAppName;

@Configuration
@Import({
        TelemostAuditLogMockConfiguration.class
})
public class TelemostTestContextConfiguration {

    @Autowired
    private Tvm2 tvm2;

    @Autowired
    private StaffService staffService;

    @Bean
    public AppName appName()
    {
        return new SimpleAppName("telemost", "telemost-backend");
    }

    @Bean
    public ApplicationInfo applicationInfo()
    {
        return ApplicationInfo.UNKNOWN;
    }

    @Bean
    @OverridableValuePrefix("telemost")
    public DataSourceProperties dataSourceProperties(EmbeddedPostgres embeddedPostgres)
    {
        return new EmbeddedDBDataSourceProperties(
                PreparedDbProvider.forPreparer("telemost_db", embeddedPostgres)
                        .createDatabase()
        );
    }

    @Bean
    public EmbeddedPostgres pg() throws IOException
    {
        return EmbeddedPostgres
                .builder()
                .setServerConfig("max_connections", "100")
                .start();
    }

    @Bean
    @Primary
    public BasicXivaClient basicXivaClient()
    {
        return Mockito.mock(BasicXivaClient.class);
    }

    @Bean
    @Primary
    public ChatClient chatClient() {
        return new ChatClientStub();
    }

    @Bean
    @Primary
    public CalendarClient calendarClient() {
        return new CalendarClientStub();
    }

    @Bean
    @Primary
    public PropertyManager propertyManager() {
        return new PropertyManagerStub();
    }

    @Bean
    public ConferenceHelper conferenceHelper()
    {
        return new ConferenceHelper();
    }

    @Bean
    @Primary
    public ManagedChannel mediatorChannel(RoomGrpcMockService roomImplBase, GrpcCleanupRule grpcCleanupRule) throws IOException
    {
        String serverName = InProcessServerBuilder.generateName();
        grpcCleanupRule.register(InProcessServerBuilder.forName(serverName).directExecutor()
                .addService(roomImplBase).build().start());
        return grpcCleanupRule.register(InProcessChannelBuilder.forName(serverName)
                .intercept(new LoggingGrpcInterceptor(TelemostConstants.GRPC_REQUEST_ID_PREFIX,
                        Metadata.Key.of(TelemostConstants.GRPC_MEDIATOR_REQUEST_TRACING_KEY, Metadata.ASCII_STRING_MARSHALLER), () -> Boolean.TRUE))
                .intercept(new DeadlineInterceptor(1000L)).directExecutor().build());
    }

    @Bean
    public RoomGrpcMockService roomImplBase()
    {
        return new RoomGrpcMockService();
    }

    @Bean
    public GrpcCleanupRule grpcCleanupRule()
    {
        return TelemostBaseContextTest.grpcCleanupRule;
    }

    @Bean
    @Primary
    public Blackbox2 prodBlackbox2()
    {
        return new Blackbox2(new MockBlackboxRequestExecutor(() -> TelemostBaseContextTest.usersData));
    }

    @Bean
    @Primary
    public ExperimentsManager experimentsManager(UaasConditionParser uaasConditionParser,
            UaasOverrideController uaasOverrideController, UaasClient uaasClient)
    {
        return new MockExperimentsManager(uaasConditionParser, uaasOverrideController, uaasClient);
    }

    @Bean
    @Primary
    public BroadcastService broadcastService(BroadcastUriService broadcastUriService,
                                             BroadcastDao broadcastDao, StreamDao streamDao)
    {
        return new BroadcastService(3, broadcastUriService, broadcastDao, streamDao);
    }

    @Bean
    @Primary
    public UgcLiveClient ugcLiveClient() {
        return new UgcLiveClientStub();
    }

    @Bean
    @Primary
    public UgcLiveStreamPublisher ugcLiveStreamPublisher() {
        return Mockito.mock(UgcLiveStreamPublisher.class);
    }

    @Bean
    @Primary
    public TranslatorsOrchestrator translatorsOrchestrator() {
        return new TranslatorsOrchestratorStub();
    }

    @Bean
    @Primary
    public TranslatorClient translatorClient() {
        return Mockito.mock(TranslatorClient.class);
    }

    @Bean
    @Primary
    public UserService userService(UserDao userDao)
    {
        return new UserService(1, userDao, staffService);
    }

    @PostConstruct
    public void startServices()
    {
        tvm2.start();
    }

    @PreDestroy
    public void stopServices()
    {
        tvm2.stop();
    }

}
