package ru.yandex.market.notifier.util.providers;

import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.AddressLanguage;
import ru.yandex.market.checkout.checkouter.delivery.AddressSource;
import ru.yandex.market.checkout.checkouter.delivery.Recipient;

public abstract class AddressProvider {

    public static Address getAddress() {
        return createAddressImpl();
    }

    private static AddressImpl createAddressImpl() {
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
        shopAddress.setRecipient(recipient.getPerson().getFormattedName());
        shopAddress.setPhone(recipient.getPhone());
        shopAddress.setRecipientEmail(recipient.getEmail());
        shopAddress.setRecipientPerson(recipient.getPerson());
        shopAddress.setLanguage(AddressLanguage.RUS);
        shopAddress.setOutletName("Планета здоровья");
        shopAddress.setAddressSource(AddressSource.NEW);
        return shopAddress;
    }
}
