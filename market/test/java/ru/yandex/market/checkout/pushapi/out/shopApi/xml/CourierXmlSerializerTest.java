package ru.yandex.market.checkout.pushapi.out.shopApi.xml;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.pushapi.client.entity.order.Courier;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;

public class CourierXmlSerializerTest {
    CourierInfoXmlSerializer serializer = new CourierInfoXmlSerializer();

    @Test
    public void testSerializeVehicle() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                new Courier() {{
                    setVehicleNumber("А001AA001");
                    setVehicleDescription("Фиолетовая KIA Rio");
                }},
                "<courier " +
                        "   vehicle-number='А001AA001' " +
                        "   vehicle-description='Фиолетовая KIA Rio' " +
                        "/>"
        );
    }

    @Test
    public void testSerializeCourierFullNamePhone() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                new Courier() {{
                    setFullName("Перекопский Константин Аркадьевич");
                    setPhone("+79145037777");
                    setPhoneExtension("123");
                }},
                "<courier " +
                        "   full-name='Перекопский Константин Аркадьевич' " +
                        "   phone='+79145037777' " +
                        "   phone-extension='123' " +
                        "/>"
        );
    }
}
