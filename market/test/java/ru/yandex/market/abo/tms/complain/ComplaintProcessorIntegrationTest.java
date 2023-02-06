package ru.yandex.market.abo.tms.complain;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.api.entity.complaint.ComplaintType;
import ru.yandex.market.abo.core.complain.ComplaintManager;
import ru.yandex.market.abo.core.complain.model.Complaint;
import ru.yandex.market.abo.core.complain.service.ComplaintService;
import ru.yandex.market.abo.core.complain.service.MailComplaintService;
import ru.yandex.market.abo.core.datacamp.client.DataCampClient;
import ru.yandex.market.abo.core.hiding.util.HiddenOffersNotifier;
import ru.yandex.market.abo.core.hiding.util.OfferInfoService;
import ru.yandex.market.abo.core.hiding.util.filter.ComplaintOfferFilterService;
import ru.yandex.market.abo.core.hiding.util.model.CheckStatus;
import ru.yandex.market.abo.core.hiding.util.model.FreshOfferWrapper;
import ru.yandex.market.abo.core.indexer.Generation;
import ru.yandex.market.abo.core.indexer.GenerationService;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.offer.report.OfferService;
import ru.yandex.market.abo.core.shop.ShopInfo;
import ru.yandex.market.abo.core.shop.ShopInfoService;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.abo.test.TestHelper;
import ru.yandex.market.common.report.indexer.IdxAPI;
import ru.yandex.market.common.report.indexer.model.OfferDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * created on 27.10.15.
 */
class ComplaintProcessorIntegrationTest extends EmptyTest {

    @Autowired
    NewComplaintProcessor newComplaintProcessor;
    @Autowired
    RemovedComplaintProcessor removedComplaintProcessor;
    @Autowired
    WaitingComplaintProcessor waitingComplaintProcessor;

    @InjectMocks
    private OfferInfoService offerInfoService;
    @Mock
    private DataCampClient dataCampClient;
    @Mock
    private MbiApiService mbiApiService;
    @Mock
    private OfferService offerService;
    @Mock
    private IdxAPI idxApiService;
    @Mock
    private ShopInfoService shopInfoService;
    @Mock
    private ComplaintService complaintService;
    @Mock
    private ExecutorService reportPool;
    @Mock
    private ExecutorService feedPool;
    private Map<ComplaintProcessor, CheckStatus> processorToStatusMap;
    private Date oldFeedCheckDate;

    private static final Date COMPLAINT_DATE = DateUtils.addMinutes(NOW, -10);
    private static final Date ACTUAL = DateUtils.addMinutes(COMPLAINT_DATE, 1);
    private static final Date OUTDATED = DateUtils.addMinutes(COMPLAINT_DATE, -1);
    private static final Date VERY_OUTDATED = DateUtils.addHours(COMPLAINT_DATE, -1);

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        when(mbiApiService.isPushPartner(anyLong())).thenReturn(false);
        oldFeedCheckDate = OUTDATED;

        var shop = mock(ShopInfo.class);
        when(shop.isSmb()).thenReturn(false);
        when(shopInfoService.getShopInfo(anyLong())).thenReturn(shop);

        ComplaintManager complaintManager = new ComplaintManager();
        GenerationService generationService = mock(GenerationService.class);
        Generation generation = mock(Generation.class);
        when(generation.getReleaseDate()).thenReturn(new Timestamp(new Date().getTime()));
        when(generationService.loadPrevReleaseGeneration()).thenReturn(generation);
        complaintManager.setComplaintService(complaintService);
        ComplaintOfferFilterService offerFilterService = new ComplaintOfferFilterService();
        offerFilterService.setHideOffersDBService(complaintService);
        offerFilterService.setOfferInfoService(offerInfoService);
        complaintManager.setOfferFilterService(offerFilterService);


        MailComplaintService mailComplaintService = new MailComplaintService();
        MbiApiService mbiApiService = mock(MbiApiService.class);
        when(mbiApiService.sendMessageToShop(anyLong(), anyInt(), any())).thenReturn(true);
        HiddenOffersNotifier hiddenOffersNotifier = new HiddenOffersNotifier();
        hiddenOffersNotifier.setMbiApiService(mbiApiService);
        hiddenOffersNotifier.setGenerationService(generationService);
        mailComplaintService.setComplaintService(complaintService);
        mailComplaintService.setHiddenOffersNotifier(hiddenOffersNotifier);

