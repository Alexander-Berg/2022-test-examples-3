package ru.yandex.market.deepmind.app.controllers;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deepmind.app.DeepmindBaseAppDbTestClass;
import ru.yandex.market.deepmind.app.pojo.DisplaySskuStatusWarning;
import ru.yandex.market.deepmind.app.utils.DeepmindUtils;
import ru.yandex.market.deepmind.app.web.aprove.BusinessProcess;
import ru.yandex.market.deepmind.app.web.aprove.DisplayShopSkuStatusApproveInfo;
import ru.yandex.market.deepmind.app.web.aprove.StartBusinessProcessAsyncRequest;
import ru.yandex.market.deepmind.app.web.aprove.StartBusinessProcessRequest;
import ru.yandex.market.deepmind.app.web.ssku_availability.ShopSkuAvailabilityWebFilter;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.mocks.BeruIdMock;
import ru.yandex.market.deepmind.common.mocks.StorageKeyValueServiceMock;
import ru.yandex.market.deepmind.common.openapi.ReplenishmentService;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryTeamRepository;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.SeasonalMskuRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.msku.info.MskuInfoRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusFilter;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.stock.MskuStockRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.background.BackgroundServiceMock;
import ru.yandex.market.deepmind.common.services.category.DeepmindCategoryCachingService;
import ru.yandex.market.deepmind.common.services.category.DeepmindCategoryCachingServiceMock;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverter;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverterImpl;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuHelperService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusServiceImpl;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusValidationServiceImpl;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichApproveToInactiveExcelComposer;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichApproveToPendingExcelComposer;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichSpecialOrderExcelComposer;
import ru.yandex.market.deepmind.common.services.tracker_strategy.ApproveWithAssortmentCommitteeHelper;
import ru.yandex.market.deepmind.common.services.tracker_strategy.BackToPendingApproveStrategy;
import ru.yandex.market.deepmind.common.services.tracker_strategy.SpecialOrderStrategy;
import ru.yandex.market.deepmind.common.services.tracker_strategy.ToInactiveApproveStrategy;
import ru.yandex.market.deepmind.common.services.tracker_strategy.ToPendingApproveStrategy;
import ru.yandex.market.deepmind.common.services.yt.EnrichApproveToPendingLoader;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverDataRepository;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverTicketRepository;
import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverFactory;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionStatus.ActionStatus;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingService;
import ru.yandex.market.tracker.tracker.MockSession;

import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.ACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.DELISTED;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.INACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.INACTIVE_TMP;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.PENDING;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.REAL_SUPPLIER;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.THIRD_PARTY;
import static ru.yandex.market.deepmind.common.pojo.SskuStatusReason.NO_PURCHASE_PRICE;
import static ru.yandex.market.deepmind.common.pojo.SskuStatusReason.WAITING_FOR_ENTER;

public class ShopSkuStatusApproveControllerTest extends DeepmindBaseAppDbTestClass {

    private ShopSkuStatusApproveController controller;
    private BackgroundActionResultController backgroundController;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private SskuStatusRepository sskuStatusRepository;
    @Resource
    private OffersConverter offersConverter;
    @Resource
    private MskuStatusRepository mskuStatusRepository;
    @Resource
    private MskuRepository deepmindMskuRepository;
    @Resource
    private MskuStockRepository mskuStockRepository;
    @Resource
    private DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource(name = "deepmindTransactionHelper")
    private TransactionHelper transactionHelper;
    @Resource
    private TrackerApproverDataRepository trackerApproverDataRepository;
    @Resource
    private TrackerApproverTicketRepository trackerApproverTicketRepository;
    @Resource
    private DeepmindCategoryManagerRepository deepmindCategoryManagerRepository;
    @Resource
    private DeepmindCategoryTeamRepository deepmindCategoryTeamRepository;
    @Resource
    private ObjectMapper trackerApproverObjectMapper;
    @Resource
    protected ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    private SskuMskuStatusService sskuMskuStatusService;
    private BackgroundServiceMock backgroundServiceMock;
    private DeepmindUtils deepmindUtils;
    private String deepmindRobotLogin = "robot-deepmind-login";
    private StorageKeyValueServiceMock storageKeyValueService = new StorageKeyValueServiceMock();

