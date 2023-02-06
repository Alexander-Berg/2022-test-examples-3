package ru.yandex.market.deliverycalculator.indexer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.deliverycalculator.common.ScheduledTask;
import ru.yandex.market.deliverycalculator.indexer.config.FunctionalTestConfig;
import ru.yandex.market.deliverycalculator.workflow.test.CacheTestUtils;

/**
 * Базовый класс для написания функциональных тестов в модуле delivery calculator indexer.
 */
@DbUnitDataSet
@ContextConfiguration(
        loader = AnnotationConfigContextLoader.class,
        classes = FunctionalTestConfig.class
)
@ActiveProfiles({"functionalTest", "development"})
@ExtendWith(SpringExtension.class)
public abstract class FunctionalTest extends JupiterDbUnitTest {

    protected static final RestTemplate REST_TEMPLATE = new RestTemplate();

    @Autowired
    @Qualifier("baseUrl")
    protected String baseUrl;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void beforeEach() {
        CacheTestUtils.cleanRegionCache(applicationContext);
        CacheTestUtils.cleanTariffCaches(applicationContext);
        CacheTestUtils.cleanSolomonCache(applicationContext);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public final void runTasks(Class<? extends ScheduledTask>... scheduledTasks) {
        for (final Class<? extends ScheduledTask> scheduledTask : scheduledTasks) {
            ScheduledTask task = applicationContext.getBean(scheduledTask);
            task.run();
        }
    }

}
