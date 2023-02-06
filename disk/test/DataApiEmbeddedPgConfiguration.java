package ru.yandex.chemodan.app.dataapi.core.dao.test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.boot.value.OverridableValuePrefix;
import ru.yandex.chemodan.util.jdbc.DataSourceProperties;
import ru.yandex.chemodan.util.jdbc.JdbcDatabaseConfigurator;
import ru.yandex.chemodan.util.jdbc.JdbcDatabaseConfiguratorContextConfiguration;
import ru.yandex.chemodan.util.sharpei.SharpeiClient;
import ru.yandex.chemodan.util.sharpei.SharpeiDatabaseInfo;
import ru.yandex.chemodan.util.sharpei.SharpeiShardInfo;
import ru.yandex.chemodan.util.sharpei.SharpeiUserInfo;
import ru.yandex.chemodan.util.sharpei.UserId;
import ru.yandex.chemodan.util.test.EmbeddedDBDataSourceProperties;
import ru.yandex.misc.db.embedded.EmbeddedPostgres;
import ru.yandex.misc.db.embedded.ImportEmbeddedPg;
import ru.yandex.misc.db.embedded.PreparedDbProvider;

import static org.mockito.Matchers.any;
import static ru.yandex.chemodan.app.dataapi.core.dao.test.ActivateDataApiEmbeddedPg.DATAAPI_EMBEDDED_PG;

/**
 * @author vpronto
 */
@Configuration
@Import(JdbcDatabaseConfiguratorContextConfiguration.class)
@ImportEmbeddedPg
@Profile(DATAAPI_EMBEDDED_PG)
public class DataApiEmbeddedPgConfiguration {

    @Autowired
    private JdbcDatabaseConfiguratorContextConfiguration dbConfiguratorConfig;

    @Bean
    public PreparedDbProvider.DbInfo dbInfoShard1(EmbeddedPostgres embeddedPostgres) {
        return getProvider(embeddedPostgres).createDatabase();
    }

    @Bean
    public PreparedDbProvider.DbInfo dbInfoShard2(EmbeddedPostgres embeddedPostgres) {
        return getProvider(embeddedPostgres).createDatabase();
    }

    private PreparedDbProvider getProvider(EmbeddedPostgres embeddedPostgres) {
        return PreparedDbProvider.forPreparer("dataapidb", embeddedPostgres);
    }

    @Bean
    @OverridableValuePrefix("dataapi")
    public DataSourceProperties dataapiDataSourceProperties(List<PreparedDbProvider.DbInfo> dbInfo) {
        return new EmbeddedDBDataSourceProperties(filter(dbInfo).get(0));
    }

    private List<PreparedDbProvider.DbInfo> filter(List<PreparedDbProvider.DbInfo> dbInfos) {
        return dbInfos.stream().filter(d -> d.getDbName().contains("dataapi")).collect(Collectors.toList());
    }

    @Bean
    @Primary
    public SharpeiClient configureSharpeiClient(List<PreparedDbProvider.DbInfo> allDbInfos) {
        SharpeiClient client = Mockito.mock(SharpeiClient.class);
        List<PreparedDbProvider.DbInfo> dbInfos = filter(allDbInfos);
        List<SharpeiShardInfo> shardInfos = new ArrayList<>();
        for (int i = 0; i < dbInfos.size(); i++) {
            PreparedDbProvider.DbInfo dbInfo = dbInfos.get(i);
            shardInfos.add(getShardInfo(dbInfo, dbInfo.getPort(), i));

        }
        Mockito.when(client.getShards()).thenReturn(Cf.wrap(shardInfos));
        Mockito.when(client.findUser(any(UserId.class)))
                .thenReturn(Option.of(new SharpeiUserInfo(shardInfos.get(0), Option.empty())));
        return client;
    }

    /**
     * enable it for testing flapping ds
     * @param allDbInfos
     * @return
     */
    public SharpeiClient configureFlappingMaster(List<PreparedDbProvider.DbInfo> allDbInfos) {
        SharpeiClient client = Mockito.mock(SharpeiClient.class);
        List<PreparedDbProvider.DbInfo> dbInfos = filter(allDbInfos);
        PreparedDbProvider.DbInfo dbInfo = dbInfos.get(0);
        List<SharpeiShardInfo> shardInfosWrong = new ArrayList<>();
        shardInfosWrong.add(getShardInfo(dbInfo, 12345, 0));
        List<SharpeiShardInfo> shardInfosCorrect = new ArrayList<>();
        shardInfosCorrect.add(getShardInfo(dbInfo, dbInfo.getPort(), 0));

        Mockito.when(client.getShards())
                .thenReturn(Cf.wrap(shardInfosWrong))
                .thenReturn(Cf.wrap(shardInfosWrong))
                .thenReturn(Cf.wrap(shardInfosCorrect).subList(0,1));

        Mockito.when(client.findUser(any(UserId.class)))
                .thenReturn(Option.of(new SharpeiUserInfo(shardInfosCorrect.get(0), Option.empty())));
        return client;
    }

    private SharpeiShardInfo getShardInfo(PreparedDbProvider.DbInfo dbInfo, int port, int shard) {
        return new SharpeiShardInfo(
                shard,
                "mock-shard-" + shard,
                Cf.arrayList(new SharpeiDatabaseInfo(
                                SharpeiDatabaseInfo.Role.MASTER,
                                SharpeiDatabaseInfo.Status.ALIVE,
                                new SharpeiDatabaseInfo.State(0),
                                new SharpeiDatabaseInfo.Address(dbInfo.getHost(), port, dbInfo.getDbName(), "mock-master")), //wrong port
                        new SharpeiDatabaseInfo(
                                SharpeiDatabaseInfo.Role.REPLICA,
                                SharpeiDatabaseInfo.Status.ALIVE,
                                new SharpeiDatabaseInfo.State(0),
                                new SharpeiDatabaseInfo.Address(dbInfo.getHost(), dbInfo.getPort(), dbInfo.getDbName(), "mock-slave"))));
    }

    @Bean
    public JdbcDatabaseConfigurator dataapiDbConfigurator(
            DataSourceProperties dataapiDataSourceProperties,
            SharpeiClient sharpeiClient)
    {
        return dbConfiguratorConfig.consSharpeiJdbcConfigurator(dataapiDataSourceProperties, sharpeiClient);
    }
}
