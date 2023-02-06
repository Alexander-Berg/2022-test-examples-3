package ru.yandex.market.deepmind.common.services.statuses;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.assertions.DeepmindAssertions;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.stock.MskuStockRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.statuses.pojo.MskuStatusWarning;
import ru.yandex.market.deepmind.common.services.statuses.pojo.SskuStatusWarning;
import ru.yandex.market.deepmind.common.services.statuses.pojo.StatusWarning.Type;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;

import static org.assertj.core.api.Assertions.assertThat;


public class SskuMskuStatusServiceImplTest extends DeepmindBaseDbTestClass {
    @Resource
    private MskuStockRepository mskuStockRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private MskuRepository deepmindMskuRepository;
    @Resource
    private SskuStatusRepository sskuStatusRepository;
    @Resource
    private MskuStatusRepository mskuStatusRepository;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Resource
    protected DeepmindWarehouseRepository deepmindWarehouseRepository;

    private SskuMskuStatusServiceImpl sskuMskuStatusService;
    private SskuMskuStatusValidationService sskuMskuStatusValidationService;

    @Before
    public void setUp() throws Exception {
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        serviceOfferReplicaRepository.save(YamlTestUtil.readOffersFromResources("availability/offers.yml"));

        deepmindMskuRepository.save(TestUtils.newMsku(404040));
        deepmindMskuRepository.save(TestUtils.newMsku(505050));
        deepmindMskuRepository.save(TestUtils.newMsku(100000));
        deepmindMskuRepository.save(TestUtils.newMsku(10));
        deepmindMskuRepository.save(TestUtils.newMsku(20));

        var sskuMskuHelperService = new SskuMskuHelperService(serviceOfferReplicaRepository, sskuStatusRepository,
            mskuStatusRepository);
        sskuMskuStatusValidationService = new SskuMskuStatusValidationServiceImpl(mskuStockRepository,
            serviceOfferReplicaRepository, deepmindSupplierRepository, sskuMskuHelperService);
        sskuMskuStatusService = new SskuMskuStatusServiceImpl(sskuStatusRepository, mskuStatusRepository,
            sskuMskuStatusValidationService, sskuMskuHelperService, transactionTemplate);
    }

    @Test
    public void testUpdateMsku() {
        sskuStatusRepository.save(sskuStatus(60, "sku4", OfferAvailability.DELISTED));
        var newStatus = mskuStatusRepository.save(mskuStatus(404040L, MskuStatusValue.REGULAR));
        newStatus.setMskuStatus(MskuStatusValue.ARCHIVE);

        var result = sskuMskuStatusService.saveStatuses(
            new SskuMskuStatusGroup().addMskuStatuses(newStatus),
            new SskuMskuStatusContext()
        );
        DeepmindAssertions.assertThat(result).isOk();
    }

    @Test
    public void testUpdateSsku() {
        var newStatus = sskuStatusRepository.save(sskuStatus(60, "sku4", OfferAvailability.ACTIVE));
        newStatus.setAvailability(OfferAvailability.INACTIVE);
        mskuStatusRepository.save(mskuStatus(404040L, MskuStatusValue.REGULAR));

        var result = sskuMskuStatusService.saveStatuses(
            new SskuMskuStatusGroup().addSskuStatuses(newStatus),
            new SskuMskuStatusContext()
        );
        DeepmindAssertions.assertThat(result).isOk();
    }

    @Test
    public void testUpdateMskuAndSsku() {
        var ssku = sskuStatusRepository.save(sskuStatus(60, "sku4", OfferAvailability.ACTIVE));
        ssku.setAvailability(OfferAvailability.DELISTED);
        var msku = mskuStatusRepository.save(mskuStatus(404040L, MskuStatusValue.REGULAR));
        msku.setMskuStatus(MskuStatusValue.ARCHIVE);

        var result = sskuMskuStatusService.saveStatuses(
            new SskuMskuStatusGroup().addSskuStatuses(ssku).addMskuStatuses(msku),
            new SskuMskuStatusContext()
        );
        DeepmindAssertions.assertThat(result).isOk();
    }

