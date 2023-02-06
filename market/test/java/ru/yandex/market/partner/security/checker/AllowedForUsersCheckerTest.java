package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.UserInfo;
import ru.yandex.market.core.security.checker.AllowedForUsersChecker;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

import static org.junit.jupiter.api.Assertions.assertEquals;


class AllowedForUsersCheckerTest extends FunctionalTest {

    @Autowired
    AllowedForUsersChecker allowedForUsersChecker;

    @Autowired
    PassportService passportService;

    static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(123, new UserInfo(123, "Смешарик", "a@b.ru", "smesharik"), true),
                Arguments.of(124, new UserInfo(124, "Федя Трамп", "a@b.ru", "fedya-trump"), true),
                Arguments.of(125, new UserInfo(125, "Фу Бар", "a@b.ru", "Foo.Bar"), true),
                Arguments.of(126, new UserInfo(126, "Баз", "a@b.ru", "baz"), false),
                Arguments.of(127, new UserInfo(127, "Бар Фу", "a@b.ru", "foobar"), false)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("args")
    void test(long uid, UserInfo userInfo, boolean expected) {
        MockPartnerRequest request = new MockPartnerRequest(uid, 1L, PartnerId.supplierId(1L));
        Mockito.when(passportService.getUserInfo(uid)).thenReturn(userInfo);
        Authority authority = new Authority("auth", "fedya.trump, smesharik   ,  FOO-BAR");
        assertEquals(expected, allowedForUsersChecker.checkTyped(request, authority));
    }
}
