package ru.yandex.market.ff.dbqueue.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.PutFFInboundRegistryPayload;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryType;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundRegistry;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class PutFFInboundProcessingServiceTest extends IntegrationTest {

    @Autowired
    private PutFFInboundRegistryProcessingService putFFInboundRegistryProcessingService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DatabaseSetup("classpath:db-queue/service/put-ff-inbound/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/put-ff-inbound/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void putFFInboundSuccess() throws Exception {
        long registryId = 1;
        InboundRegistry registry = InboundRegistry.builder(
                ResourceId.builder().build(),
                ResourceId.builder().setYandexId("10").build(),
                RegistryType.PLANNED
        ).build();
        transactionTemplate.execute(status -> {
            putFFInboundRegistryProcessingService.processPayload(new PutFFInboundRegistryPayload(registryId, registry));
            return null;
        });
        Mockito.verify(fulfillmentClient).putInboundRegistry(eq(registry), any());
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/put-ff-inbound/before-multiple-same-items.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/put-ff-inbound/after-multiple-same-items.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void putFFInboundSuccessWithMultipleSameItemsInRegistry() throws Exception {
        long registryId = 1;
        InboundRegistry registry = InboundRegistry.builder(
                ResourceId.builder().build(),
                ResourceId.builder().setYandexId("10").build(),
                RegistryType.PLANNED
        ).build();
        transactionTemplate.execute(status -> {
            putFFInboundRegistryProcessingService.processPayload(new PutFFInboundRegistryPayload(registryId, registry));
            return null;
        });
        Mockito.verify(fulfillmentClient).putInboundRegistry(eq(registry), any());
    }
}