    @Before
    public void setUp() throws Exception {
        offersConverter.clearCache();
        var factory = new TrackerApproverFactory(
            trackerApproverDataRepository,
            trackerApproverTicketRepository,
            transactionTemplate,
            trackerApproverObjectMapper);
        var trackerSession = new MockSession();
        var open = trackerSession.statuses().add("open");
        var closed = trackerSession.statuses().add("closed");
        trackerSession.transitions().add(List.of(open), "close", closed);

        var sskuMskuHelperService = new SskuMskuHelperService(serviceOfferReplicaRepository, sskuStatusRepository,
            mskuStatusRepository);
        var sskuMskuStatusValidationService = new SskuMskuStatusValidationServiceImpl(mskuStockRepository,
            serviceOfferReplicaRepository, deepmindSupplierRepository, sskuMskuHelperService);
        sskuMskuStatusService = new SskuMskuStatusServiceImpl(sskuStatusRepository, mskuStatusRepository,
        sskuMskuStatusValidationService, sskuMskuHelperService, transactionTemplate);
        var enrichToPendingExelComposer = new EnrichApproveToPendingExcelComposer(deepmindMskuRepository,
            deepmindSupplierRepository, Mockito.mock(GlobalVendorsCachingService.class),
            Mockito.mock(MasterDataHelperService.class), serviceOfferReplicaRepository,
            Mockito.mock(DeepmindCategoryCachingService.class),
            Mockito.mock(DeepmindCategoryManagerRepository.class), Mockito.mock(DeepmindCategoryTeamRepository.class),
            Mockito.mock(MskuInfoRepository.class), Mockito.mock(SeasonRepository.class),
            Mockito.mock(SeasonalMskuRepository.class), Mockito.mock(EnrichApproveToPendingLoader.class),
            Mockito.mock(OffersConverter.class), Mockito.mock(SskuStatusRepository.class)
        );
        var enrichSpecialOrderExcelComposer = new EnrichSpecialOrderExcelComposer(deepmindMskuRepository,
            deepmindSupplierRepository, Mockito.mock(GlobalVendorsCachingService.class),
            Mockito.mock(MasterDataHelperService.class), serviceOfferReplicaRepository,
            Mockito.mock(DeepmindCategoryCachingService.class),
            Mockito.mock(DeepmindCategoryManagerRepository.class),
            Mockito.mock(DeepmindCategoryTeamRepository.class),
            Mockito.mock(MskuInfoRepository.class),
            Mockito.mock(SeasonRepository.class),
            Mockito.mock(SeasonalMskuRepository.class),
            Mockito.mock(EnrichApproveToPendingLoader.class),
            offersConverter,
            Mockito.mock(SskuStatusRepository.class)
        );
        var enrichToInactiveExelComposer = new EnrichApproveToInactiveExcelComposer(deepmindMskuRepository,
            deepmindSupplierRepository,
            serviceOfferReplicaRepository, Mockito.mock(MskuInfoRepository.class));

        factory.registerStrategy(new ToPendingApproveStrategy(trackerSession,
            deepmindSupplierRepository,
            Mockito.mock(ApproveWithAssortmentCommitteeHelper.class),
            sskuMskuStatusService,
            sskuStatusRepository,
            "TEST",
            enrichToPendingExelComposer,
            transactionHelper,
            deepmindWarehouseRepository,
            deepmindRobotLogin));
        factory.registerStrategy(new ToInactiveApproveStrategy(trackerSession,
            deepmindSupplierRepository,
            Mockito.mock(ApproveWithAssortmentCommitteeHelper.class),
            enrichToInactiveExelComposer,
            sskuMskuStatusService,
            sskuStatusRepository,
            "TEST",
            transactionHelper,
            deepmindRobotLogin,
            storageKeyValueService));
        factory.registerStrategy(new BackToPendingApproveStrategy(trackerSession,
            deepmindSupplierRepository,
            Mockito.mock(ApproveWithAssortmentCommitteeHelper.class),
            sskuMskuStatusService,
            sskuStatusRepository,
            "TEST",
            enrichToPendingExelComposer,
            transactionHelper,
            deepmindWarehouseRepository,
            deepmindRobotLogin));
        factory.registerStrategy(new SpecialOrderStrategy(trackerSession,
            deepmindSupplierRepository,
            Mockito.mock(ApproveWithAssortmentCommitteeHelper.class),
            sskuMskuStatusService,
            sskuStatusRepository,
            "TEST",
            enrichSpecialOrderExcelComposer,
            Mockito.mock(ReplenishmentService.class),
            Mockito.mock(TransactionHelper.class),
            Mockito.mock(OffersConverter.class),
            deepmindWarehouseRepository,
            "https://url-to-demand.ru/{demandId}",
            deepmindRobotLogin
        ));

        backgroundServiceMock = new BackgroundServiceMock();
        deepmindUtils = new DeepmindUtils(
            new OffersConverterImpl(jdbcTemplate, new BeruIdMock(), deepmindSupplierRepository),
            deepmindCategoryManagerRepository,
            deepmindCategoryTeamRepository,
            new DeepmindCategoryCachingServiceMock()
        );
        backgroundController = new BackgroundActionResultController(backgroundServiceMock);

        controller = new ShopSkuStatusApproveController(
            serviceOfferReplicaRepository,
            sskuStatusRepository,
            backgroundServiceMock,
            deepmindUtils,
            trackerSession,
            factory.getFacade(ToInactiveApproveStrategy.TYPE),
            factory.getFacade(ToPendingApproveStrategy.TYPE),
            factory.getFacade(BackToPendingApproveStrategy.TYPE),
            factory.getFacade(SpecialOrderStrategy.TYPE),
            factory.getFacade(ToPendingApproveStrategy.TYPE),
            factory.getFacade(BackToPendingApproveStrategy.TYPE),
            factory.getFacade(SpecialOrderStrategy.TYPE),
            new StorageKeyValueServiceMock()
        );
    }

