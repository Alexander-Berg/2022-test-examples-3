package ru.yandex.chemodan.app.psbilling.core;

import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import ru.yandex.chemodan.app.psbilling.core.config.PsBillingCoreMocksConfig;
import ru.yandex.chemodan.app.psbilling.core.config.featureflags.FeatureFlags;
import ru.yandex.chemodan.app.psbilling.core.mocks.BazingaTaskManagerMock;
import ru.yandex.chemodan.app.psbilling.core.users.UserInfoService;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;

import static ru.yandex.misc.db.embedded.ActivateEmbeddedPg.EMBEDDED_PG;

@ContextConfiguration(classes = {PsBillingCoreTestConfig.class})
@ActiveProfiles(EMBEDDED_PG)
public abstract class AbstractPsBillingDBTest extends AbstractJUnit4SpringContextTests {
    @Autowired
    protected BazingaTaskManagerMock bazingaTaskManagerStub;
    @Autowired
    protected UserInfoService userInfoService;
    @Autowired
    private PsBillingCoreMocksConfig mockConfig;
    @Autowired
    private FeatureFlags featureFlags;

    @Before
    public void before() {
        DateUtils.unfreezeTime();
        DateTimeZone.setDefault(DateTimeZone.forOffsetHours(3));
        DateUtils.freezeTime();

        mockConfig.resetMocks();
        userInfoService.addRegionCode("225", "ru");
        featureFlags.getNewGroupSyncEnabled().setValue("true");

    }

    @After
    public void after() {
        DateUtils.unfreezeTime();
    }
}
