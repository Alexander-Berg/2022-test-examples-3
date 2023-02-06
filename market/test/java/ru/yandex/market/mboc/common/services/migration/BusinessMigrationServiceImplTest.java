package ru.yandex.market.mboc.common.services.migration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPictures;
import Market.DataCamp.DataCampOfferStatus;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.PartnerCategoryOuterClass;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.business.migration.BusinessMigration;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.common.datacamp.DataCampOfferUtil;
import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.datacamp.service.DataCampIdentifiersService;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationOfferResolution;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationOfferState;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationStatusType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationOffer;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationStatus;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.MigrationOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationRemovedOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationStatusRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferMetaRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferUpdateSequenceService;
import ru.yandex.market.mboc.common.services.datacamp.OfferGenerationHelper;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

public class BusinessMigrationServiceImplTest extends BaseDbTestClass {
    private static final Integer DST_BIZ = 1;
    private static final Integer SRC_BIZ = 2;
    private static final Integer BLUE_SHOP_ID = 3;
    private static final Integer WHITE_SHOP_ID = 4;

    private static final int CATEGORY_ID = 1;
    private static final int MODEL_ID = 109;
    private static final String DEFAULT_SHOP_SKU = "ssku";

    @Autowired
    private MigrationStatusRepository migrationStatusRepository;
    @Autowired
    private MigrationOfferRepository migrationOfferRepository;
    @Autowired
    private MigrationRemovedOfferRepository migrationRemovedOfferRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private OfferMetaRepository offerMetaRepository;
    @Autowired
    protected OfferUpdateSequenceService offerUpdateSequenceService;

    private MigrationService migrationService;
    private BusinessMigrationServiceImpl businessMigrationService;

    @Before
    public void setUp() {
        SupplierConverterServiceMock supplierConverterService = new SupplierConverterServiceMock();
        DataCampIdentifiersService dataCampIdentifiersService = new DataCampIdentifiersService(
            SupplierConverterServiceMock.BERU_ID, SupplierConverterServiceMock.BERU_BUSINESS_ID,
            supplierConverterService);

        SupplierService supplierService = new SupplierService(supplierRepository);
        migrationService = new MigrationService(migrationStatusRepository, migrationOfferRepository,
            migrationRemovedOfferRepository, supplierRepository, offerUpdateSequenceService, offerMetaRepository);
        businessMigrationService = new BusinessMigrationServiceImpl(migrationService, migrationStatusRepository,
            transactionHelper, dataCampIdentifiersService, supplierService,
            Mockito.mock(ComplexMonitoring.class), migrationOfferRepository, offerRepository);

        Supplier targetBusiness = new Supplier(DST_BIZ,
            "target biz",
            "biz.biz",
            "biz org",
            MbocSupplierType.BUSINESS);
        Supplier sourceBusiness = new Supplier(SRC_BIZ,
            "source biz",
            "biz.biz",
            "biz org",
            MbocSupplierType.BUSINESS);
        Supplier supplier = new Supplier(BLUE_SHOP_ID,
            "sup",
            "sup.biz",
            "biz org",
            MbocSupplierType.THIRD_PARTY)
            .setBusinessId(SRC_BIZ);
        Supplier whiteSupplier = new Supplier(WHITE_SHOP_ID,
            "sup",
            "sup.biz",
            "biz org",
            MbocSupplierType.MARKET_SHOP)
            .setBusinessId(SRC_BIZ);
        supplierRepository.insertBatch(targetBusiness, sourceBusiness, supplier, whiteSupplier);

        migrationService.checkAndUpdateCache();
    }

