package ru.yandex.market.abo.core.outlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.EmptyTestWithTransactionTemplate;
import ru.yandex.market.abo.core.outlet.maps.OutletApprover;
import ru.yandex.market.abo.core.outlet.maps.OutletTolokaApprover;
import ru.yandex.market.abo.core.outlet.maps.OutletYtManager;
import ru.yandex.market.abo.core.outlet.maps.model.OutletTolokaCheck;
import ru.yandex.market.abo.core.outlet.maps.model.OutletTolokaResult;
import ru.yandex.market.abo.core.outlet.model.OutletCheck;
import ru.yandex.market.abo.core.outlet.model.status.OutletCheckStatus;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.OutletInfoDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 21.02.18.
 */
public class OutletCheckManagerTest extends EmptyTestWithTransactionTemplate {
    private static final long ABO_ID = -1;
    private static final long MBI_OUTLET_ID = 1;

    @InjectMocks
    OutletCheckManager outletCheckManager;
    @Mock
    OutletCheckService outletCheckService;
    @Mock
    OutletApprover outletApprover;
    @Mock
    OutletYtManager outletYtManager;
    @Mock
    OutletCheck outletCheck;
    @Mock
    OutletInfo outletInfo;
    @Mock
    OutletInfoDTO outletDTO;
    @Mock
    OutletTolokaCheck outletTolokaCheck;
    @Mock
    MbiApiService mbiApiService;
    @Mock
    OutletTolokaResult outletTolokaResult;
    @Mock
    OutletTolokaApprover outletTolokaApprover;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(outletDTO.toOutletInfo()).thenReturn(outletInfo);
    }

    @Test
    public void checkOutletsWithMapsDb() {
        var outletChecks = List.of(outletCheck);
        var mbiOutlets = List.of(outletDTO);
        when(outletCheck.getStatus()).thenReturn(OutletCheckStatus.NEW);
        when(outletCheck.getId()).thenReturn(ABO_ID);
        when(outletCheck.getMbiOutletId()).thenReturn(MBI_OUTLET_ID);

        when(outletCheckService.loadChecksToCompareWithSprav()).thenReturn(outletChecks);
        when(mbiApiService.getOutlets(any())).thenReturn(mbiOutlets);
        when(outletApprover.approveOutlets(outletChecks, mbiOutlets))
                .thenReturn(Collections.singletonList(outletTolokaCheck));

        outletCheckManager.checkOutletsWithSpravDb();

        verify(outletApprover).approveOutlets(outletChecks, mbiOutlets);
        verify(outletCheckService).save(outletChecks);
        verify(outletYtManager).saveForTolokers(Collections.singletonList(outletTolokaCheck));
    }

    @Test
    public void processChecksFromToloka() {
        when(outletCheckService.loadChecksInYt()).thenReturn(List.of(outletCheck));
        when(outletCheck.getId()).thenReturn(ABO_ID);
        when(outletCheck.getMbiOutletId()).thenReturn(MBI_OUTLET_ID);

        var tolokaResults = List.of(outletTolokaResult);
        when(outletYtManager.getTolokaResults(Set.of(ABO_ID))).thenReturn(tolokaResults);
        when(outletTolokaResult.getOutletCheckId()).thenReturn(ABO_ID);

        when(mbiApiService.getOutlet(MBI_OUTLET_ID)).thenReturn(outletDTO);

        outletCheckManager.processChecksFromToloka();

        verify(outletTolokaApprover).approveOutlets(Map.of(ABO_ID, outletCheck), tolokaResults);
        var checksToSavecaptor = ArgumentCaptor.forClass(Collection.class);
        verify(outletCheckService).save(checksToSavecaptor.capture());
        assertEquals(List.of(outletCheck), new ArrayList<OutletCheck>(checksToSavecaptor.getValue()));
    }
}
