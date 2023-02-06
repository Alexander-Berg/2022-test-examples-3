package ru.yandex.market.mboc.common.masterdata.parsing.validator;

import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.BlocksToMasterDataMergerImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.MasterDataIntoBlocksSplitterImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.ExistingCCCodeCacheMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.CachedItemBlockValidationContextProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionsValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.AdditionalValidationBlocksProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.BoxCountValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.BusinessPartBlocksValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.CustomsCommodityCodeBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.DeliveryTimeBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.DimensionsBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.DocumentRegNumbersBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.GTINValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.GuaranteePeriodBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.LifeTimeBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.ManufacturerCountriesBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.MasterDataBlocksValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.MinShipmentBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.QuantumOfSupplyBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.ServicePartBlocksValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.ShelfLifeBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.TransportUnitBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.VetisGuidsBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.WeightNetValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.WeightTareValidator;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.parsing.MasterDataValidator;
import ru.yandex.market.mboc.common.masterdata.services.category.MdmCategorySettingsService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mboc.common.utils.MdmProperties;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * @author dmserebr
 * @date 12/09/2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ShippingUnitValidationTest {

    private static final long SEED = 15498763092L;
    private static final String VALUE_NOT_IN_RANGE_ERROR_CODE = MbocErrors.get()
        .excelValueMustBeInRange("", "0", "0", "0")
        .getErrorCode();
    private static final String WEIGHT_DIMENSIONS_INCONSISTENT_ERROR_CODE = MbocErrors.get()
        .excelWeightDimensionsInconsistent("", 0, 0, 0, 0)
        .getErrorCode();

    private MasterData masterData;
    private MasterDataValidator masterDataValidator;

    @Before
    public void initData() {
        StorageKeyValueServiceMock keyValueService = new StorageKeyValueServiceMock();
        keyValueService.putValue(MdmProperties.SIZE_LONG_MAX_VALUE_KEY, 220L);
        keyValueService.putValue(MdmProperties.SIZE_MIDDLE_MAX_VALUE_KEY, 120L);
        keyValueService.putValue(MdmProperties.SIZE_SHORT_MAX_VALUE_KEY, 80L);
        keyValueService.putValue(MdmProperties.WEIGHT_MAX_VALUE_KEY, 100L);

        masterData = MasterDataValidationTest.generateValidMasterData(TestDataUtils.defaultRandom(SEED));

        MdmCategorySettingsService mdmCategorySettingsService = Mockito.mock(MdmCategorySettingsService.class);
        CategoryCachingService categoryCachingService = new CategoryCachingServiceMock();
        MdmParamCache mdmParamCache = TestMdmParamUtils
            .createParamCacheMock(TestMdmParamUtils.createDefaultKnownMdmParams());

        var servicePartValidationService = new ServicePartBlocksValidationService(
            new MinShipmentBlockValidator(),
            new TransportUnitBlockValidator(),
            new DeliveryTimeBlockValidator(),
            new QuantumOfSupplyBlockValidator()
        );

        var weightDimensionsValidator = new WeightDimensionsValidator(categoryCachingService);
        var cachedItemBlockValidationContextProvider =
            new CachedItemBlockValidationContextProviderImpl(keyValueService);

        var dimensionsBlockValidator = new DimensionsBlockValidator(
            weightDimensionsValidator,
            cachedItemBlockValidationContextProvider
        );

        var weightNetValidator = new WeightNetValidator(
            weightDimensionsValidator,
            cachedItemBlockValidationContextProvider
        );

        var weightTareValidator = new WeightTareValidator(
            weightDimensionsValidator,
            cachedItemBlockValidationContextProvider
        );

        BeruId beruId = new BeruIdMock(10000, 10001);
        ExistingCCCodeCacheMock existingCCCodeCacheMock = new ExistingCCCodeCacheMock();

        var customsCommodityCodeBlockValidator =
            new CustomsCommodityCodeBlockValidator(beruId, keyValueService, existingCCCodeCacheMock);

        var businessDataBlocksValidationService = new BusinessPartBlocksValidationService(
            new ShelfLifeBlockValidator(categoryCachingService),
            new LifeTimeBlockValidator(mdmCategorySettingsService, categoryCachingService),
            new GuaranteePeriodBlockValidator(mdmCategorySettingsService, categoryCachingService),
            new BoxCountValidator(),
            new GTINValidator(),
            new ManufacturerCountriesBlockValidator(new StorageKeyValueServiceMock()),
            new VetisGuidsBlockValidator(),
            new DocumentRegNumbersBlockValidator(),
            dimensionsBlockValidator,
            weightNetValidator,
            weightTareValidator,
            customsCommodityCodeBlockValidator
        );

        var masterDataBlocksValidationService = new MasterDataBlocksValidationService(
            new ShelfLifeBlockValidator(categoryCachingService),
            new LifeTimeBlockValidator(mdmCategorySettingsService, categoryCachingService),
            new GuaranteePeriodBlockValidator(mdmCategorySettingsService, categoryCachingService),
            new BoxCountValidator(),
            customsCommodityCodeBlockValidator,
            new DeliveryTimeBlockValidator(),
            new GTINValidator(),
            new ManufacturerCountriesBlockValidator(new StorageKeyValueServiceMock()),
            new MinShipmentBlockValidator(),
            new QuantumOfSupplyBlockValidator(),
            new TransportUnitBlockValidator(),
            new VetisGuidsBlockValidator(),
            new DocumentRegNumbersBlockValidator(),
            dimensionsBlockValidator,
            weightNetValidator,
            weightTareValidator
        );

        masterDataValidator = new MasterDataValidator(
            new MasterDataIntoBlocksSplitterImpl(mdmParamCache),
            servicePartValidationService,
            businessDataBlocksValidationService,
            masterDataBlocksValidationService,
            new BlocksToMasterDataMergerImpl(),
            new AdditionalValidationBlocksProvider(mdmParamCache));
    }

    @Test
    public void testValidateShipmentUnitOk() {
        masterData.setItemShippingUnit(ItemWrapperTestUtil.generateShippingUnit(
            1.0, 1.2, 1.5, 0.02, 0.015, null).build());
        Assertions.assertThat(masterDataValidator.validateMasterData(masterData)).isEmpty();

        masterData.setItemShippingUnit(
            ItemWrapperTestUtil.generateShippingUnit(0.012, 40.0, 1.0, 0.005, 0.005, null).build());
        Assertions.assertThat(masterDataValidator.validateMasterData(masterData)).isEmpty();

        masterData.setItemShippingUnit(
            ItemWrapperTestUtil.generateShippingUnit(110.0, 200.0, 50.0, 90.0, 80.0, null).build());
        Assertions.assertThat(masterDataValidator.validateMasterData(masterData)).isEmpty();
    }

    @Test
    public void testIncompleteDimensions() {
        masterData.setItemShippingUnit(MdmIrisPayload.ShippingUnit.newBuilder()
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000L).build()).build());

        Assertions.assertThat(masterDataValidator.validateMasterData(masterData).stream().map(ErrorInfo::toString))
            .containsExactlyInAnyOrder("Значения размеров в колонке 'Габариты в сантиметрах с учетом упаковки' " +
                    "должны быть заполнены для всех трех измерений",
                "Отсутствует значение для колонки 'Вес в упаковке в килограммах'");
    }

    @Test
    public void testValidateShipmentUnitOutOfBounds() {
        masterData.setItemShippingUnit(ItemWrapperTestUtil.generateShippingUnit(
            0.001, 0.005, 6000.0, 0.000002, 0.000003, null).build());
        List<ErrorInfo> errorInfos = masterDataValidator.validateMasterData(masterData);

        assertSoftly(softly -> {
            softly.assertThat(errorInfos).hasSize(5);

            softly.assertThat(errorInfos.stream().map(ErrorInfo::getErrorCode).collect(Collectors.toList()))
                .containsOnly(VALUE_NOT_IN_RANGE_ERROR_CODE);
            softly.assertThat(errorInfos.stream().map(ErrorInfo::getLevel).collect(Collectors.toList()))
                .containsOnly(ErrorInfo.Level.ERROR);

            softly.assertThat(errorInfos.get(0).toString())
                .isEqualTo("Значение '6000' для колонки 'Высота в упаковке в сантиметрах' " +
                    "должно быть в диапазоне 0.01 - 220");
            softly.assertThat(errorInfos.get(1).toString())
                .isEqualTo("Значение '0.005' для колонки 'Ширина в упаковке в сантиметрах' " +
                    "должно быть в диапазоне 0.01 - 120");
            softly.assertThat(errorInfos.get(2).toString())
                .isEqualTo("Значение '0.001' для колонки 'Длина в упаковке в сантиметрах' " +
                    "должно быть в диапазоне 0.01 - 80");

            softly.assertThat(errorInfos.get(3).toString())
                .isEqualTo("Значение '0.000002' для колонки 'Вес в упаковке в килограммах' " +
                    "должно быть в диапазоне 0.00001 - 100");
            softly.assertThat(errorInfos.get(4).toString())
                .isEqualTo("Значение '0.000003' для колонки 'Вес в килограммах без упаковки (нетто)' " +
                    "должно быть в диапазоне 0.00001 - 100");
        });
    }

    @Test
    public void testDensityTooHigh() {
        masterData.setItemShippingUnit(
            ItemWrapperTestUtil.generateShippingUnit(0.1, 1.0, 10.0, 0.02, 0.03, null).build());
        List<ErrorInfo> errorInfos = masterDataValidator.validateMasterData(masterData);

        assertSoftly(softly -> {
            softly.assertThat(errorInfos).hasSize(2);

            softly.assertThat(errorInfos.stream().map(ErrorInfo::getErrorCode).collect(Collectors.toList()))
                .containsOnly(WEIGHT_DIMENSIONS_INCONSISTENT_ERROR_CODE);
            softly.assertThat(errorInfos.stream().map(ErrorInfo::getLevel).collect(Collectors.toList()))
                .containsOnly(ErrorInfo.Level.ERROR);

            softly.assertThat(errorInfos.get(0).toString()).isEqualTo(
                "Значение веса '0.02' в колонке 'Вес в упаковке в килограммах' " +
                    "не соответствует размерам (Д/Ш/В: 0.1/1/10) в сантиметрах. " +
                    "Проверьте корректность веса и размеров");
            softly.assertThat(errorInfos.get(1).toString())
                .isEqualTo("Значение веса '0.03' в колонке 'Вес в килограммах без упаковки (нетто)' " +
                    "не соответствует размерам (Д/Ш/В: 0.1/1/10) в сантиметрах. " +
                    "Проверьте корректность веса и размеров");
        });
    }

    @Test
    public void testDensityTooLow() {
        masterData.setItemShippingUnit(
            ItemWrapperTestUtil.generateShippingUnit(220.0, 120.0, 80.0, 0.9, 0.8, null).build());
        List<ErrorInfo> errorInfos = masterDataValidator.validateMasterData(masterData);

        assertSoftly(softly -> {
            softly.assertThat(errorInfos).hasSize(2);

            softly.assertThat(errorInfos.stream().map(ErrorInfo::getErrorCode).collect(Collectors.toList()))
                .containsOnly(WEIGHT_DIMENSIONS_INCONSISTENT_ERROR_CODE);
            softly.assertThat(errorInfos.stream().map(ErrorInfo::getLevel).collect(Collectors.toList()))
                .containsOnly(ErrorInfo.Level.ERROR);

            softly.assertThat(errorInfos.get(0).toString()).isEqualTo(
                "Значение веса '0.9' в колонке 'Вес в упаковке в килограммах' " +
                    "не соответствует размерам (Д/Ш/В: 220/120/80) в сантиметрах. " +
                    "Проверьте корректность веса и размеров");
            softly.assertThat(errorInfos.get(1).toString())
                .isEqualTo("Значение веса '0.8' в колонке 'Вес в килограммах без упаковки (нетто)' " +
                    "не соответствует размерам (Д/Ш/В: 220/120/80) в сантиметрах. " +
                    "Проверьте корректность веса и размеров");
        });
    }

    /**
     * Check that no divisions by zero are encountered.
     */
    @Test
    public void testZeroValues() {
        masterData.setItemShippingUnit(
            ItemWrapperTestUtil.generateShippingUnit(0.0, 0.0, 0.0, 0.0, 0.0, null).build());
        List<ErrorInfo> errorInfos = masterDataValidator.validateMasterData(masterData);

        assertSoftly(softly -> {
            softly.assertThat(errorInfos).hasSize(5);
            softly.assertThat(errorInfos.stream().map(ErrorInfo::getErrorCode).collect(Collectors.toList()))
                .containsOnly(VALUE_NOT_IN_RANGE_ERROR_CODE);
        });
    }
}
