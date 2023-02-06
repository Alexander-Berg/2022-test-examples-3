package ru.yandex.market.deepmind.common.services.tracker_approver.excel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.common.DeepmindBaseEnhancedTrackerApproverExecutorTestClass;
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryManager;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryTeam;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuInfo;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SeasonPeriod;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SeasonalMsku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.openapi.ReplenishmentService;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.AssortSskuRepository;
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
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.category.DeepmindCategoryCachingService;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverterImpl;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusServiceImpl;
import ru.yandex.market.deepmind.common.services.tracker_strategy.ApproveWithAssortmentCommitteeHelper;
import ru.yandex.market.deepmind.common.services.tracker_strategy.ToPendingApproveStrategyV2;
import ru.yandex.market.deepmind.common.services.yt.AbstractLoader;
import ru.yandex.market.deepmind.common.services.yt.AssortmentResponsiblesLoader;
import ru.yandex.market.deepmind.common.services.yt.EnrichApproveToPendingLoaderMock;
import ru.yandex.market.deepmind.common.services.yt.pojo.EnrichApproveToPendingYtInfo;
import ru.yandex.market.deepmind.common.services.yt.pojo.EnrichApproveToPendingYtLoadRequest;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketState;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverRawData;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverTicketRawStatus;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverDataRepository;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverTicketRepository;
import ru.yandex.market.deepmind.tracker_approver.utils.JsonWrapper;
import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingService;
import ru.yandex.startrek.client.Session;

import static ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository.CATMAN;

public class EnrichApproveToPendingExcelComposerTest extends DeepmindBaseEnhancedTrackerApproverExecutorTestClass {

    private final Long mskuId = 922337203685L;
    private final Long seasonOneId = 1115L;
    private final Long seasonTwoId = 1116L;
    private final Long vendorId = 133L;
    private final String ticket = "TEST-1";

    @Autowired
    private TrackerApproverDataRepository trackerApproverDataRepository;
    @Autowired
    private TrackerApproverTicketRepository trackerApproverTicketRepository;
    @Autowired
    private DeepmindCategoryManagerRepository deepmindCategoryManagerRepository;
    @Autowired
    private DeepmindCategoryTeamRepository deepmindCategoryTeamRepository;
    @Resource
    private DeepmindWarehouseRepository deepmindWarehouseRepository;

    private AbstractLoader<EnrichApproveToPendingYtInfo, EnrichApproveToPendingYtLoadRequest>
        enrichApproveToPendingLoader;
    private EnrichApproveToPendingExcelComposer enrichApproveToPendingExcelComposer;

    private Msku msku;
    private Supplier supplier;
    private MskuInfo mskuInfo;
    private SeasonRepository.SeasonWithPeriods seasonOneWithPeriods;
    private SeasonRepository.SeasonWithPeriods seasonTwoWithPeriods;
    private Category category;
    private ToPendingApproveStrategyV2 strategySpy;

