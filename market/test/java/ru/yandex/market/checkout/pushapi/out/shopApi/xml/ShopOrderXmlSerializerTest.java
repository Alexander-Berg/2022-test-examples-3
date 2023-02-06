package ru.yandex.market.checkout.pushapi.out.shopApi.xml;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.GregorianCalendar;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.IOUtils;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.TaxSystem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.shop.shipment.ShipmentDateCalculationRegionalRule;
import ru.yandex.market.checkout.checkouter.shop.shipment.ShipmentDateCalculationRule;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeliveryWithRegion;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutTimeFormat;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.client.xml.ParcelXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.order.BuyerXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.order.ItemPromoXmlSerializer;
import ru.yandex.market.checkout.pushapi.providers.ShopOrderProvider;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrder;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrderItem;
import ru.yandex.market.checkout.pushapi.shop.entity.StubExtraContext;
import ru.yandex.market.checkout.util.EnhancedRandomHelper;
import ru.yandex.market.checkout.util.ShopOrderTestData;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.checkout.checkouter.shop.shipment.ShipmentDateCalculationRegionalRule.BaseDateForCalculation.DELIVERY_DATE;
import static ru.yandex.market.checkout.checkouter.shop.shipment.ShipmentDateCalculationRegionalRule.BaseDateForCalculation.ORDER_CREATION_DATE;

//MARKETCHECKOUT-4009 run all randomized tests several times to prevent floating failures
public class ShopOrderXmlSerializerTest {

    private static final EnhancedRandom enhancedRandom = EnhancedRandomHelper.createEnhancedRandom();

    private ShopOrderXmlSerializer serializer = new ShopOrderXmlSerializer();
    private DeliveryWithRegionXmlSerializer deliveryWithRegionXmlSerializer
            = mock(DeliveryWithRegionXmlSerializer.class);
    private ShopOrderItemShopXmlSerializer orderItemXmlSerializer = mock(ShopOrderItemShopXmlSerializer.class);
    private DeliveryWithRegion deliveryWithRegion = mock(DeliveryWithRegion.class);
    private ShopOrderItem offerItem1 = new ShopOrderItem() {{
        setFeedId(1L);
        setOfferId("item1");
        setFeedCategoryId("Камеры");
        setOfferName("OfferName");
        setPrice(new BigDecimal("4567"));
        setBuyerPriceBeforeDiscount(new BigDecimal("4569"));
        setCount(5);
        setDelivery(true);
    }};
    private ShopOrderItem offerItem2 = new ShopOrderItem() {{
        setFeedId(1L);
        setOfferId("item2");
        setFeedCategoryId("Камеры");
        setOfferName("OfferName");
        setPrice(new BigDecimal("4567"));
        setBuyerPriceBeforeDiscount(new BigDecimal("4569"));
        setCount(5);
        setDelivery(true);
    }};

    @BeforeEach
    public void setUp() throws Exception {
        CheckoutDateFormat checkoutDateFormat = new CheckoutDateFormat();
        serializer.setCheckoutDateFormat(checkoutDateFormat);
        XmlTestUtil.initMockSerializer(
                deliveryWithRegionXmlSerializer,
                deliveryWithRegion,
                writer -> writer.addNode("delivery", "delivery")
        );
        XmlTestUtil.initMockSerializer(
                orderItemXmlSerializer,
                offerItem1,
                writer -> writer.addNode("item", "item1")
        );
        XmlTestUtil.initMockSerializer(
                orderItemXmlSerializer,
                offerItem2,
                writer -> writer.addNode("item", "item2")
        );

        serializer.setDeliveryWithRegionXmlSerializer(deliveryWithRegionXmlSerializer);
        serializer.setShopOrderItemXmlSerializer(orderItemXmlSerializer);
        serializer.setBuyerXmlSerializer(new BuyerXmlSerializer());
        serializer.setStubExtraContextXmlSerializer(
                new StubExtraContextXmlSerializer(new ShipmentDateCalculationRuleXmlSerializer()));
    }

