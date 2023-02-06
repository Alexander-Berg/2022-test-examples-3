package ru.yandex.market.mboc.common.masterdata.parsing.validator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.ValidationContext;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.BlocksToMasterDataMergerImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.MasterDataIntoBlocksSplitterImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepositoryMock;
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
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.ValueCommentBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.VetisGuidsBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.WeightNetValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.WeightTareValidator;
import ru.yandex.market.mbo.mdm.common.service.MdmParameterValueCachingServiceMock;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.model.TransportUnit;
import ru.yandex.market.mboc.common.masterdata.parsing.MasterDataValidator;
import ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields;
import ru.yandex.market.mboc.common.masterdata.parsing.utils.TimeInUnitsConverter;
import ru.yandex.market.mboc.common.masterdata.services.category.MdmCategorySettingsService;
import ru.yandex.market.mboc.common.masterdata.services.category.MdmCategorySettingsServiceImpl;
import ru.yandex.market.mboc.common.masterdata.services.category.TimeParamLimits;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.category.CategoryCachingService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.ErrorInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * @author jkt on 25.01.19.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class MasterDataValidationTest {
    protected static final MinShipmentBlockValidator MIN_SHIPMENT_VALIDATOR = new MinShipmentBlockValidator();
    protected static final DeliveryTimeBlockValidator DELIVERY_TIME_VALIDATOR = new DeliveryTimeBlockValidator();
    protected static final QuantumOfSupplyBlockValidator QUANTUM_OF_SUPPLY_VALIDATOR =
        new QuantumOfSupplyBlockValidator();

    protected static final String VALUE_REQUIRED_ERROR_CODE = MbocErrors.get()
        .excelValueIsRequired("")
        .getErrorCode();
    protected static final String VALUE_NOT_IN_RANGE_ERROR_CODE = MbocErrors.get()
        .excelValueMustBeInRange("", "0", "0", "0")
        .getErrorCode();
    private static final long SEED = 15498763092L;
    private static final String MANUFACTURER_COUNTRY_ERROR_CODE = MbocErrors.get()
        .mdManufacturerCountryValue("", "", "")
        .getErrorCode();
    private static final String CUSSTOMS_COMMODITY_CODE_ERROR_CODE = MbocErrors.get()
        .mdCustomsCommodityCode("", "")
        .getErrorCode();
    private static final String COMMENT_INVALID_CHARS_ERROR_CODE = MbocErrors.get()
        .mdCommentHasInvalidCharacters("", "")
        .getErrorCode();
    private static final String INVALID_GUID_ERROR_CODE = MbocErrors.get()
        .excelInvalidGuid("", "")
        .getErrorCode();
    private static final String INVALID_FORMAT_ERROR_CODE = MbocErrors.get()
        .excelInvalidValueFormat("", "")
        .getErrorCode();

    private static final String INVALID_COUNTRY = "INVALID_COUNTRY";
    private static final TimeInUnits INVALID_SHELF_LIFE_LONG = new TimeInUnits(
        ShelfLifeBlockValidator.DEFAULT_TIME_LIMIT.getMaxValue().getTimeInUnit(TimeInUnits.TimeUnit.DAY) + 1000,
        TimeInUnits.TimeUnit.DAY
    );
    private static final TimeInUnits INVALID_SHELF_LIFE_SHORT = new TimeInUnits(
        ShelfLifeBlockValidator.DEFAULT_TIME_LIMIT.getMinValue().getTimeInUnit(TimeInUnits.TimeUnit.DAY) - 1,
        TimeInUnits.TimeUnit.DAY
    );
    private static final TimeInUnits INVALID_LIFE_TIME_LONG = new TimeInUnits(
        LifeTimeBlockValidator.DEFAULT_TIME_LIMIT.getMaxValue().getTimeInUnit(TimeInUnits.TimeUnit.DAY) + 1000,
        TimeInUnits.TimeUnit.DAY
    );
    private static final TimeInUnits INVALID_LIFE_TIME_SHORT = new TimeInUnits(
        LifeTimeBlockValidator.DEFAULT_TIME_LIMIT.getMinValue().getTimeInUnit(TimeInUnits.TimeUnit.DAY) - 1,
        TimeInUnits.TimeUnit.DAY
    );
    private static final TimeInUnits INVALID_GUARANTEE_PERIOD_LONG = new TimeInUnits(
        GuaranteePeriodBlockValidator.DEFAULT_TIME_LIMIT.getMaxValue().getTimeInUnit(TimeInUnits.TimeUnit.DAY) + 1000,
        TimeInUnits.TimeUnit.DAY
    );
    private static final TimeInUnits INVALID_GUARANTEE_PERIOD_SHORT = new TimeInUnits(
        GuaranteePeriodBlockValidator.DEFAULT_TIME_LIMIT.getMinValue().getTimeInUnit(TimeInUnits.TimeUnit.DAY) - 1,
        TimeInUnits.TimeUnit.DAY
    );
    private static final TimeInUnits BLACKLISTED_TIME = new TimeInUnits(999, TimeInUnits.TimeUnit.DAY);
    private static final String BLACKLISTED_TIME_STRING_REPRESENTATION = "999 дней";
    private static final int INVALID_MIN_SHIPMENT = MinShipmentBlockValidator.MIN_SHIPMENT_MAX + 100;
    private static final int INVALID_TRANSPORT_UNIT_SIZE = TransportUnitBlockValidator.TRANSPORT_UNIT_SIZE_MAX + 100;
    private static final int INVALID_DELIVERY_TIME = DeliveryTimeBlockValidator.DELIVERY_DAYS_MAX + 10;
    private static final int INVALID_QUANTUM_OF_SUPPLY = QuantumOfSupplyBlockValidator.QUANTUM_MAX + 100;
    private static final String INVALID_CUSTOMS_COMMODITY_CODE = "INVALID_CUSTOMS_COMMODITY_CODE";
    private static final String INVALID_GUARANTEE_PERIOD_COMMENT = "李秀英";

    protected MasterData masterData;
    protected MasterDataValidator masterDataValidator;
    protected StorageKeyValueService storageKeyValueService;
    protected MdmParamCache mdmParamCache;
    protected MdmParameterValueCachingServiceMock parameterValueCachingServiceMock;
    protected MdmCategorySettingsService categorySettingsService;
    protected CategoryParamValueRepositoryMock categoryParamValueRepository;
    protected ShelfLifeBlockValidator shelfLifeBlockValidator;
    protected LifeTimeBlockValidator lifeTimeBlockValidator;
    protected GuaranteePeriodBlockValidator guaranteePeriodBlockValidator;
    protected BeruIdMock beruIdMock;
    protected CategoryCachingService categoryCachingServiceMock;

    private EnhancedRandom defaultRandom;

    static MasterData generateValidMasterData(EnhancedRandom defaultRandom) {
        return TestDataUtils.generateMasterData("Test_sku", 123, defaultRandom);
    }

    @Before
    public void initData() {
        defaultRandom = TestDataUtils.defaultRandom(SEED);
        masterData = generateValidMasterData(defaultRandom);

        storageKeyValueService = new StorageKeyValueServiceMock();
        mdmParamCache = TestMdmParamUtils.createParamCacheMock(TestMdmParamUtils.createDefaultKnownMdmParams());
        parameterValueCachingServiceMock = new MdmParameterValueCachingServiceMock();
        categoryParamValueRepository = new CategoryParamValueRepositoryMock();
        categorySettingsService = new MdmCategorySettingsServiceImpl(parameterValueCachingServiceMock,
            null, categoryParamValueRepository);
        categoryCachingServiceMock = new CategoryCachingServiceMock();

        shelfLifeBlockValidator = new ShelfLifeBlockValidator(categoryCachingServiceMock);
        lifeTimeBlockValidator = new LifeTimeBlockValidator(categorySettingsService, categoryCachingServiceMock);
        guaranteePeriodBlockValidator =
            new GuaranteePeriodBlockValidator(categorySettingsService, categoryCachingServiceMock);

        var servicePartValidationService = new ServicePartBlocksValidationService(
            MIN_SHIPMENT_VALIDATOR,
            new TransportUnitBlockValidator(),
            DELIVERY_TIME_VALIDATOR,
            QUANTUM_OF_SUPPLY_VALIDATOR
        );
        var weightDimensionsValidator = new WeightDimensionsValidator(categoryCachingServiceMock);
        var cachedItemBlockValidationContextProvider =
            new CachedItemBlockValidationContextProviderImpl(storageKeyValueService);

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
            new CustomsCommodityCodeBlockValidator(beruId, storageKeyValueService, existingCCCodeCacheMock);

        var businessPartValidationService = new BusinessPartBlocksValidationService(
            shelfLifeBlockValidator,
            lifeTimeBlockValidator,
            guaranteePeriodBlockValidator,
            new BoxCountValidator(),
            new GTINValidator(),
            new ManufacturerCountriesBlockValidator(storageKeyValueService),
            new VetisGuidsBlockValidator(),
            new DocumentRegNumbersBlockValidator(),
            dimensionsBlockValidator,
            weightNetValidator,
            weightTareValidator,
            customsCommodityCodeBlockValidator
        );

        var masterDataBlocksValidationService = new MasterDataBlocksValidationService(
            shelfLifeBlockValidator,
            lifeTimeBlockValidator,
            guaranteePeriodBlockValidator,
            new BoxCountValidator(),
            customsCommodityCodeBlockValidator,
            DELIVERY_TIME_VALIDATOR,
            new GTINValidator(),
            new ManufacturerCountriesBlockValidator(storageKeyValueService),
            MIN_SHIPMENT_VALIDATOR,
            QUANTUM_OF_SUPPLY_VALIDATOR,
            new TransportUnitBlockValidator(),
            new VetisGuidsBlockValidator(),
            new DocumentRegNumbersBlockValidator(),
            dimensionsBlockValidator,
            weightNetValidator,
            weightTareValidator
        );

        beruIdMock = new BeruIdMock();
        masterDataValidator = new MasterDataValidator(
            new MasterDataIntoBlocksSplitterImpl(mdmParamCache),
            servicePartValidationService,
            businessPartValidationService,
            masterDataBlocksValidationService,
            new BlocksToMasterDataMergerImpl(),
            new AdditionalValidationBlocksProvider(mdmParamCache));
    }

    @Test
    public void whenMasterDataIsValidShouldNotFail() {
        List<ErrorInfo> errors = masterDataValidator.validateMasterData(masterData);

        assertThat(errors).isEmpty();
    }

    @Test
    public void whenValidatingCountryShouldFailInvalid() {
        masterData.getManufacturerCountries().add(INVALID_COUNTRY);

        List<ErrorInfo> countryErrors = ManufacturerCountriesBlockValidator.validateManufacturerCountries(
            masterData.getManufacturerCountries(), true
        );
        List<ErrorInfo> masterDataErrors = masterDataValidator.validateMasterData(masterData);

        assertSoftly(softly -> {
            softly.assertThat(countryErrors).hasSize(1);
            softly.assertThat(countryErrors).containsExactlyInAnyOrderElementsOf(masterDataErrors);

            ErrorInfo error = countryErrors.get(0);

            softly.assertThat(error.getErrorCode()).isEqualTo(MANUFACTURER_COUNTRY_ERROR_CODE);
            softly.assertThat(error.getLevel()).isEqualTo(ErrorInfo.Level.ERROR);
            softly.assertThat(error.toString()).contains(SskuMasterDataFields.MANUFACTURER_COUNTRY);
            softly.assertThat(error.toString()).contains(INVALID_COUNTRY);
        });
    }

    @Test
    public void whenManufacturerCountriesIsEmptyShouldFail() {
        masterData.getManufacturerCountries().clear();

        List<ErrorInfo> errors = masterDataValidator.validateMasterData(masterData);

        assertSoftly(softly -> {
            softly.assertThat(errors).hasSize(1);

            ErrorInfo error = errors.get(0);

            softly.assertThat(error.getErrorCode()).isEqualTo(VALUE_REQUIRED_ERROR_CODE);
            softly.assertThat(error.getLevel()).isEqualTo(ErrorInfo.Level.ERROR);
            softly.assertThat(error.toString()).contains(SskuMasterDataFields.MANUFACTURER_COUNTRY);
        });
    }

    @Test
    public void whenNotRequiredFieldsMissingShouldNotFail() {
        masterData.setShelfLife(null);
        masterData.setLifeTime(null);
        masterData.setGuaranteePeriod(null);
        masterData.setMinShipment(MasterData.NO_VALUE);
        masterData.setTransportUnitSize(MasterData.NO_VALUE);
        masterData.setQuantityInPack(MasterData.NO_VALUE);
        masterData.setDeliveryTime(MasterData.NO_VALUE);
        masterData.setQuantumOfSupply(MasterData.NO_VALUE);
        masterData.setCustomsCommodityCode(null);
        masterData.setManufacturer(null);

        List<ErrorInfo> errors = masterDataValidator.validateMasterData(masterData);

        assertThat(errors).isEmpty();
    }

    @Test
    @Ignore
    public void whenValidatingShelfLifeShouldFailWithCorrectError() {
        masterData.setShelfLife(INVALID_SHELF_LIFE_LONG);

        List<ErrorInfo> shelfLifeErrors =
            ShelfLifeBlockValidator.validateShelfLifeInDefaultCategory(masterData.getShelfLife());
        List<ErrorInfo> masterDataErrors = masterDataValidator.validateMasterData(masterData);

        assertThat(shelfLifeErrors).hasSize(1);
        ErrorInfo error = shelfLifeErrors.iterator().next();

        assertSoftly(softly -> {
            softly.assertThat(masterDataErrors).containsExactly(error);

            softly.assertThat(error.getErrorCode()).isEqualTo(VALUE_NOT_IN_RANGE_ERROR_CODE);
            softly.assertThat(error.getLevel()).isEqualTo(ErrorInfo.Level.ERROR);
            softly.assertThat(error.toString()).contains(SskuMasterDataFields.SHELF_LIFE);
            softly.assertThat(error.toString()).contains(INVALID_SHELF_LIFE_LONG.getTime() + "");
        });
    }

    @Test
    public void whenShelfLifeOutOfRangeShouldFail() {
        assertSoftly(softly -> {
            softly.assertThat(ShelfLifeBlockValidator.validateShelfLifeInDefaultCategory(INVALID_SHELF_LIFE_SHORT))
                .isNotEmpty();
            softly.assertThat(ShelfLifeBlockValidator.validateShelfLifeInDefaultCategory(INVALID_SHELF_LIFE_LONG))
                .isNotEmpty();
        });
    }

    @Test
    @Ignore
    public void whenValidatingLifeTimeShouldFailWithCorrectError() {
        masterData.setLifeTime(INVALID_LIFE_TIME_LONG);

        List<ErrorInfo> lifeTimeErrors =
            LifeTimeBlockValidator.validateLifeTimeInDefaultCategory(masterData.getLifeTime());
        List<ErrorInfo> masterDataErrors = masterDataValidator.validateMasterData(masterData);

        assertThat(lifeTimeErrors).hasSize(1);
        ErrorInfo error = lifeTimeErrors.iterator().next();

        assertSoftly(softly -> {
            softly.assertThat(masterDataErrors).containsExactly(error);

            softly.assertThat(error.getErrorCode()).isEqualTo(VALUE_NOT_IN_RANGE_ERROR_CODE);
            softly.assertThat(error.getLevel()).isEqualTo(ErrorInfo.Level.ERROR);
            softly.assertThat(error.toString()).contains(SskuMasterDataFields.LIFE_TIME);
            softly.assertThat(error.toString()).contains(INVALID_LIFE_TIME_LONG.getTime() + "");
        });
    }

    @Test
    public void whenValidatingGuaranteePeriodCommentShouldFailWithCorrectError() {
        masterData.setGuaranteePeriodComment(INVALID_GUARANTEE_PERIOD_COMMENT);

        Optional<ErrorInfo> error =
            GuaranteePeriodBlockValidator.validateGuaranteePeriodComment(masterData.getGuaranteePeriodComment());
        List<ErrorInfo> masterDataErrors = masterDataValidator.validateMasterData(masterData);

        assertThat(error).isPresent();
        ErrorInfo errorInfo = error.get();

        assertSoftly(softly -> {
            softly.assertThat(masterDataErrors).containsExactly(errorInfo);

            softly.assertThat(errorInfo.getErrorCode()).isEqualTo(COMMENT_INVALID_CHARS_ERROR_CODE);
            softly.assertThat(errorInfo.getLevel()).isEqualTo(ErrorInfo.Level.ERROR);
            softly.assertThat(errorInfo.toString()).contains(SskuMasterDataFields.GUARANTEE_PERIOD_COMMENT);
        });
    }

    @Test
    public void whenLifeTimeOutOfRangeShouldFail() {
        assertSoftly(softly -> {
            softly.assertThat(LifeTimeBlockValidator.validateLifeTimeInDefaultCategory(INVALID_LIFE_TIME_SHORT))
                .isNotEmpty();
            softly.assertThat(LifeTimeBlockValidator.validateLifeTimeInDefaultCategory(INVALID_LIFE_TIME_LONG))
                .isNotEmpty();
        });

    }

    @Test
    @Ignore
    public void whenValidatingGuaranteePeriodShouldFailWithCorrectError() {
        masterData.setGuaranteePeriod(INVALID_GUARANTEE_PERIOD_LONG);

        List<ErrorInfo> guaranteePeriodErrors =
            GuaranteePeriodBlockValidator.validateGuaranteePeriodInDefaultCategory(masterData.getGuaranteePeriod());
        List<ErrorInfo> masterDataErrors = masterDataValidator.validateMasterData(masterData);

        assertThat(guaranteePeriodErrors).hasSize(1);
        ErrorInfo error = guaranteePeriodErrors.iterator().next();

        assertSoftly(softly -> {
            softly.assertThat(masterDataErrors).containsExactly(error);

            softly.assertThat(error.getErrorCode()).isEqualTo(VALUE_NOT_IN_RANGE_ERROR_CODE);
            softly.assertThat(error.getLevel()).isEqualTo(ErrorInfo.Level.ERROR);
            softly.assertThat(error.toString()).contains(SskuMasterDataFields.GUARANTEE_PERIOD);
            softly.assertThat(error.toString()).contains(INVALID_GUARANTEE_PERIOD_LONG.getTime() + "");
        });
    }

    @Test
    public void whenGuaranteePeriodOutOfRangeShouldFail() {
        assertSoftly(softly -> {
            softly.assertThat(
                    GuaranteePeriodBlockValidator.validateGuaranteePeriodInDefaultCategory(INVALID_GUARANTEE_PERIOD_SHORT))
                .isNotEmpty();
            softly.assertThat(
                    GuaranteePeriodBlockValidator.validateGuaranteePeriodInDefaultCategory(INVALID_GUARANTEE_PERIOD_SHORT))
                .isNotEmpty();
        });

    }

    @Test
    public void whenValidatingMinShipmentShouldFailWithCorrectError() {
        masterData.setMinShipment(INVALID_MIN_SHIPMENT);

        Optional<ErrorInfo> minShipmentError = MIN_SHIPMENT_VALIDATOR.validateValue(masterData.getMinShipment());
        List<ErrorInfo> masterDataErrors = masterDataValidator.validateMasterData(masterData);

        assertThat(minShipmentError).isPresent();
        ErrorInfo error = minShipmentError.get();

        assertSoftly(softly -> {
            softly.assertThat(masterDataErrors).containsExactly(error);

            softly.assertThat(error.getErrorCode()).isEqualTo(VALUE_NOT_IN_RANGE_ERROR_CODE);
            softly.assertThat(error.getLevel()).isEqualTo(ErrorInfo.Level.ERROR);
            softly.assertThat(error.toString()).contains(SskuMasterDataFields.MIN_SHIPMENT);
            softly.assertThat(error.toString()).contains(INVALID_MIN_SHIPMENT + "");
        });
    }

    @Test
    public void whenValidatingTransportUnitSizeShouldFailWithCorrectError() {
        masterData.setTransportUnitSize(INVALID_TRANSPORT_UNIT_SIZE);

        Optional<ErrorInfo> transportUnitSizeError = TransportUnitBlockValidator.validateTransportUnit(
            masterData.getTransportUnit()
        );
        List<ErrorInfo> masterDataErrors = masterDataValidator.validateMasterData(masterData);

        assertThat(transportUnitSizeError).isPresent();
        ErrorInfo error = transportUnitSizeError.get();

        assertSoftly(softly -> {
            softly.assertThat(masterDataErrors).containsExactly(error);
            softly.assertThat(error.getLevel()).isEqualTo(ErrorInfo.Level.ERROR);
            softly.assertThat(error.toString()).contains(SskuMasterDataFields.TRANSPORT_UNIT_SIZE);
            softly.assertThat(error.toString()).contains(INVALID_TRANSPORT_UNIT_SIZE + "");

            if (!masterData.hasQuantityInPack()) {
                softly.assertThat(error.getErrorCode()).isEqualTo(VALUE_NOT_IN_RANGE_ERROR_CODE);
            } else {
                softly.assertThat(error.getErrorCode()).isEqualTo(INVALID_FORMAT_ERROR_CODE);
            }
        });
    }

    @Test
    public void whenTransportUnitSizeOutOfRangeShouldFail() {
        assertSoftly(softly -> {
            softly.assertThat(
                TransportUnitBlockValidator.validateTransportUnit(new TransportUnit(
                    TransportUnitBlockValidator.TRANSPORT_UNIT_SIZE_MIN - 2, 0))
            ).isPresent();
            softly.assertThat(
                TransportUnitBlockValidator.validateTransportUnit(new TransportUnit(
                    TransportUnitBlockValidator.TRANSPORT_UNIT_SIZE_MAX + 1, 0))
            ).isPresent();
        });

    }

    @Test
    public void whenValidatingDeliveryTimeShouldFailWithCorrectError() {
        masterData.setDeliveryTime(INVALID_DELIVERY_TIME);

        Optional<ErrorInfo> deliveryTimeError = DELIVERY_TIME_VALIDATOR.validateValue(masterData.getDeliveryTime());
        List<ErrorInfo> masterDataErrors = masterDataValidator.validateMasterData(masterData);

        assertThat(deliveryTimeError).isPresent();
        ErrorInfo error = deliveryTimeError.get();

        assertSoftly(softly -> {
            softly.assertThat(masterDataErrors).containsExactly(error);

            softly.assertThat(error.getErrorCode()).isEqualTo(VALUE_NOT_IN_RANGE_ERROR_CODE);
            softly.assertThat(error.getLevel()).isEqualTo(ErrorInfo.Level.ERROR);
            softly.assertThat(error.toString()).contains(SskuMasterDataFields.DELIVERY_TIME);
            softly.assertThat(error.toString()).contains(INVALID_DELIVERY_TIME + "");
        });
    }

    @Test
    public void whenValidatingQuantumOfSupplyShouldFailWithCorrectError() {
        masterData.setQuantumOfSupply(INVALID_QUANTUM_OF_SUPPLY);

        Optional<ErrorInfo> quantumError = QUANTUM_OF_SUPPLY_VALIDATOR.validateValue(masterData.getQuantumOfSupply());
        List<ErrorInfo> masterDataErrors = masterDataValidator.validateMasterData(masterData);

        assertThat(quantumError).isPresent();
        ErrorInfo error = quantumError.get();

        assertSoftly(softly -> {
            softly.assertThat(masterDataErrors).containsExactly(error);

            softly.assertThat(error.getErrorCode()).isEqualTo(VALUE_NOT_IN_RANGE_ERROR_CODE);
            softly.assertThat(error.getLevel()).isEqualTo(ErrorInfo.Level.ERROR);
            softly.assertThat(error.toString()).contains(SskuMasterDataFields.QUANTUM_OF_SUPPLY);
            softly.assertThat(error.toString()).contains(INVALID_QUANTUM_OF_SUPPLY + "");
        });
    }

    @Test
    public void whenValidatingCustomsCommodityCodeShouldFailWithCorrectError() {
        masterData.setCustomsCommodityCode(INVALID_CUSTOMS_COMMODITY_CODE);

        Optional<ErrorInfo> quantumError = CustomsCommodityCodeBlockValidator.validateCustomsCommodityCode(
            masterData.getCustomsCommodityCode()
        );
        List<ErrorInfo> masterDataErrors = masterDataValidator.validateMasterData(masterData);

        assertThat(quantumError).isPresent();
        ErrorInfo error = quantumError.get();

        assertSoftly(softly -> {
            softly.assertThat(masterDataErrors).containsExactly(error);

            softly.assertThat(error.getErrorCode()).isEqualTo(CUSSTOMS_COMMODITY_CODE_ERROR_CODE);
            softly.assertThat(error.getLevel()).isEqualTo(ErrorInfo.Level.ERROR);
            softly.assertThat(error.toString()).contains(SskuMasterDataFields.CUSTOMS_COMMODITY_CODE);
            softly.assertThat(error.toString()).contains(INVALID_CUSTOMS_COMMODITY_CODE);
        });
    }

    @Test
    public void whenVetisGuidsValidShouldNotFail() {
        masterData.setVetisGuids(Collections.singletonList("3ecd3446-d0ac-4953-ac71-3d22033a4aa2"));
        Assertions.assertThat(masterDataValidator.validateMasterData(masterData)).isEmpty();
    }

    @Test
    public void whenVetisGuidsInvalidShouldFail() {
        masterData.setVetisGuids(Arrays.asList("AC3431A7-fc44-449B-A0CF-BE0B512F1405", "test123", "!!"));
        List<ErrorInfo> errorInfos = masterDataValidator.validateMasterData(masterData);

        assertSoftly(softly -> {
            softly.assertThat(errorInfos).hasSize(2);
            softly.assertThat(errorInfos.stream().map(ErrorInfo::getErrorCode).collect(Collectors.toList()))
                .containsOnly(INVALID_GUID_ERROR_CODE);
            softly.assertThat(errorInfos.get(0).getLevel()).isEqualTo(ErrorInfo.Level.ERROR);
            softly.assertThat(errorInfos.get(0).toString()).isEqualTo("Значение в колонке " +
                "'GUID в системе \"Меркурий\"' не соответвует правилам записи GUID: 'test123'");
            softly.assertThat(errorInfos.get(1).toString()).isEqualTo("Значение в колонке " +
                "'GUID в системе \"Меркурий\"' не соответвует правилам записи GUID: '!!'");
        });
    }

    @Test
    public void whenGuaranteePeriodLongerThanLifeTimeShouldFail() {
        final String guaranteePeriodInDaysStringRepresentation = "1095 дней";
        final String lifeTimeInDaysStringRepresentation = "730 дней";
        TimeInUnits lifeTime = new TimeInUnits(730, TimeInUnits.TimeUnit.DAY);
        TimeInUnits guaranteePeriod = new TimeInUnits(1095, TimeInUnits.TimeUnit.DAY);

        masterData.setLifeTime(lifeTime)
            .setGuaranteePeriod(guaranteePeriod);

        ErrorInfo expectedError = MbocErrors.get().guaranteePeriodLongerThanLifeTime(
            guaranteePeriodInDaysStringRepresentation,
            lifeTimeInDaysStringRepresentation
        );

        List<ErrorInfo> errors = masterDataValidator.validateMasterData(masterData);

        Assertions.assertThat(errors).containsOnly(expectedError);
    }

    @Test
    public void whenTimeIsUnlimitedInDefaultCategoryShouldNotFail() {
        String fieldName = SskuMasterDataFields.SHELF_LIFE;
        TimeParamLimits defaultCategoryLimits = ShelfLifeBlockValidator.DEFAULT_TIME_LIMIT;
        List<ErrorInfo> errors =
            ValueCommentBlockValidator.validateTimeValue(fieldName, TimeInUnits.UNLIMITED, defaultCategoryLimits);
        assertThat(errors).isEmpty();
    }

    @Test
    public void whenTimeIsUnlimitedInCategoryWhereUnlimitedNotAllowedShouldFail() {
        String fieldName = SskuMasterDataFields.SHELF_LIFE;
        TimeParamLimits categoryLimits = new TimeParamLimits(
            new TimeInUnits(3, TimeInUnits.TimeUnit.DAY),
            new TimeInUnits(10, TimeInUnits.TimeUnit.YEAR),
            false
        );
        List<ErrorInfo> error =
            ValueCommentBlockValidator.validateTimeValue(fieldName, TimeInUnits.UNLIMITED, categoryLimits);
        ErrorInfo expectedError = MbocErrors.get().excelValueMustBeInRange(
            fieldName,
            TimeInUnitsConverter.convertToStringRussian(TimeInUnits.UNLIMITED),
            TimeInUnitsConverter.convertToStringRussian(categoryLimits.getMinValue()),
            TimeInUnitsConverter.convertToStringRussian(categoryLimits.getMaxValue())
        );
        assertThat(error).containsOnly(expectedError);
    }

    @Test
    public void testMasterDataFiltration() {
        ShopSkuKey shopSkuKey = new ShopSkuKey(12, "213");
        long categoryId = 825;

        MasterData beforeFiltration = new MasterData();
        beforeFiltration.copyDataFieldsFrom(masterData);
        beforeFiltration.setShopSkuKey(shopSkuKey)
            .setCategoryId(categoryId)
            .setShelfLife(INVALID_SHELF_LIFE_LONG)
            .setDeliveryTime(INVALID_DELIVERY_TIME)
            .setManufacturerCountries(List.of(INVALID_COUNTRY))
            .setGuaranteePeriod(INVALID_GUARANTEE_PERIOD_SHORT);

        MasterData expected = new MasterData();
        expected.copyDataFieldsFrom(masterData);
        expected.setShopSkuKey(shopSkuKey)
            .setCategoryId(categoryId)
            .setShelfLife(null)
            .setShelfLifeComment(null)
            .setDeliveryTime(0)
            .setManufacturerCountries(null)
            .setGuaranteePeriod(null)
            .setGuaranteePeriodComment(null);

        MasterData filtered =
            masterDataValidator.filterMasterData(beforeFiltration, ValidationContext.EMPTY_CONTEXT);
        Assertions.assertThat(filtered).isEqualTo(expected);
    }

    @Test
    public void whenContainsNotExistingDocumentRegNumbersShouldFail() {
        Set<QualityDocument> existingDocuments = Stream.of("124", "678", "901")
            .map(regNumber -> new QualityDocument().setRegistrationNumber(regNumber))
            .collect(Collectors.toSet());
        var validationContext = new ValidationContext(0L, List.of(), false, existingDocuments, List.of());

        masterData.setRegNumbers(List.of("901", "125", "678"));

        Assertions.assertThat(masterDataValidator.validateMasterData(masterData, validationContext))
            .containsOnly(MbocErrors.get().documentWithRegistrationNumbersDoesNotExist("125"));
    }
}
