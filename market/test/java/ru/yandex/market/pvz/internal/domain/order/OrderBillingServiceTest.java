package ru.yandex.market.pvz.internal.domain.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistic.api.model.delivery.CargoType;
import ru.yandex.market.pvz.client.billing.dto.BillingOrderDto;
import ru.yandex.market.pvz.client.model.order.DeliveryServiceType;
import ru.yandex.market.pvz.client.model.order.OrderType;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.order.OrderCommandService;
import ru.yandex.market.pvz.core.domain.order.OrderRepository;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestBrandRegionFactory;
import ru.yandex.market.pvz.core.test.factory.TestDeliveryServiceFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.DELIVERED_TO_RECIPIENT;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.TRANSMITTED_TO_RECIPIENT;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_ITEMS;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_PAYMENT_STATUS;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_PAYMENT_TYPE;
import static ru.yandex.market.pvz.internal.controller.billing.mapper.BillingOrderDtoMapper.MAX_SCALE;

@Slf4j
@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderBillingServiceTest {

    private final TestableClock clock;

    private final TestDeliveryServiceFactory deliveryServiceFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestBrandRegionFactory brandRegionFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;
    private final OrderCommandService orderCommandService;
    private final OrderRepository orderRepository;
    private final OrderBillingService orderBillingService;

    @Test
    void whenGetDeliveredOrdersThenSuccess() {
        LocalDateTime dateTime = LocalDateTime.of(2021, 9, 1, 9, 0);
        var deliveryService = deliveryServiceFactory.createDeliveryService();
        var legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .deliveryService(deliveryService).build());
        var pickupPoint = pickupPointFactory.createPickupPoint(
                TestPickupPointFactory.CreatePickupPointBuilder.builder().legalPartner(legalPartner).build());
        ZoneOffset offset = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        clock.setFixed(dateTime.toInstant(offset), offset);

        Order order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .params(TestOrderFactory.OrderParams.builder()
                                .deliveryServiceType(DeliveryServiceType.DBS)
                                .build())
                        .build());
        orderFactory.receiveOrder(order.getId());
        order = orderFactory.verifyOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.UNKNOWN, null);
        Order transmitted = orderRepository.findByIdOrThrow(order.getId());
        assertThat(transmitted.getStatus()).isEqualTo(TRANSMITTED_TO_RECIPIENT);
        assertThat(transmitted.getDeliveredAt())
                .isEqualTo(clock.instant().atOffset(offset));

        String yadoOrderId = "123-LO-123";
        Order order2 = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .deliveryServiceType(DeliveryServiceType.YANDEX_DELIVERY)
                        .externalId(yadoOrderId)
                        .build())
                .pickupPoint(pickupPoint).build());
        orderFactory.receiveOrder(order2.getId());
        order2 = orderFactory.verifyOrder(order2.getId());
        orderFactory.deliverOrder(order2.getId(), OrderDeliveryType.UNKNOWN, null);
        Order transmitted2 = orderRepository.findByIdOrThrow(order2.getId());
        assertThat(transmitted2.getStatus()).isEqualTo(TRANSMITTED_TO_RECIPIENT);
        assertThat(transmitted2.getDeliveredAt())
                .isEqualTo(clock.instant().atOffset(offset));

        orderCommandService.commitDeliverByIds(List.of(order.getId(), order2.getId()));
        Order delivered = orderRepository.findByIdOrThrow(order2.getId());
        assertThat(delivered.getStatus()).isEqualTo(DELIVERED_TO_RECIPIENT);
        Order delivered2 = orderRepository.findByIdOrThrow(order2.getId());
        assertThat(delivered2.getStatus()).isEqualTo(DELIVERED_TO_RECIPIENT);

        List<BillingOrderDto> output = orderBillingService.getDeliveredOrdersByDaysPeriod(
                LocalDate.now(clock), LocalDate.now(clock)
        );

        BillingOrderDto expected1 = BillingOrderDto.builder()
                .id(order.getId())
                .externalId(order.getExternalId())
                .deliveryServiceId(deliveryService.getId())
                .pickupPointId(pickupPoint.getId())
                .paymentType(DEFAULT_PAYMENT_TYPE.name())
                .paymentStatus(DEFAULT_PAYMENT_STATUS.name())
                .deliveredAt(dateTime.atOffset(offset))
                .orderType(OrderType.CLIENT)
                .paymentSum(
                        StreamEx.of(DEFAULT_ITEMS)
                                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getCount())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(MAX_SCALE, RoundingMode.HALF_UP)
                )
                .itemsSum(
                        StreamEx.of(DEFAULT_ITEMS)
                                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getCount())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(MAX_SCALE, RoundingMode.HALF_UP)
                )
                .yandexDelivery(false)
                .deliveryServiceType(DeliveryServiceType.DBS)
                .build();

        BillingOrderDto expected2 = BillingOrderDto.builder()
                .id(order2.getId())
                .externalId(order2.getExternalId())
                .deliveryServiceId(deliveryService.getId())
                .pickupPointId(pickupPoint.getId())
                .paymentType(DEFAULT_PAYMENT_TYPE.name())
                .paymentStatus(DEFAULT_PAYMENT_STATUS.name())
                .deliveredAt(dateTime.atOffset(offset))
                .orderType(OrderType.CLIENT)
                .paymentSum(
                        StreamEx.of(DEFAULT_ITEMS)
                                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getCount())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(MAX_SCALE, RoundingMode.HALF_UP)
                )
                .itemsSum(
                        StreamEx.of(DEFAULT_ITEMS)
                                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getCount())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(MAX_SCALE, RoundingMode.HALF_UP)
                )
                .yandexDelivery(true)
                .deliveryServiceType(DeliveryServiceType.YANDEX_DELIVERY)
                .build();

        List<BillingOrderDto> expected = List.of(expected1, expected2);
        assertThat(output)
                .hasSize(expected.size())
                .hasSameElementsAs(expected);
    }

    @Test
    void whenGetDeliveredOrdersWithLimitThenSuccess() {
        LocalDateTime dateTime = LocalDateTime.of(2021, 9, 1, 9, 0);
        var deliveryService = deliveryServiceFactory.createDeliveryService();
        var legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .deliveryService(deliveryService).build());
        var pickupPoint = pickupPointFactory.createPickupPoint(
                TestPickupPointFactory.CreatePickupPointBuilder.builder().legalPartner(legalPartner).build());
        ZoneOffset offset = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        clock.setFixed(dateTime.toInstant(offset), offset);

        Order order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .params(TestOrderFactory.OrderParams.builder()
                                .deliveryServiceType(DeliveryServiceType.DBS)
                                .build())
                        .build());
        orderFactory.receiveOrder(order.getId());
        order = orderFactory.verifyOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.UNKNOWN, null);
        Order transmitted = orderRepository.findByIdOrThrow(order.getId());
        assertThat(transmitted.getStatus()).isEqualTo(TRANSMITTED_TO_RECIPIENT);
        assertThat(transmitted.getDeliveredAt())
                .isEqualTo(clock.instant().atOffset(offset));

        String yadoOrderId = "123-LO-123";
        Order order2 = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .deliveryServiceType(DeliveryServiceType.YANDEX_DELIVERY)
                        .externalId(yadoOrderId)
                        .build())
                .pickupPoint(pickupPoint).build());
        orderFactory.receiveOrder(order2.getId());
        order2 = orderFactory.verifyOrder(order2.getId());
        orderFactory.deliverOrder(order2.getId(), OrderDeliveryType.UNKNOWN, null);
        Order transmitted2 = orderRepository.findByIdOrThrow(order2.getId());
        assertThat(transmitted2.getStatus()).isEqualTo(TRANSMITTED_TO_RECIPIENT);
        assertThat(transmitted2.getDeliveredAt())
                .isEqualTo(clock.instant().atOffset(offset));

        orderCommandService.commitDeliverByIds(List.of(order.getId(), order2.getId()));
        Order delivered = orderRepository.findByIdOrThrow(order2.getId());
        assertThat(delivered.getStatus()).isEqualTo(DELIVERED_TO_RECIPIENT);
        Order delivered2 = orderRepository.findByIdOrThrow(order2.getId());
        assertThat(delivered2.getStatus()).isEqualTo(DELIVERED_TO_RECIPIENT);

        List<BillingOrderDto> output = orderBillingService.getDeliveredOrdersByDaysPeriod(
                LocalDate.now(clock), LocalDate.now(clock), 1L, 0L
        );

        BillingOrderDto expected1 = BillingOrderDto.builder()
                .id(order.getId())
                .externalId(order.getExternalId())
                .deliveryServiceId(deliveryService.getId())
                .pickupPointId(pickupPoint.getId())
                .paymentType(DEFAULT_PAYMENT_TYPE.name())
                .paymentStatus(DEFAULT_PAYMENT_STATUS.name())
                .deliveredAt(dateTime.atOffset(offset))
                .orderType(OrderType.CLIENT)
                .paymentSum(
                        StreamEx.of(DEFAULT_ITEMS)
                                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getCount())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(MAX_SCALE, RoundingMode.HALF_UP)
                )
                .itemsSum(
                        StreamEx.of(DEFAULT_ITEMS)
                                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getCount())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(MAX_SCALE, RoundingMode.HALF_UP)
                )
                .yandexDelivery(false)
                .deliveryServiceType(DeliveryServiceType.DBS)
                .build();

        List<BillingOrderDto> expected = List.of(expected1);
        assertThat(output)
                .hasSize(expected.size())
                .hasSameElementsAs(expected);

        output = orderBillingService.getDeliveredOrdersByDaysPeriod(
                LocalDate.now(clock), LocalDate.now(clock), 1L, 1L
        );

        BillingOrderDto expected2 = BillingOrderDto.builder()
                .id(order2.getId())
                .externalId(order2.getExternalId())
                .deliveryServiceId(deliveryService.getId())
                .pickupPointId(pickupPoint.getId())
                .paymentType(DEFAULT_PAYMENT_TYPE.name())
                .paymentStatus(DEFAULT_PAYMENT_STATUS.name())
                .deliveredAt(dateTime.atOffset(offset))
                .orderType(OrderType.CLIENT)
                .paymentSum(
                        StreamEx.of(DEFAULT_ITEMS)
                                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getCount())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(MAX_SCALE, RoundingMode.HALF_UP)
                )
                .itemsSum(
                        StreamEx.of(DEFAULT_ITEMS)
                                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getCount())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(MAX_SCALE, RoundingMode.HALF_UP)
                )
                .yandexDelivery(true)
                .deliveryServiceType(DeliveryServiceType.YANDEX_DELIVERY)
                .build();

        expected = List.of(expected2);
        assertThat(output)
                .hasSize(expected.size())
                .hasSameElementsAs(expected);
    }

    @Test
    void deliverPartialOrders() {
        LocalDateTime dateTime = LocalDateTime.of(2021, 9, 1, 9, 0);
        LocalDateTime commitTime = LocalDateTime.of(2021, 9, 1, 10, 0);

        var deliveryService = deliveryServiceFactory.createDeliveryService();
        var legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .deliveryService(deliveryService).build());
        brandRegionFactory.createDefaults();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .brandingType(PickupPointBrandingType.FULL)
                                .build())
                        .build()
        );
        ZoneOffset offset = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        clock.setFixed(dateTime.toInstant(offset), offset);

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .partialDeliveryAllowed(true)
                        .partialDeliveryAvailable(true)
                        .items(List.of(
                                TestOrderFactory.OrderItemParams.builder()
                                        .count(1)
                                        .price(BigDecimal.valueOf(100))
                                        .cargoTypeCodes(List.of(CargoType.FASHION.getCode()))
                                        .isService(false)
                                        .uitValues(List.of("uit_11"))
                                        .cargoTypeCodes(List.of(600))
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .count(2)
                                        .price(BigDecimal.valueOf(200))
                                        .cargoTypeCodes(List.of(CargoType.FASHION.getCode()))
                                        .isService(false)
                                        .uitValues(List.of("uit_21", "uit_22"))
                                        .cargoTypeCodes(List.of(600))
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .count(1)
                                        .price(BigDecimal.valueOf(50))
                                        .isService(true)
                                        .name("Доставка")
                                        .build()
                        ))
                        .build())
                .build());

        orderFactory.receiveOrder(order.getId());
        order = orderFactory.verifyOrder(order.getId());
        order = orderFactory.partialDeliver(order.getId(), List.of("uit_21", "uit_11"));

        clock.setFixed(commitTime.toInstant(offset), offset);
        orderCommandService.commitPartialDeliver(order.getId());
        orderCommandService.commitDeliverByIds(List.of(order.getId()));
        Order delivered = orderRepository.findByIdOrThrow(order.getId());
        assertThat(delivered.getStatus()).isEqualTo(DELIVERED_TO_RECIPIENT);

        List<BillingOrderDto> output = orderBillingService.getDeliveredOrdersByDaysPeriod(
                LocalDate.now(clock), LocalDate.now(clock)
        );

        BillingOrderDto expected1 = BillingOrderDto.builder()
                .id(order.getId())
                .externalId(order.getExternalId())
                .deliveryServiceId(deliveryService.getId())
                .pickupPointId(pickupPoint.getId())
                .paymentType(DEFAULT_PAYMENT_TYPE.name())
                .paymentStatus(DEFAULT_PAYMENT_STATUS.name())
                .deliveredAt(commitTime.atOffset(offset))
                .orderType(OrderType.CLIENT)
                .paymentSum(BigDecimal.valueOf(250).setScale(MAX_SCALE, RoundingMode.HALF_UP))
                .itemsSum(BigDecimal.valueOf(200).setScale(MAX_SCALE, RoundingMode.HALF_UP))
                .yandexDelivery(false)
                .deliveryServiceType(DeliveryServiceType.MARKET_COURIER)
                .build();

        List<BillingOrderDto> expected = List.of(expected1);
        assertThat(output).isEqualTo(expected);
    }

}
