package ru.yandex.market.abo.tms.complain;

import java.util.Date;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.api.entity.complaint.ComplaintType;
import ru.yandex.market.abo.api.entity.offer.hidden.details.HidingDetails;
import ru.yandex.market.abo.core.complain.model.Complaint;
import ru.yandex.market.abo.core.complain.service.ComplaintService;
import ru.yandex.market.abo.core.hiding.util.checker.CheckerFactory;
import ru.yandex.market.abo.core.hiding.util.checker.FeedReportDiff;
import ru.yandex.market.abo.core.hiding.util.model.CheckStatus;
import ru.yandex.market.abo.core.storage.json.hidden.JsonHidingDetailsService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * created on 21.06.17.
 */
class ComplaintProcessorTest {
    private static final ComplaintType COMPLAINT_TYPE = ComplaintType.PRICE;

    @InjectMocks
    private NewComplaintProcessor complaintProcessor;
    @Mock
    private CheckerFactory checkerFactory;
    @Mock
    private Complaint complaint;
    @Mock
    private FeedReportDiff feedReportDiff;
    @Mock
    private JsonHidingDetailsService jsonHidingDetailsService;
    @Mock
    private ComplaintService complaintService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(complaint.isExternal()).thenReturn(true);
        when(complaintService.complaintFromSpammer(complaint)).thenReturn(false);
        when(checkerFactory.getAllComplaintCheckers()).thenReturn(new HashMap<>() {{
            put(COMPLAINT_TYPE, feedReportDiff);
        }});
    }

    @Test
    void findDiff() {
        when(feedReportDiff.diff(complaint)).thenReturn(HidingDetails.newBuilder().build());
        when(complaint.getOfferRemovedDate()).thenReturn(new Date());
        complaintProcessor.findDiff(complaint);
        verify(complaint).hideOffer();
        verify(complaint, never()).setComplaintType(any());
        verify(complaint).setHidingReason(COMPLAINT_TYPE);
        verify(jsonHidingDetailsService).saveFeedDiff(any(), any());
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    void findNoDiff(boolean complaintFromSpammer) {
        when(complaintService.complaintFromSpammer(complaint)).thenReturn(complaintFromSpammer);

        when(feedReportDiff.diff(complaint)).thenReturn(null);
        when(complaint.hasActualFeed()).thenReturn(true);
        complaintProcessor.findDiff(complaint);
        verify(complaint).setCheckStatus(complaintFromSpammer ? CheckStatus.CANCELLED_AS_SPAM :
                CheckStatus.GENERATE_TICKET);
        verifyNoMoreInteractions(jsonHidingDetailsService);
    }
}
