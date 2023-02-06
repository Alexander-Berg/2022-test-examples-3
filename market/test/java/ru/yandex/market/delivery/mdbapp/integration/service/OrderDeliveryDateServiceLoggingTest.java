package ru.yandex.market.delivery.mdbapp.integration.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.delivery.mdbapp.components.logging.json.GetOrdersDeliveryDateErrorLogger;
import ru.yandex.market.delivery.mdbapp.components.logging.json.GetOrdersDeliveryDateResultLogger;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Order;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.OrderRequest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.OrderRequestStatus;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.RequestCondition;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.godd.OrderRequestRepository;
import ru.yandex.market.delivery.mdbclient.model.delivery.OrderDeliveryDate;
import ru.yandex.market.delivery.mdbclient.model.delivery.ResourceId;
import ru.yandex.market.delivery.mdbclient.model.request.GetOrdersDeliveryDateError;
import ru.yandex.market.delivery.mdbclient.model.request.GetOrdersDeliveryDateResult;

public class OrderDeliveryDateServiceLoggingTest {
    public static final ZoneOffset ZONE_OFFSET = ZonedDateTime.now().getOffset();

    public static final ResourceId RESOURCE_ID_1 = new ResourceId("1", "aaa");
    public static final ResourceId RESOURCE_ID_2 = new ResourceId("2", "bbb");

    public static final OrderDeliveryDate DD1 = new OrderDeliveryDate(
        RESOURCE_ID_1,
        OffsetDateTime.of(2020, 9, 29, 0, 0, 0, 0, ZONE_OFFSET),
        OffsetTime.of(10, 0, 0, 0, ZONE_OFFSET),
        OffsetTime.of(22, 0, 0, 0, ZONE_OFFSET),
        "dd1"
    );
    public static final OrderDeliveryDate DD2 = new OrderDeliveryDate(
        RESOURCE_ID_2,
        OffsetDateTime.of(2020, 9, 29, 0, 0, 0, 0, ZONE_OFFSET),
        OffsetTime.of(10, 0, 0, 0, ZONE_OFFSET),
        OffsetTime.of(22, 0, 0, 0, ZONE_OFFSET),
        "dd2"
    );
    public static final String PROCESS_ID = "process1";
    public static final long ORDER_ID_1 = 1L;
    public static final long ORDER_ID_2 = 2L;
    public static final long ORDER_ID_3 = 3L;
    public static final Instant NOW = Instant.ofEpochMilli(0);

    private final OrderRequest orderRequest1 = createOrderRequest(10L, ORDER_ID_1, 1L);
    private final OrderRequest orderRequest2 = createOrderRequest(20L, ORDER_ID_1, 2L);
    private final OrderRequest orderRequest3 = createOrderRequest(30L, ORDER_ID_2, 3L);
    private final OrderRequest orderRequest4 = createOrderRequest(40L, ORDER_ID_3, 4L);

    private OrderDeliveryDateService orderDeliveryDateService;
    private OrderRequestRepository orderRequestRepository;
    private GetOrdersDeliveryDateResultLogger resultLogger;
    private GetOrdersDeliveryDateErrorLogger errorLogger;

    @Before
    public void setUp() {
        orderRequestRepository = Mockito.mock(OrderRequestRepository.class);
        resultLogger = Mockito.mock(GetOrdersDeliveryDateResultLogger.class);
        errorLogger = Mockito.mock(GetOrdersDeliveryDateErrorLogger.class);

        orderDeliveryDateService = Mockito.spy(new OrderDeliveryDateService(
            null,
            orderRequestRepository,
            null,
            resultLogger,
            errorLogger,
            new OrderDeliveryDateProcessIdProvider(() -> "")
        ));

        Mockito.doNothing().when(orderDeliveryDateService)
            .recalculateRequestsAndCorrectDeliveryDates(Mockito.any(), Mockito.any());
    }

    @After
    public void tearDown() {
        Mockito.verifyNoMoreInteractions(orderRequestRepository, resultLogger, errorLogger);
    }

    @Test
    public void processSuccessResponse() {
        Mockito.when(orderRequestRepository.findByProcessId(PROCESS_ID)).
            thenReturn(List.of(
                orderRequest1,
                orderRequest2,
                orderRequest3,
                orderRequest4
            ));

        GetOrdersDeliveryDateResult result = new GetOrdersDeliveryDateResult(
            PROCESS_ID,
            1234L,
            List.of(DD1, DD2)
        );
        orderDeliveryDateService.processSuccessResponse(result, NOW);

        Mockito.verify(orderRequestRepository).findByProcessId(PROCESS_ID);

        Mockito.verify(resultLogger).logDeliveryDateResult(
            Mockito.eq(result),
            Mockito.eq(Map.of(
                ORDER_ID_1, List.of(orderRequest1, orderRequest2),
                ORDER_ID_2, List.of(orderRequest3)
            ))
        );

        Mockito.verify(orderRequestRepository).setStatus(Mockito.eq(List.of(
            orderRequest1.getId(),
            orderRequest2.getId(),
            orderRequest3.getId()
        )), Mockito.eq(OrderRequestStatus.RECEIVED));

        Mockito.verify(orderDeliveryDateService)
            .recalculateRequestsAndCorrectDeliveryDates(result.getOrderDeliveryDates(), NOW);
    }

    @Test
    public void processErrorResponse() {
        Mockito.when(orderRequestRepository.findByProcessId(PROCESS_ID)).
            thenReturn(List.of(
                orderRequest1,
                orderRequest2,
                orderRequest3,
                orderRequest4
            ));

        GetOrdersDeliveryDateError error = new GetOrdersDeliveryDateError(
            PROCESS_ID,
            1234L,
            List.of(RESOURCE_ID_1, RESOURCE_ID_2),
            "error"
        );
        orderDeliveryDateService.processErrorResponse(error);

        Mockito.verify(orderRequestRepository).findByProcessId(PROCESS_ID);

        Mockito.verify(errorLogger).logDeliveryDateError(
            Mockito.eq(error),
            Mockito.eq(Map.of(
                ORDER_ID_1, List.of(orderRequest1, orderRequest2),
                ORDER_ID_2, List.of(orderRequest3)
            ))
        );

        Mockito.verify(orderRequestRepository).setStatus(Mockito.eq(List.of(
            orderRequest1.getId(),
            orderRequest2.getId(),
            orderRequest3.getId()
        )), Mockito.eq(OrderRequestStatus.FAIL));
    }

    private OrderRequest createOrderRequest(long id, long orderId, long conditionId) {
        return new OrderRequest()
            .setId(id)
            .setOrder(new Order().setId(orderId))
            .setRequestCondition(new RequestCondition().setId(conditionId));
    }

}
