package ru.yandex.market.mboc.common.services.offers.tracker;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.tracker.IssueMock;
import ru.yandex.market.mbo.tracker.TrackerService;
import ru.yandex.market.mbo.tracker.utils.IssueStatus;
import ru.yandex.market.mbo.tracker.utils.IssueUtils;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.ProcessingTicketInfo;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoService;
import ru.yandex.market.mboc.common.processingticket.TicketFilter;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.mbousers.models.MboUser;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStrategy;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStrategy.OptionalTicket;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.MbocComparators;
import ru.yandex.startrek.client.model.Issue;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static ru.yandex.market.mboc.common.services.offers.tracker.OffersTrackerService.TOTAL_OFFERS;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class MatchingOffersTrackerServiceTest extends BaseOffersTrackerServiceTestClass {

    protected List<Offer> allOffers;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        categoryCachingService.addCategory(new Category().setCategoryId(10).setName("Category 10"));
        categoryCachingService.addCategory(new Category().setCategoryId(12).setName("Category 12"));
        categoryCachingService.addCategory(new Category().setCategoryId(13).setName("Category 13"));
        categoryCachingService.addCategory(new Category().setCategoryId(20).setName("Category 20"));

        allOffers = YamlTestUtil.readOffersFromResources("offers/tracker-offers-for-matching.yml");
        allOffers.forEach(it -> it.setIsOfferContentPresent(true));
        offerRepository.insertOffers(allOffers);
    }

    @Test
    public void testCreateTicket() {
        List<Offer> expectedUnprocessedOffers = getUnprocessedOffers(1);
        Issue ticket = createMatchingTicket(1, AUTHOR).getTicket();

        // поверяем, что созданный файл соответствует ожидаемому
        ExcelFile expectedExcelFile = matchingConverter.convert(expectedUnprocessedOffers);
        ExcelFile headerExcelFile = trackerServiceMock.getHeaderExcelFile(ticket);
        Assertions.assertThat(headerExcelFile).isEqualTo(expectedExcelFile);

        // проверяем, что у этих офферов помелись статусы и проставились айдишники тикетов
        List<Long> offerIds = expectedUnprocessedOffers.stream().map(Offer::getId).collect(toList());
        List<Offer> offersFromDb = offerRepository.getOffersByIds(offerIds);

        Assertions.assertThat(offersFromDb)
            .extracting(Offer::getProcessingStatus)
            .allMatch(processingStatus -> processingStatus == ProcessingStatus.IN_PROCESS);

        Assertions.assertThat(offersFromDb)
            .extracting(Offer::getTrackerTicket)
            .allMatch(ticketKey -> ticketKey.equals(ticket.getKey()));

        checkProcessingTicketCreated(ticket, offersFromDb);
    }

    @Test
    public void testCreateTicketByOfferIds() {
        List<Offer> allUnprocessedOffers = getUnprocessedOffers(1);

        //Возьмём рандомные два оффера
        List<Offer> expectedUnprocessedOffers = allUnprocessedOffers.subList(0, 2);
        List<Long> offerIds = expectedUnprocessedOffers.stream().map(Offer::getId).collect(Collectors.toList());

        Issue ticket = createMatchingTicket(offerIds, AUTHOR).getTicket();
        // поверяем, что созданный файл соответствует ожидаемому
        ExcelFile expectedExcelFile = matchingConverter.convert(expectedUnprocessedOffers);
        ExcelFile headerExcelFile = trackerServiceMock.getHeaderExcelFile(ticket);
        Assertions.assertThat(headerExcelFile).isEqualTo(expectedExcelFile);

        // проверяем, что у этих офферов помелись статусы и проставились айдишники тикетов
        List<Offer> offersFromDb = offerRepository.getOffersByIds(offerIds);

        Assertions.assertThat(offersFromDb)
            .extracting(Offer::getProcessingStatus)
            .allMatch(processingStatus -> processingStatus == ProcessingStatus.IN_PROCESS);

        Assertions.assertThat(offersFromDb)
            .extracting(Offer::getTrackerTicket)
            .allMatch(ticketKey -> ticketKey.equals(ticket.getKey()));

        checkProcessingTicketCreated(ticket, offersFromDb);
    }

    @Test
    public void testNoCreatedTicketForEmptySupplier() {
        OptionalTicket ticket = createMatchingTicket(12345, AUTHOR);
        Assertions.assertThat(ticket.hasTicket()).isFalse();
        Assertions.assertThat(processingTicketInfoService.find(new TicketFilter())).isEmpty();
    }

    @Test
    public void testCreateSeveralHeaderFilesSplitOnManagers() {
        // задаем менеджеров категории
        mboUsersRepository.insert(new MboUser(14, "Сережка", "s-ermakov"));
        mboUsersRepository.insert(new MboUser(200, "User", "user"));

        CategoryInfo categoryInfo = new CategoryInfo();
        categoryInfo.setCategoryId(10);
        categoryInfo.setInputManagerUid(14L);
        categoryInfoRepository.insert(categoryInfo);

        // создаем тикет
        Issue ticket = createMatchingTicket(1, AUTHOR).getTicket();

        // проверяем, что разбиение прошло корректно
        List<ExcelFile> headerExcelFiles1 = trackerServiceMock.getHeaderExcelFiles(ticket);
        List<ExcelFile> expectedFiles = Arrays.asList(
            matchingConverter.convert(Arrays.asList(getOffer(1), getOffer(2))),
            matchingConverter.convert(Arrays.asList(getOffer(3)))
        );

        Assertions.assertThat(headerExcelFiles1).isEqualTo(expectedFiles);
    }

    @Test
    public void testCreateSecondTicketForMoreOffers() {
        List<Offer> unprocessedOffers1 = getUnprocessedOffers(1);
        OptionalTicket firstTry = createMatchingTicket(1, AUTHOR);
        Assertions.assertThat(firstTry.hasTicket()).isTrue();

        Offer newOffer = getOffer(3);
        newOffer.setId(10).setShopSku("sku10-new");
        offerRepository.insertOffer(newOffer);

        OptionalTicket secondTry = createMatchingTicket(1, AUTHOR);
        Assertions.assertThat(secondTry.hasTicket()).isTrue();

        ExcelFile headerExcelFile1 = trackerServiceMock.getHeaderExcelFile(firstTry.getTicket());
        ExcelFile expectedExcelFile1 = matchingConverter.convert(unprocessedOffers1);
        Assertions.assertThat(headerExcelFile1).isEqualTo(expectedExcelFile1);

        ExcelFile headerExcelFile2 = trackerServiceMock.getHeaderExcelFile(secondTry.getTicket());
        ExcelFile expectedExcelFile2 = matchingConverter.convert(singletonList(newOffer));
        Assertions.assertThat(headerExcelFile2).isEqualTo(expectedExcelFile2);
    }

    @Test
    public void testProcessOffers() {
        auditServiceMock.reset();
        // создаем тикеты
        Issue ticket1 = createMatchingTicket(1, AUTHOR).getTicket();
        Issue ticket2 = createMatchingTicket(2, AUTHOR).getTicket();
        Assertions.assertThat(processingTicketInfoService.find(new TicketFilter())).hasSize(2);

        // получаем "шаблоны" для заполнения данными
        ExcelFile.Builder excelFile1 = trackerServiceMock.getHeaderExcelFile(ticket1).toBuilder();
        ExcelFile.Builder excelFile2 = trackerServiceMock.getHeaderExcelFile(ticket2).toBuilder();

        // эмулируем работу контентов. Т.е. заполняем маппинги.
        // заполняем разными данными, ради разнообразия теста
        setExcelFileSku(excelFile1, 1, 11);
        setExcelFileSku(excelFile1, 2, 22);
        setExcelFileComment(excelFile1, 3, "Comment #3", ContentCommentType.INCORRECT_INFORMATION);

        excelFile2.clearLine(1);
        setExcelFileComment(excelFile2, 2, "Comment #5", ContentCommentType.INCORRECT_INFORMATION);
        setExcelFileSku(excelFile2, 3, 66);

        // комментируем с добавлением файлов
        trackerServiceMock.commentWithAttachment(ticket1, excelFile1.build());
        trackerServiceMock.commentWithAttachment(ticket2, excelFile2.build());

        Assertions.assertThat(auditServiceMock.getStaffLoginsExceptUploadQueue()).containsExactly(TEST_USER);
        Assertions.assertThat(auditServiceMock.countActions(
            action -> action.getPropertyName().equals("tracker_ticket"))).isEqualTo(6);
        auditServiceMock.reset();

        // второй тикет переводим в статус решен
        IssueUtils.resolveIssue(ticket2);

        // Запускаем обход тикетов и обновление мапингов
        Map<Long, Offer> processedOffers = offersTrackerService.processMatchingTickets().stream()
            .collect(Collectors.toMap(Offer::getId, Function.identity()));
        Assertions.assertThat(processedOffers.keySet()).containsExactlyInAnyOrder(1L, 2L, 3L, 5L, 6L);

        // проверяем, что у офферов корректно выставлены мапинги
        // и корректно проставлен пользователь, обновивший маппинги
        var expected0 = OffersProcessingStatusService.clearOfferInternalSendStatus(createOfferWithMapping(1, 11, ticket1));
        MbocAssertions.assertThat(processedOffers.get(1L)).isEqualTo(expected0);

        var expected1 = OffersProcessingStatusService.clearOfferInternalSendStatus(createOfferWithMapping(2, 22, ticket1));
        MbocAssertions.assertThat(processedOffers.get(2L)).isEqualTo(expected1);

        var expected2 = OffersProcessingStatusService.clearOfferInternalSendStatus(createOfferWithComment(3, "Comment #3", ticket1))
            .updateProcessingStatusIfValid(ProcessingStatus.NEED_INFO);
        MbocAssertions.assertThat(processedOffers.get(3L))
            .usingRecursiveComparison().ignoringFields("lastVersion", "offerContent.description", "transientModifiedBy")
            .isEqualTo(expected2);

        var expected3 = OffersProcessingStatusService.clearOfferInternalSendStatus(createOfferWithComment(5, "Comment #5", ticket2))
            .updateProcessingStatusIfValid(ProcessingStatus.NEED_INFO);
        MbocAssertions.assertThat(processedOffers.get(5L)).isEqualTo(expected3);

        var expected4 = OffersProcessingStatusService.clearOfferInternalSendStatus(createOfferWithMapping(6, 66, ticket2))
            .setSupplierSkuMappingStatus(Offer.MappingStatus.REJECTED)
            .setSupplierSkuMappingCheckTs(DateTimeUtils.dateTimeNow())
            .setSupplierSkuMappingCheckLogin("mapping-user");
        MbocAssertions.assertThat(processedOffers.get(6L))
            .isEqualTo(expected4);

        // проверяем, что такие же офферы сохранены в БД
        List<Offer> offersFromDb = offerRepository.getOffersByIdsWithOfferContent(processedOffers.keySet());
        MbocAssertions.assertThat(offersFromDb).containsExactlyInAnyOrderElementsOf(
            processedOffers.values().stream().map(o -> o.copy().setTransientModifiedBy(null)).collect(toList()));

        // проверяем, что 4 оффер никак не изменился (потому что в файлах от контента его не было)
        Offer offer4 = offerRepository.getOfferById(4);
        Offer expectedOffer4 = getOffer(4)
            .updateProcessingStatusIfValid(ProcessingStatus.IN_PROCESS)
            .setTrackerTicket(ticket2)
            .setProcessingTicketId(processingTicketInfoService.getByTicketTitle(ticket2.getKey()).getId())
            .storeOfferContent(OfferContent.copyToBuilder(offer4.extractOfferContent()).id(offer4.getId()).build());
        MbocAssertions.assertThat(offer4)
            .isEqualTo(expectedOffer4);

        ProcessingTicketInfo processingTicketInfo = processingTicketInfoService.getById(offer4.getProcessingTicketId());
        Assertions.assertThat(processingTicketInfo.getCompleted()).isNull();

        Assertions.assertThat(processingTicketInfoService.convertActiveCounts(processingTicketInfo))
            .isEqualTo(ProcessingTicketInfoService.countByCategory(Collections.singletonList(offer4)));

        // Actions must be attributed to mapping-user
        Assertions.assertThat(auditServiceMock.getStaffLoginsExceptUploadQueue()).containsExactly("mapping-user");
        Assertions.assertThat(auditServiceMock.countActions(
            action -> action.getPropertyName().equals("processing_status"))).isEqualTo(5);
    }

    @Test
    public void testTicketCustomField() {
        Issue ticket = createMatchingTicket(1, AUTHOR).getTicket();
        long expectedOffers = allOffers.stream().filter(o -> o.getBusinessId() == 1).count();
        Assert.assertEquals((int) expectedOffers, ((IssueMock) ticket).getCustomField(TOTAL_OFFERS));
    }

    @Test
    public void testZeroMappingsAreIgnored() {
        // создаем тикет и заполяем в нем часть данных
        Issue ticket = createMatchingTicket(1, AUTHOR).getTicket();
        ExcelFile.Builder excelFile = trackerServiceMock.getHeaderExcelFile(ticket).toBuilder();

        // заполняем в нем первую часть данных и забираем их
        setExcelFileSku(excelFile, 1, 1);
        setExcelFileSku(excelFile, 2, 1);
        setExcelFileComment(excelFile, 3, "Test comment 1", ContentCommentType.INCORRECT_INFORMATION);
        trackerServiceMock.commentWithAttachment(ticket, excelFile.build());
        offersTrackerService.processMatchingTickets();

        // переоткрываем тикет, так как робот его автоматически закрыл
        IssueUtils.reopenIssue(ticket);

        // заполняем вторую и забираем их
        setExcelFileSku(excelFile, 1, 11);
        setExcelFileSku(excelFile, 2, 0);
        setExcelFileComment(excelFile, 2, "Test comment 2", ContentCommentType.INCORRECT_INFORMATION);
        setExcelFileComment(excelFile, 3, "Test comment 3", ContentCommentType.INCORRECT_INFORMATION);
        trackerServiceMock.commentWithAttachment(ticket, excelFile.build());
        offersTrackerService.processMatchingTickets();

        // проверяем, что маппинги выставились правильно
        Offer offer1 = offerRepository.getOfferById(1L);
        Offer offer2 = offerRepository.getOfferById(2L);
        Offer offer3 = offerRepository.getOfferById(3L);
        Assertions.assertThat(offer1.getContentSkuMapping().getMappingId()).isEqualTo(11L);
        Assertions.assertThat(offer1.getApprovedSkuMapping().getMappingId()).isEqualTo(11L);
        // 0 не стирает маппинг, обрабатываем только комментарий
        Assertions.assertThat(offer2.getContentSkuMapping().getMappingId()).isEqualTo(1L);
        Assertions.assertThat(offer2.getApprovedSkuMapping().getMappingId()).isEqualTo(1L);
        Assertions.assertThat(offer2.getContentComments())
            .isEqualTo(singletonList(new ContentComment(ContentCommentType.INCORRECT_INFORMATION, "Test comment 2")));
        Assertions.assertThat(offer3.getContentSkuMapping()).isNull();
        Assertions.assertThat(offer3.getApprovedSkuMapping()).isNull();
        Assertions.assertThat(offer3.getContentComment()).isNull();
        Assertions.assertThat(offer3.getContentComments())
            .isEqualTo(singletonList(new ContentComment(ContentCommentType.INCORRECT_INFORMATION, "Test comment 3")));
    }

    @Test
    public void testResolvedTicketWillBeClosedAfterProcessing() {
        // создаем тикет
        Issue ticket = createMatchingTicket(1, AUTHOR).getTicket();

        // эмулируем работу контентов. Т.е. заполняем маппинги.
        ExcelFile.Builder excelFile = trackerServiceMock.getHeaderExcelFile(ticket).toBuilder();
        setExcelFileSku(excelFile, 1, 11);
        setExcelFileSku(excelFile, 2, 22);
        setExcelFileSku(excelFile, 3, 33);

        // комментируем с добавлением файлов
        trackerServiceMock.commentWithAttachment(ticket, excelFile.build());

        // тикет переводим в статус решен
        IssueUtils.resolveIssue(ticket);

        // Запускаем обход тикетов и обновление мапингов
        List<Offer> processedOffers = offersTrackerService.processMatchingTickets();
        Assertions.assertThat(processedOffers).hasSize(3);

        // проверяем, что тикет закрыт и робот написал комментарий
        Assertions.assertThat(IssueUtils.isClosed(ticket))
            .withFailMessage("Expected ticket %s to be closed, actual %s",
                ticket.getKey(), ticket.getStatus().getKey())
            .isTrue();
        List<String> rawComments = trackerServiceMock.getRawComments(ticket);
        Assertions.assertThat(rawComments.get(rawComments.size() - 1))
            .contains("Работа по тикету завершена");
    }

    @Test
    public void testResolvedTicketWontCloseIfNotAllProcessed() {
        // создаем тикеты
        Issue ticket = createMatchingTicket(1, AUTHOR).getTicket();

        // получаем "шаблоны" для заполнения данными
        ExcelFile.Builder excelFile = trackerServiceMock.getHeaderExcelFile(ticket).toBuilder();
        setExcelFileSku(excelFile, 1, 11);
        setExcelFileSku(excelFile, 2, 22);
        excelFile.clearLine(3);

        // комментируем с добавлением файлов
        trackerServiceMock.commentWithAttachment(ticket, excelFile.build());

        // переводим тикет в состояние решен
        IssueUtils.resolveIssue(ticket);

        // Запускаем обход тикетов и обновление мапингов
        List<Offer> processedOffers = offersTrackerService.processMatchingTickets();
        Assertions.assertThat(processedOffers).hasSize(2);

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
    }

    /**
     * Проверяем, что статусы офферов корректно сменяются в ходе пайплайна их обработки.
     */
    @Test
    public void testCorrectProcessingStatusChanges() {
        // ФАЗА 1. Создание тикета
        List<Offer> expectedUnprocessedOffers = getUnprocessedOffers(1);
        List<Long> offerIds = expectedUnprocessedOffers.stream().map(Offer::getId).collect(toList());

        // создаем тикет
        Issue ticket = createMatchingTicket(1, AUTHOR).getTicket();

        // проверяем, что у этих офферов помелись статусы и проставились айдишники тикетов
        List<Offer> offersFromDb = offerRepository.getOffersByIds(offerIds);

        Assertions.assertThat(offersFromDb)
            .extracting(Offer::getProcessingStatus)
            .allMatch(processingStatus -> processingStatus == ProcessingStatus.IN_PROCESS);

        Assertions.assertThat(offersFromDb)
            .extracting(Offer::getTrackerTicket)
            .allMatch(ticketKey -> ticketKey.equals(ticket.getKey()));

        // ФАЗА 2. Оператор произвел изменения

        // получаем "шаблоны" для заполнения данными
        ExcelFile.Builder excelFile = trackerServiceMock.getHeaderExcelFile(ticket).toBuilder();

        // эмулируем работу контентов. Т.е. заполняем маппинги.
        setExcelFileSku(excelFile, 1, 11);
        setExcelFileSku(excelFile, 2, 22);
        setExcelFileSku(excelFile, 3, 33);

        // комментируем с добавлением файлов
        trackerServiceMock.commentWithAttachment(ticket, excelFile.build());

        // Запускаем обход тикетов и обновление мапингов
        offersTrackerService.processMatchingTickets();

        // проверяем, что статус офферов перешел в PROCESSED
        List<Offer> offersFromDb2 = offerRepository.getOffersByIds(offerIds);
        Assertions.assertThat(offersFromDb2)
            .extracting(Offer::getProcessingStatus)
            .allMatch(processingStatus -> processingStatus == ProcessingStatus.PROCESSED);

        // ФАЗА 3. Переводим статус тикета в "РЕШЕН"
        IssueUtils.resolveIssue(ticket);

        // Запускаем обход тикетов и обновление мапингов
        List<Offer> processedOffers2 = offersTrackerService.processMatchingTickets();
        Assertions.assertThat(processedOffers2).isEmpty();

        // все офферы должны остаться в статусе PROCESSED
        List<Offer> offersFromDb3 = offerRepository.getOffersByIds(offerIds);
        Assertions.assertThat(offersFromDb3)
            .extracting(Offer::getProcessingStatus)
            .allMatch(processingStatus -> processingStatus == ProcessingStatus.PROCESSED);

        // ФАЗА 4. При переокрытии тикета, статусы также должны отстаться в статусе PROCESSED
        ((IssueMock) ticket).setIssueStatus(IssueStatus.OPEN);

        // Запускаем обход тикетов и обновление мапингов
        List<Offer> processedOffers3 = offersTrackerService.processMatchingTickets();
        Assertions.assertThat(processedOffers3).isEmpty();

        List<Offer> offersFromDb4 = offerRepository.getOffersByIds(offerIds);
        Assertions.assertThat(offersFromDb4)
            .extracting(Offer::getProcessingStatus)
            .allMatch(processingStatus -> processingStatus == ProcessingStatus.PROCESSED);
    }

    /**
     * Тест, проверяет, что если к тикету успели приложиться больше файлов с пересекающимися офферами,
     * то сохранятся только последние офферы.
     */
    @Test
    public void testLastOffersWillBeSavedToDb() {
        // ФАЗА 1. Создание тикета
        Issue ticket = createMatchingTicket(1, AUTHOR).getTicket();

        // получаем "шаблон" для заполнения данными
        ExcelFile.Builder excelFile = trackerServiceMock.getHeaderExcelFile(ticket).toBuilder();
        ExcelFile.Builder copyExcelFile = new ExcelFile(excelFile.build()).toBuilder();

        // эмулируем работу контентов. Т.е. заполняем маппинги.
        // в первом файле оставляем offer только с id 1, 2
        setExcelFileSku(excelFile, 1, 11);
        setExcelFileSku(excelFile, 2, 22);
        excelFile.clearLine(3);

        // во втором файле оставляем offer только с id 2, 3
        setExcelFileSku(copyExcelFile, 2, 222);
        setExcelFileSku(copyExcelFile, 3, 333);
        copyExcelFile.clearLine(1);

        // комментируем с добавлением файлов
        trackerServiceMock.commentWithAttachment(ticket, excelFile.build());
        trackerServiceMock.commentWithAttachment(ticket, copyExcelFile.build());

        // ФАЗА 2. Запускаем обход тикетов и обновление мапингов
        List<Offer> processedOffers = offersTrackerService.processMatchingTickets();

        List<Offer> offersFromDb = offerRepository.findOffers(new OffersFilter().addBusinessId(1).setFetchOfferContent(true));
        offersFromDb.sort(Comparator.comparing(Offer::getId));

        var expected0 = OffersProcessingStatusService.clearOfferInternalSendStatus(createOfferWithMapping(1, 11,
            ticket));
        MbocAssertions.assertThat(processedOffers.get(0)).isEqualTo(expected0);
        MbocAssertions.assertThat(offersFromDb.get(0))
            .usingRecursiveComparison()
            .ignoringFields("lastVersion", "offerContent.description", "transientModifiedBy", "contentProcessed")
            .isEqualTo(expected0);

        var expected1 = OffersProcessingStatusService.clearOfferInternalSendStatus(createOfferWithMapping(2, 222,
            ticket));
        MbocAssertions.assertThat(processedOffers.get(1)).isEqualTo(expected1);
        MbocAssertions.assertThat(offersFromDb.get(1))
            .usingRecursiveComparison()
            .ignoringFields("lastVersion", "offerContent.description", "transientModifiedBy", "contentProcessed")
            .isEqualTo(expected1);

        var expected2 = OffersProcessingStatusService.clearOfferInternalSendStatus(createOfferWithMapping(3, 333,
            ticket));
        MbocAssertions.assertThat(processedOffers.get(2)).isEqualTo(expected2);
        MbocAssertions.assertThat(offersFromDb.get(2))
            .usingRecursiveComparison()
            .ignoringFields("lastVersion", "offerContent.description", "transientModifiedBy", "contentProcessed")
            .isEqualTo(expected2);
    }

    @Test
    public void testNothingUpdatedIfNoChanges() {
        // создаем тикета
        Issue issue = createMatchingTicket(1, AUTHOR).getTicket();

        // запоминаем офферы из БД
        List<Offer> offersFromDb = offerRepository.findOffers(new OffersFilter().addBusinessId(1));

        // обходим тикеты
        offersTrackerService.processMatchingTickets();

        // проверяем, что lastVersion у offer не изменился
        List<Offer> offersAfterZeroChanges = offerRepository.findOffers(new OffersFilter().addBusinessId(1));
        Assertions.assertThat(offersAfterZeroChanges)
            .usingElementComparator(MbocComparators.OFFERS_WITH_CREATED_UPDATED)
            .containsExactlyInAnyOrderElementsOf(offersFromDb);
    }

    @Test
    public void testNotUpdatedOffersWontSave() {
        // ФАЗА 1. Создание тикетов
        List<Long> offerIds = getUnprocessedOffersIds(1, 2);
        Issue ticket1 = createMatchingTicket(1, AUTHOR).getTicket();
        Issue ticket2 = createMatchingTicket(2, AUTHOR).getTicket();

        // запоминаем офферы из БД
        Map<Long, Offer> offersFromDb = offerRepository.getOffersByIds(offerIds).stream()
            .collect(Collectors.toMap(Offer::getId, Function.identity()));

        // ФАЗА 2. Производим частичный апдейт
        // по первому тикету поменяем только один оффер
        // а второй тикет переводим в статус решен и тоже помечаем только один оффер
        // получаем "шаблоны" для заполнения данными
        ExcelFile.Builder excelFile1 = trackerServiceMock.getHeaderExcelFile(ticket1).toBuilder();
        ExcelFile.Builder excelFile2 = trackerServiceMock.getHeaderExcelFile(ticket2).toBuilder();

        // эмулируем работу контентов. Т.е. заполняем маппинги.
        // заполняем не для всех строк, ради разнообразия теста
        setExcelFileSku(excelFile1, 1, 11);
        excelFile1.clearLine(3); // удалям 2 строки, чтобы в итоговом файле осталась только одна строка
        excelFile1.clearLine(2);

        setExcelFileSku(excelFile2, 1, 44);
        excelFile2.clearLine(3); // удалям 3 строки, чтобы в итоговом файле осталась только одна строка
        excelFile2.clearLine(2);
        excelFile2.clearLine(4);

        // комментируем с добавлением файлов
        trackerServiceMock.commentWithAttachment(ticket1, excelFile1.build());
        trackerServiceMock.commentWithAttachment(ticket2, excelFile2.build());

        // Переводим тикет в статус решен. Все связанные офферы должны перейти в статус закрыт
        IssueUtils.resolveIssue(ticket2);

        // ФАЗА 3. Обходим тикеты
        offersTrackerService.processMatchingTickets();

        // ФАЗА 4. Проверки
        Map<Long, Offer> offersAfterProcessing = offerRepository.getOffersByIds(offerIds).stream()
            .collect(Collectors.toMap(Offer::getId, Function.identity()));

        // офферы с id 1, 4 поменялись, так как они были в файлах
        Assertions.assertThat(offersAfterProcessing.get(1L).getLastVersion())
            .isGreaterThan(offersFromDb.get(1L).getLastVersion());
        Assertions.assertThat(offersAfterProcessing.get(4L).getLastVersion())
            .isGreaterThan(offersFromDb.get(4L).getLastVersion());

        // проверяем, что офферы с id 2, 5, 6 никак не поменялись, потому что их никто в файлах не менял
        Assertions.assertThat(offersAfterProcessing.get(2L).getLastVersion())
            .isEqualTo(offersFromDb.get(2L).getLastVersion());
        Assertions.assertThat(offersAfterProcessing.get(3L).getLastVersion())
            .isEqualTo(offersFromDb.get(3L).getLastVersion());
        Assertions.assertThat(offersAfterProcessing.get(5L).getLastVersion())
            .isEqualTo(offersFromDb.get(5L).getLastVersion());
        Assertions.assertThat(offersAfterProcessing.get(5L).getLastVersion())
            .isEqualTo(offersFromDb.get(5L).getLastVersion());
    }

    /**
     * Тест проверяет, что если что-то сломалось во время парсинга одного тикета,
     * то остальные тикеты корректно распарсятся и зальются.
     */
    @Test
    public void testIfSomethingBrokeInOneTicketItWontAffectOthers() {
        // ФАЗА 1. Создание тикетов
        Issue ticket1 = createMatchingTicket(1, AUTHOR).getTicket();
        Issue ticket2 = createMatchingTicket(2, AUTHOR).getTicket();

        Map<Long, Offer> offersById = offerRepository.findOffers(new OffersFilter().addBusinessId(1)
            .setFetchOfferContent(true)).stream()
            .collect(Collectors.toMap(Offer::getId, Function.identity()));

        // ФАЗА 2. Заполняем тикеты файлами
        // получаем "шаблоны" для заполнения данными
        ExcelFile.Builder excelFile1 = trackerServiceMock.getHeaderExcelFile(ticket1).toBuilder();
        ExcelFile.Builder excelFile2 = trackerServiceMock.getHeaderExcelFile(ticket2).toBuilder();

        // эмулируем работу контентов. Т.е. заполняем маппинги.
        // в первом файле на месте числа пишем строку
        setExcelFileSku(excelFile1, 1, "eleven");

        // второй файл остается корректным
        setExcelFileSku(excelFile2, 1, 44);
        setExcelFileSku(excelFile2, 2, 55);
        setExcelFileSku(excelFile2, 3, 66);

        // комментируем с добавлением файлов
        trackerServiceMock.commentWithAttachment(ticket1, excelFile1.build());
        trackerServiceMock.commentWithAttachment(ticket2, excelFile2.build());

        // ФАЗА 3. Обходим тикеты
        Assertions.assertThat(offersTrackerService.processMatchingTickets())
            .extracting(Offer::getId)
            .containsExactlyInAnyOrder(4L, 5L, 6L);

        // ФАЗА 4. Проверяем, что обновились только офферы с id 4, 5, 6
        // офферы с id 1, 2, 3 никак не поменялись, потому что парсинг файла бы неудачным
        MbocAssertions.assertThat(offerRepository.getOfferById(1L))
            .isEqualTo(offersById.get(1L));
        MbocAssertions.assertThat(offerRepository.getOfferById(2L))
            .isEqualTo(offersById.get(2L));
        MbocAssertions.assertThat(offerRepository.getOfferById(3L))
            .isEqualTo(offersById.get(3L));

        var expected = OffersProcessingStatusService.clearOfferInternalSendStatus(
            createOfferWithMapping(4, 44, ticket2, null));
        MbocAssertions.assertThat(offerRepository.getOfferById(4L))
            .isEqualTo(expected);

        var expected1 = OffersProcessingStatusService.clearOfferInternalSendStatus(
            createOfferWithMapping(5, 55, ticket2, null)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.REJECTED)
            .setSupplierSkuMappingCheckTs(DateTimeUtils.dateTimeNow())
            .setSupplierSkuMappingCheckLogin("mapping-user"));
        MbocAssertions.assertThat(offerRepository.getOfferById(5L))
            .isEqualTo(expected1);

        var expected2 = OffersProcessingStatusService.clearOfferInternalSendStatus(
            createOfferWithMapping(6, 66, ticket2, null)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.REJECTED)
            .setSupplierSkuMappingCheckTs(DateTimeUtils.dateTimeNow())
            .setSupplierSkuMappingCheckLogin("mapping-user"));
        MbocAssertions.assertThat(offerRepository.getOfferById(6L))
            .isEqualTo(expected2);
    }

    /**
     * Тест проверяет, что когда в тикете 2 файла, один из которых нельзя распарсить по каким-то причинам,
     * а другой вполне корректный. То система отпишется о проблемах в первом файла, а второй успешно заберет.
     */
    @Test
    public void testIfOneFileInTicketIsBrokenItWontAffectOther() {
        // ФАЗА 1. Создание тикета
        Issue ticket = createMatchingTicket(1, AUTHOR).getTicket();

        Map<Long, Offer> offersById = offerRepository.findOffers(new OffersFilter().addBusinessId(1)
            .setFetchOfferContent(true)).stream()
            .collect(Collectors.toMap(Offer::getId, Function.identity()));

        // получаем "шаблон" для заполнения данными
        ExcelFile.Builder excelFile = trackerServiceMock.getHeaderExcelFile(ticket).toBuilder();
        ExcelFile.Builder copyExcelFile = new ExcelFile(excelFile.build()).toBuilder();

        // эмулируем работу контентов. Т.е. заполняем маппинги
        // первый файл будет битым. Т.е. оффер id 1 будет битым, а оффер id 2 будет корректным
        // это нужно, чтобы проверить, что система точно не сохранит оффер с id 2
        setExcelFileSku(excelFile, 1, "some broken line");
        setExcelFileSku(excelFile, 2, 22);
        excelFile.clearLine(3);

        // во втором файле оставляем offer только с id 3
        copyExcelFile.clearLine(1);
        copyExcelFile.clearLine(2);
        setExcelFileSku(copyExcelFile, 3, 333);

        // комментируем с добавлением файлов
        trackerServiceMock.commentWithAttachment(ticket, excelFile.build());
        trackerServiceMock.commentWithAttachment(ticket, copyExcelFile.build());

        // ФАЗА 2. Запускаем обход тикетов и обновление мапингов
        Assertions.assertThat(offersTrackerService.processMatchingTickets())
            .extracting(Offer::getId)
            .containsExactlyInAnyOrder(3L);

        // ФАЗА 3. Проверки
        // В БД должен был обновиться только оффер с id 3.
        // остальные офферы не должны были поменяться

        // проверяем, что у офферов корректно выставлены мапинги
        MbocAssertions.assertThat(offerRepository.getOfferById(1L))
            .isEqualTo(offersById.get(1L));
        MbocAssertions.assertThat(offerRepository.getOfferById(2L))
            .isEqualTo(offersById.get(2L));
        MbocAssertions.assertThat(offerRepository.getOfferById(3L))
            .isEqualTo(OffersProcessingStatusService.clearOfferInternalSendStatus(
                createOfferWithMapping(3, 333, ticket, null)));
    }

    /**
     * Тест проверяет, что если парсинг файлов упал, то робот отпишется в трекер.
     */
    @Test
    public void testIfFileParsingFailedThenRobotWillWriteItToTracker() {
        // создаем тикет
        Issue issue = createMatchingTicket(1, AUTHOR).getTicket();

        // Заполняем тикеты файлами
        ExcelFile.Builder excelFile = trackerServiceMock.getHeaderExcelFile(issue).toBuilder();
        setExcelFileSku(excelFile, 1, "eleven");
        trackerServiceMock.commentWithAttachment(issue, excelFile.build());

        // Пытаемся получить офферы из трекера
        Assertions.assertThat(offersTrackerService.processMatchingTickets()).isEmpty();

        // проверяем, что в трекер записался комментарий
        List<String> rawComments = trackerServiceMock.getRawComments(issue);
        String lastComment = rawComments.get(rawComments.size() - 1);
        Assertions.assertThat(lastComment)
            .contains("Unsuccessfully retrieval")
            .contains("Ошибка на строке 2: Не получилось распарсить значение 'eleven' в столбце 'Market_sku_id'")
            .contains("Причина: For input string: \"eleven\"");
    }

    /**
     * Тест проверяет, что если парсинг файлов упал по системной ошибке, то робот
     * отпишется об ошибке парсинга в тикет, а сам метод вернет exception.
     */
    @Test
    public void testIfFileParsingFailedWithSystemErrorThenRobotWillWriteItToTrackerAndFail() {
        Mockito.when(modelStorageCachingService.getModelsFromMboThenPg(Mockito.anyCollection()))
            .then(this::failedGetModels);

        // создаем тикет
        Issue issue = createMatchingTicket(1, AUTHOR).getTicket();

        // Заполняем тикеты файлами
        ExcelFile.Builder excelFile = trackerServiceMock.getHeaderExcelFile(issue).toBuilder();
        setExcelFileSku(excelFile, 1, 1);
        trackerServiceMock.commentWithAttachment(issue, excelFile.build());

        // Пытаемся получить офферы из трекера
        Assertions.assertThatThrownBy(() -> {
            offersTrackerService.processMatchingTickets();
        }).hasMessageContaining("Failed to process offers from tickets: MCP-1");

        // проверяем, что в трекер записался комментарий
        List<String> rawComments = trackerServiceMock.getRawComments(issue);
        String lastComment = rawComments.get(rawComments.size() - 1);
        Assertions.assertThat(lastComment)
            .contains("Unsuccessfully retrieval")
            .contains("Fail to load models: [1]");
    }

    /**
     * Тест проверяет, что если парсинг файлов упал по ошибки парсинга и системной ошибке, то робот
     * отпишется об ошибке парсинга в тикет, а сам метод вернет exception.
     */
    @Test
    public void testIfFileParsingFailedWithSystemAndParseErrorsThenRobotWillWriteItToTrackerAndFail() {
        Mockito.when(modelStorageCachingService.getModelsFromMboThenPg(Mockito.anyCollection()))
            .then(this::failedGetModels);

        // создаем тикет
        Issue issue = createMatchingTicket(1, AUTHOR).getTicket();

        // Заполняем тикеты файлами
        ExcelFile.Builder excelFile = trackerServiceMock.getHeaderExcelFile(issue).toBuilder();
        setExcelFileSku(excelFile, 1, "eleven");
        setExcelFileSku(excelFile, 2, 2);
        trackerServiceMock.commentWithAttachment(issue, excelFile.build());

        // Пытаемся получить офферы из трекера
        Assertions.assertThatThrownBy(() -> {
            offersTrackerService.processMatchingTickets();
        }).hasMessageContaining("Failed to process offers from tickets: MCP-1");

        // проверяем, что в трекер записался комментарий
        List<String> rawComments = trackerServiceMock.getRawComments(issue);
        String lastComment = rawComments.get(rawComments.size() - 1);
        Assertions.assertThat(lastComment)
            .contains("Unsuccessfully retrieval")
            .contains("Ошибка на строке 2: Не получилось распарсить значение 'eleven' в столбце 'Market_sku_id' ")
            .contains("Причина: For input string: \"eleven\"")
            .contains("Fail to load models: [2]");
    }

    private Object failedGetModels(InvocationOnMock answer) {
        Collection<Long> modelIds = answer.getArgument(0);
        throw new RuntimeException("Fail to load models: " + modelIds);
    }

    @Test
    public void testResolvedTicketWontCloseIfOffersFailedToParse() {
        // создаем тикет
        Issue issue = createMatchingTicket(1, AUTHOR).getTicket();

        // Заполняем тикеты файлами
        ExcelFile.Builder excelFile = trackerServiceMock.getHeaderExcelFile(issue).toBuilder();
        setExcelFileSku(excelFile, 1, "eleven");
        trackerServiceMock.commentWithAttachment(issue, excelFile.build());

        // переводим тикет в статус решен
        IssueUtils.resolveIssue(issue);

        // Пытаемся получить офферы из трекера
        Assertions.assertThat(offersTrackerService.processMatchingTickets()).isEmpty();

        // проверяем, что тикет перешел в статус открыт, а робот отписался комментарием
        MbocAssertions.assertThat(trackerServiceMock.getTicket(issue.getKey())).isOpen();
        List<String> rawComments = trackerServiceMock.getRawComments(issue);
        String lastComment = rawComments.get(rawComments.size() - 1);
        Assertions.assertThat(lastComment)
            .contains("Робот переоткрыл тикет");
        ProcessingTicketInfo processingTicketInfo = processingTicketInfoService.getByTicketTitle(issue.getKey());

        Assertions.assertThat(processingTicketInfoService.convertActiveCounts(processingTicketInfo).values()
            .stream()
            .mapToInt(Integer::intValue)
            .sum()
        ).isGreaterThan(0);
    }

    @Test
    public void testTicketWillAutoCloseIfAllOffersSuccessfullyProcessed() {
        Issue issue = createMatchingTicket(1, AUTHOR).getTicket();

        ExcelFile.Builder excelFile = trackerServiceMock.getHeaderExcelFile(issue).toBuilder();
        setExcelFileSku(excelFile, 1, 11);
        setExcelFileSku(excelFile, 2, 22);
        setExcelFileSku(excelFile, 3, 33);

        trackerServiceMock.commentWithAttachment(issue, excelFile.build());

        // обрабатываем тикет
        offersTrackerService.processMatchingTickets();

        // Ticket shouldn't be closed as it was just created
        MbocAssertions.assertThat(trackerServiceMock.getTicket(issue.getKey())).isOpen();
        List<String> rawComments = trackerServiceMock.getRawComments(issue);
        String lastComment = rawComments.get(rawComments.size() - 1);
        Assertions.assertThat(lastComment).isNotEqualTo(TrackerService.AUTOCLOSE_COMMENT);

        // Pretend it was created long ago
        ((IssueMock) issue).setCreatedAt(Instant.now().minus(Duration.standardHours(1)));

        offersTrackerService.processMatchingTickets();

        // проверяем, что тикет перешел в статус закрыт и появился комментарий
        MbocAssertions.assertThat(trackerServiceMock.getTicket(issue.getKey())).isClosed();
        rawComments = trackerServiceMock.getRawComments(issue);
        lastComment = rawComments.get(rawComments.size() - 1);
        Assertions.assertThat(lastComment).isEqualTo(TrackerService.AUTOCLOSE_COMMENT);

        ProcessingTicketInfo processingTicketInfo = processingTicketInfoService.getByTicketTitle(issue.getKey());
        Assertions.assertThat(processingTicketInfo.getActiveOffers()).isEqualTo(ProcessingTicketInfoService.EMPTY_MAP);
        Assertions.assertThat(processingTicketInfo.getCompleted()).isNotNull();
    }

    @Test
    public void testSetSupplierMappingStatusCorrectly() {
        // создаем тикет
        Issue issue = createMatchingTicket(2, AUTHOR).getTicket();
        ExcelFile.Builder excelFile = trackerServiceMock.getHeaderExcelFile(issue).toBuilder();
        setExcelFileSku(excelFile, 1, 1);
        setExcelFileSku(excelFile, 2, 101);
        setExcelFileSku(excelFile, 3, 100);
        trackerServiceMock.commentWithAttachment(issue, excelFile.build());

        // обходим тикеты
        offersTrackerService.processMatchingTickets();

        Offer offer4 = offerRepository.getOfferById(4);
        Offer offer5 = offerRepository.getOfferById(5);
        Offer offer6 = offerRepository.getOfferById(6);

        // Проверяем корректное установление supplier sku mapping status и других атрибутов
        Assertions.assertThat(offer4.getSupplierSkuMappingStatus()).isEqualTo(Offer.MappingStatus.NONE);
        Assertions.assertThat(offer5.getSupplierSkuMappingStatus()).isEqualTo(Offer.MappingStatus.REJECTED);
        Assertions.assertThat(offer6.getSupplierSkuMappingStatus()).isEqualTo(Offer.MappingStatus.ACCEPTED);
    }

    @Test
    public void testProcessingTicketPostCreation() {
        Issue ticket = createMatchingTicket(1, AUTHOR).getTicket();
        ProcessingTicketInfo ticketInfo = processingTicketInfoService.getByTicketTitle(ticket.getKey());
        //emulating not created yet ticket
        processingTicketInfoService.delete(ticketInfo);
        Assertions.assertThat(processingTicketInfoService.findAll()).hasSize(0);
        List<Offer> ticketOffers = offerRepository.findOffers(new OffersFilter().addTrackerTickets(ticket))
            .stream()
            .map(o -> o.setProcessingTicketId(null))
            .collect(Collectors.toUnmodifiableList());
        offerRepository.updateOffers(ticketOffers);

        // эмулируем работу контентов. Т.е. заполняем маппинги.
        ExcelFile.Builder excelFile = trackerServiceMock.getHeaderExcelFile(ticket).toBuilder();
        setExcelFileSku(excelFile, 1, 11);
        setExcelFileSku(excelFile, 2, 22);
        excelFile.clearLine(3);

        // комментируем с добавлением файлов
        trackerServiceMock.commentWithAttachment(ticket, excelFile.build());

        // тикет переводим в статус решен
        IssueUtils.resolveIssue(ticket);

        // Запускаем обход тикетов и обновление мапингов
        List<Offer> processedOffers = offersTrackerService.processMatchingTickets();
        Assertions.assertThat(processedOffers).hasSize(2);
        ticketInfo = processingTicketInfoService.getByTicketTitle(ticket.getKey());
        Assertions.assertThat(processingTicketInfoService.convertActiveCounts(ticketInfo).values()
            .stream()
            .mapToInt(Integer::intValue)
            .sum()
        ).isEqualTo(1);
    }

    // HELP METHODS

    private Offer getOffer(long id) {
        return allOffers.stream()
            .filter(offer -> offer.getId() == id)
            .findFirst()
            .map(Offer::new)
            .orElseThrow(() -> new RuntimeException("Failed to find offer by id: " + id));
    }

    private Offer createOfferForMatchingTicket(long id,
                                               Supplier supplier,
                                               Offer.BindingKind bindingKind,
                                               ProcessingStatus processingStatus) {
        return new Offer().setId(id)
            .setProcessingStatusInternal(processingStatus)
            .setBusinessId(supplier.getId())
            .setShopSku("ssku" + id)
            .setTitle("title" + id)
            .setIsOfferContentPresent(true)
            .setShopCategoryName("category name")
            .storeOfferContent(OfferContent.builder().build())
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .addNewServiceOfferIfNotExistsForTests(supplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setBindingKind(bindingKind);
    }

    private List<Offer> getUnprocessedOffers(int... supplierId) {
        Set<Integer> supplierIds = Arrays.stream(supplierId).boxed().collect(Collectors.toSet());
        return allOffers.stream()
            .filter(offer -> offer.getAcceptanceStatus() == Offer.AcceptanceStatus.OK)
            .filter(offer -> offer.getProcessingStatus() == ProcessingStatus.IN_PROCESS
                || offer.getProcessingStatus() == ProcessingStatus.REOPEN)
            .filter(offer -> offer.getContentSkuMapping() == null)
            .filter(offer -> supplierIds.contains(offer.getBusinessId()))
            .map(Offer::new)
            .collect(toList());
    }

    private List<Long> getUnprocessedOffersIds(int... supplierId) {
        return getUnprocessedOffers(supplierId).stream()
            .map(Offer::getId)
            .collect(toList());
    }

    private Offer createOfferWithMapping(long id, long skuId, Issue ticket) {
        return createOfferWithMapping(id, skuId, ticket, "mapping-user");
    }

    private Offer createOfferWithMapping(long id, long skuId, Issue ticket, String transientModifiedBy) {
        Offer offer = getOffer(id);
        offer.setContentSkuMapping(new Offer.Mapping(skuId, DateTimeUtils.dateTimeNow()));
        offer.updateApprovedSkuMapping(new Offer.Mapping(skuId, DateTimeUtils.dateTimeNow()),
            Offer.MappingConfidence.CONTENT);
        offer.updateProcessingStatusIfValid(ProcessingStatus.PROCESSED);
        offer.setTrackerTicket(ticket);
        offer.approve(Offer.MappingType.CONTENT, Offer.MappingConfidence.CONTENT);
        offer.setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);

        Optional<Model> model = Optional.ofNullable(
            modelStorageCachingService.getModelsFromMboThenPg(Collections.singleton(skuId)).get(skuId));
        offer.setVendorId(model.map(Model::getVendorId).orElse(0));
        offer.setCategoryIdForTests(model.map(Model::getCategoryId).orElse(0L), Offer.BindingKind.APPROVED);
        offer.setModelId(model.map(Model::getSkuParentModelId).orElse(0L));
        offer.setMappingModifiedBy("mapping-user");
        offer.setTransientModifiedBy(transientModifiedBy);
        ProcessingTicketInfo ticketInfo = processingTicketInfoService.getByTicketTitle(ticket.getKey());
        offer.setProcessingTicketId(ticketInfo.getId());
        offer.storeOfferContent(OfferContent.builder().id(offer.getId()).build());

        return offer;
    }

    private Offer createModerationOffer(long id, long skuId, Issue ticket, String transientModifiedBy) {
        Offer offer = getOffer(id);
        offer.setContentSkuMapping(new Offer.Mapping(skuId, DateTimeUtils.dateTimeNow()));
        offer.updateApprovedSkuMapping(new Offer.Mapping(skuId, DateTimeUtils.dateTimeNow()),
            Offer.MappingConfidence.CONTENT);
        offer.updateProcessingStatusIfValid(ProcessingStatus.PROCESSED);
        offer.setTrackerTicket(ticket);
        offer.approve(Offer.MappingType.CONTENT, Offer.MappingConfidence.CONTENT);

        Optional<Model> model = Optional.ofNullable(
            modelStorageCachingService.getModelsFromMboThenPg(Collections.singleton(skuId)).get(skuId));
        offer.setVendorId(model.map(Model::getVendorId).orElse(0));
        offer.setCategoryIdForTests(model.map(Model::getCategoryId).orElse(0L), Offer.BindingKind.APPROVED);
        offer.setModelId(model.map(Model::getSkuParentModelId).orElse(0L));
        offer.setMappingModifiedBy("mapping-user");
        offer.setTransientModifiedBy(transientModifiedBy);
        return offer;
    }


    private Offer createOfferWithComment(long id, String comment, Issue ticket) {
        Offer offer = getOffer(id);
        offer.updateProcessingStatusIfValid(ProcessingStatus.PROCESSED);
        offer.setTrackerTicket(ticket);
        offer.setContentComments(new ContentComment(ContentCommentType.INCORRECT_INFORMATION, comment));
        offer.setMappingModifiedBy("mapping-user");
        offer.setTransientModifiedBy("mapping-user");
        offer.setCommentModifiedBy("mapping-user");
        ProcessingTicketInfo ticketInfo = processingTicketInfoService.getByTicketTitle(ticket.getKey());
        offer.setProcessingTicketId(ticketInfo.getId());
        offer.storeOfferContent(OfferContent.builder().id(offer.getId()).build());
        return offer;
    }

    public OptionalTicket createMatchingTicket(int supplierId, String author) {
        return matchingOffersProcessingStrategy.createTrackerTicketOrThrow(new OffersFilter()
            .addBusinessId(supplierId), author);
    }

    public OffersProcessingStrategy.OptionalTicket createMatchingTicket(Collection<Long> offerIds,
                                                                        @Nullable String author) {
        return matchingOffersProcessingStrategy.createTrackerTicketOrThrow(new OffersFilter()
            .setOfferIds(offerIds), author);
    }
}