    @Test
    public void startProcess() {
        deepmindSupplierRepository.save(
            create1PSupplier(1, "000008"),
            create1PSupplier(2, "000009"),
            create1PSupplier(3, "000010"),
            create1PSupplier(4, "000011"),
            create1PSupplier(5, "000012")
        );
        insertOffer(1, "ssku-1", INACTIVE_TMP);
        insertOffer(2, "ssku-2", INACTIVE_TMP);
        insertOffer(3, "ssku-3", ACTIVE);
        insertOffer(4, "ssku-4", INACTIVE);
        insertOffer(5, "ssku-5", DELISTED);

        var key1 = new ServiceOfferKey(1, "ssku-1");
        var key2 = new ServiceOfferKey(2, "ssku-2");
        var key3 = new ServiceOfferKey(3, "ssku-3");
        var key4 = new ServiceOfferKey(4, "ssku-4");
        var key5 = new ServiceOfferKey(5, "ssku-5");

        var sskuStatuses = sskuStatusRepository.findMap(new SskuStatusFilter()
            .addShopSkuKeys(key1, key2));
        sskuStatusRepository.save(
            sskuStatuses.get(key1).setReason(WAITING_FOR_ENTER).setComment(WAITING_FOR_ENTER.getLiteral()),
            sskuStatuses.get(key2).setReason(WAITING_FOR_ENTER).setComment(WAITING_FOR_ENTER.getLiteral())
        );

        var errorsToPending = controller.start(new StartBusinessProcessRequest()
            .setKeys(List.of(key1, key2))
            .setComment("comment")
            .setBusinessProcess(BusinessProcess.TO_PENDING)
        );

        var errorsToInactive = controller.start(new StartBusinessProcessRequest()
            .setKeys(List.of(key3))
            .setComment("comment")
            .setBusinessProcess(BusinessProcess.TO_INACTIVE)
        );

        var errorsToBackToPending = controller.start(new StartBusinessProcessRequest()
            .setKeys(List.of(key4, key5))
            .setComment("comment")
            .setBusinessProcess(BusinessProcess.BACK_TO_PENDING)
        );

        Assertions.assertThat(errorsToPending).isEmpty();
        Assertions.assertThat(errorsToInactive).isEmpty();
        Assertions.assertThat(errorsToBackToPending).isEmpty();

        var infos = controller.list(List.of(key1, key2, key3, key4, key5));

        Assertions.assertThat(infos).containsExactlyInAnyOrder(
            displayApproveData(1, "ssku-1", "TEST-1", BusinessProcess.TO_PENDING),
            displayApproveData(2, "ssku-2", "TEST-1", BusinessProcess.TO_PENDING),
            displayApproveData(3, "ssku-3", "TEST-2", BusinessProcess.TO_INACTIVE),
            displayApproveData(4, "ssku-4", "TEST-3", BusinessProcess.BACK_TO_PENDING),
            displayApproveData(5, "ssku-5", "TEST-3", BusinessProcess.BACK_TO_PENDING)
        );
    }

