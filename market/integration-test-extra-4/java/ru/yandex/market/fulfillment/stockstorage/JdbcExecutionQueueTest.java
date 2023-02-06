package ru.yandex.market.fulfillment.stockstorage;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.fulfillment.stockstorage.domain.entity.ExecutionQueueItem;
import ru.yandex.market.fulfillment.stockstorage.repository.JdbcExecutionQueue;
import ru.yandex.market.fulfillment.stockstorage.service.execution.ExecutionQueueType;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.WarehouseAwareExecutionQueuePayload;
import ru.yandex.market.request.trace.RequestContextHolder;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.fulfillment.stockstorage.service.execution.ExecutionQueueType.FULL_SYNC_STOCK;

public class JdbcExecutionQueueTest extends AbstractContextualTest {

    private static final LocalDateTime DEFAULT_DATE = LocalDateTime.of(2018, 1, 1, 0, 0);

    @Autowired
    private JdbcExecutionQueue executionQueue;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * Сценарий #1:
     * <p>
     * Проверяем, что при записи в пустую БД были корректно переданы значения полей сущности ExecutionQueueItem.
     */
    @Test
    @ExpectedDatabase(value = "classpath:database/expected/execution_queue/1.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushSingleRow() {
        transactionTemplate.execute(t -> {
            executionQueue.push(Collections.singleton(ExecutionQueueItem.of(
                    DEFAULT_DATE,
                    DEFAULT_DATE.plusSeconds(1),
                    FULL_SYNC_STOCK,
                    new WarehouseAwareExecutionQueuePayload(0L, 500L, 600, false, 100)
            )));

            return null;
        });
    }

    /**
     * Сценарий #2:
     * <p>
     * Проверяем, что при попытке записать в БД запись,
     * чей дубликат с т.з. связки (type, uuid) уже существует.
     * Вставки не произойдет и будет оставлена первоначальная запись.
     * Исполнение должно завершиться без ошибки.
     */
    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/2.xml")
    @ExpectedDatabase(value = "classpath:database/states/execution_queue/2.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushSingleExistingRow() {
        transactionTemplate.execute(t -> {
            executionQueue.push(Collections.singleton(ExecutionQueueItem.of(
                    LocalDateTime.of(2018, 1, 2, 0, 0),
                    DEFAULT_DATE,
                    FULL_SYNC_STOCK,
                    new WarehouseAwareExecutionQueuePayload(0L, 500L, 500, false, 1)
            )));

            return null;
        });
    }

    /**
     * Сценарий #3:
     * <p>
     * Проверяем, что при попытке забрать из БД одну запись с указанным типом
     * - она будет успешно получена и все ее поля будут корректно заполнены.
     * <p>
     * По окончанию транзакции запуленная запись должна быть удалена из БД.
     */
    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/3.xml")
    @ExpectedDatabase(value = "classpath:database/expected/execution_queue/3.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pullExistingRow() {
        Optional<ExecutionQueueItem<WarehouseAwareExecutionQueuePayload>> optionalItem =
                transactionTemplate.execute(t ->
                        executionQueue.pull(FULL_SYNC_STOCK, null, null)
                );

        SoftAssertions.assertSoftly(assertions -> {
                    assertions.assertThat(optionalItem)
                            .as("Asserting queue item existence")
                            .isPresent();

                    optionalItem.ifPresent(item -> {
                        assertions.assertThat(item.getId())
                                .as("Asserting id value")
                                .isEqualTo(100L);

                        assertions.assertThat(item.getType())
                                .as("Asserting queue_item type")
                                .isEqualTo(FULL_SYNC_STOCK);

                        assertions.assertThat(item.getCreated())
                                .as("Asserting created date time")
                                .isEqualTo(LocalDateTime.of(2018, 1, 1, 0, 0));

                        assertions.assertThat(item.getExecuteAfter())
                                .as("Asserting execute after")
                                .isEqualTo(LocalDateTime.of(2018, 1, 1, 0, 0, 1));

                        assertions.assertThat(item.getPayload())
                                .isEqualTo(new WarehouseAwareExecutionQueuePayload(0L, 500L, 500, false, 20));
                    });
                }
        );
    }


    /**
     * Сценарий #4:
     * <p>
     * Проверяем, что при попытке получить из БД сообщение с типом, записи с которым отсутствуют в БД -
     * будет возвращен Optional.empty();
     */
    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/4.xml")
    @ExpectedDatabase(value = "classpath:database/states/execution_queue/4.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pullMissingRow() {
        Optional<ExecutionQueueItem<WarehouseAwareExecutionQueuePayload>> optionalItem =
                transactionTemplate.execute(t ->
                        executionQueue.pull(FULL_SYNC_STOCK, null, null)
                );

        assertThat(optionalItem)
                .as("Assert that queue item is missing")
                .isEmpty();
    }

