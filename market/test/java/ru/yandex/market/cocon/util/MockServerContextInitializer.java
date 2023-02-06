package ru.yandex.market.cocon.util;

import org.mockserver.integration.ClientAndServer;
import org.mockserver.socket.PortFactory;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

/**
 * Реализация {@link ApplicationContextInitializer}, которая инициализирует инстанс mock-server {@link ClientAndServer}
 * на случайном доступном порту и проставляет URL для этого сервера в переменные {@code market.mbi.partner.url},
 * {@code market.analytics.platform.backend.url} и {@code tplPvz.url} для последующего использования в логике
 * расчета чекеров.
 */
public class MockServerContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        int mockPort = PortFactory.findFreePort();
        var mockServer = ClientAndServer.startClientAndServer(mockPort);

        applicationContext
                .getBeanFactory()
                .registerSingleton("mockServer", mockServer);

        applicationContext.addApplicationListener(applicationEvent -> {
            if (applicationEvent instanceof ContextClosedEvent) {
                mockServer.stop();
            }
        });

        TestPropertyValues
                .of(
                        "market.mbi.partner.url=http://localhost:" + mockPort,
                        "market.analytics.platform.backend.url=http://localhost:" + mockPort,
                        "market.mbi.checkers.url=http://localhost:" + mockPort,
                        "tplPvz.url=http://localhost:" + mockPort
                )
                .applyTo(applicationContext);
    }

}
