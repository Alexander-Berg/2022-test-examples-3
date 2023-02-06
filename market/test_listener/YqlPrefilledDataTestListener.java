package ru.yandex.market.yql_test.test_listener;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.test.context.TestContext;

import ru.yandex.market.yql_test.YqlTablePathConverter;
import ru.yandex.market.yql_test.annotation.YqlPrefilledDataTest;
import ru.yandex.market.yql_test.cache.YqlCache;
import ru.yandex.market.yql_test.proxy.YqlResponseStorage;
import ru.yandex.market.yql_test.service.QuerySuffixesService;
import ru.yandex.market.yql_test.service.YqlProxyServerService;

import static java.util.Collections.emptyMap;
import static ru.yandex.market.yql_test.utils.YqlTestUtils.getResourcePathInVCS;

public class YqlPrefilledDataTestListener extends YqlAbstractTestListener {

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        YqlPrefilledDataTest annotation = testContext.getTestMethod().getAnnotation(YqlPrefilledDataTest.class);
        if (annotation == null) {
            return;
        }

        CtxWrapper ctx = new CtxWrapper(testContext);

        String testPathInArcadia = getRequiredProperty(testContext, "yql.test.testPathInArcadia");

        YqlProxyServerService proxyService = ctx.getSpringBean(YqlProxyServerService.class);
        proxyService.start();

        YqlTablePathConverter yqlTablePathConverter = ctx.getSpringBean(YqlTablePathConverter.class);
        QuerySuffixesService suffixesService = ctx.setToContext(ctx.getSpringBean(QuerySuffixesService.class));

        suffixesService.setQuerySuffixes(
                Arrays.stream(annotation.queries())
                        .collect(
                                Collectors.toMap(
                                        YqlPrefilledDataTest.Query::name,
                                        YqlPrefilledDataTest.Query::suffix
                                )
                        )
        );

        YqlCache yqlCache = ctx.setToContext(new YqlCache(
                testContext.getTestClass().getResourceAsStream(annotation.yqlMock()),
                getResourcePathInVCS(testContext.getTestClass(), testPathInArcadia, annotation.yqlMock())
        ));

        YqlResponseStorage yqlResponseStorage = ctx.setToContext(new YqlResponseStorage(
                yqlCache,
                yqlTablePathConverter,
                emptyMap(),
                ""
        ));

        proxyService.setResponseStorage(yqlResponseStorage);
        proxyService.setRunBeforeSendingRequestToYqlServer(null);
    }

    @Override
    public void afterTestMethod(@NonNull TestContext ctx) {
        getFromContext(ctx, QuerySuffixesService.class)
                .ifPresent(queryService -> {
                    queryService.setQuerySuffixes(null);

                    //store only for YqlPrefilledDataTest tests
                    getFromContext(ctx, YqlResponseStorage.class)
                            .ifPresent(storage -> storage.flush(ctx.getTestException() == null));
                    getFromContext(ctx, YqlCache.class).ifPresent(YqlCache::saveCache);

                });
    }
}
