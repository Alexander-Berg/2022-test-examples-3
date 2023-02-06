package ru.yandex.market.mbo.mdm.common.masterdata.services.iris;

import java.time.Instant;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.SupplierDqScore;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SupplierDqScoreRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

/**
 * @author dmserebr
 * @date 02/09/2020
 */
public class SetDqScoreOnGoldenItemTest extends GoldenItemServiceImplTestBase {
    private static final int SUPPLIER_1 = 123;
    private static final int SUPPLIER_2 = 1234;
    private static final String WAREHOUSE_SOURCE_ID_1 = "12345";
    private static final String WAREHOUSE_SOURCE_ID_2 = "23456";

    @Autowired
    private SupplierDqScoreRepository supplierDqScoreRepository;

    @Before
    public void before() throws Exception {
        setUp();
        mdmSupplierRepository.insertBatch(
            new MdmSupplier().setType(MdmSupplierType.THIRD_PARTY).setId(SUPPLIER_1),
            new MdmSupplier().setType(MdmSupplierType.THIRD_PARTY).setId(SUPPLIER_2)
        );
    }

    @Test
    public void testAddDqScore() {
        supplierDqScoreRepository.insert(
            createDqScore(MasterDataSourceType.SUPPLIER, String.valueOf(SUPPLIER_1), 200));
        supplierDqScoreRepository.insert(
            createDqScore(MasterDataSourceType.WAREHOUSE, WAREHOUSE_SOURCE_ID_1, 100));

        ShopSkuKey key1 = new ShopSkuKey(SUPPLIER_1, "sku-with-scores");
        ShopSkuKey key2 = new ShopSkuKey(SUPPLIER_2, "sku-no-scores");
        masterDataRepository.insertBatch(createMasterData(key1), createMasterData(key2));

        MdmIrisPayload.ShippingUnit.Builder item1WarehouseShippingUnit1 = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(1000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(1000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(1000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(1000))
            .setConfiguraion(MdmIrisPayload.ShippingUnitConfiguration.ITEM);
        MdmIrisPayload.Item item1FromWarehouse = ItemWrapperTestUtil.createItem(
            key1, MdmIrisPayload.MasterDataSource.WAREHOUSE, WAREHOUSE_SOURCE_ID_1, item1WarehouseShippingUnit1);

        // supplier shipping unit has weight net - it should be included into golden item
        MdmIrisPayload.ShippingUnit.Builder item1SupplierShippingUnit1 = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(20000).setUpdatedTs(1000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(21000).setUpdatedTs(1000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(22000).setUpdatedTs(1000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(3000).setUpdatedTs(1000))
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1200).setUpdatedTs(1000))
            .setConfiguraion(MdmIrisPayload.ShippingUnitConfiguration.ITEM);
        MdmIrisPayload.Item item1FromSupplier = ItemWrapperTestUtil.createItem(
            key1, MdmIrisPayload.MasterDataSource.SUPPLIER, String.valueOf(SUPPLIER_1), item1SupplierShippingUnit1);

        MdmIrisPayload.ShippingUnit.Builder item2WarehouseShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(1000))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(11000).setUpdatedTs(1000))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(12000).setUpdatedTs(1000))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(2000).setUpdatedTs(1000))
            .setConfiguraion(MdmIrisPayload.ShippingUnitConfiguration.ITEM);
        MdmIrisPayload.Item item2FromWarehouse = ItemWrapperTestUtil.createItem(
            key2, MdmIrisPayload.MasterDataSource.WAREHOUSE, WAREHOUSE_SOURCE_ID_2, item2WarehouseShippingUnit);

        fromIrisItemRepository.insertBatch(
            new FromIrisItemWrapper(item1FromWarehouse),
            new FromIrisItemWrapper(item1FromSupplier),
            new FromIrisItemWrapper(item2FromWarehouse));

        sskuToRefreshRepository.enqueueAll(List.of(key1, key2), MdmEnqueueReason.CHANGED_IRIS_ITEM);
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isEqualTo(2);
        processGoldComputation();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findByIds(List.of(key1, key2));
        Assertions.assertThat(referenceItems).hasSize(2);

        // item 1 has shipping unit with score = 100 at warehouse values and 200 at supplier values
        MdmIrisPayload.ShippingUnit expectedWarehouseShippingUnit1 = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder()
                .setValue(10000).setUpdatedTs(1000).setDqScore(100))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder()
                .setValue(11000).setUpdatedTs(1000).setDqScore(100))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder()
                .setValue(12000).setUpdatedTs(1000).setDqScore(100))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder()
                .setValue(1000).setUpdatedTs(1000).setDqScore(100))
            .setConfiguraion(MdmIrisPayload.ShippingUnitConfiguration.ITEM)
            .build();
        MdmIrisPayload.ShippingUnit expectedSupplierShippingUnit1 = MdmIrisPayload.ShippingUnit.newBuilder()
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder()
                .setValue(1200).setUpdatedTs(1000).setDqScore(200))
            .setConfiguraion(MdmIrisPayload.ShippingUnitConfiguration.ITEM)
            .build();


        ReferenceItemWrapper referenceItem1 = referenceItems.stream()
            .filter(item -> item.getKey().equals(key1)).findFirst().get();
        Assertions.assertThat(referenceItem1.getItem().getInformationCount()).isEqualTo(2);
        Assertions.assertThat(referenceItem1.getItem().getInformation(0).getItemShippingUnit())
            .isEqualTo(expectedWarehouseShippingUnit1);
        Assertions.assertThat(referenceItem1.getItem().getInformation(1).getItemShippingUnit())
            .isEqualTo(expectedSupplierShippingUnit1);

        // item 2 has shipping unit with default score (0)
        MdmIrisPayload.ShippingUnit expectedShippingUnit2 = MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder()
                .setValue(10000).setUpdatedTs(1000).setDqScore(0))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder()
                .setValue(11000).setUpdatedTs(1000).setDqScore(0))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder()
                .setValue(12000).setUpdatedTs(1000).setDqScore(0))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder()
                .setValue(2000).setUpdatedTs(1000).setDqScore(0))
            .setConfiguraion(MdmIrisPayload.ShippingUnitConfiguration.ITEM)
            .build();

        ReferenceItemWrapper referenceItem2 = referenceItems.stream()
            .filter(item -> item.getKey().equals(key2)).findFirst().get();
        Assertions.assertThat(referenceItem2.getItem().getInformationCount()).isEqualTo(1);
        Assertions.assertThat(referenceItem2.getItem().getInformation(0).getItemShippingUnit())
            .isEqualTo(expectedShippingUnit2);
    }

    private static SupplierDqScore createDqScore(MasterDataSourceType sourceType, String sourceId, int score) {
        var result = new SupplierDqScore();
        result.setSourceType(sourceType);
        result.setSourceId(sourceId);
        result.setDqScore(score);
        result.setUpdatedTs(Instant.now());
        return result;
    }
}
