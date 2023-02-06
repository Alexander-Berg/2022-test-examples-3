package ru.yandex.market.logshatter.output;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.logbroker.pull.LogBrokerSourceKey;
import ru.yandex.market.logshatter.LogBatch;
import ru.yandex.market.logshatter.LogShatterService;
import ru.yandex.market.logshatter.config.ConfigValidationException;
import ru.yandex.market.logshatter.config.LogShatterConfig;
import ru.yandex.market.logshatter.logging.BatchErrorLoggerFactory;
import ru.yandex.market.logshatter.parser.LogParserProvider;
import ru.yandex.market.logshatter.reader.ReadSemaphore;
import ru.yandex.market.logshatter.reader.SourceContext;
import ru.yandex.market.logshatter.reader.logbroker.LogbrokerSourceContext;
import ru.yandex.market.logshatter.reader.logbroker2.TestParser;

import java.time.Duration;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 16.05.2019
 */
public class OutputQueueTest {
    @Rule
    public Timeout timeout = new Timeout(10, TimeUnit.SECONDS);

    // Тестовый вариант OutputQueue будет бросать NoSuchElementException если попытаться взять элемент из пустой очереди
    // В проде в такой ситуации будем ждать появления нового элемента
    private final OutputQueue outputQueue = new OutputQueue(oq -> {
        Condition condition = mock(Condition.class);
        try {
            doAnswer(invocation -> {
                if (oq.isEmpty()) {
                    throw new NoSuchElementException();
                }
                return null;
            })
                .when(condition).await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return condition;
    });

    @Test
    public void take_emptyQueue() {
        assertTakeLockFailsBecauseQueueIsEmpty();
    }

    @Test
    public void addAndRemoveTwice() throws Exception {
        SourceContext sourceContext = createEmptySourceContext();

        LogBatch logBatch1 = createLogBatch();
        sourceContext.getOutputQueue().add(logBatch1);
        outputQueue.add(sourceContext);
        popNextLogBatchAndAssertItIs(logBatch1);
        assertTakeLockFailsBecauseQueueIsEmpty();

        LogBatch logBatch2 = createLogBatch();
        sourceContext.getOutputQueue().add(logBatch2);
        outputQueue.add(sourceContext);
        popNextLogBatchAndAssertItIs(logBatch2);
        assertTakeLockFailsBecauseQueueIsEmpty();
    }

    @Test
    public void addTwoSourceContextForDifferentConfigsAndRemoveTwice() throws Exception {
        SourceContext sourceContext1 = createEmptySourceContext();
        SourceContext sourceContext2 = createEmptySourceContext();

        LogBatch logBatch1 = createLogBatch();
        sourceContext1.getOutputQueue().add(logBatch1);
        outputQueue.add(sourceContext1);

        LogBatch logBatch2 = createLogBatch();
        sourceContext2.getOutputQueue().add(logBatch2);
        outputQueue.add(sourceContext2);

        popNextLogBatchAndAssertItIs(logBatch1);
        popNextLogBatchAndAssertItIs(logBatch2);
        assertTakeLockFailsBecauseQueueIsEmpty();
    }

    @Test
    public void addTwoSourceContextForTheSameConfigAndRemoveTwice() throws Exception {
        LogShatterConfig logShatterConfig = createLogshatterConfig();
        SourceContext sourceContext1 = createEmptySourceContext(logShatterConfig);
        SourceContext sourceContext2 = createEmptySourceContext(logShatterConfig);

        LogBatch logBatch1 = createLogBatch();
        sourceContext1.getOutputQueue().add(logBatch1);
        outputQueue.add(sourceContext1);

        LogBatch logBatch2 = createLogBatch();
        sourceContext2.getOutputQueue().add(logBatch2);
        outputQueue.add(sourceContext2);

        popNextLogBatchAndAssertItIs(logBatch1);
        popNextLogBatchAndAssertItIs(logBatch2);
        assertTakeLockFailsBecauseQueueIsEmpty();
    }

    @Test
    public void addSourceContextWithTwoLogBatchesAndRemoveTwice() throws Exception {
        SourceContext sourceContext = createEmptySourceContext();

        LogBatch logBatch1 = createLogBatch();
        LogBatch logBatch2 = createLogBatch();
        sourceContext.getOutputQueue().add(logBatch1);
        sourceContext.getOutputQueue().add(logBatch2);
        outputQueue.add(sourceContext);

        popNextLogBatchAndAssertItIs(logBatch1);
        popNextLogBatchAndAssertItIs(logBatch2);
        assertTakeLockFailsBecauseQueueIsEmpty();
    }

    @Test
    public void addSourceContextWithTwoLogBatchesAndSourceContextWithOneLogBatchAndRemoveThreeTimes() throws Exception {
        SourceContext sourceContext1 = createEmptySourceContext();
        SourceContext sourceContext2 = createEmptySourceContext();

        LogBatch logBatch11 = createLogBatch();
        LogBatch logBatch12 = createLogBatch();
        sourceContext1.getOutputQueue().add(logBatch11);
        sourceContext1.getOutputQueue().add(logBatch12);
        outputQueue.add(sourceContext1);

        LogBatch logBatch2 = createLogBatch();
        sourceContext2.getOutputQueue().add(logBatch2);
        outputQueue.add(sourceContext2);

        // Порядок важен. Если взяли не все LogBatch'и из ConfigOutputQueue, то этот ConfigOutputQueue должен вернуться
        // в начало очереди, и нужно продолжить его писать. См. https://st.yandex-team.ru/MARKETINFRA-4688.
        popNextLogBatchAndAssertItIs(logBatch11, outputQueue::returnLockAndMaybeAddFirst);
        popNextLogBatchAndAssertItIs(logBatch12);
        popNextLogBatchAndAssertItIs(logBatch2);
        assertTakeLockFailsBecauseQueueIsEmpty();
    }

    @Test
    public void addOneSourceContextTwice() throws Exception {
        SourceContext sourceContext1 = createEmptySourceContext();
        SourceContext sourceContext2 = createEmptySourceContext();

        LogBatch logBatch11 = createLogBatch();
        sourceContext1.getOutputQueue().add(logBatch11);
        outputQueue.add(sourceContext1);

        LogBatch logBatch2 = createLogBatch();
        sourceContext2.getOutputQueue().add(logBatch2);
        outputQueue.add(sourceContext2);

        LogBatch logBatch12 = createLogBatch();
        sourceContext1.getOutputQueue().add(logBatch12);
        outputQueue.add(sourceContext1);

        // Порядок важен. Повторное добавление одного и того же SourceContext'а не должно двигать соответствующий
        // ConfigOutputQueue в очереди.
        popNextLogBatchAndAssertItIs(logBatch11, outputQueue::returnLockAndMaybeAddFirst);
        popNextLogBatchAndAssertItIs(logBatch12);
        popNextLogBatchAndAssertItIs(logBatch2);
        assertTakeLockFailsBecauseQueueIsEmpty();
    }

    @Test
    public void addOneSourceContextTwiceWithAddLast() throws Exception {
        SourceContext sourceContext1 = createEmptySourceContext();
        SourceContext sourceContext2 = createEmptySourceContext();

        LogBatch logBatch11 = createLogBatch();
        sourceContext1.getOutputQueue().add(logBatch11);
        outputQueue.add(sourceContext1);

        LogBatch logBatch2 = createLogBatch();
        sourceContext2.getOutputQueue().add(logBatch2);
        outputQueue.add(sourceContext2);

        LogBatch logBatch12 = createLogBatch();
        sourceContext1.getOutputQueue().add(logBatch12);
        outputQueue.add(sourceContext1);

        // Порядок важен. Повторное добавление одного и того же SourceContext'а не должно двигать соответствующий
        // ConfigOutputQueue в очереди.
        popNextLogBatchAndAssertItIs(logBatch11, outputQueue::returnLockAndMaybeAddLast);
        popNextLogBatchAndAssertItIs(logBatch2);
        popNextLogBatchAndAssertItIs(logBatch12);
        assertTakeLockFailsBecauseQueueIsEmpty();
    }

    @Test
    public void multipleLocks() throws Exception {
        // если один поток в процессе набора данных, то второму процессу нельзя выдавать
        SourceContext sourceContext1 = createEmptySourceContext();
        SourceContext sourceContext2 = createEmptySourceContext();

        LogBatch logBatch1 = createLogBatch();
        sourceContext1.getOutputQueue().add(logBatch1);
        outputQueue.add(sourceContext1);

        LogBatch logBatch2 = createLogBatch();
        sourceContext2.getOutputQueue().add(logBatch2);
        outputQueue.add(sourceContext2);

        // Одна и та же ConfigOutputQueue не может быть залочена двумя потоками.
        ConfigOutputQueue configOutputQueue1 = outputQueue.takeLock();
        ConfigOutputQueue configOutputQueue2 = outputQueue.takeLock();
        assertNotSame(configOutputQueue1, configOutputQueue2);
    }

    @Test
    public void addWhileLocked() throws Exception {
        SourceContext sourceContext1 = createEmptySourceContext();

        LogBatch logBatch1 = createLogBatch();
        sourceContext1.getOutputQueue().add(logBatch1);
        outputQueue.add(sourceContext1);

        // Начали сохранять в КХ
        outputQueue.takeLock();

        // Пока сохраняли в КХ, парсеры напарсили ещё один LogBatch
        LogBatch logBatch2 = createLogBatch();
        sourceContext1.getOutputQueue().add(logBatch2);
        outputQueue.add(sourceContext1);

        // Другой поток не должен захватить ту же самую ConfigOutputQueue
        assertTakeLockFailsBecauseQueueIsEmpty();
    }

    @Test
    public void addTwice() throws Exception {
        SourceContext sourceContext1 = createEmptySourceContext();

        LogBatch logBatch1 = createLogBatch();
        sourceContext1.getOutputQueue().add(logBatch1);

        outputQueue.add(sourceContext1);
        outputQueue.add(sourceContext1);

        popNextLogBatchAndAssertItIs(logBatch1);
        assertTakeLockFailsBecauseQueueIsEmpty();
    }

    @Test
    public void recheckTwice() throws Exception {
        SourceContext sourceContext1 = createEmptySourceContext();

        LogBatch logBatch1 = createLogBatch();
        sourceContext1.getOutputQueue().add(logBatch1);

        outputQueue.add(sourceContext1);
        ConfigOutputQueue configOutputQueue = outputQueue.takeLock();

        outputQueue.returnLockAndMaybeAddLast(configOutputQueue);
        outputQueue.returnLockAndMaybeAddLast(configOutputQueue);

        popNextLogBatchAndAssertItIs(logBatch1);
        assertTakeLockFailsBecauseQueueIsEmpty();
    }

    @Test
    public void toStringTest() throws Exception {
        assertEquals("{queueSize=0, processingSize=0}", outputQueue.toString());

        SourceContext sourceContext1 = createEmptySourceContext();
        SourceContext sourceContext2 = createEmptySourceContext();

        sourceContext1.getOutputQueue().add(createLogBatch());
        sourceContext1.getOutputQueue().add(createLogBatch());
        outputQueue.add(sourceContext1);

        sourceContext2.getOutputQueue().add(createLogBatch());
        outputQueue.add(sourceContext2);

        outputQueue.takeLock().takeLock();

        assertEquals("{queueSize=1, processingSize=1}", outputQueue.toString());
    }

    private LogBatch popNextLogBatchAndAssertItIs(LogBatch logBatch) throws InterruptedException {
        return popNextLogBatchAndAssertItIs(logBatch, outputQueue::returnLockAndMaybeAddLast);
    }

    /**
     * См. {@link LogShatterService.OutputWorker#outputOnce()}
     */
    private LogBatch popNextLogBatchAndAssertItIs(
        LogBatch logBatch,
        Consumer<ConfigOutputQueue> returnLock
    ) throws InterruptedException {
        // Просим очередь дать следующий конфиг, в котором есть что сохранять
        ConfigOutputQueue configOutputQueue = outputQueue.takeLock();

        // Просим конфиг дать следующий SourceContext
        SourceContext sourceContext = configOutputQueue.takeLock();

        // Забираем первый LogBatch из очереди SourceContext'а
        // Проверяем что этот тот LogBatch, который мы ожидали
        assertSame(logBatch, sourceContext.getOutputQueue().poll());

        // Разлочиваем sourceContext
        configOutputQueue.returnLock(sourceContext);

        // Возвращаем configOutputQueue обратно в очередь
        returnLock.accept(configOutputQueue);

        // "Сохраняем данные в Кликхаус" (в тесте ничего не делаем)

        // Возвращаем configOutputQueue обратно в очередь ещё раз на случай если добавилось больше данных пока
        // сохраняли в Кликхаус (так делает OutputWorker#outputOnce)
        returnLock.accept(configOutputQueue);

        return logBatch;
    }

    private void assertTakeLockFailsBecauseQueueIsEmpty() {
        assertThatThrownBy(outputQueue::takeLock).isInstanceOf(NoSuchElementException.class);
    }

    private static LogBatch createLogBatch() {
        return new LogBatch(
            Stream.of("firstLine", "secondLine"), 0, 0, 0,
            Duration.ofMillis(0), Collections.emptyList(), "sourceName"
        );
    }

    private static SourceContext createEmptySourceContext() throws ConfigValidationException {
        return createEmptySourceContext(createLogshatterConfig());
    }

    private static LogbrokerSourceContext createEmptySourceContext(LogShatterConfig logShatterConfig) {
        return new LogbrokerSourceContext(
            null,
            logShatterConfig,
            new LogBrokerSourceKey(null, null, null, null, null, null, null),
            mock(BatchErrorLoggerFactory.class),
            0,
            new ReadSemaphore().getEmptyQueuesCounter()
        );
    }

    private static LogShatterConfig createLogshatterConfig() throws ConfigValidationException {
        return LogShatterConfig.newBuilder()
            .setConfigFileName("/123")
            .setDataClickHouseTable(new ClickHouseTableDefinitionImpl("my.table", Collections.emptyList(), null))
            .setParserProvider(new LogParserProvider(TestParser.class.getName(), null, null))
            .build();
    }
}
