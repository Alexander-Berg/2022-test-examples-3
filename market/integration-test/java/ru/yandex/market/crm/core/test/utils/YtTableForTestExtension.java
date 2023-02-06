package ru.yandex.market.crm.core.test.utils;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.crm.util.logging.LogBuilder;

/**
 * Расширение, которое создаёт на заказ таблицы в YT, а после завершения теста автоматически удаляет их.
 * Идейный наследник класса {@link YtSchemaTestHelper}, но на технологиях JUnit5.
 *
 * @author zloddey
 */
public class YtTableForTestExtension implements AfterEachCallback {
    private static final Logger LOG = LoggerFactory.getLogger(YtTableForTestExtension.class);

    private final List<Runnable> teardownActions = new ArrayList<>();

    public void create(YtClient ytClient, YPath path, String schema) {
        ytClient.createTable(path, schema);
        teardownActions.add(() -> {
            LOG.info(
                    LogBuilder.builder("#yt_table_for_test_extension")
                            .append("Removing table...")
                            .append("PATH", path)
                            .build()
            );
            ytClient.delete(path);
        });
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        teardownActions.forEach(Runnable::run);
        teardownActions.clear();
    }
}
