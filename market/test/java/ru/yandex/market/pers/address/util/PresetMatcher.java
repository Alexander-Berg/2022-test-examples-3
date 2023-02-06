package ru.yandex.market.pers.address.util;

import javax.validation.constraints.NotNull;

import org.hamcrest.Matcher;

import ru.yandex.market.pers.address.controllers.model.ContactDto;
import ru.yandex.market.pers.address.controllers.model.NewAddressDtoRequest;
import ru.yandex.market.pers.address.controllers.model.NewPresetDtoRequest;
import ru.yandex.market.pers.address.dao.ObjectKey;
import ru.yandex.market.pers.address.model.Address;
import ru.yandex.market.pers.address.model.Contact;
import ru.yandex.market.pers.address.model.Preset;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static ru.yandex.market.pers.address.util.SamePropertyValuesAsExcept.samePropertyValuesAsExcept;

public class PresetMatcher {
    public static Matcher<Preset> samePresetDto(NewPresetDtoRequest expected, ObjectKey expectedId) {
        return allOf(
                samePresetDto(expected),
                hasProperty("id", equalTo(expectedId))
        );
    }

    public static Matcher<Preset> samePresetDto(NewPresetDtoRequest expected) {
        NewAddressDtoRequest expectedAddress = expected.getAddress();
        ContactDto expectedContact = expected.getContact();
        return allOf(
                hasProperty("address", sameAddressDto(expectedAddress)),
                hasProperty("contact", sameContactDto(expectedContact))
        );
    }

    @NotNull
    public static Matcher<? super Contact> sameContactDto(@NotNull ContactDto expectedContact) {
        return samePropertyValuesAsExcept(expectedContact.toContactBuilder().build(), "id", "objectKey");
    }

    @NotNull
    public static Matcher<? super Contact> sameContactDto(@NotNull ContactDto expectedContact, ObjectKey id) {
        return samePropertyValuesAs(expectedContact.toContactBuilder().setId(id).build());
    }

    @NotNull
    public static Matcher<? super Address> sameAddressDto(@NotNull NewAddressDtoRequest expectedAddress) {
        return samePropertyValuesAsExcept(Address.class, expectedAddress.toAddressBuilder().build(),
                "id", "objectKey", "zip", "addressLine", "geoCoderPoint", "region", "type",
                "comment", "lastTouchedTime");
    }

    @NotNull
    public static Matcher<? super Address> sameAddressDto(@NotNull NewAddressDtoRequest expectedAddress, ObjectKey id) {
        return samePropertyValuesAsExcept(Address.class, expectedAddress.toAddressBuilder().setId(id).build(),
                "zip", "addressLine", "geoCoderPoint", "region", "type", "comment", "lastTouchedTime");
    }
}
