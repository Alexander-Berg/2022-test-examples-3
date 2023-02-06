package ru.yandex.market.abo.util.pdf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.test.TestHelper;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;

/**
 * @author artemmz
 * @date 13/08/19.
 */
class OrderToPdfExporterTest extends EmptyTest {
    @Autowired
    private OrderToPdfExporter orderToPdfExporter;

    @Test
    @Disabled("ya make test fails due to some font issues - use idea when needed")
    void generatePdf() throws IOException {
        File tempFile = File.createTempFile("order-report", ".pdf");
        orderToPdfExporter.generateInventoryPdf(initOrder(), new FileOutputStream(tempFile));
        tempFile.deleteOnExit();
    }

    private static Order initOrder() {
        Order order = TestHelper.generateOrder(100500L);
        OrderItem otherItem = TestHelper.generateItem();
        otherItem.setCount(2);
        order.addItem(otherItem);
        return order;
    }
}