package ru.yandex.market.mbo.mdm.common.masterdata.services.cccode;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CustomsCommCodeRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.CCCodeValidationError.Type;
import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.CCCodeValidator.Operation;
import ru.yandex.market.mboc.common.masterdata.model.cccode.CustomsCommCode;


import static org.assertj.core.api.Assertions.assertThat;

public class CodePrefixCCCodeValidatorTest {
    private CustomsCommCodeRepositoryMock codeRepository;
    private CodePrefixCCCodeValidator validator;

    @Before
    public void setup() {
        codeRepository = new CustomsCommCodeRepositoryMock();
        validator = new CodePrefixCCCodeValidator(codeRepository);
    }

    @Test
    public void shouldValidateOnCreateAndUpdate() {
        assertThat(validator.isOperationSupported(Operation.CREATE)).isTrue();
        assertThat(validator.isOperationSupported(Operation.UPDATE)).isTrue();
        assertThat(validator.isOperationSupported(Operation.DELETE)).isFalse();
    }

    @Test
    public void whenZeroParentShouldPass() {
        List<CCCodeValidationError> errors = validator.validate(null, codeOf(1, 0, ""));
        assertThat(errors).isEmpty();
    }

    @Test
    public void whenParentNotFoundShouldFail() {
        List<CCCodeValidationError> errors = validator.validate(null, codeOf(0, 1, ""));
        assertThat(errors).containsExactlyInAnyOrder(errorOf(Type.PARENT_NOT_FOUND));
    }

    @Test
    public void whenParentIsNotPrefixShouldFail() {
        codeRepository.insert(codeOf(1, 0, "1234"));
        List<CCCodeValidationError> errors = validator.validate(null, codeOf(0, 1, "23456789"));
        assertThat(errors).containsExactlyInAnyOrder(errorOf(Type.PARENT_IS_NOT_PREFIX));
    }

    @Test
    public void whenDuplicateCodeExistsShouldFail() {
        codeRepository.insert(codeOf(1, 0, "1234"));
        List<CCCodeValidationError> errors = validator.validate(null, codeOf(0, 1, "1234"));
        assertThat(errors).containsExactlyInAnyOrder(errorOf(Type.DUPLICATE_CODE));
    }

    @Test
    public void whenCorrectShouldPass() {
        codeRepository.insert(codeOf(1, 0, "1234"));
        List<CCCodeValidationError> errors = validator.validate(null, codeOf(0, 1, "12345678"));
        assertThat(errors).isEmpty();
    }

    private CustomsCommCode codeOf(long id, long parentId, String code) {
        return new CustomsCommCode().setId(id).setParentId(parentId).setCode(code);
    }

    private CCCodeValidationError errorOf(Type errorType) {
        return new CCCodeValidationError(errorType, "");
    }
}
