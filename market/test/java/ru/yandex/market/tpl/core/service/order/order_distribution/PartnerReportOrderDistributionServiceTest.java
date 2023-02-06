package ru.yandex.market.tpl.core.service.order.order_distribution;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.partner.order_distribution.PartnerReportOrderDistributionDto;
import ru.yandex.market.tpl.api.model.order.partner.order_distribution.PartnerReportOrderDistributionParamsDto;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;


@RequiredArgsConstructor
public class PartnerReportOrderDistributionServiceTest extends TplAbstractTest {

    private final OrderGenerateService orderGenerateService;
    private final Clock clock;

    private final PartnerReportOrderDistributionService partnerReportOrderDistributionService;

    private Order prepaidOrder;
    private Order cashOrder;
    private Order cardOrder;
    private final static String externalOrderId = "zax_order_id";

    @BeforeEach
    void setUp() {
        prepaidOrder = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentType(OrderPaymentType.PREPAID)
                        .deliveryDate(LocalDate.now(clock))
                        .build()
        );

        cardOrder = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentType(OrderPaymentType.CARD)
                        .deliveryDate(LocalDate.now(clock))
                        .externalOrderId(externalOrderId)
                        .build()
        );

        cashOrder = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentType(OrderPaymentType.CARD)
                        .deliveryDate(LocalDate.now(clock))
                        .build()
        );
    }

    @Test
    void findAll() {
        Page<PartnerReportOrderDistributionDto> result = partnerReportOrderDistributionService.findAll(
                PartnerReportOrderDistributionParamsDto.builder()
                        .dateFrom(LocalDate.now(clock))
                        .dateTo(LocalDate.now(clock).plusDays(1))
                        .build()
                ,
                Pageable.unpaged()
        );
        assertThat(result.getTotalElements()).isEqualTo(3);

    }

    @Test
    void testSpecification() {
        Page<PartnerReportOrderDistributionDto> result = partnerReportOrderDistributionService.findAll(
                PartnerReportOrderDistributionParamsDto.builder()
                        .dateFrom(LocalDate.now(clock))
                        .dateTo(LocalDate.now(clock).plusDays(1))
                        .externalOrderId(externalOrderId)
                        .orderId(cardOrder.getId())
                        .statuses(Set.of(OrderDeliveryStatus.NOT_DELIVERED))
                        .build()
                ,
                Pageable.unpaged()
        );
        assertThat(result.getTotalElements()).isEqualTo(1);

    }

}
