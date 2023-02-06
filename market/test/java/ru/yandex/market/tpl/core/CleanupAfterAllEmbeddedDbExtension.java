package ru.yandex.market.tpl.core;

import javax.sql.DataSource;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.core.util.DbTestUtil;

public class CleanupAfterAllEmbeddedDbExtension implements AfterAllCallback {

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        var dataSource = SpringExtension.getApplicationContext(context).getBean("dataSource", DataSource.class);
        DbTestUtil.truncateTables(dataSource);
    }

}
