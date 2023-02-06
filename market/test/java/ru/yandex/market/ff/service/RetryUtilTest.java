package ru.yandex.market.ff.service;

import java.util.concurrent.Semaphore;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.service.util.RetryUtil;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Функциональный тест для {@link RetryUtil}.
 *
 * @author avetokhin 29/05/2018.
 */
class RetryUtilTest extends IntegrationTest {

    private static final long REQUEST_ID = 1L;

    @Autowired
    private ShopRequestFetchingService shopRequestFetchingService;

    @Autowired
    private ShopRequestModificationService shopRequestModificationService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Semaphore semaphore = new Semaphore(1);

    private int triesCount;

    private Runnable retryRunnable = new Runnable() {
        @Override
        public void run() {
            // Выполняется в транзакции.
            RetryUtil.retryOptimisticLock(transactionTemplate, () -> {
                // Увеличить счетчик выполнения данной операции.
                triesCount++;

                // Получить заявку и поменять статус.
                final ShopRequest request = shopRequestFetchingService.getRequestOrThrow(REQUEST_ID);

                try {
                    // Взять блокировку и сохранить изменения.
                    System.out.println("Updating in retry block");
                    semaphore.acquire();
                    return shopRequestModificationService.updateStatus(request, RequestStatus.CANCELLED);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    semaphore.release();
                }
            });
        }
    };

    @BeforeEach
    void init() {
        triesCount = 0;
    }

    @Test
    @DatabaseSetup("classpath:service/retry-util/before.xml")
    @ExpectedDatabase(value = "classpath:service/retry-util/after.xml", assertionMode = NON_STRICT)
    void retryOptimisticLock() throws InterruptedException {
        // Взять блокировку в основном потоке.
        semaphore.acquire();

        // Запустить параллельный поток, в котором в ретрае будет обновляться статус заявки.
        final Thread thread = new Thread(retryRunnable);
        thread.start();

        // Выждать момент, чтобы параллельный поток успел начать транзакцию и в ней залочиться.
        Thread.sleep(500);

        // Обновить в основном потоке заявку, тем самым сломав выполнение транзакции параллельного потока.
        System.out.println("Updating in main block");
        final ShopRequest request = shopRequestFetchingService.getRequestOrThrow(REQUEST_ID);
        shopRequestModificationService.updateStatus(request, RequestStatus.ARRIVED_TO_SERVICE);

        // Отпустить блокировку, дав параллельному потоку возможность запуститься, упасть из-за оптимистичной блокировки
        // и перезапустить транзакцию заново.
        semaphore.release();

        // Подождать параллельный поток.
        thread.join();

        assertThat(triesCount, equalTo(2));
    }

}
