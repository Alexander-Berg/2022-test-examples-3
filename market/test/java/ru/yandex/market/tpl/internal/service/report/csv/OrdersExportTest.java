package ru.yandex.market.tpl.internal.service.report.csv;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.partner.OrderGeneralizedStatus;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderType;
import ru.yandex.market.tpl.api.model.order.partner.PartnerReportOrderDto;
import ru.yandex.market.tpl.api.model.order.partner.PartnerReportOrderParamsDto;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.user.CourierVehicleType;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.service.order.PartnerReportOrderService;
import ru.yandex.market.tpl.internal.controller.TplIntTest;
import ru.yandex.market.tpl.internal.service.report.OrderExcelReportStrategy;
import ru.yandex.market.tpl.internal.service.report.OrdersReportService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Тест для проверки вёрсти печатной формы списка заказов.
 * Сделан для локальной проверки, поэтому помечен {@link Disabled}.
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@TplIntTest
@Slf4j
@Disabled
public class OrdersExportTest {
    private final OrderExcelReportStrategy orderExcelReportStrategy;
    @MockBean
    private PartnerReportOrderService partnerReportOrderService;
    private OrdersReportService ordersReportService;

    @BeforeEach
    public void init() {
        ordersReportService = new OrdersReportService(
                partnerReportOrderService,
                orderExcelReportStrategy
        );

        given(partnerReportOrderService.findAll(
                any(PartnerReportOrderParamsDto.class)
                )
        )
                .willReturn(getTestData());
    }

    @Disabled
    @Test
    @DisplayName("Тест для отладки верстки отчета списка заказов")
    public void getOrdersReport() throws IOException {
        String path = System.getProperty("user.home") + "/test/orders" + Instant.now() + ".xlsx";
        log.info(String.format("Тестовый отчёт списка заказов %s", path));
        FileOutputStream fos = new FileOutputStream(path);

        ordersReportService.getOrdersReportExcel(fos, new PartnerReportOrderParamsDto());

        fos.flush();
        fos.close();
    }

    @Disabled
    @Test
    @DisplayName("Тест для отладки верстки отчета списка заказов. Client Duration null or less 0.")
    public void getOrdersReportWithNullDuration() throws IOException {
        List<PartnerReportOrderDto> testData = getTestData();
        PartnerReportOrderDto dtoWithZeroDuration = createOrderDto();
        dtoWithZeroDuration.setExtraditionOrderToClientDuration(Duration.ofSeconds(-1));
        testData.add(dtoWithZeroDuration);
        testData.forEach(partnerReportOrderDto -> partnerReportOrderDto.setExtraditionOrderToClientDuration(null));
        given(partnerReportOrderService.findAll(
                any(PartnerReportOrderParamsDto.class)
                )
        ).willReturn(testData);
        String path = System.getProperty("user.home") + "/test/orders" + Instant.now() + ".xlsx";
        log.info(String.format("Тестовый отчёт списка заказов %s", path));
        FileOutputStream fos = new FileOutputStream(path);

        ordersReportService.getOrdersReportExcel(fos, new PartnerReportOrderParamsDto());

        fos.flush();
        fos.close();
    }

    private List<PartnerReportOrderDto> getTestData() {
        List<PartnerReportOrderDto> res = new ArrayList<>();
        for (int i = 0; i < 42; i++) {
            res.add(createOrderDto());
        }
        return res;
    }

    public PartnerReportOrderDto createOrderDto() {
        return PartnerReportOrderDto.builder()
                .id(987L)
                .address("Москва, Кремль, д.1, кв 14")
                .arrivalTime(LocalDateTime.now())
                .courierId(123L)
                .courierUid(456L)
                .courierName("Иван Ветров")
                .courierCompanyName("ООО Скорость")
                .courierVehicleType(CourierVehicleType.CAR)
                .orderId("external_123")
                .taskStatus("FAILED")
                .currentRoutePoint(false)
                .orderDeliveryStatus(OrderDeliveryStatus.NOT_DELIVERED)
                .taskStatusDescription("В процессе доставки")
                .generalizedTaskStatus("IN_PROCESS")
                .generalizedTaskStatusLocalize(OrderGeneralizedStatus.IN_PROGRESS.getDescription())
                .orderFlowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .failReasonType(OrderDeliveryTaskFailReasonType.DELIVERY_DATE_UPDATED)
                .failComment("Комментарий отмены/переноса")
                .failSource(Source.COURIER)
                .deliveryTime(LocalDateTime.now())
                .deliveryIntervalFrom(DateTimeUtil.DEFAULT_INTERVAL.getStart())
                .deliveryIntervalTo(DateTimeUtil.DEFAULT_INTERVAL.getEnd())
                .deliveryDate(LocalDate.now())
                .shiftDate(LocalDate.now())
                .userShiftId(678L)
                .shiftId(1L)
                .finishedAt(LocalDateTime.now())
                .userShiftStatus(UserShiftStatus.SHIFT_CLOSED)
                .actions(List.of(new OrderDeliveryTaskDto.Action(OrderDeliveryTaskDto.ActionType.REASSIGN)))
                .orderDate(LocalDate.now())
                .sortingCenterId(1)
                .sortingCenterName("МК СЦ Север")
                .deliveryServiceId(2)
                .deliveryServiceName("МК Север")
                .paymentType(OrderPaymentType.PREPAID)
                .totalPrice(BigDecimal.valueOf(4560L))
                .multiOrderId("m_123")
                .isPartOfMultiOrder(false)
                .orderType(PartnerOrderType.CLIENT)
                .routePointId(123L)
                .places(List.of("place_1", "place_2"))
                .isMultiPlace(true)
                .r18(true)
                .build();
    }
}
