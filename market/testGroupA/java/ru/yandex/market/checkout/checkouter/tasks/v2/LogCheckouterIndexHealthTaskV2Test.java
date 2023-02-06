package ru.yandex.market.checkout.checkouter.tasks.v2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;

public class LogCheckouterIndexHealthTaskV2Test extends AbstractServicesTestBase {
    @Autowired
    LogCheckouterIndexHealthTaskV2 logCheckouterIndexHealthTaskV2;

    @Test
    public void runWithoutErrors() {
        logCheckouterIndexHealthTaskV2.run(TaskRunType.ONCE);
    }
}
