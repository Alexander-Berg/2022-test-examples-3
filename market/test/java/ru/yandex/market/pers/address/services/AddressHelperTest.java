package ru.yandex.market.pers.address.services;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.yandex.market.pers.address.controllers.model.AddressType;
import ru.yandex.market.pers.address.model.Address;
import ru.yandex.market.pers.address.util.AddressHelper;

import java.util.List;

public class AddressHelperTest {

    private final Address home = getAddress(42, "Россия", "СПб", "Пушкина", "55", "4", "15", "444", AddressType.HOME);
    private final Address work = getAddress(42, "Россия", "Москва", "Пушкина", "55", "4", "15", "444", AddressType.WORK);
    private final Address other = getAddress(42, "Россия", "СПб", "Пушкина", "13", "4", "6", "123", AddressType.OTHER);
    private final Address other2 = getAddress(42, "Россия", "Москва", "Пушкина", "55", null, "15", "444", AddressType.OTHER);
    private final Address other3 = getAddress(42, "Россия", "СПб", "Пушкина", "55", null, "15", "444", AddressType.OTHER);
    private final Address lastOrder = getAddress(42, "Россия", "СПб", "Пушкина", null, "4", null, "444", AddressType.LAST_ORDER);


    @Test
    public void shouldMergeAllDuplicatesIntoOneAddress() {
        List<Address> addresses = Lists.newArrayList(lastOrder, home, other3, home);
        List<Address> mergedAddresses = AddressHelper.mergeDuplicateAddresses(addresses);
        Assertions.assertEquals(1, mergedAddresses.size());
        Assertions.assertEquals(AddressType.HOME, mergedAddresses.get(0).getType());

        addresses = Lists.newArrayList(other2, work, work);
        mergedAddresses = AddressHelper.mergeDuplicateAddresses(addresses);
        Assertions.assertEquals(1, mergedAddresses.size());
        Assertions.assertEquals(AddressType.WORK, mergedAddresses.get(0).getType());
    }

    @Test
    public void shouldNotMergeDifferentAddresses() {
        List<Address> addresses = Lists.newArrayList(home, work, other);
        Assertions.assertEquals(3, AddressHelper.mergeDuplicateAddresses(addresses).size());
    }

    @Test
    public void shouldMergeOnlySameAddresses() {
        List<Address> addresses = Lists.newArrayList(home, work, lastOrder, other, home);
        List<Address> mergedAddresses = AddressHelper.mergeDuplicateAddresses(addresses);
        Assertions.assertEquals(3, mergedAddresses.size());
        Assertions.assertEquals(AddressType.HOME, mergedAddresses.get(0).getType());
        Assertions.assertEquals(AddressType.WORK, mergedAddresses.get(1).getType());
        Assertions.assertEquals(AddressType.OTHER, mergedAddresses.get(2).getType());


        addresses = Lists.newArrayList(other, home, lastOrder, home, other2, other3, lastOrder, work, home, work, other3);
        mergedAddresses = AddressHelper.mergeDuplicateAddresses(addresses);
        Assertions.assertEquals(3, mergedAddresses.size());
        Assertions.assertEquals(AddressType.OTHER, mergedAddresses.get(0).getType());
        Assertions.assertEquals(AddressType.HOME, mergedAddresses.get(1).getType());
        Assertions.assertEquals(AddressType.WORK, mergedAddresses.get(2).getType());
    }

    private static Address getAddress(Integer regionId, String country, String city, String street, String building,
                                      String floor, String room, String intercom, AddressType type) {
        return Address.builder()
                .setRegionId(regionId)
                .setCountry(country)
                .setCity(city)
                .setStreet(street)
                .setBuilding(building)
                .setFloor(floor)
                .setRoom(room)
                .setIntercom(intercom)
                .setType(type)
                .build();
    }


}
