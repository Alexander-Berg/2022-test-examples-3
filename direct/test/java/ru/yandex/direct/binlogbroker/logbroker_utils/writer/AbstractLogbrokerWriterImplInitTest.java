package ru.yandex.direct.binlogbroker.logbroker_utils.writer;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractLogbrokerWriterImplInitTest {


    private static final Duration LOGBROKER_TIMEOUT = Duration.ofSeconds(1);

    /**
     * Тест проверяет, что если у writer'а установлен флаг инициализироваться сразу, то initialSeqNo проинициализируется
     */
    @Test
    public void testLogbrokerWriter_NotLazyInit() {

        TestLogbrokerWriter logbrokerWriter = new TestLogbrokerWriter(
                123L, false
        );

        assertThat(logbrokerWriter.getInitialMaxSeqNo()).isEqualTo(123L);
    }

    /**
     * Тест проверяет, что если у writer'а установлен флаг ленивая инициализация, то initialSeqNo не
     * проинициализируется в конструкторе, а проинициализируется при первой записи
     */
    @Test
    public void testLogbrokerWriter_LazyInit() {

        TestLogbrokerWriter logbrokerWriter = new TestLogbrokerWriter(
                123L, true
        );

        assertThat(logbrokerWriter.getInitialMaxSeqNo()).isNull();
        logbrokerWriter.writeSync(List.of());
        assertThat(logbrokerWriter.getInitialMaxSeqNo()).isEqualTo(123L);
    }


    @ParametersAreNonnullByDefault
    private class TestLogbrokerWriter extends AbstractLogbrokerWriterImpl<Object> {

        @SuppressWarnings("checkstyle:parameternumber")
        private TestLogbrokerWriter(long initSeqNo, boolean lazyInit) {
            super(getSupplier(initSeqNo),
                    LOGBROKER_TIMEOUT,
                    lazyInit);
        }

        @Override
        public LogbrokerWriteRequest makeRequest(Object o) {
            return new LogbrokerWriteRequest(new byte[0]);
        }
    }

    private Supplier<CompletableFuture<AsyncProducer>> getSupplier(long initSeqNo) {
        var mockedProducer = mock(AsyncProducer.class);
        var mockedInitResponse = mock(ProducerInitResponse.class);
        when(mockedProducer.init()).thenReturn(CompletableFuture.completedFuture(mockedInitResponse));
        when(mockedInitResponse.getMaxSeqNo()).thenReturn(initSeqNo);
        return () -> CompletableFuture.completedFuture(mockedProducer);
    }

}
