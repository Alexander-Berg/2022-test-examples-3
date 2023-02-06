package ru.yandex.direct.web.configuration.mock.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.core.testing.info.BlackboxUserInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.UserSteps;

public class TestAuthHelper {
    private final DirectWebAuthenticationSourceMock directWebAuthenticationSourceMock;
    private final UserService userService;
    private final UserSteps userSteps;

    public TestAuthHelper(
            DirectWebAuthenticationSourceMock directWebAuthenticationSourceMock, UserService userService,
            UserSteps userSteps) {
        this.directWebAuthenticationSourceMock = directWebAuthenticationSourceMock;
        this.userService = userService;
        this.userSteps = userSteps;
    }

    public UserInfo createDefaultUser() {
        UserInfo user = userSteps.createDefaultUser();
        setOperatorAndSubjectUser(user.getUid());
        return user;
    }

    public BlackboxUserInfo createDefaultBlackboxUser() {
        BlackboxUserInfo user = userSteps.createDefaultBlackboxUser();
        return user;
    }

    public void setOperator(long uid) {
        User user = userService.getUser(uid);
        directWebAuthenticationSourceMock.withOperator(user);
    }

    public void setSubjectUser(long uid) {
        User user = userService.getUser(uid);
        directWebAuthenticationSourceMock.withSubjectUser(user);
    }

    public void setOperatorAndSubjectUser(long uid) {
        User user = userService.getUser(uid);
        directWebAuthenticationSourceMock.withOperator(user).withSubjectUser(user);
    }

    public void setSecurityContext() {
        DirectAuthentication authentication = directWebAuthenticationSourceMock.getAuthentication();
        setSecurityContextWithAuthentication(authentication);
    }

    public static void setSecurityContextWithAuthentication(Authentication authentication) {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(ctx);
        ctx.setAuthentication(authentication);
    }
}
