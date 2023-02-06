package ru.yandex.market.mboc.common.services.offers.tracker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.tracker.IssueMock;
import ru.yandex.market.mbo.tracker.utils.IssueUtils;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.datacamp.repository.TempImportChangeDeltaRepository;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.model.MasterDataFromMdiConverter;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.MigrationOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationRemovedOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationStatusRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferMetaRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferUpdateSequenceService;
import ru.yandex.market.mboc.common.offers.repository.RemovedOfferRepository;
import ru.yandex.market.mboc.common.services.excel.ExcelHeaders;
import ru.yandex.market.mboc.common.services.migration.MigrationService;
import ru.yandex.market.mboc.common.services.offers.enrichment.OffersEnrichmentService;
import ru.yandex.market.mboc.common.services.offers.mapping.RecheckClassificationService;
import ru.yandex.market.mboc.common.services.offers.queue.OfferQueueService;
import ru.yandex.market.mboc.common.services.proto.AddProductInfoHelperService;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mdm.http.MasterDataProto.MasterDataInfo;
import ru.yandex.market.mdm.http.MasterDataProto.ProviderProductMasterData;
import ru.yandex.startrek.client.model.Issue;

import static ru.yandex.market.mboc.common.services.offers.tracker.OffersTrackerService.TOTAL_OFFERS;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ReSortOffersTrackerServiceTest extends BaseOffersTrackerServiceTestClass {

    private static final String HANDLER_AUTHOR = "handler-author";

    private List<Offer> allOffers;
    private AddProductInfoHelperService addProductInfoHelperService;

    @Autowired
    private MigrationStatusRepository migrationStatusRepository;
    @Autowired
    protected MigrationOfferRepository migrationOfferRepository;
    @Autowired
    protected MigrationRemovedOfferRepository migrationRemovedOfferRepository;
    @Autowired
    protected RemovedOfferRepository removedOfferRepository;
    @Autowired
    private OfferMetaRepository offerMetaRepository;
    @Autowired
    protected OfferUpdateSequenceService offerUpdateSequenceService;
    @Autowired
    protected TempImportChangeDeltaRepository tempImportChangeDeltaRepository;
    @Autowired
    @Qualifier("createReSortTickets")
    private OfferQueueService offerQueueService;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        allOffers = YamlTestUtil.readOffersFromResources("offers/tracker-offers-for-re-sort.yml");
        allOffers.forEach(it -> {
                it.setIsOfferContentPresent(true);
            }
        );
        offerRepository.insertOffers(allOffers);
        allOffers = offerRepository.findAll();

        MasterDataFromMdiConverter masterDataFromMdiConverter = Mockito.mock(MasterDataFromMdiConverter.class);

        SupplierService supplierService = new SupplierService(supplierRepository);
        MigrationService migrationService =
            new MigrationService(migrationStatusRepository, migrationOfferRepository,
                migrationRemovedOfferRepository, supplierRepository, offerUpdateSequenceService, offerMetaRepository);
        addProductInfoHelperService = new AddProductInfoHelperService(
            offerRepository,
            supplierService,
            modelStorageCachingService,
            offerMappingActionService,
            TransactionHelper.MOCK,
            categoryCachingService,
            masterDataHelperService,
            masterDataFromMdiConverter,
            Mockito.mock(OffersEnrichmentService.class),
            applySettingsService,
            offersProcessingStatusService,
            migrationService,
            removedOfferRepository,
            antiMappingRepository,
            tempImportChangeDeltaRepository,
            offerDestinationCalculator,
            storageKeyValueService,
            hashCalculator,
            Mockito.mock(RecheckClassificationService.class),
            false);

        migrationService.checkAndUpdateCache();
    }

    @Test
    public void testCorrectExcelFile() {
        Map<Long, Offer> resortOffers = getResortOffers();

        Issue ticket = createTicket(offerQueueService, offersTrackerService::createReSortTicket);

        // поверяем, что созданный файл соответствует ожидаемому
        ExcelFile expectedExcelFile = reSortConverter.convert(new ArrayList<>(resortOffers.values()));
        ExcelFile headerExcelFile = trackerServiceMock.getHeaderExcelFile(ticket);
        MbocAssertions.assertThat(headerExcelFile)
            .isEqualTo(expectedExcelFile)
            .containsValue(1, ExcelHeaders.SUPPLIER_ID, 1)
            .containsValue(1, ExcelHeaders.SUPPLIER_NAME, "Supplier 1")
            .containsValue(2, ExcelHeaders.SUPPLIER_ID, 2)
            .containsValue(2, ExcelHeaders.SUPPLIER_NAME, "Supplier 2")
            .containsValue(3, ExcelHeaders.SUPPLIER_ID, 3)
            .containsValue(3, ExcelHeaders.SUPPLIER_NAME, "Supplier 3")
            .containsValue(3, ExcelHeaders.SUPPLIER_ID, 3)
            .containsValue(3, ExcelHeaders.SUPPLIER_NAME, "Supplier 3");
    }

    @Test
    public void testCreateReSortTicket() {
        Map<Long, Offer> resortOffers = getResortOffers();

        Issue ticket = createTicket(offerQueueService, offersTrackerService::createReSortTicket);

        // проверяем, что у этих офферов помелись статусы и проставились айдишники тикетов
        List<Offer> offersFromDb = offerRepository.getOffersByIds(resortOffers.keySet());

        Assertions.assertThat(offersFromDb)
            .extracting(Offer::getProcessingStatus)
            .allMatch(Offer.ProcessingStatus.IN_RE_SORT::equals);
        Assertions.assertThat(offersFromDb)
            .extracting(Offer::getTrackerTicket)
            .allMatch(ticketKey -> Objects.equals(ticketKey, ticket.getKey()));
    }

    @Test
    public void testTicketCustomField() {
        Issue ticket = createTicket(offerQueueService, offersTrackerService::createReSortTicket);

        Assert.assertEquals(getResortOffers().size(), ((IssueMock) ticket).getCustomField(TOTAL_OFFERS));
    }

    @Test
    public void testCreateTicketAndProcessResult() {
        categoryCachingService.addCategory(10, "Category number 10");

        Map<Long, Offer> resortOffers = getResortOffers();

        Issue issue = createTicket(offerQueueService, offersTrackerService::createReSortTicket);

        // в тикет не добавляем никакие файлы, так как задания такого типа не предполагают обработку в тикетах
        // вместо этого изменяем файлы "по-ручке"
        MboMappings.ProviderProductInfoRequest request = MboMappings.ProviderProductInfoRequest.newBuilder()
            .setRequestInfo(MboMappings.ProductUpdateRequestInfo.newBuilder()
                .setUserLogin(HANDLER_AUTHOR)
                .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.CONTENT)
                .build())
            .addProviderProductInfo(createProductInfo(resortOffers.get(1L), 101))
            .addProviderProductInfo(createProductInfo(resortOffers.get(2L), 0))
            .build();
        MboMappings.ProviderProductInfoResponse response = addProductInfoHelperService.addProductInfo(request, false);
        Assertions.assertThat(response.getStatus()).isEqualTo(MboMappings.ProviderProductInfoResponse.Status.OK);

        // закрываем тикет и обрабатываем результаты
        IssueUtils.resolveIssue(issue);
        offersTrackerService.processReSortTickets();
        MbocAssertions.assertThat(trackerServiceMock.getTicket(issue.getKey())).isClosed();

        // после обработки офферы, которые заливались по ручке не должны поменяться
        // а офферы, которые не трогались должны перейти в статус rejected
        Map<Long, Offer> offersFromDb = offerRepository.getOffersByIdsWithOfferContent(resortOffers.keySet()).stream()
            .collect(Collectors.toMap(Offer::getId, Function.identity()));

        // 1 оффер был обновлен по ручке
        MbocAssertions.assertThat(offersFromDb.get(1L))
            .isEqualToIgnoreContent(getOffer(1L)
                .setTrackerTicket("MCP-1")
                .setTicketCritical(false)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.ACCEPTED, HANDLER_AUTHOR,
                    DateTimeUtils.dateTimeNow())
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
                .setModelId(123L)
                .setCategoryIdForTests(10L, Offer.BindingKind.APPROVED)
                .setMappedCategoryId(10L)
                .setMappedCategoryConfidence(Offer.MappingConfidence.CONTENT)
                .setVendorId(130)
                .approve(Offer.MappingType.CONTENT, Offer.MappingConfidence.CONTENT)
                .updateApprovedSkuMapping(new Offer.Mapping(101, DateTimeUtils.dateTimeNow()),
                    Offer.MappingConfidence.CONTENT)
                .setContentSkuMapping(new Offer.Mapping(101, DateTimeUtils.dateTimeNow()))
                .setContentCategoryMappingId(10L)
                .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT)
                .setMappingModifiedBy(HANDLER_AUTHOR)
                .addNewServiceOfferIfNotExistsForTests(
                    new Supplier(1, "test").setType(MbocSupplierType.THIRD_PARTY)));
        // 2 оффер был обновлен по ручке
        MbocAssertions.assertThat(offersFromDb.get(2L))
            .isEqualToIgnoreContent(getOffer(2L)
                .clearTrackerTicket()
                .setTicketCritical(false)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.REJECTED, HANDLER_AUTHOR,
                    DateTimeUtils.dateTimeNow())
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
                .setCategoryIdForTests(10L, Offer.BindingKind.APPROVED)
                .setMappedCategoryId(10L)
                .setMappedCategoryConfidence(Offer.MappingConfidence.CONTENT)
                .setVendorId(0)
                .setModelId(0L)
                .approve(Offer.MappingType.CONTENT, Offer.MappingConfidence.CONTENT)
                .updateApprovedSkuMapping(new Offer.Mapping(0, DateTimeUtils.dateTimeNow()),
                    null)
                .setContentSkuMapping(new Offer.Mapping(0, DateTimeUtils.dateTimeNow()))
                .setContentCategoryMappingId(10L)
                .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                .setApprovedSkuMappingConfidence(null)
                .setMappingModifiedBy(HANDLER_AUTHOR)
                .addNewServiceOfferIfNotExistsForTests(
                    new Supplier(2, "test").setType(MbocSupplierType.THIRD_PARTY)));
        // 3 оффер совсем не трогали
        MbocAssertions.assertThat(offersFromDb.get(3L))
            .isEqualToIgnoreContent(getOffer(3L)
                .clearTrackerTicket()
                .setTicketCritical(false)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.REJECTED, "test-user",
                    DateTimeUtils.dateTimeNow())
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED));
        // 6 оффер: в approved_mapping тип sku = PSKU => ProcessingStatus = AUTO_PROCESSED
        MbocAssertions.assertThat(offersFromDb.get(6L))
            .isEqualToIgnoreContent(getOffer(6L)
                .clearTrackerTicket()
                .setTicketCritical(false)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.REJECTED, "test-user",
                    DateTimeUtils.dateTimeNow())
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.AUTO_PROCESSED)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.PROCESSED));
    }

    private MboMappings.ProviderProductInfo createProductInfo(Offer offer, long contentMappingId) {
        return MboMappings.ProviderProductInfo.newBuilder()
            .setShopId(offer.getBusinessId())
            .setShopSkuId(offer.getShopSku())
            .setTitle(offer.getTitle())
            .setMappingType(MboMappings.MappingType.SUPPLIER)
            .setMarketSkuId(contentMappingId)
            .setMarketCategoryId(10)
            .setMasterDataInfo(MasterDataInfo.newBuilder()
                .setProviderProductMasterData(ProviderProductMasterData
                    .newBuilder()
                    .addManufacturerCountry("Россия")
                    .build()
                ).build())
            .build();
    }

    private Map<Long, Offer> getResortOffers() {
        return offerRepository.getOffersByIdsWithOfferContent(List.of(1L, 2L, 3L, 5L, 6L, 9L)).stream()
            .collect(Collectors.toMap(Offer::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private Offer getOffer(long id) {
        return allOffers.stream()
            .filter(offer -> offer.getId() == id)
            .findFirst().orElseThrow(() -> new RuntimeException("Failed to find offer by id " + id));
    }
}
