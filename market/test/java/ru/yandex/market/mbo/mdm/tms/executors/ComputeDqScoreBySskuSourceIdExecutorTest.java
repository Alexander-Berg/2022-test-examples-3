package ru.yandex.market.mbo.mdm.tms.executors;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.SupplierDqScore;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.FromIrisItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SupplierDqScoreRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionBlockValidationService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;

/**
 * @author dmserebr
 * @date 24/09/2020
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ComputeDqScoreBySskuSourceIdExecutorTest extends MdmBaseDbTestClass {
    private ComputeDqScoreBySskuSourceIdExecutor executor;

    private static final int SUPPLIER_ID_1 = 100;
    private static final int SUPPLIER_ID_2 = 200;
    private static final String SHOP_SKU_1 = "1001";
    private static final String SHOP_SKU_2 = "1002";
    private static final String SHOP_SKU_3 = "1003";
    private static final String WAREHOUSE_ID_1 = "wh123";

    @Autowired
    private FromIrisItemRepository fromIrisItemRepository;

    @Autowired
    private SupplierDqScoreRepository supplierDqScoreRepository;

    @Autowired
    private WeightDimensionBlockValidationService weightDimensionBlockValidationService;

    private StorageKeyValueServiceMock storageKeyValueService;

    @Before
    public void before() {
        storageKeyValueService = new StorageKeyValueServiceMock();
        storageKeyValueService.putValue(MdmProperties.SHOULD_UPDATE_DQ_SCORES, true);

        executor = new ComputeDqScoreBySskuSourceIdExecutor(
            fromIrisItemRepository,
            supplierDqScoreRepository,
            weightDimensionBlockValidationService,
            storageKeyValueService);

        executor.setBatchSize(4);
    }

    @Test
    public void test() {
        // valid item
        FromIrisItemWrapper itemWrapper1 = createTestItem(SUPPLIER_ID_1, SHOP_SKU_1,
            MdmIrisPayload.MasterDataSource.WAREHOUSE, WAREHOUSE_ID_1,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0, 10.0, 1.0, 2.0, null));

        // invalid item
        FromIrisItemWrapper itemWrapper2 = createTestItem(SUPPLIER_ID_1, SHOP_SKU_2,
            MdmIrisPayload.MasterDataSource.WAREHOUSE, WAREHOUSE_ID_1,
            ItemWrapperTestUtil.generateShippingUnit(10000.0, 10.0, 10.0, 1.0, 2.0, null));

        // valid item
        FromIrisItemWrapper itemWrapper3 = createTestItem(SUPPLIER_ID_1, SHOP_SKU_3,
            MdmIrisPayload.MasterDataSource.WAREHOUSE, WAREHOUSE_ID_1,
            ItemWrapperTestUtil.generateShippingUnit(20.0, 10.0, 10.0, 1.0, 2.0, null));

        // valid item
        FromIrisItemWrapper itemWrapper4 = createTestItem(SUPPLIER_ID_2, SHOP_SKU_1,
            MdmIrisPayload.MasterDataSource.WAREHOUSE, WAREHOUSE_ID_1,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0, 10.0, 1.0, 2.0, null));

        // invalid item
        FromIrisItemWrapper itemWrapper5 = createTestItem(SUPPLIER_ID_1, SHOP_SKU_1,
            MdmIrisPayload.MasterDataSource.SUPPLIER, String.valueOf(SUPPLIER_ID_1),
            ItemWrapperTestUtil.generateShippingUnit(10000.0, 10.0, 10.0, 1.0, 2.0, null));

        // valid item
        FromIrisItemWrapper itemWrapper6 = createTestItem(SUPPLIER_ID_1, SHOP_SKU_1,
            MdmIrisPayload.MasterDataSource.MEASUREMENT, WAREHOUSE_ID_1,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0, 10.0, 1.0, 2.0, null));

        fromIrisItemRepository.insertBatch(
            itemWrapper1, itemWrapper2, itemWrapper3, itemWrapper4, itemWrapper5, itemWrapper6);

        executor.execute();

        List<SupplierDqScore> scores = supplierDqScoreRepository.findAll();


        Assertions.assertThat(scores)
            .usingElementComparatorIgnoringFields("updatedTs")
            .containsExactlyInAnyOrder(
                new SupplierDqScore(MasterDataSourceType.WAREHOUSE, WAREHOUSE_ID_1, 250),
                new SupplierDqScore(MasterDataSourceType.MEASUREMENT, WAREHOUSE_ID_1, 1),
                new SupplierDqScore(MasterDataSourceType.SUPPLIER, String.valueOf(SUPPLIER_ID_1), 1000)
            );
    }

    private static FromIrisItemWrapper createTestItem(int supplierId,
                                                      String shopSku,
                                                      MdmIrisPayload.MasterDataSource sourceType,
                                                      String sourceId,
                                                      MdmIrisPayload.ShippingUnit.Builder shippingUnit) {

        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(
            new ShopSkuKey(supplierId, shopSku), sourceType, sourceId, shippingUnit);
        var itemWrapper = new FromIrisItemWrapper(new ShopSkuKey(supplierId, shopSku));
        itemWrapper.setSingleInformationItem(item);
        return itemWrapper;
    }
}
