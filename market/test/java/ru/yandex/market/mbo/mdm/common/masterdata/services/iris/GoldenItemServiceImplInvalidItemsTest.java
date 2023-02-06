package ru.yandex.market.mbo.mdm.common.masterdata.services.iris;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.SskuVerdictResult;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.VerdictFeature;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.VerdictGeneratorHelper;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

/**
 * Test cases of creating golden item where some of silver items are invalid.
 *
 * @author dmserebr
 * @date 11/09/2020
 */
public class GoldenItemServiceImplInvalidItemsTest extends GoldenItemServiceImplTestBase {
    @Before
    public void before() throws Exception {
        setUp();
    }

    @Test
    public void whenExistingItemIsInvalidShouldReplaceWithValidItem() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        mdmSupplierCachingService.refresh();

        int timestamp = 1000;
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(timestamp)) // bad value
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(timestamp))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(13000).setUpdatedTs(timestamp))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(timestamp));
        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, itemShippingUnit);

        referenceItemRepository.insert(new ReferenceItemWrapper());

        // correct value with lesser timestamp
        itemShippingUnit
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(timestamp - 10));

        MdmIrisPayload.Item updatedItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, itemShippingUnit);

        fromIrisItemRepository.insert(new FromIrisItemWrapper(updatedItem));
        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);

        processGoldComputation();

        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // new item information should become reference
        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).hasSize(1);

        ReferenceItemWrapper referenceItemWrapper = referenceItems.get(0);
        Assertions.assertThat(referenceItemWrapper.getMdmIdentifier()).isEqualTo(item.getItemId());

        MdmIrisPayload.Item referenceItem = referenceItemWrapper.getItem();
        Assertions.assertThat(referenceItem.getInformationCount()).isEqualTo(1);
        Assertions.assertThat(referenceItem.getInformation(0).getItemShippingUnit())
            .isEqualTo(itemShippingUnit.build());
    }

    @Test
    public void whenNewItemIgnoredAndOldHasNoShippingUnitInfoIsPreserved() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        mdmSupplierCachingService.refresh();

        int timestamp = 1000;
        MdmIrisPayload.ShippingUnit.Builder newShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(timestamp))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(timestamp))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(timestamp))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(0).setUpdatedTs(timestamp));
        MdmIrisPayload.Item newItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, newShippingUnit);

        MdmIrisPayload.Item oldItem = ItemWrapperTestUtil.createItem(key, MdmIrisPayload.MasterDataSource.MDM);

        referenceItemRepository.insert(new ReferenceItemWrapper(oldItem));
        fromIrisItemRepository.insert(new FromIrisItemWrapper(newItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);

        processGoldComputation();

        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // new item information should become reference
        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).hasSize(1);

        ReferenceItemWrapper referenceItemWrapper = referenceItems.get(0);
        Assertions.assertThat(referenceItemWrapper.getItem()).isEqualTo(oldItem);
    }

    @Test
    public void whenProcessNewItemShouldIgnoreInvalidShippingUnit() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        mdmSupplierCachingService.refresh();

        int timestamp = 1000;
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(timestamp))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(timestamp))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(13000).setUpdatedTs(timestamp))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(0).setUpdatedTs(timestamp)); // invalid
        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, itemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(item));
        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);

        processGoldComputation();

        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();
        Assertions.assertThat(referenceItemRepository.findAll()).isEmpty();
    }

    @Test
    public void whenProcessWarehouseItemWithZeroTareWeightShouldAddNewReferenceItem() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        mdmSupplierCachingService.refresh();

        // 1. insert warehouse item
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(1000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(1000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(1000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(1000))
            .setWeightTareMg(MdmIrisPayload.Int64Value.newBuilder().setValue(0).setUpdatedTs(1000));

        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, itemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(item));
        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);

        // first process should create golden item
       processGoldComputation();

        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // new item information should become reference
        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).hasSize(1);

        ReferenceItemWrapper referenceItemWrapper = referenceItems.get(0);
        Assertions.assertThat(referenceItemWrapper.getMdmIdentifier()).isEqualTo(item.getItemId());

        MdmIrisPayload.Item referenceItem = referenceItemWrapper.getItem();
        Assertions.assertThat(referenceItem.getInformationCount()).isEqualTo(1);
        Assertions.assertThat(referenceItem.getInformation(0).getItemShippingUnit())
            .isEqualTo(itemShippingUnit.clearWeightTareMg().build());
    }

    @Test
    public void whenProcessWarehouseItemWithZeroTareWeightShouldUpdateReferenceItem() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        mdmSupplierCachingService.refresh();

        // 1. insert supplier item
        MdmIrisPayload.ShippingUnit.Builder supplierItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(2000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(2000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(2000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(2000))
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(900).setUpdatedTs(2000));

        MdmIrisPayload.Item supplierItem = ItemWrapperTestUtil.createItem(key, MdmIrisPayload.MasterDataSource.SUPPLIER,
            supplierItemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(supplierItem));
        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);

        // first process should create golden item
        processGoldComputation();

        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // 2. insert warehouse item with zero weight
        MdmIrisPayload.ShippingUnit.Builder anotherWarehouseItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(13000).setUpdatedTs(1500))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(14000).setUpdatedTs(1500))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(15000).setUpdatedTs(1500))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1200).setUpdatedTs(1500))
            .setWeightTareMg(MdmIrisPayload.Int64Value.newBuilder().setValue(0).setUpdatedTs(1500))
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(0).setUpdatedTs(1500));

        MdmIrisPayload.Item warehouseItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            anotherWarehouseItemShippingUnit);
        fromIrisItemRepository.insertOrUpdate(new FromIrisItemWrapper(warehouseItem));
        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);

        // second process
        processGoldComputation();

        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // new item information should become reference
        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).hasSize(1);

        ReferenceItemWrapper referenceItemWrapper = referenceItems.get(0);
        Assertions.assertThat(referenceItemWrapper.getMdmIdentifier()).isEqualTo(warehouseItem.getItemId());

        MdmIrisPayload.Item referenceItem = referenceItemWrapper.getItem();
        Assertions.assertThat(referenceItem.getInformationCount()).isEqualTo(2);
        Assertions.assertThat(referenceItem.getInformation(0).getItemShippingUnit())
            .isEqualTo(anotherWarehouseItemShippingUnit.clearWeightTareMg().clearWeightNetMg().build());
        Assertions.assertThat(referenceItem.getInformation(1).getItemShippingUnit())
            .isEqualTo(supplierItemShippingUnit.clearLengthMicrometer().clearWidthMicrometer().
                clearHeightMicrometer().clearWeightGrossMg().build());
    }

    @Test
    public void whenProcessInvalidWarehouseItemWhenValidOneExistsShouldKeepOldWarehouseItem() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        mdmSupplierCachingService.refresh();
        MasterData masterData = createMasterData(key);
        masterDataRepository.insertOrUpdate(masterData);

        List<MasterData> masterDataListBefore = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListBefore).hasSize(1);
        Assertions.assertThat(masterDataListBefore.get(0)).isEqualTo(masterData);

        // 1. insert warehouse item
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(1000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(1000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(1000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(1000));

        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, itemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(item));
        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);

        // first process should create golden item
        processGoldComputation();

        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // 2. insert second warehouse item
        MdmIrisPayload.ShippingUnit.Builder anotherWarehouseItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(13000).setUpdatedTs(1500))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(14000).setUpdatedTs(1500))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(15000).setUpdatedTs(1500))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(0).setUpdatedTs(1500)); // invalid

        MdmIrisPayload.Item anotherWarehouseItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            anotherWarehouseItemShippingUnit);
        fromIrisItemRepository.insertOrUpdate(new FromIrisItemWrapper(anotherWarehouseItem));
        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);

        // second process
        processGoldComputation();

        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).hasSize(1);

        ReferenceItemWrapper referenceItemWrapper = referenceItems.get(0);
        Assertions.assertThat(referenceItemWrapper.getMdmIdentifier()).isEqualTo(item.getItemId());

        MdmIrisPayload.Item referenceItem = referenceItemWrapper.getItem();
        Assertions.assertThat(referenceItem.getInformationCount()).isEqualTo(0);
    }

    @Test
    public void whenProcessInvalidWarehouseItemWhenValidOneExistsWithoutSourceShouldKeepOldWarehouseItem() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        mdmSupplierCachingService.refresh();
        MasterData masterData = createMasterData(key);
        masterDataRepository.insertOrUpdate(masterData);

        List<MasterData> masterDataListBefore = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListBefore).hasSize(1);
        Assertions.assertThat(masterDataListBefore.get(0)).isEqualTo(masterData);

        // 1. insert warehouse item
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(1000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(1000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(1000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(1000));

        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, itemShippingUnit);
        item = item.toBuilder().setInformation(0, item.getInformation(0).toBuilder().clearSource().build()).build();
        fromIrisItemRepository.insert(new FromIrisItemWrapper(item));
        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);

        // first process should create golden item
        processGoldComputation();

        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // 2. insert second warehouse item
        MdmIrisPayload.ShippingUnit.Builder anotherWarehouseItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(13000).setUpdatedTs(1500))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(14000).setUpdatedTs(1500))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(15000).setUpdatedTs(1500))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(0).setUpdatedTs(1500)); // invalid

        MdmIrisPayload.Item anotherWarehouseItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            anotherWarehouseItemShippingUnit);
        fromIrisItemRepository.insertOrUpdate(new FromIrisItemWrapper(anotherWarehouseItem));
        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);

        // second process
        processGoldComputation();

        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).hasSize(1);

        ReferenceItemWrapper referenceItemWrapper = referenceItems.get(0);
        Assertions.assertThat(referenceItemWrapper.getMdmIdentifier()).isEqualTo(item.getItemId());

        MdmIrisPayload.Item referenceItem = referenceItemWrapper.getItem();
        Assertions.assertThat(referenceItem.getInformationCount()).isEqualTo(1);
        Assertions.assertThat(referenceItem.getInformation(0).getItemShippingUnit())
            .isEqualTo(itemShippingUnit.build());
    }

    @Test
    public void shouldIgnoreInvalidItemBasedOnCategorySettings() {
        ShopSkuKey key1 = new ShopSkuKey(SUPPLIER_ID, "42");
        ShopSkuKey key2 = new ShopSkuKey(SUPPLIER_ID, "43");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        mdmSupplierCachingService.refresh();

        mappingsCacheRepository.insert(new MappingCacheDao()
            .setModelKey(new ModelKey(CATEGORY_ID_1, 123L)).setShopSkuKey(key1).setUpdateStamp(1L));
        mappingsCacheRepository.insert(new MappingCacheDao()
            .setModelKey(new ModelKey(CATEGORY_ID_2, 1234L)).setShopSkuKey(key2).setUpdateStamp(2L));

        // category 1 has size max setting, category 2 does not
        CategoryParamValue sizeMaxParamValue = new CategoryParamValue();
        sizeMaxParamValue.setCategoryId(CATEGORY_ID_1);
        sizeMaxParamValue.setMdmParamId(KnownMdmParams.SIZE_LONG_MAX_CM);
        sizeMaxParamValue.setNumeric(new BigDecimal(100));
        categoryParamValueRepository.insert(sizeMaxParamValue);

        // shipping unit is invalid in category 1, but valid globally (and in category 2 which does not have limits)
        MdmIrisPayload.ShippingUnit.Builder shippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            120.0, 80.0, 40.0, 20.0, null, null, 1000);
        MdmIrisPayload.Item item1 = ItemWrapperTestUtil.createItem(key1, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            shippingUnit);
        MdmIrisPayload.Item item2 = ItemWrapperTestUtil.createItem(key2, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            shippingUnit);
        fromIrisItemRepository.insertOrUpdateAll(List.of(
            new FromIrisItemWrapper(item1), new FromIrisItemWrapper(item2)));
        sskuToRefreshRepository.enqueueAll(List.of(key1, key2), MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(2);

        storageKeyValueService.putValue(MdmProperties.CREATE_FORBIDDING_VERDICTS_ON_EMPTY_SHIPPING_UNIT, true);
        storageKeyValueService.invalidateCache();

        processGoldComputation();

        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        List<ReferenceItemWrapper> referenceItems1 = referenceItemRepository.findByIds(List.of(key1));
        Assertions.assertThat(referenceItems1).hasSize(0);
        // no information as no valid shipping units present

        List<ReferenceItemWrapper> referenceItems2 = referenceItemRepository.findByIds(List.of(key2));
        Assertions.assertThat(referenceItems2).hasSize(1);
        Assertions.assertThat(referenceItems2.get(0).getMdmIdentifier()).isEqualTo(item2.getItemId());
        Assertions.assertThat(referenceItems2.get(0).getItem().getInformationCount()).isEqualTo(1);
        Assertions.assertThat(referenceItems2.get(0).getItem().getInformation(0).getItemShippingUnit())
            .isEqualTo(shippingUnit.build());

        // check that verdict is written to DB
        Map<ShopSkuKey, SskuVerdictResult> verdicts = sskuGoldenVerdictRepository
            .findByIds(List.of(key1, key2)).stream()
            .collect(Collectors.toMap(SskuVerdictResult::getKey, Function.identity()));
        Assertions.assertThat(verdicts).hasSize(2);

        Assertions.assertThat(verdicts.get(key1).isValid()).isFalse();
        Assertions.assertThat(verdicts.get(key1).getSingleVerdictResults().keySet())
            .containsExactlyInAnyOrder(VerdictFeature.UNSPECIFIED);
        Assertions.assertThat(verdicts.get(key1).getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.UNSPECIFIED,
                List.of(
                    MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.MANUFACTURER_COUNTRY),
                    MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.BOX_DIMENSIONS),
                    MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.WEIGHT_GROSS)
                )));

        Assertions.assertThat(verdicts.get(key2).isValid()).isFalse();
        Assertions.assertThat(verdicts.get(key2).getSingleVerdictResults().keySet())
            .containsExactlyInAnyOrder(VerdictFeature.UNSPECIFIED);
        Assertions.assertThat(verdicts.get(key2).getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.UNSPECIFIED,
                List.of(MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.MANUFACTURER_COUNTRY))));
    }

    @Test
    public void whenShouldIgnoreSupplierItemMarkedAsInvalid() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        mdmSupplierCachingService.refresh();
        MasterData masterData = TestDataUtils.generateMasterData(key, random);
        masterData.setItemShippingUnit(null);
        masterData.setGoldenItemShippingUnit(null);
        masterData.setGoldenRsl(null);
        masterData.setSurplusHandleMode(null);
        masterData.setCisHandleMode(null);
        masterData.setMeasurementState(null);
        masterDataRepository.insertOrUpdate(masterData);

        List<MasterData> masterDataListBefore = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListBefore).hasSize(1);
        Assertions.assertThat(masterDataListBefore.get(0)).isEqualTo(masterData);

        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(1000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(1000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(1000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(1000))
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(900).setUpdatedTs(1000));
        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.SUPPLIER, itemShippingUnit);

        var fromIrisItemWrapper = new FromIrisItemWrapper(item);
        fromIrisItemWrapper.setValidationErrorCount(1);
        fromIrisItemRepository.insert(fromIrisItemWrapper);
        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);

        processGoldComputation();

        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // no reference item created as no valid iris items present
        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).isEmpty();
    }

    @Test
    public void testWriteInvalidVerdictIfMasterDataIsWithoutCountry() {
        ShopSkuKey key1 = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        mdmSupplierCachingService.refresh();
        MdmIrisPayload.ShippingUnit.Builder shippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            10.0, 10.0, 10.0, 2.0, null, null, 1000);
        MdmIrisPayload.Item item1 = ItemWrapperTestUtil.createItem(key1, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            shippingUnit);
        fromIrisItemRepository.insertOrUpdateAll(List.of(new FromIrisItemWrapper(item1)));
        sskuToRefreshRepository.enqueue(key1, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);

        // invalid master data (without country)
        masterDataRepository.insert(new MasterData().setShopSkuKey(key1));

        storageKeyValueService.putValue(MdmProperties.CALCULATE_SSKU_GOLDEN_VERDICTS_WITH_MASTER_DATA_KEY, true);
        storageKeyValueService.invalidateCache();

        processGoldComputation();

        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(List.of(key1));
        Assertions.assertThat(referenceItems).hasSize(1);
        Assertions.assertThat(referenceItems.get(0).getMdmIdentifier()).isEqualTo(item1.getItemId());
        Assertions.assertThat(referenceItems.get(0).getItem().getInformationCount()).isEqualTo(1);
        Assertions.assertThat(referenceItems.get(0).getItem().getInformation(0).getItemShippingUnit())
            .isEqualTo(shippingUnit.build());

        // check that verdict is written to DB - and is forbidding due to missing country
        List<SskuVerdictResult> verdicts = sskuGoldenVerdictRepository.findByIds(List.of(key1));
        Assertions.assertThat(verdicts).hasSize(1);
        Assertions.assertThat(verdicts.get(0).getKey()).isEqualTo(key1);
        Assertions.assertThat(verdicts.get(0).isValid()).isFalse();
        Assertions.assertThat(verdicts.get(0).getSingleVerdictResults().keySet())
            .containsExactlyInAnyOrder(VerdictFeature.UNSPECIFIED);
        Assertions.assertThat(verdicts.get(0).getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.UNSPECIFIED,
                List.of(MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.MANUFACTURER_COUNTRY))));
    }
}
