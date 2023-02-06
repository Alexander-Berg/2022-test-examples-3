package ru.yandex.direct.grid.processing.processor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.ExecutionResult;
import graphql.GraphQLError;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdSuspendKeywords;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdSuspendKeywordsPayload;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.request.RequestInfoGraphQlService.GET_REQ_ID_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.showcondition.KeywordsGraphQlService.SUSPEND_KEYWORDS_MUTATION;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GridGraphQLResolverInterceptorTest {
    private static final String CONTENT_LANGUAGE = "contentLanguage";
    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    campaigns(input: %s) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "        ... on GdTextCampaign {\n"
            + "          " + CONTENT_LANGUAGE + "\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    suspendedKeywordIds"
            + "  }\n"
            + "}";
    private static final GraphQlTestExecutor.TemplateMutation<GdSuspendKeywords, GdSuspendKeywordsPayload>
            NOT_ALLOWED_FOR_BLOCKED_USER_MUTATION = new GraphQlTestExecutor.TemplateMutation<>(
            SUSPEND_KEYWORDS_MUTATION, MUTATION_TEMPLATE, GdSuspendKeywords.class, GdSuspendKeywordsPayload.class);

    private static final String ALLOWED_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s\n"
            + "}";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    private GraphQlTestExecutor testExecutor;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private CampaignSteps campaignSteps;

    private GdCampaignsContainer campaignsContainer;
    private GridGraphQLContext context;
    private User operator;

    @Before
    public void initTestData() {
        UserInfo operatorInfo = userSteps.createUser(generateNewUser());
        operator = operatorInfo.getUser();

        CampaignInfo campaign = campaignSteps.createActiveTextCampaign(operatorInfo.getClientInfo());
        campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();
        context = ContextHelper.buildContext(operatorInfo.getUser(), operatorInfo.getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);

        TestAuthHelper.setDirectAuthentication(operatorInfo.getUser(), operatorInfo.getUser());

        campaignsContainer.getFilter().setCampaignIdIn(Set.of(campaign.getCampaignId()));
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }


    @Test
    public void resolverInterceptor_fieldDenied() {
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getMessage()).contains(CONTENT_LANGUAGE, "No rights for field");
    }

    @Test
    public void checkAllowedBlockedOperatorOrUser_ForNotAllowedMutation() {
        operator.setStatusBlocked(true);
        GdSuspendKeywords input = new GdSuspendKeywords()
                .withKeywordIds(List.of(RandomNumberUtils.nextPositiveLong()));
        List<GraphQLError> errors = testExecutor.doMutation(NOT_ALLOWED_FOR_BLOCKED_USER_MUTATION, input, operator)
                .getErrors();

        assertThat(errors)
                .hasSize(1)
                .extracting(GraphQLError::getMessage)
                .allMatch(errorMessage ->
                        errorMessage.contains("Client is blocked for resolver " + SUSPEND_KEYWORDS_MUTATION));
    }

    @Test
    public void checkAllowedBlockedOperatorOrUser_ForAllowedMutation() {
        operator.setStatusBlocked(true);
        String query = String.format(ALLOWED_MUTATION_TEMPLATE, GET_REQ_ID_RESOLVER_NAME);

        ExecutionResult result = processor.processQuery(null, query, null, context);

        assertThat(result.getErrors())
                .isEmpty();

        Map<String, String> data = result.getData();
        assertThat(data)
                .containsOnlyKeys(GET_REQ_ID_RESOLVER_NAME);
    }

}