    @Test
    public void validateOnly1P() {
        deepmindSupplierRepository.save(
            create1PSupplier(1, "000008"),
            create1PSupplier(2, "000009"),
            create3pSupplier(3)
        );
        insertOffer(1, "ssku-1", INACTIVE_TMP);
        insertOffer(2, "ssku-2", ACTIVE);
        insertOffer(3, "ssku-3", INACTIVE_TMP);

        var key1 = new ServiceOfferKey(1, "ssku-1");
        var key3 = new ServiceOfferKey(3, "ssku-3");

        var sskuStatuses = sskuStatusRepository.findMap(new SskuStatusFilter()
            .addShopSkuKeys(key1, key3));
        sskuStatusRepository.save(
            sskuStatuses.get(key1).setReason(WAITING_FOR_ENTER).setComment(WAITING_FOR_ENTER.getLiteral()),
            sskuStatuses.get(key3).setReason(WAITING_FOR_ENTER).setComment(WAITING_FOR_ENTER.getLiteral())
        );

        var errorsToPending = controller.start(new StartBusinessProcessRequest()
            .setKeys(List.of(key1, key3))
            .setComment("comment")
            .setBusinessProcess(BusinessProcess.TO_PENDING)
        );
        Assertions.assertThat(errorsToPending)
            .containsExactlyInAnyOrder(DisplaySskuStatusWarning.of(3, "ssku-3",
                "Для 3P запрещен запуск бизнес процесса"));

        var infos = controller.list(List.of(key1, key3));

        Assertions.assertThat(infos).isEmpty();
    }

    @Test
    public void validateStartOfSecondBP() {
        deepmindSupplierRepository.save(
            create1PSupplier(1, "000008"),
            create1PSupplier(2, "000009"),
            create1PSupplier(3, "000010")
        );
        insertOffer(1, "ssku-1", INACTIVE_TMP);
        insertOffer(2, "ssku-2", INACTIVE_TMP);
        insertOffer(3, "ssku-3", ACTIVE);

        var key1 = new ServiceOfferKey(1, "ssku-1");
        var key2 = new ServiceOfferKey(2, "ssku-2");
        var key3 = new ServiceOfferKey(3, "ssku-3");

        var sskuStatuses = sskuStatusRepository.findMap(new SskuStatusFilter()
            .addShopSkuKeys(key1, key2));
        sskuStatusRepository.save(
            sskuStatuses.get(key1).setReason(WAITING_FOR_ENTER).setComment(WAITING_FOR_ENTER.getLiteral()),
            sskuStatuses.get(key2).setReason(WAITING_FOR_ENTER).setComment(WAITING_FOR_ENTER.getLiteral())
        );

        var errorsToPending = controller.start(new StartBusinessProcessRequest()
            .setKeys(List.of(key1, key2))
            .setComment("comment")
            .setBusinessProcess(BusinessProcess.TO_PENDING)
        );
        Assertions.assertThat(errorsToPending).isEmpty();

        var errorsToInactive = controller.start(new StartBusinessProcessRequest()
            .setKeys(List.of(key1, key3))
            .setComment("comment")
            .setBusinessProcess(BusinessProcess.TO_INACTIVE)
        );
        Assertions.assertThat(errorsToInactive).containsExactlyInAnyOrder(
            DisplaySskuStatusWarning.of(1, "ssku-1", "По офферу уже запущен процесс согласования в TEST-1")
        );
    }

