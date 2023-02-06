package ru.yandex.market.deepmind.common.services.statuses;

import java.time.Instant;
import java.util.List;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaFilter;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.stock.MskuStockRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.stock.MskuStockInfo;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.mboc.common.MbocErrors;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.REAL_SUPPLIER;

public class SskuMskuStatusValidationServiceTest extends DeepmindBaseDbTestClass {

    @Resource
    private MskuStockRepository mskuStockRepository;
    @Resource
    private DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Resource
    private SskuStatusRepository sskuStatusRepository;
    @Resource
    private MskuRepository deepmindMskuRepository;
    @Resource
    private MskuStatusRepository mskuStatusRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;


    private SskuMskuStatusValidationService sskuMskuStatusValidationService;

    private MskuStatus mskuStatus1;
    private MskuStatus mskuStatus2;
    private MskuStatus mskuStatusEndOfLife;
    private MskuStatus mskuStatusArchive;

    @Before
    public void setUp() {
        serviceOfferReplicaRepository.save(
            testOffer(1, "shopSku1", 1, SupplierType.REAL_SUPPLIER),
            testOffer(2, "shopSku2", 2, SupplierType.REAL_SUPPLIER),
            testOffer(3, "shopSku3", 3, SupplierType.REAL_SUPPLIER),
            testOffer(4, "shopSku4", 4, SupplierType.REAL_SUPPLIER),
            testOffer(3, "shopSku5", 3, SupplierType.REAL_SUPPLIER),
            testOffer(5, "shopSku-3P", 3, SupplierType.THIRD_PARTY)
        );
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT_FOR_UNIT_TESTS);
        var sskuMskuHelperService = new SskuMskuHelperService(serviceOfferReplicaRepository, sskuStatusRepository,
            mskuStatusRepository);
        sskuMskuStatusValidationService = new SskuMskuStatusValidationServiceImpl(mskuStockRepository,
            serviceOfferReplicaRepository, deepmindSupplierRepository, sskuMskuHelperService);

        deepmindMskuRepository.save(getSimpleMsku(1));
        deepmindMskuRepository.save(getSimpleMsku(2));
        deepmindMskuRepository.save(getSimpleMsku(3));
        deepmindMskuRepository.save(getSimpleMsku(4));
        mskuStatus1 = mskuStatus(1, MskuStatusValue.REGULAR);
        mskuStatus2 = mskuStatus(2, MskuStatusValue.REGULAR);
        mskuStatusEndOfLife = mskuStatus(3, MskuStatusValue.END_OF_LIFE);
        mskuStatusArchive = mskuStatus(4, MskuStatusValue.ARCHIVE);
        mskuStatusRepository.save(mskuStatus1, mskuStatus2, mskuStatusEndOfLife, mskuStatusArchive);