    @Test
    public void testLockMigration() {
        List<BusinessMigration.LockBusinessResponse> responses = new ArrayList<>();
        var observer = lockObserver(responses);

        var request = BusinessMigration.LockBusinessRequest.newBuilder()
            .setDstBusinessId(DST_BIZ)
            .setSrcBusinessId(SRC_BIZ)
            .setShopId(BLUE_SHOP_ID)
            .build();

        businessMigrationService.lock(request, observer);

        Assertions.assertThat(responses).hasSize(1);
        var response = responses.get(0);
        Assert.assertEquals(BusinessMigration.Status.IN_PROGRESS, response.getStatus());
        Assertions.assertThat(response.getMessage()).isEmpty();

        List<MigrationStatus> all = migrationStatusRepository.findAll();
        Assertions.assertThat(all).hasSize(1);
        var migrationStatus = all.get(0);
        Assert.assertEquals(DST_BIZ, migrationStatus.getTargetBusinessId());
        Assert.assertEquals(SRC_BIZ, migrationStatus.getSourceBusinessId());
        Assert.assertEquals(BLUE_SHOP_ID, migrationStatus.getSupplierId());
        Assert.assertEquals(MigrationStatusType.NEW, migrationStatus.getMigrationStatus());

        // still in progress
        responses.clear();
        businessMigrationService.lock(request, observer);
        Assertions.assertThat(responses).hasSize(1);
        response = responses.get(0);
        Assert.assertEquals(BusinessMigration.Status.IN_PROGRESS, response.getStatus());

        // to the future
        hackToTheFuture();
        migrationService.checkAndUpdateCache();
        responses.clear();
        businessMigrationService.lock(request, observer);
        Assertions.assertThat(responses).hasSize(1);
        response = responses.get(0);
        Assert.assertEquals(BusinessMigration.Status.SUCCESS, response.getStatus());

        all = migrationStatusRepository.findAll();
        Assertions.assertThat(all).hasSize(1);
        migrationStatus = all.get(0);
        Assert.assertEquals(MigrationStatusType.ACTIVE, migrationStatus.getMigrationStatus());

        responses.clear();
        businessMigrationService.lock(request, observer);
        Assertions.assertThat(responses).hasSize(1);
        response = responses.get(0);
        Assert.assertEquals(BusinessMigration.Status.FAIL, response.getStatus());
        Assert.assertEquals("Migration is already started", response.getMessage());
    }

    @Test
    public void testLockSupplierNotFound() {
        List<BusinessMigration.LockBusinessResponse> responses = new ArrayList<>();
        var observer = lockObserver(responses);

        var request = BusinessMigration.LockBusinessRequest.newBuilder()
            .setDstBusinessId(DST_BIZ)
            .setSrcBusinessId(SRC_BIZ)
            .setShopId(1232342)
            .build();

        businessMigrationService.lock(request, observer);

        Assertions.assertThat(responses).hasSize(1);
        var response = responses.get(0);
        Assert.assertEquals(BusinessMigration.Status.IN_PROGRESS, response.getStatus());
        Assertions.assertThat(response.getMessage()).isEmpty();

        List<MigrationStatus> all = migrationStatusRepository.findAll();
        Assertions.assertThat(all).isEmpty();
    }

    @Test
    public void testLockMigrationInitialBlue() {
        var supplier = supplierRepository.findById(BLUE_SHOP_ID);
        supplier.setBusinessId(null);
        supplierRepository.update(supplier);

        List<BusinessMigration.LockBusinessResponse> responses = new ArrayList<>();
        var observer = lockObserver(responses);

        var request = BusinessMigration.LockBusinessRequest.newBuilder()
            .setDstBusinessId(DST_BIZ)
            .setSrcBusinessId(DST_BIZ)
            .setShopId(BLUE_SHOP_ID)
            .build();
        businessMigrationService.lock(request, observer);

        Assertions.assertThat(responses).hasSize(1);
        var response = responses.get(0);
        Assert.assertEquals(BusinessMigration.Status.IN_PROGRESS, response.getStatus());
        Assertions.assertThat(response.getMessage()).isEmpty();

        List<MigrationStatus> all = migrationStatusRepository.findAll();
        Assertions.assertThat(all).hasSize(1);
        var migrationStatus = all.get(0);
        Assert.assertEquals(DST_BIZ, migrationStatus.getTargetBusinessId());
        Assert.assertEquals(BLUE_SHOP_ID, migrationStatus.getSourceBusinessId()); // src business is set to supplier
        Assert.assertEquals(BLUE_SHOP_ID, migrationStatus.getSupplierId());
        Assert.assertEquals(MigrationStatusType.NEW, migrationStatus.getMigrationStatus());
    }


