package ru.yandex.market.checkout.checkouter.validation;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureResolverStub;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;

public class EmailValidatorTest {

    private EmailValidator emailValidator;
    private CheckouterFeatureResolverStub checkouterFeatureResolverStub;

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.stream(new Object[][]{
                // New validation tests
                {true, "timursha@yandex-team.ru", null, true},
                {true, "", IllegalArgumentException.class, true},
                {true, "abc", IllegalArgumentException.class, true},
                {true, "a@b", IllegalArgumentException.class, true},
                {true, ".a@yandex.ru", IllegalArgumentException.class, true},
                {true, "a.@yandex.ru", IllegalArgumentException.class, true},
                {true, "a.b@yandex.ru", null, true},
                {true, "sala@.gmail.com", IllegalArgumentException.class, true},
                {true, "sala@gmail...com", IllegalArgumentException.class, true},
                {true, "login@почта.рф", null, true},
                {true, "почта@yandex.ru", IllegalArgumentException.class, true},
                {true, "sergey-bp@inbox.ru", null, true},
                {true, "sergey-bp@inbox.ru", null, false},
                {true, "sergey-bp@inbox.ri", null, true},  //must be valid by simple validation only
                {true, "sergey-bp@inbox.ri", IllegalArgumentException.class, false},
                {true, "sergey+bp@inbox.ru", null, true},
                {true, "sergey,bp@inbox.ru", IllegalArgumentException.class, true},
                {true, "сергейsmith@inbox.ru", IllegalArgumentException.class, true},
                {true, "сергей@inbox.рф", IllegalArgumentException.class, true},
                {true, "сергей@inбокс.рф", IllegalArgumentException.class, true},
                {true, "сергей@inбокс.ru", IllegalArgumentException.class, true},

                // Old validation tests
                {false, "timursha@yandex-team.ru", null, true},
                {false, "timursha@yandex-team.ru", null, false},
                {false, "timursha@yandex-team.ri", null, true},  //must be valid by simple validation only
                {false, "timursha@yandex-team.ri", IllegalArgumentException.class, false},
                {false, "", IllegalArgumentException.class, true},
                {false, "abc", IllegalArgumentException.class, true},
                {false, "a@b", IllegalArgumentException.class, true},
                {false, ".a@yandex.ru", IllegalArgumentException.class, true},
                {false, "a.@yandex.ru", IllegalArgumentException.class, true},
                {false, "a.b@yandex.ru", null, true},
                {false, "sala@.gmail.com", IllegalArgumentException.class, true},
                {false, "sala@gmail...com", IllegalArgumentException.class, true},
                {false, "login@почта.рф", null, true},
                {false, "почта@yandex.ru", null, true},
                {false, "sergey-bp@inbox.ru", null, true},
                {false, "sergey+bp@inbox.ru", null, true},
                {false, "sergey,bp@inbox.ru", IllegalArgumentException.class, true},
                {false, "сергейsmith@inbox.ru", null, true},
                {false, "сергей@inbox.рф", null, true},
                {false, "сергей@inбокс.рф", null, true},
                {false, "сергей@inбокс.ru", null, true},
        }).map(Arguments::of);
    }

    @BeforeEach
    public void setup() {
        checkouterFeatureResolverStub = new CheckouterFeatureResolverStub();
        emailValidator = new EmailValidator(checkouterFeatureResolverStub);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void validateEmail(boolean isMixedCyrillicAndLatinSymbolsInEmailCheckEnabled, String email, Class<?
            extends Throwable> expectedException, boolean isSimpleValidation) {
        checkouterFeatureResolverStub.writeValue(
                BooleanFeatureType.MIXED_CYRILLIC_AND_LATIN_SYMBOLS_IN_EMAIL_CHECK_ENABLED,
                isMixedCyrillicAndLatinSymbolsInEmailCheckEnabled);

        if (expectedException != null) {
            Throwable throwable = Assertions.assertThrows(
                    expectedException,
                    () -> emailValidator.validate(email, isSimpleValidation)
            );
            Assertions.assertEquals("Email address has invalid format: " + email, throwable.getMessage());
        } else {
            emailValidator.validate(email, isSimpleValidation);
        }
    }
}
