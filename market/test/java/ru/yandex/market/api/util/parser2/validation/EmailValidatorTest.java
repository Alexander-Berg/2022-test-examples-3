package ru.yandex.market.api.util.parser2.validation;

import java.util.Collection;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.common.util.collections.Maybe;
import ru.yandex.market.api.util.parser2.validation.errors.EmailValidationError;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(Enclosed.class)
public class EmailValidatorTest {

    private static final EmailValidator EMAIL_VALIDATOR = new EmailValidator();

    @RunWith(Parameterized.class)
    public static class ShouldValidateGlobalEmails {

        @Parameterized.Parameters(name = "{index}: {0}")
        public static Collection<String> parameters() {
            return Lists.newArrayList(
                "test@yandex.ru",
                "пупкин@почта.рф",
                "guru@guru.guru",
                "john@smith.attorney",
                "prettyandsimple@example.com",
                "very.common@example.com",
                "disposable.style.email.with+symbol@example.com",
                "other.email-with-dash@example.com",
                "fully-qualified-domain@example.com",
                "user.name+tag+sorting@example.com", // (will go to user.name@example.com inbox)
                "x@example.com", // (one-letter local-part)
                "\"very.(),:;<>[]\\\".VERY.\\\"very@\\\\ \\\"very\\\".unusual\"@strange.example.com",
                "example-indeed@strange-example.com",
                "example@s.solutions", // (see the List of Internet top-level domains)
                "#!$%&'*+-/=?^_`{}|~@example.org",
                "\"()<>[]:,;@\\\\\\\"!#$%&'-/=?^_`{}| ~.a\"@example.org",
                "user@[2001:DB8::1]",
                "email@subdomain.domain.com", //Email contains dot with subdomain
                "firstname+lastname@domain.com", //	Plus sign is considered valid character
                "email@[123.123.123.123]", // Square bracket around IP address is considered valid
                "1234567890@domain.com", //	Digits in address are valid
                "email@domain-one.com", // Dash in domain name is valid
                "_______@domain.com", // Underscore in the address field is valid
                "email@domain.name", //	.name is valid Top Level Domain name
                "email@domain.co.jp", // Dot in Top Level Domain name also considered valid (use co.jp as example here)
                "firstname-lastname@domain.com" //	Dash in address field is valid
            );
        }

        @Parameterized.Parameter
        public String email;

        @Test
        public void shouldValidateGlobalEmails() throws Exception {
            assertThat(EMAIL_VALIDATOR.validate(Maybe.just(email)), nullValue());
        }
    }

    @RunWith(Parameterized.class)
    public static class ShouldNotValidateLocalEmails {

        @Parameterized.Parameters(name = "{index}: {0}")
        public static Collection<String> parameters() {
            return Lists.newArrayList(
                "admin@mailserver1", // (local domain name with no TLD, although ICANN highly discourages dotless email addresses)
                "user@localserver"
            );
        }

        @Parameterized.Parameter
        public String email;

        @Test
        public void shouldNotValidateLocalEmails() throws Exception {
            EmailValidationError error = (EmailValidationError) EMAIL_VALIDATOR.validate(Maybe.just(email));
            assertThat(error, notNullValue());
            assertThat(error.getEmail(), is(email));
        }
    }

    @RunWith(Parameterized.class)
    public static class ShouldReturnErrorForIncorrectValues {

        @Parameterized.Parameters(name = "{index}: {0}")
        public static Collection<String> parameters() {
            return Lists.newArrayList(
                "bad-email",
                "Abc.example.com", // (no @ character)
                "A@b@c@example.com", // (only one @ is allowed outside quotation marks)
                "a\"b(c)d,e:f;g<h>i[j\\k]l@example.com", // (none of the special characters in this local-part are allowed outside quotation marks)
                "just\"not\"right@example.com", // (quoted strings must be dot separated or the only element making up the local-part)
                "this is\"not\\allowed@example.com", // (spaces, quotes, and backslashes may only exist when within quoted strings and preceded by a backslash)
                "1234567890123456789012345678901234567890123456789012345678901234+x@example.com", // (too long)
                "john..doe@example.com", // (double dot before @)
                "example@localhost", // (sent from localhost)
                "john.doe@example..com", // (double dot after @)
                "#@%^%#$@#$@#.com", // Garbage
                "@domain.com", // Missing username
                "Joe Smith <email@domain.com>", // Encoded html within email is invalid
                ".email@domain.com", //	Leading dot in address is not allowed
                "email.@domain.com", //	Trailing dot in address is not allowed
                "email@domain.com (Joe Smith)", // Text followed email is not allowed
                "email@-domain.com", //	Leading dash in front of domain is invalid
                "email@111.222.333.44444", // Invalid IP format
                "email@domain..com" //	Multiple dot in the domain portion is invalid
            );
        }

        @Parameterized.Parameter
        public String email;

        @Test
        public void shouldReturnErrorForIncorrectValue() throws Exception {
            EmailValidationError error = (EmailValidationError) EMAIL_VALIDATOR.validate(Maybe.just(email));
            assertThat(error, notNullValue());
            assertThat(error.getEmail(), is(email));
        }
    }
}
