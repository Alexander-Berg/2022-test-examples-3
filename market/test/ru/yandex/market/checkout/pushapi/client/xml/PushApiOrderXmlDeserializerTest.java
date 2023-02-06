package ru.yandex.market.checkout.pushapi.client.xml;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.pushapi.client.entity.PushApiOrder;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PushApiOrderXmlDeserializerTest {

    private PushApiOrderXmlDeserializer deserializer = new PushApiOrderXmlDeserializer();

    {
        PushApiDeliveryXmlDeserializer deliveryDeserializer = new PushApiDeliveryXmlDeserializer();
        deliveryDeserializer.setCourierXmlDeserializer(new CourierXmlDeserializer());
        deserializer.setCheckoutDateFormat(new CheckoutDateFormat());
        deserializer.setPushApiDeliveryXmlDeserializer(deliveryDeserializer);
    }

    @Test
    public void testParse() throws Exception {
        final PushApiOrder actual = XmlTestUtil.deserialize(
                deserializer,
                "<order" +
                        " electronicAcceptanceCertificateCode='123456'" +
                        ">" +
                        "<delivery>" +
                        "<courier" +
                        " vehicle-number='а123бв 71 RUS'" +
                        " vehicle-description='Фиолетовая KIA Rio'" +
                        " full-name='Перекопский Константин Аркадьевич'" +
                        " courierFio='Перекопский Константин Аркадьевич'" +
                        " phone='+79145037777'" +
                        " phone-extension='123'" +
                        "/>" +
                        "</delivery>" +
                        "</order>"
        );

        assertEquals("123456", actual.getElectronicAcceptanceCertificateCode());
        assertEquals("а123бв 71 RUS", actual.getDelivery().getCourier().getVehicleNumber());
        assertEquals("Фиолетовая KIA Rio", actual.getDelivery().getCourier().getVehicleDescription());
        assertEquals("Перекопский Константин Аркадьевич", actual.getDelivery().getCourier().getFullName());
        assertEquals("+79145037777", actual.getDelivery().getCourier().getPhone());
        assertEquals("123", actual.getDelivery().getCourier().getPhoneExtension());
    }
}
