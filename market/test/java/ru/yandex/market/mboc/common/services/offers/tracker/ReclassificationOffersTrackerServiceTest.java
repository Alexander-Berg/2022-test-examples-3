package ru.yandex.market.mboc.common.services.offers.tracker;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.ProcessingTicketInfo;
import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.excel.ExcelHeaders;
import ru.yandex.market.mboc.common.services.offers.queue.OfferQueueService;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.startrek.client.model.Issue;

/**
 * @author galaev@yandex-team.ru
 * @since 26/12/2018.
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ReclassificationOffersTrackerServiceTest extends BaseOffersTrackerServiceTestClass {

    @Autowired
    @Qualifier("createReclassificationTickets")
    private OfferQueueService offerQueueService;

    private List<Offer> allOffers;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        categoryCachingService.addCategories(
            new Category().setCategoryId(12).setName("Category 12").setHasKnowledge(true),
            new Category().setCategoryId(333).setName("Category 333").setHasKnowledge(true)
        );

        allOffers = YamlTestUtil.readOffersFromResources("offers/tracker-offers-for-reclassification.yml");
        offerRepository.insertOffers(allOffers);
    }

    @Test
    public void testExcelConversion() {
        List<Offer> expectedOffers = getExpectedOffers();

        Issue ticket = createTicket(offerQueueService, offersTrackerService::createReclassificationTicket);

        ExcelFile expectedExcelFile = reclassificationConverter.convert(expectedOffers);
        ExcelFile excelFile = trackerServiceMock.getHeaderExcelFile(ticket);
        MbocAssertions.assertThat(excelFile)
            .containsHeaders(ExcelHeaders.SUPPLIER_ID.getTitle(), ExcelHeaders.SUPPLIER_NAME.getTitle())
            .isEqualTo(expectedExcelFile);
    }

    @Test
    public void testCreateTicket() {
        List<Offer> expectedOffers = getExpectedOffers();

        Issue ticket = createTicket(offerQueueService, offersTrackerService::createReclassificationTicket);

        List<Long> offerIds = expectedOffers.stream().map(Offer::getId).collect(Collectors.toList());
        List<Offer> offersFromDb = offerRepository.getOffersByIds(offerIds);

        Assertions.assertThat(offersFromDb)
            .extracting(Offer::getProcessingStatus)
            .allMatch(processingStatus -> processingStatus == Offer.ProcessingStatus.IN_RECLASSIFICATION);

        Assertions.assertThat(offersFromDb)
            .extracting(Offer::getTrackerTicket)
            .allMatch(ticketKey -> ticketKey.equals(ticket.getKey()));
    }

    @Test
    public void testProcessEmptyFile() {
        Issue ticket = createTicket(offerQueueService, offersTrackerService::createReclassificationTicket);

        ExcelFile excelFile = trackerServiceMock.getHeaderExcelFile(ticket);
        ExcelFile emptyFile = new ExcelFile.Builder()
            .addHeaders(excelFile.getHeaders())
            .build();

        trackerServiceMock.commentWithAttachment(ticket, emptyFile);

        Assertions.assertThatThrownBy(() -> offersTrackerService.processReclassificationTickets())
            .isInstanceOf(OffersProcessingException.class)
            .hasMessageContaining("Failed to process offers from tickets");

        List<String> comments = trackerServiceMock.getRawComments(ticket);
        Assertions.assertThat(comments)
            .anyMatch(comment -> comment.contains("Файл не содержит записей"));
    }

    @Test
    public void testProcessOffers() {
        Issue ticket = createTicket(offerQueueService, offersTrackerService::createReclassificationTicket);

        // заполняем файл
        ExcelFile.Builder excelFile1 = trackerServiceMock.getHeaderExcelFile(ticket).toBuilder();
        setFixedCategoryId(excelFile1, 1, 12);
        setFixedCategoryId(excelFile1, 2, 333);
        setExcelFileComment(excelFile1, 3, "Comment #3", ContentCommentType.NEED_CLASSIFICATION_INFORMATION);
        excelFile1.clearLine(4);

        // комментируем с добавлением файлов
        trackerServiceMock.commentWithAttachment(ticket, excelFile1.build());

        // Запускаем обход тикетов
        List<Offer> processedOffers = offersTrackerService.processReclassificationTickets();

        // проверяем
        MbocAssertions.assertThat(processedOffers)
                .usingWithoutYtStampComparison()
                .containsExactlyInAnyOrder(
                        createReclassifiedOffer(1, 12, ticket)
                            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
                            .setProcessingCounter(1),
                        createReclassifiedOffer(2, 333, ticket)
                            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
                            .setProcessingCounter(1),
                        createProcessedOfferWithComment(3, "Comment #3", ticket)
                                .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.IN_RECLASSIFICATION)
                                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_INFO));

        // проверяем, что такие же офферы сохранены в БД
        List<Long> ids = processedOffers.stream().map(Offer::getId).collect(Collectors.toList());
        List<Offer> offersFromDb = offerRepository.getOffersByIds(ids);
        MbocAssertions.assertThat(offersFromDb)
            .containsExactlyInAnyOrderElementsOf(processedOffers);

        ProcessingTicketInfo ticketInfo = processingTicketInfoService.getByTicketTitle(ticket.getKey());

        // проверяем, что 4 оффер никак не изменился (потому что в файлах от категорийщика его не было)
        Offer offer4 = offerRepository.getOfferById(4);
        Offer expectedOffer4 = getExpectedOffer(4)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_RECLASSIFICATION)
            .addAdditionalTicket(Offer.AdditionalTicketType.RECLASSIFICATION, ticket.getKey())
            .setTrackerTicket(ticket)
            .setProcessingTicketId(ticketInfo.getId());
        MbocAssertions.assertThat(offer4)
            .isEqualTo(expectedOffer4);
    }

    private List<Offer> getExpectedOffers() {
        return offerRepository.findAll().stream()
            .filter(offer -> offer.getAcceptanceStatus() == Offer.AcceptanceStatus.OK)
            .filter(offer -> offer.getProcessingStatus() == Offer.ProcessingStatus.IN_RECLASSIFICATION)
            .filter(offer -> offer.getContentSkuMapping() == null)
            .map(Offer::new)
            .collect(Collectors.toList());
    }

    private Offer getExpectedOffer(long offerId) {
        return allOffers.stream()
            .filter(offer -> offer.getId() == offerId)
            .findFirst()
            .map(Offer::new)
            .orElseThrow(() -> new RuntimeException("Failed to find offer by id: " + offerId));
    }

    private Offer createReclassifiedOffer(long offerId, long fixedCategoryId, Issue ticket) {
        return allOffers.stream()
            .filter(offer -> offer.getId() == offerId)
            .map(Offer::new)
            .peek(offer -> {
                offer.setTicketCritical(false);
                if (!Objects.equals(offer.getCategoryId(), fixedCategoryId)) {
                    offer.setSuggestSkuMapping(null);
                }
                offer.setCategoryIdForTests(fixedCategoryId, Offer.BindingKind.APPROVED);
                offer.setContentCategoryMappingId(fixedCategoryId);
                offer.setContentCategoryMappingStatus(Offer.MappingStatus.NEW);
                if (!Objects.equals(offer.getMappedCategoryId(), fixedCategoryId)) {
                    offer.setMappedCategoryId(fixedCategoryId);
                }
                offer.updateProcessingStatusIfValid(Offer.ProcessingStatus.CLASSIFIED);
                offer.addAdditionalTicket(Offer.AdditionalTicketType.RECLASSIFICATION, ticket.getKey());
                offer.setReclassified(true);
                offer.setMappedCategoryConfidence(Offer.MappingConfidence.CONTENT);
                offer.storeOfferContent(null);
                offer.setIsOfferContentPresent(false);
            })
            .findFirst().get();
    }

    private Offer createProcessedOfferWithComment(int offerId, String comment, Issue ticket) {
        ProcessingTicketInfo processingTicketInfo = processingTicketInfoService.getByTicketTitle(ticket.getKey());
        return allOffers.stream()
            .filter(offer -> offer.getId() == offerId)
            .map(Offer::new)
            .peek(offer -> {
                offer.setTrackerTicket(ticket);
                offer.setSuggestSkuMapping(null);
                offer.setContentComments(
                    new ContentComment(ContentCommentType.NEED_CLASSIFICATION_INFORMATION, comment));
                offer.updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED);
                offer.addAdditionalTicket(Offer.AdditionalTicketType.RECLASSIFICATION, ticket.getKey());
                offer.setReclassified(true);
                offer.setProcessingTicketId(processingTicketInfo.getId());
                offer.storeOfferContent(OfferContent.builder().id(offerId).build());
                offer.storeOfferContent(null);
                offer.setIsOfferContentPresent(false);
            })
            .findFirst().get();
    }
}
