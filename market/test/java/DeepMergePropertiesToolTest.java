package ru.yandex.market.javaframework.internal.beanfactory.mapbyqualifier;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import ru.yandex.market.javaframework.internal.properties.merge.DeepMergePropertiesTool;

import static org.assertj.core.api.Assertions.assertThat;

public class DeepMergePropertiesToolTest {

    @Test
    public void mergeObjectsTest() {
        final List<String> hobbies1 = List.of("music", "walk", "football");
        final List<String> hobbies2 = List.of("tennis", "cooking", "tv", "science");

        final String firstName1 = "Vasya";
        final String lastName1 = null;
        final String firstName2 = null;
        final String lastName2 = "Ivanov";

        final String city1 = "Moscow";
        final String street1 = "Tverskaya";
        final int house1 = 32;
        final String city2 = "Spb";
        final String street2 = null;
        final int house2 = 21;

        final Address address1 = new Address(city1, street1, house1);
        final Address address2 = new Address(city2, street2, house2);

        final Profile person1 = new Profile(firstName1, lastName1, hobbies1, address1);
        final Profile person2 = new Profile(firstName2, lastName2, hobbies2, address2);

        final DeepMergePropertiesTool mergeTool = new DeepMergePropertiesTool();

        // merge lists, without ignoring
        final Profile mergedProfile1 = mergeTool.merge(person1, person2);
        assertThat(mergedProfile1.firstName).isEqualTo(firstName1);
        assertThat(mergedProfile1.lastName).isEqualTo(lastName2);
        assertThat(mergedProfile1.hobbies).containsAll(hobbies1).containsAll(hobbies2);
        assertThat(mergedProfile1.address.city).isEqualTo(city2);
        assertThat(mergedProfile1.address.street).isEqualTo(street1);
        assertThat(mergedProfile1.address.house).isEqualTo(house2);

        // don't merge lists, without ignoring -> contains only hobbies2
        final Profile mergedProfile2 = mergeTool.merge(person1, person2, false, false);
        assertThat(mergedProfile2.firstName).isEqualTo(firstName1);
        assertThat(mergedProfile2.lastName).isEqualTo(lastName2);
        assertThat(mergedProfile2.hobbies).doesNotContainAnyElementsOf(hobbies1).containsAll(hobbies2);
        assertThat(mergedProfile2.address.city).isEqualTo(city2);
        assertThat(mergedProfile2.address.street).isEqualTo(street1);
        assertThat(mergedProfile1.address.house).isEqualTo(house2);

        // merge lists, ignore merge address -> moscow street doesn't merges even if spb street is null
        final Profile mergedProfile3 = mergeTool.merge(person1, person2, true, true,"/address");
        assertThat(mergedProfile3.firstName).isEqualTo(firstName1);
        assertThat(mergedProfile3.lastName).isEqualTo(lastName2);
        assertThat(mergedProfile3.hobbies).containsAll(hobbies1).containsAll(hobbies2);
        assertThat(mergedProfile3.address.city).isEqualTo(city2);
        assertThat(mergedProfile3.address.street).isEqualTo(street2);
        assertThat(mergedProfile3.address.house).isEqualTo(house2);
    }

