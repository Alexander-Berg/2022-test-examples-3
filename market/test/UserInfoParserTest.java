package ru.yandex.market.jmf.blackbox.support.test;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.util.ResourceHelpers;
import ru.yandex.market.jmf.blackbox.support.response.UserInfo;
import ru.yandex.market.jmf.blackbox.support.userinfo.UserInfoParser;
import ru.yandex.market.jmf.blackbox.support.userinfo.UserType;

/**
 * Created by vdorogin on 22.09.17.
 */
public class UserInfoParserTest {

    private static final String BLACKBOX_RESPONSE_FILE = "/blackbox/support/blackbox-response.json";

    @Test
    public void test() {
        List<UserInfo> result = new UserInfoParser()
                .parse(ResourceHelpers.getResource(BLACKBOX_RESPONSE_FILE));
        Assertions.assertEquals(6, result.size());

        UserInfo expected = new UserInfo();
        expected.setUid(3000448393L);
        expected.setLogin("marketTestingUser");
        expected.setDisplayName(null);
        expected.setLastName(null);
        expected.setFirstName(null);
        expected.setEmail(null);
        expected.setPhone("");
        expected.setRegDate(null);
        assertUserInfo(expected, result.get(0));

        expected.setUid(4000042507L);
        expected.setLogin("autopayment1");
        expected.setDisplayName("Фамилия Имя");
        expected.setPublicName("Фамилия И.");
        expected.setLastName("Фамилия");
        expected.setFirstName("Имя");
        expected.setEmail("autopayment1@yandex.ru");
        expected.setPhone("+79217446176");
        expected.setRegDate("2014-07-01 17:54:49");
        expected.setAvatarSlug("0/0-0");
        assertUserInfo(expected, result.get(1));

        expected.setDisplayName(null);
        expected.setLastName(null);
        expected.setFirstName(null);
        expected.setEmail(null);
        expected.setPhone(null);
        expected.setRegDate("2018-09-26 16:00:00");
        assertUserInfo(expected, result.get(2));

        expected.setRegDate(null);
        assertUserInfo(expected, result.get(3)); // "1" : "0"
        assertUserInfo(expected, result.get(4)); // "1" : null
        assertUserInfo(expected, result.get(5)); // "1" : ""
    }

    private void assertUserInfo(UserInfo expected, UserInfo actual) {
        Assertions.assertEquals(expected.getUid(), actual.getUid());
        Assertions.assertEquals(expected.getLogin(), actual.getLogin());
        Assertions.assertEquals(expected.getDisplayName(), actual.getDisplayName());
        Assertions.assertEquals(expected.getEmail(), actual.getEmail());
        Assertions.assertEquals(expected.getPhone(), actual.getPhone());
        Assertions.assertEquals(expected.getRegDate(), actual.getRegDate());
        Assertions.assertEquals(expected.getFirstName(), actual.getFirstName());
        Assertions.assertEquals(expected.getLastName(), actual.getLastName());
        Assertions.assertEquals(UserType.YANDEX, actual.getUserType());
    }
}
