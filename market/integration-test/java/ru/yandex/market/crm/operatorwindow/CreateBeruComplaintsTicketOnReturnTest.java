package ru.yandex.market.crm.operatorwindow;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.InternalComment;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.TicketCategory;
import ru.yandex.market.jmf.module.ticket.TicketTag;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.jmf.trigger.impl.TriggerServiceImpl;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.ocrm.module.complaints.BeruComplaintsTicket;
import ru.yandex.market.ocrm.module.order.OrderReturnSource;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.domain.OrderDeliveryType;
import ru.yandex.market.ocrm.module.order.domain.OrderItem;
import ru.yandex.market.ocrm.module.order.domain.OrderReturnReason;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;

@Transactional
public class CreateBeruComplaintsTicketOnReturnTest extends AbstractBeruComplaintsMailProcessingTest {

    private static final String VIP_TAG = "vip";
    private static final String YANDEX_TEAM_TAG = "yandexTeam";

    private static final Long TEST_ORDER_NUMBER = Randoms.positiveLongValue();
    private static final Long TEST_ORDER_RETURN_ID = Randoms.positiveLongValue();
    private static final String TEST_YANDEX_TEAM_EMAIL = "test@yandex-team.ru";
    private static final String TEST_BUYER_EMAIL = Randoms.email();
    private static final String TEST_BUYER_FULL_NAME = Randoms.string();
    private static final String TEST_BUYER_PHONE = Randoms.phoneNumber();
    private static final String TEST_CLIENT_EMAIL = Randoms.email();
    private static final String TEST_CLIENT_FULL_NAME = Randoms.string();
    private static final String TEST_CERTIFICATE_OF_INTEREST_PAID = Randoms.hex(8);
    private static final String TEST_COMMENT = Randoms.string();
    private static final Long DEFAULT_SERVICE_PRIORITY = 40L;
    private static final Long VIP_SERVICE_PRIORITY = 70L;

    private static final Long ORDER_ITEM_ID_1 = Randoms.positiveLongValue();
    private static final String ORDER_ITEM_TITLE_1 = Randoms.string();
    private static final String ORDER_ITEM_RETURN_REASON_1 = Randoms.string();
    private static final String ORDER_ITEM_PICTURE_URL_1 = Randoms.url();
    private static final String ORDER_ITEM_PICTURE_URL_2 = Randoms.url();
    private static final Long ORDER_ITEM_ID_2 = ORDER_ITEM_ID_1 + 1;
    private static final String ORDER_ITEM_TITLE_2 = Randoms.string();
    private static final String ORDER_ITEM_RETURN_REASON_2 = Randoms.string();

    @Inject
    protected ServiceTimeTestUtils serviceTimeTestUtils;
    @Inject
    private DbService dbService;
    @Inject
    private BcpService bcpService;
    @Inject
    private TriggerServiceImpl triggerService;
    @Inject
    private OrderTestUtils orderTestUtils;
    @Inject
    private OrderReturnSource orderReturnSource;

    private Order order;

    @Test
    @Disabled("FIXME")
    public void test1() {
        doTest(
                new TestOrder()
                        .setDropshipping(false)
                        .setPaymentType(PaymentType.PREPAID.name())
                        .setPaymentMethod(PaymentMethod.APPLE_PAY.name()),
                new TestOrderReturn()
                        .setCertificateOfInterestPaidUrl("http://" + TEST_CERTIFICATE_OF_INTEREST_PAID + ".com")
                        .setComment(TEST_COMMENT)
                        .setFullName(TEST_CLIENT_FULL_NAME)
                        .setUserEmail(TEST_CLIENT_EMAIL)
                        .setFirstItem(ReturnReasonType.BAD_QUALITY)
                        .setSecondItem(ReturnReasonType.WRONG_ITEM),
                new ExpectedTicket()
                        .setTitle("Возврат нескольких товаров из ЛК, не_ДШ, Apple/Google Pay - заказ "
                                + TEST_ORDER_NUMBER)
                        .setClientName(TEST_CLIENT_FULL_NAME)
                        .setClientEmail(TEST_CLIENT_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_INCOMING)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(DEFAULT_SERVICE_PRIORITY)
                        .setTags(Set.of())
                        .setCategories(Set.of("beruComplaintsBadQuality", "beruComplaintsWrongItem"))
                        .setComment(String.format("" +
                                        "Заявление на возврат: <a target=\"_blank\" " +
                                        "href=\"/api/order/%d/returns/%d/pdf\" " +
                                        "rel=\"noopener noreferrer\">скачать</a><br />\n" + "Справка о " +
                                        "выплаченных " +
                                        "процентах по кредиту: <a target=\"_blank\" href=\"https://sba.yandex" +
                                        ".net/redirect?url&#61;http%%3A%%2F%%2F%s.com&amp;client&#61;" +
                                        "test&amp;" +
                                        "sign&#61;sign\" rel=\"noopener noreferrer\">скачать</a><br />\n"
                                        + "Комментарий: %s<br />\n"
                                        + "Товары\n"
                                        + "<ul><li>%s\n"
                                        + "  <ul><li>Тип возврата: Есть недостатки</li><li>Причина возврата " +
                                        "товара: " +
                                        "%s</li></ul>\n"
                                        + "</li><li>%s\n"
                                        + "  <ul><li>Тип возврата: Привезли не то</li><li>Причина возврата " +
                                        "товара: " +
                                        "%s</li></ul>\n"
                                        + "</li></ul>",
                                TEST_ORDER_NUMBER, TEST_ORDER_RETURN_ID, TEST_CERTIFICATE_OF_INTEREST_PAID,
                                TEST_COMMENT, ORDER_ITEM_TITLE_1, ORDER_ITEM_RETURN_REASON_1,
                                ORDER_ITEM_TITLE_2,
                                ORDER_ITEM_RETURN_REASON_2))
        );
    }

