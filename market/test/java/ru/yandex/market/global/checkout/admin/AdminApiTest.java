package ru.yandex.market.global.checkout.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.api.AdminApiService;
import ru.yandex.market.global.checkout.api.OrderApiService;
import ru.yandex.market.global.checkout.api.UserApiService;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.domain.shop.ShopQueryService;
import ru.yandex.market.global.checkout.factory.TestCartFactory;
import ru.yandex.market.global.checkout.factory.TestCartFactory.CreateOrderCartDtoBuilder;
import ru.yandex.market.global.checkout.factory.TestElasticOfferFactory;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.factory.TestShopFactory;
import ru.yandex.market.global.common.elastic.dictionary.DictionaryQueryService;
import ru.yandex.market.global.common.jooq.Point;
import ru.yandex.market.global.common.test.TestClock;
import ru.yandex.market.global.db.jooq.enums.EDeliveryOrderState;
import ru.yandex.market.global.db.jooq.enums.EOrderState;
import ru.yandex.market.global.db.jooq.enums.EPaymentOrderState;
import ru.yandex.market.global.db.jooq.enums.EProcessingMode;
import ru.yandex.market.global.db.jooq.enums.EShopOrderState;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;
import ru.yandex.mj.generated.server.model.AdminOrderDto;
import ru.yandex.mj.generated.server.model.AdminOrdersDto;
import ru.yandex.mj.generated.server.model.CartItemDto;
import ru.yandex.mj.generated.server.model.DeliveryState;
import ru.yandex.mj.generated.server.model.GeoPointDto;
import ru.yandex.mj.generated.server.model.OfferDto;
import ru.yandex.mj.generated.server.model.OrderCartDto;
import ru.yandex.mj.generated.server.model.OrderDto;
import ru.yandex.mj.generated.server.model.OrderEvent;
import ru.yandex.mj.generated.server.model.OrderEventDto;
import ru.yandex.mj.generated.server.model.OrderPatchDto;
import ru.yandex.mj.generated.server.model.OrderState;
import ru.yandex.mj.generated.server.model.PaymentState;
import ru.yandex.mj.generated.server.model.ProcessingMode;
import ru.yandex.mj.generated.server.model.PromoApplicationType;
import ru.yandex.mj.generated.server.model.PromoWithUsagesLeftDto;
import ru.yandex.mj.generated.server.model.PromocodeDto;
import ru.yandex.mj.generated.server.model.ShopState;
import ru.yandex.passport.tvmauth.CheckedUserTicket;
import ru.yandex.passport.tvmauth.TicketStatus;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;
import static ru.yandex.market.starter.tvm.filters.UserTicketFilter.CHECKED_USER_TICKET_ATTRIBUTE;

