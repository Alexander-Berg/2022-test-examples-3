package ru.yandex.market.abo.core.outlet.maps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.outlet.OutletRecommendationBuilder;
import ru.yandex.market.abo.core.outlet.maps.model.OutletTolokaResult;
import ru.yandex.market.abo.core.outlet.model.OutletCheck;
import ru.yandex.market.abo.core.outlet.model.status.OutletCheckStatus;
import ru.yandex.market.abo.core.outlet.model.status.OutletCheckSubStatus;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.OutletInfoDTO;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 05.07.18.
 */
public class OutletTolokaApproverTest {
    private static final long ABO_CHECK_ID = 1;
    private static final long MBI_ID = -1L;
    private static final int SCHEDULE_DIFF_REJECTION_REASON_ID = 6;
    private static final int ADDRESS_DIFF_REJECTION_REASON_ID = 2;

    @InjectMocks
    private OutletTolokaApprover outletApprover;
    @Mock
    private OutletParamsComparator paramsComparator;
    @Mock
    private OutletCheck aboCheck;
    @Mock
    private OutletTolokaResult tolokaResult;
    @Mock
    private OutletInfo mbiOutlet;
    @Mock
    private OutletInfoDTO mbiOutletDTO;
    @Mock
    private OutletRecommendationBuilder recommendationBuilder;
    @Mock
    private MbiApiService mbiApiService;

    private Map<Long, OutletCheck> aboChecksById = new HashMap<>();
    private List<OutletTolokaResult> tolokaResults = new ArrayList<>();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        tolokaResults.clear();
        aboChecksById.clear();

        when(aboCheck.getId()).thenReturn(ABO_CHECK_ID);
        when(aboCheck.getMbiOutletId()).thenReturn(MBI_ID);
        when(tolokaResult.getOutletCheckId()).thenReturn(ABO_CHECK_ID);
        when(tolokaResult.hasAllData()).thenReturn(true);
        when(mbiOutletDTO.toOutletInfo()).thenReturn(mbiOutlet);
        when(mbiOutlet.getId()).thenReturn(MBI_ID);
        when(mbiApiService.getOutlet(MBI_ID)).thenReturn(mbiOutletDTO);

        aboChecksById.put(ABO_CHECK_ID, aboCheck);
        tolokaResults.add(tolokaResult);

        when(recommendationBuilder.createRecommendation(anyInt(), any()))
                .then(inv -> Arrays.stream(inv.getArguments()).map(String::valueOf).collect(Collectors.joining(" ")));
    }

    @Test
    public void approve_allFine() {
        when(paramsComparator.workingTimesEqual(mbiOutlet, tolokaResult)).thenReturn(true);
        when(paramsComparator.addressEqual(mbiOutlet, tolokaResult)).thenReturn(true);

        outletApprover.approveOutlets(aboChecksById, tolokaResults);

        verify(aboCheck).setStatus(OutletCheckStatus.APPROVED);
        verify(aboCheck).setSubStatus(OutletCheckSubStatus.TOLOKERS);
    }

    @Test
    public void approve_mbiAbsent() {
        when(mbiApiService.getOutlet(MBI_ID)).thenReturn(null);
        outletApprover.approveOutlets(aboChecksById, tolokaResults);

        verify(aboCheck).setStatus(OutletCheckStatus.CANCELED);
        verifyNoMoreInteractions(paramsComparator, recommendationBuilder);
    }

    @Test
    public void approve_workTimeDiff() {
        when(paramsComparator.workingTimesEqual(mbiOutlet, tolokaResult)).thenReturn(false);
        outletApprover.approveOutlets(aboChecksById, tolokaResults);

        verify(aboCheck).setStatus(OutletCheckStatus.REJECTED);
        verify(aboCheck).setRejectionReason(SCHEDULE_DIFF_REJECTION_REASON_ID);
        verify(aboCheck).setRecommendation(notNull());
    }

    @Test
    public void approve_addressDiff() {
        when(paramsComparator.workingTimesEqual(mbiOutlet, tolokaResult)).thenReturn(true);
        when(paramsComparator.addressEqual(mbiOutlet, tolokaResult)).thenReturn(false);
        outletApprover.approveOutlets(aboChecksById, tolokaResults);

        verify(aboCheck).setStatus(OutletCheckStatus.REJECTED);
        verify(aboCheck).setRejectionReason(ADDRESS_DIFF_REJECTION_REASON_ID);
        verify(aboCheck).setRecommendation(notNull());
    }
}
