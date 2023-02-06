package ru.yandex.market.yql_test.test_listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.ITable;
import org.jetbrains.annotations.NotNull;
import org.springframework.test.context.TestContext;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.impl.YtUtils;
import ru.yandex.market.common.test.db.SingleFileCsvDataSet;
import ru.yandex.market.yql_test.YqlTablePathConverter;
import ru.yandex.market.yql_test.YtTableHelper;
import ru.yandex.market.yql_test.annotation.YqlTest;
import ru.yandex.market.yql_test.cache.YqlCache;
import ru.yandex.market.yql_test.checker.YtChecker;
import ru.yandex.market.yql_test.proxy.YqlResponseStorage;
import ru.yandex.market.yql_test.service.QryTblTestService;
import ru.yandex.market.yql_test.service.YqlProxyServerService;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static ru.yandex.market.yql_test.checker.YtCheckerSettings.ytCheckerSettingsBuilder;
import static ru.yandex.market.yql_test.utils.YqlDbUnitUtils.readCsvFromResources;
import static ru.yandex.market.yql_test.utils.YqlTestUtils.getResourcePathInVCS;

@ParametersAreNonnullByDefault
public class YqlTestListener extends YqlAbstractTestListener {

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        YqlTest annotation = testContext.getTestMethod().getAnnotation(YqlTest.class);
        if (annotation == null) {
            return;
        }

        CtxWrapper ctx = new CtxWrapper(testContext);

        String testPathInArcadia = getRequiredProperty(testContext, "yql.test.testPathInArcadia");
        Map<String, String> schemas = readSchemas(testContext, annotation);
        String initialCsvContent = read(testContext, annotation.csv());

        YqlProxyServerService proxyService = ctx.getSpringBean(YqlProxyServerService.class);
        proxyService.start();

        YqlTablePathConverter yqlTablePathConverter = ctx.getSpringBean(YqlTablePathConverter.class);
        QryTblTestService qryTblTestService = ctx.getSpringBean(QryTblTestService.class);

        Yt yt = YtUtils.http(
                getRequiredProperty(testContext, "yql.test.yt.host"),
                getRequiredProperty(testContext, "yql.datasource.token")
        );

        YtTableHelper tableHelper = ctx.setToContext(new YtTableHelper(yt, qryTblTestService));

        YqlCache yqlCache = ctx.setToContext(new YqlCache(
                testContext.getTestClass().getResourceAsStream(annotation.yqlMock()),
                getResourcePathInVCS(testContext.getTestClass(), testPathInArcadia, annotation.yqlMock())
        ));

        YqlResponseStorage yqlResponseStorage = ctx.setToContext(new YqlResponseStorage(
                yqlCache,
                yqlTablePathConverter,
                schemas,
                initialCsvContent
        ));
        proxyService.setResponseStorage(yqlResponseStorage);

        if (!isEmpty(annotation.expectedCsv())) {
            YtChecker ytChecker = ctx.setToContext(new YtChecker(
                    ytCheckerSettingsBuilder()
                            .withSchemas(schemas)
                            .withInitialCsvContent(initialCsvContent)
                            .withExpectedCsvContent(read(testContext, annotation.expectedCsv()))
                            .build(),
                    yqlCache,
                    yt,
                    yqlTablePathConverter
            ));
            proxyService.addListener(ytChecker.getListener());
        }

