package ru.yandex.market.mbo.mdm.common.masterdata.validator.block;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ItemBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.TestBlockCreationUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.ValueCommentBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.ItemBlockValidationResult;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields;
import ru.yandex.market.mboc.common.utils.ErrorInfo;

public class MinShipmentBlockValidatorTest {
    private static final MinShipmentBlockValidator VALIDATOR = new MinShipmentBlockValidator();

    private static ItemBlock createMinShipmentBlock(int value) {
        return TestBlockCreationUtil.createNumericMdmParamValueBlock(
            KnownMdmParams.MIN_SHIPMENT,
            BigDecimal.valueOf(value)
        );
    }

    private static ErrorInfo createError(int value) {
        return MbocErrors.get().excelValueMustBeInRange(
            SskuMasterDataFields.MIN_SHIPMENT,
            String.valueOf(value),
            String.valueOf(MinShipmentBlockValidator.MIN_SHIPMENT_MIN),
            String.valueOf(MinShipmentBlockValidator.MIN_SHIPMENT_MAX)
        );
    }

    @Test
    public void whenMinShipmentIsInRangeShouldReturnNoErrors() {
        int value = (MinShipmentBlockValidator.MIN_SHIPMENT_MIN + MinShipmentBlockValidator.MIN_SHIPMENT_MAX) / 2;
        ItemBlock block = createMinShipmentBlock(value);
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of(block));

        ItemBlockValidationResult expectedResult = new ItemBlockValidationResult(block, List.of());

        Assertions.assertThat(VALIDATOR.validateValue(value)).isEmpty();
        Assertions.assertThat(VALIDATOR.validateBlock(block, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenMinShipmentTooLargeShouldReturnAppropriateError() {
        int value = MinShipmentBlockValidator.MIN_SHIPMENT_MAX + 5;
        ItemBlock block = createMinShipmentBlock(value);
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of(block));

        ErrorInfo expectedError = createError(value);
        ItemBlockValidationResult expectedResult = new ItemBlockValidationResult(block, List.of(expectedError));

        Assertions.assertThat(VALIDATOR.validateValue(value)).contains(expectedError);
        Assertions.assertThat(VALIDATOR.validateBlock(block, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenMinShipmentTooSmallShouldReturnAppropriateError() {
        int value = MinShipmentBlockValidator.MIN_SHIPMENT_MIN - 2;
        ItemBlock block = createMinShipmentBlock(value);
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of(block));

        ErrorInfo expectedError = createError(value);
        ItemBlockValidationResult expectedResult = new ItemBlockValidationResult(block, List.of(expectedError));

        Assertions.assertThat(VALIDATOR.validateValue(value)).contains(expectedError);
        Assertions.assertThat(VALIDATOR.validateBlock(block, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenMinShipmentIsZeroReturnNoError() {
        ItemBlock block = createMinShipmentBlock(MasterData.NO_VALUE);
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of(block));

        ItemBlockValidationResult expectedResult = new ItemBlockValidationResult(block, List.of());

        Assertions.assertThat(VALIDATOR.validateValue(MasterData.NO_VALUE)).isEmpty();
        Assertions.assertThat(VALIDATOR.validateBlock(block, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenNoBLockReturnNoError() {
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of());
        ItemBlockValidationResult expectedResult = new ItemBlockValidationResult(null, List.of());
        Assertions.assertThat(VALIDATOR.validateBlock(null, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenNotSupportedBlockReturnNoError() {
        ValueCommentBlock shelfLifeBlock =
            TestBlockCreationUtil.createShelfLifeBlock(TimeInUnits.UNLIMITED, "", Instant.EPOCH);
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of(shelfLifeBlock));

        Assertions.assertThat(VALIDATOR.validateBlock(shelfLifeBlock, validationData))
            .isEqualTo(new ItemBlockValidationResult(shelfLifeBlock, List.of()));
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData))
            .isEqualTo(new ItemBlockValidationResult(null, List.of()));
    }
}
