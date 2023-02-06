package ru.yandex.market.checkout.checkouter.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.AddressLanguage;
import ru.yandex.market.checkout.checkouter.delivery.AddressSource;
import ru.yandex.market.checkout.checkouter.delivery.HiddenAddressInfoDecorator;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;

public class AddressJsonHandlerTest extends AbstractJsonHandlerTestBase {

    public static final String COUNTRY = "country";
    public static final String POSTCODE = "postcode";
    public static final String CITY = "city";
    public static final String DISTRICT = "district";
    public static final String SUBWAY = "subway";
    public static final String STREET = "street";
    public static final String HOUSE = "house";
    public static final String BLOCK = "block";
    public static final String ENTRANCE = "entrance";
    public static final String ENTRY_PHONE = "entryPhone";
    public static final String FLOOR = "floor";
    public static final String APARTMENT = "apartment";
    public static final String PERSONAL_ADDRESS_ID = "personalAddressId";
    public static final String RECIPIENT = "recipient";
    public static final String PERSONAL_FULL_NAME_ID = "personalFullNameId";
    public static final String PHONE = "phone";
    public static final String PERSONAL_PHONE_ID = "personalPhoneId";
    public static final String RECIPIENT_EMAIL = "recipientEmail";
    public static final String PERSONAL_EMAIL_ID = "personalEmailId";
    public static final AddressLanguage LANGUAGE = AddressLanguage.ENG;
    public static final AddressSource ADDRESS_SOURCE = AddressSource.NEW;
    public static final String GPS = "gps";
    public static final String PERSONAL_GPS_ID = "personalGpsId";

    public static final String JSON = "{"
            + "\"country\":\"" + COUNTRY + "\","
            + "\"postcode\":\"" + POSTCODE + "\","
            + "\"city\":\"" + CITY + "\","
            + "\"district\":\"" + DISTRICT + "\","
            + "\"subway\":\"" + SUBWAY + "\","
            + "\"street\":\"" + STREET + "\","
            + "\"house\":\"" + HOUSE + "\","
            + "\"block\":\"" + BLOCK + "\","
            + "\"entrance\":\"" + ENTRANCE + "\","
            + "\"entryphone\":\"" + ENTRY_PHONE + "\","
            + "\"floor\":\"" + FLOOR + "\","
            + "\"apartment\":\"" + APARTMENT + "\","
            + "\"recipient\":\"" + RECIPIENT + "\","
            + "\"phone\":\"" + PHONE + "\","
            + "\"language\":\"ENG\","
            + "\"gps\":\"" + GPS + "\","
            + "\"addressSource\":\"NEW\","
            + "\"personalPhoneId\":\"" + PERSONAL_PHONE_ID + "\","
            + "\"recipientEmail\":\"" + RECIPIENT_EMAIL + "\","
            + "\"personalEmailId\":\"" + PERSONAL_EMAIL_ID + "\","
            + "\"personalFullNameId\":\"" + PERSONAL_FULL_NAME_ID + "\","
            + "\"personalAddressId\":\"" + PERSONAL_ADDRESS_ID + "\","
            + "\"personalGpsId\":\"" + PERSONAL_GPS_ID + "\""
            + "}";

