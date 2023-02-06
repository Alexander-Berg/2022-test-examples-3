package ru.yandex.market.mboc.app.proto;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import Market.DataCamp.DataCampOffer;
import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.mbo.http.SkuBDApi;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationOfferResolution;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationOfferState;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationStatusType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationOffer;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationRemovedOffer;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationStatus;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.offers.model.AntiMapping;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.services.excel.ExcelHeaders;
import ru.yandex.market.mboc.common.services.proto.AddProductInfoListener;
import ru.yandex.market.mboc.common.services.proto.datacamp.AdditionalData;
import ru.yandex.market.mboc.common.services.proto.datacamp.DatacampContext;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.MboMappings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author shadoff
 * created on 2/10/21
 */
public class AddProductInfoHelperServiceMigrationTest extends AddProductInfoHelperServiceTestBase {
    private static final String SHOP_SKU = "magicssku";
    private static final long MIGRATION_ID = 100L;
    private static final Long MIGRATION_OFFER_ID = 1000L;
    private static final int TARGET_BUSINESS_ID = 100;
    private static final String DEFAULT_TITLE = "Дворец";
    private Supplier sourceBusiness;
    private Supplier targetBusiness;
    private Supplier supplier;

    @Before
    public void setUp() {
        sourceBusiness = new Supplier(10000,
            "biz",
            "biz.biz",
            "biz",
            MbocSupplierType.BUSINESS);
        targetBusiness = new Supplier(TARGET_BUSINESS_ID,
            "black hole biz",
            "biz.biz",
            "biz",
            MbocSupplierType.BUSINESS);
        supplier = new Supplier(10002,
            "sup",
            "sup.biz",
            "biz",
            MbocSupplierType.THIRD_PARTY);
        supplier.setBusinessId(sourceBusiness.getId());

        supplierRepository.insertBatch(sourceBusiness, targetBusiness, supplier);
    }

    @Test
    public void testBusinessIsInMigration() {
        Offer before = commonBlueOffer();
        offerRepository.insertOffer(before);

        MigrationStatus migrationStatus = new MigrationStatus()
            .setId(1L)
            .setTargetBusinessId(before.getBusinessId())
            .setSupplierId(1234123)
            .setSourceBusinessId(1)
            .setCreatedTs(Instant.now())
            .setMigrationStatus(MigrationStatusType.ACTIVE);
        migrationStatusRepository.save(migrationStatus);

        migrationService.checkAndUpdateCache();

        MboMappings.ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(blueProductInfo().setTitle("CHANGED!").build())
                .build(), true);

        assertThat(response.getStatus()).isEqualTo(MboMappings.ProviderProductInfoResponse.Status.ERROR);
        assertThat(response.getMessage())
            .contains("Business supplier " + before.getBusinessId() + " is in migration");
        assertThat(response.getResults(0).getErrors(0).getMessage())
            .contains("Business supplier " + before.getBusinessId() + " is in migration");
        validateHasError(response.getResults(0), MboMappings.ProviderProductInfoResponse.ErrorKind.OTHER);

