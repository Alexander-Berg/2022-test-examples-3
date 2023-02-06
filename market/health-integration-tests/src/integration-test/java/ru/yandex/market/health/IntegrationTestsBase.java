package ru.yandex.market.health;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.KeeperException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.clickhouse.ClickhouseTemplate;
import ru.yandex.market.clickphite.dictionary.Dictionary;

import java.util.concurrent.TimeUnit;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DockerConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class IntegrationTestsBase {

    @Value("${logshatter.zookeeper-quorum}")
    String zookeeperQuorum;

    @Value("${logshatter.zookeeper-prefix}")
    private String zookeeperPrefix;

    @Value("${database.name}")
    private String mainDatabase;

    void initLogshatterContextAndTables(AnnotationConfigApplicationContext context) throws Exception {
        removeLogshatterLeaderZkNode();
        context.register(LogshatterTestConfig.class);
        context.refresh();
        // Ждём окончания накатывания DDL
        context.getBean("configurationService", ru.yandex.market.logshatter.config.ConfigurationService.class).getDDLWorker().awaitStatus();
    }

    void initLogshatterContextAndTables(AnnotationConfigApplicationContext context,
                                        String configDir) throws Exception {
        final String logshatterConfPath = System.getProperty(LogshatterTestConfig.LOGSHATTER_CONF_PATH_PROP_NAME);
        try {
            System.setProperty(LogshatterTestConfig.LOGSHATTER_CONF_PATH_PROP_NAME, configDir);
            initLogshatterContextAndTables(context);
        } finally {
            System.setProperty(LogshatterTestConfig.LOGSHATTER_CONF_PATH_PROP_NAME, logshatterConfPath);
        }
    }

    void initLogshatterContextAndTablesMinConfig(AnnotationConfigApplicationContext context) throws Exception {
        initLogshatterContextAndTables(context, "src/integration-test/resources/min_configs/logshatter");
    }

    /**
     * Удаляет ноду с информацией о мастере логшаттера.
     * Нужно для того, чтобы в запускаемом тесте логшаттер наверняка стал мастером.
     * @throws Exception
     */
    void removeLogshatterLeaderZkNode() throws Exception {
        CuratorFramework curatorFramework = CuratorFrameworkFactory.builder()
            .connectString(zookeeperQuorum)
            .retryPolicy(new RetryNTimes(Integer.MAX_VALUE, (int) TimeUnit.SECONDS.toMillis(30)))
            .build();
        curatorFramework.start();
        try {
            curatorFramework.delete().deletingChildrenIfNeeded().forPath("/" + zookeeperPrefix);
        } catch (KeeperException.NoNodeException ignored) {
        } finally {
            curatorFramework.close();
        }
    }

    AnnotationConfigApplicationContext createClickphiteContext() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(ClickphiteTestConfig.class);
        context.refresh();
        return context;
    }

    void dropDatabases(ClickhouseTemplate clickhouseTemplate) {
        clickhouseTemplate.update("drop database if exists " + Dictionary.DATABASE);
        clickhouseTemplate.update("drop database if exists " + mainDatabase);
    }

}
