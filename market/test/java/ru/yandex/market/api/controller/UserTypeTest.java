package ru.yandex.market.api.controller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.server.sec.User;
import ru.yandex.market.api.server.sec.Uuid;
import ru.yandex.market.api.server.sec.exceptions.AccessDeniedException;
import ru.yandex.market.api.util.ControllerUtil;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class UserTypeTest extends UnitTestBase {

    private final User passportUser = mock(User.class);
    private final User uuidUser = mock(User.class);

    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(passportUser.getOAuthUser()).thenReturn(new OauthUser(1));

        when(uuidUser.getUuid()).thenReturn(new Uuid("test"));
    }

    @Test
    public void shouldDefinePassportUserAsOauth() throws Exception {
        UserType result = UserType.getUserType(passportUser);
        Assert.assertEquals(UserType.OAUTH, result);
    }

    @Test
    public void shouldDefineMuidUserAsMobile() {
        UserType result = UserType.getUserType(uuidUser);
        Assert.assertEquals(UserType.MOBILE, result);
    }

}