    @Test
    public void serialize() throws Exception {
        AddressImpl address = EntityHelper.getAddress();

        String json = write(address);

        checkJson(json, "$." + Names.Address.COUNTRY, COUNTRY);
        checkJson(json, "$." + Names.Address.POSTCODE, POSTCODE);
        checkJson(json, "$." + Names.Address.CITY, CITY);
        checkJson(json, "$." + Names.Address.DISTRICT, DISTRICT);
        checkJson(json, "$." + Names.Address.SUBWAY, SUBWAY);
        checkJson(json, "$." + Names.Address.STREET, STREET);
        checkJson(json, "$." + Names.Address.HOUSE, HOUSE);
        checkJson(json, "$." + Names.Address.BLOCK, BLOCK);
        checkJson(json, "$." + Names.Address.ENTRANCE, ENTRANCE);
        checkJson(json, "$." + Names.Address.ENTRYPHONE, ENTRY_PHONE);
        checkJson(json, "$." + Names.Address.FLOOR, FLOOR);
        checkJson(json, "$." + Names.Address.APARTMENT, APARTMENT);
        checkJson(json, "$." + Names.Address.PERSONAL_ADDRESS_ID, PERSONAL_ADDRESS_ID);
        checkJson(json, "$." + Names.Address.RECIPIENT, RECIPIENT);
        checkJson(json, "$." + Names.Address.PERSONAL_FULL_NAME_ID, PERSONAL_FULL_NAME_ID);
        checkJson(json, "$." + Names.Address.PHONE, PHONE);
        checkJson(json, "$." + Names.Address.PERSONAL_PHONE_ID, PERSONAL_PHONE_ID);
        checkJson(json, "$." + Names.Address.RECIPIENT_EMAIL, RECIPIENT_EMAIL);
        checkJson(json, "$." + Names.Address.PERSONAL_EMAIL_ID, PERSONAL_EMAIL_ID);
        checkJson(json, "$." + Names.Address.LANGUAGE, LANGUAGE.name());
        checkJson(json, "$." + Names.Address.GPS, GPS);
        checkJson(json, "$." + Names.Address.PERSONAL_GPS_ID, PERSONAL_GPS_ID);
        checkJson(json, "$." + Names.Address.ADDRESS_SOURCE, ADDRESS_SOURCE.name());
        //
    }

    @Test
    public void serialize2() throws Exception {
        AddressImpl impl2 = new AddressImpl();
        impl2.setLanguage(null);

        String json2 = write(impl2);

        checkJson(json2, "$." + Names.Address.LANGUAGE, AddressLanguage.RUS.name());
    }

    @Test
    public void serialize3() throws Exception {
        AddressImpl impl3 = new AddressImpl();
        impl3.setStreet(null);

        String json3 = write(impl3);

        checkJson(json3, "$." + Names.Address.STREET, "");
    }

    @Test
    public void serialize4() throws Exception {
        AddressImpl impl3 = new AddressImpl();
        impl3.setStreet(null);

        HiddenAddressInfoDecorator decorator = new HiddenAddressInfoDecorator(impl3);

        String json = write(decorator);

        checkJson(json, "$." + Names.Address.STREET, "");
        checkJson(json, "$." + Names.Address.LANGUAGE, AddressLanguage.RUS.name());
    }

    @Test
    public void deserialize() throws Exception {
        Address address = read(Address.class, JSON);

        Assertions.assertEquals(COUNTRY, address.getCountry());
        Assertions.assertEquals(POSTCODE, address.getPostcode());
        Assertions.assertEquals(CITY, address.getCity());
        Assertions.assertEquals(DISTRICT, address.getDistrict());
        Assertions.assertEquals(STREET, address.getStreet());
        Assertions.assertEquals(HOUSE, address.getHouse());
        Assertions.assertEquals(BLOCK, address.getBlock());
        Assertions.assertEquals(ENTRANCE, address.getEntrance());
        Assertions.assertEquals(ENTRY_PHONE, address.getEntryPhone());
        Assertions.assertEquals(FLOOR, address.getFloor());
        Assertions.assertEquals(APARTMENT, address.getApartment());
        Assertions.assertEquals(PERSONAL_ADDRESS_ID, address.getPersonalAddressId());
        Assertions.assertEquals(RECIPIENT, address.getRecipient());
        Assertions.assertEquals(PERSONAL_FULL_NAME_ID, address.getPersonalFullNameId());
        Assertions.assertEquals(PHONE, address.getPhone());
        Assertions.assertEquals(PERSONAL_PHONE_ID, address.getPersonalPhoneId());
        Assertions.assertEquals(RECIPIENT_EMAIL, address.getRecipientEmail());
        Assertions.assertEquals(PERSONAL_EMAIL_ID, address.getPersonalEmailId());
        Assertions.assertEquals(LANGUAGE, address.getLanguage());
        Assertions.assertEquals(ADDRESS_SOURCE, address.getAddressSource());
        Assertions.assertEquals(GPS, address.getGps());
        Assertions.assertEquals(PERSONAL_GPS_ID, address.getPersonalGpsId());
    }

    @Test
    public void deserialize2() throws Exception {
        String json = "{ \"language\": \"DE\" }";

        Address address = read(Address.class, json);

        Assertions.assertEquals(AddressLanguage.UNKNOWN, address.getLanguage());
    }

}
