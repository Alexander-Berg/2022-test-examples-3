package ru.yandex.market.mbo.billing.counter.tt;

import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.tt.model.TaskType;

/**
 * Логика на 99% идентична CheckFillTaskCounterTest, поэтому просто переопределим сам счётчик и TaskType.
 */
@SuppressWarnings("checkstyle:magicnumber")
@RunWith(MockitoJUnitRunner.class)
public class CheckModelFromVendorTaskCounterTest extends CheckFillTaskCounterTest {

    @Spy
    private CheckModelFromVendorTaskCounter counter = new CheckModelFromVendorTaskCounter();

    @Override
    protected TTOperationCounter getCounter() {
        return counter;
    }

    @Override
    protected int getExpectedTaskTypeId() {
        return TaskType.CHECK_FILL_MODEL_FROM_VENDOR.getId();
    }
}
