package ru.yandex.market.mbo.mdm.common.masterdata.services.cccode;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.CCCodeValidationError.Type;
import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.CCCodeValidator.Operation;
import ru.yandex.market.mboc.common.masterdata.model.cccode.CustomsCommCode;


import static org.assertj.core.api.Assertions.assertThat;

public class CodeFormatCCCodeValidatorTest {

    private CodeFormatCCCodeValidator validator = new CodeFormatCCCodeValidator();

    @Test
    public void shouldValidateOnCreateAndUpdate() {
        assertThat(validator.isOperationSupported(Operation.CREATE)).isTrue();
        assertThat(validator.isOperationSupported(Operation.UPDATE)).isTrue();
        assertThat(validator.isOperationSupported(Operation.DELETE)).isFalse();
    }

    @Test
    public void whenNotDigitShouldFail() {
        List<CCCodeValidationError> errors = validator.validate(null, codeOf("12 043091 11"));
        assertThat(errors).containsExactlyInAnyOrder(errorOf(Type.CODE_NOT_NUMERIC));
    }

    @Test
    public void whenEmptyShouldFail() {
        List<CCCodeValidationError> errors = validator.validate(null, codeOf(""));
        assertThat(errors).containsExactlyInAnyOrder(errorOf(Type.CODE_TOO_SHORT));
        errors = validator.validate(null, codeOf(null));
        assertThat(errors).containsExactlyInAnyOrder(errorOf(Type.CODE_TOO_SHORT));
    }

    @Test
    public void whenTooLongShouldFail() {
        List<CCCodeValidationError> errors = validator.validate(null, codeOf("1204309111323237"));
        assertThat(errors).containsExactlyInAnyOrder(errorOf(Type.CODE_TOO_LONG));
    }

    @Test
    public void whenTooLongAndNotDigitsShouldFailWithBoth() {
        List<CCCodeValidationError> errors = validator.validate(null, codeOf("12043091 11323237"));
        assertThat(errors).containsExactlyInAnyOrder(errorOf(Type.CODE_TOO_LONG), errorOf(Type.CODE_NOT_NUMERIC));
    }

    @Test
    public void whenCorrectShouldPass() {
        assertThat(validator.validate(null, codeOf("12043091323237"))).isEmpty();
        assertThat(validator.validate(null, codeOf("0"))).isEmpty();
        assertThat(validator.validate(null, codeOf("12345678"))).isEmpty();
    }

    private CustomsCommCode codeOf(String code) {
        return new CustomsCommCode().setCode(code);
    }

    private CCCodeValidationError errorOf(CCCodeValidationError.Type errorType) {
        return new CCCodeValidationError(errorType, "");
    }
}
