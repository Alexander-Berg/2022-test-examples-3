package ru.yandex.market.logistics.lom.controller.order;

import java.time.Instant;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.dto.UpdateLastMileRequestDto;
import ru.yandex.market.logistics.lom.model.enums.DeliveryType;
import ru.yandex.market.logistics.lom.service.order.OrderService;
import ru.yandex.market.logistics.lom.utils.UpdateLastMileUtils;
import ru.yandex.market.logistics.lom.utils.UuidGenerator;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Изменение последней мили (смена типа доставки на PICKUP")
@DatabaseSetup("/controller/order/lastmile/before/setup.xml")
@ParametersAreNonnullByDefault
public class OrderLastMileUpdateToPickupTest extends AbstractContextualTest {

    private static final ChangeOrderRequestPayload PAYLOAD = PayloadFactory.createChangeOrderRequestPayload(
        1,
        "1",
        1
    );

    private static final Instant FIXED_TIME = Instant.parse("2021-03-01T00:00:00.00Z");
    private static final UUID NEW_ROUTE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID EXISTING_UUID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired
    private UuidGenerator uuidGenerator;

    @Autowired
    private OrderService orderService;

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @DisplayName("Успешный запрос на обновление последней мили (смена типа доставки на доставку в ПВЗ)")
    @Test
    @ExpectedDatabase(
        value = "/controller/order/lastmile/to_pickup/after/change_request_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateLastMileToPickupSuccess() throws Exception {
        doReturn(NEW_ROUTE_UUID).when(uuidGenerator).randomUuid();

        performRequest(
            UpdateLastMileUtils.validBuilder(UpdateLastMileUtils.validPayloadBuilder(), DeliveryType.PICKUP).build()
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/lastmile/to_pickup/after/response.json",
                "created",
                "updated"
            ));

        softly.assertThat(orderService.getOrderOrThrow(1L).getRouteUuid()).isEqualTo(EXISTING_UUID);
        queueTaskChecker.assertQueueTaskCreated(QueueType.PROCESS_UPDATE_LAST_MILE_TO_PICKUP, PAYLOAD);
    }

    @Test
    @DisplayName("Ошибка при попытке обновления последней мили на ПВЗ без маршрута")
    void testUpdateLastMileToPickupWithoutRoute() throws Exception {
        performRequest(
            UpdateLastMileUtils.validBuilder(
                    UpdateLastMileUtils.validPayloadBuilder(),
                    DeliveryType.PICKUP
                )
                .route(null)
                .build()
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Last mile type change with null route"));
    }

    @DisplayName("Повторный запрос на обновление последней мили (смена типа доставки на доставку в ПВЗ)")
    @Test
    @DatabaseSetup(
        value = "/controller/order/lastmile/to_pickup/after/change_request_created.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/order/lastmile/to_pickup/after/change_request_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateLastMileDuplicate() throws Exception {
        performRequest(
            UpdateLastMileUtils.validBuilder(UpdateLastMileUtils.validPayloadBuilder(), DeliveryType.PICKUP).build()
        )
            .andExpect(status().isConflict())
            .andExpect(errorMessage(
                "Active Change Request with type = CHANGE_LAST_MILE_TO_PICKUP is already exists for order 1001"
            ));
    }

    @Nonnull
    private ResultActions performRequest(UpdateLastMileRequestDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/updateLastMile", request));
    }
}
