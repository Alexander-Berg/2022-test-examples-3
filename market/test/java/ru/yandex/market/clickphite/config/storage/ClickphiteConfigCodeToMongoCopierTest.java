package ru.yandex.market.clickphite.config.storage;

import java.util.function.Function;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.devtools.test.Paths;
import ru.yandex.market.clickphite.config.ConfigurationService;
import ru.yandex.market.clickphite.config.TestConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ClickphiteConfigCodeToMongoCopierTest {
    private static final String CONF_PATH = "market/infra/market-health/config-cs-clickphite/src/conf.d";

    @Autowired
    private Function<String, ConfigurationService> configurationServiceFactory;

    @Test
    public void fakeMongo() {
        configurationServiceFactory.apply(Paths.getSourcePath(CONF_PATH));
    }
}
