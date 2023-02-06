package ru.yandex.market.crm.campaign.test;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author apershukov
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = ServiceMediumTestConfig.class)
public abstract class AbstractServiceMediumTest extends AbstractServiceTest {
}
