package ru.yandex.direct.grid.processing.service.client;

import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import graphql.ExecutionResult;
import org.jooq.Select;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.client.GdCheckClientMutationState;
import ru.yandex.direct.grid.processing.model.client.GdCheckClientMutationStatePayload;
import ru.yandex.direct.grid.processing.model.client.GdClientMutationState;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.ytwrapper.client.YtExecutionException;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasValidationResult;
import static ru.yandex.direct.grid.schema.yt.Tables.DIRECT_GRID_MYSQL_SYNC_STATES;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class CheckClientMutationStateTest {
    private static final String UNNORMALIZED_LOGIN = "vErY.cOmPlEx-LoGiN";
    private static final String NORMALIZED_LOGIN = "very-complex-login";
    private static final String SYNCED_GTID_SET = "77e3d5a6-0778-11e8-9370-ef0030ef21d4:1-1115140";
    private static final String GTID_SET_FROM_YT =
            "4b1a6ae8-786b-2866-e0fd-eea6ecc170ff:1-5525299736," + SYNCED_GTID_SET;
    private static final String NOT_SYNCED_GTID_SET = "77e3d5a6-0778-11e8-9370-ef0030ef21d4:1-2000000";
    private static final String UNKNOWN_GTID_SET = "5c78fa97-b2da-63c7-d74e-c7b0638e22ca:1-61228948";
    private static final String QUERY_NAME = "checkClientMutationState";
    private static final String QUERY_TEMPLATE = "query {\n"
            + "  %s(input: %s) {\n"
            + "    state"
            + "  }\n"
            + "}\n";

    private GridGraphQLContext context;
    private GdCheckClientMutationState request;
    private int clientShard;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private ClientMutationIdGraphQlService clientMutationIdGraphQlService;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private YtDynamicSupport gridYtSupport;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static UserInfo userInfo;

    @Before
    public void initTestData() {
        if (userInfo == null) {
            long uid = userSteps.generateNewUserUidSafely(NORMALIZED_LOGIN);
            userInfo = userSteps.createUser(generateNewUser()
                    .withUid(uid)
                    .withLogin(NORMALIZED_LOGIN));
        }
        clientShard = userInfo.getShard();

        UnversionedRowset syncStateRow = rowsetBuilder()
                .add(rowBuilder()
                        .withColValue(DIRECT_GRID_MYSQL_SYNC_STATES.GTID_SET.getName(), GTID_SET_FROM_YT))
                .build();
        doReturn(syncStateRow)
                .when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class));

        context = ContextHelper.buildContext(userInfo.getUser());

        request = new GdCheckClientMutationState()
                .withLogin(UNNORMALIZED_LOGIN)
                .withMutationId(SYNCED_GTID_SET);
    }


    @Test
    public void checkClientMutationState() {
        String query = String.format(QUERY_TEMPLATE, QUERY_NAME, graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null, context);

        checkPayload(result, GdClientMutationState.SYNCED);
    }

    @Test
    public void checkClientMutationState_WhenNotSyncedGtidSet() {
        request.setMutationId(NOT_SYNCED_GTID_SET);
        String query = String.format(QUERY_TEMPLATE, QUERY_NAME, graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null, context);

        checkPayload(result, GdClientMutationState.NOT_SYNCED);
    }

    @Test
    public void checkClientMutationState_WhenUnknownGtidSet() {
        request.setMutationId(UNKNOWN_GTID_SET);
        String query = String.format(QUERY_TEMPLATE, QUERY_NAME, graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null, context);

        checkPayload(result, GdClientMutationState.UNKNOWN);
    }

    @Test
    public void checkClientMutationState_WhenYtNotAvailable() {
        doThrow(new YtExecutionException("Can't get result of YT ..."))
                .when(gridYtSupport).selectRows(eq(clientShard), any(Select.class));

        String query = String.format(QUERY_TEMPLATE, QUERY_NAME, graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null, context);

        checkPayload(result, GdClientMutationState.UNKNOWN);
    }

    @Test
    public void checkClientMutationState_WithInvalidClientMutationId() {
        request.setMutationId("invalid value");

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(
                gridDefect(GdCheckClientMutationState.MUTATION_ID.name(), CommonDefects.invalidValue()))));

        clientMutationIdGraphQlService.checkClientMutationState(request);
    }

    @Test
    public void checkClientMutationState_WithEmptyClientLogin() {
        request.setLogin("");

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(
                gridDefect(GdCheckClientMutationState.LOGIN.name(), CommonDefects.validLogin()))));

        clientMutationIdGraphQlService.checkClientMutationState(request);
    }

    private void checkPayload(ExecutionResult result, GdClientMutationState expectedState) {
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        assertThat(data)
                .containsOnlyKeys(QUERY_NAME);

        GdCheckClientMutationStatePayload expectedPayload = new GdCheckClientMutationStatePayload()
                .withState(expectedState);
        GdCheckClientMutationStatePayload payload = GraphQlJsonUtils
                .convertValue(data.get(QUERY_NAME), GdCheckClientMutationStatePayload.class);
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

}
