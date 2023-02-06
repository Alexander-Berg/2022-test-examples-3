package ru.yandex.market.logistics.lom.service.order.route;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import yandex.market.combinator.common.Common;
import yandex.market.combinator.v0.CombinatorOuterClass;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute;
import ru.yandex.market.logistics.lom.model.enums.DeliveryType;
import ru.yandex.market.logistics.lom.repository.OrderRepository;
import ru.yandex.market.logistics.lom.service.order.combinator.CombinatorGrpcClient;
import ru.yandex.market.logistics.lom.service.order.combinator.CombinatorRouteService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;

@DisplayName("Получение нового маршрута из комбинатора")
public class GetNewDeliveryRouteByUuidTest extends AbstractOrderCombinedRouteHistoryTest {

    private static final Common.DeliveryType OLD_COMBINATOR_DELIVERY_TYPE = Common.DeliveryType.COURIER;
    private static final Common.DeliveryType NEW_COMBINATOR_DELIVERY_TYPE = Common.DeliveryType.PICKUP;
    private static final DeliveryType NEW_DELIVERY_TYPE = DeliveryType.PICKUP;
    private static final int DELIVERY_SERVICE_ID = 1003937;
    private static final int FIRST_POINT_LMS_ID = 100;

    @Autowired
    private CombinatorRouteService combinatorRouteService;

    @Autowired
    private CombinatorGrpcClient combinatorGrpcClient;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("Успешное получение маршрута")
    void testGetNewDeliveryRoute() {
        transactionTemplate.execute(transactionStatus -> {
            orderCombinedRouteHistoryService.saveRoute(orderRepository.getById(1L), combinedRoute());
            return null;
        });

        Mockito.when(combinatorGrpcClient.getDeliveryRouteFromPointRequest(any())).thenReturn(
            CombinatorOuterClass.DeliveryRoute
                .newBuilder()
                .setRoute(
                    CombinatorOuterClass.Route
                        .newBuilder()
                        .setDeliveryType(NEW_COMBINATOR_DELIVERY_TYPE)
                )
                .build()
        );

        CombinatorRoute newRoute = combinatorRouteService.getNewDeliveryRoute(
            MOCKED_UUID.toString(),
            new Order()
        );

        softly.assertThat(newRoute.getRoute().getDeliveryType()).isEqualTo(NEW_DELIVERY_TYPE);

        ArgumentCaptor<CombinatorOuterClass.DeliveryRouteFromPointRequest> argumentCaptor = ArgumentCaptor.forClass(
            CombinatorOuterClass.DeliveryRouteFromPointRequest.class
        );
        verify(combinatorGrpcClient).getDeliveryRouteFromPointRequest(argumentCaptor.capture());
        CombinatorOuterClass.DeliveryRouteFromPointRequest request = argumentCaptor.getValue();
        softly.assertThat(request.getDeliveryType()).isEqualTo(OLD_COMBINATOR_DELIVERY_TYPE);
        softly.assertThat(request.getDeliveryServiceId()).isEqualTo(DELIVERY_SERVICE_ID);
        softly.assertThat(request.getStartSegmentLmsId()).isEqualTo(FIRST_POINT_LMS_ID);
        verify(uuidGenerator).randomUuid();
        verify(ydbRepository).saveRoute(any(), refEq(combinedRoute()));
        verify(ydbRepository).getRouteByUuid(MOCKED_UUID);
    }

}
