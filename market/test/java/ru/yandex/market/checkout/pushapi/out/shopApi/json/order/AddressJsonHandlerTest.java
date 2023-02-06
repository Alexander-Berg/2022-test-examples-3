package ru.yandex.market.checkout.pushapi.out.shopApi.json.order;

import org.junit.jupiter.api.Test;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.pushapi.client.util.test.JsonTestUtil;

public class AddressJsonHandlerTest {

    private final AddressJsonSerializer serializer = new AddressJsonSerializer();

    @Test
    public void testSerializeFullAddress() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new AddressImpl() {{
                    setCountry("value_country");
                    setPostcode("value_postcode");
                    setCity("value_city");
                    setDistrict("value_district");
                    setSubway("value_subway");
                    setStreet("value_street");
                    setHouse("value_house");
                    setBlock("value_block");
                    setEntrance("value_entrance");
                    setEntryPhone("value_entryphone");
                    setFloor("value_floor");
                    setApartment("value_apartment");
                    setRecipient("value_recipient");
                    setPhone("value_phone");
                }},
                "{'country': 'value_country'," +
                        "'postcode': 'value_postcode'," +
                        "'city': 'value_city'," +
                        "'district': 'value_district'," +
                        "'subway': 'value_subway'," +
                        "'street': 'value_street'," +
                        "'house': 'value_house'," +
                        "'block': 'value_block'," +
                        "'entrance': 'value_entrance'," +
                        "'entryphone': 'value_entryphone'," +
                        "'floor': 'value_floor'," +
                        "'apartment': 'value_apartment'," +
                        "'recipient': 'value_recipient'," +
                        "'phone': 'value_phone'}"
        );
    }

    @Test
    public void testSerializePartialAddress() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new AddressImpl() {{
                    setEntrance("value_entrance");
                    setEntryPhone("value_entryphone");
                    setFloor("value_floor");
                    setApartment("value_apartment");
                }},
                "{'entrance': 'value_entrance'," +
                        "'entryphone': 'value_entryphone'," +
                        "'floor': 'value_floor'," +
                        "'street': ''," +
                        "'apartment': 'value_apartment'}"
        );
    }

    @Test
    public void testSerializeEmpty() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new AddressImpl(),
                "{'street': ''}"
        );
    }
}
