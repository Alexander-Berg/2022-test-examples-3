package ru.yandex.market.logistics.nesu.api.order;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.services.auth.blackbox.BlackboxService;
import ru.yandex.market.logistics.lom.model.enums.tags.OrderTag;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.api.auth.ApiAuthHolder;
import ru.yandex.market.logistics.nesu.api.model.order.SenderOrderDraft;
import ru.yandex.market.logistics.nesu.base.order.AbstractCreateOrderTest;
import ru.yandex.market.logistics.nesu.dto.order.OrderDraft;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createLocation;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createWithdrawBuilder;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.defaultOrderDraft;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.defaultShipmentDtoBuilder;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.orderDraftShipment;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createLogisticsPointResponseBuilder;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createLogisticsPointsFilter;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Создание черновика заказа в Open API")
class ApiCreateOrderTest extends AbstractCreateOrderTest {

    @Autowired
    private BlackboxService blackboxService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MbiApiClient mbiApiClient;

    private ApiAuthHolder authHolder;

    @BeforeEach
    void setupAuth() {
        authHolder = new ApiAuthHolder(blackboxService, objectMapper);
        authHolder.mockAccess(mbiApiClient, 1L);
    }

    @Test
    @DisplayName("Недоступный магазин")
    void noShopAccess() throws Exception {
        authHolder.mockNoAccess(mbiApiClient, 1L);

        createOrder(defaultOrderDraft())
            .andExpect(status().isNotFound())
            .andExpect(content().json("{\"message\":\"Failed to find [SENDER] with ids [1]\","
                + "\"resourceType\":\"SENDER\",\"identifiers\":[1]}"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Создание черновика с заменой склада")
    @DatabaseSetup(
        value = "/repository/delivery-options/api_warehouse_substitution.xml",
        type = DatabaseOperation.INSERT
    )
    void createOrderDraftWithSubstitution(
        @SuppressWarnings("unused") String name,
        Long oldWarehouseId,
        Long newWarehouseId
    ) throws Exception {
        LogisticsPointResponse senderWarehouse = createLogisticsPointResponseBuilder(
            newWarehouseId,
            null,
            "warehouse1",
            PointType.WAREHOUSE
        )
            .businessId(41L)
            .build();

        doReturn(List.of(senderWarehouse, WAREHOUSE_TO, PICKUP_POINT))
            .when(lmsClient).getLogisticsPoints(createLogisticsPointsFilter(Set.of(newWarehouseId, 4L, 101L), true));
        doReturn(Optional.of(senderWarehouse)).when(lmsClient).getLogisticsPoint(newWarehouseId);

        createOrder(
            defaultOrderDraft().andThen(o -> o.setShipment(orderDraftShipment().setWarehouseFrom(oldWarehouseId)))
        )
            .andExpect(status().isOk())
            .andExpect(content().json("1"));

        verifyLomOrderCreate(createLomOrderRequest().setWaybill(List.of(
            createWithdrawBuilder(
                defaultShipmentDtoBuilder().locationFrom(createLocation(newWarehouseId)).locationTo(null).build()
            )
                .build()
        )));
        verify(lmsClient).getLogisticsPoints(createLogisticsPointsFilter(Set.of(newWarehouseId, 4L, 101L), true));
        verify(lmsClient).getLogisticsPoint(newWarehouseId);
    }

    @Nonnull
    private static Stream<Arguments> createOrderDraftWithSubstitution() {
        return Stream.of(
            Arguments.of("Склад был заменен", 1L, 500L),
            Arguments.of("Нет замены для склада, есть по businessId", 2L, 2L),
            Arguments.of("Нет замены по businessId, есть по складу", 6L, 6L),
            Arguments.of("Нет замены по businessId, и складу", 7L, 7L)
        );
    }

    @Override
    protected String orderDraftObjectName() {
        return "senderOrderDraft";
    }

    @Nonnull
    @Override
    protected ResultActions createOrder(Consumer<OrderDraft> orderDraftAdjuster, Long senderId) throws Exception {
        SenderOrderDraft senderOrderDraft = new SenderOrderDraft().setSenderId(senderId);
        orderDraftAdjuster.accept(senderOrderDraft);

        return mockMvc.perform(request(HttpMethod.POST, "/api/orders", senderOrderDraft)
            .headers(authHolder.authHeaders()));
    }

    @Nonnull
    @Override
    protected ResultActions createOrder(String fileName, Long senderId) throws Exception {
        return mockMvc.perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    String.format("controller/order/create/request/api/%s.json", fileName)
                ))
                .headers(authHolder.authHeaders())
        );
    }

    @Nonnull
    @Override
    protected ResultActions createOrder(Consumer<OrderDraft> orderDraftAdjuster, Long senderId, Long shopId)
        throws Exception {
        authHolder.mockAccess(mbiApiClient, shopId);
        return createOrder(orderDraftAdjuster, senderId);
    }

    @Nonnull
    @Override
    protected OrderTag getTag() {
        return OrderTag.CREATED_VIA_DAAS_OPEN_API;
    }
}
