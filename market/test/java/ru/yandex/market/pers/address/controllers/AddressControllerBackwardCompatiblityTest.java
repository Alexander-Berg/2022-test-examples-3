package ru.yandex.market.pers.address.controllers;

import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.address.config.TestClient;
import ru.yandex.market.pers.address.controllers.model.AddressDtoResponse;
import ru.yandex.market.pers.address.controllers.model.AddressType;
import ru.yandex.market.pers.address.controllers.model.ContactDto;
import ru.yandex.market.pers.address.controllers.model.ContactDtoResponse;
import ru.yandex.market.pers.address.controllers.model.NewAddressDtoRequest;
import ru.yandex.market.pers.address.controllers.model.NewPresetDtoRequest;
import ru.yandex.market.pers.address.controllers.model.PresetsResponse;
import ru.yandex.market.pers.address.dao.ObjectKey;
import ru.yandex.market.pers.address.factories.AddressDtoFactory;
import ru.yandex.market.pers.address.factories.ContactDtoFactory;
import ru.yandex.market.pers.address.factories.PresetDtoFactory;
import ru.yandex.market.pers.address.factories.TestPlatform;
import ru.yandex.market.pers.address.model.identity.Identity;
import ru.yandex.market.pers.address.util.BaseWebTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;

public class AddressControllerBackwardCompatiblityTest extends BaseWebTest {
    @Autowired
    private TestClient testClient;

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
    void shouldReturnDistinctContacts(
            @ConvertWith(AddressControllerTest.ToPlatform.class) TestPlatform platform,
            @ConvertWith(AddressControllerTest.ToIdentity.class) Identity<?> identity
    ) throws Exception {
        NewPresetDtoRequest toSave = PresetDtoFactory.tolstogoStreet();
        testClient.addPreset(identity, toSave, platform);

        NewPresetDtoRequest toSave2 = PresetDtoFactory.tolstogoStreetAnotherRoom();
        testClient.addPreset(identity, toSave2, platform);

        List<ContactDtoResponse> contacts = testClient.getContacts(identity, platform);

        MatcherAssert.assertThat(contacts, hasSize(1));
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
    void shouldReturnAllContactsIfCreatedFrom20Api(
            @ConvertWith(AddressControllerTest.ToPlatform.class) TestPlatform platform,
            @ConvertWith(AddressControllerTest.ToIdentity.class) Identity<?> identity
    ) throws Exception {
        ContactDto contactDto = ContactDtoFactory.sample();

        testClient.addContact(identity, contactDto, platform);
        testClient.addContact(identity, contactDto, platform);

        List<ContactDtoResponse> contacts = testClient.getContacts(identity, platform);

        MatcherAssert.assertThat(contacts, hasSize(2));
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
    void shouldNotDeletePresetIfContactIsDeleted(
            @ConvertWith(AddressControllerTest.ToPlatform.class) TestPlatform platform,
            @ConvertWith(AddressControllerTest.ToIdentity.class) Identity<?> identity
    ) throws Exception {
        NewPresetDtoRequest toSave = PresetDtoFactory.tolstogoStreet();
        testClient.addPreset(identity, toSave, platform);

        List<ContactDtoResponse> contacts = testClient.getContacts(identity, platform);
        MatcherAssert.assertThat(contacts, hasSize(1));

        String contactId = contacts.get(0).getContactId();

        testClient.deleteContact(identity, new ObjectKey(contactId));

        PresetsResponse presets = testClient.getPresets(identity, platform);
        MatcherAssert.assertThat(presets.getPresets(), hasSize(1));

        contacts = testClient.getContacts(identity, platform);
        MatcherAssert.assertThat(contacts, Matchers.empty());
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
    void shouldNotDeletePresetIfAddressIsDeleted(
            @ConvertWith(AddressControllerTest.ToPlatform.class) TestPlatform platform,
            @ConvertWith(AddressControllerTest.ToIdentity.class) Identity<?> identity
    ) throws Exception {
        NewPresetDtoRequest toSave = PresetDtoFactory.tolstogoStreet();
        testClient.addPreset(identity, toSave, platform);

        List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platform);
        MatcherAssert.assertThat(addresses, hasSize(1));

        String addressId = addresses.get(0).getAddressId();

        testClient.deleteAddress(identity, new ObjectKey(addressId));

        PresetsResponse presets = testClient.getPresets(identity, platform);
        MatcherAssert.assertThat(presets.getPresets(), hasSize(1));

        addresses = testClient.getAddresses(identity, platform);
        MatcherAssert.assertThat(addresses, Matchers.empty());
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
    void shouldDeleteContactByValue(
            @ConvertWith(AddressControllerTest.ToPlatform.class) TestPlatform platform,
            @ConvertWith(AddressControllerTest.ToIdentity.class) Identity<?> identity
    ) throws Exception {
        NewPresetDtoRequest toSave1 = PresetDtoFactory.tolstogoStreet();
        testClient.addPreset(identity, toSave1, platform);

        NewPresetDtoRequest toSave2 = PresetDtoFactory.tolstogoStreetAnotherRoom();
        testClient.addPreset(identity, toSave2, platform);

        List<ContactDtoResponse> contacts = testClient.getContacts(identity, platform);
        MatcherAssert.assertThat(contacts, hasSize(1));

        testClient.deleteContact(identity, new ObjectKey(contacts.get(0).getContactId()));

        contacts = testClient.getContacts(identity, platform);
        MatcherAssert.assertThat(contacts, Matchers.empty());
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
    void shouldEditAddressByCopyOnWrite(
            @ConvertWith(AddressControllerTest.ToPlatform.class) TestPlatform platform,
            @ConvertWith(AddressControllerTest.ToIdentity.class) Identity<?> identity
    ) throws Exception {
        NewPresetDtoRequest toSave1 = PresetDtoFactory.tolstogoStreet();
        testClient.addPreset(identity, toSave1, platform);

        PresetsResponse presetsResponse = testClient.getPresets(identity, platform);
        MatcherAssert.assertThat(presetsResponse.getPresets(), hasSize(1));
        List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platform);
        MatcherAssert.assertThat(addresses, hasSize(1));
        String addressId = addresses.get(0).getAddressId();

        NewAddressDtoRequest tolstogoStreetAnotherRoomAddress = AddressDtoFactory.tolstogoStreetAnotherRoom().build();
        testClient.updateAddress(identity, new ObjectKey(addressId), platform, tolstogoStreetAnotherRoomAddress);

        presetsResponse = testClient.getPresets(identity, platform);
        MatcherAssert.assertThat(presetsResponse.getPresets(), hasSize(1));

        NewAddressDtoRequest tolstogoStAddress = toSave1.getAddress();
        assertThat(presetsResponse.getAddresses(), everyItem(allOf(
                hasProperty("regionId", equalTo(213)),
                hasProperty("city", equalTo(tolstogoStAddress.getCity())),
                hasProperty("country", equalTo(tolstogoStAddress.getCountry())),
                hasProperty("entrance", equalTo(tolstogoStAddress.getEntrance())),
                hasProperty("floor", equalTo(tolstogoStAddress.getFloor())),
                hasProperty("street", equalTo(tolstogoStAddress.getStreet())),
                hasProperty("type", equalTo(AddressType.OTHER))
        )));

        addresses = testClient.getAddresses(identity, platform);

        MatcherAssert.assertThat(addresses, hasSize(1));
        assertThat(addresses, everyItem(allOf(
                hasProperty("regionId", equalTo(213)),
                hasProperty("city", equalTo(tolstogoStreetAnotherRoomAddress.getCity())),
                hasProperty("country", equalTo(tolstogoStreetAnotherRoomAddress.getCountry())),
                hasProperty("entrance", equalTo(tolstogoStreetAnotherRoomAddress.getEntrance())),
                hasProperty("floor", equalTo(tolstogoStreetAnotherRoomAddress.getFloor())),
                hasProperty("street", equalTo(tolstogoStreetAnotherRoomAddress.getStreet())),
                hasProperty("type", equalTo(AddressType.OTHER))
        )));
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
    void shouldEditAddressByCopyOnWrite2(
            @ConvertWith(AddressControllerTest.ToPlatform.class) TestPlatform platform,
            @ConvertWith(AddressControllerTest.ToIdentity.class) Identity<?> identity
    ) throws Exception {
        NewPresetDtoRequest toSave1 = PresetDtoFactory.tolstogoStreet();
        testClient.addPreset(identity, toSave1, platform);

        PresetsResponse presetsResponse = testClient.getPresets(identity, platform);
        MatcherAssert.assertThat(presetsResponse.getPresets(), hasSize(1));
        List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platform);
        MatcherAssert.assertThat(addresses, hasSize(1));
        String addressId = addresses.get(0).getAddressId();

        NewAddressDtoRequest tolstogoStreetAnotherRoomAddress = AddressDtoFactory.tolstogoStreetAnotherRoom().build();
        testClient.updateAddress(identity, new ObjectKey(addressId), platform, tolstogoStreetAnotherRoomAddress);

        addresses = testClient.getAddresses(identity, platform);
        MatcherAssert.assertThat(addresses, hasSize(1));
        String newAddressId = addresses.get(0).getAddressId();
        MatcherAssert.assertThat(addressId, not(newAddressId));

        NewAddressDtoRequest tolstogoStreetAnotherRoomAddress2 = AddressDtoFactory.tolstogoStreetAnotherRoom()
                .setRoom("300")
                .build();
        testClient.updateAddress(identity, new ObjectKey(newAddressId), platform, tolstogoStreetAnotherRoomAddress2);

        List<AddressDtoResponse> clientAddresses = testClient.getAddresses(identity, platform);
        MatcherAssert.assertThat(clientAddresses, hasSize(1));
        MatcherAssert.assertThat(clientAddresses.get(0).getRoom(), is("300"));
        MatcherAssert.assertThat(clientAddresses.get(0).getAddressId(), is(newAddressId));

        PresetsResponse presets = testClient.getPresets(identity, platform);
        MatcherAssert.assertThat(presets.getAddresses(), hasSize(1));
        MatcherAssert.assertThat(presets.getAddresses().get(0).getRoom(), is("109"));
        MatcherAssert.assertThat(presets.getAddresses().get(0).getAddressId(), is(addressId));
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
    void shouldEditContactByCopyOnWrite(
            @ConvertWith(AddressControllerTest.ToPlatform.class) TestPlatform platform,
            @ConvertWith(AddressControllerTest.ToIdentity.class) Identity<?> identity
    ) throws Exception {
        NewPresetDtoRequest toSave1 = PresetDtoFactory.tolstogoStreet();
        testClient.addPreset(identity, toSave1, platform);

        PresetsResponse presetsResponse = testClient.getPresets(identity, platform);
        MatcherAssert.assertThat(presetsResponse.getPresets(), hasSize(1));
        List<ContactDtoResponse> contacts = testClient.getContacts(identity, platform);
        MatcherAssert.assertThat(contacts, hasSize(1));
        String contactId = contacts.get(0).getContactId();

        ContactDto sample = ContactDtoFactory.anotherSample();
        testClient.updateContact(identity, new ObjectKey(contactId), platform, sample);

        contacts = testClient.getContacts(identity, platform);
        MatcherAssert.assertThat(contacts, hasSize(1));
        String newContactId = contacts.get(0).getContactId();
        MatcherAssert.assertThat(contactId, not(newContactId));

        ContactDto sampleAnotherName = ContactDto.contactDtoBuilder()
                .setEmail("anotherUser@yandex-team.ru")
                .setRecipient("Иван Петрович Иванов")
                .setPhoneNum("+79651357954")
                .build();
        testClient.updateContact(identity, new ObjectKey(newContactId), platform, sampleAnotherName);

        List<ContactDtoResponse> clientAddresses = testClient.getContacts(identity, platform);
        MatcherAssert.assertThat(clientAddresses, hasSize(1));
        MatcherAssert.assertThat(clientAddresses.get(0).getRecipient(), is("Иван Петрович Иванов"));
        MatcherAssert.assertThat(clientAddresses.get(0).getContactId(), is(newContactId));

        PresetsResponse presets = testClient.getPresets(identity, platform);
        MatcherAssert.assertThat(presets.getContacts(), hasSize(1));
        MatcherAssert.assertThat(presets.getContacts().get(0).getRecipient(), is("Иван Иванович Иванов"));
        MatcherAssert.assertThat(presets.getContacts().get(0).getContactId(), is(contactId));
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
    void shouldDeleteContactByValueWhenPresetsAndContactsAreMixed(
            @ConvertWith(AddressControllerTest.ToPlatform.class) TestPlatform platform,
            @ConvertWith(AddressControllerTest.ToIdentity.class) Identity<?> identity
    ) throws Exception {
        NewPresetDtoRequest newPresetDtoRequest = PresetDtoFactory.tolstogoStreet();
        ContactDto newContactDto = newPresetDtoRequest.getContact();

        testClient.addPreset(identity, newPresetDtoRequest, platform);

        ObjectKey contactId = new ObjectKey(testClient.getPresets(identity, platform).getContacts().get(0).getContactId());

        NewPresetDtoRequest newPresetDtoRequest2 = PresetDtoFactory.tolstogoStreetAnotherRoom();

        testClient.addPreset(identity, newPresetDtoRequest2, platform);

        MatcherAssert.assertThat(testClient.getPresets(identity, platform).getPresets(), hasSize(2));

        testClient.addContact(identity, newContactDto, platform);

        testClient.deleteContact(identity, contactId);

        PresetsResponse presets = testClient.getPresets(identity, platform);
        MatcherAssert.assertThat(presets.getPresets(), hasSize(2));
        MatcherAssert.assertThat(presets.getContacts(), hasSize(1));

        List<ContactDtoResponse> contacts = testClient.getContacts(identity, platform);
        MatcherAssert.assertThat(contacts, hasSize(1));
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
    void shouldDeleteAddressByValueWhenPresetsAndContactsAreMixed(
            @ConvertWith(AddressControllerTest.ToPlatform.class) TestPlatform platform,
            @ConvertWith(AddressControllerTest.ToIdentity.class) Identity<?> identity
    ) throws Exception {
        NewPresetDtoRequest newPresetDtoRequest = PresetDtoFactory.tolstogoStreet();
        NewAddressDtoRequest newAddressDtoRequest = newPresetDtoRequest.getAddress();

        testClient.addPreset(identity, newPresetDtoRequest, platform);
        ObjectKey addressId = new ObjectKey(testClient.getPresets(identity, platform).getAddresses().get(0).getAddressId());

        NewPresetDtoRequest newPresetDtoRequest2 = NewPresetDtoRequest.builder()
                .setAddress(AddressDtoFactory.tolstogoStreet().build())
                .setContact(ContactDtoFactory.anotherSample())
                .build();

        testClient.addPreset(identity, newPresetDtoRequest2, platform);

        testClient.addAddress(identity, newAddressDtoRequest, platform);

        List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platform);
        MatcherAssert.assertThat(addresses, hasSize(2));

        testClient.deleteAddress(identity, addressId);

        PresetsResponse presets = testClient.getPresets(identity, platform);
        MatcherAssert.assertThat(presets.getPresets(), hasSize(2));
        MatcherAssert.assertThat(presets.getAddresses(), hasSize(1));

        addresses = testClient.getAddresses(identity, platform);
        MatcherAssert.assertThat(addresses, hasSize(1));
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
    void editedAddressesShouldNotRessurect(
            @ConvertWith(AddressControllerTest.ToPlatform.class) TestPlatform platform,
            @ConvertWith(AddressControllerTest.ToIdentity.class) Identity<?> identity
    ) throws Exception {
        NewPresetDtoRequest tolstogoStreetPreset = PresetDtoFactory.tolstogoStreet();
        NewAddressDtoRequest tolstogoStAddress = tolstogoStreetPreset.getAddress();

        ObjectKey addressId = testClient.addAddress(identity, tolstogoStAddress, platform);

        testClient.addAddress(identity, tolstogoStAddress, platform);

        testClient.addPreset(identity, tolstogoStreetPreset, platform);

        List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platform);
        MatcherAssert.assertThat(addresses, hasSize(3));

        NewAddressDtoRequest gruzinskayaStAddress = AddressDtoFactory.gruzinskayaStreet().build();
        testClient.updateAddress(identity, addressId, platform, gruzinskayaStAddress);
        addresses = testClient.getAddresses(identity, platform);

        MatcherAssert.assertThat(addresses, hasSize(3));
        MatcherAssert.assertThat(addresses, hasItem(allOf(hasProperty("regionId", equalTo(213)),
                hasProperty("city", equalTo(gruzinskayaStAddress.getCity())),
                hasProperty("country", equalTo(gruzinskayaStAddress.getCountry())),
                hasProperty("entrance", equalTo(gruzinskayaStAddress.getEntrance())),
                hasProperty("floor", equalTo(gruzinskayaStAddress.getFloor())),
                hasProperty("street", equalTo(gruzinskayaStAddress.getStreet())),
                hasProperty("type", equalTo(AddressType.OTHER))
        )));

        PresetsResponse presets = testClient.getPresets(identity, platform);
        MatcherAssert.assertThat(presets.getPresets(), hasSize(1));
        addresses = presets.getAddresses();
        MatcherAssert.assertThat(addresses, hasSize(1));

        MatcherAssert.assertThat(addresses.get(0), allOf(hasProperty("regionId", equalTo(213)),
                hasProperty("city", equalTo(tolstogoStAddress.getCity())),
                hasProperty("country", equalTo(tolstogoStAddress.getCountry())),
                hasProperty("entrance", equalTo(tolstogoStAddress.getEntrance())),
                hasProperty("floor", equalTo(tolstogoStAddress.getFloor())),
                hasProperty("street", equalTo(tolstogoStAddress.getStreet())),
                hasProperty("type", equalTo(AddressType.OTHER))
        ));
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
    void editedAddressesShouldNotRessurect2(
            @ConvertWith(AddressControllerTest.ToPlatform.class) TestPlatform platform,
            @ConvertWith(AddressControllerTest.ToIdentity.class) Identity<?> identity
    ) throws Exception {
        NewPresetDtoRequest tolstogoStreetPreset = PresetDtoFactory.tolstogoStreet();
        NewAddressDtoRequest tolstogoStAddress = tolstogoStreetPreset.getAddress();

        testClient.addAddress(identity, tolstogoStAddress, platform);
        testClient.addAddress(identity, tolstogoStAddress, platform);

        testClient.addPreset(identity, tolstogoStreetPreset, platform);
        ObjectKey addressId = new ObjectKey(testClient.getPresets(identity, platform).getAddresses().get(0).getAddressId());

        NewPresetDtoRequest tolstogoStreetAnotherContact = NewPresetDtoRequest.builder()
                .setAddress(tolstogoStAddress)
                .setContact(ContactDtoFactory.anotherSample())
                .build();
        testClient.addPreset(identity, tolstogoStreetAnotherContact, platform);

        List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platform);
        MatcherAssert.assertThat(addresses, hasSize(3));

        NewAddressDtoRequest gruzinskayaStAddress = AddressDtoFactory.gruzinskayaStreet().build();
        testClient.updateAddress(identity, addressId, platform, gruzinskayaStAddress);
        addresses = testClient.getAddresses(identity, platform);

        MatcherAssert.assertThat(addresses, hasSize(3));
        MatcherAssert.assertThat(addresses, hasItem(allOf(hasProperty("regionId", equalTo(213)),
                hasProperty("city", equalTo(gruzinskayaStAddress.getCity())),
                hasProperty("country", equalTo(gruzinskayaStAddress.getCountry())),
                hasProperty("entrance", equalTo(gruzinskayaStAddress.getEntrance())),
                hasProperty("floor", equalTo(gruzinskayaStAddress.getFloor())),
                hasProperty("street", equalTo(gruzinskayaStAddress.getStreet())),
                hasProperty("type", equalTo(AddressType.OTHER))
        )));

        PresetsResponse presets = testClient.getPresets(identity, platform);
        MatcherAssert.assertThat(presets.getPresets(), hasSize(2));
        addresses = presets.getAddresses();
        MatcherAssert.assertThat(addresses, hasSize(1));

        MatcherAssert.assertThat(addresses.get(0), allOf(hasProperty("regionId", equalTo(213)),
                hasProperty("city", equalTo(tolstogoStAddress.getCity())),
                hasProperty("country", equalTo(tolstogoStAddress.getCountry())),
                hasProperty("entrance", equalTo(tolstogoStAddress.getEntrance())),
                hasProperty("floor", equalTo(tolstogoStAddress.getFloor())),
                hasProperty("street", equalTo(tolstogoStAddress.getStreet())),
                hasProperty("type", equalTo(AddressType.OTHER))
        ));
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
    void editedContactShouldNotRessurect(
            @ConvertWith(AddressControllerTest.ToPlatform.class) TestPlatform platform,
            @ConvertWith(AddressControllerTest.ToIdentity.class) Identity<?> identity
    ) throws Exception {
        NewPresetDtoRequest tolstogoStreetPreset = PresetDtoFactory.tolstogoStreet();
        ContactDto contact = tolstogoStreetPreset.getContact();

        ObjectKey contactId = testClient.addContact(identity, contact, platform);

        testClient.addContact(identity, contact, platform);

        testClient.addPreset(identity, tolstogoStreetPreset, platform);

        List<ContactDtoResponse> contacts = testClient.getContacts(identity, platform);
        MatcherAssert.assertThat(contacts, hasSize(3));

        ContactDto anotherContact = ContactDtoFactory.anotherSample();
        testClient.updateContact(identity, contactId, platform, anotherContact);
        contacts = testClient.getContacts(identity, platform);

        MatcherAssert.assertThat(contacts, hasSize(3));
        MatcherAssert.assertThat(contacts, hasItem(allOf(
                hasProperty("recipient", equalTo(anotherContact.getRecipient())),
                hasProperty("email", equalTo(anotherContact.getEmail())),
                hasProperty("phoneNum", equalTo(anotherContact.getPhoneNum()))
        )));

        contacts = testClient.getPresets(identity, platform).getContacts();
        MatcherAssert.assertThat(contacts, hasSize(1));

        MatcherAssert.assertThat(contacts.get(0), allOf(
                hasProperty("recipient", equalTo(contact.getRecipient())),
                hasProperty("email", equalTo(contact.getEmail())),
                hasProperty("phoneNum", equalTo(contact.getPhoneNum()))
        ));
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
    void editedContactShouldNotRessurect2(
            @ConvertWith(AddressControllerTest.ToPlatform.class) TestPlatform platform,
            @ConvertWith(AddressControllerTest.ToIdentity.class) Identity<?> identity
    ) throws Exception {
        NewPresetDtoRequest tolstogoStreetPreset = PresetDtoFactory.tolstogoStreet();
        ContactDto contact = tolstogoStreetPreset.getContact();

        testClient.addContact(identity, contact, platform);

        testClient.addContact(identity, contact, platform);

        testClient.addPreset(identity, tolstogoStreetPreset, platform);

        ObjectKey contactId = new ObjectKey(testClient.getPresets(identity, platform).getContacts().get(0).getContactId());

        NewPresetDtoRequest anotherAddressPreset = NewPresetDtoRequest.builder()
                .setAddress(AddressDtoFactory.gruzinskayaStreet().build())
                .setContact(ContactDtoFactory.sample())
                .build();
        testClient.addPreset(identity, anotherAddressPreset, platform);

        List<ContactDtoResponse> contacts = testClient.getContacts(identity, platform);
        MatcherAssert.assertThat(contacts, hasSize(3));

        ContactDto anotherContact = ContactDtoFactory.anotherSample();
        testClient.updateContact(identity, contactId, platform, anotherContact);
        contacts = testClient.getContacts(identity, platform);

        MatcherAssert.assertThat(contacts, hasSize(3));
        MatcherAssert.assertThat(contacts, hasItem(allOf(
                hasProperty("recipient", equalTo(anotherContact.getRecipient())),
                hasProperty("email", equalTo(anotherContact.getEmail())),
                hasProperty("phoneNum", equalTo(anotherContact.getPhoneNum()))
        )));

        PresetsResponse presets = testClient.getPresets(identity, platform);
        MatcherAssert.assertThat(presets.getPresets(), hasSize(2));
        contacts = presets.getContacts();
        MatcherAssert.assertThat(contacts, hasSize(1));

        MatcherAssert.assertThat(contacts.get(0), allOf(
                hasProperty("recipient", equalTo(contact.getRecipient())),
                hasProperty("email", equalTo(contact.getEmail())),
                hasProperty("phoneNum", equalTo(contact.getPhoneNum()))
        ));
    }

}
