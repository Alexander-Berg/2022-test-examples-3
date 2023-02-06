package ru.yandex.direct.grid.processing.processor;

import java.time.Instant;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

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
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.PUBLIC_GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.request.RequestInfoGraphQlService.GET_REQ_ID_RESOLVER_NAME;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class GridGraphQLResolverInterceptorForPublicApiTest {

    private static final String PUBLIC_API_QUERY = ""
            + "query {\n"
            + "  %s\n"
            + "}";

    @Autowired
    @Qualifier(PUBLIC_GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    private GridGraphQLContext context;
    private User operator;

    @Before
    public void initTestData() {
        operator = generateNewUser();

        context = new GridGraphQLContext(operator, operator)
                .withInstant(Instant.now())
                .withQueriedClient(null)
                .withFetchedFieldsReslover(null);
        //для публичного апи нет данных по аутентификации
        TestAuthHelper.setNullAuthentication();
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }


    @Test
    public void checkAllowedBlockedOperator_SkipCheckForPublicApi() {
        operator.setStatusBlocked(true);
        String query = String.format(PUBLIC_API_QUERY, GET_REQ_ID_RESOLVER_NAME);

        ExecutionResult result = processor.processQuery(null, query, null, context);

        assertThat(result.getErrors())
                .isEmpty();

        Map<String, String> data = result.getData();
        assertThat(data)
                .containsOnlyKeys(GET_REQ_ID_RESOLVER_NAME);
    }

}
