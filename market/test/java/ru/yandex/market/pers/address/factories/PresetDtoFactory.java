package ru.yandex.market.pers.address.factories;

import ru.yandex.market.pers.address.controllers.model.NewPresetDtoRequest;

public class PresetDtoFactory {
    public static NewPresetDtoRequest tolstogoStreet() {
        return NewPresetDtoRequest.builder()
                .setAddress(AddressDtoFactory.tolstogoStreet().build())
                .setContact(ContactDtoFactory.sample())
                .build();
    }

    public static NewPresetDtoRequest tolstogoStreetAnotherRoom() {
        return NewPresetDtoRequest.builder()
                .setAddress(AddressDtoFactory.tolstogoStreetAnotherRoom().build())
                .setContact(ContactDtoFactory.sample())
                .build();
    }

    public static NewPresetDtoRequest slightlyChangedTolstogoStreet() {
            return NewPresetDtoRequest.builder()
                .setAddress(AddressDtoFactory.slightlyChangedTolstogoStreet().build())
                .setContact(ContactDtoFactory.sample())
                .build();
    }

    public static NewPresetDtoRequest gruzinskayaStreet() {
        return NewPresetDtoRequest.builder()
                .setAddress(AddressDtoFactory.gruzinskayaStreet().build())
                .setContact(ContactDtoFactory.anotherSample())
                .build();
    }
}
