package ru.yandex.direct.api.v5.security.utils;

import org.mockito.MockSettings;

import ru.yandex.direct.core.entity.user.model.ApiEnabled;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * @author mexicano
 */
public class ApiUserMockBuilder {
    private static final MockSettings MOCK_SETTINGS = withSettings().serializable();

    private final ApiUser user;

    public ApiUserMockBuilder(String login, long uid, long clientId, RbacRole role) {
        this(login, uid, clientId, role, MOCK_SETTINGS);
    }

    public ApiUserMockBuilder(String login, long uid, long clientId, RbacRole role, MockSettings mockSettings) {
        user = mock(ApiUser.class, mockSettings);
        when(user.getLogin()).thenReturn(login);
        when(user.getUid()).thenReturn(uid);
        when(user.getRole()).thenReturn(role);
        when(user.getClientId()).thenReturn(ClientId.fromLong(clientId));
    }

    public ApiUserMockBuilder withStatusBlocked(Boolean statusBlocked) {
        when(user.getStatusBlocked()).thenReturn(statusBlocked);
        return this;
    }

    public ApiUserMockBuilder withApiEnabled(ApiEnabled enabled) {
        when(user.getApiEnabled()).thenReturn(enabled);
        return this;
    }

    public ApiUser build() {
        return user;
    }

    public ApiUserMockBuilder withApiAllowedIps(String userApiAllowedIps) {
        when(user.getApiAllowedIps()).thenReturn(userApiAllowedIps);
        return this;
    }

    public ApiUserMockBuilder withPassportKarma(Long karma) {
        when(user.getPassportKarma()).thenReturn(karma);
        return this;
    }

    public ApiUserMockBuilder withManualUnitsLimit(Long manualUnitsLimit) {
        when(user.getApiUnitsDaily()).thenReturn(manualUnitsLimit);
        return this;
    }
}
