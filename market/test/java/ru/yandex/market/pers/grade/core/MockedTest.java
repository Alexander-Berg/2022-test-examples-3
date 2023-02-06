package ru.yandex.market.pers.grade.core;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.pers.grade.core.config.GradeCoreConfig;
import ru.yandex.market.pers.grade.core.util.GradeCommonSettings;
import ru.yandex.market.pers.service.common.util.ExpFlagService;
import ru.yandex.market.pers.test.common.AbstractPersWebTest;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         26.12.16
 */
@Import({
    GradeCoreConfig.class,
    PersCoreMockConfig.class
})
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles({"junit"})
@TestPropertySource({"classpath:/test-application.properties"})
public abstract class MockedTest extends AbstractPersWebTest {
    static {
        GradeCommonSettings.initCommonGradeProperties();
    }

    @Autowired
    @Qualifier("gradePgDataSource")
    private DataSource pgDataSource;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    protected JdbcTemplate pgJdbcTemplate;

    @Autowired
    protected ExpFlagService expFlagService;

    @Before
    public synchronized void initMocks() {
        // reset mocks
        PersTestMocksHolder.resetMocks();

        // reset exp flags
        expFlagService.reset();
    }

    @Before
    public void cleanDatabase() {
        applySqlScript(pgDataSource, "truncate.sql");
    }
}
