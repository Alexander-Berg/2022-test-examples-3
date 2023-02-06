package ru.yandex.market.checkout.checkouter.saturn;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.util.saturn.SaturnMockConfigurer;

public class SaturnRestApiTest extends AbstractWebTestBase {

    private final Random random = new Random();
    @Autowired
    private SaturnRestApi saturnRestApi;
    @Autowired
    private SaturnMockConfigurer saturnMockConfigurer;

    @Test
    public void getScoringResponse() {
        var request = new ScoringRequest(
                UUID.randomUUID().toString(),
                random.nextLong(),
                new ScoringRequestBasket(new BigDecimal(random.nextInt()), new BigDecimal(random.nextInt())));
        saturnMockConfigurer.mockScoring(request);

        var response = saturnRestApi.getScoringResponse(request);

        Assertions.assertEquals(request.getPuid(), response.getPuid());
    }
}
