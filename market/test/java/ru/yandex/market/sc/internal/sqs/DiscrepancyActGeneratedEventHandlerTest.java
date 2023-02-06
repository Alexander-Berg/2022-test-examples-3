package ru.yandex.market.sc.internal.sqs;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.s3_document.repository.S3DocumentRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.sqs.handler.DiscrepancyActGeneratedEventHandler;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;
import ru.yandex.market.sc.internal.util.SqsEventFactory;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbIntTest
public class DiscrepancyActGeneratedEventHandlerTest {

    @Autowired
    private DiscrepancyActGeneratedEventHandler discrepancyActGeneratedEventHandler;

    @Autowired
    private TestFactory testFactory;

    @Autowired
    private Clock clock;

    @Autowired
    private SqsEventFactory sqsEventFactory;

    @Autowired
    private S3DocumentRepository s3DocumentRepository;

    private SortingCenter sortingCenter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    @DisplayName("success обработки сообщения о готовности акта расхождения")
    public void successDiscrepancyActGenerated() {
        String transportationId = "transportation_1";
        String bucket = "bucket_1";
        String file = "file_1";
        Boolean isDiscrepancyExists = false;
        var params = TestFactory.CreateInboundParams
                .builder()
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .transportationId(transportationId)
                .sortingCenter(sortingCenter)
                .build();
        var inbound = testFactory.createInbound(params);
        var event = sqsEventFactory
                .createDiscrepancyActGeneratedEventSqsEvent(transportationId, bucket, file, isDiscrepancyExists);
        var responsePayload = discrepancyActGeneratedEventHandler.handle(event);
        inbound = testFactory.getInbound(inbound.getId());
        assertThat(inbound.getDiscrepancyActId()).isNotNull();
        var discrepancyActId = inbound.getDiscrepancyActId();
        assertThat(s3DocumentRepository.findByIdOrThrow(discrepancyActId)).isNotNull();
        var s3Document = s3DocumentRepository.findByIdOrThrow(discrepancyActId);
        assertThat(s3Document.getBucket()).isEqualTo(bucket);
        assertThat(s3Document.getFilename()).isEqualTo(file);
    }

}
