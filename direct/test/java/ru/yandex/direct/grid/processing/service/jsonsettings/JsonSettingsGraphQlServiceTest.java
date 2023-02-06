package ru.yandex.direct.grid.processing.service.jsonsettings;

import java.util.List;
import java.util.Map;

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
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.core.frontdb.repository.JsonSettingsRepository;
import ru.yandex.direct.grid.model.jsonsettings.GdGetJsonSettings;
import ru.yandex.direct.grid.model.jsonsettings.GdSetJsonSettings;
import ru.yandex.direct.grid.model.jsonsettings.GdUpdateJsonSettingsUnion;
import ru.yandex.direct.grid.model.jsonsettings.IdType;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.jsonsettings.GdGetJsonSettingsPayload;
import ru.yandex.direct.grid.processing.model.jsonsettings.GdSetJsonSettingsPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class JsonSettingsGraphQlServiceTest {

    private static final String SET_MUTATION = "setJsonSettings";
    private static final String GET_QUERY = "getJsonSettings";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s(input: %s) {\n"
            + "    settings\n"
            + "  }\n"
            + "}";
    private static final String QUERY_TEMPLATE = ""
            + "query {\n"
            + "  %s(input: %s) {\n"
            + "    settings\n"
            + "  }\n"
            + "}";

    private User operator;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private GridContextProvider contextProvider;

    @Autowired
    private JsonSettingsRepository jsonSettingsRepository;

    @Before
    public void initTestData() {
        UserInfo userInfo = userSteps.createDefaultUser();
        operator = userInfo.getUser();
        TestAuthHelper.setDirectAuthentication(operator);
        contextProvider.setGridContext(buildContext(operator));
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void set_Success() {
        String json = "{\"key\":123}";
        doReturn(List.of(json)).when(jsonSettingsRepository).getJsonSettings(eq(operator.getClientId().asLong()),
                eq(operator.getUid()), anyList());

        GdSetJsonSettings input = new GdSetJsonSettings()
                .withIdType(IdType.UID_AND_CLIENT_ID)
                .withUpdateItems(List.of(new GdUpdateJsonSettingsUnion().withJsonPath("$.key").withNewValue(Map.of()),
                        new GdUpdateJsonSettingsUnion().withJsonPath("$.key3").withNewValue(Map.of("newKey", 0))));

        String query = String.format(MUTATION_TEMPLATE, SET_MUTATION, graphQlSerialize(input));

        ExecutionResult result = processor.processQuery(null, query, null, contextProvider.getGridContext());
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        GdSetJsonSettingsPayload payload = GraphQlJsonUtils
                .convertValue(data.get(SET_MUTATION), GdSetJsonSettingsPayload.class);

        GdSetJsonSettingsPayload expected = new GdSetJsonSettingsPayload()
                .withSettings("{\"key\":{},\"key3\":{\"newKey\":0}}");

        assertThat(payload)
                .is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void get_Success() {
        String json = "{\"key2\":123}";
        doReturn(List.of(json)).when(jsonSettingsRepository).getJsonSettings(eq(operator.getClientId().asLong()),
                eq(operator.getUid()), anyList());

        GdGetJsonSettings input = new GdGetJsonSettings()
                .withIdType(IdType.UID_AND_CLIENT_ID)
                .withJsonPath(List.of("$.key"));

        String query = String.format(QUERY_TEMPLATE, GET_QUERY, graphQlSerialize(input));

        ExecutionResult result = processor.processQuery(null, query, null, contextProvider.getGridContext());
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        GdGetJsonSettingsPayload payload = GraphQlJsonUtils
                .convertValue(data.get(GET_QUERY), GdGetJsonSettingsPayload.class);

        GdGetJsonSettingsPayload expected = new GdGetJsonSettingsPayload()
                .withSettings(List.of("{\"key2\":123}"));

        assertThat(payload)
                .is(matchedBy(beanDiffer(expected)));
    }
}
