package ru.yandex.market.global.checkout.order;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.DataChangedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.order.OrderPaymentRepository;
import ru.yandex.market.global.checkout.domain.order.OrderRepository;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.common.test.TestUtil;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderPayment;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.global.common.test.TestUtil.doParallelCalls;

@Slf4j
public class OrderRepositoryTest extends BaseFunctionalTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderPaymentRepository orderPaymentRepository;

    @Autowired
    private TestOrderFactory testOrderFactory;

    @Test
    public void testLockIsLocking() {

        Order order = testOrderFactory.createOrder().getOrder();
        CyclicBarrier barrier = new CyclicBarrier(2);
        TestUtil.ParallelCallResults<Boolean> results = doParallelCalls(10,
                // Параллельно в 10 потоков создаем транзакции
                () -> orderRepository.ctx().transactionResult(configuration -> {
                    // Лочим заказ
                    orderRepository.lock(order.getId());
                    // И ждем пока кто нибудь еще его не залочит
                    barrier.await(200, TimeUnit.MILLISECONDS);
                    // Только если 2 потока пришли сюда за 200мс
                    return true;
                })
        );

        // Ни разу не было 2 одновременных лока
        Assertions.assertThat(results.getResults()).isEmpty();

        // Все исключения были обработаны в JOOQ и превратились в DataAccessException
        Assertions.assertThat(results.getErrors())
                .map((Function<Throwable, Class<?>>) Throwable::getClass)
                .containsOnly(DataAccessException.class);
    }

    @Test
    void testVersionIsWorking() {
        Order order1 = testOrderFactory.createOrder().getOrder();
        Order order2 = orderRepository.fetchOneById(order1.getId());

        orderRepository.update(order2);

        assertThatThrownBy(() -> orderRepository.update(order1))
                .isExactlyInstanceOf(DataChangedException.class);
    }

    @Test
    void testOrderPaymentVersionIsWorking() {
        OrderPayment orderPayment1 = testOrderFactory.createOrder().getOrderPayment();
        OrderPayment orderPayment2 = orderPaymentRepository.fetchOneById(orderPayment1.getId());

        orderPaymentRepository.update(orderPayment1);
        assertThatThrownBy(() -> orderPaymentRepository.update(orderPayment2))
                .isExactlyInstanceOf(DataChangedException.class);
    }
}