    /**
     * Сценарий #5:
     * <p>
     * Проверяем, что при вставке нескольких строк в БД одновременно все записи будут успешно вставлены.
     */
    @Test
    @ExpectedDatabase(value = "classpath:database/expected/execution_queue/5.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushMultipleRows() {
        pushMultipleRows(executionQueue::push);
    }

    @Test
    @ExpectedDatabase(value = "classpath:database/expected/execution_queue/5.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushUncheckedMultipleRows() {
        pushMultipleRows(executionQueue::pushUnchecked);
    }

    private void pushMultipleRows(Consumer<Collection<ExecutionQueueItem<?>>> consumer) {
        transactionTemplate.execute(t -> {
            consumer.accept(Arrays.asList(
                    ExecutionQueueItem.of(DEFAULT_DATE, DEFAULT_DATE.plusSeconds(1),
                            FULL_SYNC_STOCK,
                            new WarehouseAwareExecutionQueuePayload(0L, 500L, 500, false, 100)),
                    ExecutionQueueItem.of(DEFAULT_DATE, DEFAULT_DATE.plusSeconds(1),
                            FULL_SYNC_STOCK,
                            new WarehouseAwareExecutionQueuePayload(500L, 1000L, 500, false, 100)),
                    ExecutionQueueItem.of(DEFAULT_DATE, DEFAULT_DATE.plusSeconds(1),
                            FULL_SYNC_STOCK,
                            new WarehouseAwareExecutionQueuePayload(1000L, 1500L, 500, true, 100))
            ));

            return null;
        });
    }

    /**
     * Сценарий #6:
     * <p>
     * Проверяем, что при попытке получить N доступных в БД строк - они все будут получены успешно.
     */
    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/6.xml")
    public void pullMultipleRows() {
        Collection<ExecutionQueueItem<WarehouseAwareExecutionQueuePayload>> items = transactionTemplate.execute(t ->
                executionQueue.pull(FULL_SYNC_STOCK, 3, null, null)
        );

        assertThat(items)
                .as("Asserting items count")
                .hasSize(3);
    }

    /**
     * Сценарий #7:
     * <p>
     * Проверяем, что при попытке получить N строк с типом, которые отсутствуют в БД вернется пустой список.
     */
    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/7.xml")
    public void pullMultipleRowsWithMissingType() {
        Collection<ExecutionQueueItem<WarehouseAwareExecutionQueuePayload>> items = transactionTemplate.execute(t ->
                executionQueue.pull(FULL_SYNC_STOCK, 100, null, null)
        );

        assertThat(items)
                .as("Asserting items are empty")
                .isEmpty();
    }

    /**
     * Сценарий #8:
     * <p>
     * Проверяем, что строка,
     * которая уже обрабатывается в одной транзакции не видна для другой параллельной транзакции.
     */
    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/8.xml")
    public void pullSingleLockedItem() {
        assertLockingMechanism(
                () -> executionQueue.pull(FULL_SYNC_STOCK, null, null),

                firstResult -> assertThat(firstResult)
                        .as("Assert that we succeeded to lock rows from first transaction")
                        .isPresent(),

                secondResult -> assertThat(secondResult)
                        .as("Asserting that we failed to lock the same rows from second transaction")
                        .isEmpty()
        );
    }

    /**
     * Сценарий #9:
     * <p>
     * Проверяем, что строки,
     * которые уже обрабатывается в одной транзакции не видны для другой параллельной транзакции.
     */
    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/9.xml")
    public void pullMultipleLockedItem() {
        assertLockingMechanism(
                () -> executionQueue.pull(FULL_SYNC_STOCK, 2, null, null),

                firstResult -> assertThat(firstResult)
                        .as("Assert that we succeeded to lock rows from first transaction")
                        .hasSize(2),

                secondResult -> assertThat(secondResult)
                        .as("Asserting that we failed to lock the same rows from second transaction")
                        .isEmpty()
        );
    }


    /**
     * Сценарий #10:
     * <p>
     * Проверяем, что те строки, у которых executeAfter позже текущего времени не будут получены в результаты выборки.
     */
    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/10.xml")
    public void pullItemWithLateExecuteAfter() {
        transactionTemplate.execute(t -> {
            Collection<ExecutionQueueItem<WarehouseAwareExecutionQueuePayload>> items =
                    executionQueue.pull(ExecutionQueueType.FULL_SYNC_STOCK, 2, null, null);

            SoftAssertions.assertSoftly(assertions -> {
                assertions.assertThat(items)
                        .as("Assert that returned collection only have single item")
                        .hasSize(1);

                assertions.assertThat(items.iterator().next().getId())
                        .as("Asserting returned value id")
                        .isEqualTo(200L);
            });
            return null;
        });
    }


