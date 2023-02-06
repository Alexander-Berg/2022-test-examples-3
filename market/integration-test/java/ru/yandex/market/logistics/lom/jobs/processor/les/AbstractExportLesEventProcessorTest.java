package ru.yandex.market.logistics.lom.jobs.processor.les;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.mockito.Mockito.verifyNoMoreInteractions;

class AbstractExportLesEventProcessorTest extends AbstractContextualTest {
    protected static final Instant FIXED_TIME = Instant.parse("2019-06-12T00:00:00Z");
    @Autowired
    protected LesProducer lesProducer;

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lesProducer);
    }
}