    @Test
    public void testUpdateMskuAndSskuFail() {
        var ssku = sskuStatusRepository.save(sskuStatus(60, "sku4", OfferAvailability.ACTIVE));
        var msku = mskuStatusRepository.save(mskuStatus(404040L, MskuStatusValue.REGULAR));
        msku.setMskuStatus(MskuStatusValue.EMPTY);

        var result = sskuMskuStatusService.saveStatuses(
            new SskuMskuStatusGroup().addSskuStatuses(ssku).addMskuStatuses(msku),
            new SskuMskuStatusContext()
        );
        DeepmindAssertions.assertThat(result).isFailed();

        Assertions
            .assertThat(result.getErrors())
            .extracting(statusWarning -> statusWarning.getErrorInfo().render())
            .containsExactlyInAnyOrder(
                MbocErrors.get().mskuStatusTransitionWithSskuFail(404040L, MskuStatusValue.EMPTY)
                    .toString(),
                MbocErrors.get().sskuStatusTransitionDueToMskuFail(404040L).toString());
    }

    @Test
    public void saveMskuStatusWithoutExistingMskuShouldNotFail() {
        deepmindMskuRepository.trulyDelete(404040L);

        var result = sskuMskuStatusService.saveStatuses(new SskuMskuStatusGroup()
                .addMskuStatuses(mskuStatus(404040L, MskuStatusValue.REGULAR))
                .addMskuStatuses(mskuStatus(505050L, MskuStatusValue.END_OF_LIFE))
                .addSskuStatuses(sskuStatus(60, "sku4", OfferAvailability.ACTIVE))
                .addSskuStatuses(sskuStatus(77, "sku5", OfferAvailability.INACTIVE)),
            new SskuMskuStatusContext()
        );
        DeepmindAssertions.assertThat(result).isOk();

        var skuStatus4 = sskuStatusRepository.findByKey(60, "sku4");
        var skuStatus5 = sskuStatusRepository.findByKey(77, "sku5");
        Assertions.assertThat(skuStatus4).get().extracting(SskuStatus::getAvailability)
            .isEqualTo(OfferAvailability.ACTIVE);
        Assertions.assertThat(skuStatus5).get().extracting(SskuStatus::getAvailability)
            .isEqualTo(OfferAvailability.INACTIVE);
    }

    @Test
    public void saveMskuStatusWithoutExistingMskuWithPartialUpdate() {
        deepmindMskuRepository.trulyDelete(404040L);

        var result = sskuMskuStatusService.saveStatuses(new SskuMskuStatusGroup()
                .addMskuStatuses(mskuStatus(404040L, MskuStatusValue.REGULAR))
                .addMskuStatuses(mskuStatus(505050L, MskuStatusValue.END_OF_LIFE))
                .addSskuStatuses(sskuStatus(60, "sku4", OfferAvailability.ACTIVE))
                .addSskuStatuses(sskuStatus(77, "sku5", OfferAvailability.INACTIVE)),
            new SskuMskuStatusContext().setPartialUpdate(true)
        );
        result.throwIfFailed();

        var status404040 = mskuStatusRepository.findById(404040L);
        var status505050 = mskuStatusRepository.findById(505050L);

        Assertions.assertThat(status404040).get().extracting(MskuStatus::getMskuStatus)
            .isEqualTo(MskuStatusValue.REGULAR);
        Assertions.assertThat(status505050).get().extracting(MskuStatus::getMskuStatus)
            .isEqualTo(MskuStatusValue.END_OF_LIFE);

        var skuStatus4 = sskuStatusRepository.findByKey(60, "sku4");
        var skuStatus5 = sskuStatusRepository.findByKey(77, "sku5");
        Assertions.assertThat(skuStatus4).get().extracting(SskuStatus::getAvailability)
            .isEqualTo(OfferAvailability.ACTIVE);
        Assertions.assertThat(skuStatus5).get().extracting(SskuStatus::getAvailability)
            .isEqualTo(OfferAvailability.INACTIVE);
    }

