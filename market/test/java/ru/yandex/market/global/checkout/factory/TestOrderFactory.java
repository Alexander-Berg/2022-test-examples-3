package ru.yandex.market.global.checkout.factory;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.domain.order.OrderDeliveryRepository;
import ru.yandex.market.global.checkout.domain.order.OrderItemRepository;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.domain.order.OrderPaymentRepository;
import ru.yandex.market.global.checkout.domain.order.OrderRepository;
import ru.yandex.market.global.checkout.domain.promo.model.PromoApplication;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderDelivery;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderItem;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderPayment;
import ru.yandex.mj.generated.server.model.ShopExportDto;

import static ru.yandex.market.global.db.jooq.enums.EProcessingMode.AUTO;

@Transactional
public class TestOrderFactory {

    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(TestOrderFactory.class).build();

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDeliveryRepository orderDeliveryRepository;

    @Autowired
    private OrderPaymentRepository orderPaymentRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    public OrderModel createOrder() {
        return createOrder(CreateOrderBuilder.builder().build());
    }

    public OrderModel createOrder(CreateOrderBuilder builder) {
        Order order = builder.setupOrder.apply(RANDOM.nextObject(Order.class)
            .setProcessingMode(AUTO)
        );
        orderRepository.insert(order);

        OrderDelivery delivery = builder.setupDelivery.apply(RANDOM.nextObject(OrderDelivery.class))
                .setOrderId(order.getId());

        OrderPayment payment = builder.setupPayment.apply(RANDOM.nextObject(OrderPayment.class))
                .setOrderId(order.getId());

        List<OrderItem> items = builder.setupItems.apply(RANDOM.objects(OrderItem.class, 3 + RANDOM.nextInt(2))
                .collect(Collectors.toList())
        ).stream().peek(i -> i.setOrderId(order.getId())).collect(Collectors.toList());

        orderDeliveryRepository.insert(delivery);
        orderPaymentRepository.insert(payment);
        orderItemRepository.insert(items);

        return new OrderModel()
                .setOrder(order)
                .setOrderDelivery(delivery)
                .setOrderPayment(payment)
                .setOrderItems(items);
    }

    public static OrderActualization buildOrderActualization() {
        return buildOrderActualization(CreateOrderActualizationBuilder.builder().build());
    }

    public static OrderActualization buildOrderActualization(CreateOrderActualizationBuilder builder) {
        ShopExportDto shopDto = builder.setupShop.apply(RANDOM.nextObject(ShopExportDto.class));
        shopDto.getLegalEntity().setBusinessId(shopDto.getBusinessId());
        shopDto.setLegalEntityId(shopDto.getLegalEntity().getId());

        Order order = builder.setupOrder.apply(RANDOM.nextObject(Order.class))
                .setBusinessId(shopDto.getBusinessId())
                .setShopId(shopDto.getId());
        OrderDelivery delivery = builder.setupDelivery.apply(RANDOM.nextObject(OrderDelivery.class))
                .setOrderId(order.getId());
        OrderPayment payment = builder.setupPayment.apply(RANDOM.nextObject(OrderPayment.class))
                .setOrderId(order.getId());
        List<OrderItem> items = builder.setupItems.apply(RANDOM.objects(OrderItem.class, 3 + RANDOM.nextInt(2))
                .collect(Collectors.toList())
        ).stream().peek(i -> i
                .setOrderId(order.getId())
                .setBusinessId(shopDto.getBusinessId())
                .setShopId(shopDto.getId())
        ).collect(Collectors.toList());

        return new OrderActualization()
                .setOrder(order)
                .setOrderDelivery(delivery)
                .setOrderPayment(payment)
                .setOrderItems(items)
                .setShopExportDto(shopDto);
    }

    @Data
    @Builder
    public static class CreateOrderActualizationBuilder {
        @Builder.Default
        private Function<Order, Order> setupOrder = Function.identity();

        @Builder.Default
        private Function<OrderDelivery, OrderDelivery> setupDelivery = Function.identity();

        @Builder.Default
        private Function<OrderPayment, OrderPayment> setupPayment = Function.identity();

        @Builder.Default
        private Function<List<OrderItem>, List<OrderItem>> setupItems = Function.identity();

        @Builder.Default
        private Function<List<PromoApplication>, List<PromoApplication>> setupPromoApplications = Function.identity();

        @Builder.Default
        private Function<ShopExportDto, ShopExportDto> setupShop = Function.identity();
    }

    @Data
    @Builder
    public static class CreateOrderBuilder {

        @Builder.Default
        private Function<Order, Order> setupOrder = Function.identity();

        @Builder.Default
        private Function<OrderDelivery, OrderDelivery> setupDelivery = Function.identity();

        @Builder.Default
        private Function<OrderPayment, OrderPayment> setupPayment = Function.identity();

        @Builder.Default
        private Function<List<OrderItem>, List<OrderItem>> setupItems = Function.identity();
    }
}
