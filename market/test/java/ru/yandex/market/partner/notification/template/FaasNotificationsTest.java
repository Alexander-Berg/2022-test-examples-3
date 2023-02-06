package ru.yandex.market.partner.notification.template;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.market.partner.notification.service.NotificationSendContext;
import ru.yandex.market.partner.notification.service.NotificationService;
import ru.yandex.market.partner.notification.service.providers.data.NotificationSupplierInfo;

/**
 * Тесты шаблонов уведомлений о заказах, созданных на внешних площадках
 */
public class FaasNotificationsTest extends AbstractFunctionalTest {
    private static final int ORDER_CREATION_FAILED_TEMPLATE_ID = 1653481694;
    private static final int ORDER_UNREDEEMED_TEMPLATE_ID = 1653660676;
    private static final int ORDER_CANCELLATION_FAILED_TEMPLATE_ID = 1653662200;
    private static final int ORDER_DELIVERED_TEMPLATE_ID = 1653662730;

    @Autowired
    NotificationService notificationService;

    @Test
    @DisplayName("Verifying template 1653481694 - order creation failed")
    @DbUnitDataSet(after = "faas/order.creation.failed.after.csv")
    void testOrderCreationFailed() {

        List<Object> data = new ArrayList<>();
        data.add(new Order(1000000000090L, "023a7235", "2.05.2022 15:45"));
        data.add(new NotificationSupplierInfo(
                777L,
                123456L,
                "Очень длинноластая русалочка"
        ));

        var ctx = new NotificationSendContext.Builder()
                .setTypeId(ORDER_CREATION_FAILED_TEMPLATE_ID)
                .setData(data)
                .setRecepientEmail("merchant@somemail.ru")
                .setRenderOnly(true)
                .build();

        notificationService.send(ctx);
    }

    @Test
    @DisplayName("Verifying template 1653660676 - order unredeemed")
    @DbUnitDataSet(after = "faas/order.unredeemed.after.csv")
    void testOrderUnredeemed() {

        List<Object> data = new ArrayList<>();
        data.add(new Order(1000000000090L, "023a7235", "2.05.2022 15:45"));
        data.add(new NotificationSupplierInfo(
                777L,
                123456L,
                "Очень длинноластая русалочка"
        ));

        var ctx = new NotificationSendContext.Builder()
                .setTypeId(ORDER_UNREDEEMED_TEMPLATE_ID)
                .setData(data)
                .setRecepientEmail("merchant@somemail.ru")
                .setRenderOnly(true)
                .build();

        notificationService.send(ctx);
    }

    @Test
    @DisplayName("Verifying template 1653662730 - order delivered")
    @DbUnitDataSet(after = "faas/order.delivered.after.csv")
    void testOrderDelivered() {

        List<Object> data = new ArrayList<>();
        data.add(new Order(1000000000090L, "023a7235", "2.05.2022 15:45"));
        data.add(new NotificationSupplierInfo(
                777L,
                123456L,
                "Очень длинноластая русалочка"
        ));

        var ctx = new NotificationSendContext.Builder()
                .setTypeId(ORDER_DELIVERED_TEMPLATE_ID)
                .setData(data)
                .setRecepientEmail("merchant@somemail.ru")
                .setRenderOnly(true)
                .build();

        notificationService.send(ctx);
    }

    @Test
    @DisplayName("Verifying template 1653662200 - order cancellation failed")
    @DbUnitDataSet(after = "faas/order.cancellation.failed.after.csv")
    void testOrderCancellationFailed() {

        List<Object> data = new ArrayList<>();
        data.add(new Order(1000000000090L, "023a7235", "2.05.2022 15:45"));
        data.add(new NotificationSupplierInfo(
                777L,
                123456L,
                "Очень длинноластая русалочка"
        ));

        var ctx = new NotificationSendContext.Builder()
                .setTypeId(ORDER_CANCELLATION_FAILED_TEMPLATE_ID)
                .setData(data)
                .setRecepientEmail("merchant@somemail.ru")
                .setRenderOnly(true)
                .build();

        notificationService.send(ctx);
    }

    public static final class Order implements Serializable {
        private final long id;
        private final String shopOrderId;
        private final String deliveryDate;

        public Order(long id, String shopOrderId, String deliveryDate) {
            this.id = id;
            this.shopOrderId = shopOrderId;
            this.deliveryDate = deliveryDate;
        }

        public long getId() {
            return id;
        }

        public String getShopOrderId() {
            return shopOrderId;
        }

        public String getDeliveryDate() {
            return deliveryDate;
        }
    }
}
