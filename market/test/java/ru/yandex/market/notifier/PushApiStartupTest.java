package ru.yandex.market.notifier;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.notifier.configuration.NotifierWireMockConfiguration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:clients.xml")
@Import(NotifierWireMockConfiguration.class)
public class PushApiStartupTest  {
    @Autowired
    public PushApi pushApi;

    @Test
    public void shouldStartup() {
        assertThat(pushApi, notNullValue());
    }
}
