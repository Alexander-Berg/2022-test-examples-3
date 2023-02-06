package ru.yandex.market.checker.controller.interceptor;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;

import ru.yandex.market.billing.security.blackbox.AuthType;
import ru.yandex.market.billing.security.blackbox.UserInformation;
import ru.yandex.market.checker.utils.AuthUtils;

@ParametersAreNonnullByDefault
public class MockAuthInterceptor implements HandlerInterceptor {
    public static final String TEST_USER_LOGIN = "testLogin";
    public static final long TEST_USER_UID = 12345L;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        AuthUtils.saveUserInfo(request, new UserInformation(TEST_USER_LOGIN, TEST_USER_UID), AuthType.UID);
        return true;
    }
}
