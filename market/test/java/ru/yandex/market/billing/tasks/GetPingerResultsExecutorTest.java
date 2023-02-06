package ru.yandex.market.billing.tasks;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;

public class GetPingerResultsExecutorTest extends FunctionalTest {

    @Autowired
    private GetPingerResultsExecutor getPingerResultsExecutor;

    @Test
    public void getPingerResultsExecutorTest() {
        getPingerResultsExecutor.getDayPingerCutoffsLater(123L, new Date(), new Date());
    }

    @Test
    public void getPingerResultsExecutorTestNull() {
        getPingerResultsExecutor.getDayPingerCutoffsLater(123L, null, null);
    }
}