    @Test
    public void testMergeNotDryRun() {
        List<BusinessMigration.MergeOffersResponse> responses = new ArrayList<>();
        var observer = mergeObserver(responses);

        var migrationStatus = startMigration(DST_BIZ, SRC_BIZ, BLUE_SHOP_ID);

        var basicOffer = offer(DST_BIZ);
        var unitedOffer = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(basicOffer);

        var request = BusinessMigration.MergeOffersRequest.newBuilder()
            .addMergeRequestItem(BusinessMigration.MergeOffersRequestItem.newBuilder()
                .setResult(unitedOffer)
                .setConflictResolutionStrategy(BusinessMigration.ConflictResolutionStrategy.ACCEPT_TARGET)
                .build())
            .setDryRun(false)
            .build();
        businessMigrationService.merge(request, observer);

        Assertions.assertThat(responses).hasSize(1);
        var response = responses.get(0);
        Assert.assertTrue(response.getSuccess());
        var items = response.getMergeResponseItemList();
        Assertions.assertThat(items).hasSize(1);
        var item = items.get(0);
        Assert.assertEquals(BusinessMigration.Resolution.USE_TARGET, item.getResolution());
        Assertions.assertThat(item.getResult()).isEqualToComparingFieldByFieldRecursively(unitedOffer.build());

        var offers = migrationOfferRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        var offer = offers.get(0);
        Assert.assertEquals(migrationStatus.getId(), offer.getMigrationId());
        Assert.assertEquals(MigrationOfferState.NEW, offer.getState());
        Assert.assertEquals(basicOffer.getIdentifiers().getOfferId(), offer.getShopSku());
        Assert.assertEquals(MigrationOfferResolution.TARGET, offer.getResolution());
        Assertions.assertThat(offer.getErrorText()).isNull();
    }

    @Test
    public void testMergeWrongTargetDestination() {
        enableNotAllowWrongTargetResolution();
        List<BusinessMigration.MergeOffersResponse> responses = new ArrayList<>();
        var observer = mergeObserver(responses);

        var migrationStatus = startMigration(DST_BIZ, BLUE_SHOP_ID, BLUE_SHOP_ID);

        var basicOffer = offer(DST_BIZ);
        var unitedOffer = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(basicOffer);

        var request = BusinessMigration.MergeOffersRequest.newBuilder()
            .addMergeRequestItem(BusinessMigration.MergeOffersRequestItem.newBuilder()
                .setResult(unitedOffer)
                .setConflictResolutionStrategy(BusinessMigration.ConflictResolutionStrategy.ACCEPT_TARGET)
                .build())
            .setDryRun(false)
            .build();
        businessMigrationService.merge(request, observer);

        Assertions.assertThat(responses).hasSize(1);
        var response = responses.get(0);
        Assert.assertTrue(response.getSuccess());
        var items = response.getMergeResponseItemList();
        Assertions.assertThat(items).hasSize(1);
        var item = items.get(0);
        Assert.assertEquals(BusinessMigration.Resolution.USE_SOURCE, item.getResolution());
        Assertions.assertThat(item.getResult()).isEqualToComparingFieldByFieldRecursively(unitedOffer.build());

        var offers = migrationOfferRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        var offer = offers.get(0);
        Assert.assertEquals(migrationStatus.getId(), offer.getMigrationId());
        Assert.assertEquals(MigrationOfferState.NEW, offer.getState());
        Assert.assertEquals(basicOffer.getIdentifiers().getOfferId(), offer.getShopSku());
        Assert.assertEquals(MigrationOfferResolution.SOURCE, offer.getResolution());
        Assertions.assertThat(offer.getErrorText()).isNull();
    }

