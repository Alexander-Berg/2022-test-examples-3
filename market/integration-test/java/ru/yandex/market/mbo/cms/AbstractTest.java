package ru.yandex.market.mbo.cms;


import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.mbo.cms.config.CmsApiIntegrationTestConf;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ActiveProfiles(profiles = "testing")
@ContextConfiguration(classes = CmsApiIntegrationTestConf.class)
public abstract class AbstractTest {
}
