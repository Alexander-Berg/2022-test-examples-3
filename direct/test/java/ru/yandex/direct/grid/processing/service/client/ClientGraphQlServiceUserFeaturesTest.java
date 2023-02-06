package ru.yandex.direct.grid.processing.service.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.client.GdUserFeature;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientGraphQlServiceUserFeaturesTest {

    private static final String QUERY_TEMPLATE = "{\n"
            + ClientGraphQlService.USER_FEATURES_RESOLVER_NAME +"\n"
            + "}\n";
    private User subjectUser;
    private GridGraphQLContext operatorContext;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private UserSteps userSteps;

    @Before
    public void before() {
        UserInfo userInfo = userSteps.createUser(generateNewUser());
        subjectUser = generateNewUser();
        operatorContext = new GridGraphQLContext(userInfo.getUser(), subjectUser);
    }


    @Test
    public void testUserFeatures_StatusBlockedTrue() {
        subjectUser.setStatusBlocked(true);

        String query = String.format(QUERY_TEMPLATE);
        List<GdUserFeature> expectedUserFeatures = List.of(GdUserFeature.BLOCKED);

        ExecutionResult result = processor.processQuery(null, query, null, operatorContext);

        assertThat(result.getErrors()).isEmpty();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(ClientGraphQlService.USER_FEATURES_RESOLVER_NAME,
                mapList(expectedUserFeatures, GdUserFeature::toString));

        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testUserFeatures_StatusBlockedFalse() {
        subjectUser.setStatusBlocked(false);
        String query = String.format(QUERY_TEMPLATE);
        List<GdUserFeature> expectedUserFeatures = List.of();

        ExecutionResult result = processor.processQuery(null, query, null, operatorContext);

        assertThat(result.getErrors()).isEmpty();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(ClientGraphQlService.USER_FEATURES_RESOLVER_NAME,
                mapList(expectedUserFeatures, GdUserFeature::toString));

        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }
}
