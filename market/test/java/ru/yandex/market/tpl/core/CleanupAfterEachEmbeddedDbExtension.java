package ru.yandex.market.tpl.core;

import javax.sql.DataSource;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.core.util.DbTestUtil;

public class CleanupAfterEachEmbeddedDbExtension implements AfterEachCallback {


    @Override
    public void afterEach(ExtensionContext context) {
        var dataSource = SpringExtension.getApplicationContext(context).getBean("dataSource", DataSource.class);
        DbTestUtil.truncateTables(dataSource);
    }

}
