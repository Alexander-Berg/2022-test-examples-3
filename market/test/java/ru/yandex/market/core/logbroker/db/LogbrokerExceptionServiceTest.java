package ru.yandex.market.core.logbroker.db;

import java.time.Instant;
import java.util.Collections;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.kikimr.persqueue.consumer.StreamListener;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.logbroker.consumer.LogbrokerDataProcessor;
import ru.yandex.market.logbroker.consumer.LogbrokerException;
import ru.yandex.market.logbroker.consumer.TransactionalLogbrokerListener;
import ru.yandex.market.logbroker.db.LogbrokerMonitorExceptionsService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link LogbrokerMonitorExceptionsService} и {@link TransactionalLogbrokerListener}
 */
class LogbrokerExceptionServiceTest extends FunctionalTest {

    private static final String TEST_TOPIC_NAME = "test_topic";
    private static final String GENERAL_TOPIC_NAME = "general_logbroker_exception";
    private static final String TEST_HOST = "test_host";
    private static final Instant SEEN_AT = DateTimes.toInstantAtDefaultTz(2020, 12, 15);

    @Autowired
    private LogbrokerMonitorExceptionsService logbrokerMonitorExceptionsService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DbUnitDataSet(
            after = "LogbrokerExceptionsTest.addNewExceptionTest.csv"
    )
    void addNewExceptionTest() {
        logbrokerMonitorExceptionsService.addException(getLbException());
    }

    @Test
    @DbUnitDataSet(
            before = "LogbrokerExceptionsTest.addExistedExceptionTest.before.csv",
            after = "LogbrokerExceptionsTest.addExistedExceptionTest.after.csv"
    )
    void addExistedExceptionTest() {
        logbrokerMonitorExceptionsService.addException(getLbException());
    }

    @Test
    @DbUnitDataSet(
            before = "LogbrokerExceptionsTest.addExistedOtherTestExceptionTest.before.csv",
            after = "LogbrokerExceptionsTest.addExistedOtherTestExceptionTest.after.csv"
    )
    void addExistedOtherTeamExceptionTest() {
        logbrokerMonitorExceptionsService.addException(getLbException());
    }

    @Test
    @DbUnitDataSet(
            before = "LogbrokerExceptionsTest.deleteExistedTestExceptionTest.before.csv",
            after = "LogbrokerExceptionsTest.deleteExistedTestExceptionTest.after.csv"
    )
    void deleteExceptionsTest() {
        logbrokerMonitorExceptionsService.deleteException(TEST_TOPIC_NAME);
    }

    @Test
    void listenerExceptionTest() {
        MessageBatch mb = new MessageBatch(
                TEST_TOPIC_NAME,
                1,
                Collections.singletonList(new MessageData("data".getBytes(), 0, null))
        );

        LogbrokerDataProcessor reader = mock(LogbrokerDataProcessor.class);
        doThrow(new RuntimeException()).when(reader).process(mb);
        ConsumerReadResponse readResponse = mock(ConsumerReadResponse.class);
        when(readResponse.getBatches()).thenReturn(Collections.singletonList(mb));

        var lbMonitorSpy = Mockito.spy(logbrokerMonitorExceptionsService);

        TransactionalLogbrokerListener logbrokerListener = new TransactionalLogbrokerListener(
                reader, transactionTemplate, lbMonitorSpy
        );

        assertThrows(
                RuntimeException.class,
                () -> logbrokerListener.onRead(readResponse, mock(StreamListener.ReadResponder.class))
        );

        verify(lbMonitorSpy)
                .addException(Mockito
                        .argThat(arg -> arg.getTopicName().equals(TEST_TOPIC_NAME)));
    }

    @Test
    void commitExceptionTest() {
        MessageBatch mb = new MessageBatch(TEST_TOPIC_NAME, 1, Collections.emptyList());
        LogbrokerDataProcessor reader = mock(LogbrokerDataProcessor.class);

        ConsumerReadResponse readResponse = mock(ConsumerReadResponse.class);
        when(readResponse.getBatches()).thenReturn(Collections.singletonList(mb));

        StreamListener.ReadResponder readResponder = mock(StreamListener.ReadResponder.class);
        doThrow(new RuntimeException()).when(readResponder).commit();

        var lbMonitorSpy = Mockito.spy(logbrokerMonitorExceptionsService);

        TransactionalLogbrokerListener logbrokerListener = new TransactionalLogbrokerListener(
                reader, transactionTemplate, lbMonitorSpy
        );

        assertThrows(RuntimeException.class, () -> logbrokerListener.onRead(readResponse, readResponder));

        verify(lbMonitorSpy)
                .addException(Mockito
                        .argThat(arg -> arg.getTopicName().equals(GENERAL_TOPIC_NAME)));
    }

    @Nonnull
    private static LogbrokerException getLbException() {
        var lbExMock = mock(LogbrokerException.class);

        when(lbExMock.getHost()).thenReturn(TEST_HOST);
        when(lbExMock.getTopicName()).thenReturn(TEST_TOPIC_NAME);
        when(lbExMock.getSeenAt()).thenReturn(SEEN_AT);

        return lbExMock;
    }
}
