package ru.yandex.direct.core.entity.user.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.blackbox.client.BlackboxClient;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.core.entity.user.model.BlackboxUser;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.i18n.Language;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmIntegrationImpl;
import ru.yandex.direct.tvm.TvmService;
import ru.yandex.inside.passport.blackbox2.Blackbox2;
import ru.yandex.inside.passport.blackbox2.BlackboxRawRequestExecutor;
import ru.yandex.inside.passport.blackbox2.BlackboxRequestExecutorWithRetries;
import ru.yandex.inside.passport.blackbox2.protocol.BlackboxHttpException;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxFatalException;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.ip.Ipv4Address;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.tvm.TvmService.DIRECT_SCRIPTS_TEST;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore("For manual run only because tests to use external service")
public class BlackboxUserServiceManualTest {

    private static final String BLACKBOX_URL_PPCDEV = "http://ppctest-proxy.ppc.yandex.ru:7088/blackbox/";
    private static final long UID1 = 691577527L;
    private static final long UID2 = 904363533L;
    private static final long UID3 = 891335900L;
    private static final long UID4 = 1147882464L; // белорусский клиент
    private static final String LOGIN1 = "yndx-ajkon-super-1";
    private static final String LOGIN2 = "ruzhansky1";
    private static final String EMAIL = LOGIN1 + "@yandex.ru";
    private static final Language LANG = Language.RU;
    private static final Ipv4Address USER_IP = Ipv4Address.parse("127.0.0.1");
    private static final String MESSAGE_WRONG_TVM_TICKET = "failed to check service ticket: Malformed ticket";

    private BlackboxUserService service;
    private TvmIntegration tvmIntegration;

    @Before
    public void initTest() {
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
                        new BlackboxRawRequestExecutor(BLACKBOX_URL_PPCDEV,
                                Timeout.milliseconds(5000)),
                        2
                ));
        BlackboxClient blackboxClient = new BlackboxClient(blackbox2);
        service = spy(new BlackboxUserService(blackboxClient, EnvironmentType.DEVELOPMENT, tvmIntegration));
        when(service.getUserAddress()).thenReturn(USER_IP);
    }

    @Test
    public void getUidByLogin_success() {
        Long uid = service.getUidByLogin(LOGIN1).orElse(null);
        assertThat(uid).as("Uid").isEqualTo(UID1);
    }

    /**
     * При запросе в blackbox с не правильным Tvm тикетом для получения Uid пользователя по его логину  ->
     * получим исключение {@link BlackboxFatalException}
     */
    @Test
    public void getUidByLogin_withWrongTvm() {
        when(tvmIntegration.getTicket(TvmService.BLACKBOX_MIMINO)).thenReturn("wrong_ticket");
        try {
            service.getUidByLogin(LOGIN2);
            Assert.fail();
        } catch (BlackboxFatalException e) {
            assertThat(e.getMessage()).contains(MESSAGE_WRONG_TVM_TICKET);
        }
    }

    @Test
    public void getUserInfo_success() {
        BlackboxUser user = service.getUserInfo(UID1);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(user.getLogin()).as("Login").isEqualTo(LOGIN1);
            soft.assertThat(user.getEmail()).as("Email").isEqualTo(EMAIL);
            soft.assertThat(user.getLang()).as("Lang").isEqualTo(LANG);
            soft.assertThat(user.getFio()).as("Fio").isNotNull();
        });
    }

    /**
     * При запросе в blackbox с правильным Tvm тикетом для получения информации о пользователях по их uid ->
     * получим корректные данные для каждого пользователя
     */
    @Test
    public void getUserInfo_withTwoUids() {
        Map<Long, BlackboxUser> map = service.getUsersInfo(List.of(UID1, UID2));
        SoftAssertions.assertSoftly(soft -> {
            BlackboxUser user1 = map.get(UID1);
            BlackboxUser user2 = map.get(UID2);
            soft.assertThat(user1.getLogin()).as("Login").isEqualTo(LOGIN1);
            soft.assertThat(user2.getLogin()).as("Login").isEqualTo(LOGIN2);
        });
    }

    /**
     * При запросе в blackbox с не правильным Tvm тикетом для получения информации о пользователе по его uid ->
     * получим исключение {@link BlackboxFatalException}
     */
    @Test
    public void getUserInfo_withWrongTvm() {
        when(tvmIntegration.getTicket(TvmService.BLACKBOX_MIMINO)).thenReturn("wrong_ticket");
        try {
            service.getUserInfo(UID2);
            Assert.fail();
        } catch (BlackboxFatalException e) {
            assertThat(e.getMessage()).contains(MESSAGE_WRONG_TVM_TICKET);
        }
    }

    /**
     * При запросе в blackbox с не правильным Tvm тикетом для получения информации о пользователях по их uid ->
     * получим исключение {@link BlackboxHttpException}
     */
    @Test
    public void getUserInfo_withTwoUidsAndWrongTvm() {
        when(tvmIntegration.getTicket(TvmService.BLACKBOX_MIMINO)).thenReturn("wrong_ticket");
        try {
            service.getUsersInfo(List.of(UID2, UID3));
            Assert.fail();
        } catch (BlackboxHttpException e) {
            assertThat(e.getCause().getMessage()).contains(MESSAGE_WRONG_TVM_TICKET);
        }
    }

    @Test
    public void getCountryByUid_success() {
        String countryRu = service.getCountryByUid(UID1);
        String countryBy = service.getCountryByUid(UID4);
        assertThat(countryRu).isEqualTo("ru");
        assertThat(countryBy).isEqualTo("by");

    }
}