    @Test
    public void testWhiteMergeDryRun() {
        List<BusinessMigration.MergeOffersResponse> responses = new ArrayList<>();
        var observer = mergeObserver(responses);

        startMigration(DST_BIZ, SRC_BIZ, WHITE_SHOP_ID);

        var basicOffer = offer(DST_BIZ);
        var unitedOffer = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(basicOffer);

        var request = BusinessMigration.MergeOffersRequest.newBuilder()
            .addMergeRequestItem(BusinessMigration.MergeOffersRequestItem.newBuilder()
                .setResult(unitedOffer)
                .build())
            .setDryRun(true)
            .build();
        businessMigrationService.merge(request, observer);

        Assertions.assertThat(responses).hasSize(1);
        var response = responses.get(0);
        Assert.assertTrue(response.getSuccess());
        var items = response.getMergeResponseItemList();
        Assertions.assertThat(items).hasSize(1);
        var item = items.get(0);
        Assert.assertEquals(BusinessMigration.Resolution.MERGEABLE, item.getResolution());
        Assertions.assertThat(item.getResult()).isEqualToComparingFieldByFieldRecursively(unitedOffer.build());

        var offers = migrationOfferRepository.findAll();
        Assertions.assertThat(offers).isEmpty();
    }

    @Test
    public void testUnlock() {
        List<BusinessMigration.UnlockBusinessResponse> responses = new ArrayList<>();
        var observer = unlockObserver(responses);

        MigrationStatus migrationStatus = startMigration(DST_BIZ, SRC_BIZ, WHITE_SHOP_ID);

        MigrationOffer migrationOffer = new MigrationOffer()
            .setMigrationId(migrationStatus.getId())
            .setShopSku("shopSku")
            .setState(MigrationOfferState.NEW);
        migrationOffer = migrationOfferRepository.save(migrationOffer);

        var request = BusinessMigration.UnlockBusinessRequest.newBuilder()
            .setDstBusinessId(DST_BIZ)
            .setSrcBusinessId(SRC_BIZ)
            .setShopId(WHITE_SHOP_ID)
            .build();
        businessMigrationService.unlock(request, observer);

        Assertions.assertThat(responses).hasSize(1);
        var response = responses.get(0);
        Assert.assertEquals(BusinessMigration.Status.IN_PROGRESS, response.getStatus());

        var newMigrationStatus = migrationStatusRepository.getById(migrationStatus.getId());
        var newMigrationOffer = migrationOfferRepository.getById(migrationOffer.getId());
        Assert.assertEquals(MigrationStatusType.RECEIVING, newMigrationStatus.getMigrationStatus());
        Assert.assertEquals(MigrationOfferState.NEW, newMigrationOffer.getState());

        migrationStatus = migrationStatusRepository.getById(migrationStatus.getId());
        migrationStatus.setMigrationStatus(MigrationStatusType.RECEIVED);
        migrationStatusRepository.save(migrationStatus);

        responses.clear();
        businessMigrationService.unlock(request, observer);

        Assertions.assertThat(responses).hasSize(1);
        response = responses.get(0);
        Assert.assertEquals(BusinessMigration.Status.IN_PROGRESS, response.getStatus());

        updateSupplier(WHITE_SHOP_ID);

        responses.clear();
        businessMigrationService.unlock(request, observer);

        Assertions.assertThat(responses).hasSize(1);
        response = responses.get(0);
        Assert.assertEquals(BusinessMigration.Status.SUCCESS, response.getStatus());

        migrationStatus = migrationStatusRepository.getById(migrationStatus.getId());
        migrationStatus.setMigrationStatus(MigrationStatusType.FINISHED);
    }

    @Test
    public void testUnlockCancel() {
        List<BusinessMigration.UnlockBusinessResponse> responses = new ArrayList<>();
        var observer = unlockObserver(responses);

        MigrationStatus migrationStatus = startMigration(DST_BIZ, SRC_BIZ, WHITE_SHOP_ID);

        MigrationOffer migrationOffer = new MigrationOffer()
            .setMigrationId(migrationStatus.getId())
            .setShopSku("shopSku")
            .setState(MigrationOfferState.NEW); // migration offer is still new
        migrationOfferRepository.save(migrationOffer);

        var request = BusinessMigration.UnlockBusinessRequest.newBuilder()
            .setDstBusinessId(DST_BIZ)
            .setSrcBusinessId(SRC_BIZ)
            .setShopId(WHITE_SHOP_ID)
            .setCancel(true)
            .build();

        businessMigrationService.unlock(request, observer);

        Assertions.assertThat(responses).hasSize(1);
        var response = responses.get(0);
        Assert.assertEquals(BusinessMigration.Status.SUCCESS, response.getStatus());

        var newMigrationStatus = migrationStatusRepository.getById(migrationStatus.getId());
        Assert.assertEquals(MigrationStatusType.CANCELLED, newMigrationStatus.getMigrationStatus());
    }