@SuppressWarnings("ConstantConditions")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AdminApiTest extends BaseFunctionalTest {
    private static final long SHOP_ID = 40;
    private static final long BUSINESS_ID = 41;
    private static final String OFFER_ID = "OFFER_ID";
    private static final long UID = 10L;
    private static final String SOME_USER_TICKET = "some_user_ticket";

    private static final String CLAIM_ID = "claimId123456";

    private final TestClock clock;
    private final AdminApiService adminApiService;
    private final UserApiService userApiService;
    private final OrderApiService orderApiService;
    private final ShopQueryService shopQueryService;
    private final DictionaryQueryService<OfferDto> offersDictionary;
    private final TestOrderFactory orderFactory;
    private final TestCartFactory testCartFactory;
    private final TestShopFactory testShopFactory;
    private final TestElasticOfferFactory testElasticOfferFactory;

    private final TestOrderFactory testOrderFactory;

    @BeforeEach
    public void setup() {
        Mockito.when(shopQueryService.get(Mockito.anyLong()))
                .thenReturn(testShopFactory.buildShopDto()
                        .businessId(BUSINESS_ID)
                        .id(SHOP_ID)
                );

        Mockito.when(offersDictionary.get(Mockito.anyList()))
                .thenReturn(testElasticOfferFactory.buildOne());
    }

    @Test
    public void testCreatePromocode() {
        PromocodeDto promocodeDto = adminApiService.apiV1AdminPromocodeCreatePost(UID, 100_00L)
                .getBody();

        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));
        List<PromoWithUsagesLeftDto> promos = userApiService.apiV1UserAvailablePromosGet(List.of(), SOME_USER_TICKET,
                null)
                .getBody();

        Assertions.assertThat(promos)
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .build()
                )
                .containsExactly(new PromoWithUsagesLeftDto()
                        .applicationType(PromoApplicationType.PROMOCODE)
                        .name(promocodeDto.getValue())
                        .usagesLeft(1L)
                        .description("Discount")
                );
    }

    @Test
    public void testCreatedPromocodeGiveDiscount() {
        PromocodeDto promocodeDto = adminApiService.apiV1AdminPromocodeCreatePost(UID, 46L)
                .getBody();

        OrderCartDto testCart = testCartFactory.createOrderCartDto(CreateOrderCartDtoBuilder.builder()
                .setupCart(c -> c
                        .businessId(BUSINESS_ID)
                        .shopId(SHOP_ID)
                        .promocodes(List.of(promocodeDto.getValue()))
                        .items(List.of(new CartItemDto()
                                        .shopId(SHOP_ID)
                                        .businessId(BUSINESS_ID)
                                        .offerId(OFFER_ID)
                                        .count(2L)
                                )
                        )
                ).build()
        );

        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));
        OrderDto orderDto = orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, null, null, testCart
        ).getBody();

        Assertions.assertThat(orderDto.getTotalItemsCost())
                .isEqualTo(orderDto.getTotalItemsCostWithPromo() + 46L);
    }

    @Test
    public void testAllFieldsIsFilled() {
        testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o.setUid(UID))
                .build()
        );

        AdminOrdersDto dto = adminApiService.apiV1AdminOrderSearchGet(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                UID,
                null,
                null,
                null
        ).getBody();

        Assertions.assertThat(dto.getOrders().get(0)).hasNoNullFieldsOrPropertiesExcept(
                "displayNumber", "courierName", "courierPhone"
        );
    }

    @Test
    public void testSearchOrdersByUid() {
        Order order = testDataService.insertOrderWithCustomOrder(o -> o
                .setUid(UID)
        );

        AdminOrdersDto dto = adminApiService.apiV1AdminOrderSearchGet(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                UID,
                null,
                null,
                null
        ).getBody();

        //noinspection ConstantConditions
        Assertions.assertThat(dto.getOrders())
                .hasSize(1);

        Assertions.assertThat(dto.getOrders().get(0))
                .usingComparatorForType(Comparator.comparing(OffsetDateTime::toEpochSecond), OffsetDateTime.class)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .isEqualTo(order);
    }

    @Test
    public void testSearchOrdersByModificationTime() {
        clock.setTime(Instant.now(clock).plus(1, ChronoUnit.DAYS));
        OffsetDateTime now1 = OffsetDateTime.now(clock);
        Order order1 = testDataService.insertOrder();

        clock.setTime(Instant.now(clock).plus(1, ChronoUnit.DAYS));
        OffsetDateTime now2 = OffsetDateTime.now(clock);
        Order order2 = testDataService.insertOrder();

        clock.setTime(Instant.now(clock).plus(1, ChronoUnit.DAYS));
        OffsetDateTime now3 = OffsetDateTime.now(clock);

        AdminOrdersDto dto_1_2 = adminApiService.apiV1AdminOrderSearchGet(
                now1,
                now2,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ).getBody();

        //noinspection ConstantConditions
        Assertions.assertThat(dto_1_2.getOrders()).map(AdminOrderDto::getId)
                .hasSize(1)
                .containsExactly(order1.getId());

        AdminOrdersDto dto_1_3 = adminApiService.apiV1AdminOrderSearchGet(
                now1,
                now3,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ).getBody();

        //noinspection ConstantConditions
        Assertions.assertThat(dto_1_3.getOrders()).map(AdminOrderDto::getId)
                .hasSize(2)
                .containsExactly(order2.getId(), order1.getId());
    }

    @Test
    public void testSearchOrdersPaging() {
        AdminOrdersDto firstPage = adminApiService.apiV1AdminOrderSearchGet(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                2L
        ).getBody();

        //noinspection ConstantConditions
        Assertions.assertThat(firstPage.getOrders())
                .hasSize(2);

        AdminOrdersDto secondPage = adminApiService.apiV1AdminOrderSearchGet(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                firstPage.getOrders().get(1).getId(),
                2L
        ).getBody();

        //noinspection ConstantConditions
        Assertions.assertThat(secondPage.getOrders())
                .hasSize(2)
                .doesNotContainAnyElementsOf(firstPage.getOrders());
    }

    @Test
    void testOrderConfirmationUrlIsReturnedInDto() {
        OrderModel orderModel = orderFactory.createOrder();
        assertThat(orderModel.getOrderPayment().getPaymentConfirmationUrl()).isNotBlank();
        AdminOrderDto dto = adminApiService.apiV1AdminOrderGetGet(orderModel.getOrder().getId()).getBody();
        assertThat(dto.getPaymentConfirmationUrl()).isEqualTo(orderModel.getOrderPayment().getPaymentConfirmationUrl());
    }

    @Test
    void testGetAvailableEvents() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setProcessingMode(EProcessingMode.MANUAL)
                        .setOrderState(EOrderState.PROCESSING)
                        .setDeliveryState(EDeliveryOrderState.NEW)
                        .setPaymentState(EPaymentOrderState.NEW)
                        .setShopState(EShopOrderState.NEW)
                )
                .build()
        ).getOrder();

        List<OrderEvent> orderEvents = adminApiService.apiV1AdminOrderAvailableEventsGet(order.getId()).getBody();
        Assertions.assertThat(orderEvents)
                .containsExactlyInAnyOrder(
                        OrderEvent.MANUAL_ORDER_CANCELED,
                        OrderEvent.MANUAL_DELIVERY_CANCELED,
                        OrderEvent.MANUAL_DELIVERY_DELIVERED,
                        OrderEvent.SHOP_READY,
                        OrderEvent.SHOP_CANCEL,
                        OrderEvent.MANUAL_SHOP_NEW,
                        OrderEvent.PAYMENT_AUTHORIZE_START
                );
    }

    @Test
    void testSendEventInManual() {
        Order before = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setProcessingMode(EProcessingMode.MANUAL)
                        .setOrderState(EOrderState.PROCESSING)
                        .setDeliveryState(EDeliveryOrderState.NEW)
                        .setPaymentState(EPaymentOrderState.NEW)
                        .setShopState(EShopOrderState.NEW)
                )
                .build()
        ).getOrder();

        adminApiService.apiV1AdminOrderSendEventPost(before.getId(), new OrderEventDto().event(OrderEvent.SHOP_READY));
        AdminOrderDto after = adminApiService.apiV1AdminOrderGetGet(before.getId()).getBody();

        Assertions.assertThat(after)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .build()
                )
                .isEqualTo(new AdminOrderDto()
                        .processingMode(ProcessingMode.MANUAL)
                        .orderState(OrderState.PROCESSING)
                        .deliveryState(DeliveryState.NEW)
                        .paymentState(PaymentState.NEW)
                        .shopState(ShopState.READY)
                );
    }

    @Test
    void testPatch() {
        Order before = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setProcessingMode(EProcessingMode.MANUAL)
                        .setOrderState(EOrderState.CANCELING)
                        .setDeliveryState(EDeliveryOrderState.NEW)
                        .setPaymentState(EPaymentOrderState.NEW)
                        .setShopState(EShopOrderState.CANCELED)
                )
                .build()
        ).getOrder();

        OrderPatchDto patch = new OrderPatchDto()
                .version(before.getVersion())
                .cancelReason("Some cancel reason");

        adminApiService.apiV1AdminOrderPatchPost(before.getId(), patch);
        AdminOrderDto after = adminApiService.apiV1AdminOrderGetGet(before.getId()).getBody();

        Assertions.assertThat(after)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .withIgnoredFields("version")
                        .build()
                )
                .isEqualTo(patch);


        Assertions.assertThat(after)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .withComparatorForType(Comparator.comparing(OffsetDateTime::toEpochSecond),
                                OffsetDateTime.class)
                        .withIgnoredFields("cancelReason", "version", "modifiedAt")
                        .build()
                )
                .isEqualTo(before);
    }

    private void mockRequestAttributes(Map<String, Object> values) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ServletRequestAttributes attributes = new ServletRequestAttributes(request);
        values.forEach((key, value) -> attributes.setAttribute(key, value, SCOPE_REQUEST));
        RequestContextHolder.setRequestAttributes(attributes);
    }

    @SuppressWarnings("SameParameterValue")
    private static CheckedUserTicket createCheckedUserTicket(long uid) {
        return new CheckedUserTicket(
                TicketStatus.OK, "", new String[0], uid, new long[]{uid}
        );
    }

    @Test
    public void testClaimIdAndCourierLocation() {
        BigDecimal lat = BigDecimal.valueOf(12.123);
        BigDecimal lon = BigDecimal.valueOf(24.555);
        OrderModel order = testOrderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .setupDelivery(it -> it.setClaimId(CLAIM_ID).setCourierPosition(new Point(lat, lon)))
                        .build());

        AdminOrderDto dto = adminApiService.apiV1AdminOrderGetGet(order.getOrder().getId()).getBody();


        AdminOrderDto expected = new AdminOrderDto()
                .claimId(CLAIM_ID)
                .courierPosition(new GeoPointDto().lat(lat.doubleValue()).lon(lon.doubleValue()));

        Assertions.assertThat(dto).usingRecursiveComparison()
                .ignoringExpectedNullFields().isEqualTo(expected);
    }
}
