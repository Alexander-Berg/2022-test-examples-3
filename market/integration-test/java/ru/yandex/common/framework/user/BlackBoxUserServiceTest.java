package ru.yandex.common.framework.user;

import org.junit.Test;

import ru.yandex.common.framework.user.blackbox.BlackBoxService;
import ru.yandex.common.framework.user.blackbox.BlackBoxUserService;

import static org.junit.Assert.assertEquals;

/**
 * @author Olga Bolshakova (obolshakova@yandex-team.ru)
 */
public class BlackBoxUserServiceTest {

    private static final String BLACK_BOX_URL = "http://pass-test.yandex.ru/blackbox";
    private static final BlackBoxUserService USER_SERVICE = createBlackBoxUserService();


    @Test
    public void testGetUserInfoByUid() {
        final UserInfo userInfoByUid = USER_SERVICE.getUserInfo(14204L);
        assertEquals("vasya-pupkin", userInfoByUid.getLogin());
    }

    @Test
    public void testGetUserInfoByLogin() {
        final UserInfo userInfoByLogin = USER_SERVICE.getUserInfo("vasya.pupkin");
        assertEquals(14204, userInfoByLogin.getUserId());
    }


    private static BlackBoxUserService createBlackBoxUserService() {
        final BlackBoxService blackBoxService = new BlackBoxService();
        blackBoxService.setBlackBoxUrl(BLACK_BOX_URL);

        final BlackBoxUserService userService = new BlackBoxUserService();
        userService.setBlackBoxService(blackBoxService);
        return userService;
    }

}
