package ru.yandex.market.tpl.core.domain.receipt.tpl;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.receipt.ReceiptDataDto;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.order.OrderPaymentType.CARD;
import static ru.yandex.market.tpl.api.model.order.OrderPaymentType.CASH;

@RequiredArgsConstructor
public class TplReceiptMapperTest extends TplAbstractTest {
    private final OrderGenerateService orderGenerateService;
    private final TplReceiptMapper tplReceiptMapper;
    private final TransactionTemplate transactionTemplate;
    private final OrderRepository orderRepository;

    @Test
    void mapReceiptDataPaymentDto_withEmptyPartialReturnTest() {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .purchaseStrategy(OrderGenerateService.OrderGenerateParam.PurchaseStrategy.MIXED)
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                                .isFashion(true)
                                .build())
                        .build()
        );

        transactionTemplate.execute(ts -> {
            var tsOrder = orderRepository.findByIdOrThrow(order.getId());
            ReceiptDataDto.PaymentDto paymentDto = tplReceiptMapper.mapReceiptDataPaymentDto(tsOrder, CARD, Optional.empty());
            assertThat(paymentDto.getCardAmount()).isEqualTo(tsOrder.getTotalPrice());
            return null;
        });
    }

    @Test
    void mapReceiptDataPaymentDto_withPresentPartialReturnTest() {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .purchaseStrategy(OrderGenerateService.OrderGenerateParam.PurchaseStrategy.MIXED)
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                                .isFashion(true)
                                .build())
                        .build()
        );

        transactionTemplate.execute(ts -> {
            var tsOrder = orderRepository.findByIdOrThrow(order.getId());
            ReceiptDataDto.PaymentDto paymentDto = tplReceiptMapper.mapReceiptDataPaymentDto(tsOrder, CASH, Optional.of(1L));
            assertThat(paymentDto.getCashAmount()).isEqualTo(tsOrder.getCostForPurchasedOrderItemsInstancesWithDeliveryPrice());
            return null;
        });
    }

}