    @RepeatedTest(10)
    public void testPostpaid() throws Exception {
        ShopOrder shopOrder = enhancedRandom.nextObject(ShopOrder.class,
                "status", "substatus", "creationDate", "itemsTotal", "total", "buyer", "preorder",
                "fulfilment", "subsidy", "totalWithSubsidy", "deliveryWithRegion.parcels.route",
                "deliveryWithRegion.shipments.route", "deliveryWithRegion.shipment.route", "stubContext",
                "buyerItemsTotal", "buyerTotal", "buyerItemsTotalBeforeDiscount", "buyerTotalBeforeDiscount"
        );
        shopOrder.setBusinessId(1L);
        shopOrder.setId(1234L);
        shopOrder.setCurrency(Currency.RUR);
        shopOrder.setPaymentType(PaymentType.POSTPAID);
        shopOrder.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        shopOrder.setItems(
                asList(
                        offerItem1,
                        offerItem2
                )
        );
        shopOrder.setDeliveryWithRegion(deliveryWithRegion);
        shopOrder.setNotes("notes-notes-notes");
        shopOrder.setFake(true);
        shopOrder.setContext(Context.SANDBOX);
        shopOrder.setTaxSystem(TaxSystem.OSN);
        shopOrder.setElectronicAcceptanceCertificateCode("123QWEasd");

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                shopOrder,
                "<order businessId='1' id='1234' currency='RUR' payment-type='POSTPAID' " +
                        "payment-method='CASH_ON_DELIVERY' tax-system='OSN' " +
                        "electronicAcceptanceCertificateCode='123QWEasd'" +
                        "   fake='true' context='SANDBOX' subsidy-total='0'>" +
                        "    <items>" +
                        "        <item>item1</item>" +
                        "        <item>item2</item>" +
                        "    </items>" +
                        "    <delivery>delivery</delivery>" +
                        "    <notes>notes-notes-notes</notes>" +
                        "</order>"
        );
    }

    @RepeatedTest(10)
    public void testPrepaid() throws Exception {
        ShopOrder shopOrder = enhancedRandom.nextObject(ShopOrder.class,
                "status", "substatus", "creationDate", "itemsTotal", "total", "buyer", "context",
                "fake", "paymentMethod", "preorder", "fulfilment", "subsidy", "totalWithSubsidy",
                "deliveryWithRegion.parcels.route", "deliveryWithRegion.shipments.route",
                "deliveryWithRegion.shipment.route", "buyerItemsTotal", "buyerTotal",
                "buyerItemsTotalBeforeDiscount", "buyerTotalBeforeDiscount"
        );
        shopOrder.setBusinessId(1L);
        shopOrder.setId(1234L);
        shopOrder.setCurrency(Currency.RUR);
        shopOrder.setPaymentType(PaymentType.PREPAID);
        shopOrder.setItems(
                asList(
                        offerItem1,
                        offerItem2
                )
        );
        shopOrder.setDeliveryWithRegion(deliveryWithRegion);
        shopOrder.setNotes("notes-notes-notes");
        shopOrder.setTaxSystem(TaxSystem.USN);
        shopOrder.setCreationDate(new GregorianCalendar(2020, Calendar.MARCH, 20, 10, 0, 0).getTime());
        shopOrder.setStubContext(new StubExtraContext());
        shopOrder.setElectronicAcceptanceCertificateCode("123QWEasd");
        shopOrder.getStubContext().setShipmentDateCalculationRule(ShipmentDateCalculationRule.builder()
                .withHourBefore(13)
                .withRuleForLocalDeliveryRegion(ShipmentDateCalculationRegionalRule.builder()
                        .withDaysToAdd(1)
                        .withBaseDateForCalculation(ORDER_CREATION_DATE)
                        .build())
                .withRuleForNonLocalDeliveryRegion(ShipmentDateCalculationRegionalRule.builder()
                        .withDaysToAdd(-1)
                        .withBaseDateForCalculation(DELIVERY_DATE)
                        .build())
                .withHolidays(singletonList(LocalDate.of(2020, 5, 13)))
                .build()
        );
        shopOrder.getStubContext().setShopLocalDeliveryRegion(213L);

        String expected = IOUtils.readInputStream(ShopOrderXmlSerializerTest.class
                .getResourceAsStream("/files/pushapi/shopOrder.xml"));

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                shopOrder,
                expected
        );
    }

    @RepeatedTest(10)
    public void testSerializeEmpty() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                new ShopOrder(),
                "<order subsidy-total='0'/>"
        );
    }

    @RepeatedTest(10)
    public void testSerializeCancelled() throws Exception {
        ShopOrder shopOrder = ShopOrderProvider.prepareOrder(deliveryWithRegion, asList(offerItem1, offerItem2));

        XmlTestUtil.assertSerializeResultAndString(
                serializer, shopOrder,
                "<order businessId='1' id='1234' status='CANCELLED'" +
                        "   substatus='USER_CHANGED_MIND'" +
                        "   creation-date='06-07-2013 15:30:40'" +
                        "   currency='RUR'" +
                        "   items-total='10.75'" +
                        "   total='11.43'" +
                        "   payment-type='PREPAID'" +
                        "   payment-method='CARD_ON_DELIVERY'" +
                        "   fake='true' context='SANDBOX' subsidy-total='0'>" +
                        "       <items>" +
                        "           <item>item1</item>" +
                        "           <item>item2</item>" +
                        "       </items>" +
                        "       <delivery>delivery</delivery>" +
                        "       <buyer id='1234567890' last-name='Tolstoy' first-name='Leo' middle-name='Nikolaevich'" +
                        " " +
                        "personal-email-id='9e92bc743c624f958b8876c7841a653b' " +
                        "personal-full-name-id='a1c595eb35404207aecfa080f90a8986' " +
                        "personal-phone-id='c0dec0dedec0dec0dec0dec0dedec0de' " +
                        "phone='+71234567891' email='a@b.com' uid='359953025' />" +
                        "</order>"
        );
    }

    @RepeatedTest(10)
    public void testSerializeWithSberId() throws Exception {
        ShopOrder shopOrder = ShopOrderProvider.prepareOrderWithSberId(
                deliveryWithRegion, asList(offerItem1, offerItem2));

        XmlTestUtil.assertSerializeResultAndString(
                serializer, shopOrder,
                "<order businessId='1' id='1234' status='CANCELLED'" +
                        "   substatus='USER_CHANGED_MIND'" +
                        "   creation-date='06-07-2013 15:30:40'" +
                        "   currency='RUR'" +
                        "   items-total='10.75'" +
                        "   total='11.43'" +
                        "   payment-type='PREPAID'" +
                        "   payment-method='CARD_ON_DELIVERY'" +
                        "   fake='true' context='SANDBOX' subsidy-total='0'>" +
                        "       <items>" +
                        "           <item>item1</item>" +
                        "           <item>item2</item>" +
                        "       </items>" +
                        "       <delivery>delivery</delivery>" +
                        "       <buyer id='1234567890' last-name='Tolstoy' first-name='Leo' middle-name='Nikolaevich'" +
                        " " +
                        "personal-email-id='9e92bc743c624f958b8876c7841a653b' " +
                        "personal-full-name-id='a1c595eb35404207aecfa080f90a8986' " +
                        "personal-phone-id='c0dec0dedec0dec0dec0dec0dedec0de' " +
                        "phone='+71234567891' email='a@b.com' uid='2305843009213693951' />" +
                        "</order>"
        );
    }

    @RepeatedTest(10)
    public void testSerializeNotCancelled() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                new ShopOrder() {{
                    setBusinessId(1L);
                    setId(1234L);
                    setStatus(OrderStatus.DELIVERED);
                }},
                "<order businessId='1' id='1234' status='DELIVERED' subsidy-total='0'/>"
        );
    }

    @RepeatedTest(10)
    public void testSerializeTotalWithSubsidy() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                new ShopOrder() {{
                    setBusinessId(1L);
                    setId(1234L);
                    setStatus(OrderStatus.DELIVERED);
                    setTotalWithSubsidy(BigDecimal.valueOf(999));
                }},
                "<order businessId='1' id='1234' status='DELIVERED' subsidy-total='0' total-with-subsidy='999'/>"
        );
    }

    @Test
    @DisplayName("тест полной сериализации заказа")
    public void testFullOrderSerialization() throws Exception {
        ShopOrderXmlSerializer fullSerializer = new ShopOrderXmlSerializer();
        DeliveryWithRegionXmlSerializer deliverySerializer = new DeliveryWithRegionXmlSerializer();
        deliverySerializer.setAddressShopXmlSerializer(new AddressShopXmlSerializer());
        deliverySerializer.setParcelXmlSerializer(new ParcelXmlSerializer());
        deliverySerializer.setCourierXmlSerializer(new CourierInfoXmlSerializer());
        deliverySerializer.setCheckoutDateFormat(new CheckoutDateFormat());
        deliverySerializer.setCheckoutTimeFormat(new CheckoutTimeFormat());
        fullSerializer.setDeliveryWithRegionXmlSerializer(deliverySerializer);
        fullSerializer.setBuyerXmlSerializer(new BuyerXmlSerializer());
        fullSerializer.setShopOrderItemXmlSerializer(new ShopOrderItemShopXmlSerializer(new ItemPromoXmlSerializer()));
        fullSerializer.setStubExtraContextXmlSerializer(new StubExtraContextXmlSerializer(new ShipmentDateCalculationRuleXmlSerializer()));
        fullSerializer.setBuyerXmlSerializer(new BuyerXmlSerializer());
        fullSerializer.setCheckoutDateFormat(new CheckoutDateFormat());

        String expected = IOUtils.readInputStream(this.getClass()
                .getResourceAsStream("/files/pushapi/fullShopOrder.xml"));
        XmlTestUtil.assertSerializeResultAndString(fullSerializer, ShopOrderTestData.get(), expected);
    }
}
