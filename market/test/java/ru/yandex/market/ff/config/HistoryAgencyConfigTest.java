package ru.yandex.market.ff.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;

public class HistoryAgencyConfigTest extends IntegrationTest {
    @Autowired
    HistoryAgencyPropsConfig.HistoryAgencyConfigProperties props;

    @Test
    public void testPropsReading() {
        Assertions.assertEquals(2, props.getStorageThreadCount());
        Assertions.assertEquals(50, props.getStorageMaxBatchInEvents());
        Assertions.assertEquals(200, props.getStorageMaxBufferInEvents());
        Assertions.assertEquals(15, props.getStorageMaxDelayInSeconds());
    }

}
