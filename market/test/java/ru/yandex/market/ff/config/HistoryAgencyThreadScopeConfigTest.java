package ru.yandex.market.ff.config;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.config.HistoryAgencyContextBeanConfig.HistoryStepContext;
import ru.yandex.market.ff.config.HistoryAgencyContextBeanConfig.StateTransition;
import ru.yandex.market.ff.framework.history.wrapper.MyThreadContext;

class HistoryAgencyThreadScopeConfigTest extends IntegrationTest {

    @Autowired
    private ObjectFactory<MyThreadContext<HistoryStepContext>> historyBlockContextGetter;

    @Test
    public void test() throws InterruptedException {


        AtomicReference<Optional<StateTransition>> statusThreadA = new AtomicReference<>();
        AtomicReference<Optional<StateTransition>> statusThreadAafter = new AtomicReference<>();
        AtomicReference<Optional<StateTransition>> statusThreadB = new AtomicReference<>();
        AtomicReference<Optional<StateTransition>> statusThreadBafter = new AtomicReference<>();
        Thread threadA = new Thread(() -> {
            statusThreadA.set(historyBlockContextGetter.getObject().getStepContext().getStateTransition());
            historyBlockContextGetter.getObject().getStepContext()
                    .setStatusChange(RequestStatus.CREATED, RequestStatus.FINISHED);
            statusThreadAafter.set(historyBlockContextGetter.getObject().getStepContext().getStateTransition());
        });

        Thread threadB = new Thread(() -> {
            statusThreadB.set(historyBlockContextGetter.getObject().getStepContext().getStateTransition());
            historyBlockContextGetter.getObject().getStepContext()
                    .setStatusChange(RequestStatus.CREATED, RequestStatus.IN_PROGRESS);
            statusThreadBafter.set(historyBlockContextGetter.getObject().getStepContext().getStateTransition());
        });

        threadA.start();
        threadA.join();

        threadB.start();
        threadB.join();

        Assertions.assertEquals(Optional.empty(), statusThreadA.get());
        Assertions.assertEquals(Optional.empty(), statusThreadB.get());
        Assertions.assertEquals(
                Optional.of(new StateTransition(RequestStatus.CREATED, RequestStatus.FINISHED)),
                statusThreadAafter.get());
        Assertions.assertEquals(
                Optional.of(new StateTransition(RequestStatus.CREATED, RequestStatus.IN_PROGRESS)),
                statusThreadBafter.get());
    }
}
