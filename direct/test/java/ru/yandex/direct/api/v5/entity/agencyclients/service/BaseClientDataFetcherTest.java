package ru.yandex.direct.api.v5.entity.agencyclients.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.blackbox.client.BlackboxClient;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.core.entity.account.score.service.AccountScoreService;
import ru.yandex.direct.core.entity.campaign.service.WalletService;
import ru.yandex.direct.core.entity.client.service.AgencyClientRelationService;
import ru.yandex.direct.core.entity.client.service.ClientLimitsService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.integrations.configuration.IntegrationsConfiguration;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmIntegrationImpl;
import ru.yandex.direct.tvm.TvmService;
import ru.yandex.inside.passport.blackbox2.Blackbox2;
import ru.yandex.inside.passport.blackbox2.BlackboxRawRequestExecutor;
import ru.yandex.inside.passport.blackbox2.BlackboxRequestExecutorWithRetries;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.thread.ExecutionRuntimeException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.tvm.TvmService.DIRECT_SCRIPTS_TEST;

@Ignore("Для ручного запуска. Ходит в blackbox")
@Api5Test
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class BaseClientDataFetcherTest {

    private static final String BLACKBOX_URL = "http://ppctest-proxy.ppc.yandex.ru:7088/blackbox/";
    private static final String MESSAGE_WRONG_TVM_TICKET = "Failed to check service ticket: Malformed ticket";

    private static final Long UID_1 = 12L;
    private static final Long UID_2 = 41L;
    private static final Long UID_3 = 77L;

    private TvmIntegration tvmIntegration;
    private BaseClientDataFetcher baseClientDataFetcher;
    @Autowired
    private @Qualifier(IntegrationsConfiguration.BLACKBOX_EXECUTOR_SERVICE)
    ExecutorService executor;

    @Before
    public void prepare() {
        Map<String, Object> conf = new HashMap<>();
        conf.put("tvm.enabled", true);
        conf.put("tvm.app_id", DIRECT_SCRIPTS_TEST.getId());
        conf.put("tvm.secret", "file://~/.direct-tokens/tvm2_direct-scripts-test");
        DirectConfig directConfig = DirectConfigFactory.getConfig(EnvironmentType.DEVELOPMENT, conf);
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        tvmIntegration = spy(TvmIntegrationImpl.create(directConfig, scheduler));

        Blackbox2 blackbox2 = new Blackbox2(
                new BlackboxRequestExecutorWithRetries(
                        new BlackboxRawRequestExecutor(BLACKBOX_URL,
                                Timeout.milliseconds(5000)),
                        2
                ));
        BlackboxClient blackboxClient = new BlackboxClient(blackbox2);

        AgencyClientRelationService agencyClientRelationService = mock(AgencyClientRelationService.class);
        AccountScoreService accountScoreService = mock(AccountScoreService.class);
        ClientLimitsService clientLimitsService = mock(ClientLimitsService.class);
        ClientService clientService = mock(ClientService.class);
        WalletService walletService = mock(WalletService.class);

        baseClientDataFetcher = new BaseClientDataFetcher(agencyClientRelationService, accountScoreService,
                blackboxClient, clientLimitsService, clientService, walletService, executor, tvmIntegration,
                EnvironmentType.DEVELOPMENT);
    }

    /**
     * При запросе в blackbox с правильным Tvm тикетом для получения телефонов пользователей с определенными uid ->
     * получим корректные маски-номера для данных uid
     */
    @Test
    public void getSmsPhones_oneUid() {
        String expectedMaskedE164Number1 = "+7000*****58";
        String expectedMaskedE164Number2 = "+7000*****69";

        Map<Long, String> map = baseClientDataFetcher.getSmsPhones(List.of(UID_1, UID_2));
        assertThat(map.get(UID_1)).isEqualTo(expectedMaskedE164Number1);
        assertThat(map.get(UID_2)).isEqualTo(expectedMaskedE164Number2);
    }

    /**
     * При запросе в blackbox с правильным Tvm тикетом для получения телефона пользователя с определенным uid  ->
     * получим корректную маску-номер для данного uid
     */
    @Test
    public void getSmsPhones_multipleUids() {
        String expectedMaskedE164Number = "+7000*****14";

        Map<Long, String> map = baseClientDataFetcher.getSmsPhones(List.of(UID_3));
        assertThat(map.get(UID_3)).isEqualTo(expectedMaskedE164Number);
    }

    /**
     * При запросе в blackbox с не правильным Tvm тикетом для получения телефона пользователя c определенным uid ->
     * получим исключение {@link ExecutionRuntimeException}
     */
    @Test
    public void getSmsPhones_oneUidWithWrongTvm() {
        when(tvmIntegration.getTicket(TvmService.BLACKBOX_MIMINO)).thenReturn("wrong_ticket");
        try {
            baseClientDataFetcher.getSmsPhones(List.of(UID_1));
            Assert.fail();
        } catch (ExecutionRuntimeException e) {
            assertThat(e.getMessage()).contains(MESSAGE_WRONG_TVM_TICKET);
        }
    }

    /**
     * При запросе в blackbox с не правильным Tvm тикетом для получения телефонов пользователей с определенными uid ->
     * получим исключение {@link ExecutionRuntimeException}
     */
    @Test
    public void getSmsPhones_multipleUidsWithWrongTvm() {
        when(tvmIntegration.getTicket(TvmService.BLACKBOX_MIMINO)).thenReturn("wrong_ticket");
        try {
            baseClientDataFetcher.getSmsPhones(List.of(UID_2, UID_3));
            Assert.fail();
        } catch (ExecutionRuntimeException e) {
            assertThat(e.getCause().getCause().getMessage()).contains(MESSAGE_WRONG_TVM_TICKET);
        }
    }
}
