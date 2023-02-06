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

public class DeliveryTimeBlockValidatorTest {
    private static final DeliveryTimeBlockValidator VALIDATOR = new DeliveryTimeBlockValidator();

    private static ItemBlock createDeliveryBlock(int value) {
        return TestBlockCreationUtil.createNumericMdmParamValueBlock(
            KnownMdmParams.DELIVERY_TIME,
            BigDecimal.valueOf(value)
        );
    }

    private static ErrorInfo createError(int value) {
        return MbocErrors.get().excelValueMustBeInRange(
            SskuMasterDataFields.DELIVERY_TIME,
            String.valueOf(value),
            String.valueOf(DeliveryTimeBlockValidator.DELIVERY_DAYS_MIN),
            String.valueOf(DeliveryTimeBlockValidator.DELIVERY_DAYS_MAX)
        );
    }

    @Test
    public void whenDeliveryTimeIsInRangeShouldReturnNoErrors() {
        int value = (DeliveryTimeBlockValidator.DELIVERY_DAYS_MIN + DeliveryTimeBlockValidator.DELIVERY_DAYS_MAX) / 2;
        ItemBlock block = createDeliveryBlock(value);
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of(block));

        ItemBlockValidationResult expectedResult = new ItemBlockValidationResult(block, List.of());

        Assertions.assertThat(VALIDATOR.validateValue(value)).isEmpty();
        Assertions.assertThat(VALIDATOR.validateBlock(block, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenDeliveryTimeTooLargeShouldReturnAppropriateError() {
        int value = DeliveryTimeBlockValidator.DELIVERY_DAYS_MAX + 5;
        ItemBlock block = createDeliveryBlock(value);
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of(block));

        ErrorInfo expectedError = createError(value);
        ItemBlockValidationResult expectedResult = new ItemBlockValidationResult(block, List.of(expectedError));

        Assertions.assertThat(VALIDATOR.validateValue(value)).contains(expectedError);
        Assertions.assertThat(VALIDATOR.validateBlock(block, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenDeliveryTimeTooSmallShouldReturnAppropriateError() {
        int value = DeliveryTimeBlockValidator.DELIVERY_DAYS_MIN - 2;
        ItemBlock block = createDeliveryBlock(value);
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of(block));

        ErrorInfo expectedError = createError(value);
        ItemBlockValidationResult expectedResult = new ItemBlockValidationResult(block, List.of(expectedError));

        Assertions.assertThat(VALIDATOR.validateValue(value)).contains(expectedError);
        Assertions.assertThat(VALIDATOR.validateBlock(block, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenDeliveryTimeIsZeroReturnNoError() {
        ItemBlock block = createDeliveryBlock(MasterData.NO_VALUE);
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
