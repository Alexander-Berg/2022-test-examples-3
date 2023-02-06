package ru.yandex.market.rg.asyncreport.shipment;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Функциональный тест для {@link FirstMileShipmentOrdersReportGenerator}.
 */
@DbUnitDataSet(before = {"Tanker.csv", "FirstMileShipmentOrdersReportGeneratorTest.before.csv"})
class FirstMileShipmentOrdersReportGeneratorTest extends AbstractFirstMileShipmentReportGeneratorTest {

    @Autowired
    private FirstMileShipmentOrdersReportGenerator reportGenerator;

    @Test
    @DisplayName("Отчет по заказам отгрузки в формате XLS")
    void testShopXls() {
        List<String> expected = List.of(
                "Информация о заказе",
                "Номер заказа;Ваш номер заказа;Дата оформления;Ваш SKU;Название товара;Количество;Ваша цена (за шт.);Статус заказа;Статус изменён;Способ оплаты;Склад отгрузки;Дата отгрузки;Грузоместа;Регион доставки",
                "56;myBestOrderEver12321;27.11.2020;ssku1;Самый лучший товар;1;1;Подтверждается;27.12.2020;;;02.01.2021;2;",
                "56;myBestOrderEver12321;27.11.2020;ssku2;Не самый лучший товар;1000;10;Подтверждается;27.12.2020;;;02.01.2021;2;"
        );

        mockNesuClient();
        mockCheckouterClient();

        checkReport("full", 101L, reportGenerator, inputStream -> xlsCheck(expected, inputStream, ";"));

        ArgumentCaptor<OrderSearchRequest> captor = ArgumentCaptor.forClass(OrderSearchRequest.class);
        Mockito.verify(checkouterClient, Mockito.times(1)).getOrders(Mockito.any(), captor.capture());
        final OrderSearchRequest searchRequest = captor.getValue();
        Assertions.assertEquals(searchRequest.rgbs, Set.of(Color.BLUE));
        Assertions.assertEquals(searchRequest.orderIds, List.of(56L));
    }

}
