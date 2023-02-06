package ru.yandex.market.queuedcalls;


import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.queuedcalls.model.TestQCType.FIRST;
import static ru.yandex.market.queuedcalls.model.TestQCType.SECOND;

public class QueuedCallCancellationTokenTest extends AbstractQueuedCallTest {

    @Test
    public void checkGlobalCancellation() {
        Instant someTime0 = setFixedTime("2027-07-06T10:15:30");
        createQueuedCall(FIRST, 11L);
        setFixedTime("2027-11-06T13:15:30");

        QueuedCallSettings settings = qcSettingsService.readSettings();
        settings.setGloballySuspended(true);
        qcSettingsService.updateSettings(settings);

        //несмотря на успешную имплементацию, обработка не должна проходить, вызов висит в очереди
        executeQueuedCalls(FIRST, id -> {
            assertEquals(11L, id.longValue());
            return null;
        });
        checkCallInQueue(FIRST, 11L, equalTo(someTime0));

        Instant someTime1 = setFixedTime("2027-12-06T13:15:30");

        // теперь включаем и смотрим что проходит обработка
        settings = qcSettingsService.readSettings();
        settings.setGloballySuspended(false);
        qcSettingsService.updateSettings(settings);
        executeQueuedCalls(FIRST, id -> {
            assertEquals(11L, id.longValue());
            return null;
        });

        checkCallCompletedAfterExecution(FIRST, 11L, equalTo(someTime1));
    }

    @Test
    public void checkQueuedCallTypeCancellation() {
        Instant someTime0 = setFixedTime("2027-07-06T10:15:30");

        createQueuedCall(FIRST, 11L);
        createQueuedCall(SECOND, 22L);

        Instant someTime1 = setFixedTime("2027-11-06T13:15:30");

        QueuedCallSettings settings = qcSettingsService.readSettings();
        settings.getOrCreateTypeSettings(FIRST).setProcessingDisabled(true);
        qcSettingsService.updateSettings(settings);

        //несмотря на успешную имплементацию, обработка не должна проходить
        executeQueuedCalls(FIRST, id -> {
            assertEquals(11L, id.longValue());
            return null;
        });
        checkCallInQueue(FIRST, 11L, equalTo(someTime0));

        //а этот тип должен работать нормально
        executeQueuedCalls(SECOND, id -> {
            assertEquals(22L, id.longValue());
            return null;
        });
        checkCallCompletedAfterExecution(SECOND, 22L, equalTo(someTime1));
    }
}
