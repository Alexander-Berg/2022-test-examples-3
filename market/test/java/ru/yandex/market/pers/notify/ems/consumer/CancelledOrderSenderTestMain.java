package ru.yandex.market.pers.notify.ems.consumer;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.pers.notify.model.NotificationSubtype;

/**
 * Тесты проверяет отправку писем типа {@link NotificationSubtype#ORDER_CANCELLED} с использованием рассылятора.
 *
 * Проверяются все допустимые подстатусы (по одному тесту на каждый подстатус).
 * В тестах контролируется преобразование подстатуса заказа в подстатус, понимаемый шаблоном рассылятора.
 * Также контролируется отправка закодированного для совместимости с URL (в UTF-8) secretKey и взаимодействие
 * с клиентом checkouter и abo.
 *
 * Для отправки реальных писем с помощью рассылятора надо заменить замокированный бин клиента рассылятора
 * на реальный бин (в файле "mock-market-mailer.xml"), а также заполнить свойства реального бина клиента рассылятора
 * в файле "test-application.properties"
 *
 * @author semin-serg
 */
public class CancelledOrderSenderTestMain extends CancelledOrderSenderTestsCommon {

    @Test
    public void testUNPAID() throws Exception {
        //именно такое обозначение закреплено в шаблоне для OrderSubstatus.USER_NOT_PAID
        //согласно https://st.yandex-team.ru/MARKETMAIL-604#1511365145000
        test(OrderSubstatus.USER_NOT_PAID, "UNPAID");
    }

    @Test
    public void testUSER_UNREACHABLE() throws Exception {
        test(OrderSubstatus.USER_UNREACHABLE, "USER_UNREACHABLE");
    }

    @Test
    public void testUSER_REFUSED_DELIVERY() throws Exception {
        test(OrderSubstatus.USER_REFUSED_DELIVERY, "USER_REFUSED_DELIVERY");
    }

    @Test
    public void testUSER_REFUSED_PRODUCT() throws Exception {
        test(OrderSubstatus.USER_REFUSED_PRODUCT, "USER_REFUSED_PRODUCT");
    }

    @Test
    public void testSHOP_FAILED() throws Exception {
        test(OrderSubstatus.SHOP_FAILED, "SHOP_FAILED");
    }

    @Test
    public void testUSER_REFUSED_QUALITY() throws Exception {
        test(OrderSubstatus.USER_REFUSED_QUALITY, "USER_REFUSED_QUALITY");
    }

    @Test
    public void testREPLACING_ORDER() throws Exception {
        test(OrderSubstatus.REPLACING_ORDER, "REPLACING_ORDER");
    }

    @Test
    public void testPROCESSING_EXPIRED() throws Exception {
        test(OrderSubstatus.PROCESSING_EXPIRED, "PROCESSING_EXPIRED");
    }

    @Test
    public void testPENDING_EXPIRED() throws Exception {
        test(OrderSubstatus.PENDING_EXPIRED, "PENDING_EXPIRED");
    }

    @Test
    public void testSHOP_PENDING_CANCELLED() throws Exception {
        test(OrderSubstatus.SHOP_PENDING_CANCELLED, "SHOP_PENDING_CANCELLED");
    }

    @Test
    public void testPENDING_CANCELLED() throws Exception {
        test(OrderSubstatus.PENDING_CANCELLED, "PENDING_CANCELLED");
    }

    @Test
    public void testUSER_FRAUD() throws Exception {
        test(OrderSubstatus.USER_FRAUD, "USER_FRAUD");
    }


}
