package ru.yandex.direct.internaltools.tools.modreasons;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.modreasons.model.DeleteModReasonsParameters;
import ru.yandex.direct.validation.defect.ids.NumberDefectIds;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.internaltools.tools.modreasons.model.DeleteModReasonsParameters.CID_FIELD_NAME;
import static ru.yandex.direct.internaltools.tools.modreasons.model.DeleteModReasonsParameters.CLIENTID_FIELD_NAME;
import static ru.yandex.direct.internaltools.tools.modreasons.model.DeleteModReasonsParameters.SHARD_FIELD_NAME;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;


@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DeleteModReasonsToolTest {
    private int shard;
    @Autowired
    private Steps steps;
    @Autowired
    private DeleteModReasonsTool deleteModReasonsTool;
    private ClientId ourClient;

    @Before
    public void setup() {
        ClientInfo defaultClientAndUser = steps.clientSteps().createDefaultClient();
        ourClient = defaultClientAndUser.getClientId();
        shard = defaultClientAndUser.getShard();
    }

    @Test
    public void validationPositiveWithShardTest() {
        DeleteModReasonsParameters parameters = new DeleteModReasonsParameters();
        parameters.setShard(shard);
        parameters.setClientId(ourClient.asLong());
        parameters.setCid(3);

        ValidationResult<DeleteModReasonsParameters, Defect> result = deleteModReasonsTool.validate(parameters);
        assertThat("Ошибок нет", result, hasNoErrors());
    }

    @Test
    public void validationPositiveExistingCidWithShardTest() {
        DeleteModReasonsParameters parameters = new DeleteModReasonsParameters();
        parameters.setShard(shard);
        parameters.setClientId(ourClient.asLong());
        parameters.setCid(2);

        ClientInfo otherClientAndUser = steps.clientSteps().createClient(new ClientInfo().withShard(shard));
        ClientId otherClient = otherClientAndUser.getClientId();

        ValidationResult<DeleteModReasonsParameters, Defect> result = deleteModReasonsTool.validate(parameters);
        assertThat("Ошибок нет", result, hasNoErrors());
    }

    @Test
    public void validationPositiveWithoutShardTest() {
        DeleteModReasonsParameters parameters = new DeleteModReasonsParameters();
        parameters.setClientId(ourClient.asLong());
        parameters.setCid(3);

        ValidationResult<DeleteModReasonsParameters, Defect> result = deleteModReasonsTool.validate(parameters);
        assertThat("Ошибок нет", result, hasNoErrors());
    }

    @Test
    public void validationNegativeParamsWithShardTest() {
        DeleteModReasonsParameters parameters = new DeleteModReasonsParameters();
        parameters.setShard(-shard);
        parameters.setClientId(-ourClient.asLong());
        parameters.setCid(-3);

        ValidationResult<DeleteModReasonsParameters, Defect> result = deleteModReasonsTool.validate(parameters);
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result).as("Есть ошибка на шард").is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field(SHARD_FIELD_NAME)), NumberDefectIds.MUST_BE_GREATER_THAN_MIN))));
        softly.assertThat(result).as("Есть ошибка на ClientID").is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field(CLIENTID_FIELD_NAME)), DefectIds.MUST_BE_VALID_ID))));
        softly.assertThat(result).as("Есть ошибка на cid").is(matchedBy(
                hasDefectDefinitionWith(validationError(path(field(CID_FIELD_NAME)), DefectIds.MUST_BE_VALID_ID))));

        Integer totalErrorsCount = result.flattenErrors().size();
        softly.assertThat(totalErrorsCount).as("Всего три ошибки").isEqualTo(3);
        softly.assertAll();
    }

    @Test
    public void validationNegativeParamsWithoutShardTest() {
        DeleteModReasonsParameters parameters = new DeleteModReasonsParameters();
        parameters.setClientId(-ourClient.asLong());
        parameters.setCid(-3);

        ValidationResult<DeleteModReasonsParameters, Defect> result = deleteModReasonsTool.validate(parameters);
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result).as("Есть ошибка на ClientID").is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field(CLIENTID_FIELD_NAME)), DefectIds.MUST_BE_VALID_ID))));
        softly.assertThat(result).as("Есть ошибка на cid").is(matchedBy(
                hasDefectDefinitionWith(validationError(path(field(CID_FIELD_NAME)), DefectIds.MUST_BE_VALID_ID))));
        Integer totalErrorsCount = result.flattenErrors().size();
        Integer errorsCount = result.getErrors().size();

        softly.assertThat(totalErrorsCount).as("Всего две ошибки").isEqualTo(2);
        softly.assertThat(errorsCount).as("На верхнем уровне нет ошибок").isEqualTo(0);
        softly.assertAll();
    }

    @Test
    public void validationNegativeNXClientIdWithoutShardTest() {
        DeleteModReasonsParameters parameters = new DeleteModReasonsParameters();
        parameters.setClientId(ourClient.asLong() + 10_000);
        parameters.setCid(1);

        ValidationResult<DeleteModReasonsParameters, Defect> result = deleteModReasonsTool.validate(parameters);
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result).as("Есть ошибка, что не найдено").is(matchedBy(hasDefectDefinitionWith(
                validationError(DefectIds.OBJECT_NOT_FOUND))));
        Integer totalErrorsCount = result.flattenErrors().size();

        softly.assertThat(totalErrorsCount).as("Всего одна ошибка").isEqualTo(1);
        softly.assertAll();
    }
}
