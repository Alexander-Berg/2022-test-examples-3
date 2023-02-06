package ru.yandex.direct.core.testing.data;

import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.direct.core.entity.user.model.BlackboxUser;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.i18n.Language;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.rbac.RbacRole;

public final class TestUsers {

    public static final String DEFAULT_USER_EMAIL = "at-direct-super@yandex.ru";
    public static final String DEFAULT_USER_LOGIN = "user_login";

    private TestUsers() {
    }

    /**
     * Получение пользователя с фиксированными данными.
     * Можно использовать в случаях, когда в тесте достаточно одного пользователя
     * или когда нужно использовать данные этого пользователя в тесте
     *
     * @return модель пользователя с предопределенными данными.
     */
    public static User defaultUser() {
        return new User()
                .withLogin(DEFAULT_USER_LOGIN)
                .withEmail(DEFAULT_USER_EMAIL)
                .withPhone("22222")
                .withFio("Ivanov I.I.")
                .withSendNews(false)
                .withSendAccNews(true)
                .withSendWarn(true)
                .withRole(RbacRole.CLIENT)
                .withRepType(RbacRepType.CHIEF)
                .withLang(Language.RU)
                .withCanManagePricePackages(false)
                .withCanApprovePricePackages(false)
                .withStatusBlocked(false)
                .withIsReadonlyRep(false)
                .withMetrikaCountersNum(0);
    }

    /**
     * Создать пользователя со случайным логином, email'ом
     * Метод создан для того, чтобы избежать коллизии login - shard в тестах, степах,
     * где могут использоваться более одного пользователя.
     * Имхо, стоит использовать везде, где не требуются фиксированные данные пользователя.
     *
     * @return модель пользователя со случайным логином и email'ом
     */
    public static User generateNewUser() {
        return new User()
                .withEmail(generateRandomLogin() + "@yandex.ru")
                .withPhone("8888")
                .withFio("Эдуард Сгенеренный")
                .withSendNews(false)
                .withSendAccNews(true)
                .withSendWarn(true)
                .withRole(RbacRole.CLIENT)
                .withRepType(RbacRepType.CHIEF)
                .withLang(Language.RU)
                .withGeoId(0L)
                .withChiefUid(null)
                .withCanManagePricePackages(false)
                .withCanApprovePricePackages(false)
                .withStatusBlocked(false)
                .withSuperManager(false)
                .withIsReadonlyRep(false)
                .withMetrikaCountersNum(0);
    }

    public static String generateRandomLogin() {
        return "random_login_" + RandomStringUtils.randomNumeric(7);
    }

    public static BlackboxUser generateNewBlackboxUser() {
        String userLogin = generateRandomLogin();
        String userEmail = userLogin + "@yandex.ru";

        return new BlackboxUser(0L, userLogin, "0/0", userEmail, "Эдуард Сгенеренный", Language.RU);
    }
}
