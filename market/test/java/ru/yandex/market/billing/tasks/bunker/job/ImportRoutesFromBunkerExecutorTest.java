package ru.yandex.market.billing.tasks.bunker.job;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.bunker.loader.BunkerLoader;

import static org.mockito.Mockito.when;

/**
 * Тесты для джобы {@link ImportRoutesFromBunkerExecutor}.
 */
class ImportRoutesFromBunkerExecutorTest extends FunctionalTest {

    private static final String NODE_NAME = "/market-partner/routes";
    private static final String VERSION = "latest";

    @Autowired
    private BunkerLoader bunkerLoader;

    @Autowired
    private ImportRoutesFromBunkerExecutor routesFromBunkerExecutor;

    /**
     * Тест проверяет корректность работы механизма сохранения роутов в базу.
     * Механизм должен очистить таблицу, собрать новые данные и сохранить их в таблицу.
     */
    @Test
    @DbUnitDataSet(
            before = "save_routes_from_bunker_test.before.csv",
            after = "save_routes_from_bunker_test.after.csv"
    )
    void saveRoutesTest() throws Exception {
        when(bunkerLoader.getNodeStream(NODE_NAME, VERSION)).
                thenReturn(this.getClass().getResourceAsStream("test_data_from_bunker.js"));
        routesFromBunkerExecutor.doJob(Mockito.mock(JobExecutionContext.class));
    }
}