        proxyService.setRunBeforeSendingRequestToYqlServer(() -> {
            tableHelper.cleanTestDir();
            initTables(testContext, schemas, annotation.csv());
        });
    }

    @Override
    public void afterTestMethod(TestContext ctx) {
        YqlTest annotation = ctx.getTestMethod().getAnnotation(YqlTest.class);
        if (annotation == null) {
            return;
        }

        try {
            checkExpectedCsv(ctx);
            saveCache(ctx);
        } catch (InconsistentCacheException e) {
            getFromContext(ctx, YqlCache.class).ifPresent(YqlCache::clearCache);
        } catch (AssertionError e) {
            saveCache(ctx);
            throw e;
        } finally {
            cleanTestDir(ctx);
        }
    }

    private void checkExpectedCsv(TestContext ctx) {
        // если тест уже сгенерировал исключение, то не делаем проверку
        if (ctx.getTestException() != null) {
            return;
        }

        Optional<YtChecker> optionalYtChecker = getFromContext(ctx, YtChecker.class);
        if (optionalYtChecker.isEmpty()) {
            return;
        }

        YtChecker ytChecker = optionalYtChecker.get();
        YqlResponseStorage yqlResponseStorage = getFromContext(ctx, YqlResponseStorage.class)
                .orElseThrow(() -> new IllegalStateException("YqlResponseStorage must exist in TestContext " +
                        "when YtChecker exists"));

        if (!yqlResponseStorage.isUsedCacheQueriesEmpty() && !ytChecker.isCacheMatched()) {
            throw new InconsistentCacheException("Can't check result tables in YT because some YQL-requests was " +
                    "answered by cached YQL-responses. \n" +
                    "Cache cleared. \n" +
                    "Restart the test, please.");
        }

        ytChecker.checkYtTables();
    }

    private void cleanTestDir(TestContext testContext) {
        getFromContext(testContext, YtTableHelper.class)
                .ifPresent(helper -> {
                    if (helper.isUsed()) {
                        helper.cleanTestDir();
                    }
                });
    }

    private void saveCache(TestContext ctx) {
        getFromContext(ctx, YqlResponseStorage.class)
                .ifPresent(storage -> storage.flush(ctx.getTestException() == null));
        getFromContext(ctx, YqlCache.class).ifPresent(YqlCache::saveCache);
    }

    @NotNull
    private Map<String, String> readSchemas(TestContext ctx, YqlTest annotation) {
        Map<String, String> schemas = new HashMap<>();
        for (String schemaName : annotation.schemas()) {
            String schemaPath = String.format("%s/%s.schema",
                    annotation.schemasDir(),
                    schemaName.substring(2)
            );
            // в таблицах юзается ":" (минуты:секунды), а в именах файлов
            // для аркадии двоеточие запрещено: используем алиас "--colon--"
            var name = schemaName.replaceAll("--colon--", ":");
            schemas.put(name, read(ctx, schemaPath));
        }
        return schemas;
    }

    private void initTables(TestContext ctx, Map<String, String> schemas, String csvPath) {
        YtTableHelper tableHelper = getFromContext(ctx, YtTableHelper.class)
                .orElseThrow(() -> new IllegalStateException("YtTableHelper not found in test context"));

        SingleFileCsvDataSet dataSet = readCsvFromResources(ctx.getTestClass(), csvPath);

        for (Map.Entry<String, String> schemaNameAndValue : schemas.entrySet()) {
            String schemaName = schemaNameAndValue.getKey();
            String schema = schemaNameAndValue.getValue();
            tableHelper.createTable(schemaName, schema, getTable(dataSet, schemaName));
        }
    }

    private ITable getTable(SingleFileCsvDataSet dataSet, String schemaName) {
        try {
            return getTableChecked(dataSet, schemaName);
        } catch (DataSetException e) {
            throw new IllegalStateException("can't get table from data set", e);
        }
    }

    private ITable getTableChecked(SingleFileCsvDataSet dataSet, String schemaName) throws DataSetException {
        for (ITable table : dataSet.getTables()) {
            String tableName = table.getTableMetaData().getTableName();
            if (schemaName.equals(tableName)) {
                return table;
            }
            String withLatest = tableName.replaceAll("\\d\\d\\d\\d-\\d\\d-\\d\\d", "latest")
                    .replaceAll("\\d\\d\\d\\d-\\d\\d", "latest");
            if (schemaName.equals(withLatest)) {
                return table;
            }
        }
        throw new IllegalStateException("No table in csv " + schemaName);
    }
}
