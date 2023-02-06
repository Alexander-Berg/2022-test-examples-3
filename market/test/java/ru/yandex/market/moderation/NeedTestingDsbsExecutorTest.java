package ru.yandex.market.moderation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.verifyNoInteractions;

public class NeedTestingDsbsExecutorTest extends FunctionalTest {
    @Autowired
    private NeedTestingDsbsExecutor tested;

    @Test
    @DbUnitDataSet(before = "needTestingDsbsExecutor.before.csv",
            after = "needTestingDsbsExecutor.after.csv")
    void testSuccessfulJobExecution() {
        tested.doJob(null);

        verifyNoInteractions(partnerNotificationClient);
    }
}
