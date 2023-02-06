package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.security.checker.Has2FAChecker;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Has2FACheckerTest extends FunctionalTest {
    @Autowired
    Has2FAChecker has2FAChecker;

    @Autowired
    PassportService passportService;

    static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(123, true, true),
                Arguments.of(126, false, false)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("args")
    void test(long uid, boolean has2FA, boolean expected) {
        MockPartnerRequest request = new MockPartnerRequest(uid, 1L, PartnerId.supplierId(1L));
        Mockito.when(passportService.has2FAOn(uid)).thenReturn(has2FA);
        Authority authority = new Authority();
        assertEquals(expected, has2FAChecker.checkTyped(request, authority));
    }
}
