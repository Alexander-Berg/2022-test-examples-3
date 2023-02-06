package ru.yandex.market.crm.campaign;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.crm.environment.Environment;
import ru.yandex.market.crm.environment.EnvironmentResolver;

/**
 * Created by vivg on 23.06.17.
 */
@Ignore
public class AppTest extends IntegrationTestBase {

    @Inject
    EnvironmentResolver environmentProvider;
    @Value("${environment}")
    private String environment;

    @Test
    public void testChangeEnvironment() {
        Assert.assertEquals(environment, environmentProvider.get().name().toLowerCase());
        environmentResolver.setEnvironment(Environment.PRODUCTION);
        Assert.assertEquals(Environment.PRODUCTION, environmentProvider.get());
    }
}
