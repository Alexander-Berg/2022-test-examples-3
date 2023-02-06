package ru.yandex.market.abo.core.complain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.complain.model.Complaint;
import ru.yandex.market.abo.core.complain.service.ComplaintService;
import ru.yandex.market.abo.core.datacamp.client.DataCampClient;
import ru.yandex.market.abo.core.hiding.util.OfferInfoService;
import ru.yandex.market.abo.core.hiding.util.filter.ComplaintOfferFilterService;
import ru.yandex.market.abo.core.hiding.util.model.CheckStatus;
import ru.yandex.market.abo.core.indexer.Generation;
import ru.yandex.market.abo.core.indexer.GenerationService;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.offer.report.OfferService;
import ru.yandex.market.abo.core.shop.ShopInfo;
import ru.yandex.market.abo.core.shop.ShopInfoService;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.abo.test.TestHelper;
import ru.yandex.market.common.report.MarketReportOverloadedException;
import ru.yandex.market.common.report.indexer.IdxAPI;
import ru.yandex.market.common.report.indexer.model.OfferDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * created on 18.11.15.
 */
@SuppressWarnings("unchecked")
class ComplaintManagerTest extends EmptyTest {
    @Autowired
    ComplaintManager complaintManager;

    @InjectMocks
    private OfferInfoService offerInfoService;
    @Mock
    private ComplaintService complaintService;
    @Mock
    private DataCampClient dataCampClient;
    @Mock
    private ShopInfoService shopInfoService;
    @Mock
    private IdxAPI idxApiService;
    @Mock
    private GenerationService generationService;
    @Mock
    private OfferService offerService;
    @Mock
    private MbiApiService mbiApiService;
    @Mock
    private ExecutorService reportPool;
    @Mock
    private ExecutorService feedPool;

    @BeforeEach
    void init() {
        final int COMPLAINT_NUM = 10;
        MockitoAnnotations.openMocks(this);
        when(mbiApiService.isPushPartner(anyLong())).thenReturn(false);
        ComplaintOfferFilterService offerFilterService = new ComplaintOfferFilterService();
        when(complaintService.findComplaints(any(), anyBoolean())).thenReturn(initComplaints(COMPLAINT_NUM));
        Generation generation = mock(Generation.class);
        when(generationService.loadLastReleaseGeneration()).thenReturn(generation);
        offerFilterService.setHideOffersDBService(complaintService);
        complaintManager.setComplaintService(complaintService);
        offerFilterService.setOfferInfoService(offerInfoService);
        complaintManager.setOfferFilterService(offerFilterService);
        var shop = mock(ShopInfo.class);
        when(shop.isSmb()).thenReturn(false);
        when(shopInfoService.getShopInfo(anyLong())).thenReturn(shop);
        TestHelper.mockExecutorService(reportPool, feedPool);
    }

    private List<Complaint> initComplaints(int num) {
        List<Complaint> complaints = new ArrayList<>();
        while (num > 0) {
            Complaint complaint = new Complaint();
            complaint.setWareMd5(String.valueOf(num--));
            complaint.setId(RND.nextLong());
            complaints.add(complaint);
        }
        return complaints;
    }

    @Test
    void testComplaintsHaveOffers() throws Exception {
        when(offerService.findFirstWithParams(any())).thenReturn(initOffer()).thenReturn(null);
        List<Complaint> complaints = complaintManager.prepare(CheckStatus.NEW, false);
        complaints.forEach(c -> assertNotNull(c.getReportOffer()));
        ((List<Complaint>) getUpdatedComplaints().get(0)).forEach(c -> assertEquals(c.getCheckStatus(), CheckStatus.LEAVE_ALONE));
    }

    @Test
    void testReportException() throws Exception {
        // в случае недоступности репорта проверка должна быть отложена
        when(offerService.findFirstWithParams(any())).thenThrow(new MarketReportOverloadedException());
        List<Complaint> complaints = complaintManager.prepare(CheckStatus.NEW, false);
        assertTrue(complaints.isEmpty());
        assertEquals(0, getLastUpdatedComplaints().size());
    }

    @Test
    void testFeedException() throws Exception {
        //в случае ошибок на стороне фида проверка должна быть отложена
        when(idxApiService.findOfferWithDefaultDelivery(any(), any(), any(), Mockito.<Long>any())).thenThrow(new IllegalStateException());
        when(offerService.findFirstWithParams(any())).thenReturn(initOffer());
        List<Complaint> complaints = complaintManager.prepare(CheckStatus.NEW, false);
        assertTrue(complaints.isEmpty());
    }

    @Test
    void testHaveNoDuplicates() throws Exception {
        List<Complaint> duplicates = new ArrayList<>();
        duplicates.addAll(initComplaints(1));
        duplicates.addAll(initComplaints(1));
        duplicates.addAll(initComplaints(1));
        when(complaintService.findComplaints(any(), anyBoolean())).thenReturn(duplicates);
        when(offerService.findFirstWithParams(any())).thenReturn(initOffer());
        when(idxApiService.findOfferWithDefaultDelivery(any(), any(), any(), Mockito.<Long>any()))
                .thenReturn(mock(OfferDetails.class));
        List<Complaint> complaints = complaintManager.prepare(CheckStatus.NEW, false);
        assertEquals(1, complaints.size());
        assertEquals(2, getUpdatedComplaints().get(0).size());
        getLastUpdatedComplaints().forEach(c -> assertEquals(c.getCheckStatus(), CheckStatus.LEAVE_ALONE));
    }

    private Offer initOffer() {
        Offer offer = new Offer();
        offer.setShopId(0L);
        return offer;
    }

    private List<List> getUpdatedComplaints() {
        ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
        verify(complaintService, atLeastOnce()).updateTillNextGen(argument.capture());
        return argument.getAllValues();
    }

    private List<Complaint> getLastUpdatedComplaints() {
        List<List> res = getUpdatedComplaints();
        return res.get(res.size() - 1);
    }
}
