package ru.yandex.market.api.integration;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;
import ru.yandex.market.api.server.ApplicationContextHolder;
import ru.yandex.market.api.server.Environment;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;

/**
 * Created by vdorogin on 18.05.17.
 */
@ActiveProfiles("test")
@ContextConfiguration(classes = TestAppConfig.class)
@WithContext
@RunWith(Parameterized.class)
public abstract class ParameterizedContainerTestBase extends UnitTestBase {
    protected TestContextManager testContextManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        this.testContextManager = new TestContextManager(getClass());
        this.testContextManager.prepareTestInstance(this);
    }

    @BeforeClass
    public static void setUpClass() {
        ApplicationContextHolder.setEnvironment(Environment.INTEGRATION_TEST);
    }
}
