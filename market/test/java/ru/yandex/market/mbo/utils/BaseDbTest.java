package ru.yandex.market.mbo.utils;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.market.mbo.common.utils.PGaaSInitializer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
    initializers = PGaaSInitializer.class,
    classes = DbTestConfiguration.class
)
@Transactional
public abstract class BaseDbTest {
}
