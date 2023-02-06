package ru.yandex.direct.grid.processing.service.dialogs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DialogInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dialogs.client.DialogsClient;
import ru.yandex.direct.dialogs.client.model.Skill;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.dialogs.GdAddDialogPayload;
import ru.yandex.direct.grid.processing.model.dialogs.GdAddDialogRequest;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getGdValidationResults;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DialogGraphQLServiceTest {

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private DialogsClient dialogsClient;

    private static final String ADD_DIALOG_MUTATION = "addDialog";

    private static final String QUERY_TEMPLATE = "{\n"
            + "  client(searchBy:{id: %s}) {    \n"
            + "    dialogs{\n"
            + "      id\n"
            + "      skillId\n"
            + "      clientId\n"
            + "      isActive\n"
            + "      name\n"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final String MUTATION_TEMPLATE = "mutation {\n"
            + "  %s(input: %s) {    \n"
            + "    dialog{\n"
            + "      id\n"
            + "      skillId\n"
            + "      clientId\n"
            + "      isActive\n"
            + "      name\n"
            + "    }\n"
            + "    validationResult{\n"
            + "      errors{\n"
            + "        code,\n"
            + "        params,\n"
            + "        path\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

    private ClientInfo clientInfo;
    private GridGraphQLContext context;

    @Before
    public void setUp() {
        initMocks(this);
        clientInfo = steps.clientSteps().createDefaultClient();
        context = ContextHelper.buildContext(clientInfo.getChiefUserInfo().getUser());
        TestAuthHelper.setDirectAuthentication(clientInfo.getChiefUserInfo().getUser());
    }

    @Test
    public void getDialogs_success() {
        DialogInfo defaultDialog1 = steps.dialogSteps().createDefaultDialog(clientInfo, "skillId1");
        DialogInfo defaultDialog2 = steps.dialogSteps().createDefaultDialog(clientInfo, "skillId2");

        String query = String.format(QUERY_TEMPLATE, clientInfo.getClientId());
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());
        Map<String, Object> data = result.getData();
        assumeThat(data, notNullValue());

        Map<String, Object> dialog1 = new HashMap<>();
        dialog1.put("id", defaultDialog1.getDialog().getId());
        dialog1.put("skillId", defaultDialog1.getDialog().getSkillId());
        dialog1.put("clientId", defaultDialog1.getDialog().getClientId());
        dialog1.put("isActive", defaultDialog1.getDialog().getIsActive());
        dialog1.put("name", defaultDialog1.getDialog().getName());

        Map<String, Object> dialog2 = new HashMap<>(dialog1);
        dialog2.put("id", defaultDialog2.getDialog().getId());
        dialog2.put("skillId", defaultDialog2.getDialog().getSkillId());

        Map<String, Object> expected = singletonMap(
                "client", singletonMap(
                    "dialogs",
                    ImmutableList.of(
                            dialog1,
                            dialog2
                    )
                )
        );
        steps.dialogSteps().deleteDialog(clientInfo, defaultDialog1.getCampaignId(), defaultDialog1.getDialog());
        steps.dialogSteps().deleteDialog(clientInfo, defaultDialog2.getCampaignId(), defaultDialog2.getDialog());
        assertThat(data, beanDiffer(expected));
    }

    @Test
    public void getDialogs_userDialogs_success() {
        String skillId = "skillId1";
        Skill skill = createSkill(skillId);
        doReturn(singletonMap(clientInfo.getUid(), singletonList(skill)))
                .when(dialogsClient).getSkillsByUserId(anyList());

        String query = String.format(QUERY_TEMPLATE, clientInfo.getClientId());
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());
        Map<String, Object> data = result.getData();
        assumeThat(data, notNullValue());

        Map<String, Object> expectedDialog = new HashMap<>();
        expectedDialog.put("id", null);
        expectedDialog.put("skillId", skill.getSkillId());
        expectedDialog.put("name", skill.getName());
        expectedDialog.put("isActive", true);
        expectedDialog.put("clientId", clientInfo.getClientId().asLong());

        Map<String, Object> expected = singletonMap(
                "client", singletonMap(
                        "dialogs",
                        ImmutableList.of(
                                expectedDialog
                        )
                )
        );
        assertThat(data, beanDiffer(expected));
    }

    @Test
    public void addDialogs_newDialog_success() {
        String skillId = "skillId1";
        Skill skill = createSkill(skillId);
        GdAddDialogRequest gdAddDialogRequest = new GdAddDialogRequest().withSkillId(skillId);

        doReturn(singletonList(skill)).when(dialogsClient).getSkills(anyList());

        String query = String.format(MUTATION_TEMPLATE, ADD_DIALOG_MUTATION, graphQlSerialize(gdAddDialogRequest));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());

        Object dataValue = getDataValue(result.getData(), "addDialog/dialog/id");
        assumeThat(dataValue, notNullValue());
    }

    @Test
    public void addDialogs_existedDialog_success() {
        String skillId = "skillId2";
        Skill skill = createSkill(skillId);
        DialogInfo defaultDialog = steps.dialogSteps().createDefaultDialog(clientInfo, skillId);
        GdAddDialogRequest gdAddDialogRequest = new GdAddDialogRequest().withSkillId(skillId);

        doReturn(singletonList(skill)).when(dialogsClient).getSkills(anyList());

        String query = String.format(MUTATION_TEMPLATE, ADD_DIALOG_MUTATION, graphQlSerialize(gdAddDialogRequest));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());

        verify(dialogsClient, never()).getSkills(singletonList(skillId));

        Object dataValue = getDataValue(result.getData(), "addDialog/dialog/id");
        assumeThat(dataValue, equalTo(defaultDialog.getDialog().getId()));
    }

    @Test
    public void addDialogs_objectNotFound() {
        String skillId = "skillId3";
        GdAddDialogRequest gdAddDialogRequest = new GdAddDialogRequest().withSkillId(skillId);

        doReturn(emptyList()).when(dialogsClient).getSkills(anyList());

        String query = String.format(MUTATION_TEMPLATE, ADD_DIALOG_MUTATION, graphQlSerialize(gdAddDialogRequest));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        List<GdValidationResult> gdValidationResults = getGdValidationResults(result.getErrors());
        assumeThat(gdValidationResults, hasSize(0));

        Map<String, Object> data = result.getData();
        GdAddDialogPayload payload = convertValue(data.get(ADD_DIALOG_MUTATION), GdAddDialogPayload.class);
        GdAddDialogPayload expectedPayload = new GdAddDialogPayload().withValidationResult(new GdValidationResult()
                .withErrors(singletonList(new GdDefect()
                        .withCode("DefectIds." + objectNotFound().defectId().toString()))));
        assertThat(payload, beanDiffer(expectedPayload).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void addDialogs_invalidValue() {
        String skillId = "skillId4";
        GdAddDialogRequest gdAddDialogRequest = new GdAddDialogRequest().withSkillId(skillId);
        Skill skill = createSkill(skillId);
        skill.setBotGuid(null);

        doReturn(singletonList(skill)).when(dialogsClient).getSkills(anyList());

        String query = String.format(MUTATION_TEMPLATE, ADD_DIALOG_MUTATION, graphQlSerialize(gdAddDialogRequest));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        assumeThat(result.getErrors(), hasSize(1));
        String errorMessage = "Null botGuid for skillId " + skillId;
        assertThat(result.getErrors().get(0).getMessage(), containsString(errorMessage));
    }

    private Skill createSkill(String skillId) {
        Skill skill = new Skill();
        skill.setSkillId(skillId);
        skill.setBotGuid("botGuid");
        skill.setName("name");
        skill.setOnAir(true);
        return skill;
    }
}