    @Test
    public void mergeObjectsInListsTest() {
        final String city1 = "Moscow";
        final String street1 = "Tverskaya";
        final int house1 = 32;
        final String city2 = "Spb";
        final String street2 = null;
        final int house2 = 21;
        final String city3 = "Sochi";
        final String street3 = "Lenina";
        final int house3 = 15;

        final Address address1 = new Address(city1, street1, house1);
        final Address address2 = new Address(city2, street2, house2);
        final Address address3 = new Address(city3, street3, house3);

        final AddressBook addressBook1 = new AddressBook(List.of(address1, address2));
        final AddressBook addressBook2 = new AddressBook(List.of(address3));

        final DeepMergePropertiesTool mergeTool = new DeepMergePropertiesTool();

        final AddressBook merged1 = mergeTool.merge(addressBook1, addressBook2);
        assertThat(merged1.addresses.size()).isEqualTo(3);
        assertThat(merged1.addresses.get(0).city).isEqualTo(city1);
        assertThat(merged1.addresses.get(0).street).isEqualTo(street1);
        assertThat(merged1.addresses.get(0).house).isEqualTo(house1);
        assertThat(merged1.addresses.get(1).city).isEqualTo(city2);
        assertThat(merged1.addresses.get(1).street).isEqualTo(street2);
        assertThat(merged1.addresses.get(1).house).isEqualTo(house2);
        assertThat(merged1.addresses.get(2).city).isEqualTo(city3);
        assertThat(merged1.addresses.get(2).street).isEqualTo(street3);
        assertThat(merged1.addresses.get(2).house).isEqualTo(house3);

        final AddressBook merged2 = mergeTool.merge(addressBook1, addressBook2, true, false);
        assertThat(merged2.addresses.size()).isEqualTo(3);
        assertThat(merged2.addresses.get(0).city).isEqualTo(city1);
        assertThat(merged2.addresses.get(0).street).isEqualTo(street1);
        assertThat(merged2.addresses.get(0).house).isEqualTo(house1);
        assertThat(merged2.addresses.get(1).city).isEqualTo(city2);
        assertThat(merged2.addresses.get(1).street).isEqualTo(street2);
        assertThat(merged2.addresses.get(1).house).isEqualTo(house2);
        assertThat(merged2.addresses.get(2).city).isEqualTo(city3);
        assertThat(merged2.addresses.get(2).street).isEqualTo(street3);
        assertThat(merged2.addresses.get(2).house).isEqualTo(house3);

        final AddressBook merged3 = mergeTool.merge(addressBook1, addressBook2, false, true);
        assertThat(merged3.addresses.size()).isEqualTo(2);
        assertThat(merged3.addresses.get(0).city).isEqualTo(city3);
        assertThat(merged3.addresses.get(0).street).isEqualTo(street3);
        assertThat(merged3.addresses.get(0).house).isEqualTo(house3);
        assertThat(merged3.addresses.get(1).city).isEqualTo(city2);
        assertThat(merged3.addresses.get(1).street).isEqualTo(street2);
        assertThat(merged3.addresses.get(1).house).isEqualTo(house2);

        final AddressBook merged4 = mergeTool.merge(addressBook1, addressBook2, false, false);
        assertThat(merged4.addresses.size()).isEqualTo(1);
        assertThat(merged4.addresses.get(0).city).isEqualTo(city3);
        assertThat(merged4.addresses.get(0).street).isEqualTo(street3);
        assertThat(merged4.addresses.get(0).house).isEqualTo(house3);
    }

    @Test
    public void mergeObjectsInLists_FirstListIsNull_Test() {
        final String city1 = "Moscow";
        final String street1 = "Tverskaya";
        final int house1 = 32;
        final String city2 = "Spb";
        final String street2 = null;
        final int house2 = 21;
        final String city3 = "Sochi";
        final String street3 = "Lenina";
        final int house3 = 15;

        final Address address1 = new Address(city1, street1, house1);
        final Address address2 = new Address(city2, street2, house2);
        final Address address3 = new Address(city3, street3, house3);

        final AddressBook addressBook1 = new AddressBook(null);
        final AddressBook addressBook2 = new AddressBook(List.of(address3));

        final DeepMergePropertiesTool mergeTool = new DeepMergePropertiesTool();

        final AddressBook merged1 = mergeTool.merge(addressBook1, addressBook2);
        assertThat(merged1.addresses.size()).isEqualTo(1);
        assertThat(merged1.addresses.get(0).city).isEqualTo(city3);
        assertThat(merged1.addresses.get(0).street).isEqualTo(street3);
        assertThat(merged1.addresses.get(0).house).isEqualTo(house3);

        final AddressBook merged2 = mergeTool.merge(addressBook1, addressBook2, true, false);
        assertThat(merged2.addresses.size()).isEqualTo(1);
        assertThat(merged2.addresses.get(0).city).isEqualTo(city3);
        assertThat(merged2.addresses.get(0).street).isEqualTo(street3);
        assertThat(merged2.addresses.get(0).house).isEqualTo(house3);

        final AddressBook merged3 = mergeTool.merge(addressBook1, addressBook2, false, true);
        assertThat(merged3.addresses.size()).isEqualTo(1);
        assertThat(merged3.addresses.get(0).city).isEqualTo(city3);
        assertThat(merged3.addresses.get(0).street).isEqualTo(street3);
        assertThat(merged3.addresses.get(0).house).isEqualTo(house3);

        final AddressBook merged4 = mergeTool.merge(addressBook1, addressBook2, false, false);
        assertThat(merged4.addresses.size()).isEqualTo(1);
        assertThat(merged4.addresses.get(0).city).isEqualTo(city3);
        assertThat(merged4.addresses.get(0).street).isEqualTo(street3);
        assertThat(merged4.addresses.get(0).house).isEqualTo(house3);
    }

