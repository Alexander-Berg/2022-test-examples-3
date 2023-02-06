package ru.yandex.market.routing.config;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.routing.external.request.RoutingRequestReceiver;
import ru.yandex.market.tpl.common.web.monitoring.juggler.JugglerClient;
import ru.yandex.market.tpl.core.external.routing.vrp.client.VrpClient;

@MockBean({
        VrpClient.class,
        JugglerClient.class,
        RoutingRequestReceiver.class
})
@Configuration
public class MockIntegrationTestConfig {
}
