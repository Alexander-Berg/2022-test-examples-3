package ru.yandex.market.ir.tms.logs;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.transactions.Transaction;
import ru.yandex.market.ir.tms.dao.LogsDao;
import ru.yandex.market.ir.tms.dao.MboLogsController;
import ru.yandex.market.ir.tms.dao.OracleLogDao;
import ru.yandex.market.ir.tms.dao.YtDiffDao;
import ru.yandex.market.ir.tms.utils.Log4jAwareSpringJUnit4ClassRunner;
import ru.yandex.market.mbo.common.ZooKeeper.ZooKeeperService;
import ru.yandex.market.mbo.core.dashboard.GenerationsDao;
import ru.yandex.market.mbo.gwt.models.dashboard.SuperControllerSession;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

/**
 * @author amaslak
 */
@RunWith(Log4jAwareSpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = LogsJobExecutorTest.Conf.class, initializers = LogsJobExecutorTest.Init.class)
@SuppressWarnings("checkstyle:magicnumber")
public class LogsJobExecutorTest {

    public static final String INDEXER_TYPE = "test.indexer";

    public static class Init implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();

            MockSettings settings = Mockito.withSettings();

            beanFactory.registerSingleton("generationsDao", Mockito.mock(GenerationsDao.class, settings));
            beanFactory.registerSingleton("oracleLogDao", Mockito.mock(OracleLogDao.class, settings));
            beanFactory.registerSingleton("mboLogsController", Mockito.mock(MboLogsController.class, settings));
            beanFactory.registerSingleton("logsDao", Mockito.mock(LogsDao.class, settings));
            beanFactory.registerSingleton("ytDiffDao", Mockito.mock(YtDiffDao.class, settings));
            beanFactory.registerSingleton("zooKeeperService", Mockito.mock(ZooKeeperService.class, settings));
        }

    }

    @Configuration
    public static class Conf {

        @Bean(autowire = Autowire.BY_NAME)
        public LogsJobExecutor executor() {
            LogsJobExecutor executor = new LogsJobExecutor();
            executor.setIndexerType(INDEXER_TYPE);
            executor.setDatabase(SuperControllerSession.Database.YT);
            executor.setCleanOraclePartitions(false);
            return executor;
        }

    }

    @Autowired
    private LogsDao logsDao;

    @Autowired
    private GenerationsDao generationsDao;

    @Autowired
    private MboLogsController mboLogsController;

    @Autowired
    private LogsJobExecutor executor;

    @Autowired
    private ZooKeeperService zooKeeperService;

    @Test
    public void testExecutor() throws Exception {
        Mockito.doReturn("mbo_offers_mr").when(logsDao).getOffersTableName();

        Mockito.doAnswer(i -> Optional.of(YPath.simple("//tmp/20121212_1212/" + i.getArgument(0))))
            .when(logsDao)
            .getGenerationTable(anyString());

        Mockito.doReturn("20121212_1212")
            .when(logsDao)
            .parseTableSessionId(any());

        Mockito.doReturn(ImmutableSortedMap.of("20121212_1212", 1))
            .when(logsDao)
            .getSessions(any());

        //noinspection unchecked
        Mockito.doAnswer(i -> {
            Consumer<Transaction> callback = i.getArgument(1);
            callback.accept(Mockito.mock(Transaction.class));
            return null;
        }).when(logsDao)
            .doInTransactionWithLock(anyString(), any(Consumer.class));

        Mockito.doReturn(ImmutableSet.of("20121212_1212"))
            .when(generationsDao)
            .createNewSessions(
                any(), anyString(), anyString(),
                any(), anyString(), Mockito.anyBoolean()
            );

        doAnswer(i -> {
            Runnable runnable = i.getArgument(3);
            runnable.run();
            return null;
        }).when(zooKeeperService).doWithLock(anyString(), anyLong(), any(), any(Runnable.class));

        AtomicBoolean answersOk = new AtomicBoolean(false);

        Answer<Boolean> checkMbologsArgs = i -> {
            Assert.assertEquals(SuperControllerSession.Database.YT, i.getArgument(0));
            Assert.assertEquals(INDEXER_TYPE, i.getArgument(1));
            Assert.assertEquals("20121212_1212/mbo_offers_mr", i.getArgument(2));
            Assert.assertEquals("//tmp/20121212_1212/mbo_offers_mr", i.getArgument(3).toString());
            Assert.assertEquals("mbo_offers_mr", i.getArgument(4));
            answersOk.set(true);
            return true;
        };
        Mockito.doAnswer(checkMbologsArgs)
            .when(mboLogsController)
            .runMbologs(
                any(), anyString(),
                anyString(), any(), anyString(),
                any(), Mockito.anySet(), any(),
                Mockito.anyBoolean()
            );

        executor.setCopyOfferParams(false);
        executor.doRealJob(null);

        Assert.assertTrue(answersOk.get());
    }
}
