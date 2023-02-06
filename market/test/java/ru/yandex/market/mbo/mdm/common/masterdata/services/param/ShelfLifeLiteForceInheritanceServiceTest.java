package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuGoldenParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendToDatacampQRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.GoldenItemServiceImplTestBase;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class ShelfLifeLiteForceInheritanceServiceTest extends GoldenItemServiceImplTestBase {

    private static final Integer CATEGORY_ID = 1;
    private static final Long MSKU_ID = 13L;
    private static final String SSKU = "SSKU";
    private static final Integer SUPPLIER_ID = 1313;
    private static final Integer SUPPLIER_ID2 = 1314;
    private static final Integer BUSINESS_ID = 1414;
    private static final ShopSkuKey SHOP_SKU1 = new ShopSkuKey(SUPPLIER_ID, SSKU);
    private static final MasterDataSource INHERITED_MASTER_DATA_SOURCE =
        new MasterDataSource(MasterDataSourceType.MSKU_INHERIT, MasterDataSourceType.addMskuSourcePrefix(MSKU_ID,
            "145"));

    @Autowired
    private SendToDatacampQRepository sendToDatacampQRepository;
    @Autowired
    private MskuToRefreshRepository mskuToRefreshRepository;
    @Autowired
    private BeruId beruId;
    @Autowired
    private MdmParamCache mdmParamCache;

    private CommonMsku msku;
    private MappingCacheDao mapping;
    private MdmSupplier default3pSupplier;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        msku = createMsku();
        mapping = createMapping(SHOP_SKU1);
        default3pSupplier = createSupplier(SUPPLIER_ID, MdmSupplierType.THIRD_PARTY, true);
        sskuExistenceRepository.clearRepository();
        sskuExistenceRepository.markExistence(SHOP_SKU1, true);

        goldSskuRepository.deleteAllSskus();
        sskuToRefreshRepository.deleteAll();
        masterDataRepository.deleteAll();
        storageKeyValueService.invalidateCache();
    }

    @Test
    public void whenShelfLifeInheritanceEnabledCorrectlyInherit() {
        sskuToRefreshRepository.enqueue(SHOP_SKU1, MdmEnqueueReason.CHANGED_MSKU_DATA);
        processGoldComputation();

        List<SskuGoldenParamValue> gold = findAllGoldenParamValues();
        Assertions.assertThat(masterDataRepository.findAll()).isEmpty();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedBatch(1000)).isEmpty();
        Assertions.assertThat(sendToDatacampQRepository.findAll().size()).isOne();
        Assertions.assertThat(sendToDatacampQRepository.findAll().get(0).getEntityKey()).isEqualTo(SHOP_SKU1);
        Assertions.assertThat(mskuToRefreshRepository.findAll()).isEmpty();
        Assertions.assertThat(gold).hasSize(3);
        Assertions.assertThat(gold)
            .usingElementComparatorIgnoringFields("modificationInfo")
            .containsExactlyInAnyOrderElementsOf(msku.getValues().stream()
                .map(pv -> {
                    var result = new SskuGoldenParamValue().setShopSkuKey(SHOP_SKU1);
                    pv.copyTo(result);
                    return (SskuGoldenParamValue) result;
                })
                .collect(Collectors.toList()));
        Assertions.assertThat(gold)
            .map(MdmParamValue::getMasterDataSource)
            .allMatch(INHERITED_MASTER_DATA_SOURCE::equals);
    }

    @Test
    public void whenInheritNewBlockShouldOverwriteCompletely() {
        sskuToRefreshRepository.enqueue(SHOP_SKU1, MdmEnqueueReason.CHANGED_MSKU_DATA);
        processGoldComputation();

        List<SskuGoldenParamValue> gold = findAllGoldenParamValues();
        Assertions.assertThat(masterDataRepository.findAll()).isEmpty();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedBatch(1000)).isEmpty();
        Assertions.assertThat(sendToDatacampQRepository.findAll().size()).isOne();
        Assertions.assertThat(sendToDatacampQRepository.findAll().get(0).getEntityKey()).isEqualTo(SHOP_SKU1);
        Assertions.assertThat(mskuToRefreshRepository.findAll()).isEmpty();
        Assertions.assertThat(gold).hasSize(3);
        Assertions.assertThat(gold)
            .usingElementComparatorIgnoringFields("modificationInfo")
            .containsExactlyInAnyOrderElementsOf(msku.getValues().stream()
                .map(pv -> {
                    var result = new SskuGoldenParamValue().setShopSkuKey(SHOP_SKU1);
                    pv.copyTo(result);
                    return (SskuGoldenParamValue) result;
                })
                .collect(Collectors.toList()));
        Assertions.assertThat(gold)
            .map(MdmParamValue::getMasterDataSource)
            .allMatch(INHERITED_MASTER_DATA_SOURCE::equals);

        // overwrite data
        var updatedMsku =
            msku.clearParamValues()
                .addParamValue((MskuParamValue) new MskuParamValue()
                    .setMskuId(MSKU_ID)
                    .setXslName(mdmParamCache.get(KnownMdmParams.SHELF_LIFE_UNIT).getXslName())
                    .setMdmParamId(KnownMdmParams.SHELF_LIFE_UNIT)
                    .setOption(new MdmParamOption(1).setRenderedValue("дни"))
                    .setMasterDataSourceType(MasterDataSourceType.MEASUREMENT)
                    .setMasterDataSourceId("152"))
                .addParamValue((MskuParamValue) new MskuParamValue()
                    .setMskuId(MSKU_ID)
                    .setXslName(mdmParamCache.get(KnownMdmParams.SHELF_LIFE).getXslName())
                    .setMdmParamId(KnownMdmParams.SHELF_LIFE)
                    .setMasterDataSourceType(MasterDataSourceType.MEASUREMENT)
                    .setMasterDataSourceId("152")
                    .setNumeric(BigDecimal.valueOf(12)));
        mskuRepository.insertOrUpdateMsku(updatedMsku);

        sskuToRefreshRepository.enqueue(SHOP_SKU1, MdmEnqueueReason.CHANGED_MSKU_DATA);
        processGoldComputation();

        var expectedMdSource = new MasterDataSource(
            MasterDataSourceType.MSKU_INHERIT,
            MasterDataSourceType.addMskuSourcePrefix(MSKU_ID, "152")
        );
        gold = findAllGoldenParamValues();
        Assertions.assertThat(masterDataRepository.findAll()).isEmpty();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedBatch(1000)).isEmpty();
        Assertions.assertThat(sendToDatacampQRepository.findAll().size()).isOne();
        Assertions.assertThat(sendToDatacampQRepository.findAll().get(0).getEntityKey()).isEqualTo(SHOP_SKU1);
        Assertions.assertThat(mskuToRefreshRepository.findAll()).isEmpty();
        Assertions.assertThat(gold).hasSize(2);
        Assertions.assertThat(gold)
            .usingElementComparatorIgnoringFields("modificationInfo")
            .containsExactlyInAnyOrderElementsOf(updatedMsku.getValues().stream().map(pv -> {
                    var result = new SskuGoldenParamValue().setShopSkuKey(SHOP_SKU1);
                    pv.copyTo(result);
                    return (SskuGoldenParamValue) result;
                })
                .collect(Collectors.toList()));
        Assertions.assertThat(gold)
            .map(MdmParamValue::getMasterDataSource)
            .allMatch(expectedMdSource::equals);
    }

    @Test
    public void whenInheritNewBlockShouldRewriteOnlyIfNew() {
        sskuToRefreshRepository.enqueue(SHOP_SKU1, MdmEnqueueReason.CHANGED_MSKU_DATA);
        processGoldComputation();

        List<SskuGoldenParamValue> gold = findAllGoldenParamValues();
        Assertions.assertThat(masterDataRepository.findAll()).isEmpty();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedBatch(1000)).isEmpty();
        Assertions.assertThat(sendToDatacampQRepository.findAll().size()).isOne();
        Assertions.assertThat(sendToDatacampQRepository.findAll().get(0).getEntityKey()).isEqualTo(SHOP_SKU1);
        Assertions.assertThat(mskuToRefreshRepository.findAll()).isEmpty();
        Assertions.assertThat(gold).hasSize(3);
        Assertions.assertThat(gold)
            .usingElementComparatorIgnoringFields("modificationInfo")
            .containsExactlyInAnyOrderElementsOf(msku.getValues().stream()
                .map(pv -> {
                    var result = new SskuGoldenParamValue().setShopSkuKey(SHOP_SKU1);
                    pv.copyTo(result);
                    return (SskuGoldenParamValue) result;
                })
                .collect(Collectors.toList()));
        Assertions.assertThat(gold)
            .map(MdmParamValue::getMasterDataSource)
            .allMatch(INHERITED_MASTER_DATA_SOURCE::equals);

        // try update inherited data
        sskuToRefreshRepository.enqueue(SHOP_SKU1, MdmEnqueueReason.CHANGED_MSKU_DATA);
        processGoldComputation();

        List<SskuGoldenParamValue> newGold = findAllGoldenParamValues();
        Assertions.assertThat(masterDataRepository.findAll()).isEmpty();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedBatch(1000)).isEmpty();
        Assertions.assertThat(sendToDatacampQRepository.findAll().size()).isOne();
        Assertions.assertThat(sendToDatacampQRepository.findAll().get(0).getEntityKey()).isEqualTo(SHOP_SKU1);
        Assertions.assertThat(mskuToRefreshRepository.findAll()).isEmpty();
        Assertions.assertThat(newGold).hasSize(3);
        Assertions.assertThat(newGold)
            .containsExactlyInAnyOrderElementsOf(gold);
        Assertions.assertThat(newGold.get(0).getUpdatedTs()).isEqualTo(gold.get(0).getUpdatedTs());
    }

    @Test
    public void whenInheritShouldCorrectlyInheritOnSeveralSskus() {
        // add 1 more mapping
        createSupplier(SUPPLIER_ID2, MdmSupplierType.THIRD_PARTY, true);
        createSupplier(BUSINESS_ID, MdmSupplierType.BUSINESS, false);
        mdmSupplierCachingService.refresh();
        var key2 = new ShopSkuKey(SUPPLIER_ID2, SSKU);
        var bizKey = new ShopSkuKey(BUSINESS_ID, SSKU);
        createMapping(key2);
        createMapping(bizKey);
        sskuExistenceRepository.markExistence(key2, true);

        sskuToRefreshRepository.enqueue(bizKey, MdmEnqueueReason.CHANGED_MSKU_DATA);
        processGoldComputation();

        List<SskuGoldenParamValue> gold = findAllGoldenParamValues();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedBatch(1000)).isEmpty();
        Assertions.assertThat(gold).hasSize(3);
        Assertions.assertThat(gold)
            .usingElementComparatorIgnoringFields("modificationInfo")
            .containsExactlyInAnyOrderElementsOf(msku.getValues().stream()
                .flatMap(pv -> {
                    var result1 = new SskuGoldenParamValue().setShopSkuKey(bizKey);
                    pv.copyTo(result1);
                    return Stream.of((SskuGoldenParamValue) result1);
                })
                .collect(Collectors.toList()));
        Assertions.assertThat(gold)
            .map(MdmParamValue::getMasterDataSource)
            .allMatch(INHERITED_MASTER_DATA_SOURCE::equals);
    }

    @Test
    public void whenShelfLifeInheritanceEnabledCorrectlyInheritOn2ndStage() {
        createSupplier(beruId.getId(), MdmSupplierType.FIRST_PARTY, false);
        var key = new ShopSkuKey(beruId.getId(), SSKU + "." + SSKU);
        createMapping(key);

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_MSKU_DATA);
        processGoldComputation();

        List<SskuGoldenParamValue> gold = findAllGoldenParamValues();
        Assertions.assertThat(masterDataRepository.findAll()).isEmpty();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedBatch(1000)).isEmpty();
        Assertions.assertThat(sendToDatacampQRepository.findAll().size()).isOne();
        Assertions.assertThat(sendToDatacampQRepository.findAll().get(0).getEntityKey()).isEqualTo(key);
        Assertions.assertThat(mskuToRefreshRepository.findAll()).isEmpty();
        Assertions.assertThat(gold).hasSize(3);
        Assertions.assertThat(gold)
            .usingElementComparatorIgnoringFields("modificationInfo")
            .containsExactlyInAnyOrderElementsOf(msku.getValues().stream()
                .map(pv -> {
                    var result = new SskuGoldenParamValue().setShopSkuKey(key);
                    pv.copyTo(result);
                    return (SskuGoldenParamValue) result;
                })
                .collect(Collectors.toList()));
        Assertions.assertThat(gold)
            .map(MdmParamValue::getMasterDataSource)
            .allMatch(INHERITED_MASTER_DATA_SOURCE::equals);
    }

    @Test
    public void whenInheritShouldCorrectlyInheritOnSeveralSskus2() {
        // create 2 stage
        createSupplier(beruId.getId(), MdmSupplierType.FIRST_PARTY, false);
        var key = new ShopSkuKey(beruId.getId(), SSKU + "." + SSKU);
        createMapping(key);
        // create biz key
        createSupplier(BUSINESS_ID, MdmSupplierType.BUSINESS, false);
        var bizKey = new ShopSkuKey(BUSINESS_ID, SSKU);
        createMapping(bizKey);
        mdmSupplierCachingService.refresh();

        sskuToRefreshRepository.enqueueAll(List.of(key, bizKey), MdmEnqueueReason.CHANGED_MSKU_DATA);
        processGoldComputation();

        List<SskuGoldenParamValue> gold = findAllGoldenParamValues();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedBatch(1000)).isEmpty();
        Assertions.assertThat(gold).hasSize(6);
        Assertions.assertThat(gold)
            .usingElementComparatorIgnoringFields("modificationInfo")
            .containsExactlyInAnyOrderElementsOf(msku.getValues().stream()
                .flatMap(pv -> {
                    var result1 = new SskuGoldenParamValue().setShopSkuKey(bizKey);
                    pv.copyTo(result1);
                    var result2 = new SskuGoldenParamValue().setShopSkuKey(key);
                    pv.copyTo(result2);
                    return Stream.of((SskuGoldenParamValue) result1, (SskuGoldenParamValue) result2);
                })
                .collect(Collectors.toList()));
        Assertions.assertThat(gold)
            .map(MdmParamValue::getMasterDataSource)
            .allMatch(INHERITED_MASTER_DATA_SOURCE::equals);
    }

    private MdmSupplier createSupplier(Integer supplierId, MdmSupplierType type, boolean businessEnabled) {
        var supplier = new MdmSupplier()
            .setId(supplierId)
            .setType(type)
            .setDeleted(false)
            .setBusinessEnabled(businessEnabled)
            .setBusinessId(BUSINESS_ID);
        mdmSupplierRepository.insertOrUpdate(supplier);
        return supplier;
    }

    private MappingCacheDao createMapping(ShopSkuKey key) {
        var result = new MappingCacheDao()
            .setMskuId(MSKU_ID)
            .setCategoryId(CATEGORY_ID)
            .setShopSkuKey(key);
        mappingsCacheRepository.insert(result);
        return result;
    }

    private CommonMsku createMsku() {
        var result = new CommonMsku(CATEGORY_ID, MSKU_ID)
            .setParamValues(createMskuParamValues(MSKU_ID).stream()
                .collect(Collectors.toMap(MskuParamValue::getMdmParamId, Function.identity())));
        mskuRepository.insertOrUpdateMsku(result);
        return result;
    }

    private List<MskuParamValue> createMskuParamValues(Long mskuId) {
        MskuParamValue value = (MskuParamValue) new MskuParamValue()
            .setMskuId(mskuId)
            .setXslName(mdmParamCache.get(KnownMdmParams.SHELF_LIFE).getXslName())
            .setMdmParamId(KnownMdmParams.SHELF_LIFE)
            .setMasterDataSourceType(MasterDataSourceType.MEASUREMENT)
            .setMasterDataSourceId("145")
            .setNumeric(BigDecimal.valueOf(10));
        MskuParamValue comment = (MskuParamValue) new MskuParamValue()
            .setMskuId(mskuId)
            .setXslName(mdmParamCache.get(KnownMdmParams.SHELF_LIFE_COMMENT).getXslName())
            .setMdmParamId(KnownMdmParams.SHELF_LIFE_COMMENT)
            .setString("abc")
            .setMasterDataSourceType(MasterDataSourceType.MEASUREMENT)
            .setMasterDataSourceId("145");
        MskuParamValue unit = (MskuParamValue) new MskuParamValue()
            .setMskuId(mskuId)
            .setXslName(mdmParamCache.get(KnownMdmParams.SHELF_LIFE_UNIT).getXslName())
            .setMdmParamId(KnownMdmParams.SHELF_LIFE_UNIT)
            .setOption(new MdmParamOption(1).setRenderedValue("дни"))
            .setMasterDataSourceType(MasterDataSourceType.MEASUREMENT)
            .setMasterDataSourceId("145");
        return List.of(value, comment, unit);
    }

    private List<SskuGoldenParamValue> findAllGoldenParamValues() {
        return goldSskuRepository.findAllSskus().stream()
            .map(CommonSsku::getBaseValues)
            .flatMap(List::stream)
            .map(SskuGoldenParamValue::fromSskuParamValue)
            .collect(Collectors.toList());
    }
}
