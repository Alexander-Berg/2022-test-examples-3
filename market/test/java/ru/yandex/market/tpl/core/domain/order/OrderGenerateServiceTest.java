package ru.yandex.market.tpl.core.domain.order;

import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.core.service.order.OrderFeaturesResolver;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiredArgsConstructor
class OrderGenerateServiceTest extends TplAbstractTest {
    private static final BigDecimal ITEMS_PRICE = new BigDecimal("123.45");
    private final OrderGenerateService orderGenerateService;
    private final OrderFeaturesResolver orderFeaturesResolver;


    @Test
    void shouldCreateOrderWithValidSumPrice() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsPrice(ITEMS_PRICE)
                        .itemsCount(2)
                        .itemsItemCount(2)
                        .build())
                .build());

        Assertions.assertThat(order.getItems())
                .hasSize(2);

        OrderItem firstItem = order.getItems().get(0);
        OrderItem secondItem = order.getItems().get(1);

        Assertions.assertThat(firstItem.getCount())
                .isEqualByComparingTo(2);
        Assertions.assertThat(firstItem.getPrice())
                .isEqualByComparingTo(ITEMS_PRICE);
        Assertions.assertThat(firstItem.getSumPrice())
                .isEqualByComparingTo("246.9");

        Assertions.assertThat(secondItem.getCount())
                .isEqualByComparingTo(2);
        Assertions.assertThat(secondItem.getPrice())
                .isEqualByComparingTo(ITEMS_PRICE);
        Assertions.assertThat(secondItem.getSumPrice())
                .isEqualByComparingTo("246.9");
    }

    @Test
    void createCorrectFashionOrder() {
        //when
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentType(OrderPaymentType.CASH)
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .items(
                                OrderGenerateService.OrderGenerateParam.Items.builder()
                                        .isFashion(true)
                                        .build()
                        )
                        .build()
        );

        //then
        assertTrue(orderFeaturesResolver.isFashion(order));
    }
}
