package ru.yandex.market.sc.internal.sqs.handler;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.tm.TrnReadyEvent;
import ru.yandex.market.sc.core.domain.outbound.OutboundTrnDocsService;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundTrnDocs;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.sqs.SqsEventType;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;
import ru.yandex.market.sc.internal.util.SqsEventFactory;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbIntTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class TrnDocumentsReadyEventHandlerTest {

    private final TrnDocumentsReadyEventHandler handler;
    private final SqsEventFactory sqsEventFactory;
    private final OutboundTrnDocsService outboundTrnDocsService;
    private final TestFactory testFactory;
    private final Clock clock;

    @Test
    void consumeTrnDocumentsReadyEvent() {
        List<String> documents = List.of(
                "https://market-tpl-sc.s3.yandex.net/TRN_09.02.2022_1.xlsx",
                "https://market-tpl-sc.s3.yandex.net/TRN_09.02.2022_2.xlsx",
                "https://market-tpl-sc.s3.yandex.net/TRN_09.02.2022_3.xlsx"
        );
        var outbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("100010001")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant())
                .toTime(clock.instant())
                .sortingCenter(testFactory.storedSortingCenter(1001L))
                .logisticPointToExternalId(testFactory.storedSortingCenter(1002L).getYandexId())
                .build()
        );
        handler.handle(sqsEventFactory.makeSqsEvent(
                SqsEventType.REVERT_ORDER_DAMAGED,
                Instant.now(clock).toEpochMilli(),
                new TrnReadyEvent(1001L, "100010001", documents))
        );

        OutboundTrnDocs actualDocuments = outboundTrnDocsService.findDocuments(outbound).orElseThrow();
        assertThat(actualDocuments.getDocuments()).hasSize(documents.size());
        assertThat(actualDocuments.getDocuments()).containsAnyElementsOf(documents);
    }
}
