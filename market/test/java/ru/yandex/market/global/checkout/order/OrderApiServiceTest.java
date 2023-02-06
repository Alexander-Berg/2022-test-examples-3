package ru.yandex.market.global.checkout.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseApiTest;
import ru.yandex.market.global.checkout.api.AdminApiService;
import ru.yandex.market.global.checkout.api.CartApiService;
import ru.yandex.market.global.checkout.api.OrderApiService;
import ru.yandex.market.global.checkout.api.exception.CartActualizationException;
import ru.yandex.market.global.checkout.api.exception.NotFoundException;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.domain.order.OrderQueryService;
import ru.yandex.market.global.checkout.domain.order.OrderUtil;
import ru.yandex.market.global.checkout.domain.shop.ShopQueryService;
import ru.yandex.market.global.checkout.factory.TestCartFactory;
import ru.yandex.market.global.checkout.factory.TestElasticOfferFactory;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.factory.TestShopFactory;
import ru.yandex.market.global.checkout.factory.TestShopFactory.CreateShopDtoBuilder;
import ru.yandex.market.global.checkout.mapper.EntityMapper;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.common.elastic.dictionary.DictionaryQueryService;
import ru.yandex.market.global.common.jooq.Point;
import ru.yandex.market.global.common.test.TestClock;
import ru.yandex.market.global.common.util.configuration.ConfigurationService;
import ru.yandex.market.global.db.jooq.enums.EDeliveryOrderState;
import ru.yandex.market.global.db.jooq.enums.EOrderState;
import ru.yandex.market.global.db.jooq.enums.EPaymentOrderState;
import ru.yandex.market.global.db.jooq.enums.EShopOrderState;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderPayment;
import ru.yandex.mj.generated.server.model.AddressDto;
import ru.yandex.mj.generated.server.model.AdminOrderDto;
import ru.yandex.mj.generated.server.model.CartActualizeDto;
import ru.yandex.mj.generated.server.model.CartDto;
import ru.yandex.mj.generated.server.model.CartItemActualizeDto;
import ru.yandex.mj.generated.server.model.CartItemDto;
import ru.yandex.mj.generated.server.model.GeoPointDto;
import ru.yandex.mj.generated.server.model.OfferDto;
import ru.yandex.mj.generated.server.model.OrderCartDto;
import ru.yandex.mj.generated.server.model.OrderDeliverySchedulingType;
import ru.yandex.mj.generated.server.model.OrderDto;
import ru.yandex.mj.generated.server.model.OrderItemDto;
import ru.yandex.mj.generated.server.model.OrderState;
import ru.yandex.mj.generated.server.model.OrderUpdateDto;
import ru.yandex.mj.generated.server.model.OrdersDto;
import ru.yandex.mj.generated.server.model.ScheduleItemDto;
import ru.yandex.mj.generated.server.model.ShopOrderPatchDto;
import ru.yandex.mj.generated.server.model.ShopState;
import ru.yandex.mj.generated.server.model.YGAddend;
import ru.yandex.mj.generated.server.model.YGAdditionalPropertyDto;
import ru.yandex.mj.generated.server.model.YGCalculation;
import ru.yandex.mj.generated.server.model.YGDestination;
import ru.yandex.mj.generated.server.model.YGLegalEntityDto;
import ru.yandex.mj.generated.server.model.YGOrderDto;
import ru.yandex.mj.generated.server.model.YGOrdersSearchResponseDto;
import ru.yandex.mj.generated.server.model.YGReceipt;
import ru.yandex.mj.generated.server.model.YangoLegalEntityType;

import static ru.yandex.market.global.checkout.mapper.EntityMapper.MAPPER;
import static ru.yandex.market.global.checkout.util.PaymentUtil.APPLE_PAY_PAYMETHOD;
import static ru.yandex.market.global.common.test.TestUtil.createCheckedUserTicket;
import static ru.yandex.market.global.common.test.TestUtil.mockRequestAttributes;
import static ru.yandex.market.global.common.util.StringFormatter.sf;
import static ru.yandex.market.starter.tvm.filters.UserTicketFilter.CHECKED_USER_TICKET_ATTRIBUTE;