    /**
     * Сценарий #11:
     * <p>
     * Проверяем, что при попытке забрать из БД одну запись с указанным типом
     * - она будет успешно получена и все ее поля будут корректно заполнены.
     * <p>
     * По окончанию транзакции запуленная запись должна быть удалена из БД.
     */
    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/11.xml")
    @ExpectedDatabase(value = "classpath:database/expected/execution_queue/11.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pull() {
        Optional<ExecutionQueueItem<WarehouseAwareExecutionQueuePayload>> optionalItem =
                transactionTemplate.execute(t ->
                        executionQueue.pull(FULL_SYNC_STOCK, null, null)
                );

        SoftAssertions.assertSoftly(assertions -> assertions.assertThat(optionalItem).isEmpty()
        );
    }


    /**
     * Сценарий #12:
     * <p>
     * Проверяем, что при попытке получить N доступных в БД строк при это одна из них с некорректным payload
     * - будут получены только корректные.
     */
    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/12.xml")
    public void pullMultipleRowButOneRowIsCorrupted() {
        Collection<ExecutionQueueItem<WarehouseAwareExecutionQueuePayload>> items = transactionTemplate.execute(t ->
                executionQueue.pull(FULL_SYNC_STOCK, 3, null, null)
        );

        assertThat(items)
                .as("Asserting items count")
                .hasSize(2);
    }

    /**
     * Сценарий #13:
     * <p>
     * Проверяем, что при попытке добавить запись в БД, в то время, как существующая запись с такими же uuid и типом
     * обрабатывается,
     * новая запись добавлена не будет и транзакция не будет блокирована.
     */
    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/2.xml")
    @ExpectedDatabase(value = "classpath:database/states/execution_queue/empty.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void concurrentPullAndPushEqual() throws InterruptedException {
        withRequiresNewTransactionPropagation(() -> transactionTemplate.execute(t1 -> {
            executionQueue.pull(FULL_SYNC_STOCK, null, null);
            transactionTemplate.execute(t2 -> {
                executionQueue.push(Collections.singleton(ExecutionQueueItem.of(
                        LocalDateTime.of(2018, 1, 2, 0, 0),
                        DEFAULT_DATE,
                        FULL_SYNC_STOCK,
                        new WarehouseAwareExecutionQueuePayload(0L, 500L, 500, false, 1))));
                return null;
            });
            return null;
        }));
    }

    /**
     * Сценарий #14:
     * <p>
     * Проверяем, что при попытке добавить запись в БД, в то время, как другая запись обрабатывается,
     * новая запись будет добавленаб существующая успешно обработана.
     */
    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/2.xml")
    @ExpectedDatabase(value = "classpath:database/states/execution_queue/13.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void concurrentPullAndPushDifferent() throws InterruptedException {
        withRequiresNewTransactionPropagation(() -> transactionTemplate.execute(t1 -> {
            executionQueue.pull(FULL_SYNC_STOCK, null, null);
            transactionTemplate.execute(t2 -> {
                executionQueue.push(Collections.singleton(ExecutionQueueItem.of(
                        LocalDateTime.of(2018, 1, 2, 0, 0),
                        DEFAULT_DATE.plusSeconds(2),
                        FULL_SYNC_STOCK,
                        new WarehouseAwareExecutionQueuePayload(0L, 500L, 500, true, 2))));
                return null;
            });
            return null;
        }));
    }

    /**
     * Сценарий #15:
     * <p>
     * Проверяем, что при попытке добавить запись в БД, в то время, как существующая запись с такими же uuid и типом
     * обрабатывается
     * и упадет с ошибкой, новая запись добавлена не будет, транзакция не будет блокирована, сфейленная транзакция
     * откатится.
     */
    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/2.xml")
    @ExpectedDatabase(value = "classpath:database/states/execution_queue/2.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void concurrentPullAndPushException() throws InterruptedException {
        Assertions.assertThrows(RuntimeException.class, () ->
                withRequiresNewTransactionPropagation(() -> transactionTemplate.execute(t1 -> {
                    executionQueue.pull(FULL_SYNC_STOCK, null, null);
                    transactionTemplate.execute(t2 -> {
                        executionQueue.push(Collections.singleton(ExecutionQueueItem.of(
                                LocalDateTime.of(2018, 1, 2, 0, 0),
                                DEFAULT_DATE,
                                FULL_SYNC_STOCK,
                                new WarehouseAwareExecutionQueuePayload(0L, 500L, 500, false, 1))));
                        return null;
                    });
                    throw new RuntimeException();
                })));
    }


