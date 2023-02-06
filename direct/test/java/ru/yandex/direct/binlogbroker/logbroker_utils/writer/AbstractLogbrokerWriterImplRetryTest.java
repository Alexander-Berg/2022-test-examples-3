package ru.yandex.direct.binlogbroker.logbroker_utils.writer;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.kikimr.persqueue.producer.AsyncProducer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static ru.yandex.direct.binlogbroker.logbroker_utils.writer.LogbrokerProducerTestUtils.createFailureCloseProducerSupplier;
import static ru.yandex.direct.binlogbroker.logbroker_utils.writer.LogbrokerProducerTestUtils.createFailureProducerSupplier;

public class AbstractLogbrokerWriterImplRetryTest {

    /**
     * Методы init() и write() из замоканного LogbrokerProducer'а должны завершиться за это время
     **/
    private static final Duration LOGBROKER_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration RETRY_DELAY = Duration.ofSeconds(0);

    /**
     * Тест проверяет, что при неудавшейся записи в Logbroker, она будет повторена retryCnt раз и завершится с ошибкой,
     * при этом каждый раз будет закрываться старая и открываться новая сессия logbrokerProducer'a
     **/
    @Test
    public void testLogbrokerWriter_RetryAsyncWrite() {
        int retryCnt = 10;
        int writeFailureCnt = retryCnt + 1;
        int initFailureCnt = 0;
        int createFailureCnt = 0;
        AtomicLong writeCnt = new AtomicLong();
        AtomicLong initCnt = new AtomicLong();
        AtomicLong closeCnt = new AtomicLong();
        AtomicLong createCnt = new AtomicLong();
        RuntimeException writeException = new RuntimeException("write exception");

        FailureLogbrokerWriter logbrokerWriter = new FailureLogbrokerWriter(
                writeCnt,
                initCnt,
                closeCnt,
                createCnt,
                writeFailureCnt,
                initFailureCnt,
                createFailureCnt,
                retryCnt,
                writeException
        );

        assertThatThrownBy(() -> logbrokerWriter.write(Collections.singletonList(new Object())).get())
                .isInstanceOf(ExecutionException.class)
                .hasCause(writeException);
        assertThat(writeCnt.get()).isEqualTo(retryCnt + 1);
        assertThat(initCnt.get()).isEqualTo(retryCnt + 1);
        assertThat(closeCnt.get()).isEqualTo(retryCnt);
        assertThat(createCnt.get()).isEqualTo(retryCnt + 1);
    }

    /**
     * Тест проверяет, что при неудавшейся один раз записи в Logbroker, она повторится еще раз и заершится успешно,
     * при этом старая сессия logbrokerProducer'a будет закрыта и открыта новая
     **/
    @Test
    public void testLogbrokerWriter_RetryAsyncWrite_OneTimeFailure() {
        int retryCnt = 10;
        int writeFailureCnt = 1;
        int initFailureException = 0;
        int createFailureCnt = 0;
        AtomicLong writeCnt = new AtomicLong();
        AtomicLong initCnt = new AtomicLong();
        AtomicLong closeCnt = new AtomicLong();
        AtomicLong createCnt = new AtomicLong();
        RuntimeException writeException = new RuntimeException("write exception");

        FailureLogbrokerWriter logbrokerWriter = new FailureLogbrokerWriter(
                writeCnt,
                initCnt,
                closeCnt,
                createCnt,
                writeFailureCnt,
                initFailureException,
                createFailureCnt,
                retryCnt,
                writeException);


        assertThatCode(() -> logbrokerWriter.write(Collections.singletonList(new Object())).get())
                .doesNotThrowAnyException();
        assertThat(writeCnt.get()).isEqualTo(writeFailureCnt + 1);
        assertThat(initCnt.get()).isEqualTo(writeFailureCnt + 1);
        assertThat(closeCnt.get()).isEqualTo(writeFailureCnt);
        assertThat(createCnt.get()).isEqualTo(writeFailureCnt + 1);
    }


