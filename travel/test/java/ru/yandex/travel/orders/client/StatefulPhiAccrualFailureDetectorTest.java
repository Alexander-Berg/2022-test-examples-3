package ru.yandex.travel.orders.client;

import org.junit.Ignore;

public class StatefulPhiAccrualFailureDetectorTest {

    @Ignore
    public void testIsMaster() throws InterruptedException {
        StatefulPhiAccrualFailureDetector subject = new StatefulPhiAccrualFailureDetector(
                10, 200, 500, 0, 40
        );
        subject.heartbeat(System.currentTimeMillis(), ChannelState.READY_MASTER);
        for (int i = 0; i < 100; i++) {
            Thread.sleep(1000);
            System.out.println(subject.phi(System.currentTimeMillis()));
        }

    }
}