    @Test
    public void partialUpdateWithMskuStatuses() {
        var newStatus = mskuStatus(404040L, MskuStatusValue.REGULAR);
        mskuStatusRepository.save(newStatus);

        var result1 = sskuMskuStatusService.saveStatuses(
            new SskuMskuStatusGroup().addMskuStatuses(newStatus),
            new SskuMskuStatusContext()
        );
        DeepmindAssertions.assertThat(result1)
            .containsErrorsExactlyInAnyOrder(new MskuStatusWarning(Type.CONCURRENT_UPDATE, 404040L,
                MbocErrors.get().mskuStatusTransitionParallelFail(404040L)));

        // second call, with partial update
        var result2 = sskuMskuStatusService.saveStatuses(
            new SskuMskuStatusGroup().addMskuStatuses(newStatus),
            new SskuMskuStatusContext().setPartialUpdate(true)
        );
        DeepmindAssertions.assertThat(result2).isPartialOk()
            .totalSaved(0)
            .containsWarningsExactlyInAnyOrder(
                new MskuStatusWarning(Type.CONCURRENT_UPDATE, 404040L,
                    MbocErrors.get().mskuStatusTransitionParallelFail(404040L))
            );

        // third call, with partial update and one success save
        var result3 = sskuMskuStatusService.saveStatuses(new SskuMskuStatusGroup()
                .addMskuStatuses(newStatus)
                .addMskuStatuses(mskuStatus(505050L, MskuStatusValue.REGULAR)),
            new SskuMskuStatusContext().setPartialUpdate(true)
        );
        DeepmindAssertions.assertThat(result3).isPartialOk()
            .savedMskuIds(505050L).savedSskuIds()
            .containsWarningsExactlyInAnyOrder(
                new MskuStatusWarning(Type.CONCURRENT_UPDATE, 404040L,
                    MbocErrors.get().mskuStatusTransitionParallelFail(404040L))
            );
    }

    @Test
    public void partialUpdateWithSskuStatuses() {
        var newStatus = sskuStatus(60, "sku4", OfferAvailability.ACTIVE);
        sskuStatusRepository.save(sskuStatus(60, "sku4", OfferAvailability.ACTIVE));
        mskuStatusRepository.save(mskuStatus(404040L, MskuStatusValue.REGULAR));

        var result1 = sskuMskuStatusService.saveStatuses(
            new SskuMskuStatusGroup().addSskuStatuses(newStatus),
            new SskuMskuStatusContext()
        );
        DeepmindAssertions.assertThat(result1)
            .containsErrorsExactlyInAnyOrder(new SskuStatusWarning(Type.CONCURRENT_UPDATE,
                new ServiceOfferKey(60, "sku4"), MbocErrors.get().sskuStatusTransitionParallelFail(60, "sku4")));

        // second call, with partial update
        var result2 = sskuMskuStatusService.saveStatuses(
            new SskuMskuStatusGroup().addSskuStatuses(newStatus),
            new SskuMskuStatusContext().setPartialUpdate(true)
        );
        DeepmindAssertions.assertThat(result2).isPartialOk()
            .totalSaved(0)
            .containsWarningsExactlyInAnyOrder(
                new SskuStatusWarning(Type.CONCURRENT_UPDATE, 60, "sku4",
                    MbocErrors.get().sskuStatusTransitionParallelFail(60, "sku4"))
            );

        // third call, with partial update and one success save
        mskuStatusRepository.save(mskuStatus(505050L, MskuStatusValue.REGULAR));
        var result3 = sskuMskuStatusService.saveStatuses(
            new SskuMskuStatusGroup().addSskuStatuses(
                newStatus,
                sskuStatus(77, "sku5", OfferAvailability.INACTIVE)
            ),
            new SskuMskuStatusContext().setPartialUpdate(true)
        );

        DeepmindAssertions.assertThat(result3).isPartialOk()
            .savedMskuIds().savedSskuIds(77, "sku5")
            .containsWarningsExactlyInAnyOrder(
                new SskuStatusWarning(Type.CONCURRENT_UPDATE, 60, "sku4",
                    MbocErrors.get().sskuStatusTransitionParallelFail(60, "sku4"))
            );
    }

