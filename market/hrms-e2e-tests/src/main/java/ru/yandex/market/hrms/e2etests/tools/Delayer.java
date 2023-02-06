package ru.yandex.market.hrms.e2etests.tools;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