    @Test
    public void test2() {
        doTest(
                new TestOrder()
                        .setDropshipping(true)
                        .setPaymentType(PaymentType.PREPAID.name())
                        .setPaymentMethod(PaymentMethod.GOOGLE_PAY.name())
                        .setDeliveryTypeCode("testDeliveryType"),
                new TestOrderReturn()
                        .setComment(null)
                        .setFullName(TEST_CLIENT_FULL_NAME)
                        .setUserEmail(TEST_CLIENT_EMAIL)
                        .setFirstItem(ReturnReasonType.BAD_QUALITY),
                new ExpectedTicket()
                        .setTitle("Возврат товара из ЛК, Есть недостатки, ДШ, Apple/Google Pay - заказ " + TEST_ORDER_NUMBER)
                        .setClientName(TEST_CLIENT_FULL_NAME)
                        .setClientEmail(TEST_CLIENT_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_INCOMING)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(DEFAULT_SERVICE_PRIORITY)
                        .setTags(Set.of())
                        .setCategories(Set.of("beruComplaintsBadQuality"))
                        .setComment(String.format("" +
                                        "Заявление на возврат: <a target=\"_blank\" " +
                                        "href=\"/api/order/%d/returns/%d/pdf\" " +
                                        "rel=\"noopener noreferrer\">скачать</a><br />\n\n"
                                        + "Комментарий: <br />\n"
                                        + "Товары\n"
                                        + "<ul><li>%s\n"
                                        + "  <ul><li>Тип возврата: Есть недостатки</li><li>Причина возврата " +
                                        "товара: " +
                                        "%s</li><li>Способ доставки: " +
                                        "testDeliveryType title</li></ul>\n"
                                        + "</li></ul>",
                                TEST_ORDER_NUMBER, TEST_ORDER_RETURN_ID, ORDER_ITEM_TITLE_1,
                                ORDER_ITEM_RETURN_REASON_1
                        ))
        );
    }

    @Test
    @Disabled("FIXME")
    public void test3() {
        doTest(new TestOrder()
                        .setDropshipping(true)
                        .setPaymentType(PaymentType.PREPAID.name())
                        .setPaymentMethod(PaymentMethod.GOOGLE_PAY.name()),
                new TestOrderReturn()
                        .setComment(null)
                        .setFullName(TEST_CLIENT_FULL_NAME)
                        .setUserEmail(TEST_CLIENT_EMAIL)
                        .setFirstItemWithPictures(ReturnReasonType.BAD_QUALITY),
                new ExpectedTicket()
                        .setTitle("Возврат товара из ЛК, Есть недостатки, ДШ, Apple/Google Pay - заказ " + TEST_ORDER_NUMBER)
                        .setClientName(TEST_CLIENT_FULL_NAME)
                        .setClientEmail(TEST_CLIENT_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_INCOMING)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(DEFAULT_SERVICE_PRIORITY)
                        .setTags(Set.of())
                        .setCategories(Set.of("beruComplaintsBadQuality"))
                        .setComment(String.format(""
                                        + "Заявление на возврат: <a "
                                        + "target=\"_blank\" href=\"/api/order/%d/returns/%d/pdf\" " +
                                        "rel=\"noopener" +
                                        " " +
                                        "noreferrer\">скачать</a><br />\n\n"
                                        + "Комментарий: <br />\n"
                                        + "Товары\n"
                                        + "<ul><li>%s\n"
                                        + "  <ul>"
                                        + "<li>Тип возврата: Есть недостатки</li>"
                                        + "<li>Причина возврата товара: %s</li>"
                                        + "<li>Фото: <a target=\"_blank\" href=\"https://sba.yandex" +
                                        ".net/redirect?url&#61;%s&amp;client&#61;test&amp;" +
                                        "sign&#61;sign\" rel=\"noopener noreferrer\">скачать</a> <a " +
                                        "target=\"_blank\"" +
                                        " href=\"https://sba.yandex.net/redirect?url&#61;%s&amp;client&#61;" +
                                        "test&amp;sign&#61;sign\" rel=\"noopener " +
                                        "noreferrer\">скачать</a></li>"
                                        + "</ul>\n"
                                        + "</li>"
                                        + "</ul>",
                                TEST_ORDER_NUMBER, TEST_ORDER_RETURN_ID, ORDER_ITEM_TITLE_1,
                                ORDER_ITEM_RETURN_REASON_1,
                                URLEncoder.encode(ORDER_ITEM_PICTURE_URL_1, StandardCharsets.UTF_8),
                                URLEncoder.encode(ORDER_ITEM_PICTURE_URL_2, StandardCharsets.UTF_8)
                        ))
        );
    }

    @Test
    public void test4() {
        doTest(
                new TestOrder()
                        .setDropshipping(false)
                        .setPaymentType(PaymentType.PREPAID.name())
                        .setPaymentMethod(PaymentMethod.CREDIT.name()),
                new TestOrderReturn()
                        .setComment(TEST_COMMENT)
                        .setUserEmail(TEST_CLIENT_EMAIL)
                        .setFirstItem(ReturnReasonType.BAD_QUALITY),
                new ExpectedTicket()
                        .setTitle("Возврат товара из ЛК, Есть недостатки, не_ДШ, кредит - заказ " +
                                TEST_ORDER_NUMBER)
                        .setClientName(TEST_BUYER_FULL_NAME)
                        .setClientEmail(TEST_CLIENT_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_INCOMING)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(DEFAULT_SERVICE_PRIORITY)
                        .setTags(Set.of())
                        .setCategories(Set.of("beruComplaintsBadQuality"))
        );
    }

    @Test
    public void test5() {
        doTest(
                new TestOrder()
                        .setDropshipping(true)
                        .setPaymentType(PaymentType.PREPAID.name())
                        .setPaymentMethod(PaymentMethod.CREDIT.name()),
                new TestOrderReturn()
                        .setComment(TEST_COMMENT)
                        .setFullName(TEST_CLIENT_FULL_NAME)
                        .setFirstItem(ReturnReasonType.BAD_QUALITY),
                new ExpectedTicket()
                        .setTitle("Возврат товара из ЛК, Есть недостатки, ДШ, кредит - заказ " +
                                TEST_ORDER_NUMBER)
                        .setClientName(TEST_CLIENT_FULL_NAME)
                        .setClientEmail(TEST_BUYER_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_INCOMING)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(DEFAULT_SERVICE_PRIORITY)
                        .setTags(Set.of())
                        .setCategories(Set.of("beruComplaintsBadQuality"))
        );
    }