    @Test
    public void partialUpdateWithSskuMsku1() {
        var newMskuStatus = mskuStatus(404040L, MskuStatusValue.REGULAR);
        var newSskuStatus = sskuStatus(60, "sku4", OfferAvailability.ACTIVE);
        mskuStatusRepository.save(newMskuStatus);

        var result1 = sskuMskuStatusService.saveStatuses(new SskuMskuStatusGroup()
                .addMskuStatuses(newMskuStatus)
                .addSskuStatuses(newSskuStatus),
            new SskuMskuStatusContext().setPartialUpdate(true)
        );
        DeepmindAssertions.assertThat(result1).isPartialOk()
            .totalSaved(0)
            .containsWarningsExactlyInAnyOrder(
                // Ни один из блоков не сохранился, так как msku + ssku связаны
                new MskuStatusWarning(Type.CONCURRENT_UPDATE, 404040L,
                    MbocErrors.get().mskuStatusTransitionParallelFail(404040L)),
                new SskuStatusWarning(Type.CONCURRENT_UPDATE, 60, "sku4",
                    MbocErrors.get().mskuStatusTransitionParallelFail(404040L))
            );
    }

    @Test
    public void partialUpdateWithSskuMsku2() {
        var newMskuStatus = mskuStatus(404040L, MskuStatusValue.REGULAR);
        var newSskuStatus = sskuStatus(60, "sku4", OfferAvailability.ACTIVE);
        sskuStatusRepository.save(sskuStatus(60, "sku4", OfferAvailability.ACTIVE));

        var result1 = sskuMskuStatusService.saveStatuses(new SskuMskuStatusGroup()
                .addMskuStatuses(newMskuStatus)
                .addSskuStatuses(newSskuStatus),
            new SskuMskuStatusContext().setPartialUpdate(true)
        );
        DeepmindAssertions.assertThat(result1).isPartialOk()
            .totalSaved(0)
            .containsWarningsExactlyInAnyOrder(
                // Ни один из блоков не сохранился, так как msku + ssku связаны
                new MskuStatusWarning(Type.CONCURRENT_UPDATE, 404040L,
                    MbocErrors.get().sskuStatusTransitionParallelFail(60, "sku4")),
                new SskuStatusWarning(Type.CONCURRENT_UPDATE, 60, "sku4",
                    MbocErrors.get().sskuStatusTransitionParallelFail(60, "sku4"))
            );
    }

