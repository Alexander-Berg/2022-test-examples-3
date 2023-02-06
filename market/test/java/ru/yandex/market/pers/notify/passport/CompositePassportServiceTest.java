package ru.yandex.market.pers.notify.passport;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ru.yandex.market.pers.notify.mock.MockedBlackBoxPassportService;
import ru.yandex.market.pers.notify.passport.model.FullUserInfo;
import ru.yandex.market.pers.notify.passport.model.UserInfo;
import ru.yandex.market.pers.notify.test.MockedDbTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pers.notify.mock.MockFactory.SBER_ID;

class CompositePassportServiceTest extends MockedDbTest {

    @Autowired
    private CompositePassportService passportService;

    @Autowired
    private MockedBlackBoxPassportService blackBoxPassportService;

    @Test
    void getUserInfoForPassportUid() {
        final UserInfo userInfo = passportService.getUserInfo(1L);
        assertNotNull(userInfo);
        assertEquals(1L, userInfo.getId().longValue());
        assertTrue(StringUtils.isNotBlank(userInfo.getLogin()));
    }

    @Test
    void getUserInfoForSberId() {
        final UserInfo userInfo = passportService.getUserInfo(SBER_ID);
        assertNotNull(userInfo);
        assertNotNull(userInfo.getId());
        assertEquals(SBER_ID, userInfo.getId().longValue());
        assertNull(userInfo.getLogin());
    }

    @Test
    void findUid() {
        long uid = passportService.findUid(null);
        assertEquals(0L, uid);

        uid = passportService.findUid("");
        assertEquals(0L, uid);

        blackBoxPassportService.doReturn(11L, "test");
        uid = passportService.findUid("test");
        assertEquals(11L, uid);
    }

    @Test
    void getUserParamsForPassportUid() {
        final Map<String, String> userParams = passportService.getUserParams(1L);
        assertNotNull(userParams);
        assertEquals(0, userParams.size());
    }

    @Test
    void getUserParamsForSberId() {
        final Map<String, String> userParams = passportService.getUserParams(SBER_ID);
        assertNotNull(userParams);
        assertEquals(6, userParams.size());
    }

    @Test
    void getUsersForPassportUid() {
        final Collection<UserInfo> users = passportService.getUsers(Arrays.asList(1L, 2L, null));
        assertNotNull(users);
        assertEquals(2, users.size());
    }

    @Test
    void getUsersForSberId() {
        final Collection<UserInfo> users = passportService.getUsers(Arrays.asList(SBER_ID, null));
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals(SBER_ID, users.iterator().next().getId().longValue());
    }

    @Test
    void getEmailsForPassportUid() {
        blackBoxPassportService.doReturn(1L, Collections.singletonList("test@blackbox.ru"));
        final List<String> emails = passportService.getEmails(1L);
        assertNotNull(emails);
        assertEquals(1, emails.size());
        assertTrue(emails.contains("test@blackbox.ru"));
    }

    @Test
    void getEmailsForSberId() {
        final List<String> emails = passportService.getEmails(SBER_ID);
        assertNotNull(emails);
        assertEquals(2, emails.size());
        assertThat(emails, containsInAnyOrder("first@email.com", "second@email.com"));
    }

    @Test
    void getFullUserInfoForPassportUid() {
        final FullUserInfo userInfo = passportService.getFullUserInfo(1L);
        assertNotNull(userInfo);
        assertEquals(1L, userInfo.getUid().longValue());
    }

    @Test
    void getFullUserInfoForSberId() {
        final FullUserInfo userInfo = passportService.getFullUserInfo(SBER_ID);
        assertNotNull(userInfo);
        assertEquals(SBER_ID, userInfo.getUid().longValue());
        assertEquals("last_name_sber_id first_name_sber_id", userInfo.getName());
    }
}
