package ru.yandex.market.loyalty.core.utils;

import ru.yandex.market.loyalty.core.model.coin.UserInfo;

public class UserDataFactory {
    public static final String DEFAULT_UUID = "123123123";
    public static final String ANOTHER_UUID = "12312321321321";
    public static final String DEFAULT_USER_FULL_NAME_ID = "id_777";
    public static final long DEFAULT_MUID = 1152921504656412582L;
    public static final long ANOTHER_MUID = 4142921504656412123L;
    public static final String DEFAULT_YANDEX_UID = "123213213";
    public static final String ANOTHER_YANDEX_UID = "123213214";
    public static final long DEFAULT_UID = 100L;
    public static final int DEFAULT_COINS_LIMIT = 500;
    public static final long DEFAULT_SBER_ID_UID = (1L << 61) - 1L;
    public static final String DEFAULT_EMAIL = "my_email";
    public static final String DEFAULT_EMAIL_ID = "my_email_id_777";
    public static final String DEFAULT_PHONE = "+79654865487";
    public static final String DEFAULT_PHONE_ID = "+79654865487_id_777";
    public static final String ANOTHER_EMAIL = "other_email";
    public static final String ANOTHER_PHONE = "89653333333";
    public static final long ANOTHER_UID = 110L;
    public static final long THIRD_UID = 4564L;
    public static final String DEFAULT_USER_NAME = "Иванов Иван";
    public static final Boolean DEFAULT_IS_B2B_USER = false;

    private UserDataFactory() {
    }

    public static UserInfo.Builder defaultNoAuthInfo() {
        return UserInfo.builder()
                .setYandexUid(DEFAULT_YANDEX_UID)
                .setUuid(DEFAULT_UUID)
                .setPersonalPhoneId(DEFAULT_PHONE_ID)
                .setMuid(DEFAULT_MUID)
                .setEmail(DEFAULT_EMAIL)
                .setPersonalEmailId(DEFAULT_EMAIL_ID)
                .setPersonalFullNameId(DEFAULT_USER_FULL_NAME_ID)
                .setPersonalPhoneId(DEFAULT_PHONE_ID)
                .setIsB2B(DEFAULT_IS_B2B_USER);
    }
}
