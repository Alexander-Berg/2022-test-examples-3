package ru.yandex.market.logistics.util.client.tvm;

import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource("/log-only-mode.properties")
class LogOnlyModeTest extends AbstractTvmServiceTicketTest {

    @Override
    public void checkAuth(TvmErrorType error, String ticket) {
        TvmAuthenticationToken authentication = new TvmAuthenticationToken(ticket, "", "", REQUEST_URI);
        Authentication authenticate = authenticationManager.authenticate(authentication);
        softly.assertThat(authenticate.isAuthenticated()).isTrue();
    }

}
