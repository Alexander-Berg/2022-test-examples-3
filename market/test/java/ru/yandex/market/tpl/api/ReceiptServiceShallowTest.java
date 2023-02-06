package ru.yandex.market.tpl.api;

import java.time.Clock;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.common.util.SynchronizedData;
import ru.yandex.market.tpl.core.advice.PersonalDataResponseBodyAdvice;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxClient;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.receipt.lifepay.LifePayService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserCommandService;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.service.user.UserAuthService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author valter
 */
public abstract class ReceiptServiceShallowTest {

    @MockBean
    protected Clock clock;

    @MockBean
    protected BlackboxClient blackboxClient;

    @MockBean
    protected UserAuthService userAuthService;
    @MockBean
    protected LifePayService lifePayService;
    @Autowired
    protected MockMvc mockMvc;
    @MockBean
    protected TvmClient tvmClient;
    @MockBean
    private UserCommandService userCommandService;
    @MockBean
    private SynchronizedData<String, User> usersByYaProId;
    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;
    @MockBean
    private TransactionTemplate transactionTemplate;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private PersonalDataResponseBodyAdvice personalDataResponseBodyAdvice;

    @BeforeEach
    void setUp() {
        ClockUtil.initFixed(clock);
        when(blackboxClient.oauth(anyString(), anyString())).thenThrow(RuntimeException.class);
        when(userAuthService.findByUid(anyLong())).thenThrow(RuntimeException.class);
    }

}
