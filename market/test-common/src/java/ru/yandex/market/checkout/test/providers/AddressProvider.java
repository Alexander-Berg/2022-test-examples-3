package ru.yandex.market.checkout.test.providers;

import java.util.function.Consumer;

import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.AddressLanguage;
import ru.yandex.market.checkout.checkouter.delivery.AddressSource;
import ru.yandex.market.checkout.checkouter.delivery.Recipient;
import ru.yandex.market.checkout.checkouter.returns.SenderAddress;

public abstract class AddressProvider {

    public static final String POSTCODE = "131488";
    public static final String GEOBASE_POSTCODE = "119021";
    public static final String PERSONAL_ADDRESS_ID = "34251639gwbcesaqq239098jhcdxe454";
    public static final String PERSONAL_ADDRESS_ID_ANOTHER = "34251639gwbcesaqq239098jhcdxe453";
    public static final String PERSONAL_GPS_ID = "k4bv310ccd62ndkgwe3w3c56lgh78924";
    public static final String PERSONAL_GPS_ID_ANOTHER = "k4bv310ccd62ndkgwe3w3c56lgh78923";

    public static Address getAddress() {
        return createAddressImpl();
    }

    public static Address getAddress(Consumer<AddressImpl> postprocessor) {
        AddressImpl addressImpl = createAddressImpl();
        postprocessor.accept(addressImpl);
        return addressImpl;
    }

    public static Address getAddress(Recipient recipient) {
        return createAddressImpl(recipient);
    }

    public static SenderAddress getSenderAddress() {
            SenderAddress address = new SenderAddress(
                    "Россия", "Москва", "Инакентий Маркетович", PERSONAL_ADDRESS_ID,
                    "a41f94bbac915383d80c0d76ef22e576",
                    "+79607481463", "0123456789abcdef0123456789abcdef",
                    AddressLanguage.RUS, "60.606071,56.858464", PERSONAL_GPS_ID,
                    "inakentii@market.ru", "daedbf8c1b57e4603928612fa3c47509");
            address.setPostcode("123456");
            address.setSubway("метро Очаково");
            address.setStreet("улица Яблочково");
            address.setKm("26");
            address.setHouse("24");
            address.setBlock("2а");
            address.setBuilding("4к");
            address.setEstate("34л");
            address.setEntrance("1");
            address.setEntryPhone("345*3464");
            address.setFloor("4");
            address.setApartment("47");
            address.setNotes("Вход через платяной шкаф");
            return address;
    }

    private static AddressImpl createAddressImpl() {
        return createAddressImpl(RecipientProvider.getDefaultRecipient());
    }

    private static AddressImpl createAddressImpl(Recipient recipient) {
        AddressImpl shopAddress = new AddressImpl();
        shopAddress.setCountry("Русь");
        shopAddress.setPostcode("131488");
        shopAddress.setCity("Питер");
        shopAddress.setDistrict("Московский район");
        shopAddress.setSubway("Петровско-Разумовская");
        shopAddress.setStreet("Победы");
        shopAddress.setHouse("13");
        shopAddress.setBuilding("222");
        shopAddress.setBlock("666");
        shopAddress.setEntrance("404");
        shopAddress.setEntryPhone("007");
        shopAddress.setFloor("8");
        shopAddress.setApartment("303");
        shopAddress.setPersonalAddressId(PERSONAL_ADDRESS_ID_ANOTHER);
        shopAddress.setRecipient(recipient.getPerson().getFormattedName());
        shopAddress.setPhone(recipient.getPhone());
        shopAddress.setPersonalPhoneId(recipient.getPersonalPhoneId());
        shopAddress.setRecipientEmail(recipient.getEmail());
        shopAddress.setPersonalEmailId(recipient.getPersonalEmailId());
        shopAddress.setRecipientPerson(recipient.getPerson());
        shopAddress.setPersonalFullNameId(recipient.getPersonalFullNameId());
        shopAddress.setLanguage(AddressLanguage.RUS);
        shopAddress.setOutletName("Планета здоровья");
        shopAddress.setAddressSource(AddressSource.NEW);
        return shopAddress;
    }

    public static Address getAnotherAddress() {
        return createAnotherAddressImpl();
    }