        newComplaintProcessor.setComplaintManager(complaintManager);
        newComplaintProcessor.setComplaintService(complaintService);
        waitingComplaintProcessor.setComplaintManager(complaintManager);
        waitingComplaintProcessor.setComplaintService(complaintService);
        removedComplaintProcessor.setComplaintManager(complaintManager);
        removedComplaintProcessor.setComplaintService(complaintService);
        removedComplaintProcessor.setMailComplaintService(mailComplaintService);

        processorToStatusMap = new HashMap<>();
        processorToStatusMap.put(newComplaintProcessor, CheckStatus.NEW);
        processorToStatusMap.put(removedComplaintProcessor, CheckStatus.OFFER_REMOVED);
        processorToStatusMap.put(waitingComplaintProcessor, CheckStatus.WAIT_FOR_FEED);

        TestHelper.mockExecutorService(reportPool, feedPool);
    }

    @Test
    void testHasActualFeed() {
        Date createDate = new Date();
        OfferDetails offerDetails = new OfferDetails(createDate, null, 0, false, 1, "");
        Complaint complaint = initComplaint(1);
        complaint.setFreshOffer(new FreshOfferWrapper(offerDetails));

        complaint.setCreateTime(createDate);
        assertFalse(complaint.hasActualFeed());
        complaint.setCreateTime(DateUtils.addMinutes(createDate, -31));
        assertTrue(complaint.hasActualFeed());
    }

    @FunctionalInterface
    private interface ComplaintProcessorInitializer {
        void init(boolean diffFound, boolean actual, ComplaintProcessor complaintProcessor) throws Exception;
    }

    @FunctionalInterface
    private interface ComplaintCheck {
        void check(ComplaintProcessorInitializer initializer) throws Exception;
    }

    // find the difference between feed and report -> remove offer.
    private ComplaintCheck diffFound_OutdatedFeed_NewProcessor = initializer ->
            check(initializer, true, false, newComplaintProcessor, CheckStatus.OFFER_REMOVED, true);

    // find the difference between feed and report -> remove offer. The fact that feed is outdated doesn't matter.
    private ComplaintCheck diffFound_ActualFeed_NewProcessor = initializer ->
            check(initializer, true, true, newComplaintProcessor, CheckStatus.OFFER_REMOVED, true);

    // difference didn't found and feed is outdated -> wait actual feed
    private ComplaintCheck diffDoesntFound_OutdatedFeed_NewProcessor = initializer ->
            check(initializer, false, false, newComplaintProcessor, CheckStatus.WAIT_FOR_FEED);

    // difference didn't found and feed is actual -> ticket
    private ComplaintCheck diffDoesntFound_ActualFeed_NewProcessor = initializer ->
            check(initializer, false, true, newComplaintProcessor, CheckStatus.GENERATE_TICKET);

    // The difference found between removed offer and report -> remove
    private ComplaintCheck diffFound_AnyFeed_RemovedProcessor = initializer -> {
        check(initializer, true, false, removedComplaintProcessor, CheckStatus.OFFER_REMOVED);
        check(initializer, true, true, removedComplaintProcessor, CheckStatus.OFFER_REMOVED);
    };

    // There is no difference between removed offer and report -> return
    private ComplaintCheck diffDoesntFound_AnyFeed_RemovedProcessor = initializer -> {
        check(initializer, false, false, removedComplaintProcessor, CheckStatus.OFFER_RETURNED);
        check(initializer, false, true, removedComplaintProcessor, CheckStatus.OFFER_RETURNED);
    };

    // There is no difference between offer and report but feed is a bit outdated
    private ComplaintCheck diffDoesntFound_OutdatedFeed_WaitingProcessor = initializer ->
            check(initializer, false, false, waitingComplaintProcessor, CheckStatus.WAIT_FOR_FEED);

    // There is no difference between offer and report but feed is very outdated
    private ComplaintCheck diffDoesntFound_VeryOutdatedFeed_WaitingProcessor = initializer ->
            check(initializer, false, false, waitingComplaintProcessor, CheckStatus.GENERATE_TICKET);

    // There is no difference between offer and report, feed is actual -> ticket
    private ComplaintCheck diffDoesntFound_ActualFeed_WaitingProcessor = initializer ->
            check(initializer, false, true, waitingComplaintProcessor, CheckStatus.GENERATE_TICKET);

    // Found the difference between offer and report, feed is actual -> remove
    private ComplaintCheck diffFound_ActualFeed_WaitingProcessor = initializer ->
            check(initializer, true, true, waitingComplaintProcessor, CheckStatus.OFFER_REMOVED);

    @Test
    void absent_OutdatedFeed_NewProcessor() throws Exception {
        test(this::initAbsenceCheck, diffFound_OutdatedFeed_NewProcessor);
    }

    @Test
    void absent_ActualFeed_NewProcessor() throws Exception {
        test(this::initAbsenceCheck, diffFound_ActualFeed_NewProcessor);
    }

    @Test
    void present_OutdatedFeed_NewProcessor() throws Exception {
        test(this::initAbsenceCheck, diffDoesntFound_OutdatedFeed_NewProcessor);
    }

    @Test
    void present_ActualFeed_NewProcessor() throws Exception {
        test(this::initAbsenceCheck, diffDoesntFound_ActualFeed_NewProcessor);
    }

    @Test
    void absent_AnyFeed_RemovedProcessor() throws Exception {
        test(this::initAbsenceCheck, diffFound_AnyFeed_RemovedProcessor);
    }

    @Test
    void present_AnyFeed_RemovedProcessor() throws Exception {
        test(this::initAbsenceCheck, diffFound_AnyFeed_RemovedProcessor);
    }

    @Test
    void present_diffDoesntFound_AnyFeed_RemovedProcessor() throws Exception {
        test(this::initAbsenceCheck, diffDoesntFound_AnyFeed_RemovedProcessor);
    }

    @Test
    void present_OutdatedFeed_WaitingProcessor() throws Exception {
        test(this::initAbsenceCheck, diffDoesntFound_OutdatedFeed_WaitingProcessor);
    }

    @Test
    void present_ActualFeed_WaitingProcessor() throws Exception {
        test(this::initAbsenceCheck, diffDoesntFound_ActualFeed_WaitingProcessor);
    }

    @Test
    void absent_ActualFeed_WaitingProcessor() throws Exception {
        test(this::initAbsenceCheck, diffFound_ActualFeed_WaitingProcessor);
    }

    @Test
    void absent_OutdatedFeed_WaitingProcessor_too_long() throws Exception {
        oldFeedCheckDate = VERY_OUTDATED;
        test(this::initAbsenceCheck, diffDoesntFound_VeryOutdatedFeed_WaitingProcessor);
    }

    // prices tests

    @Test
    void diffPrices_OutdatedFeed_NewProcessor() throws Exception {
        test(this::initPriceCheck, diffFound_OutdatedFeed_NewProcessor);
    }

    @Test
    void diffPrices_ActualFeed_NewProcessor() throws Exception {
        test(this::initPriceCheck, diffFound_ActualFeed_NewProcessor);
    }

    @Test
    void equalPrices_OutdatedFeed_NewProcessor() throws Exception {
        test(this::initPriceCheck, diffDoesntFound_OutdatedFeed_NewProcessor);
    }

    @Test
    void equalPrices_ActualFeed_NewProcessor() throws Exception {
        test(this::initPriceCheck, diffDoesntFound_ActualFeed_NewProcessor);
    }

    @Test
    void diffPrices_AnyFeed_RemovedProcessor() throws Exception {
        test(this::initPriceCheck, diffFound_AnyFeed_RemovedProcessor);
    }

    @Test
    void equalPrices_AnyFeed_RemovedProcessor() throws Exception {
        test(this::initPriceCheck, diffFound_AnyFeed_RemovedProcessor);
    }

    @Test
    void equalPrices_diffDoesntFound_AnyFeed_RemovedProcessor() throws Exception {
        test(this::initPriceCheck, diffDoesntFound_AnyFeed_RemovedProcessor);
    }

    @Test
    void equalPrices_OutdatedFeed_WaitingProcessor() throws Exception {
        test(this::initPriceCheck, diffDoesntFound_OutdatedFeed_WaitingProcessor);
    }

    @Test
    void equalPrices_ActualFeed_WaitingProcessor() throws Exception {
        test(this::initPriceCheck, diffDoesntFound_ActualFeed_WaitingProcessor);
    }

    @Test
    void diffPrices_ActualFeed_WaitingProcessor() throws Exception {
        test(this::initPriceCheck, diffFound_ActualFeed_WaitingProcessor);
    }

    @Test
    void diffPrices_OutdatedFeed_WaitingProcessor_too_long() throws Exception {
        oldFeedCheckDate = VERY_OUTDATED;
        test(this::initPriceCheck, diffDoesntFound_VeryOutdatedFeed_WaitingProcessor);
    }

    // onStock tests
    @Test
    void doesntAvailable_OutdatedFeed_NewProcessor() throws Exception {
//        todo disabled for a while, reconsider logic in MARKETASSESSOR-6746
//        test(this::initOnStockCheck, diffFound_OutdatedFeed_NewProcessor);
    }

    @Test
    void doesntAvailable_ActualFeed_NewProcessor() throws Exception {
//        todo disabled for a while, reconsider logic in MARKETASSESSOR-6746
//        test(this::initOnStockCheck, diffFound_ActualFeed_NewProcessor);
    }

    @Test
    void onStock_OutdatedFeed_NewProcessor() throws Exception {
        test(this::initOnStockCheck, diffDoesntFound_OutdatedFeed_NewProcessor);
    }

    @Test
    void onStock_ActualFeed_NewProcessor() throws Exception {
        test(this::initOnStockCheck, diffDoesntFound_ActualFeed_NewProcessor);
    }

    @Test
    void doesntAvailable_AnyFeed_RemovedProcessor() throws Exception {
//        todo disabled for a while, reconsider logic in MARKETASSESSOR-6746
//        test(this::initOnStockCheck, diffFound_AnyFeed_RemovedProcessor);
    }

    @Test
    void onStock_AnyFeed_RemovedProcessor() throws Exception {
//        todo disabled for a while, reconsider logic in MARKETASSESSOR-6746
//        test(this::initOnStockCheck, diffFound_AnyFeed_RemovedProcessor);
    }

    @Test
    void onStock_diffDoesntFound_AnyFeed_RemovedProcessor() throws Exception {
        test(this::initOnStockCheck, diffDoesntFound_AnyFeed_RemovedProcessor);
    }

    @Test
    void onStock_OutdatedFeed_WaitingProcessor() throws Exception {
        test(this::initOnStockCheck, diffDoesntFound_OutdatedFeed_WaitingProcessor);
    }

    @Test
    void onStock_ActualFeed_WaitingProcessor() throws Exception {
        test(this::initOnStockCheck, diffDoesntFound_ActualFeed_WaitingProcessor);
    }

    @Test
    void doesntAvailable_ActualFeed_WaitingProcessor() throws Exception {
//        todo disabled for a while, reconsider logic in MARKETASSESSOR-6746
//        test(this::initOnStockCheck, diffFound_ActualFeed_WaitingProcessor);
    }

    @Test
    void onStock_OutdatedFeed_WaitingProcessor_too_long() throws Exception {
        oldFeedCheckDate = VERY_OUTDATED;
        test(this::initOnStockCheck, diffDoesntFound_VeryOutdatedFeed_WaitingProcessor);
    }

    private void test(ComplaintProcessorInitializer initializer, ComplaintCheck check) throws Exception {
        check.check(initializer);
    }

    private void check(ComplaintProcessorInitializer initializer,
                       boolean diffFound,
                       boolean actualFeed,
                       ComplaintProcessor complaintProcessor,
                       CheckStatus checkStatus) throws Exception {
        check(initializer, diffFound, actualFeed, complaintProcessor, checkStatus, false);
    }

    private void check(ComplaintProcessorInitializer initializer,
                       boolean diffFound,
                       boolean actualFeed,
                       ComplaintProcessor complaintProcessor,
                       CheckStatus checkStatus,
                       boolean remove) throws Exception {
        initializer.init(diffFound, actualFeed, complaintProcessor);
        checkLastUpdatedComplaintStatus(checkStatus);
        if (remove) {
            checkLastUpdatedComplaintRemoveDate();
        }
    }

    private void initAbsenceCheck(boolean absent, boolean actual, ComplaintProcessor processor) throws Exception {
        Complaint complaint = initComplaint(0);
        complaint.setComplaintType(ComplaintType.SITE);
        complaint.setCheckStatus(processorToStatusMap.get(processor));
        when(complaintService.findComplaints(any(), anyBoolean())).thenReturn(new ArrayList<>(Collections.singletonList(complaint)));
        Offer offer = initOffer();
        when(offerService.findFirstWithParams(any())).thenReturn(offer);
        OfferDetails offerDetails = initOfferDetails(actual ? ACTUAL : oldFeedCheckDate);
        doReturn(absent ? null : offerDetails).when(idxApiService).findOfferWithDefaultDelivery(any(), any(), any(), Mockito.<Long>any());

        processor.processComplaints();
    }

    private void initPriceCheck(boolean priceDiff, boolean actual, ComplaintProcessor processor) throws Exception {
        double OFFER_PRICE = 100.0;
        double FEED_PRICE = 101.1;
        Complaint complaint = initComplaint(0);
        complaint.setComplaintType(ComplaintType.PRICE);
        complaint.setCheckStatus(processorToStatusMap.get(processor));
        when(complaintService.findComplaints(any(), anyBoolean())).thenReturn(new ArrayList<>(Collections.singletonList(complaint)));
        Offer offer = initOffer();
        offer.setPrice(new BigDecimal(OFFER_PRICE));
        when(offerService.findFirstWithParams(any())).thenReturn(offer);
        OfferDetails offerDetails = initOfferDetails(actual ? ACTUAL : oldFeedCheckDate);
        when(offerDetails.getPrice()).thenReturn(priceDiff ? FEED_PRICE : OFFER_PRICE);
        when(idxApiService.findOfferWithDefaultDelivery(any(), any(), any(), Mockito.<Long>any())).thenReturn(offerDetails);

        processor.processComplaints();
    }

    private void initOnStockCheck(boolean onStockDiff, boolean actual, ComplaintProcessor processor) throws Exception {
        Complaint complaint = initComplaint(0);
        complaint.setComplaintType(ComplaintType.AVAILABILITY);
        complaint.setCheckStatus(processorToStatusMap.get(processor));
        when(complaintService.findComplaints(any(), anyBoolean())).thenReturn(new ArrayList<>(Collections.singletonList(complaint)));
        Offer offer = initOffer();
        offer.setOnStock(true);
        when(offerService.findFirstWithParams(any())).thenReturn(offer);
        OfferDetails offerDetails = initOfferDetails(actual ? ACTUAL : oldFeedCheckDate);
        when(offerDetails.getAvailable()).thenReturn(!onStockDiff);
        when(idxApiService.findOfferWithDefaultDelivery(any(), any(), any(), Mockito.<Long>any())).thenReturn(offerDetails);

        processor.processComplaints();
    }

    private void checkLastUpdatedComplaintStatus(CheckStatus status) {
        assertEquals(status, getLastUpdatedComplaints().get(0).getCheckStatus());
    }

    private void checkLastUpdatedComplaintRemoveDate() {
        assertNotNull(getLastUpdatedComplaints().get(0).getOfferRemovedDate());
    }

    private OfferDetails initOfferDetails(Date date) {
        OfferDetails offerDetails = initOfferDetails();
        when(offerDetails.getLastChecked()).thenReturn(date);
        return offerDetails;
    }

    private OfferDetails initOfferDetails() {
        return mock(OfferDetails.class);
    }

    private List<Complaint> getLastUpdatedComplaints() {
        List<List> res = getAllUpdatedComplaints();
        return res.get(res.size() - 1);
    }

    private List<List> getAllUpdatedComplaints() {
        ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
        verify(complaintService, atLeastOnce()).updateTillNextGen(argument.capture());
        return argument.getAllValues().stream().filter(list -> list.size() > 0).collect(Collectors.toList());
    }

    private Offer initOffer() {
        Offer offer = new Offer();
        offer.setShopId(0L);
        offer.setBaseGeneration("20330324_0327");
        offer.setClassifierMagicId("magic_id");
        return offer;
    }

    private Complaint initComplaint(int id) {
        Complaint complaint = new Complaint();
        complaint.setWareMd5(String.valueOf(id));
        complaint.setId(id);
        complaint.setCreateTime(COMPLAINT_DATE);
        complaint.setOfferRemovedDate(new Date(0));
        return complaint;
    }
}