    @Test
    public void testUnlockCannotCancel() {
        List<BusinessMigration.UnlockBusinessResponse> responses = new ArrayList<>();
        var observer = unlockObserver(responses);

        MigrationStatus migrationStatus = startMigration(DST_BIZ, SRC_BIZ, WHITE_SHOP_ID);

        MigrationOffer migrationOffer = new MigrationOffer()
            .setMigrationId(migrationStatus.getId())
            .setShopSku("shopSku")
            .setState(MigrationOfferState.RECEIVED); // migration offer is already received
        migrationOfferRepository.save(migrationOffer);

        var request = BusinessMigration.UnlockBusinessRequest.newBuilder()
            .setDstBusinessId(DST_BIZ)
            .setSrcBusinessId(SRC_BIZ)
            .setShopId(WHITE_SHOP_ID)
            .setCancel(true)
            .build();

        businessMigrationService.unlock(request, observer);

        Assertions.assertThat(responses).hasSize(1);
        var response = responses.get(0);
        Assert.assertEquals(BusinessMigration.Status.FAIL, response.getStatus());

        var newMigrationStatus = migrationStatusRepository.getById(migrationStatus.getId());
        Assert.assertEquals(MigrationStatusType.ACTIVE, newMigrationStatus.getMigrationStatus());
    }

    @Test
    public void testAsyncFinish() {
        enableAllowUnlockBeforeOffersReceive();

        List<BusinessMigration.AsyncFinishBusinessResponse> responses = new ArrayList<>();
        var observer = asyncFinishObserver(responses);

        MigrationStatus migrationStatus = startMigration(DST_BIZ, SRC_BIZ, WHITE_SHOP_ID);

        var request = BusinessMigration.AsyncFinishBusinessRequest.newBuilder()
            .setDstBusinessId(DST_BIZ)
            .setSrcBusinessId(SRC_BIZ)
            .setShopId(WHITE_SHOP_ID)
            .build();

        businessMigrationService.asyncFinish(request, observer);

        Assertions.assertThat(responses).hasSize(1);
        var response = responses.get(0);
        Assert.assertEquals(BusinessMigration.Status.IN_PROGRESS, response.getStatus());

        migrationStatus = migrationStatusRepository.getById(migrationStatus.getId());
        migrationStatus.setMigrationStatus(MigrationStatusType.RECEIVING);
        migrationStatusRepository.save(migrationStatus);

        responses.clear();
        businessMigrationService.asyncFinish(request, observer);

        Assertions.assertThat(responses).hasSize(1);
        response = responses.get(0);
        Assert.assertEquals(BusinessMigration.Status.IN_PROGRESS, response.getStatus());

        migrationStatus = migrationStatusRepository.getById(migrationStatus.getId());
        migrationStatus.setMigrationStatus(MigrationStatusType.RECEIVED);
        migrationStatusRepository.save(migrationStatus);

        responses.clear();
        businessMigrationService.asyncFinish(request, observer);

        Assertions.assertThat(responses).hasSize(1);
        response = responses.get(0);
        Assert.assertEquals(BusinessMigration.Status.IN_PROGRESS, response.getStatus());

        updateSupplier(WHITE_SHOP_ID);

        responses.clear();
        businessMigrationService.asyncFinish(request, observer);

        Assertions.assertThat(responses).hasSize(1);
        response = responses.get(0);
        Assert.assertEquals(BusinessMigration.Status.SUCCESS, response.getStatus());

        migrationStatus = migrationStatusRepository.getById(migrationStatus.getId());
        migrationStatus.setMigrationStatus(MigrationStatusType.FINISHED);
    }

