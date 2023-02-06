package ru.yandex.market.ff.dbqueue.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.CreateAutoAdditionalSupplyOnUnknownBoxesPayload;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class CreateAutoAdditionalSupplyOnUnknownBoxesProcessingServiceTest extends IntegrationTest {

    @Autowired
    private CreateAutoAdditionalSupplyOnUnknownBoxesProcessingService service;

    @Test
    @DatabaseSetup("classpath:db-queue/service/create-auto-additional-supply/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/create-auto-additional-supply/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createAutoAdditionalSupply() {
        service.processPayload(new CreateAutoAdditionalSupplyOnUnknownBoxesPayload(10));
    }
}