    /**
     * Тест проверяет, что при неудавшейся инициализации LogbrokerProducer'a,
     * она будет повторена retryCnt раз и завершится с ошибкой
     **/
    @Test
    public void testLogbrokerWriter_RetryAsyncInit() {
        int retryCnt = 2;
        int initFailureCnt = retryCnt + 1;
        int writeFailureCnt = 0;
        int createFailureCnt = 0;
        RuntimeException initException = new RuntimeException("init exception");
        AtomicLong writeCnt = new AtomicLong();
        AtomicLong initCnt = new AtomicLong();
        AtomicLong closeCnt = new AtomicLong();
        AtomicLong createCnt = new AtomicLong();
        FailureLogbrokerWriter logbrokerWriter = new FailureLogbrokerWriter(
                writeCnt,
                initCnt,
                closeCnt,
                createCnt,
                writeFailureCnt,
                initFailureCnt,
                createFailureCnt,
                retryCnt,
                initException);
        assertThatThrownBy(() -> logbrokerWriter.write(Collections.singletonList(new Object())).get())
                .isInstanceOf(ExecutionException.class)
                .hasCause(initException);
        assertThat(initCnt.get()).isEqualTo(retryCnt + 1);
        assertThat(writeCnt.get()).isEqualTo(0);
        assertThat(closeCnt.get()).isEqualTo(0);
        assertThat(createCnt.get()).isEqualTo(1);
    }

    /**
     * Тест проверяет, что при неудавшейся один раз инициализации LogbrokerProducer'a,
     * она будет повторена один раз и заершится успешно
     **/
    @Test
    public void testLogbrokerWriter_RetryAsyncInit_OneTimeFailure() {
        int retryCnt = 10;
        int initFailureCnt = 1;
        int writeFailureCnt = 0;
        int createFailureCnt = 0;
        AtomicLong writeCnt = new AtomicLong();
        AtomicLong initCnt = new AtomicLong();
        AtomicLong closeCnt = new AtomicLong();
        AtomicLong createCnt = new AtomicLong();
        RuntimeException initException = new RuntimeException("init exception");

        FailureLogbrokerWriter logbrokerWriter = new FailureLogbrokerWriter(
                writeCnt,
                initCnt,
                closeCnt,
                createCnt,
                writeFailureCnt,
                initFailureCnt,
                createFailureCnt,
                retryCnt,
                initException);

        assertThatCode(() -> logbrokerWriter.write(Collections.singletonList(new Object())).get())
                .doesNotThrowAnyException();
        assertThat(initCnt.get()).isEqualTo(initFailureCnt + 1);
        assertThat(writeCnt.get()).isEqualTo(1);
        assertThat(closeCnt.get()).isEqualTo(0);
        assertThat(createCnt.get()).isEqualTo(1);
    }

    /**
     * Тест проверяет, что при неудавшемся несколько раз создании LogbrokerProducer'a,
     * оно повторится несколько раз и все дальнейшие действия завершатся успешно
     **/
    @Test
    public void testLogbrokerWriter_RetryAsyncCreate_FailureSeveralTime() {
        int retryCnt = 10;
        int initFailureCnt = 0;
        int writeFailureCnt = 0;
        int createFailureCnt = 3;
        AtomicLong writeCnt = new AtomicLong();
        AtomicLong initCnt = new AtomicLong();
        AtomicLong closeCnt = new AtomicLong();
        AtomicLong createCnt = new AtomicLong();
        RuntimeException createException = new RuntimeException("create exception");

        FailureLogbrokerWriter logbrokerWriter = new FailureLogbrokerWriter(
                writeCnt,
                initCnt,
                closeCnt,
                createCnt,
                writeFailureCnt,
                initFailureCnt,
                createFailureCnt,
                retryCnt,
                createException);

        assertThatCode(() -> logbrokerWriter.write(Collections.singletonList(new Object())).get())
                .doesNotThrowAnyException();
        assertThat(initCnt.get()).isEqualTo(1);
        assertThat(writeCnt.get()).isEqualTo(1);
        assertThat(closeCnt.get()).isEqualTo(0);
        assertThat(createCnt.get()).isEqualTo(createFailureCnt + 1);
    }

    /**
     * Тест проверяет, что при неудавшемся создании LogbrokerProducer'a,
     * оно повторится retryCnt раз и завершится с ошибкой
     **/
    @Test
    public void testLogbrokerWriter_RetryAsyncCreate_Failure() {
        int retryCnt = 10;
        int initFailureCnt = 0;
        int writeFailureCnt = 0;
        int createFailureCnt = retryCnt + 1;
        AtomicLong writeCnt = new AtomicLong();
        AtomicLong initCnt = new AtomicLong();
        AtomicLong closeCnt = new AtomicLong();
        AtomicLong createCnt = new AtomicLong();
        RuntimeException createException = new RuntimeException("create exception");

        FailureLogbrokerWriter logbrokerWriter = new FailureLogbrokerWriter(
                writeCnt,
                initCnt,
                closeCnt,
                createCnt,
                writeFailureCnt,
                initFailureCnt,
                createFailureCnt,
                retryCnt,
                createException);

        assertThatThrownBy(() -> logbrokerWriter.write(Collections.singletonList(new Object())).get())
                .isInstanceOf(ExecutionException.class)
                .hasCause(createException);
        assertThat(initCnt.get()).isEqualTo(0);
        assertThat(writeCnt.get()).isEqualTo(0);
        assertThat(closeCnt.get()).isEqualTo(0);
        assertThat(createCnt.get()).isEqualTo(retryCnt + 1);
    }

