package ru.yandex.direct.web.configuration.mock.auth;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;

import static com.google.common.base.Preconditions.checkState;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DirectWebAuthenticationSourceMock extends DirectWebAuthenticationSource {

    private final DirectAuthentication directAuthenticationMock;

    public DirectWebAuthenticationSourceMock() {
        directAuthenticationMock = mock(DirectAuthentication.class);
        when(directAuthenticationMock.isAuthenticated()).thenReturn(true);
    }

    @Override
    public DirectAuthentication getAuthentication() {
        return directAuthenticationMock;
    }

    public DirectWebAuthenticationSourceMock withOperator(User operator) {
        when(directAuthenticationMock.getOperator()).thenReturn(operator);
        return this;
    }

    public DirectWebAuthenticationSourceMock withSubjectUser(User subjectUser) {
        when(directAuthenticationMock.getSubjectUser()).thenReturn(subjectUser);
        return this;
    }

    public static DirectWebAuthenticationSourceMock castToMock(DirectWebAuthenticationSource source) {
        checkState(source instanceof DirectWebAuthenticationSourceMock,
                "экземпляр DirectWebAuthenticationSource не является инстансом класса DirectWebAuthenticationSourceMock");
        return (DirectWebAuthenticationSourceMock) source;
    }
}