    @Test
    public void startProcessAsync() {
        deepmindSupplierRepository.save(
            create1PSupplier(1, "000008"),
            create1PSupplier(2, "000009"),
            create1PSupplier(3, "000010"),
            create1PSupplier(4, "000011"),
            create1PSupplier(5, "000012")
        );
        insertOffer(1, "ssku-1", INACTIVE_TMP);
        insertOffer(2, "ssku-2", INACTIVE_TMP);
        insertOffer(3, "ssku-3", ACTIVE);
        insertOffer(4, "ssku-4", DELISTED);
        insertOffer(5, "ssku-5", INACTIVE);


        var key1 = new ServiceOfferKey(1, "ssku-1");
        var key2 = new ServiceOfferKey(2, "ssku-2");
        var key3 = new ServiceOfferKey(3, "ssku-3");
        var key4 = new ServiceOfferKey(4, "ssku-4");
        var key5 = new ServiceOfferKey(5, "ssku-5");

        var sskuStatuses = sskuStatusRepository.findMap(new SskuStatusFilter()
            .addShopSkuKeys(key1, key2));
        sskuStatusRepository.save(
            sskuStatuses.get(key1).setReason(WAITING_FOR_ENTER).setComment(WAITING_FOR_ENTER.getLiteral()),
            sskuStatuses.get(key2).setReason(WAITING_FOR_ENTER).setComment(WAITING_FOR_ENTER.getLiteral())
        );

        var toPendingId = controller.startAsync(new StartBusinessProcessAsyncRequest()
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setSupplierIds(List.of(1, 2)))
            .setComment("comment")
            .setBusinessProcess(BusinessProcess.TO_PENDING)
        );

