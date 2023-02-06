package ru.yandex.market.pers.address.factories;

import ru.yandex.market.pers.address.controllers.model.AddressDtoResponse;
import ru.yandex.market.pers.address.controllers.model.ContactDto;
import ru.yandex.market.pers.address.controllers.model.ContactDtoResponse;
import ru.yandex.market.pers.address.controllers.model.NewAddressDtoRequest;
import ru.yandex.market.pers.address.controllers.model.NewPresetDtoRequest;
import ru.yandex.market.pers.address.controllers.model.PresetsResponse;
import ru.yandex.market.pers.address.dao.ObjectKey;
import ru.yandex.market.pers.address.model.Preset;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class TestPlatform {

    public abstract String path();

    public static NewPresetDtoRequest newPresetDtoRequest(ContactDto contact, NewAddressDtoRequest address) {
        return NewPresetDtoRequest.builder()
                .setAddress(address)
                .setContact(contact)
                .build();
    }

    public static List<Preset> toPresetModels(PresetsResponse presetsResponse) {
        Map<String, AddressDtoResponse> addressIndex = presetsResponse
                .getAddresses()
                .stream()
                .collect(Collectors.toMap(AddressDtoResponse::getId, Function.identity()));

        Map<String, ContactDtoResponse> contactIndex = presetsResponse
                .getContacts()
                .stream()
                .collect(Collectors.toMap(ContactDtoResponse::getId, Function.identity()));

        return presetsResponse.getPresets().stream()
            .map(p -> {
                AddressDtoResponse addressDtoResponse = addressIndex
                    .get(p.getAddressId());
                ContactDtoResponse contactDtoResponse = contactIndex
                    .get(p.getContactId());
                return new Preset(new ObjectKey(p.getId()), "",
                    addressDtoResponse
                        .toAddressBuilder()
                        .setId(new ObjectKey(addressDtoResponse.getAddressId()))
                        .build(),
                    contactDtoResponse
                        .toContactBuilder()
                        .setId(new ObjectKey(addressDtoResponse.getAddressId()))
                        .build());
            })
            .collect(Collectors.toList());
    }

    public static final TestPlatform BLUE = new TestPlatform() {
        @Override
        public String path() {
            return "blue";
        }
    };

    public static final TestPlatform RED = new TestPlatform() {
        @Override
        public String path() {
            return "red";
        }
    };
}
