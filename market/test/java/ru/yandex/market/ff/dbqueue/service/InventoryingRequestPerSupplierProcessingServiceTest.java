package ru.yandex.market.ff.dbqueue.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.InventoryingRequestPerSupplierPayload;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class InventoryingRequestPerSupplierProcessingServiceTest extends IntegrationTest {

    @Autowired
    private InventoryingRequestPerSupplierProcessingService service;

    @Test
    @DatabaseSetup("classpath:db-queue/service/inventorying-request-per-supplier-processing/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/inventorying-request-per-supplier-processing/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void processPayload() {
        service.processPayload(new InventoryingRequestPerSupplierPayload(3, 1));
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/inventorying-request-per-supplier-processing/before-partially-saved.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/inventorying-request-per-supplier-processing" +
            "/after-partially-saved.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void processPartiallySavedPayload() {
        service.processPayload(new InventoryingRequestPerSupplierPayload(3, 2));
    }
}

