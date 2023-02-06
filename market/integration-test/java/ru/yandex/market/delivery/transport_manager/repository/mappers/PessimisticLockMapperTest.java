package ru.yandex.market.delivery.transport_manager.repository.mappers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.EntityType;

class PessimisticLockMapperTest extends AbstractContextualTest {
    @Autowired
    private PessimisticLockMapper mapper;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_with_plan_inbound_register_units.xml")
    void lockById() {
        final AtomicBoolean tx1Completed = new AtomicBoolean(false);
        final ExecutorService executorService = Executors.newSingleThreadExecutor();

        try {
            transactionTemplate.execute(tx1 -> {
                doLock();

                // Запускаем вторую транзакцию в другом потоке
                executorService.submit(() -> {
                    transactionTemplate.execute(tx2 -> {
                        // Тут транзакция 1 не должна ещё закончиться (одной секунды сна родительского
                        // должно хватить на запуск дочернего почти всегда)
                        softly.assertThat(tx1Completed).isFalse();

                        doLock();

                        // Этот код выполнится только после того, как родительская транзакция закоммичена
                        softly.assertThat(tx1Completed).isTrue();
                        return null;
                    });
                });

                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                // Проставляем флаг, что родительская транзакция скоро закоммитится
                tx1Completed.set(true);
                return null;
            });
        } finally {
            executorService.shutdown();
        }
    }

    private void doLock() {
        mapper.lockById(EntityType.TRANSPORTATION_UNIT, 1);
    }
}
