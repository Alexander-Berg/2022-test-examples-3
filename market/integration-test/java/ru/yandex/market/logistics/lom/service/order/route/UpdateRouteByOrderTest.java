package ru.yandex.market.logistics.lom.service.order.route;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute;
import ru.yandex.market.logistics.lom.service.order.OrderService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DatabaseSetup(
    value = "/service/order/route/before/prepare_orders_invalid_route.xml",
    type = DatabaseOperation.INSERT
)
@DisplayName("Тест обновления маршрута для заказа, упавшего при создании из комбинаторного маршрута")
class UpdateRouteByOrderTest extends AbstractOrderCombinedRouteHistoryTest {

    @Autowired
    private OrderService orderService;

    @Test
    @DisplayName("У заказа статус не VALIDATION_ERROR")
    void notErrorStatusUpdateTest() {
        CombinatorRoute route = new CombinatorRoute();

        softly.assertThatThrownBy(() -> orderService.updateRouteByOrder(1L, route))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(
                "Replace route is allowed only in state VALIDATION_ERROR and without created waybill segments"
            );
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("У заказа существуют waybill сегменты")
    void waybillSegmentExistUpdateTest() {
        CombinatorRoute route = new CombinatorRoute();

        softly.assertThatThrownBy(() -> orderService.updateRouteByOrder(4L, route))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(
                "Replace route is allowed only in state VALIDATION_ERROR and without created waybill segments"
            );
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @ExpectedDatabase(
        value = "/service/order/route/after/prepare_orders_invalid_route.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Не ловим исключения и нормально проходим в метод сохранения в базу нового маршрута")
    void successfullUpdateTest() {
        CombinatorRoute route = new CombinatorRoute();

        orderService.updateRouteByOrder(3L, route);

        verify(uuidGenerator).randomUuid();
        verify(ydbRepository).saveRoute(
            refEq(expectedRouteHistory(3L), "id"),
            refEq(objectMapper.valueToTree(route))
        );
        verifyUidsInYdb(List.of(MOCKED_UUID));
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CONVERT_ROUTE_TO_WAYBILL,
                PayloadFactory.createOrderIdAuthorPayload(
                    3L,
                    null,
                    null,
                    1L
                )
        );
    }

    @AfterEach
    @Test
    void checkNoMoreInteractions() {
        verifyNoMoreInteractions(uuidGenerator, ydbRepository);
    }
}
