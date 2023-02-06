package ru.yandex.market.pers.address.factories;

import ru.yandex.market.pers.address.controllers.model.AddressFullnessState;
import ru.yandex.market.pers.address.model.Address;

import static ru.yandex.market.pers.address.factories.AddressDtoFactory.GRUZINSKAIA_STREET;
import static ru.yandex.market.pers.address.factories.AddressDtoFactory.TOLSTOGO_STREET;

public class AddressFactory {
    public static Address.Builder tolstogoStreet() {
        return Address.builder()
            .setRegionId(213)
            .setMetro("Парк Культуры")
            .setCountry("Россия")
            .setCity("Москва")
            .setStreet(TOLSTOGO_STREET)
            .setBuilding("16")
            .setEntrance("2")
            .setFloor("2")
            .setIntercom("849*5")
            .setCargoLift(false)
            .setFullnessState(AddressFullnessState.FULLY_FILLED);
    }

    public static Address.Builder grouzinskayaStreet() {
        return Address.builder()
            .setRegionId(213)
            .setMetro("Барикадная")
            .setCountry("Россия")
            .setCity("Москва")
            .setStreet(GRUZINSKAIA_STREET)
            .setEntrance("1")
            .setRoom("54")
            .setBuilding("12")
            .setCargoLift(false)
            .setFullnessState(AddressFullnessState.FULLY_FILLED);
    }
}
