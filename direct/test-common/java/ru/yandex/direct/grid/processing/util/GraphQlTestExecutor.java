package ru.yandex.direct.grid.processing.util;

import java.util.Map;

import graphql.ExecutionResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.grid.processing.model.api.GdApiResponse;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;

/**
 * Небольшая обёртка для упрощения запросов в гриды для тестов.
 */
@Service
public class GraphQlTestExecutor {
    private final GridGraphQLProcessor processor;

    public GraphQlTestExecutor(@Qualifier(GRAPH_QL_PROCESSOR) GridGraphQLProcessor processor) {
        this.processor = processor;
    }

    public <I, P extends GdApiResponse> P doMutationAndGetPayload(Mutation<I, P> mutation, I input, User operator) {
        return doMutationAndGetPayload(mutation, input, operator, operator);
    }

    public <I, P extends GdApiResponse> P doMutationAndGetPayload(Mutation<I, P> mutation, I input,
                                                                  User operator, User subjectUser) {
        ExecutionResult result = doMutation(mutation, input, operator, subjectUser);
        GraphQLUtils.logErrors(result.getErrors());
        assertThat(result.getErrors()).isEmpty();
        Map<String, Object> data = result.getData();
        return GraphQlJsonUtils.convertValue(data.get(mutation.getMutationName()), mutation.getPayloadClass());
    }

    public <I> ExecutionResult doMutation(Mutation<I, ?> mutation, I input, User operator) {
        return doMutation(mutation, input, operator, operator);
    }

    public <I> ExecutionResult doMutation(Mutation<I, ?> mutation, I input, User operator, User subjectUser) {
        return processor.processQuery(null, mutation.getQuery(input), null, buildContext(operator, subjectUser));
    }

    public static <P extends GdApiResponse> void validateResponseSuccessful(P actualPayload) {
        assertThat(actualPayload.getValidationResult()).isNull();
    }

    public interface Mutation<I, P extends GdApiResponse> {
        String getMutationName();

        String getQuery(I input);

        Class<P> getPayloadClass();
    }

    public static class TemplateMutation<I, P extends GdApiResponse> implements Mutation<I, P> {
        private final String mutationName;
        private final Class<P> payloadClass;
        private final String queryTemplate;

        public TemplateMutation(String mutationName, String queryTemplate,
                                @SuppressWarnings("unused") Class<I> inputClass,
                                Class<P> payloadClass) {
            this.mutationName = mutationName;
            this.payloadClass = payloadClass;
            this.queryTemplate = queryTemplate;
        }

        @Override
        public String getMutationName() {
            return mutationName;
        }

        @Override
        public String getQuery(I input) {
            return String.format(queryTemplate, mutationName, graphQlSerialize(input));
        }

        @Override
        public Class<P> getPayloadClass() {
            return payloadClass;
        }
    }
}