    public static Address getAnotherAddress(Consumer<AddressImpl> postProcessor) {
        AddressImpl anotherAddressImpl = createAnotherAddressImpl();
        postProcessor.accept(anotherAddressImpl);
        return anotherAddressImpl;
    }

    public static Address getAddressBigHouseField() {
        return createAddressBigHouseFieldImpl();
    }

    private static AddressImpl createAnotherAddressImpl() {
        Recipient recipient = RecipientProvider.getDefaultRecipient();
        AddressImpl shopAddress = new AddressImpl();
        shopAddress.setCountry("Русь");
        shopAddress.setPostcode(POSTCODE);
        shopAddress.setCity("Москва");
        shopAddress.setSubway("Парк Культуры");
        shopAddress.setStreet("Льва Толстого");
        shopAddress.setHouse("16");
        shopAddress.setEntrance("2");
        shopAddress.setPersonalAddressId(PERSONAL_ADDRESS_ID_ANOTHER);
        shopAddress.setPersonalGpsId(PERSONAL_GPS_ID_ANOTHER);
        shopAddress.setLanguage(AddressLanguage.RUS);
        shopAddress.setRecipient(recipient.getPerson().getFormattedName());
        shopAddress.setPhone(recipient.getPhone());
        shopAddress.setPersonalPhoneId(recipient.getPersonalPhoneId());
        shopAddress.setRecipientEmail(recipient.getEmail());
        shopAddress.setPersonalEmailId(recipient.getPersonalEmailId());
        shopAddress.setRecipientPerson(recipient.getPerson());
        shopAddress.setPersonalFullNameId(recipient.getPersonalFullNameId());
        return shopAddress;
    }

    private static AddressImpl createAddressBigHouseFieldImpl() {
        Recipient recipient = RecipientProvider.getDefaultRecipient();

        AddressImpl shopAddress = new AddressImpl();
        shopAddress.setCountry("Русь");
        shopAddress.setPostcode("131488");
        shopAddress.setCity("Москва");
        shopAddress.setSubway("Парк Культуры / Park Culturi and Otdiha");
        shopAddress.setStreet("Льва Толстого");
        shopAddress.setHouse(" 104 остановка \"ПАМЯТНИК ЧЕРНОБЫЛЬЦАМ\"");
        shopAddress.setEntrance("2");
        shopAddress.setPersonalAddressId(PERSONAL_ADDRESS_ID_ANOTHER);
        shopAddress.setPersonalGpsId(PERSONAL_GPS_ID_ANOTHER);
        shopAddress.setLanguage(AddressLanguage.RUS);
        shopAddress.setRecipient(recipient.getPerson().getFormattedName());
        shopAddress.setPhone(recipient.getPhone());
        shopAddress.setPersonalPhoneId(recipient.getPersonalPhoneId());
        shopAddress.setRecipientEmail(recipient.getEmail());
        shopAddress.setPersonalEmailId(recipient.getPersonalEmailId());
        shopAddress.setRecipientPerson(recipient.getPerson());
        shopAddress.setPersonalFullNameId(recipient.getPersonalFullNameId());
        return shopAddress;
    }


    public static Address getAnotherAddressWithSameCity() {
        Recipient recipient = RecipientProvider.getDefaultRecipient();
        AddressImpl shopAddress = new AddressImpl();
        shopAddress.setCountry("Русь");
        shopAddress.setPostcode("131488");
        shopAddress.setCity("Питер");
        shopAddress.setSubway("Маяковская");
        shopAddress.setStreet("Невский просект");
        shopAddress.setHouse("16");
        shopAddress.setEntrance("2");
        shopAddress.setPersonalAddressId(PERSONAL_ADDRESS_ID_ANOTHER);
        shopAddress.setPersonalGpsId(PERSONAL_GPS_ID_ANOTHER);
        shopAddress.setLanguage(AddressLanguage.RUS);
        shopAddress.setRecipient(recipient.getPerson().getFormattedName());
        shopAddress.setPhone(recipient.getPhone());
        shopAddress.setPersonalPhoneId(recipient.getPersonalPhoneId());
        shopAddress.setRecipientEmail(recipient.getEmail());
        shopAddress.setPersonalEmailId(recipient.getPersonalEmailId());
        shopAddress.setRecipientPerson(recipient.getPerson());
        shopAddress.setPersonalFullNameId(recipient.getPersonalFullNameId());
        return shopAddress;
    }

