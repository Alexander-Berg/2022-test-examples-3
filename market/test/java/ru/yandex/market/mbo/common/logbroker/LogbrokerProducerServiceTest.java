package ru.yandex.market.mbo.common.logbroker;

import com.google.common.collect.Streams;
import com.google.common.hash.Hashing;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.kikimr.persqueue.producer.async.AsyncProducerConfig;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static ru.yandex.market.mbo.common.logbroker.LogbrokerEvent.Status.ERROR;
import static ru.yandex.market.mbo.common.logbroker.LogbrokerEvent.Status.SENT;

/**
 * Проверяем общий функционал писателя в Логброкер.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class LogbrokerProducerServiceTest {

    // Топик - это "корзина" внутри Логброкера, в которую будет складываться всё, что мы пишем. Из неё же
    // наши потребители будут считывать данные.
    private static final String TOPIC = "MBOC to RND SERVICE";

    // Source ID - уникальный идентификатор писателя в топик. В один топик могут писать несколько источников.
    private static final String SOURCE_ID = "Mboc-data-provider";

    // Порядковый номер записываемого в ЛБ события для данного топика и Source ID. Итого, три данных поля образуют
    // первичный ключ (topic, seqNo, sourceId). Подробнее: https://wiki.yandex-team.ru/logbroker/docs/concepts/.
    private static final long START_SEQ_NO = 100500L;

    private LogbrokerProducerService logbrokerProducerService;

    // Фабрика, в которой хранится главным образом хост и порт сервера с Логброкером. Нужна для создания писателя в ЛБ.
    private LogbrokerClientFactoryMock logbrokerClientFactory;
    // Сам писатель в ЛБ.
    private AsyncProducerMock asyncProducer;
    // Конфиг писателя с топиком, ИД источника, настройками секьюрити и метаданными.
    private AsyncProducerConfig asyncProducerConfig;

    private Function<String, byte[]> converter = String::getBytes;

    @Before
    public void setup() {
        asyncProducer = new AsyncProducerMock();
        logbrokerClientFactory = new LogbrokerClientFactoryMock(asyncProducer);
        asyncProducerConfig = AsyncProducerConfig.builder(TOPIC, SOURCE_ID.getBytes(StandardCharsets.UTF_8)).build();
        asyncProducer.mockInitResponse(START_SEQ_NO, TOPIC);
        logbrokerProducerService = new LogbrokerProducerServiceImpl(logbrokerClientFactory, asyncProducerConfig);
    }

    /**
     * Проверим, что при успешной отправке в ЛБ событиям проставляется тройной ключ (topic, seqNo, sourceId).
     */
    @Test
    public void testLogbrokerPropertiesAssignedToSentEvents() {
        // Вся работа с нашим ЛБ происходит через контекст. В нём мы указываем, что будем отправлять, какими батчами
        // и как будем обрабатывать каждый успешный или неуспешный батч.
        AtomicInteger successes = new AtomicInteger(0);
        LogbrokerContext<String> context = new LogbrokerContext<String>()
            .setEvents(events(100), 50, converter)
            .setOnSuccessBatchConsumer(batch -> {
                assertEquals(50, batch.size());
                batch.forEach(event -> {
                    assertTrue(event.getSeqNo() > START_SEQ_NO);
                    assertEquals(TOPIC, event.getTopic());
                    assertEquals(SOURCE_ID, event.getSourceId());
                    assertEquals(SENT, event.getStatus());
                });
                successes.incrementAndGet();
            })
            .setOnFailureBatchConsumer(batch -> fail("There shouldn't be any failed batches."));

        logbrokerProducerService.uploadEvents(context);
        assertEquals(2, successes.get());
    }

    @Test
    public void testNoEventsDoesNothing() {
        LogbrokerContext<String> context = new LogbrokerContext<String>()
            .setOnSuccessBatchConsumer(batch -> fail("Should not process anything."))
            .setOnFailureBatchConsumer(batch -> fail("Should not process anything."));
        List<LogbrokerEvent<String>> processedEvents = logbrokerProducerService.uploadEvents(context);
        assertTrue(processedEvents.isEmpty());
    }

    @Test
    public void testNoPostProcessorsOk() {
        // Если побатчевые результаты записи неинтересны, можно не указывать обработчики успеха/неуспеха. Результат
        // всё равно вернётся в виде проапдейченного списка эвентов, которые мы изначально подали на вход.
        LogbrokerContext<String> context = new LogbrokerContext<String>()
            .setEvents(events(100), 25, converter);
        List<LogbrokerEvent<String>> processedEvents = logbrokerProducerService.uploadEvents(context);
        assertEquals(100, processedEvents.size());
        Streams.forEachPair(context.getEvents().stream(), processedEvents.stream(), (original, processed) -> {
            assertEquals(original.getEvent(), processed.getEvent());
        });
        processedEvents.forEach(event -> {
            assertTrue(event.getSeqNo() > START_SEQ_NO);
            assertEquals(SENT, event.getStatus());
        });
    }

    @Test
    public void testFailedWriteDoesntFailWholeBatch() {
        AtomicInteger successfulEvents = new AtomicInteger(0);
        AtomicInteger failedEvents = new AtomicInteger(0);
        // Нарочно сделаем так, что в первом и третьем из четырёх батчей зафейлятся по одной записи.
        ProducerWriteResponse failResponse1 = new ProducerWriteResponse(START_SEQ_NO + 12, 0, true);
        ProducerWriteResponse failResponse3 = new ProducerWriteResponse(START_SEQ_NO + 56, 0, true);
        asyncProducer.mockWriteResponse(failResponse1);
        asyncProducer.mockWriteResponse(failResponse3);

        // Ожидается, что все неупавшие записи попадут в успешный коллбэк, а двое несчастных - в фейловый обработчик.
        LogbrokerContext<String> context = new LogbrokerContext<String>()
            .setEvents(events(100), 25, converter)
            .setOnSuccessBatchConsumer(batch -> {
                batch.forEach(event -> {
                    assertTrue(
                        event.getSeqNo() > START_SEQ_NO &&
                        event.getSeqNo() != START_SEQ_NO + 12 &&
                        event.getSeqNo() != START_SEQ_NO + 56);
                    assertEquals(SENT, event.getStatus());
                    successfulEvents.incrementAndGet();
                });
            })
            .setOnFailureBatchConsumer(batch -> {
                batch.forEach(event -> {
                    assertTrue(
                        event.getSeqNo() == START_SEQ_NO + 12 ||
                        event.getSeqNo() == START_SEQ_NO + 56);
                    assertEquals(ERROR, event.getStatus());
                    failedEvents.incrementAndGet();
                });
            });

        logbrokerProducerService.uploadEvents(context);
        assertEquals(98, successfulEvents.get());
        assertEquals(2, failedEvents.get());
    }

    @Test
    public void testExceptionInTheMiddleOfTheBatchHandledGracefully() {
        // Если в ходе записи в ЛБ произошло исключение, то обработка батча прекращается. При этом он весь
        // считается упавшим. В общем случае при работе с ЛБ мы гарантируем запись только при успешном ответе, в
        // остальных же случаях может как записаться, так и не записаться. Так что если что-то записалось реально, но
        // упало "визуально", то это норма (с) и в целом соответствует контракту ЛБ.
        AtomicInteger successfulEvents = new AtomicInteger(0);
        AtomicInteger failedEvents = new AtomicInteger(0);
        asyncProducer.mockFailWithExceptionOnSeqNo(START_SEQ_NO + 36);

        LogbrokerContext<String> context = new LogbrokerContext<String>()
            .setEvents(events(100), 50, converter)
            .setOnSuccessBatchConsumer(batch -> {
                batch.forEach(event -> {
                    // Т.к. в случае тотального фейла не успевают проставиться seqNo, номерация успешных продолжится
                    // с последнего сгенерированного номера. В нашем случае после падения на 36-ом, новый батч
                    // начнётся с 37-го. Однако это не значит, что упало только 36 штук. Упали все 50 в первом батче,
                    // просто 14 из них даже не успели получить свой номер.
                    assertTrue(event.getSeqNo() > START_SEQ_NO + 36);
                    assertEquals(SENT, event.getStatus());
                    successfulEvents.incrementAndGet();
                });
            })
            .setOnFailureBatchConsumer(batch -> {
                batch.forEach(event -> {
                    assertTrue(event.getSeqNo() == null || event.getSeqNo() <= START_SEQ_NO + 36);
                    assertEquals(ERROR, event.getStatus());
                    failedEvents.incrementAndGet();
                });
            });

        logbrokerProducerService.uploadEvents(context);
        assertEquals(50, successfulEvents.get());
        assertEquals(50, failedEvents.get());
    }

    private List<LogbrokerEvent<String>> events(int n) {
        List<LogbrokerEvent<String>> events = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String anyStr = Hashing.sha512().hashInt(i).toString();
            events.add(new LogbrokerEvent<String>().setEvent(anyStr));
        }
        return events;
    }

    private boolean within(long val, long leftExclusive, long rightInclusive) {
        return val > leftExclusive && val <= rightInclusive;
    }
}
