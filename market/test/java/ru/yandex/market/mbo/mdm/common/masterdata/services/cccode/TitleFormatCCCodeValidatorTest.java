package ru.yandex.market.mbo.mdm.common.masterdata.services.cccode;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.CCCodeValidationError.Type;
import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.CCCodeValidator.Operation;
import ru.yandex.market.mboc.common.masterdata.model.cccode.CustomsCommCode;

import static org.assertj.core.api.Assertions.assertThat;

public class TitleFormatCCCodeValidatorTest {

    private TitleFormatCCCodeValidator validator = new TitleFormatCCCodeValidator();

    @Test
    public void shouldValidateOnCreateAndUpdate() {
        assertThat(validator.isOperationSupported(Operation.CREATE)).isTrue();
        assertThat(validator.isOperationSupported(Operation.UPDATE)).isTrue();
        assertThat(validator.isOperationSupported(Operation.DELETE)).isFalse();
    }

    @Test
    public void whenEmptyShouldFail() {
        List<CCCodeValidationError> errors = validator.validate(null, codeOf(""));
        assertThat(errors).containsExactlyInAnyOrder(errorOf(Type.EMPTY_TITLE));
        errors = validator.validate(null, codeOf(null));
        assertThat(errors).containsExactlyInAnyOrder(errorOf(Type.EMPTY_TITLE));
        errors = validator.validate(null, codeOf("    \t  \n  "));
        assertThat(errors).containsExactlyInAnyOrder(errorOf(Type.EMPTY_TITLE));
    }

    @Test
    public void whenCorrectShouldPass() {
        assertThat(validator.validate(null, codeOf("X"))).isEmpty();
        assertThat(validator.validate(null, codeOf("Lorem ipsum dolor sit amet"))).isEmpty();
        assertThat(validator.validate(null, codeOf("."))).isEmpty();
    }

    private CustomsCommCode codeOf(String title) {
        return new CustomsCommCode().setTitle(title);
    }

    private CCCodeValidationError errorOf(Type errorType) {
        return new CCCodeValidationError(errorType, "");
    }
}
