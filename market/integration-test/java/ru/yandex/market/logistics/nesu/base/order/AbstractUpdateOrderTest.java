package ru.yandex.market.logistics.nesu.base.order;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.altay.unifier.HttpUnifierClient;
import ru.yandex.altay.unifier.UnificationRequest;
import ru.yandex.market.logistics.delivery.calculator.client.DeliveryCalculatorSearchEngineClient;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillOrderRequestDto;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Direction;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.lom.model.search.Sort;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils;
import ru.yandex.market.logistics.nesu.dto.MultiplaceItem;
import ru.yandex.market.logistics.nesu.dto.order.OrderDraft;
import ru.yandex.market.logistics.nesu.dto.order.OrderDraftDeliveryOption;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.utils.SenderAvailableDeliveriesUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.defaultDeliverySearchRequestBuilder;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.getDefaultAddressBuilder;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.getSimpleUnifierReplyWithAddress;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.mockCourierSchedule;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.mockDeliveryOptionValidation;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createDefaultLomItem;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createItem;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createPlace;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createRootUnit;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.defaultDeliveryOption;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.defaultDeliveryOptionServices;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.ownDeliveryFilter;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createLogisticsPointResponse;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createLogisticsPointResponseBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractUpdateOrderTest extends AbstractContextualTest {

    protected static final long ORDER_ID = 42L;
    private static final Instant INSTANT = Instant.parse("2019-02-02T12:00:00.00Z");

    protected static final LogisticsPointResponse WAREHOUSE_TO =
        createLogisticsPointResponse(4L, 5L, "warehouse2", PointType.WAREHOUSE);
    protected static final LogisticsPointResponse PICKUP_POINT =
        createLogisticsPointResponse(101L, 5L, "pick", PointType.PICKUP_POINT);

    @Autowired
    protected LMSClient lmsClient;
    @Autowired
    protected LomClient lomClient;
    @Autowired
    private DeliveryCalculatorSearchEngineClient deliveryCalculatorSearchEngineClient;
    @Autowired
    private FeatureProperties featureProperties;
    @Captor
    private ArgumentCaptor<WaybillOrderRequestDto> captor;
    @Autowired
    protected HttpUnifierClient unifierClient;

    @BeforeEach
    void setup() {
        doReturn(List.of(warehouseFrom(3L), WAREHOUSE_TO, PICKUP_POINT)).when(lmsClient)
            .getLogisticsPoints(refEq(LmsFactory.createLogisticsPointsFilter(Set.of(3L, 4L, 101L), true)));

        mockGetPartnerByIds(Set.of(5L, 53916L, 45L));

        when(lmsClient.getPartner(5L)).thenReturn(Optional.of(LmsFactory.createPartner(5L, null)));

        when(lmsClient.getScheduleDay(1L)).thenReturn(Optional.of(LmsFactory.createScheduleDayDto(1)));

        mockCourierSchedule(lmsClient, 42, Set.of(5L));
        clock.setFixed(INSTANT, ZoneId.systemDefault());

        when(featureProperties.getShopIdsForExtendedNeeds()).thenReturn(List.of());

        when(unifierClient.unify(any(UnificationRequest.class)))
            .thenReturn(getSimpleUnifierReplyWithAddress(getDefaultAddressBuilder().build()));
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(unifierClient);
    }

    @Test
    @DisplayName("Кейс с отсутствующим идентификатором заказа")
    void unknownOrder() throws Exception {
        updateOrder(OrderDtoFactory.defaultOrderDraft(), -1L)
            .andExpect(status().isNotFound())
            .andExpect(content().json("{\"message\":\"Failed to find [ORDER] with ids [-1]\","
                + "\"resourceType\":\"ORDER\",\"identifiers\":[-1]}"));
    }

    @Test
    @DisplayName("Редактирование заказа другого сендера")
    void anotherSender() throws Exception {
        when(lomClient.getOrder(ORDER_ID, Set.of()))
            .thenReturn(Optional.of(new OrderDto().setId(ORDER_ID).setPlatformClientId(3L).setSenderId(2L)));

        updateOrder(OrderDtoFactory.defaultOrderDraft(), ORDER_ID)
            .andExpect(status().isNotFound())
            .andExpect(content().json("{\"message\":\"Failed to find [SENDER] with ids [2]\","
                + "\"resourceType\":\"SENDER\",\"identifiers\":[2]}"));
    }

    @Test
    @DisplayName("Редактирование заказа при разных идентификаторах платформ")
    void anotherPlatformSender() throws Exception {
        when(lomClient.getOrder(ORDER_ID, Set.of()))
            .thenReturn(Optional.of(new OrderDto().setId(ORDER_ID).setPlatformClientId(1L)));

        updateOrder(OrderDtoFactory.defaultOrderDraft(), ORDER_ID)
            .andExpect(status().isNotFound())
            .andExpect(content().json("{\"message\":\"Failed to find [ORDER] with ids [42]\","
                + "\"resourceType\":\"ORDER\",\"identifiers\":[42]}"));
    }

    @Test
    @DisplayName("Успешное редактирование заказа")
    void success() throws Exception {
        mockSuccess();
        mockAvailablePartners();

        updateOrder(OrderDtoFactory.defaultOrderDraft(), ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(content().string("42"));

        verifyUnifierClient();
    }

    @Test
    @DisplayName("Успешное редактирование заказа с многоместкой - смена externalId грузоместа")
    void successPlaceExternalId() throws Exception {
        mockSuccess();
        mockAvailablePartners();
        WaybillOrderRequestDto requestDto = OrderDtoFactory.createLomOrderRequest();
        requestDto.setItems(List.of(createDefaultLomItem("ext_item_id", Set.of("new-ext-id"), null)))
            .setUnits(List.of(
                OrderDtoFactory.createPlaceUnitBuilder().externalId("new-ext-id").build(),
                createRootUnit()
            ));

        updateOrder(
            OrderDtoFactory.defaultOrderDraft().andThen(o ->
                o.setPlaces(List.of(createPlace(
                        45,
                        30,
                        15,
                        50,
                        List.of(createItem())
                    )
                        .setExternalId("new-ext-id")))
                    .setItems(List.of(createItem().setPlaceExternalIds(List.of("new-ext-id"))))
            ),
            ORDER_ID
        )
            .andExpect(status().isOk())
            .andExpect(content().string("42"));
        WaybillOrderRequestDto value = captor.getValue();
        assertThatModelEquals(requestDto, value);

        verifyUnifierClient();
    }

    @Test
    @DisplayName("Успешное редактирование заказа с многоместкой - добавление товара")
    void successAddItem() throws Exception {
        mockSuccess();
        mockAvailablePartners();
        mockDeliveryOptionValidation(
            5,
            deliveryCalculatorSearchEngineClient,
            lmsClient,
            defaultDeliverySearchRequestBuilder()
                .offerPrice(40000L)
                .build()
        );
        WaybillOrderRequestDto requestDto = OrderDtoFactory.createLomOrderRequest();
        requestDto.setItems(List.of(
                createDefaultLomItem("ext_item_id", Set.of("ext_place_id"), null),
                createDefaultLomItem("new-item-id", null, null)
            ))
            .setUnits(List.of(
                OrderDtoFactory.createPlaceUnitBuilder().externalId("ext_place_id").build(),
                createRootUnit()
            ));

        updateOrder(
            OrderDtoFactory.defaultOrderDraft().andThen(o -> {
                    MultiplaceItem multiplaceItem = createItem();
                    multiplaceItem.setExternalId("new-item-id");
                    o.setPlaces(List.of(createPlace(
                            45,
                            30,
                            15,
                            50,
                            List.of(
                                createItem(),
                                createItem().setExternalId("new-item-id")
                            )
                        )))
                        .setItems(List.of(
                            createItem().setPlaceExternalIds(List.of("ext_place_id")),
                            multiplaceItem
                        ));
                    o.setDeliveryOption((OrderDraftDeliveryOption) defaultDeliveryOption()
                        .setServices(defaultDeliveryOptionServices("6.800", "0.75")));
                }
            ),
            ORDER_ID
        )
            .andExpect(status().isOk())
            .andExpect(content().string("42"));
        WaybillOrderRequestDto value = captor.getValue();
        assertThatModelEquals(requestDto.getItems(), value.getItems());

        verifyUnifierClient();
    }

    @Test
    @DisplayName("Невалидный черновик заказа для YaGo")
    void invalidOrderDraftForYaGo() throws Exception {
        when(lomClient.getOrder(42L, Set.of()))
            .thenReturn(Optional.of(new OrderDto().setId(42L).setPlatformClientId(3L).setSenderId(1L)));
        when(featureProperties.getShopIdsForExtendedNeeds()).thenReturn(List.of(1L));

        updateOrder(OrderDtoFactory.defaultExtendedInvalidOrderDraft(), 42L)
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/order/validation/extended_order_validation.json"));
    }

    @Test
    @DisplayName("Невалидный черновик заказа не для YaGo")
    void invalidOrderDraftNotForYaGo() throws Exception {
        mockSuccess();

        updateOrder(OrderDtoFactory.defaultExtendedInvalidOrderDraft(), 42L).andExpect(status().isOk());

        verifyUnifierClient();
    }

    protected void mockSuccess() {
        when(lmsClient.searchPartners(LmsFactory.createPartnerFilter(
            Set.of(5L),
            null,
            Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING)
        )))
            .thenReturn(List.of(LmsFactory.createPartner(5L, PartnerType.DELIVERY)));
        doReturn(Optional.of(
            createLogisticsPointResponse(5L, 6L, "Sorting center warehouse", PointType.WAREHOUSE)
        ))
            .when(lmsClient).getLogisticsPoint(5L);
        doReturn(Optional.of(
            createLogisticsPointResponse(3L, 41L, null, "warehouse1", PointType.WAREHOUSE)
        ))
            .when(lmsClient).getLogisticsPoint(3L);

        when(lomClient.searchShipments(any(), any()))
            .thenReturn(PageResult.empty(new Pageable(0, 1, new Sort(Direction.ASC, "id"))));

        when(lomClient.getOrder(ORDER_ID, Set.of()))
            .thenReturn(Optional.of(new OrderDto().setId(ORDER_ID).setPlatformClientId(3L).setSenderId(1L)));

        when(lomClient.updateOrderDraft(eq(ORDER_ID), captor.capture()))
            .thenAnswer(i -> {
                Long orderId = i.getArgument(0);
                WaybillOrderRequestDto draft = i.getArgument(1);
                return new OrderDto()
                    .setId(orderId)
                    .setPlatformClientId(draft.getPlatformClientId())
                    .setSenderId(draft.getSenderId());
            });
        mockDeliveryOptionValidation(5, deliveryCalculatorSearchEngineClient, lmsClient);
    }

    protected void mockAvailablePartners() {
        SenderAvailableDeliveriesUtils.mockGetSenderAvailableDeliveries(
            lmsClient,
            LmsFactory.createPartner(6L, PartnerType.SORTING_CENTER),
            List.of(
                LmsFactory.createPartner(5L, PartnerType.DELIVERY),
                LmsFactory.createPartner(42L, PartnerType.DELIVERY),
                LmsFactory.createPartner(53916L, PartnerType.DELIVERY)
            ),
            List.of(
                createLogisticsPointResponse(5L, 203L, 6L, "Sorting center warehouse", PointType.WAREHOUSE),
                createLogisticsPointResponse(50L, 201L, 5L, "warehouse50", PointType.WAREHOUSE),
                createLogisticsPointResponse(420L, 201L, 42L, "warehouse420", PointType.WAREHOUSE)
            )
        );

        doReturn(List.of(TestOwnDeliveryUtils.partnerBuilder().build()))
            .when(lmsClient).searchPartners(ownDeliveryFilter().setMarketIds(Set.of(201L)).build());
    }

    @Nonnull
    protected ResultActions updateOrder(Consumer<OrderDraft> orderAdjuster, Long orderId) throws Exception {
        OrderDraft orderDraft = new OrderDraft();
        orderAdjuster.accept(orderDraft);
        return updateOrder(orderDraft, orderId);
    }

    @Nonnull
    protected abstract ResultActions updateOrder(OrderDraft orderDraft, Long orderId) throws Exception;

    @Nonnull
    protected static LogisticsPointResponse warehouseFrom(Long pointId) {
        return createLogisticsPointResponseBuilder(pointId, null, "warehouse1", PointType.WAREHOUSE)
            .handlingTime(Duration.ofDays(2))
            .businessId(41L)
            .build();
    }

    private void mockGetPartnerByIds(Set<Long> ids) {
        when(lmsClient.searchPartners(refEq(LmsFactory.createPartnerFilter(ids, null))))
            .thenReturn(
                ids.stream()
                    .map(id -> LmsFactory.createPartner(id, PartnerType.DELIVERY))
                    .collect(Collectors.toList())
            );
    }

    protected void verifyUnifierClient() {
        verify(unifierClient).unify(
            new UnificationRequest()
                .setAddress(Optional.of(
                    "Россия, " +
                        "Республика Мордовия, " +
                        "Городской округ Саранск, " +
                        "Саранск, " +
                        "recipient_street, " +
                        "recipient_house, " +
                        "recipient_housing, " +
                        "recipient_building, " +
                        "recipient_room"
                ))
        );
    }
}
