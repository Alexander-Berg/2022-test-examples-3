package ru.yandex.market.abo.clch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.market.abo.clch.model.Email;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Olga Bolshakova
 * @date 13.11.2007
 * Time: 11:30:06
 */
class CheckerUtilsTest {
    @Test
    void testCutPhone() {
        assertEquals(CheckerUtils.cutPhone("+7(495)730-00-06"), "7300006");
        assertEquals(CheckerUtils.cutPhone("542-30-23"), "5423023");
        assertEquals(CheckerUtils.cutPhone("(495)6835572"), "6835572");
    }

    @Test
    void testEmail() {
        final Email email = new Email("kinzoru-market@narod.ru");

        assertEquals(email.getDomain(), "ru");
        for (final String prefix : email.getPrefixes()) {
            assertEquals(prefix, "kinzoru-market");
        }
        for (final String postfix : email.getPostfixes()) {
            assertEquals(postfix, "narod");
        }
    }

    @ParameterizedTest
    @CsvSource({"porta@003.ru, www.003.ru, true", "foo@bar.ru, buzz.com, false"})
    void testIsEmailLikeDomain(String email, String domain, boolean same) {
        assertEquals(same, CheckerUtils.isEmailLikeDomain(new Email(email), CheckerUtils.cutUrl(domain)));
    }
}
