package ru.yandex.market.gutgin.tms.base;

import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.gutgin.tms.config.TestConfig;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.partner.content.common.BaseDbCommonTest;

/**
 * @author s-ermakov
 */
@ContextConfiguration(initializers = PGaaSZonkyInitializer.class, classes = TestConfig.class)
public abstract class BaseDbGutGinTest extends BaseDbCommonTest {
}
