package ru.yandex.market.ir.autogeneration.common.util;

import io.qameta.allure.Issue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.robot.db.ParameterValueComposer;

@Issue("MARKETIR-9204")
public class OptionUtilsTest {
    private static final long VENDOR_ID_1 = 123;
    private static final long VENDOR_ID_2 = 456;
    private static final String VENDOR_NAME_1 = "vendor 1, vendor 1";
    private static final String VENDOR_NAME_1_EXCEL = "vendor 1 vendor 1";
    private static final String VENDOR_NAME_2 = "vendor 2";
    private static final String VENDOR_NAME_1_ALIAS = "vendor 1 alias";
    private static final String VENDOR_NAME_1_GLOBAL_ALIAS = "vendor 1 global alias";

    private CategoryData categoryData;

    @Before
    public void setUp() throws Exception {
        categoryData = buildCategoryData();
    }

    @Test
    public void getVendorIdByName() {
        Long vendorId = OptionUtils.getVendorId(categoryData, VENDOR_NAME_1_EXCEL);
        Assert.assertEquals((Long) VENDOR_ID_1, vendorId);
    }

    @Test
    public void getVendorIdByAlias() {
        Long vendorId = OptionUtils.getVendorId(categoryData, VENDOR_NAME_1_ALIAS);
        Assert.assertEquals(vendorId, (Long) VENDOR_ID_1);
    }

    @Test
    public void getVendorIdByGlobalAlias() {
        Long vendorId = OptionUtils.getVendorId(categoryData, VENDOR_NAME_1_GLOBAL_ALIAS);
        Assert.assertEquals(vendorId, (Long) VENDOR_ID_1);
    }

    @Test
    public void getVendorIdNoSuchVendor() {
        Long vendorId = OptionUtils.getVendorId(categoryData, "no such vendor");
        Assert.assertNull(vendorId);
    }

    @Test
    public void getVendorIdNotGuru() {
        Long vendorId = OptionUtils.getVendorId(categoryData, VENDOR_NAME_2);
        Assert.assertEquals(vendorId, (Long) VENDOR_ID_2);
    }

    private CategoryData buildCategoryData() {
        MboParameters.Category category = MboParameters.Category.newBuilder()
            .setLeaf(true)
            .addParameter(
                MboParameters.Parameter.newBuilder()
                    .setId(ParameterValueComposer.VENDOR_ID)
                    .setXslName(ParameterValueComposer.VENDOR)
                    .setValueType(MboParameters.ValueType.ENUM)
                    .setParamType(MboParameters.ParameterLevel.MODEL_LEVEL)
                    .setIsUseForGuru(true)
                    .setSkuMode(MboParameters.SKUParameterMode.SKU_NONE)
                    .addOption(
                        MboParameters.Option.newBuilder()
                            .setId(VENDOR_ID_1)
                            .addName(
                                MboParameters.Word.newBuilder()
                                    .setName(VENDOR_NAME_1)
                            )
                            .addAlias(
                                MboParameters.EnumAlias.newBuilder()
                                    .setAlias(
                                        MboParameters.Word.newBuilder()
                                            .setName(VENDOR_NAME_1_ALIAS)
                                    )
                            )
                            .setIsGuruVendor(true)
                    )
                    .addOption(
                        MboParameters.Option.newBuilder()
                            .setId(VENDOR_ID_2)
                            .addName(
                                MboParameters.Word.newBuilder()
                                    .setName(VENDOR_NAME_2)
                            )
                            .setIsGuruVendor(false)
                    )
                    .build()
            )
            .addVendorAlias(
                MboParameters.VendorAlias.newBuilder()
                    .setId(VENDOR_ID_1)
                    .addGlobal(
                        MboParameters.Word.newBuilder()
                            .setName(VENDOR_NAME_1_ALIAS)
                    )
                    .addGlobal(
                        MboParameters.Word.newBuilder()
                            .setName(VENDOR_NAME_1_GLOBAL_ALIAS)
                    )
                    .build()
            )
            .addVendorAlias(
                MboParameters.VendorAlias.newBuilder()
                    .setId(VENDOR_ID_2)
                    .addGlobal(
                        MboParameters.Word.newBuilder()
                            .setName(VENDOR_NAME_2)
                    )
                    .build()
            )
            .build();

        return CategoryData.build(category);
    }
}