        sskuStatusRepository.save(
            sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE),
            sskuStatus(2, "shopSku2", OfferAvailability.ACTIVE),
            sskuStatus(3, "shopSku5", OfferAvailability.DELISTED)
        );
    }

    @Test
    public void validateDelistedTest() {
        serviceOfferReplicaRepository.save(
            testOffer(1, "shopSku111", 3, SupplierType.REAL_SUPPLIER),
            testOffer(2, "shopSku222", 1, SupplierType.REAL_SUPPLIER)
        );
        sskuStatusRepository.save(sskuStatus(1, "shopSku111", OfferAvailability.ACTIVE));
        var group = setMappings(List.of(mskuStatus1, mskuStatus2),
            List.of(
                sskuStatus(1, "shopSku1", OfferAvailability.DELISTED),
                sskuStatus(1, "shopSku111", OfferAvailability.INACTIVE),
                sskuStatus(2, "shopSku222", OfferAvailability.INACTIVE),
                sskuStatus(2, "shopSku2", OfferAvailability.DELISTED))
        );

        var stockInfo = new MskuStockInfo()
            .setSupplierId(1)
            .setShopSkuKey(new ServiceOfferKey(1, "shopSku1"))
            .setWarehouseId(172)
            .setFitInternal(1);
        mskuStockRepository.insert(stockInfo);

        var result = sskuMskuStatusValidationService.validate(group);

        assertThat(result.getMskuValidStatuses())
            .extracting(MskuStatus::getMarketSkuId)
            .containsExactly(2L);

        assertThat(result.getSskuValidStatuses())
            .extracting(SskuStatus::getSupplierId, SskuStatus::getShopSku)
            .containsExactlyInAnyOrder(Tuple.tuple(2, "shopSku2"), Tuple.tuple(1, "shopSku111"));

        assertThat(result.getSskuMskuStatusWarnings())
            .extracting(statusWarning -> statusWarning.getErrorInfo().render())
            .containsExactlyInAnyOrder(
                MbocErrors.get().offerDelistedHasStocks().toString(),
                MbocErrors.get().mskuStatusTransitionDueToSskuFail(1, "shopSku1").render(),
                MbocErrors.get().mskuStatusTransitionDueToSskuFail(1, "shopSku1").render());
    }

    @Test
    public void validateDelistedWhenWarehouseNotFromUI() {
        var group = setMappings(List.of(mskuStatus1, mskuStatus2),
            List.of(
                sskuStatus(1, "shopSku1", OfferAvailability.DELISTED)
            ));

        mskuStockRepository.insert(new MskuStockInfo()
            .setSupplierId(1)
            .setShopSkuKey(new ServiceOfferKey(1, "shopSku1"))
            .setWarehouseId(1)
            .setFitInternal(1)
        );

        var result = sskuMskuStatusValidationService.validate(group);

        assertThat(result.getSskuMskuStatusWarnings()).isEmpty();
    }

    @Test
    public void validateDelistedWhenFitNegativeTest() {
        var group = setMappings(List.of(mskuStatus1, mskuStatus2),
            List.of(
                sskuStatus(1, "shopSku1", OfferAvailability.DELISTED)
            ));

        mskuStockRepository.insert(new MskuStockInfo()
            .setSupplierId(1)
            .setShopSkuKey(new ServiceOfferKey(1, "shopSku1"))
            .setWarehouseId(172)
            .setFitInternal(-5)
        );

        var result = sskuMskuStatusValidationService.validate(group);

        assertThat(result.getSskuMskuStatusWarnings()).isEmpty();
    }

    @Test
    public void validateMskuWithNoSskusTest() {
        var group = setMappings(List.of(mskuStatus(5555, MskuStatusValue.REGULAR)), List.of());

        var result = sskuMskuStatusValidationService.validate(group);

        assertThat(result.getMskuValidStatuses())
            .hasSize(0);

        assertThat(result.getSskuMskuStatusWarnings())
            .extracting(statusWarning -> statusWarning.getErrorInfo().render())
            .containsExactly(MbocErrors.get()
                .mskuStatusTransitionNoSskuFail(5555L, MskuStatusValue.REGULAR)
                .toString());
    }

    @Test
    public void validateEndOfLifeTest() {
        var group = setMappings(List.of(mskuStatus1, mskuStatusEndOfLife),
            List.of(
                sskuStatus(1, "shopSku1", OfferAvailability.DELISTED),
                sskuStatus(3, "shopSku3", OfferAvailability.ACTIVE))
        );

        var result = sskuMskuStatusValidationService.validate(group);

        assertThat(result.getMskuValidStatuses())
            .extracting(MskuStatus::getMarketSkuId)
            .containsExactly(1L, 3L);

        assertThat(result.getSskuValidStatuses())
            .extracting(SskuStatus::getSupplierId, SskuStatus::getShopSku)
            .containsExactlyInAnyOrder(Tuple.tuple(1, "shopSku1"), Tuple.tuple(3, "shopSku3"));

        assertThat(result.getSskuMskuStatusWarnings()).isEmpty();
    }

    @Test
    public void validateEndOfLifeWhereSskuChangesTest() {
        sskuStatusRepository.save(sskuStatus(3, "shopSku3", OfferAvailability.INACTIVE));

        var group = setMappings(List.of(mskuStatus1),
            List.of(
                sskuStatus(1, "shopSku1", OfferAvailability.DELISTED),
                sskuStatus(3, "shopSku3", OfferAvailability.ACTIVE))
        );

        var result = sskuMskuStatusValidationService.validate(group);

        assertThat(result.getMskuValidStatuses())
            .extracting(MskuStatus::getMarketSkuId)
            .containsExactly(1L);

        assertThat(result.getSskuValidStatuses())
            .extracting(SskuStatus::getSupplierId, SskuStatus::getShopSku)
            .containsExactlyInAnyOrder(Tuple.tuple(1, "shopSku1"), Tuple.tuple(3, "shopSku3"));

        assertThat(result.getSskuMskuStatusWarnings()).isEmpty();
    }

    @Test
    public void validateArchiveTest() {
        var group = setMappings(List.of(mskuStatus1, mskuStatusArchive),
            List.of(
                sskuStatus(1, "shopSku1", OfferAvailability.DELISTED),
                sskuStatus(4, "shopSku4", OfferAvailability.ACTIVE))
        );

        var result = sskuMskuStatusValidationService.validate(group);

        assertThat(result.getMskuValidStatuses())
            .extracting(MskuStatus::getMarketSkuId)
            .containsExactlyInAnyOrder(1L, 4L);

        assertThat(result.getSskuValidStatuses())
            .extracting(SskuStatus::getSupplierId, SskuStatus::getShopSku)
            .containsExactlyInAnyOrder(Tuple.tuple(1, "shopSku1"), Tuple.tuple(4, "shopSku4"));

        assertThat(result.getSskuMskuStatusWarnings()).isEmpty();
    }

    @Test
    public void validateEmptyState() {
        var result = sskuMskuStatusValidationService.validate(new SskuMskuStatusGroup()
            .addMskuStatuses(mskuStatus(1, MskuStatusValue.EMPTY)));

        var offers = serviceOfferReplicaRepository.findOffers(new ServiceOfferReplicaFilter()
            .setApprovedSkuIds(List.of(1L)));

        //  проверяем, что у msku были маппинги
        assertThat(offers).isNotEmpty();
        assertThat(result.getSskuMskuStatusWarnings())
            .extracting(statusWarning -> statusWarning.getErrorInfo().render())
            .containsExactly(
                MbocErrors.get().mskuStatusTransitionWithSskuFail(1L, MskuStatusValue.EMPTY).toString()
            );
    }

    @Test
    public void validateEmptyStateWithNoOffers() {
        deepmindMskuRepository.save(getSimpleMsku(100500));

        var result = sskuMskuStatusValidationService.validate(new SskuMskuStatusGroup()
            .addMskuStatuses(mskuStatus(100500, MskuStatusValue.EMPTY)));
        var offers = serviceOfferReplicaRepository.findOffers(new ServiceOfferReplicaFilter()
            .setApprovedSkuIds(List.of(100500L)));

        //  проверяем, что у msku не было маппингов
        assertThat(offers).isEmpty();
        assertThat(result.getSskuMskuStatusWarnings()).isEmpty();
    }

    @Test
    public void failIfSskuFinishTimeIsPassedForActiveOrDelisted() {
        var group1 = setMappings(List.of(), List.of(
            sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE)
                .setStatusFinishAt(Instant.now())
        ));
        Assertions.assertThatCode(() -> sskuMskuStatusValidationService.validate(group1))
            .hasMessageContaining("Finish status time is only allowed in INACTIVE_TMP, you pass: ACTIVE");

        var group2 = setMappings(List.of(), List.of(
            sskuStatus(1, "shopSku1", OfferAvailability.DELISTED)
                .setStatusFinishAt(Instant.now())
        ));
        Assertions.assertThatCode(() -> sskuMskuStatusValidationService.validate(group2))
            .hasMessageContaining("Finish status time is only allowed in INACTIVE_TMP, you pass: DELISTED");

        var group3 = setMappings(List.of(), List.of(
            sskuStatus(1, "shopSku1", OfferAvailability.INACTIVE_TMP)
                .setStatusFinishAt(Instant.now())
                .setComment("Test")
        ));
        Assertions.assertThatCode(() -> sskuMskuStatusValidationService.validate(group3))
            .doesNotThrowAnyException();

        var group4 = setMappings(List.of(), List.of(
            sskuStatus(1, "shopSku1", OfferAvailability.INACTIVE)
                .setStatusFinishAt(Instant.now())
        ));
        Assertions.assertThatCode(() -> sskuMskuStatusValidationService.validate(group4))
            .hasMessageContaining("Finish status time is only allowed in INACTIVE_TMP, you pass: INACTIVE");
    }

    @Test
    public void failIfSskuStatusFor1P() {
        var group1 = setMappings(List.of(), List.of(
            sskuStatus(5, "shopSku-3P", OfferAvailability.INACTIVE_TMP).setStatusFinishAt(Instant.now())
        ));

        Assertions.assertThatThrownBy(() -> sskuMskuStatusValidationService.validate(group1))
            .hasMessageContaining("Status INACTIVE_TMP only allowed for 1P supplier");

        var group2 = setMappings(List.of(), List.of(
            sskuStatus(5, "shopSku-3P", OfferAvailability.PENDING)
        ));

        Assertions.assertThatThrownBy(() -> sskuMskuStatusValidationService.validate(group2))
            .hasMessageContaining("Status PENDING only allowed for 1P supplier");
    }

    @Test // DEEPMIND-685
    public void failIfSskuStatusFor1pAndNotExists() {
        var group1 = setMappings(List.of(), List.of(
            sskuStatus(5, "offer-without-mapping", OfferAvailability.INACTIVE_TMP)
                .setStatusFinishAt(Instant.now())
        ));

        Assertions.assertThatThrownBy(() -> sskuMskuStatusValidationService.validate(group1))
            .hasMessageContaining("Status INACTIVE_TMP only allowed for 1P supplier");

        var group2 = setMappings(List.of(), List.of(
            sskuStatus(5, "offer-without-mapping", OfferAvailability.PENDING)
        ));

        Assertions.assertThatThrownBy(() -> sskuMskuStatusValidationService.validate(group2))
            .hasMessageContaining("Status PENDING only allowed for 1P supplier");
    }

    private SskuMskuStatusGroup setMappings(List<MskuStatus> mskuStatuses, List<SskuStatus> sskuStatuses) {
        var newGroup = new SskuMskuStatusGroup();
        if (!mskuStatuses.isEmpty()) {
            newGroup = newGroup.addMskuStatuses(mskuStatuses);
        }
        if (!sskuStatuses.isEmpty()) {
            newGroup = newGroup.addSskuStatuses(sskuStatuses);
            var mappings = serviceOfferReplicaRepository.findShopSkuMappingsByKeys(new ServiceOfferReplicaFilter(),
                newGroup.getShopSkuKeys());
            newGroup.addMappings(mappings);
        }
        return newGroup;
    }

    private SskuStatus sskuStatus(int supId, String shopSku, OfferAvailability availability) {
        return new SskuStatus().setSupplierId(supId).setShopSku(shopSku)
            .setAvailability(availability)
            .setStatusStartAt(Instant.now());
    }

    private static MskuStatus mskuStatus(long id, MskuStatusValue status) {
        return new MskuStatus().setMarketSkuId(id).setMskuStatus(status)
            .setStatusStartAt(Instant.now());
    }

    private Msku getSimpleMsku(long id) {
        return new Msku()
            .setId(id)
            .setTitle("Msku #" + id)
            .setDeleted(false)
            .setVendorId(1L)
            .setModifiedTs(Instant.now())
            .setCategoryId(1L)
            .setSkuType(SkuTypeEnum.SKU);
    }

    private ServiceOfferReplica testOffer(
        int supplierId, String shopSku, long approvedSkuMappingId, SupplierType supplierType) {
        if (deepmindSupplierRepository.findByIds(List.of(supplierId)).isEmpty()) {
            var supplier = new Supplier().setId(supplierId).setName("test_supplier_" + supplierId)
                .setSupplierType(supplierType);
            if (supplierType == REAL_SUPPLIER) {
                supplier.setRealSupplierId("0000" + supplierId);
            }
            deepmindSupplierRepository.save(supplier);
        }
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setCategoryId(1L)
            .setSeqId(0L)
            .setMskuId(approvedSkuMappingId)
            .setSupplierType(supplierType)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }
}
