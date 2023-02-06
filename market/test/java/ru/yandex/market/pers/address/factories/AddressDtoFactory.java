package ru.yandex.market.pers.address.factories;

import java.math.BigDecimal;

import ru.yandex.market.pers.address.controllers.model.AddressFullnessState;
import ru.yandex.market.pers.address.controllers.model.LocationDto;
import ru.yandex.market.pers.address.controllers.model.NewAddressDtoRequest;

public class AddressDtoFactory {
    public static final String TOLSTOGO_STREET = "ул. Льва Толстого";
    public static final String PROFSOYUZNAYA_STREET = "Профсоюзная улица";
    public static final String ZARECHNAYA_STREET = "Заречная улица";
    public static final String GRUZINSKAIA_STREET = "ул. б. Грузинская";

    public static NewAddressDtoRequest.Builder tolstogoStreet() {
        return NewAddressDtoRequest.builder()
                .setRegionId(213)
                .setCountry("Россия")
                .setCity("Москва")
                .setStreet(TOLSTOGO_STREET)
                .setBuilding("16")
                .setFloor("3")
                .setRoom("109")
                .setEntrance("2")
                .setIntercom("235")
                .setZip("119021")
                .setComment("Адрес офиса.")
                .setLocation(new LocationDto(new BigDecimal("55.733969"), new BigDecimal("37.587093")))
                .setFullnessState(AddressFullnessState.FULLY_FILLED);
    }

    public static NewAddressDtoRequest.Builder slightlyChangedTolstogoStreet() {
        return NewAddressDtoRequest.builder()
                .setRegionId(213)
                .setCountry("Россия")
                .setCity("Москва")
                .setStreet(TOLSTOGO_STREET)
                .setBuilding("16 ")
                .setFloor("3")
                .setRoom("109")
                .setEntrance("2")
                .setIntercom("236")
                .setZip("119021")
                .setLocation(new LocationDto(new BigDecimal("55.733969"), new BigDecimal("37.587093")))
                .setFullnessState(AddressFullnessState.FULLY_FILLED);
    }

    public static NewAddressDtoRequest.Builder tolstogoStreetAnotherRoom() {
        return NewAddressDtoRequest.builder()
                .setRegionId(213)
                .setCountry("Россия")
                .setCity("Москва")
                .setStreet(TOLSTOGO_STREET)
                .setBuilding("16")
                .setFloor("3")
                .setRoom("100")
                .setEntrance("2")
                .setIntercom("235")
                .setZip("119021")
                .setLocation(new LocationDto(new BigDecimal("55.733969"), new BigDecimal("37.587093")))
                .setFullnessState(AddressFullnessState.FULLY_FILLED);
    }

    public static NewAddressDtoRequest.Builder gruzinskayaStreet() {
        return NewAddressDtoRequest.builder()
                .setRegionId(213)
                .setCountry("Россия")
                .setCity("Москва")
                .setStreet(GRUZINSKAIA_STREET)
                .setBuilding("12")
                .setFloor("10")
                .setRoom("894")
                .setEntrance("5")
                .setIntercom("1123")
                .setZip("123242")
                .setLocation(new LocationDto(new BigDecimal("55.763356"), new BigDecimal("37.579376")))
                .setFullnessState(AddressFullnessState.FULLY_FILLED);
    }
}