    public static Address getEnglishAddress() {
        Recipient recipient = RecipientProvider.getDefaultRecipient();
        AddressImpl address = new AddressImpl();
        address.setCountry("Russia");
        address.setPostcode("131488");
        address.setCity("St. Petersburg");
        address.setSubway("Petrovso-Razumovskaya");
        address.setStreet("Pobedi");
        address.setHouse("13");
        address.setBlock("666");
        address.setEntrance("404");
        address.setEntryPhone("007");
        address.setFloor("8");
        address.setApartment("303");
        address.setPersonalAddressId(PERSONAL_ADDRESS_ID_ANOTHER);
        address.setPersonalGpsId(PERSONAL_GPS_ID_ANOTHER);
        address.setRecipient(recipient.getPerson().getFormattedName());
        address.setPhone(recipient.getPhone());
        address.setRecipientEmail(recipient.getEmail());
        address.setRecipientPerson(recipient.getPerson());
        address.setLanguage(AddressLanguage.ENG);
        return address;
    }

    public static Address getAddressWithoutPostcode() {
        Recipient recipient = RecipientProvider.getDefaultRecipient();
        AddressImpl shopAddress = new AddressImpl();
        shopAddress.setCountry("Русь");
        shopAddress.setCity("Питер");
        shopAddress.setSubway("Петровско-Разумовская");
        shopAddress.setStreet("Победы");
        shopAddress.setHouse("13");
        shopAddress.setBuilding("222");
        shopAddress.setBlock("666");
        shopAddress.setEntrance("404");
        shopAddress.setEntryPhone("007");
        shopAddress.setFloor("8");
        shopAddress.setApartment("303");
        shopAddress.setPersonalAddressId(PERSONAL_ADDRESS_ID_ANOTHER);
        shopAddress.setPersonalGpsId(PERSONAL_GPS_ID_ANOTHER);
        shopAddress.setRecipient(recipient.getPerson().getFormattedName());
        shopAddress.setPhone(recipient.getPhone());
        shopAddress.setPersonalPhoneId(recipient.getPersonalPhoneId());
        shopAddress.setRecipientEmail(recipient.getEmail());
        shopAddress.setPersonalEmailId(recipient.getPersonalEmailId());
        shopAddress.setRecipientPerson(recipient.getPerson());
        shopAddress.setPersonalFullNameId(recipient.getPersonalFullNameId());
        shopAddress.setLanguage(AddressLanguage.RUS);
        return shopAddress;
    }

    public static Address getAddressWithPreciseRegionId() {
        Recipient recipient = RecipientProvider.getDefaultRecipient();
        AddressImpl shopAddress = new AddressImpl();
        shopAddress.setCountry("Русь");
        shopAddress.setPostcode("131488");
        shopAddress.setCity("Питер");
        shopAddress.setDistrict("Московский район");
        shopAddress.setSubway("Петровско-Разумовская");
        shopAddress.setStreet("Победы");
        shopAddress.setHouse("13");
        shopAddress.setBuilding("222");
        shopAddress.setBlock("666");
        shopAddress.setEntrance("404");
        shopAddress.setEntryPhone("007");
        shopAddress.setFloor("8");
        shopAddress.setApartment("303");
        shopAddress.setPersonalAddressId(PERSONAL_ADDRESS_ID_ANOTHER);
        shopAddress.setPersonalGpsId(PERSONAL_GPS_ID_ANOTHER);
        shopAddress.setRecipient(recipient.getPerson().getFormattedName());
        shopAddress.setPhone(recipient.getPhone());
        shopAddress.setPersonalPhoneId(recipient.getPersonalPhoneId());
        shopAddress.setRecipientEmail(recipient.getEmail());
        shopAddress.setPersonalEmailId(recipient.getPersonalEmailId());
        shopAddress.setRecipientPerson(recipient.getPerson());
        shopAddress.setPersonalFullNameId(recipient.getPersonalFullNameId());
        shopAddress.setLanguage(AddressLanguage.RUS);
        shopAddress.setOutletName("Планета здоровья");
        shopAddress.setPreciseRegionId(225L);
        return shopAddress;
    }

}
