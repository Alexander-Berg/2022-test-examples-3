package ru.yandex.market.deepmind.app;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.security.core.context.SecurityContextHolder;
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
import ru.yandex.market.deepmind.common.utils.SecurityContextAuthenticationUtils;
import ru.yandex.market.yql_test.test_listener.YqlTestListener;

@RunWith(SpringRunner.class)
@TestExecutionListeners(value = {
    DependencyInjectionTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class,
    YqlTestListener.class,
})
@ContextConfiguration(
    initializers = JooqPGaaSZonkyInitializer.class,
    classes = {
        DeepmindDbTestConfiguration.class,
    }
)
@Transactional
@DbUnitTruncatePolicy(dataSource = "deepmindDataSource", truncateType = TruncateType.NOT_TRUNCATE)
public abstract class DeepmindBaseAppDbTestClass {
    public static final String DEEPMIND_APP_TEST_USER = "deepmind-app-test-user";

    @Before
    public void setUpUser() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            SecurityContextAuthenticationUtils.setAuthenticationToken(DEEPMIND_APP_TEST_USER);
        }
    }

    @After
    public void tearDownUser() {
        SecurityContextAuthenticationUtils.clearAuthenticationToken();
    }
}
