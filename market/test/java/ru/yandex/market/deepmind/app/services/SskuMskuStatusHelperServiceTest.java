package ru.yandex.market.deepmind.app.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deepmind.app.pojo.DisplayMskuStatusWarning;
import ru.yandex.market.deepmind.app.pojo.DisplaySskuStatusWarning;
import ru.yandex.market.deepmind.app.web.DisplayMskuInfo.DisplayMskuStatusValue;
import ru.yandex.market.deepmind.app.web.MskuAvailabilityStatusToSave;
import ru.yandex.market.deepmind.app.web.UpdateMskuStatusRequest;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.MskuFilter;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaFilter;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.stock.MskuStockRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.background.BackgroundServiceMock;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuHelperService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusServiceImpl;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusValidationServiceImpl;
import ru.yandex.market.deepmind.common.stock.MskuStockInfo;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.mboc.common.MbocErrors;

import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.REAL_SUPPLIER;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.THIRD_PARTY;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;

@Slf4j
public class SskuMskuStatusHelperServiceTest extends DeepmindBaseDbTestClass {

    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private MskuStockRepository mskuStockRepository;
    @Resource
    private SskuStatusRepository sskuStatusRepository;
    @Resource
    private MskuRepository deepmindMskuRepository;
    @Resource
    private MskuStatusRepository mskuStatusRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Resource
    private DeepmindWarehouseRepository deepmindWarehouseRepository;
    private BackgroundServiceMock backgroundServiceMock;

    private SskuMskuStatusHelperService sskuMskuStatusHelperService;
    private SskuMskuStatusService sskuMskuStatusService;

    @Before
    public void setUp() {
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT_FOR_UNIT_TESTS);
        backgroundServiceMock = new BackgroundServiceMock();
        var sskuMskuHelperService = new SskuMskuHelperService(serviceOfferReplicaRepository, sskuStatusRepository,
            mskuStatusRepository);
        var sskuMskuStatusValidationService = new SskuMskuStatusValidationServiceImpl(mskuStockRepository,
            serviceOfferReplicaRepository, deepmindSupplierRepository, sskuMskuHelperService);
        sskuMskuStatusService = new SskuMskuStatusServiceImpl(sskuStatusRepository, mskuStatusRepository,
            sskuMskuStatusValidationService, sskuMskuHelperService, transactionTemplate);

