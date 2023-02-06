package ru.yandex.direct.api.v5.security.ticket;

import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.direct.api.v5.security.DirectApiCredentials;
import ru.yandex.direct.api.v5.security.exception.BadCredentialsException;
import ru.yandex.direct.api.v5.security.internal.DirectApiInternalAuthRequest;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.entity.application.service.ApiAppAccessService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class TvmUserTicketAuthProviderTest {

    private static final String TVM_SERVICE_TICKET = "TVM_SERVICE_TICKET";
    private static final String TVM_USER_TICKET = "TVM_USER_TICKET";
    private static final String APPLICATION_ID = "APPLICATION_ID";
    private static final long OPERATOR_UID = 3333;
    private static final String OPERATOR_LOGIN = "OPERATOR_LOGIN";

    @Mock
    ApiAppAccessService apiAppAccessService;
    @Mock
    TvmIntegration tvmIntegration;
    @Mock
    private DirectConfig directConfig;
    @Mock
    private ShardHelper shardHelper;

    private TvmUserTicketAuthProvider tvmUserTicketAuthProvider;

    @Before
    public void setUp() {
        initMocks(this);

        when(tvmIntegration.getTvmService(eq(TVM_SERVICE_TICKET)))
                .thenReturn(TvmService.DIRECT_DEVELOPER);
        when(tvmIntegration.checkUserTicket(eq(TVM_USER_TICKET))).thenReturn(OPERATOR_UID);

        DirectConfig tvmAuthBranch = mock(DirectConfig.class);
        when(directConfig.getBranch(eq("tvm_api_auth"))).thenReturn(tvmAuthBranch);
        when(tvmAuthBranch.asMap())
                .thenReturn(Map.of(TvmService.DIRECT_DEVELOPER.name().toLowerCase(), APPLICATION_ID));

        when(apiAppAccessService.checkApplicationAccess(eq(APPLICATION_ID)))
                .thenReturn(true);

        when(shardHelper.getLoginByUid(eq(OPERATOR_UID))).thenReturn(OPERATOR_LOGIN);

        tvmUserTicketAuthProvider = new TvmUserTicketAuthProvider(apiAppAccessService, tvmIntegration, shardHelper,
                directConfig);
    }

    @Test
    public void authenticate_success() {
        DirectApiCredentials credentials = mock(DirectApiCredentials.class);
        when(credentials.getServiceTicket()).thenReturn(TVM_SERVICE_TICKET);
        when(credentials.getUserTicket()).thenReturn(TVM_USER_TICKET);

        DirectApiInternalAuthRequest internalAuthRequest = tvmUserTicketAuthProvider.authenticate(credentials);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(internalAuthRequest.getCredentials()).isEqualTo(credentials);
            soft.assertThat(internalAuthRequest.getPrincipal()).isEqualTo(OPERATOR_LOGIN);
            soft.assertThat(internalAuthRequest.getUid()).isEqualTo(OPERATOR_UID);
            soft.assertThat(internalAuthRequest.getTvmUserTicket()).isEqualTo(TVM_USER_TICKET);
            soft.assertThat(internalAuthRequest.getApplicationId()).isEqualTo(APPLICATION_ID);
        });
    }

    @Test(expected = BadCredentialsException.class)
    public void authenticate_whenServiceTicketNotSet_failure() {
        DirectApiCredentials credentials = mock(DirectApiCredentials.class);
        when(credentials.getUserTicket()).thenReturn(TVM_USER_TICKET);

        tvmUserTicketAuthProvider.authenticate(credentials);
    }

}
