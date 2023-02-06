package ru.yandex.market.delivery.transport_manager.repository.mappers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.axapta.document.AxaptaDocumentRequest;
import ru.yandex.market.delivery.transport_manager.domain.entity.axapta.document.AxaptaDocumentRequestStatus;

@DatabaseSetup({
    "/repository/transportation/multiple_transportations_deps.xml",
    "/repository/transportation/multiple_transportations.xml",
    "/repository/transportation/register.xml",
    "/repository/axapta_document_request/several_requests.xml"
})
class AxaptaDocumentMapperTest extends AbstractContextualTest {
    @Autowired
    private AxaptaDocumentMapper axaptaDocumentMapper;

    @BeforeEach
    void setUp() {
        clock.setFixed(
            LocalDateTime.of(2022, 12,  11, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
    }

    @Test
    void findById() {
        AxaptaDocumentRequest request = axaptaDocumentMapper.findById(7L);

        softly.assertThat(request).isEqualTo(
            request(
                7L,
                AxaptaDocumentRequestStatus.SENT_TO_DC,
                1L,
                List.of(111L, 222L, 333L),
                "123",
                List.of(111L, 222L),
                List.of("doc1", "doc2")
            )
        );
    }

    @Test
    void findByAxaptaRequestId() {
        AxaptaDocumentRequest request = axaptaDocumentMapper.findByAxaptaRequestId("124");

        softly.assertThat(request).isEqualTo(
            request(
                8L,
                AxaptaDocumentRequestStatus.SENT_TO_DC,
                1L,
                List.of(777L, 888L),
                "124",
                List.of(888L),
                List.of("doc6", "doc7", "doc8")
            )
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/axapta_document_request/expected/several_requests_after_set_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void setStatus() {
        axaptaDocumentMapper.setStatus(
            List.of(1L, 3L),
            AxaptaDocumentRequestStatus.WAITING_AXAPTA_RESPONSE,
            LocalDateTime.now(clock)
        );
    }

    @Test
    void findOldestWithStatus() {
        List<AxaptaDocumentRequest> oldestWithStatus =
            axaptaDocumentMapper.findOldestWithStatus(AxaptaDocumentRequestStatus.NEW, 2);

        softly.assertThat(oldestWithStatus).containsExactlyInAnyOrder(
            request(4L, AxaptaDocumentRequestStatus.NEW, 1L, null, null, null, null),
            request(1L, AxaptaDocumentRequestStatus.NEW, 1L, null, null, null, null)
        );

        List<AxaptaDocumentRequest> oldestWithStatus1 =
            axaptaDocumentMapper.findOldestWithStatus(AxaptaDocumentRequestStatus.SENDING_TO_DC, 100);

        softly.assertThat(oldestWithStatus1).isEmpty();
    }

    @Test
    void insert() {
        AxaptaDocumentRequest request = request(
            null,
            AxaptaDocumentRequestStatus.NEW,
            1L,
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            Collections.emptyList()
        );
        request.setRequestedRegisterId(10001L);

        axaptaDocumentMapper.insert(request);

        softly.assertThat(axaptaDocumentMapper.findById(9L))
            .isEqualTo(request.setId(9L));
    }

    @Test
    void update() {
        AxaptaDocumentRequest request = axaptaDocumentMapper.findById(7L);

        request.setDocumentUrls(List.of("new list 1", "new list 2"));
        request.setStatus(AxaptaDocumentRequestStatus.ERROR);
        request.setInboundsUsedByAxapta(List.of(666L, 777L));
        request.setRequestedRegisterId(10001L);

        axaptaDocumentMapper.update(request, LocalDateTime.now(clock));
        AxaptaDocumentRequest requestAfterModification = axaptaDocumentMapper.findById(7L);

        softly.assertThat(requestAfterModification).isEqualTo(request);
    }

    private static AxaptaDocumentRequest request(
        Long id,
        AxaptaDocumentRequestStatus status,
        Long transportationId,
        List<Long> inboundsRequestedToShip,
        String axaptaRequestId,
        List<Long> inboundsUsedByAxapta,
        List<String> documentUrls
    ) {
        return new AxaptaDocumentRequest()
            .setId(id)
            .setStatus(status)
            .setTransportationId(transportationId)
            .setInboundsRequestedToShip(inboundsRequestedToShip)
            .setAxaptaRequestId(axaptaRequestId)
            .setInboundsUsedByAxapta(inboundsUsedByAxapta)
            .setDocumentUrls(documentUrls);
    }
}
