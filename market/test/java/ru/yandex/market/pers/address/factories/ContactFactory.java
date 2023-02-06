package ru.yandex.market.pers.address.factories;

import ru.yandex.market.pers.address.model.Contact;

public class ContactFactory {
    public static final String DEFAULT_FIRST_NAME = "Иван";
    public static final String DEFAULT_LAST_NAME = "Иванов";
    public static final String DEFAULT_EMAIL = "user@yandex-team.ru";
    public static final String DEFAULT_PHONE_NUM = "+796514578945";
    public static final String DEFAULT_SECOND_NAME = "Иванович";

    public static Contact sample() {
        return sampleBuilder()
            .build();
    }

    public static Contact.Builder sampleBuilder() {
        return Contact.builder()
            .setEmail(DEFAULT_EMAIL)
            .setFirstName(DEFAULT_FIRST_NAME)
            .setSecondName(DEFAULT_SECOND_NAME)
            .setLastName(DEFAULT_LAST_NAME)
            .setPhoneNum(DEFAULT_PHONE_NUM);
    }
}
