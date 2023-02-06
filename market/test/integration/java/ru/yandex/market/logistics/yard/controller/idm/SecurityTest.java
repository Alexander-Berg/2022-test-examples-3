package ru.yandex.market.logistics.yard.controller.idm;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.web.access.WebInvocationPrivilegeEvaluator;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.inside.passport.PassportDomain;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox2.protocol.BlackboxMethod;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxCorrectResponse;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxNewSession;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxSessionIdException;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketChecker;
import ru.yandex.market.logistics.yard.base.AbstractContextualTest;
import ru.yandex.market.logistics.yard.config.BlackBoxProperties;
import ru.yandex.market.logistics.yard.config.SecurityConfig;
import ru.yandex.market.logistics.yard.config.tvm.BlackBoxClientConfig;
import ru.yandex.market.logistics.yard.config.tvm.TvmConfig;
import ru.yandex.market.logistics.yard.config.tvm.TvmProperties;
import ru.yandex.market.logistics.yard.service.auth.BlackboxRequestManager;
import ru.yandex.misc.ip.IpAddress;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({
        BlackBoxClientConfig.class,
        SecurityConfig.class,
        TvmConfig.class
})
@AutoConfigureMockMvc()
@EnableConfigurationProperties(value = {BlackBoxProperties.class, TvmProperties.class})
public class SecurityTest extends AbstractContextualTest {

    private static final BlackboxCorrectResponse blackboxResponse = spy(new BlackboxCorrectResponse(
            BlackboxMethod.SESSION_ID,
            Option.empty(),
            Option.empty(),
            BlackboxSessionIdException.BlackboxSessionIdStatus.VALID.getId(),
            Option.empty(),
            Option.of(new Tuple2<>(new PassportUid(1234465), PassportDomain.YANDEX_TEAM_RU)),
            Option.of("MY_LOGIN"),
            Cf.map(),
            Cf.list(),
            Cf.list(),
            Cf.list(),
            Option.empty(),
            Cf.map(),
            Option.empty(),
            Option.empty(),
            Option.of(new BlackboxNewSession("1232gvb", PassportDomain.YANDEX_TEAM_RU.toString(),
                    123L, true)),
            Option.of(new BlackboxNewSession("dhrh124", PassportDomain.YANDEX_TEAM_RU.toString(),
                    123L, false)),
            Option.empty(),
            Option.empty(),
            Option.empty(),
            Option.empty())
    );

    @MockBean
    private BlackboxRequestManager blackboxRequestManager;

    @MockBean
    private TvmClient tvmClient;

    @MockBean
    private TvmTicketChecker tvmTicketChecker;

    @Autowired
    private WebInvocationPrivilegeEvaluator webInvocationPrivilegeEvaluator;

    @Test
    public void noSessionCookiesTest() throws Exception {
        mockMvc.perform(get("/equeue/172/operator-windows/all")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void noSessionIdCookieTest() throws Exception {
        mockMvc.perform(get("/equeue/172/operator-windows/all")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(new Cookie("sessionid2", "123456"))
                .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void invalidSessionCookiesTest() throws Exception {
        when(blackboxRequestManager.checkBySessionCookies(any(IpAddress.class), anyString(), any()))
                .thenReturn(blackboxResponse);
        when(blackboxResponse.getStatus())
                .thenReturn(BlackboxSessionIdException.BlackboxSessionIdStatus.INVALID.getId());

        mockMvc.perform(get("/equeue/172/operator-windows/all")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(new Cookie("Session_id", "123456"))
                .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void blackBoxResponseExceptionTest() throws Exception {
        when(blackboxRequestManager.checkBySessionCookies(any(IpAddress.class), anyString(), any()))
                .thenThrow(new RuntimeException());

        mockMvc.perform(get("/equeue/172/operator-windows/all")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(new Cookie("Session_id", "123456"))
                .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void invalidServiceTicketAuthTest() throws Exception {
        doThrow(new RuntimeException()).when(tvmTicketChecker).isRequestedUrlAcceptable(anyString());

        mockMvc.perform(get("/idm/info/")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Ya-Service-Ticket", "sdgsgd43364")
                .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void noServiceTicketExceptionAuthTest() throws Exception {
        mockMvc.perform(get("/idm/info/")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is4xxClientError());
    }

}