    @Test
    public void mergeObjectsInLists_SecondListIsNull_Test() {
        final String city1 = "Moscow";
        final String street1 = "Tverskaya";
        final int house1 = 32;
        final String city2 = "Spb";
        final String street2 = null;
        final int house2 = 21;
        final String city3 = "Sochi";
        final String street3 = "Lenina";
        final int house3 = 15;

        final Address address1 = new Address(city1, street1, house1);
        final Address address2 = new Address(city2, street2, house2);
        final Address address3 = new Address(city3, street3, house3);

        final AddressBook addressBook1 = new AddressBook(List.of(address1, address2));
        final AddressBook addressBook2 = new AddressBook(null);

        final DeepMergePropertiesTool mergeTool = new DeepMergePropertiesTool();

        final AddressBook merged1 = mergeTool.merge(addressBook1, addressBook2);
        assertThat(merged1.addresses.size()).isEqualTo(2);
        assertThat(merged1.addresses.get(0).city).isEqualTo(city1);
        assertThat(merged1.addresses.get(0).street).isEqualTo(street1);
        assertThat(merged1.addresses.get(0).house).isEqualTo(house1);
        assertThat(merged1.addresses.get(1).city).isEqualTo(city2);
        assertThat(merged1.addresses.get(1).street).isEqualTo(street2);
        assertThat(merged1.addresses.get(1).house).isEqualTo(house2);

        final AddressBook merged2 = mergeTool.merge(addressBook1, addressBook2, true, false);
        assertThat(merged2.addresses.size()).isEqualTo(2);
        assertThat(merged2.addresses.get(0).city).isEqualTo(city1);
        assertThat(merged2.addresses.get(0).street).isEqualTo(street1);
        assertThat(merged2.addresses.get(0).house).isEqualTo(house1);
        assertThat(merged2.addresses.get(1).city).isEqualTo(city2);
        assertThat(merged2.addresses.get(1).street).isEqualTo(street2);
        assertThat(merged2.addresses.get(1).house).isEqualTo(house2);

        final AddressBook merged3 = mergeTool.merge(addressBook1, addressBook2, false, true);
        assertThat(merged3.addresses.size()).isEqualTo(2);
        assertThat(merged3.addresses.get(0).city).isEqualTo(city1);
        assertThat(merged3.addresses.get(0).street).isEqualTo(street1);
        assertThat(merged3.addresses.get(0).house).isEqualTo(house1);
        assertThat(merged3.addresses.get(1).city).isEqualTo(city2);
        assertThat(merged3.addresses.get(1).street).isEqualTo(street2);
        assertThat(merged3.addresses.get(1).house).isEqualTo(house2);

        final AddressBook merged4 = mergeTool.merge(addressBook1, addressBook2, false, false);
        assertThat(merged4.addresses.size()).isEqualTo(2);
        assertThat(merged4.addresses.get(0).city).isEqualTo(city1);
        assertThat(merged4.addresses.get(0).street).isEqualTo(street1);
        assertThat(merged4.addresses.get(0).house).isEqualTo(house1);
        assertThat(merged4.addresses.get(1).city).isEqualTo(city2);
        assertThat(merged4.addresses.get(1).street).isEqualTo(street2);
        assertThat(merged4.addresses.get(1).house).isEqualTo(house2);
    }