    @Test
    public void test6() {
        doTest(
                new TestOrder()
                        .setDropshipping(false)
                        .setPaymentType(PaymentType.PREPAID.name())
                        .setPaymentMethod(PaymentMethod.YANDEX.name()),
                new TestOrderReturn()
                        .setComment(TEST_COMMENT)
                        .setUserEmail(VIP_EMAIL)
                        .setFirstItem(ReturnReasonType.BAD_QUALITY),
                new ExpectedTicket()
                        .setTitle("Возврат товара из ЛК, Есть недостатки, не_ДШ, ПРЕДоплата - заказ " +
                                TEST_ORDER_NUMBER)
                        .setClientName(TEST_BUYER_FULL_NAME)
                        .setClientEmail(VIP_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_VIP)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(VIP_SERVICE_PRIORITY + 25)
                        .setTags(Set.of(VIP_TAG))
                        .setCategories(Set.of("beruComplaintsBadQuality"))
        );
    }

    @Test
    public void test7() {
        doTest(
                new TestOrder()
                        .setDropshipping(true)
                        .setPaymentType(PaymentType.PREPAID.name())
                        .setPaymentMethod(PaymentMethod.YANDEX.name())
                        .setBuyerEmail(VIP_EMAIL),
                new TestOrderReturn()
                        .setComment(TEST_COMMENT)
                        .setUserEmail(TEST_CLIENT_EMAIL)
                        .setFirstItem(ReturnReasonType.BAD_QUALITY),
                new ExpectedTicket()
                        .setTitle("Возврат товара из ЛК, Есть недостатки, ДШ, ПРЕДоплата - заказ " +
                                TEST_ORDER_NUMBER)
                        .setClientName(TEST_BUYER_FULL_NAME)
                        .setClientEmail(TEST_CLIENT_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_VIP)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(VIP_SERVICE_PRIORITY + 25)
                        .setTags(Set.of(VIP_TAG))
                        .setCategories(Set.of("beruComplaintsBadQuality"))
        );
    }

