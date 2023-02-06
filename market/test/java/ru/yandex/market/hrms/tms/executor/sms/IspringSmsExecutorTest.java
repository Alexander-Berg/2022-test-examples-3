package ru.yandex.market.hrms.tms.executor.sms;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.test.configurer.YaSmsConfigurer;
import ru.yandex.market.hrms.tms.AbstractTmsTest;

@TestPropertySource(properties = {
        "market-hrms.ispring.sms.batch-size=2"
})
class IspringSmsExecutorTest extends AbstractTmsTest {

    @Autowired
    private IspringSmsExecutor executor;

    @Autowired
    private YaSmsConfigurer yaSmsConfigurer;

    private static final String SUCCESS_RESULT = """
            <?xml version="1.0" encoding="windows-1251"?>
            <doc>
                <message-sent id="127000000003456" />
                <gates ids="15" />
            </doc>
            """;

    @Test
    @DbUnitDataSet(
            before = "IspringSmsExecutorTest.employeeHappyPath.before.csv",
            after = "IspringSmsExecutorTest.employeeHappyPath.after.csv"
    )
    public void employeeHappyPath() {
        yaSmsConfigurer.mockSendSmsSuccess(SUCCESS_RESULT);
        executor.executeJob(null);
        executor.executeJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "IspringSmsExecutorTest.outstaffHappyPath.before.csv",
            after = "IspringSmsExecutorTest.outstaffHappyPath.after.csv"
    )
    public void outstaffHappyPath() {
        yaSmsConfigurer.mockSendSmsSuccess(SUCCESS_RESULT);
        executor.executeJob(null);
        executor.executeJob(null);
    }
}
