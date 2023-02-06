package config.classmapping.order;

import config.classmapping.BaseClassMappingsTest;
import org.junit.jupiter.api.Test;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.AddressType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AddressClassMappingsTest extends BaseClassMappingsTest {
    @Test
    public void testParse() throws Exception {
        final Address actual = deserialize(AddressImpl.class,
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
                }

        @Test
        public void testParseEmpty() throws Exception {
        final Address address = deserialize(AddressImpl.class,
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
        }

    @Test
    public void testFullySerialize() throws Exception {
        serializeAndCompare(
                new AddressImpl() {{
                    setCountry("value_country");
                    setPostcode("value_postcode");
                    setCity("value_city");
                    setSubway("value_subway");
                    setStreet("value_street");
                    setHouse("value_house");
                    setBlock("value_block");
                    setEntrance("value_entrance");
                    setEntryPhone("value_entryphone");
                    setFloor("value_floor");
                    setApartment("value_apartment");
                }},
                "<address country='value_country'" +
                        "         postcode='value_postcode'" +
                        "         city='value_city'" +
                        "         subway='value_subway'" +
                        "         street='value_street'" +
                        "         house='value_house'" +
                        "         block='value_block'" +
                        "         entrance='value_entrance'" +
                        "         entryphone='value_entryphone'" +
                        "         floor='value_floor'" +
                        "         apartment='value_apartment' />"
        );
    }

    @Test
    public void testFullyPartially() throws Exception {
        serializeAndCompare(
                new AddressImpl() {{
                    setSubway("value_subway");
                    setStreet("value_street");
                    setHouse("value_house");
                    setBlock("value_block");
                    setEntrance("value_entrance");
                    setEntryPhone("value_entryphone");
                    setFloor("value_floor");
                    setApartment("value_apartment");
                }},
                "<address subway='value_subway'" +
                        "         street='value_street'" +
                        "         house='value_house'" +
                        "         block='value_block'" +
                        "         entrance='value_entrance'" +
                        "         entryphone='value_entryphone'" +
                        "         floor='value_floor'" +
                        "         apartment='value_apartment'/>"
        );
    }

    @Test
    public void testSerializeEmpty() throws Exception {
        serializeAndCompare(
                new AddressImpl(),
                "<address street=\"\"/>"
        );
    }

    @Test
    public void testSerializeShopAddress() throws Exception {
        serializeAndCompare(
                new AddressImpl() {{
                    setType(AddressType.SHOP);
                }},
                "<shop-address street=\"\"/>"
        );
    }

    @Test
    public void testSerializeBuyerAddress() throws Exception {
        serializeAndCompare(
                new AddressImpl() {{
                    setType(AddressType.BUYER);
                }},
                "<buyer-address street=\"\"/>"
        );
    }

    @Test
    public void testSerializeUnknownAddressType() throws Exception {
        serializeAndCompare(
                new AddressImpl() {{
                    setType(AddressType.UNKNOWN);
                }},
                "<address street=\"\"/>"
        );
    }
}
