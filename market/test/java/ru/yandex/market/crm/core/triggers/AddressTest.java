package ru.yandex.market.crm.core.triggers;

import org.junit.Test;

import ru.yandex.market.crm.triggers.services.bpm.variables.Address;

import static org.junit.Assert.assertEquals;

public class AddressTest {
    @Test
    public void addressFormattedTest() {
        Address address = new Address();
        address.setApartment("123");
        address.setFloor("7");
        address.setEntrance("34");
        address.setEstate("44");
        address.setPostcode("644106");
        address.setBlock("1");
        address.setBuilding("2");
        address.setHouse("13а");
        address.setStreet("пр-кт Маркса");
        address.setCity("г.Омск");
        address.setCountry("Россия");
        address.setEntryPhone("3");
        address.setKm("55");
        address.setScheduleString("с 8 до 17");
        assertEquals("644106, г.Омск, пр-кт Маркса, км. 55, д. 13а, владение 44, корп. 1, стр. 2, кв. 123, " +
                "вход 34, этаж 7, домофон 3", address.formattedAddress());
    }

}
