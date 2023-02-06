package ru.yandex.market.checkout.pushapi.client.xml;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.pushapi.client.entity.PushApiOrder;
import ru.yandex.market.checkout.pushapi.client.entity.order.Courier;
import ru.yandex.market.checkout.pushapi.client.entity.order.PushApiDelivery;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.xml.order.AddressXmlSerializer;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.checkout.pushapi.client.util.test.PushApiTestHelper.createLongDate;
import static ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil.sameXmlAs;
import static ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil.serialize;

public class PushApiOrderXmlSerializerTest {

    private PushApiOrderXmlSerializer serializer = new PushApiOrderXmlSerializer();

    {
        PushApiDeliveryXmlSerializer deliverySerializer = new PushApiDeliveryXmlSerializer();
        deliverySerializer.setCourierXmlSerializer(new CourierXmlSerializer());
        deliverySerializer.setAddressXmlSerializer(new AddressXmlSerializer());
        deliverySerializer.setParcelXmlSerializer(new ParcelXmlSerializer());
        deliverySerializer.setCheckoutDateFormat(new CheckoutDateFormat());
        serializer.setCheckoutDateFormat(new CheckoutDateFormat());
        serializer.setPushApiDeliveryXmlSerializer(deliverySerializer);
    }

    @Test
    void testSerializeElectronicAcceptanceCertificateCode() throws Exception {
        PushApiOrder order = getPushApiOrder();
        order.setElectronicAcceptanceCertificateCode("123456");

        final String result = serialize(serializer, order);

        assertThat(result, sameXmlAs(
                "<order" +
                        "   electronicAcceptanceCertificateCode='123456'" +
                        "   id='1234'" +
                        "   status='DELIVERY'" +
                        "   substatus='COURIER_FOUND'" +
                        "   creation-date='23-11-2021 10:30:40'" +
                        "   currency='RUR'" +
                        "   payment-type='PREPAID'" +
                        "   payment-method='CARD_ON_DELIVERY'" +
                        "   fake='false'" +
                        "   context='SANDBOX'" +
                        "   accept-method='0' subsidy-total='0' fulfilment='false'/>"
        ));
    }

    @Test
    void testSerializeVehicleNumberAndDescription() throws Exception {
        PushApiOrder order = getPushApiOrder();
        PushApiDelivery delivery = new PushApiDelivery();
        Courier courier = new Courier();
        courier.setVehicleNumber("а123бв 71 RUS");
        courier.setVehicleDescription("Фиолетовая KIA Rio");
        delivery.setCourier(courier);
        order.setDelivery(delivery);
        final String result = serialize(serializer, order);

        assertThat(result, sameXmlAs(
                "<order" +
                        "   id='1234'" +
                        "   status='DELIVERY'" +
                        "   substatus='COURIER_FOUND'" +
                        "   creation-date='23-11-2021 10:30:40'" +
                        "   currency='RUR'" +
                        "   payment-type='PREPAID'" +
                        "   payment-method='CARD_ON_DELIVERY'" +
                        "   fake='false'" +
                        "   context='SANDBOX'" +
                        "   accept-method='0' subsidy-total='0' fulfilment='false'>" +
                        "<delivery>" +
                        "<courier" +
                        "   vehicle-number='а123бв 71 RUS'" +
                        "   vehicle-description='Фиолетовая KIA Rio'" +
                        "/>" +
                        "</delivery>" +
                        "</order>"
        ));
    }


    @Test
    void testSerializeCourierFioAndPhone() throws Exception {
        PushApiOrder order = getPushApiOrder();
        Courier courier = new Courier();
        courier.setFullName("Перекопский Константин Аркадьевич");
        courier.setPhone("+79145037777");
        courier.setPhoneExtension("123");
        PushApiDelivery delivery = new PushApiDelivery();
        delivery.setCourier(courier);
        order.setDelivery(delivery);

        final String result = serialize(serializer, order);

        assertThat(result, sameXmlAs(
                "<order" +
                        "   id='1234'" +
                        "   status='DELIVERY'" +
                        "   substatus='COURIER_FOUND'" +
                        "   creation-date='23-11-2021 10:30:40'" +
                        "   currency='RUR'" +
                        "   payment-type='PREPAID'" +
                        "   payment-method='CARD_ON_DELIVERY'" +
                        "   fake='false'" +
                        "   context='SANDBOX'" +
                        "   accept-method='0' subsidy-total='0' fulfilment='false'>" +
                        "   <delivery>" +
                        "   <courier " +
                        "       phone='+79145037777'" +
                        "       phone-extension='123'" +
                        "       full-name='Перекопский Константин Аркадьевич'" +
                        "   />" +
                        "   </delivery>" +
                        "</order>"
        ));
    }

    private PushApiOrder getPushApiOrder() {
        PushApiOrder order = new PushApiOrder();
        order.setId(1234L);
        order.setStatus(OrderStatus.DELIVERY);
        order.setSubstatus(OrderSubstatus.COURIER_FOUND);
        order.setCreationDate(createLongDate("2021-11-23 10:30:40"));
        order.setCurrency(Currency.RUR);
        order.setPaymentType(PaymentType.PREPAID);
        order.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        order.setFake(false);
        order.setContext(Context.SANDBOX);
        return order;
    }
}
