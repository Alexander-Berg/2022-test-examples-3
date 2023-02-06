package ru.yandex.market.mboc.common.masterdata.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.mbo.lightmapper.JsonMapper;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuGoldenParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmMskuQueueInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueuePriorities;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.SskuToRefreshInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.CategoryRsl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.MskuRsl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.Rsl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.RslThreshold;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.SupplierRsl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.SskuPartnerVerdictResult;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.SskuVerdictResult;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.VerdictFeature;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.VerdictResult;
import ru.yandex.market.mbo.mdm.common.masterdata.model.warehouse.MdmWarehouse;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.FromIrisItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendReferenceItemQRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendToDatacampQRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl.CategoryRslRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl.MskuRslRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl.SupplierRslRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.verdict.SskuGoldenVerdictRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.verdict.SskuPartnerVerdictRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.VerdictGeneratorHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.warehouse.MdmWarehouseService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionsValidator;
import ru.yandex.market.mbo.mdm.common.rsl.RslType;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mbo.mdm.common.util.TimestampUtil;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.model.cutoff.OfferCutoff;
import ru.yandex.market.mboc.common.masterdata.model.cutoff.OfferCutoffInfo;
import ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.repository.cutoff.OfferCutoffFilter;
import ru.yandex.market.mboc.common.masterdata.services.cutoff.OfferCutoffService;
import ru.yandex.market.mboc.common.masterdata.services.cutoff.OfferCutoffTypeProvider;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

@SuppressWarnings("checkstyle:magicNumber")
public class SskuToRefreshProcessingServiceReferenceItemTest extends SskuToRefreshProcessingServiceBaseTest {
    private static final int SEED = 555;
    private static final ShopSkuKey BUSINESS_KEY = new ShopSkuKey(1000, "sku");
    private static final ShopSkuKey BUSINESS_KEY_2 = new ShopSkuKey(100, "sku");
    private static final ShopSkuKey STAGE2_KEY = new ShopSkuKey(2, "sku");

    private static final ShopSkuKey STAGE3_KEY1 = new ShopSkuKey(31, "sku");
    private static final ShopSkuKey STAGE3_KEY2 = new ShopSkuKey(32, "sku");
    private static final ShopSkuKey WHITE_IN_BIZ_KEY = new ShopSkuKey(77, "sku");
    private static final ShopSkuKey WHITE_LONELY_KEY = new ShopSkuKey(88, "sku");
    private static final ShopSkuKey UNKNOWN_KEY = new ShopSkuKey(99, "sku");

    // MARKETMDM-43: Change back to 19 after fix MARKETMDM-154.
    private static final int LOWER_OLDER_DELIVERY_TIME = 20;
    // MARKETMDM-43: Change back to 3 after fix MARKETMDM-154.
    private static final int LOWER_OLDER_BOX_COUNT = 4;

    @Autowired
    private FromIrisItemRepository fromIrisItemRepository;
    @Autowired
    private MdmWarehouseService mdmWarehouseService;
    @Autowired
    private OfferCutoffService cutoffService;
    @Autowired
    private SskuGoldenVerdictRepository sskuGoldenVerdictRepository;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private SskuGoldenParamUtil sskuGoldenParamUtil;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private CategoryParamValueRepository categoryParamValueRepository;
    @Autowired
    private SendReferenceItemQRepository sendReferenceItemQRepository;
    @Autowired
    private MskuToRefreshRepository mskuToRefreshRepository;
    @Autowired
    private SendToDatacampQRepository sendToDatacampQRepository;
    @Autowired
    private SilverSskuRepository silverSskuRepository;
    @Autowired
    private MskuRslRepository mskuRslRepository;
    @Autowired
    private CategoryRslRepository categoryRslRepository;
    @Autowired
    private SupplierRslRepository supplierRslRepository;
    @Autowired
    private MskuRepository mskuRepository;
    @Autowired
    private MasterDataRepository masterDataRepository;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;
    @Autowired
    private SskuPartnerVerdictRepository sskuPartnerVerdictRepository;
    @Autowired
    private MdmSupplierCachingService mdmSupplierCachingService;

    private final EnhancedRandom random = TestDataUtils.defaultRandom(SEED);

    @Before
    public void before() {
        mdmWarehouseService.addOrUpdateAll(List.of(
            new MdmWarehouse().setId(145L).setLmsType(PartnerType.FULFILLMENT),
            new MdmWarehouse().setId(48046L).setLmsType(PartnerType.DROPSHIP)
        ));

        sskuExistenceRepository.markExistence(List.of(STAGE2_KEY, STAGE3_KEY1, STAGE3_KEY2), true);
    }

    @Test
    public void testStage3GroupHandled() {
        prepareBusinessGroup(false);
        long outdatedTs = 1;
        long slightlyOutdatedTs = 2;
        long quiteRecentTs = 3;
        long mostRecentTs = 4;
        long almostFuture = 5;

        var businessShippingUnit = // Предположим, это ВГХ по бизнесу от ЕОХ или других не ИРИСовых источников в будущем
            ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0, 10.0, 1.0, null, null, outdatedTs);
        var stage2ShippingUnit =
            ItemWrapperTestUtil.generateShippingUnit(70.0, 70.0, 70.0, 13.0, null, null, mostRecentTs);
        var stage3ShippingUnit1 =
            ItemWrapperTestUtil.generateShippingUnit(20.0, 20.0, 20.0, 2.0, null, null, slightlyOutdatedTs);
        var stage3ShippingUnit2 =
            ItemWrapperTestUtil.generateShippingUnit(30.0, 30.0, 30.0, 3.0, null, null, quiteRecentTs);
        var whiteShippingUnit =
            ItemWrapperTestUtil.generateShippingUnit(40.0, 40.0, 40.0, 4.0,
                null, null, almostFuture);
        var unknownShippingUnit =
            ItemWrapperTestUtil.generateShippingUnit(50.0, 50.0, 50.0, 5.0,
                null, null, almostFuture);
        var whiteLonelyShippingUnit =
            ItemWrapperTestUtil.generateShippingUnit(60.0, 60.0, 60.0, 6.0,
                null, null, almostFuture);


