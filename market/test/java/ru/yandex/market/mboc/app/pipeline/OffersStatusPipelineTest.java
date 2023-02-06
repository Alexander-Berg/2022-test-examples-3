package ru.yandex.market.mboc.app.pipeline;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import ru.yandex.market.mbo.tracker.utils.IssueStatus;
import ru.yandex.market.mboc.app.offers.OffersController;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStrategy;
import ru.yandex.market.mboc.common.services.offers.queue.OfferQueueService;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.startrek.client.model.Issue;

import static org.junit.Assert.assertEquals;

/**
 * @author shadoff
 * created on 4/9/21
 */
public class OffersStatusPipelineTest extends BasePipelineTest {

    @Test
    public void classificationCreateTicket() {
        Offer offer = OfferTestUtils.nextOffer()
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION);
        offerRepository.insertOffer(offer);

        assertBulkLoadResponse(Offer.ProcessingStatus.IN_CLASSIFICATION, 0);

        tmsCreateTrackerTickets().accept(offer);
        Offer updated = offerRepository.getOfferById(offer.getId());
        Assert.assertNotNull(updated.getProcessingTicketId());
        Assert.assertNotNull(updated.getTrackerTicket());

        assertBulkLoadResponse(Offer.ProcessingStatus.IN_CLASSIFICATION, 1);
    }

    @Test
    public void matchingCreateTicket() {
        Offer offer = OfferTestUtils.nextOffer()
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS);
        offerRepository.insertOffer(offer);

        assertBulkLoadResponse(Offer.ProcessingStatus.IN_PROCESS, 0);

        tmsCreateTrackerTickets().accept(offer);
        Offer updated = offerRepository.getOfferById(offer.getId());
        Assert.assertNotNull(updated.getProcessingTicketId());
        Assert.assertNotNull(updated.getTrackerTicket());

        assertBulkLoadResponse(Offer.ProcessingStatus.IN_PROCESS, 1);
    }

    @Test
    public void deduplicateOffer() {
        Offer offer = OfferTestUtils.nextOffer()
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
            .setBusinessId(OfferTestUtils.TEST_SUPPLIER_ID)
            .setShopSku(SHOP_SKU);
        offerRepository.insertOffer(offer);

        assertBulkLoadResponse(Offer.ProcessingStatus.IN_PROCESS, 0);

        irPPPRemapsOffer(MARKET_SKU_ID_2,
            "ppp",
            MboMappings.ProductUpdateRequestInfo.ChangeType.DEDUPLICATION).accept(offer);

        Offer updated = offerRepository.getOfferById(offer.getId());
        Assert.assertEquals(updated.getApprovedSkuMappingConfidence(), Offer.MappingConfidence.DEDUPLICATION);

        assertBulkLoadResponse(Offer.ProcessingStatus.PROCESSED, 1);
    }

    @Test
    public void sendToWaitContent() {
        sendToCommentStatus(Offer.ProcessingStatus.WAIT_CONTENT, ContentCommentType.FOR_REVISION,
            () -> createTicket(createWaitContentTicketsQueueService, offersTrackerService::createWaitContentTickets),
            offersTrackerService::processWaitContentTickets);
    }

    @Test
    public void sendToWaitCatalog() {
        sendToCommentStatus(Offer.ProcessingStatus.WAIT_CATALOG, ContentCommentType.FOR_MANAGER,
            () -> createTicket(createWaitCatalogTicketsQueueService, offersTrackerService::createWaitCatalogTickets),
            offersTrackerService::processWaitCatalogTickets);
    }

    @Test
    public void sendToNoSizeMeasureValue() {
        sendToCommentStatus(Offer.ProcessingStatus.NO_SIZE_MEASURE_VALUE_, ContentCommentType.NO_SIZE_MEASURE_VALUE,
            () -> createTicket(createNoSizeMeasureValueTicketsQueueService,
                offersTrackerService::createNoSizeMeasureValuesTickets),
            offersTrackerService::processNoSizeMeasureValuesTickets);
    }

    @Test
    public void testAutoProcessedCategoryChanged() {
        Offer offer = OfferTestUtils.nextOffer()
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(new Offer.Mapping(MARKET_SKU_ID_1, LocalDateTime.now(), Offer.SkuType.PARTNER20))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.AUTO_PROCESSED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setMarketSpecificContentHash(1235L)
            .setMarketSpecificContentHashSent(1235L);
        offerRepository.insertOffer(offer);
        offer = offerRepository.getOfferById(offer.getId());

        Assert.assertEquals(Offer.ProcessingStatus.AUTO_PROCESSED, offer.getProcessingStatus());

        long anotherCategoryId = OfferTestUtils.TEST_CATEGORY_INFO_ID + 1;
        categoryCachingService.addCategory(OfferTestUtils
            .defaultCategory()
            .setCategoryId(anotherCategoryId)
        );

        classificationOffersProcessingService.processOffer(supplierRepository.findById(offer.getBusinessId()),
            offer, anotherCategoryId);

        offerRepository.updateOffer(offer);
        Assert.assertEquals(Offer.ProcessingStatus.CONTENT_PROCESSING, offer.getProcessingStatus());
    }

    protected static OffersProcessingStrategy.OptionalTicket createTicket(
        OfferQueueService offerQueueService,
        Function<List<Long>, OffersProcessingStrategy.OptionalTicket> ticketFunction
    ) {
        AtomicReference<OffersProcessingStrategy.OptionalTicket> ticketReference = new AtomicReference<>();
        offerQueueService.handleQueueBatch(offerIds -> ticketReference.set(
            ticketFunction.apply(offerIds)
        ));
        return ticketReference.get();
    }

    private void sendToCommentStatus(Offer.ProcessingStatus status,
                                     ContentCommentType commentType,
                                     java.util.function.Supplier<OffersProcessingStrategy.OptionalTicket> createTicket,
                                     java.util.function.Supplier<List<Offer>> processTicket) {
        Supplier supplier = supplierRepository.findById(OfferTestUtils.TEST_SUPPLIER_ID);
        supplier.setNewContentPipeline(false);
        supplierRepository.update(supplier);

        //offer has comment
        Offer offer = OfferTestUtils.nextOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .setContentComments(List.of(new ContentComment(commentType, "xz")))
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.REOPEN);
        offersProcessingStatusService.processOffers(List.of(offer));
        offerRepository.insertOffer(offer);

        Offer updated = offerRepository.getOfferById(offer.getId());
        Assert.assertNull(updated.getTrackerTicket());
        Assert.assertEquals(status, updated.getProcessingStatus());

        //create STATUS ticket
        createTicket.get();
        updated = offerRepository.getOfferById(offer.getId());
        Assert.assertNotNull(updated.getTrackerTicket());

        //process STATUS ticket
        String trackerTicket = updated.getTrackerTicket();
        Issue ticket = trackerServiceMock.getTicket(trackerTicket);
        trackerServiceMock.updateTicketStatus(ticket, IssueStatus.RESOLVED);
        processTicket.get();
        updated = offerRepository.getOfferById(offer.getId());
        Assert.assertEquals(Offer.ProcessingStatus.IN_PROCESS, updated.getProcessingStatus());
        Assertions.assertThat(updated.getContentComments()).isEmpty();

        //send to IN_PROCESS
        updated = offerRepository.getOfferById(offer.getId());
        Assert.assertNull(updated.getTrackerTicket());
        Assert.assertEquals(Offer.ProcessingStatus.IN_PROCESS, updated.getProcessingStatus());

        tmsCreateTrackerTickets().accept(offer);
        updated = offerRepository.getOfferById(offer.getId());
        Assert.assertNotNull(updated.getTrackerTicket());
    }

    @SneakyThrows
    private void assertBulkLoadResponse(Offer.ProcessingStatus status, int count) {
        MockHttpServletResponse response = new MockHttpServletResponse();
        offersController.bulkLoad(OffersController.RequestKind.JSON, null, null, null, false, List.of(status), null,
            null, null, null, response);
        assertEquals(200, response.getStatus());
        assertEquals("text/json", response.getContentType());

        if (count == 0) {
            Assertions.assertThat(response.getContentAsString()).isEmpty();
        } else {
            String[] lines = response.getContentAsString().split("\n");
            assertEquals(count, lines.length);
        }
    }
}
