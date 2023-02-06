package ru.yandex.market.delivery.transport_manager.service.health.product;

import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;

@DatabaseSetup({
    "/repository/transportation/multiple_transportations_deps.xml",
    "/repository/transportation/multiple_transportations.xml",
    "/repository/health/transportation/unit_documents.xml"
})
public class UnitDocumentsHealthCheckerTest extends AbstractContextualTest {
    @Autowired
    private UnitDocumentsHealthChecker checker;

    @Test
    void checkTasksCreatedInTime() {
        clock.setFixed(Instant.parse("2021-07-12T17:01:59Z"), ZoneOffset.UTC);
        softly.assertThat(checker.checkTasksCreatedInTime())
            .isEqualTo("2;1 transportation unit documents are in NEW state for more than 1 minutes");
    }

    @Test
    void checkResultsReceivedInTime() {
        clock.setFixed(Instant.parse("2021-07-12T18:02:59Z"), ZoneOffset.UTC);
        softly.assertThat(checker.checkResultsReceivedInTime())
            .isEqualTo("2;1 transportation unit documents are in LGW_SENT state for more than 2 minutes");
        clock.setFixed(Instant.parse("2021-07-12T18:00:59Z"), ZoneOffset.UTC);
        softly.assertThat(checker.checkResultsReceivedInTime())
            .isEqualTo("0;OK");
    }

    @Test
    void checkErrorsQuantity() {
        Mockito.when(propertyService.getInt(TmPropertyKey.UNIT_DOCUMENTS_IN_ERROR_THRESHOLD)).thenReturn(2);
        Mockito.when(propertyService.getInt(TmPropertyKey.UNIT_DOCUMENTS_IN_ERROR_HOUR_GAP)).thenReturn(1);

        clock.setFixed(Instant.parse("2021-07-12T15:02:59Z"), ZoneOffset.UTC);
        softly.assertThat(checker.checkErrorsQuantity())
            .isEqualTo("2;2 transportation unit documents are in ERROR state");
        clock.setFixed(Instant.parse("2021-07-12T15:20:59Z"), ZoneOffset.UTC);
        softly.assertThat(checker.checkErrorsQuantity())
            .isEqualTo("0;OK");
    }

    @Test
    @DatabaseSetup(
        value = "/repository/transportation/update/all_processed_outbound.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup("/repository/partner_info/partner_info.xml")
    @DatabaseSetup(
        value = "/repository/transportation_unit_documents/empty_documents.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    void checkProcessOutbounds() {
        clock.setFixed(Instant.parse("2020-07-12T15:02:59Z"), ZoneOffset.UTC);
        softly.assertThat(checker.checkProcessedOutbounds())
            .contains("[5]");

        clock.setFixed(Instant.parse("2021-07-13T15:20:59Z"), ZoneOffset.UTC);
        softly.assertThat(checker.checkErrorsQuantity())
            .isEqualTo("0;OK");
    }
}
