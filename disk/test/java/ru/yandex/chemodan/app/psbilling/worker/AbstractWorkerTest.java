package ru.yandex.chemodan.app.psbilling.worker;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener;

import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingDBTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingGroupsFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingProductsFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingUsersFactory;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceDao;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.worker.configuration.PsBillingWorkerTestConfig;

@SqlConfig(dataSource = "psBillingDataSource")
@TestExecutionListeners({AbstractPsBillingCoreTest.TruncateDbExecutionListener.class,
        SqlScriptsTestExecutionListener.class})
@ContextConfiguration(classes = {PsBillingWorkerTestConfig.class})
public abstract class AbstractWorkerTest extends AbstractPsBillingDBTest {
    @Autowired
    protected PsBillingGroupsFactory psBillingGroupsFactory;
    @Autowired
    protected PsBillingProductsFactory psBillingProductsFactory;
    @Autowired
    protected PsBillingUsersFactory psBillingUsersFactory;
    @Autowired
    protected GroupServiceDao groupServiceDao;
    @Autowired
    protected GroupDao groupDao;

    @Before
    public void init() {
        DateUtils.unfreezeTime();
        DateUtils.freezeTime();
    }
}
