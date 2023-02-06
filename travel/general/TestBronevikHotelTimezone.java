package ru.yandex.travel.hotels.common.partners.bronevik.utils.timezone;

import ru.yandex.travel.hotels.common.partners.bronevik.proto.THotelTimezone;

public class TestBronevikHotelTimezone implements BronevikHotelTimezone{

    @Override
    public String getTimezoneID(String hotelId) {
        return "Europe/Moscow";
    }

    @Override
    public THotelTimezone getHotelTimezone(String hotelId) {
        return THotelTimezone.newBuilder()
                .setOriginalId("244")
                .setTimezoneId("Europe/Moscow")
                .build();
    }
}
