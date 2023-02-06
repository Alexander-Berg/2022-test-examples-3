package ru.yandex.market.delivery.deliveryintegrationtests.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class Delayer {
    private static final Logger log = LoggerFactory.getLogger(Delayer.class);

    private Delayer() { }

    public static void delay(long timeout, TimeUnit timeUnit) {
        log.info("Waiting for {} {}", timeout, timeUnit.toString().toLowerCase());
        try {
            timeUnit.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
