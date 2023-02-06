package ru.yandex.common.util;

import junit.framework.TestCase;
import org.junit.Assert;

/**
 * User: Olga Kuzmina (strangelet)
 * Date: 27.04.2010  17:51:43
 * Юнит тест проверяющий EmailUtils#validate(String), который валидирует (по синтаксису) емейлы
 *
 * @see EmailUtils#validate(String)
 */
public class EmailValidatorTest extends TestCase {
    private String[] invalid_emails = new String[]{"partner", " ", "",
            "partner@.topcode.my", "partner123@yandex.a",
            "partner123@.com", "partner123@.com.com", "-partner@yandex.ru",
            ".partner@yandex.ru", "_partner@yandex.ru", "partner()*@ya.ru",
            "partner@%*.com", "partner@partner@ya.ru", "partner@ya.com.1a", "Яpartner@ya.com", "partner@Я.com",
            "partner@yandex..team.ru",
            "a@b.c", "_a@b.cd", "-a@b.cd", "a@b.c-d"
    };


    private String[] valid_emails = new String[]{"partner@yandex.com",
            "partner-300@yandex.com", "partner.300@yandex.com",
            "partner777@yandex.ru", "partner_300@yandex.net",
            "partner.100@yandex.com.ru", "partner@1.ru", "partner..2010@ya.ru",
            "partner--2010@ya.ru", "partner__2010@ya.ru", "partner.-_2010@ya.ru",
            "partner.market@ya.c-5.com", "2010.partner@yandex.ua",
            "partner@yandex-team.ru", "partner@yandex.team.ru", "partner@yandex_team.ru",
            "partner@yandex--team.ru", "partner@yandex__team.ru",
            "a@b.cd", "ivan@pertov", "iavn_pertov_@xxx.travel", "iavn_pertov_@xxx.travels", "iavn_pertov_@my-abc.travels", "t--@yandex-team.travels",
            "t++@yandex-team.travels"
    };


    public void testInvalidEmails() {
        System.out.println("Check invalid emails");
        for (String email : invalid_emails) {
            boolean valid = EmailUtils.validate(email);
            System.out.println("Is valid Email : " + email + " , " + valid);
            Assert.assertEquals(false, valid);

        }
    }

    public void testValidEmails() {
        System.out.println("Check valid emails");
        for (String email : valid_emails) {
            boolean valid = EmailUtils.validate(email);
            System.out.println("Is valid Email : " + email + " , " + valid);
            Assert.assertEquals(true, valid);

        }
    }
}
