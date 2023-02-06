package ru.yandex.market.billing.model;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.tms.quartz2.model.Executor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;


class TmsExecutorTest extends FunctionalTest {


    @Autowired
    List<Executor> executors;

    @Test
    public void checkJobsParents() {
        for (Object executor : executors) {
            assertThat(executor + " doesn't extend from TmsExecutor", executor, instanceOf(TmsExecutor.class));
        }
    }
}
