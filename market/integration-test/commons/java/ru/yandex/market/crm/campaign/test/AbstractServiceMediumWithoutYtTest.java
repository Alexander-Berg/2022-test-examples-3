package ru.yandex.market.crm.campaign.test;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author zloddey
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = ServiceMediumWithoutYtTestConfig.class)
public abstract class AbstractServiceMediumWithoutYtTest extends AbstractServiceTest {
}
