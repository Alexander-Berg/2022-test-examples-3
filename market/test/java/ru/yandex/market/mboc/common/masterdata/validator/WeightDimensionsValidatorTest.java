package ru.yandex.market.mboc.common.masterdata.validator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.DimensionsBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ItemBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.WeightNetBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.WeightTareBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.CategorySettingsBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.ItemBlockValidationContext;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionsValidator;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.utils.ErrorInfo;

/**
 * @author dmserebr
 * @date 24/04/2020
 */
@SuppressWarnings("checkstyle:magicNumber")
public class WeightDimensionsValidatorTest {
    private final ItemBlockValidationContext validationContext = new ItemBlockValidationContext(
        220L, // size long max value
        120L, // size middle max value
        80L,  // size short max value
        100L, // weight max value
        Map.of());
    private CategoryCachingServiceMock categoryCachingService;
    private WeightDimensionsValidator validator;

    @Before
    public void setUp() throws Exception {
        categoryCachingService = new CategoryCachingServiceMock();
        validator = new WeightDimensionsValidator(categoryCachingService);
    }

    @Test
    public void testDimensionsValid() {
        DimensionsBlock dimensionsBlock = new DimensionsBlock(
            createMdmParamValue(new BigDecimal(10)),
            createMdmParamValue(new BigDecimal(12)),
            createMdmParamValue(new BigDecimal(14)),
            createMdmParamValue(new BigDecimal(1)));

        List<ErrorInfo> errors = validator.validateDimensionsBlock(dimensionsBlock, validationContext);

        Assertions.assertThat(errors).isEmpty();
    }

    @Test
    public void testZeroDimensions() {
        DimensionsBlock dimensionsBlock = new DimensionsBlock(
            createMdmParamValue(new BigDecimal(0)),
            createMdmParamValue(new BigDecimal(0)),
            createMdmParamValue(new BigDecimal(0)),
            createMdmParamValue(new BigDecimal(0)));

        List<ErrorInfo> errors = validator.validateDimensionsBlock(dimensionsBlock, validationContext);

        Assertions.assertThat(errors.stream().map(ErrorInfo::toString))
            .containsExactlyInAnyOrder(
                "Значение '0' для колонки 'Длина в упаковке в сантиметрах' должно быть в диапазоне 0.01 - 220",
                "Значение '0' для колонки 'Ширина в упаковке в сантиметрах' должно быть в диапазоне 0.01 - 120",
                "Значение '0' для колонки 'Высота в упаковке в сантиметрах' должно быть в диапазоне 0.01 - 80",
                "Значение '0' для колонки 'Вес в упаковке в килограммах' должно быть в диапазоне 0.00001 - 100");
    }

    @Test
    public void testWeightGrossMissing() {
        DimensionsBlock dimensionsBlock = new DimensionsBlock(
            createMdmParamValue(new BigDecimal(10)),
            createMdmParamValue(new BigDecimal(12)),
            createMdmParamValue(new BigDecimal(14)),
            null);

        List<ErrorInfo> errors = validator.validateDimensionsBlock(dimensionsBlock, validationContext);

        Assertions.assertThat(errors.stream().map(ErrorInfo::toString))
            .containsExactlyInAnyOrder("Отсутствует значение для колонки 'Вес в упаковке в килограммах'");
    }

    @Test
    public void testLengthAndWeightGrossMissing() {
        DimensionsBlock dimensionsBlock = new DimensionsBlock(
            null,
            createMdmParamValue(new BigDecimal(12)),
            createMdmParamValue(new BigDecimal(14)),
            null);

        List<ErrorInfo> errors = validator.validateDimensionsBlock(dimensionsBlock, validationContext);

        Assertions.assertThat(errors.stream().map(ErrorInfo::toString))
            .containsExactlyInAnyOrder("Значения размеров в колонке 'Габариты в сантиметрах с учетом упаковки' " +
                    "должны быть заполнены для всех трех измерений",
                "Отсутствует значение для колонки 'Вес в упаковке в килограммах'");
    }