    @Before
    @SuppressWarnings("checkstyle:MethodLength")
    public void setup() {

        int supplierId = 111;
        String shopSku = "shop-sku-111";
        ServiceOfferKey shopSkuKey = new ServiceOfferKey(supplierId, shopSku);
        ServiceOfferReplica serviceOfferReplica =
            new ServiceOfferReplica().setSupplierId(supplierId).setShopSku(shopSku).setMskuId(mskuId);
        msku = new Msku()
            .setTitle("Test msku 1")
            .setCategoryId(12L)
            .setVendorId(vendorId)
            .setId(mskuId);

        mskuInfo = new MskuInfo().setMarketSkuId(mskuId).setInTargetAssortment(true);

        supplier = new Supplier().setSupplierType(SupplierType.FIRST_PARTY).setName("supplier name").setId(supplierId);

        MasterData masterData = new MasterData()
            .setSupplierId(shopSkuKey.getSupplierId())
            .setShopSku(shopSkuKey.getShopSku())
            .setMinShipment(1231)
            .setQuantumOfSupply(554);

        GlobalVendorsCachingService globalVendorsCachingService = Mockito.mock(GlobalVendorsCachingService.class);
        Mockito.when(globalVendorsCachingService.getVendor(Mockito.anyLong())).thenReturn(null);

        MasterDataHelperService masterDataHelperService = Mockito.mock(MasterDataHelperService.class);
        Mockito.when(masterDataHelperService.findSskuMasterData(Mockito.any()))
            .thenReturn(List.of(masterData));

        Season seasonOne = new Season().setId(seasonOneId);
        Season seasonTwo = new Season().setId(seasonTwoId);

        seasonOneWithPeriods = new SeasonRepository.SeasonWithPeriods(seasonOne,
            List.of(new SeasonPeriod().setFromMmDd("11-11").setToMmDd("12-12").setSeasonId(seasonOneId)));
        seasonTwoWithPeriods = new SeasonRepository.SeasonWithPeriods(seasonTwo,
            List.of(new SeasonPeriod().setFromMmDd("13-11").setToMmDd("14-12").setSeasonId(seasonTwoId)));

        SeasonRepository seasonRepository = Mockito.mock(SeasonRepository.class);
        Mockito.when(seasonRepository.findWithPeriods(Mockito.any()))
            .thenReturn(List.of(seasonOneWithPeriods, seasonTwoWithPeriods));

        SeasonalMskuRepository seasonalMskuRepository = Mockito.mock(SeasonalMskuRepository.class);
        Mockito.when(seasonalMskuRepository.findByMskuIds(Mockito.any()))
            .thenReturn(List.of(new SeasonalMsku().setMskuId(mskuId).setSeasonalId(seasonOneId),
                new SeasonalMsku().setMskuId(mskuId).setSeasonalId(seasonTwoId)));


        MskuInfoRepository mskuInfoRepository = Mockito.mock(MskuInfoRepository.class);
        Mockito.when(mskuInfoRepository.findByIdsMap(Mockito.any()))
            .thenReturn(Map.of(mskuId, mskuInfo));

        ServiceOfferReplicaRepository serviceOfferReplicaRepository =
            Mockito.mock(ServiceOfferReplicaRepository.class);
        Mockito.when(serviceOfferReplicaRepository.findOffersByKeys(Mockito.anyList()))
            .thenReturn(List.of(serviceOfferReplica));

        var deepmindMskuRepository = Mockito.mock(MskuRepository.class);
        Mockito.when(deepmindMskuRepository.findMap(Mockito.any())).thenReturn(Map.of(mskuId, msku));

        SupplierRepository supplierRepository = Mockito.mock(SupplierRepository.class);
        Mockito.when(supplierRepository.findByIdsMap(Mockito.any())).thenReturn(Map.of(supplierId, supplier));
        supplierRepository.save(new Supplier().setId(1).setName("1"));
        CategoryManager categoryManager = deepmindCategoryManagerRepository.save(
            new CategoryManager().setStaffLogin("catman111").setCategoryId(msku.getCategoryId()).setRole(CATMAN)
                .setFirstName("").setLastName("")
        );

        category = new Category().setName("test category");
        DeepmindCategoryCachingService deepmindCategoryCachingService =
            Mockito.mock(DeepmindCategoryCachingService.class);
        Mockito.when(deepmindCategoryCachingService.getCategoriesMap(Mockito.any()))
            .thenReturn(Map.of(
                msku.getCategoryId(),
                category));

        deepmindCategoryTeamRepository.save(
            new CategoryTeam()
                .setCategoryId(12L)
                .setCatteam("dreamteam"));
        SskuStatusRepository sskuStatusRepository = Mockito.mock(SskuStatusRepository.class);
        Mockito.doReturn(Map.of(shopSkuKey, new SskuStatus()
                .setSupplierId(supplierId)
                .setShopSku(shopSku)
                .setAvailability(OfferAvailability.ACTIVE)
                .setComment("comment")
                .setStatusFinishAt(null)
                .setModifiedByUser(false)))
            .when(sskuStatusRepository).findMap(List.of(shopSkuKey));

        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT_FOR_SPECIAL_ORDERS);

        enrichApproveToPendingLoader = new EnrichApproveToPendingLoaderMock(getExtendedEnrichApproveToPendingYtInfo());

        enrichApproveToPendingExcelComposer = new EnrichApproveToPendingExcelComposer(
            deepmindMskuRepository, supplierRepository, globalVendorsCachingService, masterDataHelperService,
            serviceOfferReplicaRepository, deepmindCategoryCachingService, deepmindCategoryManagerRepository,
            deepmindCategoryTeamRepository, mskuInfoRepository, seasonRepository, seasonalMskuRepository,
            enrichApproveToPendingLoader, offersConverter, sskuStatusRepository
        );

