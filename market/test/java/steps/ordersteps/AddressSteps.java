package steps.orderSteps;

import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.AddressLanguage;
import ru.yandex.market.checkout.checkouter.delivery.AddressType;

public class AddressSteps {
    private static final String POSTCODE = "630090";
    private static final String CITY = "Москва";
    private static final String SUBWAY = "Парк Культуры";
    private static final String STREET = "Льва Толстого";
    private static final String HOUSE = "15в/3";
    private static final String BLOCK = "422";
    private static final String ENTRANCE = "6";
    private static final String FLOOR = "4";
    private static final String APARTMENT = "22";
    private static final String RECIPIENT = "recipient";
    private static final String PHONE = "+70987654321";
    private static final AddressLanguage LANGUAGE = AddressLanguage.RUS;
    private static final String COUNTRY = "Россия";
    private static final String KM = "101";
    private static final String NOTES = "notes";
    private static final String ESTATE = "22";
    private static final String GPS = "32.416,76.589";
    private static final AddressType ADDRESS_TYPE = AddressType.SHOP;
    private static final String BUILDING = "345";
    private static final String ENTRY_PHONE = "+71234567809";

    private AddressSteps() {
    }

    public static AddressImpl getAddress() {
        AddressImpl address = new AddressImpl();

        address.setPostcode(POSTCODE);
        address.setCity(CITY);
        address.setSubway(SUBWAY);
        address.setStreet(STREET);
        address.setHouse(HOUSE);
        address.setBlock(BLOCK);
        address.setEntrance(ENTRANCE);
        address.setFloor(FLOOR);
        address.setApartment(APARTMENT);
        address.setRecipient(RECIPIENT);
        address.setPhone(PHONE);
        address.setLanguage(LANGUAGE);
        address.setCountry(COUNTRY);
        address.setKm(KM);
        address.setNotes(NOTES);
        address.setEstate(ESTATE);
        address.setGps(GPS);
        address.setType(ADDRESS_TYPE);
        address.setBuilding(BUILDING);
        address.setEntryPhone(ENTRY_PHONE);

        return address;
    }
}
