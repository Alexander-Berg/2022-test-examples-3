package ru.yandex.direct.grid.processing.processor;

import java.util.Arrays;
import java.util.Collection;

import graphql.ExecutionResult;
import graphql.schema.GraphQLSchema;
import graphql.schema.visibility.DefaultGraphqlFieldVisibility;
import graphql.schema.visibility.GraphqlFieldVisibility;
import graphql.schema.visibility.NoIntrospectionGraphqlFieldVisibility;
import org.assertj.core.api.SoftAssertions;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.common.lettuce.LettuceConnectionProvider;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.service.dataloader.GridDataLoaderRegistry;
import ru.yandex.direct.tracing.TraceHelper;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;

/**
 * Тест, который проверяет, что параметры GraphQL схемы не показываем на проде и на ТС
 */
@GridProcessingTest
@RunWith(Parameterized.class)
public class GridGraphQLSchemaFieldVisibilityTest {

    private static final String GET_SCHEMA_DATA_QUERY = "{\n"
            + "  __schema {\n"
            + "    queryType { name }\n"
            + "    mutationType { name }\n"
            + "    subscriptionType { name }\n"
            + "    types {\n"
            + "      kind\n"
            + "    \tname\n"
            + "    \tdescription\n"
            + "    }\n"
            + "    directives {\n"
            + "      name\n"
            + "      description\n"
            + "      locations\n"
            + "    }\n"
            + "  }\n"
            + "}";

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    @Qualifier("gridGraphQLServices")
    private Collection<?> gridGraphQLServices;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private LettuceConnectionProvider lettuce;

    @Autowired
    private TraceHelper traceHelper;

    @Parameterized.Parameter
    public EnvironmentType environmentType;

    @Parameterized.Parameter(1)
    public GraphqlFieldVisibility expectedGraphQLFieldVisibility;

    @Parameterized.Parameters(name = "EnvironmentType {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {EnvironmentType.PRODUCTION, NoIntrospectionGraphqlFieldVisibility.NO_INTROSPECTION_FIELD_VISIBILITY},
                {EnvironmentType.PRESTABLE, NoIntrospectionGraphqlFieldVisibility.NO_INTROSPECTION_FIELD_VISIBILITY},
                {EnvironmentType.TESTING, NoIntrospectionGraphqlFieldVisibility.NO_INTROSPECTION_FIELD_VISIBILITY},

                {EnvironmentType.DEVELOPMENT, DefaultGraphqlFieldVisibility.DEFAULT_FIELD_VISIBILITY},
                {EnvironmentType.DEVTEST, DefaultGraphqlFieldVisibility.DEFAULT_FIELD_VISIBILITY},
        });
    }


    @Test
    public void checkGraphQLSchemaFieldVisibility() {
        GraphQLSchema graphQLSchema = GridGraphQLProcessor.prepareSpqrSchema(gridGraphQLServices, environmentType,
                null);
        assertThat(graphQLSchema.getFieldVisibility())
                .isEqualTo(expectedGraphQLFieldVisibility);
    }

    @Test
    public void checkResultOf_getGraphQLSchemaData() {
        GridGraphQLProcessor processor =
                new GridGraphQLProcessor(gridGraphQLServices, traceHelper, environmentType, "pew",
                        lettuce, new GridDataLoaderRegistry(emptyList()), null, "grid");
        UserInfo userInfo = userSteps.createUser(generateNewUser());
        GridGraphQLContext operator = new GridGraphQLContext(userInfo.getUser());

        ExecutionResult result = processor.processQuery(null, GET_SCHEMA_DATA_QUERY, null, operator);
        checkResult(result, expectedGraphQLFieldVisibility);
    }

    private void checkResult(ExecutionResult result, GraphqlFieldVisibility graphQLFieldVisibility) {
        assertThat(result).isNotNull();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(result.getExtensions())
                .isNull();

        if (graphQLFieldVisibility.equals(DefaultGraphqlFieldVisibility.DEFAULT_FIELD_VISIBILITY)) {
            soft.assertThat(result.getErrors())
                    .isEmpty();
            soft.assertThat((Object) result.getData())
                    .isNotNull();
        } else {
            soft.assertThat(result.getErrors())
                    .isNotNull();
            soft.assertThat((Object) result.getData())
                    .isNull();
        }

        soft.assertAll();
    }
}