        strategySpy = Mockito.spy(new ToPendingApproveStrategyV2(
            Mockito.mock(Session.class),
            Mockito.mock(ApproveWithAssortmentCommitteeHelper.class),
            "TEST",
            Mockito.mock(EnrichApproveToPendingExcelComposer.class),
            Mockito.mock(TransactionHelper.class),
            Mockito.mock(ReplenishmentService.class),
            "link",
            Mockito.mock(SupplierRepository.class),
            Mockito.mock(SskuMskuStatusServiceImpl.class),
            Mockito.mock(SskuStatusRepository.class),
            Mockito.mock(MskuStatusRepository.class),
            Mockito.mock(MskuInfoRepository.class),
            Mockito.mock(AssortSskuRepository.class),
            Mockito.mock(ServiceOfferReplicaRepository.class),
            deepmindWarehouseRepository,
            Mockito.mock(OffersConverterImpl.class),
            namedParameterJdbcTemplate,
            Mockito.mock(AssortmentResponsiblesLoader.class),
            deepmindRobotLogin,
            storageKeyValueService,
            "corefixTrackerField"
        ));
    }

    private List<EnrichApproveToPendingYtInfo> getExtendedEnrichApproveToPendingYtInfo() {
        EnrichApproveToPendingYtInfo enrichApproveToPendingYtInfo = EnrichApproveToPendingYtInfo.builder()
            .hid(12L)
            .mskuId(mskuId)
            .seasonalFlag(false)
            .mskuType("msku type val")
            .mskuStatus("msku status val")
            .mskuBarcode("barcode")
            .minrefToday(0.1d)
            .minrefMed30Days(0.1d)
            .price3PToday(0.1d)
            .price3PMed30Days(0.1d)
            .lowerPriceDays(12L)
            .higherPriceDays(14L)
            .purchasePrice(2.1d)
            .off1PFby(12L)
            .off3PFby(12L)
            .off3PFbyPl(12L)
            .off3PFbs(12L)
            .off3PDbs(12L)
            .cntOffOnPriceband(0.1d)
            .stockFfOnPriceband(10)
            .fit1P(12)
            .fit1PDead(12)
            .fit3P(12)
            .fit3PDead(12)
            .daysOnStock3P(12L)
            .avgGmvMsku1P(0.1d)
            .avgCntMsku1P(1000.1d)
            .avgGmvMsku3Pff(2000.1d)
            .avgCntMsku3Pff(0.1d)
            .avgGmvMsku3POther(0.1d)
            .avgCntMsku3POther(0.1d)
            .avgGmvHid1P(0.1d)
            .avgCntHid1P(0.1d)
            .avgGmvHid3Pff(0.1d)
            .avgCntHid3Pff(0.1d)
            .avgGmvHid3POther(0.1d)
            .avgCntHid3POther(0.1d)
            .ue1P12Mnth(0.1d)
            .grossMarginPr1P12Mnth(0.1d)
            .ue3Pff12Mnth(0.1d)
            .etrPr3Pff12Mnth(0.1d)
            .ue3Pother12Mnth(0.1d)
            .etrPr3Pother12Mnth(0.1d)
            .ue1PHid12Mnth(0.1d)
            .grossMarginPr1PHid12Mnth(0.1d)
            .ue3PFfHid12Mnth(0.1d)
            .etrPr3PFfHid12Mnth(0.1d)
            .ue3POtherHid12Mnth(0.1d)
            .etrPr3POtherHid12Mnth(0.1d)
            .avgFmHid(0.1d)
            .deliveryDsbsDays(0.1d)
            .deliveryFfDays(0.1d)
            .deliveryOtherDays(0.1d)
            .inOffWoFF30Days(10L)
            .gmvTotal60Days(620.62)
            .cntTotal60Days(62)
            .sofDeadstockSince("12-09-2020")
            .tomDeadstockSince("13-09-2020")
            .sofKgtDeadstockSince("06-08-20202")
            .reg1pDeadstockSince("13-08-2021")
            .reg3pDeadstockSince("08-10-2021")
            .sofDeadstockSales(0)
            .tomDeadstockSales(1)
            .sofKgtDeadstockSales(1)
            .itemPriceBeforePromoAll(256.01)
            .itemPriceBeforePromoFf(123.08)
            .minrefUrl("url//minref")
            .price3pFfToday(20.04)
            .price3pFfMed30days(60.04)
            .inOffers31days(12L)
            .inOffers3pFf30days(1234560L)
            .ue3pFfHid3mnth(45.9)
            .ue3pHidOther3mnth(46.1)
            .etrPr3pFfHid3mnth(47.4)
            .etrPr3pOthersHid3mnth(48.7)
            .stockWithTransit(9999L)
            .ueCalculated(100.01)
            .build();
        return List.of(enrichApproveToPendingYtInfo);
    }

    @Test
    public void testCorrectExcelFile() {
        String type = "test";
        trackerApproverTicketRepository.save(
            new TrackerApproverTicketRawStatus().setTicket(ticket).setState(TicketState.NEW).setType(type));

        var sskuInRepo = List.of(
            new ServiceOfferKey(111, "shop-sku-111"));

        var trackerApproverData = sskuInRepo.stream()
            .map(key -> new TrackerApproverRawData()
                .setTicket(ticket)
                .setKey(JsonWrapper.fromObject(key))
                .setType(type)
            ).collect(Collectors.toList());

        trackerApproverDataRepository.save(trackerApproverData);

        ExcelFile actual = enrichApproveToPendingExcelComposer.processKeys(sskuInRepo,
            strategySpy.getDefaultSpecialOrderData(sskuInRepo));

        ExcelFile expected = getExcelFrom("excel_files/testCorrectExcelFile.xlsx");

        Assertions.assertThat(actual).isEqualTo(expected);
    }
}