        var businessItem = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            BUSINESS_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "172", businessShippingUnit));
        var stage2Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE2_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "171", stage2ShippingUnit));
        var stage3Item1 = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE3_KEY1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "66", stage3ShippingUnit1));
        var stage3Item2 = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE3_KEY2, MdmIrisPayload.MasterDataSource.WAREHOUSE, "27", stage3ShippingUnit2));
        var whiteItem = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            WHITE_IN_BIZ_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "28", whiteShippingUnit));
        var unknownItem = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            UNKNOWN_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "29", unknownShippingUnit));
        var whiteLonelyItem = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            WHITE_LONELY_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "30", whiteLonelyShippingUnit));
        fromIrisItemRepository.insertBatch(businessItem, stage2Item, stage3Item1, stage3Item2,
            whiteItem, unknownItem, whiteLonelyItem);

        sskuToRefreshProcessingService.processShopSkuKeys(
            List.of(BUSINESS_KEY, STAGE2_KEY, STAGE3_KEY1, STAGE3_KEY2, WHITE_IN_BIZ_KEY, UNKNOWN_KEY, WHITE_LONELY_KEY)
        );

        Map<ShopSkuKey, ReferenceItemWrapper> goldenItems =
            referenceItemRepository.findByIds(List.of(BUSINESS_KEY, STAGE2_KEY, STAGE3_KEY1, STAGE3_KEY2,
                    WHITE_IN_BIZ_KEY, UNKNOWN_KEY, WHITE_LONELY_KEY))
                .stream()
                .collect(Collectors.toMap(ItemWrapper::getShopSkuKey, Function.identity()));
        Assertions.assertThat(goldenItems).doesNotContainKey(BUSINESS_KEY);
        Assertions.assertThat(goldenItems).doesNotContainKey(WHITE_IN_BIZ_KEY);
        Assertions.assertThat(goldenItems).doesNotContainKey(UNKNOWN_KEY);
        Assertions.assertThat(goldenItems).doesNotContainKey(WHITE_LONELY_KEY);
        Assertions.assertThat(goldenItems.get(STAGE2_KEY).getCombinedItemShippingUnit())
            .isEqualTo(stage2ShippingUnit.build());
        // Входящие в бизнес-группу итемы должны иметь общие бизнес-ВГХ. В нашем примере это самые свежие ВГХ, взятые
        // из последнего юнита.
        Assertions.assertThat(goldenItems.get(STAGE3_KEY1).getCombinedItemShippingUnit())
            .isEqualTo(stage3ShippingUnit2.build());
        Assertions.assertThat(goldenItems.get(STAGE3_KEY2).getCombinedItemShippingUnit())
            .isEqualTo(stage3ShippingUnit2.build());
    }

    @Test
    public void testThinGroupByBizKeyHasHandledVerdicts() {
        prepareBusinessGroup(false);

        var stage3ShippingUnit1 =
            ItemWrapperTestUtil.generateShippingUnit(20.0, 20.0, 20.0, 2.0, null, null);

        var stage3Item1 = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE3_KEY1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "66", stage3ShippingUnit1));
        stage3Item1.setProcessed(true); // avoid triggering
        fromIrisItemRepository.insertBatch(stage3Item1);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(STAGE3_KEY1));

        var expectedVerdictByBusinessKey = new SskuVerdictResult();
        expectedVerdictByBusinessKey.setKey(BUSINESS_KEY);
        expectedVerdictByBusinessKey.setValid(false);
        expectedVerdictByBusinessKey.setSingleVerdictResults(Map.of(
            VerdictFeature.UNSPECIFIED,
            VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.UNSPECIFIED,
                List.of(MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.MANUFACTURER_COUNTRY))))
        );

        // verdict by biz key was created from processNewIrisItems
        var expectedVerdictByService1 = new SskuVerdictResult();
        expectedVerdictByService1.setKey(STAGE3_KEY1);
        expectedVerdictByService1.setValid(true);
        expectedVerdictByService1.setSingleVerdictResults(Map.of(
            VerdictFeature.UNSPECIFIED,
            VerdictGeneratorHelper.createOkVerdict(VerdictFeature.UNSPECIFIED))
        );

        var expectedVerdictByService2 = new SskuVerdictResult();
        expectedVerdictByService2.setKey(STAGE3_KEY2);
        expectedVerdictByService2.setValid(true);
        expectedVerdictByService2.setSingleVerdictResults(Map.of(
            VerdictFeature.UNSPECIFIED,
            VerdictGeneratorHelper.createOkVerdict(VerdictFeature.UNSPECIFIED))
        );

        Assertions.assertThat(sskuGoldenVerdictRepository
                .findByIds(List.of(BUSINESS_KEY, STAGE3_KEY1, STAGE3_KEY2)
                ))
            .usingElementComparatorIgnoringFields("updatedTs", "mdmVersionId", "mdmVersionTs", "contentVersionId",
                "mdmId")
            .containsExactlyInAnyOrder(
                expectedVerdictByBusinessKey,
                expectedVerdictByService1,
                expectedVerdictByService2);
    }

    @Test
    public void whenStage3GroupRecomputedShouldAffectEvenUnchangedItems() {
        prepareBusinessGroup(true);
        long outdatedTs = 1;
        long slightlyOutdatedTs = 2;
        long quiteRecentTs = 3;
        long mostRecentTs = 4;
        long almostFuture = 5;

        var businessShippingUnit = // Предположим, это ВГХ по бизнесу от ЕОХ или других не ИРИСовых источников в будущем
            ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0, 10.0, 1.0, null, null, outdatedTs);
        var existingShippingUnit =
            ItemWrapperTestUtil.generateShippingUnit(70.0, 70.0, 70.0, 13.0, null, null, quiteRecentTs);
        var stage3ShippingUnit1 =
            ItemWrapperTestUtil.generateShippingUnit(20.0, 20.0, 20.0, 2.0, null, null, slightlyOutdatedTs);
        var stage3ShippingUnit2 =
            ItemWrapperTestUtil.generateShippingUnit(30.0, 30.0, 30.0, 3.0, null, null, mostRecentTs);
        var existingWhiteShippingUnit =
            ItemWrapperTestUtil.generateShippingUnit(40.0, 40.0, 40.0, 4.0,
                null, null, almostFuture);

        var businessItem = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            BUSINESS_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "172", businessShippingUnit));
        var existingItem = new ReferenceItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE2_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "171", existingShippingUnit));
        var stage3Item1 = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE3_KEY1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "66", stage3ShippingUnit1));
        var stage3Item2 = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE3_KEY2, MdmIrisPayload.MasterDataSource.WAREHOUSE, "27", stage3ShippingUnit2));
        var existingWhiteItem = new ReferenceItemWrapper(ItemWrapperTestUtil.createItem(
            WHITE_IN_BIZ_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "28", existingWhiteShippingUnit));
        fromIrisItemRepository.insertBatch(businessItem, stage3Item1, stage3Item2);
        referenceItemRepository.insertBatch(existingItem, existingWhiteItem);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(BUSINESS_KEY, STAGE3_KEY1, STAGE3_KEY2));

        Map<ShopSkuKey, ReferenceItemWrapper> goldenItems =
            referenceItemRepository.findByIds(List.of(BUSINESS_KEY, STAGE2_KEY, STAGE3_KEY1, STAGE3_KEY2,
                    WHITE_IN_BIZ_KEY))
                .stream()
                .collect(Collectors.toMap(ItemWrapper::getShopSkuKey, Function.identity()));
        Assertions.assertThat(goldenItems).doesNotContainKey(BUSINESS_KEY);
        Assertions.assertThat(goldenItems.get(STAGE2_KEY).getCombinedItemShippingUnit())
            .isEqualTo(stage3ShippingUnit2.build());
        Assertions.assertThat(goldenItems.get(STAGE3_KEY1).getCombinedItemShippingUnit())
            .isEqualTo(stage3ShippingUnit2.build());
        Assertions.assertThat(goldenItems.get(STAGE3_KEY2).getCombinedItemShippingUnit())
            .isEqualTo(stage3ShippingUnit2.build());
        //white not changed
        Assertions.assertThat(goldenItems.get(WHITE_IN_BIZ_KEY).getCombinedItemShippingUnit())
            .isEqualTo(existingWhiteShippingUnit.build());
    }

    @Test
    public void testUpdateVersionIdForCertainSourceTypes() {
        ShopSkuKey key = new ShopSkuKey(123, "42");
        prepareSuppliersBySsku(List.of(key), MdmSupplierType.THIRD_PARTY);
        var itemShippingUnit = ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0, 10.0, 1.0, null, null);

        List<MdmIrisPayload.MasterDataSource> expectedVersionedSources = List.of(
            MdmIrisPayload.MasterDataSource.WAREHOUSE,
            MdmIrisPayload.MasterDataSource.SUPPLIER,
            MdmIrisPayload.MasterDataSource.MDM,
            MdmIrisPayload.MasterDataSource.MEASUREMENT
        );

        for (MdmIrisPayload.MasterDataSource masterDataSource : MdmIrisPayload.MasterDataSource.values()) {
            if (masterDataSource == MdmIrisPayload.MasterDataSource.UNRECOGNIZED) {
                continue;
            }
            fromIrisItemRepository.deleteAll();

            MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(key, masterDataSource, "421", itemShippingUnit);
            fromIrisItemRepository.insert(new FromIrisItemWrapper(item));

            sskuToRefreshProcessingService.processShopSkuKeys(List.of(key));

            List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(List.of(key));

            var information = referenceItems.get(0).getItem().getInformation(0);
            Assertions.assertThat(information.getVersionId() > 0).isEqualTo(
                expectedVersionedSources.contains(masterDataSource));
            Assertions.assertThat(information.getItemShippingUnit()).isEqualTo(itemShippingUnit.build());
        }
    }

    @Test
    public void whenNoRslRulesRemoveOldRsl() {
        prepareBusinessGroup(false);
        long outdatedTs = 1;
        long slightlyOutdatedTs = 2;
        long quiteRecentTs = 3;
        long mostRecentTs = 4;

        var businessShippingUnit = // Предположим, это ВГХ по бизнесу от ЕОХ или других не ИРИСовых источников в будущем
            ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0, 10.0, 1.0, null, null, outdatedTs);
        var stage2ShippingUnit =
            ItemWrapperTestUtil.generateShippingUnit(70.0, 70.0, 70.0, 13.0, null, null, mostRecentTs);
        var stage3ShippingUnit1 =
            ItemWrapperTestUtil.generateShippingUnit(20.0, 20.0, 20.0, 2.0, null, null, slightlyOutdatedTs);
        var stage3ShippingUnit2 =
            ItemWrapperTestUtil.generateShippingUnit(30.0, 30.0, 30.0, 3.0, null, null, quiteRecentTs);

        MdmIrisPayload.Item bizItemPayload = createItem(
            BUSINESS_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "172",
            null, null, null, null, businessShippingUnit);
        var businessItem = new FromIrisItemWrapper(BUSINESS_KEY);
        businessItem.setReferenceItem(bizItemPayload);

        MdmIrisPayload.Item stage2ItemPayload = createItem(
            STAGE2_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "171",
            1, 10, 2, 10, stage2ShippingUnit);
        var stage2Item = new FromIrisItemWrapper(STAGE2_KEY);
        stage2Item.setReferenceItem(stage2ItemPayload);

        MdmIrisPayload.Item stage3Key1Payload = createItem(
            STAGE3_KEY1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "66",
            1, 10, 2, 10, stage3ShippingUnit1);
        var stage3Item1 = new FromIrisItemWrapper(STAGE3_KEY1);
        stage3Item1.setReferenceItem(stage3Key1Payload);

        MdmIrisPayload.Item stage3Key2Payload = createItem(
            STAGE3_KEY2, MdmIrisPayload.MasterDataSource.WAREHOUSE, "27",
            1, 10, 2, 10, stage3ShippingUnit2);
        var stage3Item2 = new FromIrisItemWrapper(STAGE3_KEY2);
        stage3Item2.setReferenceItem(stage3Key2Payload);

        ReferenceItemWrapper wr2 = new ReferenceItemWrapper(STAGE2_KEY);
        wr2.setReferenceItem(stage2ItemPayload);

        ReferenceItemWrapper wr31 = new ReferenceItemWrapper(STAGE3_KEY1);
        wr31.setReferenceItem(stage3Key1Payload);

        referenceItemRepository.insertBatch(wr2, wr31);

        fromIrisItemRepository.insertBatch(businessItem, stage2Item, stage3Item1, stage3Item2);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(BUSINESS_KEY, STAGE2_KEY, STAGE3_KEY1, STAGE3_KEY2));

        Map<ShopSkuKey, ReferenceItemWrapper> goldenItems =
            referenceItemRepository.findByIds(List.of(BUSINESS_KEY, STAGE2_KEY, STAGE3_KEY1, STAGE3_KEY2))
                .stream()
                .collect(Collectors.toMap(ItemWrapper::getShopSkuKey, Function.identity()));
        Assertions.assertThat(goldenItems).doesNotContainKey(BUSINESS_KEY);
        Assertions.assertThat(goldenItems.get(STAGE2_KEY).getCombinedItemShippingUnit())
            .isEqualTo(stage2ShippingUnit.build());
        // Входящие в бизнес-группу итемы должны иметь общие бизнес-ВГХ. В нашем примере это самые свежие ВГХ, взятые
        // из последнего юнита.
        Assertions.assertThat(goldenItems.get(STAGE3_KEY1).getCombinedItemShippingUnit())
            .isEqualTo(stage3ShippingUnit2.build());
        Assertions.assertThat(goldenItems.get(STAGE3_KEY2).getCombinedItemShippingUnit())
            .isEqualTo(stage3ShippingUnit2.build());

        // Так как нет никаких правил ОСГ, то все старые никому не нужные никчемные ОСГ исчезли
        Assertions.assertThat(referenceItemRepository.findAll())
            .map(ItemWrapper::getItem)
            .flatMap(MdmIrisPayload.Item::getInformationList)
            .allMatch(referenceInformation -> referenceInformation.getMinOutboundLifetimePercentageCount() == 0)
            .allMatch(referenceInformation -> referenceInformation.getMinOutboundLifetimeDayCount() == 0)
            .allMatch(referenceInformation -> referenceInformation.getMinInboundLifetimePercentageCount() == 0)
            .allMatch(referenceInformation -> referenceInformation.getMinInboundLifetimeDayCount() == 0);
    }

    @Test
    public void testCalculateRslForGoldenItem() {
        //prepare Rsl
        Integer categoryId = 12345;
        Long mskuId = 111L;

        prepareRsls(categoryId, mskuId);
        CommonMsku commonMsku = prepareTestMsku(categoryId, mskuId);

        mskuRepository.insertOrUpdateMsku(commonMsku);

        mappingsCacheRepository.insert(
            new MappingCacheDao().setCategoryId(categoryId)
                .setShopSkuKey(BUSINESS_KEY)
                .setMskuId(mskuId)
        );

        prepareBusinessGroup(false);
        long outdatedTs = 1;
        long slightlyOutdatedTs = 2;
        long quiteRecentTs = 3;
        long mostRecentTs = 4;

        var businessShippingUnit = // Предположим, это ВГХ по бизнесу от ЕОХ или других не ИРИСовых источников в будущем
            ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0, 10.0, 1.0, null, null, outdatedTs);
        var stage2ShippingUnit =
            ItemWrapperTestUtil.generateShippingUnit(70.0, 70.0, 70.0, 13.0, null, null, mostRecentTs);
        var stage3ShippingUnit1 =
            ItemWrapperTestUtil.generateShippingUnit(20.0, 20.0, 20.0, 2.0, null, null, slightlyOutdatedTs);
        var stage3ShippingUnit2 =
            ItemWrapperTestUtil.generateShippingUnit(30.0, 30.0, 30.0, 3.0, null, null, quiteRecentTs);

        //Обратный поток RSL от ирис, который мы должны проигнорить
        MdmIrisPayload.Item bizItemPayload = createItem(
            BUSINESS_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "172",
            null, null, null, null, businessShippingUnit);
        var businessItem = new FromIrisItemWrapper(BUSINESS_KEY);
        businessItem.setReferenceItem(bizItemPayload);

        MdmIrisPayload.Item stage2ItemPayload = createItem(
            STAGE2_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "171",
            1, 10, 2, 10, stage2ShippingUnit);
        var stage2Item = new FromIrisItemWrapper(STAGE2_KEY);
        stage2Item.setReferenceItem(stage2ItemPayload);

        MdmIrisPayload.Item stage3Key1Payload = createItem(
            STAGE3_KEY1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "66",
            1, 10, 2, 10, stage3ShippingUnit1);
        var stage3Item1 = new FromIrisItemWrapper(STAGE3_KEY1);
        stage3Item1.setReferenceItem(stage3Key1Payload);

        MdmIrisPayload.Item stage3Key2Payload = createItem(
            STAGE3_KEY2, MdmIrisPayload.MasterDataSource.WAREHOUSE, "27",
            1, 10, 2, 10, stage3ShippingUnit2);
        var stage3Item2 = new FromIrisItemWrapper(STAGE3_KEY2);
        stage3Item2.setReferenceItem(stage3Key2Payload);

        ReferenceItemWrapper wr2 = new ReferenceItemWrapper(STAGE2_KEY);
        wr2.setReferenceItem(stage2ItemPayload);

        ReferenceItemWrapper wr31 = new ReferenceItemWrapper(STAGE3_KEY1);
        wr31.setReferenceItem(stage3Key1Payload);

        referenceItemRepository.insertBatch(wr2, wr31);

        fromIrisItemRepository.insertBatch(businessItem, stage2Item, stage3Item1, stage3Item2);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(BUSINESS_KEY, STAGE2_KEY, STAGE3_KEY1, STAGE3_KEY2));

        Map<ShopSkuKey, ReferenceItemWrapper> goldenItems =
            referenceItemRepository.findByIds(List.of(BUSINESS_KEY, STAGE2_KEY, STAGE3_KEY1, STAGE3_KEY2))
                .stream()
                .collect(Collectors.toMap(ItemWrapper::getShopSkuKey, Function.identity()));
        Assertions.assertThat(goldenItems).doesNotContainKey(BUSINESS_KEY);
        Assertions.assertThat(goldenItems.get(STAGE2_KEY).getCombinedItemShippingUnit())
            .isEqualTo(stage2ShippingUnit.build());
        // Входящие в бизнес-группу итемы должны иметь общие бизнес-ВГХ. В нашем примере это самые свежие ВГХ, взятые
        // из последнего юнита.
        Assertions.assertThat(goldenItems.get(STAGE3_KEY1).getCombinedItemShippingUnit())
            .isEqualTo(stage3ShippingUnit2.build());
        Assertions.assertThat(goldenItems.get(STAGE3_KEY2).getCombinedItemShippingUnit())
            .isEqualTo(stage3ShippingUnit2.build());

        // У STAGE2_KEY нет маппинга на msku => нет expir_date => нет новых ОСГ, а все старые удалились
        Assertions.assertThat(goldenItems.get(STAGE2_KEY).getItem().getInformationList())
            .allMatch(referenceInformation -> referenceInformation.getMinOutboundLifetimePercentageCount() == 0)
            .allMatch(referenceInformation -> referenceInformation.getMinOutboundLifetimeDayCount() == 0)
            .allMatch(referenceInformation -> referenceInformation.getMinInboundLifetimePercentageCount() == 0)
            .allMatch(referenceInformation -> referenceInformation.getMinInboundLifetimeDayCount() == 0);

        Assertions.assertThat(goldenItems.get(STAGE3_KEY2).getItem().getInformationCount()).isEqualTo(2);
        Assertions.assertThat(goldenItems.get(STAGE3_KEY1).getItem().getInformation(1)
            .getMinInboundLifetimeDay(0).getValue()).isEqualTo(5);
        Assertions.assertThat(goldenItems.get(STAGE3_KEY1).getItem().getInformation(1)
            .getMinOutboundLifetimeDay(0).getValue()).isEqualTo(40);
        Assertions.assertThat(goldenItems.get(STAGE3_KEY1).getItem().getInformation(1).getMinInboundLifetimeDayList())
            .isNotEmpty();

        Assertions.assertThat(goldenItems.get(STAGE3_KEY2).getItem().getInformationCount()).isEqualTo(2);
        Assertions.assertThat(goldenItems.get(STAGE3_KEY2).getItem().getInformation(1)
            .getMinInboundLifetimeDay(0).getValue()).isEqualTo(5);
        Assertions.assertThat(goldenItems.get(STAGE3_KEY2).getItem().getInformation(1)
            .getMinOutboundLifetimeDay(0).getValue()).isEqualTo(40);

    }

    @Test
    public void testHideWhenNoDimensions() {
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS, true);
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS_DRY_RUN, false);
        storageKeyValueService.invalidateCache();

        ShopSkuKey k1 = new ShopSkuKey(1, "123");
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            null, null, null, null, null, null);
        FromIrisItemWrapper item1 = new FromIrisItemWrapper(
            ItemWrapperTestUtil.createItem(k1,
                MdmIrisPayload.MasterDataSource.SUPPLIER, "123", itemShippingUnit)
        );

        ShopSkuKey k2 = new ShopSkuKey(1, "456");
        FromIrisItemWrapper item2 = new FromIrisItemWrapper(
            ItemWrapperTestUtil.createItem(k2, MdmIrisPayload.MasterDataSource.SUPPLIER, "123")
        );

        fromIrisItemRepository.insertBatch(List.of(item1, item2));
        mappingsCacheRepository.insertOrUpdateAll(List.of(
            generateMappingCache(k1, random.nextLong(), random.nextInt()),
            generateMappingCache(k2, random.nextLong(), random.nextInt())));
        prepareSuppliersBySsku(List.of(k1, k2), MdmSupplierType.THIRD_PARTY);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(k1, k2));

        Assertions.assertThat(cutoffService.findCutoffs(new OfferCutoffFilter())).containsExactlyInAnyOrder(
            generateCutoff(k1, OfferCutoffTypeProvider.missDimensionsError(), OfferCutoff.CutoffState.OPEN),
            generateCutoff(k2, OfferCutoffTypeProvider.missDimensionsError(), OfferCutoff.CutoffState.OPEN)
        );
    }

    @Test
    public void testHideWhenWrongDimensions() {
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS, true);
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS_DRY_RUN, false);
        storageKeyValueService.invalidateCache();

        ShopSkuKey k = new ShopSkuKey(1, "123");
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            10.0, 10.0, 10000.0, 1.0, null, null);
        FromIrisItemWrapper item = new FromIrisItemWrapper(
            ItemWrapperTestUtil.createItem(k,
                MdmIrisPayload.MasterDataSource.SUPPLIER, "123", itemShippingUnit)
        );

        fromIrisItemRepository.insert(item);
        mappingsCacheRepository.insert(generateMappingCache(k, random.nextLong(), random.nextInt()));
        prepareSuppliersBySsku(List.of(k), MdmSupplierType.THIRD_PARTY);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(k));

        Assertions.assertThat(cutoffService.findCutoffs(new OfferCutoffFilter())).containsExactlyInAnyOrder(
            generateCutoff(k, generateDefaultInvalidDimensionsCutoffInfo(), OfferCutoff.CutoffState.OPEN)
        );
    }

    @Test
    public void testHideWhenPartialDimensions() {
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS, true);
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS_DRY_RUN, false);
        storageKeyValueService.invalidateCache();

        ShopSkuKey k = new ShopSkuKey(1, "123");
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            10.0, 10.0, null, 1.0, null, null);
        FromIrisItemWrapper item = new FromIrisItemWrapper(
            ItemWrapperTestUtil.createItem(k,
                MdmIrisPayload.MasterDataSource.SUPPLIER, "123", itemShippingUnit)
        );

        fromIrisItemRepository.insert(item);
        mappingsCacheRepository.insert(generateMappingCache(k, random.nextLong(), random.nextInt()));
        prepareSuppliersBySsku(List.of(k), MdmSupplierType.THIRD_PARTY);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(k));

        Assertions.assertThat(cutoffService.findCutoffs(new OfferCutoffFilter())).containsExactlyInAnyOrder(
            generateCutoff(k, OfferCutoffTypeProvider.missDimensionsError(), OfferCutoff.CutoffState.OPEN)
        );
    }

    @Test
    public void testWhenHasReferenceItemThenDoNothing() {
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS, true);
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS_DRY_RUN, false);
        storageKeyValueService.invalidateCache();

        ShopSkuKey k = new ShopSkuKey(1, "123");
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            10.0, 10.0, 10.0, 1.0, null, null);
        FromIrisItemWrapper item = new FromIrisItemWrapper(
            ItemWrapperTestUtil.createItem(k,
                MdmIrisPayload.MasterDataSource.SUPPLIER, "123", itemShippingUnit)
        );

        var existingItem = new ReferenceItemWrapper(ItemWrapperTestUtil.createItem(
            k, MdmIrisPayload.MasterDataSource.SUPPLIER, "123", itemShippingUnit));
        referenceItemRepository.insert(existingItem);

        fromIrisItemRepository.insert(item);
        mappingsCacheRepository.insert(generateMappingCache(k, random.nextLong(), random.nextInt()));

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(k));

        Assertions.assertThat(cutoffService.findCutoffs(new OfferCutoffFilter())).isEmpty();
    }

    @Test
    public void testShowMissWhenValidDimensions() {
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS, true);
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS_DRY_RUN, false);
        storageKeyValueService.invalidateCache();

        ShopSkuKey k = new ShopSkuKey(1, "123");
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            10.0, 10.0, 10.0, 1.0, null, null);
        FromIrisItemWrapper item = new FromIrisItemWrapper(
            ItemWrapperTestUtil.createItem(k,
                MdmIrisPayload.MasterDataSource.SUPPLIER, "123", itemShippingUnit)
        );
        cutoffService.openCutoff(generateCutoff(k, OfferCutoffTypeProvider.missDimensionsError(),
            OfferCutoff.CutoffState.OPEN));
        fromIrisItemRepository.insert(item);
        mappingsCacheRepository.insert(generateMappingCache(k, random.nextLong(), random.nextInt()));
        prepareSuppliersBySsku(List.of(k), MdmSupplierType.THIRD_PARTY);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(k));

        Assertions.assertThat(cutoffService.findCutoffs(new OfferCutoffFilter())).containsExactlyInAnyOrder(
            generateCutoff(k, OfferCutoffTypeProvider.missDimensionsError(), OfferCutoff.CutoffState.CLOSED)
        );
    }

    @Test
    public void testShowWrongWhenValidDimensions() {
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS, true);
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS_DRY_RUN, false);
        storageKeyValueService.invalidateCache();

        ShopSkuKey k = new ShopSkuKey(1, "123");
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            10.0, 10.0, 10.0, 1.0, null, null);
        FromIrisItemWrapper item = new FromIrisItemWrapper(
            ItemWrapperTestUtil.createItem(k,
                MdmIrisPayload.MasterDataSource.SUPPLIER, "123", itemShippingUnit)
        );
        cutoffService.openCutoff(generateCutoff(k, generateDefaultInvalidDimensionsCutoffInfo(),
            OfferCutoff.CutoffState.OPEN));
        fromIrisItemRepository.insert(item);
        mappingsCacheRepository.insert(generateMappingCache(k, random.nextLong(), random.nextInt()));
        prepareSuppliersBySsku(List.of(k), MdmSupplierType.THIRD_PARTY);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(k));

        Assertions.assertThat(cutoffService.findCutoffs(new OfferCutoffFilter())).containsExactlyInAnyOrder(
            generateCutoff(k, generateDefaultInvalidDimensionsCutoffInfo(), OfferCutoff.CutoffState.CLOSED)
        );
    }

    @Test
    public void testChangeMissToWrongDimensions() {
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS, true);
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS_DRY_RUN, false);
        storageKeyValueService.invalidateCache();

        ShopSkuKey k = new ShopSkuKey(1, "123");
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            10.0, 10000.0, 10.0, 1.0, null, null);
        FromIrisItemWrapper item = new FromIrisItemWrapper(
            ItemWrapperTestUtil.createItem(k,
                MdmIrisPayload.MasterDataSource.SUPPLIER, "123", itemShippingUnit)
        );
        cutoffService.openCutoff(generateCutoff(k, OfferCutoffTypeProvider.missDimensionsError(),
            OfferCutoff.CutoffState.OPEN));
        fromIrisItemRepository.insert(item);
        mappingsCacheRepository.insert(generateMappingCache(k, random.nextLong(), random.nextInt()));
        prepareSuppliersBySsku(List.of(k), MdmSupplierType.THIRD_PARTY);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(k));

        Assertions.assertThat(cutoffService.findCutoffs(new OfferCutoffFilter())).containsExactlyInAnyOrder(
            generateCutoff(k, OfferCutoffTypeProvider.missDimensionsError(), OfferCutoff.CutoffState.CLOSED),
            generateCutoff(k, generateDefaultInvalidDimensionsCutoffInfo(), OfferCutoff.CutoffState.OPEN)
        );
    }

    @Test
    public void testChangeWrongToMissDimensions() {
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS, true);
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS_DRY_RUN, false);
        storageKeyValueService.invalidateCache();

        ShopSkuKey k = new ShopSkuKey(1, "123");
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            null, null, null, null, null, null);
        FromIrisItemWrapper item = new FromIrisItemWrapper(
            ItemWrapperTestUtil.createItem(k,
                MdmIrisPayload.MasterDataSource.SUPPLIER, "123", itemShippingUnit)
        );
        cutoffService.openCutoff(generateCutoff(k, generateDefaultInvalidDimensionsCutoffInfo(),
            OfferCutoff.CutoffState.OPEN));
        fromIrisItemRepository.insert(item);
        mappingsCacheRepository.insert(generateMappingCache(k, random.nextLong(), random.nextInt()));
        prepareSuppliersBySsku(List.of(k), MdmSupplierType.THIRD_PARTY);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(k));

        Assertions.assertThat(cutoffService.findCutoffs(new OfferCutoffFilter())).containsExactlyInAnyOrder(
            generateCutoff(k, generateDefaultInvalidDimensionsCutoffInfo(), OfferCutoff.CutoffState.CLOSED),
            generateCutoff(k, OfferCutoffTypeProvider.missDimensionsError(), OfferCutoff.CutoffState.OPEN)
        );
    }

    @Test
    public void testWhenValidDimensionsThenNoAction() {
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS, true);
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS_DRY_RUN, false);
        storageKeyValueService.invalidateCache();

        ShopSkuKey k = new ShopSkuKey(1, "123");
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            10.0, 10.0, 10.0, 1.0, null, null);
        FromIrisItemWrapper item = new FromIrisItemWrapper(
            ItemWrapperTestUtil.createItem(k,
                MdmIrisPayload.MasterDataSource.SUPPLIER, "123", itemShippingUnit)
        );

        fromIrisItemRepository.insert(item);
        mappingsCacheRepository.insert(generateMappingCache(k, random.nextLong(), random.nextInt()));
        prepareSuppliersBySsku(List.of(k), MdmSupplierType.THIRD_PARTY);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(k));

        Assertions.assertThat(cutoffService.findCutoffs(new OfferCutoffFilter())).isEmpty();
    }

    @Test
    public void testWhenNoMappingsThenNoAction() {
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS, true);
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS_DRY_RUN, false);
        storageKeyValueService.invalidateCache();

        ShopSkuKey k = new ShopSkuKey(1, "123");
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            10.0, 10.0, 10000.0, 1.0, null, null);
        FromIrisItemWrapper item = new FromIrisItemWrapper(
            ItemWrapperTestUtil.createItem(k,
                MdmIrisPayload.MasterDataSource.SUPPLIER, "123", itemShippingUnit)
        );
        prepareSuppliersBySsku(List.of(k), MdmSupplierType.THIRD_PARTY);
        fromIrisItemRepository.insert(item);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(k));

        Assertions.assertThat(cutoffService.findCutoffs(new OfferCutoffFilter())).isEmpty();
    }

    @Test
    public void testWhenWhiteOrUnknownSupplierThenNoAction() {
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS, true);
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS_DRY_RUN, false);
        storageKeyValueService.invalidateCache();

        ShopSkuKey k1 = new ShopSkuKey(1, "123");
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit1 = ItemWrapperTestUtil.generateShippingUnit(
            10.0, 10.0, 10000.0, 1.0, null, null);
        FromIrisItemWrapper item1 = new FromIrisItemWrapper(
            ItemWrapperTestUtil.createItem(k1,
                MdmIrisPayload.MasterDataSource.SUPPLIER, "123", itemShippingUnit1)
        );
        ShopSkuKey k2 = new ShopSkuKey(2, "234");
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit2 = ItemWrapperTestUtil.generateShippingUnit(
            10.0, 10.0, 10000.0, 1.0, null, null);
        FromIrisItemWrapper item2 = new FromIrisItemWrapper(
            ItemWrapperTestUtil.createItem(k2,
                MdmIrisPayload.MasterDataSource.SUPPLIER, "234", itemShippingUnit2)
        );
        prepareSuppliersBySsku(List.of(k1), MdmSupplierType.MARKET_SHOP);
        mappingsCacheRepository.insertOrUpdateAll(List.of(
            generateMappingCache(k1, random.nextLong(), random.nextInt()),
            generateMappingCache(k2, random.nextLong(), random.nextInt())
        ));
        fromIrisItemRepository.insertBatch(item1, item2);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(k1, k2));

        Assertions.assertThat(cutoffService.findCutoffs(new OfferCutoffFilter())).isEmpty();
    }

    @Test
    public void testInvalidDimsOfferCutoffParams() {
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS, true);
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS_DRY_RUN, false);
        storageKeyValueService.invalidateCache();

        ShopSkuKey key = new ShopSkuKey(1, "123");
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            10.0, 10000.0, 10.0, 1.0, null, null);
        FromIrisItemWrapper item = new FromIrisItemWrapper(
            ItemWrapperTestUtil.createItem(key,
                MdmIrisPayload.MasterDataSource.SUPPLIER, "123", itemShippingUnit)
        );
        fromIrisItemRepository.insert(item);
        MappingCacheDao mapping = generateMappingCache(key, 123L, 123);
        mappingsCacheRepository.insert(mapping);
        prepareSuppliersBySsku(List.of(key), MdmSupplierType.THIRD_PARTY);

        BigDecimal sizeShortMinValue = WeightDimensionsValidator.SIZE_MIN.add(BigDecimal.ONE);
        CategoryParamValue sizeShortMin = new CategoryParamValue();
        sizeShortMin.setCategoryId(mapping.getCategoryId())
            .setMdmParamId(KnownMdmParams.SIZE_SHORT_MIN_CM)
            .setNumeric(sizeShortMinValue);
        categoryParamValueRepository.insert(sizeShortMin);
        BigDecimal sizeLongMaxValue = WeightDimensionsValidator.SIZE_LONG_MAX.add(BigDecimal.ONE);
        storageKeyValueService.putValue(MdmProperties.SIZE_LONG_MAX_VALUE_KEY, sizeLongMaxValue);
        storageKeyValueService.invalidateCache();

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(key));

        OfferCutoff expectedOfferCutoff =
            generateCutoff(key, generateDefaultInvalidDimensionsCutoffInfo(), OfferCutoff.CutoffState.OPEN);
        expectedOfferCutoff.addErrorData(OfferCutoffTypeProvider.MIN_SHORT_SIZE_PARAM_NAME, sizeShortMinValue);
        expectedOfferCutoff.addErrorData(OfferCutoffTypeProvider.MAX_LONG_SIZE_PARAM_NAME, sizeLongMaxValue);
        expectedOfferCutoff.setErrorData(updateErrorData(expectedOfferCutoff.getErrorData()));

        List<OfferCutoff> allCutoffs = cutoffService.findCutoffs(new OfferCutoffFilter());
        Assertions.assertThat(allCutoffs).hasSize(1);
        assertEquals(allCutoffs.get(0), expectedOfferCutoff);
    }

    @Test
    public void testInvalidDensityOfferCutoffs() {
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS, true);
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS_DRY_RUN, false);
        storageKeyValueService.invalidateCache();

        ShopSkuKey k = new ShopSkuKey(1, "123");
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            0.1, 0.1, 0.1, 100.0, null, null); // 100 кг/мм^3 ~в 5*10^6 раз плотнее, чем платина
        FromIrisItemWrapper item = new FromIrisItemWrapper(
            ItemWrapperTestUtil.createItem(k,
                MdmIrisPayload.MasterDataSource.SUPPLIER, "123", itemShippingUnit)
        );

        fromIrisItemRepository.insert(item);
        mappingsCacheRepository.insert(generateMappingCache(k, random.nextLong(), random.nextInt()));
        prepareSuppliersBySsku(List.of(k), MdmSupplierType.THIRD_PARTY);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(k));

        OfferCutoff expectedOfferCutoff =
            generateCutoff(k, OfferCutoffTypeProvider.invalidDensityError(), OfferCutoff.CutoffState.OPEN);

        List<OfferCutoff> allCutoffs = cutoffService.findCutoffs(new OfferCutoffFilter());
        Assertions.assertThat(allCutoffs).hasSize(1);
        assertEquals(allCutoffs.get(0), expectedOfferCutoff);
    }

    @Test
    public void testInvalidDensityCutoffClosing() {
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS, true);
        storageKeyValueService.putValue(MdmProperties.HIDE_OFFERS_WITHOUT_DIMS_DRY_RUN, false);
        storageKeyValueService.invalidateCache();

        ShopSkuKey k = new ShopSkuKey(1, "123");

        //upload existing cutoff
        OfferCutoff existingCutoff =
            generateCutoff(k, OfferCutoffTypeProvider.invalidDensityError(), OfferCutoff.CutoffState.OPEN);
        cutoffService.openCutoff(existingCutoff);

        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            1.0, 1.0, 1.0, 0.001, null, null); // 1 г/см^3 - вода
        FromIrisItemWrapper item = new FromIrisItemWrapper(
            ItemWrapperTestUtil.createItem(k,
                MdmIrisPayload.MasterDataSource.SUPPLIER, "123", itemShippingUnit)
        );

        fromIrisItemRepository.insert(item);
        mappingsCacheRepository.insert(generateMappingCache(k, random.nextLong(), random.nextInt()));
        prepareSuppliersBySsku(List.of(k), MdmSupplierType.THIRD_PARTY);

        OfferCutoff expectedCutoff = new OfferCutoff().copyFrom(existingCutoff)
            .setState(OfferCutoff.CutoffState.CLOSED);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(k));

        List<OfferCutoff> allCutoffs = cutoffService.findCutoffs(new OfferCutoffFilter());
        Assertions.assertThat(allCutoffs).hasSize(1);
        assertEquals(expectedCutoff, allCutoffs.iterator().next());
    }

    @Test
    public void testSavingIntoSskuGoldenParamRepository() {
        prepareSuppliers(List.of(1), MdmSupplierType.THIRD_PARTY);
        prepareSuppliers(List.of(2), MdmSupplierType.MARKET_SHOP);
        List<String> shopSkus1 = List.of("312");
        List<String> shopSkus2 = List.of("abc/4543", "[что-то]=(something)");

        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit1 = ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0,
            10.0, 1.0, null, null);

        List<FromIrisItemWrapper> items1 = Stream.concat(shopSkus1.stream(), shopSkus2.stream())
            .map(sku -> new ShopSkuKey(1, sku))
            .map(key -> ItemWrapperTestUtil.createItem(key, MdmIrisPayload.MasterDataSource.WAREHOUSE, "123",
                itemShippingUnit1))
            .map(FromIrisItemWrapper::new)
            .collect(Collectors.toList());
        fromIrisItemRepository.insertBatch(items1);

        List<String> shopSkusWhite = List.of("423");
        List<FromIrisItemWrapper> itemsWhite = shopSkusWhite.stream()
            .map(sku -> new ShopSkuKey(2, sku))
            .map(key -> ItemWrapperTestUtil.createItem(key, MdmIrisPayload.MasterDataSource.WAREHOUSE, "123",
                itemShippingUnit1))
            .map(FromIrisItemWrapper::new)
            .collect(Collectors.toList());
        fromIrisItemRepository.insertBatch(itemsWhite);

        List<String> shopSkusUnk = List.of("534");
        List<FromIrisItemWrapper> itemsUnknown = shopSkusUnk.stream()
            .map(sku -> new ShopSkuKey(3, sku))
            .map(key -> ItemWrapperTestUtil.createItem(key, MdmIrisPayload.MasterDataSource.WAREHOUSE, "123",
                itemShippingUnit1))
            .map(FromIrisItemWrapper::new)
            .collect(Collectors.toList());
        fromIrisItemRepository.insertBatch(itemsUnknown);

        storageKeyValueService.putValue(MdmProperties.WRITE_OWN_SSKU_WD_GLOBALLY, true);
        storageKeyValueService.putValue(MdmProperties.CATEGORIES_TO_APPLY_FORCE_INHERITANCE, List.of(333));
        mappingsCacheRepository.insertOrUpdateAll(List.of(
            new MappingCacheDao().setCategoryId(333)
                .setShopSkuKey(new ShopSkuKey(1, "312"))
                .setMskuId(111L),
            new MappingCacheDao().setCategoryId(333)
                .setShopSkuKey(new ShopSkuKey(2, "423"))
                .setMskuId(111L),
            new MappingCacheDao().setCategoryId(333)
                .setShopSkuKey(new ShopSkuKey(3, "534"))
                .setMskuId(111L)
        ));

        sskuToRefreshProcessingService.processShopSkuKeys(
            Stream.of(items1, itemsWhite, itemsUnknown)
                .flatMap(List::stream)
                .map(FromIrisItemWrapper::getShopSkuKey)
                .collect(Collectors.toList())
        );

        Set<String> resultingSkus1 = referenceItemRepository.findAll().stream()
            .map(ItemWrapper::getShopSku)
            .collect(Collectors.toSet());
        Assertions.assertThat(resultingSkus1).containsExactlyInAnyOrderElementsOf(Stream.of(shopSkus1, shopSkus2)
            .flatMap(List::stream)
            .collect(Collectors.toList()));

        List<String> resultingSkus2 = goldSskuRepository.findAllSskus().stream()
            .map(CommonSsku::getShopSkuKey)
            .map(ShopSkuKey::getShopSku)
            .collect(Collectors.toList());
        Assertions.assertThat(resultingSkus2).containsOnlyElementsOf(Stream.concat(shopSkus1.stream(),
            shopSkus2.stream()).collect(Collectors.toList())
        );

        List<String> shopSkus3 = List.of("12345");
        List<String> shopSkus4 = List.of("abcdef", "уаукукак");
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit2 = ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0,
            10.0, 1.0, null, null);
        List<FromIrisItemWrapper> items2 = Stream.concat(shopSkus3.stream(), shopSkus4.stream())
            .map(sku -> new ShopSkuKey(1, sku))
            .map(key -> ItemWrapperTestUtil.createItem(key, MdmIrisPayload.MasterDataSource.WAREHOUSE, "123",
                itemShippingUnit2))
            .map(FromIrisItemWrapper::new)
            .collect(Collectors.toList());
        fromIrisItemRepository.insertBatch(items2);

        storageKeyValueService.putValue(MdmProperties.WRITE_OWN_SSKU_WD_GLOBALLY, false);
        storageKeyValueService.putValue(MdmProperties.CATEGORIES_TO_WRITE_OWN_SSKU_WD, List.of(666));
        storageKeyValueService.invalidateCache();
        mappingsCacheRepository.insert(
            new MappingCacheDao().setCategoryId(666)
                .setShopSkuKey(new ShopSkuKey(1, "12345"))
                .setMskuId(111L)
        );

        sskuToRefreshProcessingService.processShopSkuKeys(
            items2.stream()
                .map(FromIrisItemWrapper::getShopSkuKey)
                .collect(Collectors.toList())
        );

        List<String> resultingSkus3 = goldSskuRepository.findAllSskus().stream()
            .map(CommonSsku::getShopSkuKey)
            .map(ShopSkuKey::getShopSku)
            .collect(Collectors.toList());
        Assertions.assertThat(resultingSkus3).containsOnlyElementsOf(Stream.concat(
            Stream.concat(shopSkus1.stream(), shopSkus2.stream()), shopSkus3.stream()).collect(Collectors.toSet())
        );
    }

    @Test
    public void readExistingReferenceItemsFromGoldenParamTable() {
        ShopSkuKey shopSkuKey = new ShopSkuKey(42, "test");

        // test with stage 4 (read existing ref items from golden param table) + write into golden param table
        storageKeyValueService.putValue(MdmProperties.WRITE_OWN_SSKU_WD_GLOBALLY, true);
        storageKeyValueService.invalidateCache();

        // create 4 ssku golden param values
        List<SskuGoldenParamValue> goldenParamValues = sskuGoldenParamUtil.createSskuGoldenParamValuesFromReferenceItem(
            new ReferenceItemWrapper(ItemWrapperTestUtil.createItem(shopSkuKey,
                MdmIrisPayload.MasterDataSource.WAREHOUSE, ItemWrapperTestUtil.generateShippingUnit(
                    100.0, 100.0, 50.0, 10.0, null, null,
                    1234L))), SskuGoldenParamUtil.ParamsGroup.SSKU);
        goldSskuRepository.insertOrUpdateSsku(new CommonSsku(shopSkuKey).setBaseValues(goldenParamValues));

        // new item is the same, only source updated timestamp is different.
        // If the previous value is found, we won't update VGH (see MdmGoldenItemServiceImpl.isUpdateNeeded)
        var fromIrisItem = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(shopSkuKey,
            MdmIrisPayload.MasterDataSource.WAREHOUSE, ItemWrapperTestUtil.generateShippingUnit(
                100.0, 100.0, 50.0, 10.0, null, null,
                12345678L)));
        fromIrisItemRepository.insert(fromIrisItem);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(shopSkuKey));

        Mockito.verify(goldSskuRepositorySpy, Mockito.never()).insertOrUpdateSskus(Mockito.anyList());

        List<SskuGoldenParamValue> newGoldenParams = goldSskuRepository.findAllSskus().stream()
            .map(CommonSsku::getBaseValues)
            .flatMap(List::stream)
            .map(SskuGoldenParamValue::fromSskuParamValue)
            .collect(Collectors.toList());
        Assertions.assertThat(newGoldenParams.stream().map(SskuGoldenParamValue::getSourceUpdatedTs))
            .containsOnly(TimestampUtil.toInstant(1234L));
    }

    @Test
    public void testSskuProcessingEnqueuesToOtherQueuesWithCorrectPriorities() {
        long mskuId1 = 1, mskuId2 = 2, mskuId3 = 3;
        mappingsCacheRepository.insertOrUpdateAll(List.of(
            generateMappingCache(STAGE3_KEY1, mskuId1, 2),
            generateMappingCache(STAGE3_KEY2, mskuId2, 3),
            generateMappingCache(STAGE2_KEY, mskuId3, 4) // won't be enqueued
        ));

        MdmSupplier business = new MdmSupplier()
            .setId(BUSINESS_KEY.getSupplierId())
            .setType(MdmSupplierType.BUSINESS);
        MdmSupplier stage2Partner = new MdmSupplier()
            .setId(STAGE2_KEY.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(business.getId())
            .setBusinessEnabled(false);
        MdmSupplier stage3Partner1 = new MdmSupplier()
            .setId(STAGE3_KEY1.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(business.getId())
            .setBusinessEnabled(true);
        MdmSupplier anotherBusiness = new MdmSupplier()
            .setId(BUSINESS_KEY_2.getSupplierId())
            .setType(MdmSupplierType.BUSINESS);
        MdmSupplier stage3Partner2 = new MdmSupplier()
            .setId(STAGE3_KEY2.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(anotherBusiness.getId())
            .setBusinessEnabled(true);
        mdmSupplierRepository.insertBatch(business, stage2Partner, stage3Partner1,
            anotherBusiness, stage3Partner2);
        mdmSupplierCachingService.refresh();


        long outdatedTs = 1;
        long slightlyOutdatedTs = 2;
        long mostRecentTs = 4;

        var businessShippingUnit =
            ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0, 10.0, 1.0, null, null, outdatedTs);
        var stage3ShippingUnit1 =
            ItemWrapperTestUtil.generateShippingUnit(20.0, 20.0, 20.0, 2.0, null, null, slightlyOutdatedTs);
        var stage3ShippingUnit2 =
            ItemWrapperTestUtil.generateShippingUnit(30.0, 30.0, 30.0, 3.0, null, null, mostRecentTs);
        var unchangedShippingUnit =
            ItemWrapperTestUtil.generateShippingUnit(40.0, 40.0, 40.0, 4.0, null, null, mostRecentTs);

        var businessItem = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            BUSINESS_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "172", businessShippingUnit));
        var stage3Item1 = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE3_KEY1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "66", stage3ShippingUnit1));
        var stage3Item2 = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE3_KEY2, MdmIrisPayload.MasterDataSource.WAREHOUSE, "27", stage3ShippingUnit2));
        var unchangedItem = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE2_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "69", unchangedShippingUnit));
        fromIrisItemRepository.insertBatch(businessItem, stage3Item1, stage3Item2, unchangedItem);
        // Добавим уже существующее не меняющееся золото
        referenceItemRepository.insert(new ReferenceItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE2_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "69", unchangedShippingUnit)));

        // Задаем приоритет для ключей
        sskuToRefreshProcessingService.processSsku(Stream.of(BUSINESS_KEY, STAGE3_KEY1, STAGE3_KEY2, STAGE2_KEY)
            .map(it -> {
                var info = new SskuToRefreshInfo().setShopSku(it.getShopSku()).setSupplierId(it.getSupplierId());
                info.setPriority(MdmQueuePriorities.DATACAMP_OFFERS_PRIORITY);
                return info;
            })
            .collect(Collectors.toList())
        );

        // Проверяем, что во все очереди ключи добавились с нужным приоритетом
        Map<Integer, List<SskuToRefreshInfo>> infosByPriority = sendToDatacampQRepository.findAll().stream()
            .collect(Collectors.groupingBy(MdmQueueInfoBase::getPriority));
        Assertions.assertThat(infosByPriority.get(MdmQueuePriorities.DATACAMP_OFFERS_PRIORITY).size()).isEqualTo(2);
        Set<ShopSkuKey> enqueuedKeys = infosByPriority.get(MdmQueuePriorities.DATACAMP_OFFERS_PRIORITY).stream()
            .map(MdmQueueInfoBase::getEntityKey).collect(Collectors.toSet());
        Assertions.assertThat(enqueuedKeys).containsExactlyInAnyOrder(BUSINESS_KEY, BUSINESS_KEY_2);

        Map<Integer, List<MdmMskuQueueInfo>> mskusByPriority = mskuToRefreshRepository.findAll().stream()
            .collect(Collectors.groupingBy(MdmMskuQueueInfo::getPriority));
        Assertions.assertThat(mskusByPriority.get(MdmQueuePriorities.DATACAMP_OFFERS_PRIORITY).size()).isEqualTo(2);
        List<Long> enqueuedMskus = mskusByPriority.get(MdmQueuePriorities.DATACAMP_OFFERS_PRIORITY).stream()
            .map(MdmQueueInfoBase::getEntityKey).collect(Collectors.toList());
        Assertions.assertThat(enqueuedMskus).containsOnly(mskuId1, mskuId2);

        Map<Integer, List<SskuToRefreshInfo>> refItemsByPriority = sendReferenceItemQRepository.findAll().stream()
            .collect(Collectors.groupingBy(MdmQueueInfoBase::getPriority));
        Assertions.assertThat(refItemsByPriority.get(MdmQueuePriorities.DATACAMP_OFFERS_PRIORITY).size()).isEqualTo(2);
        List<ShopSkuKey> enqueuedSskus = refItemsByPriority.get(MdmQueuePriorities.DATACAMP_OFFERS_PRIORITY).stream()
            .map(MdmQueueInfoBase::getEntityKey).collect(Collectors.toList());
        Assertions.assertThat(enqueuedSskus).containsOnly(STAGE3_KEY1, STAGE3_KEY2);
    }

    @Test
    public void testThatShelfLifeBlockIsBuiltUsingExistingSilverSskuValuesWhileMerging() {
        prepareBusinessGroup(false);
        Instant ts = Instant.now();

        MasterData masterData = electrumSskuWithOldShelfLifeComment(STAGE3_KEY1);
        masterData.setModifiedTimestamp(LocalDateTime.now().minusDays(14));
        masterDataRepository.insertOrUpdate(masterData);

        var sskuByService = silverSskuWithShelfLife(STAGE3_KEY1, "worse_source",
            MasterDataSourceType.SUPPLIER, ts.minus(100, ChronoUnit.DAYS), 0, false,
            "хранить в сухом помещении!", 1L);
        var sskuByBiz = silverSskuWithShelfLife(BUSINESS_KEY, "best_source",
            MasterDataSourceType.SUPPLIER, ts.minus(17, ChronoUnit.DAYS), 5, true,
            "хранить в сухом помещении!!!", 586L);

        silverSskuRepository.insertOrUpdateSsku(sskuByService);
        silverSskuRepository.insertOrUpdateSsku(sskuByBiz);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(BUSINESS_KEY, STAGE3_KEY1, STAGE3_KEY2));

        MasterData resultMasterData = masterDataRepository.findById(STAGE3_KEY1);

        MasterData expectedMasterData = electrumSskuWithOldShelfLifeComment(STAGE3_KEY1)
            .setShelfLife(5, TimeInUnits.TimeUnit.YEAR)
            .setShelfLifeComment("хранить в сухом помещении!!!");
        Assertions.assertThat(resultMasterData).isEqualTo(expectedMasterData);
    }

    @Test
    public void testSilverValuesMergedToGold() {
        prepareBusinessGroup(false);
        Instant ts = Instant.now();
        var editorSsku = silverSsku(STAGE3_KEY1, "cloudcat", MasterDataSourceType.MDM_OPERATOR, ts.plusSeconds(2),
            2, 18, List.of("Китай"), List.of(), null);
        var supplierSsku = silverSsku(STAGE3_KEY2, "supplier", MasterDataSourceType.SUPPLIER, ts,
            LOWER_OLDER_BOX_COUNT, LOWER_OLDER_DELIVERY_TIME, List.of("Монголия"), List.of(), null);
        var anotherSsku = silverSsku(STAGE3_KEY2, "xdoc", MasterDataSourceType.SUPPLIER, ts.plusSeconds(1),
            4, 20, List.of("Вьетнам"), List.of(), null);
        silverSskuRepository.insertOrUpdateSsku(editorSsku);
        silverSskuRepository.insertOrUpdateSsku(supplierSsku);
        silverSskuRepository.insertOrUpdateSsku(anotherSsku);
        jdbcTemplate.update("update mdm.ssku_silver_param_value set updated_ts = source_updated_ts ", Map.of());


        sskuToRefreshProcessingService.processShopSkuKeys(List.of(BUSINESS_KEY, STAGE3_KEY1, STAGE3_KEY2));

        MasterData service1 = masterDataRepository.findById(STAGE3_KEY1);
        MasterData service2 = masterDataRepository.findById(STAGE3_KEY2);
        Assertions.assertThat(masterDataRepository.totalCount()).isEqualTo(2);

        MasterData expected1 = electrumSsku(STAGE3_KEY1, 2, 18,
            List.of("Монголия", "Китай", "Вьетнам"), List.of());
        MasterData expected2 = electrumSsku(STAGE3_KEY2, 2, 20,
            List.of("Монголия", "Китай", "Вьетнам"), List.of());
        Assertions.assertThat(service1).isEqualTo(expected1);
        Assertions.assertThat(service2).isEqualTo(expected2);
    }

    @Test
    public void testSilverValuesWithoutEoxPresenceAreNotMergedToGold() {
        prepareBusinessGroup(false);
        Instant ts = Instant.now();
        sskuExistenceRepository.markExistence(STAGE3_KEY1, false);
        var editorSsku = silverSsku(STAGE3_KEY1, "cloudcat", MasterDataSourceType.MDM_OPERATOR, ts.plusSeconds(2),
            2, 18, List.of("Китай"), List.of(), null);
        var supplierSsku = silverSsku(STAGE3_KEY2, "supplier", MasterDataSourceType.SUPPLIER, ts,
            LOWER_OLDER_BOX_COUNT, LOWER_OLDER_DELIVERY_TIME, List.of("Монголия"), List.of(), null);
        var anotherSsku = silverSsku(STAGE3_KEY2, "xdoc", MasterDataSourceType.SUPPLIER, ts.plusSeconds(1),
            4, 20, List.of("Вьетнам"), List.of(), null);
        silverSskuRepository.insertOrUpdateSsku(editorSsku);
        silverSskuRepository.insertOrUpdateSsku(supplierSsku);
        silverSskuRepository.insertOrUpdateSsku(anotherSsku);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(BUSINESS_KEY, STAGE3_KEY1, STAGE3_KEY2));

        MasterData service2 = masterDataRepository.findById(STAGE3_KEY2);
        Assertions.assertThat(masterDataRepository.totalCount()).isEqualTo(1);

        MasterData expected2 = electrumSsku(STAGE3_KEY2, 4, 20,
            List.of("Вьетнам", "Монголия"), List.of());
        Assertions.assertThat(service2).isEqualTo(expected2);
    }

    @Test
    public void testPartialVghFiltering() {
        // given
        prepareBusinessGroup(false);
        Instant ts = Instant.now();
        var eoxSsku = silverSskuWithVgh(BUSINESS_KEY, "supplier", MasterDataSourceType.SUPPLIER, ts,
            10, 0, 0, 5, null);

        silverSskuRepository.insertOrUpdateSsku(eoxSsku);

        // when
        sskuToRefreshProcessingService.processShopSkuKeys(List.of(BUSINESS_KEY, STAGE3_KEY1));

        // then
        ReferenceItemWrapper service = referenceItemRepository.findById(STAGE3_KEY1);
        Assertions.assertThat(service).isNull();

    }

    private SilverCommonSsku silverSskuWithVgh(ShopSkuKey key,
                                               String sourceId,
                                               MasterDataSourceType type,
                                               Instant updatedTs,
                                               int length,
                                               int height,
                                               int width,
                                               int weightGross,
                                               Long masterDataVersion) {
        List<SskuSilverParamValue> result = new ArrayList<>();
        if (length > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.LENGTH);
            value.setXslName(paramCache.get(KnownMdmParams.LENGTH).getXslName());
            value.setNumeric(BigDecimal.valueOf(length));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (height > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.HEIGHT);
            value.setXslName(paramCache.get(KnownMdmParams.HEIGHT).getXslName());
            value.setNumeric(BigDecimal.valueOf(height));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (width > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.WIDTH);
            value.setXslName(paramCache.get(KnownMdmParams.WIDTH).getXslName());
            value.setNumeric(BigDecimal.valueOf(width));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (weightGross > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.WEIGHT_GROSS);
            value.setXslName(paramCache.get(KnownMdmParams.WEIGHT_GROSS).getXslName());
            value.setNumeric(BigDecimal.valueOf(length));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (masterDataVersion != null && masterDataVersion > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION)
                .setXslName(paramCache.get(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION).getXslName())
                .setNumeric(BigDecimal.valueOf(masterDataVersion));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        return TestDataUtils.wrapSilver(result);
    }

    @Test
    public void testReferenceItemInheritedFromMskuProperly() {
        // given
        prepareBusinessGroup(false);
        Instant ts = Instant.now();
        // just normal ssku with empty services
        long mskuId = 2222L;
        int categoryId = 111;
        var eoxSsku = silverSskuWithVgh(BUSINESS_KEY, "supplier", MasterDataSourceType.SUPPLIER, ts,
            10, 10, 10, 5, null);
        // Для добавления страны
        var supplierSsku = silverSsku(STAGE3_KEY1, "cloudcat", MasterDataSourceType.SUPPLIER, ts,
            2, 18, List.of("Китай"), List.of(), null);
        silverSskuRepository.insertOrUpdateSsku(eoxSsku);
        silverSskuRepository.insertOrUpdateSsku(supplierSsku);

        // extended category limits
        CategoryParamValue sizeLongMax = new CategoryParamValue();
        sizeLongMax.setCategoryId(categoryId)
            .setMdmParamId(KnownMdmParams.SIZE_LONG_MAX_CM)
            .setNumeric(BigDecimal.valueOf(1000L));
        categoryParamValueRepository.insert(sizeLongMax);

        // msku with length that is greater that general limit
        mappingsCacheRepository.insertOrUpdateAll(List.of(
            new MappingCacheDao().setCategoryId(categoryId)
                .setShopSkuKey(BUSINESS_KEY)
                .setMskuId(mskuId),
            new MappingCacheDao().setCategoryId(categoryId)
                .setShopSkuKey(STAGE3_KEY1)
                .setMskuId(mskuId),
            new MappingCacheDao().setCategoryId(categoryId)
                .setShopSkuKey(STAGE3_KEY2)
                .setMskuId(mskuId)));
        CommonMsku commonMsku = prepareTestMskuWithLongLength(categoryId, mskuId);
        mskuRepository.insertOrUpdateMsku(commonMsku);

        // force inheritance to receive data from msku
        storageKeyValueService.putValue(MdmProperties.CATEGORIES_TO_APPLY_FORCE_INHERITANCE, List.of(categoryId));
        storageKeyValueService.invalidateCache();

        // when
        sskuToRefreshProcessingService.processShopSkuKeys(List.of(BUSINESS_KEY, STAGE3_KEY1));

        // then
        var goldenVerdicts = sskuGoldenVerdictRepository.findByIds(List.of(BUSINESS_KEY, STAGE3_KEY1));
        Assertions.assertThat(goldenVerdicts).allMatch(VerdictResult::isValid);
        var referenceItems = referenceItemRepository.findByIds(List.of(STAGE3_KEY1));
        Assertions.assertThat(referenceItems).hasSize(1);
        var referenceItem = referenceItems.get(0);
        // check that vgh ws propagated from msku
        Assertions.assertThat(referenceItem.getCombinedItemShippingUnit().getWeightGrossMg().getValue())
            .isEqualTo(20_000_000L);
        Assertions.assertThat(referenceItem.getCombinedItemShippingUnit().getLengthMicrometer().getValue())
            .isEqualTo(6_660_000L);
    }

    private CommonMsku prepareTestMskuWithLongLength(Integer categoryId, Long mskuId) {
        MskuParamValue length = new MskuParamValue();
        length
            .setMasterDataSourceType(MasterDataSourceType.SUPPLIER)
            .setSourceUpdatedTs(Instant.now())
            .setMdmParamId(KnownMdmParams.LENGTH)
            .setNumeric(BigDecimal.valueOf(666));
        MskuParamValue width = new MskuParamValue();
        width
            .setMasterDataSourceType(MasterDataSourceType.SUPPLIER)
            .setSourceUpdatedTs(Instant.now())
            .setMdmParamId(KnownMdmParams.WIDTH)
            .setNumeric(BigDecimal.valueOf(50));
        MskuParamValue height = new MskuParamValue();
        height
            .setMasterDataSourceType(MasterDataSourceType.SUPPLIER)
            .setSourceUpdatedTs(Instant.now())
            .setMdmParamId(KnownMdmParams.HEIGHT)
            .setNumeric(BigDecimal.valueOf(50));
        MskuParamValue weight = new MskuParamValue();
        weight
            .setMasterDataSourceType(MasterDataSourceType.SUPPLIER)
            .setSourceUpdatedTs(Instant.now())
            .setMdmParamId(KnownMdmParams.WEIGHT_GROSS)
            .setNumeric(BigDecimal.valueOf(20));

        CommonMsku commonMsku = new CommonMsku(new ModelKey(categoryId, mskuId), List.of(length, width, height, weight))
            .setMskuId(mskuId);
        return commonMsku;
    }

    @Test
    public void testCalculateMasterDataAndReferenceItemsAndGenerateVerdicts() {
        prepareBusinessGroup(false);
        storageKeyValueService.invalidateCache();

        var stage3ShippingUnit1 =
            ItemWrapperTestUtil.generateShippingUnit(20.0, 20.0, 20.0, 2.0, null, null);

        var stage3Item1 = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE3_KEY1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "66", stage3ShippingUnit1));
        stage3Item1.setProcessed(true);
        fromIrisItemRepository.insertBatch(stage3Item1);

        Instant ts = Instant.now();
        long mdVersionBusinessKey = 5L;
        var eoxSsku = silverSsku(BUSINESS_KEY, "supplier", MasterDataSourceType.SUPPLIER, ts,
            1, 17, List.of("Россия", "Монголия", "Китай", "Вьетнам"), List.of(), mdVersionBusinessKey);
        // master data version берется из бизнес-части
        var editorSsku = silverSsku(STAGE3_KEY1, "cloudcat", MasterDataSourceType.MDM_OPERATOR, ts.plusSeconds(2),
            2, 18, List.of("Китай"), List.of(), null);
        var supplierSsku = silverSsku(STAGE3_KEY2, "supplier", MasterDataSourceType.SUPPLIER, ts,
            LOWER_OLDER_BOX_COUNT, LOWER_OLDER_DELIVERY_TIME, List.of("Монголия"), List.of(), null);
        var anotherSsku = silverSsku(STAGE3_KEY2, "xdoc", MasterDataSourceType.SUPPLIER, ts.plusSeconds(1),
            4, 20, List.of("Вьетнам"), List.of(), null);
        silverSskuRepository.insertOrUpdateSsku(eoxSsku);
        silverSskuRepository.insertOrUpdateSsku(editorSsku);
        silverSskuRepository.insertOrUpdateSsku(supplierSsku);
        silverSskuRepository.insertOrUpdateSsku(anotherSsku);
        jdbcTemplate.update("update mdm.ssku_silver_param_value set updated_ts = source_updated_ts ", Map.of());

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(BUSINESS_KEY, STAGE3_KEY1, STAGE3_KEY2));

        MasterData service1 = masterDataRepository.findById(STAGE3_KEY1);
        MasterData service2 = masterDataRepository.findById(STAGE3_KEY2);
        Assertions.assertThat(masterDataRepository.totalCount()).isEqualTo(2);

        MasterData expected1 = electrumSsku(STAGE3_KEY1, 2, 18,
            List.of("Россия", "Монголия", "Китай", "Вьетнам"), List.of(), mdVersionBusinessKey);
        MasterData expected2 = electrumSsku(STAGE3_KEY2, 2, 20,
            List.of("Россия", "Монголия", "Китай", "Вьетнам"), List.of(), mdVersionBusinessKey);
        Assertions.assertThat(service1).isEqualTo(expected1);
        Assertions.assertThat(service2).isEqualTo(expected2);

        Map<ShopSkuKey, SskuVerdictResult> goldenVerdicts = sskuGoldenVerdictRepository
            .findByIds(List.of(BUSINESS_KEY, STAGE3_KEY1, STAGE3_KEY2))
            .stream()
            .collect(Collectors.toMap(SskuVerdictResult::getKey, Function.identity()));
        var expectedVerdictByBusinessKey = new SskuVerdictResult();
        expectedVerdictByBusinessKey.setKey(BUSINESS_KEY);
        expectedVerdictByBusinessKey.setValid(true);
        expectedVerdictByBusinessKey.setContentVersionId(5L);
        expectedVerdictByBusinessKey.setSingleVerdictResults(Map.of(
            VerdictFeature.UNSPECIFIED,
            VerdictGeneratorHelper.createOkVerdict(VerdictFeature.UNSPECIFIED))
        );
        var expectedVerdictByService1 = new SskuVerdictResult();
        expectedVerdictByService1.setKey(STAGE3_KEY1);
        expectedVerdictByService1.setValid(true);
        expectedVerdictByService1.setContentVersionId(5L);
        expectedVerdictByService1.setSingleVerdictResults(Map.of(
            VerdictFeature.UNSPECIFIED,
            VerdictGeneratorHelper.createOkVerdict(VerdictFeature.UNSPECIFIED))
        );
        var expectedVerdictByService2 = new SskuVerdictResult();
        expectedVerdictByService2.setKey(STAGE3_KEY2);
        expectedVerdictByService2.setValid(true);
        expectedVerdictByService2.setContentVersionId(5L);
        expectedVerdictByService2.setSingleVerdictResults(Map.of(
            VerdictFeature.UNSPECIFIED,
            VerdictGeneratorHelper.createOkVerdict(VerdictFeature.UNSPECIFIED))
        );

        // master data version должна совпадать с тем, что лежит в mdm.ssku_silver_param_value
        Assertions.assertThat(goldenVerdicts).isNotEmpty();
        Assertions.assertThat(goldenVerdicts.keySet()).contains(BUSINESS_KEY);
        Assertions.assertThat(goldenVerdicts.get(BUSINESS_KEY).getContentVersionId()).isEqualTo(mdVersionBusinessKey);
        Assertions.assertThat(goldenVerdicts.values())
            .usingElementComparatorIgnoringFields("updatedTs", "mdmVersionId", "mdmVersionTs", "mdmId")
            .containsExactlyInAnyOrder(
                expectedVerdictByBusinessKey,
                expectedVerdictByService1,
                expectedVerdictByService2);
    }

    @Test
    @SneakyThrows
    public void testCalculateMasterDataAndRefItemWithMasterDataVersion() {
        prepareBusinessGroup(false);
        storageKeyValueService.putValue(MdmProperties.CALCULATE_MD_GOLDEN_VERDICTS_ENABLED_KEY, true);
        storageKeyValueService.invalidateCache();

        var stage3ShippingUnit1 =
            ItemWrapperTestUtil.generateShippingUnit(20.0, 20.0, 20.0, 2.0, null, null);

        var stage3Item1 = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE3_KEY1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "66", stage3ShippingUnit1));
        stage3Item1.setProcessed(true); // avoid triggering
        fromIrisItemRepository.insertBatch(stage3Item1);

        Instant ts = Instant.now();
        long mdVersionBusinessKey = 5L;
        long mdVersionServiceKey1 = 2L;
        long mdVersion1ServiceKey2 = 1L;
        long mdVersion2ServiceKey2 = 3L;
        var eoxSsku = silverSsku(BUSINESS_KEY, "supplier", MasterDataSourceType.SUPPLIER, ts,
            1, 17, List.of("Россия", "Монголия", "Китай", "Вьетнам"), List.of(), mdVersionBusinessKey);
        // master data version берется из бизнес-части
        var editorSsku = silverSsku(STAGE3_KEY1, "cloudcat", MasterDataSourceType.MDM_OPERATOR, ts.plusSeconds(2),
            2, 18, List.of("Китай"), List.of(), mdVersionServiceKey1);
        var supplierSsku = silverSsku(STAGE3_KEY2, "supplier", MasterDataSourceType.SUPPLIER, ts,
            LOWER_OLDER_BOX_COUNT, LOWER_OLDER_DELIVERY_TIME, List.of("Монголия"), List.of(), mdVersion2ServiceKey2);
        var anotherSsku = silverSsku(STAGE3_KEY2, "xdoc", MasterDataSourceType.SUPPLIER, ts.plusSeconds(1),
            4, 20, List.of("Вьетнам"), List.of(), mdVersion1ServiceKey2);
        silverSskuRepository.insertOrUpdateSsku(eoxSsku);
        silverSskuRepository.insertOrUpdateSsku(editorSsku);
        silverSskuRepository.insertOrUpdateSsku(supplierSsku);
        silverSskuRepository.insertOrUpdateSsku(anotherSsku);

        jdbcTemplate.update("update mdm.ssku_silver_param_value set updated_ts = source_updated_ts ", Map.of());

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(BUSINESS_KEY, STAGE3_KEY1, STAGE3_KEY2));

        MasterData service1 = masterDataRepository.findById(STAGE3_KEY1);
        MasterData service2 = masterDataRepository.findById(STAGE3_KEY2);
        Assertions.assertThat(masterDataRepository.totalCount()).isEqualTo(2);

        MasterData expected1 = electrumSsku(STAGE3_KEY1, 2, 18,
            List.of("Россия", "Монголия", "Китай", "Вьетнам"), List.of(),
            5L);
        MasterData expected2 = electrumSsku(STAGE3_KEY2, 2, 20,
            List.of("Россия", "Монголия", "Китай", "Вьетнам"), List.of(),
            5L);
        Assertions.assertThat(service1).isEqualTo(expected1);
        Assertions.assertThat(service2).isEqualTo(expected2);

        Map<ShopSkuKey, SskuVerdictResult> goldenVerdicts = sskuGoldenVerdictRepository
            .findByIds(List.of(BUSINESS_KEY, STAGE3_KEY1, STAGE3_KEY2))
            .stream()
            .collect(Collectors.toMap(SskuVerdictResult::getKey, Function.identity()));
        var expectedVerdictByBusinessKey = new SskuVerdictResult();
        expectedVerdictByBusinessKey.setKey(BUSINESS_KEY);
        expectedVerdictByBusinessKey.setValid(true);
        expectedVerdictByBusinessKey.setContentVersionId(5L);
        expectedVerdictByBusinessKey.setSingleVerdictResults(Map.of(
            VerdictFeature.UNSPECIFIED,
            VerdictGeneratorHelper.createOkVerdict(VerdictFeature.UNSPECIFIED))
        );
        var expectedVerdictByService1 = new SskuVerdictResult();
        expectedVerdictByService1.setKey(STAGE3_KEY1);
        expectedVerdictByService1.setValid(true);
        expectedVerdictByService1.setContentVersionId(5L);
        expectedVerdictByService1.setSingleVerdictResults(Map.of(
            VerdictFeature.UNSPECIFIED,
            VerdictGeneratorHelper.createOkVerdict(VerdictFeature.UNSPECIFIED))
        );
        var expectedVerdictByService2 = new SskuVerdictResult();
        expectedVerdictByService2.setKey(STAGE3_KEY2);
        expectedVerdictByService2.setValid(true);
        expectedVerdictByService2.setContentVersionId(5L);
        expectedVerdictByService2.setSingleVerdictResults(Map.of(
            VerdictFeature.UNSPECIFIED,
            VerdictGeneratorHelper.createOkVerdict(VerdictFeature.UNSPECIFIED))
        );

        // master data version должна совпадать с тем, что лежит в mdm.ssku_silver_param_value
        Assertions.assertThat(goldenVerdicts).isNotEmpty();
        Assertions.assertThat(goldenVerdicts.keySet()).contains(BUSINESS_KEY);
        Assertions.assertThat(goldenVerdicts.get(BUSINESS_KEY).getContentVersionId()).isEqualTo(mdVersionBusinessKey);
        Assertions.assertThat(goldenVerdicts.values())
            .usingElementComparatorIgnoringFields("updatedTs", "mdmVersionId", "mdmVersionTs", "mdmId")
            .containsExactlyInAnyOrder(
                expectedVerdictByBusinessKey,
                expectedVerdictByService1,
                expectedVerdictByService2);
    }

    private void prepareBusinessGroup(boolean fullStage3) {
        MdmSupplier business = new MdmSupplier()
            .setId(BUSINESS_KEY.getSupplierId())
            .setType(MdmSupplierType.BUSINESS);
        MdmSupplier stage2Partner = new MdmSupplier()
            .setId(STAGE2_KEY.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(business.getId())
            .setBusinessEnabled(fullStage3);
        MdmSupplier stage3Partner1 = new MdmSupplier()
            .setId(STAGE3_KEY1.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(business.getId())
            .setBusinessEnabled(true);
        MdmSupplier stage3Partner2 = new MdmSupplier()
            .setId(STAGE3_KEY2.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(business.getId())
            .setBusinessEnabled(true);
        MdmSupplier whiteInBiz = new MdmSupplier()
            .setId(WHITE_IN_BIZ_KEY.getSupplierId())
            .setType(MdmSupplierType.MARKET_SHOP)
            .setBusinessId(business.getId())
            .setBusinessEnabled(true);
        MdmSupplier whiteLonely = new MdmSupplier()
            .setId(WHITE_LONELY_KEY.getSupplierId())
            .setType(MdmSupplierType.MARKET_SHOP);
        mdmSupplierRepository.insertBatch(business, stage2Partner, stage3Partner1, stage3Partner2,
            whiteInBiz, whiteLonely);
        mdmSupplierCachingService.refresh();
    }

    @Test
    public void testWhenWhiteOrUnknownSskuThenNoPartnerVerdict() {
        // given
        prepareBusinessGroup(true);

        // when
        sskuToRefreshProcessingService.processShopSkuKeys(List.of(UNKNOWN_KEY, WHITE_LONELY_KEY));

        // then
        List<SskuPartnerVerdictResult> results = sskuPartnerVerdictRepository.findByIds(List.of(UNKNOWN_KEY,
            WHITE_LONELY_KEY));
        Assertions.assertThat(results).isEmpty();
    }

    @Test
    public void testCorrectPartnerVerdictWhenDataWithOldStyleSupplierIdsPresented() {
        // given
        prepareBusinessGroup(true);
        Instant ts = Instant.now();

        // Правильная поставщиковская ССКУ с SourceId в новом стиле = DATACAMP
        var eoxSsku = SilverCommonSsku.fromCommonSsku(builder(BUSINESS_KEY)
                .with(KnownMdmParams.WIDTH, 14L)
                .with(KnownMdmParams.HEIGHT, 14L)
                .with(KnownMdmParams.LENGTH, 14L)
                .with(KnownMdmParams.WEIGHT_GROSS, 14L)
                .with(KnownMdmParams.MANUFACTURER_COUNTRY, "Россия", "Китай")
                .build(),
            new MasterDataSource(MasterDataSourceType.SUPPLIER, "oldFashionedKey"));

        // Добавляем данные поставщика с sourceId старого типа без бизнес части
        var oldSupplierData = silverSsku(STAGE3_KEY1, "oldFashionedKey", MasterDataSourceType.SUPPLIER,
            ts.minus(1000, ChronoUnit.SECONDS), 2, 18, List.of("Индия"), List.of(), null);

        // Обязательно вставляем в таком порядке, чтобы гарантировать, что данные в старом формате имеют
        // максимальную updatedTs
        silverSskuRepository.insertOrUpdateSsku(eoxSsku);
        silverSskuRepository.insertOrUpdateSsku(oldSupplierData);

        // when
        sskuToRefreshProcessingService.processShopSkuKeys(List.of(BUSINESS_KEY));

        // then
        var goldenVerdicts = sskuGoldenVerdictRepository.findByIds(List.of(BUSINESS_KEY, STAGE3_KEY1));
        Assertions.assertThat(goldenVerdicts).allMatch(VerdictResult::isValid);
        var partnerVerdicts = sskuPartnerVerdictRepository.findByIds(List.of(BUSINESS_KEY, STAGE3_KEY1));
        Assertions.assertThat(partnerVerdicts).allMatch(VerdictResult::isValid);
    }

    private void prepareSuppliers(List<Integer> supplierIds, MdmSupplierType type) {
        mdmSupplierRepository.insertOrUpdateAll(
            supplierIds.stream()
                .map(k -> new MdmSupplier().setId(k).setType(type))
                .collect(Collectors.toList())
        );
        mdmSupplierCachingService.refresh();
    }

    private void prepareSuppliersBySsku(List<ShopSkuKey> keys, MdmSupplierType type) {
        prepareSuppliers(keys.stream().map(ShopSkuKey::getSupplierId).collect(Collectors.toList()), type);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public static MdmIrisPayload.Item createItem(ShopSkuKey key,
                                                 MdmIrisPayload.MasterDataSource sourceType,
                                                 String sourceId,
                                                 Integer rslMinInboundLifetimeDay,
                                                 Integer rslMinInboundStartDay,
                                                 Integer rslMinOutboundLifetimeDay,
                                                 Integer rslMinOutboundStartDay,
                                                 MdmIrisPayload.ShippingUnit.Builder... shippingUnits) {
        MdmIrisPayload.ReferenceInformation.Builder information = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(MdmIrisPayload.Associate.newBuilder()
                .setId(sourceId)
                .setType(sourceType));

        for (MdmIrisPayload.ShippingUnit.Builder shippingUnit : shippingUnits) {
            if (shippingUnit.getConfiguraion() == MdmIrisPayload.ShippingUnitConfiguration.ITEM) {
                information.setItemShippingUnit(shippingUnit);
            } else {
                information.addShippingUnit(shippingUnit);
            }
        }

        MdmIrisPayload.ReferenceInformation.Builder rslInformation = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(MdmIrisPayload.Associate.newBuilder()
                .setId(MasterDataSourceType.RSL_SOURCE_ID)
                .setType(MdmIrisPayload.MasterDataSource.MDM));

        if (rslMinInboundLifetimeDay != null && rslMinInboundStartDay != null) {
            rslInformation.addMinInboundLifetimeDay(MdmIrisPayload.RemainingLifetime.newBuilder()
                .setValue(rslMinInboundLifetimeDay)
                .setStartDate(rslMinInboundStartDay));
        }
        if (rslMinOutboundLifetimeDay != null && rslMinOutboundStartDay != null) {
            rslInformation.addMinOutboundLifetimeDay(MdmIrisPayload.RemainingLifetime.newBuilder()
                .setValue(rslMinOutboundLifetimeDay)
                .setStartDate(rslMinOutboundStartDay));
        }

        return MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(key.getSupplierId())
                .setShopSku(key.getShopSku()))
            .addInformation(information)
            .addInformation(rslInformation)
            .build();
    }

    private OfferCutoff generateCutoff(ShopSkuKey key, OfferCutoffInfo cutoffInfo, OfferCutoff.CutoffState state) {
        return new OfferCutoff()
            .setSupplierId(key.getSupplierId())
            .setShopSku(key.getShopSku())
            .setOfferCutoffInfo(cutoffInfo)
            .setState(state);
    }

    private OfferCutoffInfo generateDefaultInvalidDimensionsCutoffInfo() {
        OfferCutoffInfo cutoffInfo = OfferCutoffTypeProvider.outOfBoundsDimensionsError(
            WeightDimensionsValidator.SIZE_MIN, WeightDimensionsValidator.SIZE_SHORT_MAX,
            WeightDimensionsValidator.SIZE_MIN, WeightDimensionsValidator.SIZE_MIDDLE_MAX,
            WeightDimensionsValidator.SIZE_MIN, WeightDimensionsValidator.SIZE_LONG_MAX,
            WeightDimensionsValidator.WEIGHT_MIN, WeightDimensionsValidator.WEIGHT_MAX
        );
        cutoffInfo.setErrorData(updateErrorData(cutoffInfo.getErrorData()));
        return cutoffInfo;
    }

    private void assertEquals(OfferCutoff expected, OfferCutoff actual) {
        Assertions.assertThat(actual).isEqualTo(expected);
        Assertions.assertThat(actual.getErrorData().getParams()).isEqualTo(expected.getErrorData().getParams());
        Assertions.assertThat(actual.getErrorCode()).isEqualTo(expected.getErrorCode());
    }

    /**
     * Need because of conversion: OfferCutoff -> JSON -> OfferCutoff in OfferCutoffRepositoryImpl.
     * BigDecimals can be parsed as Integer, Double... -> equals don't work.
     */
    private OfferCutoff.ErrorData updateErrorData(OfferCutoff.ErrorData errorData) {
        try {
            String serialized = JsonMapper.DEFAULT_OBJECT_MAPPER.writeValueAsString(errorData);
            return JsonMapper.DEFAULT_OBJECT_MAPPER.readValue(serialized, OfferCutoff.ErrorData.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private MappingCacheDao generateMappingCache(ShopSkuKey key, long mskuId, int categoryId) {
        return new MappingCacheDao()
            .setShopSkuKey(key)
            .setMskuId(mskuId)
            .setCategoryId(categoryId);
    }

    private CommonMsku prepareTestMsku(Integer categoryId, Long mskuId) {
        MskuParamValue goldShelfLife = new MskuParamValue();
        goldShelfLife
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
            .setSourceUpdatedTs(Instant.now().plusSeconds(100500))
            .setMdmParamId(KnownMdmParams.SHELF_LIFE)
            .setNumeric(BigDecimal.valueOf(20));
        MskuParamValue goldShelfLifeUnit = new MskuParamValue();
        goldShelfLifeUnit
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
            .setSourceUpdatedTs(Instant.now().plusSeconds(100500))
            .setMdmParamId(KnownMdmParams.SHELF_LIFE_UNIT)
            .setOption(new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.WEEK)));
        MskuParamValue expirDate = new MskuParamValue();
        expirDate
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
            .setSourceUpdatedTs(Instant.now())
            .setMdmParamId(KnownMdmParams.EXPIR_DATE)
            .setBool(true);
        CommonMsku commonMsku = new CommonMsku(new ModelKey(categoryId, mskuId), List.of(goldShelfLife,
            goldShelfLifeUnit, expirDate)).setMskuId(mskuId);
        return commonMsku;
    }

    private void prepareRsls(Integer categoryId, Long mskuId) {
        RslThreshold threshold1 = new RslThreshold()
            .setBeginningOfThreshold(new TimeInUnits(0, TimeInUnits.TimeUnit.DAY))
            .setEndOfThreshold(new TimeInUnits(30, TimeInUnits.TimeUnit.DAY))
            .setInRslDays(3)
            .setInRslPercents(43)
            .setOutRslDays(7)
            .setOutRslPercents(21);
        RslThreshold threshold2 = new RslThreshold()
            .setBeginningOfThreshold(new TimeInUnits(1, TimeInUnits.TimeUnit.MONTH))
            .setEndOfThreshold(TimeInUnits.UNLIMITED)
            .setInRslDays(12)
            .setInRslPercents(23)
            .setOutRslDays(20)
            .setOutRslPercents(10);

        MskuRsl mskuRsl = new MskuRsl().setMskuId(mskuId)
            .setInRslDays(5)
            .setOutRslDays(40)
            .setActivatedAt(Rsl.DEFAULT_START_DATE);
        CategoryRsl categoryRsl = new CategoryRsl()
            .setCategoryId(categoryId)
            .setInRslDays(3)
            .setOutRslPercents(5)
            .setActivatedAt(Rsl.DEFAULT_START_DATE);
        SupplierRsl supplierRsl = new SupplierRsl()
            .setType(RslType.THIRD_PARTY)
            .setRealId("")
            .setSupplierId(BUSINESS_KEY.getSupplierId())
            .setCategoryId(categoryId)
            .setActivatedAt(Rsl.DEFAULT_START_DATE)
            .setCargoType750(true)
            .setRslThresholds(List.of(threshold1, threshold2));
        SupplierRsl globalRsl = new SupplierRsl()
            .setType(RslType.GLOBAL_THIRD_PARTY)
            .setActivatedAt(Rsl.DEFAULT_START_DATE)
            .setRealId("")
            .setCargoType750(false)
            .setRslThresholds(List.of(threshold1, threshold2));

        mskuRslRepository.insert(mskuRsl);
        categoryRslRepository.insert(categoryRsl);
        supplierRslRepository.insert(supplierRsl);
        supplierRslRepository.insert(globalRsl);
    }

    private MasterData electrumSskuWithOldShelfLifeComment(ShopSkuKey key) {
        MasterData masterData = new MasterData();
        masterData.setShopSkuKey(key);
        masterData.setShelfLifeComment("Старый коммент про срок годности");
        return masterData;
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private SilverCommonSsku silverSskuWithShelfLife(ShopSkuKey key,
                                                     String sourceId,
                                                     MasterDataSourceType type,
                                                     Instant updatedTs,
                                                     int shelfLifePeriod,
                                                     boolean shelfLifeUnit,
                                                     String shelLifeComment,
                                                     Long masterDataVersion) {
        List<SskuSilverParamValue> result = new ArrayList<>();
        if (shelfLifePeriod > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.SHELF_LIFE);
            value.setXslName(paramCache.get(KnownMdmParams.SHELF_LIFE).getXslName());
            value.setNumeric(BigDecimal.valueOf(shelfLifePeriod));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (shelfLifeUnit) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.SHELF_LIFE_UNIT);
            value.setXslName(paramCache.get(KnownMdmParams.SHELF_LIFE_UNIT).getXslName());
            value.setOption(new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.YEAR)));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (shelLifeComment != null) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.SHELF_LIFE_COMMENT);
            value.setXslName(paramCache.get(KnownMdmParams.SHELF_LIFE_COMMENT).getXslName());
            value.setStrings(List.of(shelLifeComment));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        return TestDataUtils.wrapSilver(result);
    }

    /**
     * Чтобы не мучиться с заполнением SSKU, выдели по одному параметру под каждый интересный тип бизнес-мержа. На
     * них и будем проверяться.
     *
     * @return
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    private SilverCommonSsku silverSsku(ShopSkuKey key,
                                        String sourceId,
                                        MasterDataSourceType type,
                                        Instant updatedTs,
                                        int boxCount, // по последнему
                                        int deliveryTime, // по сервисам
                                        List<String> countries,
                                        List<String> regNumbers, // уникальное объединение
                                        Long masterDataVersion) {
        List<SskuSilverParamValue> result = new ArrayList<>();
        if (boxCount > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.BOX_COUNT);
            value.setXslName(paramCache.get(KnownMdmParams.BOX_COUNT).getXslName());
            value.setNumeric(BigDecimal.valueOf(boxCount));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (deliveryTime > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.DELIVERY_TIME);
            value.setXslName(paramCache.get(KnownMdmParams.DELIVERY_TIME).getXslName());
            value.setNumeric(BigDecimal.valueOf(deliveryTime));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (countries.size() > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.MANUFACTURER_COUNTRY);
            value.setXslName(paramCache.get(KnownMdmParams.MANUFACTURER_COUNTRY).getXslName());
            value.setStrings(countries);
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (regNumbers.size() > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.DOCUMENT_REG_NUMBER);
            value.setXslName(paramCache.get(KnownMdmParams.DOCUMENT_REG_NUMBER).getXslName());
            value.setStrings(regNumbers);
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (masterDataVersion != null && masterDataVersion > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION)
                .setXslName(paramCache.get(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION).getXslName())
                .setNumeric(BigDecimal.valueOf(masterDataVersion));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        return TestDataUtils.wrapSilver(result);
    }

    private MasterData electrumSsku(ShopSkuKey key,
                                    int boxCount, // по последнему
                                    int deliveryTime, // по сервисам
                                    List<String> countries,
                                    List<String> regNumbers, // уникальное объединение
                                    Long masterDataVersion) {  // по последнему
        MasterData masterData = new MasterData();
        masterData.setShopSkuKey(key);
        if (boxCount > 0) {
            masterData.setBoxCount(boxCount);
        }
        if (deliveryTime > 0) {
            masterData.setDeliveryTime(deliveryTime);
        }
        if (countries.size() > 0) {
            masterData.setManufacturerCountries(countries);
        }
        if (regNumbers.size() > 0) {
            masterData.addAllQualityDocuments(
                regNumbers.stream().map(this::generateQualityDocument).collect(Collectors.toList()));
        }
        if (masterDataVersion != null) {
            masterData.setDatacampMasterDataVersion(masterDataVersion);
        }
        return masterData;
    }

    private MasterData electrumSsku(ShopSkuKey key,
                                    int boxCount, // по последнему
                                    int deliveryTime, // по сервисам
                                    List<String> countries,
                                    List<String> regNumbers) { // уникальное объединение
        return electrumSsku(key, boxCount, deliveryTime, countries, regNumbers, null);
    }

    private QualityDocument generateQualityDocument(String regNumber) {
        return new QualityDocument()
            .setId(Long.parseLong(regNumber))
            .setType(QualityDocument.QualityDocumentType.DECLARATION_OF_CONFORMITY)
            .setStartDate(LocalDate.now().minusYears(10))
            .setEndDate(LocalDate.now().plusYears(100))
            .setRegistrationNumber(regNumber);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
