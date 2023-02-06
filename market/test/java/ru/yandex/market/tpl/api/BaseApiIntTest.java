package ru.yandex.market.tpl.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.tpl.common.web.blackbox.BlackboxClient;
import ru.yandex.market.tpl.common.web.blackbox.OAuthUser;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserUtil;
import ru.yandex.market.tpl.core.service.user.UserAuthService;
import ru.yandex.market.tpl.core.service.user.transport.TransportType;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.company.Company.DEFAULT_COMPANY_NAME;

@ApiIntTest
public abstract class BaseApiIntTest {
    protected static final Long UID = 1L;
    public static final String AUTH_HEADER_VALUE = "OAuth uid-" + UID;

    @Autowired
    protected BlackboxClient blackboxClient;

    @Autowired
    protected UserAuthService userAuthService;

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    private TvmClient tvmClient;

    @BeforeEach
    public void setUpBase() {
        mockBlackboxClient(UID);
    }

    protected void mockBlackboxClient(Long uid) {
        mockBlackboxClient(new OAuthUser(uid, Set.of(), null, List.of()));
    }

    protected void mockBlackboxClient(OAuthUser user) {
        when(blackboxClient.oauth(anyString(), anyString()))
                .thenReturn(user);
    }

    protected void mockUser(Long id) {
        User user = UserUtil.createUserWithoutSchedule(
                UID,
                TransportType.builder()
                        .name("Машинка")
                        .capacity(BigDecimal.valueOf(10.0))
                        .build(),
                Company.builder()
                        .name(DEFAULT_COMPANY_NAME)
                        .login("test@yandex.ru")
                        .taxpayerNumber("01234567890")
                        .juridicalAddress("г. Москва")
                        .phoneNumber("88005553535")
                        .build());
        UserUtil.setId(user, UID);
        when(userAuthService.findByUid(anyLong())).thenReturn(Optional.of(user));
    }
}
