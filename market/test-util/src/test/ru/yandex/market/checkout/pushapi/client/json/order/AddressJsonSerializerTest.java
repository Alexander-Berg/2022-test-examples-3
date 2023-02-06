package ru.yandex.market.checkout.pushapi.client.json.order;

import org.junit.Test;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.pushapi.client.util.test.JsonTestUtil;

public class AddressJsonSerializerTest {

    private final AddressJsonSerializer serializer = new AddressJsonSerializer();

    @Test
    public void testSerializeFullAddress() throws Exception {
        JsonTestUtil.assertJsonSerialize(
            serializer,
            new Address() {{
                setCountry("value_country");
                setPostcode("value_postcode");
                setCity("value_city");
                setSubway("value_subway");
                setStreet("value_street");
                setHouse("value_house");
                setBlock("value_block");
                setEntrance("value_entrance");
                setEntryphone("value_entryphone");
                setFloor("value_floor");
                setApartment("value_apartment");
                setRecipient("value_recipient");
                setPhone("value_phone");
            }},
            "{'country': 'value_country'," +
                "'postcode': 'value_postcode'," +
                "'city': 'value_city'," +
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
            new Address() {{
                setEntrance("value_entrance");
                setEntryphone("value_entryphone");
                setFloor("value_floor");
                setApartment("value_apartment");
            }},
            "{'entrance': 'value_entrance'," +
                "'entryphone': 'value_entryphone'," +
                "'floor': 'value_floor'," +
                "'apartment': 'value_apartment'}"
        );
    }

    @Test
    public void testSerializeEmpty() throws Exception {
        JsonTestUtil.assertJsonSerialize(
            serializer,
            new Address(),
            "{}"
        );
    }
}
