package ru.yandex.market.checkout;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractContainerTestBase;
import ru.yandex.market.checkout.checkouter.client.CartParameters;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;

public class ChaosMonkeyTest extends AbstractContainerTestBase {

    @Autowired
    CheckouterClient checkouterClient;

    @Test
    void shouldGet500() {
        ErrorCodeException ex = Assertions.assertThrows(ErrorCodeException.class,
                () -> checkouterClient.cart(MultiCartProvider.single(OrderProvider.getBlueOrder()),
                        CartParameters.builder()
                        .withExperiments("checkouter-chaosmonkey-500=1")
                        .build()));

        assertThat(ex.getStatusCode(), comparesEqualTo(500));
    }

    @Test
    void shouldGet400() {
        ErrorCodeException ex = Assertions.assertThrows(ErrorCodeException.class,
                () -> checkouterClient.cart(MultiCartProvider.single(OrderProvider.getBlueOrder()),
                        CartParameters.builder()
                        .withExperiments("checkouter-chaosmonkey-400=1")
                        .build()));

        assertThat(ex.getStatusCode(), comparesEqualTo(400));
    }
}
