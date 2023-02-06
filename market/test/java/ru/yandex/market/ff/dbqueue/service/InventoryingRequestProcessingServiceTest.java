package ru.yandex.market.ff.dbqueue.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.InventoryingRequestPayload;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class InventoryingRequestProcessingServiceTest extends IntegrationTest {

    @Autowired
    private InventoryingRequestProcessingService service;

    @Test
    @DatabaseSetup("classpath:db-queue/service/inventorying-request-processing/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/inventorying-request-processing/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void processPayload() {
        service.processPayload(new InventoryingRequestPayload(3));
    }
}