        Offer after = offerRepository.getOfferById(before.getId());
        assertThat(after.getTitle()).isEqualTo(before.getTitle());
    }

    @Test
    public void testBusinessIsInMigrationFinished() {
        Offer before = commonBlueOffer();
        offerRepository.insertOffer(before);

        MigrationStatus migrationStatus = new MigrationStatus()
            .setId(1L)
            .setTargetBusinessId(before.getBusinessId())
            .setSupplierId(1234123)
            .setSourceBusinessId(1)
            .setCreatedTs(Instant.now())
            .setMigrationStatus(MigrationStatusType.FINISHED);
        migrationStatusRepository.save(migrationStatus);

        MboMappings.ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(blueProductInfo().setTitle("CHANGED!").build())
                .build(), true);

        assertThat(response.getStatus()).isEqualTo(MboMappings.ProviderProductInfoResponse.Status.OK);
    }

    @Test
    public void testSaveOfferInMigration() {
        prepareDefaultMigration(targetBusiness.getId(), sourceBusiness.getId(), supplier.getId());
        DatacampContext datacampContext = getMigrationDatacampContext(targetBusiness, supplier, SHOP_SKU);

        MboMappings.ProviderProductInfo productInfo = blueProductInfo()
            .setShopSkuId(SHOP_SKU)
            .setShopId(targetBusiness.getId())
            .setTitle("Title!")
            .build();

        MboMappings.ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(productInfo)
                .build(), datacampContext, AddProductInfoListener.NOOP);

        assertThat(response.getStatus()).isEqualTo(MboMappings.ProviderProductInfoResponse.Status.OK);

        MigrationOffer resultMigrationOffer = migrationOfferRepository.getById(MIGRATION_OFFER_ID);
        assertThat(resultMigrationOffer.getState()).isEqualTo(MigrationOfferState.RECEIVED);
        assertThat(resultMigrationOffer.getServiceOffer()).isNull(); // no source service offer to save

        Offer newOffer =
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(TARGET_BUSINESS_ID, SHOP_SKU)).get(0);
        assertThat(newOffer.getTitle()).isEqualTo("Title!");
        assertThat(newOffer.getServiceOffers()).hasSize(1); // new service offer created
        assertTrue(newOffer.getServiceOffer(supplier.getId()).isPresent());
    }

    @Test
    public void testErrorSavingInMigrationOffer() {
        prepareDefaultMigration(targetBusiness.getId(), sourceBusiness.getId(), supplier.getId());
        DatacampContext datacampContext = getMigrationDatacampContext(targetBusiness, supplier, SHOP_SKU);

        offerRepository.insertOffers(OfferTestUtils.nextOffer()
            .setShopSku(SHOP_SKU)
            .setBusinessId(sourceBusiness.getId()));

        MboMappings.ProviderProductInfo productInfo = blueProductInfo()
            .setShopId(targetBusiness.getId())
            .setShopSkuId(SHOP_SKU)
            .setTitle("CHANGED!")
            .addUrl("wrong url is here")
            .build();

        MboMappings.ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(productInfo)
                .build(), datacampContext, AddProductInfoListener.NOOP);

        assertThat(response.getStatus()).isEqualTo(MboMappings.ProviderProductInfoResponse.Status.OK);

        MigrationOffer resultMigrationOffer = migrationOfferRepository.getById(MIGRATION_OFFER_ID);
        assertThat(resultMigrationOffer.getState()).isEqualTo(MigrationOfferState.RECEIVED);
        assertThat(resultMigrationOffer.getServiceOffer()).isNull(); // no source service offer to save

        Offer newOffer = offerRepository
            .findOffersByBusinessSkuKeysWithOfferContent(new BusinessSkuKey(TARGET_BUSINESS_ID, SHOP_SKU))
            .get(0);
        assertThat(newOffer.getTitle()).isEqualTo("CHANGED!");
        assertThat(newOffer.getServiceOffers()).hasSize(1); // new service offer created
        assertTrue(newOffer.getServiceOffer(supplier.getId()).isPresent());

        assertThat(response.getResults(0).getErrorsList()).isEmpty();
        assertThat(newOffer.getOfferErrors())
            .extracting(ErrorInfo::getErrorCode)
            .containsExactlyInAnyOrder(
                MbocErrors.get().malformedUrl("stub").getErrorCode()
            );
    }

    @Test
    public void testMigrateServiceOfferFromSource() {
        prepareDefaultMigration(targetBusiness.getId(), sourceBusiness.getId(), supplier.getId());
        Offer sourceOffer = offer(sourceBusiness.getId(), supplier);
        Supplier otherSupplier = new Supplier(10003,
            "sup other",
            "sup.biz",
            "biz",
            MbocSupplierType.THIRD_PARTY);
        otherSupplier.setBusinessId(sourceBusiness.getId());
        sourceOffer.addNewServiceOfferIfNotExistsForTests(otherSupplier);
        Offer.ServiceOffer sourceServiceOffer = sourceOffer.getServiceOffer(supplier.getId()).orElseThrow();

        supplierRepository.insert(otherSupplier);
        offerRepository.insertOffer(sourceOffer);

        DatacampContext datacampContext = getMigrationDatacampContext(targetBusiness, supplier, SHOP_SKU);

        MboMappings.ProviderProductInfo productInfo = blueProductInfo()
            .setShopId(targetBusiness.getId())
            .setShopSkuId(SHOP_SKU)
            .setTitle("CHANGED!")
            .build();

        MboMappings.ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(productInfo)
                .build(), datacampContext, AddProductInfoListener.NOOP);

        assertThat(response.getStatus()).isEqualTo(MboMappings.ProviderProductInfoResponse.Status.OK);

        MigrationOffer resultMigrationOffer = migrationOfferRepository.getById(MIGRATION_OFFER_ID);
        assertThat(resultMigrationOffer.getState()).isEqualTo(MigrationOfferState.RECEIVED);
        assertThat(resultMigrationOffer.getServiceOffer()).isNotNull(); // no source service offer to save
        Offer.ServiceOffer savedServiceOffer =
            migrationService.deserializeServiceOffer(resultMigrationOffer.getServiceOffer());

        Assertions.assertThat(savedServiceOffer).isEqualToComparingFieldByFieldRecursively(sourceServiceOffer);

        Offer newOffer =
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(TARGET_BUSINESS_ID, SHOP_SKU)).get(0);
        assertThat(newOffer.getTitle()).isEqualTo("CHANGED!");
        assertThat(newOffer.getServiceOffers()).hasSize(1); // new service offer created
        assertTrue(newOffer.getServiceOffer(supplier.getId()).isPresent());
        Offer.ServiceOffer resultServiceOffer = newOffer.getServiceOffer(supplier.getId()).orElseThrow();
        Assertions.assertThat(resultServiceOffer).isEqualToComparingFieldByFieldRecursively(sourceServiceOffer);

        Offer newSourceOffer =
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(sourceBusiness.getId(), SHOP_SKU)).get(0);
        assertThat(newSourceOffer.getTitle()).isEqualTo(DEFAULT_TITLE); // not changed
        assertFalse(newSourceOffer.getServiceOffer(supplier.getId()).isPresent()); // service offer removed
        assertTrue(newSourceOffer.getServiceOffer(otherSupplier.getId()).isPresent()); // other supplier is present
    }

    @Test
    public void testUpdateWithoutMigrationService() {
        prepareDefaultMigration(targetBusiness.getId(), sourceBusiness.getId(), supplier.getId());
        Offer sourceOffer = offer(sourceBusiness.getId(), supplier);

        offerRepository.insertOffer(sourceOffer);

        Supplier otherSupplier = new Supplier(10003,
            "sup other",
            "sup.biz",
            "biz",
            MbocSupplierType.THIRD_PARTY);

        supplierRepository.insert(otherSupplier);
        Offer targetOffer = offer(targetBusiness.getId(), otherSupplier);
        offerRepository.insertOffer(targetOffer);

        DatacampContext datacampContext = getMigrationDatacampContext(targetBusiness, otherSupplier, SHOP_SKU);

        MboMappings.ProviderProductInfo productInfo = blueProductInfo()
            .setShopId(targetBusiness.getId())
            .setShopSkuId(SHOP_SKU)
            .setTitle("CHANGED!")
            .build();

        MboMappings.ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(productInfo)
                .build(), datacampContext, AddProductInfoListener.NOOP);

        assertThat(response.getStatus()).isEqualTo(MboMappings.ProviderProductInfoResponse.Status.OK);

        MigrationOffer resultMigrationOffer = migrationOfferRepository.getById(MIGRATION_OFFER_ID);
        assertThat(resultMigrationOffer.getState()).isEqualTo(MigrationOfferState.NEW);

        Offer newOffer =
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(TARGET_BUSINESS_ID, SHOP_SKU)).get(0);
        assertThat(newOffer.getTitle()).isEqualTo("CHANGED!");
        assertThat(newOffer.getServiceOffers()).hasSize(1); // new service offer created
        assertTrue(newOffer.getServiceOffer(otherSupplier.getId()).isPresent());

        Offer newSourceOffer =
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(sourceBusiness.getId(), SHOP_SKU)).get(0);
        assertThat(newSourceOffer.getTitle()).isEqualTo(DEFAULT_TITLE); // not changed
        assertTrue(newSourceOffer.getServiceOffer(supplier.getId()).isPresent());
    }

    @Test
    public void testMigrateReplaceTargetOfferWhenSourceResolution() {
        prepareDefaultMigration(targetBusiness.getId(), sourceBusiness.getId(), supplier.getId());
        Offer sourceOffer = offer(sourceBusiness.getId(), supplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION);
        offerRepository.insertOffer(sourceOffer);

        Supplier targetOtherSupplier = new Supplier(10004,
            "target other",
            "sup.biz",
            "biz",
            MbocSupplierType.THIRD_PARTY);
        targetOtherSupplier.setBusinessId(targetBusiness.getId());
        supplierRepository.insert(targetOtherSupplier);

        Offer targetOffer = offer(targetBusiness.getId(), targetOtherSupplier)
            .setProcessingStatusInternal(Offer.ProcessingStatus.OPEN);
        offerRepository.insertOffer(targetOffer);

        var migrationOffer =
            migrationOfferRepository.getById(MIGRATION_OFFER_ID).setResolution(MigrationOfferResolution.SOURCE);
        migrationOfferRepository.save(migrationOffer);

        DatacampContext datacampContext = getMigrationDatacampContext(targetBusiness, supplier, SHOP_SKU);

        MboMappings.ProviderProductInfo productInfo = blueProductInfo()
            .setShopId(targetBusiness.getId())
            .setShopSkuId(SHOP_SKU)
            .setTitle("CHANGED!")
            .build();

        MboMappings.ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(productInfo)
                .build(), datacampContext, AddProductInfoListener.NOOP);

        assertThat(response.getStatus()).isEqualTo(MboMappings.ProviderProductInfoResponse.Status.OK);

        MigrationOffer resultMigrationOffer = migrationOfferRepository.getById(MIGRATION_OFFER_ID);
        assertThat(resultMigrationOffer.getState()).isEqualTo(MigrationOfferState.RECEIVED);

        Offer updatedTargetOffer =
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(TARGET_BUSINESS_ID, SHOP_SKU)).get(0);
        assertThat(updatedTargetOffer.getTitle()).isEqualTo("CHANGED!");
        assertThat(updatedTargetOffer.getServiceOffers()).hasSize(2);
        assertTrue(updatedTargetOffer.getServiceOffer(supplier.getId()).isPresent());

        assertEquals(Offer.ProcessingStatus.IN_CLASSIFICATION, updatedTargetOffer.getProcessingStatus());

        Offer updatedSourceOffer =
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(sourceBusiness.getId(), SHOP_SKU)).get(0);
        assertThat(updatedSourceOffer.getTitle()).isEqualTo(DEFAULT_TITLE); // not changed
        assertFalse(updatedSourceOffer.getServiceOffer(supplier.getId()).isPresent()); // service offer removed
        assertEquals(Offer.ProcessingStatus.IN_CLASSIFICATION, updatedSourceOffer.getProcessingStatus());
    }

    @Test
    public void testMigrateSourceResolutionSourceOfferNotFound() {
        prepareDefaultMigration(targetBusiness.getId(), sourceBusiness.getId(), supplier.getId());

        Supplier targetOtherSupplier = new Supplier(10004,
            "target other",
            "sup.biz",
            "biz",
            MbocSupplierType.THIRD_PARTY);
        targetOtherSupplier.setBusinessId(targetBusiness.getId());
        supplierRepository.insert(targetOtherSupplier);

        var migrationOffer =
            migrationOfferRepository.getById(MIGRATION_OFFER_ID).setResolution(MigrationOfferResolution.SOURCE);
        migrationOfferRepository.save(migrationOffer);

        DatacampContext datacampContext = getMigrationDatacampContext(targetBusiness, supplier, SHOP_SKU);

        MboMappings.ProviderProductInfo productInfo = blueProductInfo()
            .setShopId(targetBusiness.getId())
            .setShopSkuId(SHOP_SKU)
            .setTitle("CHANGED!")
            .build();

        MboMappings.ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(productInfo)
                .build(), datacampContext, AddProductInfoListener.NOOP);

        assertThat(response.getStatus()).isEqualTo(MboMappings.ProviderProductInfoResponse.Status.OK);

        // migration offer marked as received
        MigrationOffer resultMigrationOffer = migrationOfferRepository.getById(MIGRATION_OFFER_ID);
        assertThat(resultMigrationOffer.getState()).isEqualTo(MigrationOfferState.RECEIVED);
        assertThat(resultMigrationOffer.getErrorText()).isEqualTo("source offer not found");

        Offer updatedTargetOffer =
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(TARGET_BUSINESS_ID, SHOP_SKU)).get(0);
        assertThat(updatedTargetOffer.getTitle()).isEqualTo("CHANGED!");
        assertThat(updatedTargetOffer.getServiceOffers()).hasSize(1);
        assertTrue(updatedTargetOffer.getServiceOffer(supplier.getId()).isPresent());

        assertEquals(Offer.ProcessingStatus.OPEN, updatedTargetOffer.getProcessingStatus());
    }

    @Test
    public void testRemoveOfferInitialMigration() {
        prepareDefaultMigration(targetBusiness.getId(), supplier.getId(), supplier.getId(),
            MigrationOfferResolution.TARGET);

        Offer sourceOffer = offer(supplier.getId(), supplier);
        Offer.ServiceOffer sourceServiceOffer = sourceOffer.getServiceOffer(supplier.getId()).orElseThrow();

        offerRepository.insertOffer(sourceOffer);

        Assertions.assertThat(
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(supplier.getId(), SHOP_SKU))).isNotEmpty();

        DatacampContext datacampContext = getMigrationDatacampContext(targetBusiness, supplier, SHOP_SKU);

        MboMappings.ProviderProductInfo productInfo = blueProductInfo()
            .setShopId(targetBusiness.getId())
            .setShopSkuId(SHOP_SKU)
            .setTitle("CHANGED!")
            .build();

        MboMappings.ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(productInfo)
                .build(), datacampContext, AddProductInfoListener.NOOP);

        assertThat(response.getStatus()).isEqualTo(MboMappings.ProviderProductInfoResponse.Status.OK);

        MigrationOffer resultMigrationOffer = migrationOfferRepository.getById(MIGRATION_OFFER_ID);
        assertThat(resultMigrationOffer.getState()).isEqualTo(MigrationOfferState.RECEIVED);
        assertThat(resultMigrationOffer.getServiceOffer()).isNotNull(); // no source service offer to save
        Offer.ServiceOffer savedServiceOffer =
            migrationService.deserializeServiceOffer(resultMigrationOffer.getServiceOffer());

        Assertions.assertThat(savedServiceOffer).isEqualToComparingFieldByFieldRecursively(sourceServiceOffer);

        Offer newOffer =
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(TARGET_BUSINESS_ID, SHOP_SKU)).get(0);
        assertThat(newOffer.getTitle()).isEqualTo("CHANGED!");
        assertThat(newOffer.getServiceOffers()).hasSize(1); // new service offer created
        assertTrue(newOffer.getServiceOffer(supplier.getId()).isPresent());
        Offer.ServiceOffer resultServiceOffer = newOffer.getServiceOffer(supplier.getId()).orElseThrow();
        Assertions.assertThat(resultServiceOffer).isEqualToComparingFieldByFieldRecursively(sourceServiceOffer);

        // offer is removed
        Assertions.assertThat(
            offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(supplier.getId(), SHOP_SKU))).isEmpty();

        List<MigrationRemovedOffer> migrationRemovedOffers = migrationRemovedOfferRepository.findAll();
        Assertions.assertThat(migrationRemovedOffers).hasSize(1);
        MigrationRemovedOffer migrationRemovedOffer = migrationRemovedOffers.get(0);
        assertEquals(MIGRATION_OFFER_ID, migrationRemovedOffer.getMigrationOfferId());

        String json = migrationRemovedOffer.getRemovedOffer().data();
        Assertions.assertThat(json).isNotEmpty();
        Assertions.assertThat(json).contains(String.valueOf(sourceOffer.getId()));

        Assertions.assertThat(migrationRemovedOffer.getRemovedOfferContent()).isNotNull();
    }

    @Test
    public void testAntiMappingMigration() {
        prepareDefaultMigration(targetBusiness.getId(), sourceBusiness.getId(), supplier.getId());
        Offer sourceOffer = offer(sourceBusiness.getId(), supplier)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION);
        offerRepository.insertOffer(sourceOffer);

        AntiMapping source1 = saveAntiMapping(sourceOffer, 1L);
        AntiMapping source2 = saveAntiMapping(sourceOffer, 2L);
        AntiMapping source3 = saveAntiMapping(sourceOffer, 10L);

        Supplier targetOtherSupplier = new Supplier(10004,
            "target other",
            "sup.biz",
            "biz",
            MbocSupplierType.THIRD_PARTY);
        targetOtherSupplier.setBusinessId(targetBusiness.getId());
        supplierRepository.insert(targetOtherSupplier);

        Offer targetOffer = offer(targetBusiness.getId(), targetOtherSupplier)
            .setProcessingStatusInternal(Offer.ProcessingStatus.OPEN);
        offerRepository.insertOffer(targetOffer);

        saveAntiMapping(targetOffer, 1L);
        saveAntiMapping(targetOffer, 5L);
        saveAntiMapping(targetOffer, 10L, true);

        var migrationOffer =
            migrationOfferRepository.getById(MIGRATION_OFFER_ID).setResolution(MigrationOfferResolution.TARGET);
        migrationOfferRepository.save(migrationOffer);

        DatacampContext datacampContext = getMigrationDatacampContext(targetBusiness, supplier, SHOP_SKU);

        MboMappings.ProviderProductInfo productInfo = blueProductInfo()
            .setShopId(targetBusiness.getId())
            .setShopSkuId(SHOP_SKU)
            .setTitle("CHANGED!")
            .build();

        MboMappings.ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(productInfo)
                .build(), datacampContext, AddProductInfoListener.NOOP);

        assertThat(response.getStatus()).isEqualTo(MboMappings.ProviderProductInfoResponse.Status.OK);

        MigrationOffer resultMigrationOffer = migrationOfferRepository.getById(MIGRATION_OFFER_ID);
        assertThat(resultMigrationOffer.getState()).isEqualTo(MigrationOfferState.RECEIVED);

        List<AntiMapping> source =
            antiMappingRepository.findByFilter(AntiMappingRepository.newFilter().setOfferIds(sourceOffer.getId())
                .setExcludeDeleted(true));
        Assertions.assertThat(source).containsExactlyInAnyOrder(source1, source2, source3);

        List<AntiMapping> target =
            antiMappingRepository.findByFilter(AntiMappingRepository.newFilter().setOfferIds(targetOffer.getId())
                .setExcludeDeleted(true));
        Assertions.assertThat(target).extracting(AntiMapping::getNotSkuId).containsExactlyInAnyOrder(1L, 2L, 5L);
    }

    private AntiMapping saveAntiMapping(Offer offer, Long skuId) {
        return saveAntiMapping(offer, skuId, false);
    }

    private AntiMapping saveAntiMapping(Offer offer, Long skuId, boolean deleted) {
        AntiMapping antiMapping = new AntiMapping()
            .setOfferId(offer.getId())
            .setNotModelId(OfferTestUtils.TEST_MODEL_ID)
            .setNotSkuId(skuId)
            .setSourceType(AntiMapping.SourceType.MODERATION_REJECT)
            .setCreatedTs(Instant.now().minus(3, ChronoUnit.DAYS))
            .setUpdatedTs(Instant.now().minus(2, ChronoUnit.DAYS))
            .setUpdatedUser("test user updated")
            .markNeedsUpload();

        if (deleted) {
            antiMapping.setDeletedTs(Instant.now());
            antiMapping.setDeletedUser("deleted user");
        }

        return antiMappingRepository.insert(antiMapping);
    }

    private void prepareDefaultMigration(int target, int source, int supplierId, MigrationOfferResolution resolution) {
        MigrationStatus migrationStatus = new MigrationStatus()
            .setId(MIGRATION_ID)
            .setTargetBusinessId(target)
            .setSourceBusinessId(source)
            .setSupplierId(supplierId)
            .setCreatedTs(Instant.now())
            .setMigrationStatus(MigrationStatusType.ACTIVE);
        migrationStatusRepository.save(migrationStatus);
        MigrationOffer migrationOffer = new MigrationOffer()
            .setId(MIGRATION_OFFER_ID)
            .setMigrationId(MIGRATION_ID)
            .setShopSku(SHOP_SKU)
            .setResolution(resolution)
            .setState(MigrationOfferState.NEW);
        migrationOfferRepository.save(migrationOffer);

        migrationService.checkAndUpdateCache();
    }

    private void prepareDefaultMigration(int target, int source, int supplierId) {
        prepareDefaultMigration(target, source, supplierId, MigrationOfferResolution.SOURCE);
    }

    private DatacampContext getMigrationDatacampContext(Supplier targetBusiness, Supplier supplier, String shopSku) {
        return DatacampContext.builder()
            .processDataCampData(true)
            .enrichedOffers(Map.of(new BusinessSkuKey(targetBusiness.getId(), shopSku),
                UltraController.EnrichedOffer.newBuilder()
                    .setMarketSkuPublishedOnBlueMarket(true)
                    .setMarketSkuPublishedOnMarket(true)
                    .setCategoryId(21)
                    .setMarketCategoryName("category-name")
                    .setModelId(22)
                    .setMarketModelName("model-name")
                    .setMarketSkuId(23)
                    .setMarketSkuName("sku-name")
                    .setVendorId(25)
                    .setMarketVendorName("UC-Vendor-name")
                    .setEnrichType(UltraController.EnrichedOffer.EnrichType.APPROVED_MODEL)
                    .setSkutchType(SkuBDApi.SkutchType.SKUTCH_BY_MODEL_ID)
                    .setClassifierCategoryId(34)
                    .setClassifierConfidentTopPrecision(0.1)
                    .setMatchedId(22)
                    .build()))
            .suppliers(Map.of(supplier.getId(), supplier, targetBusiness.getId(), targetBusiness))
            .offersAdditionalData(Map.of(new BusinessSkuKey(targetBusiness.getId(), shopSku),
                AdditionalData.builder()
                    .extraShopFields(Map.of(
                        "PRICE", "21.1",
                        "OTHER_PARAM", "hello there",
                        ExcelHeaders.PRICE.getTitle(), "10.23"
                    ))
                    .isDataCampOffer(true)
                    .dataCampContentVersion(100500L)
                    .picUrls("pic1\npic2\npic3")
                    .sourcePicUrls("sourcePic0\nsourcePic1\nsourcePic2")
                    .supplierIds(ImmutableSet.of(supplier.getId()))
                    .unitedSize("40/182")
                    .build()
            ))
            .dcOffers(Map.of(new BusinessSkuKey(
                targetBusiness.getId(), shopSku), DataCampOffer.Offer.newBuilder().build()))
            .build();
    }

    private Offer offer(int businessId, Supplier supplier) {
        return new Offer()
            .setBusinessId(businessId)
            .setShopSku(SHOP_SKU)
            .setTitle(DEFAULT_TITLE)
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setShopCategoryName("DEFAULT_SHOP_CATEGORY_NAME")
            .addNewServiceOfferIfNotExistsForTests(supplier);
    }
}