    @Test
    public void partialUpdateWithTwoGroupsSskuMsku() {
        // создаем новый оффер, так как в хранилище только один оффер, прилепленный к 404040
        serviceOfferReplicaRepository.save(offer(60, "new-ssku"));

        var newMskuStatus1 = mskuStatus(404040L, MskuStatusValue.REGULAR);
        var newSskuStatus11 = sskuStatus(60, "sku4", OfferAvailability.ACTIVE);
        var newSskuStatus12 = sskuStatus(60, "new-ssku", OfferAvailability.ACTIVE);

        var newMskuStatus2 = mskuStatus(505050L, MskuStatusValue.NPD)
            .setNpdStartDate(LocalDate.now());
        var newSskuStatus2 = sskuStatus(77, "sku5", OfferAvailability.ACTIVE);
        sskuStatusRepository.save(sskuStatus(60, "sku4", OfferAvailability.ACTIVE));

        var result1 = sskuMskuStatusService.saveStatuses(new SskuMskuStatusGroup()
                .addMskuStatuses(newMskuStatus1, newMskuStatus2)
                .addSskuStatuses(newSskuStatus11, newSskuStatus12, newSskuStatus2),
            new SskuMskuStatusContext().setPartialUpdate(true)
        );
        DeepmindAssertions.assertThat(result1).isPartialOk()
            .savedMskuIds(newMskuStatus2.getMarketSkuId())
            .savedSskuIds(newSskuStatus2.getSupplierId(), newSskuStatus2.getShopSku())
            .containsWarningsExactlyInAnyOrder(
                // Ни один из блоков не сохранился, так как msku + ssku связаны
                new MskuStatusWarning(Type.CONCURRENT_UPDATE, 404040L,
                    MbocErrors.get().sskuStatusTransitionParallelFail(60, "sku4")),
                new SskuStatusWarning(Type.CONCURRENT_UPDATE, 60, "sku4",
                    MbocErrors.get().sskuStatusTransitionParallelFail(60, "sku4")),
                // new-ssku тоже не сохранился, так как это связка и все должно работать в связке
                new SskuStatusWarning(Type.CONCURRENT_UPDATE, 60, "new-ssku",
                    MbocErrors.get().sskuStatusTransitionParallelFail(60, "sku4"))
            );
    }

    @Test
    public void testCountUpdatedCount() {
        serviceOfferReplicaRepository.save(
            offer(1, "sku11"),
            offer(1, "sku22"),
            offer(1, "sku33"),
            offer(1, "sku44"),
            offer(1, "sku55"),
            offer(1, "sku66")
        );

        sskuStatusRepository.save(
            sskuStatus(1, "sku11", OfferAvailability.ACTIVE),
            sskuStatus(1, "sku22", OfferAvailability.ACTIVE),
            sskuStatus(1, "sku33", OfferAvailability.ACTIVE),
            sskuStatus(1, "sku44", OfferAvailability.INACTIVE),
            sskuStatus(1, "sku55", OfferAvailability.INACTIVE),
            sskuStatus(1, "sku66", OfferAvailability.DELISTED)
        );

        mskuStatusRepository.save(
            mskuStatus(404040, MskuStatusValue.REGULAR),
            mskuStatus(505050, MskuStatusValue.REGULAR),
            mskuStatus(100000, MskuStatusValue.END_OF_LIFE),
            mskuStatus(10, MskuStatusValue.IN_OUT),
            mskuStatus(20, MskuStatusValue.IN_OUT)
        );

        var result = sskuMskuStatusService.countStatuses();

        Assertions.assertThat(result.getSskuCount()).containsOnly(
            Map.entry(OfferAvailability.ACTIVE, 3),
            Map.entry(OfferAvailability.INACTIVE, 2),
            Map.entry(OfferAvailability.DELISTED, 1)
        );

        Assertions.assertThat(result.getMskuCount()).containsOnly(
            Map.entry(MskuStatusValue.REGULAR, 2),
            Map.entry(MskuStatusValue.END_OF_LIFE, 1),
            Map.entry(MskuStatusValue.IN_OUT, 2)
        );
    }

    @Test
    public void testChangeStatusStartAt() {
        serviceOfferReplicaRepository.save(offer(1, "sku1"));

        var before = saveAndGet(sskuStatus(1, "sku1", OfferAvailability.ACTIVE));
        var afterCreate = before.getStatusStartAt();

        var after = saveAndGet(before.setAvailability(OfferAvailability.INACTIVE));
        var afterUpdate = after.getStatusStartAt();

        assertThat(afterUpdate).isAfter(afterCreate);
    }