    @Test
    public void test8() {
        doTest(
                new TestOrder()
                        .setDropshipping(false)
                        .setPaymentType(PaymentType.POSTPAID.name())
                        .setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY.name()),
                new TestOrderReturn()
                        .setComment(TEST_COMMENT)
                        .setUserEmail(TEST_YANDEX_TEAM_EMAIL)
                        .setFirstItem(ReturnReasonType.BAD_QUALITY),
                new ExpectedTicket()
                        .setTitle("Возврат товара из ЛК, Есть недостатки, не_ДШ, ПОСТоплата - заказ " +
                                TEST_ORDER_NUMBER)
                        .setClientName(TEST_BUYER_FULL_NAME)
                        .setClientEmail(TEST_YANDEX_TEAM_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_VIP)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(VIP_SERVICE_PRIORITY + 5)
                        .setTags(Set.of(YANDEX_TEAM_TAG))
                        .setCategories(Set.of("beruComplaintsBadQuality"))
        );
    }

    @Test
    public void test9() {
        doTest(
                new TestOrder()
                        .setDropshipping(true)
                        .setPaymentType(PaymentType.POSTPAID.name())
                        .setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY.name())
                        .setBuyerEmail(TEST_YANDEX_TEAM_EMAIL),
                new TestOrderReturn()
                        .setComment(TEST_COMMENT)
                        .setUserEmail(TEST_CLIENT_EMAIL)
                        .setFirstItem(ReturnReasonType.BAD_QUALITY),
                new ExpectedTicket()
                        .setTitle("Возврат товара из ЛК, Есть недостатки, ДШ, ПОСТоплата - заказ " +
                                TEST_ORDER_NUMBER)
                        .setClientName(TEST_BUYER_FULL_NAME)
                        .setClientEmail(TEST_CLIENT_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_VIP)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(VIP_SERVICE_PRIORITY + 5)
                        .setTags(Set.of(YANDEX_TEAM_TAG))
                        .setCategories(Set.of("beruComplaintsBadQuality"))
        );
    }

    @Test
    public void test10() {
        doTest(
                new TestOrder()
                        .setDropshipping(false)
                        .setPaymentType(PaymentType.PREPAID.name())
                        .setPaymentMethod(PaymentMethod.GOOGLE_PAY.name()),
                new TestOrderReturn()
                        .setComment(TEST_COMMENT)
                        .setFirstItem(ReturnReasonType.DO_NOT_FIT),
                new ExpectedTicket()
                        .setTitle("Возврат товара из ЛК, Не подошел, не_ДШ, Apple/Google Pay - заказ " +
                                TEST_ORDER_NUMBER)
                        .setClientName(TEST_BUYER_FULL_NAME)
                        .setClientEmail(TEST_BUYER_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_INCOMING)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(DEFAULT_SERVICE_PRIORITY)
                        .setTags(Set.of())
                        .setCategories(Set.of("beruComplaintsDoNotFit"))
        );
    }

    @Test
    public void test11() {
        doTest(
                new TestOrder()
                        .setDropshipping(true)
                        .setPaymentType(PaymentType.PREPAID.name())
                        .setPaymentMethod(PaymentMethod.APPLE_PAY.name()),
                new TestOrderReturn()
                        .setComment(TEST_COMMENT)
                        .setFirstItem(ReturnReasonType.DO_NOT_FIT),
                new ExpectedTicket()
                        .setTitle("Возврат товара из ЛК, Не подошел, ДШ, Apple/Google Pay - заказ " +
                                TEST_ORDER_NUMBER)
                        .setClientName(TEST_BUYER_FULL_NAME)
                        .setClientEmail(TEST_BUYER_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_INCOMING)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(DEFAULT_SERVICE_PRIORITY)
                        .setTags(Set.of())
                        .setCategories(Set.of("beruComplaintsDoNotFit"))
        );
    }

    @Test
    public void test12() {
        doTest(
                new TestOrder()
                        .setDropshipping(false)
                        .setPaymentType(PaymentType.PREPAID.name())
                        .setPaymentMethod(PaymentMethod.CREDIT.name()),
                new TestOrderReturn()
                        .setComment(TEST_COMMENT)
                        .setUserEmail(TEST_CLIENT_EMAIL)
                        .setFirstItem(ReturnReasonType.WRONG_ITEM),
                new ExpectedTicket()
                        .setTitle("Возврат товара из ЛК, Привезли не то, не_ДШ, кредит - заказ " +
                                TEST_ORDER_NUMBER)
                        .setClientName(TEST_BUYER_FULL_NAME)
                        .setClientEmail(TEST_CLIENT_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_INCOMING)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(DEFAULT_SERVICE_PRIORITY)
                        .setTags(Set.of())
                        .setCategories(Set.of("beruComplaintsWrongItem"))
        );
    }


    @Test
    public void test13() {
        doTest(
                new TestOrder()
                        .setDropshipping(true)
                        .setPaymentType(PaymentType.PREPAID.name())
                        .setPaymentMethod(PaymentMethod.CREDIT.name()),
                new TestOrderReturn()
                        .setComment(TEST_COMMENT)
                        .setFullName(TEST_CLIENT_FULL_NAME)
                        .setFirstItem(ReturnReasonType.WRONG_ITEM),
                new ExpectedTicket()
                        .setTitle("Возврат товара из ЛК, Привезли не то, ДШ, кредит - заказ " +
                                TEST_ORDER_NUMBER)
                        .setClientName(TEST_CLIENT_FULL_NAME)
                        .setClientEmail(TEST_BUYER_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_INCOMING)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(DEFAULT_SERVICE_PRIORITY)
                        .setTags(Set.of())
                        .setCategories(Set.of("beruComplaintsWrongItem"))
        );
    }

    @Test
    public void test14() {
        doTest(
                new TestOrder()
                        .setDropshipping(true)
                        .setPaymentType(PaymentType.UNKNOWN.name())
                        .setPaymentMethod(PaymentMethod.UNKNOWN.name()),
                new TestOrderReturn()
                        .setComment(TEST_COMMENT)
                        .setFullName(TEST_CLIENT_FULL_NAME)
                        .setFirstItem(ReturnReasonType.WRONG_ITEM),
                new ExpectedTicket()
                        .setTitle("Возврат товара из ЛК, Привезли не то, ДШ - заказ " + TEST_ORDER_NUMBER)
                        .setClientName(TEST_CLIENT_FULL_NAME)
                        .setClientEmail(TEST_BUYER_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_INCOMING)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(DEFAULT_SERVICE_PRIORITY)
                        .setTags(Set.of())
                        .setCategories(Set.of("beruComplaintsWrongItem"))
        );
    }

    @Test
    public void test15() {
        doTest(
                new TestOrder()
                        .setDropshipping(true)
                        .setPaymentType(PaymentType.PREPAID.name())
                        .setPaymentMethod(PaymentMethod.CREDIT.name()),
                new TestOrderReturn()
                        .setComment(TEST_COMMENT)
                        .setFullName(TEST_CLIENT_FULL_NAME),
                new ExpectedTicket()
                        .setTitle("Возврат товара из ЛК - заказ " + TEST_ORDER_NUMBER)
                        .setClientName(TEST_CLIENT_FULL_NAME)
                        .setClientEmail(TEST_BUYER_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_INCOMING)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(DEFAULT_SERVICE_PRIORITY)
                        .setTags(Set.of())
                        .setCategories(Set.of())
        );
    }

    @Test
    public void test16() {
        doTest(
                new TestOrder()
                        .setDropshipping(true)
                        .setPaymentType(PaymentType.PREPAID.name())
                        .setPaymentMethod(PaymentMethod.CREDIT.name()),
                new TestOrderReturn()
                        .setComment(TEST_COMMENT)
                        .setFullName(TEST_CLIENT_FULL_NAME)
                        .setFirstItem(ReturnReasonType.UNKNOWN),
                new ExpectedTicket()
                        .setTitle("Возврат товара из ЛК, UNKNOWN, ДШ, кредит - заказ " + TEST_ORDER_NUMBER)
                        .setClientName(TEST_CLIENT_FULL_NAME)
                        .setClientEmail(TEST_BUYER_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_INCOMING)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(DEFAULT_SERVICE_PRIORITY)
                        .setTags(Set.of())
                        .setCategories(Set.of())
        );
    }

    @Test
    public void test17() {
        doTest(
                new TestOrder()
                        .setDropshipping(true)
                        .setPaymentType(PaymentType.PREPAID.name())
                        .setPaymentMethod(PaymentMethod.CREDIT.name()),
                new TestOrderReturn()
                        .setComment(TEST_COMMENT)
                        .setFullName(TEST_CLIENT_FULL_NAME)
                        .setFirstItem(null),
                new ExpectedTicket()
                        .setTitle("Возврат товара из ЛК, ДШ, кредит - заказ " + TEST_ORDER_NUMBER)
                        .setClientName(TEST_CLIENT_FULL_NAME)
                        .setClientEmail(TEST_BUYER_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_INCOMING)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(DEFAULT_SERVICE_PRIORITY)
                        .setTags(Set.of())
                        .setCategories(Set.of())
        );
    }

    @Test
    public void test18_fastReturnToFastReturnStack() {
        doTest(
                new TestOrder()
                        .setPaymentType(PaymentType.PREPAID.name())
                        .setPaymentMethod(PaymentMethod.YANDEX.name()),
                new TestOrderReturn()
                        .setFastReturn(true)
                        .setComment(TEST_COMMENT)
                        .setFullName(TEST_CLIENT_FULL_NAME)
                        .setFirstItem(ReturnReasonType.BAD_QUALITY),
                new ExpectedTicket()
                        .setTitle("Возврат товара из ЛК, Есть недостатки, не_ДШ, ПРЕДоплата - заказ " +
                                TEST_ORDER_NUMBER)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_FAST_RETURN)
                        .setClientName(TEST_CLIENT_FULL_NAME)
                        .setClientEmail(TEST_BUYER_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(DEFAULT_SERVICE_PRIORITY)
                        .setCategories(Set.of("beruComplaintsBadQuality"))
                        .setTags(Set.of())
        );
    }

    @Test
    public void test19_notFastReturnToGeneralStack() {
        doTest(
                new TestOrder()
                        .setPaymentType(PaymentType.PREPAID.name())
                        .setPaymentMethod(PaymentMethod.YANDEX.name()),
                new TestOrderReturn()
                        .setFastReturn(false)
                        .setComment(TEST_COMMENT)
                        .setFullName(TEST_CLIENT_FULL_NAME)
                        .setFirstItem(ReturnReasonType.BAD_QUALITY, 2),
                new ExpectedTicket()
                        .setTitle("Возврат товара из ЛК, Есть недостатки, не_ДШ, ПРЕДоплата - заказ " +
                                TEST_ORDER_NUMBER)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_INCOMING)
                        .setClientName(TEST_CLIENT_FULL_NAME)
                        .setClientEmail(TEST_BUYER_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(DEFAULT_SERVICE_PRIORITY)
                        .setCategories(Set.of("beruComplaintsBadQuality"))
                        .setTags(Set.of())
        );
    }

    @Test
    public void test20_fastReturnWithLargeSizeToFastReturnStack() {
        doTest(
                new TestOrder()
                        .setDropshipping(true)
                        .setPaymentType(PaymentType.PREPAID.name())
                        .setPaymentMethod(PaymentMethod.YANDEX.name()),
                new TestOrderReturn()
                        .setFastReturn(true)
                        .setLargeSize(true)
                        .setComment(TEST_COMMENT)
                        .setFullName(TEST_CLIENT_FULL_NAME)
                        .setFirstItem(ReturnReasonType.WRONG_ITEM),
                new ExpectedTicket()
                        .setTitle("Возврат товара из ЛК, Привезли не то, ДШ, ПРЕДоплата - заказ " + TEST_ORDER_NUMBER)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_FAST_RETURN)
                        .setClientName(TEST_CLIENT_FULL_NAME)
                        .setClientEmail(TEST_BUYER_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(DEFAULT_SERVICE_PRIORITY)
                        .setCategories(Set.of("beruComplaintsWrongItem"))
                        .setTags(Set.of())
        );
    }

    @Test
    public void test21_notFastReturnWithLargeSizeToLargeSizeStack() {
        doTest(
                new TestOrder()
                        .setDropshipping(false)
                        .setPaymentType(PaymentType.PREPAID.name())
                        .setPaymentMethod(PaymentMethod.YANDEX.name()),
                new TestOrderReturn()
                        .setFastReturn(false)
                        .setLargeSize(true)
                        .setComment(TEST_COMMENT)
                        .setFullName(TEST_CLIENT_FULL_NAME)
                        .setFirstItem(ReturnReasonType.WRONG_ITEM),
                new ExpectedTicket()
                        .setTitle("Возврат товара из ЛК, Привезли не то, не_ДШ, ПРЕДоплата - заказ " + TEST_ORDER_NUMBER)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_LARGE_SIZE)
                        .setClientName(TEST_CLIENT_FULL_NAME)
                        .setClientEmail(TEST_BUYER_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(DEFAULT_SERVICE_PRIORITY)
                        .setCategories(Set.of("beruComplaintsWrongItem"))
                        .setTags(Set.of())
        );
    }

    @Test
    public void test22_dsWithNotFastReturnWithoutReturnAddressToNoAddressStack() {
        doTest(
                new TestOrder()
                        .setDropshipping(true)
                        .setPaymentType(PaymentType.PREPAID.name())
                        .setPaymentMethod(PaymentMethod.YANDEX.name()),
                new TestOrderReturn()
                        .setFastReturn(false)
                        .setFirstItemWithoutReturnAddress(ReturnReasonType.BAD_QUALITY)
                        .setComment(TEST_COMMENT)
                        .setFullName(TEST_CLIENT_FULL_NAME)
                        .setSecondItem(ReturnReasonType.WRONG_ITEM),
                new ExpectedTicket()
                        .setTitle("Возврат нескольких товаров из ЛК, ДШ, ПРЕДоплата - заказ " + TEST_ORDER_NUMBER)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_DS_NO_ADDRESS)
                        .setClientName(TEST_CLIENT_FULL_NAME)
                        .setClientEmail(TEST_BUYER_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(DEFAULT_SERVICE_PRIORITY)
                        .setCategories(Set.of("beruComplaintsWrongItem", "beruComplaintsBadQuality"))
                        .setTags(Set.of())
        );
    }

    @Test
    public void test23_dsWithFastReturnWithoutReturnAddressToFastReturnStack() {
        doTest(
                new TestOrder()
                        .setDropshipping(true)
                        .setPaymentType(PaymentType.PREPAID.name())
                        .setPaymentMethod(PaymentMethod.YANDEX.name()),
                new TestOrderReturn()
                        .setFastReturn(true)
                        .setFirstItemWithoutReturnAddress(ReturnReasonType.WRONG_ITEM)
                        .setComment(TEST_COMMENT)
                        .setFullName(TEST_CLIENT_FULL_NAME),
                new ExpectedTicket()
                        .setTitle("Возврат товара из ЛК, Привезли не то, ДШ, ПРЕДоплата - заказ " + TEST_ORDER_NUMBER)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_FAST_RETURN)
                        .setClientName(TEST_CLIENT_FULL_NAME)
                        .setClientEmail(TEST_BUYER_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(DEFAULT_SERVICE_PRIORITY)
                        .setCategories(Set.of("beruComplaintsWrongItem"))
                        .setTags(Set.of())
        );
    }

    @Test
    public void test24_dsWithNotFastReturnWithLargeSizeWithoutReturnAddressToLargeSizeStack() {
        doTest(
                new TestOrder()
                        .setDropshipping(true)
                        .setPaymentType(PaymentType.PREPAID.name())
                        .setPaymentMethod(PaymentMethod.YANDEX.name()),
                new TestOrderReturn()
                        .setFastReturn(false)
                        .setLargeSize(true)
                        .setFirstItemWithoutReturnAddress(ReturnReasonType.BAD_QUALITY)
                        .setComment(TEST_COMMENT)
                        .setFullName(TEST_CLIENT_FULL_NAME)
                        .setSecondItem(ReturnReasonType.BAD_QUALITY),
                new ExpectedTicket()
                        .setTitle("Возврат нескольких товаров из ЛК, ДШ, ПРЕДоплата - заказ " + TEST_ORDER_NUMBER)
                        .setServiceCode(Constants.Service.BERU_COMPLAINTS_LARGE_SIZE)
                        .setClientName(TEST_CLIENT_FULL_NAME)
                        .setClientEmail(TEST_BUYER_EMAIL)
                        .setClientPhone(TEST_BUYER_PHONE)
                        .setOrderNumber(TEST_ORDER_NUMBER)
                        .setPriority(DEFAULT_SERVICE_PRIORITY)
                        .setCategories(Set.of("beruComplaintsBadQuality"))
                        .setTags(Set.of())
        );
    }

    @Test
    public void shouldNotCreateNewTicketIfTicketWithSameReturnIdAlreadyExists() {
        var testOrder = new TestOrder()
                .setDropshipping(true)
                .setPaymentType(PaymentType.PREPAID.name())
                .setPaymentMethod(PaymentMethod.GOOGLE_PAY.name());
        var testOrderReturn = new TestOrderReturn()
                .setComment(TEST_COMMENT)
                .setFullName(TEST_CLIENT_FULL_NAME)
                .setFirstItem(ReturnReasonType.WRONG_ITEM);

        order = createOrder(testOrder);
        Return orderReturn = createOrderReturn(testOrderReturn);
        doReturn(orderReturn).when(orderReturnSource).getReturn(TEST_ORDER_RETURN_ID, false);
        orderTestUtils.fireOrderImportedEvent(order, ClientRole.USER, HistoryEventType.ORDER_RETURN_CREATED,
                TEST_ORDER_RETURN_ID);
        // Ожидаем, что создали тикет
        getSingleOpenedBeruComplaintsTicket();

        // Отправим то же самое событие еще раз
        orderTestUtils.fireOrderImportedEvent(order, ClientRole.USER, HistoryEventType.ORDER_RETURN_CREATED,
                TEST_ORDER_RETURN_ID);
        // не должны создать новый тикет
        getSingleOpenedBeruComplaintsTicket();
    }


    private void doTest(
            TestOrder testOrder,
            TestOrderReturn testOrderReturn,
            ExpectedTicket expectedTicket
    ) {
        // настройка системы
        editReturnReasonTitle(ReturnReasonType.BAD_QUALITY, "Есть недостатки");
        editReturnReasonTitle(ReturnReasonType.DO_NOT_FIT, "Не подошел");
        editReturnReasonTitle(ReturnReasonType.WRONG_ITEM, "Привезли не то");

        // вызов системы
        order = createOrder(testOrder);
        Return orderReturn = createOrderReturn(testOrderReturn);
        // todo https://st.yandex-team.ru/MARKETCHECKOUT-19188
        doReturn(orderReturn).when(orderReturnSource).getReturn(TEST_ORDER_RETURN_ID, false);

        orderTestUtils.fireOrderImportedEvent(order, ClientRole.USER, HistoryEventType.ORDER_RETURN_CREATED,
                TEST_ORDER_RETURN_ID);
        orderTestUtils.mockGetOrderHistory(order, null);


        // проверка утверждений
        BeruComplaintsTicket actual = getSingleOpenedBeruComplaintsTicket();

        assertEquals(expectedTicket.getTitle(), actual.getTitle());
        assertEquals(expectedTicket.getClientName(), actual.getClientName());
        assertEquals(expectedTicket.getClientEmail(), actual.getClientEmail());
        assertEquals(expectedTicket.getClientPhone(), actual.getClientPhone().getMain());
        assertEquals(TEST_ORDER_RETURN_ID.toString(), actual.getReturnId());

        assertNotNull(actual.getService());
        assertEquals(expectedTicket.getServiceCode(), actual.getService().getCode());

        assertNotNull(actual.getOrder());
        assertEquals(expectedTicket.getOrderNumber(), actual.getOrder().getTitle());

        assertEquals(expectedTicket.getPriority(), actual.getPriorityLevel());
        assertEquals(expectedTicket.getTags(), actual.getTags().stream()
                .map(TicketTag::getCode)
                .collect(Collectors.toSet())
        );
        assertEquals(expectedTicket.getCategories(), actual.getCategories().stream()
                .map(TicketCategory::getCode)
                .collect(Collectors.toSet())
        );
        assertSingleComment(actual, expectedTicket.getComment());
    }

    private void editReturnReasonTitle(ReturnReasonType returnReason, String title) {
        OrderReturnReason orderReturnReason = dbService.getByNaturalId(
                OrderReturnReason.FQN, OrderReturnReason.CODE, returnReason.name());
        bcpService.edit(orderReturnReason, Map.of(OrderReturnReason.TITLE, title));
    }

    private void assertSingleComment(Ticket ticket, String expectedBody) {
        List<Comment> comments = dbService.list(Query.of(InternalComment.FQN)
                .withFilters(Filters.eq(Comment.ENTITY, ticket)));
        assertEquals(1, comments.size());
        if (null != expectedBody) {
            Comment comment = comments.get(0);
            assertEquals(expectedBody, comment.getBody());
        }
    }

    private Order createOrder(TestOrder testOrder) {
        Map<String, Object> props = new HashMap<>(Map.of(
                Order.NUMBER, TEST_ORDER_NUMBER,
                Order.PAYMENT_METHOD, testOrder.getPaymentMethod(),
                Order.PAYMENT_TYPE, testOrder.getPaymentType(),
                Order.BUYER_EMAIL, testOrder.getBuyerEmail(),
                Order.BUYER_PHONE, testOrder.getBuyerPhone(),
                Order.BUYER_LAST_NAME, testOrder.getBuyerFullName(),
                Order.BUYER_FIRST_NAME, "",
                Order.BUYER_MIDDLE_NAME, ""
        ));

        Optional.ofNullable(testOrder.getDeliveryTypeCode())
                .map(x -> dbService
                        .list(Query
                                .of(OrderDeliveryType.FQN)
                                .withFilters(Filters.eq(OrderDeliveryType.CODE, x)))
                        .stream()
                        .findFirst()
                        .orElseGet(() -> bcpService.create(OrderDeliveryType.FQN, Map.of(
                                OrderDeliveryType.CODE, testOrder.getDeliveryTypeCode(),
                                OrderDeliveryType.TITLE, testOrder.getDeliveryTypeCode() + " title"
                        ))))
                .ifPresent(x -> props.put(Order.DELIVERY_TYPE, x));
        props.putAll(testOrder.isDropshipping()
                ? getDropshippingOrderProps()
                : getNotDropshippingOrderProps()
        );
        Order order = orderTestUtils.createOrder(props);
        createOrderItem(order, testOrder.isDropshipping(), ORDER_ITEM_ID_1, ORDER_ITEM_TITLE_1);
        createOrderItem(order, testOrder.isDropshipping(), ORDER_ITEM_ID_2, ORDER_ITEM_TITLE_2);
        return order;
    }

    private Map<String, Object> getDropshippingOrderProps() {
        return Maps.of(
                Order.IS_FULFILMENT, Boolean.FALSE,
                Order.DELIVERY_PARTNER_TYPE, DeliveryPartnerType.YANDEX_MARKET
        );
    }

    private Map<String, Object> getNotDropshippingOrderProps() {
        return Maps.of(Order.IS_FULFILMENT, Boolean.TRUE);
    }

    private void createOrderItem(Order order, boolean isDropshipping, Long checkouterId, String title) {
        Map<String, Object> props = new HashMap<>(Map.of(
                OrderItem.CHECKOUTER_ID, checkouterId,
                OrderItem.TITLE, title
        ));
        if (!isDropshipping) {
            props.putAll(Maps.of(
                    OrderItem.AT_SUPPLIER_WAREHOUSE, Boolean.FALSE,
                    OrderItem.SUPPLIER_TYPE, SupplierType.FIRST_PARTY
            ));
        }
        props.putAll(Map.of(
                OrderItem.BUYER_PRICE, new BigDecimal("2001"),
                OrderItem.COUNT, 2
        ));
        orderTestUtils.mockOrderItem(order, props);
    }

    private Return createOrderReturn(TestOrderReturn testOrderReturn) {
        Return orderReturn = new Return();
        orderReturn.setId(TEST_ORDER_RETURN_ID);
        orderReturn.setFullName(testOrderReturn.getFullName());
        orderReturn.setUserEmail(testOrderReturn.getUserEmail());
        orderReturn.setComment(testOrderReturn.getComment());
        orderReturn.setCertificateOfInterestPaidUrl(testOrderReturn.getCertificateOfInterestPaidUrl());
        orderReturn.setItems(testOrderReturn.getItems().stream()
                .map(testItem -> {
                    ReturnItem item = new ReturnItem();
                    item.setItemId(testItem.getItemId());
                    item.setItemTitle(testItem.getItemTitle());
                    item.setReasonType(testItem.getReasonType());
                    item.setReturnReason(testItem.getReturnReason());
                    item.setPicturesUrls(testItem.getPicturesUrls());
                    item.setCount(testItem.getCount());
                    item.setReturnAddressDisplayed(testItem.getReturnAddressDisplayed());
                    return item;
                })
                .collect(Collectors.toList())
        );
        orderReturn.setLargeSize(testOrderReturn.getLargeSize());
        orderReturn.setFastReturn(testOrderReturn.getFastReturn());
        return orderReturn;
    }

    private static class TestOrder {

        private boolean dropshipping;
        private String paymentMethod;
        private String paymentType;
        private String buyerEmail = TEST_BUYER_EMAIL;
        private String buyerPhone = TEST_BUYER_PHONE;
        private String buyerFullName = TEST_BUYER_FULL_NAME;
        private String deliveryTypeCode;

        public boolean isDropshipping() {
            return dropshipping;
        }

        public TestOrder setDropshipping(boolean dropshipping) {
            this.dropshipping = dropshipping;
            return this;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public TestOrder setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public String getPaymentType() {
            return paymentType;
        }

        public TestOrder setPaymentType(String paymentType) {
            this.paymentType = paymentType;
            return this;
        }

        public String getBuyerEmail() {
            return buyerEmail;
        }

        public TestOrder setBuyerEmail(String buyerEmail) {
            this.buyerEmail = buyerEmail;
            return this;
        }

        public String getBuyerPhone() {
            return buyerPhone;
        }

        public TestOrder setBuyerPhone(String buyerPhone) {
            this.buyerPhone = buyerPhone;
            return this;
        }

        public String getBuyerFullName() {
            return buyerFullName;
        }

        public TestOrder setBuyerFullName(String buyerFullName) {
            this.buyerFullName = buyerFullName;
            return this;
        }

        public String getDeliveryTypeCode() {
            return deliveryTypeCode;
        }

        public TestOrder setDeliveryTypeCode(String deliveryTypeCode) {
            this.deliveryTypeCode = deliveryTypeCode;
            return this;
        }

        @Override
        public String toString() {
            return "TestOrder{" +
                    "dropshipping=" + dropshipping +
                    ", paymentMethod='" + paymentMethod + '\'' +
                    ", paymentType='" + paymentType + '\'' +
                    ", buyerEmail='" + buyerEmail + '\'' +
                    ", buyerPhone='" + buyerPhone + '\'' +
                    ", buyerFullName='" + buyerFullName + '\'' +
                    '}';
        }
    }

    private static class TestOrderReturn {

        private final List<TestOrderReturnItem> items = new ArrayList<>();
        private String fullName;
        private String userEmail;
        private String comment;
        private String certificateOfInterestPaidUrl;
        private boolean largeSize;
        private boolean fastReturn;

        public String getFullName() {
            return fullName;
        }

        public TestOrderReturn setFullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public String getUserEmail() {
            return userEmail;
        }

        public TestOrderReturn setUserEmail(String userEmail) {
            this.userEmail = userEmail;
            return this;
        }

        public String getComment() {
            return comment;
        }

        public TestOrderReturn setComment(String comment) {
            this.comment = comment;
            return this;
        }

        public String getCertificateOfInterestPaidUrl() {
            return certificateOfInterestPaidUrl;
        }

        public TestOrderReturn setCertificateOfInterestPaidUrl(String certificateOfInterestPaidUrl) {
            this.certificateOfInterestPaidUrl = certificateOfInterestPaidUrl;
            return this;
        }

        public boolean getLargeSize() {
            return largeSize;
        }

        public TestOrderReturn setLargeSize(boolean largeSize) {
            this.largeSize = largeSize;
            return this;
        }

        public boolean getFastReturn() {
            return fastReturn;
        }

        public TestOrderReturn setFastReturn(boolean fastReturn) {
            this.fastReturn = fastReturn;
            return this;
        }

        public List<TestOrderReturnItem> getItems() {
            return items;
        }

        public TestOrderReturn setFirstItem(ReturnReasonType reasonType) {
            return setFirstItem(reasonType, 1);
        }

        public TestOrderReturn setFirstItem(ReturnReasonType reasonType, int count) {
            TestOrderReturnItem item = new TestOrderReturnItem();
            item.setItemId(ORDER_ITEM_ID_1);
            item.setItemTitle(ORDER_ITEM_TITLE_1);
            item.setReasonType(reasonType);
            item.setReturnReason(ORDER_ITEM_RETURN_REASON_1);
            item.setCount(count);
            items.add(item);
            return this;
        }

        public TestOrderReturn setFirstItemWithoutReturnAddress(ReturnReasonType reasonType) {
            TestOrderReturnItem item = new TestOrderReturnItem();
            item.setItemId(ORDER_ITEM_ID_1);
            item.setItemTitle(ORDER_ITEM_TITLE_1);
            item.setReasonType(reasonType);
            item.setReturnReason(ORDER_ITEM_RETURN_REASON_1);
            item.setReturnAddressDisplayed(false);
            items.add(item);
            return this;
        }

        public TestOrderReturn setFirstItemWithPictures(ReturnReasonType reasonType) {
            TestOrderReturnItem item = new TestOrderReturnItem();
            item.setItemId(ORDER_ITEM_ID_1);
            item.setItemTitle(ORDER_ITEM_TITLE_1);
            item.setReasonType(reasonType);
            item.setReturnReason(ORDER_ITEM_RETURN_REASON_1);
            try {
                item.setPicturesUrls(List.of(new URL(ORDER_ITEM_PICTURE_URL_1), new URL(ORDER_ITEM_PICTURE_URL_2)));
            } catch (MalformedURLException malformedURLException) {
                throw new RuntimeException(malformedURLException);
            }
            items.add(item);
            return this;
        }

        public TestOrderReturn setSecondItem(ReturnReasonType reasonType) {
            TestOrderReturnItem item = new TestOrderReturnItem();
            item.setItemId(ORDER_ITEM_ID_2);
            item.setReasonType(reasonType);
            item.setReturnReason(ORDER_ITEM_RETURN_REASON_2);
            items.add(item);
            return this;
        }

        @Override
        public String toString() {
            return "TestOrderReturn{" +
                    "fullName='" + fullName + '\'' +
                    ", userEmail='" + userEmail + '\'' +
                    ", comment='" + comment + '\'' +
                    ", certificateOfInterestPaidUrl='" + certificateOfInterestPaidUrl + '\'' +
                    ", items=" + items +
                    ", largeSize=" + largeSize +
                    ", fastReturn=" + fastReturn +
                    '}';
        }
    }

    private static class TestOrderReturnItem {

        private Long itemId;
        private String itemTitle;
        private ReturnReasonType reasonType;
        private String returnReason;
        private List<URL> picturesUrls;
        private int count = 1;
        private Boolean returnAddressDisplayed;

        public Long getItemId() {
            return itemId;
        }

        public TestOrderReturnItem setItemId(Long itemId) {
            this.itemId = itemId;
            return this;
        }

        public String getItemTitle() {
            return itemTitle;
        }

        public TestOrderReturnItem setItemTitle(String itemTitle) {
            this.itemTitle = itemTitle;
            return this;
        }

        public ReturnReasonType getReasonType() {
            return reasonType;
        }

        public TestOrderReturnItem setReasonType(ReturnReasonType reasonType) {
            this.reasonType = reasonType;
            return this;
        }

        public String getReturnReason() {
            return returnReason;
        }

        public TestOrderReturnItem setReturnReason(String returnReason) {
            this.returnReason = returnReason;
            return this;
        }

        public List<URL> getPicturesUrls() {
            return picturesUrls;
        }

        public TestOrderReturnItem setPicturesUrls(List<URL> picturesUrls) {
            this.picturesUrls = picturesUrls;
            return this;
        }

        public int getCount() {
            return count;
        }

        public TestOrderReturnItem setCount(int count) {
            this.count = count;
            return this;
        }

        public Boolean getReturnAddressDisplayed() {
            return returnAddressDisplayed;
        }

        public TestOrderReturnItem setReturnAddressDisplayed(Boolean returnAddressDisplayed) {
            this.returnAddressDisplayed = returnAddressDisplayed;
            return this;
        }

        @Override
        public String toString() {
            return "TestOrderReturnItem{" +
                    "itemId=" + itemId +
                    ", itemTitle='" + itemTitle + '\'' +
                    ", reasonType=" + reasonType +
                    ", returnReason='" + returnReason + '\'' +
                    ", picturesUrls=" + picturesUrls +
                    ", count=" + count +
                    ", returnAddressDisplayed=" + returnAddressDisplayed +
                    '}';
        }
    }

    private static class ExpectedTicket {

        private String title;
        private String clientName;
        private String clientEmail;
        private String clientPhone;
        private String serviceCode;
        private Long orderNumber;
        private Long priority;
        private Set<String> tags;
        private Set<String> categories;
        private String comment;

        public String getTitle() {
            return title;
        }

        public ExpectedTicket setTitle(String title) {
            this.title = title;
            return this;
        }

        public String getClientName() {
            return clientName;
        }

        public ExpectedTicket setClientName(String clientName) {
            this.clientName = clientName;
            return this;
        }

        public String getClientEmail() {
            return clientEmail;
        }

        public ExpectedTicket setClientEmail(String clientEmail) {
            this.clientEmail = clientEmail;
            return this;
        }

        public String getClientPhone() {
            return clientPhone;
        }

        public ExpectedTicket setClientPhone(String clientPhone) {
            this.clientPhone = clientPhone;
            return this;
        }

        public String getServiceCode() {
            return serviceCode;
        }

        public ExpectedTicket setServiceCode(String serviceCode) {
            this.serviceCode = serviceCode;
            return this;
        }

        public Long getOrderNumber() {
            return orderNumber;
        }

        public ExpectedTicket setOrderNumber(Long orderNumber) {
            this.orderNumber = orderNumber;
            return this;
        }

        public Long getPriority() {
            return priority;
        }

        public ExpectedTicket setPriority(Long priority) {
            this.priority = priority;
            return this;
        }

        public Set<String> getTags() {
            return tags;
        }

        public ExpectedTicket setTags(Set<String> tags) {
            this.tags = tags;
            return this;
        }

        public Set<String> getCategories() {
            return categories;
        }

        public ExpectedTicket setCategories(Set<String> categories) {
            this.categories = categories;
            return this;
        }

        public String getComment() {
            return comment;
        }

        public ExpectedTicket setComment(String comment) {
            this.comment = comment;
            return this;
        }

        @Override
        public String toString() {
            return "ExpectedTicket{" +
                    "title='" + title + '\'' +
                    ", clientName='" + clientName + '\'' +
                    ", clientEmail='" + clientEmail + '\'' +
                    ", clientPhone='" + clientPhone + '\'' +
                    ", serviceCode='" + serviceCode + '\'' +
                    ", orderNumber=" + orderNumber +
                    ", priority=" + priority +
                    ", tags=" + tags +
                    ", categories=" + categories +
                    ", comment='" + comment + '\'' +
                    '}';
        }
    }
}
