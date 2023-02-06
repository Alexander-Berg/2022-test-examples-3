package ru.yandex.market.adv.yt.test.extension;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ExceptionCollector;

import ru.yandex.market.adv.loader.file.FileLoader;
import ru.yandex.market.adv.yt.YtDynamicClientFactory;
import ru.yandex.market.adv.yt.YtStaticClientFactory;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.adv.yt.test.model.TableContent;
import ru.yandex.market.adv.yt.test.model.TableInfo;
import ru.yandex.market.adv.yt.test.service.YtDynamicService;
import ru.yandex.market.adv.yt.test.service.YtService;
import ru.yandex.market.adv.yt.test.service.YtStaticService;
import ru.yandex.market.common.test.annotation.AnnotationUtils;
import ru.yandex.market.yt.client.YtClientProxy;

/**
 * Класс-расширение для JUNIT 5,
 * нацеленное на предварительную загрузку таблиц и их сверку с ожидаемым результатом по завершению выполнения теста.
 * Date: 11.01.2022
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
public class YtExtension implements BeforeEachCallback, AfterEachCallback {

    private final YtService ytDynamicService = new YtDynamicService();
    private final YtService ytStaticService = new YtStaticService();

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        call(context,
                YtUnitDataSet::before,
                YtUnitDataSet::create,
                ytUnitDataSet -> null,
                tableInfo -> {
                    if (tableInfo.isDynamic()) {
                        ytDynamicService.createTable(tableInfo);
                    } else {
                        ytStaticService.createTable(tableInfo);
                    }
                });
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        call(context,
                ytUnitDataSet -> StringUtils.isEmpty(ytUnitDataSet.after())
                        ? ytUnitDataSet.before()
                        : ytUnitDataSet.after(),
                ytUnitDataSet -> null,
                YtUnitDataSet::exist,
                tableInfo -> {
                    if (tableInfo.isDynamic()) {
                        ytDynamicService.checkAndCleanTable(tableInfo);
                    } else {
                        ytStaticService.checkAndCleanTable(tableInfo);
                    }
                });
    }

    private void call(ExtensionContext context,
                      Function<YtUnitDataSet, String> getTableFileFunction,
                      Function<YtUnitDataSet, Boolean> createTableFunction,
                      Function<YtUnitDataSet, Boolean> existTableFunction,
                      Consumer<TableInfo> tableInfoConsumer) throws Exception {
        Set<YtUnitDataSet> dataSets = getDataSets(context);
        if (dataSets.isEmpty()) {
            return;
        }

        ApplicationContext springContext = SpringExtension.getApplicationContext(context);
        ObjectMapper objectMapper = springContext.getBean("ytObjectMapper", ObjectMapper.class);
        FileLoader fileLoader = springContext.getBean(FileLoader.class);
        ExceptionCollector exceptionCollector = new ExceptionCollector();

        for (YtUnitDataSet dataSet : dataSets) {
            exceptionCollector.execute(
                    () -> {
                        YtUnitScheme scheme = dataSet.scheme();
                        String tableFile = getTableFileFunction.apply(dataSet);

                        YtClientProxy ytClient = scheme.isDynamic()
                                ? springContext.getBean(YtDynamicClientFactory.class).createClient()
                                : springContext.getBean(YtStaticClientFactory.class).createClient();

                        List<?> rows = StringUtils.isEmpty(tableFile) ?
                                List.of() :
                                objectMapper.readValue(
                                        fileLoader.loadFile(tableFile, context.getRequiredTestClass()),
                                        objectMapper.getTypeFactory()
                                                .constructCollectionType(List.class, scheme.model())
                                );
                        TableInfo tableInfo = TableInfo.builder()
                                .ytClient(ytClient)
                                .model(scheme.model())
                                .dynamic(scheme.isDynamic())
                                .path(scheme.path())
                                .tableContent(TableContent.builder()
                                        .create(createTableFunction.apply(dataSet))
                                        .exist(existTableFunction.apply(dataSet))
                                        .rows(rows)
                                        .build())
                                .ignoreColumns(scheme.ignoreColumns())
                                .build();

                        tableInfoConsumer.accept(tableInfo);
                    }
            );
        }

        exceptionCollector.assertEmpty();
    }

    private Set<YtUnitDataSet> getDataSets(ExtensionContext context) {
        return context.getTestMethod()
                .map(testMethod -> AnnotationUtils.findMethodAnnotations(testMethod, YtUnitDataSet.class))
                .orElse(Set.of());
    }
}
