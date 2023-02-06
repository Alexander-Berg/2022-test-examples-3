package ru.yandex.market.crm.campaign.test;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.crm.campaign.test.loggers.TestExecutedActionsLogger;
import ru.yandex.market.crm.core.test.utils.SecurityUtils;
import ru.yandex.market.crm.http.security.BlackboxProfile;
import ru.yandex.market.crm.util.logging.LogBuilder;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.mcrm.db.test.AbstractDbTest;
import ru.yandex.market.mcrm.db.test.DbTestTool;
import ru.yandex.market.mcrm.utils.test.StatefulHelper;

/**
 * @author apershukov
 */
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@TestPropertySource("/mcrm_int_test.properties")
public abstract class AbstractServiceTest extends AbstractDbTest {

    private static final BlackboxProfile ADMIN_PROFILE = SecurityUtils.profile("admin");
    private static final Logger LOG = LoggerFactory.getLogger("TESTS");

    @Inject
    protected YtClient ytClient;

    @Inject
    private DbTestTool dbTestTool;

    @Inject
    private TestExecutedActionsLogger executedActionsLogger;

    @Inject
    private ListableBeanFactory beanFactory;

    @BeforeEach
    public void commonSetUp(TestInfo info) {
        log("Started", info);

        beanFactory.getBeansOfType(StatefulHelper.class).values()
                .forEach(StatefulHelper::setUp);

        SecurityUtils.setAuthentication(ADMIN_PROFILE);
    }

    @AfterEach
    public void commonTearDown(TestInfo info) {
        dbTestTool.clearDatabase();
        executedActionsLogger.reset();

        beanFactory.getBeansOfType(StatefulHelper.class).values()
                .forEach(StatefulHelper::tearDown);

        log("Finished", info);
    }

    private void log(String message, TestInfo info) {
        LOG.info(
                LogBuilder.builder("#tests")
                        .append("TEST", info.getDisplayName())
                        .append(message)
                        .build()
        );
    }
}
