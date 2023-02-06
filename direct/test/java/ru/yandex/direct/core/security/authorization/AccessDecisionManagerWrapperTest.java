package ru.yandex.direct.core.security.authorization;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.core.testing.data.TestUsers;

import static org.mockito.Mockito.doThrow;

@ParametersAreNonnullByDefault
public class AccessDecisionManagerWrapperTest {

    private User user;
    private Authentication authentication;
    private Object object;
    private Collection<ConfigAttribute> configAttributes;

    @Mock
    private AccessDecisionManager wrappedManager;

    @InjectMocks
    private AccessDecisionManagerWrapper accessDecisionManagerWrapper;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void initTestData() {
        MockitoAnnotations.initMocks(this);

        user = TestUsers.generateNewUser();
        authentication = new DirectAuthentication(user, user);
        object = new Object();
        configAttributes = Collections.emptyList();
    }


    @Test
    public void checkExceptionMessage_WhenAccessDenied() {
        doThrow(new AccessDeniedException("bla bla"))
                .when(wrappedManager).decide(authentication, object, configAttributes);
        thrown.expect(ru.yandex.direct.core.security.AccessDeniedException.class);
        thrown.expectMessage("access is denied for " + user.getLogin());

        accessDecisionManagerWrapper.decide(authentication, object, configAttributes);
    }

}