    @Test
    public void testValuesNotInRangeAndWeightGrossMissing() {
        DimensionsBlock dimensionsBlock = new DimensionsBlock(
            createMdmParamValue(new BigDecimal(10000)),
            createMdmParamValue(new BigDecimal(12000)),
            createMdmParamValue(new BigDecimal(14)),
            null);

        List<ErrorInfo> errors = validator.validateDimensionsBlock(dimensionsBlock, validationContext);

        Assertions.assertThat(errors.stream().map(ErrorInfo::toString))
            .containsExactlyInAnyOrder("Отсутствует значение для колонки 'Вес в упаковке в килограммах'",
                "Значение '12000' для колонки 'Ширина в упаковке в сантиметрах' должно быть в диапазоне 0.01 - 220",
                "Значение '10000' для колонки 'Длина в упаковке в сантиметрах' должно быть в диапазоне 0.01 - 120");
    }

    @Test
    public void testMaximumPossibleBoxAndRotate() {
        DimensionsBlock dimensionsBlock1 = new DimensionsBlock(
            createMdmParamValue(new BigDecimal(220)),
            createMdmParamValue(new BigDecimal(120)),
            createMdmParamValue(new BigDecimal(80)),
            createMdmParamValue(new BigDecimal(100)));
        DimensionsBlock dimensionsBlock2 = new DimensionsBlock(
            createMdmParamValue(new BigDecimal(120)),
            createMdmParamValue(new BigDecimal(220)),
            createMdmParamValue(new BigDecimal(80)),
            createMdmParamValue(new BigDecimal(100)));
        DimensionsBlock dimensionsBlock3 = new DimensionsBlock(
            createMdmParamValue(new BigDecimal(80)),
            createMdmParamValue(new BigDecimal(120)),
            createMdmParamValue(new BigDecimal(220)),
            createMdmParamValue(new BigDecimal(100)));
        DimensionsBlock dimensionsBlock4 = new DimensionsBlock(
            createMdmParamValue(new BigDecimal(220)),
            createMdmParamValue(new BigDecimal(80)),
            createMdmParamValue(new BigDecimal(120)),
            createMdmParamValue(new BigDecimal(100)));

        List<ErrorInfo> allErrors = Stream.concat(
            Stream.concat(
                validator.validateDimensionsBlock(dimensionsBlock1, validationContext).stream(),
                validator.validateDimensionsBlock(dimensionsBlock2, validationContext).stream()
            ), Stream.concat(
                validator.validateDimensionsBlock(dimensionsBlock3, validationContext).stream(),
                validator.validateDimensionsBlock(dimensionsBlock4, validationContext).stream()
            )).collect(Collectors.toList());

        Assertions.assertThat(allErrors).isEmpty();
    }

    @Test
    public void testBoxSlightlyBiggerAndHeavier() {
        DimensionsBlock dimensionsBlock = new DimensionsBlock(
            createMdmParamValue(new BigDecimal(121)),
            createMdmParamValue(new BigDecimal(221)),
            createMdmParamValue(new BigDecimal(81)),
            createMdmParamValue(new BigDecimal(101))
        );
        List<ErrorInfo> errors = validator.validateDimensionsBlock(dimensionsBlock, validationContext);

        Assertions.assertThat(errors.stream().map(ErrorInfo::toString))
            .containsExactlyInAnyOrder(
                "Значение '221' для колонки 'Ширина в упаковке в сантиметрах' должно быть в диапазоне 0.01 - 220",
                "Значение '121' для колонки 'Длина в упаковке в сантиметрах' должно быть в диапазоне 0.01 - 120",
                "Значение '81' для колонки 'Высота в упаковке в сантиметрах' должно быть в диапазоне 0.01 - 80",
                "Значение '101' для колонки 'Вес в упаковке в килограммах' должно быть в диапазоне 0.00001 - 100");
    }

