package ru.yandex.market.mbo.cms.tms;


import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.mbo.cms.tms.conf.CmsTmsIntegrationTestConf;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ActiveProfiles(profiles = "testing")
@ContextConfiguration(classes = CmsTmsIntegrationTestConf.class)
public abstract class AbstractTest {
}
