package ru.yandex.market.tpl.core.service.order;

import java.math.BigDecimal;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
@Slf4j
public class ExpensiveOrderShowDocumentValidatorTest extends TplAbstractTest {

    private final OrderGenerateService orderGenerateService;
    private final ExpensiveOrderShowDocumentValidator validator;

    @Test
    void whenOrderPriceIsBelowThresholdThenNotShowDocument() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsPrice(OrderPhotoValidator.EXPENSIVE_ORDER_TOTAL_PRICE.subtract(BigDecimal.ONE))
                        .itemsCount(1)
                        .itemsItemCount(1)
                        .build())
                .build());
        assertThat(validator.isShowDocumentRequired(order)).isFalse();
    }

    @Test
    void whenTotalOrderPriceIsAboveThresholdThenShowDocument() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsPrice(OrderPhotoValidator.EXPENSIVE_ORDER_TOTAL_PRICE.subtract(BigDecimal.ONE))
                        .itemsCount(1)
                        .itemsItemCount(1)
                        .build())
                .deliveryPrice(BigDecimal.ONE)
                .build());
        assertThat(validator.isShowDocumentRequired(order)).isTrue();
    }

    @Test
    void whenAnyOrderRequiresDocumentThenShowDocument() {
        Order expensive = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsPrice(OrderPhotoValidator.EXPENSIVE_ORDER_TOTAL_PRICE)
                        .itemsCount(1)
                        .itemsItemCount(1)
                        .build())
                .build());
        Order notExpensive = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsPrice(OrderPhotoValidator.EXPENSIVE_ORDER_TOTAL_PRICE.subtract(BigDecimal.ONE))
                        .itemsCount(1)
                        .itemsItemCount(1)
                        .build())
                .build());
        assertThat(validator.isShowDocumentRequired(List.of(expensive, notExpensive))).isTrue();
    }
}
