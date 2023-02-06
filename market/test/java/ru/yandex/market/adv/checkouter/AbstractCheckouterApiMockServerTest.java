package ru.yandex.market.adv.checkouter;

import org.mockserver.client.MockServerClient;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.adv.checkouter.config.CheckouterApiAutoconfiguration;
import ru.yandex.market.adv.config.CommonBeanAutoconfiguration;
import ru.yandex.market.adv.test.AbstractMockServerTest;

@SpringBootTest(classes = {
        CommonBeanAutoconfiguration.class,
        JacksonAutoConfiguration.class,
        CheckouterApiAutoconfiguration.class
})
@TestPropertySource(locations = {"/applications.properties"})
public class AbstractCheckouterApiMockServerTest extends AbstractMockServerTest {

    public AbstractCheckouterApiMockServerTest(MockServerClient server) {
        super(server);
    }
}
