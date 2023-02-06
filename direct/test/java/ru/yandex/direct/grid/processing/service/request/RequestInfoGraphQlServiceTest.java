package ru.yandex.direct.grid.processing.service.request;

import java.util.Map;

import graphql.ExecutionResult;
import graphql.language.OperationDefinition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.request.RequestInfoGraphQlService.GET_REQ_ID_RESOLVER_NAME;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RequestInfoGraphQlServiceTest {

    private static final String QUERY_TEMPLATE = "%s {\n"
            + GET_REQ_ID_RESOLVER_NAME
            + "}";

    private GridGraphQLContext context;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Before
    public void initTestData() {
        context = ContextHelper.buildDefaultContext();
    }


    @Test
    public void checkQueryReqId() {
        String query = String.format(QUERY_TEMPLATE, OperationDefinition.Operation.QUERY.name().toLowerCase());

        processQueryAndCheckData(query);
    }

    @Test
    public void checkMutationGetReqId() {
        String query = String.format(QUERY_TEMPLATE, OperationDefinition.Operation.MUTATION.name().toLowerCase());

        processQueryAndCheckData(query);
    }


    private void processQueryAndCheckData(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, context);

        assertThat(result.getErrors())
                .isEmpty();

        Map<String, String> data = result.getData();
        assertThat(data)
                .containsOnlyKeys(GET_REQ_ID_RESOLVER_NAME);
        assertThat(data.get(GET_REQ_ID_RESOLVER_NAME))
                .isNotEmpty();
    }

}
