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

public class QuantumOfSupplyBlockValidatorTest {
    private static final QuantumOfSupplyBlockValidator VALIDATOR = new QuantumOfSupplyBlockValidator();

    private static ItemBlock createQuantumOfSupplyBlock(int value) {
        return TestBlockCreationUtil.createNumericMdmParamValueBlock(
            KnownMdmParams.QUANTUM_OF_SUPPLY,
            BigDecimal.valueOf(value)
        );
    }

    private static ErrorInfo createError(int value) {
        return MbocErrors.get().excelValueMustBeInRange(
            SskuMasterDataFields.QUANTUM_OF_SUPPLY,
            String.valueOf(value),
            String.valueOf(QuantumOfSupplyBlockValidator.QUANTUM_MIN),
            String.valueOf(QuantumOfSupplyBlockValidator.QUANTUM_MAX)
        );
    }

    @Test
    public void whenQuantumOfSupplyIsInRangeShouldReturnNoErrors() {
        int value = (QuantumOfSupplyBlockValidator.QUANTUM_MIN + QuantumOfSupplyBlockValidator.QUANTUM_MAX) / 2;
        ItemBlock block = createQuantumOfSupplyBlock(value);
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of(block));

        ItemBlockValidationResult expectedResult = new ItemBlockValidationResult(block, List.of());

        Assertions.assertThat(VALIDATOR.validateValue(value)).isEmpty();
        Assertions.assertThat(VALIDATOR.validateBlock(block, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenQuantumOfSupplyTooLargeShouldReturnAppropriateError() {
        int value = QuantumOfSupplyBlockValidator.QUANTUM_MAX + 5;
        ItemBlock block = createQuantumOfSupplyBlock(value);
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of(block));

        ErrorInfo expectedError = createError(value);
        ItemBlockValidationResult expectedResult = new ItemBlockValidationResult(block, List.of(expectedError));

        Assertions.assertThat(VALIDATOR.validateValue(value)).contains(expectedError);
        Assertions.assertThat(VALIDATOR.validateBlock(block, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenQuantumOfSupplyTooSmallShouldReturnAppropriateError() {
        int value = QuantumOfSupplyBlockValidator.QUANTUM_MIN - 2;
        ItemBlock block = createQuantumOfSupplyBlock(value);
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of(block));

        ErrorInfo expectedError = createError(value);
        ItemBlockValidationResult expectedResult = new ItemBlockValidationResult(block, List.of(expectedError));

        Assertions.assertThat(VALIDATOR.validateValue(value)).contains(expectedError);
        Assertions.assertThat(VALIDATOR.validateBlock(block, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenQuantumOfSupplyIsZeroReturnNoError() {
        ItemBlock block = createQuantumOfSupplyBlock(MasterData.NO_VALUE);
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
