package ru.yandex.market.tpl.carrier.driver;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.tpl.carrier.driver.service.user.UserAuthService;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxClient;
import ru.yandex.market.tpl.common.web.blackbox.OAuthUser;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DriverApiIntTest
public abstract class BaseDriverApiIntTest {
    protected static final Long UID = 1L;
    public static final String AUTH_HEADER_VALUE = "OAuth uid-" + UID;
    public static final String TAXI_UID_HEADER_VALUE = String.valueOf(UID);
    public static final String TAXI_PROFILE_ID_HEADER_VALUE = "123";
    public static final String TAXI_PARK_ID_HEADER_VALUE = "456";

    @Autowired
    protected BlackboxClient blackboxClient;

    @Autowired
    protected UserAuthService userAuthService;

    @Autowired
    protected MockMvc mockMvc;

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
}
