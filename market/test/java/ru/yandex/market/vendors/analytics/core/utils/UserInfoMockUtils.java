package ru.yandex.market.vendors.analytics.core.utils;

import org.mockito.Mockito;

import ru.yandex.common.framework.user.UserInfo;

import static org.mockito.Mockito.when;

/**
 * @author ogonek
 */
public class UserInfoMockUtils {

    private UserInfoMockUtils() {
    }

    public static UserInfo mockUserInfo(String userLogin, long userId) {
        var userInfo = Mockito.mock(UserInfo.class);
        when(userInfo.getLogin()).thenReturn(userLogin);
        when(userInfo.getUserId()).thenReturn(userId);
        return userInfo;
    }

}