@SuppressWarnings("ConstantConditions")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderApiServiceTest extends BaseApiTest {
    public static final String APPLE_PAY_PAYMETHOD_WITH_ID = APPLE_PAY_PAYMETHOD
            + "_610-611a0d2e-5d3a-411a-965b-db3270ac7e80";
    private static final Point COURIER_POSITION = new Point(BigDecimal.valueOf(12.123), BigDecimal.valueOf(24.555));
    private static final long SHOP_ID = 40;
    private static final long SHOP_TARIFF_ID = 999111;
    private static final long BUSINESS_ID = 41;
    private static final String OFFER_ID = "OFFER_ID";
    private static final long UID = 10L;
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(OrderApiServiceTest.class).build();
    private static final String SOME_USER_TICKET = "some_user_ticket";
    private static final String SOME_YA_TAXI_USERID = "some-ya-taxi-userid";
    private static final String IDEMPOTENCY_KEY = UUID.randomUUID().toString();
    private static final String[] IGNORED_FIELDS = {
            "yaTaxiUserId",
            "promocodes",
            "appliedPromoIds",
            "referralId"
    };
    private static final Instant TIME = Instant.parse("2023-09-01T14:14:00.00Z");
    private static final RecursiveComparisonConfiguration RECURSIVE_COMPARISON_CONFIGURATION =
            RecursiveComparisonConfiguration.builder()
                    .withIgnoreAllExpectedNullFields(true)
                    .withIgnoreCollectionOrder(true)
                    .withComparatorForType(
                            Comparator.comparing(OffsetDateTime::toEpochSecond), OffsetDateTime.class
                    )
                    .build();
    private final TestOrderFactory orderFactory;
    private final TestCartFactory testCartFactory;
    private final TestShopFactory testShopFactory;
    private final TestClock clock;
    private final ShopQueryService shopQueryService;

    private final DictionaryQueryService<OfferDto> offersDictionary;
    private final OrderApiService orderApiService;
    private final CartApiService cartApiService;
    private final OrderQueryService orderQueryService;
    private final TestElasticOfferFactory testElasticOfferFactory;
    private final ConfigurationService configurationService;
    private final AdminApiService adminApiService;

    @BeforeEach
    public void setup() {
        Mockito.when(shopQueryService.get(Mockito.anyLong()))
                .thenReturn(testShopFactory.buildShopDto(CreateShopDtoBuilder.builder()
                                .setupShop(s -> s
                                        .id(SHOP_ID)
                                        .businessId(BUSINESS_ID)
                                        .defaultTariffId(SHOP_TARIFF_ID)
                                )
                                .build()
                        )
                );
        Mockito.when(offersDictionary.get(Mockito.anyList()))
                .thenReturn(testElasticOfferFactory.buildOne(
                        offer -> offer.price(51_00L)));
    }

    @Test
    public void testCreateOrderIdempotency() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        OrderDto created = orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, IDEMPOTENCY_KEY, createOrderCartDto()
        ).getBody();

        OrderDto createdAgain = orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, IDEMPOTENCY_KEY, createOrderCartDto()
        ).getBody();

        Assertions.assertThat(created).isEqualTo(createdAgain);
    }

    @Test
    public void testCartActualizeReturnCorrectCart() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));
        CartDto cartDto = cartApiService.apiV1CartActualizePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, createCartActualizeDto()
        ).getBody().getCart();

        cartDto.setTrustPaymethodId("SOME_TRUST_PAYMETHOD_ID");
        Assertions.assertThatCode(() -> orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, IDEMPOTENCY_KEY, fromCart(cartDto)
        )).doesNotThrowAnyException();
    }

    private OrderCartDto fromCart(CartDto cartDto) {
        return EntityMapper.MAPPER.toOrderCartDto(cartDto)
                .paymentReturnUrl("https://ya.ru/");
    }

    @Test
    public void testCartActualizeAllowEmptyCart() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        CartActualizeDto testCartActualizeDto = createCartActualizeDto()
                .items(List.of());

        Assertions.assertThatCode(() -> cartApiService.apiV1CartActualizePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, testCartActualizeDto
        )).doesNotThrowAnyException();
    }

    @Test
    public void testCartActualizeAllowEmptyUid() {
        CartActualizeDto testCartActualizeDto = createCartActualizeDto()
                .items(List.of());

        Assertions.assertThatCode(() -> cartApiService.apiV1CartActualizePost(
                null, SOME_YA_TAXI_USERID, testCartActualizeDto
        )).doesNotThrowAnyException();
    }

    @Test
    public void testCartActualizeUpdateAdult() {

        Mockito.when(offersDictionary.get(Mockito.anyList()))
                .thenReturn(testElasticOfferFactory.buildOne(
                        offer -> offer.price(123L)
                                .adult(true)
                                .businessId(BUSINESS_ID)
                                .shopId(SHOP_ID)
                                .offerId(OFFER_ID)));


        CartActualizeDto cartActualizeDto = testCartFactory.createCartActualizeDto(
                TestCartFactory.CreateCartActualizeDtoBuilder.builder()
                        .setupCartActualize(ca -> ca
                                .shopId(SHOP_ID)
                                .businessId(BUSINESS_ID)
                                .items(List.of(
                                        new CartItemActualizeDto()
                                                .businessId(BUSINESS_ID)
                                                .shopId(SHOP_ID)
                                                .offerId(OFFER_ID)
                                                .count(1L)
                                )))
                        .build()
        );

        CartDto actual = cartApiService.apiV1CartActualizePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, cartActualizeDto
        ).getBody().getCart();

        Assertions.assertThat(actual.getItems())
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .build()
                )
                .containsExactlyInAnyOrder(
                        new CartItemDto()
                                .offerId(OFFER_ID)
                                .adult(true)
                );

    }


    @Test
    public void testEmptyOrderNotAllowed() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        CartDto testCartDto = new CartDto()
                .businessId(BUSINESS_ID)
                .shopId(SHOP_ID)
                .recipientAddress(RANDOM.nextObject(AddressDto.class))
                .recipientFirstName(RANDOM.nextObject(String.class))
                .recipientLastName(RANDOM.nextObject(String.class))
                .recipientPhone(RANDOM.nextObject(String.class))
                .trustPaymethodId(RANDOM.nextObject(String.class));

        Assertions.assertThatThrownBy(() -> orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, IDEMPOTENCY_KEY,
                EntityMapper.MAPPER.toOrderCartDto(testCartDto)
        )).isInstanceOf(CartActualizationException.class);
    }

    @Test
    public void testCreateOrder() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        OrderCartDto cart = createOrderCartDto();
        OrderDto created = orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, IDEMPOTENCY_KEY, cart
        ).getBody();

        Assertions.assertThat(created)
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(cart);
        Assertions.assertThat(created.getVisibleForShop()).isFalse();
    }

    @Test
    public void testCreateApplePayOrder() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        String appleToken = "apple_token1234567890";
        OrderCartDto cart = createOrderCartDto();
        cart.setAppleToken(appleToken);
        cart.setTrustPaymethodId(APPLE_PAY_PAYMETHOD);
        OrderDto created = orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, IDEMPOTENCY_KEY, cart
        ).getBody();

        OrderPayment payment = orderQueryService.getPayment(created.getId());

        Assertions.assertThat(payment.getTrustPaymethodId()).isEqualTo(APPLE_PAY_PAYMETHOD);
        Assertions.assertThat(payment.getAppleToken()).isEqualTo(appleToken);
    }

    @Test
    public void testCreateApplePayOrderWithMissedToken() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        OrderCartDto cart = createOrderCartDto();
        cart.setAppleToken(null);
        cart.setTrustPaymethodId(APPLE_PAY_PAYMETHOD);

        Assertions.assertThatThrownBy(() -> orderApiService.apiV1OrderCreatePost(SOME_USER_TICKET,
                SOME_YA_TAXI_USERID, IDEMPOTENCY_KEY, cart));
    }

    @Test
    public void testCreateApplePayOrderWithoutToken() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        OrderCartDto cart = createOrderCartDto();
        cart.setAppleToken(null);
        cart.setTrustPaymethodId(APPLE_PAY_PAYMETHOD_WITH_ID);

        OrderDto created = orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, IDEMPOTENCY_KEY, cart
        ).getBody();

        OrderPayment payment = orderQueryService.getPayment(created.getId());

        Assertions.assertThat(payment.getTrustPaymethodId()).isEqualTo(APPLE_PAY_PAYMETHOD_WITH_ID);
        Assertions.assertThat(payment.getAppleToken()).isNull();

    }

    @Test
    public void testCreateScheduledOrder() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        OrderCartDto cart = createScheduledCart();
        OrderDto created = orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, IDEMPOTENCY_KEY, cart
        ).getBody();

        Assertions.assertThat(created)
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(cart);
        Assertions.assertThat(created.getVisibleForShop()).isFalse();
    }

    @Test
    public void testTariffIdIsActualized() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        OrderDto created = orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, IDEMPOTENCY_KEY, createOrderCartDto()
        ).getBody();

        Assertions.assertThat(orderQueryService.getItems(created.getId()))
                .allMatch(item -> Objects.equals(item.getTariffId(), item.getMarketCategoryId()));
    }

    @Test
    public void testShopDeliveryCostIsActualizedForCheapOrders() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        Mockito.when(offersDictionary.get(Mockito.anyList()))
                .thenReturn(testElasticOfferFactory.buildOne(
                        offer -> offer.price(49_00L / 2)));

        OrderDto created = orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, IDEMPOTENCY_KEY, createOrderCartDto()
        ).getBody();

        Assertions.assertThat(created.getDeliveryCostForShop()).isZero();
    }

    @Test
    public void testShopDeliveryCostIsActualizedForExpensiveOrders() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        Mockito.when(offersDictionary.get(Mockito.anyList()))
                .thenReturn(testElasticOfferFactory.buildOne(
                        offer -> offer.price(51_00L / 2)));

        OrderDto created = orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, IDEMPOTENCY_KEY, createOrderCartDto()
        ).getBody();

        Assertions.assertThat(created.getDeliveryCostForShop()).isNotZero();
    }

    @Test
    public void testCreateOrderNameDiffer() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        OrderCartDto toCreate = MAPPER.toOrderCartDto(createOrderCartDto()).items(List.of(new CartItemDto()
                .shopId(SHOP_ID)
                .businessId(BUSINESS_ID)
                .offerId(OFFER_ID)
                .name("bebebe")
                .count(2L)
        ));

        Assertions.assertThatThrownBy(() -> orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, IDEMPOTENCY_KEY, toCreate
        )).isInstanceOf(CartActualizationException.class);
    }

    @Test
    public void testCreateOrderPriceDiffer() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        OrderCartDto toCreate = MAPPER.toOrderCartDto(createOrderCartDto())
                .items(List.of(new CartItemDto()
                        .shopId(SHOP_ID)
                        .businessId(BUSINESS_ID)
                        .offerId(OFFER_ID)
                        .price(1L)
                        .count(2L)
                ));

        Assertions.assertThatThrownBy(() -> orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, null, toCreate
        )).isInstanceOf(CartActualizationException.class);
    }


    @Test
    @Disabled
    public void testCreateOrderShopIsClosed() {
        clock.setTime(Instant.parse("2021-12-10T12:27:00.00Z"));

        Mockito.when(shopQueryService.get(Mockito.anyLong()))
                .thenReturn(testShopFactory.buildShopDto(CreateShopDtoBuilder.builder()
                                .setupShop(s -> s
                                        .id(SHOP_ID)
                                        .businessId(BUSINESS_ID)
                                        .schedule(List.of(
                                                new ScheduleItemDto()
                                                        .open(true)
                                                        .day("ANY")
                                                        .startAt("00:00:00")
                                                        .endAt("23:59:59"),
                                                new ScheduleItemDto()
                                                        .open(false)
                                                        .day("2021-12-10")
                                                        .startAt("00:00:00")
                                                        .endAt("23:59:59")
                                        ))
                                )
                                .build()
                        )
                );

        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        Assertions.assertThatThrownBy(() -> orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, IDEMPOTENCY_KEY, createOrderCartDto()
        )).isInstanceOf(CartActualizationException.class);
    }


    @Test
    public void testCreateOrderCartIsEmpty() {
        clock.setTime(Instant.parse("2021-12-10T12:27:00.00Z"));

        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        OrderCartDto toCreate = MAPPER.toOrderCartDto(createOrderCartDto())
                .items(List.of());

        Assertions.assertThatThrownBy(() -> orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, IDEMPOTENCY_KEY, toCreate
        )).isInstanceOf(CartActualizationException.class);
    }

    @Test
    public void testGetOrder() {
        OrderDto dto = orderApiService.apiV1OrderGetGet(testData.getNewOrder().getId()).getBody();
        Assertions.assertThat(testData.getNewOrder())
                .usingComparatorForType(Comparator.comparing(OffsetDateTime::toEpochSecond), OffsetDateTime.class)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .ignoringCollectionOrder()
                .isEqualTo(dto);
    }

    @Test
    public void testGetYGOrderNotFound() {
        OrderModel orderModel = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o.setUid(UID + 1))
                .build()
        );

        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));
        Assertions.assertThatThrownBy(
                () -> orderApiService.apiV1OrderGetYgGet(orderModel.getOrder().getId(), SOME_USER_TICKET)
        ).isInstanceOf(NotFoundException.class);
    }

    @Test
    public void testGetYGOrder() {
        OrderModel orderModel = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setUid(UID)
                        .setOrderState(EOrderState.PROCESSING)
                        .setDeliveryState(EDeliveryOrderState.DELIVERING_ORDER)
                        .setPaymentState(EPaymentOrderState.AUTHORIZED)
                        .setShopState(EShopOrderState.READY)
                )
                .setupDelivery(d -> d.setRecipientApartment(null))
                .build()
        );

        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));
        YGOrderDto dto = orderApiService.apiV1OrderGetYgGet(orderModel.getOrder().getId(), SOME_USER_TICKET).getBody();

        List<YGAddend> addends = Stream.concat(
                orderModel.getOrderItems().stream()
                        .map(i -> new YGAddend()
                                .name(i.getName())
                                .count(i.getCount().intValue())
                                .cost(OrderUtil.getStringPrice(
                                        i.getPrice(),
                                        orderModel.getOrder().getCurrency()
                                ) + " $SIGN$$CURRENCY$")
                        ),
                Stream.of(new YGAddend()
                        .name("Delivery")
                        .cost(OrderUtil.getStringPrice(
                                orderModel.getOrder().getDeliveryCostForRecipient(),
                                orderModel.getOrder().getCurrency()
                        ) + " $SIGN$$CURRENCY$"))
        ).collect(Collectors.toList());

        Assertions.assertThat(dto)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .withIgnoreCollectionOrder(true)
                        .withComparatorForType(Comparator.comparing(OffsetDateTime::toInstant), OffsetDateTime.class)
                        .build())
                .isEqualTo(new YGOrderDto()
                        .orderId(orderModel.getOrder().getId().toString())
                        .marketOrderId(orderModel.getOrder().getId())
                        .createdAt(orderModel.getOrder().getCreatedAt())
                        .title(orderModel.getOrderDelivery().getShopName())
                        .service("market_locals")
                        .status("delivery")
                        .destinations(List.of(new YGDestination()
                                .shortText(Lists.newArrayList(
                                        orderModel.getOrderDelivery().getRecipientAddress().getLocality(),
                                        orderModel.getOrderDelivery().getRecipientAddress().getStreet(),
                                        orderModel.getOrderDelivery().getRecipientAddress().getHouse(),
                                        orderModel.getOrderDelivery().getRecipientAddress().getApartment()
                                ).stream().filter(Objects::nonNull).collect(Collectors.joining(" ")))
                                .point(List.of(
                                        orderModel.getOrderDelivery().getRecipientAddress()
                                                .getCoordinates().getLat().doubleValue(),
                                        orderModel.getOrderDelivery().getRecipientAddress()
                                                .getCoordinates().getLon().doubleValue()
                                ))
                        ))
                        .legalEntities(List.of(
                                new YGLegalEntityDto()
                                        .type(YangoLegalEntityType.SHOP)
                                        .title("shop")
                                        .addAdditionalPropertiesItem(new YGAdditionalPropertyDto()
                                                .title("name")
                                                .value(orderModel.getOrderDelivery().getShopName())
                                        ),
                                new YGLegalEntityDto()
                                        .type(YangoLegalEntityType.DELIVERY_SERVICE)
                                        .title("delivery")
                                        .addAdditionalPropertiesItem(new YGAdditionalPropertyDto()
                                                .title("name")
                                                .value("YanGo")
                                        )

                        ))
                        .calculation(new YGCalculation()
                                .finalCost(OrderUtil.getStringPrice(
                                        orderModel.getOrder().getTotalCost(),
                                        orderModel.getOrder().getCurrency()
                                ) + " $SIGN$$CURRENCY$")
                                .discount(OrderUtil.getStringPrice(
                                        orderModel.getOrder().getTotalItemsCost()
                                                - orderModel.getOrder().getTotalItemsCostWithPromo(),
                                        orderModel.getOrder().getCurrency()
                                ) + " $SIGN$$CURRENCY$")
                                .currencyCode(orderModel.getOrder().getCurrency().name())
                                .addends(addends)
                        )
                        .receipts(List.of(
                                new YGReceipt()
                                        .receiptUrl(orderModel.getOrderPayment().getPaymentConfirmationUrl())
                                        .title("Payment Confirmation")
                        ))
                );
    }

    @Test
    public void testSearchOrdersByShopId() {
        clock.setTime(TIME);

        Order order = testDataService.insertOrderWithCustomOrder(o -> o
                .setShopId(SHOP_ID)
        );

        OrdersDto dto = orderApiService.apiV1OrderSearchGet(
                SHOP_ID,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ).getBody();

        //noinspection ConstantConditions
        Assertions.assertThat(dto.getOrders())
                .hasSize(1);

        Assertions.assertThat(order)
                .usingComparatorForType(Comparator.comparing(OffsetDateTime::toEpochSecond), OffsetDateTime.class)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(dto.getOrders().get(0));
    }

    @Test
    public void testSearchUserOrdersPaging() {
        IntStream.range(0, 3).forEach(i -> testDataService.insertOrderWithCustomOrder(o -> o.setUid(UID)));

        mockRequestAttributes(Map.of(
                CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)
        ));

        OrdersDto firstPage = orderApiService.apiV1OrderUserSearchGet(
                SOME_USER_TICKET,
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

        OrdersDto secondPage = orderApiService.apiV1OrderUserSearchGet(
                SOME_USER_TICKET,
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
                .hasSize(1)
                .doesNotContainAnyElementsOf(firstPage.getOrders());
    }

    @Test
    public void testSearchUserOrders() {
        mockRequestAttributes(Map.of(
                CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(testData.getNewOrder().getUid())
        ));

        OrdersDto dto = orderApiService.apiV1OrderUserSearchGet(
                SOME_USER_TICKET,
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
        Assertions.assertThat(dto.getOrders())
                .hasSize(1);

        Assertions.assertThat(testData.getNewOrder())
                .usingComparatorForType(Comparator.comparing(OffsetDateTime::toEpochSecond), OffsetDateTime.class)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(dto.getOrders().get(0));
    }

    @Test
    public void testSearchUserOrderByState() {
        OrderModel processingOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o.setUid(UID).setOrderState(EOrderState.PROCESSING))
                .build());

        //noinspection unused
        OrderModel canceledOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o.setUid(UID).setOrderState(EOrderState.CANCELED))
                .build());

        OrderModel finishedOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o.setUid(UID).setOrderState(EOrderState.FINISHED))
                .build());

        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));
        OrdersDto dto = orderApiService.apiV1OrderUserSearchGet(
                SOME_USER_TICKET,
                null,
                null,
                null,
                List.of(OrderState.FINISHED, OrderState.PROCESSING),
                null,
                null,
                null,
                null,
                100L
        ).getBody();

        Assertions.assertThat(dto.getOrders().toArray())
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        // Assertj неправильно обрабатывает withComparedFields
                        // Поэтому, сравнить только по некоторым полям, можно только, если игнорировать
                        // все кроме них. Заклинание ниже как раз это и делает.
                        .withIgnoredFieldsMatchingRegexes("(?!(?:id|uid)$).*")
                        .build()
                )
                .containsExactlyInAnyOrder(List.of(finishedOrder.getOrder(), processingOrder.getOrder()).toArray());
    }

    @Test
    public void testSearchUserYGOrders() {
        OrderModel userOrderModel1 = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o.setUid(UID))
                .build()
        );

        OrderModel userOrderModel2 = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o.setUid(UID))
                .build()
        );

        //noinspection unused
        OrderModel otherUserOrderModel = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o.setUid(UID + 1))
                .build()
        );

        mockRequestAttributes(Map.of(
                CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)
        ));

        YGOrdersSearchResponseDto dto = orderApiService.apiV1OrderUserSearchYgGet(
                SOME_USER_TICKET,
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
        Assertions.assertThat(dto.getOrders())
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        .withComparedFields("market_order_id")
                        .build()
                )
                .containsExactlyInAnyOrder(
                        new YGOrderDto()
                                .marketOrderId(userOrderModel1.getOrder().getId()),
                        new YGOrderDto()
                                .marketOrderId(userOrderModel2.getOrder().getId())
                );
    }

    @Test
    public void testSearchOrdersInPast() {
        clock.setTime(TIME);

        Order order = testDataService.insertOrderWithCustomOrder(o -> o
                .setShopId(SHOP_ID)
        );

        OrdersDto dto = orderApiService.apiV1OrderSearchGet(
                SHOP_ID,
                null,
                order.getCreatedAt(),
                null,
                null,
                null,
                null,
                null
        ).getBody();

        //noinspection ConstantConditions
        Assertions.assertThat(dto.getOrders()).isEmpty();
    }

    @Test
    public void testSearchOrdersInFuture() {
        clock.setTime(TIME);

        Order order = testDataService.insertOrderWithCustomOrder(o -> o
                .setShopId(SHOP_ID)
        );

        OrdersDto dto = orderApiService.apiV1OrderSearchGet(
                SHOP_ID,
                order.getCreatedAt().plusDays(1),
                null,
                null,
                null,
                null,
                null,
                null
        ).getBody();

        //noinspection ConstantConditions
        Assertions.assertThat(dto.getOrders()).isEmpty();
    }

    @Test
    public void testSearchOrdersNow() {
        clock.setTime(TIME);

        Order order = testDataService.insertOrderWithCustomOrder(o -> o
                .setShopId(SHOP_ID)
        );

        OrdersDto dto = orderApiService.apiV1OrderSearchGet(
                SHOP_ID,
                order.getCreatedAt().minusSeconds(1),
                order.getCreatedAt().plusSeconds(1),
                null,
                null,
                null,
                null,
                null
        ).getBody();

        //noinspection ConstantConditions
        Assertions.assertThat(dto.getOrders())
                .hasSize(1);

        Assertions.assertThat(order)
                .usingComparatorForType(Comparator.comparing(OffsetDateTime::toEpochSecond), OffsetDateTime.class)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(dto.getOrders().get(0));
    }

    @Test
    public void testShopReady() {
        OrderDto updated = orderApiService.apiV1OrderShopReadyPost(testData.getNewOrder().getId()).getBody();
        Assertions.assertThat(updated)
                .hasFieldOrPropertyWithValue("shopState", ShopState.READY);
    }

    @Test
    public void testShopCancel() {
        OrderDto updated = orderApiService.apiV1OrderShopCancelPost(testData.getNewOrder().getId()).getBody();
        Assertions.assertThat(updated)
                .hasFieldOrPropertyWithValue("shopState", ShopState.CANCELED);
    }

    @Test
    public void testRecipientCancel() {
        mockRequestAttributes(Map.of(
                CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(testData.getNewOrder().getUid())
        ));

        OrderDto updated = orderApiService.apiV1OrderRecipientCancelPost(testData.getNewOrder().getId()).getBody();
        Assertions.assertThat(updated)
                .hasFieldOrPropertyWithValue("orderState", OrderState.CANCELING);
    }

    @Test
    public void testShopUpdate() {
        Order existingOrder = testDataService.insertOrderWithCustomOrder(order ->
                order.setBusinessId(BUSINESS_ID)
                        .setShopId(SHOP_ID)
                        .setOrderState(EOrderState.PROCESSING)
                        .setShopState(EShopOrderState.NEW)
                        .setDeliveryState(EDeliveryOrderState.NEW)
                        .setPaymentState(EPaymentOrderState.NEW)
        );

        OrderUpdateDto updateDto = RANDOM.nextObject(OrderUpdateDto.class)
                .items(List.of(new CartItemActualizeDto()
                        .shopId(SHOP_ID)
                        .businessId(BUSINESS_ID)
                        .offerId(OFFER_ID)
                        .count(2L)
                ));

        OrderDto updated = orderApiService.apiV1OrderShopUpdatePost(existingOrder.getId(), updateDto)
                .getBody();

        Assertions.assertThat(updated)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .ignoringCollectionOrder()
                .isEqualTo(updateDto);
    }

    @Test
    public void testCourierLocationInOrder() {
        OrderModel order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupDelivery(it -> it.setCourierPosition(COURIER_POSITION))
                .build()
        );

        OrderDto dto = orderApiService.apiV1OrderGetGet(order.getOrder().getId()).getBody();

        GeoPointDto expected = new GeoPointDto()
                .lat(COURIER_POSITION.getLat().doubleValue())
                .lon(COURIER_POSITION.getLon().doubleValue());

        Assertions.assertThat(dto.getCourierPosition()).usingRecursiveComparison()
                .ignoringExpectedNullFields().isEqualTo(expected);
    }

    @Test
    public void testCourierLocation() {
        OrderModel order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupDelivery(it -> it.setCourierPosition(COURIER_POSITION))
                .build()
        );

        GeoPointDto dto = orderApiService.apiV1OrderCourierPositionGet(order.getOrder().getId()).getBody();

        GeoPointDto expected = new GeoPointDto()
                .lat(COURIER_POSITION.getLat().doubleValue())
                .lon(COURIER_POSITION.getLon().doubleValue());

        Assertions.assertThat(dto).usingRecursiveComparison()
                .ignoringExpectedNullFields().isEqualTo(expected);
    }

    private OrderCartDto createOrderCartDto() {
        return createCartDto(null, null);
    }

    private OrderCartDto createCartDto(String trustPaymethod, String appleToken) {
        return testCartFactory.createOrderCartDto(TestCartFactory.CreateOrderCartDtoBuilder.builder()
                .setupCart(c -> new OrderCartDto()
                        .businessId(BUSINESS_ID)
                        .shopId(SHOP_ID)
                        .recipientFirstName(c.getRecipientFirstName())
                        .recipientLastName(c.getRecipientLastName())
                        .recipientPhone(c.getRecipientPhone())
                        .trustPaymethodId(trustPaymethod != null ? trustPaymethod : c.getTrustPaymethodId())
                        .appleToken(appleToken)
                        .deliverySchedulingType(OrderDeliverySchedulingType.NOW)
                        .items(List.of(new CartItemDto()
                                .shopId(SHOP_ID)
                                .businessId(BUSINESS_ID)
                                .offerId(OFFER_ID)
                                .count(2L)
                        ))
                        .paymentReturnUrl("https://ya.ru/")
                        .estimatedDeliveryTime(OffsetDateTime.now(clock))
                )
                .build()
        );
    }

    private OrderCartDto createScheduledCart() {
        OffsetDateTime requestedDeliveryTime = OffsetDateTime.now(clock)
                .plus(1, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.HOURS);

        return testCartFactory.createOrderCartDto(TestCartFactory.CreateOrderCartDtoBuilder.builder()
                .setupCart(c -> new OrderCartDto()
                        .businessId(BUSINESS_ID)
                        .shopId(SHOP_ID)
                        .recipientFirstName(c.getRecipientFirstName())
                        .recipientLastName(c.getRecipientLastName())
                        .recipientPhone(c.getRecipientPhone())
                        .trustPaymethodId(c.getTrustPaymethodId())
                        .deliverySchedulingType(OrderDeliverySchedulingType.TO_REQUESTED_TIME)
                        .requestedDeliveryTime(requestedDeliveryTime)
                        .items(List.of(new CartItemDto()
                                .shopId(SHOP_ID)
                                .businessId(BUSINESS_ID)
                                .offerId(OFFER_ID)
                                .count(2L)
                        ))
                        .paymentReturnUrl("https://ya.ru/")
                )
                .build()
        );
    }

    @Test
    void testPatch() {
        Order before = orderFactory.createOrder().getOrder();
        OffsetDateTime shopSeenAt = OffsetDateTime.now(clock);

        ShopOrderPatchDto patch = new ShopOrderPatchDto()
                .version(before.getVersion())
                .shopSeenAt(shopSeenAt);

        orderApiService.apiV1OrderShopPatchPost(before.getId(), patch);
        OrderDto after = orderApiService.apiV1OrderGetGet(before.getId()).getBody();

        Assertions.assertThat(after)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .withComparatorForType(
                                Comparator.comparing(OffsetDateTime::toEpochSecond), OffsetDateTime.class
                        )
                        .withIgnoredFields("version")
                        .build()
                )
                .isEqualTo(patch);


        Assertions.assertThat(after)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .withIgnoreCollectionOrder(true)
                        .withComparatorForType(
                                Comparator.comparing(OffsetDateTime::toEpochSecond), OffsetDateTime.class
                        )
                        .withIgnoredFields("shopSeenAt", "version", "modifiedAt", "promocodes")
                        .build()
                )
                .isEqualTo(before);

        Assertions.assertThat(after.getPromocodes())
                .containsExactlyInAnyOrder(before.getPromocodes());

    }

    @Test
    public void testVisibleForShop() {
        clock.setTime(TIME);

        Order order = testDataService.insertOrderWithCustomOrder(o -> o
                .setShopId(SHOP_ID).setVisibleForShop(false)
        );

        OrdersDto dto = orderApiService.apiV1OrderSearchGet(
                SHOP_ID,
                null,
                null,
                null,
                null,
                null,
                null,
                true
        ).getBody();

        //noinspection ConstantConditions
        Assertions.assertThat(dto.getOrders()).isEmpty();
    }

    @Test
    public void testCorrectReturnURL() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        OrderCartDto cart = createOrderCartDto().paymentReturnUrl("https://ya.ru/{orderId}/")
                .trustPaymethodId("cart-12345678fffsdf567NEW");
        OrderDto created = orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, IDEMPOTENCY_KEY, cart
        ).getBody();

        Assertions.assertThat(created)
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(cart);

        OrderPayment payment = orderQueryService.getPayment(created.getId());
        Assertions.assertThat(payment.getPaymentRedirectUrl()).isEqualTo(sf("https://ya.ru/{}/", created.getId()));
    }

    @Test
    public void testWaitingPayment() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        OrderCartDto cart = createOrderCartDto()
                .paymentReturnUrl("https://ya.ru/{orderId}/")
                .trustPaymethodId("cart-12345678fffsdf566NEW");
        OrderDto created = orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, IDEMPOTENCY_KEY, cart
        ).getBody();

        Assertions.assertThat(created.getOrderState()).isEqualTo(OrderState.WAITING_PAYMENT);
        Assertions.assertThat(created.getRequired3ds()).isNotNull().isTrue();
    }


    @Test
    public void testOrderEstimatedDeliveryTime() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        OffsetDateTime estimatedDeliveryTime = OffsetDateTime.now(clock).plus(60, ChronoUnit.MINUTES);

        OrderCartDto orderCart = createOrderCartDto()
                .estimatedDeliveryTime(estimatedDeliveryTime);
        OrderDto created = orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, IDEMPOTENCY_KEY, orderCart
        ).getBody();

        Assertions.assertThat(created).isNotNull();
        Assertions.assertThat(created.getEstimatedDeliveryTime()).isEqualTo(estimatedDeliveryTime);

        OrderDto extracted = orderApiService.apiV1OrderGetGet(created.getId()).getBody();

        Assertions.assertThat(extracted).isNotNull();
        Assertions.assertThat(extracted.getEstimatedDeliveryTime()).isEqualTo(estimatedDeliveryTime);

        AdminOrderDto adminOrderDto = adminApiService.apiV1AdminOrderGetGet(created.getId()).getBody();
        Assertions.assertThat(adminOrderDto).isNotNull();
        Assertions.assertThat(adminOrderDto.getEstimatedDeliveryTime()).isEqualTo(estimatedDeliveryTime);
    }

    @Test
    public void testNullableOrderEstimatedDeliveryTime() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        OrderCartDto orderCart = createOrderCartDto()
                .estimatedDeliveryTime(null);
        OrderDto created = orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, IDEMPOTENCY_KEY, orderCart
        ).getBody();

        Assertions.assertThat(created).isNotNull();
        Assertions.assertThat(created.getEstimatedDeliveryTime()).isNull();

        OrderDto extracted = orderApiService.apiV1OrderGetGet(created.getId()).getBody();

        Assertions.assertThat(extracted).isNotNull();
        Assertions.assertThat(extracted.getEstimatedDeliveryTime()).isNull();

        AdminOrderDto adminOrderDto = adminApiService.apiV1AdminOrderGetGet(created.getId()).getBody();
        Assertions.assertThat(adminOrderDto).isNotNull();
        Assertions.assertThat(adminOrderDto.getEstimatedDeliveryTime()).isNull();
    }



    private CartActualizeDto createCartActualizeDto() {
        return testCartFactory.createCartActualizeDto(TestCartFactory.CreateCartActualizeDtoBuilder.builder()
                .setupCartActualize(c -> new CartActualizeDto()
                        .businessId(BUSINESS_ID)
                        .shopId(SHOP_ID)
                        .recipientFirstName(c.getRecipientFirstName())
                        .recipientLastName(c.getRecipientLastName())
                        .recipientPhone(c.getRecipientPhone())
                        .deliverySchedulingType(OrderDeliverySchedulingType.NOW)
                        .items(List.of(new CartItemActualizeDto()
                                .shopId(SHOP_ID)
                                .businessId(BUSINESS_ID)
                                .offerId(OFFER_ID)
                                .count(2L)
                        ))
                ).build()
        );
    }

    @Test
    public void testCreateOrderWithoutPlus() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        OrderCartDto cart = createOrderCartDto()
                .plusEarned(null)
                .plusSpent(null)
                .plusAction(null);
        cart.getItems().forEach(it -> it.plusEarned(null).plusSpent(null));
        OrderDto created = orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, SOME_YA_TAXI_USERID, IDEMPOTENCY_KEY, cart
        ).getBody();

        Assertions.assertThat(created.getPlusEarned()).isNull();
        Assertions.assertThat(created.getPlusSpent()).isNull();
        for (OrderItemDto item : created.getItems()) {
            Assertions.assertThat(item.getPlusEarned()).isNull();
            Assertions.assertThat(item.getPlusSpent()).isNull();
        }
    }


}
