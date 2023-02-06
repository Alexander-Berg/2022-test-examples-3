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
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields;
import ru.yandex.market.mboc.common.utils.ErrorInfo;

public class BoxCountValidatorTest {

    private static final BoxCountValidator VALIDATOR = new BoxCountValidator();

    private static ItemBlock createBoxCountBlock(int value) {
        return TestBlockCreationUtil.createNumericMdmParamValueBlock(
            KnownMdmParams.BOX_COUNT,
            BigDecimal.valueOf(value)
        );
    }

    private static ErrorInfo createError(int value) {
        return MbocErrors.get().excelValueMustBeInRange(
            SskuMasterDataFields.BOX_COUNT,
            String.valueOf(value),
            String.valueOf(BoxCountValidator.BOX_COUNT_MIN),
            String.valueOf(BoxCountValidator.BOX_COUNT_MAX)
        );
    }

    @Test
    public void whenBoxCountIsInRangeShouldReturnNoErrors() {
        int value = (BoxCountValidator.BOX_COUNT_MIN + BoxCountValidator.BOX_COUNT_MAX) / 2;
        ItemBlock block = createBoxCountBlock(value);
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of(block));

        ItemBlockValidationResult expectedResult = new ItemBlockValidationResult(block, List.of());

        Assertions.assertThat(BoxCountValidator.validateBoxCount(value)).isEmpty();
        Assertions.assertThat(VALIDATOR.validateBlock(block, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenBoxCountTooLargeShouldReturnAppropriateError() {
        int value = BoxCountValidator.BOX_COUNT_MAX + 5;
        ItemBlock block = createBoxCountBlock(value);
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of(block));

        ErrorInfo expectedError = createError(value);
        ItemBlockValidationResult expectedResult = new ItemBlockValidationResult(block, List.of(expectedError));

        Assertions.assertThat(BoxCountValidator.validateBoxCount(value)).contains(expectedError);
        Assertions.assertThat(VALIDATOR.validateBlock(block, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenBoxCountTooSmallShouldReturnAppropriateError() {
        int value = BoxCountValidator.BOX_COUNT_MIN - 1;
        ItemBlock block = createBoxCountBlock(value);
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of(block));

        ErrorInfo expectedError = createError(value);
        ItemBlockValidationResult expectedResult = new ItemBlockValidationResult(block, List.of(expectedError));

        Assertions.assertThat(BoxCountValidator.validateBoxCount(value)).contains(expectedError);
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
