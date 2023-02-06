package ru.yandex.market.checkout.checkouter.context;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.Configurable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;

public class CheckouterContextTest extends AbstractServicesTestBase {

    @Autowired
    private ApplicationContext applicationContext;

    @DisplayName("Контекст чекаутера должен стартовать")
    @Test
    public void testContextStart() {
        HttpClient httpClient = (HttpClient) applicationContext
                .getBean("balanceCheckServiceHttpClient");
        Assertions.assertNotNull(httpClient);
        Assertions.assertTrue(((Configurable) httpClient).getConfig().getSocketTimeout() > 0,
                "Read timeout should be configured for balanceCheckServiceHttpClient");
    }
}
