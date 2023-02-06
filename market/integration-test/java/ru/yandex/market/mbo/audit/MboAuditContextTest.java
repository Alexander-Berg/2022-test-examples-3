package ru.yandex.market.mbo.audit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.mbo.audit.config.AuditIntegrationTestConfig;
import ru.yandex.market.mbo.common.utils.PGaaSInitializer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AuditIntegrationTestConfig.class, initializers = PGaaSInitializer.class)
public class MboAuditContextTest {
    @Test
    public void contextLoads() {
        // function body intentionally left blank
    }
}
