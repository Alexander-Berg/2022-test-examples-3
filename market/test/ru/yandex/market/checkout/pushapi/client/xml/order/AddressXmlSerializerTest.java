package ru.yandex.market.checkout.pushapi.client.xml.order;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.jupiter.api.RepeatedTest;

import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.AddressType;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.test.providers.RecipientProvider;

//MARKETCHECKOUT-4009 run all randomized tests several times to prevent floating failures
public class AddressXmlSerializerTest {

    private final EnhancedRandom enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandom();

    private AddressXmlSerializer serializer = new AddressXmlSerializer();


    @RepeatedTest(10)
    public void testFullySerialize() throws Exception {
        AddressImpl address = enhancedRandom.nextObject(AddressImpl.class, "type", "recipient", "phone");
        address.setCountry("value_country");
        address.setPostcode("value_postcode");
        address.setCity("value_city");
        address.setDistrict("value_district");
        address.setSubway("value_subway");
        address.setStreet("value_street");
        address.setHouse("value_house");
        address.setBlock("value_block");
        address.setEntrance("value_entrance");
        address.setEntryPhone("value_entryphone");
        address.setFloor("value_floor");
        address.setApartment("value_apartment");
        address.setGps("12.34,56.78");
        address.setRecipientPerson(RecipientProvider.getDefaultRecipient().getPerson());
        address.setRecipientEmail("leo@ya.ru");
        address.setPersonalFullNameId("1");
        address.setPersonalEmailId("2");
        address.setPersonalPhoneId("3");
        address.setPersonalAddressId("4");
        address.setPersonalGpsId("5");
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                address,
                "<address country='value_country'" +
                        "         postcode='value_postcode'" +
                        "         city='value_city'" +
                        "         district='value_district'" +
                        "         subway='value_subway'" +
                        "         street='value_street'" +
                        "         house='value_house'" +
                        "         block='value_block'" +
                        "         entrance='value_entrance'" +
                        "         entryphone='value_entryphone'" +
                        "         floor='value_floor'" +
                        "         personalAddressId='4' " +
                        "         personalEmailId='2' " +
                        "         personalFullNameId='1' " +
                        "         personalGpsId='5' " +
                        "         personalPhoneId='3' " +
                        "         recipientFirstName='Leo'" +
                        "         recipientLastName='Tolstoy'" +
                        "         apartment='value_apartment'" +
                        "         recipientEmail='leo@ya.ru'" +
                        "         gps='12.34,56.78' " +
                        "/>"
        );
    }

    @RepeatedTest(10)
    public void testFullyPartially() throws Exception {
        AddressImpl address = enhancedRandom.nextObject(AddressImpl.class, "type", "recipient", "recipientPerson",
                "phone", "country", "postcode", "city", "district", "gps", "recipientEmail",
                "personalAddressId", "personalEmailId", "personalFullNameId", "personalGpsId", "personalPhoneId");
        address.setSubway("value_subway");
        address.setStreet("value_street");
        address.setHouse("value_house");
        address.setBlock("value_block");
        address.setEntrance("value_entrance");
        address.setEntryPhone("value_entryphone");
        address.setFloor("value_floor");
        address.setApartment("value_apartment");
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                address,
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

    @RepeatedTest(10)
    public void testSerializeEmpty() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                new AddressImpl(),
                "<address street=\"\"/>"
        );
    }

    @RepeatedTest(10)
    public void testSerializeShopAddress() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                new AddressImpl() {{
                    setType(AddressType.SHOP);
                }},
                "<shop-address street=\"\"/>"
        );
    }

    @RepeatedTest(10)
    public void testSerializeBuyerAddress() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                new AddressImpl() {{
                    setType(AddressType.BUYER);
                }},
                "<buyer-address street=\"\"/>"
        );
    }

    @RepeatedTest(10)
    public void testSerializeUnknownAddressType() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                new AddressImpl() {{
                    setType(AddressType.UNKNOWN);
                }},
                "<address street=\"\"/>"
        );
    }
}
