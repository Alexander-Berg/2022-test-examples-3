package ru.yandex.market.ff.dbqueue.service;


import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.EnrichRequestItemPayload;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class EnrichRequestItemProcessingServiceTest extends IntegrationTest {

    @Autowired
    private EnrichRequestItemProcessingService service;

    @Test
    @DatabaseSetup("classpath:db-queue/service/enrich-request-item-processing/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/enrich-request-item-processing/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void processPayloadTest() {
        service.processPayload(new EnrichRequestItemPayload(3, 3, 0));
    }

}
