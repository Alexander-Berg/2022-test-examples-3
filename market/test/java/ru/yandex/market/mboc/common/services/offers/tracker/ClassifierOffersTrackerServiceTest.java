package ru.yandex.market.mboc.common.services.offers.tracker;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.tracker.IssueMock;
import ru.yandex.market.mbo.tracker.TrackerService;
import ru.yandex.market.mbo.tracker.utils.IssueUtils;
import ru.yandex.market.mbo.tracker.utils.TicketType;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.ProcessingTicketInfo;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.modelform.CachedModelForm;
import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStrategy;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.startrek.client.model.Issue;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.mboc.common.services.offers.tracker.OffersTrackerService.TOTAL_OFFERS;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ClassifierOffersTrackerServiceTest extends BaseOffersTrackerServiceTestClass {

    protected List<Offer> allOffers;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        categoryCachingService.addCategories(
            new Category().setCategoryId(12).setName("Category 12").setHasKnowledge(true),
            new Category().setCategoryId(333).setName("Category 333").setHasKnowledge(true)
        );

        allOffers = YamlTestUtil.readOffersFromResources("offers/tracker-offers-for-classification.yml");
        allOffers.forEach(offer -> {
            offer.setIsOfferContentPresent(true);
        });
        offerRepository.insertOffers(allOffers);
    }

    @Test
    public void testCreateTicket() {
        List<Offer> expectedUnprocessedOffers = getUnprocessedOffers(1);
        Issue ticket = createClassificationTicket(1, AUTHOR).getTicket();

        // поверяем, что созданный файл соответствует ожидаемому
        ExcelFile expectedExcelFile = classifierConverter.convert(expectedUnprocessedOffers);
        ExcelFile headerExcelFile = trackerServiceMock.getHeaderExcelFile(ticket);
        Assertions.assertThat(headerExcelFile).isEqualTo(expectedExcelFile);

        // проверяем, что у этих офферов помелись статусы и проставились айдишники тикетов
        List<Long> offerIds = expectedUnprocessedOffers.stream().map(Offer::getId).collect(Collectors.toList());
        List<Offer> offersFromDb = offerRepository.getOffersByIds(offerIds);

        Assertions.assertThat(offersFromDb)
            .extracting(Offer::getProcessingStatus)
            .allMatch(processingStatus -> processingStatus == Offer.ProcessingStatus.IN_CLASSIFICATION);

        Assertions.assertThat(offersFromDb)
            .extracting(Offer::getTrackerTicket)
            .allMatch(ticketKey -> ticketKey.equals(ticket.getKey()));
    }

    @Test
    public void testCreateTicketByOfferIds() {
        List<Offer> allUnprocessedOffers = getUnprocessedOffers(1);

        //Возьмём рандомные два оффера
        List<Offer> expectedUnprocessedOffers = allUnprocessedOffers.subList(0, 2);
        List<Long> offerIds = expectedUnprocessedOffers.stream().map(Offer::getId).collect(Collectors.toList());

        Issue ticket = createClassificationTicket(offerIds, AUTHOR).getTicket();
        // поверяем, что созданный файл соответствует ожидаемому
        ExcelFile expectedExcelFile = classifierConverter.convert(expectedUnprocessedOffers);
        ExcelFile headerExcelFile = trackerServiceMock.getHeaderExcelFile(ticket);
        Assertions.assertThat(headerExcelFile).isEqualTo(expectedExcelFile);

        // проверяем, что у этих офферов помелись статусы и проставились айдишники тикетов
        List<Offer> offersFromDb = offerRepository.getOffersByIds(offerIds);

        Assertions.assertThat(offersFromDb)
            .extracting(Offer::getProcessingStatus)
            .allMatch(processingStatus -> processingStatus == Offer.ProcessingStatus.IN_CLASSIFICATION);

        Assertions.assertThat(offersFromDb)
            .extracting(Offer::getTrackerTicket)
            .allMatch(ticketKey -> ticketKey.equals(ticket.getKey()));
    }

    @Test
    public void testProcessOffers() {
        // создаем тикеты
        Issue ticket = createClassificationTicket(1, AUTHOR).getTicket();

        // получаем "шаблоны" для заполнения данными
        ExcelFile.Builder excelFile1 = trackerServiceMock.getHeaderExcelFile(ticket).toBuilder();

        setFixedCategoryId(excelFile1, 1, 12);
        setFixedCategoryId(excelFile1, 2, 333);
        setExcelFileComment(excelFile1, 3, "Comment #3", ContentCommentType.NEED_CLASSIFICATION_INFORMATION);
        excelFile1.clearLine(4);

        // комментируем с добавлением файлов
        trackerServiceMock.commentWithAttachment(ticket, excelFile1.build());

        // Запускаем обход тикетов и обновление мапингов
        List<Offer> processedOffers = offersTrackerService.processClassificationTickets();

        // проверяем, что у офферов корректно выставлены мапинги
        MbocAssertions.assertThat(processedOffers)
            .containsExactlyInAnyOrder(
                createOfferWithFixedCategoryId(1, 12, false)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
                    .setProcessingCounter(2),
                createOfferWithFixedCategoryId(2, 333, false)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
                    .setProcessingCounter(2),
                createProcessedOfferWithComment(3, "Comment #3", ticket, false)
                    .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.IN_CLASSIFICATION)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_INFO));

        // проверяем, что такие же офферы сохранены в БД
        List<Long> ids = processedOffers.stream().map(Offer::getId).collect(Collectors.toList());
        List<Offer> offersFromDb = offerRepository.getOffersByIds(ids);
        MbocAssertions.assertThat(offersFromDb)
            .containsExactlyInAnyOrderElementsOf(processedOffers);
        ProcessingTicketInfo ticketInfo = processingTicketInfoService.getByTicketTitle(ticket.getKey());

        // проверяем, что 4 оффер никак не изменился (потому что в файлах от категорийщика его не было)
        Offer offer4 = offerRepository.getOfferById(4);
        Offer expectedOffer4 = getOffer(4)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
            .setTrackerTicket(ticket)
            .setProcessingTicketId(ticketInfo.getId());
        MbocAssertions.assertThat(offer4)
            .isEqualTo(expectedOffer4);
    }

    @Test
    public void testProccessingCommentOverridesFixedCategory() {
        // создаем тикеты
        Issue ticket = createClassificationTicket(1, AUTHOR).getTicket();

        // получаем "шаблоны" для заполнения данными
        ExcelFile.Builder excelFile1 = trackerServiceMock.getHeaderExcelFile(ticket).toBuilder();

        setFixedCategoryId(excelFile1, 1, 12);
        setExcelFileComment(excelFile1, 1, "Comment #3", ContentCommentType.NEED_CLASSIFICATION_INFORMATION);
        setFixedCategoryId(excelFile1, 2, 333);
        setExcelFileComment(excelFile1, 2, "", null);
        excelFile1.clearLine(3);
        excelFile1.clearLine(4);

        // комментируем с добавлением файлов
        trackerServiceMock.commentWithAttachment(ticket, excelFile1.build());

        // Поставм ему комментарий - надо будет проверить
        Offer offer = offerRepository.getOfferById(2L);
        offer.setContentComment("Some bad comment");
        offerRepository.updateOffers(Collections.singleton(offer));

        // Запускаем обход тикетов и обновление мапингов
        List<Offer> processedOffers = offersTrackerService.processClassificationTickets();

        // проверяем, что у офферов корректно выставлены мапинги
        processedOffers.sort(Comparator.comparing(Offer::getId));
        Assertions.assertThat(processedOffers).hasSize(2);
        Offer offer1 = processedOffers.get(0);
        Assertions.assertThat(offer1.getContentComments())
            .hasSize(1)
            .containsExactly(new ContentComment(ContentCommentType.NEED_CLASSIFICATION_INFORMATION, "Comment #3"));
        Assertions.assertThat(offer1.getBindingKind()).isEqualTo(Offer.BindingKind.SUGGESTED);

        Offer offer2 = processedOffers.get(1);
        Assertions.assertThat(offer2.getContentComment()).isNullOrEmpty();
        Assertions.assertThat(offer2.getBindingKind()).isEqualTo(Offer.BindingKind.APPROVED);
    }

    @Test
    public void testTicketWillAutoCloseIfAllOffersSuccessfullyProcessed() {
        Issue ticket = createClassificationTicket(1, AUTHOR).getTicket();

        ExcelFile.Builder excelFile1 = trackerServiceMock.getHeaderExcelFile(ticket).toBuilder();

        setFixedCategoryId(excelFile1, 1, 12);
        setFixedCategoryId(excelFile1, 2, 12);
        setFixedCategoryId(excelFile1, 3, 12);
        setFixedCategoryId(excelFile1, 4, 12);
        trackerServiceMock.commentWithAttachment(ticket, excelFile1.build());

        // Pretend it was created long ago
        ((IssueMock) ticket).setCreatedAt(Instant.now().minus(Duration.standardHours(1)));
        offersTrackerService.processClassificationTickets();

        // проверяем, что тикет перешел в статус закрыт и появился комментарий
        MbocAssertions.assertThat(trackerServiceMock.getTicket(ticket.getKey())).isClosed();
        List<String> rawComments = trackerServiceMock.getRawComments(ticket);
        String lastComment = rawComments.get(rawComments.size() - 1);
        Assertions.assertThat(lastComment).isEqualTo(TrackerService.AUTOCLOSE_COMMENT);
    }

    @Test
    public void testResolvedTicketWillClose() {
        // создаем тикеты
        Issue ticket = createClassificationTicket(1, AUTHOR).getTicket();

        // получаем "шаблоны" для заполнения данными
        ExcelFile.Builder excelFile1 = trackerServiceMock.getHeaderExcelFile(ticket).toBuilder();

        setFixedCategoryId(excelFile1, 1, 12);
        setFixedCategoryId(excelFile1, 2, 12);
        setFixedCategoryId(excelFile1, 3, 12);
        setExcelFileComment(excelFile1, 4, "Comment", ContentCommentType.NEED_CLASSIFICATION_INFORMATION);

        // комментируем с добавлением файлов
        trackerServiceMock.commentWithAttachment(ticket, excelFile1.build());

        // переводим тикет в состояние решен
        IssueUtils.resolveIssue(ticket);

        // Запускаем обход тикетов и обновление мапингов
        offersTrackerService.processClassificationTickets();

        // проверем что исходный тикет закрыт
        Issue refreshedTicket = trackerServiceMock.getTicket(ticket.getKey());
        MbocAssertions.assertThat(refreshedTicket).isClosed();
        List<String> rawComments = trackerServiceMock.getRawComments(ticket);
        String lastComment = rawComments.get(rawComments.size() - 1);
        Assertions.assertThat(lastComment).isEqualTo(TrackerService.CLOSE_COMMENT);
    }

    @Test
    public void testResolvedTicketWontCloseIfNotAllProcessed() {
        // создаем тикеты
        Issue ticket = createClassificationTicket(1, AUTHOR).getTicket();

        // получаем "шаблоны" для заполнения данными
        ExcelFile.Builder excelFile1 = trackerServiceMock.getHeaderExcelFile(ticket).toBuilder();

        setFixedCategoryId(excelFile1, 1, 12);
        setFixedCategoryId(excelFile1, 2, 12);
        setFixedCategoryId(excelFile1, 3, 12);
        excelFile1.clearLine(4);

        // комментируем с добавлением файлов
        trackerServiceMock.commentWithAttachment(ticket, excelFile1.build());

        // переводим тикет в состояние решен
        IssueUtils.resolveIssue(ticket);

        // Запускаем обход тикетов и обновление мапингов
        offersTrackerService.processClassificationTickets();

        // проверем что исходный тикет НЕ закрыт, а в статусе открыт
        Issue refreshedTicket = trackerServiceMock.getTicket(ticket.getKey());
        MbocAssertions.assertThat(refreshedTicket).isOpen();

        // проверяем, что робот отписался в тикет о том, что некорые офферы не заполнены
        List<String> rawComments = trackerServiceMock.getRawComments(refreshedTicket);
        String lastComment = rawComments.get(rawComments.size() - 2);
        Assertions.assertThat(lastComment)
            .contains("Incomplete processing");
        String closeComment = rawComments.get(rawComments.size() - 1);
        Assertions.assertThat(closeComment)
            .contains("Робот переоткрыл тикет");

        // Проверяем, что НЕ создан тикет на матчинг
        Optional<Issue> matchingTicket = trackerServiceMock.fetchOpenTickets(TicketType.MATCHING).stream().findFirst();
        Assertions.assertThat(matchingTicket).isEmpty();
    }

    @Test
    public void testResolvedTicketWontCloseIfNotAllProcessedBecauseOfError() {
        // создаем тикеты
        Issue ticket = createClassificationTicket(1, AUTHOR).getTicket();

        // получаем "шаблоны" для заполнения данными
        ExcelFile.Builder excelFile1 = trackerServiceMock.getHeaderExcelFile(ticket).toBuilder();

        setFixedCategoryId(excelFile1, 1, 12);
        setFixedCategoryId(excelFile1, 2, 12);
        setFixedCategoryId(excelFile1, 3, 12);
        excelFile1.setValue(4, 0, "four");

        // комментируем с добавлением файлов
        trackerServiceMock.commentWithAttachment(ticket, excelFile1.build());

        // переводим тикет в состояние решен
        IssueUtils.resolveIssue(ticket);

        // Запускаем обход тикетов и обновление мапингов
        Assertions.assertThat(offersTrackerService.processClassificationTickets()).isEmpty();

        // проверем что исходный тикет НЕ закрыт, а в статусе открыт
        Issue refreshedTicket = trackerServiceMock.getTicket(ticket.getKey());
        MbocAssertions.assertThat(refreshedTicket).isOpen();

        // проверяем, что робот отписался в тикет о том, что не смог распарсить значение
        List<String> rawComments = trackerServiceMock.getRawComments(refreshedTicket);
        String errorComment = rawComments.get(rawComments.size() - 2);
        Assertions.assertThat(errorComment)
            .contains("Unsuccessfully retrieval")
            .contains("Ошибка на строке 5: Не получилось распарсить значение 'four' в столбце 'Offer_id'");
        String closeComment = rawComments.get(rawComments.size() - 1);
        Assertions.assertThat(closeComment)
            .contains("Робот переоткрыл тикет");

        // Проверяем, что НЕ создан тикет на матчинг
        Optional<Issue> matchingTicket = trackerServiceMock.fetchOpenTickets(TicketType.MATCHING).stream().findFirst();
        Assertions.assertThat(matchingTicket).isEmpty();
    }

    @Test
    public void testClassifiedCategoryWithoutKnowledgeWillBeInNoKnowledgeState() {
        // мокируем категории в которых есть знания
        Mockito.when(modelFormService.getPublishedModelForms(Mockito.anyCollection()))
            .then(i -> {
                Collection<Long> categoryIds = i.getArgument(0);
                return categoryIds.stream().distinct()
                    .filter(id -> id == 12)
                    .map(id -> new CachedModelForm(id, true))
                    .collect(Collectors.toMap(CachedModelForm::getCategoryId, Function.identity()));
            });
        categoryCachingService.setCategoryHasKnowledge(12, true);
        categoryCachingService.setCategoryHasKnowledge(333, false);

        Issue ticket = createClassificationTicket(1, AUTHOR).getTicket();

        ExcelFile.Builder excelFile1 = trackerServiceMock.getHeaderExcelFile(ticket).toBuilder();
        setFixedCategoryId(excelFile1, 1, 12);
        setFixedCategoryId(excelFile1, 2, 12);
        setFixedCategoryId(excelFile1, 3, 333);
        setFixedCategoryId(excelFile1, 4, 333);

        trackerServiceMock.commentWithAttachment(ticket, excelFile1.build());

        // Запускаем обход тикетов
        Map<Long, Offer> processedOffers = offersTrackerService.processClassificationTickets().stream()
            .collect(Collectors.toMap(Offer::getId, Function.identity()));

        MbocAssertions.assertThat(processedOffers.get(1L))
            .isEqualTo(createOfferWithFixedCategoryId(1, 12, false)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
                .setProcessingCounter(2));
        MbocAssertions.assertThat(processedOffers.get(2L))
            .isEqualTo(createOfferWithFixedCategoryId(2, 12, false)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
                .setProcessingCounter(2));
        MbocAssertions.assertThat(processedOffers.get(3L))
            .isEqualTo(createOfferWithFixedCategoryId(3, 333, false)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_KNOWLEDGE));
        MbocAssertions.assertThat(processedOffers.get(4L))
            .isEqualTo(createOfferWithFixedCategoryId(4, 333, false)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_KNOWLEDGE));
    }

    @Test
    public void testTicketCustomField() {
        Issue ticket = createClassificationTicket(1, AUTHOR).getTicket();
        long expectedOffers = allOffers.stream().filter(o -> o.getBusinessId() == 1).count();
        assertEquals((int) expectedOffers, ((IssueMock) ticket).getCustomField(TOTAL_OFFERS));
    }

    private Offer createOfferForClassificationTicket(long id,
                                                     Supplier supplier,
                                                     Offer.BindingKind bindingKind,
                                                     Offer.ProcessingStatus processingStatus) {
        return new Offer().setId(id)
            .setProcessingStatusInternal(processingStatus)
            .setBusinessId(supplier.getId())
            .setShopSku("ssku" + id)
            .setTitle("title" + id)
            .setShopCategoryName("category name")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .addNewServiceOfferIfNotExistsForTests(supplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setBindingKind(bindingKind);
    }

    private Offer createOfferWithFixedCategoryId(long offerId, long fixedCategoryId,
                                                 Boolean withContent) {
        return allOffers.stream()
            .filter(offer -> offer.getId() == offerId)
            .map(Offer::new)
            .peek(offer -> {
                offer.setTicketCritical(false);
                if (!Objects.equals(offer.getCategoryId(), fixedCategoryId)) {
                    offer.setSuggestSkuMapping(null);
                }
                offer.setCategoryIdForTests(fixedCategoryId, Offer.BindingKind.APPROVED);
                if (!Objects.equals(offer.getMappedCategoryId(), fixedCategoryId)) {
                    offer.setMappedCategoryId(fixedCategoryId);
                }
                offer.updateProcessingStatusIfValid(Offer.ProcessingStatus.CLASSIFIED);
                offer.setMappedCategoryConfidence(Offer.MappingConfidence.CONTENT);
                offer.setContentCategoryMappingId(fixedCategoryId);
                offer.setContentCategoryMappingStatus(Offer.MappingStatus.NEW);
                if (!withContent) {
                    offer.storeOfferContent(null);
                    offer.setIsOfferContentPresent(false);
                }
            })
            .findFirst().get();
    }

    private Offer createProcessedOfferWithComment(int offerId, String comment, Issue ticket, Boolean withContent) {
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
                offer.setProcessingTicketId(processingTicketInfo.getId());
                if (!withContent) {
                    offer.storeOfferContent(null);
                    offer.setIsOfferContentPresent(false);
                }
            })
            .findFirst().get();
    }

    private Offer getOffer(int offerId) {
        return allOffers.stream()
            .peek(offer -> {
                offer.storeOfferContent(offer.getOfferContentBuilder().id(offerId).build());
            })
            .filter(offer -> offer.getId() == offerId)
            .findFirst()
            .map(Offer::new)
            .orElseThrow(() -> new RuntimeException("Failed to find offer by id: " + offerId));
    }

    private List<Offer> getUnprocessedOffers(int... supplierId) {
        Set<Integer> supplierIds = Arrays.stream(supplierId).boxed().collect(Collectors.toSet());
        return allOffers.stream()
            .filter(offer -> offer.getAcceptanceStatus() == Offer.AcceptanceStatus.OK)
            .filter(offer -> offer.getProcessingStatus() == Offer.ProcessingStatus.IN_CLASSIFICATION)
            .filter(offer -> offer.getContentSkuMapping() == null)
            .filter(offer -> supplierIds.contains(offer.getBusinessId()))
            .map(Offer::new)
            .collect(Collectors.toList());
    }

    private OffersProcessingStrategy.OptionalTicket createClassificationTicket(int supplierId,
                                                                               @Nullable String author) {
        return classificationOffersProcessingStrategy.createTrackerTicketOrThrow(
            new OffersFilter().addBusinessId(supplierId), author);
    }

    public OffersProcessingStrategy.OptionalTicket createClassificationTicket(Collection<Long> offerIds,
                                                                              @Nullable String author) {
        return classificationOffersProcessingStrategy.createTrackerTicketOrThrow(
            new OffersFilter().setOfferIds(offerIds), author);
    }
}
