package ru.yandex.market.api.integration;

import javax.inject.Inject;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.api.server.ApplicationContextHolder;
import ru.yandex.market.api.server.Environment;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;

@ActiveProfiles("test")
@ContextConfiguration(classes = TestAppConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
@WithContext
public abstract class ContainerTestBase extends UnitTestBase {

    protected static final RestTemplate REST_TEMPLATE = new RestTemplate();

    @Inject
    @Qualifier("baseUrl")
    protected String baseUrl;

    @BeforeClass
    public static void setUpClass() {
        ApplicationContextHolder.setEnvironment(Environment.INTEGRATION_TEST);
    }
}