    @Test
    public void testEqualDimensions() {
        DimensionsBlock dimensionsBlock = new DimensionsBlock(
            createMdmParamValue(new BigDecimal(300)),
            createMdmParamValue(new BigDecimal(300)),
            createMdmParamValue(new BigDecimal(300)),
            createMdmParamValue(new BigDecimal(100))
        );
        List<ErrorInfo> errors = validator.validateDimensionsBlock(dimensionsBlock, validationContext);

        Assertions.assertThat(errors.stream().map(ErrorInfo::toString))
            .containsExactlyInAnyOrder(
                "Значение '300' для колонки 'Длина в упаковке в сантиметрах' должно быть в диапазоне 0.01 - 220",
                "Значение '300' для колонки 'Ширина в упаковке в сантиметрах' должно быть в диапазоне 0.01 - 120",
                "Значение '300' для колонки 'Высота в упаковке в сантиметрах' должно быть в диапазоне 0.01 - 80");
    }

    @Test
    public void testDensityTooLarge() {
        DimensionsBlock dimensionsBlock = new DimensionsBlock(
            createMdmParamValue(new BigDecimal(1)),
            createMdmParamValue(new BigDecimal(1)),
            createMdmParamValue(new BigDecimal(1)),
            createMdmParamValue(new BigDecimal(100))
        );
        List<ErrorInfo> errors = validator.validateDimensionsBlock(dimensionsBlock, validationContext);

        Assertions.assertThat(errors.stream().map(ErrorInfo::toString))
            .containsExactlyInAnyOrder("Значение веса '100' в колонке 'Вес в упаковке в килограммах' " +
                "не соответствует размерам (Д/Ш/В: 1/1/1) в сантиметрах. " +
                "Проверьте корректность веса и размеров");
    }

    @Test
    public void testWeightNetValid() {
        WeightNetBlock weightNetBlock = new WeightNetBlock(
            createMdmParamValue(new BigDecimal(10)));

        List<ErrorInfo> errors = validator.validateWeightNetBlock(weightNetBlock, validationContext);

        Assertions.assertThat(errors).isEmpty();
    }

