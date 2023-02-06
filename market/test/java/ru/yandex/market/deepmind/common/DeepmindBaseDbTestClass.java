package ru.yandex.market.deepmind.common;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.common.test.db.DbUnitTruncatePolicy;
import ru.yandex.market.common.test.db.TruncateType;
import ru.yandex.market.deepmind.common.config.DeepmindDbTestConfiguration;
import ru.yandex.market.deepmind.common.config.JooqPGaaSZonkyInitializer;
import ru.yandex.market.yql_test.test_listener.YqlTestListener;

@RunWith(SpringRunner.class)
@ContextConfiguration(
    initializers = JooqPGaaSZonkyInitializer.class,
    classes = {
        DeepmindDbTestConfiguration.class,
    }
)
@TestExecutionListeners(value = {
    DependencyInjectionTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class,
    YqlTestListener.class,
})
@Transactional
@DbUnitTruncatePolicy(dataSource = "deepmindDataSource", truncateType = TruncateType.NOT_TRUNCATE)
public abstract class DeepmindBaseDbTestClass {
    public static final int BERU_ID = 465852;
    public static final int BERU_BUSINESS_ID = 924574;
}
