package ru.yandex.market.mbo.mdm.common.masterdata.services.cccode;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.mboc.common.masterdata.model.cccode.Cis;
import ru.yandex.market.mboc.common.masterdata.model.cccode.CustomsCommCode;
import ru.yandex.market.mboc.common.masterdata.model.cccode.MdmParamMarkupState;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.CCCodeValidationError.Type.GROUP_HAS_MARKUPS;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.CCCodeValidator.Operation.CREATE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.CCCodeValidator.Operation.DELETE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.CCCodeValidator.Operation.UPDATE;

public class GroupFormatCCCodeValidatorTest {

    private final GroupFormatCCCodeValidator validator = new GroupFormatCCCodeValidator();

    @Test
    public void shouldValidateOnCreateAndUpdate() {
        assertThat(validator.isOperationSupported(CREATE)).isTrue();
        assertThat(validator.isOperationSupported(UPDATE)).isTrue();
        assertThat(validator.isOperationSupported(DELETE)).isFalse();
    }

    @Test
    public void whenCodeNotEmptyShouldFail() {
        List<CCCodeValidationError> errors = validator.validate(null, codeOf("1234"));
        assertThat(errors).containsExactlyInAnyOrder(errorOf(CCCodeValidationError.Type.GROUP_CODE_IS_NOT_EMPTY));
    }

    @Test
    public void whenHasMarkupShouldPass() {
        assertThat(validator.validate(null,
            codeOf("").setHonestSign(new MdmParamMarkupState().setCis(Cis.REQUIRED))))
            .containsExactlyInAnyOrder(errorOf(GROUP_HAS_MARKUPS));
        assertThat(validator.validate(null,
            codeOf("").setMercury(new MdmParamMarkupState().setCis(Cis.REQUIRED))))
            .containsExactlyInAnyOrder(errorOf(GROUP_HAS_MARKUPS));
        assertThat(validator.validate(null, codeOf("").setTraceable(true)))
            .containsExactlyInAnyOrder(errorOf(GROUP_HAS_MARKUPS));

    }

    @Test
    public void whenCorrectShouldPass() {
        assertThat(validator.validate(null, codeOf(""))).isEmpty();
        assertThat(validator.validate(null, codeOf(null))).isEmpty();
    }

    private CustomsCommCode codeOf(String code) {
        return new CustomsCommCode().setCode(code);
    }

    private CCCodeValidationError errorOf(CCCodeValidationError.Type errorType) {
        return new CCCodeValidationError(errorType, "");
    }

}