    @Test
    public void testWeightNetZero() {
        WeightNetBlock weightNetBlock = new WeightNetBlock(
            createMdmParamValue(new BigDecimal(0)));

        List<ErrorInfo> errors = validator.validateWeightNetBlock(weightNetBlock, validationContext);

        Assertions.assertThat(errors.stream().map(ErrorInfo::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(
                "Значение '0' для колонки 'Вес в килограммах без упаковки (нетто)' " +
                    "должно быть в диапазоне 0.00001 - 100");
    }

    @Test
    public void testWeightNetTooLarge() {
        WeightNetBlock weightNetBlock = new WeightNetBlock(
            createMdmParamValue(new BigDecimal(1000)));

        List<ErrorInfo> errors = validator.validateWeightNetBlock(weightNetBlock, validationContext);

        Assertions.assertThat(errors.stream().map(ErrorInfo::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(
                "Значение '1000' для колонки 'Вес в килограммах без упаковки (нетто)' " +
                    "должно быть в диапазоне 0.00001 - 100");
    }

    @Test
    public void testWeightTareValid() {
        WeightTareBlock weightTareBlock = new WeightTareBlock(
            createMdmParamValue(new BigDecimal(10)));

        List<ErrorInfo> errors = validator.validateWeightTareBlock(weightTareBlock, validationContext);

        Assertions.assertThat(errors).isEmpty();
    }

    @Test
    public void testWeightTareZero() {
        WeightTareBlock weightTareBlock = new WeightTareBlock(
            createMdmParamValue(new BigDecimal(0)));

        List<ErrorInfo> errors = validator.validateWeightTareBlock(weightTareBlock, validationContext);

        Assertions.assertThat(errors.stream().map(ErrorInfo::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(
                "Значение '0' для колонки 'Вес тары в килограммах' должно быть в диапазоне 0.00001 - 100");
    }

    @Test
    public void testWeightTareTooLarge() {
        WeightTareBlock weightTareBlock = new WeightTareBlock(
            createMdmParamValue(new BigDecimal(1000)));

        List<ErrorInfo> errors = validator.validateWeightTareBlock(weightTareBlock, validationContext);

        Assertions.assertThat(errors.stream().map(ErrorInfo::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(
                "Значение '1000' для колонки 'Вес тары в килограммах' должно быть в диапазоне 0.00001 - 100");
    }

    @Test
    public void testWeightNetDensityTooLarge() {
        WeightNetBlock weightNetBlock = new WeightNetBlock(
            createMdmParamValue(new BigDecimal(100)));
        DimensionsBlock dimensionsBlock = new DimensionsBlock(
            createMdmParamValue(new BigDecimal(1)),
            createMdmParamValue(new BigDecimal(1)),
            createMdmParamValue(new BigDecimal(1)),
            createMdmParamValue(new BigDecimal(100))
        );

        var context = new ItemBlockValidationContext(
            220L, // size long max value
            120L, // size middle max value
            80L,  // size short max value
            100L, // weight max value
            Map.of(ItemBlock.BlockType.DIMENSIONS, List.of(dimensionsBlock)));

        List<ErrorInfo> errors = validator.validateWeightNetBlock(weightNetBlock, context);

        Assertions.assertThat(errors.stream().map(ErrorInfo::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(
                "Значение веса '100' в колонке 'Вес в килограммах без упаковки (нетто)' " +
                    "не соответствует размерам (Д/Ш/В: 1/1/1) в сантиметрах. " +
                    "Проверьте корректность веса и размеров");
    }

    @Test
    public void testCategoryLimits() {
        DimensionsBlock dimensionsBlock = new DimensionsBlock(
            createMdmParamValue(new BigDecimal(121)),
            createMdmParamValue(new BigDecimal(221)),
            createMdmParamValue(new BigDecimal(81)),
            createMdmParamValue(new BigDecimal(101))
        );

        CategorySettingsBlock categorySettingsBlock = new CategorySettingsBlock(1L, Map.of(
            KnownMdmParams.SIZE_LONG_MIN_CM, createCategoryParamValue(new BigDecimal(20)),
            KnownMdmParams.SIZE_LONG_MAX_CM, createCategoryParamValue(new BigDecimal(200)),
            KnownMdmParams.SIZE_SHORT_MIN_CM, createCategoryParamValue(new BigDecimal(10)),
            KnownMdmParams.SIZE_MIDDLE_MAX_CM, createCategoryParamValue(new BigDecimal(130))
        ));

        ItemBlockValidationContext categoryValidationContext = new ItemBlockValidationContext(
            220L, // size long max value
            120L, // size middle max value
            80L,  // size short max value
            100L, // weight max value
            Map.of(ItemBlock.BlockType.CATEGORY_SETTINGS, List.of(categorySettingsBlock)));
        List<ErrorInfo> errors = validator.validateDimensionsBlock(dimensionsBlock, categoryValidationContext);

        Assertions.assertThat(errors.stream().map(ErrorInfo::toString))
            .containsExactlyInAnyOrder(
                "Значение '221' для колонки 'Ширина в упаковке в сантиметрах' должно быть в диапазоне 20 - 200",
                "Значение '81' для колонки 'Высота в упаковке в сантиметрах' должно быть в диапазоне 10 - 80",
                "Значение '101' для колонки 'Вес в упаковке в килограммах' должно быть в диапазоне 0.00001 - 100");
    }

    @Test
    public void whenHaveCategoryNameAddItToLimitsErrorMessage() {
        DimensionsBlock dimensionsBlock = new DimensionsBlock(
            createMdmParamValue(new BigDecimal(121)),
            createMdmParamValue(new BigDecimal(221)),
            createMdmParamValue(new BigDecimal(81)),
            createMdmParamValue(new BigDecimal(101))
        );

        categoryCachingService.addCategory(1L, "Радиоактивные отходы");
        CategorySettingsBlock categorySettingsBlock = new CategorySettingsBlock(1L, Map.of(
            KnownMdmParams.SIZE_LONG_MIN_CM, createCategoryParamValue(new BigDecimal(20)),
            KnownMdmParams.SIZE_LONG_MAX_CM, createCategoryParamValue(new BigDecimal(200)),
            KnownMdmParams.SIZE_SHORT_MIN_CM, createCategoryParamValue(new BigDecimal(10)),
            KnownMdmParams.SIZE_MIDDLE_MAX_CM, createCategoryParamValue(new BigDecimal(130))
        ));

        ItemBlockValidationContext categoryValidationContext = new ItemBlockValidationContext(
            220L, // size long max value
            120L, // size middle max value
            80L,  // size short max value
            100L, // weight max value
            Map.of(ItemBlock.BlockType.CATEGORY_SETTINGS, List.of(categorySettingsBlock)));
        List<ErrorInfo> errors = validator.validateDimensionsBlock(dimensionsBlock, categoryValidationContext);

        Assertions.assertThat(errors.stream().map(ErrorInfo::toString))
            .containsExactlyInAnyOrder(
                "Значение '221' для колонки 'Ширина в упаковке в сантиметрах'" +
                    " для товара из категории 'Радиоактивные отходы' должно быть в диапазоне 20 - 200",
                "Значение '81' для колонки 'Высота в упаковке в сантиметрах'" +
                    " для товара из категории 'Радиоактивные отходы' должно быть в диапазоне 10 - 80",
                "Значение '101' для колонки 'Вес в упаковке в килограммах'" +
                    " для товара из категории 'Радиоактивные отходы' должно быть в диапазоне 0.00001 - 100"
            );
    }

    @Test
    public void testCategoryLimitsDisabledForTrustedSources() {
        DimensionsBlock dimensionsBlock = new DimensionsBlock(
            createMdmParamValue(new BigDecimal(119))
                .setMasterDataSourceType(MasterDataSourceType.MEASUREMENT).setMasterDataSourceId("172"),
            createMdmParamValue(new BigDecimal(219))
                .setMasterDataSourceType(MasterDataSourceType.MEASUREMENT).setMasterDataSourceId("172"),
            createMdmParamValue(new BigDecimal(79))
                .setMasterDataSourceType(MasterDataSourceType.MEASUREMENT).setMasterDataSourceId("172"),
            createMdmParamValue(new BigDecimal(99))
                .setMasterDataSourceType(MasterDataSourceType.MEASUREMENT).setMasterDataSourceId("172")
        );
        DimensionsBlock inheritedDimensionsBlock = new DimensionsBlock(
            createMdmParamValue(new BigDecimal(119))
                .setMasterDataSourceType(MasterDataSourceType.MSKU_INHERIT)
                .setMasterDataSourceId("msku:100656629470 supplier_id:542601 shop_sku:1102orange measurement:171"),
            createMdmParamValue(new BigDecimal(219))
                .setMasterDataSourceType(MasterDataSourceType.MSKU_INHERIT)
                .setMasterDataSourceId("msku:100656629470 supplier_id:542601 shop_sku:1102orange measurement:171"),
            createMdmParamValue(new BigDecimal(79))
                .setMasterDataSourceType(MasterDataSourceType.MSKU_INHERIT)
                .setMasterDataSourceId("msku:100656629470 supplier_id:542601 shop_sku:1102orange measurement:171"),
            createMdmParamValue(new BigDecimal(99))
                .setMasterDataSourceType(MasterDataSourceType.MSKU_INHERIT)
                .setMasterDataSourceId("msku:100656629470 supplier_id:542601 shop_sku:1102orange measurement:171")
        );

        CategorySettingsBlock categorySettingsBlock = new CategorySettingsBlock(1L, Map.of(
            KnownMdmParams.SIZE_LONG_MIN_CM, createCategoryParamValue(new BigDecimal(20)),
            KnownMdmParams.SIZE_LONG_MAX_CM, createCategoryParamValue(new BigDecimal(200)),
            KnownMdmParams.SIZE_SHORT_MIN_CM, createCategoryParamValue(new BigDecimal(10)),
            KnownMdmParams.SIZE_MIDDLE_MAX_CM, createCategoryParamValue(new BigDecimal(130))
        ));

        ItemBlockValidationContext categoryValidationContext = new ItemBlockValidationContext(
            220L, // size long max value
            120L, // size middle max value
            80L,  // size short max value
            100L, // weight max value
            Map.of(ItemBlock.BlockType.CATEGORY_SETTINGS, List.of(categorySettingsBlock)));

        Assertions.assertThat(validator.validateDimensionsBlock(dimensionsBlock, categoryValidationContext)).isEmpty();
        Assertions.assertThat(validator.validateDimensionsBlock(inheritedDimensionsBlock, categoryValidationContext))
            .isEmpty();
    }

    @Test
    public void testCategoryLimitsAreActiveForWarehouseSourceType() {
        DimensionsBlock dimensionsBlock = new DimensionsBlock(
            createMdmParamValue(new BigDecimal(119))
                .setMasterDataSourceType(MasterDataSourceType.WAREHOUSE).setMasterDataSourceId("172"),
            createMdmParamValue(new BigDecimal(219))
                .setMasterDataSourceType(MasterDataSourceType.WAREHOUSE).setMasterDataSourceId("172"),
            createMdmParamValue(new BigDecimal(79))
                .setMasterDataSourceType(MasterDataSourceType.WAREHOUSE).setMasterDataSourceId("172"),
            createMdmParamValue(new BigDecimal(99))
                .setMasterDataSourceType(MasterDataSourceType.WAREHOUSE).setMasterDataSourceId("172")
        );

        DimensionsBlock inheritedDimensionsBlock = new DimensionsBlock(
            createMdmParamValue(new BigDecimal(119))
                .setMasterDataSourceType(MasterDataSourceType.MSKU_INHERIT)
                .setMasterDataSourceId("msku:100656629470 supplier_id:542601 shop_sku:1102orange warehouse:171"),
            createMdmParamValue(new BigDecimal(219))
                .setMasterDataSourceType(MasterDataSourceType.MSKU_INHERIT)
                .setMasterDataSourceId("msku:100656629470 supplier_id:542601 shop_sku:1102orange warehouse:171"),
            createMdmParamValue(new BigDecimal(79))
                .setMasterDataSourceType(MasterDataSourceType.MSKU_INHERIT)
                .setMasterDataSourceId("msku:100656629470 supplier_id:542601 shop_sku:1102orange warehouse:171"),
            createMdmParamValue(new BigDecimal(99))
                .setMasterDataSourceType(MasterDataSourceType.MSKU_INHERIT)
                .setMasterDataSourceId("msku:100656629470 supplier_id:542601 shop_sku:1102orange warehouse:171")
        );

        CategorySettingsBlock categorySettingsBlock = new CategorySettingsBlock(1L, Map.of(
            KnownMdmParams.SIZE_LONG_MIN_CM, createCategoryParamValue(new BigDecimal(20)),
            KnownMdmParams.SIZE_LONG_MAX_CM, createCategoryParamValue(new BigDecimal(200)),
            KnownMdmParams.SIZE_SHORT_MIN_CM, createCategoryParamValue(new BigDecimal(10)),
            KnownMdmParams.SIZE_MIDDLE_MAX_CM, createCategoryParamValue(new BigDecimal(130))
        ));

        ItemBlockValidationContext categoryValidationContext = new ItemBlockValidationContext(
            220L, // size long max value
            120L, // size middle max value
            80L,  // size short max value
            100L, // weight max value
            Map.of(ItemBlock.BlockType.CATEGORY_SETTINGS, List.of(categorySettingsBlock)));

        List<ErrorInfo> errors1 = validator.validateDimensionsBlock(dimensionsBlock, categoryValidationContext);
        List<ErrorInfo> errors2 =
            validator.validateDimensionsBlock(inheritedDimensionsBlock, categoryValidationContext);
        Assertions.assertThat(errors1).isEqualTo(errors2);
        Assertions.assertThat(errors1.stream().map(ErrorInfo::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(
                "Значение '219' для колонки 'Ширина в упаковке в сантиметрах' должно быть в диапазоне 20 - 200");
    }

    private MdmParamValue createMdmParamValue(BigDecimal value) {
        var pv = new MdmParamValue();
        pv.setNumeric(value);
        return pv;
    }

    private CategoryParamValue createCategoryParamValue(BigDecimal value) {
        var pv = new CategoryParamValue();
        pv.setNumeric(value);
        return pv;
    }
}
