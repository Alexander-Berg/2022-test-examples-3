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
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.crm.campaign.services.security.Roles;
import ru.yandex.market.crm.campaign.test.loggers.TestExecutedActionsLogger;
import ru.yandex.market.crm.core.test.utils.SecurityUtils;
import ru.yandex.market.crm.dao.UsersRolesDao;
import ru.yandex.market.crm.domain.Account;
import ru.yandex.market.crm.domain.CompositeUserRole;
import ru.yandex.market.crm.http.security.BlackboxProfile;
import ru.yandex.market.crm.util.logging.LogBuilder;
import ru.yandex.market.mcrm.db.test.AbstractDbTest;
import ru.yandex.market.mcrm.db.test.DbTestTool;
import ru.yandex.market.mcrm.http.HttpEnvironment;
import ru.yandex.market.mcrm.utils.test.StatefulHelper;

/**
 * @author apershukov
 */
@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@TestPropertySource("/mcrm_int_test.properties")
public abstract class AbstractControllerIntTest extends AbstractDbTest {

    private static final BlackboxProfile ADMIN_PROFILE = SecurityUtils.adminProfile();

    private static final Logger LOG = LoggerFactory.getLogger("TESTS");

    @Inject
    protected MockMvc mockMvc;
    @Inject
    protected HttpEnvironment httpEnvironment;
    @Inject
    private DbTestTool dbTestTool;
    @Inject
    private TestExecutedActionsLogger executedActionsLogger;
    @Inject
    private UsersRolesDao usersRolesDao;

    @Inject
    private ListableBeanFactory beanFactory;

    @BeforeEach
    public void commonSetUp(TestInfo info) {
        log("Started", info);
        beanFactory.getBeansOfType(StatefulHelper.class).values()
                .forEach(StatefulHelper::setUp);

        usersRolesDao.addRole(ADMIN_PROFILE.getUid(), new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.ADMIN));
        SecurityUtils.setAuthentication(ADMIN_PROFILE);
    }

    @AfterEach
    public void commonTearDown(TestInfo info) {
        dbTestTool.clearDatabase();
        httpEnvironment.tearDown();
        executedActionsLogger.reset();

        beanFactory.getBeansOfType(StatefulHelper.class).values()
                .forEach(StatefulHelper::tearDown);

        SecurityUtils.clearAuthentication();

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
