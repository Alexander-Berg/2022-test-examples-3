package ru.yandex.chemodan.app.dataapi.core.dao.test;

import org.joda.time.Duration;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.dataapi.api.db.ref.external.ExternalDatabasesRegistry;
import ru.yandex.chemodan.app.dataapi.api.user.DataApiUserId;
import ru.yandex.chemodan.app.dataapi.core.dao.JdbcShardContextConfiguration;
import ru.yandex.chemodan.app.dataapi.core.dao.usermeta.MetaUser;
import ru.yandex.chemodan.app.dataapi.core.dao.usermeta.UserMetaManager;
import ru.yandex.chemodan.app.dataapi.core.datasources.disk.DiskDataSource;
import ru.yandex.chemodan.app.dataapi.core.manager.DataApiManager;
import ru.yandex.chemodan.boot.ChemodanInitContextConfiguration;
import ru.yandex.chemodan.boot.DiskAppVersion;
import ru.yandex.chemodan.util.ReflectionUtils;
import ru.yandex.chemodan.util.jdbc.JdbcDatabaseConfiguratorContextConfiguration;
import ru.yandex.chemodan.util.sharpei.DynamicShardMetaNotifier;
import ru.yandex.commune.alive2.location.TestLocationResolverConfiguration;
import ru.yandex.commune.db.shard2.Shard2;
import ru.yandex.commune.db.shard2.ShardManager2;
import ru.yandex.commune.db.shard2.ShardMetaNotifier;
import ru.yandex.commune.dynproperties.DynamicPropertyManager;
import ru.yandex.inside.admin.conductor.Conductor;
import ru.yandex.inside.admin.conductor.NotFoundConductorException;
import ru.yandex.misc.env.EnvironmentType;
import ru.yandex.misc.thread.ThreadUtils;
import ru.yandex.misc.version.AppName;
import ru.yandex.misc.version.SimpleAppName;
import ru.yandex.misc.version.Version;

/**
 * @author tolmalev
 */
@Import({
        JdbcDatabaseConfiguratorContextConfiguration.class,
        JdbcShardContextConfiguration.class,
        ChemodanInitContextConfiguration.class,
        DataApiEmbeddedPgConfiguration.class,
        TestLocationResolverConfiguration.class,
})
@Configuration
@ImportDataApiEmbeddedPg
public class JdbcShardDaoTestsContextConfiguration {

    @Value("${dataapi.shard.default-number}")
    protected int defaultShardNumber;

    @Bean
    public Version version() {
        return DiskAppVersion.VERSION;
    }

    @Bean
    public EnvironmentType environmentType() {
        return EnvironmentType.TESTS;
    }

    @Bean
    public ExternalDatabasesRegistry externalDatabasesRegistry() {
        return Mockito.mock(ExternalDatabasesRegistry.class);
    }

    @Bean
    public DynamicPropertyManager dynamicPropertyManager() {
        return Mockito.mock(DynamicPropertyManager.class);
    }

    @Bean
    public Conductor conductor() {
        Conductor mock = Mockito.mock(Conductor.class);

        Mockito.when(mock.getGroupHosts("mail_pg_common_test")).thenReturn(
                Cf.list("pg-common-test01f.mail.yandex.net",
                        "pg-common-test01h.mail.yandex.net",
                        "pg-common-test01i.mail.yandex.net",
                        "pg-common-test02f.mail.yandex.net",
                        "pg-common-test02h.mail.yandex.net",
                        "pg-common-test02i.mail.yandex.net"));

        Mockito.when(mock.getDcNameIfExists("sysdb01e.dst.yandex.net")).thenReturn(Option.of("iva5"));
        Mockito.when(mock.getDcNameIfExists("sysdb01g.dst.yandex.net")).thenReturn(Option.of("fol6"));
        Mockito.when(mock.getDcNameIfExists("sysdb01h.dst.yandex.net")).thenReturn(Option.of("sas-1-3-3"));
        Mockito.when(mock.getDcNameIfExists("pg-common-test01f.mail.yandex.net")).thenReturn(Option.of("myt2"));
        Mockito.when(mock.getDcNameIfExists("pg-common-test01h.mail.yandex.net")).thenReturn(Option.of("sas-2-1-4"));
        Mockito.when(mock.getDcNameIfExists("pg-common-test01i.mail.yandex.net")).thenReturn(Option.of("man4"));
        Mockito.when(mock.getDcNameIfExists("pg-common-test02f.mail.yandex.net")).thenReturn(Option.of("myt2"));
        Mockito.when(mock.getDcNameIfExists("pg-common-test02h.mail.yandex.net")).thenReturn(Option.of("sas-2-1-4"));
        Mockito.when(mock.getDcNameIfExists("pg-common-test02i.mail.yandex.net")).thenReturn(Option.of("man4"));

        Mockito.when(mock.getDcNameIfExists(Mockito.anyString())).thenReturn(Option.empty());
        Mockito.when(mock.getDcNameByHost(Mockito.anyString())).thenThrow(new NotFoundConductorException(""));
        return mock;
    }

    @Bean
    public UserMetaManager userMetaManager() {
        UserMetaManager mock = Mockito.mock(UserMetaManager.class);

        Mockito
                .when(mock.findMetaUser(Matchers.any()))
                .then(invocation -> {
                    DataApiUserId uid = (DataApiUserId) invocation.getArguments()[0];
                    return Option.of(new MetaUser(uid, defaultShardNumber, false, Cf.set()));
                });

        Mockito
                .when(mock.findMetaUser(Matchers.any(), Matchers.anyBoolean()))
                .then(invocation -> {
                    DataApiUserId uid = (DataApiUserId) invocation.getArguments()[0];
                    return Option.of(new MetaUser(uid, defaultShardNumber, false, Cf.set()));
                });

        return mock;
    }

    @Bean
    public DataApiManager databaseManager() {
        return Mockito.mock(DataApiManager.class);
    }

    @Bean
    public DiskDataSource nativeDataApiManager() {
        return Mockito.mock(DiskDataSource.class);
    }

    @Bean
    public Shard2 defaultShard(ShardManager2 shardManager2) {
        //XXX: bad code :(
        ShardMetaNotifier shardMetaNotifier =
                ReflectionUtils.getFieldValue(shardManager2, ShardMetaNotifier.class, "shardMetaNotifier");
        if (shardMetaNotifier instanceof DynamicShardMetaNotifier) {
            while (!((DynamicShardMetaNotifier) shardMetaNotifier).isInitialized()) {
                ((DynamicShardMetaNotifier) shardMetaNotifier).start();
                ThreadUtils.sleep(Duration.millis(100));
            }
        }
        return shardManager2.shards().sortedBy(s -> s.getShardInfo().getId()).first();
    }

    @Bean
    public AppName appName() {
        return new SimpleAppName("test", "test");
    }
}
