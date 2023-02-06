package ru.yandex.market.aliasmaker.meta;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.aliasmaker.meta.IntegrationTests.IntegrationTestConfig;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;

/**
 * @author apluhin
 * @created 4/22/22
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IntegrationTestConfig.class,
        initializers = PGaaSZonkyInitializer.class)
@Transactional
public abstract class BaseTest {
}
