package ru.yandex.chemodan.app.dataapi.test;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import com.google.common.util.concurrent.MoreExecutors;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.dataapi.DataApiCoreContextConfiguration;
import ru.yandex.chemodan.app.dataapi.api.db.DatabaseAccessType;
import ru.yandex.chemodan.app.dataapi.api.db.ref.external.ExternalDatabasesRegistry;
import ru.yandex.chemodan.app.dataapi.apps.profile.address.AddressManager;
import ru.yandex.chemodan.app.dataapi.apps.profile.address.SetAddressLinesFromGeocoderTask;
import ru.yandex.chemodan.app.dataapi.core.dao.ShardPartitionDataSource;
import ru.yandex.chemodan.app.dataapi.core.dao.data.DatabasesJdbcDao;
import ru.yandex.chemodan.app.dataapi.core.dao.test.ImportDataApiEmbeddedPg;
import ru.yandex.chemodan.app.dataapi.core.dao.usermeta.UserMetaManager;
import ru.yandex.chemodan.app.dataapi.core.datasources.BasicDataSourceRegistry;
import ru.yandex.chemodan.app.dataapi.core.datasources.DataSourceTypeRegistry;
import ru.yandex.chemodan.app.dataapi.core.datasources.migration.DsMigrationDataSourceRegistry;
import ru.yandex.chemodan.app.dataapi.core.generic.GenericObjectManagerTest;
import ru.yandex.chemodan.app.dataapi.core.xiva.DataApiXivaPushSender;
import ru.yandex.chemodan.app.dataapi.core.xiva.XivaUrlHelper;
import ru.yandex.chemodan.app.dataapi.test.stubs.UserMetaManagerStub;
import ru.yandex.chemodan.boot.ChemodanInitContextConfiguration;
import ru.yandex.chemodan.boot.DiskAppVersion;
import ru.yandex.chemodan.util.jdbc.JdbcDatabaseConfigurator;
import ru.yandex.chemodan.ydb.dao.ThreadLocalYdbTransactionManager;
import ru.yandex.chemodan.zk.configuration.ZkEmbedded;
import ru.yandex.commune.a3.action.result.ApplicationInfo;
import ru.yandex.commune.admin.web.AdminApp;
import ru.yandex.commune.alive2.location.TestLocationResolverConfiguration;
import ru.yandex.commune.bazinga.test.BazingaTaskManagerStub;
import ru.yandex.commune.dynproperties.DynamicPropertiesContextConfiguration;
import ru.yandex.commune.dynproperties.DynamicProperty;
import ru.yandex.commune.dynproperties.DynamicPropertyBenderMapperHolder;
import ru.yandex.commune.zk2.ZkConfiguration;
import ru.yandex.commune.zk2.ZkPath;
import ru.yandex.commune.zk2.client.ZkManagerContextConfiguration;
import ru.yandex.inside.admin.conductor.ConductorContextConfiguration;
import ru.yandex.misc.bender.BenderMapper;
import ru.yandex.misc.env.EnvironmentType;
import ru.yandex.misc.net.HostnameUtils;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.reflection.ReflectionUtils;
import ru.yandex.misc.spring.ServicesStarter;
import ru.yandex.misc.spring.initializer.InitMethodInvokerConfiguration;
import ru.yandex.misc.version.AppName;
import ru.yandex.misc.version.SimpleAppName;
import ru.yandex.misc.version.Version;

/**
 * @author tolmalev
 */
@ImportDataApiEmbeddedPg
@Import({
        DataApiCoreContextConfiguration.class,
        ZkManagerContextConfiguration.class,
        ChemodanInitContextConfiguration.class,
        ConductorContextConfiguration.class,
        DynamicPropertiesContextConfiguration.class,
        InitMethodInvokerConfiguration.class,
        TestLocationResolverConfiguration.class,
})
@Configuration
public class FullContextTestsContextConfiguration {

    @Bean
    public ZkConfiguration zkConfiguration(ZkEmbedded zkEmbedded) throws Exception {
        return zkEmbedded.getConfiguration();
    }

    @Bean
    public ZkEmbedded zkEmbedded() throws Exception {
        return new ZkEmbedded();
    }

    @Bean
    public ZkPath zkRoot() {
        return new ZkPath("/" + TestConstants.DATAAPI + "/"
                + HostnameUtils.localHostname()
                + "_" + Random2.R.nextAlnum(5));
    }

    @Bean
    public Version version() {
        return DiskAppVersion.VERSION;
    }

    @Bean
    public EnvironmentType environmentType() {
        return EnvironmentType.TESTS;
    }

