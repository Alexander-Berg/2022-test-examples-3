package ru.yandex.market.personal.converter;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.lang.NonNull;

import ru.yandex.market.logistics.personal.model.Address;
import ru.yandex.market.personal.client.model.FullName;
import ru.yandex.market.personal.client.model.GpsCoord;

public class ConverterUtils {
    public static final String EMAIL = "abc@gmail.com";
    public static final String PHONE = "89123456789";

    private ConverterUtils() {
    }

    @NonNull
    public static FullName createFullName() {
        return new FullName().forename("Иван")
            .surname("Иванов")
            .patronymic("Иванович");
    }

    @NonNull
    public static Address createAddress() {
        return Address.builder()
            .locality("Населенный_пункт")
            .subRegion("Городской_округ")
            .settlement("Поселение")
            .district("Микрорайон")
            .street("Улица")
            .house("Дом")
            .building("Строение")
            .housing("Корпус")
            .room("Квартира_или_офис")
            .zipCode("Почтовый_индекс")
            .porch("Подъезд")
            .floor(15)
            .metro("Станция_метро")
            .geoId(213)
            .intercom("Код_домофона")
            .comment("Комментарий")
            .build();
    }

    @NonNull
    public static Map<String, String> createAddressMap() {
        return Map.ofEntries(
            Map.entry("locality", "Населенный_пункт"),
            Map.entry("subRegion", "Городской_округ"),
            Map.entry("settlement", "Поселение"),
            Map.entry("district", "Микрорайон"),
            Map.entry("street", "Улица"),
            Map.entry("house", "Дом"),
            Map.entry("building", "Строение"),
            Map.entry("housing", "Корпус"),
            Map.entry("room", "Квартира_или_офис"),
            Map.entry("zipCode", "Почтовый_индекс"),
            Map.entry("porch", "Подъезд"),
            Map.entry("floor", "15"),
            Map.entry("metro", "Станция_метро"),
            Map.entry("geoId", "213"),
            Map.entry("intercom", "Код_домофона"),
            Map.entry("comment", "Комментарий")
        );
    }

    @NonNull
    public static GpsCoord createGps() {
        return new GpsCoord().latitude(BigDecimal.ONE)
            .longitude(BigDecimal.ZERO);
    }
}
