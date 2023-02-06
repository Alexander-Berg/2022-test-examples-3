package ru.yandex.market.pers.address.factories;

import ru.yandex.market.pers.address.model.Preset;

public class PresetFactory {
    public static Preset tolstogoStreet() {
        return Preset.builder()
            .setAddress(AddressFactory.tolstogoStreet().build())
            .setContact(ContactFactory.sample())
            .build();
    }

    public static Preset grouzinskayaStreet() {
        return Preset.builder()
            .setAddress(AddressFactory.grouzinskayaStreet().build())
            .setContact(ContactFactory.sample())
            .build();
    }

}