    @Test
    public void mergeObjectsInLists_UpdateListIsLarger_Test() {
        final String city1 = "Moscow";
        final String street1 = "Tverskaya";
        final int house1 = 32;
        final String city2 = "Spb";
        final String street2 = null;
        final int house2 = 21;
        final String city3 = "Sochi";
        final String street3 = "Lenina";
        final int house3 = 15;

        final Address address1 = new Address(city1, street1, house1);
        final Address address2 = new Address(city2, street2, house2);
        final Address address3 = new Address(city3, street3, house3);

        final AddressBook addressBook1 = new AddressBook(List.of(address1));
        final AddressBook addressBook2 = new AddressBook(List.of(address2, address3));

        final DeepMergePropertiesTool mergeTool = new DeepMergePropertiesTool();

        final AddressBook merged1 = mergeTool.merge(addressBook1, addressBook2);
        assertThat(merged1.addresses.size()).isEqualTo(3);
        assertThat(merged1.addresses.get(0).city).isEqualTo(city1);
        assertThat(merged1.addresses.get(0).street).isEqualTo(street1);
        assertThat(merged1.addresses.get(0).house).isEqualTo(house1);
        assertThat(merged1.addresses.get(1).city).isEqualTo(city2);
        assertThat(merged1.addresses.get(1).street).isEqualTo(street2);
        assertThat(merged1.addresses.get(1).house).isEqualTo(house2);
        assertThat(merged1.addresses.get(2).city).isEqualTo(city3);
        assertThat(merged1.addresses.get(2).street).isEqualTo(street3);
        assertThat(merged1.addresses.get(2).house).isEqualTo(house3);

        final AddressBook merged2 = mergeTool.merge(addressBook1, addressBook2, true, false);
        assertThat(merged2.addresses.size()).isEqualTo(3);
        assertThat(merged2.addresses.get(0).city).isEqualTo(city1);
        assertThat(merged2.addresses.get(0).street).isEqualTo(street1);
        assertThat(merged2.addresses.get(0).house).isEqualTo(house1);
        assertThat(merged2.addresses.get(1).city).isEqualTo(city2);
        assertThat(merged2.addresses.get(1).street).isEqualTo(street2);
        assertThat(merged2.addresses.get(1).house).isEqualTo(house2);
        assertThat(merged2.addresses.get(2).city).isEqualTo(city3);
        assertThat(merged2.addresses.get(2).street).isEqualTo(street3);
        assertThat(merged2.addresses.get(2).house).isEqualTo(house3);

        final AddressBook merged3 = mergeTool.merge(addressBook1, addressBook2, false, true);
        assertThat(merged3.addresses.size()).isEqualTo(2);
        assertThat(merged3.addresses.get(0).city).isEqualTo(city2);
        // spb street is null, moscow street merged
        assertThat(merged3.addresses.get(0).street).isEqualTo(street1);
        assertThat(merged3.addresses.get(0).house).isEqualTo(house2);
        assertThat(merged3.addresses.get(1).city).isEqualTo(city3);
        assertThat(merged3.addresses.get(1).street).isEqualTo(street3);
        assertThat(merged3.addresses.get(1).house).isEqualTo(house3);

        final AddressBook merged4 = mergeTool.merge(addressBook1, addressBook2, false, false);
        assertThat(merged4.addresses.size()).isEqualTo(2);
        assertThat(merged4.addresses.get(0).city).isEqualTo(city2);
        // spb street is null, but moscow street not merged
        assertThat(merged4.addresses.get(0).street).isEqualTo(street2);
        assertThat(merged4.addresses.get(0).house).isEqualTo(house2);
        assertThat(merged4.addresses.get(1).city).isEqualTo(city3);
        assertThat(merged4.addresses.get(1).street).isEqualTo(street3);
        assertThat(merged4.addresses.get(1).house).isEqualTo(house3);

        // TODO fix
        /*final AddressBook merged5 = mergeTool.merge(addressBook1, addressBook2, false, true, "/addresses/street");
        assertThat(merged5.addresses.size()).isEqualTo(2);
        assertThat(merged5.addresses.get(0).city).isEqualTo(city2);
        // spb street is null, moscow street merged
        assertThat(merged5.addresses.get(0).street).isEqualTo(street2);
        assertThat(merged5.addresses.get(0).house).isEqualTo(house2);
        assertThat(merged5.addresses.get(1).city).isEqualTo(city3);
        assertThat(merged5.addresses.get(1).street).isEqualTo(street3);
        assertThat(merged5.addresses.get(1).house).isEqualTo(house3);*/
    }

    private static class AddressBook {
        private List<Address> addresses;

        public AddressBook() {
        }

        public AddressBook(List<Address> addresses) {
            this.addresses = addresses;
        }

        public List<Address> getAddresses() {
            return addresses;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AddressBook that = (AddressBook) o;
            return Objects.equals(addresses, that.addresses);
        }

        @Override
        public int hashCode() {
            return Objects.hash(addresses);
        }
    }

    private static class Address {
        private String city;
        private String street;
        private Integer house;

        public Address() {
        }

        public Address(String city, String street, Integer house) {
            this.city = city;
            this.street = street;
            this.house = house;
        }

        public String getCity() {
            return city;
        }

        public String getStreet() {
            return street;
        }

        public Integer getHouse() {
            return house;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Address address = (Address) o;
            return Objects.equals(city, address.city) && Objects.equals(street, address.street) && Objects.equals(house, address.house);
        }

        @Override
        public int hashCode() {
            return Objects.hash(city, street, house);
        }
    }

    private static class Profile {
        private String firstName;
        private String lastName;
        private List<String> hobbies;
        private Address address;

        public Profile() {
        }

        public Profile(String firstName, String lastName, List<String> hobbies, Address address) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.hobbies = hobbies;
            this.address = address;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public List<String> getHobbies() {
            return hobbies;
        }

        public Address getAddress() {
            return address;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Profile profile = (Profile) o;
            return Objects.equals(firstName, profile.firstName) && Objects.equals(lastName, profile.lastName) && Objects.equals(hobbies, profile.hobbies) && Objects.equals(address, profile.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(firstName, lastName, hobbies, address);
        }
    }
}
