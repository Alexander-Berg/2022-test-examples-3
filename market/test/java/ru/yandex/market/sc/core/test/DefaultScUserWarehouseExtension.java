package ru.yandex.market.sc.core.test;

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.sc.core.domain.user.model.UserRole;

public class DefaultScUserWarehouseExtension implements BeforeTestExecutionCallback {

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        var appContext = SpringExtension.getApplicationContext(context);
        var testFactory = appContext.getBean(TestFactory.class);
        var sc = testFactory.storedSortingCenter();
        testFactory.storedUser(sc, TestFactory.USER_UID_LONG, UserRole.ADMIN);
        testFactory.storedWarehouse(TestFactory.WAREHOUSE_YANDEX_ID);
    }
}
