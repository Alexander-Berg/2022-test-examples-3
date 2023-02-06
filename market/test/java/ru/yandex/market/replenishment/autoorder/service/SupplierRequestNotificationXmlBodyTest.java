package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;

import static org.junit.Assert.assertEquals;

public class SupplierRequestNotificationXmlBodyTest extends FunctionalTest {

    @Autowired
    SupplierRequestNotificationService supplierRequestNotificationService;

    @Test
    public void testSupplierRequestNotificationXmlBody() {
        String xmlBody = supplierRequestNotificationService.getXmlBody(
                123,
                "333",
                "234",
                LocalDate.of(2020, 5, 15),
                LocalDate.of(2020, 5, 20),
                "https://partner-test.market.yandex.ru",
                "TestShop",
                10,
                1234,
                "Яндекс.Маркет (Тестовый склад)"
        );
        assertEquals(TestUtils.readResource("/xml-template/expected-message-to-shop.xml"), xmlBody);
    }
}