    /**
     * Сценарий #16
     * Проверяем, что при pushBack в execution_queue будет взято максимальное из значение
     * execute_after между:
     * - Присланным из приложения
     * - Макс. execute_after для данного типа очереди
     * <p>
     * В данном сценарии будет взят 2 вариант.
     */
    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/16.xml")
    @ExpectedDatabase(value = "classpath:database/expected/execution_queue/16.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void retryWithMaxExecuteAfterFromDatabase() {
        RequestContextHolder.createContext("0/0");
        transactionTemplate.execute(t -> {
            executionQueue.retry(ExecutionQueueItem.of(
                    LocalDateTime.of(1970, 1, 1, 0, 0),
                    LocalDateTime.of(1970, 1, 1, 0, 0),
                    ExecutionQueueType.KOROBYTES_SYNC,
                    new WarehouseAwareExecutionQueuePayload(500L, 1000L, 500, true, 1)
            ), "error");

            return null;
        });
    }

    /**
     * Сценарий #17
     * Проверяем, что при pushBack в execution_queue будет взято максимальное из значение
     * execute_after между:
     * - Присланным из приложения
     * - Макс. execute_after для данного типа очереди
     * <p>
     * В данном сценарии будет взят 1 вариант.
     */
    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/17.xml")
    @ExpectedDatabase(value = "classpath:database/expected/execution_queue/17.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void retryWithMaxExecuteAfterFromApplication() {
        RequestContextHolder.createContext("0/0");
        transactionTemplate.execute(t -> {
            executionQueue.retry(ExecutionQueueItem.of(
                    LocalDateTime.of(1970, 1, 1, 0, 0),
                    LocalDateTime.of(2070, 1, 1, 0, 0),
                    ExecutionQueueType.KOROBYTES_SYNC,
                    new WarehouseAwareExecutionQueuePayload(500L, 1000L, 500, true, 1)
            ), "error");

            return null;
        });
    }

    /**
     * Сценарий #18:
     * <p>
     * Проверяем, что при попытке получить N доступных в БД строк при будут получены только те,
     * у которых количество попыток не больше 15
     * <p>
     * В данном сценарии будет получена одна задача.
     */
    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/26.xml")
    public void pullMultipleRowWithAttemptCountLessFifteens() {
        Collection<ExecutionQueueItem<WarehouseAwareExecutionQueuePayload>> items = transactionTemplate.execute(t ->
                executionQueue.pull(FULL_SYNC_STOCK, 3, null, null)
        );

        assertThat(items)
                .as("Asserting items count")
                .hasSize(1);
    }

    /**
     * Сценарий #19:
     * <p>
     * Проверяем, что при попытке получить одну доступную в БД запись с указанным типом будет получена та,
     * у которой количество попыток не больше 10 (задано в SystemProperty)
     * <p>
     * В данном сценарии будет получена одна задача.
     */
    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/27.xml")
    public void pullOneRowWithAttemptCountLessTen() {
        Optional<ExecutionQueueItem<WarehouseAwareExecutionQueuePayload>> optionalItem =
                transactionTemplate.execute(t ->
                        executionQueue.pull(FULL_SYNC_STOCK, null, null)
                );

        SoftAssertions.assertSoftly(assertions -> {
                    assertions.assertThat(optionalItem)
                            .as("Asserting queue item existence")
                            .isPresent();

                    optionalItem.ifPresent(item -> {
                        assertions.assertThat(item.getId())
                                .as("Asserting id value")
                                .isEqualTo(200L);

                        assertions.assertThat(item.getAttemptNumber())
                                .as("Asserting attempt number")
                                .isEqualTo(10);
                    });
                }
        );
    }

    private void withRequiresNewTransactionPropagation(Runnable runnable) {
        int initialBehaviour = transactionTemplate.getPropagationBehavior();
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            runnable.run();
        } finally {
            transactionTemplate.setPropagationBehavior(initialBehaviour);
        }
    }

    private <R> void assertLockingMechanism(Supplier<R> repositoryCall,
                                            Consumer<R> firstAssertion,
                                            Consumer<R> secondAssertion) {
        withRequiresNewTransactionPropagation(() -> transactionTemplate.execute(t1 -> {
            R firstTransactionResult = repositoryCall.get();
            firstAssertion.accept(firstTransactionResult);

            transactionTemplate.execute(t2 -> {
                R secondTransactionResult = repositoryCall.get();

                secondAssertion.accept(secondTransactionResult);

                return null;
            });

            return null;
        }));
    }
}
