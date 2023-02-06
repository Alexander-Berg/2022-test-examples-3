package ru.yandex.market.checkout.pushapi.client.xml.order;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.test.providers.RecipientProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AddressXmlDeserializerTest {

    private AddressXmlDeserializer deserializer = new AddressXmlDeserializer();

    @Test
    public void testParse() throws Exception {
        final Address actual = XmlTestUtil.deserialize(
                deserializer,
                "<address country='value_country'" +
                        "         postcode='value_postcode'" +
                        "         city='value_city'" +
                        "         district='value_district'" +
                        "         subway='value_subway'" +
                        "         street='value_street'" +
                        "         house='value_house'" +
                        "         building='value_building'" +
                        "         block='value_block'" +
                        "         estate='value_estate'" +
                        "         entrance='value_entrance'" +
                        "         entryphone='value_entryphone'" +
                        "         floor='value_floor'" +
                        "         apartment='value_apartment'" +
                        "         note='value_note'" +
                        "         recipientFirstName='Leo'" +
                        "         recipientLastName='Tolstoy'" +
                        "         recipientEmail='value_recipient_email'" +
                        "         gps='value_gps' />"
        );

        assertEquals("value_country", actual.getCountry());
        assertEquals("value_postcode", actual.getPostcode());
        assertEquals("value_city", actual.getCity());
        assertEquals("value_district", actual.getDistrict());
        assertEquals("value_subway", actual.getSubway());
        assertEquals("value_street", actual.getStreet());
        assertEquals("value_house", actual.getHouse());
        assertEquals("value_block", actual.getBlock());
        assertEquals("value_entrance", actual.getEntrance());
        assertEquals("value_entryphone", actual.getEntryPhone());
        assertEquals("value_floor", actual.getFloor());
        assertEquals("value_apartment", actual.getApartment());
        assertEquals("value_recipient_email", actual.getRecipientEmail());
        assertEquals("value_gps", actual.getGps());
        assertEquals(RecipientProvider.getDefaultRecipient().getPerson(), actual.getRecipientPerson());
    }

    @Test
    public void testParseEmpty() throws Exception {
        final Address address = XmlTestUtil.deserialize(
                deserializer,
                "<address />"
        );

        assertNotNull(address);
        assertNull(address.getCountry());
        assertNull(address.getPostcode());
        assertNull(address.getCity());
        assertNull(address.getDistrict());
        assertNull(address.getSubway());
        assertNull(address.getStreet());
        assertNull(address.getHouse());
        assertNull(address.getBlock());
        assertNull(address.getEntrance());
        assertNull(address.getEntryPhone());
        assertNull(address.getFloor());
        assertNull(address.getApartment());
        assertNull(address.getRecipientPerson());
        assertNull(address.getRecipientEmail());
    }
}