    /**
     * Тест проверяет, что неудачное закрытие logbrokerProducer'a не влияет на дальнейшие попытки записи
     **/
    @Test
    public void testLogbrokerWriter_FailureClose() {
        int retryCnt = 10;
        int writeFailureCnt = retryCnt + 1;
        int initFailureCnt = 0;
        int createFailureCnt = 0;
        AtomicLong writeCnt = new AtomicLong();
        AtomicLong initCnt = new AtomicLong();
        AtomicLong closeCnt = new AtomicLong();
        AtomicLong createCnt = new AtomicLong();
        RuntimeException writeException = new RuntimeException("write exception");

        FailureCloseLogbrokerWriter logbrokerWriter = new FailureCloseLogbrokerWriter(
                writeCnt,
                initCnt,
                closeCnt,
                createCnt,
                writeFailureCnt,
                initFailureCnt,
                createFailureCnt,
                retryCnt,
                writeException
        );

        assertThatThrownBy(() -> logbrokerWriter.write(Collections.singletonList(new Object())).get())
                .isInstanceOf(ExecutionException.class)
                .hasCause(writeException);
        assertThat(writeCnt.get()).isEqualTo(retryCnt + 1);
        assertThat(initCnt.get()).isEqualTo(retryCnt + 1);
        assertThat(closeCnt.get()).isEqualTo(retryCnt);
        assertThat(createCnt.get()).isEqualTo(retryCnt + 1);
    }

    @ParametersAreNonnullByDefault
    private class FailureLogbrokerWriter extends AbstractLogbrokerWriterImpl<Object> {

        /**
         * @param writeFailureCnt количество неудачных завершений метода {@link AsyncProducer#write(byte[], long)}
         * @param initFailureCnt  количество неудачных завершений метода {@link AsyncProducer#init()}
         * @param exception       исключение, бросающееся в случае неудачных завершений методов
         **/
        @SuppressWarnings("checkstyle:parameternumber")
        private FailureLogbrokerWriter(AtomicLong writeCnt, AtomicLong initCnt, AtomicLong closeCnt,
                                       AtomicLong createCnt,
                                       int writeFailureCnt, int initFailureCnt, int createFailureCnt, int retryCount,
                                       RuntimeException exception) {
            super(createFailureProducerSupplier(writeCnt, initCnt, closeCnt, createCnt, writeFailureCnt, initFailureCnt,
                    createFailureCnt,
                    exception),
                    LOGBROKER_TIMEOUT,
                    new LogbrokerWriterRetryConfig(retryCount, RETRY_DELAY));
        }

        @Override
        public LogbrokerWriteRequest makeRequest(Object o) {
            return new LogbrokerWriteRequest(new byte[0]);
        }
    }


    /**
     * Класс аналогичный {@link FailureLogbrokerWriter},
     * но внутри создается logbrokerProducer с неудачно завершающися методом {@link AsyncProducer#closeFuture()}
     **/
    @ParametersAreNonnullByDefault
    @SuppressWarnings("checkstyle:parameternumber")
    private class FailureCloseLogbrokerWriter extends AbstractLogbrokerWriterImpl<Object> {
        private FailureCloseLogbrokerWriter(
                AtomicLong writeCnt, AtomicLong initCnt, AtomicLong closeCnt,
                AtomicLong createCnt, int writeFailureCnt, int initFailureCnt, int createFailureCnt, int retryCount,
                RuntimeException exception
        ) {
            super(createFailureCloseProducerSupplier(writeCnt, initCnt, closeCnt, createCnt, writeFailureCnt,
                    initFailureCnt, createFailureCnt,
                    exception),
                    LOGBROKER_TIMEOUT,
                    new LogbrokerWriterRetryConfig(retryCount, RETRY_DELAY));
        }

        @Override
        public LogbrokerWriteRequest makeRequest(Object o) {
            return new LogbrokerWriteRequest(new byte[0]);
        }
    }
}
