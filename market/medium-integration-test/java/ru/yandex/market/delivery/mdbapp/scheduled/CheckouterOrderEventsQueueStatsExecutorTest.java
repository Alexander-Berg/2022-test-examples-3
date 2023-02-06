package ru.yandex.market.delivery.mdbapp.scheduled;

import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.mdbapp.AbstractMediumContextualTest;
import ru.yandex.market.delivery.mdbapp.components.service.OrderEventsService;
import ru.yandex.market.logistics.logging.backlog.layout.logback.BackLogLayout;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("Проверка выгрузки статистики для очереди событий чекаутера")
class CheckouterOrderEventsQueueStatsExecutorTest extends AbstractMediumContextualTest {

    @Autowired
    private OrderEventsService orderEventsService;

    private CheckouterOrderEventsQueueStatsExecutor executor;

    private final ConsoleAppender logAppender = mock(ConsoleAppender.class);

    @BeforeEach
    public void setUp() {
        Logger logger = (((Logger) LoggerFactory.getLogger("BACK_LOG_TSKV")));
        LayoutWrappingEncoder layoutWrappingEncoder = new LayoutWrappingEncoder();
        layoutWrappingEncoder.setLayout(new BackLogLayout());
        logAppender.setEncoder(layoutWrappingEncoder);
        logger.addAppender(logAppender);
        logger.setLevel(Level.INFO);
    }

    @Test
    @DisplayName("Успех")
    @DatabaseSetup("/scheduled/order_events_stats/before/setup.xml")
    void success() {
        ArgumentCaptor<Appender> argumentCaptor = ArgumentCaptor.forClass(Appender.class);

        executor = new CheckouterOrderEventsQueueStatsExecutor(orderEventsService);
        executor.doJob(null);

        verify(logAppender).doAppend(argumentCaptor.capture());
        softly.assertThat(1).isEqualTo(argumentCaptor.getAllValues().size());
        LoggingEvent loggingEvent = (LoggingEvent) argumentCaptor.getAllValues().get(0);
        softly.assertThat(loggingEvent.getMessage()).isEqualTo("ORDER_EVENTS_QUEUE_STATS");
        Map<String, String> mdc = loggingEvent.getMDCPropertyMap();
        softly.assertThat(mdc.get("back_log:extra_keys")).isEqualTo("numEvents");
        softly.assertThat(mdc.get("back_log:extra_values")).isEqualTo("1");
    }


}
