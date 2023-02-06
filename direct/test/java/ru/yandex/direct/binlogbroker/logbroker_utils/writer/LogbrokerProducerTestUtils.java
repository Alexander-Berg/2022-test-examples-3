package ru.yandex.direct.binlogbroker.logbroker_utils.writer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;

import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;

import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LogbrokerProducerTestUtils {
    private LogbrokerProducerTestUtils() {
    }

    /**
     * LogbrokerAsyncProducer для имитации падения методов write() и init(). Считает количество вызовов write(), init
     * (), close()
     *
     * @param writeFailureCnt количество неудавшихся попыток записи
     * @param initFailureCnt  количество неудавшихся попыток инициализации
     * @param ex:             при вызове методов, пока счетчиках неудач > 0 возвращает {@link CompletableFuture},
     *                        завершенное с ex
     **/
    @SuppressWarnings("checkstyle:parameternumber")
    static Supplier<CompletableFuture<AsyncProducer>> createFailureProducerSupplier(
            AtomicLong writeCnt,
            AtomicLong initCnt, AtomicLong closeCnt, AtomicLong createCnt, int writeFailureCnt, int initFailureCnt,
            int createFailureCnt, RuntimeException ex
    ) {
        AtomicInteger writeFailureCntAtomic = new AtomicInteger(writeFailureCnt);
        AtomicInteger initFailureCntAtomic = new AtomicInteger(initFailureCnt);
        AsyncProducer asyncProducer = mock(AsyncProducer.class);

        mockInit(asyncProducer, initCnt, initFailureCntAtomic, ex);
        mockWrite(asyncProducer, writeCnt, writeFailureCntAtomic, ex, null);
        mockClose(asyncProducer, closeCnt);

        AtomicInteger createFailureCntAtomic = new AtomicInteger(createFailureCnt);
        return mockCreate(asyncProducer, createCnt, createFailureCntAtomic, ex);

    }

    @SuppressWarnings("checkstyle:parameternumber")
    static Supplier<CompletableFuture<AsyncProducer>> createSuccessfulProducerSupplier(
            List<byte[]> written
    ) {
        AtomicInteger writeFailureCntAtomic = new AtomicInteger(0);
        AtomicInteger initFailureCntAtomic = new AtomicInteger(0);
        AsyncProducer asyncProducer = mock(AsyncProducer.class);

        RuntimeException ex = new RuntimeException();

        mockInit(asyncProducer, new AtomicLong(0), initFailureCntAtomic, ex);
        mockWrite(asyncProducer, new AtomicLong(0), writeFailureCntAtomic, ex, Collections.synchronizedList(written));
        mockClose(asyncProducer, new AtomicLong(0));

        AtomicInteger createFailureCntAtomic = new AtomicInteger(0);
        return mockCreate(asyncProducer, new AtomicLong(0), createFailureCntAtomic, ex);
    }

    // CHECKSTYLE:OFF

    /**
     * Метод аналогичный
     * {@link #createFailureProducerSupplier(AtomicLong, AtomicLong, AtomicLong, AtomicLong, int, int, int, RuntimeException)}
     * но тут {@link AsyncProducer#closeFuture()} будет завершаться с ошибкой
     **/
    // CHECKSTYLE:ON
    @SuppressWarnings("checkstyle:parameternumber")
    static Supplier<CompletableFuture<AsyncProducer>> createFailureCloseProducerSupplier(
            AtomicLong writeCnt,
            AtomicLong initCnt,
            AtomicLong closeCnt,
            AtomicLong createCnt,
            int writeFailureCnt,
            int initFailureCnt,
            int createFailureCnt,
            RuntimeException ex
    ) {
        AtomicInteger writeFailureCntAtomic = new AtomicInteger(writeFailureCnt);
        AtomicInteger initFailureCntAtomic = new AtomicInteger(initFailureCnt);
        AsyncProducer asyncProducer = mock(AsyncProducer.class);

        mockInit(asyncProducer, initCnt, initFailureCntAtomic, ex);
        mockWrite(asyncProducer, writeCnt, writeFailureCntAtomic, ex, null);
        mockFailureClose(asyncProducer, closeCnt);

        AtomicInteger createFailureCntAtomic = new AtomicInteger(createFailureCnt);
        return mockCreate(asyncProducer, createCnt, createFailureCntAtomic, ex);
    }


    /**
     * LogbrokerAsyncProducer для просмотра seqNo, передающихся в метод write
     **/
    static Supplier<CompletableFuture<AsyncProducer>> createProducerSupplierWithCapturedSeqNo(
            long initialSeqNo,
            ArgumentCaptor<Long> seqNoCapture
    ) {
        return createProducerSupplierWithCapturedSeqNo(initialSeqNo, 0, seqNoCapture);
    }


    /**
     * LogbrokerAsyncProducer, неудочно записывающий первые writeFailureCnt объектов, сохраняющий seqNo, передавшиеся
     * в метод write
     *
     * @param writeFailureCnt количество неудачных вызовов метода {@link AsyncProducer#write(byte[], long)}
     **/
    static Supplier<CompletableFuture<AsyncProducer>> createProducerSupplierWithCapturedSeqNo(
            long initialSeqNo,
            int writeFailureCnt,
            ArgumentCaptor<Long> seqNoCapture
    ) {
        AsyncProducer asyncProducer = mock(AsyncProducer.class);
        AtomicInteger writeFailureCntAtomic = new AtomicInteger(writeFailureCnt);
        /* Mocked AsyncProducer.init() */
        ProducerInitResponse initResponse = createMockedProducerInitResponse(initialSeqNo);
        CompletableFuture<ProducerInitResponse> initResponseFuture = CompletableFuture.completedFuture(initResponse);
        when(asyncProducer.init()).thenReturn(initResponseFuture);

        /* Mocked AsyncProducer.closeFuture() */
        doNothing().when(asyncProducer).close();
        when(asyncProducer.closeFuture()).thenReturn(CompletableFuture.completedFuture(null));

        /* Mocked AsyncProducer.write() with capture seqNo and failing writeFailureCnt times*/

        ProducerWriteResponse producerWriteResponse = createMockedProducerWriteResponse();
        Supplier<CompletableFuture<ProducerWriteResponse>> writeResponseFutureSupplier =
                () -> CompletableFuture.supplyAsync(() -> {
                    if (writeFailureCntAtomic.decrementAndGet() < 0) {
                        return producerWriteResponse;
                    }
                    throw new RuntimeException("write exception");
                });

        doAnswer(invocation -> writeResponseFutureSupplier.get()).when(asyncProducer)
                .write(any(), seqNoCapture.capture(), anyLong());

        return () -> CompletableFuture.completedFuture(asyncProducer);
    }

    private static ProducerInitResponse createMockedProducerInitResponse(long initialSeqNo) {
        ProducerInitResponse initResponse = mock(ProducerInitResponse.class);
        when(initResponse.getMaxSeqNo()).thenReturn(initialSeqNo);
        return initResponse;
    }

    private static ProducerWriteResponse createMockedProducerWriteResponse() {
        ProducerWriteResponse writeResponse = mock(ProducerWriteResponse.class);
        when(writeResponse.isAlreadyWritten()).thenReturn(false);
        return writeResponse;
    }

    private static void mockInit(AsyncProducer asyncProducer, AtomicLong initCnt, AtomicInteger failureCnt,
                                 RuntimeException ex) {
        ProducerInitResponse initResponse = createMockedProducerInitResponse(0L);
        Supplier<CompletableFuture<ProducerInitResponse>> initResponseFutureSupplier =
                () -> CompletableFuture.supplyAsync(() -> {
                    initCnt.incrementAndGet();
                    if (failureCnt.decrementAndGet() < 0) {
                        return initResponse;
                    }
                    throw ex;
                });
        doAnswer(invocation -> initResponseFutureSupplier.get()).when(asyncProducer).init();
    }

    private static void mockWrite(AsyncProducer asyncProducer, AtomicLong writeCnt, AtomicInteger failureCnt,
                                  RuntimeException ex, List<byte[]> writtenObjects) {
        ProducerWriteResponse writeResponse = createMockedProducerWriteResponse();

        Function<InvocationOnMock, CompletableFuture<ProducerWriteResponse>> writeResponseFutureSupplier =
                (invocation) -> CompletableFuture.supplyAsync(() -> {
                    if (writtenObjects != null) {
                        writtenObjects.add(invocation.getArgument(0));
                    }

                    writeCnt.incrementAndGet();
                    if (failureCnt.decrementAndGet() < 0) {
                        return writeResponse;
                    }
                    throw ex;
                });

        doAnswer(writeResponseFutureSupplier::apply).when(asyncProducer).write(any(), anyLong(), anyLong());
    }

    private static Supplier<CompletableFuture<AsyncProducer>> mockCreate(AsyncProducer asyncProducer,
                                                                         AtomicLong createCnt,
                                                                         AtomicInteger failureCnt,
                                                                         RuntimeException ex) {
        return () -> CompletableFuture.supplyAsync(() -> {
            createCnt.incrementAndGet();
            if (failureCnt.decrementAndGet() < 0) {
                return asyncProducer;
            }
            throw ex;
        });
    }

    private static void mockClose(AsyncProducer asyncProducer, AtomicLong closeCnt) {
        doAnswer(invocation -> {
            closeCnt.incrementAndGet();
            return null;
        }).when(asyncProducer).close();
    }

    private static void mockFailureClose(AsyncProducer asyncProducer, AtomicLong closeCnt) {
        doAnswer(invocation -> {
            closeCnt.incrementAndGet();
            throw new RuntimeException("close exception");
        }).when(asyncProducer).close();
    }
}
