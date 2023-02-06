package ru.yandex.market.mbo;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
    locations = {"classpath:mbo-lite/test-config.xml"},
    initializers = MboLiteIntegrationTestInitializer.class
)
public abstract class MboLiteIntegrationTestBase {

}