    @Test
    public void testDontChangeStatusStartAt() {
        serviceOfferReplicaRepository.save(offer(1, "sku1"));

        var before = saveAndGet(sskuStatus(1, "sku1", OfferAvailability.ACTIVE));
        var afterCreate = before.getStatusStartAt();

        var after = saveAndGet(before.setComment("comment"));
        var afterUpdate = after.getStatusStartAt();

        assertThat(afterUpdate).isEqualTo(afterCreate).isNotNull();
    }

    @Test
    public void testSetPreviousAvailability() {
        serviceOfferReplicaRepository.save(offer(1, "sku1"));

        var before = saveAndGet(sskuStatus(1, "sku1", OfferAvailability.ACTIVE));
        Assertions.assertThat(before.getPreviousAvailability()).isNull();

        var after = saveAndGet(before.setAvailability(OfferAvailability.INACTIVE));
        Assertions.assertThat(after.getPreviousAvailability()).isEqualTo(OfferAvailability.ACTIVE);

        var after3 = saveAndGet(before.setAvailability(OfferAvailability.DELISTED));
        Assertions.assertThat(after3.getPreviousAvailability()).isEqualTo(OfferAvailability.INACTIVE);
    }

    @Test // DEEPMIND-685
    public void failIfSskuStatusFor1pAndNotExists() {
        var before3p = sskuStatus(42, "sku1", OfferAvailability.INACTIVE);
        var after3p = saveAndGet(before3p);

        Assertions.assertThat(after3p.getAvailability()).isEqualTo(OfferAvailability.INACTIVE);

        var before1p = sskuStatus(77, "sku10", OfferAvailability.INACTIVE);
        var after1p = saveAndGet(before1p);

        Assertions.assertThat(after1p.getAvailability()).isEqualTo(OfferAvailability.INACTIVE);
    }

    @Test // DEEPMIND-685
    public void failToSavePendingStatusFor3PSupplierWithoutMapping() {
        var before = sskuStatus(42, "sku1", OfferAvailability.PENDING);
        var result = sskuMskuStatusService.saveStatuses(new SskuMskuStatusGroup()
                .addSskuStatuses(before),
            new SskuMskuStatusContext()
        );
        Assertions.assertThat(result.getStatus())
            .isEqualTo(SskuMskuStatusResult.Status.FAILED);
        Assertions.assertThat(result.getException())
            .hasMessageContaining("Status PENDING only allowed for 1P supplier");
    }

    private ServiceOfferReplica offer(
        int supplierId, String sku) {
        var supplier = deepmindSupplierRepository.findById(supplierId);
        var mbocSupplier = new Supplier()
            .setId(supplier.get().getId())
            .setMbiBusinessId(supplier.get().getBusinessId())
            .setType(MbocSupplierType.valueOf(supplier.get().getSupplierType().name()));
        return new ServiceOfferReplica()
            .setBusinessId(mbocSupplier.getEffectiveBusinessId())
            .setSupplierId(supplierId)
            .setShopSku(sku)
            .setTitle("title " + sku)
            .setCategoryId(1L)
            .setSeqId(0L)
            .setMskuId(404040L)
            .setSupplierType(supplier.get().getSupplierType())
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

    private SskuStatus sskuStatus(int supplierId, String shopSku, OfferAvailability status) {
        return new SskuStatus()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setAvailability(status)
            .setStatusStartAt(Instant.now());
    }

    private MskuStatus mskuStatus(long mskuId, MskuStatusValue status) {
        return new MskuStatus().setMarketSkuId(mskuId).setMskuStatus(status)
            .setStatusStartAt(Instant.now());
    }

    private SskuStatus saveAndGet(SskuStatus sskuStatus) {
        var result = sskuMskuStatusService.saveStatuses(new SskuMskuStatusGroup()
                .addSskuStatuses(sskuStatus),
            new SskuMskuStatusContext()
        );
        result.throwIfFailed();
        return sskuStatusRepository.findByKey(sskuStatus.getSupplierId(), sskuStatus.getShopSku()).get();
    }
}
