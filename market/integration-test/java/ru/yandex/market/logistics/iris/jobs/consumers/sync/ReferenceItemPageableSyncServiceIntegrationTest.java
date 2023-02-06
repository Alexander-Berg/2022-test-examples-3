package ru.yandex.market.logistics.iris.jobs.consumers.sync;

import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemReference;
import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.source.Source;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.jobs.model.PageableExecutionQueueItemPayload;

import static org.mockito.Mockito.when;

public class ReferenceItemPageableSyncServiceIntegrationTest extends AbstractContextualTest {

    @Autowired
    private ReferenceItemPageableSyncService service;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Test
    @DatabaseSetup("classpath:fixtures/setup/reference_item_pageable_sync/1.xml")
    @ExpectedDatabase(value = "classpath:fixtures/setup/reference_item_pageable_sync/1.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void syncWorksCorrectInCaseEmptyItemReferenceInLgwAnswer() {
        Source source = new Source("172", SourceType.WAREHOUSE);
        PageableExecutionQueueItemPayload payload = new PageableExecutionQueueItemPayload("req", 1, 3, 1, source);

        when(fulfillmentClient.getReferenceItems(2, 1, new Partner(172L)))
                .thenReturn(Collections.singletonList(createEmptyItemReference()));

        service.processPayload(payload);
    }

    private ItemReference createEmptyItemReference() {
        return new ItemReference(
                null,
                null,
                null,
                Collections.emptySet(),
                null
        );
    }
}
