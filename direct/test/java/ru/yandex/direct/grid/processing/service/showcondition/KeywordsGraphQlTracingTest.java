package ru.yandex.direct.grid.processing.service.showcondition;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import graphql.ExecutionResult;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.lettuce.LettuceConnectionProvider;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdBulkRefineKeywords;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdRefineKeyword;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.processor.interceptor.GridResolverInterceptor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.service.dataloader.GridDataLoaderRegistry;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.tracing.Trace;
import ru.yandex.direct.tracing.TraceGuard;
import ru.yandex.direct.tracing.TraceHelper;
import ru.yandex.direct.web.core.exception.RateLimitExceededException;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class KeywordsGraphQlTracingTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n" +
            "  %s(input: %s) {\n" +
            "    words {\n" +
            "      count\n" +
            "    }\n" +
            "  }\n" +
            "}\n";

    // Должно совпадать с maxRequests, указанном в аннотации перед тестируемым методом
    private static final int REQUESTS_TO_INCREMENT = 50;

    private GridGraphQLProcessor processor;

    private LettuceConnectionProvider lettuce;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private GridContextProvider contextProvider;

    @Autowired
    GridResolverInterceptor resolverInterceptor;


    @Autowired
    @Qualifier("gridGraphQLServices")
    private Collection<?> gridGraphQLServices;


    private TraceHelper traceHelper;
    private TraceGuard traceGuard;

    private EnvironmentType environmentType = EnvironmentType.DEVELOPMENT;
    private final Map<String, Long> mockLettuceStorage = new HashMap<>();

    @Before
    public void initTestData() {
        UserInfo userInfo = userSteps.createDefaultUser();
        User operator = userInfo.getUser();
        TestAuthHelper.setDirectAuthentication(operator);
        contextProvider.setGridContext(buildContext(operator));
        lettuce = mock(LettuceConnectionProvider.class);
        mockTraceHelper();

        processor = new GridGraphQLProcessor(gridGraphQLServices, traceHelper, environmentType, "pew",
                        lettuce, new GridDataLoaderRegistry(emptyList()), resolverInterceptor, "grid");
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    private void mockTraceHelper() {
        traceHelper = mock(TraceHelper.class);
        traceGuard = mock(TraceGuard.class);
        when(traceHelper.guard(any(Trace.class))).thenReturn(traceGuard);
    }


    @SuppressWarnings("SameParameterValue")
    private void mockLettuce(long increment) {
        RedisAdvancedClusterCommands commandsLettuce = mock(RedisAdvancedClusterCommands.class);
        when(lettuce.call(anyString(), any()))
                .then(invocation -> {
                    Function cmd = invocation.getArgument(1);
                    //noinspection unchecked
                    return cmd.apply(commandsLettuce);
                });

        mockLettuceStorage.clear();
        //noinspection unchecked
        when(commandsLettuce.set(anyString(), anyString(), any()))
                .then(invocation -> {
                    Object[] arguments = invocation.getArguments();
                    String key = (String) arguments[0];
                    long value = Long.parseLong((String) arguments[1], 10);

                    if (mockLettuceStorage.containsKey(key)) {
                        return null;
                    }

                    mockLettuceStorage.put(key, value);

                    return "OK";
                });

        //noinspection unchecked
        when(commandsLettuce.incr(anyString())).then(
                invocation -> {
                    String key = (String) invocation.getArguments()[0];
                    if (!mockLettuceStorage.containsKey(key)) {
                        mockLettuceStorage.put(key, (long) 0);
                    }

                    long value = mockLettuceStorage.get(key) + increment;
                    mockLettuceStorage.remove(key);
                    mockLettuceStorage.put(key, value);
                    return value;
                });
    }

    @Test
    public void refineKeyword_rateLimitExceededAndTraceClosed() {
        GdRefineKeyword gdRefineKeyword = new GdRefineKeyword()
                .withKeyword("keyword")
                .withGeo(singletonList(Region.RUSSIA_REGION_ID))
                .withMinusWords(singletonList("minusKeywords"));

        mockLettuce(REQUESTS_TO_INCREMENT);

        String query = String.format(QUERY_TEMPLATE, "refineKeyword", graphQlSerialize(gdRefineKeyword));

        // Первый запуск не превышает лимит запросов
        ExecutionResult result = processor.processQuery(null, query, null, contextProvider.getGridContext());

        assertThat(result.getErrors()).isEmpty();
        verify(traceHelper).guard(any(Trace.class));
        verify(traceGuard).close();

        // Второй запуск превышает лимит запросов, должна быть ошибка
        assertThatThrownBy(()-> processor.processQuery(null, query, null, contextProvider.getGridContext()))
                .isInstanceOf(RateLimitExceededException.class);
        verify(traceHelper, times(2)).guard(any(Trace.class));
        verify(traceGuard, times(2)).close();
    }

    @Test
    public void bulkRefineKeywords_rateLimitExceededAndTraceClosed() {
        GdBulkRefineKeywords gdBulkRefineKeywords = new GdBulkRefineKeywords()
                .withKeywords(singletonList("keyword"))
                .withGeo(singletonList(Region.RUSSIA_REGION_ID))
                .withMinusWords(singletonList("minusKeywords"));

        mockLettuce(REQUESTS_TO_INCREMENT);

        String query = String.format(QUERY_TEMPLATE, "bulkRefineKeywords", graphQlSerialize(gdBulkRefineKeywords));

        // Первый запуск не превышает лимит запросов
        ExecutionResult result = processor.processQuery(null, query, null, contextProvider.getGridContext());
        assertThat(result.getErrors()).isEmpty();
        verify(traceHelper).guard(any(Trace.class));
        verify(traceGuard).close();

        // Второй запуск превышает лимит запросов, должна быть ошибка
        assertThatThrownBy(()-> processor.processQuery(null, query, null, contextProvider.getGridContext()))
                .isInstanceOf(RateLimitExceededException.class);
        verify(traceHelper, times(2)).guard(any(Trace.class));
        verify(traceGuard, times(2)).close();
    }

}

