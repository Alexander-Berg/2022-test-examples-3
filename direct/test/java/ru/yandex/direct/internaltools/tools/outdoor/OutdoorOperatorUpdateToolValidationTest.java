package ru.yandex.direct.internaltools.tools.outdoor;

import org.junit.Test;

import ru.yandex.direct.internaltools.tools.outdoor.model.OutdoorOperatorUpdateParameter;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.internaltools.tools.outdoor.OutdoorOperatorUpdateTool.LOGIN_FIELD;
import static ru.yandex.direct.internaltools.tools.outdoor.OutdoorOperatorUpdateTool.OPERATOR_NAME_FIELD;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.validLogin;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class OutdoorOperatorUpdateToolValidationTest {

    private OutdoorOperatorUpdateTool outdoorOperatorUpdateTool = new OutdoorOperatorUpdateTool(null);

    @Test
    public void validate_GoodLoginAndName_Pass() {
        OutdoorOperatorUpdateParameter parameter = new OutdoorOperatorUpdateParameter();
        parameter.setLogin("yndx.zhur23.super");
        parameter.setOperatorName("operator_name");
        ValidationResult<OutdoorOperatorUpdateParameter, Defect> result = outdoorOperatorUpdateTool.validate(parameter);
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_Login_NotAcceptNonLatinChars() {
        OutdoorOperatorUpdateParameter parameter = new OutdoorOperatorUpdateParameter();
        parameter.setLogin("yndx.жур.super");
        parameter.setOperatorName("operator_name");

        ValidationResult<OutdoorOperatorUpdateParameter, Defect> result = outdoorOperatorUpdateTool.validate(parameter);
        assertThat(result, hasDefectDefinitionWith(validationError(path(field(LOGIN_FIELD)), validLogin())));
    }

    @Test
    public void validate_OperatorName_NotBlank() {
        OutdoorOperatorUpdateParameter parameter = new OutdoorOperatorUpdateParameter();
        parameter.setLogin("yndx.zhur23.super");
        parameter.setOperatorName("");

        ValidationResult<OutdoorOperatorUpdateParameter, Defect> result = outdoorOperatorUpdateTool.validate(parameter);
        assertThat(result,
                hasDefectDefinitionWith(validationError(path(field(OPERATOR_NAME_FIELD)), notEmptyString())));
    }

    @Test
    public void validate_OperatorName_NotAcceptDigits() {
        OutdoorOperatorUpdateParameter parameter = new OutdoorOperatorUpdateParameter();
        parameter.setLogin("yndx.zhur23.super");
        parameter.setOperatorName("operator_name23");

        ValidationResult<OutdoorOperatorUpdateParameter, Defect> result = outdoorOperatorUpdateTool.validate(parameter);
        assertThat(result, hasDefectDefinitionWith(validationError(path(field(OPERATOR_NAME_FIELD)), invalidValue())));
    }

    @Test
    public void validate_OperatorName_AcceptLatinLetters() {
        OutdoorOperatorUpdateParameter parameter = new OutdoorOperatorUpdateParameter();
        parameter.setLogin("yndx.zhur23.super");
        parameter.setOperatorName("OperatorАутдор Первый ABC_один");

        ValidationResult<OutdoorOperatorUpdateParameter, Defect> result = outdoorOperatorUpdateTool.validate(parameter);
        assertThat(result, hasNoDefectsDefinitions());
    }
}
