package ru.yandex.market.communication.proxy.tms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.telephony.client.TelephonyClient;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.communication.proxy.AbstractCommunicationProxyTest;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DisableRedirectsExecutorTest extends AbstractCommunicationProxyTest {

    @Autowired
    private DisableRedirectsExecutor executor;
    @Autowired
    private TelephonyClient telephonyClient;

    @Test
    @DbUnitDataSet(
            before = "DisableOld.before.csv",
            after = "DisableOld.after.csv"
    )
    void disableOld() {
        executor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "DisableToBeDowntimed.before.csv",
            after = "DisableToBeDowntimed.after.csv"
    )
    void disableMarked() {
        executor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "DisableOld.before.csv",
            after = "DisableOld.after.csv"
    )
    void testTelephonyCalledWithFlagEnabled() {
        executor.doJob(null);

        verify(telephonyClient, times(1)).unlinkServiceNumber("y7yvzfYQOF7", true);
        verify(telephonyClient, times(1)).unlinkServiceNumber("y7yvzfYQOF8", true);
    }

    @Test
    @DbUnitDataSet(
            before = "DisableOld.before.csv",
            after = "DisableOld.failedTelephony.csv"
    )
    void verifyDatabaseNotChangedIfTelephonyExceptionThrown() {
        doThrow(new RuntimeException())
                .when(telephonyClient)
                .unlinkServiceNumber("y7yvzfYQOF7", true);
        doThrow(new RuntimeException())
                .when(telephonyClient)
                .unlinkServiceNumber("y7yvzfYQOF8", true);

        executor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "DisableOldMultipleOrders.before.csv",
            after = "DisableOldMultipleOrders.after.csv"
    )
    @DisplayName("Проверить что удаляется только редирект по связанному заказу")
    void verifyRedirectsDisabledForSpecificOrder() {
        executor.doJob(null);
    }

}
