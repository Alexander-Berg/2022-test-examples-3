package ru.yandex.market.reporting.resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.reporting.config.IntegrationTestConfig;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IntegrationTestConfig.class)
public class PingServiceITest {

    @Value("${service.url}")
    private String serviceUrl;

    private WebTarget target;

    @Before
    public void setUp() {
        target = ClientBuilder.newClient().target(serviceUrl);
    }

    @Test
    public void test() {
        String responseMsg = target.path("ping").request().get(String.class);

        assertThat(responseMsg, is("0;OK"));
    }
}
