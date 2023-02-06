package ru.yandex.market.deepmind.common.services.tracker_strategy;

import java.util.List;

import org.junit.Before;
import org.mockito.Mockito;

import ru.yandex.market.deepmind.common.DeepmindBaseEnhancedTrackerApproverExecutorTestClass;
import ru.yandex.market.deepmind.common.openapi.ReplenishmentService;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryTeamRepository;
import ru.yandex.market.deepmind.common.repository.SeasonalMskuRepository;
import ru.yandex.market.deepmind.common.repository.msku.info.MskuInfoRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuHelperService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusServiceImpl;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusValidationServiceImpl;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichSpecialOrderExcelComposer;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.ExcelComposer;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.KeyMetaV2;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.MetaV2;
import ru.yandex.market.deepmind.common.services.yt.AssortmentResponsiblesLoader;
import ru.yandex.market.deepmind.common.services.yt.EnrichApproveToPendingLoader;
import ru.yandex.market.deepmind.common.services.yt.EnrichApproveToPendingLoaderMock;
import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverFacade;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingService;
import ru.yandex.market.replenishment.autoorder.openapi.client.api.SpecialOrderApiMock;

/**
 * Tests of {@link SpecialOrderStrategyV2}.
 */
public abstract class SpecialOrderStrategyBaseTestClass extends DeepmindBaseEnhancedTrackerApproverExecutorTestClass {
    protected SskuMskuStatusService sskuMskuStatusService;
    protected ReplenishmentService replenishmentService;
    protected AssortmentResponsiblesLoader assortmentResponsiblesLoader;
    protected SpecialOrderStrategyV2 strategySpy;
    protected TrackerApproverFacade<ServiceOfferKey, MetaV2, KeyMetaV2> facade;
    protected ExcelComposer fromUserExcelComposer;

    @Before
    public void setUp() {
        super.setUp();
        var sskuMskuHelperService = new SskuMskuHelperService(serviceOfferReplicaRepository, sskuStatusRepository,
            mskuStatusRepository);
        var sskuMskuStatusValidationService = new SskuMskuStatusValidationServiceImpl(mskuStockRepository,
            serviceOfferReplicaRepository, deepmindSupplierRepository, sskuMskuHelperService);
        sskuMskuStatusService = new SskuMskuStatusServiceImpl(sskuStatusRepository, mskuStatusRepository,
            sskuMskuStatusValidationService, sskuMskuHelperService, transactionTemplate);

        var approveWithACHelper = new ApproveWithAssortmentCommitteeHelper(session,
            economicMetricsRepository, transactionHelper);
        var approveWithACHelperSpy = Mockito.spy(approveWithACHelper);

        var excelComposer = new EnrichSpecialOrderExcelComposer(deepmindMskuRepository,
            deepmindSupplierRepository, Mockito.mock(GlobalVendorsCachingService.class),
            Mockito.mock(MasterDataHelperService.class), serviceOfferReplicaRepository, categoryCachingService,
            Mockito.mock(DeepmindCategoryManagerRepository.class), Mockito.mock(DeepmindCategoryTeamRepository.class),
            Mockito.mock(MskuInfoRepository.class), Mockito.mock(SeasonRepository.class),
            Mockito.mock(SeasonalMskuRepository.class), Mockito.mock(EnrichApproveToPendingLoader.class),
            offersConverter, sskuStatusRepository
        );

        var service = new ReplenishmentService(new SpecialOrderApiMock());
        replenishmentService = Mockito.spy(service);
        assortmentResponsiblesLoader = Mockito.mock(AssortmentResponsiblesLoader.class);
        strategySpy = Mockito.spy(new SpecialOrderStrategyV2(
            session,
            approveWithACHelperSpy,
            "TEST",
            excelComposer,
            transactionHelper,
            replenishmentService,
            "https://link.to/{demandId}",
            deepmindSupplierRepository,
            sskuMskuStatusService,
            sskuStatusRepository,
            mskuStatusRepository,
            mskuInfoRepository,
            assortSskuRepository,
            offerRepository,
            deepmindWarehouseRepository,
            offersConverter,
            namedParameterJdbcTemplate,
            assortmentResponsiblesLoader,
            deepmindRobotLogin,
            storageKeyValueService,
            "corefixTrackerField"
        ));
        factory.registerStrategy(strategySpy);
        facade = factory.getFacade(strategySpy.getType());

        MasterDataHelperService masterDataHelperService = Mockito.mock(MasterDataHelperService.class);
        Mockito.when(masterDataHelperService.findSskuMasterData(Mockito.any()))
            .thenReturn(List.of(
                new MasterData().setSupplierId(111).setShopSku("shop-sku-111").setMinShipment(1).setQuantumOfSupply(1),
                new MasterData().setSupplierId(222).setShopSku("shop-sku-222").setMinShipment(2).setQuantumOfSupply(2),
                new MasterData().setSupplierId(333).setShopSku("shop-sku-333").setMinShipment(3).setQuantumOfSupply(3),
                new MasterData().setSupplierId(444).setShopSku("shop-sku-444").setMinShipment(4).setQuantumOfSupply(4)
            ));


        var enrichApproveToPendingLoader = new EnrichApproveToPendingLoaderMock(getEnrichApproveToPendingYtInfo());

        fromUserExcelComposer = new FromUserExcelComposerMock(deepmindMskuRepository,
            deepmindSupplierRepository, Mockito.mock(GlobalVendorsCachingService.class),
            masterDataHelperService, serviceOfferReplicaRepository, categoryCachingService,
            Mockito.mock(DeepmindCategoryManagerRepository.class), Mockito.mock(DeepmindCategoryTeamRepository.class),
            Mockito.mock(MskuInfoRepository.class), Mockito.mock(SeasonRepository.class),
            Mockito.mock(SeasonalMskuRepository.class), enrichApproveToPendingLoader,
            offersConverter, sskuStatusRepository, true
        );
    }

}