    @Test
    public void testMergeDuplicationNotError() {
        List<BusinessMigration.MergeOffersResponse> responses = new ArrayList<>();
        var observer = mergeObserver(responses);

        startMigration(DST_BIZ, SRC_BIZ, BLUE_SHOP_ID);

        var basicOffer = offer(DST_BIZ);
        var unitedOffer = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(basicOffer);

        var request = BusinessMigration.MergeOffersRequest.newBuilder()
            .addMergeRequestItem(BusinessMigration.MergeOffersRequestItem.newBuilder()
                .setResult(unitedOffer)
                .setConflictResolutionStrategy(BusinessMigration.ConflictResolutionStrategy.ACCEPT_TARGET)
                .build())
            .setDryRun(false)
            .build();
        businessMigrationService.merge(request, observer);

        var offers = migrationOfferRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        var offer = offers.get(0);
        Assert.assertEquals(basicOffer.getIdentifiers().getOfferId(), offer.getShopSku());
        Assert.assertEquals(MigrationOfferResolution.TARGET, offer.getResolution());

        responses.clear();
        var request2 = BusinessMigration.MergeOffersRequest.newBuilder()
            .addMergeRequestItem(BusinessMigration.MergeOffersRequestItem.newBuilder()
                .setResult(unitedOffer)
                .setConflictResolutionStrategy(BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE)
                .build())
            .setDryRun(false)
            .build();
        businessMigrationService.merge(request2, observer);
        Assertions.assertThat(responses).hasSize(1);
        var response = responses.get(0);
        Assert.assertTrue(response.getSuccess());

        offers = migrationOfferRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        offer = offers.get(0);
        Assert.assertEquals(basicOffer.getIdentifiers().getOfferId(), offer.getShopSku());
        Assert.assertEquals(MigrationOfferResolution.TARGET, offer.getResolution()); //not changed
    }

    @Test
    public void testMergeOfferShouldNotBeStored() {
        List<BusinessMigration.MergeOffersResponse> responses = new ArrayList<>();
        var observer = mergeObserver(responses);

        startMigration(DST_BIZ, SRC_BIZ, WHITE_SHOP_ID);

        var basicOffer = offer(DST_BIZ);
        var unitedOffer = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(basicOffer);

        var request = BusinessMigration.MergeOffersRequest.newBuilder()
            .addMergeRequestItem(BusinessMigration.MergeOffersRequestItem.newBuilder()
                .setResult(unitedOffer)
                .build())
            .setDryRun(false)
            .build();
        businessMigrationService.merge(request, observer);

        Assertions.assertThat(responses).hasSize(1);
        var response = responses.get(0);
        Assert.assertTrue(response.getSuccess());
        var items = response.getMergeResponseItemList();
        Assertions.assertThat(items).hasSize(1);
        var item = items.get(0);
        Assertions.assertThat(item.getResult()).isEqualToComparingFieldByFieldRecursively(unitedOffer.build());

        var offers = migrationOfferRepository.findAll();
        Assertions.assertThat(offers).isEmpty();
    }

    @Test
    public void testWhiteOfferShouldBeStored() {
        List<BusinessMigration.MergeOffersResponse> responses = new ArrayList<>();
        var observer = mergeObserver(responses);

        var migrationStatus = startMigration(DST_BIZ, SRC_BIZ, WHITE_SHOP_ID);

        var basicOffer = offer(DST_BIZ);
        var unitedOffer = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(basicOffer);

        var offer = simpleOffer(SRC_BIZ, supplierRepository.findById(WHITE_SHOP_ID));
        offerRepository.insertOffer(offer);

        var request = BusinessMigration.MergeOffersRequest.newBuilder()
            .addMergeRequestItem(BusinessMigration.MergeOffersRequestItem.newBuilder()
                .setResult(unitedOffer)
                .setConflictResolutionStrategy(BusinessMigration.ConflictResolutionStrategy.ACCEPT_TARGET)
                .build())
            .setDryRun(false)
            .build();
        businessMigrationService.merge(request, observer);

        Assertions.assertThat(responses).hasSize(1);
        var response = responses.get(0);
        Assert.assertTrue(response.getSuccess());
        var items = response.getMergeResponseItemList();
        Assertions.assertThat(items).hasSize(1);
        var item = items.get(0);
        Assertions.assertThat(item.getResult()).isEqualToComparingFieldByFieldRecursively(unitedOffer.build());

        var offers = migrationOfferRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        var migrationOffer = offers.get(0);
        Assert.assertEquals(migrationStatus.getId(), migrationOffer.getMigrationId());
        Assert.assertEquals(MigrationOfferState.NEW, migrationOffer.getState());
        Assert.assertEquals(basicOffer.getIdentifiers().getOfferId(), offer.getShopSku());
        Assert.assertEquals(MigrationOfferResolution.TARGET, migrationOffer.getResolution());
        Assertions.assertThat(migrationOffer.getErrorText()).isNull();
    }

