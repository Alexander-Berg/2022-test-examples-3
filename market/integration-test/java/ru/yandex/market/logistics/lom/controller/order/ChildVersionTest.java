package ru.yandex.market.logistics.lom.controller.order;

import java.util.function.Consumer;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.CancellationOrderRequest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.enums.CancellationOrderStatus;
import ru.yandex.market.logistics.lom.repository.CancellationOrderRequestRepository;
import ru.yandex.market.logistics.lom.repository.OrderRepository;

@DisplayName("Проверка изменения версии заказа при изменении child-объектов")
@DatabaseSetup("/controller/order/before/order_with_versioned_children.xml")
public class ChildVersionTest extends AbstractContextualTest {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CancellationOrderRequestRepository cancellationOrderRequestRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("Обновление сегмента waybill изменяет версию заказа")
    @ExpectedDatabase(
        value = "/controller/order/after/order_new_version.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void updateWaybillSegmentTest() {
        updateChild(order -> order.getWaybill().get(0).setExternalId("new_ext_id"));
    }

    @Test
    @DisplayName("Обновление заявки на отмену изменяет версию заказа")
    @ExpectedDatabase(
        value = "/controller/order/after/order_new_version.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void updateCancellationRequestTest() {
        updateChild(
            order -> order.getCancellationRequests().iterator().next().setStatus(CancellationOrderStatus.PROCESSING)
        );
    }

    @Test
    @DisplayName("Обновление заявки на отмену изменяет версию заказа напрямую, через репозиторий")
    @ExpectedDatabase(
        value = "/controller/order/after/order_new_version.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void cancellationOrderRequestRepositoryUpdate() {
        CancellationOrderRequest request = cancellationOrderRequestRepository.findById(1L).orElseThrow();
        request.setStatus(CancellationOrderStatus.PROCESSING);
        cancellationOrderRequestRepository.save(request);
    }

    private void updateChild(Consumer<Order> updated) {
        transactionTemplate.execute(status -> {
            Order order = orderRepository.findById(1L).orElseThrow();
            updated.accept(order);
            orderRepository.save(order);
            return null;
        });
    }
}
