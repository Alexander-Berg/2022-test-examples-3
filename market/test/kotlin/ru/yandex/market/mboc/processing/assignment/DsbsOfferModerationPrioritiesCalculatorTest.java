package ru.yandex.market.mboc.processing.assignment;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferProcessingAssignment;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.processing.BaseOfferProcessingTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class DsbsOfferModerationPrioritiesCalculatorTest extends BaseOfferProcessingTest {
    @Autowired
    private OfferProcessingAssignmentRepository assignmentRepository;

    private JdbcTemplate yqlJdbcTemplateMock;
    private JdbcTemplate jdbcTemplateSpied;

    private StorageKeyValueServiceMock storageKeyValueServiceMock = new StorageKeyValueServiceMock();
    private DsbsOfferModerationPrioritiesCalculator calculator;

    @Before
    public void setUp() throws Exception {
        yqlJdbcTemplateMock = mock(JdbcTemplate.class);
        jdbcTemplateSpied = spy(jdbcTemplate);
        calculator = new DsbsOfferModerationPrioritiesCalculator(
            TransactionHelper.MOCK, jdbcTemplateSpied, yqlJdbcTemplateMock, storageKeyValueServiceMock,
            "", "", "", "", "");
    }

    @Test
    public void testStartingFromAnotherStage() {
        storageKeyValueServiceMock.putValue(
            DsbsOfferModerationPrioritiesCalculator.CURRENT_STAGE_KEY,
            DsbsOfferModerationPrioritiesCalculator.JobStage.UPDATE_OFFER_PROCESSING_ASSIGNMENTS.name()
        );

        // this will blow up on yt import
        Mockito.doThrow(new TestException())
            .when(yqlJdbcTemplateMock).query(anyString(), any(RowCallbackHandler.class));

        storageKeyValueServiceMock.putValue(
            DsbsOfferModerationPrioritiesCalculator.YT_IMPORT_TS_KEY,
            OffsetDateTime.now()
        );

        assertThatNoException().isThrownBy(() -> calculator.calculatePriorities());
    }

    @Test
    public void testYtImportTooYoung() {
        storageKeyValueServiceMock.putValue(
            DsbsOfferModerationPrioritiesCalculator.CURRENT_STAGE_KEY,
            DsbsOfferModerationPrioritiesCalculator.JobStage.FILL_TEMP_TABLE_FROM_YT.name()
        );

        // put now() as if yt import has just happened
        storageKeyValueServiceMock.putValue(
            DsbsOfferModerationPrioritiesCalculator.YT_IMPORT_TS_KEY,
            OffsetDateTime.now()
        );

        // this will blow up on yt import
        Mockito.doThrow(new TestException())
            .when(yqlJdbcTemplateMock).query(anyString(), any(RowCallbackHandler.class));

        // no exception is thrown == no yt import happened
        assertThatNoException().isThrownBy(() -> calculator.calculatePriorities());
    }

    @Test
    public void testYtImportTooOld() {
        // put current stage != FILL_TEMP_TABLE_FROM_YT
        storageKeyValueServiceMock.putValue(
            DsbsOfferModerationPrioritiesCalculator.CURRENT_STAGE_KEY,
            DsbsOfferModerationPrioritiesCalculator.JobStage.UPDATE_OFFER_PROCESSING_ASSIGNMENTS.name()
        );

        // mark yt import as one year old - it must be old enough
        storageKeyValueServiceMock.putValue(
            DsbsOfferModerationPrioritiesCalculator.YT_IMPORT_TS_KEY,
            OffsetDateTime.now().minusYears(1L)
        );

        // this will blow up on yt import
        Mockito.doThrow(new TestException())
            .when(yqlJdbcTemplateMock).query(anyString(), any(RowCallbackHandler.class));

        // no exception is thrown == no yt import happened
        assertThatThrownBy(() -> calculator.calculatePriorities())
            .isInstanceOf(TestException.class);
    }


    @Test
    public void applyToOfferProcessingAssignments() {
        var assignments = IntStream.rangeClosed(1, 100).mapToObj(i -> new OfferProcessingAssignment()
            .setOfferId(i)
            .setProcessingStatus(Offer.ProcessingStatus.IN_MODERATION)
        ).collect(Collectors.toList());

        assignmentRepository.insertBatch(assignments);

        jdbcTemplate.update("insert into mbo_category." + DsbsOfferModerationPrioritiesCalculator.TABLE_NAME
            + "(offer_id, priority) select offer_id, offer_id as priority from "
            + OfferProcessingAssignmentRepository.TABLE_NAME);

        var list = jdbcTemplate.queryForList("select * from mbo_category."
                + DsbsOfferModerationPrioritiesCalculator.TABLE_NAME);
        assertThat(list).hasSize(100);

        calculator.updateOfferProcessingAssignments();

        assignmentRepository.findAll()
            .forEach(assignment -> assertThat(assignment.getPriority()).isEqualTo(assignment.getOfferId()));
    }

    private static class TestException extends RuntimeException {
    }
}
