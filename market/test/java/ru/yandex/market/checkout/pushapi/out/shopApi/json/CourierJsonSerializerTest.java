package ru.yandex.market.checkout.pushapi.out.shopApi.json;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.pushapi.client.entity.order.Courier;
import ru.yandex.market.checkout.pushapi.client.util.test.JsonTestUtil;

public class CourierJsonSerializerTest {

    private final CourierJsonSerializer serializer = new CourierJsonSerializer();

    @Test
    public void testSerializeVehicle() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new Courier() {{
                    setVehicleNumber("А001AA001");
                    setVehicleDescription("Фиолетовая KIA Rio");
                }},
                "{" +
                        " 'vehicleNumber': 'А001AA001'," +
                        " 'vehicleDescription': 'Фиолетовая KIA Rio'" +
                        "}");
    }

    @Test
    public void testSerializeCourierFullNamePhone() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new Courier() {{
                    setFullName("Перекопский Константин Аркадьевич");
                    setPhone("+79145037777");
                    setPhoneExtension("123");
                }},
    "{" +
                "  'fullName': 'Перекопский Константин Аркадьевич'," +
                "  'phone': '+79145037777'," +
                "  'phoneExtension': '123'" +
                "}");
    }
}
