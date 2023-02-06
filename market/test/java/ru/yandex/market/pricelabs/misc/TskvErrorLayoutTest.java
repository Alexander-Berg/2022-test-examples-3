package ru.yandex.market.pricelabs.misc;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class TskvErrorLayoutTest {

    @Test
    void testErrors() {
        log.info("Test info");
        log.error("Test error");
        log.error("Test error with exception", new Exception());
        log.error("Test error with exception", new Exception("not null"));
        log.error("Test error with exception", new Exception("not null 2", new Exception("not null 3")));
    }
}
