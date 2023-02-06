package ru.yandex.direct.grid.processing.service.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.shyiko.mysql.binlog.GtidSet;
import graphql.ExecutionResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.core.entity.sync.service.MysqlStateService;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.client.GdGetClientMutationId;
import ru.yandex.direct.grid.processing.model.client.GdGetClientMutationIdPayload;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdAddKeywords;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdAddKeywordsItem;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static com.google.common.collect.Iterables.getFirst;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CheckGetClientMutationIdTest {

    private static final String GET_CLIENT_MUTATION_ID = "getClientMutationId";
    private static final String GET_CLIENT_MUTATION_ID_TEMPLATE = ""
            + "mutation {\n"
            + "  %s(input: %s) {\n"
            + "    mutationId\n"
            + "  }\n"
            + "}";

    private static final String ADD_KEYWORDS_MUTATION = "addKeywords";
    private static final String GET_CLIENT_MUTATION_ID_TEMPLATE_WITH_ANOTHER_MUTATION = ""
            + "mutation {\n"
            + "  %s(input: %s) {\n"
            + "    addedItems {\n"
            + "      adGroupId,\n"
            + "      keywordId\n"
            + "    }\n"
            + "  }\n"
            + "  %s(input: %s) {\n"
            + "    mutationId\n"
            + "  }\n"
            + "}";

    private User operator;
    private GdGetClientMutationId request;
    private GdAddKeywords addKeywordsRequest;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private MysqlStateService mysqlStateService;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private UserSteps userSteps;
    private long adGroupId;

    @Before
    public void initTestData() {
        UserInfo userInfo = userSteps.createUser(generateNewUser());

        AdGroupInfo adGroupInfo = adGroupSteps.createActiveTextAdGroup(userInfo.getClientInfo());
        adGroupId = adGroupInfo.getAdGroupId();
        operator = UserHelper.getUser(adGroupInfo);
        TestAuthHelper.setDirectAuthentication(operator);

        addKeywordsRequest = new GdAddKeywords()
                .withAddItems(Collections.singletonList(new GdAddKeywordsItem()
                        .withAdGroupId(adGroupId)
                        .withKeyword("some keyword"))
                );

        request = new GdGetClientMutationId()
                .withLogin(userInfo.getUser().getLogin());
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }


    @Test
    public void checkGetClientMutationId() {
        String query = String.format(GET_CLIENT_MUTATION_ID_TEMPLATE, GET_CLIENT_MUTATION_ID,
                graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        Map<String, Object> data = result.getData();
        assertThat(data)
                .containsOnlyKeys(GET_CLIENT_MUTATION_ID);

        GdGetClientMutationIdPayload payload = GraphQlJsonUtils
                .convertValue(data.get(GET_CLIENT_MUTATION_ID), GdGetClientMutationIdPayload.class);

        checkGetClientMutationIdPayload(payload);
    }

    @Test
    public void checkGetClientMutationId_WithAnotherMutation() {
        String query = String.format(GET_CLIENT_MUTATION_ID_TEMPLATE_WITH_ANOTHER_MUTATION, ADD_KEYWORDS_MUTATION,
                graphQlSerialize(addKeywordsRequest),
                GET_CLIENT_MUTATION_ID, graphQlSerialize(request)
        );
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        Map<String, Object> data = result.getData();
        assertThat(data)
                .containsOnlyKeys(ADD_KEYWORDS_MUTATION, GET_CLIENT_MUTATION_ID);

        GdGetClientMutationIdPayload payload = GraphQlJsonUtils
                .convertValue(data.get(GET_CLIENT_MUTATION_ID), GdGetClientMutationIdPayload.class);

        checkGetClientMutationIdPayload(payload);
    }

    @Test
    public void checkAddKeywordsMutation_WithTooLongKeyword_Failure() {
        String tooLongKeyword = "one two three four five six seven eight nine ten";
        GdAddKeywords gdAddKeywords = new GdAddKeywords()
                .withAddItems(Collections.singletonList(new GdAddKeywordsItem()
                        .withAdGroupId(adGroupId)
                        .withKeyword(tooLongKeyword)));
        String query = String.format(GET_CLIENT_MUTATION_ID_TEMPLATE_WITH_ANOTHER_MUTATION, ADD_KEYWORDS_MUTATION,
                graphQlSerialize(gdAddKeywords),
                GET_CLIENT_MUTATION_ID, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        GraphQLUtils.checkErrors(result.getErrors());
        Map<String, Object> data = result.getData();
        List addedItems = GraphQLUtils.getDataValue(data, "addKeywords/addedItems");
        assertThat(addedItems).isEmpty();
    }

    private void checkGetClientMutationIdPayload(GdGetClientMutationIdPayload payload) {
        GtidSet gtidSet = new GtidSet(payload.getMutationId());
        assertThat(gtidSet.getUUIDSets())
                .hasSize(1);
        GtidSet.UUIDSet uuidSet = getFirst(gtidSet.getUUIDSets(), null);

        GtidSet.UUIDSet currentServerGtidSet = mysqlStateService.getCurrentServerGtidSet(request.getLogin());

        assertThat(uuidSet.isContainedWithin(currentServerGtidSet))
                .as("Полученный в мутации uuidSet содержится в currentServerGtidSet ")
                .isTrue();
    }

}
