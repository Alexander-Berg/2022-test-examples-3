package ru.yandex.market.mbo.mdm.common.masterdata.model.goldenblocks;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

/**
 * @author dmserebr
 * @date 19/02/2020
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ItemWrapperToItemBlocksConversionTest {
    @Test
    public void testConvertEmptyItem() {
        ShopSkuKey shopSkuKey = new ShopSkuKey(1, "test");
        ReferenceItemWrapper itemWrapper = new ReferenceItemWrapper(shopSkuKey);

        Assertions.assertThat(
            ItemWrapperHelper.joinFromBlocks(shopSkuKey, ItemWrapperHelper.splitIntoBlocks(itemWrapper)))
            .isEqualTo(itemWrapper);
    }

    @Test
    public void testConvertItemWithValidWarehouseAndSupplierInformation() {
        ShopSkuKey shopSkuKey = new ShopSkuKey(1, "test");

        MdmIrisPayload.ReferenceInformation warehouseInformation1 = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(MdmIrisPayload.Associate.newBuilder()
                .setId("145")
                .setType(MdmIrisPayload.MasterDataSource.WAREHOUSE))
            .setItemShippingUnit(
                ItemWrapperTestUtil.generateShippingUnit(10.0, 12.0, 14.0, 1.0, null, null))
            .build();

        MdmIrisPayload.ReferenceInformation warehouseInformation2 = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(MdmIrisPayload.Associate.newBuilder()
                .setId("147")
                .setType(MdmIrisPayload.MasterDataSource.WAREHOUSE))
            .setItemShippingUnit(
                ItemWrapperTestUtil.generateShippingUnit(11.0, 12.0, 14.0, 1.0, null, null))
            .build();

        MdmIrisPayload.ReferenceInformation supplierInformation = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(MdmIrisPayload.Associate.newBuilder()
                .setId("1")
                .setType(MdmIrisPayload.MasterDataSource.SUPPLIER))
            .setItemShippingUnit(
                ItemWrapperTestUtil.generateShippingUnit(20.0, 22.0, 24.0, 2.0, 5.0, null))
            .build();

        ReferenceItemWrapper itemWrapper = new ReferenceItemWrapper(shopSkuKey);
        itemWrapper.setReferenceItem(itemWrapper.getItem().toBuilder()
            .addInformation(warehouseInformation1)
            .addInformation(warehouseInformation2)
            .addInformation(supplierInformation)
            .build());

        Assertions.assertThat(
            ItemWrapperHelper.joinFromBlocks(shopSkuKey, ItemWrapperHelper.splitIntoBlocks(itemWrapper)))
            .isEqualTo(itemWrapper);
    }

    @Test
    public void testConvertItemWithInvalidInformation() {
        // Should keep invalid blocks as well - they should be further handled by validators.
        ShopSkuKey shopSkuKey = new ShopSkuKey(1, "test");

        MdmIrisPayload.ReferenceInformation warehouseInformation1 = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(MdmIrisPayload.Associate.newBuilder()
                .setId("145")
                .setType(MdmIrisPayload.MasterDataSource.WAREHOUSE))
            .setItemShippingUnit(
                ItemWrapperTestUtil.generateShippingUnit(10.0, 12.0, 14.0, 1.0, null, null)) // valid vgh
            .build();

        MdmIrisPayload.ReferenceInformation warehouseInformation2 = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(MdmIrisPayload.Associate.newBuilder()
                .setId("147")
                .setType(MdmIrisPayload.MasterDataSource.WAREHOUSE))
            .setItemShippingUnit(
                ItemWrapperTestUtil.generateShippingUnit(10.0, 11.0, 13.0, 0.0, null, 5.0)) // invalid vgh, good tare
            .build();

        MdmIrisPayload.ReferenceInformation supplierInformation = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(MdmIrisPayload.Associate.newBuilder()
                .setId("1")
                .setType(MdmIrisPayload.MasterDataSource.SUPPLIER))
            .setItemShippingUnit(
                ItemWrapperTestUtil.generateShippingUnit(20.0, 22.0, 24.0, null, 0.0, null)) // invalid vgh & tare
            .build();

        FromIrisItemWrapper itemWrapper = new FromIrisItemWrapper(shopSkuKey);
        itemWrapper.setReferenceItem(itemWrapper.getItem().toBuilder()
            .addInformation(warehouseInformation1)
            .addInformation(warehouseInformation2)
            .addInformation(supplierInformation)
            .build());

        ReferenceItemWrapper expectedItemWrapper = new ReferenceItemWrapper(shopSkuKey);
        expectedItemWrapper.setReferenceItem(expectedItemWrapper.getItem().toBuilder()
            .addInformation(warehouseInformation1)
            .addInformation(warehouseInformation2)
            .addInformation(supplierInformation)
            .build());

        ReferenceItemWrapper result = ItemWrapperHelper.joinFromBlocks(shopSkuKey,
            ItemWrapperHelper.splitIntoBlocks(itemWrapper));
        Assertions.assertThat(result).isEqualTo(expectedItemWrapper);
    }
}
