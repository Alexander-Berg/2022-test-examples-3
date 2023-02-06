package ru.yandex.market.mboc.tms.executors;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.erp.ErpMappingExporterDao;
import ru.yandex.market.mboc.common.erp.ErpMappingExporterService;
import ru.yandex.market.mboc.common.erp.model.ErpMapping;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.offers.upload.ErpOfferUploadQueueService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mboc.tms.utils.UploadApprovedMappingsHelper.createNonProcessedOffer;
import static ru.yandex.market.mboc.tms.utils.UploadApprovedMappingsHelper.createOffer;
import static ru.yandex.market.mboc.tms.utils.UploadApprovedMappingsHelper.createUploadToYtOffer;
import static ru.yandex.market.mboc.tms.utils.UploadApprovedMappingsHelper.createWhiteOffer;

/**
 * Тесты {@link UploadApprovedMappingsErpExecutor}.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class UploadApprovedMappingsErpExecutorTest extends BaseDbTestClass {

    private static final long CATEGORY_ID = 20;
    private static final int BERU_ID = 465852;
    private UploadApprovedMappingsErpExecutor uploadApprovedMappingsErpExecutor;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private ErpOfferUploadQueueService erpOfferUploadQueueService;

    private Offer offer1;
    private Offer nonApprovedOffer;
    private ErpMappingExporterService erpMappingExporterService;
    private ErpMappingExporterDao erpMappingExporterDao;

    private Supplier supplier1;
    private Supplier supplier2;
    private Supplier realSupplier1;

    @Before
    public void setUp() throws Exception {
        erpMappingExporterDao = Mockito.mock(ErpMappingExporterDao.class);
        erpMappingExporterService = new ErpMappingExporterService(
            TransactionHelper.MOCK, erpMappingExporterDao, BERU_ID);
        uploadApprovedMappingsErpExecutor = new UploadApprovedMappingsErpExecutor(
                offerRepository,
                supplierRepository,
                transactionHelper,
                erpMappingExporterService,
                erpOfferUploadQueueService,
                true
        );

        supplier1 = new Supplier(1, "Test supplier", null, null);
        supplierRepository.insert(supplier1);
        supplier2 = new Supplier(2, "Supplier #2", null, null);
        supplierRepository.insert(supplier2);
        realSupplier1 = new Supplier(11, "Real supplier", null, null)
            .setType(MbocSupplierType.REAL_SUPPLIER)
            .setRealSupplierId("77777");
        supplierRepository.insert(realSupplier1);
        supplierRepository.insert(new Supplier(3, "Supplier #3", null, null));
        supplierRepository.insert(new Supplier(OfferTestUtils.TEST_SUPPLIER_ID, "Test supplier"));

        offer1 = createOffer(supplier1);
        nonApprovedOffer = createNonProcessedOffer(supplier1);
    }

    private static List<ErpMapping> captureErpInsertMappingsRequest(
        ErpMappingExporterDao erpMappingExporterDao, int number
    ) {
        ArgumentCaptor<List<ErpMapping>> erpMappingsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(erpMappingExporterDao, times(number)).insertMappings(erpMappingsCaptor.capture());
        return erpMappingsCaptor.getAllValues().stream().flatMap(l -> l.stream()).collect(Collectors.toList());
    }

    @Test
    public void testNoRequestsIfNoApprovedOffers() throws Exception {
        offerRepository.insertOffer(nonApprovedOffer);
        assertThat(offerRepository.getOfferById(nonApprovedOffer.getId())).matches(this::notNeedsUploadToErp);

        // запускаем
        uploadApprovedMappingsErpExecutor.execute();

        // проверяем
        captureErpInsertMappingsRequest(erpMappingExporterDao, 0);
        assertThat(offerRepository.getOfferById(nonApprovedOffer.getId())).matches(this::notNeedsUploadToErp);
    }

    @Test
    public void testNoRequestsIfNothingInQueue() throws Exception {
        offerRepository.insertOffer(offer1);
        erpOfferUploadQueueService.dequeueOfferIds(List.of(offer1.getId()));
        assertThat(offerRepository.getOfferById(offer1.getId())).matches(this::notNeedsUploadToErp);

        // запускаем
        uploadApprovedMappingsErpExecutor.execute();

        // проверяем
        captureErpInsertMappingsRequest(erpMappingExporterDao, 0);
        assertThat(offerRepository.getOfferById(offer1.getId())).matches(this::notNeedsUploadToErp);
    }

    @Test
    public void testWhiteOffer() {
        Supplier supplier1 = supplierRepository.findById(1)
            .setType(MbocSupplierType.MARKET_SHOP);
        supplierRepository.update(supplier1);
        Offer whiteOffer = createWhiteOffer(supplier1);
        whiteOffer.setSupplierSkuMapping(
            new Offer.Mapping(
                11,
                LocalDateTime.of(2018, Month.APRIL, 28, 15, 26)
            )
        );

        offerRepository.insertOffer(whiteOffer);
        uploadApprovedMappingsErpExecutor.execute();

        captureErpInsertMappingsRequest(erpMappingExporterDao, 0);
        assertThat(offerRepository.getOfferById(whiteOffer.getId())).matches(this::notNeedsUploadToErp);
    }

    @Test
    public void testNegativeScenario() {
        // arrange

        Offer testOffer = createOffer(1001, realSupplier1);

        offerRepository.insertOffer(testOffer);
        offerRepository.insertOffer(nonApprovedOffer);
        assertThat(testOffer).matches(this::needsUploadToErp);
        assertThat(nonApprovedOffer).matches(this::notNeedsUploadToErp);

        Offer newOffer = offerRepository.getOfferById(testOffer.getId());
        Offer newNonApprovedOffer = offerRepository.getOfferById(nonApprovedOffer.getId());

        when(erpMappingExporterDao.insertMappings(any())).thenThrow(new RuntimeException("some error"));

        // act
        Assertions.assertThatThrownBy(() -> {
            uploadApprovedMappingsErpExecutor.execute();
        }).isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Action 'Process offers batch' failed");

        assertThat(testOffer).matches(this::needsUploadToErp);
        assertThat(nonApprovedOffer).matches(this::notNeedsUploadToErp);

        // This offers should not be changed at all
        assertThat(offerRepository.getOfferById(testOffer.getId()))
                .usingRecursiveComparison().isEqualTo(newOffer);
        assertThat(offerRepository.getOfferById(nonApprovedOffer.getId()))
            .usingRecursiveComparison().isEqualTo(newNonApprovedOffer);
    }

    @Test
    public void testSuccessUploadIfUcOnSecondCallReturnTrue() {

        offerRepository.insertOffer(createOffer(realSupplier1));

        when(erpMappingExporterDao.insertMappings(any()))
            .thenThrow(new RuntimeException("first error"))
            .thenReturn(1);

        // запускаем
        uploadApprovedMappingsErpExecutor.execute();

        // проверяем
        captureErpInsertMappingsRequest(erpMappingExporterDao, 2);
    }

    @Test
    public void testSimple3PErpWillNotExport() {
        Supplier thirdPartySupplier = new Supplier(10, "Test supplier", null, null)
            .setType(MbocSupplierType.THIRD_PARTY);
        supplierRepository.insert(thirdPartySupplier);

        Offer offer3p = createUploadToYtOffer(
            1001,
            Offer.MappingType.APPROVED,
            Offer.MappingDestination.BLUE,
            thirdPartySupplier,
            "12345"
        );
        offer3p.setVendorId(123);
        offerRepository.insertOffer(offer3p);
        assertFalse(needsUploadToErp(offer3p));

        uploadApprovedMappingsErpExecutor.execute();

        List<ErpMapping> erpRequest = captureErpInsertMappingsRequest(erpMappingExporterDao, 0);
        assertThat(erpRequest).hasSize(0);
    }

    @Test
    public void testErpExportContentAndApproved() {
        Offer approvedOffer = createUploadToYtOffer(
            1001,
            Offer.MappingType.APPROVED,
            Offer.MappingDestination.BLUE,
            realSupplier1,
            "1111"
        );
        approvedOffer.setVendorId(123);
        offerRepository.insertOffer(approvedOffer);

        Offer contentOffer = createUploadToYtOffer(
            1002,
            Offer.MappingType.CONTENT,
            Offer.MappingDestination.BLUE,
            realSupplier1,
            "2222"
        );
        contentOffer.setVendorId(123);
        offerRepository.insertOffer(contentOffer);

        uploadApprovedMappingsErpExecutor.execute();

        List<ErpMapping> erpRequest = captureErpInsertMappingsRequest(erpMappingExporterDao, 1);
        assertThat(erpRequest).hasSize(2);
        checkContainsErpMapping(
            erpRequest,
            BERU_ID,
            String.format("%s.%s", realSupplier1.getRealSupplierId(), approvedOffer.getShopSku()),
            1001
        );
        checkContainsErpMapping(
            erpRequest,
            BERU_ID,
            String.format("%s.%s", realSupplier1.getRealSupplierId(), contentOffer.getShopSku()),
            1002
        );
    }

    @Test
    public void testErpExportDeleted() {
        Offer approvedOffer = createUploadToYtOffer(
            0,
            Offer.MappingType.APPROVED,
            Offer.MappingDestination.BLUE,
            realSupplier1,
            "1111"
        );
        approvedOffer.setVendorId(123);
        offerRepository.insertOffer(approvedOffer);
        uploadApprovedMappingsErpExecutor.execute();

        List<ErpMapping> erpRequest = captureErpInsertMappingsRequest(erpMappingExporterDao, 1);
        assertThat(erpRequest).hasSize(1);
        checkContainsErpMapping(
            erpRequest,
            BERU_ID,
            String.format("%s.%s", realSupplier1.getRealSupplierId(), approvedOffer.getShopSku()),
            0
        );
    }

    @Test
    public void testErpExportBySupplierType() {
        Supplier supplier10 = new Supplier(10, "Third party", null, null)
            .setType(MbocSupplierType.THIRD_PARTY);
        supplierRepository.insert(supplier10);

        Offer approvedOffer = createUploadToYtOffer(
            1001,
            Offer.MappingType.APPROVED,
            Offer.MappingDestination.BLUE,
            supplier10,
            "1111"
        );
        approvedOffer.setVendorId(123);
        offerRepository.insertOffer(approvedOffer);

        Offer contentOffer = createUploadToYtOffer(
            1002,
            Offer.MappingType.APPROVED,
            Offer.MappingDestination.BLUE,
            realSupplier1,
            "2222"
        );
        contentOffer.setVendorId(123);
        offerRepository.insertOffer(contentOffer);

        uploadApprovedMappingsErpExecutor.execute();

        List<ErpMapping> erpRequest = captureErpInsertMappingsRequest(erpMappingExporterDao, 1);
        assertThat(erpRequest).hasSize(1);
        checkContainsErpMapping(
            erpRequest,
            BERU_ID,
            String.format("%s.%s", realSupplier1.getRealSupplierId(), contentOffer.getShopSku()),
            1002
        );
    }

    private void checkContainsErpMapping(List<ErpMapping> erpMappings, int shopId, String shopSkuId, long skuId) {
        assertThat(erpMappings)
            .extracting(
                ErpMapping::getSupplierId,
                ErpMapping::getSskuId,
                ErpMapping::getMskuId
            )
            .contains(Assertions.tuple(shopId, shopSkuId, skuId));
    }

    @Test
    public void testErpMappings() {

        Offer testOffer1 = createOffer(1001, realSupplier1);
        offerRepository.insertOffer(testOffer1);
        Offer testOffer2 = createOffer(1002, realSupplier1).setShopSku("123456");
        offerRepository.insertOffer(testOffer2);

        uploadApprovedMappingsErpExecutor.execute();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErpMapping>> mappingsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(erpMappingExporterDao, times(1)).insertMappings(mappingsCaptor.capture());
        List<ErpMapping> mappings = mappingsCaptor.getValue();

        assertThat(mappings).hasSize(2);
        assertThat(testOffer1).matches(this::notNeedsUploadToErp);
        assertThat(testOffer2).matches(this::notNeedsUploadToErp);
        assertThat(mappings)
            .extracting(
                m -> m.getSskuId(),
                m -> m.getRealSupplierId(),
                m -> m.getBarcodes(),
                m -> m.getMskuId(),
                m -> m.getTitle()
            )
            .containsExactlyInAnyOrder(
                Assertions.tuple(
                    String.format("%s.%s", realSupplier1.getRealSupplierId(), testOffer1.getShopSku()),
                    realSupplier1.getRealSupplierId(),
                    "44444",
                    1001L,
                    "Content offer"
                ),
                Assertions.tuple(
                    String.format("%s.%s", realSupplier1.getRealSupplierId(), testOffer2.getShopSku()),
                    realSupplier1.getRealSupplierId(),
                    "44444",
                    1002L,
                    "Content offer"
                )
            );
    }

    @Test
    public void testDuplicateShopSkuInErp() {
        Supplier supplier1 = supplierRepository.findById(1)
            .setRealSupplierId("REAL")
            .setType(MbocSupplierType.REAL_SUPPLIER);
        Supplier supplier2 = supplierRepository.findById(2)
            .setRealSupplierId("REAL2")
            .setType(MbocSupplierType.REAL_SUPPLIER);
        supplierRepository.update(supplier1);
        supplierRepository.update(supplier2);

        Offer testOffer1 = createOffer(1001, supplier1).setShopSku("123456");
        Offer testOffer2 = createOffer(1001, supplier2).setShopSku("123456");

        offerRepository.insertOffer(testOffer1);
        offerRepository.insertOffer(testOffer2);

        uploadApprovedMappingsErpExecutor.execute();

        assertThat(testOffer1).matches(this::notNeedsUploadToErp);
        assertThat(testOffer2).matches(this::notNeedsUploadToErp);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErpMapping>> mappingsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(erpMappingExporterDao, times(2)).insertMappings(mappingsCaptor.capture());

        assertThat(mappingsCaptor.getAllValues().stream().flatMap(l -> l.stream().map(ErpMapping::getSskuId)))
            .containsExactlyInAnyOrder(
                String.format("%s.%s", supplier1.getRealSupplierId(), testOffer1.getShopSku()),
                String.format("%s.%s", supplier2.getRealSupplierId(), testOffer2.getShopSku())
            );
    }

    @Test
    public void testUploadSupplierMappingDelete() {
        Offer someOffer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setSupplierSkuMapping(OfferTestUtils.mapping(0))
            .approve(Offer.MappingType.SUPPLIER, Offer.MappingConfidence.PARTNER_SELF)
            .setMappingDestination(Offer.MappingDestination.WHITE)
            .setUploadToYtStamp(1L);

        offerRepository.insertOffer(someOffer);

        uploadApprovedMappingsErpExecutor.execute();

        Offer updated = offerRepository.getOfferById(someOffer.getId());
        assertThat(updated).matches(this::notNeedsUploadToErp);
        assertEquals(Long.valueOf(CATEGORY_ID), updated.getCategoryId());
        assertEquals(0, updated.getApprovedSkuMapping().getMappingId());
    }

    @Test
    public void testMappingUploadedForFMCGSupplierType() {
        // arrange
        Supplier fmcgSupplier = OfferTestUtils.fmcgSupplier();
        Offer fmcgOffer = createOffer(1, fmcgSupplier);
        supplierRepository.insert(fmcgSupplier);
        offerRepository.insertOffer(fmcgOffer);

        // act
        uploadApprovedMappingsErpExecutor.execute();

        Offer offerFromDb = offerRepository.getOfferById(fmcgOffer.getId());
        assertThat(offerFromDb).matches(this::notNeedsUploadToErp);
        assertThat(offerFromDb.getLastVersion()).isGreaterThan(fmcgOffer.getLastVersion());
    }

    @Test
    public void testOfferMappedToBusinessSupplierErpIgnore() {
        Supplier bizSupplier = OfferTestUtils.businessSupplier();
        supplierRepository.insert(bizSupplier);

        Offer bizOffer = createUploadToYtOffer(
            1,
            Offer.MappingType.APPROVED,
            Offer.MappingDestination.BLUE,
            bizSupplier,
            "ssku1");
        offerRepository.insertOffer(bizOffer);
        assertThat(bizOffer).matches(this::notNeedsUploadToErp);

        uploadApprovedMappingsErpExecutor.execute();

        assertThat(bizOffer).matches(this::notNeedsUploadToErp);
        captureErpInsertMappingsRequest(erpMappingExporterDao, 0);
    }

    private boolean notNeedsUploadToErp(Offer offer) {
        return !needsUploadToErp(offer);
    }

    private boolean notNeedsUploadToErp(Long offerId) {
        return !needsUploadToErp(offerId);
    }

    private boolean needsUploadToErp(Offer offer) {
        return needsUploadToErp(offer.getId());
    }

    private boolean needsUploadToErp(Long offerId) {
        return erpOfferUploadQueueService.areAllOfferIdsInQueue(List.of(offerId));
    }
}
