package ru.yandex.market.crm.triggers.test;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.crm.triggers.services.marketb2b.MarketB2BConfig;
import ru.yandex.market.crm.util.logging.LogBuilder;
import ru.yandex.market.mcrm.db.test.DbTestTool;
import ru.yandex.market.mcrm.utils.test.StatefulHelper;

/**
 * @author apershukov
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ServiceIntTestConfig.class })
@ActiveProfiles("test")
@TestPropertySource("/tp_test.properties")
public abstract class AbstractServiceTest {

    private static final Logger LOG = LoggerFactory.getLogger("TESTS");

    @Rule
    public TestName name = new TestName();

    @Inject
    private DbTestTool dbTestTool;

    @Inject
    private ListableBeanFactory beanFactory;

    @Before
    public void commonSetUp() {
        log("Started");

        beanFactory.getBeansOfType(StatefulHelper.class).values()
                .forEach(StatefulHelper::setUp);
    }

    @After
    public void commonTearDown() {
        dbTestTool.clearDatabase();

        beanFactory.getBeansOfType(StatefulHelper.class).values()
                .forEach(StatefulHelper::tearDown);

        log("Finished");
    }

    private void log(String message) {
        LOG.info(
                LogBuilder.builder("#tests")
                        .append("TEST", getClass().getSimpleName() + "." + name.getMethodName())
                        .append(message)
                        .build()
        );
    }
}
