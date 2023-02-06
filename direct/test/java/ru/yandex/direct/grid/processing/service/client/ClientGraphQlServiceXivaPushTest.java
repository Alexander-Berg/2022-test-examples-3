package ru.yandex.direct.grid.processing.service.client;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientGraphQlServiceXivaPushTest {

    private static final String QUERY_TEMPLATE = "{\n"
        + "  client(searchBy: {login: \"%s\"}) {\n"
        + "    info {\n"
        + "      xivaSecretSign {\n"
        + "        sign,\n"
        + "        ts\n"
        + "      }\n"
        + "    }\n"
        + "  }\n"
        + "}\n";


    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    protected GridGraphQLProcessor processor;

    @Autowired
    private UserSteps userSteps;

    private UserInfo userInfo;
    private GridGraphQLContext operatorContext;


    @Before
    public void before() {
        userInfo = userSteps.createUser(generateNewUser());
        operatorContext = new GridGraphQLContext(userInfo.getUser());
    }

    @Test
    public void testSignNotNull() {
        String query = String.format(QUERY_TEMPLATE, userInfo.getUser().getLogin());
        ExecutionResult executionResult = processor.processQuery(null, query, null, operatorContext);
        assumeThat(executionResult.getErrors(), empty());

        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "info", ImmutableMap.of(
                                "xivaSecretSign", ImmutableMap.of(
                                        "sign", "test-sign",
                                        "ts", "test-ts"
                                )
                        )
                )
        );
        Assert.assertThat(executionResult.getData(), beanDiffer(expected));
    }
}