        sskuMskuStatusHelperService = new SskuMskuStatusHelperServiceImpl(serviceOfferReplicaRepository,
            backgroundServiceMock, sskuMskuStatusService, sskuMskuStatusValidationService, sskuStatusRepository,
            deepmindMskuRepository, mskuStatusRepository, transactionTemplate);
    }

    @Test
    public void saveSskuStatusTest() {
        var supplier = create3pSupplier(1);
        deepmindSupplierRepository.save(supplier);
        serviceOfferReplicaRepository.save(createOffer(1, "shopSku1", 1));

        sskuStatusRepository.save(sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null));

        deepmindMskuRepository.save(msku(1L));
        mskuStatusRepository.save(mskuStatus(1, MskuStatusValue.REGULAR));

        sskuMskuStatusHelperService.updateSskuStatuses(List.of(
            sskuState(1, "shopSku1", OfferAvailability.INACTIVE, "comment")
        ), "unit-test", false);
        Assertions
            .assertThat(sskuStatusRepository.findByKey(1, "shopSku1").get().getAvailability())
            .isEqualTo(OfferAvailability.INACTIVE);
        Assertions
            .assertThat(sskuStatusRepository.findByKey(1, "shopSku1").get().getComment())
            .isEqualTo("comment");
    }

    @Test
    public void saveSskuStatusInactiveTmpTest() {
        var supplier = create1PSupplier(1, "000076");
        deepmindSupplierRepository.save(supplier);
        var offer = createOffer(1, "shopSku1", 1);
        serviceOfferReplicaRepository.save(offer);

        sskuStatusRepository.save(sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null));

        deepmindMskuRepository.save(msku(1L));
        mskuStatusRepository.save(mskuStatus(1, MskuStatusValue.REGULAR));

        var statusFinishAt = Instant.now().plus(10, ChronoUnit.DAYS);
        sskuMskuStatusHelperService.updateSskuStatuses(List.of(
            sskuState(1, "shopSku1", OfferAvailability.INACTIVE_TMP, "comment")
                .setStatusFinishAt(statusFinishAt)
        ), "unit-test", false);
        Assertions
            .assertThat(sskuStatusRepository.findByKey(1, "shopSku1").get())
            .extracting(SskuStatus::getAvailability, SskuStatus::getStatusFinishAt)
            .containsExactly(OfferAvailability.INACTIVE_TMP, statusFinishAt);
    }

    @Test
    public void saveSskuStatusNoChangesTest() {
        var supplier = create3pSupplier(1);
        deepmindSupplierRepository.save(supplier);
        var offer = createOffer(1, "shopSku1", 1);
        serviceOfferReplicaRepository.save(offer);

        sskuStatusRepository.save(sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null));

        deepmindMskuRepository.save(msku(1L));
        mskuStatusRepository.save(mskuStatus(1, MskuStatusValue.REGULAR));

        sskuMskuStatusHelperService.updateSskuStatuses(List.of(
            sskuState(1, "shopSku1", OfferAvailability.ACTIVE, "")
        ), "unit-test", false);
        Assertions
            .assertThat(sskuStatusRepository.findByKey(1, "shopSku1").get().getAvailability())
            .isEqualTo(OfferAvailability.ACTIVE);
        Assertions
            .assertThat(sskuStatusRepository.findByKey(1, "shopSku1").get().getComment())
            .isEqualTo(null);
    }

    @Test
    public void updateSskuStatusStatusFinishAtComparisonTest() {
        var supplier = create1PSupplier(1, "000076");
        deepmindSupplierRepository.save(supplier);
        var offer = createOffer(1, "shopSku1", 1);
        serviceOfferReplicaRepository.save(offer);

        sskuStatusRepository.save(sskuStatus(1, "shopSku1", OfferAvailability.INACTIVE_TMP, null)
            .setStatusFinishAt(Instant.now()));

        deepmindMskuRepository.save(msku(1L));
        mskuStatusRepository.save(mskuStatus(1, MskuStatusValue.REGULAR));

        var statusFinishAt = Instant.now().plusSeconds(10);
        sskuMskuStatusHelperService.updateSskuStatuses(List.of(
            sskuState(1, "shopSku1", OfferAvailability.INACTIVE_TMP, null)
                .setStatusFinishAt(statusFinishAt)
                .setComment("Test")
        ), "unit-test", false);

        Assertions
            .assertThat(sskuStatusRepository.findByKey(1, "shopSku1")).get()
            .extracting(SskuStatus::getAvailability, SskuStatus::getStatusFinishAt)
            .containsExactly(OfferAvailability.INACTIVE_TMP, statusFinishAt);
    }

    @Test
    public void saveSskuStatusesTest() {
        prepareDefaultState();

        var shopSkuKeysToSave = List.of(
            sskuState(1, "shopSku1", OfferAvailability.INACTIVE, "comment1"),
            sskuState(2, "shopSku2", OfferAvailability.INACTIVE, "comment2"));

        sskuMskuStatusHelperService.updateSskuStatuses(shopSkuKeysToSave, "unit-test", false);

        Assertions
            .assertThat(sskuStatusRepository.findByKey(1, "shopSku1").get().getAvailability())
            .isEqualTo(OfferAvailability.INACTIVE);

        Assertions
            .assertThat(sskuStatusRepository.findByKey(2, "shopSku2").get().getAvailability())
            .isEqualTo(OfferAvailability.INACTIVE);

        Assertions
            .assertThat(sskuStatusRepository.findByKey(1, "shopSku1").get().getComment())
            .isEqualTo("comment1");
        Assertions
            .assertThat(sskuStatusRepository.findByKey(2, "shopSku2").get().getComment())
            .isEqualTo("comment2");
    }

    @Test
    public void emptyStringFieldsAsNullCheckTest() {
        prepareDefaultState();

        var shopSkuKeysToSave = List.of(
            sskuState(1, "shopSku1", OfferAvailability.ACTIVE, ""));

        sskuMskuStatusHelperService.updateSskuStatuses(shopSkuKeysToSave, "unit-test", false);

        Assertions
            .assertThat(sskuStatusRepository.findByKey(1, "shopSku1").get().getAvailability())
            .isEqualTo(OfferAvailability.ACTIVE);

        Assertions
            .assertThat(sskuStatusRepository.findByKey(1, "shopSku1").get().getComment())
            .isEqualTo(null);
    }

    @Test
    public void saveSskuStatusesInvalidTransitionTest() {
        prepareDefaultState();

        var stockInfo = new MskuStockInfo()
            .setSupplierId(42)
            .setShopSkuKey(new ServiceOfferKey(1, "shopSku1"))
            .setWarehouseId((int) TOMILINO_ID)
            .setFitInternal(1);
        mskuStockRepository.insert(stockInfo);

        var shopSkuKeysToSave = List.of(
            sskuState(1, "shopSku1", OfferAvailability.DELISTED, "comment1"),
            sskuState(2, "shopSku2", OfferAvailability.DELISTED, "comment2"));

        var warnings = sskuMskuStatusHelperService.updateSskuStatuses(shopSkuKeysToSave, "unit-test", true);

        Assertions
            .assertThat(warnings)
            .hasSize(1);

        Assertions
            .assertThat(sskuStatusRepository.findByKey(1, "shopSku1").get().getAvailability())
            .isEqualTo(OfferAvailability.ACTIVE);

        Assertions
            .assertThat(sskuStatusRepository.findByKey(2, "shopSku2").get().getAvailability())
            .isEqualTo(OfferAvailability.DELISTED);
    }

    @Test
    public void saveSskuStatusesInBackValidTransitionTest() {
        var supplier = create1PSupplier(1, "000076");
        var supplier2 = create1PSupplier(2, "000077");
        deepmindSupplierRepository.save(supplier, supplier2);

        var offer1 = createOffer(1, "shopSku1", 1);
        var offer2 = createOffer(2, "shopSku2", 2);
        serviceOfferReplicaRepository.save(offer1, offer2);

        deepmindMskuRepository.save(msku(1L), msku(2L));

        sskuStatusRepository.save(
            sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null),
            sskuStatus(2, "shopSku2", OfferAvailability.ACTIVE, null));

        var statusFinishAt = Instant.now().plus(10, ChronoUnit.DAYS);
        var request = new SskuStatusToSaveAsync()
            .setStatus(OfferAvailability.INACTIVE_TMP)
            .setComment("comment")
            .setStatusFinishAt(statusFinishAt);
        var filter = new ServiceOfferReplicaFilter();
        // check correct update
        sskuMskuStatusHelperService.updateSskuStatusesAsync(filter, "", request, null);

        var result = backgroundServiceMock.getMidStatus(0);
        Assertions.assertThat(result.getMessage()).isEqualTo("Успешное выполнение. Успешно обработано 2 записей");
        Assertions.assertThat(result.getParams()).isEqualTo(null);

        var sskuStatus1 = sskuStatusRepository.findByKey(1, "shopSku1");
        var sskuStatus2 = sskuStatusRepository.findByKey(2, "shopSku2");

        Assertions.assertThat(sskuStatus1).get()
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment, SskuStatus::getStatusFinishAt)
            .containsExactly(OfferAvailability.INACTIVE_TMP, "comment", statusFinishAt);
        Assertions.assertThat(sskuStatus2).get()
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment, SskuStatus::getStatusFinishAt)
            .containsExactly(OfferAvailability.INACTIVE_TMP, "comment", statusFinishAt);
    }

    @Test
    public void updateSskuStatusesInBackCheckTaskStatusTest() {
        prepareDefaultState();

        var filter = new ServiceOfferReplicaFilter();
        var request = new SskuStatusToSaveAsync()
            .setStatus(OfferAvailability.INACTIVE)
            .setComment("comment")
            .setApprovePartialUpdate(true);
        sskuMskuStatusHelperService.updateSskuStatusesAsync(filter, "", request, 1);

        var midStatus = backgroundServiceMock.getMidStatus(2);
        Assertions.assertThat(midStatus.getMessage()).isEqualTo("Успешно обработано 1 записей");

        var result = backgroundServiceMock.getMidStatus(0);
        Assertions.assertThat(result.getMessage()).isEqualTo("Успешное выполнение. Успешно обработано 2 записей");
        Assertions.assertThat(result.getParams()).isEqualTo(null);

        Assertions
            .assertThat(sskuStatusRepository.findByKey(1, "shopSku1").get().getAvailability())
            .isEqualTo(OfferAvailability.INACTIVE);
        Assertions
            .assertThat(sskuStatusRepository.findByKey(2, "shopSku2").get().getAvailability())
            .isEqualTo(OfferAvailability.INACTIVE);
    }

    @Test
    public void updateSskuStatusesInBackCheckTaskWarningStatusTest() {
        prepareDefaultState();

        var stockInfo = new MskuStockInfo()
            .setSupplierId(1)
            .setShopSkuKey(new ServiceOfferKey(1, "shopSku1"))
            .setWarehouseId((int) TOMILINO_ID)
            .setFitInternal(1);
        mskuStockRepository.insert(stockInfo);

        var request = new SskuStatusToSaveAsync()
            .setStatus(OfferAvailability.DELISTED)
            .setComment("comment")
            .setApprovePartialUpdate(true);
        sskuMskuStatusHelperService.updateSskuStatusesAsync(new ServiceOfferReplicaFilter(), "", request, 1);

        var midStatus = backgroundServiceMock.getMidStatus(1);
        Assertions.assertThat(midStatus.getMessage())
            .isEqualTo("Успешно обработано 1 записей, общее количество ошибок 1");

        var result = backgroundServiceMock.getMidStatus(0);
        Assertions.assertThat(result.getMessage()).isEqualTo("Выполнено с ошибками. Успешно: 1, ошибки: 1");
        Assertions.assertThat(result.getParams()).isEqualTo(List.of(
            DisplaySskuStatusWarning.of(1, "shopSku1", MbocErrors.get().offerDelistedHasStocks())
        ));

        Assertions
            .assertThat(sskuStatusRepository.findByKey(1, "shopSku1").get().getAvailability())
            .isEqualTo(OfferAvailability.ACTIVE);
        Assertions
            .assertThat(sskuStatusRepository.findByKey(2, "shopSku2").get().getAvailability())
            .isEqualTo(OfferAvailability.DELISTED);
    }

    @Test
    public void updateSskuStatusInBackInvalidTransitionNotPartialTest() {
        // prepare data
        prepareDefaultState();

        var stockInfo = new MskuStockInfo()
            .setSupplierId(1)
            .setShopSkuKey(new ServiceOfferKey(1, "shopSku1"))
            .setWarehouseId((int) TOMILINO_ID)
            .setFitInternal(1);
        mskuStockRepository.insert(stockInfo);

        // execute service
        var filter = new ServiceOfferReplicaFilter();
        var request = new SskuStatusToSaveAsync()
            .setStatus(OfferAvailability.DELISTED)
            .setComment("comment");

        sskuMskuStatusHelperService.updateSskuStatusesAsync(filter, "", request, null);

        var result = backgroundServiceMock.getMidStatus(0);
        Assertions.assertThat(result.getMessage()).isEqualTo("Ошибки при сохранении: 1");
        Assertions.assertThat(result.getParams()).isEqualTo(List.of(
            DisplaySskuStatusWarning.of(1, "shopSku1", MbocErrors.get().offerDelistedHasStocks())
        ));

        Assertions
            .assertThat(sskuStatusRepository.findByKey(1, "shopSku1").get().getAvailability())
            .isEqualTo(OfferAvailability.ACTIVE);
        Assertions
            .assertThat(sskuStatusRepository.findByKey(2, "shopSku2").get().getAvailability())
            .isEqualTo(OfferAvailability.ACTIVE);
    }

    @Test
    public void saveSskuStatusesApprovedPartialUpdateTest() {
        prepareDefaultState();

        var stockInfo = new MskuStockInfo()
            .setSupplierId(1)
            .setShopSkuKey(new ServiceOfferKey(1, "shopSku1"))
            .setWarehouseId((int) TOMILINO_ID)
            .setFitInternal(1);
        mskuStockRepository.insert(stockInfo);

        // execute service
        var filter = new ServiceOfferReplicaFilter();
        var request = new SskuStatusToSaveAsync()
            .setStatus(OfferAvailability.DELISTED)
            .setComment("comment")
            .setApprovePartialUpdate(true);
        sskuMskuStatusHelperService.updateSskuStatusesAsync(filter, "", request, null);

        var result = backgroundServiceMock.getMidStatus(0);
        Assertions.assertThat(result.getMessage()).isEqualTo("Выполнено с ошибками. Успешно: 1, ошибки: 1");
        Assertions.assertThat(result.getParams()).isEqualTo(List.of(
            DisplaySskuStatusWarning.of(1, "shopSku1", MbocErrors.get().offerDelistedHasStocks())
        ));

        Assertions
            .assertThat(sskuStatusRepository.findByKey(1, "shopSku1").get().getAvailability())
            .isEqualTo(OfferAvailability.ACTIVE);
        Assertions
            .assertThat(sskuStatusRepository.findByKey(2, "shopSku2").get().getAvailability())
            .isEqualTo(OfferAvailability.DELISTED);
    }

    @Test
    public void updateSskuStatusDontChangeIfExistsTest() {
        var supplier = create3pSupplier(1);
        deepmindSupplierRepository.save(supplier);
        var offer = createOffer(1, "shopSku1", 1);
        serviceOfferReplicaRepository.save(offer);

        sskuStatusRepository.save(
            sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null));

        var modifiedAt = sskuStatusRepository.findByKey(1, "shopSku1").get().getModifiedAt();

        deepmindMskuRepository.save(msku(1L));
        mskuStatusRepository.save(mskuStatus(1, MskuStatusValue.REGULAR));

        sskuMskuStatusHelperService.updateSskuStatuses(
            List.of(sskuState(1, "shopSku1", OfferAvailability.ACTIVE, null)), "unit-test", false);

        var statusO = sskuStatusRepository.findByKey(1, "shopSku1");
        Assertions.assertThat(statusO).get()
            .extracting(SskuStatus::getModifiedAt)
            .isEqualTo(modifiedAt);
    }

    @Test
    public void updateSskuStatusDontChangeIfExistsInBackTest() {
        var supplier = create3pSupplier(1);
        deepmindSupplierRepository.save(supplier);
        var offer = createOffer(1, "shopSku1", 1);
        serviceOfferReplicaRepository.save(offer);

        sskuStatusRepository.save(
            sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null));

        var modifiedAt = sskuStatusRepository.findByKey(1, "shopSku1").get().getModifiedAt();

        deepmindMskuRepository.save(msku(1L));
        mskuStatusRepository.save(mskuStatus(1, MskuStatusValue.REGULAR));

        var filter = new ServiceOfferReplicaFilter();
        var request = new SskuStatusToSaveAsync()
            .setStatus(OfferAvailability.ACTIVE)
            .setApprovePartialUpdate(true);
        sskuMskuStatusHelperService.updateSskuStatusesAsync(filter, "user", request, null);

        var result = backgroundServiceMock.getMidStatus(0);
        Assertions.assertThat(result.getMessage()).isEqualTo("Успешное выполнение. Успешно обработано 1 записей");
        Assertions.assertThat(result.getParams()).isEqualTo(null);

        Assertions.assertThat(sskuStatusRepository.findByKey(1, "shopSku1")).get()
            .extracting(SskuStatus::getModifiedAt)
            .isEqualTo(modifiedAt);
    }

    @Test
    public void rollbackChangesIfBackgroundUpdateWasCancelled() {
        deepmindSupplierRepository.save(create3pSupplier(1));
        serviceOfferReplicaRepository.save(
            createOffer(1, "shopSku1", 1),
            createOffer(1, "shopSku2", 1),
            createOffer(1, "shopSku3", 1),
            createOffer(1, "shopSku4", 1),
            createOffer(1, "shopSku5", 1)
        );

        sskuStatusRepository.save(
            sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null),
            sskuStatus(1, "shopSku2", OfferAvailability.ACTIVE, null),
            sskuStatus(1, "shopSku3", OfferAvailability.ACTIVE, null),
            sskuStatus(1, "shopSku4", OfferAvailability.ACTIVE, null),
            sskuStatus(1, "shopSku5", OfferAvailability.ACTIVE, null)
        );

        deepmindMskuRepository.save(msku(1L));
        mskuStatusRepository.save(mskuStatus(1, MskuStatusValue.REGULAR));

        backgroundServiceMock.cancelAll(true);

        var filter = new ServiceOfferReplicaFilter();
        var request = new SskuStatusToSaveAsync()
            .setStatus(OfferAvailability.INACTIVE);
        sskuMskuStatusHelperService.updateSskuStatusesAsync(filter, "user", request, 1);

        var result = backgroundServiceMock.getMidStatus(0);
        Assertions.assertThat(result.getMessage()).contains("Произошла ошибка. Напишите разработчикам");
        Assertions.assertThat(result.getMessage()).contains("cancel requested");
        Assertions.assertThat(result.getParams()).isEqualTo(null);

        Assertions.assertThat(sskuStatusRepository.findByKey(1, "shopSku1")).get()
            .extracting(SskuStatus::getAvailability).isEqualTo(OfferAvailability.ACTIVE);
        Assertions.assertThat(sskuStatusRepository.findByKey(1, "shopSku2")).get()
            .extracting(SskuStatus::getAvailability).isEqualTo(OfferAvailability.ACTIVE);
        Assertions.assertThat(sskuStatusRepository.findByKey(1, "shopSku3")).get()
            .extracting(SskuStatus::getAvailability).isEqualTo(OfferAvailability.ACTIVE);
        Assertions.assertThat(sskuStatusRepository.findByKey(1, "shopSku4")).get()
            .extracting(SskuStatus::getAvailability).isEqualTo(OfferAvailability.ACTIVE);
        Assertions.assertThat(sskuStatusRepository.findByKey(1, "shopSku5")).get()
            .extracting(SskuStatus::getAvailability).isEqualTo(OfferAvailability.ACTIVE);
    }

    @Test
    public void updateMskuAvailabilityTest() {
        deepmindMskuRepository.save(msku(1L));
        mskuStatusRepository.save(mskuStatus(1L, MskuStatusValue.NPD));
        deepmindMskuRepository.save(msku(2L));

        var offer = createOffer(1, "shopSku1", 1);
        serviceOfferReplicaRepository.save(offer);
        serviceOfferReplicaRepository.save(createOffer(1, "shopSku2", 2));
        sskuStatusRepository.save(
            sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null),
            sskuStatus(1, "shopSku2", OfferAvailability.ACTIVE, null));

        sskuMskuStatusHelperService.updateMskuStatuses(
            List.of(mskuState(1L, MskuStatusValue.REGULAR).setComment("comment"),
                mskuState(2L, MskuStatusValue.REGULAR).setComment("comment2")),
            "myuser", false);

        var statusOpt = mskuStatusRepository.findById(1L);
        Assertions.assertThat(statusOpt).get()
            .extracting(MskuStatus::getMskuStatus)
            .isEqualTo(MskuStatusValue.REGULAR);
        Assertions.assertThat(statusOpt).get()
            .extracting(MskuStatus::getNpdFinishDate)
            .isNull();
        Assertions.assertThat(statusOpt).get()
            .extracting(MskuStatus::getNpdStartDate)
            .isNull();

        Assertions.assertThat(mskuStatusRepository.findById(2L)).get()
            .extracting(MskuStatus::getMskuStatus)
            .isEqualTo(MskuStatusValue.REGULAR);

        Assertions.assertThat(mskuStatusRepository.findById(2L)).get()
            .extracting(MskuStatus::getComment)
            .isEqualTo("comment2");
    }

    @Test
    public void updateMskuStatusCheckTaskStatusTest() {
        deepmindMskuRepository.save(msku(1L));
        mskuStatusRepository.save(mskuStatus(1L, MskuStatusValue.NPD));
        deepmindMskuRepository.save(msku(2L));

        var offer = createOffer(1, "shopSku1", 1);
        serviceOfferReplicaRepository.save(offer);
        serviceOfferReplicaRepository.save(createOffer(1, "shopSku2", 2));
        sskuStatusRepository.save(
            sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null),
            sskuStatus(1, "shopSku2", OfferAvailability.ACTIVE, null));

        var filter = new MskuFilter();
        var request = new UpdateMskuStatusRequest()
            .setMskuStatus(DisplayMskuStatusValue.fromDbValue(MskuStatusValue.REGULAR))
            .setComment("comment");

        sskuMskuStatusHelperService.updateMskuStatusesAsync(filter, "myuser", request, true, 1);

        var midStatus = backgroundServiceMock.getMidStatus(2);
        var result = backgroundServiceMock.getMidStatus(0);

        Assertions.assertThat(midStatus.getMessage()).isEqualTo("Успешно обработано 1 записей");
        Assertions.assertThat(result.getMessage()).isEqualTo("Успешное выполнение. Успешно обработано 2 записей");

        var status1 = mskuStatusRepository.findById(1L);
        var status2 = mskuStatusRepository.findById(2L);
        Assertions.assertThat(status1).get()
            .extracting(MskuStatus::getMskuStatus, MskuStatus::getComment)
            .containsExactly(MskuStatusValue.REGULAR, "comment");

        Assertions.assertThat(status2).get()
            .extracting(MskuStatus::getMskuStatus, MskuStatus::getComment)
            .containsExactly(MskuStatusValue.REGULAR, "comment");
    }

    @Test
    public void updateMskuStatusCheckTaskStatusWarningTest() {
        deepmindMskuRepository.save(msku(1L));
        mskuStatusRepository.save(mskuStatus(1L, MskuStatusValue.NPD));
        deepmindMskuRepository.save(msku(2L));

        var offer = createOffer(1, "shopSku1", 1);
        serviceOfferReplicaRepository.save(offer);
        sskuStatusRepository.save(
            sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null));

        var filter = new MskuFilter();
        var request = new UpdateMskuStatusRequest()
            .setMskuStatus(DisplayMskuStatusValue.fromDbValue(MskuStatusValue.EMPTY))
            .setComment("comment");
        sskuMskuStatusHelperService.updateMskuStatusesAsync(filter, "myuser", request, true, 1);

        var result = backgroundServiceMock.getMidStatus(0);
        Assertions.assertThat(result.getMessage()).isEqualTo("Выполнено с ошибками. Успешно: 1, ошибки: 1");
        Assertions.assertThat(result.getParams()).isEqualTo(List.of(
            DisplayMskuStatusWarning.of(1L, MbocErrors.get().mskuStatusTransitionWithSskuFail(1, MskuStatusValue.EMPTY))
        ));

        var status1 = mskuStatusRepository.findById(1L);
        var status2 = mskuStatusRepository.findById(2L);
        Assertions.assertThat(status1).get()
            .extracting(MskuStatus::getMskuStatus)
            .isEqualTo(MskuStatusValue.NPD);

        Assertions.assertThat(status2).get()
            .extracting(MskuStatus::getMskuStatus)
            .isEqualTo(MskuStatusValue.EMPTY);
    }

    @Test
    public void updateMskuStatusInvalidTransitionNotPartialTest() {
        deepmindMskuRepository.save(msku(1L));
        mskuStatusRepository.save(mskuStatus(1L, MskuStatusValue.NPD));

        var offer = createOffer(1, "shopSku1", 1);
        serviceOfferReplicaRepository.save(offer);
        serviceOfferReplicaRepository.save(createOffer(1, "shopSku2", 2));
        sskuStatusRepository.save(
            sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null));

        Assertions.assertThatThrownBy(() -> sskuMskuStatusHelperService.updateMskuStatuses(
            List.of(mskuState(1L, MskuStatusValue.EMPTY).setComment("comment")),
            "myuser", false))
            .hasMessageContaining(MbocErrors.get().mskuStatusTransitionWithSskuFail(1L, MskuStatusValue.EMPTY)
                .toString());

        var statusOpt = mskuStatusRepository.findById(1L);
        Assertions.assertThat(statusOpt).get()
            .extracting(MskuStatus::getMskuStatus)
            .isEqualTo(MskuStatusValue.NPD);
    }

    @Test
    public void updateMskuStatusInvalidTransitionNotPartialInBackTest() {
        deepmindMskuRepository.save(msku(1L));
        mskuStatusRepository.save(mskuStatus(1L, MskuStatusValue.NPD));

        var offer = createOffer(1, "shopSku1", 1);
        serviceOfferReplicaRepository.save(offer);
        serviceOfferReplicaRepository.save(createOffer(1, "shopSku2", 2));
        sskuStatusRepository.save(
            sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null));

        var filter = new MskuFilter();
        var request = new UpdateMskuStatusRequest()
            .setMskuStatus(DisplayMskuStatusValue.fromDbValue(MskuStatusValue.EMPTY))
            .setComment("comment");
        sskuMskuStatusHelperService.updateMskuStatusesAsync(filter, "", request, false, 1);

        var result = backgroundServiceMock.getMidStatus(0);
        Assertions.assertThat(result.getMessage()).isEqualTo("Ошибки при сохранении: 1");
        Assertions.assertThat(result.getParams()).isEqualTo(List.of(
            DisplayMskuStatusWarning.of(1L, MbocErrors.get().mskuStatusTransitionWithSskuFail(1, MskuStatusValue.EMPTY))
        ));

        var statusOpt = mskuStatusRepository.findById(1L);
        Assertions.assertThat(statusOpt).get()
            .extracting(MskuStatus::getMskuStatus)
            .isEqualTo(MskuStatusValue.NPD);
    }

    @Test
    public void updateMskuAvailabilitiesInBackTest() {
        deepmindMskuRepository.save(msku(1L));
        mskuStatusRepository.save(mskuStatus(1L, MskuStatusValue.NPD));
        deepmindMskuRepository.save(msku(2L));

        var offer = createOffer(1, "shopSku1", 1);
        serviceOfferReplicaRepository.save(offer);
        serviceOfferReplicaRepository.save(createOffer(1, "shopSku2", 2));
        sskuStatusRepository.save(
            sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null),
            sskuStatus(1, "shopSku2", OfferAvailability.ACTIVE, null));

        var filter = new MskuFilter();
        var request = new UpdateMskuStatusRequest()
            .setMskuStatus(DisplayMskuStatusValue.fromDbValue(MskuStatusValue.REGULAR))
            .setComment("comment");
        sskuMskuStatusHelperService.updateMskuStatusesAsync(filter, "myuser", request, true, 1);

        var result = backgroundServiceMock.getMidStatus(0);
        Assertions.assertThat(result.getMessage()).isEqualTo("Успешное выполнение. Успешно обработано 2 записей");
        Assertions.assertThat(result.getParams()).isEqualTo(null);

        var status1 = mskuStatusRepository.findById(1L);
        var status2 = mskuStatusRepository.findById(2L);

        Assertions.assertThat(status1).get()
            .extracting(MskuStatus::getMskuStatus, MskuStatus::getComment)
            .containsExactly(MskuStatusValue.REGULAR, "comment");

        Assertions.assertThat(status2).get()
            .extracting(MskuStatus::getMskuStatus, MskuStatus::getComment)
            .containsExactly(MskuStatusValue.REGULAR, "comment");
    }

    @Test
    public void setNpdStartDateIfNpd() {
        deepmindMskuRepository.save(msku(1L));
        mskuStatusRepository.save(mskuStatus(1L, MskuStatusValue.IN_OUT));
        deepmindMskuRepository.save(msku(2L));

        var offer = createOffer(1, "shopSku1", 1);
        serviceOfferReplicaRepository.save(offer);
        serviceOfferReplicaRepository.save(createOffer(1, "shopSku2", 2));
        sskuStatusRepository.save(
            sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null),
            sskuStatus(1, "shopSku2", OfferAvailability.ACTIVE, null));

        sskuMskuStatusHelperService.updateMskuStatuses(List.of(
            mskuState(1L, MskuStatusValue.NPD).setComment("comment"),
            mskuState(2L, MskuStatusValue.NPD).setNpdFinishDate(LocalDate.now().plusDays(20)).setComment("comment")),
            "myuser", false);

        var status1 = mskuStatusRepository.findById(1L);
        var status2 = mskuStatusRepository.findById(2L);
        Assertions.assertThat(status1).get()
            .extracting(MskuStatus::getMskuStatus)
            .isEqualTo(MskuStatusValue.NPD);
        Assertions.assertThat(status1).get()
            .extracting(MskuStatus::getNpdStartDate)
            .isEqualTo(LocalDate.now());
        Assertions.assertThat(status1).get()
            .extracting(MskuStatus::getSeasonId)
            .isNull();
        Assertions.assertThat(status1).get()
            .extracting(MskuStatus::getInoutStartDate)
            .isNull();

        Assertions.assertThat(status2).get()
            .extracting(MskuStatus::getMskuStatus)
            .isEqualTo(MskuStatusValue.NPD);
        Assertions.assertThat(status2).get()
            .extracting(MskuStatus::getNpdStartDate)
            .isEqualTo(LocalDate.now());
        Assertions.assertThat(status2).get()
            .extracting(MskuStatus::getNpdFinishDate)
            .isEqualTo(LocalDate.now().plusDays(20));
    }

    @Test
    public void updateTheSameStatus() {
        deepmindMskuRepository.save(msku(1L));
        mskuStatusRepository.save(mskuStatus(1L, MskuStatusValue.NPD)
            .setNpdStartDate(LocalDate.now().minusDays(1)));
        deepmindMskuRepository.save(msku(2L));

        var offer = createOffer(1, "shopSku1", 1);
        serviceOfferReplicaRepository.save(offer);
        sskuStatusRepository.save(
            sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null));

        var warnings = sskuMskuStatusHelperService.updateMskuStatuses(List.of(
            mskuState(1L, MskuStatusValue.NPD)
                .setNpdFinishDate(LocalDate.now().plusDays(60))
                .setComment("comment")),
            "myuser", false);

        Assertions.assertThat(warnings).isEmpty();
        var status1 = mskuStatusRepository.findById(1L);
        Assertions.assertThat(status1).get()
            .extracting(MskuStatus::getMskuStatus)
            .isEqualTo(MskuStatusValue.NPD);
        Assertions.assertThat(status1).get()
            .extracting(MskuStatus::getNpdStartDate)
            .isEqualTo(LocalDate.now().minusDays(1));
        Assertions.assertThat(status1).get()
            .extracting(MskuStatus::getNpdFinishDate)
            .isEqualTo(LocalDate.now().plusDays(60));
        Assertions.assertThat(status1).get()
            .extracting(MskuStatus::getSeasonId)
            .isNull();
        Assertions.assertThat(status1).get()
            .extracting(MskuStatus::getInoutStartDate)
            .isNull();
    }

    @Test
    public void setNpdStartDateIfNpdInBackground() {
        deepmindMskuRepository.save(msku(1L));
        mskuStatusRepository.save(mskuStatus(1L, MskuStatusValue.REGULAR));
        deepmindMskuRepository.save(msku(2L));

        var offer = createOffer(1, "shopSku1", 1);
        serviceOfferReplicaRepository.save(offer);
        serviceOfferReplicaRepository.save(createOffer(1, "shopSku2", 2));
        sskuStatusRepository.save(
            sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null),
            sskuStatus(1, "shopSku2", OfferAvailability.ACTIVE, null));

        var filter = new MskuFilter();
        var date = LocalDate.now().plusDays(60);
        var request = new UpdateMskuStatusRequest()
            .setMskuStatus(DisplayMskuStatusValue.fromDbValue(MskuStatusValue.NPD))
            .setNpdFinishDate(date);
        sskuMskuStatusHelperService.updateMskuStatusesAsync(filter, "user", request, false, 1);

        var result = backgroundServiceMock.getMidStatus(0);
        Assertions.assertThat(result.getMessage()).isEqualTo("Успешное выполнение. Успешно обработано 2 записей");
        Assertions.assertThat(result.getParams()).isEqualTo(null);

        var status1 = mskuStatusRepository.findById(1L);
        var status2 = mskuStatusRepository.findById(2L);
        Assertions.assertThat(status1).get()
            .extracting(MskuStatus::getMskuStatus, MskuStatus::getNpdStartDate)
            .containsExactly(MskuStatusValue.NPD, LocalDate.now());
        Assertions.assertThat(status2).get()
            .extracting(MskuStatus::getMskuStatus, MskuStatus::getNpdStartDate)
            .containsExactly(MskuStatusValue.NPD, LocalDate.now());
    }

    @Test
    public void dontSaveIfStatusDontChange() {
        deepmindMskuRepository.save(msku(1L));
        mskuStatusRepository.save(mskuStatus(1L, MskuStatusValue.NPD)
            .setNpdStartDate(LocalDate.now().minusDays(5))
        );
        var modifiedAt = mskuStatusRepository.findById(1L).get().getModifiedAt();

        var offer = createOffer(1, "shopSku1", 1);
        serviceOfferReplicaRepository.save(offer);
        sskuStatusRepository.save(
            sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null));

        var warnings = sskuMskuStatusHelperService.updateMskuStatuses(
            List.of(mskuState(1L, MskuStatusValue.NPD)),
            "user", false);
        Assertions.assertThat(warnings).isEmpty();

        var statusO = mskuStatusRepository.findById(1L);
        Assertions.assertThat(statusO).get()
            .extracting(MskuStatus::getModifiedAt)
            .isEqualTo(modifiedAt);
    }

    @Test
    public void dontSaveIfStatusDontChangeInBackground() {
        deepmindMskuRepository.save(msku(1L));
        mskuStatusRepository.save(mskuStatus(1L, MskuStatusValue.NPD)
            .setMskuStatus(MskuStatusValue.REGULAR)
            .setNpdStartDate(null)
        );

        var modifiedAt = mskuStatusRepository.findById(1L).get().getModifiedAt();

        var offer = createOffer(1, "shopSku1", 1);
        serviceOfferReplicaRepository.save(offer);
        sskuStatusRepository.save(
            sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null));

        var filter = new MskuFilter().setMarketSkuIds(1L);
        var request = new UpdateMskuStatusRequest()
            .setMskuStatus(DisplayMskuStatusValue.fromDbValue(MskuStatusValue.REGULAR));
        sskuMskuStatusHelperService.updateMskuStatusesAsync(filter, "user", request, false, 1);

        var result = backgroundServiceMock.getMidStatus(0);
        Assertions.assertThat(result.getMessage()).isEqualTo("Успешное выполнение. Успешно обработано 1 записей");
        Assertions.assertThat(result.getParams()).isEqualTo(null);

        var statusO = mskuStatusRepository.findById(1L);
        Assertions.assertThat(statusO).get()
            .extracting(MskuStatus::getModifiedAt)
            .isEqualTo(modifiedAt);
    }

    @Test
    public void isSskuModifiedByUser() {
        deepmindMskuRepository.save(msku(1L));
        mskuStatusRepository.save(mskuStatus(1L, MskuStatusValue.REGULAR));

        var offer = createOffer(1, "shopSku1", 1);
        serviceOfferReplicaRepository.save(offer);
        sskuStatusRepository.save(sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null));

        sskuMskuStatusHelperService.updateMskuStatuses(List.of(mskuState(1L, MskuStatusValue.NPD)), "user", false);

        var statusO = mskuStatusRepository.findById(1L);
        Assertions.assertThat(statusO).get()
            .extracting(MskuStatus::getModifiedByUser)
            .isEqualTo(true);
    }

    @Test
    public void saveInactiveTempWithStartAt() {
        var supplier = create1PSupplier(1, "000076");
        deepmindSupplierRepository.save(supplier);
        var offer = createOffer(1, "shopSku1", 1);
        serviceOfferReplicaRepository.save(offer);
        sskuStatusRepository.save(sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null));

        sskuMskuStatusHelperService.updateSskuPlannedStatuses(List.of(
            plannedSskuState(1, "shopSku1")
                .setStartAt(Instant.parse("2020-12-03T10:15:30.00Z"))
                .setFinishAt(Instant.parse("2020-12-20T10:15:30.00Z"))
                .setComment("To INACTIVE_TMP")
        ), "unit-test", false);

        var status = sskuStatusRepository.findByKey(1, "shopSku1");
        Assertions.assertThat(status).get()
            .isEqualToIgnoringGivenFields(
                sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null)
                    .setPlannedStartAt(Instant.parse("2020-12-03T10:15:30.00Z"))
                    .setPlannedFinishAt(Instant.parse("2020-12-20T10:15:30.00Z"))
                    .setPlannedComment("To INACTIVE_TMP"),
                "modifiedAt", "modifiedLogin", "statusStartAt", "previousAvailability",
                "modifiedByUser", "hasNoPurchasePrice", "hasNoValidContract"
            );
    }

    @Test
    public void saveInactiveTemp3P() {
        var supplier = create3pSupplier(1);
        deepmindSupplierRepository.save(supplier);
        var offer = createOffer(1, "shopSku1", 1);
        serviceOfferReplicaRepository.save(offer);
        sskuStatusRepository.save(sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null));

        Assertions.assertThatThrownBy(() -> sskuMskuStatusHelperService.updateSskuPlannedStatuses(List.of(
            plannedSskuState(1, "shopSku1")
                .setStartAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .setFinishAt(Instant.now().plus(3, ChronoUnit.DAYS))
                .setComment("To INACTIVE_TMP")
        ), "unit-test", false))
        .hasMessageContaining("only allowed for 1P supplier");
    }

    @Test
    public void savePlannedWithAlreadyInactiveTmp() {
        var supplier = create1PSupplier(1, "000076");
        deepmindSupplierRepository.save(supplier);
        var offer = createOffer(1, "shopSku1", 1);
        serviceOfferReplicaRepository.save(offer);
        sskuStatusRepository.save(sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null));

        // first save inactive_tmp
        var warnings1 = sskuMskuStatusHelperService.updateSskuStatuses(List.of(
            sskuState(1, "shopSku1", OfferAvailability.INACTIVE_TMP, null)
                .setStatusFinishAt(Instant.parse("2020-12-01T10:15:30.00Z"))
                .setComment("Comment")
        ), "user", false);
        Assertions.assertThat(warnings1).isEmpty();

        // check, that inactive_tmp is saved
        Assertions.assertThat(sskuStatusRepository.findByKey(1, "shopSku1")).get()
            .extracting(SskuStatus::getAvailability, SskuStatus::getStatusFinishAt)
            .containsExactly(OfferAvailability.INACTIVE_TMP, Instant.parse("2020-12-01T10:15:30.00Z"));

        // save planned INACTIVE_TMP
        var warnings2 = sskuMskuStatusHelperService.updateSskuPlannedStatuses(List.of(
            plannedSskuState(1, "shopSku1")
                .setStartAt(Instant.parse("2020-12-03T10:15:30.00Z"))
                .setFinishAt(Instant.parse("2020-12-20T10:15:30.00Z"))
                .setComment("To INACTIVE_TMP")
        ), "unit-test", false);
        Assertions.assertThat(warnings2).isEmpty();

        Assertions.assertThat(sskuStatusRepository.findByKey(1, "shopSku1")).get()
            .usingRecursiveComparison()
            .ignoringFields("modifiedAt", "modifiedLogin", "statusStartAt", "previousAvailability",
                "modifiedByUser", "hasNoPurchasePrice", "hasNoValidContract")
            .isEqualTo(
                sskuStatus(1, "shopSku1", OfferAvailability.INACTIVE_TMP, null)
                    .setStatusFinishAt(Instant.parse("2020-12-01T10:15:30.00Z"))
                    .setComment("Comment")
                    .setPlannedStartAt(Instant.parse("2020-12-03T10:15:30.00Z"))
                    .setPlannedFinishAt(Instant.parse("2020-12-20T10:15:30.00Z"))
                    .setPlannedComment("To INACTIVE_TMP")
            );
    }

    private void prepareDefaultState() {
        deepmindSupplierRepository.save(create3pSupplier(1), create3pSupplier(2));
        serviceOfferReplicaRepository.save(
            createOffer(1, "shopSku1", 1),
            createOffer(2, "shopSku2", 2)
        );

        sskuStatusRepository.save(
            sskuStatus(1, "shopSku1", OfferAvailability.ACTIVE, null),
            sskuStatus(2, "shopSku2", OfferAvailability.ACTIVE, null)
        );

        deepmindMskuRepository.save(msku(1L), msku(2L));
        mskuStatusRepository.save(
            mskuStatus(1, MskuStatusValue.REGULAR),
            mskuStatus(2, MskuStatusValue.REGULAR)
        );
    }

    private Supplier create1PSupplier(int id, String rsId) {
        return new Supplier().setId(id).setName("test").setSupplierType(REAL_SUPPLIER).setRealSupplierId(rsId);
    }

    private Supplier create3pSupplier(int id) {
        return new Supplier().setId(id).setName("test").setSupplierType(THIRD_PARTY);
    }

    private Msku msku(long mskuId) {
        return new Msku()
            .setId(mskuId)
            .setTitle("Msku #" + mskuId)
            .setDeleted(false)
            .setVendorId(1L)
            .setModifiedTs(Instant.now())
            .setCategoryId(1L)
            .setSkuType(SkuTypeEnum.SKU)
            .setDeleted(false);
    }

    private MskuStatus mskuStatus(long mskuId, MskuStatusValue status) {
        var mskuStatus = new MskuStatus()
            .setMarketSkuId(mskuId)
            .setMskuStatus(status)
            .setStatusStartAt(Instant.now())
            .setNpdStartDate(LocalDate.now());
        if (status == MskuStatusValue.NPD) {
            mskuStatus.setNpdStartDate(LocalDate.now());
        }
        if (status == MskuStatusValue.IN_OUT) {
            mskuStatus.setInoutStartDate(LocalDate.now());
            mskuStatus.setInoutFinishDate(LocalDate.now().plusDays(60));
        }
        return mskuStatus;
    }

    private MskuAvailabilityStatusToSave mskuState(long mskuId, MskuStatusValue status) {
        var mskuState = new MskuAvailabilityStatusToSave()
            .setMskuId(mskuId)
            .setNewStatus(DisplayMskuStatusValue.valueOf(status.name()));
        if (status == MskuStatusValue.IN_OUT) {
            mskuState.setInoutFinishDate(LocalDate.now().plusDays(60));
        }
        return mskuState;
    }

    private SskuStatus sskuStatus(int supplierId, String shopSku, OfferAvailability availability, String comment) {
        return new SskuStatus()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setAvailability(availability)
            .setComment(comment);
    }

    private SskuStatusToSave sskuState(int supplierId, String shopSku, OfferAvailability availability,
                                       String comment) {
        return new SskuStatusToSave()
            .setShopSkuKey(new ServiceOfferKey(supplierId, shopSku))
            .setNewAvailabilityStatus(availability)
            .setComment(comment);
    }

    private SskuPlannedStatusToSave plannedSskuState(int supplierId, String shopSku) {
        return new SskuPlannedStatusToSave()
            .setShopSkuKey(new ServiceOfferKey(supplierId, shopSku));
    }

    private ServiceOfferReplica createOffer(int supplierId, String shopSku, long mskuId) {
        var suppliers = deepmindSupplierRepository.findByIds(List.of(supplierId));
        Supplier supplier;
        if (suppliers.isEmpty()) {
            supplier = new Supplier()
                .setId(supplierId)
                .setName("test_supplier_" + supplierId)
                .setSupplierType(THIRD_PARTY);
            deepmindSupplierRepository.save(supplier);
        } else {
            supplier = suppliers.get(0);
        }
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setCategoryId(1L)
            .setSeqId(0L)
            .setMskuId(mskuId)
            .setSupplierType(supplier.getSupplierType())
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }
}
