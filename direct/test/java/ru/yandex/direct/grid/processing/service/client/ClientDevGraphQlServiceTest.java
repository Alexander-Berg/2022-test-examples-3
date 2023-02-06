package ru.yandex.direct.grid.processing.service.client;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.test.utils.RandomEnumUtils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientDevGraphQlServiceTest {
    private static final String FULL_QUERY = "{\n"
            + "  clientDev {\n"
            + "    clientId\n"
            + "    shard\n"
            + "    uid\n"
            + "    environment\n"
            + "    role\n"
            + "    operatorLogin\n"
            + "    operatorFeatures\n"
            + "    clientFeatures\n"
            + "  }\n"
            + "}\n";

    private static final String PARTIAL_QUERY = "{\n"
            + "  clientDev {\n"
            + "    clientId\n"
            + "    shard\n"
            + "    role\n"
            + "  }\n"
            + "}\n";

    private static final FeatureName CLIENT_FEATURE = RandomEnumUtils.getRandomEnumValue(FeatureName.class);
    private static final FeatureName OPERATOR_FEATURE = RandomEnumUtils.getRandomEnumValue(FeatureName.class);

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private EnvironmentType environmentType;

    @Test
    public void fullQuery() {
        UserInfo userInfo = steps.userSteps().createUser(generateNewUser());
        steps.featureSteps().addClientFeature(userInfo.getClientInfo().getClientId(),
                CLIENT_FEATURE, true);
        GridGraphQLContext context = new GridGraphQLContext(userInfo.getUser());

        ExecutionResult result = processor.processQuery(null, FULL_QUERY, null, context);
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
                "clientDev",
                ImmutableMap.<String, Object>builder()
                        .put("clientId", userInfo.getClientInfo().getClientId().asLong())
                        .put("shard", userInfo.getShard())
                        .put("uid", userInfo.getUid())
                        .put("environment", environmentType.name())
                        .put("role", RbacRole.CLIENT.name())
                        .put("operatorLogin", userInfo.getUser().getLogin())
                        .put("clientFeatures", singletonList(CLIENT_FEATURE.getName()))
                        .put("operatorFeatures", singletonList(CLIENT_FEATURE.getName()))
                        .build()
        );
        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void partialQuery() {
        UserInfo userInfo = steps.userSteps().createUser(generateNewUser());
        GridGraphQLContext context = new GridGraphQLContext(userInfo.getUser());

        ExecutionResult result = processor.processQuery(null, PARTIAL_QUERY, null, context);
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
                "clientDev",
                ImmutableMap.of(
                        "clientId", userInfo.getClientInfo().getClientId().asLong(),
                        "shard", userInfo.getShard(),
                        "role", RbacRole.CLIENT.name()
                )
        );
        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void correctWhenSubjectNotEqualsOperator() {
        UserInfo operator = steps.userSteps().createUser(generateNewUser());
        UserInfo subjectUser = steps.userSteps().createUser(generateNewUser());
        steps.featureSteps().addClientFeature(operator.getClientInfo().getClientId(), OPERATOR_FEATURE, true);
        steps.featureSteps().addClientFeature(subjectUser.getClientInfo().getClientId(), CLIENT_FEATURE, true);
        GridGraphQLContext context = new GridGraphQLContext(operator.getUser(), subjectUser.getUser());

        ExecutionResult result = processor.processQuery(null, FULL_QUERY, null, context);
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
                "clientDev",
                ImmutableMap.<String, Object>builder()
                        .put("clientId", subjectUser.getClientInfo().getClientId().asLong())
                        .put("shard", subjectUser.getShard())
                        .put("uid", subjectUser.getUid())
                        .put("environment", environmentType.name())
                        .put("role", RbacRole.CLIENT.name())
                        .put("operatorLogin", operator.getUser().getLogin())
                        .put("clientFeatures", singletonList(CLIENT_FEATURE.getName()))
                        .put("operatorFeatures", singletonList(OPERATOR_FEATURE.getName()))
                        .build()
        );
        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }
}
