package ru.yandex.market.mbo.mdm.common.masterdata.services.iris;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.util.TimestampUtil;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

/**
 * @author dmserebr
 * @date 03/09/2020
 */
public class GoldenItemServiceImplTest extends GoldenItemServiceImplTestBase {
    @Before
    public void before() throws Exception {
        setUp();

        weightDimensionsService.setReplaceWarehouseWithSupplierFromDefaultTs(
            TimestampUtil.toInstant(LocalDateTime.of(3000, 1, 1, 0, 0)));
        storageKeyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
    }

    @Test
    public void whenProcessEmptyItemShouldNotChangeMasterData() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        MasterData masterData = createMasterData(key);
        masterDataRepository.insertOrUpdate(masterData);

        List<MasterData> masterDataListBefore = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListBefore).hasSize(1);
        MasterData masterDataBefore = masterDataListBefore.get(0);
        Assertions.assertThat(masterDataBefore).isEqualTo(masterData);

        // item without shipping units
        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(key, MdmIrisPayload.MasterDataSource.WAREHOUSE);

        fromIrisItemRepository.insert(new FromIrisItemWrapper(item));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        List<MasterData> masterDataListAfter = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListAfter).hasSize(1);
        MasterData masterDataAfter = masterDataListAfter.get(0);
        Assertions.assertThat(masterDataAfter).isEqualTo(masterData);

        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).isEmpty();
    }

    @Test
    public void whenProcessUpdatedItemShouldUpdateMasterData() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));

        int timestamp = 1000;
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(timestamp))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(timestamp))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(timestamp))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(timestamp));
        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, itemShippingUnit);

        referenceItemRepository.insert(new ReferenceItemWrapper());

        itemShippingUnit
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(15000).setUpdatedTs(timestamp + 1));

        MdmIrisPayload.Item updatedItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, itemShippingUnit);

        fromIrisItemRepository.insert(new FromIrisItemWrapper(updatedItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
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
    public void whenProcessNewItemShouldAddItToReferenceData() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        MasterData masterData = createMasterData(key);
        masterDataRepository.insertOrUpdate(masterData);

        List<MasterData> masterDataListBefore = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListBefore).hasSize(1);
        Assertions.assertThat(masterDataListBefore.get(0)).isEqualTo(masterData);

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

        // master_data should not be affected
        List<MasterData> masterDataListAfter = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListAfter).hasSize(1);
        MasterData masterDataAfter = masterDataListAfter.get(0);
        Assertions.assertThat(masterDataAfter).isEqualTo(masterData);
    }

    @Test
    public void whenProcessMdmItemShouldAddItAsWell() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        MasterData masterData = createMasterData(key);
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
            key, MdmIrisPayload.MasterDataSource.MDM, itemShippingUnit);

        fromIrisItemRepository.insert(new FromIrisItemWrapper(item));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
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

        // master_data should not be affected
        List<MasterData> masterDataListAfter = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListAfter).hasSize(1);
        MasterData masterDataAfter = masterDataListAfter.get(0);
        Assertions.assertThat(masterDataAfter).isEqualTo(masterData);
    }

    @Test
    public void whenProcessMdmItemWithMeasurementShouldAddItWithFlagValueTrue() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        MasterData masterData = createMasterData(key);
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
            key, MdmIrisPayload.MasterDataSource.MEASUREMENT, itemShippingUnit);

        fromIrisItemRepository.insert(new FromIrisItemWrapper(item));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
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
        Assertions.assertThat(referenceItem.getMeasurementState().getIsMeasured())
            .isEqualTo(true);

        // master_data should not be affected
        List<MasterData> masterDataListAfter = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListAfter).hasSize(1);
        MasterData masterDataAfter = masterDataListAfter.get(0);
        Assertions.assertThat(masterDataAfter).isEqualTo(masterData);
    }

    @Test
    public void whenProcessSupplierItemShouldAddItAsWell() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        MasterData masterData = createMasterData(key);
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

        fromIrisItemRepository.insert(new FromIrisItemWrapper(item));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
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

        // master_data should not be affected
        List<MasterData> masterDataListAfter = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListAfter).hasSize(1);
        MasterData masterDataAfter = masterDataListAfter.get(0);
        Assertions.assertThat(masterDataAfter).isEqualTo(masterData);
    }

    @Test
    public void whenProcessSupplierItemWhenMdmItemExistsShouldIgnoreIt() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        MasterData masterData = createMasterData(key);
        masterDataRepository.insertOrUpdate(masterData);

        List<MasterData> masterDataListBefore = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListBefore).hasSize(1);
        Assertions.assertThat(masterDataListBefore.get(0)).isEqualTo(masterData);

        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(1000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(1000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(1000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(1000));

        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.MDM, itemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(item));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        MdmIrisPayload.ShippingUnit.Builder supplierItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(13000).setUpdatedTs(2000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(14000).setUpdatedTs(2000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(15000).setUpdatedTs(2000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(2000).setUpdatedTs(2000));

        MdmIrisPayload.Item supplierItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.SUPPLIER,
            supplierItemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(supplierItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
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

        // master_data should not be affected
        List<MasterData> masterDataListAfter = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListAfter).hasSize(1);
        MasterData masterDataAfter = masterDataListAfter.get(0);
        Assertions.assertThat(masterDataAfter).isEqualTo(masterData);
    }

    @Test
    public void whenProcessWarehouseItemWhenMdmItemExistsShouldOverwriteIt() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        MasterData masterData = createMasterData(key);
        masterDataRepository.insertOrUpdate(masterData);

        List<MasterData> masterDataListBefore = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListBefore).hasSize(1);
        Assertions.assertThat(masterDataListBefore.get(0)).isEqualTo(masterData);

        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(1000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(1000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(1000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(1000));

        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.MDM, "", itemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(item));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        MdmIrisPayload.ShippingUnit.Builder whItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(13000).setUpdatedTs(2000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(14000).setUpdatedTs(2000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(15000).setUpdatedTs(2000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(2000).setUpdatedTs(2000));

        MdmIrisPayload.Item whItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            whItemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(whItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
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
            .isEqualTo(whItemShippingUnit.build());
        Assertions.assertThat(referenceItem.getInformation(0).getSource().getType())
            .isEqualTo(MdmIrisPayload.MasterDataSource.WAREHOUSE);

        // master_data should not be affected
        List<MasterData> masterDataListAfter = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListAfter).hasSize(1);
        MasterData masterDataAfter = masterDataListAfter.get(0);
        Assertions.assertThat(masterDataAfter).isEqualTo(masterData);
    }

    @Test
    public void whenProcessMdmItemWhenWarehouseItemExistsShouldOverwriteIt() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        MasterData masterData = createMasterData(key);
        masterDataRepository.insertOrUpdate(masterData);

        List<MasterData> masterDataListBefore = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListBefore).hasSize(1);
        Assertions.assertThat(masterDataListBefore.get(0)).isEqualTo(masterData);

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
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        MdmIrisPayload.ShippingUnit.Builder mdmItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(13000).setUpdatedTs(2000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(14000).setUpdatedTs(2000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(15000).setUpdatedTs(2000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(2000).setUpdatedTs(2000));

        MdmIrisPayload.Item mdmItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.MDM,
            mdmItemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(mdmItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
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
            .isEqualTo(mdmItemShippingUnit.build());
        Assertions.assertThat(referenceItem.getInformation(0).getSource().getType())
            .isEqualTo(MdmIrisPayload.MasterDataSource.MDM);

        // master_data should not be affected
        List<MasterData> masterDataListAfter = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListAfter).hasSize(1);
        MasterData masterDataAfter = masterDataListAfter.get(0);
        Assertions.assertThat(masterDataAfter).isEqualTo(masterData);
    }

    @Test
    public void whenProcessSupplierItemWhenWarehouseItemExistsShouldIgnoreIt() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        MasterData masterData = createMasterData(key);
        masterDataRepository.insertOrUpdate(masterData);

        List<MasterData> masterDataListBefore = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListBefore).hasSize(1);
        Assertions.assertThat(masterDataListBefore.get(0)).isEqualTo(masterData);

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
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        MdmIrisPayload.ShippingUnit.Builder supplierItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(13000).setUpdatedTs(2000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(14000).setUpdatedTs(2000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(15000).setUpdatedTs(2000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(2000).setUpdatedTs(2000));

        MdmIrisPayload.Item supplierItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.SUPPLIER,
            supplierItemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(supplierItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
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

        // master_data should not be affected
        List<MasterData> masterDataListAfter = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListAfter).hasSize(1);
        MasterData masterDataAfter = masterDataListAfter.get(0);
        Assertions.assertThat(masterDataAfter).isEqualTo(masterData);
    }

    @Test
    public void whenProcessRichSupplierItemWhenMdmItemExistsShouldEnrichIt() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        MasterData masterData = createMasterData(key);
        masterDataRepository.insertOrUpdate(masterData);

        List<MasterData> masterDataListBefore = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListBefore).hasSize(1);
        Assertions.assertThat(masterDataListBefore.get(0)).isEqualTo(masterData);

        MdmIrisPayload.ShippingUnit.Builder mdmShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(1000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(1000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(1000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(1000));

        MdmIrisPayload.Item mdmItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.MDM, mdmShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(mdmItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        MdmIrisPayload.ShippingUnit.Builder supplierItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(13000).setUpdatedTs(2000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(14000).setUpdatedTs(2000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(15000).setUpdatedTs(2000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1200).setUpdatedTs(2000))
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(800).setUpdatedTs(2000));

        MdmIrisPayload.Item supplierItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.SUPPLIER,
            supplierItemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(supplierItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // new item information should become reference
        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).hasSize(1);

        ReferenceItemWrapper referenceItemWrapper = referenceItems.get(0);
        Assertions.assertThat(referenceItemWrapper.getMdmIdentifier()).isEqualTo(mdmItem.getItemId());

        MdmIrisPayload.Item referenceItem = referenceItemWrapper.getItem();
        Assertions.assertThat(referenceItem.getInformationCount()).isEqualTo(2);
        Assertions.assertThat(referenceItem.getInformation(0).getItemShippingUnit())
            .isEqualTo(mdmShippingUnit.build());
        Assertions.assertThat(referenceItem.getInformation(1).getItemShippingUnit())
            .isEqualTo(supplierItemShippingUnit.clearLengthMicrometer().clearWidthMicrometer().
                clearHeightMicrometer().clearWeightGrossMg().build());

        // master_data should not be affected
        List<MasterData> masterDataListAfter = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListAfter).hasSize(1);
        MasterData masterDataAfter = masterDataListAfter.get(0);
        Assertions.assertThat(masterDataAfter).isEqualTo(masterData);
    }

    @Test
    public void whenProcessRichWarehouseItemWhenMdmItemExistsShouldOverwriteIt() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        MasterData masterData = createMasterData(key);
        masterDataRepository.insertOrUpdate(masterData);

        List<MasterData> masterDataListBefore = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListBefore).hasSize(1);
        Assertions.assertThat(masterDataListBefore.get(0)).isEqualTo(masterData);

        MdmIrisPayload.ShippingUnit.Builder mdmShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(1000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(1000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(1000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(1000));

        MdmIrisPayload.Item mdmItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.MDM, "", mdmShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(mdmItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        MdmIrisPayload.ShippingUnit.Builder whItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(13000).setUpdatedTs(2000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(14000).setUpdatedTs(2000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(15000).setUpdatedTs(2000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1200).setUpdatedTs(2000))
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(800).setUpdatedTs(2000));

        MdmIrisPayload.Item whItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            whItemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(whItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // new item information should become reference
        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).hasSize(1);

        ReferenceItemWrapper referenceItemWrapper = referenceItems.get(0);
        Assertions.assertThat(referenceItemWrapper.getMdmIdentifier()).isEqualTo(mdmItem.getItemId());

        MdmIrisPayload.Item referenceItem = referenceItemWrapper.getItem();
        Assertions.assertThat(referenceItem.getInformationCount()).isEqualTo(1);
        Assertions.assertThat(referenceItem.getInformation(0).getItemShippingUnit())
            .isEqualTo(whItemShippingUnit.build());
        Assertions.assertThat(referenceItem.getInformation(0).getSource().getType())
            .isEqualTo(MdmIrisPayload.MasterDataSource.WAREHOUSE);

        // master_data should not be affected
        List<MasterData> masterDataListAfter = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListAfter).hasSize(1);
        MasterData masterDataAfter = masterDataListAfter.get(0);
        Assertions.assertThat(masterDataAfter).isEqualTo(masterData);
    }

    @Test
    public void whenProcessRichMdmItemWhenWarehouseItemExistsShouldOverwriteIt() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        MasterData masterData = createMasterData(key);
        masterDataRepository.insertOrUpdate(masterData);

        List<MasterData> masterDataListBefore = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListBefore).hasSize(1);
        Assertions.assertThat(masterDataListBefore.get(0)).isEqualTo(masterData);

        MdmIrisPayload.ShippingUnit.Builder whShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(1000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(1000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(1000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(1000));

        MdmIrisPayload.Item whItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, whShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(whItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        MdmIrisPayload.ShippingUnit.Builder mdmItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(13000).setUpdatedTs(2000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(14000).setUpdatedTs(2000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(15000).setUpdatedTs(2000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1200).setUpdatedTs(2000))
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(800).setUpdatedTs(2000));

        MdmIrisPayload.Item mdmItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.MDM,
            mdmItemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(mdmItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // new item information should become reference
        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).hasSize(1);

        ReferenceItemWrapper referenceItemWrapper = referenceItems.get(0);
        Assertions.assertThat(referenceItemWrapper.getMdmIdentifier()).isEqualTo(whItem.getItemId());

        MdmIrisPayload.Item referenceItem = referenceItemWrapper.getItem();
        Assertions.assertThat(referenceItem.getInformationCount()).isEqualTo(1);
        Assertions.assertThat(referenceItem.getInformation(0).getItemShippingUnit())
            .isEqualTo(mdmItemShippingUnit.build());
        Assertions.assertThat(referenceItem.getInformation(0).getSource().getType())
            .isEqualTo(MdmIrisPayload.MasterDataSource.MDM);

        // master_data should not be affected
        List<MasterData> masterDataListAfter = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListAfter).hasSize(1);
        MasterData masterDataAfter = masterDataListAfter.get(0);
        Assertions.assertThat(masterDataAfter).isEqualTo(masterData);
    }

    @Test
    public void whenProcessRichSupplierItemWhenWarehouseItemExistsShouldEnrichIt() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        MasterData masterData = createMasterData(key);
        masterDataRepository.insertOrUpdate(masterData);

        List<MasterData> masterDataListBefore = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListBefore).hasSize(1);
        Assertions.assertThat(masterDataListBefore.get(0)).isEqualTo(masterData);

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
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        MdmIrisPayload.ShippingUnit.Builder supplierItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(13000).setUpdatedTs(2000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(14000).setUpdatedTs(2000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(15000).setUpdatedTs(2000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1200).setUpdatedTs(2000))
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(800).setUpdatedTs(2000));

        MdmIrisPayload.Item supplierItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.SUPPLIER,
            supplierItemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(supplierItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // new item information should become reference
        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).hasSize(1);

        ReferenceItemWrapper referenceItemWrapper = referenceItems.get(0);
        Assertions.assertThat(referenceItemWrapper.getMdmIdentifier()).isEqualTo(item.getItemId());

        MdmIrisPayload.Item referenceItem = referenceItemWrapper.getItem();
        Assertions.assertThat(referenceItem.getInformationCount()).isEqualTo(2);
        Assertions.assertThat(referenceItem.getInformation(0).getItemShippingUnit())
            .isEqualTo(itemShippingUnit.build());
        Assertions.assertThat(referenceItem.getInformation(1).getItemShippingUnit())
            .isEqualTo(supplierItemShippingUnit.clearLengthMicrometer().clearWidthMicrometer().
                clearHeightMicrometer().clearWeightGrossMg().build());

        // master_data should not be affected
        List<MasterData> masterDataListAfter = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListAfter).hasSize(1);
        MasterData masterDataAfter = masterDataListAfter.get(0);
        Assertions.assertThat(masterDataAfter).isEqualTo(masterData);
    }

    @Test
    public void whenProcessSupplierItemWhenBothWarehouseAndSupplierItemExistShouldReplaceOldSupplierItem() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
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

        // 2. insert first supplier item
        MdmIrisPayload.ShippingUnit.Builder supplierItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(13000).setUpdatedTs(2000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(14000).setUpdatedTs(2000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(15000).setUpdatedTs(2000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1200).setUpdatedTs(2000))
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(800).setUpdatedTs(2000));

        MdmIrisPayload.Item supplierItem = ItemWrapperTestUtil.createItem(key, MdmIrisPayload.MasterDataSource.SUPPLIER,
            supplierItemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(supplierItem));

        // first process should create golden item
        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // 3. insert second supplier item
        MdmIrisPayload.ShippingUnit.Builder anotherSupplierItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1400).setUpdatedTs(3000))
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(600).setUpdatedTs(3000));

        MdmIrisPayload.Item anotherSupplierItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.SUPPLIER,
            anotherSupplierItemShippingUnit);
        fromIrisItemRepository.insertOrUpdate(new FromIrisItemWrapper(anotherSupplierItem));

        // second process
        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // new item information should become reference
        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).hasSize(1);

        ReferenceItemWrapper referenceItemWrapper = referenceItems.get(0);
        Assertions.assertThat(referenceItemWrapper.getMdmIdentifier()).isEqualTo(item.getItemId());

        MdmIrisPayload.Item referenceItem = referenceItemWrapper.getItem();
        Assertions.assertThat(referenceItem.getInformationCount()).isEqualTo(2);
        Assertions.assertThat(referenceItem.getInformation(0).getItemShippingUnit())
            .isEqualTo(itemShippingUnit.build());
        Assertions.assertThat(referenceItem.getInformation(1).getItemShippingUnit())
            .isEqualTo(anotherSupplierItemShippingUnit.clearLengthMicrometer().clearWidthMicrometer().
                clearHeightMicrometer().clearWeightGrossMg().build());

        // master_data should not be affected
        List<MasterData> masterDataListAfter = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListAfter).hasSize(1);
        MasterData masterDataAfter = masterDataListAfter.get(0);
        Assertions.assertThat(masterDataAfter).isEqualTo(masterData);
    }

    @Test
    public void whenProcessSupplierItemWhenBothMdmAndSupplierItemExistShouldReplaceOldSupplierItem() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
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
            key, MdmIrisPayload.MasterDataSource.MDM, itemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(item));

        // 2. insert first supplier item
        MdmIrisPayload.ShippingUnit.Builder supplierItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(13000).setUpdatedTs(2000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(14000).setUpdatedTs(2000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(15000).setUpdatedTs(2000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1200).setUpdatedTs(2000))
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(800).setUpdatedTs(2000));

        MdmIrisPayload.Item supplierItem = ItemWrapperTestUtil.createItem(key, MdmIrisPayload.MasterDataSource.SUPPLIER,
            supplierItemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(supplierItem));

        // first process should create golden item
        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // 3. insert second supplier item
        MdmIrisPayload.ShippingUnit.Builder anotherSupplierItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1400).setUpdatedTs(3000))
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(600).setUpdatedTs(3000));

        MdmIrisPayload.Item anotherSupplierItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.SUPPLIER,
            anotherSupplierItemShippingUnit);
        fromIrisItemRepository.insertOrUpdate(new FromIrisItemWrapper(anotherSupplierItem));

        // second process
        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // new item information should become reference
        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).hasSize(1);

        ReferenceItemWrapper referenceItemWrapper = referenceItems.get(0);
        Assertions.assertThat(referenceItemWrapper.getMdmIdentifier()).isEqualTo(item.getItemId());

        MdmIrisPayload.Item referenceItem = referenceItemWrapper.getItem();
        Assertions.assertThat(referenceItem.getInformationCount()).isEqualTo(2);
        Assertions.assertThat(referenceItem.getInformation(0).getItemShippingUnit())
            .isEqualTo(itemShippingUnit.build());
        Assertions.assertThat(referenceItem.getInformation(1).getItemShippingUnit())
            .isEqualTo(anotherSupplierItemShippingUnit.clearLengthMicrometer().clearWidthMicrometer().
                clearHeightMicrometer().clearWeightGrossMg().build());

        // master_data should not be affected
        List<MasterData> masterDataListAfter = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListAfter).hasSize(1);
        MasterData masterDataAfter = masterDataListAfter.get(0);
        Assertions.assertThat(masterDataAfter).isEqualTo(masterData);
    }

    @Test
    public void whenProcessWarehouseItemWhenBothWarehouseAndSupplierItemExistShouldReplaceOldWarehouseItem() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
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

        // 2. insert supplier item
        MdmIrisPayload.ShippingUnit.Builder supplierItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(13000).setUpdatedTs(2000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(14000).setUpdatedTs(2000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(15000).setUpdatedTs(2000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1200).setUpdatedTs(2000))
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(800).setUpdatedTs(2000));

        MdmIrisPayload.Item supplierItem = ItemWrapperTestUtil.createItem(key, MdmIrisPayload.MasterDataSource.SUPPLIER,
            supplierItemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(supplierItem));

        // first process should create golden item
        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // 3. insert second warehouse item
        MdmIrisPayload.ShippingUnit.Builder anotherWarehouseItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(16000).setUpdatedTs(1500))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(17000).setUpdatedTs(1500))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(18000).setUpdatedTs(1500))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1400).setUpdatedTs(1500));

        MdmIrisPayload.Item anotherWarehouseItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            anotherWarehouseItemShippingUnit);
        fromIrisItemRepository.insertOrUpdate(new FromIrisItemWrapper(anotherWarehouseItem));

        // second process
        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // new item information should become reference
        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).hasSize(1);

        ReferenceItemWrapper referenceItemWrapper = referenceItems.get(0);
        Assertions.assertThat(referenceItemWrapper.getMdmIdentifier()).isEqualTo(item.getItemId());

        MdmIrisPayload.Item referenceItem = referenceItemWrapper.getItem();
        Assertions.assertThat(referenceItem.getInformationCount()).isEqualTo(2);
        Assertions.assertThat(referenceItem.getInformation(0).getItemShippingUnit())
            .isEqualTo(anotherWarehouseItemShippingUnit.build());
        Assertions.assertThat(referenceItem.getInformation(1).getItemShippingUnit())
            .isEqualTo(supplierItemShippingUnit.clearLengthMicrometer().clearWidthMicrometer().
                clearHeightMicrometer().clearWeightGrossMg().build());

        // master_data should not be affected
        List<MasterData> masterDataListAfter = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListAfter).hasSize(1);
        MasterData masterDataAfter = masterDataListAfter.get(0);
        Assertions.assertThat(masterDataAfter).isEqualTo(masterData);
    }

    @Test
    public void whenProcessMdmItemWhenBothMdmAndSupplierItemExistShouldReplaceOldMdmItem() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        mdmSupplierCachingService.refresh();
        MasterData masterData = createMasterData(key);
        masterDataRepository.insertOrUpdate(masterData);

        List<MasterData> masterDataListBefore = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListBefore).hasSize(1);
        Assertions.assertThat(masterDataListBefore.get(0)).isEqualTo(masterData);

        // 1. insert mdm item
        MdmIrisPayload.ShippingUnit.Builder mdmShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(1000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(1000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(1000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(1000));

        MdmIrisPayload.Item mdmItem = ItemWrapperTestUtil.createMdmItem(
            key, "", mdmShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(mdmItem));

        // 2. insert supplier item
        MdmIrisPayload.ShippingUnit.Builder supplierItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(13000).setUpdatedTs(2000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(14000).setUpdatedTs(2000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(15000).setUpdatedTs(2000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1200).setUpdatedTs(2000))
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(800).setUpdatedTs(2000));

        MdmIrisPayload.Item supplierItem = ItemWrapperTestUtil.createItem(key, MdmIrisPayload.MasterDataSource.SUPPLIER,
            supplierItemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(supplierItem));

        // first process should create golden item
        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // 3. insert second mdm item
        MdmIrisPayload.ShippingUnit.Builder anotherMdmItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(16000).setUpdatedTs(1500))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(17000).setUpdatedTs(1500))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(18000).setUpdatedTs(1500))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1400).setUpdatedTs(1500));

        MdmIrisPayload.Item anotherMdmItem = ItemWrapperTestUtil.createMdmItem(
            key, "", anotherMdmItemShippingUnit);
        fromIrisItemRepository.insertOrUpdate(new FromIrisItemWrapper(anotherMdmItem));

        // second process
        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // new item information should become reference
        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).hasSize(1);

        ReferenceItemWrapper referenceItemWrapper = referenceItems.get(0);
        Assertions.assertThat(referenceItemWrapper.getMdmIdentifier()).isEqualTo(mdmItem.getItemId());

        MdmIrisPayload.Item referenceItem = referenceItemWrapper.getItem();
        Assertions.assertThat(referenceItem.getInformationCount()).isEqualTo(2);
        Assertions.assertThat(referenceItem.getInformation(0).getItemShippingUnit())
            .isEqualTo(anotherMdmItemShippingUnit.build());
        Assertions.assertThat(referenceItem.getInformation(1).getItemShippingUnit())
            .isEqualTo(supplierItemShippingUnit.clearLengthMicrometer().clearWidthMicrometer().
                clearHeightMicrometer().clearWeightGrossMg().build());

        // master_data should not be affected
        List<MasterData> masterDataListAfter = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListAfter).hasSize(1);
        MasterData masterDataAfter = masterDataListAfter.get(0);
        Assertions.assertThat(masterDataAfter).isEqualTo(masterData);
    }

    @Test
    public void whenLessThanTenPercentDifferenceWithReferenceItemShouldUpdate() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));

        int timestamp = 1000;
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(timestamp))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(timestamp))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(timestamp))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(timestamp));
        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, itemShippingUnit);

        referenceItemRepository.insert(new ReferenceItemWrapper(item));

        var newItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder(itemShippingUnit.buildPartial())
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10500).setUpdatedTs(timestamp + 1));

        MdmIrisPayload.Item updatedItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, newItemShippingUnit);

        fromIrisItemRepository.insert(new FromIrisItemWrapper(updatedItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
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
            .isEqualTo(newItemShippingUnit.build());
    }

    @Test
    public void whenLessThanTenPercentDifferenceWithAnotherWarehouseItemShouldNotUpdate() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));

        int timestamp = 1000;
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(timestamp))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(timestamp))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(timestamp))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(timestamp));
        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, "145", itemShippingUnit);

        fromIrisItemRepository.insert(new FromIrisItemWrapper(item));

        var newItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder(itemShippingUnit.buildPartial())
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10500).setUpdatedTs(timestamp + 1));

        MdmIrisPayload.Item updatedItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, "147", newItemShippingUnit);

        fromIrisItemRepository.insert(new FromIrisItemWrapper(updatedItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
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
    public void whenLessThanTenPercentDifferenceAndWrongOrderShouldNotUpdate() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));

        int timestamp = 1000;
        var itemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(timestamp))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(timestamp))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(timestamp))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(timestamp));
        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, "421", itemShippingUnit);

        fromIrisItemRepository.insert(new FromIrisItemWrapper(item));

        var newItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder(itemShippingUnit.buildPartial())
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10500).setUpdatedTs(timestamp - 1))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11500).setUpdatedTs(timestamp - 1))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12500).setUpdatedTs(timestamp - 1))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1050).setUpdatedTs(timestamp - 1));

        MdmIrisPayload.Item updatedItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, "422", newItemShippingUnit);

        fromIrisItemRepository.insert(new FromIrisItemWrapper(updatedItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
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
    public void whenItemNotChangedShouldNotUpdateVersion() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));

        int timestamp = 1000;
        var itemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(timestamp))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(timestamp))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(timestamp))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(timestamp));
        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, "421", itemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(item));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        long versionId = referenceItems.get(0).getItem().getInformation(0).getVersionId();

        // less than 10% diff
        var newItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder(itemShippingUnit.buildPartial())
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(timestamp + 10))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(timestamp + 10))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(timestamp + 10))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(timestamp + 10));

        MdmIrisPayload.Item updatedItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, "422", newItemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(updatedItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // new item information should become reference
        List<ReferenceItemWrapper> newReferenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(newReferenceItems).hasSize(1);

        ReferenceItemWrapper referenceItemWrapper = newReferenceItems.get(0);
        Assertions.assertThat(referenceItemWrapper.getItem().getInformationCount()).isEqualTo(1);
        Assertions.assertThat(referenceItemWrapper.getItem().getInformation(0).getVersionId())
            .isEqualTo(versionId);
        Assertions.assertThat(referenceItemWrapper.getItem().getInformation(0).getItemShippingUnit())
            .isEqualTo(itemShippingUnit.build());
    }

    @Test
    public void testShouldKeepCorrectOrderOfInformations() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        MasterData masterData = createMasterData(key);
        masterDataRepository.insertOrUpdate(masterData);

        List<MasterData> masterDataListBefore = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListBefore).hasSize(1);
        Assertions.assertThat(masterDataListBefore.get(0)).isEqualTo(masterData);

        // 1. insert item from WH2
        MdmIrisPayload.ShippingUnit.Builder itemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(2000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(2000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(2000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(2000));

        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, "WH2", itemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(item));

        // 2. insert item from WH1
        MdmIrisPayload.ShippingUnit.Builder anotherWarehouseItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(1500))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(1500))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(1500))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(1500))
            .setWeightTareMg(MdmIrisPayload.Int64Value.newBuilder().setValue(200).setUpdatedTs(1500));

        MdmIrisPayload.Item anotherWarehouseItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, "WH1",
            anotherWarehouseItemShippingUnit);
        fromIrisItemRepository.insertOrUpdate(new FromIrisItemWrapper(anotherWarehouseItem));

        // 3. insert item from supplier
        MdmIrisPayload.ShippingUnit.Builder supplierItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(800).setUpdatedTs(1500));

        MdmIrisPayload.Item supplierItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.SUPPLIER,
            "supplier",
            supplierItemShippingUnit);
        fromIrisItemRepository.insertOrUpdate(new FromIrisItemWrapper(supplierItem));

        // 4. insert RSL item
        var rslItemBuilder = ItemWrapperTestUtil.createItem(key, MdmIrisPayload.MasterDataSource.MDM,
                MasterDataSourceType.RSL_SOURCE_ID)
            .toBuilder();
        MdmIrisPayload.ReferenceInformation.Builder rslInfo = rslItemBuilder.getInformation(0).toBuilder()
            .addMinInboundLifetimeDay(MdmIrisPayload.RemainingLifetime.newBuilder().setValue(1).setStartDate(10))
            .addMinOutboundLifetimeDay(MdmIrisPayload.RemainingLifetime.newBuilder().setValue(2).setStartDate(10));
        rslItemBuilder.clearInformation().addInformation(rslInfo);
        MdmIrisPayload.Item rslItem = rslItemBuilder.build();
        referenceItemRepository.insertOrUpdate(new ReferenceItemWrapper(rslItem));

        // process
        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).hasSize(1);

        ReferenceItemWrapper referenceItemWrapper = referenceItems.get(0);
        Assertions.assertThat(referenceItemWrapper.getMdmIdentifier()).isEqualTo(item.getItemId());

        MdmIrisPayload.Item referenceItem = referenceItemWrapper.getItem();
        Assertions.assertThat(referenceItem.getInformationCount()).isEqualTo(4);
        Assertions.assertThat(referenceItem.getInformation(0).getItemShippingUnit())
            .isEqualTo(itemShippingUnit.build());
        Assertions.assertThat(referenceItem.getInformation(1).getItemShippingUnit())
            .isEqualTo(supplierItemShippingUnit.build());
        Assertions.assertThat(referenceItem.getInformation(2).getItemShippingUnit())
            .isEqualTo(anotherWarehouseItemShippingUnit
                .clearLengthMicrometer().clearWidthMicrometer().clearHeightMicrometer().clearWeightGrossMg().build());
        Assertions.assertThat(referenceItem.getInformation(3).hasItemShippingUnit()).isFalse();
        Assertions.assertThat(referenceItem.getInformation(3).getMinInboundLifetimeDayCount()).isNotZero();
        Assertions.assertThat(referenceItem.getInformation(3).getMinOutboundLifetimeDayCount()).isNotZero();
    }

    @Test
    public void shouldChangeSupplierSourceToWarehouseIfDimensionsEqual() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));

        MdmIrisPayload.ShippingUnit.Builder shippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            10.0, 10.0, 10.0, 1.0, null, null, 1000);
        MdmIrisPayload.Item supplierItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.SUPPLIER, shippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(supplierItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        MdmIrisPayload.Item warehouseItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, shippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(warehouseItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // new item information should become reference
        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).hasSize(1);

        ReferenceItemWrapper referenceItemWrapper = referenceItems.get(0);
        Assertions.assertThat(referenceItemWrapper.getMdmIdentifier()).isEqualTo(supplierItem.getItemId());

        MdmIrisPayload.Item referenceItem = referenceItemWrapper.getItem();
        Assertions.assertThat(referenceItem.getInformationCount()).isEqualTo(1);
        Assertions.assertThat(referenceItem.getInformation(0).getSource().getType())
            .isEqualTo(MdmIrisPayload.MasterDataSource.WAREHOUSE);
        Assertions.assertThat(referenceItem.getInformation(0).getItemShippingUnit())
            .isEqualTo(shippingUnit.build());
    }

    @Test
    public void shouldChangeSupplierSourceToMdmIfDimensionsEqual() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));

        MdmIrisPayload.ShippingUnit.Builder shippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            10.0, 10.0, 10.0, 1.0, null, null, 1000);
        MdmIrisPayload.Item supplierItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.SUPPLIER, shippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(supplierItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        MdmIrisPayload.Item warehouseItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.MDM, shippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(warehouseItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // new item information should become reference
        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).hasSize(1);

        ReferenceItemWrapper referenceItemWrapper = referenceItems.get(0);
        Assertions.assertThat(referenceItemWrapper.getMdmIdentifier()).isEqualTo(supplierItem.getItemId());

        MdmIrisPayload.Item referenceItem = referenceItemWrapper.getItem();
        Assertions.assertThat(referenceItem.getInformationCount()).isEqualTo(1);
        Assertions.assertThat(referenceItem.getInformation(0).getSource().getType())
            .isEqualTo(MdmIrisPayload.MasterDataSource.MDM);
        Assertions.assertThat(referenceItem.getInformation(0).getItemShippingUnit())
            .isEqualTo(shippingUnit.build());
    }

    @Test
    public void whenProcessSupplierWarehouseAndMeasurementItem() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        MasterData masterData = createMasterData(key);
        masterDataRepository.insertOrUpdate(masterData);

        List<MasterData> masterDataListBefore = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListBefore).hasSize(1);
        Assertions.assertThat(masterDataListBefore.get(0)).isEqualTo(masterData);

        // create warehouse item
        MdmIrisPayload.ShippingUnit.Builder warehouseShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(1000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(1000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(1000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(1000))
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1500).setUpdatedTs(1000));
        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.WAREHOUSE, warehouseShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(item));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // create measurement item
        MdmIrisPayload.ShippingUnit.Builder measurementShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(13000).setUpdatedTs(2000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(14000).setUpdatedTs(2000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(15000).setUpdatedTs(2000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(2000).setUpdatedTs(2000));

        MdmIrisPayload.Item measurementItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.MEASUREMENT,
            measurementShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(measurementItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // create supplier item
        MdmIrisPayload.ShippingUnit.Builder supplierShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(13000).setUpdatedTs(3000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(14000).setUpdatedTs(3000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(15000).setUpdatedTs(3000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(2000).setUpdatedTs(3000))
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1900).setUpdatedTs(3000))
            .setWeightTareMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1800).setUpdatedTs(3000));

        MdmIrisPayload.Item supplierItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.SUPPLIER,
            supplierShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(supplierItem));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // new item information should become reference
        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).hasSize(1);

        ReferenceItemWrapper referenceItemWrapper = referenceItems.get(0);
        Assertions.assertThat(referenceItemWrapper.getMdmIdentifier()).isEqualTo(item.getItemId());

        MdmIrisPayload.Item referenceItem = referenceItemWrapper.getItem();
        Assertions.assertThat(referenceItem.getInformationCount()).isEqualTo(3);

        MdmIrisPayload.ShippingUnit.Builder expectedShippingUnit1 = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(13000).setUpdatedTs(2000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(14000).setUpdatedTs(2000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(15000).setUpdatedTs(2000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(2000).setUpdatedTs(2000));
        MdmIrisPayload.ShippingUnit.Builder expectedShippingUnit2 = MdmIrisPayload.ShippingUnit.newBuilder()
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1500).setUpdatedTs(1000));
        MdmIrisPayload.ShippingUnit.Builder expectedShippingUnit3 = MdmIrisPayload.ShippingUnit.newBuilder()
            .setWeightTareMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1800).setUpdatedTs(3000));

        Assertions.assertThat(referenceItem.getInformation(0).getSource().getType())
            .isEqualTo(MdmIrisPayload.MasterDataSource.MEASUREMENT);
        Assertions.assertThat(referenceItem.getInformation(0).getItemShippingUnit())
            .isEqualTo(expectedShippingUnit1.build());

        Assertions.assertThat(referenceItem.getInformation(1).getSource().getType())
            .isEqualTo(MdmIrisPayload.MasterDataSource.WAREHOUSE);
        Assertions.assertThat(referenceItem.getInformation(1).getItemShippingUnit())
            .isEqualTo(expectedShippingUnit2.build());

        Assertions.assertThat(referenceItem.getInformation(2).getSource().getType())
            .isEqualTo(MdmIrisPayload.MasterDataSource.SUPPLIER);
        Assertions.assertThat(referenceItem.getInformation(2).getItemShippingUnit())
            .isEqualTo(expectedShippingUnit3.build());

        // master_data should not be affected
        List<MasterData> masterDataListAfter = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListAfter).hasSize(1);
        MasterData masterDataAfter = masterDataListAfter.get(0);
        Assertions.assertThat(masterDataAfter).isEqualTo(masterData);
    }

    @Test
    public void whenSupplierItemReceivedTsAfterSwitchingMomentShouldRaiseSupplierSourcePriorityToWarehousePriority() {
        //switching moment :: if supplier source type block is received after warehouse source type block,
        //then supplier's block priority will be raised to warehouses'
        weightDimensionsService.setReplaceWarehouseWithSupplierFromDefaultTs(
            TimestampUtil.toInstant(LocalDateTime.of(2020, 1, 1, 0, 0)));

        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
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

        // 2. insert supplier item
        MdmIrisPayload.ShippingUnit.Builder supplierItemShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(13000).setUpdatedTs(5000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(14000).setUpdatedTs(5000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(15000).setUpdatedTs(5000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1200).setUpdatedTs(5000))
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(800).setUpdatedTs(5000));

        MdmIrisPayload.Item supplierItem = ItemWrapperTestUtil.createItem(key, MdmIrisPayload.MasterDataSource.SUPPLIER,
            supplierItemShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(supplierItem));

        // first process should create golden item
        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // second process
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
            .isEqualTo(supplierItemShippingUnit.build());

        // master_data should not be affected
        List<MasterData> masterDataListAfter = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListAfter).hasSize(1);
        MasterData masterDataAfter = masterDataListAfter.get(0);
        Assertions.assertThat(masterDataAfter).isEqualTo(masterData);
    }

    @Test
    public void testCalculateGoldenItemUsingSskuSilverParamValues() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY));
        MasterData masterData = createMasterData(key);
        masterDataRepository.insertOrUpdate(masterData);
        storageKeyValueService.putValue(MdmProperties.USE_SSKU_SILVER_PARAM_VALUES_IN_GOLDEN_CALC, true);
        sskuExistenceRepository.markExistence(List.of(key), true);

        // create supplier item in from_iris_item
        MdmIrisPayload.ShippingUnit.Builder supplierShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            10.0, 10.0, 10.0, 1.0, 0.8, 0.2, 1000);
        MdmIrisPayload.Item supplierItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.SUPPLIER,
            supplierShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(supplierItem));

        // create items in ssku_silver_param_value
        MdmIrisPayload.ShippingUnit.Builder eoxShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            15.0, 10.0, 15.0, 2.0, 0.9, null, 2000);
        MdmIrisPayload.ShippingUnit.Builder mdmOperatorShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            20.0, 10.0, 15.0, 2.0, null, null, 3000);
        silverSskuRepository.insertOrUpdateSsku(createSskuSilverParamValuesFromShippingUnit(
            eoxShippingUnit.build(), key, MasterDataSourceType.SUPPLIER, String.valueOf(SUPPLIER_ID)));
        silverSskuRepository.insertOrUpdateSsku(createSskuSilverParamValuesFromShippingUnit(
            mdmOperatorShippingUnit.build(), key, MasterDataSourceType.MDM_OPERATOR, "anna-ivanovna"));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // new item information should become reference
        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).hasSize(1);

        ReferenceItemWrapper referenceItemWrapper = referenceItems.get(0);
        Assertions.assertThat(referenceItemWrapper.getMdmIdentifier()).isEqualTo(supplierItem.getItemId());

        MdmIrisPayload.Item referenceItem = referenceItemWrapper.getItem();
        Assertions.assertThat(referenceItem.getInformationCount()).isEqualTo(3);

        MdmIrisPayload.ShippingUnit.Builder expectedShippingUnit1 = ItemWrapperTestUtil.generateShippingUnit(
            20.0, 10.0, 15.0, 2.0, null, null, 3000);
        MdmIrisPayload.ShippingUnit.Builder expectedShippingUnit2 = ItemWrapperTestUtil.generateShippingUnit(
            null, null, null, null, 0.9, null, 2000);
        MdmIrisPayload.ShippingUnit.Builder expectedShippingUnit3 = ItemWrapperTestUtil.generateShippingUnit(
            null, null, null, null, null, 0.2, 1000);

        Assertions.assertThat(referenceItem.getInformation(0).getItemShippingUnit())
            .isEqualTo(expectedShippingUnit1.build());
        Assertions.assertThat(referenceItem.getInformation(1).getItemShippingUnit())
            .isEqualTo(expectedShippingUnit2.build());
        Assertions.assertThat(referenceItem.getInformation(2).getItemShippingUnit())
            .isEqualTo(expectedShippingUnit3.build());

        // master_data should not be affected
        List<MasterData> masterDataListAfter = masterDataRepository.findByShopSkuKeys(Collections.singleton(key));
        Assertions.assertThat(masterDataListAfter).hasSize(1);
        MasterData masterDataAfter = masterDataListAfter.get(0);
        Assertions.assertThat(masterDataAfter).isEqualTo(masterData);
    }

    @Test
    public void testIncludeBusinessSskuSilverParamValuesIntoServiceGoldenItem() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID, "42");
        ShopSkuKey businessKey = new ShopSkuKey(BUSINESS_ID, "42");
        sskuExistenceRepository.markExistence(List.of(key), true);

        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID)
            .setBusinessEnabled(true).setBusinessId(BUSINESS_ID).setType(MdmSupplierType.THIRD_PARTY));
        mdmSupplierRepository.insert(new MdmSupplier().setId(BUSINESS_ID).setType(MdmSupplierType.BUSINESS));

        MasterData masterData = createMasterData(key);
        masterDataRepository.insertOrUpdate(masterData);
        storageKeyValueService.putValue(MdmProperties.USE_SSKU_SILVER_PARAM_VALUES_IN_GOLDEN_CALC, true);

        // create supplier item in from_iris_item
        MdmIrisPayload.ShippingUnit.Builder supplierShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            10.0, 10.0, 10.0, 1.0, 0.8, null, 1000);
        MdmIrisPayload.Item supplierItem = ItemWrapperTestUtil.createItem(
            key, MdmIrisPayload.MasterDataSource.SUPPLIER,
            supplierShippingUnit);
        fromIrisItemRepository.insert(new FromIrisItemWrapper(supplierItem));

        // create business item in ssku_silver_param_value
        MdmIrisPayload.ShippingUnit.Builder eoxShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            15.0, 10.0, 15.0, 2.0, null, null, 2000);
        silverSskuRepository.insertOrUpdateSsku(createSskuSilverParamValuesFromShippingUnit(
            eoxShippingUnit.build(), businessKey, MasterDataSourceType.SUPPLIER, String.valueOf(BUSINESS_ID)));

        sskuToRefreshRepository.enqueue(key, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(1);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        // new item information should become reference
        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(Collections.singleton(key));
        Assertions.assertThat(referenceItems).hasSize(1);

        ReferenceItemWrapper referenceItemWrapper = referenceItems.get(0);
        Assertions.assertThat(referenceItemWrapper.getMdmIdentifier()).isEqualTo(supplierItem.getItemId());

        MdmIrisPayload.Item referenceItem = referenceItemWrapper.getItem();
        Assertions.assertThat(referenceItem.getInformationCount()).isEqualTo(2);

        MdmIrisPayload.ShippingUnit.Builder expectedShippingUnit1 = ItemWrapperTestUtil.generateShippingUnit(
            15.0, 10.0, 15.0, 2.0, null, null, 2000);
        MdmIrisPayload.ShippingUnit.Builder expectedShippingUnit2 = ItemWrapperTestUtil.generateShippingUnit(
            null, null, null, null, 0.8, null, 1000);

        Assertions.assertThat(referenceItem.getInformation(0).getItemShippingUnit())
            .isEqualTo(expectedShippingUnit1.build());
        Assertions.assertThat(referenceItem.getInformation(1).getItemShippingUnit())
            .isEqualTo(expectedShippingUnit2.build());
    }

    @Test
    public void testCalculateGoldenItemPerServiceKeyIncludingSskuSilverParamValuesWhenBothServiceKeysInQueue() {
        // Подготовим данные.
        // 0. Проставим нужные рубильники.
        storageKeyValueService.putValue(MdmProperties.USE_SSKU_SILVER_PARAM_VALUES_IN_GOLDEN_CALC, true);

        // 1. Создадим бизнес-группы.
        prepareBusinessGroup(false);

        // 2. Сгенерим SSKU мастер-данные.
        ShopSkuKey offerForStage2 = new ShopSkuKey(STAGE2_KEY.getSupplierId(), STAGE2_KEY.getShopSku());
        MasterData masterData1 = new MasterData();
        masterData1.setShopSkuKey(offerForStage2);
        masterData1.setCustomsCommodityCode("50600980031");
        masterData1.setItemShippingUnit(ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0, 10.0, 1.0, null, null).build());

        ShopSkuKey offerForStage3Key1 = new ShopSkuKey(STAGE3_KEY1.getSupplierId(), STAGE3_KEY1.getShopSku());
        MasterData masterData2 = new MasterData();
        masterData2.setShopSkuKey(offerForStage3Key1);
        masterData2.setItemShippingUnit(ItemWrapperTestUtil.generateShippingUnit(20.0, 20.0, 20.0, 2.0, null, null).build());

        ShopSkuKey offerForStage3Key2 = new ShopSkuKey(STAGE3_KEY2.getSupplierId(), STAGE3_KEY2.getShopSku());
        MasterData masterData3 = new MasterData();
        masterData3.setShopSkuKey(offerForStage3Key2);
        masterData3.setItemShippingUnit(ItemWrapperTestUtil.generateShippingUnit(30.0, 30.0, 30.0, 3.0, null, null).build());

        masterDataRepository.insertBatch(masterData1, masterData2, masterData3);

        // 3. Создадим маппинги.
        ModelKey key = new ModelKey(1L, 100L);
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key).setShopSkuKey(offerForStage2).setUpdateStamp(1L));
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key).setShopSkuKey(offerForStage3Key1).setUpdateStamp(2L));
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key).setShopSkuKey(offerForStage3Key2).setUpdateStamp(3L));

        // 4. Добавим данные в mdm.ssku_silver_param_value по бизнес-ключу.
        int sskuSilverTs = 2000;
        MdmIrisPayload.ShippingUnit.Builder businessShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            23.0, 23.0, 23.0, 2.3, 0.9, null, sskuSilverTs);
        silverSskuRepository.insertOrUpdateSsku(createSskuSilverParamValuesFromShippingUnit(
            businessShippingUnit.build(), BUSINESS_KEY, MasterDataSourceType.SUPPLIER, String.valueOf(SUPPLIER_ID)));

        // 5. Добавим в очередь обновленные ssku.
        long slightlyOutdatedTs = 2;
        long quiteRecentTs = 3;
        long mostRecentTs = 4;

        var stage2ShippingUnit =
            ItemWrapperTestUtil.generateShippingUnit(75.0, 75.0, 75.0, 17.0, null, null, mostRecentTs);
        var stage3ShippingUnit1 =
            ItemWrapperTestUtil.generateShippingUnit(25.0, 25.0, 25.0, 2.5, null, null, slightlyOutdatedTs);
        var stage3ShippingUnit2 =
            ItemWrapperTestUtil.generateShippingUnit(35.0, 35.0, 35.0, 3.5, null, null, quiteRecentTs);

        var stage2Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE2_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "171", stage2ShippingUnit));
        var stage3Item1 = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE3_KEY1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "66", stage3ShippingUnit1));
        var stage3Item2 = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE3_KEY2, MdmIrisPayload.MasterDataSource.WAREHOUSE, "27", stage3ShippingUnit2));

        fromIrisItemRepository.insertBatch(stage2Item, stage3Item1, stage3Item2);

        // Запускаем пересчет золота.
        sskuToRefreshRepository.enqueueAll(
            List.of(STAGE2_KEY, STAGE3_KEY1, STAGE3_KEY2),
            MdmEnqueueReason.CHANGED_IRIS_ITEM
        );
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(3);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        Map<ShopSkuKey, ReferenceItemWrapper> goldenItems =
            referenceItemRepository.findByIds(List.of(BUSINESS_KEY, STAGE2_KEY, STAGE3_KEY1, STAGE3_KEY2))
                .stream()
                .collect(Collectors.toMap(ItemWrapper::getShopSkuKey, Function.identity()));
        Assertions.assertThat(goldenItems).doesNotContainKey(BUSINESS_KEY);
        Assertions.assertThat(goldenItems.get(STAGE2_KEY).getCombinedItemShippingUnit())
            .isEqualTo(stage2ShippingUnit.build());
        // Параметр mdm.ssku_silver_param_values из ssku_silver_param по бизнес-ключу должен прорасти в сервисы.
        stage3ShippingUnit1.setWeightNetMg(
            MdmIrisPayload.Int64Value.newBuilder()
                .setValue(900000)
                .setUpdatedTs(sskuSilverTs)
                .build()
        );
        stage3ShippingUnit2.setWeightNetMg(
            MdmIrisPayload.Int64Value.newBuilder()
                .setValue(900000)
                .setUpdatedTs(sskuSilverTs)
                .build()
        );

        Assertions.assertThat(goldenItems.get(STAGE3_KEY1).getCombinedItemShippingUnit())
            .isEqualTo(stage3ShippingUnit2.build());
        Assertions.assertThat(goldenItems.get(STAGE3_KEY2).getCombinedItemShippingUnit())
            .isEqualTo(stage3ShippingUnit2.build());
    }

    @Test
    public void testCalculateGoldenItemUsingCategoryPVMskuPVSskuSilverPVForBusinessGroupByServiceOrBusinessKey() {
        // Подготовим данные.
        // 0. Проставим нужные рубильники.
        storageKeyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        storageKeyValueService.putValue(MdmProperties.USE_SSKU_SILVER_PARAM_VALUES_IN_GOLDEN_CALC, true);

        // 1. Создадим бизнес-группы.
        prepareBusinessGroup(false);

        // 2. Сгенерим SSKU мастер-данные.
        ShopSkuKey offerForStage2 = new ShopSkuKey(STAGE2_KEY.getSupplierId(), STAGE2_KEY.getShopSku());
        MasterData masterData1 = new MasterData();
        masterData1.setShopSkuKey(offerForStage2);
        masterData1.setCustomsCommodityCode("50600980031");
        masterData1.setItemShippingUnit(ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0, 10.0, 1.0, null, null).build());

        ShopSkuKey offerForStage3Key1 = new ShopSkuKey(STAGE3_KEY1.getSupplierId(), STAGE3_KEY1.getShopSku());
        MasterData masterData2 = new MasterData();
        masterData2.setShopSkuKey(offerForStage3Key1);
        masterData2.setItemShippingUnit(ItemWrapperTestUtil.generateShippingUnit(20.0, 20.0, 20.0, 2.0, null, null).build());

        ShopSkuKey offerForStage3Key2 = new ShopSkuKey(STAGE3_KEY2.getSupplierId(), STAGE3_KEY2.getShopSku());
        MasterData masterData3 = new MasterData();
        masterData3.setShopSkuKey(offerForStage3Key2);
        masterData3.setItemShippingUnit(ItemWrapperTestUtil.generateShippingUnit(30.0, 30.0, 30.0, 3.0, null, null).build());

        masterDataRepository.insertBatch(masterData1, masterData2, masterData3);

        // 3. Создадим маппинги.
        ModelKey key = new ModelKey(1L, 100L);
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key).setShopSkuKey(offerForStage2).setUpdateStamp(1L));
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key).setShopSkuKey(offerForStage3Key1).setUpdateStamp(2L));
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key).setShopSkuKey(offerForStage3Key2).setUpdateStamp(3L));

        // 4. Сгенерим категорийную настройку - Максимальное значение веса (кг).
        CategoryParamValue categoryValue = new CategoryParamValue().setCategoryId(key.getCategoryId());
        categoryValue.setMdmParamId(KnownMdmParams.WEIGHT_MAX_KG);
        categoryValue.setNumeric(BigDecimal.valueOf(100500));
        categoryValue.setXslName("-");
        categoryValue.setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        categoryParamValueRepository.insert(categoryValue);

        // 5. Сгенерим ВГХ-параметры на уровне MSKU.
        MskuParamValue lengthMskuValue = new MskuParamValue().setMskuId(key.getModelId());
        lengthMskuValue.setMdmParamId(KnownMdmParams.LENGTH);
        lengthMskuValue.setNumeric(BigDecimal.valueOf(40));
        lengthMskuValue.setXslName("-");
        lengthMskuValue.setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);

        MskuParamValue widthMskuValue = new MskuParamValue().setMskuId(key.getModelId());
        widthMskuValue.setMdmParamId(KnownMdmParams.WIDTH);
        widthMskuValue.setNumeric(BigDecimal.valueOf(40));
        widthMskuValue.setXslName("-");
        widthMskuValue.setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);

        MskuParamValue heightMskuValue = new MskuParamValue().setMskuId(key.getModelId());
        heightMskuValue.setMdmParamId(KnownMdmParams.HEIGHT);
        heightMskuValue.setNumeric(BigDecimal.valueOf(40));
        heightMskuValue.setXslName("-");
        heightMskuValue.setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);

        MskuParamValue weightMskuValue = new MskuParamValue().setMskuId(key.getModelId());
        weightMskuValue.setMdmParamId(KnownMdmParams.WEIGHT_GROSS);
        weightMskuValue.setNumeric(BigDecimal.valueOf(4));
        weightMskuValue.setXslName("-");
        weightMskuValue.setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);

        var commonMsku =
            new CommonMsku(key, List.of(lengthMskuValue, widthMskuValue, heightMskuValue, weightMskuValue));

        // 6. Добавим данные в mdm.ssku_silver_param_value по бизнес-ключу.
        // В итоговое золото должен попасть параметр weightNetKg.
        int sskuSilverTs = 2000;
        MdmIrisPayload.ShippingUnit.Builder eoxShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            23.0, 23.0, 23.0, 2.3, 0.9, null, sskuSilverTs);
        silverSskuRepository.insertOrUpdateSsku(createSskuSilverParamValuesFromShippingUnit(
            eoxShippingUnit.build(), BUSINESS_KEY, MasterDataSourceType.SUPPLIER, String.valueOf(SUPPLIER_ID)));

        // 7. Создадим уже существующие золотые записи, которые должны будут полностью обновиться после пересчета.
        ReferenceItemWrapper stage2KeyExistingReferenceItem =
            getItem(STAGE2_KEY, 12.0, 12.0, 12.0, 1.2, null, null, 1);
        ReferenceItemWrapper stage3Key1ExistingReferenceItem =
            getItem(STAGE3_KEY1, 13.0, 13.0, 13.0, 1.3, null, null, 2);
        ReferenceItemWrapper stage3Key2ExistingReferenceItem =
            getItem(STAGE3_KEY2, 14.0, 14.0, 14.0, 1.4, null, null, 3);

        referenceItemRepository.insertBatch(stage2KeyExistingReferenceItem,
            stage3Key1ExistingReferenceItem, stage3Key2ExistingReferenceItem);

        // 8. Добавим в очередь обновленные ssku.
        long outdatedTs = 1;
        long slightlyOutdatedTs = 2;
        long quiteRecentTs = 3;
        long mostRecentTs = 4;

        var businessShippingUnit = // Предположим, это ВГХ по бизнесу от ЕОХ или других не ИРИСовых источников в будущем
            ItemWrapperTestUtil.generateShippingUnit(15.0, 15.0, 15.0, 1.5, null, null, outdatedTs);
        var stage2ShippingUnit =
            ItemWrapperTestUtil.generateShippingUnit(75.0, 75.0, 75.0, 17.0, null, null, mostRecentTs);
        var stage3ShippingUnit1 =
            ItemWrapperTestUtil.generateShippingUnit(25.0, 25.0, 25.0, 2.5, null, null, slightlyOutdatedTs);
        // Должно прорасти в золото.
        var stage3ShippingUnit2 =
            ItemWrapperTestUtil.generateShippingUnit(35.0, 35.0, 35.0, 3.5, null, null, quiteRecentTs);

        var businessItem = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            BUSINESS_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "172", businessShippingUnit));
        var stage2Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE2_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "171", stage2ShippingUnit));
        var stage3Item1 = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE3_KEY1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "66", stage3ShippingUnit1));
        var stage3Item2 = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE3_KEY2, MdmIrisPayload.MasterDataSource.WAREHOUSE, "27", stage3ShippingUnit2));

        fromIrisItemRepository.insertBatch(businessItem, stage2Item, stage3Item1, stage3Item2);

        // Запускаем пересчет золота.
        sskuToRefreshRepository.enqueueAll(
            List.of(BUSINESS_KEY, STAGE2_KEY, STAGE3_KEY1, STAGE3_KEY2),
            MdmEnqueueReason.CHANGED_IRIS_ITEM
        );
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(4);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        Map<ShopSkuKey, ReferenceItemWrapper> goldenItems =
            referenceItemRepository.findByIds(List.of(BUSINESS_KEY, STAGE2_KEY, STAGE3_KEY1, STAGE3_KEY2))
                .stream()
                .collect(Collectors.toMap(ItemWrapper::getShopSkuKey, Function.identity()));
        Assertions.assertThat(goldenItems).doesNotContainKey(BUSINESS_KEY);
        Assertions.assertThat(goldenItems.get(STAGE2_KEY).getCombinedItemShippingUnit())
            .isEqualTo(stage2ShippingUnit.build());
        // Входящие в бизнес-группу итемы должны иметь общие бизнес-ВГХ. В нашем примере это самые свежие ВГХ, взятые
        // из последнего юнита + добавленный параметр из mdm.ssku_silver_param_values.
        stage3ShippingUnit2.setWeightNetMg(
            MdmIrisPayload.Int64Value.newBuilder()
                .setValue(900000)
                .setUpdatedTs(sskuSilverTs)
                .build()
        );

        Assertions.assertThat(goldenItems.get(STAGE3_KEY1).getCombinedItemShippingUnit())
            .isEqualTo(stage3ShippingUnit2.build());
        Assertions.assertThat(goldenItems.get(STAGE3_KEY2).getCombinedItemShippingUnit())
            .isEqualTo(stage3ShippingUnit2.build());
    }

    @Test
    public void testCalculateGoldenItemShouldLoadFullGroupByServiceKeyAndCalculateGoldenForGroup() {
        // Подготовим данные.
        // 0. Проставим нужные рубильники.
        storageKeyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        storageKeyValueService.putValue(MdmProperties.USE_SSKU_SILVER_PARAM_VALUES_IN_GOLDEN_CALC, true);

        // 1. Создадим бизнес-группы.
        prepareBusinessGroup(false);

        // 2. Сгенерим SSKU мастер-данные.
        ShopSkuKey offerForStage2 = new ShopSkuKey(STAGE2_KEY.getSupplierId(), STAGE2_KEY.getShopSku());
        MasterData masterData1 = new MasterData();
        masterData1.setShopSkuKey(offerForStage2);
        masterData1.setCustomsCommodityCode("50600980031");
        masterData1.setItemShippingUnit(ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0, 10.0, 1.0, null, null).build());

        ShopSkuKey offerForStage3Key1 = new ShopSkuKey(STAGE3_KEY1.getSupplierId(), STAGE3_KEY1.getShopSku());
        MasterData masterData2 = new MasterData();
        masterData2.setShopSkuKey(offerForStage3Key1);
        masterData2.setItemShippingUnit(ItemWrapperTestUtil.generateShippingUnit(20.0, 20.0, 20.0, 2.0, null, null).build());

        ShopSkuKey offerForStage3Key2 = new ShopSkuKey(STAGE3_KEY2.getSupplierId(), STAGE3_KEY2.getShopSku());
        MasterData masterData3 = new MasterData();
        masterData3.setShopSkuKey(offerForStage3Key2);
        masterData3.setItemShippingUnit(ItemWrapperTestUtil.generateShippingUnit(30.0, 30.0, 30.0, 3.0, null, null).build());

        masterDataRepository.insertBatch(masterData1, masterData2, masterData3);

        // 3. Создадим маппинги.
        ModelKey key = new ModelKey(1L, 100L);
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key).setShopSkuKey(offerForStage2).setUpdateStamp(1L));
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key).setShopSkuKey(offerForStage3Key1).setUpdateStamp(2L));
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key).setShopSkuKey(offerForStage3Key2).setUpdateStamp(3L));

        // 4. Добавим данные в mdm.ssku_silver_param_value по бизнес-ключу.
        // В итоговое золото должен попасть параметр weightNetKg.
        int sskuSilverTs = 2000;
        MdmIrisPayload.ShippingUnit.Builder eoxShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            23.0, 23.0, 23.0, 2.3, 0.9, null, sskuSilverTs);
        // В итоговое золото должен попасть параметр weightTareKg.
        MdmIrisPayload.ShippingUnit.Builder stage3Key1ShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            null, null, null, null, null, 1.2, sskuSilverTs + 1);
        silverSskuRepository.insertOrUpdateSsku(createSskuSilverParamValuesFromShippingUnit(
            eoxShippingUnit.build(), BUSINESS_KEY, MasterDataSourceType.SUPPLIER, String.valueOf(SUPPLIER_ID)));
        silverSskuRepository.insertOrUpdateSsku(createSskuSilverParamValuesFromShippingUnit(
            stage3Key1ShippingUnit.build(), STAGE3_KEY1, MasterDataSourceType.SUPPLIER, String.valueOf(SUPPLIER_ID)));

        // 6. Создадим уже существующие золотые записи, которые должны будут полностью обновиться после пересчета.
        ReferenceItemWrapper stage2KeyExistingReferenceItem =
            getItem(STAGE2_KEY, 12.0, 12.0, 12.0, 1.2, null, null, 1);
        ReferenceItemWrapper stage3Key1ExistingReferenceItem =
            getItem(STAGE3_KEY1, 13.0, 13.0, 13.0, 1.3, null, null, 2);
        ReferenceItemWrapper stage3Key2ExistingReferenceItem =
            getItem(STAGE3_KEY2, 14.0, 14.0, 14.0, 1.4, null, null, 3);

        referenceItemRepository.insertBatch(stage2KeyExistingReferenceItem,
            stage3Key1ExistingReferenceItem, stage3Key2ExistingReferenceItem);

        // 7. Добавим в очередь 2 обновленные сервисные ssku, одна из которых принадлежит бизнес-группе.
        // В итоге золото должно обновиться у всей бизнес-группы.
        long quiteRecentTs = 3;
        long mostRecentTs = 4;

        var stage2ShippingUnit =
            ItemWrapperTestUtil.generateShippingUnit(75.0, 75.0, 75.0, 17.0, null, null, mostRecentTs);
        // Должно прорасти в золото для всех сервисов существующей бизнес-группы.
        var stage3ShippingUnit2 =
            ItemWrapperTestUtil.generateShippingUnit(35.0, 35.0, 35.0, 3.5, null, null, quiteRecentTs);

        var stage2Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE2_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "171", stage2ShippingUnit));
        var stage3Item2 = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE3_KEY2, MdmIrisPayload.MasterDataSource.WAREHOUSE, "27", stage3ShippingUnit2));

        fromIrisItemRepository.insertBatch(stage2Item, stage3Item2);

        // Запускаем пересчет золота.
        sskuToRefreshRepository.enqueueAll(
            List.of(STAGE2_KEY, STAGE3_KEY2),
            MdmEnqueueReason.CHANGED_IRIS_ITEM
        );
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(2);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        Map<ShopSkuKey, ReferenceItemWrapper> goldenItems =
            referenceItemRepository.findByIds(List.of(BUSINESS_KEY, STAGE2_KEY, STAGE3_KEY1, STAGE3_KEY2))
                .stream()
                .collect(Collectors.toMap(ItemWrapper::getShopSkuKey, Function.identity()));
        Assertions.assertThat(goldenItems).doesNotContainKey(BUSINESS_KEY);
        Assertions.assertThat(goldenItems.get(STAGE2_KEY).getCombinedItemShippingUnit())
            .isEqualTo(stage2ShippingUnit.build());
        // Входящие в бизнес-группу итемы должны иметь общие бизнес-ВГХ. Это самые свежие ВГХ, взятые
        // из последнего юнита + добавленный параметр из mdm.ssku_silver_param_values.
        stage3ShippingUnit2
            .setWeightNetMg(
                MdmIrisPayload.Int64Value.newBuilder()
                    .setValue(900000)
                    .setUpdatedTs(sskuSilverTs)
                    .build())
            .setWeightTareMg(MdmIrisPayload.Int64Value.newBuilder()
                .setValue(1200000)
                .setUpdatedTs(sskuSilverTs + 1)
                .build());

        Assertions.assertThat(goldenItems.get(STAGE3_KEY1).getCombinedItemShippingUnit())
            .isEqualTo(stage3ShippingUnit2.build());
        Assertions.assertThat(goldenItems.get(STAGE3_KEY2).getCombinedItemShippingUnit())
            .isEqualTo(stage3ShippingUnit2.build());
    }

    @Test
    public void testCalculateGoldenItemShouldLoadFullGroupByBusinessKeyAndCalculateGoldenForGroup() {
        // Подготовим данные.
        // 0. Проставим нужные рубильники.
        storageKeyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        storageKeyValueService.putValue(MdmProperties.USE_SSKU_SILVER_PARAM_VALUES_IN_GOLDEN_CALC, true);

        // 1. Создадим бизнес-группы.
        prepareBusinessGroup(false);

        // 2. Сгенерим SSKU мастер-данные.
        ShopSkuKey offerForStage2 = new ShopSkuKey(STAGE2_KEY.getSupplierId(), STAGE2_KEY.getShopSku());
        MasterData masterData1 = new MasterData();
        masterData1.setShopSkuKey(offerForStage2);
        masterData1.setCustomsCommodityCode("50600980031");
        masterData1.setItemShippingUnit(ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0, 10.0, 1.0, null, null).build());

        ShopSkuKey offerForStage3Key1 = new ShopSkuKey(STAGE3_KEY1.getSupplierId(), STAGE3_KEY1.getShopSku());
        MasterData masterData2 = new MasterData();
        masterData2.setShopSkuKey(offerForStage3Key1);
        masterData2.setItemShippingUnit(ItemWrapperTestUtil.generateShippingUnit(20.0, 20.0, 20.0, 2.0, null, null).build());

        ShopSkuKey offerForStage3Key2 = new ShopSkuKey(STAGE3_KEY2.getSupplierId(), STAGE3_KEY2.getShopSku());
        MasterData masterData3 = new MasterData();
        masterData3.setShopSkuKey(offerForStage3Key2);
        masterData3.setItemShippingUnit(ItemWrapperTestUtil.generateShippingUnit(30.0, 30.0, 30.0, 3.0, null, null).build());

        masterDataRepository.insertBatch(masterData1, masterData2, masterData3);

        // 3. Создадим маппинги.
        ModelKey key = new ModelKey(1L, 100L);
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key).setShopSkuKey(offerForStage2).setUpdateStamp(1L));
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key).setShopSkuKey(offerForStage3Key1).setUpdateStamp(2L));
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key).setShopSkuKey(offerForStage3Key2).setUpdateStamp(3L));

        // 4. Добавим данные в mdm.ssku_silver_param_value по сервисному-ключу.
        // В итоговое золото должен попасть параметр weightNetKg.
        int sskuSilverTs = 2000;
        MdmIrisPayload.ShippingUnit.Builder eoxShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            23.0, 23.0, 23.0, 2.3, 0.9, null, sskuSilverTs);
        silverSskuRepository.insertOrUpdateSsku(createSskuSilverParamValuesFromShippingUnit(
            eoxShippingUnit.build(), STAGE3_KEY1, MasterDataSourceType.SUPPLIER, String.valueOf(SUPPLIER_ID)));

        // 5. Добавим в очередь бизнес-ssku и сервисную со 2-й стадии.
        // В итоге должна подгрузиться вся бизнес-группа по бизнес-ключу и золото должно быть одним на группу.
        long quiteRecentTs = 3;
        long mostRecentTs = 4;

        // Должно прорасти в золото.
        var businessShippingUnit = // Предположим, это ВГХ по бизнесу от ЕОХ или других не ИРИСовых источников в будущем
            ItemWrapperTestUtil.generateShippingUnit(15.0, 15.0, 15.0, 1.5, null, null, quiteRecentTs);
        var stage2ShippingUnit =
            ItemWrapperTestUtil.generateShippingUnit(75.0, 75.0, 75.0, 17.0, null, null, mostRecentTs);

        var businessItem = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            BUSINESS_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "172", businessShippingUnit));
        var stage2Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE2_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "171", stage2ShippingUnit));

        fromIrisItemRepository.insertBatch(stage2Item, businessItem);

        // Запускаем пересчет золота.
        sskuToRefreshRepository.enqueueAll(
            List.of(BUSINESS_KEY, STAGE2_KEY),
            MdmEnqueueReason.CHANGED_IRIS_ITEM
        );
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(2);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        Map<ShopSkuKey, ReferenceItemWrapper> goldenItems =
            referenceItemRepository.findByIds(List.of(BUSINESS_KEY, STAGE2_KEY, STAGE3_KEY1, STAGE3_KEY2))
                .stream()
                .collect(Collectors.toMap(ItemWrapper::getShopSkuKey, Function.identity()));
        Assertions.assertThat(goldenItems).doesNotContainKey(BUSINESS_KEY);
        Assertions.assertThat(goldenItems.get(STAGE2_KEY).getCombinedItemShippingUnit())
            .isEqualTo(stage2ShippingUnit.build());
        // Входящие в бизнес-группу итемы должны иметь общие бизнес-ВГХ. Это самые свежие ВГХ, взятые
        // из бизнес-юнита + добавленный параметр из mdm.ssku_silver_param_values.
        businessShippingUnit.setWeightNetMg(
            MdmIrisPayload.Int64Value.newBuilder()
                .setValue(900000)
                .setUpdatedTs(sskuSilverTs)
                .build()
        );

        Assertions.assertThat(goldenItems.get(STAGE3_KEY1).getCombinedItemShippingUnit())
            .isEqualTo(businessShippingUnit.build());
        Assertions.assertThat(goldenItems.get(STAGE3_KEY2).getCombinedItemShippingUnit())
            .isEqualTo(businessShippingUnit.build());
    }

    @Test
    public void testCalculateGoldenItemShouldLoadGroupByBusinessKeyAndCalculateGoldenForGroupExceptThoseNotFromEOX() {
        // Подготовим данные.
        // 0. Проставим нужные рубильники.
        storageKeyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        storageKeyValueService.putValue(MdmProperties.USE_SSKU_SILVER_PARAM_VALUES_IN_GOLDEN_CALC, true);

        // 1. Создадим бизнес-группы, но сделаем так, что один из сервисов не пришёл из ЕОХ и мы про него не знаем.
        prepareBusinessGroup(false);
        sskuExistenceRepository.markExistence(STAGE3_KEY2, false);

        // 2. Сгенерим SSKU мастер-данные.
        ShopSkuKey offerForStage2 = new ShopSkuKey(STAGE2_KEY.getSupplierId(), STAGE2_KEY.getShopSku());
        MasterData masterData1 = new MasterData();
        masterData1.setShopSkuKey(offerForStage2);
        masterData1.setCustomsCommodityCode("50600980031");
        masterData1.setItemShippingUnit(ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0, 10.0, 1.0, null, null).build());

        ShopSkuKey offerForStage3Key1 = new ShopSkuKey(STAGE3_KEY1.getSupplierId(), STAGE3_KEY1.getShopSku());
        MasterData masterData2 = new MasterData();
        masterData2.setShopSkuKey(offerForStage3Key1);
        masterData2.setItemShippingUnit(ItemWrapperTestUtil.generateShippingUnit(20.0, 20.0, 20.0, 2.0, null, null).build());

        ShopSkuKey offerForStage3Key2 = new ShopSkuKey(STAGE3_KEY2.getSupplierId(), STAGE3_KEY2.getShopSku());
        MasterData masterData3 = new MasterData();
        masterData3.setShopSkuKey(offerForStage3Key2);
        masterData3.setItemShippingUnit(ItemWrapperTestUtil.generateShippingUnit(30.0, 30.0, 30.0, 3.0, null, null).build());

        masterDataRepository.insertBatch(masterData1, masterData2, masterData3);

        // 3. Создадим маппинги.
        ModelKey key = new ModelKey(1L, 100L);
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key).setShopSkuKey(offerForStage2).setUpdateStamp(1L));
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key).setShopSkuKey(offerForStage3Key1).setUpdateStamp(2L));
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key).setShopSkuKey(offerForStage3Key2).setUpdateStamp(3L));

        // 4. Добавим данные в mdm.ssku_silver_param_value по сервисному-ключу.
        // В итоговое золото должен попасть параметр weightNetKg.
        int sskuSilverTs = 2000;
        MdmIrisPayload.ShippingUnit.Builder eoxShippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            23.0, 23.0, 23.0, 2.3, 0.9, null, sskuSilverTs);
        silverSskuRepository.insertOrUpdateSsku(createSskuSilverParamValuesFromShippingUnit(
            eoxShippingUnit.build(), STAGE3_KEY1, MasterDataSourceType.SUPPLIER, String.valueOf(SUPPLIER_ID)));

        // 5. Добавим в очередь бизнес-ssku и сервисную со 2-й стадии.
        // В итоге должна подгрузиться вся бизнес-группа по бизнес-ключу и золото должно быть одним на группу.
        long quiteRecentTs = 3;
        long mostRecentTs = 4;

        // Должно прорасти в золото.
        var businessShippingUnit = // Предположим, это ВГХ по бизнесу от ЕОХ или других не ИРИСовых источников в будущем
            ItemWrapperTestUtil.generateShippingUnit(15.0, 15.0, 15.0, 1.5, null, null, quiteRecentTs);
        var stage2ShippingUnit =
            ItemWrapperTestUtil.generateShippingUnit(75.0, 75.0, 75.0, 17.0, null, null, mostRecentTs);

        var businessItem = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            BUSINESS_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "172", businessShippingUnit));
        var stage2Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            STAGE2_KEY, MdmIrisPayload.MasterDataSource.WAREHOUSE, "171", stage2ShippingUnit));

        fromIrisItemRepository.insertBatch(stage2Item, businessItem);

        // Запускаем пересчет золота.
        sskuToRefreshRepository.enqueueAll(
            List.of(BUSINESS_KEY, STAGE2_KEY),
            MdmEnqueueReason.CHANGED_IRIS_ITEM
        );
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(2);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        Map<ShopSkuKey, ReferenceItemWrapper> goldenItems =
            referenceItemRepository.findByIds(List.of(BUSINESS_KEY, STAGE2_KEY, STAGE3_KEY1, STAGE3_KEY2))
                .stream()
                .collect(Collectors.toMap(ItemWrapper::getShopSkuKey, Function.identity()));
        Assertions.assertThat(goldenItems).doesNotContainKey(BUSINESS_KEY);
        Assertions.assertThat(goldenItems.get(STAGE2_KEY).getCombinedItemShippingUnit())
            .isEqualTo(stage2ShippingUnit.build());
        // Входящие в бизнес-группу итемы должны иметь общие бизнес-ВГХ. Это самые свежие ВГХ, взятые
        // из бизнес-юнита + добавленный параметр из mdm.ssku_silver_param_values.
        businessShippingUnit.setWeightNetMg(
            MdmIrisPayload.Int64Value.newBuilder()
                .setValue(900000)
                .setUpdatedTs(sskuSilverTs)
                .build()
        );

        Assertions.assertThat(goldenItems.get(STAGE3_KEY1).getCombinedItemShippingUnit())
            .isEqualTo(businessShippingUnit.build());
        Assertions.assertThat(goldenItems).doesNotContainKey(STAGE3_KEY2); // золото для не-ЕОХового сервиса скипается.
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
        mdmSupplierRepository.insertBatch(business, stage2Partner, stage3Partner1, stage3Partner2);
    }

    private ReferenceItemWrapper getItem(ShopSkuKey key,
                                         Double length, Double width, Double height,
                                         Double weightGross, Double weightNet, Double weightTare,
                                         long ts) {
        MdmIrisPayload.Item.Builder itemBuilder = MdmIrisPayload.Item.newBuilder()
            .setItemId(ItemWrapperTestUtil.getMdmIdentifier(key));

        itemBuilder.addInformationBuilder()
            .setSource(
                MdmIrisPayload.Associate.newBuilder()
                    .setId("1")
                    .setType(MdmIrisPayload.MasterDataSource.WAREHOUSE)
            )
            .setItemShippingUnit(
                ItemWrapperTestUtil.generateShippingUnit(length, width, height, weightGross, weightNet, weightTare, ts)
                    .setConfiguraion(MdmIrisPayload.ShippingUnitConfiguration.ITEM)
            );
        var result = new ReferenceItemWrapper()
            .setReferenceItem(itemBuilder.build());
        result.setProcessed(false);

        return result;
    }
}