        var toInactiveId = controller.startAsync(new StartBusinessProcessAsyncRequest()
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setSupplierIds(List.of(3)))
            .setComment("comment")
            .setBusinessProcess(BusinessProcess.TO_INACTIVE)
        );

        var backToPendingId = controller.startAsync(new StartBusinessProcessAsyncRequest()
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setSupplierIds(List.of(4, 5)))
            .setComment("comment")
            .setBusinessProcess(BusinessProcess.BACK_TO_PENDING)
        );

        var actionResultToPending = backgroundController.getActionResult(toPendingId);
        var actionResultToInactive = backgroundController.getActionResult(toInactiveId);
        var actionResultBackToPending = backgroundController.getActionResult(backToPendingId);

        Assertions.assertThat(actionResultToPending.getStatus()).isEqualTo(ActionStatus.FINISHED);
        Assertions.assertThat(actionResultToPending.getMessage()).contains("Тикет создан");
        Assertions.assertThat(actionResultToPending.getParams()).isEqualTo("TEST-1");
        Assertions.assertThat(actionResultToInactive.getStatus()).isEqualTo(ActionStatus.FINISHED);
        Assertions.assertThat(actionResultToInactive.getMessage()).contains("Тикет создан");
        Assertions.assertThat(actionResultToInactive.getParams()).isEqualTo("TEST-2");
        Assertions.assertThat(actionResultBackToPending.getStatus()).isEqualTo(ActionStatus.FINISHED);
        Assertions.assertThat(actionResultBackToPending.getMessage()).contains("Тикет создан");
        Assertions.assertThat(actionResultBackToPending.getParams()).isEqualTo("TEST-3");

        var infos = controller.list(List.of(key1, key2, key3, key4, key5));

        Assertions.assertThat(infos).containsExactlyInAnyOrder(
            displayApproveData(1, "ssku-1", "TEST-1", BusinessProcess.TO_PENDING),
            displayApproveData(2, "ssku-2", "TEST-1", BusinessProcess.TO_PENDING),
            displayApproveData(3, "ssku-3", "TEST-2", BusinessProcess.TO_INACTIVE),
            displayApproveData(4, "ssku-4", "TEST-3", BusinessProcess.BACK_TO_PENDING),
            displayApproveData(5, "ssku-5", "TEST-3", BusinessProcess.BACK_TO_PENDING)
        );
    }

    @Test
    public void validateAsync() {
        deepmindSupplierRepository.save(
            create1PSupplier(1, "000008"),
            create1PSupplier(2, "000009"),
            create3pSupplier(3)
        );
        insertOffer(1, "ssku-1", INACTIVE_TMP);
        insertOffer(2, "ssku-2", ACTIVE);
        insertOffer(3, "ssku-3", INACTIVE_TMP);

        var key1 = new ServiceOfferKey(1, "ssku-1");
        var key3 = new ServiceOfferKey(3, "ssku-3");

        var sskuStatuses = sskuStatusRepository.findMap(new SskuStatusFilter()
            .addShopSkuKeys(key1, key3));
        sskuStatusRepository.save(
            sskuStatuses.get(key1).setReason(WAITING_FOR_ENTER).setComment(WAITING_FOR_ENTER.getLiteral()),
            sskuStatuses.get(key3).setReason(WAITING_FOR_ENTER).setComment(WAITING_FOR_ENTER.getLiteral())
        );

        var toPendingId = controller.startAsync(new StartBusinessProcessAsyncRequest()
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setSupplierIds(List.of(1, 3)))
            .setComment("comment")
            .setBusinessProcess(BusinessProcess.TO_PENDING)
        );

        var actionResultToPending = backgroundController.getActionResult(toPendingId);
        Assertions.assertThat(actionResultToPending.getStatus()).isEqualTo(ActionStatus.FAILED);
        Assertions.assertThat((Collection<DisplaySskuStatusWarning>) actionResultToPending.getParams())
            .containsExactlyInAnyOrder(
                DisplaySskuStatusWarning.of(3, "ssku-3", "Для 3P запрещен запуск бизнес процесса")
            );

        var infos = controller.list(List.of(key1, key3));
        Assertions.assertThat(infos).isEmpty();
    }

    @Test
    public void validateToPendingReasonAndStatus() {
        deepmindSupplierRepository.save(
            create1PSupplier(1, "000008"),
            create1PSupplier(2, "000009"),
            create1PSupplier(3, "000010")
        );
        insertOffer(1, "ssku-1", INACTIVE_TMP);
        insertOffer(2, "ssku-2", INACTIVE_TMP);
        insertOffer(3, "ssku-3", ACTIVE);

        var key1 = new ServiceOfferKey(1, "ssku-1");
        var key2 = new ServiceOfferKey(2, "ssku-2");
        var key3 = new ServiceOfferKey(3, "ssku-3");

        var sskuStatuses = sskuStatusRepository.findMap(new SskuStatusFilter()
            .addShopSkuKeys(key1, key2, key3));
        sskuStatusRepository.save(
            sskuStatuses.get(key1).setReason(WAITING_FOR_ENTER).setComment(WAITING_FOR_ENTER.getLiteral()),
            sskuStatuses.get(key3).setReason(WAITING_FOR_ENTER).setComment(WAITING_FOR_ENTER.getLiteral())
        );

        var errorsToPending = controller.start(new StartBusinessProcessRequest()
            .setKeys(List.of(key1, key2, key3))
            .setComment("comment")
            .setBusinessProcess(BusinessProcess.TO_PENDING)
        );

        Assertions
            .assertThat(errorsToPending)
            .extracting(DisplaySskuStatusWarning::getErrorMessage)
            .containsExactlyInAnyOrder(
                MbocErrors.get().sskuWrongStateForToPending(key2.toString(), INACTIVE_TMP.getLiteral(),
                    INACTIVE_TMP.getLiteral(), WAITING_FOR_ENTER.getLiteral()).toString(),
                MbocErrors.get().sskuWrongStateForToPending(key3.toString(), ACTIVE.getLiteral(),
                    INACTIVE_TMP.getLiteral(), WAITING_FOR_ENTER.getLiteral()).toString()
            );
    }

    @Test
    public void validateToInactiveStatus() {
        deepmindSupplierRepository.save(
            create1PSupplier(1, "000008"),
            create1PSupplier(2, "000009"),
            create1PSupplier(3, "000010"),
            create1PSupplier(4, "000011")
        );
        insertOffer(1, "ssku-1", INACTIVE_TMP);
        insertOffer(2, "ssku-2", PENDING);
        insertOffer(3, "ssku-3", ACTIVE);
        insertOffer(4, "ssku-4", DELISTED);

        var key1 = new ServiceOfferKey(1, "ssku-1");
        var key2 = new ServiceOfferKey(2, "ssku-2");
        var key3 = new ServiceOfferKey(3, "ssku-3");
        var key4 = new ServiceOfferKey(4, "ssku-4");

        var errorsToInactive = controller.start(new StartBusinessProcessRequest()
            .setKeys(List.of(key1, key2, key3, key4))
            .setComment("comment")
            .setBusinessProcess(BusinessProcess.TO_INACTIVE)
        );

        Assertions
            .assertThat(errorsToInactive)
            .extracting(DisplaySskuStatusWarning::getErrorMessage)
            .containsExactlyInAnyOrder("Ssku (supplier_id: 4; shop_sku: ssku-4) со статусом 'DELISTED' " +
                "должен быть в одном из статусов: ACTIVE, PENDING, INACTIVE_TMP."
            );
    }

    @Test
    public void validateBackToPendingStatus() {
        deepmindSupplierRepository.save(
            create1PSupplier(1, "000008"),
            create1PSupplier(2, "000009"),
            create1PSupplier(3, "000010"),
            create1PSupplier(4, "000011")
        );
        insertOffer(1, "ssku-1", INACTIVE_TMP, NO_PURCHASE_PRICE.getLiteral());
        insertOffer(2, "ssku-2", PENDING);
        insertOffer(3, "ssku-3", INACTIVE);
        insertOffer(4, "ssku-4", DELISTED);

        var key1 = new ServiceOfferKey(1, "ssku-1");
        var key2 = new ServiceOfferKey(2, "ssku-2");
        var key3 = new ServiceOfferKey(3, "ssku-3");
        var key4 = new ServiceOfferKey(4, "ssku-4");

        var errorsBackToPending = controller.start(new StartBusinessProcessRequest()
            .setKeys(List.of(key1, key2, key3, key4))
            .setComment("comment")
            .setBusinessProcess(BusinessProcess.BACK_TO_PENDING)
        );

        Assertions
            .assertThat(errorsBackToPending)
            .extracting(DisplaySskuStatusWarning::getErrorMessage)
            .containsExactlyInAnyOrder(
                MbocErrors.get().sskuWrongState(key1.toString(), INACTIVE_TMP.name(), "INACTIVE, DELISTED").toString(),
                MbocErrors.get().sskuWrongState(key2.toString(), PENDING.name(), "INACTIVE, DELISTED").toString());
    }

    private DisplayShopSkuStatusApproveInfo displayApproveData(int supplierId, String ssku,
                                                               String ticket, BusinessProcess businessProcess) {
        return new DisplayShopSkuStatusApproveInfo()
            .setSupplierId(supplierId).setShopSku(ssku).setTicket(ticket).setBusinessProcess(businessProcess);
    }

    protected void insertOffer(int supplierId, String shopSku, OfferAvailability availability, String comment) {
        insertOffer(supplierId, shopSku, availability, comment, 111);
    }

    protected void insertOffer(int supplierId, String shopSku, OfferAvailability availability) {
        insertOffer(supplierId, shopSku, availability, null, 111);
    }

    protected Supplier create1PSupplier(int id, String rsId) {
        return new Supplier().setId(id).setName("test").setSupplierType(REAL_SUPPLIER).setRealSupplierId(rsId);
    }

    protected Supplier create3pSupplier(int id) {
        return new Supplier().setId(id).setName("test").setSupplierType(THIRD_PARTY);
    }

    protected void insertOffer(int supplierId, String shopSku, OfferAvailability availability,
                               String comment, long mskuId) {
        var suppliers = deepmindSupplierRepository.findByIds(List.of(supplierId));
        Supplier supplier;
        if (suppliers.isEmpty()) {
            supplier = new Supplier().setId(supplierId).setName("test_supplier_" + supplierId)
                .setSupplierType(THIRD_PARTY);
            deepmindSupplierRepository.save(supplier);
        } else {
            supplier = suppliers.get(0);
        }
        var offer = new ServiceOfferReplica()
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
        serviceOfferReplicaRepository.save(offer);
        sskuStatusRepository.save(new SskuStatus()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setAvailability(availability)
            .setComment(comment)
        );
    }
}
