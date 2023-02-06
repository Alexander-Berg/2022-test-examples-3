package ru.yandex.market.tpl.internal.service.report.csv;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.partner.order_distribution.PartnerCourierDto;
import ru.yandex.market.tpl.api.model.order.partner.order_distribution.PartnerReportOrderDistributionDto;
import ru.yandex.market.tpl.api.model.order.partner.order_distribution.PartnerReportOrderDistributionParamsDto;
import ru.yandex.market.tpl.core.service.order.order_distribution.PartnerReportOrderDistributionService;
import ru.yandex.market.tpl.internal.controller.TplIntTest;
import ru.yandex.market.tpl.internal.service.report.OrderDistributionReportStrategy;
import ru.yandex.market.tpl.internal.service.report.OrdersDistributionReportService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Тест для проверки вёрсти печатной формы упрощенного списка заказов.
 * Сделан для локальной проверки, поэтому помечен {@link Disabled}.
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@TplIntTest
@Slf4j
@Disabled
public class DistributionOrderExportTest {
    private final OrderDistributionReportStrategy orderExcelReportStrategy;
    @MockBean
    private PartnerReportOrderDistributionService partnerReportOrderService;
    private OrdersDistributionReportService ordersReportService;

    @BeforeEach
    void setUp() {
        ordersReportService = new OrdersDistributionReportService(
                partnerReportOrderService,
                orderExcelReportStrategy
        );
        given(partnerReportOrderService.findAll(
                        any(PartnerReportOrderDistributionParamsDto.class)
                )
        ).willReturn(getTestData());
    }


    @Test
    @Disabled
    @DisplayName("Тест для отладки верстки отчета списка заказов")
    public void getOrdersReport() throws IOException {
        String path = System.getProperty("user.home") + "/test/orders" + Instant.now() + ".xlsx";
        log.info(String.format("Тестовый отчёт списка заказов %s", path));
        FileOutputStream fos = new FileOutputStream(path);

        ordersReportService.getOrdersReportExcel(fos, new PartnerReportOrderDistributionParamsDto());

        fos.flush();
        fos.close();
    }

    private List<PartnerReportOrderDistributionDto> getTestData() {
        List<PartnerReportOrderDistributionDto> res = new ArrayList<>();
        for (int i = 0; i < 42; i++) {
            res.add(createOrderDto());
        }
        return res;
    }

    public PartnerReportOrderDistributionDto createOrderDto() {
        return PartnerReportOrderDistributionDto.builder()
                .address("Москва, Кремль, д.1, кв 14")
                .courier(PartnerCourierDto.builder().courierId(123L).courierUid(456L).courierName("Иван Ветров").build())
                .orderId(1L)
                .externalOrderId("ex1")
                .address("aaa")
                .deliveryIntervalFrom(Instant.now())
                .deliveryIntervalTo(Instant.now())
                .paymentType(OrderPaymentType.PREPAID)
                .totalPrice(BigDecimal.valueOf(4560L))
                .orderDeliveryStatus(OrderDeliveryStatus.NOT_DELIVERED)
                .build();
    }
}
