package ru.yandex.market.checkout.carter;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.carter.client.Carter;
import ru.yandex.market.checkout.carter.model.CartRequest;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.web.UserContext;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static ru.yandex.market.checkout.carter.model.UserIdType.UID;

public class ChaosMonkeyTest extends CarterContainerTestBase {

    @Autowired
    private Carter carterClient;

    private UserContext uidContext;

    @BeforeEach
    public void setUp() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        uidContext = UserContext.of(OwnerKey.of(Color.BLUE, UID, "" + rnd.nextLong(1, Long.MAX_VALUE)));
    }

    @Test
    void shouldGet500() {
        ErrorCodeException ex = Assertions.assertThrows(ErrorCodeException.class,
                () -> carterClient.getCart(CartRequest.builder(uidContext.getUserAnyId(), uidContext.getUserIdType())
                        .withExperiments("carter-chaosmonkey-500=1")
                        .withRgb(uidContext.getColor())
                        .withIgnoreTvmCheck(true)
                        .build()));

        assertThat(ex.getStatusCode(), comparesEqualTo(500));
    }

    @Test
    void shouldGet400() {
        ErrorCodeException ex = Assertions.assertThrows(ErrorCodeException.class,
                () -> carterClient.getCart(CartRequest.builder(uidContext.getUserAnyId(), uidContext.getUserIdType())
                        .withExperiments("carter-chaosmonkey-400=1")
                        .withRgb(uidContext.getColor())
                        .withIgnoreTvmCheck(true)
                        .build()));

        assertThat(ex.getStatusCode(), comparesEqualTo(400));
    }
}
