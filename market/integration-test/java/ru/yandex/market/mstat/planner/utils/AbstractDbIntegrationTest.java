package ru.yandex.market.mstat.planner.utils;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.mstat.planner.config.ServicesAndDaoConfig;
import ru.yandex.market.mstat.planner.config.TestConfig;
import ru.yandex.market.starter.postgres.config.ZonkyEmbeddedPostgresConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ZonkyEmbeddedPostgresConfiguration.class, TestConfig.class, ServicesAndDaoConfig.class})
@TestPropertySource({"classpath:properties/10_application_test.properties"})
@ActiveProfiles("integration-tests")
public abstract class AbstractDbIntegrationTest {

    @Autowired
    protected IntegrationTestData data;

    @Before
    public void create() {
        data.destroyData();
        data.createData();
    }

}
