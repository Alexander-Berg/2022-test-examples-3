package ru.yandex.market.tpl.core.service.order;

import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.EXPENSIVE_ORDERS_REQUIRE_PHOTO;

@RequiredArgsConstructor
@Slf4j
class OrderPhotoValidatorTest extends TplAbstractTest {

    private final OrderGenerateService orderGenerateService;
    private final OrderPhotoValidator orderPhotoValidator;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    @BeforeEach
    void init() {
        configurationServiceAdapter.insertValue(EXPENSIVE_ORDERS_REQUIRE_PHOTO, true);
    }

    @Test
    void whenOrderIsJustBelowExpensiveValueAPhotoIsNotRequired() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsPrice(OrderPhotoValidator.EXPENSIVE_ORDER_TOTAL_PRICE.subtract(BigDecimal.ONE))
                        .itemsCount(1)
                        .itemsItemCount(1)
                        .build())
                .build());
        assertThat(orderPhotoValidator.isPhotoRequired(order)).isFalse();
    }

    @Test
    void whenOrderTotalPriceIsAboveExpensiveValuieAPhotoIsRequired() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsPrice(OrderPhotoValidator.EXPENSIVE_ORDER_TOTAL_PRICE.subtract(BigDecimal.ONE))
                        .itemsCount(1)
                        .build())
                .deliveryPrice(BigDecimal.ONE)
                .build());
        assertThat(orderPhotoValidator.isPhotoRequired(order)).isTrue();
    }

    @Test
    void whenOrderIsCheapAPhotoIsNotRequired() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsPrice(BigDecimal.ONE)
                        .build())
                .build());
        assertThat(orderPhotoValidator.isPhotoRequired(order)).isFalse();
    }

    @Test
    void whenOrderIsFashionThenPhotoIsNotRequired() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsPrice(OrderPhotoValidator.EXPENSIVE_ORDER_TOTAL_PRICE.subtract(BigDecimal.ONE))
                        .itemsCount(1)
                        .isFashion(true)
                        .build())
                .deliveryPrice(BigDecimal.ONE)
                .build());
        assertThat(orderPhotoValidator.isPhotoRequired(order)).isFalse();
    }

}