    @Bean
    public XivaUrlHelper xivaUrlHelper(@Value("${xiva.host}") String xivaUrl,
            @Value("%{xiva.ya-team.host}") String xivaYaTeamUrl)
    {
        return new XivaUrlHelper(xivaUrl, xivaYaTeamUrl);
    }

    @Bean
    @Primary
    public DsMigrationDataSourceRegistry migrationDataSourceRegistry(
            DatabasesJdbcDao databasesJdbcDao,
            UserMetaManager userMetaManager,
            DataSourceTypeRegistry dsTypeRegistry,
            BasicDataSourceRegistry dsRegistry)
    {
        DsMigrationDataSourceRegistry target = new DsMigrationDataSourceRegistry(databasesJdbcDao, userMetaManager, dsTypeRegistry, dsRegistry);
        return Mockito.spy(target);
    }

    @Bean
    @Primary
    public DataApiXivaPushSender xivaPushSender() {
        DataApiXivaPushSender mockSender = Mockito.mock(DataApiXivaPushSender.class);
        Field f = ReflectionUtils.getField(mockSender.getClass(), "collapsableDatabases");
        f.setAccessible(true);
        try {
            f.set(mockSender, DynamicProperty.cons("collapsable-database-keys", Cf.list()));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return mockSender;
    }

    @Bean
    public ExternalDatabasesRegistry externalDatabasesRegistry() {
        ExternalDatabasesRegistry mock = Mockito.mock(ExternalDatabasesRegistry.class);

        //TODO: allow add aliases in test. Implement in memory registry.

        Mockito
                .when(mock.getExternalDatabaseAccessType(Matchers.any()))
                .thenReturn(Option.empty());

        Mockito
                .when(mock.getExternalDatabaseAccessType(TestConstants.EXT_DB_ALIAS_RW))
                .thenReturn(Option.of(DatabaseAccessType.READ_WRITE));

        Mockito
                .when(mock.getExternalDatabaseAccessType(TestConstants.EXT_DB_ALIAS_RO))
                .thenReturn(Option.of(DatabaseAccessType.READ_ONLY));

        Mockito
                .when(mock.getExternalDatabaseAccessType(GenericObjectManagerTest.RO_DB_ALIAS))
                .thenReturn(Option.of(DatabaseAccessType.READ_ONLY));

        Mockito
                .when(mock.getExternalDatabaseAccessType(GenericObjectManagerTest.RW_DB_ALIAS))
                .thenReturn(Option.of(DatabaseAccessType.READ_WRITE));

        Mockito
                .when(mock.getExternalIdsByInternal(Matchers.any()))
                .thenReturn(Cf.arrayList());

        Mockito
                .when(mock.getAll())
                .thenReturn(Cf.list());

        return mock;
    }

    @Bean
    public BazingaTaskManagerStub bazingaTaskManager() {
        return new BazingaTaskManagerStub();
    }

    @Bean
    public SetAddressLinesFromGeocoderTask setAddressLinesFromGeocoderTask(AddressManager addressesManager) {
        return new SetAddressLinesFromGeocoderTask(addressesManager);
    }

    @Bean
    public DataCleaner dataApiShardPartitionDaoSupport(
            ShardPartitionDataSource dataSource,
            Optional<ThreadLocalYdbTransactionManager> ydbTransactionManager)
    {
        return new DataCleaner(dataSource, ydbTransactionManager);
    }

    @Bean
    public UserMetaManager userMetaManager(JdbcDatabaseConfigurator dataapiDbConfigurator) {
        return new UserMetaManagerStub(dataapiDbConfigurator.getShardIds());
    }

    @Bean
    public ExecutorService databaseManagerExecutorService() {
        return MoreExecutors.newDirectExecutorService();
    }

    @Bean
    public AdminApp adminApp() {
        return Mockito.mock(AdminApp.class);
    }

    @Bean
    public AppName appName() {
        return new SimpleAppName(TestConstants.DATAAPI, TestConstants.DATAAPI);
    }

    @Bean
    public ApplicationInfo applicationInfo() {
        return new ApplicationInfo(TestConstants.DATAAPI, TestConstants.DATAAPI);
    }

    @Bean
    public UserInitializer userInitializer(
            DataCleaner dataCleaner,
            UserMetaManager userMetaManager,
            @Value("${dataapi.shard.default-number}") int defaultShardNumber)
    {
        return new UserInitializer(dataCleaner, userMetaManager, defaultShardNumber);
    }

    @Bean
    public ServicesStarter servicesStarter() {
        return new ServicesStarter();
    }

    @Bean
    @Primary
    public DynamicPropertyBenderMapperHolder dynamicPropertyBenderMapperHolder() {
        return new DynamicPropertyBenderMapperHolder(new BenderMapper());
    }
}
