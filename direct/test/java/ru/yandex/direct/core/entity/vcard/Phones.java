package ru.yandex.direct.core.entity.vcard;

import ru.yandex.direct.core.entity.vcard.model.Phone;

public final class Phones {
    public static Phone phone(String countryCode, String cityCode, String phone, String ext) {
        return new Phone()
                .withCountryCode(countryCode)
                .withCityCode(cityCode)
                .withPhoneNumber(phone)
                .withExtension(ext);
    }
}
