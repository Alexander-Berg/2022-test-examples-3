package ru.yandex.direct.internaltools.tools.asap;

import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.asap.model.SetAsSoonAsPossibleToolParams;
import ru.yandex.direct.internaltools.tools.asap.model.SetAsSoonAsPossibleToolResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.direct.core.entity.user.service.validation.UserDefects.userNotFound;
import static ru.yandex.direct.internaltools.tools.asap.model.SetAsSoonAsPossibleToolParams.ASAP_FIELD_NAME;
import static ru.yandex.direct.internaltools.tools.asap.model.SetAsSoonAsPossibleToolParams.LOGINS_FIELD_NAME;
import static ru.yandex.direct.internaltools.tools.asap.model.SetAsSoonAsPossibleToolParams.LOGINS_TEXT_FIELD_NAME;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SetAsSoonAsPossibleToolTest {

    private static final String NONEXISTENT_LOGIN = "user_not_exists";

    @Autowired
    private Steps steps;

    @Autowired
    private SetAsSoonAsPossibleTool setAsSoonAsPossibleTool;

    private ClientInfo clientInfo;
    private ClientInfo anotherClientInfo;
    private ClientInfo anotherShardClientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        steps.clientOptionsSteps().addEmptyClientOptions(clientInfo);
        anotherClientInfo = steps.clientSteps().createDefaultClient();
        steps.clientOptionsSteps().addEmptyClientOptions(anotherClientInfo);
        anotherShardClientInfo = steps.clientSteps().createDefaultClientAnotherShard();
        steps.clientOptionsSteps().addEmptyClientOptions(anotherShardClientInfo);
    }

    @Test
    public void validate_LoginsTextIsNull_ValidationError() {
        ValidationResult<SetAsSoonAsPossibleToolParams, Defect> vr = setAsSoonAsPossibleTool.validate(
                new SetAsSoonAsPossibleToolParams().withSetAsap(true));

        Matcher<DefectInfo<Defect>> expected = validationError(path(field(LOGINS_TEXT_FIELD_NAME)), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_AsapIsNull_ValidationError() {
        ValidationResult<SetAsSoonAsPossibleToolParams, Defect> vr = setAsSoonAsPossibleTool.validate(
                new SetAsSoonAsPossibleToolParams().withLoginsToSetAsap(clientInfo.getLogin()));

        Matcher<DefectInfo<Defect>> expected = validationError(path(field(ASAP_FIELD_NAME)), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_NonExistentLogin_ValidationError() {
        ValidationResult<SetAsSoonAsPossibleToolParams, Defect> vr = setAsSoonAsPossibleTool.validate(
                new SetAsSoonAsPossibleToolParams().withLoginsToSetAsap(NONEXISTENT_LOGIN).withSetAsap(true));

        Matcher<DefectInfo<Defect>> expected = validationError(path(field(LOGINS_FIELD_NAME), index(0)), userNotFound());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_EverythingIsOk_NoValidationError() {
        ValidationResult<SetAsSoonAsPossibleToolParams, Defect> vr = setAsSoonAsPossibleTool.validate(
                new SetAsSoonAsPossibleToolParams().withLoginsToSetAsap(clientInfo.getLogin()).withSetAsap(true));

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void getMassData_SetAsapToOneUser_ResultIsOk() {
        List<SetAsSoonAsPossibleToolResult> results = setAsSoonAsPossibleTool.getMassData(
                new SetAsSoonAsPossibleToolParams()
                        .withSetAsap(true)
                        .withLoginsToSetAsap(clientInfo.getLogin()));

        Assert.assertThat(results, contains(new SetAsSoonAsPossibleToolResult(clientInfo.getLogin(), true)));
    }

    @Test
    public void getMassData_SetAsapToTwoUsersInOneShard_ResultIsOk() {
        List<SetAsSoonAsPossibleToolResult> results = setAsSoonAsPossibleTool.getMassData(
                new SetAsSoonAsPossibleToolParams()
                        .withSetAsap(true)
                        .withLoginsToSetAsap(clientInfo.getLogin() + "," + anotherClientInfo.getLogin()));

        Assert.assertThat(results, containsInAnyOrder(
                new SetAsSoonAsPossibleToolResult(clientInfo.getLogin(), true),
                new SetAsSoonAsPossibleToolResult(anotherClientInfo.getLogin(), true)));
    }

    @Test
    public void getMassData_SetAsapToTwoUsersInDifferentShards_ResultIsOk() {
        List<SetAsSoonAsPossibleToolResult> results = setAsSoonAsPossibleTool.getMassData(
                new SetAsSoonAsPossibleToolParams()
                        .withSetAsap(true)
                        .withLoginsToSetAsap(clientInfo.getLogin() + "," + anotherShardClientInfo.getLogin()));

        Assert.assertThat(results, containsInAnyOrder(
                new SetAsSoonAsPossibleToolResult(clientInfo.getLogin(), true),
                new SetAsSoonAsPossibleToolResult(anotherShardClientInfo.getLogin(), true)));
    }

    @Test
    public void getMassData_SetAndUnsetAsapToOneUser_ResultIsOk() {
        setAsSoonAsPossibleTool.getMassData(
                new SetAsSoonAsPossibleToolParams()
                        .withSetAsap(true)
                        .withLoginsToSetAsap(clientInfo.getLogin()));

        List<SetAsSoonAsPossibleToolResult> results = setAsSoonAsPossibleTool.getMassData(
                new SetAsSoonAsPossibleToolParams()
                        .withSetAsap(false)
                        .withLoginsToSetAsap(clientInfo.getLogin()));

        Assert.assertThat(results, contains(
                new SetAsSoonAsPossibleToolResult(clientInfo.getLogin(), false)));
    }

    @Test
    public void getMassData_SetAsapToOneUser_ForClientWithEmptyFlags_ResultIsOk() {
        steps.clientOptionsSteps().setClientFlags(clientInfo, "");

        List<SetAsSoonAsPossibleToolResult> results = setAsSoonAsPossibleTool.getMassData(
                new SetAsSoonAsPossibleToolParams()
                        .withSetAsap(true)
                        .withLoginsToSetAsap(clientInfo.getLogin()));

        Assert.assertThat(results, contains(
                new SetAsSoonAsPossibleToolResult(clientInfo.getLogin(), true)));
    }
}
