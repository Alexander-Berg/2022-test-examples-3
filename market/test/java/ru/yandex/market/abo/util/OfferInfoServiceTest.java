package ru.yandex.market.abo.util;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutorService;

import Market.DataCamp.DataCampOffer;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.complain.model.Complaint;
import ru.yandex.market.abo.core.datacamp.client.DataCampClient;
import ru.yandex.market.abo.core.hiding.util.OfferInfoService;
import ru.yandex.market.abo.core.offer.report.Cpa;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.offer.report.OfferService;
import ru.yandex.market.abo.core.offer.report.ReportParam;
import ru.yandex.market.abo.core.shop.ShopInfo;
import ru.yandex.market.abo.core.shop.ShopInfoService;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.abo.test.TestHelper;
import ru.yandex.market.common.report.indexer.IdxAPI;
import ru.yandex.market.common.report.indexer.model.OfferDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * created on 12.07.16.
 */
class OfferInfoServiceTest {
    private static final Random RND = new Random();
    @InjectMocks
    private OfferInfoService offerInfoService;

    @Mock
    private OfferService offerService;
    @Mock
    private IdxAPI idxApiService;
    @Mock
    private DataCampClient dataCampClient;
    @Mock
    private ShopInfoService shopInfoService;
    @Mock
    private ShopInfo shop;
    @Mock
    private MbiApiService mbiApiService;
    @Mock
    private ExecutorService reportPool;
    @Mock
    private ExecutorService feedPool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mbiApiService.isPushPartner(anyLong())).thenReturn(false);
        when(shopInfoService.getShopInfo(anyLong())).thenReturn(shop);
        TestHelper.mockExecutorService(reportPool, feedPool);
    }

    @Test
    void testAddReportOffers() throws Exception {
        Offer offer = reportOffer();
        Complaint complaint = new Complaint();
        when(offerService.findFirstWithParams(any())).thenReturn(offer);
        offerInfoService.addReportOffers(
                (c -> new ReportParam[]{ReportParam.from(ReportParam.Type.CPA, Cpa.Real)}),
                Lists.newArrayList(complaint));
        assertEquals(offer, complaint.getReportOffer());
    }

    @Test
    void testAddFeedInfo() {
        when(shop.isSmb()).thenReturn(false);
        OfferDetails offerDetails = new OfferDetails(
                new Date(), new Date(), RND.nextDouble(), RND.nextBoolean(), RND.nextInt(), "session");
        Complaint complaint = new Complaint();
        complaint.setReportOffer(reportOffer());
        when(idxApiService.findOfferWithDefaultDelivery(any(), any(), any(), Mockito.<Long>any()))
                .thenReturn(offerDetails);
        offerInfoService.addFeedInfo(Lists.newArrayList(complaint));
        assertEquals(offerDetails, complaint.getFreshOffer().getIdxOffer());
    }

    @Test
    void testAddDataCampOffer() {
        when(shop.isSmb()).thenReturn(true);
        var offer = DataCampOffer.Offer.newBuilder().build();
        when(dataCampClient.getOffer(anyLong(), any())).thenReturn(offer);
        var complaint = new Complaint();
        complaint.setReportOffer(reportOffer());
        offerInfoService.addFeedInfo(Lists.newArrayList(complaint));
        assertEquals(offer, complaint.getFreshOffer().getDataCampOffer());
    }

    private static Offer reportOffer() {
        var offer = new Offer();
        offer.setShopId(0L);
        return offer;
    }
}
