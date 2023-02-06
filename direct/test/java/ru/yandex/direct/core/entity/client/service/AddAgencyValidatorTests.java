package ru.yandex.direct.core.entity.client.service;

import ru.yandex.direct.core.entity.client.model.AddAgencyClientRequest;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;

/**
 * Вспомагательный класс для тестов на AgencyClients.add
 */
public class AddAgencyValidatorTests {
    static final UidAndClientId AGENCY = UidAndClientId.of(1L, ClientId.fromLong(2L));

    static final String LOGIN = "login";
    static final String FIRST_NAME = "firstName";
    static final String LAST_NAME = "lastName";
    static final String NOTIFICATION_EMAIL = "notify@email.com";
    static final CurrencyCode CURRENCY = CurrencyCode.RUB;

    public static AddAgencyClientRequest defaultRequest() {
        return new AddAgencyClientRequest()
                .withLogin(LOGIN)
                .withFirstName(FIRST_NAME)
                .withLastName(LAST_NAME)
                .withNotificationEmail(NOTIFICATION_EMAIL)
                .withCurrency(CURRENCY);
    }
}