    private MigrationStatus startMigration(int target, int source, int supplier) {
        MigrationStatus migrationStatus = new MigrationStatus()
            .setTargetBusinessId(target)
            .setSourceBusinessId(source)
            .setSupplierId(supplier)
            .setMigrationStatus(MigrationStatusType.ACTIVE);
        return migrationStatusRepository.save(migrationStatus);
    }

    private void updateSupplier(int supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId);
        supplierRepository.update(supplier.setBusinessId(DST_BIZ));
    }

    @SneakyThrows
    private void enableAllowUnlockBeforeOffersReceive() {
        Field field = businessMigrationService.getClass().getDeclaredField("allowUnlockBeforeOffersReceived");
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(businessMigrationService, true);
    }

    @SneakyThrows
    private void enableNotAllowWrongTargetResolution() {
        Field field = businessMigrationService.getClass().getDeclaredField("notAllowWrongTargetResolution");
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(businessMigrationService, true);
    }

    @SneakyThrows
    private void hackToTheFuture() {
        Field field = businessMigrationService.getClass().getDeclaredField("CACHE_WARM_SECONDS_TO_WAIT");
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(businessMigrationService, -1L);
    }

    public static StreamObserver<BusinessMigration.LockBusinessResponse> lockObserver(
       List<BusinessMigration.LockBusinessResponse> responses) {
       return
           new StreamObserver<>() {
               @Override
               public void onNext(BusinessMigration.LockBusinessResponse value) {
                   responses.add(value);
               }

               @Override
               public void onError(Throwable t) {
                   throw new RuntimeException("error");
               }

               @Override
               public void onCompleted() {
//                   do nothing
               }
           };
    }

    public static StreamObserver<BusinessMigration.MergeOffersResponse> mergeObserver(
        List<BusinessMigration.MergeOffersResponse> responses) {
        return
            new StreamObserver<>() {
                @Override
                public void onNext(BusinessMigration.MergeOffersResponse value) {
                    responses.add(value);
                }

                @Override
                public void onError(Throwable t) {
                    throw new RuntimeException("error");
                }

                @Override
                public void onCompleted() {
//                   do nothing
                }
            };
    }

    public static StreamObserver<BusinessMigration.UnlockBusinessResponse> unlockObserver(
        List<BusinessMigration.UnlockBusinessResponse> responses) {
        return
            new StreamObserver<>() {
                @Override
                public void onNext(BusinessMigration.UnlockBusinessResponse value) {
                    responses.add(value);
                }

                @Override
                public void onError(Throwable t) {
                    throw new RuntimeException("error");
                }

                @Override
                public void onCompleted() {
//                   do nothing
                }
            };
    }

    public static StreamObserver<BusinessMigration.AsyncFinishBusinessResponse> asyncFinishObserver(
        List<BusinessMigration.AsyncFinishBusinessResponse> responses) {
        return
            new StreamObserver<>() {
                @Override
                public void onNext(BusinessMigration.AsyncFinishBusinessResponse value) {
                    responses.add(value);
                }

                @Override
                public void onError(Throwable t) {
                    throw new RuntimeException("error");
                }

                @Override
                public void onCompleted() {
//                   do nothing
                }
            };
    }

    private static DataCampOffer.Offer.Builder offer(int businessId) {
        var pictures = OfferBuilder.pictures(List.of(
            Pair.of("pic1", DataCampOfferPictures.MarketPicture.Status.AVAILABLE),
            Pair.of("pic2", DataCampOfferPictures.MarketPicture.Status.AVAILABLE),
            Pair.of("pic3", DataCampOfferPictures.MarketPicture.Status.AVAILABLE)
        ));
        return DataCampOffer.Offer.newBuilder()
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setOfferId(DEFAULT_SHOP_SKU)
                .setBusinessId(businessId))
            .setStatus(DataCampOfferStatus.OfferStatus.newBuilder()
                .setVersion(DataCampOfferStatus.VersionStatus.newBuilder()
                    .setActualContentVersion(DataCampOfferMeta.VersionCounter.newBuilder()
                        .setCounter(100500L)
                        .build())
                    .build())
                .build())
            .setPictures(pictures)
            .setContent(DataCampOfferContent.OfferContent.newBuilder()
                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                    .setPartner(DataCampOfferMapping.Mapping.newBuilder()
                        .setMarketCategoryId(1)
                        .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                            .setTimestamp(DataCampOfferUtil.toTimestamp(DateTimeUtils.instantNow()))
                            .build())
                        .setMarketSkuId(MODEL_ID))
                    .setUcMapping(DataCampOfferMapping.Mapping.newBuilder()
                        .setMarketCategoryId(CATEGORY_ID)
                        .setMarketCategoryName("category-name")
                        .setMarketModelId(22)
                        .setMarketModelName("model-name")
                        .setMarketSkuId(23)
                        .setMarketSkuName("sku-name")
                        .build()))
                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                    .setOriginal(DataCampOfferContent.OriginalSpecification.newBuilder()
                        .setGroupId(DataCampOfferMeta.Ui32Value.newBuilder().setValue(1).build())
                        .build())
                    .setActual(DataCampOfferContent.ProcessedSpecification.newBuilder()
                        .setTitle(OfferGenerationHelper.stringValue("Title"))
                        .setVendor(OfferGenerationHelper.stringValue("Vendor"))
                        .setVendorCode(OfferGenerationHelper.stringValue("VendorCode"))
                        .setBarcode(DataCampOfferMeta.StringListValue.newBuilder()
                            .addValue("Barcode1")
                            .addValue("Barcode2").build())
                        .setDescription(OfferGenerationHelper.stringValue("Description"))
                        .setUrl(OfferGenerationHelper.stringValue("ololol.com"))
                        .setOfferParams(DataCampOfferContent.ProductYmlParams.newBuilder()
                            .addParam(DataCampOfferContent.OfferYmlParam.newBuilder()
                                .setName("PARAM")
                                .setValue("21.1")
                                .build())
                            .addParam(DataCampOfferContent.OfferYmlParam.newBuilder()
                                .setName("OTHER_PARAM")
                                .setValue("hello there"))
                            .build())
                        .setCategory(PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                            .setName("shop-category-name")
                            .build()))
                )
                .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                    .setMarketSkuPublishedOnBlueMarket(true)
                    .setMarketSkuPublishedOnMarket(true)
                    .setVendorId(25)
                    .setVendorName("UC-Vendor-name")
                    .setIrData(DataCampOfferContent.EnrichedOfferSubset.newBuilder()
                        .setEnrichType(Market.UltraControllerServiceData.UltraController
                            .EnrichedOffer.EnrichType.ET_APPROVED_MODEL)
                        .setSkutchType(Market.UltraControllerServiceData.UltraController
                            .EnrichedOffer.SkutchType.SKUTCH_BY_MODEL_ID)
                        .setClassifierCategoryId(34)
                        .setClassifierConfidentTopPercision(0.1)
                        .setMatchedId(22)
                        .build())
                    .build()));
    }

    private static Offer simpleOffer(int businessId, Supplier supplier) {
        return new Offer()
            .setBusinessId(businessId)
            .setShopSku(DEFAULT_SHOP_SKU)
            .setTitle("DEFAULT_TITLE")
            .setShopCategoryName("DEFAULT_SHOP_CATEGORY_NAME")
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setApprovedSkuMappingInternal(new Offer.Mapping(1234, LocalDateTime.now()))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT)
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .addNewServiceOfferIfNotExistsForTests(supplier);
    }
}
